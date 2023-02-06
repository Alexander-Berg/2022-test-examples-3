'use strict';

const PO = require('./Traffic.page-object')('touch-phone');

function getQueryValidator() {
    return query =>
        query.utm_source === 'serp' &&
        query.utm_medium === 'maps-touch-phone' &&
        query.portal === '0';
}

specs('Колдунщик пробок', function() {
    describe('Для города без баллов', function() {
        const queryText = 'пробки в туапсе';
        const urlPath = '/maps/1058/tuapse/probki';

        beforeEach(async function() {
            await this.browser.yaOpenSerp({ text: queryText, data_filter: 'traffic' }, PO.traffic());
        });

        hermione.also.in('safari13');
        it('Проверка внешнего вида', async function() {
            await this.browser.yaShouldNotExist(PO.traffic.semaphore(), 'В колдунщике пробок появился светофор');

            await this.browser.yaShouldNotExist(
                PO.traffic.status(),
                'В колдунщике пробок появилось время с описанием пробок',
            );

            await this.browser.assertView('plain', PO.traffic(), { hideElements: PO.traffic.map.pane() });
        });

        it('Проверка ссылок и счётчиков', async function() {
            const queryValidator = getQueryValidator();

            await this.browser.yaCheckLink2({
                selector: PO.traffic.map.link(),
                message: 'Неверная ссылка на карте',
                url: {
                    href: {
                        url: 'https://yandex.ru' + urlPath,
                        queryValidator,
                    },
                    ignore: ['protocol', 'hostname'],
                },
                baobab: {
                    path: '/$page/$main/$result/map/map',
                },
            });

            await this.browser.yaCheckLink2({
                selector: PO.traffic.map.expand(),
                message: 'Неверная ссылка в кнопке расхлопа',
                url: {
                    href: {
                        url: 'https://yandex.ru' + urlPath,
                        queryValidator,
                    },
                    ignore: ['protocol', 'hostname'],
                },
                baobab: {
                    path: '/$page/$main/$result/map/open',
                },
            });

            await this.browser.yaCheckResultMetrics({
                name: 'maps',
                place: 'main',

                shows: 1,
                extClicks: 2,
                dynClicks: 0,

                allClicks: 2,
                allExtClicks: 2,
                allDynClicks: 0,

                allMainClicks: 2,
                allMainExtClicks: 2,
                allMainDynClicks: 0,

                allParallelClicks: 0,
                allParallelDynClicks: 0,
                allParallelExtClicks: 0,

                miscClicks: 0,
                requests: 1,
                sessions: 1,
            });
        });
    });

    it('Для города c 0 баллов', async function() {
        await this.browser.yaOpenSerp({ foreverdata: '1997556160', data_filter: 'traffic' }, PO.traffic());
        await this.browser.yaShouldBeVisible(PO.traffic.semaphore(), 'В колдунщике пробок отсутствует светофор');

        await this.browser.yaShouldBeVisible(
            PO.traffic.status(),
            'В колдунщике пробок отсутствует время с описанием пробок',
        );

        await this.browser.assertView('plain', PO.traffic(), { hideElements: PO.traffic.map.pane() });
    });

    describe('Для города с баллами', function() {
        const queryText = 'пробки в москве';
        const urlPath = '/maps/213/moscow/probki';

        beforeEach(async function() {
            await this.browser.yaOpenSerp(
                { text: queryText, data_filter: 'traffic' },
                [PO.traffic(), PO.traffic.semaphore(), PO.traffic.status()],
            );
        });

        it('Проверка внешнего вида', async function() {
            await this.browser.assertView('plain', PO.traffic(), { hideElements: PO.traffic.map.pane() });
        });

        it('Проверка ссылок и счётчиков', async function() {
            const queryValidator = getQueryValidator();

            await this.browser.yaCheckLink2({
                selector: PO.traffic.map.link(),
                message: 'Неверная ссылка на карте',
                url: {
                    href: {
                        url: 'https://yandex.ru' + urlPath,
                        queryValidator,
                    },
                    ignore: ['protocol', 'hostname'],
                },
                baobab: {
                    path: '/$page/$main/$result/map/map',
                },
            });

            await this.browser.yaCheckLink2({
                selector: PO.traffic.map.expand(),
                message: 'Неверная ссылка в кнопке расхлопа',
                url: {
                    href: {
                        url: 'https://yandex.ru' + urlPath,
                        queryValidator,
                    },
                    ignore: ['protocol', 'hostname'],
                },
                baobab: {
                    path: '/$page/$main/$result/map/open',
                },
            });

            await this.browser.yaCheckResultMetrics({
                name: 'maps',
                place: 'main',

                shows: 1,
                extClicks: 2,
                dynClicks: 0,

                allClicks: 2,
                allExtClicks: 2,
                allDynClicks: 0,

                allMainClicks: 2,
                allMainExtClicks: 2,
                allMainDynClicks: 0,

                allParallelClicks: 0,
                allParallelDynClicks: 0,
                allParallelExtClicks: 0,

                miscClicks: 0,
                requests: 1,
                sessions: 1,
            });
        });
    });
});
