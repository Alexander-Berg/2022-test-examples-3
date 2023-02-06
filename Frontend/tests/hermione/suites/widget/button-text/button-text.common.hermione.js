const { build, getUrl, widgetParamsUpdaterFactory } = require('../utils');

specs({
    feature: 'Текст кнопки',
}, function () {
    it('Текст кнопки отображается и обрезается по длине', async function () {
        await this.browser.url(getUrl(build.widget));
        await this.browser.waitForVisible('.ya-chat-widget');

        // Short button text
        this.browser.execute(...widgetParamsUpdaterFactory());
        await this.browser.waitForVisible('.ya-chat-widget');
        await this.browser.assertView('button-text-short', '.hermione');

        // Long button text
        this.browser.execute(...widgetParamsUpdaterFactory({ buttonText: 'Невероятно длинный текст кнопки' }));
        await this.browser.waitForVisible('.ya-chat-widget');
        await this.browser.assertView('button-text-long', '.hermione');
    });
});
