package ru.yandex.market.billing.partner.gmv;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.categories.model.CategoryWalker;
import ru.yandex.market.billing.imports.geo.RegionWalker;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Tests for {@link PartnerGmvCalculationService}
 */
@DbUnitDataSet(before = "PartnerGmvCalculationService.before.csv")
@DbUnitDataSet(after = "PartnerGmvCalculationService.after.csv")
class PartnerGmvCalculationServiceTest extends FunctionalTest {

    private static final LocalDate FEBRUARY_17_2022 = LocalDate.of(2022, 2, 17);

    @Autowired
    CategoryWalker categoryWalker;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    CategoryRegionGmvDao categoryRegionGmvDao;
    @Autowired
    RegionWalker regionWalker;
    @Autowired
    PartnerGmvDao partnerGmvDao;
    @Autowired
    TransactionTemplate pgTransactionTemplate;

    @Mock
    PartnerGmvYtDao partnerGmvYtDao;

    private PartnerGmvCalculationService partnerGmvCalculationService;

    @BeforeEach
    void setUp() {
        partnerGmvCalculationService = new PartnerGmvCalculationService(categoryRegionGmvDao, regionWalker,
                categoryWalker, partnerGmvDao, pgTransactionTemplate, partnerGmvYtDao, environmentService);
    }

    @Test
    @DisplayName("Успешный подсчёт без выгрузки в YT")
    void testSuccess() {
        partnerGmvCalculationService.process(FEBRUARY_17_2022);
        Mockito.verifyNoInteractions(partnerGmvYtDao);
    }

    @Test
    @DisplayName("Успешный подсчёт с выгрузкой в YT")
    @DbUnitDataSet(before = "PartnerGmvCalculationService.env.before.csv")
    void testSuccessStoreToYt() {
        partnerGmvCalculationService.process(FEBRUARY_17_2022);
        Mockito.verify(partnerGmvYtDao).storeGmvsToYt(eq(FEBRUARY_17_2022), anyList());
    }

}
