package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupSimple;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithContentLanguage;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.ContentLanguage;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.queryrec.model.Language;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds.ContentLanguage.BAD_LANGUAGE;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds.Gen.OPERATOR_CANNOT_SET_CONTENT_LANGUAGE;
import static ru.yandex.direct.core.entity.region.RegionConstants.LANGUAGE_TO_ALLOWED_REGIONS_CONTAINER;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithContentLanguageUpdateValidationTypeSupportTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private RbacService rbacService;
    @Mock
    private GeoTreeFactory geoTreeFactory;
    @Mock
    private AdGroupRepository adGroupRepository;

    private CampaignWithContentLanguageUpdateValidationTypeSupport validationTypeSupport;

    @Mock
    private GeoTree russianGeoTree;

    private ClientId clientId;
    private CampaignValidationContainer container;
    private Long operatorUid;
    private Long campaignId;

    private ModelChanges<CampaignWithContentLanguage> campaignModelChanges;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.MCBANNER},
        });
    }

    @Before
    public void before() {
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        campaignId = RandomNumberUtils.nextPositiveLong();
        operatorUid = RandomNumberUtils.nextPositiveLong();
        container = CampaignValidationContainer.create(0, operatorUid, clientId);

        doReturn(russianGeoTree)
                .when(geoTreeFactory).getRussianGeoTree();
        validationTypeSupport = new CampaignWithContentLanguageUpdateValidationTypeSupport(
                rbacService, geoTreeFactory, adGroupRepository);

        doReturn(true)
                .when(russianGeoTree).isRegionsIncludedIn(anyCollection(), anySet());
        List<AdGroupSimple> adGroups = List.of(new AdGroup().withGeo(List.of()).withId(1L));
        Map<Long, List<AdGroupSimple>> map = Map.of(campaignId, adGroups);
        doReturn(map)
                .when(adGroupRepository).getAdGroupSimpleByCampaignsIds(anyInt(), anyList());
        CampaignWithContentLanguage campaign = createCampaign(ContentLanguage.RU);
        var contentLanguagesWithAvailableRegions = StreamEx.of(List.of(ContentLanguage.values()))
                .filter(c -> LANGUAGE_TO_ALLOWED_REGIONS_CONTAINER.containsKey(Language.getByName(c.getTypedValue())))
                .toList();

        campaignModelChanges =
                ModelChanges.build(campaign, CampaignWithContentLanguage.CONTENT_LANGUAGE,
                        contentLanguagesWithAvailableRegions.get(
                                RandomNumberUtils.nextPositiveInteger(contentLanguagesWithAvailableRegions.size())));
    }

    @Test
    public void preValidate_Successfully() {
        doReturn(RbacRole.SUPER).when(rbacService).getUidRole(operatorUid);

        var vr = validationTypeSupport.preValidate(container, new ValidationResult<>(List.of(campaignModelChanges)));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void preValidate_expectOperatorCannotSetContentLanguage() {
        doReturn(RbacRole.CLIENT).when(rbacService).getUidRole(operatorUid);

        var vr = validationTypeSupport.preValidate(container, new ValidationResult<>(List.of(campaignModelChanges)));
        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithContentLanguage.CONTENT_LANGUAGE)),
                OPERATOR_CANNOT_SET_CONTENT_LANGUAGE)));
    }

    @Test
    public void preValidate_expectBadLanguage() {
        doReturn(RbacRole.SUPER).when(rbacService).getUidRole(operatorUid);
        List<Long> geo = List.of(123L);
        List<AdGroupSimple> adGroups = List.of(new AdGroup().withGeo(geo).withId(2L));
        Map<Long, List<AdGroupSimple>> map = Map.of(campaignId, adGroups);
        doReturn(map)
                .when(adGroupRepository).getAdGroupSimpleByCampaignsIds(anyInt(), anyList());
        doReturn(false)
                .when(russianGeoTree).isRegionsIncludedIn(eq(geo), anySet());

        var vr = validationTypeSupport.preValidate(container, new ValidationResult<>(List.of(campaignModelChanges)));
        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithContentLanguage.CONTENT_LANGUAGE)), BAD_LANGUAGE)));
    }

    private CampaignWithContentLanguage createCampaign(ContentLanguage contentLanguage) {
        CommonCampaign campaign = TestCampaigns.newCampaignByCampaignType(campaignType)
                .withId(campaignId)
                .withClientId(clientId.asLong())
                .withOrderId(RandomNumberUtils.nextPositiveLong())
                .withName("valid_campaign_name")
                .withUid(operatorUid);
        return ((CampaignWithContentLanguage) campaign)
                .withContentLanguage(contentLanguage);
    }
}
