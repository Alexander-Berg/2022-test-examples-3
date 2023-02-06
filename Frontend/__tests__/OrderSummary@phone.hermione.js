describe('OrderSummary', () => {
    const isDevelopment = process.env.NODE_ENV === 'development';
    const isTrendboxCIPR = process.env.TRENDBOX_GITHUB_EVENT_TYPE === 'pull_request';
    const isSandboxCIPR = process.env.BUILD_BRANCH && process.env.BUILD_BRANCH.includes('pull');

    if (!isDevelopment && !isTrendboxCIPR && !isSandboxCIPR) {
        hermione.skip.in(/.*/, 'тесты на сторибуке сейчас не работают в castle и релизах');
    }
    describe('storybook', () => {
        it('Внешний вид', async function() {
            const { browser } = this;
            await browser.yaOpenEcomStory('ordersummary', 'внешний-вид');
            await browser.assertView('plain', '.story');

            // Выставляем скидку магазина
            await browser.yaChangeKnob('number', 'shopDiscount', 2001);
            await browser.yaChangeKnob('number', 'oldValue', 5000);
            // Немного ждём, когда гермиона увидит новый размер элемента.
            await browser.pause(300);
            await browser.assertView('with-shop-discount', '.story');

            // Выставляем скидку по промокоду
            await browser.yaChangeKnob('number', 'shopDiscount', 0);
            await browser.yaChangeKnob('number', 'promoCodeDiscount', 2001);
            // Немного ждём, когда гермиона увидит новый размер элемента.
            await browser.pause(300);
            await browser.assertView('with-promo-discount', '.story');

            // Выставляем скидку магазина и по промокоду
            await browser.yaChangeKnob('number', 'shopDiscount', 1000);
            await browser.yaChangeKnob('number', 'promoCodeDiscount', 1001);
            // Немного ждём, когда гермиона увидит новый размер элемента.
            await browser.pause(300);
            await browser.assertView('with-shop-and-promo-discount', '.story');

            // Короткий вид
            await browser.yaChangeKnob('boolean', 'shortView', true);
            // Немного ждём, когда гермиона увидит новый размер элемента.
            await browser.pause(300);
            await browser.assertView('with-shop-and-promo-discount-short', '.story');

            // С доставкой
            await browser.yaChangeKnob('number', 'shopDiscount', 1500);
            await browser.yaChangeKnob('number', 'deliveryPrice', 500);
            // Немного ждём, когда гермиона увидит новый размер элемента.
            await browser.pause(300);
            await browser.assertView('with-delivery', '.story');

            // С бесплатной доставкой
            await browser.yaChangeKnob('number', 'shopDiscount', 1000);
            await browser.yaChangeKnob('number', 'deliveryPrice', 0);
            // Немного ждём, когда гермиона увидит новый размер элемента.
            await browser.pause(300);
            await browser.assertView('with-free-delivery', '.story');
        });
    });
});
