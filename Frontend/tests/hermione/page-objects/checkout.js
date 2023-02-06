/**
 * Селекторы фрейма чекаута
 */

module.exports = {
    /**
     * Корневой элемент страницы
     */
    root: '#root',

    /**
     * Селекторы основного экрана
     */
    mainScreen: '[class*="MainScreen_container"]',
    mainScreenHeaderButtonClose: '[class*="Header_icon-close"]',
    mainScreenStickyHeader: '[class*="Header_sticky"]',
    mainScreenServiceAgreementLink: '[class*="ServiceAgreement_link"]',

    /**
     * Селекторы блока с кнопкой "Оформить заказ"
     */
    orderSummary: '[class*="OrderSummary_container"]',
    orderSummaryButton: '[class*="OrderSummaryButton_button"]',
    orderSummaryButtonDisable: '[class*="OrderSummaryButton_disabled"]',

    /**
     * Селекторы блока "Комментарий к заказу"
     */
    orderCommentInput: '[class*=Extra_container]',
    orderCommentInputEmpty: '[class*=Extra_container] [class*="Form_empty"]',
    orderCommentInputButtonClear: '[class*=Extra_container] [class*="Clear_visible"]',

    /**
     * Селектор лоадера чекаута
     */
    pageLoader: '[class*="PageLoader_container"]',

    /**
     * Селектор блока с товарами на главном экране
     */
    primaryOrderProducts: '[class*="Products_container"]',
    productAllButton: '[class*="Products_all"]',
    productsCarousel: '[class*="Products_body"]',

    /**
     * Селекторы экрана "Ваш заказ"
     */
    productList: '[class*="ProductsScreenProductList_grid"]',
    productSummary: '[class*="ProductsScreenSummary_grid"]',

    /**
     * Селекторы шапки доп.экранов
     */
    stickyHeader: '[class*="ScreenHeader_content"]',
    headerBackButton: '[class*="ScreenHeader_button"]',

    /**
     * Селектор кнопки, расположенной внизу экрана
     */
    screenButton: '[class*="ScreenButton_button"]',
    stickyScreenButton: '[class*="ScreenButton_sticky"]',

    /**
     * Селекторы блока "Доставка"
     */
    deliveryAddressProfile: '[class*="DeliveryAddress_profile-addresses"]',
    deliveryAddressForm: '[class*="AddressForm_container"]',
    deliveryAddressSuggestLoader: '[class*="Spin2_progress"]',
    deliveryAddressSuggestTitle: '[class*="AddressSuggest_title"]',
    deliveryCityButton: '[class*="DeliveryCity_name"] [class*="CompactBlock_title"]',
    deliveryCityError: '[class*="DeliveryCity_name"] [class*="CompactBlock_error"]',
    deliveryPickupOptionControl: '[class*="PickupOptionControl_container"]',
    deliveryPrimaryOrder: '[class*="PrimaryOrder_delivery"]',
    deliveryPrimaryOrderTime: '[class*="PrimaryOrder_delivery-time"]',
    deliveryTimeDateInfo: '[class*="MainScreenDeliveryTime_date-info"] [class*="MainScreenDeliveryTime_date"]',
    deliveryMethodsFirstOption: '[class*="DeliveryMethods_container"] .Radiobox-Radio:first-child',
    deliveryMethodsRadiobox: '[class*="DeliveryMethods_container"] .Radiobox-Radio',
    deliveryProfileAddresses: '[class*="DeliveryAddress_profile-addresses"]',
    deliveryProfileAddressActive: '[class*="DeliveryAddress_profile-addresses"] .Radiobox-Radio_checked',
    deliveryProfileAddressesError: '[class*="DeliveryAddress_profile-addresses"] [class*="Form_error"]',
    deliveryProfileAddressesTitle: '[class*="DeliveryAddress_profile-addresses"] [class*="ProfileAddresses_title"]',
    deliveryProfileButtonNewAddresses: '[class*="DeliveryAddress_profile-addresses"] [class*="NewAddress_link"]',
    deliveryProfileButtonAddressesEdit: '[class*="ProfileAddresses_edit"]',
    deliveryProfileAddressesFirstOption: '[class*="DeliveryAddress_profile-addresses"] .Radiobox-Radio:first-child',
    PickupOptionLabel: '[class*="PickupOption_pickup"] [class*="PickupOption_label"]',

    /**
     * Селекторы схлопнутого блока "Доставка"
     */
    compactDeliveryButton: '[class*="CompactBlock_container"][href$="/delivery-methods"]',

    /**
     * Селекторы экрана "Доставка"
     */
    deliveryScreen: '[class*="DeliveryMethodsScreenComponent_wrapper"]',

    /**
     * Селекторы экрана со списокм постаматов
     */
    pickupScreen: '[class*="PickupOptionsScreenComponent_container__3p6uJ"]',

    /**
     * Селекторы экрана "Новый адрес"/"Адрес получателя"
     */
    addressScreen: '[class*="UpdateAddressScreen_wrapper"]',

    /**
     * Селекторы экрана "Выбор города"
     */
    screenCityHeaderContainer: '[class*="ScreenHeader_container"]',
    screenCitySearchInput: '[class*="CityScreenComponent_container"] input',
    screenCitySearchInputButtonClear: '[class*="CityScreenComponent_container"] .Textinput-Clear_visible',
    screenCitySearchResultsContainer: '[class*="SearchResults_container"]',
    screenCitySearchResultsEmpty: '[class*="SearchResults_empty"]',
    screenCitySearchResultsItem: '[class*="SearchResults_item-title"]',

    /**
     * Селекторы экрана "Выбор даты и времени доставка"
     */
    screenDateOptionsDate: '[class*="DeliveryTimeScreenDateOptions_date"]',
    screenDateOptionsDay: '[class*="DeliveryTimeScreenDateOptions_day"]',
    screenDateOptionsDateContent: '[class*="DeliveryTimeScreenDateOptions_content"]',
    screenDateOptionsDateSelected: '[class*="DeliveryTimeScreenDateOptions_date_selected"]',
    screenTimeOptionsContainer: '[class*="DeliveryTimeScreenTimeOptions_container"]',
    screenTimeOptionsFirstRadiobox: '[class*="DeliveryTimeScreenTimeOptions_container"] .Radiobox-Radio:first-child',
    screenTimeOptionsCheckedRadiobox: '[class*="DeliveryTimeScreenTimeOptions_container"] .Radiobox-Radio_checked',

    /**
     * Селекторы блока "Получатель"
     */
    contactsBlock: '[class*="Contacts_block"]',
    contactsBlockFormError: '[class*="Contacts_block"] [class*="Form_error"]',

    /**
     * Селекторы схлопнутого блока "Получатель"
     */
    compactContactsButton: '[class*="CompactBlock_container"][href$="/contacts"]',
    compactContactsSubtitle: '[class*="CompactBlock_container"][href$="/contacts"] [class*="CompactBlock_subtitle"]',

    /**
     * Селекторы экрана "Получатель"
     */
    contactsScreen: '[class*="ContactsScreenComponent_wrapper"]',
    contactsScreenFormError: '[class*="ContactsScreenComponent_wrapper"] [class*="Form_error"]',
    contactsScreenInputButtonClear: '[class*="ContactsScreenComponent_wrapper"] .Textinput_focused .Textinput-Clear_visible',

    /**
     * Селекторы схлопнутого блока "Способ оплаты"
     */
    compactPaymentMethodsButton: '[class*="CompactBlock_container"][href$="/payment-methods"]',

    /**
     * Селекторы экрана "Способ оплаты"
     */
    paymentMethodsScreen: '[class*="PaymentMethodsScreenComponent_payment-methods"]',

    /**
     * Возвращает селектор кнопки "Назад" для переданого экрана
     * @param {String} screenSelector
     * @returns {String}
     */
    getHeaderBackButtonByScreen(screenSelector) {
        const buttonSelector = '[class*="ScreenHeader_button"]';

        return screenSelector ? `${screenSelector} ${buttonSelector}` : buttonSelector;
    },

    /**
     * Осуществляет поиск поля ввода по его названию
     *
     * @param {Object} bro
     * @param {String} inputName
     * @returns {Promise}
     */
    async searchInputByName(bro, inputName) {
        let input;
        let textarea;

        try {
            input = await bro.searchElementByPropertiesValue(bro, 'input', 'name', inputName);
        } catch {
            // do nothing
        }

        try {
            // Для того, чтобы корректно работало заполнение textarea необходимо выбирать родительский элемент
            const textareaSelector = await bro.searchElementByPropertiesValue(bro, 'textarea', 'name', inputName);
            const textareaWrapSelectors = await bro.findElementsWithChild('.Textarea-Wrap', textareaSelector);

            textarea = textareaWrapSelectors[0];
        } catch {
            // do nothing
        }

        return input || textarea;
    },

    /**
     * Осуществляет поиск города в результатах поиска
     *
     * @param {Object} bro
     * @param {String} cityName
     * @returns {Promise}
     */
    async searchCityInSearchResultByName(bro, cityName) {
        return await bro.searchElementByPropertiesValue(bro, this.screenCitySearchResultsItem, 'textContent', cityName);
    },

    /**
     * Осуществляет поиск адреса в результатах поиска
     *
     * @param {Object} bro
     * @param {String} address
     * @returns {Promise}
     */
    async searchAddressInSuggestByName(bro, address) {
        return await bro.searchElementByPropertiesValue(bro, this.deliveryAddressSuggestTitle, 'textContent', address);
    },

    /**
     * Осуществляет поиск адреса в списках адресов доставки
     *
     * @param {Object} bro
     * @param {String} address
     * @returns {Promise}
     */
    async searchDeliveryAddress(bro, address) {
        return await bro.searchElementByPropertiesValue(bro, this.deliveryProfileAddressesTitle, 'textContent', address);
    },

    /**
     * Осуществляет поиск способа доставки по его названию
     *
     * @param {Object} bro
     * @param {String} nameDeliveryMethod
     * @returns {Promise}
     */
    async searchDeliveryMethod(bro, nameDeliveryMethod) {
        return await bro.searchElementByPropertiesValue(bro, this.deliveryMethodsRadiobox, 'textContent', nameDeliveryMethod);
    },

    /**
     * Осуществляет поиск пункта самовывоза
     *
     * @param {Object} bro
     * @param {String} namePickupOptionLabel
     * @returns {Promise}
     */
    async searchPickupOptionLabel(bro, namePickupOptionLabel) {
        return await bro.searchElementByPropertiesValue(bro, this.PickupOptionLabel, 'textContent', namePickupOptionLabel);
    },

    /**
     * На экране "Выбор даты и времени" осуществляет поиск кнопки даты доставки по дате
     *
     * @param {Object} bro
     * @param {String} day
     * @returns {Promise}
     */
    async searchButtonDay(bro, day) {
        return await bro.searchElementByPropertiesValue(bro, this.screenDateOptionsDay, 'textContent', day);
    },

    /**
     * Осуществляет поиск поля ввода
     * И кликает по найденному полю
     *
     * @param {Object} bro
     * @param {String} inputName
     * @returns {Promise}
     */
    async searchAndClickInput(bro, inputName) {
        const input = await this.searchInputByName(bro, inputName);
        await bro.click(input);
    },

    /**
     * Осуществляет поиск поля ввода
     * Заполняет найденное поле
     *
     * @param {Object} bro
     * @param {String} inputName
     * @param {String} text
     * @returns {Promise}
     */
    async searchAndFillInput(bro, inputName, text) {
        await this.searchAndClickInput(bro, inputName);
        await bro.keys(text);
    },

    /**
     * Осуществляет поиск поля ввода
     * Очищает поле ввода и далее заполняет его
     *
     * @param {Object} bro
     * @param {String} inputName
     * @param {String} text
     * @returns {Promise}
     */
    async searchAndClearFillInput(bro, inputName, text) {
        await this.searchAndClickInput(bro, inputName);
        await bro.deleteAndSendKeys(bro, text);
    },

    /**
     * Осуществляет поиск поля ввода
     * Кликает на найденное поле
     * Очищает поле ввода нажатием на кнопку "Х" по переданному селектору
     *
     * @param {Object} bro
     * @param {String} inputName
     * @param {String} selectorButtonClear
     * @returns {Promise}
     */
    async searchAndClearInput(bro, inputName, selectorButtonClear) {
        await this.searchAndClickInput(bro, inputName);
        await bro.waitForVisible(selectorButtonClear, 5000);

        await bro.click(selectorButtonClear);
        await bro.waitForVisible(selectorButtonClear, 5000, true);
    },

    /**
     * Нажимает на кнопку "Все"
     * Ожидает появления списка товаров
     *
     * @param {Object} bro
     * @returns {Promise}
     */
    async clickProductAllButton(bro) {
        await bro.click(this.productAllButton);
        await bro.waitForVisible(this.productAllButton, 5000, true);
        await bro.waitForVisible(this.productList, 10000);
    },

    /**
     * Осуществляет переход к экрану "Выбор города"
     *
     * @param {Object} bro
     * @returns {Promise}
     */
    async openCityScreen(bro) {
        await bro.click(this.deliveryCityButton);
        await bro.waitForVisible(this.deliveryCityButton, 5000, true);
        await bro.waitForVisible(this.screenCitySearchInput, 5000);
        await bro.waitForVisible(this.screenCitySearchResultsEmpty, 5000, true);
        await bro.waitForVisible(this.screenCitySearchResultsItem, 5000);
    },

    /**
     * На экране "Выбор города" нажимает на город из результатов поиска
     *
     * @param {Object} bro
     * @param {String} cityName
     * @returns {Promise}
     */
    async clickCityInSearchResult(bro, cityName) {
        const city = await this.searchCityInSearchResultByName(bro, cityName);
        await bro.click(city);
        await bro.waitForVisible(this.pageLoader, 5000);
    },

    /**
     * Осуществляет переход к экрану "Выбор города"
     * В дефолтном списке выбирает гороод по названию
     * И кликает по нему
     *
     * @param {Object} bro
     * @param {String} cityName
     * @returns {Promise}
     */
    async selectCityFromDefaultList(bro, cityName) {
        await this.openCityScreen(bro);
        await this.clickCityInSearchResult(bro, cityName);
    },

    /**
     * Нажимает на схлопнутый блок "Доставка"
     * И ожидает загрузки экрана "Доставка"
     *
     * @param {Object} bro
     * @returns {Promise}
     */
    async clickCompactDeliveryButton(bro) {
        await bro.click(this.compactDeliveryButton);
        await bro.waitForVisible(this.deliveryScreen, 5000);
        await bro.waitForVisible(this.mainScreen, 1000, true);
    },

    /**
     * Нажимает на схлопнутый блок "Способ оплаты"
     * И ожидает загрузки экрана "Способ оплаты"
     *
     * @param {Object} bro
     * @returns {Promise}
     */
    async clickCompactPaymentMethodsButton(bro) {
        await bro.click(this.compactPaymentMethodsButton);
        await bro.waitForVisible(this.paymentMethodsScreen, 5000);
        await bro.waitForVisible(this.mainScreen, 1000, true);
    },

    /**
     * В форме указания адреса доставки кликает на адрес из результатов поиска
     *
     * @param {Object} bro
     * @param {String} addressName
     * @returns {Promise}
     */
    async clickAddressInSearchResult(bro, addressName) {
        const address = await this.searchAddressInSuggestByName(bro, addressName);
        await bro.click(address);
    },

    /**
     * Осуществляет поиск адреса доставки
     * И нажимает на найденный адрес
     *
     * @param {Object} bro
     * @param {String} addressName
     * @returns {Promise}
     */
    async searchAndClickDeliveryAddress(bro, addressName) {
        const address = await this.searchDeliveryAddress(bro, addressName);
        await bro.click(address);
        await bro.waitForVisible(this.pageLoader, 5000);
    },

    /**
     * Осуществляет поиск способа доставки
     * И нажимает на найденный способ
     *
     * @param {Object} bro
     * @param {String} nameDeliveryMethod
     * @returns {Promise}
     */
    async searchAndClickDeliveryMethod(bro, nameDeliveryMethod) {
        const deliveryMethod = await this.searchDeliveryMethod(bro, nameDeliveryMethod);
        await bro.click(deliveryMethod);
        await bro.waitForVisible(this.pageLoader, 5000);
    },

    /**
     * Осуществляет поиск адреса самовывоза
     * И нажимает на найденный адрес
     *
     * @param {Object} bro
     * @param {String} namePickupOptionLabel
     * @returns {Promise}
     */
    async searchAndClickPickupOptionLabel(bro, namePickupOptionLabel) {
        const pickupOptionLabel = await this.searchPickupOptionLabel(bro, namePickupOptionLabel);
        await bro.click(pickupOptionLabel);
        await bro.waitForVisible(this.pageLoader, 5000);
    },

    /**
     * Нажимает на кнопку редактирования адреса
     * И ожидает загрузки экрана "Адрес пользователя"
     *
     * @param {Object} bro
     * @returns {Promise}
     */
    async clickAddressesEditButton(bro) {
        await bro.click(this.deliveryProfileButtonAddressesEdit);
        await bro.waitForVisible(this.addressScreen, 5000);
        await bro.waitForVisible(this.deliveryScreen, 1000, true);
    },

    /**
     * Осуществляет поиск поля ввода по его типу
     * Заполняет найденное поле
     *
     * @param {Object} bro
     * @param {String} text
     * @returns {Promise}
     */
    async fillCitySearchInput(bro, text) {
        await bro.click(this.screenCitySearchInput);
        await bro.keys(text);
    },

    /**
     * Ожидает появления блока перехода к экрану "Дата и время доставки"
     * Осуществляет переход к экрану "Дата и время доставки"
     *
     * @param {Object} bro
     * @returns {Promise}
     */
    async openDeliveryTimeScreen(bro) {
        await bro.waitForVisible(this.deliveryPrimaryOrderTime, 5000);

        await bro.click(this.deliveryPrimaryOrderTime);
        await bro.waitForVisible(this.deliveryPrimaryOrderTime, 5000, true);
        await bro.waitForVisible(this.screenDateOptionsDate, 5000);
    },

    /**
     * Осуществляет поиск переданной даты
     * Нажимает на контрол найденной даты
     *
     * @param {Object} bro
     * @param {String} day
     * @returns {Promise}
     */
    async searchAndClickDeliveryDay(bro, day) {
        const selectorButtonDay = await this.searchButtonDay(bro, day);
        await bro.click(selectorButtonDay);
        await bro.waitForVisible(this.screenDateOptionsDateSelected, 5000);
        await bro.waitForVisible(this.screenTimeOptionsCheckedRadiobox, 5000, true);
    },

    /**
     * Нажимает на схлопнутый блок "Получатель"
     * Ожидает появления экрана "Получатель"
     *
     * @param {Object} bro
     * @returns {Promise}
     */
    async clickCompactContactsButton(bro) {
        await bro.click(this.compactContactsButton);
        await bro.waitForVisible(this.contactsScreen, 5000);
        await bro.waitForVisible(this.mainScreen, 1000, true);
    },

    /**
     * Нажимает на кнопку "Оформить заказ"
     * И ожидает появления лоадера
     *
     * @param {Object} bro
     * @returns {Promise}
     */
    async clickOrderSummaryButton(bro) {
        await bro.click(this.orderSummaryButton);
        await bro.waitForVisible(this.pageLoader, 5000);
    },

    /**
     * Открывает стартовую страницу эмуляции магазина и фрейм чекаута
     *
     * @param {Object} bro
     * @param {Object} checkoutDetails – стартовые настройки чекаута
     * @param {Object} checkoutOptions – опции для открытия чекаута, которые можно передать в нативный конструктор
     * @returns {Promise}
     */
    async open(bro, checkoutDetails, checkoutOptions = {}) {
        await bro.url('/?hermione=1&reset_session_storage=1');

        await bro.waitUntil(async function() {
            const result = await bro.execute(() => Boolean(window.__respondToEvent));
            return result.value;
        }, 10000, 'Страница чекаута не загрузилась за 10 секунд');

        await bro.handleCheckoutEvent('getCheckoutDetails', checkoutDetails);
        await bro.handleCheckoutEvent('getCheckoutOptions', {
            shopName: 'Беру',
            shopIcon: 'https://yastatic.net/market-export/_/i/favicon/pokupki/196.png',
            baseUrl: '/turbo',
            ...checkoutOptions,
        });
    },
};
