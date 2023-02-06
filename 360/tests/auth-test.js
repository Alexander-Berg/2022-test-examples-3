const PageObjects = require('../page-objects/public');
const getUser = require('@ps-int/ufo-hermione/helpers/getUser')(require('../config/index').login);
const { PUBLIC_PDF_FILE_URL, WAITING_AUTH_TIMEOUT } = require('../config/index').consts;

describe('Авторизация ->', () => {
    beforeEach(async function() {
        await this.browser.url(PUBLIC_PDF_FILE_URL);
    });

    it('Окончание получения авторизации со всех доменов', async function() {
        await this.browser
            .yaWaitForVisible(
                PageObjects.loginLinkButton(),
                WAITING_AUTH_TIMEOUT,
                'сбор авторизации не завершился за отведённое время'
            );
    });

    it('diskpublic-548: diskpublic-266: Смоук: Простая авторизация (одним пользователем)', async function() {
        await this.browser.yaPublicWaitUnauthorizedInAllDomains();
        await this.browser.yaPublicFirstUserAuthorize(getUser('test'));
    });
    it('diskpublic-549: diskpublic-259: Авторизация b2b пользователем', async function() {
        await this.browser.yaPublicWaitUnauthorizedInAllDomains();
        await this.browser.yaPublicFirstUserAuthorize(getUser('b2b'));
    });
    it('diskpublic-550: diskpublic-260: Авторизация pdd пользователем', async function() {
        await this.browser.yaPublicWaitUnauthorizedInAllDomains();
        await this.browser.yaPublicFirstUserAuthorize(getUser('pdd'));
    });

    it('diskpublic-564: diskpublic-268: Мультиавторизация (двумя пользователями)', async function() {
        await this.browser.yaPublicWaitUnauthorizedInAllDomains();
        await this.browser.yaPublicFirstUserAuthorize(getUser('test'));
        await this.browser.yaSecondUserAuthorize(getUser('test2'));
    });

    it('diskpublic-1702: diskpublic-2264: Переключение пользователей', async function() {
        await this.browser.yaPublicWaitUnauthorizedInAllDomains();
        await this.browser.yaPublicFirstUserAuthorize(getUser('test'));
        await this.browser.yaSecondUserAuthorize(getUser('test2'));
        await this.browser.yaClick(PageObjects.legoUser.userPic());
        await this.browser.yaClick(PageObjects.legoUser.changeUser());
        await this.browser.yaWaitForVisible(
            PageObjects.legoUser.userPic(),
            WAITING_AUTH_TIMEOUT,
            'Не дождались появления аватарки пользователя после авторизации'
        );
    });

    it('diskpublic-1641: diskpublic-1794: Смоук: Разлогин', async function() {
        await this.browser.yaClick(PageObjects.loginLinkButton());
        await this.browser.login(getUser('test'));
        await this.browser.logout();
        await this.browser.yaWaitForVisible(PageObjects.loginLinkButton(), 'Кнопка логина не появилась');
    });

    it('diskpublic-565: diskpublic-2265: AssertView: Проверка отображения попапа мультиавторизации', async function() {
        await this.browser.yaClick(PageObjects.loginLinkButton());
        await this.browser.login(getUser('test'));
        await this.browser.yaWaitForVisible(PageObjects.legoUser.userPic(),
            'Не дождались появления аватарки пользователя после авторизации'
        );
        await this.browser.yaClick(PageObjects.legoUser.userPic());
        await this.browser.yaWaitForVisible(PageObjects.legoUser.popup());
        await this.browser.assertView('auth-popup', PageObjects.legoUser.popup(), {
            ignoreElements: [
                PageObjects.legoUser.ticker(),
                PageObjects.legoUser.popup.counter(),
            ]
        });
    });
});
