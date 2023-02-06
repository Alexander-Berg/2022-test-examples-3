package ru.yandex.market.ir.tms.barcode.storage;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.Mockito;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.ir.tms.barcode.helper.YtHelper;
import ru.yandex.market.ir.tms.barcode.row.AllModelsRow;
import ru.yandex.market.ir.tms.barcode.row.BarcodeStorageRow;
import ru.yandex.market.ir.tms.barcode.row.CategoriesRow;
import ru.yandex.market.ir.tms.barcode.row.OfferRow;
import ru.yandex.market.ir.tms.barcode.row.ParsedSiteRow;
import ru.yandex.market.ir.tms.barcode.row.SuperControllerRow;
import ru.yandex.market.ir.tms.barcode.row.SupplierRow;
import ru.yandex.market.ir.tms.barcode.row.ValidationResultRow;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.any;

public class BarcodeStorageFullExecutorTest {
    private static final String BARCODE_1 = "4607058031664";
    private static final String BARCODE_2 = "4607059021664";
    private static final String BARCODE_3 = "4670003110691";
    private static final String BARCODE_4 = "4670004100691";
    private static final String BARCODE_4_WS = "467 000410 0691"; // good barcode, whitespaces should be removed
    private static final String BARCODE_5 = "0000000001922";
    private static final String BAD_BARCODE = "OZ1223-4344-4534";

    private static final String OFFER_BARCODE_1 = "000000008587";
    private static final String OFFER_BARCODE_2 = "0000000120326";
    private static final String UNUSED_BARCODE_1 = "0000000129633";
    private static final long SUPPLIER_ID_1 = 8934423L;
    private static final String BLUE_DOMAIN_1 = "anysome.com";

    private static final long OK_HID = 1;
    private static final long OK_MODEL_ID = 678;
    private static final long OK_MODIFICATION_ID = 789;
    private static final long OK_SKU_ID = 123;
    private static final long UNUSED_ID = 99999;

    private static final String WHITELIST_RAW_DOMAIN = "ledpremium.tiu.ru";

    @Rule
    public TestName name = new TestName();

    @Test
    public void testBarcodeStorage1() throws IOException {
        YPath testDir = YtHelper.prepareTestDir(this.getClass().getSimpleName(), name.getMethodName());
        YPath scDir = testDir.child("supercontroller");
        YPath parsedSitesDir = testDir.child("parsed-sites");
        prepareScAndParsedSites(scDir, parsedSitesDir);

        YPath barcodesDir = testDir.child("barcode-storage");
        YPath emptyBarcodesTable = barcodesDir.child("recent").child("full");
        prepareEmptyBarcodesTable(emptyBarcodesTable);

        YPath mboExportDir = testDir.child("mbo-export");
        prepareCategories(mboExportDir);
        prepareModels(mboExportDir);
        prepareSkus(mboExportDir);

        YPath mstatDictionariesDir = testDir.child("mstat-dictionaries");
        prepareBlueOffers(mstatDictionariesDir);
        prepareSuppliers(mstatDictionariesDir);

        BarcodeStorageFullExecutor executor = new BarcodeStorageFullExecutor(
            YtHelper.mockYt(),
            YtHelper.getJdbcTemplate(),
            scDir.toString(),
            barcodesDir.toString(),
            mboExportDir.toString(),
            mstatDictionariesDir.toString(),
            parsedSitesDir.toString()
        );
        executor = Mockito.spy(executor);
        Mockito.doReturn(false)
            .when(executor)
            .isSessionExists(any());
        Mockito.doReturn("non-existent-sc-session")
            .when(executor)
            .getLastOkScSession();
        String newSessionId = "new-session";
        Mockito.doReturn(newSessionId)
            .when(executor)
            .createSessionId();
        Mockito.doReturn(parsedSitesDir)
            .when(executor)
            .getLastUnprocessedParsedSitesSessionOrEmptyDir();

        executor.execute();
        doAssert(barcodesDir.child(newSessionId).child("full"));
        doAssertValidationResult(parsedSitesDir.child("validation_errors"));
    }

    private void doAssert(YPath tablePath) {
        ListF<BarcodeStorageRow> readResult = YtHelper.read(tablePath, BarcodeStorageRow.class);
        List<String> barcodes = readResult.stream()
            .map(BarcodeStorageRow::getBarcode)
            .collect(Collectors.toList());
        Assertions.assertThat(barcodes)
            .containsExactlyInAnyOrder(
                BARCODE_1,
                BARCODE_2,
                BARCODE_3,
                BARCODE_4,
                BARCODE_5,
                OFFER_BARCODE_1,
                OFFER_BARCODE_2
            );
        List<String> domains = readResult.stream()
            .map(BarcodeStorageRow::getDomain)
            .collect(Collectors.toList());
        Assertions.assertThat(domains)
            .containsExactlyInAnyOrder(
                WHITELIST_RAW_DOMAIN,
                "domain.one",
                "domain.one",
                // and from sc table:
                "http",
                "http",
                BLUE_DOMAIN_1,
                BLUE_DOMAIN_1
            );
    }

    private void doAssertValidationResult(YPath tablePath) {
        List<String> badBarcodes = YtHelper.read(tablePath, ValidationResultRow.class).stream()
            .map(ValidationResultRow::getBarcode)
            .collect(Collectors.toList());
        Assertions.assertThat(badBarcodes)
            .containsExactlyInAnyOrder(
                BARCODE_3 + "|" + BARCODE_4,
                BAD_BARCODE,
                BARCODE_1
            );
    }

