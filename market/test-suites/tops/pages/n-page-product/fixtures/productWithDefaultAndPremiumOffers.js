// @flow

// flowlint-next-line untyped-import: off
import {mergeState, createProduct, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

const shopId = '431782';
const productId = '123';
const premiumOfferId = '456';
const defaultOfferId = '457';
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

const createOfferOptions = (benefitType: string) => ({
    categories: createCategories(),
    navnodes: createNavnodes(),
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
    shop: {
        id: shopId,
        entity: 'shop',
        name: 'Тестовый магазин проекта Фулфиллмент',
        status: 'actual',
        slug: 'slag-suag-swag',
        logo: 'shop-logo',
        feed: {
            id: 123123,
        },
    },
    benefit: {
        type: benefitType,
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
    cpc: `${Math.random()}`,
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
});


const product = createProduct({
    deletedId: null,
    categories: createCategories(),
    navnodes: createNavnodes(),
    slug: 'product',
}, productId);

const state = mergeState([
    product,
    createOffer(
        createOfferOptions('cheapest'),
        defaultOfferId
    ),
    createOffer(
        createOfferOptions('premium'),
        premiumOfferId
    ),
]);


const route = {
    productId,
    slug,
};

export default {
    state,
    route,
    defaultOfferId,
};
