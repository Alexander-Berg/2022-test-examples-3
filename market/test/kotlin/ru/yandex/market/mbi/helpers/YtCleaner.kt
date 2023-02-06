package ru.yandex.market.mbi.helpers

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.reflections.Reflections
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.market.mbi.orderservice.common.annotations.DynamicTable
import ru.yandex.market.mbi.orderservice.common.model.yt.SortedTableEntity
import ru.yandex.market.mbi.orderservice.common.service.yt.dynamic.TableBindingHolder
import ru.yandex.market.yt.binding.YTBinder
import ru.yandex.market.yt.client.YtClientProxy
import kotlin.reflect.KClass

const val CLEANUP_YT_TABLES = "cleanupTables"

private val defaultTablesToClean =
    Reflections("ru.yandex.market.mbi.orderservice.common.model.yt")
        .getTypesAnnotatedWith(DynamicTable::class.java)
        .map { Pair(it, requireNotNull(it.getAnnotation(DynamicTable::class.java))) }
        .map { Pair(it.first as Class<out SortedTableEntity<out Any>>, it.second.keyClass.java) }
        .associateBy({ it.first }, { it.second })

@Target(AnnotationTarget.CLASS)
@Tag(CLEANUP_YT_TABLES)
annotation class CleanupTables(val classes: Array<KClass<out SortedTableEntity<*>>> = [])

class YtCleaner(private val rwClient: YtClientProxy, private val tableBindingHolder: TableBindingHolder) {

    fun cleanUp(tables: Map<Class<out SortedTableEntity<out Any>>, Class<out Any>> = defaultTablesToClean) {
        tables.forEach {
            doCleanUp(it.value, it.key)
        }
    }

    private fun <K, V : SortedTableEntity<*>> doCleanUp(keyClass: Class<K>, valueClass: Class<V>) {
        val valueBinder = tableBindingHolder[valueClass]
        val keyBinder = YTBinder.getBinder(keyClass)

        val path = valueBinder.table
        rwClient.selectRows("* from [$path]", valueBinder.binder).also { rows ->
            if (rows.isNotEmpty()) {
                @Suppress("UNCHECKED_CAST")
                rwClient.deleteRows(path, keyBinder, rows.map { it.sortingKey() } as List<K>)
            }
        }
    }
}

class YTCleanerExtension : BeforeEachCallback {
    override fun beforeEach(context: ExtensionContext) {
        val store = getStore(context.root)
        val parent = context.parent.orElseThrow()

        if (store.get("cleaner") == null) {
            val springCtx = SpringExtension.getApplicationContext(parent)
            val cleaner = requireNotNull(springCtx.getBean(YtCleaner::class.java))
            store.put("cleaner", cleaner)
        }

        val tags = parent.tags
        if (tags.contains(CLEANUP_YT_TABLES)) {
            val classesToClean = requireNotNull(parent.testClass.get().getAnnotation(CleanupTables::class.java)).classes
            val classMap = defaultTablesToClean.filter { (k, _) -> classesToClean.contains(k.kotlin) }
            store.get("cleaner", YtCleaner::class.java).cleanUp(classMap)
        }
    }

    private fun getStore(context: ExtensionContext): ExtensionContext.Store {
        return context.getStore(ExtensionContext.Namespace.GLOBAL)
    }
}
