package ru.yandex.direct.jobs.util.juggler;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import ru.yandex.direct.ansiblejuggler.model.notifications.NotificationMethod;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.env.EnvironmentTypeProvider;
import ru.yandex.direct.env.ProductionOnly;
import ru.yandex.direct.env.TypicalEnvironment;
import ru.yandex.direct.jobs.util.juggler.checkinfo.JobWorkingCheckInfo;
import ru.yandex.direct.juggler.JugglerStatus;
import ru.yandex.direct.juggler.check.DirectNumericCheck;
import ru.yandex.direct.juggler.check.DirectNumericChecksBundle;
import ru.yandex.direct.juggler.check.annotation.JugglerCheck;
import ru.yandex.direct.juggler.check.annotation.OnChangeNotification;
import ru.yandex.direct.juggler.check.checkinfo.AnnotationBasedJugglerCheckInfo;
import ru.yandex.direct.juggler.check.checkinfo.NumericCheckInfo;
import ru.yandex.direct.juggler.check.model.CheckTag;
import ru.yandex.direct.scheduler.hourglass.HourglassJob;
import ru.yandex.direct.scheduler.support.DirectJob;
import ru.yandex.direct.scheduler.support.DirectShardedJob;
import ru.yandex.direct.utils.Condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.juggler.check.model.NotificationRecipient.CHAT_INTERNAL_SYSTEMS_MONITORING;

class JobsAppJugglerChecksProviderGenerationTest {
    private static final String TEST_HOST = "test.host.ru";
    private static final String TEST_SERVICE_NAME = "test.service.yandex";
    private static final List<Integer> TEST_PARAMS = Arrays.asList(1, 15, 7);
    private static final EnvironmentType TEST_ENV = EnvironmentType.DEV7;

    @JugglerCheck(ttl = @JugglerCheck.Duration(hours = 7), tags = {CheckTag.GROUP_INTERNAL_SYSTEMS})
    private static class NumericCheck extends DirectNumericCheck {
        public NumericCheck() {
            super(TEST_SERVICE_NAME, 1L, 10L, TEST_HOST, "descr", true);
        }

        @Override
        public List<String> getRawServiceNames() {
            return TEST_PARAMS.stream()
                    .map(p -> String.format("%s_item.%s", p, TEST_SERVICE_NAME))
                    .collect(Collectors.toList());
        }
    }

    @JugglerCheck(ttl = @JugglerCheck.Duration(hours = 4), tags = {
            CheckTag.GROUP_INTERNAL_SYSTEMS}, needCheck = ProductionOnly.class)
    private static class NeverCheckJob extends DirectJob {
        @Override
        public void execute() {
        }
    }

    @JugglerCheck(ttl = @JugglerCheck.Duration(hours = 4), tags = {CheckTag.GROUP_INTERNAL_SYSTEMS})
    private static class NotShardedJob extends DirectJob {
        @Override
        public void execute() {
        }
    }

    @JugglerCheck(
            ttl = @JugglerCheck.Duration(days = 1, minutes = 15),
            tags = {CheckTag.YT},
            notifications = {
                    @OnChangeNotification(recipient = CHAT_INTERNAL_SYSTEMS_MONITORING,
                            status = {JugglerStatus.OK, JugglerStatus.CRIT},
                            method = NotificationMethod.TELEGRAM),
            }
    )
    private static class ShardedJob extends DirectShardedJob {
        @Override
        public void execute() {
        }
    }

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private EnvironmentTypeProvider environmentTypeProvider;

