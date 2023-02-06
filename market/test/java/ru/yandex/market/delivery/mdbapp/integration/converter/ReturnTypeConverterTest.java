package ru.yandex.market.delivery.mdbapp.integration.converter;

import java.util.Collection;
import java.util.List;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnType;

@RunWith(Parameterized.class)
public class ReturnTypeConverterTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    public ReturnTypeConverter converter = new ReturnTypeConverter();

    @Parameter
    public ReturnReasonType returnReasonType;

    @Parameter(1)
    public ReturnType expected;

    @Parameters
    public static Collection<Object[]> data() {
        return List.of(new Object[][]{
            {
                ReturnReasonType.BAD_QUALITY,
                ReturnType.WITH_DISADVANTAGES
            },
            {
                ReturnReasonType.WRONG_ITEM,
                ReturnType.WRONG
            },
            {
                ReturnReasonType.DO_NOT_FIT,
                ReturnType.UNSUITABLE
            },
            {
                ReturnReasonType.UNKNOWN,
                ReturnType.UNSUITABLE
            },
            {
                null,
                null
            }
        });
    }

    @Test
    public void test() {
        softly.assertThat(converter.convert(returnReasonType))
            .isEqualTo(expected);
    }
}
