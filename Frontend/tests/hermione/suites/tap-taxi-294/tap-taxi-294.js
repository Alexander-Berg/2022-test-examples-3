const indexPage = require('../../page-objects/index');

describe('tap-taxi-294: Адрес. Отображение введенного подъезда в поле на главном экране/в шторке выбора маршрута', function() {
    it('Должен отображаться номер веденного подъезда', async function() {
        const bro = this.browser;
        await bro.auth('taxi');

        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);

        await indexPage.porchForm.open(bro);
        await bro.keys('1');
        await bro.assertViewAfterLockFocusAndHover('porchForm', indexPage.root, { ignoreElements: [
            indexPage.map.container,
            indexPage.tariff.tariffsSelector,
            indexPage.addressPoint.fromInput,
            indexPage.addressPoint.toInput
        ] });

        await bro.click(indexPage.porchForm.saveButton);
        await bro.waitForVisible(indexPage.porchForm.container, 5000, true);

        // Ждем запроса за новым адресом
        await bro.pause(1000);

        const addressExpected = 'Кремлёвская набережная, 1с14, подъезд 1';
        const addressCurrent = await bro.getText(indexPage.addressPoint.fromInputText);
        bro.assertTexts(addressCurrent, addressExpected, `На главном экране в поле "Откуда" должен отображаться адрес"${addressExpected}", а отображается "${addressCurrent}"`);

        await indexPage.clickAddressAndWaitSuggest(bro,
            indexPage.addressPoint.fromInput,
            indexPage.addressSuggest.searchFrom
        );

        const addressSuggestCurrent = await bro.getText(indexPage.addressSuggest.fromInput);
        bro.assertTexts(addressSuggestCurrent, addressExpected, `В модалке выбора маршрута в поле "Откуда" должен отображаться адрес"${addressExpected}", а отображается "${addressSuggestCurrent}"`);
    });
});