    private Map<Class, JobWorkingCheckInfo> jobToCheck;
    private List<NumericCheckInfo> numericChecks;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);

        doAnswer(i -> {
            Class cls = i.getArgument(0);
            if (HourglassJob.class.isAssignableFrom(cls)) {
                return ImmutableMap.<String, HourglassJob>builder()
                        .put("key1", new NotShardedJob())
                        .put("key2", new ShardedJob())
                        .put("key3", new NeverCheckJob())
                        .build();
            } else if (DirectNumericCheck.class.isAssignableFrom(cls)) {
                return Collections.singletonMap("key9", new NumericCheck());
            } else if (DirectNumericChecksBundle.class.isAssignableFrom(cls)) {
                return Collections.emptyMap();
            }
            throw new IllegalArgumentException();
        }).when(applicationContext).getBeansOfType(any());

        doReturn((Condition) () -> true)
                .when(applicationContext).getBean(eq(TypicalEnvironment.class));
        doReturn((Condition) () -> false)
                .when(applicationContext).getBean(eq(ProductionOnly.class));

        doReturn(Arrays.asList(1, 3, 5))
                .when(shardHelper).dbShards();

        doReturn(TEST_ENV)
                .when(environmentTypeProvider).get();

        JobsAppJugglerChecksProvider provider =
                new JobsAppJugglerChecksProvider(shardHelper, environmentTypeProvider, applicationContext);

        jobToCheck = provider.getJobToCheck();
        numericChecks = provider.getNumericChecks();

        assertThat(jobToCheck)
                .containsOnlyKeys(NotShardedJob.class, ShardedJob.class);
    }

    @Test
    void checkRegistryGeneratedNotShardedWell() {
        JobWorkingCheckInfo checkInfo = jobToCheck.get(NotShardedJob.class);
        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(checkInfo.getServiceName())
                .isEqualTo("jobs.NotShardedJob.working." + TEST_ENV.getLegacyName());
        soft.assertThatThrownBy(() -> checkInfo.shardedServiceName(1))
                .isInstanceOf(IllegalStateException.class);
        soft.assertThat(checkInfo.getTTL())
                .isEqualTo(Duration.ofHours(4));
        soft.assertThat(checkInfo.getTags())
                .containsExactlyInAnyOrder(CheckTag.GROUP_INTERNAL_SYSTEMS);
        soft.assertThat(checkInfo.getNotifications())
                .isEmpty();

        soft.assertAll();
    }

    @Test
    void checkRegistryGeneratedShardedWell() {
        JobWorkingCheckInfo checkInfo = jobToCheck.get(ShardedJob.class);
        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(checkInfo.getServiceName())
                .isEqualTo("jobs.ShardedJob.working." + TEST_ENV.getLegacyName());
        soft.assertThat(checkInfo.shardedServiceName(1))
                .isEqualTo("jobs.ShardedJob.working." + TEST_ENV.getLegacyName() + ".shard_1");
        soft.assertThat(checkInfo.getTTL())
                .isEqualTo(Duration.ofDays(1).plusMinutes(15));
        soft.assertThat(checkInfo.getTags())
                .containsExactlyInAnyOrder(CheckTag.YT);
        soft.assertThat(checkInfo.getNotifications())
                .size().isEqualTo(1);

        soft.assertAll();
    }

    @Test
    void checkRegistryNotGeneratedBadEnv() {
        JobWorkingCheckInfo checkInfo = jobToCheck.get(NeverCheckJob.class);
        assertThat(checkInfo)
                .isNull();
    }

    @Test
    void numericCheckCorrect() {
        assertThat(numericChecks)
                .size().isEqualTo(1);

        AnnotationBasedJugglerCheckInfo checkInfo = numericChecks.get(0);

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(checkInfo.getServiceName())
                .isEqualTo(TEST_SERVICE_NAME + "." + TEST_ENV.getLegacyName());
        soft.assertThat(checkInfo.getTTL())
                .isEqualTo(Duration.ofHours(7));
        soft.assertThat(checkInfo.getTags())
                .containsExactlyInAnyOrder(CheckTag.GROUP_INTERNAL_SYSTEMS);
        soft.assertThat(checkInfo.getNotifications())
                .isEmpty();
        soft.assertThat(checkInfo)
                .isInstanceOf(NumericCheckInfo.class);

        soft.assertAll();
    }
}
