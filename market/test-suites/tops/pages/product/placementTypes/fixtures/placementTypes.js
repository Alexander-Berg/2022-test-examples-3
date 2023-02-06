import {
    mergeState,
    createProduct,
    createOffer,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import {offerMock} from '@self/project/src/spec/hermione/fixtures/offer/offer';

const productId = '123';
const slug = 'onetwothree';

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

const product = createProduct({
    slug: 'some-slug',
    type: 'model',
    categories,
    navnodes,
    offers: {
        count: 1,
    },
}, productId);

const delivery = ({deliveryPartnerTypes = 'YANDEX_MARKET'}) => ({
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
    deliveryPartnerTypes: [deliveryPartnerTypes],
});

const shopWithLogo = {
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
};

const supplier = ({type = '1'}) => ({
    entity: 'shop',
    id: 981983,
    name: 'mix-mobile.ru',
    business_id: 780641,
    business_name: 'Балякин Михаил Юрьевич',
    type,
});

const thirdPartDefaultOffer = createOffer({
    ...offerMock,
    benefit: {
        type: 'default',
        description: 'Хорошая цена от надёжного магазина',
        isPrimary: true,
    },
    cpa: 'real',
    offerColor: 'blue',
    delivery: delivery({deliveryPartnerTypes: 'YANDEX_MARKET'}),
    supplier: supplier({type: '3'}),
    shop: shopWithLogo,
}, offerMock.wareId);

const firstPartDefaultOffer = createOffer({
    ...offerMock,
    benefit: {
        type: 'default',
        description: 'Хорошая цена от надёжного магазина',
        isPrimary: true,
    },
    cpa: 'real',
    offerColor: 'blue',
    delivery: delivery({deliveryPartnerTypes: 'YANDEX_MARKET'}),
    supplier: supplier({type: '1'}),
    shop: shopWithLogo,
    ownMarketPlace: true,
}, offerMock.wareId);

const dsbsDefaultoffer = createOffer({
    ...offerMock,
    benefit: {
        type: 'default',
        description: 'Хорошая цена от надёжного магазина',
        isPrimary: true,
    },
    cpa: 'real',
    offerColor: 'white',
    delivery: delivery({deliveryPartnerTypes: 'SHOP'}),
}, offerMock.wareId);

const cpcDefaultOffer = createOffer({
    ...offerMock,
    benefit: {
        type: 'default',
        description: 'Хорошая цена от надёжного магазина',
        isPrimary: true,
    },
    shop: shopWithLogo,
}, offerMock.wareId);

const route = {
    productId,
    slug,
};

const cpcOfferState = mergeState([
    {
        data: {
            search: {
                totalOffersBeforeFilters: 1,
                total: 1,
                totalOffers: 1,
            },
        },
    },
    product,
    cpcDefaultOffer,
]);

const productWithFirstPartyOfferState = mergeState([
    {
        data: {
            search: {
                totalOffersBeforeFilters: 1,
                total: 1,
                totalOffers: 1,
            },
        },
    },
    product,
    firstPartDefaultOffer,
]);

const productWithThirdPartyOfferState = mergeState([
    {
        data: {
            search: {
                totalOffersBeforeFilters: 1,
                total: 1,
                totalOffers: 1,
            },
        },
    },
    product,
    thirdPartDefaultOffer,
]);

const productWithDsbsOfferState = mergeState([
    {
        data: {
            search: {
                totalOffersBeforeFilters: 1,
                total: 1,
                totalOffers: 1,
            },
        },
    },
    product,
    dsbsDefaultoffer,
]);

export {
    route,
    productWithFirstPartyOfferState,
    productWithThirdPartyOfferState,
    productWithDsbsOfferState,
    cpcOfferState,
};
