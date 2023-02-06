/* eslint-disable max-len */

import customDeals from './customDeals';

export default {
    entity: 'page',
    id: 66108,
    rev: 360011,
    type: 'catalog',
    name: 'Хаб скидок_dsk_new_deals_hub',
    hasContextParams: true,
    bindings: [
        {
            entity: 'navnode',
            id: 61522,
        },
        {
            entity: 'domain',
            id: 'ru',
        },
        {
            entity: 'domain',
            id: 'by',
        },
        {
            entity: 'domain',
            id: 'kz',
        },
        {
            entity: 'domain',
            id: 'ua',
        },
    ],
    content: {
        entity: 'box',
        rows: [
            {
                entity: 'box',
                name: 'Grid12',
                props: {
                    type: 'row',
                    width: 'default',
                    layout: true,
                    grid: 1,
                },
                nodes: [
                    {
                        entity: 'box',
                        name: 'Grid12',
                        props: {
                            type: 'column',
                            layout: false,
                            width: 1,
                            position: 'default',
                            sticky: false,
                        },
                        nodes: [
                            {
                                props: {},
                                resources: {
                                    garsons: [
                                        {
                                            params: {
                                                data: customDeals,
                                            },
                                            id: 'CustomDeals',
                                        },
                                    ],
                                },
                                id: 54764056,
                                name: 'CustomDealsFeed',
                                entity: 'widget',
                            },
                            {
                                props: {},
                                id: 54764100,
                                name: 'DealsHubHeader',
                                entity: 'widget',
                            },
                        ],
                    },
                ],
            },
            {
                entity: 'box',
                name: 'Grid12',
                props: {
                    type: 'row',
                    width: 'default',
                    layout: true,
                    grid: 12,
                },
                nodes: [
                    {
                        entity: 'box',
                        name: 'Grid12',
                        props: {
                            type: 'column',
                            layout: false,
                            width: 2,
                            position: 'default',
                            sticky: false,
                        },
                        nodes: [
                            {
                                resources: {
                                    garsons: [
                                        {
                                            params: {
                                                depth: 2,
                                                nid: 61522,
                                            },
                                            id: 'NavigationTree',
                                        },
                                    ],
                                },
                                props: {
                                    promoTypes: [
                                        {
                                            id: 'discount',
                                            name: 'скидки',
                                        },
                                        {
                                            id: 'promo-code',
                                            name: 'промокоды',
                                        },
                                        {
                                            id: 'gift-with-purchase',
                                            name: 'подарок за покупку',
                                        },
                                        {
                                            id: 'n-plus-m',
                                            name: 'больше за ту же цену',
                                        },
                                    ],
                                },
                                id: 52314301,
                                name: 'PromoNavigationTree',
                                entity: 'widget',
                            },
                        ],
                    },
                    {
                        entity: 'box',
                        name: 'Grid12',
                        props: {
                            type: 'column',
                            layout: false,
                            width: 10,
                            position: 'default',
                            sticky: false,
                        },
                        nodes: [
                            {
                                props: {
                                    titleTheme: 'subtitle',
                                    withCategories: false,
                                    paddingBottom: 'none',
                                    paddingLeft: 'condensed',
                                    showHeader: true,
                                    headerText: 'Выгодно прямо сейчас',
                                    additionalProductUrlParams: {
                                        'promo-type': 'market',
                                    },
                                    paddingTop: 'dense',
                                    showMoreButton: true,
                                    showTabs: false,
                                    showTitle: true,
                                    paddingRight: 'none',
                                },
                                resources: {
                                    garsons: [
                                        {
                                            id: 'PrimeSearch',
                                            count: 9999,
                                            params: {
                                                'onstock': '1',
                                                'promo-type': 'market',
                                                'use-default-offers': 1,
                                                'has-promo': 1,
                                                'hid': '90533,13077405,8476097,91335,15726400,16044621,15685457,15685787,15553892,90569,90584,15450081,90589,90566,90478,91650,7814994,7815007,12410815,10470548,91031,91019,138608,91013,90639,91122,91491,10498025,14369750,91529,90539,15927546,15368134,15714102,90688,12714763,90586,278374,90599,90404,308016,5017483,13771450,6504970,15646095,10683243,15646187,281935,267389,91610,763072,284394,15448926,91651,12894143,1031249,12499575,90698,7812201,7812207,91259,10682618,10682647,6159024,10683251,12907256,14247341,14247369,7070735,13314877,14996686,14990285,13239503,13239135,14993676,91183,8476099,90565,16302537,16302536,90578,765280,90588,90567,90463,6269371,90462,91599,90713,13360765,10682550,13360751,90799,989023,12382295,6427100,14334315,723088,91052,14333188,90548,90555,2724669,15880008,91148,91519,90534,8480722,13776946,13776757,15356747,10682659,10682629,91011,1564516,91635,91611,90690,12894020,91517,454910,13199962,13792611,14868726,989025,10790731,989026,16053627',
                                                'promo-check-min-price': 3,
                                                'allow-collapsing': 1,
                                            },
                                        },
                                    ],
                                },
                                id: 56166304,
                                name: 'Recommendations',
                                entity: 'widget',
                            },
                        ],
                    },
                ],
            },
            {
                entity: 'box',
                name: 'Grid12',
                props: {
                    type: 'row',
                    width: 'default',
                    layout: true,
                    grid: 1,
                },
                nodes: [
                    {
                        entity: 'box',
                        name: 'Grid12',
                        props: {
                            type: 'column',
                            layout: false,
                            width: 1,
                            position: 'default',
                            sticky: false,
                        },
                        nodes: [
                            {
                                entity: 'widget',
                                name: 'Subscription',
                                id: 56385862,
                                loadMode: 'default',
                                props: {
                                    type: 'ADVERTISING',
                                    place: 'footer',
                                    title: 'Каждую неделю — скидки и акции.<br/>Подпишитесь и узнайте раньше всех.',
                                    messageThanks: 'Спасибо! Вы подписаны на рассылку.',
                                    theme: 'dealsHub',
                                    colorScheme: 'light',
                                },
                            },
                        ],
                    },
                ],
            },
        ],
    },
};

/* eslint-enable max-len */
