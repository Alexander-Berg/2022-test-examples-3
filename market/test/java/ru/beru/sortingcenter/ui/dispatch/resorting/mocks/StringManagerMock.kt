package ru.beru.sortingcenter.ui.dispatch.resorting.mocks

import org.junit.Rule
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.quality.Strictness
import ru.beru.sortingcenter.R
import ru.yandex.market.sc.core.resources.StringManager

class StringManagerMock(private vararg val mockedResourceIds: Int) : StringManager {
    @Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

    private val stringManager: StringManager = mock(StringManager::class.java)

    private val stringResourcesMocks: Map<Int, String> = mapOf(
        R.string.dispatch_not_ready to "Лот, либо заказ, не будут отгружены, так как не относятся к текущей ячейке",
        R.string.empty to "",
        R.string.error to "Ошибка",
        R.string.finish_dispatch to "Завершить отгрузку",
        R.string.invalid_lot to "Лот не относится к этой ячейке",
        R.string.lot_in_wrong_status to "Лот находится в неверном статусе",
        R.string.lot_success_dispatch to "Лот будет отгружен",
        R.string.order to "Заказ",
        R.string.order_already_scanned to "Заказ уже отсканирован",
        R.string.order_success_dispatch to "Заказ %s будет отгружен",
        R.string.place to "Место",
        R.string.successfully to "Успешно",
        R.string.resort_entity_to_any_keep_cell to "Отсортируйте %s в любую ячейку хранения",
        R.string.resort_entity_to_any_return_cell to "Отсортируйте %s в любую ячейку возврата",
        R.string.resort_entity_to_one_of_specific_keep_cells to "Отсортируйте %s в одну из следующих ячеек хранения:",
        R.string.resort_entity_to_one_of_specific_return_cells to "Отсортируйте %s в одну из следующих ячеек возврата:",
        R.string.waiting_dispatch_order_or_lot to "Отсканируйте заказы или лоты из ячейки перед отгрузкой курьеру",
    )

    init {
        mockedResourceIds.forEach { resourceId ->
            stringResourcesMocks[resourceId]?.let { mockedString ->
                val stringNeedsFormatting = mockedString.contains("%")

                if (!stringNeedsFormatting) {
                    `when`(stringManager.getString(resourceId))
                        .thenReturn(mockedString)
                    return@let
                }

                `when`(stringManager.getString(ArgumentMatchers.eq(resourceId), any<Any>()))
                    .thenAnswer {
                        val formatArgs = it.arguments.drop(1)

                        mockedString.format(*formatArgs.toTypedArray())
                    }
            }
        }
    }

    override fun getString(resId: Int): String {
        checkIfResourceInStringResourcesMocks(resId)
        checkIfResourceInMockedResourceIds(resId)

        return stringManager.getString(resId)
    }

    override fun getString(resId: Int, vararg formatArgs: Any?): String {
        checkIfResourceInStringResourcesMocks(resId)
        checkIfResourceInMockedResourceIds(resId)

        return stringManager.getString(resId, *formatArgs)
    }

    private fun checkIfResourceInStringResourcesMocks(resId: Int) {
        require(stringResourcesMocks.containsKey(resId)) {
            "There is no resource with id $resId in stringResourcesMocks of StringManagerMock"
        }
    }

    private fun checkIfResourceInMockedResourceIds(resId: Int) {
        require(mockedResourceIds.contains(resId)) {
            "There is no resource with id $resId in mockedResourceIds of StringManagerMock"
        }
    }
}
