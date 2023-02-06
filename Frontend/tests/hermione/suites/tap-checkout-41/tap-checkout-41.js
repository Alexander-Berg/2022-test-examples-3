const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-41.json');

describe('tap-checkout-41: Редактор адреса. Сохранение внесенных изменений с повторным открытием чекаута', function() {
    afterEach(async function() {
        const bro = this.browser;

        const addressFormVisible = await bro.isVisible(checkoutPage.deliveryAddressForm);
        if (!addressFormVisible) {
            await checkoutPage.clickAddressesEditButton(bro);
        }

        const street = 'Невский проспект, 88';
        await checkoutPage.searchAndClearFillInput(bro, 'address', street);
        await bro.waitForVisible(checkoutPage.deliveryAddressSuggestLoader, 5000);
        await bro.waitForVisible(checkoutPage.deliveryAddressSuggestLoader, 20000, true);

        await checkoutPage.clickAddressInSearchResult(bro, street);
        await checkoutPage.searchAndClearFillInput(bro, 'flat', '12');
        await checkoutPage.searchAndClearFillInput(bro, 'porch', '2');
        await checkoutPage.searchAndClearFillInput(bro, 'floor', '32');
        await checkoutPage.searchAndClearFillInput(bro, 'intercom', '321');

        await bro.click(checkoutPage.screenButton);
        await bro.waitDeliveryProfileAddressesLoad();
    });

    it('Изменения адреса должны сохраниться', async function() {
        const bro = this.browser;
        await bro.auth('tap-checkout-41');

        await checkoutPage.open(bro, defaultTestData);
        await bro.waitDeliveryProfileAddressesLoad();
        await bro.waitForVisible(checkoutPage.deliveryProfileButtonAddressesEdit);

        await checkoutPage.clickAddressesEditButton(bro);
        await bro.assertView('deliveryAddressDefault', checkoutPage.root);

        const street = 'Пискарёвский проспект, 2к2Щ';
        await checkoutPage.searchAndClearFillInput(bro, 'address', street);
        await bro.waitForVisible(checkoutPage.deliveryAddressSuggestLoader, 5000);
        await bro.waitForVisible(checkoutPage.deliveryAddressSuggestLoader, 20000, true);

        await checkoutPage.clickAddressInSearchResult(bro, street);
        await checkoutPage.searchAndClearFillInput(bro, 'flat', '12B');
        await checkoutPage.searchAndClearFillInput(bro, 'porch', '1C');
        await checkoutPage.searchAndClearFillInput(bro, 'floor', '11');
        await checkoutPage.searchAndClearFillInput(bro, 'intercom', 'Домофон');

        await bro.click(checkoutPage.screenButton);
        await bro.waitDeliveryProfileAddressesLoad();

        await bro.click(checkoutPage.mainScreenHeaderButtonClose);
        await bro.waitForVisible(checkoutPage.mainScreenHeaderButtonClose, 5000, true);

        await checkoutPage.open(bro, defaultTestData);
        await bro.handleCheckoutEvent('restoreState', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);
        await bro.waitForVisible(checkoutPage.deliveryProfileButtonAddressesEdit);
        await bro.assertView('compactDeliveryAddressAfterEdit', checkoutPage.root);

        await checkoutPage.clickAddressesEditButton(bro);
        await bro.assertView('deliveryAddressAfterEdit', checkoutPage.root);
    });
});
