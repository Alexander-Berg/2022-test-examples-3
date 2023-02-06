package ru.yandex.market.logistics.mqm.dto

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Тест для ClaimOrdersFileName")
class ClaimOrdersFileNameTest {

    @Test
    @DisplayName("Проверки корректности парсинга без итерации")
    fun checkValidNameWithoutIteration() {
        val validName = "2022-12-23_32492384.xlsx"
        val parsedName = ClaimOrdersFileName.parseFileName(validName, ClaimOrdersFileName.FILE_NAME_APPROVAL_REGEX)
        val formattedName = parsedName?.getNextFileName(validName, ClaimOrdersFileName.FILE_NAME_APPROVAL_REGEX)

        assertSoftly {
            parsedName shouldNotBe null
            parsedName?.date shouldBe "2022-12-23"
            parsedName?.issueKey shouldBe "32492384"
            formattedName shouldBe "2022-12-23_32492384_1.xlsx"
        }
    }

    @Test
    @DisplayName("Проверки корректности парсинга без итерации")
    fun checkValidNameForPaidWithoutIteration() {
        val validName = "2022-12-23_32492384_PAID.xlsx"
        val parsedName = ClaimOrdersFileName.parseFileName(validName, ClaimOrdersFileName.FILE_NAME_PARTIALLY_REGEX)
        val formattedName = parsedName?.getNextFileName(validName, ClaimOrdersFileName.FILE_NAME_PARTIALLY_REGEX)

        assertSoftly {
            parsedName shouldNotBe null
            parsedName?.date shouldBe "2022-12-23"
            parsedName?.issueKey shouldBe "32492384"
            formattedName shouldBe "2022-12-23_32492384_1.xlsx"
        }
    }

    @Test
    @DisplayName("Проверки корректности парсинга с итерации")
    fun checkValidNameWithIteration() {
        val validName = "2022-12-23_32492384_451.xlsx"
        val parsedName = ClaimOrdersFileName.parseFileName(validName, ClaimOrdersFileName.FILE_NAME_APPROVAL_REGEX)
        val formattedName = parsedName?.getNextFileName(validName, ClaimOrdersFileName.FILE_NAME_APPROVAL_REGEX)

        assertSoftly {
            parsedName shouldNotBe null
            parsedName?.date shouldBe "2022-12-23"
            parsedName?.issueKey shouldBe "32492384"
            parsedName?.iteration shouldBe 451
            formattedName shouldBe "2022-12-23_32492384_452.xlsx"
        }
    }

    @Test
    @DisplayName("Проверки, что парсинг не обрабатывает невалидные строки")
    fun checkInvalidName() {
        val invalidName1 = "2022-12-23_32492384_451.asv"
        val invalidName2 = "2022-12-23-32492384_451.csv"
        val parsedName1 = ClaimOrdersFileName.parseFileName(invalidName1, ClaimOrdersFileName.FILE_NAME_APPROVAL_REGEX)
        val parsedName2 = ClaimOrdersFileName.parseFileName(invalidName2, ClaimOrdersFileName.FILE_NAME_APPROVAL_REGEX)

        assertSoftly {
            parsedName1 == null
            parsedName2 == null
        }
    }
}
