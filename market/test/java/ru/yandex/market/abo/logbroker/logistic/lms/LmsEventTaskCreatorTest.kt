package ru.yandex.market.abo.logbroker.logistic.lms

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import ru.yandex.market.abo.core.lms.LmsEventTaskService
import ru.yandex.market.abo.core.lms.LmsEventTaskType.EXPRESS_WAREHOUSE_CHANGED
import ru.yandex.market.abo.core.lms.LmsEventTaskType.WAREHOUSE_CREATED
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB
import ru.yandex.market.logistics.management.entity.logbroker.EventDto

/**
 * @author zilzilok
 */
class LmsEventTaskCreatorTest {
    private val lmsEventTaskService = LmsEventTaskService(mock(), mock())
    private val lmsEventTaskCreator = LmsEventTaskCreator(mock(), lmsEventTaskService)

    @Test
    fun `create creation task`() {
        val task = lmsEventTaskCreator.createTaskOrNull(getCreationEvent())!!
        assertEquals(EVENT_ID, task.eventId)
        assertEquals(SERVICE_ID, task.partnerId)
        assertEquals(DSBB, task.partnerModel)
        assertEquals(WAREHOUSE_CREATED, task.type)
        assertNull(task.body)
    }

    @Test
    fun `create change task`() {
        val task = lmsEventTaskCreator.createTaskOrNull(getChangeEvent())!!
        assertEquals(EVENT_ID, task.eventId)
        assertEquals(SERVICE_ID, task.partnerId)
        assertEquals(DSBB, task.partnerModel)
        assertEquals(EXPRESS_WAREHOUSE_CHANGED, task.type)
        assertNotNull(task.body)
    }

    private fun getChangeEvent() =
        MAPPER.readValue(javaClass.getResourceAsStream("/logistic/update_warehouse.json"), EventDto::class.java)

    private fun getCreationEvent() =
        MAPPER.readValue(
            """
            {
              "eventId": $EVENT_ID,
              "entityType": "BUSINESS_WAREHOUSE",
              "entitySnapshot": {
                "partnerId": $SERVICE_ID,
                "partnerType": "DROPSHIP"
              },
              "entityDiff": [
                {
                  "op": "replace",
                  "fromValue": null,
                  "path": "/partnerId",
                  "value": $SERVICE_ID
                }
              ]
            }
            """, EventDto::class.java
        )

    companion object {
        private val MAPPER = ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(JavaTimeModule())
        private const val EVENT_ID = 1L
        private const val SERVICE_ID = 23L
    }
}
