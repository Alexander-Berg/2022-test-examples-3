describe('Ecom-tap', function() {
    describe('Отсутствие информации о заказе в форме', function() {
        it('Внешний вид', async function() {
            const browser = this.browser;

            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/',
                    'turbo-app-cart-form-summary-disabled': 1,
                },
                query: {
                    patch: 'setRegion',
                },
            });

            await browser.click('.CartButton');

            await browser.yaWaitForVisible('.EcomScreen_type_cart .EcomCartForm');
            await browser.yaWaitForHidden('.NavigationTransition_state_entering');
            await browser.yaScrollPage('#pickup_0-delivery + label', 0.3);
            await browser.click('#pickup_0-delivery + label');
            await browser.yaScrollPageToBottom();

            await browser.assertView('plain', ['.CartForm-RadioGroup', '.CartForm-SubmitButton']);
        });
    });
});
