package ru.yandex.market.mbisfintegration.importer.mbi;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbisfintegration.converters.impl.mbi.SupplierConverter;
import ru.yandex.market.mbisfintegration.datapreparation.impl.AccountPreparationService;
import ru.yandex.market.mbisfintegration.generated.sf.model.Account;
import ru.yandex.market.mbisfintegration.generated.sf.model.RecordType;
import ru.yandex.market.mbisfintegration.importer.ImportConfiguration;
import ru.yandex.market.mbisfintegration.salesforce.SfDate;

import static ru.yandex.market.mbisfintegration.importer.mbi.ImportEntityType.SUPPLIER;

/**
 * Тесты импорта синих магазинов из MBI (от xml до таблички entities в БД)
 * TODO дописать подобные интеграционные тесты для контактов и белых
 * TODO дописать тесты на другие сценарии (например когда в бд уже есть запись) + на граничные условия
 */
class SupplierImportTest extends AbstractMbiImportTest {

    private static final long IMPORTED_SUPPLIER_ID = 637936L;

    @Autowired
    AccountPreparationService accountPreparationService;

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        entityClass = Account.class;
        entityType = SUPPLIER;
        converter = new SupplierConverter();
        importConfig = new ImportConfiguration(entityClass, "uri", "supplier_id", entityType);
    }

    @Test
    void testImportNewSupplier() {
        doImport(accountPreparationService, "classpath:/import.sources/supplier_crm_export.xml");
        Assertions.assertThat(this.<Account>findEntityData(IMPORTED_SUPPLIER_ID))
                .isEqualTo(
                        new Account()
                                .withName("TEST-STORE")
                                .withSupplierIDC(637936.0)
                                .withClientIDC(85521155.0)
                                .withExpressC(false)
                                .withModelIsSelectedC(true)
                                .withLEIsFilledC(true)
                                .withCatalogIsUploadedC(true)
                                .withReleasedC(true)
                                //Первая схема из приоритетных
                                .withDistributionSchemeC("FBS")
                                //Схемы отсортированы в порядке приоритета
                                .withAllDistributionSchemesC("FBS - CONFIGURE;DBS - TESTED;FBY+ - SUCCESS")
                                .withWebsite("test-store.ru")
                                .withBusinessIDC("915906")
                                .withCompanyIDC("21641793")
                                .withOwnerStaffLoginC("i-familia")
                                .withRecordType(new RecordType().withName("Supplier"))
                                .withGoodsCategoryInternalC("16033856")
                                .withFirstRegistrationDateC(SfDate.parseIso("2022-01-01T13:30:11"))
                );
    }

    @Test
    void testIgnoreOnboardingStates() {
        doImport(
                accountPreparationService,
                "classpath:/import.sources/supplier_crm_export_ignored_onboarding_states.xml"
        );
        Assertions.assertThat(this.<Account>findEntityData(IMPORTED_SUPPLIER_ID))
                .isEqualTo(
                        new Account()
                                .withName("TEST-STORE")
                                .withSupplierIDC(637936.0)
                                .withClientIDC(85521155.0)
                                .withExpressC(false)
                                .withDistributionSchemeC("FBS")
                                .withAllDistributionSchemesC("FBS - SUCCESS")

                                //Проставляем шаги воронки, тк у партнера схема размещения в success
                                .withModelIsSelectedC(true)
                                .withLEIsFilledC(true)
                                .withCatalogIsUploadedC(true)
                                .withReleasedC(true)

                                .withWebsite("test-store.ru")
                                .withBusinessIDC("915906")
                                .withCompanyIDC("21641793")
                                .withOwnerStaffLoginC("i-familia")
                                .withRecordType(new RecordType().withName("Supplier"))

                                .withGoodsCategoryInternalC("16033856")
                                .withFirstRegistrationDateC(SfDate.parseIso("2022-01-01T13:30:11"))
                );
    }
}
