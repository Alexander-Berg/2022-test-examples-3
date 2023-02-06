import {createShopInfo} from '@yandex-market/kadavr/mocks/Report/helpers';
import {routes} from '@self/platform/spec/hermione/configs/routes';

const shopInfo = createShopInfo({
    entity: 'shop',
    id: routes.shop.shopId,
    status: 'actual',
    oldStatus: 'actual',
    slug: 'some',
    ratingToShow: 3.166666667,
    overallGradesCount: 218,
}, routes.shop.shopId);

function buildReview({id, shopId, uid}) {
    return {
        id: id || 1,
        shop: {
            id: shopId,
        },
        created: new Date('2015-01-01').getTime(),
        averageGrade: 2,
        type: 0,
        cpa: true,
        pro: 'z'.repeat(1999),
        contra: 'x'.repeat(1999),
        comment: 'c'.repeat(1999),
        anonymous: 0,
        user: {
            uid,
        },
        photos: null,
        votes: {
            agree: 300,
            reject: 200,
            total: 500,
        },
    };
}

export {
    shopInfo,
    buildReview,
};

