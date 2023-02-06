const indexPage = require('../page-objects');

describe('app-editor-17: Главная. Удаление всех добавленных сервисов из первого экрана панели', function() {
    it('При удалении всех добавленных сервисов в панели должен отображаться только один экран', async function() {
        const bro = this.browser;

        await indexPage.openAndCheckPage(bro);

        let appPinnedOne = await indexPage.searchPinnedApp(1, 1, 4);
        await bro.click(appPinnedOne.buttonDelete);
        await bro.waitForVisible(appPinnedOne.icon, 2000, true);

        let appPinnedTwo = await indexPage.searchPinnedApp(1, 2, 1);
        await bro.click(appPinnedTwo.buttonDelete);
        await bro.waitForVisible(appPinnedTwo.icon, 2000, true);

        await bro.assertView('app-editor-17-appsSearchApp', indexPage.pinnedAppsPanel);
    });
});
