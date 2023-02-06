const indexPage = require('../../page-objects/index');

describe('tap-taxi-54: Главная. Заказа такси, когда геопозиция пользователя неизвестна', function() {
    hermione.config.testTimeout(90000);
    hermione.only.in('chrome-no-geolocation');
    hermione.also.in('chrome-no-geolocation');
    it('Полное флоу заказа такси должно пройти без ошибок', async function() {
        const bro = this.browser;

        await bro.auth('taxi-54');
        await indexPage.open(bro, { comment: 'search-0,wait-0,speed-2000', gfrom: '55.736916,37.641769', center: '55.736916,37.641769' });
        await bro.waitForVisible(indexPage.addressPoint.porchButton, 20000);
        await bro.waitForVisible(indexPage.topNotification, 5000);

        await indexPage.addressPoint.clickPositionFirst(bro);
        await bro.waitForVisible(indexPage.map.addressToPlacemark, 5000);

        await bro.waitForVisible(indexPage.orderFormButtonDisabled, 20000, true);
        await bro.click(indexPage.orderFormButton);
        await bro.waitForVisible(indexPage.orderStatus.containerHidden, 5000, true);
        await bro.waitForVisible(indexPage.topNotification, 5000);
        await bro.waitForVisible(indexPage.orderStatus.drivingViewInfo, 40000);
        await bro.waitForVisible(indexPage.orderStatus.completeViewInfo, 40000);
        await bro.waitForVisible(indexPage.orderStatus.completeButton, 5000);

        await bro.click(indexPage.orderStatus.completeButton);
        await bro.waitForVisible(indexPage.topNotification, 5000);
        await bro.waitForVisible(indexPage.tariff.buttonActivePrice, 5000);
        // Ждем загрузки цен
        await bro.pause(2000);
        await bro.hideElement(indexPage.map.container);
        await bro.assertView('formSearchAfterTripFinished', indexPage.orderForm);
    });
});
