package ru.yandex.market.pers.tms.timer.paid;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.pay.client.PersPayClient;
import ru.yandex.market.pers.pay.model.PersPayEntity;
import ru.yandex.market.pers.pay.model.PersPayEntityType;
import ru.yandex.market.pers.pay.model.PersPayUser;
import ru.yandex.market.pers.pay.model.PersPayUserType;
import ru.yandex.market.pers.pay.model.PersPayer;
import ru.yandex.market.pers.pay.model.PersPayerType;
import ru.yandex.market.pers.pay.model.dto.PaymentOfferDto;
import ru.yandex.market.pers.tms.MockedPersTmsTest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static ru.yandex.market.pers.tms.timer.paid.PaidGradesCheckerExecutor.API_BATCH_SIZE;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 16.03.2021
 */
public class PaidGradesCheckerExecutorTest extends MockedPersTmsTest {
    private static final long MODEL_ID = 234234;
    private static final long USER_ID = 562462;

    @Autowired
    private PaidGradesCheckerExecutor executor;

    @Autowired
    private PersPayClient persPayClient;

    @Autowired
    private GradeCreator gradeCreator;

    @Test
    public void testPaidMarkingWorks() throws Exception {
        long gradeId = gradeCreator.createModelGrade(MODEL_ID, USER_ID);
        long gradeId2 = gradeCreator.createModelGrade(MODEL_ID, USER_ID + 1);
        long gradeId3 = gradeCreator.createModelGrade(MODEL_ID + 1, USER_ID);

        executor.updateLastId(gradeId);

        pgJdbcTemplate.update("update grade set cr_time = cr_time - interval '1' day where 1=1");

        Mockito.when(persPayClient.findPayments(anyCollection())).thenReturn(List.of(
            new PaymentOfferDto(
                new PersPayUser(PersPayUserType.UID, USER_ID),
                new PersPayEntity(PersPayEntityType.MODEL_GRADE, MODEL_ID),
                new PersPayer(PersPayerType.MARKET, "EXP"), 10),
            new PaymentOfferDto( // not in the list of requested
                new PersPayUser(PersPayUserType.UID, USER_ID + 2),
                new PersPayEntity(PersPayEntityType.MODEL_GRADE, MODEL_ID),
                new PersPayer(PersPayerType.MARKET, "EXP"), 10),
            new PaymentOfferDto(
                new PersPayUser(PersPayUserType.UID, USER_ID),
                new PersPayEntity(PersPayEntityType.MODEL_GRADE, MODEL_ID + 1),
                new PersPayer(PersPayerType.MARKET, "EXP"), 10)
        ));

        executor.runTmsJob();

        List<Long> paidGrades = pgJdbcTemplate
            .queryForList("select grade_id from paid_grade order by grade_id", Long.class);

        Mockito.verify(persPayClient, Mockito.times(1)).findPayments(anyCollection());

        assertEquals(List.of(gradeId, gradeId3), paidGrades);
        assertEquals(gradeId3 + 1, executor.getLastId());
    }

    @Test
    public void testPaidSkipsPaid() throws Exception {
        long gradeId = gradeCreator.createModelGrade(MODEL_ID, USER_ID);

        executor.updateLastId(gradeId);

        pgJdbcTemplate.update("update grade set cr_time = cr_time - interval '1' day where 1=1");

        Mockito.when(persPayClient.findPayments(anyCollection())).thenReturn(List.of(
            new PaymentOfferDto(
                new PersPayUser(PersPayUserType.UID, USER_ID),
                new PersPayEntity(PersPayEntityType.MODEL_GRADE, MODEL_ID),
                new PersPayer(PersPayerType.MARKET, "EXP"), 10)
        ));

        executor.runTmsJob();

        List<Long> paidGrades = pgJdbcTemplate
            .queryForList("select grade_id from paid_grade order by grade_id", Long.class);

        Mockito.verify(persPayClient, Mockito.times(1)).findPayments(anyCollection());

        assertEquals(List.of(gradeId), paidGrades);
        assertEquals(gradeId + 1, executor.getLastId());

        // try again
        executor.updateLastId(gradeId - 1);
        executor.runTmsJob();

        Mockito.verify(persPayClient, Mockito.times(1)).findPayments(anyCollection());

        assertEquals(gradeId - 1, executor.getLastId());
    }

    @Test
    public void testFoundNoPaid() throws Exception {
        long gradeId = gradeCreator.createModelGrade(MODEL_ID, USER_ID);

        executor.updateLastId(gradeId);

        pgJdbcTemplate.update("update grade set cr_time = cr_time - interval '1' day where 1=1");

        Mockito.when(persPayClient.findPayments(anyCollection())).thenReturn(List.of(
            new PaymentOfferDto(
                new PersPayUser(PersPayUserType.UID, USER_ID),
                new PersPayEntity(PersPayEntityType.MODEL_GRADE, MODEL_ID + 1),
                new PersPayer(PersPayerType.MARKET, "EXP"), 10)
        ));

        executor.runTmsJob();

        List<Long> paidGrades = pgJdbcTemplate
            .queryForList("select grade_id from paid_grade order by grade_id", Long.class);

        Mockito.verify(persPayClient, Mockito.times(1)).findPayments(anyCollection());

        assertEquals(List.of(), paidGrades);
        assertEquals(gradeId + 1, executor.getLastId());
    }

    @Test
    public void testFindPaymentBatcher() throws Exception {
        int testSize = 2 * API_BATCH_SIZE + 1;
        List<String> source = IntStream.range(0, testSize).mapToObj(Integer::toString).collect(Collectors.toList());

        MutableInt shift = new MutableInt(0);

        Mockito.when(persPayClient.findPayments(anyCollection())).then(req -> {
            Collection<String> data = req.getArgument(0);
            Integer startIdx = shift.getValue();
            int endIdx = Math.min(startIdx + API_BATCH_SIZE, source.size());
            assertEquals(source.subList(startIdx, endIdx), data);
            shift.add(data.size());

            return data.stream()
                .map(item -> new PaymentOfferDto(
                    new PersPayUser(PersPayUserType.UID, USER_ID),
                    new PersPayEntity(PersPayEntityType.MODEL_GRADE, item),
                    new PersPayer(PersPayerType.MARKET, "EXP"), 10))
                .collect(Collectors.toList());
        });

        List<PaymentOfferDto> result = executor.findPayments(source);
        Mockito.verify(persPayClient, Mockito.times(3)).findPayments(anyCollection());

        List<String> entities = result.stream().map(PaymentOfferDto::getEntityId).collect(Collectors.toList());
        assertEquals(source, entities);
    }


}
