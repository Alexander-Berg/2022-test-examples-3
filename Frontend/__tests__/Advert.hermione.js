const _ = require('lodash');
const getPlatformByBrowser = require('../../../../hermione/utils/get-platform-by-browser');

function withIndex(selector, index) {
    typeof selector === 'function' && (selector = selector());
    return selector + `[data-index="${index}"]`;
}

specs({
    feature: 'advert-react',
}, () => {
    hermione.only.notIn('safari13');
    it('Реклама должна не показываться на странице под флагом adv_disabled', function() {
        return this.browser
            .url('?stub=advert/react-basic.json&exp_flags=force-react-advert=1')
            .isExisting(PO.reactAdvert())
            .then(isExisting => assert.isFalse(isExisting, 'Реклама видна на странице'));
    });

    hermione.only.in(['chrome-phone'], 'Ускоряем браузеронезависимые тесты');
    hermione.only.notIn('safari13');
    it('Реклама не должна показываться на странице при медленных соединениях под флагом', function() {
        return this.browser
            .url('?stub=advert/react-basic.json&exp_flags=force-react-advert=1&exp_flags=adv-disabled=0&exp_flags=ads-on-bad-connection=1&user_connection=slow_connection=1')
            .isExisting(PO.reactAdvert())
            .then(isExisting => assert.isFalse(isExisting, 'Реклама видна на странице'));
    });

    hermione.only.notIn('safari13');
    it('Реактовая реклама должна не показываться без флага включающего ее', function() {
        return this.browser
            .url('?stub=advert/react-basic.json&exp_flags=adv-disabled=0')
            .isExisting(PO.reactAdvert())
            .then(isExisting => assert.isFalse(isExisting, 'Реклама видна на странице'));
    });

    hermione.only.notIn('safari13');
    it('Реклама должна показываться на странице без флага adv_disabled', function() {
        return this.browser
            .url('?text=test_news&exp_flags=adv-disabled=0&exp_flags=force-react-advert=1')
            .isExisting(PO.reactAdvert())
            .then(isExisting => assert.isTrue(isExisting, 'Реклама не видна на странице'));
    });

    hermione.only.notIn('safari13');
    it('Скриншот стаба рекламы', function() {
        return this.browser
            .url('?stub=advert/react-basic.json&exp_flags=adv-disabled=0&hermione_advert=stub&exp_flags=force-react-advert=1')
            .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
            .yaWaitAdvert(PO, 'Реклама не загрузилась')
            .isExisting(PO.reactAdvert())
            .then(isExisting => assert.isTrue(isExisting, 'Реклама не видна на странице'))
            .assertView('plain', PO.page());
    });

    [
        {
            name: 'partner',
            type: '1219',
            pageId: '196263',
        },
        {
            name: 'adfox',
            type: '2930',
            pageId: '257673',
        },
    ].forEach(ad => {
        hermione.only.notIn('safari13');
        it(`RUM-счётчики про скорость рекламы (${ad.name})`, function() {
            let platformId = '584'; // touch
            let stubPart = '';
            let secondPagePO = PO.firstRelatedAutoload();

            if (getPlatformByBrowser(hermione, this.browser) === 'desktop') {
                stubPart = '-desktop';
                platformId = '2048'; // desktop
                secondPagePO = PO.firstRelatedAutoloadDesktop();
            }

            const counter = {
                path: '/tech/perf/time',
                vars: {
                    '143': `28.2719.${platformId}`, // bs: page = ru/turbo/touch
                    '1366': ad.pageId, // bs: paid = …
                    '76': '1',
                },
                varPatterns: {
                    '2322': /[0-9.]+/, // bs: start_time
                    '2877': /[0-9.]+/, // bs: delta
                },
            };

            return this.browser
                .url(
                    `?stub=advert%2Freact-rum-counters${stubPart}-1-${ad.name}.json` +
                    '&exp_flags=adv-disabled=0' +
                    '&exp_flags=force-react-advert=1'
                )
                .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
                .yaWaitAdvert(PO, 'Реклама не загрузилась')
                .isExisting(PO.reactAdvert())
                .then(isExisting => assert.isTrue(isExisting, 'Реклама не видна на странице'))
                .yaWaitForVisible(PO.reactAdvert(), 'Реклама не видна на странице')
                .yaScrollPageToBottom()
                .yaIndexify(PO.relatedAutloadDesktop())
                .yaWaitForVisible(secondPagePO, 'Не загрузилась вторая страница')
                .yaCheckCounter(
                    () => {},
                    (
                        counter.vars['1701'] = `${ad.type}.1724`, // bs: id = load
                        [counter] // передаваемый аргумент в функцию
                    ),
                    `Не сработал счетчик на загрузку ${ad.name}`,
                )
                .yaCheckCounter(
                    () => {},
                    (
                        counter.vars['1701'] = `${ad.type}.3105`, // bs: id = prerender
                        [counter] // передаваемый аргумент в функцию
                    ),
                    `Не сработал счетчик на пререндер ${ad.name}`,
                )
                .yaCheckCounter(
                    () => {},
                    (
                        counter.vars['1701'] = `${ad.type}.2046`, // bs: id = render
                        [counter] // передаваемый аргумент в функцию
                    ),
                    `Не сработал счетчик на рендеринг ${ad.name}`,
                )
                .yaGetCounters(counters => {
                    const advPerfCounters = _.filter(
                        counters.client,
                        counter =>
                            counter.path === '690.2096.207' && // bs: /tech/perf/time
                            (counter.vars['1701'].endsWith('.1724') || counter.vars['1701'].endsWith('.2046') || counter.vars['1701'].endsWith('.3105'))
                    );

                    assert.equal(
                        advPerfCounters.length,
                        3,
                        `Должен быть отправлен /tech/perf/time только на первую рекламу ${ad.name}`
                    );
                });
        });
    });

    hermione.only.in(['chrome-phone'], 'Ускоряем браузеронезависимые тесты');
    hermione.only.notIn('safari13');
    it('Должны пробрасываться параметры cacheId и adSessionId', function() {
        return this.browser
            .url('?stub=advert/partner-cache-1.json&exp_flags=adv-disabled=0&hermione_advert=stub&exp_flags=force-react-advert=1&load-react-advert-script=1')
            .yaIndexify(PO.page.result)
            .yaAdvertCallParams({ blockId: 'R-I-260855-1' })
            .then(params => {
                assert.equal(params.cacheId, '123', 'Не пробросился cacheId для первой рекламы на первой странице');
                assert.equal(params.adSessionId, 'abc', 'Не пробросился adSessionId для первой рекламы на первой странице');
            })
            .yaAdvertCallParams({ blockId: 'R-I-260855-2' })
            .then(params => {
                assert.equal(params.cacheId, '234', 'Не пробросился cacheId для второй рекламы на первой странице');
                assert.equal(params.adSessionId, 'abc', 'Не пробросился adSessionId для второй рекламы на первой странице');
            })
            .yaScrollPageToBottom()
            .yaWaitForVisible(withIndex(PO.page.result, 1))
            .yaAdvertCallParams({ blockId: 'R-I-260855-3' })
            .then(params => {
                assert(!params.hasOwnProperty('cacheId'), 'Ненужный cacheId для первой рекламы на второй странице');
                assert(!params.hasOwnProperty('adSessionId'), 'Ненужный adSessionId для первой рекламы на второй странице');
            })
            .yaAdvertCallParams({ blockId: 'R-I-260855-4' })
            .then(params => {
                assert.equal(params.cacheId, '456', 'Не пробросился cacheId для второй рекламы на второй странице');
                assert.equal(params.adSessionId, 'bcd', 'Не пробросился adSessionId для второй рекламы на второй странице');
            })
            .yaScrollPageToBottom()
            .yaWaitForVisible(withIndex(PO.page.result, 2))
            .yaAdvertCallParams({ blockId: 'R-I-260855-5' })
            .then(params => {
                assert(!params.hasOwnProperty('cacheId'), 'Ненужный cacheId для первой рекламы на третьей странице');
                assert(!params.hasOwnProperty('adSessionId'), 'Ненужный adSessionId для первой рекламы на третьей странице');
            })
            .yaAdvertCallParams({ blockId: 'R-I-260855-6' })
            .then(params => {
                assert(!params.hasOwnProperty('cacheId'), 'Ненужный cacheId для второй рекламы на третьей странице');
                assert(!params.hasOwnProperty('adSessionId'), 'Ненужный adSessionId для второй рекламы на третьей странице');
            });
    });
});
