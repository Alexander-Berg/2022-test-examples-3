package ru.yandex.direct.jobs.balance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcPropertyName;
import ru.yandex.direct.common.db.PpcPropertyType;
import ru.yandex.direct.core.entity.balance.repository.BalanceInfoQueueRepository;
import ru.yandex.direct.dbschema.ppc.enums.BalanceInfoQueueObjType;
import ru.yandex.direct.dbschema.ppc.enums.BalanceInfoQueueSendStatus;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.solomon.SolomonPushClient;
import ru.yandex.monlib.metrics.registry.MetricRegistry;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.dbschema.ppc.tables.BalanceInfoQueue.BALANCE_INFO_QUEUE;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@JobsTest
@ExtendWith(SpringExtension.class)
class BalanceInfoQueueCleanerTest {

    private static final int SHARD = 1;
    private static final String CLEAR_BALANCE_INFO_QUEUE_PROPERTY =
            String.format("CLEAR_BALANCE_INFO_QUEUE_SHARD_%d", SHARD);

    private static final PpcPropertyName<LocalDate> CLEAR_BALANCE_INFO_QUEUE_PROPERTY_NAME =
            new PpcPropertyName<>(CLEAR_BALANCE_INFO_QUEUE_PROPERTY, PpcPropertyType.LOCAL_DATE);

    private static final int LIMIT_AGE_DAYS = 7; //выставляем равным BalanceInfoQueueCleaner.LIMIT_AGE_DAYS

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private BalanceInfoQueueRepository balanceInfoQueueRepository;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Mock
    private SolomonPushClient solomonPushClient;

    @Captor
    private ArgumentCaptor<MetricRegistry> captor;

    private BalanceInfoQueueCleaner jobUnderTest;

    private List<Long> balanceInfoQueueIds;

    @BeforeEach
    void init() {
        initMocks(this);
        balanceInfoQueueIds = new ArrayList<>();
        jobUnderTest =
                new BalanceInfoQueueCleaner(SHARD, balanceInfoQueueRepository, ppcPropertiesSupport, solomonPushClient);
    }

    /**
     * Заполняет таблицу ppc.balance_info_queue данными для тестов.
     *
     * @param objType    - тип объекта (cid | uid)
     * @param addTime    - время добавления записи
     * @param sendStatus - статус отправки в баланс (Wait, Sending, Send, Error)
     */
    private Long fillBallanceInfoQueueWith(BalanceInfoQueueObjType objType,
                                           LocalDateTime addTime, BalanceInfoQueueSendStatus sendStatus) {
        Long recId = dslContextProvider.ppc(SHARD)
                .insertInto(BALANCE_INFO_QUEUE)
                .columns(BALANCE_INFO_QUEUE.OPERATOR_UID, BALANCE_INFO_QUEUE.OBJ_TYPE, BALANCE_INFO_QUEUE.CID_OR_UID,
                        BALANCE_INFO_QUEUE.ADD_TIME,
                        BALANCE_INFO_QUEUE.SEND_STATUS, BALANCE_INFO_QUEUE.PRIORITY)
                .values(1L, objType, 12345L, addTime, sendStatus, 77L)
                .returning(BALANCE_INFO_QUEUE.ID)
                .fetchOne()
                .getId();
        balanceInfoQueueIds.add(recId);
        return recId;
    }

    /**
     * Получаем список id записей из ppc.balance_info_queue ограниченный списком созданных в тесте записей
     * (balanceInfoQueueIds)
     *
     * @return список всех id в таблице (из созданных в текущем тесте)
     */
    private List<Long> getBallanceInfoQueueIds() {
        return dslContextProvider.ppc(SHARD)
                .select(BALANCE_INFO_QUEUE.ID)
                .from(BALANCE_INFO_QUEUE)
                .where(BALANCE_INFO_QUEUE.ID.in(balanceInfoQueueIds))
                .fetch(BALANCE_INFO_QUEUE.ID);
    }

