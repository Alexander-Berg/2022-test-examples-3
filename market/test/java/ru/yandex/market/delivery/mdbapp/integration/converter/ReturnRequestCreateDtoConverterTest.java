package ru.yandex.market.delivery.mdbapp.integration.converter;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnRequestCreateDto;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnRequestCreateDtoWithItems;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnRequestDto;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnRequestDtoWithItems;

public class ReturnRequestCreateDtoConverterTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    public ReturnRequestCreateDtoConverter converter =
        new ReturnRequestCreateDtoConverter(new ReturnItemCreateDtoConverter());

    @Test
    public void shouldConvertWithItems() {
        // when:
        final var actual = converter.convert(returnRequestDtoWithItems());

        // then:
        softly.assertThat(actual).isEqualToComparingFieldByField(returnRequestCreateDtoWithItems());
    }

    @Test
    public void shouldConvertWithoutItems() {
        // when:
        final var actual = converter.convert(returnRequestDto());

        // then:
        softly.assertThat(actual).isEqualToComparingFieldByField(returnRequestCreateDto());
    }
}
