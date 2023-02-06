/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/model\/1732210983\/offers\.json/;

const query = {
    text: 'Мобильный телефон Apple iPhone X 256GB',
    category_id: '91491',
    price_min: '40475',
    price_max: '80949'
};

const result = {
    comment: 'text = "Мобильный телефон Apple iPhone X 256GB"\ncategory_id = "91491"\nprice_min = "40475"\nprice_max = "80949"',
    status: 200,
    body: {
        offers: {
            items: [
                {
                    id: 'yDpJekrrgZHhWq-Wp6qRdvNfa4PxPDXc2hLK1jAygZrFUHEzWA6JaOyiJlHF4z36VHeM56UoFWifGS8BAEkp7U2o7BUJvyHmmoBMtgUmKwWPdXTdVq1YJPD7woAMJdon-O8B3QWahswXA6llQc-6Ec6U4ZeyrkGt1xWWWbi9mRyKy4See-GyLZw6IGlvLv92my-HpXFH9sFBzlVGHqXfbPKqYdA5oq5ZZhG6RNwDF6SfaAyITMOSAeWji3Kp3tjTKchsDJo16H0nb8NVkGA99HBUdG3XxA5hVgCnIQVD62c',
                    wareMd5: 'MGvQw-j1h9RDgnQab4d9dg',
                    modelId: 1732210983,
                    name: 'Телефон Apple iPhone X 256Gb Space Gray',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0Lmqp4ktVibyCsn9y-uo0UOO-7dxpD4okx7QmB3w13ZGmnY5v3O5bx4BiGoQ4gr-Kplx5tsdtb8gO4IwkrtuNu8FqF4ge08lcKP2We8wOZJT9IHNl3AEb5pq1JZbenFjYkez8UXWzhyDb8rJX6bvlOxDsOrQzjINKzpmr-oVtW7MicaWPU6GY7iAikyz1rxCp_CCWV1q1NuFtxLTtiduN-cXxTKQCtfA-5Q6hIDrvrPsYOKnSgApNpMXhnqFAIkZjqFPObD2ZlYjnVr_e7uD6aR50kx8B0TxYHMnCCgWSJ2tVXQLgRmkJ5OBrsdOOjJjE4tccJnDau1izS59UgpTqCr_ZbOX_9vshhVpP-CdA0zXJm3-lJOqVN2HVCD69g28abIvqsRFRwQWGtYrL2YT_62ttzxjoPiwXJmDkKnIBpQulKDD13vvGDL2xXhRMwvOH_BmrffknRnod4Sy7XiGqB_FFKO7l3xh21O34ma95W-lwDHgK6qlTMUH0PNFsy7FyMgmmSeWgn3NWtjsJE5mc8zgwdnRyXhcu0qzOyJBqEoTQhAYZwaVz0m8J89ZsE2Gki_rA8ffzC85mzB-S4PufLWCW3PVjKxflEib3CrMKuuiqiv33aKqkVBpoMVE5AQbFFoIeZT9C_IFDCmoLrZ-rrYK8TlcOqEAkEPlEXDuYyx3fb9ZwhMfl8G9Fzj_EYFo2DEODgnyrYV?data=QVyKqSPyGQwwaFPWqjjgNpyd4-X04Sq7rebq2wm_wwNrWW6w-zhZmKw4qQYcjl_kTcC9r011X2qkTH7eWd-nw1vA1zrD37QODPpAtwKuwR_uIq5mHNgxH384Yzq1ojOwFaMn4Bf32H1dPe0hkYPjIx_rVBgoQMwxKgYaihRPdthFLgsSZCitduesjiYB8DZAi7po-srGjlsmDjvya_Wu_zuqYbBmeBtcJvzru9ZvCiruES8q0oB5GbsTYzKNAjIPrPK0e-m508s,&b64e=1&sign=02c01167aa046c9bc4cf2326774d08f9&keyno=1',
                    price: {
                        value: '80849',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 368738,
                        name: 'TechFast',
                        shopName: 'TechFast',
                        url: 'techfast.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 262,
                        regionId: 213,
                        createdAt: '2016-07-20',
                        returnDeliveryAddress: 'Москва, проспект Буденного, дом 53, 105275'
                    },
                    phone: {
                        number: '+7 495 744-62-82',
                        sanitizedNumber: '+74957446282',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0Lmqp4ktVibyCsn9y-uo0UT0swpMDYjtDQ78eLVp1xF1ciIWN5cFZvGqOItlL-DoFC9xeXxPfBMZHi6xML1lgkbaIJOpbk9MjepxeDYkKdp1SF6D7dGJYLgiNpjL2HUIJwgkEzrECHDWf1bEwkZz2bKbUH5ebyGh5uQ8iI3IPY2czAPnHKtd1NCKxRo7jmbgG0JZuXAx5GeJuoVu235k3WI3_cAv79McdUDW4keF3Zr_dkqLvRQm168l4qbK665fE33Sw5sH_luwGfUVut6LyKQ70JFIKFSpfRRrzXrl_BRDWAsZ6BeipmKD8agmUJ70KtpvOvjVTYUhmliDjq0FYXFteqXDHa21ljdkwxa04Ryq2kEfVyzTuuabyPdeftV4l2nL1VxRl8CCiQ25M8aN2eGGVH1NcpJ_8kqfpCXSJjns6ukKVobOfQzUP0ZfqBcKrdMZ8bD-R1ChUjZFSj2Gu859_jeHKh85f40w1vgz3vFjLC1aai8RH5EfHo0ke4J-EcGQs_OObLttj-GBtBQOIdpmMFr-YMjzFHlNzmPuIze4VUJHBxS6H4QMb61Li3TDcbb-nLmJltb4e0zWiX9MuidbkJ6-cuHKsnP16_7o9bUoUwjBzpU-PDaKwln-FEJtKvHSE86qnmQFQ2d7qAgY43faH_-kcwJA_vGCFrlFCeBXasD4QZw835tGevZcfLLqf4yJYdWIOiF?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9X2YSSYiFywGF_PkcUQFH-_6K4zDnOBtmCdIiM3agMBQ0pPQDofPIj4vGVHLXzRVjBw46hwFuYrvIOUQulEO5clNrRnOrElHaYwhbi_DXYXYZhqRylSd0b4gk4EopplkSSbejAJ0QcbCcOMOX9qXDfueSLO7Ch9yFWDXpn6TJTbA,,&b64e=1&sign=9d30d5a5c07c11f2a5477f759a45e1ca&keyno=1'
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
                        delivery: false,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: false,
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва',
                        brief: 'не производится (самовывоз)',
                        full: ''
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/208469/market_1Of259utTIiVnWqja7GFtA/orig',
                            width: 600,
                            height: 600
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/208469/market_1Of259utTIiVnWqja7GFtA/orig',
                        width: 600,
                        height: 600
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/208469/market_1Of259utTIiVnWqja7GFtA/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '744-6282'
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
                            longitude: '37.73986',
                            latitude: '55.758038'
                        },
                        pointId: '496598',
                        pointName: 'Техфаст',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'проспект Буденного',
                        premiseNumber: '53',
                        shopId: 368738
                    },
                    warranty: 1,
                    description: 'Общие характеристики Тип смартфон Версия ОС iOS 11 Тип корпуса классический Материал корпуса стекло Конструкция водозащита Тип SIM-карты nano SIM Количество SIM-карт 1 Вес 174 г Размеры (ШxВxТ) 70.9x143.6x7.7 мм Экран Тип экрана цветной OLED, сенсорный Тип сенсорного экрана мультитач, емкостный Диагональ 5.8 дюйм. Сила нажатия на экран есть Размер изображения 2436x1125 Число пикселей на дюйм (PPI) 463 Автоматический поворот экрана есть Мультимедийные возможности Тыловая фотокамера двойная 12/12 МП',
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 1,
                    categoryId: 91491,
                    link: 'https://market.yandex.ru/offer/MGvQw-j1h9RDgnQab4d9dg?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=z1c9QCXlplF5pRsUfB-X0xIX6bVCwSu5JHa_e5GuXZmyrKucjp6dZoDARVfGeFtpSth5YkyA42clt_Y5wkQvo6J8jdQq7mP7b69TalhFIk0F6OzQPG2mZAXyzZPX-WUl&lr=213',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZGO6jM06K0EC4pmFYuCAXZ8ixPg2_bgKJBENn7vaDsUBtjGzFV-eX9cYAsgammCzsZDdaJrOrOU5fYFaaR9vqRrpq1un3W2Uocsrug0dn0w2Z4imSzJiH_9EXMSWV51vLeLj4F1MLEF5Mow3GFJtNpJDWvYH7qrkm8n1_vz2a0xe-SrDiBj2fR391zZeLcJeRf3xDygyQ7Ta6QJjo1qNEmerkF93kTR24Gg6aLXQanjGeliiQP1_4gkqcl0Rf3xORGuazjlxfSkTU09sLOc8ZIpYeayk6sxM7Q',
                    wareMd5: 'fF5ONJMyADuznIb70RR6kw',
                    modelId: 1732210983,
                    name: 'Apple iPhone X 256Gb Space Gray (Серый Космос)',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0Lmqp4ktVibyCsn9y-uo0UOO-7dxpD4otx6pYjumECAoWeKglF-zY2u8B3HmSiyJGX1zYReWYvZTB64QjDnUxg1vRj0KgVjryLe5yUA3dPlKI3eyKWdBJtZG_cZCna10F7-Tohc_ML2tERPBmnXR2ZAxSsYRo0QCJuaSTEd-Ttj6tGildZEIssROAZObrZsmU1c4NwIIhXfI0ZH_UgKYwokyQHonykhf1rE_w-odF4wYc65Nz_uehffm7DRTdj_bLY81XyT4iJTwo_0g0U_H6MbUrD9GEYZuhxdhoGgSTghZslb3IazKd9-ey2YZURNXF9tF_oi46ysxXUZtDAlq7Usogr2PNlJNYityfvUJeDWGroNhBGg281_6RCzoYm1ZlH626-aRTF-z6qzWkU56yEPEnOBLFALWajz4Wny4Z1GIS9QqVG-L817Anz0LE9CrJbZhzXCgX3m-5ccm0du_vfasxHDqbTPI5Vvo8q_NvNy40tpKn1KJAZSDAvMh6rB3Yxl3ZjgQFzlPVN-C_7vHhCXZ5sDAeX0qPVvMPJXU0DTGVSXh993DF1KUaGDvwKb5dZ4P5jMlrs27_d-HcJi-OnZQVfSwiLtLxA0HXMf-WpRo5TkalXF89yF-FrrUxW8LlYBMKRzu0d1q5hiAqGMpPbpTbpiaQcaod85kiC7jJvACWbXfDDXdpz5G7ueR2T-zqYiM4tjDQ7G?data=QVyKqSPyGQwwaFPWqjjgNiq8ga6Kxjbm1EQ1U6HWt89lCqB2uU7so_mHkIW1fUweFca-zzM5hbRHYCbzKS60TcSMzMrKRE9cNS3hH5YQrKBa1cv53x_ViyCWH0_aRqachhvstKn6N-5GO3ABiQUV_jdIuLiEfVm80twtnD7ng-n7xgsY3fPYMYhgBmvkMNbpCSEbPAa6lYf3bjw-Xa2kT2oyMTFEAmzpYUGWVMPEL8dPaSWeFknxkpz7YBuTCkeITLzGfdbK0lk,&b64e=1&sign=cf36a79b3589ba555157c21f4fdf9085&keyno=1',
                    price: {
                        value: '80790',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 397212,
                        name: 'PRO Mobile',
                        shopName: 'PRO Mobile',
                        url: 'vnkpro.ru',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 9,
                        regionId: 213,
                        createdAt: '2017-01-12',
                        returnDeliveryAddress: 'Москва, Багратионовский пр-д, дом 7, корпус 2, 121087'
                    },
                    phone: {
                        number: '+7 495 106-22-66',
                        sanitizedNumber: '+74951062266',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0Lmqp4ktVibyCsn9y-uo0UT0swpMDYjtGWjA2BnFd8Sco56Zn8uqq3gNFiSy9Kc0YHGlqiB7LP4q3EypPNszYxPFRcsVyMjz4s8GAp22CFfVQcU9rIdlvL5R5ftSZvra4Q1DtL2BYeDz8rfS-tTsc5S1ocQmh6OatH5YcdEWoPsbBSSzxT272CImIojeJoFd91e_Ovs4O3hXWrSAgUQYCMACDEYIU7gTfiFDzcyRmtGRin9a-A8n9MMNi7B0BNWhliuRpvkMURIte6snwKAumOodO7RW0YAhsJlEicIfFQ2QcJwkE8KNPd2MWO1yWhYqZnPsDJbfU6X1eBQIaPQVpAxUv-Bo5h7URPSm9JLRY8-ymwaNrrRhPaiu0lzFd5P_ZYNqeEBBuI8iQwAC08fn_wdsWkmsIBNtXWNGJrtYW7F69GbrasfKhn1JVEPQpZfYev0FXNxGsJi1Oj8qiPVmpZlpvAiHuEgmMYX2teUCdaOLC7zPbhDI9iSVaaEHcQT_sh6zaNpsgxm6txwrwX9rkzDIQLDFtuNw5ALDi1H3kE5n9-S70DqwFrnRMv65VC5WOW7-ZVTZgUIURj8LusLhWQGv_42LdKs6sj_aEFiR-C0pGIJY8QNn1mI4EnGAaGvFw0GQR7UWKWQGUR7zQLF09ymb3KC_3GMAty62VpO531n7befm3Mxmah0AzTNS45pUJYX41HWn_Vp?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9CK0-x1f39br1xVZ-q9-dOcyvdHrl550SX3M8XM5W8I_12jTSt-DQkWhVnxTmUcvb1pQRe8BlP9zQzBoLXLl_MTeLLDd91Id5tuG-l-vZmC69Z9T4Dv1FV-vuquHyZQcgufW1zGYf9QyzWUAHcRCDMP2eBZnb-KPFpR8z6yvChKw,,&b64e=1&sign=772b6b8f38f907c8e1a712df77522987&keyno=1'
                    },
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
                                    value: '400',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 0,
                                dayTo: 1,
                                orderBefore: '20',
                                defaultLocalDelivery: true
                            }
                        ],
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва',
                        brief: 'в Москву — 400 руб., возможен самовывоз',
                        full: ''
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/221955/market_Pg8GjbGu-5heSHRBtBWAaQ/orig',
                            width: 99,
                            height: 200
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/221955/market_Pg8GjbGu-5heSHRBtBWAaQ/orig',
                        width: 99,
                        height: 200
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/221955/market_Pg8GjbGu-5heSHRBtBWAaQ/200x200',
                            width: 99,
                            height: 200
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '106-2266'
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
                            longitude: '37.501312',
                            latitude: '55.741759'
                        },
                        pointId: '560891',
                        pointName: 'PRO Mobile',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Багратионовский пр-д',
                        premiseNumber: '7',
                        shopId: 397212,
                        block: '2'
                    },
                    warranty: 0,
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 1,
                    categoryId: 91491,
                    link: 'https://market.yandex.ru/offer/fF5ONJMyADuznIb70RR6kw?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=z1c9QCXlplGRMUicl4QRHZLAAPHvfizEAHuUUiOu1tSQwTdA8J9gAxpZsOLh_zRkRuKzLuW_y7Q3_uiSSbIXgLcaZYofM_GIWY_ZarDkvidFHQn071RwmPPtsGeMQKQ6&lr=213',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZGCXzGrW_b92BZB180fphqAK54KgLlK2cJfcBb163DOZCILONfAKtG7uFx1MGqGF3SFsTUZC9oDIDpCbM0QJsW36ySbFBRTh3nxS2S2zL-IcG3F2SCGgyVMJ9xccMWKc0hNy4WfbluolT8uYwq3QDeI323R44crdFITG-Z4aDuJLBWgVD6vhGQwJsueEq5dXDYKS_o71__OQFAwucFz9JUbQ-l3o8dwBs3nGV8s2eEx5zOfHOGwKklyqeVDMOZGDqom2YsqqulY4bZgdwQRpfTjnHKEJjUtdkI',
                    wareMd5: 'KdjK_GUY-_ZJzxKPz11_5w',
                    modelId: 1732210983,
                    name: 'Apple iPhone X 256 Gb Space Gray',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0Lmqp4ktVibyCsn9y-uo0UOO-7dxpD4omPXg8-4luluzfwnBxPVcXoCuRh_2hP-qmLANamb_YQKFZlDmvDpEIYdJKI-EMLm0qnessnDI-5hngqsFEBagfADVvfUOXVsvAihM5_xypL_7gas0Zn-zp07v6vfCl5hm7CvB0evcr6NAYFFBMjzv1rB8VHRme2xxpHEbzXtMCNxLzl7R2xOqZ0lkVGB622AVHSJHGOU51orCcZ-2V98NEZFCVQYnwt38sWdkzp0C1vl1OsGEtY_LZDrCGrlSF6CKkadHgh45iS70iRUB91anoEVE0_Cntro7xrHyE_cFhMjQUoRvLL-3U0StdqE2wcAN3aX3hxDLOFQT8fWqIX01Zirx0zCg0ouo1gV0RyvkpwwrU1eS2WOYmyZ5MtVfj77dJ-t7OgPFR9ttRWMaAy96RlybE6g1sFzlq8Vid9wrx7NMonHtL-NQvgFV-Eg47y1TT4lgksh-90N6Dsu8q8OMul-QaZEYfpcvVWXsGmLza11yHBFa0k73ZMqfEemeblb9b_tetPljrIRrdNH0ksSu-dk9tEpSIw8O62VKuETGAfu0tR_5UhTm6Wxw2lFGKVdetxXaVgZaVNfrisDn37-nPDAhLCyw2Yu0ZUoXNKSbuut-1BhjEm0qsr4sN-62m1JnvIFRoLoYHh7yllQt4-dYycfHQIPEOezHB80S5cdFMuVOd6BdVMLGeM,?data=QVyKqSPyGQwwaFPWqjjgNoP4jJQzf_he9y6qOBZmpp6tFZp_kTIv7UnOQS91gUQoZcvjGCf-H5xaTY82iuayyXAzBs5U42qZKdsTgPwV8yfyWkWiFqQiTqhq3ta_pFlPm6Sf-8v1XxBQi7_h7JnJZ2QbfNcE0zCgqQsTPea-uO3kBdc_UJx3g4vjL4GBx7AQl82S-67qEglZX51lZmj8lq_0llWQQ0i_h7zCz27ezculMSo5slWVJqb88z72HhjgIpxjT4DQCdog36y6i5hHKjflku-zo9FKDcHMJUCGD5e7kKXA7FiO1XQRF0S8hzdB5V_nIJXsCIsHMvP65tGdcrn18PpLFaYHNQRj36ra_ShbGD5fIAoJFWT9YX1dmox6DY8hAvtmQsFOYjX9de5SM6gzGPGDcc4i&b64e=1&sign=3498a2cfac52235a01a7182cf11dae9c&keyno=1',
                    price: {
                        value: '80750',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 423057,
                        name: 'iMoscow.Store',
                        shopName: 'iMoscow.Store',
                        url: 'imoscow.store',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 11,
                        regionId: 213,
                        createdAt: '2017-06-13',
                        returnDeliveryAddress: 'Москва, Новодмитровская, дом 1, строение 13, Возможно ли добавить второй адрес? Улица Барклая д 6 индекс 121087, 127015'
                    },
                    phone: {
                        number: '+7 499 755-69-65',
                        sanitizedNumber: '+74997556965',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0Lmqp4ktVibyCsn9y-uo0UT0swpMDYjtAAlehXmqC6JCQXzxVCBSNIAtdy6Yfy_B_HuGdVpSn9WqgZnuCLgu2os27eQSdQbAHOrw8tnV1CF5dQ3Bx0A0Vhs6_vQtp8Sv5UAWR8W0mRES17b6U_d_X50FlmkUtCrZnuGcGvnQxyJsrxAa25fez62OOuL01wGFRjGr_W9rRjFgRJesZJjt-7eVHDKIx2GLIqQ1g5mdLWWfIL7RtfeP0xHSiKu3zAkRfcq71FfSIL485UaP6O_PZTfmAytbRWGbEFSKKb9s4mSaqBSNtaMjKQhDIV7UgCMO5tnXhD2VRe2C_PkkfY1E30Mcr6NSUSLVOUCNlAkWQoZ-G1DfNZwGBrpHZSfCoDHe_OIOYI8ZIfbJScnJIjKfaKF_yHc3z6Vb3YWyckm21_e2FFV95_69IXTmpiGVcsUNb7LWWOergto_U9eL--IqSzAyxyi_exVrTsfr9r6xw3QEylm8LviW0Uvr_vSc4TXlPyDSUhYo_dkIFpi9LBLLRBpVRoMl5t-_4WK0fKSyUf1yE2lRJXeQNyrufgFEPce-IqQ0XfJhR3EKZpOnPAon3UW_-5Ai0ficG8y-5tXFymyiBd-kTMBlJnuTeSaKYlVj-gLyn7hb70RQxSkEZCdK49YwV45Hzo_rrGPVzrOMVMXIqvXi_F-to3_-liH0aPVPEDmZ_LnjK53a05Qa7GWXRA,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_DDsC080LbSOq5BFfUcvNHQhLXktZtS-SuRqSe2FDa2zH2RuPAAzUe08KmQhnd9StRKAL5ynN2fdK5Xy7lPhZq2IGvDAo8NIbIrlfv2KxQf911tG1WOONwWzVFTQvlycWYUEHyDjRIc4wSCUMRcPm4e4wNaewofo0JGxXl0SF7fg,,&b64e=1&sign=2c1ec1efedeea09d08fd93205fbffd47&keyno=1'
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
                                dayFrom: 2,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва',
                        brief: 'в Москву — 290 руб., возможен самовывоз',
                        full: ''
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/245136/market_7kKYlH6P7GUnbsM9HxPnlw/orig',
                            width: 704,
                            height: 942
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/245913/market_S3xyZrs_QcQ7FfRsgaVzow/orig',
                            width: 931,
                            height: 691
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/245913/market_LHLGRK7jlexVgafBgZMDsQ/orig',
                            width: 526,
                            height: 174
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/245136/market_7kKYlH6P7GUnbsM9HxPnlw/orig',
                        width: 704,
                        height: 942
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/245136/market_7kKYlH6P7GUnbsM9HxPnlw/190x250',
                            width: 186,
                            height: 250
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/245913/market_S3xyZrs_QcQ7FfRsgaVzow/190x250',
                            width: 190,
                            height: 141
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/245913/market_LHLGRK7jlexVgafBgZMDsQ/190x250',
                            width: 190,
                            height: 62
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '499',
                            city: '755',
                            number: '696-5'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '6',
                                workingHoursFrom: '12:00',
                                workingHoursTill: '20:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.501312',
                            latitude: '55.741759'
                        },
                        pointId: '6404973',
                        pointName: 'БЦ Рубин',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Багратионовский проезд',
                        premiseNumber: '7',
                        shopId: 423057,
                        block: '2'
                    },
                    warranty: 0,
                    description: 'Корпус: стекло, края корпуса выполнены из нержавеющей стали. Процессор: A11 Bionic c 64-битной архитектурой (М11). Разрешение : 2436х1125 пикселей, 458 пикселей/дюйм Дисплей: Super Retina HD диагональ 5,8 дюйма Камера: TrueDepth 7 Мп f/2.2 ; камера 12 Мп с широкоугольным f/1.8 и телеобьективом f/1.4. Face ID.',
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 1,
                    categoryId: 91491,
                    link: 'https://market.yandex.ru/offer/KdjK_GUY-_ZJzxKPz11_5w?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=z1c9QCXlplGExKdUZukRvW3C_U2WfLYMETaSfPKbm5_-l1axGPYNv15Uo9QMvP8D5ZY3A-AtREfdnVcxAeHaEdNwTKre4uycL_uLlzBYcSYa6-D-FEcloEifizWU6o3w&lr=213',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                }
            ],
            page: 1,
            total: 3,
            count: 3
        }
    }
};

module.exports = new ApiMock(host, pathname, query, result);
