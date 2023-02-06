'use strict';

const PO = require('./SimilarCompanies.page-object').touchPhone;

specs({
    feature: 'Одна организация',
    type: 'Похожие организации',
}, function() {
    describe('На выдаче', function() {
        it('Основные проверки в 1орге', async function() {
            await this.browser.yaOpenSerp({
                text: 'кафе пушкин',
                srcskip: 'YABS_DISTR',
                data_filter: 'companies',
            }, PO.oneOrg());

            await this.browser.yaWaitForVisible(PO.oneOrg.similarCompanies(), 'Блок похожих организаций не виден');

            await this.browser.assertView('plain', PO.oneOrg.similarCompanies());

            await this.browser.yaCheckBaobabCounter(
                () => this.browser.yaScrollContainer(PO.oneOrg.similarCompanies.scroller.wrap(), 500), {
                    path: '/$page/$main/$result/composite/similar/scroller',
                    event: 'scroll',
                    behaviour: { type: 'dynamic' },
                },
            );

            await this.browser.yaOpenOverlayAjax(
                () => this.browser.yaCheckBaobabCounter(PO.oneOrg.similarCompanies.scroller.firstItem.link(), {
                    path: '/$page/$main/$result/composite/similar/scroller/topic/topic[@externalId@entity="organization" and @externalId@id and @oidRk and @pos=0 and @oid and @overlayTrigger=true and @behaviour@type="dynamic"]',
                }),
                PO.overlayOneOrg.similarCompanies(),
                'Сайдблок с карточкой организации не появился',
            );

            await this.browser.yaCheckResultMetrics({
                name: 'orgs',
                place: 'main',
                shows: 1,
                extClicks: 0,
                dynClicks: 3,
                requests: 1,
            });
        });

        hermione.only.notIn('searchapp-phone', 'Реализация отправки счетчиков написана с использованием Intersection Observer API.');
        it('Счетчик показа похожих в 1орг на выдаче', async function() {
            await this.browser
                .yaOpenSerp({
                    text: 'кафе пушкин',
                    srcskip: 'YABS_DISTR',
                    data_filter: 'companies',
                }, PO.oneOrg());

            await this.browser.yaWaitForVisible(PO.oneOrg.similarCompanies(), 'Блок похожих организаций не виден');

            await this.browser.yaScroll(PO.oneOrg.similarCompanies.scroller.firstItem());

            await this.browser.yaCheckBaobabCounter(() => {}, {
                path: '/$page/$main/$result/composite/similar/scroller/topic[@externalId@entity="organization" and @externalId@id and @oidRk and @pos=0 and @oid]',
                event: 'tech',
                type: 'topic-show',
            });
        });

        it('Основные проверки в отеле', async function() {
            await this.browser.yaOpenSerp({
                text: 'мета москва',
                srcskip: 'YABS_DISTR',
                data_filter: 'companies',
            }, PO.oneOrg());

            await this.browser.yaWaitForVisible(PO.oneOrg.similarCompanies(), 'Блок похожих организаций не виден');

            await this.browser.assertView('hotel', PO.oneOrg.similarCompanies());

            await this.browser.yaCheckBaobabCounter(
                () => this.browser.yaScrollContainer(PO.oneOrg.similarCompanies.scroller.wrap(), 500), {
                    path: '/$page/$main/$result/composite/similar/scroller',
                    event: 'scroll',
                    behaviour: { type: 'dynamic' },
                },
            );

            await this.browser.yaOpenOverlayAjax(
                () => this.browser.yaCheckBaobabCounter(PO.oneOrg.similarCompanies.scroller.firstItem.link(), {
                    path: '/$page/$main/$result/composite/similar/scroller/topic/topic[@externalId@entity="organization" and @externalId@id and @oidRk and @pos=0 and @offerId and @oid and @overlayTrigger=true and @behaviour@type="dynamic"]',
                }),
                PO.overlayOneOrg.similarCompanies(),
                'Сайдблок с карточкой организации не появился',
            );

            await this.browser.yaCheckResultMetrics({
                name: 'orgs',
                place: 'main',
                shows: 1,
                extClicks: 0,
                dynClicks: 3,
                requests: 1,
            });
        });

        hermione.only.notIn('searchapp-phone', 'Реализация отправки счетчиков написана с использованием Intersection Observer API.');
        it('Счетчик показа похожих в отелях на выдаче', async function() {
            await this.browser.yaOpenSerp({
                text: 'отель хаятт екатеринбург',
                srcskip: 'YABS_DISTR',
                data_filter: 'companies',
            }, PO.oneOrg());

            await this.browser.yaWaitForVisible(PO.oneOrg.similarCompanies(), 'Блок похожих организаций не виден');

            await this.browser.yaScroll(PO.oneOrg.similarCompanies.scroller.firstItem());

            await this.browser.yaCheckBaobabCounter(() => {}, {
                path: '/$page/$main/$result/composite/similar/scroller/topic[@externalId@entity="organization" and @externalId@id and @oidRk and @pos=0 and @offerId and @oid]',
                event: 'tech',
                type: 'topic-show',
            });
        });
    });

    it('В оверлее', async function() {
        await this.browser.yaOpenSerp({
            text: 'кафе пушкин',
            srcskip: 'YABS_DISTR',
            data_filter: 'companies',
        }, PO.oneOrg());

        await this.browser.yaOpenOverlayAjax(
            PO.oneOrg.tabsMenu.about(),
            PO.overlayOneOrg(),
            'Оверлей не показался',
        );

        await this.browser.yaWaitForVisible(PO.overlayOneOrg.similarCompanies(), 'Блок похожих организаций не показался в оверлее');

        // Из-за гистограммы и отзывов меняется высота и не получается нормально доскроллить
        // ждем пока они загрузятся
        await this.browser.yaWaitForVisible(PO.overlayOneOrg.visitsHistogram(), 'Блок гистограммы не показался в оверлее');
        await this.browser.yaWaitForVisible(PO.overlayOneOrg.reviewsList(), 'Блок отзывов не показался в оверлее');

        await this.browser.yaScrollOverlay(PO.overlayOneOrg.similarCompanies());

        await this.browser.assertView('overlay', PO.overlayOneOrg.similarCompanies());

        await this.browser.yaOpenOverlayAjax(
            () => this.browser.yaCheckBaobabCounter(PO.overlayOneOrg.similarCompanies.scroller.firstItem.link(), {
                path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/about/side-block-bcard/composite/similar/scroller/topic/topic[@externalId@entity="organization" and @externalId@id and @oidRk and @pos=0 and @oid and @overlayTrigger=true and @behaviour@type="dynamic"]',
            }),
            PO.overlayOneOrg(),
            'Оверлей с выбранной организацией не открылся',
        );

        await this.browser.yaCheckResultMetrics({
            name: 'orgs',
            place: 'main',
            shows: 1,
            extClicks: 0,
            dynClicks: 2,
            requests: 1,
        });
    });

    hermione.only.notIn('searchapp-phone', 'Реализация отправки счетчиков написана с использованием Intersection Observer API.');
    it('Счетчик показа похожих в оверлее', async function() {
        await this.browser.yaOpenSerp({
            text: 'кафе пушкин',
            srcskip: 'YABS_DISTR',
            data_filter: 'companies',
        }, PO.oneOrg());

        await this.browser.yaOpenOverlayAjax(
            PO.oneOrg.tabsMenu.about(),
            PO.overlayOneOrg(),
            'Оверлей не показался',
        );

        await this.browser.yaWaitForVisible(PO.overlayOneOrg.similarCompanies(), 'Блок похожих организаций не виден');

        await this.browser.yaScroll(PO.overlayOneOrg.similarCompanies.scroller.firstItem());

        await this.browser.yaCheckBaobabCounter(() => {}, {
            path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/about/side-block-bcard/composite/similar/scroller/topic[@externalId@entity="organization" and @externalId@id and @oidRk and @pos=0 and @oid]',
            event: 'tech',
            type: 'topic-show',
        });
    });
});
