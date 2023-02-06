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
    ],
    sectionPositionForDelete: [
        { paginatorPosition: 9, carouselPosition: 9 },
        { paginatorPosition: 8, carouselPosition: 8 },
        { paginatorPosition: 7, carouselPosition: 7 },
        { paginatorPosition: 6, carouselPosition: 6 },
        { paginatorPosition: 5, carouselPosition: 5 },
        { paginatorPosition: 4, carouselPosition: 4 },
        { paginatorPosition: 3, carouselPosition: 3 },
        { paginatorPosition: 2, carouselPosition: 2 }
    ]
};

describe('app-editor-31: Поиск. Добавление сервиса, который ранее уже был добавлен в панель', function() {
    it('Должна быть возможность через поиска добавить сервис, который уже добавлен в панель сервисов', async function() {
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
        const nameService = await bro.getText(panelPinnedActiveCell.name);

        for (let item of testData.sectionPositionForDelete) {
            const pagePrevious = await indexPage.carouselPage(bro, item.carouselPosition);
            await bro.swipeRight(pagePrevious.selector);

            const paginatorActive = await indexPage.pinnedPaginatorActive(item.paginatorPosition);
            await bro.waitForVisible(paginatorActive, 5000);

            const appPinned = await indexPage.searchPinnedApp(item.carouselPosition, 2, 5);
            await bro.waitForVisible(appPinned.icon, 500);

            await bro.click(appPinned.buttonDelete);
            await bro.waitForVisible(appPinned.cellEmpty, 5000);
        }

        const pageTwo = await indexPage.carouselPage(bro, 1);
        await bro.swipeRight(pageTwo.selector);

        const pinnedAppCell = await indexPage.searchPinnedApp(1, 1, 1);
        await bro.click(pinnedAppCell.cellEmpty);
        await bro.waitForVisible(pinnedAppCell.cellActive);

        await indexPage.openAndCheckSearchPopup(bro);
        await indexPage.fillInputAndCheckResult(bro, nameService);
        await indexPage.searchAppAndAddInSearchPopup(bro, nameService);
        await bro.assertView('app-editor-31-pinnedAppsPanel', indexPage.pinnedAppsPanel);
    });
});
