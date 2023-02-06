const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-23.json');

describe('tap-checkout-23: Доставка. Переход на экран "Выбор города" с возвратом назад', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    it('Должна быть возможность вернуться на основной экран с экрана "Выбор города"', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);

        await bro.waitForVisible(checkoutPage.deliveryCityButton, 5000);

        await checkoutPage.openCityScreen(bro);
        await bro.click(checkoutPage.headerBackButton);
        await bro.waitForVisible(checkoutPage.screenCitySearchInput, 5000, true);
        await bro.waitForVisible(checkoutPage.deliveryCityButton, 5000);

        await checkoutPage.openCityScreen(bro);
        await bro.back();

        await bro.waitForVisible(checkoutPage.screenCitySearchInput, 5000, true);
        await bro.waitForVisible(checkoutPage.deliveryCityButton, 5000);
    });
});
