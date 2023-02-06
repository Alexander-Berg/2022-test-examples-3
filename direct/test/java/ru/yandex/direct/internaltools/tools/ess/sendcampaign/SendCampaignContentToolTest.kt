package ru.yandex.direct.internaltools.tools.ess.sendcampaign

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcProperty
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.testing.db.TestPpcPropertiesSupport
import ru.yandex.direct.ess.client.EssClient
import ru.yandex.direct.ess.common.models.BaseLogicObject
import ru.yandex.direct.internaltools.tools.ess.sendcampaign.model.SendCampaignParam
import ru.yandex.direct.internaltools.tools.ess.sendcampaign.model.SendCampaignResultRow.Companion.createResults
import ru.yandex.direct.internaltools.tools.ess.sendcampaign.service.RateLimitingService
import ru.yandex.direct.validation.builder.Constraint
import ru.yandex.direct.validation.defect.NumberDefects
import ru.yandex.direct.validation.defect.StringDefects
import ru.yandex.direct.validation.result.Defect

class SendCampaignContentToolTest {

    val essObjectsSent: MutableList<out BaseLogicObject> = mutableListOf()

    val essClient = mock<EssClient> { mock ->
        whenever(mock.addLogicObjectsForProcessor(any(), any(), any<List<BaseLogicObject>>(), any())).then {
            essObjectsSent.addAll(it.getArgument(2))
        }
    }

    val limitProperty = mock<PpcProperty<Int>>() {
        whenever(mock.getOrDefault(any()))
            .doReturn(10000)
    }

    val stateProperty = mock<PpcProperty<String>>() {
        whenever(mock.getOrDefault(any()))
            .doReturn("")
    }


    val ppcPropertiesSupport = mock<PpcPropertiesSupport> {mock ->
        whenever(mock.get(eq(PpcPropertyNames.SEND_CAMPAIGNS_TO_ESS_TRANSPORT_OBJECTS_LIMIT)))
            .doReturn(limitProperty)
        whenever(mock.get(eq(PpcPropertyNames.SEND_CAMPAIGNS_TO_ESS_TRANSPORT_STATE)))
            .doReturn(stateProperty)
    }

    val service = mock<SendCampaignContentService> { mock ->
        on(mock.getContent(any(), any(), anyOrNull())) doAnswer { serviceMock ->
            val cids = serviceMock.getArgument<Collection<Long>>(0)
            val ignored = serviceMock.getArgument<Set<String>>(1)
            val logicProcessNames = setOf("campaigns", "ad_groups") - ignored

            sequence {
                for (cid in cids) {
                    val objectBatches = logicProcessNames.map { logicProcessName ->
                        LogicObjectBatch(
                            logicProcessName,
                            objects = listOf(object : BaseLogicObject() {}),
                        )
                    }

                    yield(CampaignContent(
                        cid = cid,
                        shard = 1,
                        objectBatches = objectBatches
                    ))
                }
            }
        }
    }

    val tool = spy(SendCampaignContentTool(service, essClient, ppcPropertiesSupport)) {mock
        Mockito.doReturn(Constraint<SendCampaignParam, Defect<*>> { null }).`when`(mock).isTimeoutActiveConstraint()
    }

    fun SendCampaignContentTool.process(paramBuilder: SendCampaignParam.() -> Unit) =
        this.process(SendCampaignParam().apply(paramBuilder))

    @Test
    fun `successful execution test`() {
        val result = tool.process {
            campaignsToSend = "42"
        }
        assertThat(result.data)
            .containsExactlyElementsOf(
                createResults(
                    lastSentCampaign = 42,
                    objectsAmounts = mapOf(
                        "ad_groups" to 1,
                        "campaigns" to 1,
                    ),
                )
            )
        assertThat(essObjectsSent).hasSize(2)
    }

    @Test
    fun `nothing should be sent to ess if sending is prohibited`() {
        val result = tool.process {
            campaignsToSend = "42"
            sendingToEss = false
        }
        assertThat(result.data)
            .containsExactlyElementsOf(
                createResults(
                    lastSentCampaign = 42,
                    objectsAmounts = mapOf(
                        "ad_groups" to 1,
                        "campaigns" to 1,
                    ),
                )
            )
        assertThat(essObjectsSent).hasSize(0)
    }

    @Test
    fun `multiple cids are parsed correctly`() {
        val param = SendCampaignParam()
        param.campaignsToSend = "42 56 78"
        val defects = tool.validate(param)
            .flattenErrors()
        assertThat(defects)
            .isEmpty()
    }

    @Test
    fun `empty cids field`() {
        val param = SendCampaignParam()
        param.campaignsToSend = ""
        val defects = tool.validate(param)
            .flattenErrors()
            .map { it.defect }
        assertThat(defects)
            .containsExactlyInAnyOrder(StringDefects.notEmptyString())
    }

    @Test
    fun `incorrect cids field`() {
        val param = SendCampaignParam()
        param.campaignsToSend = "12 34 a"
        val defects = tool.validate(param)
            .flattenErrors()
            .map { it.defect }
        assertThat(defects)
            .containsExactlyInAnyOrder(NumberDefects.isWholeNumber())
    }
}
