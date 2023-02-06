package ru.yandex.direct.core.entity.strategy.type.defaultmanualstrategy

import java.math.BigDecimal
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.DefaultManualStrategy
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestDefaultManualStrategy
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges

@CoreTest
@RunWith(SpringRunner::class)
internal class DefaultManualStrategyUpdateValidationTypeSupportTest : StrategyUpdateOperationTestBase() {
    private lateinit var clientInfo: ClientInfo

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
    }

    override fun getShard() = clientInfo.shard

    override fun getClientId(): ClientId = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    @Test
    fun `update to valid strategy`() {
        val strategy = TestDefaultManualStrategy.clientDefaultManualStrategy()
            .withDayBudget(BigDecimal.valueOf(10000L))

        val addOperation = createAddOperation(listOf(strategy))

        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, DefaultManualStrategy::class.java)
            .process(BigDecimal.valueOf(20000L), DefaultManualStrategy.DAY_BUDGET)

        prepareAndApplyValid(listOf(modelChanges))
    }

    //TODO раскоментить тест когда сделаем возможность обновить список cid'ов
//    @Test
//    fun `validation failed on invalid enable_cpc_hold update`() {
//        val strategy = TestDefaultManualStrategy.clientDefaultManualStrategy()
//            .withEnableCpcHold(null)
//
//        val addOperation = createAddOperation(listOf(strategy))
//
//        addOperation.prepareAndApply()
//
//        val modelChanges = ModelChanges(strategy.id, DefaultManualStrategy::class.java)
//            .process(true, DefaultManualStrategy.ENABLE_CPC_HOLD)
//
//        val vr = prepareAndApplyInvalid(listOf(modelChanges))
//
//        val matcher = Matchers.hasDefectDefinitionWith<Any>(
//            Matchers.validationError(
//                PathHelper.path(PathHelper.index(0), PathHelper.field(DefaultManualStrategy.ENABLE_CPC_HOLD)),
//                notNull()
//            )
//        )
//
//        vr.check(matcher)
//    }
}
