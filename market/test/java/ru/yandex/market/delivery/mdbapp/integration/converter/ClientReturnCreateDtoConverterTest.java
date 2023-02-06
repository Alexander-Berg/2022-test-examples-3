package ru.yandex.market.delivery.mdbapp.integration.converter;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.clientReturnCreateDto;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnRequestDtoWithItems;

public class ClientReturnCreateDtoConverterTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    public ClientReturnCreateDtoConverter converter = new ClientReturnCreateDtoConverter();

    @Test
    public void shouldConvert() {
        // when:
        final var actual = converter.convert(returnRequestDtoWithItems());

        // then:
        softly.assertThat(actual).isEqualToComparingFieldByField(clientReturnCreateDto());
    }
}
