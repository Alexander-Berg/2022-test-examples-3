package ru.yandex.direct.logicprocessor.processors.bsexport.adgroup.showcondition

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import ru.yandex.direct.ess.logicobjects.bsexport.DebugInfo
import ru.yandex.direct.ess.logicobjects.bsexport.adgroup.AdGroupShowConditionType
import ru.yandex.direct.ess.logicobjects.bsexport.adgroup.BsExportAdGroupShowConditionObject
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.BsExportMultipliersServiceTestIgnored

@ContextConfiguration(classes = [BsExportMultipliersServiceTestIgnored.EssConfiguration::class])
@WebAppConfiguration
@ExtendWith(SpringExtension::class)
@Disabled("Запускать вручную с выставленным -Dyandex.environment.type=development")
internal class AdGroupShowConditionHandlerDispatcherTestIgnored {
    @Autowired
    lateinit var handlerDispatcher: AdGroupShowConditionHandlerDispatcher

    @Test
    fun upsert1() {
        val showCondition = BsExportAdGroupShowConditionObject(adGroupId = 18, null, AdGroupShowConditionType.GEO, DebugInfo())
        handlerDispatcher.dispatch(1, listOf(showCondition))
    }

    @Test
    fun upsertTime1() {
        val showCondition2 = BsExportAdGroupShowConditionObject(adGroupId = 4628521771, null, AdGroupShowConditionType.TIME, DebugInfo())
        val showCondition3 = BsExportAdGroupShowConditionObject(adGroupId = 4629468306, null, AdGroupShowConditionType.TIME, DebugInfo())
        handlerDispatcher.dispatch(16, listOf(showCondition2, showCondition3))
    }
}
