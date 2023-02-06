package ru.yandex.market.global.partner.util;

import java.math.BigDecimal;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BiPredicate;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.jetbrains.annotations.NotNull;

public class AssertjUtil {
    public static final RecursiveComparisonConfiguration DEFAULT_COMPARISON_CONFIGURATION =
            createDefaultComparisonConfigurationBuilder()
                    .build();

    public static boolean doubleEqualsExtended(Object dbl, Object o) {
        if (o instanceof BigDecimal) {
            return ((BigDecimal) o).doubleValue() == (Double) dbl;
        }
        return dbl.equals(o);
    }

    public static boolean timeStrEqualsExtended(Object str, Object o) {
        if (o instanceof OffsetTime) {
            return DateTimeFormatter.ofPattern("HH:mm:ss").format((OffsetTime) o).equals(str);
        }
        return str.equals(o);
    }

    @NotNull
    public static RecursiveComparisonConfiguration.Builder createDefaultComparisonConfigurationBuilder() {
        return RecursiveComparisonConfiguration.builder()
                .withIgnoreAllExpectedNullFields(true)
                .withIgnoreAllOverriddenEquals(true)
                .withIgnoreCollectionOrder(true)
                .withEqualsForType((BiPredicate<Object, Object>) AssertjUtil::doubleEqualsExtended, Double.class);
    }

}
