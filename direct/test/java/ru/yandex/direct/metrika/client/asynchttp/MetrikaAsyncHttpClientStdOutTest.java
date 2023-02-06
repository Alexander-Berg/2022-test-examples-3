package ru.yandex.direct.metrika.client.asynchttp;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.util.HashedWheelTimer;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcPropertyName;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.metrika.client.MetrikaConfiguration;
import ru.yandex.direct.metrika.client.model.request.RetargetingGoal;
import ru.yandex.direct.metrika.client.model.request.RetargetingGoalGroup;
import ru.yandex.direct.metrika.client.model.response.RetargetingCondition;
import ru.yandex.direct.metrika.client.model.response.UpdateCounterGrantsResponse;
import ru.yandex.direct.metrika.client.model.response.UserCounters;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmIntegrationImpl;

import static com.google.common.primitives.Longs.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.tvm.TvmService.DIRECT_SCRIPTS_TEST;
import static ru.yandex.direct.tvm.TvmService.METRIKA_API_TEST;
import static ru.yandex.direct.tvm.TvmService.METRIKA_AUDIENCE_TEST;
import static ru.yandex.direct.tvm.TvmService.METRIKA_INTERNAL_API_TEST;

/**
 * Выводит в stdout значения объектов возвращённых клиентом Метрики.
 * Данный тест может использоваться, в частности, при внесении изменений в транспорт к ручкам Метрики. Им проверяется,
 * что возвращаемые результаты у транкового и модифицированного клиентов полностью совпадают.
 */
@Ignore("Ходит в тестовые инстансы метрики")
public class MetrikaAsyncHttpClientStdOutTest {

    private static final List<Long> UIDS = asList(561470577L, 560864147L,
            562476698L, 205493856L, 562006141L, 562352172L, 562644600L, 562640878L, 562663702L, 562531046L);

    private MetrikaAsyncHttpClient client;

    @Before
    public void setUp() throws Exception {
        //TVM
        Map<String, Object> conf = new HashMap<>();
        conf.put("tvm.enabled", true);
        conf.put("tvm.app_id", DIRECT_SCRIPTS_TEST.getId());
        conf.put("tvm.api.url", "https://tvm-api.yandex.net");
        conf.put("tvm.api.error_delay", "5s");
        conf.put("tvm.secret", "file:////etc/direct-tokens/tvm2_direct-scripts-test");
        DirectConfig directConfig = DirectConfigFactory.getConfig(EnvironmentType.TESTING, conf);
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        TvmIntegration tvmIntegration = TvmIntegrationImpl.create(directConfig, scheduler);
        assumeThat(sa -> assertThat(tvmIntegration.getTicket(METRIKA_INTERNAL_API_TEST)).isNotNull());
        assumeThat(sa -> assertThat(tvmIntegration.getTicket(METRIKA_AUDIENCE_TEST)).isNotNull());
        assumeThat(sa -> assertThat(tvmIntegration.getTicket(METRIKA_API_TEST)).isNotNull());

        //MetrikaConfiguration
        MetrikaConfiguration metrikaConfiguration =
                new MetrikaConfiguration("http://internalapi-test.metrika.yandex.ru:8096",
                        "http://ipv6.audience-intapid-test.metrika.yandex.ru:8099",
                        "https://api-metrika-test.metrika.yandex.net");

        //AsyncHttpClient
        DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder();
        builder.setRequestTimeout(Ints.saturatedCast(Duration.ofSeconds(30).toMillis()));
        builder.setReadTimeout(Ints.saturatedCast(Duration.ofSeconds(30).toMillis()));
        builder.setConnectTimeout(Ints.saturatedCast(Duration.ofSeconds(10).toMillis()));
        builder.setConnectionTtl(Ints.saturatedCast(Duration.ofMinutes(1).toMillis()));
        builder.setPooledConnectionIdleTimeout(
                Ints.saturatedCast(Duration.ofSeconds(20).toMillis()));
        builder.setIoThreadsCount(2);
        builder.setNettyTimer(new HashedWheelTimer(
                new ThreadFactoryBuilder().setNameFormat("ahc-timer-%02d").setDaemon(true).build()));
        DefaultAsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient(builder.build());

        var ppcPropertiesSupport = mock(PpcPropertiesSupport.class);
        when(ppcPropertiesSupport.get(any(PpcPropertyName.class), any(Duration.class))).thenCallRealMethod();
        client = new MetrikaAsyncHttpClient(metrikaConfiguration, asyncHttpClient,
                tvmIntegration, ppcPropertiesSupport, false);
    }


    @Test
    public void getUsersCountersNum() {
        List<UserCounters> usersCountersNum = client.getUsersCountersNum(UIDS);
        assertThat(usersCountersNum).isNotEmpty();

        writeResult("getUsersCountersNum", usersCountersNum);
    }

    @Test
    public void getGoalsByUids() {
        Map<Long, List<RetargetingCondition>> goalsByUids = client.getGoalsByUids(UIDS);
        assertThat(goalsByUids).isNotEmpty();

        writeResult("getGoalsByUids", goalsByUids);
    }

    @Test
    public void estimateUsersByCondition() {
        int goalId = 2899978;
        int maxPeriodDays = 540;
        List<RetargetingGoal> retargetingGoals = singletonList(new RetargetingGoal(goalId, maxPeriodDays));
        List<RetargetingGoalGroup> condition = singletonList(new RetargetingGoalGroup(RetargetingGoalGroup.Type.OR,
                retargetingGoals));
        long usersCount = client.estimateUsersByCondition(condition);
        assertThat(usersCount).isGreaterThan(0L);

        writeResult("estimateUsersByCondition", usersCount);
    }

    @Test
    public void getProductImpressions() {
        long counterId = 42L;
        Double productImpressions = client.getProductImpressionsByCounterId(Set.of(counterId), 10).get(counterId);
        assertThat(productImpressions).isGreaterThan(0d);

        writeResult("getProductImpressions", productImpressions);
    }

    @Test
    public void getConversionVisitsCountByGoalIdForCounterIds() {
        //счетчик из документации метрики
        int counterId = 44147844;
        var conversionVisitsCountByGoalIdForCounterIds =
                client.getGoalsConversionInfoByCounterIds(List.of(counterId), 10);
        assertThat(conversionVisitsCountByGoalIdForCounterIds).isNotEmpty();

        writeResult("getVisitNumberByGoalIdForCounterIds", conversionVisitsCountByGoalIdForCounterIds);
    }

    @Test
    public void updateCounterGrants() {
        UpdateCounterGrantsResponse grants = client.updateCounterGrants(46836780, singleton("yndx-ajkon-super-1"));
        assertThat(grants.isSuccessful()).isTrue();

        writeResult("updateCounterGrants", grants);
    }

    private void writeResult(String methodName, Object result) {
        String resultString = String.format("\n\n#%s:\n%s", methodName, result.toString());
        System.out.println(resultString);
    }
}
