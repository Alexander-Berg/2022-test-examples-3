package ru.beru.sortingcenter.ui.xdoc.acceptance

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.HttpException
import ru.beru.sortingcenter.R
import ru.beru.sortingcenter.analytics.AppMetrica
import ru.beru.sortingcenter.ui.common.scannerdestination.ScannerDestinationViewModel.DescriptionStatus
import ru.beru.sortingcenter.ui.common.scannerdestination.ScannerDestinationViewModel.LabelStatus
import ru.beru.sortingcenter.ui.xdoc.acceptance.XDocAcceptanceAsserts.`assert description`
import ru.beru.sortingcenter.ui.xdoc.acceptance.XDocAcceptanceAsserts.`assert label`
import ru.beru.sortingcenter.ui.xdoc.acceptance.XDocAcceptanceAsserts.`assert scanner`
import ru.beru.sortingcenter.ui.xdoc.acceptance.dialogs.DialogEvent
import ru.yandex.market.sc.core.data.inbound.AcceptanceInbound
import ru.yandex.market.sc.core.data.inbound.Inbound
import ru.yandex.market.sc.core.data.sortable.SortableType
import ru.yandex.market.sc.core.network.domain.NetworkInboundUseCases
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.core.utils.domain.sound.SoundPlayer
import ru.yandex.market.sc.test.network.mocks.IdManager
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.network.mocks.TestFactory.createPalletCharacteristics
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.beru.sortingcenter.ui.xdoc.acceptance.model.scanner.ScannerModeImpl as ScannerMode

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class XDocAcceptanceViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkInboundUseCases: NetworkInboundUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    @Mock
    private lateinit var soundPlayer: SoundPlayer

    private val stringManager = TestStringManager()
    private lateinit var viewModel: XDocAcceptanceViewModel

    private val bogusInboundId = IdManager.getExternalId(-1)
    private val inboundErrorMessage = "Inbound does not exist"
    private val palletErrorMessage = "Barcode already was used"

    @Before
    fun setUp() {
        viewModel = XDocAcceptanceViewModel(networkInboundUseCases, appMetrica, stringManager)
        XDocAcceptanceAsserts.bind(viewModel, stringManager)
    }

    @After
    fun tearDown() {
        viewModel.forceReset()
        `wait for inbound`()
    }

    @Test
    fun `wait for inbound`() = runTest {
        `assert scanner`(mode = ScannerMode.InboundCode)
        `assert label`(text = stringManager.getString(R.string.empty), status = LabelStatus.Neutral)
        `assert description`(text = stringManager.getString(R.string.empty), status = DescriptionStatus.Neutral)
    }

    @Test
    fun `scan inbound with error`() = runTest {
        val scanResult = ScanResultFactory.getScanResultBarcode(bogusInboundId)
        val response =
            TestFactory.getResponseError<Int>(code = 400, errorMessage = inboundErrorMessage)

        whenAccept(bogusInboundId).thenThrow(HttpException(response))

        viewModel.processScanResult(scanResult)

        `assert scanner`(mode = ScannerMode.InboundCode, overlayState = OverlayState.Failure)
        `assert label`(text = R.string.error, status = LabelStatus.Error)
        `assert description`(text = inboundErrorMessage, status = DescriptionStatus.Neutral)
    }

    @Test
    fun `scan pallet with error`() = runTest {
        val response =
            TestFactory.getResponseError<Int>(code = 418, errorMessage = palletErrorMessage)

        val inbound = TestFactory.getInbound()
        val scanInboundResult = ScanResultFactory.getScanResultBarcode(inbound.externalId)

        val invalidPalletId = getPalletId(inbound.id)
        val scanInvalidPalletResult = ScanResultFactory.getScanResultBarcode(invalidPalletId)

        val validPalletId = getPalletId(inbound.id, 1)
        val scanValidPalletResult = ScanResultFactory.getScanResultBarcode(validPalletId)

        whenAccept(inbound.externalId)
            .thenReturn(inbound.toAcceptance())
        `when`(
            networkInboundUseCases.linkToInbound(
                inbound.externalId,
                invalidPalletId,
                SortableType.XDOC_PALLET
            )
        )
            .thenThrow(HttpException(response))

        viewModel.processScanResult(scanInboundResult)
        viewModel.handleSelectInboundType(SortableType.XDOC_PALLET)

        `assert scanner`(mode = ScannerMode.PalletMode, overlayState = OverlayState.None)
        `assert label`(
            text = R.string.inbound_external_id,
            externalId = inbound.externalId,
            status = LabelStatus.Neutral
        )
        `assert description`(
            text = R.string.scan_pallet_external_id,
            status = DescriptionStatus.Neutral
        )

        viewModel.processScanResult(scanInvalidPalletResult)

        `assert scanner`(mode = ScannerMode.PalletMode, overlayState = OverlayState.Failure)
        `assert label`(text = R.string.error, status = LabelStatus.Error)
        `assert description`(text = palletErrorMessage, status = DescriptionStatus.Neutral)

        `when`(
            networkInboundUseCases.linkToInbound(
                inbound.externalId,
                validPalletId,
                SortableType.XDOC_PALLET
            )
        )
            .thenReturn(Unit)

        viewModel.processScanResult(scanValidPalletResult)
        viewModel.handleCharacteristics(createPalletCharacteristics(), soundPlayer)
    }

    @Test
    fun `scan and accept pallet`() = runTest {
        val inbound = TestFactory.getInbound()
        val palletId = getPalletId(inbound.id)
        val scanInboundResult = ScanResultFactory.getScanResultBarcode(inbound.externalId)
        val scanPalletResult = ScanResultFactory.getScanResultBarcode(palletId)

        whenAccept(inbound.externalId)
            .thenReturn(inbound.toAcceptance())
        `when`(
            networkInboundUseCases.linkToInbound(
                inbound.externalId,
                palletId,
                SortableType.XDOC_PALLET
            )
        )
            .thenReturn(Unit)

        viewModel.processScanResult(scanInboundResult)
        viewModel.handleSelectInboundType(SortableType.XDOC_PALLET)

        `assert scanner`(mode = ScannerMode.PalletMode, overlayState = OverlayState.None)
        `assert label`(
            text = R.string.inbound_external_id,
            externalId = inbound.externalId,
            status = LabelStatus.Neutral
        )
        `assert description`(
            text = R.string.scan_pallet_external_id,
            status = DescriptionStatus.Neutral
        )

        viewModel.processScanResult(scanPalletResult)

        `assert scanner`(mode = ScannerMode.DoNotScan, overlayState = OverlayState.None)
        `assert label`(text = stringManager.getString(R.string.empty), status = LabelStatus.Neutral)
        `assert description`(
            text = stringManager.getString(R.string.empty),
            status = DescriptionStatus.Neutral
        )

        viewModel.handleCharacteristics(createPalletCharacteristics(), soundPlayer)

        `assert scanner`(mode = ScannerMode.InboundCode, overlayState = OverlayState.Success)
        `assert label`(text = R.string.successfully, status = LabelStatus.Success)
        `assert description`(
            text = R.string.pallet_was_accepted,
            externalId = palletId,
            status = DescriptionStatus.Neutral
        )
    }

    @Test
    fun `scan and accept box`() = runTest {
        val inbound = TestFactory.getInbound()
        val boxId = getPalletId(inbound.id)
        val scanInboundResult = ScanResultFactory.getScanResultBarcode(inbound.externalId)
        val scanBoxResult = ScanResultFactory.getScanResultBarcode(boxId)

        whenAccept(inbound.externalId)
            .thenReturn(inbound.toAcceptance())
        `when`(
            networkInboundUseCases.linkToInbound(
                inbound.externalId,
                boxId,
                SortableType.XDOC_BOX
            )
        )
            .thenReturn(Unit)

        viewModel.processScanResult(scanInboundResult)
        viewModel.handleSelectInboundType(SortableType.XDOC_BOX)

        `assert scanner`(mode = ScannerMode.BoxMode, overlayState = OverlayState.None)
        `assert label`(
            text = R.string.inbound_external_id,
            externalId = inbound.externalId,
            status = LabelStatus.Neutral
        )
        `assert description`(
            text = R.string.scan_box_external_id,
            status = DescriptionStatus.Neutral
        )

        viewModel.processScanResult(scanBoxResult)

        `assert scanner`(mode = ScannerMode.InboundCode, overlayState = OverlayState.Success)
        `assert label`(text = R.string.successfully, status = LabelStatus.Success)
        `assert description`(
            text = R.string.box_was_accepted,
            externalId = boxId,
            status = DescriptionStatus.Neutral
        )
    }

    @Test
    fun `scan inbound and not scan pallet`() = runTest {
        val inbound = TestFactory.getInbound()
        val scanInboundResult = ScanResultFactory.getScanResultBarcode(inbound.externalId)

        whenAccept(inbound.externalId)
            .thenReturn(inbound.toAcceptance())

        viewModel.processScanResult(scanInboundResult)
        viewModel.handleSelectInboundType(SortableType.XDOC_PALLET)

        `assert scanner`(mode = ScannerMode.PalletMode, overlayState = OverlayState.None)
        `assert label`(
            text = R.string.inbound_external_id,
            externalId = inbound.externalId,
            status = LabelStatus.Neutral
        )
        `assert description`(
            text = R.string.scan_pallet_external_id,
            status = DescriptionStatus.Neutral
        )
    }

    @Test
    fun `scan inbound and not scan box`() = runTest {
        val inbound = TestFactory.getInbound()
        val scanInboundResult = ScanResultFactory.getScanResultBarcode(inbound.externalId)

        whenAccept(inbound.externalId)
            .thenReturn(inbound.toAcceptance())

        viewModel.processScanResult(scanInboundResult)
        viewModel.handleSelectInboundType(SortableType.XDOC_BOX)

        `assert scanner`(mode = ScannerMode.BoxMode, overlayState = OverlayState.None)
        `assert label`(
            text = R.string.inbound_external_id,
            externalId = inbound.externalId,
            status = LabelStatus.Neutral
        )
        `assert description`(
            text = R.string.scan_box_external_id,
            status = DescriptionStatus.Neutral
        )
    }

    @Test
    fun `scan and accept box twice`() = runTest {
        val inbound = TestFactory.getInbound()
        val boxId = getPalletId(inbound.id)
        val secondBoxId = getPalletId(inbound.id, 1)
        val scanInboundResult = ScanResultFactory.getScanResultBarcode(inbound.externalId)
        val scanBoxResult = ScanResultFactory.getScanResultBarcode(boxId)
        val scanBoxResult2 = ScanResultFactory.getScanResultBarcode(secondBoxId)

        whenAccept(inbound.externalId)
            .thenReturn(inbound.toAcceptance())
        `when`(
            networkInboundUseCases.linkToInbound(
                inbound.externalId,
                boxId,
                SortableType.XDOC_BOX
            )
        )
            .thenReturn(Unit)

        viewModel.processScanResult(scanInboundResult)
        viewModel.handleSelectInboundType(SortableType.XDOC_BOX)

        `assert scanner`(mode = ScannerMode.BoxMode, overlayState = OverlayState.None)
        `assert label`(
            text = R.string.inbound_external_id,
            externalId = inbound.externalId,
            status = LabelStatus.Neutral
        )
        `assert description`(
            text = R.string.scan_box_external_id,
            status = DescriptionStatus.Neutral
        )

        viewModel.processScanResult(scanBoxResult)

        `assert scanner`(mode = ScannerMode.InboundCode, overlayState = OverlayState.Success)
        `assert label`(text = R.string.successfully, status = LabelStatus.Success)
        `assert description`(
            text = R.string.box_was_accepted,
            externalId = boxId,
            status = DescriptionStatus.Neutral
        )

        val inboundAfter = inbound.copy(sortableType = SortableType.XDOC_BOX)

        whenAccept(inboundAfter.externalId)
            .thenReturn(inboundAfter.toAcceptance())

        viewModel.processScanResult(scanInboundResult)

        `assert scanner`(mode = ScannerMode.BoxMode, overlayState = OverlayState.None)
        `assert label`(
            text = R.string.inbound_external_id,
            externalId = inboundAfter.externalId,
            status = LabelStatus.Neutral
        )
        `assert description`(
            text = R.string.scan_box_external_id,
            status = DescriptionStatus.Neutral
        )

        viewModel.processScanResult(scanBoxResult2)

        `assert scanner`(mode = ScannerMode.InboundCode, overlayState = OverlayState.Success)
        `assert label`(text = R.string.successfully, status = LabelStatus.Success)
        `assert description`(
            text = R.string.box_was_accepted,
            externalId = secondBoxId,
            status = DescriptionStatus.Neutral
        )
    }

    @Test
    fun `scan and accept pallet twice`() = runTest {
        val inbound = TestFactory.getInbound()
        val palletId = getPalletId(inbound.id)
        val secondBoxId = getPalletId(inbound.id, 1)
        val scanInboundResult = ScanResultFactory.getScanResultBarcode(inbound.externalId)
        val scanPalletResult = ScanResultFactory.getScanResultBarcode(palletId)
        val scanPalletResult2 = ScanResultFactory.getScanResultBarcode(secondBoxId)

        whenAccept(inbound.externalId)
            .thenReturn(inbound.toAcceptance())
        `when`(
            networkInboundUseCases.linkToInbound(
                inbound.externalId,
                palletId,
                SortableType.XDOC_PALLET
            )
        )
            .thenReturn(Unit)

        viewModel.processScanResult(scanInboundResult)
        viewModel.handleSelectInboundType(SortableType.XDOC_PALLET)

        `assert scanner`(mode = ScannerMode.PalletMode, overlayState = OverlayState.None)
        `assert label`(
            text = R.string.inbound_external_id,
            externalId = inbound.externalId,
            status = LabelStatus.Neutral
        )
        `assert description`(
            text = R.string.scan_pallet_external_id,
            status = DescriptionStatus.Neutral
        )

        viewModel.processScanResult(scanPalletResult)
        viewModel.handleCharacteristics(createPalletCharacteristics(), soundPlayer)

        `assert scanner`(mode = ScannerMode.InboundCode, overlayState = OverlayState.Success)
        `assert label`(text = R.string.successfully, status = LabelStatus.Success)
        `assert description`(
            text = R.string.pallet_was_accepted,
            externalId = palletId,
            status = DescriptionStatus.Neutral
        )

        val inboundAfter = inbound.copy(sortableType = SortableType.XDOC_PALLET)

        whenAccept(inboundAfter.externalId)
            .thenReturn(inboundAfter.toAcceptance())

        viewModel.processScanResult(scanInboundResult)

        `assert scanner`(mode = ScannerMode.PalletMode, overlayState = OverlayState.None)
        `assert label`(
            text = R.string.inbound_external_id,
            externalId = inboundAfter.externalId,
            status = LabelStatus.Neutral
        )
        `assert description`(
            text = R.string.scan_pallet_external_id,
            status = DescriptionStatus.Neutral
        )

        viewModel.processScanResult(scanPalletResult2)
        viewModel.handleCharacteristics(createPalletCharacteristics(), soundPlayer)

        `assert scanner`(mode = ScannerMode.InboundCode, overlayState = OverlayState.Success)
        `assert label`(text = R.string.successfully, status = LabelStatus.Success)
        `assert description`(
            text = R.string.pallet_was_accepted,
            externalId = secondBoxId,
            status = DescriptionStatus.Neutral
        )
    }

    @Test
    fun `scan and accept multiple destination pallet`() = runTest {
        val firstInbound = TestFactory.getInbound(destination = "first")
        val secondInbound = TestFactory.getInbound(destination = "second")
        val palletId = getPalletId(secondInbound.id)
        val scanInboundResult = ScanResultFactory.getScanResultBarcode(firstInbound.externalId)
        val scanPalletResult = ScanResultFactory.getScanResultBarcode(palletId)

        whenAccept(firstInbound.externalId)
            .thenReturn(listOf(firstInbound, secondInbound).toAcceptance())

        viewModel.processScanResult(scanInboundResult)

        `when`(
            networkInboundUseCases.linkToInbound(
                secondInbound.externalId,
                palletId,
                SortableType.XDOC_PALLET
            )
        )
            .thenReturn(Unit)

        viewModel.handleSelectDestination(secondInbound.destination!!)
        viewModel.handleSelectInboundType(SortableType.XDOC_PALLET)

        `assert scanner`(mode = ScannerMode.PalletMode, overlayState = OverlayState.None)
        `assert label`(
            text = R.string.inbound_external_id,
            externalId = secondInbound.externalId,
            status = LabelStatus.Neutral
        )
        `assert description`(
            text = R.string.scan_pallet_external_id,
            status = DescriptionStatus.Neutral
        )

        viewModel.processScanResult(scanPalletResult)
        viewModel.handleCharacteristics(createPalletCharacteristics(), soundPlayer)

        `assert scanner`(mode = ScannerMode.InboundCode, overlayState = OverlayState.Success)
        `assert label`(text = R.string.successfully, status = LabelStatus.Success)
        `assert description`(
            text = R.string.pallet_was_accepted,
            externalId = palletId,
            status = DescriptionStatus.Neutral
        )
    }

    @Test
    fun `scan and accept multiple destination pallet twice`() = runTest {
        val firstInbound = TestFactory.getInbound(destination = "first")
        val secondInbound = TestFactory.getInbound(destination = "second")
        val firstPalletId = getPalletId(secondInbound.id)
        val secondPalletId = getPalletId(secondInbound.id, 1)
        val scanInboundResult = ScanResultFactory.getScanResultBarcode(firstInbound.externalId)
        val scanPalletResult = ScanResultFactory.getScanResultBarcode(firstPalletId)
        val scanPalletResult2 = ScanResultFactory.getScanResultBarcode(secondPalletId)

        `when`(
            networkInboundUseCases.acceptInbound(
                firstInbound.externalId,
                Inbound.Type.XDOC_TRANSIT
            )
        ).thenAnswer {
            listOf(firstInbound, secondInbound).toAcceptance()
        }

        viewModel.processScanResult(scanInboundResult)
        viewModel.handleSelectDestination(secondInbound.destination!!)
        viewModel.handleSelectInboundType(SortableType.XDOC_PALLET)

        `assert scanner`(mode = ScannerMode.PalletMode, overlayState = OverlayState.None)
        `assert label`(
            text = R.string.inbound_external_id,
            externalId = secondInbound.externalId,
            status = LabelStatus.Neutral
        )
        `assert description`(
            text = R.string.scan_pallet_external_id,
            status = DescriptionStatus.Neutral
        )

        `when`(
            networkInboundUseCases.linkToInbound(
                secondInbound.externalId,
                firstPalletId,
                SortableType.XDOC_PALLET
            )
        )
            .thenReturn(Unit)

        viewModel.processScanResult(scanPalletResult)
        viewModel.handleCharacteristics(createPalletCharacteristics(), soundPlayer)

        `assert scanner`(mode = ScannerMode.InboundCode, overlayState = OverlayState.Success)
        `assert label`(text = R.string.successfully, status = LabelStatus.Success)
        `assert description`(
            text = R.string.pallet_was_accepted,
            externalId = firstPalletId,
            status = DescriptionStatus.Neutral
        )

        `when`(
            networkInboundUseCases.acceptInbound(
                firstInbound.externalId,
                Inbound.Type.XDOC_TRANSIT
            )
        ).thenAnswer {
            secondInbound.copy(sortableType = SortableType.XDOC_PALLET).toAcceptance()
        }
        viewModel.processScanResult(scanInboundResult)

        `assert scanner`(mode = ScannerMode.PalletMode, overlayState = OverlayState.None)
        `assert label`(
            text = R.string.inbound_external_id,
            externalId = secondInbound.externalId,
            status = LabelStatus.Neutral
        )
        `assert description`(
            text = R.string.scan_pallet_external_id,
            status = DescriptionStatus.Neutral
        )

        viewModel.processScanResult(scanPalletResult2)

        viewModel.handleCharacteristics(createPalletCharacteristics(), soundPlayer)

        `assert scanner`(mode = ScannerMode.InboundCode, overlayState = OverlayState.Success)
        `assert label`(text = R.string.successfully, status = LabelStatus.Success)
        `assert description`(
            text = R.string.pallet_was_accepted,
            externalId = secondPalletId,
            status = DescriptionStatus.Neutral
        )
    }

    @Test
    fun `scan and accept pallet then fail vgh enter then success vgh enter`() = runTest {
        val inbound = TestFactory.getInbound()
        val palletId = getPalletId(inbound.id)
        val scanInboundResult = ScanResultFactory.getScanResultBarcode(inbound.externalId)
        val scanPalletResult = ScanResultFactory.getScanResultBarcode(palletId)
        val palletCharacteristics = createPalletCharacteristics()

        whenAccept(inbound.externalId).thenReturn(inbound.toAcceptance())
        `when`(
            networkInboundUseCases.linkToInbound(
                inbound.externalId,
                palletId,
                SortableType.XDOC_PALLET
            )
        )
            .thenReturn(Unit)

        viewModel.processScanResult(scanInboundResult)
        viewModel.handleSelectInboundType(SortableType.XDOC_PALLET)

        `assert scanner`(mode = ScannerMode.PalletMode, overlayState = OverlayState.None)
        `assert label`(
            text = R.string.inbound_external_id,
            externalId = inbound.externalId,
            status = LabelStatus.Neutral
        )
        `assert description`(
            text = R.string.scan_pallet_external_id,
            status = DescriptionStatus.Neutral
        )

        viewModel.processScanResult(scanPalletResult)

        `when`(
            networkInboundUseCases.saveVGH(
                palletId,
                SortableType.XDOC_PALLET,
                palletCharacteristics
            )
        )
            .thenThrow(IllegalStateException())
            .thenReturn(Unit)
        viewModel.handleCharacteristics(palletCharacteristics, soundPlayer)

        assertThat(
            viewModel.dialogEvents.getOrAwaitValue().get()
        ).isInstanceOf(DialogEvent.OpenEnterVgh::class.java)
        assertThat(
            viewModel.toastsMessages.getOrAwaitValue().get()
        ).isEqualTo(stringManager.getString(R.string.error_fallback_unknown))

        `assert scanner`(mode = ScannerMode.DoNotScan, overlayState = OverlayState.None)
        `assert label`(text = stringManager.getString(R.string.empty), status = LabelStatus.Neutral)
        `assert description`(text = stringManager.getString(R.string.empty), status = DescriptionStatus.Neutral)

        viewModel.handleCharacteristics(palletCharacteristics, soundPlayer)

        `assert scanner`(mode = ScannerMode.InboundCode, overlayState = OverlayState.Success)
        `assert label`(text = R.string.successfully, status = LabelStatus.Success)
        `assert description`(
            text = R.string.pallet_was_accepted,
            externalId = palletId,
            status = DescriptionStatus.Neutral
        )
    }

    private fun getPalletId(inboundId: Long, count: Long = 0): ExternalId =
        ExternalId("XDOC-$inboundId-$count")

    private fun Inbound.toAcceptance() = AcceptanceInbound.Single(this)

    private fun List<Inbound>.toAcceptance() = AcceptanceInbound.Multiple(this)

    private suspend fun whenAccept(
        externalId: ExternalId,
        type: Inbound.Type = Inbound.Type.XDOC_TRANSIT,
    ) = `when`(networkInboundUseCases.acceptInbound(externalId, type = type))
}
