package ru.yandex.market.sc.operation.lot_accept

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.operation.lot_accept.data.flow.LotAcceptFlowOperationData
import ru.yandex.market.sc.core.data.lot.LotAcceptanceResponse.StatusCode
import ru.yandex.market.sc.core.data.operation.lot_accept.LotAcceptContext
import ru.yandex.market.sc.core.data.operation.lot_accept.LotData
import ru.yandex.market.sc.core.data.operation.operations.LotAccept
import ru.yandex.market.sc.core.data.operation.operations.Operation
import ru.yandex.market.sc.core.data.zone.Flow
import ru.yandex.market.sc.core.data.zone.FlowName
import ru.yandex.market.sc.core.data.zone.Process
import ru.yandex.market.sc.core.network.domain.NetworkAcceptanceUseCases
import ru.yandex.market.sc.core.network.domain.NetworkFlowUseCases
import ru.yandex.market.sc.core.resources.StringManager
import ru.yandex.market.sc.core.ui.vo.LotExternalIdVo
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.operation.lot_accept.analytics.AppMetrica
import ru.yandex.market.sc.operation.lot_accept.data.Action
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.MockitoHelper
import ru.yandex.market.test.utils.getOrAwaitValue


@RunWith(MockitoJUnitRunner.StrictStubs::class)
class LotAcceptViewModelTest {
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

    private lateinit var viewModel: LotAcceptViewModel
    private val lotExternalId = ExternalId("lotExternalId")
    private val lotExternalId2 = ExternalId("lotExternalId2")
    private val lotExternalId3 = ExternalId("lotExternalId3")
    private val inboundExternalId = ExternalId("inboundExternalId")
    private val acceptDateTime = "06.05.2022 15:56"
    private val inboundDate = "2022-05-05"
    private val userAcceptedBy = "мерч"
    private val placeCount = 10
    private val placeCount2 = 15
    private val placeCount3 = 7
    private val flow = Flow(
        systemName = FlowName("SOME_FLOW"),
        displayName = "some_flow",
        currentProcess = Process("SOME_PROCESS"),
    )

    private val context = LotAcceptContext(
        lotList = listOf()
    )
    private val operation = LotAccept(
        systemName = Operation.Name.LOT_ACCEPT,
        displayName = "Приёмка лота",
        context = context,
    )
    private val lotList = listOf(
        LotData(
            externalId = lotExternalId,
            inboundExternalId = inboundExternalId,
            placeCount = placeCount,
            isCrossDock = false,
        ),
        LotData(
            externalId = lotExternalId2,
            inboundExternalId = null,
            placeCount = placeCount2,
            isCrossDock = false,
        ),
        LotData(
            externalId = lotExternalId3,
            inboundExternalId = inboundExternalId,
            placeCount = placeCount3,
            isCrossDock = false,
        ),
    )

    private val state
        get() = viewModel.state.getOrAwaitValue()

    @Before
    fun setUp() {
        viewModel =
            LotAcceptViewModel(
                networkAcceptanceUseCases,
                networkFlowUseCases,
                stringManager,
                appMetrica
            )
        val flowOperationData = LotAcceptFlowOperationData(
            operation = operation,
            flow = flow
        )
        viewModel.addApiConfig(flowOperationData)
        viewModel.init(flowOperationData)
    }

