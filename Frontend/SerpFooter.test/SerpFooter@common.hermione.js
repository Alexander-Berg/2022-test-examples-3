'use strict';

specs({ feature: 'Футер' }, function() {
    describe('Стандартные проверки', function() {
        beforeEach(async function() {
            const { browser, PO } = this;

            await browser.yaOpenSerp({
                text: 'test',
                lr: 213,
                srcskip: 'YABS_DISTR',
                exp_flags: 'yabs_distr=0',
            }, PO.footer());

            await browser.yaScroll(PO.footer());
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид', async function() {
            const { browser, PO } = this;

            await browser.assertView('footer', PO.serpFooter(), {
                excludeElements: PO.serpFooter(),
                allowViewportOverflow: true,
            });
        });

        describe('Переключатель региона', function() {
            it('Внешний вид', async function() {
                const { browser, PO } = this;

                await browser.yaWaitForVisible(PO.serpFooter.region());

                await browser.assertView('region-change', PO.serpFooter.region(), { excludeElements: PO.serpFooter() });

                const region = await browser.getText(PO.serpFooter.region.link());
                assert.equal(region, 'Москва', 'В переключателе региона в футере не указан город "Москва"');
            });

            hermione.only.notIn('searchapp-phone');
            it('Отправка счетчиков', async function() {
                const { browser, PO } = this;

                await browser.yaCheckBaobabCounter(PO.serpFooter.region.link(), {
                    path: '/$page/$footer/region',
                });
            });
        });

        describe('Поиск в других системах', function() {
            it('Внешний вид', async function() {
                const { browser, PO } = this;

                await browser.assertView('searchengines', PO.serpFooter.searchengines(), { excludeElements: PO.serpFooter() });
            });

            it('Отправка счетчиков', async function() {
                const { browser, PO } = this;
                await browser.yaOpenSerp({
                    text: 'test',
                    tld: 'ru',
                    l10n: 'ru',
                    srcskip: 'YABS_DISTR',
                    exp_flags: 'yabs_distr=0',
                    data_filter: 'no_results',
                }, PO.serpFooter.searchengines());

                await browser.yaCheckBaobabCounter(PO.serpFooter.searchengines.firstLink(), {
                    path: '/$page/$footer/link[@type="google"]',
                });

                await browser.yaCheckBaobabCounter(PO.serpFooter.searchengines.secondLink(), {
                    path: '/$page/$footer/link[@type="bing"]',
                });

                await browser.yaCheckBaobabCounter(PO.serpFooter.searchengines.thirdLink(), {
                    path: '/$page/$footer/link[@type="mail"]',
                });
            });

            it('Блок поиска в других системах для домена .com.tr', async function() {
                const { browser, PO } = this;

                await browser.yaOpenSerp({
                    text: 'test',
                    tld: 'com.tr',
                    l10n: 'tr',
                    srcskip: 'YABS_DISTR',
                    exp_flags: 'yabs_distr=0',
                    data_filter: 'no_results',
                }, '.b-page');

                await browser.yaShouldNotExist(PO.serpFooter.searchengines());
            });

            [
                { tld: 'by', l10n: 'be' },
                { tld: 'kz', l10n: 'kk' },
                { tld: 'ua', l10n: 'uk' },
                { tld: 'com', lr: 87 },
            ].forEach(({ tld, l10n, lr }) => {
                it(`Блок поиска в других системах для .${tld}`, async function() {
                    const { browser, PO } = this;
                    const query = {
                        text: 'test',
                        tld: tld,
                        data_filter: 'no_results',
                        exp_flags: 'yabs_distr=0',
                    };

                    if (l10n) query.l10n = l10n;
                    if (lr) query.lr = lr;

                    await browser.yaOpenSerp(query, PO.serpFooter.searchengines());
                });
            });
        });

        describe('Обратная связь и справка', function() {
            it('Внешний вид', async function() {
                const { browser, PO } = this;

                await browser.assertView('feedback', PO.serpFooter.help(), { excludeElements: PO.serpFooter() });
            });

            it('Отправка счетчиков для ссылки \'Обратная связь\'', async function() {
                const { browser, PO } = this;

                await browser.yaCheckBaobabCounter(PO.serpFooter.help.feedback(), {
                    path: '/$page/$footer/feedback',
                });
            });

            it('Отправка счетчиков для ссылки \'Справка\'', async function() {
                const { browser, PO } = this;

                await browser.yaCheckBaobabCounter(PO.serpFooter.help.help(), {
                    path: '/$page/$footer/help',
                });
            });
        });

        hermione.only.notIn('searchapp-phone', 'В ПП нет этого поля');
        describe('Настройки', function() {
            it('Внешний вид', async function() {
                const { browser, PO } = this;

                await browser.assertView('settings', PO.serpFooter.settings(), {
                    excludeElements: PO.serpFooter(),
                });
            });

            it('Отправка счетчиков для ссылки \'Настройки\'', async function() {
                const { browser, PO } = this;
                await browser.yaCheckBaobabCounter(PO.serpFooter.settings.link(), {
                    path: '/$page/$footer/settings',
                });
            });
        });
    });

    hermione.also.in('iphone-dark');
    hermione.only.notIn('searchapp-phone');
    it('Пустой поисковый запрос', async function() {
        const { browser, PO } = this;

        await browser.yaOpenSerp({
            text: 'test',
            lr: 213,
            srcskip: 'YABS_DISTR',
            exp_flags: 'yabs_distr=0',
            data_filter: 'no_results',
        }, PO.serpFooter());

        await browser.assertView('plain', PO.serpFooter(), {
            excludeElements: PO.serpFooter(),
            allowViewportOverflow: true,
        });
    });

    it('С флагом pumpkin', async function() {
        const { browser, PO } = this;

        await browser.yaOpenPumpkin({
            text: 'pumpkin',
            lr: 213,
            srcskip: 'YABS_DISTR',
            data_filter: 'no_results',
        });

        await browser.assertView('footer-pumpkin', PO.serpFooter(), {
            excludeElements: PO.serpFooter(),
            allowViewportOverflow: true,
        });
    });
});
