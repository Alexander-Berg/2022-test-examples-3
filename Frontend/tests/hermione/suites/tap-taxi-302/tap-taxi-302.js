const indexPage = require('../../page-objects/index');

describe('tap-taxi-302: Выбор адреса. Отображение модалки "Адрес не поддерживается сервисом" при старте приложения с последующим изменением адреса', function() {
    it('Должна появиться "Адрес не поддерживается сервисом" при открытии главной страницы', async function() {
        const bro = this.browser;

        await bro.auth('taxi');
        // Координаты города "Ханты-Мансийск"
        await bro.setGeoLocation({
            latitude: 61.0042,
            longitude: 69.0019,
            altitude: 0,
        });

        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.commonModal, 5000);

        await bro.hideElement(indexPage.map.container);
        await bro.assertView('errorModal', indexPage.commonModal);

        await bro.click(indexPage.commonModalButton);
        await bro.waitForVisible(indexPage.commonModal, 5000, true);

        await indexPage.clickAddressAndWaitSuggest(bro,
            indexPage.addressPoint.fromInput,
            indexPage.addressSuggest.searchFrom
        );
        await indexPage.addressSuggest.fillInputAndClickFirstResult(bro, 'Москва Усачева 62', indexPage.addressSuggest.searchFrom);

        await bro.back();
        await bro.waitForVisible(indexPage.addressSuggest.container, 5000, true);
        await bro.waitForVisible(indexPage.commonModal, 5000, true);
        await bro.waitForVisible(indexPage.tariff.buttonActivePrice, 10000);
    });
});
