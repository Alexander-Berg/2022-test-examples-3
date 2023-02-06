import {createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

export const offerMock = {
    entity: 'offer',
    titles: {'raw': 'Яндекс.Станция, фиолетовая', 'highlighted': [{'value': 'Яндекс.Станция, фиолетовая'}]},
    slug: 'yandex-stantsiia-fioletovaia',
    categories: [{
        entity: 'category',
        id: 15553892,
        nid: 71716,
        name: 'Умные колонки',
        slug: 'umnye-kolonki',
        fullName: 'Умные колонки',
    }],
    cpc: 'vdMHywk6BPsxlRcdae6zjc1HK4A_BV2DL0IsSw9aAgWBXjXa1Md3A5xK6zXHJpUkBcxYep-9lCbD6gMbDSi9YTrotrTBsUwlwWa5qDs2appu5nWzdTQoaI7fZGkTuySajSdaE6y-XH5ufJhEtQ3pcfBtDBv4Md9m',
    navnodes: [{
        entity: 'navnode',
        id: 71716,
        name: 'Умные колонки',
        slug: 'umnye-kolonki',
        fullName: 'Умные колонки',
        isLeaf: true,
        rootNavnode: {},
    }],

    isPreorder: true,
    wareId: 'r7GbdwfUxfiW2FyntzHGgA',
    prices: {
        currency: 'RUR',
        value: '9991',
        isDeliveryIncluded: false,
        isPickupIncluded: false,
        rawValue: '9991',
    },
    cpa: 'real',
    shop: {
        entity: 'shop',
        id: 431782,
        name: 'Тестовый магазин',
        business_id: 10627113,
        business_name: 'Тестовый магазин',
        slug: 'testovyi-magazin',
    },
    sku: '100256653698',
};

export const offerPreorder = createOffer(offerMock, offerMock.wareId);

export default {
    offerPreorder,
};

