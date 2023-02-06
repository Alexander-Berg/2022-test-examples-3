package ru.yandex.market.wms.dimensionmanagement.repository

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.dimensionmanagement.configuration.DimensionManagementIntegrationTest
import ru.yandex.market.wms.dimensionmanagement.core.domain.ContainerStatus
import ru.yandex.market.wms.dimensionmanagement.core.dto.ContainerDto

class ContainerRepositoryTest : DimensionManagementIntegrationTest() {

    @Autowired
    private val repository: ContainerRepository? = null

    @Test
    @DatabaseSetup("/repository/container-repository/immutable-state.xml")
    @ExpectedDatabase(
        value = "/repository/container-repository/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun findContainerTest() {
        val containerId = "TM00001"
        val expectedContainer = ContainerDto(
            containerId = containerId,
            stationId = 1,
            stationLoc = "loc1",
            status = ContainerStatus.ACTIVE,
            parentContainerId = "TM00002"
        )

        val container = repository!!.findContainer(containerId)

        Assertions.assertEquals(expectedContainer, container)
    }

    @Test
    @DatabaseSetup("/repository/container-repository/immutable-state.xml")
    @ExpectedDatabase(
        value = "/repository/container-repository/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun findChildContainersTest() {
        val parentContainerId = "TM00002"
        val expectedContainers = listOf(
            ContainerDto(
                containerId = "TM00001",
                stationId = 1,
                stationLoc = "loc1",
                status = ContainerStatus.ACTIVE,
                parentContainerId = parentContainerId
            )
        )

        val containers = repository!!.findChildContainers(parentContainerId)

        Assertions.assertEquals(expectedContainers, containers)
    }

    @Test
    @DatabaseSetup("/repository/container-repository/immutable-state.xml")
    @ExpectedDatabase(
        value = "/repository/container-repository/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun findActiveContainerAssignedToStationTest() {
        val containerId = "TM00001"
        val expectedContainer = ContainerDto(
            containerId = containerId,
            stationId = 1,
            stationLoc = "loc1",
            status = ContainerStatus.ACTIVE,
            parentContainerId = "TM00002"
        )

        val container = repository!!.findActiveContainerAssignedToStation(1)

        Assertions.assertNotNull(container)
        Assertions.assertEquals(expectedContainer, container)
    }

    @Test
    @DatabaseSetup("/repository/container-repository/immutable-state.xml")
    @ExpectedDatabase(
        value = "/repository/container-repository/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun findRootContainerInWorkAssignedToStationTest() {
        val containerId = "TM00002"
        val expectedContainer = ContainerDto(
            containerId = containerId,
            stationId = 1,
            stationLoc = "loc1",
            status = ContainerStatus.IN_PROGRESS,
            parentContainerId = null
        )

        val container = repository!!.findRootContainerInWorkAssignedToStation(1)

        Assertions.assertNotNull(container)
        Assertions.assertEquals(expectedContainer, container)
    }

    @Test
    @DatabaseSetup("/repository/container-repository/immutable-state.xml")
    @ExpectedDatabase(
        value = "/repository/container-repository/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun findRootContainerInWorkAssignedToStationByIdAndStationLocTest() {
        val containerId = "TM00002"
        val expectedContainer = ContainerDto(
            containerId = containerId,
            stationId = 1,
            stationLoc = "loc1",
            status = ContainerStatus.IN_PROGRESS,
            parentContainerId = null
        )

        val container = repository!!.findRootContainerInWorkAssignedToStation("TM00002", "loc1")

        Assertions.assertNotNull(container)
        Assertions.assertEquals(expectedContainer, container)
    }

    @Test
    @DatabaseSetup("/repository/container-repository/immutable-state.xml")
    @ExpectedDatabase(
        value = "/repository/container-repository/assign-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignContainerTest() {
        repository!!.assignContainer("TM00004", 2)
    }

    @Test
    @DatabaseSetup("/repository/container-repository/immutable-state.xml")
    @ExpectedDatabase(
        value = "/repository/container-repository/assign-with-parent-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignContainerWithParentTest() {
        repository!!.assignContainer("TM00004", 2, "TM00003")
    }

    @Test
    @DatabaseSetup("/repository/container-repository/immutable-state.xml")
    @ExpectedDatabase(
        value = "/repository/container-repository/update-status-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateContainerStatusTest() {
        repository!!.updateContainerStatus("TM00003", ContainerStatus.ACTIVE)
    }

    @Test
    @DatabaseSetup("/repository/container-repository/immutable-state.xml")
    @ExpectedDatabase(
        value = "/repository/container-repository/delete-with-children-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun deleteByIdWithChildrenTest() {
        repository!!.deleteByIdWithChildren("TM00002")
    }

    @Test
    @DatabaseSetup("/repository/container-repository/immutable-state.xml")
    @ExpectedDatabase(
        value = "/repository/container-repository/delete-without-children-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun deleteByIdWithoutChildrenTest() {
        repository!!.deleteByIdWithChildren("TM00003")
    }

    @Test
    @DatabaseSetup("/repository/container-repository/immutable-state.xml")
    @ExpectedDatabase(
        value = "/repository/container-repository/delete-child-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun deleteChildByIdTest() {
        repository!!.deleteByIdWithChildren("TM00001")
    }
}
