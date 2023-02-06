import { YtObject } from '../../../src/infrastructure/data-mappers/DomainInfoDataMapperUtils';
import DomainInfo from '../../../src/domain/models/domain-info/DomainInfo';
import DomainInfoId from '../../../src/domain/models/domain-info/DomainInfoId';
import DeviceType from '../../../src/domain/models/DeviceType';
import DomainInfoType from '../../../src/domain/models/domain-info/DomainInfoType';
import DomainInfoStatus from '../../../src/domain/models/domain-info/DomainInfoStatus';
import Payload from '../../../src/domain/models/domain-info/Payload';
import DomainInfoGatewayUtils from '../../../src/infrastructure/gateways/DomainInfoGatewayUtils';

export const ytPlainObjects: YtObject[] = [
    {
        domain: 'ozon.ru',
        status: 'ok',
        type: 'shop',
        restricted: false,
        payload: {
            fixedHeaderSelector: '._3deb9b',
            'tablet-productPageSelector': 'TABLET',
            'tablet-cart': {
                urlTemplate: 'TABLET',
                multiplyItemsPrice: false,
                currency: 'TABLET',
                titles: 'TABLET',
                quantities: 'TABLET',
                prices: 'TABLET',
                totalPrices: 'TABLET',
            },
            'mobile-checkout': {
                urlTemplate: 'MOBILE',
                placeOrderBtn: 'MOBILE',
            },
            cart: {
                urlTemplate: '/cart',
                multiplyItemsPrice: true,
                currency: '.cart-item__column .price-block:first-child .price-block-part:first-child',
                titles: '.title span',
                quantities: '.static-quantity',
                prices: '.cart-item__column .price-block:first-child .price-block-part:first-child',
                totalPrices: '.total-middle-footer-text',
            },
            selector: {
                name: 'div.top > div.top-base-column-top > div h1, h1',
                isbn: '.isbn',
                pictures: '.magnifier-image.shown > img',
                category: '.b1f li, ol[itemscope="itemscope"] li[itemprop="itemListElement"] a span',
                price: '.top-sale-block > div > div> div> div> div:only-child',
                currency: '.top-sale-block > div > div> div> div> div:only-child',
                vendor: 'a[href*=brand]:not([data-test-id=detail-brand-logo])',
            },
            'mobile-productPageSelector': 'MOBILE',
            ajax: [
                {
                    actions: [
                        {
                            name: 'change',
                            conditions: [
                                {
                                    selector: '.top-sale-block',
                                    shouldBe: 'exist',
                                },
                            ],
                        },
                    ],
                    urlTemplates: [
                        '/context/detail/id/',
                    ],
                },
            ],
            'tablet-checkout': {
                urlTemplate: 'TABLET',
                placeOrderBtn: 'TABLET',
            },
            mobileFiexedSelector: '._3deb9b',
            mobileFixedSelector: '._7db0fb',
            checkout: {
                urlTemplate: '/order_done',
                placeOrderBtn: '.total .button.green.large.full-width',
            },
            'mobile-cart': {
                urlTemplate: 'MOBILE',
                multiplyItemsPrice: false,
                currency: 'MOBILE',
                titles: 'MOBILE',
                quantities: 'MOBILE',
                prices: 'MOBILE',
                totalPrices: 'MOBILE',
            },
            urlTemplates: [
                '/detail/id/',
            ],
            'tablet-selector': {
                name: 'TABLET',
                isbn: 'TABLET',
                pictures: 'TABLET',
                category: 'TABLET',
                price: 'TABLET',
                currency: 'TABLET',
                vendor: 'TABLET',
            },
            'mobile-selector': {
                name: 'MOBILE',
                isbn: 'MOBILE',
                pictures: 'MOBILE',
                category: 'MOBILE',
                price: 'MOBILE',
                currency: 'MOBILE',
                vendor: 'MOBILE',
            },
        },
        signature: '9e4a80a8e3b4cf8a4c8b9545e56f45c0',
        comments: [],
        rules: [
            'shop',
            'market-shop',
            'tablet-shop',
            'tablet-blacklisted',
            'mobile-shop',
        ],
    },
    {
        domain: 'ozes.ru',
        status: 'ok',
        type: 'shop',
        restricted: false,
        payload: {
            selector: {
                name: 'h1',
                pictures: '#pictureContainer  a > img',
                category: '#breadcrumbs',
                price: '.fixContainer a.price.changePrice',
                currency: '.fixContainer a.price.changePrice',
                vendor: '.propertyList a[href*="/brands/"]',
            },
            cart: {
                urlTemplate: '/cart/',
                multiplyItemsPrice: false,
                currency: '#allSum',
                titles: '#basketProductList  a.name > span',
                quantities: '.basketQty .qty',
                prices: '#basketProductList  a.price',
                totalPrices: '#allSum',
            },
            urlTemplates: [
                '/catalog/',
            ],
            checkout: {
                urlTemplate: 'ORDER_ID',
                placeOrderBtn: '#newOrder',
            },
        },
        signature: 'a43a7cd1069b64b4fb6bafbcd7405c8e',
        comments: [],
        rules: [
            'shop',
            'market-shop',
        ],
    },
    {
        domain: 'oxymix.ru',
        status: 'ok',
        type: 'shop',
        restricted: false,
        payload: {},
        signature: '99914b932bd37a50b983c5e7c90ae93b',
        comments: [],
        rules: [
            'shop',
            'market-toloka-shops',
        ],
    },
];

