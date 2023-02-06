package ru.yandex.market.mbo.mdm.common.service.bmdm.converter.ssku;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.ServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.services.metadata.MetadataProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmBooleanAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmEnumAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmInt64AttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmNumericAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmStringAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmStructAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.entity.BmdmEntityToParamValuesConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.param.BmdmAttributeToMdmParamConverterImpl;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mdm.http.MdmEntity;

import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.DATACAMP_MASTER_DATA_VERSION;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.DELIVERY_TIME;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.MIN_SHIPMENT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.QUANTITY_IN_PACK;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.QUANTUM_OF_SUPPLY;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SERVICE_EXISTS;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SUPPLY_SCHEDULE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.TRANSPORT_UNIT_SIZE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.VAT;

public class BmdmEntityToGoldenServiceSskuConverterTest extends MdmBaseDbTestClass {

    private static final long TEST_MD_VERSION = 111L;

    @Autowired
    private MetadataProvider metadataProvider;

    private BmdmEntityToGoldenServiceSskuConverter converter;

    @Before
    public void setUp() throws Exception {
        converter = createConverter();
    }

    private BmdmEntityToGoldenServiceSskuConverter createConverter() {
        var bmdmAttributeToMdmParamConverter = new BmdmAttributeToMdmParamConverterImpl(metadataProvider);
        var universalConverter = new BmdmEntityToParamValuesConverterImpl(metadataProvider);
        universalConverter.updateAttributeConverters(
            List.of(
                new BmdmStructAttributeValuesToParamValuesConverter(universalConverter),
                new BmdmEnumAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
                new BmdmStringAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
                new BmdmBooleanAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
                new BmdmInt64AttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
                new BmdmNumericAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter)
            )
        );

        return new BmdmEntityToGoldenServiceSskuConverterImpl(universalConverter);
    }

    @Test
    public void shouldConvertGoldenServiceSskuToBmdmAndBack() {
        ShopSkuKey skuKey = new ShopSkuKey(1111, "good_sku");
        List<SskuParamValue> paramValues = List.of(
            createSskuParamValue(MIN_SHIPMENT, "minShipment", 6, skuKey),
            createSskuParamValue(TRANSPORT_UNIT_SIZE, "transportUnitSize", 100, skuKey),
            createSskuParamValue(QUANTUM_OF_SUPPLY, "quantumOfSupply", 2, skuKey),
            createSskuParamValue(DELIVERY_TIME, "deliveryTime", 10, skuKey),
            createSskuParamValue(SERVICE_EXISTS, "serviceExists", true, skuKey),
            createSskuParamValue(DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", TEST_MD_VERSION, skuKey),
            createSskuParamValue(VAT, "NDS", new MdmParamOption(1L), skuKey),
            createSskuParamValue(QUANTITY_IN_PACK, "quantityInPack", 12, skuKey),
            createSskuParamValue(SUPPLY_SCHEDULE, "supplySchedule", new MdmParamOption(2L), skuKey)
        );

        ServiceSsku goldenServiceSsku = new ServiceSsku();
        goldenServiceSsku.setKey(skuKey);
        goldenServiceSsku.setParamValues(paramValues);

        MdmEntity converted = converter.goldenServiceSskuToEntity(goldenServiceSsku);
        ServiceSsku convertedBack = converter.entityToGoldenServiceSsku(converted, skuKey.getShopSku());

        Assertions.assertThat(convertedBack.getKey()).isEqualTo(goldenServiceSsku.getKey());
        Assertions.assertThat(convertedBack).isEqualTo(goldenServiceSsku);
    }

    private SskuParamValue createSskuParamValue(long paramId, String xslName, Object value, ShopSkuKey shopSkuKey) {
        return TestMdmParamUtils.createSskuParamValue(
            paramId,
            shopSkuKey,
            xslName,
            value);
    }

}
