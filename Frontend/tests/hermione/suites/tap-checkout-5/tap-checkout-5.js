const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-5.json');

describe('tap-checkout-5: Список товаров. Отображение информации на экране "Ваш заказ', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    it('На экране "Ваш заказ" должна отображаться полная информация о заказе', async function() {
        const bro = this.browser;

        await openCheckoutProductList(this);
        await bro.assertView('tap-checkout-5-productList', checkoutPage.root);
    });

    it('На экране "Ваш заказ" должно отображаться длинное описание доп.информации товара', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        testData.orders[0].cartDetails[1].label = 'Длинное описание дополнительной информации корзины';

        await openCheckoutProductList(this);
        await bro.assertView('tap-checkout-5-productList', checkoutPage.root);
    });

    it('На экране "Ваш заказ" должна отображаться информация только с весом заказа', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        delete testData.orders[0].cartDetails[0].label;
        delete testData.orders[0].cartDetails[0].amount;
        delete testData.orders[0].cartDetails[2].label;
        delete testData.orders[0].cartDetails[2].amount;

        await openCheckoutProductList(this);
        await bro.assertView('tap-checkout-5-productList', checkoutPage.root);
    });

    it('На экране "Ваш заказ" должна отображаться информация только о скидке товара', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        delete testData.orders[0].cartDetails[0].label;
        delete testData.orders[0].cartDetails[0].amount;
        delete testData.orders[0].cartDetails[1].label;
        delete testData.orders[0].cartDetails[1].value;

        await openCheckoutProductList(this);
        await bro.assertView('tap-checkout-5-productList', checkoutPage.root);
    });

    async function openCheckoutProductList(ctx) {
        const bro = ctx.browser;
        const testData = ctx.currentTest.testData;

        await checkoutPage.open(bro, testData);

        await bro.waitForVisible(checkoutPage.productAllButton, 5000);

        await checkoutPage.clickProductAllButton(bro);
    }
});
