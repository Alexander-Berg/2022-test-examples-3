'use strict';

const PO = require('./OrgPriceSwiper.page-object')['touch-phone'];

specs({
    feature: 'Одна организация',
    type: 'Свайпер прайсов',
}, function() {
    hermione.also.in('iphone-dark');
    describe('Прайсы', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'Sharsky, Москва',
                exp_flags: 'GEO_1org_prices_swiper=1',
                srcskip: 'YABS_DISTR',
                data_filter: 'companies',
            }, PO.oneOrg.orgPrices());
        });

        it('Клик в таб', async function() {
            const { browser } = this;

            await browser.yaCheckBaobabCounter(PO.oneOrg.tabsMenu.prices(), {
                path: '/$page/$main/$result/composite/tabs/controls/prices',
            });
            await browser.yaWaitForVisible(PO.swiperModal(), 'Свайпер не открылся');
            await browser.assertView('plain', PO.swiperModal());

            await browser.yaCheckBaobabCounter(PO.swiperModal.categories.secondItem(), {
                path: '/$page/$main/$result/composite/orgs-price-swiper/swiper-modal/swiper/category',
            });
        });

        it('Клик в тайтл врезки', async function() {
            const { browser } = this;

            await browser.yaCheckBaobabCounter(PO.oneOrg.orgPrices.title(), {
                path: '/$page/$main/$result/composite/prices/title',
            });
            await browser.yaWaitForVisible(PO.swiperModal(), 'Свайпер не открылся');
        });

        it('Клик в "Смотреть все цены"', async function() {
            const { browser } = this;

            await browser.yaCheckBaobabCounter(PO.oneOrg.orgPrices.more(), {
                path: '/$page/$main/$result/composite/prices/more',
            });
            await browser.yaWaitForVisible(PO.swiperModal(), 'Свайпер не открылся');
        });

        it('В сайдблоке', async function() {
            const { browser } = this;

            await browser.click(PO.oneOrg.tabsMenu.about());
            await browser.yaWaitForVisible(PO.overlayOneOrg(), 'Оверлей не открылся');

            await browser.yaCheckBaobabCounter(PO.overlayOneOrg.tabsMenu.prices(), {
                path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/controls/prices',
            });
            await browser.yaWaitForVisible(PO.swiperModal(), 'Свайпер не открылся');
            await browser.assertView('plain', PO.swiperModal());

            await browser.yaCheckBaobabCounter(PO.swiperModal.categories.secondItem(), {
                path: '/$subresult/overlay-wrapper/content/tabs/controls/orgs-price-swiper/swiper-modal/swiper/category',
            });
        });
    });

    it('Меню', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'кафе Пушкин',
            exp_flags: 'GEO_1org_prices_swiper=1',
            srcskip: 'YABS_DISTR',
            data_filter: 'companies',
        }, PO.oneOrg.orgPrices());

        await browser.click(PO.oneOrg.tabsMenu.prices());
        await browser.yaWaitForVisible(PO.swiperModal(), 'Свайпер не открылся');
        await browser.assertView('plain', PO.swiperModal());
    });
});