    private void executeJob() {
        assertThatCode(() -> jobUnderTest.execute())
                .doesNotThrowAnyException();
    }

    /**
     * Проверка, что записи с send_status = "Error" и возрастом < LIMIT_AGE_DAYS дней не удалятся
     */
    @Test
    void testOfNotDeletingRecordsWithStatusError() {
        LocalDateTime addTime = LocalDateTime.now().minusDays(LIMIT_AGE_DAYS).plusMinutes(1L);
        fillBallanceInfoQueueWith(BalanceInfoQueueObjType.cid, addTime, BalanceInfoQueueSendStatus.Error);
        fillBallanceInfoQueueWith(BalanceInfoQueueObjType.uid, addTime.plusDays(1), BalanceInfoQueueSendStatus.Error);

        executeJob();

        assertThat("проверяем что записи не удалились",
                getBallanceInfoQueueIds().size(),
                equalTo(2));
    }

    /**
     * Проверка, что записи с send_status = "Error", "Send" и возрастом > LIMIT_AGE_DAYS дней удалятся
     */
    @Test
    void testOfDeletingOutdatedRecordsWithStatusError() {
        LocalDateTime addTime = LocalDateTime.now().minusDays(LIMIT_AGE_DAYS + 1);
        fillBallanceInfoQueueWith(BalanceInfoQueueObjType.cid, addTime, BalanceInfoQueueSendStatus.Error);
        fillBallanceInfoQueueWith(BalanceInfoQueueObjType.cid, addTime, BalanceInfoQueueSendStatus.Send);
        fillBallanceInfoQueueWith(BalanceInfoQueueObjType.uid, addTime.minusDays(10), BalanceInfoQueueSendStatus.Error);
        fillBallanceInfoQueueWith(BalanceInfoQueueObjType.uid, addTime.minusDays(10), BalanceInfoQueueSendStatus.Send);

        executeJob();

        //проверяем, что отправленные в графит данные соответствууют кол-ву удаленных записей
        verify(solomonPushClient).sendMetrics(captor.capture());
        Assertions.assertThat(captor.getValue().estimateCount())
                .as("отправленные метрики")
                .isEqualTo(1);

        assertThat("проверяем что кол-во записей равно 0",
                getBallanceInfoQueueIds().size(),
                equalTo(0));
    }

    /**
     * Проверка, что все записи с send_status = "Send" удалятся вне зависимости от времени добавления.
     */
    @Test
    void testOfDeletingAnyRecordsWithStatusSend() {
        LocalDateTime addTime = LocalDateTime.now().minusDays(LIMIT_AGE_DAYS).plusMinutes(1L);
        fillBallanceInfoQueueWith(BalanceInfoQueueObjType.cid, addTime, BalanceInfoQueueSendStatus.Send);
        fillBallanceInfoQueueWith(BalanceInfoQueueObjType.uid, addTime.plusDays(1), BalanceInfoQueueSendStatus.Send);
        fillBallanceInfoQueueWith(BalanceInfoQueueObjType.cid, addTime.minusDays(1), BalanceInfoQueueSendStatus.Send);

        executeJob();

        assertThat("проверяем что кол-во записей равно 0",
                getBallanceInfoQueueIds().size(),
                equalTo(0));
    }


    /**
     * Проверка, что запись с send_status = "Wait" не удаляется вне зависимости от возраста.
     */
    @Test
    void testOfNonDeletingRecordsWithStatusWait() {
        LocalDateTime addTime = LocalDateTime.now().minusDays(LIMIT_AGE_DAYS);
        fillBallanceInfoQueueWith(BalanceInfoQueueObjType.cid, addTime.plusMinutes(1), BalanceInfoQueueSendStatus.Wait);
        fillBallanceInfoQueueWith(BalanceInfoQueueObjType.cid, addTime.minusDays(2), BalanceInfoQueueSendStatus.Wait);
        fillBallanceInfoQueueWith(BalanceInfoQueueObjType.uid, addTime.minusMonths(2), BalanceInfoQueueSendStatus.Wait);

        executeJob();

        assertThat("проверяем что записи не удалились",
                getBallanceInfoQueueIds().size(),
                equalTo(3));
    }

