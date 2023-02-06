const indexPage = require('../../page-objects/index');

describe('tap-taxi-296: Адрес. Повторное отображение кнопки "Подъезд" после удаления адреса', function() {
    it('На главном экране должна отображаться кнопка "Подъезд" после удаления номера подъезда через модалку выбора адреса', async function() {
        const bro = this.browser;
        await bro.auth('taxi');

        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);

        await indexPage.porchForm.open(bro);
        await bro.keys('1');
        await bro.click(indexPage.porchForm.saveButton);
        await bro.waitForVisible(indexPage.porchForm.container, 5000, true);
        await bro.waitForVisible(indexPage.addressPoint.porchButton, 5000, true);

        const addressName = 'Кремлёвская набережная, 1с14';
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
        await bro.waitForVisible(indexPage.addressPoint.porchButton, 5000);
        await bro.hideElement(indexPage.map.container);
        await bro.assertView('addressPointFromInput', indexPage.addressPoint.fromInput,);

        await indexPage.porchForm.open(bro);

        const porchFormText = await bro.getText(indexPage.porchForm.input);
        bro.assertTexts(porchFormText, '', `В модалке "Укажите номер подъезда" поле ввода должно отображается пустым, а в нем отображается текст "${porchFormText}"`);
    });
});
