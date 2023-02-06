package ru.yandex.market.mbisfintegration.importer.yt;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.impl.ytree.YTreeDoubleNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.market.mbisfintegration.converters.impl.yt.SupplierPartnerRatingYtConverter;
import ru.yandex.market.mbisfintegration.datapreparation.impl.YtAccountPreparationService;
import ru.yandex.market.mbisfintegration.entity.Entity;
import ru.yandex.market.mbisfintegration.generated.sf.model.Account;
import ru.yandex.market.mbisfintegration.generated.sf.model.RecordType;
import ru.yandex.market.mbisfintegration.importer.ImportConfiguration;
import ru.yandex.yt.ytclient.proxy.YtClient;

import static ru.yandex.market.mbisfintegration.importer.mbi.ImportEntityType.SUPPLIER;

public class SupplierPartnerRatingYtImportTest extends AbstractYtImportTest {

    private static final long IMPORTED_SUPPLIER_ID = 637936L;

    @Autowired
    YtAccountPreparationService ytAccountPreparationService;

    @Autowired
    YtClient ytClientMock;

    @Autowired
    YtMockHelper ytMockHelper;

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        entityClass = Account.class;
        entityType = SUPPLIER;
        converter = new SupplierPartnerRatingYtConverter();
        importConfig = new ImportConfiguration(entityClass, "uri", "partner_id", entityType);

        ytMockHelper.mockSelectSingleRow(
                "active_order_limit, partner_id, rating from " +
                        "[//home/market/production/b2b-crm/dictionaries/partner_rating/partner_rating]",
                "active_order_limit", new YTreeDoubleNodeImpl(12.345, Map.of()),
                "partner_id", new YTreeIntegerNodeImpl(true, IMPORTED_SUPPLIER_ID, Map.of()),
                "rating", new YTreeDoubleNodeImpl(67.890, Map.of())
        );

        ytImporter = new SupplierPartnerRatingYtImport(ytClientMock, configurationService, ytAccountPreparationService);
    }

    @Test
    void readAndConvertOneRow() {
        entityService.add(
                new Entity(IMPORTED_SUPPLIER_ID,
                        entityType,
                        null,
                        new Account().withSupplierIDC((double) IMPORTED_SUPPLIER_ID)
                )
        );
        ytImporter.doImport(converter);
        Assertions.assertThat(this.<Account>findEntityData(IMPORTED_SUPPLIER_ID))
                .isEqualTo(
                        new Account()
                                .withSupplierIDC((double) IMPORTED_SUPPLIER_ID)
                                .withQualityIndexC(68.0)
                                .withOrdersLimitC(12.345)
                                .withRecordType(new RecordType().withName("Supplier"))
                );
    }
}
