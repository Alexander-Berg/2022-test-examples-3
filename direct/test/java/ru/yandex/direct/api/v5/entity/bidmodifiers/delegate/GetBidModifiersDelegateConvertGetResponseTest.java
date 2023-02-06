package ru.yandex.direct.api.v5.entity.bidmodifiers.delegate;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.yandex.direct.api.v5.bidmodifiers.BidModifierGetItem;
import com.yandex.direct.api.v5.bidmodifiers.BidModifierTypeEnum;
import com.yandex.direct.api.v5.bidmodifiers.DemographicsAdjustmentGet;
import com.yandex.direct.api.v5.bidmodifiers.GetResponse;
import com.yandex.direct.api.v5.bidmodifiers.IncomeGradeAdjustmentGet;
import com.yandex.direct.api.v5.bidmodifiers.MobileAdjustmentGet;
import com.yandex.direct.api.v5.bidmodifiers.ObjectFactory;
import com.yandex.direct.api.v5.bidmodifiers.RegionalAdjustmentGet;
import com.yandex.direct.api.v5.bidmodifiers.RetargetingAdjustmentGet;
import com.yandex.direct.api.v5.bidmodifiers.SerpLayoutAdjustmentGet;
import com.yandex.direct.api.v5.bidmodifiers.SmartAdAdjustmentGet;
import com.yandex.direct.api.v5.general.IncomeGradeEnum;
import com.yandex.direct.api.v5.general.SerpLayoutEnum;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.api.v5.entity.bidmodifiers.validation.GetBidModifiersValidationService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.common.util.PropertyFilter;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;

import static com.yandex.direct.api.v5.bidmodifiers.BidModifierLevelEnum.AD_GROUP;
import static com.yandex.direct.api.v5.general.AgeRangeEnum.AGE_18_24;
import static com.yandex.direct.api.v5.general.GenderEnum.GENDER_FEMALE;
import static com.yandex.direct.api.v5.general.YesNoEnum.YES;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.BID_MODIFIER_AD_GROUP_ID;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.BID_MODIFIER_CAMPAIGN_ID;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.BID_MODIFIER_ID;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.BID_MODIFIER_LEVEL;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.BID_MODIFIER_TYPE;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.DEMOGRAPHICS_ADJUSTMENT_AGE;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.DEMOGRAPHICS_ADJUSTMENT_BID_MODIFIER;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.DEMOGRAPHICS_ADJUSTMENT_ENABLED;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.DEMOGRAPHICS_ADJUSTMENT_GENDER;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.INCOME_GRADE_ADJUSTMENT_BID_MODIFIER;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.INCOME_GRADE_ADJUSTMENT_ENABLED;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.INCOME_GRADE_ADJUSTMENT_GRADE;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.MOBILE_ADJUSTMENT_BID_MODIFIER;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.REGIONAL_ADJUSTMENT_BID_MODIFIER;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.REGIONAL_ADJUSTMENT_ENABLED;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.REGIONAL_ADJUSTMENT_REGION_ID;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.RETARGETING_ADJUSTMENT_ACCESSIBLE;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.RETARGETING_ADJUSTMENT_BID_MODIFIER;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.RETARGETING_ADJUSTMENT_ENABLED;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.RETARGETING_ADJUSTMENT_RETARGETING_CONDITION_ID;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.SERP_LAYOUT_BID_MODIFIER;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.SERP_LAYOUT_ENABLED;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.SERP_LAYOUT_SERP_LAYOUT;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.SMART_AD_ADJUSTMENT_BID_MODIFIER;

@RunWith(Parameterized.class)
public class GetBidModifiersDelegateConvertGetResponseTest {

    private static final long LIMITED_BY = 100L;
    private static final long ID1 = 12L;
    private static final long ID2 = 123L;
    private static final long ID3 = 1234L;
    private static final long ID4 = 12345L;
    private static final long ID5 = 123456L;
    private static final long ID6 = 1234567L;
    private static final long ID7 = 12345678L;
    private static final long CAMPAIGN_ID = 22L;
    private static final long AD_GROUP_ID = 33L;
    private static final int PERCENT = 44;
    private static final long REGION_ID = 55L;
    private static final long RETARGETING_CONDITION_ID = 66L;

