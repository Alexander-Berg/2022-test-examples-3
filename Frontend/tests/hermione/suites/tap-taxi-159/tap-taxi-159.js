const indexPage = require('../../page-objects/index');

describe('tap-taxi-159: Выбор адреса. Возврат к шторке выбора маршрута по нажатию на кнопку back с экран выбора адреса на карте', function() {
    it('Должен произойти возврат к модалке выбора адреса', async function() {
        const bro = this.browser;

        await bro.auth('taxi');
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);

        await indexPage.clickAddressAndWaitSuggest(
            bro,
            indexPage.addressPoint.fromInput,
            indexPage.addressSuggest.searchFrom
        );
        await bro.waitForVisible(indexPage.addressSuggest.mapButton, 5000);

        await clickAddressSuggestMapButtonAndBackAddressSuggest(bro, indexPage.addressSuggest.searchFrom);

        const addressSuggestSearchTo = await indexPage.addressSuggest.searchInput(
            bro,
            indexPage.addressSuggest.searchTo
        );
        await bro.click(addressSuggestSearchTo.input);
        await bro.waitForVisible(addressSuggestSearchTo.inputFocused, 5000);

        await clickAddressSuggestMapButtonAndBackAddressSuggest(bro, indexPage.addressSuggest.searchTo);
    });

    async function clickAddressSuggestMapButtonAndBackAddressSuggest(bro, input) {
        let addressSuggestInput = await indexPage.addressSuggest.searchInput(bro, input);

        await bro.click(indexPage.addressSuggest.mapButton);
        await bro.waitForVisible(indexPage.addressInteractiveScreen.container, 5000);
        await bro.waitForVisible(addressSuggestInput.inputFocused, 5000, true);

        await bro.back();
        await bro.waitForVisible(indexPage.addressInteractiveScreen.container, 5000, true);
        addressSuggestInput = await indexPage.addressSuggest.searchInput(bro, input);
        await bro.waitForVisible(addressSuggestInput.inputFocused, 5000);
    }
});
