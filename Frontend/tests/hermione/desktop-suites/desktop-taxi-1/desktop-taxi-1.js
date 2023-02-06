const indexPage = require('../../page-objects/index');

describe('desktop-taxi-1: Главная. Заказ такси с полным маршрутом', function() {
    hermione.config.testTimeout(90000);
    it('Полное флоу заказа такси должно пройти без ошибок', async function() {
        const bro = this.browser;
        await bro.auth('taxi-1');

        await indexPage.open(bro, { comment: 'search-0,wait-10,speed-3000' });
        await bro.waitForVisible(indexPage.tariff.buttonActivePrice, 50000);
        await bro.waitForVisible(indexPage.tariff.buttonActivePriceNonEmpty, 20000);
        await bro.hideElement(indexPage.map.container);

        await indexPage.addressPoint.clickSuggestPositionFirst(bro);
        await bro.waitForVisible(indexPage.tariff.buttonActivePriceNonEmpty, 20000);

        await bro.pause(1000);
        //Ждем доступность кнопки Заказать
        await bro.waitUntil(async function() {
            return await bro.getAttribute(indexPage.orderFormButtonState, 'aria-disabled') === 'false';
        }, 10000);

        await bro.click(indexPage.orderFormButton);
        await bro.waitForVisible(indexPage.orderFormHidden, 5000, true);
        await bro.waitForVisible(indexPage.orderStatus.containerHidden, 5000, true);
        await bro.waitForVisible(indexPage.orderFormButton, 50000, true);
        await bro.waitForVisible(indexPage.orderStatus.searchViewInfo, 50000);
        await bro.pause(2000);
        await bro.assertView('formSearch', indexPage.orderForm, { ignoreElements: [indexPage.headerContainer] });

        await bro.waitForVisible(indexPage.orderStatus.drivingViewInfo, 30000);
        await bro.hideElement(indexPage.map.container);
        await bro.assertView('formDrivingView', indexPage.orderStatus.orderStatusView);

        await bro.waitForVisible(indexPage.orderStatus.tripRating, 30000);
        await bro.assertView('formTrip', indexPage.orderForm);

        await bro.waitForVisible(indexPage.orderStatus.completeButton, 30000);
        await bro.hideElement(indexPage.map.container);
        await bro.assertView('formFinished', indexPage.orderStatus.container);

        await bro.click(indexPage.orderStatus.completeButton);

        await bro.waitForVisible(indexPage.tariff.buttonActivePrice, 10000);
        await bro.waitForVisible(indexPage.tariff.buttonActivePriceNonEmpty, 10000);
        await bro.assertView('formSearchAfterTripFinished', indexPage.orderForm);
    });
});
