const { getUrl, widgetParamsUpdaterFactory } = require('../utils');

specs({
    feature: 'Бадж с непрочитанными сообщениями',
}, function () {
    ['widget', 'widget_ya'].forEach((item) => {
        it(`Проверка непрочитанных в сборке ${item}`, async function () {
            await this.browser.url(getUrl(item));
            await this.browser.waitForVisible('.ya-chat-widget');

            // Нет непрочитанных
            await this.browser.assertView(`no-unread-${item}`, '.hermione');

            // Непрочитанные стиль "Точка"
            this.browser.execute(...widgetParamsUpdaterFactory({ badgeCount: 1, theme: 'light', badgeType: 'dot', isMobile: false }));
            await this.browser.waitForVisible('.ya-chat-widget');
            await this.browser.assertView(`unread-light-dot-${item}`, '.hermione');

            this.browser.execute(...widgetParamsUpdaterFactory({ badgeCount: 1, theme: 'dark', badgeType: 'dot', isMobile: false }));
            await this.browser.waitForVisible('.ya-chat-widget');
            await this.browser.assertView(`unread-dark-dot-${item}`, '.hermione');

            this.browser.execute(...widgetParamsUpdaterFactory({ badgeCount: 1, theme: 'legacy', badgeType: 'dot', isMobile: false }));
            await this.browser.waitForVisible('.ya-messenger');
            await this.browser.assertView(`unread-legacy-dot-${item}`, '.ya-messenger');

            // Непрочитанные стиль "Счетчик", количество меньше максимального
            this.browser.execute(...widgetParamsUpdaterFactory({ badgeCount: 1, theme: 'light', badgeType: 'num', isMobile: false }));
            await this.browser.waitForVisible('.ya-chat-widget');
            await this.browser.assertView(`unread-light-num-ltmax-${item}`, '.hermione');

            this.browser.execute(...widgetParamsUpdaterFactory({ badgeCount: 1, theme: 'dark', badgeType: 'num', isMobile: false }));
            await this.browser.waitForVisible('.ya-chat-widget');
            await this.browser.assertView(`unread-dark-num-ltmax-${item}`, '.hermione');

            this.browser.execute(...widgetParamsUpdaterFactory({ badgeCount: 1, theme: 'legacy', badgeType: 'num', isMobile: false }));
            await this.browser.waitForVisible('.ya-messenger');
            await this.browser.assertView(`unread-legacy-num-ltmax-${item}`, '.ya-messenger');

            // Непрочитанные стиль "Счетчик", количество больше максимального
            this.browser.execute(...widgetParamsUpdaterFactory({ badgeCount: 100, theme: 'light', badgeType: 'num', isMobile: false }));
            await this.browser.waitForVisible('.ya-chat-widget');
            await this.browser.assertView(`unread-light-num-gtmax-${item}`, '.hermione');

            this.browser.execute(...widgetParamsUpdaterFactory({ badgeCount: 100, theme: 'dark', badgeType: 'num', isMobile: false }));
            await this.browser.waitForVisible('.ya-chat-widget');
            await this.browser.assertView(`unread-dark-num-gtmax-${item}`, '.hermione');

            this.browser.execute(...widgetParamsUpdaterFactory({ badgeCount: 100, theme: 'legacy', badgeType: 'num', isMobile: false }));
            await this.browser.waitForVisible('.ya-messenger');
            await this.browser.assertView(`unread-legacy-num-gtmax-${item}`, '.ya-messenger');
        });
    });
});
