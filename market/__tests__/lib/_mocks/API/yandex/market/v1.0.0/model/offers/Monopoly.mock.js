/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/model\/1730875803\/offers\.json/;

const query = {
    text: 'Настольная игра MONOPOLY Монополия с банковскими картами, обновленная (B6677)',
    category_id: '10682647',
    price_min: '1150'
};
const result = {
    comment: 'text = "Мобильный телефон Apple iPhone X 256GB"\ncategory_id = "91491"\nprice_min = "40475"\nprice_max = undefined',
    status: 200,
    body: {
        offers: {
            items: [
                {
                    id: 'yDpJekrrgZE32ctA1t3XK9flbhihsL5dtYtPziiGsKv9fAAUqRxqXg',
                    wareMd5: 'VYKgtSJ-hH0vHG9ZzVKB0w',
                    modelId: 1730875803,
                    name: 'Настольная игра HASBRO B6677 Монополия с банковскими картами (обновленная)',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mUlF6_FfSqswKzcqr_8vUkQUJf8jTkyfIVIbA-DPBcGe1QQHFUYWOqgjHcbOHsirqIh1ljqidXKJzluSF2rOcxYiJtnazp7jjViGjnjzbZkPxhsRqrg1ThC7FN9XebJd3nrj-XKT4D-g__nWP79k2rpFTfZGruxmkiVBtGmRuXJS66i5-mELGt3027wrIGXglSGFTVKP-0ockKdPvUjmC8G7eJlHQT9lxu6pffeV_93SMXO8cx7n_wftgyt0W14vjT83kencg-v56ZiyjwVC4ykjaXg0VcBwp4fJEuIxPw3HMqfc2ArOTPqk7SroKt8sFRLAcQHyq4Qxzi-7HK6mlbdUg34eBO8aihgIsrIw35Dh1Iu4vn580BYR_L3sk0_AyeTVPCiUlxPFQZftSLTrb30cqT9OIZQLRp6fNZNx62OMlwY997O7cN47qUQijHU733AdhzyNNs_rjG96XTuaF-Ck2O7Lrkxn5xtdDWPsQGUdpD-h_vrjL7ceUnRPkfdA9Sst4dPa31OEMfFuSaEC5y6AbSP2mCZe-a18ABPkoeuyajrKNu76EcrcQTePAXwXxS_mF2sfvF5Ufk58btWaNXz6ggkNM_egPu4AiCpo7dV4CdGY15kXgWHaiWLYMFgn1YlY8Tzp-H8m9r0l61JPY45YG6nVfEMRVr9_-aOY8P0_OMP_lUxGDKgSLbiR-697DaAl3RZyYmXjta5KKKuIaAelTVURAjQX3xQZCDOycKTXODzV5wFrQWkZKq2omoibQNpScYmHwHgk2RI2z1ufiTdeDUq_G0lOM59sa9lWVPROriGXJimyoFI-3eppp63iKBLrBS83Qi0kfdm6ZzLttT-ve1C7N1CaW5v4cUuwnc8R?data=QVyKqSPyGQwNvdoowNEPjeHCAo-a2AEEMgvw6Gv7rodwqTifDSzaDcFSJWWZewOk9OWrt7uVlCZZ_4i1TW6Jhv7iu4X03FjGPjeookZBQ7CFuQ1Hc2j6nznh6s5M2j-SldiEl799qwHDi3YGJ68sQic_9OqvRhkqj9P04rJ4tKECywcXNQ9XWhBKXaq7kqhx-ZnQ44iFw3O_b1VK7xWvNqsQleoW4ovkaBX_64J6OUd8QxPuLCIWxpeYOeQ04CT1euFavM69X18iZOL41_-zTQFMS0Ya__9o7LQ-qnW_I4rXn1fMLYFukCjCyjlATIvZmJPH9c70n0SmQmhN5Y6WhjUtM7JBtlAAczZkYSidLYHDguKGFpuk4S5_iIJab6_J6A3VytlRfmgfXoN1cxkTw3MyU_dmJt4TJnXiGMJ7CNdp6MSTe-jCBWe16AD20x2yPS43XRfxyOSGMBE0nnsB97W7-iX4cOKSclfD-vN6EtfrcdxzblV5WFhjZSJMyKkwSnhIuB_fLJpjXuPbblIgiKS0jhPXwP5umc1ZalNBt_mznoYI3LUW4jw8w6U0jkCfigVk_CDAWw3Ta8wxS4mjNCH051qy1_3ATC12-qw0psjASDi6QpVHM-Lg8noGpomyCehSfZchqIZy22pbdepxv_aUHyv8uhzu3JDPPS7UI5pvGDlJltfE4FqK1Nm5lpnU1n1rOSRsK4jL6GtiRAHbvQqwZFWQF8BUSKvhOBBsmtk_c-WT_829rutoNCzxArdIx4zRZY4oLb8,&b64e=1&sign=5d6d477d2de82d47bfad42716362e263&keyno=1',
                    price: {
                        value: '3010',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 255,
                        name: 'ОНЛАЙН ТРЕЙД.РУ',
                        shopName: 'ОНЛАЙН ТРЕЙД.РУ',
                        url: 'www.onlinetrade.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 67561,
                        regionId: 213,
                        createdAt: '2001-09-07',
                        returnDeliveryAddress: 'Москва, Ленинградский проспект, дом 80, корпус 17, вход в магазин с Балтийской улицы, ОНЛАЙН ТРЕЙД.РУ на Соколе, 125190'
                    },
                    phone: {
                        number: '+7 495 225-95-22',
                        sanitizedNumber: '+74952259522',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mUlF6_FfSqswKzcqr_8vUkQUJf8jTkyfIVIbA-DPBcGe1QQHFUYWOqgjHcbOHsirqIh1ljqidXKJzluSF2rOcxYiJtnazp7jjViGjnjzbZkPxhsRqrg1ThC7FN9XebJd3nrj-XKT4D-g__nWP79k2rpFTfZGruxmkiVBtGmRuXJS66i5-mELGt1sf9rXOXFYE1prFDlYdORiSJmXDGdBbfiuJnbgmCaO-bpyMyUgGx1i1-spD0iZ3pDEvT3OP3YbDYznhnPUlzONWiA3x55dN6GDjKW6VyxibeejA4PrT7dCLI0HJU47797un1vqmLoJfW9IGg9IMkTN13Vj69I9ZofriIUwhLhKFdjIrWPfI9uEMwhnctgVI1PWZK74YVs1_5JCm3SKx2kFfGHttnDnCHCm7AD41SNvEuPVg8uMpTomHEWBzAsZlFL_QDWfDE0xXPBUu3nmfWpdnzZh82bowGNOHC5GCeI5TEe-vLMh3dsv7CZjAnG0IDG2BmDCkPZKg0ymXtiNFOJ1eaepIk7jq079b5-G_LxDN5Y-2Xb9BSqFqbHGKZo55RE-UwqZmNMvRLsariwgAWTFxaf5DvZUwnKW8wVI8BeEWrlMD-Vs-nAuqRvUQ30HJJnzPKEiqoVLGXlnMzC8mM-rQw4_Rc5g-nz6VyulvO2MazrIKmEskj9dZ-wuuAvbH8J70B1zbZkyoyjHH9MiafEQQyLLvr4GRcISap1HgH1tW0ff5eJ0ma6dfOMJTGLQOQ3OsfYV9_MSqsi0meebtB2s-WS8GyxhjCyTXzedtq3LgMg4rulNyqcrBQlwm7eiNyonoEGeOzkVHj1QeWx3QwSZg0JV3DA_LK2_xoDX9-r5XkvgwkD7adnI?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_xePeVRacg5zv5nuyfFboprYKssfPN51dQGG7XWgAZQkGtGAhSPzC_YXJdodLPGXIJMy99QtPiHDc00ZJj6EblFjmlavY-yZSIvdyasPA2RdUCcOOt49hMDTQOiF2zFA2iHS5FHF1I0r9WIVsGJuCQcd41UNoGQ-E0g3EAaQifEQ,,&b64e=1&sign=843a737c2a43e89e79b94399dfd5bd9d&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '300',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: true,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '300',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 3,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 300 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/363663/market_HIW8w4hgdebgMRkbfIcDXg/orig',
                            width: 600,
                            height: 600
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/246786/market_BHY6B9d3u5maecWetR42Fw/orig',
                            width: 600,
                            height: 600
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/363663/market_HIW8w4hgdebgMRkbfIcDXg/orig',
                        width: 600,
                        height: 600
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/363663/market_HIW8w4hgdebgMRkbfIcDXg/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/246786/market_BHY6B9d3u5maecWetR42Fw/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '225-9522'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '21: 00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.50502496',
                            latitude: '55.67525905'
                        },
                        pointId: '8560154',
                        pointName: 'ОНЛАЙН ТРЕЙД.РУ (м. Проспект Вернадского)',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Проспект Вернадского',
                        premiseNumber: '39',
                        shopId: 255
                    },
                    warranty: 0,
                    outletCount: 51,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/VYKgtSJ-hH0vHG9ZzVKB0w?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=Hj6MnxEtdH4lY-2ET8ol8jkD0gbcfH_PPD2ZhN9V1mkVQHJKP43OjJWQa4IRBYZGxJB_I4AljpFlNeM9iU1_Huq-SV-nvG-OAqlMs6-UcRZ9V63hknYHfP4NRmlx0Yad&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZFeyGamqf8ilH-x_CMZnfVwRaIokmLkvIMKlEdwrtSh5gxQJrvOGGw1jyS_BnQCxS6k969EEivkFxav4GlJlytJ-ptNrZcEZx9yRUT79dbb4BZoSG9Fn9RIj0hfTfpua8RS9ijCKViMJ5ufPREAiaAgsphSzNznb7tngLHq5BvAVnWBa7wHw50obgAX9H41aYaQIKkB2Gv1sdv3MAFwAY4e9oNx6D77MXji6fNDM3DzXXtlbLd4QsFuKrk7pf70jcT_nfzEq--3DGOgXCRmgkpNMKgP0x-TuCE',
                    wareMd5: 'RbR3dhKWBHmXgw8YheU6Xw',
                    modelId: 1730875803,
                    name: 'HASBRO Настольная игра Монополия с банковскими картами (обновленная) B6677121',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYd1l5XbrmxOx8VKM3nxcVdRao7k0MeXeeaBAM_j60ZSSSchXL40Q-eGoNumT45SvAR3r8ArxpP94U3CJEhaSPrRGqSnnGq2k188YYmI0ySvZn6WEBQX_2ZRLz6bR0V13SooYABcahfgIU1JelEZ0RG66L0dKE3-zNTear-su5XBfHqgQTJalrILbzy-rsoZDuofiDphXLIhKEgklRKNym4IY2OMAWZqpkj5MzpIlSp0FrwWwU48-Ly4TUs53U8GK0TIyte03MMw4pgENjkoLoMT5aJzIelPqWwAJjafKnI5DpgEngYE7vbqUUfpCB7ratrXzmlm3HjGusS7b4vBvfde_dqk-GZtHcc6kIEx0COiclZ51IhNZve1zAaCooL1JzKX1gU0uaviIJeS2m2XCsix9K6dUoWaI-shg82u5OHaVWk3j6SM3dGGq6qm4MBRI6FFkCA38zTjZtxN6hvINpdeYgO6IUPP3LFq4WLjUAbgvb2pD163WgYcCDZ7a25Q8CHr0t2_zr58RsF9G7WvEujaaPj3A8Wraq6vJM3q0lmwslRfwnri0EN8Vxx3AFpj__B6bsI-23ZBmW1l__sxN9o9v5N28s7a7NWW1kWlSKuThBsvUBlkC-eaK1c7t2anZteudxjO6KWWTxKdfbSQPAGft6c9WD-BlYDktEA5ekjw76hwgjYMR3-7RzW_Z3Sah8qW9bZ410xf9XEhLa96nXMVley9L19XCFXj_iFDXUYTW_1mU1ZJPRNh9yK3bCUROnjayem7rKRh3betyuiWugc9pB1WsPBMJ-xJwSMRl_wYv7hQyG4W0IoplyzFYWuRQNANk8z39RT5tdOsNx23ql5FVyCczxHoGmriLyKYzBFUkm4ztdhbOsY,?data=QVyKqSPyGQwwaFPWqjjgNg2oGQjHGn1O7jiJv1h69YpRb87amKxokaDzXyIyus6hS-3BfdkSShIuWC82Ba_Ca7fwGVOLHgUMWEiNTGSsWpyNR5HcwQhPC6CZ4keCVvg0OS--QXWnPQr0itqiEIsg3Oh7gumQq7NxXIcGOZgsJlo,&b64e=1&sign=990d31f1db5e55f79facbaf3b0d95c20&keyno=1',
                    price: {
                        value: '2490',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 17737,
                        name: 'АЛВЕР.РУ',
                        shopName: 'АЛВЕР.РУ',
                        url: 'alver.ru',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 618,
                        regionId: 213,
                        createdAt: '2008-11-20',
                        returnDeliveryAddress: 'Москва, Проспект Вернадского, дом 78, строение 7, 3-й этаж офис Салонсвязи.ру, Алвер.ру, 119454'
                    },
                    phone: {
                        number: '+7 (495) 796-28-27',
                        sanitizedNumber: '+74957962827',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYd1l5XbrmxOx8VKM3nxcVdRao7k0MeXeeaBAM_j60ZSSSchXL40Q-eGoNumT45SvAR3r8ArxpP94U3CJEhaSPrRGqSnnGq2k188YYmI0ySvZn6WEBQX_2ZRLz6bR0V13SooYABcahfgIU1JelEZ0RG66L0dKE3-zNTear-su5XBfHqgQTJalrK3Ozkk72XIm1o2BLO6qtdyvo-x_o6_ssWZ-RLS2uZLHgwNY-FPXcuDUN2BFxlYiit-i22aMafFlT5aJuZiOjmT-OG7ff6oohKh6jK9f46OUcwmtYPYPUK88BVcAFuD-wZlhzdhecxpLOLU23PvXbMoyrcX3nbOTZiR_m4ItOHvd40fh2V77ObNu58LKgKHkeKMdh35xBnNsI-WbZrW6mylkQbS41aTt8aCOWGBbtokgyttQ8I0_62cJ0vmBMs5LV99kp54LTQ77DhfGfWhPonslmyk0d__CP5r_2gCu2X9Xjedm0sVxoo2HtVp8Oq4sFVPtDFvXjuo-qf0S1cz8qy9_P6y0cNMKZbDjszaOT-yHCceIliTXNM0Sy6ncZiu_hlHyKIL1BF29tSACaKYI0h_-_qePhYR2V2aHKl8bvcerNiboZEraqiy0WbRFj7h0V3Xc62FEX77RR4sQmtNgTX-zYJfJf3ZdOHkBW0EfA52Rww_x2ewQEJCwlwZxlrysZ1REf8oyhCtbsatNaKEMgKejMLC_1wg7Hz8l9u1uRqYBuF54HTx3iHQUa3EiJNVTFReMPkLpKYR9ExpkYhI1J6hVex3VRV4ErskdNV4GP0-l5aILDX9kB7hXaBbDcZxWzFMUzA8DHY5VtzmOJc9XbLthhbUXXAufW9zvOTPHgikwXci16zgJq7tUGwGt6pLY-Y,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8eReoOlrbeOt17rFKxnMIrSUWTvOMeVue2GLxR9UjBNi9mqXZwh0jEsSK0Yx3x4lWE41nRb4Q7GbVkmKq37o2yFy3n03bpWzhLQIYNOoKdlPnAZUwLmC0e5yO11KjEs5wFMi4jQTmNzpWDh0FQp-lfXOuCp_Tx8tGGBiUhdNqBJtQYPO5KTuuL&b64e=1&sign=3cf431b07dfb72d76ab552c793ab2715&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '350',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '350',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 350 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/203248/market_MaYjNEG_NBlc8TljjLdztQ/orig',
                            width: 440,
                            height: 440
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/203248/market_MaYjNEG_NBlc8TljjLdztQ/orig',
                        width: 440,
                        height: 440
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/203248/market_MaYjNEG_NBlc8TljjLdztQ/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '796-2827'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '20: 00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.47926379',
                            latitude: '55.67066424'
                        },
                        pointId: '48835',
                        pointName: 'Alver.ru на Юго-Западной',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Проспект Вернадского',
                        premiseNumber: '78',
                        shopId: 17737,
                        building: '7'
                    },
                    warranty: 1,
                    outletCount: 1,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/RbR3dhKWBHmXgw8YheU6Xw?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=b_WwR3irM8HCNLjNKsQfJMV5cQBh0Irqa4gRgxYO7qvoLiLUd_eerq7HxQuLvdg35HUwdgv1N0whu_EoeRp4tkF9TZ8fJqj7NOfuU_K5AjfVVyHOhmXbqM4An98zhs4U988MKbP54mo%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgdO7xu0sRPDdmuBnH6SewWtlNUYts7rFLiGGcJbOCp134fXyO1oLDgP6DA6uJ9dRgxVXwJS3qtEjzRkHuxyDjcGkRiqpK6osKnDxKUd0TPmrk_SA6JsUNOrk3Zn2k7uTMVpStajqVwCE_dlZQHe_Db6Pz35redXGBgEPdZP0bFfPqTbzwoOGwq1lhPkhe_bHW4aXFgIcVJ0P6XYepPe2DN3rDw-3g1bZlN8wyirOnhJU1Co95QeCfjsXVNTgwfvGImP7VnZnRq90LI80usc_FqA3yUCKNhMcHXWXSGO3Xxp7IPktBgpk2RKnBE1ljB9pKThBJEJm7IQ3zXsyFUQgzjcu8mV4HMei39LKQhMdKp0e3xkabHfHRiVAwdxUQhRMmPBgQnmdBdyUAKkQxK_Po9E2WLPyfilyJeV8V4_V87qgcMHCgb7T90kSnQrW3LjsY407qX42EcT4Bg0ENSu9hd2DR3szU89VvZeD2W9sxp9thOGn_xIyZdz1yjKbiB8O_t8UKM9glbBvrNgZqpw2xk1dS0r3XbSVLwqID6nFjmfIrwaqrSAEJ851cJtNczsyw2h_sIHTPiabH9BFIE047I1ndNLBoiKySW2cYQc4_nkG0gWUkG--z0-tXJJeup1QhC3VsVzyuGZ1Mssa49oYMeQMauy9OrC7dxtFTwPK8kNubwLiPVUX2pqDquM_83uoM1sBT-582_UwWhqL8ekD037cMBVLAJV3SkdfJa8lo2muFZvgKiP98tY1f0dt_E0Dbtqrkvk9Z0Oe7E_p5KUrAAeyft3J5eJSqeHkDqF3glcafqXfnIOVBRE8-DaPQDV6Ce9Ov-gcXidIEjADE0tdMTJltCYsqdLJsl7StuIuCdiNoOVxGkIQkgruW_oTpCJA1sFFe2fPBYPrEDempni8qOU,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6qvsZwyFL2Nz3coxV8zcF4ilS8e4rRrXJNOsLnTX_rAPamGdhwOFKcnd7jP-6ZVPfcXKWi4cPM5kzSu4kwWpxDZ7hUWFtKCb0a4rdDYfVUzo-Cij1areujodB5bhtxl7045uB1VFL_bgok2B1rU3giBxNE7x62M_5fM3OB6134WjOdBV_spOMlN_JlvpEKK31xQPIsGG3QZmec_y6LIQc5dtrHR_xP4F6OArmGbEspok_gAR4Z9NIPBX8hqbnb7rMvd1E1R4eAFY_y9O2bklmGfJZpAJuw0NmfF4PFDbcrDXUMrCwMUNypW5vDJoLX682lG_6UqVgH3Q,,&b64e=1&sign=c09372a8fe386ad5628d846463eb9e6d&keyno=1',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZEGqvpvMGyhJsOY3lroaO4xOzk2g1PKSc5xit7Oqre4kg',
                    wareMd5: 'j1Io0n6j-rpRKvfdSEmJQw',
                    modelId: 1730875803,
                    name: 'Настольная игра Hasbro Monopoly B6677 Монополия с банковскими картами (обновленная)',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mR7UDlM_im8_syy1PvZJmq7QLZAJxplHAD8o1Rpgk5E2C02uJGoX3vqjqpJRpZLR5ThrN2kSsn_MTnSddo0_dZm_Tt6Qekhc11Cz9V3kLOwS8UXYCw53AgM0WaWFWuPERuanwIhHQ7PIh9XnaSN1mgTy1TIQxphgFjfd3co-RyvqRrPAgkrJ8zBIcLor2Nw6CZsOh2gWxtiiT36uvzs2_-TJW9uTDVlLSbRRmaBOepFVJeCiJXQT_qyxM3ohGMNqLujU_JRZVjyKMa2RceD8OY8wzIw03IPXotAqV2FGTxLT2EZ52obd88XbfNeO2V454M0OXKypjbh64hIYcJS5z47_U6GASmLecX4cqwtMZUOJkXUvNOVdpCqJ8xgNbZOfad0ccaLfF35yK8PIumP1sFSAA8lGqYwTSGMBrD0LouT047JyUvznDQ6rMuKsgZzPMg8fUXv-3PSJbZ3FX30eLjS89pU6KPpMHKszr2VbCmyChZshCekvAVNthTJDy7PbO1eq5c_PhwLGwXokGD_9LKaL8s1XW4NrJoOj97CDu5SqUTSIgkD_cf5Ym1CaC31jAlkroqGfsXuDrNRkkFvQKyGCqmFVra8ps2GH9ufR3w7GjxZ-IWbfxGAED3xW3Z6Uwaf832t5VWAjRqFkcPgHu28jti9LsiYxaA37Yz_fSLjClRybKcAnnbKSb8quGb3Cfi0xgbkax43xkSY3GFD1DXgms0ZBj1CGLIHToottvaUVk0C_4oXvzG0Z9a0yN_pWVjJMEqkQCq1I168WoPvKFF--TLzWrszT0L98Wgj4TuwEojq7W8pktNEvKdT-GIQQJDwo2pL9UlemiGw0TcWfRI0O1mFv7yccYjrtbtaADR7WqK4gDqfTtJU,?data=QVyKqSPyGQwNvdoowNEPjRcd1X5IkopGx8TOY7lHsB2IzdZCVgSOE4bzcsD5Zw5aRVUE3FYAfxuvVRDDe5Dkm263fkWIh555MhjCV2krHHOdzkugNJDnXlpKKbmZsbE2VXQdtOFa_Xr445lYQmqJcO-dQaCuUIZnpg4fT0QYdP5PPdZQMLD2TpU6jadqKUFhYt1sI_x41vrT8JAdJKudmTtfvRFNCAakJelLNDRUEtl-fuuPs3Ux-og5t0Z5hyOjdfOgMQvFf8CrSgXgsLvBmVM9bn2OB7nsAdpMG9WT36Pa_-FLbQfwg08aMGlLH5X1IlgWjpYzt6RqIh8ELcTWQLMrPUyZZMGwmx_c26sqvV0ipizdXs9eARvhEPQ8evDg2oSkGx-zN8fJZlJzYTIk3HXEFr3LR9CspnZ853VIc28wbXIi6UaQ3qH2xlKfVXy5mV-lOwAalhGTAlb26p9WG8FWspQAdwcuQSt3s7HhtjhMb8ijubnzSKjjePiMPhJNTxaV4NOCvx2gVvCSeOPgDRUMpPAPWws3ncfJ9cBdswiajsPDgaS0iZckIjPYAjKikUdGjWTOMrYX3si5SJQXcrzwARp-FCN7&b64e=1&sign=0a6aa533f600d600781f932e65235475&keyno=1',
                    price: {
                        value: '2799',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 56564,
                        name: 'TOY.RU',
                        shopName: 'TOY.RU',
                        url: 'toy.ru',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 5417,
                        regionId: 213,
                        createdAt: '2011-02-16',
                        returnDeliveryAddress: 'Москва, Профсоюзная, дом 118, ТЦ "Тропа", 117437'
                    },
                    phone: {
                        number: '+7 (495) 215-22-44',
                        sanitizedNumber: '+74952152244',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mR7UDlM_im8_syy1PvZJmq7QLZAJxplHAD8o1Rpgk5E2C02uJGoX3vqjqpJRpZLR5ThrN2kSsn_MTnSddo0_dZm_Tt6Qekhc11Cz9V3kLOwS8UXYCw53AgM0WaWFWuPERuanwIhHQ7PIh9XnaSN1mgTy1TIQxphgFjfd3co-RyvqRrPAgkrJ8zCQ8rUhWN7w-34gJdni_hRr8h1RDzDNot2FqkebDPx1I4nHtR978EwIlp63jF23Oum_Ghkp1twyncX0YlYfJfbzmQYFgXBSw5SYveVaJnn_wAAmlqydlsIUsFDpPoIU1IO36UXhmd2fndNhlvMCeDUVRutTLQiEHKjCee0CdRyBEHyO-DNUdukjN9U7IcjZjzXaZp90Qzkr42Olx33j8ALi0S6akjJkNoYKDNp614Z2upknMsItpz6EgeAiBPH-qoHAEWwTL4K3McLW4UJnowdbc93Iv8PdFfv7cIpDxkgctRaYJGtWQggZSQ0FQmmeJCOzDtQ667THBgxEMegxa43IyN8hX646sviXSHfi3BpBArFDHio4IXC8Sw8Pr-_kimd_QWobFlrXEUIdGj8TALQOYQhRgjBiniLTCyumcGnOXrwqKF7_g2obzYTZTELA_4QP_B02ORR9hZKjqmjj4zLLGKbT53cbnpe9gJ1W5mObSBrdDZ6_Wz3PiHFyTDSX8qU4FY3HHRlE6Nd07BHRlLVMuzHOrNN7gxTVcjviv5j85b8iB4M-S0CUu8KDGTt5iahdgsBDp98UyAIWE3-eO3G7DRd_tM3Qe2yO6OUdlrCrD8yCDnIbl9EjUYgzdl98gzfUa61M2KUYoJuJYtGMtqYnNby1SjuMxJaaQ-KQoTBgggdyUC3WRyWBByySPN4tgyk,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8OuHeDkJTcsCf_LUOLG-9WaYJgkP2nUT8sIzJW0sfhp9ZOvAapB7Kxy6kU_HhZskDF8ZM7l2yHkX2-RZo1xQ-XS3YNOZNf-LG4Hm_9qtI5cipxuxUPVKqArCx04Mx0p8v7L4sdSNLna76F8lmUZmW-2yy-kFNDhuHC_lr2uDFnw-iYijzksKAR&b64e=1&sign=b5738453b1030b3abee173db4c32b80c&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '99',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: true,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '99',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            },
                            {
                                price: {
                                    value: '299',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 1,
                                defaultLocalDelivery: false
                            }
                        ],
                        brief: 'в Москву — 99 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/193095/market_AmZ5VUlvDcslxj7Q8w6LRg/orig',
                            width: 600,
                            height: 600
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/193095/market_AmZ5VUlvDcslxj7Q8w6LRg/orig',
                        width: 600,
                        height: 600
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/193095/market_AmZ5VUlvDcslxj7Q8w6LRg/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '8',
                            city: '800',
                            number: '500-2370'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '22: 00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.593875',
                            latitude: '55.752176'
                        },
                        pointId: '468143',
                        pointName: 'Магазин игрушек Toy.ru (Новый Арбат)',
                        pointType: 'MIXED',
                        localityName: 'Москва',
                        thoroughfareName: 'Новый арбат',
                        premiseNumber: '13',
                        shopId: 56564
                    },
                    warranty: 1,
                    description: 'Представляем вашему вниманию легендарную настольную игру - Монополия от Hasbro обновленной версии! Производитель окончательно отказался от бумажных денег:  теперь расплачиваться за покупки нужно банковскими картами! В целом же суть игры осталась прежней - скупайте дома и недвижимость, стройте дома и отели, зарабатывайте деньги и выигрывайте! Монополия - замечательная экономическая стратегия, которая способствует развитию логики и аналитических способностей, а также просто станет отличным поводом собраться в',
                    outletCount: 34,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    recommended: 1,
                    link: 'https://market.yandex.ru/offer/j1Io0n6j-rpRKvfdSEmJQw?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=PbLCvZdvMcN_ubUqciylFXeOPgG5UCbPYj0h-xOh4MNESlugmV1mVE9GO8uhOq7BE5i2Ca_E9k4xcvDnL6ilGWaR61TzXtCPxPdomYNhdYFuI_I8fNfhm_ish4K4SHMc&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZGjqZYZGM9ZWr-tTfKFmGJV07D6-wmih5QgG4V4ykwdBQ',
                    wareMd5: 'c6ZBiBXArZ66eG1KjLsQ9A',
                    modelId: 1730875803,
                    name: 'Настольная игра Games Монополия с банковскими картами (обновленная) Hasbro Art-[B6677121]',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma50WjUqdyg3-ciBRK4dJLpXJnG0T4B-vxPlaKKiBqvaWK-f0tlRlVhREYo7qW5wf2Vx9JpXHwKkVXH1HNlAXDU12dJYoXu6E1gzUdlQw_Yn1AIIP0HKE-iPLGbOff4HRTirFyug1d6831luc6-JlmSSzC5xPCKoxzabSIfC1n0__fCUHY8c7BuAfnhWzG4i2wICdlLfuq54eAKgaL3HSfHSc8Gl09K_8VwL1oGIAlp3EMeCUVb07hOzfSOWRB1PQF3wW4VePpO7zPl3i-V3hKZxNJkV8YLbs1fJXeJofKzDguiwRvLYdXrftTFOtgiLy1yi-RaZUIYSVvXmiO7OC0ngVOk1Hc1RHwnc-KtvDRlS944EGjqYxcIH4ZwRdw9XoB8WePsa_nIu6GiaWRGPFUPzAJXAjWOCv7yvT7XLJ2r90Aw56Hc1MgfiNvImRDMC_Fp9G3ZqcKT8ATQG9fltjNpL90BfQAFz84VXzEB1KE1UWGtKo5hwTjk0oCQrq1KHStwfVDgivyLDVIlHceDzyKojIQTLpZdc8fxXU4pHbOski3q4QddRgSE6RVJ8CLabrDytOjncsFHvrm55Zi_AhXeoxEHLQPYzc8iy1PZ5kdjS8x2hTI_oL3-PrQmLHRm7PxXkjvJOHHDx3PaGboP8IfD7V5D_2FtKrxaNbGXUDMvaZW1_YdVWSe84MKRFkQ059UNpZqO2NMikayydLFG-E4EPSpF4fvFFOvklpC4-IDAVrsc6_hf5oLyLs5CsGVzQwbx3g6jCo1pgMeEm4aZfPBs6RdkL_uiLfm-pUy-BSbPDIIqWZF812LMDPntSmCdLeuA6VJEYuXi61b4dHt78iCWb6I2uU848nLvX3-_8VLkk?data=QVyKqSPyGQwwaFPWqjjgNg9nnmsfiFMidC7w7Isd9ZfCPUS79RqtNDSn_FrfV2LwxQ6v0yr3fwWKd75IzYq3840tv0YwlPqoOJEgzEw16AkXeEGZLPUWPgZJl5GoxlxpHMZgV6bB1cbznbMLT_bjL7eOZuidpFHGhVBHICQh_Ac0JYZUYopJebqEC-MiCfbqIO0doDRNZ0a_Deuj9IF55Q,,&b64e=1&sign=7fbd2f9a4be1ce0241998eca47377247&keyno=1',
                    price: {
                        value: '2343',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 208435,
                        name: 'Игрушки.Ру - igrucki.ru',
                        shopName: 'Игрушки.Ру - igrucki.ru',
                        url: 'igrucki.ru',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 114,
                        regionId: 213,
                        createdAt: '2014-02-12',
                        returnDeliveryAddress: 'Москва, 2 Южнопортовый пр 35, дом 35, 115088'
                    },
                    phone: {
                        number: '+7 495 204-15-01',
                        sanitizedNumber: '+74952041501',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma50WjUqdyg3-ciBRK4dJLpXJnG0T4B-vxPlaKKiBqvaWK-f0tlRlVhREYo7qW5wf2Vx9JpXHwKkVXH1HNlAXDU12dJYoXu6E1gzUdlQw_Yn1AIIP0HKE-iPLGbOff4HRTirFyug1d6831luc6-JlmSSzC5xPCKoxzabSIfC1n0__fCUHY8c7BsaMC_qN5yvQlAuW-WtFIaaVfiRJ5J3U98WVYbzkhRXOayEoSZEZ7EQOTXb90TA-kqHqelseCkaj_vi-dV_BZAD02Zoqz2bPhErdPcQ8-CBD_2tqeikFJSB7Wlmss81LJPIfxjBBYnnHd3mmZqt1FDikAZBxHvAD-3IHW7jZjiz4BqvUp9XveC-rbWuo3g53urlyQB7_XZWU5BDBdxKHiz8e5qaUjObJJsNv65hSEkVTytMu73k7hcVy6dDFdd2U8kHtY_syaw9TyPKkhh_2xbR8Ywx6zoHWYKcHyShmliZk0L_qz0u3h37kmRKyO2ky5aGvVhBLrIrAA4LliXT0GnDTMr-fCmB2ZU92-Sw-abAM1mVBZyeisRUDch9h66HRYWn7uG3Z0uaoyZfyjzk-DOLlSh3VdkrIXMZOx6BCFWXZHsxMiyWwPJGSx4op24e_CXGW6eFYsED7vCKdsTpQr_BGTIQDoJ-me7aoqF_vsW2CgCbDizM7-REQFKQH7yH7-VvhBmvwcnJ_Oa3p-_7Po48hV9jMUVFKmCgOlFleEP_qZcIHC4zv7Xdy3dNh_Z0jqtzAF1CtiJzmGKxilscBsPdpd-6798FEa9uGXmUdtkAuArBw0jirT3WmJOxaAOiSb1HpX3QoOodGGIqZZTBbL_n2rBi5-6dTlMsSv_XtoTI1sXI1UveMfOe?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9RTrz7-ntfEmcBI8bDZpPEKd5VpRJe4JaKDpaclcnr46SZzfmdZtO50AiCei8BsGJbdnzjioMdyrJ1_ua453XUNVgEhS7ZKyLk8-Xe5iJA1W88iBEgykD8Kjcjr3KedboYxGUCChGdj-gGlvAVqNVhS48kW0EeYt6BO9U73yAUsWpkuS38yPEP&b64e=1&sign=be4e86c4025bfe6b80f349551dc1ae87&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '250',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: false,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '250',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 3,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 250 руб.',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/486815/market__VOhwa_yXR7RARRJeWWl4w/orig',
                            width: 900,
                            height: 587
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/486815/market__VOhwa_yXR7RARRJeWWl4w/orig',
                        width: 900,
                        height: 587
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/486815/market__VOhwa_yXR7RARRJeWWl4w/190x250',
                            width: 190,
                            height: 123
                        }
                    ],
                    warranty: 1,
                    description: 'Размер ДхШхВ (см):  5x26.7x40 Объем (м3):  0.0053 Вес (кг):  1.020 НовинкаТеперь все карточки читаются банковским устройством ( в том числе и карточки с собственностью), что делает игру более динамичной и увлекательной. Просто приложите карточку к банковскому устройству и деньги уже зачислены Вам на счетКак стать победителем:  покупайте собственностьВ конце игры она превратиться в деньги. Вам нужно иметь больше всех денег и собственности, когда любой игрок обанкротится.',
                    outletCount: 0,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/c6ZBiBXArZ66eG1KjLsQ9A?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=aivE5aP1c4VuvSBl2H1vAqoUuBWzwEGEBp6WI3D4wJnwgNamuCQYffqlEDWUdIaxVfkXJgxtCzEqRxptvx3JY7vT-WowwervN-oAq3SExCVsG_If8VhaJRkt5N2XOBVE&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZHrBFkD1H14hmUH48HeBLM921a_ykJ5iugDdNyCAbrRYJNVrrFettMpCOY8vq1S1dSCdiD1dxM077sGJZiRK-xfKMxw-XeXLX1zJ3Q1-FrqmlyHVZpOM0-nA6Ik_5hJqanQej9R4L9XHluDGOXGxr8-hrpPoso65khEaAw1G2GZOKbSiR2VQ7x9smMev2rBn7dj-NURprhWb7n1lKNIKYuRRoquBjFYw_uVfHYV3cuIi_6Z1OWuUI-tbicAe7W1cXx6W28j0wYcS_rArO0knQ3fRjBgnCQSuWc',
                    wareMd5: 'ARVKUEYLbMB16MArPJRj1w',
                    modelId: 1730875803,
                    name: 'Монополия с банковскими картами (обновленная) настольная игра Hasbro B6677',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86AX88ejg7K6EoGqHI8fUrAzuLQB6wylpgu3bOilCczp99k57RoobEJnez8XzXFELsTY3Kqg--3K_vQDJFkS3arhGgszr457ci51nWR1mXZ5OgiP51ITF96_986kXU6V_9XpV0r4sAc6Tz-oRz8m1pXm7GD-nIOyCFFMSxTanA7fsNIbQkcNlkw-XveqNGRjmo0x2Ke2gDTyQH6UdLveLhbxAHk5UtN3UCPgq7gccShO3wMaGvHQmepxgmfNVqgX_mo5KviZWnMBuCnJY8hT3mhPqIRtWHORD7pMcN31hxv0O69mcKA85L4u22M0zfNfEcWBJ23I42QCdsQjWrxLw3fn9epOL72rXvaSo9VofOvKqH0VDOlL0SNRlTSwPvUVh8hj_zrz2Tixns_9LbY6qgS34dVgMjJ_I5UsBcyVKT9lNsJeGhYepf08esELF_iEx6yxenkPpDHuc12izp5aGhMQAfFWze8jYVe9LrOAfn2tUe6Ls30ayRLMYDumqbDxlgSuXcidHBJFJvf-tCZWq0Vx27NekvMmFNmFMfoNpwwbYXViYPAFsl-ztgrL474fY7v8oT9Rz58LlQ-V86t29G_A-kK16ddq7MEBBehpYVcp5TE5suvqcyp0TewaJ3vq2FNS8rD7VTfgXktlgWlS1jlghFLxHfL-zsJY3cgm_VEU_IQ2GEceDld5pPqzvXakfrzfFVt_lAGRlVrZV-ycPfTUs1q5nkuneZUu2JzK-Ws7atI2KEcUAfhCqhwwbyZpf2rqRe17Dxi_5PjBAA4x_ffkwZSUy_fo7is6o9e0zKWuE0EJNTS-BgIfd6QtqaxbLdkzq0kQXrZjvgmIzhs4gRbTDzKVZUyWJS?data=QVyKqSPyGQwNvdoowNEPjTuL6UDLo_yLuah2yF3m1oAfIbQThK5PUnIWwtHpMyL3l_oL_yuB5Mt141_QeOTxGLYlFWQV8d1B06Sp7nUblTHExuMSvvjoFVRIIEASlFCyKl6WaGCbHg7hqNbSBCnD4Zi576SLigiAuur9ATmSLoM_Bs_dhNktNUpGgbOUuVXHCPjGOk1hjOHy9vHH8GJwhs3Rs8K2Yaz9NL4VwJBBZCpE3IzeCRthoFNYJyWTP2rJDXjV6bNa6LjMEOOTerTVxHXxz0pxfzOU89FHtH001Cd2n92xjGi8joOontHxzHctOXgLtfe2rZwyWZ_bp2cDu1XmWBiZMe8VBnBIxqPYTGTnjdJsoLfqAEi9r09o49AV81TeK_R7-7xXqvznMeePNw,,&b64e=1&sign=8a489e23a164d76b15d9441c58b7a043&keyno=1',
                    price: {
                        value: '2985',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 314283,
                        name: 'OVDI.RU Подарки',
                        shopName: 'OVDI.RU Подарки',
                        url: 'ovdi.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 235,
                        regionId: 213,
                        createdAt: '2015-10-08',
                        returnDeliveryAddress: 'Москва, Живописная, дом 5, корпус 6, офис 104, 123103'
                    },
                    phone: {
                        number: '+7 499 110-30-48',
                        sanitizedNumber: '+74991103048',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86AX88ejg7K6EoGqHI8fUrAzuLQB6wylpgu3bOilCczp99k57RoobEJnez8XzXFELsTY3Kqg--3K_vQDJFkS3arhGgszr457ci51nWR1mXZ5OgiP51ITF96_986kXU6V_9XpV0r4sAc6Tz-oRz8m1pXm7GD-nIOyCFFMSxTanA7fuT0lya49mE5o4t_qAxXB6iBKC0vsG6-3nR9qqPgpdeZJ_jdyCbZAYr7g0HXX4iZguatunmYCwItq1ij09ANuGU6PPuc04hgg-pr1DiBzlH4xtWdaHzHuxa01IyuKZ48oWg1KkAGIZBCZp8TZ6kiDzPjCL8g5B6vZvBYX0v6wvkIw0ukOWPxjLP2Ma2OWrQWRd3nuTEqORHxD_Zb1k70RlgUOcYaHVWScSaxI4T0PXgxmu0TrXfaL3XtCtSDx1Q834yFsc0dRURpcytRKf9ohPL7B9eZYoCCZKPo02Z2jBBrP_lBYDg3nVesl0aFpkCL8uRtlRYIb3KxGiA8IK3zQDzSEu2Xu9G0hlgCZeV2yK25Ns-BgpJ5QbSO7n0nl2tTNjXOvVQ9QR0LwYJCCfliPpHhoPvsGlIweIQhAVWrTSCFajXXXfRSvOGtV9xsFYFzQzVeV2v374sawrnsuOBIAQfCic-FtlfOZmE8fW7JX5dZJfVXxHVQqjqgiaDW1FfBpvPsGs2rEiV2lsurTRpptYQokgOk9694mTjvYGrAL70DBgqLSRJAqoJ7px1gSFDy0le3R90TOo_DyRvKEG4kJldHVZhmsb2B7Lc_klsUW0cHwBtyFXDoh79bfGgL_ymspVtZr2cP8h0jIFZav_pTDF18RJEfvBW0t6SOZ9jq9hmFcbQfpUG-8kw?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8zrS9PAU4JBQt3IqJUHdkXeU0ztJfHfJmoipafFc-aMbiKkfh12SlsTCnbu_7PSt8RPGQwW4pAJg6y7k0B5_dH38b6mUkN2hj_nQb5ZkMh2-GPY9n-9S-yluCQ4oCpEtghoLL-h4YKJd2Xzlg3MuYIIEkivVXcWqHWSF76TBOxsE7jqQ2Ujxjh&b64e=1&sign=0216c601171fb84ef668b71cbe886f5e&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '250',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '250',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 250 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/216074/market_8XkBn7CJpuVMkj-ni289xQ/orig',
                            width: 600,
                            height: 600
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/169660/market_VLpjBkx2ldRRTslnXY58Kg/orig',
                            width: 1000,
                            height: 1000
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/231203/market_SH2ZRdi5rmiKxiPso0L13A/orig',
                            width: 1000,
                            height: 1000
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/168879/market_1a_QZIohgKKnNWM3vWeS7A/orig',
                            width: 1000,
                            height: 1000
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/218908/market_i43OuuwtJh3z_GWnJpG3lg/orig',
                            width: 1000,
                            height: 1000
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/102460/market_whwdkd_fYLGVlT00Hs1olg/orig',
                            width: 1000,
                            height: 1000
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/203633/market_jlbsjbf_vMuvIURuxiXUXg/orig',
                            width: 1000,
                            height: 1000
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/213450/market_2GjSYkKPNxK-IcOnDcctUw/orig',
                            width: 1000,
                            height: 1000
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/366186/market_tA9TNCQOtqWLZ6ztTIeD_A/orig',
                            width: 1000,
                            height: 1000
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/486815/market_GLcaoKnbsrtr8DP2kiNcMA/orig',
                            width: 1000,
                            height: 1000
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/216074/market_8XkBn7CJpuVMkj-ni289xQ/orig',
                        width: 600,
                        height: 600
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/216074/market_8XkBn7CJpuVMkj-ni289xQ/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/169660/market_VLpjBkx2ldRRTslnXY58Kg/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/231203/market_SH2ZRdi5rmiKxiPso0L13A/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/168879/market_1a_QZIohgKKnNWM3vWeS7A/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/218908/market_i43OuuwtJh3z_GWnJpG3lg/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/102460/market_whwdkd_fYLGVlT00Hs1olg/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/203633/market_jlbsjbf_vMuvIURuxiXUXg/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/213450/market_2GjSYkKPNxK-IcOnDcctUw/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/366186/market_tA9TNCQOtqWLZ6ztTIeD_A/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/486815/market_GLcaoKnbsrtr8DP2kiNcMA/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '915',
                            number: '154-4595'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '6',
                                workingHoursFrom: '11: 00',
                                workingHoursTill: '19: 00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.466557',
                            latitude: '55.806611'
                        },
                        pointId: '781429',
                        pointName: 'ПВЗ МОСКВА, МАРШАЛА ВАСИЛЕВСКОГО УЛИЦА (М. ЩУКИНСКАЯ)',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Москва, Маршала Василевского, д. 13, корп. 1',
                        shopId: 314283
                    },
                    warranty: 0,
                    description: 'Настольная игра Hasbro Монополия с банковскими карточками — одна из самых увлекательных настольных игр! Что может быть интереснее, чем зарабатывать миллионы, руководствуясь простыми правилами! Монополия с банковскими карточками — это обновленная версия Монополии, в которой нет бумажных игровых денег, но есть банковский аппарат! И у каждого игрока есть собственная пластиковая карта с безразмерным счетом, на котором будет денег столько, сколько сможете заработать на управлении недвижимостью.',
                    outletCount: 68,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/ARVKUEYLbMB16MArPJRj1w?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=XKYYFmeg8N8yFRGd0knSjgELDT2hByg3fDp2YsiRh9vYX-NXoVvuOJL_VQNxdDMvFGOYcnHT5lAF_ms-53PlqgejfBGY9Xgtc51mWnaRbYnsz2X6TaZo5erlhvBSPc9pCJkbd2sv1zo%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgT0_JxrGk5sniNsj4Q5ig6BGJ2esZCwGjwOcQdLidPmChQIxO_DT04vJLpb1JDi4R1exW1UGiy23Dt-FduXjPgEW9Q4i7vq1jcpYqi7jqdUIZ47Yp9wIS_6QXewoDIrsYKqan-GXu8B7kkXUl78L-jXhwuBsG4LXhanHAxs7Kzx2Rnnan2s0uIMMX81OimtuM4MUlZrhb4kevvrpMq-cx4B6JQSVFwjIsMTTYbqycprGz-MGrspmtIlbV04iZcN4f5bBHaHLQETVKbuLhv6HZAY5XMl4NmMycn0b03Pg_SQHDBYejRChSEu-DFc0KC9o4wEljODyzvl9o5TGcrqPhtXU3XfBiicm4LODDg3STp3Ek6-5cYErIuX3TuN6UIFiLyuJlq-ZoM-xIeKvJXkrJITqvjnca3iFjYx1VAKdNDrajYEsluLsOcLnD0u2_oY6meb_9OYhOGVh1An30BCw9FiFNWVQLMeDOnE3snLZzkoV9GSvPgknaoDAYuOxBr03UuUhGD1meSUnbpgfG3W61-W_2-jy7oGg_4PLARhtkdxi0sIAZzIoeLDItbAl5azKB2ttZVh5JaQXKjdSRUF09ZvlJekw0w28M2dR3Ka6T7YKPUWz_dIsLZw-uTcRBGrrEHUSUP-UsnUbjo_ZkZf9YekZdnxtZlzHqFVjRRpuXecYg2YhZW4H8zl6sPGPCebsoYS9YjlTGk5X9FLCdR2N5YsknxvkBIyBrVTNmZepX5u1qfvuzCwf1qyRSg3zZn5Vvqmd4jIgMOUlFzUHNxI8bo2cOjqVlfaLxYkueQDPM9d89wdX47ITff6CnuaLzkVNj6a5MJQ7U3AxSNLC0y-aVsjFHYY70MCAjbr2bZy5S20GaaAi2ovYBV2NLrOzxPeW94Qt8SyAxkyN?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-5Xpg7JtHHAsFnhe6xc9qY94zeinxnDHVJO2vStdP3O0MQ67uYUEsEj4NelH6ukuBDkxF4SCwTeGLDuvxCtxi6X4d9bQF4C6Q-EuvTTUdNgwf53Wx0FIvsHTFtPU1JkMwljhW4ZgEwGcilBVvIEIL5hi166NP_LIYhVIWDEAfro8oGIui-lh0ZMPSyq0bP-2rp0FRtUVkVZrlqyxrOvVLZgigXpawVgiArTIULI-M2dwz-0k8I_DOzI-1NDH-Mj8gEe2h5_t1D09TArFRlSo5hzonHUF5FRH1d9in2MR--hxtDspbfoyPLBwNUo4F_RLZrbN_xFA3mW_A,,&b64e=1&sign=2ece52f00eb5f6708d282efe45ccd8b2&keyno=1',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZGX5JbaBYMmPUgTHxl0Fgj7XBI8w6l8V1Pqxl9CdVaJWQXpKzV738C5wBd_4jcwjvAHTQlp3SO-RdlcJtM49fmQeEltOM0Qa7IRVZMLS61G9w1MFmMb-BD8XCRpfqqJgmm5y3vNeiJ2aZMogwB600z3IOKwcikU2L0ujh9p9Ufb14vWcG4tCG9U5zjV0KKy4CntjRdZyjHcqKzVZHTfXbed4dGcTPOjkBKruKjcO8iYsAVNjdZ8CFmMpwdgS49jYcAGwerWEb7fEKFzGHD1u2KrXUCE7kUOZi4',
                    wareMd5: 'DvkC0ro73VIqh4ix0-3zgg',
                    modelId: 1730875803,
                    name: 'HASBRO Настольная игра Монополия с банковскими картами (обновленная)',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYlD7it9T5Of6AmkNJ-5rX4Vp_mKH7liwAEOxIpKfzbWKEfFA0O_awqbJRIA12K9Ok4zvPUOzwjXWChPOKh47qDkOZd-cK3TfkNLw5CvaJ9US9C2bWM2p8GkSMdsSrwyYeXKCKciMG85OWK9GA37rU858WF2O1NTCKY5800X2GpT3ZDco9OWT_8APmyXNrbxQ_aVQRnXc5A3h_qOGz3SSy3yhLhVidHb4B_5VmcgjP56isozMs66cmdp-hIaTzoMt16osS6ERSQy0T9RceEaDoQ-5LMiiXiSHhGflvXhBVEMu-_evu-ZbuJuS7utgjZfhhXtLyQdEOYZO8xSb0xCXuJHazQfLwJDb2uTkHqc5riLdAHyt_4DQ4N_yObjaLg2KvfDr1LpDdey01B_w1Dd3S10wC8AMtQ-PjDOXJs2wsIS7d3_ziBybEtL0NnKwMzSxv2GI1Tv0CRtCQSY1pIVWhUoSCPVtFeszTEygfgWweWE1_rciCSt0yFFxhHU6gkjl7g_jw6ncEvEsPa2sV18nkVjNH4nIE0QMaLiTTluT3DuZfh5D9nOgXzQgMd2ArJV6cDhoNrVOGfkLCFHKbw5np6EqxczBcttqcvKsqRLTBu1gn_p1PuYgEf39iqK2hNUnXpJ1l2XtDg_dB7MT75NSyYjxzpUmSqcgqvCgjRuDRmEGvA4bDnej_imjgArxnp2PugwR5ZdejrihmsG3jOeuJBTAhYfc40YEDiWGfYKxNsc312FpnD6LqS2eOzQMRc0Cb5e2DToo53vrgYENsxIhDcCvd4cSuXkSJMHNS7C_Ynf54B-_HIhjAV740VdcfoorqyTbTAn91QnkR-c4T_lK3mrMWuhYWZPn-oXLpHBHA33?data=QVyKqSPyGQwwaFPWqjjgNrcRl2JdfzyMwZvmFd1zWQ8i_qPoTgd0OJjYrzuiK9uFnza04tdPiMs3ZY21spMWDc1J1MhT77ru_vLoCPpMiJhgW8vwUlJcNE4JQ1AF3LO7HM4Yf2_j2wlDcFk1A2MKTLU5qsz5loim5X4wYA8-aP4MVc9J54cr6gAZokVZye1C7QLVhLYqHVhgq2vXS_GdP8lI9Sgmtu4X&b64e=1&sign=db104be6903b9753aedd9c463d482814&keyno=1',
                    price: {
                        value: '2560',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 427758,
                        name: 'IDC: Shop',
                        shopName: 'IDC: Shop',
                        url: 'idcshop.ru',
                        status: 'actual',
                        rating: 2,
                        gradeTotal: 2,
                        regionId: 213,
                        createdAt: '2017-07-18',
                        returnDeliveryAddress: 'Москва, 2-й Южнопортовый проезд, дом 26, строение А, 115088'
                    },
                    phone: {
                        number: '+7 (499) 350-98-01',
                        sanitizedNumber: '+74993509801',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYlD7it9T5Of6AmkNJ-5rX4Vp_mKH7liwAEOxIpKfzbWKEfFA0O_awqbJRIA12K9Ok4zvPUOzwjXWChPOKh47qDkOZd-cK3TfkNLw5CvaJ9US9C2bWM2p8GkSMdsSrwyYeXKCKciMG85OWK9GA37rU858WF2O1NTCKY5800X2GpT3ZDco9OWT__OOPWkLVXUfrF9rE3TZC50AJ4-VseKmW7UTHifMmFuaYyF1fDRjA-gLRhpA8kvST65u_3jASnALgrwYFg4_99G7z_Q7lnwZ4r-_0BIdrMTOQgDnGVrk5kpOrRaIL0_jMqHziByG7FYwuILGlM6wdLxlgXI78Xs5Owel6JKairlTIIzoCtCHzYGTkKK86e_9_EkBzX6Q-whNIl98nTHnRb8PnOlFDujBt4wwnnKElJtejalhQNBwkDU4OCaS4czaOjqTGhhDN-ZT4AxExmY5XRWSjaCgBhuz_6eIIMbIJ1Y9AFKi-rsutomtQPaoLe7Vd6Q4cv37i1nfI7t17VKkaf4RN3JHqpqBGTyIVYD1TO8QlCBtEIXqGSZ0B3BnRijJLOiqjpIP--R7GbXbHeylFQNW5QG1K8o4wP2LZ2naB553ILJe-C_kLTQoQJFbcN2makKP0SVZdy4w06pc0rK7i2CQM1MZrF9L7XSVBLWEjRoZCKRScSF1tkC4MQsdm67ykOpzetDA3_ZevS9-AEVw5Ma6PQDD9yX3kh6n7D9zWv2ljYdb_J4u1r84QZTpT7k1kbXPpFKoA2bHQe5QVqMIon89DoTKkEVGTpC3_F7qfVmfXDNbtbEnCjdRQ-eESDQKbpM8_hrgycynAfT16fSlkmFKGZlyv2kO4nlLVFXYyKa2wSWkfiW8qxS?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_cx2G7yJjXSZoopi2OBULbCUBhLLeFNCdIi4yiTtthJJ-L176s8gl2BnlDnqgKgdU8eI7CX9c_WN5Ce3G3PV4SJRx8im4kNLR1961BMG2AnrJVWobGo-AnqF_UQ5EU8zbKz5hKnf0rehaTQh90sspwpY9x9N2Pck5GYSlPN2Bc5_ipgkgUZZuk&b64e=1&sign=a81c11d3c4cd747a45b5e34c7d0aba88&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '250',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: false,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '250',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 250 руб.',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/228527/market_eUM8gQ2J6pQlDdZ0Qvq8sA/orig',
                            width: 600,
                            height: 600
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/228527/market_eUM8gQ2J6pQlDdZ0Qvq8sA/orig',
                        width: 600,
                        height: 600
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/228527/market_eUM8gQ2J6pQlDdZ0Qvq8sA/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    warranty: 0,
                    description: 'Настольная игра Монополия с банковскими картами (обновленная',
                    outletCount: 0,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/DvkC0ro73VIqh4ix0-3zgg?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=HUlimjO1HptWzW_86g6FEtyJvhq4XQviAnPGFs7r0Hv_HA78ivfOb-LfWT4ZKXWyPj5Y44cmb8YEUNY5OvsnHSDGiG1NM2F6RdhYmlb_RSkawXXBbDA_o_479eSMqBMT&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZGeq39Z6hbcf2fg0rtAo90Xq_giqSi9lFnKuA3G5B18CA',
                    wareMd5: 'i1UgMg2RzXP2MovvPnO9gw',
                    modelId: 1730875803,
                    name: 'Настольная игра Монополия с банковскими картами (обновленная) -русская версия - HASBRO - B6677121',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tpc_P94ZpY5XY_V7_GImSBZJ_S3y_k7PBZ3L1n_tckUXDXvp_5saI6qTy4i7vrQaFUcSDDF1HhP2EOUrc1gbKY-8LwIo1u3KPZHIAuhDX7AmTEdRRwdGpvdcxKT_acFvMj70yVuCUYQfzsWrE78_xWkzsoiQUzoXzjXOKMTX-to1NVZGhBb_QqdxzW0f_7qXwnh87l-0jkZG8oK2_lHlzK3pBEkcZ2D4f-rbpVDzIWNlQ8oQr4dMZg_EyeF5FbhbtCIV2ycEEAeRFEjkONiwtI82L3d_xRltbeK37oQdLLd6JeFMup9KmCna30lT3u9wMpRYJ_Cq4uiYcA4Orec6CWvos9yY1ICnzgxvlE40lG7p9tTqBEbRFpDEhmWzkgUjfqNSDBXIE4cnsxT8DvIhGou7B_nxPu6bMVYdcnPa8oH-HhWFRwiXI-l81pDIOB-vDRNXXPkWs9naO0nZHgFuZOk2kP1cSsm2ZAhu55EW-iy3erMNYajoNcoyceN6DlMVW5QDAsmi47zIlwHg26ycnKCI2XsILosWSIEgEYPfjyWtpzsoQK5yJAPYJdy_KXZwQU05CiRqMhsMrOqxr-VV8UE20ZYFWmbGxlwhebx1fAFlDNyd_kd60vTY5HRJclvlQB3vE1mU6JEq5dffBXgXKKOb9cXqT7VwBCd-b71tBAmsNHbVQqKtyv6eKnN-3aU-QE1h6pVY6M0ITWdOGkyH5LNHKZ1ZUwcrisZpaJyOq6rPreFu0Y700I4FkMbC-btF7BY635JlVtY53FN2TljPvxmFR5tsmK5z8AvzD79XqJE3scoP1j_4x7d-vfSWOA-vPockxvR6QtYiwtWFvRlOShtKDbydNnIjeStGQ9CelZ-Y,?data=QVyKqSPyGQwwaFPWqjjgNkcldWRYAE0top9a3HzjhYJNCShQG1bFhUnb_w-LhFV5jB9UZfqpZGQBsrNeeDkSIgY12YCsn6PRtafv33cKDeEiE9x2XtmtvhD5FN89bia6TVlSzGxqrCtzHHH_sEuJC2Y46zxKreNn0Zith32nM_ab2MWcnlIKsF7w-3LiJyaZ2lqdnlzJD6O_SHO0tCdjfzsY4-3CG5_Xl0YwgGVRkrxsY-y7l6pqhGt6RSJSlcmYbkuynAkHqnqkNzTg2jHYOg,,&b64e=1&sign=0c6ee700b38fd22ba422f4fa1b9508c8&keyno=1',
                    price: {
                        value: '2515',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 398232,
                        name: 'O-TOY.ru',
                        shopName: 'O-TOY.ru',
                        url: 'o-toy.ru',
                        status: 'actual',
                        rating: 3,
                        gradeTotal: 65,
                        regionId: 213,
                        createdAt: '2017-01-26',
                        returnDeliveryAddress: 'Москва, ул. Народного Ополчения, дом 34, строение 1, Офис 14, звонить за час, прием посетителей с 16: 00 до 19: 00, 123154'
                    },
                    phone: {
                        number: '8 499 110-34-66',
                        sanitizedNumber: '84991103466',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tpc_P94ZpY5XY_V7_GImSBZJ_S3y_k7PBZ3L1n_tckUXDXvp_5saI6qTy4i7vrQaFUcSDDF1HhP2EOUrc1gbKY-8LwIo1u3KPZHIAuhDX7AmTEdRRwdGpvdcxKT_acFvMj70yVuCUYQfzsWrE78_xWkzsoiQUzoXzjXOKMTX-to1Ppwkq9J0sVoAXvTPZThwxqz5RE5RRgKn_nXLCtviOfRrROUoEBf2oBYm_gBaT0aG5XZ7MZWV5DMExlheGqg1q3Eds-RloZs92b8YhbBoW4cgWF87NaGBe7JMe9MVYPBswdXMz9w3BQqr1-ZIcRJXTKCmrRWRplIiWVjPAVqcldeVzmjA6BWtEVGiXGzvlLJJleFzwMOGFZUeohQljbBbsA4jq136IqSyM9MLFfa8g9Dj7h_6u7gnGkMNW3T3QfAq5PpbWJPVZEyUuDBvX4x9I8O882Mq_DNvPD5ONT9NBrRi17VbaFDgeoMhSo95b4myi3q3XLaRe_6CF-bKn9oOAWaLWYZ-q4_cbw-FU6RpnjzmhsGDp6potLHxbRQ2TsQYcGKSchdTmVKeuowOnV3UR2vm9mfMi8ItmgcAkAVMgTfHS3o-MnswAZX3hUXJLlFRiFw3kVhOxl-w2z-8Ms7iD6G45HzbfhHzTaxQ6oZ9Rr_hwX3OXjw2Zm4NzxnoAtBB8we6MQomiOEQe1bvBjNpelAiPXT0AA_5FZmRqkLms_OXvqC2EEejj_TDZo5wrSUiCYnvSjlEmrF0-2JW_F4CjXlN_AODDvFKr5-7bMVxdGFMA0QiEQaVVW1R9BCENd0-HhcSH57Ua14JBJNC9XozKyG7Jgpnnmccats7NeowIwRXbQqlNpKga-Fwyp8Uo7Mo,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-5HgZ5yoKpBmrON2OpKEvZuv1_uO5qpvgOy5rv0EMXKCTlaKJ7oN4jJrPgvk98bHGd_teAyR1CfV23C5P3H6TjyFQLyZu5Lrj7yiH6VrSYNWKAsU8-yIT1Qn4EC3HnxK7uByPZ9RVOyCDhyFt7Sp0s5hmnd9bEFfLHxAhU2TVT63opJ1z7mCzD&b64e=1&sign=febfe29ee801d9dd5b31bf44c7f6127a&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '290',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: false,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '290',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 1,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 290 руб.',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_MUKKOQdGhQImqX6BUx7IPw/orig',
                            width: 1200,
                            height: 853
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_MUKKOQdGhQImqX6BUx7IPw/orig',
                        width: 1200,
                        height: 853
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_MUKKOQdGhQImqX6BUx7IPw/190x250',
                            width: 190,
                            height: 135
                        }
                    ],
                    warranty: 0,
                    outletCount: 0,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/i1UgMg2RzXP2MovvPnO9gw?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=y2Wqv873_LllrEOapbb03xdxogVBFmgWFJvN-bkYJ0Sx6nH6O1oXzMU4Qsn6-Qnja0MK3mHBsN3gf44m0stV_SMzIFQiWzs_ju-c0Mx6khanlDUi7n3kvQc-kcPKa1hv&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZEG7IzynCyWDC_semcfO2F8FzSzFtWDWSlTLEzq6TScO4NN0a4a-G-LKJxf7vk32G4TM6Ri-LOpkPHalWw5xYmDDILjqkW791ynG6KSGlchwOmI403ACrHEkPLsvRRWMXqu9_tVbdAEKQGwrlv1PxUU17x03sHS5igGKb-6ASXrdh8IOx4eG2ESnrmtsMDofoXYUvbkasx71jFAWaTD8i3NR8HWCniMSlVxB5c3kJTmNINrpGu7feqvFwNvHVnZt3tTbB0LsrDUHbNa2krt5Hh-Oxe5U6wxDBY',
                    wareMd5: 'xwddqN8iiPWRCMoT51EGhQ',
                    modelId: 1730875803,
                    name: 'Настольная игра Монополия с банковскими картами',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZg77bymqGBdcAgErBXw8TZaGdzQy69y4Sz19Z8RmX2_YJrRMT_EitNQgQN2SLCX7zHA3eSguvdr3q-bQt4ZNNlHS6fa1xHFzdS5LrZVS7yteTuk67xdQouasBaSeqJlE3X26IOLSn9TQX6Vayatj2Frz57PtyoZ_RgZXtBJCBhV18ZJbk2XG3ZqeUCYArNZRSTLlYV6bD1KLnTu-GGiZJ3o6zsWxdL1l7mrVSWsquben7PsRDG-f51aBzGtUltScX9mAGFWW3Bw1VFBxFaGcGzfiwZv0LHQ2Gt9alHGUZa9KuGbrQOJyl8dvtIGrs7La8w5Fya-hFMoR8LhjJyPczqbFtADtm_fPEUQIsg5l9rb4-WE68aCMvJmBBaXnXUNNWy5FbOy9sXa99lFlIOn6fSyuwaYyvkbkHsyHxOCAMvYJFVLsdt5CRdTDPGO7ZF5-OB2dznWAJmDx0O_W0mlrAtgcY9twhEtoTel8DjII-Gkl0EDo8NL7OOMeY55sDdv2u_I7HDZsS1ZKeQ7TkLS_kpKzjw6Ovy6Etsre4Xv7Dcw9qHNsZhFGrdDRuVKiSzDj25r8iLnlup-obZHqGVWr3ijYPDtcMfXFlB8gao-Kkr3X6z_NUjzyVW3Kq-6jgm9ynGMjiETV97SQq0YmyrJkEV-Yswol6ev5f4lc3Z3lk6UaCxe_1DJ2g7gq5F4B8oHyAsl75FcyfUVs1OhArc-Y3PoewfRWE6_4IdrQvGV8lz61PZbxaP6UpT7ywMhT-UuShvy9RrDL9VA4_7Aeg6T05dOlj7nXO1tquCf0B2DfsNVMiK1gpyZfjhd6T3Xcl3zapaNDGsTQ7Lu0nmtjRg0Eknl67oPDSFxMmWe0dnmnDJoQX7qp7FV_2Q,?data=QVyKqSPyGQwwaFPWqjjgNi-mXaTHyrM52t_arBPnUZxN-zwz-lEEfwwU7CeiTH92jwassm_aW1Sn89gDmu2_1qU2AgnTRCZFRZuYP7IGF_pX6IR-ZoK9x5eFzCQcp2OwoDdJkp98fBwMrtNWFpDPg9CzWamzEA6Af6TTx00xpKdRyrAjZSW_FTZQa4_vLKKkd2IHsKGUl9UamQ7Qlhk03yjGrKLkBzNgWkoVSpo5D1L2dBCB_BB7coFK8xU-ceHPHXGot3AH4QQT8a9j-zl0g3ZmaH1phDxkNoC880OEo_pSd_5vNyhsitqKHrle-_5m5WpfPv2OMYmaYYcTPKF9305GBgwfocV52ackWu9tnR3P9NTuj-gHLXt_Ey1mdT-wztCOPDtglSugEZ2rlCDXSUyVejULB5c37QJ5Y0iN_Sbm1ebPhqfa7yJayENc4pIHom17gfElA1rK77xjM7I-FP6ZD80Z3_WxCKz22YWqe2khYlEmLXMdN4B1CWrmfsf2_8ctiYHmLtrl7zbG9kegKjoVtql3Dy-Bbxo9XNr_c8AEShNll6gzwpXi2VVqN2P-prOeKzYDi_wn5tMxnR2DPt8m4d8REqkCKNV0UQk7vso,&b64e=1&sign=84557a491149b624998a06cf6fd36620&keyno=1',
                    price: {
                        value: '2194',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 253502,
                        name: 'ABtoys.ru Магазин детских игрушек',
                        shopName: 'ABtoys.ru Магазин детских игрушек',
                        url: 'www.abtoys.ru',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 165,
                        regionId: 213,
                        createdAt: '2014-10-03',
                        returnDeliveryAddress: 'Москва, Щелковское ш, дом 100, корпус 107, 105523'
                    },
                    phone: {
                        number: '8 495 988-29-15',
                        sanitizedNumber: '84959882915',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZg77bymqGBdcAgErBXw8TZaGdzQy69y4Sz19Z8RmX2_YJrRMT_EitNQgQN2SLCX7zHA3eSguvdr3q-bQt4ZNNlHS6fa1xHFzdS5LrZVS7yteTuk67xdQouasBaSeqJlE3X26IOLSn9TQX6Vayatj2Frz57PtyoZ_RgZXtBJCBhV18ZJbk2XG3beX7kMRfwEx9uibGMSXU4fJiQisSOdcEiT7aaipiI1eZDjlMc2ehXw9badUrDldiXIxwY7910nm7r3yF_dUFCu2Adxkh64mxnPbwEitdPibsHULE5rZzkA9m6M20YVOdJH70xdsEUM59G3foaD9PX5WEMd5-EnMgPEp7Kx9KwCWSqvsd-cQJYt_-FSxz9i6HFOM1ij-Rwc56oo3bQptXefV72fpcrZZnHH4TU0q5X2NGH-jYPtiJt5yBECGLfFF-fH4GJ2m0CBf031xp_f9JoLmepsSPmfBNMVS2x88TPSxXTaDR7ciox4pmBnFua0KRJ5Esk13IFoVFQHxYElPTwHVzKJq2_nJApMQBYuHmVfEHjQx1KXFGUj9YrjfXx0jZNP3LhPBXdgUKiRzUTgtbHbZaJh4Pf6exNEomOKcPFSQUfR-VH9Bt1_tMnGIENFo4QauwPJo9N9dMhQiPGn4gNLUqCfoZLi5xHQxMKBlO8-XIxXowJY18PEZQNu18tkfi7_dP9zqRKkVvr3hwuBZUFQmoFgdrqTC1CAULfPnJHaunN_C9s2lbiUwAFR_uUevybOCRowh8ha7ijmDhQuWTE-IExHinOKF1kkrjIdeq4xlvmUZRhhmcnU1zyskge72x6Jdb7oopdaQMB0cHGo5Kil-aovPJXbH8zFNC6Eke65EmgLk7Ku94aIywLa0AnBr34,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8-HoOEA6DgyvI4tx0cM2hcVT2YubJOBb82dxSP87baUUoMnO9S5X4mpDPDyEEGTEBHB76JvlwZJXRxFT7pmn3kU08cbWOpLH-mPPNWh9kdltVLnKXHWlkHuc1AOvJt8fx-UjrbqeVYRUYTkLC63Ni0KvBVdjVRePSmZqYX3FmWPoj2aw9TibtO&b64e=1&sign=5cc50c85a3a1ece3cefec5576d899f29&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '350',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '350',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 4,
                                dayTo: 4,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 350 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_a79gMhhi9K8y829MqRq1mQ/orig',
                            width: 700,
                            height: 700
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_a79gMhhi9K8y829MqRq1mQ/orig',
                        width: 700,
                        height: 700
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_a79gMhhi9K8y829MqRq1mQ/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '916',
                            number: '352-0432'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '9: 00',
                                workingHoursTill: '20: 00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.831174',
                            latitude: '55.808999'
                        },
                        pointId: '362421',
                        pointName: 'Самовывоз в день покупки Щелковская',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Щелковское шоссе',
                        premiseNumber: '100',
                        shopId: 253502,
                        block: '107'
                    },
                    warranty: 0,
                    description: 'Монополия – это логическая игра, любимая детьми и их родителями. Возможность потренироваться в создании собственного бизнеса, работа с банковскими карточками, разорение конкурентов – здесь возможно все! Да, вы не ошиблись, эта версия Монополии отличается от своих предшественников, и в ней есть устройство «Банк без границ», работающее по принципу банкомата. Это намного более удобно, чем играть с большим количеством монет разного номинала. Попробуйте! Вам понравится!',
                    outletCount: 24,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/xwddqN8iiPWRCMoT51EGhQ?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=I66uKmuULlsaMzrEkgxfGuds5cNd97cuPRfsP6EKLR9P7imY6pIJC5WjbTED0JNBfjuBm8x_wmLEFfLujnrMnrarMb4YkrFE3LjFe8AnqkIgIBdCNqpgdoRoLoMA_pfxlU2ApzG9t0Q%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabcR4TJfVe7AXprIo7-tdc2WyJe5q4eB-zdvYVBTW5u1gqgvI2279JTdjrE8yc0p1KV2c_xTWgAaVFPp41MJBNuSlbq1TXQ2PhyR97SZ_6I5z7hgHRBkbMIMhOBvHSLHCeMzo1OjUgxfDHp8gLSHpT-jjx5uyK6yk2tfPYTumK69_HzWV6HO0q2Kc5iOrVd99eA7B6lAY76j7StKm0uHwWwJbzaMB1B-g7RuZiqlqTkNYSrRa6fSEfHHspk2zpJ-sZBRbVEWWaxtc2MWE5zSZ-QMszVqQFjYbvABI7J8hzcZElwDZc5WcVpDjcqBqOgqpLIJCR9AS_L66_qD3rIopePopAzi5WxkGo2lcvvHreJBZhkK0oY6B0inebw6cmllONl_l321qMV0fLEFxia2nma_wnYd-Jj8W8Ucr_J2S89TW4YHf4febxQGhHvjFZs_0H-2qQRq_gD-fAPECLfPK1rD9agF0gRScOXM6jwxhwBrKhAfKyBYaOVAuZVspdysuREsfK96mG8CoyeT9Fi8vFKjwbvwbBKSDpQU1u8tGYl4hzjzXyzeB084oQ8-1MzR-SoSGs74-zZuLRNZ2HdLbDoMAEYoTBVD3y0hzI6mgWF-fH2y4Y9gYTaa_S7MzpigGsjpiTK0MaQiqDRkRTYzgSgd1aZ_rj0kv2Yjt_KPI2i9H9sl1NEcwOwrs6XqyV1IrgEozcUd3fGqpIOKfB38XL3JLVu9lVaPMa44n6IchyMLvI6UREBbUmwYdMn6d7T2P50WI1MJrnvwKnrSDpaH2Q4w0L9_Nnkjwv7f7E42Cx5qDwTnsgEipWykzxre4HIL-_lErSux7JD1eSR0vDI7M9-xtqw-EngJv1CeVIKc58rgs8ixI83_61pDcaNWxvi6rWv9g7LIB-ZCQ,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXeYg_5DdXLbbRUy1tmLE3xMFF4UlHgUiNWfi9J8gZ0zF0O7NwMC1Hp3N3n4ScvrCvuD6cCVwVBw9YrQln6ncjk03MIW80HvPmFzPNNnv2qY8lKmnfOGCHMIp6tXwxiWu2tYVymjBYaL0Z0aKpI5UTPWzf3MZa6p5nvxLaQi2l4VDne12FftuKtsp3G6u3KCNQTnJ2RQscsWdCqGupXF68hsOM6GtCQquT7CaKeZEin5PuxiRhUICDVsZj_TwAegvxBNLNcb4iuPFZUaj_OlbE59MaUcaetwZe9Kva2qUjGOUTKF2QUGyopg,,&b64e=1&sign=5bb41312c66faafd2af4e7d71866fcc7&keyno=1',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZHy9mmTie6Qf74OFnHPmULC8kAw8aONrXnx6rhp-wPdu5ogLw-HE7zb8yX6EOHlVuHwwhYVsc_8k3_BVJt46qk8JBC-Udwnm8NQxYAwRAW0fEEnYJB3RwyZgXVvO1oY7k_qiMC3LNHukF9IWgliJ4JEyFDIW_0W-y1B5iDUXSu3MzHLr6GF9GckC5PgMySTnBKc7gyzrIRO8j5IlT_xKA34cHO4LUPbAjiZbV9L-KbLQZ4gVR7Csv8Dn4ZJO02i4to1iuO555esN72LddmKYtxv-q1xcZPdrGM',
                    wareMd5: 'yXE8qASXrz0315Yobf70_g',
                    modelId: 1730875803,
                    name: 'Hasbro Games B6677 Монополия с банковскими картами обновленная',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mSjN784BcAcPGAE1fkJrJHdykcA0R_YlqI7rIeya5P0ze5UpeK1xqfTY-24b6ak8iandv1qJT1S8xY3HIdfazMF9-GkF0ghLATB6A6iy7pnmBq4X6GnJniSwNaR0pycVT17Zc0n4IirJtRYFnKOxlcOnMacVxCGWBEA08DVPgq3bdv502R4VXoP6yN3ti7ooZzRTLuCwBVP1mljrgteiX59Z24-q3rjrIrFkiRb_ZoSV_7SfDQvI-1aKSXxRrGEZ0rR5gPeD0D3q_OLc8X7X4MwPLqWYJHQOcKgmLeI3IDzR0J36nuW1Tjj0UGWWsdk4OF2D5q710TGNIdPbC-LxGfYedRQH4g8WFJXH7gTt3XxDbcWO7t1miXQUyQEd0NnSpYhYAJ1uFIt0KkgDAj03IY0BWSvLYtyh-robQ9MMWxbDeXAebWHz2UT36h0pO_CNNMYeqfzhcH2C_ZJuJvCppTcvOWj5SscA1L__PvsC5TW0DHX_HlSsPYXdscDAo8YxZzrlIeJrNyFIucJmRtVe1902m98ojy7VscRHtCMMAXEdXg-fbUjXdRoCpNz70Ilv6XD51_5AUdyiepS6wZ42gx-gfuE0RBL4XCOzUCNzom8jXcYpBHuUueX-cg8i6GGRi5mZHG9IcNMdpFnvAyLh1AQsbmKJuds3Ka2AvopmkyPLLg35lZEFtCpjtupBwTt7fqayIFiWdW3HV8aW449t6B4bosVokC_URIPmEs6fxUvkksKfwJMBpa-Jp4QQyl5F6dtzlt4xDx2oj4-p5lus5OSt_VcY40LoJWT8Jw8lSPbN-CIiMDpzQ3nWI2OCaqpG76GlvKF0P6dr8iHF1PdUPAHfgm3xLK8-h8V1MriSiKJu?data=QVyKqSPyGQwwaFPWqjjgNs-QcH5Ghp7HhMKrmo4hU7CwjE8BIVTWQc188o6pwUiVlK2lXtiTGFAAgFrfHGsmWQCALkAdAUlJcQsHCA7zzBa57opDOJLhuL7H81_MZenKIe8F7ZMl4BNnULosgr1oVCLeFhLyIMB5EdNcJRT4fCJeP_aJiTFKmxftysxOcy9aZFocMBheN8uwYumBZkPO3IJpw44TET2tOcwM1RZwzbKCGhGI1K5K0Ow9E_fMCKVHh-a2zQwpft_o46ncSeiRRXA12K2V5ozxmI0YQ604CSJrWKNz1qgjFapSO0_Zp2qrBZ7koon3TR4k0Yzd0ec9epETmc1ZZxcCAlIQ-bR1b70LDhGbtJbtLLER77P64YdlcMyixzcCUVxfDUyNSOJZMgzr_x8cWpkJknqnO1NQyRN6yCfrZeO7RfxxuHivSrwOZHdXQsgvXC2-p_2eIh4ALhdkboa6nNHppNGY8odGrBPUrueTr4ZW-B2To42JAPYhbpbGThF03cpvJT3SOnCFZRRMIvsEfBwBjrxMwQvHOePOFDapK-vum893BjACj9rhhCQU5I45ym6HshvrffFXrY23Q4KgkxbR28KgeAtO8X4ztE3fYBR9RNcdUDvAJe26uftrIk6g_uMikLDV64f27QkFlB8MKqbI&b64e=1&sign=e2ea2829f7a132ce7870380691df0679&keyno=1',
                    price: {
                        value: '3379',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 384803,
                        name: 'NBorn.ru',
                        shopName: 'NBorn.ru',
                        url: 'nborn.ru',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 441,
                        regionId: 213,
                        createdAt: '2016-10-28'
                    },
                    phone: {
                        number: '+7 (495) 626-26-90',
                        sanitizedNumber: '+74956262690',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mSjN784BcAcPGAE1fkJrJHdykcA0R_YlqI7rIeya5P0ze5UpeK1xqfTY-24b6ak8iandv1qJT1S8xY3HIdfazMF9-GkF0ghLATB6A6iy7pnmBq4X6GnJniSwNaR0pycVT17Zc0n4IirJtRYFnKOxlcOnMacVxCGWBEA08DVPgq3bdv502R4VXoNPfarx-BJv9AqBzUo8VUczjFYv1P_Y15Xy5gJa7b9FNdKXGsJnWGTFx7OZwXpjqMtjSym9MTptvzp4YhZrsIr9pnsOc7KNfn8hCj1J6EKv_FmlIQgpv99GoWf4nXyk6ybeFRUzORy9prRdFs5TtzUtdZF2LyWrnultoZNCLIunxxQtwhzedYVeeS8rD9z2tWKoVPIV4vJUB7NgJfB6pcfK397c-kOhIxDkYbW4JEMr7dGm06_FOKKsW5CPNXzbbmrYzC1kMbOl8etjTB80F7Hg8rdgoA3FUkj9oYhAnsw1IQULirkEhu6o376I0fLAgQyB5K8ui0LaURVlqkELyKcgM04jbUWnnexFPvxr-Yqtlyc79eDEo0NvZ61XQYuuROri3XSUMjXhpb_7z2bXpOodUJ5OIlFh5dh-aolA4smq45hTY1W1YFZF7un2pfEJaSgk0gdpzhhNYsYnxMnmgdDyl_Z6qOxLcbqsZYYN06rSMAUTa8tJd6o8jLU2wZv1nha_TKZ6CnAyoiGBekK0JfiRZZgvx87y1tN9qvhEjr9KgKlWmmfiVDbUGIGGyMF3lFYPJ_QbWzU8Iw7BgUoDAV757kK1n5B-xVZ5cD9FDm0OAP5N39i97GiUV0ZGf5w3__GVZKzgQNv_1JanOVfqZN7tBRWe-EMVEE9IzJiYTc_rIQzBHOqOh18U?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8C2COp2ZfPU17TJj1_PwxRc9Kdu7hLteMXrle18cVRoV2qZXOmcEsOs34Gz7ZmH63v17e1Fk-BQZezVfH_rQgzl6cR3zUckvk35XbpcxuARqJZ0jOuxDrtNZg05r-hEJtvjAsXNVqJGarNFzQOyTmtOBlCUQTB8-qMlxyjF5GcLCWS_i8vpb8C&b64e=1&sign=497d4ab6f999349db813ad36817a1f03&keyno=1'
                    },
                    delivery: {
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: true,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '0',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 3,
                                dayTo: 7,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — бесплатно, возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_0meYAtiHeYxjqqZJ3h_ZhQ/orig',
                            width: 600,
                            height: 600
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_R6nPtYeRnTuAUJ94SHdk0A/orig',
                            width: 600,
                            height: 600
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/235547/market_c_NwxwwzpM8hEW2KV9bZPw/orig',
                            width: 600,
                            height: 600
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_0meYAtiHeYxjqqZJ3h_ZhQ/orig',
                        width: 600,
                        height: 600
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_0meYAtiHeYxjqqZJ3h_ZhQ/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_R6nPtYeRnTuAUJ94SHdk0A/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/235547/market_c_NwxwwzpM8hEW2KV9bZPw/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '626-2690'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '20: 00'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '6',
                                workingHoursFrom: '11: 00',
                                workingHoursTill: '18: 00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.605355',
                            latitude: '55.802816'
                        },
                        pointId: '1050701',
                        pointName: 'Nborn.ru',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: '8-й проезд Марьиной рощи',
                        premiseNumber: '30',
                        shopId: 384803,
                        building: '3'
                    },
                    warranty: 0,
                    outletCount: 3,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/yXE8qASXrz0315Yobf70_g?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=oUfJYP9_mB-WXQRd1ImsWCXZHcNt8qtos05baOof7tNJZeDfvNlzla00uOfqvKk7hpk2ZIaEDwVsCMSiuw25nYcL4po8PjRPZWEeCvOE8MtC1xOHcNkX248E_FkMaeQd&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZE34da0q7bFDQl-CNoXY1vYySFY-_deoVZv5kgYZ4UJnH8OP8h86jMbF6x_DOgmPWELMbfiCAcc0gOTSDx1_SSM9CFGoXAujgbk00mudjL_VNIfFWfxCyvUZ04aTA1G89pPeiCOliRw28eaoGVP1_K5ecsBGw3Jqk4cn5IQ5zz__WMs5wL0pk3-hsU4Gobg0mb2V1bfKrpR060Z7x63g7Eh82CgDA09Z3b2g1EXlaam1xAt6xysOM0WjztcqiuXiEF9q3SzliIFc6HlZOs48IFYu2aFQ8uiiG4',
                    wareMd5: 'EJzs3luzi8WwYcvBQt0SLw',
                    modelId: 1730875803,
                    name: 'Настольная игра Hasbro Монополия Банк без границ',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86bwNsH0sX1vc_bpZZFryGrbuEbD72ptXeKPa7b2JUzjipNfcE6vWAxrCGpEx321RGsBG3iBckmmEtCNnks8vma85j1FTm_fHFJZRujbRiGlc8fB3tlMcs5ZobnPWE7rzgCKljYUU7bVk6L2iJNZucb4Mq7KQwvq1wgfv9_J3NiqjqJXYvn8Ag0ZnoUeonDli3y2rg81jHHp7su_r-d7W2qMZJIfu8GFZG3b71V-y3UPuLbWVOEOpRGZp7saF1bA-GStbQN5Cg0ToE1Go7veELKgN3afki6vN7p-0xqgTP7becEMvycQo1EESY1i9hI22qxJ3XeRY6o2Ru63eK3T23JrjJC4GaK33KPBJBtwKVxNjfNeInV0Q8uDQyGNPzDW2yPeaUs-O6TlBBAfvztMh6NqHiWXlt4M4lwy2lrCeVugyBbAq0CnxQnbgVaCCcL2p9Q2md137N7_LPD-gSu3CfB-HwB8E-j9dUSlaZgGB5G9K0kF9QXuNad_xf8INUc-YoqDBDihdTyZHbRRWTgzFGqJmn0ck3UyQD_MgB-cj5XqZjQxBG1wTVjNok2-HGGMDvoX0pWLjRSSC6IDsdX8mWkub4JVgXbwnwu4yCuLslQakTZU0x1Nh-FSgBAtk21ytOCzHzJba6MVj2DhhfbeV5qWoCte4pXhT3zKKXy2fgqrUEQCJkhScUuOz2giyCkt5m1vsOS803wq8iYXlCmqj0HGq5uadFVlxT2R-1tFQO9wSDoeyNfbXwWcT1Y9AxtOFNIyrb7qSvbyAXz5GQ9NbJYl6hewvqdsA0zA8JFwLFuB2OWrIM-5jCsfhUR4Ca2jlZm5v_y--lE7L2yn5Y1lR0Lr2hnlZt83d7yy6KNzzBkXI,?data=QVyKqSPyGQwwaFPWqjjgNq31P5uLbHikJZEo5WiNoG3ULxX9IBcGI8oeC4DjpvvwVbXokDSo_tTCsRNN5rUqGcd4gBqaY-szV_LNXMmp7EN673N-nrDcpNlFCNOxPnFhFKns1cBqsDt9r21XCcn-2I0iR9rS5vP91sSaCHsbHlXZy8kHgbmWgIUZVD4p5GThz0lkywYjifAELKfSJM3dznOqfCA6E3wm2MkE_SbjoKLtgaVhkHjjEm--E4agV_HrtNTI4Xc6XrzCUzs7wdQBEgGx-LlESFVu18wshMcLE5MnvfV2njzBVnBCXJJ5cgtj-ExXtDQ2bSsMChoAwzFhd-3uXEqEjz8giHo_lT-DBShXWhhwy-hrC9ymhVzeQo6sFuEiYuKqMsuhc2X-46xHeUe4ZlQ5n9BNsaMO74nMY35EG9jf3c7Lr20qhVmCqQ4PrQ--1WDEUB86chHqXpoCU-pyl2pMtNKj&b64e=1&sign=42cd63298bec60f789b0081ee09e444a&keyno=1',
                    price: {
                        value: '2990',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 350233,
                        name: 'КЕЙ (Москва)',
                        shopName: 'КЕЙ (Москва)',
                        url: 'key.ru',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 2489,
                        regionId: 213,
                        createdAt: '2016-03-31',
                        returnDeliveryAddress: 'Санкт-Петербург, проспект Энгельса, дом 124, корпус к1А, ТРК «Вояж», супермаркетов цифровой техники «КЕЙ», 194356'
                    },
                    phone: {
                        number: '8 800 5005 074',
                        sanitizedNumber: '88005005074',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86bwNsH0sX1vc_bpZZFryGrbuEbD72ptXeKPa7b2JUzjipNfcE6vWAxrCGpEx321RGsBG3iBckmmEtCNnks8vma85j1FTm_fHFJZRujbRiGlc8fB3tlMcs5ZobnPWE7rzgCKljYUU7bVk6L2iJNZucb4Mq7KQwvq1wgfv9_J3Niqjw4mV_CSeUJiFFJ1Ik0EuE4lGf5T6NYzH9C77vYLDoQzqo2LbmzU15HQ7IlEPHK5XDhGJh78El7HZpRfN23HCkdBPuAYIrvToJlbiPxyOjSJfxqaZmv0CyA6nZS_6J0pKALHKkV_l8C5cVUOnfB9oNTbopw3Pqliav-Y7xYhEv_sVuWfJjWZf4zCu7ehDXnyAmuSZIXvQaa4L6nnZRfkG04Dy_zbG7z76KYx4gJ6xeD5mESjIIKkSH7J-d2bFsoNQ-4PnLKxd-MYFZ8F1VU-qWIUQqJRhZqA62Ppb6fPjvOcBhMYAPUq8PJSbkoNogsiUruGfKi2TaIlXhymZ_h4c-Gy4KhgCZsqUSOLoV0sbGRnFjBGmGtiIlLJKTVVKXrwb9973SWV2jn4DoUDNilPwb3uUNX0aZSO0vBJ32kcfpgLQJk2OR8Rx6bAXc0cfKtApF9JbQVC3z2V14P97oBS44-DJTWBQIFsmpUeaazpGbsM7nOPqIk9ApH6c67hxtZngNYP-a0e6hRYYZtetzC7tWye8zaURfn3TFI5QljRpKmRa8EZImvoESmCQbx69toUtym95-Oz-V_yhb8JOZ8QbyOAc8ppCeF_SDpeOt5pz8nLo8xhMl2frP7wWEqR7unYpLCUcRLQ5wGxEaeqVBS_ev5-WcDxvjdkBz3MVm1pvOmvy2-YezeLF7PumWXZGzZJA,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8ffA4-23vuScR3iBhnCYpfVkl4OFxH0-QQ4mTrxYkt7KzphuIRgp-0C236NOShWoGvmakBVdQjV7Io0vRfcT2LSr6e6Z6FeEKyvaAcVaxdeapE1o-ukS3rR8vsdJqKbTyEnsFk3Kd1GT-Cl-p3vbkAew49qv0w2ro_kdmonHQzUtkymo88ybST&b64e=1&sign=64c0230d9565937b0a802d2db90261e1&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '390',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '390',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 3,
                                dayTo: 5,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 390 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/168879/market_EF8jOYzu6qEwfgKfop8W5w/orig',
                            width: 1184,
                            height: 828
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168879/market_EF8jOYzu6qEwfgKfop8W5w/orig',
                        width: 1184,
                        height: 828
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/168879/market_EF8jOYzu6qEwfgKfop8W5w/190x250',
                            width: 190,
                            height: 132
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '800',
                            number: '500-5074'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '20: 00'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '6',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '16: 00'
                            },
                            {
                                workingDaysFrom: '7',
                                workingDaysTill: '7',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '14: 00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.529268',
                            latitude: '55.602983'
                        },
                        pointId: '2811925',
                        pointName: 'КЕЙ',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'ул. Тарусская',
                        premiseNumber: '18',
                        shopId: 350233,
                        block: '1'
                    },
                    warranty: 1,
                    description: 'Тут и говорить ничего не надо, ибо название говорит само за себя:  самая известная, поистине легендарная и любимая многими экономическая настольная игра с банковскими картами',
                    outletCount: 91,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/EJzs3luzi8WwYcvBQt0SLw?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=XKYYFmeg8N8eoJxqOpiBiW3MHBQXgUaVUl_j3PIAZ2oIoC5tZibHJNQ1MiplByTiKd9qwvZyve4oIKrJ34onhv_KWZ4058kHAVRujH3jvDZJPQ77Pwn-rReohFeKqwn6B3VHejApB9Y%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97EpG1W8wuf-SaAbSY-iQNDN_E-M_UkO2S_D1wpjKA3tzLp4A6tY9ZcX4X6LWqe2tN_b-JY8zaiigy_4nQAgPCxUULGdur5X7p85kV5ocN0z3fBRzzBj0OIi3ZFuRNiZm-_O8uc4HZhJe2agqYldWf8uebu4PC2Pwsu6WDKQ6BM1k4J-VzusERNA132E1LTczYRWWOVBi350QXvCZdxdqVg9-5-nJB6mXD0TQWCyW_fNP9PVmA-oAiExcdT_mBWgTYKsvbO5PykARo7IrntIs_vOgpmsAOHvyktugxM-KBGcbqD80e_IvRuJ7eTKZKMchNMXDIJrRkLerMEhppfeWdWE0ITtJKzlmOaIGXKGfZlSc7L6Cf576274RJ7l_ygbNyqOnBTWOkkzCjX1XA1bGxISKwb1HmUc8FrcKsfkcxF_ZW63C-atwT3V4IVYHV3j4fBFBcER-NMrAj7lcaxzOQKMWlizp_eZjpH2AL6ptvVu6IirozpHXLuTeJvpmN6OV0GqkZx4dZteOw08PS21nlTX3eC-wfhDymnvhfvt7iP-EVhk69owTcRFXZL4-lLybWswwMvzDAbgBgga6XikE3lCko5n75AAO5FH3nfStQ1IVh08CoAeGSlDhxguBCBzhJlM8yx3sFfuJk9VbsJdyps8cecILBYPfIumIrU-Qxjqf_d7A6DMttQmPZjxNRUzreLTRiNGEnmL4aGruEHt-EEs,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXnteZ-vzCC2VsHOzKS2MeFUmgM5znepkwV1sjG_wghr7hkpp24815ERzTGZgq4NuX20fazzynvIaZbJX03nRwG4G9O1M6xhOLfSe9tFSjT_vSrQkVizuqafDdHA6SfOZwySRUJocV_BL0ZFJ8jDc2stcPZdP3Uyxi0vijJILgL-PJj51YUxAJ3yzsJzg8ln3gnELpasP5XPAeBmbFtEUTiEDD1BS_b5k9OSmuKYPkC_bdrQruUjFXQScJuOpB4ebNinf2ZMRyGoUOH0jwzmNR-Nq4dntiJNGaGHq1UFw3z7vvIYed9aZJ7Q,,&b64e=1&sign=f430a173e3541af0abce795e122bede0&keyno=1',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZEeXWGJ79IOhIGZlEBYlDkZO28bhgEIDtULWrX0aJ-9y-n5U2282GK1Nhduya-LLHjAWor58_ASu_BgH1oNH07NYzkICcDcmo03hutQ6T9OupVzFZND5_sm5X0YU-Jm00ZHg_b12_pWc1MktBxpsxZZYdi16tK47rY6Y5iqbPY9IZkbNwofyTmC-5OMANuL08vX3TKPtNjVJwCa-TifP63TOz5rkuTpR_YryONq-j5J68cznaE9oUv-GQkPcQ-LU69SEwFrRK6MXowpOAkohn7sXispQmJWmW4',
                    wareMd5: 'naCBA46SX2tRXtHNmOe2Aw',
                    modelId: 1730875803,
                    name: 'Настольная игра Hasbro games Monopoly С банковскими картами (обновленная) 6677',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mf5fIUVrEGurJDGDPH3rSay8CncEjb_QJvtlo-w2NtMswPrmKOaiQtoJAb9T0lNBU52G9dRQfGdilBmjOzW3bUn-hZZzwo85zLrkBTsqgnQmFroGKBff_vnkQ5HE1E-iGL6XY_DaEjS_nuuIaI3AKlVgRI6-fxMB2IVue5kUSMPWV8KWmm38-eCiZrHFkqVHeTbxqLaeNIyEFfGKJs6j54Wci3iVmwyC9etjtQWuBStEYpv65OukVcqWkes3inrwryrVYRe6i0H22WmFqGSoXC9wSOOpJyHLl2ejMHQpAqMvVTPj9SI8206nGtJjxARag7x5xvNKdHHGqGj0Nt6mBlG3xuxzY_MDY-JqeejpKuG7Ob_jZVNG5FYSU2zufPemTk8CTmWba6754hSJO5-ENMWRqlXvZEQDu4h9_xW7OixP7lnv9E65dSGVNDxrr9wj04f5poPXHPhxGoFzfbMcpFAHckKNcKYkfgck6kbFs2PWecyjD4Q36hTBwminUA2Krwht4A30dZv4oFucxJ3iwHHQXPeAjf6sYyTAo3rdvlvt8xLoviXjoF1eL2B4F6D97ZtxodNfF0B2Ckk3jv3z4C6Sw2SKRpEIkRYSkFAYfbyZ59AZ9F1NYoM-GX5rCKiUj6nWLuIk0UuTxVIip63nFUQXd2oGGvg1WBkwkFLgElXXfD6d31dbyTy-SdXd8JfPK7W1pePMO375Y35-3NMHd8FxWo_3gbo5jFcJ3xGlYV0PZ0Tbkn_xaWWNXfTZnWjHmppP3qVXzmWTOr2f1gc46mAS-Lep82bQU6MSoBdoXAr9OVX45-Ve_NliR56CD16juW7vwuXxXfZfoMFXa2J85RoXz2r-zvmOkc-LucegFf-V?data=QVyKqSPyGQwwaFPWqjjgNipc_NFEv4VsrsdnuMffXNjwtUWOxNTj-YxZcIvF2NmKck_ZaeNDK3g5bUAGEFpJu3_x0YMXXLfwFwPw8HPm_eplNXWLEnDhB9fG4K12XqxQpntzfR0Wjeb591301C1MBG01_BcQJiZS&b64e=1&sign=e6924bb8ae3d242d87fe4d17465e151f&keyno=1',
                    price: {
                        value: '2500',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 96053,
                        name: 'Babyandtoys',
                        shopName: 'Babyandtoys',
                        url: 'babyandtoys.ru',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 867,
                        regionId: 213,
                        createdAt: '2012-03-14'
                    },
                    phone: {
                        number: '+7(495)774-71-36',
                        sanitizedNumber: '+74957747136',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mf5fIUVrEGurJDGDPH3rSay8CncEjb_QJvtlo-w2NtMswPrmKOaiQtoJAb9T0lNBU52G9dRQfGdilBmjOzW3bUn-hZZzwo85zLrkBTsqgnQmFroGKBff_vnkQ5HE1E-iGL6XY_DaEjS_nuuIaI3AKlVgRI6-fxMB2IVue5kUSMPWV8KWmm38-eCUm_j-3ogAWAhCCnDKxqJZuWZfSXuYjtJZJy4iouoFkbS_EsPTTkzpGLNXa3aVkpNcv6GA1QzQ0QelOscnQmJgHeXa1rjckKNDnQixUVGfXU66YhvsdV6Ho_gE2ydfuAagtXHzS--Y5NnjDL-vgr8m_wGtba6pJ6YvWoORsDPxiQSLKPuPZuZgfhtm33xYsHWjOR-Ee36S4_fg6jwoNyKSGJttIl1ws6ZuCy9J_vAeSullItqeBQxCwFwuOTEg3_xo9M6sIgnaJc17APj_q_mznY2pUoHy8ihofOdHi-zA1ajso_tOTwQ8Oa867Q5PJOiwB0Qqaxe3ZqN2SwILDD3oAnlqun2wxXR00BQp_ETJirCyTxHBmvuntm8SjJI_9OjqDAGm_knzgZhN5YFc8Pv2pYhwH7ovvCtGSEt8ulLJuq54rAfkYJZzVOKZxLV_cHHIy5P9yZI_G_pty8EhZ6FQsg5JHGCiNI91Lx6JEvoSZL-v5BO1O2_uf_Ljo9yqSEjvqnmzwstrjOSNjkDQOsqdPKvWb4citZ7cJ9jVdWqot8g3OeWl5YF1CT7KYnJ2mQs3JdTcLffjCBZUnHpy5-3xKvWEchrwvdtAaLL_HWBjq4ptc13ZCiE0mgEeDkbHtLXFqYBpJKOvgj9qRRbw6H0pt2UE8t7PneLXMZnIEeCe0BcycYJNgSYL?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8trEt_AXyBRlTPR-2_RCnu_3NHZN7WVw6GPfMPcFeqZUm1_7z-LNq-puKX0lhUbHdUQhCV6BvgttYv566VUoSZzRbaMg1HQ3R_jC2_Srmfdkw6CzTftNKtnKhVbreK6E_sdUcFO0bcENGwquYz986mgP7PPrBxS80JdNzixiosZG8iEHWriyEs&b64e=1&sign=5c90c6b6119b7cf9c5d35def9b199d7d&keyno=1'
                    },
                    delivery: {
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: false,
                        store: false,
                        delivery: true,
                        free: true,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '0',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 0,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — бесплатно',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_9p6Oy5LCeU-HeYMNU_-eFA/orig',
                            width: 300,
                            height: 300
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_9p6Oy5LCeU-HeYMNU_-eFA/orig',
                        width: 300,
                        height: 300
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_9p6Oy5LCeU-HeYMNU_-eFA/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    warranty: 0,
                    outletCount: 0,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/naCBA46SX2tRXtHNmOe2Aw?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=z3ZJfuTLpyk-q2kvYe3yCLrSvUyITY43_o43lNeVzhxt4wiCaY90gkC4-7Lk_MrZnrIIHHMmy33uY7eMJb7FhnEFJf5x9oV_ar6tpdbx91eh3H7SoL1b3j7tT7ZaIIUA&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZEoX45tIgkzHMKJlS6wygpSvDz9USXAEbS4h-RsvG4un0FB1ZVPpXf_cUx89dUpPtVbTqf7r-g68Y19Sx3iBHJngaCgIWO6J9d0CApwS25IfVjDNQV7X0YJ1uEV92IgoC_xOZeWjGJeu6tbMYFQDMaox1qCWowLu3xm8SbUPwFuFf9Rk0v1vYiJTq3d3IbnCDdj4A4nAGBZ_waMuSfeewx4MkUX1Ps5YzaDjFGNI-jn7t92_-dky3lRWV1DOi7psD_NbWqT6Nc6L2iA6nfOZrQnyhdzsdCUpKg',
                    wareMd5: 'kpMK87lekzt-biEgc-I-PA',
                    modelId: 1730875803,
                    name: 'игра для всех возрастов Hasbro Монополия с банковскими картами (обновленная) B6677121',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Zu0b6Qjj_JjVwqJjnCim6eQ9foo42r3DyBY_77LfXbzz_YZ7RyIQY35_5z5aDeYS8TTrc01fGIjoyI1kL-cqxUnlCtW8Bt0VKMpmJaPp5lB8No8Oms8yhyaiud4OlEAAhOLxcw10p6sFB5DyMLQiBjjrdhPayxkrxyuOvIp1CkiszF4Qy6lpQlHDDWWSDZBM3NhAZoyuMTj9AND78UKigIh0JHZVy8keBazHvUQz3bKllSnn16x2nsKB1p9Dq_qqT_aniKOTRAIc_x3fA48iGXuE06GnQTACRztNQBPeBayVrnf_hmDeInLAXKt-QRDcKqgKVmzvorL0O4lhr3Q4N9bWFmzflCoZAnHx1DAWSPkkSO1HnLxOolFaAYGQ6WLPcuuc22Q2YRVfMt92lGT429R5sZWY_q3A_G88W2ETnYjzMplxJTvaPv7mzs1J3OTTeTM7E1sfGKmSKiQf7K2rnPk4gNIO583NjmyHd3hm70VS31etmJabv504zmPfBBeIdQiD0JXHyZaS01GTMtyungc5aA_ecV0NuTcpeI4edAz7-zXytGfgFOHydQcLkzxrMBYYh2QiHaMKT0Q2BbspZz5ZFeFueIB4TYlI6ONUV2tOpYj-D57HSo3dxNV-rYN1X2uZtRAPJVGObdrKl-aN609AdSSHFJOJ2kdyF97SqZTpg0dhZyKpNntgymykGXMwzYf_bEuDMRs?data=QVyKqSPyGQwNvdoowNEPjT-PVv8pTBpLuRC9I9MystJwPJThW0Z5IekfWJtSOvxIYfMS1cppbmrzM8Gal9roQB_ee1crHjqvIVheOZgNreJCgyRTDlD-tsKthS056n2uCVRRuPiUKPbDvZNkVcTT0BqL0-B1c4Fa852HUHqqO9f3asdpeuUwdNkLcCncNEVGII7ZIkYko4I7jaF1NZtzAx6jNCUF0LltnG8VJuMiHK9BfCYwwFvF-Qb0p3OUTwfYnJoj9r5WLMXCo5ePaS7E9opEAgkkskeCyjWbBNQ9CdMkMT0yPmnPCs7GHFbm9bi5lh92DjJxftpddCa39afgtvLqd93iAksPx7e5I_HSIOfTC0wblTmQg35UjinWdSdNDT8SoiuPlaJ5_rg-ZxHohNoGuFzprtepVgeAEqfp82jREtWBC_d3OocIMMEwQYpcaYSAH-WD8kS70eBkrOvLKWqqiZA6gZXxoog29ugNOo2sOnI7svUrKTtrwLjsWSdZQAqZZ-_blR35866arhZZ6c2E8Suv4RqR9xVfFcoNc5Iw5qkEHhG98WnnbHV5Gc___G9xn3ypqMJj6lIKpuhXM979W6poy9FzaqCTHTJ_AFCA4bWtEuH1-mYFUd1s_8ztq2zciINS5Jr3RujGp4L-WqDAYYCu9jKZMO7qmESGvDX7QNH_QjWpn-CnWoqApgwZ5wcO2aCDr5BwWyC7WWEMC-8hbMWGMrtQPzWXjzuFpE9dWmzpBUtzcAxGklgcl3PnU5l80hu7X8JyO9eddt_CLqb4Wbu4vPdXxvpPlw1Cjj4RI4AJxNO0OneJv2vS5cuKhqVwaQj5g4Vcti21PNDZEcr3uP5OQDTMD70yH5H1ScODKDj6SkpLhhPMCPnPxUpNqrcjnPlcCCl4sXVpEpNKwa4xdlkw85NUK7A-UgyXDkYk6_VSuDO_tQ,,&b64e=1&sign=0a2583cf72ef6ac6b656e7a617583976&keyno=1',
                    price: {
                        value: '2520',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 4122,
                        name: 'KomfortBT.ru',
                        shopName: 'KomfortBT.ru',
                        url: 'komfortbt.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 8051,
                        regionId: 213,
                        createdAt: '2007-01-26',
                        returnDeliveryAddress: 'Москва, 2-я Фрезерная, дом 14, строение 1Б, 5 этаж, 109202'
                    },
                    phone: {
                        number: '+7 (495) 983-31-39',
                        sanitizedNumber: '+74959833139',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Zu0b6Qjj_JjVwqJjnCim6dsDvTmqp7qPintSJVDWkEMNOg4lObowJxFhTFp6Hcv1C2iS_DV6Y9ijJiPKXgSfac8lOGwI8sCHPuAUwP2ziYkkB2IX7dhi8-WybFU7myVxm0IMlhOYN_xr8KnyV-eEeh6JOHdTLfwvdkKt3f4X9v1Y23C6FU1n7BeJmGyKX2jHuE07LaOeqwdM8pgWTPVdtuzcLruDQNknhZSrkq3LW7f7DYDpXdIq-zt_7BYfzYR0bgYHBHrMKFXEVhnD-v9RYPpAuyTiaSJ8wr-ilJrlDKtufyxmGPRl9ROUITS_9f-XEEJOESyO8BMcxhjYVWtP5bs-A4zG6jiooBigfTe1dpZyQr-192vQtwkeUf2S9zOnV9gy8Xu-b2_r7h5OH7gtO1o1pPQocUdC4FH7p1ybGe1fOyBpWSoJ11EHVcH1pw2cgOB7eiuxOtsWwTMVp6FuxEbr7JC7eSQFNNf46NqtLnGN6dpUJUtNyMw5T7q0A7ily-PJX2ke-rRPynOiXdOKP3h5NZQ1pu2dHDh600KFQ0SVLRwUU8N0v0Wa58APCsoyNXsRhrLxGQxHLdYjeNckZpgtaJ9w96ap4Zzu0XUDWWUOSlazETKYwMH_BDb3NanJsqY1I4llN2yxBmB_WCuKgoOlyBuzs77AiayZL4EJ5R38QI9VqBc0Hc3wnelgpwlyKMSq8l_1x9T?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9l9jc8YuYZ5afqL2Xt9_BxEt8K2Qoa-J5lP1fWG_GPw8kKU2FlfytYW7UeP-AgCQAgoLG49FVYRDH3b9Zx4fS4Up8hCF-TnG900_s9gWyZsbS8m8q6ogfBdIc6wkI5z86smkvvO4i_iK0y8RIkAKxb7rzh4PNFIw_Ka5njJ_oUdQ,,&b64e=1&sign=20ab188c0f2670acdf56b2c60fde8c76&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '250',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: true,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '250',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 5,
                                dayTo: 5,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 250 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/367259/market_azMhBi35fktG-ieInko-7w/orig',
                            width: 600,
                            height: 600
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/102460/market_Ccrg72wgPQPWm4-6nOtofw/orig',
                            width: 1000,
                            height: 1000
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/367259/market_azMhBi35fktG-ieInko-7w/orig',
                        width: 600,
                        height: 600
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/367259/market_azMhBi35fktG-ieInko-7w/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/102460/market_Ccrg72wgPQPWm4-6nOtofw/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '660-1712'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '6',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '21: 00'
                            },
                            {
                                workingDaysFrom: '7',
                                workingDaysTill: '7',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '18: 00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.57599966',
                            latitude: '55.77223029'
                        },
                        pointId: '384463',
                        pointName: 'Пункт самовывоза в центре Москвы',
                        pointType: 'MIXED',
                        localityName: 'Москва',
                        thoroughfareName: 'Электрический переулок',
                        premiseNumber: '3/10',
                        shopId: 4122,
                        building: '1'
                    },
                    warranty: 1,
                    description: '"Монополия" - одна из самых увлекательных настольных игр, которая обучает торговле недвижимостью. Изобретенная в Америке во времена великой депрессии, эта игра позволит вам распоряжаться большими деньгами и быстро разбогатеть. Теперь все карточки читаются банковским устройством (в том числе и карточки с собственностью), что делает игру более динамичной и увлекательной. Просто приложите карточку к банковскому устройству и деньги уже зачислены Вам на счет! Как стать победителем:  покупайте собственность!',
                    outletCount: 4,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/kpMK87lekzt-biEgc-I-PA?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=XKYYFmeg8N-pd-mrpwV_9VvYgpCkFTF6NfjfDwnRHTCiHVWRUCdUSiLLy-WHjLXhrmp1e6oynKMHxPA3FOlH36SKZFw6s9A9LsKY-0UilIYZfeP-ST3OdhqIKO8PIXV197daXI5FzmU%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97EpG1W8wuf-SaAbSY-iQNDOYw6ETyf_oOiJs2WDbOsNCRS33sT2pSPo7B6JRHt0m-Q4oxzLKLM4rjEUCnYudgC1V_d5_GhHmdXIPE9at7gWp7YWmMKE6X1co7p9Vs2Wr-6BWLZ_z-qwee5lq7lg-1HdDSu78ARae9bNTPloekOBguQ8rKe4qh8_sjBFDyszhduV0UFyvvoaaWxV0WgCqZtsACy5PTP6limhlCF-06nGblzyAti3kXOE3Fddw41-Gj3JXPjfycACohgbvQDBGxc4Bnxev2cZOrCRV7Q_Ybv-upomi1fP-Jflu1Xb8we5P4m1INDdjN0MCHhiAj2Z0AMloVmaQR7J-qJEcLwTArOWfKwE1bG5tB7HTLNdrY4OljMd7RdwfvpR2-0YU20SlQzZblZk3biykY0tvSpAGujbdvmD5tdD6HY5JgtEqodWP-x846IGHD-Dbf9o4VSgTpGjoyxOExv3KGEjIFoKzXmfiWDatqy5rIgmJekrw_omtiyK60KNeF8FbtogTOpz-uC5tDdI6_VYj4s8pjY6UJt0vWosEG6IMrSbqNiifrTtKcIldJboyNw2Tpj_ApXgPTCiU86_GA71LNCIIQAE7t5YgciDUj7qPQwpkH2S20Ibpo5NXdkSLDO--Ipi_sBsVeZGLPeDsQcTfZqMeMbtdCnYKqayJ2Wlxc4dxasT09_rTTGNXWrPT-9Rm?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXMKhIiMM3ofHNxvDVyLJXTbnQslPGq_doL2xRTRY04RyFaFT9Sy4ryOlYWvuV3NFZcLZu5Sdc5NQWrz4jmVPRP6vh6k2RDIjXdKzRV0dE2krXBlruG5zaQ6j8vzbk6H25kLupYv4t6ccW7s4L9fROrq-lW-nNwaxfpOjMSezEcmBL2yDjyfZR4C-EWfWqmFKXQl28fJJUtkmKM493ZHgrXQP5fo3gO9VimHklwJhM-s77jsuTYlbYWfRH13hIaY1D1RZsvZZB4qSRQJd8SfQm8DON8UDZ82I-eAITWPja27nSUWFYzGZbSA,,&b64e=1&sign=25cf2c6183c9e03906d7449a6930b84e&keyno=1',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZF0HLyOdVu0f2AGmWE0SO_WBkA3BoABGib2m43FnYMht79auVj_YUPAch2TPLdADKqzFLxQLQfMXLuMbHaIa6yKahUlsGcYNtsaGtUxRRA4CQp1_bxhrIhFGoPom3RX-ctJ6MW2MclejvWoTs9bymQRRY0DhQbhw3wfkBCw4guc2x_iilCKlIOCM-bKQH99s16YDytRYORjoilOJqPARTJcGiCS9veFJi7hEHIJjTGYGx_wHG4Vve_Um-aAUOd03-PY9G7XmH-H3bxZiB7XyjqDp7wBQeIgBzA',
                    wareMd5: 'jfot7wdt98P0DY9N-loI1Q',
                    modelId: 1730875803,
                    name: 'Настольная игра Монополия с банковскими картами (новая версия) B6677 Hasbro',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mY2KuJPeRWaXTEEzpuhAq9jTPuXukOmPbI74FnXTG-eAAQ8kUcXiw7Tn3Yehv940u0tY16QfzbLXL-pVoeZEO2n7BSK7_Fc2K7oaqdZzz0t7j275YYi_YF9DQqD-TL0Fv73odBHiMl-VvOJbVBxbPpyJPRX71bTPOOWA4bPQD--ByHJggiTP_XXRcAtZ3YuFY4VRhgTRgt2UTuCjcDLy_oKQH6-60SiRittQywQJbti-wAJXztnzkIugQKFr2N2NvsSTNP0Xi1IWDnVUbs1VX5YF3uYwC9MutZV3y9DHVMNy6BTAM4ME5hOz1PzwXnKaJL_C6UaxwJs46GpebMbcduQgraAIWk3gkZGRmk4ppB3MHfTkAbKYWzOIOBBIDUjyzgA1sqVfaJn7leg2VazeGNWxNq0Iv9qtULqr9YAZWzo7ttE6vFhBMvabqKDCIl4vH3dDila9xKNjMPsJOUgRGve3mBNpo4skVhtqEQmjQJg6yNsy4cDe3HdLgnx92N11GKMOXgSMRY6VRcVW6sUrFsoAOGBnUcEYSpSqFytKZ_NRcGaeFk1KFDeCrkvjqxgk2hOEjHTAUAxUVqLzy_MfrLi41Sr3Rt6jA1kGwKGqocp15VCd9Ze5AF4LGwkjTBHnXYdcoXbXDLSY5QtUflqseMYOxPrNpVYZCx58RFqPdbBKhVH12q4UY2Iu_zusfy5_ycVI4JYaINsWi1RIIDVzqC5-z4EcK6zH_lPNJAxTBrTceTQbBDd_aERR0nphNA5b2UbrCvgFUU-tnUbBHDQTPPPfWLErUQpQSp4MjN-2G0iR-X24dcziWgMmPUOn5gw0SyQcAbiVWI_oEqf_D1hDd7-GGetF61aUIDuGaLU9mBkMqWteH7voasQ,?data=QVyKqSPyGQwwaFPWqjjgNtHye4F6PH3UF7xUEaF_Js1KoIepvH-qq3bp0IdS74QEk8MqItmGk_3MP_c5CP_pMtXmIkutlpkXOXrgPIMEyoyhSd2CyklEaFiKibcpCpg11Xc3do_JdcVZvpavSbDDvyEh1030TatpBEfX8r_wSmMj_M_zEaf_0D8lMS3xGMVntMCDrEZg-7P44UuL0ZVExCSKVj2LLw2EM-reGSGH7SUoyB-HW5a6UQ,,&b64e=1&sign=805337e51b00ac5f3b1ad796a3cfb148&keyno=1',
                    price: {
                        value: '2427',
                        currencyName: 'руб.',
                        currencyCode: 'RUR',
                        discount: '5',
                        base: '2555'
                    },
                    shopInfo: {
                        id: 253862,
                        name: 'ХИТ Плаза',
                        shopName: 'ХИТ Плаза',
                        url: 'hitplaza.ru',
                        status: 'actual',
                        rating: 3,
                        gradeTotal: 132,
                        regionId: 213,
                        createdAt: '2014-10-06'
                    },
                    phone: {
                        number: '8 495 922-50-44',
                        sanitizedNumber: '84959225044',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mY2KuJPeRWaXTEEzpuhAq9jTPuXukOmPbI74FnXTG-eAAQ8kUcXiw7Tn3Yehv940u0tY16QfzbLXL-pVoeZEO2n7BSK7_Fc2K7oaqdZzz0t7j275YYi_YF9DQqD-TL0Fv73odBHiMl-VvOJbVBxbPpyJPRX71bTPOOWA4bPQD--ByHJggiTP_XU8eFJHPnvhPy4NRi5lSwq16mY-8hC-yq_RZ6WtUd6DLvp6P4AzVam3QwOJp6fLdN2MIw6IaXn_3NiQBza2MZTvXnlOqWgLfBmMliHjqWMIqFZ3qgAYRTQqTXMVk37Et7IkFoqeJ3tRyCRKH143_O3Y8VI1fYKZpDV2jJyzaVcKwf--VNYXtyR3LYW2MaZLnPCaKcAdFQehYeuAdSJjYRY8_LU74hHYDWGwU6YbvWSZFoJRsZ89YL2QMbs4O-93qjeiesMKB9pM2ARgKYS65LIUPnTCGvK8brkjS2qUS1GGgP2YrvsHB6IWgRBM9jpruyU52nelQ6iwoah502Z_Bb90NR-NR1jeUcqc9VRJ-U4eoOU8QPxc0rE4cMX5vQcOG1r-onVJqtgDYQ59CpMAsM-qwc8BC9qckiYWzGx0W-_BQTCgw_Z_xd41t9layj_d224kpnddPKbZbtUK2v1EeLMvYpD5BpcRdIsT0pi5LsmoylP6HVK62RYsdAWUQd4iiz6CvmmiLYsjtg0ubcr83su_pgV_Zml6j5GDxITHd1RWtZ60mxq8ALpcwtAw_za596BhooIaz3ZyAWawfF01xOWqY5mLQ0ESyRO-6B9r7IjxK-aqe7LtqwtIJDcDpMeFDH7eyIDV9o5D7aEgcQ6g483XaSkQHN2aBBKvyIqdMLg15UFmbHpQM18y88je6KG-blI,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O95uPi2gRBVNhWEKeDL1ItjaSgGs_TwNHlSaPccE5fShDol0AlwT1ZoLxEDNrLT9KCsrgLf9DHUOIIhlunt7WpkQKTADD7UzYpcwbHw6Po_g9keYyESB75sK-XRbOIzXEUfeVfMxl8wfOxNJH7ETsPraiFyU9Hc7ofeOtDy7NUxkHjXaxKISfRe&b64e=1&sign=0d5ca5213a02da42642b7f1d2c91930c&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '270',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: false,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '270',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 3,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 270 руб.',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/247272/market_r-7sB9PwtS4U0vWJMY55Gw/orig',
                            width: 600,
                            height: 413
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/247272/market_r-7sB9PwtS4U0vWJMY55Gw/orig',
                        width: 600,
                        height: 413
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/247272/market_r-7sB9PwtS4U0vWJMY55Gw/190x250',
                            width: 190,
                            height: 130
                        }
                    ],
                    warranty: 0,
                    description: 'Легендарная «Монополия» теперь стала еще лучше! В обновленной версии этой всемирно известной игры появилось множество нововведений, которые делают игровой процесс намного более увлекательным.',
                    outletCount: 0,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/jfot7wdt98P0DY9N-loI1Q?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=1ar9rTf3IRdeRFjoIYx2h7E0fro_KOp8Px4d0Je3K0xoXsEX6fzc0_DTzZQsgfhueL7Qn1TBp1d8hLIfwg5I8p2spWTP42JlVuVVLi9Wde-cGtnraZqtCLTPN8dDkchA&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZFWtACkHWY4xeuCyguyohSauGoAk3rUOP1-n1KtO3UeJtjv16-HthOBsZFHh5JsohoJge-R1Vw8LH0AZa2STCxyAAHov1LKMiKO8jE0VZonmbwJ69KXD-5MUP2_DfiZ09QppEnb0xmd4OGohGMnxYwDiJ3He4VF4jtW_iw98Qu9Or5UmdKAbx8MZif93m90GcdNHWE1dPS8M_18Bfh8-HXVioPhpORESGjEz_NUNBIA8ISLurL8ZfNk5srB83cwjnn4XitjN9qQrFCJwaEedUQmeAa_IsNhHtc',
                    wareMd5: 'ZKx7IeGQUJdloRvhBqI1ig',
                    modelId: 1730875803,
                    name: 'Настольная игра Монополия с банковскими картами (обновленная)',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Zu0b6Qjj_JjVwqJjnCim6drxCkas-Joymn2JEg7nOQ2QaPfOfJJ04M0RzW6349xlapxjHCrAfFoOUTjCJhe_uiR4q5c5sUrj844HZR-owREo3JZi3hHholPyIuK22bsRsv-9Rf0W7mfnI8G5u7XB_gTIcMuUKyJFf-mwqUuQP2bnGlRRYnpzKu7CgI7-WTuXHSzjdAYBwMaQ6b6MYopDhm1cvzxyldSRRim7Mr7w22fuZ7yoJLVCHRefRbX18hPkpuddJUjWtIN-yKqIx1HkRvi3qCZvTx1BFBn8jG71B4nD0YnGoSEHh7WOOzDR3f4xfVP4KZqxja-FqFOYaU8DT0PPaOWnMxsSphUiWYFP-3c2DrnJ_FhnIosVwYEe2LbCriJBSQdXF9qrocnYdJlWmwXMcRkRmNKzqekYrKWBNo8K-Q20Bv-O802jwNwyDfGiNtIaqaCJ9qqMX4-H2EDmr8L78ESknDWuIs_wdTDz5ZnZ4cepKOzIVS1knnAq11xPcVek4nLlkTo07CFHtdDMTJ68kJpkwW09hjidG0WvWBrNjvLO0r8veT3cSVFbf64y65paQ7DbkFwDNO1AYcNfRiAcJapFmQAObZjfD8KR0nVSsSsFnFb_ZJBnzVxYi0zozjQJyoFCS-Z3FJ2QlrfrB0j0H5mZLBDz_4jYJTb8L-TBSmN39Y-xFcG9HzplmqlOas65DrXxKRCok1eRXxja7g,?data=QVyKqSPyGQwNvdoowNEPjVspVxNaMmlsAUFExFkm5_Ph4Z_OjnpMoY_vh-3ZwwpkUMb6Pk541EAtTMXXFf_yaNOPZmlcLbZY2nZFOFS1k3GlTOxQBe-G46zIW2Yuc-cKwp8puwE-ucWYhwqqrwq4SFUC5c0BlkTk5DOluugRBFgdiT3sPX2XZOBST79rAgO75H-P8CaN5dklJf1QHd8cGWLZhzjKnIWeLxOt2-SRQzeVuAbOk1QfYO3J4LKc4Not2vDk03xAPfDo9rdJd3oJwxUI1Rv2PbPjTrkzPC9wdYrp4biWLANF3xUkjOyl_TEVQ-H5RlWcYTTXFe3_A206buPKj45Eynn5wSK4WDH-P97pcdMG9CM_x-Kd_m-etayD_rQXDbz5aLECL5YjzwU6Ree6iTEM9pBd7TMeWU3Ok3OnoVOqnzPFwJjgCEeWpm5Lx7Jf_q1OOLxHbqtCs6jEpFPKeyLxx9zJhswdfj8d7jv3lVmkD6H_55ZYRSynhYvLDKh9RRtJnbrr1E3AQndhDRXnAmiYj0XdMk88owNG5TBaLg79RvacSMkqPPaaJtMzRD15UWyABdI,&b64e=1&sign=ded88d6986693a9d5647acd49f75d3d6&keyno=1',
                    price: {
                        value: '2450',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 434473,
                        name: 'ПАМ & ПАМ',
                        shopName: 'ПАМ & ПАМ',
                        url: 'pampamstore.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 37,
                        regionId: 213,
                        createdAt: '2017-09-01',
                        returnDeliveryAddress: 'Москва, Коровинское шоссе, дом 14, корпус 1, 127486'
                    },
                    phone: {
                        number: '+7 495 777-25-63',
                        sanitizedNumber: '+74957772563',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Zu0b6Qjj_JjVwqJjnCim6frO-vToWhVX5FcQ8esItWcCtPZwDu5MNFACj4Dkg-dQIcj39By7nBKXigG5M2J2gzNz7ICffXBUGOascd9GOez9hFoNrKbfsvC_eB1ilh-o4Tf7dN7oO-x-9heJLVAXxR-Mn_LbC_pN8H98MO3ORA69ETekWJpeV3HVo7zI2bR5gC6m1JEYj7gc7mDeVskYopE2a7yGRqlVnF5XHujcTRYWLBDA6eYCey7Lgh3cVJV9wCMyj-ReFkWgpK-FU0e88WkhkE_lWdlIPkQiGpXsKRZ3rQLdcwb0BWilgcfw08nle4Rkmv6Hzmc0J_9wF9GUX0T_xCYbbUq6A95aJ2AQzzqC2L9PdgMaaFzBCB0laUjr5qEuwmMCCIt0S92EZYeRsaho-J5q1ICX7I1WUGFl6QGxowepjS0m3ajq00h3W5S3cLpzpp8gjFzietLH6f0J7YpiLxuGaf9U8cJTU1cpQAXC4mdoN9ArEKFfl2t6vyd4NE1LrZlI1c8nh397EbZSr2-UwRyHTguOsnM0W6y3u6cVUVnBvjphbQSL4anD0oyL0Kcjt3cDzyYwyKKlEZANYUS2siRSkEnii1yPiVDy5ZZC0PUDmh8HvjY7a9WXgtg897C-pF1rPHGk7zQB2e7ywfQeIYd_JKISdsFXel3lDbtgKahPFW14Nbz3kEh22A3DGhQ0ijCD2embiEyMU6Ozgk,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O95qeTZ1lNs0497WAyZU7I0dE1hFaHDeSfJbmQ783IGKf7zIkQt2FOC5n2XiVBwBg21EJn9FS5A3ng1s_nm15cUQwwd3JEEHs6YfwbwHKtBjx0tky_rwqDiSyrkdpT9Kf25KcHaXHfA7UCcxrJtZgGDnXB34Y8ewrupQeOXBqH8i8lx1MbZyEzc&b64e=1&sign=0c36717fe15ce20df6a3c02d0f495ab8&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '250',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: false,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '250',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 250 руб.',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/172627/market_gOG6LV0cGB8DFg8uKVpoaw/orig',
                            width: 600,
                            height: 600
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/172627/market_gOG6LV0cGB8DFg8uKVpoaw/orig',
                        width: 600,
                        height: 600
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/172627/market_gOG6LV0cGB8DFg8uKVpoaw/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    warranty: 0,
                    outletCount: 0,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/ZKx7IeGQUJdloRvhBqI1ig?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=XKYYFmeg8N-TfU2aeLLhsqKRQ74dVpUW9xVg0BI5gtchJRkTGTqXP1kgh6YrjWObW38evsV0sdt22nsFpa-0O3B1vwnQBItamodLoQ7X9nkjDmY_Va0FY1xzxBvz9NbC&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZFEwnsFeUmHJxUZjMfY3NVJMNtXzK3tSI5uMGk4BQbQNw',
                    wareMd5: 'eXPpBk4nR-Vew4lzhYvpjA',
                    modelId: 1730875803,
                    name: 'Монополия Hasbro с банковскими картами (обновленная) 6677',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8medeQi9byJrN0U10jwcsSTilVN34w7q8tsXIdCSDEmXOG-TVA0vS046aEkjlOaezhDU8YmRbNdD1WNWzcBPSIr4Pe4lAgD-Nx884BkSKmPVO2IU0Bujv9JPf10ol6OfRvBgY2IJWbOBd3KDqFPlv79BlzuHY5cbTPyDrGbyMbTuoOAjidHBZFOQcbANlGaM-lrCXDjhfJp38kBJqwhcgSYqBRJ2oz0gFY5hX6r4Pi4b6PSdU7TY0xHAwVf_NI1QPLcMZiArIbDV9EiYCnvwSpg7wVJ82UuFEjxSMP163TQnsqbrmjSyjFhMSFZRuIoQlRbzGeYf68zd-hrZwzcxK894btildRD77xyMO7YBIZNf4j3SaK2jEocshu0aIBDxaetTFjULdPYFByOG1AvP9-HaCGylPytQ8YKyBlL58iq24MULOw04Graqdpg3c0rh7pGS630Q9Uomx7ShsQfzCd6HjpNtov9lF2nJqaERhPOj0CzvS0sTVslNTXaGVzcHuoYuC3A72hdLSJiddbhbxJexrDiO0T80F8EHQAqxSl3ojzZASp33N6bTZG60TzlY9SGIFlqyEp1U6uUnsyJFKNumeE9nVDHFQ8piMhDLsx2zXjLiCJc4fJyq7nD9PFx1ieR0CyqrWjitLiZBKUJ288Zbf1DaP8vfwqBCjOaLAdKKGSSS0r6z8VlDMNN6mr4S3ge35YX5QsX18S6Z0DNUnDZtjonLf78ksGGhKg4wBgrTAMTlnEcTNptXvq2WKrdBgA929U3Y05VLnUDs41WlIdC7ZYVYpxUdPJBuXTYLbRlREVWZF9SE6rScJIk1DpfMS-5WsA07DA6QRznDlMyHCSnZ8xwhjAMXbHjhmalYqGtX0?data=QVyKqSPyGQwwaFPWqjjgNtydMHNnehHqr9s3lnVzTSgjShofJ3piqhKgRrA-1gzNVzjt_kWQ1vLzqxSbNUoWTrrc90L5uUCrK6aaG2ReI1XzwtvGjhtSMXVSNyC8dV6lXfKaT_zlx3Yl1ImPcKK0_qsUPaxDkyhUFoKnxKGFxKdr-G0RZZxFi7fyjnku9VXZkrDHX2QU__D8oAMu8YenJr_wzS0vzLiuHd_XqIsoyrenVecZUbXcvRZJy2IFVwxdsVT3D5r1-ST5a-Ga-GxzFOltVqsJKika7fN_ZyPoYSlgthjHQalP084_yjaCSF-S_gpbtBwEaf4XVwEybJynZqpkTej_lvWw5YDIQuQFVoFX17vfnPoYun0sr82MeESBhmDlTifoiROKqFgcGHwUMSPjl-shuUS7IYXzGG1gib2Grf24eKnHeQMtF6kfgdNPxTlIhpFhFggfyM-BdV4w7qzMqhgl9vKmqxDGF0kVs0OgCN4hC4ugtZnzkzoZqtv5rZmvrFs73hEV5AtkpRTB7_bcK6ckbHS06ypRnXxjP4mRSXk6TdouRhxCX6qbm-lnGO-7x_ogqjPXl7VRqe_yDWrP61mysW8zd9Trd0za5lImM_x0gvK_MvNMJZObZJaFvzsyMQRKs1ND_YgXjs91Ek_6_F2CBaJhlkuKhrBqGuTqAI3nhUfP8S0OMdjk2FCnBEEGPn5fFtxuNEsfXB6n2g,,&b64e=1&sign=8d709371516e77eb81bf1d5a659d72bf&keyno=1',
                    price: {
                        value: '2449',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 1672,
                        name: 'TechPort.ru',
                        shopName: 'TechPort.ru',
                        url: 'techport.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 53994,
                        regionId: 213,
                        createdAt: '2005-06-28',
                        returnDeliveryAddress: 'Москва, Ленинская Слобода, дом 26, строение 2, ТЦ "ОранжПарк" (5 мин. от метро), ежедневно с 11 до 21 часов 8 (495) 258-81-30, 115280'
                    },
                    phone: {
                        number: '+7 (495) 228-66-69',
                        sanitizedNumber: '+74952286669',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8medeQi9byJrN0U10jwcsSTilVN34w7q8tsXIdCSDEmXOG-TVA0vS046aEkjlOaezhDU8YmRbNdD1WNWzcBPSIr4Pe4lAgD-Nx884BkSKmPVO2IU0Bujv9JPf10ol6OfRvBgY2IJWbOBd3KDqFPlv79BlzuHY5cbTPyDrGbyMbTuoOAjidHBZFOR_uNwMeG6emk2lJ8R-fQxQf3Zsl-mg9j_n-UHyHILXlS9pI9dtnhdR4vKjqcosCSMNu3Kkm0l97GrsF873F3qG-XdZuTWswY45a8gwgfvkiIf75mvUc4prcGKGiAaN-Y7f0hyBekEzzDmm2GX5ikkKWp9IcDKISuOl4oL5dy2UEfGRHUgUJ3TSrI2Vsej4EfzP9eZpFjf6MIIJmEX-UOY5tsgUrUmFtNCavPunUi6GhLLFz2E36E9yNqxUpulR1mblPS59dvi9P6gTcLqtQVcxf7r7Qdy7W_A-_jwzeWVA49eRqWdbp3ASdbomxt4blflpxO4tiqZ3mIU4Zj3IIcjPGd4nTsRzKgXrgAGtsjuKcdWuUcrX6cVz8zT5lEleSG1ZSAjVIaGKLUjoOQBqrH5xqMnomo0BD9aFNp5JKOpIte11kFIAkoDorrlBhUV_HN6TVackknzNjCmO0-YTmeumgcXFCvOLoWcEdkX6AK5t0-exGNVAycx9s_9qJEZ29QXJ7_XmgYGtYKf9EFjOVsOrPTZsIQgJ9ROxUbcIUjiTgjeHeecUvBOqlieE4-oJ3f1SoSgbeVv5Ol2srR70heLieHOMtD46-lo0lHP6Qd3pVq_eyTqyp8h72h0OoZQdoRAvxCP9_lmjS4ePFi36wHp2KSPcLk48ykUHyppSLx0QPQeGbKvuiwkC?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8uoaKNyzfQTQ6874-0NlPNBFd04elsKBeKU4FcDqHRe0qbARR3ez3FCD1CRr8LHdMPaCoKaE-QlMbTy8TnO9gCUOoGNX4YiLETu4BUes3GQnAKrAkgLiyc0kiadP_7GlnLW6DVM2M4b5GtFRSE0mRZZDOt8I7FDkkbX8FVSnMjXA,,&b64e=1&sign=790c043729e70fea434bb805d5a67c0b&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '250',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '250',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 3,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 250 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/235547/market_NGXnossw-d3Tlj7BGSJ4SQ/orig',
                            width: 300,
                            height: 300
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/235547/market_NGXnossw-d3Tlj7BGSJ4SQ/orig',
                        width: 300,
                        height: 300
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/235547/market_NGXnossw-d3Tlj7BGSJ4SQ/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '228-6669'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '11: 00',
                                workingHoursTill: '21: 00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.656631',
                            latitude: '55.712142'
                        },
                        pointId: '154545',
                        pointName: 'Пункт выдачи заказов - Techport.ru',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Ленинская Слобода',
                        premiseNumber: '26',
                        shopId: 1672,
                        building: '2'
                    },
                    warranty: 0,
                    outletCount: 1,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/eXPpBk4nR-Vew4lzhYvpjA?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=XKYYFmeg8N9qaG8ex-vaDTGq2G8p_OWIccU4Z60Z-A3RxQBkt7A_f-metbCjKPRI69REsBFISlGWMhXZ8SolG5b9V2P-pmrDeXnRL8v5LJjtKidY2FJh3YD2A2kAM6-a&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZFztdPyCAhDCwOf0Q6n_G-9Eq-a6iDN5Y5BsGbZmUZrmQ',
                    wareMd5: 'dZKxN6JsZg55Pir6OpdR0Q',
                    modelId: 1730875803,
                    name: 'Настольная игра настольная игра "Монополия с банковскими картами" (новая версия) (Hasbro Игры B6677)',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Zu0b6Qjj_JjVwqJjnCim6coAUZmJc22D7ntbg1i5bkq833yo3AAcPobAtAbOKdm4rQtzNbNvFXUW5NJkORgSlbKxMrGweA-PqRPYvttAHFKalX4eydzQEuUtlNk92gXIHpDZ2vs8luIL-ndYiXs63tMbOsBLSEo8gp6-yXguGYGwnu6mMYBO6teoLeKuoRuj2bnhr-WmPugY3P_4fYfgbDCkFmEPCjTuW3A5uLE6ZLprrHNUgJVm_43tfkaz_8nZb1tVsOnxcKzD9NLKhv53ZfQxycmauvZhw52wdaQytliudh7RUsgdSiOW1ZMDmIezsGT-r5dIzb71GDns91-OA-2oESHXUqyZxiAUxt_rntWiCmKhVblCXaVdT_GnlI1-ED58kjRqVBlgjkB_Zsfi1rxHfHUHtI2OwSHOP4y1iZS20cUfbGfCjR7z8WKwNxE2Hw_tHl7aGYaptJYVoEAjxmIEmvoBbaVidnQUhIr0z7Xc4kKnKsgfqm3l7tz-MTs_XGJk-87iGatZZSPJ3L33M-UU40xc6YrtIbBTHSNVzlDFZCJvw8J4q6VFfSm-CtHaxgsnczeTF7wr4aXbFbuYgGWnkNMjk0WcrsapZEisssSeoWJ6mBAmeOZ1ls6yB2GjUsvI1DaUzIPi72x2aZA1oqvqqauGpQhrwmw0YhZj1HEVtEdPkRBp6pMZWZHe7lU53RavV3kpIAa?data=QVyKqSPyGQwNvdoowNEPjT1rTd_mg6_jLqGUgfPI0BZlM_LB4VIRcRIAizBsb6YjKES3bHYuVtS2T59tF_Cq41aDz5B58B2k5n85XXXkzwV3i3_U9XrFOGfXfklOIpWAyZvinYQimvfYTCo1rBkHv6vz6SmmSjjyYZ_CMgFQSqrpfyrt0f2DQFbyZ0QKAFrcaJseNHlpJr6m5g9hVVJJ1Jtqi0ZXdA0S8gNx0eQWLJo,&b64e=1&sign=a563243635c5b4ed1746644eff621118&keyno=1',
                    price: {
                        value: '3079',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 359282,
                        name: 'BabyBrick.ru',
                        shopName: 'BabyBrick.ru',
                        url: 'www.babybrick.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 294,
                        regionId: 213,
                        createdAt: '2016-05-23',
                        returnDeliveryAddress: 'Москва, Спартаковская ул, дом 19, строение 3, Russian Federation, кв. 60, 105066'
                    },
                    phone: {

                    },
                    delivery: {
                        price: {
                            value: '250',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '250',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 4,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 250 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/168879/market_GVqlSK8hkCUom1NNqGJ-fw/orig',
                            width: 225,
                            height: 169
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168879/market_GVqlSK8hkCUom1NNqGJ-fw/orig',
                        width: 225,
                        height: 169
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/168879/market_GVqlSK8hkCUom1NNqGJ-fw/190x250',
                            width: 190,
                            height: 142
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '510-2061'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '6',
                                workingHoursFrom: '9: 00',
                                workingHoursTill: '19: 00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.676987',
                            latitude: '55.773672'
                        },
                        pointId: '460918',
                        pointName: 'BabyBrick.ru',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Спартаковская',
                        premiseNumber: '19',
                        shopId: 359282,
                        building: '3'
                    },
                    warranty: 1,
                    description: 'Представляем вашему вниманию новую версию настольной игры "Монополия с банковскими картами" от бренда Hasbro!в обновленной версии игры Monopoly E-banking, как и в первоначально "Монополии с банковскими карточками" вы сможете использовать для совершения каких-либо экономических манипуляций банковские картыДля удобства в коробке с игрой вы найдете и специальный терминал, который поможет вам отчислять и получать деньги за совершение каких-либо покупокТовар поступит в продажу в самое ближайшее время!',
                    outletCount: 1,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/dZKxN6JsZg55Pir6OpdR0Q?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=XKYYFmeg8N_3-HEQ4bqMPJqMK6bqXbboJTXIBMNNSIIrZxUO2cSeN37j466c3Nl23L74CbcbE0qEGAR3ramy71MC6ZbLYbt-9m_Vs_vQPBD2YUsqga52HBmmEn7rn8IW&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZEdIq7gwuwirGQXI_i4vtlizgGvs0FuBWLmHOTHuCIPgw',
                    wareMd5: 'RfQkjlkkRYuF1h8vz2CxVA',
                    modelId: 1730875803,
                    name: 'Настольная игра Hasbro Монополия с банковскими картами (обновленная) B6677',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Zu0b6Qjj_JjVwqJjnCim6fXWoUYwzPn32LHpA0qtUlRxybvzCT-pJc80AvVo6zP-C-vB7O8l-VVjYMCKD6L2eqIXZYhMHF_G6J6U8IJzzT5ZqSg0FxRmObKSGvUJCa3GmzwAuxqSpQj_vimwiY7DvH2sw7OkAAd8_WdFrZLC8vYc0b30KKFAYg2kqmzccfX8Q4OkfLMlmDzuTa-yI1hx4T5gW6J3nigI94uTv6AP3wQgACfXBfSIfgieL_SpeSz17NdeXidBv7ZMxw8-AUBBDbBGJ1XGUcK6vboM8xi_SlXPpPGppFf3h_nYCnePJk3cR12_D_NTSy73HRXtpoxHktNz6HkztpdNVUK-j2-vgOXT2D-tzkLsjSYw9gc79o4eZNlP9gySGBgzRISKAAMD0I2HTg095ZaaeZMw7U3xHXwWxadDRiSEQRT3ZKunAdFZ132R-X5fMXYcKrTCHF0iET2Zwx42PGJyYMT_Mn_pKeYqIdAC0ftYxT5MN93SU1dv1s-gFvos4dsPRjgusTE2TW6Ykpwd7X7DP8yH48V5EUE9-lkM3H4SrXk6S5AK6KJ_Bv5OZ8OEibPdpx-qjUOUpf3-SxIhpzWr3gNcCMx5HcdeaVhtbzG5namMyyS7KGZ740fmGVUSUNr53OYeVslLQrFWCaV8SWPfqOju2x6sw95S25_JZjLTn4Fxf50__gWrT-99I9JDwFC?data=QVyKqSPyGQwNvdoowNEPjfkGoD3gnuOoZe_UdPwWrBaY01M-YstNhSGVtzZLzgBZgX8OMFQpr6OIs9YgIk7mdlbIjUkTGHtjPNLgyzNc5oS4fRlzRDQtMQaysi2p3MNcRyUIqpVh58XiCU1z8-u0TYDRcUzoiv4AFEhHshfdjHOrCrskFp22rZCQ90a3uIixjgsp3l3J470ZbhWqX5YQL4sgDrkND-CmURw0SeRYMLZoSWSc1sPc63KubVqo9gXPX8726saHm6doT6SedGkirw3ZdUBywTrB2mUKxKFXMUx-Ta3CdeAKntrjtSE6o3tR4zd9J6q67Kk69ZiMVyKRi-E1Wqs4WBMXajChtQ0M6ltIZqSIDnb5PwFMAYHGZZ2YUlj8aTiV6FmmHXBvzhuyHZ42uE1ODk8pImxMFHdpgH47qNA4awFyFHqiMEzZnxLOIr5bBuPDHp23SQhnQUVYVXnImhodM1S1X24aC7U0tWITfVe5W5VT64737iwq1MQG8ScVgoJ-AxAvSxh3kMzfUtmrI1vD6VtGcK2fO_HoSUTgdlzokA6qUZc1VfDOfeXgnwO0N9skHjPoi9XFjGPvqg,,&b64e=1&sign=7a6e9834fb91aef9a6a1709017f0d915&keyno=1',
                    price: {
                        value: '2380',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 43150,
                        name: 'ОГО!',
                        shopName: 'ОГО!',
                        url: 'ogo1.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 7864,
                        regionId: 213,
                        createdAt: '2010-07-27'
                    },
                    phone: {
                        number: '+7 (495) 229-56-53',
                        sanitizedNumber: '+74952295653',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Zu0b6Qjj_JjVwqJjnCim6cOxJJ7gWeYm2q14BnaDpgXljsPIvy9a7s5MDFv9I_PnhezhCUUFPIKSoHnzWaY94Bth8qn4-11ij42Z641xNL4uiy_3k_tcRhOO2m4J9VtUZeawjZQUfYGJt8Abhp-TMgoO2XYrLbnxdBXKKIxytNTsL0nDYJlBnO8PcRq_flNKTCuMxesOq8yV3v6-I371gYdO0ZzAcaDSvGnPn4xuRgoaD6qbWsCloI7ibB9_pXQfaE-1F5YSoEEPQYyu2FW0CRiTD7iPRKDOqb3ouZaZBq5oBapqND-Pt-tuKW68wdfj8PpNlHk6i1ff0d83OGwdt5f_4d0sZpoVThmSiehA9FiX3L8VNXd6HSI4PoHEu53AlCfuAiNYLF4Nk7_gbV0YQVlXsEedzd73oI-pSRaHBBvrd-hUVgrYxxPIIP3lgIjcZeB8Fo5FMPtxfZZPIdhf7LC0daBbaNk3rRN9D-Hcf-ELU1sxi4DinjQvf9Qvrk2C96zEhqWNZEsJ-yf_zpUGyqeHKUurYnRjmifTD5O2bydDOV8qktFFtfXD2fOEKJ3YHfDDMGPl1MEvy2T2vZOH3rcVMxgUvdxs-9-WYFkGVn0HBI8lgXL8LTdq_2zm0lHIPVhzDA1Ldq_B02ovKaATY-NoaAs1Ij1ty_BzvDSMT6W5EhZFAt4o9xSiSt3jXwimSmbYpn9g6oG?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O89jvkhxX46vXdVv_GHCu9QCh5FwqjrT3UOZTLNCVuHAuH3utUc6NFLw4OGZjaId8n82PB47njFWgy6EWDotkM8bI5YKg_P2cby0tYjVnv71SdSoVk8RfdNYMFVBIVR64fQiBCvXRK1ZCgaDcHEUS0_5quV24c9gTLkSRsH-UnXoxLwqgrgJDh7&b64e=1&sign=ec951813c6062c7c55e7589040216192&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '399',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: true,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '399',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 2,
                                orderBefore: '17',
                                defaultLocalDelivery: true
                            },
                            {
                                price: {
                                    value: '450',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 2,
                                defaultLocalDelivery: false
                            }
                        ],
                        brief: 'в Москву — 399 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/235547/market_TYjHlF_z4M5YQff1uhj73A/orig',
                            width: 700,
                            height: 700
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/224461/market_pBRKZUI-WTKtC-sA3PF3yg/orig',
                            width: 700,
                            height: 700
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/235547/market_TYjHlF_z4M5YQff1uhj73A/orig',
                        width: 700,
                        height: 700
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/235547/market_TYjHlF_z4M5YQff1uhj73A/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/224461/market_pBRKZUI-WTKtC-sA3PF3yg/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '229-5653'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '22: 00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.559883',
                            latitude: '55.793227'
                        },
                        pointId: '280904',
                        pointName: 'ОГО!',
                        pointType: 'MIXED',
                        localityName: 'Москва',
                        thoroughfareName: 'Театральная Аллея',
                        premiseNumber: '3',
                        shopId: 43150,
                        building: '1'
                    },
                    warranty: 1,
                    description: 'Описание:  Представляем вашему вниманию легендарную настольную игру - Монополия от Hasbro обновленной версии! Производитель окончательно отказался от бумажных денег:  теперь расплачиваться за покупки нужно банковскими картами! В целом же суть игры осталась прежней - скупайте дома и недвижимость, стройте дома и отели, зарабатывайте деньги и выигрывайте! Монополия - замечательная экономическая стратегия, которая способствует развитию логики и аналитических способностей, а также просто станет отличным поводом',
                    outletCount: 11,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/RfQkjlkkRYuF1h8vz2CxVA?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=XKYYFmeg8N-7RuWnChB_t59jRrHFzQanNPAt7sAB4KHqPShx0IjECa7SUHl9rqMyjDuDF9Es7e5hMtN8vzhu8YMalxlFh6KJMMBahQSUeJim_CbhkWF-PAFB15v_woW9&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZE2mYNSBviHbuggeSk_v1fNKY8eKo9KRDgCiuPPejC8LWgxqEyqeYs8M28cZ-M5tIxJtvhUHiA_Rlqa6I08wqzGvePSr0O3MooXShTvgt-RIT_aV88QqdMU1iE792n8iBAtB8dJpkRjkPexzuWPoakqHMktsmNpwf83AmdJnmxcUforeqiAneJfl4T11eDNf9-WmFu4d7jtIYT12IfiMmaXNhReYqLdx-VetVW914X5B7nQM9S3bg2dVNNyunU4VZ5TcpZuIZ7BM1_Y2IeXHuCO3UnoHzWTYXk',
                    wareMd5: '39QCk2xf8SQCn5k1akAJcA',
                    modelId: 1730875803,
                    name: 'Настольная игра Монополия с банковскими картами + батарейки ААА',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mUOjV6Z7ajow4Kh8wH5Bm-cpmdVH25OT8nYfk4locNCGm0KNOgi9sxw6W2X7fUUs4WcRLbcKuNBEdLFvKt1-Y6m8eBRFrgrEFYJjA9R6s-c7_bMO5fH0LqN-Zz9STFBKOH04d6LT6qosFSy_SdauJ0PsER_bHB8ZwpKk38xll89ca8WhycWCmczq6ATWjeDWWUl95-Ux1cKdIpQD1G_cdlb3tVeW1TjPI7tPH02nJfaSf6JNElrJd9aHlGSTN6V_aeOfZIlShLWKt6IfRrvYnRvoXKS2EpOQ5Gvr7d7_skdSL5a6VVZmrv_5T5sfq5KkCbbELuyM7a2U9hHp_gNEdQ5zY3Y6QC74hLqFpXoPuwhIlH7wEiImRib26UZl7hT4qr1aV2NHrze4hxNkfYrKqrjCkLtZjx7rny59YFeXBjtwbv8Dt_hEm3LwGAL-HGQmmLoHmf9-4m5ldphWoM2NET1mzixBphystEwEtXdUmxpeBAV_ydMw2rAMzJGVAS3qsyqx24q9gWubAC13pPbzer_7dIOTlZIqwC5bVU_oabbr6qQB9K2H9qkKUiEK2MXTh7lYzkB9jts01o92se1YqaISZkghK4BTMgzrIBK_guc03p10aTNVT-5qHw1CZ25WIWiZZRS0kLIWmesUefI5zj0OeGp3-tlJIHYHbVQ0-SR2HZw9_TJZqRgmiC2_ZNSUwa8qpsZSvzsX_uxb-0HZkSbKfut8-8kWAeyGXE_iaqGWv1jR4imCKdejkuacOZfPUzrFlLg4K4O0QTxHPMxapYUyYid2sbnMp24HBvT1fws8AAw4fycldubbqQtcPLkzcoJyMxw7TuQ3B4lAK3mKWOMUayBbNL_6f5oyadzcxv0k?data=QVyKqSPyGQwNvdoowNEPjWDqp7eX5kyi0uGW3LKi_s3I9FtEbo-WmTpxp7w_pMxlhsjF5uAjdro1qTMS-7jUbHz7sxW5s84qkqZ6fsspp4GV-yU9DgNmTMvHktQLRy99BWnvGti3An65XmYrzyOVFq3LmDPd6NiaFbe2tVSt-X8_Rq9kwM-zdGweUZ3trtDZenCYX1mPA-P08UCwam9rhIQWOD4JgyeHu-UWDIupjKu4i7ZU4V6vj7tkjGNppG0vP47SO4cgR5nKJJkrCEzOadsG79xn7iQrBi-x5rqyUFs8rg_1_zqAl0wq5YdK0dth12jNwPHqA3M4pEf19lagglmfGQf6kU0yTzxG5YCFJW0YXW2DloJPDCTM0OtRRlNAx3FNKK7ez4-ln2AxSQBmq8c-tvj-uGPSus9wAiskICaHWLvrTvuhpseEAwCJPNV7pezKkIhbH1yRdiID0Dy3IPWH_E439OZv_qe0q-qLI3qiVnphadijh9LRuENVai-Ck_SD22ALhOfOyh4EDOpOw63ACGt239NIT6WZmrlUy1glsx2M4siT-UrWeE_zI9Rnt-jz5_QRc-kof2ynl0QM0MkNr8lb42RckSOpH7GzFPA,&b64e=1&sign=4b1923d30078a27e4676bd452376f9c9&keyno=1',
                    price: {
                        value: '3130',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 4014,
                        name: 'Игровед',
                        shopName: 'Игровед',
                        url: 'www.igroved.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 452,
                        regionId: 213,
                        createdAt: '2006-12-20'
                    },
                    phone: {
                        number: '+7 495 668-06-08',
                        sanitizedNumber: '+74956680608',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mUOjV6Z7ajow4Kh8wH5Bm-cpmdVH25OT8nYfk4locNCGm0KNOgi9sxw6W2X7fUUs4WcRLbcKuNBEdLFvKt1-Y6m8eBRFrgrEFYJjA9R6s-c7_bMO5fH0LqN-Zz9STFBKOH04d6LT6qosFSy_SdauJ0PsER_bHB8ZwpKk38xll89ca8WhycWCmcx1yG4O1g7cs5Iym-f_6e2ZjYfr1Rl351UvQZwn-F0yMyM9JBEOD76Poci6bGMN5xkORuIgDN-JaE9KBf4z7qjWQDHxQiDY0Cpge4DZa0WODH68diXSG4gwYucu825DoVDFAF_iKRymP1C2c6ccMJXYZR-5VuzZs5p1EURa5oNrLiu2hQ3OprfmF4dHx-tIkbBp-vu5xLYi1bruUyS5Al7GsBu26LjgBHlPtjW8Ksl6InzzdQCl-FjqxnqM1gA-4MtGa1SN4ia8-jK5bwOcFCJRAVrkuk8CF9ogukNK6Q14VWs-s8AdUdNxwDnZQxjYK_FAua7jrzI26Vu6pC2MVls1kgmQz2aX36g51n8oK_ieOBR6-3StfYJamxN8vhH1AUzxQtpWX1hmBHzgT657lHm_JmZchi30lI11EyyaYHKkmtN-vXTESB9fRa54QHI_OUbK6k5ONL2_s-B7KVwQF-_byqHrGrDgcvlXw9mfDFITymczOK2GqwPGHpFcIO45NMzMSVNrBNtgkGntdc2aloTAxtZSe9sUpGPG-tyvesk-jqV63H5EW7G-g09wZThCRFB0scb34-p4AjjwSr-EfMq_sWqFAooS_NJn6E9cMIKa28h6Uji9_mrXGNAxh6GfBVeEfpMESb5XkHI3aJoPDsGaJRE26m4pddAg76THvZFUp0H-_HUMMpP3?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8V2DbUaXB5zpqyEg2D0gex8Rj_OE_EbAkcIEWYEBWaYSSkmNc5981oohK8FqyDcIRKUWg8KwFfSVycwizvMX-1ki-r9wfDLmPLRU9e0MKTFMI9iCCyJ6NlH7pnUGGKDNHtWOMRK-XdRUZ8HsUIq6HFJSsoDNXa_e72KmUjWCoKNg,,&b64e=1&sign=56fd1f465967d855eb6397afe6c6634b&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '149',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: true,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '149',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 0,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 149 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/167132/market_Y7OkIqNg4tIiAFR8D15WBg/orig',
                            width: 190,
                            height: 180
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/167132/market_Y7OkIqNg4tIiAFR8D15WBg/orig',
                        width: 190,
                        height: 180
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/167132/market_Y7OkIqNg4tIiAFR8D15WBg/190x250',
                            width: 190,
                            height: 180
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '668-0608'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '21: 00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.62912014',
                            latitude: '55.74161337'
                        },
                        pointId: '283821',
                        pointName: 'Магазин настольных игр Игровед (Москва)',
                        pointType: 'MIXED',
                        localityName: 'Москва',
                        thoroughfareName: 'Садовнический проезд',
                        premiseNumber: '6',
                        shopId: 4014
                    },
                    warranty: 0,
                    description: 'Покупайте и продавайте недвижимость, стройте дома и отели, собирайте арендную плату, платите налоги... Вас ждёт успех! От классической Монополии эта версия отличается наличием пластиковых карточек и банковского терминала. Все расчёты между игроками ведутся без бумажных «денег».',
                    outletCount: 5,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/39QCk2xf8SQCn5k1akAJcA?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=XKYYFmeg8N9DWiUFsGNBtkApgrAESLBTtbFVce316Ys8P5u7aTO5ejfgCPvG_pQQZHKBYSdvsM8sDMiMucUkzDWBoOIEeHIrRFynmgQorWdtxhq7vs6ym0r2jP9vNPHU&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZEvKAPQUeEgWNLX55qcfkpldUwZS9UzeeiPzZ8LxsD1DBoQYYhDU8DdxjoTx7yvaorYxmb9TJmPq1AREzMiuQAursn2rpFBEUTPVlNmiUdVN3M6CZrOh2DGNu92SQEzSBGV8KV4sRrZ4lv42JyGU1lqAL8F8EICTsGSbX7GfAIVpP0MvmNJXhDnDwhhbKkIywV8kv6_gjCCqLuV1pki0ZmK9QUKOShutj6A6VQmNHJw4lEDb0SqXbLW0RkJkQ3oKzGYXoY9waGTxPQBNbItl5Zb7OmnNtwAkRc',
                    wareMd5: 'LzrQ-DSHdPqWR7uoROnrWA',
                    modelId: 1730875803,
                    name: 'HASBRO Настольная игра монополия b 6677 с банковскими картами обновленная',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mbgORdUsQ9WNDz7jF7kYPNvMX9t45rEk9cyur5b8WNB0hFJqkLSyLFyz4l2jiSun8HOlClkffjR_wG5tuyP4HpZHdUQkIp-YnV35VCaMK9J5w6ZAX2eb5k5O-Jnuc7EVCY9beLQ1RSi4qty6b7vUw0JTw01j-Q-zhAesM2HE02LyFj9LZ2b2Ra4jawMpw3KM1juWxc5bAsgr9lU9aYG6lFcSWH3XIxTwhFsAHye9rJKPsNR2k09FoMdik00CriTaYcnl9336HPmBktoX2LEsrwDarDGw02SYcON2LL8bSO0-bWdx18N56a5KEUMKAv9TGeNpc8BcTNRL_mcjKJGYaIuA2d7KgN1n_Zio6G4HVBky6NOq-8sA2Zvl_g2Dgrp7wQ982TbTbh20uAv9r2ncUcKcgxjOFM1dKACA6EeVuiZ-YS_3A5neuyXdn7pr-0qOrT_qgcwcDqcFGrfy7dKqPdptnXZkIGoppualOzW7OeJv9uzzPYMXBEJa7QeiXQSfHKMrAbuUAgDsONfRd_q0EWdE226eL4CtaaU5S6ktBkSVLWbrMF7mszBALh2QLHvEp4NQMNzznkE5V17i7PCtQCQCm5SZZo6qjAwWi-5u12zYGxTJbsqZgV-NPDfVeyX5XnCqbXQqn4TeRIb-ozbb8I4Ps6zqhB3hBpcIsR3JElzckT8oCxiiGnaw7s9oyk6vf19ltc562AnIN0zb18hDUKBT4Hs1IGY9OjS0Pyrb16V7gBGofr0UJaynBuEJw3_M6dfDh7_SHyul6k1DeX_MHCWz5-hfRQJAdWMUCkgQTyv2IVEKwpyM7rmvhvkwicnGJuloPki0ErXQQwppgLA_boo8GtlPXIWJLie5y1-TBUBp?data=QVyKqSPyGQwwaFPWqjjgNjnwQl5o6nBcWP6_wdMiM76pL8bA2jMgb0tDxSSQfYpVujvCwSazZX-IslPgsCUnCT6k91fxb2mjMRv3UX2ZAXCBd4mlPAxKIqUZtxdebOFCdxnmRbrb0ihSUequY2j7zoRd76vsMcnRMp2_GZMzar27VXW_VpD7Hre_4dNb4V0K1xLk9NJ0ZUV11ralHrjWEuMOmPg4rlkDYFD4J-8mawjPtyPxeSD7NzVNlajP4has72G933iQT4DYa0GNOU4GvsaHTZ0wBDvsIFMz0xvsG234I70gdn7ArWUut8PC83RczBFeFJrHqPklCENH1geMyDahYAMJQZDj9uHmDhRTxwCGBZBfcV00JwI7vLzrPk6G423uSKkUZTvSMC-FLR1GTIVgd1EN68WiMKXDNgBCOs-WIQ5A2uq8-LS_hH3UVlFDyK_-qAu1P4yBI5VenIKpsqXSimbRRDqN38yu4b1LpL6yJJB3RiWg9F9PDkYDOrrRtrShgnk2Xr0,&b64e=1&sign=5a06c21fe96e5d4169a175be16ca5941&keyno=1',
                    price: {
                        value: '2452',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 1497,
                        name: 'ЭЛЕКТРОКЛАД',
                        shopName: 'ЭЛЕКТРОКЛАД',
                        url: 'electroklad.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 11325,
                        regionId: 213,
                        createdAt: '2005-03-29',
                        returnDeliveryAddress: 'Москва, проспект Мира, дом 119, 129223'
                    },
                    phone: {
                        number: '+7 495 191-89-09',
                        sanitizedNumber: '+74951918909',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mbgORdUsQ9WNDz7jF7kYPNvMX9t45rEk9cyur5b8WNB0hFJqkLSyLFyz4l2jiSun8HOlClkffjR_wG5tuyP4HpZHdUQkIp-YnV35VCaMK9J5w6ZAX2eb5k5O-Jnuc7EVCY9beLQ1RSi4qty6b7vUw0JTw01j-Q-zhAesM2HE02LyFj9LZ2b2Ra6_Bq7lEj1fpEkX6IaRPCPFKuX8epQuzVAqFa_5-C1YwTibm-2-yrN2BbysDVEU7oLktZhCnGdHMAAhR9Ts5fU6tLHLZekoArrJHvXXxEXCzix9Kc0bHfQ2nCfJPXFc2ubNhP19U0P7mIh3VSDE9GS5lbw9XSinAk8zIqwc18ShD095zhaPyfwSlT53DVw0dpqkaoFfu4Fure8Pr5jHgPiRDT9Y4XWlUbMmWXs3SDqS1TzpdARy9aEQl7rEXMtzwMf_UlwI_B-vYQz7I_XMuyLj8Cjtk6qFRhKsCvykJNKvVeNNtFf0QNazUm3pJoSmB0f3tfHzjpiceWBXbZ894LPTqC3wvoXAgsFeHz-9P_oFfdZI4-iE0LlVigvbnQhI-8v0WjIfXJJsGrrG2ADTjWh2NIy86e4scvwHinnBOkhMjGxmwxlLks3XdGF0C8ZZqBijVU2-MXsiaw2MquevIbmFAPbHSxVgrcXtwY8aLBWPU7uYxuVgWDsjJaFsnw14BhZTZBINMoMbSfdNnPXgPmGQcDMYPPv2E9lEKGvo9eBYO9DJyHvkvLixIlu1aejdauR_GrZGkZU8xPD2ylLisj4zRPaVYbS4jMXb-jxfMkKpE_Md64Y7zLyLFU9ofzjZ1p7r4sSlMvSaBueqlsrvlZVN4XW2XV5oVdA0DuQarqdj535uNX1Orlj4?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_x3ofEJHmTDlX3dpX0rWwvkzCoHQa7eLUNq304588Om4voYeZAJtsPc4JAWYmDz4HMCuUbE3PnK3Ot7pOKgGxXKyD9RspqiDEPQzULdL_8P14KHWq5cSkfBAs1EmRnHgx9raSakeI4CAjBPSvoVg1DdUs-Z3xBZ2ryOj0-7n5ntA,,&b64e=1&sign=0d5335b9a1174408dcc97379df88effa&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '250',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: false,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '250',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 1,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 250 руб.',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/230492/market_asdISi6Ilxg4nwVcUdytPw/orig',
                            width: 600,
                            height: 600
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/230492/market_asdISi6Ilxg4nwVcUdytPw/orig',
                        width: 600,
                        height: 600
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/230492/market_asdISi6Ilxg4nwVcUdytPw/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    warranty: 1,
                    description: 'Обновленная игра Monopoly E-Banking Артикул:  B6677 Бренд:  Hasbro Возраст:  от 8 лет Для мальчиков и девочек',
                    outletCount: 0,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/LzrQ-DSHdPqWR7uoROnrWA?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=RO7b5ItwATbY84c-HECdqO7Ex9Qxe1V2wpNUDIaaDDWpZ7l0duKcS8zjiArB3TccyevhIlvyhRrG5nZnxeq0jafqx9l99Dee4l2aCdKJuHCjWizkGNeVGRuO8zlQTHo_&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZFgAWN5N-4KRtb4X696jNlt9kKsVbJsVAt2UXwzQqXTf5sy9_f47VQRrNJAIvYD7YZOzRj7eNpR1ZoXxnkMPM-s2tzfdjvUG5zeQ4P3m7KUoIqvKvbjI6qbSYO3iNXoS02MF_YrnjBAu4ne3BUQy5osVph-8GfvUa-VI3Pa0wMkfEqRz1bJ6U-N4P37mpwJFyTB69ggdW4jZpQtpOBfQZHvpeOelt7PsNyfp9S04ZLMLkCERAwWnfYuB1wCQAf__-C7la-o3ncZZreelrbWbKtCWAvMJMlbry0',
                    wareMd5: 'S7AEh1wk_6-1ypzSRnzY9A',
                    modelId: 1730875803,
                    name: 'Монополия с банковскими картами (обновленная)',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8medeQi9byJrN0U10jwcsSTilVN34w7q8tsXIdCSDEmXOG-TVA0vS046aEkjlOaezhDU8YmRbNdD1WNWzcBPSIr4Pe4lAgD-Nx884BkSKmPVO2IU0Bujv9JPf10ol6OfRvBgY2IJWbOBd3KDqFPlv79BlzuHY5cbTPyDrGbyMbTuoOAjidHBZFOTTk8DQChh-B_sqedfebtqe2dYFw_ZbzsAzJwOvgulNKGoT-g69qDvaCcYUhwf9FfLq7jSjKkBIF9-466wbSaa1DwERn7w3iEhY5133niLsGoyej8LG0w_nsmQ9HrCYUbUgEhxYl1lLlhHEBbawnRD1G3Bi5SN1Rg-MFPdXmw_iqN5_SE4TpE2X_DwtE9ShEUoP1W5tCXmShB4sQiR0cC98yWJZb29BMFJC65CYwqDqa-WPz9g5_6IF1P0armm_5DPx8DZefGTS3ST2ES3Uh_n_2NR37F45wCk3Zx3FfRMVLzkmhyYR1FAUaV6dJPx2F-ohEZKkQ8vuSMRdMbTqm04SMqq-f1t4cVE4r1enNSfziJulp0B2BGRSXrS9tciKT_t00FFnmLKh7Fyz1X07MrKbiuDClcUhejhVK2wiCuj2WzjB2SE0shzr6AabDWf_P913Kqn88tBeUOpPXtbuPdH4tGmSb7-j_ZTuSrz_rOlIyMYm7tyMG_a5iMunSuerR56kBtz300wJpSS_dTZmSaTaIZrEUnL1EkcIlRCPwOzgIdJBpaDp5N-fdVnaHaqSQBX_-lu2SGyfRme-lgog_ucUiG0u0JP7AQbjDgFUwfjpxhbVN493Zq_LnmhnRU6gm2yUDlFd-Zf9Rx5hSEDLgXfQYg_DoALqTW364aNA4uKRvd-P76CxsFfS?data=QVyKqSPyGQwwaFPWqjjgNrtoDExARLxPtmxDf4238RU0pHgkSSkMpzy5b5uP8Gupw6tOvjWIjIaIloMBgisOAhi6bf0BHBwTvfZRI5MJ97ip4vWWLtCs4FNdhB5xNFU-ByYPmo-XbtziNvgMiOmh3uY3W7wSETlgJlhBgG9SAwEOKv7i7psaKJ23HVaPwojKTuR9b3fXo_hQ_-2ly-OCaen-CU6289-K392nAlEtQjOzp-u2mvTV6wrcXP_6BOgPQg5e81JUR5Zzp0AK9BDKlZk3WN35wOA4vO5br8YUiKlH1dRgMBJD-PR-b5uwsQVRWo6xgdr73d4,&b64e=1&sign=a7f608dc3b1d01ba4b47c643e23bbfd2&keyno=1',
                    price: {
                        value: '2961',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 8060,
                        name: 'UKAZKA.RU',
                        shopName: 'UKAZKA.RU',
                        url: 'www.ukazka.ru',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 484,
                        regionId: 213,
                        createdAt: '2008-06-30'
                    },
                    phone: {
                        number: '+7 (495) 640-55-22',
                        sanitizedNumber: '+74956405522',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8medeQi9byJrN0U10jwcsSTilVN34w7q8tsXIdCSDEmXOG-TVA0vS046aEkjlOaezhDU8YmRbNdD1WNWzcBPSIr4Pe4lAgD-Nx884BkSKmPVO2IU0Bujv9JPf10ol6OfRvBgY2IJWbOBd3KDqFPlv79BlzuHY5cbTPyDrGbyMbTuoOAjidHBZFOS5xq4UBGg6qyM_F_oyQg3Fhv77afweg3np9OmSbezbij65ye2668vtpM7iuZAOsTcibeneK0AJEB__vAY_SXQErxXg9GuNiufkKb5jRV206BlGM92QLSaWhH7gpw8sAZiXcWua_OkMSVLhy7gLfuRlY27x4WsathDLPrYo5Z508knpjgcrV3TVqwN3KckfkZb_uG7cuLKeMvQ8O99-WgBP2zfp8DaREe6QLbu3XoQymUNyJea_63dNDbDNzBuQa790CtUB15-bk8V4S0ewVpMLMxtwcHEQipA_YtHjP0tTajlNk1pMiRQxxSuyEZkwjgkB2hJpSmcmVR73vGecO_qYI67DL0Sf6fS1PXz4GVQt2PHWatqKV8qruiJhf05ravxUrM0pEk471n6EpZ_uBPppEUTqo0IiWDD4fnekUYjKUzqORkFWIWefLXyCOn0fYVvMGGS0g_f4buCnSiDNxsuBTQ01pnnJUZPsVz_hiMFozNslbR05OxcMApNVpRhfZl-CVARac8mofnxRaxUz0fhqz4m_pYGCfjT2rpfergFIGnSArZHF-7Zh3z_UDUwT_F-ZzAKUNG-XzLYQXbZbOVuiV5voulXIoy1xn5EebVI5yxCGFJWu9yajxKaIzFHNV_CGi-D4ussOvyy2W_UQ84HDBqp5N4N2OzNRjj9g56-SEhKxNCdLTx9p?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-ftBSprZJW3WCI-VmaDejMUjo1YNyEulxAsCMCGdsOeun0GbV4y7DoS4aTenmtHwMa8YYJIXYqCopiK6KCad9jBZfplSe_txRFUBaLIFbnHjXkzMN46BOOH4hR8pNxCNZB_gX2BGrqX9N0yAiDwFmRGZfimn5gPQAtjg78gw0HrQ,,&b64e=1&sign=cd745859ff5c94aee16ab5c62ad69a4b&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '180',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '180',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 2,
                                orderBefore: '18',
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 180 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/231668/market_q0Njut5xqLiLv7ljJnWtig/orig',
                            width: 700,
                            height: 553
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/231668/market_q0Njut5xqLiLv7ljJnWtig/orig',
                        width: 700,
                        height: 553
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/231668/market_q0Njut5xqLiLv7ljJnWtig/190x250',
                            width: 190,
                            height: 150
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '640-5522'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '9: 00',
                                workingHoursTill: '20: 00'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '6',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '18: 00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.64524173',
                            latitude: '55.73054207'
                        },
                        pointId: '2157',
                        pointName: 'Ukazka.ru Пункт самовывоза на Павелецкой',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Кожевническая',
                        premiseNumber: '7',
                        shopId: 8060,
                        building: '1'
                    },
                    warranty: 0,
                    description: 'Теперь все карточки читаются банковским устройством ( в том числе и карточки с собственностью), что делает игру более динамичной и увлекательной. Просто приложите карточку к банковскому устройству и деньги уже зачислены Вам на счет!',
                    outletCount: 1,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/S7AEh1wk_6-1ypzSRnzY9A?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=RO7b5ItwATaXtAbAJ3epJD-OQu059FTGPbtN3gDuMw-lsvxwX2-G38RlOmYlgXBbVUO2TCIxXHwN4G5gIlihST9-WJqGzEtKnN8zax3UX-iOI_Z7yP--b-P9JqjA1CJD&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZEw2ksEqzZ5DRMk2VB3l37uvv7Iby3-LDBfBI8qn4Rli8839h-k_gEMSwg7Nw6pZUBmJnhinN48f4qSN2n_OINuaVDamHv2Vs50pxC2qvRJfs_uDDHfB5q-Pcp3hzkYisXdjqZFsJ-R351WNjHAJlkhDbhIBgMdTADhrMRIXOZwjmd1YtX4OuagFexj5qZULVReBlan5YIFbY6Po6KEhVcJ1E61d78AX4t0In1YbtMwlOgdPOCqZWAM2CxJ5OY1tJBv3aNkLNgRzr38Wdc24K2ecCvqXOb_Xig',
                    wareMd5: '8D8JDIxE1SY5Yd2pwnqbQQ',
                    modelId: 1730875803,
                    name: 'Hasbro Monopoly Monopoly B6677 Монополия с банковскими картами (обновленная)',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Zu0b6Qjj_JjVwqJjnCim6ey65LrbMUVa9Vg7gM5BLAtuxT2n3i0kuvEGMKr8xFOtaen8icub1BdzXpR8au7DqC-8Fgl-SpkM2Zt-Vg2ejlR53AzQjidTg31dBkhj5qqOxe8Sm5iEByHSCBd9sZVoYEgj0WDxNX5iC3UsD_RkBfAVPU60NCPgOPNI8EEUTkY5TdR195uBjH8rFQ2l2P2qbBuUBo0--WHU-McAomtqPa4FhIyRPXqFlX9k_SADKz5z0loEm_KYHCSO8T6352Jh6tLYwRxNsL8kX6PXpZ3f7ub0WMqakRZnIaCzA8b1wc6wKdWqtaAsoIutjirRG-th6UVeA5xSzBHoNSbPfEJ9o2egvBUnFx012qobmyqQzEhiZSwlTwGP1l-X_TJmJQjQYSVGoWAMbc_hVQs6lSjzT_QEUTMc7xNxhx27H7Ne3PyhY3SDkhGXHp3n_7lErIObkUmeNUmIklWmPPilQduz9oI5JarOm1Mr1nTcoi55Rxyq1V4CcJHcoQUQSjMvGYbFO06CFn3dxD-kNeOIKiyRV-r25fDN2XBSBcqDzIKvufo6a5TWy0QQklsJca7iOt5WwkrwlH6ihTs8orzJX_99wvdz--4RMgSv47QJ-NLONds4lSbXXaOLX5thPjjA0wbdVNY1Ui3K8kAWKzmOXw-G9dpMQm3K5hRmaRHSsJf6uP6gpopa_PqMmkf?data=QVyKqSPyGQwwaFPWqjjgNiFf3QMBC9aLLv80bNETFX27VN7vpk3oaSxlxBIxXaq4Qt1k-bEGB2PM_TkIOrhLkccTIEa0Bq4bMWkoEwMhVXxCPd7jvABeuCXNAg-gYNo9n8AKsZSVQaQbAzlk_zsbUtpJNfydF3Kzoywbz46CjOhmhIUysyY2G-MqRmfPC6x7u2VQRMcI7oe5WfpGLuuHiZifrrdyiwABZawnHI_NB9EjeIPQLC3lUi2SwvyOeZS6fypF-2YwgxrstDcXJB8zCCWHBP7AnKIgi4oaHR2SFL3gZ_sOjGyRxNuJKItULcgDqbMhqQLeEP7NZCYH9SzUlAz-DjZw5VDwfRB_Q2AnVXnYMluTJXEr2oF6OOfaGdgWZ7SMdZkW3uNG9TxTmORjIr0cFGLwspXfYSCkLur5hcDAGaqfa7ViCIZ1Wg8pktyp8m0zp8JIRP9ACrEWSZVB6nRJkyCdSXJ-i16MtmfPg66Rfb-KXC_DrcN9gpP0aHk1ysfDnkIh5mWkMCkuCQxxHyx5PGcF6lm9B3rTEJ81xnFS57emHVeJ6gG7bTBa_0lKq2EZFGmVjrKomedIWIFOWg,,&b64e=1&sign=695f1250cc500fc5426ddebc0583fb82&keyno=1',
                    price: {
                        value: '3695',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 400336,
                        name: 'IGRUTOY.RU',
                        shopName: 'IGRUTOY.RU',
                        url: 'igrutoy.ru',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 17,
                        regionId: 213,
                        createdAt: '2017-01-30',
                        returnDeliveryAddress: 'Москва, Нижегородская, дом 50, корпус В, 109052'
                    },
                    phone: {
                        number: '8 495 507-20-92',
                        sanitizedNumber: '84955072092',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Zu0b6Qjj_JjVwqJjnCim6fOxGDqU1KpuBLNUWm5sJ0ZPx8v15AYrfsVQt2RSWpESusOTwYpNoU0qyemQCQqO5msH1q83kb1ZouqtZQ9pKKJ-t0-7239kF2kt2kWeSE3jeZ1Kwx2cmSfaaeXCKmSljaeg64pSCYKFzBwOOcmiqVKogv-cpRY1H8WJh6x1qv7m3swUfrkqCYuj3c8IVE-N5ysMYK1GEH8Gi00nmdOI_2sgM3W6xkOLnKCMJvGNPzKTLZLPpDIsdmwbYUc0iTWc68pYG__wsHjQeVBVsA35KEPSRaU_Oq3M4n4Qjk0HWBgyOw4kkqPxhY5knD1ElpxBOtOtbR_SME2_AxCPfq62sin5J2mggxdQI8e40iImOEre3jaITM1kl9r6zsp9_dcYpICNpmEZWW6BaNjrZuaDyP4QPdP0_1pfS8dK-GC_i7hQSILn45sPW9HcMtn_d449EbebHd0gs8hlnjLYKxtusO0omqKQl4OuM5nBuG2LTlByRhk8EDgsIyMTBZzsZMJ8_Ypl_6rV1Hh5RowVQdF2pFnoizVZ-GvrEt2O7jBvd_BQ5cZY3kRQYvBWxuufN_K_HS1zO51QkCdeqZ3HL2tWsWOGMBQyfxiDAiZB-GChr2_4_UeEKU_EA0r_3D7lie8Eeo2kA-1p-4iXmygkL1bu626uLnzVI-rGpc_BEzIvFQV6kO0mywrPccc?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8eIeOl3wJHKLA5rha7hR_F-zi8tX26JQQDRQT0_ByaeQqpIOUkRFSzSJcea7bVXu3oDMo_RJCsUvCcH9_mF-3KX7bGYlhBf8YTNyc-Icxsbo9TSSJuUwZ9sFNhh80rWKgYXVECs4O9UE-1-hTfqe_Tb_gcBSAzPr9EoM2aXeYtK5e5IwrzpjBl&b64e=1&sign=c568292570af8ade5c0793a98d03f303&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '250',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '250',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 250 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_l8fn17yMLza3T2Ac7zgdAw/orig',
                            width: 600,
                            height: 600
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/196766/market_nOvSFESHc8nd2aF-iy_o2w/orig',
                            width: 600,
                            height: 600
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/373800/market_pagykBaNWocOLEi186hfXQ/orig',
                            width: 600,
                            height: 600
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_l8fn17yMLza3T2Ac7zgdAw/orig',
                        width: 600,
                        height: 600
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_l8fn17yMLza3T2Ac7zgdAw/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/196766/market_nOvSFESHc8nd2aF-iy_o2w/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/373800/market_pagykBaNWocOLEi186hfXQ/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '507-2092'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '6',
                                workingHoursFrom: '9: 30',
                                workingHoursTill: '18: 30'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.70200012',
                            latitude: '55.73540222'
                        },
                        pointId: '560675',
                        pointName: 'Пункт самовывоза',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Нижегородская',
                        premiseNumber: '50',
                        shopId: 400336,
                        block: 'В'
                    },
                    warranty: 0,
                    outletCount: 1,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/8D8JDIxE1SY5Yd2pwnqbQQ?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=RO7b5ItwATawXcDCdI4E-bht7VzBep2daXv9T8d4nQEbzOR_LZwn0oNa6xVDtrtsZMTPSg5bR79JF59MCmjMy9FKIwIDCxzxhHCrfHKNAkubms5LpRH8tRteebbIU6C9&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZFRExfFgOQLlfasMY0CDWNUllPomn_gbDT0y_8hDK-epeBxA3fTq3-gzgNbWlU7nDQIM1DvAgKJR7HZA_TCLi_zpmAJNZXaWfmvCH_k3PV3h2akEOwdvgLWa9Q6xkIgLwumA5M4WCz2aiOOrqBF-Q__AtZkkbRRudu2as8EmTOBxlhiT_EDIG7B0PLLa2JkketIWRD92kWTwQA3aVR6Xu5GfMQ6jyFa48i8n2mFUKZjRfE_NUPqX3LXhueUH5JH-ichLx19r7_XLG7Q5lEaslFw8DNvbrEaaIE',
                    wareMd5: 'IVHCxeKeQCawLsTiq6_-8A',
                    modelId: 1730875803,
                    name: 'Настольная игра Монополия с банковскими карточками Hasbro Games (B6677)',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mY2KuJPeRWaXKiISju11x-DbtlDrQTHPRscXX5TEIEjOlwlptnfHTzwm_xtSBP8dzCLg_69ElsqmevJy-XBCHEilO126HQcvAJjKqClzF4HQclNMV3DC7SgE8H7pLZ8sAKyhr7qBRprqg8_wxdNcOhzmjZIme6g4iSF3ijc8xDHpCr6426eV2xiupqh72FIEdWYdv157lzk2laRE1tIGRMyqkNdRMoSrF9roWDnuGIoqiQkVBbIi_5AMsE3nQkefKlk3O_1gsYf9QtZE4L2TVCCelxMgswUqY5nhrPgswT1D13s4DYrllhZlfkg740Tz_0nT0ySVnuHf9m70CXlZiEmDBFXlJACoS3doKELZOT_VqrnvXR9JroV_o6F4yr9Jb-8_ywxbp6o-LHWM_gO8F8mZ_6lZ3h5nTxhWWCvZBlXGHUlO0u-AkVmeAwZpmFC-9k1MtH5tfD7lKGVhOay20tggFXtZvZdG80bVVbCteSKG8F6FGH--Ug1o_Yvpp1ThbclEkuuSJUkHJT53HwlhH-DoIG1z9jL_1wobI-IX4C7erl0yuqe5FAHDNo3JJGLtWCBN4v6BxaOauA-JagVAaQ_NrrBr5-pUWNJtO5g9vpAYK9q7LLEQ55xxZzOMUpTzN3qDS7W0dg0Q2M7V6WjddG5kJ8t-Mbaymwmv88HtlOmB0nXQkePnFwmEcMoOOXLmzsjlxdv7UsOaE7E6zSQoBsJPxJZIqij5uSzmcJzQZTQpDZVYMaUJzWXbvJH3Jj8fTcWXP2q-VPDTHOnbLeLt0KRXrOxyedU9ohE4AqhGdoFXGlKesec2wrKKASFdyRBqVCZPuyo7RAEouV6PTDXHEApiVG2zgO5wEdq5L_nm7C4rE3zaNw9JLlw,?data=QVyKqSPyGQwNvdoowNEPjaks9pSynrX3G19_10P74r6zsCeFmDD064eVMPF1rtYMPnF3AqPXaa09SPaObRCJ6wekLKWFB4UzlaNACvt0Mj8dLCePrdr0lolutZlFS9IOsWhYmEEpsw5sAsNkS7GU4pREh5QAPyHhrxQlBnxpOHSDblrIIgv84ZfWXmwuUl4S-XXWjudMCLiv5Y7cGNwjLyoNEOZaZs8OoZju3whMeND9RMwXIDMB7auUuNW6t_17ucHAGL-xABA_-giqyVL9UeKP6MOo9Q-Z-GrRjzxqipHVf-gPSRBQY2pEDfRIWPO6OBlaDJxo2CapshrKOo-sKF0bbysBi2hjvzLC9PWfSn9kx-MA_VFji9NDpwMJ65BD6ScgG8O_Twy5Bn2qVDQVN5V6H6ReCl1mHlhmm0UiwHOJlohpUO-uee3ouNif7BXh_gyQ3Mw7AQNa5xgkec5OEwmyTRJns24O7wh5cKGLYAh5z_ixH4w3UT-vkW42DEBJhBbOmuaamMmRNQfy1-hGu68p2RZcJbXoGLOeCEVNR_roFWJwViN9v7dRdi3wQymFa4WWRLUrJwUKLTbww8kedud6tGQvBRp_&b64e=1&sign=199f799713907c01d38c37bf161a3ddf&keyno=1',
                    price: {
                        value: '2829',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 186624,
                        name: 'detyam.gramix.ru',
                        shopName: 'detyam.gramix.ru',
                        url: 'detyam.gramix.ru',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 4938,
                        regionId: 213,
                        createdAt: '2013-10-22'
                    },
                    phone: {
                        number: '+7 495 505-63-83',
                        sanitizedNumber: '+74955056383',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mY2KuJPeRWaXKiISju11x-DbtlDrQTHPRscXX5TEIEjOlwlptnfHTzwm_xtSBP8dzCLg_69ElsqmevJy-XBCHEilO126HQcvAJjKqClzF4HQclNMV3DC7SgE8H7pLZ8sAKyhr7qBRprqg8_wxdNcOhzmjZIme6g4iSF3ijc8xDHpCr6426eV2xhPycidK6T4WDLHTApx4n2We-ogVZmHuN-ifWdAzXq8bi0wSux6S-MRYBsTmEX0LjV-PyC0Hd602NVRqQ6vUk2zTH1I0TJBhHMat71Y5v7peoLzJifyk751buEw28kXWrEFEtgeM23c1EEHeVGYl-UZwnFfPtNpLMNUkFGpu045ZPgtm-yJ0cdPsvGpKzxK8yZGozGaaIyVP0YT1vn9cLnSG39_pHezMk6vY7DvF0L7YbR1mTiFPsBrJ6uELV5xvGrs1AlJA8fr3yLV3HsCMPrrQ8XNCvcyUsgq56HDNJuPI_799xIXOyX4xcuk5TM3P3r650k3QY8JdnCx8C8HzkWI-b2ruDDUZ1rkJmqs53fvPaFIuerbcyOCwbo6qutSvIN27oJKEHtdRupJhIxBzkCZuYcGkV0Inzn61lhhsYU-MW5ibx4Xe6PoqsavF_k790LaJw_4Pn86pbztRZvQEg9KM4M7-NG3Sxa0LrirGcUhteX49inz0nvkLHMjOJrww856IQCP_mJmifXsxDP-7PaL1md3ty4Lwbqm2yF9rBIwtMNRUuqAJgTGO6Iv1BCpUXV1rlmfUViLvnQVQbld7HWrF_LP4vgdsFi52OEoPj9UPgXscNfKioawcprVYr_wu-tivTx1wqBhWR4QhujGxZ4XV1HLxflBNhuThlvx3xpWQROTWgJ2F3WWVbiZLSTHplI,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O95eDMjKCvPCLENiP76u6Txit2hRlbrANk-7T7CD0Qn1t-gqxtNEJhIWnSIuCnZhKJCbh8SHa76_tonfsthskhtSapn40u3zrjDFPXPnBIf51WWqQEo0JnBPStuu0jhCVCZS_ujsBzz0k2ys5-zD5ZTOPNfcmbTMXhMH1oSx1SXv0q30gSuf_Ls&b64e=1&sign=7d7cdbdd395a0bd5e7b7d6c5a314b268&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '149',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: false,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '149',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 1,
                                orderBefore: '20',
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 149 руб.',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165151/market_epKI3xaIi6pvaKP_RbZcmg/orig',
                            width: 250,
                            height: 250
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165151/market_epKI3xaIi6pvaKP_RbZcmg/orig',
                        width: 250,
                        height: 250
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165151/market_epKI3xaIi6pvaKP_RbZcmg/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    warranty: 0,
                    description: 'Обновленная версия «Монополии» Для детей от 8-ми лет Цель игры — выиграть, оставшись единственным не обанкротившимся игроком Время игры:  от 40 минут Количество игроков:  2-8 человек Картонная упаковка Электронное банковское устройство для карточек Простые и понятные правила Питание:  2 батареи типа «ААА» напряжением 1,5V Размеры:  40х26,5х5 см Вес:  1 кг.',
                    outletCount: 0,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/IVHCxeKeQCawLsTiq6_-8A?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=RO7b5ItwATae11WhDg7W3zAYnBmL70xtKQ3R3AzepRfK8DwAYKS-Ditx1RvztvCgFtdbhg0_JkKK834o4PsmIxx053oY0y69VF-u3c2zuoSNXTM0TTNsd9iZkfuWsL83&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZG3XjDo2gWURA_6KdTiQGCDuMtNxcgDNuiVtk3eXW-HLw',
                    wareMd5: 'WPSSqLHoKVnurlD1GUBWjg',
                    modelId: 1730875803,
                    name: 'Hasbro Games Монополия с банковскими картами (обновленная) B6677',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mUOjV6Z7ajow4Kh8wH5Bm-cpmdVH25OT8nYfk4locNCGm0KNOgi9sxw6W2X7fUUs4WcRLbcKuNBEdLFvKt1-Y6m8eBRFrgrEFYJjA9R6s-c7_bMO5fH0LqN-Zz9STFBKOH04d6LT6qosFSy_SdauJ0PsER_bHB8ZwpKk38xll89ca8WhycWCmcwzB9gdGEqiihBOf9BFFOBUK9Gkc640rFheuouc_Y3FKJOfq6TBlVRrMnEzI6LrujWF9IPLtPKoFFfgbKFAAo7_lXwtlo_CR62HWc4Z6uASbEobvIhB9z4MbOXKs08kZTwa6G6o787pkd3-otaGzImhkwUiriNH9bV_6BTm-YArDnZlHbWY5eJ0bIsGdpbrAuYntnGQYVYKePjk3upd__QieXRdh24L4fwv7lh8IVTYTZlOO8-61_DOYI20F9CfIC53sGwr9px_lSGqN3UdbqPfMNanYgnYZLjkbJU3UmD1Pv3mMPrTcv3ZWIRQ-NxUndwp5NIGuDwhV9ZCxy8HjdnfFpEmojG77DZOmH9o4k6cnYtVy91UcLMPzod4hkGk6at7hnR7gMdae5JIq86Ftk0JezB0hskHqCroQ35G4e9R4PQOPFu9cASAAnm7vwvANBUkUf3Kgx5s876tLpi7VZRrE-3C9iegdhRi8X0-qFmSUhexHG1FzCzl0H5H6gcydG7vXa0JwZC35wgHvFoifJNZyub50xAyxLaugtD19ijns1837YCKyd5iJtzZ4UqZjNZ5HMQRgym0QqXZDohn6NZK1oPCi0rJUyfE7AdFq1j2UkIPqEnmX0XniE1ZyRT6ympFGWMoMAJKxYa_Fbu1PEK_FRQINfkFxqnhembenrsUliq6si58jFGuPT7dg0hZueU,?data=QVyKqSPyGQwwaFPWqjjgNhNouZ5NjKxOe7vTulNC_HmzUcy33NyrBpgnASVBLLY4Nw-CLK3PiI2Md6FquAC27EqVkWR4BTTaTH0v6HYWv90vlznuvcjB2PA89IuXBMlrQiV4t0MJ3F2AGsbR667kHg,,&b64e=1&sign=2bcf81675a9da057867cac018a1c202d&keyno=1',
                    price: {
                        value: '2340',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 164323,
                        name: 'Маленький Город',
                        shopName: 'Маленький Город',
                        url: 'malenkiy-gorod.ru',
                        status: 'actual',
                        rating: 3,
                        gradeTotal: 550,
                        regionId: 213,
                        createdAt: '2013-06-18'
                    },
                    phone: {
                        number: '+7 495 669-29-11',
                        sanitizedNumber: '+74956692911',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mUOjV6Z7ajow4Kh8wH5Bm-cpmdVH25OT8nYfk4locNCGm0KNOgi9sxw6W2X7fUUs4WcRLbcKuNBEdLFvKt1-Y6m8eBRFrgrEFYJjA9R6s-c7_bMO5fH0LqN-Zz9STFBKOH04d6LT6qosFSy_SdauJ0PsER_bHB8ZwpKk38xll89ca8WhycWCmczjqsghaeqrzEiKl1amNbOJwZBtUXv5x10Te2aKKvvCjwbUFqYT6a6p42ebB8nt0ID1M-sdbTjNHYYNjIqc7TupxD5J-bLe3-7Rp6NGK0W_e_6Q7-2gbnUJk1KUG2Q8_8eQwNSQrwx7r0HyAv4YPfvvBJ7ejh2eKu2TMTphFqhEwuiSvp0tf_bwlJlpoPSMjXyQmkTnFzoXIkNe4rNe8J3jyR9eYwlasiFns-cOR29U1DYTbo3diAY0kY7ffNKVPccSFeI8z2lSjYBCH6IxqqT1gm4fGyEjl8OMfDv0eq_KICOoZe7roNHTcIKgVPmI3FChIR3_QxKvP--k34WfqQjszfKgAMqyqZJhTDN79KNrafjD27nGB1ICFEmnI3KsQMUDdRPpWoqXa6k30bablbR2X0lPZbxbjS3uysObxPo0QM_KP0A6AEeQKMDo-TfUPYQ8NCP2NxHhnT4qR4diFwWk67TqCoxugESfzmbawo5Wup49nVp9716fcAyjYPR8QRMIqihy8For8kJtnq8YfmPhd3gGA2nBXYs3_X0t5cl_DTQh0FRhbyCwfQqsQIwW4rVk9yGnwsXvfT7lp_CC-FPNOLHUAvXNi0gPsvTpJLP7efZ6nLbSIbd1NohXgknzataf8ywRULILYsR2enlfKNmUeAxfWOAWtdpQ_fjINxi-dA9fcsXLDYVRoKPNzCj_7G8,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9lDW4rMFaPICMd4_-La2jICj4bP_mDUJkuQ_dTXlGCcrf8ANpVzPqykxZcl9nbhHuiceGBN0BpiBfNLg1cgwshwCXDsgsaz_v7nuAy8B7Vk_Fk8-b-z5BQKGVXzhVktP6FcodIRAd6-SXWXJ7awztssibRZdCaau5BYvmkALnvyD2lxJ7IUqgV&b64e=1&sign=f6fe8711ab1b453839850e1112b5e220&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '290',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '290',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 0,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 290 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_aLa3Qtx5Mhd2zUdvOVLtFA/orig',
                            width: 600,
                            height: 600
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_aLa3Qtx5Mhd2zUdvOVLtFA/orig',
                        width: 600,
                        height: 600
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_aLa3Qtx5Mhd2zUdvOVLtFA/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '669-2911'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '18: 30'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.67642023',
                            latitude: '55.7188343'
                        },
                        pointId: '306584',
                        pointName: 'Маленький Город',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'ул. Шарикоподшипниковская',
                        premiseNumber: '11',
                        shopId: 164323,
                        building: '1'
                    },
                    warranty: 1,
                    description: 'Эксклюзивная русская версия. Вместо денег используются только банковские карты, теперь нет нужды тратить время на размен денег. Все взаиморасчеты совершаются в электронном виде с помощью специального терминала.',
                    outletCount: 1,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/WPSSqLHoKVnurlD1GUBWjg?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=RO7b5ItwATbwRfYVNrL7aXBtl7vTtpSrhGITQUQq4MEPQnD6JdBxf76MX2fjHwbTVs9-X_L-wqqCMqBssHypdF0oRJcfTETxvsTdKkQcRLiOrPPtOmZumhBYPTEggQmy&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZF5LsJJpat44uZq3h4tJ9YAKLtf3QSZ-_ItRP2rr2wwcA',
                    wareMd5: 'pTu3_TmbFh3IwTcrk5wAyA',
                    modelId: 1730875803,
                    name: 'Монополия Банк без границ (с банковскими картами) обновленная B6677 Настольная игра Hasbro GAMES',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mY2KuJPeRWaXnR_aAt_Ssb-vRd69KIETthoQL_TmRnOJUIMpkr1VEtsSZex241u-E71TIjmgDoyFIyf8hCHYC2l6XSBk2l4AVvVas7TvEQgPYSppgBkvOXaQxv6g4-4J90UDSjKIR9VEHE5OOjD7Pe6o-pbISiU4lLofAW0T-ti0tjQIjrV4i7Ccr8vlc5VTEZPxGRcvctCZKHDpfTQAgKNYhPj2wWNqLOlvuxYsSyVc565LlrDlrRXUpCQTRMj1XGs7X8gDkzf-Ox9_K8SBtrkoIvUX_Ruak94DIfYgf6OhvV-9msqvZ3CpntiIjqo4Rk7tGL5qPqyM-E5mCF-r7d5x_nicuvuH_lz4ml8MzwuIsFCCARItSCBk6USX_hTSmCREb7L8BDJAonTWdoEAvuNy1COJF_RukIoQqJzrvNuOc0nzzMrv_XTTkem6mlGzbiQt5VHIDGX3483GHRrHLAVJeb6sbpkt9lrGpjz4J2h2h_vzClqQMPeBtKsKbEQLGWWQwW8Xt1BPb-HWg3lZyOxetmZPSqZ5yB1rPPfOMzD7OA3BsNuKB2Alz3EJmeh6i9ESLuA1f1WKakv8ln0oV_SfvoAGfoMHiUBvZmoP3oRBBuSWSV-tfwzt-76YJlc0tgwrBUfO5AdEEbYwc1wZKgsSXVS12fKyXzriOR8J5RWb0zf1APZdXMfTMdP0R6tFjnHFv13-U27q7MHWZVoC61_S674HzTH3WIUxW7EcXqSDNq22EShbs-KVSBWtLVlQgMEf7zGeAwYgzLvdKMc0b83MwUdb1meQJLtg7FUkE6H1O6GRyU9-xaq0PwVtbKJkeGOsiygHCGr4VidxD9659Ve3WsLK65EL0lLCK0cY353lImiVBGaUyRM,?data=QVyKqSPyGQwwaFPWqjjgNu15Tq_EtrAxLM7GdPCdRYAsCO3xbg7-Yy40bIgvodE_wpQs93j9U_XcnJGsn_66XygIfLmwWThzeRwp697A47lmW8LrDUPRaNHj0GlWV4Ji_8xxgcrT8hgdUpNw3BLU3GQ43wtTX4GkiVgwSIhoAKwXIXiePr2eLNI5DCysENBr7Ul-rJ_N5-jG0gkIKpYoclQZhrzQm_ltLQJ3DUp9y4ZWU3ruEncH4JyCv6z-RLcGI1Nq_ARMPpQ1fAFGiBIIWS5zUMSywAXqoDYi0hfWRjnTyz0FtPtPWeJRoDFS1LL0Rw5oc1jdt9vjyga91AuBkQFa6VMJnAuauAu0u2xfSPoFzKysOVhU9X5YzGDam6GUuob7xNpz0gIgPa9LrGGbqhLK_kaWsizNjYQDLLJaYY-CoSXSzhAdyivv0fpAqcYaa-SQNnOVnL55E7DwxPth-NT_3VuFaEmsImpOuuI8KP0nPItg1MaAbYqSxuztAHFcTaX39VGLIj4hqor30wC4oA-6DJUsqcPXnJjICA0oeMG6r-wOSt2f8zVhxRbOvturC41LZlXt9jCvf1d2N5mOLqqT2lnFo-6-7BhnKU886rigmFkMCGxC3T-4lL7ONJ_71VHtasVczxqCI4vG4Gv-tWXdh3kZ-Jo4CaLS7N98RPwwkBDDTMjYfj3pmxyD5cHP&b64e=1&sign=34d3ae159b9bbf8c2b8f0fbd90ecc723&keyno=1',
                    price: {
                        value: '2527',
                        currencyName: 'руб.',
                        currencyCode: 'RUR',
                        discount: '5',
                        base: '2660'
                    },
                    shopInfo: {
                        id: 293277,
                        name: 'ДВА ПИРАТА онлайн покупки',
                        shopName: 'ДВА ПИРАТА онлайн покупки',
                        url: 'dvapirata.ru',
                        status: 'actual',
                        rating: 3,
                        gradeTotal: 106,
                        regionId: 213,
                        createdAt: '2015-05-20',
                        returnDeliveryAddress: 'Москва, Сокольническая площадь, дом 4А, ТЦ "Русское раздолье", 3 этаж, офис 306,, 107113'
                    },
                    phone: {
                        number: '+7 495 921-51-57',
                        sanitizedNumber: '+74959215157',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mY2KuJPeRWaXnR_aAt_Ssb-vRd69KIETthoQL_TmRnOJUIMpkr1VEtsSZex241u-E71TIjmgDoyFIyf8hCHYC2l6XSBk2l4AVvVas7TvEQgPYSppgBkvOXaQxv6g4-4J90UDSjKIR9VEHE5OOjD7Pe6o-pbISiU4lLofAW0T-ti0tjQIjrV4i7BKAXa---kqiyc8IQhRBLoJ0VGNRwfyRZsElTFxFDSy09gv9d1g6Ba0ajcqbks1EtQUO--hc-uWO2-oKMDbI7BQKtzQ_Nd6d2qU7QsNk5az3NbBgCrCLzrjTHuXgWrk4Ehuu0va8phcCP63dh5peyFWSN25kkScCvH6YbhJmrjrDHObcvEycFDT3cDXd3haYn87E2MVXI93WNOZtp8l3F3nmboavkcXaujQr-UEU0yrqrVEczImzKQcYnheQB6vbtDgh5bSKWpR6F-c3ugfchXu0cSb7SmuSFPI9tNfBP84F-gnyFAQY6sJwB_L2nfuTvaBHWdq0hiaN_VYdtS8x86WWzfjkfjuSEBAm-K_INQIkPbA8KI5AKH7YddYA67Nyv6HrgvDIYGqSy_BF4lw7XkDxVnJwJ6hLG3903l3YVJvICUbA2bE3Ycv16EkE7NmrO3kG_GDtM4KRf_K3t6UfWDgH1K_LHgE36_toSI9UkQTYnmvyB5QvHDf_kjMdkL-qGobn4rud3vpHSdftd87UhSIIzwEyCR4G727oC_9KKzKxF222qD9uJ3N1qpLZYesDOJFWXr2fKRBvakl10A1bIzD_nUW_U9TgcssbPifwFrSJ6YWbdlTlo9XrjDhZi37xKAAXcCfIChxX63BFgdjri-SdjNumjDBxXh2OmHQFKcE1yCNf8TNLabv4ZK3EUo5cnI,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-lpF-LgVGImYkegd8A2q6Yww5rOpoEBAyPALERJj1DzEGEaNfRrJq8SSIYJFNSfPZb02Cew9IE4YhRkxvj2bW9s6DAxKJ58khfAOD09BDTLM2Ou9IPufGn2mkbo2lJzy82XJI28v5dQiv_KIIWQn3c1L0B51nS5Tb1ijNEkw6pJI2LooJ5vsSI&b64e=1&sign=1a688d2e8a8689230d8396ef5b4ba3ab&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '280',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: false,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '280',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 280 руб.',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/225051/market_mEQVamrCC7SQtGMZirvD8g/orig',
                            width: 574,
                            height: 407
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/225051/market_mEQVamrCC7SQtGMZirvD8g/orig',
                        width: 574,
                        height: 407
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/225051/market_mEQVamrCC7SQtGMZirvD8g/190x250',
                            width: 190,
                            height: 134
                        }
                    ],
                    warranty: 0,
                    outletCount: 0,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/pTu3_TmbFh3IwTcrk5wAyA?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=RO7b5ItwATackddW1qvWSVgyP7cdwq7YQ2SRsfH5aM62fuPGtadZCi7ZigJIK3muxVz56BYnhzIjt-pC8mDbSDcYuDsx44Sta_7z5nIzkX2aGI0w30igT78yQPTQm1L9&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZE9X_vyx-WCDAyBcZ-BOKLmp6VDwAoWmyHDuDvwWla88kjq6azu2Rn8aLTeyG1Uq1yUyyh3FPIX1sAgeL-sshtjVz2P_-obLOt6i9jQkGGH6bBvv0BNCq8qNz3qalC1JPzoOfwPeeqax_NAb0WJewK_BJgGdcbGeAmwwG-_T2P3ApV1AJauAMk3wmdobcQyoN5HtsREbB4L1hs-vETes9YCS4j-WAX_d5gI7NYwmDTHQa7bCFcATfQ6uYuO8NtgJb89z7sNSWBPhUi-utgHxkjZbb7UltbnZXE',
                    wareMd5: 'iUOVP9AJ-2rbdo7F8GLF9A',
                    modelId: 1730875803,
                    name: 'Настольная игра HASBRO B6677 Монополия с банковскими картами (обновленная)',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Zu0b6Qjj_JjVwqJjnCim6etX1WC0gfy9xSnUpqMKO7tZSGLhecPW95oO7rVfWQN-hvf7ZjQFm9J37trMYgACt8emQkkkG9vu8vGm9ILzEQ2kNWmhq0QT9Mox1e8Yzcd7KYgRpD7sVua57RhrrQvt-fOc3L1_LCx4TNJ0evCswehmkLaAFUfuSFFlY6Yzk4VYDrw-t0M4_z5hczM4ccMH9JyIh_fHFOBAUI63gEPtMkst1iKdW8jYab_dT3UV-EMUrcZvxqR5W8PT1k0wVdmqxV6DngmMmQ_EsPLqfcdiMcsVmOYOYmj7_A3R-JgJbBOntfkdwabaGPvsT9qDHV0Ww3o13_jmwx-dcDZxZVv6D-Ve-mt8pORMBmYK7ZbevmiHsh9_a_0Tu6KfJUuP7fILz466HWaEeUpE_BNKl5PmTr4OA1bRD0KJfMY3ZUHRjI1MOw7pL_X2EZG9jKTJMIqp44bWB7k4526hNdWyvKuUv3OX6a1eKzcDqN9I4oqN8N5KA70Cp8uqR1S4KJqN1j2dBwkSrEqW69Jut-vuLUdYyL0EmcYuboCMm_903DzSjPXTFAtjsInBCNm0ocmFXSYt0hWtCkAnOV6u4u7ZeBSFhwpEfOVD3fujrEvL_KVdwMf4PV5vSlBT5z8bx5R4HKiVTcCciSRq8ejIq8eXwdrHHTeMbxTQQ7eIQ7jzlehA33bVtlUdRuVfKHo?data=QVyKqSPyGQwwaFPWqjjgNhk4rTN-sddqHZqf9NHgkGm3Y7Nsx0J2Z3E8yQibGxT6xFSwgKTnxZS5Bww4wJlMw_S5ef5DjETZgsu-XpOOGc2Rf4XPc3dsFAytXNc3OqfVOFJUwif1b9UeJYXNle6ysxG3Smo746FkuJijrjTJ8N6PJBtz0DAeZHHtOowVcXRrvNeEXUBU3kexbZn2L6I_gtVWGRQkul9C6yFX0QK__zB_aPt4eIEG_GTGyS97haDCTD3FoWrkp5lRdSxG3uFpdA,,&b64e=1&sign=b21317d1cf6b03bda581a3d4a66a5ffa&keyno=1',
                    price: {
                        value: '2750',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 409820,
                        name: 'Велокидс',
                        shopName: 'Велокидс',
                        url: 'velikkids.ru',
                        status: 'actual',
                        rating: 3,
                        gradeTotal: 9,
                        regionId: 213,
                        createdAt: '2017-03-24'
                    },
                    phone: {
                        number: '+7495968 83 58',
                        sanitizedNumber: '+74959688358',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Zu0b6Qjj_JjVwqJjnCim6eCdy0NrIEBedBj92IEKyjHrIIxX6PZ-y_wSUT_CxzSb9YEwRnjwwiAnrIlNzs_yj6rkgZXMTnKJbJHBrQULYR3FI4vPiZCE67pNpxJ_sV4vjz4raQwsyNA0PxLqWysAkY8C8JozSPtgHcwClH6fDkc2CeCTDTRf3LdESL1R4YdGiC28xTQ4xup9FFfYRbMJgP4zf75P8pDUA_tNG5Ts6-oAMjGGgZzHmbgI-ZDx2z7GLfuyPOhtCG9ILpFxIJRgoALVGZxFCGdDFnViNoLVsf5ZR5lSER15hZWZSJzIQITbCvCE5McP0oSe3yteWyymSeF7B5i2rx2w-tUHtd5DZR3RJwRXGdeV31AO3Fgyn6i3jXEqU0BIT1tnDfXTV44qoT0yE21x7ZcOpOLNLYy9hvMVL27CnKaiuD-bSICbGfOfrd2okVvNu2xux9mToTh9ZhFECtWGQ6pXJuGexzpd0gOz9odgeTkauKPYMt3i0r87B3SjFycKynouJ9nZxIx7sB5A3FWchLlPgHAS74Vfp08O7YOmILdr7HWo4ADLgdnL-yIjpckev-qWq48HZtBLcPT3oLU2oHZhWaX7zr73U2fxU_llVc-EG589mnvmAM1j4cFuIeGKl4zuhswO3kn2Pw5JaPjNG2f9Mk62S-6y8MRSynC-v5sj2RHw22KOroQL8q1u9gYbnJU?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9aI3_3uo62FWnS2wIxOW4SoCY4ZqA2EbAAK2r3SHtw2WmP4u3DQ2ufSwia6Jw41FjKb_GQA72DrdzKQANq1gHr3MGpnbygM4SX-ADaGXOWKSsIYRTtZgpwAFXTIz05V6jrz3grtZXNL-el4zjVRYn3NDqf23hwZxpa3RgglCB-pZbLBR9Y-hzl&b64e=1&sign=698dcd2e7e1e173a17dc6a571f606eac&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '250',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: true,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '250',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 2,
                                orderBefore: '20',
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 250 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [

                    ],
                    previewPhotos: [

                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '968-8358'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '21: 00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.593318',
                            latitude: '55.795499'
                        },
                        pointId: '647800',
                        pointName: 'Велокидс',
                        pointType: 'MIXED',
                        localityName: 'Москва',
                        thoroughfareName: 'Сущевский вал',
                        premiseNumber: '5',
                        shopId: 409820,
                        building: '9'
                    },
                    warranty: 0,
                    description: 'Монополия с банковскими картами (обновленная) - это одна из самых известных и увлекательных настольных игр, которая несомненно поднимет настроение. Игра Монополия заставит каждого участника почувствовать себя настоящим бизнесменом. Монополия проста в использовании и играть в эту игру одно удовольствие. Монополия является экономической семейной игрой, которая подарит незабываемые моменты. Цель данной игры, это естественно выиграть и остаться единственным не обанкротившимся главным участником.',
                    outletCount: 1,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/iUOVP9AJ-2rbdo7F8GLF9A?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=RO7b5ItwATYiIf1dFN-MKktfKy9fYlpf-mtoT6evilDHRbjHWHv39CGETSwjbP6Wg2od5R5rZNdxMAmdTfXbevNkEz08KDnyNx7FTkwJh73q4dw1JluVLF1eOkQb-mqs&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZFedaRMg7j1DIFPiLW6Ulsz2ukBknvd_vOp8MAOpM4KA7L-t_kvT6e0AUTBxypkVyp9DkbkBCp-wZNjrluhVqpfYtI-FUeX7nbeGc8Zk7fEZyZ0sjM2LYfvfsWTgf_BErkmhlT2hMApKYeHg1Q-IHo5MnzjjlHhJsCHLro-cPAgL1pUoMX137DAzuXnf-hoYKPbpFRTVfwvtk2aweCmkKRIZhg6I6zxz5Qx_Dh54U1zAoBp6WWFeOWF05iSnxF3YNVaxD04Ktw1QG-QSIQ01tznSp0Sv_PSEYU',
                    wareMd5: 'jyh_F3q6qakcz22G9P2eDw',
                    modelId: 1730875803,
                    name: 'Конструктор Hasbro Настольные игры 6677B Монополия с банковскими карточками обновленная',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Zu0b6Qjj_JjVwqJjnCim6dOuDEloYF-XBj2BS9Cddi8OloN6Wa2zsI4WkYQzPTY19wG2sBdZslZnEkTGWVhECw8238z3qE7qU19ia6iSPXM1gltmopHjRBe5h-xS3RN1xQFCBZndfSD_snW9T7RSalVhF1rYjzwliTXVxYkuG78p9ROVzEjY_a41NdkFTzQlXcn8bbqVePUGMlH-Jgx-eVoztdgmBCudcVQGk3eXZf7NiL5Ke2D6gvzhvTq_WhJHKQ7QzpbflbwESifjH1pj64DysP2yM-M34JuLiUA-FgJLwqhvN4Q5X517wDX6LPfSjzwJTu7eg2ZRc13-Qhzg-i_agHs1lvgZEBnTSTbRrgRGMlHGSX-SByPwEFk1V-ss9eIkNrcOjSpASdU4tNcoWkUlfY99hARV_RtAzjombOKYDyxHEU4xbVS2zoK97AqnmQEE0JL2QNYKaU5aHoqtrBhMZ3X93YohBklbXoefwLTOa9RtxPteu9-KC7mGfeUeAnLPnVxm01mBBHTdQ-2szxHKjZK58x1w2IWo7MwSkOuEfvkRNVp48_hut5zOZv_-F_JVTUjt-9HpAYGRFp3So4eMnZQfZLyHr7mOqOa0CtxnhS2-uWgfh1ZIHKlBkN7jKZvravHQO4kZ7eDRsCeitDoO9brqxRoUb1Ca3QkcR1O98TDTMFhq42FN435ewb8qhEzVXhF5OSP?data=QVyKqSPyGQwwaFPWqjjgNoBB3O0qqyPqa12uUPl7wuHoMKWA6WazBNZj5csThnd_wGQobQiYhmkvt1Fa5DlikSBuXrmb4My65SJXw6pdMvqGbgApVALDUzg-Vrxv6iq-r98CQ3Da3Fc3GE1CzCMEZLPQ3RxTKqUESzNngA4veso6CikOVp9y1iruZ-QofgFdbTLjj7R0WiiwPLN0QsA8FtQzU1avkiCe3uBsAPg90dqgfLKDBnoUjGJjFOjOjcD0Dq8mtUO4pb1GcHwVJ90-8CcxHuVMGRY73nwNMry711ZUQQgF7boUpfHk6qVO09Q5W9a7ST9SBc3aNN3XIgrGjiYC8Gv8EhE5QBngOVRWw4sZYXrRTk6zNfMFtIy__vAbTS7Z4RQwQMUxkTgiVVHEVUuP7WXajuEhZD2VIbtqEWS9HX0zn3jYFA,,&b64e=1&sign=e8fbd6592b45ebed40f0bd093d1b965f&keyno=1',
                    price: {
                        value: '3618',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 173657,
                        name: 'Hasbro Shop Ru',
                        shopName: 'Hasbro Shop Ru',
                        url: 'www.hasbro-shop.ru',
                        status: 'actual',
                        rating: 3,
                        gradeTotal: 1,
                        regionId: 213,
                        createdAt: '2013-08-12'
                    },
                    phone: {
                        number: '+7 499 367-54-90',
                        sanitizedNumber: '+74993675490',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Zu0b6Qjj_JjVwqJjnCim6fjtmXotuxHEXlhQDkMNnQ3zAsDG15qqAnhP6-ygPXWPSszz3kESVE9hBdDxD4mIDpeWDGvabMbBmIWEsY00yw78hU2KuKlytCMPWPzzlwgKIPkS6NGTzgul7CVGHoqsZgEveK2r-uOLqDIaGKD0G4lXUe1xvX-Xqw119mNKsOrUyQITLoKRiyvbTyoFnA49zbaPVsANsXtuRq3cFyXPHR2wGyT9ambMtOBiORucfzIizMSk-S7Cy814QBrffYmGVonZMLRoh-OzPvgBHi17foZZQAeQFdKNrN0YI7aIpgUc6rhPAFysRL-JBOmz3Qni9nmqiKEn_8VUQq3BcK13FZ7CORql5QbKnJgCztniKBBS4G6J9AicV2auVCeXYcXkgYJ67ZyxTbPaEm3hPpnvJTz5LG4FUGAf-7DZlEG1KEgJvhrv2MEQsBCuqZxiYBxqUWnAW7ZoADj8bNRpo3EB_p4nxbi55b6GW2aa-lz1YgjudXMijj4cNVcu-_FEsuhL_YBMVUNm1hefaLgWWQeoQCJoRev3KzsWCbMmpo4kUDxCNcNUfVN-RBIqfrDXKMYsKP_cjSf4Mrjjm0VcYjgysLJ9g949V2BECll5fab9ACO4w0-SmgF4lFwfyUCOUGMqZ_Jk7g2HIqio4QN8EQkoowQumGg1iI7rGZuj3sJ5rT3fZ4V5iGtJWA4?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O--cdsWPxqIvd_pVzMwxJUZdkUrnEROIOrcyWxJI0a3QO2OrZ5BYamJ2IhL-2dcaeRpNCqhlArswiL-RBY6dhhikEmmsLSDnecloNJWlTfe8VxBW3Hc1YFDVSZD_tCooUOG0boZl9bQgpXVu90xdDrguh7v1Lk86rt-YjYQsxfN5Uqcn_tyms_X&b64e=1&sign=ac25f9d107fef78a9b0e6b2129387277&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '300',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: true,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '300',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 1,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 300 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/250456/market_E46ndRKHs5br0Dw_y9SKNw/orig',
                            width: 500,
                            height: 500
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/250456/market_E46ndRKHs5br0Dw_y9SKNw/orig',
                        width: 500,
                        height: 500
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/250456/market_E46ndRKHs5br0Dw_y9SKNw/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '499',
                            number: '367-5490'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '19: 00'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '6',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '16: 00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.79719474',
                            latitude: '55.79985672'
                        },
                        pointId: '469859',
                        pointName: 'Hasbro Shop',
                        pointType: 'MIXED',
                        localityName: 'Москва',
                        thoroughfareName: 'Верхняя Первомайская',
                        premiseNumber: '49',
                        shopId: 173657,
                        block: '1'
                    },
                    warranty: 1,
                    description: 'Обновленная Монополия с банковскими карточками. Устройство Банк без границ позволяет вам быстро вносить в банк свое состояние, подогревать или обваливать рынок, а также одним касанием покупать собственность. Устройство теперь работает с карточками Собственности и События. В комплекте:  Устройство для работы с карточками «Банк без границ» (к нему нужно отдельно купить 3 «пальчиковые» батарейки типа ААА), Игровое поле, 4 пластиковые фишки игроков, 22 дома, 4 банковские карты, 22 карточки собственности, 23',
                    outletCount: 1,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/jyh_F3q6qakcz22G9P2eDw?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=RO7b5ItwATb-v_5oNCqktMR3bQU1Ib1UkUb_T1SpZX06G8PsqVSiHH7lUIpONBX9NLAx4xbw1L4KO4qgoB2MQFcsYtNtwIXIpjzwVk3PFS5UeuTNbomT29HGEoE9Pl4H&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZHQ87e-TNY1VKBpeZ5e0WVfbesDAS7WC1xk1mBXI1vLx7_iykAEh7-rcwgc1mcCQxq0GTu8nWGOhYB-CgKLYBUM6WA1nEjlGzilrTOF3WYWFU-68QEOKKALJheIxHCncveb5WeUALQ_U_ePA7tHXy5DhXsROyiwh_BVHWH4tShg8XbWkfbJU84nvtl12x53JDjum6IusgkzppapwJbgbMrFhSjcy4Zc-cdZ-D4SSqDRUbWMl27yqBxpcLSFWOCinMuaj6-3rsXr2Qmfh2dCE1x9xWo4EkHDX8s',
                    wareMd5: 'tz4Z9yTWru0VdwvJidmbIQ',
                    modelId: 1730875803,
                    name: 'Игрушка Hasbro Monopoly B6677 Монополия банк без границ',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Zu0b6Qjj_JjVwqJjnCim6fHLXvb-RZ2BUFAATPmVy7J2uxnl8r3sF__1l2DZbDgKHBrXx3iqgS6EwvIj8KwTc2EuFgWzDLU5rDcBdJT4kOroqRA1_IKrwiDbzIo_sICiHva6LBNJMTYk5v6FmtdUXn6MESFS2lSfcssqZH7G0nZtjXDtfYDSUko2aXnfUt9xI8oelanh1t7A23-JR-rCSx9IOxNolnxk_vHMAUfuBaIeOKUm4ZLripjDk0E-WQiQNXekY9j1MuJhTzu7SkmrrL0DR5qOl573sjIDRGV4Rn0HyEB8BbTftJI5Ma1qKHT0NbyJ3FPjJ9XcUud7Q3cWyltVVA2cOPOf0XjbV-pjIYf5B7JlcxqL3OwGUtvtZYY9AqSGVesPD2oYmn3XVf5ZFQpe9hoo7nXheCiUkybyInk7Lha0yHGyb5dV7KLS2Or4-juZ07qh3nECRYX5wYt3dsgBABGYi-MOqAbDx4ohF6Q5kAXJN5HKOuzFJsKXHvuBGzi2oYmENjNH1LTCBE2AksEMcP4cQpVLR15txFR1EO-CPrNMyp1Fqomu_918jq6VArk03L4mY3wWotEnErID8MkzCtUX4Z6IEOPJ9EYMsksPudKGpmOIt_GCH0kfGQruTcghOcQP165zfTeuUZq-KONOPVzGPoZ7r28JZ1AHaKxmZZqUR0UUUQaCXF36dby1QJUWejF5NaC?data=QVyKqSPyGQwwaFPWqjjgNv5lYE3vGNniNTasHjcjQ0WNsgilY2fPAYpazCOE0Ub6zw_AhWf6qJapHc6U2iFPn3tjwSitmfh30uewBmFhDCnrH137LGMhOEyqndTZ-Zlx3QZGyDKXoGY,&b64e=1&sign=18c9076a7bce118a7f698378ae1671b2&keyno=1',
                    price: {
                        value: '2871',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 30254,
                        name: 'ab24.ru',
                        shopName: 'ab24.ru',
                        url: 'ab24.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 456,
                        regionId: 213,
                        createdAt: '2009-11-29',
                        returnDeliveryAddress: 'Москва, Окружной проезд, дом 16, Пожалуйста, выберите, Окружной проезд, 105187'
                    },
                    phone: {
                        number: '+7 985 227-66-75',
                        sanitizedNumber: '+79852276675',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Zu0b6Qjj_JjVwqJjnCim6dVf9AmGzDnuviA_fEGsO_rRDtGSc9q0ZEqMD1dTNj_5e4W9Ofzmjo_63dlvq4OlFhoSTzNWmZB6S6EpE46nTyX7UsnybRrN1Dth3WyDA71u6Ag32MViAIYvU1b9NbXIWypzurg2a_InGLFufB3nKGYqXSXvj9mOHxBaJjwxLcVZerlIecLkBVDLXy9JjOwQCbeygQk7PScOxrWO6tB58KIP1AdG-yRek7_9sxoJG5m3OYRBxCKjhliBvNWRYh7RKgPPGIdIHpQF2jWCXkY2XvNI3cXGEE6ri45-SkYIAEcfCGGVLlItFrvRDyNCj92iwbGYsawZJyH30UKNaHprJGhvj5IGzT_bKNwGiDybwM3GBuLA5Bgp6k3n75IofiCGpWOHx_y0bigpPWLF50ygmYrfyJlnnW0c7bcNHsxXJPvJtnezJcUMWSdNTkZ67_M-b1Di17deqZJ0HFGI2nMMg5pw0CIHn9NriMqrXbVSlBtdo5paUZl0Cp3YkTAAI7JSk0W1XHAUk20ch7zHFvm770mnFXTzA_jwGPBw2rp91ugnHZTv87oevK87ZzXPnvRKgPjWC_QSKRe6J-PIAYExvmgWzqlcFwbQfzQ3tOC5FPiqBDA_mWgVpMVe-ummFU3U4f27qMzLrx9WLJlbtDMtthSdz-_ESjldm-VTFedqr3tujTFbUSCPu6u?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_1XvVjQ1sL6j6jr9T0h2fA4oYO7dqq2i3u9tfpOMpTeNSSr5xqB-JIybWhdrHPPa1K0X6BJ6iHKTGBuYYRy55jbl_JGWM0EIKL2h6Vpk_g53vWjpVhDc0WNZvFR9R_VthtfsRYIXnIdznJz_p71Wv2n8dPM4gOMCQGbqAdbdcW4i0ALr6zENsw&b64e=1&sign=e376997a92d1ada3fcffd39a1da9a17e&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '300',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: false,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '300',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 3,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 300 руб.',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/169986/market_2TqlfhVY-ROVcTb3swfXuw/orig',
                            width: 720,
                            height: 598
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/169986/market_2TqlfhVY-ROVcTb3swfXuw/orig',
                        width: 720,
                        height: 598
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/169986/market_2TqlfhVY-ROVcTb3swfXuw/190x250',
                            width: 190,
                            height: 157
                        }
                    ],
                    warranty: 0,
                    outletCount: 0,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/tz4Z9yTWru0VdwvJidmbIQ?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=RO7b5ItwATYReFeF8I-uxzOu9B7YFrAFxRp7bcOXJ3-2WwKzKCmCm1v4IVqhsXkzHq0aAAhwwaNx8H89RhCHrW_Cq0W2JuBzKH1NXzdkHZ18MfcsCq6ac0Wp19mx9-l9&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZF_O3XxYGlGWU24iASOReatLxMarQOnTodbWbHZ4bPaY_MTSJQr4sozfkLTbkaryf7MbGOVtHv8pFIhw7ayQK3KTutSSKsa9P7bLXW-3ftmw_jc3R-VlD4R64pdKbzlydZzhab-ZWLwTBD51Z3dBMOCj4xE_sxzPMBy8cEy4zGTn-DDzjtgjFMz1OSYYry-lhiZloIScj7GL_RhZUZ5h4iZWX0_8n44uKASmHHLV6_kMhcdwJRYjgDtgQxP6gyRQFA9g3PJbeK7lWljkg2r4RSo7I1qWujUGww',
                    wareMd5: '17zJ7DWyI4bDavJurTRb7w',
                    modelId: 1730875803,
                    name: 'Настольная игра Монополия "Банк без границ" Hasbro',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Zu0b6Qjj_JjVwqJjnCim6cjNBZN17xGNeSAcELNS5HZiE4KnaoPZLBmx2Tm3cO4LFluaZBLgUT_i38zxDpskZ_iblycWZKYEY2TV06FaFfvxcZeIQQLPz2pymz7OWZ4sBXPndsXOkkmRmjx1j_lY-HUmkeek-Y5843hF8uqWABbWvB9Ks63wjfEQnQujI-BS_H9OQUPI-FDm1ngrbJNEnLsS3vr3vo4BjoSkQ6IhqHmxD1buFITwj7ePU6TUd-GRvXAWewnq25E-rSOJpKQDiDFuN5TGADtUQbcnZWuFBrBmXENW6JaDvFRoohAoY84YUpOnTOCQXexjHd5l9NXCB-U3AboZQs2MH0k7TEBYrr_3JDIMUkm8XN2As9V8VBPV_u-fR6YweiEX8RLRPYk0QXaOFY7_mFcQUu3YR5Syl6P5oSHnu8DR9px1JNvPNTTeTOII062OO5vcZ6ldora3EZbQqKHP7A34F0dkGyHXEJPr3a1OH3xQ2VSvdOten3QIWBKN5gATYzqF-1RVRPfYAY-Aq1MZDHyqvhNO8QMPiPs4_3oGcSKnYoCnOlwHrMxOk03YS3SG5vz1Z853OVk4VmVsjO6_eaL822B5vJoOPJyKjy6WG93aCIGuhg5ETI-xfqZMxx5_IXFcdvH96uaaZ4eybNbjZie_kiuZxHBRV4EOI-NcMhDglApTd1453aqwNa6ENQeVpSX8q2CYLXxjPw,?data=QVyKqSPyGQwwaFPWqjjgNl-TfeFnXmyUxowmb_qenor47tF3y6bBOLHoSuptdxC3WwrsMDrOXTejMROQA3Z--KXYprRS5muuTrBbmsh9KdmEzkad0PUVJgVwRZB78E9Bbg1GQ02EkY8gt3-vekU3CscquYY1bBrIJFcMKIAFEpMJxaBfFAhjCxoYpeKKO4Zf3PW_X3asskh44mQ3O3UuTqlSLbOcFTh0A1M-rnwu3FW5o55X4bgLVLKIewBcF4v5krFHnrPAXZVzmgkEVWfSNSKmCu09PVlx&b64e=1&sign=48fd84c9ea4290ada5ef34e4bacbf766&keyno=1',
                    price: {
                        value: '2662',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 432348,
                        name: 'V3Toys игрушки',
                        shopName: 'V3Toys игрушки',
                        url: 'www.v3toys.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 7929,
                        regionId: 213,
                        createdAt: '2017-08-17',
                        returnDeliveryAddress: 'Москва, Трубная, дом 32, корпус 4, Как добраться:  м. Цветной бульвар. Из метро повернуть направо и идти до светофора. Перейти по пешеходному переходу, далее идти по ул. Малый Сухаревский переулок. На углу дома 21, стр. 3 повернуть налево, пройти 150 метров и повернуть направо и Вы упретесь в д.32, с.4. Вход со двора, 127051'
                    },
                    phone: {
                        number: '+7 495 278-08-75',
                        sanitizedNumber: '+74952780875',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Zu0b6Qjj_JjVwqJjnCim6eRbhj89VWYcO3Dk1Ybzk-C1uLk2OoqEnUIYDbVTAGaJiWwweBC7VN2Iez_4pzA6Fes7Q7nQMERjXvEGou7WaFCOoioxOFOyFUXXj6IKx-l4Et-awaD3N63Oh7vkmz2QJo54OclfCteLfoGEKXds29b1OZBDwuLWMUqMyKXTadl9RXISKoYbGexgvQZydtM-t-zoxCmoljEggV9P5kolFMJSEiHG_oOOs6ya24ymCrTMZiu9QVPOpJjaxbyy7P1LtuyloPbHdeulwJBnLrxbrs4UsLbSkaPlAnXZIRvXVRJNE0oYaED-duk3taK0zAamdjBZ-zvua78zSZoDPZJR5hAHH-oDHIqx0A_t_TmiY_9AVlkNpEl6GlRBc4BYKM_WRkK6MTLjQxFPQaK7Og_fgUrKaZJx2RHMBOxdkXptqxfL2uWIIx5AAjEKZHepyTuSlDz7ftPUuwavkLMVmGY90DTuBM-55aW9T6mbSWLvEo3Dc6g5TRBlyrOKGEKSMXgGUqpB0K5b4DnkPKnoRHjSmWsDYeH62ym9gZCa65kX5oIad15aOOmNbKez0_gFHidNIfL6gx33GYQYLsSXzMAB2Wii5jEuQCcHgNVeamdhST35mBDC1zhjuKbeHq3DnWonH2MA4Via7xPcgGhi_7_5DkgERKrx6OBimbrnGC__Hh8GuHlgS7mKNXHur8qS2WcaXs,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-FFliaAbSmxTgLUTdWmHssBBwo8a4uasDEol0HbLmLGP7r0JtpaiYXnHl7tC5tOmQtMDL319kE0NY3jbpv0rNCMJsn1PfDiMaCTrlNavYjraLt6WQD2x4MgZt_7QjYQP7FoyEPamBlaV6CeXg1_G4O-kM0arHt57wJlp9BkCEW9XrHyLLEQWC0&b64e=1&sign=71c7f75ffe4083594f0d88b7f6805015&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '199',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '199',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 4,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 199 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/167181/market_B0p0gHbhJgrk47byB8YUlA/orig',
                            width: 1000,
                            height: 1000
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/167181/market_B0p0gHbhJgrk47byB8YUlA/orig',
                        width: 1000,
                        height: 1000
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/167181/market_B0p0gHbhJgrk47byB8YUlA/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '2780875'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '22: 00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.634434',
                            latitude: '55.824005'
                        },
                        pointId: '1784337',
                        pointName: 'ВДНХ',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: '1-я Останкинская',
                        premiseNumber: '55',
                        shopId: 432348
                    },
                    warranty: 1,
                    description: 'Представляем вашему вниманию новую версию настольной игры "Монополия с банковскими картами" от бренда Hasbro! В обновленной версии игры Monopoly E-banking, как и в первоначально "Монополии с банковскими карточками" вы сможете использовать для совершения каких-либо экономических манипуляций банковские карты. Для удобства в коробке с игрой вы найдете и специальный терминал, который поможет вам отчислять и получать деньги за совершение каких-либо покупок.',
                    outletCount: 188,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/17zJ7DWyI4bDavJurTRb7w?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=RO7b5ItwATbxBEYguqzQqqQsxZ0jmEiRdkmaxwWnp1l1ZxkAkFgvHtsOnJXN0DfvTyw7paY4GQEiDuRzSNSSBZ14EQZpdjW00iDMkyBfA57kePKGUoSDJ0RbR722Cgw1&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZGle87HDLsSxz8ovjLJTJ2IyNQBEPNMOwNpupqNbaZ3WbI_6yMbTArZHdwJZHyP1GBRSEfgne0WXh_wDamlKF22HAZRGe4Hm-wzaMlHWNOKEDjaj9dQqnkZi3MS6B8qjyMkW-5UI0XuKh8Sg010rAqPWXDqlkCT0D0FdqHnm0obsNMQqcQAci0qOY7Kkd43fpjFpM2zZICbk14S4MmRnmwc-FkQ4xDs1fES3pQwTh28t1GcsF4stkfYczNXQm-lHoPWtx1v2vd8wS_yv0Q_xUkaJRTXoYsHXZE',
                    wareMd5: 'rmujBjJ0WtzR6qCT0Hu8Jg',
                    modelId: 1730875803,
                    name: 'Hasbro Монополия с банковскими картами (B6677121)',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Zu0b6Qjj_JjVwqJjnCim6f4ThU7B90LGR5qbSmhGy6C_dNmHyi2bKBdGoMQ3inV8tAavZRFwY1OIzJtCukUAEAHyDvsOifcNRmD0qNiXjRRVmc83VKN7SWGZuBF3QZX9ApMmKlfq2BbzX-6NzQ-pxP54lb2-pu9nhnTZLCNPJW1MImnvM_GY-FFjEpEmhf1r0gEybGoN1wm4dCyPlrxkWjairsTrlwTNBnFKSfH0wjgXEzm827gI8_c1eCjgMS5-dnnZqRKHEUMHY0QTwtN5lOGaL5tOVEGxjzi4q9PlBDz5WwYNWpzBcgkOu97qJ94wdHIfUK_bCj0J4PMKuNS_SMhAgKLhF5InHzLHoaWoUvCxzhdY0G5p_X9S_JjyAoS2KwogIOnVxC8-IuqQDlU7qNH_PHujcoRpxNv2AbQX0Kr4Yu8VESuHFLIHbTnlZ90WIB4JzuTfeU_0o5PouDzjpZL6L9oE9akdMsH4i05NkR9C23JsarjXXkOdpONwguB2GZzGWDHBcnkm32zXWCjZlHsYanhrwmpwulu1j1ssNAPJSrxQECXhkvlith1zhxacDx5p2C6EmubNw7JnGO6UGgrQ7Ek2qiVITjjg_cZKw06XNKEU9pGmF2wJ4ZBIPTzPOy4F4FFoYUtucczNzWXHVjM2LDKhDamCB2atDfzQ3N9nZYPVgD5jv4cvCy6wCzpnBjItf-sw_kO?data=QVyKqSPyGQwNvdoowNEPjTZRe6bMG-0zzr9vm27fOtsWUk8js471u46GMPdt0ICFCR0lshZOnmmJ8Ber4xkE1doGdBjrnMk8YMShTbGfZgryKijIA2GMxRWY_0je_r8C9Vyh29PyN9d4P8X26LQQ10qw7y0zCbHfJVAuGCXbINo,&b64e=1&sign=9a36027805a9488d5ed47e005d93f80d&keyno=1',
                    price: {
                        value: '4687',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 52585,
                        name: 'Лабиринт',
                        shopName: 'Лабиринт',
                        url: 'labirint.ru',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 19799,
                        regionId: 213,
                        createdAt: '2010-12-16'
                    },
                    phone: {
                        number: '8 800 500-9525',
                        sanitizedNumber: '88005009525',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Zu0b6Qjj_JjVwqJjnCim6c-ZoY_BkzEGsfe5TEVZkh-AV1A9zXzgHKPTPPf8CEz6DTcubVPV3wJJlyfEhNgzevcMVhz_b8DDb-NRUQGwy8mKAYN664Wzm-GYhdVeIgqyeoCiCfOPnpqbM68ZKqIhCJCf9cVtxQ_65kvorFjxdricsbBtRx6IUeyw58Mbi8qNBf-YFZg2bNRsvJAKi23SiVAgqRQEsHGyPmmxjvkLXjsENExmecByLAwsucV-4tWF7uso33vJLVrskRw9EJEoR0dRkeGmPav9KE7i3Yh41IdJpSjKp6edXpxlnDkOMP-4iwiT9ZOFch7xTOOz5M_Q0ZMyQuzi3KpqQIPv7cqds17ysvYl44pgNmatZ7RBKptU_1vOk5mLej5_v8T-DKj053W5ibt4kBFrgP2geFKTGVd7wudgRbJAJINDif27BstgLW-IWGuTgccDxB4cJBJVKz3L6QAbGNJjC3ozFIMCCaT9wjyfg7IQAnI9KMuK3YVjYTikHX2vvamTL7ShWhdgofpGlr9r9yeInzgmGaBUcC-XOe37ScRyurtOyf-ocwGa0NvTNRFYpOx39CMYcqq7xrPxAKZEW5sgWH7FzOG1uli7gctmAi-kBcMfQ_ataRFphC1hw6BfLc4J0ZG_tSs9lBjssKjLNSRlTRdPwmvr4GoEM7JNSkH8ceRhcGoxmZOG3Vb1rIt7Crw?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9ySxmSAQmx5IzjWCnebfKOdEaAjl5G6i4w60eI9JTjECFeG1mymDGelUm8Uiu-VjW78YTjbor4cm-9oT06wpCwNCpLQT8Xz_0Ag8CaJoER5TwOJyL5a8Y2Py8ZtFi2lGlygc0nbuSZBZBnYFKPPyc4H2NDYjAclhsdWa-QoNG7lmJSXARLX20D&b64e=1&sign=ea62cbab90d303afe0b4a46e891b32d3&keyno=1'
                    },
                    delivery: {
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: true,
                        delivery: true,
                        free: true,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '0',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 3,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — бесплатно, возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/248114/market_fZzCNQxG7SbnmwISzkFcjw/orig',
                            width: 220,
                            height: 340
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/196766/market_4FBXN9XVBx9BsF9uIW7rEw/orig',
                            width: 800,
                            height: 687
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/217370/market_le_kzUas194S5Z1UG9HtXQ/orig',
                            width: 800,
                            height: 598
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/247921/market_AhgGFB6ofBDz7AucOQGzUg/orig',
                            width: 800,
                            height: 598
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_cEYVqZ1fr0F5gntyCjCXQw/orig',
                            width: 800,
                            height: 598
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/364087/market_LLL7nBHvqHH5F1r398Z5XA/orig',
                            width: 800,
                            height: 598
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/466758/market_hpx3ZxmCM9S1ly_zNH83VA/orig',
                            width: 800,
                            height: 598
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/217370/market_7k_TwTyTVcec0x7QB_1jgA/orig',
                            width: 800,
                            height: 609
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/363663/market__I8l2px6-ZxXytXVOCTKdA/orig',
                            width: 800,
                            height: 456
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/247921/market_Q1bT3Le9FdEyL37RR6dFgA/orig',
                            width: 800,
                            height: 675
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/248114/market_fZzCNQxG7SbnmwISzkFcjw/orig',
                        width: 220,
                        height: 340
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/248114/market_fZzCNQxG7SbnmwISzkFcjw/190x250',
                            width: 161,
                            height: 250
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/196766/market_4FBXN9XVBx9BsF9uIW7rEw/190x250',
                            width: 190,
                            height: 163
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/217370/market_le_kzUas194S5Z1UG9HtXQ/190x250',
                            width: 190,
                            height: 142
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/247921/market_AhgGFB6ofBDz7AucOQGzUg/190x250',
                            width: 190,
                            height: 142
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_cEYVqZ1fr0F5gntyCjCXQw/190x250',
                            width: 190,
                            height: 142
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/364087/market_LLL7nBHvqHH5F1r398Z5XA/190x250',
                            width: 190,
                            height: 142
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/466758/market_hpx3ZxmCM9S1ly_zNH83VA/190x250',
                            width: 190,
                            height: 142
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/217370/market_7k_TwTyTVcec0x7QB_1jgA/190x250',
                            width: 190,
                            height: 144
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/363663/market__I8l2px6-ZxXytXVOCTKdA/190x250',
                            width: 190,
                            height: 108
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/247921/market_Q1bT3Le9FdEyL37RR6dFgA/190x250',
                            width: 190,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '7459525'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '1',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '22: 00'
                            },
                            {
                                workingDaysFrom: '2',
                                workingDaysTill: '2',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '22: 00'
                            },
                            {
                                workingDaysFrom: '3',
                                workingDaysTill: '3',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '22: 00'
                            },
                            {
                                workingDaysFrom: '4',
                                workingDaysTill: '4',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '22: 00'
                            },
                            {
                                workingDaysFrom: '5',
                                workingDaysTill: '5',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '22: 00'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '6',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '22: 00'
                            },
                            {
                                workingDaysFrom: '7',
                                workingDaysTill: '7',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '22: 00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.4666375',
                            latitude: '55.67795057'
                        },
                        pointId: '8728121',
                        pointName: '"Лабиринт"',
                        pointType: 'MIXED',
                        localityName: 'Москва',
                        thoroughfareName: 'улица Мичуринский Проспект, Олимпийская Деревня',
                        premiseNumber: '3',
                        shopId: 52585,
                        block: '1'
                    },
                    warranty: 0,
                    description: 'Игра "Монополия" с банковскими картами. Теперь все карточки читаются банковским устройством (в том числе и карточки с собственностью), что делает игру более динамичной и увлекательной. Просто приложите карточку к банковскому устройству и деньги уже зачислены Вам на счет. В комплекте:  1 игровое поле, 1 устройство "Банк без границ", 4 пластмассовые фишки, 22 дома, 49 карточек (4 банковских карты, 22 карточки Собственности, 23 карточки События), 2 игральных кубика и инструкция к игре.',
                    outletCount: 235,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/rmujBjJ0WtzR6qCT0Hu8Jg?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=RO7b5ItwATbsufSGBeKcyvCs8lsNazC5jFxyxv6vP2Fr4MSmmJ_lcpKIxwmlkK3L12q5qd9uZaCHoaDOSzDFotlE4CPmWkn6HAuTUkvEU2lc7IKJtxLcvhxz5rl-uQ-p&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                },
                {
                    id: 'yDpJekrrgZEl8-2Xxq8q1breo_VDfulKkrUrhLrsPhyh26YRbs66JRqOr3jlTtQ93c_YM-bPEzH9MHr6Q2DE0oGq8ZF0d1Y4h4eH3wAl6NikO6WfCoRCLcFmxJPSQYXflXro59_v_fbdFf5_-tGB2ft_l-EMfs-U14Qq5x_S26y7nLpXCR9zMF4_8UqqVVCWfXba2QrIES7H82OXx4YLKTLVo5juLe_COLPuDg4Cvie5pOKwEvaeBh_-wPTik3aB04Tnlwl12gZqS6IbSMlfIV_TwhCPWXUspmAGUfykUEE',
                    wareMd5: 'EN6T56egLAm4L_m62A6bTQ',
                    modelId: 1730875803,
                    name: 'настольная игра Hasbro Монополия с банковскими картами (обновленная)',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8medeQi9byJrN0U10jwcsSTilVN34w7q8tsXIdCSDEmXOG-TVA0vS046aEkjlOaezhDU8YmRbNdD1WNWzcBPSIr4Pe4lAgD-Nx884BkSKmPVO2IU0Bujv9JPf10ol6OfRvBgY2IJWbOBd3KDqFPlv79BlzuHY5cbTPyDrGbyMbTuoOAjidHBZFORApJs1oR58UkgnoPwEJLX26kzdO2JRFPqSksOS2wqasLKWqBpFoOQuQTNWvp0-JRVhpLasc-RMPz2z4QliDW2ldaZfgc--Pj8Mb7WG3_TIJqVUDLGuDV66VOxm5_QusBZ77MkwcLNwVGhLzDtlI6x7nOFsZW_6IpGxOOp9O-LNuE_fcP7sypInOhE18Yv_bYJouA2M5QlnfVIA0JX0v1vwOSrJ2RUN0jxRoZSy9n_hCWX0fAXgJMNyJki18r_4T4JLsSOvAZ0mTBBDZ9X4wmV7M3yDloAGcTN3Nda4wny2t6b37P0Z8AL3x4M2uemrL0e1JJZoaR6wPOyC-mhoAx5tn2UTQbNt5EC8HqsTnv4aG9qJ6h_S-_KrjU9UJ-gQX8U63K9wFOsXQYI8UKGLTwRZYRpLvRk1tgiklxVRobU81AgKcMVKMxmpXjy_2XiWwwoj-E6pxp5Qr4l3C4k67dhW7VOvGgc6PLOWhw0vdF6ekjZsK1orVUrUuuPt3MU65sYQfJ0JooSmhVOH930DlG_DAvHtpofwZYR9GNZnjx_Bw_3YodtHefwcySNfkMjTwlAWDdLnHqFJaS-TVHsk8thrHNQpsjNXWkUS-lblTIFWUiWEM5KOSqwW27ZijYPufFFIkR8iIYEfX-WeWXzTTjZeTvyULRwTdLtklNQP-COG3AzMFWPviO-4?data=QVyKqSPyGQwwaFPWqjjgNtye1CaLs7cvGFHyN7moUbP3N-2HBJLDGxlkv3ZA-vmjQHGCjYJnAtrW3sidsvJuKMBP8clIEIbS5-KxPejm4UL9RalcKzyazKPhPEuzsgn0oMVvOL7J7f3j0ljo4DJnkxQ3xc-MIf-gsZWRJ6t5ff12WbtEQbywoPVr5VdtbACU9KeTi863PRfc_ObrucPO3OeTYS0xfXxV6Q5RiUY7Ff3p83FEza20TecbHu13xjFP-b81VUwOIuQGx0tNo3F5av6bt3XNCly-FfiSrvPvTZhzY4Y7c2QaGjJqbgMFI1l7Wex1GxdJGu8qCIBzXjccJqyFOrb5nezj1Ae5VxJlqzhRSXS9HJ-rNRFw3GoqS9NpQtfJZiQWuqQQDZUL09Rs8MmLLZrzqwOOSI8oe3hUtqC8aZ9oQH-gUwiT9tPob5FImehNf0jppr8dttcdJJQO_VFANeE-pDcDIW5ybe6NOCAmkYtwIkbJuV7hnrXX9XnEXjEj3WiruO2ow3Nk5V_4m6G5yCnV8sO_cYXJc0-RykrUvVNkxBrc49Jp78dm7IhK02WbehMsn-8,&b64e=1&sign=ead0c8fd5e8af0e5da19afe2b0375042&keyno=1',
                    price: {
                        value: '2825',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 264849,
                        name: 'Город Игрушек',
                        shopName: 'Город Игрушек',
                        url: 'mag.gorod-igrushek.ru',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 43,
                        regionId: 213,
                        createdAt: '2014-12-01'
                    },
                    phone: {
                        number: '8 (495) 641-03-79',
                        sanitizedNumber: '84956410379',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8medeQi9byJrN0U10jwcsSTilVN34w7q8tsXIdCSDEmXOG-TVA0vS046aEkjlOaezhDU8YmRbNdD1WNWzcBPSIr4Pe4lAgD-Nx884BkSKmPVO2IU0Bujv9JPf10ol6OfRvBgY2IJWbOBd3KDqFPlv79BlzuHY5cbTPyDrGbyMbTuoOAjidHBZFOT6-CU6u2eI550SiqCJn2qBErcec_Y2fsBi4Xva2o2YEHX5SJpxa00DFlc9ToZkApsLl3XVP64v71SRcsavlmefl0Nn0laS3axGeFJ4FlKXcT9A6uWDtreli-UnfBJkQ0u2-aRUsRmQlSjpTBdXJ-t2gRoKepaRQxzzVJqDIqf4Oiyljqkw5nQZErdKrIYqItsGwjSCKlICV8fIn7jyXDcxgzD2qy_IU-X6tC9Q2NzhOooOloUE82HmhFxc8hzN9GHOVNyf1um954puBpvRsRXmMSekQXClexBs0bpMMsPxtl1BCrogwxcAlqvC2xmyIHWpEkr3tQsfot6tGEz-CMsY-MKGJHgFwISTtbXocbIEkHhRzdbYqN2VdmjXfN3uItJfbU7ZEpondyjza1-4OYOL86jYIgmh6Kz0THItA5yE9-JmFjBNGoLgEylAoHc2S5YYoq_ekBQq2yMqIiNfwrFvZQDJT__TSY9vztZqZ5AgcjvltPCP1s4lEuoYseV3jkvaaNBIx_xTNbMdxVqqFgzjEevEsuzOfluToQpD87VHwjo8ddnsZYd1Ray1ufB-DDsFsmkFoKSbA3HRFrdgKHCo3Yv5pf04k0TeTiJ6qwf8_3i7f-sTvc-AYzOnpKm8DSaYJaxMqWN1xjZU_joA8vk_ZNIFuinfhKWml7930HmuIbZylwKoow3T?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-BgaqnhSJc6uy7qSLIShsfDYU5zaRZwxDttAhFF-RBhSXXmPbbMGldRd74boFj4YcTT6zkYLmjuL9mlpz1L2qnJ5W6vlNDyWS2x8HgQYgYe6fiAGcu_pT5plP_Nlf_9UCXaJXpH3go3wQKTwj82-iDPuSOTHyaBcFE10XC8kVfKkhaMva8-MyL&b64e=1&sign=e4683232c4c37c6eae47831c22502d36&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '300',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 12380664,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: true,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '300',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 9,
                                dayTo: 9,
                                orderBefore: '18',
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 300 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/248830/market_viya1t4JA53BaqwriWfahA/orig',
                            width: 250,
                            height: 250
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/248830/market_viya1t4JA53BaqwriWfahA/orig',
                        width: 250,
                        height: 250
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/248830/market_viya1t4JA53BaqwriWfahA/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '771-6073'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '10: 00',
                                workingHoursTill: '23: 00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.659075',
                            latitude: '55.757222'
                        },
                        pointId: '371722',
                        pointName: 'Город Игрушек - Атриум',
                        pointType: 'MIXED',
                        localityName: 'Москва',
                        thoroughfareName: 'Земляной вал',
                        premiseNumber: '33',
                        shopId: 264849
                    },
                    warranty: 0,
                    description: 'Монополия с банковскими картами (обновленная) - Hasbro - B66771210',
                    outletCount: 10,
                    vendor: {
                        id: 15157809,
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                        link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                        name: 'Hasbro games'
                    },
                    variations: 1,
                    categoryId: 10682647,
                    link: 'https://market.yandex.ru/offer/EN6T56egLAm4L_m62A6bTQ?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=RO7b5ItwATZDUULdzHeJB3MZd_0OLHFK4F1TOS-WZgzGNAeoPRc7gfzd_ukdJHn0WgU6kLGuDiDeF5cEbo4yqgT-bVJoeItEU37gdiOfuT8N9pyPrf2B977MJAtaAVrq&lr=213',
                    category: {
                        id: 10682647,
                        type: 'GURU',
                        advertisingModel: 'HYBRID',
                        name: 'Настольные игры'
                    },
                    vendorId: 15157809
                }
            ],
            page: 1,
            total: 41,
            count: 30
        }
    }
};

module.exports = new ApiMock(host, pathname, query, result);
