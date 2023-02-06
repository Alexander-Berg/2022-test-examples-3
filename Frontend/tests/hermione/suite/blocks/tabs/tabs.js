const selectors = require('../../../page-objects').details;

describe('Блоки', function() {
    describe('Страница детального прогноза', function() {
        describe('Табы', function() {
            const CASES = {
                today: {
                    name: 'Таб текущего дня',
                    testCases: {
                        withLocation: { name: 'с локацией', url: 'moscow/details' },
                        withoutLocation: { name: 'без локации', url: 'details/' }
                    }
                },
                tomorrow: {
                    name: 'Таб завтра',
                    testCases: {
                        withLocation: { name: 'с локацией', url: 'moscow/details/tomorrow' },
                        withoutLocation: { name: 'без локации', url: 'details/tomorrow' },
                    }
                },
                dayAfterTomorrow: {
                    name: 'Таб после завтра',
                    testCases: {
                        withLocation: { name: 'с локацией', url: 'moscow/details/day-2' },
                        withoutLocation: { name: 'без локации', url: 'details/day-2' },
                    }
                },
            };
            const DESCRIBES = Object.keys(CASES);

            for (let i = 0; i < DESCRIBES.length; i += 1) {
                const { name, testCases } = CASES[DESCRIBES[i]];

                describe(name, function() {
                    const ITS = Object.values(testCases);

                    for (let j = 0; j < ITS.length; j += 1) {
                        it.langs.full();
                        it(ITS[j].name, async function() {
                            await this.browser
                                .ywOpenPage(ITS[j].url, {
                                    lang: this.lang,
                                    query: {
                                        usemock: `turboapp_moscow${this.lang.tld !== 'ru' ? `_${this.lang.tld}` : ''}`,
                                    }
                                })
                                .ywWaitForVisible(selectors.Tabs, 5000)
                                .assertView('detailsPage', selectors.Tabs);
                        });
                    }
                });
            }

            hermione.only.in('chromeMobile');
            it('Табы идут по порядку календарных дней', async function() {
                await this.browser
                    .ywOpenPage('donetsk/details/today', {
                        lang: this.lang,
                        query: {
                            usemock: 'spa_double_day',
                        }
                    })
                    .ywWaitForVisible(selectors.Tabs, 5000);

                const daysNumbers = await this.browser.execute(selector => {
                    const numbers = [];

                    document
                        .querySelectorAll(selector)
                        .forEach(item => numbers.push(item.innerHTML));

                    return numbers.join(',');
                }, selectors.TabsTabAll);

                assert.equal('Пт, 26,Сб, 27,Вс, 28,Пн, 29,Вт, 30,Ср, 31,Чт, 1 апреля,Пт, 2,Сб, 3,Вс, 4', daysNumbers.value, 'Неверная нумерация дней - проверьте forecasts.date');
            });
        });
    });
});
