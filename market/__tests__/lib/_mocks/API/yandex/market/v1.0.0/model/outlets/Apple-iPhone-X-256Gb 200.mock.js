/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/model\/1732210983\/outlets\.json/;

const query = {};

const result = {
    comment: 'model/1732210983',
    status: 200,
    body: {
        outlets: {
            outlet: [
                {
                    offer: {
                        id: 'yDpJekrrgZG9uQD_5mntCpKVUhn_QtY2G71U0kB6lnyYvV3fqXYWtouz3HF_7h9defReWbOoMc3pkWk82w6OxZ-46FFF_8Lo1Pr7Cim1RE_nat8-lj_DccDS-pi7MiiTkjJ8Q-Ez35ygjyHdpTkD10we2_Mq3fm1LCD5jQjnMZM1Hc5xFFN3DUjllUcRZLGP3eHiJ8ZSNRoNp2oKlEqWuV6c4UZoO7adn5SLulY01FV7Cwf5tUbwXnEtagjRCfmDmGn-WWwt6sM4yblBqrLjM0Mk_pLeNoO7hc5RfgnAhvA',
                        wareMd5: 'ar1n_UFAkX__CuP1SGglhg',
                        name: 'Apple iPhone X (10) 256Gb White',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRvy3ypcvE38MPn0fzoho7Cv_FPgFKQL1IiDqLns9nCwinkyLgcBEcNX00YpQzqi8BCRDoD3fkDNhxx4G9pI1HCzjHg26A5HFE4nTBPNSQWdFgplfZ-8FRioavK-_-3eRVYsfPQc5S6lQX9tr2Ahd0OJpj606GcbKPAo61YKanAZtRKsGB2zzIrwZldphy4OEYny_rmKxXnddcjF5tECbYdMY6vGXfMQJupVKSUtfKJL-Yi9HZP_dnwpNHLE6NFMQ97Dd_tpN6fOa0uKrx0-YcFkA98xgweHk9vH-NSgvEVVBlxvwGI9K1fwTjQjSMQRI8GjQ4aENa1iqC4tPcpK_PuhFCBQxnKB35-jCHu7uea2RYQ-K5gA6zPipNR_tjwwrIaKCrQszSc1jbez2E3NpUrBSoLOZpCw65_nkve9kLeUbgtHyme_m9URnd2jiMHJDIiYxy3d1DJZEj6mjCM8SVkvYvqUT6IC_j3tisslNSu9OezCEVtB4r5R7IVg-KMHxtoAOlroJpbznRSUyw24KA_C9zIpCOLABrvxSueJyBBgwxGoMPCqzCdkJKyyWhePrM5oNQ5YNpTUMMPyOGO8ICFVzv-bDtyXyjeJOM5qhn0Kz_MByXvmGtSudYO0GFmMAHD5GZVhSr2JsjU-4hkflXZ5Uiqk_mK-C_rhYqWDX45MkFQqTbiOuWGFAiJCAudnJmk0wMsIVpP1sqoS6l5ksb19a9YYhu8tDIBg0o3GaQX3l2QiaWYgPbrX-daftR_q1mELTWQ4pDygvd2FJYl-Lw2cmOvWUUFOyjaJslO3xOwWxVUsSGEki4RgcvKoFaYK2L3lY6nMJ8lqQvvc-fdAT6BRUQZEqIK_i2VYpQkgaY7g?data=QVyKqSPyGQwwaFPWqjjgNsUHFoqNHYylp5Jq65MvR-EOyMgvNyEpJuOu_so8XPceYv8lH2gNhMtgl_QT9Z72Yuv_YHB_X8LIgTM0bT2WYCBC2Mlx5v_oi5gPbiQa7BTX1U2iiPH-WK9PKyjh2m3JAX5n5pHVMKpMAUnJA0yUTBzF8tUv5AWAqbVf8YxHVJzDwefJ0EWjYsCLSpW34Y0en7aUa8D4nFp_WTEUc_jVbXSxt5MeVDX8RtEaYI6b4IkGwKgLkmwTy4w,&b64e=1&sign=9d9d16c5e28a7e91e493b21ba6f6d775&keyno=1',
                        price: {
                            value: '86990',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopInfo: {
                            id: 38876,
                            name: 'MCDIGITAL.RU',
                            shopName: 'MCDIGITAL.RU',
                            url: 'mcdigital.ru',
                            status: 'actual',
                            rating: 5,
                            gradeTotal: 335,
                            regionId: 213,
                            createdAt: '2010-05-06',
                            returnDeliveryAddress: 'Москва, Барклая, дом 8, пав 498, 121087'
                        },
                        phone: {
                            number: '+7 (499) 40-333-98',
                            sanitizedNumber: '+74994033398',
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRvy3ypcvE38MPn0fzoho7Cv_FPgFKQL1IiDqLns9nCwinkyLgcBEcNX00YpQzqi8BCRDoD3fkDNhxx4G9pI1HCzjHg26A5HFE4nTBPNSQWdFgplfZ-8FRioavK-_-3eRVYsfPQc5S6lQX9tr2Ahd0OJpj606GcbKPAo61YKanAZtRKsGB2zzIoZyyCphQc290esPJuR6xykI9BlQjwtwZEgSXhAFZ4vDIJ5dIWSgA5kYVpDlu4haJvuUtXb_xiRGRnRS3pel9MYwRbIQl_gtzpw6e8mbzoS_t05JG5rRfOlwsT0EydAf5QbJcenauSWZwAMWuiJpJgoRHSdHMsNTmCFS1tCl0hN4COvKJISk92GH_AMfMMpZKVNG5FGj2JwgsKj3yDuDBZJnO7p0qvqcGauPXCgKATW0WaQsx4Ru_E6In3zxdfTetIzxjZXX68qrAqSiQk2GOoswbQ2Mmnw8H89tHM1pogzeIIdkaiTY_FYTzsA5ts1I264OqFStX9PH3mJq_cyAJBhMTa0pNylae98iRYY3J0DbXZeEsvNbFyzugx-1dIeL_MUdy2VuIpXfgxlfSvV65aRN6js8WM1PFu3vnff3XD9z1QQRKCe3cGKjDycvLwF9K4i-R5RF15GkdkjHiImwiJ5Prbfu98yqv1_ndbxJX_70Dio6ziEZd3PZfklkwyKS4dMFNPEC1EuOEHXoQcZGDTRrLGUKoUsNDmuccJaqxYAH3GVPasQqwQjGz0UwpVgykchzQXu6Zqxajj-iYYMAT5I_fyEmfnu4mIz-dIqm9AkIz2tVxuV--Wbp6iwahF19mPqxz_fH33_4h6pwfpmq13j6bRRMridg07RHXLc0CuW4tZRJ-9jkcJg?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9-vTu3T2RtGO-nhf9XN3ynLDPvA7HscZmP8c4r1QJsZnKuvLuyhQ_7ObPSTwSBy1Lk6Y75gflUiS9xDGYoKukqd7B36CdE6zEb6v22RtDLfdbqvqTVNwZcLo5qQ8oyYkRS3w0_YfYA6CO0ySehzWv6jkBnnMmUPZLat9ghdGzjaA,,&b64e=1&sign=67c46574fa8db778d602e96d52400170&keyno=1'
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
                                        value: '400',
                                        currencyName: 'руб.',
                                        currencyCode: 'RUR'
                                    },
                                    dayFrom: 1,
                                    dayTo: 1,
                                    defaultLocalDelivery: true
                                }
                            ],
                            brief: 'в Москву — 400 руб., возможен самовывоз',
                            full: '',
                            priorityRegion: 213,
                            regionName: 'Москва',
                            userRegionName: 'Москва'
                        },
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/406938/market_zN_IKVetz9XCiuQnzGqKiQ/orig',
                            width: 254,
                            height: 386
                        },
                        previewPhotos: [],
                        warranty: 1,
                        description: 'смартфон, iOS 11, экран 5.8\', разрешение 2436x1125, двойная камера 12/12 МП, автофокус, F/1.8, память 256 Гб, без слота для карт памяти, 3G, 4G LTE, LTE-A, Wi-Fi, Bluetooth, NFC, GPS, ГЛОНАСС, вес 174 г, ШxВxТ 70.90x143.60x7.70 мм',
                        vendor: {
                            id: 153043,
                            site: 'http://www.apple.com/ru',
                            picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                            link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                            name: 'Apple'
                        },
                        categoryId: 91491,
                        category: {
                            id: 91491,
                            type: 'GURU',
                            advertisingModel: 'CPA',
                            name: 'Мобильные телефоны'
                        },
                        vendorId: 153043
                    },
                    phone: {
                        country: '7',
                        city: '499',
                        number: '403-3398'
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
                            workingHoursFrom: '10:00',
                            workingHoursTill: '17:00'
                        }
                    ],
                    geo: {
                        geoId: 213,
                        longitude: '37.502786',
                        latitude: '55.741135'
                    },
                    pointId: '343029',
                    pointName: 'Техника apple - McDigital.ru',
                    pointType: 'MIXED',
                    localityName: 'Москва',
                    thoroughfareName: 'Барклая',
                    premiseNumber: '8',
                    shopId: 38876
                },
                {
                    offer: {
                        id: 'yDpJekrrgZFNpj2jvJaEeRgJzTcGbfRBfjxoWpHVBHh_wgaEJAeh-MjxI68wpQbBQiWV0rFFwYAfZd2tVmXUeArWdBHT9RIxbEWY0dnT_gmhm1yp5Lx9ilbkvxNroKvzi7VFp-FBI5c41m-Jj7UjAdjgR9w8OC49j-oqfj1lkWyaYW-_51Mh-L7WhB5xY2X7W2fKYr1w3qEEoqBUQ54j2pmQQCOupSO33D_YhiX5VRlvk1VGHaia7dJdtpbvAwjulPd7is9B124RJ5_OTzmfdwRk3yjtkL48dXf0f--4TGBQak75ce7thlw-ZAPOAHEv',
                        wareMd5: 'm3B0lYbYsIgP-F5nCMey1g',
                        name: 'Смартфон Apple iPhone X 256GB Серебристый',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8fRxCJIIEUi0XNYM59FBJ-icc75j8rbtInnGHIsMWsmTxrkVnl23jYhU013MXFUvY_uf7PUzJOdRkP9Grf31FOv2a69Di5rQPKYi9JsIkC-LXCnQ5jxh_ag68TrldQ73VVoy0Bl5MuVVp2ueYMoBJuZJu4RKqEdTpcFBvUdQ5Bit3vgcc7Gub-MFpAyc8CFJja1uResO24D8Q1V4ZD7GjYlwp9as293dXDCcS9xEdHtoyxLdC0PLqG0gCwTEyY-6zzYBY7dQWGsqm9Bu-77ybnSeZB-MCcTx2dYdSrGDZ5fau-7UDc727SyOfHUpJmmXzJXKnQLotVFMsAqmQr2jF81it8VK1e-xDbXWWDruFD0DCLDId5Sy24ZdYhBysNZt3Y28_fQ7KL3jJmg3Zb0k8iWxUvgHYwWglWmiBrjLiH7yI5IVr7R40nTbupqWWVk-l8XafKeeGzpeKn6Oh4UddmtGtA3NXvr40CPBrw-WICEmqAppd71xsNmckAVK-oMCESL6Avd1LB5YQfssL2RA43b7o0oZI8R__EkpT1qhAJfEx4qLBz0G5zknZYN65EPxi-JN5s06u6F_KYaDRPU-An3shKtZuVopIB0Dy6XF2kz4NfAV6SNpyL2nIISmjGDfKRHEtJDpHnV6yYj4Ok0lIZSzqrCwLJbD71F1paz7RAz-cM7-Aa6v863maIS8510njTVM2bGB4RB93CwrLIQe-_l5PmPyulfhf9KYBBo0XNGdPpLtO9gAxPO0SvciPGucU0Zxp33gmdnYW_t1g7DYudAuXxD7Y400tLYoKWuePC55-rN2eR7ED1Xva5TdVXiNsHjW0h_mOi1BSIFo7EzUCtLW-zLUV5lO7?data=QVyKqSPyGQwNvdoowNEPjVpPboYa0ioUFVtKkPAY2mq21KJUlQS9zG9cYuf2iXOwjYXTqRTLJfjpvRXMaSYAL7w4a_6kGnswDOcgRx1dn7Yz-IH0nZRT_ldVl2kiZuA0Qe5DBp3_1dESNlirlA1Lm4XcydVZBbF5iKlXIv2iMasgg07VeUignZJyEtLa5CrodE08jeMLTSncl7Yb0gkJpc72DWA6GwblMv3csYWNy-Ydlw-NKRGYFXYcyYUGGDmOLpxZ-3NHuVo5KMt1OKWhP2DH_vyqT793rWUPN7sDQWG8Zg2LzgPm3oOFmTehKe0QhVNrTGb-Sy1-h4bQrfx7_5Pwlp1QEpohjvzp_jpJpyWeg4AzSZNHfIuwOUkkjjcD9j1g5YCXRyV1TSNUFpV8Fzcch-qGyQkjTYWvhRYoCbEOoRDiQH0ciEz44ExwzdRg0986hUmgOsQ,&b64e=1&sign=192527f2415ce494d79597930acee03c&keyno=1',
                        price: {
                            value: '91990',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopInfo: {
                            id: 74832,
                            name: 'Phonempire.ru',
                            shopName: 'Phonempire.ru',
                            url: 'phonempire.ru',
                            status: 'actual',
                            rating: 5,
                            gradeTotal: 2738,
                            regionId: 213,
                            createdAt: '2011-09-13',
                            returnDeliveryAddress: 'Москва, Юных Ленинцев, дом 83, корпус 4, Офис 16, 109443'
                        },
                        phone: {
                            number: '+7 495 221-79-11',
                            sanitizedNumber: '+74952217911',
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8fRxCJIIEUi0XNYM59FBJ-icc75j8rbtInnGHIsMWsmTxrkVnl23jYhU013MXFUvY_uf7PUzJOdRkP9Grf31FOv2a69Di5rQPKYi9JsIkC-LXCnQ5jxh_ag68TrldQ73VVoy0Bl5MuVVp2ueYMoBJuZJu4RKqEdTpcFBvUdQ5Bite9s00D-UMXwpXzdS1W7FuSFrp_sLkybhw2YBExwXRR3mXWvyntDx5OBTUsdcgxKdEnWQaGg4PJgFeJJhonU4qQuHAQOaLlHtu80EGdm8Fs_pgmG2T_fpNm_rOaEPtUDVsqCJHya7mTs_nnhSabEresdWMI33m7LGMUw9vqzyaVVk2iGRNFpKD1zFY4CjHjC-ujT_9X286W2jbhSgkjRDCE1JoRcelmpkyC1pI9WN9HzUSrd5biOBedcAPEv70zwPGydPh2oc1CaPuvp710L-iBYqjIO-DEcZRODk_WR42-aK8W9hS26HeaEAynw2UzDjAGRHNoEpYIgvLxlIL4jLy5PM5VqB9N9sEdLzp3NKdpPALumvyZbqlqaJz7MuCnVO0nN3aGhYl3WxdWw38FJe94rsO3afXR93hWrfkJbrb6nLSCL9uktRJgxExZkjNOIdqZeGcdpjFVdjegks8TMq9Ju-gkSo0CcHWxyWGezoTkoDJqyP1sgDqOrGOZyONOxhHB7K772PI3ftNc5cErw0WKQWw8HWU3T0c_DZRpkvtahr8G5Y5SBwagrQJTe2p8vMW0Fh92n-tsQZJ81_rMzH55jZ3XtocW5q3dAL_MVQ7NT6EF04NqRa31HskoPiWtaepDWN9S-YtWNOu9HqEAvV17AWctz5rKZOx8BADTG2YLfYEYkt46kNs?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O84g3V9WzXhpMJsX5ODJMpiqzShBOst1ajAS765fIhBVwj4JpNGjUmDBg9jzQ0WTtXM7tj84JmQIY-Ss-5jGhZDAzVUStjxEel0D6cKMYqm6z5BmGhef6QF0Ea5SRvLNTvnByzKG569PxeuRh9qvllgAJvkI-turMWHqR-Qb1V6Gw,,&b64e=1&sign=0f7281f63c9604ea4c5377d23e58afea&keyno=1'
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
                                        value: '390',
                                        currencyName: 'руб.',
                                        currencyCode: 'RUR'
                                    },
                                    dayFrom: 1,
                                    dayTo: 1,
                                    defaultLocalDelivery: true
                                }
                            ],
                            brief: 'в Москву — 390 руб., возможен самовывоз',
                            full: '',
                            priorityRegion: 213,
                            regionName: 'Москва',
                            userRegionName: 'Москва'
                        },
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_j8dSDt6hFZn4QuaIXWC2wQ/orig',
                            width: 800,
                            height: 800
                        },
                        previewPhotos: [],
                        warranty: 0,
                        description: 'смартфон, iOS 11; экран 5.8\', разрешение 2436x1125; двойная камера 12/12 МП, автофокус, F/1.8; память 256 Гб, без слота для карт памяти; 3G, 4G LTE, LTE-A, Wi-Fi, Bluetooth, NFC, GPS, ГЛОНАСС; вес 174 г, ШxВxТ 70.90x143.60x7.70 мм',
                        vendor: {
                            id: 153043,
                            site: 'http://www.apple.com/ru',
                            picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                            link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                            name: 'Apple'
                        },
                        categoryId: 91491,
                        cpa: 1,
                        cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgbHb6joFcwnl0GEDJt_cLT7d9YGEiK9jA3lyiu1cpeJtkEGnwVDCUMY2Cc-81-yFL2Dux3DJMGKlSFbpoUz44uzYouk5oXtOsK6EPtw85GOiG7YIDQfZYbLA8-2f7oVMNWcbwbvTBWRsC3sTa4VbZZ2T9SWdGQGf9GXCwYD1HTbR8c1qKcVeAEpuhuV76R_xgEctfGEmCl5BJAx1uJN_2olHgWzhPtvwNL9Ei5b_zOp-LEND-YeNyglUxssEyFj5469PlY-RLCGnqfp34i4ZjJQvLCf_Lu9cdGlZEUVIZeFdxAC3_BdNK8pH0t1bIoGyfU2lgWlT8CC-PeXn3HbK36D5HKi4cMoF_qTqAeBsJ0klB26BhwJ4ZM9svVAF484ArrwhF4fA9w-haRc0Fbj3TPnzowO9adPIvIOj56eDbr1WorYbZTFQdqSIolFJdWcCurdDbHNv24P4HJLqmjimzmLtQxGPqwdpdrK_NlN8LyQo8hgGvjIDCCTPU5mcM1oQOS4go7uXHaA0jRVwPnzH4mabEQexWLWjsKBQtAmvEWNFQNKoUZSp2MS4sLzhgofgkMaWXE2eG7XSybjRubAdCSQiuzadzVU1yEiGOdxo2NXiJ2VAtmrdjQEGC1ts7sFHZrdQJHJUOsQKygHWLsedgNLqDU8gmNo_rqf0a1VrZoO2xfWN3opiqfi2OLDBQ29Y_hDFsanr59H-WE3kx9y2GAZC55MZNoauKgDOGRK1zUXfKVgg9yoS8WMX-bq8S0eqnwrK5mPOKeR3NOiiHP21CcDidI8req1Z6VXwNRL7OzijjFgA6veGZcXFIsjpFuWkj113pU-HvD45ABCemRmPAwVvTgGOkf68e1B8O5OcKcGakoNlBcBQAGy6BedO-5bRKiHU5Dww7kia?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-620yvVzTqRVPhBZ31cCTd7YzxdqZWnuzJc-1U4f9LANjLsBzf6UByvvp5GKDza9l6CjxDdhbGsRK0bJmqBwBHmsiQYzCZUmVNLNVIiK0cUzHUuVbU9LKcLWaLmFxgElNyrd0tP2K_ZPo8tY046uexwKRGRNO1mr9Nhuql1j0r-1UZl8IQqkboPlieyiCdrvlAGfpvxcpU-HN-LOOIdf3D6i1E1yHEGVkCwgzSVdrfxzJP-rCUq0hOKL7TPq5givFQOrdju6i5ywLqPt-MkcThVQUV8MCN_ZLloJjdhEMLje5Fe6l_85Qg2w8DQZHWKNPYxJHhhyKqWldtfDBhMHbEIdm_EMKk-xXE,&b64e=1&sign=d30688649d725842ea30fe782e6a81e4&keyno=1',
                        category: {
                            id: 91491,
                            type: 'GURU',
                            advertisingModel: 'CPA',
                            name: 'Мобильные телефоны'
                        },
                        vendorId: 153043
                    },
                    phone: {
                        country: '7',
                        city: '495',
                        number: '221-7911'
                    },
                    schedule: [
                        {
                            workingDaysFrom: '1',
                            workingDaysTill: '5',
                            workingHoursFrom: '10:00',
                            workingHoursTill: '20:30'
                        },
                        {
                            workingDaysFrom: '6',
                            workingDaysTill: '6',
                            workingHoursFrom: '12:00',
                            workingHoursTill: '18:00'
                        }
                    ],
                    geo: {
                        geoId: 213,
                        longitude: '37.77384292',
                        latitude: '55.70235181'
                    },
                    pointId: '297606',
                    pointName: 'Офис на Юных Ленинцев',
                    pointType: 'DEPOT',
                    localityName: 'Москва',
                    thoroughfareName: 'Юных Ленинцев',
                    premiseNumber: '83',
                    shopId: 74832,
                    block: '4'
                },
                {
                    offer: {
                        id: 'yDpJekrrgZH5zGTiKSVfEcnPwfoc2J6jIi14boRSqMqW0IwzJJoIdH4daSt_NPTSpUrhfpL1o02YiLbSdArNzuhK9LZZBj6nsiATOFu1NNsJ7OL1ZEr41fhItIv3bnTxEoSDcsyWjiptzMDAdIz9cPylTy_vnzzl48KVixgWvWSA0yWG_60qmEzcWgsSFJ9EH9wqIvzhDDVqN3YX7kXS7F0Yx3j_crnx4uXAbWbhelwi37Fi2xTcE8SS8pNDVqMvGO0wyHMcqQpXguq2EZpvz2hS6dVULSv-2d6LohPq5X0',
                        wareMd5: 'ENAqxGcH-ygSsE26Rj9tkA',
                        name: 'Смартфон Apple iPhone X, 256 ГБ, Space Gray',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mXSI9tzupePOHT_4FkaPsRRDmUQG3Kc117iksg7whyyEqMtUpVaVY1HfwcyIETECbogFd3klZXHk4SslF5P4pMqx3DLjDaHIm0LPov0lJy7haQDeFJc8tDzHWHAx6LjzhhlFBiqSJVHZKeDkbGkvtEYxg54b50YFQvZ7ouCxRRY_wYeUDA7j0xC64X8yjw0sByKRcWzPNn6qRjw877jSC8epH0MOUZTrm4PW8svDUx6GAqTjQZChEFxfswIPxWSW58VUpHhrCEwikzaUAj2O_8KGZkEXPkUto9D3gXQVvEgquWKFXzxICjB813y8X67wg5_vxKvwBr34khjWOExnNtL9nWCEQ-z7UT60-M3e5uNNxQD_hR_5OL-nTpDAIrqZbZveD-f8_hu59pJx60kUa9CyqVrqCOwu1xn2KrXMYTP0OdGoAHxGiBzJh-L2y848l3Tgk9JOlxWI4epaSIk7pS6KfOwQFgHg7E5EjTVMHSVPr1ki1Q2QukcKrJc2yrT8K-RdQSEd7jQSMja044UhcLyLY4XF2wrfhI5BeeeFMZ3ExlRir1H4qe1wt6ab38QIU8B0lXdQzDT9j3Vne8WaJlzGE2W0JzAMUZNm8amjJPARkEKUkwXFg0zAQXiwT9on0uA5l4Cq2z75C2Kxsr48pffbvnx4Oon3-XLqqXIS64gscfVgO3UQIw-YAlhHyOONyfyhZr2cNyzfoybbVM4qwOKix8SdgBF8rH5JKaW1GEJrTLlgVl7-dTHfzX1cJnon0Bc2lZerwP_1iH2dC0KSz-ydNDf42WVWpapp0wsxK8llRH4X2bVEACwQ7U-CDQCHa1QQ3xkg1dgfRKHoH_HDcn1WuOE_Kej6wWhegkmD4fQc?data=QVyKqSPyGQwNvdoowNEPjVX89eIOJ_Cmt4eiV66YfMWEX-kvVn8UeTAkHveBpPQZ7YWurgwdcP__QCy8KLtUgARjfNcmx3ituMNr-jfxnzhGhRyxHM81EydhqI4EWmE4O24iZnaxsWbpLOdkZbmFIkyo2mxKoqGRoXfnJYjT8ZnU60T_IlkZswQjeweTE52YmRCbrXIBQuiu1IeUOwNfWOX_2O5I4UUTCbkUUTFvo3nEVaA-qGgJBA,,&b64e=1&sign=a1940e0c68ffe885347e9ef330a4d0cb&keyno=1',
                        price: {
                            value: '82500',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopInfo: {
                            id: 411945,
                            name: 'Phonestore21',
                            shopName: 'Phonestore21',
                            url: 'phonestore21.ru',
                            status: 'actual',
                            rating: 5,
                            gradeTotal: 286,
                            regionId: 213,
                            createdAt: '2017-04-11'
                        },
                        phone: {
                            number: '+7 495 9999-421',
                            sanitizedNumber: '+74959999421',
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mXSI9tzupePOHT_4FkaPsRRDmUQG3Kc117iksg7whyyEqMtUpVaVY1HfwcyIETECbogFd3klZXHk4SslF5P4pMqx3DLjDaHIm0LPov0lJy7haQDeFJc8tDzHWHAx6LjzhhlFBiqSJVHZKeDkbGkvtEYxg54b50YFQvZ7ouCxRRY_wYeUDA7j0xBKD6QxGnR6jEz80Dni3N7PI2aOf-iBeMafwt0Zu0BmJ-buS6HyOomzdO8Rvlen_gCU2YBWxf7oVtoSHT0v3tEGk6G6b_gOh3NQDrJOWqAIfLk7jrsCgvMPPTKMykK0UsvgKFmuBXR7UDOabXWOsmRsbuJSW9-tejYBhGT8vaVqZZDlAr9kezI0EYpulho4p3oj2PBkWjjuScqO-GelnV6jOzMTBys91vJpRtwcQQy0rCt_SnHQTRL_qjTji0DuM6Urpi-5cWYmf5Vt7Msf4D-XdSCPrcUfY41CDGwZaqkOLijO0iMNgf9hVv7bA2_7gfWe65QGlj1GAajzOKcbKgV2Ccqgu5Fpx2lvCYcxgAspd_YU0xAJ_5xntl9l9CiudxMJy7J1K7Qc3zIMEpSA8PEdga-AZ8Mb9JqSkiZAWpFaBsqmwqTuyKHiJq2vUd9qdTbf4yPBNKoBoYmNhtJK20-5q1nS1-6rOM7NkMZfpyC2bB2vwbDYNeqOmCveamZAylEqWQxro9CNMBKqWuvfhIWlm4MbE6Rxvb53lMn5xiXulhlTD-ckybIovEI6KbGIOJbKHiaitFSDk0UNm03lm1_4IIfObELJu38qW17cJ03iGoIVBGroVq4Jc1rvcc2SZ2yKE51fUWwbUjJK6UUNxhR77d4Gdx_9CQXK4Cu9rV2BmgtXpj4YXKfx?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_ejPCdqx_TR7tZuMw6NneewY3pT1gTJKvqyPCzx_PY_bTc5K1btL-yPuaW7vqg-LxuLrdBGfUHFTF1osVqBncXVqU-7z-zsqkXrVwvJndnH9VJRTO0J6xdBbN_nwNfimQwSfKtkvTUKCkkkx1prIcYjKtjYlRWKJNDdQr86O5nKw,,&b64e=1&sign=a9da07f22aafebb9a5aa9c17ebaa9b63&keyno=1'
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
                                        value: '400',
                                        currencyName: 'руб.',
                                        currencyCode: 'RUR'
                                    },
                                    dayFrom: 1,
                                    dayTo: 1,
                                    orderBefore: '18',
                                    defaultLocalDelivery: true
                                }
                            ],
                            brief: 'в Москву — 400 руб., возможен самовывоз',
                            full: '',
                            priorityRegion: 213,
                            regionName: 'Москва',
                            userRegionName: 'Москва'
                        },
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_iqc3KVJT-ntxNBfaJYkZoA/orig',
                            width: 400,
                            height: 400
                        },
                        previewPhotos: [],
                        warranty: 1,
                        description: 'Смартфон Apple iPhone X, 256 ГБ, серый космос Цвет корпуса: Space Gray Память: 256 Гб Размер и весТолщина: 7,7 ммДлина: 143,6 ммШирина: 70,9 ммВес: 174 граммаЭкранСтанда',
                        vendor: {
                            id: 153043,
                            site: 'http://www.apple.com/ru',
                            picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                            link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                            name: 'Apple'
                        },
                        categoryId: 91491,
                        category: {
                            id: 91491,
                            type: 'GURU',
                            advertisingModel: 'CPA',
                            name: 'Мобильные телефоны'
                        },
                        vendorId: 153043
                    },
                    phone: {
                        country: '7',
                        city: '495',
                        number: '999-9421'
                    },
                    schedule: [
                        {
                            workingDaysFrom: '1',
                            workingDaysTill: '6',
                            workingHoursFrom: '9:00',
                            workingHoursTill: '19:00'
                        },
                        {
                            workingDaysFrom: '7',
                            workingDaysTill: '7',
                            workingHoursFrom: '9:00',
                            workingHoursTill: '16:00'
                        }
                    ],
                    geo: {
                        geoId: 213,
                        longitude: '37.644684',
                        latitude: '55.716458'
                    },
                    pointId: '717589',
                    pointName: 'Phonestore 21',
                    pointType: 'MIXED',
                    localityName: 'Москва',
                    thoroughfareName: 'Павелецкая набережная',
                    premiseNumber: '2',
                    shopId: 411945,
                    building: '2'
                },
                {
                    offer: {
                        id: 'yDpJekrrgZH5zGTiKSVfEcnPwfoc2J6jIi14boRSqMqW0IwzJJoIdH4daSt_NPTSpUrhfpL1o02YiLbSdArNzuhK9LZZBj6nsiATOFu1NNsJ7OL1ZEr41fhItIv3bnTxEoSDcsyWjiptzMDAdIz9cPylTy_vnzzl48KVixgWvWSA0yWG_60qmEzcWgsSFJ9EH9wqIvzhDDVsLAI8Uo1sUcsgjHKFbvhQ0gFwFFbS9Hi5UtqDuMQ1V6OB7CjSRJsm-SKI_sE3hz--GCpeV9sNlpaUAWGyAIR9ef7C2BvrSuo',
                        wareMd5: 'ENAqxGcH-ygSsE26Rj9tkA',
                        name: 'Смартфон Apple iPhone X, 256 ГБ, Space Gray',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mXSI9tzupePOHT_4FkaPsRRDmUQG3Kc117iksg7whyyEqMtUpVaVY1HfwcyIETECbogFd3klZXHk4SslF5P4pMqx3DLjDaHIm0LPov0lJy7haQDeFJc8tDzHWHAx6LjzhhlFBiqSJVHZKeDkbGkvtEYxg54b50YFQvZ7ouCxRRY_wYeUDA7j0xC1oKKIPg9b_dEl-lzdUgvGJtDUE3CboVZJ8vrYiUbOVbT075WX7fIVoR33KnB7bsNwhRVpr5LQCB4aCAaFM06x5-5fD4HZbUaKp_MlACSObErVCXma6Q20QjW-A6v8w42WBS6Z3E89JkpO39cvy2rE4ixe9RxryXfa2ycH1pqGy9OVO66t3kOnuJ26lSaULk96q8jmYsp_I8QnFJogDUbtthT2diyEba3NxvmPzvP2T2rW9Jhu14u4xtIZLP7sJyfoV5EaYocsSfNbaDFtRjbjovlbBH2YHP9_IraHRGcE4jzmrz-aV26XuLAK52ORk79Xza5TN-JxCGhCWzRzQwTRwHpBpTBwWAYIMwjzKF_pxZtff6IOtFLwjMM-PFbdDFaIy3i4CsWel_jk1bOHSfDFyZNon5OR6IWL7YDWvxeyUirxdxnOhitFntaKLeNR59vPM-QYPG55NN4AcWY7lbq1WRWCXNYBOUl3Z_etEJeX-H5zYbReqYOuEo2woHdme_0_w5CHbsLnRsTv7-LWx742Dy5MQzKI9RDR7AFiu-rPGynqUb_s_xiSvuTXRUdi7ZVZX-0hXP8_zua9d5-OWzR3aMX44bckjXubygXl35LRTHn09NevogoHrxBYNWRiUhf4JMmoRAfbYaHUX6ke4yd0QfDqEf0zPcaU4Nok9kMj6OR1KfqTswpq?data=QVyKqSPyGQwNvdoowNEPjVX89eIOJ_Cmt4eiV66YfMWEX-kvVn8UeTAkHveBpPQZ7YWurgwdcP__QCy8KLtUgARjfNcmx3ituMNr-jfxnzhGhRyxHM81EydhqI4EWmE4O24iZnaxsWbpLOdkZbmFIkyo2mxKoqGRoXfnJYjT8ZnU60T_IlkZswQjeweTE52YmRCbrXIBQuiu1IeUOwNfWAnGJMkCeXAwQMCnK0GUHaWaqWIypxOu1Q,,&b64e=1&sign=e28fd99c4b71685db9381a6a9e6a6629&keyno=1',
                        price: {
                            value: '82500',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopInfo: {
                            id: 411945,
                            name: 'Phonestore21',
                            shopName: 'Phonestore21',
                            url: 'phonestore21.ru',
                            status: 'actual',
                            rating: 5,
                            gradeTotal: 286,
                            regionId: 213,
                            createdAt: '2017-04-11'
                        },
                        phone: {
                            number: '+7 495 9999-421',
                            sanitizedNumber: '+74959999421',
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mXSI9tzupePOHT_4FkaPsRRDmUQG3Kc117iksg7whyyEqMtUpVaVY1HfwcyIETECbogFd3klZXHk4SslF5P4pMqx3DLjDaHIm0LPov0lJy7haQDeFJc8tDzHWHAx6LjzhhlFBiqSJVHZKeDkbGkvtEYxg54b50YFQvZ7ouCxRRY_wYeUDA7j0xC9AlGVQsaWcV5LHfugDdxjPFl4JYv0LEBR07ACYD2GJ42c5EVv9gU6np2yJxMp7Yf8FcbrkUWSdKBAG3EPqLO6ho9ITAu10v6tV7A2F6h52Ts_M7rOS5iUnjKgj6rZcsoP1jsAQ_sTiVBelLbquiYXGctkxPBGdzZ-mPhj4JRmqfozj3_05yYnvQbPxgG8O9J6DOgiKAB0DJzLDomRfdGiOWlssRmc7FiaNHZa5279kIao9D0n8oRnsvnJhfXHlh3eTjc6Agz_yD7WTE_oFpL9tQhIvHb36s4X9fQpVN4MVbegyQcsNjrQdl2doyf5GQ70Os0lhRbbKcoOEpJ3SYP387EkiPcv7Svt8K9xHk0XG5c74gSvfsbSDizkAs2jBBCVYbR6VMxD_qNIgkOmUBPRLgQ7CsiZU15BPw1shTeqvydHg8WhV-xRb-n5w19DXN8is0b57AbC2eSB05CfJJxT6_O_EwTrg2tofyrbPso--WxWu_c_cpHNouloPgbxuaNkGB-0ZT5x3OdZon5yxwQu7TCHndZvUqvtSpXwvvidP8go467_D6mh4JLDvu_CbC8oIEkix2OsQGa9g2HaCGt9N1-EsBfJF67ruSxMNoKI8a13WOm5qa_ZEWWQVFJn0WjOFmFN4Le1fnTAsjUcRImFN5gAKmuoaH_iCzZJmyqZvhL8B9Fk9vWo?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_ejPCdqx_TR7tZuMw6NneewY3pT1gTJKvqyPCzx_PY_bTc5K1btL-yPuaW7vqg-LxuLrdBGfUHFTF1osVqBncXVqU-7z-zsqkXrVwvJndnH9VJRTO0J6xdBbN_nwNfimQwSfKtkvTUKCkkkx1prIcYjKtjYlRWKJNDdQr86O5nKw,,&b64e=1&sign=fa26d54b2a499c8ef2ab6b909b8b8b14&keyno=1'
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
                                        value: '400',
                                        currencyName: 'руб.',
                                        currencyCode: 'RUR'
                                    },
                                    dayFrom: 1,
                                    dayTo: 1,
                                    orderBefore: '18',
                                    defaultLocalDelivery: true
                                }
                            ],
                            brief: 'в Москву — 400 руб., возможен самовывоз',
                            full: '',
                            priorityRegion: 213,
                            regionName: 'Москва',
                            userRegionName: 'Москва'
                        },
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_iqc3KVJT-ntxNBfaJYkZoA/orig',
                            width: 400,
                            height: 400
                        },
                        previewPhotos: [],
                        warranty: 1,
                        description: 'Смартфон Apple iPhone X, 256 ГБ, серый космос Цвет корпуса: Space Gray Память: 256 Гб Размер и весТолщина: 7,7 ммДлина: 143,6 ммШирина: 70,9 ммВес: 174 граммаЭкранСтанда',
                        vendor: {
                            id: 153043,
                            site: 'http://www.apple.com/ru',
                            picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                            link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                            name: 'Apple'
                        },
                        categoryId: 91491,
                        category: {
                            id: 91491,
                            type: 'GURU',
                            advertisingModel: 'CPA',
                            name: 'Мобильные телефоны'
                        },
                        vendorId: 153043
                    },
                    phone: {
                        country: '7',
                        city: '495',
                        number: '999-9421'
                    },
                    schedule: [
                        {
                            workingDaysFrom: '1',
                            workingDaysTill: '5',
                            workingHoursFrom: '9:00',
                            workingHoursTill: '19:00'
                        }
                    ],
                    geo: {
                        geoId: 213,
                        longitude: '37.558077',
                        latitude: '55.847745'
                    },
                    pointId: '2424644',
                    pointName: 'Phonestore-Север',
                    pointType: 'DEPOT',
                    localityName: 'Москва',
                    thoroughfareName: '3-й Нижнелихоборский проезд',
                    premiseNumber: '1А',
                    shopId: 411945
                },
                {
                    offer: {
                        id: 'yDpJekrrgZEYCWkLb1r5iDafzSNUsXGmhEzP93tQjhYvuFQ2r2x_UmD0eIoM7tvmnPC1j6WCaSmk2g6DMIo4Dnqeg1NpZKdo2hYyMYvLxw75gRnp3IR1-4BKDdOErb8J2tV9To2pcAJV-Vf0qKq_qTCGhsnDjniCMPWteIm9XvPUOOsCdDNU5Di0fGuM7cxiL6oE8ZNQR9mi369mTAxFk4jkb7mX68U6XtOYlBhc6ooZ4RSsHpxVnOxVv9JfAZj3XY1mofDcIQFYmb8mSWqyfBTQvFjLIILC-SXA2XZqQV4',
                        wareMd5: '-aujpEVtjhp9p4VxGT93PA',
                        name: 'Apple iPhone X 256Gb Silver',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRvy3ypcvE38AdxiO5mefm348ZxsFrkzWBcT1VesRHM0BpekmhBNFiJckdH7ZR2Bl_xZcxndUuW4_Eo-5eynXaIXE8UKSCVaejFcDs_ue1i0_Aah0wGQROgt6m065Fdp2V1-kY8Q1Zkbox8ppxYaiAYEa8pnO2NDDWsTuFTZE0uuLZ6c_kyew5w0QirE95WB6Y48LoAJ93UZc6FhXzBsK5j7tGPjxfjXQglz5XF8HJ01J9Tyw7R0uBrmmpX9pOTtwmKIXCypXZvQiXw0Sk5QhKqCkJACwmeU8GFbHQ64LZx4zXhknKFajdg97jEKKBDQlrvDX4WxKaJAejfQfoIRRc1SGgqLK_TcgVgMdojEt1ywzuxjHEcIu1CpBvy2cI1GBGomX_H55YLePFio1b4BFpE6Xe1wAzEJ62SVlkeCxQZVR53wM5v1O72fTdsehLPooOXFn5YTcA9M1tHtIUcXYrjeLSeC6uz-_Q9tWyk00koYP0k1IT9C6z8dLRZaa1gnj79p1VSfPMaP0lztzMWVuKfDdpG44NB1MqjRWUfgytQA_amy-KJ2Qx3BPSZJ3LeFyjGKL01vTYn0rtFPrZpQXL-jrHbRB01Rn4lcMnxBjMJgqO1LgW9sNEOMQUyRYnD0cKpUEGJdcIhqAFaeIfwmsTbepb_hqunGf4SDBUb2P4GOs4aB37dC97t6wuJ1K3FF-3Ies-8FrLqsO_9vFSQeCpZ1f-4TdiVhdrMtyaAGqb9HiwXcxZq3LljxkuVtl5A-X-LNbHkcNiDfxnkcHvrgxMpu-vzStOUjyQNZCIIdBgLExOXte761ws_CIaE3Eny6Cf3fHFXBB6LIigGSdyRtUnGpEs-0OSZ__Jek99LSIyJo?data=QVyKqSPyGQwwaFPWqjjgNkgiu40OdXmpeHBz9r9FXzbn3IBXsKP8ZgzdpclTfCHo0B9DlC-eNlIqCJD3a_F-bASvwJYZTF6OXB44j3HoaY9txZ-rSCfT0ClUaw5FYFUtimlw6yWjHTZKVH1lcsJsYnnWqV0cNMzrDZS_ykhEitHr-mflZxp2d4-tc4wsvI2KodjFZbyfDPAms5XBJrnyK8kOEWhDMnAGy-dBSL3SYieMxXI7NjWwEZ-tpgzYZD7SU3bIk_ph4rNvxqEAwyZbwmjvid1_SpQM4DCwwLtUiO8,&b64e=1&sign=ec822cc874c3764a0655716f7f007162&keyno=1',
                        price: {
                            value: '81990',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopInfo: {
                            id: 307420,
                            name: 'itUnite.ru',
                            shopName: 'itUnite.ru',
                            url: 'itunite.ru',
                            status: 'actual',
                            rating: 5,
                            gradeTotal: 70,
                            regionId: 213,
                            createdAt: '2015-08-27',
                            returnDeliveryAddress: 'Москва, Багратионовский проезд, дом 7, ТЦ \'Горбушкин Двор\'сектор F2, пав. 007, 121087'
                        },
                        phone: {
                            number: '+7 495 226-19-24',
                            sanitizedNumber: '+74952261924',
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRvy3ypcvE38AdxiO5mefm348ZxsFrkzWBcT1VesRHM0BpekmhBNFiJckdH7ZR2Bl_xZcxndUuW4_Eo-5eynXaIXE8UKSCVaejFcDs_ue1i0_Aah0wGQROgt6m065Fdp2V1-kY8Q1Zkbox8ppxYaiAYEa8pnO2NDDWsTuFTZE0uuLZ6c_kyew5w1bhMglR93TM5aBxSx4VgJ40QoGHHaxkCaxo2EXOiSx5wwrdCuaF0u2qgzRQkgbpYFaIpGH9MJ56vMwEWVyWULDY4a7ujV2Rvb7cuCZd23tggl2CClZDueIJ6gGp6auuhTuCnpO-zi-qD08NEGxwXFFfVGT2AqaCI1ytzI-wzvFSlH6lCCGxAb8iQZcnL_wssFKSOdQnzb5FTGTA-zH_Whco-wliaie4DcvmI2j6GWCgDhUTU9Jfa8OFR4JbLp0slfoxam-4_D2Yhg8MMz-YXyIw5-wa1wR5czNeongAbqSS6K13TXDYz5lLZpCVpCnlAY8WZVjWVdRxsA65bt4HsCgujFjPgIoSvu6GKtkjxEdzseQm1SV7kx3cisPYSg7kHHv2Epl6Pahl8YTPEgZ28tQkLhILk5dsKeJ1yFbw0rnm2aX1YqnkBd6oO9RGC9-3nXaurDlF7sbLVbRfaAbXQZGAtNsXTgIcUE_v21Fy_hz4gVcRNDRhIFuTwTJM4f67ezK1xRPDcGD1EQiD90y1iWW__tAZOw8dCqhZq_8pR-kFjWLrgnTDx841cuvaSocp9zMGIN-8pcSD2970sFlwpJLe9IXGz8GsGo29Veowg8ceTVghJ6NSDYVsvU1BDnV0iLRQRByxaspS5uqWtHjXjr7p_Le317ZWl_Omu9U3Hkc71-AeEmePly?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-ZPn696BT0025BxORKOnindpLyjc73lWxfb1lxvTBILgrW0ucRETxXIsw0yWFd3SXsEI-v6v4MzXd5THhLjoUJRhTiQ0lRhx7Ltj4ahpZwYgqq2J30qaETnnlw4Q2b7KgnEQ6P409Gpqsl-gOM54ddJ41VHp_Q4TsPfh0sFJ-_LA,,&b64e=1&sign=0c15bd1987e3e05f167914abc3037480&keyno=1'
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
                                    dayFrom: 0,
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
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/237949/market_xzktTIRXMT86AvYsqVh7sw/orig',
                            width: 420,
                            height: 420
                        },
                        previewPhotos: [],
                        warranty: 1,
                        description: 'смартфон, iOS 11 экран 5.8\', разрешение 2436x1125 двойная камера 12/12 МП, автофокус, F/1.8 память 256 Гб, без слота для карт памяти 3G, 4G LTE, LTE-A, Wi-Fi, Bluetooth, NFC, GPS, ГЛОНАСС вес 174 г, ШxВxТ 70.90x143.60x7.70 мм гарантия производителя 12 месяцев',
                        vendor: {
                            id: 153043,
                            site: 'http://www.apple.com/ru',
                            picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                            link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                            name: 'Apple'
                        },
                        categoryId: 91491,
                        category: {
                            id: 91491,
                            type: 'GURU',
                            advertisingModel: 'CPA',
                            name: 'Мобильные телефоны'
                        },
                        vendorId: 153043
                    },
                    phone: {
                        country: '7',
                        city: '495',
                        number: '226-1924'
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
                        longitude: '37.504043',
                        latitude: '55.74149'
                    },
                    pointId: '439856',
                    pointName: 'Интернет-магазин itUnite.ru',
                    pointType: 'MIXED',
                    localityName: 'Москва',
                    thoroughfareName: 'БАГРАТИОНОВСКИЙ ПРОЕЗД',
                    premiseNumber: '7',
                    shopId: 307420,
                    block: '3'
                },
                {
                    offer: {
                        id: 'yDpJekrrgZEqztOR-m2MUy-ZPlhoDKhIHTFCvIfpf9FLBUDCB1BmwkoaH8bY0j-pR-3SeufMbejIFO6Igk4VovEexAJhFXG8-_N3YXqh9QMvmIoTVMIwO1NIMWuGkyaQwTpmEfOdCj9Q65HEElPFocdGSaDWdLxADMhbg1P6LUvvCcDvaU8ATAQOU_ru3OQJfkfHby6WmITebvpYf0eyJFe2OWWnr8paSDTzSa2c2GT7DZMF9kZXUXdC0q_fnYNuXAJns2LiUR1ejsr_fFV_gYxjzud_1-pjhh1dGjkTqFA',
                        wareMd5: '86oTy6XmUu2sxMsOXskbdQ',
                        name: 'Смартфон Apple iPhone X 256GB Space Gray темно-серый MQAF2 A1901',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mXSI9tzupePOJB5CiAAhqHceMMpryjziCVuRpPTybSN1aahyZhRg02SbpeLFWS2DWF0Qc8PQ1zmcK96JUrcpBm3QH0mCdtASK-g_H5P4zdl1YPHyE4MJPCpwh4CpfwzRAuFF8VEWjcBHR2BZltbeciMPKN0FYjTURBrKLWxHPFs4QAj7eAWTJZF6BbrWEm2HDch2TnOeZ55idrLAwVRBsufTxyvbKlEJJ2DRLlBTUvY3-JR5QiuFe1b2-f2PyF98t4zu9hXoQs8TX2LfwwYq9fE17WWP3MFZMd1XRE49rP7UbmrCUkvGYFSuviJKUcjOOYk0Vd3nCZHQrV0mUKjMi4fxG-_SbvELxQW-tRQHBEAgyXUi3DKB_odq26_uP3A_OhsVLBjo2hai9nknDfzYIts5Ut7igXiKWhp4wVJF5z33XEspqXpKeEZ1-5M_bEA_6lFFTGFmka9SkrC_ioey4E1mkLbKAzebDbrZzlwKMuVd0BdJCQ9rKGJ3mxtzbz_YPPNFOBc1Hca8FzaV_HbqoHV3hCCjAgDtOUgtJHz58n2Vh2OD1oGggp4oAJ8_lZoQafGn_Qn8pms3UsDwu_eOXyjku3xiVljfZOAQf9MmwboWhO-MFg10uXHq8mj0Zj5bXWHGgrZEgbCL_H8zq5DVH4y3oZu6W5Gtw4HNnTplS_0rzIo8NltoF0wkBomI6xW1G0f6WqGHebjbp30-zlciX3_F5r-4RiOne3Fy1-bKZHOBg14SRN70vc9wCFGD_-o9ixWOzFfTgKhU6f_QuE7RuSpshAe62do_M5K_eHPjqs056Iy0KKwcgWkvumcAaIsZnzxZplyA-rzo28CbgWjGPfubydsHe_zGqA,,?data=QVyKqSPyGQwwaFPWqjjgNqnG3t6z_QgIspgTFPe60Mz_U_FkJ9EAvzZcm-VpHyNDsM9th8Pt97R76Iohyi7mTLowVLF2jIzkvKI8lmIC4tGpCIQxY0iwdoXtN6niigQAhBCsET4u_MiG1ncflDJMXE96BvrkguUtNa77uZXp4Q_RviuJOnr5S99ZaJzcYtnQWh5os9BK9gkNgB_oRBZ0jShzG4KbukBbr1IKgWjcGsBUoWYMfq_2ZFH-hz_mripniRrNaQNtTr5brZrQy5P53qrEbKNpwQS5Q7pBUPZlTNvBNA_0WvuPDNoR1vXQ0GsAqZheMkto76USV6qWDrtCQDGhGjCjppe_OcZi4EjztLN0-l9u7mgX8ymW5yseJgVqlY9qhtvFQuqmBERw3uysS4oBdY8c7wsYaKGLvWWlq4Codk7tINq22kVFaIPelqljXVkhoT4sHSE,&b64e=1&sign=3aecaf6139b3ebbef89d05ab2e85854d&keyno=1',
                        price: {
                            value: '83990',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopInfo: {
                            id: 8185,
                            name: 'iCult.ru',
                            shopName: 'iCult.ru',
                            url: 'icult.ru',
                            status: 'actual',
                            rating: 5,
                            gradeTotal: 1304,
                            regionId: 213,
                            createdAt: '2008-07-07',
                            returnDeliveryAddress: 'Москва, Багратионовский проезд, дом 7, 1 этаж, B1-018, 121087'
                        },
                        phone: {
                            number: '+7 (495) 6496990',
                            sanitizedNumber: '+74956496990',
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mXSI9tzupePOJB5CiAAhqHceMMpryjziCVuRpPTybSN1aahyZhRg02SbpeLFWS2DWF0Qc8PQ1zmcK96JUrcpBm3QH0mCdtASK-g_H5P4zdl1YPHyE4MJPCpwh4CpfwzRAuFF8VEWjcBHR2BZltbeciMPKN0FYjTURBrKLWxHPFs4QAj7eAWTJZGyssBev2un8O9ldvi1608p4muVc0aYfLTVmovhVMJP_CG-FRq3KrvTCs47DaEywCVJ7cZlLNuE2v3Kv5U9l2QPmI8plrZsm-j4j9H5BGqN1mFaZrj4jTdLSeCtIzYVNffl9Etu_S85O_40jT4JhA-XkxE1kRn2NOPxrVp-Ul_H5KUFDgOCunq8F6sQfcFMpqSQuGfeLx_SKhK0-nluP485ChcxYPV1e4iXOFW-zKOmimMm_0O6ZPq7n-K_lhHRhwiFVxKvEkGecnxLbBSAflED2GyFgv11YJxV_q8jt_0rDMzEgXjFdlxrEwRstyxWt30gyLp87-57Fwr-URILwV3fYbtbVLhKcp7tMXU8JzDN8FY_ndavZoZs5VBCB8IWsug0AsgpCf34oZ9nE8a37kphR_6j-2WQbA6Apo9JY1EHHb8dLHziIIPldIIAKbaLTrT19FTtfeH0tCdpTaapA8qOKvCut43Axt8whxjvhboRWGtzYlXe-ML4_MNJJ_Y4OJfyOJ1S5BHARyKXuiW9yyrqbPa3_eIvHz4vj4w1MLN2tgX8HupbWs5ubkO5iBDgJnHbabxmvNwUh192AFJcW42mNekrWtv54QXYXfDc8UehwIK6BIYLvdUSxb-dj2kyMfvTYFy7bp_akYMHm78U_pGMn4Hrv3LBZC9lQ5ImfsnZjg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_vShBiBA4ux2rS5I7eI8nESYS96uQshVR6Zab2YPRMpAYvNcvdGaN-GMvUrDhX8R1BR4zbkkOn-FNZOKu-7YHpBHxEOOwDXeCO3i8CkehU1AKZGcO7FnLt1eNWBgOjvui-4jGZen5oZ8PJwWRfIO1xvMltR8090shcm7Kslkq2cA,,&b64e=1&sign=57822a85f07d61913e9cc28caf13d8b4&keyno=1'
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
                                    dayFrom: 0,
                                    dayTo: 2,
                                    defaultLocalDelivery: true
                                }
                            ],
                            brief: 'в Москву — 300 руб., возможен самовывоз',
                            full: '',
                            priorityRegion: 213,
                            regionName: 'Москва',
                            userRegionName: 'Москва'
                        },
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/219360/market_X-bqbyMKrSbZGoWxqksxnA/orig',
                            width: 600,
                            height: 600
                        },
                        previewPhotos: [],
                        warranty: 1,
                        vendor: {
                            id: 153043,
                            site: 'http://www.apple.com/ru',
                            picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                            link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                            name: 'Apple'
                        },
                        categoryId: 91491,
                        cpa: 1,
                        cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97PhXlUenREUCbD4FqjGVZs0--b8Y05byzSOLwX5aipDGuLMr3sUhE8UjPexPEkrx6a-jq8-KQ9vqyrQ0DizrS72TbzByMMsl8z42RekfJk6CSHb5WWs1HU-_z7BS7Z_KTBKXtBegd1wpiGqDYJmy780jC1a7X8D-efYn7__6N3oUEGkWJz_nH_ff9rtG9z-MhV7uYySJbJkjGcP-c8tQ6nHdbrP6_RMt2kHaWSXyZYnI-kKslN-WUiLS90CZMwzjW3AJzJSLrZEyj6gI-EOqzpvMrX4zc-ca_C-d8OZ_nzQzl2rF1uzIsKFSStTYvp8-4LsgIPp8BFgIlse4vbCDyYC0lhrUEtRGqbYVA5yCGRy914d_Cfq_E7MzYbG8wa6Q-Aj_FKnNqDG94KbVp-yZv6b3U8KTL1odiiM_IrqKJyDA6qwsUnpvPjXl4bnhjnbySK4yE_cJU6p3UazRJjinV2FRNW5GJwDsy0miq3V_9ozXK7xeRYWAfMs9zJHl5LK3iZXPpB9BcjeGDboLQALFFf7LrW8TUkto52SZ8jOBFSiNV7fwYwEVpWYGhlmZ0oR59PDRG9iJjPFoOVaAncVVD3asKOVa-oCYtItNiwJCqIShRwTaZA58HRgAImQDVZdLh2O7_sL8KGGmp1f1y9wFSjhSRzH2iSdxoVrWCPwxEyM-92f3umDiwB1KtZ-d25CtPg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXboE-FHcb96dVPu093sA-ODPwBH1HgD0hSCk5Cp3JluA2rRm3nfutqNzDOw00ldEfreDTAwSOd1ofrH3kkWlZxAlBwtd8POsWqhWmZHI1HVMpd-cW_pGxN8ehH8mcqPrdm7Vsookl1zlBsW2cgPUQ1Tigqf6nDOt7Xlvollt9Vp7JRqhY1qDLe8oDdOqCy158c0aPjt6LZRvCskV6wLqLvBYK-bjErft9iJHqzmUkcky7-WW45s0cBw8SysnEQ8GD8okZfI7QNMdU5Pw5gVe5jo9r3-wAx7EzSUto32iklMQ5dfPH21MA0A,,&b64e=1&sign=2a4ececd05bec870f77c842434f83947&keyno=1',
                        category: {
                            id: 91491,
                            type: 'GURU',
                            advertisingModel: 'CPA',
                            name: 'Мобильные телефоны'
                        },
                        vendorId: 153043
                    },
                    phone: {
                        country: '7',
                        city: '495',
                        number: '649-6990'
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
                        longitude: '37.502786',
                        latitude: '55.741135'
                    },
                    pointId: '265047',
                    pointName: 'iCult.ru',
                    pointType: 'MIXED',
                    localityName: 'Москва',
                    thoroughfareName: 'Барклая',
                    premiseNumber: '8',
                    shopId: 8185,
                    building: '1'
                },
                {
                    offer: {
                        id: 'yDpJekrrgZEqztOR-m2MUy-ZPlhoDKhIHTFCvIfpf9FLBUDCB1BmwkoaH8bY0j-pR-3SeufMbejIFO6Igk4VovEexAJhFXG8-_N3YXqh9QMvmIoTVMIwO1NIMWuGkyaQwTpmEfOdCj9Q65HEElPFocdGSaDWdLxADMhbg1P6LUvvCcDvaU8ATAQOU_ru3OQJfkfHby6WmIT0B6awqqO7FQRkMrrrYAOybQKwAP5r_cvfuj6qYCEIt-JmorAB5dcoigB0vystnU8Hhed83CcdZNkL1FVi08Mgo1pK6gHrc4M',
                        wareMd5: '86oTy6XmUu2sxMsOXskbdQ',
                        name: 'Смартфон Apple iPhone X 256GB Space Gray темно-серый MQAF2 A1901',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mXSI9tzupePOJB5CiAAhqHceMMpryjziCVuRpPTybSN1aahyZhRg02SbpeLFWS2DWF0Qc8PQ1zmcK96JUrcpBm3QH0mCdtASK-g_H5P4zdl1YPHyE4MJPCpwh4CpfwzRAuFF8VEWjcBHR2BZltbeciMPKN0FYjTURBrKLWxHPFs4QAj7eAWTJZFsLxdrHfMBhbJ4j6gi8TP9OQpFceAlTvsragSjM9kLGrNICbOVaEvygSIpH_EfiX8NWA_eNt7BwPl9Ho3sMnfYair7vPiXCK8c64P424y6ZFmlz_bfXmZhbDuygET60wiA0gF8Z6qOCWwkT9eNGByx2vBe4kUhuQqv2mD7tXtMsOQZEb9ZfiKC0X8WIq2kVPfIM6vK2uqzCb3HZwwW603BVxzS4ZoAkmITbczRa_1pXxE_okEW33VGFCGZyEoJNrz9IwXBwOVXsPRSVfhzHesFZZGH1kX0nLtfWWT4dYdpInc_LOF6vOY79Vum5XRmH1xsg2dGv_hSVwkaCHQoUhkv3znT9aCf5XiDd45BQQgLuwHNBXKn7yyfy9OTZpvDkAIJ888cNl66t5kNvb00gRguQp0oH5LJcpPAX5-7JoX8SDIrdh6TNwR7uwSti5cDpFkZTGCl3-SbcDWyywdu7zLmlM8Exl-92pymjEdmws-CA6I7KCnZfmo08K2aco6NCG39T5Hkmz8F_hdUakKhj9vulsAVrkpEL56BywkFCWuRmvZMLc7p8BCAu9_-f2HR1L8jaJLlKcDXMOTwWrbUQy0EeDRReBbBGETqRl-YmmiCArq3-GUDjejSNpyxi04jzyP1LOwgUR8ONtWGEoAmPZr_3ZjptCA_aQgfzNNeMgW94Q,,?data=QVyKqSPyGQwwaFPWqjjgNqnG3t6z_QgIspgTFPe60Mz_U_FkJ9EAvzZcm-VpHyNDsM9th8Pt97R76Iohyi7mTLowVLF2jIzkvKI8lmIC4tGpCIQxY0iwdoXtN6niigQAhBCsET4u_MiG1ncflDJMXE96BvrkguUtNa77uZXp4Q_RviuJOnr5S99ZaJzcYtnQWh5os9BK9gkNgB_oRBZ0jShzG4KbukBbr1IKgWjcGsBUoWYMfq_2ZFH-hz_mripniRrNaQNtTr5brZrQy5P53qrEbKNpwQS5Q7pBUPZlTNvBNA_0WvuPDNoR1vXQ0GsAqZheMkto76USV6qWDrtCQDGhGjCjppe_OcZi4EjztLN0-l9u7mgX8ymW5yseJgVqlY9qhtvFQuqmBERw3uysS4oBdY8c7wsYeoBXJ0wxEwIubOG1Km-Kq9HQ4SUa-3Iz-x9BJrW88Uw,&b64e=1&sign=5451365bc87ddd008354400f37ab31d9&keyno=1',
                        price: {
                            value: '83990',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopInfo: {
                            id: 8185,
                            name: 'iCult.ru',
                            shopName: 'iCult.ru',
                            url: 'icult.ru',
                            status: 'actual',
                            rating: 5,
                            gradeTotal: 1304,
                            regionId: 213,
                            createdAt: '2008-07-07',
                            returnDeliveryAddress: 'Москва, Багратионовский проезд, дом 7, 1 этаж, B1-018, 121087'
                        },
                        phone: {
                            number: '+7 (495) 6496990',
                            sanitizedNumber: '+74956496990',
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mXSI9tzupePOJB5CiAAhqHceMMpryjziCVuRpPTybSN1aahyZhRg02SbpeLFWS2DWF0Qc8PQ1zmcK96JUrcpBm3QH0mCdtASK-g_H5P4zdl1YPHyE4MJPCpwh4CpfwzRAuFF8VEWjcBHR2BZltbeciMPKN0FYjTURBrKLWxHPFs4QAj7eAWTJZF4GHfHVhQuLOlExBCCOS67e1uNLJWI-nqVPWbmSljOQuYzkawFFqrxV4BtoriCqoYmDC4Ysx5S-O60rsb0AgCRYZjJgapHyneFnNg4-UNGVhgvW9rj8pR2w5C0UbJn6rr1_ZY9Qb2ykVZQ-JVrO_6Defk1sTlj1h_93NApOjPQfBG6O1O7YNOoWS0soQY8tJWAaN0zhIB9U0imNWN4oela2qo2TQU7cFiqjYEJKlqjaHJmYY734pAeK1YLsv7M5P-dpP0DOdpJRIAC75FFBizQgWLf_ARpYuZeeB4qi3rOnvxeCPiPbXh2hRfPhVrcsCLDSdH23OYSvRSUyDnKGHRvOrCv00AhTIzS3IosW0xoWKfTsxhkLDcMAOFBbAzm_B3SVOlNgjLaqZXz1Rm3Oe09qh5wg5xsrHmlT-hQ0axXeDbd40aByNR_xsgZ0xwG7qfq2a6H_x2ITGpkBg1S46-3R0RNbePiVjxLqc-9NOUyJ3gqvNY6my5QEXy_DDpxTVRU_fbDMEb8KT9rnI86_CBZj04twVafAnrwcuAFcS44nUkQFYdko0IDmJlFAm7YhBfVSxYd3NK36Pv8DKzBr_32o9IjpqupNBhSeHF2vsVyCa9Lx-6Rf9GW1ie7dvKgcXKPWoozELojcFmR9veE6vFHKXBw5Tdr4c5M0FBZWuXesQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_vShBiBA4ux2rS5I7eI8nESYS96uQshVR6Zab2YPRMpAYvNcvdGaN-GMvUrDhX8R1BR4zbkkOn-FNZOKu-7YHpBHxEOOwDXeCO3i8CkehU1AKZGcO7FnLt1eNWBgOjvui-4jGZen5oZ8PJwWRfIO1xvMltR8090shcm7Kslkq2cA,,&b64e=1&sign=48ccbeb9a63d0de5f85b891c9b2d00c7&keyno=1'
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
                                    dayFrom: 0,
                                    dayTo: 2,
                                    defaultLocalDelivery: true
                                }
                            ],
                            brief: 'в Москву — 300 руб., возможен самовывоз',
                            full: '',
                            priorityRegion: 213,
                            regionName: 'Москва',
                            userRegionName: 'Москва'
                        },
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/219360/market_X-bqbyMKrSbZGoWxqksxnA/orig',
                            width: 600,
                            height: 600
                        },
                        previewPhotos: [],
                        warranty: 1,
                        vendor: {
                            id: 153043,
                            site: 'http://www.apple.com/ru',
                            picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                            link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                            name: 'Apple'
                        },
                        categoryId: 91491,
                        cpa: 1,
                        cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97PhXlUenREUCbD4FqjGVZs1YXHvvkmo5aTH8kgzSobSQoGl1uxrnH7S-XdSCucBhYT4P4mWxAcsfL-dVBG7wSv-K9B15_yezW6N8JEM20r0cEVEzHnC2Zg9V9BEVg8smR9zH6XOSZKIYxXs3Aya0Wo1-x6PXE-ik1GkYi6CL0EAtd1zpgb4oTEAcYUnzOAo0mS5cWpGfpoXEP8Eu3hNPjq2TMZWyRP2bGIS3YJ6G1u-pABYa1W7FEtjEEw5K96tMnM42ImQqj6FXwnUcpHCP5XFKMTJA9LWWMI3UBoUryoPenYBrPDNUbFAyBGvHbNxNba3Ta28zY4lcHQENa3112ThRgo1AjfoyrIt5XrRYwrnNV-dyNp0boiHRGf0mcYaxTYgL2_cGNulWN8P5F1f6nq1oUo8_GFFyHk5xORrvXvbDHBAuT9Bw39_AAfs7EXWL5Tl__8UQtIycRatcLoh5hjR05xkKrWDdmOBvfvJzTah4n8wmneTVcjon2lWz9VnUz4CoVbcyIeLhsboVEjqIXMzEysN5fDxR1R9BFEjyRknbBkgGG0uyvs9DwMJkKMarXmWCiqzyu0UmFB39G5BVvo6HzGpIbb-Aova_rybT9cWw6KMhNWqEo3Fdz8sDuDF94-MlYSUgOjeNwUtpUSlY_wxZNgu7_xWUwRWHF5FNYMxP2HebhOtsfysjyQ3VU-fSnw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXboE-FHcb96dVPu093sA-ODPwBH1HgD0hSCk5Cp3JluA2rRm3nfutqNzDOw00ldEfreDTAwSOd1ofrH3kkWlZxAlBwtd8POsWqhWmZHI1HVMpd-cW_pGxN8ehH8mcqPrdvRJ2d-Jj7GZW5bCmEqFMOXkXzFFhFI0psTYy3SR1B1-GXPqKt4Lvqzu-iebnQx0CN4dXJUiKMdc_KBi1EHo12Ai988J5nbMAN5w9udIBnj8yScZVAJW_fKoM9Rw1FhMYztAoTkvFIDWWYHFziGND6_kcvJzN3V5_h3ax3kBspZ8-tjsPdK9MKQ,,&b64e=1&sign=2aa76409d8c35cdac431904d053bc1cb&keyno=1',
                        category: {
                            id: 91491,
                            type: 'GURU',
                            advertisingModel: 'CPA',
                            name: 'Мобильные телефоны'
                        },
                        vendorId: 153043
                    },
                    phone: {
                        country: '7',
                        city: '968',
                        number: '793-3019'
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
                        longitude: '37.552427',
                        latitude: '55.670133'
                    },
                    pointId: '321772',
                    pointName: 'iCult Черемушки',
                    pointType: 'MIXED',
                    localityName: 'Москва',
                    thoroughfareName: 'ул. Профсоюзная',
                    premiseNumber: '56',
                    shopId: 8185
                },
                {
                    offer: {
                        id: 'yDpJekrrgZGOCNkqFRJFgkWg4FLThFP0Bl8lpOOpiULLdwb_Wrid8w',
                        wareMd5: 'iWd7Zz945Ru2xwc_OwbatA',
                        name: 'Apple iPhone X 256Gb Silver A1901',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mXSI9tzupePOHT_4FkaPsRRDmUQG3Kc117iksg7whyyEqMtUpVaVY1HfwcyIETECbogFd3klZXHk4SslF5P4pMqx3DLjDaHIm0LPov0lJy7haQDeFJc8tDzHWHAx6LjzhhlFBiqSJVHZKeDkbGkvtEYxg54b50YFQvZ7ouCxRRY_wYeUDA7j0xCdKLAfGdyLZvVL8NH4ztmaZlZhp1O3HAEcdSxunUqmeVuTdg5FMjPGF93s_KTAKowbkCql1TDguYOyTKjUaz-m-Us22zjsszCjj9qpKQo-NAjk2vnvcyZVmmuH1PiQGLQnCHHNonounE8jv2Tu0uK3DZlk-ISaiCY7x2sqZHZcMpQ5e-X85jTNXuoGDnWvIRa4pqkMuA0TmQ3MRbcDnBqz23db-Wri--_Cb4hzjbaVB6hkV3FLGjBKi_ea5EvKdYIYamKWeTBsED0ixbMYBBOof4hxl7s30SFlJMoWk1MQ3qb-CxL7cVvSiLaTBGp7uLHJXNFb4KU7Lb5niygdjaoCGJcs9P5lSAxYH3wKLY_pn1LqZaoMxeccCXiRPoacGZOfMut7BkdlHvPyrGIsRZyU4MLzf7A0NS1uxCw6OeowZ69ac2wXn6g9fY0BEXHNIb_TJdOz5K8jruDOaZ50GtTMNcwbUu2E2bHfw3zCNevHtC1KL6ZhlCHHBv3EAfZCwvDju8VjhSBgkw3NxXYRz3y9-GTWq30p2GAP2dUDBGZ1ck7qH7ZQwOMQq41Wp2oNwg1ZlaOvVfNZPsI4yFuqm5hBrs9uR4oGkIrJCLYUkErbgPFW4BzaQvkFUzVNF_uwB-EB8pYVWOYzhrPcDEzm5bhX2o4pAIpgEosEDXKG0M509Q,,?data=QVyKqSPyGQwwaFPWqjjgNjUveDTJH97ci69ROMeJn_cXsz2oz6Am9lG_asGk_2rFZifijuKKv4mqwWuxigt8bEIx-LzqADmyjfvZj07gWMbTad0GsGXX570YFnMUVDCso86LhtPiBlbl6uzXzX8wiKhfCVWbO2TBSqOnTm03hD4,&b64e=1&sign=9cd6655f407a880ba3a417673b053ff5&keyno=1',
                        price: {
                            value: '82990',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopInfo: {
                            id: 79006,
                            name: 'GB Store',
                            shopName: 'GB Store',
                            url: 'gbstore.ru',
                            status: 'actual',
                            rating: 5,
                            gradeTotal: 2803,
                            regionId: 213,
                            createdAt: '2011-10-21'
                        },
                        phone: {
                            number: '+7 (495) 7755998',
                            sanitizedNumber: '+74957755998',
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mXSI9tzupePOHT_4FkaPsRRDmUQG3Kc117iksg7whyyEqMtUpVaVY1HfwcyIETECbogFd3klZXHk4SslF5P4pMqx3DLjDaHIm0LPov0lJy7haQDeFJc8tDzHWHAx6LjzhhlFBiqSJVHZKeDkbGkvtEYxg54b50YFQvZ7ouCxRRY_wYeUDA7j0xA2EeyFe8QcIOV-KZr0wicsniFulkITCJ_-dZV_CnJtpUfZlpeTBVAjAItFGFI5WU77X2PCPydBhDkj3yURtyGnKTWKH90o1gwdUumH-GX0qFAw7kml54733cBekVEd5XidBZN4Iu_PPSWwXY_pY7QHx6QtfF36BhBZ89aqUjd64Hbc1NumkolaQQarGvVRIgdPjL-44FtE0IT2qoJoN0TKQX2rgL2myLcJZ5fOWn7CykKECDKxZ9SOlYc-QxrCjNfpVax4T0ClySzb6pE2XSWeaYeo4CuAmdL2oZckTsYht6BZkJp9xVmaxoTEl0wVdC6JXcJN30duyAwS6PPGBob-My1XNDYvdn_xbHT91IiDDNUaESHhsFQUoh8a2p5jZeemvz2eUiFWjggw6hZmdOmeEjcr95-RpOEPWpTvWRhkr8yFEvx8wqmZx3cMLLWrf0sQistO6X1f7wj2-qpy9QkEixxcc7_TRDCYaAteHgooarVpI4yLmjlAU-nV37UIwQ3s5zUean2iSR8NOnoqXepArrxAfLzxJkOmVU96cusBu_kv_DNBbynAuaR7WciN7W5PX7vs0D63c54pE8FWlYajnA7f-yYNI5p0wsPcoFo-zlhADikV_hrZzvjm08Ji6GLbpQeI49QS0R1xwWaQ3-evr4o7jlrlcpzTT8Z8D6rN_A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-a35xWe6EnaReKVogPnaTT7JyLYk0IUMFltuzVIilraiRu4C8Ro3WXCdI5TT8lfoRTUWje0ZSGs27KiCl91cf_m2TWZJjYkCh6eBVbXDGsm1PJWoktViLhWjM3VHKGRdy_1_b5jDenCs6yeDYbRMp1F7Z8DCB1FYVVNXEZKLS-lw,,&b64e=1&sign=779fe18c82bdd6d5a20065ebe96bb545&keyno=1'
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
                                    dayFrom: 0,
                                    dayTo: 2,
                                    defaultLocalDelivery: true
                                }
                            ],
                            brief: 'в Москву — 390 руб., возможен самовывоз',
                            full: '',
                            priorityRegion: 213,
                            regionName: 'Москва',
                            userRegionName: 'Москва'
                        },
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/364498/market_38gfJ0BxSl0peG5sLWIIJg/orig',
                            width: 500,
                            height: 500
                        },
                        previewPhotos: [],
                        warranty: 1,
                        description: 'Смартфон, iOS, WiFi, LTE, 3G, GPS, Bluetooth, 5.8\', 2436 x 1125, камера 12 Mpx, память 256 Гб',
                        vendor: {
                            id: 153043,
                            site: 'http://www.apple.com/ru',
                            picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                            link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                            name: 'Apple'
                        },
                        categoryId: 91491,
                        category: {
                            id: 91491,
                            type: 'GURU',
                            advertisingModel: 'CPA',
                            name: 'Мобильные телефоны'
                        },
                        vendorId: 153043
                    },
                    phone: {
                        country: '7',
                        city: '495',
                        number: '775-5998'
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
                        longitude: '37.502786',
                        latitude: '55.741135'
                    },
                    pointId: '253015',
                    pointName: 'GBstore.ru',
                    pointType: 'MIXED',
                    localityName: 'Москва',
                    thoroughfareName: 'Барклая',
                    premiseNumber: '8',
                    shopId: 79006
                },
                {
                    offer: {
                        id: 'yDpJekrrgZE3uBHCmJ0j3UKt5g8C_l9LW0p-gZy2vaFx5KDU4yh06W7zvz1s4JtYlRehYV0y7iWt2bRONQCmPfZhU-LWcE54PqtZ2PRmcf1Lk2m-9030aT3H97cGcSvnkUTdlo92R89xHpJMOa5wfdyn4ADnj72-0DOq1Ftg9odD9yjTCu6BqedxEKx_WzBTFjUUK7RBNkE_lSrTvxc6MuiQQ_30mTqTW9tvefYTTJV7j4-7bGT_-rvffPkaXl862GSTW4WmxK8ghadtwe1GAsp5ZkDm2uVm2-0RyLdeh1g',
                        wareMd5: '_exNeMYl8amUfBTuQTinnQ',
                        name: 'Смартфон Apple iPhone X 256Gb Silver (iOS 11/A11 Bionic 2400MHz/5.8\' 2436x1125/3072Mb/256Gb/4G LTE ) [MQAG2RU/A]',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRvy3ypcvE38MCKF0SFZ23a9TDdez93PC9Rgh1d1HdUZFqmwhjDQdp-paqgfBjoxisYWh8PAScuCO2J_zT7xy1-I-Jiwt0tQMVMWL04vPUim7Gmc0S3l_3ti-1tvgkkZvXSksRVr1EcHMf0_rVNm7EhZ2fy5QBIGM4MLFPgOmozAcQyBs82Qu0PcRmCegC5zbPRfv3var4cz-yuVm1BO1Gbr_ETnB3WZpY3MpBKndR4YLrowHLLxMmgyw69Qd1vD3dpRDU6fbNV5D3SPgxUMkk1A-lxbPyBZaUw-t20xUa3QAnHrR2p7ivtQFozMAobZqd1_Nhq9pqIWBDxpbi0reY3AQc1OqNWzUGKsjooOX8SYYRuE055E4Z9In_ivGbGGciQI-xFmqOs7_ul4KXW_zn22_Ht7AAR3EqDKv8NH9GUG5dv2C9d5Linlt_RRaMhxj5u67R5FpWXclyoA-G6pS0n0bzfzhmlQuzOOFNjJOesZi-siz3DVzNNv-bkfkz0YoL5q9sbn0pZU09U5GAO3947jTbPdnTf6fa82135UVgBJ1SswCQQmU_ACaqD7xcFZfQ7ym9PBs_KFKMy9ORevrcM9oy4KAlnPpYQrOi9JcILN2S9s0r0u52HOUdJHdi2NCabOZ7oZEAERr49YD_GgjrMk0g-HPjs3vdGJngjxHyJIliD5mW4aeDrvqSTxaf_9t8AoYGsGM8FKc9BVka3Mk11eTeIqZbDe9DUlTlEwQg15uKBZVmPrR8_kG8Z21rbbydYGppHbJbnGAB8u2MBKD_cogFPUcZQQFH3UxipDJbY6UjO0BwMBu_lV0deIUZyV3-klsWKYX8zOXoIWIJ6sH5t-jPwFsr-TYw,,?data=QVyKqSPyGQwNvdoowNEPjRFszCtQAnqImxtkKwlR54xlozQrLYZO7XWmkzAhm4ysYWi3l8FVgte1NJ5WewQqUqzmYvFTrubeMKXfaqMdOebjkcVMDr6494437jtRGtyOobeXUSDMscUAo0gkotqLD1p39oDIXrHEWL0g2TINnBUV9srWO3mO34MwVfLgSjfbaVOnWXk2Jnet43aj_0K4CkrRPayskUW6kofeBgpQC5B218nZmt0ha19vr-wWBddrRdKCUT5Y_XAjkHo5b0QCXYLr7ADQo8fn_YWyJBWmlqDqBC88msh0ZB-Bp6ol57O5yHccCJ2LK6s3mXBz42jJ092yBsMoNZ5HCvZYKglVNEM7hqHzFlTBhRMBocchShXE1DBKeRGEnl6CZOXHFTW9Wqva5k54a-l_HJ5TO6I0Rynmg24Vb0tC1f_uaBaeUTstqfWBnb5qnMt1q2n5W5uLKRY0m0ZQA0ydeVCRD5o1l2lsCJ5gAtUIwwvbXjxfVUQTeh0ZsIOgYvkRrnddF1ClCSmFaN9H7JA_5gWVSkmGERDvSW2o_9Y1VnPYhBMotTB15Qv_qhmD28OOokax25glEd2FMAg1Poty62oHacTLr7g1dEq-Ximysh5F1Q0-RhRJWsrNYUqByF5uPZPTjyMhkj5UK3iWO-lZnRiGRPTsm5VNcWC2A4DYTcSf9EZREzQj9GdFNZ5CZ8Gi4afgBcKDIPPWerm5E0LFkk-ayiQaNyYtIbe5DF_ILw,,&b64e=1&sign=e7514f2a93c6d3a50e890f2482c573ef&keyno=1',
                        price: {
                            value: '89490',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopInfo: {
                            id: 826,
                            name: 'НОТИК (notik.ru)',
                            shopName: 'НОТИК (notik.ru)',
                            url: 'www.notik.ru',
                            status: 'actual',
                            rating: 5,
                            gradeTotal: 6400,
                            regionId: 213,
                            createdAt: '2004-03-16',
                            returnDeliveryAddress: 'Москва, Новодмитровская, дом 2, корпус 1, 1 этаж, помещение магазина Нотик, 127015'
                        },
                        phone: {
                            number: '+7 495 777-01-99',
                            sanitizedNumber: '+74957770199',
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRvy3ypcvE38MCKF0SFZ23a9TDdez93PC9Rgh1d1HdUZFqmwhjDQdp-paqgfBjoxisYWh8PAScuCO2J_zT7xy1-I-Jiwt0tQMVMWL04vPUim7Gmc0S3l_3ti-1tvgkkZvXSksRVr1EcHMf0_rVNm7EhZ2fy5QBIGM4MLFPgOmozAcQyBs82Qu0NkIoUFgefOzPKDPcyLFD_iyGGLEGAs_MyoTxhnMk7_dLEYsT6d0ioQSi2QmjRtx29DQvU60z5IOwWYTNTDJM4kCdYvKoey6eC27Uv64wf2qvrLkXzmmeg90H4dCLdwlrBWPt5K3NYuclfruoTSFjt-Rv6Jih9g7MPx4GeD4m41D6XYRFySVIfPxJtmY9cJgtESxfflNM1ri7viOSMbVNX0YJHRmfO2hLro6GSwdslyudFKFwfdQVfqBFmRJtd3IxStJ7XTghVupihaR4Sz_d476GhNaYyaK8UCK-Ubo3Jz7UDsUA0R1_Niz2Qoe1KxF1Z3i_6TfenkrLlOyrOY7JIF_0vBrOi16MBReyWJaHU0MaOckvXsXvQWwL8NE1AYTrHlQXQ5IbJMmSDEUdecwQ3KMg78QfGxO6IAlcdyBCDpAv7tztpNp4cQ5N33r14z8bl4jZjCoSvYl_16sIRG0bbRr1XiDlUUgOzWL6wNxeLExRfXhbebocTHeHF4NygsTN90EUFzvnjo-jgHDiffB8KMHj7p5Q1LxLTivt-WnsyMs0i2oumfXVam3ziNvgh2SMMp7rt8npE8HanHJrWXR0dIi2JMryPBrFm4BmpLDO5KKBltTTMMbcoqe6C3zxFkprtI7b6GL6JP_Ur-IcgS6x6JXKP4brXBSi43QwLcfr0LDg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O82Ek66EfLGWKLUq3KQhG9T_-q2bhh3jRMiE73D3jLYX5MGwdxArT2Lt8z4RnLsSErgy_jd7TfPK5RdMN-yE0jVEV3AwEtRxTRPbJ26oEmQevD0ugoQSo77MnCmD2lDTch62ZKvhykJ4VhijvwAlm14USG-PGkEcOhTHJuL_atUGQ,,&b64e=1&sign=0aa8c204001359795de5dc07a0f131ba&keyno=1'
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
                                        value: '350',
                                        currencyName: 'руб.',
                                        currencyCode: 'RUR'
                                    },
                                    dayFrom: 1,
                                    dayTo: 1,
                                    defaultLocalDelivery: true
                                }
                            ],
                            brief: 'в Москву — 350 руб., возможен самовывоз',
                            full: '',
                            priorityRegion: 213,
                            regionName: 'Москва',
                            userRegionName: 'Москва'
                        },
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/362398/market_--Oz57NfBXeBVkWMuvHonQ/orig',
                            width: 450,
                            height: 350
                        },
                        previewPhotos: [],
                        warranty: 1,
                        description: '5.8\' Apple 2400 МГц 3072 Мб Flash drive 256 Гб 0 iOS 11 бат. - до 21.0 ч Серебристый',
                        vendor: {
                            id: 153043,
                            site: 'http://www.apple.com/ru',
                            picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                            link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                            name: 'Apple'
                        },
                        categoryId: 91491,
                        cpa: 1,
                        cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97PhXlUenREUCbD4FqjGVZs0DalQtdIGlwX8uzmbj1wojAqbq6swAc6NEXD8PtFTjVS7rQ99OBNSQmhCMLMNiEpFgYSPV0p03bQSGGw9alkWPoITeDBmo5RkuiU5lhwFM9Qynu8gJMtmbMPCbljPrwNThiPtXozKSpETQeNkVFxmRinhOd6F8PHku4POUOzeKgrBdtpNDu5OEgscPxUj5uJIJzEEwDNGXsUBdx7Z4-l7NEpqppevnDm2k4QAFzEKbcoxDR-TXbvhOSr-5ztyuOKdinyJri9iRl15AkvrVbcWk7Ej91kZ9nqI_8mN5FvDIuRR3cYT_P83k1YzzTAUM4Xdl8L_wdNX7Bq5yIE9GC2S9rVKLFXxePH38ufMVEJmH4hhRxx-iFiqEFPEnbLQsiPf_Y_8KXv49T8Gj5XCWRnT8ediddvUoX11paVrloNkg5dQ77sduw-1XZ6WnXWvdxec6M6G3714tlKR0DBjkZFW-i0lXk1Xs--TFRNxRhC188ir8T0B5gzMxRqbGVeFyPGoAMIz-ZNvYhHzOx_FZrlX_JlOFXyo0oNuIqovWE7mY6HRU4fZzgM_IK247R_wKQK53B8TovES7t-GmIxuk8RlAPWHDmDjow1WnZEsJwNTEDgz3PbBsYk0896uACIDyvO03FiBTIkoV21h69m0xQ4vD3ctHCYvdn1h3irti-zTomw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXcb28IYq4zyw2yYSWIynDg1rn-twZuwYpq1cnKbSMdbqadugf8WriOGP8U7dVzcfC6ysWqqrP4DVdadbIfdEA_DTGn0U0hHvz7p3JVbIoyk5R04R1yxa9xSproyIGgisBT15YrE4OREm59pK6OHyz1jA47FbV4yt1OYgpKiervSPx8mv_o33LbJVzwEql6g4hN25QmS8FyBHLCTyQPLDSeKD7YVPPrq2XTq5ydMOqHXEWqXLcg_nghi3W-yz4PR12eDDZwSEnXK9j5SnE4e2_9IbcuLl9cEfqFwXWgq3UN4poV8OpDK2xZQ,,&b64e=1&sign=d0cb06b42732742efcb494e41adf8777&keyno=1',
                        category: {
                            id: 91491,
                            type: 'GURU',
                            advertisingModel: 'CPA',
                            name: 'Мобильные телефоны'
                        },
                        vendorId: 153043
                    },
                    phone: {
                        country: '7',
                        city: '495',
                        number: '769-9803'
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
                            workingDaysTill: '7',
                            workingHoursFrom: '11:00',
                            workingHoursTill: '19:00'
                        }
                    ],
                    geo: {
                        geoId: 213,
                        longitude: '37.633392',
                        latitude: '55.788778'
                    },
                    pointId: '1216',
                    pointName: 'Нотик: Магазин на Рижской',
                    pointType: 'MIXED',
                    localityName: 'Москва',
                    thoroughfareName: 'ул. Гиляровского',
                    premiseNumber: '68',
                    shopId: 826,
                    building: '1'
                },
                {
                    offer: {
                        id: 'yDpJekrrgZE3uBHCmJ0j3UKt5g8C_l9LW0p-gZy2vaFx5KDU4yh06W7zvz1s4JtYlRehYV0y7iWt2bRONQCmPfZhU-LWcE54PqtZ2PRmcf1Lk2m-9030aT3H97cGcSvnkUTdlo92R89xHpJMOa5wfdyn4ADnj72-0DOq1Ftg9odD9yjTCu6BqedxEKx_WzBTFjUUK7RBNkHMcQrOsOO1_NYfb-tZmwFsSZ8gxbd9F_b-Squ2xRYiBgFEdGVD1wH830_qnt8xYD0PlCHKq1mqiypJhok61YsSrSoZdxe2Ios',
                        wareMd5: '_exNeMYl8amUfBTuQTinnQ',
                        name: 'Смартфон Apple iPhone X 256Gb Silver (iOS 11/A11 Bionic 2400MHz/5.8\' 2436x1125/3072Mb/256Gb/4G LTE ) [MQAG2RU/A]',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRvy3ypcvE38MCKF0SFZ23a9TDdez93PC9Rgh1d1HdUZFqmwhjDQdp-paqgfBjoxisYWh8PAScuCO2J_zT7xy1-I-Jiwt0tQMVMWL04vPUim7Gmc0S3l_3ti-1tvgkkZvXSksRVr1EcHMf0_rVNm7EhZ2fy5QBIGM4MLFPgOmozAcQyBs82Qu0O6xsqDpd-uNs2AA1cwWqJhlMI3ScOe6SwYFRRSO8ZQ-nCN_E5F8wyKV9WDlitrpbznP3TL4Q3DvnS7UOGj5XuNXyPNXQq5R62Xy9bM7b3ETCBsJ3p5gXtfPY0rgYxPo_rO7XO6RufGHqAG0amaI7IEOlTcR4WsuZgaNYReg8zo8yItOjNcfE0-fqM2l5ufgZ4HPyMuQ9HH-ArjUc1SdVal19P3RcsGhDbIAY3EK4GygOs5N6WgdwBiiBNgkQJ83dNkBjp7xq664GpWJEyCfthXgLws3j0ULzmHc2Eh48PvxUfrQPx49kQ77Mg2LzNTR6RuqPRXeNcGHy3MSCOIG-kYCCRqKGmuiAOnmQ9xRK_6yarTRtHL2qw53E9hpVmfy-Tozp2eVCpiST319QCGYNJu2DmyvO1HLyr_LfCQjCY6NJhZAUJNDO20BE1VSgBTgo9lZJFE9HzxECN0SKUtkYa2UPuuvy-fC8NNRjFF-13k2d-rOnOk_wSpxqpBTrKbyNhvRz-NNZ1KMx3DLSOcvaLKxAgC1XbT8fHk8v3Mhpt4zJUGgvBvlj_TnVLjVDAw5dLq0FvNEVgnvUuxhP_hm53EQDjnLy4eNtBVY2913o_oONDpmqaFjbw2frtmiOvUUeKecclX-KXGv_uK7-AXCXzgrXFxSi_E-cjx5RyNjiTH8Q,,?data=QVyKqSPyGQwNvdoowNEPjRFszCtQAnqImxtkKwlR54xlozQrLYZO7XWmkzAhm4ysYWi3l8FVgte1NJ5WewQqUqzmYvFTrubeMKXfaqMdOebjkcVMDr6494437jtRGtyOobeXUSDMscUAo0gkotqLD1p39oDIXrHEWL0g2TINnBUV9srWO3mO34MwVfLgSjfbaVOnWXk2Jnet43aj_0K4CkrRPayskUW6kofeBgpQC5B218nZmt0ha19vr-wWBddrRdKCUT5Y_XAjkHo5b0QCXYLr7ADQo8fn_YWyJBWmlqDqBC88msh0ZB-Bp6ol57O5yHccCJ2LK6s3mXBz42jJ092yBsMoNZ5HCvZYKglVNEM7hqHzFlTBhRMBocchShXE1DBKeRGEnl6CZOXHFTW9Wqva5k54a-l_HJ5TO6I0Rynmg24Vb0tC1f_uaBaeUTstqfWBnb5qnMt1q2n5W5uLKRY0m0ZQA0ydeVCRD5o1l2lsCJ5gAtUIwwvbXjxfVUQTeh0ZsIOgYvkRrnddF1ClCSmFaN9H7JA_5gWVSkmGERDvSW2o_9Y1VnPYhBMotTB15Qv_qhmD28OOokax25glEd2FMAg1Poty62oHacTLr7g1dEq-Ximysh5F1Q0-RhRJWsrNYUqByF5uPZPTjyMhkj5UK3iWO-lZnRiGRPTsm5VNcWC2A4DYTcSf9EZREzQj9GdFNZ5CZ8Gi4afgBcKDIEv_hW321xDBakY1YIV0hFcVBZ5MlSiIXA,,&b64e=1&sign=6c15d6a0dc113c47c9711e7ed7dcba21&keyno=1',
                        price: {
                            value: '89490',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopInfo: {
                            id: 826,
                            name: 'НОТИК (notik.ru)',
                            shopName: 'НОТИК (notik.ru)',
                            url: 'www.notik.ru',
                            status: 'actual',
                            rating: 5,
                            gradeTotal: 6400,
                            regionId: 213,
                            createdAt: '2004-03-16',
                            returnDeliveryAddress: 'Москва, Новодмитровская, дом 2, корпус 1, 1 этаж, помещение магазина Нотик, 127015'
                        },
                        phone: {
                            number: '+7 495 777-01-99',
                            sanitizedNumber: '+74957770199',
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRvy3ypcvE38MCKF0SFZ23a9TDdez93PC9Rgh1d1HdUZFqmwhjDQdp-paqgfBjoxisYWh8PAScuCO2J_zT7xy1-I-Jiwt0tQMVMWL04vPUim7Gmc0S3l_3ti-1tvgkkZvXSksRVr1EcHMf0_rVNm7EhZ2fy5QBIGM4MLFPgOmozAcQyBs82Qu0OV7m-AVbDfMeahQxMr6MllLFxa-pqVEBFVNikP5IDm_fkXYjDD7EXXhy_gPZ7IZ6l5npB2rj8gTxvH-7dg_dburaYZjS9IEmMVevoABuj4_VoZ80YwG2AjxhNzBg1wqCN4FCdiaeug6iJvUf8ECZWP-bqIKE8offg2GTiYHNBNejlW6XLJBd0QeJZSc1lzU-VUC9U4oArt28LWR4sSgB5-KiQOKpvGI1wnwwk55WI_NNzL95F6QMtYWK3eOefOCNRh63gdjNUYm1Sw7iiC30DjD-LBIG1IKQVtXx3H0RBwDkvOYrJuARkFR_29JpwoS9gWynSF-UN98wlci2cQr8vs6kHnVyqpc7zj0EJbepoEfU33oPcIh_sfSoXUkayYmoWIXAeCmRi1YCslnngd51-QhcaqsZVm3yeQ9hCJ6hv-EIN56cTmUXo2TB_Uvo94tSIIIjUtNFk25kWWgXGylipHHJihCT964L5f2AMUeOCi9hljAnOAy2Vlc6j6d84-yidLf09mNc7xN-IPpkONRNOUgJw76ZXBgpJXh5gcesmGQeORj_m3gpTVxvDqTu0zax9i0uiTLgrbbSCMNBa610AgW3YhYaeFsLRt4vhXyyi_4ql4sTZmVuCTu2Pae0Rw_nP3KdU6ckYp8BCwvFVQd3thJXj4_eg0dKZ3tSSkq4n2dA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O82Ek66EfLGWKLUq3KQhG9T_-q2bhh3jRMiE73D3jLYX5MGwdxArT2Lt8z4RnLsSErgy_jd7TfPK5RdMN-yE0jVEV3AwEtRxTRPbJ26oEmQevD0ugoQSo77MnCmD2lDTch62ZKvhykJ4VhijvwAlm14USG-PGkEcOhTHJuL_atUGQ,,&b64e=1&sign=90ba4073e5709d727f99b035df1f1809&keyno=1'
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
                                        value: '350',
                                        currencyName: 'руб.',
                                        currencyCode: 'RUR'
                                    },
                                    dayFrom: 1,
                                    dayTo: 1,
                                    defaultLocalDelivery: true
                                }
                            ],
                            brief: 'в Москву — 350 руб., возможен самовывоз',
                            full: '',
                            priorityRegion: 213,
                            regionName: 'Москва',
                            userRegionName: 'Москва'
                        },
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/362398/market_--Oz57NfBXeBVkWMuvHonQ/orig',
                            width: 450,
                            height: 350
                        },
                        previewPhotos: [],
                        warranty: 1,
                        description: '5.8\' Apple 2400 МГц 3072 Мб Flash drive 256 Гб 0 iOS 11 бат. - до 21.0 ч Серебристый',
                        vendor: {
                            id: 153043,
                            site: 'http://www.apple.com/ru',
                            picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                            link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                            name: 'Apple'
                        },
                        categoryId: 91491,
                        cpa: 1,
                        cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97PhXlUenREUCbD4FqjGVZs2VxK8mBVJIWbee9zJyxTvh3pjTwg5gz2joIPFDn0SrO6VnztuYb-d1d_eJyTD52sMx2Qju5R9JjTfvSQbNA-kgO40FYd5OW3dFaioZwD0a-vQH1b6sdvUSlzY9i7oBOLQcTQHzao1lMp0-anvMrUaw5rvxEIunjwAmyBZ75rKVtwZEkttmSD7ptpdklZethL1UoXp15rwHHxhSaQXxR-6zoqgLNWzek_-pJJu_azKNTAZzRCQlQ7wPVQ0DtEzlOWDMZbmI1KS16I1Hi-yfqDg83FGER31p4cFiVI8LTqob-HRKHMp0NSXWiCiLhiISKSQKMxVJ3I2uZ2CF15WxIpr-Jvb09R0CJ4GMipRbCYxzO1B_1Pr8Xi41WwhKxJI3gdTstrmIE7mpH67dfk4ib_twToSkCtN5atEqiEgyREC6Y7D1HQaRrIODuIEPA91DA1szBU2kyQKqaVDJcj4uQExPK_LgrFjHQX40cIsU4GZRHs-GXH2bXGFg69G5bv5lFmZwOxcqzTfV1luHNdE8yie9KMEdQZ3-gcssvr6twIbMRneMo1ndZJJ1rv0Xz9TUxXFSicreZaKfm1vfPFnOHE22SLsJqSRsT-faA4cgAjlQVupP4n9bDEAxnaWIA9LQiZiNsAlKQmI11k9nZBmbcpiTLgHjN5H3fhH3eMMnDfWahg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXcb28IYq4zyw2yYSWIynDg1rn-twZuwYpq1cnKbSMdbqadugf8WriOGP8U7dVzcfC6ysWqqrP4DVdadbIfdEA_DTGn0U0hHvz7p3JVbIoyk5R04R1yxa9xSproyIGgisBYgTlIMgTKoq6e0zYTmN7AP4ZxAmD6-Hs48wOxPdJ-_TosFyL_B7cr6Kw7DrK9q0eqlnAAiEj3Ae50XxtFrKVB9ZRiMj_eV_IQem5VQiyX_IpXhX4cYRfZXY1iRg5120EuOUQ0S3-mpcAEAXLD-QNEu-u4G3JxVeonA1KnrUSxyQibcl8o1_wdg,,&b64e=1&sign=ca16df3a7eeb99091b70da8b89af9b8d&keyno=1',
                        category: {
                            id: 91491,
                            type: 'GURU',
                            advertisingModel: 'CPA',
                            name: 'Мобильные телефоны'
                        },
                        vendorId: 153043
                    },
                    phone: {
                        country: '7',
                        city: '495',
                        number: '769-9805'
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
                            workingDaysTill: '7',
                            workingHoursFrom: '11:00',
                            workingHoursTill: '19:00'
                        }
                    ],
                    geo: {
                        geoId: 213,
                        longitude: '37.662407',
                        latitude: '55.708617'
                    },
                    pointId: '142565',
                    pointName: 'Нотик: Магазин на Автозаводской',
                    pointType: 'MIXED',
                    localityName: 'Москва',
                    thoroughfareName: 'ул. Автозаводская',
                    premiseNumber: '5',
                    shopId: 826
                }
            ],
            page: 1,
            total: 1562,
            count: 10
        }
    }
};

module.exports = new ApiMock(host, pathname, query, result);
