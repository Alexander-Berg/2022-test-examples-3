package ru.yandex.market.mbisfintegration.importer.mbi;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbisfintegration.converters.impl.mbi.ShopConverter;
import ru.yandex.market.mbisfintegration.datapreparation.impl.AccountPreparationService;
import ru.yandex.market.mbisfintegration.generated.sf.model.Account;
import ru.yandex.market.mbisfintegration.generated.sf.model.RecordType;
import ru.yandex.market.mbisfintegration.importer.ImportConfiguration;
import ru.yandex.market.mbisfintegration.salesforce.SfDate;

import static ru.yandex.market.mbisfintegration.importer.mbi.ImportEntityType.SHOP;

class ShopImportTest extends AbstractMbiImportTest {

    private static final long IMPORTED_SHOP_ID = 637936L;

    @Autowired
    AccountPreparationService accountPreparationService;

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        entityClass = Account.class;
        entityType = SHOP;
        converter = new ShopConverter();
        importConfig = new ImportConfiguration(entityClass, "uri", "shop_id", entityType);
    }

    @Test
    void testImportNewShop() {
        doImport(accountPreparationService, "classpath:/import.sources/shop_crm_export.xml");
        Assertions.assertThat(this.<Account>findEntityData(IMPORTED_SHOP_ID))
                .isEqualTo(
                        new Account()
                                .withName("TEST-STORE")
                                .withShopIDC(637936.0)
                                .withClientIDC(85521155.0)
                                .withDistributionSchemeC("DBS")
                                .withAllDistributionSchemesC("DBS - FAIL")
                                .withWebsite("test-store.ru")
                                .withCompanyIDC("21641793")
                                .withOwnerStaffLoginC("i-familia")
                                .withRecordType(new RecordType().withName("Shop"))
                                .withMBIRegionC("Москва")
                                .withFirstRegistrationDateC(SfDate.parseIso("2022-01-01T13:30:11"))
                );
    }
}
