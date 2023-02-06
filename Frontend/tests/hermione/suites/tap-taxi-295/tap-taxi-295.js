const indexPage = require('../../page-objects/index');

describe('tap-taxi-295: Адрес. Отсутствие отображение кнопки "Подъезд" при условии, что в шторке выбора маршрута был указан адрес с подъездом', function() {
    it('На главном экране не должна отображаться кнопка "Подъезд"', async function() {
        const bro = this.browser;

        await bro.auth('taxi');
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);

        const addressName = 'Малая Пироговская улица, 16 подъезд 2';
        await indexPage.clickAddressAndClickSuggestClearButton(bro,
            indexPage.addressPoint.fromInput,
            indexPage.addressSuggest.searchFrom
        );
        await indexPage.addressSuggest.fillInputAndClickFirstResult(bro,
            addressName,
            indexPage.addressSuggest.searchFrom
        );

        await bro.back();
        await bro.waitForVisible(indexPage.addressSuggest.container, 5000, true);
        await bro.waitForVisible(indexPage.addressPoint.porchButton, 5000, true);
        await bro.hideElement(indexPage.map.container);
        await bro.assertView('addressPointFromInput', indexPage.addressPoint.fromInput,);
    });
});
