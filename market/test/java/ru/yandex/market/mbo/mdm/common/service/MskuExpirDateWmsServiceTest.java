package ru.yandex.market.mbo.mdm.common.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang.mutable.MutableInt;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MboMskuChange;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MboMskuUpdateResult;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ExpectedMappingQuality;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.MboMskuUpdateService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.KnownMdmMboParams;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.DeepmindMskuSyncClient;
import ru.yandex.market.mboc.common.utils.IrisSskuSyncClient;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.mboc.http.DeliveryParams;
import ru.yandex.market.mboc.http.MboMappingsForDelivery;

public class MskuExpirDateWmsServiceTest extends MdmBaseDbTestClass {
    private static final int EXPECTED_RETRY_COUNT = 5;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private MskuRepository mskuRepository;
    @Autowired
    private MdmParamCache mdmParamCache;
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @Mock
    private MboMskuUpdateService mboMskuUpdateService;
    @Mock
    private DeepmindMskuSyncClient deepmindMskuSyncClient;
    @Mock
    private DeliveryParams mbocWmsApi;
    @Mock
    private IrisSskuSyncClient irisSskuSyncClient;

    private MskuExpirDateWmsService service;

    // Моковые стейты для соответствующих компонент-участниц этого причудливого танго межсетевых взаимодействий
    private Map<Long, Boolean> mboState;
    private Map<Long, Boolean> deepmindState;

    @Before
    public void setup() {
        mboState = new HashMap<>();
        deepmindState = new HashMap<>();
        var skv = new StorageKeyValueServiceMock();
        skv.putValue(MdmProperties.EXPIR_DATE_WMS_RETRY_SLEEP_MS, 1L);
        skv.putValue(MdmProperties.EXPIR_DATE_WMS_RETRY_COUNT, EXPECTED_RETRY_COUNT);
        service = new MskuExpirDateWmsService(
            mboMskuUpdateService,
            deepmindMskuSyncClient,
            mbocWmsApi,
            mappingsCacheRepository,
            mskuRepository,
            mdmParamCache,
            irisSskuSyncClient,
            skv
        );
        Mockito.when(mboMskuUpdateService.update(Mockito.any(MboMskuChange.class), Mockito.anyList(),
            Mockito.anyLong(), Mockito.anyString())).thenAnswer(invocation -> {

            MboMskuChange change = invocation.getArgument(0);
            List<Long> mskuIds = invocation.getArgument(1);
            boolean expirDate = change.getExpirDate();
            List<MboMskuUpdateResult> result = new ArrayList<>();
            for (long mskuId : mskuIds) {
                if (!mboState.containsKey(mskuId)) {
                    result.add(new MboMskuUpdateResult(mskuId, MboMskuUpdateResult.Status.NOT_OK));
                } else if (Objects.equals(mboState.get(mskuId), expirDate)) {
                    result.add(new MboMskuUpdateResult(mskuId, MboMskuUpdateResult.Status.NO_OP));
                } else {
                    mboState.put(mskuId, expirDate);
                    result.add(new MboMskuUpdateResult(mskuId, MboMskuUpdateResult.Status.OK));
                }
            }
            return result;
        });

        Mockito.when(deepmindMskuSyncClient.syncDeepmindMskuCache(Mockito.anyCollection())).thenAnswer(invocation -> {
            Collection<Long> mskuIds = invocation.getArgument(0);
            mskuIds.forEach(id -> deepmindState.put(id, mboState.get(id)));
            return true;
        });

        Mockito.when(mbocWmsApi.searchFulfilmentSskuParams(
            Mockito.any(MboMappingsForDelivery.SearchFulfilmentSskuParamsRequest.class))).thenAnswer(invocation -> {

            MboMappingsForDelivery.SearchFulfilmentSskuParamsRequest request = invocation.getArgument(0);
            List<ShopSkuKey> keys = request.getKeysList().stream()
                .map(k -> new ShopSkuKey(k.getSupplierId(), k.getShopSku()))
                .collect(Collectors.toList());
            List<Long> mskuIds = mappingsCacheRepository.findByIds(keys, ExpectedMappingQuality.ANY)
                .stream()
                .map(MappingCacheDao::getMskuId)
                .collect(Collectors.toList());
            var response = MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder();
            for (long mskuId : mskuIds) {
                if (deepmindState.get(mskuId) != null) {
                    var info = MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder();
                    info.addModelParam(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(KnownMdmMboParams.EXPIR_DATE_PARAM_ID)
                        .setBoolValue(deepmindState.get(mskuId))
                        .build())
                        .setMarketSkuId(mskuId);
                    response.addFulfilmentInfo(info);
                }
            }
            return response.build();
        });
        Mockito.when(irisSskuSyncClient.syncSskuToIris(Mockito.anyCollection()))
            .thenAnswer(invocation ->
                new IrisSskuSyncClient.IrisStats().setEnqueuedInIris(new HashSet<>(invocation.getArgument(0))));
    }

