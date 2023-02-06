package ru.yandex.direct.logicprocessor.processors.mysql2grut

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.Tables.STRATEGIES
import ru.yandex.direct.dbschema.ppc.enums.StrategiesType
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class StrategyReplicationTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var shardHelper: ShardHelper

    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationApiService: GrutApiService

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    private lateinit var clientInfo: ClientInfo

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        replicationApiService.clientGrutDao.createOrUpdateClients(
            listOf(
                ClientGrutModel(
                    clientInfo.client!!,
                    listOf()
                )
            )
        )
    }

    @Test
    fun createStrategyTest() {
        val name = "Strategy1"
        val strategyId = createStrategy("Strategy1")
        processor.withShard(clientInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    strategyId = strategyId
                )
            )
        )
        val createdStrategy = replicationApiService.strategyGrutApi.getStrategy(strategyId)
        Assertions.assertThat(createdStrategy).isNotNull
        Assertions.assertThat(createdStrategy!!.meta.clientId).isEqualTo(clientInfo.clientId!!.asLong())
        Assertions.assertThat(createdStrategy.spec.name).isEqualTo(name)
    }

    @Test
    fun createStrategy_NullNameTest() {
        val strategyId = createStrategy(null)
        processor.withShard(clientInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    strategyId = strategyId
                )
            )
        )
        val createdStrategy = replicationApiService.strategyGrutApi.getStrategy(strategyId)
        Assertions.assertThat(createdStrategy).isNotNull
        Assertions.assertThat(createdStrategy!!.meta.clientId).isEqualTo(clientInfo.clientId!!.asLong())
        Assertions.assertThat(createdStrategy.spec.name).isEqualTo("")
    }

    @Test
    fun updateStrategyTest() {
        val name = "Strategy1"
        val strategyId = createStrategy("Strategy1")
        processor.withShard(clientInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    strategyId = strategyId
                )
            )
        )
        val createdStrategy = replicationApiService.strategyGrutApi.getStrategy(strategyId)
        Assertions.assertThat(createdStrategy).isNotNull
        Assertions.assertThat(createdStrategy!!.spec.name).isEqualTo(name)
        val newName = "Strategy2"
        dslContextProvider.ppc(clientInfo.shard)
            .update(STRATEGIES)
            .set(STRATEGIES.NAME, newName)
            .where(STRATEGIES.STRATEGY_ID.eq(strategyId))
            .execute()
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    strategyId = strategyId
                )
            )
        )
        val updatedStrategy = replicationApiService.strategyGrutApi.getStrategy(strategyId)
        Assertions.assertThat(updatedStrategy!!.spec.name).isEqualTo(newName)
    }


    @Test
    fun updateStrategyNameToNullTest() {
        val name = "Strategy1"
        val strategyId = createStrategy("Strategy1")
        processor.withShard(clientInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    strategyId = strategyId
                )
            )
        )
        val createdStrategy = replicationApiService.strategyGrutApi.getStrategy(strategyId)
        Assertions.assertThat(createdStrategy).isNotNull
        Assertions.assertThat(createdStrategy!!.spec.name).isEqualTo(name)
        val newName = "Strategy2"
        dslContextProvider.ppc(clientInfo.shard)
            .update(STRATEGIES)
            .setNull(STRATEGIES.NAME)
            .where(STRATEGIES.STRATEGY_ID.eq(strategyId))
            .execute()
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    strategyId = strategyId
                )
            )
        )
        val updatedStrategy = replicationApiService.strategyGrutApi.getStrategy(strategyId)
        Assertions.assertThat(updatedStrategy!!.spec.name).isEqualTo("")
    }

    private fun createStrategy(name: String?): Long {
        val id = shardHelper.generateStrategyIds(clientInfo.clientId!!.asLong(), 1)[0]
        var query = dslContextProvider.ppc(clientInfo.shard)
            .insertInto(STRATEGIES)
            .set(STRATEGIES.STRATEGY_ID, id)
            .set(STRATEGIES.STRATEGY_DATA, "{\"sum\": \"700\", \"name\": \"autobudget\"}")
            .set(STRATEGIES.TYPE, StrategiesType.autobudget)
            .set(STRATEGIES.CLIENT_ID, clientInfo.clientId!!.asLong())
        query = if (name != null) {
            query.set(STRATEGIES.NAME, name)
        } else {
            query.setNull(STRATEGIES.NAME)
        }
        query.execute()
        return id
    }
}
