package ru.yandex.direct.core.entity.adgroup.service.validation.types;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated;
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects;
import ru.yandex.direct.core.entity.feed.model.Feed;
import ru.yandex.direct.core.entity.feed.model.UpdateStatus;
import ru.yandex.direct.core.entity.feed.validation.FeedDefects;
import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;
import ru.yandex.direct.core.testing.data.TestFeeds;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.defect.StringDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.validation.types.BaseDynSmartAdGroupValidationService.MAX_BODY_LENGTH;
import static ru.yandex.direct.core.entity.adgroup.service.validation.types.BaseDynSmartAdGroupValidationService.MAX_NAME_LENGTH;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

// TODO: при объединении смартов и ДО эти тесты должны переехать в DynSmartAdGroupValidationTest (часть там уже есть)
@ContextConfiguration(classes = CoreTestingConfiguration.class)
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class PerformanceAdGroupValidationAdGroupTest {
    @Autowired
    public Steps steps;
    @Autowired
    private PerformanceAdGroupValidation performanceAdGroupValidation;

    private ClientInfo clientInfo;
    private FeedInfo feedInfo;
    private CampaignInfo campaign;

    private static final String EMOJI_SYMBOL = "\uD83D\uDEA9";

    @Before
    public void setUp() throws Exception {
        clientInfo = steps.clientSteps().createDefaultClient();
        feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        campaign = steps.campaignSteps().createActivePerformanceCampaign(clientInfo);
    }

    private PerformanceAdGroup getPerformanceAdGroup() {
        return new PerformanceAdGroup()
                .withCampaignId(campaign.getCampaignId())
                .withType(AdGroupType.PERFORMANCE)
                .withFeedId(feedInfo.getFeedId())
                .withStatusBLGenerated(StatusBLGenerated.PROCESSING)
                .withFieldToUseAsName("some name")
                .withFieldToUseAsBody("some body");
    }

    @Test
    public void validateAdGroup_success() {
        PerformanceAdGroup performanceAdGroup = getPerformanceAdGroup();
        ValidationResult<List<PerformanceAdGroup>, Defect> actual = performanceAdGroupValidation
                .validateAdGroups(clientInfo.getClientId(), singletonList(performanceAdGroup));
        assertThat(actual).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void validateAddAdGroup_failureWhenFeedNotExist() {
        long feedId = Integer.MAX_VALUE - 1L;
        PerformanceAdGroup performanceAdGroup = getPerformanceAdGroup()
                .withFeedId(feedId);
        ValidationResult<List<PerformanceAdGroup>, Defect> actual = performanceAdGroupValidation
                .validateAddAdGroups(clientInfo.getClientId(), singletonList(performanceAdGroup));
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(index(0), field(PerformanceAdGroup.FEED_ID.name())),
                        AdGroupDefects.feedNotExist(feedId)))));
    }

    @Test
    public void validateAddAdGroup_failureWhenFeedNotSet() {
        PerformanceAdGroup performanceAdGroup = getPerformanceAdGroup()
                .withFeedId(null);
        ValidationResult<List<PerformanceAdGroup>, Defect> actual = performanceAdGroupValidation
                .validateAddAdGroups(clientInfo.getClientId(), singletonList(performanceAdGroup));
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(index(0), field(PerformanceAdGroup.FEED_ID.name())),
                        FeedDefects.feedIsNotSet()))));
    }

    @Test
    public void validateAdGroup_successWhenFeedStatusWrong() {
        //Создаём фид с недопустимым статусом
        UpdateStatus errorUpdateStatus = UpdateStatus.ERROR;
        Feed errorFeed = TestFeeds.defaultFeed(clientInfo.getClientId())
                .withUpdateStatus(errorUpdateStatus);
        FeedInfo errorFeedInfo = new FeedInfo()
                .withClientInfo(clientInfo)
                .withFeed(errorFeed);
        steps.feedSteps().createFeed(errorFeedInfo);

        //Выполняем валидацию и проверяем результат
        PerformanceAdGroup performanceAdGroup = getPerformanceAdGroup()
                .withFeedId(errorFeedInfo.getFeedId());
        ValidationResult<List<PerformanceAdGroup>, Defect> actual = performanceAdGroupValidation
                .validateAdGroups(clientInfo.getClientId(), singletonList(performanceAdGroup));
        assertThat(actual).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void validateAddAdGroup_success_whenSiteFeedHasStatusNew() {
        //Создаём фид
        FeedInfo siteFeedInfo = steps.feedSteps().createDefaultSyncedSiteFeed(clientInfo);
        steps.feedSteps().setFeedProperty(siteFeedInfo, Feed.UPDATE_STATUS, UpdateStatus.NEW);

        //Выполняем валидацию и проверяем результат
        PerformanceAdGroup performanceAdGroup = getPerformanceAdGroup()
                .withFeedId(siteFeedInfo.getFeedId());
        ValidationResult<List<PerformanceAdGroup>, Defect> actual = performanceAdGroupValidation
                .validateAdGroups(clientInfo.getClientId(), singletonList(performanceAdGroup));
        assertThat(actual).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void validateAddAdGroup_failureWhenFeedStatusWrong() {
        //Создаём фид с недопустимым статусом
        UpdateStatus errorUpdateStatus = UpdateStatus.ERROR;
        Feed errorFeed = TestFeeds.defaultFeed(clientInfo.getClientId())
                .withUpdateStatus(errorUpdateStatus);
        FeedInfo errorFeedInfo = new FeedInfo()
                .withClientInfo(clientInfo)
                .withFeed(errorFeed);
        steps.feedSteps().createFeed(errorFeedInfo);

        //Выполняем валидацию и проверяем результат
        PerformanceAdGroup performanceAdGroup = getPerformanceAdGroup()
                .withFeedId(errorFeedInfo.getFeedId());
        ValidationResult<List<PerformanceAdGroup>, Defect> actual = performanceAdGroupValidation
                .validateAddAdGroups(clientInfo.getClientId(), singletonList(performanceAdGroup));
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(index(0), field(PerformanceAdGroup.FEED_ID.name())),
                        FeedDefects.feedStatusWrong(errorUpdateStatus)))));
    }

    @Test
    public void validateAdGroup_failureWhenFieldToUseAsNameIsTooLong() {
        String fortyOneSymString = StringUtils.repeat('A', MAX_NAME_LENGTH + 1);
        PerformanceAdGroup performanceAdGroup = getPerformanceAdGroup()
                .withFieldToUseAsName(fortyOneSymString);
        ValidationResult<List<PerformanceAdGroup>, Defect> actual = performanceAdGroupValidation
                .validateAdGroups(clientInfo.getClientId(), singletonList(performanceAdGroup));
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(index(0), field(PerformanceAdGroup.FIELD_TO_USE_AS_NAME.name())),
                        FeedDefects.feedNameFieldIsTooLong(MAX_NAME_LENGTH)))));
    }

    @Test
    public void validateAdGroup_failureWhenFieldToUseAsBodyIsTooLong() {
        String fortyOneSymString = StringUtils.repeat('A', MAX_BODY_LENGTH + 1);
        PerformanceAdGroup performanceAdGroup = getPerformanceAdGroup()
                .withFieldToUseAsBody(fortyOneSymString);
        ValidationResult<List<PerformanceAdGroup>, Defect> actual = performanceAdGroupValidation
                .validateAdGroups(clientInfo.getClientId(), singletonList(performanceAdGroup));
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(index(0), field(PerformanceAdGroup.FIELD_TO_USE_AS_BODY.name())),
                        FeedDefects.feedBodyFieldIsTooLong(MAX_BODY_LENGTH)))));
    }

    @Test
    public void validateAdGroup_failureWhenTrackingParamsIsTooLong() {
        String longSymString = StringUtils.repeat('A', 1025);
        PerformanceAdGroup performanceAdGroup = getPerformanceAdGroup()
                .withTrackingParams(longSymString);
        ValidationResult<List<PerformanceAdGroup>, Defect> actual = performanceAdGroupValidation
                .validateAdGroups(clientInfo.getClientId(), singletonList(performanceAdGroup));
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(index(0), field(PerformanceAdGroup.TRACKING_PARAMS.name())),
                        CollectionDefects.maxStringLength(PerformanceAdGroupValidation.MAX_TRACKING_PARAMS_SIZE)))));
    }

    @Test
    public void validateAdGroup_failureWhenFieldToUseAsBodyHasWrongSymbols() {
        PerformanceAdGroup performanceAdGroup = getPerformanceAdGroup()
                .withFieldToUseAsBody(EMOJI_SYMBOL);
        ValidationResult<List<PerformanceAdGroup>, Defect> actual = performanceAdGroupValidation
                .validateAdGroups(clientInfo.getClientId(), singletonList(performanceAdGroup));
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(index(0), field(PerformanceAdGroup.FIELD_TO_USE_AS_BODY.name())),
                        StringDefects.admissibleChars()))));
    }

    @Test
    public void validateAdGroup_failureWhenFieldToUseAsNameHasWrongSymbols() {
        PerformanceAdGroup performanceAdGroup = getPerformanceAdGroup()
                .withFieldToUseAsName(EMOJI_SYMBOL);
        ValidationResult<List<PerformanceAdGroup>, Defect> actual = performanceAdGroupValidation
                .validateAdGroups(clientInfo.getClientId(), singletonList(performanceAdGroup));
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(index(0), field(PerformanceAdGroup.FIELD_TO_USE_AS_NAME.name())),
                        StringDefects.admissibleChars()))));
    }

    @Test
    public void validateAdGroup_failureWhenTrackingParamsHasWrongSymbols() {
        PerformanceAdGroup performanceAdGroup = getPerformanceAdGroup()
                .withTrackingParams(EMOJI_SYMBOL);
        ValidationResult<List<PerformanceAdGroup>, Defect> actual = performanceAdGroupValidation
                .validateAdGroups(clientInfo.getClientId(), singletonList(performanceAdGroup));
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(index(0), field(PerformanceAdGroup.TRACKING_PARAMS.name())),
                        StringDefects.admissibleChars()))));
    }

    @Test
    public void getAdGroupClass_success() {
        assertThat(performanceAdGroupValidation.getAdGroupClass()).isEqualTo(PerformanceAdGroup.class);
    }

}
