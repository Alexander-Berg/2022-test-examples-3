const consts = require('../config/index').consts;
const PageObjects = require('../page-objects/public');

describe('Несуществующий паблик -> ', () => {
    it('diskpublic-491: diskpublic-276: Смоук: Переход по несуществующей ссылке', async function() {
        const bro = this.browser;

        await bro.url(consts.PUBLIC_404_FILE_URL);
        await bro.yaWaitForVisible(
            PageObjects.error.errorIconBrokenLink(),
            'Иконка заблокированного файла не отобразилась'
        );
        await bro.yaWaitForVisible(PageObjects.error.errorTitle(), 'Заголовок ошибки не отобразился');
        await bro.yaWaitForVisible(PageObjects.error.errorDescription(), 'Описание ошибки не отобразилось');
    });

    it('diskpublic-1951: diskpublic-1950: Смоук: Заглушка 404. Ссылка удалена', async function() {
        const bro = this.browser;

        await bro.url(consts.PUBLIC_404_REMOVED_URL);
        await bro.yaWaitForVisible(
            PageObjects.error.errorIconBrokenLink(),
            'Иконка заблокированного файла не отобразилась'
        );
        await bro.yaWaitForVisible(PageObjects.error.errorTitle(), 'Заголовок ошибки не отобразился');
        await bro.yaWaitForVisible(PageObjects.error.errorDescription(), 'Описание ошибки не отобразилось');
    });

    it('diskpublic-1694: diskpublic-2278: Проверка директа', async function() {
        const bro = this.browser;

        await bro.url(consts.PUBLIC_404_FILE_URL);
        await bro.yaWaitForVisible(PageObjects.directFrame(), 'Отсутствует полоска директа');
    });

    it('diskpublic-491: diskpublic-276: AssertView: Проверка отображения 404 ошибки', async function() {
        const bro = this.browser;

        await bro.url(consts.PUBLIC_404_FILE_URL);
        await bro.yaWaitForVisible(PageObjects.error.errorIconBrokenLink());
        await bro.assertView('error-404', PageObjects.publicMain());
    });
});
