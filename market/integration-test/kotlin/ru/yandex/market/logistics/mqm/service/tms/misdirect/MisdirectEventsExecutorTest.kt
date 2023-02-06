package ru.yandex.market.logistics.mqm.service.tms.misdirect

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.service.yt.YtService
import java.time.Instant

class MisdirectEventsExecutorTest: AbstractContextualTest() {

    @Autowired
    private lateinit var ytService: YtService

    @Autowired
    lateinit var transactionTemplate: TransactionOperations

    @Autowired
    private lateinit var executor: MisdirectEventsExecutor

    @BeforeEach
    fun setup() {
        clock.setFixed(Instant.parse("2021-08-27T10:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @Test
    @DatabaseSetup("/tms/misdirect/setup_with_order.xml")
    @ExpectedDatabase(
        value = "/tms/misdirect/new_plan_fact_without_user_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createNewPlanFactWithoutUser() {
        mockUser(listOf())
        mockEvents(
            listOf(
                misdirectEventRecord(
                    userId = "1",
                    timestamp = Instant.parse("2021-08-27T10:00:01.00Z"),
                    barcode = "L1",
                    sortingCenter = "SC_1"
                ),
                nonMisdirectEvent(),
                misdirectEventRecord(
                    userId = "1",
                    timestamp = Instant.parse("2021-08-27T10:00:02.00Z"),
                    barcode = "L2",
                    sortingCenter = "SC_2"
                ),
                nonMisdirectEvent(),
            )
        )
        executor.run()
    }

    @Test
    @DatabaseSetup("/tms/misdirect/setup_with_order.xml")
    @ExpectedDatabase(
        value = "/tms/misdirect/new_plan_fact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createNewPlanFact() {
        val testUserId1 = "user_id_1"
        val testUserId2 = "user_id_2"
        mockUser(
            listOf(
                SortingCenterUser(
                    sortingCenterId = 1,
                    userId = testUserId1,
                    email = "e1@mail.com",
                    name = "name_1",
                    role = "role_1",
                ),
                SortingCenterUser(
                    sortingCenterId = 2,
                    userId = testUserId2,
                    email = "e2@mail.com",
                    name = "name_2",
                    role = "role_2",
                )
            )
        )
        mockEvents(
            listOf(
                misdirectEventRecord(
                    userId = testUserId1,
                    timestamp = Instant.parse("2021-08-27T10:00:01.00Z"),
                    barcode = "L1",
                    sortingCenter = "SC_1"
                ),
                nonMisdirectEvent(),
                misdirectEventRecord(
                    userId = testUserId2,
                    timestamp = Instant.parse("2021-08-27T10:00:02.00Z"),
                    barcode = "L2",
                    sortingCenter = "SC_2"
                ),
                nonMisdirectEvent(),
            )
        )
        executor.run()
    }

    @Test
    @DatabaseSetup("/tms/misdirect/setup_with_plan_fact.xml")
    @ExpectedDatabase(
        value = "/tms/misdirect/plan_fact_with_state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun doNotCreateNewPlanFactsIfExists() {
        val testUserId = "user_id"
        mockUser(
            listOf(
                SortingCenterUser(
                    sortingCenterId = 1,
                    userId = testUserId,
                    email = "e1@mail.com",
                    name = "name_1",
                    role = "role_1",
                )
            )
        )
        mockEvents(
            listOf(
                misdirectEventRecord(
                    timestamp = Instant.parse("8888-08-27T10:00:01.00Z"),
                    barcode = "L1",
                    sortingCenter = "SC_1",
                    userId = testUserId,
                ),
                misdirectEventRecord(
                    timestamp = Instant.parse("9999-08-27T10:00:04.00Z"),
                    barcode = "L1",
                    sortingCenter = "SC_1",
                    userId = testUserId,
                ),
            )
        )
        executor.run()
    }

    @Test
    @DatabaseSetup("/tms/misdirect/setup_with_order.xml")
    @ExpectedDatabase(
        value = "/tms/misdirect/no_plan_facts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun doNotCreateIfOrderNotExists() {
        val testUserId = "user_id"
        mockUser(
            listOf(
                SortingCenterUser(
                    sortingCenterId = 1,
                    userId = testUserId,
                    email = "e1@mail.com",
                    name = "name_1",
                    role = "role_1",
                )
            )
        )
        mockEvents(
            listOf(
                misdirectEventRecord(
                    timestamp = Instant.parse("8888-08-27T10:00:01.00Z"),
                    barcode = "L5",
                    sortingCenter = "SC_1",
                    userId = testUserId,
                ),
                misdirectEventRecord(
                    timestamp = Instant.parse("9999-08-27T10:00:04.00Z"),
                    barcode = "L5",
                    sortingCenter = "SC_2",
                    userId = testUserId,
                ),
                misdirectEventRecord(
                    timestamp = Instant.parse("9999-08-27T10:00:05.00Z"),
                    barcode = "L5",
                    sortingCenter = "SC_2",
                    userId = testUserId,
                ),
                misdirectEventRecord(
                    timestamp = Instant.parse("9999-08-27T10:00:06.00Z"),
                    barcode = "L5",
                    sortingCenter = "SC_2",
                    userId = testUserId,
                ),
            )
        )
        executor.run()
    }

    @Test
    @DatabaseSetup("/tms/misdirect/setup_with_plan_fact.xml")
    @ExpectedDatabase(
        value = "/tms/misdirect/setup_with_plan_fact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun nothingChangedIfNoEvents() {
        mockEvents(listOf())
        executor.run()
    }

    @Test
    @DatabaseSetup("/tms/misdirect/setup_with_plan_fact.xml")
    @ExpectedDatabase(
        value = "/tms/misdirect/no_plan_facts_after_one_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun nothingChangedIfNoMisdirectEvents() {
        mockUser(listOf())
        mockEvents(listOf(nonMisdirectEvent()))
        executor.run()
    }

    @Test
    @DatabaseSetup("/tms/misdirect/setup_with_plan_fact.xml")
    @ExpectedDatabase(
        value = "/tms/misdirect/setup_with_plan_fact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun nothingChangedIfFailed() {
        doThrow(RuntimeException("test_exception")).whenever(ytService)
            .readTableFromRowToRow<MisdirectEventRecord>(any(), any(), any(), any(), any())
        executor.run()
    }

    private fun mockEvents(events: List<MisdirectEventRecord>) {
        doReturn(events, listOf<MisdirectEventRecord>()).whenever(ytService)
            .readTableFromRowToRow<MisdirectEventRecord>(any(), any(), any(), any(), any())
    }

    private fun mockUser(users: List<SortingCenterUser>) {
        doReturn(users).whenever(ytService).readTable<SortingCenterUser>(any(), any(), any())
    }

    private fun misdirectEventRecord(
        timestamp: Instant,
        barcode: String,
        sortingCenter: String,
        userId: String,
    ) = MisdirectEventRecord(
        timestamp = timestamp.epochSecond.toString(),
        barcode = barcode,
        ownerId = userId,
        sortingCenter = sortingCenter,
        eventType = MisdirectEventsExecutor.MISDIRECT_EVENT_TYPE_FILTER,
    )

    private fun nonMisdirectEvent() = MisdirectEventRecord(
        timestamp = "1",
        barcode = "barcode",
        ownerId = "",
        sortingCenter = "sortingCenter",
        eventType = MisdirectEventsExecutor.MISDIRECT_EVENT_TYPE_FILTER,
    )
}
