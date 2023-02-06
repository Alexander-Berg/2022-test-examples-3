const indexPage = require('../../page-objects/index');
const paymentPage = require('../../page-objects/payment');

describe('tap-taxi-366: Главная. Отображении сообщения "Только безналичная оплата" с последующим изменением способа оплаты', function() {
    it('Только безналичная оплата в тарифе Élite', async function() {
        const bro = this.browser;
        await bro.auth('taxi-366');
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonActivePrice, 30000);

        // Ожидаем завершения анимации появления стоимости тарифа
        await bro.pause(1000);

        await bro.hideElement(indexPage.map.container);

        await indexPage.tariff.clickButton(bro, 'Élite');
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 5000);
        // Ждем доскролл тарифов
        await bro.pause(1000);
        await bro.assertView('indexForm', indexPage.orderForm);

        await bro.waitForVisible(indexPage.orderFormButtonDisabled, 5000);

        // Проверяем, что после смены способа оплаты на карту заказ создается
        await bro.click(paymentPage.paymentButton);
        await bro.waitForVisible(paymentPage.modal, 5000);

        // TODO: Проблема с верификацие карт, в тестовом окружении карта не приходит
        /*
        await bro.click(paymentPage.paymentMethod);

        await bro.waitForVisible(indexPage.tariff.buttonActivePrice, 30000);
        await bro.pause(1000);
        await bro.click(indexPage.orderFormButton);
        await bro.waitForVisible(indexPage.orderFormHidden, 5000, true);
        await bro.waitForVisible(indexPage.orderStatus.containerHidden, 5000, true);
        await bro.waitForVisible(indexPage.orderFormButton, 50000, true);
        */
    });
});
