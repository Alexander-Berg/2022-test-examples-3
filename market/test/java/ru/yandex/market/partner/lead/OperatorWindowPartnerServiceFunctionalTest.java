package ru.yandex.market.partner.lead;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Функциональные тесты для {@link OperatorWindowPartnerService}.
 */
@DbUnitDataSet(before = "OperatorWindowPartnerServiceFunctionalTest.csv")
public class OperatorWindowPartnerServiceFunctionalTest extends FunctionalTest {
    @Autowired
    OperatorWindowPartnerService operatorWindowPartnerService;

    @Test
    void testGetSupplier() {
        OperatorWindowSupplierDTO expected = OperatorWindowSupplierDTO.builder()
                .setSupplierId(100L)
                .setCampaignId(9100L)
                .setBusinessId(1000L)
                .setTitle("поставщик1")
                .setSalesSum("0.00")
                .setOrdersCount(0)
                .setStockRemainsCount(0)
                .setStockSkuRemainsCount(0)
                .setType("type")
                .setClientId(8000L)
                .setSuperAdminUid(10L)
                .setLogin("login1")
                .setUsesPapi(false)
                .setCreatedAt(LocalDateTime.of(2021, 3, 21, 0, 0)
                        .atZone(ZoneId.systemDefault()).toInstant())
                .build();

        OperatorWindowSupplierDTO operatorWindowSupplierDTO =
                operatorWindowPartnerService.buildOperatorWindowSupplierDTO(100L, "type");

        assertThat(operatorWindowSupplierDTO).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void testGetSupplierCrossdock() {
        OperatorWindowSupplierDTO expected = OperatorWindowSupplierDTO.builder()
                .setSupplierId(400L)
                .setCampaignId(9400L)
                .setBusinessId(2000L)
                .setTitle("поставщик4")
                .setSalesSum("0.00")
                .setOrdersCount(0)
                .setStockRemainsCount(0)
                .setStockSkuRemainsCount(0)
                .setType("type")
                .setClientId(9000L)
                .setSuperAdminUid(20L)
                .setLogin("login2")
                .setUsesPapi(true)
                .setCreatedAt(LocalDateTime.of(2021, 3, 21, 0, 0)
                        .atZone(ZoneId.systemDefault()).toInstant())
                .build();

        OperatorWindowSupplierDTO operatorWindowSupplierDTO =
                operatorWindowPartnerService.buildOperatorWindowSupplierDTO(400L, "type");

        assertThat(operatorWindowSupplierDTO).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void testGetShop() {
        OperatorWindowShopDTO expected = OperatorWindowShopDTO.builder()
                .setShopId(200L)
                .setBusinessId(2000L)
                .setCampaignId(9200L)
                .setClientId(1L)
                .setDomain(new OperatorWindowDomainDTO("domain.ru", "domain.ru"))
                .setRegion("Москва")
                .setTitle("магазин")
                .setCreatedAt(LocalDateTime.of(2021, 5, 28, 0, 0)
                        .atZone(ZoneId.systemDefault()).toInstant())
                .setSuperAdminUid(20L)
                .setSuperAdminLogin("login2")
                .setUsesPartnerApi(true)
                .build();

        OperatorWindowShopDTO operatorWindowShopDTO =
                operatorWindowPartnerService.buildOperatorWindowShopDTO(200L);

        assertThat(operatorWindowShopDTO).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void testGetDistributionTypes() {
        assertThat(operatorWindowPartnerService.getDistributionTypes(100L)).hasSameElementsAs(
                List.of(OperatorWindowPartnerDistributionTypeEnum.FBY)
        );
        assertThat(operatorWindowPartnerService.getDistributionTypes(400L)).hasSameElementsAs(
                List.of(OperatorWindowPartnerDistributionTypeEnum.FBY,
                        OperatorWindowPartnerDistributionTypeEnum.FBY_PLUS)
        );
        assertThat(operatorWindowPartnerService.getDistributionTypes(500L)).hasSameElementsAs(
                List.of(OperatorWindowPartnerDistributionTypeEnum.FBS)
        );
    }
}
