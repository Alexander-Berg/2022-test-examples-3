const indexPage = require('../../page-objects/index');

describe('tap-taxi-269: Выбор маршрута. Отображение длинного названия улицы', function() {
    hermione.also.in(['chrome-grid-320', 'chrome-grid-414']);
    it('Должно отображаться корректно длинное название улицы', async function() {
        const bro = this.browser;

        await bro.auth('taxi');
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);

        await bro.hideElement(indexPage.map.container);

        await indexPage.clickAddressAndClickSuggestClearButton(
            bro,
            indexPage.addressPoint.fromInput,
            indexPage.addressSuggest.searchFrom
        );

        await indexPage.addressSuggest.fillInputAndClickFirstResult(bro,
            'Центральный проезд Хорошёвского Серебряного Бора, 73',
            indexPage.addressSuggest.searchFrom
        );

        await bro.click(indexPage.addressSuggest.fromInput);
        await bro.assertViewAfterLockFocusAndHover('inputFocused', indexPage.addressSuggest.search, { ignoreElements: indexPage.addressSuggest.result, allowViewportOverflow: true });

        await bro.click(indexPage.addressSuggest.toInputIcon);
        await bro.assertViewAfterLockFocusAndHover('inputNotFocused', indexPage.addressSuggest.search, { ignoreElements: indexPage.addressSuggest.result, allowViewportOverflow: true });

        await bro.back();
        await bro.waitForVisible(indexPage.addressSuggest.container, 5000, true);
        await bro.waitForVisible(indexPage.tariff.buttonActivePrice, 10000);

        // Ждём полного скрытия SideBlock
        await bro.pause(2000);

        await indexPage.clickAddressAndWaitSuggest(bro,
            indexPage.addressPoint.fromInput,
            indexPage.addressSuggest.searchFrom
        );
        await bro.assertViewAfterLockFocusAndHover('inputAfterReOpen', indexPage.addressSuggest.search, { ignoreElements: indexPage.addressSuggest.result, allowViewportOverflow: true });
    });
});
