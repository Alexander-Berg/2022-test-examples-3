/* eslint-disable max-len */
import { REPORT_DEV_HOST, REPORT_DEV_PORT, REPORT_DEV_PATH } from '../../../../src/env';

const HOST = `${REPORT_DEV_HOST}:${REPORT_DEV_PORT}`;

const ROUTE = new RegExp(`/${REPORT_DEV_PATH}`);

const RESPONSE = {
    search: {
        total: 0,
        totalOffers: 0,
        totalFreeOffers: 0,
        totalOffersBeforeFilters: 0,
        totalModels: 0,
        totalPassedAllGlFilters: 0,
        adult: false,
        view: 'list',
        salesDetected: false,
        maxDiscountPercent: 0,
        shops: 0,
        totalShopsBeforeFilters: 0,
        cpaCount: 0,
        isParametricSearch: false,
        isDeliveryIncluded: false,
        isPickupIncluded: false,
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
                    initialMax: '',
                    initialMin: '',
                    min: '',
                    id: 'found',
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
                    found: 0,
                    value: 'скидки',
                    id: 'discount',
                },
                {
                    found: 0,
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
            id: 'onstock',
            type: 'boolean',
            name: 'В продаже',
            subType: '',
            kind: 2,
            values: [
                {
                    initialFound: 1,
                    checked: true,
                    value: '0',
                },
                {
                    initialFound: 0,
                    value: '1',
                },
            ],
            meta: {},
        },
    ],
    intents: [],
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
    ],
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
