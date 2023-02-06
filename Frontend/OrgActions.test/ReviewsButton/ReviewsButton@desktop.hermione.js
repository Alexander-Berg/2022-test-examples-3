'use strict';

const url = require('url');
const PO = require('../OrgActions.page-object').desktop;
const POReviewsViewer = require('../../../../../../features/ReviewsViewer/ReviewsViewer.test/ReviewsViewer.page-object/index@desktop');
const POSimilar = require('../../../SimilarCompanies/SimilarCompanies.test/SimilarCompanies.page-object/index@desktop');

specs({
    feature: 'Колдунщик 1орг',
    type: 'Кнопка написания отзыва под галереей',
}, function() {
    describe('Для залогинов', function() {
        it('На выдаче', async function() {
            const { browser } = this;

            await browser.yaOpenSerp({
                text: 'кафе пушкин',
                srcskip: 'YABS_DISTR',
                yandex_login: 'stas.mihailov666',
                data_filter: 'companies',
            }, PO.oneOrg());
            await browser.yaShouldBeVisible(PO.oneOrg.buttons.reviews(), 'Нет кнопки Оставить отзыв под галереей');
            await browser.yaAssertViewExtended('plain', PO.oneOrg.buttons(),
                { horisontalOffset: 15, verticalOffset: 50 });
            await browser.yaCheckBaobabCounter(PO.oneOrg.buttons.reviews(), {
                path: '/$page/$parallel/$result/composite/tabs/about/actions/reviews[@behaviour@type="dynamic"]',
            });
            await browser.yaWaitForVisible(POReviewsViewer.reviewViewerModal(), 'Не открылся попап с отзывами');
        });

        it('В попапе', async function() {
            const { browser } = this;

            await browser.yaOpenSerp({
                text: 'кафе пушкин',
                srcskip: 'YABS_DISTR',
                yandex_login: 'stas.mihailov666',
                data_filter: 'companies',
            }, PO.oneOrg());

            await browser.click(POSimilar.oneOrg.similarCompanies.scroller.firstItem());
            await browser.yaWaitForVisible(PO.modal.oneOrg(), 'попап с 1орг не открылся');
            await browser.yaShouldBeVisible(PO.modal.oneOrg.buttons.reviews(), 'Нет кнопки Оставить отзыв под галереей');
            await browser.yaAssertViewExtended('modal', PO.modal.oneOrg.buttons(),
                { horisontalOffset: 15, verticalOffset: 50 });
            await browser.yaCheckBaobabCounter(PO.modal.oneOrg.buttons.reviews(), {
                path: '/$page/$parallel/$result/modal-popup/modal-content-loader/content/company/tabs/about/actions/reviews[@behaviour@type="dynamic"]',
            });
            await browser.yaWaitForVisible(POReviewsViewer.reviewViewerModal(), 'Не открылся попап с отзывами');
        });
    });

    describe('Для незалогинов', function() {
        it('На выдаче', async function() {
            const { browser } = this;

            await browser.yaOpenSerp({
                text: 'кафе пушкин',
                srcskip: 'YABS_DISTR',
                data_filter: 'companies',
            }, PO.oneOrg());
            await browser.yaShouldBeVisible(PO.oneOrg.buttons.reviews(), 'Нет кнопки Оставить отзыв под галереей');
            await checkPassportLink.call(this, {
                selector: PO.oneOrg.buttons.reviews(),
                origin: 'serp_geo_reviews_btn',
                baobab: {
                    path: '/$page/$parallel/$result/composite/tabs/about/actions/reviews',
                },
                message: 'Ошибка в ссылке на Паспорт',
            });
        });

        it('В попапе', async function() {
            const { browser } = this;

            await browser.yaOpenSerp({
                text: 'кафе пушкин',
                srcskip: 'YABS_DISTR',
                data_filter: 'companies',
            }, PO.oneOrg());

            await browser.click(POSimilar.oneOrg.similarCompanies.scroller.firstItem());
            await browser.yaWaitForVisible(PO.modal.oneOrg(), 'попап с 1орг не открылся');
            await browser.yaShouldBeVisible(PO.modal.oneOrg.buttons.reviews(), 'Нет кнопки Оставить отзыв под галереей');
            await checkPassportLink.call(this, {
                selector: PO.modal.oneOrg.buttons.reviews(),
                origin: 'serp_geo_reviews_btn',
                baobab: {
                    path: '/$page/$parallel/$result/modal-popup/modal-content-loader/content/company/' +
                        'tabs/about/actions/reviews',
                },
                message: 'Ошибка в ссылке на Паспорт',
            });
        });
    });
});

async function checkPassportLink(options) {
    const pageUrl = await this.browser.getUrl();
    const pageUrlParsed = url.parse(pageUrl, true);
    const pageLr = pageUrlParsed.query.lr;

    await this.browser.yaCheckLink2({
        selector: options.selector,
        target: '_self',
        url: {
            href: 'https://passport.yandex.ru/auth?retpath=...&origin=...',
            queryValidator: query => {
                assert.equal(query.origin, options.origin, 'Ошибка в origin в ссылке на Паспорт');
                assert.isDefined(query.retpath, 'Нет retpath в ссылке на Паспорт');

                const retpath = url.parse(query.retpath, true);
                const errorMsg = msg => `${options.message}. Сломан retpath: ${msg}`;

                assert.equal(retpath.pathname, '/search/', errorMsg('pathname не ведет на Серп'));
                assert.equal(retpath.query.intent, 'reviews', errorMsg('intent не ведет на таб отзывов'));
                assert.match(retpath.query.oid, /b:\d+/, errorMsg('oid не похож на oid организации'));
                assert.isDefined(retpath.query.text, errorMsg('нет text'));
                assert.equal(retpath.query.noreask, '1', errorMsg('нет параметра отключения опечаточников'));
                assert.equal(retpath.query.lr, pageLr, errorMsg('lr не совпадает с lr исходной страницы'));

                return true;
            },
        },
        baobab: options.counter,
        message: options.message,
    });
}
