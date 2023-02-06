import type {CheckouterOrder} from '~/app/bcm/checkouter/Backend/types';
import {Status} from '~/app/entities/order/types/status';
import {PaymentType, PaymentMethod} from '~/app/entities/order/types/payment';
import {OrderContext} from '~/app/entities/order/types/context';
import {SubStatus} from '~/app/entities/order/types/subStatus';
import {MarketColor} from '~/app/entities/market/types';
import {Vat} from '~/app/constants/vat';
import {DeliveryFeature, DeliveryType, Delivery} from '~/app/entities/delivery/types';

export default (draft: Partial<CheckouterOrder> = {}, delivery?: Partial<Delivery>): CheckouterOrder => ({
    id: 32849886,
    shopId: 11103659,
    businessId: 11103660,
    status: Status.Processing,
    substatus: SubStatus.ReadyToShip,
    archived: false,
    creationDate: '19-10-2021 15:31:46',
    creationDateTimestamp: '1634646706084',
    updateDate: '19-10-2021 21:00:22',
    updateDateTimestamp: '1634666422092',
    statusUpdateDate: '19-10-2021 15:32:19',
    statusUpdateDateTimestamp: '1634646739538',
    currency: 'RUR',
    buyerCurrency: 'RUR',
    exchangeRate: 1,
    itemsTotal: 861,
    buyerItemsTotal: 861,
    total: 960,
    buyerTotal: 960,
    buyer: {
        uid: 12345667,
        firstName: 'John',
        middleName: 'Ivanovich',
        lastName: 'Smith',
    },
    feeTotal: 0,
    paymentType: PaymentType.Prepaid,
    paymentMethod: PaymentMethod.Yandex,
    items: [
        {
            id: 1,
            feedId: 201000464,
            offerId: 'strundel.865.nk2',
            wareMd5: 'TawpAbQyTm-SPtjuHOj_tA',
            feedCategoryId: '',
            categoryId: 90401,
            offerName: 'Пылесос VES electric VC-015-B черный',
            price: 201,
            count: 1,
            modelId: -1,
            description: '',
            pictures: [
                {
                    url: '//path/to_picture',
                },
            ],
            buyerPrice: 201,
            fee: 0,
            feeInt: 0,
            feeSum: 0,
            pp: 7,
            showUid: '163464667074366498623',
            realShowUid: '16346466707436649862306005',
            cartShowUid: '16346466707436649862306005',
            shopUrl: 'https://pokupki.market.yandex.ru/product/something',
            kind2Params: [],
            vat: Vat.VAT_20_120,
            vendorId: 921854,
            loyaltyProgramPartner: false,
            sku: '100256640266',
            shopSku: 'strundel.865.nk2',
            supplierId: 11103659,
            fulfilmentShopId: 11103659,
            classifierMagicId: '2e3fd1d93250545fb54cb980a9801e05',
            prepayEnabled: false,
            isRecommendedByVendor: false,
            weight: 3000,
            width: 31,
            height: 101,
            depth: 110,
            hasSnapshot: false,
            cargoTypes: [200, 300, 301],
            buyerPriceBeforeDiscount: 201,
            partnerPriceMarkup: {
                coefficients: [
                    {
                        name: 'isGoldenMatrix',
                        value: 0,
                    },
                    {
                        name: 'dynamicPriceStrategy',
                        value: 0,
                    },
                ],
            },
            categoryFullName: 'Все товары',
            preorder: false,
            supplierType: 'THIRD_PARTY',
            itemDescriptionEnglish: '',
            supplierDescription: 'Пылесос VES electric VC-015',
            manufacturerCountries: [
                {
                    id: 225,
                    name: 'Россия',
                    lingua: {
                        name: {
                            genitive: 'России',
                            preposition: 'в',
                            prepositional: 'России',
                        },
                    },
                },
            ],
            supplierWorkSchedule: 'Пн-Пт: 10:00-19:00, Сб-Вс: 10:00-18:00',
            atSupplierWarehouse: true,
            supplierCurrency: 'RUR',
            msku: 100256640266,
            warehouseId: 51449,
            fulfilmentWarehouseId: 51449,
            countInBundle: 1,
            primaryInBundle: false,
            services: [],
            bnpl: true,
        },
    ],
    delivery: {
        type: DeliveryType.Delivery,
        serviceName: 'Доставка',
        price: 99,
        buyerPrice: 99,
        dates: {
            fromDate: '19-10-2021',
            toDate: '19-10-2021',
            fromTime: '18:55',
            toTime: '19:35',
        },
        validatedDates: {},
        regionId: 213,
        deliveryServiceId: 1006360,
        deliveryPartnerType: 'YANDEX_MARKET',
        parcels: [
            {
                id: 6221156,
                weight: 5200,
                width: 38,
                height: 101,
                depth: 110,
                status: 'CREATED',
                boxes: [
                    {
                        id: 5945717,
                    },
                ],
                creationDate: '2021-10-19T12:31:45.195Z',
                shipmentDate: '2021-10-19',
                packagingTime: '2021-10-19T18:00:00Z',
                deliveryDeadlineStatus: 'DELIVERY_DATES_DEADLINE_NOW',
                route: {
                    route: {
                        cost: 106,
                        paths: [
                            {
                                point_to: 1,
                                point_from: 0,
                            },
                            {
                                point_to: 2,
                                point_from: 1,
                            },
                            {
                                point_to: 3,
                                point_from: 2,
                            },
                        ],
                        points: [
                            {
                                ids: {
                                    post_code: 0,
                                    region_id: 213,
                                    gps_coords: {
                                        lat: 0,
                                        lon: 0,
                                    },
                                    partner_id: 51449,
                                    dsbs_point_id: '',
                                    logistic_point_id: 10001001920,
                                },
                                services: [
                                    {
                                        id: 3451549,
                                        code: 'CUTOFF',
                                        cost: 0,
                                        type: 'INTERNAL',
                                        items: [
                                            {
                                                quantity: 1,
                                                item_index: 0,
                                            },
                                            {
                                                quantity: 1,
                                                item_index: 1,
                                            },
                                            {
                                                quantity: 5,
                                                item_index: 2,
                                            },
                                        ],
                                        duration: {
                                            nanos: 0,
                                            seconds: 0,
                                        },
                                        tz_offset: 10800,
                                        start_time: {
                                            nanos: 0,
                                            seconds: 1634646900,
                                        },
                                        logistic_date: {
                                            day: 19,
                                            year: 2021,
                                            month: 10,
                                        },
                                        schedule_end_time: {
                                            nanos: 0,
                                            seconds: 1634675340,
                                        },
                                        schedule_start_time: {
                                            nanos: 0,
                                            seconds: 1634590800,
                                        },
                                    },
                                    {
                                        id: 3451550,
                                        code: 'PROCESSING',
                                        cost: 0,
                                        type: 'INTERNAL',
                                        items: [
                                            {
                                                quantity: 1,
                                                item_index: 0,
                                            },
                                            {
                                                quantity: 1,
                                                item_index: 1,
                                            },
                                            {
                                                quantity: 5,
                                                item_index: 2,
                                            },
                                        ],
                                        duration: {
                                            nanos: 0,
                                            seconds: 1800,
                                        },
                                        tz_offset: 10800,
                                        start_time: {
                                            nanos: 0,
                                            seconds: 1634646900,
                                        },
                                        logistic_date: {
                                            day: 19,
                                            year: 2021,
                                            month: 10,
                                        },
                                        schedule_end_time: {
                                            nanos: 0,
                                            seconds: 1634673600,
                                        },
                                        schedule_start_time: {
                                            nanos: 0,
                                            seconds: 1634590800,
                                        },
                                    },
                                    {
                                        id: 3451551,
                                        code: 'SHIPMENT',
                                        cost: 0,
                                        type: 'OUTBOUND',
                                        items: [
                                            {
                                                quantity: 1,
                                                item_index: 0,
                                            },
                                            {
                                                quantity: 1,
                                                item_index: 1,
                                            },
                                            {
                                                quantity: 5,
                                                item_index: 2,
                                            },
                                        ],
                                        duration: {
                                            nanos: 0,
                                            seconds: 0,
                                        },
                                        tz_offset: 10800,
                                        start_time: {
                                            nanos: 0,
                                            seconds: 1634648700,
                                        },
                                        logistic_date: {
                                            day: 19,
                                            year: 2021,
                                            month: 10,
                                        },
                                        schedule_end_time: {
                                            nanos: 0,
                                            seconds: 1634673600,
                                        },
                                        schedule_start_time: {
                                            nanos: 0,
                                            seconds: 1634590800,
                                        },
                                    },
                                ],
                                segment_id: 736798,
                                partner_name: 'Тест склад',
                                partner_type: 'DROPSHIP',
                                segment_type: 'warehouse',
                            },
                            {
                                ids: {
                                    post_code: 0,
                                    region_id: 0,
                                    gps_coords: {
                                        lat: 0,
                                        lon: 0,
                                    },
                                    partner_id: 1006360,
                                    dsbs_point_id: '',
                                    logistic_point_id: 0,
                                },
                                services: [
                                    {
                                        id: 3496195,
                                        code: 'MOVEMENT',
                                        cost: 0,
                                        type: 'INTERNAL',
                                        items: [
                                            {
                                                quantity: 1,
                                                item_index: 0,
                                            },
                                            {
                                                quantity: 1,
                                                item_index: 1,
                                            },
                                            {
                                                quantity: 5,
                                                item_index: 2,
                                            },
                                        ],
                                        duration: {
                                            nanos: 0,
                                            seconds: 0,
                                        },
                                        tz_offset: 10800,
                                        start_time: {
                                            nanos: 0,
                                            seconds: 1634648700,
                                        },
                                        logistic_date: {
                                            day: 19,
                                            year: 2021,
                                            month: 10,
                                        },
                                        schedule_end_time: {
                                            nanos: 0,
                                            seconds: 1634670000,
                                        },
                                        schedule_start_time: {
                                            nanos: 0,
                                            seconds: 1634590800,
                                        },
                                    },
                                    {
                                        id: 3496194,
                                        code: 'SHIPMENT',
                                        cost: 0,
                                        type: 'OUTBOUND',
                                        items: [
                                            {
                                                quantity: 1,
                                                item_index: 0,
                                            },
                                            {
                                                quantity: 1,
                                                item_index: 1,
                                            },
                                            {
                                                quantity: 5,
                                                item_index: 2,
                                            },
                                        ],
                                        duration: {
                                            nanos: 0,
                                            seconds: 0,
                                        },
                                        tz_offset: 10800,
                                        start_time: {
                                            nanos: 0,
                                            seconds: 1634648700,
                                        },
                                        logistic_date: {
                                            day: 19,
                                            year: 2021,
                                            month: 10,
                                        },
                                        schedule_end_time: {
                                            nanos: 0,
                                            seconds: 1634670000,
                                        },
                                        schedule_start_time: {
                                            nanos: 0,
                                            seconds: 1634590800,
                                        },
                                    },
                                ],
                                segment_id: 736858,
                                partner_name: 'Яндекс Go экспресс',
                                partner_type: 'DELIVERY',
                                segment_type: 'movement',
                            },
                            {
                                ids: {
                                    post_code: 0,
                                    region_id: 213,
                                    gps_coords: {
                                        lat: 0,
                                        lon: 0,
                                    },
                                    partner_id: 1006360,
                                    dsbs_point_id: '',
                                    logistic_point_id: 0,
                                },
                                services: [
                                    {
                                        id: 3105143,
                                        code: 'DELIVERY',
                                        cost: 0,
                                        type: 'INTERNAL',
                                        items: [
                                            {
                                                quantity: 1,
                                                item_index: 0,
                                            },
                                            {
                                                quantity: 1,
                                                item_index: 1,
                                            },
                                            {
                                                quantity: 5,
                                                item_index: 2,
                                            },
                                        ],
                                        duration: {
                                            nanos: 0,
                                            seconds: 0,
                                        },
                                        tz_offset: 10800,
                                        start_time: {
                                            nanos: 0,
                                            seconds: 1634648700,
                                        },
                                        logistic_date: {
                                            day: 19,
                                            year: 2021,
                                            month: 10,
                                        },
                                        schedule_end_time: {
                                            nanos: 0,
                                            seconds: 1634677199,
                                        },
                                        schedule_start_time: {
                                            nanos: 0,
                                            seconds: 1634590800,
                                        },
                                    },
                                    {
                                        id: 3105144,
                                        code: 'LAST_MILE',
                                        cost: 0,
                                        type: 'INTERNAL',
                                        items: [
                                            {
                                                quantity: 1,
                                                item_index: 0,
                                            },
                                            {
                                                quantity: 1,
                                                item_index: 1,
                                            },
                                            {
                                                quantity: 5,
                                                item_index: 2,
                                            },
                                        ],
                                        duration: {
                                            nanos: 0,
                                            seconds: 0,
                                        },
                                        tz_offset: 0,
                                        start_time: {
                                            nanos: 0,
                                            seconds: 1634648700,
                                        },
                                        logistic_date: {
                                            day: 19,
                                            year: 2021,
                                            month: 10,
                                        },
                                        schedule_end_time: {
                                            nanos: 0,
                                            seconds: 0,
                                        },
                                        schedule_start_time: {
                                            nanos: 0,
                                            seconds: 0,
                                        },
                                    },
                                ],
                                segment_id: 714826,
                                partner_name: 'Яндекс Go экспресс',
                                partner_type: 'DELIVERY',
                                segment_type: 'linehaul',
                            },
                            {
                                ids: {
                                    post_code: 0,
                                    region_id: 213,
                                    gps_coords: {
                                        lat: 0,
                                        lon: 0,
                                    },
                                    partner_id: 1006360,
                                    dsbs_point_id: '',
                                    logistic_point_id: 0,
                                },
                                services: [
                                    {
                                        id: 5093489,
                                        code: 'CALL_COURIER',
                                        cost: 0,
                                        type: 'INTERNAL',
                                        items: [
                                            {
                                                quantity: 1,
                                                item_index: 0,
                                            },
                                            {
                                                quantity: 1,
                                                item_index: 1,
                                            },
                                            {
                                                quantity: 5,
                                                item_index: 2,
                                            },
                                        ],
                                        duration: {
                                            nanos: 0,
                                            seconds: 1200,
                                        },
                                        tz_offset: 0,
                                        start_time: {
                                            nanos: 0,
                                            seconds: 1634648700,
                                        },
                                        logistic_date: {
                                            day: 19,
                                            year: 2021,
                                            month: 10,
                                        },
                                        schedule_end_time: {
                                            nanos: 0,
                                            seconds: 0,
                                        },
                                        schedule_start_time: {
                                            nanos: 0,
                                            seconds: 0,
                                        },
                                    },
                                    {
                                        id: 5093488,
                                        code: 'HANDING',
                                        cost: 0,
                                        type: 'OUTBOUND',
                                        items: [
                                            {
                                                quantity: 1,
                                                item_index: 0,
                                            },
                                            {
                                                quantity: 1,
                                                item_index: 1,
                                            },
                                            {
                                                quantity: 5,
                                                item_index: 2,
                                            },
                                        ],
                                        duration: {
                                            nanos: 0,
                                            seconds: 0,
                                        },
                                        tz_offset: 10800,
                                        start_time: {
                                            nanos: 0,
                                            seconds: 1634649900,
                                        },
                                        logistic_date: {
                                            day: 19,
                                            year: 2021,
                                            month: 10,
                                        },
                                        schedule_end_time: {
                                            nanos: 0,
                                            seconds: 1634655600,
                                        },
                                        delivery_intervals: [
                                            {
                                                to: {
                                                    hour: 19,
                                                    minute: 35,
                                                },
                                                from: {
                                                    hour: 18,
                                                    minute: 55,
                                                },
                                            },
                                        ],
                                        schedule_start_time: {
                                            nanos: 0,
                                            seconds: 1634626800,
                                        },
                                    },
                                ],
                                segment_id: 758008,
                                partner_name: 'Яндекс Go экспресс',
                                partner_type: 'DELIVERY',
                                segment_type: 'handing',
                            },
                        ],
                        date_to: {
                            day: 19,
                            year: 2021,
                            month: 10,
                        },
                        date_from: {
                            day: 19,
                            year: 2021,
                            month: 10,
                        },
                        tariff_id: 100415,
                        cost_for_shop: 200,
                        delivery_type: 'COURIER',
                        shipment_warehouse_id: 51449,
                    },
                    offers: [
                        {
                            feed_id: 0,
                            shop_id: 11103659,
                            shop_sku: 'strundel.865.nk2',
                            partner_id: 51449,
                            available_count: 1,
                        },
                    ],
                    promise: '',
                    virtual_box: {
                        weight: 3000,
                        dimensions: [31, 101, 110],
                    },
                    packing_boxes: [
                        {
                            weight: 3000,
                            dimensions: [31, 101, 110],
                        },
                    ],
                    delivery_dates: {
                        shipment_day: 0,
                        shipment_date: {
                            nanos: 0,
                            seconds: 1634590800,
                        },
                        packaging_time: 57900,
                        shipment_by_supplier: {
                            nanos: 0,
                            seconds: 1634590800,
                        },
                        last_warehouse_offset: {
                            offset: 0,
                            warehouse_position: 0,
                        },
                        reception_by_warehouse: {
                            nanos: 0,
                            seconds: 1634590800,
                        },
                    },
                    string_delivery_dates: {
                        shipment_date: '2021-10-19',
                        packaging_time: 'PT16H300M',
                        shipment_by_supplier: '2021-10-19T00:00:00+03:00',
                        last_warehouse_offset: {
                            offset: 0,
                            warehouse_position: 0,
                        },
                        reception_by_warehouse: '2021-10-19T00:00:00+03:00',
                    },
                },
                shipmentDateTimeBySupplier: '2021-10-19T00:00:00',
                receptionDateTimeByWarehouse: '2021-10-19T00:00:00',
                items: [
                    {
                        itemId: 9781485,
                        count: 1,
                        supplierStartDateTime: '2021-10-18T21:00:00Z',
                        supplierShipmentDateTime: '2021-10-18T21:00:00Z',
                    },
                ],
            },
        ],
        vat: Vat.VAT_20_120,
        userReceived: false,
        recipient: {
            recipientName: {
                firstName: 'ыфывфы',
                lastName: 'фвыв',
            },
            phone: '+7 000 000-00-72',
            email: 'test@yandex.ru',
        },
        buyerPriceBeforeDiscount: 99,
        tariffId: 100415,
        features: [DeliveryFeature.ExpressDelivery],
        balanceOrderId: '32849886-delivery',
        marketBranded: false,
        leaveAtTheDoor: false,
        ...delivery,
    },
    fake: false,
    context: OrderContext.Market,
    notes: 'some note',
    shopOrderId: '32849886',
    paymentId: 4358052,
    userGroup: 'DEFAULT',
    noAuth: false,
    acceptMethod: 'WEB_INTERFACE',
    displayOrderId: '32849886',
    global: false,
    payment: {
        id: 4358052,
        orderId: 32849886,
        fake: false,
        status: 'HOLD',
        uid: 106993811,
        currency: 'RUR',
        totalAmount: 1000,
        creationDate: '19-10-2021 15:31:47',
        updateDate: '19-10-2021 15:32:38',
        statusUpdateDate: '19-10-2021 15:32:18',
        prepayType: 'YANDEX_MARKET',
        balancePayMethodType: 'card',
        balanceServiceId: 610,
        basketId: '616ebab55b095c65317f9ecc',
        purchaseToken: '90810cbb124ea13595ec2d13ee4e14bd',
        mbiControlEnabled: false,
    },
    taxSystem: 'OSN',
    properties: {
        paymentSystem: 'VISA',
        purchaseReferrer: 'null',
        yandexPlusUser: 'true',
        isEda: 'false',
        yandexPlus: 'false',
        experiments: 'market_dsbs_tariffs=1;market_unified_tariffs=1',
        allowYandexPay: 'false',
        isYandexPay: '0',
        partnerPaymentMethods: 'YANDEX',
        isReseller: 'false',
        platform: 'DESKTOP',
        allowSpasibo: '0',
        hasServices: 'false',
        mrid: '1634646704350/38cced4a1a4e9121336325d8b3ce0500/6',
        wasSplitByCombinator: 'false',
        directShopInShopItems: null,
        totalAdditionalMultiorderCashback: '91',
    },
    fulfilment: false,
    rgb: MarketColor.Blue,
    bnpl: false,
    buyerTotalBeforeDiscount: 960,
    buyerItemsTotalBeforeDiscount: 861,
    buyerItemsTotalDiscount: 0,
    buyerTotalDiscount: 0,
    substatusUpdateDate: '19-10-2021 15:31:46',
    preorder: false,
    substatusUpdateDateTimestamp: '1634646706084',
    ...draft,
});
