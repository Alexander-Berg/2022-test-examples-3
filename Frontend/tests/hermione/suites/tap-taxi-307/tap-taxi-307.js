const assert = require('assert');
const URL = require('url');

const indexPage = require('../../page-objects/index');

describe('tap-taxi-307: Главный. Авторизация пользователя по нажатию на кнопку "Заказать"', function() {
    it('Должна открыться страница паспорта при нажатии на кнопку "Заказать"', async function() {
        const bro = this.browser;

        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.addressPoint.positionFirst, 20000);

        await bro.click(indexPage.orderFormButton);
        await bro.switchTabByIndex(1);
        const currentUrl = await bro.getCurrentUrl();
        const url = URL.parse(currentUrl, true);
        const urlExpected = 'passport-test.yandex.ru';
        assert.strictEqual(url.hostname, urlExpected, `При выборе адреса "Куда" открывается урл "${url.hostname}", а должен "${urlExpected}"`);

        await bro.close();

        await bro.switchTabByIndex(0);
        await bro.waitForVisible(indexPage.addressSuggest.container, 5000, true);

        const orderFormButtonTextExpected = 'Авторизоваться для заказа';
        const orderFormButtonTextCurrent = await bro.getText(indexPage.orderFormButton);
        await bro.assertTexts(
            orderFormButtonTextCurrent,
            orderFormButtonTextExpected,
            `На главном экране в кнопке заказа должен отображаться текст "${orderFormButtonTextExpected}", а отображается "${orderFormButtonTextCurrent}"`
        );
    });
});
