package ru.yandex.market.delivery.mdbapp.integration.converter;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.ITEM_ID_1;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.ITEM_SKU_1;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.ITEM_SUPPLIER_1;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.PRICE_1;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnItemCreateDto;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnRequestItemDto;

public class ReturnItemCreateDtoConverterTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    public ReturnItemCreateDtoConverter converter = new ReturnItemCreateDtoConverter();

    @Test
    public void toDto() {
        // expect:
        final var actual = converter.convert(returnRequestItemDto(
            ITEM_ID_1,
            PRICE_1,
            ITEM_SKU_1,
            ITEM_SUPPLIER_1
        ));

        // then:
        softly.assertThat(actual).isEqualToComparingFieldByField(returnItemCreateDto(PRICE_1));
    }
}
