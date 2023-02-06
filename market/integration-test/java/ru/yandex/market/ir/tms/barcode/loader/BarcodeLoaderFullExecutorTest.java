package ru.yandex.market.ir.tms.barcode.loader;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.ir.tms.barcode.helper.YtHelper;
import ru.yandex.market.ir.tms.barcode.row.AllModelsRow;
import ru.yandex.market.ir.tms.barcode.row.BarcodeLoaderRow;
import ru.yandex.market.ir.tms.barcode.row.BarcodeStorageRow;
import ru.yandex.market.ir.tms.barcode.row.BlackListShopsRow;
import ru.yandex.market.ir.tms.barcode.row.CategoriesRow;
import ru.yandex.market.ir.tms.barcode.row.WhiteListCategoriesRow;
import ru.yandex.market.ir.tms.barcode.row.WhiteListShopsRow;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;

import static org.mockito.ArgumentMatchers.any;

public class BarcodeLoaderFullExecutorTest {
    public static final int BARCODE_PARAM_ID = 14202862;
    private static final int WHITE_CATEGORY_ID = 12;
    private static final String WHITE_DOMAIN = "white.domain";
    private static final String NOT_WHITE_DOMAIN1 = "not-white.domain1";
    private static final String NOT_WHITE_DOMAIN2 = "not-white.domain2";
    private static final String QUORUMED_BARCODE = "quorumed-barcode";
    private static final String SAME_DOMAIN_NO_QUORUM_BARCODE = "same-domain-no-quorum-barcode";
    private static final String WHITE_DOMAINED_BARCODE = "white-domained-barcode";
    @Rule
    public TestName name = new TestName();

    @Test
    public void testBarcodeLoader1() throws IOException {
        YPath testDir = YtHelper.prepareTestDir(this.getClass().getSimpleName(), name.getMethodName());

        YPath barcodeStorageDir = testDir.child("barcode-storage");
        prepareBarcodeStorageDir(barcodeStorageDir);

        YPath barcodeLoaderDir = testDir.child("barcode-loader");

        YPath mboExportDir = testDir.child("mbo-export");
        prepareCategories(mboExportDir);
        prepareAllModels(mboExportDir);

        String sessionId = "20020304_0506";
        YPath loaderSessionDir = barcodeLoaderDir.child(sessionId);
        prepareBlackAndWhiteLists(loaderSessionDir);

        BarcodeLoaderFullExecutor executor = new BarcodeLoaderFullExecutor(
            YtHelper.mockYt(),
            YtHelper.getJdbcTemplate(),
            barcodeStorageDir.toString(),
            barcodeLoaderDir.toString(),
            mboExportDir.toString()
        );
        executor = Mockito.spy(executor);

        Mockito.doReturn(false)
            .when(executor)
            .isSessionExists(any());

        Mockito.doReturn("last-barcode-session")
            .when(executor)
            .resolveRecent();

        Mockito.doReturn(sessionId)
            .when(executor)
            .createSessionId();

        executor.execute();
        doAssertFull(loaderSessionDir.child("loader-full"));
        doAssertQuorum(loaderSessionDir.child("parts").child("quorum").child("full"));
        doAssertWhite(loaderSessionDir.child("parts").child("white-list").child("full"));
    }

    private void prepareBlackAndWhiteLists(YPath loaderSessionDir) {
        YPath blackListShops = loaderSessionDir.child("black-list-shops");

        YtHelper.createTableAndWrite(
            blackListShops,
            YtHelper.readBlackListShopsAttributes(),
            BlackListShopsRow.class,
            Cf.list(new BlackListShopsRow("black.domain", "functional test"))
        );

        YPath whiteListCategories = loaderSessionDir.child("white-list-categories");
        YtHelper.createTableAndWrite(
            whiteListCategories,
            YtHelper.readWhiteListCategoriesAttributes(),
            WhiteListCategoriesRow.class,
            Cf.list(
                new WhiteListCategoriesRow("white-name", WHITE_CATEGORY_ID, "white-department")
            )
        );

        YPath whiteListShops = loaderSessionDir.child("white-list-shops");

        YtHelper.createTableAndWrite(
            whiteListShops,
            YtHelper.readWhiteListShopsAttributes(),
            WhiteListShopsRow.class,

            Cf.list(
                new WhiteListShopsRow(WHITE_DOMAIN, "functional test")
            )
        );
    }

