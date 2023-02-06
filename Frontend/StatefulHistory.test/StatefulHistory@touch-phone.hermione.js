'use strict';

const PO = require('./StatefulHistory.page-object')('touch-phone');

specs('Колдунщик истории запросов в stateful сценарии', function() {
    describe('Базовый вариант', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp(
                {
                    text: 'foreverdata',
                    data_filter: 'stateful-history',
                    foreverdata: '4275897214',
                },
                PO.statefulHistory(),
            );
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид', async function() {
            await this.browser.assertView('stateful-history', PO.statefulHistory());
        });

        hermione.also.in('iphone-dark');
        it('Клик в айтем расхлоп', async function() {
            await this.browser.yaShouldNotBeVisible(PO.statefulHistory.HistoryCollapser.CollapserOpened());

            await this.browser.yaCheckBaobabCounter(
                () => this.browser.click(PO.statefulHistory.HistoryCollapser.Collapser.Label()),
                {
                    path: '/$page/$main/$result/stateful-history/day-requests/request/Collapser/link',
                    behaviour: { type: 'dynamic' },
                },
            );

            await this.browser.yaWaitForVisible(PO.statefulHistory.HistoryCollapser.CollapserOpened());

            await this.browser.assertView('stateful-history-with-opened', PO.statefulHistory());

            await this.browser.yaCheckLink2({
                selector: PO.statefulHistory.HistoryCollapser.CollapserOpened.StatefulSite(),
                baobab: {
                    path: '/$page/$main/$result/stateful-history/day-requests/request/Collapser/site/link',
                },
                target: '_blank',
            });
        });

        it('Клик в айтем ссылку', async function() {
            await this.browser.yaCheckBaobabCounter(
                () => this.browser.click(PO.statefulHistory.statefulRequest()),
                {
                    path: '/$page/$main/$result/stateful-history/day-requests/request/link',
                },
            );
        });

        hermione.also.in('iphone-dark');
        it('Очистка истории', async function() {
            await this.browser.yaCheckBaobabCounter(
                () => this.browser.click(PO.statefulHistory.settingsIcon()),
                {
                    path: '/$page/$main/$result/settings-icon',
                    behaviour: { type: 'dynamic' },
                },
            );

            await this.browser.yaWaitForVisible(PO.statefulSettingPopup());

            await this.browser.assertView('with-settings-popup', PO.statefulHistory());

            await this.browser.yaCheckBaobabCounter(
                () => this.browser.click(PO.statefulSettingPopup.Link()),
                {
                    path: '/$page/$main/$result/stateful-history/clear-history-link',
                    behaviour: { type: 'dynamic' },
                },
            );

            await this.browser.yaWaitForHidden(PO.statefulSettingPopup());

            await this.browser.yaWaitForVisible(PO.statefulHistoryDrawer());
            await this.browser.yaWaitForVisible(PO.statefulHistoryDrawer.buttonConfirm());
            await this.browser.yaWaitForVisible(PO.statefulHistoryDrawer.buttonCancel());

            await this.browser.assertView('stateful-clear-drawer', PO.statefulHistoryDrawer(), {
                hideElements: [PO.main(), PO.header()],
            });

            await this.browser.yaCheckBaobabCounter(
                () => this.browser.click(PO.statefulHistoryDrawer.buttonConfirm()),
                {
                    path: '/$page/$main/$result/stateful-history/clear-history-confirm',
                    behaviour: { type: 'dynamic' },
                },
            );

            await this.browser.yaWaitForVisible(PO.statefulHistory.subtitle());

            await this.browser.assertView('after-clear-data', PO.statefulHistory());

            await this.browser.yaCheckBaobabCounter(() => {}, {
                path: '/$page/$main/$result/stateful-history',
                event: 'tech',
                type: 'serp-stateful-clear-history',
                data: { themeId: '4311' },
            });
        });
    });

    describe('Без истории', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp(
                {
                    text: 'foreverdata',
                    data_filter: 'stateful-history',
                    foreverdata: '3360604784',
                },
                PO.statefulHistory(),
            );
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид', async function() {
            await this.browser.assertView('stateful-history-empty', PO.statefulHistory());
        });
    });

    describe('С кнопкой Показать ещё', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp(
                {
                    text: 'foreverdata',
                    data_filter: 'stateful-history',
                    foreverdata: '2945574214',
                },
                PO.statefulHistory(),
            );
        });

        it('Внешний вид кнопки и догрузка сайтов', async function() {
            await this.browser.yaShouldNotBeVisible(PO.statefulHistory.HistoryCollapser.CollapserOpened());

            await this.browser.yaCheckBaobabCounter(
                () => this.browser.click(PO.statefulHistory.SecondHistoryCollapser.Collapser.Label()),
                {
                    path: '/$page/$main/$result/stateful-history/day-requests/request/Collapser/link',
                    behaviour: { type: 'dynamic' },
                },
            );

            await this.browser.yaWaitForVisible(PO.statefulHistory.HistoryCollapser.CollapserOpened());
            await this.browser.yaWaitForVisible(PO.statefulHistory.HistoryCollapser.CollapserOpened.Button());

            await this.browser.assertView('stateful-history-button', PO.statefulHistory());

            await this.browser.yaCheckBaobabCounter(
                () => this.browser.click(PO.statefulHistory.HistoryCollapser.CollapserOpened.Button()),
                {
                    path: '/$page/$main/$result/stateful-history/day-requests/request/Collapser/show-all',
                    behaviour: { type: 'dynamic' },
                },
            );

            await this.browser.yaWaitForHidden(PO.statefulHistory.HistoryCollapser.CollapserOpened.Button());

            await this.browser.assertView('stateful-history-with-all-sites', PO.statefulHistory());
        });
    });

    describe('Много поисков в истории без переходов', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp(
                {
                    text: 'foreverdata',
                    data_filter: 'stateful-history',
                    foreverdata: '2507067362',
                },
                PO.statefulHistory(),
            );
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид', async function() {
            await this.browser.assertView('stateful-history-many-empty-requests', PO.statefulHistory());

            await this.browser.click(PO.statefulHistory.HistoryCollapser.Collapser.Label());
            await this.browser.yaWaitForVisible(PO.statefulHistory.HistoryCollapser.CollapserOpened());

            await this.browser.assertView('stateful-history-expanded', PO.statefulHistory());
        });
    });
});
