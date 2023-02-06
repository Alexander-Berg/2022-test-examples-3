package ru.yandex.market.crm.triggers.test.helpers;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.impl.core.instance.CoreExecution;
import org.camunda.bpm.engine.impl.core.model.CoreModelElement;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.junit.Assert;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import ru.yandex.market.mcrm.db.Constants;
import ru.yandex.market.mcrm.utils.test.StatefulHelper;
import ru.yandex.misc.thread.ThreadUtils;

/**
 * Предназначен для отслеживания активности процессов в тестах
 */
@Component
public class BpmProcessSpy implements ExecutionListener, StatefulHelper {

    private final JdbcTemplate jdbcTemplate;

    private final Map<String, Map<String, String>> activityStates = new HashMap<>();
    private final Object lock = new Object();

    private static final long WAIT_DURATION = 10 * 1000; // 10 sec.
    private static final String PROC_INS = "PROCESS_INSTANCE";

    public BpmProcessSpy(@Named(Constants.DEFAULT_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void setUp() {

    }

    @Override
    public void tearDown() {
        activityStates.clear();
    }

    public Map<String, Map<String, String>> getCurrentActivityStates() {
        return new HashMap<>(activityStates);
    }

    public void waitProcessEnd(String pid) {
        waitEvent(pid, PROC_INS, EVENTNAME_END);
    }

    public void waitActivityStart(String pid, String id) {
        waitEvent(pid, id, EVENTNAME_START);
    }

    public void waitActivityEnd(String pid, String id) {
        waitEvent(pid, id, EVENTNAME_END);
    }

    public void waitProcessesEnd(Duration duration) {
        long waitTill = System.currentTimeMillis() + duration.toMillis();
        try {
            while (true) {
                synchronized (lock) {
                    if (allProcessEnded()) {
                        break;
                    }
                    long waitTime = waitTill - System.currentTimeMillis();
                    if (waitTime <= 0) {
                        String pids = activityStates.entrySet().stream()
                                .filter(e -> !EVENTNAME_END.equals(e.getValue().get(PROC_INS)))
                                .map(Map.Entry::getKey)
                                .collect(Collectors.joining(", "));
                        throw new AssertionError("Processes " + pids + " waiting in " + duration + " was " +
                                "expired");
                    }
                    lock.wait(waitTime);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void waitActivityStartDB(String pid, String activityId) {
        long deadline = System.currentTimeMillis() + WAIT_DURATION;
        while (System.currentTimeMillis() < deadline) {
            Boolean reached = jdbcTemplate.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM act_ru_execution WHERE proc_inst_id_ = ? AND act_id_ = ?)",
                    Boolean.class,
                    pid,
                    activityId
            );

            if (Boolean.TRUE.equals(reached)) {
                return;
            }

            ThreadUtils.sleep(500);
        }

        Assert.fail("Process '" + pid + "' has not reached activity '" + activityId + "'");
    }

    private void waitEvent(String pid, String id, String eventName) {
        long waitTill = System.currentTimeMillis() + WAIT_DURATION;
        try {
            while (true) {
                synchronized (lock) {
                    Map<String, String> states = activityStates.get(pid);
                    if (null != states && eventName.equals(states.get(id))) {
                        break;
                    }
                    long waitTime = waitTill - System.currentTimeMillis();
                    if (waitTime <= 0) {
                        throw new AssertionError("Waiting activity id=" + id + " in process instance " + pid + " was " +
                            "expired");
                    }
                    lock.wait(waitTime);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void notify(DelegateExecution execution) {
        FlowElement element = execution.getBpmnModelElementInstance();
        if (element == null) {
            return;
        }

        String activityId = element.getId();
        if (execution instanceof CoreExecution) {
            CoreModelElement eventSource = ((CoreExecution) execution).getEventSource();
            activityId = eventSource instanceof ProcessDefinition ? PROC_INS : eventSource.getId();
        }
        synchronized (lock) {
            activityStates.computeIfAbsent(execution.getProcessInstanceId(), k -> new HashMap<>())
                    .put(activityId, execution.getEventName());
            lock.notifyAll();
        }
    }

    private boolean allProcessEnded() {
        return activityStates.entrySet().stream()
            .allMatch(e -> EVENTNAME_END.equals(e.getValue().get(PROC_INS)));
    }
}
