const indexPage = require('../page-objects');

describe('app-editor-22: Главная. Отображение сервисов с длинным названием', function() {
    it('Длинное название сервиса в панели запиненных приложений должно уходить в троеточие', async function() {
        const bro = this.browser;

        await indexPage.openAndCheckPage(bro);
        await bro.waitForVisible(indexPage.appsSearchRecentAppFirst, 5000);

        const appOneSelector = await indexPage.appsSearchAppById(bro, 0, 1);
        await bro.click(appOneSelector.app);

        const panelPinnedActiveCellTwo = await indexPage.searchPinnedApp(1, 1, 2);
        await bro.waitForVisible(panelPinnedActiveCellTwo.cellActive, 5000);

        await bro.click(appOneSelector.app);
        await bro.waitForVisible(panelPinnedActiveCellTwo.cellEmpty, 5000, true);
        await bro.waitForVisible(panelPinnedActiveCellTwo.icon, 5000);

        const panelPinnedActiveCellOne = await indexPage.searchPinnedApp(1, 1, 1);
        await bro.changeTextElement(panelPinnedActiveCellOne.name, 'Тестирование');
        await bro.changeTextElement(panelPinnedActiveCellTwo.name, 'Тестирование');
        await bro.assertView('app-editor-22-pinnedAppsPanel', indexPage.pinnedAppsPanel);
    });

    it('Длинное название сервиса в блоке "Добавить недавние" должно уходить в троеточие', async function() {
        const bro = this.browser;

        await indexPage.openAndCheckPage(bro);
        await bro.waitForVisible(indexPage.appsSearchRecentAppFirst, 5000);

        const appRecentSelector = await indexPage.searchAppsList(bro, 0);
        await bro.waitForVisible(appRecentSelector.list, 2000);

        const appOneSelector = await indexPage.appsSearchAppById(bro, 0, 1);
        await bro.waitForVisible(appOneSelector.name, 2000);
        await bro.changeTextElement(appOneSelector.name, 'Тестирование');

        const appTwoSelector = await indexPage.appsSearchAppById(bro, 0, 2);
        await bro.changeTextElement(appTwoSelector.name, 'Тестирование');

        const appsSearchAppsOne = await indexPage.searchAppsList(bro, 0);
        await bro.assertView('app-editor-22-appsSearchApp', appsSearchAppsOne.list);
    });

    it('Длинное название сервиса в блоке "Рекомендованные приложения" должно уходить в троеточие', async function() {
        const bro = this.browser;

        await indexPage.openAndCheckPage(bro);
        await bro.waitForVisible(indexPage.appsSearchRecentAppFirst, 5000);

        const appRecommendedSelector = await indexPage.searchAppsList(bro, 1);
        await bro.waitForVisible(appRecommendedSelector.list, 2000);

        const appOneSelector = await indexPage.appsSearchAppById(bro, 1, 1);
        await bro.waitForVisible(appOneSelector.name, 2000);
        await bro.changeTextElement(appOneSelector.name, 'Тестирование');

        const appTwoSelector = await indexPage.appsSearchAppById(bro, 1, 2);
        await bro.changeTextElement(appTwoSelector.name, 'Тестирование');

        const appsSearchAppsOne = await indexPage.searchAppsList(bro, 1);
        await bro.assertView('app-editor-22-appsSearchApp', appsSearchAppsOne.list);
    });

    it('Длинное название сервиса в блоке "Все сервисы" должно уходить в троеточие', async function() {
        const bro = this.browser;

        await indexPage.openAndCheckPage(bro);
        await bro.waitForVisible(indexPage.appsSearchRecentAppFirst, 5000);

        const appAllSelector = await indexPage.searchAppsList(bro, 2);
        await bro.waitForVisible(appAllSelector.list, 2000);

        const appOneSelector = await indexPage.appsSearchAppById(bro, 2, 1);
        const appTwoSelector = await indexPage.appsSearchAppById(bro, 2, 2);

        await bro.scroll(appOneSelector.name);

        await bro.changeTextElement(appOneSelector.name, 'Тестирование');
        await bro.changeTextElement(appTwoSelector.name, 'Тестирование');

        const appsSearchAppsOne = await indexPage.searchAppsList(bro, 2);

        await bro.pause(5000);
        await bro.assertView('app-editor-22-appsSearchApp', appsSearchAppsOne.list);
    });
});
