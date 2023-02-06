import {mergeState, createProduct, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

const productId = '123';
const slug = 'onetwothree';
export const CASHBACK_AMOUNT = 100;

const offer1Id = '456';
const offer2Id = '457';

const CASHBACK_PROMO = {
    type: 'blue-cashback',
    value: CASHBACK_AMOUNT,
};

const categories = [
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

const navnodes = [
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

const payments = {
    deliveryCard: true,
    deliveryCash: true,
    prepaymentCard: true,
    prepaymentOther: false,
};

const product = createProduct({
    offers: {
        count: 2,
    },
    type: 'model',
    categories,
    navnodes,
    slug,
}, productId);

const offer = {
    isCutPrice: false,
    categories,
    navnodes,
    payments,
    wareId: productId,
    urls: {
        encrypted: '/redir/encrypted',
        decrypted: '/redir/decrypted',
        geo: '/redir/geo',
        offercard: '/redir/offercard',
    },
    shop: {
        id: 1,
        name: 'shop',
        slug: 'shop',
        feed: {
            id: 123123,
        },
    },
    cpc: 'DqqPjIrWS5xIT',
    vendor: {
        id: 2222,
        entity: 'vendor',
        name: 'some_vendor_name',
        slug: 'some-vendor-name',
    },
    promo: CASHBACK_PROMO,
    promos: [CASHBACK_PROMO],
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

const route = {
    productId,
    slug,
};

const state = mergeState([
    {
        data: {
            search: {
                totalOffersBeforeFilters: 2,
            },
        },

    },
    product,
    createOffer(offer, offer1Id),
    createOffer(offer, offer2Id),

    createOffer(offer),
    createOffer(offer),
    createOffer(offer),
    createOffer(offer),
    createOffer(offer),
]);

export default {
    state,
    route,
};
