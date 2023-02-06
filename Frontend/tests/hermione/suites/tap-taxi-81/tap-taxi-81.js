const indexPage = require('../../page-objects/index');

describe('tap-taxi-81: Главная. Переход по ссылке "Пользовательское соглашение" в шторке "Детали поездки', function() {
    it('Должна отображаться информация', async function() {
        const bro = this.browser;
        await bro.auth('taxi-81');
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonActivePrice, 30000);

        // Ожидаем завершения анимации появления стоимости тарифа
        await bro.pause(1000);

        await bro.click(indexPage.orderFormButton);
        await bro.waitForVisible(indexPage.orderFormHidden, 5000, true);
        await bro.waitForVisible(indexPage.orderStatus.containerHidden, 5000, true);
        await bro.waitForVisible(indexPage.orderStatus.container, 30000);
        await bro.swipeUp(indexPage.orderStatus.container, 1000);
        await bro.waitForVisible(indexPage.orderStatus.details, 5000);

        await bro.click(indexPage.orderStatus.details);
        await bro.waitForVisible(indexPage.orderStatus.detailsModalRules, 5000);

        await bro.hideElement(indexPage.map.container);

        // Ждём, когда найдётся машина
        await bro.waitForVisible(indexPage.orderStatus.drivingViewInfo, 30000);
        await bro.assertView('details-modal-part1', indexPage.root);

        await bro.swipeUp(indexPage.orderStatus.detailsModalScroll, 500);
        await bro.assertView('details-modal-part2', indexPage.root);
    });
});
