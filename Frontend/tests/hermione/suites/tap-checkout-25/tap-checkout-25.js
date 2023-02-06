const assert = require('assert');

const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-25.json');

// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('tap-checkout-25: Выбор города. Поиск города по названию с повторным открытием экрана выбора города', function() {
    beforeEach(function() {
        this.defaultData = cloneDeep(defaultTestData);
    });

    it('На главном экране должен отображаться город, который был выбран в результатах поиска города', async function() {
        const bro = this.browser;
        const testData = this.defaultData;

        await checkoutPage.open(bro, testData);
        await bro.waitForVisible(checkoutPage.deliveryCityButton, 5000);

        await checkoutPage.openCityScreen(bro);
        await checkoutPage.fillCitySearchInput(bro, 'ханты-Мансийск');
        await bro.waitForVisible(checkoutPage.screenCitySearchResultsItem, 10000);
        await bro.assertViewAfterLockFocusAndHover('deliveryCityResults', checkoutPage.root);

        await checkoutPage.clickCityInSearchResult(bro, 'Ханты-Мансийск');
        await bro.handleCheckoutEvent('cityChange', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        const deliveryCityButtonCurrentName = await bro.getText(checkoutPage.deliveryCityButton);
        assert.strictEqual(deliveryCityButtonCurrentName, 'Ханты-Мансийск', `На главном экране должно отображаться название города "Ханты-Мансийск", а отображается "${deliveryCityButtonCurrentName}"`);

        await checkoutPage.openCityScreen(bro);
        const currentSearchMessage = await bro.getAttribute(checkoutPage.screenCitySearchInput, 'value');
        assert.strictEqual(currentSearchMessage, '', `Поле поиска должно быть пустым, а в нем отображается текст "${currentSearchMessage}"`);
    });
});
