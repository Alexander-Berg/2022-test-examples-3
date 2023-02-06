package ru.yandex.market.wms.picking.repositories

import io.qameta.allure.kotlin.junit4.AllureRunner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.market.wms.picking.data.entities.Assignment
import ru.yandex.market.wms.picking.data.entities.Container
import ru.yandex.market.wms.picking.data.entities.PickingItem
import ru.yandex.market.wms.picking.generators.*

@ExperimentalCoroutinesApi
@RunWith(AllureRunner::class)
class InMemoryPickingStateRepositoryTest {

    private val seedGenerator = SeedAutoIncrementer()

    private val initializedAssignments: List<Assignment> = generateAssignmentList(seedGenerator.nextState())
    private val initializedContainers: List<Container> = generateContainerList(seedGenerator.nextState())
    private val initializedPickingItems: List<PickingItem> = generateItemList(seedGenerator.nextState())

    private lateinit var pickingStateRepository: InMemoryPickingStateRepository

    private suspend fun setDataForRepository() {
        pickingStateRepository.setAssignments(initializedAssignments)
        pickingStateRepository.setContainers(initializedContainers)
        pickingStateRepository.setPickingItems(initializedPickingItems)
    }

    @Before
    fun `SetUp PickingRepository`() = runTest {
        pickingStateRepository = InMemoryPickingStateRepository()
        setDataForRepository()
    }

    @Test
    fun `Test setting assignments`() = runTest {
        assertEquals(initializedAssignments, pickingStateRepository.assignments().first())

        val newAssignments = generateAssignmentList(seedGenerator.nextState())
        pickingStateRepository.setAssignments(newAssignments)

        assertEquals(newAssignments, pickingStateRepository.assignments().first())
        assertNotEquals(initializedAssignments, newAssignments)
    }

    @Test
    fun `Test adding new assignment`() = runTest {
        assertEquals(initializedAssignments, pickingStateRepository.assignments().first())

        val newAssignment = generateAssignment(seedGenerator.nextState())
        pickingStateRepository.addOrUpdateAssignment(newAssignment)

        assertEquals(initializedAssignments + newAssignment, pickingStateRepository.assignments().first())
    }

    @Test
    fun `Test updating assignment`() = runTest {
        val newAssignmentToUpdate = initializedAssignments.last().copy(workingArea = "another_working_area")
        pickingStateRepository.addOrUpdateAssignment(newAssignmentToUpdate)

        val assignmentsInRepo = pickingStateRepository.assignments().first()
        val lastAssignmentInRepo = assignmentsInRepo.last()

        assertEquals(newAssignmentToUpdate, lastAssignmentInRepo)
        assertEquals(1, assignmentsInRepo.filter { it.pickingOrder == newAssignmentToUpdate.pickingOrder }.size)
    }

    @Test
    fun `Test setting containers`() = runTest {
        assertEquals(initializedContainers, pickingStateRepository.containers().first())

        val newContainers = generateContainerList(seedGenerator.nextState())
        pickingStateRepository.setContainers(newContainers)

        assertEquals(newContainers, pickingStateRepository.containers().first())
        assertNotEquals(newContainers, initializedContainers)
    }

    @Test
    fun `Test adding new container`() = runTest {
        assertEquals(initializedContainers, pickingStateRepository.containers().first())

        val newContainer = generateContainer(seedGenerator.nextState())
        pickingStateRepository.addOrUpdateContainer(newContainer)

        assertEquals(initializedContainers + newContainer, pickingStateRepository.containers().first())
    }

    @Test
    fun `Test updating new container`() = runTest {
        val newContainerToUpdate = initializedContainers.last().copy(assignmentNumber = "another_assignment")
        pickingStateRepository.addOrUpdateContainer(newContainerToUpdate)

        val containersInRepo = pickingStateRepository.containers().first()
        val lastContainer = containersInRepo.last()

        assertEquals(newContainerToUpdate, lastContainer)
        assertEquals(1, containersInRepo.filter { it.containerId == newContainerToUpdate.containerId }.size)
    }

    @Test
    fun `Test setting picking items`() = runTest {
        assertEquals(initializedPickingItems, pickingStateRepository.pickingItems().first())

        val newPickingItems = generateItemList(seedGenerator.nextState())
        pickingStateRepository.setPickingItems(newPickingItems)

        assertEquals(newPickingItems, pickingStateRepository.pickingItems().first())
        assertNotEquals(newPickingItems, initializedPickingItems)
    }

    @Test
    fun `Test adding new picking item`() = runTest {
        assertEquals(initializedPickingItems, pickingStateRepository.pickingItems().first())

        val newPickingItem = generateItem(seedGenerator.nextState())
        pickingStateRepository.addOrUpdatePickingItem(newPickingItem)

        assertEquals(initializedPickingItems + newPickingItem, pickingStateRepository.pickingItems().first())
    }

    @Test
    fun `Test updating picking item`() = runTest {
        val newPickingItemToUpdate = initializedPickingItems.last().copy(location = "another_location")
        pickingStateRepository.addOrUpdatePickingItem(newPickingItemToUpdate)

        val pickingItemsInRepo = pickingStateRepository.pickingItems().first()
        val lastPickingItemInRepo = pickingItemsInRepo.last()

        assertEquals(newPickingItemToUpdate, lastPickingItemInRepo)
        assertEquals(1, pickingItemsInRepo.filter { it.pickPositionKey == newPickingItemToUpdate.pickPositionKey }.size)
    }

    @Test
    fun `Test adding a list of picking items`() = runTest {
        val newPickingItemsToUpdate = generateItemList(seedGenerator.nextState())
        pickingStateRepository.addOrUpdatePickingItems(newPickingItemsToUpdate)

        assertEquals(initializedPickingItems + newPickingItemsToUpdate, pickingStateRepository.pickingItems().first())
    }

    @Test
    fun `Test removing all picking items with field moved == false (all items case)`() = runTest {
        pickingStateRepository.setPickingItems(initializedPickingItems.map { it.copy(moved = false) })
        pickingStateRepository.removeUnmovedItem()

        assertTrue(pickingStateRepository.pickingItems().first().isEmpty())
    }

    @Test
    fun `Test removing all picking items with field moved == false (random items case)`() = runTest {
        pickingStateRepository.removeUnmovedItem()

        val movedItems = initializedPickingItems.filter { it.moved }
        val allItems = pickingStateRepository.pickingItems().first()

        assertEquals(movedItems, allItems)
    }

    @Test
    fun `Test clearing repository`() = runTest {
        pickingStateRepository.clear()

        assertTrue(pickingStateRepository.assignments().first().isEmpty())
        assertTrue(pickingStateRepository.containers().first().isEmpty())
        assertTrue(pickingStateRepository.pickingItems().first().isEmpty())
    }
}