    private void prepareCategories(YPath mboExportDir) {
        YPath path = mboExportDir.child("recent").child("categories");
        int hid = 1;
        YtHelper.createTableAndWrite(
            path,
            YtHelper.readMboCategoriesAttributes(),
            CategoriesRow.class,
            Cf.list(
                new CategoriesRow(hid, false,
                    MboParameters.Category.newBuilder().setHid(hid).setGrouped(false).build())
            )
        );
    }

    private void prepareAllModels(YPath mboExportDir) {
        YPath path = mboExportDir.child("recent").child("models").child("all_models");
        YtHelper.createEmptyTable(path, YtHelper.readMboAllModelsAttributes());
        YtHelper.write(
            path,
            Cf.list(new AllModelsRow(createModel("423"))).map(AllModelsRow::toNode)
        );
    }

    private ModelStorage.Model createModel(String barcode) {
        return ModelStorage.Model.newBuilder()
            .setId(1)
            .setCurrentType("GURU")
            .setCategoryId(2)
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(BARCODE_PARAM_ID)
                .addStrValue(ModelStorage.LocalizedString.newBuilder()
                    .setIsoCode("ru")
                    .setValue(barcode)
                    .build())
                .build())
            .build();
    }

    @SuppressWarnings("checkstyle:LineLength")
    private YPath prepareBarcodeStorageDir(YPath barcodeStorageDir) {
        YPath full = barcodeStorageDir.child("recent").child("full");
        final int millisInMicro = 1_000;
        long lastSeenTs = System.currentTimeMillis() * millisInMicro;
        int i = 1;
        YtHelper.createTableAndWrite(
            full,
            YtHelper.readStorageAttributes(),
            BarcodeStorageRow.class,
            Cf.list(
                new BarcodeStorageRow(i++, i++, i++, WHITE_DOMAINED_BARCODE, "barcode", WHITE_CATEGORY_ID, lastSeenTs, 1, WHITE_DOMAIN),
                new BarcodeStorageRow(i++, i++, i++, "no-quorum-barcode", "barcode", WHITE_CATEGORY_ID, lastSeenTs, 1, NOT_WHITE_DOMAIN1),
                new BarcodeStorageRow(i++, i++, i++, "no-quorum-barcode", "barcode", WHITE_CATEGORY_ID, lastSeenTs, 1, NOT_WHITE_DOMAIN2),

                new BarcodeStorageRow(i, 0, 0, SAME_DOMAIN_NO_QUORUM_BARCODE, "barcode", WHITE_CATEGORY_ID, lastSeenTs, 1, NOT_WHITE_DOMAIN2),
                new BarcodeStorageRow(i++, 0, 0, SAME_DOMAIN_NO_QUORUM_BARCODE, "barcode", WHITE_CATEGORY_ID, lastSeenTs, 1, NOT_WHITE_DOMAIN2),

                new BarcodeStorageRow(i, 0, 0, QUORUMED_BARCODE, "barcode", WHITE_CATEGORY_ID, lastSeenTs, 1, NOT_WHITE_DOMAIN1),
                new BarcodeStorageRow(i, 0, 0, QUORUMED_BARCODE, "barcode", WHITE_CATEGORY_ID, lastSeenTs, 1, NOT_WHITE_DOMAIN2)
            )
        );
        return full;
    }

    private void doAssertFull(YPath path) {
        Assertions.assertThat(readBarcodes(path))
            .containsExactlyInAnyOrder(
                QUORUMED_BARCODE,
                SAME_DOMAIN_NO_QUORUM_BARCODE,
                WHITE_DOMAINED_BARCODE
            );
    }

    private void doAssertQuorum(YPath path) {
        Assertions.assertThat(readBarcodes(path))
            .containsExactlyInAnyOrder(
                QUORUMED_BARCODE
            );
    }

    private void doAssertWhite(YPath path) {
        Assertions.assertThat(readBarcodes(path))
            .containsExactlyInAnyOrder(
                WHITE_DOMAINED_BARCODE
            );
    }

    private List<String> readBarcodes(YPath path) {
        return YtHelper.read(path, BarcodeLoaderRow.class).stream()
            .map(BarcodeLoaderRow::getBarcode)
            .collect(Collectors.toList());
    }

}
