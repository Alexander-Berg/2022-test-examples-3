import {
    mergeState,
    createProduct,
    createOffer,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import {cpaOfferMock} from './cpaOffer.mock';

const productId = '123';
const slug = 'onetwothree';

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

const payments = {
    deliveryCard: true,
    deliveryCash: true,
    prepaymentCard: true,
    prepaymentOther: false,
};

const product = createProduct({
    offers: {
        count: 2,
        items: [cpaOfferMock.wareId, offer2Id],
    },
    offersCount: 2,
    isNew: false,
    categories,
    slug,
    description: 'test',
    prices: {
        min: '100',
        max: '10000',
        currency: 'RUR',
    },
    rating: 4,
    titles: {
        raw: 'Наушники Apple EarPods (3.5 мм) белый',
        highlighted: [{value: 'Наушники Apple EarPods (3.5 мм)', highlight: false}],
    },
    type: 'guru',
    navnodes: {
        entity: 'navnode',
        id: 18041766,
        name: 'Наушники и Bluetooth-гарнитуры',
        slug: 'naushniki-i-bluetooth-garnitury',
        fullName: 'Наушники и Bluetooth-гарнитуры',
        isLeaf: true,
        rootNavnode: {},
    },
    reviewsCount: 0,
    vendor: {
        entity: 'vendor',
        id: 153043,
        name: 'Apple',
        slug: 'apple',
        website: 'http://www.apple.com/ru',
        logo: {
            entity: 'picture',
            url: '//avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
            thumbnails: [],
        },
        filter: '7893318:153043',
    },
}, productId);

const offerCPA = {
    ...cpaOfferMock,
    model: {id: parseInt(productId, 10)},
};

const offerCPC = {
    isCutPrice: false,
    categories,
    navnodes,
    payments,
    shop: {
        id: 1,
        name: 'shop',
        slug: 'shop',
    },
    vendor: {
        id: 2222,
        entity: 'vendor',
        name: 'some_vendor_name',
        slug: 'some-vendor-name',
    },
    wareId: productId + offer2Id,
    cpc: `DqqPjIrWS5xIT${offer2Id}`,
    urls: {
        encrypted: '/redir/encrypted',
        decrypted: '/redir/decrypted',
        geo: '/redir/geo',
        offercard: '/redir/offercard',
    },
    prices: {
        currency: 'RUR',
        value: '1100',
        isDeliveryIncluded: false,
        isPickupIncluded: false,
        rawValue: '1100',
    },
    bundleCount: 1,
    bundleSettings: {quantityLimit: {minimum: 1, step: 1}},
};

const route = {
    productId,
    slug,
};

const state = mergeState([
    {
        data: {
            search: {
                total: 2,
                totalOffers: 2,
                totalOffersBeforeFilters: 2,
            },
        },

    },
    product,
    createOffer(offerCPA, cpaOfferMock.wareId),
    createOffer(offerCPC, offer2Id),
]);

export default {
    state,
    route,
};
