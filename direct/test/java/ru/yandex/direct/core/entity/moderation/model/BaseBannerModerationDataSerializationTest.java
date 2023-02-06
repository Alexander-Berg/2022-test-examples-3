package ru.yandex.direct.core.entity.moderation.model;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.direct.utils.JsonUtils;

public class BaseBannerModerationDataSerializationTest {

    @Test
    public void testDefaultInitialized() {
        var data = new BaseBannerModerationData();
        Assertions.assertThat(JsonUtils.toJson(data)).isEqualTo("{}");
    }

    @Test
    public void testSocialAdvertisementNull() {
        var data = new BaseBannerModerationData();
        data.setIsSocialAdvertisement(null);
        Assertions.assertThat(JsonUtils.toJson(data)).isEqualTo("{}");
    }

    @Test
    public void testSocialAdvertisementFalse() {
        var data = new BaseBannerModerationData();
        data.setIsSocialAdvertisement(false);
        Assertions.assertThat(JsonUtils.toJson(data)).isEqualTo("{\"social_advertising\":false}");
    }

    @Test
    public void testSocialAdvertisementTrue() {
        var data = new BaseBannerModerationData();
        data.setIsSocialAdvertisement(true);
        Assertions.assertThat(JsonUtils.toJson(data)).isEqualTo("{\"social_advertising\":true}");
    }
}
