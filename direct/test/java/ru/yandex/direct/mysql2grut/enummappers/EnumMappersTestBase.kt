package ru.yandex.direct.mysql2grut.enummappers

import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SoftAssertionsExtension::class)
open class EnumMappersTestBase {
    @InjectSoftAssertions
    protected lateinit var softly: SoftAssertions

    /**
     * Универсальный тест для проверки мапингу enum'а
     *
     * @param sourceEnumValues      значения исходного enum (Enum.values())
     * @param mapperFunction        маппер, который проверяем
     * @param targetUnknownValue    значение целевого enum'а которое возвращает маппер в случае неуспеха
     * @param skipValues            значения исходного Enum'а котороые на надо проверять (например им не нужен мапинг)
     */
    protected fun <From : Enum<From>, To : Enum<To>> testBase(
        sourceEnumValues: Array<From>,
        mapperFunction: Function1<From, To>,
        targetUnknownValue: To,
        skipValues: Set<From> = emptySet(),
    ) {
        sourceEnumValues
            .filterNot { it in skipValues }
            .forEach {
                with(softly) {
                    assertThat(it)
                        .extracting(mapperFunction)
                        .withFailMessage("${it.javaClass.simpleName}.$it doesn't have mapping")
                        .isNotNull
                        .isNotEqualTo(targetUnknownValue)
                }
            }
        skipValues.forEach {
            with(softly) {
                assertThat(it)
                    .extracting(mapperFunction)
                    .`as`("${it.javaClass.simpleName}.$it excluded from test, but mapped. Forget to remove from exceptions?")
                    .isEqualTo(targetUnknownValue)
            }
        }
    }
}
