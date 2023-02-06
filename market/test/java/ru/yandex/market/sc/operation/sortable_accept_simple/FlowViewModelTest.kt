package ru.yandex.market.sc.operation.sortable_accept_simple

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.*
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.data.acceptance.InitialAcceptanceResponse.StatusCode
import ru.yandex.market.sc.core.data.operation.operations.Operation
import ru.yandex.market.sc.core.data.operation.operations.SortableAcceptSimple
import ru.yandex.market.sc.core.data.operation.sortable_accept_simple.SortableAcceptSimpleConfig
import ru.yandex.market.sc.core.data.operation.sortable_accept_simple.SortableAcceptSimpleContext
import ru.yandex.market.sc.core.data.warehouse.Warehouse
import ru.yandex.market.sc.core.data.zone.Flow
import ru.yandex.market.sc.core.data.zone.FlowName
import ru.yandex.market.sc.core.data.stage.Stage
import ru.yandex.market.sc.core.network.domain.NetworkAcceptanceUseCases
import ru.yandex.market.sc.core.resources.StringManager
import ru.yandex.market.sc.core.data.zone.Process
import ru.yandex.market.sc.core.network.domain.NetworkFlowUseCases
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.operation.sortable_accept_simple.analytics.AppMetrica
import ru.yandex.market.sc.operation.sortable_accept_simple.data.Action
import ru.yandex.market.sc.operation.sortable_accept_simple.data.flow.SortableAcceptSimpleFlowOperationData
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.MockitoHelper
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class FlowViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var stringManager: StringManager

    @Mock
    private lateinit var appMetrica: AppMetrica

    @Mock
    private lateinit var networkAcceptanceUseCases: NetworkAcceptanceUseCases

    @Mock
    private lateinit var networkFlowUseCases: NetworkFlowUseCases

    private lateinit var viewModel: FlowViewModel
    private val placeExternalId = ExternalId("placeIdTest")
    private val acceptDateTime = "06.05.2022 15:56"
    private val userAcceptedBy = "мерч"
    private val placesList = mutableListOf(ExternalId("2323423"), ExternalId("239582359"))
    private val rightWarehouseName = "rightWarehouse"
    private val wrongWarehouseName = "wrongWarehouse"
    private val ticketId = "ticketId"
    private val rightWarehouse = Warehouse(
        id = 1,
        name = rightWarehouseName,
        type = Warehouse.Type.UNKNOWN,
    )
    private val wrongWarehouse = Warehouse(
        id = 2,
        name = wrongWarehouseName,
        type = Warehouse.Type.UNKNOWN,
    )
    private val flowWithWarehouse = Flow(
        systemName = FlowName("MERCHANT_INITIAL_ACCEPTANCE"),
        displayName = "",
        currentProcess = Process("process"),
    )

    private val config = SortableAcceptSimpleConfig(
        validateMerchant = true
    )
    private val context = SortableAcceptSimpleContext(
        ticketId = null,
        externalIds = listOf(),
        warehouse = null
    )
    private val operation = SortableAcceptSimple(
        systemName = Operation.Name.SORTABLE_ACCEPT_SIMPLE,
        displayName = "Первичная приёмка",
        context = context,
        config = config
    )

    private val state
        get() = viewModel.state.getOrAwaitValue()

    @Before
    fun setUp() {
        viewModel = FlowViewModel(
            networkAcceptanceUseCases,
            networkFlowUseCases,
            stringManager,
            appMetrica,
        )
        val flowOperationData = SortableAcceptSimpleFlowOperationData(
            operation = operation,
            flow = flowWithWarehouse
        )
        viewModel.addApiConfig(flowOperationData)
        viewModel.init(flowOperationData)
    }

    @Test
    fun `success scan with status NEED_ANOTHER_BARCODE with two barcodes`() = runTest {
        val firstPickResponse = TestFactory.getInitialAcceptanceResponse(
            status = StatusCode.NEED_ANOTHER_BARCODE
        )
        val secondPickResponse = TestFactory.getInitialAcceptanceResponse(
            placeExternalId = placeExternalId,
            status = StatusCode.OK
        )
        val scanFirstBarcode = ScanResultFactory.getScanResultBarcode("barcode-1")
        val scanSecondBarcode = ScanResultFactory.getScanResultBarcode("barcode-2")

        `when`(
            networkAcceptanceUseCases.initialAcceptance(
                anyList(),
                any(),
                MockitoHelper.anyObject(),
                any()
            )
        )
            .thenReturn(
                firstPickResponse,
                secondPickResponse
            )

        viewModel.preAccept(scanFirstBarcode)

        Truth.assertThat(state.warehouse).isNull()
        Truth.assertThat(state.scannedBarcode).isEqualTo(scanFirstBarcode.value)

        viewModel.preAccept(scanSecondBarcode)

        Truth.assertThat(state.warehouse).isNull()
        Truth.assertThat(state.isLoading).isEqualTo(false)
        Truth.assertThat(state.externalIds).isEqualTo(listOf(placeExternalId))
    }

    @Test
    fun `success acceptance with select`() = runTest {
        val acceptanceResponse = TestFactory.getInitialAcceptanceResponse(
            placeExternalId = placeExternalId,
            status = StatusCode.OK
        )

        `when`(
            networkAcceptanceUseCases.initialAcceptance(
                anyList(),
                any(),
                MockitoHelper.anyObject(),
                anyBoolean(),
            )
        )
            .thenReturn(
                acceptanceResponse,
            )

        viewModel.preAcceptWithSelection(placeExternalId, needAcceptCancelled = true) {}

        Truth.assertThat(state.warehouse).isNull()
        Truth.assertThat(state.isLoading).isEqualTo(false)
        Truth.assertThat(state.externalIds).isEqualTo(listOf(placeExternalId))
    }

    @Test
    fun `success scan check current warehouse name`() = runTest {
        val rightWarehouseResponse = TestFactory.getInitialAcceptanceResponse(
            status = StatusCode.OK,
            warehouse = rightWarehouse
        )
        val wrongWarehouseResponse = TestFactory.getInitialAcceptanceResponse(
            status = StatusCode.WRONG_WAREHOUSE,
            warehouse = wrongWarehouse
        )
        val scanFirstBarcode = ScanResultFactory.getScanResultBarcode("barcode-1")
        val scanSecondBarcode = ScanResultFactory.getScanResultBarcode("barcode-2")

        `when`(
            networkAcceptanceUseCases.initialAcceptance(
                barcodeList = anyList(),
                warehouseId = any(),
                apiConfig = MockitoHelper.anyObject(),
                needAcceptCancelled = any()
            )
        )
            .thenReturn(
                rightWarehouseResponse,
                wrongWarehouseResponse
            )

        viewModel.preAccept(scanFirstBarcode)

        Truth.assertThat(viewModel.warehouseName).isEqualTo(rightWarehouseName)

        viewModel.preAccept(scanSecondBarcode)

        Truth.assertThat(viewModel.warehouseName).isEqualTo(rightWarehouseName)
        Truth.assertThat(state.wrongWarehouse?.name).isEqualTo(wrongWarehouseName)
    }

    @Test
    fun `is revert accept method called`() = runTest {
        viewModel.revertAccept { }
        Mockito.verify(networkAcceptanceUseCases, Mockito.times(1))
            .revertAccept(MockitoHelper.anyObject())
        Truth.assertThat(state.isLoading).isEqualTo(false)
    }

    @Test
    fun `is finish accept method called`() = runTest {
        viewModel.finishAccept { }
        Mockito.verify(networkAcceptanceUseCases, Mockito.times(1))
            .finishAccept(MockitoHelper.anyObject(), MockitoHelper.anyObject())
        Truth.assertThat(state.isLoading).isEqualTo(false)
    }

    @Test
    fun `action SetErrorText`() = runTest {
        val errorText = "error text"
        viewModel.changeState(Action.SetErrorText(errorText))
        Truth.assertThat(state.errorText).isEqualTo(errorText)
        Truth.assertThat(state.isLoading).isEqualTo(false)
    }

    @Test
    fun `action ResetScannedBarcode`() = runTest {
        viewModel.changeState(Action.ResetScannedBarcode)
        Truth.assertThat(state.scannedBarcode).isNull()
    }

    @Test
    fun `action Loading`() = runTest {
        viewModel.changeState(Action.Loading)
        Truth.assertThat(state.isLoading).isEqualTo(true)
    }

    @Test
    fun `action PreAccept status OK`() = runTest {
        val acceptanceResponse = TestFactory.getInitialAcceptanceResponse(
            placeExternalId = placeExternalId,
            status = StatusCode.OK,
            warehouse = rightWarehouse
        )
        viewModel.changeState(Action.PreAccept(acceptanceResponse, placeExternalId.value))
        Truth.assertThat(state.externalIds.contains(placeExternalId)).isEqualTo(true)
        Truth.assertThat(state.warehouse).isEqualTo(acceptanceResponse.warehouse)
        Truth.assertThat(state.statusCode).isEqualTo(StatusCode.OK)
    }

    @Test
    fun `action PreAccept status CAN_BE_RETURNED`() = runTest {
        val acceptanceResponse = TestFactory.getInitialAcceptanceResponse(
            placeExternalId = placeExternalId,
            status = StatusCode.CAN_BE_RETURNED,
            stage = Stage(
                systemName = "systemName",
                displayName = "Отмена",
            ),
            warehouse = rightWarehouse
        )
        viewModel.changeState(Action.PreAccept(acceptanceResponse, placeExternalId.value))
        Truth.assertThat(state.warehouse).isEqualTo(acceptanceResponse.warehouse)
        Truth.assertThat(state.placeExternalId).isEqualTo(acceptanceResponse.placeExternalId)
        Truth.assertThat(state.statusCode).isEqualTo(StatusCode.CAN_BE_RETURNED)
        Truth.assertThat(state.stageDisplayName).isEqualTo(acceptanceResponse.stage?.displayName)
    }

    @Test
    fun `action PreAccept status OK withoutWarehouse`() = runTest {
        val acceptanceResponse = TestFactory.getInitialAcceptanceResponse(
            placeExternalId = placeExternalId,
            status = StatusCode.OK,
            warehouse = rightWarehouse
        )
        val withWarehouse = false
        viewModel.changeState(
            Action.SetOperationData(
                context, config.copy(
                    validateMerchant = withWarehouse
                )
            )
        )
        viewModel.changeState(Action.PreAccept(acceptanceResponse, placeExternalId.value))
        Truth.assertThat(state.warehouse).isEqualTo(null)
    }

    @Test
    fun `action PreAccept status NEED_ANOTHER_BARCODE`() = runTest {
        val acceptanceResponse = TestFactory.getInitialAcceptanceResponse(
            placeExternalId = placeExternalId,
            status = StatusCode.NEED_ANOTHER_BARCODE,
            warehouse = rightWarehouse
        )
        viewModel.changeState(Action.PreAccept(acceptanceResponse, placeExternalId.value))
        Truth.assertThat(state.scannedBarcode).isEqualTo(placeExternalId.value)
        Truth.assertThat(state.statusCode).isEqualTo(StatusCode.NEED_ANOTHER_BARCODE)
    }

    @Test
    fun `action PreAccept status WRONG_WAREHOUSE`() = runTest {
        val acceptanceResponse = TestFactory.getInitialAcceptanceResponse(
            placeExternalId = placeExternalId,
            status = StatusCode.WRONG_WAREHOUSE,
            warehouse = wrongWarehouse
        )
        viewModel.changeState(Action.PreAccept(acceptanceResponse, placeExternalId.value))
        Truth.assertThat(state.wrongWarehouse).isEqualTo(acceptanceResponse.warehouse)
        Truth.assertThat(state.statusCode).isEqualTo(StatusCode.WRONG_WAREHOUSE)
        Truth.assertThat(state.placeExternalId).isEqualTo(placeExternalId)
    }

    @Test
    fun `action PreAccept status ALREADY_ACCEPTED`() = runTest {
        val acceptanceResponse = TestFactory.getInitialAcceptanceResponse(
            placeExternalId = placeExternalId,
            status = StatusCode.ALREADY_ACCEPTED,
            acceptDateTime = acceptDateTime,
            userAcceptedBy = userAcceptedBy,
        )
        viewModel.changeState(Action.PreAccept(acceptanceResponse, placeExternalId.value))
        Truth.assertThat(state.statusCode).isEqualTo(StatusCode.ALREADY_ACCEPTED)
        Truth.assertThat(state.placeExternalId).isEqualTo(placeExternalId)
        Truth.assertThat(state.acceptDateTime).isEqualTo(acceptanceResponse.acceptDateTime)
        Truth.assertThat(state.userAcceptedBy).isEqualTo(acceptanceResponse.userAcceptedBy)
    }

    @Test
    fun `action SetWithWarehouseStatus`() = runTest {
        Truth.assertThat(state.withWarehouse).isEqualTo(true)
        val withWarehouse = false
        viewModel.changeState(
            Action.SetOperationData(
                context, config.copy(
                    validateMerchant = withWarehouse
                )
            )
        )
        Truth.assertThat(state.withWarehouse).isEqualTo(false)
    }

    @Test
    fun `action SetOperationData with context and config`() = runTest {
        Truth.assertThat(state.withWarehouse).isEqualTo(true)
        Truth.assertThat(state.config?.validateMerchant).isEqualTo(true)
        Truth.assertThat(state.context?.externalIds).isEqualTo(mutableListOf<ExternalId>())
        Truth.assertThat(state.context?.ticketId).isEqualTo(null)
        Truth.assertThat(state.context?.warehouse).isEqualTo(null)
        val withWarehouse = false
        val config = config.copy(
            validateMerchant = withWarehouse,
        )
        val context = context.copy(
            externalIds = placesList,
            warehouse = rightWarehouse,
            ticketId = ticketId
        )
        viewModel.changeState(Action.SetOperationData(context, config))
        Truth.assertThat(state.withWarehouse).isEqualTo(false)
        Truth.assertThat(state.config).isEqualTo(config)
        Truth.assertThat(state.ticketId).isEqualTo(ticketId)
        Truth.assertThat(state.context).isEqualTo(context)
        Truth.assertThat(state.warehouse).isEqualTo(rightWarehouse)
        Truth.assertThat(state.externalIds).isEqualTo(placesList)
    }
}
