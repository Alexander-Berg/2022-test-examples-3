describe('ScreenContentError', function() {
    const isDevelopment = process.env.NODE_ENV === 'development';
    const isTrendboxCIPR = process.env.TRENDBOX_GITHUB_EVENT_TYPE === 'pull_request';
    const isSandboxCIPR = process.env.BUILD_BRANCH && process.env.BUILD_BRANCH.indexOf('pull') > -1;
    if (!isDevelopment && !isTrendboxCIPR && !isSandboxCIPR) {
        hermione.skip.in(/.*/, 'тесты на сторибуке сейчас не работают в castle и релизах');
    }
    describe('storybook', () => {
        it('Внешний вид экрана ошибки', async function() {
            const { browser } = this;

            await browser.yaOpenEcomStory('screencontenterror', 'plain');
            await browser.yaMockImages();
            await browser.assertView('plain', '.ScreenContentError');
        });
    });
});
