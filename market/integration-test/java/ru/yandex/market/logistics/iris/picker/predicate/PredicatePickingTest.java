package ru.yandex.market.logistics.iris.picker.predicate;

import java.math.BigDecimal;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.logistics.iris.core.index.complex.Dimension;
import ru.yandex.market.logistics.iris.core.index.complex.Dimensions;
import ru.yandex.market.logistics.iris.core.index.complex.StockLifetime;
import ru.yandex.market.logistics.iris.core.index.field.Field;
import ru.yandex.market.logistics.iris.core.index.field.PredefinedFields;
import ru.yandex.market.logistics.iris.picker.predefined.predicate.PredicateProvider;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class PredicatePickingTest {

    private final static PredicateProvider PROVIDER = new PredicateProvider();

    @Test
    public void notAcceptableDimensions() {

        Dimensions dimensions = createDimensions(12.12, 13.13, null);
        testDimensions(PredefinedFields.DIMENSIONS, dimensions, false);
        dimensions = createDimensions(12.12, null, 13.13);
        testDimensions(PredefinedFields.DIMENSIONS, dimensions, false);
        dimensions = createDimensions(null, 12.12, 13.13);
        testDimensions(PredefinedFields.DIMENSIONS, dimensions, false);

        dimensions = createDimensions(12.12, 13.13, -4.);
        testDimensions(PredefinedFields.DIMENSIONS, dimensions, false);
        dimensions = createDimensions(12.12, -4., 13.13);
        testDimensions(PredefinedFields.DIMENSIONS, dimensions, false);
        dimensions = createDimensions(-4., 12.12, 13.13);
        testDimensions(PredefinedFields.DIMENSIONS, dimensions, false);

        dimensions = createDimensions(12.12, 13.13, 0.);
        testDimensions(PredefinedFields.DIMENSIONS, dimensions, false);
        dimensions = createDimensions(12.12, 0., 13.13);
        testDimensions(PredefinedFields.DIMENSIONS, dimensions, false);
        dimensions = createDimensions(0., 12.12, 13.13);
        testDimensions(PredefinedFields.DIMENSIONS, dimensions, false);
    }

    @Test
    public void okDimensions() {
        Dimensions dimensions = createDimensions(12.12, 13.13, 14.14);
        boolean res = PROVIDER.checkField(PredefinedFields.DIMENSIONS, dimensions);
        assertThat(res).isTrue();
    }

    @Test
    public void notOkWeights() {
        Dimension w = Dimension.of(negative());

        testDimension(PredefinedFields.WEIGHT_GROSS, w, false);

        testDimension(PredefinedFields.WEIGHT_NETT, w, false);

        testDimension(PredefinedFields.WEIGHT_TARE, w, false);

        w = Dimension.of(zero());

        testDimension(PredefinedFields.WEIGHT_GROSS, w, false);

        testDimension(PredefinedFields.WEIGHT_NETT, w, false);

        testDimension(PredefinedFields.WEIGHT_TARE, w, false);
    }

    @Test
    public void OkWeights() {
        Dimension w = Dimension.of(positive());

        testDimension(PredefinedFields.WEIGHT_GROSS, w, true);

        testDimension(PredefinedFields.WEIGHT_NETT, w, true);

        testDimension(PredefinedFields.WEIGHT_TARE, w, true);
    }

    @Test
    public void objectFieldsOk() {
        StockLifetime lifetime = new StockLifetime(ImmutableMap.of());
        boolean res = PROVIDER.checkField(PredefinedFields.STOCK_LIFETIME, lifetime);
        assertThat(res).isTrue();
    }

    @Test
    public void objectFieldNotOk() {
        boolean res = PROVIDER.checkField(PredefinedFields.STOCK_LIFETIME, null);
        assertThat(res).isFalse();
    }

    private static void testDimensions(Field<Dimensions> field, Dimensions value, boolean isTrue) {
        boolean res = PROVIDER.checkField(field, value);
        assertThat(res).isEqualTo(isTrue);
    }

    private static void testDimension(Field<Dimension> field, Dimension value, boolean isTrue) {
        boolean res = PROVIDER.checkField(field, value);
        assertThat(res).isEqualTo(isTrue);
    }

    private static Dimensions createDimensions(Double width, Double length, Double height) {
        return createDimensions(of(width), of(length), of(height));
    }

    private static Dimensions createDimensions(BigDecimal width, BigDecimal length, BigDecimal height) {
        return new Dimensions(
                createDimension(width),
                createDimension(height),
                createDimension(length));
    }

    private static Dimension createDimension(BigDecimal value) {
        return Dimension.of(value);
    }

    private static BigDecimal of(Double val) {
        return Optional.ofNullable(val).map(BigDecimal::valueOf).orElse(null);
    }

    private static BigDecimal negative() {
        return of(-4.);
    }

    private static BigDecimal positive() {
        return of(15.);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO;
    }

}
