const { getUrl, build, widgetParamsUpdaterFactory } = require('../utils');

specs({
    feature: 'Состояние кнопки на десктопах',
}, function () {
    it('Состояния кнопки корректно отображаются на десктопах', async function () {
        await this.browser.url(getUrl(build.widget, { collapsedDesktop: 'never' }));
        await this.browser.waitForVisible('.ya-chat-widget');

        // collapsedDesktop=never
        await this.browser.assertView('collapsed-never-no-hover', '.hermione');
        await this.browser.moveToObject('.ya-chat-button');
        await this.browser.assertView('collapsed-never-hover', '.hermione');

        // collapsedDesktop=always
        await this.browser.moveToObject('body');
        this.browser.execute(...widgetParamsUpdaterFactory({ collapsedDesktop: 'always' }));
        await this.browser.waitForVisible('.ya-chat-widget');
        await this.browser.assertView('collapsed-always-no-hover', '.hermione');
        await this.browser.moveToObject('.ya-chat-button');
        await this.browser.assertView('collapsed-always-hover', '.hermione');

        // collapsedDesktop=hover
        await this.browser.moveToObject('body');
        this.browser.execute(...widgetParamsUpdaterFactory({ collapsedDesktop: 'hover' }));
        await this.browser.waitForVisible('.ya-chat-widget');
        await this.browser.assertView('collapsed-hover-no-hover', '.hermione');
        await this.browser.moveToObject('.ya-chat-button');
        await this.browser.assertView('collapsed-hover-hover', '.hermione');
    });
});
