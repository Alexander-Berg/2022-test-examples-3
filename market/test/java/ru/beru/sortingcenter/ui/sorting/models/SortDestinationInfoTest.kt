package ru.beru.sortingcenter.ui.sorting.models

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.beru.sortingcenter.R
import ru.beru.sortingcenter.ui.sorting.orders.models.SortDestinationInfo
import ru.beru.sortingcenter.ui.sorting.orders.state.State
import ru.yandex.market.sc.core.resources.StringManager
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class SortDestinationInfoTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var stringManager: StringManager

    private lateinit var sortDestinationInfo: SortDestinationInfo
    private val state = MutableLiveData<State>()

    @Before
    fun setUp() {
        `when`(stringManager.getString(R.string.empty)).thenReturn("")
        `when`(
            (stringManager.getString(
                eq(R.string.sort_cell_description),
                ArgumentMatchers.any(ExternalId::class.java)
            ))
        ).thenAnswer {
            val externalId = it.getArgument<ExternalId>(1)
            return@thenAnswer "Сортировка заказа\n$externalId\nЯчейка"
        }
    }

    @Test
    fun `scan order with type KEEP with cells without palletization required`() {
        val order = TestFactory.createOrder()
            .keep()
            .withAvailableCells(
                listOf(
                    TestFactory.getCourierCell(),
                    TestFactory.getCourierCell(),
                )
            )
            .updatePalletizationRequired(false)
            .build()
        val place = order.places.first()

        sortDestinationInfo = SortDestinationInfo(state, stringManager)
        state.value = State.ScanSuccess.ScanPlace(order, place)

        assertEquals(
            stringManager.getString(R.string.sort_cell_description, place.externalId),
            sortDestinationInfo.description.getOrAwaitValue()
        )
        assertEquals(place.joinedCellInfo, sortDestinationInfo.number.getOrAwaitValue())
        assertEquals(true, sortDestinationInfo.isNumberAvailable.getOrAwaitValue())
    }

    @Test
    fun `scan order with type KEEP with cells & lots without palletization required`() {
        val order = TestFactory.createOrder()
            .keep()
            .withAvailableLots(
                listOf(
                    TestFactory.createCellLot(),
                    TestFactory.createCellLot(),
                )
            )
            .withAvailableCells(
                listOf(
                    TestFactory.getCourierCell(),
                    TestFactory.getCourierCell(),
                )
            )
            .updatePalletizationRequired(false)
            .build()
        val place = order.places.first()

        sortDestinationInfo = SortDestinationInfo(state, stringManager)
        state.value = State.ScanSuccess.ScanPlace(order, place)

        assertEquals(
            stringManager.getString(R.string.sort_cell_description, place.externalId),
            sortDestinationInfo.description.getOrAwaitValue()
        )
        assertEquals(place.joinedCellInfo, sortDestinationInfo.number.getOrAwaitValue())
        assertEquals(true, sortDestinationInfo.isNumberAvailable.getOrAwaitValue())
    }

    @Test
    fun `scan order with type KEEP with lots with palletization required`() {
        val order = TestFactory.createOrder()
            .keep()
            .withAvailableLots(
                listOf(
                    TestFactory.createCellLot(),
                    TestFactory.createCellLot(),
                )
            )
            .updatePalletizationRequired(true)
            .build()
        val place = order.places.first()

        sortDestinationInfo = SortDestinationInfo(state, stringManager)
        state.value = State.ScanSuccess.ScanPlace(order, place)

        assertEquals(
            stringManager.getString(R.string.empty),
            sortDestinationInfo.description.getOrAwaitValue()
        )
        assertEquals(false, sortDestinationInfo.isNumberAvailable.getOrAwaitValue())
    }
}