    @Test
    fun `status has changed after scan`() = runTest {
        val response = TestFactory.getLotAcceptanceResponse(
            status = StatusCode.OK,
            lotExternalId = lotExternalId,
            placeCount = placeCount
        )
        val scanQr = ScanResultFactory.getScanResultQR(lotExternalId)

        Mockito.`when`(
            networkAcceptanceUseCases.lotPreAccept(
                lotExternalId,
                viewModel.apiConfig
            )
        )
            .thenReturn(
                response,
            )

        Truth.assertThat(state.statusCode).isNull()
        viewModel.preAccept(scanQr)
        Truth.assertThat(state.statusCode).isEqualTo(StatusCode.OK)
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
    fun `action Loading`() = runTest {
        viewModel.changeState(Action.Loading)
        Truth.assertThat(state.isLoading).isEqualTo(true)
    }

    @Test
    fun `action PreAccept status OK`() = runTest {
        val acceptanceResponse = TestFactory.getLotAcceptanceResponse(
            status = StatusCode.OK,
            lotExternalId = lotExternalId,
            inboundExternalId = inboundExternalId,
            placeCount = placeCount,
            inboundDate = inboundDate,
        )
        viewModel.changeState(Action.PreAccept(acceptanceResponse))

        Truth.assertThat(state.statusCode).isEqualTo(StatusCode.OK)
        Truth.assertThat(state.lots).isEqualTo(
            listOf(
                LotData(
                    externalId = acceptanceResponse.lotExternalId,
                    inboundExternalId = acceptanceResponse.inboundExternalId,
                    placeCount = acceptanceResponse.placeCount,
                    isCrossDock = false,
                )
            )
        )
        Truth.assertThat(state.scFrom).isEqualTo(acceptanceResponse.scFrom)
        Truth.assertThat(state.scTo).isEqualTo(acceptanceResponse.scTo)
        Truth.assertThat(state.lotStatus).isEqualTo(acceptanceResponse.lotStatus)
        Truth.assertThat(state.inboundExternalId).isEqualTo(acceptanceResponse.inboundExternalId)
        Truth.assertThat(state.inboundDate).isEqualTo(acceptanceResponse.inboundDate)
        Truth.assertThat(state.externalId).isEqualTo(acceptanceResponse.lotExternalId)
    }

    @Test
    fun `action PreAccept status ALREADY_ACCEPTED`() = runTest {
        val acceptanceResponse = TestFactory.getLotAcceptanceResponse(
            status = StatusCode.ALREADY_ACCEPTED,
            lotExternalId = lotExternalId,
            userAcceptedBy = userAcceptedBy,
            acceptDateTime = acceptDateTime,
            placeCount = 10
        )
        viewModel.changeState(Action.PreAccept(acceptanceResponse))
        Truth.assertThat(state.externalId).isEqualTo(acceptanceResponse.lotExternalId)
        Truth.assertThat(state.statusCode).isEqualTo(StatusCode.ALREADY_ACCEPTED)
        Truth.assertThat(state.acceptDateTime).isEqualTo(acceptanceResponse.acceptDateTime)
        Truth.assertThat(state.userAcceptedBy).isEqualTo(acceptanceResponse.userAcceptedBy)
    }

    @Test
    fun `action SetOperationData with context`() = runTest {
        Truth.assertThat(state.context?.lotList).isEqualTo(listOf<LotData>())
        val context = context.copy(
            lotList = lotList
        )
        viewModel.changeState(Action.SetOperationData(context))
        Truth.assertThat(state.context).isEqualTo(context)
        Truth.assertThat(state.lots).isEqualTo(lotList.toMutableList())
    }

    @Test
    fun `properties calculation - placesCount`() = runTest {
        Truth.assertThat(state.placesCount).isEqualTo(0)
        val sum = placeCount + placeCount2 + placeCount3
        val context = context.copy(
            lotList = lotList
        )
        viewModel.changeState(Action.SetOperationData(context))
        Truth.assertThat(state.placesCount).isEqualTo(sum)
    }

    @Test
    fun `properties calculation - inboundsAmount`() = runTest {
        Truth.assertThat(state.inboundsAmount).isEqualTo(0)
        val context = context.copy(
            lotList = lotList
        )
        viewModel.changeState(Action.SetOperationData(context))
        Truth.assertThat(state.inboundsAmount).isEqualTo(1)
    }

    @Test
    fun `properties calculation - inboundLotsMap`() = runTest {
        Truth.assertThat(state.inboundLotsMap.size).isEqualTo(0)
        val context = context.copy(
            lotList = lotList
        )
        viewModel.changeState(Action.SetOperationData(context))
        Truth.assertThat(state.inboundLotsMap).isEqualTo(
            mapOf(
                inboundExternalId to listOf(
                    LotExternalIdVo(
                        value = lotExternalId.value,
                        isCrossDockIconVisible = false,
                    ),
                    LotExternalIdVo(
                        value = lotExternalId3.value,
                        isCrossDockIconVisible = false,
                    ),
                ),
                null to listOf(
                    LotExternalIdVo(
                        value = lotExternalId2.value,
                        isCrossDockIconVisible = false,
                    ),
                ),
            )
        )
    }
}
