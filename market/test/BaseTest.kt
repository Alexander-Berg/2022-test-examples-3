package ru.yandex.market.tpl.courier.test

import io.qameta.allure.android.runners.AllureAndroidJUnit4
import io.qameta.allure.kotlin.Allure
import org.junit.Ignore
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import ru.yandex.market.tpl.courier.arch.ext.instrumentedTestRuleManager
import ru.yandex.market.tpl.courier.domain.account.AccountCredentials
import ru.yandex.market.tpl.courier.domain.feature.auth.Uid

@RunWith(AllureAndroidJUnit4::class)
@Ignore("Base test case")
open class BaseTest() {

    val uid: Uid by lazy { accountCredentials.get().uid }

    /**
     * Получаем свободную учетку
     */
    @get:Rule(order = 0)
    val accountCredentials = instrumentedTestRuleManager.getAccountCredentials()

    @get:Rule(order = 1)
    val beforeStartRule = object : ExternalResource() {
        override fun before() {
            Allure.step("Подготовка данных до запуска активити") {
                beforeActivityStart(accountCredentials.get())
            }
        }

        override fun after() {
            Allure.step("Очистка данных после завершения теста") {
                afterActivityStop(accountCredentials.get())
            }
        }
    }

    @get:Rule(order = 2)
    val rule: TestRule = instrumentedTestRuleManager.getRule(
        getAccountCredentials = accountCredentials::get,
        prepareDataBlock = ::prepareData
    )

    /**
     * Для переопределения в тестах, выполняется после запуска активити
     */
    open fun prepareData() {
        /* no-op */
    }

    /**
     * Для переопределения в тестах, выполняется до запуска активити
     */
    open fun beforeActivityStart(credentials: AccountCredentials) {
        /* no-op */
    }

    open fun afterActivityStop(credentials: AccountCredentials) {
        /* no-op */
    }
}