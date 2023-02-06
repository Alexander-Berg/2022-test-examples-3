const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-102.json');
const errorTestData = require('./tap-checkout-error-102.json');

describe('tap-checkout-102: Доставка. Отображение разной длины ошибки под блоком выбора города', function() {
    beforeEach(function() {
        this.defaultData = cloneDeep(defaultTestData);
        this.errorData = cloneDeep(errorTestData);
    });

    hermione.also.in(['chrome-grid-320', 'chrome-grid-414']);
    it('Должна корректно отображаться ошибка в две строки, которая расположена под блоком выбора города', async function() {
        const bro = this.browser;
        const errorData = this.errorData;

        await openCheckoutAndSelectionCity(this);

        await bro.handleCheckoutEvent('cityChange', errorData);
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);
        await bro.assertView('deliveryCityError', checkoutPage.root);
    });

    hermione.also.in(['chrome-grid-320', 'chrome-grid-414']);
    it('Должна корректно отображаться ошибка в три строки, которая расположена под блоком выбора города', async function() {
        const bro = this.browser;
        const errorData = this.errorData;

        await openCheckoutAndSelectionCity(this);

        const context = await bro.executionContext;
        if (context.browserId === 'chrome-grid-414') {
            errorData.validationErrors.orders[0].city = 'Проверка отображения ошибки в блоке выбора города при ответе на событие "cityChange". Ошибка должна отображаться в три строки';
        } else {
            errorData.validationErrors.orders[0].city = 'Проверка отображения ошибки в блоке выбора города. Ошибка должна отображаться в три строки';
        }

        await bro.handleCheckoutEvent('cityChange', errorData);
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);
        await bro.assertView('deliveryCityError', checkoutPage.root);
    });

    hermione.also.in(['chrome-grid-320', 'chrome-grid-414']);
    it('Должна корректно отображаться ошибка в четыре строки, которая расположена под блоком выбора города', async function() {
        const bro = this.browser;
        const errorData = this.errorData;

        await openCheckoutAndSelectionCity(this);

        const context = await bro.executionContext;
        if (context.browserId === 'chrome-grid-414') {
            errorData.validationErrors.orders[0].city = 'Проверка отображения ошибки в блоке выбора города при ответе на событие "cityChange". Ошибка должна быть очень длинной и должна отображаться в четыре строки';
        } else {
            errorData.validationErrors.orders[0].city = 'Проверка отображения ошибки в блоке выбора города. Ошибка должна быть очень длинной и должна отображаться в четыре строки';
        }

        await bro.handleCheckoutEvent('cityChange', errorData);
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);
        await bro.assertView('deliveryCityError', checkoutPage.root);
    });

    async function openCheckoutAndSelectionCity(ctx) {
        const bro = ctx.browser;
        const testData = ctx.defaultData;

        await checkoutPage.open(bro, testData);

        await bro.waitForVisible(checkoutPage.deliveryCityButton, 5000);

        await checkoutPage.selectCityFromDefaultList(bro, 'Москва');
    }
});
