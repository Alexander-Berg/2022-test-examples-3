package ru.yandex.market.core.datacamp;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Функциональные тесты для {@link UnitedCatalogOffersCountService}.
 */
@DbUnitDataSet(before = "unitedCatalogOffersCountTest.before.csv")
public class UnitedCatalogOffersCountServiceTest extends FunctionalTest {

    private static final long SUPPLIER_ID = 774L;

    @Autowired
    private UnitedCatalogOffersCountService unitedCatalogOffersCountService;

    @Autowired
    private SaasService saasService;

    @Test
    @DisplayName("Для партнера с Единым каталогом метод вернет идентификатор привязанного к нему бизнеса")
    void testGetPartnerIdUnitedCatalogOffers() {
        int offersAmount = 120;
        SaasSearchResult resultMock = SaasSearchResult.builder()
                .setOffers(List.of())
                .setTotalCount(offersAmount)
                .build();
        when(saasService.searchBusinessOffers(any()))
                .thenReturn(resultMock);
        PartnerId partnerId = PartnerId.supplierId(SUPPLIER_ID);
        assertEquals(offersAmount, unitedCatalogOffersCountService.getNumberOfUnitedOffersForBusinessByPartner(partnerId));
        assertEquals(offersAmount, unitedCatalogOffersCountService.getNumberOfUnitedOffersForPartner(partnerId));
    }
}
