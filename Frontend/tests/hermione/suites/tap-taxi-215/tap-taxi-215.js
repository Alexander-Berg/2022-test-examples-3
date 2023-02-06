const assert = require('assert');
const URL = require('url');

const indexPage = require('../../page-objects/index');

describe('tap-taxi-215: Главный. Авторизация пользователя при выборе адреса "Куда" в форме главного экрана и в саджесте', function() {
    it('Должна открыться страница паспорта при выборе адреса "Куда" через форму главного экрана', async function() {
        const bro = this.browser;

        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.addressPoint.positionFirst, 20000);
        await bro.hideElement(indexPage.map.container);
        await bro.assertView('indexForm', indexPage.orderForm);

        await indexPage.addressPoint.clickPositionFirst(bro);
        await assertOpenPassportPage(bro);
    });

    it('Должна открыться страница паспорта при выборе адреса "Куда" через саджест', async function() {
        const bro = this.browser;

        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.addressPoint.positionFirst, 20000);

        await indexPage.clickAddressAndWaitSuggest(bro,
            indexPage.addressPoint.toInput,
            indexPage.addressSuggest.searchTo
        );
        await indexPage.addressSuggest.fillInputAndClickFirstResult(bro, 'Москва Манежная площадь, 1с2', indexPage.addressSuggest.searchFrom);
        await assertOpenPassportPage(bro);
    });

    async function assertOpenPassportPage(bro) {
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
    }
});
