package ru.yandex.market.mdm.app.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.SingleIrisItemValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class IrisItemControllerTest extends MdmBaseDbTestClass {
    private static final int SERVICE_ID = 100065406;
    private static final int BUSINESS_ID = 203294822;
    private MockMvc mockMvc;

    @Autowired
    private SilverSskuRepository silverSskuRepository;
    @Autowired
    private ServiceSskuConverter converter;
    @Autowired
    private MdmSupplierCachingService mdmSupplierCachingService;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;

    @Before
    public void setup() {
        IrisItemController controller = new IrisItemController(
            Mockito.mock(ReferenceItemRepository.class),
            null,
            Mockito.mock(SingleIrisItemValidationService.class),
            silverSskuRepository,
            converter
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(BUSINESS_ID)
            .setType(MdmSupplierType.BUSINESS)
        );
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(SERVICE_ID)
            .setBusinessEnabled(true)
            .setBusinessId(BUSINESS_ID)
            .setType(MdmSupplierType.THIRD_PARTY)
        );
        mdmSupplierCachingService.refresh();
    }

    @Test
    public void checkEoxSilverItems() throws Exception {
        ShopSkuKey key = new ShopSkuKey(BUSINESS_ID, "shop_sku111");

        var now = Instant.now();
        Instant tsMeasurement = now.minusSeconds(100);
        Instant tsSupplier = now.plusSeconds(100);

        silverSskuRepository.insertOrUpdateSsku(TestDataUtils.wrapSilver(List.of(
            silverValue(key, KnownMdmParams.BOX_COUNT, "2", tsSupplier, MasterDataSourceType.SUPPLIER),
            silverValue(key, KnownMdmParams.WEIGHT_GROSS, "1", tsSupplier, MasterDataSourceType.SUPPLIER),
            silverValue(key, KnownMdmParams.LENGTH, "30", tsSupplier, MasterDataSourceType.SUPPLIER),
            silverValue(key, KnownMdmParams.WIDTH, "40", tsSupplier, MasterDataSourceType.SUPPLIER),
            silverValue(key, KnownMdmParams.HEIGHT, "50", tsSupplier, MasterDataSourceType.SUPPLIER)
        )));
        silverSskuRepository.insertOrUpdateSsku(TestDataUtils.wrapSilver(List.of(
            silverValue(key, KnownMdmParams.DELIVERY_TIME, "7", tsMeasurement, MasterDataSourceType.MEASUREMENT),
            silverValue(key, KnownMdmParams.WEIGHT_GROSS, "1.01", tsMeasurement, MasterDataSourceType.MEASUREMENT),
            silverValue(key, KnownMdmParams.LENGTH, "34", tsMeasurement, MasterDataSourceType.MEASUREMENT),
            silverValue(key, KnownMdmParams.WIDTH, "42", tsMeasurement, MasterDataSourceType.MEASUREMENT),
            silverValue(key, KnownMdmParams.HEIGHT, "51", tsMeasurement, MasterDataSourceType.MEASUREMENT),
            silverValue(key, KnownMdmParams.WEIGHT_TARE, "0.2", tsMeasurement, MasterDataSourceType.MEASUREMENT)
        )));

        mockMvc.perform(get("/mdm-api/iris-items/silver_items/{ssku}/{supplierId}",
            key.getShopSku(), key.getSupplierId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(jsonPath("$[0].item.information[0].itemShippingUnit.weightGrossMg.updatedTs")
                .value(tsSupplier.toEpochMilli()))
            .andExpect(jsonPath("$[1].item.information[0].itemShippingUnit.weightGrossMg.updatedTs")
                .value(tsMeasurement.toEpochMilli()))

            .andExpect(jsonPath("$[0].item.information[0].itemShippingUnit.weightGrossMg.value").value("1000000"))
            .andExpect(jsonPath("$[1].item.information[0].itemShippingUnit.weightGrossMg.value").value("1010000"))

            .andExpect(jsonPath("$[0].item.information[0].itemShippingUnit.widthMicrometer.value").value("400000"))
            .andExpect(jsonPath("$[1].item.information[0].itemShippingUnit.widthMicrometer.value").value("420000"))

            .andExpect(jsonPath("$[0].item.information[0].itemShippingUnit.heightMicrometer.value").value("500000"))
            .andExpect(jsonPath("$[1].item.information[0].itemShippingUnit.heightMicrometer.value").value("510000"))

            .andExpect(jsonPath("$[0].item.information[0].itemShippingUnit.lengthMicrometer.value").value("300000"))
            .andExpect(jsonPath("$[1].item.information[0].itemShippingUnit.lengthMicrometer.value").value("340000"))

            .andExpect(jsonPath("$[1].item.information[0].itemShippingUnit.weightTareMg.value").value("200000"));
    }

    @Test
    public void checkEoxSilverItemsRetrievedByService() throws Exception {
        ShopSkuKey serviceKey = new ShopSkuKey(SERVICE_ID, "shop_sku111");
        ShopSkuKey key = new ShopSkuKey(BUSINESS_ID, "shop_sku111");
        sskuExistenceRepository.markExistence(List.of(serviceKey), true);

        Instant ts = Instant.now();

        silverSskuRepository.insertOrUpdateSsku(TestDataUtils.wrapSilver(List.of(
            silverValue(key, KnownMdmParams.DELIVERY_TIME, "7", ts, MasterDataSourceType.MEASUREMENT),
            silverValue(key, KnownMdmParams.WEIGHT_GROSS, "1.01", ts, MasterDataSourceType.MEASUREMENT),
            silverValue(key, KnownMdmParams.LENGTH, "34", ts, MasterDataSourceType.MEASUREMENT),
            silverValue(key, KnownMdmParams.WIDTH, "42", ts, MasterDataSourceType.MEASUREMENT),
            silverValue(key, KnownMdmParams.HEIGHT, "51", ts, MasterDataSourceType.MEASUREMENT),
            silverValue(key, KnownMdmParams.WEIGHT_TARE, "0.2", ts, MasterDataSourceType.MEASUREMENT)
        )));
        jdbcTemplate.update("update mdm.ssku_silver_param_value set updated_ts = source_updated_ts ", Map.of());

        mockMvc.perform(get("/mdm-api/iris-items/silver_items/{ssku}/{supplierId}",
            serviceKey.getShopSku(), serviceKey.getSupplierId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(jsonPath("$[0].receivedTs.seconds").value(ts.getEpochSecond()))
            .andExpect(jsonPath("$[0].item.information[0].itemShippingUnit.weightGrossMg.value").value("1010000"))
            .andExpect(jsonPath("$[0].item.information[0].itemShippingUnit.widthMicrometer.value").value("420000"))
            .andExpect(jsonPath("$[0].item.information[0].itemShippingUnit.heightMicrometer.value").value("510000"))
            .andExpect(jsonPath("$[0].item.information[0].itemShippingUnit.lengthMicrometer.value").value("340000"))
            .andExpect(jsonPath("$[0].item.information[0].itemShippingUnit.weightTareMg.value").value("200000"));
    }

    private static SskuSilverParamValue silverValue(ShopSkuKey key,
                                                    long mdmParamId,
                                                    String decimal,
                                                    Instant ts,
                                                    MasterDataSourceType source) {
        return (SskuSilverParamValue) new SskuSilverParamValue()
            .setShopSkuKey(key)
            .setMasterDataSourceId("-")
            .setMasterDataSourceType(source)
            .setMdmParamId(mdmParamId)
            .setXslName("-")
            .setNumeric(new BigDecimal(decimal))
            .setSourceUpdatedTs(ts)
            .setUpdatedTs(ts);
    }
}
