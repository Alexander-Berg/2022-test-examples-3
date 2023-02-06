/* eslint-disable max-len */

'use strict';

const ApiMock = require('../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v2\.1\.0\/search/;

const query = {
    text: 'url:"cultgoods.com/products/smartfon-apple-iphone-se-64gb-silver-a1662*"'
};

const result = {
    comment: 'text = "url:"cultgoods.com/products/smartfon-apple-iphone-se-64gb-silver-a1662*""',
    status: 200,
    body: {
        status: 'OK',
        context: {
            region: {
                id: 213,
                name: 'Москва',
                type: 'CITY',
                childCount: 14,
                country: 225
            },
            currency: {
                id: 'RUR',
                name: 'руб.'
            },
            page: {
                number: 1,
                count: 30,
                total: 1,
                totalItems: 1
            },
            processingOptions: {
                checkSpelled: true,
                text: 'url:"cultgoods.com/products/smartfon-apple-iphone-se-64gb-silver-a1662*"',
                actualText: 'url:"cultgoods.com/products/smartfon-apple-iphone-se-64gb-silver-a1662*"',
                highlightedText: '',
                adult: false
            },
            id: '1514501601354/8904dde0025a715a956bf7f1cda352ae',
            time: '2017-12-29T01:53:21.454+03:00',
            link: 'https://market.yandex.ru/search?onstock=0&text=url%3A%22cultgoods.com%2Fproducts%2Fsmartfon-apple-iphone-se-64gb-silver-a1662*%22&free-delivery=0&how&pp=1002&clid=2210364&distr_type=4',
            marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210364&distr_type=4'
        },
        items: [
            {
                __type: 'offer',
                id: 'yDpJekrrgZGZGWPAtlC0aqAL5ihHLj2hk3YbR50XzlZK-IjOr75SLmcq6jJXI8DZrx9K9eM3E2UpqhMWuUH8JdCkrSOuDDuvUUAZUo00ZufIS7M6DR6gUFSSUBl_K5r9rnJkD0bLGAVEfcFQlim4vCpso7rnp-8uzZVE2tYvXs8Vwf8Wp6P4HqhOSMu0RJtctCBMg89bLBDxPCTYMAEMiWZeG4Bvcpgi8i_Ovt-s0h2JgMvuM2MBmh0HNJxdj66x-aZyhng4u_UYyi5vguDVyPpjNUyeS8CHRz9GTYnkhIw',
                wareMd5: 'nyiEPi-lA4kejgu9X_uhrQ',
                name: 'Смартфон Apple iPhone SE 64Gb Silver A1662',
                description: 'iPhone SE - смартфон корпорации Apple. Представлен 21 марта2016 года. Внешне схож с iPhone 5s, однако имеет многие характеристики IPhone 6s. Работает на операционной системе iOS 9.3, содержит процессор Apple A9, со-процессор Apple M9 и сканер отпечатков пальцев (Touch ID), встроенный в кнопку Home чуть ниже экрана. Выполнен в четырёх цветовых решениях (серый космос, серебристый, золотой и розовое золото).',
                price: {
                    value: '24450'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mf5fIUVrEGur84qC-PSK5TYHFQfwNL92WuhMrWHdRZCiuhlG3xsRNd4l4MYX6ONy1I9hDKtUnBmplsC9DXB_0Zes-RWXqu-9f6v1Q81c-cE3rOFeDHcPLIYoh1P5rF7pY0sKiWmSBJarEi05eePw7Fy6SB_mCTvKFaS27ljJWY08PUA1p65CVzrcpZVLyMeNAaxYQUmyMlbM8mtWloQO2c7pwstgXU4wWzkL9tGJgZ8rFasEfTA7_T34TI4Wl41npxZd1DEU60HUy6QBwZ_vYVBrEQ8uWZoEtwibxmfjz3p9rgnY9n8iy6ytVaqaHdpznA6I4j6y7YRc491DHngNwraPm0vnVbf8fPOX86xAs0SufF1vHQ2Rtgn97OVRjMKVno4TBC_K0dZMKXKO1j4mlqKBoLeVSqDGMA_tAI__p8JMaj5gKcUG_naF_xVOY_siFIAh2Lp5XrnMT0Y9_Oju3AM44A_03Y2_3kuniqd0UGWUJSS_Aw4A7_sp2iRCAJRsM11xV39cNgNjlUpRALGcYxhsM_16t6HI-pJV5L1DFSXzkRoeemsrkxo6-bOVQdArO8Pz-lzqAINy-nwBpnbd-XWlfzI455A1E8Ja_b_3L32CToXJeg7FACtQ_b_UapqFDUsnBwbw-j44RWqn8j5JOsurjhhYFV4dQ1vnpYHdoVjXbTQHYT3CVDuFT-HRwVKus70fPHRX6SclMjKVIJBk77GNmwgLMdPTbtYtVLqaN6NcH-bLfF4o-F9xS7O7dATi-2iLiGZSHJvyeF4o-2iK-jGvgTKrDUqnzDCMzglzAm-1kexfekdlGKZAqU9MWe9Y2bJLU_fr78zo-5ZgDVhSpYBMYAa7VC0JGA,,?data=QVyKqSPyGQwwaFPWqjjgNt96Q-I4eLvRt1Jd-FahOC1oLxErrgxA7U8HsI7pvjzVNeHCAbnFcfTqF9YngERSVUOqAY-Mf0DS-16OjPY5CR5RPoQ_p6Jar-n89K2eNeFm6ucx1ZHN0m-QfYQaUI21UNxJSrzIgzpvMMxdlK23zs7LoGPzzVJBp-TR4_IfNaIed8fjCy7Wr58BPSHmfTcmgcIeBsm0-2qafOogzQ3FTFVC1w_gskwlX20NxQaNuHZR5mcF-_9bGxXt-EwL6MNdghc20J_VWkj0Izx74IiQKX9JojCQqwOZ88UXBXXWmEQc2Wi8G4WQF1MjW9NzxBV7dt5vf2AqJpVDRNr9yw_AdfiWeZ6dLVs_2g,,&b64e=1&sign=d19431a186e01c5aed036e802fde380a&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iajeob9-Udi2hay1rqr3rV9pdQb9UnRSfgDz1SqrfBK8PwT8hsq7Q_Zmzs6Ttl91CcDR7sAF6oaGVZnyUPJHjjdQK1JfFbIjQvwNwGwyUSos0UwPIqg6imypM7rKCuz_wgciw3xeqRolE2SKBP4uBPkLEhsEuOGIHhJGZTshSfMW7Gv78PSpysz1XCZ5MBMHQ30vSS84lij05UiniFsuKg_FvqQnW5nV5OcK6m7MxnuJC9vftW3zVXNLur0aKh7PlfJ4aaQW61_-Ji4cqr9jcGYU2fWT3PNJI0T-0xiP3ztwum2JdNDRf9HGDb3L7SnHRO3mIyj2j_5zipLtBPy3WzzdG_JFFaW0Dz5P35mjjJBA9Y-WDopw9hclkxX669MKPVE5r1kDWlC5i9iOWwK8SUIkq4dhEDsGfmFqKSNtJs-s-276vpYkogpxfsA96Kom-j7IBQGjcnjaLDEB68ijS4yIPXWlXWaspoQeEGvmX1gmcTYgLtZutnZHBleN7g3jPcpVan-5rkeRlU71TpbYfrMddrPZbaPV6VH4_bxZ7YvkqwgOPntG7zcBBdpDqwEPxZuDvNdeqwuTeMgIsr3B3G1j2hlmGGfgHggm6gp0L3FfrSTynbIO4BhiNOqIoH6T7G-LTwfQhKxk6V0N3ljBgyzxorXPg5mX7AmYM3QlCGVwZ3LTNWZ8i6lhcZU6VvJ-nQ4yj9uFU0snSJykQKcNpJ4v_B-rps3vTQ9qiHs0zwnXzdoReH93ZoYlHJuJnMTpF98q6Ws8H-66e8Sw0ozE9kfMykQ-LR4OLa-EescGmlQfzVNj0Fk9M6p34ORq_a-q9DkW5rNkrdj-XVT72GLW8YOwzMLGkI1BgA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2SR5TxyIlwRC4YIr110iKIpvsmqZjOotIAAu_ObqjfXlWQHnYQIACKJDvBqoQyqrbolwNs07yzYqAgaBOV48Plx1MzSJTL9VTmXYXVIKC_-ygkr3cdQ9x-0,&b64e=1&sign=2adc0bb1c4b4827f69000d9bf45f4bd1&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: 225
                    },
                    rating: {
                        value: 5,
                        count: 639,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'ИП Тетера Василий Васильевич',
                            ogrn: '317774600015592',
                            address: '127273, Москва г, Декабристов ул, дом № 20, корпус 2, квартира 20',
                            postalAddress: 'Багратионовский проезд, д.7, здание БЦ \'Рубин\'',
                            type: 'IP',
                            contactUrl: 'cultgoods.com'
                        }
                    ],
                    id: 115587,
                    name: 'www.CULTGOODS.com',
                    domain: 'cultgoods.com',
                    registered: '2012-08-20',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/115587/reviews?pp=1002&clid=2210364&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                onStock: true,
                phone: {
                    number: '+7 495 790 93 53',
                    sanitized: '+74957909353',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mf5fIUVrEGur84qC-PSK5TYHFQfwNL92WuhMrWHdRZCiuhlG3xsRNd4l4MYX6ONy1I9hDKtUnBmplsC9DXB_0Zes-RWXqu-9f6v1Q81c-cE3rOFeDHcPLIYoh1P5rF7pY0sKiWmSBJarEi05eePw7Fy6SB_mCTvKFaS27ljJWY08PUA1p65CVzpL7fH4jqXmg2URb_k8TwImZDefxpuLsf5SRCXR2_5M7MP0Uyr6QQH9Vaty2f4JEwfgXYtVYGRQAas_cwJVcd2MU_HTujIqUVgU7ntBX0ln59FVYqQDGSBBYk8edhs51Yc6Kr1Rm_MXNepA1PVU5b-tVyZxfTOKVy_3H_M366Q934LE3VKk4Ap8sAAHh64dru9WSNi-BJEYgCls61D-yrZU3gQL4zkewMPNPKFp5MytRrrPrAModHJ_ZSfQdFu4zBrkeWMPYet5qm-ph-oP_E7rh2dgB3KQzElFVGLcUV9PzUsC-VkP206TE2bObtZg6o6z2dUYum_d4o4Vea1d59MZnZplW98W4j4Jd6WYq9ygSr1UnlgpQsrHEH5wXS1gdSEaERoxoaLxk5Z0zpAyAnjCr5O2QbvZNp92RZLCLNiP1A2U5sqSrt5-jCrx_TnnxrIujxjyM_ZRGFDd8NWEzeG60GTXO7OgxVH_w1QL1TNtrTumaOOblzT0SVIzG2nv4ECdad4B61jl_GYWEZXU6Z5Ux4oT2z5FM-bXjFDoNYcd3Iv3h0OoRi1zH683ei2ks4dBKXtbekNXNwhW6ohokcNuGhZdQ3MowLGuQZIsh94IqBC6nZo7UX7BZFcSK799g4xZZlNmrw91O8cfxhh0C0tAnXX5_eCLXq5xzwP6OfMwgg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_1zBEF_FUaqBDvKYWgG3LXZB_vwCwNG6Kd2d5LAifZ0rwBtkL5iI5WUfKBLeUhVzhEpdm1CcMPgay8YJkLOV69R0JwaxyEyzjH-7I-y7v7kIr32hAsFittfuAmEdv_Op41XHF7hJC4OmV3TExVAbhUvDLpppTlX-nkWGuTgD2lmQ,,&b64e=1&sign=e5b7a84f61efd63804e26ba88e854335&keyno=1'
                },
                delivery: {
                    price: {
                        value: '399'
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
                        nameRuGenitive: 'Москвы',
                        nameRuAccusative: 'Москву',
                        type: 'CITY',
                        childCount: 14,
                        country: 225
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        nameRuGenitive: 'Москвы',
                        nameRuAccusative: 'Москву',
                        type: 'CITY',
                        childCount: 14,
                        country: 225
                    },
                    brief: 'в Москву — 399 руб., возможен самовывоз',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            conditions: {
                                price: {
                                    value: '399'
                                },
                                daysFrom: 0,
                                daysTo: 12
                            },
                            default: true,
                            brief: 'на&nbsp;заказ'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                vendor: {
                    id: 153043,
                    name: 'Apple',
                    site: 'http://www.apple.com/ru',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/nyiEPi-lA4kejgu9X_uhrQ?hid=91491&model_id=13584123&pp=1002&clid=2210364&distr_type=4&cpc=gq6kGiTf4rUButL288kKD4X0dvLxUJIwxcpQPHzlEx2PYXpNEijL-075UoICjhvYU8oyWNeNfAADrbZUt4hnjPilL2UhiF-pVRS3WdfJzsTmPM74g42s2rX-AARqIiZG&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 96,
                    height: 200,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_NGvrwrZYAQkugW1DvZJolg/orig'
                },
                photos: [
                    {
                        width: 96,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_NGvrwrZYAQkugW1DvZJolg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 96,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_NGvrwrZYAQkugW1DvZJolg/200x200'
                    }
                ]
            }
        ],
        categories: [
            {
                id: 91491,
                name: 'Мобильные телефоны',
                childCount: 0,
                findCount: 1
            }
        ]
    }
};

module.exports = new ApiMock(host, pathname, query, result);
