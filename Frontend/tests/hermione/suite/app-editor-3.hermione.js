const indexPage = require('../page-objects');

const nameApp = 'Афиша';

describe('app-editor-3: Главная. Добавление сервиса через поиск в пустую ячейку после передачи get-параметра "selectedItemPosition"', function() {
    it('Сервис должен добавиться в пустую ячейку', async function() {
        const bro = this.browser;

        await indexPage.openAndCheckPage(bro, { selectedItemPosition: 8 });
        await indexPage.openAndCheckSearchPopup(bro);
        await indexPage.fillInputAndCheckResult(bro, nameApp);
        await bro.assertView('app-editor-3-SearchPopup', indexPage.appPage, { tolerance: 10 });

        await indexPage.searchAppAndAddInSearchPopup(bro, nameApp);
        await bro.assertView('app-editor-3-pinnedAppsPanel', indexPage.pinnedAppsPanel);
    });
});
