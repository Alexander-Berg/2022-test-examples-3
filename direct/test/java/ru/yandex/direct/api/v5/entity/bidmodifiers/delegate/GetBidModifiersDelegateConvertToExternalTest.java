package ru.yandex.direct.api.v5.entity.bidmodifiers.delegate;

import java.util.List;

import com.yandex.direct.api.v5.bidmodifiers.BidModifierGetItem;
import com.yandex.direct.api.v5.bidmodifiers.BidModifierTypeEnum;
import com.yandex.direct.api.v5.bidmodifiers.DemographicsAdjustmentGet;
import com.yandex.direct.api.v5.bidmodifiers.IncomeGradeAdjustmentGet;
import com.yandex.direct.api.v5.bidmodifiers.MobileAdjustmentGet;
import com.yandex.direct.api.v5.bidmodifiers.ObjectFactory;
import com.yandex.direct.api.v5.bidmodifiers.OperatingSystemTypeEnum;
import com.yandex.direct.api.v5.bidmodifiers.RegionalAdjustmentGet;
import com.yandex.direct.api.v5.bidmodifiers.RetargetingAdjustmentGet;
import com.yandex.direct.api.v5.bidmodifiers.SerpLayoutAdjustmentGet;
import com.yandex.direct.api.v5.bidmodifiers.SmartAdAdjustmentGet;
import com.yandex.direct.api.v5.general.IncomeGradeEnum;
import com.yandex.direct.api.v5.general.SerpLayoutEnum;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierGeo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPerformanceTgo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPerformanceTgoAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPrismaIncomeGrade;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPrismaIncomeGradeAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRegionalAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargeting;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafaretPosition;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafaretPositionAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.GenderType;
import ru.yandex.direct.core.entity.bidmodifier.OsType;
import ru.yandex.direct.core.entity.bidmodifier.TrafaretPosition;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionLiteral;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionOperator;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionParameter;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;

import static com.yandex.direct.api.v5.bidmodifiers.BidModifierLevelEnum.AD_GROUP;
import static com.yandex.direct.api.v5.bidmodifiers.BidModifierLevelEnum.CAMPAIGN;
import static com.yandex.direct.api.v5.general.AgeRangeEnum.AGE_18_24;
import static com.yandex.direct.api.v5.general.GenderEnum.GENDER_FEMALE;
import static com.yandex.direct.api.v5.general.YesNoEnum.NO;
import static com.yandex.direct.api.v5.general.YesNoEnum.YES;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.bidmodifier.AgeType._18_24;
import static ru.yandex.direct.core.entity.bidmodifier.GenderType.FEMALE;

@RunWith(Parameterized.class)
public class GetBidModifiersDelegateConvertToExternalTest {

    private static final long ID = 11L;
    private static final long CAMPAIGN_ID = 22L;
    private static final long AD_GROUP_ID = 33L;
    private static final int PERCENT = 44;
    private static final long REGION_ID = 55L;
    private static final long RETARGETING_CONDITION_ID = 66L;

    private static final ObjectFactory FACTORY = new ObjectFactory();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @InjectMocks
    private GetBidModifiersDelegate delegate;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public BidModifier param;

