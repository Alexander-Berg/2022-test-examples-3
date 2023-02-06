package ru.yandex.direct.core.testing.steps

import org.jooq.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeo
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeoSegment
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeoSimple
import ru.yandex.direct.core.entity.hypergeo.repository.HyperGeoRepository
import ru.yandex.direct.core.entity.hypergeo.repository.HyperGeoSegmentRepository
import ru.yandex.direct.core.entity.retargeting.model.ConditionType
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition
import ru.yandex.direct.core.entity.retargeting.model.Rule
import ru.yandex.direct.core.entity.retargeting.model.RuleType
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository
import ru.yandex.direct.core.testing.data.defaultHyperGeo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.test.utils.randomPositiveLong
import java.time.LocalDateTime

@Component
class HyperGeoSteps {

    @Autowired
    private lateinit var hyperGeoSegmentRepository: HyperGeoSegmentRepository

    @Autowired
    private lateinit var hyperGeoRepository: HyperGeoRepository

    @Autowired
    private lateinit var retargetingConditionRepository: RetargetingConditionRepository

    fun createHyperGeo(clientInfo: ClientInfo, hyperGeo: HyperGeo = defaultHyperGeo()): HyperGeo {
        createHyperGeoSegments(clientInfo, hyperGeo.hyperGeoSegments)
        createHyperGeoRetargetingCondition(clientInfo, hyperGeo)

        return hyperGeo
    }

    fun <T : Collection<HyperGeoSegment>> createHyperGeoSegments(clientInfo: ClientInfo, hyperGeoSegments: T): T {
        hyperGeoSegments.forEach {
            it.id = randomPositiveLong()
            it.clientId = clientInfo.clientId!!.asLong()
        }
        hyperGeoSegmentRepository.addHyperGeoSegments(clientInfo.shard, hyperGeoSegments)

        return hyperGeoSegments
    }

    private fun createHyperGeoRetargetingCondition(clientInfo: ClientInfo, hyperGeo: HyperGeo) {
        val hyperGeoRetargetingCondition = convertHyperGeo(clientId = clientInfo.clientId!!, hyperGeo = hyperGeo)
        retargetingConditionRepository.add(clientInfo.shard, listOf(hyperGeoRetargetingCondition))
        hyperGeo.id = hyperGeoRetargetingCondition.id
    }

    fun createHyperGeoLink(config: Configuration, hyperGeoIdByAdGroupId: Map<Long, Long>) {
        hyperGeoRepository.linkHyperGeosToAdGroups(config, hyperGeoIdByAdGroupId)
    }
}

fun convertHyperGeo(clientId: ClientId, hyperGeo: HyperGeo): RetargetingCondition =
    RetargetingCondition()
        .withId(hyperGeo.id)
        .withClientId(clientId.asLong())
        .withName(hyperGeo.name)
        .withAvailable(true)
        .withType(ConditionType.geo_segments)
        .withDeleted(false)
        .withLastChangeTime(LocalDateTime.now())
        .withRules(
            listOf(Rule()
                .withType(RuleType.OR)
                .withGoals(hyperGeo.hyperGeoSegments.map { it.id }.map { Goal().withId(it) as Goal })))
        as RetargetingCondition

fun convertHyperGeoToSimple(hyperGeo: HyperGeo): HyperGeoSimple =
    HyperGeoSimple()
        .withId(hyperGeo.id)
        .withName(hyperGeo.name)
        .withHyperGeoSegmentIds(hyperGeo.hyperGeoSegments.map { it.id })
