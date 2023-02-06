const assert = require('assert');
const URL = require('url');

const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-53.json');

describe('tap-checkout-53: Общий. Переход по ссылке "Условия"', function() {
    it('Должен произойти переход по ссылке "Условия"', async function() {
        const bro = this.browser;

        await checkoutPage.open(bro, defaultTestData);
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);
        await bro.waitForVisible(checkoutPage.mainScreenServiceAgreementLink, 10000);

        await bro.click(checkoutPage.mainScreenServiceAgreementLink);

        await bro.switchTabByIndex(1);
        const currentUrl = await bro.getCurrentUrl();
        const url = URL.parse(currentUrl, true);
        const urlExpected = 'https://yandex.ru/legal/payer_termsofuse/';
        assert.strictEqual(url.href, urlExpected, `По клику на ссылку "Условия" открывается урл "${url.href}", а должен "${urlExpected}"`);
    });
});
