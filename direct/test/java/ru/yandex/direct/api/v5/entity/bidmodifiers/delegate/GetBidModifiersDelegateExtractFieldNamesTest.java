package ru.yandex.direct.api.v5.entity.bidmodifiers.delegate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.yandex.direct.api.v5.bidmodifiers.BidModifierFieldEnum;
import com.yandex.direct.api.v5.bidmodifiers.DemographicsAdjustmentFieldEnum;
import com.yandex.direct.api.v5.bidmodifiers.DesktopAdjustmentFieldEnum;
import com.yandex.direct.api.v5.bidmodifiers.DesktopOnlyAdjustmentFieldEnum;
import com.yandex.direct.api.v5.bidmodifiers.GetRequest;
import com.yandex.direct.api.v5.bidmodifiers.IncomeGradeAdjustmentFieldEnum;
import com.yandex.direct.api.v5.bidmodifiers.MobileAdjustmentFieldEnum;
import com.yandex.direct.api.v5.bidmodifiers.RegionalAdjustmentFieldEnum;
import com.yandex.direct.api.v5.bidmodifiers.RetargetingAdjustmentFieldEnum;
import com.yandex.direct.api.v5.bidmodifiers.SerpLayoutAdjustmentFieldEnum;
import com.yandex.direct.api.v5.bidmodifiers.SmartAdAdjustmentFieldEnum;
import com.yandex.direct.api.v5.bidmodifiers.SmartTvAdjustmentFieldEnum;
import com.yandex.direct.api.v5.bidmodifiers.TabletAdjustmentFieldEnum;
import com.yandex.direct.api.v5.bidmodifiers.VideoAdjustmentFieldEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.entity.bidmodifiers.validation.GetBidModifiersValidationService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.common.util.PropertyFilter;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.BID_MODIFIER_AD_GROUP_ID;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.BID_MODIFIER_CAMPAIGN_ID;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.BID_MODIFIER_ID;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.BID_MODIFIER_LEVEL;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.BID_MODIFIER_TYPE;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.DEMOGRAPHICS_ADJUSTMENT_AGE;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.DEMOGRAPHICS_ADJUSTMENT_BID_MODIFIER;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.DEMOGRAPHICS_ADJUSTMENT_ENABLED;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.DEMOGRAPHICS_ADJUSTMENT_GENDER;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.DESKTOP_ADJUSTMENT_BID_MODIFIER;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.DESKTOP_ONLY_ADJUSTMENT_BID_MODIFIER;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.INCOME_GRADE_ADJUSTMENT_BID_MODIFIER;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.INCOME_GRADE_ADJUSTMENT_ENABLED;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.INCOME_GRADE_ADJUSTMENT_GRADE;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.MOBILE_ADJUSTMENT_BID_MODIFIER;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.MOBILE_ADJUSTMENT_OPERATING_SYSTEM_TYPE;
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
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.TABLET_ADJUSTMENT_BID_MODIFIER;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.TABLET_ADJUSTMENT_OPERATING_SYSTEM_TYPE;

@RunWith(Parameterized.class)
public class GetBidModifiersDelegateExtractFieldNamesTest {

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public GetRequest request;

    @Parameterized.Parameter(2)
    public Set<BidModifierAnyFieldEnum> expectedFields;

