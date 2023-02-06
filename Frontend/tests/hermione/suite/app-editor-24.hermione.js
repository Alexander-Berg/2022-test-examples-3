const indexPage = require('../page-objects');
const testData = {
    sectionPosition: [
        { paginatorPosition: 2, selectionPagePosition: 1, carouselPosition: 2 },
        { paginatorPosition: 3, selectionPagePosition: 2, carouselPosition: 3 },
        { paginatorPosition: 4, selectionPagePosition: 3, carouselPosition: 4 },
        { paginatorPosition: 5, selectionPagePosition: 4, carouselPosition: 5 },
        { paginatorPosition: 6, selectionPagePosition: 5, carouselPosition: 6 },
        { paginatorPosition: 7, selectionPagePosition: 6, carouselPosition: 7 },
        { paginatorPosition: 8, selectionPagePosition: 7, carouselPosition: 8 },
        { paginatorPosition: 9, selectionPagePosition: 8, carouselPosition: 9 },
        { paginatorPosition: 10, selectionPagePosition: 9, carouselPosition: 10 }
    ]
};

describe('app-editor-24: Главная. Максимальное отображение экранов в панели редактирования', function() {
    it('В панели редактирование максимальное отображение экранов должно ровняться 10', async function() {
        const bro = this.browser;

        await indexPage.openAndCheckPage(bro);
        const pageOne = await indexPage.carouselPage(bro, 0);
        await bro.swipeLeft(pageOne.selector);

        for (let item of testData.sectionPosition) {
            const paginatorActive = await indexPage.pinnedPaginatorActive(item.paginatorPosition);
            await bro.waitForVisible(paginatorActive, 5000);

            const panelPinnedCell = await indexPage.searchPinnedApp(item.carouselPosition, 2, 5);
            await bro.click(panelPinnedCell.cell);
            await bro.waitForVisible(panelPinnedCell.cellActive, 2000);

            const appOneSelector = await indexPage.appsSearchAppById(bro, 0, 1);
            await bro.click(appOneSelector.app);
            await bro.waitForVisible(panelPinnedCell.icon, 2000);

            const pageNext = await indexPage.carouselPage(bro, item.selectionPagePosition);
            await bro.swipeLeft(pageNext.selector);

            const paginatorActiveNext = await indexPage.pinnedPaginatorActive(item.paginatorPosition + 1);
            if (item.paginatorPosition + 1 <= 10) {
                await bro.waitForVisible(paginatorActiveNext, 5000);
            } else {
                await bro.waitForVisible(paginatorActiveNext, 5000, true);
            }
        }

        const panelPinnedActiveCell = await indexPage.searchPinnedApp(10, 2, 5);
        await bro.changeTextElement(panelPinnedActiveCell.name, 'Тестирование');
        await bro.assertView('app-editor-24-pinnedAppsPanel', indexPage.pinnedAppsPanel, { ignoreElements: panelPinnedActiveCell.icon });
    });
});
