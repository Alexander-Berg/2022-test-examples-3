const assert = require('assert');

const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-105.json');

describe('tap-checkout-105: Выбор города. Изменение ранее выбранного города ', function() {
    beforeEach(function() {
        this.defaultData = cloneDeep(defaultTestData);
    });

    it('Должна быть возможность изменить ранее выбранный город', async function() {
        const bro = this.browser;
        const testData = this.defaultData;

        await checkoutPage.open(bro, testData);
        await bro.waitForVisible(checkoutPage.deliveryCityButton, 5000);

        await selectCity(bro, 'Ханты-Мансийск');
        await selectCity(bro, 'Екатеринбург');

        await checkoutPage.openCityScreen(bro);
        const currentSearchMessage = await bro.getAttribute(checkoutPage.screenCitySearchInput, 'value');
        assert.strictEqual(currentSearchMessage, '', `Поле поиска должно быть пустым, а в нем отображается текст "${currentSearchMessage}"`);
    });

    async function selectCity(bro, cityName) {
        await checkoutPage.openCityScreen(bro);
        await checkoutPage.fillCitySearchInput(bro, cityName);
        await bro.waitForVisible(checkoutPage.screenCitySearchResultsItem, 10000);

        await checkoutPage.clickCityInSearchResult(bro, cityName);
        await bro.handleCheckoutEvent('cityChange', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        const deliveryCityButtonCurrentName = await bro.getText(checkoutPage.deliveryCityButton);
        assert.strictEqual(deliveryCityButtonCurrentName, cityName, `На главном экране должно отображаться название города "${cityName}", а отображается "${deliveryCityButtonCurrentName}"`);
    }
});
