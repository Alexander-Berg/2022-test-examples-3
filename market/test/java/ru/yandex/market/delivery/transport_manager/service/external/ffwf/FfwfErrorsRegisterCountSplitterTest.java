package ru.yandex.market.delivery.transport_manager.service.external.ffwf;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.entity.register.UnitCount;
import ru.yandex.market.delivery.transport_manager.service.register.splitter.dto.RegisterUnitKey;
import ru.yandex.market.ff.client.dto.RequestItemDTO;
import ru.yandex.market.ff.client.dto.RequestItemErrorDTO;
import ru.yandex.market.ff.client.enums.RequestItemErrorAttributeType;
import ru.yandex.market.ff.client.enums.RequestItemErrorType;

import static org.assertj.core.api.Assertions.assertThat;

class FfwfErrorsRegisterCountSplitterTest {

    public static final String SSKU = "000001.123456";
    public static final long SUPPLIER_ID = 1234L;
    public static final String REAL_SUPPLIER_ID = "000001";
    public static final RegisterUnitKey REGISTER_UNIT_KEY = new RegisterUnitKey(SSKU, SUPPLIER_ID, REAL_SUPPLIER_ID);
    public static final UnitCount UNIT_COUNT = new UnitCount().setQuantity(100);

    @Test
    void testOk() {
        RequestItemDTO itemDTO = new RequestItemDTO();

        itemDTO.setArticle(SSKU);
        itemDTO.setRealSupplierId(REAL_SUPPLIER_ID);
        itemDTO.setSupplierId(SUPPLIER_ID);

        FfwfErrorsRegisterCountSplitter splitter = new FfwfErrorsRegisterCountSplitter(List.of(
            itemDTO
        ));

        assertThat(splitter.getDenyReason(REGISTER_UNIT_KEY, UNIT_COUNT)).isBlank();
        assertThat(splitter.getFinalNormalQuantity(REGISTER_UNIT_KEY, UNIT_COUNT)).isEqualTo(100);
    }

    @Test
    void testCountCorrection() {
        RequestItemDTO itemDTO = new RequestItemDTO();

        itemDTO.setArticle(SSKU);
        itemDTO.setRealSupplierId(REAL_SUPPLIER_ID);
        itemDTO.setSupplierId(SUPPLIER_ID);

        RequestItemErrorDTO err = new RequestItemErrorDTO();
        RequestItemErrorDTO.AttributeDTO attr = new RequestItemErrorDTO.AttributeDTO();
        attr.setType(RequestItemErrorAttributeType.CURRENTLY_ON_STOCK);
        attr.setValue("80");
        err.setType(RequestItemErrorType.NOT_ENOUGH_ON_STOCK);
        err.setAttributes(List.of(attr));
        itemDTO.setValidationErrors(List.of(err));

        FfwfErrorsRegisterCountSplitter splitter = new FfwfErrorsRegisterCountSplitter(List.of(
            itemDTO
        ));

        assertThat(splitter.getDenyReason(REGISTER_UNIT_KEY, UNIT_COUNT)).isEqualTo("NOT_ENOUGH_ON_STOCK");
        assertThat(splitter.getFinalNormalQuantity(REGISTER_UNIT_KEY, UNIT_COUNT)).isEqualTo(80);
    }

    @Test
    void testAllFailed() {
        RequestItemDTO itemDTO = new RequestItemDTO();

        itemDTO.setArticle(SSKU);
        itemDTO.setRealSupplierId(REAL_SUPPLIER_ID);
        itemDTO.setSupplierId(SUPPLIER_ID);

        RequestItemErrorDTO err = new RequestItemErrorDTO();
        err.setType(RequestItemErrorType.ASSORTMENT_SKU_NOT_FOUND);
        itemDTO.setValidationErrors(List.of(err));

        FfwfErrorsRegisterCountSplitter splitter = new FfwfErrorsRegisterCountSplitter(List.of(
            itemDTO
        ));

        assertThat(splitter.getDenyReason(REGISTER_UNIT_KEY, UNIT_COUNT)).isEqualTo("ASSORTMENT_SKU_NOT_FOUND");
        assertThat(splitter.getFinalNormalQuantity(REGISTER_UNIT_KEY, UNIT_COUNT)).isEqualTo(0);
    }

}