    private void prepareEmptyBarcodesTable(YPath barcodesTable) {
        YtHelper.createEmptyTable(barcodesTable, YtHelper.readStorageAttributes());
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private void prepareScAndParsedSites(YPath scSessionDir, YPath parsedSitesDir) {
        YPath mboOffersMr = scSessionDir.child("recent").child("mbo_offers_mr");
        YtHelper.createTableAndWrite(mboOffersMr, YtHelper.readScAttributes(), SuperControllerRow.class,
            Cf.list(new SuperControllerRow(
                1,
                "http",
                2,
                3,
                4,
                BARCODE_1 + "|" + BARCODE_2,
                5,
                23, // BARCODE_MATCH
                3, // PUBLISHED_MODIFICATION
                "anything_but_corrective_matching"
            ))
        );


        YPath parsedSitesTable = parsedSitesDir.child("the_table");
        YtHelper.createTableAndWrite(parsedSitesTable, YtHelper.readParsedSitesAttributes(), ParsedSiteRow.class,
            Cf.list(
                // invalid row (bad hid and model, sku, modification ids)
                new ParsedSiteRow(
                    BARCODE_3 + "|" + BARCODE_4,
                    "vendor",
                    "ledpremium.tiu.ru", // whitelist domain, should not be shrank
                    10,
                    11,
                    12,
                    13
                ),
                // valid row
                new ParsedSiteRow(
                    BARCODE_3 + "|" + BARCODE_4_WS,
                    "vendor",
                    "ru.domain.one", // should be shrank to level 2
                    OK_MODEL_ID,
                    0,
                    0,
                    OK_HID
                ),
                // valid row
                new ParsedSiteRow(
                    BARCODE_5,
                    "vendor",
                    WHITELIST_RAW_DOMAIN, // whitelist domain, should not be shrank to level 2
                    OK_MODEL_ID,
                    0,
                    0,
                    OK_HID
                ),
                // invalid row (bad barcode, vendor and domain)
                new ParsedSiteRow(
                    BAD_BARCODE,
                    "badvendor",
                    null,
                    OK_MODEL_ID,
                    0,
                    0,
                    OK_HID
                ),
                // invalid row (bad hid, domain)
                new ParsedSiteRow(
                    BARCODE_1,
                    "vendor",
                    "bad domain",
                    OK_MODEL_ID,
                    OK_MODIFICATION_ID,
                    OK_SKU_ID,
                    0
                )
            )
        );
    }

    private void prepareCategories(YPath mboExportDir) {
        YPath path = mboExportDir.child("recent").child("categories");
        YtHelper.createTableAndWrite(
            path,
            YtHelper.readMboCategoriesAttributes(),
            CategoriesRow.class,
            Cf.list(
                new CategoriesRow(OK_HID, true, MboParameters.Category.newBuilder().setHid(OK_HID)
                    .setLeaf(true).setGrouped(false).build())
            )
        );
    }

    private void prepareModels(YPath mboExportDir) {
        YPath path = mboExportDir.child("recent").child("models").child("models");
        YtHelper.createEmptyTable(path, YtHelper.readMboAllModelsAttributes());
        YtHelper.write(
            path,
            Cf.list(
                new AllModelsRow(createModel(OK_MODEL_ID, UNUSED_ID)),
                new AllModelsRow(createModel(UNUSED_ID, OK_MODIFICATION_ID))
            ).map(AllModelsRow::toNode)
        );
    }

    private void prepareSkus(YPath mboExportDir) {
        YPath path = mboExportDir.child("recent").child("models").child("sku");
        YtHelper.createEmptyTable(path, YtHelper.readMboAllModelsAttributes());
        YtHelper.write(
            path,
            Cf.list(new AllModelsRow(createModel(OK_SKU_ID, UNUSED_ID))).map(AllModelsRow::toNode)
        );
    }

    public static ModelStorage.Model createModel(long id, long parentId) {
        return ModelStorage.Model.newBuilder()
            .setId(id)
            .setCurrentType("GURU")
            .setCategoryId(2)
            .setParentId(parentId)
            .build();
    }

    private void prepareBlueOffers(YPath mstatDictionariesDir) {
        YPath path = mstatDictionariesDir.child("mbo").child("mboc_offers").child("latest");
        YtHelper.createTableAndWrite(
            path,
            YtHelper.readMstatMbocOffersAttributes(),
            OfferRow.class,
            Cf.list(
                new OfferRow(OK_HID, SUPPLIER_ID_1, OK_SKU_ID,
                    String.format("%s,%s", OFFER_BARCODE_1, OFFER_BARCODE_2)),
                new OfferRow(OK_HID, SUPPLIER_ID_1, UNUSED_ID, UNUSED_BARCODE_1),
                new OfferRow(OK_HID, SUPPLIER_ID_1, 0, UNUSED_BARCODE_1)
            )
        );
    }

    private void prepareSuppliers(YPath mstatDictionariesDir) {
        YPath path = mstatDictionariesDir.child("suppliers").child("latest");
        YtHelper.createTableAndWrite(
            path,
            YtHelper.readMstatSuppliersAttributes(),
            SupplierRow.class,
            Cf.list(
                new SupplierRow(SUPPLIER_ID_1, BLUE_DOMAIN_1)
            )
        );
    }


}

