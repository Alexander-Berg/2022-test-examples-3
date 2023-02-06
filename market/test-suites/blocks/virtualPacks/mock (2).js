import {mergeState, createProduct, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';
import {offerMock} from '@self/project/src/spec/hermione/fixtures/offer/offer';

const minimum = 4;
const minCount = minimum - 1;

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

const delivery = ({deliveryPartnerTypes = 'SHOP'}) => ({
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
    isExpress: false,
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

const firstPartDefaultOffer = () => createOffer({
    ...offerMock,
    payments: {
        deliveryCard: false,
        deliveryCash: false,
        prepaymentCard: true,
        prepaymentOther: false,
    },
    prices: {
        currency: 'RUR',
        value: '7290',
        isDeliveryIncluded: false,
        rawValue: '7290',
    },
    benefit: {
        type: 'default',
        description: 'Хорошая цена от надёжного магазина',
        isPrimary: true,
    },
    cpa: 'real',
    offerColor: 'blue',
    delivery: delivery({deliveryPartnerTypes: 'SHOP'}),
    supplier: supplier({type: '1'}),
    shop,
    ownMarketPlace: true,
    bundleSettings: {
        quantityLimit: {
            minimum: minimum,
            step: 1,
            maximum: 999,
        },
    },
    stockStoreCount: minCount,
    cpc: 'abcd0',
}, offerMock.wareId);

const state = mergeState([
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
    firstPartDefaultOffer(),
]);

const route = {
    productId,
    slug,
};

export {
    state,
    route,
    minCount,
};
