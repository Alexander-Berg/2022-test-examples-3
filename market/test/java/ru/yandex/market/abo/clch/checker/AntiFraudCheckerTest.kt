package ru.yandex.market.abo.clch.checker

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.abo.clch.ClchTest
import ru.yandex.market.abo.core.antifraud.v2.model.AntiFraudClchResult
import ru.yandex.market.abo.core.antifraud.v2.model.AntiFraudCloneFeature
import ru.yandex.market.abo.core.antifraud.v2.model.AntiFraudCloneFeature.CURRENT_GLUE
import ru.yandex.market.abo.core.antifraud.v2.model.AntiFraudCloneFeature.CURRENT_SAME_CONTACTS
import ru.yandex.market.abo.core.antifraud.v2.model.AntiFraudCloneFeature.CURRENT_SAME_JUR_INFO
import ru.yandex.market.abo.core.antifraud.v2.model.AntiFraudCloneFeature.PASSPORT_GLUE
import ru.yandex.market.abo.core.antifraud.v2.model.AntiFraudCloneFeature.PASSPORT_SAME_CONTACTS
import ru.yandex.market.abo.core.antifraud.v2.model.AntiFraudCloneFeature.SAME_CONTACTS
import ru.yandex.market.abo.core.antifraud.v2.model.AntiFraudGlueContacts
import ru.yandex.market.abo.core.antifraud.v2.service.AntiFraudClchResultService

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru), zilzilok
 * @date 11.03.2020
 */
