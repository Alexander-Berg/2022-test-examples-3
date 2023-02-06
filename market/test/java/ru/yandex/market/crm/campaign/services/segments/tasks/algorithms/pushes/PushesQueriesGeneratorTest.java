package ru.yandex.market.crm.campaign.services.segments.tasks.algorithms.pushes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.crm.campaign.domain.actions.periodic.ActionExecutedEvent;
import ru.yandex.market.crm.campaign.domain.actions.status.BuildSegmentStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.IssueBunchStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.SendPushesStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.StepStatus;
import ru.yandex.market.crm.campaign.domain.periodic.EventType;
import ru.yandex.market.crm.campaign.domain.sending.PushSendingFactInfo;
import ru.yandex.market.crm.campaign.domain.sending.SendingFactType;
import ru.yandex.market.crm.campaign.domain.sending.periodic.UploadedEvent;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.services.actions.periodic.ActionEventsDAO;
import ru.yandex.market.crm.campaign.services.segments.tasks.algorithms.sendings.Entity;
import ru.yandex.market.crm.campaign.services.segments.tasks.algorithms.sendings.Query;
import ru.yandex.market.crm.campaign.services.segments.tasks.algorithms.sendings.RangeQuery;
import ru.yandex.market.crm.campaign.services.sending.facts.PushSendingFactInfoDAO;
import ru.yandex.market.crm.campaign.services.sending.periodic.PushSendingEventsDAO;
import ru.yandex.market.crm.campaign.test.StepStatusDAOMock;
import ru.yandex.market.crm.core.domain.CommonDateRange;
import ru.yandex.market.crm.platform.commons.SendingType;
import ru.yandex.market.crm.util.LiluCollectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author apershukov
 */
@ExtendWith(MockitoExtension.class)
class PushesQueriesGeneratorTest {

    private static final String SENDING_1 = "sending_1";
    private static final String SENDING_2 = "sending_2";

    @Mock
    private PushSendingFactInfoDAO pushSendingFactInfoDAO;

    @Mock
    private PushSendingEventsDAO pushSendingEventsDAO;

    @Mock
    private ActionEventsDAO actionEventsDAO;

    private StepStatusDAOMock stepsStatusDAO;

    private PushesQueriesGenerator generator;

    @BeforeEach
    void setUp() {
        stepsStatusDAO = new StepStatusDAOMock();
        generator = new PushesQueriesGenerator(
                pushSendingFactInfoDAO,
                pushSendingEventsDAO,
                stepsStatusDAO,
                actionEventsDAO
        );
    }

    /**
     * В случае если параметры, влияющие на временные рамки в которых будут
     * рассматриваться факты отправки не заданы, запросы подбираются с расчетом
     * на год с момента запуска вычисления
     */
    @Test
    void testDefaultQuerySet() {
        var params = new PushAlgorithmParams();

        var queries = generator.generate(params);
        assertThat(queries, hasSize(2));

        var rangeQuery = queries.get(0);
        assertEquals(Query.Type.RANGE, rangeQuery.getType());

        var now = today();
        var expectedPeriod = new CommonDateRange(now.minusYears(1), now.minusDays(1));
        var periods = ((RangeQuery) rangeQuery).getPeriods();
        assertThat(periods, hasSize(1));
        assertPeriod(expectedPeriod, periods.get(0));

        var recentQuery = queries.get(1);
        assertEquals(Query.Type.RECENT, recentQuery.getType());
    }

    /**
     * Если в параметрах задан период, запросы будут сделаны только к таблицам этого периода
     */
    @Test
    void testConsiderPushesInSpecifiedPeriod() {
        var now = today();
        var period = new CommonDateRange(now.minusDays(14), now);

        var params = new PushAlgorithmParams()
                .setPeriod(period);

        var queries = generator.generate(params);
        assertThat(queries, hasSize(2));

        var rangeQuery = queries.get(0);
        assertEquals(Query.Type.RANGE, rangeQuery.getType());

        var expectedPeriod = new CommonDateRange(period.getStartDate(), now.minusDays(1));
        var periods = ((RangeQuery) rangeQuery).getPeriods();
        assertThat(periods, hasSize(1));
        assertPeriod(expectedPeriod, periods.get(0));

        var recentQuery = queries.get(1);
        assertEquals(Query.Type.RECENT, recentQuery.getType());
    }

