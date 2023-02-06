const selectors = require('../../../page-objects');

describe('Блоки', function() {
    describe('Главная страница', function() {
        describe('Алерты', function() {
            describe('Один алерт', function() {
                it('Алерт под фактом', async function() {
                    return this.browser
                        .ywOpenPage('moscow', {
                            lang: this.lang,
                            query: {
                                usemock: 'alerts-combo-1',
                            },
                        })
                        .ywWaitForVisible(selectors.index.AlertsSingle, 5000)
                        .pause(50) // possible layout shift
                        .ywHideCamerasAndNews()
                        .assertView('alert-popup', selectors.index.AlertsPopup, {
                            allowViewportOverflow: true,
                            hideElements: [selectors.index.HourlyContainer]
                        })
                        .click(selectors.index.AlertsVoteButton)
                        .assertView('alert-notice', selectors.index.AlertsPopup, {
                            allowViewportOverflow: true,
                            hideElements: [selectors.index.HourlyContainer]
                        })
                        .ywWaitForVisible(selectors.index.AlertsPopup, 5000, 'не скрылся попап', true)
                        .assertView('alerts', selectors.index.AlertsSingle);
                });
            });

            it('Несколько алертов', async function() {
                return this.browser
                    .ywOpenPage('moscow', {
                        lang: this.lang,
                        query: {
                            usemock: 'alerts-combo-3',
                        },
                    })
                    .ywWaitForVisible(selectors.index.AlertsSwipeableContent, 5000)
                    .pause(50) // possible layout shift
                    .ywHideCamerasAndNews()
                    .ywHidePopup()
                    .assertView('alerts', selectors.index.AlertsSwipeableContent);
            });
        });
    });
});
