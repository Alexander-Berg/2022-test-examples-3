const indexPage = require('../../page-objects/index');

describe('tap-taxi-78: Главная. Закрытие шторки "Детали поездки" смахиванием вниз', function() {
    it('Шторка должна закрыться ', async function() {
        const bro = this.browser;
        await bro.auth('taxi-78');
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonActivePriceNonEmpty, 30000);

        // Ожидаем завершения анимации появления стоимости тарифа
        await bro.pause(1000);

        await bro.click(indexPage.orderFormButton);
        await bro.waitForVisible(indexPage.orderFormHidden, 5000, true);
        await bro.waitForVisible(indexPage.orderStatus.containerHidden, 5000, true);
        await bro.waitForVisible(indexPage.orderStatus.container, 30000);
        await bro.pause(1000);
        await bro.swipeUp(indexPage.orderStatus.container, 1000);
        await bro.pause(1000);
        await bro.waitForVisible(indexPage.orderStatus.details, 5000);

        await bro.click(indexPage.orderStatus.details);
        await bro.waitForVisible(indexPage.orderStatus.detailsModal, 5000);

        await bro.swipeDown(indexPage.orderStatus.detailsModal, 1000);
        await bro.pause(1000);
        await bro.waitForVisible(indexPage.orderStatus.detailsModal, 5000, true);
        await bro.waitForVisible(indexPage.orderStatus.cancelButton, 5000);
    });
});