    /**
     * Если в параметрах задан период, не включающий текущую дату, итоговые запросы не
     * включают запрос к таблице recent
     */
    @Test
    void testDoNotQueryRecentTableIfTodayIsNotIncludedInPeirod() {
        var now = today();
        var period = new CommonDateRange(now.minusDays(14), now.minusDays(7));

        var params = new PushAlgorithmParams()
                .setPeriod(period);

        var queries = generator.generate(params);
        assertThat(queries, hasSize(1));

        var query = queries.get(0);
        assertEquals(Query.Type.RANGE, query.getType());

        var periods = ((RangeQuery) query).getPeriods();
        assertThat(periods, hasSize(1));
        assertPeriod(period, periods.get(0));
    }

    /**
     * Если заданный период перекрывает только сегодняшний день запрос делается только
     * к таблице recent
     */
    @Test
    void testForTodayOnlyPeriodQueryRecentTable() {
        var now = today();
        var period = new CommonDateRange(now, now);

        var params = new PushAlgorithmParams()
                .setPeriod(period);

        var queries = generator.generate(params);
        assertThat(queries, hasSize(1));

        var query = queries.get(0);
        assertEquals(Query.Type.RECENT, query.getType());
    }

    /**
     * Если в параметрах в качестве рассылки указана одноразовая push-рассылка, имеющая
     * факт отправки, запросы делаются только к таблице дата которой соответствует дате отправки
     *
     * Интервал (включающий дату выгрузки рассылки), заданный в параметрах при этом игнорируется.
     */
    @Test
    void testQueryOnlyTablesThatMightContainSendingFacts() {
        var uploadTime = LocalDateTime.now().minusDays(30);
        prepareSendingFacts(SENDING_1, sendingFact(uploadTime));

        var params = new PushAlgorithmParams()
                .setEntities(List.of(disposableSending(SENDING_1)))
                .setPeriod(new CommonDateRange(128));

        var queries = generator.generate(params);
        assertThat(queries, hasSize(1));

        var query = queries.get(0);
        assertEquals(Query.Type.RANGE, query.getType());

        var periods = ((RangeQuery) query).getPeriods();
        assertThat(periods, hasSize(1));

        var expectedPeriod = new CommonDateRange(uploadTime.toLocalDate(), uploadTime.toLocalDate());
        assertPeriod(expectedPeriod, periods.get(0));
    }

    /**
     * Если у указанной рассылки есть несколько фактов выгрузки в разные дни запрос
     * будет делаться ко всем таблицам, соответствующим датам выгрузки
     */
    @Test
    void testQueryMultipleUploadDates() {
        var upload1Time = LocalDateTime.now().minusDays(30);
        var upload2Time = LocalDateTime.now().minusDays(25);

        prepareSendingFacts(SENDING_1,
                sendingFact(upload1Time),
                sendingFact(upload2Time)
        );

        var params = new PushAlgorithmParams()
                .setEntities(List.of(disposableSending(SENDING_1)));

        var queries = generator.generate(params);
        assertThat(queries, hasSize(1));

        var query = queries.get(0);
        assertEquals(Query.Type.RANGE, query.getType());

        var periods = ((RangeQuery) query).getPeriods();
        assertThat(periods, hasSize(2));

        var expectedPeriod1 = new CommonDateRange(upload1Time.toLocalDate(), upload1Time.toLocalDate());
        assertPeriod(expectedPeriod1, periods.get(0));

        var expectedPeriod2 = new CommonDateRange(upload2Time.toLocalDate(), upload2Time.toLocalDate());
        assertPeriod(expectedPeriod2, periods.get(1));
    }

    /**
     * Если у рассылки из параметров есть несколько фактов выгрузки в один и тот же день
     * периоды дат таблиц к которым будут делаться запросы не дублируются
     */
    @Test
    void testDoNotDuplicatePeriodsInQuery() {
        var uploadDay = today().minusDays(30).atStartOfDay();
        var upload1Time = uploadDay.plusHours(7);
        var upload2Time = uploadDay.plusHours(12);

        prepareSendingFacts(SENDING_1,
                sendingFact(upload1Time),
                sendingFact(upload2Time)
        );

        var params = new PushAlgorithmParams()
                .setEntities(List.of(disposableSending(SENDING_1)));

        var queries = generator.generate(params);
        assertThat(queries, hasSize(1));

        var query = queries.get(0);
        assertEquals(Query.Type.RANGE, query.getType());

        var periods = ((RangeQuery) query).getPeriods();
        assertThat(periods, hasSize(1));

        var expectedPeriod = new CommonDateRange(upload1Time.toLocalDate(), upload1Time.toLocalDate());
        assertPeriod(expectedPeriod, periods.get(0));
    }

