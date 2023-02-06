const selectors = require('../../page-objects');

describe('Страницы', function() {
    describe('Главная', function() {
        it('Страница', async function() {
            // делаем выборку всех детей
            const invisibleElements = [
                selectors.index.HourlyMain + ' *',
                selectors.index.HourlyContainer + ' *',
                selectors.index.Forecast + ' *',
                selectors.index.MonthLinkSecond + ' *',
                selectors.index.MonthCardTextWrap + ' *',
                selectors.index.MapsWidget + ' *',
                selectors.index.MapAlert + ' *',
                selectors.index.HistoryCard + ' *',
                selectors.meteum + ' *',
                selectors.footer + ' *',
            ];
            const hideElements = [selectors.header.Common, selectors.tech.BaobabButton];

            await this.browser
                .ywOpenPage('moscow', {
                    lang: this.lang,
                    query: {
                        usemock: `turboapp_moscow${this.lang.tld !== 'ru' ? `_${this.lang.tld}` : ''}`,
                    },
                })
                .ywWaitForVisible(selectors.mainScreen, 10000)
                .ywHideCamerasAndNews()
                .ywDisguiseAllIndexBlocks()
                .assertView('MainPage', selectors.fullPage, { invisibleElements, hideElements });
        });

        it('Первый экран', async function() {
            await this.browser
                .ywOpenPage('moscow', {
                    lang: this.lang,
                    query: {
                        usemock: 'turboapp-weather-main',
                        showmethehamster: { spa_new_fact: 1, spa_main_report_force: 1 }
                    },
                })
                .ywWaitForVisible(selectors.index.Header, 10000)
                .assertView('MainHeader', selectors.index.Header)
                .assertView('MainMapImg', selectors.index.MapImg, {
                    hideElements: [selectors.index.MapImgItem, selectors.index.MapImgPin]
                })
                .assertView('MainFact', selectors.index.HourlyMain, {
                    allowViewportOverflow: true,
                    hideElements: [selectors.index.MapImgItem, selectors.index.MainSliders]
                });
        });

        hermione.only.in('chromeMobile');
        it('Первый экран (вне зоны наукаста)', async function() {
            await this.browser
                .ywOpenPage('moscow', {
                    lang: this.lang,
                    query: {
                        usemock: 'without_nowcast',
                        showmethehamster: { spa_new_fact: 1, spa_main_report_force: 1 }
                    },
                })
                .ywWaitForVisible(selectors.index.Header, 10000)
                .assertView('MainHeader', selectors.index.Header)
                .assertView('MainFact', selectors.index.HourlyMain, {
                    allowViewportOverflow: true,
                    hideElements: [selectors.index.MapImgItem, selectors.index.MainSliders]
                });
        });

        hermione.only.in('chromeMobile');
        it('Первый экран(темная тема)', async function() {
            await this.browser
                .ywOpenPage('moscow', {
                    lang: this.lang,
                    query: {
                        usemock: 'turboapp-weather-main',
                        showmethehamster: { spa_new_fact: 1, spa_color_theme: 'dark', spa_main_report_force: 1 }
                    },
                })
                .ywWaitForVisible(selectors.index.Header, 10000)
                .assertView('MainHeader', selectors.index.Header)
                .assertView('MainMapImg', selectors.index.MapImg, {
                    hideElements: [selectors.index.MapImgItem, selectors.index.MapImgPin]
                })
                .assertView('MainFact', selectors.index.HourlyMain, {
                    allowViewportOverflow: true,
                    hideElements: [selectors.index.MapImgItem, selectors.index.MainSliders]
                });
        });

        it('Скелетон', async function() {
            const hideElements = [selectors.tech.BaobabButton];

            await this.browser
                .ywOpenPage('moscow', {
                    lang: this.lang,
                    query: {
                        usemock: 'turboapp-skeleton',
                        /*jshint camelcase: false */
                        showmethehamster: { show_skeleton: 1 },
                    },
                })
                .ywStopSkeletonAnimation()
                .assertView('MainPage_skeleton', selectors.fullPage, { hideElements });
        });
    });
});
