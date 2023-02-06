package ru.yandex.direct.jobs.payment;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.balance.client.model.response.CheckBindingResponse;
import ru.yandex.direct.core.entity.payment.model.AutopayParams;
import ru.yandex.direct.core.entity.payment.model.TurnOnAutopayJobParams;
import ru.yandex.direct.core.entity.payment.model.TurnOnAutopayJobResult;
import ru.yandex.direct.core.entity.payment.service.AutopayService;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbqueue.model.DbQueueJob;
import ru.yandex.direct.dbqueue.model.DbQueueJobStatus;
import ru.yandex.direct.dbqueue.repository.DbQueueRepository;
import ru.yandex.direct.dbqueue.service.DbQueueService;
import ru.yandex.direct.dbqueue.steps.DbQueueSteps;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.TURN_ON_AUTOPAY;

@JobsTest
@ExtendWith(SpringExtension.class)
class TurnOnAutopayJobTest {

    private static final int SHARD = 1;
    private static final String PURCHASE_TOKEN = "some_token";
    private static final String PAYMETHOD_ID = "card-12345";

    @Autowired
    private DbQueueService dbQueueService;

    @Autowired
    private DbQueueSteps dbQueueSteps;

    @Autowired
    private DbQueueRepository dbQueueRepository;

    @Mock
    private BalanceService balanceService;

    @Mock
    private AutopayService autopayService;

    @Autowired
    private Steps steps;

    private UserInfo userInfo;
    private TurnOnAutopayJob job;
    private Long jobId;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);

        userInfo = steps.clientSteps().createClient(new ClientInfo().withShard(SHARD)).getChiefUserInfo();

        job = new TurnOnAutopayJob(SHARD, dbQueueService, balanceService, autopayService);
        dbQueueSteps.registerJobType(TURN_ON_AUTOPAY);
        dbQueueSteps.clearQueue(TURN_ON_AUTOPAY);

        when(balanceService.getUserCardIds(userInfo.getUid())).thenReturn(List.of(PAYMETHOD_ID));

        TurnOnAutopayJobParams params = new TurnOnAutopayJobParams()
                .withPurchaseToken(PURCHASE_TOKEN)
                .withAutopayParams(new AutopayParams());

        jobId = dbQueueRepository.insertJob(SHARD, TURN_ON_AUTOPAY, userInfo.getClientId(), userInfo.getUid(), params)
                .getId();
    }

    static Stream<Arguments> testData() {
        return Stream.of(
                arguments(true, "success", true, true, true),
                arguments(true, "error", false, true, false),
                arguments(false, "success", false, true, false),
                arguments(true, "unknown_status", false, true, false),
                arguments(true, "in_progress", false, false, null)
        );
    }

    @ParameterizedTest
    @MethodSource("testData")
    void test(boolean isLastChangeNotAfter, String bindingStatus,
              boolean needToTurnAutopay, boolean jobFinished, Boolean finishedWithSuccess) {
        when(autopayService.isLastChangeNotAfter(eq(SHARD), eq(userInfo.getClientId()), any()))
                .thenReturn(isLastChangeNotAfter);

        when(balanceService.checkBinding(userInfo.getUid(), PURCHASE_TOKEN))
                .thenReturn(
                        new CheckBindingResponse()
                                .withBindingResult(bindingStatus)
                                .withPaymentMethodId(PAYMETHOD_ID)
                );

        executeJob();

        verify(autopayService, times(needToTurnAutopay ? 1 : 0))
                .turnOnAutopay(eq(SHARD), eq(userInfo.getUid()), eq(userInfo.getClientId()), any());

        var foundJob = dbQueueRepository.findJobById(SHARD, TURN_ON_AUTOPAY, jobId);

        var expected = new DbQueueJob<TurnOnAutopayJobParams, TurnOnAutopayJobResult>()
                .withStatus(jobFinished ? DbQueueJobStatus.FINISHED : DbQueueJobStatus.NEW)
                .withResult(jobFinished ? new TurnOnAutopayJobResult(finishedWithSuccess) : null);
        assertThat(foundJob).isEqualToComparingOnlyGivenFields(expected, "status", "result");
    }

    private void executeJob() {
        assertThatCode(() -> job.execute()).doesNotThrowAnyException();
    }
}
