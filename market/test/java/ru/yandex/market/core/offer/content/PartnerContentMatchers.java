package ru.yandex.market.core.offer.content;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.core.offer.content.parameter.CategoryParameterAssignment;
import ru.yandex.market.core.offer.content.parameter.EnumerationValue;
import ru.yandex.market.mbi.util.MbiMatchers;

@ParametersAreNonnullByDefault
class PartnerContentMatchers {
    static Matcher<CategoryParameterAssignment> isBooleanParameterAssignment(long parameterId, boolean value) {
        return Matchers.allOf(
                MbiMatchers.transformedBy(CategoryParameterAssignment::parameterId, Matchers.is(parameterId)),
                isBooleanParameterAssignment(value)
        );
    }

    static Matcher<CategoryParameterAssignment> isBooleanParameterAssignment(boolean value) {
        return MbiMatchers.instanceOf(
                CategoryParameterAssignment.Boolean.class,
                MbiMatchers.transformedBy(CategoryParameterAssignment.Boolean::booleanValue, Matchers.is(value))
        );
    }

    static Matcher<CategoryParameterAssignment> isTextParameterAssignment(long parameterId, String value) {
        return Matchers.allOf(
                MbiMatchers.transformedBy(CategoryParameterAssignment::parameterId, Matchers.is(parameterId)),
                isTextParameterAssignment(value)
        );
    }

    static Matcher<CategoryParameterAssignment> isTextParameterAssignment(String text) {
        return MbiMatchers.instanceOf(
                CategoryParameterAssignment.Text.class,
                MbiMatchers.transformedBy(CategoryParameterAssignment.Text::textValue, Matchers.is(text))
        );
    }

    static Matcher<CategoryParameterAssignment> isNumericParameterAssignment(long parameterId, double value) {
        return Matchers.allOf(
                MbiMatchers.transformedBy(CategoryParameterAssignment::parameterId, Matchers.is(parameterId)),
                isNumericParameterAssignment(value)
        );
    }

    static Matcher<CategoryParameterAssignment> isNumericParameterAssignment(double number) {
        return MbiMatchers.instanceOf(
                CategoryParameterAssignment.Numeric.class,
                MbiMatchers.transformedBy(CategoryParameterAssignment.Numeric::numericValue, Matchers.is(number))
        );
    }

    static Matcher<CategoryParameterAssignment> isEnumeraionParameterAssignment(
            long parameterId,
            int enumerationValueId
    ) {
        return Matchers.allOf(
                MbiMatchers.transformedBy(CategoryParameterAssignment::parameterId, Matchers.is(parameterId)),
                isEnumeraionParameterAssignment(enumerationValueId)
        );
    }

    static Matcher<CategoryParameterAssignment> isEnumeraionParameterAssignment(int enumerationValueId) {
        return MbiMatchers.instanceOf(
                CategoryParameterAssignment.Enumeration.class,
                MbiMatchers.transformedBy(
                        CategoryParameterAssignment.Enumeration::enumerationValueId,
                        Matchers.is(enumerationValueId))
        );
    }

    @Nonnull
    static Matcher<EnumerationValue> isEnumerationValue(int id, String name) {
        return Matchers.allOf(
                MbiMatchers.transformedBy(EnumerationValue::id, Matchers.is(id)),
                MbiMatchers.transformedBy(EnumerationValue::name, Matchers.is(name))
        );
    }
}