    private GetBidModifiersDelegate delegate;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] getParameters() {

        List<BidModifierFieldEnum> fieldNames = asList(BidModifierFieldEnum.values());
        List<BidModifierFieldEnum> fieldNamesWithDuplicates = new ArrayList<>(fieldNames);
        fieldNamesWithDuplicates.addAll(fieldNames);

        List<MobileAdjustmentFieldEnum> mobileFieldNames = asList(MobileAdjustmentFieldEnum.values());
        List<MobileAdjustmentFieldEnum> mobileFieldNamesWithDuplicates = new ArrayList<>(mobileFieldNames);
        mobileFieldNamesWithDuplicates.addAll(mobileFieldNames);

        List<TabletAdjustmentFieldEnum> tabletFieldNames = asList(TabletAdjustmentFieldEnum.values());
        List<TabletAdjustmentFieldEnum> tabletFieldNamesWithDuplicates = new ArrayList<>(tabletFieldNames);
        tabletFieldNamesWithDuplicates.addAll(tabletFieldNames);

        List<DesktopAdjustmentFieldEnum> desktopFieldNames = asList(DesktopAdjustmentFieldEnum.values());
        List<DesktopAdjustmentFieldEnum> desktopFieldNamesWithDuplicates = new ArrayList<>(desktopFieldNames);
        desktopFieldNamesWithDuplicates.addAll(desktopFieldNames);

        List<DesktopOnlyAdjustmentFieldEnum> desktopOnlyFieldNames = asList(DesktopOnlyAdjustmentFieldEnum.values());
        List<DesktopOnlyAdjustmentFieldEnum> desktopOnlyFieldNamesWithDuplicates =
                    new ArrayList<>(desktopOnlyFieldNames);
        desktopOnlyFieldNamesWithDuplicates.addAll(desktopOnlyFieldNames);

        List<SmartTvAdjustmentFieldEnum> smartTvFieldNames = asList(SmartTvAdjustmentFieldEnum.values());
        List<SmartTvAdjustmentFieldEnum> smartTvFieldNamesWithDuplicates = new ArrayList<>(smartTvFieldNames);
        smartTvFieldNamesWithDuplicates.addAll(smartTvFieldNames);

        List<RegionalAdjustmentFieldEnum> regionalFieldNames = asList(RegionalAdjustmentFieldEnum.values());
        List<RegionalAdjustmentFieldEnum> regionalFieldNamesWithDuplicates = new ArrayList<>(regionalFieldNames);
        regionalFieldNamesWithDuplicates.addAll(regionalFieldNames);

        List<DemographicsAdjustmentFieldEnum> demographicsFieldNames = asList(DemographicsAdjustmentFieldEnum.values());
        List<DemographicsAdjustmentFieldEnum> demographicsFieldNamesWithDuplicates
                = new ArrayList<>(demographicsFieldNames);
        demographicsFieldNamesWithDuplicates.addAll(demographicsFieldNames);

        List<RetargetingAdjustmentFieldEnum> retargetingFieldNames = asList(RetargetingAdjustmentFieldEnum.values());
        List<RetargetingAdjustmentFieldEnum> retargetingFieldNamesWithDuplicates
                = new ArrayList<>(retargetingFieldNames);
        retargetingFieldNamesWithDuplicates.addAll(retargetingFieldNames);

        List<VideoAdjustmentFieldEnum> videoFieldNames = asList(VideoAdjustmentFieldEnum.values());
        List<VideoAdjustmentFieldEnum> videoFieldNamesWithDuplicates =
                new ArrayList<>(videoFieldNames);
        videoFieldNamesWithDuplicates.addAll(videoFieldNames);

        List<SmartAdAdjustmentFieldEnum> smartAdFieldNames = asList(SmartAdAdjustmentFieldEnum.values());
        List<SmartAdAdjustmentFieldEnum> smartAdFieldNamesWithDuplicates = new ArrayList<>(smartAdFieldNames);
        smartAdFieldNamesWithDuplicates.addAll(smartAdFieldNames);

        List<SerpLayoutAdjustmentFieldEnum> positionFieldNames = asList(SerpLayoutAdjustmentFieldEnum.values());
        List<SerpLayoutAdjustmentFieldEnum> positionFieldNamesWithDuplicates = new ArrayList<>(positionFieldNames);

        List<IncomeGradeAdjustmentFieldEnum> incomeGradeFieldNames = asList(IncomeGradeAdjustmentFieldEnum.values());
        List<IncomeGradeAdjustmentFieldEnum> incomeGradeFieldNamesWithDuplicates =
                new ArrayList<>(incomeGradeFieldNames);
        incomeGradeFieldNamesWithDuplicates.addAll(incomeGradeFieldNames);

        positionFieldNamesWithDuplicates.addAll(positionFieldNames);

        Set<BidModifierAnyFieldEnum> fieldNamesSet = ImmutableSet.of(BID_MODIFIER_ID, BID_MODIFIER_CAMPAIGN_ID,
                BID_MODIFIER_AD_GROUP_ID, BID_MODIFIER_LEVEL, BID_MODIFIER_TYPE);

        Set<BidModifierAnyFieldEnum> mobileFieldNamesSet = ImmutableSet.of(
                MOBILE_ADJUSTMENT_BID_MODIFIER, MOBILE_ADJUSTMENT_OPERATING_SYSTEM_TYPE);
        Set<BidModifierAnyFieldEnum> tabletFieldNamesSet = ImmutableSet.of(
                TABLET_ADJUSTMENT_BID_MODIFIER, TABLET_ADJUSTMENT_OPERATING_SYSTEM_TYPE);
        Set<BidModifierAnyFieldEnum> desktopFieldNamesSet = ImmutableSet.of(DESKTOP_ADJUSTMENT_BID_MODIFIER);
        Set<BidModifierAnyFieldEnum> desktopOnlyFieldNamesSet = ImmutableSet.of(DESKTOP_ONLY_ADJUSTMENT_BID_MODIFIER);
        Set<BidModifierAnyFieldEnum> regionalFieldNamesSet = ImmutableSet.of(REGIONAL_ADJUSTMENT_REGION_ID,
                REGIONAL_ADJUSTMENT_BID_MODIFIER, REGIONAL_ADJUSTMENT_ENABLED);
        Set<BidModifierAnyFieldEnum> demographicsFieldNamesSet = ImmutableSet.of(DEMOGRAPHICS_ADJUSTMENT_GENDER,
                DEMOGRAPHICS_ADJUSTMENT_AGE, DEMOGRAPHICS_ADJUSTMENT_BID_MODIFIER, DEMOGRAPHICS_ADJUSTMENT_ENABLED);
        Set<BidModifierAnyFieldEnum> retargetingFieldNamesSet = ImmutableSet.of(
                RETARGETING_ADJUSTMENT_RETARGETING_CONDITION_ID, RETARGETING_ADJUSTMENT_BID_MODIFIER,
                RETARGETING_ADJUSTMENT_ACCESSIBLE, RETARGETING_ADJUSTMENT_ENABLED);

        Set<BidModifierAnyFieldEnum> smartAdFieldNamesSet = ImmutableSet.of(SMART_AD_ADJUSTMENT_BID_MODIFIER);

        Set<BidModifierAnyFieldEnum> positionFieldNamesSet = ImmutableSet.of(SERP_LAYOUT_SERP_LAYOUT,
                SERP_LAYOUT_BID_MODIFIER, SERP_LAYOUT_ENABLED);
        Set<BidModifierAnyFieldEnum> incomeGradeFieldNamesSet = ImmutableSet.of(
                INCOME_GRADE_ADJUSTMENT_GRADE,
                INCOME_GRADE_ADJUSTMENT_BID_MODIFIER,
                INCOME_GRADE_ADJUSTMENT_ENABLED
        );

        Set<BidModifierAnyFieldEnum> allFieldNamesSet = ImmutableSet.copyOf(BidModifierAnyFieldEnum.values());

        return new Object[][]{
                {"empty request", new GetRequest(), emptySet()},
                {"with field names", new GetRequest().withFieldNames(fieldNames), fieldNamesSet},
                {"with mobile field names", new GetRequest()
                        .withMobileAdjustmentFieldNames(mobileFieldNames),
                        mobileFieldNamesSet},
                {"with tablet field names", new GetRequest()
                        .withTabletAdjustmentFieldNames(tabletFieldNames),
                        tabletFieldNamesSet},
                {"with desktop field names", new GetRequest()
                        .withDesktopAdjustmentFieldNames(desktopFieldNames),
                        desktopFieldNamesSet},
                {"with desktop only field names", new GetRequest()
                        .withDesktopOnlyAdjustmentFieldNames(desktopOnlyFieldNames),
                        desktopOnlyFieldNamesSet},
                {"with field names", new GetRequest()
                        .withRegionalAdjustmentFieldNames(regionalFieldNames), regionalFieldNamesSet},
                {"with demographics field names", new GetRequest()
                        .withDemographicsAdjustmentFieldNames(demographicsFieldNames), demographicsFieldNamesSet},
                {"with retargeting field names", new GetRequest()
                        .withRetargetingAdjustmentFieldNames(retargetingFieldNames), retargetingFieldNamesSet},
                {"with smart ad field names", new GetRequest()
                        .withSmartAdAdjustmentFieldNames(smartAdFieldNames), smartAdFieldNamesSet},
                {"with income grade field names", new GetRequest()
                        .withIncomeGradeAdjustmentFieldNames(incomeGradeFieldNames), incomeGradeFieldNamesSet},
                {"with position field names", new GetRequest()
                        .withSerpLayoutAdjustmentFieldNames(positionFieldNames), positionFieldNamesSet},
                {"with all field names",
                        new GetRequest()
                                .withFieldNames(fieldNames)
                                .withMobileAdjustmentFieldNames(mobileFieldNames)
                                .withTabletAdjustmentFieldNames(tabletFieldNames)
                                .withDesktopAdjustmentFieldNames(desktopFieldNames)
                                .withDesktopOnlyAdjustmentFieldNames(desktopOnlyFieldNames)
                                .withSmartTvAdjustmentFieldNames(smartTvFieldNames)
                                .withRegionalAdjustmentFieldNames(regionalFieldNames)
                                .withDemographicsAdjustmentFieldNames(demographicsFieldNames)
                                .withRetargetingAdjustmentFieldNames(retargetingFieldNames)
                                .withVideoAdjustmentFieldNames(videoFieldNames)
                                .withSmartAdAdjustmentFieldNames(smartAdFieldNames)
                                .withSerpLayoutAdjustmentFieldNames(positionFieldNames)
                                .withIncomeGradeAdjustmentFieldNames(incomeGradeFieldNames),
                        allFieldNamesSet},
                {"with all field names with duplicates",
                        new GetRequest()
                                .withFieldNames(fieldNamesWithDuplicates)
                                .withMobileAdjustmentFieldNames(mobileFieldNamesWithDuplicates)
                                .withTabletAdjustmentFieldNames(tabletFieldNamesWithDuplicates)
                                .withDesktopAdjustmentFieldNames(desktopFieldNamesWithDuplicates)
                                .withDesktopOnlyAdjustmentFieldNames(desktopOnlyFieldNamesWithDuplicates)
                                .withSmartTvAdjustmentFieldNames(smartTvFieldNamesWithDuplicates)
                                .withRegionalAdjustmentFieldNames(regionalFieldNamesWithDuplicates)
                                .withDemographicsAdjustmentFieldNames(demographicsFieldNamesWithDuplicates)
                                .withRetargetingAdjustmentFieldNames(retargetingFieldNamesWithDuplicates)
                                .withVideoAdjustmentFieldNames(videoFieldNamesWithDuplicates)
                                .withSmartAdAdjustmentFieldNames(smartAdFieldNamesWithDuplicates)
                                .withSerpLayoutAdjustmentFieldNames(positionFieldNamesWithDuplicates)
                                .withIncomeGradeAdjustmentFieldNames(incomeGradeFieldNamesWithDuplicates),
                        allFieldNamesSet},
        };
    }

    @Before
    public void before() {
        delegate = new GetBidModifiersDelegate(
                mock(ApiAuthenticationSource.class),
                mock(GetBidModifiersValidationService.class),
                mock(BidModifierService.class),
                new PropertyFilter(),
                mock(AdGroupService.class));
    }

    @Test
    public void test() {
        assertThat(delegate.extractFieldNames(request)).containsExactlyInAnyOrderElementsOf(expectedFields);
    }
}
