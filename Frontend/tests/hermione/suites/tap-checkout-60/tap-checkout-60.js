const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-60.json');

describe('tap-checkout-60: Выбор даты и времени. Отображение фиксированной кнопки "Выбрать" и шапка "Дата и время доставки" при скролле страницы', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    it('При скролле страницы должны отображаться фиксированная шапка и кнопка "Выбрать"', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);

        // Ждем события выбора первого значения из списка возможных способов доставки
        await bro.handleCheckoutEvent('datetimeOptionChange', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await checkoutPage.openDeliveryTimeScreen(bro);

        await bro.click(checkoutPage.screenDateOptionsDate);
        await bro.waitForVisible(checkoutPage.screenDateOptionsDateSelected, 5000);
        await bro.waitForVisible(checkoutPage.screenTimeOptionsContainer, 5000);
        await bro.assertView('screenTimeOptionsDefault', checkoutPage.root);

        await bro.scrollTop(checkoutPage.root, 1000);
        await bro.assertView('screenTimeOptionsScrollUp', checkoutPage.root);

        await bro.scrollTop(checkoutPage.root, 0);
        await bro.assertView('screenTimeOptionsScrollDown', checkoutPage.root);
    });
});
