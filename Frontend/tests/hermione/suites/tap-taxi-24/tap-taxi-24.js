const indexPage = require('../../page-objects/index');

describe('tap-taxi-24: Главная. Отмена заказа на этапе поиска такси', function() {
    it('Заказ должен отмениться', async function() {
        const bro = this.browser;
        await bro.auth('taxi-24');
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonActivePrice, 30000);

        // Ожидаем завершения анимации появления стоимости  тарифа
        await bro.pause(1000);

        //Ждем доступность кнопки Заказать
        await bro.waitUntil(async function() {
            return await bro.getAttribute(indexPage.orderFormButtonState, 'aria-disabled') === 'false';
        });

        await bro.hideElement(indexPage.map.container);

        await bro.waitForEnabled(indexPage.orderFormButton, 5000);
        await bro.click(indexPage.orderFormButton);
        await bro.waitForVisible(indexPage.orderFormHidden, 5000, true);
        await bro.waitForVisible(indexPage.orderStatus.containerHidden, 5000, true);
        await bro.waitForVisible(indexPage.orderFormButton, 50000, true);
        await bro.waitForVisible(indexPage.orderStatus.cancelButton, 50000);

        await bro.click(indexPage.orderStatus.cancelButton);
        await bro.waitForVisible(indexPage.commonModalContent, 5000);
        await bro.assertView('cancelPopup', indexPage.commonModalContent);

        await bro.click(indexPage.commonModalConfirmButton);

        await bro.waitForVisible(indexPage.commonModalContent, 5000);
        await bro.hideElement(indexPage.orderForm);
        await bro.assertView('cancelInfoPopup', indexPage.commonModalContent);
    });
});
