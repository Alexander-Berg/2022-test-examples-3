package ru.yandex.direct.api.v5.entity.ads.delegate;

import java.util.Collections;

import com.yandex.direct.api.v5.ads.AdAddItem;
import com.yandex.direct.api.v5.ads.AdFieldEnum;
import com.yandex.direct.api.v5.ads.AdUpdateItem;
import com.yandex.direct.api.v5.ads.AddRequest;
import com.yandex.direct.api.v5.ads.AdsSelectionCriteria;
import com.yandex.direct.api.v5.ads.GetRequest;
import com.yandex.direct.api.v5.ads.MobileAppAdAdd;
import com.yandex.direct.api.v5.ads.MobileAppAdUpdate;
import com.yandex.direct.api.v5.ads.ObjectFactory;
import com.yandex.direct.api.v5.ads.TextAdAdd;
import com.yandex.direct.api.v5.ads.TextAdFieldEnum;
import com.yandex.direct.api.v5.ads.TextAdUpdate;
import com.yandex.direct.api.v5.ads.UpdateRequest;
import com.yandex.direct.api.v5.ads.VideoExtensionAddItem;
import com.yandex.direct.api.v5.ads.VideoExtensionUpdateItem;
import com.yandex.direct.api.v5.general.YesNoEnum;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.security.utils.ApiAuthenticationSourceMockBuilder;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.entity.user.repository.ApiUserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultVideoAddition;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;

@Api5Test
@RunWith(SpringRunner.class)
public class ShowTitleAndBodyApiTest {
    private static final ObjectFactory FACTORY = new ObjectFactory();

    @Autowired
    private Steps steps;
    @Autowired
    ApiUserRepository apiUserRepository;
    @Autowired
    ApiAuthenticationSource apiAuthenticationSourceMock;
    @Autowired
    AddAdsDelegate addAdsDelegate;
    @Autowired
    UpdateAdsDelegate updateAdsDelegate;
    @Autowired
    GetAdsDelegate getAdsDelegate;
    private ClientInfo clientInfo;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();

