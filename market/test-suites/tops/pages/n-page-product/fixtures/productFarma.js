import {mergeState, createProduct, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

const shopId = '431782';
const productId = '123';
const offerId = '456';
const slug = 'onetwothree';

const createCategories = () => [
    {
        entity: 'category',
        id: 91491,
        name: 'Лечение гриппа и простуды',
        fullName: 'Лечение гриппа и простуды',
        type: 'guru',
        isLeaf: true,
        slug: 'sredstva-ot-grippa-i-prostudy',
    },
];

const createNavnodes = () => [
    {
        entity: 'navnode',
        id: 54726,
        name: 'Лечение гриппа и простуды',
        slug: 'sredstva-ot-grippa-i-prostudy',
        fullName: 'Лечение гриппа и простуды',
        isLeaf: true,
        rootNavnode: {},
    },
];

const product = createProduct({
    deletedId: null,
    categories: createCategories(),
    navnodes: createNavnodes(),
    slug: 'product',
    specs: {
        internal: [{value: 'medicine'}],
    },
}, productId);

const offer = createOffer({
    categories: createCategories(),
    navnodes: createNavnodes(),
    model: {
        id: productId,
    },
    prices: {
        currency: 'RUR',
        value: '345',
        isDeliveryIncluded: false,
        rawValue: '345',
    },
    shop: {
        id: shopId,
        entity: 'shop',
        name: 'Тестовый магазин',
        status: 'actual',
        slug: 'slag-suag-swag',
        logo: 'shop-logo',
        feed: {
            id: 123123,
        },
    },
    benefit: {
        type: 'recommended',
        description: 'Хорошая цена от надёжного магазина',
        isPrimary: true,
    },
    payments: {
        deliveryCard: true,
        deliveryCash: true,
        prepaymentCard: true,
        prepaymentOther: false,
    },
    urls: {
        U_DIRECT_OFFER_CARD_URL: 'http://example.com',
        decrypted: 'http://example.com',
        direct: 'http://example.com',
        encrypted: 'http://example.com',
        geo: 'http://example.com',
        offercard: 'http://example.com',
        pickupGeo: 'http://example.com',
        postomatGeo: 'http://example.com',
        showPhone: 'http://example.com',
        storeGeo: 'http://example.com',
    },
    vendor: {
        id: 1,
        webpageRecommendedShops: '/some-url/',
        name: 'vendor',
        logo: {
            url: 'logo-url',
        },
    },
    cpa: 'real',
    cpc: 'YVwBN9ETvPXGDZSiKF2l7yI7ewwo3VPSxwc_i4zikHLQonAxhPCtDEi6sGg0m78tNJMVpses-WvglfZYpW1ZqaxAkk' +
        '6Jkk9okufOs3YBoznAc40Hlkj9wdfSS5fsdZswCS8u1xJXkmg,',
    delivery: {
        shopPriorityRegion: {
            entity: 'region',
            id: 62007514,
            name: 'ea mol',
            lingua: {
                name: {
                    accusative: 'ut aliqua',
                    genitive: 'veniam Excepteur consequat',
                    preposition: 'sit',
                    prepositional: 'nulla amet',
                },
            },
        },
        shopPriorityCountry: {
            entity: 'region',
            id: 59868827,
            name: 'in officia exercitation',
            lingua: {
                name: {
                    accusative: 'anim aute',
                    genitive: 'reprehenderit',
                    preposition: 'dolor ad Duis aliqua sunt',
                    prepositional: 'voluptate cillum',
                },
            },
        },
        region: {
            lingua: {
                name: {
                    accusative: 'anim aute',
                    genitive: 'reprehenderit',
                    preposition: 'dolor ad Duis aliqua sunt',
                    prepositional: 'voluptate cillum',
                },
            },
            title: 'Регион, в который будет осуществляться доставка курьером',
        },
        price: {
            currency: 'reprehenderit ',
            value: 87304050,
        },
        specs: {
            internal: [{value: 'medicine'}],
        },
        options: [],
    },
}, offerId);

const state = mergeState([
    product,
    offer,
]);

const route = {
    productId,
    slug,
};

export default {
    state,
    route,
    offerId,
    productId,
};
