package ru.yandex.market.pers.tms.timer.paid;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.moderation.GradeModeratorModificationProxy;
import ru.yandex.market.pers.grade.core.service.GradeChangesService;
import ru.yandex.market.pers.pay.model.PersPayEntity;
import ru.yandex.market.pers.pay.model.PersPayEntityState;
import ru.yandex.market.pers.pay.model.PersPayEntityType;
import ru.yandex.market.pers.pay.model.PersPayUser;
import ru.yandex.market.pers.pay.model.PersPayUserType;
import ru.yandex.market.pers.pay.model.dto.PaymentEntityStateEventDto;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.util.FormatUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 19.03.2021
 */
public class PaidContentLbDumperExecutorTest extends MockedPersTmsTest {
    private static final long MODEL_ID = 31334;
    private static final long USER_ID = 23434;

    @Autowired
    private PaidContentLbDumperExecutor executor;

    @Autowired
    private GradeCreator gradeCreator;

    @Autowired
    private GradeChangesService gradeChangesService;

    @Autowired
    @Qualifier("lbkxClientFactory")
    private LogbrokerClientFactory logbrokerClientFactory;

    @Autowired
    private GradeModeratorModificationProxy gradeModeratorModificationProxy;

    @Test
    public void testEventsSent() throws Exception {
        long[] gradeIds = {
            gradeCreator.createModelGrade(MODEL_ID, USER_ID + 1, ModState.UNMODERATED), // updated ready later
            gradeCreator.createModelGrade(MODEL_ID, USER_ID + 2, ModState.UNMODERATED), // updated approved later
            createGradeNoText(MODEL_ID, USER_ID + 3, ModState.READY), // updated later, but not registered
            gradeCreator.createModelGrade(MODEL_ID, USER_ID, ModState.APPROVED),
            gradeCreator.createModelGrade(MODEL_ID + 1, USER_ID, ModState.UNMODERATED),
            gradeCreator.createModelGrade(MODEL_ID + 2, USER_ID, ModState.READY),
            gradeCreator.createModelGrade(MODEL_ID + 3, USER_ID, ModState.REJECTED),
            gradeCreator.createModelGrade(MODEL_ID + 4, USER_ID, ModState.AUTOMATICALLY_REJECTED),
            createGradeNoText(MODEL_ID + 5, USER_ID, ModState.READY),
            createGradeNoText(MODEL_ID + 6, USER_ID, ModState.AUTOMATICALLY_REJECTED),
            createGradeNoText(MODEL_ID + 7, USER_ID, ModState.SPAMMER),
            createGradeNoText(MODEL_ID + 8, USER_ID, ModState.READY), // removed
        };

        executor.updateLastId(gradeIds[2]);

        updateModState(gradeIds[0], ModState.READY);
        updateModState(gradeIds[1], ModState.APPROVED);
        updateModState(gradeIds[2], ModState.REJECTED);

        // update only first two
        gradeChangesService.markChanged(List.of(gradeIds[0], gradeIds[1]));

        // move date for first grade to ensure only one record would be removed
        pgJdbcTemplate.update(
            "update PAID_GRADE_EVENTS set cr_time = cr_time - interval '1' day where grade_id = ?", gradeIds[0]);

        // remove grade
        pgJdbcTemplate.update("update grade set state = 2 where id = ?", gradeIds[11]);

        List<PaymentEntityStateEventDto> expectedList = List.of(
            buildEvent(MODEL_ID, USER_ID + 1, gradeIds[0], PersPayEntityState.READY),
            buildEvent(MODEL_ID, USER_ID + 2, gradeIds[1], PersPayEntityState.APPROVED),

            buildEvent(MODEL_ID, USER_ID, gradeIds[3], PersPayEntityState.APPROVED),
            buildEvent(MODEL_ID + 1, USER_ID, gradeIds[4], PersPayEntityState.READY),
            buildEvent(MODEL_ID + 2, USER_ID, gradeIds[5], PersPayEntityState.READY),
            buildEvent(MODEL_ID + 3, USER_ID, gradeIds[6], PersPayEntityState.REJECTED),
            buildEvent(MODEL_ID + 4, USER_ID, gradeIds[7], PersPayEntityState.REJECTED),
            buildEvent(MODEL_ID + 5, USER_ID, gradeIds[8], PersPayEntityState.CREATED),
            buildEvent(MODEL_ID + 6, USER_ID, gradeIds[9], PersPayEntityState.CREATED),
            buildEvent(MODEL_ID + 7, USER_ID, gradeIds[10], PersPayEntityState.REJECTED),
            buildEvent(MODEL_ID + 8, USER_ID, gradeIds[11], PersPayEntityState.REJECTED)
        );

        runAndExpect(expectedList);

        // check lastId and events queue
        assertEquals(gradeIds[gradeIds.length - 1], executor.getLastId());

        assertEquals(List.of(), getPaidGradesEvent(false));
        assertEquals(List.of(gradeIds[0], gradeIds[1]), getPaidGradesEvent(true));

        // add some events to queue and rerun
        gradeChangesService.markChanged(List.of(gradeIds[0], gradeIds[1], gradeIds[9], gradeIds[10], gradeIds[11]));

        expectedList = List.of(
            buildEvent(MODEL_ID, USER_ID + 1, gradeIds[0], PersPayEntityState.READY),
            buildEvent(MODEL_ID, USER_ID + 2, gradeIds[1], PersPayEntityState.APPROVED),
            buildEvent(MODEL_ID + 6, USER_ID, gradeIds[9], PersPayEntityState.CREATED),
            buildEvent(MODEL_ID + 7, USER_ID, gradeIds[10], PersPayEntityState.REJECTED),
            buildEvent(MODEL_ID + 8, USER_ID, gradeIds[11], PersPayEntityState.REJECTED)
        );

        runAndExpect(expectedList);

        assertEquals(List.of(), getPaidGradesEvent(false));
        assertEquals(
            List.of(gradeIds[0], gradeIds[0], gradeIds[1], gradeIds[1], gradeIds[9], gradeIds[10], gradeIds[11]),
            getPaidGradesEvent(true)
        );
    }

