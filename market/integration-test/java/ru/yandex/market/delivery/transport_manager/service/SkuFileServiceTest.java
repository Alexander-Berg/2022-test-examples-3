package ru.yandex.market.delivery.transport_manager.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.exception.AdminValidationException;
import ru.yandex.market.delivery.transport_manager.model.dto.StockKeepingUnitDto;
import ru.yandex.market.delivery.transport_manager.model.enums.CountType;
import ru.yandex.market.delivery.transport_manager.service.interwarehouse.transportation_task.SkuFileService;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class SkuFileServiceTest extends AbstractContextualTest {

    @Autowired
    private SkuFileService skuFileService;

    @Test
    void testSingleGroup() {
        byte[] bytes = getFile("defect_register.csv");

        Map<String, List<StockKeepingUnitDto>> units = skuFileService.convertFile(bytes, null);
        softly.assertThat(units.get(null)).containsExactlyInAnyOrder(
            new StockKeepingUnitDto()
                .setSsku("a1")
                .setCount(11)
                .setCountType(CountType.DEFECT)
                .setSupplierId("b1")
                .setRealSupplierId("c1"),
            new StockKeepingUnitDto()
                .setSsku("a2")
                .setCount(1)
                .setCountType(CountType.DEFECT)
                .setSupplierId("b2")
                .setRealSupplierId("c2"),
            new StockKeepingUnitDto()
                .setSsku("a3")
                .setCount(1)
                .setCountType(CountType.DEFECT)
                .setSupplierId("b3")
                .setRealSupplierId(null),
            new StockKeepingUnitDto()
                .setSsku("a4")
                .setCount(14)
                .setCountType(CountType.DEFECT)
                .setSupplierId("b4")
                .setRealSupplierId(null)
        );
    }

    @Test
    void testMultipleGroups() {
        byte[] bytes = getFile("defect_register_multiple_groups.csv");

        Map<String, List<StockKeepingUnitDto>> units = skuFileService.convertFile(bytes, null);
        softly.assertThat(units.get("100")).containsExactlyInAnyOrder(
            new StockKeepingUnitDto()
                .setSsku("a1")
                .setCount(11)
                .setCountType(CountType.DEFECT)
                .setSupplierId("b1")
                .setRealSupplierId("c1")
                .setCsvFileGroupingKey("100")
        );
        softly.assertThat(units.get("200")).containsExactlyInAnyOrder(
            new StockKeepingUnitDto()
                .setSsku("a2")
                .setCount(1)
                .setCountType(CountType.DEFECT)
                .setSupplierId("b2")
                .setRealSupplierId("c2")
                .setCsvFileGroupingKey("200"),
            new StockKeepingUnitDto()
                .setSsku("a3")
                .setCount(1)
                .setCountType(CountType.DEFECT)
                .setSupplierId("b3")
                .setRealSupplierId(null)
                .setCsvFileGroupingKey("200"),
            new StockKeepingUnitDto()
                .setSsku("a4")
                .setCount(14)
                .setCountType(CountType.DEFECT)
                .setSupplierId("b4")
                .setRealSupplierId(null)
                .setCsvFileGroupingKey("200")
        );
    }

    @Test
    void emptyRegister() {
        byte[] bytes = getFile("empty_register.csv");
        softly.assertThatThrownBy(() -> skuFileService.convertFile(bytes, null))
            .isInstanceOf(AdminValidationException.class)
            .hasMessage("Пустой реестр");
    }

    @Test
    void zeroCountRegister() {
        byte[] bytes = getFile("zero_count.csv");
        softly.assertThatThrownBy(() -> skuFileService.convertFile(bytes, null))
            .isInstanceOf(AdminValidationException.class)
            .hasMessage("Некоторые ssku с нулевым кол-вом");
    }

    @Test
    void missingCountRegister() {
        byte[] bytes = getFile("missing_count.csv");
        softly.assertThatThrownBy(() -> skuFileService.convertFile(bytes, null))
            .isInstanceOf(AdminValidationException.class)
            .hasMessage("Некоторые ssku с нулевым кол-вом");
    }

    @Test
    void emptySkus() {
        byte[] bytes = getFile("absent_ssku.csv");
        softly.assertThatThrownBy(() -> skuFileService.convertFile(bytes, null))
            .isInstanceOf(AdminValidationException.class)
            .hasMessage("Отсутствуют некоторые ssku");
    }

    @Test
    void emptySupplierIds() {
        byte[] bytes = getFile("absent_supplier_id.csv");
        softly.assertThatThrownBy(() -> skuFileService.convertFile(bytes, null))
            .isInstanceOf(AdminValidationException.class)
            .hasMessage("У некоторых ssku отсутствует supplierId");
    }

    @Test
    void successParseSskuWithComma() {
        byte[] bytes = getFile("ssku_with_comma.csv");
        softly.assertThat(
            skuFileService.convertFile(bytes, null).values().stream()
                .flatMap(Collection::stream)
                .toList()
        ).hasSize(2);
    }

    private byte[] getFile(String name) {
        return extractFileContent(String.format("controller/admin/transportation_task/%s", name)).getBytes();
    }
}
