import java.util.*;

/**
 * Parses and evaluates dice roll expressions (e.g., "2d6+3; d10 & 3d4").
 */
public class DiceParser {

    /**
     * Helper class to manage and tokenize the input stream.
     */
    private static class StringStream {
        private StringBuffer buffer;

        public StringStream(String input) {
            this.buffer = new StringBuffer(input);
        }

        private void skipWhitespace() {
            int index = 0;
            while (index < buffer.length() && Character.isWhitespace(buffer.charAt(index))) {
                index++;
            }
            buffer.delete(0, index);
        }

        public boolean isEmpty() {
            skipWhitespace();
            return buffer.length() == 0;
        }

        /**
         * Refactored: Renamed from getInt() to readUnsignedInt() for clarity.
         */
        public Integer readUnsignedInt() {
            skipWhitespace();
            int index = 0;
            while (index < buffer.length() && Character.isDigit(buffer.charAt(index))) {
                index++;
            }

            if (index == 0) return null;

            try {
                Integer result = Integer.parseInt(buffer.substring(0, index));
                buffer.delete(0, index);
                return result;
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * Refactored: Simplified and clarified signed integer reading.
         */
        public Integer readSignedInt() {
            skipWhitespace();
            StringStream state = save();
            if (matchAndConsume("+")) {
                Integer value = readUnsignedInt();
                if (value != null) return value;
                restore(state);
                return null;
            } else if (matchAndConsume("-")) {
                Integer value = readUnsignedInt();
                if (value != null) return -value;
                restore(state);
                return null;
            }
            return readUnsignedInt();
        }

        /**
         * Refactored: Renamed from checkAndEat to matchAndConsume for clarity.
         */
        public boolean matchAndConsume(String token) {
            skipWhitespace();
            if (buffer.indexOf(token) == 0) {
                buffer.delete(0, token.length());
                return true;
            }
            return false;
        }

        public StringStream save() {
            return new StringStream(buffer.toString());
        }

        public void restore(StringStream saved) {
            this.buffer = new StringBuffer(saved.buffer);
        }

        @Override
        public String toString() {
            return buffer.toString();
        }
    }

    public static Vector<DieRoll> parseRoll(String input) {
        StringStream stream = new StringStream(input.toLowerCase());
        Vector<DieRoll> result = parseRollRecursive(stream, new Vector<>());
        return stream.isEmpty() ? result : null;
    }

    /**
     * Refactored: Improved naming to parseRollRecursive and reduced unnecessary recursion.
     */
    private static Vector<DieRoll> parseRollRecursive(StringStream stream, Vector<DieRoll> accumulator) {
        Vector<DieRoll> rolls = parseRepeatedDice(stream);
        if (rolls == null) return null;

        accumulator.addAll(rolls);

        if (stream.matchAndConsume(";")) {
            return parseRollRecursive(stream, accumulator);
        }

        return accumulator;
    }

    /**
     * Refactored: Simplified and renamed from parseXDice to parseRepeatedDice for clarity.
     */
    private static Vector<DieRoll> parseRepeatedDice(StringStream stream) {
        StringStream saved = stream.save();
        Integer count = stream.readUnsignedInt();
        int repeat = 1;

        if (count != null && stream.matchAndConsume("x")) {
            repeat = count;
        } else {
            stream.restore(saved);
        }

        DieRoll roll = parseDice(stream);
        if (roll == null) return null;

        Vector<DieRoll> rolls = new Vector<>();
        for (int i = 0; i < repeat; i++) {
            rolls.add(roll);
        }

        return rolls;
    }

    private static DieRoll parseDice(StringStream stream) {
        return parseChainedDice(parseDiceBase(stream), stream);
    }

    /**
     * Parses a single dice expression, e.g., "2d6+3".
     */
    private static DieRoll parseDiceBase(StringStream stream) {
        Integer count = stream.readUnsignedInt();
        int numDice = (count != null) ? count : 1;

        if (!stream.matchAndConsume("d")) return null;

        Integer sides = stream.readUnsignedInt();
        if (sides == null) return null;

        Integer bonus = stream.readSignedInt();
        return new DieRoll(numDice, sides, (bonus != null) ? bonus : 0);
    }

    /**
     * Refactored: Recursive chaining for "&" joined dice rolls.
     */
    private static DieRoll parseChainedDice(DieRoll base, StringStream stream) {
        if (base == null) return null;

        if (stream.matchAndConsume("&")) {
            DieRoll next = parseDice(stream);
            if (next == null) return null;
            return parseChainedDice(new DiceSum(base, next), stream);
        }

        return base;
    }

    /**
     * Quick test of dice expression parsing and evaluation.
     */
    private static void test(String input) {
        Vector<DieRoll> rolls = parseRoll(input);
        if (rolls == null) {
            System.out.println("Invalid input: " + input);
        } else {
            System.out.println("Parsing: " + input);
            for (DieRoll roll : rolls) {
                System.out.println(roll + ": " + roll.makeRoll());
            }
        }
    }

    public static void main(String[] args) {
        test("d6");
        test("2d6");
        test("d6+5");
        test("4X3d8-5");
        test("12d10+5 & 4d6+2");
        test("d6 ; 2d4+3");
        test("4d6+3 ; 8d12 -15 ; 9d10 & 3d6 & 4d12 +17");
        test("4d6 + xyzzy");
        test("hi");
        test("4d4d4");
    }
}
