const qs = require('querystring');
const addressInteractiveScreen = require('./address-interactive');
const addressPoint = require('./address-point');
const addressSuggest = require('./address-suggest');
const map = require('./map');
const orderStatus = require('./order-status');
const payment = require('./payment');
const popup = require('./popup');
const porchForm = require('./porch-form');
const tariff = require('./tariff');
const menuModal = require('./menu-modal');
const costCenter = require('./cost-center');

/**
 * Содержит селекторы элементов формы заказа такси на главной странице
 */

module.exports = {
    addressInteractiveScreen,
    addressPoint,
    addressSuggest,
    map,
    orderStatus,
    payment,
    popup,
    porchForm,
    tariff,
    menuModal,
    costCenter,

    /**
     * Корневой элемент страницы
     */
    root: '#root',

    /**
     * Селектор уведомления вверху страницы
     */
    topNotification: '[data-testid="TopNotification"]',

    /**
     * Поиск элементов модалки "Оплата только картой"
     *
     * @param {Object} bro
     * @param {Number} selectionPos
     * @returns {Promise}
     */
    async searchCardAddModal(bro, selectionPos) {
        const cardAddModal = '[class^="CardAddModal_container__"]';
        const selectors = await bro.findElements(cardAddModal);
        const selector = selectors[selectionPos];

        return {
            selector,
            addButton: `${selector} [class*="Button2_view_action"]`,
            cancelButton: `${selector} [class*="CardAddModal_cancel"]`,
        };
    },

    /**
     * Поиск элементов модалки "Добавление карты"
     *
     * @param {Object} bro
     * @param {Number} selectionPos
     * @returns {Promise}
     */
    async searchBindCardModal(bro, selectionPos) {
        const bindCardModal = '.BindCardModal-Wrapper';
        const selectors = await bro.findElements(bindCardModal);
        const selector = selectors[selectionPos];

        return {
            selector,
        };
    },

    /**
     * Селекторы модалки
     */
    commonModal: '[data-testid="CommonModal_container"]',
    commonModalContent: '[data-testid="CommonModal_content"]',
    commonModalButton: '[class*="CommonModal_button__"]',
    commonModalContainerLoading: '[class*="CommonModal_container_loading"]',
    commonModalConfirmButton: '[class*="CommonModal_button"].Button2_view_action',
    commonModalCancelButton: '[class*="CommonModal_button"].Button2_view_default',

    /**
     * Селекторы формы заказа такси
     */
    orderForm: '[class^="MainScreen_order"]',
    orderFormButton: '[class*="OrderConfirm_button__"]',
    orderFormButtonDisabled: '[class*="OrderConfirm_button__"][disabled=""]',
    orderFormHidden: '[class*="OrderForm_hidden"]',
    headerTitle: '[class*="Header_title"]',
    headerTimer: '[class*="ProgressStatus_timer__"]',
    headerProgress: '[class*="ProgressStatus_progress__"]',
    orderFormButtonState: '[class*="OrderForm_button-confirm__"] [class*="OrderConfirm_order__"]',
    headerContainer: '[class*="Header_container"]',
    tariffRequirementsButton: '[class*="OrderForm_form__"] [class*="RequirementsButton_wrapper__"]',
    costCenterButton: '[class*="CostCenterButton_costCenterButtonWrapper__"]',
    costCenterButtonSubtext: '[class*="CostCenterButton_costCenterButtonWrapper__"] [class^="CostCenterButton_comment-costcenter__"]',

    /**
     * На главном экране нажимает на поле ввода
     * Ожидает появления модалки выбора маршрута
     *
     * @param {Object} bro
     * @param {String} inputFormPos
     * @param {Number} inputSuggestPos
     * @returns {Promise}
     */
    async clickAddressAndWaitSuggest(bro, inputFormPos, inputSuggestPos) {
        await bro.click(inputFormPos);
        await bro.waitForVisible(addressSuggest.container);

        const addressSuggestInput = await addressSuggest.searchInput(bro, inputSuggestPos);
        await bro.waitForVisible(addressSuggestInput.inputFocused, 5000);
    },

    /**
     * На главном экране нажимает на поле ввода
     * В модалке выбора маршрута нажимает на кнопку очищения поля ввода
     *
     * @param {Object} bro
     * @param {Number} inputFormPos
     * @param {Number} inputSuggestPos
     * @returns {Promise}
     */
    async clickAddressAndClickSuggestClearButton(bro, inputFormPos, inputSuggestPos) {
        await this.clickAddressAndWaitSuggest(bro, inputFormPos, inputSuggestPos);
        await addressSuggest.clickClearButton(bro);
    },

    /**
     * Открывает турбоапп такси
     *
     * @param {Object} bro
     * @param {Object} query
     * @returns {Promise}
     */
    async open(bro, query = {}) {
        const baseUrl = bro.options.desiredCapabilities.baseUrl;
        const addtionalUrl = baseUrl ? baseUrl : '/tap';

        await bro.url(`${addtionalUrl}/?${qs.encode({
            comment: 'search-5,wait-10,speed-3000',
            hide_fs_promotions: true,
            hide_promoblocks: true,
            ...query
        })}`);
    },

    /**
     * Открывает турбоапп такси без комментариев
     *
     * @param {Object} bro
     * @param {Object} query
     * @returns {Promise}
     */
    async openWithoutComment(bro, query = {}) {
        const baseUrl = bro.options.desiredCapabilities.baseUrl;
        const addtionalUrl = baseUrl ? baseUrl : '/tap';

        await bro.url(`${addtionalUrl}/?${qs.encode({
            hide_fs_promotions: true,
            hide_promoblocks: true,
            ...query
        })}`);
    },
};
