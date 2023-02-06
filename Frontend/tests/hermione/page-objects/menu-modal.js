/**
 * Содержит селекторы информационного меню сервиса
 */

module.exports = {
    /**
     * Селекторы информационного меню
     */
    button: '[class^="Menu_button__"]',
    content: '[data-testid="MenuModal_content"]',
    link: '[data-testid="MenuModal_content"] .ListItem-Content',
    promo: '[class^="MenuModal_promo__"]',
    sideControl: '[class^="MenuModal_link__"] .SideBlock-Control',
    subModal: '[data-testid="SubMenuModal_content"]',
    subModelLink: '[data-testid="SubMenuModal_content"] .ListItem-Content',
    userInfo: '[class^="MenuModal_user__"]',

    /**
     * Осуществляет поиск ссылок в информационном меню
     *
     * @param {Object} bro
     * @param {String} linkName
     * @returns {Promise}
     */
    async searchLinkByName(bro, linkName) {
        return await bro.searchElementByPropertiesValue(bro, this.link, 'textContent', linkName);
    },

    /**
     * Осуществляет поиск ссылок в подменю информационного меню
     *
     * @param {Object} bro
     * @param {String} linkName
     * @returns {Promise}
     */
    async searchSubModelLinkByName(bro, linkName) {
        return await bro.searchElementByPropertiesValue(bro, this.subModelLink, 'textContent', linkName);
    },

    /**
     * Нажимает на ссылку по переданному названию ссылки
     *
     * @param {String} bro
     * @param {Object} nameLink
     * @returns {Promise}
     */
    async clickLink(bro, nameLink) {
        const link = await this.searchLinkByName(bro, nameLink);
        await bro.click(link);
    },

    /**
     * Нажимает на ссылку по переданному названию ссылки в под модалке
     *
     * @param {String} bro
     * @param {Object} nameLink
     * @returns {Promise}
     */
    async clickSubModelLink(bro, nameLink) {
        const link = await this.searchSubModelLinkByName(bro, nameLink);
        await bro.click(link);
    },

    /**
     * Открывает информационное меню
     *
     * @param {Object} bro
     * @returns {Promise}
     */
    async open(bro) {
        await bro.click(this.button);
        await bro.waitForVisible(this.content, 5000);
        await bro.waitModalOpen();
    },
};