    @Parameterized.Parameter(2)
    public BidModifierGetItem expected;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] getParameters() {

        return new Object[][]{
                {"MobileBidModifier default",
                        buildMobileBidModifier(),
                        buildDefaultBidModifierGetItem(BidModifierType.MOBILE_MULTIPLIER)
                                .withType(BidModifierTypeEnum.MOBILE_ADJUSTMENT)
                                .withMobileAdjustment(
                                new MobileAdjustmentGet()
                                        .withOperatingSystemType(FACTORY.createMobileAdjustmentGetOperatingSystemType(
                                                OperatingSystemTypeEnum.IOS))
                                        .withBidModifier(PERCENT))},
                {"MobileBidModifier adGroupId=null",
                        buildMobileBidModifier()
                                .withAdGroupId(null),
                        buildDefaultBidModifierGetItem(BidModifierType.MOBILE_MULTIPLIER)
                                .withAdGroupId(FACTORY.createBidModifierGetItemAdGroupId(null))
                                .withLevel(CAMPAIGN)
                                .withType(BidModifierTypeEnum.MOBILE_ADJUSTMENT)
                                .withMobileAdjustment(
                                new MobileAdjustmentGet()
                                        .withOperatingSystemType(FACTORY.createMobileAdjustmentGetOperatingSystemType(
                                                OperatingSystemTypeEnum.IOS))
                                        .withBidModifier(PERCENT))},
                {"GeoBidModifier default",
                        buildGeoBidModifier(true),
                        buildDefaultBidModifierGetItem(BidModifierType.GEO_MULTIPLIER)
                                .withType(BidModifierTypeEnum.REGIONAL_ADJUSTMENT)
                                .withRegionalAdjustment(new RegionalAdjustmentGet()
                                .withEnabled(YES)
                                .withBidModifier(PERCENT)
                                .withRegionId(REGION_ID))},
                {"GeoBidModifier enabled=false",
                        buildGeoBidModifier(false),
                        buildDefaultBidModifierGetItem(BidModifierType.GEO_MULTIPLIER)
                                .withType(BidModifierTypeEnum.REGIONAL_ADJUSTMENT)
                                .withRegionalAdjustment(new RegionalAdjustmentGet()
                                .withEnabled(NO)
                                .withBidModifier(PERCENT)
                                .withRegionId(REGION_ID))},
                {"DemographyBidModifier default",
                        buildDemographyBidModifier(true, FEMALE, _18_24),
                        buildDefaultBidModifierGetItem(BidModifierType.DEMOGRAPHY_MULTIPLIER)
                                .withType(BidModifierTypeEnum.DEMOGRAPHICS_ADJUSTMENT)
                                .withDemographicsAdjustment(new DemographicsAdjustmentGet()
                                .withEnabled(YES)
                                .withBidModifier(PERCENT)
                                .withGender(FACTORY.createDemographicsAdjustmentGetGender(GENDER_FEMALE))
                                .withAge(FACTORY.createDemographicsAdjustmentGetAge(AGE_18_24)))},
                {"DemographyBidModifier enabled=false, gender=null, age=null",
                        buildDemographyBidModifier(false, null, null),
                        buildDefaultBidModifierGetItem(BidModifierType.DEMOGRAPHY_MULTIPLIER)
                                .withType(BidModifierTypeEnum.DEMOGRAPHICS_ADJUSTMENT)
                                .withDemographicsAdjustment(new DemographicsAdjustmentGet()
                                .withEnabled(NO)
                                .withBidModifier(PERCENT)
                                .withGender(FACTORY.createDemographicsAdjustmentGetGender(null))
                                .withAge(FACTORY.createDemographicsAdjustmentGetAge(null)))},
                {"DemographyBidModifier age=unknown",
                        buildDemographyBidModifier(true, FEMALE, AgeType.UNKNOWN),
                        buildDefaultBidModifierGetItem(BidModifierType.DEMOGRAPHY_MULTIPLIER)
                                .withType(BidModifierTypeEnum.DEMOGRAPHICS_ADJUSTMENT)
                                .withDemographicsAdjustment(new DemographicsAdjustmentGet()
                                .withEnabled(YES)
                                .withBidModifier(PERCENT)
                                .withGender(FACTORY.createDemographicsAdjustmentGetGender(GENDER_FEMALE))
                                .withAge(FACTORY.createDemographicsAdjustmentGetAge(null)))},
                {"RetargetingBidModifier default",
                        buildRetargetingBidModifier(true, true),
                        buildDefaultBidModifierGetItem(BidModifierType.RETARGETING_MULTIPLIER)
                                .withType(BidModifierTypeEnum.RETARGETING_ADJUSTMENT)
                                .withRetargetingAdjustment(new RetargetingAdjustmentGet()
                                .withEnabled(YES)
                                .withBidModifier(PERCENT)
                                .withRetargetingConditionId(RETARGETING_CONDITION_ID)
                                .withAccessible(YES))},
                {"RetargetingBidModifier enabled=false, accessible=false",
                        buildRetargetingBidModifier(false, false),
                        buildDefaultBidModifierGetItem(BidModifierType.RETARGETING_MULTIPLIER)
                                .withType(BidModifierTypeEnum.RETARGETING_ADJUSTMENT)
                                .withRetargetingAdjustment(new RetargetingAdjustmentGet()
                                .withEnabled(NO)
                                .withBidModifier(PERCENT)
                                .withRetargetingConditionId(RETARGETING_CONDITION_ID)
                                .withAccessible(NO))},
                {"SmartAdBidModifier default",
                        buildSmartAdBidModifier(),
                        buildDefaultBidModifierGetItem(BidModifierType.PERFORMANCE_TGO_MULTIPLIER)
                                .withType(BidModifierTypeEnum.SMART_AD_ADJUSTMENT)
                                .withSmartAdAdjustment(new SmartAdAdjustmentGet().withBidModifier(PERCENT))},
                {"TrafaretPositionBidModifier enabled=false, trafaretPosition=alone",
                        buildTrafaretPositionBidModifier(false, TrafaretPosition.ALONE),
                        buildDefaultBidModifierGetItem(BidModifierType.TRAFARET_POSITION_MULTIPLIER)
                                .withType(BidModifierTypeEnum.SERP_LAYOUT_ADJUSTMENT)
                                .withSerpLayoutAdjustment(new SerpLayoutAdjustmentGet()
                                .withEnabled(NO)
                                .withBidModifier(PERCENT)
                                .withSerpLayout(SerpLayoutEnum.ALONE))},
                {"TrafaretPositionBidModifier enabled=false, trafaretPosition=suggest",
                        buildTrafaretPositionBidModifier(false, TrafaretPosition.SUGGEST),
                        buildDefaultBidModifierGetItem(BidModifierType.TRAFARET_POSITION_MULTIPLIER)
                                .withType(BidModifierTypeEnum.SERP_LAYOUT_ADJUSTMENT)
                                .withSerpLayoutAdjustment(new SerpLayoutAdjustmentGet()
                                .withEnabled(NO)
                                .withBidModifier(PERCENT)
                                .withSerpLayout(SerpLayoutEnum.SUGGEST))},
                {"TrafaretPositionBidModifier enabled=true, trafaretPosition=alone",
                        buildTrafaretPositionBidModifier(true, TrafaretPosition.ALONE),
                        buildDefaultBidModifierGetItem(BidModifierType.TRAFARET_POSITION_MULTIPLIER)
                                .withType(BidModifierTypeEnum.SERP_LAYOUT_ADJUSTMENT)
                                .withSerpLayoutAdjustment(new SerpLayoutAdjustmentGet()
                                .withEnabled(YES)
                                .withBidModifier(PERCENT)
                                .withSerpLayout(SerpLayoutEnum.ALONE))},
                {"TrafaretPositionBidModifier enabled=true, trafaretPosition=suggest",
                        buildTrafaretPositionBidModifier(true, TrafaretPosition.SUGGEST),
                        buildDefaultBidModifierGetItem(BidModifierType.TRAFARET_POSITION_MULTIPLIER)
                                .withType(BidModifierTypeEnum.SERP_LAYOUT_ADJUSTMENT)
                                .withSerpLayoutAdjustment(new SerpLayoutAdjustmentGet()
                                .withEnabled(YES)
                                .withBidModifier(PERCENT)
                                .withSerpLayout(SerpLayoutEnum.SUGGEST))},
                {"IncomeGradeBidModifier default",
                        buildPrismaIncomeGradeBidModifier(),
                        buildDefaultBidModifierGetItem(BidModifierType.PRISMA_INCOME_GRADE_MULTIPLIER)
                                .withType(BidModifierTypeEnum.INCOME_GRADE_ADJUSTMENT)
                                .withIncomeGradeAdjustment(
                                new IncomeGradeAdjustmentGet()
                                        .withEnabled(YES)
                                        .withBidModifier(PERCENT)
                                        .withGrade(IncomeGradeEnum.HIGH)
                        )
                }
        };
    }

    private static BidModifierMobile buildMobileBidModifier() {
        return new BidModifierMobile()
                .withId(ID)
                .withCampaignId(CAMPAIGN_ID)
                .withAdGroupId(AD_GROUP_ID)
                .withType(BidModifierType.MOBILE_MULTIPLIER)
                .withEnabled(true)
                .withMobileAdjustment(
                        new BidModifierMobileAdjustment().withId(ID).withOsType(OsType.IOS).withPercent(PERCENT));
    }

    private static BidModifierGeo buildGeoBidModifier(boolean enabled) {
        return new BidModifierGeo()
                .withCampaignId(CAMPAIGN_ID)
                .withAdGroupId(AD_GROUP_ID)
                .withType(BidModifierType.GEO_MULTIPLIER)
                .withEnabled(enabled)
                .withRegionalAdjustments(singletonList(
                        new BidModifierRegionalAdjustment()
                                .withId(ID)
                                .withPercent(PERCENT)
                                .withRegionId(REGION_ID)
                                .withHidden(false)
                ));
    }

    private static BidModifierDemographics buildDemographyBidModifier(boolean enabled, GenderType gender, AgeType age) {
        return new BidModifierDemographics()
                .withCampaignId(CAMPAIGN_ID)
                .withAdGroupId(AD_GROUP_ID)
                .withType(BidModifierType.DEMOGRAPHY_MULTIPLIER)
                .withEnabled(enabled)
                .withDemographicsAdjustments(
                        singletonList(
                                new BidModifierDemographicsAdjustment()
                                        .withId(ID)
                                        .withPercent(PERCENT)
                                        .withGender(gender)
                                        .withAge(age)
                        )
                );
    }

    private static BidModifierRetargeting buildRetargetingBidModifier(boolean enabled, boolean accessible) {
        return new BidModifierRetargeting()
                .withCampaignId(CAMPAIGN_ID)
                .withAdGroupId(AD_GROUP_ID)
                .withType(BidModifierType.RETARGETING_MULTIPLIER)
                .withEnabled(enabled)
                .withRetargetingAdjustments(
                        singletonList(
                                new BidModifierRetargetingAdjustment()
                                        .withId(ID)
                                        .withPercent(PERCENT)
                                        .withRetargetingConditionId(RETARGETING_CONDITION_ID)
                                        .withAccessible(accessible)
                        )
                );
    }

    private static BidModifierPerformanceTgo buildSmartAdBidModifier() {
        return new BidModifierPerformanceTgo()
                .withId(ID)
                .withCampaignId(CAMPAIGN_ID)
                .withAdGroupId(AD_GROUP_ID)
                .withType(BidModifierType.PERFORMANCE_TGO_MULTIPLIER)
                .withPerformanceTgoAdjustment(
                        new BidModifierPerformanceTgoAdjustment()
                                .withId(ID)
                                .withPercent(PERCENT)
                );
    }

    private static BidModifierPrismaIncomeGrade buildPrismaIncomeGradeBidModifier() {
        return new BidModifierPrismaIncomeGrade()
                .withId(ID)
                .withAdGroupId(AD_GROUP_ID)
                .withCampaignId(CAMPAIGN_ID)
                .withType(BidModifierType.PRISMA_INCOME_GRADE_MULTIPLIER)
                .withEnabled(true)
                .withExpressionAdjustments(
                        List.of(new BidModifierPrismaIncomeGradeAdjustment()
                                .withId(ID)
                                .withPercent(PERCENT)
                                .withCondition(List.of(List.of(
                                        new BidModifierExpressionLiteral()
                                                .withOperation(BidModifierExpressionOperator.EQ)
                                                .withParameter(BidModifierExpressionParameter.PRISMA_INCOME_GRADE)
                                                .withValueString("1")
                                ))))

                );
    }

    private static BidModifierTrafaretPosition buildTrafaretPositionBidModifier(boolean enabled,
                                                                                TrafaretPosition trafaretPosition) {
        return new BidModifierTrafaretPosition()
                .withCampaignId(CAMPAIGN_ID)
                .withAdGroupId(AD_GROUP_ID)
                .withType(BidModifierType.TRAFARET_POSITION_MULTIPLIER)
                .withEnabled(enabled)
                .withTrafaretPositionAdjustments(
                        singletonList(
                                new BidModifierTrafaretPositionAdjustment()
                                        .withId(ID)
                                        .withPercent(PERCENT)
                                        .withTrafaretPosition(trafaretPosition)
                        )
                );
    }

    private static BidModifierGetItem buildDefaultBidModifierGetItem(BidModifierType type) {
        return new BidModifierGetItem()
                .withId(BidModifierService.getExternalId(ID, type))
                .withCampaignId(CAMPAIGN_ID)
                .withAdGroupId(FACTORY.createBidModifierGetItemAdGroupId(AD_GROUP_ID))
                .withLevel(AD_GROUP);
    }

    @Test
    public void test() {
        BidModifierGetItem actual = delegate.convertToExternal(param).get(0);
        assertThat(actual, beanDiffer(expected));
    }
}
