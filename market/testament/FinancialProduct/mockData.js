import {PAGE_IDS_COMMON, PAGE_IDS_DESKTOP} from '@self/root/src/constants/pageIds';
import {FINANCIAL_PRODUCTS} from '@self/root/src/entities/financialProducts';

export const page = {
    id: {
        checkout: PAGE_IDS_DESKTOP.YANDEX_MARKET_MY_CHECKOUT,
        offer: PAGE_IDS_COMMON.OFFER,
        product: PAGE_IDS_DESKTOP.YANDEX_MARKET_PRODUCT,
        fake: 'fake:page',
    },
    routes: {
        checkout: '/my/checkout',
    },
    regionId: 10,
    pageName: 'PRODUCT_BUNDLE_MAIN',
};

const wareId = '4444';

export const offerShowPlace = {
    id: 'OTNDnItfwROQcHB5I9mm3Wf0hfLKOwLRjsnnzqBwxhMU87ravkcM8PTGhK6YgeB4ILTeT6gKV9giCSwsy6er8UvkQ__nUawzLaNDU30oqJ4VdxHv9lpXppo3IloexjcjyEQgFROhVRDku24CbKOcS7MIemqVVZLvG-qefObbYho,',
    offerId: '0xEncI1U1r-EZfZGb8NCzA',
    cpc: 'Fx6A3CyJ41T0bDGYD8oJGV2d3QX8t5qLywHpT5Ui4LbLYYNBCqMVGX_QFb-2gLbhU2rnPXpofM2IivKBHZbWibNI2Qc-jp8PgOLSH4o1W6viCjZak2nCceLSV2f2EONMQWNKQtXXpcRB_vc710OmJduNBOiuvx5vwvFXj_oJz1GhFvdkFDtzuA,,',
};

const paymentsBnpl = [{
    amount: 3000,
    datetime: '2022-12-16',
    status: 'coming',
}, {
    amount: 1000,
    datetime: '2022-12-20',
    status: 'coming',
}, {
    amount: 1000,
    datetime: '2022-12-24',
    status: 'coming',
}, {
    amount: 1000,
    datetime: '2022-12-28',
    status: 'coming',
}];

const bnplPlan1 = {
    type: 'bnpl',
    constructor: '2month',
    visualProperties: {
        nextDatesDescription: 'еще 5 платежей',
        nextPaymentsDescription: 'по 2222',
        colors: {},
        shortTitle: '2 месяца',
    },
    fee: 0,
    payments: paymentsBnpl,
    detailsUrl: 'https://mocked.url#longsplit',
};

export const bnplInfoManyPlans = {
    defaultPlan: '2month',
    plans: [bnplPlan1, {
        constructor: '4month',
        visualProperties: {
            nextDatesDescription: 'еще 5 платежей',
            nextPaymentsDescription: 'по 2222',
            colors: {},
            shortTitle: '4 месяца',
        },
        fee: 444,
        payments: paymentsBnpl,
        detailsUrl: 'https://mocked.url#longsplit',
    }],
};

export const bnplInfoOnePlan = {
    defaultPlan: '2month',
    plans: [bnplPlan1],
};

export const offer = {
    id: offerShowPlace.offerId,
    wareId,
    cpa: 'real',
    creditInfo: {
        termRange: {
            min: 24,
            max: 24,
        },
        bestOptionId: '1',
        initialPayment: {
            currency: 'RUR',
            value: '0',
        },
        monthlyPayment: {
            currency: 'RUR',
            value: '5717',
        },
    },
    bnplAvailable: false,
    price: {
        value: 102490,
        currency: 'RUR',
    },
    installmentsInfo: {
        selected: {
            term: 3,
            monthlyPayment: {
                currency: 'RUR',
                value: 36250,
            },
        },
        options: [
            {
                term: 3,
                monthlyPayment: {
                    currency: 'RUR',
                    value: 36250,
                },
            },
        ],
    },
    financialProductPriorities: [
        [
            'TINKOFF_INSTALLMENTS',
            'TINKOFF_CREDIT',
        ],
        [
            'TINKOFF_CREDIT',
        ],
    ],
    shopId: '431782',
    feed: {
        id: '123',
    },
    titles: {
        raw: 'title',
    },
    categoryIds: [1234325],
};

export const params = {
    offerId: offer.id,
    cpc: offerShowPlace.cpc,
    shopId: offer.shopId,
    productId: 1414986413,
    skuId: '101417664739',
};

const financialProductsActive = {
    BNPL: true,
    TINKOFF_INSTALLMENTS: true,
    TINKOFF_CREDIT: true,
};

