const assert = require('assert');

const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-81.json');

describe('tap-checkout-81: Выбор города. Очищение поля ввода по нажатию на кнопку "Х"', function() {
    beforeEach(function() {
        this.defaultData = cloneDeep(defaultTestData);
    });

    it('По нажатию на кнопку "Х" поле ввода должно очищаться', async function() {
        const bro = this.browser;
        const testData = this.defaultData;

        await checkoutPage.open(bro, testData);

        await bro.waitForVisible(checkoutPage.deliveryCityButton, 5000);

        await checkoutPage.openCityScreen(bro);
        await checkoutPage.fillCitySearchInput(bro, 'Проверка очищения поля');
        await bro.waitForVisible(checkoutPage.screenCitySearchResultsEmpty, 5000);
        await bro.waitForVisible(checkoutPage.screenCitySearchInputButtonClear, 5000);

        await bro.click(checkoutPage.screenCitySearchInputButtonClear);
        await bro.waitForVisible(checkoutPage.screenCitySearchInputButtonClear, 5000, true);
        await bro.waitForVisible(checkoutPage.screenCitySearchResultsEmpty, 5000, true);
        await bro.waitForVisible(checkoutPage.screenCitySearchResultsItem, 5000);

        const searchMessageCurrent = await bro.getAttribute(checkoutPage.screenCitySearchInput, 'value');
        assert.strictEqual(searchMessageCurrent, '', `Поле поиска должно быть пустым, а в нем отображается текст "${searchMessageCurrent}"`);
    });
});
