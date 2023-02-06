/**
 * Содержит селекторы блока тарифов и информационной модалки о тарифе
 */

module.exports = {
    /**
     * Селекторы блока с карточками тарифов
     */
    tariffsSelector: '[class*="TariffSelector_tariffs-container-visible_yes"]',
    buttonActive: '[data-testid="TariffButton_active"]',
    buttonTitleActive: '[data-testid="TariffButton_active"] [class^="TariffButton_title__"]',
    buttonActivePrice: '[data-testid="TariffButton_active"] [class^="Price_price__"]',
    buttonActivePriceEmpty: '[data-testid="TariffButton_active"] [class^="Price_price_empty"]',
    buttonActivePriceNonEmpty: '[data-testid="TariffButton_active"] [class^="Price_price__"]:not([class*="Price_price_empty"])',
    buttonActiveInfo: '[data-testid="TariffButton_active"] [data-testid="TariffButton_info"]',
    buttonTitle: '[class^="TariffButton_title__"]',
    priceSurge: '[class*="Price_price_surge__"]',
    tariffButtonInRequirementsModal: '[class*="TariffRequirementsModal_details__"] [class*="Button2"]',

    /**
     * Селекторы информационной модалки тарифа
     */
    infoModal: '[class*="TariffInfoModal_container__"]',
    infoModalImage: '[class*="TariffInfoModal_tariff-info__"]',
    infoModalSideControl: '[class^="TariffInfoModal_modal__"] .SideBlock-Control',
    infoModalSurge: '[class^="TariffInfoModal_modal__"] [class^="TariffInfoModal_surge__"]',
    infoModalCancelButton: '[class*="TariffInfoModal_close__"]',

    /**
     * Селекторы модалки требований с информацией о тарифе
     */
    requirementsModalScroller: '[class^="OrderForm_scroll-wrapper__"]',
    requirementsModal: '[data-testid="TariffRequirementsModal_expanded"]',
    requirementsModalTariffInfo: '[class*="TariffRequirementsModal_tariff-info__"]',
    requirementsModalDetailsButton: '[class*="TariffRequirementsModal_details__"]',

    /**
     * Селекторы модалки "Повышенный интерес к такси"
     */
    surgeModal: '[data-testid="SurgeModal_modal"]',

    /**
     * Осуществляет поиск селекторов кнопки тарифа
     *
     * @param {Number} tariffButtonPosition начинается с 1
     * @returns {String}
     */
    getButtonSelectors(tariffButtonPosition) {
        const selector = `[class^="TariffButton_button"]:nth-of-type(${tariffButtonPosition})`;

        return {
            selector,
            price: `${selector} [class^="Price_price"]`,
        };
    },

    /**
     * Осуществляет поиск карточки тарифа
     *
     * @param {Object} bro
     * @param {String} tariffName
     * @returns {Promise}
     */
    async searchButtonByName(bro, tariffName) {
        return await bro.searchElementByPropertiesValue(bro, this.buttonTitle, 'textContent', tariffName);
    },

    /**
     * Осуществляет поиск активной карточки тарифа
     *
     * @param {Object} bro
     * @param {String} tariffName
     * @returns {Promise}
     */
    async searchButtonActiveByName(bro, tariffName) {
        return await bro.searchElementByPropertiesValue(bro, this.buttonTitleActive, 'textContent', tariffName);
    },

    /**
     * Нажимает на карточку найденного тарифа
     *
     * @param {Object} bro
     * @param {String} tariffName
     * @returns {Promise}
     */
    async clickButton(bro, tariffName) {
        const tariffButton = await this.searchButtonByName(bro, tariffName);
        await bro.click(tariffButton);

        const tariffButtonActive = await this.searchButtonActiveByName(bro, tariffName);
        await bro.waitForVisible(tariffButtonActive, 5000);
    },

    /**
     * Нажимает на карточку активного тарифа
     * Ожидает загрузки информационной модалки тарифа
     *
     * @param {Object} bro
     * @returns {Promise}
     */
    async clickTariffActiveAndWaitForInfoModal(bro) {
        await bro.waitForVisible(this.buttonTitleActive, 5000);

        await bro.click(this.buttonTitleActive);
        await bro.waitForVisible(this.infoModalImage, 10000);
    },

    /**
     * Нажимает на карточку активного тарифа
     * Ожидает загрузки модалки требованией с информацией о тарифе
     *
     * @param {Object} bro
     * @returns {Promise}
     */
    async clickTariffActiveAndWaitRequirementsModal(bro) {
        await bro.waitForVisible(this.buttonTitleActive, 5000);

        await bro.click(this.buttonTitleActive);
        await bro.waitForVisible(this.requirementsModal, 10000);
        await bro.pause(1000);
        await bro.waitForVisible(this.requirementsModalTariffInfo, 10000);
    },

    /**
     * Нажимает на карточку активного тарифа
     * Нажимает кнопку "подробнее о тарифе"
     * Ожидает загрузки информационной модалки тарифа
     *
     * @param {Object} bro
     * @returns {Promise}
     */
    async clickTariffActiveAndWaitInfoModal(bro) {
        await this.clickTariffActiveAndWaitRequirementsModal(bro);

        await bro.click(this.requirementsModalDetailsButton);
        await bro.waitForVisible(this.infoModalImage, 10000);
    },
};