    @Test
    public void testEventsProcessedByTime() throws Exception {
        // some approved grades to notify about
        long[] gradeIds = {
            gradeCreator.createModelGrade(MODEL_ID, USER_ID, ModState.APPROVED),
            gradeCreator.createModelGrade(MODEL_ID, USER_ID + 1, ModState.APPROVED),
        };

        // ignore lastId in search
        executor.updateLastId(gradeIds[gradeIds.length - 1]);
        executor.setBatchSize(2);

        // save signals
        gradeChangesService.markChanged(List.of(gradeIds[0], gradeIds[1]));

        // make them old
        pgJdbcTemplate.update(
            "update PAID_GRADE_EVENTS set cr_time = cr_time - interval '1' minute");

        // insert more fresh signals
        gradeChangesService.markChanged(List.of(gradeIds[0]));

        List<PaymentEntityStateEventDto> expectedList = List.of(
            buildEvent(MODEL_ID, USER_ID, gradeIds[0], PersPayEntityState.APPROVED),
            buildEvent(MODEL_ID, USER_ID + 1, gradeIds[1], PersPayEntityState.APPROVED)
        );

        runAndExpect(expectedList);

        // check lastId and events queue
        assertEquals(gradeIds[gradeIds.length - 1], executor.getLastId());

        assertEquals(List.of(gradeIds[0]), getPaidGradesEvent(false));
        assertEquals(List.of(gradeIds[0], gradeIds[1]), getPaidGradesEvent(true));

        // rerun, send new signals
        expectedList = List.of(
            buildEvent(MODEL_ID, USER_ID, gradeIds[0], PersPayEntityState.APPROVED)
        );

        runAndExpect(expectedList);

        assertEquals(List.of(), getPaidGradesEvent(false));
        assertEquals(List.of(gradeIds[0], gradeIds[0], gradeIds[1]), getPaidGradesEvent(true));
    }


