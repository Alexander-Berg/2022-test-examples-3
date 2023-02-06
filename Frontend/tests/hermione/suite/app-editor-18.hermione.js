const indexPage = require('../page-objects');

const nameApp = 'Авиа';
const nameAppNew = 'Афиша';

describe('app-editor-18: Поиск. Поиск сервиса с последующим добавлением после очищения поля ввода по нажатию на кнопку "X"', function() {
    it('Сервис должен добавиться в пустую ячейку', async function() {
        const bro = this.browser;

        await indexPage.openAndCheckPage(bro, { selectedItemPosition: 12 });
        await indexPage.openAndCheckSearchPopup(bro);
        await indexPage.fillInputAndCheckResult(bro, nameApp);
        await indexPage.clearInputAndCheckResult(bro);

        await indexPage.fillInputAndCheckResult(bro, nameAppNew);
        await indexPage.searchAppAndAddInSearchPopup(bro, nameAppNew);
        await bro.assertView('app-editor-18-pinnedAppsPanel', indexPage.pinnedAppsPanel);
    });
});
