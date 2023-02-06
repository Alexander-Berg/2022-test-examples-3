const { build, getUrl, widgetParamsUpdaterFactory } = require('../utils');

specs({
    feature: 'Заголовок виджета',
}, function () {
    it('Заголовок виджета отображается и обрезается по длине', async function () {
        await this.browser.url(getUrl(build.widget));
        await this.browser.waitForVisible('.ya-chat-widget');

        // Short title text
        this.browser.execute(...widgetParamsUpdaterFactory());
        await this.browser.waitForVisible('.ya-chat-widget');
        await this.browser.click('.ya-chat-button');
        await this.browser.waitForVisible('.ya-chat-header');
        await this.browser.assertView('title-text-short', '.ya-chat-header', { ignoreElements: ['.ya-chat-widget__mount'] });

        // Long title text
        this.browser.execute(...widgetParamsUpdaterFactory({ title: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.' }));
        await this.browser.waitForVisible('.ya-chat-widget');
        await this.browser.click('.ya-chat-button');
        await this.browser.waitForVisible('.ya-chat-header');
        await this.browser.assertView('title-text-long', '.ya-chat-header', { ignoreElements: ['.ya-chat-widget__mount'] });
    });
});
