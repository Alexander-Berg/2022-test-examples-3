/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v2\.1\.0\/models\/1730875803\/offers/;

const query = {};

const result = {
    comment: 'models/1730875803',
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
                total: 2
            },
            processingOptions: {
                adult: false
            },
            id: '1518535071107/f684846bc671cb56a107ac04008c54a1',
            time: '2018-02-13T18:17:51.422+03:00',
            marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4'
        },
        offers: [
            {
                id: 'yDpJekrrgZGMMVoVIEAtd-i7j73Wn8W6MmbTNmpGIiUBu858JJsDxGK4sqba1k1XdF2yDQ_PCuXnZJlZ9mvSCJDtRqzzSCvnJhwGC3Oj9nXvOradZpSb6rXQZwGEGC2luYnmSxavUIAGNjJtinsq49Vi0riOeVcYx5Xqe_Z0pVGQy0D7jlwGtUGR9sOA-_Kyk390hd3mAVyQdTesAP7ty1A1xJVfCvUFx4cT7zW0f1SuMuEAxTyWfJqSDTAK6QDPO6h-Tr9YLMtJgu5mIIAreCmawPKAJgQo53CdMrgGlwI',
                wareMd5: 'OWppTF4-1y3DrN0oKi5TTg',
                name: 'Monopoly Настольная игра Банк без границ',
                description: 'Цель настольной игры Monopoly \"Банк без границ\" - будьте самым богатым игроком, когда любой другой игрок обанкротится!Игра рассчитана на 2-4 игроков в возрасте от восьми лет. Эта версия \"Монополии\" предназначена для быстрой игры и содержит некоторые совсем новые правила! Больше не нужно собирать полный комплект собственного одного цвета! На каждый участок собственности, который вы покупаете, сразу же можно ставить дом! В игре нет денег!',
                price: {
                    value: '2329'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mehRahswkml8QwZMTPvTF6be1ihcOCeOrWkh7ZsC3rVJF-Kz6F5HE3-_lRtbJG0sb68gDjBA2tcHSZTTJOy1xWZ8zcocEVqQuQIPajKN1SMI9KuDmU2vgFJaSlV9L5M9fPFbTK8UpsxYnxx6rAEqY0AqCGXqWSx8nTdGANHxn2QyXezRbziBsRVbahkgj_yYqBdua96nxVTqJNy4TTEA5lDA91SCT8naWU3bIujKAsO8y27ddEvfvH3qti80SCxzRSiFPQmFes8uCLE8fhobR2CDYUH7DwZGvcAfWS_cHMOK0cS3prCKj7GLzkbBaDQLgczM4Sz1T_kSqHifl3a4HQPBA_hQk_u84_LVEV4sulhH6sObjWOLdRSP_9UvyBxWicBgm8QLMz17dAnjd4YLzWP0yTRBM4ALLz_tjJbwhHFBJTDQTSX3aKUQ3cPw5UJvf4rTFW-zFpj5kRVzSAkSeqW3uBCEpM-VG4XkmRDZ8MFNODXXqlpM3EaPGoaMnG8PEwbrII4hvKszslMwOh9uJ3T5yLdD__HlrnY9QdKPDJgRaJ3s_ezbcJP2Z8S1O8Sh2TNwtShgBmLEp53TLEz1oPlWnXtsRpkVFuIuRDYi7mccY32oMkKJqzsjWcf8FDxrzpDZE0T3NzMQHArmwIeS-poKxeMG2Ao2-Cp6Zw8CCxDg7r4acnrm-VMjy7RFrvzV_CRLk3m5DZvXD9W3v_N8aUJJaNk2QS_ivsxissc4klF4cxbsWaSmCpLsNb8DN9zCEOkj0SEKtySInqw8J7QaIRBA0Iu3JEzcefRwGkUV1sZ3UA5FxDi9MvVz-TJMDLCu2ilJ6OcXE0AyxERuUmn7TQp9urqWTFj0x5NcsxAfMMNcpIVTRHNrQmLARwUWscPa0A,,?data=QVyKqSPyGQwNvdoowNEPjUO6Lp2FicSHwuLrkBRU9MB2L71swKP9DwSZnoHPlJhRZwJIdUCMFHTTfCPTkPqsECFfdDb3lyMFXGRyQ0O3zJ7-NfgZikMuemIZ132OnEkfEWJPFLJpUBgCLpY4IzCj3APX0o7kOf-hfs9YnTzaSiqr4xnu3FEGmXGZhvRI2IzmReRumgw_23uoRy6qTup4-6yiff7f7J1NppPbA_NUAq_m9XkQO_Qn8b1BUZqLjwaqwcp7hIStvc4R9vziOlLJGyFRu_N3WXTTzlEZ9bNSUoS1oQJ73PxYAefkqU8NU-UWWfx8APsKNk7ILF6O7jNtBw,,&b64e=1&sign=1e93fc94587be11415ff38d5c761209e&keyno=1',
                directUrl: 'https://www.ozon.ru/context/detail/id/136609533/?utm_content=136609533&utm_source=cpc_yandex_market&utm_medium=cpc&utm_campaign=msk_div_hobby&utm_term=136609533',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iQKg9ijkV3IR4dMvzpGgKMyTqIIo6gwsm4lScIOjNvvLTZ2pEae8pYKFWZcsbSTl1qRTPfjxvQpOOXxYE78Fm0rbl3ACaPsnx_7amSS2_w8iKAIeO-fzyPHJLB8XA8WWNZxRuYNEvQTotQPFwLK8y9xSoMyTbQvC4gh2hjf71h_7Kye2yVi_My-E0PjtGEtOTst488CdogGSqUN5gtrJtJgOtNJ1yeguPXkxoqj53Yb8grh12GLO4OfhkCwN5_fAmz5Mc7rWd3iBGwHWzHsBER6Jlv9VGW9CaJbSgDBelCA5NBurfQ9W4DPaCIi_l4h1_PcHoAelAX_cmnZr5CkgL46QLU9aucc4RGvGkaJ8sgP-JT7O72dp6LXPawwxgz5aIQ-VftlLjuhwfS4gr06xoizrZm1_RLb0kxkmXuu9hnHQsIgcPJ9z7DsGKSTNifMzVR94KY0sczSQU2vMOgjnDenzbVNstf83ckCx5w55iCuRW4hCLGvvf3E41gRolX_h_8fBkDyqQ1rFA5JixEAEfk52JVXVXeaNVOGqPTVs8AxW9n0_Hq4kmx1OgbeEbZQQVVmr2x0nsWKzc_HBD_h9CUUtA1B6T-AD174W5la-hor65l82gm3Uklh_8ZANjauKIevvEcP_m6h4TuWFfmnJRtJKU5Ez2q4phB5J995JVEjMOAzS5yhR67xNu2tCViaqvbmfQ8VS_PoXikKYDbrR-Nra-ABZ0BwlBeXgz4QY1xedxr8X-y5ZX1g5GVz1bEeVTAlEP6FYj3rd8VeZy90YmiwwVuKhGU-l4x3c2hY6aL_1eOSwgUjDeskcwUP8M-f1PS0hzpsBY1RnHIWwZ_eXK10YNDhPK912qvgUQOzr_MP7uANnGvEIWAs,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cgu-yFWmVG6C0C3Dzlt1h4EUIlYLlPC9XmdQ6ZeuPlUZLTbk6341Z1HwWzZ-CFUWnmgE2T4WLEEnGxxJ52Wiw6eUth7Rkp_oOYmDNJPbhEZ-0eRatm5HZA,&b64e=1&sign=4299d0b04f6a9cc087eeb3bb310518cf&keyno=1',
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
                        value: 2,
                        count: 34087,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 5380,
                                percent: 16
                            },
                            {
                                value: 2,
                                count: 1689,
                                percent: 5
                            },
                            {
                                value: 3,
                                count: 1780,
                                percent: 5
                            },
                            {
                                value: 4,
                                count: 5332,
                                percent: 16
                            },
                            {
                                value: 5,
                                count: 19906,
                                percent: 58
                            }
                        ]
                    },
                    id: 155,
                    name: 'OZON.ru',
                    domain: 'www.ozon.ru',
                    registered: '2000-12-22',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/155/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                onStock: true,
                phone: {
                    number: '+7 (495) 730 67 67',
                    sanitized: '+74957306767',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mehRahswkml8QwZMTPvTF6be1ihcOCeOrWkh7ZsC3rVJF-Kz6F5HE3-_lRtbJG0sb68gDjBA2tcHSZTTJOy1xWZ8zcocEVqQuQIPajKN1SMI9KuDmU2vgFJaSlV9L5M9fPFbTK8UpsxYnxx6rAEqY0AqCGXqWSx8nTdGANHxn2QyXezRbziBsRVbahkgj_yYqBGrzxLuS9AHTUm5fuVFbPJIjNf8BzHOrVJHDXkXhbAhmgO9xti7MDw8SslWNpOces1argPMnYmkbepLHo_OdzNh9X2N8tJRhe9_BM9ayfW9LOIo7jfXXoR2yMMeAP_moT20JQG5k-AQ_DWQl4QkObUNnO2BppAX5hVgLjle6B8GhLQRZzPmI5ICER9kJbdrkAaGt4l6g6KX6nV6HB_v9SUOnlU-1jQiwD3zG3reo3KB3-8mRrYktbN2QewlQkvvtWYkrXOYj-pfqbHni5eBwtDUQniR7dmzt3Q465CeTXhTPEHAxqBSjd_B00VPyd4LIvYF67P2UrjMe-Tgp1vQOtqIhYz14nRdcX5sP563-d_eEU9DlaqbKfoYt9fSGrdU3hEjAUdBoKohqW3-vhQNW1q6E61Jhw07QQUeaHcrVTITa9wH8dSPnQyT41WciofMlzT8kvqWzcP8b7e732siC2ewiu37p-x7s6FYbVYrxHsuHjvXfCRi7L2JTQHEENsX2RZc_YlVKMsxRFTPg1jqd5UdPUZFSJA8RF4jc0vzpRomemigMe7vim8icS_7AbiRexqY5yHVc2fUrkSRajWVDvL-TYqW72VTjAKltQYoE-Uopn4nJzZzMf0QTZ1TAMgCps3bjsUk1BeNoroTiohcbtPVREm36y3I6ZqyF95EBv9MAtR10e9bFYTh9erYQI4v5w,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8o9LkYShGPM_ef2iVb1zb44uhOctTEF7WgcZEZwesr7Efy3_iK5tkKgI5SBSzVOT-LMDqw2H4GMRU9ib1gk5GD_joioFgtnuEN0l2M-i-zJviFvzm1-9_jJDYbOeyV8zgH1oC30B5M8rgV9tNTYLwe3vf8uK7Lhm65UKWV9R_lcQ,,&b64e=1&sign=40d39cabf9cbca4b6279788878ae0c4a&keyno=1'
                },
                photo: {
                    width: 1200,
                    height: 853,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/230492/market_QJHCBCVG2MIDr3EPqfs_kA/orig'
                },
                delivery: {
                    price: {
                        value: '299'
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
                    brief: 'в Москву — 299 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '99'
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
                                    value: '299'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: true,
                link: 'https://market.yandex.ru/offer/OWppTF4-1y3DrN0oKi5TTg?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=rqI1e_K2bxBwLcFp-kts8WyAevF24h6Yt38Om2RYYtG_K5TatLye5CQfgZBTmBqjkfI5Ou07xwt2MBS8vKeKjfN2QJJnzuVdMSa6fMA_Es_N_9ph8JL21Kb9IUEpZtUp&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 1200,
                        height: 853,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/230492/market_QJHCBCVG2MIDr3EPqfs_kA/orig'
                    },
                    {
                        width: 1200,
                        height: 678,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/205766/market_My6RtBtKRcgPWO__eB1D6A/orig'
                    },
                    {
                        width: 354,
                        height: 390,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/204557/market_UMbKLSBLDhvKWzmv7q_YuQ/orig'
                    },
                    {
                        width: 800,
                        height: 789,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/174398/market_yCI5qxJYgZh4DF_09Zm9AA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 135,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/230492/market_QJHCBCVG2MIDr3EPqfs_kA/190x250'
                    },
                    {
                        width: 190,
                        height: 107,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/205766/market_My6RtBtKRcgPWO__eB1D6A/190x250'
                    },
                    {
                        width: 181,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/204557/market_UMbKLSBLDhvKWzmv7q_YuQ/200x200'
                    },
                    {
                        width: 190,
                        height: 187,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/174398/market_yCI5qxJYgZh4DF_09Zm9AA/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFYw3UKTkjyqz0_dF0BB_eYP59qIixf2rAvZwqD31FFitJftxfivbxCXaDGymPTLdt8BHZXLEDPUm_8d5UZtKnYyRx0UoAGOf3IxcmEYJOW27UIhX0s3oe9vSsS-UgK1Qdn734ZoFL_IC6Mlhqv4pggI1lKWx5bD-PX-hA4W7KZtzA1Olvc7vrEweqleq5M20NRJt6dEO99JraqEkOP--J-LCDAxGHH-887w7r_RjEw_0Ovd4E17IUiAuOToQgWXIJZh696iVGUJGCNvwbnuyOcqhlL6twNpBQ',
                wareMd5: 'jOi15j9gloBYb4vxP2C7YA',
                name: 'Игра настольная \"Монополия с банковскими карточками\", MONOPOLY Hasbro, в коробке, B6677',
                description: 'Монополия с банковскими карточками и электронным банковским устройством в обновленном дизайне. Теперь проводить все денежные операции стало быстро и удобно.• В комплекте: электронное банковское устройство. • Развивает стратегическое мышление. • Рекомендовано детям с 8 лет. • Яркая, красочная упаковка.',
                price: {
                    value: '4278'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdL0yBxy2UoTesEbXiLdWQCanDYMg66akAmcC4zO9nn_GDzglNVBElyYaBjOj3RldDdZ3DmolWGqDnIPLT4pF3wUwsdd6RrHPkjhMpwLtRzwM_tMlCsBthMQD4Dq1kQkIkP7CPFemSoVCDsJPaMSRmUO8ZNJ6FO3J4BN2rEgdXn7SSgBTkfCgpdaUW6GTpJ5fZEcj2JX_9udlKpqSJNGnHPF0VoEQMJFpZsFtGgPiLoGt_3JNhrY1Cu53hNs-6_p6-2bnsMbn8z_nUBbZHH3pwhJ1R4z4qN8lewYnBov57wPc9QkyWIZ3tIrX3PRzKJ0QX8Y2OVbJHYvmzqtZ9SBbXuH0Q8dyAhy5L2bBUEdQlJKBfZ4FBr3juG61xQqDaCjXnfTNumobZ-87E9yfzd7d7XwprLAhfdRvJrpveO3uYG4t3P8NDIn5eUNWwjaDpmqTQZbIOGl7talEBG7noZmSqExp8WaCqJhA6lq0FgywOHJasL3kusV1gaEix1ARixIWPgy5irITijPBB2B-H4BxKeNmhxpg0_PDIHbXROT5C3cNs8_aZtC_-Fs2d4VvQFnANpqGOE-ZWy2GQ_iiEk0NkuEa89-xVjmc_znnyTTI_7n-VyQD7e_vKo8A6gFquFKta1NgGAv6plvT4IGbc0fcU_Nu2ZKDPjv7q0f4Aa2hXyd1jj2ibgRaiXQH3G0M5uY9xUJ0tSgjIVfcJD8sArF3AKxp_dRuQV1FUQzLLNs2IGKqOr0n0yrQmYHgCeTFXr0z49ATe7r1mdI8isf9NY5zELm2y6IjJpL9W5TvGcu4iuHfy9ldg6mHjpxqLwO8qm2X9fcp_ryXTnXPZlsKc9s70Bt3SxFlubmqKCm5EH4MKJ_R5rlioct3aU,?data=QVyKqSPyGQwNvdoowNEPjRs_L7fQIxquDdBJAfYe32AFZ24TeMwsRC7Nt_3FHTQuIo1WIw-ct48baEhbxICaISt1QPYb6Oog2kcfrbkicMiVWkMKFCbouN07O6uV_n6krCVGPS7K2OmuldJCD0xNPe6sGZIOdEVktustg-rq23WIxcTEY2SwAR5LzplQ_4aqnnz9jtHriCdBaGkxCMUZ7WulViPv3Yr00aWHEWkB2yDvGzA_kRxvpDuUEI94kQBfNte8euFadFfzxfVdPy6Fw3FlXGdzUv5CnNLJOq0wVSMfNARaH88Up9qRRF0p4sqS6vnROJd2jFg,&b64e=1&sign=1e74918b3d83762da89cde6b23591293&keyno=1',
                directUrl: 'https://kit-hobby.ru/catalog/tvorchestvo_khobbi/igry_i_razvivayushchie_tovary1/igry_nastolnye_i_napolnye1/163541/',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iUN78dzh5jkXBnr7BnuRr_wsE2swz24dH_CoWx9OIqARv6vEyDt7DdtW4nIKtG8TR3FwQy0gQkpfrYRq99eLWY5VCFOMd5SYJNTu0f0q4z15911q28bZT-Yg7MPyeso7vtdWFjGtsV_2oVhIb948vre2TrtRFHWGHUSTv5EOM7tWKj6RJPI7mxKkFM4w93fKK2BCFnxqzgb7IhlxRBn2fk00mAwajM8WGOLNzjfUCAmmkjFNRn7uSmACo7DbUh0IBbzCFE4_dIKXrV8yt74G8OyYG582lk3J7fj_bmfjC_IqMRl0jKziGTSi0oBRDJkp5_prHcCUC8L5N1F4H02gb0e59V86OGWOTRj3Ap0-YfzQDQKA87ra0m6y2DdIVnGtw_rr5TaXYBwZR0LBKXkHpC_pqRUBjz8m-DJD7HgnW5_x2pQyLv3t47IajpFLdCqvtnlTFMRrvIMvyVm1tjJqx65Bp5uebBywmTmF5H9pJViVEIbMIty_0D0V7gqlyxiNAxhFwbxgmrpU3ZonrG_r_2nbKrH-YBDbNinpB76HDAoUaU0EQtQo1GV3DslPEvyT4Yv0cTwtr7pao6zUVyUsUPlwCVM5__TsEvb3fRkcbhtJ6Pbz7mpJMVJ3icdGs3i_mSt7RJQ9eNr48-wrL4WxEXMsg_vYqKRluxAY4ueaK0yF7ZOc9E1T2jjRMCuStsIT-E4JRWEW7WWMkmqBMypljFefAWqhWozjibR1Tm-_omMdM-1yXXnDDxSSyBdygrewuAah8R041eruk04DEr8aDdYfVjVTxWI9ONp1_SUOZQflRKQROlk5AJse5Ba5CTSWPHriJylZa4igyvOjErOaVfFYQQxNTPpqJdDfq3X49-IVG4g39wd5rU8,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cST3GCKkP1V6wibVlTucBY8hQfYFu-VZYlHrtpaRpLvhbNuhE1rL93j5-mqkO-X64OdVmN9X0exRRZxpGjp7SXysbCB5L3IqQD3-PcJtlYOFcK7cr7OqvU,&b64e=1&sign=7fa1e79da0ba18e30dd45360a02d759b&keyno=1',
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
                        count: 87,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 2,
                                percent: 2
                            },
                            {
                                value: 2,
                                count: 1,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 4,
                                percent: 5
                            },
                            {
                                value: 4,
                                count: 4,
                                percent: 5
                            },
                            {
                                value: 5,
                                count: 76,
                                percent: 87
                            }
                        ]
                    },
                    id: 394515,
                    name: 'Kit-Hobby.ru - Офис. Школа. Творчество',
                    domain: 'kit-hobby.ru',
                    registered: '2016-12-26',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Киевского шоссе, дом 22, 22 км, 4, Бизнес-Парк \"Румянцево\", корпус Е, этаж 9, офис 901Е-И, 108811',
                    opinionUrl: 'https://market.yandex.ru/shop/394515/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                photo: {
                    width: 160,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/209514/market_lHCX-HMrpLdZu35tGnsPVw/orig'
                },
                delivery: {
                    price: {
                        value: '249'
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
                    brief: 'в Москву — 249 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 100,
                                name: 'Другая'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 2,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: '2&nbsp;дня • 1 пункт',
                            outletCount: 1
                        },
                        {
                            service: {
                                id: 100,
                                name: 'Другая'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 3,
                                daysTo: 3,
                                orderBefore: 24
                            },
                            brief: '3&nbsp;дня • 2 пункта',
                            outletCount: 2
                        },
                        {
                            service: {
                                id: 100,
                                name: 'Другая'
                            },
                            conditions: {
                                price: {
                                    value: '50'
                                },
                                daysFrom: 3,
                                daysTo: 3,
                                orderBefore: 24
                            },
                            brief: '3&nbsp;дня • 1 пункт',
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
                                    value: '249'
                                },
                                daysFrom: 7,
                                daysTo: 7
                            },
                            brief: '7&nbsp;дней'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/jOi15j9gloBYb4vxP2C7YA?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=YjCB5Xf_x9ELOR1hvvxV_fF6yQU9sJu2Em9YaJtMhKn3ElUevPfZY9cvbLUNJlPdlQVXqe6ASRi5CTYjRfHDAzwJ_ZdCitWVPwuRROsDVRVGLsYjm06PEbwx-qmtP81n9PXoedT2M-s%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgalABJJEhXEzzEqXYK_xG6lQj1pRJmCckLDFRZbT3kc4O2g1_rA9wW15LiOokwZyVSPvhznlrXhaKg3MrAIfx60BQ1jyq5PdQ8PdqAobDrD52d5AZrtFx9-bIQEhIr25NE4XkuDSK7nfmhQcvpIgi9oRzjcQSS4LaQ-EcACtB1hP66zAS0-KsCelUoiXgR3jnGRqZoMpfS9_kdpHe-t7zSfc628d0emWmSloaoQ3sv--RMQe6d9BtEsUh661U_aws26BlrnjNgAGgdtq4d0-LrPpRB97QuV_Gms4q4NETqKCRc6wKbGTo78_T99qWv966aKhnmXniFdwaYLlFY9I4V0u5r-EoIedTQ2gO1rDDIn-77NphH3xgkMtt9i7CrZRHImT4-NV54nMiWKBHfH3q0Au5bd9xdJ8wxvADjoO0ufV0qhCHCN8wkkOCm2QamTaYUiQL17MsmYt29dBcfE717jVgqKzXOpuTIL4rZGsArqstbNmneYq2co7bfcmlZkh67sFxtovt0q-2YYWNtb3HpN4jcLGyAYZie0Jl-PLnGzLC3v8i2sQmk-ddT7RV9s3ZGEKjW302PqYwa1fp3IJVMdOouHpkFYqk7uNPLmSPPzbpn_S-G3BLVXme1-FxhON2VOgBqenx1vxMeZfrs7qp3eiPYQ0UZHlU7gn8zckT6hIWWVKd85kuLB0NV_T9A1kMybpfT0lG8D2Hm7qD5UcvkXAq2R6bMDZ2FAgIGYoCEC8soDnegv8palJAjwcRu2ZSv_S3jcXkqW6-z_KPUExS4vU5X75wD3-g_OzGQSRshHE44bCShGs_SiGpa7mdq8C7Ey4IcCElZ2-SnNlejMad7BeBDtbFp4TLmJ-_F6hWOw2uonYhokntluzAOQiqEYZu37pxrpGlMzibShlI480i-4,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-5N3-ZUI4cYP8KCrsTyhz3K9NZ9xgAFFXY8siFLev13sKnJF8-9kYUEDmzKror3lkRX2KVzgIKjhepMrgfedfUVbgxyrlKhRG4mQubmjHgtjg7ePC_eO5CujUottDrMdbbpRIZLXE92RfcnQfucekgGu7raJ0PcX21WR7xlViSD7PwqRINVoR_h15vWYXbjK84Q_2NbkIhUqff1cCuYABg3I_d7CMhKLZAo04oWtQUVrCheJOhEO9s5gtIHxu5Q-6Lpba3c2RFzVeIuogdVnJJG6rrq3yeg9ZqaSqb8HZmFBPkn_K7WH1P08_h-OsBW-xNIovmHajvjVw,,&b64e=1&sign=f1453ddd57a621ebdd792554bed6df21&keyno=1',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: true
                },
                photos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/209514/market_lHCX-HMrpLdZu35tGnsPVw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/209514/market_lHCX-HMrpLdZu35tGnsPVw/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEGqvpvMGyhJsOY3lroaO4xOzk2g1PKSc5xit7Oqre4kg',
                wareMd5: 'j1Io0n6j-rpRKvfdSEmJQw',
                name: 'Настольная игра Hasbro Monopoly B6677 Монополия с банковскими картами (обновленная)',
                description: 'Представляем вашему вниманию легендарную настольную игру - Монополия от Hasbro обновленной версии! Производитель окончательно отказался от бумажных денег: теперь расплачиваться за покупки нужно банковскими картами! В целом же суть игры осталась прежней - скупайте дома и недвижимость, стройте дома и отели, зарабатывайте деньги и выигрывайте! Монополия - замечательная экономическая стратегия, которая способствует развитию логики и аналитических способностей, а также просто станет отличным поводом собраться в',
                price: {
                    value: '2799'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mWVNwM32r97vMJXMBBbJyFssX4IER5FzYJnQFUEagj6Apo78wTGXklDmIvKqtd-UrJpOMnJrpPp8BiV39WQ_p2UE7Pb07GlfAtxKWvKUU1vqUeTvROzAEWuiLTTLHDhI_h2S3kjT_UkSNKRxEeFQEV1Wpj4bui-T2jC2ht_e9eWyv9PS0GldsE3B4vzU8U2CkJlRXEBWXuyPyXiwNWfp-jucuFKbXDHdqveFG6oyjiLHBhuFkKjSkc2TOwEdIkxgN-3997bmyXYTlUsMY40TopMphRPn1i5GZCJw8qFpeP6q4-4-bihloDp9n9aAxZ0nRFFDNv6aJA27qbH6I3P0FlDwuPgeKzBktHafmjWu4-trDMcs26IdEMqufTJyF5x1terOiCS596AJ4fu7I2ip09wRK97tmsEcq3gEPXiiQdYHF032GDeB7O33eg8JYrk8M5NwIAi6X0K5Rv9IY8XdqTecwak0reO94kysWpWkSLJCslZeZFGkFis1Nxzo-nJNPelq7nXb5SPRJL6V3POHHI_c99vfdnbctlIJTbIKIyFPrU6URCr5lFQSQI52gCtbrkmFI6iJ51q5wbzgtaYQN2MD0V2cVNqHT-ZXBS2frKkh4tKa--Q1eq5fxjTIYozfNzI4bUYhsEdCpoYy1gvi0WAfjd3F4R6FOkKbnn112xSPSs_nN4vjnMDJxupBe9kZ9U_E9iFdjXKxX8tzswWPWvPIgEZeSIsPz7QqBJtiwJa8LvHlAPbUyXNBA2u7j368h4uvALK39Edx0pgtIPFUWOYEGMDPosxz-Hxa5GW5MbFqL6LDSGP6cuY6gaAqUefotoVcFAvdKxisRDzxUh3ufQK3fJ0KRo1WgYvPdjU7izYZ1fNZXdeI0u0,?data=QVyKqSPyGQwNvdoowNEPjRcd1X5IkopGx8TOY7lHsB2IzdZCVgSOE4bzcsD5Zw5aRVUE3FYAfxuvVRDDe5Dkm263fkWIh555MhjCV2krHHOdzkugNJDnXlpKKbmZsbE2VXQdtOFa_Xr445lYQmqJcO-dQaCuUIZnpg4fT0QYdP5PPdZQMLD2TpU6jadqKUFhYt1sI_x41vrT8JAdJKudmTtfvRFNCAakJelLNDRUEtl-fuuPs3Ux-og5t0Z5hyOjdfOgMQvFf8CrSgXgsLvBmVM9bn2OB7nsAdpMG9WT36Pa_-FLbQfwg08aMGlLH5X1IlgWjpYzt6RqIh8ELcTWQLMrPUyZZMGwmx_c26sqvV0ipizdXs9eARvhEPQ8evDg2oSkGx-zN8fJZlJzYTIk3HXEFr3LR9CspnZ853VIc28wbXIi6UaQ3qH2xlKfVXy5mV-lOwAalhGTAlb26p9WG8FWspQAdwcuQSt3s7HhtjhMb8ijubnzSKjjePiMPhJNTxaV4NOCvx2gVvCSeOPgDRUMpPAPWws3ZWuKFPAcFRl_IxPm4WrlRca_7_CFZFMVkhP2XGQDzqmy3DN4Gq2kaQBAvlgcdKwo&b64e=1&sign=7c1fb7446923931c4586bb8acb074f7c&keyno=1',
                directUrl: 'https://www.toy.ru/catalog/monopoliya/other_games_b6677_monopoliya_s_bankovskimi_kartami_obnovlennaya/?from_yandex_market=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRufkc5G38e6HULXVkfcuMDB9XRjw60gA9YC7jLq3YI3B1Po2OlkLc98N6w8DOTfd0SD4sN6cMUNV610_IOAvBbrWvVHcPp_ZvoLgGRbHQ79G0SEpXEfa06ocJkxBZdEFKP-Y5EtszAf403KdGXKbUXIOLx2_8297s3fFX7tfbuxtizGRHVF7doJMRvwrZ2oGihUajbak3D-wqzDejKbS59B6kuRtJtieyoTv28FvColqxiFPgkWQLvQAPvAZHzvZehdPa1GPYq5AR6Uq31tYtmegRRUmC6cRt1e7HcVlOU3cN2mqhH6tDsgGoxugGIVFw3xYC3S_8O36k-eGjWU5ww1hJGtK4-ncvLYZLh69R3kzxIChlbB-ji3yBKjRKqTyVWsGih3KG2kDfw3labu9s1yLiXcvZDdkSVaZU1_hIDOD-ltVRbLab8_5z_Uly05IayQmjrGkpvFH9Ui86wVAcZokXXSdp01c9Ka9Hf4lpDzEXXwCeFOKGkv0nXJJ2YU9f_jW1EbYP78tunIBZ9KmmoRNULSKUVZU_1ImICn4qL7WqCMXWXT2Xgv5d8ymKa-enmiu_4fgN2p2xfuEoSggAtbxuNfA3s7VDou788VGHsLyUHqFc8EuRfBdrv5ZAHyVUsp8RCfET-CylsX_HjXbgzHwN50E8JxGCbHvG79kux8beYJqOBK6lI-5EQBuZzxsW6_7Tel0arMsNR6Cx1svWwQNJzMfdo6e4WUvqYiHAlZaiSADauw2_92Vq5z4xCc5TC2KKCrEcWetUEzKup3fh2OudjEHWInaLEjyB1Wh4QS0GkyUUWkI6hTudYnYEWFlle1ucaxzfhLGcQPKbDqbZhAkcfqR8Rkx8-LCkr3JKpREXjzvWOMiIA98e41zIlQ5Bo,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2btM1dgQVldwGzawYIo0XF_E1DGH_r98NFfdzquLNUMuowMEYQBV8zArdoUMmfn48lU8kiEWis6M5FUF8hFuP2qEFbZ2QXKMg1VuGoOwCc4NAagU2rBDOgg,&b64e=1&sign=b236925e530073bee1d88b195d81a947&keyno=1',
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
                        count: 5494,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 499,
                                percent: 9
                            },
                            {
                                value: 2,
                                count: 111,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 81,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 120,
                                percent: 2
                            },
                            {
                                value: 5,
                                count: 4683,
                                percent: 85
                            }
                        ]
                    },
                    id: 56564,
                    name: 'TOY.RU',
                    domain: 'toy.ru',
                    registered: '2011-02-16',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Профсоюзная, дом 118, ТЦ \"Тропа\", 117437',
                    opinionUrl: 'https://market.yandex.ru/shop/56564/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                onStock: true,
                phone: {
                    number: '+7 (495) 215-22-44',
                    sanitized: '+74952152244',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mWVNwM32r97vMJXMBBbJyFssX4IER5FzYJnQFUEagj6Apo78wTGXklDmIvKqtd-UrJpOMnJrpPp8BiV39WQ_p2UE7Pb07GlfAtxKWvKUU1vqUeTvROzAEWuiLTTLHDhI_h2S3kjT_UkSNKRxEeFQEV1Wpj4bui-T2jC2ht_e9eWyv9PS0GldsE3B4vzU8U2CkKXI9n8XhsJ14k-17CS-LEG6B5DI1IbubmOjHdtCMQRyE2nmuE38gow5OIRn1BtePRrvkRvukiL_TapoJuZqWAcviw0ZUC-QTL2EN5FM6Ziq8V3pBcPWO8KAeITGdj7TH8bjiQOv8Am3DGxQTFST9PrB6kA8_AcmKzZkyGqzBh0F9Y4lSux3ChpxxmeALDnp0pEOBSRm0-RIzcf5vP2cKwj-Li74vlv32p-hLJBT0WQJnkwPCyCrBaXQIxaR3BTtnkg39h3gA0K3FDS5cdrV1PuS_Dg-qqejeCSISTlAUMd42KJ01ZwYf3tM3WiWmYH5_ATNhJNc8FFDIevP0UGOOqPztpJvqwrf-PFRR95sa3_C9KavIfW_uENEqkhqxFmGn4uaY_OUxyDajJHEEaF7OPMOZ3tm57sWjfqUzoB8PtgzbEPELDQrChEi972G8eEuBLAFvAtlW7ymFWXSfXmVGAm7IACZ_BHjkcavWSowsm5Rhma9fVt2o5m05aNlEaox2tfEHVJnw4YiovfksZfWlLn7FjXGvr9ox2pzuz4o-io-MM3hjUtH3JSilU6WDOf3UARvAZTBEL-qFKUZiup0JMf9_UJ581QF_-EV63r2NXwkA6SrdNtBtU-22Cp-hFtIkvCRGxFEBGmjBF8k0DCNUvk-3g8Cit-neRupNfQW-xcvZLRP7UNBsGs,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8OuHeDkJTcsCf_LUOLG-9WaYJgkP2nUT8sIzJW0sfhp9ZOvAapB7Kxy6kU_HhZskDF8ZM7l2yHkX2-RZo1xQ-XS3YNOZNf-LG4Hm_9qtI5cipxuxUPVKqArCx04Mx0p8v7L4sdSNLna76F8lmUZmW-bqbyMRIp7rs0_7aB3J2w_YYLmrHNx1Cf&b64e=1&sign=aabc78f0f62cef28c4879fef355cdc49&keyno=1'
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/174581/market_W8f3HiEC3gNz0zdMup3wDA/orig'
                },
                delivery: {
                    price: {
                        value: '99'
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
                    brief: 'в Москву — 99 руб., возможен самовывоз',
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
                            brief: 'Срок уточняйте при заказе • 15 пунктов магазина',
                            outletCount: 15
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
                            brief: 'до&nbsp;2 дней • 18 пунктов магазина',
                            outletCount: 18
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
                                    value: '99'
                                },
                                daysFrom: 2,
                                daysTo: 2
                            },
                            brief: '2&nbsp;дня'
                        },
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '299'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: true,
                link: 'https://market.yandex.ru/offer/j1Io0n6j-rpRKvfdSEmJQw?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=xj-ZmCZLmBAHTBOTk4tWIq2w8RekyeR-5SaiL5pTFPmqvDXZbzuZTXYCQL7Z8UQN3ndvCRNVgR0Ojs2TJTAU-PIDK5ndhKQlMvbEacpnvPz8dL5Mwxl6GhV8BPwMqD7D&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/174581/market_W8f3HiEC3gNz0zdMup3wDA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/174581/market_W8f3HiEC3gNz0zdMup3wDA/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZGvdSG4WBl3Zva7m0gZ8RDohljZnpNWsyLmg94V7m7XXoO6sX5_36d9MVse8-_wqT3EELF5LtCjtA_yM9q8WXuEIFfjaH8yd6JJ-yADjQkOi1ZSyzYLoQ2cN83WNQBgMwWcOrws9Q0XTFCwFlbbp-C38eCSY8LJVCvbF9HaeWQAoxiOxMoJNuLJQBkNCYx6fo1MeHpEcnqySeWvcpP4eT4RGS6hRprrfLnPIag-zxqXCo3MpE7IM3amruOvjk-R3Ba_i6pcH-5MuGSywEFuRcBSj7UiYzgKiRE',
                wareMd5: '6cl8Cj66gtEMxlPMNh2g1w',
                name: 'Настольная игра Monopoly с банковскими картами (обновленная)',
                description: 'Настольная игра Monopoly «Монополия с банковскими картами» - это обновленная версия полюбившейся стратегии. Яркий дизайн, широкий функционал никого не оставят равнодушными и подарят массу положительных эмоций. Особенности набора Суть игры состоит в том, чтобы участники собирали бонусы и преумножали свои накопления. Для этого необходимо кидать кубики, приобретать недвижимость и собирать ренту.',
                price: {
                    value: '2199',
                    discount: '37',
                    base: '3480'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcpog535vFrfNbF76q7XJZzd2fP6upKyUrAVRmvYTC_PlGkJgurJIurveyolx_hVp1uH3Wa23bH8X8j1Zya6cj0YOAui67zpKvmhaGargk9hf8jNNZHA5710W0PATa9a3IW9__Nr5bRzf6nYeGb4JVj1nSrQL2Qdvv8ELlpWM2p8_dWHzJAuwp_SqFbVHv86qG5RLr4sBIb5ASNTj9N6_1IZOKNIZkyNixJxCYzWFE1V2TyVuPESo0c1GcPx7-ktj4cebDjFE3QO5e07QQ284F-w7XKklfHfO2BsEJzYw-iD0ukp6CANU5pZnlHbuV2bZCB_cPdAXX-Y6yFZmv03cZDw6a2nUNDO7uH205X9Xjpep3765HRuXeSDawkatxF2lVyT_h8lBrabbNgzVDOG0uu5ZGSNhuZ_AF84qOm_Tlm-DWyUNT_z5UMHpvYBbBY3bck6_70KDRUiJ9vm2ISeo7oy5D-i_9fAOh6VL8tSyH_34CphZ1OUnMfuffj03iG2VxSnhwKD3n8LhdRopk8TO1rQEe6r1DSUA_pNZNDA-KGgaCgX65DBFLncJmZdQSfxlVanRMkqO0XUSeN9BBr_VCUAd5EhhljV8sNsmy9BZCTm5mQe4tZzss4hjwhi2G55NBi4Rz7NPgTix2Rcc2AnN7nBw3BL0y3lWgas1qby9ImAXJHM41YH8K2rQcBaeTjbmm5hjbMO2lb79741E2h8S0TXyYk3bSOhogPj-LkVMK6OR8e1kn6Xyozkli88a2HWEeZQt9GstFEVlk1hwyBvBJkjS-UBmSUGMxvHdxQy2pro1CGO2a4kB8mQwuUAE9UFlzC05uC2E2kFaFgrr7vtiDahQkxl4fQXWg5TJbdieVzf_7CWGnYyDgr6K80OrIjfCQ,,?data=QVyKqSPyGQwwaFPWqjjgNrSTXEpR9O8Xwc9ZJ5kIj4KSmIfoqRylMqn7_T9axhLUOxi8ITmzhby_xqeLrNY3X37Ozc_zT0EzHhcLTzCMdvRin_MJfR-wcUEzrPQu-mJKsIREvp3DmQCEbxpy5DBYRr6smvgF3OmqV8wwAFn0cnU2AHVREcCv_hJTZKGqFRqSpoe3HfWYj6KP0vUjmYjswu6BfW3Rky2RDZZnLkI2O6UF2o4RQ6Tcbax6B8upRAykvAZti9rpkAMEsxRxCVtaXBuw4TZYLRttF9WcdU6gMWm_rRG873QwamJGj-lJpjRucum-IJeKiqwfJGWHtH1byUgZNg8-v39M&b64e=1&sign=0a26a4d9158ad20da3e9e688529aeddd&keyno=1',
                directUrl: 'http://www.dochkisinochki.ru/icatalog/products/869410/?marketing=yandexmarket&mcpa=price&utm_campaign=msk&utm_content=3702242&utm_medium=price&utm_source=yandexmarket',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iVgHz7IOxWxE_Mzp0Qrbd4-DoDX2H6LPVusGUoHA4QxgdAHpC3HSxpiutT9hmczxfVAsmkEDzmK_zzVu1IVWyKZx2CyK7jLNw91F1CC_B7i2DWN4sVus5fBLvKQdOej7bQ7rm5V69Ei9uaIc3lWoTttnWd0bE3PrC8D6oLOLbg3po3QVTQyW5q33rd3pIKnwHpxVXvANcrKLWuTIic5XdxDLJhnNiWL3ltju8BRf2JLhLKDl5OV0J07lmBurMHfbyFgKxvW4BHHzlFqHFTnjMteEdoipUV-IYA2hUOm-9vX2JiXmRbjhJE6jM_9JnXd_-K20TN3HSJiv6TfyHoN7pMAPH7ANCxqkF-MdYmxCpY6GmS2K7voA_0x8KXUDSVj6KH4Pfm_nAKW6SK-K1ovehjKJ50X6qZQHrZ2rq5a7HZkrhgKptupoSEJFr6btmqF4JKhI3UJ4zVN0sjbbziqcCon227O6np9Af9Uh0nqUomVoKjOxt0rZDF_GtnilzfQmYGGcex-R24atHX4oYY2vkPtNDr-G4YkQDsGwR3ZRE1-00nV4-E2xAgNI_qv7MkB2cXmu7QtwKMSo8Nen-EPWiFEKoQtka12F2D9IgpshtflrT8oCx7PjZnAp6-AoiecDeIaMc3puS7Trc_0yATY8H9cCH-00pEJ1vvPMBw6QyIDADuOvRMXA4OeOqdlhktCr4JZzdgRdO6AtQimkYqgxrfKZKLAtb08gMS3C35iAPKLxyc5bKkKihfYhSOxGPr0bfcIzMRWHy6v5Rsr9Frm8V1KuOI9DHceG8Q-WC5wbwx37ZJWgWhnjCDDvhVF03SJ3KV-DuS61OBaDNsWh_-Sodda7mfJmYo6ZuFGCiQ0wtJbbr51LhhyRgtxWxdWeTJ2-Iw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bn2u2rvQHKoAenKwamDlyUb7RcYihuB-8rndcOckvL4RBnZxuEHe4MhtZ0HLLhvyyoZZNKnYr1M1GsE284Wh6g3u9_GjxhlbO56mgPsIBdmxB1aBlsvABo,&b64e=1&sign=14d5cd9f396d7660ee7a1ecf648e299d&keyno=1',
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
                        count: 9370,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 720,
                                percent: 8
                            },
                            {
                                value: 2,
                                count: 187,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 162,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 1196,
                                percent: 13
                            },
                            {
                                value: 5,
                                count: 7105,
                                percent: 76
                            }
                        ]
                    },
                    id: 179876,
                    name: 'Дочки и Сыночки',
                    domain: 'dochkisinochki.ru',
                    registered: '2013-09-17',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Ангелов Переулок, дом 1, корпус 1, ТЦ \"КУБ\" 2-3 этаж, 125368',
                    opinionUrl: 'https://market.yandex.ru/shop/179876/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                onStock: true,
                photo: {
                    width: 1000,
                    height: 1076,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/208616/market_YafVySBIwd6V7E_RcwvRaA/orig'
                },
                delivery: {
                    price: {
                        value: '150'
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
                    brief: 'в Москву — 150 руб., возможен самовывоз',
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
                            brief: 'до&nbsp;2 дней • 27 пунктов магазина',
                            outletCount: 27
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
                                    value: '150'
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 19
                            },
                            brief: 'завтра при заказе до 19:00'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: true,
                link: 'https://market.yandex.ru/offer/6cl8Cj66gtEMxlPMNh2g1w?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=-FhZudVSr3RF197ZoVpOL9G-2WclBaLcU_Knr7i5owV_OqQPzU7wpA6IcQw_O2MY1QF7fB1HD8GM60JLO0UTPg2CKGBDXMlwGY8w3uZCRKmMT0b1MNml_ZIPmSS_0Gx9&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 1000,
                        height: 1076,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/208616/market_YafVySBIwd6V7E_RcwvRaA/orig'
                    },
                    {
                        width: 1000,
                        height: 1076,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/249455/market_Ng_9-XLucvJBJr-KwyrBcg/orig'
                    },
                    {
                        width: 1000,
                        height: 1076,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/221366/market_KVVUrWUmJsrdiiFS2-1N6g/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 185,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/208616/market_YafVySBIwd6V7E_RcwvRaA/200x200'
                    },
                    {
                        width: 185,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/249455/market_Ng_9-XLucvJBJr-KwyrBcg/200x200'
                    },
                    {
                        width: 185,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/221366/market_KVVUrWUmJsrdiiFS2-1N6g/200x200'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZE32ctA1t3XK9flbhihsL5dtYtPziiGsKv9fAAUqRxqXg',
                wareMd5: 'VYKgtSJ-hH0vHG9ZzVKB0w',
                name: 'Настольная игра HASBRO B6677 Монополия с банковскими картами (обновленная)',
                description: '',
                price: {
                    value: '3010'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mWC93rp0fi4WGqaQy7l7aRZEdz1H6dxr79CUQ7GmtiiytiX1b4pHiSucOjr5qzpAIuDBQUL53wEoqx_7h6F4K9Uqbbu34HNCRMrKqZOO3r-kBrxhkITVtl0Hp9DUH1ECASkEk4rQn0GTKAYey2Zl-LQZegBQG0FT2_Rcam7fVrIfbZSf-P1ESPcvmURdxQ_GTSR8tQbeXVts0zrTqrNXvpGjwb6k9Aq4hvFKk-8tPDbR8oFupBHiZkdC1UXGeRgQm6H3HNtU-cIj1tkNwN6W5pKhYe83bWpR8qHd29xJKURP6nWxilCgSxiR926P44pViTlPINAFfPe46vZGFNq6FVMGh2g56H8CqXeDXdho66YTR8ZIV10NeJtrJDUZDLYnPBRSVZFDL-akLqmYnn9ypvfOLPtxsIzpIMSzUR48TOmKzBOTmJ_Dj2l3bWK-XnKQPJFR8n64FvoXMOqTj8n5jM0q92YzkFxlBid4Ef3-IG8S2ZMA-zRzNYcXfh3RrVfCHlMF53wmGtKA6lFekZQS5X3ZEQV7df4vxMDVHoz9sjkkozopta6J53ryzr9YIocG2um50Lgatbdu69wZWRc5-jJi-A9YPpm5jZuHzSkP-rW_TnrBq2liEv-d8Y6eeDbAa334bbSkpdkxZkfrp62S6PUbMmlXqU0QjWQ-ZrEjvWOtsuI_KKSm_eKKV6YeroSn8RjqF9IljjQOCxEgxh8xVtOrY33ZOtBGETF4s9qQ3PjulJ3y4rgtlEO_q9Y_vAjbwX5eohP62_tBoUjfSLZg1gsbJO1ewaVaI7PJNbFSsojIBsMtwWdjoBV11w3cesUdZE1Lp_HzUDtIBcCOr099X3Dr09wxgbs8Jd09RMLb_BMW?data=QVyKqSPyGQwNvdoowNEPjeHCAo-a2AEEMgvw6Gv7rodwqTifDSzaDcFSJWWZewOk9OWrt7uVlCZZ_4i1TW6Jhv7iu4X03FjGPjeookZBQ7CFuQ1Hc2j6nznh6s5M2j-SldiEl799qwHDi3YGJ68sQic_9OqvRhkqj9P04rJ4tKECywcXNQ9XWhBKXaq7kqhx-ZnQ44iFw3O_b1VK7xWvNqsQleoW4ovkaBX_64J6OUd8QxPuLCIWxpeYOeQ04CT1euFavM69X18iZOL41_-zTQFMS0Ya__9o7LQ-qnW_I4rXn1fMLYFukCjCyjlATIvZmJPH9c70n0SmQmhN5Y6WhjUtM7JBtlAAczZkYSidLYHDguKGFpuk4S5_iIJab6_J6A3VytlRfmgfXoN1cxkTw3MyU_dmJt4TJnXiGMJ7CNdp6MSTe-jCBWe16AD20x2yPS43XRfxyOSGMBE0nnsB97W7-iX4cOKSclfD-vN6EtfrcdxzblV5WFhjZSJMyKkwSnhIuB_fLJpjXuPbblIgiKS0jhPXwP5umc1ZalNBt_mznoYI3LUW4jw8w6U0jkCfigVk_CDAWw3Ta8wxS4mjNCH051qy1_3ATC12-qw0psjASDi6QpVHM-Lg8noGpomyCehSfZchqIZy22pbdepxv_aUHyv8uhzu3JDPPS7UI5pvGDlJltfE4FqK1Nm5lpnU1n1rOSRsK4hqfhLaoFvF_5us1XdS9klWUYlRUQeg7GQUBfdwqEw3i6Lz5dnbJEJ0QFNs4SjkSPU,&b64e=1&sign=55a5a17c2924192b7d75637f1922866c&keyno=1',
                directUrl: 'https://www.onlinetrade.ru/catalogue/nastolnye_igry-c494/hasbro/nastolnaya_igra_hasbro_b6677_monopoliya_s_bankovskimi_kartami_obnovlennaya-582816.html?utm_source=market.yandex.ru&utm_medium=cpc&city=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4icecJlq3aTyCjij3PQJIoWBdJOebxFgN_Pne6TzU4AO4oj8bHXpYdBjNEduHANzS0QRJjtYtAalwLtXHpiRshRnGRQILzgd7DLkueViV0xhtYSMs0oOey_SV7o88KqeC8WhB2EWn47nztJCcagty44Oyi5tm9Ewu-_zHqcgsUBiNxUZQ1kXb3XGh3tKzOJAwMfiyY71QUjN2cafrl85JzjJ7jb-LMBSS4EqaYCy4Y8DnBKEwJLqElb33y29DhPJRDM_yBEStj6D3Zzj-mpxUM2R12AmyRhY-prirqDF_wCcbBhSfsO1wGhO7CAzVQveNT1yn-2jOivPDimShw4Y1jGXDHISEEkzxk5qQRq4Wvgtuqd2XJAoCIXWRZfzRj1CJmLwyOA7TkkNDU_vR6gSR9QVjiNelyRAp366srXzMXaKHO0bWpxudUGiVOrOa_jeTYKM2MeD7CK9oTpQFWp8tqk8n5-h8OoXKcoykxGM5iBRJ_DWowmgnIpTm2OKIwDoh4oFZT9X2x1qKivVYX0xvpk8MVev5gw8cTNOOcf95dmEQAbiPYoVoZAoEPJPYVMNq8V6Pvvh7uE8X_HLPkjebWpRVNLG5O0i3KftEGFX3CPCW0uDpiDY-S-u0iA0CrGBWaLgGq8yipAf4Li61nC_JZQtA2qdopSWzjR4_ypIg1pCV9QUJwfMpvWx7cWXWtPLeVDzmeA4H5E-W5YETix87Z9DOpfy3fkwaCBmzdQkyb40hAYwubkYmqV91LR8Uxz5MO_HR41PFRgZj7kTwY17zNSEgk-d-O-1_3oWw-A4dwe9135Isp0en34u_y8ODmf7Lfg28GEJ2E8jqh-Mny3P4T_s6KxZChD0_Cdg0GQ-oM51V?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2UaWiSVZ7Yv2fDnW3KYsBTjPn7vdqsEDhDbCPCCT8pznaNEVdINzm8f5FEAxhlyQnJkGPEnW_Z1SLNoVLb_DE0GqGsrH0fGGzuqw3Z1AmDzVwaU181jnPDo,&b64e=1&sign=92b583fa5c2168f2db81eb0c8de141e8&keyno=1',
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
                        count: 68301,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 2986,
                                percent: 4
                            },
                            {
                                value: 2,
                                count: 1064,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 1213,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 6531,
                                percent: 10
                            },
                            {
                                value: 5,
                                count: 56502,
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
                    id: 1730875803
                },
                phone: {
                    number: '+7 495 225-95-22',
                    sanitized: '+74952259522',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mWC93rp0fi4WGqaQy7l7aRZEdz1H6dxr79CUQ7GmtiiytiX1b4pHiSucOjr5qzpAIuDBQUL53wEoqx_7h6F4K9Uqbbu34HNCRMrKqZOO3r-kBrxhkITVtl0Hp9DUH1ECASkEk4rQn0GTKAYey2Zl-LQZegBQG0FT2_Rcam7fVrIfbZSf-P1ESPcsu2hTXU83EJO5T7UlTb0YC_IDhi1ogaiYuUbXI6om70NNItEpxgIqob1Z4h6Qe_ZfNfw_cffa00Cb0DhBsLAmHBiEjW-6KeA4gBvsvYlTf1lqyC-l7fs9vE6KjgYT4ky0wsc5H_PG64vff5m9eoAWtZqmrw6CqK8BlsV-FbK409oeLw1WnosSQUawKmqOavRSP8iAsWLSUkjY7pmhoKWU1Hj3Gn-7hwlQuWC_ri03Xiiq-bTWryHzBUOp2tqj-uWQwQXaWl79ktqFGky0T3geT3NWewSC-9YW0obmY3owW3CHmmawlK88LGs1n5uQuKjRCWi25ldAB9zB1-_DwNHFM9aBhboZ_wIu0iPCHT5K9WEsRpGMb-vx5o02aBXGqLKwONw7Ye10pYdCZY49p5EsDBsm-gqUImJ3sIgRuX8uRF484E-4MVkRfEYHzQhEHubG3FnRP3oV-GBAKRK9LopJ98m-p_XbuXJtPIbLXkzXclyIy6t_v1w-Est4V6FVnrVXMVMB4pkbh70Yxk8tC--GdNYej_9LK98a4adW0nnKZnUZxAcYQzRyEY7lq0A6g2tzAAjAVsHo-nultYi0z_RXTA6ttkD_5_2M7L7IakGsagyODCY3kpKMcTK1LUGTNKGeTEUwNza1bGB8e9zNj_YDZ2-0U9SylNK4TZ92_T-gwf78oeVUKS2v?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_xePeVRacg5zv5nuyfFboprYKssfPN51dQGG7XWgAZQkGtGAhSPzC_YXJdodLPGXIJMy99QtPiHDc00ZJj6EblFjmlavY-yZSIvdyasPA2RdUCcOOt49hMDTQOiF2zFA2iHS5FHF1I0r9WIVsGJuCQKIoGXEbb7dWNpa95OivZnA,,&b64e=1&sign=c5368e003bc8f1f7a955cb8dc98ccf2f&keyno=1'
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/363663/market_HIW8w4hgdebgMRkbfIcDXg/orig'
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
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 24
                            },
                            brief: 'завтра • 51 пункт магазина',
                            outletCount: 51
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
                                daysFrom: 2,
                                daysTo: 3
                            },
                            brief: '2-3 дня'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/VYKgtSJ-hH0vHG9ZzVKB0w?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=HgSUzSrrd38gO7ABCnFPJG3f7Yk-sNh_7E4TB94xsz5rTRoLE5wpT13Q2xVno_VSNvhoKE9lwBTf_rRRNvhfQGYrsRLwbLOD8cgj5c-XcLuODeynPkX-DW18fT4SaFDf&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/363663/market_HIW8w4hgdebgMRkbfIcDXg/orig'
                    },
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/246786/market_BHY6B9d3u5maecWetR42Fw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/363663/market_HIW8w4hgdebgMRkbfIcDXg/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/246786/market_BHY6B9d3u5maecWetR42Fw/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEwDUdLttSr-OZElwUS4nxUbvj4bbUbr6bFtDMhnH34cJLCE92pjwa9qjmg2_vXgWXIwuuA5bwDwbwmN6c1Qx8KwkspZlCWd9v9HfW-IuJVjIbPN_83GyYsPPj1A8CcjR3A6v8pXHHcWNvNEvvjthGDTuU5uVLmlqCiSXSQYfkaTc1HS06GHf8s_iiRE8gW0d6Hjce3BjXhO3JDV3p9J8bekPUSTwMt71nr96DPAXievKYZrN-nsLYYPwGKJuWHGwwl_DdSSfslgd0KuJoqvGRm4f3S4Q3afd4',
                wareMd5: 'lkHhz-odF9-4xVS16ZWl0w',
                name: 'Настольная игра Hasbro Монополия Банк без границ B6677121',
                description: 'Все более роскошная, все менее сложная.Монополия с каждым новым изданием становится всё более роскошной. Всемирно известная экономическая стратегия «Монополия» уже давно стала легендой, а новый поток поклонников получила её обновленная версия с банковскими картами — максимально близкая к реальности. Сейчас уже и дети умеют расплачиваться картами, а если ещё не пробовали, то в игре как раз научатся. Меняется оболочка, но начинка остается прежней.',
                price: {
                    value: '3090'
                },
                promocode: true,
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgymDKxMlErRzvzuJKX55Tm3aQg8C8pzM32Hg7aCoAo8-jXi9GaRtPXgPFSwB_dN8nPnNfOdoMBjs7BCG5x9OET2V7zBUpz4Ing3CRfH9b_vDRCoCgbmki3t_4fXjP1cuj99cI1I8oLpju4_eKx4G9Ok6HjnnawwLeTGxMBCHpNckp98JGTXNNwL6esLncGzUwcsXXrYjcrKYGMZHiav47OS6SAPfZWRChDQnQeWwPJ6LDCxgNOzRadRofku23OXmpc0mviJYubhKaAhK1xbCoyDlkZ_s_try16bwGktLVOBvAk20sQznVDYPxu-TLgHj_ldy3h-kA4GoryjW-HGwKa7jmOh8uJmUc3jd1E8utaplSx3QQreC4WHo9B6KKaG4TYbYqLhHkiH--C7kRv7fZ6x4zII0ZkM6t7Yi0hAscSQzLYzOUHi5PeLO0hSF4QorwtgqHu3kxdMkjIt-MolmDvgqr2x1l6gC2z73-s4Poa8hAv2Dj4oZZnfy8St9JYUYMyWP1Lex5eGVa7xQdfYZksqZ4pJD9UHszRw6mt4VbNTDJ_tCd1nJpnmK0sNEkN85RBL2lJEt-Ls1T1_FNtvZd1Z-yzDaplPOsYo8VXuRgYJFRMJ3XLfcCro-YhQiRxsmNDjM_vShLwqg45MgD7M2uHb1BAmO1h77wwy7ZGGqvBjAuwkHjY9VpMjSL5uM8R7m0kndrKu3-G9SxcCYWAxDB_r5ESMVN-cMsRK6TDmIvshR0-liuSCXonbzIStT8zIVKXslDqA3bWqsfAJqNh0LVdOOrlMuRht78itY7PfwcVvM3261zEc9pQ_eAyAbpIo_vGt971uZis_1F1l46JlXRS7KJisjrUFrDV4?data=QVyKqSPyGQwwaFPWqjjgNl8aC3_D4V8KiBoXGRhNJf3aaxwmoHDOFGvusOJk3c9mdKtSRIl8XajWJkBA9ygDutlwtskYqi-WzlPPeLIvW2Gl5sgXTCkijdSc7_-jET5ZFp-zGFDEtdNmvt0gatDNVSCZdZT9BnWf4WrFU9yTyh6ILcOB56baL2tYRJgjTNTKSRfJUaNfCeBELlk6STFAsHTNBOVYJ7MugLt4LWerj-s,&b64e=1&sign=7aefdd0495800aa5992f30597a6dd6a5&keyno=1',
                directUrl: 'http://podarig.ru/strategic/monopoly_with_cards',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRtNkb9mq-LhCBN_kucu1cXrdlmgEHtUJ-gCxgmN4rox2_JSALil3QvvxzV5ZnpyKU96NWi6IYjtSI9hpFmI0ijxiFh2MpoMVew4M_XGq48eqZ5jtw9c2fwdfDe9rmn8OGpK8Ji9nFUWDZnHGSiO_ACqomIoXbTYKOC6o7KWIqs1iXAJ_OiM4viWhGz-nPZ6NnjZi4N5eawl6hBtLTu-RPIXLKy1taejynx_a0jEspgXFKqpPCOmSoygiN8G_q9DgLSnpDZ98BAF6FziKKKEMU5CALy_7BFZ6CdvOIL34Gea8xeAFoe40FCPfAApN4hquDyqc4Q_lxWrLVv54CsQ7O-W3hkZOQYBqTh4LTV09XdYzzQZA3Q4FSZX2mR0XGOuf9nsT0kFV4J2goO0MlFp_NMp4g2dwJh0_uRoC9L4i4Srg2JDDgZYVZmXDRBnhSG2L8EHv5QuwLHIYaJIGwmfPv3Ge4UuKqwHWpk1OGdqaxOPBkY-H-0kDijglcDCXdsB_BVKs2Q9o2SCFtgOBRvvhSwvyO41xcKODmxE7qxw9NcXAEFBBuEW5bei2oKaKCtUn-PYMsU6jBAWO_pJKz28aevPPdvIyQwDOlZG3vodP83rnn5E8ZPTcLlCwy7DROAzdKjqkW89P6EXXMasBx0KJMAStD7kmgzO2Pc3a6Q-BX6wRVMUjQWGBzlxpsgc9BYui6jTy2FZAAIYz1OPmJ0MdDmHm5Q6h-hSXQDcTRHTTrruA-GGgcSgLRTVmwLp0uGSDlrqDrM--Rg__-ONwvwIlxgfFPs3ESsaNmpFugZU_oTA5ceKPDykQwXrmogNx2ee3kr-6Vuit_BVlApddY-d4sLHui4Smc7AnDOBb2viHIUcxEEuCQ4PNnTk?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2VymyxEGfKCuAN_5zSLiOIGL5U1WyFjlC3vmHOxycsvygEXCIa7kkSEC_akc2PZawDkaeI1scgFDPF4zF7n5o1DB_ok4r_Jtbyu1_mHYDUBgunasmsUTSPQ,&b64e=1&sign=d875bf406893872165960c873e090f30&keyno=1',
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
                        count: 41,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 0,
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
                                count: 1,
                                percent: 2
                            },
                            {
                                value: 5,
                                count: 41,
                                percent: 98
                            }
                        ]
                    },
                    id: 395204,
                    name: 'Подари игру',
                    domain: 'podarig.ru',
                    registered: '2017-04-09',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Волгоградский проспект, дом 32, корпус 4, Офис 44. Предварительно необходимо созвониться с менеджером, 109316',
                    opinionUrl: 'https://market.yandex.ru/shop/395204/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                onStock: true,
                phone: {
                    number: '+7(499)499-63-31',
                    sanitized: '+74994996331',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgymDKxMlErRzvzuJKX55Tm3aQg8C8pzM32Hg7aCoAo8-jXi9GaRtPXgPFSwB_dN8nPnNfOdoMBjs7BCG5x9OET2V7zBUpz4Ing3CRfH9b_vDRCoCgbmki3t_4fXjP1cuj99cI1I8oLpju4_eKx4G9Ok6HjnnawwLeTGxMBCHpNckp9VhjroUlouCcLyvxHzmG2Cf2b2LymfyoB30j_F0qelJS52yn-50yswPf9NeZrn4-oN8kUqpuH2ZyfHAt9Vh1lDBRkDA6-fGZLVP5IhUZARXgFjCOOQqyIKUwtsTx1raCIrmZhh5qDDpAsh85wwcNtR0pDEqCbqDcCzr1wkNHTxrxiSTAQDnqOyt5npCouIJxuFUA3MbchLQxAUGkr87RE2evvtUzpD1HYFrtUsGVA6bt6R5tjyJTZ2xpsrGOI2RN49eQOmM9CuHhl_PThaOwB6ZtCD2ySuEY1jiA-g__mTCfhU98AeYt7ETPzvBC5r2Q4GX0SYcBBVAlSG1bvTdvXxV0F6Wl9fefJ5aHwVKgzcwYGZF4WFfCh6Jim-vWmahS2RIirlgvhgGaYeQ9jSGx1bGklmzxzVf-4xDV4hWb3nfWHTI9hjMA9S451AqmnSHDSr7K2en3XtoXbbh2V4qJkDqYVlzwVed6Eynnythrgy37PLZepj6fnQOws0ejgInwLpJUsH6KWHhZLwEeFsmduUIoHDKiaVXX9YsUtHLgLVFiAWw9wI0ZDouBQbEeXxSSLG82L_SHYJZ_Y6yJGsIFMX1XaGF0q3okjH4KvWaCJv6H20M6d7gv0a8JW1SYP6UJ1eswmxlHgoR9ggaPL1FUHPpyDtYmjD0jp0ifuUJUnQRCXxnIe0A-Ty?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8Jn4VVu5t9RAXDOJh7-3iE0NPPT1hO4CED8hUwOZFwVsNPydzIq8k2mhaqFAATaadl3w9ecKgSNJ2MX1p9fyIX9LLbb2T4mhlA-Rf9lDuaRDnovh3_PYc1EkSWxVeca7L9HUtKGj8a3S3oaVSmk0xhIp5u3KUR_keraSFnR6TP147T3X21fiEV&b64e=1&sign=fb8bb551c83c990f4be976b54437b917&keyno=1'
                },
                photo: {
                    width: 650,
                    height: 650,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_51_kG6rRLVBVrIUe2CsQhg/orig'
                },
                delivery: {
                    price: {
                        value: '0'
                    },
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
                                orderBefore: 20
                            },
                            brief: 'завтра при заказе до 20:00'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/lkHhz-odF9-4xVS16ZWl0w?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=-JfCYqr_odW_S0NMeT0yxd4agWSfd5E-_w8ALpPcu1DHcau2sSyqp6P9zjPICiTpOkhjmAVDw1h900_SL4pHXjLLtwLqa7YvxKhlynTspDhY4dWiYfcVbW3vJMyKGN36twjb37CXbnk%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgVT0Cfd3Me29Y6ycHd2aiEJcYXJftFNm-KnwtEg-gTh925LPWDf0SMV5rvmgqgtR1aqn11OuW8uObHbC3BqPVavcikUcTnBFmCoMjhz5Mf-tWzo0n0ztejW72JDtzsOs2CbXp_oVOls_7BPrYjRdxVj_is56wbiZj5ei32FDRpM3W5SIZnkXZOQNKeV6jl9QnWc5ey0I1zQVHkCumZDTvRvgkNhFAjIByFlOR_oDt8RnOTo7HSd7uHirEiPMV6rypQUlFents7HmOYCWZj6L-VR6K0BqKjc5Lm8bJqfdokmixoFn_npdwxWhshbOuWIgIAmvN4IXNHqyWZPxLRBXnb2pbKhYGllZMUWpeuHF4s_OtR1nJPUeqdPTEPKYG8kj-A1YdhHTNpLj28wHFmYERDe2X1TjgE8xHiDWpCEIHsALz3p6aVngNgnbbIOk0fFJSfkK5ylw2ltODnChdUZnFZJ8CrsLBaCOY3PVgPe9sntQNBa7sHzBa57cAoW7Ha_g1aXLuxHgcaTcIEz3ydQAWUh1IMFJwM2Ni2dM4vKufvTU6reWJUI_4afLvw4376hxPkbm3GC568x3f08wzmw7XZSa1sUuzsE6SCDCV4ZBqaTwTT7Zp1ZI-Kyy9SC6dcv6lHRE_8GPozopBk3F3-jhnCP-Y46hDz1bmIWAT7kFZEgzbCf67SVd5yZ5dUsbOYlGuDCiTEqeQg4FLYN-5XnKFZ506bMwHeFke3ngYv7eu0nhiTUMAdtpGcgJJ6FDZiaSN9G_RFCNT0kNUN1GSGRd5MtfuoS4MJ9AFDGu2O1CMdMJUBVJN-zasvTDe6oY1hBrxU4kKWZxyMG82aBXUFQ4EjGxPtqZvRIlTQTHjBIjU0_e2MKwm8Hqfw0g77ZNQRzBxWq82tbXIKC5?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6SR8_J3YsECCdema3JOzJ2y9v6_OCdFfEL038N82D0OW-RNF-FVYivtkEdErYlXw9UHiewB4zKd5xVX8alQJyTkc1VQhA3WpNzeWpfm1x6prSuS18r4Ey2CqL9zMafvfBVYA3OKmmX0vvPA-Na4PBMTqpU1Dt5ZtAEKACRd_1xyrdOp-eQJTuk7YW2n5CQFxAiwGamSCa68jdyYOaO3vSYNJLw9EjjVqr6-_U4v7EhLX_or1En7TBiUZvz7lISULJp__AuHHy7_jmVnLoUhVDx29g5MFxpVgMEbwN2RPVk3ug2OmQau57vhZdoCNRbXzDZMzdIIYXhYQ,,&b64e=1&sign=897137b3b48c3dfd47160d1b8342dc91&keyno=1',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: true
                },
                photos: [
                    {
                        width: 650,
                        height: 650,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_51_kG6rRLVBVrIUe2CsQhg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_51_kG6rRLVBVrIUe2CsQhg/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZHO7kYrXm7qb2_WO2z8Dx3oRZlFO82ESVSpQhwIvj-1vYLnU44atZaVdZnA8R3IXyAIZ1oDltehlfXJKOa5mWglg6Zhjx2On_2mmP-WuYgU4A18CFdddKbl9IhAOiOfgwXjIvrrb47LlygC0hsOMCJy3ed89UDl2fjVuzmgIl_CpP4OI51XOC9Ppq6pfOvHhm7kaadtr8j159T4_tMXXVJCxqiAhPKcQS1DfmF6yW38q4NlfriWz1n4T_m_4vr_-fIRwzlAeNjAQOIrEQ6tavztBgebcXmZj4M',
                wareMd5: 'jIUvXKxQYCEPe1tMm_hCmA',
                name: 'hasbro настольная игра монополия с банковскими картами (обновленная) b6677121',
                description: '',
                price: {
                    value: '2490'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYZtQG4lD-7xs74dEgtsAP3JTY3WAuWozO_ROHdv9owvmaaJ6rTtlr4YzBKqxW_ZQkB_XUyab_YGADSAhg7HnY1gDgIeIvNUhRvrbjL2lQ7B7x7VrmY3Xh6G0nXrbYuRW6cA6zJaqglpKMqp2ZthRkwM5AD0B-WoGSYi--JKTf85VA6YZq-2Jjyu0am9Jc77aL834idk5BjRtaNq6bXjwpm0v--hB9xKnW3eCAvKkOtmlgKzEZ_xU9GkcPxYGdZWnc04SOnbZiIFOREQGHIr6u_x5WNETPu8NBEMNV_uuBslUeYLv8j6hlNkpS9t-0GPJqrbdWYPKaSPd9KJx6gJAif9nSknCW34mo65elfW4d2T_7xZn-0lLMKLGg6camSG9zxTKsLUnNxu8c2BgcXqd7rCN2Jew3kxAlAR1GhK9VH-YqHt7iZkhZwolcIRIueR4X-AmuyFelRNwtA3KQnvcTQYf3Aw5ckiQ38AFXYlV44TPVD4OG0v0YQ2qq9EIPJLmTrDwGHNHgjoPl7G7r5PKfkQLtQnfIf3zhU1XAtZ1y4iZu6-nYSz_tlkW_SIfCUXBGyhZ5dIOhSsmbLJz2NL7MhmtueQziu7uipz0fNxU8YLBYVHuLjDbWqqqkbtws4Kv4f83tcBsKL8iV704wUrPSHYIdlUGTpRFFz-3uQJfhaslekdrDyfNcVXi0ng15MDg6mGG68mQpucMTvzXLka5xSpOYEdMDEUdxWp-lWr0w16kfZ5ZhJ1x5FuHOgEWUDkh1653lfGAVPAOOr9Ew2KE2fUJAIiYAxRqlDa1gpwRxF8PkcYF4vAjpelUGAeQsUpINq2HSHbm7_MgLfsAOLQg3MFjen3_MLFht_zbJ7GMxLoMVcAZmtFv4w,?data=QVyKqSPyGQwwaFPWqjjgNg2oGQjHGn1O7jiJv1h69YpRb87amKxokaDzXyIyus6hS-3BfdkSShIuWC82Ba_Ca7fwGVOLHgUMWEiNTGSsWpzn84rRrCY03f2NR8h7b7sG5i3gVf6wr4MpyM9H37m8udgzU_-a3ss8LDEfKh5kPfM,&b64e=1&sign=af4e4228af03c93b34aa26956b42e2de&keyno=1',
                directUrl: 'http://www.alver.ru/catalog_thetoys/tng_monopoly/id_6/',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRu4Y2f_crk7D_jumWFuma4RckhImhjge5ztUeQs2FFE6WihHPMesgiYjnxu7YRmAwDwL3IABMvaWetNZrjPnz6ENjT3k-y9-VLT1vG4wN4fspEri9XQY-ny8EM5rsVdSJczItQT7hCZNGUQVeP4eXKIHMGHnoB0Gz2zfk7j6-wvUZQuplRE4Vt-AjE1fw0oGxv-LALaInOb_p38PYf4WqSCPMb9vS2eCUeLuniBPQR0Tt2ppNqqVdZt_cg3GauSu8YQjZBuVO3_zGr0vGMOB0ZZmY3mAFbA25xwbS3x3ViCe7JwT39nuMmJUf3JQDiRLjohjHnTBk7khCHb9sx49ZMHQdSOzS07CowrQpDS2YONmhBWO9J1e6Tgxd3Py5cAlBGjTeoZOl3SdRagomiLZ8dYA7n2j22YV9B0GmrENlAHKN9ZdefOiPdxkxhdgViErpBF-y12IV_SruyXgoKsseTi6C4Ecq4mr39y2REGIMmrTDHUdd3SHw3AdU1D6HuW1lTgWE0gk9m1jo50KRX7nKHqfxFHKGCTlDjVkUxqLDGdd5jdAdgaUfwrZh0x0Ugr-fV-MA1MrWyq7pUIJQPELeqr5_7BMjxJNq8p49uJUEhfFdslOLePMOsrxUAKf9psLIP9x3CL851m4XvZBsw_3JudqAD-oPjNcfPuxP0nN9nesoyj2rxLxYngNU6nHsOWdXW2hqR1hcmUdgVJ9zrKSvTkdxv8JGess7EP6RifVjrXrGagw5VicF1Pka_lgKTpEkSoTZdvzVRZt_LVe8hPB1aopFEWTt77AjXRJNvf1V-EKUCZiwu2Gt-YX5dGxPb5MwhZckbqI0x9QuPZPbKvG9e9wvp2T9LRlxTdsrQA9mv9jOYHFLbjziHs?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cgwrLCWA1nsenagTGKIXjOjNlABujt3yf3AySyT_4cyYPLMK3D0F7ONVcq6atUVNENBV4xn5eMaOkWtYiq1l7j5N6ud3GxrOHd8LgLC-EIpwL_Itg8OfMA,&b64e=1&sign=3a2fe3d02dab98fc3356c759e8cbebd0&keyno=1',
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
                        count: 2403,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 118,
                                percent: 5
                            },
                            {
                                value: 2,
                                count: 55,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 45,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 270,
                                percent: 11
                            },
                            {
                                value: 5,
                                count: 1915,
                                percent: 80
                            }
                        ]
                    },
                    id: 17737,
                    name: 'АЛВЕР.РУ',
                    domain: 'alver.ru',
                    registered: '2008-11-20',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Проспект Вернадского, дом 78, строение 7, 3-й этаж офис Салонсвязи.ру, Алвер.ру, 119454',
                    opinionUrl: 'https://market.yandex.ru/shop/17737/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                onStock: true,
                phone: {
                    number: '+7 (495) 796-28-27',
                    sanitized: '+74957962827',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYZtQG4lD-7xs74dEgtsAP3JTY3WAuWozO_ROHdv9owvmaaJ6rTtlr4YzBKqxW_ZQkB_XUyab_YGADSAhg7HnY1gDgIeIvNUhRvrbjL2lQ7B7x7VrmY3Xh6G0nXrbYuRW6cA6zJaqglpKMqp2ZthRkwM5AD0B-WoGSYi--JKTf85VA6YZq-2Jjw4iR3O1BHHYgOU5AV8lhEHt29grEtSp_K5lPGdEWsjrvFqfhuyciBBGHIHjVCCkNZHinUKg6pc2kG46jk3pIeyhAnehrAi3uEAYZbnzVbDEUmN8j36lFRNRnhm1aLmJ-_BqzHGRIn5tZyJzyKf_QPf8MYyNp2YqjUdbB1M4Nk40pTkTFKk4VbuT8-eeiR8iXaULOBuh0fRiZbbGd0Gkdz7RJp0Iy7_ASYW5O4Yfsw4N8OuhEmuWC4J1ctpf-3HJEGnOXNbfFy4ocZYgIo_i-tCLY1ryEBlAL2eRtZjnIfQe6uC9LhAmTtGf8k8f5eoTNbA4WzRoADugmqwMYfaZxv9aDCdcWY4I5V6xJcYgwfFdAchs_WE5e-OrKfBYGb8uZXqv31Kwgtm1hy-rJ33pYOzvWUCrj612g0cuonloKe6TDdx3JlBo0VS3-lmA4fxClS-fW9kIb4JTZbYjb4SfZb8FIUec625w-4eCw6vCApid1hxQU9HKXFSUz_AKkkJbUafjcSRmJL6kOQFlDoPuxwoBvZiW1pyjHIejMZssc1Ii2jRaJviWqD29uFckVxwUuVJYMeUAKlwzAsAUpUVnmjD9VudRLHBrvEnZKTZZvSITeruOArRm9S-fod_V_6NpLQv9unXwKsDAWivisY_LfYUTjUYeQfrxcvY1PTtmPh1Tj0IttAHm5TzARzgWdecgSA,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O99tNuGjKnqMoKi2EW_t3hoFdE3s57dmNSeuj4V7HdlXzfa7If2UJlYSwtHal1q9MsEhQnpuZY82hyFl4SxC0YAxh2bEuq4M4QAbZD92jr4xDv-l2eTIk91wCDgfe9yewckDkXr7pZTUTdetErqhVqR_uDq_aWqMVshpZE1vnBjPDxQkTEtWwHu&b64e=1&sign=44abe4e2600f88f3ad6113e139545a49&keyno=1'
                },
                photo: {
                    width: 440,
                    height: 440,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/203248/market_MaYjNEG_NBlc8TljjLdztQ/orig'
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
                                daysFrom: 1,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: '1-2 дня • 1 пункт магазина',
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
                                    value: '350'
                                },
                                daysFrom: 2,
                                daysTo: 2
                            },
                            brief: '2&nbsp;дня'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/jIUvXKxQYCEPe1tMm_hCmA?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=shyGfZboeebS2GZYDhrst15OKYsoFwBvlIy1d0E6DHCbsEgMOcP1LyRkejFlxh2spcVNxT0bwSJQJxXgzqRMoIlGAP_-vf24O71ZG56bxgoD23LxF2RL5M-yt-c6zk-wOkj30IusXjg%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgWPg1NFH6BomTdUb5AHBQC-NZL5Yuzl2mePg7IwkL33RduOap-SOvoVA9A6TEkrhGx7DwsKlGZj9QhtvH8APHxU3aXuggVBz6XUdKQjvpfzyci7hjWrwv4J3TzGOMjDrt-GzUjWzuotk9um_uwolxGjTQbzv5EGdmMcUXelkJbnE4krgOCkQIE0B6hIsJKMBZGMLAW5RZr7qSyPS67rCgi-YcGgQZozew-nVkXat29kU93Gqz2Qo-L5Pr9SozOeRSyTcTyZZnszzdjbixV7_G-yE5jX5US35hPWcsDhZI33sPqU302UNolPlxZ_G5PAjQor-C-jefNjhdxEopi0_S15VelRfviL8h8hYAbHeq8akyoFZH_zveRT9RlpRsLj80EL2LGYG-ycytuTUAnsBtsubyf8jxPa-2gppBxRWFDvl2yYlVOL2CJQYTppzYg5o_1cUNY-Rc5SoUOIT6lQQHXNaSZrvtpeLtC7ahIxiwjaK_PL9n941GsYc-mp4fHWLS1SjYf-mXLp1zCqH4TJ2JSGHPVBUD9DlTxt_ypAL7LnXxVq_pNYsFV9nS0MUCwdG8_xXKZE7d51l_L6NQtY9Ay4q0T2vA4Tm_NCqnPqmbSCv-3BAAidDasWBkES_2PygxGJPoE5HSLnIax0ItFLotF_ipD9lxAOHAXbqGD8nn5mL2Og-c83vfSPN8eOqC2lr12vJoaOgTM2AdtaqtmJC6nUOvNnLennJ37E02DXSrHSP_hrvWzqjkFs5CeCfmPRmrDcAMx78tig79siiPrJeNbKl41KdmrJ7ZSEfSUnASu9NpRGDGQJJaTpi-Nd7BR9ythRvKv7Zey_0OvLMTppCo0v5in6DWoSt6sX4r48h2NoP5ZYVK2MFYOArquebuMqi0S2q-FOrCzPAhcOvn0Nwi_E,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-4y1UsTYGsuxFxwkaGphRdrJypKdP2HLgOeuB0D6tpln5urBV-vmMpGKRXXkc2qZPcCrcItDafh6RLJGcfN34sS820Rqxcivko7jZKTO7RcF5zjfEFvoCE3CXBgcKacA0sFXxD606N0mmNb6DEGaxj1Qw1F0y62_BsrWFC_IRhaze_OevhhP3EV7ulCdZxZnE8qvt5GdcxFvFY34DQMc7PmjhXzTHc8WuY-QOmODHgrI_-fx_ycAonEombUDg53w86ezFoigtbDAez9hc_oKC4VxnwzDilMqg-XYx0rWdmvnb3stEem8x8Fu8zBTygMapFtmPDNOXjujg,,&b64e=1&sign=3a27fe9568cbc40bf37e452ecf705deb&keyno=1',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: true
                },
                photos: [
                    {
                        width: 440,
                        height: 440,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/203248/market_MaYjNEG_NBlc8TljjLdztQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/203248/market_MaYjNEG_NBlc8TljjLdztQ/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZGjqZYZGM9ZWr-tTfKFmGJV07D6-wmih5QgG4V4ykwdBQ',
                wareMd5: 'c6ZBiBXArZ66eG1KjLsQ9A',
                name: 'Настольная игра Games Монополия с банковскими картами (обновленная) Hasbro Art-[B6677121]',
                description: 'Размер ДхШхВ (см): 5x26.7x40 Объем (м3): 0.0053 Вес (кг): 1.020 НовинкаТеперь все карточки читаются банковским устройством ( в том числе и карточки с собственностью), что делает игру более динамичной и увлекательной. Просто приложите карточку к банковскому устройству и деньги уже зачислены Вам на счетКак стать победителем: покупайте собственностьВ конце игры она превратиться в деньги. Вам нужно иметь больше всех денег и собственности, когда любой игрок обанкротится.',
                price: {
                    value: '2343'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mfy_tSMWrC53gSsPWYMv8iHZ-caGwP7aZm4-4skGLz2_GgwrmjFPEMJHzd8_icc6nVMEn3VvZfLBoMlvGec9RykurBJhG5-YfMNR1lSkjJWfAk9CC0pisJpZAAHg2MQVPXcpUZtlSbzu55NUfZOBNOpuJaSMZ97wCqcuMPil2RKKCNXhl0qZu3DE_aBhXVS4Tmgj1szmytXnGIudT8IuNR-mpkggInU2sYPpp85WPmCW3wHFZkxTM00fCfR0M2rQjw4zNUZQRjhGv4WelJz4jxznseLesg1yPY60XCdWoBteNxXpiO8Te-Uxsj--XH8-ots-asCM-tWbU-R3ROkrZfJScqc-HsH_pIDB8Tw6zoGlzfDE5HykXbLU9kzg5E6pxOWBXcPA0ykLGw0HR_MRqTS9wJvsbEPKMOa4AAc0eNXEPb9AeYal6El4Vhym3eJRdUFdVP58n3Mgz0HNr-tnFrSzz1OcaEZ7IvDWmRa0U_JpEV5jK__5Xv8D_b8oWqWRIwY0dJ3G_PrnE5URKUWkaUoagRG3HHj9i__JKuIZk4raZFhFFlCTGQbgKtyKgWUZbYOlTE0Uo2X1stGiNPy-Youp7M96BxHjmQdg94cdvDOC6JL8v0b6aG6MkryLClYwHlfYn_wSPFqeM36mA4l0bsFjS5RoLZfPO6QTlTLj1yczViXyzLKUjjHzQeDXJapZm3xfvgeih_2PrmC4XM-f-MC5bs9Ju1q-sPLdtKoyUCyRK6DIh28b0dCufFuRmoIRYjDAtb5VSHSx_0f1zK9OPzAQR-fWIwv_TWKz4faKAl0qP0ZptqDRmOUs3QPYsl0hX0u4TA1NrbUFI_TvKYp2sNl_3LofW3NFNM9PSQ5gVna-?data=QVyKqSPyGQwwaFPWqjjgNg9nnmsfiFMidC7w7Isd9ZfCPUS79RqtNDSn_FrfV2LwxQ6v0yr3fwWKd75IzYq3840tv0YwlPqoOJEgzEw16AkXeEGZLPUWPgZJl5GoxlxpHMZgV6bB1cbznbMLT_bjL-M6S6fz_EjTZ4rJfmrDVCzmZd6Kihebz-vQaUKvo2b5AWmqVqXsDJrYwuFl9Hen7g,,&b64e=1&sign=fcf6a6a53186cf6488fcf05c801b6817&keyno=1',
                directUrl: 'http://igrucki.ru/product/GAMES_monopoliya_s_bankovskimi_kartami_obnovlennaya-56408/',
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
                        count: 114,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 9,
                                percent: 8
                            },
                            {
                                value: 2,
                                count: 1,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 5,
                                percent: 4
                            },
                            {
                                value: 4,
                                count: 1,
                                percent: 1
                            },
                            {
                                value: 5,
                                count: 98,
                                percent: 86
                            }
                        ]
                    },
                    id: 208435,
                    name: 'Игрушки.Ру - igrucki.ru',
                    domain: 'igrucki.ru',
                    registered: '2014-02-12',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, 2 Южнопортовый пр 35, дом 35, 115088',
                    opinionUrl: 'https://market.yandex.ru/shop/208435/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                phone: {
                    number: '+7 495 204-15-01',
                    sanitized: '+74952041501',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mfy_tSMWrC53gSsPWYMv8iHZ-caGwP7aZm4-4skGLz2_GgwrmjFPEMJHzd8_icc6nVMEn3VvZfLBoMlvGec9RykurBJhG5-YfMNR1lSkjJWfAk9CC0pisJpZAAHg2MQVPXcpUZtlSbzu55NUfZOBNOpuJaSMZ97wCqcuMPil2RKKCNXhl0qZu3D4bV8ZNUhQE6Jopvs5waE1Fn_LF_NO_agH9pdaxt49KQxjsnrw8ILYsq8pN2vggxaw3kGAGD2QjbVdYuK5suwHlwlbVrSyAWk_Aeif7EpDg0CYvC1fg7wAvkPsxeY_3ltFpL33cdNg6M_qSRKT_kaMzqqsLmus3X9i1Dne1F3jruLHEclHeKl4C0rPzU5Ufz9KsjgAEWDA4KHUpSIgjwBqBaP8ZfbaJY6o0h3QHXGEu_yjeUUtg1QlqVkCFDf45yPsM6-LhTj_af-_IAnxZaAvesQ3PnV87f4fgZ3fKtif4vJtq7yUBNQP_doGFh1-9dpKGnDwYs62R7a9ELBjizeefkcikk374LzA5Yam_WimRDgiRcP89hl_3MBqf9af8llWpqr2gnKbtH5Nm352H29mBu5fJDPYZWsvsWJkO6h6j3v88o2xdGAa5542JlooUQ3esZzPLZnM1GHs0soeI13jo-lhnRnmCzvx8JVce9_LpFDCjXml3RydhVuLlkkKMQaQO-kgvqgOPKLUQyM4pJokZOmeO1U4pYGuMNs_mjDhpcbVgvh9pYQq-gLulYp35akH7ZgNW82wz67s8w_2gT5a-U8O7tVC3KDe90CasSH1U_X5zLLKExEdCrevcNyda8CD1c1i3ryiYxdBvDvv7xbYLFV5A_IT3yuynfjTdkaFDFeJtKDcFNdB?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9RTrz7-ntfEmcBI8bDZpPEKd5VpRJe4JaKDpaclcnr46SZzfmdZtO50AiCei8BsGJbdnzjioMdyrJ1_ua453XUNVgEhS7ZKyLk8-Xe5iJA1W88iBEgykD8Kjcjr3KedboYxGUCChGdj-gGlvAVqNVhkk9Zz3vW1GR2G8zUQr2MWjEhmLl-flCm&b64e=1&sign=89ae7be88ae02781e7017a4d78ad6b69&keyno=1'
                },
                photo: {
                    width: 900,
                    height: 587,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/486815/market__VOhwa_yXR7RARRJeWWl4w/orig'
                },
                delivery: {
                    price: {
                        value: '250'
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
                    brief: 'в Москву — 250 руб.',
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
                                    value: '250'
                                },
                                daysFrom: 2,
                                daysTo: 3
                            },
                            brief: '2-3 дня'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/c6ZBiBXArZ66eG1KjLsQ9A?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=SCK06JiEkdf0E-A5faR6rmuBJOWqNSOuFHdBfGBnrphK6azzbIEme2vqvEo99NlMQ3W6EjJGMBY46I07LOQRA_FYnfx6VGeA7IIAmxSv91_Ajl_K0hjZbK-mZPfTPn1N&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 900,
                        height: 587,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/486815/market__VOhwa_yXR7RARRJeWWl4w/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 123,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/486815/market__VOhwa_yXR7RARRJeWWl4w/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZHrBFkD1H14hmUH48HeBLM921a_ykJ5iugDdNyCAbrRYLhrx4qI2O-sxG26PI8PdGRTBglcWXqviRr2-S_qlT7b4oYPVNbAYxrAQPfifmxtE9PCRGTCX6gkZYaQxJPRf6SWyOeOLXaYlNucbGNaMQrfy6RPzhiYTeha7j_7tO786YXv24aScX8lACr_OBBi_lK54ywF3rTEbBiBpgiw8iqmpsK8tfk2jNTjcUwyKOeXzndw7QpeBjCamFXVSTGL7bwFyX5BHa1fcIXW2AZwkomGqqw5Ve16W9Q',
                wareMd5: 'ARVKUEYLbMB16MArPJRj1w',
                name: 'Монополия с банковскими картами (обновленная) настольная игра Hasbro B6677',
                description: 'Настольная игра Hasbro Монополия с банковскими карточками — одна из самых увлекательных настольных игр! Что может быть интереснее, чем зарабатывать миллионы, руководствуясь простыми правилами! Монополия с банковскими карточками — это обновленная версия Монополии, в которой нет бумажных игровых денег, но есть банковский аппарат! И у каждого игрока есть собственная пластиковая карта с безразмерным счетом, на котором будет денег столько, сколько сможете заработать на управлении недвижимостью.',
                price: {
                    value: '2985'
                },
                promocode: true,
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mSjN784BcAcP1I6dp5tbwe8uLFTp2GmYsZp3OHF1y8wd49-Oh09uB2bVXaXRLTWphambPQcFfb3Bqr5khosn1pl_0jqbwf9V_GrsnCqqmzfLoHtvv2t7Ox6H4w1WrpTkKVFO_YmpT8sfQn2mIxBl6mvN6GKso1gojj7j08SoQRHkTdSo-Cqk5v8yc8ycOzD46zilnwFn18K_ppIwxFrgCSQecn4ok6LHya1m8424_BMTT6bxRxZ55YCyaHrUsFwCEb4I59l0NLTTPiO8ksZegWd2OcPZE_5NUMtA2DZYwpUsDyI3xxtPuUMTK1d3irhMuKGM3uCFL3iYTgmRIkO4IT6LV-_6aqDWdPd7EkRquArgyhfypCBNzuMDK81fMNVIDFW2rr2LWzedYbHVd_KUME96GVLsYicQ7hBtdrWZDEO8RTHamHoXje9Ou5UEOP3nNNa0EuB2nGH2zNh7rIvIrMdF5L0OEb5_J61rndXlC5ExseB37f2CJd6puVt_vaStnbZ5juMj9NOArHCTpIFqVo4jhsdhF-BwelOvP_k57Tlj_6XP7OOJd8LIbCPyA7XDY2A8cACF1-B_TjXMAqAhqqPo2cXh1RJdAv7R2K9EKw4ibyupqgJbiVdkFTPbemCYIX96h6s-A5YWBL-Z_4Xi4Sj0ThgoAA30AvFVOsOVmW7UR2mmOoPVoLrjh1NkqA_i9nq56MMdg-YAy959br-CB35Lvu4cK3oFV_QvrXyIvWt0CXBlFi-n8CHmJxvW8-wq7-LZqZsk60ULYoBEN-8aeHI4CzxVPFIgCe_BdvCtbGO3Qgo34n-A7I82WZ2o7luKBKGeyGu_noyFouZ7HgdJeeo_D4tpSC24lJqUaRttnx4w?data=QVyKqSPyGQwNvdoowNEPjTuL6UDLo_yLuah2yF3m1oAfIbQThK5PUnIWwtHpMyL3l_oL_yuB5Mt141_QeOTxGLYlFWQV8d1B06Sp7nUblTHExuMSvvjoFVRIIEASlFCyKl6WaGCbHg7hqNbSBCnD4Zi576SLigiAuur9ATmSLoM_Bs_dhNktNUpGgbOUuVXHCPjGOk1hjOHy9vHH8GJwhs3Rs8K2Yaz9NL4VwJBBZCpE3IzeCRthoFNYJyWTP2rJDXjV6bNa6LjMEOOTerTVxHXxz0pxfzOU89FHtH001Cd2n92xjGi8joOontHxzHctOXgLtfe2rZwyWZ_bp2cDuyX1kXZZ5xEG7IVdUcEgsD29u45xffjw7pSkl6HAUXfzZg6nS6ESrWabUpxMOAMyhg,,&b64e=1&sign=89b2163c60db21f4bbb38ed22a71e8af&keyno=1',
                directUrl: 'https://ovdi.ru/shop/tovar/monopoliya-s-bankovskimi-kartami-obnovlennaya-nastolnaya-igra-hasbro-b6677/?utm_source=Yandex%20Market&utm_medium=cpc&utm_term=9445',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRufkc5G38e6HWUuVTB10SWYboomLJQKWwwKqE7v4LTNWCqGncYhg36JIKEFgjzTpGM-0P8z1YCuIbSAFybU3M9lTaWMb8r-SuPFTbKKdK3udk4bs1ELRO9TopFAxZey3ZGmhzy0dcKdNJsxwo6EZ2J-v0jlHS8CrMG1cRDMeZe3k0bWks_Iw82RV77_gX2NRtEhAm3a5Yi1hE70cMFGQTg5G4bRStJ5mIXbrb1bVgzGOLu3c618B_aKnpfZ8j3vMeL-pIYCuXTWiGC-vO9c0GROt4duFJV8wJxW6nCO-B7s5IWzMb3isan9FrlVgvrX5cdxMcH6JENpgEkkS5sArZ3XHH_T4TalnDAzs2-AoWOEcczKVtjgf6VDPuWUsB56h-Rw1qnFm4IIrNcvJNPnmiTYwEJ-Nx98bcGsUgZE8ADUzA-zhxZBVFduJWg_9hlyJrp1zovohPJauEP4_QgQS1IqEoyryWOd6w-K-_3wtFIGZnaq6OjVZaR4970o9LPYpUhfwm2SGTP1OKbyhglkCyHIvkaQVqHUEhXYUrmLQdh1u2zqPHi93qsGcb6AUVuGmQ6YXu9IAzIiUgj3JDyVokJzagBENDX1ZLtdoxcZ96l2rmHqfxxs4vs3uv-kfEeF5mrTsse98bdpxdRTvrQX_DCotHrdYH054NYM4M24E739xIcCj3iWrcEUTiShSjibNi5DBPlbVZVKaa-dRMrk3438EzGt6XpHo0f4u_qRA9CTn-1r0e78chduMQKuEdk00t-TjeGeccGOaiGCzgbZHoeoJ2gzf1-EMaP3CeBr7deMeRASPLDLTCJLl6js8TucXWOFlo4MaL7ak7HioFQ2Bg4DdmCV1UgN9J5yGCTvlDKkfrLCS6txBIYG?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XEu-5ukaXj0CIzBX4YmjUE6qj_WLuHCL2LpVhtP9c9QLk8tyjjYSdtKuktHLFlMrxGm00lDO_Bg7kSaoouiGw59xKgvhCm5nVepv8oMqRmlxx-1b2K3K6M,&b64e=1&sign=8db6bcf62f7a0d0ca9add831046eaaef&keyno=1',
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
                        count: 258,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 6,
                                percent: 2
                            },
                            {
                                value: 2,
                                count: 1,
                                percent: 0
                            },
                            {
                                value: 3,
                                count: 4,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 13,
                                percent: 5
                            },
                            {
                                value: 5,
                                count: 234,
                                percent: 91
                            }
                        ]
                    },
                    id: 314283,
                    name: 'OVDI.RU Подарки',
                    domain: 'ovdi.ru',
                    registered: '2015-10-08',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Живописная, дом 5, корпус 6, офис 104, 123103',
                    opinionUrl: 'https://market.yandex.ru/shop/314283/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                onStock: true,
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/216074/market_8XkBn7CJpuVMkj-ni289xQ/orig'
                },
                delivery: {
                    price: {
                        value: '250'
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
                    brief: 'в Москву — 250 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 111,
                                name: 'Shop Logistic'
                            },
                            conditions: {
                                price: {
                                    value: '180'
                                },
                                daysFrom: 2,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: '2&nbsp;дня • 2 пункта, SHOP LOGISTIC',
                            outletCount: 2
                        },
                        {
                            service: {
                                id: 111,
                                name: 'Shop Logistic'
                            },
                            conditions: {
                                price: {
                                    value: '180'
                                },
                                daysFrom: 2,
                                daysTo: 4,
                                orderBefore: 24
                            },
                            brief: '2-4 дня • 64 пункта, SHOP LOGISTIC',
                            outletCount: 64
                        },
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '180'
                                },
                                daysFrom: 2,
                                daysTo: 4,
                                orderBefore: 24
                            },
                            brief: '2-4 дня • 1 пункт магазина',
                            outletCount: 1
                        },
                        {
                            service: {
                                id: 107,
                                name: 'PickPoint'
                            },
                            conditions: {
                                price: {
                                    value: '180'
                                },
                                daysFrom: 2,
                                daysTo: 4,
                                orderBefore: 24
                            },
                            brief: '2-4 дня • 1 пункт, PICKPOINT',
                            outletCount: 1
                        },
                        {
                            service: {
                                id: 111,
                                name: 'Shop Logistic'
                            },
                            conditions: {
                                price: {
                                    value: '180'
                                },
                                daysFrom: 2,
                                daysTo: 6,
                                orderBefore: 24
                            },
                            brief: '2-6 дней • 10 пунктов, SHOP LOGISTIC',
                            outletCount: 10
                        },
                        {
                            service: {
                                id: 111,
                                name: 'Shop Logistic'
                            },
                            conditions: {
                                price: {
                                    value: '180'
                                },
                                daysFrom: 3,
                                daysTo: 3,
                                orderBefore: 24
                            },
                            brief: '3&nbsp;дня • 1 пункт, SHOP LOGISTIC',
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
                                    value: '250'
                                },
                                daysFrom: 2,
                                daysTo: 2
                            },
                            brief: '2&nbsp;дня'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/ARVKUEYLbMB16MArPJRj1w?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=oRqkkl9vMU5nwlLJkeCEyxWSS2sHjv5KSofaSMWxjF-5XGULWKOT532AY7iRL5kAWrKv6lk2Yvz5JwJHNhjGvWopYW5HtjwkODSpV2Gl6UWpSGBqjsScpIxtV7hEw2cVYGGdNYsoA7c%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgQwRUyR1w_hoNg0-nxDpyAInCLLD68IMsZnur40sE81C9UPUsiR88JMshzZ4Zwwi-JXD8SkqGNEVEdu3gNHu4f8Z_50A-fPBzI4D0oY1jdETzYyC4kd1tGx5nDLxxjt-I5EGPdu68_2ZE6KbrN8m8CaF4PKwmL-WLbYsFtw8lzA7Ggm3aEMfPuzhq1X3VwHBan4WZt4_yYgvCr20DIcGWh-IGbXAGYJ-pkmTBBIwVCia5uChJoIqL1mRK1V1_BHLG1YeY_mSikG2LBHdu4LZokUAlXFCtpOghRkcMT_cKIhlBIaqr4X5BX6Z1m0tv1B348Yp6xhwI1H7AkTjNb9MAoXayNqbGUSEsvZVVCoWPNbfQhgZdPPYZHL-W3YplvL7RRRE29NxjR3qlu-hhjXtsprhdFSKPadpeB5DNZmMXAsNq4i0FGkr1mjWlW6M3E4Lt6IBqFnNoUE27XIyF7byIBLUdAbwXhh2sMyVUAwQXCbNGfa-qDOOZH27Puz-zozhqCyghzgO7gGpgxGOzYMORf_wUaJjsJzuMctVFXR3ViT2h08NXURPQaeh9beyXmIF2fIpl1Kjnx7CzQFL11NnpenyH-1qutPJ40KjuH7K7H2dYbEPeGTbZdsQurmVDR0TNxpY4Jf_4VwZPNO2q8vqT_PpbdC0n6HBI3B4UlWYRMr4IFVKR2tiDMUb00GNQWdAu_XpagkK9DedlhVMkpmZY6cMDqIupJ3IHOOjEBQv60No75n4hBdSylwi-9ylKMzr7qfsWLCj-eKz69_kbcmbgTgykppF2ZYPQl4g2bTvo3P4se0OWn_0495YiJd6gGMijdfoMYFupFlOYjk3LFfTXbw1xJkJzEmy52WMnG7c-DIPQrA6Dj3CPXUc-eJKTBACsGRSZYMIQIdd?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-5tRCaJeNdJjn5fvJKqDpvrd_NphtY8NJKleAPeG6i_iNps6ygMbhQgmIzNbHt1IRBNJyiH2WGWSH3dCje6LZKTm832sHRoJFxl3JhkBvs5TstI0qvOU3-gBhFf2FwcXhAom-1oTtvPdAlW5CxORifZ5mg0lYjiHQKwiNWI5zLxAMddkDbpXCoaizFiu2GDF9fCMIDKuXcz1A7JE9MOIv3776GL5kryFgaeDQ8W4XHTCaGRb04KNVHgODkmIsG9Imsjfy3w_J6UGhIwKZp_jx-2K79wTXXDrChcKc5_hN5TW3WqnDvlsndrLp_enipi_YNOPkFQORAHKw,,&b64e=1&sign=386877610e2fff9d475ba384c8f2b4fc&keyno=1',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: true
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/216074/market_8XkBn7CJpuVMkj-ni289xQ/orig'
                    },
                    {
                        width: 1000,
                        height: 1000,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/169660/market_VLpjBkx2ldRRTslnXY58Kg/orig'
                    },
                    {
                        width: 1000,
                        height: 1000,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/231203/market_SH2ZRdi5rmiKxiPso0L13A/orig'
                    },
                    {
                        width: 1000,
                        height: 1000,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168879/market_1a_QZIohgKKnNWM3vWeS7A/orig'
                    },
                    {
                        width: 1000,
                        height: 1000,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/218908/market_i43OuuwtJh3z_GWnJpG3lg/orig'
                    },
                    {
                        width: 1000,
                        height: 1000,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/102460/market_whwdkd_fYLGVlT00Hs1olg/orig'
                    },
                    {
                        width: 1000,
                        height: 1000,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/203633/market_jlbsjbf_vMuvIURuxiXUXg/orig'
                    },
                    {
                        width: 1000,
                        height: 1000,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/213450/market_2GjSYkKPNxK-IcOnDcctUw/orig'
                    },
                    {
                        width: 1000,
                        height: 1000,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/366186/market_tA9TNCQOtqWLZ6ztTIeD_A/orig'
                    },
                    {
                        width: 1000,
                        height: 1000,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/486815/market_GLcaoKnbsrtr8DP2kiNcMA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/216074/market_8XkBn7CJpuVMkj-ni289xQ/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/169660/market_VLpjBkx2ldRRTslnXY58Kg/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/231203/market_SH2ZRdi5rmiKxiPso0L13A/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168879/market_1a_QZIohgKKnNWM3vWeS7A/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/218908/market_i43OuuwtJh3z_GWnJpG3lg/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/102460/market_whwdkd_fYLGVlT00Hs1olg/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/203633/market_jlbsjbf_vMuvIURuxiXUXg/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/213450/market_2GjSYkKPNxK-IcOnDcctUw/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/366186/market_tA9TNCQOtqWLZ6ztTIeD_A/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/486815/market_GLcaoKnbsrtr8DP2kiNcMA/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEcLLEAeEQpLMbTbIJ9XrZgHVWU5FiIkZ6ON8QfCKGtN79-O1zTw3ZAw1_yiwluT8tg_8WJXCUP8AbSNW8eDyXQ1c1f33yA8cQ9TOLFXt5MXmuXk--gl2yViEHdxVtyi11ho2naln440MBT_Re2-nG1cTa04ZAmltMXcmjrzPRwwRh9sP-JYMToAmUyYFNcaVnT7ESOXWh0iHpysjoL-DZJKG5ONNqI955YmLQqdM78UJzGRA0GFeo6pioisWWw1-CJn7ez5Zqiq6aSLr7fjPkQh5g5jzLp_Tk',
                wareMd5: 'BeaPDMxMS00m5opm0Ue4Zg',
                name: 'Hasbro Монополия с банковскими картами Русскоязычная',
                description: '',
                price: {
                    value: '2386'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8maQOtCyou41W_dQPgAgpaCe9eYZfjqkhcibuAfPdfyvvzFAGBEl6ctnLAeyI8wRWb_m753nksxEGaJK6xf1M0ynHXnZ0nFfBTe_teGidjvYS-5aau4eHQDe8eeuLPg2wYkuyLr-ncRW6a87ds_pQzT2I-W0JQ6JVFf08uzMFqZJujvKchSBgSgrgm0XiKiK8TVT703fGoHsfyCefwQnhqCsW1nTk1soCzc1gxq4KRjO_YLr2BJKIGlmIWnM0AwsyAphyXvB8ztdNQCkLhErfau-3lftI1NLQammEAQ56lSjKTQlhlxtEpSPB2RU6L8ELCghKxdr1Qw0VUSJBRRAsIvNjqAn_ZuCQfBJ5VsDEGo0ndOo8yIJxRlhNCuxXYl9_-h2dD0011-Sw_Y_hxVJl4XCTvXI8ydyyybQNirZ7n2gvqCf0exJ6kEBJ7WCYBY49Mnmw5-_7pA3DCdEpEPGlopgL1zB1gNi5iJMpGoWwdrIma9I3A4HdDeX7SUXF7Kr7PzisMrYWF-opmyqe0-VvQFGqcj-ty1T1gSdsmjEBQptqcASfwTaZFUFiXt8CgmoVtFjr5JrbqpzECgyP3dexS7L7ea4bHRRxB9JClqaxV6TOBKmSOQbGLGhlyAQQiULUC9xHLkeTy1BeWvSq9d61LhLs-DxjtZFS1z7K3pw4vDHQsLrV3zVnUKASJKLCCoEG0assvpYXGXOSgocte_aKUkP52qEKQDiu793v1hEOaS67MQLIbUtHxpuZl4Eccur5NJZWwh0OPFNCaf6Z-X2X8wZtVi1K8CM_iQYMtJOVPKrizieLlRcdzfQmjc2O1pXvHp-VMwDnzPR2Hord6O4wxk5R-jCcQOwOa09aNjQeTIZ9?data=QVyKqSPyGQwNvdoowNEPjQsjSLrQfZc0SujJcZn19WcKRnp182cBxbp8WzUqnSR8ZEaFw8BDe0b1R2ez7aKXr2hIzqWfoCZxffIn_PGvvzV5f-gfPd2QdbnoyQ7IGUTh-8KyPmIT0BPYxeJ2e_fVK552bCdCU-sNZDndki78lWLc-Ysy9h7xxBGU9ZMZsxGa7G5usGg3NsADCru6_wPZNQ17Rltd_FJb8-wkxmUqbajecWO-SlhhVJDal25poOAI4MIaeKeZvl2Mh7wW1KAypFb4r1Ar-V1uhqwaBCHoZ88Qrx59R6CteJClL1mgA6uzDEUWRTnRQn9-FZH188XW7wml1WhQXLFwCXSnpBnngcXV9GlvpxCS8fiRrct_pAQnmBpfapEvU6tbgDBnXlR0tXVYp4gNeNON4ggzoQwD_e3kwXQIhPCfsL4YuRTCAaWEkdxZUTWp-li5djmw8PbEirhUYYYfanBd9iQ7oxxxCdirwwayC8jBXZih8MLRhsSkfl3WFq1ZL4LsFOwNEZ0qVU8-mCXnTg7WDbFFOLsEcgUbyT_NcE2bLw,,&b64e=1&sign=554dee6c1b795f8d1c8388ac3d878a1e&keyno=1',
                directUrl: 'https://planeta-detey.ru/monopoliya/hasbro/games_monopoliya_s_bankovskimi_kartami_obnovlennaya.html',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVPBGByS6AZwbQmG8vS5EAgjsipdr2bAD1hjzfLxeNBtkugLgjlJRPUUj4DquZxJfuP8SHHnQDwYc9oNkF2N8UerbULRpb9IrgA-b7ivMk47jJjugMWQj2aOQD8kC-9mmt9LRU-fBsqIiI8y4ju7bNa9M8lKeEm3_aa4clRiLVZo-dFeDmo-32A-7tdRgY2TXyK7hrXG0loXAqi3KwfDnSCcXFG5tfEezCdLFaVx94FqZnuNf8QTZRsFQHeazrK5bMMvoaJSU-MTsSTESZo4m7rQrZkSjK4j6NZG9OGNWa1p0AKKtO8sR74n7acwQlaPR8k8QO6K11tBEqH6I0tUixSQS-q8PF9R-SafMYBIBE-N6FNlCP1cCgs0eNScPVE2-hKi5gzlbYtF0JYMQNJUG4BfoaVtCI8r6Jsjyd9vwF4yObgdbXYmxKppLlNb3ImHsiIbpu3JbrI2FdRJD0wLtcrSepr46h877ixxBqCkMvnDNGYbd-fV2zC0LD9nQpTLAkSDWUWtYcRa6NEaIFuemgN0vFnXYnZ2PTd-Ft19s7wL0RoU7pYeyMMulsdQFLAzr1YflshuEvCq-xcquadpX2sIVS2OSJSyB5m7CcRwxcIjHa_5nkGJiA1uHME0C_NfGdZOO7TD5rMuiwnPpfi7sqi_bXlFiqUN-LYiBOCxYPdsyvT9OFzQh2eVCEcWt59Q82er4rLM9Z0JaA6LYrZ2rlKf7OjiOrhI0vu3CwYjRaU-eze2G738FPLDnS7pL9aR1PJvCe0OasSCqRTI2ri5pU9cxTyUigH_UI1VD2QO2w2G2uFABUFIZ9lIz-qr5wK_L_ru7Ve8khJXLyGYZ8REgtSMnF27GzF2fJYjkgDBj65g2?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2UbylSbAUItmWvO-8yD9vmA2sRVBnQ_MUmNywNfvo4TkkjfQYru1eCCBaesO8JVPUcPYMEz_yHgJsmuR7n_AvnjSqF8_VPZyrBSa-we5b0AuArag4Of1HVw,&b64e=1&sign=e9c793a2481b6d4e5383c5c1a3ce91cd&keyno=1',
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
                        count: 67,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 0,
                                percent: 0
                            },
                            {
                                value: 2,
                                count: 1,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 0,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 3,
                                percent: 4
                            },
                            {
                                value: 5,
                                count: 63,
                                percent: 94
                            }
                        ]
                    },
                    id: 57782,
                    name: 'Планета Детей',
                    domain: 'planeta-detey.ru',
                    registered: '2011-03-05',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва',
                    opinionUrl: 'https://market.yandex.ru/shop/57782/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                onStock: true,
                phone: {
                    number: '+7 (499) 166-58-74',
                    sanitized: '+74991665874',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8maQOtCyou41W_dQPgAgpaCe9eYZfjqkhcibuAfPdfyvvzFAGBEl6ctnLAeyI8wRWb_m753nksxEGaJK6xf1M0ynHXnZ0nFfBTe_teGidjvYS-5aau4eHQDe8eeuLPg2wYkuyLr-ncRW6a87ds_pQzT2I-W0JQ6JVFf08uzMFqZJujvKchSBgSgpJN-rPHFtRA0xNXzhhHu3rmNkUBPYM25CTEho_keUZ7GfVfJFP5mczdZPaw0uAR_O4S0H1K5f-clkdiUOzh1-uLGs2CVXT8559H1cvDQw81ALevjmTE0y1y0A_gkmVicIpdckqWmlHAgjakaAXZ5WE3eJWDYyt2u6ggkhRfl6pAsN67EqpNhaVAOWKv3afxbfQTxDGYCxP2sZEr614Z8xLSo5ff95xcA2G4h6PYdHcGrqxk34Gm_Ect7zV1Buoxn4ET2SHDoqg3ZP4uXjNNzZrqKKcUghCgkdud-RKjI6JqKHIlge_tctpYrJ7jDe-AVhRxT81GOlpY3lVpiTRBLu2n0oKkhP2FwRnkAQftKVkwCquBwfz8DU4vsJoC30zCIFZE_3T3eDk9mRb9gJ8C7ZnsnpiaR3Hwi9WwLV0VplHxNp2YE4yLgw_o6T-tuiklFSuOeQd7EwRFyb8HPL2NuIOn3UYPsIwOI_87_SZKYgi_n3zFqImBnzKN88RKxh6nGGkgDtk0soYXWTevWYp_8pNYJQzrKJ1A8qHvElih7tV09pzxNmuAntpBgclgDieciZ76WjQizT91ZXX9pDUZjksVxzLaIXk1vPo7-Kn5369lc965v6xHEWdvWsderXHP6binpdF7veXOJAnG1AznUru1QfktmpUaV6aTZaWOb1EPDa59MExXSDR?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8ve6Y-005VTkrhlFR_N1m9fnMb7On3Ih2lhebH_yUD62tPTRro2qv0eKAHCzBYFs3FhWeKG_Qjd2txavC87mPRFR5NPb3xV8gDMwWsH0CiuDZJafQjMZwwtrL_8dmi3umFHO56WHGTziwc3dvQ7MTrl-Vu_9MbNg1SQpaKFOEp5qEtpfAjkzke&b64e=1&sign=e19eb04f215d230ba575226e985448d2&keyno=1'
                },
                photo: {
                    width: 296,
                    height: 296,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/205818/market_XjOnYhZQrp1SyyLNJBaN-Q/orig'
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
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 24
                            },
                            brief: 'завтра • 1 пункт магазина',
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
                                daysFrom: 1,
                                daysTo: 2
                            },
                            brief: '1-2 дня'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/BeaPDMxMS00m5opm0Ue4Zg?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=jndpArvlW5xo8IvAC0lEuACo62GnQYNMcKTjbq7K2FbqpMZgEd7ksaDqiOu_tZk-zvsbwLy8X76WodLhy_Smp3StZP84w2g5omnYvtR4ixooZPzR6jzJoAOqrF8zVoAK&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 296,
                        height: 296,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/205818/market_XjOnYhZQrp1SyyLNJBaN-Q/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/205818/market_XjOnYhZQrp1SyyLNJBaN-Q/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZHLJp5h7PkqsPBRYoLfrj6WBaGKAbdCcXZ0B3B9JZfNfNpxlcKoPi_UJTWUB-L-WC_MbPw7w9QXc5lesTGBJ0zs9WNX45tsHifZguEkHDvjIYxujEO6WQBntrq1dKaqiXtupXEM9rvpaA0c1h484MVG7FHY1jJ77P-pEmH3y0sIngvqbhdN3o9sFlC3Xf31jAwRGHgePFPDyWWCpQEFLQf0Rfk5hQlww6ryLG7KHWki2nThY4RzDGilkItxbfQrXrFvnNfVIVthT5_894m94WktvvzrUXiKBqw',
                wareMd5: '1P29rnCdGQwbXv-PxyrPcg',
                name: 'Настольная игра Hasbro games Monopoly С банковскими картами (обновленная)',
                description: 'Настольная игра Hasbro games Monopoly С банковскими картами (обновленная',
                price: {
                    value: '2490'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8C6zLzPaA5JPj1ygLsfEu5xLmxrU7LVbL1zlWUtHUXTqoxVnxXeRka7A2cungUYahUas5PdO3lJrqSo6CXzbqAVgcc361Oo6fSBaavbZCvwTrTzbZoz38Mo7zOE6vy8dFgnXN_fXtM8p8-h0bC72xvWOPlb74VYv0gYv-a7yiyqUp3faWv2GL-eU_rThSutBC2RSP5-GX0rhP_m6kGtbv66pPrdsNdk5wdUV2u-DeWTnM6-0h99XBrKy5auNlD6nK68oAwgUwvvt1rF63hApJMxcnXeYOqflMEw9giVKS4maJz8JAkciffuvxyx-PhLFU7LpwtMOi5uZM9ePpXyR3g_dC0jz3L9nAQENrO1V3ryJ0iQEGq3ht2dTaN-pCVeXHHWGdYfJ4oL62DoeyY6rg-y3e6vNq4yOKgx2aQDUqnoVU4maLqxQctM-4JRafXDUa-CsryiP79iwFQNcRbJW1WSaOc-EWk9-L2Vpx6kfGdKcT9_D4pByJ7bR6W023E_q7BYdeGyfkUURNcjfDRI9D0ggTeZId_DylC4s_4YIOENYD5bAe-pHSj7DngKlyZ9QEY0ENE96zudvkhJmsY9z0IWkn6lQgh2WhGB6gJmMhO1fto3ZGHBPUMiCGI_xfgi9qfmGV1qizmro5VsZT4setvzjRmew-RT9r1PEED1OFBsRprEnK44m3Rqb2ob7is4DMppdD3UKgQE3vVhm7agF5ghZAVT6lkzcMCVnwQT5sNYgrW8wTCmndii2gzJpoRmvpLOWgJSog1uSgujtHL6hYowG1RA6kvoUVrrWr7IG1xRAhNpPbUd9CTMZyOxC1iV4JMmcOLRqHius9bIduPwYdshVDIIiFiOft6yYTcDSX2aA,?data=QVyKqSPyGQwwaFPWqjjgNrcRl2JdfzyMwZvmFd1zWQ8i_qPoTgd0OJjYrzuiK9uFnza04tdPiMs3ZY21spMWDc1J1MhT77ru_vLoCPpMiJhgW8vwUlJcNE4JQ1AF3LO7HM4Yf2_j2wlDcFk1A2MKTEnARK6jvHx_g1Eut9kFD8VqxZM3DwgeIwCALJ3mnQn8AKz1TZCFmRgxBE9LJ1_NNOpXLqsLWumw&b64e=1&sign=16655d53958f7c77bf7687673a1ee4ed&keyno=1',
                directUrl: 'http://idcshop.ru/products/b6677121',
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
                        value: 2,
                        count: 2,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 0,
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
                                count: 0,
                                percent: 0
                            },
                            {
                                value: 5,
                                count: 2,
                                percent: 100
                            }
                        ]
                    },
                    id: 427758,
                    name: 'IDC:Shop',
                    domain: 'IDCShop',
                    registered: '2017-07-18',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, 2-й Южнопортовый проезд, дом 26, строение А, 115088',
                    opinionUrl: 'https://market.yandex.ru/shop/427758/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                onStock: true,
                phone: {
                    number: '+7 (499) 350-98-01',
                    sanitized: '+74993509801',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8C6zLzPaA5JPj1ygLsfEu5xLmxrU7LVbL1zlWUtHUXTqoxVnxXeRka7A2cungUYahUas5PdO3lJrqSo6CXzbqAVgcc361Oo6fSBaavbZCvwTrTzbZoz38Mo7zOE6vy8dFgnXN_fXtM8p8-h0bC72xvWOPlb74VYv0gYv-a7yiyqUztt4Cuv2UCDJIh3iO1PUBJ43SLm9Qcff7-Kf-Yf2fJJ8umq3sRKYa-c4ieCQ8ARZdhqUsBHI4LiQfSyVibYMV06ElRxzzgtecvqDMPDBDuNVeKd8HebPU8RJRhDqQXEkgWiMy6fqOyNbu1gP7VNKiqXh9NMagVL4fznuXICe1ciu5OFiaUKX3KXr18lKsH2shWH2cpmFtwOJiU73PIYJ7o2c1krhFW9lnZxWbYSmp9YZ6nxuoCGi0QFqBdQSD9uFE0T7FjlrtXSPvvaiS-zbTd0xBnsYHy-r6DAwN-_dEh5gvMyzGvKvcsMI3LQK5i4SyMITXSPNgDSSuLwqx6Vqx_JOR6xrbHly59CO9vvR0JYIAbOgYwlLRpaTbDNghUmC68rR2hEUUgkFvZOFg-9iDupCXLFLX-xQHRXtfw9W4Bfye1LpSvTBjOtLkjCr2lWYC555CGVARcNkKJncqHdS1HhHlZ768LElH1jHQyZGN7tvPGz99YMjTWg6wGqvPN65nAg0wn7smSGtbA0WeTdQsuw-WAA9y3c605AS3IduD6WzrFN3Ctlv3PbcqDCPeJHjQBEpdR7ZhqdWKjdRbBgPo7WKDXoFFoOG5r77LVpp0gcw4JKU78Uspa0JtvCfm65LEc50tEbJPyGMjZ2iamQAZHTELPP3Dt66XuSpR_sSQlndNbomh6NpfR6-Lt1vNyOQ,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-VW-bN2ZCPdvcFrMqWvgt3oAwA_S_BBzUZ7D_4Q_KXqTKdgkPKtFXpvOKMbMDqrBwfS5-DaHVuQLRCT8JgblwC9cDMrOh8Vz0shR4otTS5LN7CqBPjehRMy1ZA0nBs5lCLpe8drs_IRjT1ptL7zFTaqV_h__hO23dms-3WAfjc3ey2q_p55l9P&b64e=1&sign=e3cbb4e692fb96b4a77c2b5d2dd5649f&keyno=1'
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/228527/market_eUM8gQ2J6pQlDdZ0Qvq8sA/orig'
                },
                delivery: {
                    price: {
                        value: '250'
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
                    brief: 'в Москву — 250 руб.',
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
                                    value: '250'
                                },
                                daysFrom: 2,
                                daysTo: 2
                            },
                            brief: '2&nbsp;дня'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/1P29rnCdGQwbXv-PxyrPcg?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=eYlSzy-peuMX7YKZBU61hKfD1-xDeTmTBsbbAZbjzFOKc_4-r45UNBdMy1PdYe_LQ6CR4Dbwf3zhTwvWOKhPOLxi94_atBaV99xilNlAs1gZIECEmaMPdatGywF0qDCM&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/228527/market_eUM8gQ2J6pQlDdZ0Qvq8sA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/228527/market_eUM8gQ2J6pQlDdZ0Qvq8sA/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEG7IzynCyWDC_semcfO2F8FzSzFtWDWSlTLEzq6TScO4NN0a4a-G-LKJxf7vk32G4TM6Ri-LOpkLlOhw39HRi9VwAq_YwTLuPpdBRwgxfB9qmzKFFAaZFe2okpdvtQUANueKOnuwFGxWdiW7M0OAMwN-Iw6bsmK_o8rY_ZMtQcVKvr9zAe4obAJFa-0oMOZHjdOvRZA9SImzt_CTwsrjOE9sqISdZXBFGy6BR__xuL3tjB9bYaxtJRJBF9wNyPC3SPnGBxoQGCFYzEIIcD3gnUiAG_QDrTauY',
                wareMd5: 'xwddqN8iiPWRCMoT51EGhQ',
                name: 'Настольная игра Монополия с банковскими картами',
                description: 'Монополия – это логическая игра, любимая детьми и их родителями. Возможность потренироваться в создании собственного бизнеса, работа с банковскими карточками, разорение конкурентов – здесь возможно все! Да, вы не ошиблись, эта версия Монополии отличается от своих предшественников, и в ней есть устройство «Банк без границ», работающее по принципу банкомата. Это намного более удобно, чем играть с большим количеством монет разного номинала. Попробуйте! Вам понравится!',
                price: {
                    value: '2194'
                },
                promocode: true,
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mSjN784BcAcPLM_slZg_vXhOxkJSnXfdMknBWMqBnECrTybTs6Vl8rTuPtJ6yNf3VMITS2nrb0ir-hhgM-6rdVU-2oDULiwYwBY6NYxc3fItspxkoLccRG7UFMHc5QOdJ7VqpBt5Gst4GyPgsqJ1DoPmeLrX7Nx0twarb5V5UAFBYwAfuMpvo_NBQt0bN6efCAF7K03P71aMMsomeIiJ-vDTB8c-_HjjuKT8pyWCjRkQDBqxYI5wAksB1i152vQz84tLeN7vvAXiO7rA9lHwRdRI_2uKEtS8_2Sa_nQSx6tSHtVHJSiQWnfy5wz-yZdtBNSRFBi7A9YBveUNnhKK5LQuA39ynz-2IrL0djB4lEMKuUqs-gEv9SEa4xSkYe-k2QguPSvZU2k5lVnBFFEvh4WS7oOrE6fNGMrQHroP_2P_atp_FipP4oklThfI9lM2qW2PG-hNp039jL2I6hzGOu3HvYFXmf3skYW8KTVTMntNU2xgvuuffKB4c2WBRdRpxYqOttJ4sqy9VmdJ8GLjfGuG5FAG0XK-rhHXrN24MWatP5m-z14EhwHCRzGswQEDbX8xbqzB8_CnGrQkouoX4--O-cmD_SQcg-Mv9Er36yIjqFKdnXPruOs9YTi4rl7hmhPto3qAyeo0LF24m3Msl5R47M4ago7RTdbNxYeOioxNVkk8ooJ3_UJIHr_Y-LjwV8Gr0t6kVX9UfribV_MLnUhYRmlg6tGhiugYIuvuaprIHFC0MfoRKDziJXShRVgX2N8cTyRK5l6tvvs1Ea_0pTl4_vidYJL158_db0opsT0vLeiyfUVFOokdcYlRTzNFCON_Lu0XDrlmxh1aLHyncg20OSbNWUpfguy9UVczoGWmHdqwFsICr74,?data=QVyKqSPyGQwwaFPWqjjgNi-mXaTHyrM52t_arBPnUZxN-zwz-lEEfwwU7CeiTH92jwassm_aW1Sn89gDmu2_1qU2AgnTRCZFRZuYP7IGF_pX6IR-ZoK9x5eFzCQcp2OwoDdJkp98fBwMrtNWFpDPg9CzWamzEA6Af6TTx00xpKdRyrAjZSW_FTZQa4_vLKKkd2IHsKGUl9UamQ7Qlhk03yjGrKLkBzNgWkoVSpo5D1L2dBCB_BB7coFK8xU-ceHPHXGot3AH4QQT8a9j-zl0g3ZmaH1phDxkNoC880OEo_pSd_5vNyhsitqKHrle-_5m5WpfPv2OMYmaYYcTPKF9305GBgwfocV52ackWu9tnR3P9NTuj-gHLXt_Ey1mdT-wztCOPDtglSugEZ2rlCDXSUyVejULB5c37QJ5Y0iN_Sbm1ebPhqfa7yJayENc4pIHom17gfElA1rK77xjM7I-FP6ZD80Z3_WxCKz22YWqe2khYlEmLXMdN4B1CWrmfsf2_8ctiYHmLtrl7zbG9kegKjoVtql3Dy-Bbxo9XNr_c8AjEba2rIbSxvco1O5rKcU67qJODUo0R5GmYA0hHXdtXopllKln9nnUeV6S7l3Yk54,&b64e=1&sign=0b9da98eb8b4efb1de47c28c8f2f6a1f&keyno=1',
                directUrl: 'http://www.abtoys.ru/product/112406_Nastolnaya_igra_Monopoliya_s_bankovskimi_kartami_obnovlennaya?utm_source=market.yandex.ru&utm_term=112406',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRufkc5G38e6HWUuVTB10SWYvWMJOUSvXn0GlPw4W76JXdIh5Eiu-pTW0rsln_igsg_1ACESG7RtS-gXMZ9g-3yCtXkyvAp8nOzU6JuHZqmn5qCEVlQATFDsuVclr9XJPACwqzW7agHIoRcBTDph6Bo4OWD-ELp7DEenSu-JiCtvWu9HD1xm6h380r5ZMY2L8NwFEqelRScgmjecTKuRfmtnu4G4tF39jHCWdj-Ws0BQuJRwQHRCm7oWJguWBHAJuXNF55M2lu7lj1w2TuzH6khUGmUbO78JTQ_bwYC7tbAuacT6aPsrrt-CCPpe2kpSFl38YXwRxB5Kol6ZTINoKII19xdSwE41KRT_Kze6hGOSUOe8oIdzrnol_d6rNR2DauZ2ESl4tsH5USHlBtOkddiQU5uj0vF1baOnWOEo7LuqLAorm-zR9eBQDsSakHuZsMzNF3YVeveEEuw1xKqqX_os0PEuqYatkhTjDoLgmhpVBx96ARRZEBe5vxjNQinorgyz5aZUnYBzyE0D9HqNM32vyUKZrG7nPZfRbtTisJ5snE4XvzqbqC7HRRMC69bqGhVrsLmaHOj512MLCXs1_ihjkhkuut2j4RAubB9mqXqaUweXxWL48rrUpe86dKgbvo66In2jsd6hj80N2UA86HVMBIP-0vEg11BS1ERERZDiWvTiuBUvIAS9wrKxV0zccgj0lY0nC_8-5W9zrbP8FI0bQAHek1lX6nL22WPeKeJgJkkNyY1wp7JMpPYFlqYKodwPjTWrc6Igrj7U0wkhR-xeMDAh4_kN5rvOBDR9aff2P4-nqXf5fVeCN82UFUderyi7P2tJRIWbWwNuOzUNahcvEh9-O3zK6K9N3JU9TOvORUzSkRIcUJav?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2R_nZUIr9DPdgJJ1H-OEAL_gJMRbq5N0hDYR0cqoMks0u_prwobL5fFLmmJ7jBJfGKhBqLIuflPeebT1pxQGfHnczlny3O5LMWzjLglpRBK-uKRvu2ufBss,&b64e=1&sign=b0ce87dd6f1d59e0d2e888261dbf0096&keyno=1',
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
                        count: 167,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 38,
                                percent: 23
                            },
                            {
                                value: 2,
                                count: 5,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 10,
                                percent: 6
                            },
                            {
                                value: 4,
                                count: 6,
                                percent: 4
                            },
                            {
                                value: 5,
                                count: 108,
                                percent: 65
                            }
                        ]
                    },
                    id: 253502,
                    name: 'ABtoys.ru Магазин детских игрушек',
                    domain: 'abtoys.ru',
                    registered: '2014-10-03',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Щелковское ш, дом 100, корпус 107, 105523',
                    opinionUrl: 'https://market.yandex.ru/shop/253502/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                phone: {
                    number: '8 495 988-29-15',
                    sanitized: '84959882915',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mSjN784BcAcPLM_slZg_vXhOxkJSnXfdMknBWMqBnECrTybTs6Vl8rTuPtJ6yNf3VMITS2nrb0ir-hhgM-6rdVU-2oDULiwYwBY6NYxc3fItspxkoLccRG7UFMHc5QOdJ7VqpBt5Gst4GyPgsqJ1DoPmeLrX7Nx0twarb5V5UAFBYwAfuMpvo_PhtZKHabUotGGqu3LGNy0DZ-UJEQOc-1IiejdHe3MPTSTmpN6eAlhRKcoVBclgxavghEd3zqfu37J610NLGs5uxB7Ux9d-7akFXvGnGlU8lmtU_vr2GjTjKdMUPH3mRLcegMzVTfhf6vUvXz8eYDAfLKOPWJ0fsW3jBPD-fKixYoOEJxQdRlT15osdxRC0EqfOpXAFb3Pli0W8JrHqtovu3LQKrpaTF1-vNaP5A_CIgMd0mLu1xgY25TSAUxUhvCjucZY9WceVh0BEahfjX-DzUIdzzVhyFC6P8agTVFJkvzNHN5eShQWeC5-0jJ0eaB0uGlJYXpH3cwtRQXTzO3bdQK3xIJ4-TBS_g6R0HxxhelYRLtnZLIVrjxCY1jj43h_SP4tHND9XLa0jP4jF99ZuLdv-N7vbrQC-5L1NRHBKauOY7RxCQ7-oThgnzkGLj14IyKNq9s-ooivyl4cSE0YztZ9w62Wng5QkcwEAOtBCVm4pFM5HZWqfLhzH05e6nY4SHoNrQcKvmZ1xLL60hDe7kHqVZWQvicS4SymmIPOIfmBCfstEX3-guPyx3-O_K1QWTFpESLeAWDS5LmrBUlB-UdsT83CyWKjs4DyDfd9Ezo-1aQe1KiDS5BGbqZdknShQvaKgQPBQpyOqgUn4yjLMlHLZIAcDqxJ8YwgByMH-z9dqOq32fFPq4AdkspYgZ1g,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8-HoOEA6DgyvI4tx0cM2hcVT2YubJOBb82dxSP87baUUoMnO9S5X4mpDPDyEEGTEBHB76JvlwZJXRxFT7pmn3kU08cbWOpLH-mPPNWh9kdltVLnKXHWlkHuc1AOvJt8fx-UjrbqeVYRUYTkLC63Ni0Hzs64NEswl20zk2sspS593hP_k885Km2&b64e=1&sign=a86934441fb1f5b4ffbdc472eecefcf0&keyno=1'
                },
                photo: {
                    width: 700,
                    height: 700,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_a79gMhhi9K8y829MqRq1mQ/orig'
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
                                    value: '170'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 22 пункта магазина',
                            outletCount: 22
                        },
                        {
                            service: {
                                id: 119
                            },
                            conditions: {
                                price: {
                                    value: '170'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 1 пункт',
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
                                    value: '350'
                                },
                                daysFrom: 4,
                                daysTo: 4
                            },
                            brief: '4&nbsp;дня'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/xwddqN8iiPWRCMoT51EGhQ?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=oRqkkl9vMU4usavUBaLCYrFOEq0JC0f8sVdNwSavoxMFhbGcSmdiSgf2H_vUiJ5_iLHTQsnHXG0CPxp0RrdqNXBqK8ZfN1cW5F6ivRA7GI0RiwtlBNI3rJf2nPURDj-Vubo70gPP-X0%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabcR4TJfVe7AXprIo7-tdc2WyJe5q4eB-zdvYVBTW5u1gqgvI2279JTdjrE8yc0p1KV2c_xTWgAaVFPp41MJBNuSlbq1TXQ2PhyR97SZ_6I5z7hgHRBkbMIMhOBvHSLHCeMzo1OjUgxfDHp8gLSHpT-jjx5uyK6yk2tfPYTumK69_HzWV6HO0q2LxgSmthlJeGXyvxWpIzU9tIletZQVFbWn7AroncMfmkaxjbLPmh-5kQ7D3DyfudFAucD_OvWGZuycs1nPNXXbu0tXv1WbdAOhuCF_rPLRzCaJnnV3TljGBef5CWiOWBARfHYw7EgwNtsndTUkW6Ncy7AI0R-INRW3sSgwhQHzchOizUZ2YFO--akvEnI9_YCP3ngOQV19gg4uTNal4Y5RXEeU6eXI8cYKY8iGo6Md4eIaJAJ5KQkJVZ4cUAVR6nrFyFO5nuF_GeapbY8oUhadiglSP94WBk8SLkF6XX-gVE52Wm658BTjTwTbCszkDFX003lPPDZqqPCEieL0KjGjLLt_En49GBbPFf6FqENLaerx-S7_tWL3vvBGD48fHEEbiK_XMCu34vVpoAAPMYKdjMEnTzlKMDLP1qdtbqq4kFQIKnVZgxh4aXSxe7xn7ldxFzxS6TVW1QJ78-u3N0zJnJqqNdK7KN-ZyysEOjirxFCTbEjChUgpDTGnMlYSRjctTHE9uw2wBO9bJIprAI6oECP-5B3KW5CiAzew8sxVlCe2BAaqv_LuOc3e4XzsvD_juGTRV9u33fGrsh7O8ig11v1_nmb1hLyrHCxw0Zx5X4xQhi7b1ZSu1FidAYe-q1tZT__CJF_8nlEhPZF-PUw2SOzNwIwAYkXdivYQy0aIeEEqI9t3HgIyoW7S2oUBsp75vGZE0tAkPghUYX34CC0qs,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXeYg_5DdXLbaJIKcuNVrJYC1Kxd12GmbbxO4Vc3LUk_3uB9Fmo2B0Gld8PhRZsKb8vRgLsQhk3Q3uOk79Sr4-g6yeksfjwfuvLoZ4rTZoHH3Z8XjdXu9I592ERHU_pMk6fl-Ofi88ndyBheGy37DTAPyl1Cr0-ndrTKaQECK5ZtXGujHxZpUO8dFap17LX_KfdFJjyYvdV6Xbk72Cy-YDZosdaNR9OX_3lvEWPSxHTC3xtehfSEYBmCPgdR7xbQ6ZUKJOJn_CzuOTmEMjOlm-dHwOsWsacD1PGeI3jv2pxoR2UjXY4tpchg,,&b64e=1&sign=c4f4f0e36fa066d6e28cd070694cf34a&keyno=1',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: true
                },
                photos: [
                    {
                        width: 700,
                        height: 700,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_a79gMhhi9K8y829MqRq1mQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_a79gMhhi9K8y829MqRq1mQ/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEUf5UgxncBZj0M0XkH_KPvWvCtdtw2vXDXfNxSpvwsXuKgYSZJzfLqb66e_rDyX4fAEtW8FBwBFYmElzVvbxPHUXWjg5ix31th65FBWe_pvyRPJs4IBCAeW2lTkTvKgrBeysQsScSCTZLcDaG67LtT5pIrUONIyasd1BVKAvQRc6Jc1R_khUfaSKeGEOXVGoZlS_8k41o-Fv5puccyxmyYOwzed73feBKTnfr1lC7hsmEw6qLxsAooB9GvIKckYjc906GNDHPNQc4GweTgK91Yowv-vgQCdUc',
                wareMd5: 'UHOAL42bjOAAOGiCT6HWIg',
                name: 'Настольная игра Монополия с банковскими картами (обновленная)',
                description: 'Теперь все карточки читаются банковским устройством ( в том числе и карточки с собственностью), что делает игру более динамичной и увлекательной. Просто приложите карточку к банковскому устройству и деньги уже зачислены Вам на счет!',
                price: {
                    value: '2769'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mSjN784BcAcPfMarxRwmZGrIaOp-yxQ1Cz4rKH6ks9omn3YWVA-iqIQnfY5na506J8ohtDzUtxyMAIZijL2yJXZDNJBMlWv30o5x19uE0qvwhCIEw1gENHxGsjIG7LtlXLgQ4jZUlEEOgrpeY6VFle7nSLwuBdHKzmKbeM-PIMtIfCxmjeOpFKpt5FNmZFXHiNStZ-Vknumoe61dP8EdjivgYpvvxiHGu6CKklbOSNZpYB6rBXozB5bXWqVzmQg5YcTdHqeQVuYvyPmCA-ftySPlnMUbyk-jUloOzgiLgoSayWkhEJJjgA-5muYhWxmK8_fNcbgG0HtHHCYVshVLiUVCz_SusoJRfT49Nn9dW3RUfoq4EVvBEbiGtnfdPmkxqkejbQd09HP-WW9hBCcMFfK2RNILgiWGg0yNgS65mCm1VvVJI2_3FfueT624tPNd9Jjxrtir3Buts5vvXBv5Dd376rUCSuhyptkDCcxvX5KNMQxFFXbN18PEMmuQXI1dCIUov_vgocKtfucYOH72Rl7Dy7kCEDZ7VQ_8rqMcLT8dZMJHjqLi9369aX0ncCQEA_czNbO3HDpNf_2y-8pBvb294mhXowvrl11d6TP5BR5Txc-OTFN_i8Ya4L5sqSOovoVtuARIM71S9buTIcOgbcvW6hy3aKk5VrJXRbS0d6ylx7tFEYlaovKLWOhlH52EPt2cs0HEO6mvSgAPeiCQX8wG0HStVAqsOwnV6-yjf3DIGlBg5ooNNQ95cainwdnxdR5rbSKyd2UHZokCAFg9XqIy8zuxlap9XjWK7xActJx0Aa83o_GSszm6D1DRArNDtIZhLKtwUzPZAnwysiWsy1FzonDe4fmn-2jNNKKV-FxcG9jgZ6b6_Mw,?data=QVyKqSPyGQwNvdoowNEPjbqs-5i34UpQ0Fq18l58HhE6uKiugTcGGlpdoqI4OsT-73jXZifoJR_Fc1ok_AC0fxDv5rEq5HOnkPu_3DnyRe6ykDHKQ_94H5Clsdrx_CtpODqbFNO66o_e70e_BOhvA5TVU-ZsnmaWRf2n5HUPN5DSUWYVqapMuinP75UxCPHhNn3j9U1QxHvXwEJr0Y3lKbwW_3aRNvuvSg9zmhs7PRpHkrr36UXemMwdvceElDnWxM7q9Xkjv3EftIrmtZ8O-Z-2Sz2eEjiEyUc7SsguVpSOC1mOt8QZIQjGcj3gXTk1Lyfh3gHA2PwCZI9oaeWGBm2KNsTNm75JVLU3Yo1Nk5moyKXeIJXm8g,,&b64e=1&sign=dc7f3b0051d2e218f9f05e4c369d7634&keyno=1',
                directUrl: 'https://3d-toy.ru/catalog/hasbro-toys/the-game-monopoly/nastolnaya-igra-monopoliya-s-bankovskimi-kartami-obnovlennaya-/?r1=yandext&r2=',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRufkc5G38e6HYRqyfMORvr9_uEYjyL8nef_seIxy9nwWmixI4YeFISdrX2cxA7McCeDNqp-ZEBdIzlQ-fVwIwIgAtW9ET5EYSRImbNg4i7gBWNVi2j4qsKUp7TRAD-WEtzjRk_3oKUWNlJ9SCM-H8-gs9Wr-YSVaGwFH9xT8ih6w49PRvfGEIOq6CmYHihMfeJH7UBV49IZvvMUGXkpEAmvqjskpZ1kN7-rjEYZed8Bi262iRAoRMP2hPV7QwZJCn5ZU_siLry9SBjUNT8LuCJJmQ80tO57ZAitgf03q-2EoXOqa_20akT4UnFI6FvIXFXGWR-tO7xycp1FIGZH6IQn_v9GO9vNtk9RLcqYUujI9a0Isrz2JPEg2hZCM9zapQEDffDEvNIgf5D8hxrSsvoM6--MbWj3pS5Tv5vLFQEiIZHWtl0J_zebuAZqPNuPXIZciBsqZE5Mau2GjzWsdtlsxAlL2UttmOEW6LNi8i-PmUYBuIcYLPMFtgu87fepOEsoYPfZ3TXubJpyeppTJl5i0Pn_yDg1xbsyxEIrTJ0qVGiDrR2EfYzZ3BMldO4URkn2BoUtCR7M_zQW-XJRWbiP9RRzowveknJdKDLq_aBmuYL_Pq0b-zNGcyiu_klfjKUydwY-KHPZlGN_icgDz1EKPpeR09fzU7FGw_j4vqrIkLFA87BaLh4yRQbTEvSwD4fDF2A-SnfBVjEjnRzxOFjBwKgw7pLvoYyPdDiDceZQp7VVN0HGYl_wFnIaoDedmUP2iQzU6RIvZnRzE9spdtTRM-bBRNK3dy5Ej7V98ZHeRqMEuLNsnsUTEsCy3PvbECZuKuhCbK2QJ26VYNDw3pEoQOabmgvzd8ozCCq5v5we2kkmMM5ogoieZ-I_cBUvNVA,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a8_owzbFICmrq7NBTDnb_EFeFLirexR6MvVKM0X-7LiBf47VOszu7TCqhnxSd1_BL1fe2v5uotLDeSLUYbMmifAVgvqaMlDqB9iMhlEiFoTzGPuzc7QnSQ,&b64e=1&sign=37a06991ef62ae11e94165b2b6fd090b&keyno=1',
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
                        count: 378,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 2,
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
                                count: 20,
                                percent: 5
                            },
                            {
                                value: 5,
                                count: 356,
                                percent: 94
                            }
                        ]
                    },
                    id: 297348,
                    name: '3D TOY',
                    domain: '3d-toy.ru',
                    registered: '2015-06-16',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Реутов, Головашкина, дом 10, 143968',
                    opinionUrl: 'https://market.yandex.ru/shop/297348/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                onStock: true,
                phone: {
                    number: '8 800 100-24-92',
                    sanitized: '88001002492',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mSjN784BcAcPfMarxRwmZGrIaOp-yxQ1Cz4rKH6ks9omn3YWVA-iqIQnfY5na506J8ohtDzUtxyMAIZijL2yJXZDNJBMlWv30o5x19uE0qvwhCIEw1gENHxGsjIG7LtlXLgQ4jZUlEEOgrpeY6VFle7nSLwuBdHKzmKbeM-PIMtIfCxmjeOpFKrijImI0iCu7pHX5xup3SVdYvARPb5Xqo5_WUxen65v1zBupcUpSv4GwKxkK2MdnbEQfYkYsOjx0o22Qn0p0GszO6Ew__KFxsRS3qFR8PMjHMSyFHJ8HE6IFBHwyzHRzwHlBKIx5NDQ7-T3YUz4_8ZPChi4ylTzbtNbOD8mV6se6bOvNmeiGuYmcNAFRXyDXykSGG76e3QKja2cNSGwQcpmg5wG2oL36DxYemX1IkfrJlZ_rVKVE45bQXlTPq783Tu-HBp_KAVjqmMoP0-o0S21R0lGZyxz9lCg37_eq6W-ICNA26qz4t5qqbPLIlLPVJIqTQ4cpjm_Utisl6uqqy5bMUWfZbAaSjVGrSNgf3qDZSxPI0y7hm8SfbLekJwpkXS6kG6oLsT_LvGlbwEltsg5MXXXgYx0L8f7U4CjElohqRBO74FVAGs77AT69Jt90I_JByY7I1WCAKPSgjoDiW-uIL0MkG3861uqf0sx6saRlmkfkmw1n9pIOd82dD8-_xDF_qRaXsVVWftAzGBfvoGPTeU5173rQ5Fe0Zn1dwIaC-2xWim6VocZRhj7uhE7VPj04giqCDX8dTA5TMRZnuw3xuGokmE9lVonsYKqklHbMrFzDZB3-ZiSNuq_m1vbWiNJiPv7PMXow5L-IOumInapI360sCncMt0MjkPqRbhfsdPPiCHvu7o7mgW8BQUzjyw,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8YzhCyUlFOfPVdeZRyHk-G5n9dgUu3BhQEMVznYvLb9l0txdlfptiSlZ8lG6Gj8Kkur6et8_8HM2nsCD3U_dJoaOo2J5js5Q61Ar2LHK30T4J5AWa49OxmHcU-eElOCB_9zDuscbWI8IlYcjSPPIqhqjXMOQo9AjMcAamNs17uRUOsVLohkqj0&b64e=1&sign=3859e9c47762e6ef6000de923cb4a2c6&keyno=1'
                },
                photo: {
                    width: 970,
                    height: 970,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/363663/market_MW9NoOIlNl6pXFbvOsz3hg/orig'
                },
                delivery: {
                    price: {
                        value: '295'
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
                    brief: 'в Москву — 295 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 100,
                                name: 'Другая'
                            },
                            conditions: {
                                price: {
                                    value: '180'
                                },
                                daysFrom: 1,
                                daysTo: 3,
                                orderBefore: 24
                            },
                            brief: '1-3 дня • 4 пункта',
                            outletCount: 4
                        },
                        {
                            service: {
                                id: 100,
                                name: 'Другая'
                            },
                            conditions: {
                                price: {
                                    value: '195'
                                },
                                daysFrom: 1,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: '1-2 дня • 1 пункт',
                            outletCount: 1
                        },
                        {
                            service: {
                                id: 100,
                                name: 'Другая'
                            },
                            conditions: {
                                price: {
                                    value: '195'
                                },
                                daysFrom: 1,
                                daysTo: 3,
                                orderBefore: 24
                            },
                            brief: '1-3 дня • 150 пунктов',
                            outletCount: 150
                        },
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '195'
                                },
                                daysFrom: 1,
                                daysTo: 3,
                                orderBefore: 24
                            },
                            brief: '1-3 дня • 2 пункта магазина',
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
                                    value: '295'
                                },
                                daysFrom: 1,
                                daysTo: 2
                            },
                            brief: '1-2 дня'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/UHOAL42bjOAAOGiCT6HWIg?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=1HCGr2PjIsUsAFYCLoMlt4JrusyyXN1e50Yznr3HEYSlMzJV2vDv03cZuNkNbm66Y0-6h-zCKZHqG4pPji7laGXXLgAGlJN0joJe9-hXsb6Zdpi9bh18ujKXtYnapA2y&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 970,
                        height: 970,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/363663/market_MW9NoOIlNl6pXFbvOsz3hg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/363663/market_MW9NoOIlNl6pXFbvOsz3hg/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZE34da0q7bFDQl-CNoXY1vYySFY-_deoVZv5kgYZ4UJnH8OP8h86jMbF6x_DOgmPWELMbfiCAcc0vEs3y5nSsM1SI7CNkm-2uxSMQneUfat94q3hH1hJLG-Fq_bHKEBu1uiYZVXzdnTapCQhIXLq71eW9IV3D_r2JefEUHm6OpbaWD61iHaPUcRVUOBYayuzBboxPvHBWiYFCitwhGQ9mS56gB1cBL50FjtU7B3L9Bf8o7j9gz0PPv7ty-8Nf8mWMAyolqzSAcAGX0XodjMRYq89ypj_yVFiFY',
                wareMd5: 'EJzs3luzi8WwYcvBQt0SLw',
                name: 'Настольная игра Hasbro Монополия Банк без границ',
                description: 'Тут и говорить ничего не надо, ибо название говорит само за себя: самая известная, поистине легендарная и любимая многими экономическая настольная игра с банковскими картами',
                price: {
                    value: '2990'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZ4H9_aAm-SdXUBmN0WL0hnGUTbtOMo7t-jS0Su9XO2QuvjkDGF8rWeokatwcggpmDLthLJNlKtImPfLO_jVKPo4SRvnAKZk4BwLpoSXcNIzqKg3s96GY6LFwPpO22YRD5kjkb7CdOy14js_QlqzMQHWLKz6ydzu8qZnC6x_SU-gipk8LuZ0kANmKPN6tcOUNaf_SxSVXwpfLMvDwvguQ7w5_XeOBWYRZY_RFa-vNImfBaEf7JtSzc6aSV3yUWsg22Ezgmwo4qekkiQgEkGEEtk4DuU7Ve4I2TXIbgmDrW9fgf5s2s1F2ykb5MYrbU5joRtzyoaxFkgSxsSiNzn4mVrlN3agPaj4gmKu0OF_gzXP4gpLzQmJMvMz6BWtCEY_gYAFMUPDcqveVb6EHfLOQsyJwbhqI3hPRfVZ1dqm4H-U8mgxxqtpt9WdtLKF-K0vX2HpI2-WFbDjOYykSEAX2TwGb6HTZjYcW1q5g4sqWWUzSC8cs1fMhqJydfQvgNSy6Y2QGyv8PYgk6AJjcu1KT-5-u97ODXO6WOL43y4fQcxd4QARoDdNdViYMc7rsufpc0XYYsB0o7hBulM1gYnB89XCzlEBt6InHCxCeBMnFqI6kdIRUpyiaDpfZpdt6b5sJ2e1vwYkyPDswV86ioLSQG_0vzMe5WtD5RgsQ0c4h9LKuN4CBMGry0QtDYkRkQYSAe9FoKSJMdz8IwnJVhcmXGok5-EOr4_epZvFWXrw5ZqyrePZlt-uqSTyMUEgzmO13XXBMQaMo7LuJpK3rxN1tvYyLoNplFjW5Ua1_zNIjLprhQOa_J_4lfRU5bYw0A6ePGxUGGCr_8by9EYJxVEomwT7xyElpAiKzas_FfZU9pEwXPolTFTu6_Q,?data=QVyKqSPyGQwwaFPWqjjgNq31P5uLbHikJZEo5WiNoG3ULxX9IBcGI8oeC4DjpvvwVbXokDSo_tTCsRNN5rUqGcd4gBqaY-szV_LNXMmp7EN673N-nrDcpNlFCNOxPnFhFKns1cBqsDt9r21XCcn-2I0iR9rS5vP91sSaCHsbHlXZy8kHgbmWgIUZVD4p5GThz0lkywYjifAELKfSJM3dznOqfCA6E3wm2MkE_SbjoKLtgaVhkHjjEm--E4agV_HrtNTI4Xc6XrzCUzs7wdQBEgGx-LlESFVu18wshMcLE5MnvfV2njzBVnBCXJJ5cgtj-ExXtDQ2bSsMChoAwzFhd-3uXEqEjz8giHo_lT-DBShXWhhwy-hrC9ymhVzeQo6sFuEiYuKqMsuhc2X-46xHeUe4ZlQ5n9BNAVHz1lhj8BjmhdlcpsePN91egPZOEewtaqiYEJ_YI6RNRoQz_MenZfbvJHT6iS2C&b64e=1&sign=7f1fcbbf63dfc81b6b9b69c8283cc187&keyno=1',
                directUrl: 'http://key.ru/shop/igrovye_pristavki_igry/nastolnyye-igry/nastol_naya_igra_monopoliya_bank_bez_granic/?region=moscow&utm_term=473718&utm_source=market.yandex.ru&utm_medium=cpc&utm_content=nastolnyye-igry',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iaSx5Wl_QyUFFzoN_aeb1zs2Uo-MPCwEYCnHH892TlAju7-dP67xp7lZHgZDvlasDM1A0CKiH6QYPpR2z1h1qtkXt5G9Q-TlIFTVW5gvFffv_ESCqlwmIfXvsypQ4tRqtw1MIvN_BIrr16tVHNDwM_foOz7kTC5cg96hw0_HdExXIHg_TkPBsooEg392A9-O5Qs69AHjEafQV6rRe5BudiHtSg2--7kYP6tP83JxHXcXZ_nMLWKs7WR2ZY_4MrLN17H5oha7QIpFBMuBZ6bquwseLQbR9T9PqADlFA-usjqT9uw2_QtZVx9KEEChq02x4bXOW6MWB5b8dTKwhNGMLTy9pa_oAFYamj0OCzz6M2_nXTkWMeExb-uCQUbzsG9qgH1Sb2ZxtQnapNpgAxV2GS1p1LWIFoD54kPm7pJS79c51zgbemyisa8H5h1RmFg7yYllylcjW5cyWDYaST-I8ngULP80M0GnTFcEyYmaUj4Z0rfrGzABzqvsKCN3mZEgGMUkKv0ZjWv9nW4qj75tYgXYtamki4RybgjxU7tiop-R0mR0UntGA-hdqdqmUd4ouiE7WlZhuZfiTHN9M2MDAYicPBSz4fFcZGtYOadtGjA7n6DtxxYZ8Og4Ktrm7wwJ6IuMr3ktnnqbf6XyYcKereK76CazBLTyi-yktexWuNNeTypp8IG_JvJEvXmuc4i3aeWLpQnOKA0uQNzSsd9qXeLG4et0iQ0i7-j4ytgLwvt_E-5KtsjjWoWvoEB8Ggh0hbqamdMpo0tP3p48e4O9TXv9r8Nj5Fwc1Qy8q1ntup02DGzimMNjJR5JItBMMRgsldXhYpkjSwDL3aAJ7iQAbyAconFWw_Y3LlxWuC4KSWlP?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2TbkMHEibMcglLw5MngND0HIVH9-Brm02ArcNk90MyssAtQjrncm4Im9dVVLwCc0uGp1U4aMK7SOabubTqzfd7EgmoJ4wljcQ07Z43M23xxthzmnHQ_vS_Q,&b64e=1&sign=76a63472e07eb777dfa8e823f17ac81a&keyno=1',
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
                        count: 2523,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 301,
                                percent: 12
                            },
                            {
                                value: 2,
                                count: 83,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 58,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 172,
                                percent: 7
                            },
                            {
                                value: 5,
                                count: 1909,
                                percent: 76
                            }
                        ]
                    },
                    id: 350233,
                    name: 'КЕЙ (Москва)',
                    domain: 'msk.key.ru',
                    registered: '2016-03-31',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Санкт-Петербург, проспект Энгельса, дом 124, корпус к1А, ТРК «Вояж», супермаркетов цифровой техники «КЕЙ», 194356',
                    opinionUrl: 'https://market.yandex.ru/shop/350233/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                phone: {
                    number: '8 800 5005 074',
                    sanitized: '88005005074',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZ4H9_aAm-SdXUBmN0WL0hnGUTbtOMo7t-jS0Su9XO2QuvjkDGF8rWeokatwcggpmDLthLJNlKtImPfLO_jVKPo4SRvnAKZk4BwLpoSXcNIzqKg3s96GY6LFwPpO22YRD5kjkb7CdOy14js_QlqzMQHWLKz6ydzu8qZnC6x_SU-gipk8LuZ0kAOi43SOhkx-AGjfdAI8Db3STIMwNxErmo5eWKtxGPCIZsx5VIFQJkhl6xOi78nHVffxrsOXbKF0pXF1E0xU28bAY5FBTyBpiaIsQsgzA9XvohP83E4N0m5S35Mt4rjBuzkjZGSaMxueP-N0nk3SwzGXweXYNxSBfCWy17xDww-NEvA1-5RQAUjmr-HzoEFU0deuU5OBz34A95CSioc8s7wqmKmJ81BJziLtlewZy8VfXOhF9zadj4gaMRfjBqmjKnLY7KRwsQkE1XAJXCPRa0K_glCWLCwkzDo0M7XYXNuwJTy-ycXkwCc7cLp67SZUMcdRft5D7z-gPuJuU7gqY5jUF30zHqbdOiiMkjID7T6jPz4K1fAA7TwAqXhtnOK0ppfatLkCmT4uB0PagQKXygeDbndcWiI5LtAzEprSpUqkGkr8rmvb5H26nk1XNxDMZDh6fdnokbNWZZ9ZqQxEvW9b1UDq2V0Safh7n-gkKGKdgljneEBaD3kSEnY53g1gaZMBbzH2xAnatSbw0Oi20amm-caTo5pKtH-1tmCILM28QOXFqacEbT3mlxyQLrUnIAPZGgZ6JJhFyI5pDp5wDZjSQ-F4bYMoo2K-xvtao18Ma907VXaZu1mh9pdUOPAzBmnPNpaROmIjUvHCghBL68XZHOMYVI6Dpf_6JdyU62Tp3ZvLC69C3_I07ufpjL4w8IQ,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8ffA4-23vuScR3iBhnCYpfVkl4OFxH0-QQ4mTrxYkt7KzphuIRgp-0C236NOShWoGvmakBVdQjV7Io0vRfcT2LSr6e6Z6FeEKyvaAcVaxdeapE1o-ukS3rR8vsdJqKbTyEnsFk3Kd1GT-Cl-p3vbkAo4kgwFjbn1wRQxqXLUyO2GrW-4hn4A-7&b64e=1&sign=a6dadb7e22dd6c7cd66aba13438de8e3&keyno=1'
                },
                photo: {
                    width: 1184,
                    height: 828,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/168879/market_EF8jOYzu6qEwfgKfop8W5w/orig'
                },
                delivery: {
                    price: {
                        value: '390'
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
                    brief: 'в Москву — 390 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 51
                            },
                            conditions: {
                                price: {
                                    value: '290'
                                },
                                daysFrom: 3,
                                daysTo: 4,
                                orderBefore: 24
                            },
                            brief: '3-4 дня • 93 пункта',
                            outletCount: 93
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
                                    value: '390'
                                },
                                daysFrom: 3,
                                daysTo: 4
                            },
                            brief: '3-4 дня'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/EJzs3luzi8WwYcvBQt0SLw?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=e-rAPHg5WPR6_8KKjK9HCxRgH911KqM0tLimw6u7_eibTii-5zn6_6HmzdCcdWUtkRo9HTXh3zfkZEpVJNbamrbKxG7GQRWSl-ZdeRzWBU5TS_jUbNBnDN9nXMtnMCy0V_JSOgcMzPY%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97BTbHVvfo8CrCUgDg_oT5iRHC6Bm4vAdd_jQeA1fkMunanOwmcHQ1dnsJCk-OGw2qNxuvpLtNVlPHwZgorGpowaXigb43q_I0dYKyhR_qinujBqpuLds88c5o2Iczkct3lHVRC3tYagpRpx9YsVpttTbG-2C__7egR5ywiRZA0nFAECminawvu4xln4gk7n6z6dKrBjd7A1nOV3wo5DwKEpbrv8FbeKhEnPZsN12mJskUGbSxxHJvfV40Cyhh7ZcdqziWcosBuXIJk3zjXaPMNlSd-4OL3WrR3DJesS7oVvd4qpScmaQn_5HkloPR0NEG8DS_8GO3wiLpay_RCKejQG_odeh_2FnnoVpFKBgO7PH_8DhpF76bRjErhGAJdtXMNZaBB8N3YwB20fYsQcd2DUCciscjkr_ddOpcRA7762X1t5B-HprZwMb80XxuKvyPECM1VQzeOU1a__Tww8iMmxsq0jTV8ojQamHLDaRyPmXCQjFOQeiVk6ca4eJ0m1-4ML2EYuIV6VbgEeYV8PlXNPcTWE0bYhH3t8DQqWFuDfoSPwx7vk7UhZXGRk67NzaQMWIuWLN9Ebk46z-HzCog8gKpad6hscszZG5d31MNy2k4UDSXfiEOLyw4efSK4XT7YEHeGPZVo49rSKh7Jq_ZSD_E4IxxctpcyBnpSbdq9TGS2pLFTBg1POshq6x1T4xbrOmCNusaO5-2t2sSQU2lZU,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXnteZ-vzCC2XuYhjFjDa4NeLITaDMeQQXDggiI4bIxn6q2J-D_J-clal1oMo3_dwGPDIj6vzOuC-qtFDJTvnp0EUH2PVh1-fbb7iJNtW-mwiNYBcz10ht4RFU4b_oNoCjgfHY-ZPLt9fgoIC7lJAwbBeetQpyy8JVolsF015t1RFJIBwErUdxN6DRCYuXIfQIhSWphM8F3UebaEdscA0QsFaws-h-Ym3v48ajFaqWbro-B7ukMsDTdqn7b_UkXt3bvyki-14_1BoL2Vm1OKIEtkVOppjcJDOH-kPEh5jjdZbpRSNdTB_cIQ,,&b64e=1&sign=e21e5faeb5e662d8005399dfa2c1a818&keyno=1',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: true
                },
                photos: [
                    {
                        width: 1184,
                        height: 828,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168879/market_EF8jOYzu6qEwfgKfop8W5w/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 132,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168879/market_EF8jOYzu6qEwfgKfop8W5w/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZG6QqQWUCE3jXljIdAbJmu3Ii9Axb5ZoNR0S67WLZicWahwpz4jTfG49kj5xiR9rJgBdf5amJPtI2iKk77AmdWSmygA50jlHjbmY9ZqVlfdDHshyeclMTW-rvXZUG8ttEfkXtpp_q8IKZ82txCIdwcF_VfHibi25ZSM-mjzJ6nq4mxsyW6oGv08Gn7H7EiOmG4RWCoo1Hpze9XN_h5cpXE5PkVUdhzCHOwpCzMw5gV-C1jtbrd0gsTNf07AEpCGScvXG2gEEKeF63VWzW4MnyyqM3Hx4iyAsew',
                wareMd5: 'QOrtPafbtlv2peZWDS8HMg',
                name: 'Игра В6677 Монополия с банковскими картами (обновленная) Хасбро',
                description: 'Новая версия настольной игры Монополия с банковскими картами e-banking от бренда Хасбро. В комплекте: 1 игровое поле, 1 устройство Банк без границ, 4 пластмассовые фишки, 22 дома, 49 карточек (4 Банковских карты, 22 карточки Собственности, 23 карточки События), 2 игральных кубика и инструкция к игре. Теперь все карточки читаются банковским устройством ( в том числе и карточки с собственностью), что делает игру более динамичной и увлекательной.',
                price: {
                    value: '2659'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZ4H9_aAm-SdXUBmN0WL0hnGUTbtOMo7t-jS0Su9XO2QuvjkDGF8rWeokatwcggpmDLthLJNlKtImPfLO_jVKPo4SRvnAKZk4BwLpoSXcNIzqKg3s96GY6LFwPpO22YRD5kjkb7CdOy14js_QlqzMQHWLKz6ydzu8qZnC6x_SU-gipk8LuZ0kAMti-Euel_V_FkRx6jiuiDZffL3FbyGQygb-Rw5j1IoH2QjC31r7QyKVr-P7TaQv6T6LUTbQQb3O623yiqmCEw4Xgtr3eYf7A_JiZqqVhyPw2M9uxOeDzREEsy-w2WcNCDMNntvaajzUbZJWWzmGOS1nDKy_MfpjuhuDbn1ubMMMa_U3VktGEzwdyhBa80ruw4e0YztSdD_Wr3XjlcmrLePinxvswY9kVn_dQaVKbHJ5YrJ7kuGHUEG0eT2Z1mWOFrdy30yKmlAO_KM_nsEW4RMdKniPFi4cg4HUW1AKMA8lQZmh-KqJ6Oo89p6C6Cc6JUAqktu4lg4yLDLtTBhc_DCV5GILee0kxbMp66P8niARZyN8niedMABG8NlyZX9tnnYGlqMo8W_kn_YTH2EMmkEQTCMjggr35-0vfBYFICjAi2B-K2xbdRWvBnAorYlrckDzNLz_WODARZ4emzj_2MNgSDAhV6QB5rnvcWQYNd-tAfqxKZYkAbRae4_mf0XE1Ccw4eqj0YhFQkVLUVzA5Vkf2l4GIuvvm_yXXTV5k50P03CNiOPwxiURusRr5-RgvzY9p7NbvzmtH8dYvS79ulVg7yWDnQG7SOg_t8r6GRrwvcHEm7ARwZyJi4qNC8ZD7sfEVW191t9pROrEWTG2xjBFu3UGwhAILFhyU-TwZrxJlF059WgKs7U?data=QVyKqSPyGQwNvdoowNEPjWDqp7eX5kyi-HQ_TsWGZunuPtEOJYYO-ZJJTxWg4TEu0UC0NZIScrULeo8mfHPebkgWDZoLAKStOH2HiUtSZ8Y9D7e0AFUrkZnxre3IEifEEU-hpqPYxoCtEW1R21ySRGOi4envlEBmHWpMIgEq9XzfWOkLdDeGbvnFn1ApnuK3KVV2SSMYg3BG9ZyKwk0a_mL-oq_-ad5qQ4lzN0HlUBWoqRIQQcSiuOKwEQPLMA7V0DJZfYHHJApYWuR_XrSoi35o797qB0Yh22WGDIBwb_H86IxRwsU1Cku6_N8whfWif4fx0KAJMUcDllqL54uEGZ83vIsmX7VX8b9utHrXvPedUigb89vy2U0v02f7aTw0eGiTS9NdkZ8,&b64e=1&sign=5558b1c044b6068de9930ebcf15c58df&keyno=1',
                directUrl: 'https://www.igrushki-tut.ru/catalog/igrushki_hasbro/monopoly/28739.html?utm_source=YandexMarket&utm_medium=cpc&utm_content=28739&utm_term=28739',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iaSx5Wl_QyUFFzoN_aeb1zs2Uo-MPCwEYCnHH892TlAju7-dP67xp7lZHgZDvlasDM1A0CKiH6QYPpR2z1h1qtkXt5G9Q-TlIFTVW5gvFffv_ESCqlwmIfXvsypQ4tRqtw1MIvN_BIrr16tVHNDwM_foOz7kTC5cg96hw0_HdExXIHg_TkPBsorKeNIu_PWdN_fcpEAxac-PoFoLWJjjx09F70RAAUWzb7nMAh-mSJgxyX4sUSrBtYJ0Bn9P42XSyXsWI3K4yryEO5Uv57eRH3f7_BzHlb4Hnju1WKdPuiLsw1RKZ8pzBnwZa22pbbUNYN997qstSs1nDpiGkrjmA_pFPyq11YDrjjea7lEB3p3AGUTbqfePP0zMEmVsqybrTZ65ltfFis0znjdclUQOmwJeBK1ZGDRrewmfydtQ3jXi3mC8TbGTrVx8k36VKt7EeMFocO_qrS_6xKDGV_jscQ3LeWcK2srlaO8J5UE--Q0vaMYMitOcXa_69JZmdUWvAVRINvIzHfcjTxcnKeZjAGy7rhXDKTpwrYEP9eAicMrGMjQslxg-FKKSCRvriq4ov6zRIVL6pADecAX3g5EKy5Lb9msFndxVzeAfZOakYrEyDOD7lLTETh8eBPtWI5Hh7Jumf3bRw-H8eBH1sqZD5_DqzxUi-j6AGNM3cQ01yh7XEcbcpvHmC4GCh99ZKjoZhDhlA10jPwdlpkmzn9-Wpmjr2Em4CdOKXsnCINtGiee91Pow-6bXg9oQIyMDsphi78nSYlzscZpU9B4oVGfATBSK3GnsNC5dR34JgGyinDh4EbpVlbuYFXKj6EBHkhdWFzEiPyVg6g5aBfMxmQNQbaBxej-Z9yG6X0JwnaY2cx3-?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2fmciaQFZtk6tGbhZ5Jg80ykDgfqwcaE_adC5sC9XOPybEqcj8safvFR5Nq3kBnklHplDGQ8-NU-yX4uVeFlv2Bnrm7xvqbTAjDGE65zeiaOqyz-fNdcZYE,&b64e=1&sign=af36dae5a2ac2cfff7b98498f362e7a3&keyno=1',
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
                        count: 583,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 51,
                                percent: 9
                            },
                            {
                                value: 2,
                                count: 4,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 2,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 6,
                                percent: 1
                            },
                            {
                                value: 5,
                                count: 522,
                                percent: 89
                            }
                        ]
                    },
                    id: 55588,
                    name: 'Игрушки-Тут',
                    domain: 'igrushki-tut.ru',
                    registered: '2011-02-03',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Василия Петушкова, дом 8, 2й этаж, 125476',
                    opinionUrl: 'https://market.yandex.ru/shop/55588/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                photo: {
                    width: 640,
                    height: 640,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/163900/market_1HXNhGSfzJ4w_g9LrXtRJw/orig'
                },
                delivery: {
                    price: {
                        value: '250'
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
                    brief: 'в Москву — 250 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '100'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 45 пунктов магазина',
                            outletCount: 45
                        },
                        {
                            service: {
                                id: 111,
                                name: 'Shop Logistic'
                            },
                            conditions: {
                                price: {
                                    value: '100'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 1 пункт, SHOP LOGISTIC',
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
                                    value: '250'
                                },
                                daysFrom: 2,
                                daysTo: 3
                            },
                            brief: '2-3 дня'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/QOrtPafbtlv2peZWDS8HMg?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=e-rAPHg5WPRG4kmWGKUzZKhWrcZKNKUdPNzG1WR34L-5LOpcZgv6UKuf0pL7gukJ--N9jVbGYdxmii6VFlva50JSweF4_a-6fyJ9qkmRObYG6qFQ6nSUjvtVpPKOLTNHTvi02-drtBw%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgRNAm4m1oRAK08K3xTlzk8jSZBJoCC5z7td2fIhNsizwi9Xfr4-dFJ1VAjBY4efuvkNSIvM46YIuI_xVxlx0ewRt342s-j2vgnT7ecdl3blQFy-eX1zI7ksRX7oH7V30nHxua8tJkiW4nLa7kIDRZq0BDq3CB0fSgPOAkKch1LI1sryb4Tm85OWFLRuUYYf1dj3xsPbhenFE_3o0OkQzyBogx8PsZP-LonJIqEA7-aVHAx35skyCd3BuSIKJHt8a6_pKZO2yGRXanRfuXNgvarinDf6mvwwHQb3BgSXb3a_eLp8_onk_X0nMhHjuTnhsZPma6Nijqsi_WM458_VxTBr_EdgLPsJBCpgD-Q-yen5E_Pf9Dk1Hf4Nh_9L3NikCtulODdt85z9Rzvmk8SggmB0oPZcDAjkKEGEwM4Ou2ICmbvDJLFNjosLzB3bsclhMdvou5veoLbD-GJ1N_OCczu50aYVpxPnNhyPHPXfTD_gws3NFdgTF6S9Jkket-7YVS9nU5x_w3awG-HdCY5QW_pktW3CEkZzFDfooL64xqiPiyagYiFXMN_H4M80Qy44jVGOGdjCvkvLx9GTUczVM5RKX_tRqeB-HqtdjP16gmrGxSZAxJk_j9EAoIkdEeQOp-sOuvdUZ17hf_AMJXUdhq3rJiZ2KaSiDacIYl9qKDpsULBJD2dVQHK0rv02d-WiOkn7G7TNFtfKCaNfv5fvq2eSm-twAPAOnOxzXdKm6B7sf9yzOJcFZ9XeO3vsPxzC5afjjftLgIfwltFz67YkF110G2vd8ChR218kU-a7TxZIAgTRqY-SuKKowZHC9zM_TvXMMl2NX5cx8qcL9oOPyg3_iwfK50YvYcvWm_a_b2jhJAr0YWzJYR3JGdm_VcHqzTLX5C46BptJVthTexZ8borU,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-7u3dPVJgwLU9QNpCOQpNI_kHpXnAWo98m56tJyB5BBMXMp_M6zhMmX3RcnldwtRWVFSXj-4IhX8KaIR6Nl0WD0xMc6cjpaxoVe1KAoWHk0craLuorSuiQwhi-gGUIMH6eD7fIrvpgnrhXLHCmOhg-57pfek7uNAAFixnKiraAbfiHLIWDAPTgz3y8ljBfKy76t53--S0b3bbqNU7wfKpS4gioUGH_chkr4JKs9SDhmiT1fQHuvW8kYIzyDltr5HT0_w_4txYH6GvVrW3smYEIJ2-o7KGSEVUm2lo_RBgvLLifriFcG1FfQyp2zxg0LvXDdARAC3rfr-A,,&b64e=1&sign=f6d061ffd3ccb8e01c3bd262b667d05e&keyno=1',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 640,
                        height: 640,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/163900/market_1HXNhGSfzJ4w_g9LrXtRJw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/163900/market_1HXNhGSfzJ4w_g9LrXtRJw/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEC2h-JUgitAyPocLDRWC4fdkzMNHQoiIFV-r5QZ-XXY5CySGcbPKxdRHflbegJHBm_kMe1LxQbmHiHc_HqnJW1EW60Ej2yNKba_UTAXTHSHcnaq0sLm-iiVwvS8a2oE2x9GEiwiZInI6gleD7bxlP-ISsOi3SunbWq47SDIm_xZrGEoA7ZC61wUG38KJzyfz4O8z_9pJ0gMOjN9qi_WTFqRndXndZxMu515w0WVNT1eASvMtPdN8NQndgfgxhBmBHi-irq7kSh5pXsHOxCX-ZB-HEEZ21L9SQ',
                wareMd5: 'NXpLg7vu-r3vLP05MpfTMg',
                name: 'Монополия с банковскими картами (обновленная 2016)',
                description: 'Карты, деньги, два кубика',
                price: {
                    value: '3150'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86u4AYIacoBlvJ_Dr2CtTR2OQvR06xRaWLrCBVmr4icFCZXi5jV74bhefkKaPMZV0mboIkaDMDQ0bLPd9DigTWaprqdmaypHx57XxEUy0ezNeu_rpS9TZU7q2Dy7hs0ohuHvgxYjwRDNB2IeJj9A8oTZshJ53B8GuneqffZVNyGlntO9uCwy-0Tcd-lpSScjbAZxN-EJNq5gke9VJV8eY4mdYk89QzojB6nW-zvRRNywpSXaVDxufx21aKIrEWtcs4dm8Jk1rfIkDsAefXX9jChUFZ3cgN_BnfhEkL16Qa7rcnph-AfbWJhufkwf6aVAnLrRnnsWNfoL5wSG3NrkgPcvIyoX8fgjd-66YFqkAiBnWTPI6g8T_WczRZQO18ip2C83rLYhqn1NccBuPXWTxW6L7IJSwRVjGMK5IG3TUA-r6-xwR5MfVZVhQ0dSFQYJH2RjPwcozhY9pAIu7_NYCvq20mdP57gzMDhXnrj1SeXN2qEblTYcyWpXhVVAlSxlVfeSDDOF90YGDabDur_AzrghjI55XyiDK9UUxzeISSVTvEeGlj_OdP7ZwudLeR4HfcIAm0U63qHQJ7hP-OZi5R4XUD0WmkVjNG8BwoWbKkB7LGa2vInn32FS2KwIvALNX-_wcpvkwlHAU2Vw0d7zpRE4OqQAsq1wu059QqanCIS3wn4sNffawRCYzFCm73qpEdbGimPLW0jtQECYhqt-ZXnv7WeQLdnpOCuMJGItp38oTjFlYzXKcWYwEEloKP9ecC_HoRPNf42a8XQdfAORJ5IC1PTbJt2BumRuWih71-Ux3uTc2Sp8yxH_lxI7ZADSsZNYxZIO7uPZTA9-pZmSpZiwyMkQaWPlqn?data=QVyKqSPyGQwNvdoowNEPjYaX93spY7xX6hhdMwyJeUXtTuXDLWuhaQG4a1hZzvdYYmWApz2oDFRHrM1EaNS9vhF0oppB3PHxGUsLTNkzndag1Ha3VDCob1CQmNuSqrfZRwieXHol3TS09S4zFidk0k0d8_woNTv450DC9pXG3IddrjyNGSQ1ER7zEIVWMF4KyCYlb4BDNSFVvhz-c6wb06hiZHleGgVqE4JzkFU1JYdsJlwSyRRzrcWj_-JKQ6NLwQ1A1DVuHJN-KElyBzIZiibG7L-UgGe2Sh76R-5HsuPF02CciQ0X4vyYqY96Pc6KDBIwjW1wJDpsSa-PhPghHRwgBWzDcIj85G1wSNZGyRsQqsw7urTgnTZv9ywf39QXaRItZYf3HVU_JPczsAJAdPBsd0jHVxCxbFulPI7UgLuy8kJNXqIjbA6QPOQd95hdt6wFImXyl2qosMWtYYiaBqAXW_extV5s5CQORIVH8Kyu8bgw2OVhRw,,&b64e=1&sign=b1d53a347dee8f6eb08ae20e7fd926bb&keyno=1',
                directUrl: 'https://www.mosigra.ru/Face/Show/monopoly_banking_2016/',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iRIi4WZvMEuk7SYv75j_689ngg7hnLjZo3UVU29FSWyL8zNBANVUwGbd0LMcW8_doFXWbXDAFbl9I6famTvBrpTL0Rax8l0LK3qsYMj0VcoegyvoPFE4m0KctxmYrSDblaO7kGOZw2eKsZOFtKfj4jLqWVs_W4ltFz4WuJeaoUhIqrtmOByBU1vByj2GHEOGYuM3L8V9l3ptmAIcOEAVP5aHEuEBpaxWrmBCFDoRZXz1vIWWdRLKFKWxScAOrTZ7R-8aDm4untQ_aYDoA_kr2zqoHQgoRdU1oLi9L8TIei4CWb6lZkKMwS8nByf2Flhrt9Q-ct-jYaYINKc1a0fRkg0G05jiE8asox4bqTMd68Vj-mC9pxwe8VsHq1jP1X8gmxL7U9sIfQWspSf0dJSG8r6SNW4w3qS2B9tsEfYQW8e24o8aD2PqE0LZPtwhodW8XS60gEDNvXiWJInX9-kuxgRl0jq5RJxBVmalIns28xmHUHDeT16cG_ooM8xswO3_EOH9IB24Rex8GVDv_lGlntEwNrQyQ0Xqoxbx8e9YorU7yhQG4F7ce7qeHb92HMyLDTlwMp1s1iresxC-h7uV-ijqNvveAPvNB1qVQazDbuaYD65Eg7zORNgQC3j1-o43WAwmqoYQdAgyg5ddXTzaL1P8CnSDau2zzX3LCXhnVkThueFbw4_-oAzX-lf9epD8O0EOI5GY9ha07y75Gz3aqy84GR54OYjNOLk4w_SyUFu80pk7GDwwYQhrIEihwv-_Jmw9ar790W7IBIZLhOyrPbbZevEhpQTGVcQZcpJwO6D_a_j85VgmztJIgOHZQf26xEBsFyH7ZUAj696gbk8beTAJBvQZqKLPK1kGBlW8OZBG?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2ayRdNbL5ECXJKgid85M1MJimoOXN7dOQIQtGvA_-arN2KgQLN8JbLHbqx1oxZQpELLybrTg4W_NrGUPEjjg-Mxr72UNhlPI2raxXpndf4KvHbqwQKq8CE4,&b64e=1&sign=f9944c928e5fa870b2de842494dba764&keyno=1',
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
                        count: 657,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 43,
                                percent: 7
                            },
                            {
                                value: 2,
                                count: 18,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 14,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 40,
                                percent: 6
                            },
                            {
                                value: 5,
                                count: 542,
                                percent: 82
                            }
                        ]
                    },
                    id: 30186,
                    name: 'Мосигра',
                    domain: 'mosigra.ru',
                    registered: '2009-11-25',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Народная, дом 8, Магазин \"Мосигра\", 115172',
                    opinionUrl: 'https://market.yandex.ru/shop/30186/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                onStock: true,
                phone: {
                    number: '+7(495) 7894101',
                    sanitized: '+74957894101',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86u4AYIacoBlvJ_Dr2CtTR2OQvR06xRaWLrCBVmr4icFCZXi5jV74bhefkKaPMZV0mboIkaDMDQ0bLPd9DigTWaprqdmaypHx57XxEUy0ezNeu_rpS9TZU7q2Dy7hs0ohuHvgxYjwRDNB2IeJj9A8oTZshJ53B8GuneqffZVNyGlmKrtPZkJcSbAkvHruSfajj4oSGhWMedMFsXLMa1OVJi3BSQZep55oFkSO53-Iu3RDAqzJBTgSrlj58aU5l2OOszRXreuaWvUBwMS8JmEorReNG_8dyXW6Z-s4tO_Wtnsi4VF3pDoM9_As6ANpqZZ_UZCifDGROwpgv2AF-G7YBZS2ATly4CjBskCZg-AGrqYAaRd-V_EYILr8t32T2D_xHZlTjCLZhpZunGpt2lbbnFCwsXbPdsXOdgjjVWyJQY90F2yCXAzTp9cLGheXMoy-mXXO1J0NEPtsP3DU4jWHMvuInbDbtkywineJV3JFCPCzRPCUYGl0fgJ4IBb8aV34zz6CfDebNy18Mt0b56t098uZhxyWecv1Y61qdCDWow-LB9bI3swWEYBa5GXHmuE1r_4o-ZwemwbcsxBtEguFFeuGeXkksYsNh80SEZRRLxDZXiOSms9SUo2OGz2ZH2u-asf8H686b3VOmvwx7XxGafBnx95f91i8pDRtcKabu4vm_ourOh8TnOhSMcao0BtiK2o4E0R80J7QpKwZY2YxRn3BKNkp_TCCGiVyb6Eep6udeFf6vLrxyTh8vtAxH2JegpBlDhwXn0qUr0MLtLyDq91y7kNd6B8CspuwG7vJGJKZhGK1MTth0JwEAhwGRaJhFXF4PXqU7kV0ClL1g_mtKVCji8XXDZHgX?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_sudNyOQq1ATftFaz1MdX4vCmdvQUlm94rhqQh46F06OqJj6rDyxPa6DK5IB1UElzzPx4ANCuP0avTaeVoJPRgD8xf-vikPIbHklyAiOWiPjelimjNlz8VwBsC0ZQOBkJ5e2E7T3yoSLaHy_B_Di-zS6zYIGHi_hpaPhtufD3V-jwnuAT1WJeF&b64e=1&sign=614c5f320da0a94e15df96646443b90f&keyno=1'
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/364755/market_SR_UAr7kJd0btVU5bgD9Rg/orig'
                },
                delivery: {
                    price: {
                        value: '0'
                    },
                    free: true,
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
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 20 пунктов магазина',
                            outletCount: 20
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
                                daysFrom: 0,
                                daysTo: 2
                            },
                            brief: 'до&nbsp;2 дней'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/NXpLg7vu-r3vLP05MpfTMg?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=vt3Ikny_B0RcL4sfjdCq-bCw82LNykfWuONWxciPZ_FWl_gAHQEhfj3ou19cbWweFTVMUJm5qFrXQ-7LHkq8lOXOACg76gjYA-Y4W4Ge3wqqtyO6e-JK98wPtB28NDpz&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/364755/market_SR_UAr7kJd0btVU5bgD9Rg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/364755/market_SR_UAr7kJd0btVU5bgD9Rg/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZG006Ck8IDuRonvUH99cgMRAIevKwO9WFoxISfCw7kBFUYCqCgHtlkuLxBZ3f57Zp0h1qCtfwBo0KOFYQswV7eHDQdnYwVv6bjCvlHIFlN1--GClLBhoKcHtiFGgwXNSt4g9nwwVv5wFlPuo-G2-FKY671lMJ4pYwDRwtx53KWnaHQboL1oVSU_CTv3UtaW6t2nz5Lf-YMGdeKcohI5aqtVHMzgw8X-KMcPVS4nkiK14KM_E7YzRUlx88nM779ZoI9sibMeljtsOYFBNtmBoyDPz3Cfk-fHmoI',
                wareMd5: 'mOcyRvOawbacBlArdyPCsg',
                name: 'Игрушка Hasbro Monopoly Монополия банк без границ',
                description: 'Монополия с банковскими картами и с меняющейся ценой недвижимости',
                price: {
                    value: '3077'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Vt2-BSM2AAXnd4Y714v0C2j6cQHwiQJQCplj6aadThxzHc52r67qINeCyzSFJJl8d1v4PynKrEC9R6TMH_GA-Bp46WqctsiK_ECBto1cVxWZSu_nAmBV3esEUkeK6D1QBrMaN5HZkq9FXcbXIcuJsDenZ2putIxKH3A_gB0YSCHV2YdjHiYfhb68aqQOGLy2Evs4pArLQob_F9sthIV4VapCCGOjrDfJuBBu5Pmss2HgsgWohPewD65jBwL4YWLFijdik18lQ_CFzI7IoFrRLC0Czn8ZFAOCOm9X_Y7JTsTr960PvqqKtAF1C7gibT9hPvrB8S6u0GFGJgt7tJPIjiyi4CAN34VUcnv-bbohz0KlZqxaMjZvW_odnrAHQsN-Fb9djrypDi61QOPnAiy0aM3RYSbDJKDArLlSd8E4f5au-6BNNUhQB3e0Dzv6LV_yINN5RmUEETA4biPZQmZGZn8Bkx3S8QgJZLxR_XgBOjRfEl01cs66wtNbqNyb_jIsdDG2TOFVeAQvOw3w3fYVIgU5JaOlcns4i8dBEJJnl2eNpe0fZP9h5P2G34ESwf9mm4qKZz-MdfsD3B_yhZ2ekyHl3BCNpZnHjqZfok-YFxKeXNqUQEnCqXBdtDM6Vw_bQoYooqUiasQHr7W3MiRoB05lgRVMIq1berr-SxD0MT2dl-7da91lLuYeuFzVWLLoKI243d_17mEGL6iUG8CP8A,?data=QVyKqSPyGQwwaFPWqjjgNhOfHMfqnBj5S7FkRoG529LWVxU4X8hl_D2o5TIEiNLST7Bg_5gLTivrOCn0TiN9RnVJt8WbDdoGfKdRUrnncjLph1NlOTW8YxwxithxIPCvlTCMtK-J6nGfoORpP7O6p819JBddpw_BJPJcF04dlVf1vKuO9Ge9NWXwfco7MMg-QVHmMo5F2hQNX2B_s36IV5I5VjRHomydLB0EaTA3_OEbDkDedomVMJjVpXKDDKIVG8azBcOo3zQQpU4_H1hdZeSSPVK3lCf83rzlsFgFdx32LEcedZkST2iIIqpv4u5HSXfm_xT3939eWV5NZ7HczSl06cCXHmKHpIyNjbxp8bKSYdm9ZhIGisEBBbiznciHP-dxVwOijMNA_cCPySLbMGIX-No32SsHfIXvbSzNcIinndZm62hWFY1H8NSazw-1fm7CrObCi9vS0SHL1AMmCksyBbeAAG7Lu5qrWydmilt-N2Fjmh6DSorXrA-eKkU-A1vqclmOdWnnrDJiB1mM1TcC0pYFze7_&b64e=1&sign=e9e3bbb991859cbbbd30ba5fd696f08c&keyno=1',
                directUrl: 'http://www.nix.ru/autocatalog/toys_hasbro/Hasbro-Monopoly-B6677-Monopoliya-bank-bez-granits_317460.html',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRtNkb9mq-LhCKNVQTD-xsfM-KNfCH_RRx2kCH_dAQxJD_s_lxFXvmsYTX-By-kQZuSAXVSe2_iOcOia8iKYPHcBuADXkuLVN6yHDR80vbE5fM_K_lQ2155wmLfIj9ZQ5qE_5Vu0d-9zvH6VYbSUQlloMjNDx5yit4hvFgyhcDh2Za8VqWstE2NqhRUNTfpVsriSC6Kw1UE_1uVSvF_luBhOyR4sXx3hEw_Xqik0OI2mUCKowArEFklGubkEYwdTN0Qqygf_mrXIuDSAFkew2hHRBw4ZoE8vDCxBJ-_7pfh84qDfFmkWtAPM0qbzP7p94jao3vtiMitkdiaeqcmWTb703SbX4wdmARLY9p4BO9TuY-Pw01lN632D43koOUPGmJMziIRooCqicPuFmPOA3FcYsGZtwZ8CBCiAJtc6W0NOKkwRgyQAEGUS5xDEaiQjxeMuX5NjI9CTHI3uSM9cNBZiCAlWWtZ9G5oyQGbr5_PheS2i7SSvAmDk2KIHj32ybhIB8DlJLwhIJZ4SXkaf42tiUquq_fNoH8fWUjl7vN-MT9g618AgZfOX-kzhqLH8MwA8p7M0BF74tRwC_EdQ_aV49tbOZbqE-v-q9q2pWbE1vJ9dPOknn5IKU_m6PGIJkPl7M4kFmQ1snTNCBNEvyle1dWakRJrTRl5uWVukLJYKCSBcum2aB3rZu0Q8wpx3C_sfcTIDyVuKlvgaZRSrEcmpGnPuImqFSbAIIl-C6MKXcbUYjkAOOPhEj0dar0mvZ_bvBP32wSLC9-86lWJGJkM8ewpmPK1r8eJAMvpDOsB7e0OlcQmq8-zigUS2hl2eajEnmqcXtBYt-fEzQDyt7TeS1uetOJbAS1w8pgh7ypMetQzwc-SUNB-p?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XM4hAUyQ1t41F7iW0v4Yux7OE0KrJdvKvGARMP6bPScGDAfEvhjBG_AhrXRPwUXjt4FbjzM8Gl5t4XNRhEiGVHFDVpx-s0yLQmpQYFt1hVPf1IkbFqkh9M,&b64e=1&sign=c70930c7b9d9263c55589fa62616b722&keyno=1',
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
                        value: 3,
                        count: 642,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 132,
                                percent: 21
                            },
                            {
                                value: 2,
                                count: 32,
                                percent: 5
                            },
                            {
                                value: 3,
                                count: 13,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 33,
                                percent: 5
                            },
                            {
                                value: 5,
                                count: 433,
                                percent: 67
                            }
                        ]
                    },
                    id: 141168,
                    name: 'НИКС',
                    domain: 'nix.ru',
                    registered: '2013-02-06',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Звёздный бульвар, дом 19, 129085',
                    opinionUrl: 'https://market.yandex.ru/shop/141168/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                onStock: true,
                phone: {
                    number: '+74959743333',
                    sanitized: '+74959743333',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Vt2-BSM2AAXnd4Y714v0C3UvJdC6Sj5BXWyBalipPmOBiHfiwI2bleHhfmjGo1Tesw_TkVBDWCSv3imNjaKMOSAI5dLWnzmHoLcHDHBCZlyloWluAKs4sCZlfhS2nmUCXSXBxbYQ4-Tze_fxHIBPujlYRk0OEJzRjl5KLlJKZnLJNMdz6OOfZ9XKu6JP2d9S7LhQGcgGSrQE4ISzMVFq9WBBOwRdzAulL4WX-IxqczGKPSN2JO2Qnj0tJmWI2Z9dP9LoSMVDOPGgcybt_weUC9ogWcVu0RdiAzwXe-1zgWW8-xpucIesFOn7ljhShFI9IOkek6-2DD0b_UvklEckrZzKp-PCYdqNQPPz6eSaUNpoRD0ZnvumnSFgQFl7w37hTd-kuAzU0uca1qps1ZzzOyXVBLH1gY5RKz_XOku078vTnUxM-Vf9hUrSCmrfxmVnoC49QAAKhI7k4hy6LhiNsdhUbnC8yXgVk__wpBn_O0b9j0gHYesV7ABTJV_jSLe-A1qMWcuFMysFUix4R3XHCFYKilpUoymx86UKyeL10s0QPtFui74TY8aN2Z3_dPLIS9-Z0tj8KLfnk77g6X2xKxE-yAX_KLCIQulkuniZdNSi1OMFC9TVXEcdxjURSbxyLqud9WOpDx1hyeAmhIlSXKlK2yhh7AaoqixnhkoFmHVFV3ayMJmEd7SCu8HnctdcwZJWQy_XK-ucy_U-tIH99E,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9JCZHwKhHdpgX70vhLpn99ysiMrY9qqi25Zf0JhhO5U4-0xZSALnZ5NQC0yui6KnZsonFPJYvfPoseOfmNF77WO32rj9cXLaCrsuglvRETPYYf7u8lrNzzHFQrpaUDEPgzgxc24BeZbugHNuuQyqrQu3JYI9NjCGEodae_Q4xvwADL-iVF3E-q&b64e=1&sign=43444280c9e2e4ee8bc86ba082825690&keyno=1'
                },
                photo: {
                    width: 720,
                    height: 482,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/239999/market_ZqwnSTbDqqsnoxKq3zq9ZA/orig'
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
                            brief: 'до&nbsp;2 дней • 7 пунктов магазина',
                            outletCount: 7
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
                                    value: '300'
                                },
                                daysFrom: 2,
                                daysTo: 2
                            },
                            brief: '2&nbsp;дня'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/mOcyRvOawbacBlArdyPCsg?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=e-rAPHg5WPRBsRsTBh96w_0V42GYP-JcZcSAU4KhdBsldy-UkTlnLKxQ6VNQsZF9pczz1HcEsy4ZNepSpXrNQQFeQDVg3HKSHFOKg8qmO9oUvA6u95Zyq6IJUULs6jTU2HA6N_5YVw0%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97BTbHVvfo8CrCUgDg_oT5iTnW3RGGDcoEYCXPZhylCZZ_QNwS4JGaqS6QqUrx7to74fiuwRHlSpNPxKrh6gNa32kz-cpNh-BBwvg_Wa47nxEtgtgIffgjx4CYBsxUZ875_FfwZ7pWAKwyoLirb231YVN5LIPldPP0mYKnJlzk-_8ZsUiNu1SHLooxnfVY21MyPOQWp9MuIfzNqADn_SdXtFUng-H7mLDEQkDbC2sYctQsumixH8EZCNM7NtOYvjGzap53CHeG5BvEAj0EfYzX_sz3j6GuR_fs-ry7IQXu-mfd35XxiGeY4l6-cGtvDvOSydMcdvvwvEllvo0VBLq8cOGfirT8bfGunJ_bSm5RnZzmpREt6GXf7vhXqjmEBWp6GIAU3Iea_nHj2hRortlbtT2P07-tJDApdSvyfNueUCXCuZ3OoanICjikvJCyT4xKKFvi6J40xvWhMrxMUCgNT1HAxU5ZtPDi_TGDoRzw4xoB_EmZDpig-5hiH3PWPwEW5jjc5ei4oOPCpx-XQJ52zABubMo6iqXKXDq8gvZ5wRFt2HXjNAkPY1PCq3wZpLkszbUcsBYSOW-mNHx1Z2Eqh9piU33_MscJl9hVYK-NqU98dhXR3RNRCpgsiTnDkpxq_rAiQdVOSQtGrvrioSaD_RYOxijkkpOt9Wqk9fA70DnTwcKVlxi8-l_qGl9B7YYkQTf0c0cXE1q4AZFKWJkEOc,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXyGGYFDhgdG81PGT05SxqY2J0eXtB3TSdnKZsaZEHpSgLMi9Cg_D-6VtTF8mjvaItAxEI6c8GxlStq7aGvSNpYPwCgRJEX9cM_DCq9drmvb37ejIY2FJmSMglsMTzMJbt_Gd4lrVETV8u07ha6vaWkiFAkXlkxAVsXjFWNJ2zO0r78wmO7HxbWBARSKANPjJ8qRN2DDHSVnnrRigzvdhzPfM1pQnuWSl68ADtGLpkj86AAmLfWaWvuLyrFG2Ifu14jTIjUyIzEdADW8LXbf9SB7WgP0GqT_eCgTUT31hxnHwMAS5QALc79w,,&b64e=1&sign=18fa1ff8197a280259967dfe5cb296b4&keyno=1',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 720,
                        height: 482,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/239999/market_ZqwnSTbDqqsnoxKq3zq9ZA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 127,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/239999/market_ZqwnSTbDqqsnoxKq3zq9ZA/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEeXWGJ79IOhIGZlEBYlDkZO28bhgEIDtULWrX0aJ-9y-n5U2282GK1Nhduya-LLHjAWor58_ASu0naTs96felxduuTPO0nt68dsrGFbbF29OGXuec1odXTR8Scvvx9-b2GWh0NhiUdoB29NhTqoLUbC4FrQmggmmCOZJ-cEWwuqzngMAo47SAR5pUYuPTgT5iimzE2iQgiR0IGKMfJFpnJ3AoMVgAgD7Y_9Ei0xgQwZqJD8pGfS1jdBaT5jip_krgbw6PPxBme-zZaWCL1RJZhjvtbEf62o9w',
                wareMd5: 'naCBA46SX2tRXtHNmOe2Aw',
                name: 'Настольная игра Hasbro games Monopoly С банковскими картами (обновленная) 6677',
                description: '',
                price: {
                    value: '2500'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mS2X5VJ_UneMITGv64lo0QZUjQ_IJuF2yHJo7Uq62mei4ED_ZkmhZHfN9ydIlvnqWWTBc39gvdlFsJNgB1sSTX-EbVhhXS3OOfFNI4rCawALg3FxLg4Lw_PMfk5IoDcnAvlWxJrUy9j_MHXQcqIsFOC5IdnH_NSAWZQ81L-3SyaOsETIvGNKravenTlHCEpVcb-xpAegvwrXbGWXvksOLzZR5ZhBht6A_bXLQtl_pUyvQiIwYXTWTGpF3PgvvrkTO21XgbSDVF6yQt823PldO4AFZt1knidX7UOAn5BSYZpKKS7ZCXOKt1u5Caa0HDgAb_6GnpRTnK6ty4u5OwCcVxGCMToiCf-jnV4DWe-ORrmFtWnf02WC5mDrpY-_a0PPA9Lj01Kcss4IDh0U3o1VEVq0Z26DBw5E7_DtIeK7tS2fCwQ2MS07FtTO96QB0j5qFnPEJwzHDRPgv1ooqNd9DupkVchJAAugHwRD4RkDQ9_GalVXAteIZCryfbcIkCAj4qpznxuTgc2ECw4wyKuoyHx1o0GRSSRjqqTercN3sBA7yzi5GGmtSJLClGzxJyXjIIMU4nWU_GYa54fXQPqaMhSCtX9GoIKQlCb87f5I5fLQkFMM1Nfw3eLI4MLA3Of3smx2CqTPAP20zC2SmHRGWP6iUKxccxTnG_Wv3Augmn_j2T62SgEEE67OigxEymZSiCXsOLdCbJR1BntFxhCo31rC5oGTah7SGX9CTz5qTJqgjByoI2Z-WFqd6db9qwWSyJkaEhmjliqg4ZosDc2fE-eYJ8_ymDH_tSEEpila8a6ZNMpOGHKzLUNuSmOuV5cf_-rcX3_AJZf8r1IUAitasTtXj68KbBxYnnDW5cJ2lcN1?data=QVyKqSPyGQwwaFPWqjjgNipc_NFEv4VsrsdnuMffXNjwtUWOxNTj-YxZcIvF2NmKck_ZaeNDK3g5bUAGEFpJu7IyDwpZuiVw3cIBK5bQkkGaHGs0QHX7s26t0ykGOFc3ayBMtLlGPNZ2-7XNZ2mFoNURXZ3-WRNo&b64e=1&sign=a3506ec52cb7501314e41f347375a4b1&keyno=1',
                directUrl: 'http://www.babyandtoys.ru/shop/UID_3164.html',
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
                        count: 876,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 29,
                                percent: 3
                            },
                            {
                                value: 2,
                                count: 6,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 2,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 7,
                                percent: 1
                            },
                            {
                                value: 5,
                                count: 832,
                                percent: 95
                            }
                        ]
                    },
                    id: 96053,
                    name: 'Babyandtoys',
                    domain: 'babyandtoys.ru',
                    registered: '2012-03-14',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/96053/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                onStock: true,
                phone: {
                    number: '+7(495)774-71-36',
                    sanitized: '+74957747136',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mS2X5VJ_UneMITGv64lo0QZUjQ_IJuF2yHJo7Uq62mei4ED_ZkmhZHfN9ydIlvnqWWTBc39gvdlFsJNgB1sSTX-EbVhhXS3OOfFNI4rCawALg3FxLg4Lw_PMfk5IoDcnAvlWxJrUy9j_MHXQcqIsFOC5IdnH_NSAWZQ81L-3SyaOsETIvGNKrauKH0qx4VFq5Jq90EK1uc3X3oDx63nJdmvKmVRgi62hfR5KINGTCvSof9r2xKtiKwht8wHHX--rfOzH5Q12uLqA9O_m4EosYm0BSS26tp3nc6IKlDNGCKcwwlJR2Gh4LUA6vNXgT1pa6wDCpzrWNlzItrb0RilXY5UQoihRFnehBUTHisbN5cxGU5BZz2KOePHw8czTkb-Ej4TMPn0RQDF3ZbOMwWtecGDVWmi5-Soj5LMgnMAnt4hNLUkgbQAAQ8e_Um-YK2mly1XAkiTjSg3G_NxfTAuiWSxPMIsCBL9Vfz2H03nIspYMbIjKjBdNqNWxWflRZoBZjke52CmVsZE10k9WBJ5vLZksZJEyvLsO1SztBVHAoAtDYxEVo8CN66H2Gf8Z6i2f1oA9sTjpqg_bIG4G24RfbZXfXBOlhgUMP1IUs-8omhRcfR-DZkSsKX2cVpSZWjSZ2iqSs6iHb19R1vaDeuj3A5lq5aSfw-byCOhZPExGA7TmlmR_x2xDVdmUE_nizHZnCuM16GHGBR416Q-1yMW6hHQvEZbed4pwpE4NDq__xklXGSaVsOWjPS1IwLQ1o3g6KC7E5H6l2ooPTdeEeCJVp37cQHA1OJpUClXoH9pVulmHzJzivByoAhc5ZvI3qYy6ip3Miut2XmBk-EyeFdg6vJmpxLY0-mS24AFYXvvT4OFB?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8trEt_AXyBRlTPR-2_RCnu_3NHZN7WVw6GPfMPcFeqZUm1_7z-LNq-puKX0lhUbHdUQhCV6BvgttYv566VUoSZzRbaMg1HQ3R_jC2_Srmfdkw6CzTftNKtnKhVbreK6E_sdUcFO0bcENGwquYz986mp64ZN5y2FWbe8gT7gNkmCxkma3E0Mq4w&b64e=1&sign=2822b0785fa3467877438349ab9a1a6f&keyno=1'
                },
                photo: {
                    width: 300,
                    height: 300,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_9p6Oy5LCeU-HeYMNU_-eFA/orig'
                },
                delivery: {
                    price: {
                        value: '0'
                    },
                    free: true,
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
                    brief: 'в Москву — бесплатно',
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
                                daysFrom: 0,
                                daysTo: 2
                            },
                            brief: 'до&nbsp;2 дней'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/naCBA46SX2tRXtHNmOe2Aw?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=fVwzcw-60Gjl_kCZGDp4gi2vx7_5mzmD1IhIwMvoWmq5l95_8uWWLAIO2Y_Kjw4sMb_ONRz37zAZqjZ883WyXeOqwNGYfBRDYapMWGzkV5T0QJ2Dskc1Ne4ECZYczc_w&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 300,
                        height: 300,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_9p6Oy5LCeU-HeYMNU_-eFA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_9p6Oy5LCeU-HeYMNU_-eFA/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZF0HLyOdVu0f2AGmWE0SO_WBkA3BoABGib2m43FnYMht79auVj_YUPAch2TPLdADKqzFLxQLQfMXFwLqKBvZ6pS0HzANj49iXmPZXoiaMKbpKQaxX1vk2FHZOGup62BPYKyrBxnNfPCUhCxychqO9xGnddBk42EkwBsDGVBI7LEP8xWbYzEntC41hRmNOjW5Mv5IwzdglvT0QCAonfR9NUpDKokRhuY0w3oXPwi48XbJb5uYfzwbr9M2pd-2I8pUthRp4TSHuEdAfWrYAd98VJj5hkLoTu9a0E',
                wareMd5: 'jfot7wdt98P0DY9N-loI1Q',
                name: 'Настольная игра Монополия с банковскими картами (новая версия) B6677 Hasbro',
                description: 'Легендарная «Монополия» теперь стала еще лучше! В обновленной версии этой всемирно известной игры появилось множество нововведений, которые делают игровой процесс намного более увлекательным.',
                price: {
                    value: '2427',
                    discount: '5',
                    base: '2555'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mf5fIUVrEGur5HjtMJON2DPzo6gIbvRvEc1oH0fqCGV5KDqhHJtvwpXBvDmzCCmVNP6CjbmmfmzeAG_4IqPEPWsj-G7D3oYEoNr74aV6s9ixsprsrOCdKFvsoehcCMSfR0HQNnW33kYI5Xu4fK-e_yrvjJlp0T-LY1MT5W5abBESjZrDGTqdTVfoWgPvXXu0NRxtwSU87QgVHpomcO9tCAHoS7DOOf_nt1O_TLkX-OrD-wMgivCLRuIKrliEZ9YYWy5Mcxe9H6Jrf0H0-8pKbzT9TxDvymi23HTHbSR6QMq83EQIvNHcT1GFQRsheY4NL1YeFVp_fwIpZBkjy_LaNMK3im3kitptTWk1joVifXSOvV_Dz6cUFwQljsanWoVZANGOauqTk3wxsPgjtOHZ8oflz4K2vA8qJU83NUQgpVIgJQQHKpBXQv50dWu0z0fOcifpplxRsmv7m_FOQY0e46OckYZtfj97pvDoTMnqgIZKaZw-LVl83XclFOxSf0MMg3cC__xdqCMIyYocBk3I8UfZ99X_a5t-uE4r2l97nULWWEMqurLA0SNJA9FR-HIXJIyzmVFzDnhsOzCBC8-0zCy3v8lFvAWmX7MiDjeZmNke5sVw5C6uEkRZCnNRJx31YVE8ac6N2OS0NBz9SJTR1g0BC5ZNABQY18GfPC9iaGDnAd7jhi12G5PrpGLWu3rX1Qx-A41wdWsZN-bKVYfDJeu4-8osPJBj2dm-ZsFQ71hRPRggavs9Oq4hPxRZxi5GN5tGsc4W7I_kqhV_FCoNTXwULQzwRf2cfkErPWdE01vLq6-XfkOP54eMhR6VFeZL2psCfKCZWUwtC06tTi_bTe8JvdkLHkQIVikem2WZfpcs42uYc2EOqZA,?data=QVyKqSPyGQwwaFPWqjjgNtHye4F6PH3UF7xUEaF_Js1KoIepvH-qq3bp0IdS74QEk8MqItmGk_3MP_c5CP_pMtXmIkutlpkXOXrgPIMEyoyhSd2CyklEaFiKibcpCpg11Xc3do_JdcVZvpavSbDDvyEh1030TatpBEfX8r_wSmMj_M_zEaf_0EM3XFKcNd2B7CIC7N1USbzSdBmPPQPWtv28mczwlwUQkM5HO9GT1K0o2hLhANEx9g,,&b64e=1&sign=c7e47627384831750c49b8db204c6a2c&keyno=1',
                directUrl: 'http://www.hitplaza.ru/product/nastolnaya-igra-monopoliya-s-bankovskimi-kartami-novaya-versiya-b6677-hasbro-/',
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
                        value: 3,
                        count: 134,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 12,
                                percent: 9
                            },
                            {
                                value: 2,
                                count: 4,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 0,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 6,
                                percent: 4
                            },
                            {
                                value: 5,
                                count: 112,
                                percent: 84
                            }
                        ]
                    },
                    id: 253862,
                    name: 'ХИТ Плаза',
                    domain: 'hitplaza.ru',
                    registered: '2014-10-06',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/253862/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                photo: {
                    width: 600,
                    height: 413,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/247272/market_r-7sB9PwtS4U0vWJMY55Gw/orig'
                },
                delivery: {
                    price: {
                        value: '270'
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
                    brief: 'в Москву — 270 руб.',
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
                                    value: '270'
                                },
                                daysFrom: 1,
                                daysTo: 3
                            },
                            brief: '1-3 дня'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/jfot7wdt98P0DY9N-loI1Q?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=NMmaZHnqMRM2n4YnvHQjsa8weGjeMy0SzxxVg-8_Rrkx2BJSGEBKunuRJfiaC5RNyYX89-UDzXH-ngA3jSvjWU47pDqlNZfuTqJelKotcYMUXgitZQiLsHujsq9BvY7v&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 600,
                        height: 413,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/247272/market_r-7sB9PwtS4U0vWJMY55Gw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 130,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/247272/market_r-7sB9PwtS4U0vWJMY55Gw/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZGeq39Z6hbcf2fg0rtAo90Xq_giqSi9lFnKuA3G5B18CA',
                wareMd5: 'i1UgMg2RzXP2MovvPnO9gw',
                name: 'Настольная игра Монополия с банковскими картами (обновленная) -русская версия - HASBRO - B6677121',
                description: '',
                price: {
                    value: '2345'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8medeQi9byJrN0U10jwcsSTilVN34w7q8tsXIdCSDEmXOG-TVA0vS046aEkjlOaezhDU8YmRbNdD1WNWzcBPSIr4Pe4lAgD-Nx884BkSKmPVO2IU0Bujv9JPf10ol6OfRvBgY2IJWbOBd3KDqFPlv79BlzuHY5cbTP3BDIxX2sqox6_6A-edJizlCGKIJ4YETRq4qMXhZaZartNvO3J7U_IL_ddANZwM9vYT4bCz_xb0zt1k23UWVnMp9Crn9J0iVvxSNC6IKiCtPD4RlRoKj0I4Mx6Epq4JtqVp79vYyJxoi8ncnH1Sts-bTA99ZVuJvDd1KcwmfttsjYLZ4QoV8JnEgrVjxfSY9514TxdsJTT9TrrtiG0EFHQY9582kwjzy9AyJe8pxhJIblf4wBW9sV-ZWeoSWCM06qrfUQHc5vYe_1ngNauKGLLk9uMMGCt9nJ2WJAWPvUJfuDx56FaH2kK_dvAsNXgSmGG4W2JFgaolJJr9yb488pLUfuPPzIo0DRk-w0JSoq4sPodrblSxjAQHkS8W1NeLAUtF5zFFW6f2haG82oUvwwD_g5jtVSQtg6PmdR1qwqOO31rgrs7eJ-8MyN4oxAPV1ZBurJw__U7alGWTDx0eXX-034gZRd2tC-OKQ0DG2LG-80Kx3gkz_ehTq8cQDDXty9r0BDulT4ZmXwOVIqyTCl8WZfTE0qBZb1AOWbWRziuMvyhJjoT3xM3nH5C1bO8u8VIKOdBaoLbXcQBx5ORYJ7EMar_-K7mjugKPJoj7tAiJCmOzi-XGoEjbRQy56lc00MA3I6sMg7E1HLMOhduosS5eV9hpIo5fqWY84cXGtUpAq5a07WhbhjKpX8nj6Xg4F2K150R4BGlEb9M2u6f8rips,?data=QVyKqSPyGQwwaFPWqjjgNkcldWRYAE0top9a3HzjhYJNCShQG1bFhUnb_w-LhFV5jB9UZfqpZGQBsrNeeDkSIgY12YCsn6PRtafv33cKDeEiE9x2XtmtvhD5FN89bia6TVlSzGxqrCtzHHH_sEuJC2Y46zxKreNn0Zith32nM_ab2MWcnlIKsF7w-3LiJyaZ2lqdnlzJD6MypgecfrMtZa3MDlPi1SBebKFR2VTCgjnZ-uJMsewt_k7KLUFxMh9kqba-um_ntaRZgPt2jOcdCA,,&b64e=1&sign=d416ee3b9e6e375e1335a3b88c20d7c4&keyno=1',
                directUrl: 'http://o-toy.ru/nastolnaya-igra-monopoliya-s-bankovskimi-kartami-obnovlennaya',
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
                        value: 3,
                        count: 65,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 1,
                                percent: 2
                            },
                            {
                                value: 2,
                                count: 0,
                                percent: 0
                            },
                            {
                                value: 3,
                                count: 1,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 5,
                                percent: 8
                            },
                            {
                                value: 5,
                                count: 58,
                                percent: 89
                            }
                        ]
                    },
                    id: 398232,
                    name: 'O-TOY.ru',
                    domain: 'o-toy.ru',
                    registered: '2017-01-26',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, ул. Народного Ополчения, дом 34, строение 1, Офис 14, звонить за час, прием посетителей с 16:00 до 19:00, 123154',
                    opinionUrl: 'https://market.yandex.ru/shop/398232/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                onStock: true,
                phone: {
                    number: '8 499 110-34-66',
                    sanitized: '84991103466',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8medeQi9byJrN0U10jwcsSTilVN34w7q8tsXIdCSDEmXOG-TVA0vS046aEkjlOaezhDU8YmRbNdD1WNWzcBPSIr4Pe4lAgD-Nx884BkSKmPVO2IU0Bujv9JPf10ol6OfRvBgY2IJWbOBd3KDqFPlv79BlzuHY5cbTP3BDIxX2sqox6_6A-edJizlYUlEQd1upqzu_dq1jyd_gOUoQ_ZR7u7gT5JTI6DqytIywtKt9XCF_cqWAbHIuFqYRC3U6iMdENqFTn6o9sad_aY-msqeMx5LC_WP0IkAPgeSgXY1o-fJe_ESB0OTN7QwOVC_kforuBSQaam_-fXWdCsWmLD_AgN3zh3dv7eD8xmh5Z55pS20bU2fFMBojsOIUz8UZfk0EKBtbZgDTv_cu1amkrYedaYibwiBzHxp43zZHQgO2wQRRxsBVAoQ1b-BN1UyNa9fbgWd_9pzIFgPmsSmOkgVnxm6NCKk4yW6erh679GmtsA-vFo9UIYfcbAxlXyfjit7Vsfmvwkuly-9Phg_X_Gf-fpnQNcYDgq77_ySN5WFugT-_E31n9l-y6BD7dL1KS0y4qv7QIYjrAzLUBvzwS_tTFfDo7zp4R8ZyWwgsPzArf38A6BSu0aXKgrT0VmvaEQUWIEIhS8muTavOxyX111LsR08CWiBmXf_4ymkXHAFt2s4pkglIHw9CRGSkObgoYOPCEHGYRH_JKaaik9yAr4KV3aujGPZjWwMH1HC09L5AKPFoGfx7TmJTN3IvPfhbpqKDA849t7oNkFBWmQ1Fk52XaVGCVpg_-GaNE-GtM31-oWLuUZH1eZAfO52RTvKp8qmr5FH3h2e7EvHCE8D4W3XOEfz4F4D_20arf7AOfuHhx8iNW0gdx6cF37c,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-5HgZ5yoKpBmrON2OpKEvZuv1_uO5qpvgOy5rv0EMXKCTlaKJ7oN4jJrPgvk98bHGd_teAyR1CfV23C5P3H6TjyFQLyZu5Lrj7yiH6VrSYNWKAsU8-yIT1Qn4EC3HnxK7uByPZ9RVOyCDhyFt7Sp0sIedKqrV4REqEcMg-4BCMuXj6zPebPARD&b64e=1&sign=14d432b8c6eaab242ab89c19815cab74&keyno=1'
                },
                photo: {
                    width: 1200,
                    height: 853,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_MUKKOQdGhQImqX6BUx7IPw/orig'
                },
                delivery: {
                    price: {
                        value: '290'
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
                    brief: 'в Москву — 290 руб.',
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
                                daysTo: 2
                            },
                            brief: '2&nbsp;дня'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/i1UgMg2RzXP2MovvPnO9gw?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=e-rAPHg5WPRc7FksTt2UGA44P5dTcOxOxYWzrZiUm0oGE8zlyRG5DSp1SEAYg9bpcrY7oeCAV62mWM_Nvv1DcNJzaxhV58bioaC7XkVZG_I6AVsHaeYBLn1gjxdfgdHX&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 1200,
                        height: 853,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_MUKKOQdGhQImqX6BUx7IPw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 135,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_MUKKOQdGhQImqX6BUx7IPw/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFztdPyCAhDCwOf0Q6n_G-9Eq-a6iDN5Y5BsGbZmUZrmQ',
                wareMd5: 'dZKxN6JsZg55Pir6OpdR0Q',
                name: 'Настольная игра настольная игра \"Монополия с банковскими картами\" (новая версия) (Hasbro Игры B6677)',
                description: 'Представляем вашему вниманию новую версию настольной игры \"Монополия с банковскими картами\" от бренда Hasbro!в обновленной версии игры Monopoly E-banking, как и в первоначально \"Монополии с банковскими карточками\" вы сможете использовать для совершения каких-либо экономических манипуляций банковские картыДля удобства в коробке с игрой вы найдете и специальный терминал, который поможет вам отчислять и получать деньги за совершение каких-либо покупокТовар поступит в продажу в самое ближайшее время!',
                price: {
                    value: '3079'
                },
                promocode: true,
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Vt2-BSM2AAXnd4Y714v0C1Av0Avy9OVPaqEcrTNUt-9Xp9L3-cAcnmzaLDUiYPxlOIfnWIvUISwQs1Cewzo1NmbyGEwh6zXhNnB351L-Pg51xsiMv3AlFyNoG9eQP54ywEESlAzUMTh5qLSdsdvw952n_hAWD5ECtp1pAoFdlMGHps6GGfXXISCP73VPjuNRbHhuX5uHuzr4udi5A0GtPBphwBd5cIaFebaO7Py4PY-rJMpNnaCgwX_wo8U1CPFsTrjadyBHF81YFu90ZvdDLx69-fc-RQDq3fpJk_rBxDp-UQERck1SJQKGxPGcesUR9oJpTXeFttNALvDO2_mCBAi2K020ZWJkCY4-Eq0i9KTqml3-2tZwxeVvRQS3wpATQFV8V3IZR_Zf04qfMDAQCLnaqFuS1c-OBRVpr_I_M5KUBIikMi-8t3pb6MUcGZ88OMgghzmdJOxhW1Tg7JpML2EHHkdaEbtxx4yVQWiEY6nsMk75YmK3Sxre1IDOvJOZ9ikvdaScOfReU7Nm47fskQ7taJmDPbZkqkTi5ARoCybPAM9l_r0-qZ0NZTHAG-PMy7xLUS7IjcUjIx8_lgxn_h2jwbojZuiiPi04ONtThaU5msgT4fSbXSBMtc8mHXF2MNHr_sWao05T1i74EFpvn__DpEqUnoVPOjiXMdfMJ4ynzJkkmVeCfBrSJ6-7eBZWFVx95ExW7zZ?data=QVyKqSPyGQwNvdoowNEPjT1rTd_mg6_jLqGUgfPI0BZlM_LB4VIRcRIAizBsb6YjKES3bHYuVtS2T59tF_Cq41aDz5B58B2k5n85XXXkzwV3i3_U9XrFOGfXfklOIpWAyZvinYQimvfYTCo1rBkHv6vz6SmmSjjyYZ_CMgFQSqpYtkO_Czt_kIocHSXWhP6eIhW64ngbAuRgUzz4wwf3abJXrSQ90klVy6DscFQeK98,&b64e=1&sign=dc199dfca1f65d653bb304274e2e7568&keyno=1',
                directUrl: 'https://toy.babybrick.ru/hasbroigry-b6677.html',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRtNkb9mq-LhCKNVQTD-xsfM-KNfCH_RRx2kCH_dAQxJD_s_lxFXvmsYTX-By-kQZuSAXVSe2_iOcOia8iKYPHcBuADXkuLVN6yHDR80vbE5fM_K_lQ2155wmLfIj9ZQ5qE_5Vu0d-9zvH6VYbSUQlloMjNDx5yit4hvFgyhcDh2Za8VqWstE2NqhRUNTfpVsrguaybBY_8vNdMEldBMrdv3X5NqZ_rgo7W0rD4hcEZpgbDuaRXk3auJSTkQQXS4LBxQkyles_W9KtElyHV7KPc8qAAl-xTu-JRqzbqmVAM2UKMoPwbNnxgNI7qdGMp2ecLv0sftBC3vLRwfRY1f-qJgC9gfPAtjF4xeMBhaI7Rj1r_MRfty-kXK7Rj6BCypnD2A9DdXkN3-BI426d9jZFFEPYCmDRLvSzs7MorBX4ZW6sOTuVdPYvhKLGSjtZyvCJ-OLvtJwD-AWeXbUA2M-zxxvaURn5UFaVJaSpbEC34MgPQCQ-7hz64ZW_zrnsvurR6hbuWDMZkeyYo3IEnGFGQRyXbqMpnjnwC93ikQWWxTkxszQWxMufkN1qcj3o0NwgNqEWPP8yghOg6WQ4OP9QBCUSJFCa9Cr_W1SVqToYORkfHDoaKBvxPvlU-24CxFuTVocEYNak42A1ZHy_nwoSm6qysccRYA6hr4Dzs5jhsAk37pMipXsSHQ0jvFkl0k2ale50ZDbC_THp-2BhAqEqqlz9Gy530-ZOwYaQGoJBeDTgleTsgapcaSfKWsubmbjhPMczswHih248kZAJRyuQt0lQ64qExPxHHkQybk-v38OYnZaVlqmyZuQuMZTlRE5cdMx8B2OBWk5L24UMhjPyR8Qp7m4aLP77cSfK-aUOnQt5d4uDncpgOO?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XjiKYHZ-MkWgx5AbSx4OOHbILmaRwItD_bnFpHoIQj4AovWHBbvaYOu2-CXinao96ILE1BEfb2mD4Ppa5H-36a2sx0EW4xLmRySZJuGUtxZikF-w43qC8s,&b64e=1&sign=21f269a9c59961c2adee0b2bb77c2e01&keyno=1',
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
                        count: 300,
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
                                count: 1,
                                percent: 0
                            },
                            {
                                value: 3,
                                count: 2,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 12,
                                percent: 4
                            },
                            {
                                value: 5,
                                count: 281,
                                percent: 94
                            }
                        ]
                    },
                    id: 359282,
                    name: 'BabyBrick.ru',
                    domain: 'babybrick.ru',
                    registered: '2016-05-23',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Спартаковская ул, дом 19, строение 3, Russian Federation, кв. 60, 105066',
                    opinionUrl: 'https://market.yandex.ru/shop/359282/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                photo: {
                    width: 225,
                    height: 169,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/168879/market_GVqlSK8hkCUom1NNqGJ-fw/orig'
                },
                delivery: {
                    price: {
                        value: '250'
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
                    brief: 'в Москву — 250 руб., возможен самовывоз',
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
                                    value: '250'
                                },
                                daysFrom: 2,
                                daysTo: 4
                            },
                            brief: '2-4 дня'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/dZKxN6JsZg55Pir6OpdR0Q?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=e-rAPHg5WPTCTHhY8AyvxnJ2nBBeJJWuED4qL4DYCyqVOAtdgDVW98xgWPo-r2TkHDvOewWEVey_hnfjRx14sP3LkvHP8IfL9u_71EKMjyGBgK6hkDbFG_KhJsyXpYxK&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 225,
                        height: 169,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168879/market_GVqlSK8hkCUom1NNqGJ-fw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 142,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168879/market_GVqlSK8hkCUom1NNqGJ-fw/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFEwnsFeUmHJxUZjMfY3NVJMNtXzK3tSI5uMGk4BQbQNw',
                wareMd5: 'eXPpBk4nR-Vew4lzhYvpjA',
                name: 'Монополия Hasbro с банковскими картами (обновленная) 6677',
                description: '',
                price: {
                    value: '2449'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8me5oPYhUniLkx6gtjr5GxnzmTwNdoWHNTJ9jwcTXSmwvZuaa70flluvk3vw6RvCTBh1PNsmsF7L3ZxFEbU1JCEEImx627aWreL-__ugsGRfQsphQ4ZaV5-FqKZnTsif_KvnvEvDrV6DQDiRqeda-CaqnEJR9nJtPtPI6r0Gnkv_Cz5Fvt_ZVe8FCLVTQFnvmVFpAY9FtZHNPEQUIerwSsy_71ug_xUAHsHOTgYZWCJ64SUnXRU9n1cXclAxBPLqAqgZhzOK9Z2kvB2LV5-OCZc_qwhetbyc1bN2aI6m53J0cLiJwAf-3T6sde8162YLqHy6P_g0joOtga8FTOwtOHDaels1WpnZHsvfme6N6dQopfs9mvAN6LkscAmhOWDkFqhLOR53l88nYlMNWKwBKvNZryVQWFaieu1IN4CkJmkXM6MGnXK8TICRBRtE6HOQApm0CoLw7cesqvSiYsaXAYYIpF0L0z6hctRybxMXLQo88VRq3olhRP5ubuMnkwkLJZW8sAd2YVQzT7Sxmj-Mr5TkHzcGKKMFS-1rITs0h4NPjKdeRYxq23-YyKx5K0yYS51RHbBSQifh6zrXxEBFDt2tPpMsN-8QwA8QJnBoaBL0zvWkZ88L5fwL_9ez88BTmU8YAgkYtTsGJuCuclKlAbL_Wip7SBMxY5gaxNum0Qa9rc3Fwe-nm_wmZOvUE6WJXrMlLmJpFSw8YHmtUwkI1uruuJgkAflieHicOP3WbltLIsIw9GeBh37HjLlyOfuGcjgsNKVzdbfIMpCB40z_4nGEPMWJNjciuhI84F_R6NGykzIIqH05jwUefGn-lrHTbs--3sXQfSDM6ybVRMUb9Gk_V0pu0w8J-QX1b7ILO-bhn?data=QVyKqSPyGQwwaFPWqjjgNtydMHNnehHqr9s3lnVzTSgjShofJ3piqhKgRrA-1gzNVzjt_kWQ1vLzqxSbNUoWTrrc90L5uUCrK6aaG2ReI1XzwtvGjhtSMXVSNyC8dV6lXfKaT_zlx3Yl1ImPcKK0_qsUPaxDkyhUFoKnxKGFxKdr-G0RZZxFi7fyjnku9VXZkrDHX2QU__D8oAMu8YenJr_wzS0vzLiuHd_XqIsoyrenVecZUbXcvRZJy2IFVwxdsVT3D5r1-ST5a-Ga-GxzFOltVqsJKika7fN_ZyPoYSlgthjHQalP084_yjaCSF-S_gpbtBwEaf4XVwEybJynZqpkTej_lvWw5YDIQuQFVoFX17vfnPoYun0sr82MeESBhmDlTifoiROKqFgcGHwUMSPjl-shuUS7IYXzGG1gib2Grf24eKnHeQMtF6kfgdNPxTlIhpFhFggfyM-BdV4w7qzMqhgl9vKmqxDGF0kVs0OgCN4hC4ugtZnzkzoZqtv5rZmvrFs73hEV5AtkpRTB7_bcK6ckbHS06ypRnXxjP4mRSXk6TdouRhxCX6qbm-lnGO-7x_ogqjPXl7VRqe_yDWrP61mysW8zd9Trd0za5lImM_x0gvK_MvNMJZObZJaFvzsyMQRKs1ND_YgXjs91EipXCEdqFBKXXtaYUsfmjcXcLO_VbVhz--ToT4yyNpp4_r9xI-WC3xgedgPUZnmKLw,,&b64e=1&sign=0fe169a20a782779ea408d0c78243d6a&keyno=1',
                directUrl: 'http://www.techport.ru/katalog/dlja-kompanii-i-semi/monopolija-hasbro-s-bankovskimi-kartami?rc=ym&city=MSK&utm_source=market.yandex.ru&utm_medium=referral&utm_term=589913&utm_campaign=marketMSK',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRvVL8634LDQ-YyFgwt67O1EPH4EWc8ZuSt3iTitRRluA98VPFNM6QjBjTI0_dX9NFRqI0dZ017pJT4yR-gikgYkmY94pUdA2JfkpUn3I_K-28UnMV7FOyupsYg-w3ELRSsARvsd6vQ5PqJ6uHwyxjIAZ5YJ2I4CsPiqMZMeyZVlP5_A3gU8pziYmhDqq8Cy9d0x9VEtSYhfYCyiZD815LvMHu-6HDo8Yon0kc_4w_Ksh0loeyyv8eQqaIldxy3Bk8gamtZQoYHRH1m4B9zpTCL936ZUMtjBYYWErK0H8zA4uRUu5-lWBzpmWlfZtYXFIOvjH5REZUB4UgEn7TNnzZ_X--CJ0Dus_iQI0XkLoxE5ky_TW0iy87ym9-vaHSsaH92ZM5PIrOQoHLklm--8LVXZbOWQdjZS6da3uUniW0jCuv595nUENrJVAcrJrkO1lm4C4WrfSLF9T9cn2H2_WMpeiJHORYfMKQbsp6sOe7XIf0XDaVK_Sd4dXfsDvLlFGO9_sDZAercnE8ph5eKncqfQkbSlGQHN1U_OhHsK08hnJ-hGudSP5laAbOXkXHJrpEXZgb5_USLEVPcKKKtBuczBVJO6wLcLcmFYplLlEn0GIMh5ir5cLPDi4mqsrz5v6sv2gm0PbqZ5XDSiguB5V7qZRdILlBj5vRl8TYYE3z9T50bswEDHhY5bSwznfb0bk23kyXbuS17wTU3YGuF523TIX5dXfXbmsVaCg0G2a3CItQdoIN4jijmal5KdFUk4efQ1nKdj1wK0CYYEfuvuVeYfyRG9g4VNHPYnEeVsqHDriDqb0LLD-CNmVotZcHYWmMmVI_BbwMxbhjRRNvmTudYtG12YSlq4NO9XfsUgKDzguwk3Uz0Fzyj2?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2fIAj2sHvgAKZQ_XhZry-kT2wISPjY9p1xqEF57zJzQdasr0gKFjiU2wc5Qe0uVWRPtqS-iNvoTI4DJ_nXW3a8D1-zCZPWNB_07HJU5w_BPEcT77pOmjnXA,&b64e=1&sign=63ce4a000f8d406eb0867467642a0cb2&keyno=1',
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
                        count: 54760,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 4795,
                                percent: 9
                            },
                            {
                                value: 2,
                                count: 1020,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 895,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 5196,
                                percent: 9
                            },
                            {
                                value: 5,
                                count: 42854,
                                percent: 78
                            }
                        ]
                    },
                    id: 1672,
                    name: 'TechPort.ru',
                    domain: 'techport.ru',
                    registered: '2005-06-28',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Ленинская Слобода, дом 26, строение 2, ТЦ \"ОранжПарк\" (5 мин. от метро), ежедневно с 11 до 21 часов 8 (495) 258-81-30, 115280',
                    opinionUrl: 'https://market.yandex.ru/shop/1672/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                phone: {
                    number: '+7 (495) 228-66-69',
                    sanitized: '+74952286669',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8me5oPYhUniLkx6gtjr5GxnzmTwNdoWHNTJ9jwcTXSmwvZuaa70flluvk3vw6RvCTBh1PNsmsF7L3ZxFEbU1JCEEImx627aWreL-__ugsGRfQsphQ4ZaV5-FqKZnTsif_KvnvEvDrV6DQDiRqeda-CaqnEJR9nJtPtPI6r0Gnkv_Cz5Fvt_ZVe8FTCSTAkUN-Kjn2TMKOMY2FbQxeYkfBtJMrAqER6nPHPmsI6cfDWXCKrGJ-othVCbREholMFJ7zNhFyDjgz3BVZHdqOAKsDXFqILQ9AEMlQlFZZ0CWzmTOeUUh0gr0jlpH8b6RCzX9VxlwurCmA47K547kAYa-6uwkdWl8XaCDBLX38Z-A69J9qagmikiYiyo3Qz1tNu2iCeQreF_dELDnj6XIBD_stUpgqzw8X6kxkRXEnPqfwXhIz55KeeOoRl07B7rrQwtb6mPbMcjfOQMJw4bMzTv304V2gYzE730lSRVgMbJn7-6XsDBPGCXDgy_MWLjk2WFVCuylGgV0-pSRQqhfOMR2xQpkuAWcBWz4n4GZFFk42l5pedrr_jSiJBN0QWoE08Z92z_bK4IflkHDBToTWTBPo8kEZcXEt-TFdPStc4eOzTQDNuNjg0k1b28CmZJmDFshFX4Bn181mNSoQqYs406tOEjVAl98qeQ8lfUNnDce5Xe-PGizcPTsAXl8cWjsR_GOX1wRE5WO1GSQpLOb5N6rvBaOvfQMWgaZ5KXQnJefCc3ZjQsXL-HiHbpRwQDXTnwN-7ElJS1GAjNbX26CvNvYnO7slv8hBkXrtIcyKG6NdmmPOZcF_vntk67HU5rcgGp5mlCKNJlxv8m90NsnJzc_JMMK1L0jcZsucjlubzPLjvOes?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8uoaKNyzfQTQ6874-0NlPNBFd04elsKBeKU4FcDqHRe0qbARR3ez3FCD1CRr8LHdMPaCoKaE-QlMbTy8TnO9gCUOoGNX4YiLETu4BUes3GQnAKrAkgLiyc0kiadP_7GlnLW6DVM2M4b5GtFRSE0mRZ85ORDk8r_LC9vhu9fpwtGg,,&b64e=1&sign=6a2b1075e24384c25f26248b6fa71266&keyno=1'
                },
                photo: {
                    width: 300,
                    height: 300,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/235547/market_NGXnossw-d3Tlj7BGSJ4SQ/orig'
                },
                delivery: {
                    price: {
                        value: '250'
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
                    brief: 'в Москву — 250 руб., возможен самовывоз',
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
                                    value: '250'
                                },
                                daysFrom: 2,
                                daysTo: 3
                            },
                            brief: '2-3 дня'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/eXPpBk4nR-Vew4lzhYvpjA?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=e-rAPHg5WPQitvbz1QxhgOEv7cW5a0Jnwd1qzcKrA0U7osJ2ArshujMR-Qbaqx6bBew6PE1o-is4pquAemssBeqD9e7rSZbCXIIVpE7MtNkr84tgUZAwdnQq_xFZrxIX&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 300,
                        height: 300,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/235547/market_NGXnossw-d3Tlj7BGSJ4SQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/235547/market_NGXnossw-d3Tlj7BGSJ4SQ/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEvKAPQUeEgWNLX55qcfkpldUwZS9UzeeiPzZ8LxsD1DBoQYYhDU8DdxjoTx7yvaorYxmb9TJmPqygYK08lxllijRavS7Xv9kJXaU1BqTwciQD5hpRPe1ZoGXOZjrQFy0ex0kxVWzFpeMQSKqx8z8mOalcRCiWcnUx5UFDMAKaK46lmW4T0YWG-0IuV6r8abBaC5yaX2Wtgck3nxcDfutBr4uJNDxK-tlh1QEVnokUbNZSUjSMFztWV_4RkHd_CW8VkaSuIzjKfNEhtivqgTB8lW8QW4hjkQvY',
                wareMd5: 'LzrQ-DSHdPqWR7uoROnrWA',
                name: 'HASBRO Настольная игра монополия b 6677 с банковскими картами обновленная',
                description: 'Обновленная игра Monopoly E-Banking Артикул: B6677 Бренд: Hasbro Возраст: от 8 лет Для мальчиков и девочек',
                price: {
                    value: '2452'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mbgORdUsQ9WNDz7jF7kYPNvMX9t45rEk9cyur5b8WNB0hFJqkLSyLFyz4l2jiSun8HOlClkffjR_wG5tuyP4HpZHdUQkIp-YnV35VCaMK9J5w6ZAX2eb5k5O-Jnuc7EVCY9beLQ1RSi4qty6b7vUw0JTw01j-Q-zhCcqRsMO3hLTD11QgcOCL_XoAwlbegDegeSVNL_kOsrAqw7YWJdyxHpOVLife_oO4E8hZvjO3VAtNzYfxmzO77ONm0jXpue2czj7AlXvq8sPOCO1fBoJtQpjzKe99AYLmx2WnhpYYF9OItWXbpwikCn85EkniscFB-4GbIfS-avsaE4jkYpmkSqKgb2WJFtgs-JKQjasYwyvwI1kp0G-z8eSLWmLesp_Cqonlc0UT6XfnzeZwYAeamup28sh6N3V7Dkq2o3E_MWmZCJpksPB1VTXsxstuk86Efmw56MXSAiFeg3MTekoGbc_-V_D2tI-7aD5EOcp_fAR4X25aIdy66Af64TaNCQEj5KfWnUwmKNmc1bnOXiloBR9NjKdoL8-f2LCxMWWRSoFgsGi11YxnW5HjiYfuxS_eWsnoGMoASZK41lYN9om9SMUGQ0TnKFAVKsU-hjUh4EKzInA3gKGxdD22ngaspdyP9jn1KHE59J4MlaUbsZo9__oZdjSt2Rd_KxYFZh-UASwNbNikUhxffwOjyTtmsXU-61AOF1NxHfjpN-4LyS4O5Yq5PbDny1i2Sor1F6ZwRoeAXKuOgZfBxFPMFrugmjCO4UFyzkUMOzu3_7XP6ZOfmn672O_WPj5LLKhepI6yszrjX8wEBbil6-gwVK_fBga3XSlyLplC51S0yb9sXlz80psUToXTBLsyQBnmAo83Z-d?data=QVyKqSPyGQwwaFPWqjjgNjnwQl5o6nBcWP6_wdMiM76pL8bA2jMgb0tDxSSQfYpVujvCwSazZX-IslPgsCUnCT6k91fxb2mjMRv3UX2ZAXCBd4mlPAxKIqUZtxdebOFCdxnmRbrb0ihSUequY2j7zoRd76vsMcnRMp2_GZMzar27VXW_VpD7Hre_4dNb4V0K1xLk9NJ0ZUV11ralHrjWEuMOmPg4rlkDYFD4J-8mawjPtyPxeSD7NzVNlajP4has72G933iQT4DYa0GNOU4GvsaHTZ0wBDvsIFMz0xvsG234I70gdn7ArWUut8PC83RczBFeFJrHqPklCENH1geMyDahYAMJQZDj9uHmDhRTxwCGBZBfcV00JwI7vLzrPk6G423uSKkUZTvSMC-FLR1GTIVgd1EN68WiMKXDNgBCOs-WIQ5A2uq8-LS_hH3UVlFDyK_-qAu1P4wp6XvBESIFNoAayoP8WlPXYPo6I52pnYJ2KrsDN6L2A2VESrscD_8SCslnkiK1wPs,&b64e=1&sign=30160e10d92e621c7aab293cb13043db&keyno=1',
                directUrl: 'http://electroklad.ru/nastolnaia-igra-monopoliia-s-bankovskimi-kartami-no-81849.html',
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
                        count: 11377,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 1587,
                                percent: 14
                            },
                            {
                                value: 2,
                                count: 298,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 271,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 954,
                                percent: 8
                            },
                            {
                                value: 5,
                                count: 8267,
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
                    id: 1730875803
                },
                onStock: true,
                phone: {
                    number: '+7 495 191-89-09',
                    sanitized: '+74951918909',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mbgORdUsQ9WNDz7jF7kYPNvMX9t45rEk9cyur5b8WNB0hFJqkLSyLFyz4l2jiSun8HOlClkffjR_wG5tuyP4HpZHdUQkIp-YnV35VCaMK9J5w6ZAX2eb5k5O-Jnuc7EVCY9beLQ1RSi4qty6b7vUw0JTw01j-Q-zhCcqRsMO3hLTD11QgcOCL_UvEayZZCLsN4duHeJMHsOM84_25qdhyrjav1yPbOLCZmMma52F7Mdiz48i7EKL9WzVH0cvXHeqY6d2O9wdgxeIlQFfcrFOiuYVk9gM4_-lH3IAv3tNYZk0yAHLqk69JL1Ku7oi3D1VflKGPzKtPszshW8FeOm9tOLY9JSLNlBsppPnpqbQbq-Oc-aGVrYJR1NM3DvtQ6kpdM79uWNc_V_IocmWLhlho7OqOULfzpH9EM1FURzgqvNB01hY9zcU394JKL2weJXkOw_wITQQBxECt4bTvhyO0KRZvPpK0t0McobLvd-xNviE9Te75UVguAX_thjuAFXE7KXZ8YsWPucRbPgnl--Ru32bdJ-ciW0L0gcQnqWpVgXXSTLH-8YxKsmuTG7AvPe0f-zTZDKfjGN0G8C-B8u6I8NN_ZrGPTQkkdtALFu_pc-Fut2_UDhJIM_xsx1CR7kmqBg8SnyMFLKdx-g3aAmt8vadATM5TqgZUqKo6F7m3D6ElCSzV8EfeAF5KemnAr9MOaic4_KKsTn9b2OFWTHN3Qn33eL0fuaSah6ygO-4XkOE7jvuUugLtYthgNdOBBfZkOO-RkWhh9syKjcApWe51cndq6Oc09PlGdUxHhVd9MsTLmIPQJ9G-u1wBoKHgM1Wja5nTGM8Z4-fR_kvKE3fzi6I-ZwrWnJ-EVhVwkJk_bEX?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_x3ofEJHmTDlX3dpX0rWwvkzCoHQa7eLUNq304588Om4voYeZAJtsPc4JAWYmDz4HMCuUbE3PnK3Ot7pOKgGxXKyD9RspqiDEPQzULdL_8P14KHWq5cSkfBAs1EmRnHgx9raSakeI4CAjBPSvoVg1DtMoI6LFMX5p3-cY5t40cTA,,&b64e=1&sign=b68a1392def0f0bfae89c9216d89e8b8&keyno=1'
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/230492/market_asdISi6Ilxg4nwVcUdytPw/orig'
                },
                delivery: {
                    price: {
                        value: '250'
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
                    brief: 'в Москву — 250 руб.',
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
                                    value: '250'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/LzrQ-DSHdPqWR7uoROnrWA?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=e-rAPHg5WPQLElhUbLhDXoVI8Cl6WSr3EIlvQk9vIeQ22DUqC_oSZBN3heAUbVu6hPhWgRBcIhmHScdpi8M_Wx2Ny9sHaQZPgV4mPH675vI-DkBxOkjC_J1MoEQe4IX-&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/230492/market_asdISi6Ilxg4nwVcUdytPw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/230492/market_asdISi6Ilxg4nwVcUdytPw/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZE2mYNSBviHbuggeSk_v1fNKY8eKo9KRDgCiuPPejC8LWgxqEyqeYs8M28cZ-M5tIxJtvhUHiA_Rpw6bAKpXfBGo9MbNJMwfkVR3zjPnFKzaH00-RtFFYthcCAdkUVlqEwlEwlF8o5quFzjGCYhu09D8CcrxWkVns9eWXDpIrNNIM3AS_0IXZ47Q8wcrn1XOQ0YbHYfHeFt_oTMmaDaqYNKxuqgMQZaQZ42SNw8qtLXdlD73ascKQy7Qmt6U_O5vHGZ5TgcmVNt3qoon7NNV2uNskLMgPrmrvg',
                wareMd5: '39QCk2xf8SQCn5k1akAJcA',
                name: 'Настольная игра Монополия с банковскими картами + батарейки ААА',
                description: 'Покупайте и продавайте недвижимость, стройте дома и отели, собирайте арендную плату, платите налоги... Вас ждёт успех! От классической Монополии эта версия отличается наличием пластиковых карточек и банковского терминала. Все расчёты между игроками ведутся без бумажных «денег».',
                price: {
                    value: '3130'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZ4H9_aAm-SdXUBmN0WL0hnGUTbtOMo7t-jS0Su9XO2QuvjkDGF8rWeokatwcggpmDLthLJNlKtImPfLO_jVKPo4SRvnAKZk4BwLpoSXcNIzqKg3s96GY6LFwPpO22YRD5kjkb7CdOy14js_QlqzMQHWLKz6ydzu8qZnC6x_SU-gipk8LuZ0kANTai5B0jcEV5FnbEa_VO21eMfDAV_B_XrKsGzSEPX2jc3glfWAdiZIUirmqLwZswEuWShiZ0bjeTeecjkwNchtZzNb8JMchsi-cMO9f-_BpBAmlCQX-5LPcGXKa3cqEnHYzMm3Je8-ImkehWg8e4mmrOzlsj7-ZI2K2KNPTYbhv7DnlA-iQFUXMEcVW3hgJ45GEcQQcaaCXjsIlbvP87ivg6rehDdaZ3I0W2ASk_psqkirblhBJaS-3JToBARS1O-QZgU5duZSSulWfLPb10sQUkIHVXJsZNFGNWQBKY1NLOJe_OseScDiCWDGzRQAhRrywkfyECqttd8Awkht_iYkGpI49sys5R--n9uWg23Trs6scHFJiONVVMg8RjxK6MrourscfFVImn8LfRKQ56I0GT3NFKGUFztHjay2WAzfR7nyPazZ0oXWdfD8ey6_MHtNY3WnQQ5Vb8GanKakWbAxmuizoz6POOTBPUdgt207EgLs4zsm2H90J7VG4hJ5lSYCoGIwvwa1Zirtoa7gnxnFtVNH-FuxC_JpsvCamsTn0ldwPWucRvn8tMjri92_j07tVUzCocj8KpkVafRSHsklnzxnU24pJ5tJPTxotZEoDdwBsoHB2-zyrhPb1MTZH-2PRSQoovD2r-vq1V1YjYwpaOtMh-pA-Y0zDtdoOinCQNEzYY-Mk1z7?data=QVyKqSPyGQwNvdoowNEPjWDqp7eX5kyi0uGW3LKi_s3I9FtEbo-WmTpxp7w_pMxlhsjF5uAjdro1qTMS-7jUbHz7sxW5s84qkqZ6fsspp4GV-yU9DgNmTMvHktQLRy99BWnvGti3An65XmYrzyOVFq3LmDPd6NiaFbe2tVSt-X8_Rq9kwM-zdGweUZ3trtDZenCYX1mPA-P08UCwam9rhIQWOD4JgyeHu-UWDIupjKu4i7ZU4V6vj7tkjGNppG0vP47SO4cgR5nKJJkrCEzOadsG79xn7iQrBi-x5rqyUFs8rg_1_zqAl0wq5YdK0dth12jNwPHqA3M4pEf19lagglmfGQf6kU0yTzxG5YCFJW0YXW2DloJPDCTM0OtRRlNAx3FNKK7ez4-ln2AxSQBmq8c-tvj-uGPSus9wAiskICaHWLvrTvuhpseEAwCJPNV7pezKkIhbH1yRdiID0Dy3IPWH_E439OZv_qe0q-qLI3qiVnphadijh9LRuENVai-Ck_SD22ALhOfOyh4EDOpOw63ACGt239NIT6WZmrlUy1gL4fwORoQsBPP2-wUQCTzvdT3CeClsN89im5Tl7BrmCU0BA8RaijJG7_aHwEakOjA,&b64e=1&sign=60fbdfed1a42364292a087678dc3e664&keyno=1',
                directUrl: 'https://www.igroved.ru/games/monopoly/monopoly-russia-bank-cards/?utm_source=yandex&utm_medium=cpc&utm_campaign=market&utm_content=Moscow&utm_term=%D0%9C%D0%BE%D0%BD%D0%BE%D0%BF%D0%BE%D0%BB%D0%B8%D1%8F+%D1%81+%D0%B1%D0%B0%D0%BD%D0%BA.+%D0%BA%D0%B0%D1%80%D1%82%D0%B0%D0%BC%D0%B8',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iaSx5Wl_QyUFFzoN_aeb1zs2Uo-MPCwEYCnHH892TlAju7-dP67xp7lZHgZDvlasDM1A0CKiH6QYPpR2z1h1qtkXt5G9Q-TlIFTVW5gvFffv_ESCqlwmIfXvsypQ4tRqtw1MIvN_BIrr16tVHNDwM_foOz7kTC5cg96hw0_HdExXIHg_TkPBsoqHsIOu1Xkpoawx_dfluIAVI-fSGkHDpohtPTRZld4UeFcEl9YZC9KcDhWFY2HsBxT6xCwMuHNKLFpKOqtU09-IzT7PXUDxneqOQcyF-0H_EG-cU4zZgF9m6TJ7rKGqFGBmtxxDE41SnBJzdDYZQpZhJ49uNgHiG5usl56mpvLAZwcacN2Qg4bjrcT-4VZwajzrWK4-Hezzlh73a7wFbV9EmIXssEo_X7ettYYCVpVTusB2EcA2K8s00ezSggQxeisHFcOTx2c2K8JKVUf4IPXtkbzr913LJLylso_de45WoFaIXRKUpgfBFvJiLkINISwlRuREHaztVdcEc0pGejEKfwscDzXSjQdqqxr_lT4l4ycqPNY3rMNOegKMVB4MF0S5_QqDq9aHwMBnFgQ8ycO_kwELfwap10fu4ZfdSdQiLXvTo-WJLuG5g3OFFVw3rGBAi5aYXQ7QeecRlZCv916sn3Dx4jEamypsdzhaWaBva13kvv9eNX_UgpfMRlvEkeUQ1fFhwa0Svjn8suT6bqaJPiCYCSwlM0Qgiu4E0lfjTG7Q4SxbjsyQtAWI3xiywcfx7U9X8_QM6voh1GDQ7iNnXo7Xs3wqjHa6M9xDeVzmdZ0rfSUu28KpiiV100LXwPlej0rHUBNHs_x4D_M-pAm7AfJcoBZMS8UBx7nLuDFulvhFm99wqwcS?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2V1hZv2BYQ97cKg7it5azr_VbTgiQ-rBm3W5lgSm7tK2DQ0pcDyhRF_L72CFxbMBqURQTnOxBy5JiET1h-vbrqIB1uiaixzaHlg4QTjbk-QMBZS4ZsQh0T4,&b64e=1&sign=1b9f0b3e40d820614ab7d931cbc289a6&keyno=1',
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
                        count: 453,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 7,
                                percent: 2
                            },
                            {
                                value: 2,
                                count: 5,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 3,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 16,
                                percent: 4
                            },
                            {
                                value: 5,
                                count: 422,
                                percent: 93
                            }
                        ]
                    },
                    id: 4014,
                    name: 'Игровед',
                    domain: 'igroved.ru',
                    registered: '2006-12-20',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/4014/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                onStock: true,
                phone: {
                    number: '+7 495 668-06-08',
                    sanitized: '+74956680608',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZ4H9_aAm-SdXUBmN0WL0hnGUTbtOMo7t-jS0Su9XO2QuvjkDGF8rWeokatwcggpmDLthLJNlKtImPfLO_jVKPo4SRvnAKZk4BwLpoSXcNIzqKg3s96GY6LFwPpO22YRD5kjkb7CdOy14js_QlqzMQHWLKz6ydzu8qZnC6x_SU-gipk8LuZ0kAOg2DpwrVVevDSwp8cCAXLD-5-Su77I6sJ1RKtNGQEUIbZwTuu2UlueLX-Zrn2RYoanzT8cqlkpNnOB0_CYHduzz0Rxfo5Tdih7xoFkRiTSF5W9qnEIODlf9jKXxAqLDjSMJcLr6befVuSi0cwnb_ap7idiqXSSlbs0IX_0NabuOBs6xmFr7b0kCo9dmGIpzHFW3E6lR0BF-C2aZuh71fazY7geSBkvgErmw7JT0E9PpwNkCnfRypNcc35mMdg3bVNLjWMD-MZ2Hz-nsU3jLlZhmLJ-lRsvujGd_32b2AOHP_d_QrUCTmdBrEadFJP8rRos2WeLfw8A2Mq0XeQA6SFSm9hhGTxovTAPTonkEbg6_FUPeNyDNddXcKphfaRqoDs6FcJ6s6SiTpUeiLk7TX6hDYmgrMjWg4pMmk7wV0iUnE_kv0bpU5RPaxtgZdDefiH8V4ZLh3aZfEO7Hk5YapZJcl9rP347ZbdmshsFVuQi6Gph1oyKHGnlF3lE-2_mjJBPyQYBZEqJskg2I4fg-wYaN8POfEWM-SG_5BoI2o2PqO5iAAoLgGtueryc6dosluw-IwgbqY65GNa8UaLVlvsW2EGCku0pVzSjpeliv-SBPdT9spHvB9L3sk3lUidzX0qUBK2oZ8BJMn2VUBS3kh0wIRM4ytT6LPXbCMSoAqcyeMsjfMagSTp6?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8V2DbUaXB5zpqyEg2D0gex8Rj_OE_EbAkcIEWYEBWaYSSkmNc5981oohK8FqyDcIRKUWg8KwFfSVycwizvMX-1ki-r9wfDLmPLRU9e0MKTFMI9iCCyJ6NlH7pnUGGKDNHtWOMRK-XdRUZ8HsUIq6HFVOU5yfe63zqUlVHmHGhTog,,&b64e=1&sign=2a2c075b115e70ce3f88c9ba79accfb2&keyno=1'
                },
                photo: {
                    width: 190,
                    height: 180,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/167132/market_Y7OkIqNg4tIiAFR8D15WBg/orig'
                },
                delivery: {
                    price: {
                        value: '149'
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
                    brief: 'в Москву — 149 руб., возможен самовывоз',
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
                                    value: '149'
                                },
                                daysFrom: 0,
                                daysTo: 2
                            },
                            brief: 'до&nbsp;2 дней'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/39QCk2xf8SQCn5k1akAJcA?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=e-rAPHg5WPQiqOX1TaHhhlqxSCprVsHbMsFFxzVNLqO-SuIkaNZOpxc8CIEDkJJGx9ZeeBgMaR-8QcNgJt-f09_7DjMuRePGdSxHg1EGyi6M4AAVILoUXXMRyWnCRO7A&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 190,
                        height: 180,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/167132/market_Y7OkIqNg4tIiAFR8D15WBg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 180,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/167132/market_Y7OkIqNg4tIiAFR8D15WBg/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFRExfFgOQLlfasMY0CDWNUllPomn_gbDT0y_8hDK-epeBxA3fTq3-gzgNbWlU7nDQIM1DvAgKJR4ik7EBc-dpA4QIo0fTCKOXGyvPPv3WofEHskszpnwouwbif30i8vkrPdpQzzOeH-AlH3JeHu18wQDwpg_H8uIQTYt9YRv_jFUvha12Rk_ychZjOtOtUTDWjSQImih2_y5TWJD2fbXNOgYjH2F9WXMX_zHzck7Xi35rBDCSbLqqGb3K2JCJPKno5DRN7VzzgiOYSZY3EK4breaYKX8bjUXI',
                wareMd5: 'IVHCxeKeQCawLsTiq6_-8A',
                name: 'Настольная игра Монополия с банковскими карточками Hasbro Games (B6677)',
                description: 'Обновленная версия «Монополии» Для детей от 8-ми лет Цель игры — выиграть, оставшись единственным не обанкротившимся игроком Время игры: от 40 минут Количество игроков: 2-8 человек Картонная упаковка Электронное банковское устройство для карточек Простые и понятные правила Питание: 2 батареи типа «ААА» напряжением 1,5V Размеры: 40х26,5х5 см Вес: 1 кг.',
                price: {
                    value: '2829'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mY2KuJPeRWaXKiISju11x-DbtlDrQTHPRscXX5TEIEjOlwlptnfHTzwm_xtSBP8dzCLg_69ElsqmevJy-XBCHEilO126HQcvAJjKqClzF4HQclNMV3DC7SgE8H7pLZ8sAKyhr7qBRprqg8_wxdNcOhzmjZIme6g4idPFTIMmbain_8kuyBRVTRH4sOF2KFAZj8sKAjW9L7aIfqagZ9fWainF352G4qnPc_tXtbus9Iwv0cmRClAyOQ5YX-2f92H20E0Lad34ZNxobtyfSTU5bbeZXZTN9k5FgGbHpMKft4jE9dR95whL2Vew-cOru-U8vRvFikF-4MkodkPshcdPtl3rO_JVt6s017LSqfcvk_wbYxzYIoOWotSCwdMLZdbX4uAIc2srC72-ZxQd0cSs9pWHyPtuEkiNx-LrE8_zH3CNQT1pDB_im6mFPBCAZc-yQN32-rVWZz4fkyBzCdU81N5I0g7DIdH_aaEB0HPQrCtwhiQTYlorjrq8KpHF7XKq0O04JaECYDYWUgFuAc-XVie5bzmNp2Egxr5KGHtvl5O6Z5t1D16iD6lL1wXZRFze0PeH-79b6vl9eKrFsuH9s4mXjlZnZv7teGWeEk28TttwmtTAo4I4ZILlvv0Udkuz4kaGi3ThgB3LcbBNG9tlk2JZ229TkwjpLFZUEn8FX_AtIJAESVQlgFd93PUxY9WrNEmKPf1PsL4bLs9asga-qWumb5UfZW8FM-SEPp3W9POhRNIFTv92FkwUM83QpHOWg-2vvqUZHnJiV3OfYUsip2y5rOHvT8-S6lUQGZ_QIkNSHePCuy8gRCy8nVeg-iQaNTFK7aHsHm3iEoHq1b-KkR91QL8ZigBOXT63pQUkuX9ZT4OmO-jA06I,?data=QVyKqSPyGQwNvdoowNEPjaks9pSynrX3G19_10P74r6zsCeFmDD064eVMPF1rtYMPnF3AqPXaa09SPaObRCJ6wekLKWFB4UzlaNACvt0Mj8dLCePrdr0lolutZlFS9IOsWhYmEEpsw5sAsNkS7GU4pREh5QAPyHhrxQlBnxpOHSDblrIIgv84ZfWXmwuUl4S-XXWjudMCLiv5Y7cGNwjLyoNEOZaZs8OoZju3whMeND9RMwXIDMB7auUuNW6t_17ucHAGL-xABA_-giqyVL9UeKP6MOo9Q-Z-GrRjzxqipHVf-gPSRBQY2pEDfRIWPO6OBlaDJxo2CapshrKOo-sKF0bbysBi2hjvzLC9PWfSn9kx-MA_VFji9NDpwMJ65BD6ScgG8O_Twy5Bn2qVDQVN5V6H6ReCl1mHlhmm0UiwHOJlohpUO-uee3ouNif7BXh_gyQ3Mw7AQNa5xgkec5OEwmyTRJns24O7wh5cKGLYAh5z_ixH4w3UT-vkW42DEBJhBbOmuaamMmRNQfy1-hGu68p2RZcJbXoizdastSuPH1OAafkdb7c7NymngBdS4TQ7rZ5L2nmgrAoZxmSv1CJQSYwDU5rSE7I&b64e=1&sign=147a86909eb28b26e02a89c578132e1e&keyno=1',
                directUrl: 'https://detyam.gramix.ru/nastolnaya-igra-monopoliya-s-bankovskimi-kartochkami-hasbro-games-b6677.html',
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
                        count: 4941,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 536,
                                percent: 11
                            },
                            {
                                value: 2,
                                count: 95,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 35,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 145,
                                percent: 3
                            },
                            {
                                value: 5,
                                count: 4130,
                                percent: 84
                            }
                        ]
                    },
                    id: 186624,
                    name: 'detyam.gramix.ru',
                    domain: 'detyam.gramix.ru',
                    registered: '2013-10-22',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/186624/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                onStock: true,
                phone: {
                    number: '+7 495 505-63-83',
                    sanitized: '+74955056383',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mY2KuJPeRWaXKiISju11x-DbtlDrQTHPRscXX5TEIEjOlwlptnfHTzwm_xtSBP8dzCLg_69ElsqmevJy-XBCHEilO126HQcvAJjKqClzF4HQclNMV3DC7SgE8H7pLZ8sAKyhr7qBRprqg8_wxdNcOhzmjZIme6g4idPFTIMmbain_8kuyBRVTRETQW7RItejjlEjbpMlXSpFmwB8dVFNmVv9bSh7BuDHk_2OVRlukr1fdpKm2yVV8gh3PSg5qotPBiZXv1RVZE-ir51YoSDLtppv2AMx9jMiqSQV0VwoFBzS-WKTP_QGt-kEw4GDkOUYIHomDO6Gk6u9DSTCe-uDYRZ12TmjnI4ZSXQKnf11OXhINhfyNJEAknmz9ocfnJrJq6dtrPoh1I2BBticQLLZYRJNee3sHvKmaQfmTy9rCSBV-n5jLiOBNE3Vc_AzkeCKKuLhpD63O8gPGuH5GeDuW8dM3n-cxrOhJu4NbugphmRBSocY7knoc5hP714tP6Uz9e3UccEyuH6EopqWxE1lU3qX_UyTWW6myfZcoN_v_mRAyICFCESnOhsM8ydIhHIsUGcoM9Xrn3KRPIstbGbEQECtCPcDWpj1F1I30by2UK93OnUwygiyKbI8Lxb-0WUPVUU2TFpKZJjs7FaNgwKzFJSLSYXUx5IBchu7FjaJS3pyROL2tWw6ISO-DCFw-WSKeCw2XuIQXAwuL-38JuG7fUdwPmfmEOcenXZvFPjHkTMF1gzruqBIQR24K2vGNtt_P6y3iPXM4EIA38vcXMQdTZjuzMWRKawh1xAPAsxV1AEXiEv-bpWLoqgaWm6JnAXQULVBeOE0D5SJQjQPOkYKrCXGxoaiRo91782IIk9VFj8UafnSmKDo0_c,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O95eDMjKCvPCLENiP76u6Txit2hRlbrANk-7T7CD0Qn1t-gqxtNEJhIWnSIuCnZhKJCbh8SHa76_tonfsthskhtSapn40u3zrjDFPXPnBIf51WWqQEo0JnBPStuu0jhCVCZS_ujsBzz0k2ys5-zD5ZTPWeRTIKGt3Do7qJhtYiHbsAubuvbm3zJ&b64e=1&sign=ef92e6a6d0fb6191c737189d21f6ff8c&keyno=1'
                },
                photo: {
                    width: 250,
                    height: 250,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/165151/market_epKI3xaIi6pvaKP_RbZcmg/orig'
                },
                delivery: {
                    price: {
                        value: '149'
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
                    brief: 'в Москву — 149 руб.',
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
                                    value: '149'
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 20
                            },
                            brief: 'завтра при заказе до 20:00'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/IVHCxeKeQCawLsTiq6_-8A?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=e-rAPHg5WPRNMdgaopyotQrbDdrAwVunkGg1EazVKzvB49lET86gDzXPrqc5ytJ8b_wx46f0j1oXcgpxAOXJlvIlpyog9T3QIIYvqM3rxgeRydQJdQBuD8LJ4AI9wu8e&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 250,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165151/market_epKI3xaIi6pvaKP_RbZcmg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165151/market_epKI3xaIi6pvaKP_RbZcmg/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZG3XjDo2gWURA_6KdTiQGCDuMtNxcgDNuiVtk3eXW-HLw',
                wareMd5: 'WPSSqLHoKVnurlD1GUBWjg',
                name: 'Hasbro Games Монополия с банковскими картами (обновленная) B6677',
                description: 'Эксклюзивная русская версия. Вместо денег используются только банковские карты, теперь нет нужды тратить время на размен денег. Все взаиморасчеты совершаются в электронном виде с помощью специального терминала.',
                price: {
                    value: '2340'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8medeQi9byJrN0U10jwcsSTilVN34w7q8tsXIdCSDEmXOG-TVA0vS046aEkjlOaezhDU8YmRbNdD1WNWzcBPSIr4Pe4lAgD-Nx884BkSKmPVO2IU0Bujv9JPf10ol6OfRvBgY2IJWbOBd3KDqFPlv79BlzuHY5cbTP3BDIxX2sqox6_6A-edJizlNeAg1Ithg358syzeoNe8R0DnV2L-22aLYCR-d9PX0Oq7Vc97fKJ1zgDVaj-Olnj4N2rHQi5WU6CBWX59kotg2oJ2b22AuRSD-FYXMihOn5KhMfBhk9ILvXa-eQSRSomfvL6J3izUIOI7N0CFitStz6l0O9G_T4BGdp9JlVApe1MieTL7qWjSrW5gM85r9nt9BD7qNfy_uBqEUnfI8l5c85QOWj3MVrk0wPunu6wGhRgvTthj5O0Aaylua4lZLtH0A0e-ovwtDoUoDlvHl8r3Trt2KMn2I9XIgfhUey-Tcoc6lS77n_LBVbSMGiQaNhhAD-z1gs1xqXm84-N9FixxuNeSaEfqEyZHW8YxeVVrUZeL8BQPlFhvSL0hslhpnGex_70gu1zY5XEY0JpE-GcmKI6vIVOtxkPXs11Z9exAIjEsUXCX4hIuVhCW6zqW4yqhUhGoOC_UdXLUXMaTruDQsyVaSlcZ97DzbwvYPyOJ4Y6zcYb9PmvAyrhYuYhD7cN17RJFJvGGjymNpnWPkxjdAn9aAhPzMtFhdpupsMSBnmbSAA9-irWNah1-QamjfHpggrEh02g4bKKPdmXoK3fq7SLyEPTTqYHPqbTkORrXsFcr-81UGviIiAL1tEXtXTKKrcoA23CLIa2FpDWHrEwqhSA9RuSWGN3Pl6FfoOGFumn2SAsjuYzr-?data=QVyKqSPyGQwwaFPWqjjgNhNouZ5NjKxOe7vTulNC_HmzUcy33NyrBpgnASVBLLY4Nw-CLK3PiI2Md6FquAC27BguhCb3DC1wdAz-6AuZNlKAzi6IhI6470M8SGcI5f8RDP5nB3EnKzs7qjxDTt5WFQ,,&b64e=1&sign=18c6ea1c955736731ec953895c138598&keyno=1',
                directUrl: 'http://www.malenkiy-gorod.ru/details/15070',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqw2dUY23spcOCuINGPhrYdg48x3jQw6Q7Gl-l5ubhkbB72-_YMKh5QwD7n62Ug8TJm1JTEiwpWv4FG_mS0gk2ePznLVCQxy-XxVQF20dqvge8kcQ1IjI7azeKKVSVPg3t4KNW6M80y4nukXO34ccn8D0lUJMxsESjZBUQV5Z-KFcj_7bRkEjGPZXFt-wJqi1FTDnQ-9YhEAK3OOxURJUb5ZLTGo6RmBSzB5zGGtdbaefRTvlgIPBcyVO4y1qz03jVKJdd0-ySc7UEJPWoZTkSf7jJ02MGFOOfkojwnnJ_L_7UNtoX5bkHu6aVAT9vit62EelWiwryBgVL4DAhlu3GOiQPcRQaNYVk7VS-qMGICsf6f0pPkNOQElSKIXeHmm9WutPuwxsREGbUw3GyvgJclu_XpLLytKivf5lmR0wb0Vz4zBRHYtv1q8XS218HoDbu_qDfgEFna2Lc4cJ39yNtv4A8Llj2JZqYFPsMzJviNR2ALCPl5F6DIXhu_6BMNI5DZ6zhU13mJbewBEI4JDZWpxg6qPv0ALO6-KNYl-cna1Vi0lYYZfienLpWn4ZoI0X4c-smaWKt6ceH6JkOzWW3s6-0l_98F9GeqhQCJrrgTBXYePtQ54N_HPKtfFYzPs6sXLBWaRofZU5V0bPDiuK2SBggqSPkhq-X_7xsFRgMR-ksHCDdMjnG027z0Pc5ow48A0B5qrCCmORhpd3VEwLfxQuLPMVL5XCUDMn1AGbB7CAb4IMKFbbC7FyuL6-WYxkdioHb2dRGMzQkd1LI-0owz9Zr6DsZS2wk2c_akbuvrlPa98dpih8h4QgmqQbrcWE3AZJpVmp37OxS7OUyXkkbR5mmDwFiowGrmknSWmle-Doku6_Hg8Ur?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2ZrGCSL5-_jUmdkOfISbHHEhOcZUo9Y7qSfidH3NGTV_qZWOX6Y85XKw7t7MEePzGLKvgDLRAtx4nXutd-ZH5AVdvJ656YKTuItGGGw2jL0zaNJUQkK2SXo,&b64e=1&sign=d3e8880505726e160e1afe1070cc8620&keyno=1',
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
                        count: 559,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 17,
                                percent: 3
                            },
                            {
                                value: 2,
                                count: 4,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 1,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 16,
                                percent: 3
                            },
                            {
                                value: 5,
                                count: 521,
                                percent: 93
                            }
                        ]
                    },
                    id: 164323,
                    name: 'Маленький Город',
                    domain: 'malenkiy-gorod.ru',
                    registered: '2013-06-18',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/164323/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                onStock: true,
                phone: {
                    number: '+7 495 669-29-11',
                    sanitized: '+74956692911',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8medeQi9byJrN0U10jwcsSTilVN34w7q8tsXIdCSDEmXOG-TVA0vS046aEkjlOaezhDU8YmRbNdD1WNWzcBPSIr4Pe4lAgD-Nx884BkSKmPVO2IU0Bujv9JPf10ol6OfRvBgY2IJWbOBd3KDqFPlv79BlzuHY5cbTP3BDIxX2sqox6_6A-edJizk__NJMgwoHzfZFK9WqewwKTmsX_sEV0aBIW4z9beu07CMUxFPYuVcCgGBUyFJfPjbxNu4LsTkeUeXwz1IFwZrZJxYbkbgeGfbf3nXhPzYLuwsB2tNR_METFdUbADE1oQJkCP-XJ7A0GpKggWZWKxNnv-GxlmY8b_f0kigzqPOCRTv_kiUemG3DFT8i9tTcxGCHxyY2wIo5-9o1dFsLJv5RzUAswM-_5mDrmFtkTur4DQPNAfdTf7E7agRMTYjE8dLupt1aNrB9QSn91va9amwK9hckjNQHm8r9Cg0ReDtK5o2ohIgWSFCLd2SpcUwBWrHxBuuY9lmzXWO9XydkXULvRfqqcDoUF54kjzrTRpPPDCCgTE-2klul12C8Rouzkf7GEjv9o9bEG5qZHfnlsZvmDd5rZvs7KcnQNF8SMizfaO81UXVM-mdH1a7PJwNXD7LN_fhVEOORS1ujQP0Yecl39Cb36XHr4rnxKpyUr6gQx7SS3V8tIA8A5dAWK7V8yVz4DPaq6ztJT7ec7rC91cpmAqQ1G4x6GjqjWdz4BvQxcgD-1qagskJrhGFyMgsUEKgxFRPgP91xVtsjagosMPYmbW6P6At8NzrtnY6_fOfuOWhr8uCnal2KQwygol2JsPGbD_xNiMfDTS5ctmUycc6DIIMWD8jYgg0ZJAXIBXS_AM9Vuyq1TKMj?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9lDW4rMFaPICMd4_-La2jICj4bP_mDUJkuQ_dTXlGCcrf8ANpVzPqykxZcl9nbhHuiceGBN0BpiBfNLg1cgwshwCXDsgsaz_v7nuAy8B7Vk_Fk8-b-z5BQKGVXzhVktP6FcodIRAd6-SXWXJ7awzts64riGoZmBbv8Ya6o55uloQ-MN3opWc2O&b64e=1&sign=c854addd9c69405f4247707551273974&keyno=1'
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_aLa3Qtx5Mhd2zUdvOVLtFA/orig'
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
                                    value: '290'
                                },
                                daysFrom: 0,
                                daysTo: 2
                            },
                            brief: 'до&nbsp;2 дней'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/WPSSqLHoKVnurlD1GUBWjg?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=e-rAPHg5WPT4dodwMdkv0Z8mH-h9n1cOxmzPzJiaV58HT5vAb6YiBMZM__sPzmXYhYe_Df84glGXg8WRPlSPmzdjsV7yJ7akAt88ZXR2rMCkyWNfRO-8AvnCAasgdcym&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_aLa3Qtx5Mhd2zUdvOVLtFA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_aLa3Qtx5Mhd2zUdvOVLtFA/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEw2ksEqzZ5DRMk2VB3l37uvv7Iby3-LDBfBI8qn4Rli8839h-k_gEMSwg7Nw6pZUBmJnhinN48f0sYnYkbBuYeRXQiwPwa-ib771YaSG4kI3Qh9ikZ1VKrr-sm1v1TqvioRAT-MBkiqM6NGPb00X2hZabZrSUCOXlyz6UXq9JnBKu3cAJiOO4vxB3TUH-Sd2C41YjpYBhCcAyrempjoTOEjqNVvtAvhPJCqXBnvdnzRbtD5I_KRrWkt6xYjVTwDnPA2lY5WCnmExElAau22Wt8ejJ58HPUoPQ',
                wareMd5: '8D8JDIxE1SY5Yd2pwnqbQQ',
                name: 'Hasbro Monopoly Monopoly B6677 Монополия с банковскими картами (обновленная)',
                description: '',
                price: {
                    value: '3695'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Vt2-BSM2AAXnd4Y714v0C3lmcZwTtB0utHJWdKSWAhjKwjKxUiju-iBn9cmlgXxb-hTid1M50a4Zvpj5h_I03YZHRmRKBKz4rTMHXL8jWSq9Shk8GNRljLnsmHwADv3XvsYbWB2MVEPA-XRhUmnZF8kjMzVUp_QrI0buXe_60bCJ5JT2okLEywUYUyQrWGlT10kXk0k8f6oPUAgGk7BKTHb9DSVzFYBFpJm6gvZxsdiiii7WNOIn-zvjZY143TiN7XVQwGvieT4fvytHAD97oI7T6WvyE8H203T5mqaQBOYOYYzfS1BhyL2ReXgEsGtHVI4lmFGLVSiBHzF0n68Jj0H_-1r4k2V-oPCqfSVJXFvdAzzD9ak6EALUb3qE00LMHvUcRXAzfLTmx6YY7eWWJufJd1M_Ysipwj1r_C8qL3shMHrSVxyOP6xMyq49dGPTbCyMPE2--cWeKzMob6XSNbZURXFwffd1D9bg57013FfdaiIutm4S90wDtCFM_Q2n0ERq4pZQ4EuFmgWsXwCsctmjP9j9raLySZtp8ZbnUHjrnc3DSAcYTvTZMSsGVk5QotA3u7PrlUbSsVtY02o0A_-k-B9b3CrVUiHmvyjT8sWMq9YeRc4fXpoagGsNVXqa-jk-D1-rcI0E6vo91bZVSSdNtLAd8kP_NTbWc1qqOVuDZOIGqNItPtKOQZrgoozAFmDOfoL-DtF?data=QVyKqSPyGQwwaFPWqjjgNiFf3QMBC9aLLv80bNETFX27VN7vpk3oaSxlxBIxXaq4Qt1k-bEGB2PM_TkIOrhLkccTIEa0Bq4bMWkoEwMhVXxCPd7jvABeuCXNAg-gYNo9n8AKsZSVQaQbAzlk_zsbUtpJNfydF3Kzoywbz46CjOhmhIUysyY2G-MqRmfPC6x7u2VQRMcI7oe5WfpGLuuHiZifrrdyiwABZawnHI_NB9EjeIPQLC3lUi2SwvyOeZS6fypF-2YwgxrstDcXJB8zCCWHBP7AnKIgi4oaHR2SFL3gZ_sOjGyRxNuJKItULcgDqbMhqQLeEP7NZCYH9SzUlAz-DjZw5VDwfRB_Q2AnVXnYMluTJXEr2oF6OOfaGdgWZ7SMdZkW3uNG9TxTmORjIr0cFGLwspXfYSCkLur5hcDAGaqfa7ViCIZ1Wg8pktyp8m0zp8JIRP9ACrEWSZVB6nRJkyCdSXJ-i16MtmfPg66Rfb-KXC_DrcN9gpP0aHk1ysfDnkIh5mWkMCkuCQxxH7cBtFhaDle9-DX7ZIlX4OKguYTkfofBXecBq09Z7HT_ENQhWnZ2ywOUY5gdZYkt1w,,&b64e=1&sign=73fca45125b189865c83e15e894ca690&keyno=1',
                directUrl: 'http://igrutoy.ru/monopoliya/2892-monopoly-b6677-monopoliya-s-bankovskimi-kartami-obnovlennaya.html',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRtNkb9mq-LhCKNVQTD-xsfM-KNfCH_RRx2kCH_dAQxJD_s_lxFXvmsYTX-By-kQZuSAXVSe2_iOcOia8iKYPHcBuADXkuLVN6yHDR80vbE5fM_K_lQ2155wmLfIj9ZQ5qE_5Vu0d-9zvH6VYbSUQlloMjNDx5yit4hvFgyhcDh2Za8VqWstE2NqhRUNTfpVsrju1YMyIXLnyoOfQx8U5QkPeuhTv99V4WwOwLc5fI464KwBIv1I-dCpBVqhKVyyBYn7eLVhYpmqJI-pP76vLI6U7croD6TC_GfslsYGso0WUKJWMdJ4jaGsqCRyCnArUKe7wFuzThLib8-qn4l9W9UMgDgkXDOx0U5C06Yz72wi5-9l23vjEDfK4K-qJ4ED7t5utYssb23TLmDoctgyxiRhdtkFSb2tGvbSDLrl4C7--OfiFX6UJlTIPTFIa23LEGNkv6IRnunFz8m7v9eL41rQ6czrAd2izc2u1sKQFI4bn8_tgvRCyEHrxTzAfQasp9uCjUGSHwvicDAvqb0-sZmoKbCG9fBPtoHWDCZY1s9yND6aiIl0UoAGiEUV82b6EmSdpKIWiDmOrx9awwtLXl3o3Dkfby31s7V8-X1Rh_Nf493uBYbJULeHmvCXubKCbFXgI_x9_3KhGEeoCQhRG3IE-0En1A99EsQcdMPp6tSXovoZ493BEoanXQNbs3jc-G7MkHY7PJNIEA-ypfGSieDZIBfq2zwq8BTh4lRGqnZPthRp6Unzd4FBUyo8OzyN3G7flxJZNG7mClwtqDjoCGQQ_GaeBcGZMxCT94vbAxWDmhO7YsmYoH4H44rwK5eVj9s7X1IHWAclLeBneRI7lpZkbDSbwsrymGHvU6rFQ7kOfEioww3j2o8h?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2RjJ4ntgqCzjPXX-Eoswi6DR4XunCQ1eSUMGG7r-Gnuyngr7JIpjn3hlyKUcekONHwZG4IemtRjnxD3bl7feqou7eaiQ4sLCfc6-QNVsI12dKE9FW6NnKxk,&b64e=1&sign=883c524c69a79d4acb33dac5bd914fdb&keyno=1',
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
                        count: 17,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 2,
                                percent: 12
                            },
                            {
                                value: 2,
                                count: 2,
                                percent: 12
                            },
                            {
                                value: 3,
                                count: 0,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 1,
                                percent: 6
                            },
                            {
                                value: 5,
                                count: 12,
                                percent: 71
                            }
                        ]
                    },
                    id: 400336,
                    name: 'IGRUTOY.RU',
                    domain: 'igrutoy.ru',
                    registered: '2017-01-30',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Нижегородская, дом 50, корпус В, 109052',
                    opinionUrl: 'https://market.yandex.ru/shop/400336/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                onStock: true,
                phone: {
                    number: '8 495 507-20-92',
                    sanitized: '84955072092',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Vt2-BSM2AAXnd4Y714v0C3KsZpZoHJrz7YTUf-_-QHRRF4KuZLAUi60flTWHUnwJAJAuWBc-zmj1Q_BCOAoi1C5AiGiJzwPRevqr7CfS8Kz_lp5Dc6IAzQiGgWdcBWbfEGS-0ufV1oA1YdBWHKAZg_wnR7EpxffkmwBePtowNN8L_hR3c9sa7VoBRcf2wvz7URGlk4QvobT7JZQugjmt_GoavkeDQXRsBsLsihrK3iVcmNcxHIKFhCCB2RgkutwEskz0RqEjXy6DhvJp1sqmv_I3hx6Hwzg4GWUI1tBiwW9G_jQktiQbwoB6Wz6zlIDsGPxZBetv0tv3_LuMi_j8ULGE0X7uE9rRZ2_FjB_Y42xzbzDMl96yJnFlU2qHvz5z7yoWKLj69xXy_YlAu5EM9M5ofqyIKeZUaQZGkvW3OCjDZYWr_Xp_eaz7CXnZrPt63e9wEeqLCV89o2bXKs8i5SLNl4wDcx5tiqC8hiV_J2-pHEDftTU7iscU0yfJ_rYJYMd43WPbPGfwA9gUT4e1ZXFDKLr6VPh2RRDMmt0QqIh3oeGA7LMPF5HM6U1RdiuStDIOMko7890pwjTdILJFHO6jDpPhIp3Dm49l56nZVEE3KkWy60tP0k4hcA4SLsBcow-dEiQvUiQ6wge7-81IBCJRuo-gqzBEgxUTA9x7FeymJAiCXPCMwAfL-zzQAZgsb-JJfygKAQz?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8eIeOl3wJHKLA5rha7hR_F-zi8tX26JQQDRQT0_ByaeQqpIOUkRFSzSJcea7bVXu3oDMo_RJCsUvCcH9_mF-3KX7bGYlhBf8YTNyc-Icxsbo9TSSJuUwZ9sFNhh80rWKgYXVECs4O9UE-1-hTfqe_T0aUqqrVvNPEWLFaAqNjhd4i8-xTnhSIf&b64e=1&sign=02fb289c4a98c88939a16da3d9da2665&keyno=1'
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_l8fn17yMLza3T2Ac7zgdAw/orig'
                },
                delivery: {
                    price: {
                        value: '250'
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
                    brief: 'в Москву — 250 руб., возможен самовывоз',
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
                                daysTo: 3,
                                orderBefore: 24
                            },
                            brief: '2-3 дня • 1 пункт магазина',
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
                                    value: '250'
                                },
                                daysFrom: 1,
                                daysTo: 2
                            },
                            brief: '1-2 дня'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/8D8JDIxE1SY5Yd2pwnqbQQ?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=e-rAPHg5WPTUWSX8TUtMoHIHn1adyqFwOw1RJEw8moUkD8cCnWQOUNLOTEBRUrfYlhcdmC1jHEQ3o3klrFoa9HQJ1RzS3YvZWK-wBA2nBt_5pIMKR6xfy1ydJ_nhqMpf&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_l8fn17yMLza3T2Ac7zgdAw/orig'
                    },
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/196766/market_nOvSFESHc8nd2aF-iy_o2w/orig'
                    },
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/373800/market_pagykBaNWocOLEi186hfXQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_l8fn17yMLza3T2Ac7zgdAw/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/196766/market_nOvSFESHc8nd2aF-iy_o2w/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/373800/market_pagykBaNWocOLEi186hfXQ/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZExwAmAEn-q8buMmrpYf8pBoiPOfwgojUczgpqVgLjJvg',
                wareMd5: 'EuWuxhrRToE-fy2pyISfFg',
                name: 'Monopoly Банк без границ B6677 Монополия с банковскими картами - обновленная Hasbro',
                description: 'Обновленная версия мегапопулярной экономической стратегии. На сей раз производитель полностью отказался от бумажных денег и перешел на банковские карты. Отныне расплачиваться за покупки можно только ими! В остальном же игра осталась прежней: нужно накапливать богатства, зарабатывать деньги, устранять конкурентов и шаг за шагом уверенно двигаться к победе. Монополия с банковскими картами способствует развитию логического мышления, стратегических навыков, она будет одинаково полезна и детям, и взрослым.',
                price: {
                    value: '2330',
                    discount: '10',
                    base: '2599'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdzEzHTpi0PjeftP8JIXLj2muHJcBx4f5a6i0xLWoojzwbbMFSbivOKxZuESzaG8wIrtC68zPcTXToUeGT2pq2HItCdzjK-bUNF8gO8-LmKfoQ5Y81KifT8fDk2bGCS6xXsP6uHVMZBprF4z3FjKOJSSWMZibE7mdRvhtE07_wTpXoduLrdZYQSlM3aiZLHlVXo2EpRFyTysShKvsszALXvas45GAitXlGlC-qhcmOtLJSbfBsANLHV6SCCsQLBYmw6SK9Uf5zgTxKCwIxzp3OeymvmHcNtZHLd0-gMNR9s-fw7F1qFjj84yUSOfQINbx0E1MhSQILbpm7F2hIluFgly1nrIJcauxG9NRimVGRnVOdZY4rm7rK7HMKS3KQGJg7NOe0e4Dcc43CJUTpV86AdgphjDogDc21Lp2NeeVFJvQw53vrICii8K3_7I8XqPRHME16x1b6otWNXRjNGRrZOwvgEYyezQs3yCRLuAPVsytPGsPLg96cX0qg2khp6vw1NVPFi1O8NAtoTyPviatT0Gm4hNeKjjml1X4gKT9NTM_894WxmPR9Gu4O8mCvFe32ZA4P_xAyLkbGgbCDMtygLb72UQxreGc0qqZpjUnR0YH-7DT0a6ztWo_gXN9A3ocV2c0hTQTy3VY9AhR2fz6EjGb8kGyNUd_V06KrDChND_HTYVbMFEe1N-hUuTt7Rx41G7SULurU-0uRxpNDyFnaWI4PbU50Onfy2PE-f0BHlmEc5Wr0l4IWCY0xyjEA8D6Vs-MRCVpWsRmPGb7YBU206tE6VuQtR2jUAZigJq7sjTrE537TK8J8L8tWa1YJqvQ615OupWbAIQgQSHtHdf2MUV7ZcU1kbZjr2hvXmvEaNjZBDw0Gl5Dno,?data=QVyKqSPyGQwNvdoowNEPjZYTydLDHpJ7q0X_YebLYeeL_sDDCJ8DCPtVVgHQecbC5nDwZLVZxIMUcR6i6eh2Mi279McY0GBA2z7EI3FT53POVx36LbsV0ug0Evq3WDElz14hSugxK0FZLhRPl26_JPdgF1DqoF2ds6yhmgLhI0_Xd3_oIMLV9f-kC7X3tKLBhrac4oI0XWRyxc_DEcA0uemoa2OpksE3l6ktdy5ppAomUd5qX_2TRCqcd1KltykSk_lkYq7ecaUYgXBIvTNDC_eD84WXdcOxLg0FUPbGaHppFIV14D8Ji2bfXDJf-eevQbrRQd3A3rM3Edk5CmOHejrqAY_9dZUJ-nqkeHuhGPv7i2SYGKGCov5V0UX43fJZvnOsV5b3Z-SWoWV8xrcuw5urjZSWIBog2bUScUIM6WLGwOy9Y5waaqkoeWNPsi_zZ83UpsQAulZ2A44PWveRGmZ4On6CIQjAsuYmcObV6YQEY-yC9ENwakKVEA-uphsKdCbmEuXkUWvibjTm2ZmKgDlUUpxa-W8GkOmacV1-yqvTxJE3n_BMdV8f9ynT8HC7weksToVYqvJwnduXpz9yUHEcbZXePuY4OwLPgLr4P1XxqGY4pNP2ou1QP5HeUu4Z&b64e=1&sign=9b911d950d4f965d8b1062504ad6e6cb&keyno=1',
                directUrl: 'https://lentaigr.ru/razvivayushie-igrushki/monopoly-b6677-monopoliya-s-bankovskimi-kartami-obnovlennaya',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRufkc5G38e6HZ4hS0tzvn-B5g1Ve2MPPxz877ncv3Oe9Ouu7ZD8eFPZ4gMXjp0jS7_fhMFm_3v9UMgI_3qdwZxWw24Dx3lvwYkMkJVnJ_RZw72QyX1C6CsYl807jlWKhui9u_ErkSJziJlqxYn30M5NW2MEzWqbpLmhvuN0M3w97txoKKhUzoSpCAn7DDS9LYfKDrviyMenwaKr4fRYmKfRBmtFfHsGwXsuIhdAEFjulBI3xYKqNRs6K93CQXDIbkLhxT_JHFFjGKBlqCOP1bSvZSbyQpvjQOOTjGDMrgndkDuqYopMkXdKXR6h_Pl_HYzBjrjBsR9d8iOF_O_40Y8mZ2WnFR7WlsJNCDWlFwbXZ2RvKkyEyZxOIyQCDraWwf3n--b1gcjhAUmxbxY2LH10W3ar19zMDpI_2oEr5ZWIvEzVkRTFDZoFDLA2-yFYw4vGXk4yQZ1iooKk_tK06aRyqAOCddTm0ceapRRTLNAdZpatmcNpwtDgQ6GfEa_k_l38boh2L25_kj1zNjW1qX02iSdO3oe1D8qY0ssZQcLrhlUb_BjmbIRsOf2pd2G-S9cPt2PetT8vfg5VfyV6ViQtk7nI0oKdhp_uE6TOnLYy1ewne6aDjr0Jq-Gbu98-6V8G8fNHuvCviVa0UJUXRs1UfEA0c4PjBr67PKY7kXf0qu16WF7y_tATDV1SDxSwtIvtiOEA8kxmgdbbEaK8OBWl6I7PUsiRA5St3QirS21b8WCFhfQU5OE7RjUjfrjer7VB1VIaT1WM2LjbqKjdy5TqWxJgYCMvrhYGRkEV3wJvSvFt4lqAyJ5CGloBtYLZqQO2ekUpfrDgCZWbLsXWXDKeWjqKTaFBEvHFd9jrjveae0vSu_HxSMz7?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2UBJewduMBgEVRBjyF9iuLJdkS4Gv3TaprV-ZBXMveZAPu9WGTsR9D3qe2xGu8IeuG4Ob-5iEvwkxk8LQ9j4i-FMfdgTwswVqBIkNOlVsXbylGwUx2TDzp8,&b64e=1&sign=e3e7ff1516f5c15e236b1ffcb2a09672&keyno=1',
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
                        value: 3,
                        count: 94,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 14,
                                percent: 15
                            },
                            {
                                value: 2,
                                count: 2,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 2,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 2,
                                percent: 2
                            },
                            {
                                value: 5,
                                count: 74,
                                percent: 79
                            }
                        ]
                    },
                    id: 217016,
                    name: 'ЛЕНТА ИГРУШКИ',
                    domain: 'lentaigr.ru',
                    registered: '2014-03-21',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Октябрьская, дом 80, строение 1, необходимо предварительное оформление документов на возврат, 127521',
                    opinionUrl: 'https://market.yandex.ru/shop/217016/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                onStock: true,
                phone: {
                    number: '+7 499 394-48-32',
                    sanitized: '+74993944832',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdzEzHTpi0PjeftP8JIXLj2muHJcBx4f5a6i0xLWoojzwbbMFSbivOKxZuESzaG8wIrtC68zPcTXToUeGT2pq2HItCdzjK-bUNF8gO8-LmKfoQ5Y81KifT8fDk2bGCS6xXsP6uHVMZBprF4z3FjKOJSSWMZibE7mdRvhtE07_wTpXoduLrdZYQST0FfvE7R0EXsXYWsOEgqE9rYozT9RS3TN9lV9hayyOQeD7TESXQxoXrOUFLuG9FBpACtMdQKjY3b4AVMeOtOkqDGNFu6OzXuQkwwA-tzoYGRt6zN6P7kZH88iT0UM3QWi6n43LActNk9vpZQ1YCrPm5n5QZjItKhAg53RS-YC_V7nXLi5g_YhFu0cGh_2MECexnM-VaXbnSlagM4GbbIyEYJetYkZPK9X7t4lC4ZnHKdngGN3pL-YDud1Lo9nAPyudzaOtplV1ImiOIYcZyH-uBlbLgtRsKEu3RrrLRD-eH7_yPIqV3HiTahZDE2pKWFzlt4YvxMMqV5LQD-h1jfdJKZmAh3KdkB1rfnq9_cxXIzNELq5r8RuqJ_ySEWvXQzArdhxmKlQed42VbA_RfI_mqzcd1DAwtJy1UjhTI1end_wnfu5qyX9VbB70u6l7mWAzIduyrmclQQhLEzcys_FZondZkKxy162sVLeoly5hi5EDtHrgwSZlewfhGfl9OvjMU33sm1AXnhukubzuYqphWnxyw8MGyeMAE4pKw3qIVOMoQ1YIKDpnFq6JghGa4mPoGy2sRFzLolmxKnjYkQHX5Hlb5nflAIMKzsvkgNmnydF7-rlADfZRwMptsm0wiy0iMYxmkQgY8kFhCGtbKFn9eSyvwOZyxNZeUmMWcRoRuQJQGEdj9jbItekmmad0Wg,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8QrBfXLsBPdymobwH2jwS7fI0IOdc-b0d_R1ZvCfTjVVhD_rqrHiz5G6LV_PSXserf2wFP1HoRuLZuo1qVZ16qwzUYMH5gqOZN8RRuITeZ6b0mq9z5O2_QNqxWXnQqbg1-1qrhuxSsTdZvK8550511-GHdozCgmt1OiaSdHKe6nJDwPNuQc792&b64e=1&sign=0f728d8bb662adead2d29bdc8e192313&keyno=1'
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/235547/market_oPz6-YyqjgTycaFVRmBVeA/orig'
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
                                    value: '290'
                                },
                                daysFrom: 0,
                                daysTo: 2
                            },
                            brief: 'до&nbsp;2 дней'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/EuWuxhrRToE-fy2pyISfFg?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=e-rAPHg5WPQrH92Lx0O32vqe0nEgkiQEa_KqyULOJ_BSFnmsmpm8Qq1ErQ7d17eSuKd0-ESciB9EJs4tOoDCLzbH7Q9N6foW9Ckq6u_n_gYrbOq6OXsahq8rubAOK6S4&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/235547/market_oPz6-YyqjgTycaFVRmBVeA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/235547/market_oPz6-YyqjgTycaFVRmBVeA/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZF5LsJJpat44uZq3h4tJ9YAKLtf3QSZ-_ItRP2rr2wwcA',
                wareMd5: 'pTu3_TmbFh3IwTcrk5wAyA',
                name: 'Монополия Банк без границ (с банковскими картами) обновленная B6677 Настольная игра Hasbro GAMES',
                description: '',
                price: {
                    value: '2527',
                    discount: '5',
                    base: '2660'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mY2KuJPeRWaXnR_aAt_Ssb-vRd69KIETthoQL_TmRnOJUIMpkr1VEtsSZex241u-E71TIjmgDoyFIyf8hCHYC2l6XSBk2l4AVvVas7TvEQgPYSppgBkvOXaQxv6g4-4J90UDSjKIR9VEHE5OOjD7Pe6o-pbISiU4lJYdyNn40HshG4f8_pQBBPE5MQYMHcz6M8YazdWhFrd1YW22QFxDCHHs3HWaqzqUXqV4Pa_5ye7FBSgXlLhYOf589t0r4maX52jKU1Hxp4cXkgfUFhAmRmm02rZidSPfVvnTAAr51qa_UI35cG12eMeO6m0eoEtVja1ZZ8JjKr2R19MnMk98O8zSZA9OZlvEh6oNkrlNDGyItZsD99tU0tmNJYFqJUGeau4BFBsOoUr6cSN5OruXzaHNSrD8_J9dERnR1Sd54zi-hNJxd5sm5qeatiGxhlwWs6c2S90Q_aBV5ftbeNeUuJt7anza5r_Fj_lhZl-6bLZFBqcXdU4aN2wEYDTeu4BRs5CCSbMsp813Li_9t7xE7AW4Sb3BtTLsKoJO7u5MY4BXyIKzrFHiBvBagDy-W7OqAhkrRUge3wfK6P5ViNBUohJjUniK5R9QzCSvjGR7NMGSbGHht0RdtLgLX2yZJdRf7oek5PeuW8T95a60ek9U2WXY95RFg7jVHkiRuOMVYRsujoPXyEgKeqqsO9l8-n7oKHyiVG6GbhrNDEVro7V_h9knbUd5wQAGNlnebczWVulPBz7LH3xzU5KkBNiKWRcV6sTNFzT_IQp8d0DfJV-g1R36YYxjvRz0S03ySy6dpxWrzjOTYP5zuJ6tysQrL0EG6gL_z5lCgtiA06TmK7npblwJ4Yl0CvRcwhTFEMEkQ55wRd1sm_NwrvA,?data=QVyKqSPyGQwwaFPWqjjgNu15Tq_EtrAxLM7GdPCdRYAsCO3xbg7-Yy40bIgvodE_wpQs93j9U_XcnJGsn_66XygIfLmwWThzeRwp697A47lmW8LrDUPRaNHj0GlWV4Ji_8xxgcrT8hgdUpNw3BLU3GQ43wtTX4GkiVgwSIhoAKwXIXiePr2eLNI5DCysENBr7Ul-rJ_N5-jG0gkIKpYoclQZhrzQm_ltLQJ3DUp9y4ZWU3ruEncH4JyCv6z-RLcGI1Nq_ARMPpQ1fAFGiBIIWS5zUMSywAXqoDYi0hfWRjnTyz0FtPtPWeJRoDFS1LL0Rw5oc1jdt9vjyga91AuBkQFa6VMJnAuauAu0u2xfSPoFzKysOVhU9X5YzGDam6GUuob7xNpz0gIgPa9LrGGbqhLK_kaWsizNjYQDLLJaYY-CoSXSzhAdyivv0fpAqcYaa-SQNnOVnL55E7DwxPth-NT_3VuFaEmsImpOuuI8KP0nPItg1MaAbYqSxuztAHFcTaX39VGLIj4hqor30wC4oA-6DJUsqcPXnJjICA0oeMG6r-wOSt2f8zVhxRbOvturC41LZlXt9jCvf1d2N5mOLqqT2lnFo-6-7BhnKU886rigmFkMCGxC3T-4lL7ONJ_7IPLrLJG80ajH2IwhBpXkh1Yqz3F6Lcw7nC78sV2nX7jab1qyu3yZO9Uv7lH_njvk&b64e=1&sign=69e701d99fbc28a7aab18b022a3bc149&keyno=1',
                directUrl: 'http://dvapirata.ru/product/monopoliya-s-bankovskimi-kartami-obnovlennaya-b6677-nastolnaya-igra-hasbro-games/',
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
                        value: 3,
                        count: 108,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 6,
                                percent: 6
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
                                percent: 12
                            },
                            {
                                value: 5,
                                count: 89,
                                percent: 82
                            }
                        ]
                    },
                    id: 293277,
                    name: 'ДВА ПИРАТА онлайн покупки',
                    domain: 'dvapirata.ru',
                    registered: '2015-05-20',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Сокольническая площадь, дом 4А, ТЦ \"Русское раздолье\", 3 этаж, офис 306,, 107113',
                    opinionUrl: 'https://market.yandex.ru/shop/293277/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                onStock: true,
                phone: {
                    number: '+7 495 921-51-57',
                    sanitized: '+74959215157',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mY2KuJPeRWaXnR_aAt_Ssb-vRd69KIETthoQL_TmRnOJUIMpkr1VEtsSZex241u-E71TIjmgDoyFIyf8hCHYC2l6XSBk2l4AVvVas7TvEQgPYSppgBkvOXaQxv6g4-4J90UDSjKIR9VEHE5OOjD7Pe6o-pbISiU4lJYdyNn40HshG4f8_pQBBPHWmKECmNswCPSJGQ8J3eoPgZx4FGyVByMZUR-y41jOXNZL_8d4PzgSHPveke6fEyWKuh40ALp0XPmq_3hGI0zcmJj5atbRNG9Tge_ZeTygCdvrrmR2Px2sMeAgGcKSpNM_CIhLC3TianD68Ddm7CrgZ-aKcEaaiFKkVpj5iF13quRjnzE6kFV90wicyED14Vtdem4kFibXsJxOrCc-SO-_2yYVHbygSzUkyrXbLpVXFFcU22x2Y4nfL8s48hMtq4jEgL_8UxpgwrUABJzTYecIXHzODbIV1_vox_TED31s2jJv-ZZbHdFfnDG2Yp81qxd8mqEC6-Pi7BapTKpnJI8csn0xbaZ0NIL65YJEeKjhR6M-Y9eLXXAi9rI9-LSCpwAmaL_TnuiUz8y0buApKk1Brxsis4y9WRdBV2eFm7W5MLTJ7sWKVFmFzDj95hOts-VXWuZOWglaPYHCA5dCAk5SjDSWaRBSzRRZb7HBZdpqlErL9xIKdjIEABQQ0hTLpO6Ih9L_ogdtCxDPMTHpPjhPVL3HzmE2g3C6MQwHmQRiCgANkm_DGb5NlZyetGlm7dJqlLV_1eEBKRW95PPeG7ua2x8qyxE88rogYHIRDoz1zvQoaIN46Tkk4PKUcSXvxyDK6U1rttiE6Wot8L1L_73kL6tPpIV3wUka_kfket32qMjDjdfb22OX-BCqRFuef34,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-lpF-LgVGImYkegd8A2q6Yww5rOpoEBAyPALERJj1DzEGEaNfRrJq8SSIYJFNSfPZb02Cew9IE4YhRkxvj2bW9s6DAxKJ58khfAOD09BDTLM2Ou9IPufGn2mkbo2lJzy82XJI28v5dQiv_KIIWQn3c8yES3y17QtqfnvRYdX7xPFiC0WlTMGHs&b64e=1&sign=55aca12a09dcdc580af14709cb993f3b&keyno=1'
                },
                photo: {
                    width: 574,
                    height: 407,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/225051/market_mEQVamrCC7SQtGMZirvD8g/orig'
                },
                delivery: {
                    price: {
                        value: '280'
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
                    brief: 'в Москву — 280 руб.',
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
                                daysFrom: 1,
                                daysTo: 2
                            },
                            brief: '1-2 дня'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/pTu3_TmbFh3IwTcrk5wAyA?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=e-rAPHg5WPQQXk_SsjrcHsQ9Vw_v-80LPwHQ4D1hyliTDdDTSXPdO-0KNgKrIy9swIDzfwYu6yIqEzRgyuOsaztMCagKMMOdHfssHXgd6otFiX6r18SRij0vQaPupq1g&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 574,
                        height: 407,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/225051/market_mEQVamrCC7SQtGMZirvD8g/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 134,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/225051/market_mEQVamrCC7SQtGMZirvD8g/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFedaRMg7j1DIFPiLW6Ulsz2ukBknvd_vOp8MAOpM4KA7L-t_kvT6e0AUTBxypkVyp9DkbkBCp-wTTjItacgwcQEcnvVjUaIZ-sleKRqgy3-YHPgZSHqH16I3XI8GyCLnsPqsXCmKewWePN8boA0W8qr3ZSwVHyLiJZ287GYtEsYPccJ335Boac_zeyF4qsBK0yR6d7Xh0CFA-1h5CP6ojk8ZDjOKihb9_O-VEUszec-ITFvMV7FyofR1kSCNY0a4mSli7OSIplOiZtTNhF8L5ce_hNcQz-Azw',
                wareMd5: 'jyh_F3q6qakcz22G9P2eDw',
                name: 'Конструктор Hasbro Настольные игры 6677B Монополия с банковскими карточками обновленная',
                description: 'Обновленная Монополия с банковскими карточками. Устройство Банк без границ позволяет вам быстро вносить в банк свое состояние, подогревать или обваливать рынок, а также одним касанием покупать собственность. Устройство теперь работает с карточками Собственности и События. В комплекте: Устройство для работы с карточками «Банк без границ» (к нему нужно отдельно купить 3 «пальчиковые» батарейки типа ААА), Игровое поле, 4 пластиковые фишки игроков, 22 дома, 4 банковские карты, 22 карточки собственности, 23',
                price: {
                    value: '3618'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Vt2-BSM2AAXnd4Y714v0C3Hh5DCYtm8xM5nsvCOrly1-rxUe3QFlAOB1O_t7K2ry9IYSCHTlZkahJzrInpPru2sVmpAyhpYsH-ww8jUVfhtpLr5vAYj9pNTj2vxvK2Rcm6NRvRjIrM8yyRDRyzdV9lsClGQ5xpHXqqk2hHoKxIKzjl3PD_Z-BFEkKP4RhfsNTmjUKFsoexLLi38VsCqSagDkARaM2EFnoHXe7gJ4-_uMOBJJICDNjuCVYc0xDERuZoQsa006HGiAB85mL84BmDljHDtaTXoQD3wPsul_AZ18lrLkGhNPKC4t_qPoHwGVoPtNZ2pvCp4ZaVqrr12Qf0qjWxb_jbrgpOiyz1PZGCUvqp_gLLEIKrCmkFITBXUE6BktGyVBt9V-9drs_IWHfiykwk2xl9bK0Ap2WvEx72YBhPSmWUjYovVOADuF0aibkRFH0mky_pN-St1SHQqrvX1rNnvADI7BcG9s1zDFjb59wOvds4yGC8na-rMLSJqR5cLt6s5PqORrZnUuDlnQ8pklfeLhgkaBg_CjW3Tv0f9L6dvcHR5ZReDwdHL__BzZ5nInh8ndsfGdB97SQjALu01REOFlArgNYiGJA05sxCReTTJVymRt9fkuJoRHoaJDls74LGupVO4luwFxKmknnWqdxbnhxtGoPjZSV6LzyUeQPkJrAjeksPgONPXJ2LwAf8ZMPcgk7Rm?data=QVyKqSPyGQwwaFPWqjjgNoBB3O0qqyPqa12uUPl7wuHoMKWA6WazBNZj5csThnd_wGQobQiYhmkvt1Fa5DlikSBuXrmb4My65SJXw6pdMvqGbgApVALDUzg-Vrxv6iq-r98CQ3Da3Fc3GE1CzCMEZLPQ3RxTKqUESzNngA4veso6CikOVp9y1iruZ-QofgFdbTLjj7R0WiiwPLN0QsA8FtQzU1avkiCe3uBsAPg90dqgfLKDBnoUjGJjFOjOjcD0Dq8mtUO4pb1GcHwVJ90-8CcxHuVMGRY73nwNMry711ZUQQgF7boUpfHk6qVO09Q5W9a7ST9SBc3aNN3XIgrGjiYC8Gv8EhE5QBngOVRWw4sZYXrRTk6zNdiZLmDUdbZs7sdiJgMfANOapuEjnSMDpjkFksd7PL1PJV9tElEZTKHVImYewYsRjw,,&b64e=1&sign=acba2ff887d571fdea04b7ccdbea41d6&keyno=1',
                directUrl: 'http://www.hasbro-shop.ru/kategorii/nastolnye-igry/monopolija/nastolnye-igry-hasbro-6677b-monopolija-s-bankovskimi-kartochkami-obnovlennaja.html?utm_source=market.yandex.ru&utm_term=5503',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRtNkb9mq-LhCKNVQTD-xsfM-KNfCH_RRx2kCH_dAQxJD_s_lxFXvmsYTX-By-kQZuSAXVSe2_iOcOia8iKYPHcBuADXkuLVN6yHDR80vbE5fM_K_lQ2155wmLfIj9ZQ5qE_5Vu0d-9zvH6VYbSUQlloMjNDx5yit4hvFgyhcDh2Za8VqWstE2NqhRUNTfpVsriJYmxFlkZjgmDj7oBNB2277gzMHK5JeJz7DZUP_vV9WR3fZWAjIBxLjAfesNT3XT6Y9VeGiSfPGsnWsXx9evWqVMbbScPq-4q3tWlRnghG_JOuZr1Oy-iZvQko9mMWHI8TOzAVcOfxchL_4RvuqloKpyjmxJgFJs5jX0tSSUSSAIXkt-WwqF5HfRFSiYpb4A3aPuzZNFpXyEE0E57hPRrAcQsgweNlbpBWkaIeHrCskwvaIDwlVPr7hCj_vKD3SXKO0MiVnM2iMy7X8Wq_YnckBsvP0t0ZdRn8TRAI4q9lf1QvgYiG-r6zYEBr6fbNBgVbf8Y-etgOdCm81EfLZzZLxzPiijaXWYw4UuO_nqpCkfmstBJ8Yid6pTow69atUzbHJQgoRReXOlNSgncYVZ-zgJ1xzJNAYNG34uSIziMkLed9QF1G4qTC8NpD2KU6A32bwpZTrZasBeLBQs3rnnVyKQokKKJ3QZzS7nRxiBWNaL7f4Qzo4EZegV5GVvKpuSW-geplDogTaKEP5GnHDbC8AEZCgyDwkKlbBGfQplvCD6l-IoWO4aB85R9o2VpnWahlVmRF7M_vOWbHaAHfuZgI8o0XVMz7kS5IXFYm-1tmweh8dz-G_R4X7m33q1Jhc1zAzoZRTPRhR_xGPAhEa6mliOYxwBirtsbuWkTV_0CHvrPwA0ua7iI2?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2WZyfRYU7yIyJsiQLMRU5nfS-kVc5PmRJsbEMnvEQUQhw2_lvYZfoTaUS6h465jLWsxB6uZ0MJs27i9dtwvaIrGLKiSJy3yaPlkqWA0vfe7L-X7VLgkBlsE,&b64e=1&sign=56db5bc943c740db85410cb2e8527e2b&keyno=1',
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
                        value: 3,
                        count: 1,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 0,
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
                                count: 0,
                                percent: 0
                            },
                            {
                                value: 5,
                                count: 1,
                                percent: 100
                            }
                        ]
                    },
                    id: 173657,
                    name: 'Hasbro Shop Ru',
                    domain: 'hasbro-shop.ru',
                    registered: '2013-08-12',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/173657/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875803
                },
                onStock: true,
                phone: {
                    number: '+7 499 367-54-90',
                    sanitized: '+74993675490',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8ma2r3UMLMyQAA-Pp1YSuj28mYtwAj9NT_HCOo6bhtCkZ9xEtZJnQAIJrDUtS7mQwXiXv2bCHLCm9qY8Al5angMz_kbYrSLys7Vh-q6QU6NZJiPH_Xj_GlFB5YIyaGE8HDvijnwDl0g-UEa2LG078_Gv3yEOJy3RI3Vt2-BSM2AAXnd4Y714v0C0sa3Ui6WSNl6CB5cmesDyaQ6t45EizrS5UHSWiG_lf5x5JSD0lrwXqnCpmt_nlV5KuQqzvsxba8TPZpW7n6KpBsNYdGX8wbUtsa7wE7c-j4CifQRKUpCSCP9u77kTNsU5WxHO4Xw41DvWbMJ1C7ZIPhZ7sdm067Gxi0UfiTz_gLWTEXqATUXHHN6ALflYpauo0JN3cWZX1pfDMDijYMcXhd74f7koDq2KihMEs7y4ejMlRWuwu_cBsmO-ebAgiBPnST7WGSQKQyGoLeFD4rEganUleQ641o52Qa55NysS6JY1QLRa5VM-fLON-PUmVqktOL4ZCFv28ZZdEjZpSxDaUuTrA45Ck1cnzu1qQSaOF5dHoEx2qxQgvb33JIfJy4dn_eKRdca_n5UQdfuSdk5tGJzrI1eoiFui3D50Mbzpg4o65cpeTBzgZWB4byjBh8ZpHwEtSHBB7c3QKsq3OutqBxQvD0OQXVm20UPDwUhg7sv_B_Tf8ZOO5LjJUHAs82i5lM2xW6gGBnnBHsiv4I3Y7V6LTy97EUtk9BV1EMQYD6Vy5sn0rU8jCrzYHsZpxHOr60fwvhlBjuNhsWCbOFFBAJENEtEXwDFWKbUwdHj7AZJBC46dh85UHUHjMhMWvQ7-oq3leZ7izjl1t9_xrkA1kGjwgpyp6ToZr4c70sMFrT9L3prYu57Zj?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O--cdsWPxqIvd_pVzMwxJUZdkUrnEROIOrcyWxJI0a3QO2OrZ5BYamJ2IhL-2dcaeRpNCqhlArswiL-RBY6dhhikEmmsLSDnecloNJWlTfe8VxBW3Hc1YFDVSZD_tCooUOG0boZl9bQgpXVu90xdDrglEjyJHf-GZ5qSExBWECz2HWs72vU-6Yr&b64e=1&sign=ec16d6afabf98f263100c309d89383e9&keyno=1'
                },
                photo: {
                    width: 500,
                    height: 500,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/250456/market_E46ndRKHs5br0Dw_y9SKNw/orig'
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
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/jyh_F3q6qakcz22G9P2eDw?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=e-rAPHg5WPRxWO4aL418avP_TG58y_5IjqswJtOzFPzIajZIoEugfm984zFwKWLxYv1uI3tkBfu_ATwZQX7PpRQp_st6vPgb1nO28fDIBRTn5Q2sLlxx_m9Q94LB1OQy&lr=213',
                variationCount: 1,
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 500,
                        height: 500,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/250456/market_E46ndRKHs5br0Dw_y9SKNw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/250456/market_E46ndRKHs5br0Dw_y9SKNw/120x160'
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
                values: []
            },
            {
                id: '-9',
                name: 'Наличие скидки',
                type: 'BOOLEAN',
                values: []
            }
        ]
    }
};

module.exports = new ApiMock(host, pathname, query, result);