class AntiFraudCheckerTest @Autowired constructor(
    private val antiFraudClchResultService: AntiFraudClchResultService,
    private val antiFraudChecker: AntiFraudChecker
) : ClchTest() {

    @BeforeEach
    fun init() {
        antiFraudChecker.configure(CheckerDescriptor(21, "antiFraudChecker"))
        antiFraudChecker.done()
    }

    @Test
    fun `checking shop by clch res with history glue`() {
        val result = AntiFraudClchResult().apply {
            compositeId = AntiFraudClchResult.Key(SHOP_ID, CLONE_SHOP_ID)
            cloneFeatures = arrayOf(SAME_CONTACTS)
            glueContacts = AntiFraudGlueContacts().apply {
                sameContacts = arrayOf("+7777777777", "123@123.ru")
            }
            distance = DISTANCE
        }
        antiFraudClchResultService.save(listOf(result))
        flushAndClear()

        val expectedCheckValue = """
            |Сработавшие правила:
            |Исторические пересечения контактов владельцев
            |Контакты магазина:
            |+7777777777
            |123@123.ru
            |Схожесть тематики магазинов: 1.0
            |""".trimMargin()
        checkCheckerResult(expectedCheckValue)
    }

    @Test
    fun `checking shop by clch res with current glue`() {
        val result = AntiFraudClchResult().apply {
            compositeId = AntiFraudClchResult.Key(SHOP_ID, CLONE_SHOP_ID)
            cloneFeatures = arrayOf(CURRENT_GLUE, CURRENT_SAME_CONTACTS, CURRENT_SAME_JUR_INFO)
            currentGlueContacts = AntiFraudGlueContacts().apply {
                sameContacts = arrayOf("+7777777777", "123@123.ru")
                sameJurInfo = arrayOf("i543534535", "o312321312")
            }
            distance = DISTANCE
        }
        antiFraudClchResultService.save(listOf(result))
        flushAndClear()

        val expectedCheckValue = """
            |Сработавшие правила:
            |Пересечения актуальных контактов владельцев
            |Магазины имеют одинаковую юридическую информацию
            |Контакты магазина:
            |+7777777777
            |123@123.ru
            |ИНН 543534535
            |ОГРН 312321312
            |Схожесть тематики магазинов: 1.0
            |""".trimMargin()
        checkCheckerResult(expectedCheckValue)
    }

    @Test
    fun `checking shop by clch res with passport glue`() {
        val result = AntiFraudClchResult().apply {
            compositeId = AntiFraudClchResult.Key(SHOP_ID, CLONE_SHOP_ID)
            cloneFeatures = arrayOf(PASSPORT_GLUE, PASSPORT_SAME_CONTACTS)
            passportGlueContacts = AntiFraudGlueContacts().apply {
                sameContacts = arrayOf("+7777777777", "123@123.ru")
            }
            distance = DISTANCE
        }
        antiFraudClchResultService.save(listOf(result))
        flushAndClear()

        val expectedCheckValue = """
            |Сработавшие правила:
            |Пересечение паспортных контактов владельцев
            |Контакты магазина:
            |+7777777777
            |123@123.ru
            |Схожесть тематики магазинов: 1.0
            |""".trimMargin()
        checkCheckerResult(expectedCheckValue)
    }

    @Test
    fun `checking shop by clch res with passport glue with current same jur info`() {
        val result = AntiFraudClchResult().apply {
            compositeId = AntiFraudClchResult.Key(SHOP_ID, CLONE_SHOP_ID)
            cloneFeatures = arrayOf(PASSPORT_GLUE, PASSPORT_SAME_CONTACTS, CURRENT_SAME_JUR_INFO)
            passportGlueContacts = AntiFraudGlueContacts().apply {
                sameContacts = arrayOf("+7777777777", "123@123.ru")
            }
            currentGlueContacts = AntiFraudGlueContacts().apply {
                sameContacts = arrayOf("+7777777777", "123@123.ru")
                sameJurInfo = arrayOf("i543534535", "o312321312")
            }
            distance = DISTANCE
        }
        antiFraudClchResultService.save(listOf(result))
        flushAndClear()

        val expectedCheckValue = """
            |Сработавшие правила:
            |Пересечение паспортных контактов владельцев
            |Магазины имеют одинаковую юридическую информацию
            |Контакты магазина:
            |+7777777777
            |123@123.ru
            |ИНН 543534535
            |ОГРН 312321312
            |Схожесть тематики магазинов: 1.0
            |""".trimMargin()
        checkCheckerResult(expectedCheckValue)
    }

    @Test
    fun `checking shop by clch res with no priority features`() {
        val notPriorityFeatures = enumValues<AntiFraudCloneFeature>().filter { !it.priority }
        val result = AntiFraudClchResult().apply {
            compositeId = AntiFraudClchResult.Key(SHOP_ID, CLONE_SHOP_ID)
            cloneFeatures = notPriorityFeatures.toTypedArray()
            distance = DISTANCE
        }
        antiFraudClchResultService.save(listOf(result))
        flushAndClear()

        antiFraudChecker.warmUpCache(listOf(SHOP_ID, CLONE_SHOP_ID))
        assertEquals(0.0, antiFraudChecker.checkShops(SHOP_ID, CLONE_SHOP_ID).result)
        antiFraudChecker.done()
    }

    @Test
    fun `checking shop by clch res with no actual features`() {
        val notActualFeatures = enumValues<AntiFraudCloneFeature>().filter { !it.actual }
        val result = AntiFraudClchResult().apply {
            compositeId = AntiFraudClchResult.Key(SHOP_ID, CLONE_SHOP_ID)
            cloneFeatures = notActualFeatures.toTypedArray()
            distance = DISTANCE
        }
        antiFraudClchResultService.save(listOf(result))
        flushAndClear()

        val expectedCheckValue = """
            |Сработавшие правила:
            |нет актуальных правил
            |Контакты магазина:
            |нет
            |Схожесть тематики магазинов: 1.0
            |""".trimMargin()
        checkCheckerResult(expectedCheckValue)
        assertEquals(0.0, antiFraudChecker.checkShops(SHOP_ID, CLONE_SHOP_ID).result)
    }

    private fun checkCheckerResult(expectedCheckValue: String) {
        antiFraudChecker.warmUpCache(listOf(SHOP_ID, CLONE_SHOP_ID))
        val checkerResult = antiFraudChecker.checkShops(SHOP_ID, CLONE_SHOP_ID)
        print(checkerResult.value1)
    }

    companion object {
        private const val SHOP_ID = 123L
        private const val CLONE_SHOP_ID = 124L
        private const val DISTANCE = 1.0
    }
}
