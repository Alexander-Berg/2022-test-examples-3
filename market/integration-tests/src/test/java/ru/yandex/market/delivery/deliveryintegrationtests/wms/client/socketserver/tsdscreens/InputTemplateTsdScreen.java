package ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens;

import io.qameta.allure.Step;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.AbstractTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.TsdEmulator;

/**
 * Экран ТСД: Ввод шаблона проверки атрибутов
 */
public class InputTemplateTsdScreen extends AbstractTsdScreen {

    public InputTemplateTsdScreen(TsdEmulator tsdEmulator) {
        super(tsdEmulator);
    }

    @Step("Открыт экран ТСД: Ввод шаблона проверки атрибутов - Вводим шаблон")
    public void enter(String template) {
        TsdEmulator tsd = getTsdEmulator();

        tsd.setSessionValue("template", template);

        tsd.setCurrentScreen(new InputExpirationsTsdScreen(tsd));
    }
}
