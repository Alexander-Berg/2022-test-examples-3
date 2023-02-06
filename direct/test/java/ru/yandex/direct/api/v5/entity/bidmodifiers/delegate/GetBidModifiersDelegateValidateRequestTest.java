package ru.yandex.direct.api.v5.entity.bidmodifiers.delegate;

import java.util.Collection;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.bidmodifiers.BidModifiersSelectionCriteria;
import com.yandex.direct.api.v5.bidmodifiers.GetRequest;
import com.yandex.direct.api.v5.general.LimitOffset;
import one.util.streamex.LongStreamEx;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.common.validation.ApiDefectPresentation;
import ru.yandex.direct.api.v5.common.validation.DefectPresentationService;
import ru.yandex.direct.api.v5.entity.bidmodifiers.validation.BidModifiersDefectPresentations;
import ru.yandex.direct.api.v5.entity.bidmodifiers.validation.GetBidModifiersValidationService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.common.util.PropertyFilter;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefectIds;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MAX_LIMIT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MIN_LIMIT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MIN_OFFSET;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.Constants.ADGROUP_IDS_LIMIT;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.Constants.CAMPAIGN_IDS_LIMIT;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.Constants.GET_HARD_LIMIT;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.Constants.IDS_LIMIT;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.validation.GetBidModifiersValidationService.AD_GROUP_IDS_PATH_KEY;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.validation.GetBidModifiersValidationService.CAMPAIGN_IDS_PATH_KEY;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.validation.GetBidModifiersValidationService.IDS_PATH_KEY;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.validation.GetBidModifiersValidationService.PAGE_PATH_KEY;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.validation.GetBidModifiersValidationService.SELECTION_CRITERIA_PATH_KEY;
import static ru.yandex.direct.api.v5.validation.ApiDefectDefinitions.arraySizeExceeded;
import static ru.yandex.direct.api.v5.validation.ApiDefectDefinitions.incorrectPageLimitExceeded;
import static ru.yandex.direct.api.v5.validation.ApiDefectDefinitions.incorrectPageNegativeOffset;
import static ru.yandex.direct.api.v5.validation.ApiDefectDefinitions.incorrectPageNonPositiveLimit;
import static ru.yandex.direct.api.v5.validation.ApiDefectDefinitions.selectionCriteriaParamMissed;
import static ru.yandex.direct.api.v5.validation.Matchers.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class GetBidModifiersDelegateValidateRequestTest {
    private static final String LIMIT_PATH_KEY = "Limit";
    private static final DefectPresentationService DEFECT_PRESENTATION_SERVICE =
            new DefectPresentationService(BidModifiersDefectPresentations.HOLDER);

    private GetBidModifiersDelegate delegate;

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public GetRequest request;

    @Parameterized.Parameter(2)
    public Consumer<ValidationResult<GetRequest, DefectType>> assertion;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"передан пустой SelectionCriteria",
                        new GetRequest()
                                .withSelectionCriteria(new BidModifiersSelectionCriteria()),
                        assertionForErrors(path(field(SELECTION_CRITERIA_PATH_KEY)),
                                selectionCriteriaParamMissed(
                                        asList(CAMPAIGN_IDS_PATH_KEY, AD_GROUP_IDS_PATH_KEY, IDS_PATH_KEY)))
                },
                {"размер SelectionCriteria.CampaignIds больше максимально допустимого",
                        new GetRequest().withSelectionCriteria(new BidModifiersSelectionCriteria()
                                .withCampaignIds(LongStreamEx.range(CAMPAIGN_IDS_LIMIT + 1).boxed().toList())),
                        assertionForErrors(path(field(SELECTION_CRITERIA_PATH_KEY), field(CAMPAIGN_IDS_PATH_KEY)),
                                arraySizeExceeded(CAMPAIGN_IDS_LIMIT))
                },
                {"размер SelectionCriteria.CampaignIds равен максимально допустимому",
                        new GetRequest().withSelectionCriteria(new BidModifiersSelectionCriteria()
                                .withCampaignIds(LongStreamEx.range(CAMPAIGN_IDS_LIMIT).boxed().toList())),
                        null
                },
                {"размер SelectionCriteria.AdgroupIds больше максимально допустимого",
                        new GetRequest().withSelectionCriteria(new BidModifiersSelectionCriteria()
                                .withAdGroupIds(LongStreamEx.range(ADGROUP_IDS_LIMIT + 1).boxed().toList())),
                        assertionForErrors(path(field(SELECTION_CRITERIA_PATH_KEY), field(AD_GROUP_IDS_PATH_KEY)),
                                arraySizeExceeded(ADGROUP_IDS_LIMIT))
                },
                {"размер SelectionCriteria.AdgroupIds равен максимально допустимому",
                        new GetRequest().withSelectionCriteria(new BidModifiersSelectionCriteria()
                                .withAdGroupIds(LongStreamEx.range(ADGROUP_IDS_LIMIT).boxed().toList())),
                        null
                },
                {"размер SelectionCriteria.Ids больше максимально допустимого",
                        new GetRequest().withSelectionCriteria(new BidModifiersSelectionCriteria()
                                .withIds(LongStreamEx.range(IDS_LIMIT + 1).boxed().toList())),
                        assertionForErrors(path(field(SELECTION_CRITERIA_PATH_KEY), field(IDS_PATH_KEY)),
                                arraySizeExceeded(IDS_LIMIT))
                },
                {"размер SelectionCriteria.Ids равен максимально допустимому",
                        new GetRequest().withSelectionCriteria(new BidModifiersSelectionCriteria()
                                .withIds(LongStreamEx.range(IDS_LIMIT).boxed().toList())),
                        null
                },
                {"значение SelectionCriteria.Limit меньше минимально допустимого",
                        new GetRequest().withSelectionCriteria(defaultCriteria()).withPage(
                                new LimitOffset().withLimit(DEFAULT_MIN_LIMIT - 1)),
                        assertionForErrors(path(field(PAGE_PATH_KEY), field(LIMIT_PATH_KEY)),
                                incorrectPageNonPositiveLimit())
                },
                {"значение SelectionCriteria.Limit равно минимально допустимому",
                        new GetRequest().withSelectionCriteria(defaultCriteria()).withPage(
                                new LimitOffset().withLimit(DEFAULT_MIN_LIMIT)),
                        null
                },
                {"значение SelectionCriteria.Limit больше максимально допустимого",
                        new GetRequest().withSelectionCriteria(defaultCriteria()).withPage(
                                new LimitOffset().withLimit(DEFAULT_MAX_LIMIT + 1)),
                        assertionForErrors(path(field("Page"), field("Limit")),
                                incorrectPageLimitExceeded(DEFAULT_MAX_LIMIT))
                },
                {"значение SelectionCriteria.Limit равно максимально допустимому",
                        new GetRequest().withSelectionCriteria(defaultCriteria()).withPage(
                                new LimitOffset().withLimit(DEFAULT_MAX_LIMIT)),
                        null
                },
                {"значение SelectionCriteria.Offset меньше минимально допустимого",
                        new GetRequest().withSelectionCriteria(defaultCriteria()).withPage(
                                new LimitOffset().withOffset(DEFAULT_MIN_OFFSET - 1)),
                        assertionForErrors(path(field("Page"), field("Offset")),
                                incorrectPageNegativeOffset())
                },
                {"значение SelectionCriteria.Offset равно минимально допустимому",
                        new GetRequest().withSelectionCriteria(defaultCriteria()).withPage(
                                new LimitOffset().withOffset(DEFAULT_MIN_OFFSET)),
                        null
                },
                {"значение SelectionCriteria.Offset больше максимально допустимой суммы с лимитом",
                        new GetRequest().withSelectionCriteria(defaultCriteria()).withPage(
                                new LimitOffset().withOffset(GET_HARD_LIMIT + 1L)),
                        assertionForErrors(path(field("Page")),
                                new Defect<>(
                                        BidModifiersDefectIds.LimitOffsetDefects.BID_MODIFIERS_GET_HARD_LIMIT_EXCEEDED,
                                        GET_HARD_LIMIT))
                },
                {"сумма SelectionCriteria.Limit и SelectionCriteria.Offset больше максимально допустимой",
                        new GetRequest().withSelectionCriteria(defaultCriteria()).withPage(
                                new LimitOffset()
                                        .withLimit(DEFAULT_MAX_LIMIT)
                                        .withOffset(GET_HARD_LIMIT - DEFAULT_MAX_LIMIT + 1L)),
                        assertionForErrors(path(field("Page")),
                                new Defect<>(
                                        BidModifiersDefectIds.LimitOffsetDefects.BID_MODIFIERS_GET_HARD_LIMIT_EXCEEDED,
                                        GET_HARD_LIMIT))
                },
                {"сумма SelectionCriteria.Limit и SelectionCriteria.Offset равна максимально допустимой",
                        new GetRequest().withSelectionCriteria(defaultCriteria()).withPage(
                                new LimitOffset()
                                        .withLimit(DEFAULT_MAX_LIMIT)
                                        .withOffset(GET_HARD_LIMIT - DEFAULT_MAX_LIMIT)),
                        null
                },
        });
    }

    private static BidModifiersSelectionCriteria defaultCriteria() {
        return new BidModifiersSelectionCriteria()
                .withCampaignIds((long) CAMPAIGN_IDS_LIMIT)
                .withAdGroupIds((long) ADGROUP_IDS_LIMIT)
                .withIds((long) IDS_LIMIT);
    }

    @Before
    public void before() {
        delegate = new GetBidModifiersDelegate(
                mock(ApiAuthenticationSource.class),
                new GetBidModifiersValidationService(),
                mock(BidModifierService.class),
                new PropertyFilter(),
                mock(AdGroupService.class));
    }

    private static Consumer<ValidationResult<GetRequest, DefectType>> assertionForErrors(Path path,
                                                                                         Defect defect) {
        ApiDefectPresentation presentation = DEFECT_PRESENTATION_SERVICE.getPresentationFor(defect.defectId());

        DefectType expectedDefectType = presentation.toDefectType(defect.params());

        return (vresult) -> assertThat(vresult.flattenErrors(), contains(validationError(path, expectedDefectType)));
    }

    @Test
    public void test() {
        ValidationResult<GetRequest, DefectType> result = delegate.validateRequest(request);

        if (assertion == null) {
            Assert.assertThat(result, hasNoDefects());
        } else {
            assertion.accept(result);
        }

    }
}
