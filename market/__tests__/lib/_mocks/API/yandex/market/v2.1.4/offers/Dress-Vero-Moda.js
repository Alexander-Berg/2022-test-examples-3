/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /\/v2\.1\.4\/offers/;

const query = {};

const result = {
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
            id: '1526392540217/7c8be6dda45f26abae56d301435f78c3',
            time: '2018-05-15T16:55:40.972+03:00',
            marketUrl: 'https://market.yandex.ru?pp=1002'
        },
        offers: [
            {
                id: 'yDpJekrrgZEWeVlmKSaNp6A8p3visGwUDg1N_2bzXOB7bbcEjN2LCA',
                wareMd5: 'yLSpeiVOGRqfJuQvSBXv7g',
                name: 'Платье Patricia B. NY1102-4 желтый',
                description: 'Изделие полуприлегающего силуэта, выполнено из тонкой ткани (не тянется), с широкой юбочкой с карманами и лифом без рукавов с декоративными складками у горловины, застегивается на молнию, вшитую в центральный вертикальный шов спинки.',
                price: {
                    value: '3250'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZoMkyjmSYV573DsPvInCHnU8oZKVmWDLyGJTzt09TmI0-ZTXFm1Cmlb7ajmU88dhO0mddnfYVwTV-fh4g0jSB2_DvDFaHxCLl5kagZf8PBeScp6UZ-XCI-M6IKDH9kkSHxxeGanaMb8PfxdU09xQEXUa933Ngk8lbkWBHC7dKW6C3ddoeiCYsSe_G9aWzJrA0O2iIJwMhngtRM0AGd0lf71ltRoGetfkYkStLlUvlJ2eKV8bqiWYNW3NH8w9gIgTVzuYVr5MoSQ9iNIqosirwDzdAryWQmWWuz8TkdFIYWyPz2IfNFV1wnTIqiKuprJgGffupi5uRDt-XFJ5DV2w-XPZ5_MK5shxgymnEQhWvOEjTVN2Cin1TcktxQo5GAtVCDUidDEID-O6Z3S_NHULYIut6X8vEZwk7dHRfQIfoKIM5up4FT-cZRNMx9E598n8Fvx1sfZ1PZPpdeTK5CpCXRApecJgmfZGkGNe3T81ff5v4eSFzR4dIVqIUwHKcDpp98_dHld1Dya5GMtB4TT-hMvZy-lnCv4f0n0FkNuP50sXAR5Bb1hJbtBZDr7bFk5TuR2xM99Uqcm9OOf2zmmVXrbQkbesJZEOGaGtK4yH0xLvZ0z9X8NTkCqzLdQ3cntcGJCsLUPnT9j48q0YfHVVfrFyRSLTy3lCxgXK5ypMaFO-ioiDzqNRn_P-TaMCuzNiZVHocJ5KRByQuPviHEnMbAfFugUhB54g302YeVinwUy-0aZJV0GCFI55unzKy25fZhkxkhF_YT64Ql9By0M0neFIWWJZeIYktO7q0uDVvn11k0p9IXQ8gFa08Y4oQMQfYA,,?data=QVyKqSPyGQwNvdoowNEPjYcA4Bk7rPDi8lUvfS7ikngijEjgIxvtzlD4cIeHP3u3ySmNcZLn-lNzj4JPyqJecFjCJqL_V2n2GcaLU3xYclsEuOzjpjwGH14zbfRx4feHvmANUgY435OwLMIRka5tpSOLOjfTTTA05-Q6cnqgO0JDxkeCSl8qljYGwZVDN90st3iYVxLhGM1qkrJAPKTxW5abp150_BF9GGSjjn6CFOL6e0GHQtzalE_9La0JnQ7tXL9QD3g0iKzkPyAv2Q4GppSB9GRARV6Th1arD3Q4dY5SMY45nqe7M3HK00HY0j8rb41HRuajLUtUfrVIEKEGdj3ByccqUBrNoVa6QGQmSiq-431ur5Qa662M0oZgriMfK8IgTPgywgG1qxd3F2jbVkMrdu6FYoeW8PylfN8hvJuNRPDBiw7jbGxbIMPqaYRFG8-XOOoUGCigEcFKYh-ZMcKg_U6vS8SLHs-xvl4Rh9o,&b64e=1&sign=4bf615376c621b93906ed062b3dcd7e2&keyno=1',
                directUrl: 'https://www.kupivip.ru/product/w17071398285/patricia_b-plate?reff=ymarket&utm_source=ymarket_cpc&utm_medium=partner_ban&utm_campaign=ymarket_cpc&utm_content=ymarket&from=ya&partner=ymarket&utm_term=market',
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
                        count: 9771,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 599,
                                percent: 6
                            },
                            {
                                value: 2,
                                count: 182,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 326,
                                percent: 3
                            },
                            {
                                value: 4,
                                count: 1425,
                                percent: 15
                            },
                            {
                                value: 5,
                                count: 7257,
                                percent: 74
                            }
                        ]
                    },
                    id: 302690,
                    name: 'Москва KupiVIP.ru',
                    domain: 'mos.kupivip.ru',
                    registered: '2015-07-24',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/302690/reviews?pp=1002'
                },
                model: {
                    id: 34539477
                },
                phone: {
                    number: '+7 (800) 100-44-99',
                    sanitized: '+78001004499',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZoMkyjmSYV573DsPvInCHnU8oZKVmWDLyGJTzt09TmI0-ZTXFm1Cmlb7ajmU88dhO0mddnfYVwTV-fh4g0jSB2_DvDFaHxCLl5kagZf8PBeScp6UZ-XCI-MrK6pL355D4CbLswu3l3D3eRqHKcGux73Brg8vEuBSd4hKW4khLihFr4gIJL4JViD3nz7cUL3Pm69RCcO5I2exbbIxha2WJI43uCH7yGVm00e2tPiYo6I_E0i7-MnZZwJmXZ9lEA3835u-K5cdRy0_fCwyBTUXxJz3z2YFGD4Ob8wL1RZFzBCr9A7Fen-flWgvkdeUkyejZPPyJuxI6j-jqDjDMWo7_b7D05BszMCsA81ftrEhNcnNjTOWM5pPBYLVahYFBSOz7-BumlhbVxyd2Zn3feG6ZBz6EkKHSf7OCiOEzyUEX8_sBDLniLLxMTjSf6lgB5Vpo5U1Ca7bHppBZRxUBiw0y6EInZivu3gqYh3RjesWvOswBs9yBVQXoO_wdPfy-8GAax9WMF5Z5q9Bfeo2wUePkLzw9s21E2V7de9kxGIl0vTGZnUtoQuLN5k1J1_lh5k5uVzuJbLjd1YaVFaaXxKMZj3OKEpZr5G7iUdKrKbqFh046XGXOR2goaqrozHyjgLhPv24Ox9Vks0AMWCi0uj1r-NjliNAhCAmlouP49MMv_AZhaq7Ys5cHB6fHO4Yq4SwWoaBIiioSl5UfBjPhPY7dxp0mSI_dYq35WDS-Di0_P1ukvEVF9L4RzlJuswENREyAH1-t3zxR-dK9iv4VCGSf41ORVcIPpKKFzu73vyc2WHkccjLwHHgJX_mZpy_JqYtEw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9eax5iu-hiyB9QMVBsPpRzCfUgJViWToX7XHHon4lrFZk1DuQvJSaM41GeFzcHymAN1J2e2W7IjgU3wfeQvS5F91K7I1pHj2cERyz2RiJtAuSt94As2tY-ZRq2Y8NiybsLGRcDWcBwkYeVpWRL1Ujx3j76MORNSQe_4UuxmevdcA,,&b64e=1&sign=eab90892613ccc6d1603b5e063ea9e80&keyno=1'
                },
                photo: {
                    width: 533,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/236853/market_Oaq5psyMlG1A7gjP0L8b-Q/orig'
                },
                delivery: {
                    price: {
                        value: '399'
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
                    brief: 'в Москву — 399 руб.',
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
                                    value: '399'
                                }
                            },
                            brief: 'на&nbsp;заказ'
                        }
                    ]
                },
                category: {
                    id: 7811901,
                    name: 'Платья',
                    fullName: 'Женские платья',
                    type: 'VISUAL',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/yLSpeiVOGRqfJuQvSBXv7g?hid=7811901&model_id=34539477&pp=1002&cpc=yZFuqq0OJJJJRNXRkUMd6IcA00heDWjnB0OCTT_-5GQvVPRVYQBgn8k36IeTf8AdPTy7ajxeZqhkcU4F7RG1Q8ocMhak4wNI9p9Bwcli--iY4d6NAgKppg%2C%2C&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 533,
                        height: 800,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/236853/market_Oaq5psyMlG1A7gjP0L8b-Q/orig'
                    },
                    {
                        width: 533,
                        height: 800,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/228527/market_1M-HAsxFyVzuNe7FbT0rEg/orig'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEWeVlmKSaNp6A8p3visGwUDg1N_2bzXOB7bbcEjN2LCA',
                wareMd5: 'yLSpeiVOGRqfJuQvSBXv7g',
                name: 'Платье Patricia B. NY1102-4 желтый',
                description: 'Изделие полуприлегающего силуэта, выполнено из тонкой ткани (не тянется), с широкой юбочкой с карманами и лифом без рукавов с декоративными складками у горловины, застегивается на молнию, вшитую в центральный вертикальный шов спинки.',
                price: {
                    value: '3250'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZoMkyjmSYV573DsPvInCHnU8oZKVmWDLyGJTzt09TmI0-ZTXFm1Cmlb7ajmU88dhO0mddnfYVwTV-fh4g0jSB2_DvDFaHxCLl5kagZf8PBeScp6UZ-XCI-M6IKDH9kkSHxxeGanaMb8PfxdU09xQEXUa933Ngk8lbkWBHC7dKW6C3ddoeiCYsSe_G9aWzJrA0O2iIJwMhngtRM0AGd0lf71ltRoGetfkYkStLlUvlJ2eKV8bqiWYNW3NH8w9gIgTVzuYVr5MoSQ9iNIqosirwDzdAryWQmWWuz8TkdFIYWyPz2IfNFV1wnTIqiKuprJgGffupi5uRDt-XFJ5DV2w-XPZ5_MK5shxgymnEQhWvOEjTVN2Cin1TcktxQo5GAtVCDUidDEID-O6Z3S_NHULYIut6X8vEZwk7dHRfQIfoKIM5up4FT-cZRNMx9E598n8Fvx1sfZ1PZPpdeTK5CpCXRApecJgmfZGkGNe3T81ff5v4eSFzR4dIVqIUwHKcDpp98_dHld1Dya5GMtB4TT-hMvZy-lnCv4f0n0FkNuP50sXAR5Bb1hJbtBZDr7bFk5TuR2xM99Uqcm9OOf2zmmVXrbQkbesJZEOGaGtK4yH0xLvZ0z9X8NTkCqzLdQ3cntcGJCsLUPnT9j48q0YfHVVfrFyRSLTy3lCxgXK5ypMaFO-ioiDzqNRn_P-TaMCuzNiZVHocJ5KRByQuPviHEnMbAfFugUhB54g302YeVinwUy-0aZJV0GCFI55unzKy25fZhkxkhF_YT64Ql9By0M0neFIWWJZeIYktO7q0uDVvn11k0p9IXQ8gFa08Y4oQMQfYA,,?data=QVyKqSPyGQwNvdoowNEPjYcA4Bk7rPDi8lUvfS7ikngijEjgIxvtzlD4cIeHP3u3ySmNcZLn-lNzj4JPyqJecFjCJqL_V2n2GcaLU3xYclsEuOzjpjwGH14zbfRx4feHvmANUgY435OwLMIRka5tpSOLOjfTTTA05-Q6cnqgO0JDxkeCSl8qljYGwZVDN90st3iYVxLhGM1qkrJAPKTxW5abp150_BF9GGSjjn6CFOL6e0GHQtzalE_9La0JnQ7tXL9QD3g0iKzkPyAv2Q4GppSB9GRARV6Th1arD3Q4dY5SMY45nqe7M3HK00HY0j8rb41HRuajLUtUfrVIEKEGdj3ByccqUBrNoVa6QGQmSiq-431ur5Qa662M0oZgriMfK8IgTPgywgG1qxd3F2jbVkMrdu6FYoeW8PylfN8hvJuNRPDBiw7jbGxbIMPqaYRFG8-XOOoUGCigEcFKYh-ZMcKg_U6vS8SLHs-xvl4Rh9o,&b64e=1&sign=4bf615376c621b93906ed062b3dcd7e2&keyno=1',
                directUrl: 'https://www.kupivip.ru/product/w17071398285/patricia_b-plate?reff=ymarket&utm_source=ymarket_cpc&utm_medium=partner_ban&utm_campaign=ymarket_cpc&utm_content=ymarket&from=ya&partner=ymarket&utm_term=market',
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
                        count: 9771,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 599,
                                percent: 6
                            },
                            {
                                value: 2,
                                count: 182,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 326,
                                percent: 3
                            },
                            {
                                value: 4,
                                count: 1425,
                                percent: 15
                            },
                            {
                                value: 5,
                                count: 7257,
                                percent: 74
                            }
                        ]
                    },
                    id: 302690,
                    name: 'Москва KupiVIP.ru',
                    domain: 'mos.kupivip.ru',
                    registered: '2015-07-24',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/302690/reviews?pp=1002'
                },
                model: {
                    id: 34539477
                },
                phone: {
                    number: '+7 (800) 100-44-99',
                    sanitized: '+78001004499',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZoMkyjmSYV573DsPvInCHnU8oZKVmWDLyGJTzt09TmI0-ZTXFm1Cmlb7ajmU88dhO0mddnfYVwTV-fh4g0jSB2_DvDFaHxCLl5kagZf8PBeScp6UZ-XCI-MrK6pL355D4CbLswu3l3D3eRqHKcGux73Brg8vEuBSd4hKW4khLihFr4gIJL4JViD3nz7cUL3Pm69RCcO5I2exbbIxha2WJI43uCH7yGVm00e2tPiYo6I_E0i7-MnZZwJmXZ9lEA3835u-K5cdRy0_fCwyBTUXxJz3z2YFGD4Ob8wL1RZFzBCr9A7Fen-flWgvkdeUkyejZPPyJuxI6j-jqDjDMWo7_b7D05BszMCsA81ftrEhNcnNjTOWM5pPBYLVahYFBSOz7-BumlhbVxyd2Zn3feG6ZBz6EkKHSf7OCiOEzyUEX8_sBDLniLLxMTjSf6lgB5Vpo5U1Ca7bHppBZRxUBiw0y6EInZivu3gqYh3RjesWvOswBs9yBVQXoO_wdPfy-8GAax9WMF5Z5q9Bfeo2wUePkLzw9s21E2V7de9kxGIl0vTGZnUtoQuLN5k1J1_lh5k5uVzuJbLjd1YaVFaaXxKMZj3OKEpZr5G7iUdKrKbqFh046XGXOR2goaqrozHyjgLhPv24Ox9Vks0AMWCi0uj1r-NjliNAhCAmlouP49MMv_AZhaq7Ys5cHB6fHO4Yq4SwWoaBIiioSl5UfBjPhPY7dxp0mSI_dYq35WDS-Di0_P1ukvEVF9L4RzlJuswENREyAH1-t3zxR-dK9iv4VCGSf41ORVcIPpKKFzu73vyc2WHkccjLwHHgJX_mZpy_JqYtEw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9eax5iu-hiyB9QMVBsPpRzCfUgJViWToX7XHHon4lrFZk1DuQvJSaM41GeFzcHymAN1J2e2W7IjgU3wfeQvS5F91K7I1pHj2cERyz2RiJtAuSt94As2tY-ZRq2Y8NiybsLGRcDWcBwkYeVpWRL1Ujx3j76MORNSQe_4UuxmevdcA,,&b64e=1&sign=eab90892613ccc6d1603b5e063ea9e80&keyno=1'
                },
                photo: {
                    width: 533,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/236853/market_Oaq5psyMlG1A7gjP0L8b-Q/orig'
                },
                delivery: {
                    price: {
                        value: '399'
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
                    brief: 'в Москву — 399 руб.',
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
                                    value: '399'
                                }
                            },
                            brief: 'на&nbsp;заказ'
                        }
                    ]
                },
                category: {
                    id: 7811901,
                    name: 'Платья',
                    fullName: 'Женские платья',
                    type: 'VISUAL',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/yLSpeiVOGRqfJuQvSBXv7g?hid=7811901&model_id=34539477&pp=1002&cpc=yZFuqq0OJJJJRNXRkUMd6IcA00heDWjnB0OCTT_-5GQvVPRVYQBgn8k36IeTf8AdPTy7ajxeZqhkcU4F7RG1Q8ocMhak4wNI9p9Bwcli--iY4d6NAgKppg%2C%2C&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 533,
                        height: 800,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/236853/market_Oaq5psyMlG1A7gjP0L8b-Q/orig'
                    },
                    {
                        width: 533,
                        height: 800,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/228527/market_1M-HAsxFyVzuNe7FbT0rEg/orig'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEWeVlmKSaNp6A8p3visGwUDg1N_2bzXOB7bbcEjN2LCA',
                wareMd5: 'yLSpeiVOGRqfJuQvSBXv7g',
                name: 'Платье Patricia B. NY1102-4 желтый',
                description: 'Изделие полуприлегающего силуэта, выполнено из тонкой ткани (не тянется), с широкой юбочкой с карманами и лифом без рукавов с декоративными складками у горловины, застегивается на молнию, вшитую в центральный вертикальный шов спинки.',
                price: {
                    value: '3250'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZoMkyjmSYV573DsPvInCHnU8oZKVmWDLyGJTzt09TmI0-ZTXFm1Cmlb7ajmU88dhO0mddnfYVwTV-fh4g0jSB2_DvDFaHxCLl5kagZf8PBeScp6UZ-XCI-M6IKDH9kkSHxxeGanaMb8PfxdU09xQEXUa933Ngk8lbkWBHC7dKW6C3ddoeiCYsSe_G9aWzJrA0O2iIJwMhngtRM0AGd0lf71ltRoGetfkYkStLlUvlJ2eKV8bqiWYNW3NH8w9gIgTVzuYVr5MoSQ9iNIqosirwDzdAryWQmWWuz8TkdFIYWyPz2IfNFV1wnTIqiKuprJgGffupi5uRDt-XFJ5DV2w-XPZ5_MK5shxgymnEQhWvOEjTVN2Cin1TcktxQo5GAtVCDUidDEID-O6Z3S_NHULYIut6X8vEZwk7dHRfQIfoKIM5up4FT-cZRNMx9E598n8Fvx1sfZ1PZPpdeTK5CpCXRApecJgmfZGkGNe3T81ff5v4eSFzR4dIVqIUwHKcDpp98_dHld1Dya5GMtB4TT-hMvZy-lnCv4f0n0FkNuP50sXAR5Bb1hJbtBZDr7bFk5TuR2xM99Uqcm9OOf2zmmVXrbQkbesJZEOGaGtK4yH0xLvZ0z9X8NTkCqzLdQ3cntcGJCsLUPnT9j48q0YfHVVfrFyRSLTy3lCxgXK5ypMaFO-ioiDzqNRn_P-TaMCuzNiZVHocJ5KRByQuPviHEnMbAfFugUhB54g302YeVinwUy-0aZJV0GCFI55unzKy25fZhkxkhF_YT64Ql9By0M0neFIWWJZeIYktO7q0uDVvn11k0p9IXQ8gFa08Y4oQMQfYA,,?data=QVyKqSPyGQwNvdoowNEPjYcA4Bk7rPDi8lUvfS7ikngijEjgIxvtzlD4cIeHP3u3ySmNcZLn-lNzj4JPyqJecFjCJqL_V2n2GcaLU3xYclsEuOzjpjwGH14zbfRx4feHvmANUgY435OwLMIRka5tpSOLOjfTTTA05-Q6cnqgO0JDxkeCSl8qljYGwZVDN90st3iYVxLhGM1qkrJAPKTxW5abp150_BF9GGSjjn6CFOL6e0GHQtzalE_9La0JnQ7tXL9QD3g0iKzkPyAv2Q4GppSB9GRARV6Th1arD3Q4dY5SMY45nqe7M3HK00HY0j8rb41HRuajLUtUfrVIEKEGdj3ByccqUBrNoVa6QGQmSiq-431ur5Qa662M0oZgriMfK8IgTPgywgG1qxd3F2jbVkMrdu6FYoeW8PylfN8hvJuNRPDBiw7jbGxbIMPqaYRFG8-XOOoUGCigEcFKYh-ZMcKg_U6vS8SLHs-xvl4Rh9o,&b64e=1&sign=4bf615376c621b93906ed062b3dcd7e2&keyno=1',
                directUrl: 'https://www.kupivip.ru/product/w17071398285/patricia_b-plate?reff=ymarket&utm_source=ymarket_cpc&utm_medium=partner_ban&utm_campaign=ymarket_cpc&utm_content=ymarket&from=ya&partner=ymarket&utm_term=market',
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
                        count: 9771,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 599,
                                percent: 6
                            },
                            {
                                value: 2,
                                count: 182,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 326,
                                percent: 3
                            },
                            {
                                value: 4,
                                count: 1425,
                                percent: 15
                            },
                            {
                                value: 5,
                                count: 7257,
                                percent: 74
                            }
                        ]
                    },
                    id: 302690,
                    name: 'Москва KupiVIP.ru',
                    domain: 'mos.kupivip.ru',
                    registered: '2015-07-24',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/302690/reviews?pp=1002'
                },
                model: {
                    id: 34539477
                },
                phone: {
                    number: '+7 (800) 100-44-99',
                    sanitized: '+78001004499',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZoMkyjmSYV573DsPvInCHnU8oZKVmWDLyGJTzt09TmI0-ZTXFm1Cmlb7ajmU88dhO0mddnfYVwTV-fh4g0jSB2_DvDFaHxCLl5kagZf8PBeScp6UZ-XCI-MrK6pL355D4CbLswu3l3D3eRqHKcGux73Brg8vEuBSd4hKW4khLihFr4gIJL4JViD3nz7cUL3Pm69RCcO5I2exbbIxha2WJI43uCH7yGVm00e2tPiYo6I_E0i7-MnZZwJmXZ9lEA3835u-K5cdRy0_fCwyBTUXxJz3z2YFGD4Ob8wL1RZFzBCr9A7Fen-flWgvkdeUkyejZPPyJuxI6j-jqDjDMWo7_b7D05BszMCsA81ftrEhNcnNjTOWM5pPBYLVahYFBSOz7-BumlhbVxyd2Zn3feG6ZBz6EkKHSf7OCiOEzyUEX8_sBDLniLLxMTjSf6lgB5Vpo5U1Ca7bHppBZRxUBiw0y6EInZivu3gqYh3RjesWvOswBs9yBVQXoO_wdPfy-8GAax9WMF5Z5q9Bfeo2wUePkLzw9s21E2V7de9kxGIl0vTGZnUtoQuLN5k1J1_lh5k5uVzuJbLjd1YaVFaaXxKMZj3OKEpZr5G7iUdKrKbqFh046XGXOR2goaqrozHyjgLhPv24Ox9Vks0AMWCi0uj1r-NjliNAhCAmlouP49MMv_AZhaq7Ys5cHB6fHO4Yq4SwWoaBIiioSl5UfBjPhPY7dxp0mSI_dYq35WDS-Di0_P1ukvEVF9L4RzlJuswENREyAH1-t3zxR-dK9iv4VCGSf41ORVcIPpKKFzu73vyc2WHkccjLwHHgJX_mZpy_JqYtEw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9eax5iu-hiyB9QMVBsPpRzCfUgJViWToX7XHHon4lrFZk1DuQvJSaM41GeFzcHymAN1J2e2W7IjgU3wfeQvS5F91K7I1pHj2cERyz2RiJtAuSt94As2tY-ZRq2Y8NiybsLGRcDWcBwkYeVpWRL1Ujx3j76MORNSQe_4UuxmevdcA,,&b64e=1&sign=eab90892613ccc6d1603b5e063ea9e80&keyno=1'
                },
                photo: {
                    width: 533,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/236853/market_Oaq5psyMlG1A7gjP0L8b-Q/orig'
                },
                delivery: {
                    price: {
                        value: '399'
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
                    brief: 'в Москву — 399 руб.',
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
                                    value: '399'
                                }
                            },
                            brief: 'на&nbsp;заказ'
                        }
                    ]
                },
                category: {
                    id: 7811901,
                    name: 'Платья',
                    fullName: 'Женские платья',
                    type: 'VISUAL',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/yLSpeiVOGRqfJuQvSBXv7g?hid=7811901&model_id=34539477&pp=1002&cpc=yZFuqq0OJJJJRNXRkUMd6IcA00heDWjnB0OCTT_-5GQvVPRVYQBgn8k36IeTf8AdPTy7ajxeZqhkcU4F7RG1Q8ocMhak4wNI9p9Bwcli--iY4d6NAgKppg%2C%2C&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 533,
                        height: 800,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/236853/market_Oaq5psyMlG1A7gjP0L8b-Q/orig'
                    },
                    {
                        width: 533,
                        height: 800,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/228527/market_1M-HAsxFyVzuNe7FbT0rEg/orig'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEWeVlmKSaNp6A8p3visGwUDg1N_2bzXOB7bbcEjN2LCA',
                wareMd5: 'yLSpeiVOGRqfJuQvSBXv7g',
                name: 'Платье Patricia B. NY1102-4 желтый',
                description: 'Изделие полуприлегающего силуэта, выполнено из тонкой ткани (не тянется), с широкой юбочкой с карманами и лифом без рукавов с декоративными складками у горловины, застегивается на молнию, вшитую в центральный вертикальный шов спинки.',
                price: {
                    value: '3250'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZoMkyjmSYV573DsPvInCHnU8oZKVmWDLyGJTzt09TmI0-ZTXFm1Cmlb7ajmU88dhO0mddnfYVwTV-fh4g0jSB2_DvDFaHxCLl5kagZf8PBeScp6UZ-XCI-M6IKDH9kkSHxxeGanaMb8PfxdU09xQEXUa933Ngk8lbkWBHC7dKW6C3ddoeiCYsSe_G9aWzJrA0O2iIJwMhngtRM0AGd0lf71ltRoGetfkYkStLlUvlJ2eKV8bqiWYNW3NH8w9gIgTVzuYVr5MoSQ9iNIqosirwDzdAryWQmWWuz8TkdFIYWyPz2IfNFV1wnTIqiKuprJgGffupi5uRDt-XFJ5DV2w-XPZ5_MK5shxgymnEQhWvOEjTVN2Cin1TcktxQo5GAtVCDUidDEID-O6Z3S_NHULYIut6X8vEZwk7dHRfQIfoKIM5up4FT-cZRNMx9E598n8Fvx1sfZ1PZPpdeTK5CpCXRApecJgmfZGkGNe3T81ff5v4eSFzR4dIVqIUwHKcDpp98_dHld1Dya5GMtB4TT-hMvZy-lnCv4f0n0FkNuP50sXAR5Bb1hJbtBZDr7bFk5TuR2xM99Uqcm9OOf2zmmVXrbQkbesJZEOGaGtK4yH0xLvZ0z9X8NTkCqzLdQ3cntcGJCsLUPnT9j48q0YfHVVfrFyRSLTy3lCxgXK5ypMaFO-ioiDzqNRn_P-TaMCuzNiZVHocJ5KRByQuPviHEnMbAfFugUhB54g302YeVinwUy-0aZJV0GCFI55unzKy25fZhkxkhF_YT64Ql9By0M0neFIWWJZeIYktO7q0uDVvn11k0p9IXQ8gFa08Y4oQMQfYA,,?data=QVyKqSPyGQwNvdoowNEPjYcA4Bk7rPDi8lUvfS7ikngijEjgIxvtzlD4cIeHP3u3ySmNcZLn-lNzj4JPyqJecFjCJqL_V2n2GcaLU3xYclsEuOzjpjwGH14zbfRx4feHvmANUgY435OwLMIRka5tpSOLOjfTTTA05-Q6cnqgO0JDxkeCSl8qljYGwZVDN90st3iYVxLhGM1qkrJAPKTxW5abp150_BF9GGSjjn6CFOL6e0GHQtzalE_9La0JnQ7tXL9QD3g0iKzkPyAv2Q4GppSB9GRARV6Th1arD3Q4dY5SMY45nqe7M3HK00HY0j8rb41HRuajLUtUfrVIEKEGdj3ByccqUBrNoVa6QGQmSiq-431ur5Qa662M0oZgriMfK8IgTPgywgG1qxd3F2jbVkMrdu6FYoeW8PylfN8hvJuNRPDBiw7jbGxbIMPqaYRFG8-XOOoUGCigEcFKYh-ZMcKg_U6vS8SLHs-xvl4Rh9o,&b64e=1&sign=4bf615376c621b93906ed062b3dcd7e2&keyno=1',
                directUrl: 'https://www.kupivip.ru/product/w17071398285/patricia_b-plate?reff=ymarket&utm_source=ymarket_cpc&utm_medium=partner_ban&utm_campaign=ymarket_cpc&utm_content=ymarket&from=ya&partner=ymarket&utm_term=market',
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
                        count: 9771,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 599,
                                percent: 6
                            },
                            {
                                value: 2,
                                count: 182,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 326,
                                percent: 3
                            },
                            {
                                value: 4,
                                count: 1425,
                                percent: 15
                            },
                            {
                                value: 5,
                                count: 7257,
                                percent: 74
                            }
                        ]
                    },
                    id: 302690,
                    name: 'Москва KupiVIP.ru',
                    domain: 'mos.kupivip.ru',
                    registered: '2015-07-24',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/302690/reviews?pp=1002'
                },
                model: {
                    id: 34539477
                },
                phone: {
                    number: '+7 (800) 100-44-99',
                    sanitized: '+78001004499',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZoMkyjmSYV573DsPvInCHnU8oZKVmWDLyGJTzt09TmI0-ZTXFm1Cmlb7ajmU88dhO0mddnfYVwTV-fh4g0jSB2_DvDFaHxCLl5kagZf8PBeScp6UZ-XCI-MrK6pL355D4CbLswu3l3D3eRqHKcGux73Brg8vEuBSd4hKW4khLihFr4gIJL4JViD3nz7cUL3Pm69RCcO5I2exbbIxha2WJI43uCH7yGVm00e2tPiYo6I_E0i7-MnZZwJmXZ9lEA3835u-K5cdRy0_fCwyBTUXxJz3z2YFGD4Ob8wL1RZFzBCr9A7Fen-flWgvkdeUkyejZPPyJuxI6j-jqDjDMWo7_b7D05BszMCsA81ftrEhNcnNjTOWM5pPBYLVahYFBSOz7-BumlhbVxyd2Zn3feG6ZBz6EkKHSf7OCiOEzyUEX8_sBDLniLLxMTjSf6lgB5Vpo5U1Ca7bHppBZRxUBiw0y6EInZivu3gqYh3RjesWvOswBs9yBVQXoO_wdPfy-8GAax9WMF5Z5q9Bfeo2wUePkLzw9s21E2V7de9kxGIl0vTGZnUtoQuLN5k1J1_lh5k5uVzuJbLjd1YaVFaaXxKMZj3OKEpZr5G7iUdKrKbqFh046XGXOR2goaqrozHyjgLhPv24Ox9Vks0AMWCi0uj1r-NjliNAhCAmlouP49MMv_AZhaq7Ys5cHB6fHO4Yq4SwWoaBIiioSl5UfBjPhPY7dxp0mSI_dYq35WDS-Di0_P1ukvEVF9L4RzlJuswENREyAH1-t3zxR-dK9iv4VCGSf41ORVcIPpKKFzu73vyc2WHkccjLwHHgJX_mZpy_JqYtEw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9eax5iu-hiyB9QMVBsPpRzCfUgJViWToX7XHHon4lrFZk1DuQvJSaM41GeFzcHymAN1J2e2W7IjgU3wfeQvS5F91K7I1pHj2cERyz2RiJtAuSt94As2tY-ZRq2Y8NiybsLGRcDWcBwkYeVpWRL1Ujx3j76MORNSQe_4UuxmevdcA,,&b64e=1&sign=eab90892613ccc6d1603b5e063ea9e80&keyno=1'
                },
                photo: {
                    width: 533,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/236853/market_Oaq5psyMlG1A7gjP0L8b-Q/orig'
                },
                delivery: {
                    price: {
                        value: '399'
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
                    brief: 'в Москву — 399 руб.',
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
                                    value: '399'
                                }
                            },
                            brief: 'на&nbsp;заказ'
                        }
                    ]
                },
                category: {
                    id: 7811901,
                    name: 'Платья',
                    fullName: 'Женские платья',
                    type: 'VISUAL',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/yLSpeiVOGRqfJuQvSBXv7g?hid=7811901&model_id=34539477&pp=1002&cpc=yZFuqq0OJJJJRNXRkUMd6IcA00heDWjnB0OCTT_-5GQvVPRVYQBgn8k36IeTf8AdPTy7ajxeZqhkcU4F7RG1Q8ocMhak4wNI9p9Bwcli--iY4d6NAgKppg%2C%2C&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 533,
                        height: 800,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/236853/market_Oaq5psyMlG1A7gjP0L8b-Q/orig'
                    },
                    {
                        width: 533,
                        height: 800,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/228527/market_1M-HAsxFyVzuNe7FbT0rEg/orig'
                    }
                ]
            }
        ]
    }
};

module.exports = new ApiMock(host, pathname, query, result);