    @Test
    public void testEventsGenerated() throws Exception {
        long[] gradeIds = {
            gradeCreator.createModelGrade(MODEL_ID, USER_ID + 1, ModState.UNMODERATED),
            gradeCreator.createModelGrade(MODEL_ID, USER_ID + 2, ModState.UNMODERATED),
            gradeCreator.createModelGrade(MODEL_ID, USER_ID + 3, ModState.UNMODERATED),
        };

        gradeModeratorModificationProxy.moderateGradeReplies(List.of(gradeIds[0]), List.of(), 1L,
            ModState.APPROVED);
        gradeModeratorModificationProxy.moderateGradeReplies(List.of(gradeIds[1]), List.of(), 1L,
            ModState.REJECTED);
        gradeModeratorModificationProxy.moderateGradeReplies(List.of(gradeIds[2]), List.of(), 1L,
            ModState.AUTOMATICALLY_REJECTED);

        Set<Long> gradeIdsInQueue = getPaidGradesEvent(false).stream().collect(Collectors.toSet());

        List<Long> gradeIdsList = LongStream.of(gradeIds).boxed().collect(Collectors.toList());
        assertTrue(gradeIdsInQueue.containsAll(gradeIdsList));
    }

    private void runAndExpect(List<PaymentEntityStateEventDto> expectedList) throws Exception {
        initMocks();

        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);

        mockLogbrokerClientFactory(messageCaptor);

        executor.runTmsJob();

        // check messages and changes in DB

        List<PaymentEntityStateEventDto> events = messageCaptor.getAllValues().stream()
            .map(String::new)
            .map(x -> FormatUtils.fromJson(x, PaymentEntityStateEventDto.class))
            .collect(Collectors.toList());

        Map<String, PaymentEntityStateEventDto> expectedMap = expectedList.stream()
            .collect(Collectors.toMap(PaymentEntityStateEventDto::getContentId, x -> x));

        for (PaymentEntityStateEventDto event : events) {
            PaymentEntityStateEventDto expected = expectedMap.get(event.getContentId());
            assertEquals(expected.getUser(), event.getUser());
            assertEquals(expected.getEntity(), event.getEntity());
            assertEquals(expected.getUser().toShortString() + "-" + expected.getEntity().toShortString(),
                expected.getState(), event.getState());
        }
    }

    private void mockLogbrokerClientFactory(ArgumentCaptor<byte[]> messageCaptor) throws InterruptedException {
        AsyncProducer asyncProducer = mock(AsyncProducer.class);
        when(logbrokerClientFactory.asyncProducer(any())).thenReturn(asyncProducer);
        when(asyncProducer.init())
            .thenReturn(CompletableFuture.completedFuture(new ProducerInitResponse(1, "", 1, "")));
        when(asyncProducer.write(messageCaptor.capture()))
            .thenReturn(CompletableFuture.completedFuture(new ProducerWriteResponse(1, 1, true)));
    }

    private long createGradeNoText(long modelId, long userId, ModState modState) {
        ModelGrade grade = GradeCreator.constructModelGradeNoText(modelId, userId, modState);
        return gradeCreator.createGrade(grade);
    }

    private void updateModState(long gradeId, ModState modState) {
        pgJdbcTemplate.update("update grade set MOD_STATE = ? where id = ?", modState.value(), gradeId);
    }

    private PaymentEntityStateEventDto buildEvent(long modelId, long userId, long gradeId, PersPayEntityState state) {
        return new PaymentEntityStateEventDto(
            new PersPayUser(PersPayUserType.UID, userId),
            new PersPayEntity(PersPayEntityType.MODEL_GRADE, modelId),
            state,
            String.valueOf(gradeId),
            0
        );
    }

    private List<Long> getPaidGradesEvent(boolean isProcessed) {
        return pgJdbcTemplate.queryForList(
            "select grade_id from PAID_GRADE_EVENTS where processed = ? order by GRADE_ID",
            Long.class, isProcessed ? 1 : 0);
    }
}
