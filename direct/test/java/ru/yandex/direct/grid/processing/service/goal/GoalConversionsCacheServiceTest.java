package ru.yandex.direct.grid.processing.service.goal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableMap;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;

import ru.yandex.direct.common.lettuce.LettuceConnectionProvider;
import ru.yandex.direct.common.lettuce.LettuceExecuteException;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.goal.model.GoalConversionVisit;
import ru.yandex.direct.grid.core.entity.goal.model.GoalsConversionsCacheRecord;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.utils.HashingUtils;
import ru.yandex.direct.utils.JsonUtils;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.grid.processing.service.goal.GoalConversionsCacheService.CACHE_KEY_PATTERN;
import static ru.yandex.direct.grid.processing.service.goal.GoalConversionsCacheService.EXPIRATION_TIME;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.setUnion;


@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class GoalConversionsCacheServiceTest {
    private static final ClientId CLIENT_ID = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
    private static final Long COUNTER_ID_1 = 1000001L;
    private static final Long COUNTER_ID_2 = 1000002L;

    private static final Set<Long> SINGLE_COUNTER_ID = Set.of(COUNTER_ID_1);
    private static final Set<Long> MULTIPLE_COUNTER_IDS = Set.of(COUNTER_ID_1, COUNTER_ID_2);

    private static final String SINGLE_COUNTER_KEY =
            generateKey(CLIENT_ID.asLong(), SINGLE_COUNTER_ID, false);
    private static final String SINGLE_COUNTER_WITH_UNAVAILABLE_GOALS_KEY =
            generateKey(CLIENT_ID.asLong(), SINGLE_COUNTER_ID, true);
    private static final String MULTIPLE_COUNTER_KEY =
            generateKey(CLIENT_ID.asLong(), MULTIPLE_COUNTER_IDS, false);
    private static final String MULTIPLE_COUNTER_WITH_UNAVAILABLE_GOALS_KEY =
            generateKey(CLIENT_ID.asLong(), MULTIPLE_COUNTER_IDS, true);

    private static final String SINGLE_COUNTER_JSON_VALUE = JsonUtils.toJson(getTestCacheRecord(CLIENT_ID.asLong(),
            SINGLE_COUNTER_ID));
    private static final String MULTIPLE_COUNTERS_JSON_VALUE =
            JsonUtils.toJson(getTestCacheRecord(CLIENT_ID.asLong(), MULTIPLE_COUNTER_IDS));

    @Mock
    private RedisAdvancedClusterCommands<String, String> commandsLettuce;

    private GoalConversionsCacheService goalConversionsCacheService;

    @Parameterized.Parameter
    public boolean withUnavailableGoals;

    @Parameterized.Parameters
    public static Collection testData() {
        return List.of(false, true);
    }

    @Before
    public void before() {
        initMocks(this);
        LettuceConnectionProvider lettuce = mock(LettuceConnectionProvider.class);

        when(lettuce.call(anyString(), any()))
                .then(invocation -> {
                    Function<RedisAdvancedClusterCommands<String, String>, ?> cmd = invocation.getArgument(1);
                    return cmd.apply(commandsLettuce);
                });
        goalConversionsCacheService = new GoalConversionsCacheService(lettuce);
    }

    @Test
    public void saveToCacheSingleCounter_successful() {
        GoalsConversionsCacheRecord recordToCache = getTestCacheRecord(CLIENT_ID.asLong(), SINGLE_COUNTER_ID);
        goalConversionsCacheService.saveToCache(recordToCache, withUnavailableGoals);

        var expectKey = withUnavailableGoals
                ? SINGLE_COUNTER_WITH_UNAVAILABLE_GOALS_KEY
                : SINGLE_COUNTER_KEY;

        verify(commandsLettuce).setex(eq(expectKey), eq(EXPIRATION_TIME.getSeconds()),
                eq(SINGLE_COUNTER_JSON_VALUE));
    }

    @Test
    public void saveToCacheMultipleCounters_successful() {
        GoalsConversionsCacheRecord recordToCache = getTestCacheRecord(CLIENT_ID.asLong(), MULTIPLE_COUNTER_IDS);
        goalConversionsCacheService.saveToCache(recordToCache, withUnavailableGoals);

        var expectKey = withUnavailableGoals
                ? MULTIPLE_COUNTER_WITH_UNAVAILABLE_GOALS_KEY
                : MULTIPLE_COUNTER_KEY;

        verify(commandsLettuce).setex(eq(expectKey), eq(EXPIRATION_TIME.getSeconds()),
                eq(MULTIPLE_COUNTERS_JSON_VALUE));
    }

    @Test
    public void getFromCacheSingleCounter_whenKeyExists() {
        var key = withUnavailableGoals ? SINGLE_COUNTER_WITH_UNAVAILABLE_GOALS_KEY : SINGLE_COUNTER_KEY;
        doReturn(SINGLE_COUNTER_JSON_VALUE).when(commandsLettuce).get(eq(key));

        GoalsConversionsCacheRecord recordFromCache =
                goalConversionsCacheService.getFromCache(CLIENT_ID.asLong(), SINGLE_COUNTER_ID, withUnavailableGoals);
        assumeThat(recordFromCache, notNullValue());

        SoftAssertions.assertSoftly(softly -> {
            // utm параметры вырезается перед чтением/сохранением в кеш
            softly.assertThat(recordFromCache.getCounterIds()).isEqualTo(SINGLE_COUNTER_ID);
            softly.assertThat(recordFromCache.getConversionVisitsCount()).isEqualTo(getCounversionVisitsCountTestData());
        });
    }

    @Test
    public void getFromCacheMultipleCounters_whenKeyExists() {
        var key = withUnavailableGoals ? MULTIPLE_COUNTER_WITH_UNAVAILABLE_GOALS_KEY : MULTIPLE_COUNTER_KEY;
        doReturn(MULTIPLE_COUNTERS_JSON_VALUE).when(commandsLettuce).get(eq(key));

        GoalsConversionsCacheRecord recordFromCache = goalConversionsCacheService.getFromCache(CLIENT_ID.asLong(),
                MULTIPLE_COUNTER_IDS, withUnavailableGoals);
        assumeThat(recordFromCache, notNullValue());

        SoftAssertions.assertSoftly(softly -> {
            // utm параметры вырезается перед чтением/сохранением в кеш
            softly.assertThat(recordFromCache.getCounterIds()).isEqualTo(MULTIPLE_COUNTER_IDS);
            softly.assertThat(recordFromCache.getConversionVisitsCount()).isEqualTo(getCounversionVisitsCountTestData());
        });
    }

    @Test
    public void getFromCache_whenKeyDoesNotExist() {
        var key = withUnavailableGoals ? SINGLE_COUNTER_WITH_UNAVAILABLE_GOALS_KEY : SINGLE_COUNTER_KEY;
        doReturn(null).when(commandsLettuce).get(eq(key));

        GoalsConversionsCacheRecord recordFromCache = goalConversionsCacheService.getFromCache(CLIENT_ID.asLong(),
                SINGLE_COUNTER_ID, withUnavailableGoals);
        MatcherAssert.assertThat(recordFromCache, nullValue());
    }

    @Test
    public void getFromCache_whenRedisGetThrowException() {
        String key = generateKey(CLIENT_ID.asLong(), SINGLE_COUNTER_ID, withUnavailableGoals);
        doThrow(new LettuceExecuteException()).when(commandsLettuce).get(eq(key));

        GoalsConversionsCacheRecord recordFromCache = goalConversionsCacheService.getFromCache(CLIENT_ID.asLong(),
                SINGLE_COUNTER_ID, withUnavailableGoals);
        MatcherAssert.assertThat(recordFromCache, nullValue());
    }

    private static String generateKey(Long clientId, Set<Long> counterIds, boolean withUnavailableGoals) {
        Set<Long> keySet = setUnion(Set.of(clientId), counterIds);
        keySet.add(withUnavailableGoals ? 1L : 0L);
        String md5Hash = HashingUtils.getMd5HashUtf8AsHexString(
                keySet.stream().map(Object::toString).sorted().collect(Collectors.joining("_")));
        return String.format(CACHE_KEY_PATTERN, md5Hash);
    }

    private static Map<Long, GoalConversionVisit> getCounversionVisitsCountTestData() {
        return ImmutableMap.<Long, GoalConversionVisit>builder()
                .put(5000001L, new GoalConversionVisit().withCount(12L))
                .put(5000002L, new GoalConversionVisit().withCount(120L))
                .put(5000003L, new GoalConversionVisit().withCount(140L))
                .build();
    }

    private static GoalsConversionsCacheRecord getTestCacheRecord(Long clientId, Set<Long> counterIds) {
        return new GoalsConversionsCacheRecord()
                .withClientId(clientId)
                .withCounterIds(counterIds)
                .withConversionVisitsCount(getCounversionVisitsCountTestData());
    }
}
