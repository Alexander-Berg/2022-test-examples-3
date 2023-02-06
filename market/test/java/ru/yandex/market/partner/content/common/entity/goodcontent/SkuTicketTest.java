package ru.yandex.market.partner.content.common.entity.goodcontent;

import org.jooq.Converter;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.partner.content.common.db.utils.JsonBinder;

public class SkuTicketTest {
    @Test
    public void skuTicketDeserializeTest() {
        String paramJson1 = "{" +
            "\"@class\":\"ru.yandex.market.partner.content.common.entity.goodcontent.RawParamValue\"," +
            "\"paramId\":3," +
            "\"paramName\":\"param_3\"," +
            "\"value\":\"param_3 value 1\"," +
            "\"serialVersionUID\":1" +
            "}";

        String paramJson2 = "{" +
            "\"@class\":\"ru.yandex.market.partner.content.common.entity.goodcontent.RawParamValue\"," +
            "\"paramId\":3," +
            "\"value\":\"param_3 value 2\"," +
            "\"serialVersionUID\":1" +
            "}";

        String paramJson3 = "{" +
            "\"@class\":\"ru.yandex.market.partner.content.common.entity.goodcontent.RawParamValue\"," +
            "\"paramName\":\"param_4\"," +
            "\"value\":\"param_4 value\"," +
            "\"serialVersionUID\":1" +
            "}";

        String groupKey = "{" +
            "\"id\":\"group_id\"," +
            "\"empty\":false," +
            "\"new\":false" +
            "}";

        String json = "{" +
            "\"@class\":\"ru.yandex.market.partner.content.common.entity.goodcontent.SkuTicket\"," +
            "\"categoryId\":1," +
            "\"rowIndex\":2," +
            "\"shopSku\":\"shop_sku\"," +
            "\"groupKey\":" + groupKey + "," +
            "\"name\":\"raw sku\"," +
            "\"mainPictureUrl\":\"main_picture_url\"," +
            "\"otherPictureUrls\":[\"other_picture_url1\",\"other_picture_url2\"]," +
            "\"rawParamValues\":[" + paramJson1 + "," + paramJson2 + "," + paramJson3 + "]," +
            "\"serialVersionUID\":2" +
            "}";

        JsonBinder<SkuTicket> skuTicketJsonBinder = JsonBinder.ofJsonb(SkuTicket.class);

        SkuTicket skuTicket = skuTicketJsonBinder.converter().from(json);

        Assert.assertNotNull(skuTicket);

        SkuTicket expectedSkuTicket = SkuTicket.newBuilder()
            .setCategoryId(1L)
            .setRowIndex(2)
            .setShopSku("shop_sku")
            .setGroupKey(GroupKey.create("group_id"))
            .setName("raw sku")
            .setMainPictureUrl("main_picture_url")
            .addOtherPictureUrl("other_picture_url1")
            .addOtherPictureUrl("other_picture_url2")
            .addRawParamValue(new RawParamValue(3L, "param_3", "param_3 value 1"))
            .addRawParamValue(new RawParamValue(3L, null, "param_3 value 2"))
            .addRawParamValue(new RawParamValue(null, "param_4", "param_4 value"))
            .build();


        Assert.assertEquals(expectedSkuTicket, skuTicket);
    }

    //хакаем старый пайп - объединение недоступно
    @Ignore
    @Test
    public void skuTicketSerializeTest() {
        SkuTicket skuTicket = SkuTicket.newBuilder()
            .setCategoryId(1L)
            .setRowIndex(2)
            .setShopSku("shop_sku")
            .setGroupKey(GroupKey.create("group_id"))
            .setName("raw sku")
            .setMainPictureUrl("main_picture_url")
            .addOtherPictureUrl("other_picture_url1")
            .addOtherPictureUrl("other_picture_url2")
            .addRawParamValue(new RawParamValue(3L, "param_3", "param_3 value 1"))
            .addRawParamValue(new RawParamValue(3L, null, "param_3 value 2"))
            .addRawParamValue(new RawParamValue(null, "param_4", "param_4 value"))
            .build();


        JsonBinder<SkuTicket> skuTicketJsonBinder = JsonBinder.ofJsonb(SkuTicket.class);
        Converter<Object, SkuTicket> converter = skuTicketJsonBinder.converter();

        Object json = converter.to(skuTicket);
        SkuTicket parsedSkuTicket = converter.from(json);

        Assert.assertEquals(skuTicket, parsedSkuTicket);
    }
}