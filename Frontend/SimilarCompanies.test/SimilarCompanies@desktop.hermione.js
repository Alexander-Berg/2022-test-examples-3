'use strict';

const PO = require('./SimilarCompanies.page-object').desktop;

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

            await this.browser.yaCheckBaobabCounter(PO.oneOrg.similarCompanies.scroller.arrowRight(), {
                path: '/$page/$parallel/$result/composite/tabs/about/similar/scroller/scroll_right',
            });
            // иначе иногда срабатывает счетчик скролла (event=scroll на ноде scroller) и тест мигает (dynClicks+1)
            await this.browser.yaWaitUntilElementScrollStopped(PO.oneOrg.similarCompanies.scroller.wrap());
            await this.browser.yaWaitForVisible(
                PO.oneOrg.similarCompanies.scroller.arrowLeft(),
                'Стрелка скролла влево не видна',
            );
            await this.browser.yaCheckBaobabCounter(PO.oneOrg.similarCompanies.scroller.arrowLeft(), {
                path: '/$page/$parallel/$result/composite/tabs/about/similar/scroller/scroll_left',
            });
            await this.browser.yaWaitForVisible(PO.oneOrg.similarCompanies.scroller.firstItem());

            const requestUrl = await this.browser.yaGetLastAjaxDataFast('"request_intent":"one-org"', {
                field: 'url',
            }, () => this.browser.yaCheckBaobabCounter(PO.oneOrg.similarCompanies.scroller.firstItem.link(), {
                path: '/$page/$parallel/$result/composite/tabs/about/similar/scroller/topic/topic[@externalId@entity="organization" and @externalId@id and @oidRk and @pos=0 and @oid and @behaviour@type="dynamic"]',
            }));

            assert.isNotNull(requestUrl, 'В запросе отсутствует request_intent');

            await this.browser.yaWaitForVisible(PO.popup.oneOrg(), 'Попап не открылся');

            await this.browser.yaCheckResultMetrics({
                name: 'orgs',
                place: 'parallel',
                shows: 1,
                extClicks: 0,
                dynClicks: 3,
                requests: 1,
            });
        });

        it('Счетчик показа похожих в 1орг на выдаче', async function() {
            await this.browser
                .yaOpenSerp({
                    text: 'кафе пушкин',
                    srcskip: 'YABS_DISTR',
                    data_filter: 'companies',
                }, PO.oneOrg());

            await this.browser.yaWaitForVisible(PO.oneOrg.similarCompanies(), 'Блок похожих организаций не виден');

            await this.browser.yaScroll(PO.oneOrg.similarCompanies());

            await this.browser.yaCheckBaobabCounter(() => {}, {
                path: '/$page/$parallel/$result/composite/tabs/about/similar/scroller/topic[@externalId@entity="organization" and @externalId@id and @oidRk and @pos=0 and @oid]',
                event: 'tech',
                type: 'topic-show',
            });
        });

        it('Основные проверки в отеле', async function() {
            await this.browser.yaOpenSerp({
                text: 'отель рэдиссон славянская',
                srcskip: 'YABS_DISTR',
                data_filter: 'companies',
            }, PO.hotelOrg());
            await this.browser.yaWaitForVisible(PO.hotelOrg.similarCompanies(), 'Блок похожих организаций не виден');

            await this.browser.assertView('hotel', PO.hotelOrg.similarCompanies());

            await this.browser.yaCheckBaobabCounter(PO.hotelOrg.similarCompanies.scroller.arrowRight(), {
                path: '/$page/$parallel/$result/tabs/about/similar/scroller/scroll_right',
            });
            // иначе иногда срабатывает счетчик скролла (event=scroll на ноде scroller) и тест мигает (dynClicks+1)
            await this.browser.yaWaitUntilElementScrollStopped(PO.hotelOrg.similarCompanies.scroller.wrap());
            await this.browser.yaWaitForVisible(
                PO.hotelOrg.similarCompanies.scroller.arrowLeft(),
                'Стрелка скролла влево не видна',
            );
            await this.browser.yaCheckBaobabCounter(PO.hotelOrg.similarCompanies.scroller.arrowLeft(), {
                path: '/$page/$parallel/$result/tabs/about/similar/scroller/scroll_left',
            });
            await this.browser.yaWaitForVisible(PO.hotelOrg.similarCompanies.scroller.firstItem.link());

            const requestUrl = await this.browser.yaGetLastAjaxDataFast('"request_intent":"one-org"', {
                field: 'url',
            }, () => this.browser.yaCheckBaobabCounter(PO.hotelOrg.similarCompanies.scroller.firstItem.link(), {
                path: '/$page/$parallel/$result/tabs/about/similar/scroller/topic/topic[@externalId@entity="organization" and @externalId@id and @oidRk and @pos=0 and @oid and @offerId and @behaviour@type="dynamic"]',
            }));

            assert.isNotNull(requestUrl, 'В запросе отсутствует request_intent');

            await this.browser.yaWaitForVisible(PO.popup.oneOrg(), 'Попап не открылся');

            await this.browser.yaCheckResultMetrics({
                name: 'orgs',
                place: 'parallel',
                shows: 1,
                extClicks: 0,
                dynClicks: 3,
                requests: 1,
            });
        });

        it('Счетчик показа похожих в отелях на выдаче', async function() {
            await this.browser.yaOpenSerp({
                text: 'отель рэдиссон славянская',
                srcskip: 'YABS_DISTR',
                data_filter: 'companies',
            }, PO.hotelOrg());

            await this.browser.yaWaitForVisible(PO.hotelOrg.similarCompanies(), 'Блок похожих организаций не виден');

            await this.browser.yaScroll(PO.hotelOrg.similarCompanies());

            await this.browser.yaCheckBaobabCounter(() => {}, {
                path: '/$page/$parallel/$result/tabs/about/similar/scroller/topic[@externalId@entity="organization" and @externalId@id and @oidRk and @pos=0 and @oid and @offerId]',
                event: 'tech',
                type: 'topic-show',
            });
        });

        it('Мало данных', async function() {
            await this.browser.yaOpenSerp({
                lr: 54,
                text: 'Ипподром, Нижний Новгород',
                oid: 'b:159282370813',
                'serp-reload-from': 'companies',
                srcskip: 'YABS_DISTR',
                data_filter: 'companies',
            }, PO.oneOrg.similarCompanies());

            await this.browser.click(PO.oneOrg.similarCompanies.scroller.firstItem.link());
            await this.browser.yaWaitForVisible(PO.popup.oneOrg(), 'Попап не показался');
        });
    });

    it('В попапе', async function() {
        await this.browser.yaOpenSerp({
            text: 'lets rock бар',
            srcskip: 'YABS_DISTR',
            data_filter: 'companies',
        }, PO.oneOrg());

        await this.browser.click(PO.oneOrg.similarCompanies.scroller.firstItem());
        await this.browser.yaWaitForVisible(PO.popup.oneOrg());
        await this.browser.yaWaitForVisible(
            PO.popup.oneOrg.similarCompanies(),
            'Блок похожих организаций не виден в попапе',
        );

        await this.browser.yaScroll(PO.popup.oneOrg.similarCompanies());

        await this.browser.assertView('popup', PO.popup.oneOrg.similarCompanies());

        const requestUrl = await this.browser.yaGetLastAjaxDataFast('"request_intent":"one-org"', {
            field: 'url',
        }, () => this.browser.yaCheckBaobabCounter(PO.popup.oneOrg.similarCompanies.scroller.firstItem.link(), {
            path: '/$page/$parallel/$result/modal-popup/modal-content-loader/content/company/tabs/about/similar/scroller/topic/topic[@externalId@entity="organization" and @externalId@id and @oidRk and @pos=0 and @oid and @behaviour@type="dynamic"]',
        }));

        assert.isNotNull(requestUrl, 'В запросе отсутствует request_intent');

        await this.browser.yaWaitForVisible(PO.popup.oneOrg(), 'Попап не открылся');

        const count = await this.browser.yaVisibleCount(PO.popup.oneOrg());
        await assert.equal(count, 1, 'Должен быть только один попап');

        await this.browser.yaCheckResultMetrics({
            name: 'orgs',
            place: 'parallel',
            shows: 1,
            extClicks: 0,
            dynClicks: 2,
            requests: 1,
        });
    });

    it('Счетчик показа похожих в попапе', async function() {
        await this.browser.yaOpenSerp({
            text: 'lets rock бар',
            srcskip: 'YABS_DISTR',
            data_filter: 'companies',
        }, PO.oneOrg());

        await this.browser.click(PO.oneOrg.similarCompanies.scroller.firstItem());
        await this.browser.yaWaitForVisible(PO.popup.oneOrg());
        await this.browser.yaWaitForVisible(
            PO.popup.oneOrg.similarCompanies(),
            'Блок похожих организаций не виден в попапе',
        );

        await this.browser.yaScroll(PO.popup.oneOrg.similarCompanies());

        await this.browser.yaCheckBaobabCounter(() => {}, {
            path: '/$page/$parallel/$result/modal-popup/modal-content-loader/content/company/tabs/about/similar/scroller/topic[@externalId@entity="organization" and @externalId@id and @oidRk and @pos=0 and @oid]',
            event: 'tech',
            type: 'topic-show',
        });
    });

    // Проверка отсутствия бага SERP-142628
    it('Организация без отзывов', async function() {
        await this.browser.yaOpenSerp({
            text: 'istanbul havalimanı аэропорт istanbulkart',
            srcskip: 'YABS_DISTR',
            data_filter: 'companies',
        }, PO.oneOrg());

        await this.browser.click(PO.oneOrg.similarCompanies.scroller.firstItem.link());
        await this.browser.yaWaitForVisible(PO.popup.oneOrg());
    });
});
