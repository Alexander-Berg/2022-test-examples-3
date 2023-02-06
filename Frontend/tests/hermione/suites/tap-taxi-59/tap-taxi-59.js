const indexPage = require('../../page-objects/index');

describe('tap-taxi-59: Главная. Отображение в кнопке сообщение "Введите адрес назначения для этой поездки" вместо "Заказать"', function() {
    it('В кнопке заказ такси должен отображаться текст "Введите адрес назначения для этой поездки"', async function() {
        const bro = this.browser;

        await bro.auth('taxi');
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);

        await indexPage.clickAddressAndClickSuggestClearButton(bro,
            indexPage.addressPoint.fromInput,
            indexPage.addressSuggest.searchFrom
        );
        await indexPage.addressSuggest.fillInputAndClickFirstResult(bro,
            'Тюмень светофор',
            indexPage.addressSuggest.searchFrom
        );

        await bro.back();
        await bro.waitForVisible(indexPage.addressSuggest.container, 5000, true);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 10000);
        await bro.assertView('orderFormButtonAfterSelectAddressFrom', indexPage.orderFormButton);

        await indexPage.clickAddressAndWaitSuggest(bro,
            indexPage.addressPoint.toInput,
            indexPage.addressSuggest.searchTo
        );
        await indexPage.addressSuggest.fillInputAndClickFirstResult(bro,
            'Тюмень Ленина',
            indexPage.addressSuggest.searchTo
        );
        // Ждем когда загрузится estimate
        await bro.pause(2000);
        await bro.assertView('orderFormButtonAfterSelectAddressTo', indexPage.orderFormButton);
    });
});
