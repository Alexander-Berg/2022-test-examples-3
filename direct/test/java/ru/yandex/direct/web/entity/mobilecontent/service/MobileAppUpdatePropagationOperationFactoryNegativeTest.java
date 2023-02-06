package ru.yandex.direct.web.entity.mobilecontent.service;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.mobileapp.MobileAppDefects;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileAppInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.entity.mobilecontent.model.TrackingSystem;
import ru.yandex.direct.web.entity.mobilecontent.model.PropagationMode;
import ru.yandex.direct.web.entity.mobilecontent.model.PropagationRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.core.testing.data.TestBanners.activeMobileAppBanner;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MobileAppUpdatePropagationOperationFactoryNegativeTest {
    private static final String STORE_URL = "https://play.google.com/store/apps/details?id=com.kiloo.subwaysurf";
    private static final String TRACKING_URL = "https://redirect.appmetrica.yandex.com/?click_id={logid}";
    private static final String IMPRESSION_URL = "https://view.adjust.com/impression?click_id={logid}";

    @Autowired
    private Steps steps;

    @Autowired
    private MobileAppUpdatePropagationOperationFactory mobileAppUpdatePropagationOperationFactory;

    private ClientInfo clientInfo;
    private MobileAppInfo mobileAppInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        mobileAppInfo = steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL);

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo, mobileAppInfo);
        AdGroupInfo groupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(campaignInfo);

        steps.bannerSteps().createActiveMobileAppBanner(
                activeMobileAppBanner(groupInfo.getCampaignId(), groupInfo.getAdGroupId()).withHref(STORE_URL),
                groupInfo);
        steps.bannerSteps().createActiveMobileAppBanner(
                activeMobileAppBanner(groupInfo.getCampaignId(), groupInfo.getAdGroupId()).withHref(TRACKING_URL),
                groupInfo);
    }

    @Test
    public void mobileAppNotFound() {
        PropagationRequest request = PropagationRequest.builder()
                .withMobileAppId(mobileAppInfo.getMobileAppId() + 1000L)
                .withDisplayedAttributes(Collections.emptySet())
                .withPropagationMode(PropagationMode.APPLY_TO_ANY_RELATED_BANNERS_AND_REPLACE_ALL)
                .build();
        MobileAppUpdatePropagationOperationFactory.Result propagationOperation =
                mobileAppUpdatePropagationOperationFactory
                        .createPropagationOperation(clientInfo.getUid(), clientInfo.getClientId(), request);
        assertThat("Операция не создана", propagationOperation.hasOperation(), equalTo(false));
        assertThat("Вернулась ошибка 'не найдено приложение'", propagationOperation.getError(), equalTo(
                MobileAppDefects.appNotFound()));

    }

    @Test
    public void tooManyBanners() {
        PropagationRequest request = PropagationRequest.builder()
                .withMobileAppId(mobileAppInfo.getMobileAppId())
                .withDisplayedAttributes(Collections.emptySet())
                .withPropagationMode(PropagationMode.APPLY_TO_ANY_RELATED_BANNERS_AND_REPLACE_ALL)
                .build();
        MobileAppUpdatePropagationOperationFactory.Result propagationOperation =
                mobileAppUpdatePropagationOperationFactory
                        .createPropagationOperation(clientInfo.getUid(), clientInfo.getClientId(), request, 1);
        assertThat("Операция не создана", propagationOperation.hasOperation(), equalTo(false));
        assertThat("Вернулась ошибка 'не найдено приложение'", propagationOperation.getError(), equalTo(
                MobileAppDefects.tooManyBanners()));

    }

    @Test
    public void tooManyBanners_WithNewCampaign() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo, mobileAppInfo);
        AdGroupInfo groupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(campaignInfo);

        steps.bannerSteps().createActiveMobileAppBanner(
                activeMobileAppBanner(groupInfo.getCampaignId(), groupInfo.getAdGroupId()).withHref(STORE_URL),
                groupInfo);

        PropagationRequest request = PropagationRequest.builder()
                .withMobileAppId(mobileAppInfo.getMobileAppId())
                .withDisplayedAttributes(Collections.emptySet())
                .withPropagationMode(PropagationMode.APPLY_TO_ANY_RELATED_BANNERS_AND_REPLACE_ALL)
                .build();
        MobileAppUpdatePropagationOperationFactory.Result propagationOperation =
                mobileAppUpdatePropagationOperationFactory
                        .createPropagationOperation(clientInfo.getUid(), clientInfo.getClientId(), request, 2);
        assertThat("Операция не создана", propagationOperation.hasOperation(), equalTo(false));
        assertThat("Вернулась ошибка 'не найдено приложение'", propagationOperation.getError(), equalTo(
                MobileAppDefects.tooManyBanners()));

    }

    @Test
    public void notTooManyBanners_WithUacCampaign() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveUacMobileAppCampaign(clientInfo, mobileAppInfo);
        AdGroupInfo groupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(campaignInfo);

        steps.bannerSteps().createActiveMobileAppBanner(
                activeMobileAppBanner(groupInfo.getCampaignId(), groupInfo.getAdGroupId()).withHref(STORE_URL),
                groupInfo);

        PropagationRequest request = PropagationRequest.builder()
                .withMobileAppId(mobileAppInfo.getMobileAppId())
                .withDisplayedAttributes(Collections.emptySet())
                .withPropagationMode(PropagationMode.APPLY_TO_ANY_RELATED_BANNERS_AND_REPLACE_ALL)
                .build();
        MobileAppUpdatePropagationOperationFactory.Result propagationOperation =
                mobileAppUpdatePropagationOperationFactory
                        .createPropagationOperation(clientInfo.getUid(), clientInfo.getClientId(), request, 2);
        assertThat("Операция создана", propagationOperation.hasOperation(), equalTo(true));

    }

    @Test
    public void untrackableBannerHref() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.RMP_CPI_UNDER_KNOWN_TRACKING_SYSTEM_ONLY, true);
        PropagationRequest request = PropagationRequest.builder()
                .withMobileAppId(mobileAppInfo.getMobileAppId())
                .withDisplayedAttributes(Collections.emptySet())
                .withTrackingUrl(TRACKING_URL)
                .withTrackingSystem(TrackingSystem.APPMETRICA.name())
                .withPropagationMode(PropagationMode.APPLY_TO_BANNERS_WITH_SAME_TRACKING_URL_AND_REPLACE_CHANGED)
                .build();
        MobileAppUpdatePropagationOperationFactory.Result propagationOperation =
                mobileAppUpdatePropagationOperationFactory
                        .createPropagationOperation(clientInfo.getUid(), clientInfo.getClientId(), request);
        assertThat("Операция не создана", propagationOperation.hasOperation(), equalTo(false));
        assertThat("Вернулась ошибка 'нельзя поменять трекинговую систему с несовместимыми с ней ссылками баннеров'", propagationOperation.getError(), equalTo(
                MobileAppDefects.canNotChangeTrackingSystemWithUntrackableBannerHref()));
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.RMP_CPI_UNDER_KNOWN_TRACKING_SYSTEM_ONLY, false);
    }
}
