const indexPage = require('../page-objects');

describe('app-editor-35: Главная. Отображение на экране только блока "Добавить недавние" или "Рекомендованные приложения"', function() {
    it('На экране должен отсутствовать блок "Добавить недавние"', async function() {
        const bro = this.browser;

        await indexPage.openAndCheckPage(bro, { selectedItemPosition: 0 });
        await bro.waitForVisible(indexPage.appsSearchAppsFirstTitle, 5000);

        const appSelector = await indexPage.searchAppsList(bro, 0);

        for (let i = 1; i < 20; i++) {
            let appSelectorVisible = await bro.isVisible(appSelector.list);

            if (appSelectorVisible) {
                let appOneSelector = await indexPage.appsSearchAppById(bro, 0, 1);
                await bro.click(appOneSelector.app);

                let pinnedAppCell = await indexPage.searchPinnedApp(1, 2, 5);
                let pinnedAppCellEmptyVisible = await bro.isVisible(pinnedAppCell.cellEmpty);

                let pinnedAppCellNext = await indexPage.searchPinnedApp(2, 1, 1);
                let pinnedAppCellEmptyNextVisible = await bro.isVisible(pinnedAppCellNext.cellEmpty);

                if (!pinnedAppCellEmptyVisible && pinnedAppCellEmptyNextVisible) {
                    this.page = await indexPage.carouselPage(bro, 1);
                    await bro.swipeLeft(this.page.selector);

                    let panelPinnedCell = await indexPage.searchPinnedApp(2, 1, 1);
                    await bro.click(panelPinnedCell.cell);
                    await bro.waitForVisible(panelPinnedCell.cellActive, 2000);
                }
            } else {
                break;
            }
        }

        await bro.assertView('app-editor-35-appPageVisibleRecommendedBlock', indexPage.appsSearchContainer);
    });

    it('На экране должен отсутствовать блок "Рекомендованные приложения"', async function() {
        const bro = this.browser;

        await indexPage.openAndCheckPage(bro, { selectedItemPosition: 0 });
        await bro.waitForVisible(indexPage.appsSearchAppsFirstTitle, 5000);

        const appSelector = await indexPage.searchAppsList(bro, 1);

        for (let i = 1; i < 20; i++) {
            const appSelectorVisible = await bro.isVisible(appSelector.list);

            if (appSelectorVisible) {
                const appOneSelector = await indexPage.appsSearchAppById(bro, 1, 1);
                await bro.click(appOneSelector.app);

                const pinnedAppCell = await indexPage.searchPinnedApp(1, 2, 5);
                const pinnedAppCellEmptyVisible = await bro.isVisible(pinnedAppCell.cellEmpty);

                const pinnedAppCellNext = await indexPage.searchPinnedApp(2, 1, 1);
                const pinnedAppCellEmptyNextVisible = await bro.isVisible(pinnedAppCellNext.cellEmpty);

                if (!pinnedAppCellEmptyVisible && pinnedAppCellEmptyNextVisible) {
                    this.page = await indexPage.carouselPage(bro, 1);
                    await bro.swipeLeft(this.page.selector);

                    const panelPinnedCell = await indexPage.searchPinnedApp(2, 1, 1);
                    await bro.click(panelPinnedCell.cell);
                    await bro.waitForVisible(panelPinnedCell.cellActive, 2000);
                }
            } else {
                break;
            }
        }

        await bro.assertView('app-editor-35-appPageVisibleRecentBlock', indexPage.appsSearchContainer);
    });
});
