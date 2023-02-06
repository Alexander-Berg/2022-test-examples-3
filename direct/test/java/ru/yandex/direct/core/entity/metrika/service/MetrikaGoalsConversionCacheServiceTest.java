package ru.yandex.direct.core.entity.metrika.service;

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
import org.mockito.Mock;

import ru.yandex.direct.common.lettuce.LettuceConnectionProvider;
import ru.yandex.direct.common.lettuce.LettuceExecuteException;
import ru.yandex.direct.core.entity.metrika.model.GoalsConversionVisitsCountCacheRecord;
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
import static ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsConversionCacheService.EXPIRATION_TIME;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.setUnion;

@ParametersAreNonnullByDefault
public class MetrikaGoalsConversionCacheServiceTest {

    private static final Long CLIENT_ID = 1L;
    private static final Long COUNTER_ID_1 = 1000001L;
    private static final Long COUNTER_ID_2 = 1000002L;

    private static final Set<Long> SINGLE_COUNTER_ID = Set.of(COUNTER_ID_1);
    private static final Set<Long> MULTIPLE_COUNTER_IDS = Set.of(COUNTER_ID_1, COUNTER_ID_2);

    private static final String SINGLE_COUNTER_JSON_VALUE = JsonUtils.toJson(getTestCacheRecord(CLIENT_ID,
            SINGLE_COUNTER_ID));
    private static final String MULTIPLE_COUNTERS_JSON_VALUE =
            JsonUtils.toJson(getTestCacheRecord(CLIENT_ID, MULTIPLE_COUNTER_IDS));

    @Mock
    private RedisAdvancedClusterCommands<String, String> commandsLettuce;

    private MetrikaGoalsConversionCacheService metrikaGoalsConversionCacheService;

    @Before
    public void before() {
        initMocks(this);
        LettuceConnectionProvider lettuce = mock(LettuceConnectionProvider.class);

        when(lettuce.call(anyString(), any()))
                .then(invocation -> {
                    Function<RedisAdvancedClusterCommands<String, String>, ?> cmd = invocation.getArgument(1);
                    return cmd.apply(commandsLettuce);
                });
        metrikaGoalsConversionCacheService = new MetrikaGoalsConversionCacheService(lettuce);
    }

    @Test
    public void saveToCacheSingleCounter_successful() {
        GoalsConversionVisitsCountCacheRecord recordToCache = getTestCacheRecord(CLIENT_ID, SINGLE_COUNTER_ID);
        metrikaGoalsConversionCacheService.saveToCache(recordToCache);
        String key = generateKey(CLIENT_ID, SINGLE_COUNTER_ID);

        verify(commandsLettuce).setex(eq(key), eq(EXPIRATION_TIME.getSeconds()), eq(SINGLE_COUNTER_JSON_VALUE));
    }

    @Test
    public void saveToCacheMultipleCounters_successful() {
        GoalsConversionVisitsCountCacheRecord recordToCache = getTestCacheRecord(CLIENT_ID, MULTIPLE_COUNTER_IDS);
        metrikaGoalsConversionCacheService.saveToCache(recordToCache);
        String key = generateKey(CLIENT_ID, MULTIPLE_COUNTER_IDS);

        verify(commandsLettuce).setex(eq(key), eq(EXPIRATION_TIME.getSeconds()), eq(MULTIPLE_COUNTERS_JSON_VALUE));
    }

    @Test
    public void getFromCacheSingleCounter_whenKeyExists() {
        String key = generateKey(CLIENT_ID, SINGLE_COUNTER_ID);
        doReturn(SINGLE_COUNTER_JSON_VALUE).when(commandsLettuce).get(eq(key));

        GoalsConversionVisitsCountCacheRecord recordFromCache =
                metrikaGoalsConversionCacheService.getFromCache(CLIENT_ID, SINGLE_COUNTER_ID);
        assumeThat(recordFromCache, notNullValue());

        SoftAssertions.assertSoftly(softly -> {
            // utm параметры вырезается перед чтением/сохранением в кеш
            softly.assertThat(recordFromCache.getCounterIds()).isEqualTo(SINGLE_COUNTER_ID);
            softly.assertThat(recordFromCache.getConversionVisitsCount()).isEqualTo(getCounversionVisitsCountTestData());
        });
    }

    @Test
    public void getFromCacheMultipleCounters_whenKeyExists() {
        String key = generateKey(CLIENT_ID, MULTIPLE_COUNTER_IDS);
        doReturn(MULTIPLE_COUNTERS_JSON_VALUE).when(commandsLettuce).get(eq(key));

        GoalsConversionVisitsCountCacheRecord recordFromCache =
                metrikaGoalsConversionCacheService.getFromCache(CLIENT_ID, MULTIPLE_COUNTER_IDS);
        assumeThat(recordFromCache, notNullValue());

        SoftAssertions.assertSoftly(softly -> {
            // utm параметры вырезается перед чтением/сохранением в кеш
            softly.assertThat(recordFromCache.getCounterIds()).isEqualTo(MULTIPLE_COUNTER_IDS);
            softly.assertThat(recordFromCache.getConversionVisitsCount()).isEqualTo(getCounversionVisitsCountTestData());
        });
    }

    @Test
    public void getFromCache_whenKeyDoesNotExist() {
        String key = generateKey(CLIENT_ID, SINGLE_COUNTER_ID);
        doReturn(null).when(commandsLettuce).get(eq(key));

        GoalsConversionVisitsCountCacheRecord recordFromCache =
                metrikaGoalsConversionCacheService.getFromCache(CLIENT_ID, SINGLE_COUNTER_ID);
        MatcherAssert.assertThat(recordFromCache, nullValue());
    }

    @Test
    public void getFromCache_whenRedisGetThrowException() {
        String key = generateKey(CLIENT_ID, SINGLE_COUNTER_ID);
        doThrow(new LettuceExecuteException()).when(commandsLettuce).get(eq(key));

        GoalsConversionVisitsCountCacheRecord recordFromCache =
                metrikaGoalsConversionCacheService.getFromCache(CLIENT_ID, SINGLE_COUNTER_ID);
        MatcherAssert.assertThat(recordFromCache, nullValue());
    }

    private static String generateKey(Long clientId, Set<Long> counterIds) {
        Set<Long> keySet = setUnion(Set.of(clientId), counterIds);
        String md5Hash = HashingUtils.getMd5HashUtf8AsHexString(
                keySet.stream().sorted().map(Object::toString).collect(Collectors.joining("_")));
        return String.format("goals-metrika-conversions-cache-%s", md5Hash);
    }

    private static Map<Long, Long> getCounversionVisitsCountTestData() {
        return ImmutableMap.<Long, Long>builder()
                .put(5000001L, 12L)
                .put(5000002L, 120L)
                .put(5000003L, 140L)
                .build();
    }

    private static GoalsConversionVisitsCountCacheRecord getTestCacheRecord(Long clientId, Set<Long> counterIds) {
        return new GoalsConversionVisitsCountCacheRecord()
                .withClientId(clientId)
                .withCounterIds(counterIds)
                .withConversionVisitsCount(getCounversionVisitsCountTestData());
    }
}
