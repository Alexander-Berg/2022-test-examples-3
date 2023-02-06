/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v2\.1\.0\/models\/1724547969\/offers/;

const query = {};

const result = {
    comment: 'models/1724547969',
    status: 200,
    body: {
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
                    childCount: 11
                }
            },
            currency: {
                id: 'RUR',
                name: 'руб.'
            },
            page: {
                number: 1,
                count: 30,
                total: 3
            },
            processingOptions: {
                adult: false
            },
            id: '1516654826925/cf1bf48147636e7331bd279ce168f0c3',
            time: '2018-01-23T00: 00: 05.757+03: 00',
            marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4'
        },
        offers: [
            {
                id: 'yDpJekrrgZEHGQ54R7SISVI3Qh1knByVlgXJEEjGfeuM1oZdD0NkYA',
                wareMd5: 'Bx3R_wboBVZTUYojO1WQBQ',
                name: 'Телевизор LED LG 43LJ510V, черный',
                description: '',
                price: {
                    value: '25990'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRiYzfrk7hAbRbf3rU0T6oELE0vNSzJXr-k-jP0j1d1LDCI2SOfSF6-Z-8eDMid2uXVKXaDvnFsXeI9LXCRYaa_vQ5LU3cFF94Z5iXeNV66Dd1Byb0I-Hl5fCSdj-P9aeQbdGthCSHA08VA9YP25V113QHaa6LgT30S-Ut7mR-QxES6bQAtH7aXSe33CVYSfKll2i-johxqKh2rjKG5w7vCWchuxcxwjWKhuO0r_nZgFzwWxHUt6etV_wsHN9bLP0cJt8cFenp9fqXIaABRLKt5lFtysrZVo2zDHuK_Z0FRTOXb8YXdnQ9QVKMiYH7_jgx3X4Qw_SZdghatlmMMy699AuPAHT0dGyOQ1LHJLnvq3uMj47BBqN149wQ4qeDT2-BfLwkzmfPAOVWzzWVo8JYSdDA_gVLhbgJyd1wo_3mml3pw4UkiZu9L8GpsYuCG6oIQX3SN3INl2vN_hAltt4_AtcxVYGERYT67AvoU0p3s0ptVxbOZeXWMZzjAQPnjnuxzelqWGFhsJR94NqyNFzME-2XUvrLQugvNQObzONIwHDWlsPO9k4bVAWhY5oFzy4RJV37Y9QWt-2QumwsRLhc6eKoZKAsACI5mdZjYgre3AOfN_OYkK7HunZhk2Ps8CAUUSahkuQP7dKFJ9L08uKTqQudwImlwiZq2kACkPWc4j0CRYY6_joORHMgaCeVFTNIMhoEChjIiF7MaDA9CFMs-lj3BXwnsQ-S_m4z_XLR2lGadqcpbgJ1WmJK_ykYRSZgHRrK_eMt-cnovElUXqac-afYzhWSHzcI0S6W0-sLPBbYqIbWtYZD9eKquVau8R12cPDSMWuvv1lOF5muoN0bBo2t4_CIk1Vg,,?data=QVyKqSPyGQwNvdoowNEPjeHCAo-a2AEEMgvw6Gv7rodwqTifDSzaDcFSJWWZewOkYGdfOIzIrFrV95KSOXbnr_i1GqOS3vke5pOxZzf3790ymeDefHaJyOD7V0BEolLIeI2XlgXnCVK8VjYlYuwn46ACRjSXDUxUMcBxmLY5OzRvfx8ciBQRKt4yec0BnvH4Pt_zymZmZNYuMJ2MYi4FexWHaBoZdFwqOpNYvz0u00a3AOHwTJbkQ1Vf5gsdlAdtW_ov9vUtYBRoD4Iy0eY9egbamaAiJ_nh0r4p9DCOzvWtQztd5fosLOfhilq78ezB_ntp6Cg4Wr7hmVLNpqVA3i5hJnCIp9LH9CxkSCyp73Pu9yGQo6oFeEj_2L-EW_bvVAxZc39CEqFhv-TApLSnkqbnXRBnWlbK27fvHZRLmiqgMhAUkvXF3q_hLhs5T9OTkEl7GHad_-jLVbDx8QcCrS2aAVAf2SHWDBbIB5-MkDvUz21qevejnxPs8nUFA4L8HPIgsws0X6K1fa-I69LojiDHYqPM9hBhORhRR6QkmPFQrXWDWrHGgAI2P7YmtUxv&b64e=1&sign=4700ae730dec2685afea46c605b9569c&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqw2dUY23spY4oGKspqr_obqWcJZ5adGVJ4MYnluuKo0IlBEPa3tThtz-amGkV257QnxPcjzlIMkUyBO-VdJu2aPXyNpUkynndj3CsUicrDoPHtyG_5CrhLRUswN7KYXY8wyZF5N73xFgBNgOHTVW0N3o0X6CaMhrj-CI3egTkj4-MlEy7GyZ7wHvfBLYffOUEPZHL2NGeAbYUouVjyh4-ePKHGeG7WFZ0BvrGHQbjsqR_yxhlea_y4pOmGmNSrv7o0cfrlQ2wDjuB3eJJvmtYKxekDy9dPlw9dsXnLmzoBfCxIubjkPtjyaYqfZjkjMrKCeDmzuNh1h7_lyl1jqRYVLaa-Pa-ydws2jF4K_flOOjqFbY8zt3ZDDL3mkHaBsjlFStoJ8-eQcurwlUYEZNoTNBJO9oDXH3g_wjkTTixMgp6WonAtIEtzd6o7I0WamBHyHWFCpxhMe9x3dTyCRbUHy-HoF4ba5m8cGGEtd8pzHE-r3Y0IrjGHLDsxvkj1ejZEeMfGsz8UqAivDsPB0wQobR05NS7huMp4sQlPZspPBIIhGMW1_p7bDrDOLJMca3GnoZadK4dERPv7uTNrBefg0_PrOCWMcQi4RMdSXfykYwKCoDptdqSUA1hQWOKBY75wwXOpkgKDFKQSC50dTiC9UkyOtC22E0cRnfbLBBO0TbwpkliWmY0S3TZkI8S3p4ft1qtFsAZrEqCV9yIbV2mrI2TWN4AswRjlwzTb199_kskNezcbNgx0oNCfpXlL0NZ-ZIXVYSel294qHoTOBqQRQBscWFUOvrtoqAXAvQaNHg5l0x45WPlA24bKHhdCpAOKhXlexxhWubPNS5ZYbp0xGZAx_1LbOjj5_OvpAOfnw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2UaWiSVZ7Yv2fDnW3KYsBTjPn7vdqsEDhDbCPCCT8pznaNEVdINzm8fsbc1YhRQfMdcPIrNQ1NoVxaO-zhl02C19HiS8sWuZLHO5nnc448XsOOxE6w1f6Lc,&b64e=1&sign=cf85059cf4d2bb62e811e27174b2ca06&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 66964,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 2973,
                                percent: 4
                            },
                            {
                                value: 2,
                                count: 1056,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 1207,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 6400,
                                percent: 10
                            },
                            {
                                value: 5,
                                count: 55339,
                                percent: 83
                            }
                        ]
                    },
                    id: 255,
                    name: 'ОНЛАЙН ТРЕЙД.РУ',
                    domain: 'www.onlinetrade.ru',
                    registered: '2001-09-07',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Ленинградский проспект, дом 80, корпус 17, вход в магазин с Балтийской улицы, ОНЛАЙН ТРЕЙД.РУ на Соколе, 125190',
                    opinionUrl: 'https://market.yandex.ru/shop/255/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                photo: {
                    width: 800,
                    height: 528,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/169986/market_pAzqQXwnZXH6VvZuY6uMvw/orig'
                },
                delivery: {
                    price: {
                        value: '500'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 500 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 24
                            },
                            brief: 'завтра • 50 пунктов магазина',
                            outletCount: 50
                        },
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 17
                            },
                            brief: 'завтра при заказе до 17: 00 • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '500'
                                },
                                daysFrom: 1,
                                daysTo: 2,
                                orderBefore: 13
                            },
                            brief: '1-2 дня при заказе до 13: 00'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/Bx3R_wboBVZTUYojO1WQBQ?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=Gn398QZOQLEe7Th_qcLaKCNnz3faoPM9gTFi1O56VkPHPv9SDAiB6RznTYV1iiBomIK4LZg8KUF1wVMC5_GtKraT6EA1Q1HKyAYFU9JF4pIOMZfcgMw92VWp2w7TSKnm&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=255',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 800,
                        height: 528,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/169986/market_pAzqQXwnZXH6VvZuY6uMvw/orig'
                    },
                    {
                        width: 800,
                        height: 528,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/174581/market_d-0LnavdEcYdxEfphmCJHA/orig'
                    },
                    {
                        width: 800,
                        height: 528,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168221/market_NX3C-s4ZgU5BN80wSjdyBg/orig'
                    },
                    {
                        width: 800,
                        height: 528,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/204557/market_p-TL0YI9bXuhp5yxemLd6A/orig'
                    },
                    {
                        width: 800,
                        height: 528,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165839/market_uICzB4N2Y4W1XHDQAul4gQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 125,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/169986/market_pAzqQXwnZXH6VvZuY6uMvw/190x250'
                    },
                    {
                        width: 190,
                        height: 125,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/174581/market_d-0LnavdEcYdxEfphmCJHA/190x250'
                    },
                    {
                        width: 190,
                        height: 125,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168221/market_NX3C-s4ZgU5BN80wSjdyBg/190x250'
                    },
                    {
                        width: 190,
                        height: 125,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/204557/market_p-TL0YI9bXuhp5yxemLd6A/190x250'
                    },
                    {
                        width: 190,
                        height: 125,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165839/market_uICzB4N2Y4W1XHDQAul4gQ/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZHTtq66TMHpGepHpYqN-BlwTh20v9Mvvz06IpiSczqD7sHo0nWOSp6LHK7DcbmQ9UzbGEKlh2ytf3CKsdfO8dGlmT0rquIWhlSh9ikSKwq7n1eB6c1_R9nxHxNvu3kluxd3_XjaE7KSS6dymePqb_v7d8TnTwxUQGeRSEEG-5n7KoVe49NyOH9wylA0WmfEpDgiFaxNUGY2V-pzS5rw8NDPMjSv8pUE4VPn2FHOG9hdZXbsIlZBavvn2sjByY3bXTe4MdC4TFDgmjCg-PtkCB3qL6J08qs3b9o',
                wareMd5: 'MG6omadv-mOlK9ylHVUgtg',
                name: 'Телевизор 43" LG 43LJ510V черный 1920x1080 50 Гц USB',
                description: 'Бренд:  LG; Модель:  43LJ510V; Цвет:  черный; Тип:  Телевизор; Поддержка 3D:  Нет; Диагональ:  37" - 49""; Диагональ экрана:  43"; Операционная система:  ; Формат экрана:  16: 9; Максимальное разрешение экрана:  1920x1080; Светодиодная (LED) подсветка экрана:  Direct LED; Частота развёртки:  50 Гц; Поддержка стандартов цифрового телевидения:  DVB-T, DVB-T2, DVB-C, DVB-S, DVB-S2; Wi-Fi:  не поддерживается; Акустическая система:  10 Вт (2х5 Вт); Порты и разъёмы:  USB, Компонентный вход, 2 х HDMI, AV, Оптический выход',
                price: {
                    value: '24240'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mccKzZjKGxczuZFo8ZqVcqdm5Es0ALNgo2iD0QxWMdircPzClfsTnjkU5M_eliJGj5RD05EHV-3SMvWrHJz7RfbMTzK0AzIFNHHWNMzNYZJWnpk5F9ZJ1pFkx0z3rSaujZ4ONbMKWP-N_6LHDDZaet0Z5Zd4aHJFFnvf43fypdJvpd8kKijQ4LCUVHsXN87nXS9dgBGXWO_kbBmB4Yz73uB_HM3zqEJhsmVWDf8SSmLwfi372JxgJ5lZSSTVeOLlTxdilFeFaqFVrDtTfzAjQLhCiFPojqqGydJZazZj7U9_yWyFVOLwFst6zO8SYqEf4GRz9bxdIej4parb7pp6hE-AX07y59AsdZiSZ6TNtK2o3RcGHN8vuwEX4uqHiuzt81QI_zFNFDyCZI3EJUx33c_5p96W3OhO--3dgtsnupPhnIkpVJGUBM5-NKM9-5FfxPprr7Hf7HhK0VubGs7jTs07i8BTf9jLkSkHuxiAIV0fcp3Ki_gB2ZI9uHCs8WV9HqnprszmOAP4hpJvwOmZS3D4DdyeDZ0xc84zYhUjREpAzQfc-82UmpLRcrH755h1moNS633_zdBUJ--Mcg4Rw3cXNOY4XHjvS9HB2wtvb_zbX8imn-wrpuVUVVjRPiPLktYveZDbHApLPDCsL1BVT9_-6ipORl5BwuJcBwcBTSBYTHY-5NRaafFeu7BQ98rNNm7Q0cD8hFAL-iJNzNpJUlH0kB1O2iekRpnqTMmPNXiFjmgS46FLHHA5YrFzij5j4savB6X8Nlochlmq4LigrPRXQgmOW0fosZSIP1GjRkqGl2TuUohTYx-6NAMbySP9_VDWhiWcNQC_v2hzSf5ykBkhCsRODmsGFFGui-Jb6wf0?data=QVyKqSPyGQwNvdoowNEPjatQSHtLtrJ3IBS8GRcoCo5OLlPo8bCWaY5CDmX2Vt-Y-FcCT6Z5l9UV6w2RfJhPTxRYpo98589ROY95H66SaHwpUhlwxbGbcYMkuiAX7LM4FiEUqkyvNGzjyrmjjEQL4ep3xbbKAchTh6ofkoy3JY4ALVqWSCm0HizMDJ27TxYHpisf7HaUwKRx1g2T2Uyk9qmMOYJLE09lqLOaY9b0sz5SFRIxFMCjM6qXoTSN9UJfvKJsSk-7U77X90vZpCJACAMOOfblH9axGs8w99ImK2BX2vzzb90yhE3Vnb49iY1kFIHyw21sJ-FqSe2LDoEH9HhUh4bSuH2sS2mQ0nNV0l7uw6N2TCSQ2nQwiPGJDlIyPeFsBi1re3y0UYJPX3BpFGdd3jN6MDxf0XS9lEMcQWsZCe3zh3kbh4UpY9oS0VZNpvVsuAolIpw,&b64e=1&sign=d44a4548aa1c722b6e0d1d664683eaa1&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 4,
                        count: 7257,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 358,
                                percent: 5
                            },
                            {
                                value: 2,
                                count: 124,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 169,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 674,
                                percent: 9
                            },
                            {
                                value: 5,
                                count: 5932,
                                percent: 82
                            }
                        ]
                    },
                    id: 17019,
                    name: 'JUST.RU',
                    domain: 'just.ru',
                    registered: '2008-10-31',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Кронштадтский бульвар, дом 9, строение 4, магазин JUST.RU, 125212',
                    opinionUrl: 'https://market.yandex.ru/shop/17019/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                photo: {
                    width: 560,
                    height: 371,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/210846/market_IDLI4eMlE18ceZXxL5ZVRQ/orig'
                },
                delivery: {
                    price: {
                        value: '500'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 500 руб.',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '500'
                                },
                                daysFrom: 2,
                                daysTo: 2,
                                orderBefore: 13
                            },
                            brief: '2&nbsp;дня при заказе до 13: 00'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/MG6omadv-mOlK9ylHVUgtg?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=hQapvnGv5tiDBabGk-nv1fHEEWZPHjbB6ZNydV7fMRFbGFgmTJLjdQdoMpNefx7mIw5nTCF9RU2GTJuPTl0I3YniLOKA3HI5S_YWrkkP4EDVGHDHLrnpsrNVhFLvqnyWW0mgPb4kLJU%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgeo3z5fdv9bIVttgXtKMPrARHQl0MRvONlQFq4CFIVHEs41UMVrQJVrr7_0L8K1uCJeQjK0a3DeVXinyt1e9UuziSnsNKvQPuHIuht42Lj_MDrsME-0YvrUFbKIiwKVKnWvgtg2cZ4FePB32JMWynm3RmbozjbT_gqrVs-aL55-PRZ6qK5LoOetu5PiS1LEwPOgDIpKG2OM0OV3eIZFVbZ-Mozue-aZnH1O_XmFu-YoRPCgHQziXQwEuWSgT1tAeyRHwLqpjCDY5t7s1gyIjQRC1WGHf8UeNHqPal6F4aXrSod8Uo4kilrCkiJnPFtn8DE1CrbjKUr3VuCA1Qnkxymt9D4TEy6gSOy-BgMX2Nu2p2d7f0nqiWC6uGcEz-3dYJqDHZpf2PVYRJ3X4WXNwm3dE7wMqqgsLJYfFxlt8vxhkMmvQF-vWl35Ef9zYqsEkKOX844FqILocvKfD5oWfw5SkoZE_6kM4hMkx_PF31ftnn1iFbtdERUm9W4vlxpgjDi_C2s_vW9U80DphQL_af8LIiPrYQ1Vesy-YoQG_jkC59w_XsHkOCyj6P4bx4spvGA-zhowgGnP5FAvpGy581cUZPd_fX0Dg5oG1FqJGoedAxYtXXAaNsbeH7yunFLGCQisW-MJteYfIZ03VddjM90bPCBFhOUWlg-rebIM0AWL4QhwtPnUomMRMvdkvZ14YXgwDojq_sf4tGCtKRM5ZjdKutYXu9Dlsm11M2jV_vpHvdKNaq8RwNE-2dfyk1bVdCPCsYbkkLeDzYw1tK6woZUje10hds_cLDVDYymbXav4vVk0xjAhXBiAGAPSjQnEg-_ZbwIBVo88z5K5XG6_0qd7JNxsYEhFSFvSA4-p2zweW9dgrAvnifQFq-aHh_5z-gim3fikOmvhA?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6_y291PUCswld4sST3iALaRP4RxjXAZvR4UoIyeiEmmu-Su1PeUw4dv68QjbIH3xZVmIdmzX3A_iI-0BESmRMRYbd0BvOOpg8gvuUhUf4mpFhQTgpJcjABkxQL_AC1ZvNPlihbAYPr7xjel9C7hCX7YCFlx-iUAUmpGyWFh55x__v9qCJ5hkYXwNOkkkSbXaPdMifUqEx2NrZcxZ-PPDdtzSy3LtKcejJrND1p-DdDibjfeHmX-5N2E1y1N7pYsKsgIttcfqBF3ZSjXCvP2kzhx9573l_8X1aHIzFA0koQPpwgKntm-P4tpZB-UpfzqiWgUVsnRm27rw,,&b64e=1&sign=8bbbdfee8b0ee7c7849666996cec84ef&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=17019',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 560,
                        height: 371,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/210846/market_IDLI4eMlE18ceZXxL5ZVRQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 125,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/210846/market_IDLI4eMlE18ceZXxL5ZVRQ/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZH673vrypnRFH5nNUQRhvsuTGdxg8l7pNET0e8wf8ZwUv-PbFmNKlwzHj7Io6w8EuyexgApt8e-sVZIWJtsJ0FjkGD6dq7MkAZlaebd9yYC9VD2E7oW0M0A9U7eCXoYTEwkd38bJO41POVaff480_qEODXtz0W0RFLwn8zOdl_k58WMkFaQP5oVtJjtX1wmt3IRwthFUXcaBBj0rLQheNkQuVHe9DlYC5_g8oOSudbcztsdJ_r-x4dOZcRexCb22f25iswAz-aleSDDNPDN1MKy6_2UUaivXPw',
                wareMd5: 'uWHU-dezfUsmoT-8hNeM-w',
                name: 'LED Телевизор LG 43LJ510V',
                description: '',
                price: {
                    value: '22266'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mS8RFY25cEUmp9vzKm8XHV4ebQug6V_XtNsLE7APAOEeN52WrURR_PtJW_TVYEMXGaldAxlO6j5RXWKtqzIUbDETZOLfm22UcDeCrAlhZi9D7S2VoV0bZy-QR4HZ3x1MXLzziEx1ah4tJIgwsElCNujKoEemgcXqOcbMMSseusr-MhoKsZisBeKSzGS9P_bEehAUUcI66UJSIbGa91-iUBWb5VOOMjTqbQSGcm5mWAkUOdN4O0o0iSC2UrfTNMRHzyOQs0shD5dbfBL_CjeRa-us9jS4uy3yNUB-TaCp_KRiQ_DLvIeba_O62lQw7bZYo1xqUrzj7snmw7cBOs_ySJdNATFs4BYDXYAGWrA2fBK0nN8fx6k4Lbyf4gxoSM1025a9viF9BJQxy095ol_7OqJR8LJnSgsvm8Q9SGtohQOwpQWM3NoWeBTyZ392_vPOk6ejI2RVMDj_mOcr1ectxmJhtwm1DMNd3xCqvQVFWn6WRCAtLO2i6UP6VRQ3wv9txwUJSO2DFEb_VuJUh6m0-Od4hB5AzKwvl5dBBOahY5wJnKyHtHVnLTkMhsLrQTrQK9Xuvub1QVuGC9BGROlbZ-c9TASa6DISUrpuACT49iSTgKfam92sl6C4IRPljpFq2hHRvrqvb0Xl_dJmslPR_8IlwqokNye-Z47K5sJqIFdhW6Mh3yKtfP37a1xIHSnnxQI5Jkf_IaNpGirgWa4vgxJGPp7jdnr6owOqrw53_we9lTQBoqYR2tsivJoUUULmcxHylV5WI2aH4GnAV4sfTGbWE-77w1cntsP_LcKqo9waWiaowHip5VREgZaEUQNCSJqJa3S6QzlLskYnn2_nCsEF1XMqbXCLcUeZIp5GbaZL?data=QVyKqSPyGQwwaFPWqjjgNtydMHNnehHqr9s3lnVzTSgjShofJ3piqqaru0ev0BslPGfF9hk-7QMcwxve2HU2wV3ocR-yKu1eVhzDpLIaGHOOXPCpw6cGrbxpWooDzDM8h-uMdR6mrudo-K2D73nHuxnxFs7suAI28yR-aHcx2QIHpzY8P-1JSLVOg_HC49Uq4VPjQ8alBz-TFejQb5_sez9l2aA0dGw0owSOw1PIDD85umbzYrKGEEYL8a5XcVZBDuPTVD4nsEoqZiMGzv8HTbI4Y3lyqJoLzbffb_3AEDzK02AAOJgsN-R0s9zyiyVHWhCDkB_5ofQzmPIHn5lQEY2v_A9N9bVcNmww1vGyVHtm1rxSvQObdK09ybvdHePwb6k8GbOqrgXwnE5-MkTp3FUViUsPgT4FtgHHzxjisOhmXi7nJjk1B5OYJEEeo2Pmwr5-dKALp7GezjMXIWNsY3w92TQ0us3uW6LXcEWn9oYdxMLB-39PthZ_t50BcQLD4m6H0qYWEpnLl2J6CzFroXgPg3Ity-XimWnBcszNlITuJ5FJ7RRTDm6E_oEldAOkIT-HcSJ9zaEW4HpwfyAigr4_PPzavdcs1a7hIFlUvbA9NBWGItdjYxgextNWb0EJdMxTQXtegJ9D4fCZ_zl_8g,,&b64e=1&sign=e803740195ade05c3b97b9b83230fa50&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iXfMBV6jT_56_VyzD899EKP894vWIeS0blsXIXROkw4Ja2NtZ4EiloVlJODZy9-lCqoURdft2TGQFWJqjEtBhthSjcDH0-j0V4-9v1jVUt7QFUpV4svKwzhpuxbRmsWyDgy_BrpaPfyUIQtFomE2Q-UE86pkYa5H5LrINAlaNYdXMX0-moxqShR4XSGv4sLK96M8299f4rBVKP3FixR28Lyy1oQqBpcXoo_Km6Tfmy0RUGRWjjy4tfuaMWMZKs3G_kEWJQO4tEQlML8Wi_OYya-6Mciu2UCDOWNLsce98sKQRMAsaf5cD4BD_zsURzfY_UssQLsVCxjTYVIVzHcTGCav0tiINkklu3ez0qVZhB9vEAl0zKi7Sb8Kw8H8MOJR18XfijuVCnpQlD86WTR0UEB814ybHMf-CoEQbJqyYF91K9S8e7IVjYQz04tzJIF6goB8_nVQxLuCAYA1nfEyUR456VY6rHbbgDL8brg4wY_SJoEAC2GMXzidrPwjN50f0pkFR0ob57337RSjFHT4Iq1SbIw4xukC0wHyYZRIerXw4GlMgW6vbHAkcqeAyEqv63aAkYXBsyZQY2P02JAR2CIS2pwZD8dIg9YGHRaMznGX5RLe6yyIADu4GXQyAFTW2etP3my66ftgSDvJKPhKSmHzAdDUYqy5bKsbSQFx3ey63rfGpSIzvFjRhh9HmD_QJhxQWK53rTnl_O1dYj3rqsbdzWl2Tsoe9LqpE-sXv2bdTw9gyrp3bt_PCu4vQ-rJJDo6XofOVw8b-sNduNfDpRLgqhxd5fR1aBLXN209JGuyWw_Gbycmt3EPd8tJZiyEVOYDihj7bwTCiWdEp6vGXyZjd26jpR-DF2h1M7DO2DWb?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2fIAj2sHvgAKZQ_XhZry-kT2wISPjY9p1xqEF57zJzQdasr0gKFjiU3jWKkMFiLVV_6VLNcXz15PBcM_Jo3wmzeLACIe08kTCIrKwcptikNGFLBfo0NM1Io,&b64e=1&sign=62424ed894875cc2628c0621994686ce&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 53549,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 4742,
                                percent: 9
                            },
                            {
                                value: 2,
                                count: 1011,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 871,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 5055,
                                percent: 9
                            },
                            {
                                value: 5,
                                count: 41881,
                                percent: 78
                            }
                        ]
                    },
                    id: 1672,
                    name: 'TechPort.ru',
                    domain: 'techport.ru',
                    registered: '2005-06-28',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Ленинская Слобода, дом 26, строение 2, ТЦ "ОранжПарк" (5 мин. от метро), ежедневно с 11 до 21 часов 8 (495) 258-81-30, 115280',
                    opinionUrl: 'https://market.yandex.ru/shop/1672/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                phone: {
                    number: '+7 (495) 228-66-69',
                    sanitized: '+74952286669',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mS8RFY25cEUmp9vzKm8XHV4ebQug6V_XtNsLE7APAOEeN52WrURR_PtJW_TVYEMXGaldAxlO6j5RXWKtqzIUbDETZOLfm22UcDeCrAlhZi9D7S2VoV0bZy-QR4HZ3x1MXLzziEx1ah4tJIgwsElCNujKoEemgcXqOcbMMSseusr-MhoKsZisBeIzdH227wKU4FcZWQiiKAU-4b4Ga_z0AC5B2OVtpL1l8PnVma3AHh83k_94TJQQKWaxbUCjHF_QEHXFGpgvM2TUbDAOZ-UyIOPxN2zHnrv7eMRzcbuO64T5_19AtihJudFbZNSmOpXIHUq005iofi8g8PZyFNABAOAY4KjFBNN7J8QXbY2coQIt_8zkz7P0GNKPgmxj-r60FuApVAZw-H9XWuN_YEhJ6auul6pLJUSIJ3wGcqIv5_ub-TWfvZixsQNsQtA2lK0HLUzF6mz95zgjoIrelRAzAcEwEP_QlhJM0s_42_K8-brlmSoaP3IEkDKMXtEd-vj8AseSE-VLuo58du9aE5S3Q2Tlb02gSdNrOzxDIpf0eibaw1Irxhu7xs8wQJkP-_9hZV-kiQhTyDRJROibuFlJ5U4ifWQAmIToOG0xbLN3cpjly0fsqxdPrZKy4yp7HE_4DnLjhPFJwCZ3vKtxTSNgYlQrEWIAkG9cFcEX3ihVFa2peSW3VU0icq-FPWdjWeaHRdq5_HGWlY_gapRulBpsBRt9VGdkyzH0qdTRzzR9r8_a4H1KPdbsVDgOi-8Mz0jZOKbw7RXkjhLqOGqmi7lWvvuqXUbnC7jTH4t2_gmm67sBYU14f9DSDne5SGdPl7L-AvSReWWqyeoYLNezyLVnbc0-QdK3lweuy3GQ8lF3blIL?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-kQ2KNg2f9H5aweVDZOZtdyIs72fAX6OO83vfUl-DuS3xwwwKEKLlgAes619Rh-5zHitIbrLl12OhofpHdGP16WJbsh1d-AFOg5Etant3b7-y_WksPk2JXMNLevNlqW72rNvKG4_U6W8xLpfwHqmC3IDCmwM0qi34jueHQHRx2VA,,&b64e=1&sign=4afe0142df5ee663d82cc03c9dde56f1&keyno=1'
                },
                photo: {
                    width: 300,
                    height: 300,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/206654/market_myG4UW7srrHrPHywu3JIHQ/orig'
                },
                delivery: {
                    price: {
                        value: '500'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 500 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                }
                            },
                            brief: 'Срок уточняйте при заказе • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '500'
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 14
                            },
                            brief: 'завтра при заказе до 14: 00'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/uWHU-dezfUsmoT-8hNeM-w?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=11Bep3zrneuUGcd1rsQWUe12hWnpGqNx-FA6dDxd7i2lhUVNNbLgLfnzSk0PDFBijD17aNIbf5m1CM-ItTlO0zKx4heCnBlO7Jjw-xmXuwXZMqQRxaxgEQdtBth4JTjpxA3hr8vHftw%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgaAxVu81sz_2eKdWNO19as57L3XjTz383nDcyDJ9YhTqBETJBN3Gx8WXKNFKKoJVtXY3VaG1Xd-0tda9wL8CE31tVyu6tzuE7hyTQKawlUPOkrxcGtEzjJO4QHBc5-JB19bXksgnYKUr7cVvH71M1Gg-3uX0Xj7tOtwTxnSS-dg8_P3LLRkxPnLZOX2ta9nXtRlbTAYw0OAqjRoNMMotkO0_qZ5qhsxGfDkVh-cCInk0FfmtXw93euj-b3TJNLuwf6MpuDYtA3dZDZmU7iY6cHNd8RWg9jyWWoD1p29ts8mSIBZa1IEl2rsI8sUzxJFXqbetb_KYbR5NvrsBfZIXcBTiIaAWAICmXYQrOUQ3npEHgVsKV0Ft6tnM-fm1y_vRfnj522oR2ZHFcf1ksqkt2Qo2s6uZ0VkTKtskhtgbgICWYhhssRI0TzIOvStpBly8T04Ku7NWkMpwHFe1xw0bIEadU6fLSLXBcUM90rkS_yBUEV37a4Ptg2biGiT_O4XCkAU6U8WdGbwuJ7s3A3jCutD7yNiIEoAevv7zakEsAAnek01Bx7oFfXqvV93xeJuzF6LlwvuqGZG8ItDfwhE4fbXbXj9-J3uclfojiAT-uZRKW-ojfE43nm0e40g9Sn6cmWCSIKQcOaSOEvqQlPUQZ3m9Nmh3NQvzBCehIgfPZlKb5Wo8JtLTgSRxQnJrCSOVnmtTVICu35gt-Kw0oSI9bWyvXELfrwnuAQJnVYEtzs6GfqAYacZVbgppTeihK-tVd8DTobWLzz20LIQCU00Y4Z2_1FNqUm9KirqCYrARBXp5buUWV0KtWURzDSwxdvtvVjlDLlkDOPkr6UpGK2b9hpIHjdGLIGjYPWcdlPsag-tWmEJCZy1aMxDMSWQk6LmhUQEaif0NxfpU?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-4y1UsTYGsuxB2eT0SsqfqQCENuuDyHsQDICkKN2McC5H3A3OnEtEkKZzjYoqGVrRe-pflzkAqLb9h_pDu6M2ZKT20ngIZ1wi-IBEffeLrUn5WkQyWwZIAzkgLVHaY1F9RkPa2dGwgOpOQzvqJXemAzl1nYqLa9rjaVKvXtrcJW8k9tElH0zXSt_VNHCt0Jrb5v4q-WUb53uKIwURBDW4IvNtTUPHabimiBdogx1mAoKMLMnieloCnL_GT_JvM9bbAiGMzVcYxqSzEhDuKIhQr8QaX_MolYJCvIWM4EzUCFQjDGAryo2uELdiz54TAUelDouVoEja02-g,,&b64e=1&sign=84d805b187d626f05404f7d87b30e096&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=1672',
                paymentOptions: {
                    canPayByCard: true
                },
                photos: [
                    {
                        width: 300,
                        height: 300,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/206654/market_myG4UW7srrHrPHywu3JIHQ/orig'
                    },
                    {
                        width: 300,
                        height: 300,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/229624/market_8Pb8Z-VOSAf7qT1htk-vYg/orig'
                    },
                    {
                        width: 300,
                        height: 300,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/213450/market_1svfg-xTCk_B4o_-qfs2mw/orig'
                    },
                    {
                        width: 300,
                        height: 300,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/225310/market_EQQ9EmVcuoYDarcLW2rMtA/orig'
                    },
                    {
                        width: 300,
                        height: 300,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/225310/market_A4FDuGhbRxfwnNgAxvrnUg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/206654/market_myG4UW7srrHrPHywu3JIHQ/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/229624/market_8Pb8Z-VOSAf7qT1htk-vYg/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/213450/market_1svfg-xTCk_B4o_-qfs2mw/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/225310/market_EQQ9EmVcuoYDarcLW2rMtA/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/225310/market_A4FDuGhbRxfwnNgAxvrnUg/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZHyXZWnWVyppX1pD7ROqZxtC8EudWLIrgc3z8cRaVwB9tDZvOdjRept4c_DSh-49pJAZfJrg5Zu1oP7NwgmY5CIOZd2SdVdprZBd_ENojWWyW9RSKhYW00DRznC7HqA3sM4PKCp8Yw7XIj4azdwpa1xTvirNrsnyD1ilX46fZyCDAaWelq2KnnmNvbUVH4wMj_Z52ck76oLM9SqNjBnEmbKEPe6DnzrQUfz2wd_pG1Vxj249j4FsSFx3A3dtC8F-6Yz3O0L0EVBYd0KOVCXMb7qieDMje242-A',
                wareMd5: '9afvNBbAMWbGeabzNoUe4A',
                name: 'Телевизор LG 43LJ510V',
                description: '',
                price: {
                    value: '22980'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mafhF-2nc785dDaFRIgdVAlv4kx7Mv_czmrre18hEoppgWYB7sFLzSWO9jJiZ_E9Uh3Mn12A7gUqrZQXW8kL8HRb5Jxq7J30wKdqHfF2L9J3M_SihDW-BNT3p1aawWvfN9_5pdWTY6mMg4VS7_I2q9ztSXslH4S6uySSAT_iVM6Ol9kDrQj34ZmHK3sh8pS3dTGqHRZoexMxoBRid6sFjSjhu1uBO8SUwx3TKlTiOwBYYBAT5Pp9AjcIcYZoSyXwFhC9IIjb47qU2Mi-3MAEYh-JkIaea7CO2kjnZm-uf8ouZeBvkojONxUEgxV6eR_K5BJ3U1C2R9nNIQx7hcLPNhPuAJs3U2ptnl0XgVbcVAq1QRJJ86dbLyGk6VFHnx3STCRVFe3fU2jWDLB0uxwpmUyDiBEb1_Shz8v7eVNcZuqtoR57zhXlCr4EwMo7nh3ZGmSJ3P1wasqks9-dIr2kgE8eSoQv8P-qAVAImaZq073c3IFCOR9ij7IrHA7V48Lj5HCByz_k9q_0xuqtHyxqdL3kb_DYdxG4ZFbHxQVp1_AiK81a8LjkF4SGdeO08weO5r_2dbFhVv2TxWL9tcCrmLFzjyfZNCRv07qntR5HbAionh84gyY8zePC280nuzcr7MxoCYy30njCDdcJI15dTNTI6uCCSPyJg0qJhwYDk171oisJk7a6cJx0-Vx0vHNUZ5EfFnb59R4zU54-bvjh_f6vFfMCjLrePMd4pvEhArw_ha-MfNOvRpyrpntijZlIZWb6EoADGQ_bR76_ifquxVop7oa8_WO24sa6wUB5vORcKZrpFDhk7PF_PUPkeyIBNkKVdS_W1C7pFzTuhZBXZsuk2z9VjHfgJ66tMVwPWMwX?data=QVyKqSPyGQwNvdoowNEPjWjJwPNkcQ3e1JCngwCpz3Ts46XKE1d0rn0RFqd55ILR7l2uHWszp8xZUrs_IMrOqXDTJ-fI-qgniMwZgD6LGsHyr1dqxKYxPz_1Utcbezj-wK5fIKWcqPAi1S2qTkWI0xqhKhu6iUqdFfsFV-pxC03FxJFnAH91ddZ1Ln-VUgGXo8rE_Gllu9u7PNtwS6uesudFLct8NStkAdwb2D4YgxZMxtLwQ32hhNU6J92pK88O1JYjMMO9goT31QhZTW8_2zYk7rZOmRkwZV6ujdlBr_NrBRJVcj-oLyVd_TiPDotroOZCEj2QKPofNiyCUhj5iO-6t79wI11tpfim19EkUN3SzrvwDlueIfCZRHglrrJtoigBMOviltI,&b64e=1&sign=2c225867738dc2ef1862b812227a332b&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 4577,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 200,
                                percent: 4
                            },
                            {
                                value: 2,
                                count: 45,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 40,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 317,
                                percent: 7
                            },
                            {
                                value: 5,
                                count: 3975,
                                percent: 87
                            }
                        ]
                    },
                    id: 197695,
                    name: 'Assortiment-BT.ru',
                    domain: 'assortiment-bt.ru',
                    registered: '2013-12-12',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Нижегородская ул.,, дом 29-33, строение 27, 109052',
                    opinionUrl: 'https://market.yandex.ru/shop/197695/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                phone: {
                    number: '+7 495 256-22-56',
                    sanitized: '+74952562256',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mafhF-2nc785dDaFRIgdVAlv4kx7Mv_czmrre18hEoppgWYB7sFLzSWO9jJiZ_E9Uh3Mn12A7gUqrZQXW8kL8HRb5Jxq7J30wKdqHfF2L9J3M_SihDW-BNT3p1aawWvfN9_5pdWTY6mMg4VS7_I2q9ztSXslH4S6uySSAT_iVM6Ol9kDrQj34ZlK4O8mPT3ZeXkGcKxN6McYWkoALCkaIBISVd-NxRRFGYJhRolquWPUsUOCjWjA2TPq2D6B1ytjApUcyUqts3fQilaSQxZGWsI4GD2L6o-FTkJKUub2xDRgxePJcd2CG3MgYyiTC3s6C4ICiMK7CZ15D6fJcvLZS1W3b7etJbOebhWabq0V1udbsvv2SEQr_stzxWfoG_I--CpQYZ8q-MklkHKE73IPpTsAFHgDPWfjlCxXixx1Oq3RTcphVSzyi-wvULEVlFXApoCDLDFn9fFpf0bXbYDka5IAejWzztdgD34dXlctxmuT2szP-7375ubsjiMpsuTU-QBxMjXea-VolktZ99N_NaP8Mpf9nXYU_SLrL5jDvCqP4KUNbskz8NsjueqqK2IJz3lyTp-h2zl1KQsXowU-bx-nUP8_cGwTC_jl5Q5oCacvjdrDNlehtNzQohh3S4X64wQRAufFgidmVxpFy52DnZx9gJSLYiy7DuJWrTRgCAAEUiQkJJxGTGVeW9qxAAWKPWyq0Qc0V_a6S6erWYJDEeADy-N_l_3jN6I3wUQjiYRviPI1O_XKPGDG4k825PGfVQOrl9HIduFvizvYmJKLViOzqn3xHDSnGXPGNgBRuu-KS_g_gXIerTHUJ1WikV3Lw4DwSIrbvu3vY7hVZeZ2HWq_ZIE8xHeiLCZe5WZ037h2?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9mz6cYtBKXueW9MEZJ3grSMNbEYwLZbz8AgP5p0mMLWCe9QKJx_eJhjhO0VokYMFko8hIuza5KLrN2zlEfr8yQQxbNrCIbKEMv-rqj7hqY0iFS-fi_ioilIbaxKweExhc5S7F5hi75JfyznOFhZLdgoJzJUG2ifwCj4v_HFIRx7w,,&b64e=1&sign=e641cf9d54525d121f3ee6611b2ce927&keyno=1'
                },
                delivery: {
                    price: {
                        value: '450'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 450 руб.',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '450'
                                },
                                daysFrom: 1,
                                daysTo: 2
                            },
                            brief: '1-2 дня'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/9afvNBbAMWbGeabzNoUe4A?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=MZeo7j6RjEAdP-V4g2j8taHEtnjufNK9I2fYZ0aUHIM7TiQAu5KflWHDRGwIMP--ST6cscx3fN0tS6LgbsWbR9FBKBR9F-eoa8o9z5n5W9nr2FBgMUbnCkNlwiDEiz2VhcyoNKxiMs8%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgVT0Cfd3Me291u5ntAhu2ND8QHcL8wqBT7LPE2HgXFqzIxH-JSFzgYwmfKKlhTaQxAMUzxm8zQzGTZ2JWRyS0BklTka-m2bN_7lXFkpovKtvvw6ulAHMTqCjX-4dKkNxvbiZdnyCap0azyyjViUDFnYNBRZ7Pevb226HUifYmps0puRgkbzL-yQY6oVgOorHK2F96s3F8hct9kh-MRIGdrImzSIwKO-V_hv1txvY99Vcq5kYN7tENzJsgcXJYrY3F7GzwRLARnapeiiHIzSZ-Qj_JDW8HJDywMmDqniZeZf-J_0AcO2O4CBVbU2hY7SkU9geWaekYWeH9ScxDNOQtwPwbz7XK6IhtpSXWuuL5LeAd3YmoWHl_lDH1qjxYtvYhZRylq2fjASUWu5n09tgvTdD8sncabuF0YQuAEX2x9keT84SHEUGkPMq5AyaGzEG5Ew0zSjv1PAfXyRgPq8nRzcOq4S4aFMxDGqG_Gu1my9YOfHvVs_XCX3NJCHM4Ldp2Gnp7_T1oKxxn7hmoSPS83MV_cPCQ8oeExOsXqDyqrmZ73aMk8K6B-fmrmjLJUsFjK4ZvuicbBHXR0pt-eRQOVw3BBPR3w0dAo-f0GuUHjXiAO8o2JnaWbnpxJ_DIku-loJeqqX0_NJtmr5juMazJkqqgacKf5_EFKGZKjcMl552tL-n2CkCsTGd3eBMLIv7l4z-H7gTCVQH0B-oFoXTMa3US8GNBs-L9vozSFYE2gnj8pcNNOHEPb_6xpcyfqmLUaBWB5PROgDoAiHhcumehNmTkkdszg52wwfsqKduDWaPWQnaU-8FctpCLrpP-CofFixpPNXB5BA2NsYGOl1DyJZaMhWY3v3w4oTgBfM3KfURldXKZEGbpvgUowUbLLH24dpmGauKijwv?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6SR8_J3YsECCdema3JOzJ2sCP7rN21fQ_Au7bP8qD49HLTxCdO8hbCjJjrAQdi9ymkV1ZtBLUyDJXhC7tOpZGCpHsqHtrx59s1j1MWfjobLShrKWgwNzU-GeJSPWf4Eyok8BUgh71-SOBUA-VI7Yv1vqU3Eaj1t0N8z_IPuj0SUE3ViXjo91lWnExCY-A89saKei91RvMjZmx1j7yi0M3Gde9BakA77Mw3rjxWrUVo3aEKBjY2adRwp65RhJ1Tqdigws-eTE-8Ev9xKHJ7wU5MY2nD2tkCALy3od94j9lXOi5V04ASl9j9vw0odczlDQVvz1idXPQgJQ,,&b64e=1&sign=6b4a878ac96da97bed744dfad65039f8&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=197695',
                paymentOptions: {
                    canPayByCard: false
                }
            },
            {
                id: 'yDpJekrrgZGfEzQMJjn2bTyk7kV2qBLY68FfoUt384KZYq_BKar59nNYBtwiq_srkFs4v1pm6MNUYjmANmAUJMvrnyGSpg3tCpMqOyamkCLtGtjUiQaXeAX6enWmCozHyfCjVInvhjHrgbtPieXvWSdxk7KdMLScIznYiIAn7KajbmOQv8aVu7Xmx6UgFp7gVMr91fgJ4x0xz7rDf90rGrWpSNri47ZacMNCuA9_L5Amv-aaO3SZxE9tf0nedIu4l1Oc3mkiBW4Tn6b50huwhOHVJncg5ADHFKKfzPMLQHk',
                wareMd5: 'Ou2TtcY-MnAxOgrOGl73tA',
                name: 'Телевизор Телевизор LG 43LJ510V черный',
                description: '',
                price: {
                    value: '24130'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mccKzZjKGxczuZFo8ZqVcqdm5Es0ALNgo2iD0QxWMdircPzClfsTnjkU5M_eliJGj5RD05EHV-3SMvWrHJz7RfbMTzK0AzIFNHHWNMzNYZJWnpk5F9ZJ1pFkx0z3rSaujZ4ONbMKWP-N_6LHDDZaet0Z5Zd4aHJFFnvf43fypdJvpd8kKijQ4LBtsDbwkEU7A3VOm5K7XXfncEsSp6Ewsj3TWxMZ_CYXYSda2fa9om7QSDhEtDkSLtb9G1UyncahEHPFjrjS-zm8nykgPmQJN5s2H5ZWoQL1V22A8gc7jAQ46LWhThKQ8K6No_QL4DmK6BvVB0Pzl12vgYZ0uwgNkVEWb-WufolSYXP2OqdpQopPf385OVTPbe-VAN4fGkKZKnjc3sak3vE_mXNLtHHScJIsT7hlx9det4BLPB4whb7ieXvxFTuOAuHGdfP-p73UGx0WAhbdJpphEhwyjabjNO1LYW7xck7XkJDvNvrD-IlLuZw_rPV4D4vtK-xz3Y6beahJE9RVGQU4293AafMepdIVxk_SKStBA4UO1ginultBfbIwPAdKdPRygWWRVsLTGMOJVkU7kjb8P4AvzBpGVhzFzo3m6JCpd7lfdRZE2KowMwQhN6t-ivdFKcjCbP9oLE09EsLZrFgMtQUaEp7rX5uVap1WRMUfiY2FfG3pC1Qik-M6-y2sCfjywhZtWX3wji0SvIGXp2GDh1ZclPghtglIb_aJPRiWSsjGMqxjACjJIvIi_1N1xFuw4uzo5-6oYnqMpgZqM_OYLSJ40BDJ7jw6Z3hFPtcP9_L0Y4p1FA1I1_82f66jYQTWoTn5Xv_yMAHy3TW1uO_NIKMwLwyJnfwroAwAdjlAuK85QLvFL6ML?data=QVyKqSPyGQwNvdoowNEPjfgvYBxl16rtNrFl2GxiUuYDAjRT7vgYK2SStEu2WGUFiHSL_4JrxSv0N2TU5jMPyzc6GUhSFXZbYsoLrcvz77vf8ODj1vMv5oD71KoObDXFhfXAVtg2BtmlhaGSXvFHubhTEK0eYuuF_dUEWQOqa_RnyCGKhmARkL04WcEWRGiXU1jy_zHB8XMyQmbv6MNN9nwoDruEAa_GzRaXXB8S-sDGdDamQuFcXLr3KAmMCaoY2JwUuu-GcsNezHySouFs_oxe6lYHcaRekq82sKTS63Cbbwwiag6AREBecegpPDs-BAj5iLb567jOwml96DnIqqyTyCFl_q3FIO5EDWyZmwrCTtcLKIpDq27wfVeAunzpE9qLlsz-ASKFDtbVQ7SqWH1nhWM8K5tzoXCe-3MhPoZUwlEBcURk2FgdQR9bTTDTwYZyvu6xi2_cj9KjZTgdQlUDhPSfwATv7zQfwW0Tow3QYr0owM0Byw,,&b64e=1&sign=390ea095ea55b90ca1b562c379cf6528&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRvVL8634LDQ-RkUV1yGQuDnTRGVGBnqUzuhI2KHQVo8v2BS52D3mMiPNEuyQRQBGCTaxoJKG5kxOogF1NTXIuZ9lq9AH6x_UTv5_zxCEE2m3bxnpxIk0KFN8GjiLYCk15dJcF4bASyXNOTSTE8eTl8v74V98gJFaDEh2464kbxqdVnlfA3L_oJBcs06VxubS6QL7zBgWmuggff43o319C_sW1RtrCsO1bghdVNkTytE0wVxVzzGY8NwQMsjttIY-n7F8cnyP5jLmwn4kFPgOTu9pBRXpN2o4nHhVMhTk5xlXZu-Z0P4Sa_7qxKc54qv8J5j8y9n6bEvhnL0iKAqzeUZaOTG0HxIiwqFJT_Zm31ICxzwdVbslDll8mbQKBgko2lyamAZLHBDFSYMNKlNRr9ms1uHyL9MTR2d8of7Ep1-G1UwKYtskuuMPlheFlXUSkG8MLQvEdjXhikx5EW3P4LOEjGnt0SDrPsz4bPv9_QQeaf0lf-FN4REg6VDUwtDOMLNM9tr0yicSAsaxNq6PVGjJcBMcomLN-ce73DRZcuEQDhIxYGs-ghTpEefHi7wZGDlXzbJU8D88kzcm9jzTPsAdr4bHVaJ2uBtHkCQYlDtueQDNU5ocz8xduW4obyrr_N_IuVqyksTxSdcdDHoBb5OENaRh6jfc1rCV7iVzlA-owrIl8PDpPVqFv_TeD6YKd5S3XKvS_AYjqgxUCQ1r3pChGPYlu0HlI4tjR1yu2ZkhKA9FsQKDZGew7gxB4cVY1X0O5doFezoyKjmlyBBQxEXjjLJpcnOz6TOeMPsdiGRpTF6K2Plc_dxqeCYpib5rbPTxRRHR7gS8T2TPKXuI2o-oLaoDu3cT9B2_bVsC02UNqpsTjPKAFN4?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2STjPV9CxegVynNE1wNwA27BjVsDWZBo10XU1TYz60ipLxg91CkiJB2beJb122DHL0xPpXTo5qeh9pOAsZxMLhAaprbdd-77_EeIBdaHt1POBLlNB3ewFNE,&b64e=1&sign=5483db97027403b9fd762f17caae90df&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 4,
                        count: 8669,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 805,
                                percent: 9
                            },
                            {
                                value: 2,
                                count: 233,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 198,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 641,
                                percent: 7
                            },
                            {
                                value: 5,
                                count: 6793,
                                percent: 78
                            }
                        ]
                    },
                    id: 5570,
                    name: '123.ru',
                    domain: '123.ru',
                    registered: '2007-10-23',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Барабанный переулок, дом 4, строение 2, Магазин 123.РУ, 107023',
                    opinionUrl: 'https://market.yandex.ru/shop/5570/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                photo: {
                    width: 533,
                    height: 353,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_-SAt3_wG_SOWRSqXv2kazg/orig'
                },
                delivery: {
                    price: {
                        value: '490'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 490 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: '1-2 дня • 1 пункт магазина',
                            outletCount: 1
                        },
                        {
                            service: {
                                id: 119
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 2,
                                daysTo: 4,
                                orderBefore: 24
                            },
                            brief: '2-4 дня • 26 пунктов',
                            outletCount: 26
                        },
                        {
                            service: {
                                id: 119
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 2,
                                daysTo: 6,
                                orderBefore: 24
                            },
                            brief: '2-6 дней • 13 пунктов',
                            outletCount: 13
                        },
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 2,
                                daysTo: 6,
                                orderBefore: 24
                            },
                            brief: '2-6 дней • 2 пункта магазина',
                            outletCount: 2
                        },
                        {
                            service: {
                                id: 51
                            },
                            conditions: {
                                price: {
                                    value: '200'
                                },
                                daysFrom: 2,
                                daysTo: 4,
                                orderBefore: 24
                            },
                            brief: '2-4 дня • 53 пункта',
                            outletCount: 53
                        },
                        {
                            service: {
                                id: 106,
                                name: 'Boxberry'
                            },
                            conditions: {
                                price: {
                                    value: '210'
                                },
                                daysFrom: 2,
                                daysTo: 4,
                                orderBefore: 24
                            },
                            brief: '2-4 дня • 127 пунктов, BOXBERRY',
                            outletCount: 127
                        },
                        {
                            service: {
                                id: 51
                            },
                            conditions: {
                                price: {
                                    value: '210'
                                },
                                daysFrom: 2,
                                daysTo: 4,
                                orderBefore: 24
                            },
                            brief: '2-4 дня • 9 пунктов',
                            outletCount: 9
                        },
                        {
                            service: {
                                id: 106,
                                name: 'Boxberry'
                            },
                            conditions: {
                                price: {
                                    value: '210'
                                },
                                daysFrom: 2,
                                daysTo: 6,
                                orderBefore: 24
                            },
                            brief: '2-6 дней • 19 пунктов, BOXBERRY',
                            outletCount: 19
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '490'
                                },
                                daysFrom: 2,
                                daysTo: 2,
                                orderBefore: 13
                            },
                            brief: '2&nbsp;дня при заказе до 13: 00'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/Ou2TtcY-MnAxOgrOGl73tA?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=hQapvnGv5tiNe9C6gtOWZMEZST9vpbtBt0XUgBXWyoeGHNhvEVE2gu2iPnRX5i0wF7Zv2EDWAEiFVQnfxOwxupX_fy9jRReWKcOOVqo2n7jTae77_0Hu3ktfF6DtE8Ydx952YzpmfOk%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgSvpZjouigTTrAkPrQzSL2eruYF7MpZ6PX8Bu98JEK2BVQAvtZ1-8_8aFP9NYPinxaOxtwBSXgXOWmi0TeSktss3VNCgjPep1ZTWVsDxcoQBPH9_fcdPsbEEfEAbsskiRcUeXbkAMKxVc_6WkEldofFdN_6A5HYpRYupvQL4ui7PmhgLeJ0wJe4Ql9rG2JO2KL0132IaNCnPyedrxscg63E9PfARTJe6Uy8q7n5qF9dj8jnu5n5VlBm1ijM2vmNlUQwqZ2_17NfBVIrYD5z-hrdDkV0BstCmjTS3XIXIvzxj_Byl_ThZmgcZsrPlDeUzr89js3Zr8Wj-eGSLjv652lGWrvUx1JWlVZLB2Kj30bY8HmhuKUDHexHGzqWgzr4dfaN5CYCj6K62VoB8nXFrO4FI_HLoXQrQJXkHacEK3q0l-aUtzzQpIChSm1-Lxe5bgo3PXJR6qcrafvcWcnVYRov9mLfsSLKRRWNjSPGXVQV66zqFml-bHpipTILoAENpxSmkn98zB1KFa-hBHp5aa2ZJdkvga902RPO30sFVsvMDA7bg8sr5_FfUsB_z3OurtNJfDjd-jGbfH6PuuqDSUuRnguv-eLI1nOdvlIYFeSdn684olQJfthdud_Z-CPXUHYknPxI82RawEvqa1wZpIT-208DNnTVAGd3iwLPjH1Lhlvlz8N9dFW1c6zKSjG3qXyMQXXQitC9hWCUMI78Mw3-ZCkPixCrRYgZQWmWp_GOLaTDuE-Mg-nFpRn7fvrm2Tax67SlyXXTVxqXk3_gWJjHYeNCs68UTAUQ1JJ_4whIXeCTRTpxtNOQjvIbBiF5P1TYakdZ36gDTAi3xN3VlzgFP6Tu1ybHnimG3s7-w7CaSrz7h0CkGyQKCBUU8rg6T4sEMHfsn_NA-?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-4-7e1XKNfv-aHd-S8bJVRFjIenQr6Dv3nOQRD32SHvS4dFqbcd7p0JSHSNWBf1yAdgtouhZfTHrpfFY2vCzuFulfV10ZCfmcyek63T5qMZ0rfDOnfe2Zma2MfQ5QI1nXXbNtYLG5_dyb_J0l1Rptt8tzHkpGwWDiYX22P-ZD-azt6mezPubgbxqnTv703p4upVdDNwxoKFMloUxdgNq8SOAQO4PeLReBs5t0TLcNy88R-roD14zUnafzJgBdwDiyR6LpFALdLmVOeiuVqP-JWmWKhb8Pgy5zGx5Vb-prdLR05hOrbMAAve3owaOCYV5oJtQTaisJavQA,,&b64e=1&sign=abdb63d39d5b03a5dbedecb3d68e3682&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=5570',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 533,
                        height: 353,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_-SAt3_wG_SOWRSqXv2kazg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 125,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_-SAt3_wG_SOWRSqXv2kazg/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZGUhq2ClsAogMesaZNVNgDaQcXHEKIL8E_6XnFDMoRbYWs6FWpuiTFzmj1Bv4Cy9v0h8pHqXcYxilgWC3VnGtosACTu3ed3oW9ZPi9xGHniMdq31TuiBGOexzg8nQ-eNkmUz6N864OcYOneYwxGWsYp17n1CVBObTGHeCrw1xaSTZERtyTUtkNK38xJ19UQpkzA51g2_1D8vuYaDUhJEbVfg683TsozxsVNcvABHNjjzQN5uL9gDMYCy9ocoCmgg1aMy01xgpanlIHotHqI3bHytKlXJSYxTvI',
                wareMd5: 'Q5tHTz6zgYr2yHRNLpigXw',
                name: 'Телевизор LG 43LJ510V',
                description: 'ЖК-телевизор, 1080p Full HD диагональ 43" (109 см) HDMI x2, USB, DVB-T2 тип подсветки:  Direct LED',
                price: {
                    value: '23990'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mT27lmV1IqjAE7m4n5u2ulVbVkU1KUkhdoN4ogiPLRRnSuYn1qw60wdYQz_uGZxafMNuOYXfwm2Q7FJqmZ4YQa_TXja4vggi_cr0Juyt1rgpC6vxWZm42yqSXiWWb-agCTlQzahZTPpxl2kEhd94a_obx7xCrJvf5k3n9gNVrirvJa6a6VcqBstKbFJvtKwD00thJEToNuG0PwvqimaLiEzN_mVR8vuoHZp1KIUSjS9j14kJ6ereAnIn6DK4eN2sat2WzTGQqTrY26eaYP8zggQ9eyeGFEsu0l_VoqTAHbLxbGRqRss4O6N4yCD4a0Ssfd8yXPm10nubutTPz6xE5pVz5GQ71JhUnP75cLuJHhhCVimxlUPc5NfTtOAtpKht-uWcOvCCU4RO3eGnZhMD-LBBabvAEoIzKqu1_gkXXvBfdALMwqyURl_-FMrlTMAi1ZHvJS63-MOLJ9FnCQoWolfWLZJbFNtoSuu__lEP1OVIlwAGQIjvdjc6bX8WgNd1IXI4-rFibTtKxNMi_b-HFlPBvxBplIesRNC8PSAK2-O7JHVvl36p6r-yhBRaM8aDM72gX13klfEaoBBqIGXR7Rsfe-sUza_BJFE-QBB9gZLEvU33mcOCT4qxJ7--k5Y2V0lvNrKA0ByNBgfbhSfa3HtL3YFj6lXlUnZrrBkpN70XBl5w8Mwj4Z2L-8d7HfQf4AFkn9SlxezV-63IpbqjeG5mQ54JDz_kC1yMl7nBqIQEoAG-D4HaQLdaBa-0CtvN-rT0lSsYdtHq3gkbM4TqRetbKBc3bJ-lXm57US7sbzbgwYDurlSzSJAlNrxsxVhawvrqeHHlKnjxre8QdhJN1J_6CTlCXnfBQKMareMTDFao?data=QVyKqSPyGQwNvdoowNEPjWWROvLmyb8K3iqArS-4LD3H21mQtIvZZbF7xnkB5AhnaZfcog_89hdOsguDscqZqICH0nhL1IjV1pyAUClvt-uQ9sxmKohVhtVUUMMc-fRZOfsNXYGUdJ7eirgZz1AeufmCO9sdu6V5OZu5VVeL2COmbf9a0sv_LnQR1gZtfVFuvPxCcspDHUJtclx_LupvqBPPmFtZcG78Vr1_ry_lgF8BEVIzK7nI35MjLlU6k55ygekrvbyegjs,&b64e=1&sign=c458dd0346169459e7bee26a0a1f1cf9&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRtNkb9mq-LhCGp7Rvz_6mFW9zftFBoUTc0CCOmi5dyXltDZaM8q6OW75Md06b6ow1D8zL3zZ8q_FeOMbhua2SpCHv9z4wS4WbEZ3tJ7ooHO5fV6dqS7ibLYKKbMT9FbA5FgnSvdj4_g1Or4mQymP5mdogjDdxvtL-de7AWJZ5pnCYAn-aLywrvCwxHNpAApjqJ2aUExiPV-ZV-Ih3rdCAAOK3KitFr1TC0jR87mRiX4g12XSHchmCkbjMgLvX29B1M1MBHmXmC3Us0dwfHAGxIZ35wMAHyARq6P-L6DNTEfcNkivHHyOR1jvPPxSF_XM-6egcJegAQ0PGdUgA7TP47rUdkboJWqyiV0YkiTttUARs_gxMSgmYcjvAaa6ksC1svonmYz2xY3c5y2ZHWB8mgitHDVnL5YWKrRr2LxEqBNA6PwqDzy6FD-mrLB_-rniEsXfAaMXZdVkLEMJ62yMWdZ2k8x73bVL-EV2wPRSLhV6vwEkRZDn0nYbun-1_I9HRDnHw8-oGZ6qAxuruUaXRTMZ8Civr45TtXUFTnt0T0gNHFxXRFAOV6GaYlXwkIo3YOKCNAwx-hYepwRdZ9hdZfYfOM-YPPuZR_FCJxp1W9kN6WkwaKRgnJUP4UNk6FgmdCKkwBs8er-WbrpOyCjnKKLK4x7V_36WVGC4YlPk0AlpmSRY0VyO9rPOGfoTD9Aj_v4a9-FNNqeebjAkVaV0Gtrpxl1oa543kjRrnJpv0ncEHTdKoMlUihPtdD93OsjSzoVMFNXh4EwNQUDlX5xJG3pJLYmVON8_IidqMrMIan-MrS6eI-O9NtzjFeDi-2VxgWnz138jCj6P_xY291FLWfxA32Vq2KiOcVdiy_PgAGELw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2ZnnrJDXxuOxISNBVkRBaeonJCeaXb5zpE0L54YnUqbcqpVn-Uv34ZWtXqM5X_n5vczwLEtMTxCc0E3tOK3Ey6hpNB9s-TbDFm94hmlosHr0wMl6OWqf46Q,&b64e=1&sign=a677a60f4e230c78f37f4d218662b4bb&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 807,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 32,
                                percent: 4
                            },
                            {
                                value: 2,
                                count: 6,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 8,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 45,
                                percent: 6
                            },
                            {
                                value: 5,
                                count: 717,
                                percent: 89
                            }
                        ]
                    },
                    id: 26104,
                    name: 'Formula TV',
                    domain: 'formulatv.ru',
                    registered: '2009-08-19',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, барклая, дом 8, 121087',
                    opinionUrl: 'https://market.yandex.ru/shop/26104/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                photo: {
                    width: 600,
                    height: 450,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/225051/market_QdFBl4_dcmhEC5VmWpMjzw/orig'
                },
                delivery: {
                    price: {
                        value: '590'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 590 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '590'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 14
                            },
                            brief: 'до&nbsp;2 дней при заказе до 14: 00'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/Q5tHTz6zgYr2yHRNLpigXw?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=E3dxfX23BGYOSRfxr7S_J_4AksIgaPHvhx1SQbotflsmZj1ulezHXx0pwi4I8n4AymV-JG4dl-pK-Tf6ix6uRn-ix4PtFNGaHfCBSeiyvhRa103wSdgh6z8Q_s5r73t2&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=26104',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 600,
                        height: 450,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/225051/market_QdFBl4_dcmhEC5VmWpMjzw/orig'
                    },
                    {
                        width: 600,
                        height: 450,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/212340/market_jIqs3GFTm0CXVnVtUIXicQ/orig'
                    },
                    {
                        width: 600,
                        height: 450,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/217087/market_FxcjUfmHpJm_aZEEV604JQ/orig'
                    },
                    {
                        width: 600,
                        height: 450,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/196254/market_GWP5x-t-_NpJanXWXz613A/orig'
                    },
                    {
                        width: 600,
                        height: 450,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175127/market_wakf2zgtuZi0I2JKZKGrhA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 142,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/225051/market_QdFBl4_dcmhEC5VmWpMjzw/190x250'
                    },
                    {
                        width: 190,
                        height: 142,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/212340/market_jIqs3GFTm0CXVnVtUIXicQ/190x250'
                    },
                    {
                        width: 190,
                        height: 142,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/217087/market_FxcjUfmHpJm_aZEEV604JQ/190x250'
                    },
                    {
                        width: 190,
                        height: 142,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/196254/market_GWP5x-t-_NpJanXWXz613A/190x250'
                    },
                    {
                        width: 190,
                        height: 142,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175127/market_wakf2zgtuZi0I2JKZKGrhA/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZGSqhn4PcDKkq4xgMwp3EeH7-5k6XUxwd0K_yFqXTxxxAlFizCTJQ-deln16titBse72WqPanD91Eb47Kmp5qJJowLDg01uRuJKD78orGzIeZGX4b9lrmEiRBkTwRF8byjyUVSu12WV9prXJkGMM2raYt7Dq-5M_LfpR1kyc1NDHT6JdsI42dASbsjkh5aupmJKXQuGgpdW1jMsZta4j1uziQtOXZvwigei_DH6iZSNdSSlDcZG6x_Y25jYqd3-GtQUHasm2EAWr_HZCySADxC7spqQaNPhd38',
                wareMd5: 'fblPxhEofKpjoUNfB2ORnQ',
                name: 'Телевизор LG 43LJ510V 43" Black, 16: 9, 1920x1080, USB, 2xHDMI, AV, DVB-T2, C, S2',
                description: 'Размер экрана по диагонали:  43" (109.2 см',
                price: {
                    value: '25990'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRjuMjIGudii7U5NXyniEMT2xhss80M8QSTW531xZfTUPRtvFpcpZk_ve2XusigJ4nnIIsx8ht38ZiCrhMZ38kqWzECbMXb48TrwHBziz8unI3GI1wYD-kn5PvbdTqeWHxKciVdlE7MkIdd7q3NxhxNN9qQdnKtYWPyvJzSXteJD00NwffPmiBCRgoTJ2iEXPkWntNaIxyWlaRuoKBf3-DeLU9hRD_eJb-0ZHNdzmEORodFnZQON-tELaYQcH9Msud0YFWA4sjOo4RKprKCf1paltx_UeEvjgq3ydxJctDYUevbnNS1-NcUAoa70yReUnySm41-7isyndc5_LoW6eoxlhLnQWObxaPfFKB8gONiWOI8ESo3x8rOiM61hWYPgpw8_lUPw8R9UPKEMbdNw3QtFd2i03XU065Z5H11d9zVutE2e0vzdizDleu5Xa56uNQa9PiJyG2e2Sy122hoZCmKVs4A4LGsZCYB4c_LN03ls3puPmFdc_fTkvFcb6RlUhm-NhfgLwt5MaQNNh0tqRE4FLaMyGymjfMDFWfHLt5_GeiMp5nnz9LnfmltRilDqMpYyFsOeMGopCo2J4-uXlCSPYrIdek9Gq4s71r2gC3nQaofhTRn8YvetGoZCSHhko0FtDtnL4UsTfHmNCV4SIi3X0TCryNU8EF0vY1Vj68oUZBE_-19kPwu1L5IjbDUh2Qsx2ytQJuBMtxEBFez_5VAm_LGRXJ89LSJeC58-UccOLOkBZ5An7OKVqClOZWEpINgKt2EChqq4Rm8pZuqv6dsEa_pXWC0Abo43xuIRg2NmN2q7PZjgsdN_IqgWY8g5DZhX9HWJXK7w5tjd2lh12hnNAXl1pQtuSaD_RDY6acZw?data=QVyKqSPyGQwNvdoowNEPjTQMKOTl33e6NgsQ5st8WhcPL2CDycAkNmD_nWgEWL1UJagk_YqedF7ideewnt_qZbqLVDgxQwUsOKvpkBU5Y0DoP6fCPbIeCGlRJA9xNleZG_8hVPF2LKZYme3gWSZ2dccAvVInGGTkWD7JGkasIyHhMoUMej7hpVWIiLSyFTdFFm7RivEk6q9CJVRRggpv5BqXImb9Yv41AY0N3xRiZ6bKWKXlknC4UTx_1OvYW5iNCfT1qKQCNy2BSp2iPQLxglRjWw8GGn6FQtZeh3USZdH9tdH4vJEuU6QB4nU1VEeMFw0lUrqc8mwHnsMFrp5457djp3eAxS3OzBEG_FiOnnslr_EP7YUkXNsjdArgBZXDZe9f3kcD6HE9O5gI4x_m8f_4__Sx_qbQXdJZMrR5KPeBHzII31xfY3qMUj-qJKH-gQ2RWYGS6Q8nSttiJoHAZQWwsVuwFNGxgRsy2u_X2brvoLM8cLkuVp9TK0dQ08bac_180V75EfF1ILycu9l7Zc0pkQE87jhoJLrCeXiJ3m3bXDs9dd7r-eRfkWO7_PLk8vAbwdFQtlyAT8mxY7P51Nz2RUrXYMIQpV6S_ccy4nTh8lpOPPvb_hYhUeKrqLbj6ymaZWQJd_8pMD1PIZ2bSWLYYOm4Byyg70OLAoU6vZskFF_0q3OoQn_7kVpKZHU63Vg17_nsQOU,&b64e=1&sign=83d1071fbed9597c8465fe2eb8d1c9d0&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRuJcQHw7yz67IUmZT4vCusoiiP6XGnopDcaYAxRhB17HgKzmOKDaEdx_7z6YcEmGRL6QpHgDW-fviZSKk173rDxQxi37KwlSC8-uNQs00MMFsp_Pnvxai7cM9bboZjEpWe8kWl0m0fwbeELbWLkHn2jcNZWV1JJXcaDKVbGCo3U49we1NTmif9Tp-lt3CgppXacYAwFRjgTuFbi6JGuEZ7c7voGyPfFi3EL6sE9NRxo2G_3CU_jOyOhliRK-uu3neo0jX261PIs7nhCWLYjPfFtOhaYD3krYC7BRCGixqgoNv09lDBx6tQ3EzOv3Di56lc9ABdnlNEIiqVaQ4ogGXbdQktkGL9rj-C2VwpRiaGUqEA-EXM7gCekb84Kqmq80jvc_2ernk-IqtZEBar2q7tZ59k7aOkcuRnUBhbo55cPg1Hpo3FDAfY_aD1fLcXUDO5zhVps6dSTBCkPuZ-hovl8eGBLnu-H1c6_AoALDe9PNR38HKlIvvFtdddSAIk2hWxpbyZ3cXRlmgFwtC0BByhwb2OJfbzCNmlLWidF6YwTq6CXcbXCmO2n7uYVaoNWP3gCGsHwTBEdvwjP6rCqOlpEjzgBx-t5yORhMIP16JUNNSnK4b2DVALBF7YEJ6XsVZjI2mGWJQ3KLYsWoakO9kB4K5GPDsq-faFaOA4-op3XqaHCsPGMi4w2WGJnNlf7u5bkTCaqXpvSH6d_JtXbyRCq2wySELI655y97Q0fJ7y1TEcnRJvg3ZbPnHDQ5MHuLlKBKwuZpyONYREHPXgb1-DuEZ9zpZC1D2DdX2p5qhIX95Qi3uGOTvgDAcBd6JDCJiipWLJ9GHikZ5PvPjijx9HTR64fR_6GBLYRvzTGeeqjRw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2apIXTp0UrexkulNrlhtYrqAittofuGnGIrL4dgSe9zQayzvUvNv2Zx3TkdMkZ1s6l3Q7YbLlfZdQrd6z_gkbTK3y7gn240Nxcxl4WYFKObclYx9HXL66Hg,&b64e=1&sign=e6cbdbe89921cfa774579ac062bc6dbc&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 4,
                        count: 8625,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 893,
                                percent: 10
                            },
                            {
                                value: 2,
                                count: 331,
                                percent: 4
                            },
                            {
                                value: 3,
                                count: 417,
                                percent: 5
                            },
                            {
                                value: 4,
                                count: 1042,
                                percent: 12
                            },
                            {
                                value: 5,
                                count: 5942,
                                percent: 69
                            }
                        ]
                    },
                    id: 12138,
                    name: 'OLDI.RU',
                    domain: 'oldi.ru',
                    registered: '2008-09-02',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Нагорный проезд, дом 3, строение 3, 117105',
                    opinionUrl: 'https://market.yandex.ru/shop/12138/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                phone: {
                    number: '+7 (495) 22-11-111',
                    sanitized: '+74952211111',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRjuMjIGudii7U5NXyniEMT2xhss80M8QSTW531xZfTUPRtvFpcpZk_ve2XusigJ4nnIIsx8ht38ZiCrhMZ38kqWzECbMXb48TrwHBziz8unI3GI1wYD-kn5PvbdTqeWHxKciVdlE7MkIdd7q3NxhxNN9qQdnKtYWPyvJzSXteJD00NwffPmiBCfsTFlChlx4KR0dVG0rqi34uDh6jV1Qe3VksBAfyHuir-Es7FqNDizag6bBWakiJLjGb6l6k6n49riCabqRCWoUsXWUHdbIRywyhQQ9rNV-KGtsaAzEiQPsgbdkGZYa0W06L_iQIPMI5UVdc4wcwsKgMIcDG7nQwJbuWK_oE4V80q4zoE4yEmxDt3mYTrGS7K1p0RZ4xiVooEMrjjt1XMTwf9LmA9f8PG5QidnsT-YDFdLDIP3sIYqYTZIrbanNOwKdWDAYpOArI3-bFYHVe6NdsQ2ZX3LSApKvYHzWzKSW6wVy0mFl66e4-vc-RGJJLaOb-1pWr-Nix-1lqkXXk9p2eauGUmpvy8w0IWLY2dMxe9KWaq295fDIgeKaI6FCLXRma6k_S-4hJdiKYMyE2a8RQgUWWqrULOQ_m1cimbqOexk-NP10X1v7yAXFcPe_r1MuvjAG5baRnog1qiNeX6SOLpNsMLmS4uv9ONlI7Av_9lY0dAKNAhC-jWi6OPYxf5h-a1QvqykXHSO2vN22mFuwzW7drMoMv-AE_U7J8CpDSh18_hY-3tXJ9xif1xQCStSyFYQV3ZdckYTiqSz98uv-Vl8MeGaIOj-z31scTaUbATjv7cXiXDWP49r2lmthODS86lQyep1f6yxSzQb7DP0mjtPzySjhjIJ_ILuNOXd62eHkdDnk8QN?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8RdU6BoAjH9eWbsP3de4dOcORgybCYNFSAExzJa_0hpYScl_cDcv2fFtpBPkBJgkZWwH7-pI0p0AbtnHjayPgBVNH2i7rO8-_85-sXFAIXszeB0NU_1O5I4tgG_gF_7tiRI_ijJq-_L9Gl-i97Xk34lm948M1QRX1E62_0riOcVw,,&b64e=1&sign=9b9f867a0c8e768f13354e4c9a7fb253&keyno=1'
                },
                photo: {
                    width: 900,
                    height: 594,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/163651/market_3ckrTop2mTduBU_DPJ8faw/orig'
                },
                delivery: {
                    price: {
                        value: '290'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 290 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 10 пунктов магазина',
                            outletCount: 10
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '290'
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 14
                            },
                            brief: 'завтра при заказе до 14: 00'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/fblPxhEofKpjoUNfB2ORnQ?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=7nUicpqaxmfIOxOk26X6rudsDLkGCsHh5xZeKa5NCXtis2Ar5bpzRLaK2c6rvs7g_kDkp60gXMqdUYcRAufD0ULHJkbh9OuMk8SMtDCoAZ1rVmMXZid6NlL8sEjRQPle&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=12138',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 900,
                        height: 594,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/163651/market_3ckrTop2mTduBU_DPJ8faw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 125,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/163651/market_3ckrTop2mTduBU_DPJ8faw/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFoiVty60jKZoI1rd-Y0TIz3ToU2U3v_lnRPgRtYUTRmZEzJbYjJJN1pS2xB3Q8ipGAjxvuJce5zr_7wlZ5eBZCd2sB7OOpHaiTK_5Kt6nIM2Ypyc0N7bL4M0Oq42yGXhhBGCLgWV9oDcxqz_bG9OG--2PjloJBDolpU9ieKxtme_seUJkkK5dRShkzKWdziN-gOTeVD5UCVyJ8DPPIdZfZbzv2Pi4Jc6K-IE4-YMwy4XERte4lrEwyzBzvAoWf6eJoQKBtgd9oJWNTeHx9mE5O1ZtGtFdC594',
                wareMd5: 'MYUpvD7Sy8SqqycQQjPmTw',
                name: 'Телевизор LG 43LJ510V',
                description: 'ЖК, 16: 9, 43", FHD 1920*1080, TFT Direct LED, цвет:  черный, дополнительно:  DVB-C/T2, cлот для CI, вес:  8.1 кг (43LJ510V',
                price: {
                    value: '22865'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcwSZdwshUgaabG4l1Q4jBd008Qi9P48JFAMfF27jDtTxsHsXQoYvu1B61VpvG6iGx2Au8rwhCKAee6hO9FzmKxMQ_Lv2yP5Qx-Cu7onX_MN7lok4Sr8bb7Q0ujznzrkVJ7c7vRys7iCAyz3yRmzO2D4SgVQ7XmFO-6p_PoB4NzYuoLSR8DWPdyWTiCjqp6iylkh1_iYh2VYZfkFsJV-QCcSi_TBBsEF8VXXaEVXzoFB0PxMzeiq-kKevpAJFvL388LTEvmQrogs-7OKgKJMmM9BevMRoX0KEfvVI_ZRAlukYL9Zp6JYdk45FGzqpZaN0lD7Kvrr_lYtmwV4OMHqF2XUQ1B_J1QJMRsJXS1l2CYz4cU-B7IyYmlyS5tqSk47umrhHk8XplfGlIq6RDxcrtfIMljahfjHYNK_8q-beoG0l7izf0XYuy1OUkoIBPLKJvDus6yHeSMQUczZbPAV9E5BUYWhAi8mhRoF-XReTs3QmV2baUOOP0D0eu8YzH5f-CdXvOaeJAgwwOTeqhkfv6NRhfo9JNsFlDzbUjhQD3t-GRGD8trTd70dliMpilchgVTW-24lpkkbgtAsI8OcjnSHCqSeq41bUpjErwuqcypKDWrCI6jNQ0k-5M9Nfux4rCPvH5BMj_r3MKdDcqppK0MsxE-QwOzzEfUSQydUM9okNXkN_2XUSaEelXG78UYQW04NWiqNGlk0GcGTG6bYp8z4ZFFbGlU1JXBkt2oNtocW_ObJ9ajpTjpJMl2TUaV0KiDMO-NITG2vTi8hApRpKVG18tKb8I4uWf9SwCuZ8fQ8vHincI-F5Ur55u6jD_EH6NB-s4aOCL-mPBaHkkQNuF-VZ-gS-f56AA,,?data=QVyKqSPyGQwNvdoowNEPjQGFAe42Qvx8uazO5ryr1yH071s0IFz-d2z_WOvgLZPAhQSk_OVLMMOpxe-gEZgTUwDUfWR78Rw7Zo_X8sJUa4ttoTLUb0ok4UyX9QxQE0743hfduRd9kxWuuKRQcm5abK_qL2-LATsgNrJIvDgg6r_f_LGIj6n-K_Wohu1l8S80ZvkzN7L0K_ZA5SMytrUCW1FYWqOmodpTq8D06Wai_Js8g9bncnEwLzXDBfY-DfB4kiJ3NRJVCW77NOn9w20nvlhAE5cPet-5JC9lnFfbowwaIrh7SJQx3MmZ0QX4WvO4gqWSvzGluEP8fvNdelfUxBG5tSZcQB474zSV8ayx1TEEcfMdki_l_50XLg8QMhewzPOM5da5UX-k4AC_jtIsStypuK-KPMuqdhhtGXLeppckPCsdpquBzelLJoqK8y1kPAoDsIvz_mLP8DwlBpNjxCIFm4ixMTnmWqxKqnqI7d5-n7gEd8MDdNB2n_aXKNFr&b64e=1&sign=bf935171975829072bc374c8be78cedc&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRuJcQHw7yz67KuxPacy_Nnn8L6CsG1RunKpKnVAyQnbsDOreUkX0z6HvD5LWZD6Lig8UfBJpIXnRhYS67Jd951ub7M7oIvPiMnC1Lf4OG8hpcdI65ALlKOhWInFxpsbOM3sn-isfUqlvh0wrzwB4y7DPW1snDqySXeW0AgsZ03SKJ67ipBat9p6kCTTUqbE7w4PC40AraUWoJz-deiD_srw7bhLzM1PhzFhTJDuH-pRvOxvFRajbLF_PozlWKGADb3gSnV6qXfDYeCvqQKbCHLwIftWElA485Xo11i7HAH5GtBuw62Q1XiVrvlbr7WnbBB24sB79zQIf0x0BIiUs1qneLK3Fzc1rhU48fxXLNnW-9RjQ-DQrDx_lhWNWa_vrlf0ybRU_p4-ABBEDuXzB3iA8iIkTnu3nhQi4qoOjaai8oSu5tnyqZXSikodIpxwYM-d4Er37bZDsWzoY0bxp8dfHCqbh9M8qVR-6cX_KvEJvSkfAx1omjrcrAdk-NdFkGdvGdsFZHjrKyv2EZiRdwv01z_PLvIbt12i9zKJwDVPh_YIpoKzhPUUh6emqb6qU9eYOsv3AU4gNDODb6rrissBKIAYpiFtwn3BkATLt_RVQZ4Z6ayjOcf5meLkugiROMExmlGNkWyznt641DOneuClVd1_s040VLSnzcDu7kYdf70nvHssOl9Oo9qwhcVarcghho2tLOrZMbo-uKJp--OsS_3VY7CfxIjL3e7zy7Gwb3zFYGpv0n-ZwQs04ou_PPKusyHyM5CpohP8lx7vr8ngRYE-tQ7SOlhlXKhQJ0Ts6fgtz3sm7f6BvsnodbzDrnQZ7pLxrJWgZb0d0E49i8F_JkXm8U-ZwNbjRovWcyhX9A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2QU3MdbNUYfYKHB3wIGvL8hEAPGIf7rKLqZ7qM7g35f_GD6S3h9w8I783eiY5h1FdAwgR2eyScMVjuzwHkl9barI-85DMxt630FkKOHMyw0oK3hlHgVyIoA,&b64e=1&sign=f07f3532f5bdd672afe31d0687313512&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 4874,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 96,
                                percent: 2
                            },
                            {
                                value: 2,
                                count: 44,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 32,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 633,
                                percent: 13
                            },
                            {
                                value: 5,
                                count: 4069,
                                percent: 83
                            }
                        ]
                    },
                    id: 493,
                    name: 'KNS.ru',
                    domain: 'www.kns.ru',
                    registered: '2003-05-14',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Шарикоподшипниковская ул., дом 38, строение 1, 1-ый этаж Московской торгово-промышленной палаты, 3 мин пешком от м. Дубровка и МЦК Дубровка, 115088',
                    opinionUrl: 'https://market.yandex.ru/shop/493/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                phone: {
                    number: '+7 (495) 6262020',
                    sanitized: '+74956262020',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcwSZdwshUgaabG4l1Q4jBd008Qi9P48JFAMfF27jDtTxsHsXQoYvu1B61VpvG6iGx2Au8rwhCKAee6hO9FzmKxMQ_Lv2yP5Qx-Cu7onX_MN7lok4Sr8bb7Q0ujznzrkVJ7c7vRys7iCAyz3yRmzO2D4SgVQ7XmFO-6p_PoB4NzYuoLSR8DWPdzG1qR4au3CobiLSDvFPcPFoDVnfJ4Uevdfxzavrr1cr2m6SRX5CtyxNot5MhuLcIyezL3f6K1Tv1iC1uo-AdmrTZJvmYKiMMwXPdjK7AD7t4T7OctIYOYgLEXIBXFkAbsslgjj7sYoWXnhxRZQ3aCf3CCb4Q0qZYbJPgnUJ97A7NqConfFxIfsZL43ETYo75bpemW7yl05zwJQaU8hxevh60vdBfgGomeshJwHW9nuMRWGdLvPtIePtCU37hwMgSinjq0u6t5ezhOIq8_DOZmhQPVlXN-xGZEDQSrAfnb3KOEKxU18UxksKr26HvzSt2U_ufR_i2FzOg_hcLe2WtjReX2wE9AUEH18n-FhRMqj_rc26jdy24KW_oWAym3Y2D081NJ0_mmcjK2cknOMzNPxrIljvcVqgiZJmOXbnmiYg-qgv5HvDWrL3JH9bzzaigf8n0AM4fv-5lGrh9dxMcdxHh9rM64baAC5hkfoAQM2tq7RelGQWS-zmHPMjCicBTDvWyE7t16TjHHRozowxnbe4dE_xNN1RDXs1-pD7pxdrkHEP36bS8iDx6YYX2MLKxEUEZPMKBRX5yg6plzUD4m1ci9CE18v4KKUpneEGdBu4_czpi3itBNyxu8NbtfQ3HBL6mlpoqwSE6L-LgV-eKiPHVN28wCLUIhksYz3obFsjw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-Rdj7Uc2p_w0jz8BEjPQBlN-mP1EYw43POFWA5yP37y8YaWW8gdOEzIBcK3bkpwwPFGbXSOllCIFRJetau1ymn4pU5cGWcp9OCVG41unlzA4WdcaXfFr4ODKj4H4HCl4QoVjLFtWrxq3jJHUAKzBBVCkgltfiU8BmjPmUPON81ng,,&b64e=1&sign=45ebdb851781d9c387464f636a8cd0d9&keyno=1'
                },
                photo: {
                    width: 200,
                    height: 200,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/246786/market_nIfvFkxmxnLpTFe5TD8flw/orig'
                },
                delivery: {
                    price: {
                        value: '400'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 400 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 3,
                                orderBefore: 17
                            },
                            brief: '1-3 дня при заказе до 17: 00 • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '400'
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 17
                            },
                            brief: 'завтра при заказе до 17: 00'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/MYUpvD7Sy8SqqycQQjPmTw?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=L9AgswCOW5c0BsihkvpPf2E7ZZvULSV-KHg7Fbk7g64X3OK2GcQgziaeYhSmiZENzPJSccHOuTSog-gd6pXOn_6aHkRxWsubNiQESrH0mz-lbNT7bWxRugmpUmfcz-G3&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=493',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 200,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/246786/market_nIfvFkxmxnLpTFe5TD8flw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/246786/market_nIfvFkxmxnLpTFe5TD8flw/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZGJOU7jblyT9lxYN6LQNdDBtQxQdK156iAEEUH4Uaw7nYBPu0hTPgC913ye_w418MqKn7ChzFYXbMNQpPfJY73-0T7n7bXUXBaPucd3aYmVs3NN1-77KPLTSuICR0FjzmQc3pGcRzpZVhqm3SD8KrvV0yQS2fJ-8LFkgjpfRc_mXD9r5m54YBuR-aKDGGlo2asc7FXBgFbet3B5rsy-1O6LKKU_4dDyrpzmHfR20FDmKkzyrEbEI6SEZ6UGg8La3M_xDlkGDNGfZxnzrcVG247meREt2po1QTY',
                wareMd5: 'nZeJG1dp8PZ5XpxOjmsCOg',
                name: 'Телевизор LG 43LJ510V',
                description: 'По-новому глубокие и насыщенные цвета Помимо улучшения цветопередачи, уникальные технологии обработки изображения отвечают за регулировку тона, насыщенности и яркости. Революционное качество изображения и цвета Разрешение Full HD 1080p отвечает стандартам высокой четкости, отображая на экране 1080 (прогрессивных) линий разрешения, для более четкого и детального изображения. Улучшить изображение? Запросто!',
                price: {
                    value: '21790'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcwSZdwshUgaabG4l1Q4jBd008Qi9P48JFAMfF27jDtTxsHsXQoYvu1B61VpvG6iGx2Au8rwhCKAee6hO9FzmKxMQ_Lv2yP5Qx-Cu7onX_MN7lok4Sr8bb7Q0ujznzrkVJ7c7vRys7iCAyz3yRmzO2D4SgVQ7XmFO-6p_PoB4NzYuoLSR8DWPdyy4vlQFu_uB3ZPjV1UzHrJODtO6RefvXN_KQGmOTNPTG4I_fnHfAIhGxcZ7KFjCpvSLIp_ZGg2qjFyEdxt1QrvQLo-IGAML0U4XtTXpD3QrXRFaUr6d3U1zSt0voKZ0xiS_qp9VZ4UAL2eKev76f8NYglapR9CGwT77fA7jCbdSdUUWjSpszE5nW0af9dJmdeSVUjhCfB4oVGXGsQyEeIGgL0zVQPSFQ6K8cPXeBWwuyJkVf3MrSZ6RycV6mKuRGGq1Ra6w8P6gWoBgZCFUhBo17XlpbRnhI2-0O4a7VV1CEtj8j8d-3IjBRTz2nOwBfXL8KGxJolyLq3MLt2PfMY8ojZa0M14fXxuto3HRURnpc1vI0CN4lorjkuSprbQXhX2tQCC6YMWTjxDbtvWIcG0KtE0FiY2ZAi8LFdoIGlg6vh0xVFOWOnfh_0CsnAAeLordsWRCjEw_safe1K-7--CJqdvkDOdBwfME27jSWFezkVDKY8gJjk0UxMC86jqXdCpgqNz4Y9AH-s1Dd9iSXLd9XZ7WiFjCUH-arsk8qgLQDCE3GTKHpCUHLzYmZNVsiJEws1qEagsgWyE1pegGD1bkxHKHEp6xFitxapPOEdirJo2EU0NMv2zSsvrn5M-0udEJwM5vs-5TSCWKOmOvOuD4nIQZQVoPVOSKMn6jE_DGkLp0qqDr92z?data=QVyKqSPyGQwwaFPWqjjgNg9J8lIJ2Oz9qZC6_dCVBEWl_uzPJOehn351-Xu9JcwaCACqrDNSNOw286xOZEwUR6x34wK_WW3VpawpWECK2YldMNRx_91KDZyekFeg-fhCs7Vwu8VQ1eFSiwO37sAnwq4NNAoB5yie&b64e=1&sign=94f682eae8a694b43766f80415adfb25&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 2354,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 89,
                                percent: 4
                            },
                            {
                                value: 2,
                                count: 18,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 18,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 112,
                                percent: 5
                            },
                            {
                                value: 5,
                                count: 2117,
                                percent: 90
                            }
                        ]
                    },
                    id: 18615,
                    name: 'MobilMarket',
                    domain: 'mobilmarket.ru',
                    registered: '2008-12-19',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/18615/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                photo: {
                    width: 133,
                    height: 133,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/206654/market_8b3CnBYw59mfpmioItTO1Q/orig'
                },
                delivery: {
                    price: {
                        value: '750'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 750 руб.',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '750'
                                },
                                daysFrom: 0,
                                daysTo: 2
                            },
                            brief: 'до&nbsp;2 дней'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/nZeJG1dp8PZ5XpxOjmsCOg?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=L9AgswCOW5cs6Ae5QMJlc-jYV9E0TJ0H9-sVBeOeKGpf0bOjSC-Ufn6sFggxvezZxTDKmc0jFBzqpaDT4X_qscwjL0B-P9HKqSm56zrNoSCJ8UjC1DVfHtbpJgPnn62q&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=18615',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 133,
                        height: 133,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/206654/market_8b3CnBYw59mfpmioItTO1Q/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 120,
                        height: 120,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/206654/market_8b3CnBYw59mfpmioItTO1Q/90x120'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEiHMHHN1Hxs6vzSN8KK8G5QSzQXUgPIcr8wmUR3qIKsg',
                wareMd5: 't4SacB-BLPDzs5kN4KMv0g',
                name: 'LED телевизоры Lg 43LJ510V',
                description: '',
                price: {
                    value: '21900'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcwSZdwshUgaabG4l1Q4jBd008Qi9P48JFAMfF27jDtTxsHsXQoYvu1B61VpvG6iGx2Au8rwhCKAee6hO9FzmKxMQ_Lv2yP5Qx-Cu7onX_MN7lok4Sr8bb7Q0ujznzrkVJ7c7vRys7iCAyz3yRmzO2D4SgVQ7XmFO-6p_PoB4NzYuoLSR8DWPdyr1E8djlAg3dwpX4ALHgFqJP022RkzC380yjmGTtpUnoR4NV86RPV-lmQBbO9LN2k2EjDohYs5p4nMEnPGvPAD7jdQ-upVh1vDLtVg7H-2gbOfoIPwe2MKHvQ7EI7d_7OAEgGDvzZXynd51sFBL7Q5jDfSosqiUDlxo0JZLzr-Fu2tiz2W_DDeplyW0ilghif3UGtuHSD7RYhQaZ59UsLq9FXGQuZ9h7CIvsooK-EdUxUlgqbR_ZPV2hJBM6HBgiCH2YmmELTGJlOJjw3pqANTd0cfGpD7rYaqnu8gVPv7Sd8PkDm9zQHqTE0xO36cO8iCVAj-6rTKnIHunOlKAprdxHY-RFE-eTOMZ79f91ZYcUxHmVoAckEiFlkArNg4mhWSI_AWYVpY5k4DTn5igEXNGGtUun5eW0nGoteisGOllrUo2Zllk24vHqi201ljf6mKYX0BzEzjlQl-oGd6t4fEH2Kk2mig6D39a7aez1LIf8Tm6MzfQDfCOoPOx8fsy6igqFU_ynmBeWj56ZLB5TIK-hW6ZKHANlDswB_4JYAF6QTeMqSbtUu8TkBDms-zOUd2Cg1tYaagKbE0aajIMxUfibDOgF6hbarz_SJj3IxPxQmXcnAXu0UFYDMDzvLaOCyS7sG8zXeCeGuvE0eROc36QA_5LEku6F-KVe3J1BGfbNwTiV8DRHKH?data=QVyKqSPyGQwNvdoowNEPjXaBZquvqIMhBoyxUPKfRyHNFqKmVR9KlqlQ2EiS9GS_vbEBo5hlgK92yPfo40wyoVLq40D87Mfnl6VYwpBIBo7swKuiq5tYIi3hIDv4tu_SO2jMd5nusqGVH7YXr7MRBgdwQbtMBpZfWxNTBpVs-_Xc9edrpxikU45iJ030PFdF&b64e=1&sign=f2e8ac84489484c543d01bf68cfd42ab&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRuJcQHw7yz67KuxPacy_Nnn8L6CsG1RunKpKnVAyQnbsDOreUkX0z6HvD5LWZD6Lig8UfBJpIXnRhYS67Jd951ub7M7oIvPiMnC1Lf4OG8hpcdI65ALlKOhWInFxpsbOM3sn-isfUqlvh0wrzwB4y7DPW1snDqySXeW0AgsZ03SKJ67ipBat9p6kCTTUqbE7w6ttuy3ALWdzQZlFMkjYgDhBnymMN8O0ftJm-sSScHKDz-MWCQ3ZZaT7pGfeXUsBmdjfuiISwVB2JZ2bauml4uopSjwQKZshX_KOmSZNuoLYxDKkidwiq5x7p_oMCVsnPFJNHNng35c1OwHvzZBtm-lyMZsvJ5f0D7VArB8ReRcEb9cyavN0uFxX-xKyUtCklLUIRG69BShSRdWtrQev8f67nkGpfGqtDir1iRSPULAqFSk7Z8rx2ZYJejzZGI_hd2oftcYhz1GK2nKIFyUV14J5Wiq9F-m2E508ywO8Q3Vkwb3YJzknaNmBPQuRSbqBlGBia7QqoquwruXM-zh3xEgorSrbQ8uZec8jnp4ISiqTwjcusFdOQ6-_dwj1BsyBxCBqdV0b_K1pWLWZywataI4w6wJSa2tSQktgVwkMHQ3WW3FKLGLch0NKA7cgEy7NQN7xImQjmkTyvx3qFXTXD2YUAsAUMLh1MhSwlzhMnPdZhOjGz8o1Xx5pzs7J41amfVE9piw0nv9TrjzQr_2m7uiRUxsK-LWgxl1a7d5V9Y3RrPO306qW10ACcqvrwIY7ZHmxAhgVZJ1kD1yHQ6cWCPgVdNGMMQQOa4tisvroCxlI9JF2a1MdhtUWB_29cFoZDvMhG_FPO05uuO_QBQ0MConRHqkkA9L7cqU7n-Sc5V6aJ2J2_W1RUsW?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cBqr6EXhHUrpjRqpAof9ySIwHWqmm6GW7A5MVS-SYLr9D4XsoOtdnmbLs2e6W90n9TfROyFU-h2fYUnkHRMpyvT6mV0xcs0c64bVpSCxF0v0OD1nUmmyAU,&b64e=1&sign=ca0d707bc9ff56ef35e187f7bfe95ecc&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 649,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 15,
                                percent: 2
                            },
                            {
                                value: 2,
                                count: 1,
                                percent: 0
                            },
                            {
                                value: 3,
                                count: 3,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 21,
                                percent: 3
                            },
                            {
                                value: 5,
                                count: 609,
                                percent: 94
                            }
                        ]
                    },
                    id: 59449,
                    name: 'ShopTech.ru',
                    domain: 'shoptech.ru',
                    registered: '2011-03-24',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Барклая, дом 8, 3 этаж пав.380, 121087',
                    opinionUrl: 'https://market.yandex.ru/shop/59449/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                phone: {
                    number: '+7(916) 861-22-99',
                    sanitized: '+79168612299',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcwSZdwshUgaabG4l1Q4jBd008Qi9P48JFAMfF27jDtTxsHsXQoYvu1B61VpvG6iGx2Au8rwhCKAee6hO9FzmKxMQ_Lv2yP5Qx-Cu7onX_MN7lok4Sr8bb7Q0ujznzrkVJ7c7vRys7iCAyz3yRmzO2D4SgVQ7XmFO-6p_PoB4NzYuoLSR8DWPdwwSuPuz7uy4x9fUcdcoC3dbeIs9wMjXwp0THmjPNbkaNry2T-SMDMbzyvvLmSoBhegYlmn25iQDcEkZ0hnWoihDU6h2cPCXjsHorbV3R2BTK1kwm06uJbVlj0KSg4QtNKwu5qnsL7AXX3Ff_xSbGcHbfsvbbWP9O8FIeRYIpsz2eERT-5l9VZ45TqnuuO1S3dQ-jR8tRRBU2JQRn3bR7paL-P6nyY8KwWLzbdF-HgegNa7BAD5rt7Uv60WxZZXslS56PMSWm67S4N0ZwCagAObwRoBP77TLfiUM-0hoOXf92LC0bWb6jmiMRRS6D7CXqhk4wiIA7tIFoLkH8kFxBGhjYyF21aoAemrgu501zjhYy_-GqHsUAwpUPNrY7YKElTFyOhdegH9jVuJwrOFS52zbLM7h950aq3YdOAfzMN4MX8r0EFt0PUs03udG2_4MA7ljTs30Bm2-TNcfoEE5sHdxWTyMjvFbkGDFF0TbBr1DN4QUU3keA5Oxrs3jQ2jgahNheIoe_HAcaLGxq-UVg10_q-Fe7FB_NohnXn7OxBu4dR3hhIREbH0BXf8qD135FofW8EpBGqQFyLvVwKPuEkqnaWA5cxg20roO5fX6iUhSuDcmyYaI78RPlJQs_eLNRjNCc6L2bH-I9C0PzcOcAiJXPMjSqZ6ZnWhJCtR-zNVRjKWN0ghsRZP?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8BXJmQbhErj2b24FQeJDYVZwdoPnHZZl63PQyA_FNMdS_r6ZJMXMt4KZJ6Bx7MVxSMWTGSHrKtX1NlEQSjeZqrmGXz5fj2BXBKUYxfnUGzslSEi2ZChjpubkTQ6yPOndx2kh_DiwvPuOrR24_-YNW5QXCVvF0fT3djzNuEqPdmkg,,&b64e=1&sign=e6f7594c125f032e64c88786425b7372&keyno=1'
                },
                photo: {
                    width: 600,
                    height: 450,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/374599/market___3axAW4z6JlGAbERjqkxg/orig'
                },
                delivery: {
                    price: {
                        value: '700'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 700 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '700'
                                },
                                daysFrom: 0,
                                daysTo: 2
                            },
                            brief: 'до&nbsp;2 дней'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/t4SacB-BLPDzs5kN4KMv0g?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=L9AgswCOW5exOWUfzOYuJsbIAl5f6W_F8TA2W_42LwrGAzy_YNXd-f-rzcn-rvuGZvvSZxlnjlOnjbfs3x2mCsWNgvopVfxOE7jO0TTU7DWRe2EqHmKH_O49HjAkDbuN&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=59449',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 600,
                        height: 450,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/374599/market___3axAW4z6JlGAbERjqkxg/orig'
                    },
                    {
                        width: 400,
                        height: 300,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/167132/market_lqOg63I2Qzm_BKN97qo46w/orig'
                    },
                    {
                        width: 400,
                        height: 300,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/231668/market_YxDvRYJtKbgAgu_mSkxBjA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 142,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/374599/market___3axAW4z6JlGAbERjqkxg/190x250'
                    },
                    {
                        width: 190,
                        height: 142,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/167132/market_lqOg63I2Qzm_BKN97qo46w/190x250'
                    },
                    {
                        width: 190,
                        height: 142,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/231668/market_YxDvRYJtKbgAgu_mSkxBjA/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFz89cRD1wJ2pGYWEL3FIzlfxsVuSRminazSupdH2VoEw',
                wareMd5: 'kvq9ejmw_RqbgGav55XD7w',
                name: 'LCD(ЖК) телевизор LG 43LJ510V',
                description: '43" (109 см), FHD LED ,PMI 300 (Refresh Rate 50Hz) ,50Hz ,2.0ch (10W) ,2 pole ,Black',
                price: {
                    value: '22557'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcwSZdwshUgaabG4l1Q4jBd008Qi9P48JFAMfF27jDtTxsHsXQoYvu1B61VpvG6iGx2Au8rwhCKAee6hO9FzmKxMQ_Lv2yP5Qx-Cu7onX_MN7lok4Sr8bb7Q0ujznzrkVJ7c7vRys7iCAyz3yRmzO2D4SgVQ7XmFO-6p_PoB4NzYuoLSR8DWPdwBt_LpoaeqEo7wYTIsoJfAgoVELEE0K7i0ZqUdnuC_kr5G6SYcT6iNotImTRRYgWVrHK_ZyOChEAVnCbLgEswAREWsNZtvV0GufYBde-r9s7ADm7O6rd0xOABDZ3GFDFC9Na466jbTJh-kq4RzbnyOunyxZlVI-YYSeVXBY5EP0ib_AEJ7Bsoqcu5oXPv_xDM8QWpX8P26cpqeSXKro71ivrmCXBjT6Acn6k4kwwDTtGhO8s0Fy_D0Cts74UimkLWZTXAZWDKW30zfXz3CWefsmtWIxJzMLx9hBpkIYlBJsJyZGjAJqVXOLM1zfUC0nD4xe5viy6PUNert5Bh4hWaHN2-iO0kkMQH3o5JlDwAm9ZHRk1X_oM0fMV_qL7VXK9ZyhndgVqkBJzWOy25fYz7IQs3bK1t0f3DMNXYGbujwDBJp3S27ftVitn2E0D5CmhQpWvdAGeNiASHctEF8jfPF9l4ZbiBUZiNQepyJBRyufhahtfNP5NoUEJMXjVM9_DSqjySpIBZ3T3NOu5092UQPZFRoI9SpsAk5trqDQEPmjg-1te_dMbbCbMbZev9yL9UCZfXF4rn4POU_6ThRsik7k_dSBrWz3KDGhgF8Ld0Gy6pPR1aV_NViacg48h6Qhrc1HMEJ9UYtuCIX7GM2hfDHrydoavp_oBWwV3ePWt8k66SPVG0_VYN6?data=QVyKqSPyGQwwaFPWqjjgNvqZIqElaZiAYh7f0Kl-e7LqtCBPCunI2Xl00yyKnEhko67u-CfMsreu0YsBe2nc0TM1eKkPEPTd_oq6JutTNXEXu3XirsxNIBhVkjB8H6tJBPeEoTZAe8A8viBxCOCcJYZH5x3dXXOB&b64e=1&sign=351c44706a83e9bdfb8cc24a7a91ebc1&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRuJcQHw7yz67KuxPacy_Nnn8L6CsG1RunKpKnVAyQnbsDOreUkX0z6HvD5LWZD6Lig8UfBJpIXnRhYS67Jd951ub7M7oIvPiMnC1Lf4OG8hpcdI65ALlKOhWInFxpsbOM3sn-isfUqlvh0wrzwB4y7DPW1snDqySXeW0AgsZ03SKJ67ipBat9p6kCTTUqbE7w5bp-1zQynlDz2E2q5LzjbPvq_bKByLeqPsllQHBocvjN5f-p7QXqkExo7Dh0V79eSGx0_kOu6MOZg7K16cWBmXl3L_-OOjrEkRbDp5hHI_h6uNIdjxGJ1zllg8e_1A98noc5Aw8ehv6FxE3gbh0_Za2xK7g0eY1r-7bazwHLWQFxj3Hiwme0ImJsdalfpaWYdC7Wn2ZtLVZP_4aigwBfutCpUtaHXVJ3YajDOQtdHVn9lI2EY9PCBbD148Xk3QFPGm_n_3QtC8vh9O_rLA74LXLeHarzRIm5-Zm2sxOmfNH_YZ4CMbvWoeOMcaVd8p2V3_WaIIU6HtOVevBc7EPxPiTjuQza4EFf6jzZeSHA54BBw7wRepRPRaoF53GHeUr0Lky1bRfSFuEuyGUveYdPelfXfDVBUvmowi7Jd14koyKwkwnaEYdkb8XCB6-iPsmMOuACchMdXNBtyOARY9WvJQ2WXXhp33pk6CQ_Uxgv59tE5GJWVto6qtyeQyc7Z2FWgMocbluUkqX5JUFt-Y4OHmJs2yXtlRCxesRAnkxde4bq_oc13aoxE0Yt5afzxabDKsd7RxJx1OnRi8Ddn7mf8pn1-mu4N8If57cFmUXs2jl5cFSVP09lS5WeG11Q23d-h6RBSd-YhwgOkedf9jh-nTi-gFAXrumOXAqC2DvlXjHJEgfkJpqUf9?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2S6w90DXuw3hs23ZSi0wGKFdCuYlMaSfGEKFpo_rL-cbwiOH7q5K8dGQcRxgBIg-snBBDwHW-mpMWaitp5mQduKKqLpuQarqAlrVcFKellGtRY21Qh67tRo,&b64e=1&sign=7d3cbcb1fa5cc81e092ccf71edad30ed&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 23411,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 2131,
                                percent: 9
                            },
                            {
                                value: 2,
                                count: 336,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 297,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 1991,
                                percent: 9
                            },
                            {
                                value: 5,
                                count: 18659,
                                percent: 80
                            }
                        ]
                    },
                    id: 71538,
                    name: 'EL50',
                    domain: 'klin.elmall50.ru',
                    registered: '2011-08-07',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Ивана Бабушкина, дом 17, корпус 2, часы работы с 15-18 по будням, вход с левого торца здания, 117292',
                    opinionUrl: 'https://market.yandex.ru/shop/71538/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                phone: {
                    number: '+7 495 150-20-19',
                    sanitized: '+74951502019',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcwSZdwshUgaabG4l1Q4jBd008Qi9P48JFAMfF27jDtTxsHsXQoYvu1B61VpvG6iGx2Au8rwhCKAee6hO9FzmKxMQ_Lv2yP5Qx-Cu7onX_MN7lok4Sr8bb7Q0ujznzrkVJ7c7vRys7iCAyz3yRmzO2D4SgVQ7XmFO-6p_PoB4NzYuoLSR8DWPdwXRd8qFRj6epNjmbPSb9r-FCpnjy5iR1OMHdSC08X3_cufkVrL0Kh8EYPLFOvUJuxwsNBkM091kFnLg3TOAXziLIEEYvvVDYTExZH-wOtJGKRKj118rWVedfsi7UhFw4cD6EQ1VCHHNF985EBJxkFgwhqc4EeWNpHp0WBk_G-ZsB4gdx5WJczNHUSilMjhsNPy4Bf3J6OfwE-mjCSQqtmGtvvT34UiuX2CWwjJe9U_4JqGrDZuydbvGzXijdv41qFOumsfuEfPxO-GlezGPidEVo3ul-R0ym3L4gCk_rbgoOpwzh9d8XDCmZnapHS9TQ9HSKoo23d5IkyYSwKrkq9ajC7qifeFjcy-oNiRCxBcpj7zbZNUwTHiTCQ-fNDo9_HuIjiMQk-EvmxkZpC2msfD8615-xZTAQ8iK6oqlzEnozWHJlVE-8qo0hSENeWeohAFW2aa4HImbXNKPjjwdxJ4eNrzyhHl7qZnt9uYclTvBtxhDnQPGGkznxkPzKD7cWEnv7HYoC-sgMvfNL6qzTxU0t6ZmfMLZCfY9kWX7xdjBQqpkGihlpf9_zbjm-EbYGjv92SJjxiQTtvA2W9g_a3J5Q0VmF8LHnr-ZRq1ViqnLputtkyNUSZUamNw4ydqlSt0uUwRwT4Y5J62lN5hZWJgNbbNcy2EKfUXjoUYNJUo-1raW1ScuZ5H?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8jtmlEOFbcpkBf-os3M8eLvrV7HnM733PAdSTt2r5PVlseHDMe-Ycdk38qrQGm4i-oN1qECcV56lepuQFDZBqlUcRVjr5CH7rPv9nPhCQxMuEk9RoPpj8YpFw-v0DMKfCk0ifr4kGEpcH64woFcre5SvTp29IsuJtph3Z4G1j7Qg,,&b64e=1&sign=3710071a35c5abb5b03159d66b89c9a6&keyno=1'
                },
                photo: {
                    width: 500,
                    height: 320,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/236212/market_GTqKdSJFYOAeE6n08SyrsA/orig'
                },
                delivery: {
                    free: true,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — бесплатно, возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                }
                            },
                            brief: 'Срок уточняйте при заказе • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 13
                            },
                            brief: 'завтра при заказе до 13: 00'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/kvq9ejmw_RqbgGav55XD7w?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=L9AgswCOW5cQp3Bogo4wpYE2-n43usUo7_hANjwr9KnKqWuelDIV6N9IyGSor7H7PMOjQ0cuporLgRyIJLVPN9A7aCgB46_O7jW3TCIu3tsX9E0bcb3AD-KnIIvAmOaW&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=71538',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 500,
                        height: 320,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/236212/market_GTqKdSJFYOAeE6n08SyrsA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 121,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/236212/market_GTqKdSJFYOAeE6n08SyrsA/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZGY2Eda_oaghPOA9YRBv0Wmbjd_lsReqegHGKhuHLjB0A',
                wareMd5: 'yeVN6MyfAuSp5p4E_sWS1A',
                name: 'Телевизор LG 43LJ510V Black',
                description: 'Помимо улучшения цветопередачи, уникальные технологии обработки изображения отвечают за регулировку тона, насыщенности и яркости. Разрешение Full HD 1080p отвечает стандартам высокой четкости, отображая на экране 1080 (прогрессивных) линий разрешения, для более четкого и детального изображения. При использовании механизма масштабирования разрешения Resolution Upscaler изображения любого качества выглядят существенно лучше. Функция Virtual Surround создает реалистичный, объемный звук.',
                price: {
                    value: '20990'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcwSZdwshUgaabG4l1Q4jBd008Qi9P48JFAMfF27jDtTxsHsXQoYvu1B61VpvG6iGx2Au8rwhCKAee6hO9FzmKxMQ_Lv2yP5Qx-Cu7onX_MN7lok4Sr8bb7Q0ujznzrkVJ7c7vRys7iCAyz3yRmzO2D4SgVQ7XmFO-6p_PoB4NzYuoLSR8DWPdz1mNuGGvpUcpwoM9oZeLHCVyuV22pZWXUu41d40_4nKO92f5o2d6cWRGtKISj9djHfoM5QHX9mUfUtjdly92od7wHJFz1jW093T8l1gF-V6VEzLhRU3ZxVnaPIGVyuqvJLTK6FaCMXR-1_7sTjFXm7x-ScS3qC_nNLlCrTaCl6EV4SYrk9b0ofkpUhni5OSoln7Xd93Y4OpRvM9gMyEBv6aa0gawkPvQhu-QvLxr8P7wU_Jxf57zFgMt3Rzh_vXVudvy1bb6fSqmc9L7LySULos5uuZhFC6xKIZnMVIzKTJuhm-cEgLl52wx_VvnP65qZHypOTKTg0EFEIjlZM6MO05BTwWHrEdjytq8S5u_hfF2U5OnMeDKHhxAyUxocfU-qyFYT3uBaBj2y1j9AsHsav7S5mzPAPblZonDcfRq9TNOCcqAjMWSG7uz2zxQ4rzJ_-Qar9Xb8Du5srWpJh0JpY8I6Esrjzcach_8u7siOKC1ziQylUITU_qvQUcZc6e9SNC3GITMNDObpzOnMG1cuxUY2BmEOxpxrMqGrOQIQ5iNDuIWO7mA2JKXSlSUVbDuW3CFlzpXDOuAEpFGFXkKf4MwdiQErrtIDxQUPGQyv0cHhgQVwjkZggR1VtxurkyvDPhkDC3EsNNkR698ilanWXv5fzkNtEYp0qcUchOU-Q0A,,?data=QVyKqSPyGQwwaFPWqjjgNgu44P1gRaYw5pNi408-jr6i24wz3yG8_YFEWxs0de5QR-QeRsnXNsntfNp3qxl9Tzer4e6yHdXD1gpUJ74ky10Zi3ZjoFPfLg_OUQTAeBMd9uEYcFvI2G9NYpbP12tJZrqj-rBwzxOYjRbdtU19Avp2nnrQrsdmNXEkSgYaGC3OmcKAvRjMzj3w8uWI9kEsteMVo19TqN4Hoh8X5QWiG9oIOZSJzGsTq9vJFNcbDDh6928hiU9w0JRjDxF9aD0C88bNg9soPuZsxoZdJQS5gN2GfFd-FCQ-EE9crDyqiAfkwe-6iKgxlSGSFY_3zt35oT0EqsZgjBwsGkuzoCDA532Ydu_-eMOsVo-xZCKq8eKS3DfKZQ6O0hE9o6OFy4P4EOEzIG_xeyk-_NgLQ86WGN08rQzEz6icDm_TADkyP4uR5Cz06AigSkHdmq2EOhDckTvdiuj5BZCbR36I2vWoEt2c8sYLrCpHnIpxig9mtHRcp_TNqCJ12MRlL7JKwk0OgtkSjokVNUuwfmV1_D7WkiI,&b64e=1&sign=0df5196134cdf21d2cd7740599d2beb2&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRuJcQHw7yz67KuxPacy_Nnn8L6CsG1RunKpKnVAyQnbsDOreUkX0z6HvD5LWZD6Lig8UfBJpIXnRhYS67Jd951ub7M7oIvPiMnC1Lf4OG8hpcdI65ALlKOhWInFxpsbOM3sn-isfUqlvh0wrzwB4y7DPW1snDqySXeW0AgsZ03SKJ67ipBat9p6kCTTUqbE7w7_ciOGT2-mxRnTYFN4Lk00ofdxxETCe8UxYxjhKIaUBaB4IR5tw4-bJcM2Pw4mQ6xJXAHN7NPy5Fc7QOqbotRAl0ztEjitlnnzMzfootDhvGWXrI7ZYhnRuB7ErI52HURKBesOWThrRpkXALhZ5wFwZul6Ommta4zy_BR76omNpnpcO1BQqtzv08Htl8REz5ZerL3bO0wcHcSt7H8ecz_s3njSB3WB11RDSNLtAb2r9x0xFrZDcA0aSnUFEJxHnF44pULPLz7wvPt4McMKse4aTzTwLCJfHVmEZzqD3JMpgrh0pq3SoA4lRJgFBlU8RBqAe5AlLoQYj64JzSTI8h-V6OGY0BUoDeE9BGXQguAy_jOHWeeNMx3ml4uL3ZP9wewnDjAaoqF0gj1aYQK_93hhS4rbyY2Q0VkY_W1tEqycAUoBBZs0waGcfVuGDNAkp6tvtcpn7uEjL_Aga8TsrLOyhZHsLe9qil3u7ad7W11fuJzcAln8e-Xhi_q8Smal7mtgw-U17-QjxE3kteUEeZAFxvNWYNfQ__fIal1Ac5lvjkv41F77vaTajPa9sj2yD4wmC3WP_wYFMubUX67Qs93FzgadXZCvHgHqS2yPeDSK_nMx3hUTFBJQcaJ8mP9fsqkYobcnxTG-dab3qDgERpz9wTXWy8KvxHDVj8Qsouzp7A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a34RqnAhXchy27bCZwvRGS_0TjG-LptDXTXpTuCsaSWvUEbUsh-lbiT94Ly4251kj7VbhMuQ6Zjve6j4ipbX1e-3ET5F6lfIGB3OL83Se7kXwNg98EUK2A,&b64e=1&sign=3a988b8bfcb4a7ca0703b286ac9510e3&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 2744,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 158,
                                percent: 6
                            },
                            {
                                value: 2,
                                count: 45,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 38,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 138,
                                percent: 5
                            },
                            {
                                value: 5,
                                count: 2366,
                                percent: 86
                            }
                        ]
                    },
                    id: 430,
                    name: 'Sotino.ru',
                    domain: 'www.sotino.ru',
                    registered: '2003-02-28',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/430/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                photo: {
                    width: 940,
                    height: 620,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/247472/market_GLEMLQapUM-nOF_0VF-izw/orig'
                },
                delivery: {
                    price: {
                        value: '300'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 300 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: false,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '300'
                                },
                                daysFrom: 1,
                                daysTo: 3
                            },
                            brief: '1-3 дня'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/yeVN6MyfAuSp5p4E_sWS1A?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=L9AgswCOW5fLUV-XygHaIJcloS6SJHHX8kRjiG2KU4HJwCUZVV-VLX_3shXSHKaCNE60hJX1sMy42X2XkyJVE4Ke0LoORPfJJ4jKwjT-nKQll4MH9Iq4qSSJPQGrCnmr&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=430',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 940,
                        height: 620,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/247472/market_GLEMLQapUM-nOF_0VF-izw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 125,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/247472/market_GLEMLQapUM-nOF_0VF-izw/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZHDZV9bDu5FBQrvVXJC2Ap76YBHa8CgdRVKRAImPw1YWw',
                wareMd5: 'QW9bjFeQi7dEYBcge9P4vQ',
                name: 'Телевизор LG 43 43LJ510V LED, Full HD, PMI 300 Черный',
                description: 'Телевизор LG 43 43LJ510V LED, Full HD, PMI 300 Черный',
                price: {
                    value: '24290'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcwSZdwshUgaabG4l1Q4jBd008Qi9P48JFAMfF27jDtTxsHsXQoYvu1B61VpvG6iGx2Au8rwhCKAee6hO9FzmKxMQ_Lv2yP5Qx-Cu7onX_MN7lok4Sr8bb7Q0ujznzrkVJ7c7vRys7iCAyz3yRmzO2D4SgVQ7XmFO-6p_PoB4NzYuoLSR8DWPdzVEx0tBHfMpXYx98xR1k5NFiKYo0rD_OvNMoC7vb2YeejJzFzQxxPszyAuKiWo4t7ZdEkQNXbkz1QI-Vl9oS5CJOaZF9ewdaZSbwCLzmFbLmOyCloKCZ-m_Jmpxit9c7tXOcdqqW19sFQN2NqcuPgAE0ZAs79CJVAF-gm-E1GqCKsNzA8Zpcm13P5Is24U39kxrgKb_fZfRJQ85V-b20sonwUsbC-ne_-LNARX6586lC0wWwZi9sNsKFEmLCIfnKAnkqa9hZZ8ZAvU81RRHVVBYwOyUKQc1fkVxOJGXGciJ-jgUW2t5InkLK_4YeJTIAP-yO_aiDxTka-31auI8ALH75bPYcdDHKuUsatGm-enQeZT6WiabTSRUxGR7SW8ubjdWZ2qTZWCnB5Fdcw5JCBFafFIgJqDyp9xt0n34D22F-DGCXubyBKdv76nbv-ow7zYx68YGLxu75A9N7Bk3wohYeNCov_OwbzWjwBPgJpvawafFIk2thNzIlOpJ9bMAC7CUObHO8TXJxk_pZMnrmdsR3JsoRQ5WdME9BzIrqHpUHg4XDjVxRRh6FKe1XPEc2D60tUq0Ctn9o4IXLC573RcZFPOqLKAH5GnrAGPUsmTOlpMoeSbdcmHiiI8Et3SwPSWZuAiHz0n7Im2najQLQt8LqRxSLSfiDS_AFD3DO45_A,,?data=QVyKqSPyGQwNvdoowNEPjRFszCtQAnqImxtkKwlR54xlozQrLYZO7X3TuTnM3G6F3r5NOQ8w1PEknm7VrWpLc3rVIz2qhpVoYyJVcbaRq9SJ0_gENM2z4joS-f90um7Ny4P6MzSLW4ypeyv16GoysLHGPqBKk_3iN7QMkRUuxFvDMA77ojRjguqXiOeI2iQe8b5AXPLfW91fsaOMiTYz9vLmfL2LQSXaGjZoVQrkgs30TV33ZuI806oPePseSZSBBcb34t_9VzYAYpr4fUF-ac0OVENkSX5JyYWrxY6bv-t_SEv0vvXbjMmuo_uL0EyIWmsU_FsZu4ppSmP035RsGbTiQKTvhAoo6vSLDxgJo02UDSbnAcZrj63Ed8G9NhAxQKJPA3TNNhZ14dR7P6K8ZGls8JUmaom2OtkapUxFnz7ID86Ip1mEN9rnDWUt7Hap1Qs-reAKuak1W07Cd-7GsXMpjkcUTh7K1qGY1UJ5AHD7UPPXKqS_7JyYC2JRh0Iu5ZGkyXamy4TLvxRvYf2-jeSqh0dZRnkcbpy5IBLFKzHVCG32MQyCId3fkVBbK9pVFlKnLAJIaksS-ZlcI8ifAkQvklaKnqI7DZg4sJ7U4sND9SVobLzo8ylU4SkudmOR&b64e=1&sign=f461b0a40c41807cca3bf4eae26c7ce0&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRuJcQHw7yz67KuxPacy_Nnn8L6CsG1RunKpKnVAyQnbsDOreUkX0z6HvD5LWZD6Lig8UfBJpIXnRhYS67Jd951ub7M7oIvPiMnC1Lf4OG8hpcdI65ALlKOhWInFxpsbOM3sn-isfUqlvh0wrzwB4y7DPW1snDqySXeW0AgsZ03SKJ67ipBat9p6kCTTUqbE7w51xQUkWal7iPlbi5hlgzxUnDsfU5xOhSC4quzS5iFkdnv3_-0QJfz8Za01tLdHUQYRpX45HwX_vJkWAhqTTx0qzIsLObUJzCwEmfgrcZtBggc_mSaPCJAUy_CG07QoM_7MzHtLwKW12vqQdEtfzM1g6VEm7KJbzFFWZISNJO5U1kTTO0JBk7bQJdRZ57nxQHlJA8dGvMAi2QHqyrHjJO0o0Th-wdWeFN4Zwq4f3YPoqCzKqNIjrjzFbPULNWadYAhjUxDEJNBw0DX7jtqgKnxkWCj3WDtGNTkF502rPUoqmRQCoZXpFr5R0Fg3-R1R4tvnVO6gzorNh_Mbp8AyCwyQ70iRMaSiiB4M4ee2ZO2nXMNY8JtrTs_KMWX3LY6dSvdx8D61utP_AaUvylD5eQq1nuO7PiQI1o29Aw1cTVSfsqzxjyH4ufqt9tvNT-O9ZGtPML_EeqEuFAjjI7qVGIdba8PiVQ9112UonBgmnpQF92vAsM03sg7dssKL5Y9Mf-vwqchOILy1xLDRi1qzxsTMyN60ggIrAER7ps8ZRUPPFjSqiL5z23H8BFe9QdflvscHpVeE-m6ptFR5xM4frm6G1iuATpxprP4L9uxhOGoxRNJ3pdpqLmM-zF0rcov5zQYGOu8qq2egQ-ZQVXKniZDaoBs7ArCK9GQkSHnjz0gmCw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bLEEBFzgssnRjiy6o_c9zRc58iPwraffkC_3LlQ6cuLlhs6Wa3kbGuBBO5gYK2KDMKq4QfLma3z1-NifQYexp95rQYZEsk5aaihsOC-7APy5HlC7v0ypwk,&b64e=1&sign=f93b577e2af1d173153c7ce1e878c231&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 6424,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 296,
                                percent: 5
                            },
                            {
                                value: 2,
                                count: 90,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 121,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 586,
                                percent: 9
                            },
                            {
                                value: 5,
                                count: 5331,
                                percent: 83
                            }
                        ]
                    },
                    id: 826,
                    name: 'НОТИК (notik.ru)',
                    domain: 'www.notik.ru',
                    registered: '2004-03-16',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Новодмитровская, дом 2, корпус 1, 1 этаж, помещение магазина Нотик, 127015',
                    opinionUrl: 'https://market.yandex.ru/shop/826/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                phone: {
                    number: '+7 495 777-01-99',
                    sanitized: '+74957770199',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcwSZdwshUgaabG4l1Q4jBd008Qi9P48JFAMfF27jDtTxsHsXQoYvu1B61VpvG6iGx2Au8rwhCKAee6hO9FzmKxMQ_Lv2yP5Qx-Cu7onX_MN7lok4Sr8bb7Q0ujznzrkVJ7c7vRys7iCAyz3yRmzO2D4SgVQ7XmFO-6p_PoB4NzYuoLSR8DWPdzPP43cbycF0wVDoXXhnoXgv2ACHKR3Tyhni9V9C2sQg4hNdxoFW_HoiGwU8FNFiQzG8sx36HBHXhVJGMgObuQqGoqI3zHGqtuzLYbzrM_pAJoHT6bFbwg1xaQsksuhdhq650Aj517IdIa9ksFgtUyXT1OOhNtjIVO04WEK8chv7Xox9RzY2lrrd_X3vlU42QQBN3PaW7Bm0Vt-M36tEcBOecS6h0a52k7EM9jgh8Mqz0ysy7561IccBRQ_ieDoNS5NrgmiKyPDpREsVOjFvqduIz6SsPvTOiaU9oLwgAa2X9BRrsHJSyQSd1fqlQDwcD04jVOc3_wYyGm4f8W74nhQ82R-knlGn5KVz2K3S9xvKwbg4PDki1tLRZqQlrg_FcNFbyUPFMFJPt1Wig6eUN175mzx9xIlzGfuT93Ng2V9XDSwq9Paf-jHla9sIHQZGcFeNI7gXf5qym-EJqb54bpQwmtFJT_m_EujZ_jw3nkW2zJhm6KTSY5JzOgPXZ5H4lNJg2rF680JSIzE4IPrZKvqXZukbO46QH0j-uJlu2RRCKRwKPAt4imSwf2Ha87O_koMm5xAUpZqSl5XRWZCAUdBMty-bzHXqjQYGA86FzSHFHM2ferTiosEvJDYq6E6NW-_qVHDqElaAJsczCGNgVRdc4ZKOTL9zT4OI2Y1brY8Kw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9BMo3RBsOGmt0ud_uvZpmx4dKz7ITiIZtbH3nKgYKm0Q-8EcFKldsgFqM47lM3urbEzzCIEQuVVftXUaM6qp74F7Xz1EQdqUvTAhS05hLhHWkcpg10D6xrS1JT8NQmQBDPBbpHWLlGoOM1-iDKM0HCD7gD9trnSFANqzmUH12vBg,,&b64e=1&sign=bf5216654c074452b35e5fba1dbdbe8a&keyno=1'
                },
                photo: {
                    width: 450,
                    height: 350,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/175127/market_vE9kPDYglG8SL3fK94iNUg/orig'
                },
                delivery: {
                    price: {
                        value: '350'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 350 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 5 пунктов магазина',
                            outletCount: 5
                        }
                    ],
                    inStock: false,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '350'
                                },
                                daysFrom: 3,
                                daysTo: 5
                            },
                            brief: '3-5 дней'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/QW9bjFeQi7dEYBcge9P4vQ?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=L9AgswCOW5dNgfp6WC19STRMPK1lh9szRgkMAU8ARBovmBf5iXFi8B9BRql3XetENwIY6TsfWFutkwa3yVa3ZIC8a9FN0CREuLSm9mmAzWwtGG0Kjg3XQ3CB8KqY8wNq&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=826',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 450,
                        height: 350,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175127/market_vE9kPDYglG8SL3fK94iNUg/orig'
                    },
                    {
                        width: 450,
                        height: 350,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_ve4JWnI7Vcfqx8ps5kPHbg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 147,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175127/market_vE9kPDYglG8SL3fK94iNUg/190x250'
                    },
                    {
                        width: 190,
                        height: 147,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_ve4JWnI7Vcfqx8ps5kPHbg/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZGaEJuUTQio5biZkoopC2q5Th1ElOEU7SffWSBXSrcP6Cuy-kOrqH6ITYGGThmtX1becjuKsyCmR4ZJ-1LFZpYWeluZcguKCAolXL8eDldungWQGZjDUNKE4IgJSPmFvRu6IFX3dgFRGtUb5mf7vJeckxYqWMOY9-SBIkqRNvhdSNDi9Zvk9d0Wlh9JGT53xYpVNVrZa5J6hyRgzg03CGg6Q6IWdyudMu96-Z9xNMlRtA8KnDNZMlnP5axyGpdktX0DA0LF67Jq3KRePcO79EjIPMhh9553QUI',
                wareMd5: 'ym2oxZ7Wzi3q8tc474FQUg',
                name: 'Телевизор LG 43LJ510V',
                description: 'Разрешение:  1920x1080, Тип:  ЖК-телевизор, Диагональ:  43',
                price: {
                    value: '21287'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcwSZdwshUgaabG4l1Q4jBd008Qi9P48JFAMfF27jDtTxsHsXQoYvu1B61VpvG6iGx2Au8rwhCKAee6hO9FzmKxMQ_Lv2yP5Qx-Cu7onX_MN7lok4Sr8bb7Q0ujznzrkVJ7c7vRys7iCAyz3yRmzO2D4SgVQ7XmFO-6p_PoB4NzYuoLSR8DWPdyOADi8JW8EJYpSJ4SBqubDbaCILiHeyjrg3aa2y9b4xL8PHARdQl80LnK_m99JnnXLWh7aolawJCI-waaNsElVHpX85PitYNTkOUwoWDGl086PWefDsoxCeH8Io75zwFPWjWO5lisr_dGgs0Lny9hSB3DbWzDFIKjl7xs5Im66xKvN0ev74dtsxiA3ZCTjryouXjsH-14CmbTdJ_QIx3u_T5SqbtHFnhFoQn3f1m3TF6tgo_uneeXDA03VCPBCAbQLMiR4PtjozlrQfwqDOJH4EAUru1_NxN3Tz7TR0nE-iTUxFIDBNFQHO-JzGXowzOXQBiRqMMJGmUJd9r-Foa1jxSWHmYj6rxYDE8G4uaamfGuRKf4axBs4PrDWM0c2hp2QHimRygCswNU7dHz9KGfpcTtnYgxwRQyo95O_Ej1KF_APlkKhLnen1xXmynL9QOBkqp0y7x2sNHLbK7ZRwJQ6MLBOtDpGf1oLwMTq6nndRQiEs3uQoImHXXuD5AUNJekAfvMCPRKVtGc8pS6v8Af822zMB4_rewqLHmXTv_-UfN4EdCxOU_APTjtEandWj6e2w4udLLxZjy-4vuWI6pSh86iZwvu0vdC_iyr2Xr8bskA-n2e6nT3_AlEStAlsr9kR3KyqtJlJcYzdqyPfkrmyqeK9M7Gk9Qvb9fq5wyuDy13T6Dcsbl8q?data=QVyKqSPyGQwwaFPWqjjgNqo_Grb5e0pIf56PIoUo4j2C5Nn86meY4HDyb7zeTxzgIWtzgglMdjgEgSa_f_5KXXq_YUIaQp3h28bMYQcjiVQr3umBnp5Q848zF_GLYX-M8zQP4pMaVaHRtwpwprIHfZvHeYhynf71GxfvL-wFvJnsruy9Pd9rFPookySqA_xVQ-0gASq7Im8y7m-aNz2I13tbE3a9NB3ChgVzsvBvHLO6kCsBI5WETIohckgnWcBSFwkM0AeA6afsrsg6zRJ7UMpzjPBUOEcu&b64e=1&sign=7415cadc818c3e06b23088da3ea68021&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRuJcQHw7yz67KuxPacy_Nnn8L6CsG1RunKpKnVAyQnbsDOreUkX0z6HvD5LWZD6Lig8UfBJpIXnRhYS67Jd951ub7M7oIvPiMnC1Lf4OG8hpcdI65ALlKOhWInFxpsbOM3sn-isfUqlvh0wrzwB4y7DPW1snDqySXeW0AgsZ03SKJ67ipBat9p6kCTTUqbE7w4NKJEr8hKvZR3-7_vOUpL5uGjVsldIPBUOgn_32FM4bGgR8fEoTFTxaoy8q9IC3N9WJ1NJuEo25T4eRKI-gr3X26I7_6dpW08rXUrWzSpBrBtcctpU_TdDPwA-ASwinEMllPPlDGOXX6LEwDCjWI_fCpiV32XyWe5owMKjFj_kUwBiYYfeJTTTuaoOnSRjY0YBWMJjWtcOjdsarnzybjwfpjDJMjfS8h_yvIsSLlzPB04V52xg4SnhvtVX0QVCOwE2-mLeik7gh2jzwwr_5L1E68l-zNL-WsWUEvZIwHBN_QUf67D677icn13-RK55U-SnURxvEH18dhEQjqk73VtwpQA7OY22p0cdOC7YksO1ph4UR8pPDj-GoODtqSZTNGVDu1UilTIrTt5F7-8H4ZWMjSNAZH68BplBOqZCV9emK_LcwbY65OhGVjzinRS-OZOADoCoFxu9T6wHVu-c7ogpHk8rDssBcMoNvvYvjERdzsSDLthBC_4MnvwryjWB5SFv7xNxYOXmyzOiGLv8dl7UwBofNCU-YeV5_Lw2ey_7unlUl03C0RMfoVdaayoGD0VHL3-Vx4TjOBCnUCbn3C8JLpbTSXFthpBGH6IRKc-100oDxlM_GtolkDZd-m8UWWugesfjyvckHkOtJysNKM9o7TEPuCyEmu8f_Guj0DOJfTvJZO4Z2oHz?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2VnyTp947Y8z4YTRxai1iwluDnvu232BM9MTk565IQnaCaWqDHkCKl37a-3Bi894NFmJAcbktsSvGH1YDUayMbk7kEcTS3XHAHkJt9fG1arpSDt94SKZlb8,&b64e=1&sign=72cfd9c22017602db3afa737da280fcd&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 3429,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 508,
                                percent: 15
                            },
                            {
                                value: 2,
                                count: 91,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 42,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 103,
                                percent: 3
                            },
                            {
                                value: 5,
                                count: 2685,
                                percent: 78
                            }
                        ]
                    },
                    id: 1291,
                    name: 'TVideo.ru',
                    domain: 'tvideo.ru',
                    registered: '2004-11-30',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Электролитный проезд, дом 3, строение 88, 115230',
                    opinionUrl: 'https://market.yandex.ru/shop/1291/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                phone: {
                    number: '+7(495) 162-02-68',
                    sanitized: '+74951620268',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcwSZdwshUgaabG4l1Q4jBd008Qi9P48JFAMfF27jDtTxsHsXQoYvu1B61VpvG6iGx2Au8rwhCKAee6hO9FzmKxMQ_Lv2yP5Qx-Cu7onX_MN7lok4Sr8bb7Q0ujznzrkVJ7c7vRys7iCAyz3yRmzO2D4SgVQ7XmFO-6p_PoB4NzYuoLSR8DWPdxWqwlWj4DIIhcZIH_G6Ur3msQdF_iCrjbvLDLm4As_8fXYjxpelwErL1cRlbg65ameui8jKbOObKxWX8ok9JiR3UuLr-i0BnIHR3i-6xDBJNGVd2qG1hSwLiSJ9noMkmhyE9Q0G1gGRrXYBYNbbuR5P6XazObbn3KKcb0WXpAt5A0AcvMxs7i1tq5HAMaaUIPzcd5J_cuYLI0vcrHNDiOmR7Ti8ux637dl48GCpTuECm-klK5TYdVwj2Aw9Kp5TvIg-3kWWoDtpeikBgrdwSHpRBbi8H7yV0XQwM4Q1n2Pee2s5_l3-HyeSZlDXn9TT5EhoCe2bQlP3o7n6KTNk1-n4G2lsdQhPxF7FxSLmHxjFCFk25DoPFW9lAZea8mdUEwktWI55rrfREoSZy0bBa1BzAQ_u7Ye4-4GslntXni56qrTutTH4JCJcctjYrH_fVMSwfH0dJELl9qaYj4xuGt9ShaZnV6Ic6l9uKaGhq3FTfiTwk96PPvl11NEKxfiE5bDpSWe5cYUYpsbrnfn8PeKz618fEnVyl4n9layZJLXa5uArr5PdUzsZ_aKSlvTT8ODFkUGOzg2vPBUDqb-LYYvqyQCtC50ydaMT4yxhkutVaU_HFBS4LNgOBIX6fqRkW5kbNHizAZMrUEAwuY3pfxi-tG59HxBf-Bm_ZXNUJQvP2B5fiRb9fda?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_5Y7AGcmKEddti_o4EV3ngFV-6xkk2aEMz25YPluw14jJO4Slnl1lDD0uwSqYOTR0Hlzk9uihIiZPFL6y2E4PH5yrB-Gg2ZIW4fToT5RMQzoniLcJQ4En15ZfYOyBIgqHlpEIjdJhID04HiCy56xlzCzczAaTOCkx_HrHyZOQrrw,,&b64e=1&sign=e0fc83b4cd80b8fae47d1c5518ad6ccc&keyno=1'
                },
                photo: {
                    width: 500,
                    height: 500,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/212340/market_4eBIowGJqMGR2dXFEKm5pA/orig'
                },
                delivery: {
                    price: {
                        value: '600'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 600 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '300'
                                },
                                daysFrom: 1,
                                daysTo: 3,
                                orderBefore: 24
                            },
                            brief: '1-3 дня • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: false,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '600'
                                },
                                daysFrom: 3,
                                daysTo: 6,
                                orderBefore: 12
                            },
                            brief: '3-6 дней при заказе до 12: 00'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/ym2oxZ7Wzi3q8tc474FQUg?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=L9AgswCOW5dE9UTkYzHtPSyWpTjIW2L3XCdnQjCwx52_mPW9Rzj_78MpG72N2ZtTcJ-X_A-atcvDWwsMzaswqoNIMoBsoW44rpsZQV8ejkm0bxrV1kS0qCkMdOKaiEAg&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=1291',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 500,
                        height: 500,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/212340/market_4eBIowGJqMGR2dXFEKm5pA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/212340/market_4eBIowGJqMGR2dXFEKm5pA/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEsAOsoBCkOWMo-DB2HbRJ3IgHlwCIvRbOjVqpnYidX8w',
                wareMd5: '1KfGQ2kCFhw0I14mbJ4ubw',
                name: 'ЖК телевизор LG 43" 43LJ510V (43LJ510V)',
                description: 'ЖК-телевизор, 1080p Full HD диагональ 43" (109 см) HDMI x2, USB, DVB-T2 тип подсветки:  Direct LED 2 TV-тюнера',
                price: {
                    value: '22338'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZRRrA4uRtrCdnyR0YYXuymcA7jT4B-dt_6XBobxIo2tl5GaV0njy-MIAJseOYxAfYiwlfO_klZZOl_odDmqMmCQ-SMvRwFrKS0rbJB_yIhWuum66h7LBjzCiEEzkylYx767nyMLFwEYUTBJLKX29F6MYv-ZJtg-Bx36b6OsmeEHk-3Ci5vohFGbkNbRJITQ19f21LiQgJr6ze1c3K3SR5mdnvPAywnnUqUAoN6KKVF1mwQirl6fde2T-RpiEhjEo5bX_zGpWssQpUq49Wzg0iN86iHFive8phfRAR-Kj_LmMqYqgUPNx_0mBoPOAZ-QUJ62qYfo7MCIDH9nqc5yBCwSkLL46eXzrwckgIamp83FgfyJYwOGQrDaJSqhDv1qKhiHsNzzdfJVPi3oHZQaYRv3wGU-471WUvFnWUi9wy5SYapd5567Jn0KY3CW68mNZE2QoVcmiAQOIXiBBE9CfdK25qgSdQc3_RySPnOW_c8mmTPrOcwriEFGgmipXaKHEXqGP_ztMc-8cvyHcWHnJpyZdlqS9FE2U7NkI9FK7hSiBCWyNZoVJzsovxj8tFmW6hgviLZ5Z80xuOuGqm0-M1nltWs0aK95rfK0bkIhs3zgk-_f7KaSKSaUl95S27OM-PG6gwaqZ1q3qPjhB01U1ZBdUVyk7PWxkvkylzYi30sgcjJ9z6SsaqnPiJdq2nzpNP27rs5-lfRSL-tBSWK_o-snO3J0HUWRe2Kp1ZmBpMKN_DnYvGpbciuIyG575ap1ZdncHE_693bVD-ALuNedNiroA9JJK2psx1D9AYK0NdzkyUnZT0aURK_t38PErStXwmu_XH8uNMGpQmYSTB6AP4MDmSbMI9LlOg,,?data=QVyKqSPyGQwwaFPWqjjgNlyjJ3_ABx26H6vkNPjYxNd8sVEt_Jq68gyFm39OFLTHJvhSO9YmJw613-T6TwFLnghRxMEDceWqFvVn1jNkxNbeAfElAzxWq7ASc9sjKJSx6AzUlGbNyoqfOptnbIGU4xMK5hCGUaSeNHrkrlhm4nFJxjUw0J7XAcR6I0ca7PdQdSJNaRNVWCP9KvmEv0MdCXmCXvFgKz1quqBLLDsWVlSHaUhY3AJv_xWziKHoosqlf3z0CbeMeiOvamGEiPTd5T9FDHxd0X4bEeznlGYaZ1JLKsLwjxR4sMohNO3AP-SdMvGyjyQ-_znlv52VtGGysGXFLKyO5tVQcOkobkdn8JcIqghM4AoQbCqveOrhZSuD6F4S6r4W7MUpbEjfI0JHe_lMfes6jyeOLPnqEFs2eFE,&b64e=1&sign=1b9385c405501294f5b7866022e4732d&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVEdxTVp8ZKKSy6dQOdi4NbuzAVrQuY8-FelahnLo25xtgEHtEusdUhUhidWrgrk-OWnRGE-w_MJpKG0-IzuJDZhRXfeqMN85S5KAFII4OlsaDvqPT4Ch1rSKCAfAetzdPNl1uOmNXafp7NwDe3OpePzCRjMEBPhbpKpWa5K4LVHHQq_WfHtSgx5VyDUY-K4kXxsqtwsQIqdJ3Z8VG_afjjFc-lUcpO-TDGD6C14MYzbWm8YjlDik8hsRqGheOzGIwftnH0x0FA14aRfsaMD9LC7uGo05ZxcUyaLmcpOWesISmC-RIIxi54nGR6WaUpvZPOvmByKGdC_ukiQI1doT7qkORKq33p-tFAONk57CVXSfmPmyPRBD_IU60mrLGg0sXdXE0uJzED302hz8Wrv7KT9v7OR_A2sxaALRD6br-5u0BPp6V3ep_QR18OBaJiHcIg39fGQY_ZBxVqzpz-U45XLfEPzXWrzkedrKpvGypuMi7KBnYDDgc_yeFuDBW5_YJOZNqy_350b6gZSdyCTVGoftKO00N1elSZB7WH3RLmed2c8f2ZW_JKAk7QFlxazCb6QRsEz9S83jvvQrCmW9SpD2Qr6NjfrOgLyIwxLeY3yJymzrc1D4hTpbOW0Sobpqnt8tx8Er_aSPWAV704V_JMCIjJ9dVC2BjR7fmvLK_v_3a7NnG5zFFGCiuoT9FdK1w7hjYgBpTjX9jGy-0UDd5p-7-htqux40Ssw6i1S21lxpBTWH8jN5IQpDxPWux1Tyyqk37a68V6ltMGGcvL-TY84JXns-dLSxGs0UeebFlKQkt9gtaKM-04nbnYiQ5GYQrxZCqL67L8c-tdPTItANPF8hb95pNCwxQA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YWGjwUgX10n_YISSsvI4LYUH3AxK891Pswj5bhJmYn1DGcHJ3u7q2sqNniyG8b6bSIPqLVtZUPzQaZvaQCnS2KrAtywx__6SAc_5kPVbd0-bgivdkZoVK0,&b64e=1&sign=e159d69abd930c93b5bc39d2701da7a1&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 1498,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 120,
                                percent: 8
                            },
                            {
                                value: 2,
                                count: 43,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 32,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 191,
                                percent: 13
                            },
                            {
                                value: 5,
                                count: 1112,
                                percent: 74
                            }
                        ]
                    },
                    id: 324,
                    name: 'PlanetaShop',
                    domain: 'www.planetashop.ru',
                    registered: '2002-06-25',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Полковая улица, дом 3, строение "без №", офис PlanetaShop.ru, будние дни, 11.00-18.00, 127018',
                    opinionUrl: 'https://market.yandex.ru/shop/324/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                photo: {
                    width: 400,
                    height: 400,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/247472/market_JJ1xOyeYL54kw8zu7XqKKw/orig'
                },
                delivery: {
                    price: {
                        value: '350'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 350 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 2 пункта магазина',
                            outletCount: 2
                        }
                    ],
                    inStock: false,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '350'
                                },
                                daysFrom: 3,
                                daysTo: 6,
                                orderBefore: 13
                            },
                            brief: '3-6 дней при заказе до 13: 00'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/1KfGQ2kCFhw0I14mbJ4ubw?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=L9AgswCOW5f67pJHM6CttDR9vP7lan08-kf2Fy25zuYDp2Nyl6nSy7hrV2pscF1u4Qq2Wp-hkrDVTlpRn1pgNgqAJe8iNylkR4WjHFynV4E9O-X_oEIPvPU93Gi6MjY-&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=324',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 400,
                        height: 400,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/247472/market_JJ1xOyeYL54kw8zu7XqKKw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/247472/market_JJ1xOyeYL54kw8zu7XqKKw/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEZ2CEL2vV67f3Rp3H_WDd3AabuaHyonDJlLYDKK4Fv8_mBBEGHYzYD_Ji7zviKxwOSvWWtmPWYqgHjK6MMmVA4UkPkwt-GX3pre2_jnUWJWDmT3750_sOJQa7k0ElIGWRMH5TP0bG-S2Zqbwva6uLElOLUqZvquw-EJ2rkhzqHljxBGBc36wMXIG37c4JOqkeCjvsddP8fSWJcFg-zaHnxeNUGjQ6Ay91scCBD2ZcuaVdYFphEshCYwaIMJD5kB8XhZe4BEPtuymP-MOGVD8i-FFaNdrCVV3A',
                wareMd5: 'M0E_DrQg_C1LfoDcOKneIA',
                name: 'Телевизор LG 43LJ510V',
                description: 'Тип ЖК-телевизор Диагональ 43" (109 см) Формат экрана 16: 9 Разрешение 1920x1080 Разрешение HD 1080p Full',
                price: {
                    value: '22323'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tDIX58LwFXvM1cZzctn7l5VAhI06y4vsWtYeHX1Z4DMsJ5dzNEx9sUaRpMLF8JVBoReE4oCmT4D-guTiWdoDRuSzUB0jQOwifLbILv1wUN_4ZNVB3aOpfICEylwlenzQDoBY3_UdTcLouYOtFeWJ_1hu3g9Fu3iI51TgxzlMQhv4FxzTHTIufxPo_t56kd9ecRPnEs5_Un-EBjSHxYsxxKxIiENi--E_8-C99qlnKiOy0IXABHtX5cJwZTb7vWWYPc-efqJOn0fXsJoOsf6L8oohvAXrIh6UwL3z7itvNhnHbZKH_l7fvLL1XCi6k-puw0UeqcW9NCEQCm-EJ2pgBzv7z-YBW2SPY47p3Jt0Acr-d_jy9XLjBLKURHlcC0o59DQj67DAxReQsXVT_wxNJW6s_FjrqI0JhkNknB3tBkAMH_M5pw4SJSHLKMaorsT1JDQltSyVdXBxBsZD0HC3WU9XYT-getz42_pxVFdnToc1zCV7gpRrOrK0jOj9ScwKaINmJJV18NCRyJDEOM8nABa06TuFCfdXkQBSsgw3Vyn3KCFScvgzomZEKDEXv1LrHPuTnfe8hGqSViWxFB8yVPxyvVDeWMz_pOFMBGHQ1HDUZCIIv9IxtPZeGLIul8bqaHN8y1woCoFvl9c6MS09AWQQi44wxPDBrB8Ombu0hRbkizrVBI4VETjjvS6Dld3zqINiWJAMis19HU8n0DeBeBIzS9vjtQgmWMLlxrGzsgU9MfcztBe3w2WcIL6vK3VDhKR6_8Xhm_V3Z1KNoTYdcMl-LLhBy74EjqF255eJlX0BshrMMjFC_eV4awzyOsQv1ZUXH3ek7xrcHWgyvhnQ6dyxCUapzXubE?data=QVyKqSPyGQwwaFPWqjjgNjnwQl5o6nBcWP6_wdMiM75ipwOI6SuoZg25PFM9ju46uMPKnKNTTmMSjbKZ-QtETDQRAewb0DZvxhR6evtLbbJGWg6SB5yOaRRoO7toMIA5eFHcDfQOmtLFIps2S67kY6R9uGuoWTHbP8XdZKgt0idAvKdPuuLwAQddrJkMbJOIovPa7wHBejDijqVvWyMnnGkmmtKF8NcXUrlFLzffS5HPfIEX8NJEHVcAWMCZJucPHyB6o5HEzO22KDC-qX-D9c06NKY7Gumjiz4cn2brs5H3Qfn7gG1wKA,,&b64e=1&sign=bc322da1e5c0f3494579069bced248fd&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 11286,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 1574,
                                percent: 14
                            },
                            {
                                value: 2,
                                count: 298,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 269,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 947,
                                percent: 8
                            },
                            {
                                value: 5,
                                count: 8198,
                                percent: 73
                            }
                        ]
                    },
                    id: 1497,
                    name: 'ЭЛЕКТРОКЛАД',
                    domain: 'electroklad.ru',
                    registered: '2005-03-29',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, проспект Мира, дом 119, 129223',
                    opinionUrl: 'https://market.yandex.ru/shop/1497/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/163900/market_zm_BtuGH8jwOIfm77cM1NA/orig'
                },
                delivery: {
                    price: {
                        value: '600'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 600 руб.',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '600'
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 16
                            },
                            brief: 'завтра при заказе до 16: 00'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/M0E_DrQg_C1LfoDcOKneIA?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=7nUicpqaxmdndxUeEZbLHJaIndcfUKHAgE4L1WgpxLjH8wNdKnJW-A1LbeN_uA-SWzntHlfY-NRfgEmCKjU1NqNj5VLRJgs9OsBacDNzyQRDCMGFLFBgMWArIcvNkwWq&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=1497',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/163900/market_zm_BtuGH8jwOIfm77cM1NA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/163900/market_zm_BtuGH8jwOIfm77cM1NA/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZGAuBBqHlF2yaDUvrcL9blgtkGtPZdsKnO8NJX9Z4AwyA',
                wareMd5: 'UvhGcdLSENY5b48jSXMe5g',
                name: 'LED телевизор LG 43LJ510V "R", 43", FULL HD (1080p), черный',
                description: 'диагональ:  43"; разрешение:  1920 x 1080; HDTV FULL HD (1080p); DVB-T2; DVB-С; DVB-S2; тип USB:  мультимедийный; VESA 200x200; цвет:  черный',
                price: {
                    value: '24990'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tDIX58LwFXvM1cZzctn7l5VAhI06y4vsWtYeHX1Z4DMsJ5dzNEx9sUaRpMLF8JVBoReE4oCmT4D-guTiWdoDRuSzUB0jQOwifLbILv1wUN_4ZNVB3aOpfICEylwlenzQDoBY3_UdTcLouYOtFeWJ_1hu3g9Fu3iI51TgxzlMQhv7Dk7jKvKayL37OjP8NW_vTNRCEYqCZOR3yY9-UaO5_qa7lKCz7dlDl7lcc5n6jpraZoC_QpuzVcjMbLxXHfHn7K6MtLK_-mYP0yeTNOcc87aLUNtlZ4TMJ2Gs-SJXgTh-QtJswoQf75eeBD8bYiV20MNRtCjCzEJJTz5shb7gMT4GCgxquTTYV_YY73zQXOlh3UvDWYqPtgs5AyRhZfuNavQU9gwuLOJGBaNdb8WY-Ixe0uuoMSesbjRmsJi5ToleX4q-PMSbRd6SL-Fx9HfxF3E1y2LtWDVjtwVE2hd6Mu4g4l35dYHEwobCWor06gG6qOlMUfkAOcM7ZyOaHZHVdrKX51o3gax-MUbT18KaztOa8z2RnmdHwa3BBm1D3vh2NlKHWTJPihH1fWKOsS4FA8wkGzvHcPr-gQJm25bCabk6tZPLEADKjD_sxcq71vFRf7eB-t08qw3Tl0x0OEfXmg6sXHDiZFjujCkw_viKh8zyUsyMPqUWWtb8oi8EBOZ-otgajZl81iCpU5CftZzQLBs3GXQSKFcLoIIRuJS_JRFMMIdVH8VANQd6ssdZzva_2tzJP_EzJWWIQXDAsVwxoV_vIwpUgNsIdq7TlxPf73XQGEaSeb0tQ4QSmFS3Z3lvM4p8tdZrJ3kwlkluZgmt6wsD4-Mpx-XicQAUMCCYWSti752DEBF1m?data=QVyKqSPyGQwNvdoowNEPjc7wlEUqfa6bq7hSZROC9KAnrMWHwKzys_yj2owRFxG3wTIIXXeFmAV7Iwho2Pcevxvm-lcVRSYoL_WcnVHReYdEEEdB37Qn749TwqhDr56kFJSTaBPOXY3XFtlBcr7F5tq_594WfZQhntRegyk7xtiZOEswaQq0SG2G8bj5aywoOscAH4fmnszo5qRBa3UIk_Y2mVkGjyutyw3IZsM_WKYWKmp6qJ4ZAwcFR-B39HE3q1hjzYf66dB10KTMDN4SvpxqRNZWVHTi52NK-ludJT6cR4RryX5nPc4nv59PhrJhR2JGUubhk1sseYaKbxJwmzIbNhu4OWGg-VazYqq-BrxcGMoTkBLt_Q,,&b64e=1&sign=ca19d5f2279e93c0019baee2dc6f6545&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVB2c5UPAPMTakksWkEwoIuJ7eit5IfpDQYQYcXbdguyTZxuISpmOXUEzauL0CL5UpUicp2lJd1KxBkONQydjGNh7j_n4KzClpP6p7wvsresoTjmfZeNE87WYCpEH_-4BdOtUCmcbD_5_JFO0H1_C3x_RHIFQthrAMPJkPZk9-NIhqp3-iYJ_tZktfVJMKu6XhnpM-OZFn3HpM19ZG1NOm1Bj2BupsGzgWvWVhoyf0tWL58xmIAM7iKNKgu4t9W6Xc0CXC3y6gYWf90gNdTF_EmdShgW63ZpYf6FpXjWe7h3uflT1DzSI1Xbb-019I9Y4p_hLZlpRz8KNWcFooSYh5vsibiQ_59yLqZ1QKZA57eeZZjM1E_1e7ckme89S3GoVkktuHX92vWFsPL8ehb91VxdBLR3AQinspq1Tnpz_yzkDUxeQMSVRwpJGHYC7EOLEk8XGscW5RXKwL76klUiVv36VBa5AZA7fdBnaxDu1KsU8x-G8XYSgPkXp4pSuDukCyoVbrNn_iGVs0bfLV6fZ0QPYxh7ZnXzZfiXsUEgPp4XcPFZc3IbLfPxVn6UJJ4rwW1_DrvVlqgCiOaM4V0pWkZu_PC5nCK_cAJzUZwftINYaUM23H0jJMj0QNagLGMZq1TWKIUebPPVVqs2UvY5efUWVjLbgP1UauV3tPlKI1a1HxZAap50EdgKAdZiOYFStwqo91wDBXhOJhPGX2V_HuYPGFwcbbQrxUDFihJtqgsAqGoZXHICRZT-TrX39HGsa1jgLWePT-s9NfZAv1hFWAob-h6ps8cuXJ4jBvKFxpKmTjkuevqFWvWIsZvO5tP2Tpb_BH3Lc_-kwKKo1rNdoSDUBBoIpHL6RSikLRmEymddN?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a-7Bj5iCfJrfTeKGkP9grqOvpvRq02JvC1BTlhscn557NF_osPORdF1-d_2rHdQ905sCMrMzUYVvKZGGNOO-yl4ze4wln48R5GLkmk8sjjX0wSTqtSHJbM,&b64e=1&sign=7ef4fba3c500e77d20a7fd5073831178&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 63525,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 4817,
                                percent: 8
                            },
                            {
                                value: 2,
                                count: 1771,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 2577,
                                percent: 4
                            },
                            {
                                value: 4,
                                count: 8718,
                                percent: 14
                            },
                            {
                                value: 5,
                                count: 45642,
                                percent: 72
                            }
                        ]
                    },
                    id: 17436,
                    name: 'Ситилинк',
                    domain: 'citilink.ru',
                    registered: '2008-11-24',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, 1-ая Дубровская, дом 13А, строение 1, 115088',
                    opinionUrl: 'https://market.yandex.ru/shop/17436/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                photo: {
                    width: 900,
                    height: 594,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/237949/market_loZkIA0pEbESpirPCOpCog/orig'
                },
                delivery: {
                    price: {
                        value: '290'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 290 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                }
                            },
                            brief: 'Срок уточняйте при заказе • 13 пунктов магазина',
                            outletCount: 13
                        },
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 20 пунктов магазина',
                            outletCount: 20
                        },
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 24
                            },
                            brief: 'завтра • 1 пункт магазина',
                            outletCount: 1
                        },
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 2,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: '2&nbsp;дня • 1 пункт магазина',
                            outletCount: 1
                        },
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '90'
                                }
                            },
                            brief: 'Срок уточняйте при заказе • 5 пунктов магазина',
                            outletCount: 5
                        },
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '90'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 2 пункта магазина',
                            outletCount: 2
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '290'
                                },
                                daysFrom: 2,
                                daysTo: 2,
                                orderBefore: 19
                            },
                            brief: '2&nbsp;дня при заказе до 19: 00'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/UvhGcdLSENY5b48jSXMe5g?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=7nUicpqaxmdaJjA_ogtoSK7PzpL7VUEfjazyVHC4AqxmmO03V0SlZXfhNZJR2nwNEiyGRETMZ-b4Mzw2SB9JCyUieAw6IwAR5BIkZp2RYyE9m5Iy2W-zyQfFH-hxJlU2&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=17436',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 900,
                        height: 594,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/237949/market_loZkIA0pEbESpirPCOpCog/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 125,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/237949/market_loZkIA0pEbESpirPCOpCog/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFuACZrp09_xUzPdCV9aqnODGBSdjd5GQe8VWVcX9yudA',
                wareMd5: 'bi91QhNIhFIbOl-6GmtRgw',
                name: 'LG Group Телевизор LG 43LJ510V Black',
                description: '',
                price: {
                    value: '21550',
                    discount: '17',
                    base: '25990'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZJYCi1lj_2s5AFKiObaMoRFph6sAYLCyu5BNbW-qeo5v_fdlIlGG7sKzSIrFMt-AKnuEerZW3Sa1ojL9HlDMUXXumhAlppCwxEsrBKI94v1vYCGnaM5p8s_XdBCp6Z-GB28mjK8QOun6MoydrCzvVaUixnIDEfOpEiZrEyzHVUVdZf44FgX59yPSDhUiyUEK2rFZnIjgZELo5lRk0-cwkr5mbHeX6gWeWXfCo_XgfxFjtQKdi1nhGTdkudu18Cf7KuERqNRq1m5qkNhJMafsY8W5KOM21loO9ua7y3as7FxdBXnByhuAQyGwcEjj6-aReoQjF05u9OYwAtXBAt-qs2NJO7s6ffT075EsLbKwGmt23fOSqQg-NMMGiBFarKx1ju9tfFwRKfGsjyF1dSLqVJzJZ14src1x_rD2avxBZpxPZhh6LEnoUoCmx19l46Tzu5CheWxUInKG9dq3MZCEel_yGhCRrjoQ3Sb6rknQzAN5S7hSDmVkgHxWfK1xDtfzwFpO1IGHrgLnPbnzx6t5_84no64h9tvcKvMSnln3kpX7KoeDjrzGxXil23LocYRjh2kEmrzfWqUbXA8T52epr0Lh8oi2OEctVdzzpP_YPQ5FwSp3GqLk4WpjAO8Cly8lnAR5ok4yqRnTHprGeDtuf7khHChxedCuXNRoHd-XtRdiHwbySIeTHWoVCPcuOFpQBeGJJqqMtt6DtMt2zSWb4re_Ct0bFK70lBGWS3Aay47NZ79hqqPyDVRdgx1jsVpjVzr9snluULejDHHsyo3IFc0pWyvanCobxD0lXCI0OqMz3pe64ulWEm1RmS4XnjjYc5Hb6JQI0zLTPGAkDfQqxM7QHzT1kShhq2FBEgpuaEk?data=QVyKqSPyGQwNvdoowNEPjeJJ8lagu_bxRsQYhrbB282QqX7JVJPQvtSG9tKYsVIX_3-G3hg5gtncNhn3nCT8mFtjLlZFMnJbYmEIm2hUvhY20jb1HsCrYvrOLjihRXGoZ-RY3VhKZd6ReDiQZkt1A4UC_4H1GDPD0XfVmzRcmtMyA73dJ9VYdzwTdcFO0ugYbqin3Q5gfLMk6bRWWbTtSUwfF2TTb40NsvhpGMGu7eVuBoQW9oeIUsWRffcOshuhkByprzqHwK6CogY5hs_EAVsxfm4ML_gJRpmUbSIGTjijxbQbQQkjOYaWnOWeLhMdNoctz62BQ6-VqDOr3tt9HyCN6H5ZtUW2y81EGyuVwBJeulZUxJQwXgqAlT4eYI46dsFq1NUzCRkXcxIBQjfTrW2zDhcEB4sLh5R8KIWBwL2hqJny7sjPxt6OB4qCjP--35MeDUhliyxJGNXtLru81A,,&b64e=1&sign=fd63cfe94edadb143e29fcb9687e791f&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt3rmxb1rHsC_1QLyZR9dIlA792LNTUMer8ryCmbb2b8Z6olaw3qP9ykw2dNiMtkk-V0ZMtaH3O2qrZ2Cmf_-WDNo6Twn3cYyYM0r1O3sS510O_0eZXrvXqm6RwhWDDsuea8PpILOxfuCCuvjaaGbMN5fXmoo7bjvWVw_i-eJyNPI0Xnfj6iaSsNIAlWMEgf77Awa-GrN66S_28kUbS_cwGW_RjE5EyzWRiYki4GeL9nZ_GN5_92kE7-R87e3SZOXOXv7JxxN_mbPoCYKwx9QqsXn7cM2t24syqSXfj9iaRMLYtd-omU7BElZsxEo1i8nRCKisbBbG7MxelTfJjJDunbDPW-a-iSOm2wIm7pv-B23pJrLL39Tun6hpbGSYSgbGBOuDz9f-Pi-S0eSea4GWbuZlPtW9YiekBK1jxfsmBaOrbgnI_ISTqVn8ce-zrI4IjmtmSx1MVN_L_zk-HJDmE0JCR-8fgtl6GLj62A-GG9WvE4xjKsQ3KnmX0DAYmoIU_foTJkZXEPbd-2fQd6EApvaWnMVDzE2qfKmFK5VBFMT-6RUlx0qJ5xPbEhZaNlzo-MfclVuIvm62L7MsCvLECjT0ZcQS0tUGwXVXDHuzGOdCR6SDJfS1uQMgEFsO4OxNxhRka3q0WHQES_vOxDTLo0rbJ3lgL444EM7Q1xLWggWLyn5LglC5eQGIgqe9hm1GMA1uihgIWRxoflwNZxUPlspwZRRm0YLh1qgSKCEeBoPJmav17jMNYmOgtqhkQS-zlq4S5ixlLbRlbcoFJXnTISXrszjPnUI16em7lDxXPL-j7hXpbe1fEYBBgZDhNofNQ2uF_yKSfgrWVXR3pwQbc86pfeDX3aWFqGBk_aVFJ-8UQH4XNNQN4?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2exEsSolq2lImnBthIsTwaBZEsL0svMVJMxvDRcsOLyL8GPeF4FdRcv70PVgffhVEgOyXz0ZnLp0teyKGmuou3Ei02WZNonI2uiQgdUs6gtswkrzZBqIh_w,&b64e=1&sign=e454dd42d52c19fe50218fe4b47b287e&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 1347,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 49,
                                percent: 4
                            },
                            {
                                value: 2,
                                count: 6,
                                percent: 0
                            },
                            {
                                value: 3,
                                count: 24,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 33,
                                percent: 2
                            },
                            {
                                value: 5,
                                count: 1235,
                                percent: 92
                            }
                        ]
                    },
                    id: 108034,
                    name: 'BigAp.ru',
                    domain: 'bigap.ru',
                    registered: '2012-06-13',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Улица Барклая, дом 8, Этаж 3 пав. 375, 121087',
                    opinionUrl: 'https://market.yandex.ru/shop/108034/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/210814/market_k3LgpRN7tlaG5cQKpxFNSQ/orig'
                },
                delivery: {
                    price: {
                        value: '600'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 600 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 2,
                                daysTo: 4,
                                orderBefore: 24
                            },
                            brief: '2-4 дня • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '600'
                                },
                                daysFrom: 0,
                                daysTo: 0,
                                orderBefore: 14
                            },
                            brief: 'сегодня при заказе до 14: 00'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/bi91QhNIhFIbOl-6GmtRgw?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=7nUicpqaxmd8d5atHlG3Rlrr7FZivLuvxsMmcSV34mWojiZta65_Ze1ezbyXVbkBZMtj4iPSRkjgNlKuzahoFRWQHaDGDlKJNvdv7hDQT2EVbr2NJk_Yc_8IjLwxGFQN&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=108034',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/210814/market_k3LgpRN7tlaG5cQKpxFNSQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/210814/market_k3LgpRN7tlaG5cQKpxFNSQ/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZGEilokHZPuehKUHsyjaGkomBKrmApjWyB50WLvYxYN2FuhdC3TRY1rkY1StR8D-d2ro9zIpi8u9dm4tNpp6OyuMEU48ZfHW3rVXG-QaJvj1k-w-7WjfFuIpElEOgxahtMkX8yb_LEm1ThjuhgCdcAmZeh49nig4WGaiR8Ev0qaHD89UnhUQLnmKmW9P4EZsDqphRtwBN5UumCxrvD7HAttOaz7jKcU38o8nWJi6WyfyJfhtnm6HZtgPyW8rW8adyZDyZXxmystxV2XTZSAKGmwuG9sZnQa3bU',
                wareMd5: '57LUyDKCHqGE1deGO_pd-Q',
                name: 'Телевизор LG 43LJ510V',
                description: 'Входы:  AV, компонентный, HDMI x2, USB Тип:  ЖК-телевизор Диагональ:  43" (109 см) Формат экрана:  16: 9 Разрешение:  1920x1080 Разрешение HD:  1080p Full HD Светодиодная (LED) подсветка:  есть, Direct LED Стереозвук:  есть Индекс частоты обновления:  50 Гц Прогрессивная развертка:  есть Поддержка стереозвука NICAM:  есть Поддержка DVB-T:  DVB-T MPEG4 Поддержка DVB-T2:  есть Поддержка DVB-C:  DVB-C MPEG4 Поддержка DVB-S2:  есть Мощность звука:  10 Вт (2х5 Вт) Акустическая система:  два динамика Поддерживаемые форматы:  MP3',
                price: {
                    value: '24990'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tDIX58LwFXvM1cZzctn7l5VAhI06y4vsWtYeHX1Z4DMsJ5dzNEx9sUaRpMLF8JVBoReE4oCmT4D-guTiWdoDRuSzUB0jQOwifLbILv1wUN_4ZNVB3aOpfICEylwlenzQDoBY3_UdTcLouYOtFeWJ_1hu3g9Fu3iI51TgxzlMQhv5PL8AzXgYAg3WDVunQAFoG-rQwvM_-h32az-PlH0YUbZtkEIfowHP6D2dtEjOaL704YucqtfcmRotrNQJM81WPllHOAYJ2Z2_c4e4aqSiinHs4VBAhNa7vIRw_FsJuOmbyR0EjcuixpnDx_G0CP1My4YMtuw_UJcxG2COHAbl_etI5pc7tM7Z_gwjy5SBbudPZz2PYR97zm_Of3PixSn6C4NvWPkExmfPh91Y49oTDnjoC6AFQ5nEQuk7-TRebD4LuiG5EbKvKRM6gubc_tGgI8mQMeprD19HRNZQsscUN9Z-XiRy-NOwK7PlZN8i8hT66H64LDkFjnYVYh0k_r_5zcXpp_I0LqoGZ6RSy0IqMJsJ4HlEY2v85QN5o9TlfEQ5c7cfrUwfSct0_OyLqQmCx9_AYSSsDeDrxLLinWKPZt-vxezGUN86sH4DT_JZrqjwi4rPhiPPDYePdjSUg1JtpRj-fjqZlIAaLFPQQOaX2ZDiCHmbqVYuysAdZnPZH1teEOTiEzUQAPaS3vcWV8Tai63Fchziyqih9r3KwOmzyIa67MDUM5P1AXFQyviOYwA3_qzJlWvqQ2beq7j03Kx_SP6kRkq65wC0p4FNo-qPxE1h7VBgRIBqj1Tz7bIoA2081XRGdWyVINuRgndHWdAx_oBwAxlVdok19Uxh1XQc1dQ,,?data=QVyKqSPyGQwwaFPWqjjgNuDnUT5aPuVlEbTkbueGYgeMYddVibSzyyZUaPLbjBz9OpXsIoDZE0v4V-QUbieG67UxHDvGQ6BedPw5FeV_UU0YXWz_gI64ZuwI71lHn0vxo3cRh6NyLJpRV_146HdMg_zfGV32l4Qc5Ld-kxo6Q8KC3kn9Vlea5Z67-GwHVoum9f3gcchp-lXHMsC6w2fxYmTstpkP1qzrOeJcoKeUmUlIquVyKyVoF6yLG1mH4i-hFgcy9UPm1Mo5O-5cpRC9NlhFTcHNuDC86AsIjFWgYcTQ53oiyZItTGa7IfNlWTSf9BjkIFrzdMw2eAY4L9rn-zl94Gk9yqTwVWcjNBCkUVpbuQ0ChFMZjV0VT59OPCCbbLiotd5ue6RL2j1XaqUVGzdqT-C0xEwIsI2lb1atoYso6pqEu42ffA,,&b64e=1&sign=9e6b5c68e8f588d84e1785071fc41612&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 1572,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 99,
                                percent: 6
                            },
                            {
                                value: 2,
                                count: 43,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 35,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 89,
                                percent: 6
                            },
                            {
                                value: 5,
                                count: 1306,
                                percent: 83
                            }
                        ]
                    },
                    id: 6294,
                    name: 'MAXVIDEO',
                    domain: 'maxvideo.ru',
                    registered: '2008-02-11',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Багартионовский пр-д, дом 7, корпус 1, ТЦ Горбушкин двор пав C1-072, 121087',
                    opinionUrl: 'https://market.yandex.ru/shop/6294/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                phone: {
                    number: '+7 (495) 3746001',
                    sanitized: '+74953746001',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tDIX58LwFXvM1cZzctn7l5VAhI06y4vsWtYeHX1Z4DMsJ5dzNEx9sUaRpMLF8JVBoReE4oCmT4D-guTiWdoDRuSzUB0jQOwifLbILv1wUN_4ZNVB3aOpfICEylwlenzQDoBY3_UdTcLouYOtFeWJ_1hu3g9Fu3iI51TgxzlMQhv7VUwUoGrXgibl2th5XxgawG_IVaZjGqcFKRbl_r7cDZjrYrjFJrQ908X6d3-ie0p6w-5AqdfWzz5uI3PcP4WDrQHocgBZ-tK-MF8QA3grgJkf1tvDF7TQQS_yvDems6menme3UhRC9epafBrug_rcEwIRIoSWrwFZC_so6DpivD3rsdh3jxKOfnibv3j7iVHhlSmlRUEQK95H_w0r3b0nmR5JHkRaDT5-sX8JP00rem-WYCL4Qs051zcAqhEk-rQhSFm3uTx5MRBg8ewC2yuRs88TCWewudnb6G902e3OPRoyfOdZxSNbUNnLL8VgttfXE0UcwiAsmjSCPgV6v0FgPGt-pYNgp3ejC4rIsws5a4GEzdCqHjJdGlboOtrA5XXvNlRVUc5YJdRKKLrfdnXAdu0j9AGODJJOVT6rzXn1x0NAf6ztxBlBsT7xrUgv3jQ68407K8IZM1ugHje-Vz6cK80PM3Eu8oTJ77XUOu1mSvZct_rEmjvz87Qg0RMl4MV7-hxpmFVrPQldSK_kk31TL4nw8eqzUeWz49ZfTHoUsKFBhPigZ-Rsn_ORGrB0_oTSHSX8G73Y-SJOMhHz2QF4Bz5io6O3eXeiS72DR5wXYIp74V72da-UeJdkHiXoP7BqSTYZa8R1UBWBiUfPYgTe488haYMQt7nBskNdeg6Dr4w,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8MrWwOiQr1Q_I1ghh7mSSBHl3ZJL333xtVpa6helZparbh3D54MkxCvOSAFXA-C3vGrCSxLKg5T7D4HicLF9FgEu0OQsQkxOa9daaj072CALaKupiePOm3eIGoh1cadCWX-sIdeOwnsxPQuy0-WVqeTvYCPUuijeVxJOA1QErFlQ,,&b64e=1&sign=5e4d1b10838ca3c89a56258ce378378a&keyno=1'
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/247229/market_SRLmpp3oLwVqWFXFV4r8Mw/orig'
                },
                delivery: {
                    price: {
                        value: '300'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 300 руб.',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '300'
                                },
                                daysFrom: 1,
                                daysTo: 2,
                                orderBefore: 16
                            },
                            brief: '1-2 дня при заказе до 16: 00'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/57LUyDKCHqGE1deGO_pd-Q?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=7nUicpqaxmdjXvVupkIauHV3TonGTR3dcPA-zF7iENpaSlwsSxdDwnVVE2UhlOVota5RBW853HZphTN9A-dKNCvqdCemJZX7lokcZUHHCGq5cUQO9_4ZQPSpyY0I6a5S&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=6294',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/247229/market_SRLmpp3oLwVqWFXFV4r8Mw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/247229/market_SRLmpp3oLwVqWFXFV4r8Mw/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZGQCWaKR-AwOC8ZiDTntSrffbo1GkKfpfdbeDcYw989oH-apc8fzzedEhdo0_CBBrtWDHmWiUgXnLSg9jk3zLnTPaO9og9Qr4RkzmM4B1lpM_flgG8MtPosirVONc8hK-ZEYzhTj5oirlxsOTxfltLr55MPNcf1WHWYd48X31iU0wqVbJaDQz-UqhLwVu3bCsm6a-uUOp2CA5Z6wKDXImW5SfFIoGtlbeRIh__rdpovUGKciCObiW1wRNpjNhUnGq_VE3ld-qPV04xh-TOxXB0BWBwFbAv1yhs',
                wareMd5: '7m2M8WQNOIPjMEJ5sHfKbQ',
                name: 'ЖК Телевизор LG 43" 43LJ510V',
                description: 'ЖК-телевизор, 1080p Full HD, диагональ 43" (109 см), HDMI x2, USB, DVB-T2, тип подсветки:  Direct LED, 2 TV-тюнера',
                price: {
                    value: '23660'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tDIX58LwFXvM1cZzctn7l5VAhI06y4vsWtYeHX1Z4DMsJ5dzNEx9sUaRpMLF8JVBoReE4oCmT4D-guTiWdoDRuSzUB0jQOwifLbILv1wUN_4ZNVB3aOpfICEylwlenzQDoBY3_UdTcLouYOtFeWJ_1hu3g9Fu3iI51TgxzlMQhv5IFyXZyr2ze7vG-QuL0vKESdJ1-YHWsyKWucvmG8UP_zaMnkoZpRIAvRl4cNyYHjJVOCWjYrOKi_4nGeTZTzQ6IYQKJC7wJH0nzf9AmnLd355qvCioWrLAwwoqTTMULxPIj_bh35kfhYkLTcHG2BYeM2aV9vZDWmKarlXVRWZKFydLkyCvLxe_bZsYyMJECrQmvASVHvf5GQj8XJIS_47Qq5RK6XeH0T1UsqFXbbiZp5m0JHdOFcURBtZRozm37JS7cpW8oXI2-pTSVKXAoAp_s8W5qEsymdvBkIwY3Y7tlBzIW95hiYZhxztymS6yOeujzdyfhdCHEXrhZiaS6LXKvJyCraAfGioyr_qUE7mf0GxI6Fl9nNqOEzR5YxAIjceAp_gUPIhStCh666MoHazMVusehAQ6Z5LnQFrZDS6qvu2HFOrynrEr7D_fa7ZZdhdOmzGUKDRlVyzu4JmQBjC_luIqszQFHsJMCii1iTOY8Q036Y3U_5K1rBi38NCl7P0nlVDhWfyEBZJAtxCNPjXOtm7SbIU_z7MUPTFziwOgI9vaAStm1ZrC1Gn-2RxzgEP2NFR9L4fI85OYk_2BDCXrudpNTl5eOzRIMFXF5f24NVivY7T1p6F_bxLDH1gdzNacm-4wVtv35gZk19dMs-Dz6_wyneOhWrQBjIhf2_YTAQ,,?data=QVyKqSPyGQwwaFPWqjjgNheAGY9eRi98Iz677nrFsaKcMyX9LWyZ-x01AuatjvWsBgbo_oUr2Sv420k8E-1Qeo8yf72A_w-T-dSBsTQeK948Xft1-PEkoWVct2_uF_nZNjj8d8LQemL72ip_9EenXA,,&b64e=1&sign=4fa7c29ae346a53eb47440f045b15a27&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVB2c5UPAPMTakksWkEwoIuJ7eit5IfpDQYQYcXbdguyTZxuISpmOXUEzauL0CL5UpUicp2lJd1KxBkONQydjGNh7j_n4KzClpP6p7wvsresoTjmfZeNE87WYCpEH_-4BdOtUCmcbD_5_JFO0H1_C3x_RHIFQthrAMPJkPZk9-NIhqp3-iYJ_tZnTtlSnOVCh4b6OK95cZkO_N6FbYn1CtuXH-2NoafJ_X5utgvUMjYf4gxZ4xTZA-XYfNxAzB9W_3kgSiViqa6rAbSBX8mPB7ZWcB4Zc1C9UHZ-rG-NY6KECyxZGxfMHro6arD5Y6ju-WW1CB6s0jzyYna2P-f1BndTodi7v0D2nD23h1EvHDT6QukNb7TxyxBKLmH8C27dn-6IXPp4ynY3MMpM3ftITvLW3x_G1jhlFynk9uwuFetisHZYeE4RGndjp7ZR4THd1m8pcfFbYEeAIJYyrKdJnLqDf-Z8uos2IZlugtZl_flGr1M2RopOU_K5fbuAgy1MXB1y6pf8BZ_Jf1OnlcvEaI3Xn402hBZM8X_tSlBSfpg86sVk9TLDUIZI8l4NVF0j2vwy5BYy_tctH1he4-C5frRrp9T6H5T0nzYpWCn7grjT1-MdPIOI39YtW-VbrxN-yHRXw_YhveUrtnGtHu-sKrpo0ZUnyRx8fKXe3A5G7BibaxnsF5g-Pj1U3aNtvahj8yWXhFLNiZygPdwTDJ47O3ktm8xEUVQZvMVaAWvvYeCQuQsCcFmCn7P_BfWkE_cRhdnYTDzGedfEkweHtHIQEIuqH1OR65UpXbxC7B6J3AWkaV5uEs3xX5laT_wT59joP89m0HP1KsIEn_TpvVJq9BGEo939HolK6vg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YGb3fP5B4xTkNHpG3WolK9O1HuUqKtI10wbNm7M-3rr2op627_hNRTG9EYJVSrAj3Tfa1b1o3XBlx1eyFPzvc3JNHA-1tVOIySHC5aYkxdJLxJv5oSbq04,&b64e=1&sign=6c947ffef8e9cb857aa458c4c7852976&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 16632,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 1161,
                                percent: 7
                            },
                            {
                                value: 2,
                                count: 348,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 329,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 1237,
                                percent: 7
                            },
                            {
                                value: 5,
                                count: 13559,
                                percent: 82
                            }
                        ]
                    },
                    id: 4398,
                    name: 'Регард',
                    domain: 'regard.ru',
                    registered: '2007-03-02',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Волгоградский проспект, дом 21, Подъезд 9. ПН -ПТ 10 - 18, СБ - 10 - 17, ВС - Выходной, 109316',
                    opinionUrl: 'https://market.yandex.ru/shop/4398/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                photo: {
                    width: 701,
                    height: 466,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/169660/market_sDjir2KgeIrfXPtf7TIDoQ/orig'
                },
                delivery: {
                    price: {
                        value: '490'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 490 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '490'
                                },
                                daysFrom: 1,
                                daysTo: 2
                            },
                            brief: '1-2 дня'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/7m2M8WQNOIPjMEJ5sHfKbQ?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=7nUicpqaxmeCSE8qHDvcVYxMWUhjFEccNxE-GOWX4-suOMHqpfhy-h-OBQsHJM0vmRG0TIOw_TZW1-vL7asqQjWxkzEiOg-XhRX7eTs-NNNMMq9OijY8GAlizLFfQh3h&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=4398',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 701,
                        height: 466,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/169660/market_sDjir2KgeIrfXPtf7TIDoQ/orig'
                    },
                    {
                        width: 689,
                        height: 562,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/172627/market_GBWEHrygr3V-HhxnIH0rRA/orig'
                    },
                    {
                        width: 579,
                        height: 572,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/231203/market_2mP7zTX8153sUjYQYLJjMA/orig'
                    },
                    {
                        width: 701,
                        height: 476,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/208469/market_0Sl0C-JU-otsHb51e8gGhg/orig'
                    },
                    {
                        width: 192,
                        height: 542,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/231203/market_6QO9MzGbdsoINwyyAc3gJg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 126,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/169660/market_sDjir2KgeIrfXPtf7TIDoQ/190x250'
                    },
                    {
                        width: 190,
                        height: 154,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/172627/market_GBWEHrygr3V-HhxnIH0rRA/190x250'
                    },
                    {
                        width: 190,
                        height: 187,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/231203/market_2mP7zTX8153sUjYQYLJjMA/190x250'
                    },
                    {
                        width: 190,
                        height: 129,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/208469/market_0Sl0C-JU-otsHb51e8gGhg/190x250'
                    },
                    {
                        width: 88,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/231203/market_6QO9MzGbdsoINwyyAc3gJg/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFs1IEP9rwtKluHz-q1QTbf05YjJtUFtIfXXUjGB7RMvKl0a3V9qBGp0YECWQYVPqi5n5pzYFTS3Oo_3B2l0khSW7F4Yf4C533iBsQUSqs2DKbVM27dx4EUlgPrnrw7qZfmooA-_kYSfuvbQVt19jxuzh6CYvLHR8ZUqW_mdgKklMaa2rwA4Yod02c8cGZpKBHf9effkBPmfF2ZuYvPwGeHGOJ6J8YRcClJ8rFu6VfFxqwJyiNkh8MaE29YvnM51f4gUM8RP7SuK2wxqEuhg-KnI9gcHbnLBsE',
                wareMd5: 'lAEyJifOIHSrm6tx0toJgQ',
                name: 'Телевизор LG 43LJ510V',
                description: '',
                price: {
                    value: '23990'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tDIX58LwFXvM1cZzctn7l5VAhI06y4vsWtYeHX1Z4DMsJ5dzNEx9sUaRpMLF8JVBoReE4oCmT4D-guTiWdoDRuSzUB0jQOwifLbILv1wUN_4ZNVB3aOpfICEylwlenzQDoBY3_UdTcLouYOtFeWJ_1hu3g9Fu3iI51TgxzlMQhv4Drs9ysVo2NSDz6rtbbY2GbS7_rKblFyNBAda3CJAK41FCtPKs-mMzupIdyF0YYYTxlhCsra2mKmL9IPLJF8zqFcBnfW7UcwBpKdsLzFexViJuUsqwaiQZdHba-LFBFMCYmu-dumDuCe2z-8i_5icJby82I0g-RJ59GLAs5yfSP4JaP028PTPPtWMc1d5Z-ocZ20Qan7ChQSqsfJc4IRAjPLOF1zfVbUN0cObC8Ka0QpeP7dJTxAr6ENDW9a3blW7SBX5HyVlDmJGeLSc_yHigoO7mfQgd7ZqkJQZdvgf817O5WD9T0sqvEhw7VjLhAC_4QvXvp_tv3pYSoTF6aEfjeqLjmEruH9tv3vit8qljzeeXgHUdDeIlAXowcO1DzFEStyb87XbSGbU9wwiKW6gdXY2SJVvcyTtb0n2XVNaq5KmsK_aZfHK2V-vO6aE7sTbYXCHjBe-vQTx8UCRfjQZ53zo8PGiWlNaNMc-8VOfRY1Mlo57LrMP9N4xFs5yolvyAX_9bBrR8dzMeRYlDCKm_CWPNfcRVG-GNhk43YvapNJIhfU7bGVGnXO606NwYrgg8hppZgeFSfdfgTZtiSqvPNBzv-d-FbSV0CbIx1bcEhZ5hXmCF98hEbveVfhmhwq2z-Bag2ia8B-25fbuKGRtsp7sFpcJkL6ngcERrObVUK3SVgtUOGyr7?data=QVyKqSPyGQwwaFPWqjjgNhAIZCHeTxKTJNQdzTP1XMlB93QX7BhHu2y-jKMRlmlAlbX7NrvRrfeJ4c8W7EvEBK1H5xkc5kDv7tLzGJJlmMFGgnTcdlGrIo-0zHbxvw7nv3vXCgWqmGnLQueDEFocsxbumWN-sF5hItoTlIcPKIJs47mmjDpAjE91_5Oah3QRFBUpYCG6ohqyxumQpJnK_tEn-gPFKxMW0DnOqSlfFShUNBnYiyV52va9mQ9EMEgmrOVnib6B_2Q,&b64e=1&sign=7b6f6f174af07e7166da69d76f23132a&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVB2c5UPAPMTakksWkEwoIuJ7eit5IfpDQYQYcXbdguyTZxuISpmOXUEzauL0CL5UpUicp2lJd1KxBkONQydjGNh7j_n4KzClpP6p7wvsresoTjmfZeNE87WYCpEH_-4BdOtUCmcbD_5_JFO0H1_C3x_RHIFQthrAMPJkPZk9-NIhqp3-iYJ_tZm-Q-N3AU0aKu30-kURxuT7LAUKKsAVgM_fkvPfx1K6MHapJjrHloqwfsg7Pcr15MKV6o0gYARDFaow9OxkHSRSAjVjP8W668VUkA30rCXs478Ci3jScyGQbm8O5ClqJ7qhlVHvcRH0dAuv7x0TE2oNCGXn5WXVQW-503kc1VLtkByfGDmJwZUzOQCyzsYnTHTg5zVg6xXYcdlSRSyf0e3xGukRMqdc1vNi7sdTzIb7zTCxj-S_pa_vhPHGdOz2MCsOxMiA73gaZgIIhm7MhUMvjLw0oMdsVpXHxwG1mAOHF5h3KjvqyDjSw7HgmwtlmE-JHmzIdvPa4tj7DDUw5wroYoj3QrYd9_42oBp3ueEajAtZVYyhwKshbX9FBfzc77-Gj1QP04FuIMRC0fvzw3TzIjd03m-snc2DnuiZ7SduwXI2CZhpDlDDSGaedYjkLHRpcYhIOrj1Yc6URwJzUtlzjQHnnA6go8yNt-2EngStEOsERiqh0tq2cQdfrh_GPH72Bk6rPjz5tN48T08SyBdsMo4o7bDcdS4eGqpQa3Rp8NIUTjkd-rNZRW5pIrHEr80E-TqMl01N4fKwoD7_KPp35XSPLGKjSZAj52Uf2rfHNkfObE-2hCZgTge9H3v7FpR_BBHF4_I_-F6CP8HAGmmurx3hCpx1uc_PoT7OY7J2Iv73eBi_l-fd?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2RelyDetX8WHUk9JsKRjrXHCa4OhoQtf6x--fPSzOTiTPaU3Jpg4rRt4cTrmKK1Yn_ldIPjqSW35bHmPPUA8AWbUAki-Ch5hsG-oT-FieMRqKiQV6g3o85A,&b64e=1&sign=1e6f93c6f051b403e8ded48c9896ae6a&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 404,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 4,
                                percent: 1
                            },
                            {
                                value: 2,
                                count: 0,
                                percent: 0
                            },
                            {
                                value: 3,
                                count: 0,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 13,
                                percent: 3
                            },
                            {
                                value: 5,
                                count: 387,
                                percent: 96
                            }
                        ]
                    },
                    id: 243997,
                    name: 'Allo-Laptop',
                    domain: 'allo-laptop.ru',
                    registered: '2014-08-11',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/243997/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                phone: {
                    number: '+7 499 258-30-45',
                    sanitized: '+74992583045',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tDIX58LwFXvM1cZzctn7l5VAhI06y4vsWtYeHX1Z4DMsJ5dzNEx9sUaRpMLF8JVBoReE4oCmT4D-guTiWdoDRuSzUB0jQOwifLbILv1wUN_4ZNVB3aOpfICEylwlenzQDoBY3_UdTcLouYOtFeWJ_1hu3g9Fu3iI51TgxzlMQhv60r9-05QKrZ0huy1Vs1PORpd84_i9_yXj1fnxfJbQ-F8sCGzo3DUlBFEQ79XTwmOR8vp1N-hOnaxaEH0HqCPhbbX3xJtItRGEp5lQUROvRxFkD14_j5a3K7UyzYReUoVxv3IGTHFlNpF56hXNqM01kTgM8AnEVjtXVlfVsgZU4kjOlo4aMsa2rsm482u1JH18ZcjvjzQhzV7d0fTNbL7dBlmg8YZFTaS7DsTBsfBteMTfSf3uj0O2lnKzj1BKqSqAeb8jdfgJy5lEDAQYVD__wq7ltmhyJepYEdN_XA4yqEZhuywuSkqosL3DcaNXTGDOpl6jLNxXQeuBWIxXziTjT716RqZm5hiJdvjTlsDVGnQbtjSya-Lx7tXO5dQN9AeGWJ0pRM0iYLrTQHUUhKSc-RxTzaZQ3HyfSzMR55YpY1QUpmZImNNbEauX9i05V-VQChfw-kMotcTkj1ZTVVIXC9yA48qmYdGvxV0-WlAqyBY2iu2EoFyff853W4iqwnWd3y0cxG8guopq130dpdNOEpZ2PLVoPg5NZQ_6sKZjnuXZFK_mzJI50MoCwE1vG8sXEQaCuMdko1DsGa1ikeQ5Mp0ZmEf6q9bg7C62ZTA6yZFsk_np9o99guEc6ho7RxII74FBimpd7EA_ZuVa6atU72hHXry-iNIxEYhx1XTkK99EV1bWY_d7J?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9Hucx6k11bz_R3AODTjew6u364mBPmbgVuNd5DAqDFnNBFq_Nag4aFdB4YBeT7BVYZ-mEZUQpIYRNbzM4PqEDujTMITmrBfPsMM3ONGLANa5tsz2krUetUOXIn4fQnknkExIschW3vXTPW-zzyMc8oHzkow5g3We9rBcmcR8Tqjg,,&b64e=1&sign=a48cc7e85f87b105419963f9b282ac2e&keyno=1'
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/214785/market_Eff5vhFH4yLfMdyH_NCHNg/orig'
                },
                delivery: {
                    price: {
                        value: '500'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 500 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '500'
                                },
                                daysFrom: 1,
                                daysTo: 2
                            },
                            brief: '1-2 дня'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/lAEyJifOIHSrm6tx0toJgQ?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=7nUicpqaxmdZJEc9ndhDZlby0nanzRWdR_wlFb1JSoOYtu5t9nhgPnainU-p8Zx8Ro91TMxoeEi7b5H8UxPI4_IqWdY0BMmBco9hPfcwBosEsuGVjDw6pOj7FIA6X1IR&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=243997',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/214785/market_Eff5vhFH4yLfMdyH_NCHNg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/214785/market_Eff5vhFH4yLfMdyH_NCHNg/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZF_HjHcY5YgqspXq5TZxRZZ28JSxaUTu7p3PPfc35y5rQ',
                wareMd5: 'OCps3Z1RT-FEK3VegIOlmg',
                name: 'Телевизор LG 43LJ510V [43" (109 см)/1080p Full HD/DVB-T2/Black]',
                description: 'Телевизор 43LJ510V 43" (109 см)/1080p Full HD/DVB-T2/Black',
                price: {
                    value: '20950'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tDIX58LwFXvM1cZzctn7l5VAhI06y4vsWtYeHX1Z4DMsJ5dzNEx9sUaRpMLF8JVBoReE4oCmT4D-guTiWdoDRuSzUB0jQOwifLbILv1wUN_4ZNVB3aOpfICEylwlenzQDoBY3_UdTcLouYOtFeWJ_1hu3g9Fu3iI51TgxzlMQhv5Eo6zeMIhz1rI_uop_vBzjW7lKPMwTOJHytE5uY_cdtKjb-4T_hnr1dQZk9vfJ9OjyQA1vGcF8qcU-ZXEU4KQ3u96niQSJ_uefY8kr9mfupE7QA0QztL-RnwnTHvRKU67qciSlh6BLlCZymaBfyPm6SaJ6c_5a74Djx5Yi3RvUa3y8alsDGvI4HT6yTPl2S6HxSmpqKoLDVCYaJlCnIr_-xeSzMGcwr-sYeiFwgG4ApBRscevy49tauTRvM_e7_mIDlpo46WGK3lA9h9WfNObqlJQRh58FEXkdJHr5nNNGgyMtD6bBNYiS-liXqO2yiwjZoUij6b8YpBVlg813XVJfR3Oc73RhiT29Cin1hJTikPafLD3NOX8u3SoqfSbKMmJ-xdF9ivaGFS2EgRkV55TIdhE_NZNZQ0Sx7xWAg4kcFApZAH-aRhsBXZ3TzscOB7_S8IzGxdHvS9H0Qp-l20U0a0GtnqLGTfvkxrbCRLAT2GeWe0Z1X1abJxcHmkO_ELzph8ju3MjxQy4hrq07iA2ZNSFaBJ4ye1vsLjgUgqUe5VcRyPbMvYmEIUfQu1gQ9VmkCFeIUYE7S5G--_-M-oEXDV21sdO5Nv_H5Bcne7y6OmfGBgzvhCG7iwhNfnt8BBAUkW-WVn8fXCAXr6ivQhLQ-2hNIsMOUEk-GojbUalh9sj2I-DohpiN?data=QVyKqSPyGQwwaFPWqjjgNkEhUsamaWEJp5ccHpl9J5e68OV6TAbYwMRY0XMAx4BvmQoDahm500zi5rKee738vqSG9AMhdyITZdpnasVaAD0jFd5XYbMqX4-H2FhURSTFVF9R86hjSQms-PUT5epy74h57kk5pB1QxihaBLVJopXdYE5SrOY6dQ,,&b64e=1&sign=d0706db870e477c4644e84306ed6392a&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVB2c5UPAPMTakksWkEwoIuJ7eit5IfpDQYQYcXbdguyTZxuISpmOXUEzauL0CL5UpUicp2lJd1KxBkONQydjGNh7j_n4KzClpP6p7wvsresoTjmfZeNE87WYCpEH_-4BdOtUCmcbD_5_JFO0H1_C3x_RHIFQthrAMPJkPZk9-NIhqp3-iYJ_tZkI8Yprijl4ZhZKNeHQP7Cw7aJN9Rs4vgD3mA2QLnrmiZf0E1xsQOytCtDKy3EB9cNrQMrE4esn9s-kOjm_zFZ6jpHsina5ZmdTbZPhkLiE2qu87p5yvqx-AJd6itSwqzUQy_ZMI9pKcV1KQmYsyJZphuhTy5a6Dy9pUbW3J0sL2CdN1IdOO1D5rT0hblKvlgA4K_xDAzAQl1K8VL5iokkVqdBByDI8_PwF-tjmdEiVdGvydlGnhD_mFA1tfLesr7bwiwec1jlqbQj9AIAu3FiI3VmhWbaoi_aUXOzYpnSfwpm_HUcajHpK_9QWfdJgFpvkHMubK0Wq3ohsaqScT0PRRdj_P4hvRxAirKfo1XaTZ0akm-Zty17h6af8ECqEhVawqMQ-L2lpYEjgJWLYwY_O7zw8MmwAUXnn84Yyi2xyDAyxgodmwdigc5cNWly6VRWnH-VJqoS0jZ7CPSiLpZbIrdsjzvxUmmwFfLtYc9s_7I8SSSNxc10THZXIFI54qtxd1kskYo_AL_ViM6uzEd1-8yb-bIXegLU7Avg2JaIzD2gW7CBrC_-hhrLKoowge11FW4fkmLZRycjOpM-L2EU8_dG5oEMMIDJlGb_bW1ZcUtGiJkClrQ7bYPSyjZ0KtEuLQd0pOCcooAkcFxlNKn-UBOVtMwyI2nScX1VbY5xaNg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2VPyfI8ALb_y0P8E4uPxLF1S65dPlJSdUsJbh6dwseRyW1m57-tPKMofrq2rFWUToS-bxYXup-iqz8Q4rpdD_RjQFBekm8eaMZZmwjlXZw7dFIU_NaBLgoQ,&b64e=1&sign=36a83a37a53ac0510bfd1c708cd2c3ca&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 6376,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 272,
                                percent: 4
                            },
                            {
                                value: 2,
                                count: 87,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 130,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 769,
                                percent: 12
                            },
                            {
                                value: 5,
                                count: 5118,
                                percent: 80
                            }
                        ]
                    },
                    id: 13063,
                    name: 'COMP2YOU',
                    domain: 'comp2you.ru',
                    registered: '2008-09-08',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, проспект Буденного, дом 53, строение 2, КЦ «Буденовский», павильон И-19, 105275',
                    opinionUrl: 'https://market.yandex.ru/shop/13063/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                photo: {
                    width: 464,
                    height: 308,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/169986/market_-25bVqE0YBx22s7jpSMp4g/orig'
                },
                delivery: {
                    free: false,
                    deliveryIncluded: false,
                    carried: false,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: false,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'не производится (самовывоз)',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: '1-2 дня • 5 пунктов магазина',
                            outletCount: 5
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [

                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/OCps3Z1RT-FEK3VegIOlmg?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=7nUicpqaxme_2luW17L-BcYG1sH8qzrDEbYCvQT5zsdhIRvr_IkG9op9gm5Xc2F_UTh7yG6FuvETyxUEm9fO8t90wsUHvUAIsACnL71_-wTHB3ChZXThLf8bziLrbgq2&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=13063',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 464,
                        height: 308,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/169986/market_-25bVqE0YBx22s7jpSMp4g/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 126,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/169986/market_-25bVqE0YBx22s7jpSMp4g/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFPYxXEML_C16j_n-7zYUmPOpvBvVjrikcf2OChXmlykw',
                wareMd5: 'hHR_DL7b7X73vJlP6MQTwQ',
                name: 'Телевизор LG 43LJ510V',
                description: '43", LED, 1080p Full HD (1920x1080',
                price: {
                    value: '20950'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tDIX58LwFXvM1cZzctn7l5VAhI06y4vsWtYeHX1Z4DMsJ5dzNEx9sUaRpMLF8JVBoReE4oCmT4D-guTiWdoDRuSzUB0jQOwifLbILv1wUN_4ZNVB3aOpfICEylwlenzQDoBY3_UdTcLouYOtFeWJ_1hu3g9Fu3iI51TgxzlMQhv57GiOH5Z8iRdObp1iX5VPZDD_-UBPqgfS6n-zVXfEYhgLnHJw_VDZrBD9zbWM8lWTDyB_7g64vzZ-e8xCmrClX7cFeNhwXVH-urxlkn27-0FT-LeTJZB9RXd9SlsABVsnNLoSAxr5zYVRr2xxCmBA4TPcRDOSqB2BuPcmQbee8ZmXjbpLC7m2rNEqWp87dIecxpW3s7YH2yUTdb32sPSP4tzXa8KE6D_iSK5w5obb_SJ0VnNw6Rs89d-r0uEQb8CHtWSLYZe9158ss-2M8_8eeOwnCRjmMs5ZxRFc0gEdACQFq7D_hIbvW_A0jLqhGxKlGQcSV4krYa01lAdl836Paz1IstFHU2w1BYaNDEoKtb-IdYxN2M3wZCpQnkBmLJra0Jq0YDW-ZBJrPh-mUI5gjx7xXRHXOlXLKRwKQOND6nS-MG1gf7p7Wm8ucJCoHj6rAVyWNIOpyui7_uN_5DyestrOMkXtOLAjGoUU6mNeKPE1d76vEXiah6Xrjv3lcMLwVHgAzb1MxWR1FR0qrA4A0NjdAZALNM2Y-HgjMN1qJX8rAE0m1cecAVJQ-dU6QtJ7RoH5C0vnpwDscOSlrbYHisUcVtMuMiHYTT_PKX-Hcd4y0Vt1dztG-hL5sClBG07QUc-R248ICBtazQiB1evV-xXH8WTu1UAgz_Acn7E-7GMnyrzcFmvw4?data=QVyKqSPyGQwNvdoowNEPjffWP94VrkTQ9U2e3-FfQRgG0PvEWo6dvno4k5OpDaJPG3PUau59CEqArLFcRZA4Rgj_6fbvfQMv-J8ckj5FVu1GexWg8q3gZUbXHNoAn7fStUM6ig_b0zeQJTWfI-T5q-Ft7eLQC3a4OZgKg2NasxTVGGDwA0onnvMxzAGlfZMoWFVfcWsOqy9ZGdJgiYchrHBMxk_MV2USupIjhT5YMllpUuWyMtpDFlujg43lJCQVZ8aX66GBFTIIyKMz5to8epKTLYwVjS1FYob6De7p2LNC75eCmiNBZU4R_TZw_4k5RsNXbGRNqryCfGeNjgGGqyCX5msQwq2JJ2puzn6ODSpwvvXBfVZJduEJYCD3D6Xhzy2n4Mee3g08-2VjUM6JAz86b6GjBb98aAYBhdNQFd57m7AMkWA3g521gr865HEy6TCrQeN_RtDT5Eq926YJWLD2-uOt-8VNtkt9OvQC3L_noiC2JNV0d6D-loPWRpd6_keg8KqzvMSNaAhl1Tf7q6ZZvnvGM703oOzk6ZeQDF55qIcMGf6KlLB-FywPFEGP7FT1p_2RShI,&b64e=1&sign=4268b39c783baaaf05637d95cb6e230c&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 6739,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 94,
                                percent: 1
                            },
                            {
                                value: 2,
                                count: 38,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 66,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 476,
                                percent: 7
                            },
                            {
                                value: 5,
                                count: 6066,
                                percent: 90
                            }
                        ]
                    },
                    id: 6363,
                    name: 'BeCompact.RU',
                    domain: 'becompact.ru',
                    registered: '2008-02-20',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Орджоникидзе, дом 11, строение 44, 115419',
                    opinionUrl: 'https://market.yandex.ru/shop/6363/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                photo: {
                    width: 795,
                    height: 518,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/250421/market_t-3NmOdmiSHgac1HQ-KFPw/orig'
                },
                delivery: {
                    price: {
                        value: '500'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 500 руб.',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '500'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/hHR_DL7b7X73vJlP6MQTwQ?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=7nUicpqaxmcR6Glkf0Xn9Jv7VRPm2huu41ZPhLnWKG6KpBiAHyIQGfblw0aIh0m1q3g-svwGyWlkd-POQzSxjs-ZhsXUfpIBki6q0vQTOiOY-NFtIBZ6TZYIoLNvu5cr&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=6363',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 795,
                        height: 518,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/250421/market_t-3NmOdmiSHgac1HQ-KFPw/orig'
                    },
                    {
                        width: 636,
                        height: 624,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/245913/market_fW0ws2wWphZxnJ9MMst-Ew/orig'
                    },
                    {
                        width: 780,
                        height: 537,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/245913/market_5AliVoQeJ0Z0yiG_xrsVUA/orig'
                    },
                    {
                        width: 754,
                        height: 521,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/406938/market_ph79SmttxncBhIOkh6NPYg/orig'
                    },
                    {
                        width: 626,
                        height: 621,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/363663/market_yHTDnz30HSTWW1ke0TgQdQ/orig'
                    },
                    {
                        width: 966,
                        height: 217,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_lWtOh7zM-OCtI4Dx62_M6A/orig'
                    },
                    {
                        width: 251,
                        height: 719,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/236212/market_lSPnLy62z4thYmSDMepSKQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 123,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/250421/market_t-3NmOdmiSHgac1HQ-KFPw/190x250'
                    },
                    {
                        width: 190,
                        height: 186,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/245913/market_fW0ws2wWphZxnJ9MMst-Ew/190x250'
                    },
                    {
                        width: 190,
                        height: 130,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/245913/market_5AliVoQeJ0Z0yiG_xrsVUA/190x250'
                    },
                    {
                        width: 190,
                        height: 131,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/406938/market_ph79SmttxncBhIOkh6NPYg/190x250'
                    },
                    {
                        width: 190,
                        height: 188,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/363663/market_yHTDnz30HSTWW1ke0TgQdQ/190x250'
                    },
                    {
                        width: 190,
                        height: 42,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_lWtOh7zM-OCtI4Dx62_M6A/190x250'
                    },
                    {
                        width: 87,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/236212/market_lSPnLy62z4thYmSDMepSKQ/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZGN_9Bou2XAOOWdUF9HWZf0Q3SRteXQX7YZb3P5kopJ6XiQPzRA4JtuTxbPY0z4tKSdamg3amOYlAJR__VOOFDJdCXtJM3VbdU6MArUVmzKN7IyNq9wU5adMVWhh_CObnRfoBw90YYD6A0J-Kw_EtQiKqu2zlWP5xbXT66aHTIWjYdffFJuSaTqDavXf84KVIbWUGawpw0FIqroT5_6MGrshgrBJJT4g7k72HW2s0PCsiqMt_VNqSUeNlrISJ9u4vFX_uh4RaFSnts-4DZLpSL4QDfq-7uE_bU',
                wareMd5: '00v4a9DLkvomO9--msjCwQ',
                name: 'Телевизор ЖК 43" LG 43LJ510V черный',
                description: '',
                price: {
                    value: '22550'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tDIX58LwFXvM1cZzctn7l5VAhI06y4vsWtYeHX1Z4DMsJ5dzNEx9sUaRpMLF8JVBoReE4oCmT4D-guTiWdoDRuSzUB0jQOwifLbILv1wUN_4ZNVB3aOpfICEylwlenzQDoBY3_UdTcLouYOtFeWJ_1hu3g9Fu3iI51TgxzlMQhv6GZyXI7a7qHrKk3Zerle5AK_RslrvUYpYaIusx9VlZurInHq2Ar-0dEMtAmhZ_G6nBJIL56ugyZ8OZJ1UuimMQN_zI7vL0yjxTMyF-RV-roVP_sWDjvHHBcpTIsRaDgkTdrF6P2nWPB9Yo8ajb-kQak5ZpOQgpLgk0ipMD-UEKrkDhnRb_LJzy063p5oOLeTiYlvwcI8fDRdDesNFO5NsYP1EIC_EWRMVRmJyHnAN6uhjdqINOr78xsMgnwRD2Bb0JxPWQH3afeiEtFYfw1yhH0SevNVJP11hAI25dXIZkJWqu5gIChWGiU62vqBQ7oQZ5awS8FjLVOr6205-cY0xlqSmkr6iO-6DRcDt5f6fT2c4Q75iZQ8uinxzdcMGonM3QQ8pIvZACUhIKSSz3nbrzIy0P4OWar5kOSPltCnmydr7u72PM2icjh55KqpwEQm6v80LHi2I4kFwnqmm1P5tNWnepUsfiIFyJcphYQ99tE_HsBGuimy6QIBGMScxlbjoICg9K49fzWtrBCMlIW7PpPWbTqcqZCjNr_MJSEKaS2doOnuNsKsldPPD4R8VFn0fB02FbtWyzJHbyxg15xMZjYntiKGHOiJHeER6whFX3m7G-Dq_cwJVuJDvhMqV58E7_NLCOfqoiMQLKOH7oloPGAjcGdiHksrcUTFQkC387W7p8suCJmhZu?data=QVyKqSPyGQwNvdoowNEPjY191n2rMIoJFrdGhIFCdUryTGF-tbt634mz7hSslcch5pN5tdePhY4Q7xQ4BNJuMsXaHSqwEDd8KK0M6nrzArSChi-Amkkt0RI4eCHGHCz1jghOSrxuWWsjPlsrONagiXtxmV4isYRLsGWnjMp8laatlrdhHbjvuu9KSTqjlhs1JtP9iEdmWUeRsAE20j6tBJr52HVnNjKsJtJ4kqqNCMVQFvEUnihkGVvhmoc3SSkOtvuQEsOz4foBMsVLvNG_AEUAGpGOZNllSkDZUW6MY71Bky2k_LWzl0Z-0N21O8k6xMCHUZNFRI_QsXbRchUZWSeKehfkwvK-2g7es5hKDBASt3mXcaMST6vlQe2KNF94CHV-2LXbC75NW4TWXbE7X9mvYLvdlD6hfi8pnBTXDYg_a9e6I9g_JSmqRC2lO6nfS99zVTSwo7wKWqPGhT7Mw3nrbRQVfdd4OiIpJwokBHzMXzsO7G65V4zBZn0e0CDh_GgEjh2PfAU_kP4r2kNDNFuVNRmUJESCbAeT6GgmbJBFoaCSPHWB8MqzhGXTEvQEVRsKmHdQ0zWTQugQiGlLb7pYFayVmQIt_iC3afPiiOC_tm7qfYrz8kMDKgDDZ4vXW2EVPM8jGPATBdKLD-SRbgYfvO-fn57XDgY8hF4gY4WBdkzzfANUjw,,&b64e=1&sign=5258351f7602afbc171a0c617463c63d&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVB2c5UPAPMTakksWkEwoIuJ7eit5IfpDQYQYcXbdguyTZxuISpmOXUEzauL0CL5UpUicp2lJd1KxBkONQydjGNh7j_n4KzClpP6p7wvsresoTjmfZeNE87WYCpEH_-4BdOtUCmcbD_5_JFO0H1_C3x_RHIFQthrAMPJkPZk9-NIhqp3-iYJ_tZnz_d1rBj4y-ExPdaKWsKuWpGL0eSeO7Cjxduj6hk95Nv7IP9d7UpoV2SZstPNIGyNktxrrP2A0z8rvGHZOfv1qQzWpmAgAf5H9kAvqTgRXi0VAbJspB6PUNnRltqeI9R0OZSS3PU0lor2gYv59tBWj56-yXoHpBXiQWKDmC1x-E0sLTK87dN8ppPycMlae3m6b30zx-nCVbPnFvAej34Ka7l-UsM_2NinhPv6pCQVfuOWpQkrHnaa1hOML9L22E-ayIqPRtMh50sLH2oAnwKpTkGadCUn8cZapC3l_09h7RqQI1qDhrMh4-MfKtUvSds_Qfmnkstdfs4CsIqU83eS73ddbEArTAQtNRmWtZ0A_nyKpJTt5Gz_k_g98FtOsxUWI2WDR0g9mkiT_2qqeIUBGQwCgNqlrXZlTXGQUZbjsWb8fNdMgLbYYKus2WMlRUIVQIpdQiof325k1_BUSVrcH9VL1zIPJaX0tNg1woHv4PyfVd5vhQ-xFt5Cf_93rinRgrGGDZkt0cLqwCSqPacnlecSgZ79o-DmzbDqqYQQH6Bw_fjinc5cc-6GHiRq6PayX6yY_iVAIjYeVrEIEzM8sJSs4UW0MUkjNuDC9JcSt4Dpj8Eaxf0hlIknlkdcGk0BHc49NpGYjDrn4uOMRWH3o_zmy87JP95CTnfBMSKsIMfPPqnb0IvH-?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2ZXnLgF6U_pbuImY4O5mnN_kB9dShelvWDCFYy7uGriCrCjLCvU4ao-Y6yXeJoGgJou2zkVcYXmrPy2-rhk5B5m8n2qSv5TbmT4yJcABryyd__qngjv3k3A,&b64e=1&sign=509ea66f1c824436e4f9d1cbfd4c8102&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 2218,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 228,
                                percent: 10
                            },
                            {
                                value: 2,
                                count: 75,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 60,
                                percent: 3
                            },
                            {
                                value: 4,
                                count: 214,
                                percent: 10
                            },
                            {
                                value: 5,
                                count: 1641,
                                percent: 74
                            }
                        ]
                    },
                    id: 40885,
                    name: 'ЭЛЕКТРОЗОН',
                    domain: 'price.electrozon.ru',
                    registered: '2010-06-11',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/40885/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                photo: {
                    width: 700,
                    height: 700,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/247229/market_VbLBbeAgCFgxsedtL30DbA/orig'
                },
                delivery: {
                    price: {
                        value: '280'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 280 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '280'
                                },
                                daysFrom: 2,
                                daysTo: 3,
                                orderBefore: 13
                            },
                            brief: '2-3 дня при заказе до 13: 00'
                        },
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '450'
                                },
                                daysFrom: 1,
                                daysTo: 2
                            },
                            brief: '1-2 дня'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/00v4a9DLkvomO9--msjCwQ?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=7nUicpqaxmeavJd6kOT4V_Xk7nhzGX-I9FTsNDhcoqUyvmQITvzSdXu6vJE9Ob44C45RvDeBGkPPeEC32MW_8w7Mvbu_F4Q7lbXXcmtLW5LE0ffBwRJ03B512hOioHQQ&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=40885',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 700,
                        height: 700,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/247229/market_VbLBbeAgCFgxsedtL30DbA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/247229/market_VbLBbeAgCFgxsedtL30DbA/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZE0jzVBcHv12s8TkwL5vDDXP8-snD00MzUofJ9JQQKYTw',
                wareMd5: '__hr2_Sk6se7oMndLo-guA',
                name: 'Телевизор LG 43LJ510V',
                description: 'ЖК-телевизор, 1080p Full HD диагональ 43" (109 см) HDMI x2, USB, DVB-T2 тип подсветки:  Direct LED По-новому глубокие и насыщенные цвета Помимо улучшения цветопередачи, уникальные технологии обработки изображения отвечают за регулировку тона, насыщенности и яркости. Революционное качество изображения и цвета Разрешение Full HD 1080p отвечает стандартам высокой четкости, отображая на экране 1080 (прогрессивных) линий разрешения, для более четкого и детального изображения. Улучшить изображение? Запросто!',
                price: {
                    value: '20850'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tDIX58LwFXvM1cZzctn7l5VAhI06y4vsWtYeHX1Z4DMsJ5dzNEx9sUaRpMLF8JVBoReE4oCmT4D-guTiWdoDRuSzUB0jQOwifLbILv1wUN_4ZNVB3aOpfICEylwlenzQDoBY3_UdTcLouYOtFeWJ_1hu3g9Fu3iI51TgxzlMQhv7nCrQMLFbJ8szJWuQ3xP5aPgGCx6bdM9hvhooPlCmB-aM7IYvzDW8eBMaqWplFzyXoWdhX4AJ2ckjRdYNQXHYHjzpAH3A3RSuFhHO7OYZMHQofxF9CCSiDTzjJksqxnnV2E3EaBQQIeGwBFz_JyKdCXVO5E9UFW9LyaLkmMOYX65qgQXU8VqYSKOYBFqHerg5Vd1CJ21HFbJAGoviThdxua82eM2W9jITLvZhu9g8dpgAuau-IV542RSKMsfbM1A1LSB3FWd9hDTSSitYuKx2CeqRR5amILUu9ESeYFVwJIvRF851AiLHdicKHBL9gmKCp5nEIgLycHOIZvrLQ0fFLhfoQhZ820vuas1-QbWLumXSWIIXRjPveJhLcWj6WtjTY6VFJLMRXrIkRbk7MjHlzGxRngFePs3i0lCyWBegBCR-3k5jDbZa0OEG5rxMHSeuHtUuUigtoyzMz0idfjG_XlGSKPcQ2fnY4ykyvuhbxbKcRRqmvA3k9VX3soWpWQd0vid4fNuFUkpaeiHkCFKzrENuuAjFPRgQykey7ZkKxnS6-XcTClqstm_BZqfX9Y7TgPFQx3eShOomQwKaycm7Q4plpoROdlEJyZsPvom-bDB5nWyFwZxP85lgtbaXvH0ua8hPOqc-QuJd-AzsvSmokm2sISFuuc6nwZMLiEu0yYQ,,?data=QVyKqSPyGQwwaFPWqjjgNoIi1mBHeYelRjFR2wDOz3hLJ3Stgg3P2FreoZzgGSmcdXWYkuhe2L01PhEPvUrpK0RJwWv-NcSKT4QOvWPgugCb5dzz-OnhCpPgdJbgQnAS2J4itPnQLEomJ_KfCesS-7NYEoi8fYgxCSy7qW6q0SdWY5lPI3iAVfjPJsbimIQmZ8I6Tv9QvYIbvNIFVQ1_019cGCbeF44PI7qBsiQFLZCPuCKge1ccIf_uvZcqpojH_bAXdhpWQvRv1-ORZzuO4_z46sLYOG8Y&b64e=1&sign=7fa930a33c2d1475b5f045cd81df29b9&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVB2c5UPAPMTakksWkEwoIuJ7eit5IfpDQYQYcXbdguyTZxuISpmOXUEzauL0CL5UpUicp2lJd1KxBkONQydjGNh7j_n4KzClpP6p7wvsresoTjmfZeNE87WYCpEH_-4BdOtUCmcbD_5_JFO0H1_C3x_RHIFQthrAMPJkPZk9-NIhqp3-iYJ_tZkt0Dflfrb18zjCmem0OLTClxlEvrcqQqjopqKWSkvN_fEa2Xb-0tKFdnuqNhu1mF-FlaxyIsLg5wZsl3DulcftJFdjlPk_fdZs4NsO1Q2CllrzQLie_yjG36VsKUIjenlyh-TC8O_l8AFYs4D4HAMgt5rSKlw243a5EuJhA0YCCZYH0VXND8PHXi60xLG1q7M600f9r4-c-G2ofb9IF64IkGsXFhgw4-8rv46RAM8tSYIYvmfpMJ40Yy7hV2wpYya2KxWvFfkWO1d2xsWsjEDw0-Ke93LDcs7Bws1H_tESH9sKBYVHCFR9bNCnbyeC3deOdAeJAM8RxYsgBNuCBKfAuQcbhKZ6GlzL8reoa3WCQUhiAzTyso8oXVh0ZcpabgR4FwRhT-58BpAOkzu-WZHvOkZUxMV6zuHn59-M0vWdBbNLoSKpakCMaTIDT8dCfCmwfHz80iH9HD_Jzkj75lGk84P_Klzuz0UQVA1xHFoKN88bhoMRyPdOPmoH8UZJJx0r94SKYOHmMmgBHHtjORYoA0NMmoisRd5dwY3C3L8wkJtXyj-1Zgg9RYKXgvUWjP4N5MgHkz6eoDcLSajLBoiWDWhO5c4iFapivb5W04RiGdXwf7t5l1b4LJCyupKI7PheTH8f5Lv5fFYBGqB5xV9fCmpqAmsb7brkSevjYzdplg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2WTvsRDBOl8gc9yLAfJsGVpBal49FyVd_PkjxZBh-_JhDc9izHEIRlXfzIINr2l_GerYvKJcVx29_DGofapTJnbFz3IPRcIrIjRDqDS5n0adt8FWaHfPJ9g,&b64e=1&sign=bac44ea5e247b2b9d83a91a74317fadd&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 845,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 33,
                                percent: 4
                            },
                            {
                                value: 2,
                                count: 5,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 9,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 38,
                                percent: 4
                            },
                            {
                                value: 5,
                                count: 760,
                                percent: 90
                            }
                        ]
                    },
                    id: 5478,
                    name: 'TvTeam',
                    domain: 'tvteam.ru',
                    registered: '2007-10-08',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Барклая, дом 8, 121087',
                    opinionUrl: 'https://market.yandex.ru/shop/5478/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                phone: {
                    number: '+7 (495) 7963143',
                    sanitized: '+74957963143',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tDIX58LwFXvM1cZzctn7l5VAhI06y4vsWtYeHX1Z4DMsJ5dzNEx9sUaRpMLF8JVBoReE4oCmT4D-guTiWdoDRuSzUB0jQOwifLbILv1wUN_4ZNVB3aOpfICEylwlenzQDoBY3_UdTcLouYOtFeWJ_1hu3g9Fu3iI51TgxzlMQhv4R-b_zDchUR9xJKVBqjzAjNv1vwm-RA0ZbC607qo9WoWET6D8Ua0g8Tvg5Xfq3aI1zOsBopTdPrf_qV3jlu0x8twBpMBjO6JtVvt2VmWdQMM3Jrny3BkxFiTEJ4vACGGbmwAaWpKAITMIJihCEAfZW_fLvVq7JhrM-enNkx4ihIbeow4dj_VHQMLDd6_ept-m0xe4PcO4-9bvy4VqJCpnLXTKMoSK3uOx6ahBPScTngGMR6NVnzh_G2GwsiktAHq-SQYozsQLN9OsXauahmhxdJK6GHgjeuwt71bj6yCQ3sAH4X0ztQ7BWksyj2f2YHYIebEcFT2z_OMHAX4nr0Dsi3-E8cQoGa1AyNfYhuPZq8LzK7TsdYrCNxpdxKQMRkiCCmdLhe3nAZ5DuDhw5EIXtSyxLeVLk1asDcDMN4LNNgoaAXcouw8EdOA95RA4kKSNYiWuEmKHv8nmf2dMVjSqdVFJIEPJRECXsGwbDTApz-U4PuV7EUE-kKhIpdRhyqDosjZGC06wOxLG8ZvsUACX384rHQ8GQIWnm01GOG2kKvNecwuqc9URTCOIKiKcnnu-SFgis8-bncAaowu9IKJ3WWoMXSrfNa1F2odKLwbGCW9kxfr7UXWt0E0fH9TBlfupPKC9mGBS9-61ypiuyfFbq75HKHcyE4Qu_q1VmbhNvxg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_Ip-P8TRe6MQr8H5OaESXu148BKhJjWwxhDRNSrq2prr43ZFmNk0EZFrFczQYF1V4Jnd6APc4m8bjuTqiKsZCZYnm07mHtLPnDI0E07tfxRFNmysGAahqF_-hOnMbnpUh7rNnJqnodZdHRmU7vm-eOa4TAQ2NO6OfsSZoYP_AcCQ,,&b64e=1&sign=e008cde9e96a8979d916070e5f61c955&keyno=1'
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/477791/market_1lb4xRY15u3DGO3QmfsFyw/orig'
                },
                delivery: {
                    price: {
                        value: '600'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 600 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '300'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 20
                            },
                            brief: 'до&nbsp;2 дней при заказе до 20: 00 • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '600'
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 13
                            },
                            brief: 'завтра при заказе до 13: 00'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/__hr2_Sk6se7oMndLo-guA?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=7nUicpqaxmeTWTFc3ic63BuuKqSshAmQ_RiOZlM9DBu02teDHUMNK7XJZTGvbmXB1p1gWKQnGVHt1bthPEHDgfqDwlys3mVY_M6lb-XLeZ18Q5B19M7A6bnVDaI02acv&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=5478',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/477791/market_1lb4xRY15u3DGO3QmfsFyw/orig'
                    },
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/172323/market_TvV5kHKmybxHEcWBSNalhg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/477791/market_1lb4xRY15u3DGO3QmfsFyw/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/172323/market_TvV5kHKmybxHEcWBSNalhg/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFXCr5IIML29sVF2VtLtxoLdQvF-RS_vi1ipc3jTzL0qw',
                wareMd5: 'p2f8XebXtjsklAhuSo9vmg',
                name: 'LED телевизор LG 43LJ510V',
                description: '',
                price: {
                    value: '21450'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tDIX58LwFXvM1cZzctn7l5VAhI06y4vsWtYeHX1Z4DMsJ5dzNEx9sUaRpMLF8JVBoReE4oCmT4D-guTiWdoDRuSzUB0jQOwifLbILv1wUN_4ZNVB3aOpfICEylwlenzQDoBY3_UdTcLouYOtFeWJ_1hu3g9Fu3iI51TgxzlMQhv4fPEekL-65S2geLhmF8bhwCFbQA0gXpgsIKuAcwlmThZZA_MV-zPcAAoTFcvpXbVRz1n0sWATAxCnfXl24bLXnb5OLhBsEQiVkGbKGJnWzvGPpKqIqsQ7Xzyjh6OYNJfkh9UeMBO1nMVBCzwT7h3qNSAamXt5Ddk6zJh7LUe_CQbK02O2Zq4kKCif7WsSHxGM2QUTC0cQBVkgctv092Xt4_RIpbUw8obWXmpX8WI3YorIcXj1kcwO4NLGmj2RlJpGuf3Z1PPduOkr_32cdlgQcCa5u4x-xFwIT4p7ObKJkDyu9zFHBNWPsR3bv1v6LcpNMECP3HXwaV3P_3l6kBG7CGRPFMIN6VLYBIEd2Ne1WRsRq7mKXoTaXjLd59hOL13KdXmLpN-TMIjtboPLQzVRa2LUhnxWG4eRExLJvkbzL49o-dlMe54Mqj2vYxts4FhpI9CjZb188CE6FY6yMt4KmJ0O1PNpXnoUJP8aKu8CE6rG2zqusY5-QWJtTxJ6yLQ88Cww7VxW27abRdzx9QlWBcHnbXZW0ved7w9MuHfGrwECG7B0Wyytij4aWFj8--jNj0XwBMZxqmtdZyObalKuSLfkplMDVGhyTs75GMDpqz36dkj9gb7WV0uA5Da6TwkyXnxhzGR2U2SqN-RREXgi0zkrY_n5_2JGPH4iub2zFEA,,?data=QVyKqSPyGQwNvdoowNEPjXwQWWrc6tY3M-8eUN76dHp9h8Ojdg3UhMMfcXn2T-5TZhHWZYzTyFQxVPbeJYkoUC_Y0gYSLxznh21mgFjIGroKJABURGHtDorJY0Pu98fHi8CI0GzZmop0VuXPfsFPPxV1EcRtMMQN&b64e=1&sign=84737b696cb56913b1af817940a672c4&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 215,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 3,
                                percent: 1
                            },
                            {
                                value: 2,
                                count: 1,
                                percent: 0
                            },
                            {
                                value: 3,
                                count: 1,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 8,
                                percent: 4
                            },
                            {
                                value: 5,
                                count: 202,
                                percent: 94
                            }
                        ]
                    },
                    id: 5476,
                    name: 'ТЕХНОВЕД',
                    domain: 'technoved.ru',
                    registered: '2007-10-08',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/5476/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                photo: {
                    width: 200,
                    height: 132,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/167181/market_UdS-JOaW2I493fF2mcovMQ/orig'
                },
                delivery: {
                    price: {
                        value: '600'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 600 руб.',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '600'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 13
                            },
                            brief: 'до&nbsp;2 дней при заказе до 13: 00'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/p2f8XebXtjsklAhuSo9vmg?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=7nUicpqaxmdu0ce1aDbqyNc3K9BqOz9FAtWJ0Og7lYhJnzme-9uPSjSX-es-OvLF5YaFAe3iH0iiJKu7n3z-AL-OMlxWgAXr4O9UlY7cNwBoUX5QP7GjU3wDOhiiCSeR&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=5476',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 200,
                        height: 132,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/167181/market_UdS-JOaW2I493fF2mcovMQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 125,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/167181/market_UdS-JOaW2I493fF2mcovMQ/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZHrSFU5tKZ16rN40jAVyHDosW9XG-Q9soDB19KUEDAhtQ',
                wareMd5: 'CPDYWEHwXI2YRSFOhZyFWg',
                name: 'телевизор LG 43LJ510V, черный',
                description: 'Тип:  ЖК-телевизор; Диагональ:  43"; Разрешение:  1920x1080; Формат экрана:  16: 9; Светодиодная (LED) подсветка:  есть, Direct LED; Разрешение HD:  1080p Full HD; Стереозвук:  есть; Индекс частоты обновления:  50 Гц; Год создания модели:  2017; Прогрессивная развертка:  есть; Акустическая система:  два динамика; Объемное звучание:  есть; Декодеры аудио:  Dolby Digital; Поддержка стереозвука NICAM:  есть; Поддержка DVB-T2:  есть; Поддержка DVB-C:  DVB-C MPEG4; Поддержка DVB-T:  DVB-T',
                price: {
                    value: '22240'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tDIX58LwFXvM1cZzctn7l5VAhI06y4vsWtYeHX1Z4DMsJ5dzNEx9sUaRpMLF8JVBoReE4oCmT4D-guTiWdoDRuSzUB0jQOwifLbILv1wUN_4ZNVB3aOpfICEylwlenzQDoBY3_UdTcLouYOtFeWJ_1hu3g9Fu3iI51TgxzlMQhv5TY05aYvqJRyNX3NHqa1CRQW13yn5FGTeZlMDxCuT63wb3BpkNTzW6b2OHrVyey9LKkuMyY63p-97BeW38o8bFwSqO-gvp6qxSHDyZMd2I0ssQ_FMh_3s5TSGDewLvYf6F9Q3UpgeKeHSmW9gdqR0Cj-Piu9Uqm58NTAbL9fH4zmjxSBchFyMDpI9Z1HcKo7p2af12-Eg7W4mgpC7qZ0r_yFr0AWrKutm_BAyub-z0qBVVQ7QAbcTAnsvgB4ogjwlA1z_LrVuWQD99grEoF6UfS5wijsZAu6k-_HNjXupqffmcthBEJ4dtVenZl6kxI9Ch66b2W8uoCMmZiiZBQbP-LoW-p0wg5sB0Ne_CvMo8TYbVJukUqxberJZTStBIMiIsb3eOF3NgknoNttj67EaBuVgzLUOpuvpDHuApfeEnTrN2ueh6YD0k-mBcjJC8o_uiJf5miM4-YakyHzqxxs51NUD1CMWPXzY9XcVI3ps89Qot_gAnerSd9HlQVQZuv_9KCxuclIMTDovAmJEQ02GHuv3pSva5VubxKeG_54K3yi3Vou_At78J_YpO0GuXj-hk8Nbyo4UVhLnwQjOdgYsBXRfm03w55AQAGGx3bC0u0oDrFSZdpcnamNlZTbdS6GLETmWR86ZPH8m8B1dyBUA6hZED8wGrBKIhcMqYczpGDRvhJm0jv6aw?data=QVyKqSPyGQwwaFPWqjjgNtwFDITzDmMA14CpEWWDnAJ7k9s4UesbEkHeg4YesxsHL8_09qt-Dq_p8EErXMVWTW_ky8O-lGhQ_1JPOEybnlkE8yTskq29c19P_bPHOzYPCqnaE-RBk5fIOOs2gdHpS76kTbIQDs_oMWnZm-q1DBgz0ria3hudbgXmEXpRDa3cgdIcqUveCJlbAmT94V2ZCfFO7vfPC1Y6NSa7nz61upYpyz0BVlMvg0gpK_gZCsJ_qDVImUMav_v7i91nYFbc-HDA02nyaZUr27UgVVlYQzKi5JB9nkyawV_YJ5_sx5eZpej-hSKo8xjRbanBi8hf6hMSc0dBzN4kF9wdwertmqM,&b64e=1&sign=4fa66a662d96010572f64b94037b8436&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 4497,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 240,
                                percent: 5
                            },
                            {
                                value: 2,
                                count: 91,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 92,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 535,
                                percent: 12
                            },
                            {
                                value: 5,
                                count: 3542,
                                percent: 79
                            }
                        ]
                    },
                    id: 25017,
                    name: 'CompYou',
                    domain: 'compyou.ru',
                    registered: '2009-07-17',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, ул. Трубная, дом 25, строение 2, Прием заявлений только в личном присутствии с паспортом, вход с обратной стороны здания, 127051',
                    opinionUrl: 'https://market.yandex.ru/shop/25017/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                photo: {
                    width: 500,
                    height: 375,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/367259/market_Xpt_b0d8FWOoH6CzNXSWwg/orig'
                },
                delivery: {
                    price: {
                        value: '300'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 300 руб.',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '300'
                                },
                                daysFrom: 1,
                                daysTo: 2,
                                orderBefore: 20
                            },
                            brief: '1-2 дня при заказе до 20: 00'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/CPDYWEHwXI2YRSFOhZyFWg?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=7nUicpqaxmf7bVeTEm8wB-ntNU35PCEIYP5OOrBRd6-otOShflGMkzth2lzq59k9hYyFO2Ee4lgFddn7vuDUizTCRNEE1nOqfIjmy2znizpTFATVrrn_3HwleyjQtglt&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=25017',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 500,
                        height: 375,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/367259/market_Xpt_b0d8FWOoH6CzNXSWwg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 142,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/367259/market_Xpt_b0d8FWOoH6CzNXSWwg/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZG96HGJ-l0giBNtjW3sGpYlllb4TIn8my7j2G6VVDmo99fWtCdiLgOex0FCmbk8q81YeQxuXgoWGjGrsabGLTH7EpiM0sFHBqVUjLqyb0fQfuhOCDfbH-2v_Dk0JoAusIfJY_vjs9SZsG-Y5uwc5lrMVwKMKNquBa5MOLcTv-sjvEBz3eDVAUKHIWA-9YVV0KTQGwMYt70MG4jeV1FmOFOKnnmqRT8uGbDeii2yQopV0zR_RgE8UFa4oPCWcrdOjzbWYkRQa4Kroocra620yUuhoDJqPfDNZRY',
                wareMd5: 'NHFNwRN-P4rE_C7Z5CMjyA',
                name: 'Телевизор 43" LG 43LJ510V (Full HD 1920x1080, USB, HDMI) черный',
                description: '',
                price: {
                    value: '22490'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tDIX58LwFXvM1cZzctn7l5VAhI06y4vsWtYeHX1Z4DMsJ5dzNEx9sUaRpMLF8JVBoReE4oCmT4D-guTiWdoDRuSzUB0jQOwifLbILv1wUN_4ZNVB3aOpfICEylwlenzQDoBY3_UdTcLouYOtFeWJ_1hu3g9Fu3iI51TgxzlMQhv53gnA2WwNZjleF7sO2G1rPCkFpdUJjOUKO_ABdmPgkAPsn-af0Pvo_0uKkDQ7n99CBwOV5NKwFKebe-G4kHqj2VPtZha67YsLI_6aSMc04FWK2_jdiMPjqvTNwEwXVwecwDQMetYhO3QZeo9EKNm4dEHXvbABzjc21ssQdPk2Jzg3yuctj5hHKjUClK__ao1mqm03iDvepIDBBkJIt8DaJ6-34yoMqy3ndxLdwro7t8g9vgD31rnjXxAj9KbH3mg3oHf2wONb_tjgDccezs0gStCwMYtWqRA4SvQBLdz60IFBBNXJG4C3ly7uqJbd-2hpkXW9pR643rLpPlE6ZzmO6ltOe1HFn5JMb_Gf0Oi2ngCF-wvbD6ETvOfbzykFN33C38PoPnR8oSm_JQmum7RSl5neK-2-z5LjZ7S54oP7S46bSc_0epG2sKUivuJCjfZ8VEpxEX4e9Aud7HJTj8N8XflJLocD2lSZAT69gUS6kq98K_IW9ZdtnN9CuPkxhdtwt6fpl_ilKpUBbLr92a2NSgBvdeWnqzwrOQF1O2seH20o7Vo05CulNBNKdfK6rqLw0imWV-GRAJsJcHw2-jfy4frgUP7DtmC4Iv-gCYl3ZkASOE315BxvM_JfUs6jNIlmF_xcYE0QFt06tPzyLSVrbsrIHish7ieiqMGvPA9UwtYhL_kACur6R?data=QVyKqSPyGQwNvdoowNEPjfkGoD3gnuOoZe_UdPwWrBZmCHqTkJxBxOY0DXJgQPDp8ZIz_zpig1TlN0Pwzbli2fKpbbrfOxUthKr-qi9Y4uvwL7ul-v2jeV-P-sgooEiviFrtUsggJI0lG5ZK94sI_NQG63UZsrYu44qyd__FZXW5oBq_5Z7ON0ALmJZ5sDZGMJKbMjJIFWAwqCUhwgxZy6UYGCO_LecjJxJkSWmFi55hMF7aSQh63ggUnWZrG5WZE3YOZeOyh8_MNDmrArAzZk8oR1PXQbivqmqTykU3rNvpjR-V41Mb_2hCEVwL1Geb20KJlMOjvkGWk-NXUmp8n2tw-s9VMq2ZXtvkM-lRz74bsr9m4tMcgerLOvzNEuYHEY3PBLuETBsdNbtxJtCRbtDj5nlJZcdPYCKuyNs-Iidqez1GW3zv97TJMf6FSehCKTFVpQVqhFOCUCZLowloPCGc10PTkXMlJdsFclnr7vw2oKGhVW3lFiC4M-vMOugJ9IaN5BIsxJKKQTAVyhjkoHYIxn61y0xo0dYhSt-BIuqfLqrqheah5nJXySfG5TsjGc1i77K-9xM,&b64e=1&sign=adc6128f129e9e22ee0c2bdb7489e222&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVB2c5UPAPMTakksWkEwoIuJ7eit5IfpDQYQYcXbdguyTZxuISpmOXUEzauL0CL5UpUicp2lJd1KxBkONQydjGNh7j_n4KzClpP6p7wvsresoTjmfZeNE87WYCpEH_-4BdOtUCmcbD_5_JFO0H1_C3x_RHIFQthrAMPJkPZk9-NIhqp3-iYJ_tZl6U7OcgqsFI9eLDYlpxcJSHHNJohXuM5Cz9a77DIyKtRJvo67zETHBzn1wKdGPPV9xKulOfo9gvJZzjmnZ2K-yRLT2QRRv_ZaIRAsBfh_RAtwgxboa-d1j4oOqBJzNKzgUAc-yi6j3exd6HuH-p-7lALEIeU5X2w4Pl8p0LOif2yhp9XzaQB5_nCDXQAV9E18BthJns1ZjfljM2OpJpiSsBJL5MpfQ56Nn3yo1dK-LPJ5x5YeL4w-rrkqKlnn2-2y8Wi1qBHIO3gO87pUpvOwqsQuACWlG22TSItu3WDYNHpEHVpvCY8ENjQmJbn-Ov8FB5NsbdS91Dm1EBArZ2le4w1-Ya1aw9fFmdXppIY302icJ142VGkclsvYi-a8-sN2CxqMbtwPqE-hPuesgy84YgCO7pwpRIYv-Lru-MHh9gb9tH-HJARqSkyK3gZXphynyRcyY-UGRGy81b64ZcKtx7yVrXgUdOUgzsSnehTC8imvlVfx4AMTPJfCSEFs9qY9KViwb-WE6k64qa2bv5nEVgAauZ7ZjpYUi4cZUjo0M3fPpZLI0Sb4n2SHBKnhym3pSf10WIBz7piEEKrMGqdSVm8Tw13oH3NtzZ6_5FMl0UrJPCa522riFcUd8b4QUoHwxjUkfADdsS4pIYS-Ub1tQSabvhAlnwcML6iVe-guvMtdfNM7ztvCf?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2csZRuVGhW6mUMDPei-TkQV8BOBJIJX99zrqrdeP6P1b-cKgiUdj0RGY2XRreK3lZg4Jxxh6qEaKXiKMQxj8rCAEKMUquTVMuYCReClOmuZ9mldgNZI-uV8,&b64e=1&sign=879b5dc5438bbbeebc50f9d27d68a382&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 7823,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 382,
                                percent: 5
                            },
                            {
                                value: 2,
                                count: 160,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 225,
                                percent: 3
                            },
                            {
                                value: 4,
                                count: 594,
                                percent: 8
                            },
                            {
                                value: 5,
                                count: 6462,
                                percent: 83
                            }
                        ]
                    },
                    id: 43150,
                    name: 'ОГО!',
                    domain: 'ogo1.ru',
                    registered: '2010-07-27',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/43150/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                phone: {
                    number: '+7 (495) 229-56-53',
                    sanitized: '+74952295653',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tDIX58LwFXvM1cZzctn7l5VAhI06y4vsWtYeHX1Z4DMsJ5dzNEx9sUaRpMLF8JVBoReE4oCmT4D-guTiWdoDRuSzUB0jQOwifLbILv1wUN_4ZNVB3aOpfICEylwlenzQDoBY3_UdTcLouYOtFeWJ_1hu3g9Fu3iI51TgxzlMQhv6DmiLW2BFhQGwNjLoKOKG4RM0jKQN1WAQBRxP-nRP_LV3iuPprKBxIe804r8jPUPQ0f-q9e06zNCmZDkIIwmWeDgfSi-6mCBjCFAJbcVtlJpDblTh1d98axnrKrs7ZCwYZelYIwC_YsjE-Z4XmeXMAwj8bYfDWkfdP8mebwEy6DhSnW873zXEC_q928R8EtRfb7iGm7z1a4VlN2UMxcXKLjxpM8tc3nJkD50GU70Z5Fp7wMt05skp-kwXGxkJMB6tBrHVfk3qM29ffGhSJ4sSx6dSpMmREsPeARuR0In3QpYko7YcKEuE4gq0upqlGOnQnKPIOj8KplUA7neDL9euoGediPZ5D8Pt1tgIQsKNOan8zI6XaXBysLxFYpJwN_p5cCdkW2m1ZtfCbfEPDP7dPSaxlxdvRgPlHZLiuBn_qv4AU8sNCJa4N99b4D69L6_UewRkCQgDHL7VAYjgjwUHNGeHrfgCIQmpOGM0hBpu2wq9KLb7BKDjMCeUn0BZhGRsofdezMFZN-bYVl6HH9dQMO0gml1KUMM02KVdyLYyBBnLlzxCGYz9KPdxvQW96-EmrBUeomgwhmbn7kQpUszrdFwMSo_SS3IsEFyejOu932sbEQANA-l85ogi1AVrVZxYvklVdqv-glsW30LttOwVNdYwbM6zbe3umIaTJT1NIWU5e8AQBUos-?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-Y6RVqLE6sqrxYPp9kRr0ypk6zstSPAIp-dNoPdkPodQfO_R5ZY_bLGtumjRe4Jvp0LwsUhJnpZFtQcFgZB38Z7oLkJ3NdzHGaPexZQ7r5AAN3_3ZXgjgaX3RUB-vbUyhPSkD18FcRpkZc96wMF-lyvtks5h_uAaJkWGm5BjHRaQ,,&b64e=1&sign=bb8e4d944cb07c97d92012015b180589&keyno=1'
                },
                photo: {
                    width: 700,
                    height: 700,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/233556/market_3WGdDTxwI-D-USRRvJg5iQ/orig'
                },
                delivery: {
                    price: {
                        value: '240'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 240 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 11 пунктов магазина',
                            outletCount: 11
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '240'
                                },
                                daysFrom: 2,
                                daysTo: 3
                            },
                            brief: '2-3 дня'
                        },
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '390'
                                },
                                daysFrom: 1,
                                daysTo: 2
                            },
                            brief: '1-2 дня'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/NHFNwRN-P4rE_C7Z5CMjyA?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=7nUicpqaxmc6OJugS9UVWrmiQuKxYGuw89USJ5JDS2jP2J0wDkGc2WctllUkcEJqBFtwDeUo9LtE1TJ_0ArAEyIzAUTshF-H-w1yrbE1lZ2VaDsPTgSm3JQgc7SItmNL&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=43150',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 700,
                        height: 700,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/233556/market_3WGdDTxwI-D-USRRvJg5iQ/orig'
                    },
                    {
                        width: 700,
                        height: 700,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/247272/market_jRzZ_3YTHlLMcr6wRMwr-g/orig'
                    },
                    {
                        width: 700,
                        height: 700,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/176166/market_5kabrW1dBo1aKz6r_iHRRA/orig'
                    },
                    {
                        width: 700,
                        height: 700,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/363663/market_uZQ9XjdJh2HoIUd5gLt8WA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/233556/market_3WGdDTxwI-D-USRRvJg5iQ/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/247272/market_jRzZ_3YTHlLMcr6wRMwr-g/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/176166/market_5kabrW1dBo1aKz6r_iHRRA/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/363663/market_uZQ9XjdJh2HoIUd5gLt8WA/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEpWvW-ZrulygB3CaNc3ybtY56xeX6UHrwlbWbCNtkNjpoEV3KUd7pT_c5vMtu0RGzMAW_3YVeg-HXhfWFJtxonCT0keqFBdhuDpLOkn5pBbFxA4dmwG9fjnkUPm7iuaX50vOSGAbo5gzceNoLXzgv5L-fUbA6teTn90pij2o0DH2YnIFNFL8Nmv7Am25lgSgQpp9TL4cYD16UvkMSoMGwrGGgcsfnfAEq6yTUWuXWioGh3S1KvUjgKnPjy5T8wYn3XzdswVzaGr0cuJrDayH1PL2a5gxd1cXE',
                wareMd5: 'JylmeQuWvq9UXm2Y4eJbTw',
                name: 'Телевизор Lg 43LJ510V',
                description: 'Full HD (1920x1080) разрешение, черный цвет, модель 2017г.',
                price: {
                    value: '22400'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tDIX58LwFXvM1cZzctn7l5VAhI06y4vsWtYeHX1Z4DMsJ5dzNEx9sUaRpMLF8JVBoReE4oCmT4D-guTiWdoDRuSzUB0jQOwifLbILv1wUN_4ZNVB3aOpfICEylwlenzQDoBY3_UdTcLouYOtFeWJ_1hu3g9Fu3iI51TgxzlMQhv4ldO0auqHhZ8savCo79cJg6SwVitV4XTsT1j5hqBSpEY-T34EAi3spgsY7uMeBk86pjAhTmu_YVGMFJN_bpfrZMwKsX2v4BkgrG1KPCTdBKOas-gu47AsY3JTivdUuDWYlJBwSV3rrswHpa66scJpI9MRHo_RGiyk6f7HWQy3s3yzm3meXem93gm4_ci3fqcoUEgwVPmpI5wLFvUwwdIMNUmG-hWbzEFa9XJ5Cpemvw2EK9KB32nS3K0M1oUItGWauOtI4QG7mhwl1c0B9e0jY8uIzFFCMdzXve3jysHJ5R5NeuFpgjlLF3R9K2zSM8W9ulFCRx2qVAdfXIk4CyqZELizPHc4SURvgKZGQ0p0WOX7qVN2AmDT8Qcae586CNhq_HKCRqRoOYl50tvzzgfiMsmaDXYrL7LoJnDmKRli_w8GJ1Becon50zQa380up2CbNVD9AAfwP5-FKm5dPDXH0EW02OTTNUpdhn_hiarrd0X1VoIlo5oRuzIXQ3z5R-VNjKuFCaH_DjxbObJvpo5l8TnhXhRXao5Ky00qLmX6sQpvKaM38WWxI9UFHY59Z-1bDiLXViCy_o4eu6bQGPB-knHcxksVRaZmrRPJorDxrpHhDzFVhWIy8SKOVeeesdB6dkYRE0aFe2xexqDw2D3BNyWGab2PYOH00Yvqw6PUb6ZK8tg1zKmvA?data=QVyKqSPyGQwNvdoowNEPjcsSl6H1ugDZrOWWnOAm5dbDKxy5-m_CH28nKv54_5YE7ktnCgDUWla7myFPwzt_yavbVkJsVfS7TWzvekNGnfYqKOCbAt8Nrb8UE0hBgpVGZ7qDIEiZ4gZ6BBFWaD0y9cWMiZo8_rvlP1B1SRop-ToJqeMKjW1cLV2ZMyYCbVtkCjLSG5-652pz1YfzIMUAyS32r_gKL3gpoyX718et8Xygmcq1pVDOwztHpIVYOq9o&b64e=1&sign=3fc298d095787a6ee267f2d5a0c3123e&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVB2c5UPAPMTakksWkEwoIuJ7eit5IfpDQYQYcXbdguyTZxuISpmOXUEzauL0CL5UpUicp2lJd1KxBkONQydjGNh7j_n4KzClpP6p7wvsresoTjmfZeNE87WYCpEH_-4BdOtUCmcbD_5_JFO0H1_C3x_RHIFQthrAMPJkPZk9-NIhqp3-iYJ_tZmfJ1MvftJIoZslTSik1Ss8QEmmXW13PjhmtJhvKI6GHe6Wvsthey9WNqJ9EVjduVtw4F7oJXYMaGJ8gcxiIwGytQmoQpLTphGl2Q3jHx-Vwu3vDhlQT_KjC5gRwB4XXzY5pOC0_W_KiA6fA7OUCcF5bZ3t5Hk_O3fJBKDMvQSZCAwgjwwMoiU_rt7OhmA_UGLpG5UPcFsdsu8v39ZtcaOG0ODFITCtihMuGsdub__a4cPV9PoXGogQnzxy4b8NaFCtvKxF2aG405k9eML6MerPlMAhszIh2t3v49nocuOGGoNbsERcvAJTKWGDM8T4uvtCm6gJOcaJ--kAHfuSFqYVIFfdY8ocUe-K4Jls9TCg59L6mGjlISZXvxXSKraKs8ySmvWL4VNOqRJQ8DgyKFPZbis52ZYpv3HCMugRwB1aZaxnbKO6wm9ZtnYBBvfANicPY6cxpHjf23fcPzWtfKqyPr7OUPH-7uGq0SVoS4JuLtqOXJvYeyRKhU-BbBoBhq7Pafs_CeQq0tg2FskpG0grdO0sd2gNSHsjoaQWYrvHee7ZFRLraktBgfUquMkjaP67_tl2o6CNpV7CGedvwFCNRhYITgkfmmeWyJgt5QSNnVr17fPbLs3345DJ0NtNebY4FkeUwWzp-MhG5-1nHkNIbnWi8bLyWjRFvHwlP4sENg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2f-BCtiriK4NdTbz-pBZF8q7Vs-0_0mJ4S9REAK_6mr527VKerJrUgrXAn7TQvvc9dhIcTA-1GI_cLfO0kO6IwHjRyTQVl0-s0a2dxcHObZXhFn8vm1jdKk,&b64e=1&sign=f0853e22ac0208d93a7bacc2df4310ba&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 262,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 1,
                                percent: 0
                            },
                            {
                                value: 2,
                                count: 0,
                                percent: 0
                            },
                            {
                                value: 3,
                                count: 0,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 5,
                                percent: 2
                            },
                            {
                                value: 5,
                                count: 256,
                                percent: 98
                            }
                        ]
                    },
                    id: 321209,
                    name: 'Магазин Телевизоров',
                    domain: 'televizor.msk.ru',
                    registered: '2015-11-16',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/321209/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                phone: {
                    number: '+7 (499) 397-81-82',
                    sanitized: '+74993978182',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tDIX58LwFXvM1cZzctn7l5VAhI06y4vsWtYeHX1Z4DMsJ5dzNEx9sUaRpMLF8JVBoReE4oCmT4D-guTiWdoDRuSzUB0jQOwifLbILv1wUN_4ZNVB3aOpfICEylwlenzQDoBY3_UdTcLouYOtFeWJ_1hu3g9Fu3iI51TgxzlMQhv6FQj9RGeSq801Y0gB_KyEbyLe6S2GiYUxQa9OEDB0lkfL3ivK73K9yYgZ1o-qjrCx4eev5SSRkWk_PIgI4gz3JR9_sJnkhSvl0-oTktCN7Zv1w8mr8FhzQlyZs4Z4UBZH7HwGl4nu_0ZdQ2ApBM9Dsr0OVCkJ_Km75jHIqSRTwDr7nVVwd97vx2b6IRqYC-895HZ-8gcz7Mtcy2_HtVm6LJUSVECWW6cNHs9v0Ft1IDKaF-gK01WRSRYPNnQBrLPjtNeL_EAofdQJwg02iM7e5qDp34AoAPnLrKSlrOGLxm4SJAXTLPBUTuS0J3vnDa7WeVkf0bUwIyyiO3BZ0iD8tRe8jtgcKC0VeYBrrQBGKzVhcsnUm2gEryKVHwHg6tkp5tLsK9_I2-cRFk7BX8DNZa53hF1hrfp8m4gMihM5oLlIiaz_uo1B3wh80TbFI3p8o3WDVlWinAwHn7MU5rj1QbvNPDbr5oVOpOeFbfDL4VFSLjzR4yJguoKq5pPch79MVT7QDZVdHrRunieQMQpOPvyx65wLv_PY17IZC0lIldQHEXNonJnLwEFprMuMdzu4DQDCoEKS0EycnJ8KXQIhJTW_TsLXFmElp7qL_9vv83FFd2Y3F1M1UTIspTZnJrm520jS1CpvVkfSbvT1B7GXuMqJUsRDMOAqZdT7auFHaMhmpNp2N4L0q?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9iGEnwPo3chyCy_sEbM_X5tfhRzqsE_K8CYmfQuN_Vj59eyZrDQo9K0lstUDaxuO93cBAjuCZycBAR0i0rAWNyKzfDrFIeguLR6YAuSOKkge1tHd1lBlpe40eeLwdxp9wnRhuKC82TYlHWOEqMLHKU8vZq16jdKT655-BetD1bqw,,&b64e=1&sign=d7591f583dc4167145b67d5b171da496&keyno=1'
                },
                photo: {
                    width: 400,
                    height: 300,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/208477/market_eqFFPfnq0dfKV_L9xT05BA/orig'
                },
                delivery: {
                    price: {
                        value: '700'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 700 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 0,
                                orderBefore: 12
                            },
                            brief: 'сегодня при заказе до 12: 00 • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '700'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 12
                            },
                            brief: 'до&nbsp;2 дней при заказе до 12: 00'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/JylmeQuWvq9UXm2Y4eJbTw?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=7nUicpqaxmeO5YJea3c7og0bFDH0HibCKedpgQ7mdoxYOqG4t6WI4-T22E0Ua8aszdikWau05K50JKauBo5gJo7ICLKLbW3ub6tophjBTecqN9H14-qOxehg4a3uuoHP&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=321209',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 400,
                        height: 300,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/208477/market_eqFFPfnq0dfKV_L9xT05BA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 142,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/208477/market_eqFFPfnq0dfKV_L9xT05BA/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFqsAVLzgMkSZ_SO16Mfeu9Tn8ljB2GkZzKPFROzbkMihYRLMi5dcctnXix9uWLzxuccrGoA2Lqd5zBOrVhjZNK534ohmVgjiLzrzg3YZnQTUn0XONfhI6jq5pPg3eExA37zO7-ySjwXmw3o_3b-wI0_GXb5zT5eLiIKcgZXyNzgxkZXUIRcEtS3zKW4EGLFRifPPVmpSpGw7EfbwExDFyOj-vI2H2PREkBypb7PaH5iUlaNBpc1je3d9wuJ3uC-Q0qjswRViRq0_a4cAJCG_qt6YW1p_4HgbU',
                wareMd5: 'MURZBumKpuN0qa6tBJrbww',
                name: 'Телевизор LG 43LJ510V',
                description: 'LG 43LJ510V',
                price: {
                    value: '25055',
                    discount: '15',
                    base: '29476'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZJYCi1lj_2s5AFKiObaMoRFph6sAYLCyu5BNbW-qeo5v_fdlIlGG7sKzSIrFMt-AKnuEerZW3Sa1ojL9HlDMUXXumhAlppCwxEsrBKI94v1vYCGnaM5p8s_XdBCp6Z-GB28mjK8QOun6MoydrCzvVaUixnIDEfOpEiZrEyzHVUVdZf44FgX59yPSDhUiyUEK6Ewp4KcsNsZUIVU4g8p43CJ9rK1qehqk-bqSkhsIW1oBwi6obPb0rdp-KQnSl1Vzq5i9VDUrTbQVUjGn3p7zB0REDuhObgBlkulRor_HEpkfjVK9PSrhxEOJ7nGluUQ9ai33Ku5P5MK5vOwZjClZE3DgZLR5QomUXnlxhWwM7WyXaYQmD1DSR44Ki6KErWUNHol9JjFi6b_IFMFoiN4WxSb6NfzqlyRJo95gV-reUeiFIe3Dyapjwvf6n8x13OXDm3OYFsvThRKmTmInlFcthVr1SDVBCXuR9DCBDuE3b-M9-NfzTX8j6A24V3MGtRnrFx30MsmgEJZ0K3qGakxXiGbDbtKcXEw8ua5OFCPKVhhDTRHngYfnrctlkB8wXs5ED_lSvs-lz_f3MELZ9GWyewSXciBwl7X4upsCv-6YK-H8Mf4OlM7zszGkyuiiSyd-vWxup82JM3YyE9GhAUguPm1ZIRfn5pL-DpreXHNP2cslSryKW_zN7waxm1LwQd4qAkFtac-eGBGcutvV2s0lypnZGAExHa-gRxpJg3m2HW6s150lnr9HPjlUTLIKslINGwfXt2-9dHEzCuv4v2wgFEYqjKaHG8PG5u4gxGpVMPgRpmNFKPp_vhbC91bWsSSvRFuqgK9iEOLfE7088g9CsJiHWyW4SW7WNakNHhhH_yx?data=QVyKqSPyGQwwaFPWqjjgNudeAEtZsavFTUBR0K3NDvuhED0gFP1Sk_V--yi9Xw-RYmKrtIhNL3mvhrTcdQnk9slrzSw1zkqJ52IZgAKq52JpJGXX179O0A1hm0Xzv07GYUx0gBFN5VH-IL-Nd8xFPMn8Yi3Mk5_OamGYDqeD8KMtg5db2Ek0JYc743-Obxxk-7kbYk8OrKpvgwzJaMWYaoPDft13JtVIl8V0MB8YSinswOPZi4Wj0UwS58wm6JxQ5RFvoLknpzSI1jUVagVjjE0dbIwvvTUpqyIx67p3st48ikket2DWmhqZPM7AQFh3XzvB-AW5zRuiabeaGDQ3RjS1erh9CiQYzPinX8Psg-TvO2XMkjH52W5SQtvzelApgpuY4mL_219o7a2XXYOL9dZbAHqeXsDenrfLkkTF1opVVOj0_j3X2GNFZgXVtNQw09s2Umc6D-51P7-qzRlyZYezt2vGS7tC5cL3050aV7A,&b64e=1&sign=1c407c1392ac8bb01194be4766bf6959&keyno=1',
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
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 4,
                        count: 2979,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 438,
                                percent: 15
                            },
                            {
                                value: 2,
                                count: 96,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 49,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 200,
                                percent: 7
                            },
                            {
                                value: 5,
                                count: 2196,
                                percent: 74
                            }
                        ]
                    },
                    id: 1622,
                    name: 'techmarkets.ru',
                    domain: 'techmarkets.ru',
                    registered: '2005-06-02',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, шоссе Энтузиастов, дом 17, корпус б, 111024',
                    opinionUrl: 'https://market.yandex.ru/shop/1622/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1724547969
                },
                onStock: true,
                photo: {
                    width: 701,
                    height: 466,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/247356/market_52keHYT0Uueuk8ejsqQMfQ/orig'
                },
                delivery: {
                    price: {
                        value: '600'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
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
                            childCount: 11,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России'
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы'
                    },
                    brief: 'в Москву — 600 руб.',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '600'
                                },
                                daysFrom: 3,
                                daysTo: 6
                            },
                            brief: '3-6 дней'
                        }
                    ]
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/MURZBumKpuN0qa6tBJrbww?hid=90639&model_id=1724547969&pp=1002&clid=2210590&distr_type=4&cpc=7nUicpqaxmd32DLTKbLTFtk91LWGI3iZaqnTAsegffZWt5jQimkrOIP72ernTZ6EvcZ8itz73wntSo13lyYfM78qJFqHJ4SUuxb-FLChL9zo6L_t4d8wxp0Sy8Mop0mD&lr=213',
                offersLink: 'https://market.yandex.ru/product/1724547969/offers?pp=1002&clid=2210590&distr_type=4&fesh=1622',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 701,
                        height: 466,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/247356/market_52keHYT0Uueuk8ejsqQMfQ/orig'
                    },
                    {
                        width: 579,
                        height: 572,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/219360/market_xYhejRsPxPCzdDVv-U_kew/orig'
                    },
                    {
                        width: 701,
                        height: 476,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/172323/market_WPOLxGWuV3_q6hwhu_HUnA/orig'
                    },
                    {
                        width: 568,
                        height: 562,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/172323/market_eLnVDe4eA8Hky8ZxQRigpg/orig'
                    },
                    {
                        width: 192,
                        height: 542,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/210413/market_CRGDb-8XK2d8KUN5Hpymlg/orig'
                    },
                    {
                        width: 701,
                        height: 143,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/223477/market_uojzu6cgC427lzXSRWuRYA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 126,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/247356/market_52keHYT0Uueuk8ejsqQMfQ/190x250'
                    },
                    {
                        width: 190,
                        height: 187,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/219360/market_xYhejRsPxPCzdDVv-U_kew/190x250'
                    },
                    {
                        width: 190,
                        height: 129,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/172323/market_WPOLxGWuV3_q6hwhu_HUnA/190x250'
                    },
                    {
                        width: 190,
                        height: 187,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/172323/market_eLnVDe4eA8Hky8ZxQRigpg/190x250'
                    },
                    {
                        width: 88,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/210413/market_CRGDb-8XK2d8KUN5Hpymlg/190x250'
                    },
                    {
                        width: 190,
                        height: 38,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/223477/market_uojzu6cgC427lzXSRWuRYA/190x250'
                    }
                ]
            }
        ],
        filters: [
            {
                id: '-7',
                name: 'Заказ на маркете',
                type: 'BOOLEAN',
                values: [
                    {
                        id: '1',
                        name: '1'
                    }
                ]
            },
            {
                id: '-17',
                name: 'Рекомендуется производителем',
                type: 'BOOLEAN',
                values: [

                ]
            },
            {
                id: '-9',
                name: 'Наличие скидки',
                type: 'BOOLEAN',
                values: [

                ]
            }
        ]
    }
};

module.exports = new ApiMock(host, pathname, query, result);
