package ru.yandex.market.logistics.mqm.client

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import ru.yandex.market.logistics.mqm.model.enums.AggregationType
import ru.yandex.market.logistics.mqm.model.enums.EntityType
import ru.yandex.market.logistics.mqm.model.enums.EventType
import ru.yandex.market.logistics.mqm.model.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.model.enums.ProcessType
import ru.yandex.market.logistics.mqm.model.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.model.enums.QualityRuleProcessorType
import ru.yandex.market.logistics.mqm.model.filter.PlanFactGroupSearchFilter
import ru.yandex.market.logistics.mqm.model.filter.PlanFactSearchFilter
import ru.yandex.market.logistics.mqm.model.filter.QualityRuleSearchFilter
import ru.yandex.market.logistics.mqm.model.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.model.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.CallPartnerWithStartrekCommentPayload
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.CreateStartrekIssuePayload
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.EventCreateRequest
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.SendTelegramMessagePayload
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.UpdateStartrekIssuePayload
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.WriteToLogPayload
import ru.yandex.market.logistics.mqm.model.request.planfact.PlanFactCreateRequest
import ru.yandex.market.logistics.mqm.model.request.planfact.ProcessingError
import ru.yandex.market.logistics.mqm.model.request.planfact.SetFactTimeRequest
import ru.yandex.market.logistics.mqm.model.request.qualityrule.QualityRuleCreateRequest
import ru.yandex.market.logistics.mqm.model.request.qualityrule.QualityRuleUpdateRequest
import ru.yandex.market.logistics.mqm.model.response.planfact.PlanFactResponse
import ru.yandex.market.logistics.mqm.model.response.planfactgroup.PlanFactGroupResponse
import ru.yandex.market.logistics.mqm.model.response.qualityrule.QualityRuleResponse
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent
import java.time.Instant

class MqmClientTest : AbstractClientTest() {

    @Test
    fun getPlanFact() {
        mockServer.expect(method(HttpMethod.GET))
            .andExpect(requestTo("$uri/plan-fact/1"))
            .andRespond(
                withSuccess()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/plan_fact.json"))
            )

        assertSoftly {
            mqmClient.getPlanFact(1L) shouldBe PLAN_FACT_RESPONSE
        }
    }

    @Test
    fun createPlanFact() {
        mockServer.expect(method(HttpMethod.POST))
            .andExpect(requestTo("$uri/plan-fact"))
            .andExpect(jsonRequestContent("request/create_plan_fact.json"))
            .andRespond(
                withSuccess()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/plan_fact.json"))
            )

        assertSoftly {
            mqmClient.createPlanFact(PlanFactCreateRequest(
                entityType = EntityType.LOM_ORDER,
                entityId = 100L,
                expectedStatus = SegmentStatus.TRANSIT_DELIVERY_TRANSPORTATION.name,
                producerName = "test_producer",
                expectedStatusDatetime = Instant.parse("2021-03-30T19:00:00Z")
            )) shouldBe PLAN_FACT_RESPONSE
        }
    }

