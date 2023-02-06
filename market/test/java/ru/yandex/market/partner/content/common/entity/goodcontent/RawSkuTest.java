package ru.yandex.market.partner.content.common.entity.goodcontent;

import org.jooq.Converter;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.partner.content.common.db.utils.JsonBinder;

public class RawSkuTest {
    @Test
    public void rawSkuDeserializeTest() {
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

        String json = "{" +
            "\"@class\":\"ru.yandex.market.partner.content.common.entity.goodcontent.RawSku\"," +
            "\"categoryId\":1," +
            "\"rowIndex\":2," +
            "\"shopSku\":\"shop_sku\"," +
            "\"groupId\":\"group_id\"," +
            "\"name\":\"raw sku\"," +
            "\"mainPictureUrl\":\"main_picture_url\"," +
            "\"otherPictureUrls\":\"other_picture_urls\"," +
            "\"rawParamValues\":[" + paramJson1 + "," + paramJson2 + "," + paramJson3 + "]," +
            "\"serialVersionUID\":1" +
            "}";

        JsonBinder<RawSku> rawSkuJsonBinder = JsonBinder.ofJsonb(RawSku.class);

        RawSku rawSku = rawSkuJsonBinder.converter().from(json);

        Assert.assertNotNull(rawSku);

        RawSku expectedRawSku = RawSku.newBuilder()
            .setCategoryId(1L)
            .setRowIndex(2)
            .setShopSku("shop_sku")
            .setGroupId("group_id")
            .setName("raw sku")
            .setMainPictureUrl("main_picture_url")
            .setOtherPictureUrls("other_picture_urls")
            .addRawParamValue(new RawParamValue(3L, "param_3", "param_3 value 1"))
            .addRawParamValue(new RawParamValue(3L, null, "param_3 value 2"))
            .addRawParamValue(new RawParamValue(null, "param_4", "param_4 value"))
            .build();


        Assert.assertEquals(expectedRawSku, rawSku);
    }

    @Test
    public void rawSkuSerializeTest() {
        RawSku rawSku = RawSku.newBuilder()
            .setCategoryId(1L)
            .setRowIndex(2)
            .setShopSku("shop_sku")
            .setGroupId("group_id")
            .setName("raw sku")
            .setMainPictureUrl("main_picture_url")
            .setOtherPictureUrls("other_picture_urls")
            .addRawParamValue(new RawParamValue(3L, "param_3", "param_3 value 1"))
            .addRawParamValue(new RawParamValue(3L, null, "param_3 value 2"))
            .addRawParamValue(new RawParamValue(null, "param_4", "param_4 value"))
            .build();


        JsonBinder<RawSku> rawSkuJsonBinder = JsonBinder.ofJsonb(RawSku.class);
        Converter<Object, RawSku> converter = rawSkuJsonBinder.converter();

        Object json = converter.to(rawSku);
        RawSku parsedRawSku = converter.from(json);

        Assert.assertEquals(rawSku, parsedRawSku);
    }
}