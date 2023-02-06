const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-57.json');

// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('tap-checkout-57: Общее. Отображение комментария к заказу при повторном оформлении заказа с последующим его изменением', function() {
    it('Должна быть возможность изменить комментарий к заказу', async function() {
        const bro = this.browser;

        await checkoutPage.open(bro, defaultTestData);
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);
        await bro.waitForVisible(checkoutPage.orderCommentInput, 10000);

        await bro.click(checkoutPage.orderCommentInput);
        await bro.keys('Должна быть возможность изменить комментария к заказу');
        await bro.waitForVisible(checkoutPage.orderCommentInputEmpty, 1000, true);

        await bro.submitOrderAndCheckResult();

        await checkoutPage.open(bro, defaultTestData);
        await bro.handleCheckoutEvent('restoreState', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await bro.waitIconsLoad();
        await bro.assertView('orderComment', checkoutPage.root);

        await bro.click(checkoutPage.orderCommentInputButtonClear);
        await bro.waitForVisible(checkoutPage.orderCommentInputEmpty, 5000);

        await bro.keys('Отображается комментарий, который был изменен после открытия чекаута с уже заполненным полем "Комментарий к заказу"');
        await bro.waitForVisible(checkoutPage.orderCommentInputEmpty, 1000, true);

        await bro.submitOrderAndCheckResult();

        await checkoutPage.open(bro, defaultTestData);
        await bro.handleCheckoutEvent('restoreState', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await bro.waitIconsLoad();
        await bro.assertView('orderCommentAfterChange', checkoutPage.root);
    });
});
