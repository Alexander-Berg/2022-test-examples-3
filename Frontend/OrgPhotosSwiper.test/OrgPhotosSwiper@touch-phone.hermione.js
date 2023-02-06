'use strict';

const PO = require('./OrgPhotosSwiper.page-object')['touch-phone'];

specs({
    feature: 'Одна организация',
    type: 'Просмотрщик фото',
}, function() {
    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            text: 'Кафе Пушкин',
            exp_flags: [
                'GEO_1org_photo_swiper=1',
            ],
            srcparams: 'GEOV:experimental=add_snippet=photos/3.x',
            srcskip: 'YABS_DISTR',
            data_filter: 'companies',
        }, PO.oneOrg());
    });

    it('Внешний вид', async function() {
        const { browser } = this;

        await browser.click(PO.oneOrg.tabsMenu.photo());
        await browser.yaWaitForVisible(PO.swiperModal());
        await browser.yaWaitForVisible(PO.swiperModal.photosPage.photo());
        await browser.yaStubImage(`${PO.swiperModal()} .Image`, 610, 100);
        await browser.assertView('plain', PO.swiperModal());
    });

    hermione.only.notIn('iphone', 'iphone не умеет в landscape');
    it('Внешний вид (landscape)', async function() {
        const { browser } = this;

        await browser.click(PO.oneOrg.tabsMenu.photo());
        await browser.yaWaitForVisible(PO.swiperModal());
        await browser.yaWaitForVisible(PO.swiperModal.photosPage.photo());
        await browser.setOrientation('landscape');
        await browser.yaStubImage(`${PO.swiperModal()} .Image`, 610, 100);
        await browser.assertView('landscape', PO.swiperModal());
    });

    describe('Точки входа', async function() {
        it('Таб "Фото"', async function() {
            const { browser } = this;

            await browser.click(PO.oneOrg.tabsMenu.photo());
            await browser.yaWaitForVisible(PO.swiperModal());
        });

        it('Элемент "Все фото {n}" в галерее', async function() {
            const { browser } = this;

            await browser.click(PO.oneOrg.photoTiles.secondItem());
            await browser.yaWaitForVisible(PO.swiperModal());
        });

        it('Тег фото', async function() {
            const { browser } = this;

            await browser.click(PO.oneOrg.photoTiles.thirdItem());
            await browser.yaWaitForVisible(PO.swiperModal());
        });

        it('Стрелка "Все фото"', async function() {
            const { browser } = this;

            await browser.click(PO.oneOrg.photoTiles.more());
            await browser.yaWaitForVisible(PO.swiperModal());
        });
    });

    it('Эксперимент с плиткой', async function() {
        await this.browser.yaOpenSerp({
            text: 'Кафе Пушкин',
            exp_flags: [
                'GEO_1org_photo_swiper=1',
                'GEO_1org_photo_swiper_grid=1',
            ],
            srcparams: 'GEOV:experimental=add_snippet=photos/3.x',
            srcskip: 'YABS_DISTR',
            data_filter: 'companies',
        }, PO.oneOrg());

        const { browser } = this;

        await browser.click(PO.oneOrg.tabsMenu.photo());
        await browser.yaWaitForVisible(PO.swiperModal());
        await browser.yaWaitForVisible(PO.swiperModal.photosPage.photo());
        await browser.yaStubImage(`${PO.swiperModal()} .Image`, 200, 300);
        await browser.assertView('plain', PO.swiperModal());
    });
});
