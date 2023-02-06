import {createOffer, createProduct} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';
import {createPrice, createPriceRange} from '@yandex-market/kadavr/mocks/Report/helpers/price';
import {createEntityPicture} from '@yandex-market/kadavr/mocks/Report/helpers/picture';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import {randomString} from '@self/root/src/helpers/string';

const phoneProductRoute = routes.product.phone;
const PHONE_OFFER_WARE_ID = 'quMa97ufY4oEFgoYvgxNMQ';

const createCategories = () => [
    {
        entity: 'category',
        id: 91491,
        name: 'Мобильные телефоны',
        fullName: 'Мобильные телефоны',
        slug: 'mobilnye-telefony',
        type: 'guru',
        isLeaf: true,
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

const createProductOptions = () => ({
    type: 'model',
    showUid: randomString(),
    prices: createPriceRange(300, 400, 'RUB'),
    slug: 'smartfon-apple-iphone-7-128gb',
    titles: {
        raw: 'Тестовый телефон',
    },
    lingua: {
        type: {
            nominative: '',
            genitive: '',
            dative: '',
            accusative: '',
        },
    },
    categories: createCategories(),
    navnodes: createNavnodes(),
});

const picture = createEntityPicture(
    {
        original: {
            url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig',
        },
    },
    'product', phoneProductRoute.productId,
    '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/1hq'
);

const offerPicture = {
    entity: 'picture',
    original: {
        url: '//avatars.mds.yandex.net/get-marketpictesting/1044912/market_NzxnwHmEQUXv6y1nm0pHCA/50x50',
        width: 50,
        height: 50,
    },
    thumbnails:
        [
            {
                containerWidth: 50,
                containerHeight: 50,
                url: '//avatars.mds.yandex.net/get-marketpictesting/1044912/market_NzxnwHmEQUXv6y1nm0pHCA/50x50',
                width: 50,
                height: 50,
            },
        ],
};

const PRICE = createPrice(300, 'RUB', 300, false, {
    /**
     * Из-за бага в json-schema-faker явно указываем отсутствие скидки
     */
    discount: null,
});

const createShop = ({offerId}) => ({
    entity: 'shop',
    logo: {
        entity: 'picture',
        width: 14,
        height: 14,
        url: '//avatars.mdst.yandex.net/get-market-shop-logo/69137/2a000001678e905b308a70e389a016eaf9b7/orig',
    },
    id: 1672,
    name: 'TechPort.ru',
    gradesCount: 38303,
    overallGradesCount: 75059,
    qualityRating: 4,
    isGlobal: false,
    isCpaPrior: true,
    isCpaPartner: false,
    taxSystem: 'OSN',
    isNewRating: true,
    newGradesCount: 38303,
    newQualityRating: 4.408505861,
    newQualityRating3M: 4.364615821,
    ratingToShow: 4.364615821,
    newGradesCount3M: 3527,
    status: 'actual',
    cutoff: '',
    outletsCount: 1,
    storesCount: 0,
    pickupStoresCount: 1,
    depotStoresCount: 1,
    postomatStoresCount: 0,
    bookNowStoresCount: 0,
    subsidies: false,
    deliveryVat: 'NO_VAT',
    feed: {
        id: '123.123',
        offerId,
    },
});

const delivery = () => ({
    price: {value: 0},
    shopPriorityRegion: {
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
    shopPriorityCountry: {
        entity: 'region',
        id: 225,
        name: 'Россия',
        lingua: {
            name: {
                genitive: 'России',
                preposition: 'в',
                prepositional: 'России',
                accusative: 'Россию',
            },
        },
    },
    isPriorityRegion: true,
    isCountrywide: true,
    isAvailable: true,
    hasPickup: true,
    hasLocalStore: true,
    hasPost: false,
    isFake: false,
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
    availableServices: [
        {
            serviceId: 99,
            serviceName: 'Собственная служба',
        },
    ],
    isFree: true,
    isDownloadable: false,
    inStock: true,
    postAvailable: true,
    options: [
        {
            price: {
                currency: 'RUR',
                value: '0',
                isDeliveryIncluded: false,
            },
            dayFrom: 4,
            dayTo: 4,
            isDefault: true,
            serviceId: '99',
            paymentMethods: [
                'CASH_ON_DELIVERY',
            ],
            partnerType: 'regular',
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
        },
    ],
    pickupOptions: [
        {
            serviceId: 99,
            serviceName: 'Собственная служба',
            price: {
                currency: 'RUR',
                value: '0',
            },
            dayFrom: 4,
            dayTo: 4,
            orderBefore: 24,
            groupCount: 1,
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
        },
    ],
    deliveryPartnerTypes: ['YANDEX_MARKET'],
});

const createOfferOptions = ({id}) => ({
    type: 'offer',
    entity: 'offer',
    id: id,
    wareId: id,
    feeShow: id,
    benefit: {
        description: 'Хорошая цена от надёжного магазина',
        isPrimary: true,
        type: 'default',
    },
    bundleSettings: {quantityLimit: {maximum: 999, minimum: 1, step: 1}},
    prices: PRICE,
    filters: [{
        id: 14871214,
        type: 'enum',
        name: 'Цвет товара',
        xslname: 'color_vendor',
        subType: 'image_picker',
        kind: 2,
        position: 1,
        noffers: 1,
        valuesCount: 1,
        values: [{
            initialFound: 1,
            checked: true,
            found: 1,
            value: 'золотой',
            id: 15266392,
        }],
        valuesGroups: [{
            type: 'all',
            valuesIds: ['15266392'],
        }],
    }],
    filterState: {
        14871214: ['15266392'],
    },
    orderMinCost: {
        value: 50500,
        currency: 'RUR',
    },
    categoryIds: [],
    supplier: {
        entity: 'shop',
        id: 981983,
        name: 'mix-mobile.ru',
        business_id: 780641,
        business_name: 'Балякин Михаил Юрьевич',
        type: '1',
    },
    urls: {
        cpa: '/redir/cpa',
    },
    cpc: randomString(),
    cpa: 'real',
    categories: createCategories(),
    navnodes: createNavnodes(),
    delivery: delivery(),
    shop: createShop({offerId: id}),
    pictures: [
        offerPicture,
        offerPicture,
    ],
});

const createProductWithCPADefaultOffer = ({productId, offerId}) => createProduct({
    ...createProductOptions(),
    offers: {
        count: 7,
        items: [createOfferOptions({id: offerId})],
    },
}, productId);

const createOffers = ({count}) => {
    const states = [];

    for (let i = 0; i < count; i++) {
        states.push(createProductWithCPADefaultOffer({
            productId: `0000${i}`,
            offerId: `00000${i}`,
        }));
    }

    return mergeReportState(states);
};

const phoneProductWithCPADefaultOffer = mergeReportState([
    createProductWithCPADefaultOffer({
        productId: phoneProductRoute.productId,
        offerId: PHONE_OFFER_WARE_ID,
    }),
    createOffer(createOfferOptions({id: PHONE_OFFER_WARE_ID}), PHONE_OFFER_WARE_ID),
    picture,
]);

export {
    phoneProductRoute,
    phoneProductWithCPADefaultOffer,
    createOffers,
};
