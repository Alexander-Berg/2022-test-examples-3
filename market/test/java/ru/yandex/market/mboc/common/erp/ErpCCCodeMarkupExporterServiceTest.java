package ru.yandex.market.mboc.common.erp;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmModificationInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.ErpExportDataProvider;
import ru.yandex.market.mboc.common.erp.model.ErpCCCodeMarkupChange;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.model.cccode.Cis;
import ru.yandex.market.mboc.common.masterdata.parsing.utils.GeobaseCountry;
import ru.yandex.market.mboc.common.masterdata.parsing.utils.GeobaseCountryUtil;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.http.MboMappings;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class ErpCCCodeMarkupExporterServiceTest extends MdmBaseDbTestClass {
    private static final int SEED = 129847;
    @Autowired
    private AlreadySentDataFilterService alreadySentDataFilterService;
    @Autowired
    private MasterDataRepository masterDataRepository;
    @Autowired
    private BeruId beruId;
    @Autowired
    private MskuRepository mdmMskuRepository;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private ErpExportDataProvider erpExportDataProvider;
    @Autowired
    private ReferenceItemRepository referenceItemRepository;

    private EnhancedRandom enhancedRandom;
    private MboMappings.ApprovedMappingInfo mapping;
    private ErpExportService erpExportService;
    private ErpCCCodeMarkupExporterRepository erpCCCodeMarkupExporterRepository;

    @Before
    public void setUp() throws Exception {
        Random r = new Random(SEED);
        enhancedRandom = TestDataUtils.defaultRandom(r.nextLong());
        mapping = addOfferToDB();
        erpCCCodeMarkupExporterRepository = new ErpCCCodeMarkupExporterRepositoryMock();
        ErpCCCodeMarkupExporterService erpCCCodeMarkupExporter = new ErpCCCodeMarkupExporterService(
            erpCCCodeMarkupExporterRepository,
            alreadySentDataFilterService
        );
        erpExportService = new ErpExportServiceImpl(
            erpExportDataProvider,
            erpCCCodeMarkupExporter,
            Mockito.mock(ErpShippingUnitExporterService.class),
            Mockito.mock(ErpLogisticsMasterDataExporterService.class),
            beruId,
            mappingsCacheRepository
        );
        MdmSupplier beruSupplier = new MdmSupplier()
            .setId(beruId.getId())
            .setType(MdmSupplierType.FIRST_PARTY);
        mdmSupplierRepository.insert(beruSupplier);
    }

    @Test
    public void whenThereIsNoMasterDataInsertWithEmptyHSCodeAndEmptyManufacturerCountries() {
        erpCCCodeMarkupExporterRepository.insertCCCodeMarkupChanges(
            List.of(
                generateErpCCCodeMarkupChange(),
                generateErpCCCodeMarkupChange(),
                generateErpCCCodeMarkupChange())
        );

        MskuParamValue mskuParamValue1 = (MskuParamValue) generateMskuParamValue()
            .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX);
        MskuParamValue mskuParamValue2 = (MskuParamValue) new MskuParamValue().setMskuId(mskuParamValue1.getMskuId())
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_DISTINCT).setBool(true);
        MskuParamValue mskuParamValue3 = (MskuParamValue) new MskuParamValue().setMskuId(mskuParamValue1.getMskuId())
            .setMdmParamId(KnownMdmParams.IS_TRACEABLE).setBool(true);
        var msku = new CommonMsku(
            mskuParamValue1.getMskuId(),
            List.of(mskuParamValue1, mskuParamValue2, mskuParamValue3)
        );
        mdmMskuRepository.insertOrUpdateMsku(msku);

        MappingCacheDao mappingCache = new MappingCacheDao()
            .setMskuId(mskuParamValue1.getMskuId())
            .setSupplierId(beruId.getId())
            .setShopSku("Hakuna matata");
        mappingsCacheRepository.insert(mappingCache);

        erpExportService.exportCCCodeMarkupsByMskuIds(List.of(msku.getMskuId()), true);

        List<ErpCCCodeMarkupChange> erpRows = erpCCCodeMarkupExporterRepository.findAll();
        assertThat(erpRows).hasSize(4);
        ErpCCCodeMarkupChange change = erpRows.get(3);

        assertThat(change.getShopSku()).isEqualTo("Hakuna matata");
        assertThat(change.getHSCode()).isEqualTo("");
        assertThat(change.getHonestSignStatus()).isEqualTo(Cis.DISTINCT);
        assertThat(change.getTraceable()).isEqualTo(true);
        assertThat(change.getManufacturerCountries()).isEmpty();
        assertThat(change.getPrefixHSCode()).isEqualTo(mskuParamValue1.getString().orElseThrow());
    }

    @Test
    public void whenHSCodeIsNullInsertWithEmptyHSCode() {
        MskuParamValue mskuParamValue1 = (MskuParamValue) generateMskuParamValue()
            .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
            .setModificationInfo(new MdmModificationInfo().setUpdatedTs(Instant.now()));
        MskuParamValue mskuParamValue2 = (MskuParamValue) new MskuParamValue().setMskuId(mskuParamValue1.getMskuId())
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_DISTINCT).setBool(true)
            .setModificationInfo(new MdmModificationInfo().setUpdatedTs(Instant.now()));
        MskuParamValue mskuParamValue3 = (MskuParamValue) new MskuParamValue().setMskuId(mskuParamValue1.getMskuId())
            .setMdmParamId(KnownMdmParams.IS_TRACEABLE).setBool(true);
        var msku = new CommonMsku(
            mskuParamValue1.getMskuId(),
            List.of(mskuParamValue1, mskuParamValue2, mskuParamValue3)
        );
        mdmMskuRepository.insertOrUpdateMsku(msku);

        erpCCCodeMarkupExporterRepository.insertCCCodeMarkupChanges(List.of(
            generateErpCCCodeMarkupChange(),
            generateErpCCCodeMarkupChange(),
            generateErpCCCodeMarkupChange()
        ));
        MasterData masterData = generateMasterData(generateDocument()).setCustomsCommodityCode(null);

        masterDataRepository.insertOrUpdateAll(Collections.singletonList(masterData));

        MappingCacheDao mappingCache = new MappingCacheDao()
            .setMskuId(mskuParamValue1.getMskuId())
            .setSupplierId(beruId.getId())
            .setShopSku(masterData.getShopSku());
        mappingsCacheRepository.insert(mappingCache);

        erpExportService.exportCCCodeMarkups(List.of(masterData.getShopSkuKey()), true);

        List<ErpCCCodeMarkupChange> erpRows = erpCCCodeMarkupExporterRepository.findAll();
        assertThat(erpRows).hasSize(4);
        ErpCCCodeMarkupChange change = erpRows.get(3);

        assertThat(change.getShopSku()).isEqualTo(masterData.getShopSku());
        assertThat(change.getPrefixHSCode()).isEqualTo(mskuParamValue1.getString().orElseThrow());
        assertThat(change.getHonestSignStatus()).isEqualTo(Cis.DISTINCT);
        assertThat(change.getTraceable()).isEqualTo(true);
        assertThat(change.getHSCode()).isEqualTo("");
    }

    @Test
    public void whenThereIsNoParamInsertWithEmptyPrefixHSCodeAndNoneStatus() {
        erpCCCodeMarkupExporterRepository.insertCCCodeMarkupChanges(
            List.of(generateErpCCCodeMarkupChange(), generateErpCCCodeMarkupChange(),
                generateErpCCCodeMarkupChange())
        );

        MasterData masterData = generateMasterData(generateDocument());
        masterDataRepository.insertOrUpdate(masterData);

        MappingCacheDao mappingCache = new MappingCacheDao()
            .setMskuId(1234L)
            .setSupplierId(beruId.getId())
            .setShopSku(masterData.getShopSku());
        mappingsCacheRepository.insert(mappingCache);

        erpExportService.exportCCCodeMarkups(List.of(masterData.getShopSkuKey()), true);

        List<ErpCCCodeMarkupChange> erpRows = erpCCCodeMarkupExporterRepository.findAll();
        assertThat(erpRows).hasSize(4);
        ErpCCCodeMarkupChange change = erpRows.get(3);

        assertThat(change.getShopSku()).isEqualTo(masterData.getShopSku());
        assertThat(change.getHSCode()).isEqualTo(masterData.getCustomsCommodityCode());
        assertThat(change.getHonestSignStatus()).isEqualTo(Cis.NONE);
        assertThat(change.getPrefixHSCode()).isEqualTo("");
    }

    @Test
    public void ifEmptyManufacturerCountriesReturnEmpty() {
        String customCommCode = "kek";
        MskuParamValue mskuParamValue1 = (MskuParamValue) generateMskuParamValue()
            .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
            .setModificationInfo(new MdmModificationInfo().setUpdatedTs(Instant.now()))
            .setString(customCommCode);
        var msku = new CommonMsku(
            mskuParamValue1.getMskuId(),
            List.of(mskuParamValue1)
        );
        mdmMskuRepository.insertOrUpdateMsku(msku);

        erpCCCodeMarkupExporterRepository.insertCCCodeMarkupChanges(
            List.of(generateErpCCCodeMarkupChange(), generateErpCCCodeMarkupChange(),
                generateErpCCCodeMarkupChange())
        );

        MasterData masterData = generateMasterData(generateDocument()).setCustomsCommodityCode(null);
        masterData.setManufacturerCountries(Collections.emptyList());
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(masterData));

        MappingCacheDao mappingCache = new MappingCacheDao()
            .setMskuId(mskuParamValue1.getMskuId())
            .setSupplierId(beruId.getId())
            .setShopSku(masterData.getShopSku());
        mappingsCacheRepository.insert(mappingCache);

        erpExportService.exportCCCodeMarkupsByMskuIds(List.of(msku.getMskuId()), true);

        List<ErpCCCodeMarkupChange> erpRows = erpCCCodeMarkupExporterRepository.findAll();
        assertThat(erpRows).hasSize(4);
        ErpCCCodeMarkupChange change = erpRows.get(3);

        assertThat(change.getShopSku()).isEqualTo(masterData.getShopSku());
        assertThat(change.getManufacturerCountries()).isEmpty();
    }

    @Test
    public void testSendingOnly1PSskusMarkups() {
        String customCommCodePrefix = "ya.slomal.prod";
        MskuParamValue mskuParamValue1 = (MskuParamValue) generateMskuParamValue()
            .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
            .setModificationInfo(new MdmModificationInfo().setUpdatedTs(Instant.now()))
            .setString(customCommCodePrefix);
        var msku = new CommonMsku(
            mskuParamValue1.getMskuId(),
            List.of(mskuParamValue1)
        );
        mdmMskuRepository.insertOrUpdateMsku(msku);

        MdmSupplier supplier1 = new MdmSupplier()
            .setId(9876)
            .setType(MdmSupplierType.REAL_SUPPLIER);
        mdmSupplierRepository.insert(supplier1);

        MdmSupplier supplier2 = new MdmSupplier()
            .setId(654321)
            .setType(MdmSupplierType.THIRD_PARTY);
        mdmSupplierRepository.insert(supplier2);

        MappingCacheDao mappingCache1 = new MappingCacheDao()
            .setMskuId(msku.getMskuId())
            .setSupplierId(beruId.getId())
            .setShopSku("9876.5678");
        mappingsCacheRepository.insert(mappingCache1);

        MappingCacheDao mappingCache2 = new MappingCacheDao()
            .setMskuId(msku.getMskuId())
            .setSupplierId(supplier2.getId())
            .setShopSku("5678");
        mappingsCacheRepository.insert(mappingCache2);

        erpExportService.exportCCCodeMarkupsByMskuIds(List.of(msku.getMskuId()), true);

        List<ErpCCCodeMarkupChange> erpRows = erpCCCodeMarkupExporterRepository.findAll();
        assertThat(erpRows).hasSize(1);
        ErpCCCodeMarkupChange change = erpRows.iterator().next();

        assertThat(change.getShopSku()).isEqualTo(mappingCache1.getShopSku());
        assertThat(change.getPrefixHSCode()).isEqualTo(customCommCodePrefix);
        assertThat(erpRows.stream())
            .map(ErpCCCodeMarkupChange::getShopSku)
            .doesNotContain(mappingCache2.getShopSku());
    }

    @Test
    public void whenSskuHaveNoMappingShouldNotFail() {
        MasterData masterData1 = generateMasterData(generateDocument());
        masterDataRepository.insertOrUpdate(masterData1);
        erpExportService.exportCCCodeMarkups(List.of(masterData1.getShopSkuKey()), true);

        List<ErpCCCodeMarkupChange> erpRows = erpCCCodeMarkupExporterRepository.findAll();
        assertThat(erpRows).hasSize(1);
        ErpCCCodeMarkupChange change = erpRows.iterator().next();

        assertThat(change.getShopSku()).isEqualTo(masterData1.getShopSku());
        assertThat(change.getHSCode()).isEqualTo(masterData1.getCustomsCommodityCode());
        assertThat(change.getManufacturerCountries())
            .containsExactlyInAnyOrderElementsOf(
                masterData1.getManufacturerCountries().stream()
                    .map(GeobaseCountryUtil::countryByName)
                    .flatMap(Optional::stream)
                    .map(GeobaseCountry::getIsoName)
                    .collect(Collectors.toList())
            );
    }

    @Test
    public void whenChangeIsNotSignificantNotSendIt() {
        //given
        MasterData significantChange = generateMasterData(generateDocument())
            .setShopSku("123");
        MasterData insignificantChange = generateMasterData(generateDocument())
            .setShopSku("456")
            .setCustomsCommodityCode(null)
            .setManufacturerCountries(null);
        masterDataRepository.insertOrUpdateAll(List.of(significantChange, insignificantChange));

        //when
        erpExportService.exportCCCodeMarkups(
            List.of(significantChange.getShopSkuKey(), insignificantChange.getShopSkuKey()),
            true
        );

        //then
        List<ErpCCCodeMarkupChange> erpRows = erpCCCodeMarkupExporterRepository.findAll();
        assertThat(erpRows).hasSize(1);
        ErpCCCodeMarkupChange change = erpRows.iterator().next();

        assertThat(change.getShopSku()).isEqualTo(significantChange.getShopSku());
        assertThat(change.getHSCode()).isEqualTo(significantChange.getCustomsCommodityCode());
        assertThat(change.getManufacturerCountries())
            .containsExactlyInAnyOrderElementsOf(
                significantChange.getManufacturerCountries().stream()
                    .map(GeobaseCountryUtil::countryByName)
                    .flatMap(Optional::stream)
                    .map(GeobaseCountry::getIsoName)
                    .collect(Collectors.toList())
            );
    }

    @Test
    public void whenInsignificantIsAfterSignificantSendIt() {
        //given
        MasterData significantChange = generateMasterData(generateDocument())
            .setShopSku("123");
        MasterData insignificantChange = generateMasterData(generateDocument())
            .setShopSku("123")
            .setCustomsCommodityCode(null)
            .setManufacturerCountries(null);

        //when
        masterDataRepository.insertOrUpdate(significantChange);
        erpExportService.exportCCCodeMarkups(List.of(significantChange.getShopSkuKey()), true);
        masterDataRepository.insertOrUpdate(insignificantChange);
        erpExportService.exportCCCodeMarkups(List.of(insignificantChange.getShopSkuKey()), true);

        //then
        List<ErpCCCodeMarkupChange> erpRows = erpCCCodeMarkupExporterRepository.findAll();
        assertThat(erpRows).hasSize(2);
        ErpCCCodeMarkupChange change1 = erpRows.get(0);
        ErpCCCodeMarkupChange change2 = erpRows.get(1);

        assertThat(change1.getShopSku()).isEqualTo(significantChange.getShopSku());
        assertThat(change1.getHSCode()).isEqualTo(significantChange.getCustomsCommodityCode());
        assertThat(change1.getManufacturerCountries())
            .containsExactlyInAnyOrderElementsOf(
                significantChange.getManufacturerCountries().stream()
                    .map(GeobaseCountryUtil::countryByName)
                    .flatMap(Optional::stream)
                    .map(GeobaseCountry::getIsoName)
                    .collect(Collectors.toList())
            );

        assertThat(change2.getShopSku()).isEqualTo(significantChange.getShopSku());
        assertThat(change2.getHSCode()).isEmpty();
        assertThat(change2.getManufacturerCountries()).isEmpty();
    }

    @Test
    public void whenForceModeSendInsignificantChanges() {
        //given
        MasterData insignificantChange = generateMasterData(generateDocument())
            .setCustomsCommodityCode(null)
            .setManufacturerCountries(null);
        masterDataRepository.insertOrUpdate(insignificantChange);

        //when
        erpExportService.exportCCCodeMarkups(List.of(insignificantChange.getShopSkuKey()), false);

        //then
        List<ErpCCCodeMarkupChange> erpRows = erpCCCodeMarkupExporterRepository.findAll();
        assertThat(erpRows).hasSize(1);
        ErpCCCodeMarkupChange change = erpRows.iterator().next();

        assertThat(change.getShopSku()).isEqualTo(insignificantChange.getShopSku());
        assertThat(change.getHSCode()).isEmpty();
        assertThat(change.getManufacturerCountries()).isEmpty();
    }

    @Test
    public void testUsingSskuTraceability() {
        //given
        MasterData updatedMd = generateMasterData(generateDocument())
            .setCustomsCommodityCode(null)
            .setManufacturerCountries(null)
            .setTraceable(true);
        masterDataRepository.insertOrUpdate(updatedMd);

        //when
        erpExportService.exportCCCodeMarkups(List.of(updatedMd.getShopSkuKey()), true);

        //then
        List<ErpCCCodeMarkupChange> erpRows = erpCCCodeMarkupExporterRepository.findAll();
        assertThat(erpRows).hasSize(1);
        ErpCCCodeMarkupChange change = erpRows.iterator().next();

        assertThat(change.getShopSku()).isEqualTo(updatedMd.getShopSku());
        assertThat(change.getHSCode()).isEmpty();
        assertThat(change.getManufacturerCountries()).isEmpty();
        assertThat(change.getTraceable()).isTrue();
    }

    @Test
    public void testSskuTraceableMorePriorThanMsku() {
        //given
        String customsCommodityCode = "super-code";
        MasterData updatedMd = generateMasterData(generateDocument())
            .setCustomsCommodityCode(null)
            .setManufacturerCountries(null)
            .setTraceable(false)
            .setCustomsCommodityCode(customsCommodityCode);
        masterDataRepository.insertOrUpdate(updatedMd);

        MskuParamValue mskuParamValue = (MskuParamValue) generateMskuParamValue()
            .setMdmParamId(KnownMdmParams.IS_TRACEABLE)
            .setModificationInfo(new MdmModificationInfo().setUpdatedTs(Instant.now()))
            .setBool(true);
        var msku = new CommonMsku(
            mskuParamValue.getMskuId(),
            List.of(mskuParamValue)
        );
        mdmMskuRepository.insertOrUpdateMsku(msku);

        mappingsCacheRepository.insert(
            new MappingCacheDao()
                .setMskuId(mskuParamValue.getMskuId())
                .setSupplierId(updatedMd.getSupplierId())
                .setShopSku(updatedMd.getShopSku())
        );

        //when
        erpExportService.exportCCCodeMarkups(List.of(updatedMd.getShopSkuKey()), true);

        //then
        List<ErpCCCodeMarkupChange> erpRows = erpCCCodeMarkupExporterRepository.findAll();
        assertThat(erpRows).hasSize(1);
        ErpCCCodeMarkupChange change = erpRows.iterator().next();

        assertThat(change.getShopSku()).isEqualTo(updatedMd.getShopSku());
        assertThat(change.getHSCode()).isEqualTo(customsCommodityCode);
        assertThat(change.getManufacturerCountries()).isEmpty();
        assertThat(change.getTraceable()).isFalse();
    }

    @Test
    public void testSendingCisHandleMode() {
        //given
        ReferenceItemWrapper referenceItem = ItemWrapperTestUtil.createSurplusCisReferenceItem(
            new ShopSkuKey(mapping.getSupplierId(), mapping.getShopSku()),
            MdmIrisPayload.SurplusHandleMode.ACCEPT,
            MdmIrisPayload.CisHandleMode.ACCEPT_ONLY_DECLARED
        );
        referenceItemRepository.insertOrUpdate(referenceItem);

        //when
        erpExportService.exportCCCodeMarkups(List.of(referenceItem.getShopSkuKey()), true);

        //then
        List<ErpCCCodeMarkupChange> erpRows = erpCCCodeMarkupExporterRepository.findAll();
        assertThat(erpRows).hasSize(1);
        ErpCCCodeMarkupChange change = erpRows.iterator().next();

        assertThat(change.getShopSku()).isEqualTo(referenceItem.getShopSku());
        assertThat(change.getCisHandleMode()).isEqualTo(MdmIrisPayload.CisHandleMode.ACCEPT_ONLY_DECLARED);
    }

    @Test
    public void whenNoCisHandleModeSendNotDefined() {
        //given
        String customsCommodityCode = "super-code";
        MasterData md = generateMasterData(generateDocument())
            .setCustomsCommodityCode(null)
            .setManufacturerCountries(null)
            .setTraceable(false)
            .setCustomsCommodityCode(customsCommodityCode);
        masterDataRepository.insertOrUpdate(md);

        //when
        erpExportService.exportCCCodeMarkups(List.of(md.getShopSkuKey()), true);

        //then
        List<ErpCCCodeMarkupChange> erpRows = erpCCCodeMarkupExporterRepository.findAll();
        assertThat(erpRows).hasSize(1);
        ErpCCCodeMarkupChange change = erpRows.iterator().next();

        assertThat(change.getShopSku()).isEqualTo(md.getShopSku());
        assertThat(change.getHSCode()).isEqualTo(customsCommodityCode);
        assertThat(change.getTraceable()).isFalse();
        assertThat(change.getCisHandleMode()).isEqualTo(MdmIrisPayload.CisHandleMode.NOT_DEFINED);
    }

    private MskuParamValue generateMskuParamValue() {
        return enhancedRandom.nextObject(MskuParamValue.class);
    }

    private ErpCCCodeMarkupChange generateErpCCCodeMarkupChange() {
        return enhancedRandom.nextObject(ErpCCCodeMarkupChange.class);
    }

    private MasterData generateMasterData(QualityDocument... documents) {
        return generateMasterData(mapping, documents);
    }

    private MboMappings.ApprovedMappingInfo addOfferToDB() {
        return addOfferToDB(TestDataUtils.generate(String.class, enhancedRandom));
    }

    private MasterData generateMasterData(MboMappings.ApprovedMappingInfo mapping, QualityDocument... documents) {
        MasterData masterData = TestDataUtils.generateMasterData(new ShopSkuKey(mapping.getSupplierId(),
            mapping.getShopSku()), enhancedRandom, documents);
        masterData.setModifiedTimestamp(DateTimeUtils.dateTimeNow().minusMinutes(1));
        return masterData;
    }

    private QualityDocument generateDocument() {
        return TestDataUtils.generateDocument(enhancedRandom);
    }

    private MboMappings.ApprovedMappingInfo addOfferToDB(String shopSku) {
        return TestDataUtils
            .generateCorrectApprovedMappingInfoBuilder(enhancedRandom)
            .setSupplierId(beruId.getId())
            .setShopSku(shopSku)
            .build();
    }
}