export const pgPlainDomainInfos: DomainInfo[] = [
    new DomainInfo({
        id: DomainInfoId.fromDomainKey('85e00506-7d67-469a-bdbb-882203b87019', DeviceType.DESKTOP, DomainInfoType.SHOP),
        domain: 'oxymix.ru',
        deviceType: DeviceType.DESKTOP,
        type: DomainInfoType.SHOP,
        status: DomainInfoStatus.OK,
        restricted: false,
        comments: [],
        rules: [
            'shop',
            'market-toloka-shops',
        ],
        payload: Payload.default().data,
    }),
    new DomainInfo({
        id: DomainInfoId.fromDomainKey('0a9e8c48-5104-4a8c-94dc-5d46ce687a0f', DeviceType.DESKTOP, DomainInfoType.SHOP),
        domain: 'ozes.ru',
        deviceType: DeviceType.DESKTOP,
        type: DomainInfoType.SHOP,
        status: DomainInfoStatus.OK,
        restricted: false,
        comments: [],
        rules: [
            'shop',
            'market-shop',
        ],
        payload: {
            cart: {
                prices: '#basketProductList  a.price',
                titles: '#basketProductList  a.name > span',
                currency: '#allSum',
                quantities: '.basketQty .qty',
                totalPrices: '#allSum',
                urlTemplate: '/cart/',
                multiplyItemsPrice: false,
            },
            checkout: {
                urlTemplate: 'ORDER_ID',
                placeOrderBtn: '#newOrder',
            },
            attributes: {
                name: 'h1',
                price: '.fixContainer a.price.changePrice',
                vendor: '.propertyList a[href*="/brands/"]',
                category: '#breadcrumbs',
                currency: '.fixContainer a.price.changePrice',
                pictures: '#pictureContainer  a > img',
            },
            urlTemplates: [
                '/catalog/',
            ],
        },
    }),
    new DomainInfo({
        id: DomainInfoId.fromDomainKey('57ef7d2b-c037-40cb-9969-6735e7964e4f', DeviceType.DESKTOP, DomainInfoType.SHOP),
        domain: 'ozon.ru',
        deviceType: DeviceType.DESKTOP,
        type: DomainInfoType.SHOP,
        status: DomainInfoStatus.OK,
        restricted: false,
        comments: [],
        rules: [
            'shop',
            'market-shop',
        ],
        payload: {
            ajax: [
                {
                    actions: [
                        {
                            name: 'change',
                            conditions: [
                                {
                                    selector: '.top-sale-block',
                                    shouldBe: 'exist',
                                },
                            ],
                        },
                    ],
                    urlTemplates: [
                        '/context/detail/id/',
                    ],
                },
            ],
            cart: {
                prices: '.cart-item__column .price-block:first-child .price-block-part:first-child',
                titles: '.title span',
                currency: '.cart-item__column .price-block:first-child .price-block-part:first-child',
                quantities: '.static-quantity',
                totalPrices: '.total-middle-footer-text',
                urlTemplate: '/cart',
                multiplyItemsPrice: true,
            },
            checkout: {
                urlTemplate: '/order_done',
                placeOrderBtn: '.total .button.green.large.full-width',
            },
            attributes: {
                isbn: '.isbn',
                name: 'div.top > div.top-base-column-top > div h1, h1',
                price: '.top-sale-block > div > div> div> div> div:only-child',
                vendor: 'a[href*=brand]:not([data-test-id=detail-brand-logo])',
                category: '.b1f li, ol[itemscope="itemscope"] li[itemprop="itemListElement"] a span',
                currency: '.top-sale-block > div > div> div> div> div:only-child',
                pictures: '.magnifier-image.shown > img',
            },
            urlTemplates: [
                '/detail/id/',
            ],
            fixedHeaderSelector: '._3deb9b',
            mobileFixedSelector: '._7db0fb',
            mobileFiexedSelector: '._3deb9b',
        },
    }),
    new DomainInfo({
        id: DomainInfoId.fromDomainKey('57ef7d2b-c037-40cb-9969-6735e7964e4f', DeviceType.MOBILE, DomainInfoType.SHOP),
        domain: 'ozon.ru',
        deviceType: DeviceType.MOBILE,
        type: DomainInfoType.SHOP,
        status: DomainInfoStatus.OK,
        restricted: false,
        comments: [],
        rules: [
            'shop',
        ],
        payload: {
            cart: {
                prices: 'MOBILE',
                titles: 'MOBILE',
                currency: 'MOBILE',
                quantities: 'MOBILE',
                totalPrices: 'MOBILE',
                urlTemplate: 'MOBILE',
                multiplyItemsPrice: false,
            },
            checkout: {
                urlTemplate: 'MOBILE',
                placeOrderBtn: 'MOBILE',
            },
            attributes: {
                isbn: 'MOBILE',
                name: 'MOBILE',
                price: 'MOBILE',
                vendor: 'MOBILE',
                category: 'MOBILE',
                currency: 'MOBILE',
                pictures: 'MOBILE',
            },
            productPageSelector: 'MOBILE',
        },
    }),
    new DomainInfo({
        id: DomainInfoId.fromDomainKey('57ef7d2b-c037-40cb-9969-6735e7964e4f', DeviceType.TABLET, DomainInfoType.SHOP),
        domain: 'ozon.ru',
        deviceType: DeviceType.TABLET,
        type: DomainInfoType.SHOP,
        status: DomainInfoStatus.OK,
        restricted: false,
        comments: [],
        rules: [
            'shop',
            'blacklisted',
        ],
        payload: {
            cart: {
                prices: 'TABLET',
                titles: 'TABLET',
                currency: 'TABLET',
                quantities: 'TABLET',
                totalPrices: 'TABLET',
                urlTemplate: 'TABLET',
                multiplyItemsPrice: false,
            },
            checkout: {
                urlTemplate: 'TABLET',
                placeOrderBtn: 'TABLET',
            },
            attributes: {
                isbn: 'TABLET',
                name: 'TABLET',
                price: 'TABLET',
                vendor: 'TABLET',
                category: 'TABLET',
                currency: 'TABLET',
                pictures: 'TABLET',
            },
            productPageSelector: 'TABLET',
        },
    }),
].map(d => d.clean(true));


