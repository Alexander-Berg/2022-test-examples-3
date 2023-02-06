const selectors = require('../../../page-objects');

describe('Блоки', function() {
    hermione.only.in('chromeMobile');
    describe('Реклама', function() {
        const advConfig = {
            index: {
                // основные блоки
                '3817': 4326,
                '3818': 4327,
                '3819': 4328,
                // блоки в новостях
                '6343': 6343,
                '6344': 6344,
                '6345': 6345,
                '6346': 6346,
                '6347': 6347,
            },
            details: {
                '3826': 4334,
                '3836': 4344
            },
            months: {
                '3824': 4330,
                '3825': 4331,
                '3822': 4332,
                '3823': 4333
            },
            months30: {
                '3824': 4330,
                '3825': 4331,
                '3822': 4332
            },
            region: {
                '3846': 4354,
                '3847': 4355,
                '3848': 4356
            }
        };

        const advPrefix = 'R-I-486953-';

        const convertIdsToHamster = ids => Object.keys(ids).map(id => `${id}:${ids[id]}`).join(',');

        const advResultIds = ids => Object.values(ids).map(adv => {
            if (typeof adv === 'string') {
                return adv;
            }

            return `${advPrefix}${adv}`;
        });

        const testAdvsByPage = async function({ path, selector, ids, page }) {
            const advFlags = convertIdsToHamster(ids);

            await this.browser
                .ywOpenPage(path, {
                    query: {
                        showmethehamster: {
                            adv_spatouch: advFlags,
                            [`adv_spatouch_${page}`]: advFlags,
                            spa_maps: 1,
                            is_autotest_adv: 1
                        }
                    },
                })
                .localStorage('DELETE', 'persist:weather.main')
                .ywWaitForVisible(selectors[selector], 10000)
                .pause(3000) // реклама иногда не быстро грузится
                .ywAssertAdv(advResultIds(ids));
        };

        describe('Старые подменные рекламные блоки', function() {
            it('Главная страница', async function() {
                return testAdvsByPage.call(this, { path: 'moscow', selector: 'mainScreen', ids: advConfig.index, page: 'main' });
            });

            it('Details', async function() {
                return testAdvsByPage.call(this, {
                    path: 'moscow/details/today',
                    selector: 'detailsScreen',
                    ids: advConfig.details
                });
            });

            it('Страница месяца 30 дней', async function() {
                return testAdvsByPage.call(
                    this,
                    { path: '2/month', selector: 'monthScreen', ids: advConfig.months30, page: 'month' }
                );
            });

            it('Страница месяца январь', async function() {
                return testAdvsByPage.call(
                    this,
                    { path: '2/month/january', selector: 'monthScreen', ids: advConfig.months, page: 'month' }
                );
            });

            it('Страница региона', async function() {
                return testAdvsByPage.call(
                    this,
                    { path: 'region', selector: 'regionScreen', ids: advConfig.region, page: 'region' }
                );
            });
        });

        describe('Подменные рекламные блоки', function() {
            it('Главная страница', async function() {
                return this.browser
                    .ywOpenPage('moscow', {
                        query: {
                            showmethehamster: {
                                adv_spatouch: '2222:0',
                                adv_spatouch_main: 'top:R-I-486953-3828,middle:R-I-486953-3829,bottom:R-I-486953-3830,news_1:R-I-486953-3831,news_2:R-I-486953-3832,news_3:R-I-486953-3833,news_4:R-I-486953-3834,news_5:R-I-486953-3835',
                                is_autotest_adv: 1
                            }
                        },
                    })
                    .localStorage('DELETE', 'persist:weather.main')
                    .ywWaitForVisible(selectors.mainScreen, 10000)
                    .pause(3000) // реклама иногда не быстро грузится
                    .ywAssertAdv(['R-I-486953-3828', 'R-I-486953-3829', 'R-I-486953-3830', 'R-I-486953-3831', 'R-I-486953-3832', 'R-I-486953-3833', 'R-I-486953-3834', 'R-I-486953-3835']);
            });

            it('Страница детального прогноза', async function() {
                return this.browser
                    .ywOpenPage('moscow/details/today', {
                        query: {
                            showmethehamster: {
                                adv_spatouch: '2222:0',
                                adv_spatouch_details: 'top:R-I-486953-3817,bottom:R-I-486953-3818',
                                is_autotest_adv: 1
                            }
                        },
                    })
                    .localStorage('DELETE', 'persist:weather.main')
                    .ywWaitForVisible(selectors.detailsScreen, 10000)
                    .pause(3000) // реклама иногда не быстро грузится
                    .ywAssertAdv(['R-I-486953-3817', 'R-I-486953-3818']);
            });

            it('Страница месяца', async function() {
                return this.browser
                    .ywOpenPage('2/month', {
                        query: {
                            showmethehamster: {
                                adv_spatouch: '2222:0',
                                adv_spatouch_month: 'calendarTop:R-I-486953-3817,calendarBottom:R-I-486953-3818,bottom:R-I-486953-3819',
                                is_autotest_adv: 1
                            }
                        },
                    })
                    .localStorage('DELETE', 'persist:weather.month')
                    .ywWaitForVisible(selectors.monthScreen, 10000)
                    .pause(3000) // реклама иногда не быстро грузится
                    .ywAssertAdv(['R-I-486953-3817', 'R-I-486953-3818', 'R-I-486953-3819']);
            });

            it('Страница региона', async function() {
                return this.browser
                    .ywOpenPage('region', {
                        query: {
                            showmethehamster: {
                                adv_spatouch: '2222:0',
                                adv_spatouch_region: 'top:R-I-486953-3817,middle:R-I-486953-3818,bottom:R-I-486953-3819',
                                is_autotest_adv: 1
                            }
                        },
                    })
                    .localStorage('DELETE', 'persist:weather.region')
                    .ywWaitForVisible(selectors.regionScreen, 10000)
                    .pause(3000) // реклама иногда не быстро грузится
                    .ywAssertAdv(['R-I-486953-3817', 'R-I-486953-3818', 'R-I-486953-3819']);
            });
        });

        it('Переход на главную из поиска', async function() {
            const advFlags = convertIdsToHamster(advConfig.index);

            await this.browser
                .ywOpenPage('moscow', {
                    query: {
                        showmethehamster: {
                            // в зависимости от того, как заведены экспы
                            // одни флаги могут затирать другие
                            // приходится указывать и старый способ подмены и новый
                            // чтобы не флапали тесты
                            adv_spatouch: advFlags,
                            adv_spatouch_main: advFlags,
                            is_autotest_adv: 1
                        }
                    },
                })
                .ywWaitForVisible(selectors.mainScreen, 10000)
                .pause(3000) // ждём, пока реклама прогрузится
                .click(selectors.index.Search)
                .ywWaitForVisible(selectors.searchRecommendations, 10000)
                .execute(() => {
                    // сбрасываем загруженную рекламу из глобальной переменной
                    // чтобы при переходе обратно на главную можно было проверить загрузку рекламы заново
                    window.completelyLoadResources = [];
                })
                .click(selectors.search.RecommendationsLast)
                .ywWaitForVisible(selectors.mainScreen, 10000)
                .pause(3000)
                .ywAssertAdv(advResultIds(advConfig.index));
        });
    });
});
