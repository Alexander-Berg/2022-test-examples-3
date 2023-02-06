package ru.yandex.market.core.partner;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.campaign.model.PartnerId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Date: 24.11.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class UnitedDataCampPartnerServiceImplTest extends FunctionalTest {

    private static final long SUPPLIER_ID = 774L;
    private static final long BUSINESS_ID = 1774L;

    @Autowired
    private UnitedDataCampPartnerService unitedDataCampPartnerService;

    @ParameterizedTest(name = "Проверка на нахождение партера в ЕОХ: type = {0}; result = {2}")
    @DbUnitDataSet(before = "UnitedDataCampPartnerService.testIsUnitedPartner.before.csv")
    @CsvSource({
            "SHOP,664,false",
            "SHOP,665,true",
            "SUPPLIER,666,false",
            "SUPPLIER,667,true",
            "SUPPLIER,668,false",
            "SHOP,669,true"
    })
    void testIsUnitedPartner(CampaignType type, long id, boolean result) {
        PartnerId partnerId = PartnerId.partnerId(id, type);
        Assertions.assertThat(unitedDataCampPartnerService.isUnitedPartner(partnerId))
                .isEqualTo(result);
    }

    @Test
    @DisplayName("Для партнера без Единого каталога метод вернет идентификатор партнера по указанному типу кампании")
    @DbUnitDataSet(before = "UnitedDataCampPartnerServiceImplTest.getUnitedCatalogOrDefaultPartnerId.csv")
    void testGetPartnerIdNonUnitedCatalog() {
        PartnerId partnerId = PartnerId.supplierId(SUPPLIER_ID);
        assertEquals(SUPPLIER_ID, unitedDataCampPartnerService.getUnitedCatalogOrPartnerId(partnerId));
    }

    @Test
    @DisplayName("Для партнера с Единым каталогом метод вернет идентификатор привязанного к нему бизнеса")
    @DbUnitDataSet(before = {
            "UnitedDataCampPartnerServiceImplTest.getUnitedCatalogOrDefaultPartnerId.csv",
            "UnitedDataCampPartnerServiceImplTest.getUnitedCatalogOrDefaultPartnerId.businessForPartner.csv",
            "UnitedDataCampPartnerServiceImplTest.getUnitedCatalogOrDefaultPartnerId.unitedCatalogForPartner.csv"
    })
    void testGetPartnerIdUnitedCatalog() {
        PartnerId partnerId = PartnerId.supplierId(SUPPLIER_ID);
        assertEquals(BUSINESS_ID, unitedDataCampPartnerService.getUnitedCatalogOrPartnerId(partnerId));
    }

    @Test
    @DisplayName("Если к партнеру с включенным Единым каталогом не привязан бизнес, метод выбросит исключение")
    @DbUnitDataSet(before = {
            "UnitedDataCampPartnerServiceImplTest.getUnitedCatalogOrDefaultPartnerId.csv",
            "UnitedDataCampPartnerServiceImplTest.getUnitedCatalogOrDefaultPartnerId.unitedCatalogForPartner.csv"
    })
    void testGetPartnerIdNoBusinessFound() {
        PartnerId partnerIdWithWrongCampaignType = PartnerId.supplierId(SUPPLIER_ID);
        assertThrows(
                NullPointerException.class,
                () -> unitedDataCampPartnerService.getUnitedCatalogOrPartnerId(partnerIdWithWrongCampaignType),
                String.format("Partner %d has United catalog enabled but got no business", SUPPLIER_ID)
        );
    }
}
