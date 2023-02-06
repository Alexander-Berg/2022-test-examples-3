package ru.yandex.market.mbo.mdm.common.masterdata.services.tm;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.mdm.common.service.MdmSolomonPushService;
import ru.yandex.market.mboc.common.masterdata.repository.CargoTypeRepository;
import ru.yandex.market.mboc.common.services.modelstorage.MboModelsService;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.MbocCommon;
import ru.yandex.market.mdm.http.tm.TmCargoType;
import ru.yandex.market.mdm.http.tm.TmEnrichmentData;
import ru.yandex.market.mdm.http.tm.TmEnrichmentRequest;
import ru.yandex.market.mdm.http.tm.TmEnrichmentResponse;
import ru.yandex.market.mdm.http.tm.TmShopSkuKey;
import ru.yandex.market.mdm.http.tm.TmWeightDimensionsInfo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MdmTmIntegrationImplTest {

    private static final String SHOP_SKU = "00002.shop_sku";
    private static final int THREE_P_SUPPLIER_ID = 1000;
    private static final int ONE_P_SUPPLIER_ID = 465852;
    private static final int MOCKED_MODEL_ID = 2000;

    private MdmTmIntegrationImpl mdmTmIntegrationService;

    private ReferenceItemRepository referenceItemRepository = mock(ReferenceItemRepository.class);

    private MdmSolomonPushService mdmSolomonPushService = mock(MdmSolomonPushService.class);

    private MboMappingsService mboMappingsService = mock(MboMappingsService.class);

    private MboModelsService mboModelsService = mock(MboModelsService.class);

    private CargoTypeRepository cargoTypeRepository = mock(CargoTypeRepository.class);

    @Before
    public void setUp() {
        mdmTmIntegrationService = new MdmTmIntegrationImpl(
            new SupplierConverterServiceMock(),
            referenceItemRepository,
            mdmSolomonPushService,
            mboMappingsService,
            mboModelsService,
            cargoTypeRepository
        );

        mockIris();
        mockMboModelIds();
        mockMdmCargoTypes();
        mockMboCargoTypes();

    }

    private void mockMdmCargoTypes() {
        when(cargoTypeRepository.findAll()).thenReturn(
            List.of(
                new ru.yandex.market.mboc.common.masterdata.model.CargoType(1, "Карготип 1 (есть)", 100L),
                new ru.yandex.market.mboc.common.masterdata.model.CargoType(2, "Карготип 2 (нет)", 200L)
            )
        );
    }

    private void mockMboModelIds() {
        when(mboMappingsService.searchLiteApprovedMappingsByKeys(any())).thenAnswer(
            (Answer<MboMappings.SearchLiteMappingsResponse>) invocation -> {
                var request = (MboMappings.SearchLiteMappingsByKeysRequest) invocation.getArgument(0);
                return MboMappings.SearchLiteMappingsResponse.newBuilder()
                    .addMapping(MbocCommon.MappingInfoLite
                        .newBuilder()
                        .setSupplierId(request.getKeys(0).getSupplierId())
                        .setShopSku(request.getKeys(0).getShopSku())
                        .setModelId(MOCKED_MODEL_ID)
                        .build())
                    .build();
            });
    }

    private void mockMboCargoTypes() {
        when(mboModelsService.loadModels(any(), any(), any(), anyBoolean())).thenAnswer(
            (Answer<Map<Long, Model>>) invocation -> {
                List<Long> keys = invocation.getArgument(0);
                return keys.stream().collect(Collectors.toMap(
                    Function.identity(),
                    key -> new Model()
                        .setId(key)
                        .setParameterValues(List.of(
                            ModelStorage.ParameterValue
                                .newBuilder()
                                .setParamId(100)
                                .setBoolValue(true)
                                .build())
                        )
                ));
            }
        );
    }

    private void mockIris() {
        mockIrisSupplier(THREE_P_SUPPLIER_ID, "1000");
        mockIrisSupplier(ONE_P_SUPPLIER_ID, "00002");
    }

    private OngoingStubbing<List<ReferenceItemWrapper>> mockIrisSupplier(int supplierId, String sourceId) {
        return when(referenceItemRepository.findByIds(Set.of(
            new ru.yandex.market.mboc.common.offers.model.ShopSkuKey(
                supplierId,
                SHOP_SKU
            )
        ))).thenReturn(
            List.of(
                new ReferenceItemWrapper().setReferenceItem(
                    MdmIrisPayload.Item
                        .newBuilder()
                        .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                            .setSupplierId(supplierId)
                            .setShopSku(SHOP_SKU)
                            .build()
                        )
                        .addInformation(
                            MdmIrisPayload.ReferenceInformation.newBuilder()
                                .setItemShippingUnit(MdmIrisPayload.ShippingUnit
                                    .newBuilder()
                                    .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(20_000L).build())
                                    .build())
                                .build()
                        )
                        .addInformation(
                            MdmIrisPayload.ReferenceInformation.newBuilder()
                                .setItemShippingUnit(MdmIrisPayload.ShippingUnit
                                    .newBuilder()
                                    .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(1000L).build())
                                    .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(2000L).build())
                                    .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(3000L).build())
                                    .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(30_000L).build())
                                    .build())
                                .build()
                        )
                        .build())
            )
        );
    }

    @Test
    public void test3p() {
        TmEnrichmentResponse response =
            mdmTmIntegrationService.doSearch(TmEnrichmentRequest
                .newBuilder()
                .addShopSku(TmShopSkuKey
                    .newBuilder()
                    .setShopSku(SHOP_SKU)
                    .setSupplierId(THREE_P_SUPPLIER_ID)
                    .build()
                )
                .build()
            );
        Assertions.assertThat(response).isEqualTo(
            TmEnrichmentResponse
                .newBuilder()
                .addEnrichmentData(TmEnrichmentData
                    .newBuilder()
                    .setSupplierId(THREE_P_SUPPLIER_ID)
                    .setShopSku(SHOP_SKU)
                    .setWeightDimensionsInfo(
                        TmWeightDimensionsInfo
                            .newBuilder()
                            .setBoxHeightUm(1000L)
                            .setBoxWidthUm(2000L)
                            .setBoxLengthUm(3000L)
                            .setWeightGrossMg(30_000L)
                            .setWeightNetMg(20_000L)
                            .build()
                    )
                    .addCargoType(TmCargoType.newBuilder()
                        .setId(1)
                        .setName("Карготип 1 (есть)")
                        .build())
                    .build())
                .build()
        );
    }

    @Test
    public void test1p() {
        TmEnrichmentResponse response =
            mdmTmIntegrationService.doSearch(TmEnrichmentRequest
                .newBuilder()
                .addShopSku(TmShopSkuKey
                    .newBuilder()
                    .setShopSku(SHOP_SKU)
                    .setSupplierId(ONE_P_SUPPLIER_ID)
                    .build()
                )
                .build()
            );
        Assertions.assertThat(response).isEqualTo(
            TmEnrichmentResponse
                .newBuilder()
                .addEnrichmentData(TmEnrichmentData
                    .newBuilder()
                    .setSupplierId(ONE_P_SUPPLIER_ID)
                    .setShopSku(SHOP_SKU)
                    .setWeightDimensionsInfo(
                        TmWeightDimensionsInfo
                            .newBuilder()
                            .setBoxHeightUm(1000L)
                            .setBoxWidthUm(2000L)
                            .setBoxLengthUm(3000L)
                            .setWeightGrossMg(30_000L)
                            .setWeightNetMg(20_000L)
                            .build()
                    )
                    .addCargoType(TmCargoType.newBuilder()
                        .setId(1)
                        .setName("Карготип 1 (есть)")
                        .build())
                    .build())
                .build()
        );
    }
}
