const indexPage = require('../../page-objects/index');

describe('tap-taxi-60: Главная. Отображение попапа о необходимости привязки номера телефона по нажатию на кнопку "Заказать"', function() {
    it('Должна появиться модалка "Нужен номер телефона" по нажатию на кнопку "Заказать"', async function() {
        const bro = this.browser;

        await bro.auth('taxi-24');
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.addressPoint.positionFirst, 20000);
        await bro.waitForVisible(indexPage.tariff.buttonActivePrice, 5000);
        await bro.hideElement(indexPage.map.container);
        await bro.assertView('indexForm', indexPage.orderForm);

        await bro.click(indexPage.orderFormButton);
        await bro.waitForVisible(indexPage.commonModal, 5000);

        await bro.hideElement(indexPage.orderForm, indexPage.map.container);
        await bro.assertView('errorModal', indexPage.commonModal);

        await bro.click(indexPage.commonModalButton);
        await bro.switchTabByIndex(1);
        // const currentUrl = await bro.getCurrentUrl();
        // const url = URL.parse(currentUrl, true);
        // assert.match(url.href, /https:\/\/passport-test\.yandex\.ru\/auth\/phoneconfirm\?retpath=.+\/return-to-app/);

        await bro.close();

        await bro.switchTabByIndex(0);
        await bro.waitForVisible(indexPage.commonModal, 5000);
    });
});
