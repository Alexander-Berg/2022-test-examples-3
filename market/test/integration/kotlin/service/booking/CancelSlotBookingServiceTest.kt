package ru.yandex.market.logistics.calendaring.service.booking

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DbUnitConfiguration
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.annotation.ExpectedDatabases
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.times
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.base.NullableColumnsDataSetLoader

@DbUnitConfiguration(
    databaseConnection = ["dbUnitDatabaseConnection", "dbqueueDatabaseConnection"],
    dataSetLoader = NullableColumnsDataSetLoader::class
)
internal class CancelSlotBookingServiceTest(
    @Autowired private val cancelSlotBookingService: CancelSlotBookingService,
    @Autowired private val ffWfClientApi: FulfillmentWorkflowClientApi,
) : AbstractContextualTest() {

    @BeforeEach
    internal fun setUp() {
        Mockito.reset(ffWfClientApi)
    }

    @AfterEach
    internal fun tearDown() {
        Mockito.verifyNoMoreInteractions(ffWfClientApi)
    }

    @DatabaseSetup(value = ["classpath:fixtures/repository/booking/cancel-expired/before.xml"])

    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:fixtures/repository/booking/cancel-expired/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        ),
        ExpectedDatabase(
            value = "classpath:fixtures/repository/booking/cancel-expired/dbqueue.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT,
            connection = "dbqueueDatabaseConnection"
        )
    )
    @Test
    fun cancelExpired() {
        cancelSlotBookingService.cancelExpired()

        Mockito.verify(ffWfClientApi).deactivateBooking(Mockito.anyList())
    }
}
