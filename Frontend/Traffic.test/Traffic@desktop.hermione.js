'use strict';

let PO;

const geo = require('../../../../hermione/client-scripts/geo');
const map2 = require('../../../../hermione/client-scripts/map2');
const TrafficPO = require('./Traffic.page-object');

specs('Колдунщик пробок', function() {
    beforeEach(function() {
        PO = TrafficPO(this.currentPlatform);

        return this.browser;
    });

    describe('Для любого города', function() {
        it('Проверка внешнего вида для длинного названия города', async function() {
            await this.browser.yaOpenSerp({ foreverdata: '1533230636', data_filter: 'traffic' }, PO.traffic());
            await this.browser.yaWaitUntilMapLoaded(PO.traffic.map());

            await this.browser.assertView(
                'long-title',
                [PO.traffic.title()],
                { hideElements: PO.traffic.map.pane() },
            );
        });
    });

    describe('Для города без баллов', function() {
        it('Общие проверки', async function() {
            await this.browser.yaOpenSerp({ text: 'пробки в туапсе', data_filter: 'traffic' }, PO.traffic());
            await this.browser.yaWaitUntilMapLoaded(PO.traffic.map());
            await this.browser.yaShouldNotExist(PO.traffic.semaphore(), 'В колдунщике пробок появился светофор');

            await this.browser.yaShouldNotExist(
                PO.traffic.status(),
                'В колдунщике пробок появилось время с описанием пробок',
            );

            await this.browser.assertView('plain', PO.traffic(), { hideElements: PO.traffic.map.pane() });
        });
    });

    it('Для города c 0 баллов', async function() {
        await this.browser.yaOpenSerp({ foreverdata: '1997556160', data_filter: 'traffic' }, PO.traffic());
        await this.browser.yaWaitUntilMapLoaded(PO.traffic.map());
        await this.browser.yaShouldBeVisible(PO.traffic.semaphore(), 'В колдунщике пробок отсутствует светофор');

        await this.browser.yaShouldBeVisible(
            PO.traffic.status(),
            'В колдунщике пробок отсутствует время с описанием пробок',
        );

        await this.browser.assertView('plain', PO.traffic(), { hideElements: PO.traffic.map.pane() });
    });

    describe('Для города с баллами', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp(
                { text: 'пробки в москве', data_filter: 'traffic' },
                [PO.traffic(), PO.traffic.semaphore(), PO.traffic.status()],
            );

            await this.browser.yaWaitUntilMapLoaded(PO.traffic.map());
        });

        it('Проверка внешнего вида', async function() {
            await this.browser.assertView('plain', PO.traffic(), { hideElements: PO.traffic.map.pane() });
        });

        it('Проверка ссылок', async function() {
            await this.browser.yaCheckLink2({
                selector: PO.traffic.title.link(),
                url: {
                    href: 'https://yandex.ru/maps/213/moscow/probki?source=traffic',
                    ignore: ['protocol'],
                },
                baobab: {
                    path: '/$page/$main/$result/title',
                },
                message: 'Сломана ссылка на заголовке',
            });

            await this.browser.yaCheckLink2({
                selector: PO.traffic.semaphore.link(),
                url: {
                    href: 'https://yandex.ru/maps/213/moscow/probki?source=traffic',
                    ignore: ['protocol'],
                },
                baobab: {
                    path: '/$page/$main/$result/point',
                },
                message: 'Сломана ссылка с количеством баллов возле светофора',
            });

            await this.browser.yaWaitForVisible(PO.traffic.map.gotomap());

            await this.browser.yaCheckLink2({
                selector: PO.traffic.map.gotomap(),
                url: {
                    href: 'https://yandex.ru/maps/213/moscow/probki?source=traffic',
                    ignore: ['protocol'],
                },
                baobab: {
                    path: '/$page/$main/$result/map/goto-map',
                },
                message: 'Сломана ссылка на кнопке "На большую карту"',
            });
        });

        it('Проверка контролов на карте', async function() {
            let zoomBefore;

            const zoom1 = await this.browser.execute(map2.getZoom, PO.traffic.map());
            zoomBefore = zoom1;

            await this.browser.yaCheckBaobabCounter(PO.traffic.map.zoomin(), {
                path: '/$page/$main/$result/map/round-button[@type="zoomin"]',
                behaviour: { type: 'dynamic' },
            });

            await this.browser.yaWaitUntil('Карта не приблизилась при клике на плюс', async () => {
                const zoom = await this.browser.execute(map2.getZoom, PO.traffic.map());
                return zoomBefore < zoom;
            });

            const zoom2 = await this.browser.execute(map2.getZoom, PO.traffic.map());
            zoomBefore = zoom2;

            await this.browser.yaCheckBaobabCounter(PO.traffic.map.zoomout(), {
                path: '/$page/$main/$result/map/round-button[@type="zoomout"]',
                behaviour: { type: 'dynamic' },
            });

            await this.browser.yaWaitUntil('Карта не отдалилась при клике на минус', async () => {
                const zoom = await this.browser.execute(map2.getZoom, PO.traffic.map());
                return zoomBefore > zoom;
            });

            await this.browser.execute(
                geo.mockGeolocationAndGPSave,
                { latitude: 55.897754, longitude: 37.574169, altitude: 100 },
                { address: 'Yandex Office' },
            );

            await this.browser.yaCheckBaobabCounter(PO.traffic.map.geolocationButton(), {
                path: '/$page/$main/$result/map/round-button[@type="userlocation"]',
                behaviour: { type: 'dynamic' },
            });

            await this.browser.yaWaitUntil('На карте не появилась метка Я', () => {
                return this.browser.execute(map2.isThereYaPin, PO.traffic.map());
            });
        });
    });
});
