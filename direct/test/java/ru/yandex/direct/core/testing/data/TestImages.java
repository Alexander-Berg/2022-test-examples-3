package ru.yandex.direct.core.testing.data;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.imageio.ImageIO;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;

import ru.yandex.direct.core.entity.banner.model.old.Image;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerSimple;
import ru.yandex.direct.core.entity.banner.model.old.StatusImageModerate;
import ru.yandex.direct.imagesearch.model.ImageDoc;
import ru.yandex.direct.imagesearch.model.Thumb;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.direct.utils.CommonUtils.nvl;

@ParametersAreNonnullByDefault
public class TestImages {
    public static Image defaultImage(Long campaignId, Long adGroupId) {
        return new Image()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withImageHash(randomAlphanumeric(16))
                .withStatusModerate(StatusImageModerate.YES);
    }

    public static Image defaultImage(OldBannerSimple banner) {
        return defaultImage(banner, randomAlphanumeric(16));
    }

    public static Image defaultImage(OldBannerSimple banner, String imageHash) {
        return new Image()
                .withBannerId(banner.getId())
                .withCampaignId(banner.getCampaignId())
                .withAdGroupId(banner.getAdGroupId())
                .withImageHash(imageHash)
                .withImageText("раз два три")
                .withDisclaimerText("раз два три")
                .withStatusModerate(StatusImageModerate.YES);
    }

    public static void fillImageDefaultSystemFields(Image image) {
        image.withStatusModerate(nvl(image.getStatusModerate(), StatusImageModerate.NEW));
    }

    public static byte[] generateBlankGifImageData(int width, int height) {
        return generateBlankImageData(width, height, "gif", BufferedImage.TYPE_INT_ARGB);
    }

    public static byte[] generateBlankImageData(int width, int height, String imageExtension, int imageType) {
        BufferedImage img = new BufferedImage(width, height, imageType);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, imageExtension, byteArrayOutputStream);
        } catch (IOException ignored) {
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] generateBlankSvgImageDate(int width, int height) {
        String content = String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\"/>", width, height);
        return content.getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] generateBlankSvgImageDateWithoutLeadingXmlTag(int width, int height) {
        String content = String.format(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\"/>", width, height);
        return content.getBytes(StandardCharsets.UTF_8);
    }

    public static List<ImageDoc> createImageDocs(List<Pair<String, Integer>> pairUrlSize) {
        return StreamEx.of(pairUrlSize)
                .map(urlSize -> {
                    ImageDoc imageDoc = new ImageDoc();
                    Thumb thumb = new Thumb();
                    thumb.setId(urlSize.getKey());
                    thumb.setUrl(urlSize.getKey());
                    thumb.setHeight(urlSize.getValue());
                    thumb.setWidth(urlSize.getValue());
                    imageDoc.setBigThumb(thumb);
                    return imageDoc;
                })
                .toList();
    }
}
