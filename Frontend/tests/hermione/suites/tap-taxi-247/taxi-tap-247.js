const indexPage = require('../../page-objects/index');

describe('tap-taxi-247: Выбор адреса. Отображение модалки "Адрес не поддерживается сервисом" с последующим закрытием модалки выбора маршрута без выбора адреса', function() {
    it('Должна появиться модалка выбора маршрута по нажатию на кнопку "Изменить" в модалке "Адрес не поддерживается сервисом"', async function() {
        const bro = this.browser;

        await bro.auth('taxi');
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);

        await indexPage.clickAddressAndClickSuggestClearButton(
            bro,
            indexPage.addressPoint.fromInput,
            indexPage.addressSuggest.searchFrom
        );

        const addressName = 'Салехард ленина 44';
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

        // Ждем завершения запросов
        await bro.pause(2000);
        // Закрываем модалку выбора маршрута
        await bro.click(indexPage.addressSuggest.sideBlock);
        await bro.waitForVisible(indexPage.addressSuggest.fromInputFocused, 5000, true);
        await bro.waitForVisible(indexPage.commonModal, 5000, true);
        await bro.assertView('orderForm', indexPage.orderForm);
    });
});
