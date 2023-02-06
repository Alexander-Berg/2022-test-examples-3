const indexPage = require('../../page-objects/index');

// Флапает
// eslint-disable-next-line mocha/no-skipped-tests
describe('tap-taxi-1: Главная. Заказ такси с полным маршрутом', function() {
    hermione.config.testTimeout(90000);
    it('Полное флоу заказа такси должно пройти без ошибок', async function() {
        const bro = this.browser;
        await bro.auth('taxi-1');

        await indexPage.open(bro, { comment: 'search-0,wait-0,speed-2000' });
        await bro.waitForVisible(indexPage.addressPoint.porchButton, 20000);

        await indexPage.addressPoint.clickPositionFirst(bro);
        await bro.waitForVisible(indexPage.map.addressToPlacemark, 5000);
        await bro.waitForVisible(indexPage.addressPoint.time, 10000);

        await bro.click(indexPage.orderFormButton);
        await bro.pause(2000);
        await bro.waitForVisible(indexPage.orderStatus.containerHidden, 5000, true);
        await bro.hideElement(indexPage.map.container);
        await bro.assertView('formSearch', indexPage.orderForm, {
            ignoreElements: [indexPage.headerTitle, indexPage.headerTimer, indexPage.headerProgress],
            screenshotDelay: 5,
        });

        await bro.waitForVisible(indexPage.orderStatus.drivingViewInfo, 30000);
        await bro.hideElement(indexPage.map.container);
        await bro.assertView('formDrivingView', indexPage.orderForm);

        await bro.waitForVisible(indexPage.orderStatus.tripRating, 30000);
        await bro.assertView('formTrip', indexPage.orderForm);

        await bro.waitForVisible(indexPage.orderStatus.completeButton, 30000);
        await bro.hideElement(indexPage.map.container);
        await bro.assertView('formFinished', indexPage.orderStatus.container);

        await bro.click(indexPage.orderStatus.completeButton);

        await bro.waitForVisible(indexPage.tariff.buttonActivePrice, 5000);
        await bro.waitForVisible(indexPage.tariff.buttonActivePriceNonEmpty, 5000);
        await bro.assertView('formSearchAfterTripFinished', indexPage.orderForm);
    });
});
