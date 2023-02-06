const indexPage = require('../page-objects');

const nameApp = 'Афиша';

describe('app-editor-20: Главное. Автоматическое отображение экрана панели, на который пользователь добавил сервис через поиск', function() {
    it('В панели должен отображаться экран с активной ячейкой', async function() {
        const bro = this.browser;

        await indexPage.openAndCheckPage(bro, { selectedItemPosition: 18 });
        await bro.waitForVisible(indexPage.appsSearchRecentAppFirst, 5000);

        const pageTwo = await indexPage.carouselPage(bro, 1);
        await bro.swipeRight(pageTwo.selector);

        const pageOne = await indexPage.carouselPage(bro, 0);
        await bro.waitForVisible(pageOne.selector, 5000);

        await indexPage.openAndCheckSearchPopup(bro);
        await indexPage.fillInputAndCheckResult(bro, nameApp);

        await indexPage.searchAppAndAddInSearchPopup(bro, nameApp);
        await bro.waitForVisible(pageTwo.selector, 5000);
        await bro.assertView('app-editor-20-pinnedAppsPanel', indexPage.pinnedAppsPanel);
    });
});
