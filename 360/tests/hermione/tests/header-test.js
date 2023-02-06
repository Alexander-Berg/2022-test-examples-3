const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const clientNavigation = require('../page-objects/client-navigation');
const clientPageObjects = require('../page-objects/client');
const { assert } = require('chai');
const { NAVIGATION } = require('../config').consts;

describe('Переходы из шапки диска -> ', () => {
    beforeEach(async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-158');
    });

    hermione.only.in('chrome-phone', 'для десктопа ниже есть кейс diskclient-6177');
    it('diskclient-4870: Лого Яндекса. Переход.', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4870';
        await bro.click(clientNavigation.common.yaLogo());
        await bro.yaAssertUrlInclude('');
    });

    it('diskclient-687 diskclient-4864: Юзер-блок', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-4864' : 'diskclient-687';
        await bro.click(isMobile ? clientNavigation.common.userBlock() : clientPageObjects.psHeader.legoUser());
        await bro.click(isMobile ?
            clientNavigation.common.goToPassport() :
            clientPageObjects.psHeader.legoUser.popup.goToPassport()
        );
        await bro.yaAssertUrlInclude('https://passport.yandex.ru');
    });
});

describe('Отображение шапки -> ', () => {
    it('diskclient-1073, 5439: Отображение юзер-блока', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-264');
        const isMobile = await bro.yaIsMobile();

        const headerSelector = isMobile ? clientPageObjects.header() : clientPageObjects.psHeader();
        await bro.yaWaitForVisible(headerSelector);
        await bro.yaAssertView('diskclient-1073-1', headerSelector);

        const userBlock = isMobile ? clientNavigation.common.userBlock() : clientPageObjects.psHeader.legoUser();
        await bro.yaWaitForVisible(userBlock);
        await bro.click(userBlock);
        const userMenu = isMobile ?
            clientNavigation.common.userMenu() :
            clientPageObjects.psHeader.legoUser.popup.inner();
        await bro.yaWaitForVisible(userMenu);
        await bro.yaAssertView('diskclient-1073-2', userMenu);
    });
});

/**
 * @param {string} serviceSelector
 * @param {string} linkBase
 * @param {Object} [options]
 * @param {boolean} [options.isInPopup]
 * @param {boolean} [options.shouldOpenInNewTab]
 * @returns {Promise<void>}
 */
const assertClickServiceInPSHeader = async function(serviceSelector, linkBase, {
    isInPopup,
    shouldOpenInNewTab,
    fromQueryParam
} = {}) {
    const bro = this.browser;

    if (isInPopup) {
        await bro.click(clientPageObjects.psHeader.more());
        await bro.yaWaitForVisible(clientPageObjects.psHeaderMorePopup());
    }

    await bro.yaWaitForVisible(serviceSelector);
    await bro.click(serviceSelector);

    if (shouldOpenInNewTab) {
        const tabs = await bro.getTabIds();
        assert(tabs.length > 1, 'Новая вкладка не открылась');
        await bro.window(tabs[1]);
    }

    const url = decodeURI(await bro.getUrl());
    assert(url.startsWith(linkBase), `Ожидалось, что текущий урл (${url}) будет начинаться с "${linkBase}"`);

    if (fromQueryParam) {
        assert(url.includes('from=' + fromQueryParam),
            `Ожидалось, что текущий урл (${url}) будет содержать параметр "from=${fromQueryParam}"`);
    }

    if (shouldOpenInNewTab) {
        await bro.close();
    }
};

/**
 * Проверка внешнего вида шапки (сама шапка + попап "Ещё")
 *
 * @param {string} testpalmId
 * @returns {Promise<void>}
 */
const assertPSHeaderView = async function(testpalmId) {
    const bro = this.browser;

    await bro.yaWaitForVisible(clientPageObjects.psHeader());
    await bro.click(clientPageObjects.psHeader.more());
    await bro.yaWaitForVisible(clientPageObjects.psHeaderMorePopup());

    await bro.moveToObject('body', 0, 0); // unhover "More" icon
    await bro.pause(200); // wait unhover animation
    await bro.yaAssertView(testpalmId, 'body', {
        invisibleElements: [
            clientPageObjects.psHeader.calendarDay(),
            clientPageObjects.psHeaderMorePopup.calendarDay()
        ],
        hideElements: clientPageObjects.psHeader.legoUser.ticker(),
        ignoreElements: [clientNavigation.desktop.spaceInfoSection()]
    });
};

