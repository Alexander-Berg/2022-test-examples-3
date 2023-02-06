/* eslint-disable max-len */
import { REPORT_DEV_HOST, REPORT_DEV_PORT, REPORT_DEV_PATH } from '../../../../../../src/env';

const HOST = `${REPORT_DEV_HOST}:${REPORT_DEV_PORT}`;

const ROUTE = new RegExp(`/${REPORT_DEV_PATH}`);

const RESPONSE = {
    search: {
        total: 181,
        totalOffers: 77,
        totalFreeOffers: 0,
        totalOffersBeforeFilters: 82,
        totalModels: 104,
        totalPassedAllGlFilters: 61,
        adult: false,
        restrictionAge18: false,
        view: 'list',
        salesDetected: true,
        maxDiscountPercent: 15,
        shops: 18,
        totalShopsBeforeFilters: 20,
        cpaCount: 20,
        isParametricSearch: true,
        isMostlyFashion: false,
        category: {
            cpaType: 'cpc_and_cpa',
        },
        isDeliveryIncluded: false,
        isPickupIncluded: false,
        results: [
            {
                classifierMagicId: '225f2cce080d641d3b4484f9cb451f53',
                entity: 'offer',
                trace: {
                    fullFormulaInfo: [
                        {
                            tag: 'CpcBuy',
                            name: 'MNA_DO_20190325_simple_factors_6w_shops99m_QuerySoftMax',
                            value: '0.632031',
                        },
                    ],
                },
                vendor: {
                    entity: 'vendor',
                    id: 686784,
                    name: 'Western Digital',
                    slug: 'western-digital',
                    description:
                        'ние и внешние накопители, сетевое оборудование и бытовую электронику под брендами WD, HGST, Sandisk и G-Technology.',
                    website: 'http://www.wdc.com',
                    logo: {
                        entity: 'picture',
                        url: '//avatars.mds.yandex.net/get-mpic/1565610/img_id6909319395066389618.png/orig',
                        thumbnails: [],
                        signatures: [],
                    },
                    filter: '7893318:686784',
                },
                titles: {
                    raw: 'Жесткий диск Western Digital WD Black 1 ТБ WD10SPSX',
                    highlighted: [
                        {
                            value: 'Жесткий диск Western Digital WD Black 1 ТБ WD10SPSX',
                        },
                    ],
                },
                titlesWithoutVendor: {
                    raw: 'Жесткий диск NBook HDD 2.5" 1Tb, SATA-III, Western Digital, 64Mb, 7200rpm, Black WD10SPSX',
                    highlighted: [
                        {
                            value:
                                'Жесткий диск NBook HDD 2.5" 1Tb, SATA-III, Western Digital, 64Mb, 7200rpm, Black WD10SPSX',
                        },
                    ],
                },
                slug: 'zhestkii-disk-western-digital-wd-black-1-tb-wd10spsx',
                description:
                    'Соответствующий 2.5-дюймовому форм-фактору 1-терабайтный жесткий диск WD Black [WD10SPSX] – модель от одного из признанных мировых лидеров в области производства оборудования рассматриваемого класса. Устройство станет отличным выбором для пользователей мобильных компьютеров, обладающих совместимостью с накопителями, толщина корпуса которых равна 7 мм. Уровень производительности модели достаточен для решения любых универсальных задач.',
                eligibleForBookingInUserRegion: false,
                categories: [
                    {
                        entity: 'category',
                        id: 91033,
                        nid: 26912810,
                        name: 'Внутренние жесткие диски',
                        slug: 'vnutrennie-zhestkie-diski',
                        fullName: 'Внутренние жесткие диски',
                        type: 'guru',
                        cpaType: 'cpc_and_cpa',
                        isLeaf: true,
                        kinds: [],
                    },
                ],
                cpc:
                    'HHtuEk5O_nmib0hMXEBODQ9bk8T0OzRkZvuKJvRqm5CJGGCGxYw5zxKCwWbHrt2Tx4VoaSK7TfM8FnaMKUQ3U_8Mx883HE-kBfM_c9ZxJCM7g-6DJkIhSzEC3DX-cTdWZ6LJy_F-DbMhPgSbU53cgpinpjip58sd',
                urls: {
                    direct:
                        'https://market.yandex.ru/product/758044008?offerid=U28Zwbwp4q58vnicyWpfSQ&sku=758044008&clid=2210590&cpc=HHtuEk5O_nmib0hMXEBODQ9bk8T0OzRkZvuKJvRqm5CJGGCGxYw5zxKCwWbHrt2Tx4VoaSK7TfM8FnaMKUQ3U_8Mx883HE-kBfM_c9ZxJCM7g-6DJkIhSzEC3DX-cTdWZ6LJy_F-DbMhPgSbU53cgpinpjip58sd',
                },
                urlsByPp: {
                    '491': {
                        direct:
                            'https://market.yandex.ru/product/758044008?offerid=U28Zwbwp4q58vnicyWpfSQ&sku=758044008&clid=2210590&cpc=HHtuEk5O_nmib0hMXEBODQ9bk8T0OzRkZvuKJvRqm5CJGGCGxYw5zxKCwWbHrt2Tx4VoaSK7TfM8FnaMKUQ3U_8Mx883HE-kBfM_c9ZxJCM7g-6DJkIhSzEC3DX-cTdWZ6LJy_F-DbMhPgSbU53cgpinpjip58sd',
                    },
                },
                navnodes: [
                    {
                        entity: 'navnode',
                        id: 26912810,
                        name: 'Внутренние жесткие диски',
                        slug: 'vnutrennie-zhestkie-diski',
                        fullName: 'Внутренние жесткие диски',
                        isLeaf: true,
                        tags: ['cehac'],
                        rootNavnode: {},
                    },
                ],
                pictures: [
                    {
                        entity: 'picture',
                        original: {
                            containerWidth: 499,
                            containerHeight: 694,
                            url: '//avatars.mds.yandex.net/get-mpic/4932805/img_id7007251197463818917.jpeg/orig',
                            width: 499,
                            height: 694,
                        },
                        thumbnails: [
                            {
                                containerWidth: 50,
                                containerHeight: 50,
                                url: '//avatars.mds.yandex.net/get-mpic/4932805/img_id7007251197463818917.jpeg/50x50',
                                width: 35,
                                height: 50,
                            },
                            {
                                containerWidth: 55,
                                containerHeight: 70,
                                url: '//avatars.mds.yandex.net/get-mpic/4932805/img_id7007251197463818917.jpeg/55x70',
                                width: 50,
                                height: 70,
                            },
                            {
                                containerWidth: 60,
                                containerHeight: 80,
                                url: '//avatars.mds.yandex.net/get-mpic/4932805/img_id7007251197463818917.jpeg/60x80',
                                width: 57,
                                height: 80,
                            },
                            {
                                containerWidth: 74,
                                containerHeight: 100,
                                url: '//avatars.mds.yandex.net/get-mpic/4932805/img_id7007251197463818917.jpeg/74x100',
                                width: 71,
                                height: 100,
                            },
                            {
                                containerWidth: 75,
                                containerHeight: 75,
                                url: '//avatars.mds.yandex.net/get-mpic/4932805/img_id7007251197463818917.jpeg/75x75',
                                width: 53,
                                height: 75,
                            },
                            {
                                containerWidth: 90,
                                containerHeight: 120,
                                url: '//avatars.mds.yandex.net/get-mpic/4932805/img_id7007251197463818917.jpeg/90x120',
                                width: 86,
                                height: 120,
                            },
                            {
                                containerWidth: 100,
                                containerHeight: 100,
                                url: '//avatars.mds.yandex.net/get-mpic/4932805/img_id7007251197463818917.jpeg/100x100',
                                width: 71,
                                height: 100,
                            },
                            {
                                containerWidth: 120,
                                containerHeight: 160,
                                url: '//avatars.mds.yandex.net/get-mpic/4932805/img_id7007251197463818917.jpeg/120x160',
                                width: 115,
                                height: 160,
                            },
                            {
                                containerWidth: 150,
                                containerHeight: 150,
                                url: '//avatars.mds.yandex.net/get-mpic/4932805/img_id7007251197463818917.jpeg/150x150',
                                width: 107,
                                height: 150,
                            },
                            {
                                containerWidth: 180,
                                containerHeight: 240,
                                url: '//avatars.mds.yandex.net/get-mpic/4932805/img_id7007251197463818917.jpeg/180x240',
                                width: 172,
                                height: 240,
                            },
                            {
                                containerWidth: 190,
                                containerHeight: 250,
                                url: '//avatars.mds.yandex.net/get-mpic/4932805/img_id7007251197463818917.jpeg/190x250',
                                width: 179,
                                height: 250,
                            },
                            {
                                containerWidth: 200,
                                containerHeight: 200,
                                url: '//avatars.mds.yandex.net/get-mpic/4932805/img_id7007251197463818917.jpeg/200x200',
                                width: 143,
                                height: 200,
                            },
                            {
                                containerWidth: 240,
                                containerHeight: 320,
                                url: '//avatars.mds.yandex.net/get-mpic/4932805/img_id7007251197463818917.jpeg/240x320',
                                width: 230,
                                height: 320,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 300,
                                url: '//avatars.mds.yandex.net/get-mpic/4932805/img_id7007251197463818917.jpeg/300x300',
                                width: 215,
                                height: 300,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 400,
                                url: '//avatars.mds.yandex.net/get-mpic/4932805/img_id7007251197463818917.jpeg/300x400',
                                width: 287,
                                height: 400,
                            },
                            {
                                containerWidth: 600,
                                containerHeight: 600,
                                url: '//avatars.mds.yandex.net/get-mpic/4932805/img_id7007251197463818917.jpeg/600x600',
                                width: 431,
                                height: 600,
                            },
                        ],
                        signatures: [],
                    },
                ],
                meta: {},
                externalData: {},
                wareId: 'U28Zwbwp4q58vnicyWpfSQ',
                prices: {
                    currency: 'RUR',
                    value: '6757',
                    isDeliveryIncluded: false,
                    isPickupIncluded: false,
                    rawValue: '6757',
                },
                marketSkuCreator: 'market',
                modelAwareTitles: {
                    raw: 'Жесткий диск Western Digital WD Black 1 ТБ WD10SPSX',
                    highlighted: [
                        {
                            value: 'Жесткий диск Western Digital WD Black 1 ТБ WD10SPSX',
                        },
                    ],
                },
                specs: {
                    internal: [],
                },
                model: {
                    id: 758044008,
                },
                isCutPrice: false,
                delivery: {
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
                        type: 6,
                        subtitle: 'Москва и Московская область, Россия',
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
                        type: 3,
                    },
                    isPriorityRegion: true,
                    isCountrywide: true,
                    isAvailable: true,
                    hasPickup: false,
                    hasLocalStore: false,
                    hasPost: false,
                    isForcedRegion: false,
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
                        type: 6,
                        subtitle: 'Москва и Московская область, Россия',
                    },
                    availableServices: [
                        {
                            serviceId: 1006360,
                            serviceName: 'Экспресс-доставка Яндекса',
                        },
                    ],
                    isFree: false,
                    isDownloadable: false,
                    inStock: true,
                    postAvailable: true,
                    isExpress: true,
                    isEda: false,
                    options: [
                        {
                            price: {
                                currency: 'RUR',
                                value: '149',
                                isDeliveryIncluded: false,
                                isPickupIncluded: false,
                            },
                            dayFrom: 0,
                            dayTo: 0,
                            orderBefore: '23',
                            orderBeforeMin: '59',
                            isDefault: true,
                            serviceId: '1006360',
                            tariffId: 100796,
                            isTryingAvailable: false,
                            paymentMethods: ['YANDEX', 'CASH_ON_DELIVERY', 'CARD_ON_DELIVERY'],
                            partnerType: 'regular',
                        },
                    ],
                    deliveryPartnerTypes: ['YANDEX_MARKET'],
                    betterWithPlus: false,
                },
                cpa: 'real',
                fee: '0.0000',
                feeSum: '0',
                feeShow:
                    'OTNDnItfwROQcHB5I9mm3Y_DFjMrjsW78dHzHHU7AR3uBgO3Dz-LcVtDx3-m-udnjwUTShjnjDAgfSc8vwrmAbRSVUQ6lQtSrYJcQbwmJl7IiEBWOlsdMZEumEaAr2y37ovBN6M2_t96IoNhoJ5gmUz96FSJCZs5suq-x-7rAmrqiUMKTMtxDA,,',
                shop: {
                    entity: 'shop',
                    id: 431782,
                    name: 'Яндекс.Маркет',
                    business_id: 921035,
                    business_name: 'Яндекс.Маркет',
                    slug: 'yandex-market',
                    gradesCount: 524748,
                    overallGradesCount: 524748,
                    qualityRating: 2,
                    isGlobal: false,
                    isCpaPrior: false,
                    isCpaPartner: true,
                    taxSystem: 'OSN',
                    isNewRating: true,
                    newGradesCount: 524748,
                    newQualityRating: 4.515155846,
                    newQualityRating3M: 1.575654152,
                    ratingToShow: 1.575654152,
                    ratingType: 3,
                    newGradesCount3M: 1758,
                    status: 'actual',
                    cutoff: '',
                    loyalty_program_status: 'DISABLED',
                    isEats: false,
                    logo: {
                        entity: 'picture',
                        width: 71,
                        height: 14,
                        url:
                            '//avatars.mds.yandex.net/get-market-shop-logo/1615984/2a00000178597b92092f50ce72bad6f86dca/orig',
                        extension: 'SVG',
                    },
                    feed: {
                        id: '475690',
                        offerId: '1017560.251996',
                        categoryId: '2417',
                    },
                    outletsCount: 0,
                    storesCount: 0,
                    pickupStoresCount: 0,
                    depotStoresCount: 0,
                    postomatStoresCount: 0,
                    bookNowStoresCount: 0,
                    subsidies: true,
                    hasSafetyGuarantee: true,
                    domainUrl: 'pokupki.market.yandex.ru',
                    deliveryVat: 'NO_VAT',
                    createdAt: '2017-08-14T15:40:13',
                    mainCreatedAt: '2017-08-14T15:40:13',
                    returnDeliveryAddress: 'ООО «Яндекс.Маркет», а/я 245, Московский АСЦ, ОПС 140961',
                    homeRegion: {
                        entity: 'region',
                        id: 225,
                        name: 'Россия',
                        lingua: {
                            name: {},
                        },
                        type: 0,
                    },
                },
                supplier: {
                    entity: 'shop',
                    id: 1098222,
                    name: 'Funny Play',
                    business_id: 788934,
                    business_name: 'Funny Play',
                    gradesCount: 1400,
                    overallGradesCount: 1400,
                    qualityRating: 5,
                    isGlobal: false,
                    isCpaPrior: false,
                    type: '3',
                    taxSystem: 'PSN',
                    isNewRating: true,
                    newGradesCount: 1400,
                    newQualityRating: 4.860714286,
                    newQualityRating3M: 4.888082902,
                    ratingToShow: 4.888082902,
                    ratingType: 3,
                    newGradesCount3M: 965,
                    status: '',
                    cutoff: '',
                    warehouseId: 129375,
                    workSchedule: 'Пн-Вс: 10:00-20:00',
                    workScheduleList: [
                        {
                            day: 0,
                            from: {
                                hour: 12,
                                minute: 0,
                            },
                            to: {
                                hour: 20,
                                minute: 0,
                            },
                        },
                        {
                            day: 1,
                            from: {
                                hour: 12,
                                minute: 0,
                            },
                            to: {
                                hour: 20,
                                minute: 0,
                            },
                        },
                        {
                            day: 2,
                            from: {
                                hour: 12,
                                minute: 0,
                            },
                            to: {
                                hour: 20,
                                minute: 0,
                            },
                        },
                        {
                            day: 3,
                            from: {
                                hour: 12,
                                minute: 0,
                            },
                            to: {
                                hour: 20,
                                minute: 0,
                            },
                        },
                        {
                            day: 4,
                            from: {
                                hour: 12,
                                minute: 0,
                            },
                            to: {
                                hour: 20,
                                minute: 0,
                            },
                        },
                        {
                            day: 5,
                            from: {
                                hour: 12,
                                minute: 0,
                            },
                            to: {
                                hour: 20,
                                minute: 0,
                            },
                        },
                        {
                            day: 6,
                            from: {
                                hour: 12,
                                minute: 0,
                            },
                            to: {
                                hour: 20,
                                minute: 0,
                            },
                        },
                    ],
                    currentWorkSchedule: {
                        from: {
                            hour: 12,
                            minute: 0,
                        },
                        to: {
                            hour: 20,
                            minute: 0,
                        },
                    },
                    operationalRating: {
                        calcTime: 1645664554450,
                        lateShipRate: 0.645995,
                        cancellationRate: 0.397351,
                        returnRate: 0,
                        total: 98.36,
                        crossdockPlanFactRate: 0,
                        crossdockLateShipRate: 0,
                        crossdockReturnRate: 0,
                        ffPlanFactRate: 0,
                        ffReturnRate: 0,
                        ffLateShipRate: 0,
                        dsbsLateDeliveryRate: 0,
                        dsbsCancellationRate: 0,
                        dsbsReturnRate: 0,
                    },
                    loyalty_program_status: 'DISABLED',
                    isEats: false,
                },
                realShop: {
                    entity: 'shop',
                    id: 1098222,
                    name: 'Funny Play',
                    business_id: 788934,
                    business_name: 'Funny Play',
                    gradesCount: 1400,
                    overallGradesCount: 1400,
                    qualityRating: 5,
                    isGlobal: false,
                    isCpaPrior: false,
                    type: '3',
                    taxSystem: 'PSN',
                    isNewRating: true,
                    newGradesCount: 1400,
                    newQualityRating: 4.860714286,
                    newQualityRating3M: 4.888082902,
                    ratingToShow: 4.888082902,
                    ratingType: 3,
                    newGradesCount3M: 965,
                    status: '',
                    cutoff: '',
                    warehouseId: 129375,
                    workSchedule: 'Пн-Вс: 10:00-20:00',
                    workScheduleList: [
                        {
                            day: 0,
                            from: {
                                hour: 12,
                                minute: 0,
                            },
                            to: {
                                hour: 20,
                                minute: 0,
                            },
                        },
                        {
                            day: 1,
                            from: {
                                hour: 12,
                                minute: 0,
                            },
                            to: {
                                hour: 20,
                                minute: 0,
                            },
                        },
                        {
                            day: 2,
                            from: {
                                hour: 12,
                                minute: 0,
                            },
                            to: {
                                hour: 20,
                                minute: 0,
                            },
                        },
                        {
                            day: 3,
                            from: {
                                hour: 12,
                                minute: 0,
                            },
                            to: {
                                hour: 20,
                                minute: 0,
                            },
                        },
                        {
                            day: 4,
                            from: {
                                hour: 12,
                                minute: 0,
                            },
                            to: {
                                hour: 20,
                                minute: 0,
                            },
                        },
                        {
                            day: 5,
                            from: {
                                hour: 12,
                                minute: 0,
                            },
                            to: {
                                hour: 20,
                                minute: 0,
                            },
                        },
                        {
                            day: 6,
                            from: {
                                hour: 12,
                                minute: 0,
                            },
                            to: {
                                hour: 20,
                                minute: 0,
                            },
                        },
                    ],
                    currentWorkSchedule: {
                        from: {
                            hour: 12,
                            minute: 0,
                        },
                        to: {
                            hour: 20,
                            minute: 0,
                        },
                    },
                    operationalRating: {
                        calcTime: 1645664554450,
                        lateShipRate: 0.645995,
                        cancellationRate: 0.397351,
                        returnRate: 0,
                        total: 98.36,
                        crossdockPlanFactRate: 0,
                        crossdockLateShipRate: 0,
                        crossdockReturnRate: 0,
                        ffPlanFactRate: 0,
                        ffReturnRate: 0,
                        ffLateShipRate: 0,
                        dsbsLateDeliveryRate: 0,
                        dsbsCancellationRate: 0,
                        dsbsReturnRate: 0,
                    },
                    loyalty_program_status: 'DISABLED',
                    isEats: false,
                },
                weight: '0.1',
                dimensions: ['12', '16', '3'],
                returnPolicy: '7d',
                marketSku: '758044008',
                sku: '758044008',
                ownMarketPlace: true,
                offerColor: 'blue',
                isFreeOffer: false,
                isAnalogOffer: false,
                isDefaultOffer: true,
                supplierSku: '251996',
                shopSku: '251996',
                manufacturer: {
                    entity: 'manufacturer',
                    warranty: true,
                    code: 'WD10SPSX',
                },
                seller: {
                    price: '6757',
                    currency: 'RUR',
                    sellerToUserExchangeRate: 1,
                },
                payments: {
                    deliveryCard: false,
                    deliveryCash: false,
                    prepaymentCard: true,
                    prepaymentOther: false,
                },
                yandexBnplInfo: {
                    enabled: false,
                },
                isRecommendedByVendor: false,
                benefit: {
                    type: 'default',
                    nestedTypes: ['default'],
                    description: 'Хорошая цена от надёжного магазина',
                    isPrimary: true,
                },
                prepayEnabled: false,
                promoCodeEnabled: true,
                vat: 'NO_VAT',
                cargoType: 200,
                cargoTypes: [200, 310],
                largeSize: false,
                isFulfillment: false,
                stockStoreCount: 33,
                isAdult: false,
                restrictedAge18: false,
                isSMB: false,
                atSupplierWarehouse: true,
                fulfillmentWarehouse: 129375,
                productInfo: {
                    id: 758044008,
                    type: 'model',
                    opinions: 25,
                    rating: 4.5,
                    preciseRating: 4.58,
                    ratingCount: 59,
                    reviews: 0,
                    skuStats: {
                        totalCount: 1,
                        beforeFiltersCount: 1,
                        afterFiltersCount: 1,
                    },
                },
                refMinPrice: {
                    currency: 'RUR',
                    value: '4790',
                },
                commonMinPrice: {
                    currency: 'RUR',
                    value: '4790',
                },
                isGoldenMatrix: false,
                elasticity: [
                    {
                        demandMean: '7.86069',
                        priceVariant: {
                            currency: 'RUR',
                            value: '2388',
                        },
                    },
                    {
                        demandMean: '6.86249',
                        priceVariant: {
                            currency: 'RUR',
                            value: '2628',
                        },
                    },
                    {
                        demandMean: '6.44773',
                        priceVariant: {
                            currency: 'RUR',
                            value: '2868',
                        },
                    },
                    {
                        demandMean: '5.90121',
                        priceVariant: {
                            currency: 'RUR',
                            value: '3108',
                        },
                    },
                    {
                        demandMean: '4.13682',
                        priceVariant: {
                            currency: 'RUR',
                            value: '3348',
                        },
                    },
                    {
                        demandMean: '2.11742',
                        priceVariant: {
                            currency: 'RUR',
                            value: '3588',
                        },
                    },
                    {
                        demandMean: '1.14872',
                        priceVariant: {
                            currency: 'RUR',
                            value: '3828',
                        },
                    },
                    {
                        demandMean: '0.846221',
                        priceVariant: {
                            currency: 'RUR',
                            value: '4068',
                        },
                    },
                    {
                        demandMean: '0.809008',
                        priceVariant: {
                            currency: 'RUR',
                            value: '4308',
                        },
                    },
                    {
                        demandMean: '0.764831',
                        priceVariant: {
                            currency: 'RUR',
                            value: '4548',
                        },
                    },
                ],
                shop_category_path: 'Внутренние жесткие диски\\2.5"\\2.5" 1TB - 2Tb',
                shop_category_path_ids: '97\\99\\2417',
                isFashion: false,
                isFashionPremium: false,
                isPartialCheckoutAvailable: false,
            },
        ],
        isFashion: false,
        isFashionPremium: false,
        isPartialCheckoutAvailable: false,
    },
    intents: [],
    sorts: [],
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
