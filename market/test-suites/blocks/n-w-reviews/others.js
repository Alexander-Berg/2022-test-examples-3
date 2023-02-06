import {makeSuite, makeCase} from 'ginny';

import ShopReviewsList from '@self/platform/spec/page-objects/widgets/content/ShopReviewsList';

import {DEFAULT_USER_UID, createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';

const SHOP_ID = 774;
const OLD_REVIEWS_COUNT = 2;

function createShopOpinion(opinion = {}, id) {
    return {
        id: String(id),
        type: 0,
        shop: {
            id: SHOP_ID,
        },
        cpa: true,
        region: {
            entity: 'region',
            id: 213,
            name: 'Москва',
        },
        user: {
            uid: DEFAULT_USER_UID,
        },
        ...opinion,
    };
}

/**
 * Блок нерекомендованных отзывов -- был раньше
 * Теперь это просто разделитель "Отзывы старше 3 месяцев" в списке отзывов
 *
 * @param {PageObject.ShopReviewsList} reviews
 */
export default makeSuite('Разделитель "Отзывы старше 3 месяцев".', {
    environment: 'kadavr',
    feature: 'Структура страницы',
    story: {
        async beforeEach() {
            await this.setPageObjects({
                reviews: () => this.createPageObject(ShopReviewsList),
            });
        },
        'Отзывов нет.': {
            async beforeEach() {
                await this.browser.yaOpenPage('market:shop-reviews', {shopId: SHOP_ID});
            },
            'Разделитель не выводится.': makeCase({
                id: 'marketfront-2603',
                issue: 'MARKETVERSTKA-29510',
                async test() {
                    await this.reviews.isThreeMonthSeparatorVisible()
                        .should.eventually.equal(false, 'Разделителя нет.');
                },
            }),
        },
        'Имеется два отзыва старше 3 месяцев': {
            async beforeEach() {
                const opinion = {
                    shop: {id: SHOP_ID},
                    user: {uid: DEFAULT_USER_UID},
                    cpa: false,
                    averageGrade: 4,
                    resolved: 0,
                    created: Date.now(),
                };

                const oldOpinion = {
                    shop: {id: SHOP_ID},
                    user: {uid: DEFAULT_USER_UID},
                    cpa: false,
                    averageGrade: 4,
                    resolved: 0,
                    // отзывы старше 3 месяцев (4 месяца назад)
                    created: Date.now() - (4 * 30 * 24 * 60 * 60 * 1000),
                };

                await this.browser.setState('schema', {
                    users: [createUser()],
                    modelOpinions: [
                        createShopOpinion(opinion, 1),
                        createShopOpinion(opinion, 2),
                        createShopOpinion(oldOpinion, 3),
                        createShopOpinion(oldOpinion, 4),
                    ],
                });

                await this.browser.yaOpenPage('market:shop-reviews', {shopId: SHOP_ID});
            },
            'разделитель содержит верный заголовок.': makeCase({
                id: 'marketfront-2604',
                issue: 'MARKETVERSTKA-29511',
                async test() {
                    await this.reviews.getThreeMonthSeparatorTitleText()
                        .should.eventually.equal('Отзывы старше 3 месяцев', 'надпись верна');
                },
            }),
            'показывается правильное количество отзывов после разделителя.': makeCase({
                id: 'marketfront-2604',
                issue: 'MARKETVERSTKA-29511',
                async test() {
                    await this.reviews.getThreeMonthOlderReviewsCount()
                        .should.eventually.equal(
                            OLD_REVIEWS_COUNT,
                            'Правильное количество отзывов старше 3 месяцев'
                        );
                },
            }),
        },
    },
});
