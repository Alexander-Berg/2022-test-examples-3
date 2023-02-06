package ru.yandex.direct.api.v5.entity.bids.delegate;

import com.yandex.direct.api.v5.bids.BidsSelectionCriteria;
import com.yandex.direct.api.v5.bids.GetRequest;
import com.yandex.direct.api.v5.general.LimitOffset;
import one.util.streamex.LongStreamEx;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.api.v5.entity.bids.validation.GetBidsValidationService;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.validation.result.PathConverter;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.direct.api.v5.entity.bids.validation.GetBidsValidationService.AD_GROUP_IDS_PATH_KEY;
import static ru.yandex.direct.api.v5.entity.bids.validation.GetBidsValidationService.CAMPAIGN_IDS_PATH_KEY;
import static ru.yandex.direct.api.v5.entity.bids.validation.GetBidsValidationService.KEYWORD_IDS_PATH_KEY;
import static ru.yandex.direct.api.v5.validation.DefectTypes.incorrectPageLimitExceeded;
import static ru.yandex.direct.api.v5.validation.DefectTypes.incorrectPageNegativeOffset;
import static ru.yandex.direct.api.v5.validation.DefectTypes.incorrectPageNonPositiveLimit;
import static ru.yandex.direct.api.v5.validation.DefectTypes.incorrectPageOffsetExceeded;
import static ru.yandex.direct.api.v5.validation.DefectTypes.maxElementsInSelection;
import static ru.yandex.direct.api.v5.validation.DefectTypes.missedParamsInSelection;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class GetBidsValidationServiceTest {

    private static final long MAX_LIMIT = 10_000L;
    private static final long MAX_OFFSET = Integer.MAX_VALUE - MAX_LIMIT;
    private static final int MAX_CAMPAIGN_IDS = 10;
    private static final int MAX_ADGROUP_IDS = 1000;
    private static final int MAX_KEYWORD_IDS = 10000;
    private GetBidsValidationService validationService;

    @Before
    public void setup() {
        validationService = new GetBidsValidationService();
    }

    @Test
    public void validate_success() {
        ValidationResult<GetRequest, DefectType> result = validationService
                .validate(new GetRequest().withSelectionCriteria(new BidsSelectionCriteria().withKeywordIds(1L)));

        assertThat(result.hasAnyErrors()).isFalse();
    }

    @Test
    public void validate_failOnEmptySelectionCriteria() {
        GetRequest requestWithEmptyCriteria = new GetRequest().withSelectionCriteria(new BidsSelectionCriteria());
        ValidationResult<GetRequest, DefectType> result = validationService.validate(requestWithEmptyCriteria);

        assertThat(result.flattenErrors()).is(matchedBy(
                contains(validationError(path(field("SelectionCriteria")),
                        missedParamsInSelection(String.join(
                                ", ", asList(KEYWORD_IDS_PATH_KEY,
                                        AD_GROUP_IDS_PATH_KEY,
                                        CAMPAIGN_IDS_PATH_KEY)))))));
    }

    @Test
    public void validate_successOnMaxKeywordIds() {
        GetRequest requestWithEmptyCriteria = new GetRequest().withSelectionCriteria(
                new BidsSelectionCriteria().withKeywordIds(LongStreamEx.range(MAX_KEYWORD_IDS).boxed().toList())
        );
        ValidationResult<GetRequest, DefectType> result = validationService.validate(requestWithEmptyCriteria);

        assertThat(result.hasAnyErrors()).isFalse();
    }

    @Test
    public void validate_failOnTooManyKeywordIds() {
        GetRequest requestWithEmptyCriteria = new GetRequest().withSelectionCriteria(
                new BidsSelectionCriteria().withKeywordIds(LongStreamEx.range(MAX_KEYWORD_IDS + 1).boxed().toList())
        );
        ValidationResult<GetRequest, DefectType> result = validationService.validate(requestWithEmptyCriteria);

        assertThat(result.flattenErrors()).is(matchedBy(
                contains(validationError(path(field("SelectionCriteria"), field("KeywordIds")),
                        maxElementsInSelection(MAX_KEYWORD_IDS)))));
    }

    @Test
    public void validate_successOnMaxAdGroupIds() {
        GetRequest requestWithEmptyCriteria = new GetRequest().withSelectionCriteria(
                new BidsSelectionCriteria().withAdGroupIds(LongStreamEx.range(MAX_ADGROUP_IDS).boxed().toList())
        );
        ValidationResult<GetRequest, DefectType> result = validationService.validate(requestWithEmptyCriteria);

        assertThat(result.hasAnyErrors()).isFalse();
    }

    @Test
    public void validate_failOnTooManyAdGroupIds() {
        GetRequest requestWithEmptyCriteria = new GetRequest().withSelectionCriteria(
                new BidsSelectionCriteria().withAdGroupIds(LongStreamEx.range(MAX_ADGROUP_IDS + 1).boxed().toList())
        );
        ValidationResult<GetRequest, DefectType> result = validationService.validate(requestWithEmptyCriteria);

        assertThat(result.flattenErrors()).is(matchedBy(
                contains(validationError(path(field("SelectionCriteria"), field("AdGroupIds")),
                        maxElementsInSelection(MAX_ADGROUP_IDS)))));
    }

    @Test
    public void validate_successOnMaxCampaignIds() {
        GetRequest requestWithEmptyCriteria = new GetRequest().withSelectionCriteria(
                new BidsSelectionCriteria().withCampaignIds(LongStreamEx.range(MAX_CAMPAIGN_IDS).boxed().toList())
        );
        ValidationResult<GetRequest, DefectType> result = validationService.validate(requestWithEmptyCriteria);

        assertThat(result.hasAnyErrors()).isFalse();
    }

    @Test
    public void validate_failOnTooManyCampaignIds() {
        GetRequest requestWithEmptyCriteria = new GetRequest().withSelectionCriteria(
                new BidsSelectionCriteria().withCampaignIds(LongStreamEx.range(MAX_CAMPAIGN_IDS + 1).boxed().toList())
        );
        ValidationResult<GetRequest, DefectType> result = validationService.validate(requestWithEmptyCriteria);

        assertThat(result.flattenErrors()).is(matchedBy(
                contains(validationError(path(field("SelectionCriteria"), field("CampaignIds")),
                        maxElementsInSelection(MAX_CAMPAIGN_IDS)))));
    }

    @Test
    public void validate_failOnTooManyIdsInSeveralFields() {
        GetRequest requestWithEmptyCriteria = new GetRequest().withSelectionCriteria(
                new BidsSelectionCriteria()
                        .withKeywordIds(LongStreamEx.range(MAX_KEYWORD_IDS + 1).boxed().toList())
                        .withAdGroupIds(LongStreamEx.range(MAX_ADGROUP_IDS + 1).boxed().toList())
                        .withCampaignIds(LongStreamEx.range(MAX_CAMPAIGN_IDS + 1).boxed().toList())
        );
        ValidationResult<GetRequest, DefectType> result = validationService.validate(requestWithEmptyCriteria);

        //noinspection unchecked
        assertThat(result.flattenErrors()).is(matchedBy(containsInAnyOrder(
                validationError(path(field("SelectionCriteria"), field("KeywordIds")),
                        maxElementsInSelection(MAX_KEYWORD_IDS)),
                validationError(path(field("SelectionCriteria"), field("AdGroupIds")),
                        maxElementsInSelection(MAX_ADGROUP_IDS)),
                validationError(path(field("SelectionCriteria"), field("CampaignIds")),
                        maxElementsInSelection(MAX_CAMPAIGN_IDS)))));
    }

    @Test
    public void validate_failOnNegativeLimit() {
        GetRequest requestWithEmptyCriteria = new GetRequest()
                .withSelectionCriteria(new BidsSelectionCriteria()
                        .withKeywordIds(LongStreamEx.range(MAX_KEYWORD_IDS).boxed().toList()))
                .withPage(new LimitOffset().withLimit(-1L));
        ValidationResult<GetRequest, DefectType> result = validationService.validate(requestWithEmptyCriteria);

        assertThat(result.flattenErrors()).is(matchedBy(contains(
                validationError(path(field("Page"), field("Limit")),
                        incorrectPageNonPositiveLimit()))));
    }

    @Test
    public void validate_failOnZeroLimit() {
        GetRequest requestWithEmptyCriteria = new GetRequest()
                .withSelectionCriteria(new BidsSelectionCriteria()
                        .withKeywordIds(LongStreamEx.range(MAX_KEYWORD_IDS).boxed().toList()))
                .withPage(new LimitOffset().withLimit(0L));
        ValidationResult<GetRequest, DefectType> result = validationService.validate(requestWithEmptyCriteria);

        assertThat(result.flattenErrors()).is(matchedBy(contains(
                validationError(path(field("Page"), field("Limit")),
                        incorrectPageNonPositiveLimit()))));
    }

    @Test
    public void validate_failOnNegativeOffset() {
        GetRequest requestWithEmptyCriteria = new GetRequest()
                .withSelectionCriteria(new BidsSelectionCriteria()
                        .withKeywordIds(LongStreamEx.range(MAX_KEYWORD_IDS).boxed().toList()))
                .withPage(new LimitOffset().withOffset(-1L));
        ValidationResult<GetRequest, DefectType> result = validationService.validate(requestWithEmptyCriteria);

        assertThat(result.flattenErrors()).is(matchedBy(contains(
                validationError(path(field("Page"), field("Offset")),
                        incorrectPageNegativeOffset()))));
    }

    @Test
    public void validate_failOnTooBigOffset() {
        GetRequest requestWithEmptyCriteria = new GetRequest()
                .withSelectionCriteria(new BidsSelectionCriteria()
                        .withKeywordIds(LongStreamEx.range(MAX_KEYWORD_IDS).boxed().toList()))
                .withPage(new LimitOffset().withOffset(MAX_OFFSET + 1));
        ValidationResult<GetRequest, DefectType> result = validationService.validate(requestWithEmptyCriteria);

        assertThat(result.flattenErrors()).is(matchedBy(contains(
                validationError(path(field("Page"), field("Offset")),
                        incorrectPageOffsetExceeded(MAX_OFFSET)))));
    }

    @Test
    public void validate_failOnTooBigLimit() {
        GetRequest requestWithEmptyCriteria = new GetRequest()
                .withSelectionCriteria(new BidsSelectionCriteria()
                        .withKeywordIds(LongStreamEx.range(MAX_KEYWORD_IDS).boxed().toList()))
                .withPage(new LimitOffset().withLimit(MAX_LIMIT + 1));
        ValidationResult<GetRequest, DefectType> result = validationService.validate(requestWithEmptyCriteria);

        assertThat(result.flattenErrors()).is(matchedBy(contains(
                validationError(path(field("Page"), field("Limit")),
                        incorrectPageLimitExceeded(MAX_LIMIT)))));
    }

    @Test
    public void converter_successfulPaths() {
        PathConverter converter = validationService.pathConverter();
        converter.convert(path(field("SelectionCriteria"), field("KeywordIds"),
                field("AdGroupIds"), field("CampaignIds")));
    }

    @Test(expected = IllegalStateException.class)
    public void converter_unsuccessfulPaths() {
        // проверяем, что не всякий ключ проходит через полученный конвертер
        PathConverter converter = validationService.pathConverter();
        converter.convert(path(field("invalidKey")));
    }
}