        ApiUser operatorUser = apiUserRepository.fetchByUid(clientInfo.getShard(), clientInfo.getUid());
        new ApiAuthenticationSourceMockBuilder()
                .withOperator(operatorUser)
                .tuneAuthSourceMock(apiAuthenticationSourceMock);
    }

    @Test
    public void addWithoutFeatureValidationError() {
        var adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.DISABLE_VIDEO_CREATIVE, false);
        Creative creative = defaultVideoAddition(null, null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, adGroupInfo.getClientInfo());

        AdAddItem item = new AdAddItem()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withTextAd(new TextAdAdd()
                        .withTitle("Title")
                        .withText("Text")
                        .withMobile(YesNoEnum.NO)
                        .withHref("https://ya.ru")
                        .withVideoExtension(new VideoExtensionAddItem()
                                .withCreativeId(creativeInfo.getCreativeId())
                                .withShowTitleAndBody(YesNoEnum.YES)
                        )
                );
        AddRequest addRequest = new AddRequest().withAds(Collections.singletonList(item));
        var apiResult = addAdsDelegate.processList(addAdsDelegate.convertRequest(addRequest));
        var errors = apiResult.getResult().get(0).getErrors();
        MatcherAssert.assertThat(errors, hasSize(1));
        MatcherAssert.assertThat(errors, contains(validationError(5005)));
    }

    @Test
    public void addWithoutFeatureSuccess() {
        var adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.DISABLE_VIDEO_CREATIVE, true);
        Creative creative = defaultVideoAddition(null, null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, adGroupInfo.getClientInfo());

        AdAddItem item = new AdAddItem()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withTextAd(new TextAdAdd()
                        .withTitle("Title")
                        .withText("Text")
                        .withMobile(YesNoEnum.NO)
                        .withHref("https://ya.ru")
                        .withVideoExtension(new VideoExtensionAddItem()
                                .withCreativeId(creativeInfo.getCreativeId())
                                .withShowTitleAndBody(YesNoEnum.YES)
                        )
                );
        AddRequest addRequest = new AddRequest().withAds(Collections.singletonList(item));
        var apiResult = addAdsDelegate.processList(addAdsDelegate.convertRequest(addRequest));
        var errors = apiResult.getResult().get(0).getErrors();
        MatcherAssert.assertThat(errors, hasSize(0));
        Long adId = apiResult.getResult().get(0).getResult();
        GetRequest getRequest = new GetRequest()
                .withSelectionCriteria(new AdsSelectionCriteria().withIds(adId))
                .withTextAdFieldNames(TextAdFieldEnum.VIDEO_EXTENSION)
                .withFieldNames(AdFieldEnum.ID);
        var getResult = getAdsDelegate.convertGetResponse(
                getAdsDelegate.get(getAdsDelegate.convertRequest(getRequest)),
                getAdsDelegate.extractFieldNames(getRequest),
                100L
        );
        MatcherAssert.assertThat(getResult.getAds(), hasSize(1));
        MatcherAssert.assertThat(getResult.getAds().get(0).getTextAd(), notNullValue());
        MatcherAssert.assertThat(getResult.getAds().get(0).getTextAd().getVideoExtension(), notNullValue());
        MatcherAssert.assertThat(
                getResult.getAds().get(0).getTextAd().getVideoExtension().getValue().getShowTitleAndBody(),
                equalTo(YesNoEnum.YES)
        );
    }

    @Test
    public void addNoFlagFalseValueReturned() {
        var adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.DISABLE_VIDEO_CREATIVE, false);
        Creative creative = defaultVideoAddition(null, null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, adGroupInfo.getClientInfo());

        AdAddItem item = new AdAddItem()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withTextAd(new TextAdAdd()
                        .withTitle("Title")
                        .withText("Text")
                        .withMobile(YesNoEnum.NO)
                        .withHref("https://ya.ru")
                        .withVideoExtension(new VideoExtensionAddItem()
                                .withCreativeId(creativeInfo.getCreativeId())
                        )
                );
        AddRequest addRequest = new AddRequest().withAds(Collections.singletonList(item));
        var apiResult = addAdsDelegate.processList(addAdsDelegate.convertRequest(addRequest));
        var errors = apiResult.getResult().get(0).getErrors();
        MatcherAssert.assertThat(errors, hasSize(0));
        Long adId = apiResult.getResult().get(0).getResult();
        GetRequest getRequest = new GetRequest()
                .withSelectionCriteria(new AdsSelectionCriteria().withIds(adId))
                .withTextAdFieldNames(TextAdFieldEnum.VIDEO_EXTENSION)
                .withFieldNames(AdFieldEnum.ID);
        var getResult = getAdsDelegate.convertGetResponse(
                getAdsDelegate.get(getAdsDelegate.convertRequest(getRequest)),
                getAdsDelegate.extractFieldNames(getRequest),
                100L
        );
        MatcherAssert.assertThat(getResult.getAds(), hasSize(1));
        MatcherAssert.assertThat(getResult.getAds().get(0).getTextAd(), notNullValue());
        MatcherAssert.assertThat(getResult.getAds().get(0).getTextAd().getVideoExtension(), notNullValue());
        MatcherAssert.assertThat(
                getResult.getAds().get(0).getTextAd().getVideoExtension().getValue().getShowTitleAndBody(),
                equalTo(YesNoEnum.NO)
        );
    }

    @Test
    public void updateWithoutFeatureValidationError() {
        var bannerInfo = steps.textBannerSteps()
                .createBanner(new NewTextBannerInfo().withClientInfo(clientInfo));
        Long adId = bannerInfo.getBannerId();
        steps.featureSteps().addClientFeature(bannerInfo.getClientId(), FeatureName.DISABLE_VIDEO_CREATIVE, false);
        Creative creative = defaultVideoAddition(null, null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, bannerInfo.getClientInfo());

        AdUpdateItem item = new AdUpdateItem()
                .withId(adId)
                .withTextAd(new TextAdUpdate()
                        .withVideoExtension(new VideoExtensionUpdateItem()
                                .withCreativeId(FACTORY.createVideoExtensionUpdateItemCreativeId(
                                        creativeInfo.getCreativeId())
                                )
                                .withShowTitleAndBody(YesNoEnum.YES)
                        )
                );
        UpdateRequest updateRequest = new UpdateRequest().withAds(Collections.singletonList(item));
        var apiResult = updateAdsDelegate.processList(updateAdsDelegate.convertRequest(updateRequest));
        var errors = apiResult.getResult().get(0).getErrors();
        MatcherAssert.assertThat(errors, hasSize(1));
        MatcherAssert.assertThat(errors, contains(validationError(5005)));
    }

    @Test
    public void updateWithoutFeatureCreativeIdUnchangedSuccess() {
        Creative creative = defaultVideoAddition(null, null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        var bannerInfo = steps.textBannerSteps()
                .createBanner(new NewTextBannerInfo()
                        .withClientInfo(clientInfo)
                        .withBanner(fullTextBanner()
                                .withCreativeId(creativeInfo.getCreativeId())
                                .withCreativeStatusModerate(BannerCreativeStatusModerate.READY)
                        )
                );
        Long adId = bannerInfo.getBannerId();
        steps.featureSteps().addClientFeature(bannerInfo.getClientId(), FeatureName.DISABLE_VIDEO_CREATIVE, true);

        AdUpdateItem item = new AdUpdateItem()
                .withId(adId)
                .withTextAd(new TextAdUpdate()
                        .withVideoExtension(new VideoExtensionUpdateItem().withShowTitleAndBody(YesNoEnum.YES))
                );
        UpdateRequest updateRequest = new UpdateRequest().withAds(Collections.singletonList(item));
        var apiResult = updateAdsDelegate.processList(updateAdsDelegate.convertRequest(updateRequest));
        var errors = apiResult.getResult().get(0).getErrors();
        MatcherAssert.assertThat(errors, hasSize(0));
        GetRequest getRequest = new GetRequest()
                .withSelectionCriteria(new AdsSelectionCriteria().withIds(adId))
                .withTextAdFieldNames(TextAdFieldEnum.VIDEO_EXTENSION)
                .withFieldNames(AdFieldEnum.ID);
        var getResult = getAdsDelegate.convertGetResponse(
                getAdsDelegate.get(getAdsDelegate.convertRequest(getRequest)),
                getAdsDelegate.extractFieldNames(getRequest),
                100L
        );
        MatcherAssert.assertThat(getResult.getAds(), hasSize(1));
        MatcherAssert.assertThat(getResult.getAds().get(0).getTextAd(), notNullValue());
        MatcherAssert.assertThat(getResult.getAds().get(0).getTextAd().getVideoExtension(), notNullValue());
        MatcherAssert.assertThat(
                getResult.getAds().get(0).getTextAd().getVideoExtension().getValue().getShowTitleAndBody(),
                equalTo(YesNoEnum.YES)
        );
        MatcherAssert.assertThat(
                getResult.getAds().get(0).getTextAd().getVideoExtension().getValue().getCreativeId(),
                equalTo(creativeInfo.getCreativeId())
        );
    }

    @Test
    public void notUpdateFlagFlagUnchanged() {
        Creative creative = defaultVideoAddition(null, null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        CreativeInfo anotherCreativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.DISABLE_VIDEO_CREATIVE, true);
        var bannerInfo = steps.textBannerSteps()
                .createBanner(new NewTextBannerInfo()
                        .withClientInfo(clientInfo)
                        .withBanner(fullTextBanner()
                                .withShowTitleAndBody(true)
                                .withCreativeId(creativeInfo.getCreativeId())
                                .withCreativeStatusModerate(BannerCreativeStatusModerate.READY)
                        )
                );
        Long adId = bannerInfo.getBannerId();

        AdUpdateItem item = new AdUpdateItem()
                .withId(adId)
                .withTextAd(new TextAdUpdate()
                        .withVideoExtension(new VideoExtensionUpdateItem()
                                .withCreativeId(FACTORY.createVideoExtensionUpdateItemCreativeId(
                                        anotherCreativeInfo.getCreativeId())
                                )
                        )
                );
        UpdateRequest updateRequest = new UpdateRequest().withAds(Collections.singletonList(item));
        var apiResult = updateAdsDelegate.processList(updateAdsDelegate.convertRequest(updateRequest));
        var errors = apiResult.getResult().get(0).getErrors();
        MatcherAssert.assertThat(errors, hasSize(0));
        GetRequest getRequest = new GetRequest()
                .withSelectionCriteria(new AdsSelectionCriteria().withIds(adId))
                .withTextAdFieldNames(TextAdFieldEnum.VIDEO_EXTENSION)
                .withFieldNames(AdFieldEnum.ID);
        var getResult = getAdsDelegate.convertGetResponse(
                getAdsDelegate.get(getAdsDelegate.convertRequest(getRequest)),
                getAdsDelegate.extractFieldNames(getRequest),
                100L
        );
        MatcherAssert.assertThat(getResult.getAds(), hasSize(1));
        MatcherAssert.assertThat(getResult.getAds().get(0).getTextAd(), notNullValue());
        MatcherAssert.assertThat(getResult.getAds().get(0).getTextAd().getVideoExtension(), notNullValue());
        MatcherAssert.assertThat(
                getResult.getAds().get(0).getTextAd().getVideoExtension().getValue().getShowTitleAndBody(),
                equalTo(YesNoEnum.YES)
        );
        MatcherAssert.assertThat(
                getResult.getAds().get(0).getTextAd().getVideoExtension().getValue().getCreativeId(),
                equalTo(anotherCreativeInfo.getCreativeId())
        );
    }

    @Test
    public void updateYesFlagToNo() {
        Creative creative = defaultVideoAddition(null, null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.DISABLE_VIDEO_CREATIVE, true);
        var bannerInfo = steps.textBannerSteps()
                .createBanner(new NewTextBannerInfo()
                        .withClientInfo(clientInfo)
                        .withBanner(fullTextBanner()
                                .withShowTitleAndBody(true)
                                .withCreativeId(creativeInfo.getCreativeId())
                                .withCreativeStatusModerate(BannerCreativeStatusModerate.READY)
                        )
                );
        Long adId = bannerInfo.getBannerId();

        AdUpdateItem item = new AdUpdateItem()
                .withId(adId)
                .withTextAd(new TextAdUpdate()
                        .withVideoExtension(new VideoExtensionUpdateItem()
                                .withShowTitleAndBody(YesNoEnum.NO)
                        )
                );
        UpdateRequest updateRequest = new UpdateRequest().withAds(Collections.singletonList(item));
        var apiResult = updateAdsDelegate.processList(updateAdsDelegate.convertRequest(updateRequest));
        var errors = apiResult.getResult().get(0).getErrors();
        MatcherAssert.assertThat(errors, hasSize(0));
        GetRequest getRequest = new GetRequest()
                .withSelectionCriteria(new AdsSelectionCriteria().withIds(adId))
                .withTextAdFieldNames(TextAdFieldEnum.VIDEO_EXTENSION)
                .withFieldNames(AdFieldEnum.ID);
        var getResult = getAdsDelegate.convertGetResponse(
                getAdsDelegate.get(getAdsDelegate.convertRequest(getRequest)),
                getAdsDelegate.extractFieldNames(getRequest),
                100L
        );
        MatcherAssert.assertThat(getResult.getAds(), hasSize(1));
        MatcherAssert.assertThat(getResult.getAds().get(0).getTextAd(), notNullValue());
        MatcherAssert.assertThat(getResult.getAds().get(0).getTextAd().getVideoExtension(), notNullValue());
        MatcherAssert.assertThat(
                getResult.getAds().get(0).getTextAd().getVideoExtension().getValue().getShowTitleAndBody(),
                equalTo(YesNoEnum.NO)
        );
        MatcherAssert.assertThat(
                getResult.getAds().get(0).getTextAd().getVideoExtension().getValue().getCreativeId(),
                equalTo(creativeInfo.getCreativeId())
        );
    }

    @Test
    public void addForMobileAppAdValidationError() {
        var adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(clientInfo);
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.DISABLE_VIDEO_CREATIVE, true);
        Creative creative = defaultVideoAddition(null, null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, adGroupInfo.getClientInfo());

        AdAddItem item = new AdAddItem()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withMobileAppAd(new MobileAppAdAdd()
                        .withTitle("Title")
                        .withText("Text")
                        .withVideoExtension(new VideoExtensionAddItem()
                                .withCreativeId(creativeInfo.getCreativeId())
                                .withShowTitleAndBody(YesNoEnum.YES)
                        )
                );
        AddRequest addRequest = new AddRequest().withAds(Collections.singletonList(item));
        var validationResult = addAdsDelegate.validateRequest(addRequest);
        var errors = validationResult.flattenErrors();
        MatcherAssert.assertThat(errors, hasSize(1));
        MatcherAssert.assertThat(errors, contains(validationError(5005)));
    }

    @Test
    public void updateForMobileAppAdValidationError() {
        var adId = steps.mobileAppBannerSteps()
                .createDefaultMobileAppBanner(clientInfo, new AdGroupInfo())
                .getBannerId();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.DISABLE_VIDEO_CREATIVE, true);
        Creative creative = defaultVideoAddition(null, null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);

        AdUpdateItem item = new AdUpdateItem()
                .withId(adId)
                .withMobileAppAd(new MobileAppAdUpdate()
                        .withVideoExtension(new VideoExtensionUpdateItem()
                                .withCreativeId(
                                        FACTORY.createVideoExtensionUpdateItemCreativeId(creativeInfo.getCreativeId())
                                )
                                .withShowTitleAndBody(YesNoEnum.YES)
                        )
                );
        UpdateRequest updateRequest = new UpdateRequest().withAds(Collections.singletonList(item));
        var validationResult = updateAdsDelegate.validateRequest(updateRequest);
        var errors = validationResult.flattenErrors();
        MatcherAssert.assertThat(errors, hasSize(1));
        MatcherAssert.assertThat(errors, contains(validationError(5005)));
    }
}
