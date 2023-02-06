package ru.yandex.market.mboc.app.proto;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.DocumentProtoConverter;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.MbocBaseProtoConverter;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mdm.http.MdmCommon;
import ru.yandex.market.mdm.http.MdmDocument;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author moskovkin@yandex-team.ru
 * @since 01.07.19
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class MasterDataHelperServiceTest {
    private static final int COUNT = 17;
    private static final int SEED = 42;
    private static final int BERU_ID = SupplierConverterServiceMock.BERU_ID;
    private static final int INTERNAL_SUPPLIER_ID = 113;

    private MasterDataServiceMock masterDataServiceMock;
    private SupplierDocumentServiceMock supplierDocumentServiceMock;
    private MasterDataHelperService masterDataHelperService;
    private EnhancedRandom defaultRandom;
    private SupplierConverterServiceMock supplierConverterService;
    private StorageKeyValueService storageKeyValueService;

    @Before
    public void setup() {
        defaultRandom = TestDataUtils.defaultRandom(SEED);
        supplierConverterService = new SupplierConverterServiceMock();
        masterDataServiceMock = Mockito.spy(new MasterDataServiceMock());
        supplierDocumentServiceMock = Mockito.spy(new SupplierDocumentServiceMock(masterDataServiceMock));
        storageKeyValueService = new StorageKeyValueServiceMock();
        masterDataHelperService = new MasterDataHelperService(masterDataServiceMock, supplierDocumentServiceMock,
                supplierConverterService, storageKeyValueService);
    }

    @Test
    public void whenSupplyCorrectSskuMasterDataThenSave() {
        List<MasterData> testData = TestDataUtils.generateSskuMsterData(COUNT, defaultRandom);
        testSave(testData);
    }

    @Test
    public void whenSupply1PSskuMasterDataThenSaveWithAppropriateConversionsBackAndForth() {
        List<MasterData> testData = TestDataUtils.generateSskuMsterData(COUNT, defaultRandom);
        // для половины МДшек заменим ключи на якобы 1Ршные
        for (int i = 0; i < COUNT / 2; ++i) {
            MasterData md = testData.get(i);
            ShopSkuKey internalKey = new ShopSkuKey(INTERNAL_SUPPLIER_ID, md.getShopSku());
            ShopSkuKey externalKey = new ShopSkuKey(BERU_ID, "00646." + internalKey.getShopSku());
            md.setShopSkuKey(internalKey);
            supplierConverterService.addInternalToExternalMapping(internalKey, externalKey);
        }

        // ...и в целом всё остальное должно работать 100% аналогично обычному сценарию из теста выше.
        testSave(testData);
    }

    @Test
    public void whenSearchSskuMasterDataThenFindCorrect() {
        List<MasterData> testData = TestDataUtils.generateSskuMsterData(COUNT, defaultRandom);

        // metadata is not preserved on save
        // will be fixed if https://st.yandex-team.ru/MBO-20816
        for (MasterData masterData : testData) {
            for (QualityDocument document : masterData.getQualityDocuments()) {
                document.setMetadata(new QualityDocument.Metadata());
            }
        }
        masterDataHelperService.saveSskuMasterDataAndDocuments(testData);

        List<MasterData> dataToSearch = Lists.partition(testData, 2).get(0);
        List<ShopSkuKey> keysToSearch = dataToSearch.stream()
            .map(MasterData::getShopSkuKey)
            .collect(Collectors.toList());

        List<MasterData> found = masterDataHelperService.findSskuMasterData(keysToSearch);

        Assertions.assertThat(found)
            .usingElementComparatorIgnoringFields("modifiedTimestamp", "heavyGood", "categoryId",
                "itemShippingUnit", "nonItemShippingUnits", "preciousGood", "goldenItemShippingUnit", "goldenRsl",
                "surplusHandleMode", "cisHandleMode", "heavyGood20", "regNumbers", "datacampMasterDataVersion",
                "traceable")
            .containsOnlyElementsOf(dataToSearch);
    }

    @Test
    public void masterDataRequestsAreBatched() {
        storageKeyValueService.invalidateCache();
        storageKeyValueService.putValue(MasterDataHelperService.SEARCH_BATCH_SIZE_KEY, 10);
        List<MasterData> testData = TestDataUtils.generateSskuMsterData(COUNT, defaultRandom);

        // metadata is not preserved on save
        // will be fixed if https://st.yandex-team.ru/MBO-20816
        for (MasterData masterData : testData) {
            for (QualityDocument document : masterData.getQualityDocuments()) {
                document.setMetadata(new QualityDocument.Metadata());
            }
        }
        masterDataHelperService.saveSskuMasterDataAndDocuments(testData);

        List<ShopSkuKey> keysToSearch = testData.stream()
            .map(MasterData::getShopSkuKey)
            .collect(Collectors.toList());

        List<MasterData> found = masterDataHelperService.findSskuMasterData(keysToSearch);

        Assertions.assertThat(found)
            .usingElementComparatorIgnoringFields("modifiedTimestamp", "heavyGood", "categoryId",
                "itemShippingUnit", "nonItemShippingUnits", "preciousGood", "goldenItemShippingUnit", "goldenRsl",
                "surplusHandleMode", "cisHandleMode", "heavyGood20", "regNumbers", "datacampMasterDataVersion",
                "traceable")
            .containsOnlyElementsOf(testData);

        verify(masterDataServiceMock, times(2)).searchSskuMasterData(any());
        verify(supplierDocumentServiceMock, times(2)).findSupplierDocumentsByShopSku(any());
    }

    private void testSave(List<MasterData> testData) {
        masterDataHelperService.saveSskuMasterDataAndDocuments(testData);

        List<MdmCommon.SskuMasterData> expectedSskuMasterData = testData.stream()
            .map(MbocBaseProtoConverter::pojoToProto)
            .collect(Collectors.toList());
        Assertions.assertThat(masterDataServiceMock.getConvertedToInternalMD(supplierConverterService))
            .containsOnlyElementsOf(expectedSskuMasterData);

        List<MdmDocument.Document> expectedDocuments = testData.stream()
            .flatMap(d -> d.getQualityDocuments().stream())
            .map(DocumentProtoConverter::createProtoDocument)
            .collect(Collectors.toList());
        Assertions.assertThat(supplierDocumentServiceMock.getMdmDocuments().values())
            .containsOnlyElementsOf(expectedDocuments);

        List<MdmDocument.DocumentOfferRelation> expectedRelations = testData.stream()
            .flatMap(md -> md.getQualityDocuments().stream().map(r ->
                MdmDocument.DocumentOfferRelation.newBuilder()
                    .setRegistrationNumber(r.getRegistrationNumber())
                    .setSupplierId(md.getSupplierId())
                    .setShopSku(md.getShopSku())
                    .build()
            ))
            .collect(Collectors.toList());
        Assertions.assertThat(supplierDocumentServiceMock.getConvertedToInternalRelations(supplierConverterService))
            .containsOnlyElementsOf(expectedRelations);
    }
}

