const indexPage = require('../../page-objects/index');

describe('tap-taxi-155: Выбор адреса. Открытие информационной шторки', function() {
    it('Должно открыться информационное меню', async function() {
        const bro = this.browser;

        await bro.auth('taxi-155');
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);

        await indexPage.clickAddressAndWaitSuggest(
            bro,
            indexPage.addressPoint.fromInput,
            indexPage.addressSuggest.searchFrom
        );
        await bro.waitForVisible(indexPage.addressSuggest.mapButtonFrom, 5000);
        await bro.click(indexPage.addressSuggest.mapButtonFrom);
        await bro.waitForVisible(indexPage.orderFormHidden, 5000, true);

        await indexPage.menuModal.open(bro);
    });
});