    /**
     * Проверка, что запись с send_status = "Sending" не удаляется вне зависимости от возраста.
     */
    @Test
    void testOfNonDeletingRecordsWithStatusSending() {
        LocalDateTime addTime = LocalDateTime.now().minusDays(LIMIT_AGE_DAYS);
        fillBallanceInfoQueueWith(BalanceInfoQueueObjType.cid, addTime.plusMinutes(1),
                BalanceInfoQueueSendStatus.Sending);
        fillBallanceInfoQueueWith(BalanceInfoQueueObjType.cid, addTime.minusDays(2),
                BalanceInfoQueueSendStatus.Sending);
        fillBallanceInfoQueueWith(BalanceInfoQueueObjType.uid, addTime.minusMonths(2),
                BalanceInfoQueueSendStatus.Sending);

        executeJob();

        assertThat("проверяем что записи не удалились",
                getBallanceInfoQueueIds().size(),
                equalTo(3));
    }


    /**
     * Проверка запуска джобы раз в сутки.
     * Выставляем текущую дату в свойство CLEAR_BALANCE_INFO_QUEUE_SHARD_x; Ожидаем, что джоба не выполнится
     */
    @Test
    void getLastRunPropertyTest_AlreadyRun() {
        ppcPropertiesSupport.get(CLEAR_BALANCE_INFO_QUEUE_PROPERTY_NAME).set(LocalDateTime.now().toLocalDate());

        //создаем записи, которые заведомо подходят под условие удаления
        LocalDateTime addTime = LocalDateTime.now().minusDays(LIMIT_AGE_DAYS).minusDays(10);
        fillBallanceInfoQueueWith(BalanceInfoQueueObjType.cid, addTime, BalanceInfoQueueSendStatus.Error);
        fillBallanceInfoQueueWith(BalanceInfoQueueObjType.uid, addTime, BalanceInfoQueueSendStatus.Send);
        fillBallanceInfoQueueWith(BalanceInfoQueueObjType.cid, addTime, BalanceInfoQueueSendStatus.Wait);

        executeJob();

        //проверяем что ничего не отправили в графит
        verify(solomonPushClient, never()).sendMetrics(any(MetricRegistry.class));

        assertThat("проверяем что записи не удалились (джоба не выполнилась)",
                getBallanceInfoQueueIds().size(),
                equalTo(3));
    }

    /**
     * Проверка, что при отсутствии проперти джоба выполнится и по завершению создаст новую с текущей датой.
     */
    @Test
    void getLastRunPropertyTest_NoProperty() {

        LocalDateTime addTime = LocalDateTime.now().minusDays(LIMIT_AGE_DAYS + 1);
        fillBallanceInfoQueueWith(BalanceInfoQueueObjType.cid, addTime, BalanceInfoQueueSendStatus.Error);
        fillBallanceInfoQueueWith(BalanceInfoQueueObjType.cid, addTime, BalanceInfoQueueSendStatus.Send);

        executeJob();

        assumeThat("проверяем что кол-во записей равно 0",
                getBallanceInfoQueueIds().size(),
                equalTo(0));

        assertThat("проверяем, что созданное свойство CLEAR_BALANCE_INFO_QUEUE_SHARD_x содержит текущую дату",
                ppcPropertiesSupport.get(CLEAR_BALANCE_INFO_QUEUE_PROPERTY_NAME).get(),
                equalTo(LocalDate.now()));

    }

    @AfterEach
    void cleanRecords() {
        ppcPropertiesSupport.get(CLEAR_BALANCE_INFO_QUEUE_PROPERTY_NAME).remove();

        balanceInfoQueueRepository.deleteByIds(SHARD, balanceInfoQueueIds);
        balanceInfoQueueIds.clear();
    }


}
