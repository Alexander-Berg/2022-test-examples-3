package ru.yandex.market.logistics.lom.jobs.executor;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamGroup;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createSegmentCancellationRequestIdPayload;

@DisplayName("Тесты обработки превышения дедлайна отмены заказа")
@ParametersAreNonnullByDefault
class ProcessLongCancellationsExecutorTest extends AbstractContextualTest {

    private static final Instant DEFAULT_TIME = Instant.parse("2020-12-05T14:00:00Z");

    @Autowired
    private ProcessLongCancellationsExecutor processLongCancellationsExecutor;

    @Autowired
    private LMSClient lmsClient;

    private final JobExecutionContext jobContext = mock(JobExecutionContext.class);

    @BeforeEach
    void setup() {
        clock.setFixed(DEFAULT_TIME, ZoneId.systemDefault());
    }

    @Test
    @DatabaseSetup("/jobs/executor/processLongCancellationsExecutor/before.xml")
    @ExpectedDatabase(
        value = "/jobs/executor/processLongCancellationsExecutor/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void execute() {
        mockLMS();
        orderCancellationProperties.setCancellationSlaAutoConfirmEnabled(true);

        processLongCancellationsExecutor.doJob(jobContext);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(1L, "1", 1L)
        );
    }

    private void mockLMS() {
        doReturn(List.of(
            getPartnerExternalParamGroup(100L),
            getPartnerExternalParamGroup(223L)
        ))
            .when(lmsClient)
            .getPartnerExternalParams(Set.of(PartnerExternalParamType.DISABLE_AUTO_CANCEL_AFTER_SLA));
    }

    private PartnerExternalParamGroup getPartnerExternalParamGroup(Long partnerId) {
        return new PartnerExternalParamGroup(
            partnerId,
            List.of(
                new PartnerExternalParam(
                    PartnerExternalParamType.DISABLE_AUTO_CANCEL_AFTER_SLA.toString(),
                    "",
                    "true"
                )
            )
        );
    }
}