    @Test
    fun saveFactTime() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo("$uri/plan-fact/save-fact-time"))
            .andExpect(jsonRequestContent("request/save_fact_time.json"))
            .andRespond(
                withSuccess()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/plan_fact.json"))
            )

        mqmClient.saveFactTime(SetFactTimeRequest(
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            entityId = 1,
            expectedStatus = SegmentStatus.TRANSIT_DELIVERY_TRANSPORTATION.name,
            factDatetime = Instant.parse("2021-07-30T19:00:00Z")
        ))
    }

    @Test
    fun searchPlanFacts() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo("$uri/plan-fact/search"))
            .andExpect(jsonRequestContent("request/search_plan_facts.json"))
            .andRespond(
                withSuccess()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/plan_facts.json"))
            )

        val result = mqmClient.searchPlanFacts(PlanFactSearchFilter(
            entityTypes = setOf(EntityType.LOM_ORDER, EntityType.LOM_WAYBILL_SEGMENT),
            expectedStatuses = setOf(
                SegmentStatus.TRANSIT_DELIVERY_TRANSPORTATION.name,
                SegmentStatus.RETURN_ARRIVED.name
            ),
            expectedStatusDatetimeFrom = Instant.parse("2021-01-30T18:00:00Z"),
            expectedStatusDatetimeTo = Instant.parse("2021-05-30T21:00:00Z"),
            processingStatuses = setOf(ProcessingStatus.ENQUEUED)
        ))

        assertSoftly {
            result.size shouldBe 4
            result shouldContain PLAN_FACT_RESPONSE
        }
    }

    @Test
    fun putProcessingError() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo("$uri/plan-fact/processing-error"))
            .andExpect(jsonRequestContent("request/put_processing_error.json"))
            .andRespond(withSuccess())

        mqmClient.addProcessingError(ProcessingError(
            processType = ProcessType.LOM_ORDER_CREATE,
            entityId = 100L,
            processId = 200L,
            errorCode = 2,
            errorMessage = "message"
        ))
    }

    @Test
    fun getPlanFactGroup() {
        mockServer.expect(method(HttpMethod.GET))
            .andExpect(requestTo("$uri/plan-fact-group/1"))
            .andRespond(
                withSuccess()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/plan_fact_group.json"))
            )

        assertSoftly {
            mqmClient.getPlanFactGroup(1L) shouldBe PLAN_FACT_GROUP_RESPONSE
        }
    }

    @Test
    fun searchPlanFactGroups() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo("$uri/plan-fact-group/search"))
            .andRespond(
                withSuccess()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/plan_fact_groups.json"))
            )

        val result = mqmClient.searchPlanFactGroups(PlanFactGroupSearchFilter(
            expectedStatuses = setOf(
                SegmentStatus.TRANSIT_PICKUP.name,
                SegmentStatus.OUT.name
            ),
            scheduleTimeFrom = Instant.parse("2021-02-20T18:00:00Z"),
            scheduleTimeTo = Instant.parse("2021-05-30T21:00:00Z"),
            processingStatuses = setOf(ProcessingStatus.ENQUEUED, ProcessingStatus.PROCESSED),
            aggregationTypes = setOf(AggregationType.PARTNER, AggregationType.DATE),
            aggregationKeys = setOf("partner:135242;", "date:2021-03-30;"),
            waybillSegmentTypes = setOf(SegmentType.MOVEMENT, SegmentType.COURIER)
        ))

        assertSoftly {
            result.size shouldBe 3
            result shouldContain PLAN_FACT_GROUP_RESPONSE
        }
    }

    @Test
    fun getPlanFactsFromGroup() {
        mockServer.expect(method(HttpMethod.GET))
            .andExpect(requestTo("$uri/plan-fact-group/1/plan-facts"))
            .andRespond(
                withSuccess()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/plan_facts.json"))
            )

        val result = mqmClient.getPlanFactsFromGroup(1L)

        assertSoftly {
            result.size shouldBe 4
            result shouldContain PLAN_FACT_RESPONSE
        }
    }

    @Test
    fun getQualityRule() {
        mockServer.expect(method(HttpMethod.GET))
            .andExpect(requestTo("$uri/quality-rule/1"))
            .andRespond(
                withSuccess()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/quality_rule.json"))
            )

        assertSoftly {
            mqmClient.getQualityRule(1L) shouldBe QUALITY_RULE_RESPONSE
        }
    }

    @Test
    fun createQualityRule() {
        mockServer.expect(method(HttpMethod.POST))
            .andExpect(requestTo("$uri/quality-rule"))
            .andRespond(
                withSuccess()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/quality_rule.json"))
            )

        val result = mqmClient.createQualityRule(QualityRuleCreateRequest(
            enabled = true,
            expectedStatus = SegmentStatus.TRANSIT_COURIER_SEARCH.name,
            waybillSegmentType = SegmentType.COURIER,
            aggregationType = AggregationType.NONE,
            ruleProcessor = QualityRuleProcessorType.TSKV_LOG,
            rule = objectMapper.readerFor(Any::class.java).readValue(
                "\"rule\": {\n" +
                    "\"_type\": \".TskvLoggerPayload\",\n" +
                    "\"logEveryPlanFact\": true\n" +
                "}"
            )
        ))

        assertSoftly {
            result shouldBe QUALITY_RULE_RESPONSE
        }
    }

    @Test
    fun updateQualityRule() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo("$uri/quality-rule/2"))
            .andRespond(
                withSuccess()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/quality_rule.json"))
            )

        val result = mqmClient.updateQualityRule(
            2L,
            QualityRuleUpdateRequest(
                enabled = true,
                expectedStatus = SegmentStatus.TRANSIT_COURIER_SEARCH.name,
                waybillSegmentType = SegmentType.MOVEMENT,
                aggregationType = AggregationType.NONE,
                ruleProcessor = QualityRuleProcessorType.TSKV_LOG,
                rule = objectMapper.readerFor(Any::class.java).readValue(
                    "\"rule\": {" +
                        "\"_type\": \".TskvLoggerPayload\"," +
                        "\"logEveryPlanFact\": true" +
                    "}"
                )
            ))

        assertSoftly {
            result shouldBe QUALITY_RULE_RESPONSE
        }
    }

    @Test
    fun searchQualityRules() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo("$uri/quality-rule/search"))
            .andRespond(
                withSuccess()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/quality_rules.json"))
            )

        val result = mqmClient.searchQualityRules(QualityRuleSearchFilter(
            expectedStatuses = setOf(
                SegmentStatus.TRANSIT_COURIER_SEARCH.name,
                SegmentStatus.TRANSIT_PICKUP.name
            ),
            waybillSegmentTypes = setOf(SegmentType.COURIER, SegmentType.MOVEMENT),
            aggregationTypes = setOf(AggregationType.DATE_PARTNER, AggregationType.NONE),
            ruleProcessors = setOf(QualityRuleProcessorType.TSKV_LOG, QualityRuleProcessorType.STARTREK),
            enabled = true
        ))

        assertSoftly {
            result.size shouldBe 3
            result shouldContain QUALITY_RULE_RESPONSE
        }
    }

    @Test
    fun pushMonitoringEventWriteToLog() {
        mockServer.expect(method(HttpMethod.POST))
            .andExpect(requestTo("$uri/monitoring-event/push"))
            .andExpect(jsonRequestContent("request/push_monitoring_event_write_to_log.json"))
            .andRespond(withSuccess())

        mqmClient.pushMonitoringEvent(EventCreateRequest(
            EventType.WRITE_MESSAGE_TO_LOG,
            WriteToLogPayload("Test message")
        ))
    }

    @Test
    fun pushMonitoringEventSendTelegramMessage() {
        mockServer.expect(method(HttpMethod.POST))
            .andExpect(requestTo("$uri/monitoring-event/push"))
            .andExpect(jsonRequestContent("request/push_monitoring_event_send_telegram_message.json"))
            .andRespond(withSuccess())

        mqmClient.pushMonitoringEvent(EventCreateRequest(
            EventType.SEND_TELEGRAM_MESSAGE,
            SendTelegramMessagePayload(
                channel = "ChannelTest",
                message = "Test message",
                sender = "Me"
            )
        ))
    }

    @Test
    fun pushMonitoringEventCreateStartrekIssue() {
        mockServer.expect(method(HttpMethod.POST))
            .andExpect(requestTo("$uri/monitoring-event/push"))
            .andExpect(jsonRequestContent("request/push_monitoring_event_create_startrek_issue.json"))
            .andRespond(withSuccess())

        mqmClient.pushMonitoringEvent(EventCreateRequest(
            EventType.CREATE_STARTREK_ISSUE,
            CreateStartrekIssuePayload(
                queue = "MQMKEKST",
                summary = "aha",
                description = "description, baby",
                fields = mapOf(
                    "components" to listOf(143425, 123, 43423),
                    "tags" to listOf("call", "courier")
                ),
                csvAttachments = setOf(CreateStartrekIssuePayload.CsvAttachment(
                    fileName = "file1.csv",
                    records = listOf(
                        mapOf(
                            "column1" to 123,
                            "column2" to 442
                        ),
                        mapOf(
                            "column1" to 1423,
                            "column2" to 4452
                        )
                    )
                ))
            )
        ))
    }

    @Test
    fun pushMonitoringEventUpdateStartrekIssue() {
        mockServer.expect(method(HttpMethod.POST))
            .andExpect(requestTo("$uri/monitoring-event/push"))
            .andExpect(jsonRequestContent("request/push_monitoring_event_update_startrek_issue.json"))
            .andRespond(withSuccess())

        mqmClient.pushMonitoringEvent(
            EventCreateRequest(
                EventType.UPDATE_STARTREK_ISSUE,
                UpdateStartrekIssuePayload(
                    issueKey = "test_startrek_key",
                    comment = "test_startrek_comment",
                    fields = mapOf(Pair("a", "b"))
                )
            )
        )
    }

    @Test
    fun pushMonitoringEventCallPartnerWithStartrekComment() {
        mockServer.expect(method(HttpMethod.POST))
            .andExpect(requestTo("$uri/monitoring-event/push"))
            .andExpect(jsonRequestContent("request/push_monitoring_event_call_partner_with_startrek_comment.json"))
            .andRespond(withSuccess())

        mqmClient.pushMonitoringEvent(EventCreateRequest(
            EventType.CALL_PARTNER_WITH_STARTREK_COMMENT,
            CallPartnerWithStartrekCommentPayload(
                orderId = "test_order_id",
                ticketTitle = "Test call ticket title",
                ticketDescription = "Test call ticket description",
                clientEmail = "client@mail.test",
                clientPhone = "+7123",
                issueKey = "test_mqm_ticket",
            )
        ))
    }

    companion object {
        private val objectMapper = ObjectMapper()

        private val PLAN_FACT_RESPONSE = PlanFactResponse(
            id = 1,
            entityType = EntityType.LOM_ORDER,
            entityId = null,
            waybillSegmentType = null,
            expectedStatus = SegmentStatus.TRANSIT_DELIVERY_TRANSPORTATION.name,
            producerName = "test_producer",
            expectedStatusDatetime = Instant.parse("2021-03-30T19:00:00Z"),
            factStatusDatetime = null,
            endOfProcessingDatetime = null,
            processingStatus = ProcessingStatus.ENQUEUED,
            planFactStatus = PlanFactStatus.CREATED,
            scheduleTime = null,
            cause = null,
            subcause = null,
            additionalData = null
        )

        private val PLAN_FACT_GROUP_RESPONSE = PlanFactGroupResponse(
            id = 1,
            waybillSegmentType = SegmentType.COURIER,
            expectedStatus = SegmentStatus.OUT.name,
            processingStatus = ProcessingStatus.ENQUEUED,
            scheduleTime = Instant.parse("2021-03-30T19:00:00Z"),
            additionalData = objectMapper.readerFor(Any::class.java).readValue(
                "{\"PlanFactGroupAdditionalData\": {" +
                    "\"_type\":\".PlanFactGroupAdditionalData\", " +
                        "\"aggregationEntity\": {" +
                        "\"date\": \"2021-03-30\"" +
                    "}" +
                "}}"
            ),
            aggregationType = AggregationType.DATE,
            aggregationKey = "date:2021-03-30;"
        )

        private val QUALITY_RULE_RESPONSE = QualityRuleResponse(
            id = 2,
            enabled = true,
            expectedStatus = SegmentStatus.TRANSIT_COURIER_SEARCH.name,
            waybillSegmentType = SegmentType.MOVEMENT,
            aggregationType = AggregationType.NONE,
            ruleProcessor = QualityRuleProcessorType.TSKV_LOG,
            rule = objectMapper.readerFor(Any::class.java).readValue(
                "{" +
                    "\"_type\": \".TskvLoggerPayload\"," +
                    "\"logEveryPlanFact\": true" +
                "}"
            )
        )
    }
}
