'use strict';

const PO = require('./OfferReviews.page-object');

specs(
    {
        feature: 'Офферы',
        type: 'Рейтинг в офферах',
    },
    () => {
        const baobabRatingPath = '/$page/$main/$result/offer-reviews/rating';

        it('С расширенным гринурлом', async function() {
            const { browser } = this;

            await browser.yaOpenSerp(
                {
                    text: '',
                    foreverdata: 3754065705,
                    data_filter: 'offer_reviews',
                },
                PO.serpItemWithReviews(),
            );
            await browser.yaShouldBeVisible(PO.serpItem.reviews(), 'Рейтинг не нарисовался');
            await browser.yaCheckBaobabServerCounter({ path: baobabRatingPath });
            await browser.assertView('long-greenurl', PO.serpItem.subtitle());
        });

        it('С коротким гринурлом', async function() {
            const { browser } = this;

            await browser.yaOpenSerp(
                {
                    text: '',
                    foreverdata: 372881226,
                    data_filter: 'offer_reviews',
                },
                PO.serpItemWithReviews(),
            );
            await browser.yaShouldBeVisible(PO.serpItem.reviews(), 'Рейтинг не нарисовался');
            await browser.yaCheckBaobabServerCounter({ path: baobabRatingPath });
            await browser.assertView('short-greenurl', PO.serpItem.subtitle());
        });

        it('Открытие тултипа рейтинга', async function() {
            const { browser } = this;

            await browser.yaOpenSerp(
                {
                    text: '',
                    foreverdata: 372881226,
                    data_filter: 'offer_reviews',
                },
                PO.serpItemWithReviews(),
            );

            await browser.yaShouldBeVisible(PO.serpItem.reviews(), 'Рейтинг не нарисовался');

            await this.browser.moveToObject(PO.serpItem.reviews());
            await browser.yaWaitForVisible(PO.offerReviewsTooltip());

            // Расхлопываем область для захвата всего попапа
            // Захват массива с нужными элементами в некоторых браузерах
            // работает нестабильно, из-за чего падают тесты
            await browser.execute(function(selectors) {
                $(selectors).css({ 'padding-bottom': '50px' });
            }, PO.serpItemWithReviews());
            await browser.assertView('offer-reviews-tooltip', PO.serpItemWithReviews());
            await browser.yaCheckBaobabCounter(() => {}, {
                event: 'tech',
                type: 'offer-reviews-show',
                path: '/$page/$main/$result/offer-reviews',
            });
            await browser.yaCheckBaobabCounter(PO.offerReviewsTooltip.link(), {
                path: '/$page/$main/$result/offer-reviews-tooltip/link',
            });
        });

        it('Доступность', async function() {
            const { browser } = this;

            await browser.yaOpenSerp(
                {
                    text: '',
                    foreverdata: 372881226,
                    data_filter: 'offer_reviews',
                },
                PO.serpItemWithReviews(),
            );
            await browser.yaShouldBeVisible(PO.serpItem.reviews(), 'Рейтинг не нарисовался');

            const reivewsLabel = await browser.yaGetTexts(PO.serpItem.reviews.a11yLabel());
            assert.equal(
                reivewsLabel,
                'Рейтинг: 2,8 из 5',
                'Неверный текст рейтинга для скринридера',
            );

            const reivewsTooltipText = await browser.yaGetTexts(PO.serpItem.reviews.a11yTooltip.text());
            assert.equal(
                reivewsTooltipText,
                'Рейтинг магазина на Яндекс.Маркете',
                'Неверный текст в тултипе рейтинга для скринридера',
            );

            const reivewsTooltipLink = await browser.yaGetTexts(PO.serpItem.reviews.a11yTooltip.link());
            assert.equal(
                reivewsTooltipLink,
                '4 отзыва',
                'Неверный текст ссылки на отзывы для скринридера',
            );

            const reivewsTooltipLinkHref = await browser.getAttribute(PO.serpItem.reviews.a11yTooltip.link(), 'href');
            assert.equal(
                reivewsTooltipLinkHref,
                'https://reviews.yandex.ru/ugcpub/object-digest?otype=Site&app_id=iznanka&ranking=by_relevance&object=%2Fsite%2FZ2FybzI0LnJ1&view=serp',
                'Неверный адрес ссылки на отзывы для скринридера',
            );
        });
    },
);