    @Test
    public void whenEmptyInputShouldDoNothing() {
        var result = service.setExpirDatesOnMskus(Map.of());
        Assertions.assertThat(result.isEmpty()).isTrue();
    }

    @Test
    public void whenNotUpdatedInMboButAlreadyGoodShouldReturnInResult() {
        prepareValueInMbo(1L, true);
        prepareValueInMbo(2L, false);
        mapping(1L, 10050, "dfsfdhjh");
        mapping(1L, 10050, "xxxxxfgg");
        mapping(2L, 10875, "ooooo");

        var result = service.setExpirDatesOnMskus(Map.of(
            1L, true,
            2L, false
        ));
        Assertions.assertThat(result.getUpdatedInMbo()).isEmpty();
        Assertions.assertThat(result.getNoOpInMbo()).containsExactlyInAnyOrder(1L, 2L);
        Assertions.assertThat(result.getOkInMboc()).containsExactlyInAnyOrder(1L, 2L);
        Assertions.assertThat(result.getFailInMbo()).isEmpty();
        Assertions.assertThat(result.getFailInMboc()).isEmpty();
        Assertions.assertThat(result.getUpdatedInMdm()).isEmpty();
        Assertions.assertThat(mdmValueOf(1L)).isNull();
        Assertions.assertThat(mdmValueOf(2L)).isNull();
    }

    @Test
    public void whenNotUpdatedInMboAndHasIncorrectValueShouldNotBeReturned() {
        mapping(1L, 10050, "dfsfdhjh");
        mapping(1L, 10050, "xxxxxfgg");
        mapping(2L, 10875, "ooooo");

        var result = service.setExpirDatesOnMskus(Map.of(
            1L, true,
            2L, false
        ));
        Assertions.assertThat(result.getNoOpInMbo()).isEmpty();
        Assertions.assertThat(result.getUpdatedInMbo()).isEmpty();
        Assertions.assertThat(result.getOkInMboc()).isEmpty();
        Assertions.assertThat(result.getFailInMbo()).containsExactlyInAnyOrder(1L, 2L);
        Assertions.assertThat(result.getFailInMboc()).containsExactlyInAnyOrder(1L, 2L);
        Assertions.assertThat(result.getUpdatedInMdm()).isEmpty();
        Assertions.assertThat(mskuRepository.findAllMskus()).isEmpty();
    }

    @Test
    public void whenAllOkShouldReturnInResult() {
        prepareValueInMbo(1L, false);
        prepareValueInMbo(2L, true);
        mapping(1L, 10050, "dfsfdhjh");
        mapping(1L, 10050, "xxxxxfgg");
        mapping(2L, 10875, "ooooo");

        var result = service.setExpirDatesOnMskus(Map.of(
            1L, true,
            2L, false
        ));
        Assertions.assertThat(result.getUpdatedInMbo()).containsExactlyInAnyOrder(1L, 2L);
        Assertions.assertThat(result.getNoOpInMbo()).isEmpty();
        Assertions.assertThat(result.getOkInMboc()).containsExactlyInAnyOrder(1L, 2L);
        Assertions.assertThat(result.getFailInMbo()).isEmpty();
        Assertions.assertThat(result.getFailInMboc()).isEmpty();
        Assertions.assertThat(result.getUpdatedInMdm()).containsExactlyInAnyOrder(1L, 2L);
        Assertions.assertThat(mdmValueOf(1L)).isTrue();
        Assertions.assertThat(mdmValueOf(2L)).isFalse();
    }