    private static final ObjectFactory FACTORY = new ObjectFactory();

    private static final Set<BidModifierAnyFieldEnum> ALL_FIELDS = EnumSet.allOf(BidModifierAnyFieldEnum.class);

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    private GetBidModifiersDelegate getBidModifiersDelegate;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public List<BidModifierGetItem> items;

    @Parameterized.Parameter(2)
    public Set<BidModifierAnyFieldEnum> requestedFields;

    @Parameterized.Parameter(3)
    public List<BidModifierGetItem> expectedItems;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] getParameters() {

        return new Object[][]{
                {"all fields",
                        getInitialItems(),
                        ALL_FIELDS,
                        getInitialItems()
                },
                {"empty items",
                        emptyList(),
                        ALL_FIELDS,
                        emptyList()
                },
                {"empty fields",
                        getInitialItems(),
                        emptySet(),
                        Arrays.asList(
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                        )
                },
                {"BID_MODIFIER_ID",
                        getInitialItems(),
                        ImmutableSet.of(BID_MODIFIER_ID),
                        Arrays.asList(
                                new BidModifierGetItem().withId(ID1),
                                new BidModifierGetItem().withId(ID2),
                                new BidModifierGetItem().withId(ID3),
                                new BidModifierGetItem().withId(ID4),
                                new BidModifierGetItem().withId(ID5),
                                new BidModifierGetItem().withId(ID6),
                                new BidModifierGetItem().withId(ID7)
                        )
                },
                {"BID_MODIFIER_CAMPAIGN_ID",
                        getInitialItems(),
                        ImmutableSet.of(BID_MODIFIER_CAMPAIGN_ID),
                        Arrays.asList(
                                new BidModifierGetItem().withCampaignId(CAMPAIGN_ID),
                                new BidModifierGetItem().withCampaignId(CAMPAIGN_ID),
                                new BidModifierGetItem().withCampaignId(CAMPAIGN_ID),
                                new BidModifierGetItem().withCampaignId(CAMPAIGN_ID),
                                new BidModifierGetItem().withCampaignId(CAMPAIGN_ID),
                                new BidModifierGetItem().withCampaignId(CAMPAIGN_ID),
                                new BidModifierGetItem().withCampaignId(CAMPAIGN_ID)
                        )
                },
                {"BID_MODIFIER_AD_GROUP_ID",
                        getInitialItems(),
                        ImmutableSet.of(BID_MODIFIER_AD_GROUP_ID),
                        Arrays.asList(
                                new BidModifierGetItem()
                                        .withAdGroupId(FACTORY.createBidModifierGetItemAdGroupId(AD_GROUP_ID)),
                                new BidModifierGetItem()
                                        .withAdGroupId(FACTORY.createBidModifierGetItemAdGroupId(AD_GROUP_ID)),
                                new BidModifierGetItem()
                                        .withAdGroupId(FACTORY.createBidModifierGetItemAdGroupId(AD_GROUP_ID)),
                                new BidModifierGetItem()
                                        .withAdGroupId(FACTORY.createBidModifierGetItemAdGroupId(AD_GROUP_ID)),
                                new BidModifierGetItem()
                                        .withAdGroupId(FACTORY.createBidModifierGetItemAdGroupId(AD_GROUP_ID)),
                                new BidModifierGetItem()
                                        .withAdGroupId(FACTORY.createBidModifierGetItemAdGroupId(AD_GROUP_ID)),
                                new BidModifierGetItem()
                                        .withAdGroupId(FACTORY.createBidModifierGetItemAdGroupId(AD_GROUP_ID))
                        )
                },
                {"BID_MODIFIER_LEVEL",
                        getInitialItems(),
                        ImmutableSet.of(BID_MODIFIER_LEVEL),
                        Arrays.asList(
                                new BidModifierGetItem().withLevel(AD_GROUP),
                                new BidModifierGetItem().withLevel(AD_GROUP),
                                new BidModifierGetItem().withLevel(AD_GROUP),
                                new BidModifierGetItem().withLevel(AD_GROUP),
                                new BidModifierGetItem().withLevel(AD_GROUP),
                                new BidModifierGetItem().withLevel(AD_GROUP),
                                new BidModifierGetItem().withLevel(AD_GROUP)
                        )
                },
                {"BID_MODIFIER_TYPE",
                        getInitialItems(),
                        ImmutableSet.of(BID_MODIFIER_TYPE),
                        Arrays.asList(
                                new BidModifierGetItem().withType(BidModifierTypeEnum.MOBILE_ADJUSTMENT),
                                new BidModifierGetItem().withType(BidModifierTypeEnum.REGIONAL_ADJUSTMENT),
                                new BidModifierGetItem().withType(BidModifierTypeEnum.DEMOGRAPHICS_ADJUSTMENT),
                                new BidModifierGetItem().withType(BidModifierTypeEnum.RETARGETING_ADJUSTMENT),
                                new BidModifierGetItem().withType(BidModifierTypeEnum.SMART_AD_ADJUSTMENT),
                                new BidModifierGetItem().withType(BidModifierTypeEnum.SERP_LAYOUT_ADJUSTMENT),
                                new BidModifierGetItem().withType(BidModifierTypeEnum.INCOME_GRADE_ADJUSTMENT)
                        )
                },
                {"MOBILE_ADJUSTMENT_BID_MODIFIER",
                        getInitialItems(),
                        ImmutableSet.of(MOBILE_ADJUSTMENT_BID_MODIFIER),
                        Arrays.asList(
                                new BidModifierGetItem()
                                        .withMobileAdjustment(new MobileAdjustmentGet().withBidModifier(PERCENT)),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                        )
                },
                {"REGIONAL_ADJUSTMENT_REGION_ID",
                        getInitialItems(),
                        ImmutableSet.of(REGIONAL_ADJUSTMENT_REGION_ID),
                        Arrays.asList(
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                                        .withRegionalAdjustment(new RegionalAdjustmentGet()
                                                .withRegionId(REGION_ID)),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                        )
                },
                {"REGIONAL_ADJUSTMENT_BID_MODIFIER",
                        getInitialItems(),
                        ImmutableSet.of(REGIONAL_ADJUSTMENT_BID_MODIFIER),
                        Arrays.asList(
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                                        .withRegionalAdjustment(new RegionalAdjustmentGet()
                                                .withBidModifier(PERCENT)),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                        )
                },
                {"REGIONAL_ADJUSTMENT_ENABLED",
                        getInitialItems(),
                        ImmutableSet.of(REGIONAL_ADJUSTMENT_ENABLED),
                        Arrays.asList(
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                                        .withRegionalAdjustment(new RegionalAdjustmentGet()
                                                .withEnabled(YES)),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                        )
                },
                {"DEMOGRAPHICS_ADJUSTMENT_GENDER",
                        getInitialItems(),
                        ImmutableSet.of(DEMOGRAPHICS_ADJUSTMENT_GENDER),
                        Arrays.asList(
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                                        .withDemographicsAdjustment(new DemographicsAdjustmentGet()
                                                .withGender(
                                                        FACTORY.createDemographicsAdjustmentGetGender(GENDER_FEMALE))),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                        )
                },
                {"DEMOGRAPHICS_ADJUSTMENT_AGE",
                        getInitialItems(),
                        ImmutableSet.of(DEMOGRAPHICS_ADJUSTMENT_AGE),
                        Arrays.asList(
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                                        .withDemographicsAdjustment(new DemographicsAdjustmentGet()
                                                .withAge(FACTORY.createDemographicsAdjustmentGetAge(AGE_18_24))),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                        )
                },
                {"DEMOGRAPHICS_ADJUSTMENT_BID_MODIFIER",
                        getInitialItems(),
                        ImmutableSet.of(DEMOGRAPHICS_ADJUSTMENT_BID_MODIFIER),
                        Arrays.asList(
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                                        .withDemographicsAdjustment(new DemographicsAdjustmentGet()
                                                .withBidModifier(PERCENT)),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                        )
                },
                {"DEMOGRAPHICS_ADJUSTMENT_ENABLED",
                        getInitialItems(),
                        ImmutableSet.of(DEMOGRAPHICS_ADJUSTMENT_ENABLED),
                        Arrays.asList(
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                                        .withDemographicsAdjustment(new DemographicsAdjustmentGet()
                                                .withEnabled(YES)),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                        )
                },
                {"RETARGETING_ADJUSTMENT_RETARGETING_CONDITION_ID",
                        getInitialItems(),
                        ImmutableSet.of(RETARGETING_ADJUSTMENT_RETARGETING_CONDITION_ID),
                        Arrays.asList(
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                                        .withRetargetingAdjustment(new RetargetingAdjustmentGet()
                                                .withRetargetingConditionId(RETARGETING_CONDITION_ID)),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                        )
                },
                {"RETARGETING_ADJUSTMENT_BID_MODIFIER",
                        getInitialItems(),
                        ImmutableSet.of(RETARGETING_ADJUSTMENT_BID_MODIFIER),
                        Arrays.asList(
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                                        .withRetargetingAdjustment(new RetargetingAdjustmentGet()
                                                .withBidModifier(PERCENT)),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                        )
                },
                {"RETARGETING_ADJUSTMENT_ACCESSIBLE",
                        getInitialItems(),
                        ImmutableSet.of(RETARGETING_ADJUSTMENT_ACCESSIBLE),
                        Arrays.asList(
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                                        .withRetargetingAdjustment(new RetargetingAdjustmentGet()
                                                .withAccessible(YES)),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                        )
                },
                {"RETARGETING_ADJUSTMENT_ENABLED",
                        getInitialItems(),
                        ImmutableSet.of(RETARGETING_ADJUSTMENT_ENABLED),
                        Arrays.asList(
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                                        .withRetargetingAdjustment(new RetargetingAdjustmentGet()
                                                .withEnabled(YES)),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                        )
                },
                {"SMART_AD_ADJUSTMENT_BID_MODIFIER",
                        getInitialItems(),
                        ImmutableSet.of(SMART_AD_ADJUSTMENT_BID_MODIFIER),
                        Arrays.asList(
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                                        .withSmartAdAdjustment(new SmartAdAdjustmentGet()
                                                .withBidModifier(PERCENT)),
                                new BidModifierGetItem(),
                                new BidModifierGetItem()
                        )
                },
                {"SERP_LAYOUT_POSITION",
                        getInitialItems(),
                        ImmutableSet.of(SERP_LAYOUT_SERP_LAYOUT),
                        Arrays.asList(
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem().withSerpLayoutAdjustment(new SerpLayoutAdjustmentGet()
                                        .withSerpLayout(SerpLayoutEnum.ALONE)),
                                new BidModifierGetItem()
                        )
                },
                {"SERP_LAYOUT_BID_MODIFIER",
                        getInitialItems(),
                        ImmutableSet.of(SERP_LAYOUT_BID_MODIFIER),
                        Arrays.asList(
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem().withSerpLayoutAdjustment(new SerpLayoutAdjustmentGet()
                                        .withBidModifier(PERCENT)),
                                new BidModifierGetItem()
                        )
                },
                {"SERP_LAYOUT_ENABLED",
                        getInitialItems(),
                        ImmutableSet.of(SERP_LAYOUT_ENABLED),
                        Arrays.asList(
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem().withSerpLayoutAdjustment(new SerpLayoutAdjustmentGet()
                                        .withEnabled(YES)),
                                new BidModifierGetItem()
                        )
                },
                {"INCOME_GRADE_ADJUSTMENT_GRADE",
                        getInitialItems(),
                        ImmutableSet.of(INCOME_GRADE_ADJUSTMENT_GRADE),
                        Arrays.asList(
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem().withIncomeGradeAdjustment(new IncomeGradeAdjustmentGet()
                                        .withGrade(IncomeGradeEnum.HIGH))
                        )
                },
                {"INCOME_GRADE_ADJUSTMENT_BID_MODIFIER",
                        getInitialItems(),
                        ImmutableSet.of(INCOME_GRADE_ADJUSTMENT_BID_MODIFIER),
                        Arrays.asList(
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem().withIncomeGradeAdjustment(new IncomeGradeAdjustmentGet()
                                        .withBidModifier(PERCENT))
                        )
                },
                {"INCOME_GRADE_ADJUSTMENT_ENABLED",
                        getInitialItems(),
                        ImmutableSet.of(INCOME_GRADE_ADJUSTMENT_ENABLED),
                        Arrays.asList(
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem(),
                                new BidModifierGetItem().withIncomeGradeAdjustment(new IncomeGradeAdjustmentGet()
                                        .withEnabled(YES))
                        )
                },
        };
    }

    @Before
    public void before() {
        getBidModifiersDelegate = new GetBidModifiersDelegate(
                mock(ApiAuthenticationSource.class),
                mock(GetBidModifiersValidationService.class),
                mock(BidModifierService.class),
                new PropertyFilter(),
                mock(AdGroupService.class));
    }

    private static BidModifierGetItem buildDefaultBidModifierGetItem() {
        return new BidModifierGetItem()
                .withCampaignId(CAMPAIGN_ID)
                .withAdGroupId(FACTORY.createBidModifierGetItemAdGroupId(AD_GROUP_ID))
                .withLevel(AD_GROUP);
    }


    private static List<BidModifierGetItem> getInitialItems() {
        return Arrays.asList(
                buildDefaultBidModifierGetItem()
                        .withId(ID1)
                        .withType(BidModifierTypeEnum.MOBILE_ADJUSTMENT)
                        .withMobileAdjustment(new MobileAdjustmentGet().withBidModifier(PERCENT)),
                buildDefaultBidModifierGetItem()
                        .withId(ID2)
                        .withType(BidModifierTypeEnum.REGIONAL_ADJUSTMENT)
                        .withRegionalAdjustment(new RegionalAdjustmentGet()
                                .withEnabled(YES)
                                .withBidModifier(PERCENT)
                                .withRegionId(REGION_ID)),
                buildDefaultBidModifierGetItem()
                        .withId(ID3)
                        .withType(BidModifierTypeEnum.DEMOGRAPHICS_ADJUSTMENT)
                        .withDemographicsAdjustment(new DemographicsAdjustmentGet()
                                .withEnabled(YES)
                                .withBidModifier(PERCENT)
                                .withGender(FACTORY.createDemographicsAdjustmentGetGender(GENDER_FEMALE))
                                .withAge(FACTORY.createDemographicsAdjustmentGetAge(AGE_18_24))),
                buildDefaultBidModifierGetItem()
                        .withId(ID4)
                        .withType(BidModifierTypeEnum.RETARGETING_ADJUSTMENT)
                        .withRetargetingAdjustment(new RetargetingAdjustmentGet()
                                .withEnabled(YES)
                                .withBidModifier(PERCENT)
                                .withRetargetingConditionId(RETARGETING_CONDITION_ID)
                                .withAccessible(YES)),
                buildDefaultBidModifierGetItem()
                        .withId(ID5)
                        .withType(BidModifierTypeEnum.SMART_AD_ADJUSTMENT)
                        .withSmartAdAdjustment(new SmartAdAdjustmentGet()
                                .withBidModifier(PERCENT)),
                buildDefaultBidModifierGetItem()
                        .withId(ID6)
                        .withType(BidModifierTypeEnum.SERP_LAYOUT_ADJUSTMENT)
                        .withSerpLayoutAdjustment(new SerpLayoutAdjustmentGet()
                                .withEnabled(YES)
                                .withBidModifier(PERCENT)
                                .withSerpLayout(SerpLayoutEnum.ALONE)),
                buildDefaultBidModifierGetItem()
                        .withId(ID7)
                        .withType(BidModifierTypeEnum.INCOME_GRADE_ADJUSTMENT)
                        .withIncomeGradeAdjustment(new IncomeGradeAdjustmentGet()
                                .withEnabled(YES)
                                .withBidModifier(PERCENT)
                                .withGrade(IncomeGradeEnum.HIGH))
        );
    }

    @Test
    public void test() {
        GetResponse result = getBidModifiersDelegate.convertGetResponse(items, requestedFields, LIMITED_BY);
        assertThat(result,
                beanDiffer(new GetResponse().withBidModifiers(expectedItems).withLimitedBy(LIMITED_BY)));
    }
}
