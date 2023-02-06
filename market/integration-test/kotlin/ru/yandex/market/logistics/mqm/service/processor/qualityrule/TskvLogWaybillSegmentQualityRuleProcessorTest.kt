package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.QualityRule
import ru.yandex.market.logistics.mqm.entity.enums.QualityRuleProcessorType
import ru.yandex.market.logistics.mqm.logging.LomPlanFactTskvLogger
import ru.yandex.market.logistics.mqm.service.handler.QualityRuleHandler
import ru.yandex.market.logistics.mqm.utils.TskvLogCaptor
import java.time.Instant
import java.time.ZoneOffset

@DisplayName("Тест процессора для записи метрик в лог")
class TskvLogWaybillSegmentQualityRuleProcessorTest : AbstractContextualTest() {

    private val fixedTime = Instant.parse("2020-11-07T12:00:00Z")

    @Autowired
    @Qualifier("planFactHandler")
    private lateinit var planFactHandler: QualityRuleHandler

    @Autowired
    @Qualifier("planFactGroupHandler")
    private lateinit var planFactGroupHandler: QualityRuleHandler

    @Autowired
    lateinit var tskvLogWaybillSegmentQualityRuleProcessor: TskvLogWaybillSegmentQualityRuleProcessor

    @RegisterExtension
    @JvmField
    final val tskvLogCaptor = TskvLogCaptor(LomPlanFactTskvLogger.getLoggerName())

    @BeforeEach
    fun setup() {
        clock.setFixed(fixedTime, ZoneOffset.UTC)
    }

