package ru.yandex.market.mbo.tms.erp.dao;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Streams;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.bolts.collection.impl.DefaultIteratorF;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.erp.dao.ErpSkuMapper;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.synchronizer.export.LoggingExportRegistry;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.BaseCategoryModelsExtractorTestClass;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.constants.Models;
import ru.yandex.market.mbo.synchronizer.export.registry.RegistryWorkerTemplate;
import ru.yandex.market.mbo.yt.TestYtWrapper;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tests of {@link ErpSkuDumpService}.
 *
 * @author s-ermakov
 */
public class ErpSkuDumpServiceTest extends BaseCategoryModelsExtractorTestClass {

    private ErpSkuDumpService erpSkuDumpService;
    private YPath inputSkuYPath;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.inputSkuYPath = YPath.simple("//home/model-storage/export/recent/models/sku");

        this.erpSkuDumpService = new ErpSkuDumpService(ytWrapper, ytExportModelsTableService, tovarTreeService,
            ytExportMRService, autoUser, ytWrapper.pool(), null, inputSkuYPath);
    }

    @Test
    public void testFreshExport() {
        RegistryWorkerTemplate<?> workerTemplate = RegistryWorkerTemplate.newRegistryWorker(
            new LoggingExportRegistry(ErpSkuDumpServiceTest.class.getName())
        );

        // arrange
        List<ModelStorage.Model> models = Arrays.asList(
            // first group
            Models.M1, Models.SKU1_1, Models.SKU1_2, Models.SKU_MODIF_2,
            Models.MODIF1, Models.MODIF2,
            Models.SKU_MODIF_11, Models.SKU_MODIF_12,
            // second
            Models.M2,
            // third
            Models.M3,
            // fourth
            Models.C1,
            // fifth
            Models.PARTNER1, Models.PARTNER_SKU1_1, Models.PARTNER_SKU1_2,
            Models.PARTNER2,
            Models.PARTNER3, Models.PARTNER_SKU3_1
        );
        ytWrapper.createModelTable(tablePath, models);

        // act
        YPath skuYPath = allModelsExportPath.child("sku_raw");
        erpSkuDumpService.dumpSkusFromModelStorage(workerTemplate, skuYPath);

        // assert
        List<ErpSku> actual = Streams.stream(ytWrapper.tables().read(skuYPath, YTableEntryTypes.YSON))
            .map(ErpSku::new)
            .collect(Collectors.toList());

        Assertions.assertThat(actual)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new ErpSku(Models.SKU1_1_ENRICHED),
                new ErpSku(Models.SKU1_2_ENRICHED),
                new ErpSku(Models.SKU_MODIF_2_ENRICHED),
                new ErpSku(Models.SKU_MODIF_11_ENRICHED),
                new ErpSku(Models.SKU_MODIF_12_ENRICHED),
                new ErpSku(Models.M3_SKU_ENRICHED),
                new ErpSku(Models.PARTNER_SKU1_1_ENRICHED),
                new ErpSku(Models.PARTNER_SKU1_2_ENRICHED),
                new ErpSku(Models.PARTNER_SKU3_1_ENRICHED)
            );
    }

    @Test
    public void testExportFromStuffExtractor() {
        // arrange
        List<ModelStorage.Model> skus = Arrays.asList(
            Models.SKU1_1_ENRICHED, Models.SKU1_2_ENRICHED, Models.SKU_MODIF_2_ENRICHED, Models.SKU_MODIF_11_ENRICHED,
            Models.SKU_MODIF_12_ENRICHED, Models.M3_SKU_ENRICHED,
            Models.PARTNER_SKU1_1_ENRICHED, Models.PARTNER_SKU1_2_ENRICHED
        );
        List<YTreeMapNode> entries = TestYtWrapper.wrapToEntries(skus, true);
        ytWrapper.tables().write(inputSkuYPath, YTableEntryTypes.YSON, DefaultIteratorF.wrap(entries.iterator()));

        // act
        YPath skuYPath = allModelsExportPath.child("sku_raw");
        erpSkuDumpService.dumpSkusFromStuffExtractor(skuYPath);

        // assert
        List<ErpSku> actual = Streams.stream(ytWrapper.tables().read(skuYPath, YTableEntryTypes.YSON))
            .map(ErpSku::new)
            .collect(Collectors.toList());

        Assertions.assertThat(actual)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new ErpSku(Models.SKU1_1_ENRICHED),
                new ErpSku(Models.SKU1_2_ENRICHED),
                new ErpSku(Models.SKU_MODIF_2_ENRICHED),
                new ErpSku(Models.SKU_MODIF_11_ENRICHED),
                new ErpSku(Models.SKU_MODIF_12_ENRICHED),
                new ErpSku(Models.M3_SKU_ENRICHED),
                new ErpSku(Models.PARTNER_SKU1_1_ENRICHED),
                new ErpSku(Models.PARTNER_SKU1_2_ENRICHED)
            );
    }

    /**
     * Этот тест похож на {@link ErpSkuDumpServiceTest#testExportFromStuffExtractor()},
     * но сначала он запускает stuff выгрузку, а потом запускает erp выгрузку на данных, полученных из stuff выгрузки.
     * То есть максимальный end-to-end тест, для того, чтобы убедиться, что изменения в stuff выгрузке не поломают erp.
     */
    @Test
    public void testExportFromStuffExtractorComplex() throws Exception {
        // arrange
        List<ModelStorage.Model> models = Arrays.asList(
            // first group
            Models.M1, Models.SKU1_1, Models.SKU1_2, Models.SKU_MODIF_2,
            Models.MODIF1, Models.MODIF2,
            Models.SKU_MODIF_11, Models.SKU_MODIF_12,
            // second
            Models.M2,
            // third
            Models.M3,
            // fourth
            Models.C1,
            // fifth
            Models.PARTNER1, Models.PARTNER_SKU1_1, Models.PARTNER_SKU1_2,
            Models.PARTNER2,
            Models.PARTNER3, Models.PARTNER_SKU3_1
        );
        ytWrapper.createModelTable(tablePath, models);

        // run stuff extractor
        extractor.perform("");
        assertNoFailedFiles(registry);

        // run erp extractor
        inputSkuYPath = allModelsExportPath.child(registry.getFolderName()).child("models").child("sku");
        erpSkuDumpService = new ErpSkuDumpService(ytWrapper, ytExportModelsTableService,
            tovarTreeService, ytExportMRService, autoUser, ytWrapper.pool(), null, inputSkuYPath);
        YPath skuYPath = allModelsExportPath.child("sku_raw");
        erpSkuDumpService.dumpSkusFromStuffExtractor(skuYPath);

        // assert
        List<ErpSku> actual = Streams.stream(ytWrapper.tables().read(skuYPath, YTableEntryTypes.YSON))
            .map(ErpSku::new)
            .collect(Collectors.toList());

        Assertions.assertThat(actual)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new ErpSku(Models.SKU1_1_ENRICHED),
                new ErpSku(Models.SKU1_2_ENRICHED),
                new ErpSku(Models.SKU_MODIF_2_ENRICHED),
                new ErpSku(Models.SKU_MODIF_11_ENRICHED),
                new ErpSku(Models.SKU_MODIF_12_ENRICHED),
                new ErpSku(Models.M3_SKU_ENRICHED),
                new ErpSku(Models.PARTNER_SKU1_1_ENRICHED),
                new ErpSku(Models.PARTNER_SKU1_2_ENRICHED),
                new ErpSku(Models.PARTNER_SKU3_1_ENRICHED)
            );
    }

    public static class ErpSku {
        private long msku;
        private String name;
        private String type;
        private boolean deleted;
        private boolean published;
        private long modifiedTs;
        private long categoryId;
        private long vendorId;
        private String rawVendorName;
        private String vendorCode;
        private String barcodes;
        private String pictureUrl;
        private String grossWidth;
        private String grossLength;
        private String grossDepth;
        private String grossWeight;

        ErpSku(YTreeMapNode entry) {
            msku = entry.getLong(ErpSkuMapper.OutputFields.MSKU);
            name = entry.getString(ErpSkuMapper.OutputFields.NAME);
            type = entry.getString(ErpSkuMapper.OutputFields.TYPE);
            deleted = entry.getBool(ErpSkuMapper.OutputFields.DELETED);
            published = entry.getBool(ErpSkuMapper.OutputFields.PUBLISHED);
            modifiedTs = entry.getLong(ErpSkuMapper.OutputFields.MODIFIED_TS);
            categoryId = entry.getLong(ErpSkuMapper.OutputFields.CATEGORY_ID);
            vendorId = entry.getLong(ErpSkuMapper.OutputFields.VENDOR_ID);

            if (entry.containsKey(ErpSkuMapper.OutputFields.RAW_VENDOR_NAME)) {
                rawVendorName = entry.getString(ErpSkuMapper.OutputFields.RAW_VENDOR_NAME);
            }
            if (entry.containsKey(ErpSkuMapper.OutputFields.VENDOR_CODE)) {
                vendorCode = entry.getString(ErpSkuMapper.OutputFields.VENDOR_CODE);
            }
            if (entry.containsKey(ErpSkuMapper.OutputFields.BARCODES)) {
                barcodes = entry.getString(ErpSkuMapper.OutputFields.BARCODES);
            }
            if (entry.containsKey(ErpSkuMapper.OutputFields.PICTURE_URL)) {
                pictureUrl = entry.getString(ErpSkuMapper.OutputFields.PICTURE_URL);
            }
            if (entry.containsKey(ErpSkuMapper.OutputFields.GROSS_WIDTH)) {
                grossWidth = entry.getString(ErpSkuMapper.OutputFields.GROSS_WIDTH);
            }
            if (entry.containsKey(ErpSkuMapper.OutputFields.GROSS_LENGTH)) {
                grossLength = entry.getString(ErpSkuMapper.OutputFields.GROSS_LENGTH);
            }
            if (entry.containsKey(ErpSkuMapper.OutputFields.GROSS_DEPTH)) {
                grossDepth = entry.getString(ErpSkuMapper.OutputFields.GROSS_DEPTH);
            }
            if (entry.containsKey(ErpSkuMapper.OutputFields.GROSS_WEIGHT)) {
                grossWeight = entry.getString(ErpSkuMapper.OutputFields.GROSS_WEIGHT);
            }
        }

        ErpSku(ModelStorage.Model model) {
            this(ErpSkuMapper.mapModel(model));
        }

        @Override
        public String toString() {
            return "ErpSku{" +
                "msku=" + msku +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", deleted=" + deleted +
                ", published=" + published +
                ", modifiedTs=" + modifiedTs +
                ", categoryId=" + categoryId +
                ", vendorId=" + vendorId +
                ", rawVendorName=" + rawVendorName +
                ", vendorCode='" + vendorCode + '\'' +
                ", pictureUrl='" + pictureUrl + '\'' +
                ", grossWidth='" + grossWidth + '\'' +
                ", grossLength='" + grossLength + '\'' +
                ", grossDepth='" + grossDepth + '\'' +
                ", grossWeight='" + grossWeight + '\'' +
                ", barcodes='" + barcodes + '\'' +
                '}';
        }
    }
}
