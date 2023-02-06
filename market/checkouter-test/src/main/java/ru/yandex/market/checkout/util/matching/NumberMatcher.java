package ru.yandex.market.checkout.util.matching;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.hamcrest.CustomTypeSafeMatcher;

/**
 * @author mkasumov
 */
public abstract class NumberMatcher extends CustomTypeSafeMatcher<Number> {

    protected Number expectedNumber;

    protected NumberMatcher(Number expectedNumber) {
        super("a number equal to <" + expectedNumber + ">");
        this.expectedNumber = expectedNumber;
    }

    public static NumberMatcher numberEqualsTo(Number expectedNumber) {
        return new NumberMatcher(expectedNumber) {
            @Override
            protected boolean matchesSafely(Number item) {
                return compare(item, this.expectedNumber) == 0;
            }
        };
    }

    public static NumberMatcher numberGreaterThan(Number expectedNumber) {
        return new NumberMatcher(expectedNumber) {
            @Override
            protected boolean matchesSafely(Number item) {
                return compare(item, this.expectedNumber) > 0;
            }
        };
    }

    public static NumberMatcher numberGreaterThanOrEqualsTo(Number expectedNumber) {
        return new NumberMatcher(expectedNumber) {
            @Override
            protected boolean matchesSafely(Number item) {
                return compare(item, this.expectedNumber) >= 0;
            }
        };
    }

    public static NumberMatcher numberLessThan(Number expectedNumber) {
        return new NumberMatcher(expectedNumber) {
            @Override
            protected boolean matchesSafely(Number item) {
                return compare(item, this.expectedNumber) < 0;
            }
        };
    }

    public static NumberMatcher numberLessThanOrEqualsTo(Number expectedNumber) {
        return new NumberMatcher(expectedNumber) {
            @Override
            protected boolean matchesSafely(Number item) {
                return compare(item, this.expectedNumber) <= 0;
            }
        };
    }

    private static int compare(Number x, Number y) {
        if (isSpecial(x) || isSpecial(y)) {
            return Double.compare(x.doubleValue(), y.doubleValue());
        } else {
            return toBigDecimal(x).compareTo(toBigDecimal(y));
        }
    }

    private static boolean isSpecial(Number num) {
        boolean specialDouble =
                num instanceof Double && (Double.isNaN((Double) num) || Double.isInfinite((Double) num));
        boolean specialFloat = num instanceof Float && (Float.isNaN((Float) num) || Float.isInfinite((Float) num));
        return specialDouble || specialFloat;
    }

    private static BigDecimal toBigDecimal(Number number) {
        if (number instanceof BigDecimal) {
            return (BigDecimal) number;
        }
        if (number instanceof BigInteger) {
            return new BigDecimal((BigInteger) number);
        }
        if (number instanceof Byte || number instanceof Short
                || number instanceof Integer || number instanceof Long) {
            return BigDecimal.valueOf(number.longValue());
        }
        if (number instanceof Float || number instanceof Double) {
            return BigDecimal.valueOf(number.doubleValue());
        }

        try {
            return new BigDecimal(number.toString());
        } catch (final NumberFormatException e) {
            throw new RuntimeException("The given number (\"" + number + "\" of class " + number.getClass().getName() +
                    ") does not have a parsable string representation", e);
        }
    }
}
