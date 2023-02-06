package ru.yandex.test.util;

import java.util.Objects;

public class StringChecker implements Checker {
    private static final int MIN_PREFIX = 16;
    private static final int MIN_DIFF = 64;
    private static final String LF = "\n";
    private static final String STRING_PREFIX = "\n ";
    private static final String MINUS_PREFIX = "\n-";
    private static final String PLUS_PREFIX = "\n+";

    private final String expected;

    public StringChecker(final String expected) {
        this.expected = expected;
    }

    private static int commonPrefixLength(final String lhs, final String rhs) {
        int len = Math.min(lhs.length(), rhs.length());
        int i = 0;
        for (; i < len; ++i) {
            if (lhs.charAt(i) != rhs.charAt(i)) {
                break;
            }
        }
        return i;
    }

    private static int commonSuffixLength(
        final int off,
        final String lhs,
        final String rhs)
    {
        int len = Math.min(lhs.length(), rhs.length()) - off;
        int lhsLen = lhs.length() - 1;
        int rhsLen = rhs.length() - 1;
        int i = 0;
        for (; i < len; ++i) {
            if (lhs.charAt(lhsLen - i) != rhs.charAt(rhsLen - i)) {
                break;
            }
        }
        return i;
    }

    public static String compare(final String expected, final String actual) {
        if (expected == null) {
            if (actual == null) {
                return null;
            } else {
                return "string expected to be null:\n" + actual;
            }
        } else if (actual == null) {
            return "string was null, but expected to be:\n" + expected;
        } else if (expected.equals(actual)) {
            return null;
        }
        StringBuilder sb = new StringBuilder("string mismatch:");
        compare(sb, expected, actual);
        return new String(sb);
    }

    private static void compare(
        final StringBuilder sb,
        final String expected,
        final String actual)
    {
        int prefix = commonPrefixLength(actual, expected);
        int suffix = commonSuffixLength(prefix, actual, expected);
        String start = expected.substring(0, prefix);
        if (!start.isEmpty()) {
            sb.append(STRING_PREFIX);
            sb.append(start.replace(LF, STRING_PREFIX));
        }
        String was = expected.substring(prefix, expected.length() - suffix);
        String become = actual.substring(prefix, actual.length() - suffix);
        String end = expected.substring(expected.length() - suffix);
        if (was.length() >= MIN_PREFIX && become.length() >= MIN_PREFIX
            && (was.length() >= MIN_DIFF || become.length() >= MIN_DIFF))
        {
            int idx = was.indexOf(become.substring(0, MIN_PREFIX));
            if (idx != -1) {
                // idx > 0
                sb.append(MINUS_PREFIX);
                sb.append(was.substring(0, idx).replace(LF, MINUS_PREFIX));
                compare(sb, was.substring(idx), become);
            } else {
                idx = become.indexOf(was.substring(0, MIN_PREFIX));
                if (idx != -1) {
                    sb.append(PLUS_PREFIX);
                    sb.append(
                        become.substring(0, idx).replace(LF, PLUS_PREFIX));
                    compare(sb, was, become.substring(idx));
                } else {
                    idx = was.lastIndexOf(
                        become.substring(become.length() - MIN_PREFIX));
                    if (idx != -1) {
                        int pos = idx + MIN_PREFIX;
                        compare(sb, was.substring(0, pos), become);
                        sb.append(MINUS_PREFIX);
                        sb.append(
                            was.substring(pos).replace(LF, MINUS_PREFIX));
                    } else {
                        idx = become.lastIndexOf(
                            was.substring(was.length() - MIN_PREFIX));
                        if (idx != -1) {
                            int pos = idx + MIN_PREFIX;
                            compare(sb, was, become.substring(0, pos));
                            sb.append(PLUS_PREFIX);
                            sb.append(
                                become.substring(pos)
                                    .replace(LF, PLUS_PREFIX));
                        } else {
                            handleSuffix(sb, was, become);
                        }
                    }
                }
            }
        } else {
            handleSuffix(sb, was, become);
        }
        if (!end.isEmpty()) {
            sb.append(STRING_PREFIX);
            sb.append(end.replace(LF, STRING_PREFIX));
        }
    }

    private static void handleSuffix(
        final StringBuilder sb,
        final String was,
        final String become)
    {
        if (!was.isEmpty()) {
            sb.append(MINUS_PREFIX);
            sb.append(was.replace(LF, MINUS_PREFIX));
        }
        if (!become.isEmpty()) {
            sb.append(PLUS_PREFIX);
            sb.append(become.replace(LF, PLUS_PREFIX));
        }
    }

    @Override
    public String toString() {
        return Objects.toString(expected);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(expected);
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null && expected == null) {
            return true;
        }
        return o instanceof String && check((String) o) == null;
    }

    @Override
    public String check(final String value) {
        return compare(expected, value);
    }
}

