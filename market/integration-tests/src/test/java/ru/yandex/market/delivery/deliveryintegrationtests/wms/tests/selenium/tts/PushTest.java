package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.tts;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;

@DisplayName("Selenium: Отправка сообщений через TTS")
@Epic("Selenium Tests")
public class PushTest extends AbstractUiTest {

    @RetryableTest
    @DisplayName("Отправка сообщений через TTS")
    @ResourceLock("Отправка сообщений через TTS")
    void pushTest() {
        String modalText;
        uiSteps.Login().PerformLogin();
        uiSteps.SupervisorActivity().startDinnerActivity();
        uiSteps.Login().PerformLogin();
        uiSteps.SupervisorActivity().checkUserStatus("НПО");
        modalText = uiSteps.SupervisorActivity().sendPraisingMessageToFirstUser();
        uiSteps.SupervisorActivity().receiveMessage(modalText);
        uiSteps.Login().PerformLogin();
        uiSteps.SupervisorActivity().endDinnerActivity();
        uiSteps.Login().PerformLogin();
        uiSteps.SupervisorActivity().checkUserStatus("Неизвестно");
    }

    @AfterEach
    @Step("Удаление данных: Очистка уведомлений")
    public void tearDown() {
        DatacreatorSteps.TtsNotifications().deleteTtsNotifications(user.getLogin());
    }
}