export const oximixWithChangedRules = DomainInfo.createFrom(pgPlainDomainInfos[0], {
    id: DomainInfoId.fromDomainKey(pgPlainDomainInfos[0].domain, DeviceType.DESKTOP, DomainInfoType.SHOP),
    rules: [
        ...pgPlainDomainInfos[0].rules,
        'blacklisted',
    ],
}).clean(true);

export const getUpdatedSlice = (elem: DomainInfo): DomainInfo[] => {
    const index = pgPlainDomainInfos.findIndex(item => item.domainKey === elem.domainKey);
    const copy = Object.assign([], pgPlainDomainInfos);
    copy.splice(index, 1, elem);
    return copy;
};

export const ozonWithUpdatedPayload = DomainInfo.createFrom(pgPlainDomainInfos[3], {
    payload: {
        // @ts-ignore
        ...pgPlainDomainInfos[3].payload,
        productPageSelector: 'new-Mobile',
    },
}).clean(true);

const [aggregatedOzonResult] = DomainInfoGatewayUtils
    // @ts-ignore
    .mergeDomainInfos(Object.assign([], pgPlainDomainInfos).filter(d => d.domain === 'ozon.ru'));

export const mergedOzonWithUpdatedPayload = DomainInfo.createFrom(aggregatedOzonResult, {
    id: DomainInfoId.fromDomainKey(pgPlainDomainInfos[3].domain, DeviceType.DESKTOP, DomainInfoType.SHOP),
    payload: {
        ...aggregatedOzonResult.payload,
        'mobile-productPageSelector': 'new-Mobile',
    },
}).clean(true);
