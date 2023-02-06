import {createProduct, mergeState, createShopInfo} from '@yandex-market/kadavr/mocks/Report/helpers';

import {CATEGORIES, NAVNODES} from '@self/platform/spec/hermione/fixtures/product/common';

const getProduct = id => createProduct({
    slug: `slug-${id}`,
    navnodes: NAVNODES,
    categories: CATEGORIES,
}, id);

const SHOP_ID = 101;
const BUSINESS_ID = 10671581;

export function createState(resultsCount = 50) {
    const products = new Array(resultsCount).fill(true).map((item, i) => getProduct(i));

    return mergeState([
        ...products,
        createShopInfo({
            returnDeliveryAddress: 'hello, there!',
            shopName: 'i Love Home',
            businessId: BUSINESS_ID,
            slug: 'slug',
        }, SHOP_ID),
        {
            data: {
                search: {
                    totalOffersBeforeFilters: products.length,
                    total: products.length,
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
                    compressed: 'hs',
                },
                businessInfo: {
                    business_id: BUSINESS_ID,
                    business_name: 'i Love Home',
                    shop_name: 'i Love Home',
                    shop_info: [{
                        shop_id: SHOP_ID,
                        supplier_type: 1,
                    }],
                },
            },
        },
    ]);
}
