const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-62.json');

describe('tap-checkout-62: Доставка. Отображение ранее добавленного адреса при условии, что после добавления адреса не был осуществлен успешный заказ', function() {
    it('Должен быть заполнена форма адреса', async function() {
        const bro = this.browser;
        await bro.auth('tap-checkout-62');

        await checkoutPage.open(bro, defaultTestData);
        await bro.waitForVisible(checkoutPage.deliveryAddressForm, 5000);

        await checkoutPage.selectCityFromDefaultList(bro, 'Екатеринбург');
        await bro.handleCheckoutEvent('cityChange', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        const street = 'улица Хохрякова, 10';
        await checkoutPage.searchAndFillInput(bro, 'address', street);
        await bro.waitForVisible(checkoutPage.deliveryAddressSuggestLoader, 5000, true);

        await checkoutPage.clickAddressInSearchResult(bro, street);
        await bro.waitForVisible(checkoutPage.pageLoader, 5000);
        await bro.handleCheckoutEvent('shippingAddressChange', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await checkoutPage.searchAndFillInput(bro, 'flat', '12123');
        await checkoutPage.searchAndFillInput(bro, 'porch', '1');
        await checkoutPage.searchAndFillInput(bro, 'floor', '11');
        await checkoutPage.searchAndFillInput(bro, 'intercom', 'Домофон');
        await checkoutPage.searchAndFillInput(bro, 'comment', 'Комментарий к адресу');
        await bro.assertViewAfterLockFocusAndHover('deliveryAddressSuggest', checkoutPage.root);

        await bro.click(checkoutPage.mainScreenHeaderButtonClose);
        await bro.waitForVisible(checkoutPage.mainScreenHeaderButtonClose, 5000, true);

        await checkoutPage.open(bro, defaultTestData);
        await bro.handleCheckoutEvent('restoreState', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);
        await bro.waitForVisible(checkoutPage.deliveryAddressForm, 20000);
        await bro.assertView('deliveryAddressSuggestAfterClose', checkoutPage.root);
    });
});
