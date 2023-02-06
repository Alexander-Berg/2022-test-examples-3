const URL = require('url');

const basePage = require('./base');

/**
 * Главная страница
 * Содержит элементы, которые есть на главной странице
 */

module.exports = {
    ...basePage,

    /**
     * Селектор главной страницы
     */
    mainPage: '[class*="MainScreen_page"]',

    /**
     * Селектор блока с товарами
     */
    layoutItem(idPosition) {
        return `[class*="MainScreen_layout-item"]:nth-of-type(${idPosition})`;
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
     * Открывает главную страницу магазина
     * и проверяет, что страница загрузилась
     *
     *
     * @param {Object} bro
     * @param {*} args Параметры для метода getUrl
     * @returns {Promise}
     */
    async openAndCheckPage(bro, ...args) {
        await bro.openPage(this, ...args);
        await bro.waitForVisible(this.mainPage, 30000);
    },
};