export const financialProducts = [
    'TINKOFF_INSTALLMENTS',
    'TINKOFF_CREDIT',
];

const monthlyPayment = {
    currency: 'RUR',
    value: '5717',
};

export const result = {
    visibleSearchResultId: null,
    offerId: offerShowPlace.offerId,
    wareId,
    offerShowPlaceId: offerShowPlace.id,
    regionId: page.regionId,

    installmentsInfo: offer.installmentsInfo,
    creditInfo: offer.creditInfo,
    financialProductPriorities: offer.financialProductPriorities,

    financialProductsActive,
    financialProducts,
    monthlyPayment,
};

export const restResult = {
    skuId: undefined,
};

export const bnplResult = {
    ...result,
    financialProducts: [
        FINANCIAL_PRODUCTS.BNPL,
        ...financialProducts,
    ],
};

export const productResult = {
    ...result,
    visibleSearchResultId: 'wgal70durmo',
};

export const productBnplResult = {
    ...productResult,
    financialProducts: [
        FINANCIAL_PRODUCTS.BNPL,
        ...financialProducts,
    ],
};

export const collections = {
    offer: {[offer.id]: offer},
    offerShowPlace: {[offerShowPlace.id]: offerShowPlace},
    product: {
        1414986413: {
            entity: 'product',
            categoryIds: [
                91491,
            ],
            navnodeIds: [
                26893750,
            ],
            offersCount: 9,
            offersWithCutPriceCount: 0,
            showReview: false,
            reviewsCount: 255,
            ratingCount: 698,
            overviewsCount: 14,
            reviewIds: 14,
            specs: {},
            vendorId: 153043,
            colorVendorCount: 5,
            links: [
                {
                    type: 'filter',
                    hid: '91491',
                    filter: '12782797',
                    xslname: 'vendor_line',
                    values: [
                        '25786890',
                    ],
                },
            ],
            id: 1414986413,
            description: 'диагональ экрана: 6.10", количество основных камер: 2, память: 256 ГБ, 128 ГБ, 512 ГБ, оперативная память: 4 ГБ, емкость аккумулятора: 3240 мА⋅ч, разрешение экрана: 2532x1170, NFC, 4G LTE, 5G',
            fullDescription: 'Смартфон Apple iPhone 13 128Gb Blue, J/A',
            isNew: false,
            modelName: {
                raw: 'iPhone 13',
            },
            pictures: [],
            prices: {
                min: '105690',
                max: '125607',
                currency: 'RUR',
                avg: '105690',
            },
            rating: 5,
            preciseRating: 4.75,
            retailersCount: 9,
            reasonsToBuy: [],
            skuStats: {
                totalCount: 1,
                beforeFiltersCount: 1,
                afterFiltersCount: 1,
            },
            titles: {
                raw: 'Смартфон Apple iPhone 13',
                highlighted: [
                    {
                        value: 'Смартфон Apple iPhone 13',
                    },
                ],
            },
            titlesWithoutVendor: {
                raw: 'Смартфон  iPhone 13',
                highlighted: [
                    {
                        value: 'Смартфон  iPhone 13',
                    },
                ],
            },
            type: 'model',
            slug: 'smartfon-apple-iphone-13',
            filters: [],
            hasGoodCpa: false,
            hasExpressOffer: false,
        },
    },
    category: {
        91491: {
            entity: 'category',
            id: 91491,
            nid: 26893750,
            name: 'Мобильные телефоны',
            slug: 'mobilnye-telefony-v-orle',
            fullName: 'Мобильные телефоны',
            type: 'guru',
            cpaType: 'cpc_and_cpa',
            isLeaf: true,
            kinds: [],
        },
    },
    navnode: {
        3938: {
            id: 3938,
            isDepartment: false,
            pictures: [],
            tags: [],
        },
        26893750: {
            id: 26893750,
            isDepartment: false,
            pictures: [],
            tags: [
                'cehac',
            ],
            entity: 'navnode',
            name: 'Смартфоны',
            fullName: 'Смартфоны',
            isLeaf: true,
            slug: 'smartfony-v-orle',
            rootNavnode: 3938,
        },
    },
    navnodePicture: {},
    vendor: {
        153043: {
            logo: {
                entity: 'picture',
                url: '//avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                thumbnails: [],
            },
            id: 153043,
            slug: 'apple',
            entity: 'vendor',
            filter: '7893318:153043',
            name: 'Apple',
            website: 'http://www.apple.com/ru',
        },
    },
    review: {},
    ratingFactor: {},
};

export const FINANCIAL_PRODUCTS_BNPL = FINANCIAL_PRODUCTS.BNPL;
