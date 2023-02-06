package ru.yandex.market.wms.common.spring.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.dto.AvailablePlacementBoxesDto;
import ru.yandex.market.wms.common.model.dto.AvailablePlacementDirectionsDto;
import ru.yandex.market.wms.common.model.dto.GetSkusOnPalletResponse;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;

class BbxdDropMovingServiceTest extends IntegrationTest {

    @Autowired
    private BbxdDropMovingService service;

    @Test
    @DatabaseSetup("/db/service/bbxd-drop-moving-service/get-available-boxes-to-drop/1.xml")
    void getAvailableBoxesOnDrop1() {
        assertions.assertThatThrownBy(
                        () -> service.getAvailableBoxesOnDrop("DRP123", SkuId.of("465852", "ROV0000000000000000358")))
                .hasMessageContaining(
                        "400 BAD_REQUEST \"На дропке DRP123 нет дропнутых товаров ROV0000000000000000358 465852");
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-drop-moving-service/get-available-boxes-to-drop/2.xml")
    void getAvailableBoxesOnDrop2() {
        AvailablePlacementBoxesDto result =
                service.getAvailableBoxesOnDrop("DRP123", SkuId.of("465852", "ROV0000000000000000358"));
        assertions.assertThat(result.getQty()).isEqualTo(2);
        assertions.assertThat(result.getBoxes()).containsExactly("BOX123");
        assertions.assertThat(result.getAmount()).isEqualTo(1);
        assertions.assertThat(result.getIsFake()).isTrue();
        assertions.assertThat(result.getTitle()).isEqualTo("Товар");
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-drop-moving-service/get-available-boxes-to-drop/3.xml")
    void getAvailableBoxesOnDrop3() {
        AvailablePlacementBoxesDto result =
                service.getAvailableBoxesOnDrop("DRP123", SkuId.of("465852", "ROV0000000000000000358"));
        assertions.assertThat(result.getQty()).isEqualTo(3);
        assertions.assertThat(result.getBoxes()).contains("BOX123", "BOX456");
        assertions.assertThat(result.getAmount()).isEqualTo(2);
        assertions.assertThat(result.getIsFake()).isTrue();
        assertions.assertThat(result.getTitle()).isEqualTo("Товар");
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-drop-moving-service/get-skus-on-drop/1.xml")
    void getSkusOnDrop1() {
        GetSkusOnPalletResponse result = service.getSkusOnDrop("DRP123");
        assertions.assertThat(result.getSkus().size()).isOne();
        assertions.assertThat(result.getSkus()).anySatisfy(sku -> {
            assertions.assertThat(sku.getSku()).isEqualTo("ROV0000000000000000358");
            assertions.assertThat(sku.getStorerKey()).isEqualTo("465852");
        });
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-drop-moving-service/get-skus-on-drop/2.xml")
    void getSkusOnDrop2() {
        GetSkusOnPalletResponse result = service.getSkusOnDrop("DRP123");
        assertions.assertThat(result.getSkus().size()).isEqualTo(2);
        assertions.assertThat(result.getSkus()).anySatisfy(sku -> {
            assertions.assertThat(sku.getSku()).isEqualTo("ROV0000000000000000358");
            assertions.assertThat(sku.getStorerKey()).isEqualTo("465852");
        });
        assertions.assertThat(result.getSkus()).anySatisfy(sku -> {
            assertions.assertThat(sku.getSku()).isEqualTo("ROV0000000000000000359");
            assertions.assertThat(sku.getStorerKey()).isEqualTo("465852");
        });
    }

    @Test
    void getSkusOnDrop3() {
        assertions.assertThatThrownBy(() -> service.getSkusOnDrop("AN123123"))
                .hasMessageContaining("Тара AN123123 не является дропкой");
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-drop-moving-service/choose-drop/1.xml")
    void chooseDrop1() {
        AvailablePlacementDirectionsDto result = service.chooseDrop("DRP123");
        assertions.assertThat(result.getDropCellId()).isEqualTo("DROP_LOC");
        assertions.assertThat(result.getCarrierName()).isEqualTo("Samara");
        assertions.assertThat(result.getDropIds()).contains("DRP456");
        assertions.assertThat(result.getDropIds().size()).isOne();
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-drop-moving-service/choose-drop/2.xml")
    void chooseDrop2() {
        assertions.assertThatThrownBy(() -> service.chooseDrop("DRP123"))
                .hasMessageContaining("Дропка DRP123 находится в зоне 1 типа UNDEFINED, отличного от BBXD_SORTER");
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-drop-moving-service/choose-drop/3.xml")
    void chooseDrop3() {
        assertions.assertThatThrownBy(() -> service.chooseDrop("DRP123"))
                .hasMessageContaining("Нет доступных дропок для передроппинга с DRP123 по направлению CARRIER-01");
    }
}
