package ru.yandex.market.wms.picking.usecases

import io.mockk.mockk
import io.qameta.allure.kotlin.junit4.AllureRunner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.market.wms.picking.generators.*
import ru.yandex.market.wms.picking.repositories.InMemoryPickingStateRepository
import ru.yandex.market.wms.picking.utils.PickingContainerMerger

@ExperimentalCoroutinesApi
@RunWith(AllureRunner::class)
class PickingStateUseCasesTest {

    private val resourceManager = ResourceManagerStub()
    private val itemUseCasesMock = mockk<ItemUseCases>(relaxed = true)

    private val seedGenerator = SeedAutoIncrementer()

    private val initializedAssignments = generateAssignmentList(seedGenerator.nextState())
    private val initializedContainers = generateContainerList(seedGenerator.nextState())
    private val initializedPickingItems = generateItemList(seedGenerator.nextState())

    private lateinit var pickingStateRepository: InMemoryPickingStateRepository
    private lateinit var pickingItemsStateUseCases: PickingItemsStateUseCases
    private lateinit var assignmentsStateUseCases: AssignmentsStateUseCases
    private lateinit var containersStateUseCases: ContainersStateUseCases

    private suspend fun setDataForUseCases() {
        assignmentsStateUseCases.setAssignments(initializedAssignments)
        containersStateUseCases.setContainers(initializedContainers)
        pickingItemsStateUseCases.setPickingItems(initializedPickingItems)
    }

    @Before
    fun `SetUp PickingStateUseCases`() = runTest {
        pickingStateRepository = InMemoryPickingStateRepository()
        pickingItemsStateUseCases = PickingItemsStateUseCases(
            pickingStateRepository = pickingStateRepository,
            itemsUseCases = itemUseCasesMock,
        )
        assignmentsStateUseCases = AssignmentsStateUseCases(pickingStateRepository)
        containersStateUseCases = ContainersStateUseCases(pickingStateRepository, resourceManager)
        setDataForUseCases()
    }

    @Test
    fun `Test setting and getting assignments`() = runTest {
        assertEquals(initializedAssignments, assignmentsStateUseCases.getAssignments().first())

        val newAssignments = generateAssignmentList(seedGenerator.nextState())
        assignmentsStateUseCases.setAssignments(newAssignments)

        assertEquals(newAssignments, assignmentsStateUseCases.getAssignments().first())
        assertNotEquals(newAssignments, initializedAssignments)
    }

    @Test
    fun `Test setting and getting containers`() = runTest {
        assertEquals(initializedContainers, containersStateUseCases.getContainers().first())

        val newContainers = generateContainerList(seedGenerator.nextState())
        containersStateUseCases.setContainers(newContainers)

        assertEquals(newContainers, containersStateUseCases.getContainers().first())
        assertNotEquals(newContainers, initializedContainers)
    }

    @Test
    fun `Test adding container`() = runTest {
        assertEquals(initializedContainers, containersStateUseCases.getContainers().first())

        val newContainerToAdd = generateContainer(seedGenerator.nextState())
        containersStateUseCases.addOrUpdateContainer(newContainerToAdd)

        assertEquals(initializedContainers + newContainerToAdd, containersStateUseCases.getContainers().first())
    }

    @Test
    fun `Test updating container`() = runTest {
        val newContainerToUpdate = initializedContainers.last().copy(assignmentNumber = "another_assignment")
        containersStateUseCases.addOrUpdateContainer(newContainerToUpdate)

        val containersFromUseCase = containersStateUseCases.getContainers().first()
        val lastContainer = containersFromUseCase.last()

        assertEquals(newContainerToUpdate, lastContainer)
        assertEquals(1, containersFromUseCase.filter { it.containerId == newContainerToUpdate.containerId }.size)
    }

    @Test
    fun `Test setting and getting pickingItems`() = runTest {
        assertEquals(initializedPickingItems, pickingItemsStateUseCases.getPickingItems().first())

        val newPickingItems = generateItemList(seedGenerator.nextState())
        pickingItemsStateUseCases.setPickingItems(newPickingItems)

        assertEquals(newPickingItems, pickingItemsStateUseCases.getPickingItems().first())
        assertNotEquals(initializedPickingItems, newPickingItems)
    }

    @Test
    fun `Test adding picking item`() = runTest {
        assertEquals(initializedPickingItems, pickingItemsStateUseCases.getPickingItems().first())

        val newPickingItemToAdd = generateItem(seedGenerator.nextState())
        pickingItemsStateUseCases.addOrUpdatePickingItem(newPickingItemToAdd)

        assertEquals(initializedPickingItems + newPickingItemToAdd, pickingItemsStateUseCases.getPickingItems().first())
    }

