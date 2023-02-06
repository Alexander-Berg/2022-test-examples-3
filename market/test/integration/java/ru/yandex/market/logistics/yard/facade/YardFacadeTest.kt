package ru.yandex.market.logistics.yard.facade

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.annotation.ExpectedDatabases
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.facade.EdgeFacade
import ru.yandex.market.logistics.yard_v2.facade.YardFacade

class YardFacadeTest(
    @Autowired private val yardFacade: YardFacade,
    @Autowired private val edgeFacade: EdgeFacade
) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_flow/1/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/yard_flow/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun refreshClientStateSuccess() {
        val refreshClientStateResult = yardFacade.refreshClientState(0L)

        assertions().assertThat(refreshClientStateResult.isSuccess()).isTrue
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_flow/2/before.xml"])
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:fixtures/repository/yard_flow/2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        ),
        ExpectedDatabase(
            value = "classpath:fixtures/repository/yard_flow/2/dbqueue.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "dbqueueDatabaseConnection"
        )
    )
    fun moveToNextStateSuccess() {
        yardFacade.processClientQueueByStateToId(1001)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_flow/13/before.xml"])
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:fixtures/repository/yard_flow/13/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        ),
        ExpectedDatabase(
            value = "classpath:fixtures/repository/yard_flow/13/dbqueue.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "dbqueueDatabaseConnection"
        )
    )
    fun moveToNextStateWithCheckAvailableCapacityUnitsSuccess() {
        yardFacade.processClientQueueByStateToId(1001)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_flow/14/before.xml"])
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:fixtures/repository/yard_flow/14/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        ),
        ExpectedDatabase(
            value = "classpath:fixtures/repository/yard_flow/14/dbqueue.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "dbqueueDatabaseConnection"
        )
    )
    fun moveToNextStateWithNoAvailableCapacityUnits() {
        yardFacade.processClientQueueByStateToId(1001)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_flow/2/before.xml"])
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:fixtures/repository/yard_flow/2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        ),
        ExpectedDatabase(
            value = "classpath:fixtures/repository/yard_flow/2/dbqueue.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "dbqueueDatabaseConnection"
        )
    )
    fun moveToNextStateForClientGetClientsOnlyWithSameToStateId() {
        yardFacade.processClientQueueByStateToId(1001)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_flow/3/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/yard_flow/3/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun shouldMoveToNextStateIfCapacityIsBusyAndMovingWithinSameCapacity() {
        val result = yardFacade.processLinkedComponentsAndMoveToNextState(0, edgeFacade.getFullById(1)!!)
        assertions().assertThat(result.isSuccess()).isTrue
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_flow/5/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/yard_flow/5/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun refreshClientStateSuccessForClientAlreadyInQueue() {
        val refreshClientStateResult = yardFacade.refreshClientState(0L)

        assertions().assertThat(refreshClientStateResult.isSuccess()).isTrue
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_flow/6/before.xml"])
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:fixtures/repository/yard_flow/6/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        )
    )
    fun moveToNextStateSuccessInCaseRestrictionsOfTwoEdgesApplicableAndDifferentPriority() {
        val refreshClientStateResult = yardFacade.refreshClientState(0L)

        assertions().assertThat(refreshClientStateResult.isSuccess()).isTrue
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_flow/7/before.xml"])
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:fixtures/repository/yard_flow/7/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        )
    )
    fun moveToNextStateFailInCaseRestrictionsOfTwoEdgesApplicableAndSamePriority() {
        try {
            yardFacade.refreshClientState(0L)
        } catch (e: Exception) {
            assertions().assertThat(e.message)
                .contains("There are more than one allowed edge with max priority for client")
        }
    }

    /**
     * Проверка фриза и анфриза капасити юнитов при переходе из одного капасити в другой.
     */
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_flow/8/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/yard_flow/8/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun correctFreezeAndUnfreezeCapacityUnits() {
        val result = yardFacade.processLinkedComponentsAndMoveToNextState(0, edgeFacade.getFullById(1)!!)
        assertions().assertThat(result.isSuccess()).isTrue
    }

    /**
     * Проверка фриза при пустой мете клиента.
     */
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_flow/9/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/yard_flow/9/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun correctFreezeWithEmptyClientMeta() {
        val result = yardFacade.processLinkedComponentsAndMoveToNextState(0, edgeFacade.getFullById(1)!!)
        assertions().assertThat(result.isSuccess()).isTrue
    }

    /**
     * Проверка фриза при указанном в мете капасити юните, отличном от текущего.
     */
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_flow/10/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/yard_flow/10/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun correctFreezeAndUnfreezeWitNotEmptyClientMeta() {
        val result = yardFacade.processLinkedComponentsAndMoveToNextState(0, edgeFacade.getFullById(1)!!)
        assertions().assertThat(result.isSuccess()).isTrue
    }

    /**
     * Проверка фриза при указанном в мете капасити юните, но этот капасити юнит не активен.
     */
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_flow/11/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/yard_flow/11/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun correctFreezeAndUnfreezeWitNotEmptyClientMetaAndInactiveCapacityUnit() {
        val result = yardFacade.processLinkedComponentsAndMoveToNextState(0, edgeFacade.getFullById(1)!!)
        assertions().assertThat(result.isSuccess()).isTrue
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_flow/15/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/yard_flow/15/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun refreshAllClientsBeforeCapacity() {
        val result = yardFacade.processLinkedComponentsAndMoveToNextState(0, edgeFacade.getFullById(1)!!)
        assertions().assertThat(result.isSuccess()).isTrue
    }
    /**
     * Проверка того, что при неуспешном фризе, клиенты с высоким приоритетом удаляются из очереди.
     */
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_flow/16/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/yard_flow/16/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun removeHighPriorityClientsIfRestrictionApplicationFailed() {
        val result = yardFacade.processLinkedComponentsAndMoveToNextState(0, edgeFacade.getFullById(1)!!)
        assertions().assertThat(result.isSuccess()).isFalse
    }

    @Disabled
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_flow/17/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/yard_flow/17/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun cancelHangingClients() {
        yardFacade.cancelHangingClients()
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_flow/cancel-postponed/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/yard_flow/cancel-postponed/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testCancelPostponed() {
        yardFacade.cancelPostponed()
    }
}
