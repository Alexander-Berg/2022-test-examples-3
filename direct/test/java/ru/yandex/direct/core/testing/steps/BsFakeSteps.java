package ru.yandex.direct.core.testing.steps;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.BannerImageInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.repository.TestBannerImageRepository;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.repository.TestShardOrderIdRepository;
import ru.yandex.direct.dbutil.model.ClientId;

public class BsFakeSteps {

    @Autowired
    private TestCampaignRepository campaignRepository;

    @Autowired
    private TestBannerRepository bannerRepository;

    @Autowired
    private TestBannerImageRepository bannerImageRepository;

    @Autowired
    private TestShardOrderIdRepository shardOrderIdRepository;

    /**
     * Установить фейковый БК-шный ID заказа. Фейковый ID будет вычислен как (ID кампании * 30 + 7)
     *
     * @param campaignInfo Информация о кампании
     */
    public void setOrderId(CampaignInfo campaignInfo) {
        Long orderId = setOrderId(campaignInfo.getShard(), campaignInfo.getClientId(), campaignInfo.getCampaignId());
        campaignInfo.getCampaign().setOrderId(orderId);
    }

    public Long setOrderId(int shard, ClientId clientId, Long campaignId) {
        Long orderId = calculateOrderId(campaignId);
        campaignRepository.updateCampaignOrderIdByCid(shard, campaignId, orderId);
        shardOrderIdRepository.insertOrderIdClientId(clientId, orderId);
        return orderId;
    }

    private static long calculateOrderId(long id) {
        return id * 30 + 7;
    }

    /**
     * Задать фейковый БК-шный ID баннера
     *
     * @param bannerInfo Информация о баннере
     */
    public void setBsBannerId(AbstractBannerInfo<? extends OldBanner> bannerInfo) {
        int shard = bannerInfo.getShard();
        Long bsBannerId = bannerInfo.getBannerId() * 30 + 7;
        bannerInfo.getBanner().setBsBannerId(bsBannerId);
        bannerRepository.updateBannerId(shard, bannerInfo, bsBannerId);
    }

    /**
     * Задать фейковый БК-шный ID баннера используя таблицу banner_images
     *
     * @param bannerImageInfo Информация о баннере
     */
    public void setBsBannerId(BannerImageInfo<?> bannerImageInfo) {
        int shard = bannerImageInfo.getShard();
        Long bsBannerId = bannerImageInfo.getBannerInfo().getBannerId() * 30 + 7;
        bannerImageInfo.getBannerImage().setBsBannerId(bsBannerId);
        bannerImageRepository.updateBannerIdByImageAndHash(shard, bannerImageInfo, bsBannerId);
    }
}
