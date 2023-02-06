'use strict';

const PO = require('./AfishaEvent.page-object');

specs(
    {
        feature: 'Афиша события (deskpad)',
        experiment: 'Колдунщик на реакте',
    },
    function() {
        function openSerp(browser, text) {
            return browser.yaOpenSerp({
                text,
                ls: 213,
                srcrwr: 'UPPER:mclay:12778',
                exp_flags: ['GEO_new_afisha_event=1', 'ask_afisha_event_fts_saas=1'],
                data_filter: 'afisha-event',
            }, PO.afishaEvent());
        }

        describe('Продажа билетов', function() {
            const href = 'https://afisha.yandex.ru/moscow/concert/muse-msk';

            beforeEach(async function() {
                await openSerp(this.browser, 'концерт muse в москве');
            });

            it('Внешний вид и проверка заголовка', async function() {
                await this.browser.assertView('concert', PO.afishaEvent());

                await this.browser.yaCheckLink2({
                    selector: PO.afishaEvent.title.Link(),
                    url: { href },
                    target: '_blank',
                    baobab: {
                        path: '/$page/$main/$result/afisha-event/title',
                    },
                });
            });

            it('Гринурл', async function() {
                await this.browser.yaCheckLink2({
                    selector: PO.afishaEvent.greenurl.Link(),
                    url: { href },
                    target: '_blank',
                    baobab: {
                        path: '/$page/$main/$result/afisha-event/path/urlnav',
                    },
                });
            });

            it('Тумба и кнопка', async function() {
                await this.browser.yaCheckLink2({
                    selector: PO.afishaEvent.thumb(),
                    url: { href },
                    target: '_blank',
                    baobab: {
                        path: '/$page/$main/$result/afisha-event/thumb',
                    },
                });

                await this.browser.yaCheckBaobabCounter(PO.afishaEvent.button(), {
                    path: '/$page/$main/$result/afisha-event/ticket',
                });
            });
        });

        it('Внешний вид (продажа билетов - театр)', async function() {
            await openSerp(this.browser, 'балет щелкунчик в москве');
            await this.browser.assertView('theatre', PO.afishaEvent());
        });

        it('Внешний вид и проверка счётчиков (билетов нет)', async function() {
            const href = 'https://afisha.yandex.ru/moscow/excursions/vserossiiskii-poniforum';
            await openSerp(this.browser, 'всероссийский понифорум в москве');
            await this.browser.assertView('no_tickets', PO.afishaEvent());

            await this.browser.yaCheckLink2({
                selector: PO.afishaEvent.title.Link(),
                url: { href },
                target: '_blank',
                baobab: {
                    path: '/$page/$main/$result/afisha-event/title',
                },
            });

            await this.browser.yaCheckLink2({
                selector: PO.afishaEvent.greenurl.Link(),
                url: { href },
                target: '_blank',
                baobab: {
                    path: '/$page/$main/$result/afisha-event/path/urlnav',
                },
            });

            await this.browser.yaCheckLink2({
                selector: PO.afishaEvent.thumb(),
                url: { href },
                target: '_blank',
                baobab: {
                    path: '/$page/$main/$result/afisha-event/thumb',
                },
            });

            await this.browser.yaCheckLink2({
                selector: PO.afishaEvent.button(),
                url: { href },
                target: '_blank',
                baobab: {
                    path: '/$page/$main/$result/afisha-event/button',
                },
            });

            await this.browser.yaCheckMetrics({
                'web.total_request_count': 1,
                'web.total_click_count': 4,
                'web.total_all_click': 4,
                'web.click_count_overlong_p1_120$': 1,
            });

            // Далее проверяем метрику 'web.click_count_overlong_p1_120$'
            // После перехода на сайт нет событий на серпе = пользователь остался на сайте = 1 длинный клик.
            // А если после возвращения на серп прошло меньше 15 сек, то не должно быть сверхдлинных кликов.
            await this.browser.click(PO.pager.next());

            await this.browser.yaCheckMetrics({
                'web.total_click_count': 4,
                'web.total_all_click': 5,
                'web.click_count_overlong_p1_120$': 0,
            });
        });

        it('Внешний вид (билетов нет - концерт)', async function() {
            await openSerp(this.browser, 'концерт haleo в москве');
            await this.browser.assertView('no_tickets_concert', PO.afishaEvent());
        });

        it('Внешний вид (билетов нет - театр)', async function() {
            await openSerp(this.browser, 'вакханки в москве');
            await this.browser.assertView('no_tickets_theatre', PO.afishaEvent());
        });

        it('Внешний вид (бесплатно)', async function() {
            await openSerp(this.browser, 'Allora & Calzadilla в москве');
            await this.browser.assertView('free', PO.afishaEvent());
        });
    },
);
