package ru.yandex.direct.core.copyentity

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.direct.result.MassResult
import ru.yandex.direct.result.ResultState
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.ValidationResult

class CopyValidationResultUtilsTest {

    @Test
    fun `mergeMassResults element errors test`() {
        // 5 валидируемых объектов
        val objects: List<Any> = listOf(Any(), Any(), Any(), Any(), Any())
        val objectIds: List<Long> = listOf(123, 456, 234, 567, 345)

        // результаты добавления объектов, разбитых в чанки по 2
        val massResults: List<MassResult<Long>> = listOf(
            // результат добавления первых двух объектов
            MassResult.brokenMassAction(
                objectIds.subList(0, 2),
                // результат валидации первых двух объектов
                ValidationResult(
                    objects.subList(0, 2),
                    listOf(),
                    listOf(),
                    mapOf(
                        // ошибка валидации отдельного элемента
                        index(1) to ValidationResult(
                            objects[1],
                            listOf(CommonDefects.objectNotFound()),
                            listOf()
                        ),
                    )
                )
            ),
            // результат добавления второй пары элементов
            MassResult.brokenMassAction(
                objectIds.subList(2, 4),
                // результат валидации второй пары элементов
                ValidationResult(
                    objects.subList(2, 4),
                    listOf(),
                    listOf(),
                    mapOf(
                        index(0) to ValidationResult(
                            objects[2],
                            listOf(CommonDefects.mustBeEmpty()),
                            listOf()
                        ),
                    )
                )
            ),
            // результат добавления оставшегося элемента
            MassResult.successfulMassAction(
                objectIds.subList(4, 5),
                // результат валидации оставшегося элемента
                ValidationResult(
                    objects.subList(4, 5),
                    listOf(),
                    listOf(),
                    mapOf(
                        index(0) to ValidationResult(
                            objects[4],
                            listOf(),
                            listOf(CommonDefects.isNull())
                        )
                    )
                )
            ),
        )

        val mergedMassResult: MassResult<Long> = CopyValidationResultUtils.mergeMassResults(massResults)

        assertThat(mergedMassResult)
            .usingRecursiveComparison()
            .isEqualTo(
                MassResult(
                    massResults[0].toResultList() + massResults[1].toResultList() + massResults[2].toResultList(),
                    ValidationResult(
                        objects,
                        listOf(),
                        listOf(),
                        mapOf(
                            index(1) to ValidationResult(
                                objects[1],
                                listOf(CommonDefects.objectNotFound()),
                                listOf()
                            ),
                            index(2) to ValidationResult(
                                objects[2],
                                listOf(CommonDefects.mustBeEmpty()),
                                listOf()
                            ),
                            index(4) to ValidationResult(
                                objects[4],
                                listOf(),
                                listOf(CommonDefects.isNull())
                            ),
                        )
                    ),
                    ResultState.SUCCESSFUL
                ),
            )
    }

    @Test
    fun `mergeMassResults operation errors test`() {
        val objects: List<Any> = listOf(Any(), Any())
        val objectIds: List<Long> = listOf(123, 456)

        val massResults: List<MassResult<Long>> = listOf(
            MassResult.brokenMassAction(
                objectIds.subList(0, 1),
                ValidationResult(
                    objects.subList(0, 1),
                    listOf(CommonDefects.invalidValue()),
                    listOf()
                )
            ),
            MassResult.brokenMassAction(
                objectIds.subList(1, 2),
                ValidationResult(
                    objects.subList(0, 1),
                    listOf(),
                    listOf(),
                    mapOf(
                        index(0) to ValidationResult(
                            objects[1],
                            listOf(CommonDefects.isNull()),
                            listOf()
                        ),
                    )
                )
            )
        )

        val mergedMassResult: MassResult<Long> = CopyValidationResultUtils.mergeMassResults(massResults)

        assertThat(mergedMassResult)
            .usingRecursiveComparison()
            .isEqualTo(
                MassResult<Long>(
                    null,
                    ValidationResult(
                        objects,
                        listOf(CommonDefects.invalidValue()),
                        listOf(),
                        mapOf(
                            index(1) to ValidationResult(
                                objects[1],
                                listOf(CommonDefects.isNull()),
                                listOf()
                            ),
                        )
                    ),
                    ResultState.SUCCESSFUL
                )
            )
    }
}
