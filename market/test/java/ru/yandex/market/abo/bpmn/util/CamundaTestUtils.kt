@file:JvmName("CamundaTestUtils")

package ru.yandex.market.abo.bpmn.util

import org.camunda.bpm.engine.ProcessEngine
import org.camunda.bpm.engine.runtime.Incident
import org.camunda.bpm.engine.task.Task
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * Утильный класс, который позволяет тестировать процессы с асинхронным выполнением кубиков.
 * Таймаут указан по умолчанию, может быть переопределен при необходимости
 */
private const val DEFAULT_TIMEOUT = 20000L

/**
 * Метод дожидается завершения процесса с указанным таймаутом.
 * Есть проверки на созданные джобы и на завершенность процесса. Возможно, придется разделить на 2 метода.
 */
@JvmOverloads
@Throws(InterruptedException::class)
fun waitUntilNoActiveJobs(
    processEngine: ProcessEngine,
    processInstanceId: String,
    wait: Long = DEFAULT_TIMEOUT
): Boolean {
    val timeout = System.currentTimeMillis() + wait
    while (System.currentTimeMillis() < timeout) {
        val isFinished = processEngine.historyService
            .createHistoricProcessInstanceQuery()
            .active()
            .processInstanceId(processInstanceId)
            .count() == 0L
        if (isFinished) {
            return true
        }

        //Если был создан хотя бы один инцидент => выполнение задачи прекратилось.
        if (processEngine.runtimeService
                .createIncidentQuery()
                .processInstanceId(processInstanceId)
                .count() > 0
        ) {
            return true
        }
        val processInstanceIds = getProcessInstanceIds(processEngine, processInstanceId)

        //Если создана пользовательская задача, то тоже завершваем
        if (getListOfUserTasks(processEngine, processInstanceIds).isNotEmpty()) {
            return true
        }
        runExpectingTimers(processEngine, processInstanceIds)
        Thread.sleep(500)
    }
    return false
}

private fun getProcessInstanceIds(
    processEngine: ProcessEngine,
    processInstanceId: String
): Set<String> {
    val result = mutableSetOf<String>()
    result.add(processInstanceId)
    getProcessInstanceIds(result, processEngine, processInstanceId)
    return result
}

private fun getProcessInstanceIds(
    result: MutableSet<String>,
    processEngine: ProcessEngine,
    processInstanceId: String
) {
    val newIds = processEngine.runtimeService
        .createProcessInstanceQuery()
        .superProcessInstanceId(processInstanceId)
        .list()
        .map { it.processInstanceId }
        .filter { e: String -> !result.contains(e) }
        .toSet()
    result.addAll(newIds)
    newIds.forEach { getProcessInstanceIds(result, processEngine, it) }
}

/**
 * Запустить таймеры, которые ожидают
 */
fun runExpectingTimers(
    processEngine: ProcessEngine,
    processInstanceIds: Set<String>
) {
    val jobs = processEngine.managementService
        .createJobQuery()
        .processInstanceIds(processInstanceIds)
        .timers()
        .list()
    jobs.forEach { processEngine.managementService.executeJob(it.id) }
}

/**
 * Получить список инцидентов инстанса
 */
fun getListOfIncidents(
    processEngine: ProcessEngine,
    processInstanceId: String
): List<Incident> {
    return processEngine.runtimeService
        .createIncidentQuery()
        .processInstanceId(processInstanceId)
        .list()
}

/**
 * Получить список инцидентов инстанса
 */
fun getListOfUserTasks(
    processEngine: ProcessEngine,
    processInstanceIds: Set<String>
): List<Task> {
    val result: MutableList<Task> = ArrayList()
    processInstanceIds.forEach {
        result.addAll(getListOfUserTasks(processEngine, it))
    }
    return result
}

/**
 * Получить список инцидентов инстанса
 */
fun getListOfUserTasks(
    processEngine: ProcessEngine,
    processInstanceId: String
): List<Task> {
    return processEngine.taskService
        .createTaskQuery()
        .processInstanceIdIn(processInstanceId)
        .list()
}

/**
 * Получить список инцидентов инстанса
 */
fun getListOfUserTasksByRoot(
    processEngine: ProcessEngine,
    processInstanceId: String
): List<Task> {
    val processInstanceIds = getProcessInstanceIds(processEngine, processInstanceId)
    return getListOfUserTasks(processEngine, processInstanceIds)
}

/**
 * Проверить, какие инциденты есть у процесса.
 */
fun checkIncidents(
    processEngine: ProcessEngine,
    processInstanceId: String,
    vararg activities: String
) {
    val actualIncidents = getListOfIncidents(processEngine, processInstanceId)
    assertEquals(activities.size, actualIncidents.size)
    for (i in activities.indices) {
        val incident = actualIncidents[i]
        assertEquals(activities[i], incident.activityId)
    }
}
