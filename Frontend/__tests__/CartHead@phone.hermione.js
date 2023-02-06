describe('CartHead', function() {
    const isDevelopment = process.env.NODE_ENV === 'development';
    const isTrendboxCIPR = process.env.TRENDBOX_GITHUB_EVENT_TYPE === 'pull_request';
    const isSandboxCIPR = process.env.BUILD_BRANCH && process.env.BUILD_BRANCH.includes('pull');
    if (!isDevelopment && !isTrendboxCIPR && !isSandboxCIPR) {
        hermione.skip.in(/.*/, 'тесты на сторибуке сейчас не работают в castle и релизах');
    }
    describe('storybook', () => {
        hermione.only.notIn('iphone', 'Компонент не влезает в ширину экрана айфона');
        it('Внешний вид с ценой и скидкой', async function() {
            const { browser } = this;

            await browser.yaOpenEcomStory('carthead', 'two-elements');
            await browser.yaIndexify('.ProductItem-Info');
            await browser.yaChangeKnob('number', 'width', 240);
            await browser.assertView('long-price', '.ProductItem-Info[data-index="0"]');
            await browser.assertView('short-price', '.ProductItem-Info[data-index="1"]');
        });
    });
});
