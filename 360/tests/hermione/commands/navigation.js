const { NAVIGATION, ASSERT_TIMEOUT } = require('../config').consts;
const { photo } = require('../page-objects/client-photo2-page').common;
const navigation = require('../page-objects/client-navigation');
const { retriable } = require('@ps-int/ufo-hermione/helpers/retries');
const assert = require('chai').assert;

/**
 * @param {string} sectionName
 * @returns {Object}
 */
const getNavigation = (sectionName) => {
    if (!NAVIGATION[sectionName]) {
        throw new Error('Попытка открыть несуществующий раздел - ' + sectionName);
    } else {
        return NAVIGATION[sectionName];
    }
};

const actions = {
    //desktop-имплементации
    desktop: {
        /**
         * Открывает раздел и проверяет появление контента
         *
         * @param {string} sectionName
         * @param {boolean} [withoutListing] = false
         * @returns {Promise<void>}
         */
        async yaOpenSection(sectionName, withoutListing = false) {
            const section = getNavigation(sectionName);

            await this.yaWaitForVisible(navigation.desktop.sidebarNavigation());
            await this.element(navigation.desktop.sidebarNavigation());
            //wdio будет разрешать следующий селектор из click относительно выбранного в .element()
            //в click() селектор вида a*="text", такая фича вебдрайвера
            if (sectionName === 'tuning') {
                await this.yaOpenLinkInNewTab(
                    navigation.desktop.navigationItemTuning(),
                    { assertUrlHas: 'tuning' }
                );
            } else {
                await this.click(
                    navigation.desktop['navigationItem' + section.name](),
                    'Не получилось совершить клик на кнопку открытия раздела ' + sectionName
                );
            }
            if (withoutListing) {
                return;
            }
            return this.yaAssertSectionOpened(sectionName);
        },
    },
    //touch-имплементации
    touch: {
        /**
         * Открывает раздел и проверяет появление контента
         *
         * @param {string} sectionName
         * @param {boolean} [withoutListing] = false
         * @returns {Promise<void>}
         */
        async yaOpenSection(sectionName, withoutListing = false) {
            const section = getNavigation(sectionName);

            await this.yaWaitForVisible(navigation.touch.mobileNavigation());
            await this.element(navigation.touch.mobileNavigation());
            //wdio будет разрешать следующий селектор из click относительно выбранного в .element()
            //в click() селектор вида a*="text", такая фича вебдрайвера
            await this.click(
                navigation.touch['navigationItem' + section.name](),
                'Не получилось совершить клик на кнопку открытия раздела ' + sectionName
            );

            if (withoutListing) {
                return;
            }

            if (sectionName === 'photo') {
                return this.yaAssertPhotoSectionOpened();
            }
            return this.yaAssertSectionOpened(sectionName);
        },
        //отдельная реализация для раздела 'Фото' тач-версии, потому что в этом разделе нет заголовка
        /**
         * Открывает раздел "Фото"
         *
         * @returns {Promise<void>}
         */
        async yaAssertPhotoSectionOpened() {
            await this.yaWaitForVisible(navigation.touch.mobileNavigation());
            await this.element(navigation.touch.mobileNavigation());
            //wdio будет разрешать следующий селектор из click относительно выбранного в .element()
            //в click() селектор вида a*="text", такая фича вебдрайвера
            await this.click(
                navigation.touch.navigationItemPhoto(),
                'Не получилось совершить клик на кнопку открытия раздела Фото'
            );

            await this.yaWaitForVisible(photo.item(), 10000);
            const headerText = await this.getText(navigation.touch.navigationItemPhoto());
            assert.equal(headerText, 'ФОТО');
        }
    },
    common: {
        /**
         * Проверяет, содержит ли текущий URL подстроку
         *
         * @param {string} linkShouldContain
         * @returns {Promise<void>}
         */
        async yaAssertUrlInclude(linkShouldContain) {
            // ФФ на некоторое время устанавливает новой вкладке адрес "about:blank", поэтому пробуем несколько раз
            await retriable(async() => {
                let currentUrl = await this.getUrl();
                currentUrl = decodeURI(currentUrl);
                assert.include(currentUrl, linkShouldContain);
            }, 10, 500);
        },
        /**
         * Проверяет, открыт ли раздел
         *
         * @param {string} sectionName
         * @returns {Promise<void>}
         */
        async yaAssertSectionOpened(sectionName) {
            const section = getNavigation(sectionName);

            await this.yaWaitForVisible(section.selectors.content);
            await this.yaWaitForVisible(section.selectors.header);

            const headerText = await this.getText(section.selectors.header);

            assert.equal(headerText, section.contentTitle);
        },
        /**
         * Проверяет, открыта ли папка
         *
         * @param {string} folderName
         * @returns {Promise<void>}
         */
        async yaAssertFolderOpened(folderName) {
            const folder = NAVIGATION.folder(folderName);

            const folderContent = await this.$(folder.selectors.content);
            const folderHeader = await this.$(folder.selectors.header);

            await folderContent.waitForDisplayed();
            await folderHeader.waitForDisplayed();

            await folderHeader.waitUntil(async function() {
                return (await this.getText() === folderName);
            }, { timeout: ASSERT_TIMEOUT, timeoutMsg: `Папка ${folderName} не открыта` });
        },
        /**
         * Переключает браузер на другую вкладку
         *
         * @returns {Promise<void>}
         */
        async yaSwitchTab() {
            await this.pause(1000); // вкладка открывается не моментально
            const currentTab = await this.getCurrentTabId();
            const openedTabs = await this.getTabIds();
            assert(openedTabs.length > 1, 'Вкладка не открылась');
            const secondTab = openedTabs.find((id) => id !== currentTab);
            await this.switchTab(secondTab);
        },
        /**
         * По селектору элемента открывает ссылку в новой вкладке
         *
         * @param {string} selector
         * @param {Object} options
         * @param {boolean} options.doDoubleClick
         * @param {string} options.assertUrlHas
         * @returns {Promise<void>}
         */
        async yaOpenLinkInNewTab(selector, options = { doDoubleClick: false, assertUrlHas: '' }) {
            if (options.doDoubleClick) {
                await this.yaWaitForVisible(selector);
                await this.doubleClick(selector);
            } else {
                await this.yaWaitForVisible(selector);
                await this.click(selector);
            }

            await this.yaSwitchTab();

            await this.yaAssertUrlInclude(options.assertUrlHas);
        },
        /**
         * @param {string} url
         * @param {string} linkShouldContain
         * @param {Function} [callback]
         * @returns {Promise<void>}
         */
        async yaOpenUrlAndAssert(url, linkShouldContain, callback) {
            await this.url(url);
            await this.yaAssertUrlInclude(linkShouldContain);
            if (callback && typeof callback === 'function') {
                await callback();
            }
        }
    }
};

module.exports = exports = actions;
