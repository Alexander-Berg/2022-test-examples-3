package ru.yandex.market.mbo.mdm.common.masterdata.repository.param;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmModificationInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.service.bmdm.MetadataProviderMock;
import ru.yandex.market.mbo.mdm.common.service.bmdm.TestBmdmUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmBooleanAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmEnumAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmInt64AttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmNumericAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmStringAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmStructAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.entity.BmdmEntityToParamValuesConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.msku.BmdmEntityToCommonMskuConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.msku.BmdmEntityToCommonMskuConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.param.BmdmAttributeToMdmParamConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.param.BmdmAttributeToMdmParamConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.proto.MdmEntityStorageService;
import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mdm.http.MdmAttributeValue;
import ru.yandex.market.mdm.http.MdmEntity;
import ru.yandex.market.mdm.http.entity.GetMdmEntityByExternalKeysRequest;
import ru.yandex.market.mdm.http.entity.GetMdmEntityResponse;
import ru.yandex.market.mdm.http.entity.SaveMdmEntityRequest;
import ru.yandex.market.mdm.http.entity.SaveMdmEntityResponse;
import ru.yandex.market.mdm.http.entity.SaveMdmEntityResult;

@SuppressWarnings("checkstyle:MagicNumber")
public class MskuByStorageApiRepositoryImplTest extends MdmBaseIntegrationTestClass {
    @Mock
    private MdmEntityStorageService mdmEntityStorageService;
    @Autowired
    private MdmParamCache mdmParamCache;

    private MskuByStorageApiRepositoryImpl mskuByStorageApiRepository;
    private BmdmEntityToCommonMskuConverter converter;

