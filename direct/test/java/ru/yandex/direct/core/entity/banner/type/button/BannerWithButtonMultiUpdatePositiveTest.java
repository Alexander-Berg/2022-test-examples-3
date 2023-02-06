package ru.yandex.direct.core.entity.banner.type.button;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer;
import ru.yandex.direct.core.entity.banner.model.BannerButtonStatusModerate;
import ru.yandex.direct.core.entity.banner.model.ButtonAction;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.i18n.I18NBundle;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.fullCpmBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithButtonMultiUpdatePositiveTest extends BannerClientInfoUpdateOperationTestBase {
    private static final String HREF = "https://ya.ru";
    private static final String HREF_1 = "https://yandex.ru";
    private static final String CAPTION_CUSTOM_TEXT = "Купить лисичек";

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private BannerModifyRepository modifyRepository;

    @Autowired
    public BannersAddOperationFactory addOperationFactory;

    private CreativeInfo creativeInfo;
    private AdGroupInfo adGroupInfo;
    private BannerRepositoryContainer repositoryContainer;

    @Before
    public void before() throws Exception {
        LocaleContextHolder.setLocale(I18NBundle.RU);
        new TestContextManager(getClass()).prepareTestInstance(this);
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        clientInfo = adGroupInfo.getClientInfo();
        creativeInfo = steps.creativeSteps().addDefaultHtml5Creative(clientInfo,
                steps.creativeSteps().getNextCreativeId());
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.ASSET_BUTTON_CUSTOM_TEXT, true);
        repositoryContainer = new BannerRepositoryContainer(adGroupInfo.getShard());
    }

    @Test
    public void update() {
        CpmBanner bannerWithoutButton = fullCpmBanner(creativeInfo.getCreativeId())
                .withCampaignId(adGroupInfo.getCampaignId())
                .withAdGroupId(adGroupInfo.getAdGroupId());

        CpmBanner bannerWithButton = fullCpmBanner(creativeInfo.getCreativeId())
                .withCampaignId(adGroupInfo.getCampaignId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withButtonAction(ButtonAction.BUY)
                .withButtonCaption("Купить")
                .withButtonHref(HREF)
                .withButtonStatusModerate(BannerButtonStatusModerate.YES);

        List<Long> ids = modifyRepository.add(dslContextProvider.ppc(adGroupInfo.getShard()), repositoryContainer,
                asList(bannerWithoutButton, bannerWithButton));

        Long bannerId1 = ids.get(0);
        Long bannerId2 = ids.get(1);

        ModelChanges<CpmBanner> modelChanges1 = ModelChanges.build(bannerId1, CpmBanner.class,
                CpmBanner.BUTTON_ACTION, ButtonAction.DOWNLOAD);
        modelChanges1.process(HREF_1, CpmBanner.BUTTON_HREF);

        ModelChanges<CpmBanner> modelChanges2 = ModelChanges.build(bannerId2, CpmBanner.class,
                CpmBanner.BUTTON_ACTION, ButtonAction.CUSTOM_TEXT);
        modelChanges2.process(CAPTION_CUSTOM_TEXT, CpmBanner.BUTTON_CAPTION);
        modelChanges2.process(HREF, CpmBanner.BUTTON_HREF);

        prepareAndApplyValid(asList(modelChanges1, modelChanges2));

        CpmBanner actualBanner1 = getBanner(bannerId1);
        CpmBanner actualBanner2 = getBanner(bannerId2);

        assertThat(actualBanner1.getButtonAction()).isEqualTo(ButtonAction.DOWNLOAD);
        assertThat(actualBanner1.getButtonCaption()).isEqualTo("Скачать");
        assertThat(actualBanner1.getButtonHref()).isEqualTo(HREF_1);
        assertThat(actualBanner1.getButtonStatusModerate()).isEqualTo(BannerButtonStatusModerate.READY);

        assertThat(actualBanner2.getButtonAction()).isEqualTo(ButtonAction.CUSTOM_TEXT);
        assertThat(actualBanner2.getButtonCaption()).isEqualTo(CAPTION_CUSTOM_TEXT);
        assertThat(actualBanner2.getButtonHref()).isEqualTo(HREF);
        assertThat(actualBanner2.getButtonStatusModerate()).isEqualTo(BannerButtonStatusModerate.READY);
    }
}
