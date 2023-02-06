const indexPage = require('../../page-objects/index');

describe('tap-taxi-27: Тариф. Сброс ранее выбранного тарифа при условии, что в текущем городе данный тариф отсутствует', function() {
    it('Должна быть выбран тариф "Эконом"', async function() {
        const bro = this.browser;

        await bro.auth('taxi');
        await indexPage.open(bro, { gfrom: '55.736916,37.641769', center: '55.736916,37.641769' });

        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 15000);

        const deliveryTariffButton = await indexPage.tariff.getButtonSelectors(7);
        await indexPage.tariff.clickButton(bro, 'Premier');
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 5000);
        await assertTariffActive(bro, 'Premier');

        await indexPage.clickAddressAndClickSuggestClearButton(bro,
            indexPage.addressPoint.fromInput,
            indexPage.addressSuggest.searchFrom
        );
        await indexPage.addressSuggest.fillInputAndClickFirstResult(bro, 'Екатеринбург хохрякова 10', indexPage.addressSuggest.searchFrom);

        await bro.back();
        await bro.waitForVisible(indexPage.addressSuggest.container, 5000, true);
        await bro.waitForVisible(indexPage.tariff.buttonActivePrice, 10000);

        await bro.waitForVisible(deliveryTariffButton.selector, 20000, true);
        await assertTariffActive(bro, 'Эконом');
    });

    async function assertTariffActive(bro, nameTariff) {
        const nameTariffActiveCurrent = await bro.getText(indexPage.tariff.buttonTitleActive);
        bro.assertTexts(nameTariffActiveCurrent, nameTariff, `Должен быть выбран тариф "${nameTariff}", а выбран "${nameTariffActiveCurrent}"`);
    }
});
