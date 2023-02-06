package ru.yandex.market.logistics.calendaring.dbqueue.payload

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest

class ReleaseQuotaWhenCancelBookingTest : AbstractContextualTest() {


    /**
     * Для booking_id = 3 с типом EXPENDABLE_MATERIALS не должна быть сгенерирована таска
     * */
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/cancel-booking/release-quota-task-created/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/cancel-booking/release-quota-task-created/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
        connection = "dbqueueDatabaseConnection"
    )
    fun releaseQuotaWhenCancelingBookingTest() {
        mockMvc!!.perform(
            MockMvcRequestBuilders.delete("/booking")
                .param("bookingIds", "1", "2", "3")
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

}