    private static BmdmEntityToCommonMskuConverter bmdmEntityToCommonMskuConverter(MdmParamCache mdmParamCache) {
        MetadataProviderMock metadataProviderMock = new MetadataProviderMock();
        metadataProviderMock.addEntityType(TestBmdmUtils.FLAT_GOLD_MSKU_ENTITY_TYPE);
        metadataProviderMock.addExternalReferences(TestBmdmUtils.FLAT_GOLD_MSKU_EXTERNAL_REFERENCES);

        BmdmAttributeToMdmParamConverter bmdmAttributeToMdmParamConverter =
            new BmdmAttributeToMdmParamConverterImpl(metadataProviderMock);
        BmdmEntityToParamValuesConverterImpl universalConverter =
            new BmdmEntityToParamValuesConverterImpl(metadataProviderMock);
        universalConverter.updateAttributeConverters(List.of(
            new BmdmStructAttributeValuesToParamValuesConverter(universalConverter),
            new BmdmEnumAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
            new BmdmStringAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
            new BmdmBooleanAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
            new BmdmInt64AttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
            new BmdmNumericAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter)
        ));
        return new BmdmEntityToCommonMskuConverterImpl(universalConverter, mdmParamCache);
    }

    @Before
    public void setup() {
        converter = bmdmEntityToCommonMskuConverter(mdmParamCache);
        mskuByStorageApiRepository = new MskuByStorageApiRepositoryImpl(converter, mdmEntityStorageService);
    }

    @Test
    public void findMskus() {
        List<Long> mskuIds = List.of(1L, 2L, 3L, 4L);
        Mockito.when(mdmEntityStorageService.getByExternalKeys(Mockito.any())).then(ivc ->
            GetMdmEntityResponse.newBuilder()
                .addAllMdmEntities(getMdmEntities(((GetMdmEntityByExternalKeysRequest) ivc.getArguments()[0])
                    .getMdmExternalKeys().getMdmExternalKeysList().stream()
                    .flatMap(item -> item.getMdmAttributeValuesList().stream())
                    .flatMap(item -> item.getValuesList().stream())
                    .map(MdmAttributeValue::getInt64)
                    .collect(Collectors.toList())))
                .build());
        var mskus = mskuByStorageApiRepository.findMskus(mskuIds);
        var expectedCommonMskus = getCommonMskus(mskuIds);
        Assertions.assertThat(mskus.values()).containsExactlyInAnyOrderElementsOf(expectedCommonMskus);
    }

    @Test
    public void insertOrUpdateOkMskus() {
        List<Long> mskuIds = List.of(1L, 2L, 3L, 4L);
        List<CommonMsku> savingMskus = getCommonMskus(mskuIds);
        savingMskus.forEach(msku -> msku.getParamValues().values().forEach(value -> value
            .setModificationInfo(new MdmModificationInfo()
                .setUpdatedTs(Instant.now().minus(10, ChronoUnit.DAYS)))
        ));
        Mockito.when(mdmEntityStorageService.save(Mockito.any())).then(ivc ->
            SaveMdmEntityResponse.newBuilder()
                .addAllResults(getSaveMdmEntityResults(((SaveMdmEntityRequest) ivc.getArguments()[0])
                        .getMdmEntitiesList().stream().map(MdmEntity::getMdmId).collect(Collectors.toList()),
                    SaveMdmEntityResult.Code.OK))
                .build());

        var result = mskuByStorageApiRepository.insertOrUpdateMskus(savingMskus);
        Assertions.assertThat(result.values()).containsExactlyInAnyOrderElementsOf(savingMskus);
    }

    @Test
    public void insertOrUpdateNotOkMskus() {
        List<Long> mskuIds = List.of(1L, 2L, 3L, 4L);
        List<CommonMsku> savingMskus = getCommonMskus(mskuIds);
        savingMskus.forEach(msku -> msku.getParamValues().values().forEach(value -> value
            .setModificationInfo(new MdmModificationInfo()
                .setUpdatedTs(Instant.now().minus(10, ChronoUnit.DAYS)))
        ));

        Mockito.when(mdmEntityStorageService.save(Mockito.any())).then(ivc ->
            SaveMdmEntityResponse.newBuilder()
                .addAllResults(getSaveMdmEntityResults(((SaveMdmEntityRequest) ivc.getArguments()[0])
                        .getMdmEntitiesList().stream().map(MdmEntity::getMdmId).collect(Collectors.toList()),
                    SaveMdmEntityResult.Code.ERROR))
                .build());
        Assertions.assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
            mskuByStorageApiRepository.insertOrUpdateMskus(savingMskus);
        });
    }

    private List<SaveMdmEntityResult> getSaveMdmEntityResults(List<Long> mdmIds, SaveMdmEntityResult.Code code) {
        return mdmIds.stream()
            .map(item -> getSaveMdmEntityResult(item, code))
            .collect(Collectors.toList());
    }

    private SaveMdmEntityResult getSaveMdmEntityResult(Long mdmId, SaveMdmEntityResult.Code code) {
        return SaveMdmEntityResult.newBuilder()
            .setMdmId(mdmId)
            .setCode(code)
            .setFrom(Instant.now().toEpochMilli())
            .build();
    }

    private List<CommonMsku> getCommonMskus(List<Long> mdmIds) {
        return mdmIds.stream()
            .map(this::getCommonMsku)
            .collect(Collectors.toList());
    }

    private CommonMsku getCommonMsku(Long mskuId) {
        MskuParamValue value = new MskuParamValue().setMskuId(mskuId);
        BigDecimal entityId = BigDecimal.valueOf(mskuId * 100);
        value.setMdmParamId(KnownMdmParams.SHELF_LIFE)
            .setXslName(mdmParamCache.get(KnownMdmParams.SHELF_LIFE).getXslName())
            .setNumeric(BigDecimal.ONE)
            .setMasterDataSourceType(MasterDataSourceType.AUTO)
            .setMasterDataSourceId("vasya")
            .setUpdatedTs(Instant.EPOCH.plus(2, ChronoUnit.MILLIS));
        MskuParamValue unit = new MskuParamValue().setMskuId(mskuId);
        unit.setMdmParamId(KnownMdmParams.SHELF_LIFE_UNIT)
            .setXslName(mdmParamCache.get(KnownMdmParams.SHELF_LIFE_UNIT).getXslName())
            .setOption(new MdmParamOption(
                KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.UNLIMITED)))
            .setMasterDataSourceType(MasterDataSourceType.AUTO)
            .setMasterDataSourceId("vasya")
            .setUpdatedTs(Instant.EPOCH.plus(2, ChronoUnit.MILLIS));
        MskuParamValue version = new MskuParamValue().setMskuId(mskuId);
        version.setMdmParamId(KnownMdmParams.BMDM_ID)
            .setXslName(mdmParamCache.get(KnownMdmParams.BMDM_ID).getXslName())
            .setNumeric(entityId)
            .setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN)
            .setUpdatedTs(Instant.EPOCH.plus(123, ChronoUnit.MILLIS));
        return new CommonMsku(mskuId, List.of(value, unit, version));
    }

    private List<MdmEntity> getMdmEntities(List<Long> mdmIds) {
        return mdmIds.stream()
            .map(this::getMdmEntity)
            .collect(Collectors.toList());
    }

    private MdmEntity getMdmEntity(Long mdmId) {
        return converter.commonMskuToFlatGoldMskuEntity(getCommonMsku(mdmId));
    }
}
