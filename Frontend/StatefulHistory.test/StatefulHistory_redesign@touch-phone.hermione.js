'use strict';

const PO = require('./StatefulHistory.page-object')('touch-phone');

specs({
    feature: 'Колдунщик истории запросов в stateful сценарии',
    experiment: 'Редизайн',
}, function() {
    describe('Базовый вариант', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp(
                {
                    text: 'foreverdata',
                    data_filter: 'stateful-history',
                    foreverdata: '1232541256',
                },
                PO.statefulHistory(),
            );
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид', async function() {
            await this.browser.assertView('stateful-history', PO.statefulHistory());
        });

        it('Клик в айтем', async function() {
            await this.browser.yaCheckBaobabCounter(
                () => this.browser.click(PO.statefulHistory.requestLink()),
                {
                    path: '/$page/$main/$result/stateful-history/day-requests/request/search-request-link',
                },
            );
        });

        it('Клик в сайт', async function() {
            await this.browser.yaCheckLink2({
                selector: PO.statefulHistory.siteLink(),
                target: '_blank',
                baobab: {
                    path: '/$page/$main/$result/stateful-history/day-requests/request/site/link',
                },
            });
        });
    });

    hermione.only.in(['iphone']);
    it('Доступность', async function() {
        await this.browser.yaOpenSerp(
            {
                text: 'foreverdata',
                data_filter: 'stateful-history',
                foreverdata: '1232541256',
                exp_flags: 'a11y_validate=1',
            },
            PO.statefulHistory(),
        );

        await this.browser.yaCheckElementA11y(PO.statefulHistory());
    });

    describe('Без истории', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp(
                {
                    text: 'foreverdata',
                    data_filter: 'stateful-history',
                    foreverdata: '3038592289',
                },
                PO.statefulHistory(),
            );
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид', async function() {
            await this.browser.assertView('stateful-history-empty', PO.statefulHistory());
        });
    });

    describe('С ссылкой Ещё сайты', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp(
                {
                    text: 'foreverdata',
                    data_filter: 'stateful-history',
                    foreverdata: '1027963223',
                },
                PO.statefulHistory(),
            );
        });

        it('Внешний вид и догрузка сайтов', async function() {
            await this.browser.yaWaitForVisible(PO.statefulHistory.moreLink());

            await this.browser.assertView('stateful-history-with-more-link', PO.statefulHistory());

            await this.browser.yaCheckBaobabCounter(
                () => this.browser.click(PO.statefulHistory.moreLink()),
                {
                    path: '/$page/$main/$result/stateful-history/day-requests/request/show-all-link',
                    behaviour: { type: 'dynamic' },
                },
            );

            await this.browser.yaWaitForHidden(PO.statefulHistory.moreLink());

            await this.browser.assertView('stateful-history-with-all-sites', PO.statefulHistory());
        });
    });
});
