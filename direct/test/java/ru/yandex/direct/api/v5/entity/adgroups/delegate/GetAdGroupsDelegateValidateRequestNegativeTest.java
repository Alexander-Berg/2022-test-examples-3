package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.adgroups.AdGroupsSelectionCriteria;
import com.yandex.direct.api.v5.adgroups.GetRequest;
import com.yandex.direct.api.v5.general.LimitOffset;
import one.util.streamex.LongStreamEx;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.api.v5.common.validation.ApiDefectPresentation;
import ru.yandex.direct.api.v5.common.validation.DefaultApiPresentations;
import ru.yandex.direct.api.v5.common.validation.DefectPresentationService;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.validation.ApiDefectDefinitions;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static com.yandex.direct.api.v5.adgroups.AdGroupsSelectionCriteria.PropInfo.CAMPAIGN_IDS;
import static com.yandex.direct.api.v5.adgroups.AdGroupsSelectionCriteria.PropInfo.IDS;
import static com.yandex.direct.api.v5.adgroups.AdGroupsSelectionCriteria.PropInfo.NEGATIVE_KEYWORD_SHARED_SET_IDS;
import static com.yandex.direct.api.v5.adgroups.GetRequest.PropInfo.SELECTION_CRITERIA;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MAX_IDS_COUNT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MAX_LIMIT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MAX_OFFSET;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MIN_IDS_COUNT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MIN_LIMIT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MIN_OFFSET;
import static ru.yandex.direct.api.v5.entity.adgroups.Constants.MAX_CAMPAIGN_IDS_COUNT;
import static ru.yandex.direct.api.v5.entity.adgroups.Constants.MIN_CAMPAIGN_IDS_COUNT;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.MAX_LIBRARY_PACKS_COUNT;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@Api5Test
@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class GetAdGroupsDelegateValidateRequestNegativeTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    private static DefectPresentationService defectPresentationService =
            new DefectPresentationService(DefaultApiPresentations.HOLDER);

    @Autowired
    private GetAdGroupsDelegate delegate;

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public GetRequest request;

    @Parameterized.Parameter(2)
    public Consumer<ValidationResult<GetRequest, DefectType>> assertion;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"SelectionCriteria не передан",
                        new GetRequest(),
                        assertionForErrors(path(field(SELECTION_CRITERIA.schemaName.toString())),
                                CommonDefects.notNull())
                },
                {"передан пустой SelectionCriteria",
                        new GetRequest().withSelectionCriteria(new AdGroupsSelectionCriteria()),
                        assertionForErrors(path(field(SELECTION_CRITERIA.schemaName.toString())),
                                ApiDefectDefinitions.selectionCriteriaParamMissed(
                                        asList(IDS.schemaName.toString(), CAMPAIGN_IDS.schemaName.toString())))
                },
                /*{"размер SelectionCriteria.Ids меньше минимально допустимого",
                        new GetRequest().withSelectionCriteria(
                                new AdGroupsSelectionCriteria().withIds(new ArrayList<>()).withCampaignIds(
                                        LongStreamEx.range(MIN_CAMPAIGN_IDS_COUNT).boxed().toList())),
                        assertionForErrors(path(field(SELECTION_CRITERIA.schemaName.toString()), field(IDS.schemaName
                        .toString())),
                                Defects.minCollectionSize(DEFAULT_MIN_IDS_COUNT))
                },*/
                {"размер SelectionCriteria.Ids больше максимально допустимого",
                        new GetRequest().withSelectionCriteria(new AdGroupsSelectionCriteria()
                                .withIds(LongStreamEx.range(DEFAULT_MAX_IDS_COUNT + 1).boxed().toList())
                                .withCampaignIds(LongStreamEx.range(MIN_CAMPAIGN_IDS_COUNT).boxed().toList())),
                        assertionForErrors(path(field(SELECTION_CRITERIA.schemaName.toString()),
                                field(IDS.schemaName.toString())),
                                ApiDefectDefinitions.maxIdsInSelection())
                },
                /*{"размер SelectionCriteria.CampaignIds меньше минимально допустимого",
                        new GetRequest().withSelectionCriteria(
                                new AdGroupsSelectionCriteria().withCampaignIds(new ArrayList<>())
                                        .withIds(LongStreamEx.range(DEFAULT_MIN_IDS_COUNT).boxed().toList())),
                        assertionForErrors(path(field(SELECTION_CRITERIA.schemaName.toString()), field(CAMPAIGN_IDS
                        .schemaName.toString())),
                                Defects.minCollectionSize(MIN_CAMPAIGN_IDS_COUNT))
                },*/
                {"размер SelectionCriteria.CampaignIds больше максимально допустимого",
                        new GetRequest().withSelectionCriteria(new AdGroupsSelectionCriteria()
                                .withCampaignIds(LongStreamEx.range(MAX_CAMPAIGN_IDS_COUNT + 1).boxed().toList())
                                .withIds(LongStreamEx.range(DEFAULT_MIN_IDS_COUNT).boxed().toList())),
                        assertionForErrors(
                                path(field(SELECTION_CRITERIA.schemaName.toString()), field(CAMPAIGN_IDS.schemaName
                                        .toString())),
                                ApiDefectDefinitions.maxIdsInSelection())
                },
                {"размер SelectionCriteria.NegativeKeywordSharedSetIds больше максимально допустимого",
                        new GetRequest().withSelectionCriteria(new AdGroupsSelectionCriteria()
                                .withIds(LongStreamEx.range(DEFAULT_MIN_IDS_COUNT).boxed().toList())
                                .withNegativeKeywordSharedSetIds(LongStreamEx.range(MAX_LIBRARY_PACKS_COUNT + 1)
                                        .boxed().toList())),
                        assertionForErrors(
                                path(field(SELECTION_CRITERIA.schemaName.toString()),
                                        field(NEGATIVE_KEYWORD_SHARED_SET_IDS.schemaName.toString())),
                                ApiDefectDefinitions.maxIdsInSelection())
                },
                {"значение SelectionCriteria.Limit меньше минимально допустимого",
                        new GetRequest().withSelectionCriteria(new AdGroupsSelectionCriteria()
                                .withIds(LongStreamEx.range(DEFAULT_MIN_IDS_COUNT).boxed().toList())).withPage(
                                new LimitOffset().withLimit(DEFAULT_MIN_LIMIT - 1)),
                        assertionForErrors(path(field("Page"), field("Limit")),
                                ApiDefectDefinitions.incorrectPageNonPositiveLimit())
                },
                {"значение SelectionCriteria.Limit больше максимально допустимого",
                        new GetRequest().withSelectionCriteria(new AdGroupsSelectionCriteria()
                                .withCampaignIds(LongStreamEx.range(DEFAULT_MIN_IDS_COUNT).boxed().toList())).withPage(
                                new LimitOffset().withLimit(DEFAULT_MAX_LIMIT + 1)),
                        assertionForErrors(path(field("Page"), field("Limit")),
                                ApiDefectDefinitions.incorrectPageLimitExceeded(DEFAULT_MAX_LIMIT))
                },
                {"значение SelectionCriteria.Offset меньше минимально допустимого",
                        new GetRequest().withSelectionCriteria(new AdGroupsSelectionCriteria()
                                .withIds(LongStreamEx.range(DEFAULT_MIN_IDS_COUNT).boxed().toList())).withPage(
                                new LimitOffset().withOffset(DEFAULT_MIN_OFFSET - 1)),
                        assertionForErrors(path(field("Page"), field("Offset")),
                                ApiDefectDefinitions.incorrectPageNegativeOffset())
                },
                {"значение SelectionCriteria.Offset больше максимально допустимого",
                        new GetRequest().withSelectionCriteria(new AdGroupsSelectionCriteria()
                                .withCampaignIds(LongStreamEx.range(DEFAULT_MIN_IDS_COUNT).boxed().toList())).withPage(
                                new LimitOffset().withOffset(DEFAULT_MAX_OFFSET + 1)),
                        assertionForErrors(path(field("Page"), field("Offset")),
                                ApiDefectDefinitions.incorrectPageOffsetExceeded(DEFAULT_MAX_OFFSET))
                }
        });
    }

    private static Consumer<ValidationResult<GetRequest, DefectType>> assertionForErrors(Path path,
                                                                                         Defect defect) {
        ApiDefectPresentation presentation = defectPresentationService.getPresentationFor(defect.defectId());

        DefectType expectedDefectType = presentation.toDefectType(defect.params());

        return (vresult) -> assertThat(vresult.flattenErrors(), contains(validationError(path, expectedDefectType)));
    }

    @Test
    public void test() {
        ValidationResult<GetRequest, DefectType> validationResult = delegate.validateRequest(request);
        assertion.accept(validationResult);
    }
}
