const indexPage = require('../../page-objects/index');

describe('tap-taxi-41: Выбор маршрута. Отображение сообщения "Ничего не найдено"', function() {
    it('Должно отсутствовать отображения сообщения "Ничего не найдено" после удаления веденного значения', async function() {
        const bro = this.browser;

        await bro.auth('taxi');
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);

        const address = await bro.getText(indexPage.addressPoint.fromInputText);
        await bro.click(indexPage.addressPoint.fromInput,);
        await bro.waitForVisible(indexPage.addressSuggest.fromInputFocused, 5000);

        await bro.click(indexPage.addressSuggest.inputClearButton);
        await bro.waitForVisible(indexPage.addressSuggest.inputClearButton, 5000, true);

        await bro.keys('проверка сценария: Выбор адреса. Отображение сообщения "Ничего не найдено"');
        await bro.waitForVisible(indexPage.addressSuggest.resultEmpty, 10000);

        await bro.hideElement(indexPage.map.container);
        await bro.assertView('addressSuggestResultEmpty', indexPage.addressSuggest.result, { allowViewportOverflow: true });

        await bro.click(indexPage.addressSuggest.inputClearButton);
        await bro.waitForVisible(indexPage.addressSuggest.inputClearButton, 5000, true);

        await bro.back();
        await bro.waitForVisible(indexPage.addressSuggest.container, 5000, true);
        await bro.waitForVisible(indexPage.tariff.buttonActivePrice, 10000);

        const addressCurrent = await bro.getText(indexPage.addressPoint.fromInputText);
        bro.assertTexts(addressCurrent, address, `Должен отображаться адрес "${address}", а отображается "${addressCurrent}"`);
    });

    it('Должно отсутствовать отображения сообщения "Ничего не найдено" после закрытия модалки без очищения поля ввода', async function() {
        const bro = this.browser;

        await bro.auth('taxi');
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);

        const address = await bro.getText(indexPage.addressPoint.fromInputText);

        await indexPage.clickAddressAndClickSuggestClearButton(bro,
            indexPage.addressPoint.fromInput,
            indexPage.addressSuggest.searchFrom
        );
        await bro.keys('проверка сценария: Выбор адреса. Отображение сообщения "Ничего не найдено"');
        await bro.waitForVisible(indexPage.addressSuggest.resultEmpty, 10000);

        await bro.back();
        await bro.waitForVisible(indexPage.addressSuggest.container, 5000, true);
        await bro.waitForVisible(indexPage.tariff.buttonActivePrice, 10000);

        // Ждём полного скрытия SideBlock
        await bro.pause(2000);

        const addressCurrent = await bro.getText(indexPage.addressPoint.fromInputText);
        bro.assertTexts(addressCurrent, address, `Должен отображаться адрес"${address}", а отображается "${addressCurrent}"`);

        await indexPage.clickAddressAndWaitSuggest(bro,
            indexPage.addressPoint.fromInput,
            indexPage.addressSuggest.searchFrom
        );

        const addressSuggest = await bro.getText(indexPage.addressSuggest.fromInputFocused);
        bro.assertTexts(addressSuggest, address, `В модалке адреса должен отображаться адрес"${address}", а отображается "${addressSuggest}"`);
    });
});
