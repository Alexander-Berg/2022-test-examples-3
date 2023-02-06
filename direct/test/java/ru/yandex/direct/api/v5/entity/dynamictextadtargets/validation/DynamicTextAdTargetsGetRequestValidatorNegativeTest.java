package ru.yandex.direct.api.v5.entity.dynamictextadtargets.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import com.yandex.direct.api.v5.dynamictextadtargets.GetRequest;
import com.yandex.direct.api.v5.general.AdTargetsSelectionCriteria;
import com.yandex.direct.api.v5.general.LimitOffset;
import one.util.streamex.LongStreamEx;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.common.validation.ApiDefectPresentation;
import ru.yandex.direct.api.v5.common.validation.DefaultApiPresentations;
import ru.yandex.direct.api.v5.common.validation.DefectPresentationService;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static com.yandex.direct.api.v5.dynamictextadtargets.GetRequest.PropInfo.SELECTION_CRITERIA;
import static com.yandex.direct.api.v5.general.AdTargetsSelectionCriteria.PropInfo.AD_GROUP_IDS;
import static com.yandex.direct.api.v5.general.AdTargetsSelectionCriteria.PropInfo.CAMPAIGN_IDS;
import static com.yandex.direct.api.v5.general.AdTargetsSelectionCriteria.PropInfo.IDS;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MAX_IDS_COUNT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MAX_LIMIT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MAX_OFFSET;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MIN_IDS_COUNT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MIN_LIMIT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MIN_OFFSET;
import static ru.yandex.direct.api.v5.entity.dynamictextadtargets.validation.DynamicTextAdTargetsGetRequestValidator.MAX_ADGROUP_IDS_COUNT;
import static ru.yandex.direct.api.v5.entity.dynamictextadtargets.validation.DynamicTextAdTargetsGetRequestValidator.MAX_CAMPAIGN_IDS_COUNT;
import static ru.yandex.direct.api.v5.validation.ApiDefectDefinitions.incorrectPageLimitExceeded;
import static ru.yandex.direct.api.v5.validation.ApiDefectDefinitions.incorrectPageNegativeOffset;
import static ru.yandex.direct.api.v5.validation.ApiDefectDefinitions.incorrectPageNonPositiveLimit;
import static ru.yandex.direct.api.v5.validation.ApiDefectDefinitions.incorrectPageOffsetExceeded;
import static ru.yandex.direct.api.v5.validation.ApiDefectDefinitions.selectionCriteriaParamMissed;
import static ru.yandex.direct.api.v5.validation.DefectTypes.maxElementsInSelection;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class DynamicTextAdTargetsGetRequestValidatorNegativeTest {

    private static DynamicTextAdTargetsGetRequestValidator requestValidator;
    private static DefectPresentationService defectPresentationService =
            new DefectPresentationService(DefaultApiPresentations.HOLDER);

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
                        assertionForErrors(path(field(SELECTION_CRITERIA.schemaName.getLocalPart())),
                                CommonDefects.notNull())
                },
                {"передан пустой SelectionCriteria",
                        new GetRequest().withSelectionCriteria(new AdTargetsSelectionCriteria()),
                        assertionForErrors(path(field(SELECTION_CRITERIA.schemaName.getLocalPart())),
                                selectionCriteriaParamMissed(
                                        asList(IDS.schemaName.getLocalPart(), AD_GROUP_IDS.schemaName.getLocalPart(),
                                                CAMPAIGN_IDS.schemaName.getLocalPart())))
                },
                {"размер SelectionCriteria.Ids больше максимально допустимого",
                        new GetRequest().withSelectionCriteria(new AdTargetsSelectionCriteria()
                                .withIds(LongStreamEx.range(DEFAULT_MAX_IDS_COUNT + 1).boxed().toList())),
                        assertionForErrors(path(field(SELECTION_CRITERIA.schemaName.getLocalPart()),
                                field(IDS.schemaName.getLocalPart())),
                                maxElementsInSelection(DEFAULT_MAX_IDS_COUNT))
                },
                {"размер SelectionCriteria.AdGroupIds больше максимально допустимого",
                        new GetRequest().withSelectionCriteria(new AdTargetsSelectionCriteria()
                                .withAdGroupIds(LongStreamEx.range(MAX_ADGROUP_IDS_COUNT + 1).boxed().toList())),
                        assertionForErrors(
                                path(field(SELECTION_CRITERIA.schemaName.getLocalPart()),
                                        field(AD_GROUP_IDS.schemaName.getLocalPart())),
                                maxElementsInSelection(MAX_ADGROUP_IDS_COUNT))
                },
                {"размер SelectionCriteria.CampaignIds больше максимально допустимого",
                        new GetRequest().withSelectionCriteria(new AdTargetsSelectionCriteria()
                                .withCampaignIds(LongStreamEx.range(MAX_CAMPAIGN_IDS_COUNT + 1).boxed().toList())),
                        assertionForErrors(
                                path(field(SELECTION_CRITERIA.schemaName.getLocalPart()),
                                        field(CAMPAIGN_IDS.schemaName.getLocalPart())),
                                maxElementsInSelection(MAX_CAMPAIGN_IDS_COUNT))
                },
                {"значение SelectionCriteria.Limit меньше минимально допустимого",
                        new GetRequest().withSelectionCriteria(new AdTargetsSelectionCriteria()
                                .withIds(LongStreamEx.range(DEFAULT_MIN_IDS_COUNT).boxed().toList())).withPage(
                                new LimitOffset().withLimit(DEFAULT_MIN_LIMIT - 1)),
                        assertionForErrors(path(field("Page"), field("Limit")), incorrectPageNonPositiveLimit())
                },
                {"значение SelectionCriteria.Limit больше максимально допустимого",
                        new GetRequest().withSelectionCriteria(new AdTargetsSelectionCriteria()
                                .withCampaignIds(LongStreamEx.range(DEFAULT_MIN_IDS_COUNT).boxed().toList())).withPage(
                                new LimitOffset().withLimit(DEFAULT_MAX_LIMIT + 1)),
                        assertionForErrors(path(field("Page"), field("Limit")),
                                incorrectPageLimitExceeded(DEFAULT_MAX_LIMIT))
                },
                {"значение SelectionCriteria.Offset меньше минимально допустимого",
                        new GetRequest().withSelectionCriteria(new AdTargetsSelectionCriteria()
                                .withIds(LongStreamEx.range(DEFAULT_MIN_IDS_COUNT).boxed().toList())).withPage(
                                new LimitOffset().withOffset(DEFAULT_MIN_OFFSET - 1)),
                        assertionForErrors(path(field("Page"), field("Offset")), incorrectPageNegativeOffset())
                },
                {"значение SelectionCriteria.Offset больше максимально допустимого",
                        new GetRequest().withSelectionCriteria(new AdTargetsSelectionCriteria()
                                .withCampaignIds(LongStreamEx.range(DEFAULT_MIN_IDS_COUNT).boxed().toList())).withPage(
                                new LimitOffset().withOffset(DEFAULT_MAX_OFFSET + 1)),
                        assertionForErrors(path(field("Page"), field("Offset")),
                                incorrectPageOffsetExceeded(DEFAULT_MAX_OFFSET))
                }
        });
    }

    @BeforeClass
    public static void setUp() {
        requestValidator = new DynamicTextAdTargetsGetRequestValidator();
    }

    private static Consumer<ValidationResult<GetRequest, DefectType>> assertionForErrors(Path path,
                                                                                         Defect defect) {
        ApiDefectPresentation presentation = defectPresentationService.getPresentationFor(defect.defectId());
        DefectType expectedDefectType = presentation.toDefectType(defect.params());
        return assertionForErrors(path, expectedDefectType);
    }

    private static Consumer<ValidationResult<GetRequest, DefectType>> assertionForErrors(Path path,
                                                                                         DefectType defectType) {
        return (vresult) -> assertThat(vresult.flattenErrors(), contains(validationError(path, defectType)));
    }

    @Test
    public void test() {
        ValidationResult<GetRequest, DefectType> validationResult = requestValidator.validate(request);
        assertion.accept(validationResult);
    }
}

