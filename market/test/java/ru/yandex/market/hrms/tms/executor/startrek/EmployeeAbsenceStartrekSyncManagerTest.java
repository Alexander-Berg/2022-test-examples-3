package ru.yandex.market.hrms.tms.executor.startrek;

import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.service.startrek.IssueAnswer;
import ru.yandex.market.hrms.core.service.startrek.IssueGetAnswer;
import ru.yandex.market.hrms.core.service.startrek.IssueUpdateAnswer;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.hrms.tms.manager.EmployeeAbsenceStartrekSyncManager;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.IssueRef;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.Transition;

@DbUnitDataSet(before = "EmployeeAbsenceStartrekSyncManagerTest.before.csv")
public class EmployeeAbsenceStartrekSyncManagerTest extends AbstractTmsTest {
    @Autowired
    private EmployeeAbsenceStartrekSyncManager employeeAbsenceStartrekSyncManager;

    @Autowired
    private Session trackerSession;
    @Captor
    private ArgumentCaptor<IssueCreate> issueCreateCaptor;
    @Captor
    private ArgumentCaptor<IssueUpdate> issueUpdateCaptor;

    private static final String STARTREK_QUEUE = "TESTHRMSVIRV";

    @BeforeEach
    public void setUp() {
        Mockito.when(trackerSession.issues().create(issueCreateCaptor.capture()))
                .thenAnswer(new IssueAnswer(trackerSession, STARTREK_QUEUE));
        Mockito.when(trackerSession.issues().create(issueCreateCaptor.capture(), Mockito.any(ListF.class)))
                .thenAnswer(new IssueAnswer(trackerSession, STARTREK_QUEUE));
        Mockito.when(trackerSession.issues().update(Mockito.anyString(), issueUpdateCaptor.capture()))
                .thenAnswer(new IssueUpdateAnswer(trackerSession));
        Mockito.when(trackerSession.issues().update(Mockito.anyString(), issueUpdateCaptor.capture(), Mockito.any(ListF.class)))
                .thenAnswer(new IssueUpdateAnswer(trackerSession));

        Mockito.when(trackerSession.issues().get(Mockito.anyString()))
                .thenAnswer(new IssueGetAnswer(trackerSession));
        Mockito.when(trackerSession.issues().get(Mockito.anyString(), Mockito.any(ListF.class)))
                .thenAnswer(new IssueGetAnswer(trackerSession));
    }

    @DbUnitDataSet(
            before = "EmployeeAbsenceStartrekSyncManagerTest.createTask.before.csv",
            after = "EmployeeAbsenceStartrekSyncManagerTest.createTask.after.csv"
    )
    @Test
    public void shouldCreateTicketsInStartrek() {
        employeeAbsenceStartrekSyncManager.createTasksInStartrek();
        Mockito.verify(trackerSession.issues(), Mockito.times(2))
                .create(Mockito.any(IssueCreate.class));

        List<IssueCreate> allValues = issueCreateCaptor.getAllValues();

        IssueCreate firstIssue = allValues.get(0);

        MatcherAssert.assertThat(firstIssue.getValues(), Matchers.allOf(
                Matchers.hasEntry("queue", "TESTHRMSVIRV"),
                Matchers.hasEntry("summary", "Проставить отсутствие НН Тимур (timursha)"),
                Matchers.hasEntry("components", (Object) new long[] {12345, 12346}),
                Matchers.hasEntry("formOfWork", "НН"),
                Matchers.hasEntry("description", """
                                Тип отсутствия:
                                Неявка на работу / сотрудник пропал (НН)
                                Сотрудник:
                                Тимур (timursha)
                                Дата начала:
                                2021-02-01
                                Дата окончания:
                                2021-02-02
                                Причина отсутствия:

                                Сотрудник пропал, связаться невозможно:
                                Нет
                                Комментарий:

                                """
                )
        ));
    }

    @DbUnitDataSet(
            before = "EmployeeAbsenceStartrekSyncManagerTest.updateTask.before.csv",
            after = "EmployeeAbsenceStartrekSyncManagerTest.updateTask.after.csv"
    )
    @Test
    public void shouldUpdateTicketsInStartrek() {
        employeeAbsenceStartrekSyncManager.updateTasksInStartrek();
        Mockito.verify(trackerSession.issues(), Mockito.times(2))
                .update(Mockito.anyString(), Mockito.any(IssueUpdate.class));

        List<IssueUpdate> allValues = issueUpdateCaptor.getAllValues();

        IssueUpdate firstIssue = allValues.get(0);

        MatcherAssert.assertThat(firstIssue.getValues(), Matchers.allOf(
                Matchers.hasEntry(Matchers.is("start"), Matchers.hasProperty("set", Matchers.is(Option.of("2021-02-01")))),
                Matchers.hasEntry(Matchers.is("end"), Matchers.hasProperty("set", Matchers.is(Option.of("2021-02-02")))),
                Matchers.hasEntry(Matchers.is("description"),
                        Matchers.hasProperty("set", Matchers.is(Option.of("""
                                Тип отсутствия:
                                Неявка на работу / сотрудник пропал (НН)
                                Сотрудник:
                                Тимур (timursha)
                                Дата начала:
                                2021-02-01
                                Дата окончания:
                                2021-02-02
                                Причина отсутствия:

                                Сотрудник пропал, связаться невозможно:
                                Нет
                                Комментарий:

                                """
                ))))
        ));
    }

    @DbUnitDataSet(
            before = "EmployeeAbsenceStartrekSyncManagerTest.deleteTask.before.csv",
            after = "EmployeeAbsenceStartrekSyncManagerTest.deleteTask.after.csv"
    )
    @Test
    public void shouldCloseTicketsInStartrek() {
        employeeAbsenceStartrekSyncManager.deleteTasksInStartrek();

        ArgumentCaptor<IssueRef> issueRefs = ArgumentCaptor.forClass(IssueRef.class);
        ArgumentCaptor<Transition> transitions = ArgumentCaptor.forClass(Transition.class);
        ArgumentCaptor<IssueUpdate> issueUpdates = ArgumentCaptor.forClass(IssueUpdate.class);

        Mockito.verify(trackerSession.transitions(), Mockito.times(2))
                .execute(issueRefs.capture(), transitions.capture(), issueUpdates.capture());

        List<IssueUpdate> allValues = issueUpdates.getAllValues();

        IssueUpdate firstIssue = allValues.get(0);

        MatcherAssert.assertThat(firstIssue.getValues(), Matchers.allOf(
        ));
    }

}
