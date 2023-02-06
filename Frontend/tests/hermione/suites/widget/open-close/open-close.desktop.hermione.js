const { build, getUrl } = require('../utils');

specs({
    feature: 'Обработка кликов по виджету',
}, function () {
    it('Виджет открывается и закрывается', async function () {
        await this.browser.url(getUrl(build.widget));
        await this.browser.waitForVisible('.ya-chat-widget');

        // Виджет открывается по клику
        await this.browser.click('.ya-chat-button');
        await this.browser.waitForVisible('.ya-chat-header');
        await this.browser.assertView('widget-open', '.ya-chat-header');

        // Виджет закрывается по клику на крестик
        await this.browser.click('.ya-chat-header__close');
        await this.browser.assertView('widget-close', '.hermione');
    });
});
