package ru.yandex.direct.core.testing.steps;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.direct.core.testing.info.BannerImageFormatInfo;
import ru.yandex.direct.core.testing.repository.TestBannerImageRepository;

@Component
public class BannerImageFormatSteps {

    @Autowired
    private TestBannerImageRepository testBannerImageRepository;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private BannerSteps bannerSteps;

    public BannerImageFormatInfo createBannerImageFormat(BannerImageFormatInfo bannerImageFormatInfo) {
        if (bannerImageFormatInfo.getClientId() == null) {
            clientSteps.createClient(bannerImageFormatInfo.getClientInfo());
        }

        int shard = bannerImageFormatInfo.getShard();
        var bannerImageFormat = bannerImageFormatInfo.getBannerImageFormat();
        testBannerImageRepository.addBannerImageFormats(shard, List.of(bannerImageFormat));

        bannerSteps.addImageToImagePool(shard, bannerImageFormatInfo.getClientId(),
                bannerImageFormat.getImageHash());

        return bannerImageFormatInfo;
    }
}
