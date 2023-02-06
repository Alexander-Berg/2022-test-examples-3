'use strict';

const KeypointsPO = require('./Keypoints.page-object/index@common');

specs({
    feature: 'Колдунщик видео',
    type: 'XL',
    experiment: 'Keypoints',
}, function() {
    describe('expandable', async function() {
        beforeEach(async function() {
            const PO = this.PO;

            await this.browser.yaOpenSerp({
                text: 'тест батарей смартфонов',
                data_filter: 'videowiz',
                foreverdata: '451120279',
                exp_flags: [
                    'video_wizard_keypoints_xl_expandable=1',
                ],
            }, PO.videoWizard());
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид', async function() {
            const PO = this.PO;

            await this.browser.assertView('collapsed-view', [
                PO.videoWizard.showcase.item.thumb(),
                KeypointsPO.Keypoints(),
            ]);
            await this.browser.click(KeypointsPO.Keypoints.Title());
            await this.browser.assertView('expanded-view', [
                PO.videoWizard.showcase.item.thumb(),
                KeypointsPO.Keypoints(),
            ]);

            const href = await this.browser.getAttribute(KeypointsPO.Keypoints.SecondItem(), 'href');

            const t = new URL(href).searchParams.get('t');

            return assert(t, 'В ссылке на кейпоинте отсутствует параметр &t=');
        });

        hermione.only.notIn('searchapp-phone');
        hermione.also.in('iphone-dark');
        it('Скролл', async function() {
            const PO = this.PO;

            await this.browser.yaHideHeader();

            await this.browser.click(KeypointsPO.Keypoints.Title());

            await this.browser.execute(function(listInner, itemSelector) {
                let list = document.querySelector(listInner);
                let item = document.querySelector(itemSelector);

                list.scrollTo(item.offsetLeft, 0);
            }, KeypointsPO.Keypoints.ListInner(), KeypointsPO.Keypoints.SecondItem());

            await this.browser.yaCheckBaobabCounter(() => {}, [
                { // Клик в тайтл
                    path: '/$page/$main/$result/showcase/item/keypoints/title',
                    data: { expand: true },
                    fast: { wzrd: 'video-unisearch', subtype: 'xl' },
                },
                { // Показ кейпоинтов
                    path: '/$page/$main/$result/showcase/item/keypoints/items',
                    event: 'tech',
                    type: 'show',
                    data: { expandable: true, count: 6 },
                    fast: { wzrd: 'video-unisearch', subtype: 'xl' },
                },
                { // Скролл
                    path: '/$page/$main/$result/showcase/item/keypoints/items',
                    event: 'tech',
                    type: 'scroll',
                    data: { pos: 0 }, // тут ещё visibleCount: 3, но на iphone их 4 :/
                    fast: { wzrd: 'video-unisearch', subtype: 'xl' },
                },
            ]);

            await this.browser.yaClientBaobabCounterShouldNotBeTriggered(() => {}, {
                path: '/$page/$main/$result/showcase/item/keypoints/items',
                event: 'tech',
                type: 'scroll',
                data: { visibleCount: 3, pos: 2 },
                fast: { wzrd: 'video-unisearch', subtype: 'xl' },
            });

            await this.browser.execute(function(selector) {
                let list = document.querySelector(selector);
                list && list.scrollTo(1000, 0);
            }, KeypointsPO.Keypoints.ListInner());
            await this.browser.assertView('scrolled-view', [
                PO.videoWizard.showcase.item.thumb(),
                KeypointsPO.Keypoints(),
            ]);
        });

        it('Счётчики', async function() {
            await this.browser.click(KeypointsPO.Keypoints.Title());
            await this.browser.click(KeypointsPO.Keypoints.ThirdItem(), { leavePage: false });
            await this.browser.click(KeypointsPO.Keypoints.Title());

            await this.browser.yaCheckBaobabCounter(() => {}, [
                { // Клик в тайтл
                    path: '/$page/$main/$result/showcase/item/keypoints/title',
                    data: { expand: true },
                    fast: { wzrd: 'video-unisearch', subtype: 'xl' },
                },
                { // Показ кейпоинтов
                    path: '/$page/$main/$result/showcase/item/keypoints/items',
                    event: 'tech',
                    type: 'show',
                    data: { expandable: true, count: 6 },
                    fast: { wzrd: 'video-unisearch', subtype: 'xl' },
                },
                { // Клик в кейпоинт
                    path: '/$page/$main/$result/showcase/item/keypoints/items',
                    data: { index: 2, count: 6 },
                    fast: { wzrd: 'video-unisearch', subtype: 'xl' },
                },
                { // Клик в тайтл
                    path: '/$page/$main/$result/showcase/item/keypoints/title',
                    data: { expand: false },
                    fast: { wzrd: 'video-unisearch', subtype: 'xl' },
                },
            ]);
        });
    });

    describe('not expandable', async function() {
        beforeEach(async function() {
            const PO = this.PO;

            await this.browser.yaOpenSerp({
                text: 'тест батарей смартфонов',
                data_filter: 'videowiz',
                foreverdata: '451120279',
            }, PO.videoWizard());
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид', async function() {
            const PO = this.PO;

            await this.browser.assertView('common-view', [
                PO.videoWizard.showcase.item.thumb(),
                KeypointsPO.Keypoints(),
            ]);

            const href = await this.browser.getAttribute(KeypointsPO.Keypoints.SecondItem(), 'href');

            const t = new URL(href).searchParams.get('t');

            return assert(t, 'В ссылке на кейпоинте отсутствует параметр &t=');
        });

        hermione.only.notIn('searchapp-phone');
        hermione.also.in('iphone-dark');
        it('Скролл', async function() {
            const PO = this.PO;

            await this.browser.execute(function(selector) {
                let list = document.querySelector(selector);
                list && list.scrollTo(1000, 0);
            }, KeypointsPO.Keypoints.ListInner());
            await this.browser.assertView('scrolled-view', [
                PO.videoWizard.showcase.item.thumb(),
                KeypointsPO.Keypoints(),
            ]);
        });
    });
});
