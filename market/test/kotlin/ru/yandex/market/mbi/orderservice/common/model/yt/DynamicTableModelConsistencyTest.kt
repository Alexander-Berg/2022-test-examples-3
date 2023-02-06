package ru.yandex.market.mbi.orderservice.common.model.yt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.reflections.Reflections
import ru.yandex.market.mbi.orderservice.common.annotations.DynamicTable

/**
 * Тест проверяет, что в моделях классов, мапящихся на динтаблицы YT
 * реализованы все необходимые интерфейсы
 */
class DynamicTableModelConsistencyTest {

    @Test
    fun `test consistency of model classes`() {
        val tableClasses = Reflections("ru.yandex.market.mbi.orderservice.common")
            .getTypesAnnotatedWith(DynamicTable::class.java)

        assertThat(tableClasses).isNotEmpty
        tableClasses.forEach {
            assertThat(it.interfaces)
                .describedAs("@MigratedTable classes should implement SortedTableEntity interface")
                .withFailMessage { "Class ${it.simpleName} does not implement SortedTableEntity interface" }
                .contains(SortedTableEntity::class.java)
        }
    }
}
