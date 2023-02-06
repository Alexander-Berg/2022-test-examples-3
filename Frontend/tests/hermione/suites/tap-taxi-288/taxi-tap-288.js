const indexPage = require('../../page-objects/index');

describe('tap-taxi-288: Выбор маршрута. Отображение модалки "Адрес не поддерживается сервисом" с последующим изменением адреса', function() {
    it('Должна появиться модалка выбора маршрута по нажатию на кнопку "Изменить" в модалке "Адрес не поддерживается сервисом"', async function() {
        const bro = this.browser;

        await bro.auth('taxi');
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);

        await indexPage.clickAddressAndClickSuggestClearButton(bro,
            indexPage.addressPoint.fromInput,
            indexPage.addressSuggest.searchFrom
        );

        const addressName = 'Ханты-Мансийск безноскова 52';
        await indexPage.addressSuggest.fillInputAndClickFirstResult(bro,
            addressName,
            indexPage.addressSuggest.searchFrom
        );
        await bro.waitForVisible(indexPage.commonModal, 5000);

        await bro.hideElement(indexPage.map.container);
        await bro.assertView('errorModal', indexPage.commonModal);

        await bro.click(indexPage.commonModalButton);
        await bro.waitForVisible(indexPage.commonModal, 5000, true);
        await bro.waitForVisible(indexPage.addressSuggest.fromInputFocused, 5000);

        await indexPage.addressSuggest.clickClearButton(bro);
        await indexPage.addressSuggest.fillInputAndClickFirstResult(bro, 'Москва Усачева 62', indexPage.addressSuggest.searchFrom);

        await bro.back();
        await bro.waitForVisible(indexPage.addressSuggest.container, 5000, true);
        await bro.waitForVisible(indexPage.commonModal, 5000, true);
        await bro.waitForVisible(indexPage.tariff.buttonActivePrice, 10000);
    });
});
