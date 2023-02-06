package ru.yandex.market.mboc.tms.executors.mdm;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.app.proto.MasterDataServiceMock;
import ru.yandex.market.mboc.app.proto.SupplierDocumentServiceMock;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.parsing.utils.GeobaseCountry;
import ru.yandex.market.mboc.common.masterdata.parsing.utils.GeobaseCountryUtil;
import ru.yandex.market.mboc.common.mdm.MdmCountryGeoIds;
import ru.yandex.market.mboc.common.mdm.MdmCountryRepository;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

/**
 * @author amaslak
 */
public class ImportManufacturerCountryExecutorTest extends BaseDbTestClass {

    private static final Logger log = LoggerFactory.getLogger(ImportManufacturerCountryExecutorTest.class);

    private static final int SEED = 123123;

    @Autowired
    private MdmCountryRepository mdmCountryRepository;

    private MasterDataServiceMock masterDataService;
    private MasterDataHelperService masterDataHelperService;
    private EnhancedRandom defaultRandom;
    private StorageKeyValueServiceMock keyValueService;
    private ImportManufacturerCountryExecutor executor;

    @Before
    public void setUp() throws Exception {
        defaultRandom = TestDataUtils.defaultRandom(SEED);
        keyValueService = new StorageKeyValueServiceMock();
        masterDataService = Mockito.spy(new MasterDataServiceMock());
        SupplierConverterServiceMock supplierConverterService = new SupplierConverterServiceMock();
        SupplierDocumentServiceMock supplierDocumentServiceMock =
            Mockito.spy(new SupplierDocumentServiceMock(masterDataService));
        masterDataHelperService = new MasterDataHelperService(
            masterDataService, supplierDocumentServiceMock, supplierConverterService, storageKeyValueService);

        executor = new ImportManufacturerCountryExecutor(mdmCountryRepository, masterDataService, keyValueService);
    }

    @Test
    public void whenUpdatedMasterDataShouldImportCountries() {
        Assertions.assertThat(mdmCountryRepository.findAll()).isEmpty();

        keyValueService.putValue(
            ImportManufacturerCountryExecutor.MDM_COUNTRIES_IMPORT_STAMP_KEY,
            ImportManufacturerCountryExecutor.INITIAL_STAMP
        );
        List<MasterData> testData = TestDataUtils.generateSskuMsterData(10, defaultRandom);
        testData.get(0).setManufacturerCountries(List.of("Россия", "Нигерия", "Остров Норфолк"));
        testData.get(1).setManufacturerCountries(List.of("РФ", "Республика Гаити"));

        masterDataHelperService.saveSskuMasterDataAndDocuments(testData);

        executor.execute();

        Long afterStamp = keyValueService.getLong(
            ImportManufacturerCountryExecutor.MDM_COUNTRIES_IMPORT_STAMP_KEY,
            ImportManufacturerCountryExecutor.INITIAL_STAMP
        );

        Assertions.assertThat(afterStamp).isEqualTo(testData.size());

        List<MdmCountryGeoIds> mdmCountryGeoIdsList = mdmCountryRepository.findAll();
        Map<ShopSkuKey, MdmCountryGeoIds> geoIdsMap = mdmCountryGeoIdsList.stream().collect(Collectors.toMap(
            m -> new ShopSkuKey(m.getSupplierId(), m.getShopSku()), Function.identity()
        ));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(mdmCountryGeoIdsList).hasSize(testData.size());

            s.assertThat(geoIdsMap.get(testData.get(0).getShopSkuKey()).getGeoIds()).containsExactly(225, 20741, 98539);
            s.assertThat(geoIdsMap.get(testData.get(1).getShopSkuKey()).getGeoIds()).containsExactly(225, 21321);

            for (MasterData masterData : testData) {
                MdmCountryGeoIds mdmCountryGeoIds = geoIdsMap.get(masterData.getShopSkuKey());
                s.assertThat(mdmCountryGeoIds).isNotNull();
                //noinspection ConstantConditions
                if (mdmCountryGeoIds != null) {
                    List<Integer> geoIds = mdmCountryGeoIds.getGeoIds();
                    List<Integer> expectedGeoIds = masterData.getManufacturerCountries().stream()
                        .map(GeobaseCountryUtil::countryByName)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .map(GeobaseCountry::getGeoId)
                        .collect(Collectors.toList());
                    s.assertThat(geoIds).containsExactlyElementsOf(expectedGeoIds);
                }
            }
        });
    }
}
