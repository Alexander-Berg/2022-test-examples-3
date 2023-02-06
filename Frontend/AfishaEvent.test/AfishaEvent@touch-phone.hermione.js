'use strict';

const PO = require('./AfishaEvent.page-object');

const href = 'https://afisha.yandex.ru/moscow/concert/artik-asti-msk';

specs(
    {
        feature: 'Афиша события (touch-phone)',
        experiment: 'Колдунщик на реакте',
    },
    function() {
        function openSerp(browser, text) {
            return browser.yaOpenSerp({
                text,
                ls: 213,
                rearr: [
                    'scheme_Local/PrettySerpFeatures/ForbiddenFeatures/afisha_events_landings=1',
                    'scheme_Local/PrettySerpFeatures/ForcedFeatures/afisha_events_testing_landings=1',
                ],
                data_filter: 'afisha-event',
            }, PO.afishaEvent());
        }

        it('Проверка счётчиков', async function() {
            await openSerp(this.browser, 'артик и асти');

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

            await this.browser.yaCheckBaobabCounter(PO.afishaEvent.button(), {
                path: '/$page/$main/$result/afisha-event/ticket',
                behaviour: { type: 'dynamic' },
            });

            await this.browser.yaCheckMetrics({
                'web.total_request_count': 1,
                'web.total_click_count': 3,
                'web.total_all_click': 4,
                'web.total_dynamic_click_count': 1,
            });
        });

        it('Внешний вид (продажа билетов)', async function() {
            await openSerp(this.browser, 'артик и асти');
            await this.browser.assertView('concert', PO.afishaEvent());
        });

        hermione.also.in('safari13');
        it('Внешний вид (продажа билетов - театр)', async function() {
            await openSerp(this.browser, 'про федота стрельца');
            await this.browser.assertView('theatre', PO.afishaEvent());
        });

        it('Внешний вид (билетов нет)', async function() {
            await openSerp(this.browser, 'барокко гоголь центр');
            await this.browser.assertView('no_tickets', PO.afishaEvent());
        });
    },
);
