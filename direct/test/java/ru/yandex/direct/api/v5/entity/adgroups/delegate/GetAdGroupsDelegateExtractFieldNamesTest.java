package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.adgroups.AdGroupFieldEnum;
import com.yandex.direct.api.v5.adgroups.ContentPromotionAdGroupFieldEnum;
import com.yandex.direct.api.v5.adgroups.DynamicTextAdGroupFieldEnum;
import com.yandex.direct.api.v5.adgroups.DynamicTextFeedAdGroupFieldEnum;
import com.yandex.direct.api.v5.adgroups.GetRequest;
import com.yandex.direct.api.v5.adgroups.MobileAppAdGroupFieldEnum;
import com.yandex.direct.api.v5.adgroups.SmartAdGroupFieldEnum;
import com.yandex.direct.api.v5.adgroups.TextAdGroupFeedParamsFieldEnum;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

@Api5Test
@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class GetAdGroupsDelegateExtractFieldNamesTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public GetRequest request;

    @Parameterized.Parameter(2)
    public Set<AdGroupAnyFieldEnum> expectedFields;

    @Autowired
    private GetAdGroupsDelegate delegate;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] getParameters() {
        List<AdGroupFieldEnum> listForFieldNames = Arrays.asList(AdGroupFieldEnum.values());
        List<AdGroupFieldEnum> listForFieldNamesWithDuplicates = new ArrayList<>(listForFieldNames);
        listForFieldNamesWithDuplicates.addAll(listForFieldNames);
        Set<AdGroupAnyFieldEnum> setForFieldNames = Arrays.stream(AdGroupFieldEnum.values())
                .map(AdGroupAnyFieldEnum::fromAdGroupFieldEnum).collect(toSet());

        List<MobileAppAdGroupFieldEnum> listForMobileAppFieldNames = Arrays.asList(MobileAppAdGroupFieldEnum.values());
        List<MobileAppAdGroupFieldEnum> listForMobileAppFieldNamesWithDuplicates =
                new ArrayList<>(listForMobileAppFieldNames);
        listForMobileAppFieldNamesWithDuplicates.addAll(listForMobileAppFieldNames);
        Set<AdGroupAnyFieldEnum> setForMobileAppFieldNames = Arrays.stream(MobileAppAdGroupFieldEnum.values())
                .map(AdGroupAnyFieldEnum::fromMobileAppAdGroupFieldEnum).collect(toSet());

        List<DynamicTextAdGroupFieldEnum> listForDynamicTextFieldNames =
                Arrays.asList(DynamicTextAdGroupFieldEnum.values());
        List<DynamicTextAdGroupFieldEnum> listForDynamicTextFieldNamesWithDuplicates =
                new ArrayList<>(listForDynamicTextFieldNames);
        listForDynamicTextFieldNamesWithDuplicates.addAll(listForDynamicTextFieldNames);
        Set<AdGroupAnyFieldEnum> setForDynamicTextFieldNames = Arrays.stream(DynamicTextAdGroupFieldEnum.values())
                .map(AdGroupAnyFieldEnum::fromDynamicTextAdGroupFieldEnum).collect(toSet());

        List<DynamicTextFeedAdGroupFieldEnum> listForDynamicFeedFieldNames =
                Arrays.asList(DynamicTextFeedAdGroupFieldEnum.values());
        List<DynamicTextFeedAdGroupFieldEnum> listForDynamicFeedFieldNamesWithDuplicates =
                new ArrayList<>(listForDynamicFeedFieldNames);
        listForDynamicTextFieldNamesWithDuplicates.addAll(listForDynamicTextFieldNames);
        Set<AdGroupAnyFieldEnum> setForDynamicFeedFieldNames = Arrays.stream(DynamicTextFeedAdGroupFieldEnum.values())
                .map(AdGroupAnyFieldEnum::fromDynamicTextFeedAdGroupFieldEnum).collect(toSet());

        List<SmartAdGroupFieldEnum> listForSmartFieldNames =
                Arrays.asList(SmartAdGroupFieldEnum.values());
        List<SmartAdGroupFieldEnum> listForSmartFieldNamesWithDuplicates =
                new ArrayList<>(listForSmartFieldNames);
        listForSmartFieldNamesWithDuplicates.addAll(listForSmartFieldNames);
        Set<AdGroupAnyFieldEnum> setForSmartFieldNames = Arrays.stream(SmartAdGroupFieldEnum.values())
                .map(AdGroupAnyFieldEnum::fromSmartAdGroupFieldEnum).collect(toSet());

        List<ContentPromotionAdGroupFieldEnum> listForContentPromotionFieldNames =
                Arrays.asList(ContentPromotionAdGroupFieldEnum.values());
        List<ContentPromotionAdGroupFieldEnum> listForContentPromotionFieldNamesWithDuplicates =
                new ArrayList<>(listForContentPromotionFieldNames);
        listForContentPromotionFieldNamesWithDuplicates.addAll(listForContentPromotionFieldNames);
        Set<AdGroupAnyFieldEnum> setForContentPromotionFieldNames =
                Arrays.stream(ContentPromotionAdGroupFieldEnum.values())
                        .map(AdGroupAnyFieldEnum::fromContentPromotionAdGroupFieldEnum)
                        .collect(toSet());

        List<TextAdGroupFeedParamsFieldEnum> listForFilteredFeedsFieldNames =
                List.of(TextAdGroupFeedParamsFieldEnum.values());
        List<TextAdGroupFeedParamsFieldEnum> listForFilteredFeedsFieldNamesWithDuplicates =
                new ArrayList<>(listForFilteredFeedsFieldNames);
        listForFilteredFeedsFieldNamesWithDuplicates.addAll(listForFilteredFeedsFieldNames);


        Set<AdGroupAnyFieldEnum> setForAllFieldNames = Arrays.stream(AdGroupAnyFieldEnum.values()).collect(toSet());

        return new Object[][]{
                {"with field names", new GetRequest().withFieldNames(listForFieldNames), setForFieldNames},
                {"with field names with duplicates", new GetRequest().withFieldNames(listForFieldNamesWithDuplicates),
                        setForFieldNames},
                {"with mobile app field names",
                        new GetRequest().withMobileAppAdGroupFieldNames(listForMobileAppFieldNames),
                        setForMobileAppFieldNames},
                {"with mobile app field names with duplicates",
                        new GetRequest().withMobileAppAdGroupFieldNames(listForMobileAppFieldNamesWithDuplicates),
                        setForMobileAppFieldNames},
                {"with dynamic text field names",
                        new GetRequest().withDynamicTextAdGroupFieldNames(listForDynamicTextFieldNames),
                        setForDynamicTextFieldNames},
                {"with dynamic text field names with duplicates",
                        new GetRequest().withDynamicTextAdGroupFieldNames(listForDynamicTextFieldNamesWithDuplicates),
                        setForDynamicTextFieldNames},
                {"with dynamic feed field names",
                        new GetRequest().withDynamicTextFeedAdGroupFieldNames(listForDynamicFeedFieldNames),
                        setForDynamicFeedFieldNames},
                {"with dynamic feed field names with duplicates",
                        new GetRequest().withDynamicTextFeedAdGroupFieldNames(
                                listForDynamicFeedFieldNamesWithDuplicates),
                        setForDynamicFeedFieldNames},
                {"with smart text field names",
                        new GetRequest().withSmartAdGroupFieldNames(listForSmartFieldNames),
                        setForSmartFieldNames},
                {"with smart text field names with duplicates",
                        new GetRequest().withSmartAdGroupFieldNames(listForSmartFieldNamesWithDuplicates),
                        setForSmartFieldNames},
                {"with content promotion field names",
                        new GetRequest().withContentPromotionAdGroupFieldNames(listForContentPromotionFieldNames),
                        setForContentPromotionFieldNames},
                {"with content promotion field names with duplicates", new GetRequest()
                        .withContentPromotionAdGroupFieldNames(listForContentPromotionFieldNamesWithDuplicates),
                        setForContentPromotionFieldNames},
                {"with all field names",
                        new GetRequest().withFieldNames(listForFieldNames)
                                .withMobileAppAdGroupFieldNames(listForMobileAppFieldNames)
                                .withDynamicTextAdGroupFieldNames(listForDynamicTextFieldNames)
                                .withDynamicTextFeedAdGroupFieldNames(listForDynamicFeedFieldNames)
                                .withSmartAdGroupFieldNames(listForSmartFieldNames)
                                .withTextAdGroupFeedParamsFieldNames(listForFilteredFeedsFieldNames)
                                .withContentPromotionAdGroupFieldNames(listForContentPromotionFieldNames),
                        setForAllFieldNames},
                {"with all field names with duplicates",
                        new GetRequest().withFieldNames(listForFieldNamesWithDuplicates)
                                .withMobileAppAdGroupFieldNames(listForMobileAppFieldNamesWithDuplicates)
                                .withDynamicTextAdGroupFieldNames(listForDynamicTextFieldNamesWithDuplicates)
                                .withDynamicTextFeedAdGroupFieldNames(listForDynamicFeedFieldNamesWithDuplicates)
                                .withSmartAdGroupFieldNames(listForSmartFieldNamesWithDuplicates)
                                .withContentPromotionAdGroupFieldNames(
                                        listForContentPromotionFieldNamesWithDuplicates)
                                .withTextAdGroupFeedParamsFieldNames(listForFilteredFeedsFieldNamesWithDuplicates),
                        setForAllFieldNames},
        };
    }

    @Test
    public void test() {
        assertThat(delegate.extractFieldNames(request)).containsExactlyInAnyOrderElementsOf(expectedFields);
    }
}
