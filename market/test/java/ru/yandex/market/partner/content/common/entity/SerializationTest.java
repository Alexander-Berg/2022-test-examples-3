package ru.yandex.market.partner.content.common.entity;

import com.google.protobuf.Message;
import org.jooq.Converter;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.ir.http.PartnerContentApi;
import ru.yandex.market.partner.content.common.db.utils.JsonBinder;
import ru.yandex.market.partner.content.common.db.utils.JsonBinders;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class SerializationTest {
    @Test
    public void modelDeserializeTest() {
        String paramBoolJson = "{" +
            "\"@class\":\"ru.yandex.market.partner.content.common.entity.ParameterValue\"," +
            "\"paramId\":4," +
            "\"type\":[\"ru.yandex.market.partner.content.common.entity.ParameterType\",\"BOOL\"]," +
            "\"boolValue\":true," +
            "\"serialVersionUID\":1" +
            "}";

        String paramEnumJson = "{" +
            "\"@class\":\"ru.yandex.market.partner.content.common.entity.ParameterValue\"," +
            "\"paramId\":5," +
            "\"type\":[\"ru.yandex.market.partner.content.common.entity.ParameterType\",\"ENUM\"]," +
            "\"optionId\":6," +
            "\"serialVersionUID\":1" +
            "}";

        String paramNumericJson = "{" +
            "\"@class\":\"ru.yandex.market.partner.content.common.entity.ParameterValue\"," +
            "\"paramId\":7," +
            "\"type\":[\"ru.yandex.market.partner.content.common.entity.ParameterType\",\"NUMERIC\"]," +
            "\"numericValue\":8.9," +
            "\"serialVersionUID\":1" +
            "}";

        String paramStringJson = "{" +
            "\"@class\":\"ru.yandex.market.partner.content.common.entity.ParameterValue\"," +
            "\"paramId\":10," +
            "\"type\":[\"ru.yandex.market.partner.content.common.entity.ParameterType\",\"STRING\"]," +
            "\"stringValue\":\"string value\"," +
            "\"serialVersionUID\":1" +
            "}";

        String definingParamJson = "{" +
            "\"@class\":\"ru.yandex.market.partner.content.common.entity.ParameterValue\"," +
            "\"paramId\":11," +
            "\"type\":[\"ru.yandex.market.partner.content.common.entity.ParameterType\",\"STRING\"]," +
            "\"stringValue\":\"defining\"," +
            "\"serialVersionUID\":1" +
            "}";

        String infoParamJson = "{" +
            "\"@class\":\"ru.yandex.market.partner.content.common.entity.ParameterValue\"," +
            "\"paramId\":12," +
            "\"type\":[\"ru.yandex.market.partner.content.common.entity.ParameterType\",\"STRING\"]," +
            "\"stringValue\":\"info\"," +
            "\"serialVersionUID\":1" +
            "}";

        String skuJson = "{" +
            "\"@class\":\"ru.yandex.market.partner.content.common.entity.Sku\"," +
            "\"shopSku\":\"shop.sku\"," +
            "\"parameterList\":[" + definingParamJson + "," + infoParamJson + "]," +
            "\"imageUrlList\":[\"url3\",\"url4\"]," +
            "\"serialVersionUID\":1" +
            "}";

        String json = "{" +
            "\"@class\":\"ru.yandex.market.partner.content.common.entity.Model\"," +
            "\"categoryId\":1," +
            "\"sourceId\":2," +
            "\"guruModelId\":3," +
            "\"name\":\"model\"," +
            "\"parameterList\":[" + paramBoolJson + "," + paramEnumJson + "," + paramNumericJson + "," +
            paramStringJson + "]," +
            "\"imageUrlList\":[\"url1\",\"url2\"]," +
            "\"skuList\":[" + skuJson + "]," +
            "\"serialVersionUID\":1" +
            "}";

        JsonBinder<Model> modelJsonBinder = JsonBinders.Ticket.resModel();

        Model model = modelJsonBinder.converter().from(json);

        Assert.assertNotNull(model);

        Assert.assertEquals(1, model.getCategoryId());
        Assert.assertEquals(2, model.getSourceId());
        Assert.assertEquals(3, model.getGuruModelId().longValue());
        Assert.assertEquals("model", model.getName());
        Assert.assertEquals(Arrays.asList("url1", "url2"), model.getImageUrlList());

        Assert.assertEquals(4, model.getParameterList().size());

        Map<ParameterType, ParameterValue> modelParams = model.getParameterList().stream()
            .collect(
                Collectors.toMap(ParameterValue::getType, p -> p)
            );

        ParameterValue paramBool = modelParams.get(ParameterType.BOOL);
        Assert.assertEquals(4, paramBool.getParamId());
        Assert.assertTrue(paramBool.isBoolValue());

        ParameterValue paramEnum = modelParams.get(ParameterType.ENUM);
        Assert.assertEquals(5, paramEnum.getParamId());
        Assert.assertEquals(6, paramEnum.getOptionId().intValue());

        ParameterValue paramNumeric = modelParams.get(ParameterType.NUMERIC);
        Assert.assertEquals(7, paramNumeric.getParamId());
        Assert.assertEquals(8.9, paramNumeric.getNumericValue(), 1e-10);

        ParameterValue paramString = modelParams.get(ParameterType.STRING);
        Assert.assertEquals(10, paramString.getParamId());
        Assert.assertEquals("string value", paramString.getStringValue());

        Assert.assertEquals(1, model.getSkuList().size());

        Sku sku = model.getSkuList().get(0);

        Assert.assertEquals("shop.sku", sku.getShopSku());
        Assert.assertEquals(Arrays.asList("url3", "url4"), sku.getImageUrlList());

        Assert.assertEquals(2, sku.getParameterList().size());

        Map<Long, ParameterValue> skuParams = sku.getParameterList().stream()
            .collect(
                Collectors.toMap(ParameterValue::getParamId, p -> p)
            );

        long definingParamId = 11;
        ParameterValue definingParam = skuParams.get(definingParamId);
        Assert.assertEquals("defining", definingParam.getStringValue());

        long infoParamId = 12;
        ParameterValue infoParam = skuParams.get(infoParamId);
        Assert.assertEquals("info", infoParam.getStringValue());
    }

    @Test
    public void modelSerializeTest() {
        Model model = new Model();
        model.setCategoryId(1);
        model.setSourceId(2);
        model.setGuruModelId(3L);
        model.setName("model");

        ParameterValue paramBool = new ParameterValue();
        paramBool.setParamId(4);
        paramBool.setType(ParameterType.BOOL);
        paramBool.setBoolValue(true);

        ParameterValue paramEnum = new ParameterValue();
        paramEnum.setParamId(5);
        paramEnum.setType(ParameterType.ENUM);
        paramEnum.setOptionId(6);

        ParameterValue paramNumeric = new ParameterValue();
        paramNumeric.setParamId(7);
        paramNumeric.setType(ParameterType.NUMERIC);
        paramNumeric.setNumericValue(8.9);

        ParameterValue paramString = new ParameterValue();
        paramString.setParamId(10);
        paramString.setType(ParameterType.STRING);
        paramString.setStringValue("string value");

        model.setParameterList(Arrays.asList(paramBool, paramEnum, paramNumeric, paramString));

        model.setImageUrlList(Arrays.asList("url1", "url2"));

        Sku sku = new Sku();
        sku.setShopSku("shop.sku");

        ParameterValue paramDefining = new ParameterValue();
        paramDefining.setParamId(11);
        paramDefining.setType(ParameterType.STRING);
        paramDefining.setStringValue("defining");

        ParameterValue paramInfo = new ParameterValue();
        paramInfo.setParamId(12);
        paramInfo.setType(ParameterType.STRING);
        paramInfo.setStringValue("info");

        sku.setParameterList(Arrays.asList(paramDefining, paramInfo));

        sku.setImageUrlList(Arrays.asList("url3", "url4"));

        model.setSkuList(Collections.singletonList(sku));

        JsonBinder<Model> modelJsonBinder = JsonBinders.Ticket.resModel();
        Converter<Object, Model> converter = modelJsonBinder.converter();

        Object json = converter.to(model);
        Model parsedModel = converter.from(json);

        Assert.assertEquals(model, parsedModel);
    }

    @Test
    public void ticketSerializeTest() {
        PartnerContentApi.AddModelTicket ticket = PartnerContentApi.AddModelTicket.newBuilder()
            .setCategoryId(1)
            .setModel(
                PartnerContentApi.Model.newBuilder()
                    .setName("model")
                    .addImageUrl("url1")
                    .addImageUrl("url2")
                    .addParameter(
                        PartnerContentApi.ParameterValue.newBuilder()
                            .setParamId(4)
                            .setType(PartnerContentApi.ParameterType.BOOL)
                            .setBoolValue(true)
                            .build()
                    )
                    .addParameter(
                        PartnerContentApi.ParameterValue.newBuilder()
                            .setParamId(5)
                            .setType(PartnerContentApi.ParameterType.DEPENDENT_ENUM)
                            .setEnumOptionId(6)
                            .build()
                    )
                    .addParameter(
                        PartnerContentApi.ParameterValue.newBuilder()
                            .setParamId(7)
                            .setType(PartnerContentApi.ParameterType.NUMERIC)
                            .setNumericValue(8.9)
                            .build()
                    )
                    .addParameter(
                        PartnerContentApi.ParameterValue.newBuilder()
                            .setParamId(10)
                            .setType(PartnerContentApi.ParameterType.STRING)
                            .setStringValue("string value")
                            .build()
                    )
                    .build()
            )
            .addSku(
                PartnerContentApi.Sku.newBuilder()
                    .setShopSku("shop.sku")
                    .addImageUrl("url3")
                    .addImageUrl("url4")
                    .addParameter(
                        PartnerContentApi.ParameterValue.newBuilder()
                            .setParamId(11)
                            .setType(PartnerContentApi.ParameterType.STRING)
                            .setStringValue("defining")
                            .build()
                    )
                    .addParameter(
                        PartnerContentApi.ParameterValue.newBuilder()
                            .setParamId(12)
                            .setType(PartnerContentApi.ParameterType.STRING)
                            .setStringValue("info")
                            .build()
                    )
                    .build()
            )
            .build();

        JsonBinder<Message> modelJsonBinder = JsonBinders.Ticket.ticketProto();
        Converter<Object, Message> converter = modelJsonBinder.converter();

        Object json = converter.to(ticket);
        Message parsedTicket = converter.from(json);

        Assert.assertEquals(ticket, parsedTicket);
    }
}