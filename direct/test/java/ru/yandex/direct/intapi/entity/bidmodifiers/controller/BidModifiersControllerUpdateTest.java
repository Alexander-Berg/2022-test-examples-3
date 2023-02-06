package ru.yandex.direct.intapi.entity.bidmodifiers.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierBannerType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventory;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafaretPosition;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.OsType;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.feature.service.FeatureManagingService;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannerTypeMultiplierValuesBannerType;
import ru.yandex.direct.dbschema.ppc.enums.InventoryMultiplierValuesInventoryType;
import ru.yandex.direct.dbschema.ppc.enums.TrafaretPositionMultiplierValuesTrafaretPosition;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.validation.model.IntapiValidationResponse;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.web.core.entity.bidmodifiers.BannerTypeModifierConverter;
import ru.yandex.direct.web.core.entity.bidmodifiers.InventoryModifierConverter;
import ru.yandex.direct.web.core.entity.bidmodifiers.TrafaretPositionModifierConverter;
import ru.yandex.direct.web.core.entity.bidmodifiers.model.BidModifierBannerTypeConditionWeb;
import ru.yandex.direct.web.core.entity.bidmodifiers.model.BidModifierBannerTypeWeb;
import ru.yandex.direct.web.core.entity.bidmodifiers.model.BidModifierInventoryConditionWeb;
import ru.yandex.direct.web.core.entity.bidmodifiers.model.BidModifierInventoryWeb;
import ru.yandex.direct.web.core.entity.bidmodifiers.model.BidModifierMobileWeb;
import ru.yandex.direct.web.core.entity.bidmodifiers.model.BidModifierSingleWeb;
import ru.yandex.direct.web.core.entity.bidmodifiers.model.BidModifierTrafaretPositionConditionWeb;
import ru.yandex.direct.web.core.entity.bidmodifiers.model.BidModifierTrafaretPositionWeb;
import ru.yandex.direct.web.core.entity.bidmodifiers.model.ComplexBidModifierWeb;
import ru.yandex.direct.web.core.model.WebResponse;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierDesktop;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierMobile;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultIosBidModifierMobile;
import static ru.yandex.direct.utils.CommonUtils.nvl;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.utils.JsonUtils.fromJson;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BidModifiersControllerUpdateTest {
    private static final String DESKTOP_ADJUSTMENT_FIELD = "desktopAdjustment";
    private static final String MOBILE_ADJUSTMENT_FIELD = "mobileAdjustment";
    private static final String PERCENT_FIELD = "percent";
    private static final String OS_TYPE_FIELD = "osType";
    private static final int NEW_PERCENT = 1000;

    @Autowired
    private Steps steps;

    @Autowired
    private FeatureManagingService featureManagingService;

    @Autowired
    private BidModifiersController controller;

    @Autowired
    private BidModifierService bidModifierService;

    private MockMvc mockMvc;

    private CampaignInfo campaignInfo;
    private AdGroupInfo adGroupInfo;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        clientInfo = steps.clientSteps().createDefaultClient();
        campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
    }

    @Test
    public void updateCampaignLevelDesktopBidModifiers() throws Exception {
        steps.bidModifierSteps().createCampaignBidModifier(
                createDefaultBidModifierDesktop(campaignInfo.getCampaignId()), campaignInfo);

        update(new ComplexBidModifierWeb()
                .withDesktopModifier(
                        new BidModifierSingleWeb()
                                .withPercent(NEW_PERCENT)));

        List<BidModifier> modifiers = getModifiersAtCampaign(BidModifierType.DESKTOP_MULTIPLIER);
        assertThat(modifiers, hasSize(1));
        assertThat(modifiers, contains(
                hasProperty(DESKTOP_ADJUSTMENT_FIELD,
                        hasProperty(PERCENT_FIELD, equalTo(NEW_PERCENT)))
        ));
    }

    @Test
    public void deleteCampaignLevelDesktopBidModifiers() throws Exception {
        steps.bidModifierSteps().createCampaignBidModifier(
                createDefaultBidModifierDesktop(campaignInfo.getCampaignId()), campaignInfo);

        update(new ComplexBidModifierWeb());

        List<BidModifier> modifiers = getModifiersAtCampaign(BidModifierType.DESKTOP_MULTIPLIER);
        assertThat(modifiers, hasSize(0));
    }

    @Test
    public void updateGroupLevelDesktopBidModifiers() throws Exception {
        steps.bidModifierSteps().createAdGroupBidModifier(
                createDefaultBidModifierDesktop(campaignInfo.getCampaignId()), adGroupInfo);

        updateGroup(new ComplexBidModifierWeb()
                .withDesktopModifier(
                        new BidModifierSingleWeb()
                                .withPercent(NEW_PERCENT)));

        List<BidModifier> modifiers = getModifiersAtAdGroup(BidModifierType.DESKTOP_MULTIPLIER);
        assertThat(modifiers, hasSize(1));
        assertThat(modifiers, contains(
                hasProperty(DESKTOP_ADJUSTMENT_FIELD,
                        hasProperty(PERCENT_FIELD, equalTo(NEW_PERCENT)))
        ));
    }

    @Test
    public void deleteGroupLevelDesktopBidModifiers() throws Exception {
        steps.bidModifierSteps().createAdGroupBidModifier(
                createDefaultBidModifierDesktop(campaignInfo.getCampaignId()), adGroupInfo);

        updateGroup(new ComplexBidModifierWeb());

        List<BidModifier> modifiers = getModifiersAtAdGroup(BidModifierType.DESKTOP_MULTIPLIER);
        assertThat(modifiers, hasSize(0));
    }

    @Test
    public void updateCampaignLevelMobileBidModifiers() throws Exception {
        steps.bidModifierSteps().createCampaignBidModifier(
                createDefaultBidModifierMobile(campaignInfo.getCampaignId()), campaignInfo);

        update(new ComplexBidModifierWeb()
                .withMobileModifier(
                        new BidModifierMobileWeb()
                                .withPercent(NEW_PERCENT)));

        List<BidModifier> modifiers = getModifiersAtCampaign(BidModifierType.MOBILE_MULTIPLIER);
        assertThat(modifiers, hasSize(1));
        assertThat(modifiers, contains(
                hasProperty(MOBILE_ADJUSTMENT_FIELD,
                        hasProperty(PERCENT_FIELD, equalTo(NEW_PERCENT)))
        ));
    }

    @Test
    public void updateOsTypeToAndroidAtCampaignLevelMobileBidModifiers() throws Exception {
        steps.bidModifierSteps().createCampaignBidModifier(
                createDefaultBidModifierMobile(campaignInfo.getCampaignId()), campaignInfo);

        update(new ComplexBidModifierWeb()
                .withMobileModifier(
                        new BidModifierMobileWeb()
                                .withOsType("android")
                                .withPercent(NEW_PERCENT)));

        List<BidModifier> modifiers = getModifiersAtCampaign(BidModifierType.MOBILE_MULTIPLIER);
        assertThat(modifiers, hasSize(1));
        assertThat(modifiers, contains(
                hasProperty(MOBILE_ADJUSTMENT_FIELD,
                        allOf(
                                hasProperty(OS_TYPE_FIELD, equalTo(OsType.ANDROID)),
                                hasProperty(PERCENT_FIELD, equalTo(NEW_PERCENT))
                        ))
        ));
    }

    @Test
    public void updateOsTypeToNullAtCampaignLevelMobileBidModifiers() throws Exception {
        steps.bidModifierSteps().createCampaignBidModifier(
                createDefaultIosBidModifierMobile(campaignInfo.getCampaignId()), campaignInfo);

        update(new ComplexBidModifierWeb()
                .withMobileModifier(
                        new BidModifierMobileWeb()
                                .withOsType(null)
                                .withPercent(NEW_PERCENT)));

        List<BidModifier> modifiers = getModifiersAtCampaign(BidModifierType.MOBILE_MULTIPLIER);
        assertThat(modifiers, hasSize(1));
        assertThat(modifiers, contains(
                hasProperty(MOBILE_ADJUSTMENT_FIELD,
                        allOf(
                                hasProperty(OS_TYPE_FIELD, nullValue()),
                                hasProperty(PERCENT_FIELD, equalTo(NEW_PERCENT))
                        ))
        ));
    }

    @Test
    public void deleteCampaignLevelMobileBidModifiers() throws Exception {
        steps.bidModifierSteps().createCampaignBidModifier(
                createDefaultBidModifierMobile(campaignInfo.getCampaignId()), campaignInfo);

        update(new ComplexBidModifierWeb());

        List<BidModifier> modifiers = getModifiersAtCampaign(BidModifierType.MOBILE_MULTIPLIER);
        assertThat(modifiers, hasSize(0));
    }

    @Test
    public void updateGroupLevelMobileBidModifiers() throws Exception {
        steps.bidModifierSteps().createAdGroupBidModifier(
                createDefaultBidModifierMobile(campaignInfo.getCampaignId()), adGroupInfo);

        updateGroup(new ComplexBidModifierWeb()
                .withMobileModifier(
                        new BidModifierMobileWeb()
                                .withPercent(NEW_PERCENT)));

        List<BidModifier> modifiers = getModifiersAtAdGroup(BidModifierType.MOBILE_MULTIPLIER);

        assertThat(modifiers, hasSize(1));
        assertThat(modifiers, contains(
                hasProperty(MOBILE_ADJUSTMENT_FIELD,
                        hasProperty(PERCENT_FIELD, equalTo(NEW_PERCENT)))
        ));
    }

    @Test
    public void updateOsTypeToAndroidAtGroupLevelMobileBidModifiers() throws Exception {
        steps.bidModifierSteps().createAdGroupBidModifier(
                createDefaultBidModifierMobile(campaignInfo.getCampaignId()), adGroupInfo);

        updateGroup(new ComplexBidModifierWeb()
                .withMobileModifier(
                        new BidModifierMobileWeb()
                                .withOsType("android")
                                .withPercent(NEW_PERCENT)));

        List<BidModifier> modifiers = getModifiersAtAdGroup(BidModifierType.MOBILE_MULTIPLIER);
        assertThat(modifiers, hasSize(1));
        assertThat(modifiers, contains(
                hasProperty(MOBILE_ADJUSTMENT_FIELD,
                        allOf(
                                hasProperty(OS_TYPE_FIELD, equalTo(OsType.ANDROID)),
                                hasProperty(PERCENT_FIELD, equalTo(NEW_PERCENT))
                        ))
        ));
    }

    @Test
    public void updateOsTypeToNullAtGroupLevelMobileBidModifiers() throws Exception {
        steps.bidModifierSteps().createAdGroupBidModifier(
                createDefaultIosBidModifierMobile(campaignInfo.getCampaignId()), adGroupInfo);

        updateGroup(new ComplexBidModifierWeb()
                .withMobileModifier(
                        new BidModifierMobileWeb()
                                .withOsType(null)
                                .withPercent(NEW_PERCENT)));

        List<BidModifier> modifiers = getModifiersAtAdGroup(BidModifierType.MOBILE_MULTIPLIER);
        assertThat(modifiers, hasSize(1));
        assertThat(modifiers, contains(
                hasProperty(MOBILE_ADJUSTMENT_FIELD,
                        allOf(
                                hasProperty(OS_TYPE_FIELD, nullValue()),
                                hasProperty(PERCENT_FIELD, equalTo(NEW_PERCENT))
                        ))
        ));
    }

    @Test
    public void deleteGroupLevelMobileBidModifiers() throws Exception {
        steps.bidModifierSteps().createAdGroupBidModifier(
                createDefaultBidModifierMobile(campaignInfo.getCampaignId()), adGroupInfo);

        updateGroup(new ComplexBidModifierWeb());

        List<BidModifier> modifiers = getModifiersAtAdGroup(BidModifierType.MOBILE_MULTIPLIER);
        assertThat(modifiers, hasSize(0));
    }

    @Test
    public void updateCampaignLevelBannerTypeModifiers() throws Exception {
        CampaignInfo cpmBannerCampaign = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        ComplexBidModifierWeb updateRequest = new ComplexBidModifierWeb()
                .withBannerTypeModifier(new BidModifierBannerTypeWeb().withConditions(asList(
                        new BidModifierBannerTypeConditionWeb()
                                .withMultiplierType(
                                        BannerTypeMultiplierValuesBannerType.cpm_banner.getLiteral())
                                .withPercent(120),
                        new BidModifierBannerTypeConditionWeb()
                                .withMultiplierType(
                                        BannerTypeMultiplierValuesBannerType.cpm_outdoor.getLiteral())
                                .withPercent(230)))
                        .withEnabled(1));
        update(updateRequest, cpmBannerCampaign.getCampaignId(), "CPM_BANNER");

        List<BidModifier> modifiers =
                getModifiersAtCampaign(BidModifierType.BANNER_TYPE_MULTIPLIER, cpmBannerCampaign.getCampaignId());

        BidModifier expected = new BidModifierBannerType()
                .withCampaignId(cpmBannerCampaign.getCampaignId())
                .withType(BidModifierType.BANNER_TYPE_MULTIPLIER)
                .withBannerTypeAdjustments(mapList(updateRequest.getBannerTypeModifier().getConditions(),
                        BannerTypeModifierConverter::convert))
                .withEnabled(true);
        assertThat(modifiers, hasSize(1));
        assertThat(modifiers.get(0), beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void updateCampaignLevelInventoryTypeModifiers() throws Exception {
        CampaignInfo cpmBannerCampaign = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        ComplexBidModifierWeb updateRequest = new ComplexBidModifierWeb()
                .withInventoryModifier(new BidModifierInventoryWeb().withConditions(asList(
                        new BidModifierInventoryConditionWeb()
                                .withMultiplierType(
                                        InventoryMultiplierValuesInventoryType.instream_web.getLiteral())
                                .withPercent(120),
                        new BidModifierInventoryConditionWeb()
                                .withMultiplierType(
                                        InventoryMultiplierValuesInventoryType.inpage.getLiteral())
                                .withPercent(230),
                        new BidModifierInventoryConditionWeb()
                                .withMultiplierType(
                                        InventoryMultiplierValuesInventoryType.inapp.getLiteral())
                                .withPercent(320),
                        new BidModifierInventoryConditionWeb()
                                .withMultiplierType(
                                        InventoryMultiplierValuesInventoryType.inbanner.getLiteral())
                                .withPercent(450),
                        new BidModifierInventoryConditionWeb()
                                .withMultiplierType(
                                        InventoryMultiplierValuesInventoryType.rewarded.getLiteral())
                                .withPercent(570)
                )).withEnabled(1));
        update(updateRequest, cpmBannerCampaign.getCampaignId(), "CPM_BANNER");

        List<BidModifier> modifiers =
                getModifiersAtCampaign(BidModifierType.INVENTORY_MULTIPLIER, cpmBannerCampaign.getCampaignId());

        BidModifier expected = new BidModifierInventory()
                .withCampaignId(cpmBannerCampaign.getCampaignId())
                .withType(BidModifierType.INVENTORY_MULTIPLIER)
                .withInventoryAdjustments(mapList(updateRequest.getInventoryModifier().getConditions(),
                        InventoryModifierConverter::convert))
                .withEnabled(true);
        assertThat(modifiers, hasSize(1));
        assertThat(modifiers.get(0), beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void updateInventoryAndBannerTypeModifiersAllToZero_Error() throws Exception {
        CampaignInfo cpmBannerCampaign = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        ComplexBidModifierWeb updateRequest = new ComplexBidModifierWeb()
                .withInventoryModifier(new BidModifierInventoryWeb().withConditions(asList(
                        new BidModifierInventoryConditionWeb()
                                .withMultiplierType(
                                        InventoryMultiplierValuesInventoryType.inpage.getLiteral())
                                .withPercent(0),
                        new BidModifierInventoryConditionWeb()
                                .withMultiplierType(
                                        InventoryMultiplierValuesInventoryType.instream_web.getLiteral())
                                .withPercent(0),
                        new BidModifierInventoryConditionWeb()
                                .withMultiplierType(
                                        InventoryMultiplierValuesInventoryType.interstitial.getLiteral())
                                .withPercent(0),
                        new BidModifierInventoryConditionWeb()
                                .withMultiplierType(
                                        InventoryMultiplierValuesInventoryType.inapp.getLiteral())
                                .withPercent(0),
                        new BidModifierInventoryConditionWeb()
                                .withMultiplierType(
                                        InventoryMultiplierValuesInventoryType.inbanner.getLiteral())
                                .withPercent(0),
                        new BidModifierInventoryConditionWeb()
                                .withMultiplierType(
                                        InventoryMultiplierValuesInventoryType.rewarded.getLiteral())
                                .withPercent(0),
                        new BidModifierInventoryConditionWeb()
                                .withMultiplierType(
                                        InventoryMultiplierValuesInventoryType.preroll.getLiteral())
                                .withPercent(0),
                        new BidModifierInventoryConditionWeb()
                                .withMultiplierType(
                                        InventoryMultiplierValuesInventoryType.midroll.getLiteral())
                                .withPercent(0),
                        new BidModifierInventoryConditionWeb()
                                .withMultiplierType(
                                        InventoryMultiplierValuesInventoryType.postroll.getLiteral())
                                .withPercent(0),
                        new BidModifierInventoryConditionWeb()
                                .withMultiplierType(
                                        InventoryMultiplierValuesInventoryType.pauseroll.getLiteral())
                                .withPercent(0),
                        new BidModifierInventoryConditionWeb()
                                .withMultiplierType(
                                        InventoryMultiplierValuesInventoryType.overlay.getLiteral())
                                .withPercent(0),
                        new BidModifierInventoryConditionWeb()
                                .withMultiplierType(
                                        InventoryMultiplierValuesInventoryType.postroll_overlay.getLiteral())
                                .withPercent(0),
                        new BidModifierInventoryConditionWeb()
                                .withMultiplierType(
                                        InventoryMultiplierValuesInventoryType.postroll_wrapper.getLiteral())
                                .withPercent(0),
                        new BidModifierInventoryConditionWeb()
                                .withMultiplierType(
                                        InventoryMultiplierValuesInventoryType.inroll_overlay.getLiteral())
                                .withPercent(0),
                        new BidModifierInventoryConditionWeb()
                                .withMultiplierType(
                                        InventoryMultiplierValuesInventoryType.inroll.getLiteral())
                                .withPercent(0),
                        new BidModifierInventoryConditionWeb()
                                .withMultiplierType(
                                        InventoryMultiplierValuesInventoryType.fullscreen.getLiteral())
                                .withPercent(0)
                )))
                .withBannerTypeModifier(new BidModifierBannerTypeWeb().withConditions(asList(
                        new BidModifierBannerTypeConditionWeb()
                                .withMultiplierType(
                                        BannerTypeMultiplierValuesBannerType.cpm_banner.getLiteral())
                                .withPercent(0),
                        new BidModifierBannerTypeConditionWeb()
                                .withMultiplierType(
                                        BannerTypeMultiplierValuesBannerType.cpm_outdoor.getLiteral())
                                .withPercent(0)))
                        .withEnabled(1));
        WebResponse webResponse = controller
                .updateBidModifiers(updateRequest, cpmBannerCampaign.getCampaignId(), null, CampaignType.CPM_BANNER);
        assertThat(webResponse.isSuccessful(), is(false));
        assertThat("Ответ instanceof IntapiValidationResponse", webResponse,
                instanceOf(IntapiValidationResponse.class));
        IntapiValidationResponse webResponseError = (IntapiValidationResponse) webResponse;
        assertEquals("BidModifiersDefectIds.GeneralDefects.DEVICE_BID_MODIFIERS_ALL_ZEROS",
                webResponseError.validationResult().getErrors().get(0).getCode());
    }

    /**
     * Проверка, что отправка в ручку всех возможных корректировок на cpm_outdoor группу правильно сохраняет их в БД.
     */
    @Test
    public void setOutdoorGroupBidModifiers() throws Exception {
        campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        adGroupInfo = steps.adGroupSteps().createActiveCpmOutdoorAdGroup(campaignInfo);
        // генерировал этот файл так:
        // * выставил на бете в cpm_outdoor группе все корректировки
        // * запустил перл многострочник
        //   perl -Mmy_inc=for,protected -MHierarchicalMultipliers -MDDP -MJSON -E '
        //       my $h = HierarchicalMultipliers::mass_get_hierarchical_multipliers([{pid => 4622411346}]);
        //       *JavaIntapi::UpdateBidModifiers::call = sub { say to_json($_[0]->_prepare()) };
        //       JavaIntapi::UpdateBidModifiers->new(
        //          %{$h->[0]},campaign_id => 64451176, adgroup_id => 4622411346, campaign_type => "cpm_banner"
        //       )->call();
        //   '
        //  - он читает корректировки из базы и перехватывает значение, которое отправляется в ручку bidmodifiers.update
        // * отформатировал и сохранил в файл
        var bmJson = LiveResourceFactory.get("classpath:///bidmodifiers/full_cpm_outdoor_modifiers.json").getContent();
        updateGroupJson(bmJson, CampaignType.CPM_BANNER);

        // тест читает ожидаемое значение из снепшота, т.к. очень лениво конструировать гигантскую структуру в коде.
        var bidModifiers = getAllAdGroupModifiers();
        var bidModifiersJson = toJson(bidModifiers);
        var bidModifiersBean = fromJson(bidModifiersJson, List.class);
        // а этот файл взял в отладчике из переменной bidModifiersJson, убедился что все значения правильные
        // и тоже отформатировал
        var expectedJson = LiveResourceFactory
                .get("classpath:///bidmodifiers/full_cpm_outdoor_modifiers_db_snapshot.json")
                .getContent();
        var expectedBean = fromJson(expectedJson, List.class);
        assertThat(bidModifiersBean, beanDiffer(expectedBean).useCompareStrategy(
                allFieldsExcept(
                        BeanFieldPath.newPath(".*/id"),
                        BeanFieldPath.newPath(".*/campaignId"),
                        BeanFieldPath.newPath(".*/adGroupId"),
                        BeanFieldPath.newPath(".*/lastChange")
                )
        ));
    }

    @Test
    public void updateCampaignLevelTrafaretPositionModifiers() throws Exception {
        CampaignInfo textCampaign = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        ComplexBidModifierWeb updateRequest = new ComplexBidModifierWeb()
                .withTrafaretPositionModifier(new BidModifierTrafaretPositionWeb().withConditions(asList(
                        new BidModifierTrafaretPositionConditionWeb()
                                .withMultiplierType(
                                        TrafaretPositionMultiplierValuesTrafaretPosition.alone.getLiteral())
                                .withPercent(120),
                        new BidModifierTrafaretPositionConditionWeb()
                                .withMultiplierType(
                                        TrafaretPositionMultiplierValuesTrafaretPosition.suggest.getLiteral())
                                .withPercent(230)
                )).withEnabled(1));
        update(updateRequest, textCampaign.getCampaignId(), "TEXT");

        List<BidModifier> modifiers =
                getModifiersAtCampaign(BidModifierType.TRAFARET_POSITION_MULTIPLIER, textCampaign.getCampaignId());

        BidModifier expected = new BidModifierTrafaretPosition()
                .withCampaignId(textCampaign.getCampaignId())
                .withType(BidModifierType.TRAFARET_POSITION_MULTIPLIER)
                .withTrafaretPositionAdjustments(mapList(updateRequest.getTrafaretPositionModifier().getConditions(),
                        TrafaretPositionModifierConverter::convert))
                .withEnabled(true);
        assertThat(modifiers, hasSize(1));
        assertThat(modifiers.get(0), beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    private List<BidModifier> getModifiersAtCampaign(BidModifierType type) {
        return getModifiersAtCampaign(type, null);
    }

    private List<BidModifier> getModifiersAtCampaign(BidModifierType type, @Nullable Long campaignId) {
        return bidModifierService.getByCampaignIds(
                clientInfo.getClientId(), singleton(nvl(campaignId, campaignInfo.getCampaignId())),
                singleton(type),
                singleton(BidModifierLevel.CAMPAIGN), clientInfo.getUid());
    }

    private List<BidModifier> getModifiersAtAdGroup(BidModifierType type) {
        return bidModifierService.getByAdGroupIds(
                clientInfo.getClientId(), singleton(adGroupInfo.getAdGroupId()),
                emptySet(), singleton(type),
                singleton(BidModifierLevel.ADGROUP), clientInfo.getUid());
    }

    private List<BidModifier> getAllAdGroupModifiers() {
        var allBidModifiers = bidModifierService.getByAdGroupIds(
                clientInfo.getClientId(),
                singleton(adGroupInfo.getAdGroupId()),
                emptySet(), Set.of(BidModifierType.values()), singleton(BidModifierLevel.ADGROUP), clientInfo.getUid());
        allBidModifiers.sort(Comparator.comparingInt(bm -> bm.getType().ordinal()));
        return allBidModifiers;
    }

    private void update(ComplexBidModifierWeb updateRequest) throws Exception {
        update(updateRequest, null, null);
    }

    private void update(ComplexBidModifierWeb updateRequest, @Nullable Long campaignId, @Nullable String campaignType)
            throws Exception {
        mockMvc.perform(post("/bidmodifiers/update")
                .param("campaignId", Long.toString(nvl(campaignId, campaignInfo.getCampaignId())))
                .param("campaignType", nvl(campaignType, "TEXT"))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json("{'success': true}"));
    }

    private void updateGroup(ComplexBidModifierWeb updateRequest) throws Exception {
        updateGroupJson(toJson(updateRequest), CampaignType.TEXT);
    }

    private void updateGroupJson(String requestContent, CampaignType campaignType) throws Exception {
        mockMvc.perform(post("/bidmodifiers/update")
                .param("campaignId", Long.toString(campaignInfo.getCampaignId()))
                .param("adGroupId", Long.toString(adGroupInfo.getAdGroupId()))
                .param("campaignType", campaignType.toString())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(requestContent))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json("{'success': true}"));
    }
}
