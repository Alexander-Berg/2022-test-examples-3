async function openCartCheckout(browser) {
    await browser.yaOpenEcomSpa({
        service: 'spideradio.github.io',
        url: '/turbo/spideradio.github.io/s/rnd/2lum7hf3',
        query: { product_id: 202 },
    });

    await browser.yaWaitForVisible('.ScreenContent');

    // добавить в корзину
    await browser.yaScrollPage('.Button2_view_action', 0.3);
    await browser.click('.Button2_view_action');
    await browser.waitForVisible('.ProductScreen-Actions-Button_inCart');

    // переход в корзину
    await browser.yaScrollPage('.ProductScreen-Actions-Button_inCart', 0.3);
    await browser.click('.ProductScreen-Actions-Button_inCart');
    await browser.waitForVisible('.EcomScreen_type_cart');

    // открытие формы оплаты
    await browser.waitForVisible('.CartButton');
    await browser.yaScrollPage('.CartButton', 0.3);
    await browser.click('.CartButton');
    await browser.yaWaitForHidden('.NavigationTransition_state_entering');
}

async function assertLastMetrikaGoal(goal, message) {
    const { value: goals } = await this.execute(function() {
        return window.Ya.Metrika.getGoalsFor(45135375);
    });

    assert.deepEqual(goals[goals.length - 1], goal, message);
}

async function visitCart(browser, { name, assertMetrika = false, formAssertView }) {
    // кликаем по "добавить в корзину"
    await browser.yaScrollPage('.Button2_view_action', 0.3);
    await browser.click('.Button2_view_action');
    await browser.waitForVisible('.ProductScreen-Actions-Button_inCart');

    assertMetrika && await assertLastMetrikaGoal.call(
        browser,
        ['add-to-cart', {
            ecom_one_click_buy: 1,
            ecom_product_card: 1,
            ecom_product_card_recommendations: 1,
            product_card: true,
        }],
        'Не выполнилась цель добавления товара в корзину',
    );

    // переходим в корзину
    await browser.yaWaitForHidden('.BottomBar-ItemPopup');
    await browser.click('.ProductScreen-Actions-Button_inCart');
    await browser.waitForVisible('.EcomScreen_type_cart');
    await browser.waitForVisible('.CartButton');

    assertMetrika && await assertLastMetrikaGoal.call(
        browser,
        ['open-cart-from-product', {
            ecom_cart: 1,
        }],
        'Не выполнилась цель открытия корзины из товара',
    );

    // переходим в форму оплаты
    await browser.yaScrollPage('.CartButton', 0.3);
    await browser.click('.CartButton');
    assertMetrika && await assertLastMetrikaGoal.call(
        browser,
        ['open-check-out-form-from-cart', {
            ecom_cart: 1,
        }],
        'Не выполнилась цель открытия формы заказа',
    );

    // скриним форму оплаты
    await browser.yaWaitForVisible('.EcomScreen_type_cart .EcomCartForm');
    await browser.yaWaitForHidden('.NavigationTransition_state_entering');

    await browser.setValue('input[name=name]', name);
    await browser.setValue('input[name=customer_phone]', '+7 800 800 80 80');
    await browser.setValue('input[name=customer_email]', 'call@example.ru');

    // заполняем адрес доставки
    await browser.yaMockFetch({
        urlDataMap: {
            'address/': JSON.stringify({
                address: { id: 'randomId' },
                status: 'success',
            }),
        },
    });
    await browser.setValue('input[name="locality"]', 'москв');
    await browser.yaScrollPage('.Suggest-Item', 0.3);
    await browser.click('.Suggest-Item'); // кликаем вне инпута чтобы сбросить выпадашку
    await browser.setValue('input[name="street"]', 'street');
    await browser.click('.Suggest-Item'); // кликаем вне инпута чтобы сбросить выпадашку
    await browser.setValue('input[name="building"]', '22');

    formAssertView !== false &&
        await browser.assertView('cart-form-page-filled', '.EcomScreen_type_cart .ScreenContent');

    await browser.yaScrollPage('.CartForm-RadioItem:nth-of-type(2)', 0.3);
    await browser.click('.CartForm-RadioItem:nth-of-type(2) label');

    await browser.yaScrollPage('.CartForm-SubmitButton', 0.3);
    await browser.click('.CartForm-SubmitButton');
    assertMetrika && await assertLastMetrikaGoal.call(
        browser,
        ['send-check-out-form-from-cart', {
            ecom_cart: 1,
            payment: 'online',
        }],
        'Не выполнилась цель отправки формы заказа',
    );
}