    @Test
    fun `Test updating picking item`() = runTest {
        val newPickingItemToUpdate = initializedPickingItems.last().copy(location = "another_location")
        pickingItemsStateUseCases.addOrUpdatePickingItem(newPickingItemToUpdate)

        val pickingItemsFromUseCase = pickingItemsStateUseCases.getPickingItems().first()
        val lastPickingItem = pickingItemsFromUseCase.last()

        assertEquals(newPickingItemToUpdate, lastPickingItem)
        assertEquals(1, pickingItemsFromUseCase.filter { it.pickPositionKey == newPickingItemToUpdate.pickPositionKey }.size)
    }

    @Test
    fun `Test adding a list of picking items`() = runTest {
        val newPickingItemListToAdd = generateItemList(seedGenerator.nextState())
        pickingItemsStateUseCases.addOrUpdatePickingItems(newPickingItemListToAdd)

        assertEquals(initializedPickingItems + newPickingItemListToAdd, pickingItemsStateUseCases.getPickingItems().first())
    }

    @Test
    fun `Test remove all unmoved picking items and update picking items (all items with moved == false case)`() = runTest {
        pickingItemsStateUseCases.setPickingItems(initializedPickingItems.map { it.copy(moved = false) })

        val newPickingItemsToUpdate = generateItemList(seedGenerator.nextState())
        pickingItemsStateUseCases.updateUnmovedPickingItems(newPickingItemsToUpdate)

        assertEquals(newPickingItemsToUpdate, pickingItemsStateUseCases.getPickingItems().first())
    }

    @Test
    fun `Test remove all unmoved picking items and update picking items (random items with moved == false case)`() = runTest {
        val newPickingItemsToUpdate = generateItemList(seedGenerator.nextState())
        pickingItemsStateUseCases.updateUnmovedPickingItems(newPickingItemsToUpdate)

        assertEquals(initializedPickingItems.filter { it.moved } + newPickingItemsToUpdate, pickingItemsStateUseCases.getPickingItems().first())
    }

    @Test
    fun `Test filtering picking items by AssignmentId`() = runTest {
        val lastAssignmentId = initializedPickingItems.last().assignmentNumber
        val itemsFilteredByAssignmentId = initializedPickingItems.filter { it.assignmentNumber == lastAssignmentId }
        val itemsFromUseCaseFilteredByAssignmentId = pickingItemsStateUseCases.filterPickingItemsByAssignmentId(lastAssignmentId).first()

        assertEquals(itemsFilteredByAssignmentId, itemsFromUseCaseFilteredByAssignmentId)

        val anotherAssignmentId = "another_assignment_id"
        assertTrue(pickingItemsStateUseCases.filterPickingItemsByAssignmentId(anotherAssignmentId).first().isEmpty())
    }

    @Test
    fun `Test getting first unmoved picking item`() = runTest {
        pickingItemsStateUseCases.setPickingItems(initializedPickingItems.map { it.copy(moved = true) })
        val firstUnmovedItem = pickingItemsStateUseCases.getItemForMove().first()

        assertNull(firstUnmovedItem)

        pickingItemsStateUseCases.setPickingItems(initializedPickingItems)
        val actualFirstUnmovedItem = initializedPickingItems.firstOrNull { !it.moved }
        val firstUnmovedItemFromUseCase = pickingItemsStateUseCases.getItemForMove().first()

        assertEquals(actualFirstUnmovedItem, firstUnmovedItemFromUseCase)
    }

    @Test
    fun `Test getting containers for items`() = runTest {
        val containerMerger = PickingContainerMerger(resourceManager)
        val actualBoxContainerData = containerMerger.merge(initializedContainers, initializedAssignments)
        val boxContainerDataFromUseCase = containersStateUseCases.getContainersForItems().first()

        assertEquals(actualBoxContainerData, boxContainerDataFromUseCase)
    }

    @Test
    fun `Test filtering picking items by location`() = runTest {
        val lastLocation = initializedPickingItems.last().location
        val itemsFilteredByLocation = initializedPickingItems.filter { it.location == lastLocation }
        val itemsFromUseCaseFilteredByLocation = pickingItemsStateUseCases.filterPickingItemByLoc(lastLocation).first()

        assertEquals(itemsFilteredByLocation, itemsFromUseCaseFilteredByLocation)

        val anotherLocation = "another_location"
        assertTrue(pickingItemsStateUseCases.filterPickingItemByLoc(anotherLocation).first().isEmpty())
    }
}
