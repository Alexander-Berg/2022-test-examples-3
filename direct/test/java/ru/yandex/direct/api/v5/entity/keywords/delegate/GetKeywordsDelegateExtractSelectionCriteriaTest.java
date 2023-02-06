package ru.yandex.direct.api.v5.entity.keywords.delegate;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.general.ServingStatusEnum;
import com.yandex.direct.api.v5.keywords.GetRequest;
import com.yandex.direct.api.v5.keywords.KeywordStateSelectionEnum;
import com.yandex.direct.api.v5.keywords.KeywordStatusSelectionEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.api.v5.entity.keywords.converter.KeywordsGetRequestConverter;
import ru.yandex.direct.api.v5.entity.keywords.converter.KeywordsGetResponseConverter;
import ru.yandex.direct.api.v5.entity.keywords.validation.KeywordsGetRequestValidator;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.common.util.PropertyFilter;
import ru.yandex.direct.core.entity.bids.container.ShowConditionSelectionCriteria;
import ru.yandex.direct.core.entity.bids.container.ShowConditionStateSelection;
import ru.yandex.direct.core.entity.bids.container.ShowConditionStatusSelection;
import ru.yandex.direct.core.entity.bids.service.BidService;
import ru.yandex.direct.core.entity.keyword.model.ServingStatus;
import ru.yandex.direct.core.entity.keyword.service.KeywordService;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchService;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFields;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.DateTimeUtils.MOSCOW_TIMEZONE;
import static ru.yandex.direct.utils.DateTimeUtils.MSK;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class GetKeywordsDelegateExtractSelectionCriteriaTest {

    private static final List<Long> ids = asList(1L, 2L, 3L);
    private static final List<Long> idsWithDups = asList(1L, 2L, 3L, 1L, 2L, 3L);
    private static final Set<Long> expectedIds = new HashSet<>(ids);

    private static final List<KeywordStateSelectionEnum> states = asList(KeywordStateSelectionEnum.values());
    private static final List<KeywordStateSelectionEnum> statesWithDups =
            Stream.of(states, states).flatMap(Collection::stream).collect(toList());
    private static final ShowConditionStateSelection[] keywordStates = ShowConditionStateSelection.values();
    private static final Set<ShowConditionStateSelection> expectedStates = new HashSet<>(asList(keywordStates));

    private static final List<KeywordStatusSelectionEnum> statuses = asList(KeywordStatusSelectionEnum.values());
    private static final List<KeywordStatusSelectionEnum> statusesWithDups =
            Stream.of(statuses, statuses).flatMap(Collection::stream).collect(toList());
    private static final ShowConditionStatusSelection[] keywordStatuses = ShowConditionStatusSelection.values();
    private static final Set<ShowConditionStatusSelection> expectedStatuses = new HashSet<>(asList(keywordStatuses));

    private static final List<ServingStatusEnum> servingStatuses =
            asList(ServingStatusEnum.values());
    private static final List<ServingStatusEnum> servingStatusesWithDups =
            Stream.of(servingStatuses, servingStatuses).flatMap(Collection::stream).collect(toList());
    private static final ServingStatus[] intServingStatuses =
            new ServingStatus[]{ServingStatus.ELIGIBLE, ServingStatus.RARELY_SERVED};
    private static final Set<ServingStatus> expectedServingStatuses = new HashSet<>(asList(intServingStatuses));
    private static final String modifierSince = "2015-05-24T23:59:59Z";
    private static final LocalDateTime expectedModifierSince =
            ZonedDateTime.parse(modifierSince, DateTimeFormatter.ISO_OFFSET_DATE_TIME).withZoneSameInstant(
                    MSK).toLocalDateTime();

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public com.yandex.direct.api.v5.keywords.KeywordsSelectionCriteria selectionCriteria;

    @Parameterized.Parameter(2)
    public ShowConditionSelectionCriteria expectedSelectionCriteria;

    @Parameterized.Parameter(3)
    public DefaultCompareStrategy strategy;

    private GetKeywordsDelegate delegate;
    private KeywordsGetRequestConverter converter;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {"without all criterias", buildSelectionCriteria(), new ShowConditionSelectionCriteria(), null},
                {"with ids", buildSelectionCriteria().withIds(ids),
                        new ShowConditionSelectionCriteria().withShowConditionIds(expectedIds),
                        null},
                {"with ids with duplicate", buildSelectionCriteria().withIds(idsWithDups),
                        new ShowConditionSelectionCriteria().withShowConditionIds(expectedIds), null},
                {"with adgroup ids", buildSelectionCriteria().withAdGroupIds(ids),
                        new ShowConditionSelectionCriteria().withAdGroupIds(expectedIds), null},
                {"with adgroup ids with duplicate", buildSelectionCriteria().withAdGroupIds(idsWithDups),
                        new ShowConditionSelectionCriteria().withAdGroupIds(expectedIds), null},
                {"with campaign ids", buildSelectionCriteria().withCampaignIds(ids),
                        new ShowConditionSelectionCriteria().withCampaignIds(expectedIds), null},
                {"with campaign ids with duplicate", buildSelectionCriteria().withCampaignIds(idsWithDups),
                        new ShowConditionSelectionCriteria().withCampaignIds(expectedIds), null},
                {"with states", buildSelectionCriteria().withStates(states),
                        new ShowConditionSelectionCriteria().withStates(expectedStates),
                        allFields().forFields(newPath("states")).useMatcher(containsInAnyOrder(keywordStates))},
                {"with states with duplicate", buildSelectionCriteria().withStates(statesWithDups),
                        new ShowConditionSelectionCriteria().withStates(expectedStates),
                        allFields().forFields(newPath("states")).useMatcher(containsInAnyOrder(keywordStates))},
                {"with statuses", buildSelectionCriteria().withStatuses(statuses),
                        new ShowConditionSelectionCriteria().withStatuses(expectedStatuses),
                        allFields().forFields(newPath("statuses")).useMatcher(containsInAnyOrder(keywordStatuses))},
                {"with statuses with duplicate", buildSelectionCriteria().withStatuses(statusesWithDups),
                        new ShowConditionSelectionCriteria().withStatuses(expectedStatuses),
                        allFields().forFields(newPath("statuses")).useMatcher(containsInAnyOrder(keywordStatuses))},
                {"with servingstatuses", buildSelectionCriteria().withServingStatuses(servingStatuses),
                        new ShowConditionSelectionCriteria().withServingStatuses(expectedServingStatuses),
                        allFields().forFields(newPath("servingstatuses")).useMatcher(
                                containsInAnyOrder(servingStatuses))},
                {"with servingstatuses with duplicate",
                        buildSelectionCriteria().withServingStatuses(servingStatusesWithDups),
                        new ShowConditionSelectionCriteria().withServingStatuses(expectedServingStatuses),
                        allFields().forFields(newPath("servingstatuses")).useMatcher(
                                containsInAnyOrder(servingStatuses))},
                {"with all criterias",
                        buildSelectionCriteria()
                                .withIds(ids)
                                .withAdGroupIds(ids)
                                .withCampaignIds(ids)
                                .withStates(states)
                                .withStatuses(statuses)
                                .withServingStatuses(servingStatuses)
                                .withModifiedSince(modifierSince),
                        new ShowConditionSelectionCriteria()
                                .withShowConditionIds(expectedIds)
                                .withAdGroupIds(expectedIds)
                                .withCampaignIds(expectedIds)
                                .withStates(expectedStates)
                                .withStatuses(expectedStatuses)
                                .withServingStatuses(expectedServingStatuses)
                                .withModifiedSince(expectedModifierSince),
                        allFields()
                                .forFields(newPath("states")).useMatcher(containsInAnyOrder(keywordStates))
                                .forFields(newPath("statuses")).useMatcher(containsInAnyOrder(keywordStatuses))
                                .forFields(newPath("servingstatuses")).useMatcher(containsInAnyOrder(servingStatuses))},
        };
    }

    private static com.yandex.direct.api.v5.keywords.KeywordsSelectionCriteria buildSelectionCriteria() {
        return new com.yandex.direct.api.v5.keywords.KeywordsSelectionCriteria();
    }

    @Before
    public void prepare() {
        converter = new KeywordsGetRequestConverter(MOSCOW_TIMEZONE);
        delegate = new GetKeywordsDelegate(mock(ApiAuthenticationSource.class),
                mock(KeywordService.class), mock(RelevanceMatchService.class), mock(BidService.class),
                mock(PropertyFilter.class), mock(KeywordsGetResponseConverter.class),
                mock(KeywordsGetRequestValidator.class), converter);
    }

    @Test
    public void test() {
        ShowConditionSelectionCriteria selectionCriteria =
                delegate.extractSelectionCriteria(new GetRequest().withSelectionCriteria(this.selectionCriteria));

        BeanDifferMatcher<ShowConditionSelectionCriteria> matcher = beanDiffer(expectedSelectionCriteria);

        if (strategy != null) {
            matcher.useCompareStrategy(strategy);
        }

        assertThat(selectionCriteria).is(matchedBy(matcher));
    }

}

