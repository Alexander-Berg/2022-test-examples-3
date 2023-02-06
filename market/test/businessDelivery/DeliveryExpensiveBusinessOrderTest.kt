package ru.yandex.market.tpl.courier.test.businessDelivery

import androidx.test.filters.LargeTest
import com.agoda.kakao.screen.Screen
import io.qameta.allure.kotlin.Allure
import io.qameta.allure.kotlin.Description
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.Issue
import io.qameta.allure.kotlin.Story
import io.qameta.allure.kotlin.TmsLink
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test
import ru.yandex.market.tpl.courier.arch.ext.swipeUp
import ru.yandex.market.tpl.courier.arch.ext.testDataRepository
import ru.yandex.market.tpl.courier.arch.ext.wait
import ru.yandex.market.tpl.courier.arch.fp.Exceptional
import ru.yandex.market.tpl.courier.arch.fp.orThrow
import ru.yandex.market.tpl.courier.arch.fp.success
import ru.yandex.market.tpl.courier.domain.feature.point.RoutePointId
import ru.yandex.market.tpl.courier.domain.feature.task.OrderDeliveryTask
import ru.yandex.market.tpl.courier.domain.feature.task.delivery.PaymentType
import ru.yandex.market.tpl.courier.presentation.feature.screen.DeliveryDashboardScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.DeliveryScanScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MultiOrderScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.PassCodeScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.PhotoViewerScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.TakePhotoScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.addPhotoButton
import ru.yandex.market.tpl.courier.presentation.feature.screen.orderPhoto
import ru.yandex.market.tpl.courier.test.BaseTest
import java.lang.IllegalStateException

@LargeTest
@Epic("Доставка для бизнеса")
@DisplayName("Доставка для бизнеса")
class DeliveryExpensiveBusinessOrderTest: BaseTest() {

    private val passCode: String = "12345"
    private lateinit var orderId: String

    private fun prepareDeliveryTasks(): Exceptional<RoutePointId> {
        testDataRepository.createShift(uid)
        val routePoint = testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000000",
            isPaid = true,
            paymentType = PaymentType.Prepaid,
            itemsPrice = 60000,
            recipientNotes = "Бизнес",
            isBusinessDelivery = true,
            verificationCode = passCode,
        )

        val rotePointId = routePoint.id
        val tasks = routePoint.tasks.toList() as List<OrderDeliveryTask>

        if (tasks.isEmpty())
            throw IllegalStateException("Нет заданий на точке, хотя мы их создавали!")

        orderId = tasks.first().orderId.unwrap()

        testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000001",
            isPaid = false,
            paymentType = PaymentType.CreditCard
        )

        return success(rotePointId)
    }

    override fun prepareData() {
        val routePointId = prepareDeliveryTasks().orThrow()

        testDataRepository.checkIn()
        testDataRepository.manualFinishPickup(uid)
        testDataRepository.manualSwitchRoutePoint(routePointId, uid)
        testDataRepository.arriveAtRoutePoint(routePointId)
    }

    @Test
    @Issue("MARKETTPLAPP-1373")
    @TmsLink("courier-app-238")
    @Story("Простой b2b заказ")
    @DisplayName("Обязательность фото к одиночному b2b заказу стоимостью более 50к")
    @Description("Выдача одиночного предоплаченного b2b заказа на сумму более 50 000р")
    fun deliveryExpensiveBusinessOrderTest() {
        Screen.onScreen<MainScreen> {
            val orderView = orderCard(orderId)

            Allure.step("Дождаться появление заказа в дровере") {
                wait until {
                    orderView {
                        isVisible()
                    }
                }
            }

            Allure.step("Проскролить дровер до заказа") {
                swipeUp { rootSwipeView } until {
                    orderView { isCompletelyDisplayed() }
                }
            }

            Allure.step("Нажать на заказ в дровере") {
                orderView { click() }
            }
        }

        Screen.onScreen<MultiOrderScreen> {
            Allure.step("Дождаться появления карточки заказа") {
                wait until {
                    rootSwipeView { isVisible() }
                }
            }

            Allure.step("Проскролить экран до кнопки выдачи заказа") {
                swipeUp { rootSwipeView } until {
                    giveOutOrderButton { isCompletelyDisplayed() }
                }

                Allure.step("Нажать на кнопку выдачи заказа") {
                    giveOutOrderButton { click() }
                }
            }
        }

        Screen.onScreen<PassCodeScreen> {
            Allure.step("Дождаться появления экрана ввода кода") {
                wait until {
                    passCodeContainer { isVisible() }
                }
            }

            Allure.step("Проверить наличие инпутов для ввода кода") {
                passCode.forEachIndexed { index, _ ->
                    val input = passCodeInput(index)

                    input { isCompletelyDisplayed() }
                }
            }

            Allure.step("Вводим код") {
                passCode.forEachIndexed { index, code ->
                    val input = passCodeInput(index)

                    input.typeText(code.toString())
                }
            }
        }

        Screen.onScreen<DeliveryScanScreen> {
            waitUntilDrawerAndManualInputButtonIsDisplayed()

            openDrawerAndWaitUntilOpened()

            checkThatOrdersIsNotScanned(orderIds = listOf(orderId))

            closeDrawerAndWaitUntilClosed()

            manuallyScanOrderId(orderId)
        }

        Screen.onScreen<TakePhotoScreen> {
            Allure.step("Дождаться появления экрана добавления фотографии") {
                wait until {
                    takePhotoButton { isVisible() }
                }
            }

            Allure.step("Проверить наличие всех кнопок на экране") {
                takePhotoButton { isCompletelyDisplayed() }
                extraButton { isCompletelyDisplayed() }
                galleryButton { isCompletelyDisplayed() }
                flashButton { isCompletelyDisplayed() }
            }

            Allure.step("Делаем фотку") {
                takePhotoButton { click() }
            }
        }

        Screen.onScreen<DeliveryDashboardScreen> {
            Allure.step("Дождаться открытия дашборда выдачи") {
                wait until {
                    contentRecyclerView {
                        isDisplayed()
                    }
                }
            }

            Allure.step("Кнопки \"Добавить фото\", \"Подтвердить выдачу\", \"Закрыть\" (крестик) и карточка заказа присутствуют на экране") {
                confirmDeliveryButton {
                    isCompletelyDisplayed()
                    isEnabled()
                }

                heresyIcon {
                    isCompletelyDisplayed()
                    isEnabled()
                }

                contentRecyclerView.addPhotoButton {
                    isCompletelyDisplayed()
                    isEnabled()
                }
            }

            Allure.step("На экране есть фотка и возможность удалить ее") {
                contentRecyclerView.orderPhoto {
                    isCompletelyDisplayed()

                    photo { isCompletelyDisplayed() }
                    closeButton { isCompletelyDisplayed() }
                }
            }

            Allure.step("Проваливаемся в фотку") {
                contentRecyclerView.orderPhoto {
                    photo { click() }
                }
            }
        }

        Screen.onScreen<PhotoViewerScreen> {
            Allure.step("Дождаться открытия экрана просмотра фото") {
                wait until {
                    viewPager {
                        isCompletelyDisplayed()
                    }
                }
            }

            Allure.step("Кнопки удаления нет, нет таба с фотками") {
                tabLayout { isGone() }

                heresyIcon { doesNotExist() }

                backIcon { isCompletelyDisplayed() }
            }

            Allure.step("Возвращаемся на дашборд") {
                backIcon { click() }
            }
        }

        Screen.onScreen<DeliveryDashboardScreen> {
            Allure.step("Дождаться открытия дашборда выдачи") {
                wait until {
                    contentRecyclerView {
                        isDisplayed()
                    }
                }
            }

            Allure.step("Нажать на кнопку \"Подтвердить выдачу\"") {
                confirmDeliveryButton { click() }
            }
        }

        Screen.onScreen<MainScreen> {
            Allure.step("Дождаться, когда задание пропадет из bottom sheet'а") {
                wait until {
                    viewContainsText(orderId) perform { doesNotExist() }
                }
            }
        }
    }
}