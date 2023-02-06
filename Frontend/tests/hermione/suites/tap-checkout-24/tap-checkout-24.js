const assert = require('assert');

const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-24.json');

describe('tap-checkout-24: Выбор города. Открытие экрана после ввода некорректного значения ', function() {
    hermione.only.in('chrome-ignore-permission-geolocation');
    hermione.also.in('chrome-ignore-permission-geolocation');
    it('Поле ввода должно отображаться пустым при повторном открытии экрана "Выбор города"', async function() {
        const bro = this.browser;

        await checkoutPage.open(bro, defaultTestData);
        await bro.waitForVisible(checkoutPage.deliveryCityButton, 5000);

        await checkoutPage.openCityScreen(bro);
        await checkoutPage.fillCitySearchInput(bro, 'не найдется ни один город');
        await bro.waitForVisible(checkoutPage.screenCitySearchResultsEmpty, 10000);

        await bro.assertViewAfterLockFocusAndHover('deliveryCityResultsEmpty', checkoutPage.root);

        await bro.click(checkoutPage.headerBackButton);
        await bro.waitForVisible(checkoutPage.screenCitySearchInput, 5000, true);
        await bro.waitForVisible(checkoutPage.deliveryCityButton, 5000);

        const deliveryCityButtonCurrentName = await bro.getText(checkoutPage.deliveryCityButton);
        assert.strictEqual(deliveryCityButtonCurrentName, 'Выбрать город', `В перехода к экрану "Выбрать город" должен отображаться текст "Выбрать город", а отображается "${deliveryCityButtonCurrentName}"`);

        await checkoutPage.openCityScreen(bro);
        const searchMessageCurrent = await bro.getAttribute(checkoutPage.screenCitySearchInput, 'value');
        assert.strictEqual(searchMessageCurrent, '', `Поле поиска должно быть пустым, а в нем отображается текст "${searchMessageCurrent}"`);
    });
});
