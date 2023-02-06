import {mergeState, createProduct, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

import {cpaOfferMock} from './cpaOffer.mock';
import {dataFixture} from './dataFixture.mock';

const productId = '123';
const offerId = cpaOfferMock.wareId;
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

const product = createProduct({
    deletedId: null,
    categories: createCategories(),
    slug,
    offers: {
        items: [offerId],
    },
    isNew: false,
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
    offersCount: 1,
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

const offer = createOffer({
    ...cpaOfferMock,
    model: {id: parseInt(productId, 10)},
    benefit: {
        type: 'recommended',
        description: 'Хорошая цена от надёжного магазина',
        isPrimary: true,
    },
}, offerId);

const state = mergeState([
    product,
    offer,
    dataFixture,
    {
        data: {
            search: {
                total: 1,
                totalOffers: 1,
            },
        },
    },
]);

const route = {
    productId,
    slug,
};

export default {
    state,
    route,
    product,
    offer,
};
