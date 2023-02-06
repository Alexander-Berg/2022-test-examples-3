const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-26.json');

describe('tap-checkout-26: Выбор города. Отображение информации на экране при первом открытии', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    hermione.also.in(['chrome-grid-320', 'chrome-grid-414']);
    it('На экране "Выбор города" должен отображаться список дефолтных городов и поле поиска', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);
        await bro.waitForVisible(checkoutPage.deliveryCityButton, 5000);

        await checkoutPage.openCityScreen(bro);

        await bro.waitIconsLoad();
        await bro.assertView('screenCityDefault', checkoutPage.root);

        await bro.scrollTop(checkoutPage.root, 1000);
        await bro.assertView('screenCityDefaultScrollDown', checkoutPage.root);
    });
});
