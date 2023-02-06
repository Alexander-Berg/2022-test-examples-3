/**
 * Содержит селекторы блока "Способ оплаты"
 */

module.exports = {
    /**
     * Селектор блока способа оплаты на главном экране
     */
    selected: '[class^="MainScreen_order"] [class*="PaymentSelector_container-visible_yes__"]',
    paymentButton: '[class*="OrderForm_option"] [class*="PaymentMethodButton_container__"]',
    paymentButtonDesktop: '[class*="OrderForm_form__"] [class*="PaymentMethodButton_container__"]',

    /**
     * Селекторы модалки "Способ оплаты"
     */
    modal: '[class^="PaymentModal_modal__"]',
    modalDesktop: '[class^="PaymentModal_container__"]',
    paymentMethod: '[class^="PaymentMethods_item__"]:nth-child(1)',
    readyButton: '[class*="PaymentModal_button__"]'
};
