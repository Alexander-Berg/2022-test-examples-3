import {
    createProduct,
    createOfferForProduct,
    mergeState,
    createShopInfo,
    createBusinessInfo,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import {CATEGORIES, NAVNODES} from '@self/platform/spec/hermione/fixtures/product/common';

const getProduct = id => createProduct({
    slug: `slug-${id}`,
    navnodes: NAVNODES,
    categories: CATEGORIES,
    hasExpressOffer: true,
}, id);

const SHOP_ID = 101;
const BUSINESS_ID = 10671581;
const WAREHOUSE_ID = 76772;
const FEED_ID = 112233;

export function createState(resultsCount = 1) {
    const products = new Array(resultsCount).fill(true).map((item, i) => getProduct(i));
    const offers = products.map((product, index) => createOfferForProduct(
        {
            businessId: BUSINESS_ID,
            cpa: 'real',
            delivery: {
                courierOptions: [{price: 149, currency: 'RUR', dayFrom: 0, dayTo: 0}],
                isCourierAvailable: true,
                partnerTypes: ['YANDEX_MARKET'],
                shopPriorityRegion: 213,
                inStock: true,
                isExpress: true,
                options: [
                    {price: 149, currency: 'RUR', dayFrom: 0, dayTo: 0, timeIntervals: [{from: '00:00', to: '23:59', isDefault: true}]},
                ],
                orderBefore: '23',
                orderBeforeMin: '59',
                isDefault: true,
                availableServices: [{serviceId: 100630, serviceName: 'Экспресс-доставка Яндекса'}],
            },
        },
        index,
        WAREHOUSE_ID
    ));

    return mergeState([
        ...offers,
        createShopInfo({
            returnDeliveryAddress: 'hello, there!',
            shopName: 'i Love Home',
            businessId: BUSINESS_ID,
            slug: 'slug',
            feeds: [{warehouseId: WAREHOUSE_ID, feedId: FEED_ID}],
            workScheduleList: [
                {day: 0, from: {hour: 0, minute: 0}, to: {hour: 23, minute: 59}},
                {day: 1, from: {hour: 0, minute: 0}, to: {hour: 23, minute: 59}},
                {day: 2, from: {hour: 0, minute: 0}, to: {hour: 23, minute: 59}},
                {day: 3, from: {hour: 0, minute: 0}, to: {hour: 23, minute: 59}},
                {day: 4, from: {hour: 0, minute: 0}, to: {hour: 23, minute: 59}},
                {day: 5, from: {hour: 0, minute: 0}, to: {hour: 23, minute: 59}},
                {day: 6, from: {hour: 0, minute: 0}, to: {hour: 23, minute: 59}},
            ],
        }, SHOP_ID),
        createBusinessInfo({
            business_name: 'I Love Home',
            shop_name: 'I Love Home',
            shop_info: [{
                shop_id: SHOP_ID,
                supplier_type: 1,
            }],
        }, BUSINESS_ID),
        {
            data: {
                search: {
                    totalOffersBeforeFilters: resultsCount * 2,
                    total: resultsCount * 2,
                },
                intents: [{
                    defaultOrder: 1,
                    ownCount: 52,
                    relevance: -0.515724,
                    category: {
                        name: 'Мобильные телефоны',
                        slug: 'mobilnye-telefony',
                        uniqName: 'Мобильные телефоны',
                        hid: 91491,
                        nid: 54726,
                        isLeaf: true,
                        view: 'list',
                    },
                }],
                expressWarehouses: {
                    compressed: 'someExtraLongHash',
                },
            },
        },
    ]);
}
