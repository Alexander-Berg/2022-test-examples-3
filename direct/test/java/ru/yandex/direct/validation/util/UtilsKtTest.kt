package ru.yandex.direct.validation.util

import org.junit.Test
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.validation.constraint.CommonConstraints
import ru.yandex.direct.validation.constraint.NumberConstraints
import ru.yandex.direct.validation.defect.ids.NumberDefectIds
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.PathHelper.field

data class Obj(
    val id: Int,
    val name: String?,
)

class UtilsKtKtTest {
    @Test
    fun validateObjectExample() {
        val obj = Obj(1, null)
        val result = validateObject(obj) {
            property(Obj::id) {
                check(CommonConstraints.notNull())
                check(NumberConstraints.lessThan(100))
                // fail
                check(NumberConstraints.greaterThan(10))
            }

            property(obj::name)
                .check(CommonConstraints.notNull())
        }

        softly {
            assertThat(result.flattenErrors()).hasSize(2)

            result.subResults[field("id")]?.errors.also {
                assertThat(it).hasSize(1)
                assertThat(it?.first()?.defectId())
                    .isEqualTo(NumberDefectIds.MUST_BE_GREATER_THAN_MIN)
            }

            result.subResults[field("name")]?.errors.also {
                assertThat(it).hasSize(1)
                assertThat(it?.first()?.defectId())
                    .isEqualTo(DefectIds.CANNOT_BE_NULL)
            }
        }
    }
}
