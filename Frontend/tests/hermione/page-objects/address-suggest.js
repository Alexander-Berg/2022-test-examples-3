/**
 * Содержит модалки выбора адреса
 */

module.exports = {
    /**
     * Селекторы модалки выбора адреса
     */
    modal: '[class^="AddressSuggest_modal__"]',
    container: '[data-testid="AddressSuggest_container"]',
    fromInput: '[class*="AddressSuggest_search-from"] .Textarea',
    fromInputFocused: '[class*="AddressSuggest_search-from"] .Textarea_focused',
    toInput: '[class*="AddressSuggest_search"] div:nth-of-type(2) .Textarea',
    toInputIcon: '[class*="AddressSuggest_search"] div:nth-of-type(2) [class^= AddressTextField_icon__]',
    inputClearButton: '[data-testid="AddressSuggest_container"] .Textarea-Clear_visible',
    mapButtonFrom: '[class*="AddressSuggest_search-from__"] [class*="AddressTextField_map"]',
    mapButton: '[data-testid="AddressSuggest_container"] [class*="AddressTextField_map_visible__"]',
    result: '[data-testid="AddressSuggest_container"] .VerticalScroll',
    resultEmpty: '[data-testid="AddressSuggest_container"] [class*="AddressResults_empty__"]',
    resultFirst: '[data-testid="AddressSuggest_container"] [class*="AddressResult_result"]',
    resultSkeleton: '[data-testid="AddressSuggest_container"] [class^="AddressResultsSkeleton_item"]:first-child',
    sideBlock: '[class*="AddressSuggest_modal"] .SideBlock-Control',
    search: '[data-testid="AddressSuggest_container"] [class^="AddressSuggest_search__"]',

    /**
     * Селекторы поиска поле ввода "Откуда" и "Куда"
     */
    searchFrom: 0,
    searchTo: 1,

    /**
     * Поиск поля ввода
     *
     * @param {Object} bro
     * @param {Number} selectionPos
     * @returns {Promise}
     */
    async searchInput(bro, selectionPos) {
        const addressField = '[class*="AddressTextField_container"]';
        const selectors = await bro.findElements(addressField);
        const selector = selectors[selectionPos];

        return {
            selector,
            input: `${selector} .Textarea`,
            inputFocused: `${selector} .Textarea_focused`,
        };
    },

    /**
     * Нажимает на кнопку очищения поля ввода
     *
     * @param {Object} bro
     * @returns {Promise}
     */
    async clickClearButton(bro) {
        await bro.click(this.inputClearButton);
        await bro.waitForVisible(this.inputClearButton, 5000, true);
    },

    /**
     * Заполняет поле ввода
     *
     * @param {String} bro
     * @param {Object} addressName
     * @returns {Promise}
     */
    async fillInput(bro, addressName) {
        await bro.keys(addressName);
        await bro.waitForVisible(this.resultSkeleton, 10000, true);
    },

    /**
     * Кликает на первый результат поиска
     *
     * @param {String} bro
     * @param {Number} selectionPos
     * @returns {Promise}
     */
    async clickFirstResult(bro, selectionPos) {
        // Ожидаем появления результатов поиска после изсчезновения скелетона
        await bro.pause(1000);

        const addressSuggest = await this.searchInput(bro, selectionPos);
        const result = await this.searchResult(bro, 0);
        await bro.click(result.selector);
        await bro.waitForVisible(addressSuggest.inputFocused, 5000, true);
    },

    /**
     * Поиск элементов в результатах поиска
     *
     * @param {Object} bro
     * @param {Number} selectionPos
     * @returns {Promise}
     */
    async searchResult(bro, selectionPos) {
        const elem = '[data-testid="AddressSuggest_container"] [class^="AddressResult_result-info"]';
        const selectors = await bro.findElements(elem);

        return {
            selector: `${selectors[selectionPos]}`,
        };
    },

    /**
     * В модалке выбора адреса заполняет поле ввода
     * И кликает на первый результат поиска
     *
     * @param {String} bro
     * @param {Object} addressName
     * @param {Number} selectionPos
     * @returns {Promise}
     */
    async fillInputAndClickFirstResult(bro, addressName, selectionPos) {
        await this.fillInput(bro, addressName);
        await this.clickFirstResult(bro, selectionPos);
    },
};
