package ru.yandex.market.logistics.cte.controller;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.cte.base.SoftAssertionsSupportedTest;
import ru.yandex.market.logistics.cte.client.dto.SupplyDTO;
import ru.yandex.market.logistics.cte.client.enums.RegistryType;
import ru.yandex.market.logistics.cte.converters.SupplyDtoToSupplyConverter;
import ru.yandex.market.logistics.cte.entity.supply.Supply;

class SupplyDtoToSupplyConverterTest extends SoftAssertionsSupportedTest {
    public static final String CONSIGNOR_SUPPLY_ID = "CONSIGNOR_SUPPLY_ID";
    public static final String CONSIGNOR_NAME = "CONSIGNOR_NAME";
    public static final String FF_SUPPLY_ID = "FF_SUPPLY_ID";
    public static final long YANDEX_SUPPLY_ID = 1050;
    private final SupplyDtoToSupplyConverter classUnderTest = new SupplyDtoToSupplyConverter();

    @Test
    public void happyPath() {
        SupplyDTO input = prepareHappyPathFixture();

        Supply result = classUnderTest.convert(YANDEX_SUPPLY_ID, input);
        assertions.assertThat(result).isEqualToIgnoringGivenFields(expectedHappyPathResult(), "createdAt");

    }

    private Supply expectedHappyPathResult() {
        return Supply.builder()
                .yandexSupplyId(YANDEX_SUPPLY_ID)
                .fulfillmentSupplyId(FF_SUPPLY_ID)
                .consignorSupplyId(CONSIGNOR_SUPPLY_ID)
                .consignorName(CONSIGNOR_NAME)
                .registryType(RegistryType.REFUND)
                .build();
    }

    private SupplyDTO prepareHappyPathFixture() {
        return new SupplyDTO(CONSIGNOR_SUPPLY_ID, CONSIGNOR_NAME, FF_SUPPLY_ID, RegistryType.REFUND);
    }

}
