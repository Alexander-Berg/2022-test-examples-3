package ru.yandex.market.wms.taskrouter.task.service.taskmanagement

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.jms.core.JmsTemplate
import ru.yandex.market.wms.common.model.dto.SortPickingAssignmentsRequest
import ru.yandex.market.wms.shared.libs.async.jms.QueueNameConstants

private const val algorithmVersion = "ANYALGO"
private const val user = "User1"
private const val selectedZone = "MEZONIN_2"
private val assignmentNumbers = listOf("AN2")

class AssignmentUpdaterServiceTest(
    @Autowired val assignmentUpdaterService: AssignmentUpdaterService,
    @Autowired @SpyBean private val jmsTemplate: JmsTemplate,
) : BaseTaskManagementIntegrationTest() {

    @BeforeEach
    fun beforeEach() {
        Mockito.reset(jmsTemplate)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/taskmanagement/db/common.xml"),
        DatabaseSetup("/taskmanagement/db/reversepicking/1/before.xml")
    )
    @ExpectedDatabase(
        value = "/taskmanagement/db/reversepicking/after-reverse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `assigning reverse picking`() {
        assignmentUpdaterService.updateTasks(user, assignmentNumbers, selectedZone, algorithmVersion)
        verifySortAssignmentsAsyncRequestSent()
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/taskmanagement/db/common.xml"),
        DatabaseSetup("/taskmanagement/db/reversepicking/2/before.xml")
    )
    @ExpectedDatabase(
        value = "/taskmanagement/db/reversepicking/after-direct.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `reverse picking flag turned off`() {
        assignmentUpdaterService.updateTasks(user, assignmentNumbers, selectedZone, algorithmVersion)
        verifySortAssignmentsAsyncRequestSent()
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/taskmanagement/db/common.xml"),
        DatabaseSetup("/taskmanagement/db/reversepicking/3/before.xml")
    )
    @ExpectedDatabase(
        value = "/taskmanagement/db/reversepicking/after-direct.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `user does not match regexp`() {
        assignmentUpdaterService.updateTasks(user, assignmentNumbers, selectedZone, algorithmVersion)
        verifySortAssignmentsAsyncRequestSent()
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/taskmanagement/db/common.xml"),
        DatabaseSetup("/taskmanagement/db/reversepicking/4/before.xml")
    )
    @ExpectedDatabase(
        value = "/taskmanagement/db/reversepicking/after-direct.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `last user assignment was too long ago`() {
        assignmentUpdaterService.updateTasks(user, assignmentNumbers, selectedZone, algorithmVersion)
        verifySortAssignmentsAsyncRequestSent()
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/taskmanagement/db/common.xml"),
        DatabaseSetup("/taskmanagement/db/reversepicking/5/before.xml")
    )
    @ExpectedDatabase(
        value = "/taskmanagement/db/reversepicking/after-direct.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `selected zone is not enabled for reverse picking`() {
        assignmentUpdaterService.updateTasks(user, assignmentNumbers, selectedZone, algorithmVersion)
        verifySortAssignmentsAsyncRequestSent()
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/taskmanagement/db/common.xml"),
        DatabaseSetup("/taskmanagement/db/reversepicking/6/before.xml")
    )
    @ExpectedDatabase(
        value = "/taskmanagement/db/reversepicking/6/after-direct.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `selected zone does not match last assignment zone`() {
        assignmentUpdaterService.updateTasks(user, assignmentNumbers, selectedZone, algorithmVersion)
        verifySortAssignmentsAsyncRequestSent()
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/taskmanagement/db/common.xml"),
        DatabaseSetup("/taskmanagement/db/reversepicking/7/before.xml")
    )
    @ExpectedDatabase(
        value = "/taskmanagement/db/reversepicking/7/after-direct.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `last assignment was reverse`() {
        assignmentUpdaterService.updateTasks(user, assignmentNumbers, selectedZone, algorithmVersion)
        verifySortAssignmentsAsyncRequestSent()
    }

    private fun verifySortAssignmentsAsyncRequestSent() {
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
            Mockito.eq(QueueNameConstants.SORT_PICKING_ASSIGNMENTS),
            Mockito.eq(SortPickingAssignmentsRequest.builder().assignmentNumbers(assignmentNumbers).build())
        )
    }
}
