package ru.beru.sortingcenter.ui.prepare.lot.info

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.HttpException
import ru.beru.sortingcenter.ui.prepare.data.cache.PrepareLotCache
import ru.yandex.market.sc.core.data.lot.Lot
import ru.yandex.market.sc.core.network.cache.LotDimensionsCache
import ru.yandex.market.sc.core.network.domain.NetworkCheckUserUseCases
import ru.yandex.market.sc.core.network.domain.NetworkInboundUseCases
import ru.yandex.market.sc.core.network.domain.NetworkLotUseCases
import ru.yandex.market.sc.core.ui.arch.compose.immutable.emptyImmutableMap
import ru.yandex.market.sc.core.ui.data.formatter.LotDimensionsFormatter
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.MockitoHelper.anyObject
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.yandex.market.test.utils.mockObserver

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class PrepareLotInfoViewModelTest {

    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkLotUseCases: NetworkLotUseCases

    @Mock
    private lateinit var lotCache: PrepareLotCache

    @Mock
    private lateinit var networkCheckUserUseCases: NetworkCheckUserUseCases

    @Mock
    private lateinit var networkInboundUseCases: NetworkInboundUseCases

    @Mock
    private lateinit var lotDimensionsFormatter: LotDimensionsFormatter

    @Mock
    private lateinit var lotDimensionsCache: LotDimensionsCache

    private lateinit var viewModel: PrepareLotInfoViewModel

    @After
    fun tearDown() {
        viewModel.action.removeObserver(mockObserver)
    }

    @Test
    fun `calls preshipLot with NOT_READY action when lot has NOT_READY action`() = runTest {
        val lot = Mocks.Lots.withNotReadyForShipmentAction
        `when`(
            networkLotUseCases.preshipLot(
                lot.id,
                lot.type,
                Lot.Action.NOT_READY_FOR_SHIPMENT
            )
        ).thenReturn(lot)
        `when`(lotCache.value).thenReturn(lot)
        `when`(networkLotUseCases.getAvailableLotDimensions()).thenReturn(emptySet())
        `when`(lotDimensionsFormatter.format(anyObject(), anyObject())).thenReturn(emptyImmutableMap())

        viewModel = PrepareLotInfoViewModel(
            networkLotUseCases,
            networkCheckUserUseCases,
            networkInboundUseCases,
            lotCache,
            stringManager,
            lotDimensionsFormatter,
            lotDimensionsCache,
        )
        viewModel.action.observeForever(mockObserver)
        viewModel.onPreshipButtonClick()

        assertThat(viewModel.action.getOrAwaitValue()).isEqualTo(Lot.Action.NOT_READY_FOR_SHIPMENT)
        assertThat(viewModel.transferLotButton.isVisible.getOrAwaitValue()).isFalse()
    }

    @Test
    fun `calls preshipLot with READY action when lot has READY action`() = runTest {
        val lot = Mocks.Lots.withReadyForShipmentAction
        `when`(
            networkLotUseCases.preshipLot(
                lot.id,
                lot.type,
                Lot.Action.READY_FOR_SHIPMENT
            )
        ).thenReturn(lot)
        `when`(lotCache.value).thenReturn(lot)
        `when`(networkLotUseCases.getAvailableLotDimensions()).thenReturn(emptySet())
        `when`(lotDimensionsFormatter.format(anyObject(), anyObject())).thenReturn(emptyImmutableMap())

        viewModel = PrepareLotInfoViewModel(
            networkLotUseCases,
            networkCheckUserUseCases,
            networkInboundUseCases,
            lotCache,
            stringManager,
            lotDimensionsFormatter,
            lotDimensionsCache,
        )
        viewModel.action.observeForever(mockObserver)
        viewModel.onPreshipButtonClick()

        assertThat(viewModel.action.getOrAwaitValue()).isEqualTo(Lot.Action.READY_FOR_SHIPMENT)
        assertThat(viewModel.transferLotButton.isVisible.getOrAwaitValue()).isFalse()
    }

    @Test
    fun `preship ready for packing XDOC lot`() = runTest {
        val lot = Mocks.Lots.withReadyForPackingAction
        val vgh = TestFactory.createPalletCharacteristics()
        `when`(
            networkLotUseCases.preshipLot(
                lot.id,
                lot.type,
                Lot.Action.READY_FOR_PACKING
            )
        ).thenReturn(lot.copy(status = Lot.Status.PACKED))
        `when`(lotCache.value).thenReturn(lot)
        `when`(networkLotUseCases.getAvailableLotDimensions()).thenReturn(emptySet())
        `when`(lotDimensionsFormatter.format(anyObject(), anyObject())).thenReturn(emptyImmutableMap())

        viewModel = PrepareLotInfoViewModel(
            networkLotUseCases,
            networkCheckUserUseCases,
            networkInboundUseCases,
            lotCache,
            stringManager,
            lotDimensionsFormatter,
            lotDimensionsCache,
        )
        viewModel.action.observeForever(mockObserver)
        viewModel.onEnterLotVgh(vgh)
        viewModel.onPreshipButtonClick()

        assertThat(viewModel.lotInfo.lot.getOrAwaitValue().status).isEqualTo(Lot.Status.PACKED)
        assertThat(viewModel.transferLotButton.isVisible.getOrAwaitValue()).isFalse()
    }

    @Test
    fun `transfer button visible`() = runTest {
        val lot = Mocks.Lots.transferableLot
        `when`(lotCache.value).thenReturn(lot)
        `when`(networkLotUseCases.getAvailableLotDimensions()).thenReturn(emptySet())
        `when`(lotDimensionsFormatter.format(anyObject(), anyObject())).thenReturn(emptyImmutableMap())
        `when`(networkCheckUserUseCases.isTransferLotToLotEnabled()).thenReturn(true)

        viewModel = PrepareLotInfoViewModel(
            networkLotUseCases,
            networkCheckUserUseCases,
            networkInboundUseCases,
            lotCache,
            stringManager,
            lotDimensionsFormatter,
            lotDimensionsCache,
        )
        assertThat(viewModel.transferLotButton.isVisible.getOrAwaitValue()).isTrue()
    }

    @Test
    fun `click on preshipButton when lot with ADD_STAMP action`() = runTest {
        val lot = Mocks.Lots.withAddStampAction
        `when`(lotCache.value).thenReturn(lot)
        `when`(networkLotUseCases.getAvailableLotDimensions()).thenReturn(emptySet())
        `when`(lotDimensionsFormatter.format(anyObject(), anyObject())).thenReturn(emptyImmutableMap())

        viewModel = PrepareLotInfoViewModel(
            networkLotUseCases,
            networkCheckUserUseCases,
            networkInboundUseCases,
            lotCache,
            stringManager,
            lotDimensionsFormatter,
            lotDimensionsCache,
        )
        viewModel.action.observeForever(mockObserver)
        viewModel.onPreshipButtonClick()

        assertThat(viewModel.action.getOrAwaitValue()).isEqualTo(Lot.Action.ADD_STAMP)
        assertThat(viewModel.addStampEvent.getOrAwaitValue().get()).isEqualTo(lot)
    }

    @Test
    fun `click on deleteStamp button when lot with DELETE_STAMP action`() = runTest {
        val lot = Mocks.Lots.withDeleteStampAction
        `when`(lotCache.value).thenReturn(lot)
        `when`(networkLotUseCases.getAvailableLotDimensions()).thenReturn(emptySet())
        `when`(lotDimensionsFormatter.format(anyObject(), anyObject())).thenReturn(emptyImmutableMap())

        viewModel = PrepareLotInfoViewModel(
            networkLotUseCases,
            networkCheckUserUseCases,
            networkInboundUseCases,
            lotCache,
            stringManager,
            lotDimensionsFormatter,
            lotDimensionsCache,
        )
        viewModel.action.observeForever(mockObserver)

        viewModel.onDeleteStampButtonClick()
        assertThat(viewModel.deleteStampEvent.getOrAwaitValue().get()).isEqualTo(lot)
    }

    @Test
    fun `success lot update`() = runTest {
        val lot = Mocks.Lots.withAddStampAction
        `when`(lotCache.value).thenReturn(lot)
        `when`(networkLotUseCases.getAvailableLotDimensions()).thenReturn(emptySet())
        `when`(lotDimensionsFormatter.format(anyObject(), anyObject())).thenReturn(emptyImmutableMap())

        viewModel = PrepareLotInfoViewModel(
            networkLotUseCases,
            networkCheckUserUseCases,
            networkInboundUseCases,
            lotCache,
            stringManager,
            lotDimensionsFormatter,
            lotDimensionsCache,
        )
        viewModel.action.observeForever(mockObserver)

        `when`(networkLotUseCases.getLot(lot.externalId)).thenReturn(
            lot.copy(
                actions = listOf(Lot.Action.READY_FOR_SHIPMENT)
            )
        )

        viewModel.onLotUpdated(lot.externalId)

        assertThat(viewModel.action.getOrAwaitValue()).isEqualTo(Lot.Action.READY_FOR_SHIPMENT)
    }

    @Test
    fun `fail lot update`() = runTest {
        val lot = Mocks.Lots.withAddStampAction
        `when`(lotCache.value).thenReturn(lot)
        `when`(networkLotUseCases.getAvailableLotDimensions()).thenReturn(emptySet())
        `when`(lotDimensionsFormatter.format(anyObject(), anyObject())).thenReturn(emptyImmutableMap())

        viewModel = PrepareLotInfoViewModel(
            networkLotUseCases,
            networkCheckUserUseCases,
            networkInboundUseCases,
            lotCache,
            stringManager,
            lotDimensionsFormatter,
            lotDimensionsCache,
        )
        viewModel.action.observeForever(mockObserver)

        val response = TestFactory.getResponseError<Int>(code = 400)
        `when`(networkLotUseCases.getLot(lot.externalId)).thenThrow(
            HttpException(response)
        )

        viewModel.onLotUpdated(lot.externalId)

        assertThat(viewModel.hasError.getOrAwaitValue().get()).isNotNull()
    }
}