    @Test
    public void whenNotOkInMbocShouldRetryUntilExhausted() {
        MutableInt retries = new MutableInt(0);
        // Эмулируем неуспех на всех МСКУ, кроме одной
        Mockito.when(mbocWmsApi.searchFulfilmentSskuParams(
            Mockito.any(MboMappingsForDelivery.SearchFulfilmentSskuParamsRequest.class))).thenAnswer(invocation -> {

            retries.increment();
            long loneSuccessfulId = deepmindState.keySet()
                .stream()
                .sorted()
                .findFirst()
                .get();
            var response = MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder();
            var info = MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder();
            info.addModelParam(ModelStorage.ParameterValue.newBuilder()
                .setParamId(KnownMdmMboParams.EXPIR_DATE_PARAM_ID)
                .setBoolValue(deepmindState.get(loneSuccessfulId))
                .build())
                .setMarketSkuId(loneSuccessfulId);
            response.addFulfilmentInfo(info);
            return response.build();
        });

        prepareValueInMbo(1L, true);
        prepareValueInMbo(2L, true);
        mapping(1L, 10050, "dfsfdhjh");
        mapping(1L, 10050, "xxxxxfgg");
        mapping(2L, 10875, "ooooo");

        var result = service.setExpirDatesOnMskus(Map.of(
            1L, false,
            2L, false
        ));
        Assertions.assertThat(result.getUpdatedInMbo()).containsExactlyInAnyOrder(1L, 2L);
        Assertions.assertThat(result.getNoOpInMbo()).isEmpty();
        Assertions.assertThat(result.getFailInMbo()).isEmpty();
        Assertions.assertThat(result.getOkInMboc()).containsExactlyInAnyOrder(1L);
        Assertions.assertThat(result.getFailInMboc()).containsExactlyInAnyOrder(2L);
        Assertions.assertThat(result.getUpdatedInMdm()).containsExactlyInAnyOrder(1L, 2L);
        Assertions.assertThat(result.getUpdatedInFF().getEnqueuedInIris()).containsExactlyInAnyOrder(
            new ShopSkuKey(10050, "dfsfdhjh"),
            new ShopSkuKey(10050, "xxxxxfgg")
        );
        Assertions.assertThat(mdmValueOf(1L)).isFalse();
        Assertions.assertThat(mdmValueOf(2L)).isFalse();
        Assertions.assertThat(retries.intValue()).isEqualTo(EXPECTED_RETRY_COUNT);
    }

    @Test
    public void whenNotOkInMbocShouldRetryUntilOk() {
        MutableInt retries = new MutableInt(0);
        int successfulRetry = 3;
        // Эмулируем неуспех на всех нескольких ретраях, пока не достигнем якобы удачной попытки
        Mockito.when(mbocWmsApi.searchFulfilmentSskuParams(
            Mockito.any(MboMappingsForDelivery.SearchFulfilmentSskuParamsRequest.class))).thenAnswer(invocation -> {

            retries.increment();
            var response = MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder();
            for (long mskuId : deepmindState.keySet()) {
                var info = MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder();
                info.addModelParam(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(KnownMdmMboParams.EXPIR_DATE_PARAM_ID)
                    .setBoolValue(deepmindState.get(mskuId))
                    .build())
                    .setMarketSkuId(mskuId);
                response.addFulfilmentInfo(info);
            }
            if (retries.intValue() == successfulRetry) {
                return response.build();
            }
            // Эмулируем неудачу - выбросим часть информации
            var loneOkInfo = response.getFulfilmentInfo(0);
            return response.clearFulfilmentInfo().addFulfilmentInfo(loneOkInfo).build();
        });

        prepareValueInMbo(1L, true);
        prepareValueInMbo(2L, true);
        mapping(1L, 10050, "dfsfdhjh");
        mapping(1L, 10050, "xxxxxfgg");
        mapping(2L, 10875, "ooooo");

        var result = service.setExpirDatesOnMskus(Map.of(
            1L, false,
            2L, false
        ));
        Assertions.assertThat(result.getUpdatedInMbo()).containsExactlyInAnyOrder(1L, 2L);
        Assertions.assertThat(result.getNoOpInMbo()).isEmpty();
        Assertions.assertThat(result.getFailInMbo()).isEmpty();
        Assertions.assertThat(result.getOkInMboc()).containsExactlyInAnyOrder(1L, 2L);
        Assertions.assertThat(result.getFailInMboc()).isEmpty();
        Assertions.assertThat(result.getUpdatedInMdm()).containsExactlyInAnyOrder(1L, 2L);
        Assertions.assertThat(result.getUpdatedInFF().getEnqueuedInIris()).containsExactlyInAnyOrder(
            new ShopSkuKey(10050, "dfsfdhjh"),
            new ShopSkuKey(10050, "xxxxxfgg"),
            new ShopSkuKey(10875, "ooooo")
        );
        Assertions.assertThat(mdmValueOf(1L)).isFalse();
        Assertions.assertThat(mdmValueOf(2L)).isFalse();
        Assertions.assertThat(retries.intValue()).isEqualTo(successfulRetry);
    }

    private void prepareValueInMbo(long mskuId, Boolean existingValue) {
        mboState.put(mskuId, existingValue);
    }

    private Boolean mdmValueOf(long mskuId) {
        return mskuRepository.findMsku(mskuId)
            .flatMap(msku -> msku.getParamValue(KnownMdmParams.EXPIR_DATE))
            .flatMap(MdmParamValue::getBool)
            .orElse(null);
    }

    private void mapping(long mskuId, int supplierId, String shopSku) {
        mappingsCacheRepository.insert(new MappingCacheDao()
            .setMskuId(mskuId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setCategoryId(0)
        );
    }
}
