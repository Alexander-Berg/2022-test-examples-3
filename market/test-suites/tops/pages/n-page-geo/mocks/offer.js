import {merge, cloneDeep} from 'lodash';

const encryptedUrl = '/redir/encrypted';

const shop = {
    entity: 'shop',
    id: 324,
    name: 'ShoshannaShop',
    slug: 'shoshannaShop',
    status: 'actual',
    outletsCount: 1,
    feed: {
        id: '123.123',
        offerId: '123',
    },
};

const outlet = {
    entity: 'outlet',
    id: '1177093',
    region: {
        entity: 'region',
        id: 213,
        name: 'Москва',
        type: 6,
    },
    address: {
        fullAddress: 'Москва, Полковая, д. 3, стр. Без №',
        country: '',
        region: '',
        locality: 'Москва',
        street: 'Полковая',
        building: '3',
    },
    telephones: [
        {
            cityCode: '800',
            countryCode: '7',
            entity: 'telephone',
            extensionNumber: '',
            telephoneNumber: '7753729',
        },
    ],
    workingTime: [
        {
            daysFrom: '1',
            daysTo: '1',
            hoursFrom: '10:00',
            hoursTo: '21:00',
        },
        {
            daysFrom: '2',
            daysTo: '2',
            hoursFrom: '10:00',
            hoursTo: '21:00',
        },
        {
            daysFrom: '3',
            daysTo: '3',
            hoursFrom: '10:00',
            hoursTo: '21:00',
        },
        {
            daysFrom: '4',
            daysTo: '4',
            hoursFrom: '10:00',
            hoursTo: '21:00',
        },
        {
            daysFrom: '5',
            daysTo: '5',
            hoursFrom: '10:00',
            hoursTo: '21:00',
        },
        {
            daysFrom: '6',
            daysTo: '6',
            hoursFrom: '10:00',
            hoursTo: '21:00',
        },
        {
            daysFrom: '7',
            daysTo: '7',
            hoursFrom: '10:00',
            hoursTo: '21:00',
        },
    ],
    shop: {
        id: shop.id,
    },
    gpsCoord: {
        longitude: '37.6035721',
        latitude: '55.79971565',
    },
    isMarketBranded: false,
    bundleCount: 79,
    serviceId: 99,
    name: 'Интернет-магазин Shoshanna.ru',
};

const bundledInfo = {
    bundleCount: 25,
    bundleSettings: {
        quantityLimit: {
            maximum: 999,
            minimum: 1,
            step: 1,
        },
    },
    bundled: {
        outletIds: [outlet.id],
        count: 25,
        shopCategory: {
            count: 25,
            minPrice: {
                currency: 'RUR',
                value: '2048',
                isDeliveryIncluded: false,
            },
        },
    },
};

const prices = {
    currency: 'RUR',
    value: '11490',
    isDeliveryIncluded: false,
    rawValue: '11490',
};

const offerMock = {
    ...bundledInfo,
    entity: 'offer',
    outlet,
    shop,
    prices,
    cpc: 'testCpc',
    wareId: 'testWareId',
    urls: {
        encrypted: encryptedUrl,
        decrypted: '/redir/decrypted',
        geo: '/redir/geo',
        offercard: '/redir/offercard',
    },
};

const mobileCategory = {
    categories: [
        {
            id: 91491,
            slug: 'mobilnye-telefony',
        },
    ],
};

const clickoutButton = {
    urls: {
        encrypted: 'blah',
    },
};

const cpaButton = {
    beruFeeShow: '123',
    feeShow: '123',
    urls: {
        encrypted: '/redir/encrypted',
        cpa: '/redir/cpa',
    },
    cpa: 'real',
};

const pickupType = {
    outlet: {
        type: 'pickup',
    },
};

const storeType = {
    outlet: {
        type: 'store',
    },
};

function createOfferMock(...params) {
    const mock = merge({}, cloneDeep(offerMock), ...params);

    if (mock.outlet.type !== 'pickup' && mock.outlet.type !== 'store') {
        throw new Error('Нужно указать тип аутлета');
    }

    return mock;
}

const medicineWarnings = {
    warnings: {
        common: [
            {
                type: 'medicine',
                value: {
                    full: 'Есть противопоказания, посоветуйтесь с врачом',
                    short: 'Есть противопоказания, посоветуйтесь с врачом',
                },
            },
        ],
    },
};

const adultWarnings = {
    warnings: {
        common: [
            {
                type: 'adult',
                value: {
                    full: 'Возрастное ограничение',
                    short: 'Возрастное ограничение',
                },
            },
        ],
    },
};

export {
    pickupType,
    storeType,
    adultWarnings,
    medicineWarnings,
    mobileCategory,
    createOfferMock,
    clickoutButton,
    cpaButton,
    encryptedUrl,
};
