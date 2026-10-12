package wordfrequency;

public class PorterStemer {
    // The passed in word turned into a char array.
    // Quicker to use to rebuilding strings each time a change is made.
    private char[] wordArray;

    // Current index to the end of the word in the character array. This will
    // change as the end of the string gets modified.
    private int endIndex;

    // Index of the (potential) end of the stem word in the char array.
    private int stemIndex;

    /**
     * Stem the passed in word.
     *
     * @param word Word to evaluate
     * @return Stemmed word
     */
    public String stemWord(String word) {
        // Do nothing for empty strings or short words.
        if (word == null || word.trim().isEmpty() || word.length() <= 2) {
            return word;
        }

        wordArray = word.toCharArray();

        stemIndex = 0;
        endIndex = word.length() - 1;
        step1();
        step2();
        step3();
        step4();
        step5();
        step6();

        int length = endIndex + 1;
        return new String(wordArray, 0, length);
    }

    // step1() gets rid of plurals and -ed or -ing.
    private void step1() {
        // If the word ends with s take that off
        if (wordArray[endIndex] == 's') {
            if (endsWith("sses")) {
                endIndex -= 2;
            } else if (endsWith("ies")) {
                setEnd("i");
            } else if (wordArray[endIndex - 1] != 's') {
                endIndex--;
            }
        }
        if (endsWith("eed")) {
            if (measureConsonantSequence() > 0)
                endIndex--;
        } else if ((endsWith("ed") || endsWith("ing")) && vowelInStem()) {
            endIndex = stemIndex;
            if (endsWith("at"))
                setEnd("ate");
            else if (endsWith("bl"))
                setEnd("ble");
            else if (endsWith("iz"))
                setEnd("ize");
            else if (isDoubleConsonant(endIndex)) {
                endIndex--;
                int ch = wordArray[endIndex];
                if (ch == 'l' || ch == 's' || ch == 'z')
                    endIndex++;
            } else if (measureConsonantSequence() == 1 && isCVC(endIndex)) {
                setEnd("e");
            }
        }
    }

    // step2() turns terminal y to i when there is another vowel in the stem.
    private void step2() {
        if (endsWith("y") && vowelInStem())
            wordArray[endIndex] = 'i';
    }

    // step3() maps double suffices to single ones.
    private void step3() {
        if (endIndex == 0) return;

        switch (wordArray[endIndex - 1]) {
            case 'a':
                if (endsWith("ational")) { replaceEnd("ate"); break; }
                if (endsWith("tional")) { replaceEnd("tion"); }
                break;
            case 'c':
                if (endsWith("enci")) { replaceEnd("ence"); break; }
                if (endsWith("anci")) { replaceEnd("ance"); }
                break;
            case 'e':
                if (endsWith("izer")) { replaceEnd("ize"); }
                break;
            case 'l':
                if (endsWith("bli")) { replaceEnd("ble"); break; }
                if (endsWith("alli")) { replaceEnd("al"); break; }
                if (endsWith("entli")) { replaceEnd("ent"); break; }
                if (endsWith("eli")) { replaceEnd("e"); break; }
                if (endsWith("ousli")) { replaceEnd("ous"); }
                break;
            case 'o':
                if (endsWith("ization")) { replaceEnd("ize"); break; }
                if (endsWith("ation")) { replaceEnd("ate"); break; }
                if (endsWith("ator")) { replaceEnd("ate"); }
                break;
            case 's':
                if (endsWith("alism")) { replaceEnd("al"); break; }
                if (endsWith("iveness")) { replaceEnd("ive"); break; }
                if (endsWith("fulness")) { replaceEnd("ful"); break; }
                if (endsWith("ousness")) { replaceEnd("ous"); }
                break;
            case 't':
                if (endsWith("aliti")) { replaceEnd("al"); break; }
                if (endsWith("iviti")) { replaceEnd("ive"); break; }
                if (endsWith("biliti")) { replaceEnd("ble"); }
                break;
            case 'g':
                if (endsWith("logi")) { replaceEnd("log"); }
                break;
        }
    }

    // step4() deals with -ic-, -full, -ness etc. similar strategy to step3.
    private void step4() {
        switch (wordArray[endIndex]) {
            case 'e':
                if (endsWith("icate")) { replaceEnd("ic"); break; }
                if (endsWith("ative")) { replaceEnd(""); break; }
                if (endsWith("alize")) { replaceEnd("al"); }
                break;
            case 'i':
                if (endsWith("iciti")) { replaceEnd("ic"); }
                break;
            case 'l':
                if (endsWith("ical")) { replaceEnd("ic"); break; }
                if (endsWith("ful")) { replaceEnd(""); }
                break;
            case 's':
                if (endsWith("ness")) { replaceEnd(""); }
                break;
        }
    }

