/**
 * Дожидается изменения куки с соответствующим количеством товаров и
 * возвращает информацию об этой куке.
 * @param {number} length - ожидаемое количество товаров в куке; 0 означает ожидание отсутствия куки
 * @param {string} message - сообщение об ошибке в случае, когда ожидаемая кука не появилась
 */
async function waitCookie(length, message) {
    const bro = this.browser;

    await bro.yaWaitUntil(message, async() => {
        const cookieData = await bro.getCookie('yandex_turbo_cart_offers');

        if (length && cookieData) {
            return JSON.parse(cookieData.value).length === length;
        }

        return length === 0 && cookieData === null;
    }, 3000, 250);

    return bro.getCookie('yandex_turbo_cart_offers');
}

specs({
    feature: 'EcomCookie',
}, () => {
    hermione.only.in('chrome-phone', 'ускоряем браузеронезависимые тесты');
    describe('yandex_turbo_cart_offers', () => {
        hermione.only.notIn('safari13');
        it('Должен устанавливать куку после загрузки информации о корзине', async function() {
            const bro = this.browser;

            await bro.url('/turbo?text=ymturbo.t-dir.com/yandexturbocatalog/&exp_flags=turboforms_endpoint=/&srcrwr=SEARCH_SAAS_LISTINGS:SAAS_ANSWERS');
            await bro.yaWaitForVisible(PO.pageJsInited());
            const cookieData = await waitCookie.call(this, 1, 'кука не установилась');
            assert.deepEqual(cookieData.value, '["3242424234"]');
        });

        hermione.only.notIn('safari13');
        it('Должен устанавливать куку после добавления товара в корзину', async function() {
            const bro = this.browser;

            await bro.url('/turbo?text=ymturbo.t-dir.com/yandexturbocatalog/&exp_flags=turboforms_endpoint=/&srcrwr=SEARCH_SAAS_LISTINGS:SAAS_ANSWERS');
            await bro.yaWaitForVisible(PO.pageJsInited());
            await bro.click(PO.productAddToCart());
            const cookieData = await waitCookie.call(this, 2, 'второй товар добавился в куку');
            assert.deepEqual(cookieData.value, '["3242424234","117"]');
        });

        hermione.only.notIn('safari13');
        it('Должен удалять куку после удаления товара из корзины', async function() {
            const bro = this.browser;

            await bro.url('/turbo?text=ymturbo.t-dir.com/yandexturbocart/&exp_flags=turboforms_endpoint=/multiple/');
            await bro.yaWaitForVisible(PO.pageJsInited());
            await waitCookie.call(this, 3, 'товары не добавились в куку');

            // Удаление первого товара.
            await bro.click(PO.cartItem.remove());
            await waitCookie.call(this, 2, 'товар не удалился из куки');

            // Удаление второго и третьего товаров.
            await bro.click(PO.cartItem.remove());
            await bro.click(PO.cartItem.remove());
            await waitCookie.call(this, 0, 'кука не удалилась');
        });

        hermione.only.notIn('safari13');
        it('Должен удалять куку после оформления заказа', async function() {
            const bro = this.browser;

            await bro.url('/turbo?text=ymturbo.t-dir.com/yandexturbocart/&exp_flags=turboforms_endpoint=/');
            await bro.yaWaitForVisible(PO.pageJsInited());
            await bro.yaWaitForVisible(PO.blocks.turboButtonThemeBlue(), 'Нет кнопки "Оформить заказ"');
            await bro.click(PO.blocks.turboButtonThemeBlue());
            await bro.yaWaitForVisible(PO.orderForm());
            await bro.yaIndexify(PO.blocks.radioGroup.radioItem());
            await bro.yaIndexify(PO.blocks.inputText());
            await bro.setValue(PO.orderForm.nameField.control(), 'test');
            await bro.setValue(PO.orderForm.phoneField.control(), '88001234567');
            await bro.setValue(PO.orderForm.emailField.control(), 'test@test.ru');
            await bro.setValue(PO.orderForm.address(), 'Адрес');
            await bro.click('.turbo-form2 .radio-item[data-index="6"]');
            await bro.click(PO.orderForm.submit());
            await bro.yaWaitForVisible(
                PO.blocks.turboStatusScreenOrderSuccess(), 3000,
                'Не показался статус-скрин результата оформления заказа',
            );
            await waitCookie.call(this, 0, 'кука не удалилась');
        });
    });
});
