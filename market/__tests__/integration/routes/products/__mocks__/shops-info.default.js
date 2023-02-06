/* eslint-disable max-len */

import '../../../../../src/env';

const HOST = process.env.REPORT_URI;

const ROUTE = /\/yandsearch/;

const RESPONSE = {
    results: [
        {
            entity: 'shop',
            id: 155,
            historicOnly: false,
            shopCurrency: 'RUR',
            deliveryCurrency: 'RUR',
            shopName: 'OZON.ru',
            slug: 'ozon-ru',
            logo: '//avatars.mds.yandex.net/get-market-shop-logo/1539910/2a0000016a44aa3e1d7da56fc50014c7b893/orig',
            isGlobal: false,
            prepayEnabled: false,
            isCpaPartner: true,
            isSupplier: false,
            blueStatus: 'no',
            cpc: {
                shopsDat: 'real',
                isInShopCpcFilterDb: false,
                calculated: 'real',
            },
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
            vendorRecommendations: [
                {
                    entity: 'vendor',
                    id: 1627871,
                    name: 'Grohe',
                },
                {
                    entity: 'vendor',
                    id: 10477286,
                    name: 'Hasbro',
                },
                {
                    entity: 'vendor',
                    id: 152837,
                    name: 'VITEK',
                },
            ],
            ignoreStocks: false,
            isVirtual: false,
            isDirectShipping: false,
            oldRating: 0,
            ratingToShow: 4.541846201,
            newRating: 4.541846201,
            newRatingTotal: 4.300576827,
            skkDisabled: false,
            newGradesCount3M: 39454,
            newGradesCount: 244094,
            overallGradesCount: 244094,
            aboName: 'www.ozon.ru',
            aboOldRating: 0,
            oldRawRating: 0,
            oldStatus: 'actual',
            oldCutoff: '',
            oldGradeBase: 27594,
            oldGradesCount: 84777,
            ratingType: 3,
        },
        {
            entity: 'shop',
            id: 704,
            historicOnly: false,
            shopCurrency: 'RUR',
            deliveryCurrency: 'RUR',
            shopName: 'XCOM-SHOP.RU',
            slug: 'xcom-shop-ru',
            logo: '//avatars.mds.yandex.net/get-market-shop-logo/1528691/2a00000167bc30d2a6e7d60c9f806fd5f2ba/orig',
            isGlobal: false,
            prepayEnabled: false,
            isCpaPartner: false,
            isSupplier: false,
            blueStatus: 'no',
            cpc: {
                shopsDat: 'real',
                isInShopCpcFilterDb: false,
                calculated: 'real',
            },
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
            phones: {
                raw: '+7(495)7999669',
                sanitized: '+74957999669',
            },
            vendorRecommendations: [
                {
                    entity: 'vendor',
                    id: 153070,
                    name: 'Ricoh',
                },
                {
                    entity: 'vendor',
                    id: 152837,
                    name: 'VITEK',
                },
            ],
            ignoreStocks: false,
            isVirtual: false,
            isDirectShipping: false,
            oldRating: 0,
            ratingToShow: 4.585812357,
            newRating: 4.585812357,
            newRatingTotal: 4.522326829,
            skkDisabled: false,
            newGradesCount3M: 874,
            newGradesCount: 19013,
            overallGradesCount: 19013,
            aboName: 'www.xcom-shop.ru',
            aboOldRating: 0,
            oldRawRating: 0,
            oldStatus: 'actual',
            oldCutoff: '',
            oldGradeBase: 829,
            oldGradesCount: 11181,
            ratingType: 3,
        },
    ],
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
