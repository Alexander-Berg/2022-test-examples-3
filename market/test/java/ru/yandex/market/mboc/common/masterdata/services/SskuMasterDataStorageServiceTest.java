package ru.yandex.market.mboc.common.masterdata.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.application.monitoring.MonitoringUnit;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataFilter;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepositoryMock;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static ru.yandex.market.mboc.common.masterdata.services.SskuMasterDataStorageService.MONITORING_NAME;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class SskuMasterDataStorageServiceTest {
    private static final long SEED = 70482206L;
    private SskuMasterDataStorageService sskuMasterDataStorageService;
    private MasterDataRepository masterDataRepository;
    private SupplierConverterServiceMock supplierConverterService;
    private EnhancedRandom random;
    private ComplexMonitoring monitoring;

    @Before
    public void setup() {
        masterDataRepository = new MasterDataRepositoryMock();
        supplierConverterService = new SupplierConverterServiceMock();
        monitoring = new ComplexMonitoring();
        sskuMasterDataStorageService = new SskuMasterDataStorageService(masterDataRepository, null,
            null, supplierConverterService, monitoring);
        random = TestDataUtils.defaultRandom(SEED);
    }

    @Test
    public void whenInsertOrUpdateShouldDuplicate1P() {
        ShopSkuKey internalKey1 = new ShopSkuKey(23, "vpdmjsrkatlrmwn");
        ShopSkuKey internalKey2 = new ShopSkuKey(32, "pemflsdpteekq");
        ShopSkuKey externalKey = new ShopSkuKey(41, "pwjvnslqaltjenvptbybaoq");
        supplierConverterService.addInternalToExternalMapping(internalKey1, externalKey);

        MasterData md1 = TestDataUtils.generateMasterData(internalKey1, random);
        MasterData md2 = TestDataUtils.generateMasterData(internalKey2, random);
        sskuMasterDataStorageService.insertOrUpdateAll(List.of(md1, md2));

        Assertions.assertThat(masterDataRepository.totalCount()).isEqualTo(3);
        MasterData byInternal1 = masterDataRepository.findById(internalKey1);
        MasterData byInternal2 = masterDataRepository.findById(internalKey2);
        MasterData byExternal = masterDataRepository.findById(externalKey);
        byExternal.setShopSkuKey(internalKey1);

        Assertions.assertThat(md1).isEqualTo(byInternal1).isEqualTo(byExternal);
        Assertions.assertThat(md2).isEqualTo(byInternal2);
    }

    @Test
    public void whenSskuInBothFormatsIsInInsertListShouldNotTriplicateIt() {
        masterDataRepository = Mockito.spy(masterDataRepository);
        sskuMasterDataStorageService = new SskuMasterDataStorageService(masterDataRepository,
            null, null, supplierConverterService, new ComplexMonitoring());

        List<MasterData> actualMasterDataPassedToSave = new ArrayList<>();
        Mockito.when(masterDataRepository.insertOrUpdateAll(Mockito.anyList()))
            .thenAnswer((Answer<List<MasterData>>) invocation -> {
                List<MasterData> invocationResult = (List<MasterData>) invocation.callRealMethod();
                actualMasterDataPassedToSave.addAll(invocationResult);
                return invocationResult;
            });

        ShopSkuKey internalKey1 = new ShopSkuKey(23, "vpdmjsrkatlrmwn");
        ShopSkuKey internalKey2 = new ShopSkuKey(32, "pemflsdpteekq");
        ShopSkuKey externalKey = new ShopSkuKey(41, "pwjvnslqaltjenvptbybaoq");
        supplierConverterService.addInternalToExternalMapping(internalKey1, externalKey);

        MasterData md1 = TestDataUtils.generateMasterData(internalKey1, random);
        MasterData md2 = TestDataUtils.generateMasterData(internalKey2, random);
        MasterData md1ext = new MasterData();
        md1ext.copyDataFieldsFrom(md1);
        md1ext.setShopSkuKey(externalKey);
        sskuMasterDataStorageService.insertOrUpdateAll(List.of(md1, md2, md1ext));

        Assertions.assertThat(actualMasterDataPassedToSave).hasSize(3);
        Assertions.assertThat(actualMasterDataPassedToSave).containsExactlyInAnyOrder(md1, md1ext, md2);
    }

    @Test
    public void whenInternalKeysEncounteredShouldRiseMonitoringError() {
        ShopSkuKey internalKey = new ShopSkuKey(23, "vpdmjsrkatlrmwn");
        ShopSkuKey externalKey = new ShopSkuKey(41, "pwjvnslqaltjenvptbybaoq");
        supplierConverterService.addInternalToExternalMapping(internalKey, externalKey);

        MasterData md = TestDataUtils.generateMasterData(internalKey, random);
        sskuMasterDataStorageService.insertOrUpdateAll(List.of(md));
        MonitoringUnit unit = monitoring.getOrCreateUnit(MONITORING_NAME);
        Assertions.assertThat(unit.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        Assertions.assertThat(unit.getMessage()).isEqualTo("MasterData with internal keys found, " +
            "see log for stacktrace");
    }

    @Test
    public void testBatchWithoutInternalsIterator() {
        LocalDateTime modifiedAfter = LocalDateTime.of(1, 1, 1, 1, 1, 1);
        List<MasterData> expectedMasterData = prepareMasterDataTableForIteratorTests();
        List<MasterData> iterationResult = new ArrayList<>();
        List<Integer> batchSizes = new ArrayList<>();
        Iterator<List<MasterData>> batchIterator =
            sskuMasterDataStorageService.batchIterator(
                new MasterDataFilter().setModifiedAfter(modifiedAfter), 2
            );

        batchIterator.forEachRemaining(batch -> {
            iterationResult.addAll(batch);
            batchSizes.add(batch.size());
        });

        Assertions.assertThat(iterationResult.stream().map(MasterData::getShopSkuKey)).containsExactlyElementsOf(
            expectedMasterData.stream()
                .filter(masterData -> masterData.getModifiedTimestamp().isAfter(modifiedAfter))
                .sorted(Comparator.comparing(MasterData::getShopSkuKey))
                .map(MasterData::getShopSkuKey)
                .collect(Collectors.toList())
        );
        Assertions.assertThat(batchSizes).containsExactly(1, 0, 1, 2);
    }

    @Test
    public void testModificationTimeBatchIterator(){
        LocalDateTime modifiedAfter = LocalDateTime.of(1, 1, 1, 1, 1, 1);
        List<MasterData> expectedMasterData = prepareMasterDataTableForIteratorTests();
        List<MasterData> iterationResult = new ArrayList<>();
        List<Integer> batchSizes = new ArrayList<>();
        Iterator<List<MasterData>> batchIterator =
            sskuMasterDataStorageService.modificationTimeBatchIterator(modifiedAfter, 2);

        batchIterator.forEachRemaining(batch -> {
            iterationResult.addAll(batch);
            batchSizes.add(batch.size());
        });

        Assertions.assertThat(iterationResult).containsExactlyElementsOf(
            expectedMasterData.stream()
                .sorted(Comparator.comparing(MasterData::getModifiedTimestamp).thenComparing(MasterData::getShopSkuKey))
                .collect(Collectors.toList())
        );
        Assertions.assertThat(batchSizes).containsExactly(1, 1, 1, 1);
    }

    List<MasterData> prepareMasterDataTableForIteratorTests() {
        List<ShopSkuKey> internalKeys = List.of(
            new ShopSkuKey(12, "something5677"),
            new ShopSkuKey(14, "something9087"),
            new ShopSkuKey(16, "something8745"),
            new ShopSkuKey(16, "something1243")
        );
        List<ShopSkuKey> externalKeys = List.of(
            new ShopSkuKey(41, "something4324"),
            new ShopSkuKey(41, "something342"),
            new ShopSkuKey(3, "something8076"),
            new ShopSkuKey(9078, "somethingMore")
        );
        List<MasterData> internalKeysMasterData = internalKeys.stream()
            .map(shopSkuKey -> TestDataUtils.generateMasterData(shopSkuKey, random))
            .collect(Collectors.toList());
        IntStream.range(0, 4).forEach(
            i -> internalKeysMasterData.get(i).setModifiedTimestamp(LocalDateTime.of(100 + i, 1, 1, 1, 1))
        );
        List<MasterData> externalKeysMasterData = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            supplierConverterService.addInternalToExternalMapping(internalKeys.get(i), externalKeys.get(i));
            MasterData externalKeyMasterData = new MasterData();
            externalKeyMasterData.copyDataFieldsFrom(internalKeysMasterData.get(i));
            externalKeyMasterData.setShopSkuKey(externalKeys.get(i));
            externalKeysMasterData.add(externalKeyMasterData);
        }
        masterDataRepository.insertOrUpdateAll(internalKeysMasterData);
        masterDataRepository.insertOrUpdateAll(externalKeysMasterData);

        List<MasterData> oldModified = Stream.of(new ShopSkuKey(123, "oldModified1"), new ShopSkuKey(223, "oldModified2"))
            .map(shopSkuKey -> TestDataUtils.generateMasterData(shopSkuKey, random))
            .collect(Collectors.toList());
        oldModified.forEach(md -> md.setModifiedTimestamp(LocalDateTime.MIN));
        masterDataRepository.insertOrUpdateAll(oldModified);
        return externalKeysMasterData;
    }
}