    @Test
    fun canProcess() {
        QualityRuleProcessorType.values().forEach { processor ->
            assertSoftly {
                tskvLogWaybillSegmentQualityRuleProcessor.canProcess(
                    QualityRule().apply { ruleProcessor = processor }
                ) shouldBe (processor === QualityRuleProcessorType.TSKV_LOG)
            }
        }
    }

    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/tskvlog_event.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/tskvlog_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun processSingle() {
        planFactHandler.handle(listOf(1, 100), fixedTime)
        val log = tskvLogCaptor.results?.toString()
        val checks = setOf(
            "code=QUALITY_RULE_EVENT\t" +
                "planFactId=1\t" +
                "qualityRuleId=1\t" +
                "expectedStatus=IN\t" +
                "segmentType=FULFILLMENT\t" +
                "waybillSegmentId=2\t" +
                "partnerType=FULFILLMENT\t" +
                "partnerId=202\t" +
                "lomOrderId=1\t" +
                "orderId=LOinttest-1\t" +
                "extraKeys=partner_id,partner_type\t" +
                "extraValues=202,FULFILLMENT\n"
        )
        assertSoftly {
            checks.forEach { log shouldContain it }
        }
    }

    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/tskvlog_event_log_every_planfact.xml")
    fun processAggregatedLogEveryPlanFact() {
        planFactGroupHandler.handle(listOf(101, 103, 105, 107, 109), fixedTime)
        val log = tskvLogCaptor.results?.toString()
        val checks = setOf(
            "code=QUALITY_RULE_AGGREGATED_METRICS\t" +
                "planFactId=1\t" +
                "qualityRuleId=2\t" +
                "expectedStatus=IN\t" +
                "segmentType=FULFILLMENT\t" +
                "waybillSegmentId=2\t" +
                "partnerType=FULFILLMENT\t" +
                "partnerId=202\t" +
                "lomOrderId=1\t" +
                "orderId=LOinttest-1\t" +
                "extraKeys=aggregation_type,partner_from_id,partner_id,partner_subtype,partner_type,partner_name," +
                "partner_from_subtype,partner_from_name,partner_from_type\t" +
                "extraValues=DATE_PARTNER_RELATION_FROM,201,202,MARKET_COURIER,FULFILLMENT,Partnyor 202,TAXI_LAVKA," +
                "Partnyor 201,FULFILLMENT\n",
            "code=QUALITY_RULE_AGGREGATED_TOTAL\t" +
                "qualityRuleId=2\t" +
                "expectedStatus=IN\t" +
                "segmentType=FULFILLMENT\t" +
                "extraKeys=aggregation_type,group_segment_type,partner_from_id,partner_id,partner_subtype," +
                "partner_type,partner_name,partner_from_subtype,partner_from_name,partner_from_type,num_segments\t" +
                "extraValues=DATE_PARTNER_RELATION_FROM,FULFILLMENT,201,202,MARKET_COURIER,FULFILLMENT," +
                "Partnyor 202,TAXI_LAVKA,Partnyor 201," +
                "FULFILLMENT,1\n",
            "code=QUALITY_RULE_AGGREGATED_TOTAL\t" +
                "qualityRuleId=3\t" +
                "expectedStatus=IN\t" +
                "segmentType=FULFILLMENT\t" +
                "extraKeys=aggregation_type,group_segment_type,partner_id,partner_type,partner_name,num_segments\t" +
                "extraValues=DATE_PARTNER,FULFILLMENT,202,FULFILLMENT,Partner 202,0\n",
            "code=QUALITY_RULE_AGGREGATED_TOTAL\t" +
                "qualityRuleId=4\t" +
                "expectedStatus=IN\t" +
                "segmentType=FULFILLMENT\t" +
                "extraKeys=aggregation_type,group_segment_type,partner_id,partner_subtype,partner_type," +
                "num_segments\t" +
                "extraValues=PARTNER,FULFILLMENT,202,MARKET_COURIER,FULFILLMENT,0\n",
            "code=QUALITY_RULE_AGGREGATED_TOTAL\t" +
                "qualityRuleId=5\t" +
                "expectedStatus=IN\t" +
                "segmentType=FULFILLMENT\t" +
                "extraKeys=aggregation_type,group_segment_type,partner_from_id,partner_id,partner_subtype," +
                "partner_type,partner_from_subtype,partner_from_type,num_segments\t" +
                "extraValues=PARTNER_RELATION_FROM,FULFILLMENT,201,202,MARKET_COURIER,FULFILLMENT," +
                "MARKET_COURIER,FULFILLMENT,0\n",
            "code=QUALITY_RULE_AGGREGATED_TOTAL\t" +
                "qualityRuleId=6\t" +
                "expectedStatus=IN\t" +
                "segmentType=FULFILLMENT\t" +
                "extraKeys=aggregation_type,partner_to_id,partner_to_type,group_segment_type,partner_id," +
                "partner_subtype,partner_type,partner_to_subtype,num_segments\t" +
                "extraValues=PARTNER_RELATION_TO,201,FULFILLMENT,FULFILLMENT,202,MARKET_COURIER,FULFILLMENT," +
                "MARKET_COURIER,0\n"
        )
        assertSoftly {
            checks.forEach { log shouldContain it }
        }
    }

    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/tskvlog_empty_group.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/tskvlog_empty_group.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun processAggregatedIfEmpty() {
        planFactGroupHandler.handle(listOf(101), fixedTime)
        val log = tskvLogCaptor.results?.toString()
        val checks = setOf(
            "code=QUALITY_RULE_AGGREGATED_TOTAL\t" +
                "qualityRuleId=4\t" +
                "expectedStatus=IN\t" +
                "segmentType=FULFILLMENT\t" +
                "extraKeys=aggregation_type,group_segment_type,partner_from_id,partner_id,partner_subtype,partner_type," +
                "partner_name,partner_from_subtype,partner_from_name,partner_from_type,num_segments\t" +
                "extraValues=PARTNER,FULFILLMENT,201,202,MARKET_COURIER,FULFILLMENT," +
                "Partnyor 202,TAXI_LAVKA,Partnyor 201,FULFILLMENT,0\n"
        )
        assertSoftly {
            checks.forEach { log shouldContain it }
        }
    }

    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/tskvlog_event_default_payload.xml")
    fun processAggregatedDefault() {
        planFactGroupHandler.handle(listOf(101, 103, 105, 107, 109), fixedTime)
        val log = tskvLogCaptor.results?.toString()
        val checks = setOf(
            "code=QUALITY_RULE_AGGREGATED_TOTAL\t" +
                "qualityRuleId=2\t" +
                "expectedStatus=IN\t" +
                "segmentType=FULFILLMENT\t" +
                "extraKeys=aggregation_type,group_segment_type,partner_from_id,partner_id,partner_subtype," +
                "partner_type,partner_from_subtype,partner_from_type,num_segments\t" +
                "extraValues=DATE_PARTNER_RELATION_FROM,FULFILLMENT,201,202,TAXI_LAVKA,FULFILLMENT,MARKET_COURIER," +
                "FULFILLMENT,1\n",
            "code=QUALITY_RULE_AGGREGATED_TOTAL\t" +
                "qualityRuleId=3\t" +
                "expectedStatus=IN\t" +
                "segmentType=FULFILLMENT\t" +
                "extraKeys=aggregation_type,group_segment_type,partner_id,partner_subtype,partner_type," +
                "num_segments\t" +
                "extraValues=DATE_PARTNER,FULFILLMENT,202,MARKET_COURIER,FULFILLMENT,0\n",
            "code=QUALITY_RULE_AGGREGATED_TOTAL\t" +
                "qualityRuleId=4\t" +
                "expectedStatus=IN\t" +
                "segmentType=FULFILLMENT\t" +
                "extraKeys=aggregation_type,group_segment_type,partner_id,partner_type,num_segments\t" +
                "extraValues=PARTNER,FULFILLMENT,202,FULFILLMENT,0\n",
            "code=QUALITY_RULE_AGGREGATED_TOTAL\t" +
                "qualityRuleId=5\t" +
                "expectedStatus=IN\t" +
                "segmentType=FULFILLMENT\t" +
                "extraKeys=aggregation_type,group_segment_type,partner_from_id,partner_id,partner_address_from,partner_subtype," +
                "partner_type,partner_from_subtype,partner_from_type,num_segments\t" +
                "extraValues=PARTNER_RELATION_FROM,FULFILLMENT,201,202,Moskva_ Tverskaya_ 8_ st 1_ k 2,MARKET_COURIER,FULFILLMENT,TAXI_LAVKA," +
                "FULFILLMENT,0\n",
            "code=QUALITY_RULE_AGGREGATED_TOTAL\t" +
                "qualityRuleId=6\t" +
                "expectedStatus=IN\t" +
                "segmentType=FULFILLMENT\t" +
                "extraKeys=aggregation_type,partner_to_id,partner_to_type,group_segment_type,partner_id," +
                "partner_subtype,partner_type,partner_to_subtype,num_segments\t" +
                "extraValues=PARTNER_RELATION_TO,201,FULFILLMENT,FULFILLMENT,202,MARKET_COURIER,FULFILLMENT," +
                "TAXI_LAVKA,0\n"
        )
        assertSoftly {
            checks.forEach { log shouldContain it }
        }
    }
}
