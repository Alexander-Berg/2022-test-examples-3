const indexPage = require('../page-objects');

describe('app-editor-34: Главная. Отсутствие отображение блоков "Добавить недавние" и "Рекомендованные приложения"', function() {
    it('На экране должны отсутствовать блоки "Добавить недавние" и "Рекомендованные приложения"', async function() {
        const bro = this.browser;

        await indexPage.openAndCheckPage(bro, { selectedItemPosition: 0 });
        await bro.waitForVisible(indexPage.appsSearchAppsFirstTitle, 5000);

        for (let i = 1; i < 20; i++) {
            const appSelector = await indexPage.appsSearchAppById(bro, 0, 1);
            const appSelectorVisible = await bro.isVisible(appSelector.app);

            if (appSelectorVisible) {
                const appOneSelector = await indexPage.appsSearchAppById(bro, 0, 1);
                await bro.click(appOneSelector.app);

                const pinnedAppCell = await indexPage.searchPinnedApp(1, 2, 5);
                const pinnedAppCellEmptyVisible = await bro.isVisible(pinnedAppCell.cellEmpty);

                const pinnedAppCellNext = await indexPage.searchPinnedApp(2, 1, 1);
                const pinnedAppCellEmptyNextVisible = await bro.isVisible(pinnedAppCellNext.cellEmpty);

                if (!pinnedAppCellEmptyVisible && pinnedAppCellEmptyNextVisible) {
                    const page = await indexPage.carouselPage(bro, 1);
                    await bro.swipeLeft(page.selector);

                    const panelPinnedCell = await indexPage.searchPinnedApp(2, 1, 1);
                    await bro.click(panelPinnedCell.cell);
                    await bro.waitForVisible(panelPinnedCell.cellActive, 2000);
                }
            } else {
                break;
            }
        }

        await bro.assertView('app-editor-34-appPage', indexPage.appsSearchContainer);
    });
});