    /**
     * Если в параметрах задано несколько разных одноразовых промо-рассылок,
     * запросы делаются ко всем таблицам, даты которых соответствуют датам выгрузки указанных рассылок
     */
    @Test
    void testQueryMultipleTablesIfMultipleSendingsSpecified() {
        var upload1Time = LocalDateTime.now().minusMonths(2);
        var upload2Time = LocalDateTime.now().minusMonths(1);

        prepareSendingFacts(SENDING_1, sendingFact(upload1Time));
        prepareSendingFacts(SENDING_2, sendingFact(upload2Time));

        var params = new PushAlgorithmParams()
                .setEntities(List.of(disposableSending(SENDING_1), disposableSending(SENDING_2)));

        var queries = generator.generate(params);
        assertThat(queries, hasSize(1));

        var query = queries.get(0);
        assertEquals(Query.Type.RANGE, query.getType());

        var periods = ((RangeQuery) query).getPeriods();
        assertThat(periods, hasSize(2));

        var expectedPeriod1 = new CommonDateRange(upload1Time.toLocalDate(), upload1Time.toLocalDate());
        assertPeriod(expectedPeriod1, periods.get(0));

        var expectedPeriod2 = new CommonDateRange(upload2Time.toLocalDate(), upload2Time.toLocalDate());
        assertPeriod(expectedPeriod2, periods.get(1));
    }

    /**
     * Если в параметрах условия указана рассылка, время выгрузки которой не совпадает с периодом
     * из тех же параметров никакие запросы выполняться не будут т. к. заранее известно что данных,
     * удовлетворяющих условию быть не может.
     */
    @Test
    void testNoQueriesIfPeriodDoesNotIntersectSendingUploadDate() {
        var uploadTime = LocalDateTime.now().minusMonths(2);

        prepareSendingFacts(SENDING_1, sendingFact(uploadTime));

        var params = new PushAlgorithmParams()
                .setPeriod(new CommonDateRange(30))
                .setEntities(List.of(disposableSending(SENDING_1)));

        var queries = generator.generate(params);
        assertThat(queries, empty());
    }

    /**
     * Если в параметрах задана регулярная рассылка, несколько итераций которой уже
     * успели отправиться
     */
    @Test
    void testQueryPeriodicSendingUploadDates() {
        var upload1Time = LocalDateTime.now().minusDays(14);
        var upload2Time = LocalDateTime.now().minusDays(7);

        preparePeriodicEvents(
                uploadedEvent(upload1Time),
                uploadedEvent(upload2Time)
        );

        var params = new PushAlgorithmParams()
                .setPeriod(new CommonDateRange(30))
                .setEntities(List.of(new Entity(SENDING_1, SendingType.PERIODIC_SENDING)));

        var queries = generator.generate(params);
        assertThat(queries, hasSize(1));

        var query = queries.get(0);
        assertEquals(Query.Type.RANGE, query.getType());

        var periods = ((RangeQuery) query).getPeriods();
        assertThat(periods, hasSize(2));

        var expectedPeriod1 = new CommonDateRange(upload1Time.toLocalDate(), upload1Time.toLocalDate());
        assertPeriod(expectedPeriod1, periods.get(0));

        var expectedPeriod2 = new CommonDateRange(upload2Time.toLocalDate(), upload2Time.toLocalDate());
        assertPeriod(expectedPeriod2, periods.get(1));
    }

    /**
     * Если в параметрах была указана одноразовая акция с несколькими шагами отправки уведомлений,
     * выгружавшихся в разное время запрос делается ко всем таблицам, соответствующим датам выгрузок
     */
    @Test
    void testQueryTablesWithUploadingOfDisposableAction() {
        var upload1Time = LocalDateTime.of(today().minusDays(10), LocalTime.of(13, 0));

        prepareStepStatuses(
                finishedSegmentStep(),
                new IssueBunchStepStatus()
                        .setStepId("issue_coins")
                        .setStageStatus(StageStatus.FINISHED),
                pushesStepStatus(upload1Time, upload1Time.plusHours(1)),
                pushesStepStatus(StageStatus.IN_PROGRESS, now(), null)
        );

        var params = new PushAlgorithmParams()
                .setEntities(List.of(new Entity(SENDING_1, SendingType.ACTION)));

        var queries = generator.generate(params);
        assertThat(queries, hasSize(2));

        var rangeQuery = queries.get(0);
        assertEquals(Query.Type.RANGE, rangeQuery.getType());
        var periods = ((RangeQuery) rangeQuery).getPeriods();
        assertThat(periods, hasSize(1));

        var expectedPeriod = new CommonDateRange(upload1Time.toLocalDate(), upload1Time.toLocalDate());
        assertPeriod(expectedPeriod, periods.get(0));

        var recentQuery = queries.get(1);
        assertEquals(Query.Type.RECENT, recentQuery.getType());
    }

