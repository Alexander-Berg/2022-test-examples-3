package ui_tests.src.test.java.tools;

import static org.apache.maven.surefire.shade.org.apache.commons.lang3.ArrayUtils.isEquals;

public class Differ {

    private static String formatClassAndValue(Object value, String valueString) {
        String className = value == null ? "null" : value.getClass().getName();
        return className + "<" + valueString + ">";
    }

    private static boolean equalsRegardingNull(Object expected, Object actual) {
        if (expected == null) {
            return actual == null;
        }

        return isEquals(expected, actual);
    }

    /**
     * Вывести все данные элемента один над другим для визуального сравнения различий
     * "должно быть"
     * "получилось"
     *
     * @param message
     * @param expected
     * @param actual
     * @return
     */
    public String format(String message, Object expected, Object actual) {
        String formatted = "";
        if (message != null && !"".equals(message)) {
            formatted = message + " ";
        }
        String expectedString = String.valueOf(expected);
        String actualString = String.valueOf(actual);
        if (equalsRegardingNull(expectedString, actualString)) {
            return formatted + "\nexpected: "
                    + formatClassAndValue(expected, expectedString)
                    + "\n but was: " + formatClassAndValue(actual, actualString);
        } else {
            return formatted + "\nexpected:<" + expectedString + ">\n but was:<"
                    + actualString + ">\n";
        }
    }
}
