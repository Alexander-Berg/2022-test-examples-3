const { build, getUrl, widgetParamsUpdaterFactory } = require('../utils');

specs({
    feature: 'Иконка на кнопке виджета',
}, function () {
    it('Все иконки отображаются корректно', async function () {
        await this.browser.url(getUrl(build.widget));
        await this.browser.waitForVisible('.ya-chat-widget');

        // Auto
        await this.browser.assertView('auto', '.hermione');

        // Colored
        this.browser.execute(...widgetParamsUpdaterFactory({ buttonIcon: 'colored' }));
        await this.browser.waitForVisible('.ya-chat-widget');
        await this.browser.assertView('colored', '.hermione');

        // Black
        this.browser.execute(...widgetParamsUpdaterFactory({ buttonIcon: 'black' }));
        await this.browser.waitForVisible('.ya-chat-widget');
        await this.browser.assertView('black', '.hermione');

        // White
        this.browser.execute(...widgetParamsUpdaterFactory({ buttonIcon: 'white', theme: 'dark' }));
        await this.browser.waitForVisible('.ya-chat-widget');
        await this.browser.assertView('white', '.hermione');
    });

    it('Все иконки отображаются ожидаемо на теме схожего цвета', async function () {
        await this.browser.url(getUrl(build.widget));
        await this.browser.waitForVisible('.ya-chat-widget');

        // Colored
        this.browser.execute(...widgetParamsUpdaterFactory({ buttonIcon: 'colored', theme: 'dark' }));
        await this.browser.waitForVisible('.ya-chat-widget');
        await this.browser.assertView('colored', '.hermione');

        // Black
        this.browser.execute(...widgetParamsUpdaterFactory({ buttonIcon: 'black', theme: 'dark' }));
        await this.browser.waitForVisible('.ya-chat-widget');
        await this.browser.assertView('black', '.hermione');

        // White
        this.browser.execute(...widgetParamsUpdaterFactory({ buttonIcon: 'white' }));
        await this.browser.waitForVisible('.ya-chat-widget');
        await this.browser.assertView('white', '.hermione');
    });
});
