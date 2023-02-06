/* eslint-disable max-len */

const HOST = 'https://api.content.market.yandex.ru';

const ROUTE = /\/v[0-9.]+\/models\/[0-9]+\/offers\/default/;

const RESPONSE = {
    status: 'OK',
    context: {
        region: {
            id: 213,
            name: 'Москва',
            type: 'CITY',
            childCount: 14,
            country: {
                id: 225,
                name: 'Россия',
                type: 'COUNTRY',
                childCount: 10,
            },
        },
        currency: {
            id: 'RUR',
            name: 'руб.',
        },
        id: '1571689543807/2f1574c25920faa06ab0f47c71950500',
        time: '2019-10-21T23:25:43.946+03:00',
        marketUrl: 'https://market.yandex.ru?pp=483',
    },
    offer: {
        id: 'yDpJekrrgZHZxbqV_gXfcLXvZTZ8VKhOwMX1ShwmvFzN_Z_sGto3hw',
        wareMd5: 'ss-rzyUK7RlRv7ddRVsi0g',
        skuType: 'market',
        name: 'Зубная щетка Braun Oral-B Vitality 3D White Luxe D12.513',
        description:
            'Технические характеристики: Количество режимов: 1 Технология 2D Кол-во движений: 7600 возвратно-вращательных Тип питания: аккумулятор Ni-Mh Время непрерывной работы: до 30 мин Таймер Индикация зарядки Индикация износа щетинок Материал корпуса: пластик Вес в упаковке: 340 гр. Комплектация: - 1 насадка 3D White - зарядное устройство Oral-B Vitality D12.513 является самой доступной электрической щеткой, работающей от аккумулятора!',
        price: {
            value: '970',
        },
        cpa: false,
        url:
            'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZnHPtiAWXz_PKgYIE3xZ8BGbKUGx-vQKE-w8_UAso3t4pFMFHuKQVz0ZgyMUPgEqrGHAlVwVHVJ_v3HN6DnhlVAYHcVrMI13rnjVaCZccsPxNIVsKlu1a6DxC376p_abD51Rehu11XAJ5H8OxuO2j3-QU91kjDKV3ryI3rMOlsk8A8FzVXFZ7yj2aBZiBgDQpZGJjCN-un43t8Q2Lc_lOhVRB7G23MElVFwVls0YFqk0z3aX3UDymKwrgNRnA3DsiGUBfQRWviYwNhN59GubyCDSeYJZ75dHmvj2n54KU0Y5SNu_D1sUcQeGffmZfGlAkItLsfyglmQi1BxRbeZlEDvaYYhnYEkYwgIDGllmjdCIUNO-pMBeN8HhYofYDnhIkVlkPXLOzVB3fC2piuWdvDjXLs3gen7R4APN8ePBzBdS7Qw12-cZzEPQcx20npwk6v2NMRPsX0iILbsDSgKppoUQVFrybLYSD1w-8haVNp7AbMA1Zbe-6b4wvuecSOi-dZCC3YlHBqAXCyqIWV_zMgXX9C31oSN3PyJ_eFQU2J4HzXgbzRCeI9CcJBWKxGA6HOVJDUo6BzkbfBg0KVXWKjbndkk7yt8CfeuCYISI1Uw9Riuh6do0NY83HFDF3Jps4Du5lj8sxznOb5e9D659r6aOWX69BEmhHsRCn-Wutrz08V6XyiB5CHqnrkmc7I2141ePcA98CwXzE2jCcXUacEvUOFrmRFHxhIeNmQnwGoQriq47BVryie-JdWSXleP_xXaO9nph1HIMhHLknf2LMDD_xINFBMzCZ0RDFWgdgL3thATnR1PVjS14v0u37qHQumqxdRSESTmGQF7fgQNYAEw,?data=QVyKqSPyGQwwaFPWqjjgNmRbapEhbu1sZkeeKnzJIVk8VYk7x_IBE3pc9Tx587g4L_HAszuHuiPLQhXtIDSIdmpjhf-WuhGOVWW4EI22pqsWsC5SlbCTYX6kO-4BT8mWRUSZmTH4WBY-06AyXEfBJlkhsdKF7RGjRRNOuXskXxM9WDRgBGTCTisi7sabvYNeXfesihdSdvtoraQlvV_vpMo59R11ySM2ZxDZV2CT6GT-JpsYyt5636DQBeoZBoaqdi946DeO92Y,&b64e=1&sign=c9191c189990a1207b850533caeff266&keyno=1',
        urls: {
            483: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZnHPtiAWXz_PKgYIE3xZ8BGbKUGx-vQKE-w8_UAso3t4pFMFHuKQVz0ZgyMUPgEqrGHAlVwVHVJ_v3HN6DnhlVAYHcVrMI13rnjVaCZccsPxNIVsKlu1a6DxC376p_abD51Rehu11XAJ5H8OxuO2j3-QU91kjDKV3ryI3rMOlsk8A8FzVXFZ7yj2aBZiBgDQpZGJjCN-un43t8Q2Lc_lOhVRB7G23MElVFwVls0YFqk0z3aX3UDymKwrgNRnA3DsiGUBfQRWviYwNhN59GubyCDSeYJZ75dHmvj2n54KU0Y5SNu_D1sUcQeGffmZfGlAkItLsfyglmQi1BxRbeZlEDvaYYhnYEkYwgIDGllmjdCIUNO-pMBeN8HhYofYDnhIkVlkPXLOzVB3fC2piuWdvDjXLs3gen7R4APN8ePBzBdS7Qw12-cZzEPQcx20npwk6v2NMRPsX0iILbsDSgKppoUQVFrybLYSD1w-8haVNp7AbMA1Zbe-6b4wvuecSOi-dZCC3YlHBqAXCyqIWV_zMgXX9C31oSN3PyJ_eFQU2J4HzXgbzRCeI9CcJBWKxGA6HOVJDUo6BzkbfBg0KVXWKjbndkk7yt8CfeuCYISI1Uw9Riuh6do0NY83HFDF3Jps4Du5lj8sxznOb5e9D659r6aOWX69BEmhHsRCn-Wutrz08V6XyiB5CHqnrkmc7I2141ePcA98CwXzE2jCcXUacEvUOFrmRFHxhIeNmQnwGoQriq47BVryie-JdWSXleP_xXaO9nph1HIMhHLknf2LMDD_xINFBMzCZ0RDFWgdgL3thATnR1PVjS14v0u37qHQumqxdRSESTmGQF7fgQNYAEw,?data=QVyKqSPyGQwwaFPWqjjgNmRbapEhbu1sZkeeKnzJIVk8VYk7x_IBE3pc9Tx587g4L_HAszuHuiPLQhXtIDSIdmpjhf-WuhGOVWW4EI22pqsWsC5SlbCTYX6kO-4BT8mWRUSZmTH4WBY-06AyXEfBJlkhsdKF7RGjRRNOuXskXxM9WDRgBGTCTisi7sabvYNeXfesihdSdvtoraQlvV_vpMo59R11ySM2ZxDZV2CT6GT-JpsYyt5636DQBeoZBoaqdi946DeO92Y,&b64e=1&sign=c9191c189990a1207b850533caeff266&keyno=1',
        },
        directUrl:
            'http://morebt.ru/catalog/zubnye-shchetki/zubnaya-shchetka-braun-oral-b-vitality-3d-white-luxe-d12-513/?r1=yandext&r2=',
        shop: {
            region: {
                id: 213,
                name: 'Москва',
                type: 'CITY',
                childCount: 14,
                country: {
                    id: 225,
                    name: 'Россия',
                    type: 'COUNTRY',
                    childCount: 10,
                },
            },
            rating: {
                value: 4.5,
                count: 2189,
                status: {
                    id: 'ACTUAL',
                    name: 'Рейтинг нормально рассчитан',
                },
                distribution: [
                    {
                        value: 1,
                        count: 146,
                        percent: 10,
                    },
                    {
                        value: 2,
                        count: 30,
                        percent: 2,
                    },
                    {
                        value: 3,
                        count: 15,
                        percent: 1,
                    },
                    {
                        value: 4,
                        count: 27,
                        percent: 2,
                    },
                    {
                        value: 5,
                        count: 1206,
                        percent: 85,
                    },
                ],
            },
            id: 1805,
            name: 'MoreBT.ru',
            domain: 'morebt.ru',
            registered: '2005-09-05',
            type: 'DEFAULT',
            opinionUrl: 'https://market.yandex.ru/shop--morebt-ru/1805/reviews?pp=483',
            outlets: [],
        },
        model: {
            id: 364880001,
        },
        onStock: true,
        outletCount: 0,
        pickupCount: 0,
        localStoreCount: 0,
        photo: {
            width: 115,
            height: 490,
            url: 'https://avatars.mds.yandex.net/get-marketpic/219360/market_cvttlZ9lSQ5itCO5eivy3A/orig',
        },
        delivery: {
            price: {
                value: '290',
            },
            free: false,
            deliveryIncluded: false,
            carried: true,
            pickup: false,
            downloadable: false,
            localStore: false,
            localDelivery: true,
            shopRegion: {
                id: 213,
                name: 'Москва',
                type: 'CITY',
                childCount: 14,
                country: {
                    id: 225,
                    name: 'Россия',
                    type: 'COUNTRY',
                    childCount: 10,
                    nameGenitive: 'России',
                    nameAccusative: 'Россию',
                },
                nameGenitive: 'Москвы',
                nameAccusative: 'Москву',
            },
            userRegion: {
                id: 213,
                name: 'Москва',
                type: 'CITY',
                childCount: 14,
                country: {
                    id: 225,
                    name: 'Россия',
                    type: 'COUNTRY',
                    childCount: 10,
                    nameGenitive: 'России',
                    nameAccusative: 'Россию',
                },
                nameGenitive: 'Москвы',
                nameAccusative: 'Москву',
            },
            brief: 'в Москву — 290 руб.',
            inStock: true,
            global: false,
            post: false,
            options: [
                {
                    service: {
                        id: 99,
                        name: 'Собственная служба доставки',
                    },
                    conditions: {
                        price: {
                            value: '290',
                        },
                        daysFrom: 1,
                        daysTo: 2,
                    },
                    brief: '1-2 дня',
                },
            ],
            deliveryPartnerTypes: [],
        },
        category: {
            id: 278374,
            name: 'Электрические зубные щетки',
            fullName: 'Электрические зубные щетки',
            type: 'GURU',
            link:
                'https://market.yandex.ru/catalog--elektricheskie-zubnye-shchetki/278374/list?hid=278374&onstock=1&pp=483',
            childCount: 0,
            advertisingModel: 'CPC',
            viewType: 'LIST',
        },
        warranty: true,
        recommended: false,
        isFulfillment: false,
        link:
            'https://market.yandex.ru/offer/ss-rzyUK7RlRv7ddRVsi0g?model_id=364880001&hid=278374&pp=483&cpc=U05zywlMspP4UPczStJq7SvpyoeTEHQgiCmS4M2sOgl3qovucLtm5HQJCRq3brpGalOKjDX0PZltzTPlZdP5A-IizcnAWAz2XzgDkHhE5TuoF_W8D4Ga2xa0xICnRruT_Ufe0wJIvdE%2C&lr=213',
        paymentOptions: {
            canPayByCard: false,
        },
        isAdult: false,
        restrictedAge18: false,
        benefit: {
            type: 'default',
            description: 'Хорошая цена от надёжного магазина',
            isPrimary: true,
        },
        trace: {
            fullFormulaInfo: [
                {
                    tag: 'CpcBuy',
                    name: 'MNA_DO_20190720_shift_goal_all_factors_shops90m_c0_QSM_1000iter',
                    value: '0.939345',
                },
            ],
        },
        photos: [
            {
                width: 115,
                height: 490,
                url: 'https://avatars.mds.yandex.net/get-marketpic/219360/market_cvttlZ9lSQ5itCO5eivy3A/orig',
            },
        ],
    },
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
