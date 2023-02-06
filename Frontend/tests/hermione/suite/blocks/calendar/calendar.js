const selectors = require('../../../page-objects').month;

describe('Блоки', function() {
    describe('Страница месяца', function() {
        it.langs.full();
        it('Календарь 30 дней', async function() {
            await this.browser
                .ywOpenPage('month', {
                    lang: this.lang,
                    query: {
                        usemock: 'thirty_days_all_temps',
                        mockgql: 'gql-month-calendar-30d',
                        mocknow: 1599388979
                    },
                })
                .ywWaitForVisible(selectors.CalendarWrap, 5000)
                .ywRemoveShadowFromContainer(selectors.Calendar)
                .assertView('Calendar30days', selectors.CalendarWrap);
        });
        it.langs.full();
        it('Календарь', async function() {
            await this.browser
                .ywOpenPage('month/january', {
                    lang: this.lang,
                    query: {
                        usemock: 'month_all_temps',
                        mockgql: 'gql-month-calendar',
                        mocknow: 1601987357
                    },
                })
                .ywWaitForVisible(selectors.CalendarWrap, 5000)
                .ywRemoveShadowFromContainer(selectors.Calendar)
                .assertView('Calendar', selectors.CalendarWrap);
        });
    });
});
