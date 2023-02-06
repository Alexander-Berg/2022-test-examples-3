/* eslint-disable max-len */
import { REPORT_DEV_HOST, REPORT_DEV_PORT, REPORT_DEV_PATH } from '../../../../../../src/env';

const HOST = `${REPORT_DEV_HOST}:${REPORT_DEV_PORT}`;

const ROUTE = new RegExp(`/${REPORT_DEV_PATH}`);

const RESPONSE = {
    sorts: [
        {
            text: 'по популярности',
        },
        {
            text: 'по цене',
            options: [
                {
                    id: 'aprice',
                    type: 'asc',
                },
                {
                    id: 'dprice',
                    type: 'desc',
                },
            ],
        },
        {
            text: 'по рейтингу и цене',
            options: [
                {
                    id: 'rorp',
                },
            ],
        },
        {
            text: 'по размеру скидки',
            options: [
                {
                    id: 'discount_p',
                },
            ],
        },
    ],
    search: {
        groupBy: 'shop',
        total: 61,
        totalOffers: 61,
        totalFreeOffers: 0,
        totalOffersBeforeFilters: 61,
        totalModels: 0,
        totalPassedAllGlFilters: 61,
        adult: false,
        salesDetected: true,
        maxDiscountPercent: 0,
        shops: 61,
        totalShopsBeforeFilters: 61,
        shopOutlets: 2877,
        cpaCount: 1,
        isParametricSearch: false,
        duplicatesHidden: 0,
        category: {
            cpaType: 'cpc_and_cpa',
        },
        isDeliveryIncluded: false,
        isPickupIncluded: false,
        showBlockId: '',
        results: [],
    },
    filters: [
        {
            id: 'glprice',
            type: 'number',
            name: 'Цена',
            subType: '',
            kind: 2,
            values: [
                {
                    max: '',
                    checked: true,
                    min: '1',
                    id: 'chosen',
                },
                {
                    max: '5740',
                    initialMax: '5740',
                    initialMin: '3995.62',
                    min: '3995.62',
                    id: 'found',
                },
            ],
            presetValues: [
                {
                    initialFound: 53,
                    max: '5000',
                    unit: 'RUR',
                    found: 53,
                    value: '… – 5000',
                    id: 'v5000',
                },
                {
                    initialFound: 8,
                    unit: 'RUR',
                    found: 8,
                    value: '5000 – …',
                    min: '5000',
                    id: '5000v',
                },
            ],
            meta: {},
        },
        {
            id: 'promo-type',
            type: 'enum',
            name: 'Скидки и акции',
            subType: '',
            kind: 2,
            values: [
                {
                    found: 3,
                    value: 'скидки',
                    id: 'discount',
                },
                {
                    found: 1,
                    value: 'промокоды',
                    id: 'promo-code',
                },
                {
                    found: 0,
                    value: 'подарки за покупку',
                    id: 'gift-with-purchase',
                },
                {
                    found: 0,
                    value: 'больше за ту же цену',
                    id: 'n-plus-m',
                },
            ],
            valuesGroups: [],
            meta: {},
        },
        {
            id: 'cpa',
            type: 'boolean',
            name: 'Покупка на Маркете',
            subType: '',
            kind: 2,
            values: [
                {
                    value: '0',
                },
                {
                    found: 1,
                    value: '1',
                },
            ],
            meta: {},
        },
        {
            id: 'manufacturer_warranty',
            type: 'boolean',
            name: 'Гарантия производителя',
            subType: '',
            kind: 2,
            values: [
                {
                    found: 30,
                    value: '0',
                },
                {
                    found: 31,
                    value: '1',
                },
            ],
            meta: {},
        },
        {
            id: 'credit-type',
            type: 'boolean',
            name: 'Покупка в кредит',
            subType: '',
            kind: 2,
            hasBoolNo: true,
            values: [
                {
                    initialFound: 1,
                    found: 1,
                    value: 'credit',
                },
                {
                    initialFound: 0,
                    found: 0,
                    value: 'installment',
                },
            ],
            meta: {},
        },
        {
            id: 'qrfrom',
            type: 'boolean',
            name: 'Рейтинг магазина',
            subType: '',
            kind: 2,
            hasBoolNo: true,
            values: [
                {
                    found: 61,
                    value: '3',
                },
                {
                    found: 59,
                    value: '4',
                },
            ],
            meta: {},
        },
        {
            id: 'free-delivery',
            type: 'boolean',
            name: 'Бесплатная доставка курьером',
            subType: '',
            kind: 2,
            values: [
                {
                    initialFound: 1,
                    found: 4,
                    value: '1',
                },
            ],
            meta: {},
        },
        {
            id: 'offer-shipping',
            type: 'boolean',
            name: 'Способ доставки',
            subType: '',
            kind: 2,
            hasBoolNo: true,
            values: [
                {
                    initialFound: 58,
                    found: 58,
                    value: 'delivery',
                },
                {
                    initialFound: 45,
                    found: 45,
                    value: 'pickup',
                },
                {
                    initialFound: 16,
                    found: 16,
                    value: 'store',
                },
            ],
            meta: {},
        },
        {
            id: 'payments',
            type: 'enum',
            name: 'Способы оплаты',
            subType: '',
            kind: 2,
            values: [
                {
                    initialFound: 17,
                    found: 17,
                    value: 'Картой на сайте',
                    id: 'prepayment_card',
                },
                {
                    initialFound: 7,
                    found: 7,
                    value: 'Картой курьеру',
                    id: 'delivery_card',
                },
                {
                    initialFound: 30,
                    found: 30,
                    value: 'Наличными курьеру',
                    id: 'delivery_cash',
                },
            ],
            valuesGroups: [],
            meta: {},
        },
        {
            id: 'delivery-interval',
            type: 'boolean',
            name: 'Срок доставки курьером',
            subType: '',
            kind: 2,
            hasBoolNo: true,
            values: [
                {
                    found: 2,
                    value: '0',
                },
                {
                    found: 16,
                    value: '1',
                },
                {
                    found: 45,
                    value: '5',
                },
            ],
            meta: {},
        },
        {
            id: 'fesh',
            type: 'enum',
            name: 'Магазины',
            subType: '',
            kind: 2,
            valuesCount: 61,
            values: [
                {
                    found: 1,
                    value: 'MaxMemory.ru',
                    id: '63768',
                },
                {
                    found: 1,
                    value: 'msk.you2you.ru',
                    id: '626446',
                },
                {
                    found: 1,
                    value: 'Алекомп',
                    id: '84753',
                },
                {
                    found: 1,
                    value: 'Империя техно',
                    id: '343057',
                },
                {
                    found: 1,
                    value: 'Импульс Тех',
                    id: '208256',
                },
                {
                    found: 1,
                    value: 'Компания РЕМА',
                    id: '59778',
                },
                {
                    found: 1,
                    value: 'КотоФото.Москва',
                    id: '281111',
                },
                {
                    found: 1,
                    value: 'ОКСАР.ру',
                    id: '252831',
                },
                {
                    found: 1,
                    value: 'Ситилинк',
                    id: '17436',
                },
                {
                    found: 1,
                    value: 'Современные Системы Безопасности',
                    id: '402710',
                },
                {
                    found: 1,
                    value: 'ТехноГид',
                    id: '108546',
                },
                {
                    found: 1,
                    value: 'ТЕХНОПАРК',
                    id: '1925',
                },
                {
                    found: 1,
                    value: '123.ru',
                    id: '5570',
                },
                {
                    found: 1,
                    value: '123.ру',
                    id: '323308',
                },
                {
                    found: 1,
                    value: 'B1-Store',
                    id: '598524',
                },
                {
                    found: 1,
                    value: 'BeCompact.RU',
                    id: '6363',
                },
                {
                    found: 1,
                    value: 'CIT.ru',
                    id: '779610',
                },
                {
                    found: 1,
                    value: 'compday.ru',
                    id: '104920',
                },
                {
                    found: 1,
                    value: 'CompYou',
                    id: '25017',
                },
                {
                    found: 1,
                    value: 'Flash Computers',
                    id: '3534',
                },
                {
                    found: 1,
                    value: 'IronBook.RU',
                    id: '28484',
                },
                {
                    found: 1,
                    value: 'JUST.RU',
                    id: '17019',
                },
                {
                    found: 1,
                    value: 'KAUF',
                    id: '599',
                },
                {
                    found: 1,
                    value: 'KNS.ru',
                    id: '493',
                },
                {
                    found: 1,
                    value: 'Lanbay.ru',
                    id: '309708',
                },
                {
                    found: 1,
                    value: 'MITcor.ru',
                    id: '145766',
                },
                {
                    found: 1,
                    value: 'MVA Group',
                    id: '262986',
                },
                {
                    found: 1,
                    value: 'Myshop.ru',
                    id: '582',
                },
                {
                    found: 1,
                    value: 'netshopping.ru',
                    id: '1380',
                },
                {
                    found: 1,
                    value: 'NEWMART',
                    id: '50106',
                },
                {
                    found: 1,
                    value: 'OfficeNeeds',
                    id: '577092',
                },
                {
                    found: 1,
                    value: 'OLDI.RU',
                    id: '12138',
                },
                {
                    found: 1,
                    value: 'PC4games.ru',
                    id: '295248',
                },
                {
                    found: 1,
                    value: 'PC4YOU.RU',
                    id: '538217',
                },
                {
                    found: 1,
                    value: 'QUKE.ru',
                    id: '7076',
                },
                {
                    found: 1,
                    value: 'RASM.RU Владимир',
                    id: '260041',
                },
                {
                    found: 1,
                    value: 'Realsystem.ru',
                    id: '64249',
                },
                {
                    found: 1,
                    value: 'SafeAround.ru',
                    id: '537323',
                },
                {
                    found: 1,
                    value: 'SLY Сomputers',
                    id: '339',
                },
                {
                    found: 1,
                    value: 'Store.Softline.ru',
                    id: '107253',
                },
                {
                    found: 1,
                    value: 'TopComputer.RU',
                    id: '5205',
                },
                {
                    found: 1,
                    value: 'TTT.RU',
                    id: '68728',
                },
                {
                    found: 1,
                    value: 'www.Pleer.ru',
                    id: '720',
                },
                {
                    found: 1,
                    value: 'XCOM-SHOP.RU',
                    id: '704',
                },
                {
                    found: 1,
                    value: 'XPERT.RU',
                    id: '3141',
                },
                {
                    found: 1,
                    value: 'Дельта Механикс',
                    id: '429796',
                },
                {
                    found: 1,
                    value: 'Железа.НЕТ',
                    id: '341561',
                },
                {
                    found: 1,
                    value: 'Компео',
                    id: '661088',
                },
                {
                    found: 1,
                    value: 'КомпьютерМаркет',
                    id: '12065',
                },
                {
                    found: 1,
                    value: 'Мир USB',
                    id: '557289',
                },
                {
                    found: 1,
                    value: 'Мой ПК',
                    id: '556488',
                },
                {
                    found: 1,
                    value: 'Мультимаркет Nicom',
                    id: '587240',
                },
                {
                    found: 1,
                    value: 'Неогид',
                    id: '31208',
                },
                {
                    found: 1,
                    value: 'НИКС',
                    id: '141168',
                },
                {
                    found: 1,
                    value: 'Олимп Сервис',
                    id: '371504',
                },
                {
                    found: 1,
                    value: 'ОНЛАЙН ТРЕЙД.РУ',
                    id: '255',
                },
                {
                    found: 1,
                    value: 'ПОЗИТРОНИКА',
                    id: '385207',
                },
                {
                    found: 1,
                    value: 'Регард',
                    id: '4398',
                },
                {
                    found: 1,
                    value: 'ЭЛЕКТРОЗОН',
                    id: '40885',
                },
                {
                    found: 1,
                    value: 'Энергобум',
                    id: '262978',
                },
                {
                    found: 1,
                    value: 'Яндекс.Маркет',
                    id: '431782',
                },
            ],
            valuesGroups: [],
            meta: {},
        },
    ],
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
