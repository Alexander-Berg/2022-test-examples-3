import {
    mergeState,
    createProduct,
    createOffer,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import {offerMock} from '@self/project/src/spec/hermione/fixtures/offer/offer';

import offerTop6Mock from '@self/root/src/spec/hermione/kadavr-mock/report/offer/unitInfoWithTop6';

const offerTop6MockId = 'pqqSie9wuNOmYgPW_1Mwbg';
const offerTop6 = createOffer(offerTop6Mock, offerTop6MockId);
const expectedOfferPriceText = '2 215 ₽/ уп';

const offerExpressMock = {
    ...offerMock,
    payments: {
        deliveryCard: false,
        deliveryCash: false,
        prepaymentCard: true,
        prepaymentOther: false,
    },
    unitInfo: {
        mainUnit: 'уп',
        referenceUnits: [
            {
                unitName: 'м²',
                unitCount: 1.248,
                unitPrice: {
                    currency: 'RUR',
                    value: 1775,
                },
            },
        ],
    },
    prices: {
        currency: 'RUR',
        value: '7290',
        isDeliveryIncluded: false,
        rawValue: '7290',
        discount: {
            percent: '20',
            oldMin: '9000',
        },
    },
};
delete offerExpressMock.prices.discount;
const expectedFirstDOMainPriceText = '7 290 ₽/ уп';
const expectedReferencePriceText = '1 775 ₽/ м²';

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

const delivery = ({deliveryPartnerTypes = 'SHOP', isExpress = false}) => ({
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
    isExpress,
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
                'CARD_ON_DELIVERY',
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

const outletInfo = {
    workSchedule: 'Пн - Вс: 10:00 - 22:00',
    currentWorkSchedule: {
        from: {
            hour: 10,
            minute: 0,
        },
        to: {
            hour: 22,
            minute: 0,
        },
    },
    isNewRating: true,
    ratingToShow: 3.9,
    ratingType: 3,
    gradesCount: 12,
    overallGradesCount: 12,
    newGradesCount: 12,
    newGradesCount3M: 6,
};
const shop = {
    id: 1,
    name: 'Экспрессович',
    slug: 'shop',
    ...outletInfo,
};
const supplier = ({type = '1'}) => ({
    entity: 'shop',
    id: 1,
    name: 'Экспрессович',
    business_id: 2,
    business_name: 'Балякин Михаил Юрьевич',
    type,
    ...outletInfo,
});

const firstPartDefaultOffer = (isExpress = false) => createOffer({
    ...offerExpressMock,
    benefit: {
        type: 'default',
        description: 'Хорошая цена от надёжного магазина',
        isPrimary: true,
    },
    cpa: 'real',
    offerColor: 'blue',
    delivery: delivery({deliveryPartnerTypes: 'SHOP', isExpress}),
    supplier: supplier({type: '1'}),
    shop,
    ownMarketPlace: true,
    bundleSettings: {
        quantityLimit: {
            step: 1,
        },
    },
    cpc: 'abcd0',
}, offerExpressMock.wareId);

const expressCpaOffer = createOffer({
    ...offerExpressMock,
    benefit: {
        type: 'express-cpa',
        description: 'Хорошая цена от надёжного магазина',
        isPrimary: true,
    },
    cpa: 'real',
    offerColor: 'blue',
    delivery: delivery({deliveryPartnerTypes: 'SHOP', isExpress: true}),
    supplier: supplier({type: '1'}),
    shop,
    ownMarketPlace: true,
    bundleSettings: {
        quantityLimit: {
            step: 1,
        },
    },
    cpc: 'abcd1',
    prices: {
        currency: 'RUR',
        value: '7500',
        isDeliveryIncluded: false,
        rawValue: '7500',
    },
});
const expectedSecondDOMainPriceText = '7 500 ₽/ уп';

const createProductWithExpressCpaOfferState = ({isExpressDefaultOffer = false}) => mergeState([
    {
        data: {
            search: {
                totalOffersBeforeFilters: 2,
                total: 2,
                totalOffers: 2,
            },
        },
    },
    product,
    firstPartDefaultOffer(isExpressDefaultOffer),
    expressCpaOffer,
]);

const topState = mergeState([
    {
        data: {
            search: {
                totalOffersBeforeFilters: 3,
                total: 2,
                totalOffers: 2,
            },
        },
    },
    product,
    firstPartDefaultOffer(false),
    expressCpaOffer,
    offerTop6,
]);

const reportState = createProductWithExpressCpaOfferState({isExpressDefaultOffer: false});
const pageRoute = {
    productId,
    slug,
};
export {
    reportState,
    topState,
    pageRoute,
    expectedFirstDOMainPriceText,
    expectedSecondDOMainPriceText,
    expectedReferencePriceText,
    expectedOfferPriceText,
};
