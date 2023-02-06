package ru.yandex.direct.core.testing.steps;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.model.BannerTurboAppType;
import ru.yandex.direct.core.entity.banner.model.TurboAppInfo;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerTurboApp;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerTurboAppsRepository;
import ru.yandex.direct.core.entity.banner.type.turboapp.TurboAppsInfoRepository;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.turboapps.client.model.TurboAppInfoResponse;
import ru.yandex.direct.utils.JsonUtils;

import static ru.yandex.direct.core.testing.data.TestTurboApps.defaultTurboAppMetaContent;

public class TurboAppSteps {
    private static final String CONTENT = "{\"key\":123}";
    private static final AtomicLong TURBO_APP_INFO_ID = new AtomicLong(100L);

    @Autowired
    private TurboAppsInfoRepository turboAppsInfoRepository;

    @Autowired
    private OldBannerTurboAppsRepository bannerTurboAppsRepository;

    public Long addDefaultTurboAppInfo(int shard, Long clientId) {
        return createDefaultTurboAppInfo(shard, clientId).getTurboAppInfoId();
    }

    public TurboAppInfo createDefaultTurboAppInfo(int shard, Long clientId) {
        TurboAppInfo info = new TurboAppInfo()
                .withTurboAppId(TURBO_APP_INFO_ID.getAndIncrement())
                .withClientId(clientId)
                .withContent(JsonUtils.toJson(defaultTurboAppMetaContent()));
        turboAppsInfoRepository.addOrUpdateTurboAppInfo(shard, List.of(info));
        return info;
    }

    public void addBannerTurboApp(int shard, OldBannerTurboApp bannerTurboApp) {
        bannerTurboAppsRepository.addOrUpdateBannerTurboApps(shard, List.of(bannerTurboApp));
    }

    public void addBannerTurboAppAndTurboAppInfo(int shard, Long bannerId, TurboAppInfo info, String content) {
        addBannerTurboAppAndTurboAppInfo(shard, bannerId, info, content, BannerTurboAppType.OFFER);
    }

    public void addBannerTurboAppAndTurboAppInfo(int shard, Long bannerId, TurboAppInfo info, String content,
                                                 BannerTurboAppType type) {
        turboAppsInfoRepository.addOrUpdateTurboAppInfo(shard, List.of(info));
        OldBannerTurboApp bannerTurboApp = new OldBannerTurboApp()
                .withTurboAppInfoId(info.getTurboAppInfoId())
                .withBannerTurboAppType(type)
                .withBannerId(bannerId)
                .withContent(content);
        bannerTurboAppsRepository.addOrUpdateBannerTurboApps(shard, List.of(bannerTurboApp));
    }

    public <T extends OldBanner> void addBannerTurboApp(AbstractBannerInfo<T> bannerInfo,
                                                        TurboAppInfoResponse turboAppInfoResponse) {
        addBannerTurboAppAndTurboAppInfo(
                bannerInfo.getShard(),
                bannerInfo.getBannerId(),
                new TurboAppInfo()
                        .withClientId(bannerInfo.getClientId().asLong())
                        .withTurboAppId(turboAppInfoResponse.getAppId())
                        .withContent(turboAppInfoResponse.getMetaContent()),
                turboAppInfoResponse.getContent());
    }
}