describe('EcomCartForm', function() {
    describe('Валидация формы', function() {
        it('Корретная форма', async function() {
            const browser = this.browser;

            await openCartCheckout(browser);

            await browser.setValue('input[name=name]', 'Фамилия');
            await browser.setValue('input[name=customer_phone]', '+7 800 800 80 80');
            await browser.setValue('input[name=customer_email]', 'call@example.ru');

            // заполняем адрес доставки
            await browser.yaMockFetch({
                urlDataMap: {
                    'address/': JSON.stringify({
                        address: { id: 'randomId' },
                        status: 'success',
                    }),
                },
            });
            await browser.setValue('input[name="locality"]', 'москв');
            await browser.yaScrollPage('.Suggest-Item', 0.3);
            await browser.click('.Suggest-Item'); // кликаем вне инпута чтобы сбросить выпадашку
            await browser.setValue('input[name="street"]', 'street');
            await browser.click('.Suggest-Item'); // кликаем вне инпута чтобы сбросить выпадашку
            await browser.setValue('input[name="building"]', '22');

            await browser.yaScrollPage('.CartForm-RadioItem:nth-of-type(2)', 0.3);
            await browser.click('.CartForm-RadioItem:nth-of-type(2) label');

            await browser.yaMockImages();
            await browser.assertView('right-cart-form-page-filled', '.EcomScreen_type_cart .ScreenContent');
            await browser.yaScrollPage('.CartForm-SubmitButton', 0.3);
            await browser.click('.CartForm-SubmitButton');

            await browser.yaWaitForVisible('.EcomOrderSuccess');
            await browser.yaMockImages();
            await browser.yaAssertViewportView('success-validation');
        });

        it('Форма с ошибками', async function() {
            const browser = this.browser;

            await openCartCheckout(browser);

            await browser.setValue('input[name=customer_phone]', '+7 800');
            await browser.setValue('input[name=customer_email]', 'c@e');
            await browser.yaScrollPage('#pickup_0-delivery + label', 0.3);
            await browser.click('#pickup_0-delivery + label');

            await browser.yaScrollPage('.CartForm-SubmitButton', 0.3);
            await browser.click('.CartForm-SubmitButton');
            await browser.pause(500);
            await browser.assertView('error-validation', '.EcomCartForm');

            // Проверяем, что после ошибки, заполнив валидными данными форма отправляется
            await Promise.all(['Фамилия', '8008080', 'xample.ru']
                .map((item, index) => browser.setValue(`.CartForm-InputWrap:nth-of-type(${index + 1}) input`, item))
            );
            await browser.yaScrollPage('#courier_0-delivery + label', 0.3);
            await browser.click('#courier_0-delivery + label');
            // заполняем адрес доставки
            await browser.yaMockFetch({
                urlDataMap: {
                    'address/': JSON.stringify({
                        address: { id: 'randomId' },
                        status: 'success',
                    }),
                },
            });
            await browser.setValue('input[name="locality"]', 'москв');
            await browser.yaScrollPage('.Suggest-Item', 0.3);
            await browser.click('.Suggest-Item'); // кликаем вне инпута чтобы сбросить выпадашку
            await browser.setValue('input[name="street"]', 'street');
            await browser.click('.Suggest-Item'); // кликаем вне инпута чтобы сбросить выпадашку
            await browser.setValue('input[name="building"]', '22');

            await browser.yaScrollPage('.CartForm-SubmitButton', 0.3);
            await browser.yaWaitForHidden('.Button_in-progress', 'Кнопка осталась в inProgress');
            await browser.click('.CartForm-SubmitButton');

            await browser.yaWaitForVisible('.EcomOrderSuccess');
        });

        it('Изменение способа доставки', async function() {
            const browser = this.browser;

            await openCartCheckout(browser);

            await browser.setValue('input[name=name]', 'Фамилия');
            await browser.setValue('input[name=customer_phone]', '+7 800 800 80 80');
            await browser.setValue('input[name=customer_email]', 'call@example.ru');

            await browser.yaShouldBeVisible('.ShippingAddressFields', 'Форма для ввода адреса не видна');
            await browser.yaScrollPage('#pickup_0-delivery + label', 0.3);
            await browser.click('#pickup_0-delivery + label');
            await browser.yaShouldNotBeVisible('.ShippingAddressFields', 'Форма для ввода адреса не скрылась');
            await browser.yaScrollPage('.CartButton', 0.5);
            await browser.yaMockImages();
            await browser.assertView('delivery-pickup', '.CartForm-RadioGroup');
            await browser.yaShouldNotBeVisible('[name="shipping_address"]', 'Поле адрес видно на странице');
        });
    });

    describe('Trust', function() {
        it('Trust, успешно', async function() {
            const browser = this.browser;

            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'ymturbo.t-dir.com',
                url: '/turbo/ymturbo.t-dir.com/s/catalog/underwear/underwear-white-freedom/',
            });

            await visitCart(browser, { name: 'name' });
            await browser.yaWaitForVisible(PO.blocks.trustIframe(), 'Не показался фрейм оплаты');
            await browser.assertView('trust-iframe', '.ScreenContent-Inner');
            await browser.yaWaitForVisible('.EcomOrderSuccess');
            await browser.yaAssertViewportView('success-payment');
        });

        it('Trust, неудачно', async function() {
            const browser = this.browser;

            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'ymturbo.t-dir.com',
                url: '/turbo/ymturbo.t-dir.com/s/catalog/underwear/underwear-white-freedom/',
            });

            await visitCart(browser, { name: 'error-pay', formAssertView: false });
            await browser.yaWaitForVisible(PO.blocks.trustIframe(), 'Не показался фрейм оплаты');
            await browser.yaWaitForVisible('.CartMeta');
            await browser.yaAssertViewportView('error-payment');
        });
    });

    describe('Аватарка пользователя в корзине', function() {
        it('не должна быть на странице если пользователь не залогинен', function() {
            return this.browser
                .yaOpenEcomSpa({ service: 'spideradio.github.io', pageType: 'cart' })
                .yaWaitForVisibleWithinViewport(PO.blocks.cartButton())
                .click(PO.blocks.cartButton())
                .yaWaitForVisibleWithinViewport(PO.blocks.ecomCartForm())
                .yaShouldNotBeVisible('.CartFormInput-Input_withAvatar', 'Видна аватарка пользователя');
        });

        it('должна быть на странице если пользователь залогинен', function() {
            return this.browser
                .yaOpenEcomSpa({
                    service: 'spideradio.github.io',
                    pageType: 'cart',
                    query: { patch: 'setBlackboxData' },
                })
                .yaWaitForVisibleWithinViewport(PO.blocks.cartButton())
                .click(PO.blocks.cartButton())
                .yaWaitForVisibleWithinViewport(PO.blocks.ecomCartForm())
                .yaShouldBeVisible('.CartFormInput-Input_withAvatar', 'Аватарка пользователя не видна')
                .assertView('plain', '.CartFormInput-Input_withAvatar');
        });
    });

    it('Выбирать способ доставки по-умолчанию при отсутствии выбранного пользователем способа', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            url: '/turbo/spideradio.github.io/s/rnd/2lum7hf3',
            query: { product_id: 202 },
        });

        await browser.yaWaitForVisible('.ScreenContent');
        await browser.execute(function() {
            localStorage.setItem('turbo-app-ecom--spideradio.github.io', JSON.stringify({
                delivery: 'blabla',
            }));
        });

        // добавить в корзину
        await browser.yaScrollPage('.Button2_view_action', 0.3);
        await browser.click('.Button2_view_action');
        await browser.waitForVisible('.ProductScreen-Actions-Button_inCart');

        // переход в корзину
        await browser.yaScrollPage('.ProductScreen-Actions-Button_inCart', 0.3);
        await browser.click('.ProductScreen-Actions-Button_inCart');
        await browser.waitForVisible('.EcomScreen_type_cart');

        // открытие формы оплаты
        await browser.waitForVisible('.CartButton');
        await browser.yaScrollPage('.CartButton', 0.3);
        await browser.click('.CartButton');
        await browser.yaWaitForHidden('.NavigationTransition_state_entering');

        await browser.yaWaitForVisible(
            '.CartForm-RadioItem:nth-child(1) .CartForm-RadioLabelCheck_active',
            'Выбран не первый способ доставки'
        );

        await browser
            .execute(function() {
                const lsData = localStorage.getItem('turbo-app-ecom--spideradio.github.io');

                return JSON.parse(lsData);
            })
            .then(({ value }) => {
                assert.equal(value.delivery, 'blabla');
            });

        await browser.setValue('input[name=name]', 'Иванов Павел Васильевич');

        await browser.yaWaitUntil('В LS сохранился актуальный способ доставки', async() => {
            const requestInfo = await browser.execute(function() {
                const lsData = localStorage.getItem('turbo-app-ecom--spideradio.github.io');

                return JSON.parse(lsData);
            });

            return requestInfo.value.delivery === 'courier_0';
        });
    });

    describe('В новом дизайне', () => {
        it('Форма с комментарием', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                query: {
                    patch: ['setBlackboxData', 'addCartCommentField'],
                },
            });

            await browser.yaWaitForVisibleWithinViewport(PO.blocks.cartButton());
            await browser.click(PO.blocks.cartButton());

            await browser.yaWaitForVisibleWithinViewport(PO.blocks.ecomCartForm());
            await browser.assertView('cart-form', '.EcomScreen_type_cart .ScreenContent');

            await browser.click('.CartForm-ScreenLine .Link');

            await browser.yaWaitForVisibleWithinViewport(PO.blocks.ecomCartForm());
            await browser.assertView('comment-form', ['.Cover', '.EcomScreen_type_cart .ScreenContent']);
        });

        it('Форма без комментария', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                query: { patch: 'setBlackboxData' },
            });

            await browser.yaWaitForVisibleWithinViewport(PO.blocks.cartButton());
            await browser.click(PO.blocks.cartButton());

            await browser.yaWaitForVisibleWithinViewport(PO.blocks.ecomCartForm());
            await browser.yaMockImages();
            await browser.assertView('cart-form', '.EcomScreen_type_cart .ScreenContent');
        });
    });

    describe('Пользовательские адреса', () => {
        it('Пользователь без адресов', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                query: { patch: 'setRegion' },
                expFlags: { 'turbo-app-cart-city-suggest': 1 },
            });

            await browser.yaWaitForVisibleWithinViewport(PO.blocks.cartButton());
            await browser.click(PO.blocks.cartButton());

            await browser.yaWaitForVisibleWithinViewport(PO.blocks.ecomCartForm());
            await browser.yaScrollPage('.EcomScreen_type_cart .ShippingAddressFields', 0);
            await browser.assertView('user-address-form', '.EcomScreen_type_cart .ShippingAddressFields');
            await browser.click('.CartForm-SubmitButton');
            await browser.yaWaitForVisible('.CartFormInput_invalid');
            await browser.yaWaitUntil('Страни не подскролиллась', async() => {
                const { value } = await browser.execute(function() { return window.scrollY });

                return Number(value) < 10;
            });
            await browser.yaScrollPage('.EcomScreen_type_cart .ShippingAddressFields', 0);
            await browser.assertView('user-address-invalid-form', '.EcomScreen_type_cart .ShippingAddressFields');
        });

        it('Пользователь c адресами', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                query: { patch: 'setUserAddressesData' },
                expFlags: { 'turbo-app-cart-city-suggest': 1 },
            });

            await browser.yaWaitForVisibleWithinViewport(PO.blocks.cartButton());
            await browser.click(PO.blocks.cartButton());

            await browser.yaWaitForVisibleWithinViewport(PO.blocks.ecomCartForm());
            await browser.yaScrollPage('.EcomScreen_type_cart .ShippingAddresses', 0);
            await browser.assertView('user-addresses', '.EcomScreen_type_cart .ShippingAddresses');
        });

        it('Новый адрес', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                query: { patch: 'setUserAddressesData' },
                expFlags: { 'turbo-app-cart-city-suggest': 1 },
            });

            await browser.yaWaitForVisibleWithinViewport(PO.blocks.cartButton());
            await browser.click(PO.blocks.cartButton());

            await browser.yaWaitForVisibleWithinViewport(PO.blocks.ecomCartForm());

            await browser.yaScrollPage('.ShippingAddresses-NewAddress', 0);
            if (this.currentTest.browserId === 'iphone') {
                // Айфон не успевает подскролить страницу до клика и думает, что кнопка не видна.
                await browser.pause(300);
            }
            await browser.click('.ShippingAddresses-NewAddress');
            await browser.yaWaitForVisibleWithinViewport('.AddressForm');
            await browser.assertView('user-addresses-form', '.AddressForm');
            await browser.click('.AddressForm-FormActions .Button');
            await browser.assertView('user-addresses-form-invalid', '.AddressForm');
        });

        it('Редактирование адресов', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                query: { patch: 'setUserAddressesData' },
                expFlags: { 'turbo-app-cart-city-suggest': 1 },
            });

            await browser.yaWaitForVisibleWithinViewport(PO.blocks.cartButton());
            await browser.click(PO.blocks.cartButton());

            await browser.yaWaitForVisibleWithinViewport(PO.blocks.ecomCartForm());

            await browser.getText('.ShippingAddresses .CartForm-RadioItem:nth-child(1) .CartForm-CheckboxTitle')
                .then(text => assert.include(text, 'Москва'));

            // Редактируем паспортный адрес
            await browser.yaScrollPage('.ShippingAddresses .CartForm-RadioItem:nth-child(4) .ShippingAddresses-ChangeLink', 0);
            await browser.click('.ShippingAddresses .CartForm-RadioItem:nth-child(4) .ShippingAddresses-ChangeLink');
            await browser.yaWaitForVisibleWithinViewport('.AddressForm');
            await browser.yaShouldNotBeVisible('.AddressForm-ButtonDelete', 'Кнопку "Удалить" видно для паспортного адреса');

            await browser.yaMockFetch({
                addresses: { id: 'randomId' },
                status: 'success',
            });
            await browser.setValue('input[name=building]', '911');
            await browser.click('.AddressForm-FormActions button');
            // Ждем анимацию возврата
            await browser.yaWaitForVisible(PO.blocks.ecomCartForm(), 3000);

            await browser.yaScrollPage('.ShippingAddresses .CartForm-RadioItem:nth-child(1) .ShippingAddresses-ChangeLink', 0.3);

            await browser.elements('.ShippingAddresses .CartForm-RadioItem')
                .then(elements => { assert.equal(elements.value.length, 5) });
        });

        it('Город не из саджеста', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                query: { patch: 'setUserAddressesData' },
                expFlags: { 'turbo-app-cart-city-suggest': 1 },
            });

            await browser.yaWaitForVisibleWithinViewport(PO.blocks.cartButton());
            await browser.click(PO.blocks.cartButton());

            await browser.yaWaitForVisibleWithinViewport(PO.blocks.ecomCartForm());

            await browser.yaScrollPage('.ShippingAddresses-NewAddress', 0);
            if (this.currentTest.browserId === 'iphone') {
                // Айфон не успевает подскролить страницу до клика и думает, что кнопка не видна.
                await browser.pause(300);
            }
            await browser.click('.ShippingAddresses-NewAddress');
            await browser.yaWaitForVisibleWithinViewport('.AddressForm');

            // Приходится ждать некоторое время, так как иначе получаем плавающую ошибку
            //  "Element is not currently interactable and may not be manipulated"
            //  Кажется, помогло бы waitForClickable из следующих версия webdriverio
            await browser.pause(200);

            // Несмотря на то, что город существует, форма не должна проходить валидацию,
            //  так как он не был выбран из саджеста
            await browser.setValue('input[name=locality]', 'Нижний Новгород');

            // Чтобы скрылся саджест
            await browser.click('.AddressForm-Meta');
            await browser.yaWaitForHidden('.Suggest-Popup');

            // Выставляем в оставшиеся поля корректные значения
            // Улицу можно не выбирать из саджеста, допустимо указать произвольную
            await browser.setValue('input[name=street]', 'площадь Минина и Пожарского');

            // Чтобы скрылся саджест
            await browser.click('.AddressForm-Meta');
            await browser.yaWaitForHidden('.Suggest-Popup');

            await browser.setValue('input[name=building]', '5');

            let isLocalityErrorExist = await browser.isExisting('.ShippingAddressFields-CitySuggest .GeoSuggest-Error');
            assert.isFalse(isLocalityErrorExist, 'Сообщение об ошибке появилось до отправки формы');

            await browser.click('.AddressForm-FormActions .Button');

            isLocalityErrorExist = await browser.isExisting('.ShippingAddressFields-CitySuggest .GeoSuggest-Error');
            assert.isTrue(isLocalityErrorExist, 'Сообщение об ошибке не появилось после отправки формы');

            const error = await browser.getText('.ShippingAddressFields-CitySuggest .GeoSuggest-Error');
            assert.deepEqual(error, 'Выберите населённый пункт из списка');
        });
    });

    it('Успешный экран для залогиненного пользователя', async function() {
        const browser = this.browser;

        await browser.onRecord(() => browser.auth('tap-user'));
        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            url: '/turbo/spideradio.github.io/s/rnd/2lum7hf3',
            query: { product_id: 202 },
        });

        await visitCart(browser, { name: 'Name', formAssertView: false });

        await browser.yaWaitForVisible('.EcomOrderSuccess');
        await browser.yaAssertViewportView('success');
    });

    it('Оплата онлайн блокируется при корзине больше 150 000 р', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: 'ymturbo.t-dir.com',
            pageType: 'cart',
        });

        await browser.yaWaitForVisible('.CartHead');
        await browser.setValue('#cardHeadSelectInput', '82');
        await browser.click('.CartButton');

        await browser.yaWaitForVisible('.CartForm-SubmitButton');
        await browser.yaScrollPage('.CartForm-SubmitButton', 0);

        await browser.assertView('active', '.CartForm-RadioItem_type_payment');

        await browser.click('.CartForm-RadioItem_type_delivery:nth-child(3)');
        await browser.yaWaitForVisible('.CartForm-RadioItem_disabled');
        await browser.assertView('disabled', '.CartForm-RadioItem_type_payment');

        await browser.click('.CartForm-RadioItem_type_delivery:nth-child(1)');
        await browser.yaWaitForHidden('.CartForm-RadioItem_disabled');
        await browser.assertView('available', '.CartForm-RadioItem_type_payment');
    });

    it('Пользовательское соглашение с магазином', async function() {
        const browser = this.browser;
        await browser.yaOpenEcomSpa({
            url: '/turbo/farkop.ru/s/catalog/tyagovo_stsepnye_ustroystva/farkopy/farkop_pt_group_dlya_renault_duster_2010/',
            query: {
                product_id: 99329,
            },
        });
        // добавить в корзину
        await browser.yaScrollPage('.ProductScreen-Actions-BuyButtonCover', 0.3);
        await browser.click('.ProductScreen-Actions-BuyButtonCover .Button2_view_action');
        await browser.waitForVisible('.ProductScreen-Actions-Button_inCart');

        // переход в корзину
        await browser.yaScrollPage('.ProductScreen-Actions-Button_inCart', 0.3);
        await browser.click('.ProductScreen-Actions-Button_inCart');
        await browser.waitForVisible('.EcomScreen_type_cart');

        // открытие формы оплаты
        await browser.waitForVisible('.CartButton');
        await browser.yaScrollPage('.CartButton', 0.3);
        await browser.click('.CartButton');
        await browser.yaWaitForHidden('.NavigationTransition_state_entering');

        await browser.yaScrollPage('#online-payment_method + label', 0.3);
        await browser.click('#online-payment_method + label');
        await browser.assertView('meta', ['.EcomCartForm-SellerInfo', '.EcomCartForm-PersonalData']);
        await browser.yaScrollPage('#cash-payment_method + label', 0.3);
        await browser.click('#cash-payment_method + label');
        await browser.yaShouldNotBeVisible('.EcomCartForm-SellerInfo', 'Данные получателя не исчезли после выбора оплаты наличными');
        await browser.yaShouldBeVisible('.EcomCartForm-PersonalData', 'Пользовательское соглашение исчезло после выбора оплаты наличными');
    });
});
