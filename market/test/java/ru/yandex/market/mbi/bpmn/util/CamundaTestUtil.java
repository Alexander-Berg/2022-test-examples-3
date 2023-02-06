package ru.yandex.market.mbi.bpmn.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.Incident;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Assertions;

import static ru.yandex.market.mbi.bpmn.FunctionalTest.TEST_BUSINESS_KEY;

/**
 * Утильный класс, который позволяет тестировать процессы с асинхронным выполнением кубиков.
 * Таймаут указан по умолчанию, можт быть переопределен при необходимости
 */
@SuppressWarnings("HideUtilityClassConstructor")
public class CamundaTestUtil {

    public static final long DEFAULT_TIMEOUT = 20000L;

    public static boolean waitUntilNoActiveJobs(
            ProcessEngine processEngine,
            String processInstanceId
    ) throws InterruptedException {
        return waitUntilNoActiveJobs(processEngine, processInstanceId, DEFAULT_TIMEOUT);
    }

    /**
     * Метод дожидается завершения процесса с указанным таймаутом.
     * Есть проверки на созданные джобы и на завершенность процесса. Возможно, придется разделить на 2 метода.
     */
    public static boolean waitUntilNoActiveJobs(
            ProcessEngine processEngine,
            String processInstanceId,
            long wait
    ) throws InterruptedException {
        long timeout = System.currentTimeMillis() + wait;
        while (System.currentTimeMillis() < timeout) {
            boolean isFinished = processEngine.getHistoryService()
                    .createHistoricProcessInstanceQuery()
                    .active()
                    .processInstanceId(processInstanceId)
                    .count() == 0;
            if (isFinished) {
                return true;
            }

            //Если был создан хотя бы один инцидент => выполнение задачи прекратилось.
            if (processEngine.getRuntimeService()
                    .createIncidentQuery()
                    .processInstanceId(processInstanceId)
                    .count() > 0) {
                return true;
            }

            Set<String> processInstanceIds = getProcessInstanceIds(processEngine, processInstanceId);

            //Если создана пользовательская задача, то тоже завершваем
            if (!getListOfUserTasks(processEngine, processInstanceIds).isEmpty()) {
                return true;
            }


            runExpectingTimers(processEngine, processInstanceIds);
            Thread.sleep(500);
        }
        return false;
    }

    private static Set<String> getProcessInstanceIds(ProcessEngine processEngine,
                                                     String processInstanceId) {
        Set<String> result = new HashSet<>();
        result.add(processInstanceId);
        getProcessInstanceIds(result, processEngine, processInstanceId);
        return result;
    }

    private static void getProcessInstanceIds(Set<String> result,
                                              ProcessEngine processEngine,
                                              String processInstanceId) {
        Set<String> newIds = processEngine.getRuntimeService()
                .createProcessInstanceQuery()
                .superProcessInstanceId(processInstanceId)
                .list().stream()
                .map(Execution::getProcessInstanceId)
                .filter(e -> !result.contains(e))
                .collect(Collectors.toSet());

        result.addAll(newIds);
        newIds.forEach(id -> getProcessInstanceIds(result, processEngine, id));
    }

    /**
     * Запустить таймеры, которые ожидают
     */
    public static void runExpectingTimers(
            ProcessEngine processEngine,
            Set<String> processInstanceIds
    ) {
        final List<Job> jobs = processEngine.getManagementService()
                .createJobQuery()
                .processInstanceIds(processInstanceIds)
                .timers()
                .list();
        jobs.forEach(job -> processEngine.getManagementService().executeJob(job.getId()));
    }

    /**
     * Получить список инцидентов инстанса
     */
    public static List<Incident> getListOfIncidents(ProcessEngine processEngine, String processInstanceId) {
        return processEngine.getRuntimeService()
                .createIncidentQuery()
                .processInstanceId(processInstanceId)
                .list();
    }


    /**
     * Получить список инцидентов инстанса
     */
    public static List<Task> getListOfUserTasks(
            ProcessEngine processEngine,
            Set<String> processInstanceIds
    ) {
        List<Task> result = new ArrayList<>();
        processInstanceIds.forEach(processInstanceId ->
                result.addAll(getListOfUserTasks(processEngine, processInstanceId)));
        return result;
    }

    /**
     * Получить список инцидентов инстанса
     */
    public static List<Task> getListOfUserTasks(
            ProcessEngine processEngine,
            String processInstanceId
    ) {
        return processEngine.getTaskService()
                .createTaskQuery()
                .processInstanceIdIn(processInstanceId)
                .list();
    }
    /**
     * Получить список инцидентов инстанса
     */
    public static List<Task> getListOfUserTasksByRoot(
            ProcessEngine processEngine,
            String processInstanceId
    ) {
        Set<String> processInstanceIds = getProcessInstanceIds(processEngine, processInstanceId);
        return getListOfUserTasks(processEngine, processInstanceIds);
    }

    /**
     * Запустить процесс и дождаться его завершения.
     */
    public static ProcessInstance invoke(ProcessEngine processEngine,
                                         String processType,
                                         Map<String, Object> variables) throws InterruptedException {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                processType,
                TEST_BUSINESS_KEY,
                variables
        );

        Assertions.assertNotNull(processInstance);
        String processInstanceId = processInstance.getProcessInstanceId();
        Assertions.assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(processEngine, processInstanceId));

        return processInstance;
    }

    /**
     * Проверить, какие инциденты есть у процесса.
     */
    public static void checkIncidents(ProcessEngine processEngine,
                                      ProcessInstance instance,
                                      String... activities) {
        String processInstanceId = instance.getProcessInstanceId();
        List<Incident> actualIncidents = CamundaTestUtil.getListOfIncidents(processEngine, processInstanceId);
        Assertions.assertEquals(activities.length, actualIncidents.size());

        for (int i = 0; i < activities.length; i++) {
            Incident incident = actualIncidents.get(i);
            Assertions.assertEquals(activities[i], incident.getActivityId());
        }
    }
}
