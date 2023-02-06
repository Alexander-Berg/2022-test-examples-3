const indexPage = require('../page-objects');

const nameApp = 'А';

describe('app-editor-42: Поиск. Закрытие и открытие попапа поиска с результатами поиска с последующим добавлением сервиса', function() {
    it('Сервис должен добавиться в пустую ячейку', async function() {
        const bro = this.browser;

        await indexPage.openAndCheckPage(bro);
        await indexPage.openAndCheckSearchPopup(bro);
        await indexPage.fillInputAndCheckResult(bro, nameApp);
        await indexPage.closeSearchPopup(bro);
        await indexPage.assertCurrentTextInSearchInput(bro, nameApp);

        await indexPage.openAndCheckSearchPopupResult(bro);
        await indexPage.searchAppAndAddInSearchPopup(bro, 'Афиша');
        await bro.assertView('app-editor-42-pinnedAppsPanel', indexPage.pinnedAppsPanel);

        await indexPage.closeAndCheckClosingPage(bro);
    });
});
