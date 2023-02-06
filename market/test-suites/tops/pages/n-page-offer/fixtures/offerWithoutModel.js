import {createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

const offerId = 'offerWithButton';

const categoryMock = {
    entity: 'category',
    id: 7757500,
    nid: 57713,
    name: 'Топоры',
    slug: 'topory',
    fullName: 'Топоры',
    type: 'guru',
    cpaType: 'cpa_non_guru',
    isLeaf: true,
    kinds: [],
};

const offer = createOffer({
    cpc: 'YVwBN9ETvPXGDZSiKF2l7yI7ewwo3VPSxwc_i4zikHLQonAxhPCtDEi6sGg0m78tNJMVpses-WvglfZYpW1ZqaxAkk' +
        '6Jkk9okufOs3YBoznAc40Hlkj9wdfSS5fsdZswCS8u1xJXkmg,',
    description: 'Характеристики товара',
    prices: {
        currency: 'RUR',
        value: '34990',
        isDeliveryIncluded: false,
        rawValue: '34990',
    },
    urls: {
        encrypted: '/redir/test',
        decrypted: '/redir/test',
        offercard: '/redir/test',
        geo: '/redir/geo',
    },
    shop: {
        id: 774,
        slug: 'shop',
        name: 'test.yandex.ru',
        outletsCount: 1,
    },
    outlet: {
        entity: 'outlet',
        id: '41415',
        name: 'Лавочка с хорошим товаром в описании',
        type: 'pickup',
        purpose: [
            'pickup',
        ],
        serviceId: 99,
        email: 'myshop@goodshop.il',
        isMarketBranded: false,
        shop: {
            id: 774,
        },
        address: {
            fullAddress: 'Москва, Рандомная, д. 12, корп. 2a',
            country: '',
            region: '',
            locality: 'Москва',
            street: 'Рандомная',
            km: '',
            building: '12',
            block: '2a',
            wing: '',
            estate: '',
            entrance: '',
            floor: '',
            room: '',
            office_number: '',
            note: 'офис 50',
        },
        telephones: [
            {
                entity: 'telephone',
                countryCode: '7',
                cityCode: '950',
                telephoneNumber: '272-3645',
                extensionNumber: '8888',
            },
        ],
        workingTime: [
            {
                daysFrom: '1',
                daysTo: '1',
                hoursFrom: '00:00',
                hoursTo: '24:00',
            },
        ],
        selfDeliveryRule: {
            workInHoliday: true,
            currency: 'RUR',
            cost: '0',
            shipperHumanReadableId: 'Self',
        },
        region: {
            entity: 'region',
            id: 213,
            name: 'Москва',
            lingua: {
                name: {
                    genitive: 'Москвы',
                    preposition: 'в',
                    prepositional: 'Москве',
                    accusative: 'Москву',
                },
            },
        },
        gpsCoord: {
            longitude: '37.609218',
            latitude: '55.753559',
        },
    },
    seller: {
        comment: 'Минимальная сумма заказа 40 гривен.',
    },
    pictures: [
        {
            entity: 'picture',
            original: {
                containerWidth: 462,
                containerHeight: 500,
                url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig',
                width: 462,
                height: 500,
            },
            thumbnails: [
                {
                    containerWidth: 240,
                    containerHeight: 240,
                    url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/8hq',
                    width: 240,
                    height: 240,
                },
                {
                    containerWidth: 50,
                    containerHeight: 50,
                    url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/1hq',
                    width: 50,
                    height: 50,
                },
            ],
            filtersMatching: {
                '13887626': ['13887686'],
                '14871214': ['14899397'],
            },
        },
        {
            entity: 'picture',
            original: {
                containerWidth: 462,
                containerHeight: 500,
                url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig',
                width: 462,
                height: 500,
            },
            thumbnails: [
                {
                    containerWidth: 240,
                    containerHeight: 240,
                    url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/8hq',
                    width: 240,
                    height: 240,
                },
                {
                    containerWidth: 50,
                    containerHeight: 50,
                    url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/1hq',
                    width: 50,
                    height: 50,
                },
            ],
            filtersMatching: {
                '13887626': ['13887686'],
                '14871214': ['14899397'],
            },
        },
    ],
    filters: [
        {
            id: '14871214',
            type: 'enum',
            name: 'Цвет товара',
            xslname: 'color_vendor',
            subType: 'image_picker',
            kind: 2,
            position: 1,
            noffers: 1,
            valuesCount: 1,
            values: [
                {
                    initialFound: 1,
                    found: 1,
                    value: 'серебристый',
                    code: '#c0c0c0',
                    id: '14897638',
                },
            ],
            valuesGroups: {0: {type: 'all', valuesIds: {0: '14897638'}}},
        },
    ],
    categories: [categoryMock],
    returnPolicy: '7d',
}, offerId);

export {
    offer,
    offerId,
    categoryMock,
};
