const assert = require('assert');
const URL = require('url');

/**
 * Базовая страница
 * Содержит элементы, которые есть на любой странице
 */

module.exports = {
    /**
     * Селектор страницы
     */
    app: '.App',
    appPage: '[class^="App_app"]',
    pinnedAppsCloseButton: '[class^="Header_close-button"]',

    /**
     * Селекторы элементов панели с запиненными приложениями
     */
    pinnedAppsPanel: '[class^="PinnedApps_container"]',
    pinnedAppsPanelTitle: '[class^="PinnedApps_container"] [class^="PinnedApps_title"]',
    pinnedAppsPanelFirstCellActive: '[class^="Carousel_page"]:first-child [class^="TableList_table"] [class^="TableList_row"]:first-child [class^="TableList_cell"]:first-child [class*="ListCell_active"]',
    pinnedAppsPanelCellSkeleton: '[class^="Carousel_page"]:first-child [class^="TableList_table"] [class^="TableList_row"]:first-child [class^="TableList_cell"]:first-child [class^="Skeletons_rect"]',
    pinnedPaginatorActive(paginatorPosition) {
        return `[class^="Carousel_index"][class*="Carousel_active-index"]:nth-of-type(${paginatorPosition})`;
    },

    /**
     * Осуществляет поиск селекторов экран в панели редактирования по его переданной позиции
     *
     * @param {Object} bro
     * @param {Number} selectionPos
     * @returns {Promise}
     */
    async carouselPage(bro, selectionPos) {
        const pages = '[class^="Carousel_page"]';
        const selectors = await bro.findElements(pages);

        return {
            selector: `${selectors[selectionPos]}`,
            cellActive: `${selectors[selectionPos]} [class*="ListCell_active"]`
        };
    },

    /**
     * Осуществляет поиск селекторов ячейки в панели запиненных приложений
     *
     * @param {Number} carouselPosition начинается с 1
     * @param {Number} tableListPosition начинается с 1
     * @param {Number} appPosition начинается с 1
     * @returns {String}
     */
    searchPinnedApp(carouselPosition, tableListPosition, appPosition) {
        const selector = `[class^="Carousel_page"]:nth-of-type(${carouselPosition}) [class^="TableList_table"] [class^="TableList_row"]:nth-of-type(${tableListPosition}) [class^="TableList_cell"]:nth-of-type(${appPosition})`;

        return {
            cell: selector,
            cellActive: `${selector} [class*="ListCell_active"]`,
            cellEmpty: `${selector} [class*="ListCell_cell-empty"]`,
            buttonDelete: `${selector} [class^="ListCell_delete"]`,
            icon: `${selector} [class^="ListCell_icon"]`,
            name: `${selector} [class^="ListCell_name"]`
        };
    },

    /**
     * Селекторы элементов поиска приложений
     */
    appsSearchInput: '[class^="AppsSearch_container"] .Textinput .Textinput-Control',
    appsSearchPopup: '[class*="AppsSearch_container_shown"]',
    appsSearchButtonClose: '[class*="Search_backButton"]',
    appsSearchPopupInput: '[class*="AppsSearch_container_shown"] .Textinput',
    appsSearchPopupInputFocused: '[class*="AppsSearch_container_shown"] .Textinput_focused',
    appsSearchPopupInputClear: '[class*="AppsSearch_container_shown"] .Textinput .Textinput-Clear_visible',
    appsSearchPopupErrorMessage: '[class*="AppsSearch_container_shown"] [class^="Search_notFound"]',
    appsSearchPopupFirstResult: '[class*="AppsSearch_container_shown"] [class^="Search_results"] [class^="SearchResult_container"]:first-child',

    /**
     * Осуществляет поиск элементов результатов поиска по их названию
     *
     * @param {Object} bro
     * @param {String} name
     * @returns {Promise}
     */
    async appsSearchPopupResultByName(bro, name) {
        const items = '[class^="SearchResult_container"]';
        const selectors = await bro.findElementsWithCondition(items, (elem, args) => {
            const nameElem = elem.querySelector('[class^="SearchResult_name"]');
            return nameElem && nameElem.innerText.trim() === args[0];
        }, [name]);

        if (!selectors.length) {
            throw new Error(`Не получилось найти элемент в попапе поиска с переданным названием "${name}"`);
        }

        return {
            result: `${selectors[0]}`,
            addButton: `${selectors[0]} [class^="SearchResult_add-button"]`
        };
    },

    /**
     * Селекторы блоков "Добавить недавние" и "Рекомендованные приложения"
     */
    appsSearchContainer: '[class^="AppsSearch_container"]',
    appsSearchAppsHidden: '[class^="AppsSearch_apps_hidden"]',
    appsSearchAppsFirstTitle: '[class^="AppsSearch_apps"] [class^="AppsList_title"]:first-child',
    appsSearchRecentAppFirst: '[class^="AppsSearch_apps"] [class^="AppsList_container"]:first-child [class^="AppsList_cell"]:first-child [class*="ListCell_stub-hidden"]',

    /**
     * Осуществляет поиск блоков  "Добавить недавние" и "Рекомендованные приложения"
     *
     * @param {Object} bro
     * @param {Number} appsListPosition
     * @returns {String}
     */
    async searchAppsList(bro, appsListPosition) {
        const appsSearch = '[class^="AppsSearch_apps"] [class^="AppsList_list"]';
        const selectors = await bro.findElements(appsSearch);

        return {
            list: selectors[appsListPosition],
            cell: `${selectors[appsListPosition]} [class^="AppsList_cell"]`
        };
    },

    /**
     * Осуществляет поиск селекторов приложение блоков "Добавить недавние"
     * и "Рекомендованные приложения" по его расположению
     *
     * @param {Object} bro
     * @param {Number} appsListPosition
     * @param {Number} appPosition
     * @returns {String}
     */
    async appsSearchAppById(bro, appsListPosition, appPosition) {
        const selectorApps = await this.searchAppsList(bro, appsListPosition);
        const selector = `${selectorApps.list} [class^="AppsList_cell"]:nth-of-type(${appPosition})`;

        return {
            app: selector,
            icon: `${selector} [class^="ListCell_icon"]`,
            name: `${selector} [class^="ListCell_name"]`
        };
    },

    /**
     * @param {Object} [query]
     * @returns {String}
     */
    getUrl(query) {
        return URL.format({
            query
        });
    },

    /**
     * Открывает главную страницу редактора
     * и проверяет, что страница загрузилась
     *
     *
     * @param {Object} bro
     * @param {*} args Параметры для метода getUrl
     * @returns {Promise}
     */
    async openAndCheckPage(bro, ...args) {
        await bro.openPage(this, ...args);
        await bro.waitForVisible(this.pinnedAppsPanel, 5000);
        await bro.waitForVisible(this.pinnedAppsPanelCellSkeleton, 5000, true);
    },

    /**
     * Закрывает главную страницу редактора
     * и проверяет, что в консоль вывелось соответствующее сообщение о закрытии
     *
     *
     * @param {Object} bro
     * @returns {Promise}
     */
    async closeAndCheckClosingPage(bro) {
        await bro.waitForVisible(this.pinnedAppsCloseButton, 5000);
        await bro.click(this.pinnedAppsCloseButton);

        const messages = (await bro.log('browser')).value;
        const consoleApiMessages = messages.filter(msg => msg.source === 'console-api' && msg.level === 'WARNING');
        const texts = consoleApiMessages.map(msg => msg.message.split('"').slice(1, -1).join(''));

        assert.deepStrictEqual(texts, ['Go back!'], `Отображается сообщение '${texts}' вместо сообщения 'Go back!' `);
    },

    /**
     * Нажимает на строку поиска
     * и ожидает появления фокуса в поле ввода попапа поиска
     *
     * @param {Object} bro
     * @returns {Promise}
     */
    async openAndCheckSearchPopup(bro) {
        await bro.click(this.appsSearchInput);
        await bro.waitForVisible(this.appsSearchPopupInputFocused, 2000);
    },

    /**
     * Заполняет поле ввода в попапе поиска
     * и проверяет отображение результатов поиска
     *
     * @param {Object} bro
     * @param {String} text
     * @returns {Promise}
     */
    async fillInputAndCheckResult(bro, text) {
        await bro.keys(text);
        await bro.waitForVisible(this.appsSearchPopupFirstResult, 5000);
    },

    /**
     * Заполняет поле ввода в попапе поиска
     * и проверяет отображение сообщения об ошибке
     *
     * @param {Object} bro
     * @param {String} text
     * @returns {Promise}
     */
    async fillingInputAndCheckErrorResult(bro, text) {
        await bro.keys(text);
        await bro.waitForVisible(this.appsSearchPopupErrorMessage, 5000);
    },

    /**
     * Очищает поле ввода поиска нажатием на кнопку "Х"
     * проверяет что кнопка "Х" больше не отображается
     * и отсутствует отображение результатов поиска и ошибки
     *
     * @param {Object} bro
     * @returns {Promise}
     */
    async clearInputAndCheckResult(bro) {
        await bro.click(this.appsSearchPopupInputClear);
        await bro.waitForVisible(this.appsSearchPopupInputClear, 5000, true);
        await bro.waitForVisible(this.appsSearchPopupErrorMessage, 5000, true);
        await bro.waitForVisible(this.appsSearchPopupFirstResult, 5000, true);
    },

    /**
     * Закрывает попап поиска нажатием на стрелочку в шапке попапа
     * и ожидает полного закрытия попапа
     *
     * @param {Object} bro
     * @returns {Promise}
     */
    async closeSearchPopup(bro) {
        await bro.click(this.appsSearchButtonClose);
        await bro.waitForVisible(this.appsSearchPopup, 5000, true);
        await bro.waitForVisible(this.appsSearchAppsHidden, 5000, true);
    },

    /**
     * Проверяет, что веденное значение в поле ввода
     * соответствует отображаемому значению в поле ввода поиска
     *
     * @param {Object} bro
     * @param {String} text
     * @returns {Promise}
     */
    async assertCurrentTextInSearchInput(bro, text) {
        const textCurrent = await bro.getAttribute(this.appsSearchInput, 'defaultValue');
        assert.strictEqual(textCurrent, `${text}`, `В инпуте содержится текст "${textCurrent}", а должен "${text}"`);
    },

    /**
     * Нажимает на строку поиска
     * ожидает появления фокуса в поле ввода попапа поиска
     * и появления результатов поиска
     *
     * @param {Object} bro
     * @returns {Promise}
     */
    async openAndCheckSearchPopupResult(bro) {
        await this.openAndCheckSearchPopup(bro);
        await bro.waitForVisible(this.appsSearchPopupFirstResult, 5000);
    },

    /**
     * Осуществляет поиск сервиса по его названию в результатах поиска
     * в строке найденного сервиса нажимает на кнопку "Добавить"
     * ожидает закрытия попапа поиска
     * проверяет, что поле ввода очищается после добавления сервиса
     *
     * @param {Object} bro
     * @param {String} nameApp
     * @returns {Promise}
     */
    async searchAppAndAddInSearchPopup(bro, nameApp) {
        const resultSelector = await this.appsSearchPopupResultByName(bro, nameApp);
        await bro.click(resultSelector.addButton);
        await bro.waitForVisible(this.appsSearchAppsHidden, 5000, true);
        await this.assertCurrentTextInSearchInput(bro, '');
    },
};
