package ru.yandex.market.mbo.common.imageservice.avatars;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author ayratgdl
 * @date 28.06.17
 */
public class AvatarImageDepotServiceTest {

    @Test
    public void getImageIdTest() {
        AvatarImageDepotService avatarService = new AvatarImageDepotService();

        Assert.assertEquals(
            "12345/image_name.png",
            avatarService.getImageId("http://any.host:80/get-mpic/12345/image_name.png/orig")
        );

        Assert.assertEquals(
            "12345/image_name.png",
            avatarService.getImageId("//any.host/get-other_namespace/12345/image_name.png")
        );

        try {
            avatarService.getImageId("//any.host/get/12345/image_name.png/aa/bb");
            Assert.fail();
        } catch (Exception e) {
            // ok
        }
    }

    @Test
    public void getThumbnailUrlTest() {
        AvatarImageDepotService avatarService = new AvatarImageDepotService();
        avatarService.setReadHostPort("avatar.example.com");
        avatarService.setNamespace("mpic");

        Assert.assertEquals("//avatar.example.com/get-mpic/12345/image_name.png/100x500",
            avatarService.getThumbnailUrl("12345/image_name.png", "100x500")
        );

        Assert.assertEquals("//avatar.example.com/get-mpic/12345/image_name.svg/svg",
            avatarService.getThumbnailUrl("12345/image_name.svg", "100x500")
        );
    }

    @Test
    public void getImageIdFromThumbnailTest() {
        AvatarImageDepotService avatarService = new AvatarImageDepotService();
        avatarService.setReadHostPort("avatar.example.com");
        avatarService.setNamespace("mpic");

        String url = avatarService.getThumbnailUrl("12345/image_name.png", "100x500");
        String imageId = avatarService.getImageId(url);
        Assert.assertEquals("12345/image_name.png", imageId);
    }

    @Test
    public void getImageUrlTest() {
        AvatarImageDepotService avatarService = new AvatarImageDepotService();
        avatarService.setReadHostPort("avatar.example.com");
        avatarService.setNamespace("mpic");

        Assert.assertEquals("//avatar.example.com/get-mpic/12345/image_name.png/orig",
                            avatarService.getImageUrl("12345/image_name.png")
        );

        Assert.assertEquals("//avatar.example.com/get-mpic/12345/image_name.svg/svg",
                            avatarService.getImageUrl("12345/image_name.svg")
        );

        Assert.assertEquals("//avatar.example.com/get-mpic/12345/image_name.SVG/svg",
                            avatarService.getImageUrl("12345/image_name.SVG")
        );
    }

}
