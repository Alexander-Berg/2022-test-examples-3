module.exports = {
    /**
     * Селекторы блока ввода адреса "Откуда" "Куда"
     */
    fromInputText: '[class^="AddressesForm_address"]:nth-of-type(1) [class^=AddressView_text__]',
    porchButton: '[data-testid="PorchButton_text"]',
    positionFirst: '[class*="AddressView_position__"]:first-child',
    fromInput: '[class^="AddressesForm_address"]:nth-of-type(1)',
    toInput: '[class^="AddressesForm_address"]:nth-of-type(2)',
    time: '[class^="AddressDestinationText_time__"]',
    toInputDesk: '[class^="AddressField_container__"]:nth-of-type(2)',
    positionFirstSuggest: '[class^="AddressResult_result__"]:nth-of-type(1)',
    /**
     * В поле "Куда" нажимает на облачко первого адреса
     *
     * @param {Object} bro
     * @returns {Promise}
     */
    async clickPositionFirst(bro) {
        await bro.waitForVisible(this.positionFirst, 20000);

        await bro.click(this.positionFirst);
        await bro.waitForVisible(this.positionFirst, 5000, true);
    },

    /**
     * В поле "Куда" нажимает на первый адрес в саджесте
     *
     * @param {Object} bro
     * @returns {Promise}
     */
    async clickSuggestPositionFirst(bro) {
        await bro.click(this.toInputDesk);
        await bro.waitForVisible(this.positionFirstSuggest, 5000);
        await bro.click(this.positionFirstSuggest);
    },
};
