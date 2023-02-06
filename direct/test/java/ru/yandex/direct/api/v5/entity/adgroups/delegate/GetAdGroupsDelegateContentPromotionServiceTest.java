package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.adgroups.AdGroupFieldEnum;
import com.yandex.direct.api.v5.adgroups.AdGroupsSelectionCriteria;
import com.yandex.direct.api.v5.adgroups.ContentPromotionAdGroupFieldEnum;
import com.yandex.direct.api.v5.adgroups.GetRequest;
import com.yandex.direct.api.v5.adgroups.GetResponse;
import com.yandex.direct.api.v5.adgroups.PromotedContentTypeEnum;
import com.yandex.direct.api.v5.adgroups.PromotedContentTypeGetEnum;
import com.yandex.direct.api.v5.general.AdGroupTypesEnum;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.entity.ApiValidationException;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Api5Test
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class GetAdGroupsDelegateContentPromotionServiceTest {
    @Autowired
    private Steps steps;
    @Autowired
    private ApiAuthenticationSource auth;
    @Autowired
    private GetAdGroupsDelegate delegate;
    @Autowired
    private GenericApiService genericApiService;

    private long campaignId;
    private long adGroupId;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();

        when(auth.getOperator()).thenReturn(new ApiUser().withUid(clientInfo.getUid()));
        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientId));
        when(auth.isServicesApplication()).thenReturn(false);

        var campaignInfo = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo);
        campaignId = campaignInfo.getCampaignId();
        var adGroupInfo = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(campaignInfo, ContentPromotionAdgroupType.SERVICE);
        adGroupId = adGroupInfo.getAdGroupId();
    }

    @After
    public void resetMocks() {
        Mockito.reset(auth);
    }

    @Test
    public void doAction_ServicesApp_SelectById_ContentReturned() {
        servicesApplication();
        GetRequest request = new GetRequest()
                .withFieldNames(AdGroupFieldEnum.ID, AdGroupFieldEnum.TYPE)
                .withContentPromotionAdGroupFieldNames(ContentPromotionAdGroupFieldEnum.PROMOTED_CONTENT_TYPE)
                .withSelectionCriteria(new AdGroupsSelectionCriteria().withIds(adGroupId));
        GetResponse response = genericApiService.doAction(delegate, request);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getAdGroups()).hasSize(1);
            softly.assertThat(response.getAdGroups().get(0).getId()).isEqualTo(adGroupId);
            softly.assertThat(response.getAdGroups().get(0).getType())
                    .isEqualTo(AdGroupTypesEnum.CONTENT_PROMOTION_AD_GROUP);
            softly.assertThat(response.getAdGroups().get(0).getContentPromotionAdGroup().getPromotedContentType())
                    .isEqualTo(PromotedContentTypeGetEnum.SERVICE);
        });
    }

    @Test
    public void doAction_NotServicesApp_SelectById_ContentNotReturned() {
        GetRequest request = new GetRequest()
                .withFieldNames(AdGroupFieldEnum.ID, AdGroupFieldEnum.TYPE)
                .withContentPromotionAdGroupFieldNames(ContentPromotionAdGroupFieldEnum.PROMOTED_CONTENT_TYPE)
                .withSelectionCriteria(new AdGroupsSelectionCriteria().withIds(adGroupId));
        GetResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAdGroups()).isEmpty();
    }

    @Test
    public void doAction_ServicesApp_SelectByCampaignId_ContentReturned() {
        servicesApplication();
        GetRequest request = new GetRequest()
                .withFieldNames(AdGroupFieldEnum.ID, AdGroupFieldEnum.TYPE)
                .withContentPromotionAdGroupFieldNames(ContentPromotionAdGroupFieldEnum.PROMOTED_CONTENT_TYPE)
                .withSelectionCriteria(new AdGroupsSelectionCriteria()
                        .withCampaignIds(campaignId));
        GetResponse response = genericApiService.doAction(delegate, request);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getAdGroups()).hasSize(1);
            softly.assertThat(response.getAdGroups().get(0).getId()).isEqualTo(adGroupId);
            softly.assertThat(response.getAdGroups().get(0).getType())
                    .isEqualTo(AdGroupTypesEnum.CONTENT_PROMOTION_AD_GROUP);
            softly.assertThat(response.getAdGroups().get(0).getContentPromotionAdGroup().getPromotedContentType())
                    .isEqualTo(PromotedContentTypeGetEnum.SERVICE);
        });
    }

    @Test
    public void doAction_NotServicesApp_SelectByCampaignId_ContentNotReturned() {
        GetRequest request = new GetRequest()
                .withFieldNames(AdGroupFieldEnum.ID, AdGroupFieldEnum.TYPE)
                .withContentPromotionAdGroupFieldNames(ContentPromotionAdGroupFieldEnum.PROMOTED_CONTENT_TYPE)
                .withSelectionCriteria(new AdGroupsSelectionCriteria()
                        .withCampaignIds(campaignId));
        GetResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAdGroups()).isEmpty();
    }

    @Test
    public void doAction_ServicesApp_SelectByContentPromotionType_ContentReturned() {
        servicesApplication();
        GetRequest request = new GetRequest()
                .withFieldNames(AdGroupFieldEnum.ID, AdGroupFieldEnum.TYPE)
                .withContentPromotionAdGroupFieldNames(ContentPromotionAdGroupFieldEnum.PROMOTED_CONTENT_TYPE)
                .withSelectionCriteria(new AdGroupsSelectionCriteria()
                        .withCampaignIds(campaignId)
                        .withPromotedContentTypes(PromotedContentTypeEnum.SERVICE));
        GetResponse response = genericApiService.doAction(delegate, request);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getAdGroups()).hasSize(1);
            softly.assertThat(response.getAdGroups().get(0).getId()).isEqualTo(adGroupId);
            softly.assertThat(response.getAdGroups().get(0).getType())
                    .isEqualTo(AdGroupTypesEnum.CONTENT_PROMOTION_AD_GROUP);
            softly.assertThat(response.getAdGroups().get(0).getContentPromotionAdGroup().getPromotedContentType())
                    .isEqualTo(PromotedContentTypeGetEnum.SERVICE);
        });
    }

    @Test(expected = ApiValidationException.class)
    public void doAction_NotServicesApp_SelectByContentPromotionType_ExceptionIsThrown() {
        GetRequest request = new GetRequest()
                .withFieldNames(AdGroupFieldEnum.ID, AdGroupFieldEnum.TYPE)
                .withContentPromotionAdGroupFieldNames(ContentPromotionAdGroupFieldEnum.PROMOTED_CONTENT_TYPE)
                .withSelectionCriteria(new AdGroupsSelectionCriteria()
                        .withCampaignIds(campaignId)
                        .withPromotedContentTypes(PromotedContentTypeEnum.SERVICE));
        genericApiService.doAction(delegate, request);
    }

    private void servicesApplication() {
        when(auth.isServicesApplication()).thenReturn(true);
    }
}
