package ru.yandex.market.ff.dbqueue.service;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.CreateUtilizationOutboundsPayload;
import ru.yandex.market.ff.service.SupplierMappingService;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.http.DeliveryParams;
import ru.yandex.market.mboc.http.MboMappingsForDelivery;
import ru.yandex.market.mdm.http.MdmCommon;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class CreateUtilizationOutboundsProcessingServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private CreateUtilizationOutboundsProcessingService service;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private DeliveryParams deliveryParams;


    @BeforeEach
    void init() {
        when(deliveryParams.searchFulfilmentSskuParams(any()))
            .thenReturn(MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder().build());
    }
    /**
     * Есть несколько трансферов:
     * 1. Не загружены детали. Не должен попасть в создаваемое изъятие по этой причине
     * 2. Создано изъятие (3 и 11), не должен попасть ни в какое новое изъятие
     * 4,5. В 7-ом статусе на брак и просрок от отного поставщика с частично пересекающимися товарами,
     * для одного нет изъятий, для другого 3 изъятия, но в статусах 4, 5 и 8 (id 6, 7 и 8),
     * должны попасть в создаваемое изъятие
     * 9. В 7-ом статусе для другого поставщика, для него нет изъятий, должен попасть в создаваемое изъятие
     * 10. Для другого склада, не должен попасть в создаваемое изъятие по этой причине
     * 12. Трансфер не на утилизацию, не должен попасть ни в какое изъятие
     */
    @Test
    @DatabaseSetup("classpath:db-queue/service/create-utilization-outbounds/before.xml")
    @ExpectedDatabase(
            value = "classpath:db-queue/service/create-utilization-outbounds/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void processPayload() {
        jdbcTemplate.execute("alter sequence shop_request_id_seq restart with 13");
        service.processPayload(new CreateUtilizationOutboundsPayload(100));
    }

    /**
     * Настройкой задано, что размер изъятия утилизации не должен превышать 2 SKU.
     * Есть несколько трансферов на один и тот же склад:
     * 1. На 5 разных товаров
     * 2. На 3 разных товара другого поставщика
     *
     * В результате должно быть создано 6 изъятий (3 без родительского и еще 1 родительское с 2 дочерними):
     * 1. 2 изъятия по 2 товара первого поставщика
     * 2. 1 изъятие на 2 товара второго поставщика
     * 3. 1 общее изъятие на оставшиеся 2 товара (1 от первого и 1 от второго) + 2 дочерних к нему
     */
    @Test
    @DatabaseSetup("classpath:db-queue/service/create-utilization-outbounds/before-small-outbounds.xml")
    @ExpectedDatabase(
            value = "classpath:db-queue/service/create-utilization-outbounds/after-small-outbounds.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void processPayloadCreatingSmallOutbounds() {
        jdbcTemplate.execute("alter sequence shop_request_id_seq restart with 3");
        service.processPayload(new CreateUtilizationOutboundsPayload(100));
    }

    /**
     * Настройкой задано, что размер изъятия утилизации не должен превышать 2 SKU.
     * При этом количество товаров в изъятии утилизации не должно превышать 3.
     *
     * Есть несколько трансферов на один и тот же склад:
     * 1. На 3 товара в количестве (1, 3, 5) у одного поставщика
     * 2. На 1 товар в количестве 2 у другого поставщика
     *
     * В результате должно быть создано 5 изъятий
     * (так как сначала они разобьются по 2 строчки, а внутри уже по 3 товара):
     * 1. Изъятие на 3 вторых товара у первого поставщика
     * 2. Изъятие на 1 первый товар у первого поставщика
     * 3. Изъятие на 3 третьих товара у первого поставщика
     * 4. Изъятие на 2 третьих товара у первого поставщика
     * 5. Изъятие на 2 товара у второго поставщика
     */
    @Test
    @DatabaseSetup("classpath:db-queue/service/create-utilization-outbounds/" +
            "before-small-outbounds-with-small-items-count.xml")
    @ExpectedDatabase(
            value = "classpath:db-queue/service/create-utilization-outbounds/" +
                    "after-small-outbounds-with-small-items-count.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void processPayloadCreatingSmallOutboundsWithSmallItemsCount() {
        jdbcTemplate.execute("alter sequence shop_request_id_seq restart with 3");
        service.processPayload(new CreateUtilizationOutboundsPayload(100));
    }


    /**
     * Настройкой заданы 2 группы карготипов: 500 + 501 и 600 + 700 (и третья для всех осталных)
     * Ограничений по кол-вам не ставим для простоты.
     * (Айтемы в созданных изъятиях первым делом делятся по карготипам, а потом уже по количествам)
     *
     * В результате должно быть созданы 3 изъятия
     * 1. С товарами имеющими карготипы из первой группы - 500 или 501 (у них также могут иметься и другие карготипы)
     * 2. С товарами имеющими карготипы из первой группы - 600 или 700 (если эти товары не попали в первое изъятие)
     * 3. Все оставшиеся
     */
    @Test
    @DatabaseSetup("classpath:db-queue/service/create-utilization-outbounds/before-split-by-cargo-type.xml")
    @ExpectedDatabase(
        value = "classpath:db-queue/service/create-utilization-outbounds/after-split-by-cargo-type.xml",
        assertionMode = NON_STRICT_UNORDERED)
    public void processPayloadSplitByCargoType() {
        jdbcTemplate.execute("alter sequence shop_request_id_seq restart with 1049");
        jdbcTemplate.execute("alter sequence request_item_id_seq restart with 1049");
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(buildMappingArts3And4());
        service.processPayload(new CreateUtilizationOutboundsPayload(100));
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/create-utilization-outbounds-map-subtype/force-util/before.xml")
    @ExpectedDatabase(
            value = "classpath:db-queue/service/create-utilization-outbounds-map-subtype/force-util/after.xml",
            assertionMode = NON_STRICT)
    public void processPayloadForceUtilMapSubType() {
        jdbcTemplate.execute("alter sequence shop_request_id_seq restart with 20");
        service.processPayload(new CreateUtilizationOutboundsPayload(100));
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/create-utilization-outbounds-map-subtype/plan-util/before.xml")
    @ExpectedDatabase(
            value = "classpath:db-queue/service/create-utilization-outbounds-map-subtype/plan-util/after.xml",
            assertionMode = NON_STRICT)
    public void processPayloadPlanUtilMapSubType() {
        jdbcTemplate.execute("alter sequence shop_request_id_seq restart with 25");
        service.processPayload(new CreateUtilizationOutboundsPayload(100));
    }

    @Nonnull
    private MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse buildMappingArts3And4() {
        return MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder()
            .addFulfilmentInfo(
                MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                    .setSurplusHandleMode(MdmCommon.SurplusHandleMode.ACCEPT)
                    .setSupplierId(1)
                    .setShopSku("art3")
                    .setMarketSkuId(300L)
                    .setShopTitle("title3")
                    .setMskuTitle("market_name3")
                    .setShopVendorcode("vendorCode3")
                    .addMskuVendorcode("vendorCode3")
                    .addMskuBarcode("barcode3")
                    .addModelParam(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(SupplierMappingService.EXPIR_DATE_PARAM_ID)
                        .setBoolValue(true)
                        .build())
                    .addModelParam(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(SupplierMappingService.PACKAGE_NUM_IN_SPIKE_PARAM_ID)
                        .setNumericValue("32")
                        .build())
                    .setAllowInbound(true)
                    .addCargoTypes(
                        MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder()
                            .setId(600)
                            .build()
                    ).setGoldenWeightDimensionsInfo(MdmCommon.WeightDimensionsInfo.newBuilder()
                        .setBoxWidthUm(100000)
                        .setBoxHeightUm(101000)
                        .setBoxLengthUm(102000)
                        .build()))
            .addFulfilmentInfo(
                MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                    .setSurplusHandleMode(MdmCommon.SurplusHandleMode.ACCEPT)
                    .setSupplierId(1)
                    .setShopSku("art4")
                    .setMarketSkuId(400L)
                    .setShopTitle("title4")
                    .setMskuTitle("market_name4")
                    .setShopVendorcode("vendorCode4")
                    .addMskuVendorcode("vendorCode4")
                    .addMskuBarcode("barcode4")
                    .addModelParam(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(SupplierMappingService.EXPIR_DATE_PARAM_ID)
                        .setBoolValue(true)
                        .build())
                    .addModelParam(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(SupplierMappingService.PACKAGE_NUM_IN_SPIKE_PARAM_ID)
                        .setNumericValue("32")
                        .build())
                    .setAllowInbound(true)
                    .addCargoTypes(
                        MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder()
                            .setId(701)
                            .build()
                    ).setGoldenWeightDimensionsInfo(MdmCommon.WeightDimensionsInfo.newBuilder()
                        .setBoxWidthUm(100000)
                        .setBoxHeightUm(101000)
                        .setBoxLengthUm(102000)
                        .build()))
            .build();
    }


}
