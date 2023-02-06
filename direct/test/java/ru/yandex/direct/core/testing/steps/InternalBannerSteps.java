package ru.yandex.direct.core.testing.steps;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup;
import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.banner.model.InternalModerationInfo;
import ru.yandex.direct.core.entity.banner.model.TemplateVariable;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.NewInternalBannerInfo;
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils;
import ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestGroups.activeInternalAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewInternalBanners.fullInternalBanner;

@ParametersAreNonnullByDefault
public class InternalBannerSteps {

    @Autowired
    private BannerModifyRepository bannerModifyRepository;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private DslContextProvider dslContextProvider;

    public NewInternalBannerInfo createDefaultInternalBanner() {
        return createInternalBanner(new NewInternalBannerInfo());
    }

    public NewInternalBannerInfo createInternalBanner(AdGroupInfo adGroupInfo) {
        return createInternalBanner(new NewInternalBannerInfo()
                .withAdGroupInfo(adGroupInfo));
    }

    public NewInternalBannerInfo createInternalBanner(AdGroupInfo adGroupInfo,
                                                      BannerStatusModerate statusModerate,
                                                      Long templateId, Long templateResourceId) {
        InternalBanner internalBanner = fullInternalBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withStatusModerate(statusModerate)
                .withTemplateId(templateId)
                .withTemplateVariables(List.of(new TemplateVariable()
                        .withTemplateResourceId(templateResourceId)
                        .withInternalValue(RandomStringUtils.randomAlphanumeric(11))));

        return createInternalBanner(
                new NewInternalBannerInfo()
                        .withAdGroupInfo(adGroupInfo)
                        .withBanner(internalBanner)
        );
    }

    public NewInternalBannerInfo createModeratedInternalBanner(AdGroupInfo adGroupInfo,
                                                               BannerStatusModerate statusModerate,
                                                               String value) {
        InternalBanner internalBanner = fullInternalBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withStatusModerate(statusModerate)
                .withTemplateId(TemplatePlaceRepositoryMockUtils.PLACE_3_TEMPLATE_1)
                .withTemplateVariables(List.of(new TemplateVariable()
                        .withTemplateResourceId(TemplateResourceRepositoryMockUtils.TEMPLATE_7_RESOURCE)
                        .withInternalValue(value)))
                .withModerationInfo(new InternalModerationInfo()
                        .withIsSecretAd(true)
                        .withStatusShowAfterModeration(true)
                        .withSendToModeration(true)
                        .withTicketUrl("https://st.yandex-team.ru/LEGAL-113"));

        return createInternalBanner(
                new NewInternalBannerInfo()
                        .withAdGroupInfo(adGroupInfo)
                        .withBanner(internalBanner)
        );
    }

    public NewInternalBannerInfo createModeratedInternalBanner(AdGroupInfo adGroupInfo,
                                                               BannerStatusModerate statusModerate) {
        return createModeratedInternalBanner(adGroupInfo, statusModerate, RandomStringUtils.randomAlphanumeric(11));
    }

    public NewInternalBannerInfo createInternalBanner(NewInternalBannerInfo bannerInfo) {
        checkBannerInfoConsistency(bannerInfo);

        // если клиент не создан, создаем его
        if (bannerInfo.getClientId() == null) {
            if (bannerInfo.getClientInfo().getClient() == null) {
                bannerInfo.getClientInfo()
                        .withClient(defaultClient());
            }
            clientSteps.createClient(bannerInfo.getClientInfo());
        }

        // если группа не создана, создаем её
        // (степ создания группы создаст кампанию и клиента при необходимости)
        if (bannerInfo.getAdGroupId() == null) {
            if (bannerInfo.getAdGroupInfo().getAdGroup() == null) {
                bannerInfo.getAdGroupInfo()
                        .withAdGroup(activeInternalAdGroup(null, 0L, 0, 0));
            }
            adGroupSteps.createAdGroup(bannerInfo.getAdGroupInfo());
        }

        // если баннер не задан, устанавливаем дефолтный
        if (bannerInfo.getBanner() == null) {
            bannerInfo.withBanner(fullInternalBanner(bannerInfo.getCampaignId(),
                    bannerInfo.getAdGroupId()));
        }

        // дозаполняем поля баннера перед сохранением
        InternalBanner banner = bannerInfo.getBanner();
        banner.withAdGroupId(bannerInfo.getAdGroupId())
                .withCampaignId(bannerInfo.getCampaignId());

        var container = new BannerRepositoryContainer(bannerInfo.getShard());

        bannerModifyRepository.add(dslContextProvider.ppc(bannerInfo.getShard()), container, singletonList(banner));

        return bannerInfo;
    }

    private static void checkBannerInfoConsistency(NewInternalBannerInfo bannerInfo) {
        AdGroup adGroup = bannerInfo.getAdGroupInfo().getAdGroup();
        Banner banner = bannerInfo.getBanner();

        if (adGroup != null) {
            checkState(adGroup instanceof InternalAdGroup, "adGroup must be InternalAdGroup");
        }
        if (banner != null) {
            checkState(banner instanceof InternalBanner, "banner must be InternalBanner");
        }
    }

}
