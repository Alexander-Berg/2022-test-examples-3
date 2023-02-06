import {
    mergeState,
    createOffer,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import {offerMock} from '@self/project/src/spec/hermione/fixtures/offer/offer';

const productId = '123';
const slug = 'onetwothree';

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
    titles: {
        raw: 'Offer Offer Offer',
    },
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

const cpcOffer = createOffer({
    shop: {
        entity: 'shop',
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
    },
});

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
    cpcOffer,
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
    dsbsDefaultoffer,
]);

export {
    route,
    productWithFirstPartyOfferState,
    productWithThirdPartyOfferState,
    productWithDsbsOfferState,
    cpcOfferState,
};
