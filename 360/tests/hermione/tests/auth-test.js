const PageObjects = require('@ps-int/ufo-hermione/page-objects/auth');
const { authLoginLink, header, psHeader } = require('../page-objects/client');
const getUser = require('@ps-int/ufo-hermione/helpers/getUser')(require('../config').login);
const { NAVIGATION, PASSPORT_URL } = require('../config').consts;
const assert = require('chai').assert;

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

const getUserPicSelector = (isMobile) => isMobile ? PageObjects.user.userPic() : psHeader.legoUser.userPic();

describe('Авторизация ->', () => {
    beforeEach(async function() {
        await this.browser.url(NAVIGATION.disk.url);
    });
    /**
     * @param {string} login
     * @param {Browser} bro
     */
    async function loginAndSkipPromo(login, bro) {
        const isMobile = await bro.yaIsMobile();
        await bro.yaClientLoginFast(login);
        await bro.yaWaitForVisible(getUserPicSelector(isMobile),
            'Не дождались появления аватарки пользователя после авторизации'
        );
    }
    it('diskclient-1529, 1365: Смоук: Авторизация c морды (одним пользователем)', async function() {
        const bro = this.browser;
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-1529' : 'diskclient-1365';

        await bro.url('/');
        await bro.yaWaitForVisible(authLoginLink());
        await bro.click(authLoginLink());
        await loginAndSkipPromo('yndx-ufo-test-00', bro);
    });
    it('diskclient-1168, 716: Авторизация b2b пользователем', async function() {
        const bro = this.browser;
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-1168' : 'diskclient-716';

        await loginAndSkipPromo('b2b', bro);
    });
    it('diskclient-1169, 717: Авторизация pdd пользователем', async function() {
        const bro = this.browser;
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-1169' : 'diskclient-717';

        await loginAndSkipPromo('pdd', bro);
    });
    it('diskclient-1392, 713: Смоук: Мультиавторизация (двумя пользователями)', async function() {
        const bro = this.browser;
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-1392' : 'diskclient-713';

        await loginAndSkipPromo('yndx-ufo-test-00', bro);
        await bro.yaSecondUserAuthorize(getUser('yndx-ufo-test-01'));
    });
    it('diskclient-1176, 1531: Смоук: Переключение пользователей', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1176' : 'diskclient-1531';

        await loginAndSkipPromo('yndx-ufo-test-00', bro);
        await bro.yaSecondUserAuthorize(getUser('yndx-ufo-test-01'));
        await bro.yaSkipWelcomePopup();
        await bro.click(getUserPicSelector(isMobile));
        await bro.click(
            isMobile ?
                PageObjects.userMenu.changeUserButton() :
                psHeader.legoUser.popup.changeUser()
        );
        await bro.yaWaitForVisible(
            getUserPicSelector(isMobile),
            'Не дождались появления аватарки пользователя после авторизации'
        );
    });
    it('diskclient-1528, 1530: Разлогин', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1528' : 'diskclient-1530';

        await loginAndSkipPromo('yndx-ufo-test-00', bro);
        await bro.logout();

        await bro.yaAssertUrlInclude(PASSPORT_URL);
    });
});

hermione.only.notIn('chrome-phone-6.0');
describe('Авторизация ->', () => {
    it('diskclient-1704, 1705: assertView: Проверка отображения попапа мультиавторизации', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        const testpalmId = isMobile ? 'diskclient-1704' : 'diskclient-1705';
        this.testpalmId = testpalmId;

        await bro.yaClientLoginFast('yndx-ufo-test-00');
        await bro.click(getUserPicSelector(isMobile));
        const popupSelector = isMobile ? PageObjects.userMenu() : psHeader.legoUser.popup.inner();
        await bro.yaWaitForVisible(popupSelector);
        await bro.assertView(testpalmId, popupSelector);
    });

    it('diskclient-1092, 5450: [Plus] Проверка отображения шапки и попапа мультиавторизации для пользователя с плюсом', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5450' : 'diskclient-1092';

        await bro.yaClientLoginFast('yndx-ufo-test-plus');

        await bro.assertView(`${this.testpalmId}-1`, isMobile ? header() : psHeader());

        await bro.click(getUserPicSelector(isMobile));
        const popupSelector = isMobile ? PageObjects.userMenu() : psHeader.legoUser.popup.inner();
        await bro.yaWaitForVisible(popupSelector);
        await bro.assertView(`${this.testpalmId}-2`, popupSelector);
    });
});

describe('Авторизация ->', () => {
    /**
     * Авторизация в ВК
     *
     * @param {Object} browser
     * @param {Object} loginPage
     * @param {Object} user
     */
    async function logInVK(browser, loginPage, user) {
        await browser.url(loginPage.url);
        await browser.yaSetValue(loginPage.email, user.login);
        await browser.yaSetValue(loginPage.password, user.password);
        await browser.click(loginPage.submitButton);
    }

    const passport = {
        loginInput: '#passp-field-login[value]',
        vkButton: '.AuthSocialBlock-provider_code_vk'
    };

    it('diskclient-1042, diskclient-5405: Авторизация по ссылке с логином и uid\'ом', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1042' : 'diskclient-5405';

        const user = {
            login: 'yndx-ufo-test-303',
            uid: '887834159'
        };

        await bro.url(`${await bro.options.baseUrl}/client/trash/?uid=${user.uid}&login=${user.login}`);
        await bro.yaAssertUrlInclude(PASSPORT_URL);

        await bro.yaWaitForVisible(passport.loginInput, 'В паспорте поменялся селектор инпута логина');
        const autocompleteLogin = await bro.getValue(passport.loginInput);
        assert.equal(autocompleteLogin, user.login, 'В инпуте не подставлен логин пользователя');
    });

    hermione.auth.tus({ login: 'yndx-ufo-test-304', tus_consumer: 'disk-front-client' });
    it('diskclient-883, diskclient-1170: Авторизация переполненным юзером', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-883' : 'diskclient-1170';

        await bro.yaFreeSpaceIsEqual(0);
        await bro.yaAssertUrlInclude(await bro.options.baseUrl);
    });

    hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-73113');
    hermione.skip.in('chrome-phone', 'мигает – https://st.yandex-team.ru/CHEMODAN-73905');
    it('diskclient-5407, diskclient-1175: Соцавторизация', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1175' : 'diskclient-5407';

        const vk = {
            user: {
                login: 'yndx-ufo-test-305@yandex.ru',
                password: 'testiwan'
            },
            desktop: {
                url: 'https://vk.com',
                email: '#index_email',
                password: '#index_pass',
                submitButton: '#index_login_button'
            },
            touch: {
                url: 'https://m.vk.com',
                email: 'input[name="email"]',
                password: 'input[name="pass"]',
                submitButton: 'input[type="submit"]'
            },
        };

        await logInVK(bro, isMobile ? vk.touch : vk.desktop, vk.user);
        await bro.url(`${PASSPORT_URL}/auth?retpath=${await bro.options.baseUrl}/client/disk`);
        await bro.pause(200);
        await bro.yaWaitForVisible(passport.vkButton, 'В паспорте поменялся селектор соцавторизации ВК', 10000);
        await bro.click(passport.vkButton);
        await bro.yaAssertUrlInclude(await bro.options.baseUrl);
    });
});
