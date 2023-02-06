const addressPoint = require('./address-point');

/**
 * Содержит селекторы модалки "Укажите номер подъезда"
 */

module.exports = {
    addressPoint,

    /**
     * Селекторы модалки "Укажите номер подъезда"
     */
    container: '.SideBlock-Gesture [class*="PorchForm_container__"]',
    closeButton: '[class*="PorchForm_container__"] .Button2_view_default',
    input: '[class*="PorchForm_input__"]',
    inputFocused: '[class*="PorchForm_input__"].Textinput_focused',
    saveButton: '[class*="PorchForm_button__"].Button2_view_action',

    /**
     * Открывает модалку "Укажите номер подъезда"
     *
     * @param {Object} bro
     * @returns {Promise}
     */
    async open(bro) {
        await bro.click(addressPoint.porchButton);
        await bro.waitForVisible(this.container, 5000);
        await bro.waitForVisible(this.inputFocused, 5000);
    },
};