    // step5() takes off -ant, -ence etc., in context <c>vcvc<v>.
    private void step5() {
        if (endIndex == 0) return;

        switch (wordArray[endIndex - 1]) {
            case 'a':
                if (endsWith("al")) break; return;
            case 'c':
                if (endsWith("ance")) break;
                if (endsWith("ence")) break; return;
            case 'e':
                if (endsWith("er")) break; return;
            case 'i':
                if (endsWith("ic")) break; return;
            case 'l':
                if (endsWith("able")) break;
                if (endsWith("ible")) break; return;
            case 'n':
                if (endsWith("ant")) break;
                if (endsWith("ement")) break;
                if (endsWith("ment")) break;
                if (endsWith("ent")) break; return;
            case 'o':
                if (endsWith("ion") && stemIndex >= 0 && (wordArray[stemIndex] == 's' || wordArray[stemIndex] == 't')) break;
                if (endsWith("ou")) break; return;
            case 's':
                if (endsWith("ism")) break; return;
            case 't':
                if (endsWith("ate")) break;
                if (endsWith("iti")) break; return;
            case 'u':
                if (endsWith("ous")) break; return;
            case 'v':
                if (endsWith("ive")) break; return;
            case 'z':
                if (endsWith("ize")) break; return;
            default:
                return;
        }
        if (measureConsonantSequence() > 1)
            endIndex = stemIndex;
    }

    // step6() removes a final -e if m() > 1.
    private void step6() {
        stemIndex = endIndex;

        if (wordArray[endIndex] == 'e') {
            int a = measureConsonantSequence();
            if (a > 1 || (a == 1 && !isCVC(endIndex - 1)))
                endIndex--;
        }
        if (wordArray[endIndex] == 'l' && isDoubleConsonant(endIndex) && measureConsonantSequence() > 1)
            endIndex--;
    }

    // Returns true if the character at the specified index is a consonant.
    private boolean isConsonant(int index) {
        char c = wordArray[index];
        if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u') return false;
        return c != 'y' || (index == 0 || !isConsonant(index - 1));
    }

    // measures the number of consonant sequences between 0 and j.
    private int measureConsonantSequence() {
        int n = 0;
        int index = 0;
        while (true) {
            if (index > stemIndex) return n;
            if (!isConsonant(index)) break;
            index++;
        }
        index++;
        while (true) {
            while (true) {
                if (index > stemIndex) return n;
                if (isConsonant(index)) break;
                index++;
            }
            index++;
            n++;
            while (true) {
                if (index > stemIndex) return n;
                if (!isConsonant(index)) break;
                index++;
            }
            index++;
        }
    }

    // Return true if there is a vowel in the current stem (0 ... stemIndex)
    private boolean vowelInStem() {
        for (int i = 0; i <= stemIndex; i++) {
            if (!isConsonant(i)) return true;
        }
        return false;
    }

    // Returns true if the char at the specified index and the one preceeding it are the same consonants.
    private boolean isDoubleConsonant(int index) {
        if (index < 1) return false;
        return wordArray[index] == wordArray[index - 1] && isConsonant(index);
    }

    // cvc(i) is true <=> i-2,i-1,i has the form consonant - vowel - consonant
    private boolean isCVC(int index) {
        if (index < 2 || !isConsonant(index) || isConsonant(index - 1) || !isConsonant(index - 2)) return false;
        char c = wordArray[index];
        return c != 'w' && c != 'x' && c != 'y';
    }

    // Does the current word array end with the specified string.
    private boolean endsWith(String s) {
        int length = s.length();
        int index = endIndex - length + 1;
        if (index < 0) return false;

        for (int i = 0; i < length; i++) {
            if (wordArray[index + i] != s.charAt(i)) return false;
        }
        stemIndex = endIndex - length;
        return true;
    }

    // Set the end of the word to s.
    private void setEnd(String s) {
        int length = s.length();
        int index = stemIndex + 1;
        for (int i = 0; i < length; i++) {
            wordArray[index + i] = s.charAt(i);
        }
        // Set the end pointer to the new end of the word.
        endIndex = stemIndex + length;
    }

    // Conditionally replace the end of the word
    private void replaceEnd(String s) {
        if (measureConsonantSequence() > 0) setEnd(s);
    }
}