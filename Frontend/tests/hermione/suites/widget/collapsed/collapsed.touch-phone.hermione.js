const { getUrl, build, widgetParamsUpdaterFactory } = require('../utils');

specs({
    feature: 'Состояние кнопки на тачах',
}, function () {
    it('Состояния кнопки корректно отображаются на тачах', async function () {
        await this.browser.url(getUrl(build.widget, { collapsedTouch: 'never' }));
        await this.browser.waitForVisible('.ya-chat-widget');

        // collapsedTouch=never
        await this.browser.assertView('collapsed-never', '.hermione');

        // collapsedTouch=always
        this.browser.execute(...widgetParamsUpdaterFactory({ collapsedTouch: 'always' }));
        await this.browser.waitForVisible('.ya-chat-widget');
        await this.browser.assertView('collapsed-always', '.hermione');
    });
});
