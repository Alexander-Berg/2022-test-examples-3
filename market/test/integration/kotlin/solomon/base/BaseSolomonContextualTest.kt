package ru.yandex.market.logistics.calendaring.solomon.base

import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.dbqueue.DbqueueTaskType

abstract class BaseSolomonContextualTest(private val jdbcTemplate: JdbcTemplate): AbstractContextualTest() {

    protected fun insert(queue: DbqueueTaskType, payload: String, attempt: Int) {
        jdbcTemplate!!.update("insert into dbqueue.task (queue_name, payload, created_at, next_process_at, total_attempt) " +
            "values (?, ?, now(), now(), ?)", queue.name, payload, attempt)
    }
}
