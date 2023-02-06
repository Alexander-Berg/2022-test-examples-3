const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-96.json');

describe('tap-checkout-96: Основное. Повторное оформление заказа неавторизованным пользователем', function() {
    it('При повторном открытии чекаут должен корректно отображаться', async function() {
        const bro = this.browser;
        await checkoutPage.open(bro, defaultTestData);

        await checkoutPage.searchAndFillInput(bro, 'name', 'Name and Surname');
        await checkoutPage.searchAndFillInput(bro, 'phone', '89123123121');
        await checkoutPage.searchAndFillInput(bro, 'email', 'email@example.com');

        await checkoutPage.selectCityFromDefaultList(bro, 'Санкт-Петербург');
        await bro.handleCheckoutEvent('cityChange', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        const street = 'Невский проспект, 88';
        await checkoutPage.searchAndClearFillInput(bro, 'address', street);
        await bro.waitForVisible(checkoutPage.deliveryAddressSuggestLoader, 5000);
        await bro.waitForVisible(checkoutPage.deliveryAddressSuggestLoader, 20000, true);

        await checkoutPage.clickAddressInSearchResult(bro, street);
        await bro.waitForVisible(checkoutPage.pageLoader, 5000);

        await bro.handleCheckoutEvent('shippingAddressChange', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await checkoutPage.searchAndClearFillInput(bro, 'flat', '12');
        await checkoutPage.searchAndClearFillInput(bro, 'porch', '2');
        await checkoutPage.searchAndClearFillInput(bro, 'floor', '32');
        await checkoutPage.searchAndClearFillInput(bro, 'intercom', '321');

        await bro.submitOrderAndCheckResult();

        await checkoutPage.open(bro, defaultTestData);
        await bro.waitIconsLoad();
        await bro.waitForVisible(checkoutPage.pageLoader, 5000);

        await bro.handleCheckoutEvent('restoreState', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000);

        await bro.handleCheckoutEvent('shippingAddressChange', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);
        await bro.assertView('checkoutAfterOpenPart1', checkoutPage.root);

        await bro.scrollTop(checkoutPage.root, 1000);
        await bro.assertView('checkoutAfterOpenPart2', checkoutPage.root);
    });
});
