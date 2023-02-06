const indexPage = require('../../page-objects/index');

describe('tap-taxi-58: Главная. Нажатие на кнопку "Нет" в попапе отмены заказа', function() {
    it('Заказ не должен отмениться', async function() {
        const bro = this.browser;
        await bro.auth('taxi-58');
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.addressPoint.porchButton, 30000);

        // Ожидаем завершения анимации появления стоимости тарифа
        await bro.pause(2000);

        await bro.waitForEnabled(indexPage.orderFormButton, 5000);
        await bro.click(indexPage.orderFormButton);
        await bro.waitForVisible(indexPage.orderFormHidden, 5000, true);
        await bro.waitForVisible(indexPage.orderStatus.containerHidden, 5000, true);

        await bro.waitForVisible(indexPage.orderStatus.cancelButton, 5000);

        await bro.pause(2000);
        await bro.click(indexPage.orderStatus.cancelButton);
        await bro.waitForVisible(indexPage.commonModalContent, 5000);

        await bro.click(indexPage.commonModalCancelButton);
        await bro.waitForVisible(indexPage.commonModalContent, 5000, true);
        await bro.waitForVisible(indexPage.orderStatus.cancelButton);
    });
});
