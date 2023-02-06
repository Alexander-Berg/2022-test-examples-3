package ru.yandex.direct.logicprocessor.processors.bsexport.adgroup.resource

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import ru.yandex.direct.ess.logicobjects.bsexport.DebugInfo
import ru.yandex.direct.ess.logicobjects.bsexport.adgroup.AdGroupResourceType.ALL
import ru.yandex.direct.ess.logicobjects.bsexport.adgroup.AdGroupResourceType.COMMON_FIELDS
import ru.yandex.direct.ess.logicobjects.bsexport.adgroup.AdGroupResourceType.DELETE
import ru.yandex.direct.ess.logicobjects.bsexport.adgroup.AdGroupResourceType.INTERNAL_AD_FIELDS
import ru.yandex.direct.ess.logicobjects.bsexport.adgroup.BsExportAdGroupObject
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.BsExportMultipliersServiceTestIgnored.EssConfiguration
import ru.yandex.direct.utils.JsonUtils.fromJson
import ru.yandex.direct.utils.JsonUtils.toJson

@ContextConfiguration(classes = [EssConfiguration::class])
@WebAppConfiguration
@ExtendWith(SpringExtension::class)
@Disabled("Запускать вручную с выставленным -Dyandex.environment.type=development")
internal class BsExportAdGroupResourceHandlerDispatcherTestIgnored {
    @Autowired
    lateinit var adGroupService: BsExportAdGroupService

    @Test
    fun upsert1() {
        val group = BsExportAdGroupObject(COMMON_FIELDS, 4629249796L, 399707231L, DebugInfo())
        adGroupService.processAdGroups(1, listOf(jsonCheck(group)))
    }

    @Test
    fun upsert1Internal() {
        val group = BsExportAdGroupObject(INTERNAL_AD_FIELDS, 4336938520L, 55890731L, DebugInfo())
        adGroupService.processAdGroups(1, listOf(jsonCheck(group)))
    }

    @Test
    fun upsert2() {
        val group1 = BsExportAdGroupObject(COMMON_FIELDS, 4336938520L, 55890731L, DebugInfo())
        val group2 = BsExportAdGroupObject(INTERNAL_AD_FIELDS, 4336938520L, 55890731L, DebugInfo())
        adGroupService.processAdGroups(1, listOf(group1, group2))
    }

    @Test
    fun upsert1All() {
        val group1 = BsExportAdGroupObject(ALL, 4336938522L, 55890731L, DebugInfo())
        adGroupService.processAdGroups(1, listOf(group1))
    }

    @Test
    fun delete1() {
        val group = BsExportAdGroupObject(DELETE, 4629249797L, 399707231L, DebugInfo())
        adGroupService.processAdGroups(1, listOf(jsonCheck(group)))
    }

}

inline fun <reified T> jsonCheck(group: T): T {
    val str = toJson(group)
    println(str)
    return fromJson(str, T::class.java)
}

