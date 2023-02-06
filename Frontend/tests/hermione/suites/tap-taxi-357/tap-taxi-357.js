const indexPage = require('../../page-objects/index');

describe('tap-taxi-357: Главная. Клик по кнопке Карта.', function() {
    it('Кликнуть по адресу на главном экране, в форме заполнения кликнуть по кнопке Карта. Должна показаться карта и модалка уйти вниз', async function() {
        const bro = this.browser;
        await bro.auth('taxi-357');
        await indexPage.open(bro);

        await bro.waitForVisible(indexPage.addressPoint.fromInputText, 30000);
        await bro.click(indexPage.addressPoint.fromInputText);

        // Ожидаем завершения анимации появления точки на карте
        await bro.pause(1000);

        await bro.waitForVisible(indexPage.addressSuggest.mapButtonFrom, 3000);
        await bro.click(indexPage.addressSuggest.mapButtonFrom);

        // Ожидаем завершения анимации появления стоимости тарифа
        await bro.pause(1000);
        await bro.hideElement(indexPage.map.container);
        await bro.assertView('root', indexPage.root);
    });
});
