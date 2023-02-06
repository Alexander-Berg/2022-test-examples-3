/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/model\/1724547969\/offers\.json/;

const query = {
    text: 'LED телевизор LG 43LJ510V',
    category_id: '90639',
    price_min: '13000',
    price_max: undefined
};

const result = {
    comment: 'text = "LED телевизор LG 43LJ510V"\ncategory_id = "90639"\nprice_min = "13000"\nprice_max = undefined',
    status: 200,
    body: {
        offers: {
            items: [
                {
                    id: 'yDpJekrrgZH4O72wbGXvLbh5vYByPiHBSXQUC8voI2F5b_8TSgRWgMo1mv2VEYtV4lntjTvdxQiRPTeq_D8qyJ7F0XvNWa7b8LJ-S1UWfnKbLldvAQSbm6NMnHFr-NzQBk8k_N6qDawZXxob8r5s_T_BagOpjpGka1iuH6S0uhoUJ6fAm92X6uQK3JvtjGZ_VOcTKd6Ow84OFCVrq30ZHFiSzoJWypKmYk6AR68Hh1zhpRdA6b2nezPookIthqRhPoJSXlwpG6wpVewwi9qKP1uAKoJkQ5et9nqG4eVY74M',
                    wareMd5: 'v9BMeuMyxeqMESXJpPF1uw',
                    modelId: 1724547969,
                    name: 'Телевизор LED LG 43\' 43LJ510V',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wyt77juWO0bPKuM54hkx3tTWyw5u5rUosqjF-6vxr2MdCYpF4twWXT-7E1cNOPuTSeAdiECtMAuKkE4MMM6ok0j7lujZtZ0r76jV_innOPvwqNEfG6EWAEBP4ljCqUYIEP68VSqIww3I2bVd_JGCXR-SWzN5itGEVrxcPBjRlNFDdZ9NZh728_dMXO5cWPYrEtn4XrDVr2LsdJB5T9T1CS-oZIYIz3yLMN5xaz66RD9bu65kAzm7Vq7WYXSNaDFy-M4_KDmW9NRwvYdlS5dTp0dfDc8XRbcGAy6utREX_74Kv7jgOU754M151lSKDXTYzVaoYIZRsynba9KdKJM9gJYraErAc4-0k6lA9eBAjSfmf0_-PJPdQak6zmIk1GOYUz1giiGFauFUnNBWzyoxXwkWrtUucaQ2Ftt3dDAY4aCpdy5mcF1VhyrxhUTYK2FmA8-3pvkBwl_Be2WE9WiaAeTGW8RWiC2ms4rqFlrKIHbvTsrzj0uyJqt7M8BKr3PFGC9upCwqmizXfSivvTjDTwMXmG7CTNl6j6lSIYJL5Y5uEE-K4xxDYbYYvAXq2_2QAbATvzJlB5nksqgHy2PXlJX11Sf5TDN6PZS8z0t6r80iNXG05IxYyaZy8IArfVD6AM2gZd5EeUXh68m218h1-C715m5Gav9_Jxq3TENY3BHmDpdjCp7Prju?data=QVyKqSPyGQwNvdoowNEPjcZAdGUE8Nj9uoI7brnhxttHPalY_4BXqQ0u9iWNDs8o3FRHev5UtnChcjUWFPej4FWmlty_4IHht-8v1e76dEc-Ftsh4Chf44ryWkQ7x2-eJb_HEtzwXkPaaKY6UaUWK--alvS_kgqCvcgjaSY66c-y-wtEyRzE_Vv6PIQZ56UFZ0QFFFpA7mmyrGyyQvgZ0sWJNHWNbkz6LzJaSF1eGyqUpivY73hDOIcQCe0Nc_HJ077K6RT1lYWMHxgLoA7yrmnBIBpfkWc-Y4EUUZ1VZCbOHbT4GCT9a_BLn-keECaR85ztFqa8HU2NbyITEg9ctINLERNO1HQAKmHPeOaMgRq_ckuG3qS5KpfQbYeM942OFUHsqJqHjsTmAzxmOau7BML_1VKMonDEOSBbip-OA5o9DrLmOiI63lo_gdhspUr4&b64e=1&sign=41ad71ab78ab442e7093d524831be2c7&keyno=1',
                    price: {
                        value: '23178',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 425757,
                        name: 'Techmela',
                        shopName: 'Techmela',
                        url: 'techmela.ru',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 10,
                        regionId: 213,
                        createdAt: '2017-07-13',
                        returnDeliveryAddress: 'Москва, Проспект Мира, дом 119, строение 70, Офис находится на территории ВДНХ, 129223'
                    },
                    phone: {
                        number: '+74951503530',
                        sanitizedNumber: '+74951503530',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wyp3xsnBLJ9uEMW1-zHHO1NlQKvSanmNVDElCNMWFpKAPENWA6Mxi27h-Sxkh6SP0rnFtAZ7nbpv3gFq34hHZoLEry17C6WfRCpFVnEn2CF-hb7wGEAMQsaGl3jqynEMjDU9cUdEB0bgT0ecGJK1VLQYo1PV3Fs1wUJrq9X3xl-cJnFaw2aQQPwSEmQeeaxMiqFnEN0_TKFsArd_Y7-msd5Tp4JlaHUP55sJ2Vylx4lj44O72zoXvzt6ZXsn5PmdB_BmgT4P07kYQGRSft4xT6xeEUI0VKcYx8sHSAdnd1Fjk85_40fc2BKXnPmWrbw2kn0Qrm2DMNzoD5uZHlbLHScCRFibR1HKpNzlCx4kJFL5y9TGoYm_d-zBckpy57Bh-ZBZMYLUAWDbk2klvM-gWdCMUOkDl639zFtgUi1XbikJaGJulBxLMKwlgWMTfFtE9Lyv5-xSPKqEwYw6YrwXuHePnCOLCyF1_kGYzNzbzALU6ba43q9hj2SeddKA4rn8Z81RDbc3WnOyjybS3085AwxjDACZMAEATFVt5Ker7nx50biPXuAL9k0_lFj0g0V02gE89rV3_fNjEaDYWlf8mMKHUbDocaCHw-SP-IqT7u36ikVq0fnYvhYKCgSDQNVNbYg8CT7JS69SLZZzky-C68xx21x5pmz7QyWpJTL0L62UZ11Gny7h4IZ?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8ntyBimyPdUpAkkkEwg5-kcfTvQPCeo95VPcHyaa1I2A34CCsw7LfCP1XEby7sKUfXc_CJSIcnoQWcZ35eAAhJDI3nw27xV8gu_pAd4svEi0AL_K48QiHhX7-18gMznjSCX0c_O_1VnkyuzxvAGOll1t6KudbZG46b5_v0xHngcQ,,&b64e=1&sign=a2a8b3b9803258f548aaa6b9602592c8&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '500',
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '500',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 500 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/163900/market_lQ_fiPrRgh-A69VqpyOlmg/orig',
                            width: 800,
                            height: 528
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/163900/market_lQ_fiPrRgh-A69VqpyOlmg/orig',
                        width: 800,
                        height: 528
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/163900/market_lQ_fiPrRgh-A69VqpyOlmg/190x250',
                            width: 190,
                            height: 125
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '150-3530'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '20:00'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '6',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '17:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.641683',
                            latitude: '55.829102'
                        },
                        pointId: '1139560',
                        pointName: 'Пункт выдачи заказов',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Проспект Мира',
                        premiseNumber: '119',
                        shopId: 425757,
                        building: '70'
                    },
                    warranty: 0,
                    outletCount: 1,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/v9BMeuMyxeqMESXJpPF1uw?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtUS_Lni8CRAEHNeewV6AVV8yhPpMIN7L0Ltnid5m1FUiyKaZ28_sLLkw5kh9ubFsi-3cpIao4Sg9PdAiXT3HM7eZBTUrk778JjzP5jHdpa7GgCU8N0FM6vM&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZHyXZWnWVyppX1pD7ROqZxtC8EudWLIrgc3z8cRaVwB9v1Mctw-9WnTnQjbi547hD-NYDUumHb5TlFHHkyH091gJzCdneIdTy5k2x0haJ-rkrXmWiZ6Gk_8wciWgHlbTupgyk1REtU7oHCLXgB52Nw91U6NtQiRn6Kibe2zDJYZmx6tmixaBPBg0widqk23OWuzl0DgQh9J-JUII4WK0YyfvgkydFiScI0lTdL1SZUupkKMw1jczu2YtdhdFHgvImqzKigHZXMWFv0aNk77H3csAIF2ln5XxvU',
                    wareMd5: '9afvNBbAMWbGeabzNoUe4A',
                    modelId: 1724547969,
                    name: 'Телевизор LG 43LJ510V',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdL0yBxy2UoTesEbXiLdWQC0YrC_gE_Qd6onbK_delKmE0aG2l05TlMxZbJQOYoY4g14RnFbbxOeqlTFTu8HhkLzrFC-VEQ2E_5bE5B7zB2NgWtKEUg4Wz9WXUJNtx6fYNi9aipBjYvIXOln_wmmDag_dKe_RzY9yTpv64E-Xm4OT0x012FSUJt_tchkhwgsALxdhp8T7Ni4ByVLenFBppTKj7WLuwhPQyP8-zOdaEoBUJ-W_loD0ICbMIhmcMA8OvY2LI3_dhufLWWz4ex8N4uaCm77-6y9QSPvLLyFVRIub9hexNmoaxJ9ET4TL0iifYs_YLp_T39BAOuGtGgmEAYsA1QfesSfGoWp0JmBD0rJYGGU8ACirsg1H-kmnMM5IW3D0dht0KJ-AvbtspG0J9UW2ekSn2uqG5-8XQIx5QGw2maDp0tgOjYnMlNti7AF7Y6BJrKR4D0w9F1lSxgsfUKZM-mKKpoxZDSiIT3Ryf5kbk1yvCy4uk7PeVOohpekGZU8UPt7Hu8oyttyXd-DJqju_yuKCuqadj3k85OOoNZcbB4rV_IwtL1hIZiCXtoxqarWX42dPu-P0H-TdClSzfnRou3R6zF__GUWccjAyS_jqAgQWJ6HgwQtBHvCpZVPzNgS7TaXS45mbrobkIaecwVBfkhDVKUuqAxRelHojK3G4rmvqiRwJt834SSfisvp5B9wJJ04DiCzXFaqTD7PV2i-CU4h2n5aDpVcf-W8axxEtdvNamx4AP5oB-guL3mum9dEzB5Dzu16cbggcUZ9m9e36q0EHt_N97rJsdwc4OEe9hIaELKe-LhN57nMTIhtWmvg-IFZwFE6hrppzkW7FjdH3HoGH9lqispJSGx0MJcog5tON_QEkI8,?data=QVyKqSPyGQwNvdoowNEPjWjJwPNkcQ3e1JCngwCpz3Ts46XKE1d0rn0RFqd55ILR7l2uHWszp8xZUrs_IMrOqXDTJ-fI-qgniMwZgD6LGsHyr1dqxKYxPz_1Utcbezj-wK5fIKWcqPAi1S2qTkWI0xqhKhu6iUqdFfsFV-pxC03FxJFnAH91ddZ1Ln-VUgGXo8rE_Gllu9u7PNtwS6uesudFLct8NStkAdwb2D4YgxZMxtLwQ32hhNU6J92pK88O1JYjMMO9goT31QhZTW8_2zYk7rZOmRkwZV6ujdlBr_NrBRJVcj-oLyVd_TiPDotroOZCEj2QKPpwpNWDxLMrA4hy7a7QzrrjqG2zif-C5fJ-KjZZaLvJm_TZBkT_1J3zTIBqHVKC9MU,&b64e=1&sign=f3859934a15f3d4371b282cc769db2aa&keyno=1',
                    price: {
                        value: '23410',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 197695,
                        name: 'Assortiment-BT.ru',
                        shopName: 'Assortiment-BT.ru',
                        url: 'assortiment-bt.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 4556,
                        regionId: 213,
                        createdAt: '2013-12-12',
                        returnDeliveryAddress: 'Москва, Нижегородская ул.,, дом 29-33, строение 27, 109052'
                    },
                    phone: {
                        number: '+7 495 256-22-56',
                        sanitizedNumber: '+74952562256',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdL0yBxy2UoTesEbXiLdWQC0YrC_gE_Qd6onbK_delKmE0aG2l05TlMxZbJQOYoY4g14RnFbbxOeqlTFTu8HhkLzrFC-VEQ2E_5bE5B7zB2NgWtKEUg4Wz9WXUJNtx6fYNi9aipBjYvIXOln_wmmDag_dKe_RzY9yTpv64E-Xm4OT0x012FSUJvPM3pzjeP11mNl2xTtPTs3pjbKR1ZP1n5Zce778NYcbOeYJbTJInSxvSBQ-a2FZyP76OYZzV0P6ccKq5gUwtZeraBrMQDEs2klZqdZUM2n41uk18PcRPaD1E_Ink5Jx_KwCN2kJKbpC2mOIVnTzNsQgH7Mq49O05n3qv0Xq2mBSB7qy7HKPOrgDEU4SGo96gDaiK1Y_Uj5rLl9HKfYwXtS43O72sGbaW4HAD_4sWgVvtUvNNPL56iKXXGF0wjlVS7YDNdW3dLBtDClrIDsHJwdLlUuc-T-BrsdI37E4ZWSpA3fRZLqqjNs1uIOClvpE0wOgGSIiSUCBImT58vUD2_xUROzWQelWmRdjnKNRkgXTHUKG01tE26zhJ6hGlxWlCwGzNBkcDT6hbFH913Xp1OaJ1iQiUK0ffcER_xu-e8OcRFaNQU1u2bLv97_jiZiLzusyA9O9--oE-QmUeA9Zd1YVFIxOPVguXEGcFk7NUjuNt8NwnrpVxw0zLHqur3EL3MUmwWmLf_KJ6Mcw6oqvmXWQLv7gQcYK3eVVHMr8Hh8L5pgWRkiTlDXrKKB6WAltyqmPhmI-pQH76N81fwCwntZi52TnGTDcg5if6w6wzQpLnBNVo862QnaPxHofTOqim8-jZaMtJAQ1cykM0VTSLLBQLmNklJ1DLUnzD9VvBB890FrNmAxySsftKgpqafiW6c,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9mz6cYtBKXueW9MEZJ3grSMNbEYwLZbz8AgP5p0mMLWCe9QKJx_eJhjhO0VokYMFko8hIuza5KLrN2zlEfr8yQQxbNrCIbKEMv-rqj7hqY0iFS-fi_ioilIbaxKweExhc5S7F5hi75JfyznOFhZLdgw4GqyOT9u3DCLPSyADD-yA,,&b64e=1&sign=29db4eafeb23b242f0070667a77cbaf6&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '450',
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '450',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 450 руб.',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [],
                    previewPhotos: [],
                    warranty: 1,
                    outletCount: 0,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/9afvNBbAMWbGeabzNoUe4A?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=sommjlDwxwp0cWWwA1sd0g6a4d02caoNdCuhMl_YJcfKPOsa4hnWn-4vQKgphorez-t-lcJ92_D9uPg9N3wCMBv85wEDSqd_K3A0TVU-xkC9qEQWc9fuQxSpGY9__weDYjq2tdXYgAs%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgQo3vxbrZd7myE8WAwQxR2WndA8O_KfdtrzaaRtVfRj6MypYmkCw67gio1t06LLqef8_OkNFxWLb9MDvF7w_Bvn7kWZ_iAZfUuCXY_eE-8Z4Hycf2s_zLlj9BogaUgq8pp15-8s64PWFSj3LbVIfIejJvdq1dWhS9MlLtEXG6H6f8QQxl1K3gKjAhijwZxm8nfIvgwvBI3JFMttmHtzOChr8b9T4Ea6DPbC3etrEfB_xtHnFghfMnr7YtKDjQxZK61BbsG8OwSKNXLMht9WYN5wbcMIOifXi0BoB9m2NtE2bQqYAJw0zhY6r5kmusq836z9gjYpI00tRRiZgigU1J0SYbblaUvBhJuPC3BPUTU8HGDCxUInhXRsrAmDPSrD20O3YHF9HvjHoDAOrbM8aJid1eQJGy60D9uicvBEsZmYbR0I7JMnYGXMJ45yiM5y03IkiOBhsYAyn3t7xdDE7-jY66k9CfPmdVeEAf5zqX2COZF5WCYppwKvL7Xwht_ddhcA8Gb6qh_G6adVqFWfsoaxe52IGoffGi-uZ_bbEEaT2EhUQNmG6hucvggCV-d6TU5svvBPi79axBjQj2L-uDbAdrJmDXCOkKEdys52s15MNhFOgH5pjsgXl0jHD1k0-nc8TCPiaihchY9zohK-8S0oIKQZpEFQgeTlWdVfC_hIG7ml7QHxnGbwWGT-IcJdZFkB5MHHyOMkchjKUTLKVhYTGkkLp1vY1m20LqyPUJ8mKAYdYUWMGJ1LafpDBlc7827EQIrS7oKOvC6ZmHhP8A-S-xWjAU_wTvhu63GNX7MPVGN_x1bA24QdznwZX2zw_Pg4-C6gRDJGpqjkS4coez__oVVPaOLumS-hCu7LX0N6ocRPM7zhePiOqnPxPlWVIYDUsACnHQ63Y?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89Bv4dk51y1ErLtlaaYxHVpuCcm2pKz4Z6K2VQzXeFAHKoCQC0fIWgxhNeJbD-E4YEs-EhMGkcoh_zST3hUP6boNSp8nojSPOwGXJ_XE9_44Z9gfQAp_NirJUfLTifd9PyEXR2gtBDOnv7E_JMg9EtzxHvnhrJgUcwbO5sntUcXwlrjYIpMBgYRudGEPwo6VLAYBItbhyycg-Ju2ooQcrIfDa1yLoxzpKGIS3PkrL1VAyYYDPdFMBY7EtXhSWGLlsgH2GS2ZlLGg945jOqgx8zwZcOFOHzobnGlW4gK8D4l3H4kZL2RViXGR8jFLCoxhzkZhQlb0o_5_LtIxSKTmo-_O2hI_Jbs0t8xZ6JR-POAdLQ,,&b64e=1&sign=87bd2d65f2e9f5e976b5ea74093286d3&keyno=1',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZH673vrypnRFH5nNUQRhvsuTGdxg8l7pNET0e8wf8ZwUo1xqTJXJaGwWAyE-t9QJ5UvKivVf7_hlOQYKRkU5IsB9qF8ZCh9UMtqGtKvUumLpLJQTK0zp7d34pe1NZay_H3FMGylOdFBkMGsEF_R7lIFPLyJQd3r2jbAluYFqhYON-8qaLgkKw7a50_3meB5moohuzAxH5aarYunXNHXOKBPjqh00fmqaCAzsdwMDjlSf1jxA9O0Ctemnyn2sr9WZZAbii0CN-Sef4KJJSBzymjUL6Hfi67jzPE',
                    wareMd5: 'uWHU-dezfUsmoT-8hNeM-w',
                    modelId: 1724547969,
                    name: 'LED Телевизор LG 43LJ510V',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTWM6wM0TqfEl3YfYJJ8uKRQOWUi9keH4x2IV6gGmNCuLpnWklMjpLqiB40W0dwPcuBct8qDnfbdGytkANgG0wI9Jk8MxCXGkqIwPwgnG0CCM3isMoXgCwVRPN_kc69jKdUcxchN3se9YKujlXePp4PDoQAXsyFfjtq8UW33KryoBgaKVlC3-CFoO8Lt_HbUqru28FpD3kOoWThcxx0wiKBPfLiDJ-QgPHwiBGg58dkdXZguHix-2CdhhnfUvSTh1EXIaMQfjWbAl1Iy6NXRPSAt3qMcAsKdBmvP01siXZq-S7lgjzFAGkdxndaSB6mj7OrWrBj4DEDbYIIO0ahMc9ii2aWy38hTOx2Xd8IpTEs97uQCGPfPXDjp6ityNhXgMT9cysVdWblG9g3aOTVguHZC8MlGmRgAMYcIMQRK9k_Wy1FOydmBfKvfCTkWGTgoU69no7oxvk4-hK3AqUPMry7Pl66gkPz78y0nLnmTJqXunUtkc9xNSXsUwGnzXuzDYrNgw5uKrePPQukUyWB2p2As_OwcN2U0Z4pmiUwzUmzB1ePUt4ItRU0wYVJd6mOvSvim1pi2EFB-efb4IIKKCNTk9lp7yojOsz6ctAu_Kga3YyDzCwylWyPisyW5CYfDaXfJJ_SwBzumZR_ofBZfa48EE7-JxhiqgqJDuaDZqDjEUqq2hp45Ak_WtjsfL9eK5fQkZZqKEn4aJmLTl6qd2IZyIQtYZchgS5-dPZRzLS1PRIiGpIi1yxd9lezQBYvH8ETt5HH5VRBT3jHIBFRn_gseck7Ryotd2DFWi09vG7_8vl7qjGn5-Vd3yOZb7mfYqyT0_0KEPZw4K-JCAGmlA-NNH38zYmUZC2AvUU2ira7k?data=QVyKqSPyGQwwaFPWqjjgNtydMHNnehHqr9s3lnVzTSgjShofJ3piqqaru0ev0BslPGfF9hk-7QMcwxve2HU2wV3ocR-yKu1eVhzDpLIaGHOOXPCpw6cGrbxpWooDzDM8h-uMdR6mrudo-K2D73nHuxnxFs7suAI28yR-aHcx2QIHpzY8P-1JSLVOg_HC49Uq4VPjQ8alBz-TFejQb5_sez9l2aA0dGw0owSOw1PIDD85umbzYrKGEEYL8a5XcVZBDuPTVD4nsEoqZiMGzv8HTbI4Y3lyqJoLzbffb_3AEDzK02AAOJgsN-R0s9zyiyVHWhCDkB_5ofQzmPIHn5lQEY2v_A9N9bVcNmww1vGyVHtm1rxSvQObdK09ybvdHePwb6k8GbOqrgXwnE5-MkTp3FUViUsPgT4FtgHHzxjisOhmXi7nJjk1B5OYJEEeo2Pmwr5-dKALp7GezjMXIWNsY3w92TQ0us3uW6LXcEWn9oYdxMLB-39PthZ_t50BcQLD4m6H0qYWEpnLl2J6CzFroXgPg3Ity-XimWnBcszNlITuJ5FJ7RRTDm6E_oEldAOkIT-HcSJ9zaGNYG_Le8RKg5kJGjaEfiBaDuCSdoWj4XXJLVNFKeE3MEL-TNGheejC_gcArhnaPc51tYNSQHERYA,,&b64e=1&sign=9bea0a090b29c38aafa0ec9181aa88f9&keyno=1',
                    price: {
                        value: '25180',
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
                        gradeTotal: 52893,
                        regionId: 213,
                        createdAt: '2005-06-28',
                        returnDeliveryAddress: 'Москва, Ленинская Слобода, дом 26, строение 2, ТЦ \'ОранжПарк\' (5 мин. от метро), ежедневно с 11 до 21 часов 8 (495) 258-81-30, 115280'
                    },
                    phone: {
                        number: '+7 (495) 228-66-69',
                        sanitizedNumber: '+74952286669',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTWM6wM0TqfEl3YfYJJ8uKRQOWUi9keH4x2IV6gGmNCuLpnWklMjpLqiB40W0dwPcuBct8qDnfbdGytkANgG0wI9Jk8MxCXGkqIwPwgnG0CCM3isMoXgCwVRPN_kc69jKdUcxchN3se9YKujlXePp4PDoQAXsyFfjtq8UW33KryoBgaKVlC3-CFsD8rarY1YHXuempWxv_FKFqlQw6HyND_VxGMN6Q4CeVgCehYlmx30Fbg_J7d-3_rFhZ0LHlVMf7dNuutOZqln_0_T92wDxq3H448ArnyByS1tICAfG4LGbSI0SFulxiHW1wkesjhY94i6LFJlrr__xthN3ZBhMG3lmFMQ7dzhVM-Zwwud7HOlHvVRUrSOl9tGy4rxV33Qk1LmnNAMHOkamo7-ZLau6pSIOwbIcVdOWsPy2XFmgJAEZ4Qrq9A7DLZ21eU9u9ez2VEmnOpKjH_Lg3VEfM-LydHticLesnMbm_RZ01HQN-xqui9qn_xuWuCORb8YQFVa9X-PskYTByLMSCfYHSnRzAt-sTCUCWKZc37YdtK3oGIHvDchs6qH0d-PA-1TcuNfK7YSDRhtwD8fFS6SJV4P5t5NzU0X9RMXrsvxx4_LRmmYmnjYFwK0J86i4J3m26r5_YRDWnemI5FtdaoFbNqhEuuGQfVXfFJm2JxtZ0-olFdGO027Yni4tU4muna9ruvyxp4BxpqY394ESs2ASRNmkLoHdG6eHcX3jXabc6jc5d6Jh9n5ACdp1jh87Mu2M32fngqBWr2GjSzTRK5Kgv8oNTe6pzXqkKBowZW9M-GrmB--jveWlNG2hCn7Dt-5pdPgoHlwvKgffVga-dwHSK9_xpQHHapcGMOCwm9lgLpHLW6h?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-kQ2KNg2f9H5aweVDZOZtdyIs72fAX6OO83vfUl-DuS3xwwwKEKLlgAes619Rh-5zHitIbrLl12OhofpHdGP16WJbsh1d-AFOg5Etant3b7-y_WksPk2JXMNLevNlqW72rNvKG4_U6W8xLpfwHqmC3JMBSTVMemCRoY0D6czW9sg,,&b64e=1&sign=126b9c33deea8cc69eea2d2c85d18218&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '500',
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '500',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 500 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/206654/market_myG4UW7srrHrPHywu3JIHQ/orig',
                            width: 300,
                            height: 300
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/229624/market_8Pb8Z-VOSAf7qT1htk-vYg/orig',
                            width: 300,
                            height: 300
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/213450/market_1svfg-xTCk_B4o_-qfs2mw/orig',
                            width: 300,
                            height: 300
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/225310/market_EQQ9EmVcuoYDarcLW2rMtA/orig',
                            width: 300,
                            height: 300
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/225310/market_A4FDuGhbRxfwnNgAxvrnUg/orig',
                            width: 300,
                            height: 300
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/206654/market_myG4UW7srrHrPHywu3JIHQ/orig',
                        width: 300,
                        height: 300
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/206654/market_myG4UW7srrHrPHywu3JIHQ/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/229624/market_8Pb8Z-VOSAf7qT1htk-vYg/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/213450/market_1svfg-xTCk_B4o_-qfs2mw/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/225310/market_EQQ9EmVcuoYDarcLW2rMtA/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/225310/market_A4FDuGhbRxfwnNgAxvrnUg/120x160',
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
                                workingHoursFrom: '11:00',
                                workingHoursTill: '21:00'
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
                    warranty: 1,
                    outletCount: 1,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/uWHU-dezfUsmoT-8hNeM-w?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=lxxwNwAjKOpCzEXHhwDQwPfvA71DPQWaFj6PdjkgqX2fTrz0mcWJk7vNI0ITIXi08AWU83psLuziMJzoTnhlvuFxxaVmC4WqTLhqYc39yawxb_BMDSwGnWqU8JB0VJjEsyBql8gkJX8%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgVy2Z-7dybbPI6Hpde-GT9JMIz3wxcfuWrVCtXvsQu-029QzYkU_Zt5nfdP14oLmV9lnJ20zk_TwsSTCe6_SAlbw9nJ8yqXNJKSyiUH3lVK63y6BQw9YZQCE7ck9Ntm0zxTLciamAekv-Zk6PD5Uopdtxmqnm-At1F2M836RJCimgTgMYQtc0mzAYc7iplKpFgG7H3vYbL7tEdFzfsIQk9wZLmWWGTHgQFO58O1I6_64tuUmuv5rtNQ0x-QqnMGpT8BvNjQ35EV3Jpe3WVWz7YIiPLWGnZA2GFh_2WrB8PDjucXWna4JPG6fsz26OjgorvSXvKiCYWq-kKnco9fzL_KES7NqKIC0lSWbTukHnMuyMWdNhZNYHTy9ZUI02hFD1DUDfN8kt0fuOsi7hjMJkIqt_jtxupqs7LUkDr2IRdVKs1nkSo-QKv5lFy3yP5OYoDFMFBrpYQhyTM4EA5kQvSKl88TMBjt4O2g_Pq7LsI7aaELOpCZjj66t7W1TWbl65g22FMnOX-7c8nTCZFg0mAvHhHKguqTg5xEJ2ASfhxyrewRqBUSFPmo9LKg6yg7mR6CwTpgWOkzrdlQSePuELZAMF4YSrr-MPIWwG1b7g2y_DlwzjNorNP_5TmVsuRfG-lj4bJEdrJ8wXEkL0vlrcEvI-P91FbbGxtJS_T0zA_BKsQMeRGVTRK0x5utc1p4-m9trfWUvBewHwTQwwP_7FxkxBbmnVtOR2MB0T-S_pZY5U0c0eMAYPZCPxTe_AMvgCc_THgJ2ospNSjWOzKf3NvQd9AfgV7Q9OLt67nr6OkvREmZzZuQi7wZ98DiCwFA3-aojG68kVPk0ULrniNxDd-YYUUPfT1_xolDxCUltv9B9oPGnT-zxQpXX9mnyJCf9CTuPN_7-I4Q3?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89Bv4dk51y1ErLtlaaYxHVpuCcm2pKz4Z6KNuzwn9_pJlkVIMqH2Q-gRUi8vJhWoXBDE7C3Y0FgMYbC6gTOBqMWKzsXvH0jKXcZiDgoYQ6bKqccJJ177oWhZlsVnQTINNyqTgxVnmot6Jyf5gBa0Hu6z0iAjti28tXJnPKUn_1gVHIbzFhTus_mJQjr0CJRgl5Zw8mAP_g7noGWwLr6gS4K8TCYSx0NA1uZTmk7pspKCkz8XrUPFjS5Bpa0skYs9VYTUPN6BzzYCgD4YRAL5KIPZ0tNtK6wgE15sN4_y247gxKVw2On2Q3Y_JxZpDYsDdEb6Lwt0h7G73bVkTtOAtRsr-Qf6OkmUbYWw3oz8gkwN5w,,&b64e=1&sign=31873287f1d9c3347217d9be971a7770&keyno=1',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZGSqhn4PcDKkq4xgMwp3EeH7-5k6XUxwd0K_yFqXTxxxAlFizCTJQ-deln16titBse72WqPanD91Cg1-T-WFYvGAjaQGqWX5xOUj4dGCuu3F9DtBvApGs3wKtyCmQ5Ju9e8uFOZZeUMwkQSkZdSLwPGrY3DsCzsDBKrzhY6C3Ftw6jf9N2wDtGsVyl894tWuqldRa3kNlK_S2mQ2GD9gmMBZ1Q4aOECNqQIfV2ljVyGszcmIcsGpaduLdYpK8lFlEShhnSOcPN6dLdK0iItu205U-vi-ItiR_0',
                    wareMd5: 'fblPxhEofKpjoUNfB2ORnQ',
                    modelId: 1724547969,
                    name: 'Телевизор LG 43LJ510V 43\' Black, 16:9, 1920x1080, USB, 2xHDMI, AV, DVB-T2, C, S2',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZRRrA4uRtrCfskF3wVw1GBXKi0h-x399TyR_EKCacpvJ7Wn8aa1EfLOGP5ayfa36ocZ6IryJ0FiIQvH9jnpbKlc4ShrWMk4ygsaP15w5pJAVzTqLWlhmNmiXkAMa9ftbkQWgbMEWo0P4FLEpzEEkod_tnwgzdux4ia-sN60qNmSrJNgB8W4uYeUbNNP1JD0Ti7ZhLSSPkuqnAAlpFe5HvjTYFrn9lJt-KnDGNAKcBRFgy4CaGSyKSfEWJrpsBFdjdyK-HQp93-Mxsx9BvQ5CQ4Q2FTX6ZzXHthZpD_kkxStcxWhJ6PROlanG0kfzx5ExE5tg3lq9TEgqVJwTDgNHoC2zws0JyU_kND3uC0A9KFxK96umIatNdFe81fCNPsEB2WADjd0cOvs1oq0aGfAg0ppmIedohnsJ-7KGP5KmPoxYUk8dG-1Ecwisa-fRr46HAGD7XKyvVNYssgDc5RaQ5fJJfIwCMR0XY-rKaP8CZyIZQQQQnQx5UOP5M9ydkPiS-httbkkp17UUW6WWWzI1rGOuRvl7Knfc5rarKaX5uDuDi6oKUtSwOigcqOgpuXlPxKRmT8q1wLG4r-AR11h6MY2tNmrqNteGptu_d0RF1UBjOvMk8nL56bp7o6tasGzDsFynu6iMVa3984sV9Uo6z2yS2izVkx5qQ3jN28tF_4w9unyiLa4uudycOFx2YVCTPai0D26qQnX1jjSrAT5au2L9ZhQK-cLQbbhh1inxNjGxGmtApz9LL-C0l5n22-zlS1PWJ-KTLxO2KmT8AC8LEnop5JZD9sszCAxanXgDifxNYbAI03WAwVooK0jJ9QdfL5fs3Xn__tQ4798k-7ni1Gf3k-LZTdgriaGIHM1MS8u?data=QVyKqSPyGQwNvdoowNEPjTQMKOTl33e6NgsQ5st8WhcPL2CDycAkNmD_nWgEWL1UJagk_YqedF7ideewnt_qZbqLVDgxQwUsOKvpkBU5Y0DoP6fCPbIeCGlRJA9xNleZG_8hVPF2LKZYme3gWSZ2dccAvVInGGTkWD7JGkasIyHhMoUMej7hpVWIiLSyFTdFFm7RivEk6q9CJVRRggpv5BqXImb9Yv41AY0N3xRiZ6bKWKXlknC4UTx_1OvYW5iNCfT1qKQCNy2BSp2iPQLxglRjWw8GGn6FQtZeh3USZdH9tdH4vJEuU6QB4nU1VEeMFw0lUrqc8mwHnsMFrp5457djp3eAxS3OzBEG_FiOnnslr_EP7YUkXNsjdArgBZXDZe9f3kcD6HE9O5gI4x_m8f_4__Sx_qbQXdJZMrR5KPeBHzII31xfY3qMUj-qJKH-gQ2RWYGS6Q8nSttiJoHAZQWwsVuwFNGxgRsy2u_X2brvoLM8cLkuVp9TK0dQ08bac_180V75EfF1ILycu9l7Zc0pkQE87jhoJLrCeXiJ3m3bXDs9dd7r-eRfkWO7_PLk8vAbwdFQtlyAT8mxY7P51Nz2RUrXYMIQpV6S_ccy4nTh8lpOPPvb_hYhUeKrqLbj6ymaZWQJd__VKxgM4PAkbKjCHwpeNrIXCTVwS3OQIEUTqRBBYMX7UctoFgPSReSSn4qkWuvF35A,&b64e=1&sign=d27511d7d43bf66148e5544d8dc96463&keyno=1',
                    price: {
                        value: '25990',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 12138,
                        name: 'OLDI.RU',
                        shopName: 'OLDI.RU',
                        url: 'www.oldi.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 8543,
                        regionId: 213,
                        createdAt: '2008-09-02',
                        returnDeliveryAddress: 'Москва, Нагорный проезд, дом 3, строение 3, 117105'
                    },
                    phone: {
                        number: '+7 (495) 22-11-111',
                        sanitizedNumber: '+74952211111',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZRRrA4uRtrCfskF3wVw1GBXKi0h-x399TyR_EKCacpvJ7Wn8aa1EfLOGP5ayfa36ocZ6IryJ0FiIQvH9jnpbKlc4ShrWMk4ygsaP15w5pJAVzTqLWlhmNmiXkAMa9ftbkQWgbMEWo0P4FLEpzEEkod_tnwgzdux4ia-sN60qNmSrJNgB8W4uYelzbk7J1GVPI94u6UjsiqEkjjZkYBW8b7pAI4OndLd6971l_CXyQa7t106SR4PUwrwAzoQvBRxJO2XonoatI46plE96vR4Nsx0ZSxDJgDR8mZhsWRbrMVUTOYOHtNwnjCmWOjSKL32q9HVONkd0Pc9Bg4I1wSVFeheHmHYM9cDjm6I2b8kCC8XW9QNpblb4aDmz4bdxjuWywyk11ND5UkaTO7HPBnA1YXLKUljBtKeHC9YthcZNqtt9MwBx2Z3SYLd0Tj1YipjAtBTW1pjo1aBRxLpuHlPb0CW8brDXFLFyIFGMUSfpFFxUhBDnrhFOfwA_0SjG2cD3Qral7RK5ShNgMrN7yoV6_c_18DiowaDQRaKIQiZ8uJVZvDBxPVyvRfoa13ijzxk0pFuujibnHFVqylgCcabudyYZVMYE7sxcW343K_wX6cG0bZ4VSLHubWHmkvaPVkFkbzv0zNcOFiHKimKPaBH8fAtvCqaXKRYjuRITT64Fw6VgvHs2xfdgIg1UmwSPShk4eNfanAPS4gVNGT3Av2Z7gf9vslKOF-ZN9Eo1wjud6eVO1fjjTKk1By5HAPX8fDX3auNt4pPm-MZp_DZ4H5gz9srNCvCA62mg1Sex7EjuotjMWESxZsozPzcDmcq-fVuFnCY6JIqlf8PfcUtkcdZeA_489CRddlLnDvVGXBehFS2?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8RdU6BoAjH9eWbsP3de4dOcORgybCYNFSAExzJa_0hpYScl_cDcv2fFtpBPkBJgkZWwH7-pI0p0AbtnHjayPgBVNH2i7rO8-_85-sXFAIXszeB0NU_1O5I4tgG_gF_7tiRI_ijJq-_L9Gl-i97Xk34_DCPs2MWNCP8wn1L_SGsfQ,,&b64e=1&sign=9f277c7aeb572ef15c2ffa982034167b&keyno=1'
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '290',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
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
                            url: 'https://avatars.mds.yandex.net/get-marketpic/163651/market_3ckrTop2mTduBU_DPJ8faw/orig',
                            width: 900,
                            height: 594
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/163651/market_3ckrTop2mTduBU_DPJ8faw/orig',
                        width: 900,
                        height: 594
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/163651/market_3ckrTop2mTduBU_DPJ8faw/190x250',
                            width: 190,
                            height: 125
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '221-1111'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '9:00',
                                workingHoursTill: '21:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.620097',
                            latitude: '55.687893'
                        },
                        pointId: '48755',
                        pointName: 'OLDI на Нагорном (м. Нагатинская)',
                        pointType: 'MIXED',
                        localityName: 'Москва',
                        thoroughfareName: 'Нагорный пр-д',
                        premiseNumber: '3',
                        shopId: 12138,
                        block: '3'
                    },
                    warranty: 0,
                    description: 'Размер экрана по диагонали: 43\' (109.2 см',
                    outletCount: 11,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/fblPxhEofKpjoUNfB2ORnQ?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=b22-xX14eUpIqpi6-08JVwNyG-cj4geFyl9LPhXYm7PzUPge2w477BVRnrDGUQxomEpG-QcTKuIH_eGwvLJcxQoDe70Bnn44TRiaIoFKyWirJRJN9oQGRvQxjouBsI_z&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZGyHez37GLfcz0J8eu7H3XoH-P5NRrxIaEzIUwK1dYhZw',
                    wareMd5: 'XvNFVh5GJndQHOkeizooOQ',
                    modelId: 1724547969,
                    name: 'Телевизор lg 43\' 43lj510v черный full hd/50hz/dvb-t2/dvb-c/dvb-s2/usb rus',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wxhuHBR_1iLG2wqyw-ng2V0gE-poUYNYOTBZzgYjDnNdJ2DAb3_udpIzqzLBdHt0f_A6DtHqXeSUTslD4vUhLKivxkloK4gLlxaJ0aDqZMo0bICPMhXEQpVEKRv5iKkCdoIpzOESVEB1iERU3INhu69op-DA_Ojp7D7LSLODqS4wL9LbnrtFtS8iDkQGfC_5NL3396JVW2UbNCEEGw2jb3mchCxtNdz3K3iwhLR6wAYGyoCpqRLHQEvG798nNTJ9NGqaDl2UJYKb_IK8FRYOIEo6AWO0d1wrkR8BnXXmB4r2zvrkZYiJpMbNOyEdTt38hYjY9X-zTXcQsqcbqRsMQJP8dwySvn0z051pQH0dgof9MFd1pQcPTFdZq37pBgZMPmCTT0gxq2q8sYD2g5gh1H9UKW9oM2DTDRoTIpVja-InYzn8dSRHcshCoH5VQXJTXoGExeLmKBOjSTB4jKgjz4w8mXssS_0J7Ww14lfXDI22IL4gIAaSb4KhfJyPKsG9HriOlSgEmEMnQpo1V6Lf6AODGSpH2O8cIF6DlD58lA0jrGgdPS_rZ_y2l_XM7DgXPVHAnQ-NzkkmMgy9yIBHOMPxQjcPFvhQFr3hmNNel7LrwXqvlCy0lHHUSU8WiICC4FKEO_LWHNMrH9MRMhhsmws7J6eqAamfBH_350yj3nGD-FelEoDFGd-?data=QVyKqSPyGQwNvdoowNEPjSNaWYmenTmmns-UjXII3gs5XsbsFVUzlE_vGxIP4RIU4jiFWKCkeKDE3JPeAom3qwD7xaEYUiWEEHaIOho5Put0o2ZNEZMqupkfFw7xYtFM3Asx9wAh8m35GFtYBnUrYs2eK4DC_XA848d75bh9iadKXWDIywXMn1fufNk3Ff-yMU-maBse2bng4upaFK6huaKz9rQINQMSzw9ox4rZTcK3eeO59WrFsoBfmMwdGnpPxu4HZ4fHpgnNp2UJcZLQ1CYRblXYYVp54UerEURLCmhHsn_gL_3Ut8iyqqPYuRMIPvEvbVgSiro,&b64e=1&sign=4fba781ba07ebe920834d2d74f08ba03&keyno=1',
                    price: {
                        value: '23608',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 108546,
                        name: 'ТехноГид',
                        shopName: 'ТехноГид',
                        url: 'tech-guide.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 2725,
                        regionId: 213,
                        createdAt: '2012-06-18',
                        returnDeliveryAddress: 'Москва, Сущевский Вал, дом 3/5а, Вход с торца здания (Вывеска \'Стоматология\'), 127018'
                    },
                    phone: {
                        number: '+7 495 212-92-68',
                        sanitizedNumber: '+74952129268',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wxn5uJS0lGMqV5Ve7NaJi3_DLqn1al4xofOT5dS66b69bLK58Qg0uT_0rOo29kvm4kWJ-40xgC8HxjLkLcFjPcRiHwLDZ2Yx2kkh3EDrc4CNAdvqyZu_1fjpjzOx1vDOiobxWCdQWT_UZL5RYAqYwwKyN1jjVeVX1CfTSah2O9R-aX07pCFe-SwmP0rpvSyeGrSK3QBB6XFuOB1H6YF7eZtrPz9qimR8sd1weH7HRhKFiNzbHpzvHYIRCzSlDzC-uWdWR-XJW2BdZBP-tpWSMCYa4y2mrOsehIBGXdvATklHCR4DmFQSn-P8XWNSDzjLwcVNpXov9UiR5HpFJCGaMRa7QzfgbGkehbzfz2L07QcRd836iexQEF3YJRkP--YX1rXkYnwh80QUHafqyB6lJrd3t-cx7lABnIseNXL6DcTL2fGM5F68obFZdMFlb9CJwUM1e-kufQdVgdccb6JNedXjtru-kl5k8RKmei4RKKvrkRAcod-JwCrA9Biwrbase0GFmb0AmSOOWpiHz5vSCRqTDC7PPwUMw1kOzAm0Ao11We4AAnvYQlrYkTovuSNSDxzXFLhgXF9lj-0lEfBYZmZwGBWkgB4QhgKYVd0Y_3dN9Sym0QqLFg79PATchL8nbebJRU10GlfIg1NPVpxRk0GYUp52YO9ugE64h4FpWYLS9N67K_Ylh8F?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9GHAqkbGlKdo1N1oD-n36suxYTX36P5wjQxK8Qq_t0Dy8s1SVZTOZhs7YYSoMNcLGJ8A5iowwarKghp4wmj2FMh0Op34xDMyRh-Cve2LapPpBOQT1oUMrCxiiLpt8m7V-b6nIsrh4oaDXi7g6_EUHkXVYYbTz0vQZMCGr_E52Otg,,&b64e=1&sign=826804ce45c8b5df53b01c3dc6dd65f2&keyno=1'
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '300',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 2,
                                orderBefore: '17',
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
                            url: 'https://avatars.mds.yandex.net/get-marketpic/202108/market_kqLSBLdtNn0yXjnnB6N_ZQ/orig',
                            width: 250,
                            height: 250
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/202108/market_kqLSBLdtNn0yXjnnB6N_ZQ/orig',
                        width: 250,
                        height: 250
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/202108/market_kqLSBLdtNn0yXjnnB6N_ZQ/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '212-9268'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '11:00',
                                workingHoursTill: '20:00'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '7',
                                workingHoursFrom: '11:00',
                                workingHoursTill: '19:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.59155212',
                            latitude: '55.79263829'
                        },
                        pointId: '335798',
                        pointName: 'ТехноГид | Сущевский вал',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Сущевский вал',
                        premiseNumber: '3/5а',
                        shopId: 108546
                    },
                    warranty: 1,
                    outletCount: 1,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/XvNFVh5GJndQHOkeizooOQ?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtUxkub6yi06kpeomWDGWtORKG35j6r_z1WdpXtF2KOLkk--DOSOKP-FpGWce9vWRuFY7b7VC5az3g5N4OOIrc5b8nHPOFzT5c4UQD_1L1Xz6k2_tKHDYHIx&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZHta7MFbcmVMUXN-TCoiZGyU_B8339JW3ooSZaRfnPL6Q',
                    wareMd5: 'JaNFTic_jEm_SijmYT_XhA',
                    modelId: 1724547969,
                    name: 'LED телевизор LG 43LJ510V',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wwt7VaHxIUrIWgu1de8VyPauUKGk0iMRlgceGE8R0aVDmmVh0FOZhrwY858lxuptaEmV5rtJEyzaSFOPBc1BS_FgvfMKZe3KIsi10_GnZ1Hhv-5Asp8vswdTLRvE0LnQ-urwDOHYVD2wntU8vjk-Fyescosuulcsjw4qC3Hdcv1hr9ScNrl2JlCNDoohD8aJyG-tjqYoRlNQEzE_AWBo3f1scBGT4Fo1MjlfHdIJDfhNYEE2IxPYsyhOJp-tf-43FOjcQMD2sj_JUsOUbXohHgwPgBQuLshq5N6EwxX9u8pDDlstiVh2TmNj6j3pOrR9EVVavOOd4vcWzP6KY4FVVZezJdTVXFEe_AKdXL1-8dYI5abD_L1wLJ9csuZtPQ_h0ImrnMHf66D9un7ajb8PiHZh9Cxu1aiEWhfOdChxVe73K3Z-Npx80NDeaRDtLNemuatykD4uLDbVkBQBiWIDMThWOJM1kiR1ZTs3cC5XolJWpNNIVqJjT759M1WTG1ZiKDO1ca2mD0ns_nF5_SnwhOIBInsmJM7Ywrmfl2S_BkTr5JJjEUEqQ3MqT5rRUYF0bp70hBaPSQHLEohzs6P9kLpBXovURcJWCC4dz88pYtCI6Pn3DvNfRfDki3ZQ-eGL272amGcfJrZI_JvKy6qWR6LW8ALLlc4gyBYShj_7qXrEy-RIpWOmTjL?data=QVyKqSPyGQwwaFPWqjjgNkQI7rsW99qTHcuWn_7rkzvJ_mfSLTmOD1fTDhIHAiRaQvsYUNVQ6smW9BEGVZdHzwF2betKTeZu5MjZ6IwjHJV3whwgtLQi07gQRy-BJrJb5RwvubSMgmwi1EkW1gYCOEmmr7QeArGwe_fYRHRC9UnkZy7svXRbP2auTY_3DAnif1XzJMvDv5mDPwR_DHiu4bFO-2Bs4U4MJ9Q57yY1yiA14nbZSHCCG-7IkeDCcXjIN7hvka_PY1DnrDAnb9vjySXaCsMK7ho1jTpVGrgTZdTBTOH2KtEocRp-5VoVjPxASFtZVO8AgeUUPCDRggOp9V6Y9MkfnR-6H_dswLJhrevO6cpfXzuTVMezvr0dsIKO5gzNoLcGTNpwR9sDVAqC7D933sptVLlL&b64e=1&sign=d61abab901e5fe69b5f151a4ec9fe202&keyno=1',
                    price: {
                        value: '24000',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 23824,
                        name: 'Техника Быта',
                        shopName: 'Техника Быта',
                        url: 'tehnikabit.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 1661,
                        regionId: 213,
                        createdAt: '2009-06-02',
                        returnDeliveryAddress: 'Москва, Огородный проезд, дом 20, строение 1, 127322'
                    },
                    phone: {
                        number: '+7 (495) 6467011',
                        sanitizedNumber: '+74956467011',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wweBE6bzX96w-YORYuSXmbExk2Kut8yc8O54bGAHnj6QjwYznvtzTvsIdhyTb0IiowixRbHyzSLGh3lIRghHF4EX5GGZC4QEzZp1lReGD7t6_SG8gGDl9a_4ICmJPd981was6612XEpX7wNcqScOqAWGlQPy0cO0UTm5rtMmoPH0skUfK6piOJs6to6rGYiruMHof0WrzkD2EPngg3NLmJPglqMPJMiD2oAR-X3EbmMl6jJ7GsaR7IQDv2OTLEzWefkGJCNvI3IUo63o3mVkHOq0MQbjt3prSg6ziTNGm-FVLQVrvPn6DK3mb8rQylmIEQCk7zM1wGv2FP5gLUykT_IS6tWXO6cBhWUdpbQUbhFnBKWCoJaO6UPPnfsQPbZfmOBSObEpBsOt8RbFv4CNsDJ96jD6VHTjog2BvZI4t24trPDqPDbfRQ_Rbzs8Y9VtFImUZY3EfKp83tbr32fEAtuFw8XqFbao14hCViINhvkJ-SnW-ol_EYHmMXm5PwWfbuiQFtJ9LSzVtwHAGVchkgCNgRtQqezwdGkIB8FTtKnC6cF3I-vZrSBIb18OHesxYLU3SkDOwO9AEIsW-4S9aa9kUI1-dvyT7ZwDX8PbUGWgqkA6ExWz3S0CCNUmqJnTjCaHAUdekEq2REexNeltZtA4WQCAJi3F6pLTqDS1H8zdkRjAbuO9wAb?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-obGyv46m8GhRlWpHsoXz18Ok25v1lBb891LpuLB4DVXZ64ezce4Gj6D9PjUokyyT2Um-TQq2-FLkYGbhpt0eFYOPHeLUM-cGk5ydg5rXbHzL0kLmzj_-BJIdCm0ELvHwAOjODReOIdR_M7UAHziUPm9OkzAchtzQzTYYsjMRHHg,,&b64e=1&sign=232bada44d948f7c5ec7ee4f6678a67b&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '600',
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '600',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 4,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 600 руб.',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_q7yYIghjG3MtxiiLtZyuBg/orig',
                            width: 300,
                            height: 199
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_q7yYIghjG3MtxiiLtZyuBg/orig',
                        width: 300,
                        height: 199
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_q7yYIghjG3MtxiiLtZyuBg/190x250',
                            width: 190,
                            height: 126
                        }
                    ],
                    warranty: 0,
                    description: 'Дисплей Размер экрана: 43\' (109 см) Формат экрана: 16:9 Разрешение экрана: 1920х1080 Частота обновлений: 50 Гц Поддержка 3D: нет Звук Мощность звука 10 Вт (2х5 Вт) Акустическая система два динамика Объемное звучание есть Декодеры аудио Dolby Digital Функции Количество независимых TV-тюнеров 2 Датчик освещенности есть Мультимедиа Поддерживаемые форматы: MP3, WMA, MPEG4, HEVC (H.265), DivX, MKV, JPEG Разъемы Входы AV, компонентный, HDMI x2, USB Выходы оптический Версия интерфейса HDMI HDMI 1.4 Поддержка Wi-Fi',
                    outletCount: 0,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/JaNFTic_jEm_SijmYT_XhA?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtWw9GElp7Tx8O-4Bq39pkoxk2G-F2Ylwt1kEgNdGN0CXVzuWC37tKvbiVLUyXgEAyXWEqb2HpnwDyEXMdQ3t2vn53ve-94i4Ix3_VDF1PUv212J4st8kkyj&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZH_iAGcOGCEDYBg8CqklrWRH1AY0Neh6DuE9J94qEUGBuGKhHzK_Xt3dLI132xGxzqAjC6lF_QDX_3NViRL3nfqM4HGbfDByJUw7l1p8LhEwChbL47mKfayZfYoboAppiC1qfcoP6CRhUaZtrl0BXz5PUy4_3TOXVWiIY0zqKTQXY_sxUUCYnXiAMOsLrOZY4DOcF2_wd_dwD_1nquHcUMQxUwuyHPJhNWICtZ3B1mYl9Qmsbo9vDa2nrz4ahjMLxMesy-NLoClD8UVWZHkNCxBT2B16K8H39s',
                    wareMd5: 'iDNJ32UKr8WRLNAO01aPbQ',
                    modelId: 1724547969,
                    name: 'Телевизор жк LG 43LJ510V',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wyQHgsZC2tL6tZ3MGkrQ8-_Ej1jUQNMQ41weTb_TU3moBsASB6bRchoX7erHnvqZnpmLPrdxl1JZ7n2Fv8zHgA4GazfQf-Fp72M8GJRfK2GRvNFB7zgxZggO10mGFV1hIm3EeAN5CmPAWJkO2jNQwQgk1GvmIENOuHnSo8Z7nt_se1r7f6M3bMflsC5JUCw78ctELDWhHr6pqdvTE-idu-xcuU_8_Nx8Kou7JocWBum7jZRS3i2gdcZ3kOB5GCktY9iN37AQdNAuj91bBxosWxqqCO4CS55UfVQjkI6dsOr9bHC_jYDKGh2cSTIp5LULcd6O1aEQTmJSY5P1P3CCwGfTlNzEkD77IPWhU_j3Jfh32NX3zg91YChepsxz5nV8DJYWPsIIBen7X7o5Q7BFRelqvVlygETD15Y1fIbr7gBPQ4NRScBGZDuBPVXIHnGmsq-G-De-3hKo8zKBUbxsbT3o1V78jhHHO3s3HFP8unn8YB3QViMZ6n_W94X4oEm8hS6GBiFJA4eUBzwmgED_U9N8fL-bb2vzrXCto0s-vFfi8fgHzd0q02ezT7bkE_g-UgoXKoBTTYH47nc4l8Ot5G6--U0DVflKwsoEXZbyCmzsJp5U2iC5CONq40y876WjxM2IoAGWwypSi2GennOZhI4Kua_oU6LbKOMRkRrQHhXOgVevRYrLUpu?data=QVyKqSPyGQwNvdoowNEPjR7nB49krRc-STZ78g0K9BkJUTo0fHx8b8ZPDzyzZ2MzBfLiPSvhYSWBdKQ-qFK03cZUyfmXXJsOrT5n8IpetSe7EWNxMGpuu8v9mFcMaQqQBAQRE9daXq9l0DOchHRW-Qu5cLNKBy_pJSGtF78ucmtUGdnCwD7T7q4F0N3QMHmghoscfILlSfS0Ue_ctqanwBh06lAb1e8hGe208UlUF-6Pccc1TzfFL__xw97tFdXKd4X0YcCqokGW-74rcnLc6a6vlo-rtwZ9QVa0jDZMPNQEHSkjRbZTBoBuH2SK5IywjHXrt1dOiXJEcvJqeBsSOoDyNH-Z7IM2iF1leMW7h3_-rQY0Q0Edj9AZeW8T-DlAtI7W4fGcA1w-kiyioo1KysRWzSfUwljcVQQ59-54q2OyTZNLUXmqpjJ6HkAh8Na_&b64e=1&sign=a0c32594681637c9aae27ecb50562d21&keyno=1',
                    price: {
                        value: '21717',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 166874,
                        name: 'Media Park',
                        shopName: 'Media Park',
                        url: 'www.mtpark.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 896,
                        regionId: 213,
                        createdAt: '2013-07-03',
                        returnDeliveryAddress: 'Москва, Багратионовский проезд, дом 5, строение 19, 4 этаж., 121087'
                    },
                    phone: {
                        number: '+7 495 268-01-20',
                        sanitizedNumber: '+74952680120',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wxZhXbfttKghnGsW-PnPvqp0Rd2nKsNmB4ZNfAO0ORhiWeTruuq82OzsJ20rHofIgAsW4FlGpGQSThqW_TIGbOYmsElo0Ui7Xzgf5ur8Ufx8g4JAPPKx0qW-LYvyR2_RAQ0EFk8zkGEXF3DWNQyECsIrigO2mQpl8RYu5ig4BH_s6huDEC0jfcA-Ja9rbOsOHuqX1ZVhJHDa-FT77Q8KSYewxM14A02wU4wSe_LJCpgL5OZd59mF_tqNjhHvSKqwRYi_OrXe0y8Ku-a_oAOYVXTPSIQHObTP0TDTLRdFHQtSDn72MNM-178WYTRW95UMfDQ7uB8SYnomarrONnQ1PHTMrTxwJvSr6NE5STtxOViKPqlQkZxvTLxPMP_qBDQ7vSvdQgb_h5D2acEQ7EmLMUl4jct0Ki_WhPwWmJvnC6Xu0z9CXoDvy_ae81VawGiAPmdjbX-FTqZpbmDzDRnVg-M1YyzJ6kSI4TSk5zA3XiD3urpwvIUYoOcJQWLKf8IrttKFNFlSbvDgcsMHswKMBm3CvI0bZUrnXgT1rSYDaW-eE17Wop86G81gy8bZYJqYNDCP0QC8gWnKB0QFi3IPhwJ6lVmW52QInml3lkDwyiFtCI2RsZTJSxzF_GK_taUoeuwpNuZJgWmlD4zEREbmrLc5Hv7zeLtln64X-QaxsvhK5p_JTTwIMez?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_4WlesibEOkaovs1g2ExCNz8X7xfZLagk_9MuZXPzdVMCkJUP5GSeDi1xEecSVx_drWcfT7Lw-9j4ZPWiB9cC-VMWvVVF8Zxu66Ms4MnVHqUTS52QYOT8dp5J1aQ6Y5OhkVYBrbTpwfN-3w24VMwTV_3R2uHpuJUh6F_-fkL0Qmg,,&b64e=1&sign=927df69668ff210d724bcd16893e3682&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '650',
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '650',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 4,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 650 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/175578/market_RiOA0l90Q3K_esb6h09ZCw/orig',
                            width: 600,
                            height: 600
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175578/market_RiOA0l90Q3K_esb6h09ZCw/orig',
                        width: 600,
                        height: 600
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/175578/market_RiOA0l90Q3K_esb6h09ZCw/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '755-4543'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '11:00',
                                workingHoursTill: '19:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.50615662',
                            latitude: '55.74339047'
                        },
                        pointId: '417487',
                        pointName: 'Media Park',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Багратионовский проезд',
                        premiseNumber: '7',
                        shopId: 166874,
                        building: '33'
                    },
                    warranty: 1,
                    description: 'Основные характеристики Тип ЖК-телевизор Диагональ 43\' (109 см) Формат экрана 16:9 Разрешение 1920x1080 Разрешение HD 1080p Full HD Светодиодная (LED) подсветка есть, Direct LED Стереозвук есть Индекс частоты обновления 50 Гц Год создания модели 2017 Изображение Прогрессивная развертка есть Прием сигнала Поддержка стереозвука NICAM есть Поддержка DVB-T DVB-T MPEG4 Поддержка DVB-T2 есть Поддержка DVB-C DVB-C MPEG4 Поддержка DVB-S есть Поддержка DVB-S2 есть Звук Мощность звука 10 Вт (2х5 Вт) Акустическая',
                    outletCount: 1,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/iDNJ32UKr8WRLNAO01aPbQ?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtUfW9zxXpn6C82jnen71J5pOTK8mMXwlPXwX59ewOAPKuTseFoHYBmr02lY1xT7Ksag2StdVgj3Laih4cVyAw_j9JH6xN28WQrJcQPxmJVIisGZaNA4g87y&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZHad9xRVeRyOYlNowJuanXXnQrXtqmVE0XiLUj8WtE_LQ',
                    wareMd5: '0invPDJF73wtcLwBQjvm6A',
                    modelId: 1724547969,
                    name: 'Телевизор LG 43LJ510V',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mWm0PG16RMgg5WJp07po86GKO38Ruf1jmODnE8vDtuiRDV0g40Rq4y7CJN-8vy9DfnGpEwv4psWEM69zJb198cv22dbX6V7KRzFMd5ryEfpw6SRT6QRASZmuY6RTizhSpoWUA9T1vJd4VeOPxBsySHBEJhANRScEZqoJaJOVVAelMER-2yHgGUN4B-iAiu17jR_TkhFN4udAiLm7ZQIsGjQVm4psgB69Nu18Ety6loG_aR5UpkR_WtZX9fkszeHiwAjw8SCpXQ9iEmUOXypW1o7gJkBDXhWPHTMp27N8Ouy5AsJ53g4v649kQAPXBUa6JB7bZ19HaZ9Q4suETMe0b0swsQDDRQFn0K56LanFhDPEGK_pc_nDiq07PpQm3vyg5017nMp_ZXEwakuuf2E-O6pc07od6z28vNEPnrwQhY3ByfU_m6rdW46UHzyhxxGuqPobCfq-Vta1PAMgmXw3YK2T-6Tks-Pj2yPtahK5SQ3i6lSDQ0BUjPvr6uXSr1lPA1U_siDk0pkoTbPl2rqwLo9V3YiDO4Nx73PLFQ80-4mZL-twf9wQlT7ubs_O-7U9ZEHcA3Q6D1Jz2wS0mbwuCZHNWDRG1c6sEo_ouOTSZ4UCzLwOjvrXUvBC4bQhOrz5GrIfXiyUmcUjGYQcoaGcxqyWoLMnkS9jrm9rbPpJ8QDYzVzXTxPCM5QSh5pKNd7MWhZb8mgJZJX3QAcIpCxkuIXUOrP4eGcGGCKEyDENalEYUdypZojfYwjCT3dPuOYBCtitR1OVB_2KOYOz6Bz8tAcI0gP3fgWbsdxsUBGk_pSNX0s0Gmhc0gkezGPTexfY0lYxezj6b-dq89IxYSiOxsKpbu8voMB08g,,?data=QVyKqSPyGQwNvdoowNEPje0PAcg_xXLaHk6lD6oXnoLd5D5BFYSBcIJIRfCd-LwMULH3oCRpcaa64kNNVpij0bxJc8533eS0dRmJOD9QU8U3MNqNXPBB98IYL4VmNmOIHwfGDGoLvt_CB0oBuap35jgDsU5WCiL_MfQtXtyUzgNHRKt7edAKzikmg44eK1hnLMjtU-0mSPnq5Dw-Ckg2Dg93fYUjk-ccpF5H49kCY4imMNnEoBZHjYyeaNXdMQZ0IgG0wASzvKrZFlTpyC6tNhOHeVvHB9GGpkP1p0g13W-Lt4LmfUjs_VtyxmulalB2&b64e=1&sign=0ddfe97fef8865fab976b40ae8825dce&keyno=1',
                    price: {
                        value: '25246',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 932,
                        name: 'technohit.ru',
                        shopName: 'technohit.ru',
                        url: 'www.technohit.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 1237,
                        regionId: 213,
                        createdAt: '2004-05-24',
                        returnDeliveryAddress: 'Москва, Окружной проезд владение. 34АС20, дом 34АС20, 105275'
                    },
                    phone: {
                        number: '+7(495) 5404253',
                        sanitizedNumber: '+74955404253',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mWm0PG16RMgg5WJp07po86GKO38Ruf1jmODnE8vDtuiRDV0g40Rq4y7CJN-8vy9DfnGpEwv4psWEM69zJb198cv22dbX6V7KRzFMd5ryEfpw6SRT6QRASZmuY6RTizhSpoWUA9T1vJd4VeOPxBsySHBEJhANRScEZqoJaJOVVAelMER-2yHgGUOjsrgZDHFukeIrJXdnqHW_ZckbsJUCzF-v30EjmOJnMW7-e2czPhphty12NFsgQsTYq01E-Nb7JqjZ8_mAPyTgqDjwxi3KF7YoJkIzTssLxC_3rWvl3WZAWJGTpzy4UmEqAzEcvYspwvMZJr6SQm35-d2voJUEgtCdRJsJNegWI65H3pU8_EQS_1JpzqW_mj8J2TnDRkTEKaqQ20mj2IGKjrqMlPBgBX-0jnS_ZDrpzit5aemCbfTxJ8rH1N8aJxI3TSALeJuvqOLW1FVRl5UzozbOg7k-Nn4jhKu40D2Eob5scEhPm-XD3xTN-_8Wok9NIfo-pLw7j_bvPEYFnEGB9WeXSGZJrF9FmWm3QQi1dWKo9IhlZbRuvBOX295zRqE-CuCcK6B-YAfV0G0BJtSzhKDbESd7SAWSfoGi-spwLy9q59Ve0_HhgG3YiFQhrhTFVtRkeRPnH8oJIkP39Ovdt_65_ZL628k1WYJnWHmhYnB01iX1K4GNqW32nb36SfGi-7JLBABScL3Y29SLhvqNklVMhrbMAxvw_cRnm5h8ZNXchjBGLTGyqwohHr5wHdeqxkvled_wy0MtJOabgT_o19aovN-sP0Jb-3dr9vUC_lzVjB4Qxte3khSo-hFjCNvlvUnnoDDi-6LtD7gZtRF1kIfIewKAv7edEGjEYzDhKA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9AXyT0rvguikoDzrZ4X1oy8qLBjXDKpwaUz9d6lQNOHCA3rtoPMbrXR7u2myFCTOR2WiWXf7N07KX9KCK1VMgLm8V3la6wCtN4Ls1NvnaXkGqUiZfGeOklofqj8peTr9i9TeVmuAnOiLtgTeqs1QA48jORkAt8ijJ0X7PB-w5_LQ,,&b64e=1&sign=98d6f547ec8f5bc0a0f1da2fd6c32694&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '600',
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '600',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 2,
                                orderBefore: '20',
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 600 руб.',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/203633/market_d5BvvxdLwmYi1MF0x3jmeg/orig',
                            width: 200,
                            height: 200
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/203633/market_d5BvvxdLwmYi1MF0x3jmeg/orig',
                        width: 200,
                        height: 200
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/203633/market_d5BvvxdLwmYi1MF0x3jmeg/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    warranty: 1,
                    description: 'Телевизор LG 43LJ510V ЖК-телевизор, 1080p Full HD, диагональ 43\' (109 см), HDMI x2, USB, DVB-T2, тип подсветки: Direct LED, 2 TV-тюнера Основные характеристики Тип: ЖК-телевизор Диагональ: 43\' (109 см) Формат экрана: 16:9 Разрешение: 1920x1080 Разрешение HD: 1080p Full HD Светодиодная (LED) подсветка: есть, Direct LED Стереозвук: есть Индекс частоты обновления: 50 Гц Год создания модели: 2017 Изображение Прогрессивная развертка: есть Прием сигнала Поддержка стереозвука NICAM: есть Поддержка DVB-T: DVB-T',
                    outletCount: 0,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/0invPDJF73wtcLwBQjvm6A?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=b22-xX14eUqsxtIGQYiMcwLHlyMxiwZMMDs927wH86O-T7MxZxu_avkegoHVWAE3LJMjVaGxDLDhqZvT6geU5HWNtREw-Dl4Tib4ql0noaHmOiueueVJbVkP2CO8al9L&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZHwtGc04SWjJ5WI3r0BNUE8_kBn9zRT4017xJcSyFU98g',
                    wareMd5: 'Mkq114-69H3XqHZGc7tqMQ',
                    modelId: 1724547969,
                    name: 'Телевизор LG 43LJ510V',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZRRrA4uRtrCGHsdheCUJfycCHmxkikqyFDQRcxqWFhemAw9PbBWr-e0u1Q_RjLGz4Bsy6Hj4uxfiTrdMzNkQiSkZ-gNKOSQfthROUtMOSrOAabntuN7hDaEQqdM2foo6ZnnOnz6PGiPocKTYoUPRGSCw---MY_mwzh7wXNTzD2Dfavdc9e5foAa1FCPpYRAkcYbifzR0gCHbmxRmQ5p3hd0UbII5Qs77YqjodFRy_zbvb5CBdfEQcDCJTy6jp2NKre5_0cRx450fOp6wIOZJhAyIbY39l_IwYjKOyAQ5x41l87TGlZnDaenKqNGyeitLeiKNeZT--9bDIaR0RAh6Qn75utlV4u39LiPMRBvW4T21R47O3QS_ThqhriYD6SVQxiLgIdyTm2V8L0VKenk3OcbH1hRGaoSEgaqIGP4VN-h8JlphXJYVyx8-EKXHDCJTYYEW90cR4nQ4XuG-sn4R6_4_OqCE35vy1sAvgzkuoXnfYoMqwroqGJc3lH2B3TFuy37RDeXK5pdphvQR5akFPAW597VLYEYwXIY71dEqQJb1Sc8Jn0Lg1NQScPSrrZb5QsEMt1CvMwCIdMyedGeEkVphNTbUVQTnIApo2DHOTLyogL5bPnzH4DnW2iLpqkB1sd7YQu8LSWt4HYyd6xqW_Ab-zfQ9rRQNvHBYenUAQh_6gSRzB3DgCZT7b-dKSe6ZhHiZfat5vNAqpuAV5rCClPFavmh0XAGSkiRzD3dcMZ5Wd2RVYfSy4OtyGCTHHaftiEPpTBrhDxpts7lpz3i8U4QEGvL4igwsWHfca1w0IM_tz1CbryPi18FQV-evgFpEgi64g_44FBT-feKr0RJOH2X7uhorvJXnUVFHA-ogpvJ?data=QVyKqSPyGQwNvdoowNEPjffWP94VrkTQ9U2e3-FfQRgG0PvEWo6dvg9SHh2JCmXOE4cs54mOtIFD683YUJBtbJ3go92VwVB_21x8LvP251jy_4vgoqStpDb55kKh1XMv6M0W4RFFgzNqyMIxrzTm3s8aobzajVqo7bsalT07gdubIRM_8DuueLY-pFfxeGO8KoraRciy6D6UHYEXbn9gZvlRy1s42w-gA0uah6yoxqO3371P6p6nudOT_cJSYCNeY91PFIN_97foR9-cE8gB_cmcn-rxo9cy5PK1e1CJbJvY_cA8lidK2uxAnGLIhuMDd5_1zzkrSLgfkpvRFJP5TJGdooRe5iJANAGx57cV4NAPedUgMvh4oDc8MiEx3gb49xZ5YfGN-taCWE8-Y_ZUdtW9aZ9VkpEpG7eZ3HhOozV-4wnnZfdGyJl-IFg1Pe_xmqTMa4yHgNhLOhNjgGE1WRUG5XI1Bb1hq-koqrfEP93ixEWg_GdGu14rxpgFO1lYbq8yeSwrWfipMiAIIbIlqfUeHyzjRcoJ&b64e=1&sign=1fbf61508cd24c72e1d858bf413a2d99&keyno=1',
                    price: {
                        value: '20950',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 6363,
                        name: 'BeCompact.RU',
                        shopName: 'BeCompact.RU',
                        url: 'www.becompact.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 6692,
                        regionId: 213,
                        createdAt: '2008-02-20',
                        returnDeliveryAddress: 'Москва, Орджоникидзе, дом 11, строение 44, 115419'
                    },
                    phone: {},
                    delivery: {
                        price: {
                            value: '500',
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '500',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 1,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 500 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/250421/market_t-3NmOdmiSHgac1HQ-KFPw/orig',
                            width: 795,
                            height: 518
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/245913/market_fW0ws2wWphZxnJ9MMst-Ew/orig',
                            width: 636,
                            height: 624
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/245913/market_5AliVoQeJ0Z0yiG_xrsVUA/orig',
                            width: 780,
                            height: 537
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/406938/market_ph79SmttxncBhIOkh6NPYg/orig',
                            width: 754,
                            height: 521
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/363663/market_yHTDnz30HSTWW1ke0TgQdQ/orig',
                            width: 626,
                            height: 621
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_lWtOh7zM-OCtI4Dx62_M6A/orig',
                            width: 966,
                            height: 217
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/236212/market_lSPnLy62z4thYmSDMepSKQ/orig',
                            width: 251,
                            height: 719
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/250421/market_t-3NmOdmiSHgac1HQ-KFPw/orig',
                        width: 795,
                        height: 518
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/250421/market_t-3NmOdmiSHgac1HQ-KFPw/190x250',
                            width: 190,
                            height: 123
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/245913/market_fW0ws2wWphZxnJ9MMst-Ew/190x250',
                            width: 190,
                            height: 186
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/245913/market_5AliVoQeJ0Z0yiG_xrsVUA/190x250',
                            width: 190,
                            height: 130
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/406938/market_ph79SmttxncBhIOkh6NPYg/190x250',
                            width: 190,
                            height: 131
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/363663/market_yHTDnz30HSTWW1ke0TgQdQ/190x250',
                            width: 190,
                            height: 188
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_lWtOh7zM-OCtI4Dx62_M6A/190x250',
                            width: 190,
                            height: 42
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/236212/market_lSPnLy62z4thYmSDMepSKQ/190x250',
                            width: 87,
                            height: 250
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '640-7742'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '9:30',
                                workingHoursTill: '20:40'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '7',
                                workingHoursFrom: '10:30',
                                workingHoursTill: '18:40'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.59771834',
                            latitude: '55.708704'
                        },
                        pointId: '49344',
                        pointName: 'BeCompact.Ru',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'улица Орджоникидзе',
                        premiseNumber: '11',
                        shopId: 6363,
                        building: '44'
                    },
                    warranty: 1,
                    description: '43\', LED, 1080p Full HD (1920x1080',
                    outletCount: 4,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/Mkq114-69H3XqHZGc7tqMQ?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtXf5BZFiH4qqq7Opvdn1NhTa-5usx3ycCrSeiWQFZ7BkJKJZfyGgOwJGxS-ABYlMSDPNqIr-1unyrW2B7nOedeAGil54RfqdBG94Wmgikf50yDgDPESbpGy&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZEpWvW-ZrulygB3CaNc3ybtY56xeX6UHrwlbWbCNtkNjpoEV3KUd7pT_c5vMtu0RGxwh1T0hO7Hp9QNin32eu5kCADXNEoU3PnZ5zq-uDeGV6AdWU_S_BxywtWf19lAi29rmKv8lEmUDXcyWtRcXrOXVvMzOBeQeqrCygoiXKueBOsJcN8_aaoGUTDPbUu0bjEy-_jjVT7ZPQlhRvjdbjDQ5lxz1P2KbYPqyo0ORcuczd2-Rkx8rdG3zV3VC3xPQ64YAaLX7IU5aRqEOsFkqZU4rJl3hWGHP50',
                    wareMd5: 'JylmeQuWvq9UXm2Y4eJbTw',
                    modelId: 1724547969,
                    name: 'Телевизор Lg 43LJ510V',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wxlqFpFWuiW6giPG_8tlibu5eSqTd72p1riM7c-yXJkAcX8CXCIjF5PBw3MMzh-vov5DXCf16xS98p1tUCqNMyAQC6bOdED91pp8DK1wUbVmHQdxT1QINEikN1aGykFX6rVH3Sy8oiOV2wLrma2wpavbYCbfMx7jMn-ztWLIUq-2r1uJdCFlvoBFDoxVs2WlleJfllD2wR7_XZUgIVuX-Nxd7YCO-8tLSFzpFK9dAu2UU6pVnh6ADSlf5pPtgbkYogeunpnQrv8LlOiH6iPIvTaaUsYCOd-847dYsqh8iZFV9IaN1_z2MP4ku1q2Jn9qr0vrmf7FtaHVDZsOx0eNBxWTuAXjuQJ7DWtrVpXBY9_V6WN2k2JBPcomwrKjW0A5MvfUZf6MTy9HnGhvQ907GhGTvsFIjx17h_H-XcsXZjWZJOKA9gnZ93jrdqx0oAB7LrSSBTpSrM5NICg4f20S1_37pao-QBBIGsFPX4d_HnMbSkTBJ5EiAn-AdMtb6YigYxWV9JVdf47SjYs-cdoN3l7DlVNvHV-JdcyWrY6oxGIS91_6Hf8FdGozr2uapUnGrV3RT6oJ54330jxMJonoNhtx2K3aRRQdgLxkPIzVDVG4hIYtJUgNnfw_7PbWsJBwL5OgI-b_t1QyOuMGOY7yhlEsGu9DNPcyp17THWFy3C3uTZa6e9qX8jG?data=QVyKqSPyGQwNvdoowNEPjcsSl6H1ugDZrOWWnOAm5dbDKxy5-m_CH28nKv54_5YE7ktnCgDUWla7myFPwzt_yavbVkJsVfS7TWzvekNGnfYqKOCbAt8Nrb8UE0hBgpVGZ7qDIEiZ4gZ6BBFWaD0y9cWMiZo8_rvlP1B1SRop-ToJqeMKjW1cLV2ZMyYCbVtkLo8pbNTiClI39_MZaUuot537KECDzxDl_lgsmto3rP3nCHzliOuBHMud-AwVjPmi&b64e=1&sign=459cf4c036ea8571c99d3685ea1ea3da&keyno=1',
                    price: {
                        value: '22650',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 321209,
                        name: 'Магазин Телевизоров',
                        shopName: 'Магазин Телевизоров',
                        url: 'Televizor.msk.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 248,
                        regionId: 213,
                        createdAt: '2015-11-16'
                    },
                    phone: {
                        number: '+7 (499) 397-81-82',
                        sanitizedNumber: '+74993978182',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wxer0Ya4YbljzS2Vrh-m8t13pw8xhJ3SbIbHDupeZL8MWZ0a2soiQJECZsVzc97Qnir1qZMRW5oVdG9bl_he16TubEsiIT3VMb2V552tmCyVe4kf0Mi_0h51WIlKocBZMQ4nR3uShKNZIWfE3AONZPq3CaQaxU5TNvQkW9CmAlfyOd75MFlHb3eiDGfvg_HVskHNoK8VEoSp2wgh6_I-rvpUb8zWpX6uX8zGN8TSWLErvkhdDv0eVCUfAs085x_VoCLxOgHwIDlHPakldddpt8lc1K_ozhR683PjKtEI5v1TrjdmP_wIUNVIoqWOhUSC_HOUZV11cCXX06ENjEXCZO-tInPsW1su0m-KCD1iix4vW3scZ4r6m-re3Zu3EpFvXBktwusU0SF4V8WjAF8RdjJvBOw041ELB3uRkiWzJw43BZbQkbnlRYm-4J3PZ3LubROFWhzOwpnd2VZfQeIOI8omlfEtCjCKhJ_FH2PbBDr9urpffv0GX_AV_8etKaFdi_wbYhQM4oRme_qj1pClqMriXE6XmTam2mCKsmE7pi3LSkRmKCqt7OIMR6-A8rqrX_rcrxgZDXto9D3H--4BN0kVd7ncRZXMFNCHH3SDAHz45-fDr88Ac_fpUb0i_TvgBt9nJbhhjdu0HHnogluZxqmMKqj9bNCv_eVQwJrQeCteQz5D7DbwT33?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9iGEnwPo3chyCy_sEbM_X5tfhRzqsE_K8CYmfQuN_Vj59eyZrDQo9K0lstUDaxuO93cBAjuCZycBAR0i0rAWNyKzfDrFIeguLR6YAuSOKkge1tHd1lBlpe40eeLwdxp9wnRhuKC82TYlHWOEqMLHKUImy_GVSjANLc6DhjZywkBQ,,&b64e=1&sign=d82adc49282484844ba0198c5fe6a836&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '700',
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '700',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 1,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 700 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/208477/market_eqFFPfnq0dfKV_L9xT05BA/orig',
                            width: 400,
                            height: 300
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/208477/market_eqFFPfnq0dfKV_L9xT05BA/orig',
                        width: 400,
                        height: 300
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/208477/market_eqFFPfnq0dfKV_L9xT05BA/190x250',
                            width: 190,
                            height: 142
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '499',
                            number: '397-8182'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '10:30',
                                workingHoursTill: '19:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.50324726',
                            latitude: '55.74086569'
                        },
                        pointId: '530313',
                        pointName: 'MSK Point',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Барклая',
                        premiseNumber: '8',
                        shopId: 321209
                    },
                    warranty: 1,
                    description: 'Full HD (1920x1080) разрешение, черный цвет, модель 2017г.',
                    outletCount: 1,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/JylmeQuWvq9UXm2Y4eJbTw?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtXYp-dx2CVTGFGwbsNg_tkxtex03Bpr-kokL3JITPN0mPO2sYEZ2Mt8E0mUtU7T_ncaH9uXcrG-gKkrZ3_O4kxIapU0mmBF4N1VtYoMPGI5kzA9KArnyQJ0&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZFzSQz74NTy64TOFeKr-jRdnzT2CmCoOZaqWZ_EyOSMeO9CCoqikYFgg3lVIdB3o1YX0wUe6gGRO9KQP7svUBR8r1h_PprVOEsPoE4LKq72asf-9HgCUE8dauhNgNABTQSqeOKPsIw8auzcjvnzjusQyP3PZjVoGKZVMU8L04tR9Ng-O6akj-aAH9hJsWGv8hyMOmTe1XLNyxOYRX0fk1SJdsAJwd99USBf6nAX_ViOU0bdA2fGWGa7MUj-VNIglJSYd8Zt_csx5zZAZee4I9U5gE_tdxkljGw',
                    wareMd5: '4nHj2f7SGNaGDgLb2mowaw',
                    modelId: 1724547969,
                    name: 'Телевизор 43\' LG 43LJ510V (Full HD 1920x1080, USB, HDMI) черный',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wyq7F7U4AwXBumbr5JzRiwlvz4Y9mAIjJrbxpIoioy79fjg-ZA4OjRbd8A0h3yCwDczgoeHtIfGYTEgbOUdarCk1qmXFJNfnko8YuRhM4a49RIu0SRXuRk3_JBcC86YYnRzcqHdlbw1dTbrfdKahxU6OQS3iiZ9F8sCja5nciWYeKHU9ub85kStz4FwMHdmV1tM4JId22QG28tqcJoVHO6Ldc2W9NtdCNWl03Xc9YQDYcgXkClnc2j_C7qRQZdhHwJ04vBMQq1yO6_vWKbu9wyydMgMGy2qY8xbgSoqcvgEeht4aiiDm1hhCeaU6T7ffXC8IS6rVS6MRpsVnhrBkOuCc0bsnsdvs2C_QOakobJeaKcxMcsyzTtcNB_G36CNA4IwKb9qTsDOcvbbpfU0BNTIXz7kSjfB1lFDX4zZLj6xAOciYI5TUvJ_RmHNnOobTcmxWLpldLKTWNtmU_i2tknbOU7BlBg1aAYFWuuh8n9p86dy8FgcfRQF57mi8ewWAy-zZrK6O7NHUt2qkVpn2Qec8hd-5h1cWePSCVCNmbD_bkZ9WnBX_XhXfeii_8iiX6rvNm_D5WQ0KWN8216mDRSAb1Ikgg7x-9HZZMvxMrFfiRHB7eXxFxUu2i8OKSWlYc33oFNJS3AUHaF_hD47q0uop9b-nt17BfNlxIQcknWMZ0YLiGB0cPu6?data=QVyKqSPyGQwNvdoowNEPjaaZR_STU3giDJ6oHNwOTagb7FrtVFunoDKJYeF65GBzcSbTabFUqxNONFKQVsfpK9-FA8AvH4TJNrlHVZuqrSbCS_5yRC4C20RQN5fIsgIJsd9N_edXBdRAWLK7NNP2n4ePW6BsqvukwkuxrPVHMW9ygoI8jQsPoEw9a7zV3oTATRNxHPauybFdkXkiNLREApagDIDpdI0sxRnI2ncEALELh1xuAel_jKfUxmtTn0aAsia4WxaFkolVJOZ49d0h0-m15Db-5Xkjm96zdk9D8q2_DsO0rX-CZTbxHalXeh8yT02gMojLNoe0bSCvHCiNAYT6W3tmFJqg&b64e=1&sign=8993cbfa6325facd7bfa3346bb6bb049&keyno=1',
                    price: {
                        value: '23290',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 3534,
                        name: 'Flash Computers',
                        shopName: 'Flash Computers',
                        url: 'flashcom.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 1286,
                        regionId: 213,
                        createdAt: '2006-08-24',
                        returnDeliveryAddress: 'Москва, Мясницкая ул, дом 15, 101000'
                    },
                    phone: {
                        number: '+7(495) 228-0906',
                        sanitizedNumber: '+74952280906',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wzxPklCO8_CUMid9OSfiqqUgvgTq7O8swufFj7FHFmKYfNhXYk2V7wOdzvOjedTUXXDQZCb6u2XegPZgZmYWgwsXZDXdOOOzB6Z0j_HgPYwETXZ5XqdMxBD0zBobuCkwWEYZxs_rYGHgCQ2JiSF6xmv3PM9jAPTurzj-xRVQfcKss2cHGdYKGO4RBNsMiAtrlPiQSxIuXWgiYUEXEUx_V5su5fq9cJNKDRS3BxuzfrVNKdWpvfO0-ZRHS_VGSOG9CBQ9vQ6FltOXPAyu2Lfn7_rcOwQ1Za35Jbk5wd7wZYBalWjaX0-xrxP8uMUBrG3lyl49rwPd7zyint3J-czrNdmjO4tVr7Oy8wfu5B_cTfmjUd7x2HmDb3v4Esgg1oFf4JY7eG8YRN5-_KNGBqzf9q7W1SvBtJnFvVEh8D8jEBBtrHPR3PYlYbgeOXqLRZ7qLHwOx_VjyGUxxgAwyxAa5o_JQ8ebJAqWFUHxWD5OZI3O6Kl4u1P47EgbTkn1YcCfacb-tMr3sqiLjxKyFP-UZISTAZ3D4JoLC1WxEpg6_EsHJhOy5QuPtK_0pHBfIDBn8yKYBEZRDFmojr-d1rhHkAm4fr_gU5SC18K5MCUMFAw67nQyQKEoEgrxR-ayNrSGtmpTBITeRn8zrz3Ly3iH-OuyfY_x44UPOueiRcp2lfal83Qgk2Lnbev?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_gM-T8AfiYaMu__WFt0q1IhsRh8x_EeBcZKSmBVIuHyjTGBwAtZwGWBKXyyrLhjXFn1H3ztzByDrnK8GpklWkd3kmu4B0GbM2xrk83_vJseCCNbtRl_xDHUQvlrfUwMoucKBfKh8qu8J2xB_vuXgWrHYpQ3tUD5OfY-TuA92jaCw,,&b64e=1&sign=8eb24cfa2d2fc871343538a8beec7845&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '450',
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '450',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 450 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/210413/market_oKWK4mBnN_katxvOHWpcsg/orig',
                            width: 700,
                            height: 700
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/210413/market_oKWK4mBnN_katxvOHWpcsg/orig',
                        width: 700,
                        height: 700
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/210413/market_oKWK4mBnN_katxvOHWpcsg/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '228-0906'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '20:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.634542',
                            latitude: '55.762996'
                        },
                        pointId: '20375',
                        pointName: 'flashcom.ru',
                        pointType: 'MIXED',
                        localityName: 'Москва',
                        thoroughfareName: 'улица Мясницкая',
                        premiseNumber: '15',
                        shopId: 3534
                    },
                    warranty: 1,
                    outletCount: 1,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/4nHj2f7SGNaGDgLb2mowaw?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtULviAKmmEEoDluo3OuuBkJJB-lZVf-ihTFA3p3BbtErVW-qpl_q54W-4Vysh1O1tiIXRu3GhIFOvnpmKKhOAUTejMjb9GtVvIzMdffXXdiuE1AcNjxR05G&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZG3eSSXutGLuZg82rGv5S6j-KAJnqt2NhPWRbAr-eeIEcOxgOl1qOPSfmxxr3vqm4X9PWfe4Br1AowcRKUHCWsffbJMK-r02P7s_O_CBbTYCJc_joMcpNIRniu0akZoEwq84-ELbi_NbT5mCrXqPC1qM1JmfJs0RKWcuvWFjQ6PJrPM4cK0PH2-tSLxFVhvfpqFrJI4LFqMxekWYnkQ1RdqUyg3DLc410hk4evOaaRJG4gGnVc1AUTI_SCYEF66GlwFb94AxAshlh-3QAoabpWsYu5-Umy7rJ0',
                    wareMd5: 'ROLtOSBRhAdCADDx1RPtfA',
                    modelId: 1724547969,
                    name: 'LED телевизоры LG LG ELECTRONIKS 43LJ510V',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0ww90JGDgGWSl2RfGSEBfKqRbXb46XfS_bkKKcP-rj_TjNucxuNtK8HUeXo5oESfJFVeMYFBEdLRRWhOziLNzSEqZjyMSSL9lsuMICVfDs0rqubPdcUKcWiy8a4dpNAwsZj5nwhnxSvA6If0FoA36zPa-TjVhyD2gqmtl0DWr6eh_u62YMK0jwr1JjaDTMLQIeiaExJ8RNn8V1qb0h169F1emIgBKsLWxuDW2AYQXSwg1s6C8GC7_cocaVEilDEzLlywPpq2jFDOmupvgH-JyGwCx86QI06Jq4d_cWME2oAdkqJh2ascKiMd6ccKy3AieDcQWECQVRUvs0FB8ed-jkRQvXmYzkRnQF7M5oAkzljl5aT350Hs2FIJrEUpKfol8eg6IOcTsfyGGwou2gGc17my48MnOl3nAtqVW1atemzKT-PMBi8TGcsZ-12gNyUkxfFXvdnp2yh8iIdRvB8F90GRIdcoWHFannAv0SCukS6rc-PZHbsu094JuSd3bmHz7ZjUiUn_w-PUKl7eNe-jCp_FLc8R7VEcJ3Ko4w4BcVExJcizGweLUIIZ1YtunOnOkj5bPR0INtQ7eRx8WTJT1edGLtHkph13MBGqEeMRNvMP_MefLaW70WBSP7I_mrsO9nWxLEjpyeI0vY_WJQSdx2nlJ26Psigev5lt8VEr3s0LxHzLh9zshVvn?data=QVyKqSPyGQwwaFPWqjjgNum889hULbqqgvkeNRQVgfgV9IMA7b7bhaO_L1-W2Rka2r507Tv37pTlfI6rBZ3rDFpYAOCqlm8cpDfjTWm4DUxMvs12WC4D5_ZIcmw8SVrb9yWKo2FZc8SBHq1kp3rIOeH1emG4rHzNEOQAcDH1wUYxYAq-ud8nwmiMs82sTvDo&b64e=1&sign=f253f98d56086446856b93919b3196c8&keyno=1',
                    price: {
                        value: '24000',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 107992,
                        name: 'ARMADA TV',
                        shopName: 'ARMADA TV',
                        url: 'armadatv.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 111,
                        regionId: 213,
                        createdAt: '2012-06-13'
                    },
                    phone: {
                        number: '+7 4993941782',
                        sanitizedNumber: '+74993941782',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wzS7TZGd5pznxAK1nO23KchE7IN_UA1_j77Jo-IMe6vbCBj6KcmDITJa2rwqwy_oSH5HK-xP2ehx1oN_GuSqtevaftywd2tYH1oSRB4PU2wcWUiV0ugMFWiM5e9tSBNnq5EjB8H1CF-OnNLGzEW3p4-PbGGIteoSV4bABeE58WCBOQHNuOvEw4SwvIgMv5qWx0aJclsZUsdCeGLizJjdUunA7IhW4jzQHhewM8dJO2yv8rkSb1SjQsKoXPudG1PUyB23A4cT-DUTnU4jvTDxhcaJgvNKm4qS9YNgejD5ATSaP0qH2Um9SKa8dGwRsRf7oFyNs5Y7IqKbfd-mskwnwy8wNw9Es6bRsq48TUkmAHh9LzR8qoxqS9TwWamReduPjFVZgbxn0klvXR-kmhlO2tnAP9jg4fuAmUXJU4tYt_AWeuTDOO6JeZaisOa9oTYeLP1V0FB9UCr-ow2h0VUQSEUSvNzgU9m4wcX4Yi-KlwcqbN8yr6iAquPdgczNkVZJ9t4E1xN3aLknXPvNDGnIf6OlOkA0xZHFEwH7KwrgqGVNXZZ5RfEeAiytb454ZzPkT9FHIeQtYX5MOOlH7YSfBBNv70C4u1gOZOI-TVuLbW-8-E3xmUHZgVeFwPoVfHQFd0TfePo_EX9FtzzxvJL2ZtokPdqEfV_xet-yALVGgu4MrnhOUKTjnV2?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-anfgpyVC9EE3p5NL6wW5RgiMMRDJ97bHO6TsWVAgwazYAo_wvAITSojmHjmKZxyrcxPSEzyZanvfx7oQw0TIcJKyNh59P-ua49NKY2OyjNVunvCJYUKuynHhoilRraSmyUfuy6yh1E9WDX2gUur5T0T7C6U4O-g9i1wmB4e-yFw,,&b64e=1&sign=b403cf8a0a0b0682ffb68cf3d6be06a2&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '590',
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '590',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 590 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/219743/market_DDEebMzWX3qY9IwjYWWvzg/orig',
                            width: 260,
                            height: 171
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/219743/market_DDEebMzWX3qY9IwjYWWvzg/orig',
                        width: 260,
                        height: 171
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/219743/market_DDEebMzWX3qY9IwjYWWvzg/190x250',
                            width: 190,
                            height: 124
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '926',
                            number: '549-7366'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '12:00',
                                workingHoursTill: '20:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.502786',
                            latitude: '55.741135'
                        },
                        pointId: '8862941',
                        pointName: 'Интернет-магазин ARMADA TV',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Барклая',
                        premiseNumber: '8',
                        shopId: 107992
                    },
                    warranty: 1,
                    outletCount: 1,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/ROLtOSBRhAdCADDx1RPtfA?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtX94AmpuBsRfRbOVl2gbWs1BJKF7nKbYpBE3T90CpXZL4mgbIK0ti7L7wi_URDLUPL2gmlaIHNwyb6EyNFDIiKedAfolbypWQX0Ik9KovOrwSxjhTIM3uh4&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZE_Nzu3KhQFx1mGDaiMz2RmldwK8JK4BuWInK9dq14OSQ',
                    wareMd5: 'Lw6irgo_JpStrYnp6zPXaQ',
                    modelId: 1724547969,
                    name: 'Телевизор LG 43LJ510V',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZRRrA4uRtrCGHsdheCUJfycCHmxkikqyFDQRcxqWFhemAw9PbBWr-e0u1Q_RjLGz4Bsy6Hj4uxfiTrdMzNkQiSkZ-gNKOSQfthROUtMOSrOAabntuN7hDaEQqdM2foo6ZnnOnz6PGiPocKTYoUPRGSCw---MY_mwzh7wXNTzD2Dfavdc9e5foAKlN64tooLtTOp9t-gP-DMsPSnEC5Q4mN0qUWysW5jcQYj1KL27LSyrZPxtIMGA1oErm9fj4vdVYg5M1yeu77H2eIU8SlucpQpJHpTC_oJIOje-a-W4MtvcXOxS1KJg3ynGBM70YvyY6cYZbRrKUeyF1jYmD9hKL2QWNaUTOHLo8C2ujYsxdGKRgA0s46eEkr54AKr4bDruwX-6aIEc3nHm5i3F393jwGSq7MR8QHNwGw98F0x6VghrbMEy1KToRbAn_JeP5TW7w3TNQyZHhxu_k1S-GxBCAcgD272B6O1HvQMDD9nuUAmTinEZT_NhO586xEAsA45nW9XKG2a6B9-o-dx-OjenNphZLDk6dTm0HjXI2eR8y8pBXN3zC8eS0rqO9eqS6blftKSciq7HAW4ujESYDfYSX5V9uGcgFuXUp_adBAizhp38bHLwHRjLywY8JRBXf8TUVoUXZ7PztkSPlINp8jmU2eHONb0Nf9XvgbMLZS2RE5_WiFnF_R-wuG1ptDiw06bxog2NUzQUceBj18BLQq3MOHv4K9BZ1ktt0OlSUuR42mWN8YcRQY2DA9mJ2NP0QOP68agpoBepZGJnvwMh-F9EdJQiY_S7aseAHG4ur7TV2HAFKeszoUGgFeB43DYFCGGFN8s_4rp9w1YOLSWpyDsnZ59oCArAs6OYYRhDsQpURgP?data=QVyKqSPyGQwNvdoowNEPjXwQWWrc6tY3Ql8Cm1Fw1Lqrvc5yqFZ82ymXPkL6xeYwwie7UXr1eWIU5jTMUWG9a8RfDEp5T0FF6xesQt4j9nmAWAwPCOA15UuWMa0RRRvcHUzspmjcfUuJHauDlgl62hi0J8nha8d4IXuCqUB4ZPFf-wgZ38y6AU_Bbkoi1Y3oGcGCs-NBQu_O-9F_YqRT0Hhg3XgFRJiZi0Y7DbDugZGzAW2n5jqfkct4Q6hReR1E&b64e=1&sign=a64dbd27ad92c071f26f5a446b67dba7&keyno=1',
                    price: {
                        value: '23360',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 18958,
                        name: 'Техностор.ру',
                        shopName: 'Техностор.ру',
                        url: 'technostor.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 2096,
                        regionId: 213,
                        createdAt: '2009-02-25',
                        returnDeliveryAddress: 'Трёхгорка, Трехгорная, дом 4, БЦ \'Кутузовский Мердиан\', вход справа от магазина \'Лента\', офис 411, 143005'
                    },
                    phone: {
                        number: '+7 495 660-36-14',
                        sanitizedNumber: '+74956603614',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZRRrA4uRtrCGHsdheCUJfycCHmxkikqyFDQRcxqWFhemAw9PbBWr-e0u1Q_RjLGz4Bsy6Hj4uxfiTrdMzNkQiSkZ-gNKOSQfthROUtMOSrOAabntuN7hDaEQqdM2foo6ZnnOnz6PGiPocKTYoUPRGSCw---MY_mwzh7wXNTzD2Dfavdc9e5foCUwtJuGQwpzwYnTnIKCeciMFMBW9mmnspBEWGYVQ3eIM5UehyLjbhaL7sjEaEEfCqP1zS1xjECkq9Dx3pV7w-neUSLmAMGQIQA3IN2vpoQpGfUiOwjsST8vt02uEkp2C9p8L99fsVRwGm3CMwW2dnHEmLTM1vgX9gN9I1UgGicOACA7BLXIE5db-iaItJ45AKxROHBh-5q4084xMihkXArPWCoFDa_jfFfAcU5Do1CNWIC2Cxjj9rXpRAgfbSfsoNc5sbeTlG3741IHDFBXRQ5XkQyZNQun1M5_VtujTubF8xar2p9C53ujSrAscd166MEe-bLdGb55uGd6EtLohPFinofAwC6O00Tr6VCq2aqniFk7J0whsW8xqqpWS46aammKt5Tr0UobJgtctrIaHoSwVCfszi47-ia11sZIEcyYHkK7jan4BQOUfOSyEU7JrNdWnnSkvUjmuM19Z61T9E6X9qwkj36CsmxqHGh_jZQAC7RljzUY-zK_ZPNrgERUtYwIKEjBL_d93AkF2NDOr7G5hbYQMu7MFyv54UrKgF7Lcq4cSYxK0Y3uHAWC45sIjfpmz5aPKNTFXTL6yrsoKnkbxImmRy1eHjrrF-pFdG9x0gWBWYOeoE0hcnfog_deWPUkUmoTSlMGpBD5EMO5EOc8xatY0J5FV6K5LjTPMqP_ZOenYA5brz0?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_aM1BVbpoy7xxpv3FbmNzanUjbLDEsrkOLDV1rP5KzBbIrvQMtOX9RWX-T4X6visJl8DioOygw7Q4dOPck4AHOjNXBBG8taRWdlHbzSB4lMNmnlXw1hb7TvBUtRhliF9zwPFZrZL0iMmeBIKiR0cFf5KybhhH7uYPXY0C-e2oz0g,,&b64e=1&sign=1176ade0ca791d2a44b86b87e60839a5&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '550',
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '550',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 550 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/225325/market_9xaqYnpRvPTcG4yCE8PkYw/orig',
                            width: 701,
                            height: 466
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/225325/market_9xaqYnpRvPTcG4yCE8PkYw/orig',
                        width: 701,
                        height: 466
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/225325/market_9xaqYnpRvPTcG4yCE8PkYw/190x250',
                            width: 190,
                            height: 126
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '660-3614'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '14:00',
                                workingHoursTill: '19:00'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '7',
                                workingHoursFrom: '14:00',
                                workingHoursTill: '16:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.69050965',
                            latitude: '55.72251039'
                        },
                        pointId: '8745842',
                        pointName: 'Техностор.ру Волгоградский проспект',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Волгоградский проспект',
                        premiseNumber: '32',
                        shopId: 18958,
                        block: '8'
                    },
                    warranty: 1,
                    description: 'Артикул: 369-590 Телевизор LG 43LJ510V Общие параметры Тип телевизора LED-телевизор Модель LG 43LJ510V Цвет рамки черный Цвет подставки черный Экран Изогнутый экран нет Диагональ экрана 43\' (109 см) Разрешение экрана 1920х1080 (FullHD) Формат экрана 16:9 Стандарт HDTV Full HD 1080p Поддержка HDR нет Параметры матрицы Индекс качества изображения 300 PMI Частота обновления экрана 50 Гц Smart TV Операционная система нет Поддержка Smart TV нет Wi-Fi нет Встроенная веб-камера нет 3D возможности Поддержка 3D нет',
                    outletCount: 3,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/Lw6irgo_JpStrYnp6zPXaQ?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtVQ8fDnIlMKk-yYbgbrh-TSNEx2TXM9sn8s0LGnabrvpcQa-fW6XBZtvc4BeF1dWAR7NvpjyWCRC0l55sD4pTnZweNm1h2QMpPoBS6n0O8w5mOj32LrMOpl&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZGAuBBqHlF2yaDUvrcL9blgtkGtPZdsKnO8NJX9Z4AwyA',
                    wareMd5: 'UvhGcdLSENY5b48jSXMe5g',
                    modelId: 1724547969,
                    name: 'LED телевизор LG 43LJ510V \'R\', 43\', FULL HD (1080p), черный',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wxPm74yE3k5ssAGutw5Zo7QaU0TX67Sl3it9rtV6v7Yak9VqNTfZ4EmexO42uo7z0fREiiTjCivG6VB4Zebfn6IJP8pVeV4Om74OPSvnuKGOs-J_oECi0Qiss7xyrFsvgNpNUxSAN8e5Ff885vMj0G4IhEAfPdBkDNfnP--AqJNmwRyv6kTfCaUUcJNmPD6iwOK0zLjyujYXQ7aeU2SrEkovQz3U1vFkijHKKPyWJraFBx_HGr0TMBBBxHWU6bqyRdiKiDs4q7Jgd8GGcxAvthgp9aPFtwk-sQThas3X6lVUaPnPRf5pgSR3XbYBCyCX_QZFnFzgM0nyPqTS39J3YYMVfRQmrx21szDzZGNaRQfC6sYvogYqM6ooYiDMHsKEDVNHsoYwcWPQKj3S9OES1xz8tBMXlSM5gwAigBdCV1xJunmLICVYt9IIFt44Noe7r90JckhpA3FWCrLWB5aEBSierMcUiBRjdwPTk1mot7Z7VxUqcFxJaits6mwOTIhfFRAyQ9Mew4ukgccd563hNA9SnznrXrp16MrIr82b7GUndklfQP58VBYfI_MhC-eAn1J4irb9sOlu-ZecdSCPwEKBW6vwyJtOgWtP9Ja1fO8rGiGFmzPa9gwV-9S_-aLLZyY1vFwZAaaUCgCDarZ2rV_b7HOzTwE36iIFoilgf5qMJEoySZp5pQt?data=QVyKqSPyGQwNvdoowNEPjc7wlEUqfa6bq7hSZROC9KAnrMWHwKzys_yj2owRFxG3wTIIXXeFmAV7Iwho2Pcevxvm-lcVRSYoL_WcnVHReYdEEEdB37Qn749TwqhDr56kFJSTaBPOXY3XFtlBcr7F5tq_594WfZQhntRegyk7xtiZOEswaQq0SG2G8bj5aywoOscAH4fmnszo5qRBa3UIk_Y2mVkGjyutyw3IZsM_WKYWKmp6qJ4ZAwcFR-B39HE3q1hjzYf66dB10KTMDN4SvpxqRNZWVHTi52NK-ludJT6cR4RryX5nPa3WUg55iJ0AaA1Hn9pAEVvY2oCuf24-ewYSNAYP784sc5Xiv_W-p1NJRD2dsseL5g,,&b64e=1&sign=5418bbfb5d18fae858e029d8cf202ea8&keyno=1',
                    price: {
                        value: '25990',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 17436,
                        name: 'Ситилинк',
                        shopName: 'Ситилинк',
                        url: 'www.citilink.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 62913,
                        regionId: 213,
                        createdAt: '2008-11-24',
                        returnDeliveryAddress: 'Москва, 1-ая Дубровская, дом 13А, строение 1, 115088'
                    },
                    phone: {},
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
                            population: 11612943,
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
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: true,
                        delivery: false,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: false,
                        brief: 'не производится (самовывоз)',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/237949/market_loZkIA0pEbESpirPCOpCog/orig',
                            width: 900,
                            height: 594
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/237949/market_loZkIA0pEbESpirPCOpCog/orig',
                        width: 900,
                        height: 594
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/237949/market_loZkIA0pEbESpirPCOpCog/190x250',
                            width: 190,
                            height: 125
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '780-2002'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '22:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.768956',
                            latitude: '55.810896'
                        },
                        pointId: '42415',
                        pointName: 'Cитилинк на  Амурской',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'ул. Амурская',
                        premiseNumber: '7',
                        shopId: 17436,
                        building: '1'
                    },
                    warranty: 1,
                    description: 'диагональ: 43\'; разрешение: 1920 x 1080; HDTV FULL HD (1080p); DVB-T2; DVB-С; DVB-S2; тип USB: мультимедийный; VESA 200x200; цвет: черный',
                    outletCount: 42,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/UvhGcdLSENY5b48jSXMe5g?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtVtSXgxM__-_rl6H9FRl5oWcz1W7gKhU29cNLKIEV9aroXTCQwS5B6RRwoKeEp2MdYgchkuMmDALhmJ53EhsOpOcpo883xo-QKCiVR3bgSek98es9SowvHH&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZHKrgM422Ll9gW5kAFnBbf4k_oHhIh-smpct6wtPNUCDw',
                    wareMd5: 'nOIFrA5WDDfYSY3F3CsrnA',
                    modelId: 1724547969,
                    name: 'телевизор LG 43LJ510V, черный',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wy59fXEeaBXrNtAXx1jpGKewyfHTRC8bEAzXuJfJ-2HNQA6P7L7Ie2kFRs_qcI8m1Z2bVAzE5Uvb8114edCdVyNEuGxysF8ZnOVbDCcIJwVQCGaCzR0dXUn3Tp0b1X1fjQRcFNGP9oNlQYUcRRoRSKkMqTuEWqRnh2gkaSpZp4qgSbBM8EjcZ3nigS4_tSYfL4c_9uxZBmOQGDN6ioO9cT8-cTKzwCkK_j_9ZyHmm02plAGQaj2-Fqt05oRO8gYElNgSt9mHKXvFo8I419EGMqeHJ7MIxzJpC-4WGfR92BH17aHbNJ2_ppR0jN1-pEiuu74GEAVxR4J2RwTHWhare36ZAmI1InPkk2x2_57QKqG7wfc4sLyXZv-P_rU9oBbSEVn_sDL60yzHyjaZTbAYjfEiVRlmonJX9YKQ7LoXOooiNZGNpiSaPwzpJL1FajgiQw6G7leB4xryIh3SAWU3Qajd7I9Nsv-E5kVwQnFs6cWToDkB9QaO0y13neBQmubV_Z3l1my7kWDYcWgQ19Jm07hyUeQjGRnVMXsScMGqfaqvwPM5iIbjgrDb4mAltoSDejQUbUzEH9CW_9Gy9Tpj0erGk_wfnSWBfuzjxLLBsLP8SPm57_BSTngzWnIALkw-2WgKHHAKBaf0G-HN83T9dgu7kFZhL4fQZaHer7kg3YTIk38fU5uRK9k?data=QVyKqSPyGQwwaFPWqjjgNtwFDITzDmMA14CpEWWDnAJ7k9s4UesbEkHeg4YesxsHL8_09qt-Dq_p8EErXMVWTW_ky8O-lGhQ_1JPOEybnlkE8yTskq29c19P_bPHOzYPCqnaE-RBk5fIOOs2gdHpS76kTbIQDs_oMWnZm-q1DBgz0ria3hudbgXmEXpRDa3cgdIcqUveCJlbAmT94V2ZCfFO7vfPC1Y6NSa7nz61upYpyz0BVlMvg0gpK_gZCsJ_qDVImUMav_v7i91nYFbc-HDA02nyaZUr27UgVVlYQzKLWmce2LD5t5FeoEdiX5rZ_Bj1-_doVHN-cT2TthqFfR0hLOe4TYv0RudlPsmBBag,&b64e=1&sign=3c17125d7aa7d6198f9781e5a6be7b62&keyno=1',
                    price: {
                        value: '23935',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 25017,
                        name: 'CompYou',
                        shopName: 'CompYou',
                        url: 'compyou.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 4365,
                        regionId: 213,
                        createdAt: '2009-07-17',
                        returnDeliveryAddress: 'Москва, ул. Трубная, дом 25, строение 2, Прием заявлений только в личном присутствии с паспортом, вход с обратной стороны здания, 127051'
                    },
                    phone: {},
                    delivery: {
                        price: {
                            value: '400',
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '400',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 2,
                                orderBefore: '20',
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 400 руб.',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/367259/market_Xpt_b0d8FWOoH6CzNXSWwg/orig',
                            width: 500,
                            height: 375
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/367259/market_Xpt_b0d8FWOoH6CzNXSWwg/orig',
                        width: 500,
                        height: 375
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/367259/market_Xpt_b0d8FWOoH6CzNXSWwg/190x250',
                            width: 190,
                            height: 142
                        }
                    ],
                    warranty: 1,
                    description: 'Тип: ЖК-телевизор; Диагональ: 43\'; Разрешение: 1920x1080; Формат экрана: 16:9; Светодиодная (LED) подсветка: есть, Direct LED; Разрешение HD: 1080p Full HD; Стереозвук: есть; Индекс частоты обновления: 50 Гц; Год создания модели: 2017; Прогрессивная развертка: есть; Акустическая система: два динамика; Объемное звучание: есть; Декодеры аудио: Dolby Digital; Поддержка стереозвука NICAM: есть; Поддержка DVB-C: DVB-C MPEG4; Поддержка DVB-T: DVB-T',
                    outletCount: 0,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/nOIFrA5WDDfYSY3F3CsrnA?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtWOIVYlmsvBczvr5KHoR-8GvKmIf5kEPO_Po5y8ZVgLzoEymOjvCuSjYOu79cxHt79jr1_PpzHuwuFZm5Z2vdx_FQ-fq49PVO3LSRTN8ksNi4kxYvJm3hls&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZHGKSceRtwdO-aTxCzNBp8ETedYqZtYPfRpfS7vrlR_7EJDgduNWCc0UKi28aEHLJXkvteLbKXDFtNEmBeXuE9KOF0YJQdtjpiLlfUby3iVe6feUotyUbTfcg1tcOrb7E18RB5HqZEcOO4i81oly4lY-6pjQTCYT_9Sg1Y_Mtr5odiYs8t4yj4nVoFx6MD1OQW2rKmk_xXdy2GfW_AKenA-eiv8ANS6Y7qk1_7MWJ7hCgYry8yqzweX_6n_TrqwF7QoZuU1NT8ZquNmHk6z37jepZSZxUMR6pw',
                    wareMd5: 'Udm8BedXUXBb0-bbaRVP9A',
                    modelId: 1724547969,
                    name: 'Телевизор LED 43\' LG 43LJ510V черный',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wyoY6HyTITWf2qhHsyj59GjEUpNdIevdOVnsaPgFc54jziNMjE2vwYn3a6eeeYQe92I_Kxq8cOMGtMn1SwtKzKqGaBF97_-M05WQYYcR0PzveCHxiC7WdzeSZY1FstvUEVmMTAkRTHYsh7cIWNstTfhJzNVpFQoSY7Sm5_JgvJ_Rdf3GqhwbBZFKSjlRUDytJICibJ2rk_5e3EmmPs0U0bmTli-Hcu-ZFc0fTQB7L-IwSclKVtxPiExpxrlqNi-pFG21h-E2dZcjG2HHs3Qx7megXTwTgqZZYrNbUROTiSEaAomB2xbMNMEWanXzgKPgzNJaiLdO5UDpMd12uaHRSnfgS7fIos-BI00BesnXbKwpWdpJZCHR5-uJpBEEObJDyM4OI9FT7rwlAMf0TyuX_2VvX16PBlh1K4T5F5jMic1DDwx9iGM9iTab-FgKBvkcSlzMNP71AU_ggBxoPbXxOXwoVWRAVcaiE9m-yCRc9ttMnffmhE0vuGVUyXF7f9QSMwXBAU5UV1npv1rVQgOKplHOgifQkxXiff1rM0mcrvXjprCLpgu5--liTqQteutzSMfUGt3dtyoROgfd-pNgUw1-2N-kFZfirF8UYy_I9RbXLJ2XGXTU2-3WYBcJqnImdo7FCWfP4zxmTFxKMuXRC3ewMtRBZzhShpxbjaMgJYltopixnStj7fh?data=QVyKqSPyGQwNvdoowNEPjRNC085CYxZJ9Wvx3HHQiOZphk-qWa2jWS7-bTWIVZ8A4A53Cka55zibaWSQbNnGkNZlqNay9YpUsmP8ghcXJk1uEwct-s1sNtXav4w0bOkOiPoceaD2z02l_zv-c45TRhFocnL_7cCCmJ6_CPU7arAUi6rgr7iauKXw8tp-A2HwwbOz-fk8o9tzzGat0wHTrDuqhkDBuNqaHTzUQPjOzRV_5U4G4pDrmsvXNMQ3yu6nHugjUXcnttoZO8G9h04oxCWHcBCOR27u5KAPN8QVerR8rKiqmsFj7t02OiQbSpap62zJ8rUhKeSIEc8AqYUDHg,,&b64e=1&sign=8c3c2dda2fc5a0a0903e767112e18153&keyno=1',
                    price: {
                        value: '25990',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 385207,
                        name: 'ПОЗИТРОНИКА',
                        shopName: 'ПОЗИТРОНИКА',
                        url: 'moscow.positronica.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 1626,
                        regionId: 213,
                        createdAt: '2016-10-31',
                        returnDeliveryAddress: 'Москва, Огородный проезд, дом 20, строение 12, 127322'
                    },
                    phone: {
                        number: '+7(495)256-04-33',
                        sanitizedNumber: '+74952560433',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wwevMHUesPsNnDxBQbn_jqZXBUPSi3x8Dk7eqPzzdP9As6izBYJ46841M7Pr7XZIeZ0GX-F-H9UshZzwOxqcKgKQTjSH47emZka8S_h1njGED_wzt1uE6EVp7Hlvk17NUDIkf47TXb_UO_KG5BstrrcMD4ytAvVIqu9M21Ca-zqfIsfMp-46vVT8o3owJyclk_-yW08akx8Xo0W3_y2zpTYmscLGteqUnnCWzaYIze0__wPu_-4PzyhIAREauzBFwLFnwHAm7hqL2h7nhe_lE0pYS5hinfTpHIUwoxSz046cTLe8I72NOdoL-FenLENo7DdYqdR7CS78acM5AHnwDIp4foYPgDOTAsvue2oPgzgmdlzYXKlC-ax0qwshvclkN78ztfZ903q7tB3Gq3vUCQuiD1pIQ_GpNoIKEytCDyOTkyrNr0vFEd138GKYpItrL4u6g1Tf2xNvPhDI6Efmh7fxMWUDC6BVUn8yhsF6DMSIf7jQgyqIy8YGEvts5vTJX-tI5j_A2T7m--b8-bnkF75Rwl8PwGMMtDCfZ1LKwAmM05yJ2l-Q6Nm7k0Tv1VwIgFIbAukq5j7UqdpU9SKySbnoY51qL2FSakH2XqK1x09WINbYa2hxgijlP8-BsIFCNkPF_XD_6cBC2EUFmm2QQ1qMBW8G5YwnyMWG71NrZRPXSZNVUYphHTA?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O91MIkaFVbfxzxQQ3UlnOvGl_FApZ3b0SKG2xPG_D0n9baCn7bbQfgt1ByHfaSV5gZ1Gi0FzTRswhbOyKR9AmAVLX7ffE48tjk8iO8cu7sk6R-0EsAdWLGF01A14m5DSOHzY8Ep6M8M_5U77Ipc8ZbWMpR7QQD0EId5cqvL9CEX0Q,,&b64e=1&sign=9cca67099df93a2e885fffdad2df5dd1&keyno=1'
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '390',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 2,
                                orderBefore: '18',
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
                            url: 'https://avatars.mds.yandex.net/get-marketpic/213450/market_dAqNth295gyUjOpIMuD2Qw/orig',
                            width: 350,
                            height: 231
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/213450/market_dAqNth295gyUjOpIMuD2Qw/orig',
                        width: 350,
                        height: 231
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/213450/market_dAqNth295gyUjOpIMuD2Qw/190x250',
                            width: 190,
                            height: 125
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '389-7018'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '19:30'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.605131',
                            latitude: '55.609787'
                        },
                        pointId: '768843',
                        pointName: 'ТЦ «Электронный рай»,ул.Кировоградская, 15',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'ул. Кировоградская',
                        premiseNumber: '15',
                        shopId: 385207
                    },
                    warranty: 1,
                    description: 'Телевизор LED LG 43LJ510V черный, диагональ экрана 43\' (109.22 см), FULL HD (1080p), частота обновления 50Hz, тюнер DVB-T2, DVB-C, DVB-S2, USB разъем',
                    outletCount: 9,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/Udm8BedXUXBb0-bbaRVP9A?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtXGT7Llbm8CrFAmnwg2Tb4HeZ-0HSbIu0KuwfzzA3V1lR0Mp28uTeY5Y6nMlwJS2ILR24-8cQtslfErwH3nAbrSuvdy4gBS9jomppr3doY202EQSp_w2azQ&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZE0jzVBcHv12s8TkwL5vDDXP8-snD00MzUofJ9JQQKYTw',
                    wareMd5: '__hr2_Sk6se7oMndLo-guA',
                    modelId: 1724547969,
                    name: 'Телевизор LG 43LJ510V',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wzNhFBMOL8j2y-s1M_6ZN63kx1kF86DmCjUtD3HME_IBv0wmntizmOkacISfeXpwJEP5ecqp4wHC_596bo5iQqbIPVjHyv0RMb90td3S6la8akeK0twepwj1_7DZXtDjUjL6HHvPvrnEuMSDBcYFWYJtycReso4P7ZFIAHnHLB_zSxjl0R0VR4EyYjAQsxhu1JQ6ydXwXCbgwmjDpuvpr6MCNJnfMbWG1TfQk3ImzMwzi7oXHlXlX39Tn9cjIWxAwOX1nM9IXAQsmGbe-mbb5p55TAI5XZ8EVse-4xbeGfF29inahdJgiL67DEgCkN3WzufJhrAhxSVeSqGKCxgO47kN-JBQQKA6lK8gnpkI3Wp24asU-Glci-MLPni2iVovBUXCR8ln8fft3WxZBbALnyW5hIiSVOx4C8O8rdriJa6GoWkeZHTo_1GDRuZZSG1hVNbmnsQRd1HW5MPw3DWkF6taiCEAxo9cemq92ykBnE0utR-KhHTaqU-j7TQxtcstHUBskigJsHePeDvn6FEjw5614Baygf3mkISo4ulj8GKZ35w1DAGrs7o7WAL41NbrQPyqC-BqtepSbNsbN4qGeJqoKUoYWfCXRTn5fTKp15o3THMyAr3yQUJUGYtveiDMRzWm0wpeWjQBA0IIoQJp8Doia3sh_BM8Oq6M-1btvimPA,,?data=QVyKqSPyGQwwaFPWqjjgNoIi1mBHeYelRjFR2wDOz3hLJ3Stgg3P2FreoZzgGSmcdXWYkuhe2L01PhEPvUrpK0RJwWv-NcSKT4QOvWPgugCb5dzz-OnhCpPgdJbgQnAS2J4itPnQLEomJ_KfCesS-7NYEoi8fYgxCSy7qW6q0SdWY5lPI3iAVfjPJsbimIQmZ8I6Tv9QvYIbvNIFVQ1_019cGCbeF44PlHR6adXT0IjXFtBn_2jVhUQOptkdZB6SBrHs5U2rqF76MJgNs8T48WSAu_dQ0wq9&b64e=1&sign=6f1310ee2b8c2191785580a7e273d60a&keyno=1',
                    price: {
                        value: '21490',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 5478,
                        name: 'TvTeam',
                        shopName: 'TvTeam',
                        url: 'tvteam.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 832,
                        regionId: 213,
                        createdAt: '2007-10-08',
                        returnDeliveryAddress: 'Москва, Барклая, дом 8, 121087'
                    },
                    phone: {
                        number: '+7 (495) 7963143',
                        sanitizedNumber: '+74957963143',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wwN71issO8G6hf8nSAdBoATajNcmylX1RXVIADLcrg66Kr_O-_uBLCexYFXO7MAociXdIMgBCdPR5HXXir4jTAdd3BF_kPd9AAlRVeSZiY1QEdhT2raWLfh2dQo2tglQUeLg1BAsoGdnpf2kvlFvH026VBCfMTWSyWPJ2f-3_YEtTXhGC92xYFs_F_2L2yZcXhBvyWELMQjGt8JhMtQGMU5BeiqTcCghgUWp27Jyz2_BkKSeeB6kA3F7n2XWqxbafOVhwLDyA8uVHNDa7F8IoDzp-XLXQZa250rV_aWvAiU1sshNFfmawp3u5e1NqDTs7gGyVbXjg35DWxp42phd3RDNG4XjRN8sLUAcZjk_9XEYqLmMI9BY-cYoJwLpt0MSvtvkj-_DInVXL5JSugqzNrcQDDecgRtrI7VZ8wYfv7Z03BvUD48vkikbr73HzIw5DLqLl2EOBsMoT40Rw2YEgzHin_9SU-2El40PTafszezTKIaef5xj9lxRREpKnsE_liBsLGjZwetrC_TQl06So2LIMZ3pjSZofR5LYCiCAtpiIRls3qXvP6EiHICK4bi2dxJ_XUIXopG0tT6h4cu1VrpbRqEAyi_vDIlvHSoq-BNGeKH5kR_A0lg-D5wG7qtZebP6itgoUrYptGo8cAPJAviXfM7lTwUaG5y7WIsbzAgWw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_Ip-P8TRe6MQr8H5OaESXu148BKhJjWwxhDRNSrq2prr43ZFmNk0EZFrFczQYF1V4Jnd6APc4m8bjuTqiKsZCZYnm07mHtLPnDI0E07tfxRFNmysGAahqF_-hOnMbnpUh7rNnJqnodZdHRmU7vm-eORnsNtg1gPercYuOIPnc0LQ,,&b64e=1&sign=b56371759cf9f41f994ef9271ecd9c46&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '600',
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '600',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 600 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/477791/market_1lb4xRY15u3DGO3QmfsFyw/orig',
                            width: 600,
                            height: 600
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/172323/market_TvV5kHKmybxHEcWBSNalhg/orig',
                            width: 600,
                            height: 600
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/477791/market_1lb4xRY15u3DGO3QmfsFyw/orig',
                        width: 600,
                        height: 600
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/477791/market_1lb4xRY15u3DGO3QmfsFyw/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/172323/market_TvV5kHKmybxHEcWBSNalhg/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '796-3143'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '20:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.50231385',
                            latitude: '55.74132162'
                        },
                        pointId: '1090',
                        pointName: 'TvTeam',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Барклая',
                        premiseNumber: '8',
                        shopId: 5478
                    },
                    warranty: 1,
                    description: 'ЖК-телевизор, 1080p Full HD диагональ 43\' (109 см) HDMI x2, USB, DVB-T2 тип подсветки: Direct LED По-новому глубокие и насыщенные цвета Помимо улучшения цветопередачи, уникальные технологии обработки изображения отвечают за регулировку тона, насыщенности и яркости. Революционное качество изображения и цвета Разрешение Full HD 1080p отвечает стандартам высокой четкости, отображая на экране 1080 (прогрессивных) линий разрешения, для более четкого и детального изображения. Улучшить изображение? Запросто!',
                    outletCount: 1,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/__hr2_Sk6se7oMndLo-guA?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtVNhmQxd9r-dlezcqBZL_RhQNqM86DByg3SvPyK1Munw7JrHW-ywz7E_UtNo8pfTlcFqWvOR-jRBn7bzQhYtFeQos149L1MReSHqXAYzcMMXgWwoQnI-ega&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZFs1IEP9rwtKluHz-q1QTbf05YjJtUFtIfXXUjGB7RMvKl0a3V9qBGp0YECWQYVPqhdw4DNazW1NByXSMDjZEvXAfuzSvmfbFhfEGJ7o6kEfPVPqWRLmdaUGND6AWxc40uZti0pRmMoL_b41iz306CaUcrxqZEfsnD4e_I3XcbKA3TK9lK6-RQ4TkVPauwyB4Uxy1ZoVxk9hxc9X8MVQVwNQH01Fb3fTY8p6LpdmTRy3TqXZWK_dfYbObGI5tQeRv5YElhNnX-UnD6_cngVTd2fbKaXbN4_EFU',
                    wareMd5: 'lAEyJifOIHSrm6tx0toJgQ',
                    modelId: 1724547969,
                    name: 'Телевизор LG 43LJ510V',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wynYgYyRmPVK1bwal2B5fSuNvDYFOF2-NKAjF_DbtuIyQ8VHsRCXSLpgEpIx4sx5i9ZczMIShJC9nD571TAh7c-qtTemCPg-gFr2pczJhDvtszjNg23g9DvLbXTRY25bCo6sCaTXdtQQClLGXNOFDrjhO6TKXGBjWCkFXTHkqR1gbU18exFa-kqBhXnLoZznlXJ1pz1stH4R8jlKxB66RnGZ8J8AFlswAeFBiXuc72KJgrs-Si0J6V-F4JVZxJHOCN0k65cBTyZpO-uOFGuAAd7xDPkcYIoTWxiemu7R0jWHq8tx9w_K0YZANpqLnAHzXbsomrI57xZCT_zNGQ07b-0ay7_8IWs3JXG3wDonbCCz5b3ZQFrH3ZCLVdwl0gVbgnuEVBAI-O2TrO8ffS9UynAQ8VMkFRG00x3_jib6j6xocYrk31N7YX5pBMXezsnDI4-bnpHKiSg6cy_qLMnoPip5w8ncHCYWVQldp_je1ynDYVY825lo6yWCqCsSBi3VGIkpl0UInBCgoDhTUuC9OR_wahJE1U91Q_7p4TcgAlZ7ddt2YnrRyJBoMnT1_Y_PFtvVI_sXx-Zv3-IWUpuzS97FDXEBeC7racsv6YtXDqdX-h3jL1GY97ARP6xFpIpn6XC31l4tvvYT5EqDO0-AXblcgS8ykiOw69IZAmJZarR-sioyq7Qdcd6?data=QVyKqSPyGQwwaFPWqjjgNhAIZCHeTxKTJNQdzTP1XMlB93QX7BhHu2y-jKMRlmlAlbX7NrvRrfeJ4c8W7EvEBK1H5xkc5kDv7tLzGJJlmMFGgnTcdlGrIo-0zHbxvw7nv3vXCgWqmGnLQueDEFocsxbumWN-sF5hItoTlIcPKIJs47mmjDpAjE91_5Oah3QRFBUpYCG6ohobw0WSuigql4Wq9yn0YR-yMuiVvhe6XyQt2DD7Uh7Al7ktY37RmwejWrkavE83728,&b64e=1&sign=0f43097a17716b9d25a1566476c51919&keyno=1',
                    price: {
                        value: '24960',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 243997,
                        name: 'Allo-Laptop',
                        shopName: 'Allo-Laptop',
                        url: 'allo-laptop.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 395,
                        regionId: 213,
                        createdAt: '2014-08-11'
                    },
                    phone: {
                        number: '+7 499 258-30-45',
                        sanitizedNumber: '+74992583045',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wzuSn9S5G3Yxjs1lSrKVg6ZU1fjJauu51mp6UNLkUYBdWaxPW58cuSn_MEpnGXaQ1w2gk88rG0eUmUD_oKsH-0WZefIfubbKTIjDemiBcSxlCil62AtghHDnIOTDLdSwR1e3UP4AHS2KpaOCfRpa5d4Hsk-8zPXRlKEmWqTHGnUhB0vPIhLnXaI-l9kTLoJtPpZRib4aJQQBExQWp8gPNJrw5CsOErjgMCAo8chT84GW87wxZ01I5LMEGyl-clsjEsZyir5ze6vEXY_llbrWsPv0EQxXWfQ4PbozqLxul92Wlev8tKv9tVx0HkIASYa_fmdpFu0-r2ql7yFZtE9QG9rAQd0QUPhDk4BFIbIb-6Xjo8ReaDMCq89bqqabeTJw0nFOFAlTuywqTsoTOSl5SOpqlK2oPwvIf4x4HTAOuz6JrQi7bBxnSA44t5VNhDa4SBv2ulCeYA8pn9n4EKfhg0KLFldjiWdxj64TJgwxlNoBngFam4HsqGglS2tlqghZkfj44g9erO0n9HbWMQyosyCbuD6EArOG-aBmLR2TTTfSEzxxDhN7NknKfhVBgQlD3G6sk04qlnVw8DlpRST_Al5f6zUPch7k1xeI48h88KhFj6ArIrbVnGHwco_nBsCIBf-n9S7mxrNS-YyHvJPmaYY9Htu1CY7H5QTNxgS2Z3IvxRYmgTdf4B3?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9Hucx6k11bz_R3AODTjew6u364mBPmbgVuNd5DAqDFnNBFq_Nag4aFdB4YBeT7BVYZ-mEZUQpIYRNbzM4PqEDujTMITmrBfPsMM3ONGLANa5tsz2krUetUOXIn4fQnknkExIschW3vXTPW-zzyMc8okuB6Qwtq7Ix9z3Ea4zCVjw,,&b64e=1&sign=fda51d65f61566bd5f49b171f2437ac2&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '500',
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '500',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 500 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/214785/market_Eff5vhFH4yLfMdyH_NCHNg/orig',
                            width: 600,
                            height: 600
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/214785/market_Eff5vhFH4yLfMdyH_NCHNg/orig',
                        width: 600,
                        height: 600
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/214785/market_Eff5vhFH4yLfMdyH_NCHNg/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '499',
                            number: '258-3045'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '19:00'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '7',
                                workingHoursFrom: '11:00',
                                workingHoursTill: '18:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.690066',
                            latitude: '55.711574'
                        },
                        pointId: '363111',
                        pointName: 'КОМ-ТРЕЙДИНГ',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Угрешская',
                        premiseNumber: '2',
                        shopId: 243997,
                        building: '98'
                    },
                    warranty: 1,
                    outletCount: 1,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/lAEyJifOIHSrm6tx0toJgQ?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtX6G_V7se_jhaFUgPSrpDx3ZkKaLtRZaRLW8a3zgEyeDfxZ2I_rmO6DuYnfltE181hncsmKJka2C71Z6eYsSIo1dr5QUE56EMC4lgMZooPay-FVh0sdoKhG&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZGQCWaKR-AwOC8ZiDTntSrffbo1GkKfpfdbeDcYw989oH-apc8fzzedEhdo0_CBBrs3wL7wseqXtsqI84QIfiwYwwQg6MekkYt3aAecjKzDG4zYciuInhTqpg0qm4e0QkICHNmwwc7rkJpVeMYcF3en7f5LEHWs19DcPUaA7JWFufF4j3KkKQIm9CWdT5wthdq9yV_e_X-w7Av0miOG6ZxQHcQ93pBFpVhg1p-idXegFpgKuKvHgS-4bZxhYHwe1XQaDKFSGlDPRt0WnN6DZ3e5Ha8wfa7eiF8',
                    wareMd5: '7m2M8WQNOIPjMEJ5sHfKbQ',
                    modelId: 1724547969,
                    name: 'ЖК Телевизор LG 43\' 43LJ510V',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wwTe26OFZs4yuD7jXmtCmsNR1FqRCvsvyCsG9MK8msTD2MlQX-8J8J4nIH7qRyIP-OFxR09Se1ntFQn86mha3gA7zWcs4TGuZkj14R5KBobvGBjjr5iGEyUFvr_fWHONDQX19CZ4kj8Z9dPt210SEdyDwuCeAe7M-X67ZQ_BPdRw1o969nhbvSgv7ZbJEvInxC5Wt9caPAMSbh2MloSYfnakgdjUQFM4A31T1FUqf_UbexTEmIKa2mV4NvNPStVW1ausaPXTGtKzZMX2ZmVHqzO0zK0LhigEuwjnfo1tW09T7MzlnxBRPVqR-6NVX3b3QNwV7DmJjV1WY0sR0phq-mgx0HneT0oF-0UARfcjsiBDg9hyg2_fgOwgm-aYwF3iqPaFAfPEjIUG9iw_2xPsUJOBh0W3oU7sRKiX9ymOZTSAsWk2X84EHwloCQ2IStMczZTTgg7nH-G2XuqvYiUnxhNAkVYxFVVqmRi7L0UWfzDvFI6-d8wZ3wu_21CLUcfR8pSn9dRhkfERmCczWmPMqzyQtzsmVA4BhPwK13vfaPS2q3E-AqOkO6uwxbkb9qoVKsHqut3FoFQ4J_0QpKZaI4X-LkpfZH463sa1pdFsNXtzRCjLpE5_uvyqGUjMEM7E_wAbGSgDMIcansEdy-v6JBIzcfr9YxtsrL1llWUlS_ZAg,,?data=QVyKqSPyGQwwaFPWqjjgNheAGY9eRi98Iz677nrFsaKcMyX9LWyZ-x01AuatjvWsBgbo_oUr2Sv420k8E-1QerPKlwvJ2tlArkOmslTEqR6Dqnn7aYOB0UCrcR0xWpP-1iE7YYYQP-Iz_qSsFfQHjg,,&b64e=1&sign=5dbb9a4951b235060c0c7cef056b2700&keyno=1',
                    price: {
                        value: '24440',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 4398,
                        name: 'Регард',
                        shopName: 'Регард',
                        url: 'regard.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 16481,
                        regionId: 213,
                        createdAt: '2007-03-02',
                        returnDeliveryAddress: 'Москва, Волгоградский проспект, дом 21, Подъезд 9. ПН -ПТ 10 - 18, СБ - 10 - 17, ВС - Выходной, 109316'
                    },
                    phone: {
                        number: '+7 (495) 921-41-58',
                        sanitizedNumber: '+74959214158',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wzzUL-uqdsI_gfXkq6KjSQ1JYDFR49i04UhdxtBOeSYDgkG_mmudlufYXth_K2H-OxyjR4JEBtROjZe1l8HCYyrNikTraP9zt-7rJvtUhf5Sznz_TtZmcv9L7GyQrH7b4Tp6yZ9BhLm607wZSgbvLgs7IUJelEk6_QO0kzqc53o32AedjGrLwsBQzLfF_MEP29Z8-697ET_3v1HBIw6nMxNru_kdvO7oIQ6UCI1DxWQ-_wLrtoC2-Yy4_VOr9vMHte9EoHDZvUIw4xrTBrMIR9wjlDAbM_hgVkf_CkfJODfN6ojOxWz-a_Ewqq6Yp0U_h0JWwly8rWIVuAxZrLbmZhMjS4LEYpkttaEXYyQANamZaxtdgJi7lGAHxlXt-HekSYDvCQnISS44MnRo9E0z4Z6otgBvgehgYXqyLKRxhJwv2EWoVDMOgxlXB5Pku4jjzQA_-Gh9_OfACf-IUwiKrakXfkLdVXUfaJ29FETyoO9Sb66IovINtulrIgk0qW6fcXPoTpSsXrhPGF6djQP_NMgW-dfq6dXzfhFVoU4-XssNo3C4d64C8dumFPrA7y-epMsd-J1yq6YyF9nxhKoi3UjWxJLMH6bcggoaKwnH5KXB4oqYzg64uBNbK2oYsafpujJbPxdoROi-GLR_DvTzjUvoPO19hWFvrhsXnPRTmvaXw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8YL01euJT_zBYXWBpIGOEbnY2D3A6m6oRqh3ZdSk6VuCJeh79jc2P5WUSbVoWiyipUP1EvonkdBY-tIm8mw2dqhUU94MLT2sh4SSZsf6Hr0zllIQMvcDtb0C6bXgRn7qdYyqnNgR8iHtNouFTnFs64euheCqhXNFcQOR-rz6JEug,,&b64e=1&sign=ecd38d8f2ad6496c51ef5fa669dce430&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '490',
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '490',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 490 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/169660/market_sDjir2KgeIrfXPtf7TIDoQ/orig',
                            width: 701,
                            height: 466
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/172627/market_GBWEHrygr3V-HhxnIH0rRA/orig',
                            width: 689,
                            height: 562
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/231203/market_2mP7zTX8153sUjYQYLJjMA/orig',
                            width: 579,
                            height: 572
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/208469/market_0Sl0C-JU-otsHb51e8gGhg/orig',
                            width: 701,
                            height: 476
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/231203/market_6QO9MzGbdsoINwyyAc3gJg/orig',
                            width: 192,
                            height: 542
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/169660/market_sDjir2KgeIrfXPtf7TIDoQ/orig',
                        width: 701,
                        height: 466
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/169660/market_sDjir2KgeIrfXPtf7TIDoQ/190x250',
                            width: 190,
                            height: 126
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/172627/market_GBWEHrygr3V-HhxnIH0rRA/190x250',
                            width: 190,
                            height: 154
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/231203/market_2mP7zTX8153sUjYQYLJjMA/190x250',
                            width: 190,
                            height: 187
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/208469/market_0Sl0C-JU-otsHb51e8gGhg/190x250',
                            width: 190,
                            height: 129
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/231203/market_6QO9MzGbdsoINwyyAc3gJg/190x250',
                            width: 88,
                            height: 250
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '921-4158'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '21:00'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '6',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '18:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.68936239',
                            latitude: '55.72641414'
                        },
                        pointId: '161345',
                        pointName: 'Регард (Волгоградский пр-т)',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Волгоградский проспект',
                        premiseNumber: '21',
                        shopId: 4398
                    },
                    warranty: 1,
                    description: 'ЖК-телевизор, 1080p Full HD, диагональ 43\' (109 см), HDMI x2, USB, DVB-T2, тип подсветки: Direct LED, 2 TV-тюнера',
                    outletCount: 1,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/7m2M8WQNOIPjMEJ5sHfKbQ?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtXJ4P7sQXpsIq6ajp6t3f0uz4NmfKj8hLVbVAEEsPXcv9fVR4SY-vgYwJz1TRA6pZsbNRjPWfkzgHqah2CYkrUlLpOXMKKcnbM8ToHi08ogt7qpQ4bQSVJG&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZEg3lOFY9mJ9QaMkxF-KG3g95LuzFsNs8wQ6UHV3g3Gcg',
                    wareMd5: 'x7pUUYsD3sCsS3lmfjmOhA',
                    modelId: 1724547969,
                    name: 'LCD Телевизор LG 43LJ510V',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wwdo8YeohV7pt7tKAvWGWysE4Wh89l5FBhdMZ1BQJkun3X2Wmc_tzPetayIQDph_TmLTccOnAprPK0iIejviQmMv4RvaVbhk5BigeRWTImvJ2XGX7eh74U80V9Y8UGclIu7TngUtY6Bxg9ybSJCBNo7uoow8RhT7lC0HSH1gv0fxV1sACV6D98uHUBpRelIVRajLdh_s2mUDtuO5PznVrpYn5GNVdt2ayK_NXhU7gZ_ZK9WZRBPPGMXnrd9f0YbksO1qTKXaPp1YuTVcVBzMuE2osUkriaSFf0To-Uggwj8BI-UjF4rlkntLZmkuAiBd_MDaiHvRtc83oAkQIKp0vtdcAyX8bjCJA4W15rNTmBwJvVXsJMQ4RufnnngNi4YGQIGG_WPtSy6pxWX_0f5yyr_YIdUcKLzUIeZoI3k2_KZoiRVu2xqO4SOpnZvqK4fob3v8Q4KxRzOxGVuDGfSvPJcLLV5bH-6Bvl6v3Z7yum3Ym4g0dBXHW9zqFrGwWb1ANoIyS7bN9dA5-GJbWXGqOch2SlN2dGa9_GNKM255tJpHOEDcfol-wFevOYWiRvZZPwx39uDCCijRLbbmnalGeHxl7983frkmELaWRTJFKaxaX3BMHIPF-RwgNjoGPurAanlt30GhIJtuToEYlfUqUyWL6s0cAejTSVT1oz_0YnU4g,,?data=QVyKqSPyGQwwaFPWqjjgNsi4Ikop-ukXjlZHnfltDyWvcrX7Catouc6OfwqmPfq8B5UrkV1aRBsRKCIHDFCuNONoB_8HQGZkojQ0cKJlUY7JJVOEijYzBYTqD_EEV4_ONZGI3vzz_d2GeI8CyqJDdb_lGrJ1jFuLU8ud1RkcQsA,&b64e=1&sign=1b4a50cec8eac18820581eac4c61ed0e&keyno=1',
                    price: {
                        value: '20930',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 4158,
                        name: 'AS-Video',
                        shopName: 'AS-Video',
                        url: 'as-video.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 778,
                        regionId: 213,
                        createdAt: '2007-02-06',
                        returnDeliveryAddress: 'Москва, проезд Серебрякова, дом 14, строение 14, 129337'
                    },
                    phone: {
                        number: '+7 (495) 766-32-72',
                        sanitizedNumber: '+74957663272',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wwlUhm7h3Pd6OF6mlEFYacOJgH4CkDro518E_3nJ_rUmhZU6ZYsZRT9276c1vF9-XCSv4l8WbFvugTSzA1AViOl05iIK55fSZPOuCsIhaSBgnFjeSkkGAcvjqeivN9u2PcXJuDchDBFZi2RTVwRFa2erDkulhOB_hvANx7uBQMCeTDbT736oS6uHQ-fEElwX5gLx0h5AAzGn81WUCNBE2h2w7PJrV8He9gdvA0vSgvkJxNMOb9GYwTwzyUw-5DdoP0t5V2AiWksRiDuu4Nf7U8xHYmfdJHoDg4KpocPqwdGbWBmUbxzlj0QHsXZ6EXD0Xo3h8LHCHf2bztPBY_nQHgoGgbo8XWYcix-Eljtkh6UxaqXj1oCOKsKFmPIAsw4DOzzwjs2JHGQv_n9LtoQzQRIDOlA2lX-5EtpUHgjF0-HLnVGoIxLB3M6uV9hU7RA5Sk5E_joDzM48mcP6SMmqNuqaIVTdK2Wh8nXTqBa8fKkh9HW9E5MnXoJ3WZF5Qlmb3a52qbO8XgAxGUXMcGActMl1DWay3zxWc5VKX0rYlXfF32MVbWelFXOXcwQIzia7gM3aLWodtAZQ3jwVcBAWqeLz1w8B6060PB3jP5Q6b5bei_CF9M-Fibr33q2jY4rLFzMNspIyEw3MLQ0it6nXyANvkEDUYczAnwrFA0x2VChXw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8CyOX8Ho11TSkbrZtx_dg28giOfle_qtnGOpipi2SG8M55jqsaywl4MLNHXw3PiDUyhxOmvvYMHT_4mKcBNW2nMLA9Auj-x5zX0-qE340d80NmovGE4zqyCvJBYJV1K83W45qA9CqD6TdY1Rm4wxZQtvx0oqFzP7rt_x5WiU3WOQ,,&b64e=1&sign=95f4e92e46c5453079edd73cef52de0e&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '700',
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '700',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 1,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 700 руб.',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165151/market_9itiKsqJeTGeVbyZ1IuCpA/orig',
                            width: 701,
                            height: 466
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165151/market_9itiKsqJeTGeVbyZ1IuCpA/orig',
                        width: 701,
                        height: 466
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165151/market_9itiKsqJeTGeVbyZ1IuCpA/190x250',
                            width: 190,
                            height: 126
                        }
                    ],
                    warranty: 1,
                    description: 'ЖК-телевизор, 1080p Full HDдиагональ 43\' (109см)HDMI x2, USB, DVB-T2тип подсветки: Direct LED',
                    outletCount: 0,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/x7pUUYsD3sCsS3lmfjmOhA?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtUvl6BZWGsb05hVhrZpAeK3S2o7lgEKJF-zeN14z5_19wUjpkDOBck-oOgL-xll2rKTRqpLgYuciuWalVxbACAIYg72mFx_SlKy8ZrrIpiYbZJfA_v9u0Z2&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZFz89cRD1wJ2pGYWEL3FIzlfxsVuSRminazSupdH2VoEw',
                    wareMd5: 'kvq9ejmw_RqbgGav55XD7w',
                    modelId: 1724547969,
                    name: 'LCD(ЖК) телевизор LG 43LJ510V',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZRRrA4uRtrCGHsdheCUJfycCHmxkikqyFDQRcxqWFhemAw9PbBWr-e0u1Q_RjLGz4Bsy6Hj4uxfiTrdMzNkQiSkZ-gNKOSQfthROUtMOSrOAabntuN7hDaEQqdM2foo6ZnnOnz6PGiPocKTYoUPRGSCw---MY_mwzh7wXNTzD2Dfavdc9e5foCyHN9lrn8Fqt4lYpeGJnVZun3Wo3Bq7NBf_lqrJyd_mnbnl5PSuCp36DGq1jUO4YxROrT6p5bGc59B0vaurWHS6G0rHgQwkC-Dd2S9UAc_iL4rgArFJmXF71SytvvrM5EYqfmd9c76sNPJhneQw1u-zKIfvupzcB6Qxh8AR7hYq71kxtxW0LiMU5PcN6X0X5Ui1sD16Z2z0nb-jGCls9ae9qpcv9JlldmjWtizgIgntLIcPaS6-6EgRNR4xru_vkjrDG3-GMTkQTErtV05fbfPzPar9fKGE4DIxWckpAawMWOR4Zab2Hn6CsHeKYtnx4ev2oEjZR6vtLhAVttQTV4kfhX7Le_9kQ_lCAETLHcmV4qwimxVGyD6HpWs6Sbe8y07KtBHuJzr9TOx76d62k3EhnulHfavgY11WVN16JcyXW8ZO6bDsVgIGPyOAapSJ2nGHTeo5dqrSkg9HCNKgRtDn5mgxrfEnRm6GL6QWDLdS_v2GBFdNlk-F4TMJLDLuOu9EZY_-XN6C1ZlZXhCDYQB3O1ACckdwZBU4rw5Bq51YH--2IWwE8VSFS7caW_Gbm3UODW4eltXmr6FcHE6mYtpv3uih-6chZw8Q4SpnLLsjocubGsBF36pD4VOAj6AtdS6toG0piiWOVrSfir1SYRYwQduxSQSJzxC-b9zrd8i78bEdPGGWAGo?data=QVyKqSPyGQwwaFPWqjjgNvqZIqElaZiAYh7f0Kl-e7LqtCBPCunI2Xl00yyKnEhko67u-CfMsreu0YsBe2nc0TM1eKkPEPTdo3jvItzslqYOYpEbxZ_DJfKt8BUkv2G5MSYKs2-Nts0v2ilqTXcsDyiyH4mxm5y5&b64e=1&sign=223ba7441e850962cf1b13b0fd16f6a8&keyno=1',
                    price: {
                        value: '22946',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 71538,
                        name: 'EL50',
                        shopName: 'EL50',
                        url: 'klin.elmall50.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 23127,
                        regionId: 213,
                        createdAt: '2011-08-07',
                        returnDeliveryAddress: 'Москва, Ивана Бабушкина, дом 17, корпус 2, часы работы с 15-18 по будням, вход с левого торца здания, 117292'
                    },
                    phone: {
                        number: '+7 495 150-20-19',
                        sanitizedNumber: '+74951502019',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZRRrA4uRtrCGHsdheCUJfycCHmxkikqyFDQRcxqWFhemAw9PbBWr-e0u1Q_RjLGz4Bsy6Hj4uxfiTrdMzNkQiSkZ-gNKOSQfthROUtMOSrOAabntuN7hDaEQqdM2foo6ZnnOnz6PGiPocKTYoUPRGSCw---MY_mwzh7wXNTzD2Dfavdc9e5foCXG3bM-Q6oIGYrjZMDaaXEd_yKQmXMN65umzVTDnKTQ_cF2wYC5_njmRQ3uD1BvuCRkcpg7NUm4Fh0RgNpL8wAUJDvSYO_AkKMT-YXuCD2Yn6ps9lUEe5rgmR3JYpXXkEDELx76Qaop6gRrmYtOlFMZUhYLOHfGleKeoAxIvJ0lqCx7O3_IAtwdWY3KxC75dGvP6j1X5FjWyjLo-wIrqZKak-VxovmevoFqdYL8_Rn7Byvbv04neagxLLHtlD0Fb5MwHqzQbpT26ouzjLWHQNYuVVVEswVa3VfLPI2L2fJS9orR0Lh4U-No2E4RLC4RbWycpEScAmLDdoz0400wexuDCngi-Txg3_ZVNDPPOtruGohTckHHl0yCSLTSI_8t1jCSlzvzRt00o8pRm1wBamtuPYQ-gRmJmdK7cqrSyOC4mqVQNZ0Oj_dqL2wqpDS-ZCSSTm9p6p9_Qp7eNK9or2VAnw3A79OdAT5z3FVUSlFEoEkB9wtnk7dMZsctmBIats_6JySdR9NuMvrfni4_W-l7cMbGLohTKCKKhCI8PVkR3zotzQKpAlaEXpypPjxrRBnRYGeAaMQNmcX70govQu930wtwko-gXtbs4WeNMNtoLoIRyTQgbB2aflPZmzx49xUaWw4FxWNFjo_Yjw8UHtAcXWK8wVFEBcmc05q16J0fSCxJ2Sk0YTi?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8jtmlEOFbcpkBf-os3M8eLvrV7HnM733PAdSTt2r5PVlseHDMe-Ycdk38qrQGm4i-oN1qECcV56lepuQFDZBqlUcRVjr5CH7rPv9nPhCQxMuEk9RoPpj8YpFw-v0DMKfCk0ifr4kGEpcH64woFcre5U2sm4rjPnTCBNGGFq51kgQ,,&b64e=1&sign=56918871a3064578e42754aff90995b9&keyno=1'
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
                            population: 11612943,
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
                            population: 11612943,
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
                        localDeliveryList: [
                            {
                                price: {
                                    value: '0',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 2,
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
                            url: 'https://avatars.mds.yandex.net/get-marketpic/239999/market_W-Zka_T9qSWsx9ylSE7z8Q/orig',
                            width: 701,
                            height: 466
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/239999/market_W-Zka_T9qSWsx9ylSE7z8Q/orig',
                        width: 701,
                        height: 466
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/239999/market_W-Zka_T9qSWsx9ylSE7z8Q/190x250',
                            width: 190,
                            height: 126
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '499',
                            number: '348-1922'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '6',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '21:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '55.68422381',
                            latitude: '37.56223558'
                        },
                        pointId: '574340',
                        pointName: 'IXZT',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Ивана Бабушкина',
                        premiseNumber: '17',
                        shopId: 71538,
                        block: '2'
                    },
                    warranty: 1,
                    description: '43\' (109 см), FHD LED ,PMI 300 (Refresh Rate 50Hz) ,50Hz ,2.0ch (10W) ,2 pole ,Black',
                    outletCount: 1,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/kvq9ejmw_RqbgGav55XD7w?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtXXVlvVCxGMg2NkbHPfbPDY1UDmM7xD1Kn_3stCfF029vCvTkf6AfNh9MoNvpiL1YpxfopuiCWsbAGxK7C-6WqMBubr_rS4TXiL4niGnUNwpzvrWze1z_96&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZGbttFBhPGjyzOywQPY1U9IbM-tXXP5fLCqFjprNl-hzw',
                    wareMd5: 'g-9kQFikAMbtljGOufbnVg',
                    modelId: 1724547969,
                    name: 'Телевизор LG 43LJ510V',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wxPFjwKfz8zvHS8bSXzn7r_-1DWeGLmaJJkTYoWo7rKgNKPJRfULokNn_P9Xyz0-I-JB53kAT4DrlYUh_aZ_5AUt1P-fVCbEMZAaALcXvvDWI4UG-ZJfNJwJRl41qKw_wa2iHWjlGryLyxMpNldPfVea1C7tKgh7h4Yuji4PR37Rd442ONL0DuJBtJNAckjQwlFsuav0AgS2tl7ZpC7MKl-AHHgC9BNMeZJuc-wlBkSY6aj0LwZcwLvMee6StdXnuoIqVlDA6WBbgVTBS17ZNlMyEWHlIM7bDgmBVBkgCTWTq2hdRvG7RgNHkHxDvzegtpPNfKMbDs5R3_NxqDPa_k5NAGW_XXRegniPX_ziBiW9qSfq29P0anNUA-9UPT8hnJwJPBg6dPQbKrhOzGAlEizYxZSJvKreS6tWVy9PEPluc7m_LMCg__P9cwyF-i_q8IXJ1eaLfySblmNTOSFK75zGbPp-Mghbn5Z6K2Xjve3xiubh282JY7MiNC31CnO9ezBOICSGqrE1jN_lSznKS27prRKf2zfiKS0m8aOcbFOaRV8AloBR2Rb-t6nhYip4RVhQjU-TvG-hq8ivSlcibPV2X1BVvNCRnMBTm4EHPMPwfYenfXiOLKEnwNNHJux3wb-Bt1A8IbleT6J8FfCSBTP_a0ufv4XG00oUIOIXgsRLmRj9T03eWr5?data=QVyKqSPyGQwwaFPWqjjgNhHw945oZUy8KGZ3d3YetK0PTaY-c5yxZfRWi3DcmYkQ4UykqbiDf1i8ORGzoecPlmbASQRAiqWdVL268R4Y4n5Mklc32gFIiPOV-SkiiY267srEzRgwLK9U8msuKOOtPB6-4jatqFIG&b64e=1&sign=af5b9eda2d619dfbf8d1ae00c0674d05&keyno=1',
                    price: {
                        value: '23849',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 431516,
                        name: 'Online Центр',
                        shopName: 'Online Центр',
                        url: 'online-centre.ru',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 2,
                        regionId: 213,
                        createdAt: '2017-08-11',
                        returnDeliveryAddress: 'Москва, Сущевский Вал, дом 5, строение 15, оф.3, 127018'
                    },
                    phone: {
                        number: '8 499 394 02 34',
                        sanitizedNumber: '84993940234',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wzXi5MSoA5BWJVTZ_Jy9_rquKEH8_MMGkSDSc9IYZNI_oKnsCjdd07NZuEUMpvz46PKV6Gk7D6nhnATgi6OxBubupqjI8cEk2E8lK7gFaY2HgL0jvcIvR1xOorDNayBW7Tvueoq5AJ7_R9VEzsHVJPh8NVzq9yVjWeo5pIaDE66xPht8fXXHxhL_tGJlp78XrVHySG3m9ShL-gvxQMUIoy_ut-JNGXHuLJabTmrI5885YR8j1tymmv_DiofMHj6tDWPecIJbInYEtU9FJjDt9mcmS_bvdJPckt0LTIlXEmoW4lC8cJMWYw7uiR7_8qcghJz3cgnPmHzVnkQlwyjHJOMewS6ZcN8qfjHF5Uhpt5XajiCq55q2hjzvm7XeOdDLdZIaSBNjx5Nb3V4sZ1gO_lFKaPgEyM-QF9TkYAQvikQu8ddsWxxgO81RqjkahlP4fapZZhZ1VZR6XdwVppuMcbXQj1j12c7CO9wGqIuyuiZQAwuPkHYracW6-hb4l3xgbdSqiPnCBFJd8x1P8xvAGCrt0fHvXjLFGihQWd3hiKf0l7ptWMWPymvmJ-eJDzqBdkCwk-qnq1ycJBvMCzwWyqWOK5v3DdaPzA9WVeSwdInHdXpWMmatrnyWQuD14yk_D9b7AzDoESPodBu0W8OzuOofiankguN7-kLP4stZdgoYO-hx7YzOLNj?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_SDq07IIbL8wKFYnM35KV5GyLmI4ANHvY_5b0OlVGAvfln0E-racJ4QKm6riz_qcxR-nSfMvGeeKhiR4hnSn4ZFsO2ZAzy8WW4KxJXs57XXT4dyKr_xT_zKc1KqWjYQh8dKZ3Zg5qpZyiX58Io0O0-PhKXCx9784pSSOTWGWQIzA,,&b64e=1&sign=97c49cdf441e8f21a3a7088a038caa8a&keyno=1'
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '300',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 2,
                                orderBefore: '19',
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
                            url: 'https://avatars.mds.yandex.net/get-marketpic/205818/market_SaHinDPwWj8Jdu-QmcG4SA/orig',
                            width: 600,
                            height: 600
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/205818/market_SaHinDPwWj8Jdu-QmcG4SA/orig',
                        width: 600,
                        height: 600
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/205818/market_SaHinDPwWj8Jdu-QmcG4SA/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '499',
                            number: '394-0234'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '20:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.59462694',
                            latitude: '55.79553028'
                        },
                        pointId: '1538282',
                        pointName: 'Самовывоз',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Сущевский Вал',
                        premiseNumber: '5',
                        shopId: 431516,
                        building: '15'
                    },
                    warranty: 1,
                    description: 'диагональ: 43\' (109.22 см); разрешение: 1920 x 1080; HDTV FULL HD (1080p); DVB-T2; DVB-С; DVB-S2; тип USB: мультимедийный; VESA 200х200; цвет: черный',
                    outletCount: 1,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/g-9kQFikAMbtljGOufbnVg?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtULYD37uRslYx6KLvnngpM5sAion95dG3FcS8YEIgDnufR0826nA7c5lUg4ox862YHLozXAeuyAsL1ZtXQfxCJLn6ePqVE9xHpLdKUARjTTjLr_e_cI8yjk&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZHs_dNbxRvuplLX0lS3d5MN3i4gUOS20ckhso1LjOW0aZfpzUH1IKtdCvbEZqwcG6ZLgGHOfcuUPJzjJOLq8Y5S2u86zlqvfc6BlXszeFwfwvTDq_1Lrq09opBUifjVmwNzH_9wzVV3KgtONggaCPWucyNTxTRnC5QXrWW2_qpSrOGReMUrNo6MhxnBMbmON1M_HRJ5oUDO3EUIw69eKwIKmgsBEZYSE86pEMYheUNwkKvHXnEcaB3Ig95ACvWYpbEMUQTW6SX8dapTAY7ReTviR10c3WsnAG0',
                    wareMd5: 'it9bvq_4Kya68knqNY_prA',
                    modelId: 1724547969,
                    name: 'Телевизор LG 43LJ510V',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZRRrA4uRtrCGHsdheCUJfycCHmxkikqyFDQRcxqWFhemAw9PbBWr-e0u1Q_RjLGz4Bsy6Hj4uxfiTrdMzNkQiSkZ-gNKOSQfthROUtMOSrOAabntuN7hDaEQqdM2foo6ZnnOnz6PGiPocKTYoUPRGSCw---MY_mwzh7wXNTzD2Dfavdc9e5foCGQZhN_AM8UxZS-tPIFrC6Zd-mBsI8iJJr6Cgvp7tQ5aoKfSS4WEDUFkzmChd2SwuVni4yMmpcpMjE4ybQNtG0Q7IXlK-984HHreU_DnyIhEk_yCP5Ajh-9Ph5swcEatFLvN8jENXV5MJX-rmPv-fOnyu3ofaqxCBMPB1sR2uzElvBhOrWm3DtApBRXt9UGPfgqShOpW6nPEGX8pdi_3ZCHq60_-tve7qY9kHgFYE470ZkLhmBqNyR9TeQe_MW90Lz0pfDPLN9mEvNFbkzQqz8ibDsCxDLDt5-BG3-UKsCMLsVOnDlyoltSSGsQTnaR97KMxlUTZA9ikngbYpmgkc8_dT8M0V2Yz2NbI2F7iR2qvbrAgB4XSqzycalpTndeazQyFh0ZwvfycV79V_UUOhAzoC_GH-7YOOPEAcpu7DWK2-2w7xI-uv782sURL9GEXh7YkkhrJmj8dNdbdGU-ONqsUYNSD_ihsY2eY1DfCca5dLjzNLCkbPqRmDIBku5o_Yp8kWvFbriyPSwbG4I_dZKTwMJcIZN3M7ZjkH25tUPm6_EjlCZbrYc33sPoJTK7mcmz9RSiEskI_ftGapIqpXO3DzO8a1uIqhShZq20O7sxZHXdYKRq2oq76MCe9P_olNmLTrbgfTU8kg-Pr5ElpQiquR-JQdRUNA6T5n0sxzlUF3N7fQYhgOM?data=QVyKqSPyGQwNvdoowNEPjXwQWWrc6tY3WM3wyyPmTagp-SZrQA2-N-6SAuosg5BIojEt_iFgkbde3S6Se6KkWDLWKML3Pcv6lwuBq_haPwC2Nyd34Or3wWNGyJYCbrdCwe8uBCfRAEQFtFcldlcW6mkjRz4vVS8Ar_x7l1OHMGu2D5Rne9p5G9CPSZdoBZnM8YrUdPAk5baMtHv9rSHWRgiAVHV_s9pUGrPMTDatOhD972ZmA72JOo8KnPZzILzYkVE23W0kqLeJUuFm3fu3W3WvyYwFprVgUCdO0Hu7y6GUfr_TqhRlM7xaUDw9-sjgFqNwsEDLVU9-wX7kxxN1miBgWK7D96BHejcWYoLn_8BM3x5GK5xYEpjBs0ydZQHMLgkif4GtxfL42hSe3t6sYQ,,&b64e=1&sign=ed14574d46acfe9713de7c5429140982&keyno=1',
                    price: {
                        value: '25990',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 317377,
                        name: 'DNS Технопоинт',
                        shopName: 'DNS Технопоинт',
                        url: 'technopoint.ru',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 23092,
                        regionId: 213,
                        createdAt: '2015-10-27',
                        returnDeliveryAddress: 'Москва, Щелковское шоссе, дом 100, корпус 1А, Магазин Технопоинт, 105523'
                    },
                    phone: {
                        number: '8 800 700-4-666',
                        sanitizedNumber: '88007004666',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZRRrA4uRtrCGHsdheCUJfycCHmxkikqyFDQRcxqWFhemAw9PbBWr-e0u1Q_RjLGz4Bsy6Hj4uxfiTrdMzNkQiSkZ-gNKOSQfthROUtMOSrOAabntuN7hDaEQqdM2foo6ZnnOnz6PGiPocKTYoUPRGSCw---MY_mwzh7wXNTzD2Dfavdc9e5foAfhby5MbUBYc9tIKtRuH4Cozp3_czPWe1SAtHa-GADEzaGWh-PEhJP7wnqVDpLBNaKw6gSvLCazRegFtgqLYXRy2NFGKYT7bPXNQZj5a95w8xn016GFYjMKEuK9aFbsGdKidTXQaxjZZADepHR8kK2aWxUGfk3zYvKrhMkHshlFKFYTLEhMwrtB2zuL3OAG8gS_r2nskt3ADhqNXrLGNSZ2hJ3I9ujAz7YrqIzI4A7WeApBOWRx8jYnmAKESfop2iWK-VTofypOekUbVRSgpfJ_5bL_D18K6bERRzIWybCajenr0ENddhWhEBL_QfagVLCep5lMnYuQOnRNzdebPMN5MKOkEbFrdTROVDLCAEp_61JUc-gLp7rU8MZYn1AVrnI4WYEmKxSJwkMHqFxXQkaFTPmj1foAnloR_ybkPIOt_fjbtYrZC2KOYEzqnzXAfYQl0Y-7i5auMJXBlvPE2pb3eR5oCYcgxPPireqmwkdx3xJNMusfhpF9IGvKFBhneHvjCy6alnb22vwY06AY9loNb9Wdtipa_i-GJ5Dosa8y4WHVL0gFbMof3PbsSEi--V5NpMrja2lzWGQJxITIr0McrSW0H13Ts4j60C1qeJo89ow8JUiDAEnuVGnNQgK5nnToLBxEBPPbArAZlqDc3HzpGfn37ffQzjOD85_uxtRZ0gtUPOTAC7G?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-M_frCu2J2vGfZelugGbOccxNExQQG_52sZQs4gndg124uAXhfBkrl5BcP4TMI-jWVorfGAcfRCE4k_5svFpDAKIZICEmn5c0kCgUacY0Y5FlpQFnF6E4r3BO9sHY_MxsmCwh6oA1Eu_jSmEPqNxJArHlViq-LCopDd69mUdip-A,,&b64e=1&sign=54e92cd0ebd667e535644591f596519c&keyno=1'
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
                            population: 11612943,
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
                            population: 11612943,
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
                                dayTo: 1,
                                orderBefore: '18',
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
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165839/market_EBt7b4wPMzQgF00upECyvw/orig',
                            width: 2000,
                            height: 1326
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165839/market_EBt7b4wPMzQgF00upECyvw/orig',
                        width: 2000,
                        height: 1326
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165839/market_EBt7b4wPMzQgF00upECyvw/190x250',
                            width: 190,
                            height: 125
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '800',
                            number: '700-4666'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '22:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.833357',
                            latitude: '55.81257'
                        },
                        pointId: '532274',
                        pointName: 'DNS Технопоинт',
                        pointType: 'MIXED',
                        localityName: 'Москва',
                        thoroughfareName: 'Щелковское шоссе',
                        premiseNumber: '100',
                        shopId: 317377,
                        block: '1А'
                    },
                    warranty: 1,
                    description: 'LED-телевизор LG 43LJ510V с разрешением Full HD 1080p отвечает стандартам высокой четкости, отображая на экране 1080 (прогрессивных) линий разрешения для более четкого и детального изображения. Функция \'Virtual Surround\' создает реалистичный, объемный звук. Вы словно переноситесь в реальность, где ярко ощущаются все биты и полутона.С LG Full HD TV вы можете воспроизводить фильмы, музыку и фото максимально удобным способом прямо с USB-накопителя или жесткого диска.',
                    outletCount: 1,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/it9bvq_4Kya68knqNY_prA?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtX50QNXOs3hkXVvr5RHlHWLlzQIrheeRZZhXz8JRgcSvikTGspYohKW3b3WdUy2w4SNdfpEKxoc4KaO_ZzLCTDZZstagDBC-CsBaD_nGTb05GTkYwcTW2lr&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZE2C3hl8OqEI7xvFx-Yer0nEOVBYfOXV0Yo6dFI9LtZJNALhN24qJNhqckENRDcjnCFEpbnTDBP2YgHifb-QrDO--nrtgWEzHIxSUueJqjCBxV1sP3qYjrBm8LEFArzxkZroIeL2AjGs7R1TTpQcmEwOWR_Lch_ywi684xksMYKrLj5SGwzVH14PAY3S2-CLfAjc_gYdrtGYE0W17j3YQFLXCdcmzOWk6PoaK1RDPqcjsmr0tUm3-ShofSILOPrjFl-9Ix_csTYctGc234ZxP5CmA-fP7ya8P8',
                    wareMd5: 'kIf7IBU3ayqJorut04wJag',
                    modelId: 1724547969,
                    name: 'Телевизор LG 43LJ510V',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wygiHLZG2GuA4oB6ufuuG74Qn_ibCvfgmw7BP1PQpo5oFHBaj8O6M5nFFEOUMHSu6auqsiTaCcUvHpqW3qhPYApOWPhT769aTCU_H0lN4bZosL-8bs0QOXRQLLL9sBBXXqVAPoAm_CrgV1ht2yOwGztGrAaLddxz2sZ8rO3ohX8L464G1PYmt1MAjBKY_KNmIxXduyncARH_7pMKg1gJ9t5_Etb2VnCDbZSRPS2ozMYOMY1c-w8VGC6mwaUNwa2Iz0GT6Yi2Cbb8F-DInLy7VSzQ19Q-4wsNNizV53wLCThKC8qzugunZEY4xC1KrpA0ScYvxdsCBkSuRrHkf9z9rlhrFYxJsipkC3Nlpo8VuJBd5wjnanwVpr_cLphWXLRCyZKEP1THnWtrJz2rVw4BdY2EgQKx6bi5BNy_mcMHlt-ThZ-nEwgGS4tUAk-rCqATTQj6kDffpK8Z5z51OPzs04bshSSUTvTkCua5jNZ74TL8-1LhyWAWuGUiBCvmp3DV_U3UZE4DFlUKlIr0e9ad-lGN_-Gf5bGRvyF38xqsclzcnvwbPH15xSmqVBx77orj7bg_k6LeFTJLteN2XcqLTNuDchiwDBSZvQ7jqmDWkJklAI_12OJsKFsXXbe8rXEdtnZgcgRDHI8DvL2RovWuMt8NjoqCNqnbssUaRNqf3K0kGS5DbRKX3Fh?data=QVyKqSPyGQwNvdoowNEPjb2K80POlrmvaKmfEQCTIap6-Ab-BiftmHkZn1SnlI4nTELSfUxnq8hz4C6Gv9E7w1HhPPt9blr19yTueaFMBfOibgfVHd8ed5e5YbaiV3FJD6OZ_Pok3IeWTvoUNbgu50kQWYEAJ1xdCyxii-44HpccY-Fyv4jYFuBUx3x1uJi_kzwkQ7dWVdr4VANKlc7F977JAeT-9QcuaPujcLd9Jvw,&b64e=1&sign=cc327e282903aab7cdd6c7a1c49a975e&keyno=1',
                    price: {
                        value: '20928',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 720,
                        name: 'www.Pleer.ru',
                        shopName: 'www.Pleer.ru',
                        url: 'Pleer.ru',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 108780,
                        regionId: 213,
                        createdAt: '2003-12-18'
                    },
                    phone: {
                        number: '+7 495 775-04-55',
                        sanitizedNumber: '+74957750455',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wznhzdrLRArOo0jZhg3Gi7KvuARZvRnjQ7DruhIacGu1Z_EZkeQLGsX1W9BNtEFb-lhF6RxLclA-lPaceYNEg_OLekr8rwGoq4VmTXPJxkadRlS_cglT_i1V8Rh23o5HUMVaBYiknD4TX8gPCGzYNoxop-VE98rRpY2q8QvS8UFFMCKqC8GEM18DQAkLnl0bGdETagsBpFaRxnZe4pkshc1Vwqd6xrbM9Dodz1g2LfKwOIqgloLL6gXUR0cLwtQN5y9OXVTqsvpYvuXKvhivAry3jTN7WfHZ95YJg-C3DRC-f18J2eDDs1MPplgq2QJMrPBu2hpMadqn96kJ1PjnvDU7fcZd_zAGTHoRMSg3FNr1gnHXTBLxSRO4uTn7vEQ2xEq-l8IWLidaKTEPG2UgZy67t4HQQJOBZ8FqJiR6YIdR6u8Qv1EBRqaQ4yqJ4XgmlTvdAMQ-6ruhP6Lvjc-eLn0grd-3YUhzgyYCy9Sssx0sL8gj4jECXX5_aJ1ByxEw9OF1GSFDrFHmcDbBDzo3q1_SUZjYjFvJNH8XU3nMWaPzS4enSUSFdpAU2FDBQPvm_fZKHhZCNcskgU7Vu9loCAeWsqKlwufHs3X9Kno-ZEzoS6h5Bry6yvZnTlhKtkZEJgeEm9hvB4w8JCbk6hvnOt89RmlccCzEXOZnhXOiiMfX4dpiz3oc1SZ?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O90hf8a1uQzlIPJqwUlqNpIiCska4ekbzGRZ9wk3phpf9YZAzDR-cBui1bIQNZ0YANXWC4wp563C_9cGC_RH8lzZGq-pSitjCP2gHMVKus9V27Iesl9rBrMBB-Sk3C-J2rI2IdHI4HjAvJry_JrtmZ3cfzd2oxZd4K-nGNL3MHtTQ,,&b64e=1&sign=65db31eeedd5801ef171bef47a0ba02b&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '798',
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '798',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 798 руб.',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/370160/market_6oTrNu5UPatvX2NcGKIqVg/orig',
                            width: 940,
                            height: 620
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/370160/market_6oTrNu5UPatvX2NcGKIqVg/orig',
                        width: 940,
                        height: 620
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/370160/market_6oTrNu5UPatvX2NcGKIqVg/190x250',
                            width: 190,
                            height: 125
                        }
                    ],
                    warranty: 1,
                    description: 'По-новому глубокие и насыщенные цвета Помимо улучшения цветопередачи, уникальные технологии обработки изображения отвечают за регулировку тона, насыщенности и яркости. Революционное качество изображения и цвета Разрешение Full HD 1080p отвечает стандартам высокой четкости, отображая на экране 1080 (прогрессивных) линий разрешения, для более четкого и детального изображения. Улучшить изображение? Запросто!',
                    outletCount: 0,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/kIf7IBU3ayqJorut04wJag?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtVREjSX5A1la8LiIZ2j6DE62fmS8iGnsgixtmLXLvTrh3qI7gS_79A5s5amgQENefXXCZ1g7e1ENPQkN3H4Rr0Yj0uNWrAMeeF_4p7g7CNJLOSJDI510lFq&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZH5UuFl02Q2ICDVoxu3h3dEoIlSHfpuI8xMewnj0wRz_WrDZV7p-dFiAFPVKZD_ilAsOF1FDskFKx5t96OeGGhWXf8UscFc6YLZgOQhvyHXO-vG1sJD9ZSojzFyOAA8JbKad1-fmrr_L7EnLw0YDUtSgn2DiWbLL4I5dJbd4aYe2hGht31iksmKKyCa7RhpXv49xNrVUoBXoUPKvTIvN9WGaPmF2HeeIzOa6UoRn4NVnr0JzJZ8WUOckJaFqdd87TYKjIsnboA99WcHpm1U8dPlCOqFGb35-xo',
                    wareMd5: 'Z0B2_YadSXseweMkOICIcA',
                    modelId: 1724547969,
                    name: 'Телевизор LG 43LJ510V черный',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mX8lz2TjTlWi3ISb3AsWwNGUlBmML61i_xcoVmSATF2eC-C7hHLEr-6ZSiU3W3vRA06cnJ7ERjVlBvw7iHE4eHbCeg1LX5WyYhZZEfCBXxk7_MQAl-KV8RilN6Ux3OBbHEMNufMfM4YEMLS5RX0B4WVAoDb6F3WIP4A4DnI_4kgS4i8NL8BUAblUu-VJ9qJJEP4YZlbInNIEgGQA8KU5h6NHs-lMwrrmrh883sjoZzmeUdOruMc7Cwu3PoPJP-edecybI7CXiZJqBcMeICWrbuuSgt7TXlOSdNI_dXv8WOdXfiueCJrE_MkyVzQcBESsR4zRSTqdz5v-qoCo3qONAh9mJOhgEOSRsj6ZjmIUuFR5gqr6u2vVQ4PuKQuvvXejlSg699aSoppHTrA9KAQMgRycGE--b1vxvgzjZ-uRK66XmRtYxJjXKSSFc2PLHhP4ictAk4IewkTUulgutce90265U2rGESWHYQrZuPGG1TzYJcjkxWlF_LEzCTHzs00NLC9dEWBI6ZjtngPQCBUcZ2BJhQdHh1YV4ojRhTVPo36A5iYjCYpYkYrAarNFVLzaHeyLxXMSRs9KBVHZUUh0AWsQpab5PRmmKJS07lQi2H07O-C8ljT8D0hQOaBrNvSLsM9LQ6vncctAGRB-bj8RLey8CGhGIXitO6DR-tDorzK4FeKMDGyAuO9THOtPO07CM_oIRKe2k3RwZCdhndT8UHvzQi0CZ7tLDXTAKyrmnh0QIUEXug8EFs-lM5f7LAzxf5XNCwJdXkCLgHKCEyZZSub7n6V1qQZ7U54B5UZiC8vxq1BD4bzqORyFKSW37PNyf3-aabnFwImYCcC4CGhJ_JXsnXbjhpdkJRyUQBBIXIGu?data=QVyKqSPyGQwwaFPWqjjgNuorAXubYIJMR2T7wjaIxUiCnNT6PHMlN0J8JpWhXHU8XddMWpFpp6gnGYGyL0ERZ-7AzyOI_vNs4A6xHmkyugrpNxcsmZFMeppagXhwMep9hTUdp2giS_rfJMjBtfSlY7fzAKy25RBsc9L09GMEwoxa5ErJWdUkABdot1ScJ874KxzlVz02BjtScQx5dVY1v84OmKGJVyr8z5hFv5G5APybeHLIOD9n0UYyyw0nYSLkDyYDAwH4FrR6Ls4A1NzZXK12n-ciQ_zMj2rJKMA7DYSjUv4jQxONw1sffpfq2w03-lM-e1BvvFBbmOEW2JUdfBUM9aeUHobP2v_9O4gV0uM,&b64e=1&sign=b9b6add7909adadad7d8d02d874f3fae&keyno=1',
                    price: {
                        value: '25890',
                        currencyName: 'руб.',
                        currencyCode: 'RUR',
                        discount: '9',
                        base: '28479'
                    },
                    shopInfo: {
                        id: 162,
                        name: 'Комус',
                        shopName: 'Комус',
                        url: 'komus.ru',
                        status: 'actual',
                        rating: 3,
                        gradeTotal: 3932,
                        regionId: 213,
                        createdAt: '2001-01-11',
                        returnDeliveryAddress: 'Москва, Служба курьерской доставки Комус, Служба'
                    },
                    phone: {
                        number: '8 (495) 637-90-45',
                        sanitizedNumber: '84956379045',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mX8lz2TjTlWi3ISb3AsWwNGUlBmML61i_xcoVmSATF2eC-C7hHLEr-6ZSiU3W3vRA06cnJ7ERjVlBvw7iHE4eHbCeg1LX5WyYhZZEfCBXxk7_MQAl-KV8RilN6Ux3OBbHEMNufMfM4YEMLS5RX0B4WVAoDb6F3WIP4A4DnI_4kgS4i8NL8BUAblTpP6Rkt_rq885QbPeP9Y8meSlO1xbUF4NDwJymIjhN_R-2Xx2uAt2iKjIEoq6gpce2sZ0DHLf2EF9IMk6x523dBxba7QPdRK4xBzR98fr0KJdZiLX9JpTnYpLSLOL6g27fyQCoy85IPdgltOkdmdEQzXL6AIlBa8qtPggyfirY9ZVQFA7I5JnkJZ4DBjyHlxYWSBn_T645cqjUSlq0lii-fkT-6lkzj30Ik7UrOBUiQ1XCZqsxktenC1-WRLTnJSJGWcW_h__QRIUF1rNeu4svipVxadRduBJ1KNCEI8Q56FGaCOBidv9xwyg6scF1vy3FkACERCf6Ug2cT03oBk9kEme-QJxMETsFKBPxEU7J9pDWMwdjCgmNS372Ko6SHg5OIaJji10BkeV0BmhnMqYEBTPG_c5RTwAiJ82g9LeJjQXfuQKbP8uReQI2sJi8ovR6UaM77mTZW1qkmq6rAeXjLf1SSxR_JUl9wFjxrFVVAxfr8jwd-KVA6wpMbmHODd850JN-uUiY677vb5p2Y43a4OdtnzH_K7tvAIsbPSbORkQ0idT0Xwg3pCbFGEQMhoCR6VO5ur0Ad5nmC-5Nsx87aU9spNxG1g01pYUBy-cwbvGj7QevTbt36PLaGalK82BW3HTHDU4nxg4zP_oEPsOuYXM2aoB_wv2WU3MVofIifzTRO1mc1us?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8KUwFyTn1QB-4SKUQ7u693KOWU0Fz08D1v2XnZJQmoNyZly4EwtXEuRoJREkbxEPFMZNWPRTf5Vj8KKuqXdmDXSYBd7RQHSLIBE9hJEiQse5iDucmFS-qCYRglDo9JNW4KuQsZNC7MwzGQctBxr6lJER5IdbOwY2tbZiHCUH8fyg,,&b64e=1&sign=8791610404c15059018b9a28fc893273&keyno=1'
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
                            population: 11612943,
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
                            population: 11612943,
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
                                dayFrom: 2,
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
                            url: 'https://avatars.mds.yandex.net/get-marketpic/172106/market_cYyE8caGYGX4mQOHpeUYCQ/orig',
                            width: 800,
                            height: 800
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/172106/market_cYyE8caGYGX4mQOHpeUYCQ/orig',
                        width: 800,
                        height: 800
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/172106/market_cYyE8caGYGX4mQOHpeUYCQ/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    warranty: 1,
                    description: 'Телевизор LG 43LJ510V с разрешением Full HD 1080p отвечает стандартам высокой четкости, отображая на экране 1080 (прогрессивных) линий разрешения, для более четкого и детального изображения. Функция Virtual Surround создает реалистичный, объемный звук. Вы словно переноситесь в новую реальность, где ярко ощущаются все биты и полутона. С LG Full HD TV вы можете воспроизводить фильмы, музыку и фото максимально удобным способом — прямо с USB-флэшки или жесткого диска.',
                    outletCount: 0,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/Z0B2_YadSXseweMkOICIcA?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtX27BK-tHBxmIKqvSg5wMUDAHQs8jLKThUXtCFSFTfiKIscOUQW17aQ5NsRacpGeWmB6nun4ZnSCYa1KssxC72ESUkj0gtvoXyFQuViqlhPFnSrKN6G-IV6&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZFaU-75LzsuN5KQbPyMvJNMYMSM6mNXu3YhM-SntE8WRHYEw7AbxHHCm85hN8Oz5TefI1aMUXPzkr3VUUYVSjUJyzKk4A09EuJ-xD9OlAchMzn-xBvRd_wTDc0S2Lxpsx-A-nXm-m99shtQ6IbdGHDYDtafoLo-RfZalDEyzYiuZtoEfOPMejjhaZ3MNvTosmNvPQ3tUbRLoiUVBfjLoo33ZDrGEE3O1FSrImPuKa3KBVa1Nrv645H07x5zsj5dSdrgX-g0Wx1jWLvZC-BL13FAVHJ50ZNcRmc',
                    wareMd5: 'T9361z_q2GUX8rGX2dnZKg',
                    modelId: 1724547969,
                    name: 'Телевизор LG 43LJ510V',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wyNy-N1xIf4jQEuVFfA91d_96IMaCIGdt-AtDaEw6MephfN-XuqdYILWiBhhnhGxgfmcSeJnxWoRSLl3Dfrjtwiv66XAkyqY0iOozTGVbllb6_D1uDtI-1fIVVMsnDFhiXGqKoIjtOuVARZlxOlIV9w7ut9gD_FTg_uUljhm6_70Qq7HLnG9Oc4gRniictg-lCHEOsmSOGS7ra0FqnU048e8nGDZcmI9N7bRKM7620cE18fLzbQQO88ZPjKJabcG9znJGGn5YRwY_izkb7Orz0e5LTLNOxCOqtcJ5NKTtMYS-jbxoGqUL6g870ktimAGn4CvU4-JewJNzqBnBNXGVKzTUbK0ukr0LaI5ivPHt6ZkVGIwhtJSUHzO9kcwXnEidBa8GZFmuvrDZFDyMhWwNgRikJxJW92yZqP-PILhn-rOTEqnO24mm-W9kjLjwIEHrsqhEAo667eYURyib4rSPB6-IQo8n6cZFXoLas40_bw2t2nGyM7KhDWl1CDh2rbkQ0cUmT-vlYzVVPg0LkmM-sGg1H38_1wH_AQki0w-jscNgDxOIRYq0CgFErHFlQzFm1lzrhkFcOVs9whkI1m9HDSkkrOpxovdVmLiOZfCk3PyFPvO9IFXVWCw-E9sdFUR0siB9TKz1H582kN7-O-l_P08VqyjAPGP9F7gYMwLOHQvw,,?data=QVyKqSPyGQwwaFPWqjjgNicBAb7LqdBz4108cnBv_MJ_EFIqw9Fs5zkRAZcRqGxblNhiVxw7ajlaD4yOppk_s-UcTuBN21znLSJ2AEsUMJrFooiLwVaelU5kyPvcic_zvHaui9Ij7MvXk2uodqOPYA,,&b64e=1&sign=ab451f35f7b0fb1549fd39959e3cd8d9&keyno=1',
                    price: {
                        value: '23656',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 1498,
                        name: 'ATVmarket.ru',
                        shopName: 'ATVmarket.ru',
                        url: 'atvmarket.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 461,
                        regionId: 213,
                        createdAt: '2005-03-29'
                    },
                    phone: {
                        number: '+7 (499) 490-65-86',
                        sanitizedNumber: '+74994906586',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wxH-BN8k_4vcrY_APKKMUbN0RG8aQUeHB7p7923aw6K2_So9L0w52Dsowcc1dbWmaPULwclIQyde6qAxq85Wx_3W6IbKFk9viBtq2-uCIMIKKJ7p7cupXnuOUkZibqXyLy2ioajgQKqr5Lo0pKeYPKqBwSaikF8fXvvA2fduOdC-E0gWla1AlwKMXtgljrlO5Gf3ptypWvayWe0yfmSFiuJiiwkObHdhpQMF2UhTLtTMgUqO4bu5fJ7bP4wOY2r26hvWxLmIhXAVvIhulvIkfzO9opjo79W1stXRzMlx2TUVIk8wNQKcyfXawja7F6k-Rw_VflxmcWM9kRZGk-kSzsjkZmf0OPKQ-RiaR1JGqLApVGMn3Bx8g4s8ilJ7ZXcexXhqUnmvMxiIJ_RbHLiljPAYsnGyv8CNjifJfKEMTrmAwPk8EFlWYLiMckKc7BuemnHERsFEgdWpAFeR48Icl_pyFPQz7h5T662lyjFKOTcjTfav0D3UHRH_Nb6fMLXOGG75viRGQc0BfA2zmNgH-zwRwI2Rkw4CLAgjOvvvOBGUG5BWsdzWgkhqVryvRx1xX9FkJ-meSvZhpnmaeZkAXCyZlsZgOMgVjY_1XsxJxbMPfN3ieAZHbv_XNCjUuek3yTzV4Z0ftGg8qjO1aVsZNwjxytqhJqT6NgkK6Ax3p2vrg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_rMm7KNjEtL0d2x88R8OTFKjSgpDeBSO3EV95O36lw6m6zm8FQ5LDwHUPaBFgee7x0PTwW2ZDKcey3WylYEUyutoM9aT9wg8Rqf5FxPdKqavpVsGzSMChle4b9MwBnqpyB6_BD75_uqwi4cqMxpCxsvSCu0t3vhp6Y9QEh4HfQBQ,,&b64e=1&sign=7ede9790167eb229aadeab60698e222e&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '600',
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '600',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 3,
                                orderBefore: '18',
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 600 руб.',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/373800/market_uSYVmCxBkTVjc073ZW5xdQ/orig',
                            width: 600,
                            height: 600
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/169403/market_DFlsipmFGuRlDy0JicjWeQ/orig',
                            width: 600,
                            height: 600
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/221955/market_Z9_-jeZRghT3itoVhHHupw/orig',
                            width: 600,
                            height: 600
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/174398/market_t1wEbcfrACUdejeFrK32FA/orig',
                            width: 600,
                            height: 600
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/205271/market_pwrE-GCYSeYDyTxLX0bYhA/orig',
                            width: 600,
                            height: 600
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/373800/market_uSYVmCxBkTVjc073ZW5xdQ/orig',
                        width: 600,
                        height: 600
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/373800/market_uSYVmCxBkTVjc073ZW5xdQ/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/169403/market_DFlsipmFGuRlDy0JicjWeQ/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/221955/market_Z9_-jeZRghT3itoVhHHupw/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/174398/market_t1wEbcfrACUdejeFrK32FA/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/205271/market_pwrE-GCYSeYDyTxLX0bYhA/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    warranty: 1,
                    description: 'ЖК-телевизор, LED, 43\', 1920x1080, 1080p Full HD, мощность звука 10 ВтHDMI x2',
                    outletCount: 0,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/T9361z_q2GUX8rGX2dnZKg?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtWkDoOWHXWd0BpVjiyBm2I8yFXyiIlQVfTIwVKYIxyjDvXg47NR2qSL2E_SuAU_EOeg4W8lTKCymFOMtekKnBGEAQqsX579puJvI-30J12XuC4HrsoOnJiT&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZGN_9Bou2XAOOWdUF9HWZf0Q3SRteXQX7YZb3P5kopJ6XiQPzRA4JtuTxbPY0z4tKQNwnZjeSeEvSyIaWsCehscONs2dx22wppgEAB288M7SKX5PVvTTZEwGN3qAODvMUsCD7XwuJYwlrCZ3az3uBKjXZ221I6gltj6jHkhh_2yYgqML8o444H2P9YTLVA0YAlkJZp7oQp3dHjkJcYg6HASfAIqwKmJ5iTkCtKoGPO0Pu9lXdl8JLyyvsIRbI1vqgx8h1oL826cEwaO-q0NDTnYlAOFHYNq3hU',
                    wareMd5: '00v4a9DLkvomO9--msjCwQ',
                    modelId: 1724547969,
                    name: 'Телевизор ЖК 43\' LG 43LJ510V черный',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0wyDhFLdHnvBDKCcgJgXqdexDBZxQk76HyiwYMAOzAJkmlu60p9S3Jl50N3fSMyut48w1X_NQhPCMru0wnn97kc8sdKFN9y3bFsGKhCoIcggdBjSv-1S66FmuMjvACWYAH1TjhHhs4tq-AkC4HecTW5D-Tfx7QQMDANXhaj2Uw8TASqxVFVkTu31AFA1Z9h7WX5QVmsQvcslH59Xwn0A4YR_wzqHeYpjs2TEm3qqW4vY_hVcWgOeA3-DWM7f--YG3guM3K3dTC3KLIR2HDpv2L91UN9ev-FuI6FxmtqAXyPln9W3O4nKuBpaUMuri9UyGhDfsZPVZA9nVv6H0vUhVHTj1n-rSR2x9kA87nzKdPpb7bL90luWFJ6NSKnC6DXADyQOcmJmh_GXo2lEivirfvZdahYYqyw8SfEWLSd6io2dt6RiUG5sU_q0CiO6DGPZ9HOvnbLapFQxqi0s9DZeqGy3QqmqJUoZOl_ngX3HlJOH2OEccYbcu-iEf_nYeTiwzpXk5y2zR1qjHMeNWTQenjuOWRS3C4wXkOGdgyWZDpP-oZjqCVeqxPTbXmQJN-gNqpXSVvEUSOUANCzEpv5Y9xUK91vn_HqUUmGwXfnsCOoeejDcb5bh1VolIZIC3VXMcnf7XvpAP61DpHD9JgPRG7HEpA-MkzlaUlUZJ2hPXaI7xQdJ2G7MpzuF?data=QVyKqSPyGQwNvdoowNEPjY191n2rMIoJFrdGhIFCdUryTGF-tbt634mz7hSslcch5pN5tdePhY4Q7xQ4BNJuMsXaHSqwEDd8KK0M6nrzArSChi-Amkkt0RI4eCHGHCz1jghOSrxuWWsjPlsrONagiXtxmV4isYRLsGWnjMp8laatlrdhHbjvuu9KSTqjlhs1JtP9iEdmWUeRsAE20j6tBJr52HVnNjKsJtJ4kqqNCMVQFvEUnihkGVvhmoc3SSkOtvuQEsOz4foBMsVLvNG_AEUAGpGOZNllSkDZUW6MY71Bky2k_LWzl0Z-0N21O8k6xMCHUZNFRI_QsXbRchUZWSeKehfkwvK-2g7es5hKDBASt3mXcaMST6vlQe2KNF94CHV-2LXbC75NW4TWXbE7X9mvYLvdlD6hfi8pnBTXDYg_a9e6I9g_JSmqRC2lO6nfS99zVTSwo7wKWqPGhT7Mw3nrbRQVfdd4OiIpJwokBHzMXzsO7G65V4zBZn0e0CDh_GgEjh2PfAU_kP4r2kNDNFuVNRmUJESCbAeT6GgmbJBFoaCSPHWB8MqzhGXTEvQEVRsKmHdQ0zWTQugQiGlLb7pYFayVmQIt_iC3afPiiOC_tm7qfYrz8vr6V-Cg7-HR_Uzgrw1K-yYuUpPOtQHH7WXyFLsiCGN-1TdDRw-R_29_dvB8ST_o-A,,&b64e=1&sign=23a377d10d4333b8f000320a2d43c1d1&keyno=1',
                    price: {
                        value: '23250',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 40885,
                        name: 'ЭЛЕКТРОЗОН',
                        shopName: 'ЭЛЕКТРОЗОН',
                        url: 'electrozon.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 2207,
                        regionId: 213,
                        createdAt: '2010-06-11'
                    },
                    phone: {
                        number: '+7 (495) 120-0575',
                        sanitizedNumber: '+74951200575',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTkCGomu-M7tVc0r6wf-O6eY7TIDcW_Ffba1i8-JV17De_l2VVJ3insUWSLdO9oDv4cZC4LnDcanf8vzbeNsLqqw8t2xduAXlx7YIoS2UN0q7N10vLIe56_pqQ4VZ1xBZGoN4D5wlDl1MQzsXT6Taf_LAQi8R6V-4n0WNjUCLlb8_JqmuRDH0ww07WtujVuOa9DCmJiHZl4VAUQNb2ZcHqc-1NvFo7eiFSG9yYlWxZqf0YHfeHbDEqPD4bUSmr_xzWdgrX0WeJv_scuglyEBMe3_dtLVj73OxovGrqevxOWB14NQxTXq22FPOa-i0TkSSQNPzYPyp5cn4WJvUm2xEI2nXSY5knh7ra9_ExRNarpMu8HATW57yrtPR1KWJZchIb3AxtMVUSJacjSwkP2mqXgvqXt_qwhG6udKL-Y2xuEQLrH3qCNReQpgGDFPxykHmrRJi1XubjwCIUcK0jsMtm6QzjHQQAcj-cY-FBgsvgQXzQmctcPWPHMQjpftWfhfXWdB_tfZmVcCbdt-wx9j2T7RW4_16_wafm1VrNrAYh_HPwrBB2C_hEha8T6deUaoK5Hw23sz8nyZLutoaK__jhrKXuJNG-FrNtZeJi6zkMTwXNThm1u_caamzoNCkvA9xDFjrL-bE49iCyu4cJaqShIFJuVBxuDnF7knluqPAPAHRk8odKDN_7qr-tlMc3Sbk9FdAM7HzLuOVj0jTSf6S0SuAGilkoLt_aViKa4rnFcTuTb_iLa1Jt7B8C-bhWD8oPFdA3b1ti3cBAS2YnVkWemc3XXhIWpxvKqkV3nTtBg7zn8Q-e9-fbi70e3r0e98DzKHNmvijEj5K6J_4yxn41C2M4PYgq-akLpl33fesG8w?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_q4nrhm1iYNIIO0BJf1c5R3HI6YHAadpTqaeK8t4mE3aQ83EDFCBHG79sYE2iruEVaiZGt1gdpxp94kRpEKiB7ck1QoX7fuB6yU1HQmiuYAK9C2xcir2CADaWGxm_pcAOzlW8JdqYkcoTq-F6rYwxxvLBEKOR4W_alZtsEu9YWhA,,&b64e=1&sign=9cecc71dc23116829be6d9e19c48deee&keyno=1'
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '280',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 3,
                                dayTo: 4,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 280 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/247229/market_VbLBbeAgCFgxsedtL30DbA/orig',
                            width: 700,
                            height: 700
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/247229/market_VbLBbeAgCFgxsedtL30DbA/orig',
                        width: 700,
                        height: 700
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/247229/market_VbLBbeAgCFgxsedtL30DbA/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '120-0575'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '21:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.50290243',
                            latitude: '55.74191816'
                        },
                        pointId: '122750',
                        pointName: 'Супермаркет \'Электрозон\'',
                        pointType: 'MIXED',
                        localityName: 'Москва',
                        thoroughfareName: 'Багратионовский проезд',
                        premiseNumber: '7',
                        shopId: 40885
                    },
                    warranty: 1,
                    outletCount: 1,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/00v4a9DLkvomO9--msjCwQ?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtXghLduS5NSCmzvL5pkg0ZLzwTIzD15DsvXbGHlTR-sIb5t_sQq-Rs2aYF6Qyhnpj1Uj5r_2a9Zyws5A1zjlkNC8P-5WH_od6_mYg90pqsbfnxdMh5EqF_N&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZE5_JApxgx-sB_7dbHXUYxT8b57sLWf3BkpnlrPMkqJ_A',
                    wareMd5: 'SYnGNltqnpVrks48u2Ng4Q',
                    modelId: 1724547969,
                    name: 'LED телевизор LG 43LJ510V',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRjuMjIGudiizqA9EP0TjOjAMj_12H0Upy3U2pyClnTly-FQzHrsILvweMp_2ElhxCUwIZXHNr8eFAuQ2lph__27Bcn_E1h2_YExdm-S4WOlGh13DqdRItj19foAGUkQg98_lBTUMTUB3n0cGOIoPgWhjN3WUBF_WmaNThvnCqfTU2iKba40SEKYHzJ4wpDg5dlV_Dgl9OZdiuyx3xY04JCe7wnMWoNZqJCjG5caqidpI_epriniMdt_7sctSK4O3AOYYmqI2kEcmaG-amvLmO2mzBaTwQ4CBTph-qsjQX7UtYLGUHe3orYxUgTBinreuQZ4yHjJThZ-Co4-_GDldlDp3ZfIydkK4ytUtTFJnsrJgEl3UZ7IUkHrW1Aotxmb1j-sp_c3e8-IynubM7JY7mDcEurn3wBSuazrYdjkyh-yOUa3CQCjTn82ZZfouG88krYuqoua4ZIp9ST7FFRh7oOnFMrJhpORR-E2eY0_H5i0JWsNijhLZZtntxlL_sBSrfRZFgma0lLhEl8UifSn_zBXmEEpftN3KinFi2wdCKHhuVcy8fSyC1ir2auqVrlwz5DoxbiIHJTnz5u3TcYOcCilCbwhMMvz6gMw8ZSkTYxEid5pW2MOMFugw4wMGnUtWTyanyOCZjqe5o7-fCygyH3wIRwx1acn4Euoj0vxF8vBxmt9jHTDT-Vy0vQU1hXykt9EXVxcAapr8Aenc52CJK47fk8rrcNlIi5dyDzlmFjgnm0s10-NLo4UFXAdIuvt4pMMPGM43OIbZwegDltOnxMWdQ_Q86xNb5mybI6QPDRP0RBolIC1EGtOLmELo-UlUI3iZbXvs6kRplg1DfFO4pj3twJHhJXFKMT1OxmyZT-7?data=QVyKqSPyGQwwaFPWqjjgNoTnhbIOdIdLW5VrW5cK5mdUwO3bRO0gyCEb2BHF_m8e9s9WNPHl39GYtqwo7Lc4ZtTCUs3yEtTkyYOy00rkpa8Ib4JFJ5WtqIgjNA_2UC42DVSWSospiwnm0imZUoba_rfYStqBlEJctSBYSE5W3e0,&b64e=1&sign=9cb5d772b3083bc3645e01f605f4a60b&keyno=1',
                    price: {
                        value: '22980',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 172708,
                        name: 'PROHDTV.ru',
                        shopName: 'PROHDTV.ru',
                        url: 'prohdtv.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 343,
                        regionId: 213,
                        createdAt: '2013-08-06',
                        returnDeliveryAddress: 'Москва, Кировоградская, дом 15, ТЦ «Электронный рай», 117519'
                    },
                    phone: {
                        number: '+7 (499) 390-33-33',
                        sanitizedNumber: '+74993903333',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRjuMjIGudiizqA9EP0TjOjAMj_12H0Upy3U2pyClnTly-FQzHrsILvweMp_2ElhxCUwIZXHNr8eFAuQ2lph__27Bcn_E1h2_YExdm-S4WOlGh13DqdRItj19foAGUkQg98_lBTUMTUB3n0cGOIoPgWhjN3WUBF_WmaNThvnCqfTU2iKba40SEIT4VutVt_lCLkZl4IkncWfMvsnM5ie8sKEjPUrstjOkfxi7Ap1veQelshTwd8Tbj7amv7xAHUAGXuXua6SOywIltCZo9S_N494jMUKO8yE6wCpIrS8EHWIPwwc7A3BEgmqgmwuNscrxujqBjhFK0UkAfaX34yUESObS_PU_giLLFrodlpoVKfPhHnTh8BYTAt9eLCW6NMsCblgl2gwc4Uh9ecgD3VEZI3j1I7uSmmivJwK9EChQ_yXMWY_L-i5UOqsuLHV2spbWB7MujNAwPUYZ4kgFsx6-vaV5gHH20_2o-dhH2zXtaVDaJLhb5yrChBKYvSwjDJ1RkNAYgaFSW4bxvruwXW-ikdd4MST5ktBJmek5cKpWKAH8qg6_n3KOJ4HNCAAROXf4Y0VFY8BLVqLhhtA6udXWyGXYBE5V3nA8mjA1kceoTgA1RnhLnD2FmLSyTAtEEnjB82PFiuZ0iNhjmflLhOXA66hh8NJQ_JIvhtavyMwjOUlCI_snmEhc5qwS37Tg3O1rFV5I8Ypc5HcW3Pe0dHkNB_oaLpyjLnkU5AddPTUif16HExLSJNX9_ph4Dv3FhdZsrTYiKjMJ4dspX_RmfMyzYNSI3KU8dbtzGh5UyvbjBgjcfNpAawpuq-L6vTMncucZVwANwJ2FO6SA0fbaXk7mM10VwSE_aMd3oYhVKS1yZ61?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9TxiHYWnj5rtxUI27ls4UEzP-WcQpO7dEQUvOlOuIr7z9IfwjEPsei0rgm-9Rh0zKZcfuECy1GBwcbARy3jDMf4xHlkbFrLeMmzqYRJPz0Yik_qf2TebWlLWQFHCs_RmbpoKhOl_2qzq1wUhv0Jz0JVkuxi2v6T1wR1SIIHkX_fQ,,&b64e=1&sign=a30bd686d9a57554935aada329cf6f88&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '650',
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '650',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 4,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 650 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/202108/market_IyPhz_YP0vSwGKHzaexkdw/orig',
                            width: 800,
                            height: 527
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/249455/market_hPrQWgYR1qunYJC2dBbhjg/orig',
                            width: 800,
                            height: 527
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/486815/market_3IWSGF_N2jfs4wPLDqU-pg/orig',
                            width: 800,
                            height: 527
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/203633/market__qxPsVQv_ojTFpbRjRPGZg/orig',
                            width: 800,
                            height: 527
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/241976/market_lyw4aYVRioeL1s-mSOg-sA/orig',
                            width: 800,
                            height: 527
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_Q5pv8LRpJ29psJK_y8aUbw/orig',
                            width: 800,
                            height: 527
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/249455/market_Ut57C_mmW7LR6YJO1akYDw/orig',
                            width: 800,
                            height: 527
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_K_vFsP7GwwpkXav6Um_ngQ/orig',
                            width: 800,
                            height: 527
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/196254/market_DTaOHve-xKNS5ENqhLTCIA/orig',
                            width: 800,
                            height: 527
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/203633/market_cbd7uuz0PwbcSx1B71tDJA/orig',
                            width: 800,
                            height: 527
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/202108/market_IyPhz_YP0vSwGKHzaexkdw/orig',
                        width: 800,
                        height: 527
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/202108/market_IyPhz_YP0vSwGKHzaexkdw/190x250',
                            width: 190,
                            height: 125
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/249455/market_hPrQWgYR1qunYJC2dBbhjg/190x250',
                            width: 190,
                            height: 125
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/486815/market_3IWSGF_N2jfs4wPLDqU-pg/190x250',
                            width: 190,
                            height: 125
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/203633/market__qxPsVQv_ojTFpbRjRPGZg/190x250',
                            width: 190,
                            height: 125
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/241976/market_lyw4aYVRioeL1s-mSOg-sA/190x250',
                            width: 190,
                            height: 125
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_Q5pv8LRpJ29psJK_y8aUbw/190x250',
                            width: 190,
                            height: 125
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/249455/market_Ut57C_mmW7LR6YJO1akYDw/190x250',
                            width: 190,
                            height: 125
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_K_vFsP7GwwpkXav6Um_ngQ/190x250',
                            width: 190,
                            height: 125
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/196254/market_DTaOHve-xKNS5ENqhLTCIA/190x250',
                            width: 190,
                            height: 125
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/203633/market_cbd7uuz0PwbcSx1B71tDJA/190x250',
                            width: 190,
                            height: 125
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '499',
                            number: '390-3333'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '20:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.605158',
                            latitude: '55.609573'
                        },
                        pointId: '314612',
                        pointName: 'PROHDTV.ru',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Кировоградская',
                        premiseNumber: '15',
                        shopId: 172708
                    },
                    warranty: 1,
                    description: 'Full HD телевизор LG 43LJ510V — 43″ и Virtual Surround.',
                    outletCount: 1,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/SYnGNltqnpVrks48u2Ng4Q?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtUE9vuOr3CPCMdkef9E1ZUnyItz8FY_rgOVcIBf4lgtINSMUTdy8Yooj_zd5kvstHvvI-vAnfnc5PjgSprYvthUKSL6udLCbdUQlx6BJswpLLSKGgccljwO&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZG5z_vKfqmTv3htl5Zk_FtgvqGPHO-9WleqqX9d1BIoYrljtHJtdahRz5f9SKcu-4AsO4uCykGDv1uVerhtenD2T_cA2tYxD3BO_Rf8Ooa5ejjLdcyA6vUiEHCoUyyAutEBrElsnn09a-RKdh3btjsLkAaC9bkmTvuFyOWk2vGSIjg2CHGYFKuTzHMfYLyzz0gtoLehF3hwfUJpGK7UB9u06-nDpJar_KUyl8aG3uKiKxo2KMB9cYYJLQQE68D2Rq2mWq79ItA1MfBLcJOV-pbfUX9j8xgQwJA',
                    wareMd5: 'ewDevr1QQ4iR-4dJCnqPPA',
                    modelId: 1724547969,
                    name: 'Телевизор LG 43LJ510V',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mT27lmV1IqjAU5oBPaYIQpgIJ5ggVQKoxZ7qPOebazgmMFgGggJ0bJR53LJQ0lEP9gx_Ljy7AOujHBtvWatjsyTu5Isif6amMmGxeChDoKzfwCCQYN8ShNs91TozuhdQ9n9BREqpvEYhC_cZveLgQc8MX88clXMPzqCazS1ZrOSvMcTai-WihyD51acM7HP0bPV-p8xfFByH0GF_edgjOK1I86pP1pNRsSBCGIISl4r9TITSMg08p4wRpHfJQkyXHj1k2rS_cIBCuPMbVXZ2Xnnv0NtfDtKAZpGdgju2yyNuaSD0bF1UC-voWAftCRiAz2SJ-aeaXmFJ_f9cGjejmQZzd0quoyCA36MgOItHdoWx3wFvppziZJdgtE2uDcO0dXUOUqLv5wioR4doBfGZ498WSmWemJihEIGjoO4EKiqWBqJ3aK2VUaSoWjwl-rEM8bjdooo7zpj2yjLjrkVTk6AqSJugCfTEmwwYDPqEJlrEqoCF4qMAFpnEcxbfXAUXMw9BJizyLI7kr-tmfmHqUITqkJ2o_SIDoMdzYXQayjeZhsdcVhMr5KdE51_9aMnwVYQ5ddUIvMTiwNYpx0OVms01LW6Pt1QjsjTSWIVE5hnu_YDyUUFb5dUulJT5GQyG3lg0kcCr1pNEn1DAHv-hXX_pgQEgJTOxFX9S5-Qk7iAfe91a85RdvPcDzcQUYXy5974klW8oYK7Z7a_BTxOzLXljQMePBBK6nZCZoibRQPLSYTUzW6ixbdNeTjbLYB3otvQlGwb5FmExSKGussUdm29yabcDL8u_xvnsPsTXimnf5g2Nt7sz21-KJJeY8wklWu58kVTpfQuB6q66r5Zb-X5OfVOhVUuMJELql0mRqYeC?data=QVyKqSPyGQwNvdoowNEPjWWROvLmyb8K3iqArS-4LD3H21mQtIvZZbF7xnkB5AhnaZfcog_89hdOsguDscqZqICH0nhL1IjV1pyAUClvt-uQ9sxmKohVhtVUUMMc-fRZOfsNXYGUdJ7eirgZz1AeufmCO9sdu6V5OZu5VVeL2COmbf9a0sv_LnQR1gZtfVFuvPxCcspDHULS6QHu8zQpGN3Qe-Pr8e8_K18n-cLFsT61wgbQG4Q1fcnsEdwKf4CNd4MNGiTBB2A,&b64e=1&sign=2bf9855d175512177d8f1f2717c1d89b&keyno=1',
                    price: {
                        value: '23990',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 26104,
                        name: 'Formula TV',
                        shopName: 'Formula TV',
                        url: 'formulatv.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 765,
                        regionId: 213,
                        createdAt: '2009-08-19',
                        returnDeliveryAddress: 'Москва, барклая, дом 8, 121087'
                    },
                    phone: {
                        number: '+7 495 518-29-66',
                        sanitizedNumber: '+74955182966',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mT27lmV1IqjAU5oBPaYIQpgIJ5ggVQKoxZ7qPOebazgmMFgGggJ0bJR53LJQ0lEP9gx_Ljy7AOujHBtvWatjsyTu5Isif6amMmGxeChDoKzfwCCQYN8ShNs91TozuhdQ9n9BREqpvEYhC_cZveLgQc8MX88clXMPzqCazS1ZrOSvMcTai-WihyAbgS1SRw-J0G5-s6TLw6fCDr-isnUHkzSWol88jY9Rpa8sxASJUjknKXw1qo0oG38fLgYHlNZldch5QiDm_sHPY01odXhrujFRnVmMiNdHSAhL2zbhDd_WRsZsnikushoC5B5cEl73tsMlpFZ4eFZZc1A5-NXfJOfsSqe1ESP1xBIvjiDHJE4Py4AwLyiH7UZt7MShlzQPFAWTZsPrKJ-jOj3P_EIgyGB_z6mbTqRmPFPd-2jMaRl4Wr2IyU4st5Ajt5fktOwcYOBCaRbTlihT2YHPhLyo-7jJx3-_jD75MqxtecOfBskiYfmwXkCk0bk15S2tG2W9gScYfa1VCfBhi6MnoYPmyqZD54zAjWBnhqJsxSM7Ne6jgrDtXiO65XD5-BqEtmJgqAkJmcoOUveJQDn_uXnyzpVjluM0UeuEqJIwC6dndHhniQmmKdRHKNFjJNDHhMUYfEXU5xz4xoRlQgUOirZjGXvnkahriNss6Ig-6LgJEjXEd0i44yuIl0PPRGWxGkFPy7fkYnSMP3ELdRD4ipRHT3iU01et5kQd-nLIw7g_0uNYjKoWWRzQUk5QUyQRK4PQQnjYTYBMLroD8PusLMI3Y1dkncJSoPVVACKjqJ0X7h_asiCqk_5BzOdWaXm1lMtS3pGBBSgoDOjx-OO-PFONX-mj-8kE9iUQ9LB4UAm-WXXM?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_jih0jFOB_GZgVmw23WwLN36JOEHYhwPRGoTCduyLvNmdIPHt5PvCnkJQiYkpr7oZzYH9_6aNT4IN5HNt4Q7leahFi-o9CjwRKzForcAs_nHxGTOnDGu7kpaOMKX3qgnr75oibynF2LVybZWMapmMVAcfA0eYwAQlVGw-TESQDiw,,&b64e=1&sign=97091d373ec132cb8b8680f80a9ffdc5&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '590',
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '590',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 3,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 590 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/225051/market_QdFBl4_dcmhEC5VmWpMjzw/orig',
                            width: 600,
                            height: 450
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/212340/market_jIqs3GFTm0CXVnVtUIXicQ/orig',
                            width: 600,
                            height: 450
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/217087/market_FxcjUfmHpJm_aZEEV604JQ/orig',
                            width: 600,
                            height: 450
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/196254/market_GWP5x-t-_NpJanXWXz613A/orig',
                            width: 600,
                            height: 450
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/175127/market_wakf2zgtuZi0I2JKZKGrhA/orig',
                            width: 600,
                            height: 450
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/225051/market_QdFBl4_dcmhEC5VmWpMjzw/orig',
                        width: 600,
                        height: 450
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/225051/market_QdFBl4_dcmhEC5VmWpMjzw/190x250',
                            width: 190,
                            height: 142
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/212340/market_jIqs3GFTm0CXVnVtUIXicQ/190x250',
                            width: 190,
                            height: 142
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/217087/market_FxcjUfmHpJm_aZEEV604JQ/190x250',
                            width: 190,
                            height: 142
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/196254/market_GWP5x-t-_NpJanXWXz613A/190x250',
                            width: 190,
                            height: 142
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/175127/market_wakf2zgtuZi0I2JKZKGrhA/190x250',
                            width: 190,
                            height: 142
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '518-2966'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '11:00',
                                workingHoursTill: '20:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.502786',
                            latitude: '55.741135'
                        },
                        pointId: '315640',
                        pointName: 'ООО «ФОРМУЛА»',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Барклая',
                        premiseNumber: '8',
                        shopId: 26104
                    },
                    warranty: 1,
                    description: 'ЖК-телевизор, 1080p Full HD диагональ 43\' (109 см) HDMI x2, USB, DVB-T2 тип подсветки: Direct LED 2 TV-тюнера',
                    outletCount: 1,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/ewDevr1QQ4iR-4dJCnqPPA?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtXpq8Hr4tQvuIZGZm6GWkKu-hunUzhWpvz5yZ12ckNMnhoaP0xk-mSxQZQbp6AYaKj3J3hGIHYjg96Mmf0z9VTAaLtAcgS3hG0xLWeWGsyJieBkxrji72sn&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                },
                {
                    id: 'yDpJekrrgZEqsfFbKXtuYRpOVCv4NMPw2r_vzfKZQC02eG2UhkVoKvBPaWjzlYab3cAR1vGfRBDGYffmzqodLhW23iKBDwLTWjMjcBMsjmuuze2GeDOTFYQIuE_U2w8ocfzEzdsHl8NBzEpdhwY_71Wx5RoJJP1sUXkBSX6mOt_AbMuFAJ_kEIpeVACJJTMbb9Cyr0YxfaO98y8Fz9ySrfPOSOt3X2KxURpTgjY6fwHZO6dDD70g0gXbWwzQBJpdiZ6wUnN8pGdUGCTLSCYQJxVjHlJ184cY50oAyr5EUYM',
                    wareMd5: 'ownvdvSPij_gT-BXqeyGXw',
                    modelId: 1724547969,
                    name: 'Телевизор LG 43LJ510V',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcD-yyQuM0ZntmqJxm5mLBseXhuFe8l8g7yySnsE5F5MFJpi2iSslXiNm_b4acMeNbqUDRpMImDAyzJ3VMhXq7kjQSVW1EKq02BtmnT351V0q5_16iZ17ix6dZyw7jsNM39a3C0K3haLF3Vyk5UMzxxFXh7BJ9I-qbFkjWLzIc_IMLXRPgLpYxpbHasFrBj0cdtEq3laUSjOpm22jDjW-dMmRqA1BqW-xuENjTKA4bI9boSNgQBKAa-okLiPSlj9i-mEk3I0a_bntkBJe9jNJW-SsvxyL3IEEtjZALEGRip65A-vhHNrx9kRHs9U7ghVuG7A6pdA6ODrXEG6xWyfk4wThtFyF3JzvONOG3tIyxj2cFMOU7Ah1-sJFIBZf5INns3T8qsdDZuV52Y0u-HslTS02unG_RPrIdBlkiy1M7a7g4PiETO79gAn0a6xVwWZyAVRhdD_2o6xrNIvrGRCQSROnAckBSDfZGFoAROOppegq2g13KtZBwJJ1W6_HNuoEvDMayHfEpRNFaRtLhkyu4zVj0LisPId_rENWyoGNHNLMMPNARYos6744F_ulF-vp9SKaKo5M8dPc_SUSasgxBsYY-WlorpmtN_k5Rwo0-IuWIMs-cKG1t43jO5_r4oNYAI2_O7qolsziMpGaDq0icmznuANhQL8WJ5VpgWAQHdhvVnv5FvJv-KZ90pJjIDqoo7qq3x9Mph6nF9iCXaE1gYCGmQTu7TTsRJVHpH-k5YxDgjgUoBxbWjRIhpR7gUqph1kHzlQn1UC9PsDcibj2sSjgghfx1yEukGgUuwXmprg7DGOEhtYI8VSGH0X6buNnEetZzM2YLfvfD3mtEz67OTMdQYvadp7S2YVhUMvOJMT?data=QVyKqSPyGQwwaFPWqjjgNskD7VLU2XQuz37o0CeGyUwvGGMNfjqf93rHK-byowwmKfuolalecVf0tYmTw0ZlN6twYhvnQpGlgdJEW2mUpqkKNbI7h80ytdQQrAGeTy9JcJE-NBCMcEpH9lun_G4RLB7QyKUTk0CRVOd2KLY_RvebmtNFxAe27GQN_OOhlfLUj5MMBGMJ_p2bswPj1ZnWam5TkHEqNuVABmAXp4yRDb2PD87qcbvXGqb1igc1CPXJ1wrxjYyYZuLkqJzoiUNuZ0LXpdc-47lI50CfzbW7TOtwSc427MSFIA,,&b64e=1&sign=c1ca5ea88de002bd20203711f08a4d00&keyno=1',
                    price: {
                        value: '24631',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 144921,
                        name: 'Медиа Электроника',
                        shopName: 'Медиа Электроника',
                        url: 'www.media50.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 2705,
                        regionId: 213,
                        createdAt: '2013-02-27',
                        returnDeliveryAddress: 'Железнодорожный / Городской Округ Балашиха / Москва И Московская Область, Октябрьская, дом 1, корпус 1, 143980'
                    },
                    phone: {
                        number: '+7 495 215-16-17',
                        sanitizedNumber: '+74952151617',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcD-yyQuM0ZntmqJxm5mLBseXhuFe8l8g7yySnsE5F5MFJpi2iSslXiNm_b4acMeNbqUDRpMImDAyzJ3VMhXq7kjQSVW1EKq02BtmnT351V0q5_16iZ17ix6dZyw7jsNM39a3C0K3haLF3Vyk5UMzxxFXh7BJ9I-qbFkjWLzIc_IMLXRPgLpYxqrPOj3c7vSALx-P6dvsUyg-w5wyLk7yv9HQgqKpGiwU6k9RE1S241v2TWKRa8ianj2DzxzTBOQycH0Qlgj3Helk6cRUA-r1WfCGgZWgVMvIM9fBzy8zVWcSs10Qj8i_pPrwDkZGLlHJ0467-8nXHTVUltMA-f6Z8JOztPo_Aa-dJBf7gWBNPsvEngYhCG0-VmP22BzTr5hlurfYn4qPUeL-zbvCPVj9bRJGUsmKrox6oo_Vl19MEaCdaRzhQadUfWhxi7-4kaS5JkYRjnauLbvXKWaQZ4tJc1j0d1NqRs2GihPhQpmmgOE696W3KLAWADeUpLpwife5U7nnFIJ0Ag3Lj1y9kYtRtpx_jeUifYibOvKQobeAOMvIkPXyF6uJeZ8rXGHSEhhvon9IacsorL-dXr_8iOLU_ue_UxnsoQCQaFYsEG1jg5DvRMbuFkla-A02OG-0zSgGxgfJF9-wzojg-LujUZlLKkVlw0uRlnjC1thu3w0rYjT8_ubvBTkeVSWGJ2JtqQLakjs1dk3xky34at4fZh3mX4bqcr5D_HHTxiXkcBrWUDJc8Lq_TV6_CE2E9gTw7zcMVXpt9bCmSdGzsvTzX6VfrnCixpxyLm7Qzg2ZXcAHRxtg8TszISS33FYQQl3HdD0VWYFCeQt1aen5losc_92gFNdO0xFMpdDQNZ55Ib99Zg_?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9DrWbg0ogd8rY3JDVGBibhncgSofhOUZ1Xs7lhCGFNh6r5wf7-nBVaE6o0FQefEidMKhI6KosOwRP068Tj22sjo9sAEwkrSeN0pA1bg3mDQ8gCiAPSAYZZbGhxJJ0efsNHjhWUJLGgFF0Pdw2M_qtVfI3yPxfHgfX_Lm4cRyqx8w,,&b64e=1&sign=395725a3beefa82fd67217b063e0d462&keyno=1'
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
                            population: 11612943,
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
                            population: 11612943,
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
                                    value: '290',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 3,
                                dayTo: 5,
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
                            url: 'https://avatars.mds.yandex.net/get-marketpic/238105/market_EQMQmyTBMw930feuNNNEyg/orig',
                            width: 701,
                            height: 466
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/238105/market_EQMQmyTBMw930feuNNNEyg/orig',
                        width: 701,
                        height: 466
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/238105/market_EQMQmyTBMw930feuNNNEyg/190x250',
                            width: 190,
                            height: 126
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '215-1617'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '11:00',
                                workingHoursTill: '20:00'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '6',
                                workingHoursFrom: '11:00',
                                workingHoursTill: '18:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.756353',
                            latitude: '55.654934'
                        },
                        pointId: '477590',
                        pointName: 'Медиа Электроника',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Мячковский бульвар',
                        premiseNumber: '11',
                        shopId: 144921
                    },
                    warranty: 1,
                    description: '- ЖК-телевизор, Direct LED-подсветка - разрешение 1920x1080 (1080p Full HD) - диагональ 43\' (109 см) - встроенный цифровой тюнер DVB-T2/S2/C - разъемы HDMI x2, USB',
                    outletCount: 1,
                    vendor: {
                        id: 153074,
                        site: 'http://lg.com',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6223166511496346071/orig',
                        recommendedShops: 'http://www.lg.com/ru/yandex-recomended-retailers',
                        link: 'https://market.yandex.ru/brands/153074?pp=1002&clid=2270459&distr_type=4',
                        name: 'LG'
                    },
                    variations: 1,
                    categoryId: 90639,
                    link: 'https://market.yandex.ru/offer/ownvdvSPij_gT-BXqeyGXw?hid=90639&model_id=1724547969&pp=1002&clid=2270459&distr_type=4&cpc=R_Kc8ZpLLtU57X4WEpqv7qmO0wlmW9BA-q7sbFkCc159e_ya6v6JYVRcdfT2VjPzrREVqTOjykMicx6dNGYqofJ9XWd96eHx0NKmFfuIkhzojxoqTR9z2itvMGMGr-Pt&lr=213',
                    category: {
                        id: 90639,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Телевизоры'
                    },
                    vendorId: 153074
                }
            ],
            page: 1,
            total: 64,
            count: 30
        }
    }
};

module.exports = new ApiMock(host, pathname, query, result);
