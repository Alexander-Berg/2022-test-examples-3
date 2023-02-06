package ru.yandex.direct.oneshot.oneshots.updategeotargeting

open class UpdateGeoTargetingParamsHolder {
    // таргетинг не меняется, если старый и новый родители одновременно выбраны или не выбраны
    fun parametersNoUpdate() = listOf(
        // родители выбраны с предков
        listOf(setOf(3), setOf(3)),
        listOf(setOf(3, -14), setOf(3, -14)),
        // родители не выбраны с предков
        listOf(setOf(14), setOf(14)),
        // оба родителя с плюсом
        listOf(setOf(1, 10819), setOf(1, 10819)),
        listOf(setOf(1, 10819, -14), setOf(1, 10819, -14)),
        // оба родителя с минусом
        listOf(setOf(3, -1, -10819), setOf(3, -1, -10819)),
        listOf(setOf(3, -1, -10819, 14), setOf(3, -1, -10819, 14)),
    )

    // добавляем в таргетинг плюс регион, если регион выбран с предка и новый родитель не выбран
    fun parametersAddPlusRegion() = listOf(
        // новый родитель не выбран с предка
        listOf(setOf(10819), setOf(10819, 14)),
        // новый родитель с минусом
        listOf(setOf(3, -1), setOf(3, -1, 14)),
    )

    // добавляем в таргетинг минус регион, если регион не выбран с предка и новый родитель выбран
    fun parametersAddMinusRegion() = listOf(
        // новый родитель выбран с предка
        listOf(setOf(3, -10819), setOf(3, -14, -10819)),
        // новый родитель с плюсом
        listOf(setOf(1), setOf(1, -14)),
    )

    // убираем из таргетинга плюс регион, если регион с плюсом и новый родитель выбран
    fun parametersRemovePlusRegion() = listOf(
        // новый родитель выбран с предка
        listOf(setOf(3, -10819, 14), setOf(3, -10819)),
        // новый родитель с плюсом
        listOf(setOf(1, 14), setOf(1)),
    )

    // убираем из таргетинга минус регион, если регион с минусом и новый родитель не выбран
    fun parametersRemoveMinusRegion() = listOf(
        // новый родитель не выбран с предка
        listOf(setOf(10819, -14), setOf(10819)),
        // новый родитель с минусом
        listOf(setOf(3, -1, -14), setOf(3, -1)),
    )

    // параметры соответствуют переносу Твери(14) из Тверской области(10819) в Москву и область(1)
    fun parametersSingleUpdate() =
        parametersNoUpdate() +
        parametersAddPlusRegion() + parametersAddMinusRegion() +
        parametersRemovePlusRegion() + parametersRemoveMinusRegion()

    // параметры соответствуют переносу Ивантеевки(21623) и Красноармейска(100471)
    // из Москвы и области(1) в Пушкинский район(98604)
    fun parametersMultiUpdates() = listOf(
        listOf(setOf(1, -98604), setOf(1, -98604, 21623, 100471)),
        listOf(setOf(1, -98604, -21623), setOf(1, -98604, 100471)),
        listOf(setOf(1, -98604, -100471), setOf(1, -98604, 21623)),
        listOf(setOf(98604), setOf(98604, -21623, -100471)),
        listOf(setOf(98604, 21623), setOf(98604, -100471)),
        listOf(setOf(98604, 100471), setOf(98604, -21623))
    )
}
