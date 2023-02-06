package ru.yandex.direct.ess.config.logicobjects.bsexport.resources;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.direct.ess.config.ConverterUtils;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;

class BannerResourceTypeDeserializerTest {

    @ParameterizedTest
    @EnumSource(value = BannerResourceType.class)
    void test(BannerResourceType bannerResourceType) {
        var object = new BsExportBannerResourcesObject.Builder()
                .setBid(12L)
                .setResourceType(bannerResourceType)
                .build();
        var gotObjects = ConverterUtils.logicObjectsSerializeDeserialize(List.of(object));
        assertThat(gotObjects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(object);
    }

    @Test
    void unknownEnumValueTest() {
        var object = "{\"bid\":\"9944345668\",\n" +
                "\"cid\":\"57290952\",\n" +
                "\"pid\":\"4385582150\",\n" +
                "\"debug_info\":{\"method\":\"\", \"service\":\"\", \"reqid\": 0},\n" +
                "\"resource_type\":\"unsupported_enum_value\"}";

        var gotObject = JsonUtils.fromJson(object, BsExportBannerResourcesObject.class);
        var expectedObject = new BsExportBannerResourcesObject.Builder()
                .setBid(9944345668L)
                .setPid(4385582150L)
                .setCid(57290952L)
                .setResourceType(BannerResourceType.UNKNOWN)
                .build();
        assertThat(gotObject).isEqualToComparingFieldByFieldRecursively(expectedObject);
    }
}
