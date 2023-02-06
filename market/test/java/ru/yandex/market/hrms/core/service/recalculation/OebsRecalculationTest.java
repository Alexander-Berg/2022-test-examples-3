package ru.yandex.market.hrms.core.service.recalculation;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.employee.absence.ticket.VirvTicketStatusEnum;
import ru.yandex.market.hrms.core.domain.recalculation.OebsRecalculationService;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.Transition;

/**
 * 3 раза месяц в оебс происходит перерасчёт табеля
 * нужно после каждого перерасчёта удалять из Hermes неподтверждённые кадровые события
 * Удаление должно произойти ровно 1 раз на следующий день после перерасчёта
 * После этого удаление не должно происходить и в бд должна быть запись о том когда было произведено удаление
 */
@DbUnitDataSet(before = "OebsRecalculationTest.before.csv")
public class OebsRecalculationTest extends AbstractCoreTest {

    @Autowired
    private Session session;

    @Autowired
    private OebsRecalculationService oebsRecalculationService;

    @BeforeEach
    private void init() {
        IntStream.rangeClosed(196, 205).boxed().map(it -> "HRMS-" + it)
                .forEach(it -> {

                    Transition transition = Mockito.mock(Transition.class, Mockito.RETURNS_DEEP_STUBS);
                    Mockito.when(transition.getTo().getKey())
                            .thenReturn("closed");
                    var mock = Mockito.mock(Issue.class, Mockito.RETURNS_DEEP_STUBS);


                    Mockito.when(mock.getTransitions()).thenReturn(Cf.wrap(List.of(
                            transition
                    )));

                    Mockito.when(mock.executeTransition(Mockito.any(Transition.class), Mockito.any(IssueUpdate.class)))
                            .thenAnswer((inv) -> session.transitions().execute(
                                    mock,
                                    inv.<Transition>getArgument(0),
                                    inv.getArgument(1)
                            ));

                    Mockito.when(mock.getStatus().getKey()).thenReturn(VirvTicketStatusEnum.NEW.getStatusKey());
                    Mockito.when(mock.getStatus().getId()).thenReturn(1L);
                    Mockito.when(mock.getKey()).thenReturn(it);
                    Mockito.when(session.issues().get(it))
                            .thenReturn(mock);
                });
    }

    /**
     * дата расчёта аванса в оебс - 14-е
     * Hermes должен запуститься 15-го
     * Должны быть удалены неподтвержденные ННки и отпуска до 13-го включительно
     */
    @Test
    @DbUnitDataSet(
            before = "OebsRecalculationTest.prepayment.before.csv",
            after = "OebsRecalculationTest.prepayment.after.csv")
    public void deleteAbsencesAfterPrepaymentDate() {
        mockClock(LocalDate.of(2021, 12, 15));
        oebsRecalculationService.processCurrentMonth();
    }

    /**
     * дата расчёта корректировочного табеля в оебс - 22-е
     * Hermes должен запуститься 23-го
     * Должны быть удалены неподтвержденные ННки, подработки, отпуска из гэп и ббп за прошлый месяц
     */
    @Test
    @DbUnitDataSet(
            before = "OebsRecalculationTest.correction.before.csv",
            after = "OebsRecalculationTest.correction.after.csv")
    public void deleteAbsencesAfterCorrectionDate() {
        mockClock(LocalDate.of(2021, 12, 23));
        oebsRecalculationService.processCurrentMonth();
    }

    /**
     * дата расчёта основного табеля в оебс - 23-е
     * Hermes должен запуститься 24-го
     * Должны быть удалены неподтвержденные ННки и отпуска из гэп до 24-го включительно
     * подработки и ббп не трогаем
     */
    @Test
    @DbUnitDataSet(
            before = "OebsRecalculationTest.main.before.csv",
            after = "OebsRecalculationTest.main.after.csv")
    public void deleteAbsencesAfterMainCalendarDate() {
        mockClock(LocalDate.of(2021, 12, 24));
        oebsRecalculationService.processCurrentMonth();
    }
}
