import {
    createProduct,
    createOffer,
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';

const SHOP_ID = 1;

const PRODUCT_ID = 88;
const SLUG = 'dobro';

export const ROUTE = {
    slug: SLUG,
    productId: PRODUCT_ID,
};

const POINTS_COUNT = 10;

const PICKUP_OPTIONS = [{
    serviceId: 213,
    serviceName: 'Собственная служба',
    tariffId: 0,
    price: {
        currency: 'RUR',
        value: '100',
    },
    dayFrom: 5,
    dayTo: 5,
    orderBefore: 24,
    groupCount: POINTS_COUNT,
}];

const CATEGORIES = [
    {
        entity: 'category',
        id: 91491,
        name: 'Мобильные телефоны',
        fullName: 'Мобильные телефоны',
        type: 'guru',
        isLeaf: true,
        slug: 'mobilnye-telefony',
    },
];

const NAVNODES = [
    {
        entity: 'navnode',
        id: 54726,
        name: 'Мобильные телефоны',
        slug: 'mobilnye-telefony',
        fullName: 'Мобильные телефоны',
        isLeaf: true,
        rootNavnode: {},
    },
];

const OFFER = {
    showUid: '15423769060781146930200001',
    entity: 'offer',
    shop: {
        id: SHOP_ID,
        slug: 'shop',
        name: 'shop',
        outletsCount: 20,
        storesCount: 20,
    },
    delivery: {
        isPriorityRegion: true,
        isCountrywide: true,
        isAvailable: true,
        hasPickup: true,
        hasLocalStore: false,
        hasPost: false,
        isFake: false,
        availableServices: [
            {
                serviceId: 99,
                serviceName: 'Собственная служба',
            },
        ],
        price: {
            currency: 'RUR',
            value: '0',
            isDeliveryIncluded: false,
        },
        isFree: false,
        isDownloadable: false,
        inStock: true,
        postAvailable: true,
        pickupOptions: [
            {
                serviceId: 99,
                serviceName: 'Собственная служба',
                tariffId: 0,
                price: {
                    currency: 'RUR',
                    value: '0',
                },
                dayFrom: 0,
                dayTo: 0,
                groupCount: 20,
            },
        ],
    },
    urls: {
        encrypted: '/redir/encrypted',
        decrypted: '/redir/decrypted',
        geo: '/redir/geo',
        offercard: '/redir/offercard',
    },
    cpc: 'DqqPjIrWS5xIT',
    vendor: {
        id: 2222,
        entity: 'vendor',
        name: 'some_vendor_name',
        slug: 'some-vendor-name',
    },
};

const BENEFIT = {
    type: 'recommended',
    description: 'Хорошая цена от надёжного магазина',
    isPrimary: true,
};

const offer = createOffer({
    ...OFFER,
    shop: {
        ...OFFER.shop,
        outletsCount: POINTS_COUNT,
        storesCount: POINTS_COUNT,
    },
    delivery: {
        ...OFFER.delivery,
        pickupOptions: PICKUP_OPTIONS,
        availableServices: [
            {
                serviceId: 213,
                serviceName: 'RusPost Basildon EMS',
                isMarketBranded: true,
            },
        ],
    },
    navnodes: NAVNODES,
    categories: CATEGORIES,
    benefit: BENEFIT,
    isCutPrice: false,
});

const product = createProduct({
    slug: ROUTE.slug,
    navnodes: NAVNODES,
    categories: CATEGORIES,
}, ROUTE.productId);

const stateWithDefaultOffer = mergeState([
    offer,
    product,
    {
        data: {
            search: {
                totalOffersBeforeFilters: 2,
                total: 2,
            },
        },
    },
]);

export const buildProductWithDefaultOffer = () => {
    const dataMixin = {
        data: {
            search: {
                total: 2,
                totalOffers: 2,
            },
        },
    };

    return mergeState([
        stateWithDefaultOffer,
        dataMixin,
    ]);
};

export default {
    buildProductWithDefaultOffer,
    ROUTE,
};
