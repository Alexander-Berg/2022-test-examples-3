const indexPage = require('../../page-objects/index');

describe('tap-taxi-244: Главная. Отображение попапа о необходимости привязки номера телефона при выборе адреса "Куда"', function() {
    it('Должна появиться модалка "Нужен номер телефона" при выборе адреса "Куда" через форму главного экрана', async function() {
        const bro = this.browser;

        await bro.auth('taxi-24');
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.addressPoint.positionFirst, 20000);

        await indexPage.addressPoint.clickPositionFirst(bro);
        await bro.hideElement(indexPage.orderForm);
        await bro.hideElement(indexPage.map.container);

        await bro.pause(2000);
        await bro.assertView('errorModal', indexPage.commonModal);

        await assertOpenPassportPage(bro);
    });

    it('Должна появиться модалка "Нужен номер телефона" при выборе адреса "Куда" через саджест', async function() {
        const bro = this.browser;

        await bro.auth('taxi-24');
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.addressPoint.positionFirst, 20000);

        await indexPage.clickAddressAndWaitSuggest(bro,
            indexPage.addressPoint.toInput,
            indexPage.addressSuggest.searchTo
        );
        await indexPage.addressSuggest.fillInputAndClickFirstResult(bro, 'Москва Манежная площадь, 1с2', indexPage.addressSuggest.searchFrom);
        await bro.hideElement(indexPage.orderForm);
        await bro.hideElement(indexPage.map.container);
        await bro.waitForVisible(indexPage.commonModal, 5000);
        await bro.assertView('errorModal', indexPage.commonModal);

        await assertOpenPassportPage(bro);
    });

    async function assertOpenPassportPage(bro) {
        await bro.click(indexPage.commonModalButton);

        await bro.switchTabByIndex(1);
        // const currentUrl = await bro.getCurrentUrl();
        // const url = URL.parse(currentUrl, true);
        // assert.match(url.href, /https:\/\/passport-test\.yandex\.ru\/auth\/phoneconfirm\?retpath=.+\/return-to-app/);

        await bro.close();

        await bro.switchTabByIndex(0);
        await bro.waitForVisible(indexPage.addressSuggest.container, 5000, true);
        await bro.waitForVisible(indexPage.commonModal, 5000);
    }
});
