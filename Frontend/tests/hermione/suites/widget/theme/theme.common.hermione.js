const { build, getUrl, widgetParamsUpdaterFactory } = require('../utils');

specs({
    feature: 'Тема оформления виджета',
}, function () {
    it('Темы виджета корректно отображаются', async function () {
        await this.browser.url(getUrl(build.widget, { theme: 'light' }));
        await this.browser.waitForVisible('.ya-chat-widget');

        // Light
        await this.browser.assertView('light', '.hermione');

        // Dark
        this.browser.execute(...widgetParamsUpdaterFactory({ theme: 'dark' }));
        await this.browser.waitForVisible('.ya-chat-widget');
        await this.browser.assertView('dark', '.hermione');

        // Legacy
        this.browser.execute(...widgetParamsUpdaterFactory({ theme: 'legacy' }));
        await this.browser.waitForVisible('.ya-messenger');
        await this.browser.assertView('legacy', '.ya-messenger');

        // Hidden
        this.browser.execute(...widgetParamsUpdaterFactory({ theme: 'hidden' }));
        await this.browser.waitForVisible('.ya-chat-widget');
        await this.browser.assertView('hidden', '.hermione');
    });
});
