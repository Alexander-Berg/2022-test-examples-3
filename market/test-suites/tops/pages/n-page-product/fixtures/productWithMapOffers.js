import {mergeState, createProduct, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

const productId = '123';
const slug = 'onetwothree';

const offer1Id = '456';
const offer2Id = '457';

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

const shop = {
    entity: 'shop',
    id: 324,
    name: 'ShoshannaShop',
    slug: 'shoshannaShop',
    status: 'actual',
    outletsCount: 1,
};

const outlet = {
    entity: 'outlet',
    id: '1177093',
    address: {
        fullAddress: 'Москва, Полковая, д. 3, стр. Без №',
        locality: 'Москва',
        street: 'Полковая',
        building: '3',
    },
    shop: {
        id: shop.id,
    },
    type: 'pickup',
    gpsCoord: {
        longitude: '37.6035721',
        latitude: '55.79971565',
    },
    name: 'Интернет-магазин Shoshanna.ru',
};

const prices = {
    currency: 'RUR',
    value: '11490',
    isDeliveryIncluded: false,
    rawValue: '11490',
};

const offer = {
    entity: 'offer',
    outlet,
    shop,
    prices,
    categories,
    urls: {
        encrypted: '/redir/encrypted',
        decrypted: '/redir/decrypted',
        geo: '/redir/geo',
        offercard: '/redir/offercard',
    },
};

const product = createProduct({
    offers: {
        count: 2,
    },
    type: 'model',
    categories,
    navnodes,
}, productId);

const route = {
    productId,
    slug,
};

const state = mergeState([
    {
        data: {
            search: {
                total: 2,
            },
        },

    },
    product,
    createOffer(offer, offer1Id),
    createOffer(offer, offer2Id),
]);

export default {
    state,
    route,
};
