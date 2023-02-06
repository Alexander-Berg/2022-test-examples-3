package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.adgroups.AdGroupAddItem;
import com.yandex.direct.api.v5.adgroups.AddRequest;
import com.yandex.direct.api.v5.adgroups.AddResponse;
import com.yandex.direct.api.v5.adgroups.ContentPromotionAdGroupAdd;
import com.yandex.direct.api.v5.adgroups.PromotedContentTypeEnum;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.entity.adgroups.AdGroupDefectTypes.inconsistentAdGroupTypeToCampaign;
import static ru.yandex.direct.api.v5.validation.DefectTypes.invalidUseOfField;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@Api5Test
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class AddAdGroupsDelegateContentPromotionServiceTest {
    private static final String NAME = "Test AdGroup";
    private static final List<Long> REGION_IDS = List.of(0L);

    @Autowired
    private Steps steps;
    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private ApiAuthenticationSource auth;
    @Autowired
    private AddAdGroupsDelegate delegate;
    @Autowired
    private GenericApiService genericApiService;

    private int shard;
    private long campaignId;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        when(auth.getOperator()).thenReturn(new ApiUser().withUid(clientInfo.getUid()));
        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientId));

        var campaignInfo = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo);
        campaignId = campaignInfo.getCampaignId();
    }

    @After
    public void resetMocks() {
        Mockito.reset(auth);
    }

    @Test
    public void doAction_ServicesApp_AdGroupCreated() {
        servicesApplication();
        AddRequest request = new AddRequest()
                .withAdGroups(new AdGroupAddItem()
                        .withCampaignId(campaignId)
                        .withName(NAME)
                        .withRegionIds(REGION_IDS)
                        .withContentPromotionAdGroup(new ContentPromotionAdGroupAdd()
                                .withPromotedContentType(PromotedContentTypeEnum.SERVICE)));
        AddResponse response = genericApiService.doAction(delegate, request);

        assumeThat(response.getAddResults(), hasSize(1));

        long adGroupId = response.getAddResults().get(0).getId();
        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, List.of(adGroupId));
        ContentPromotionAdGroup expectedAdGroup = new ContentPromotionAdGroup()
                .withId(adGroupId)
                .withCampaignId(campaignId)
                .withType(AdGroupType.CONTENT_PROMOTION)
                .withContentPromotionType(ContentPromotionAdgroupType.SERVICE)
                .withName(NAME)
                .withGeo(REGION_IDS);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(adGroups).hasSize(1);
            softly.assertThat(adGroups.get(0)).isEqualToIgnoringNullFields(expectedAdGroup);
        });
    }

    @Test
    public void doAction_NotServicesApp_AdGroupNotCreated() {
        AddRequest request = new AddRequest()
                .withAdGroups(new AdGroupAddItem()
                        .withCampaignId(campaignId)
                        .withName(NAME)
                        .withRegionIds(REGION_IDS)
                        .withContentPromotionAdGroup(new ContentPromotionAdGroupAdd()
                                .withPromotedContentType(PromotedContentTypeEnum.SERVICE)));
        AddResponse response = genericApiService.doAction(delegate, request);

        assumeThat(response.getAddResults(), hasSize(1));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getAddResults()).hasSize(1);
            softly.assertThat(response.getAddResults().get(0).getId()).isNull();
            softly.assertThat(response.getAddResults().get(0).getErrors()).hasSize(1);
            softly.assertThat(response.getAddResults().get(0).getErrors().get(0).getCode())
                    .isEqualTo(invalidUseOfField().getCode());
        });
    }

    @Test
    public void doAction_ServicesApp_NotContentPromotionAdGroup_AdGroupNotCreated() {
        AddRequest request = new AddRequest()
                .withAdGroups(new AdGroupAddItem()
                        .withCampaignId(campaignId)
                        .withName(NAME)
                        .withRegionIds(REGION_IDS));
        AddResponse response = genericApiService.doAction(delegate, request);

        assumeThat(response.getAddResults(), hasSize(1));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getAddResults()).hasSize(1);
            softly.assertThat(response.getAddResults().get(0).getId()).isNull();
            softly.assertThat(response.getAddResults().get(0).getErrors()).hasSize(1);
            softly.assertThat(response.getAddResults().get(0).getErrors().get(0).getCode())
                    .isEqualTo(inconsistentAdGroupTypeToCampaign().getCode());
        });
    }

    private void servicesApplication() {
        when(auth.isServicesApplication()).thenReturn(true);
    }
}
