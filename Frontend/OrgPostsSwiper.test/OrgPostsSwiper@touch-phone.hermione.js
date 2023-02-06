'use strict';

const PO = require('./OrgPostsSwiper.page-object')['touch-phone'];

specs({
    feature: 'Одна организация',
    type: 'Свайпер новостей',
}, function() {
    hermione.also.in('iphone-dark');
    it('Основные проверки', async function() {
        const { browser } = this;

        await this.browser.yaOpenSerp({
            text: 'ателье лига джентльменов алтайский край',
            exp_flags: 'GEO_1org_posts_swiper=1',
            srcskip: 'YABS_DISTR',
            data_filter: 'companies',
        }, PO.oneOrg());

        await browser.yaCheckBaobabCounter(PO.oneOrg.tabsMenu.posts(), {
            path: '/$page/$main/$result/composite/tabs/controls/posts[@behaviour@type="dynamic"]',
        });
        await browser.yaWaitForVisible(PO.swiperModal(), 'Свайпер не открылся');
        await browser.yaWaitForVisible(PO.swiperModal.postsSwiperPage(), 'Новости не загрузились');
        await browser.assertView('plain', PO.swiperModal());

        await this.browser.yaShouldNotExist(PO.swiperModal.postsSwiperPage.sixthItem(), 'Должно быть только 5 постов');

        await browser.yaCheckBaobabCounter(PO.swiperModal.postsSwiperPage.fourthItem.more(), {
            path: '/$page/$main/$result/composite/orgs-posts-swiper/swiper-modal/swiper/posts-swiper-page/more[@behaviour@type="dynamic"]',
        });
        await browser.yaWaitForHidden(PO.swiperModal.postsSwiperPage.fourthItem.more(), 'Кнопка "Читать еще" не скрылась');
        await browser.yaWaitForVisible(PO.swiperModal.postsSwiperPage.fourthItem.textLink(), 'Ссылка в тексте не появилась');

        await browser.yaCheckLink2({
            selector: PO.swiperModal.postsSwiperPage.fourthItem.textLink(),
            baobab: {
                path: '/$page/$main/$result/composite/orgs-posts-swiper/swiper-modal/swiper/posts-swiper-page/link',
            },
            message: 'Сломана ссылка в посте',
        });
    });

    describe('Открытие из врезки в главном табе', async function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'Диди, Москва',
                exp_flags: 'GEO_1org_posts_swiper=1',
                srcskip: 'YABS_DISTR',
                data_filter: 'companies',
            }, PO.oneOrg.postsPreview());
        });

        it('Тайтл', async function() {
            const { browser } = this;

            await browser.yaCheckBaobabCounter(PO.oneOrg.postsPreview.title(), {
                path: '/$page/$main/$result/composite/posts-preview/title',
            });
            await browser.yaWaitForVisible(PO.swiperModal(), 'Свайпер не открылся');
            await browser.yaWaitForVisible(PO.swiperModal.postsSwiperPage(), 'Новости не загрузились');
        });

        it('Элемент списка', async function() {
            const { browser } = this;

            await browser.yaCheckBaobabCounter(PO.oneOrg.postsPreview.firstItemLink(), {
                path: '/$page/$main/$result/composite/posts-preview/item',
            });
            await browser.yaWaitForVisible(PO.swiperModal(), 'Свайпер не открылся');
            await browser.yaWaitForVisible(PO.swiperModal.postsSwiperPage(), 'Новости не загрузились');
        });

        it('В сайдблоке', async function() {
            const { browser } = this;

            await browser.click(PO.oneOrg.tabsMenu.about());
            await browser.yaWaitForVisible(PO.overlayOneOrg(), 'Оверлей не открылся');
            await browser.yaWaitForVisible(PO.overlayOneOrg.postsPreview(), 'В табе главное нет новостей');

            await browser.yaCheckBaobabCounter(PO.overlayOneOrg.postsPreview.title(), {
                path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/about/side-block-bcard/composite/posts-preview/title',
            });
            await browser.yaWaitForVisible(PO.swiperModal(), 'Свайпер не открылся');
            await browser.yaWaitForVisible(PO.swiperModal.postsSwiperPage(), 'Новости не загрузились');
        });
    });
});
