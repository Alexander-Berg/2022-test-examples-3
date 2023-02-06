const indexPage = require('../page-objects');

describe('app-editor-4: Главная. Добавление сервиса из списка "Рекомендованные приложения" в пустую ячейку после передачи get-параметра "selectedItemPosition"', function() {
    it('Сервис должен добавиться в пустую ячейку из блока "Рекомендованные приложения"', async function() {
        const bro = this.browser;

        await indexPage.openAndCheckPage(bro, { selectedItemPosition: 15 });

        const paginatorActive = await indexPage.pinnedPaginatorActive(2);
        await bro.waitForVisible(paginatorActive, 5000);

        const panelPinnedActiveCell = await indexPage.searchPinnedApp(2, 2, 1);
        await bro.waitForVisible(panelPinnedActiveCell.cellActive, 2000);

        const appOneSelector = await indexPage.appsSearchAppById(bro, 1, 1);
        await bro.click(appOneSelector.app);
        await bro.waitForVisible(panelPinnedActiveCell.icon, 2000);
        await bro.waitForVisible(indexPage.appsSearchAppsHidden, 5000, true);

        await bro.changeTextElement(panelPinnedActiveCell.name, 'Тестирование');
        await bro.assertView('app-editor-4-pinnedAppsPanel', indexPage.pinnedAppsPanel, { ignoreElements: panelPinnedActiveCell.icon });
    });
});
