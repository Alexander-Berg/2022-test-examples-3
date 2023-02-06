'use strict';

const PO = require('./OfferReviews.page-object');

specs(
    {
        feature: 'Офферы',
        type: 'Рейтинг в офферах',
    },
    () => {
        it('Без отзывов', async function() {
            const { browser } = this;

            await browser.yaOpenSerp(
                {
                    text: 'foreverdata',
                    foreverdata: 3718718170,
                    data_filter: 'offer_reviews',
                },
                PO.serpItemWithReviews(),
            );
            await browser.assertView('no_reviews', PO.serpItem());
            await browser.click(PO.serpItem.extralinks());
            await browser.yaWaitForVisible(PO.extralinksPopup(), 'меню не появилось');
            await browser.assertView('no_reviews_menu', [PO.serpItem(), PO.serpItem.extralinks()], { allowViewportOverflow: true });
        });

        it('С отзывами', async function() {
            const { browser } = this;

            await browser.yaOpenSerp(
                {
                    text: 'foreverdata',
                    foreverdata: 3695339678,
                    data_filter: 'offer_reviews',
                },
                PO.serpItemWithReviews(),
            );
            await browser.assertView('with_reviews', PO.serpItem());
            await browser.click(PO.serpItem.extralinks());
            await browser.yaWaitForVisible(PO.extralinksPopup(), 'меню не появилось');
            await browser.assertView('with_reviews_menu', [PO.serpItem(), PO.serpItem.extralinks()], { allowViewportOverflow: true });
            await browser.yaCheckLink2({
                selector: PO.extralinksPopup.reviews(),
                url: {
                    href: 'https://reviews.yandex.ru',
                    ignore: ['protocol', 'pathname', 'query'],
                },
                target: '_blank',
                baobab: {
                    path: '/$page/$main/$result/extralinks/extralinks-popup/reviews',
                },
                message: 'Ошибка проверки ссылки отзывов',
            });
        });

        it('Первая позиция', async function() {
            const { browser } = this;

            await browser.yaOpenSerp(
                {
                    text: 'foreverdata',
                    exp_flags: [
                        'related-above-enable-for-testing',
                    ],
                    foreverdata: 3405999941,
                },
                PO.serpItemWithReviews(),
            );
            await browser.assertView('before-related', PO.serpItemWithReviews());
            await browser.yaScroll(201);
            await browser.yaScroll(0);
            await browser.yaWaitForVisible(PO.relatedAbove(), 'Переформулировки не появились');
            await browser.assertView('after-related', PO.serpItemWithReviews());
        });

        hermione.only.in('chrome-phone', 'не браузерозависимо');
        it('Доступность', async function() {
            const { browser } = this;

            await browser.deleteCookie('yp');
            await browser.yaOpenSerp(
                {
                    text: 'foreverdata',
                    foreverdata: 3718718170,
                    data_filter: 'offer_reviews',
                },
                PO.serpItemWithReviews(),
            );

            const reivewsLabel = await browser.getAttribute(PO.serpItem.extralinks.offerReviews(), 'aria-label');
            assert.equal(
                reivewsLabel,
                'Рейтинг: 2,8 из 5',
                'Неверный лейбл рейтинга',
            );
        });
    },
);
