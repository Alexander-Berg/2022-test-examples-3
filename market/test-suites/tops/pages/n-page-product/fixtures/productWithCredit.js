import {mergeState, createProduct, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

const shopId = '431782';
const productId = '123';
const offerId = 'quMa97ufY4oEFgoYvgxNMQ';
const slug = 'onetwothree';

const createCategories = () => [
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

const createNavnodes = () => [
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

const feeShow = 'gf45678uikjhgfe4567ujhg';

const offerBase = {
    categories: createCategories(),
    navnodes: createNavnodes(),
    feeShow,
    creditInfo: {
        monthlyPayment: {
            value: 2401,
            currency: 'RUR',
        },
    },
    prices: {
        currency: 'RUR',
        value: '37490',
        isDeliveryIncluded: false,
        rawValue: '37490',
        discount: {
            oldMin: '45000',
            percent: 20,
        },
    },
    feed: {
        id: '12345566',
    },
    shop: {
        id: shopId,
        entity: 'shop',
        name: 'Тестовый магазин проекта Фулфиллмент',
        status: 'actual',
        slug: 'slag-suag-swag',
        logo: 'shop-logo',
        feed: {
            id: '123.123',
            offerId: '123',
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
        encrypted: '',
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
        options: [],
    },
};

const product = createProduct({
    deletedId: null,
    categories: createCategories(),
    navnodes: createNavnodes(),
    slug: 'product',
}, productId);

const offer = createOffer(offerBase, offerId);

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
};
