package ru.yandex.market.mbo.mdm.common.service.relation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import io.grpc.stub.StreamObserver;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuByStorageApiRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.StorageApiSilverSskuRepository;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mdm.http.MdmAttributeValue;
import ru.yandex.market.mdm.http.relations.MdmRelationSearchFilter;
import ru.yandex.market.mdm.http.relations.MdmRelationsByFilterRequest;
import ru.yandex.market.mdm.http.relations.MdmRelationsByFilterResponse;
import ru.yandex.market.mdm.http.search.MdmEntityIds;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource.DEFAULT_AUTO_SOURCE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.FLAT_GOLD_MSKU_ENTITY_TYPE_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.MAPPING_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.MAPPING_MSKU_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.MAPPING_SSKU_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.SILVER_COMMON_SSKU_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.BMDM_ID;

@RunWith(MockitoJUnitRunner.class)
public class MdmRelationServiceForMappingsTest {

    private final MskuByStorageApiRepositoryImpl mskuRepo =
        mock(MskuByStorageApiRepositoryImpl.class);
    private final StorageApiSilverSskuRepository sskuRepo = mock(StorageApiSilverSskuRepository.class);
    private final MappingsCacheRepository mappingsRepo = mock(MappingsCacheRepository.class);
    private final MdmRelationServiceForMappings service =
        new MdmRelationServiceForMappings(mskuRepo, sskuRepo, mappingsRepo);

    @Test
    public void shouldReturnMskusBySskuTest() {
        // given
        var sskuBmdmId = 199L;
        var sskuShopSkuKey = new ShopSkuKey(999, "testSku");
        var expectedMskuBmdmId = 211L;
        var expectedMskuId = 777L;
        var existedMapping = new MappingCacheDao()
            .setShopSkuKey(sskuShopSkuKey)
            .setMskuId(expectedMskuId);
        var request = MdmRelationsByFilterRequest.newBuilder()
            .setFilter(MdmRelationSearchFilter.newBuilder()
                .setByFromEntityIds(MdmEntityIds.newBuilder()
                    .addMdmIds(sskuBmdmId)
                    .setMdmEntityTypeId(SILVER_COMMON_SSKU_ID)
                    .build())
                .build())
            .build();

        given(sskuRepo.findByBmdmId(any()))
            .willReturn(Map.of(sskuBmdmId, silverCommonSsku(sskuBmdmId, sskuShopSkuKey)));
        given(mappingsRepo.findByIds(any(), any())).willReturn(List.of(existedMapping));
        given(mskuRepo.findMskus(any()))
            .willReturn(Map.of(expectedMskuId, commonMsku(expectedMskuBmdmId, expectedMskuId)));

        StreamObserver<MdmRelationsByFilterResponse> responseObserver = mock(StreamObserver.class);

        // when
        service.getRelationBySearchFilter(request, responseObserver);

        // then
        ArgumentCaptor<MdmRelationsByFilterResponse> captor =
            ArgumentCaptor.forClass(MdmRelationsByFilterResponse.class);
        Mockito.verify(responseObserver).onNext(captor.capture());
        MdmRelationsByFilterResponse response = captor.getValue();

        Assertions.assertThat(response.getMdmRelationsCount()).isEqualTo(1);
        var relation = response.getMdmRelations(0).getBaseEntity();
        Assertions.assertThat(relation.getMdmEntityTypeId()).isEqualTo(MAPPING_ID);

        var attributes = relation.getMdmAttributeValuesMap();
        Assertions.assertThat(attributes).containsKey(MAPPING_SSKU_ID);
        Assertions.assertThat(attributes.get(MAPPING_SSKU_ID).getValuesList())
            .extracting(MdmAttributeValue::getReferenceMdmId)
            .containsOnly(sskuBmdmId);

        Assertions.assertThat(attributes).containsKey(MAPPING_MSKU_ID);
        Assertions.assertThat(attributes.get(MAPPING_MSKU_ID).getValuesList())
            .extracting(MdmAttributeValue::getReferenceMdmId)
            .containsOnly(expectedMskuBmdmId);
    }

    @Test
    public void shouldReturnSskuByMskuTest() {
        // given
        var expectedSskuBmdmId = 199L;
        var sskuShopSkuKey = new ShopSkuKey(999, "testSku");
        var mskuBmdmId = 211L;
        var mskuId = 777L;
        var existedMapping = new MappingCacheDao()
            .setShopSkuKey(sskuShopSkuKey)
            .setMskuId(mskuId);
        var request = MdmRelationsByFilterRequest.newBuilder()
            .setFilter(MdmRelationSearchFilter.newBuilder()
                .setByToEntityIds(MdmEntityIds.newBuilder()
                    .addMdmIds(expectedSskuBmdmId)
                    .setMdmEntityTypeId(FLAT_GOLD_MSKU_ENTITY_TYPE_ID)
                    .build())
                .build())
            .build();

        given(mskuRepo.findByBmdmId(any())).willReturn(Map.of(mskuBmdmId, commonMsku(mskuBmdmId, mskuId)));
        given(sskuRepo.findSskus(any())).willReturn(Map.of(sskuShopSkuKey,
            List.of(silverCommonSsku(expectedSskuBmdmId, sskuShopSkuKey))));
        given(mappingsRepo.findByMskuIds(any(), any())).willReturn(List.of(existedMapping));

        StreamObserver<MdmRelationsByFilterResponse> responseObserver = mock(StreamObserver.class);

        // when
        service.getRelationBySearchFilter(request, responseObserver);

        // then
        ArgumentCaptor<MdmRelationsByFilterResponse> captor =
            ArgumentCaptor.forClass(MdmRelationsByFilterResponse.class);
        Mockito.verify(responseObserver).onNext(captor.capture());
        MdmRelationsByFilterResponse response = captor.getValue();

        Assertions.assertThat(response.getMdmRelationsCount()).isEqualTo(1);
        var relation = response.getMdmRelations(0).getBaseEntity();
        Assertions.assertThat(relation.getMdmEntityTypeId()).isEqualTo(MAPPING_ID);

        var attributes = relation.getMdmAttributeValuesMap();
        Assertions.assertThat(attributes).containsKey(MAPPING_SSKU_ID);
        Assertions.assertThat(attributes.get(MAPPING_SSKU_ID).getValuesList())
            .extracting(MdmAttributeValue::getReferenceMdmId)
            .containsOnly(expectedSskuBmdmId);

        Assertions.assertThat(attributes).containsKey(MAPPING_MSKU_ID);
        Assertions.assertThat(attributes.get(MAPPING_MSKU_ID).getValuesList())
            .extracting(MdmAttributeValue::getReferenceMdmId)
            .containsOnly(mskuBmdmId);
    }

    private static SilverCommonSsku silverCommonSsku(long bmdmId, ShopSkuKey shopSkuKey) {
        var key = new SilverSskuKey(shopSkuKey, DEFAULT_AUTO_SOURCE);
        var bmdmIdParam = new MdmParamValue().setMdmParamId(BMDM_ID)
            .setNumeric(BigDecimal.valueOf(bmdmId));
        return new SilverCommonSsku(key)
            .addBaseValue(bmdmIdParam);
    }

    private static CommonMsku commonMsku(long bmdmId, long mskuId) {
        var bmdmIdParam = new MskuParamValue();
        bmdmIdParam.setMdmParamId(BMDM_ID)
            .setNumeric(BigDecimal.valueOf(bmdmId));
        return new CommonMsku(mskuId, List.of(bmdmIdParam));
    }
}