    /**
     * Если в параметрах указана регулярная акция с несколькими выполнениями, запросы делаются
     * ко всем таблицам, даты которых соответствуют датам выгрузки оповещений из шагов отправки
     */
    @Test
    void testQueryPeriodicActionUploadTables() {
        var upload1Time = now().minusDays(14);
        var upload2Time = now().minusDays(7);

        preparePeriodActionExecutionEvents(
                executedEvent(upload1Time),
                executedEvent(upload2Time)
        );

        var params = new PushAlgorithmParams()
                .setEntities(List.of(new Entity(SENDING_1, SendingType.PERIODIC_ACTION)));

        var queries = generator.generate(params);
        assertThat(queries, hasSize(1));

        var query = queries.get(0);
        assertEquals(Query.Type.RANGE, query.getType());

        var periods = ((RangeQuery) query).getPeriods();
        assertThat(periods, hasSize(2));

        var expectedPeriod1 = new CommonDateRange(upload1Time.toLocalDate(), upload1Time.toLocalDate());
        assertPeriod(expectedPeriod1, periods.get(0));

        var expectedPeriod2 = new CommonDateRange(upload2Time.toLocalDate(), upload2Time.toLocalDate());
        assertPeriod(expectedPeriod2, periods.get(1));
    }

    @NotNull
    private LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Europe/Moscow"));
    }

    private static void assertPeriod(CommonDateRange expected, CommonDateRange actual) {
        assertAll(
                () -> assertEquals(expected.getStartDate(), actual.getStartDate(), "Unexpected start date"),
                () -> assertEquals(expected.getEndDate(), actual.getEndDate(), "Unexpected end date")
        );
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneId.of("Europe/Moscow"));
    }

    private void prepareSendingFacts(String sendingId, PushSendingFactInfo... facts) {
        when(pushSendingFactInfoDAO.getSendingFacts(sendingId))
                .thenReturn(List.of(facts));
    }

    private void preparePeriodicEvents(UploadedEvent... events) {
        when(pushSendingEventsDAO.getEvents(eq(SENDING_1), eq(EventType.UPLOADED), any()))
                .thenReturn(List.of(events));
    }

    private void prepareStepStatuses(StepStatus<?>... statuses) {
        for (var status : statuses) {
            stepsStatusDAO.upsert(SENDING_1, status);
        }
    }

    private void preparePeriodActionExecutionEvents(ActionExecutedEvent... events) {
        when(actionEventsDAO.getEvents(eq(SENDING_1), eq(EventType.ACTION_EXECUTED), any()))
                .thenReturn(List.of(events));
    }

    private static PushSendingFactInfo sendingFact(LocalDateTime startUploadingTime) {
        var fact = new PushSendingFactInfo();
        fact.setType(SendingFactType.FINAL);
        fact.setStartUploadTime(startUploadingTime);
        fact.setUploadTime(startUploadingTime.plusSeconds(1));
        return fact;
    }

    private static UploadedEvent uploadedEvent(LocalDateTime startUploadingTime) {
        var event = new UploadedEvent();
        event.setTime(startUploadingTime);
        event.setFinishTime(startUploadingTime.plusSeconds(1));
        event.setStatus(StageStatus.FINISHED);
        return event;
    }

    private static Entity disposableSending(String id) {
        return new Entity(id, SendingType.PROMO);
    }

    private static BuildSegmentStepStatus finishedSegmentStep() {
        return new BuildSegmentStepStatus()
                .setStageStatus(StageStatus.FINISHED);
    }

    private static SendPushesStepStatus pushesStepStatus(LocalDateTime startTime, LocalDateTime finishTime) {
        return pushesStepStatus(StageStatus.FINISHED, startTime, finishTime);
    }

    private static SendPushesStepStatus pushesStepStatus(StageStatus stageStatus,
                                                         LocalDateTime startTime,
                                                         LocalDateTime finishTime) {
        return new SendPushesStepStatus()
                .setStepId(UUID.randomUUID().toString())
                .setStageStatus(stageStatus)
                .setStartTime(startTime)
                .setFinishTime(finishTime);
    }

    private static ActionExecutedEvent executedEvent(LocalDateTime uploadTime) {
        var statuses = List.of(
                finishedSegmentStep(),
                pushesStepStatus(uploadTime, uploadTime.plusMinutes(1))
        );

        return new ActionExecutedEvent()
                .setStatus(StageStatus.FINISHED)
                .setStepStatuses(
                        statuses.stream()
                                .collect(LiluCollectors.index(StepStatus::getStepId))
                );
    }
}
