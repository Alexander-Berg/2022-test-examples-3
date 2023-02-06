const indexPage = require('../page-objects');

describe('app-editor-26: Главная. Удаление всех добавленных сервисов с нескольких экранов панели', function() {
    it('При удалении всех добавленных сервисов в панели должен отображаться только один экра', async function() {
        const bro = this.browser;

        await indexPage.openAndCheckPage(bro, { selectedItemPosition: 18 });

        this.panelPinnedActiveCell = await indexPage.searchPinnedApp(2, 2, 4);
        await bro.waitForVisible(this.panelPinnedActiveCell.cellActive, 2000);

        const appRecent = await indexPage.appsSearchAppById(bro, 0, 1);
        await bro.click(appRecent.app);
        await bro.waitForVisible(this.panelPinnedActiveCell.icon, 2000);

        const pageTwo = await indexPage.carouselPage(bro, 1);
        await bro.swipeRight(pageTwo.selector);

        const paginatorActive = await indexPage.pinnedPaginatorActive(1);
        await bro.waitForVisible(paginatorActive, 5000);

        const appPinnedOne = await indexPage.searchPinnedApp(1, 1, 4);
        await bro.click(appPinnedOne.buttonDelete);
        await bro.waitForVisible(appPinnedOne.icon, 2000, true);

        const appPinnedTwo = await indexPage.searchPinnedApp(1, 2, 1);
        await bro.click(appPinnedTwo.buttonDelete);
        await bro.waitForVisible(appPinnedTwo.icon, 2000, true);

        const pageOne = await indexPage.carouselPage(bro, 1);
        await bro.swipeLeft(pageOne.selector, 10);

        const paginatorTwoActive = await indexPage.pinnedPaginatorActive(2);
        await bro.waitForVisible(paginatorTwoActive, 5000);

        const appPinnedThree = await indexPage.searchPinnedApp(2, 2, 4);
        await bro.click(appPinnedThree.buttonDelete);
        await bro.waitForVisible(appPinnedThree.icon, 2000, true);

        const paginatorOneActive = await indexPage.pinnedPaginatorActive(1);
        await bro.waitForVisible(paginatorOneActive, 5000);

        this.panelPinnedActiveCell = await indexPage.searchPinnedApp(1, 1, 1);
        await bro.waitForVisible(this.panelPinnedActiveCell.cellActive, 2000);

        await bro.assertView('app-editor-26-appsSearchApp', indexPage.pinnedAppsPanel);
    });
});
