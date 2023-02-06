package ru.yandex.market.loyalty.admin.it;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.loyalty.admin.config.ITConfig;
import ru.yandex.market.loyalty.api.model.coin.SmartShoppingThumbs;
import ru.yandex.market.loyalty.core.config.Smartshopping;
import ru.yandex.market.loyalty.core.service.avatar.AvatarImageId;
import ru.yandex.market.loyalty.core.service.avatar.AvatarsClient;
import ru.yandex.market.loyalty.core.test.LoyaltySpringTestRunner;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

@Ignore("this test suite should be run manually because it uses real Avatar")
@ContextConfiguration(classes = ITConfig.class)
@RunWith(LoyaltySpringTestRunner.class)
@WebAppConfiguration
public class AvatarClientUtilForTesting {
    @Smartshopping
    @Autowired
    private AvatarsClient avatarsClient;

    @Test
    public void uploadAndDelete() throws IOException {
        AvatarImageId imageId = avatarsClient.uploadImageToAvatar(
                FileUtils.readFileToByteArray(new File(AvatarClientUtilForTesting.class.getClassLoader().getResource(
                        "image_sample.png").getFile()))
        );

        try {
            String fullThumbUrl = avatarsClient.imageLinkWithoutThumb(imageId) + SmartShoppingThumbs._328_X_328;
            RestTemplate restTemplate = new RestTemplate();
            byte[] image = restTemplate.getForObject(fullThumbUrl, byte[].class);
            assertNotNull(image);
            assertNotEquals(0, image.length);
        } finally {
            avatarsClient.removeImage(imageId);
        }
    }
}
