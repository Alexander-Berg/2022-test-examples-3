package ru.yandex.market.delivery.mdbapp.integration.converter;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnDto;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnRequestUpdateDto;

public class ReturnRequestUpdateDtoConverterTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    public ReturnRequestUpdateDtoConverter converter = new ReturnRequestUpdateDtoConverter();

    @Test
    public void test() {
        // when:
        final var actual = converter.convert(returnDto());

        // then:
        softly.assertThat(actual).isEqualToComparingFieldByField(returnRequestUpdateDto());
    }

}