hermione.only.in(clientDesktopBrowsersList); // Единая шапка ПС пока только на десктопах
describe('Единая шапка персональных сервисов -> ', () => {
    beforeEach(async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-765');
        await bro.url(NAVIGATION.disk.url);
    });

    it('diskclient-6176: Отображение шапки персональных сервисов', async function() {
        await assertPSHeaderView.call(this, 'diskclient-6176');
    });

    it('diskclient-6192: Отображение шапки персональных сервисов на узком экране', async function() {
        const bro = this.browser;

        await bro.windowHandleSize({ width: 1024, height: 768 });
        await assertPSHeaderView.call(this, 'diskclient-6192_1024x768');

        await bro.click(clientPageObjects.psHeader.more());
        await bro.yaWaitForHidden(clientPageObjects.psHeaderMorePopup());

        await bro.windowHandleSize({ width: 800, height: 600 });
        await assertPSHeaderView.call(this, 'diskclient-6192_800x600');
    });

    it('diskclient-6177: Лого Яндекса. Переход', async function() {
        const bro = this.browser;
        await bro.yaWaitForVisible(clientPageObjects.psHeader());
        const newTabExpectedParams = {
            linkShouldContain: [
                '//360.yandex.ru',
                'from=disk-header-360'
            ]
        };

        await bro.yaClickAndAssertNewTabUrl(clientPageObjects.psHeader.logoYa(), newTabExpectedParams);
        await bro.close();

        await bro.yaClickAndAssertNewTabUrl(clientPageObjects.psHeader.logo360(), newTabExpectedParams);
    });

    it('diskclient-6178: Открытие попапа пользователя', async function() {
        const bro = this.browser;
        await bro.yaWaitForVisible(clientPageObjects.psHeader());
        await bro.click(clientPageObjects.psHeader.legoUser());
        await bro.yaWaitForVisible(clientPageObjects.psHeader.legoUser.popup());
        await bro.yaAssertView('diskclient-6178', clientPageObjects.psHeader.legoUser.popup(), {
            hideElements: clientPageObjects.psHeader.legoUser.popup.unreadCounter()
        });
    });

    it('diskclient-6179: Отображение текущего дня месяца в иконке календаря', async function() {
        const bro = this.browser;
        await bro.yaWaitForVisible(clientPageObjects.psHeader());
        await bro.click(clientPageObjects.psHeader.more());
        await bro.yaWaitForVisible(clientPageObjects.psHeaderMorePopup());
        const calendarDay = await bro.getText(clientPageObjects.psHeaderMorePopup.calendarDay());
        assert.equal(calendarDay, new Date(
            new Date().toLocaleString('en-US', { timeZone: 'Europe/Moscow' })
        ).getDate());
    });

    it('diskclient-6180: Клик по баннеру Про', async function() {
        const bro = this.browser;
        await bro.yaWaitForVisible(clientPageObjects.psHeader.proBanner());
        await bro.click(clientPageObjects.psHeader.proBanner());
        const url = decodeURI(await bro.getUrl());
        assert(url.startsWith('https://mail360.yandex.ru/'));
        assert(url.includes('from=disk_promobutton'));
    });

    it('diskclient-6181: Клик по сервису Почта', async function() {
        await assertClickServiceInPSHeader.call(this, clientPageObjects.psHeader.mail(), 'https://mail.yandex.ru/');
    });

    it('diskclient-6182: Клик по сервису Диск', async function() {
        await assertClickServiceInPSHeader.call(this, clientPageObjects.psHeader.disk(), this.browser.options.baseUrl);
    });

    it('diskclient-6183: Клик по сервису Телемост', async function() {
        await assertClickServiceInPSHeader.call(
            this,
            clientPageObjects.psHeader.telemost(),
            'https://telemost.yandex.ru/'
        );
    });

    it('diskclient-ps-header-docs-click: Клик по сервису Документы', async function() {
        await assertClickServiceInPSHeader.call(
            this,
            clientPageObjects.psHeader.documents(),
            'https://docs.yandex.ru/'
        );
    });

    it('diskclient-6184: Клик по сервису Календарь', async function() {
        await assertClickServiceInPSHeader.call(
            this,
            clientPageObjects.psHeaderMorePopup.calendar(),
            'https://calendar.yandex.ru/',
            { isInPopup: true }
        );
    });

    it('diskclient-6185: Клик по сервису Про', async function() {
        await assertClickServiceInPSHeader.call(
            this,
            clientPageObjects.psHeaderMorePopup.pro(),
            'https://mail360.yandex.ru/',
            {
                isInPopup: true,
                fromQueryParam: 'disk_header'
            }
        );
    });

    it('diskclient-6186: Клик по сервису Заметки', async function() {
        await assertClickServiceInPSHeader.call(
            this,
            clientPageObjects.psHeaderMorePopup.notes(),
            `${await this.browser.options.baseUrl}/notes`,
            {
                isInPopup: true
            }
        );
    });

    it('diskclient-6187: Клик по сервису Контакты', async function() {
        await assertClickServiceInPSHeader.call(
            this,
            clientPageObjects.psHeaderMorePopup.contacts(),
            'https://mail.yandex.ru/',
            {
                isInPopup: true
            }
        );
        const url = decodeURI(await this.browser.getUrl());
        assert(url.includes('#contacts'), 'Ожидалось что текущий урл будет содержать "#contacts"');
    });

    it('diskclient-6188: Клик по сервису Мессенджер', async function() {
        await assertClickServiceInPSHeader.call(
            this,
            clientPageObjects.psHeaderMorePopup.messenger(),
            'https://yandex.ru/chat',
            {
                isInPopup: true,
                shouldOpenInNewTab: true
            }
        );
    });

    it('diskclient-6193: Клик по кнопке "Все сервисы Яндекса"', async function() {
        await assertClickServiceInPSHeader.call(
            this,
            clientPageObjects.psHeaderMorePopup.allServices(),
            'https://yandex.ru/all',
            {
                isInPopup: true
            }
        );
    });
});

hermione.only.in(clientDesktopBrowsersList); // Единая шапка ПС пока только на десктопах
describe('Единая шапка персональных сервисов для админа организации -> ', () => {
    beforeEach(async function () {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-766');
        await bro.url(NAVIGATION.disk.url);
    });

    it('diskclient-6189: Отображение шапки персональных сервисов у b2b-админа', async function () {
        await assertPSHeaderView.call(this, 'diskclient-6189');
    });

    it('diskclient-6190: Клик по сервису "Управление организацией"', async function() {
        await assertClickServiceInPSHeader.call(
            this,
            clientPageObjects.psHeaderMorePopup.adminka(),
            'https://admin.yandex.ru/',
            {
                isInPopup: true
            }
        );
    });
});
