/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/model\/1730875803\/outlets\.json/;

const query = {};

const result = {
    comment: 'model/1730875803',
    status: 200,
    body: {
        outlets: {
            outlet: [
                {
                    offer: {
                        id: 'yDpJekrrgZEw2ksEqzZ5DRMk2VB3l37uvv7Iby3-LDBfBI8qn4Rli8839h-k_gEMSwg7Nw6pZUBmJnhinN48f0x6GtrRHd0_hJxzXXgHZDgDiyDo9EV35LNRgIltMNtgPQaN8KbG9Yfn5SCR0onHI22XIWYCa74W5bCO9wVW--sR9wQmqDr_ewu1-rxmjWfeDvMLmoq9vCrEbHHqy1yXD5ZjiALEpgrbTfk-If8OzU481ufQ863-LVPj0T1Je_2Agg_7IcfgtYwzuUXc-jTgSSv4jPBNGLKLHiWti3ID3U4',
                        wareMd5: '8D8JDIxE1SY5Yd2pwnqbQQ',
                        name: 'Hasbro Monopoly Monopoly B6677 Монополия с банковскими картами (обновленная)',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mSS1quw_6gW99p7CV5HkuJyUazb-bgEq4PkIylSggFDYzw2390O49lCOK-K23dHzPPb-a9BHs7mXz8tmxK9zGbrKbZ_-T2dRlCyftuoI4j4m0er91-6g8Zfl4Q2aKujdtdLYpATw9PQcifUS1YmhqUuxkBxUS0i5EecL430Dv0BOKH5JncYcFaf8YloR8VxcMd8zis4QZoRTAhPfQ8tOM-x5DiIqAtqngwOjCtWJBQcbpUp6LOZKrbeG7xyg1Z92DAPRNNq5St5NcweFMfvl5n1lixUHFTEsHWTfkeRalWT8yjBzF7xdWs54LSBU7lMtWx12mYGDRn8wMRe9YC_1hfXi1-50MtnEiGkCyDdn-UZ9HlDBsJxCv7DsYYUCs-IIaeMeE_cy8AOjuzKVLcsCr5EzBq-Jj0yBexWohpz8dUeBQSWGEWECbPgMWt2P3J1jvOEAqxsHu-nXWm1b_Y1fg1xV5rGeTREUK2a-fjXPk4O1y3LQEpfa238F4WFbXEnaXrne_urv86PlY-OFD6gVdnti393iBAKP87yOeYkyC34vBgfXo_bJdZ-rkd_OMc2hIOtwyh44pu5NYUMI2ynVDfNIrmKKPKPZqfdD5EZuSKlEi3zGeNkjPS5FDLtvOClnxwbsTZfOg1xU_skDJ5HmaDBpx7WmGvRukXSXtNzPh1V5ijliETIXbXkVoVmRwnk4dncu1SdZD3EJIVaaYuRPZvpDWBGyByIKsuAxPYCTR7uYDGaG2Q5ceYr1LQC8upQGkyQPtwgeDFwfAX1pD2Mk_2s1I29HlkN0GzwJfA3Pe9cJucRzXl_dilC0wsPsh6j2-dca3GA16YmPzvE2izb1IsBPx0Mo1Kphr4741Jli3gsv?data=QVyKqSPyGQwwaFPWqjjgNiFf3QMBC9aLLv80bNETFX27VN7vpk3oaSxlxBIxXaq4Qt1k-bEGB2PM_TkIOrhLkccTIEa0Bq4bMWkoEwMhVXxCPd7jvABeuCXNAg-gYNo9n8AKsZSVQaQbAzlk_zsbUtpJNfydF3Kzoywbz46CjOhmhIUysyY2G-MqRmfPC6x7u2VQRMcI7oe5WfpGLuuHiZifrrdyiwABZawnHI_NB9EjeIPQLC3lUi2SwvyOeZS6fypF-2YwgxrstDcXJB8zCCWHBP7AnKIgi4oaHR2SFL3gZ_sOjGyRxNuJKItULcgDqbMhqQLeEP7NZCYH9SzUlAz-DjZw5VDwfRB_Q2AnVXnYMluTJXEr2oF6OOfaGdgWZ7SMdZkW3uNG9TxTmORjIr0cFGLwspXfYSCkLur5hcDAGaqfa7ViCIZ1Wg8pktyp8m0zp8JIRP9ACrEWSZVB6nRJkyCdSXJ-i16MtmfPg66Rfb-KXC_DrcN9gpP0aHk1ysfDnkIh5mWkMCkuCQxxH5fMLTqvvu8tNJy7T4mwBVeS4WSDHu2bj51ZpjjaTp1IZXgzubPnnpqFRaeWjFiQOg,,&b64e=1&sign=045eea7004fc4fd7bc7024ef65e278ae&keyno=1',
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
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mSS1quw_6gW99p7CV5HkuJyUazb-bgEq4PkIylSggFDYzw2390O49lCOK-K23dHzPPb-a9BHs7mXz8tmxK9zGbrKbZ_-T2dRlCyftuoI4j4m0er91-6g8Zfl4Q2aKujdtdLYpATw9PQcifUS1YmhqUuxkBxUS0i5EecL430Dv0BOKH5JncYcFaezD3-Kpkt6Od4ujG6Jkmnsdc1BiipcdP6Ys2RBflh0Tk6p814nm25TZsk8w4atwxwdWplPYk0tlkPyKUtuLwnMS_aLsyklF4fBJ-K7qG9b8IHXcoNnHRljfy_aG8GUPh0ay2U19vW609MIzkBkeUK5yUSKMt-ATymiC4Sd_yMqG0AIcktEwYe8i00k0LLjE3Gwqqzz_ecKiVEM3sWNpH31KsNF1MIGQzEN90kCYJiMD6a5Z46Uil9yLysUXPELTfNEPMRURBkFA6GjsgpC5D5fz1K7a9Yb0JI9kEWl9j6U34WHABC8NgBXeSSpCPraV63HbuP0U0fPE8ZSTQZGOjPHs1hNzt-QA8iqPMkXRhLgLTp8OZ3PM1Z3e2LHGHqlC7TzHesRjmY95ybYbaAWyCUy3a5Jhps58yQCGUprkD9vH3q1w2VP7C4DM355GE-WcTBC6CAv5gBmwhqX5z7MCWOdjAK5rod56VutBy6TMVWT9JyKzj3mYSyfEHjSR76u8AhG44Vyv035LhduPtlBs3lTS1Kn9zy2iFO8DZLqtBLbGyByBDaDzBp_FEgUEJvyIwt5_DN2P8MVCGy8WyUhbF7XC9s4UsN5q_RZNK0xD-G-4lNdIya_gK4pZPeogWEU7Lhhhwt9gWRW9YETHFGcfIDhFTpeR9-ZzBuGCJz1IuDrXQZDz9dmVk3l?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8eIeOl3wJHKLA5rha7hR_F-zi8tX26JQQDRQT0_ByaeQqpIOUkRFSzSJcea7bVXu3oDMo_RJCsUvCcH9_mF-3KX7bGYlhBf8YTNyc-Icxsbo9TSSJuUwZ9sFNhh80rWKgYXVECs4O9UE-1-hTfqe_Th9wVLZBE2KL3yAbVPgkdg2GE7r_NE4-6&b64e=1&sign=0226397d0a441010c67ad1421faeafa0&keyno=1'
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
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_l8fn17yMLza3T2Ac7zgdAw/orig',
                            width: 600,
                            height: 600
                        },
                        previewPhotos: [],
                        warranty: 0,
                        vendor: {
                            id: 15157809,
                            picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                            link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                            name: 'Hasbro games'
                        },
                        categoryId: 10682647,
                        category: {
                            id: 10682647,
                            type: 'GURU',
                            advertisingModel: 'HYBRID',
                            name: 'Настольные игры'
                        },
                        vendorId: 15157809
                    },
                    phone: {
                        country: '7',
                        city: '495',
                        number: '507-2092'
                    },
                    schedule: [
                        {
                            workingDaysFrom: '1',
                            workingDaysTill: '6',
                            workingHoursFrom: '9:30',
                            workingHoursTill: '18:30'
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
                {
                    offer: {
                        id: 'yDpJekrrgZFedaRMg7j1DIFPiLW6Ulsz2ukBknvd_vOp8MAOpM4KA7L-t_kvT6e0AUTBxypkVyp9DkbkBCp-wSF0zuR0EWdbrQdVw42Lx4AVdFLwhPtQmLjAiGEJvZ3qkjbfimB0N1lKyrCdqnv1nwkZJ2ET5r2e6-0fSENyAHsVDM_vQJ8ju5Pw8o36PSuJ622hPZforMUBo7nXnl_3Db3wwjckIHWPdiKhTJNUHMifkiB6_64_nGNTGFJcnDW9ofmfgUeTmg46eK2siK6fmENT92e5F4sMRfLsQC3Mg5o',
                        wareMd5: 'jyh_F3q6qakcz22G9P2eDw',
                        name: 'Конструктор Hasbro Настольные игры 6677B Монополия с банковскими карточками обновленная',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mSS1quw_6gW9kZxGSv5Zav3g4__rbwMvHkDyR37MXts6v59zWhuJyRgEAOrHvo_mAKW1Usv_Id8gErnw_hA5Zbsp_5e0cT4Z1_UjekqgdaLolbekbzLYKKkZydHdE-gPGf9U2SFTd9WHm2MIzeiHstmxnxK3PZDxwdxpaoJuuqlvG3Ynt241fGTUIWjDUCOBbBW7Tf3YPS9Cvh3YooDD6DYci1dHma6a-qyqGbxQ8HWrz2xKSO8JAhb_deaAxURLuND1Jtk0APHbbaLNw3I9gKOu70UkBA3O3tNyyKBif9I_CBbHt8SAf7utVyouQx6Ol0R1h7FSbciG5ttwMLa5iHwnXGF4znByNkBF4VCwXcQ0AN2-31NdoPkDuadSsPJnSFZx4NguxbQhYXNSEMTKDmG-DIsagtYFZXFCYMV9klOO1dpBy3Q2yt3OAkRYFGmWX_jfkJKcR0XApOezUM25tyI1iDEdV0w7fegAVs5BJaApZMedfogLpCQeFlCKbfo-PlDVejNImr3pu7UgtUKl3lLrbEyW2JCKDGv4Q8KKtWg3IIsvrjznRB5nnWEkwKkJyghJN2N0Tz1w0g8Uy1ivHiwkoc5ODR0gmURcWOmbXL6y1ca29dFEhUU-B_I5Gw3HMo__1eilrQDcPRdio-PpTCZQzGJ1y7P-cgXGTur4nktxLo2I5c9JeNbT96Un-WTnSIPtAuGm16vuGjxUDL89J2In0OHNwWCZbkLPmJBfIB_WiNiP7PjhJLVKJtzdtfnlP2ciUod47K_VQF-dp9URuSTbmhBaXiJrWqsev-v7JX9SlWgtEPsuj2QSdAOsgLtB4HMPcYruUxtQVmQLG6nOBw-lRzdZZOkdzEVD9eWux8DN?data=QVyKqSPyGQwwaFPWqjjgNoBB3O0qqyPqa12uUPl7wuHoMKWA6WazBNZj5csThnd_wGQobQiYhmkvt1Fa5DlikSBuXrmb4My65SJXw6pdMvqGbgApVALDUzg-Vrxv6iq-r98CQ3Da3Fc3GE1CzCMEZLPQ3RxTKqUESzNngA4veso6CikOVp9y1iruZ-QofgFdbTLjj7R0WiiwPLN0QsA8FtQzU1avkiCe3uBsAPg90dqgfLKDBnoUjGJjFOjOjcD0Dq8mtUO4pb1GcHwVJ90-8CcxHuVMGRY73nwNMry711ZUQQgF7boUpfHk6qVO09Q5W9a7ST9SBc3aNN3XIgrGjiYC8Gv8EhE5QBngOVRWw4sZYXrRTk6zNUMQ8n8hV-nAwgagZzbpinWgYnr6TfMUoV1g772MJ0lip7twM66U4tDbmBi1bRvotQ,,&b64e=1&sign=717d638ced9f12eb55a01c4046c51de7&keyno=1',
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
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mSS1quw_6gW9kZxGSv5Zav3g4__rbwMvHkDyR37MXts6v59zWhuJyRgEAOrHvo_mAKW1Usv_Id8gErnw_hA5Zbsp_5e0cT4Z1_UjekqgdaLolbekbzLYKKkZydHdE-gPGf9U2SFTd9WHm2MIzeiHstmxnxK3PZDxwdxpaoJuuqlvG3Ynt241fGQmVhO8pjeO_gkSkjwgg3740QJzZ9K11GGXUiWZCubINgVle-2tWk2bUaBPgtXTMp3KLZrFW5HYEhd2UyKqFQhdkFsa_VrK8DzR0EJyJ0eAII8_38M9iuyfMqV24COrLsEccrf7DsPGN6D13GJzc2f2ALq6Om32U0ij8jLXLU24afS4_5L9bhtIwIa6SMTAcVv9Kl8q0TyV-QHwpwTPNa7E7sOn95K1NzQiXgKTS0PJ4L_0T5YwOPUtyDqXN6_j6ljieGka7HyvHslrPJ3lwlWIawliIkvAk8FIcfgOitJRIpG3jtNI2Z4wtPSiUJz9-6US8sGQ4ysHzbafB3MkwDpwOtqVZUnQ14F47sRro3H4lFyB4SBcUb5DTvg9dvFPxfhLFV-h0wXz79qzUpmijt-xFf7JxJR_gByhHUkNN_V8BUE2fGvBSdKRKHpN5gObnRCZhlMtvQmp_SXJBm0_kCzCAGnl3AOlVGx35FA7KYMK7QR-7NAKEnTOJomfH5cFbAKuuF8-3qK_WeEkm4eycr9T4jr7BvEa6y3MnXeGWf0bXbf4-DpczcLPtNpR5tdu5Wu9rfxwtXVIjpk_jSydLKD27-Udc0PMQXGM30zOPW2uOhsuotdCnwMhInm8e6YmN1pbRR1qFtJPkf-btalGfFDAZ8oLhcdrLu3IccIu7FyKKWNkDQMZAzI2?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O--cdsWPxqIvd_pVzMwxJUZdkUrnEROIOrcyWxJI0a3QO2OrZ5BYamJ2IhL-2dcaeRpNCqhlArswiL-RBY6dhhikEmmsLSDnecloNJWlTfe8VxBW3Hc1YFDVSZD_tCooUOG0boZl9bQgpXVu90xdDrgHXi4AQ1g8vUo8vv_ASY9x-WLSIkhKv3B&b64e=1&sign=34a4b486c97492b23ae821776b43592e&keyno=1'
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
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/250456/market_E46ndRKHs5br0Dw_y9SKNw/orig',
                            width: 500,
                            height: 500
                        },
                        previewPhotos: [],
                        warranty: 1,
                        description: 'Обновленная Монополия с банковскими карточками. Устройство Банк без границ позволяет вам быстро вносить в банк свое состояние, подогревать или обваливать рынок, а также одним касанием покупать собственность. Устройство теперь работает с карточками Собственности и События. В комплекте: Устройство для работы с карточками «Банк без границ» (к нему нужно отдельно купить 3 «пальчиковые» батарейки типа ААА), Игровое поле, 4 пластиковые фишки игроков, 22 дома, 4 банковские карты, 22 карточки собственности, 23',
                        vendor: {
                            id: 15157809,
                            picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                            link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                            name: 'Hasbro games'
                        },
                        categoryId: 10682647,
                        category: {
                            id: 10682647,
                            type: 'GURU',
                            advertisingModel: 'HYBRID',
                            name: 'Настольные игры'
                        },
                        vendorId: 15157809
                    },
                    phone: {
                        country: '7',
                        city: '499',
                        number: '367-5490'
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
                            workingDaysTill: '6',
                            workingHoursFrom: '10:00',
                            workingHoursTill: '16:00'
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
                {
                    offer: {
                        id: 'yDpJekrrgZE9X_vyx-WCDAyBcZ-BOKLmp6VDwAoWmyHDuDvwWla88kjq6azu2Rn8aLTeyG1Uq1wbwKF14JeJaAwbnSQdPi_wOHC_9Q3HpJw0hzSl4kkBJE9iruRzxLNr1f7ZZZvPOp2TF4yAq406GObFgkDaK-nIDPYi8FFz9FcUnl2pXxJm0pjcpBVSQcwG22BQhSv_1ExC0YoX3_CAM83c36ytZkceJ6UETOGm--To-1XnWrWuCDEmNf8tCibSnAvNAr4EHMNlAnw_zSyxf61289MlI0labYMPmB_84RE',
                        wareMd5: 'iUOVP9AJ-2rbdo7F8GLF9A',
                        name: 'Настольная игра HASBRO B6677 Монополия с банковскими картами (обновленная)',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86Fag0qDFrcRS2xLI8PPWYhy-RW__e1QcvpqqZN9XFo_URMsMu4Js9o05heeDbtfvMIHlcqVjXC9rtZZTsrX0NNAHgeGYLFk9q8AOeoTNthSbW6At7JLnOAx8cpC52aMsNJpfLn-DKBsipUhdcnoZ4Px2GaakQ7RgB9qKxZykdU5jZ0L7VgAMOx8pePQH_GaJrQR9QBCwN4nw2cTRiGGeQn1ibiSOfbBMdPbi16W24LOoXF-cEve86TEuqiVDBIwo4ssjJbL6JMlXbLX0SkUH4ohgK94OzrXQH9I6vDe2QfdKZsjy2mpPAj9dBNfhmQZkkb2Hda97seDmedRUKYBPc90r_PPieXJD1Qk-oezj47kh1-7sDp4tNCQ1tUR6kBGi_jmC2gu0Lo5OSKn6g89TXqhLhTtNhHJZUySQSrapkvv9trfKeLhUpRSPY-u1EmZv5V_CirHV6vW1VpdGHyfy13z7riNyARlGC_WzdupHg58cpX2J7fJbj_ZtE73Ge6ATcnnmcFUU9NdMj_BgDrJSIdFcnio8Yw0ilrhcg8VoK15yrLAfk32RuwdE8cni_p5cvaDVBI_nx1XKT6Ud2xERnb5Z9Ar8rFNgG3V_ckKTwQnqZia1YZUgw6kEgW7SzINr6epu5sL9G6cRcUka0t6QIxlXmM199g_cAZhGXY-D_K4me7ei0wJ50Atzr0lOGc8wkeGKKJHYLXYMrVw_Jv4VzpsdUDjz0tqaL-P_Dj6sTvL2Dk8TWIf-nuNQhIXBbnqJXMWELgz_D_rEMUb4rTnuGO6iAd6XitWDwiAaNt2EylXym_anj-t85HLlqUsu_UwgQ4pe29mTvfB8Ql1Aas7xo9lxl8ZyJmTuK?data=QVyKqSPyGQwwaFPWqjjgNhk4rTN-sddqHZqf9NHgkGm3Y7Nsx0J2Z3E8yQibGxT6xFSwgKTnxZS5Bww4wJlMw_S5ef5DjETZgsu-XpOOGc2Rf4XPc3dsFAytXNc3OqfVOFJUwif1b9UeJYXNle6ysxG3Smo746FkuJijrjTJ8N6PJBtz0DAeZHHtOowVcXRrvNeEXUBU3kexbZn2L6I_gmnJ6xdeaeq4SUMVoCsJCCqskcWzXM0VqvYhBt_i7ho2sD8QLviDdQQZ1cvAASN-zA,,&b64e=1&sign=ad201ba54d0e4c58a5ed4a790d1a7298&keyno=1',
                        price: {
                            value: '2100',
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
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86Fag0qDFrcRS2xLI8PPWYhy-RW__e1QcvpqqZN9XFo_URMsMu4Js9o05heeDbtfvMIHlcqVjXC9rtZZTsrX0NNAHgeGYLFk9q8AOeoTNthSbW6At7JLnOAx8cpC52aMsNJpfLn-DKBsipUhdcnoZ4Px2GaakQ7RgB9qKxZykdU5jidgiofHOAAx2mzN8Zw-ee03QWAWiMAFwBVSHm6eHmYx6u5td6Ka_ML4buAqNBHB_jl0QicNFRvr5zc4LGu9uOuosPe8DJNyPWqjiFqoEBtwnnVMAMeq4Lj_I-CQF4aQGS4uKxeLJZKMRexXReuYaYHxXlZHkz8-2WDQ_so0BIn_bcV36Zt28Nb3pwWhS52IXf2_LkMq9Phy-xv5tf1e1ZsBsrvblinDLIIr-Wx0aU6yapiCW8Emc2IvXo4xtWy1boCSHK3rS4HJn-A5hB-iwwaKQM6R8BHfWFNp4w5Im5WIaDg3RSGdy_wvTFLyQES50MJOEdQvGGl6jl7uZt9AeGbzUamuwsBOb3fH-asd7ovyaipuEkUuiiY6zvCBYv7POeRTm1YYVWR9OBPQNDG8AgLooEvUUE3Bq3izdrVOUIw3wk1WrFdIW9z_0bsfh8J0nP7TTf_QuFrxWmmFi7nQKV46rR02QlG2Zll16OBAj0a79t6Tpo73V6sNCt9ltAxrp4WDSrEKt1DU0JU0cuLEVesOc-Ssw9mM7s8hCOWQxyZcI9tByxY_LgYwnqj6cIRerGSrlWC7EG5qvWnPaC0-ee_xr_yLLtcvS4FlB2CJP0Rvkg4QPXg0CTZr3QBsG-M3czrmOxBksN0tN8huEsuXEkkC9tO26DkBCQ7JLJLQdPXYzbWrvpp46o?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9aI3_3uo62FWnS2wIxOW4SoCY4ZqA2EbAAK2r3SHtw2WmP4u3DQ2ufSwia6Jw41FjKb_GQA72DrdzKQANq1gHr3MGpnbygM4SX-ADaGXOWKSsIYRTtZgpwAFXTIz05V6jrz3grtZXNL-el4zjVRYn3OrKHsRsF7Chr09mXnb9H6cCI3qi9o_cl&b64e=1&sign=746b36316ec1f5933e7ada0c7f924c88&keyno=1'
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
                        photos: [],
                        previewPhotos: [],
                        warranty: 0,
                        description: 'Монополия с банковскими картами (обновленная) - это одна из самых известных и увлекательных настольных игр, которая несомненно поднимет настроение. Игра Монополия заставит каждого участника почувствовать себя настоящим бизнесменом. Монополия проста в использовании и играть в эту игру одно удовольствие. Монополия является экономической семейной игрой, которая подарит незабываемые моменты. Цель данной игры, это естественно выиграть и остаться единственным не обанкротившимся главным участником.',
                        vendor: {
                            id: 15157809,
                            picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                            link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                            name: 'Hasbro games'
                        },
                        categoryId: 10682647,
                        category: {
                            id: 10682647,
                            type: 'GURU',
                            advertisingModel: 'HYBRID',
                            name: 'Настольные игры'
                        },
                        vendorId: 15157809
                    },
                    phone: {
                        country: '7',
                        city: '495',
                        number: '968-8358'
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
                {
                    offer: {
                        id: 'yDpJekrrgZExwAmAEn-q8buMmrpYf8pBoiPOfwgojUczgpqVgLjJvg',
                        wareMd5: 'EuWuxhrRToE-fy2pyISfFg',
                        name: 'Monopoly Банк без границ B6677 Монополия с банковскими картами - обновленная Hasbro',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mUM4HZAZE8nwAfqEdpIup04iRXFO46Yso-paA24l5qg28adMwZ1NJdbNcl6S4_xr7CvNuFum0BbzXsO6biKIUzLfjWtdE9dg1eb3rJmcUjjLITDqDbnDMzxJWxHw-QxtGE0IKooaI36rRHV94711hhr04l1l5LErNqyMvhGX9_Suc5zSyOD6SacwL8OMo_yfE4zESmQXhgIpdL4-CSjrvTr9Um_HCUj7YImKIFVkgqbzCLCU6yrOxy8C8SJxFvPDiQTEy6h-UuA9e-TrAuSHJQPen1LcDF1qMPRNxg9wjBGdxYCLxW4oD67K1OofJ7E8afhW7j1T6K2W0sEwMkX0jaRJXqO2WjKp4ixD0YmLBPKcuglh6kvfW8DfsjeMnBs3hFv1eE5ZRU-H8SiTMx54pyrrii2eS_pbkqJdskKm55KpjTz9k1d0GW7saQvYEVmp4VfZZGUM3cXQIjm6IW-DY3jchKiSFm3p_g8Rkhhi9tBwKPITNmTeJYB9LSZ8tj9HGgLvMtyFXgqsxNfwMMUUnfA3mdkaS8JPHgfe8A-XoVHzf-Nd3zphrEAl5SLMPN6cbsR3le7xi6p0TWfz0n6a96TYAYaOpnThMQavqVH9Z75PkCAVPxVlyEnTzQrcmAygYHQWj2vuwbVHoNGH6hzoOyu2ZmvAoLeyParwbU-owGAtFNo6v94IoXWb92KRRWfwuzQI4WtDsqVADFW5qvpMN0IO3Xe2VGpDnyH7_xxteQ_PU63jWKnrHyC6X96YnQyP0SxqRkP4FL92_FKxMPuD4iblr26o4Lm7jUe1hU-tMc0cqQOuUgtBcGzKQfclDMsoY4z3ITgXGRqWmfKHNbfNB55s0wl24tko42MlUBxrdKqY?data=QVyKqSPyGQwNvdoowNEPjZYTydLDHpJ7q0X_YebLYeeL_sDDCJ8DCPtVVgHQecbC5nDwZLVZxIMUcR6i6eh2Mi279McY0GBA2z7EI3FT53POVx36LbsV0ug0Evq3WDElz14hSugxK0FZLhRPl26_JPdgF1DqoF2ds6yhmgLhI0_Xd3_oIMLV9f-kC7X3tKLBhrac4oI0XWRyxc_DEcA0uemoa2OpksE3l6ktdy5ppAomUd5qX_2TRCqcd1KltykSk_lkYq7ecaUYgXBIvTNDC_eD84WXdcOxLg0FUPbGaHppFIV14D8Ji2bfXDJf-eevQbrRQd3A3rM3Edk5CmOHejrqAY_9dZUJ-nqkeHuhGPv7i2SYGKGCov5V0UX43fJZvnOsV5b3Z-SWoWV8xrcuw5urjZSWIBog2bUScUIM6WLGwOy9Y5waaqkoeWNPsi_zZ83UpsQAulZ2A44PWveRGmZ4On6CIQjAsuYmcObV6YQEY-yC9ENwakKVEA-uphsKdCbmEuXkUWvibjTm2ZmKgDlUUpxa-W8GkOmacV1-yqvTxJE3n_BMdV8f9ynT8HC7hJcfZsmV2RnGuLMS2wxdJWFrzccceW8fLWPn3PboN9mgUk5R8HZBG7ChYkty4nKB&b64e=1&sign=a12ce90f44063b9d12fad02039a41a96&keyno=1',
                        price: {
                            value: '2549',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopInfo: {
                            id: 217016,
                            name: 'ЛЕНТА ИГРУШКИ',
                            shopName: 'ЛЕНТА ИГРУШКИ',
                            url: 'lentaigr.ru',
                            status: 'actual',
                            rating: 3,
                            gradeTotal: 94,
                            regionId: 213,
                            createdAt: '2014-03-21',
                            returnDeliveryAddress: 'Москва, Октябрьская, дом 80, строение 1, необходимо предварительное оформление документов на возврат, 127521'
                        },
                        phone: {
                            number: '+7 499 394-48-32',
                            sanitizedNumber: '+74993944832',
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mUM4HZAZE8nwAfqEdpIup04iRXFO46Yso-paA24l5qg28adMwZ1NJdbNcl6S4_xr7CvNuFum0BbzXsO6biKIUzLfjWtdE9dg1eb3rJmcUjjLITDqDbnDMzxJWxHw-QxtGE0IKooaI36rRHV94711hhr04l1l5LErNqyMvhGX9_Suc5zSyOD6SafNKwYpCJrfVkP8ZOuRnB2qWx5yRLfemSrJ7_FcWsvkZOMyUoowNwBTcsdOujumPA2M6gVAhscVIJdkX-cylAc9T3L7g30hPsjshA3ek_d4fl7xztmJ6fBx7M-_edStNzzGQTh-HAhCBmhs-ZQb9cA_VJ8fC1R0y63odmvzFeK4kQYIwPDYwsZf2DjSjtA8OGpxjIh2dSvJtfcr5G0XrI_pKGKSQdDawtfBvkZ_gMZRcpAzUOkIGzD8dlp7UW7EYSAJWvCq9iblmkfmVFb4MyBAJzc18OGV5eUj33LC3tVPPJiYxDSETKWcuXNHZilehwkEHKN_fqVccIDnWqtautFfbsSmZQpBrbYtfbeR5bPJ6igWBeZFzKQNk4G8B9hLeGHJhxpqxxX0uxtFI-Yz9Yp_fTHPWraLiPSgZWwSOMixPM4A58jyX5fQ3GoXzMz2-nz3ut1gjzpRrEYByGdY3wiVAn3DrxxmP2GYS4W1FJitMqv0rIYvkO2yccUxcBZfjeQHbyOGz0N9dOp6_qDivwd3Ed5NL6UVxAJdnoNcJOc9Z6VDkuUpDHKWfwmtM_Q9kNQdzXkfmogiGn9Xc1ydb4a5uEDCbK-fW8qr8pB9qHFtlP5vtWu0drON6DHfvJVbtoO6jEwK_pOF3N-OZhy18zzhC39axXQIL6E6xOK3BFKyPTLxd5a-6ZCq?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8QrBfXLsBPdymobwH2jwS7fI0IOdc-b0d_R1ZvCfTjVVhD_rqrHiz5G6LV_PSXserf2wFP1HoRuLZuo1qVZ16qwzUYMH5gqOZN8RRuITeZ6b0mq9z5O2_QNqxWXnQqbg1-1qrhuxSsTdZvK855051103gc7FW962HSAqNApuIJzXmy3zsecIHU&b64e=1&sign=746d7d51b79799e49ec4d4aecd26b5e8&keyno=1'
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
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/235547/market_oPz6-YyqjgTycaFVRmBVeA/orig',
                            width: 600,
                            height: 600
                        },
                        previewPhotos: [],
                        warranty: 0,
                        description: 'Обновленная версия мегапопулярной экономической стратегии. На сей раз производитель полностью отказался от бумажных денег и перешел на банковские карты. Отныне расплачиваться за покупки можно только ими! В остальном же игра осталась прежней: нужно накапливать богатства, зарабатывать деньги, устранять конкурентов и шаг за шагом уверенно двигаться к победе. Монополия с банковскими картами способствует развитию логического мышления, стратегических навыков, она будет одинаково полезна и детям, и взрослым.',
                        vendor: {
                            id: 15157809,
                            picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                            link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                            name: 'Hasbro games'
                        },
                        categoryId: 10682647,
                        category: {
                            id: 10682647,
                            type: 'GURU',
                            advertisingModel: 'HYBRID',
                            name: 'Настольные игры'
                        },
                        vendorId: 15157809
                    },
                    phone: {
                        country: '7',
                        city: '499',
                        number: '394-4832'
                    },
                    schedule: [
                        {
                            workingDaysFrom: '1',
                            workingDaysTill: '5',
                            workingHoursFrom: '10:00',
                            workingHoursTill: '19:00'
                        }
                    ],
                    geo: {
                        geoId: 213,
                        longitude: '37.612811',
                        latitude: '55.799725'
                    },
                    pointId: '348148',
                    pointName: 'Пункт выдачи',
                    pointType: 'DEPOT',
                    localityName: 'Москва',
                    thoroughfareName: 'Октябрьская',
                    premiseNumber: '80',
                    shopId: 217016
                },
                {
                    offer: {
                        id: 'yDpJekrrgZEcLLEAeEQpLMbTbIJ9XrZgHVWU5FiIkZ6ON8QfCKGtN79-O1zTw3ZAw1_yiwluT8tg_8WJXCUP8O9ii3TrhllwY97LianTuQIQZZDKKikC2-xPpzG3jN0S35FrWb4xDRr9FW1nCj6l9YM5jBPywvdr1EnIYt4b6e8pVfYBVMEHaV9dbojSFXXJA5wwR5oTpEL-zNuk2C3PEyC_L0ApS_nq0-EFaF9xKk-EBWsAwWkHyWIRkBGdsD7tlgFxdTrU-PzhJzVL32AwWrXHAhLTmiLP3RDwcKAsm6A',
                        wareMd5: 'BeaPDMxMS00m5opm0Ue4Zg',
                        name: 'Hasbro Монополия с банковскими картами Русскоязычная',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8meyofUjCL4PMkiT9-ED2W_BROwe9Xei4OBiSWCSvgRcHQ39j45OlexWm35pO4QqsstfQLhGSpl3omQb8H5ghPDLYl_2k9boyJ498eNEHPmcmFNEu3WiN_bPHz_wbjUzte2vQ3DQeG3QVPeZKzqpTmmk4RzvBkESvI8FYmFVUPBnqtXRzXv0iK-56zys_cdrlSFhKfQE6dqZDW7S_551lFMdZapzLzfBkPXx3iBGK9N-xguGtX3N7Vlt2Wx22CcraBePfHU0D1MuJUxml_-sQQhxg_XVdtfHX0ZhUFmLGxndJRmhM8mxWjZY05Pjeup4Dne00baLY6Uh1nvhYIliz47brA9tSh6xY-V9hlsm78tgUpSQfRMnhxBWXPen4OjP02gGd0AidEqyU3xD5qA2N1heWlfBdVRvOS9kGIShDlVG6Hbgr_sGzb10rJ807QL4P6u3NDvjFkMPCEMeJAIsGJO95zw1og3Y4FA_N2g45TUomILLfTu_Bor2KaA1su2LAisXXjKI7gh8yPCnC_aY0JbEBkvFLw9mpTfmjzojjHNDr2ntvdZyTqECXm_HSvk_jVxYHEoynSzUJxZRpAEdjVjBhe8AZuv94M5MolehbjmnokVQwJ-jT9bLI1iSn9gOOE5bJ56fhpOC1PD0qZlL0Eyj43WWGCsqapYTPlw1yMK5dBlTBSp4TQv2Gr0_GWy7yhUAtn1jG2MkhBjG23gYvt5DlXUpYFzFvwoEpS_9BTFc1GBMMJyNjU-hKgfunzW97rqpRdSlfs2Gae-OP0pCydj9HTj3EI-yLjEoxmbQ94Zrd77NcWxMSgYn_1eRsft2WbW1VXhxTnOG7t9afxFUCH2F_W2TXOtTTEmOmDh0axb2A?data=QVyKqSPyGQwNvdoowNEPjQsjSLrQfZc0SujJcZn19WcKRnp182cBxbp8WzUqnSR8ZEaFw8BDe0b1R2ez7aKXr2hIzqWfoCZxffIn_PGvvzV5f-gfPd2QdbnoyQ7IGUTh-8KyPmIT0BPYxeJ2e_fVK552bCdCU-sNZDndki78lWLc-Ysy9h7xxBGU9ZMZsxGa7G5usGg3NsADCru6_wPZNQ17Rltd_FJb8-wkxmUqbajecWO-SlhhVJDal25poOAI4MIaeKeZvl2Mh7wW1KAypFb4r1Ar-V1uhqwaBCHoZ88Qrx59R6CteJClL1mgA6uzDEUWRTnRQn9-FZH188XW7wml1WhQXLFwCXSnpBnngcXV9GlvpxCS8fiRrct_pAQnmBpfapEvU6tbgDBnXlR0tXVYp4gNeNON4ggzoQwD_e3kwXQIhPCfsL4YuRTCAaWEkdxZUTWp-li5djmw8PbEirhUYYYfanBd9iQ7oxxxCdirwwayC8jBXdNtvT76urcjxzxCht146n2YVxW9pmBl3xjXf0kRBVe_wWiJONyphcPoTxuFHJkhyw,,&b64e=1&sign=ab9c6741299ef2cd9d9e0096e301fd45&keyno=1',
                        price: {
                            value: '2386',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopInfo: {
                            id: 57782,
                            name: 'Планета Детей',
                            shopName: 'Планета Детей',
                            url: 'planeta-detey.ru',
                            status: 'actual',
                            rating: 4,
                            gradeTotal: 67,
                            regionId: 213,
                            createdAt: '2011-03-05',
                            returnDeliveryAddress: 'Москва'
                        },
                        phone: {
                            number: '+7 (499) 166-58-74',
                            sanitizedNumber: '+74991665874',
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8meyofUjCL4PMkiT9-ED2W_BROwe9Xei4OBiSWCSvgRcHQ39j45OlexWm35pO4QqsstfQLhGSpl3omQb8H5ghPDLYl_2k9boyJ498eNEHPmcmFNEu3WiN_bPHz_wbjUzte2vQ3DQeG3QVPeZKzqpTmmk4RzvBkESvI8FYmFVUPBnqtXRzXv0iK-4b6r_jgrxXSGl3qYlsoWCpuXniClCQaeufq8vX65MVEuoU8XE5OK31ImcYrjbb7nE7rnuJADkhNwRJrhgKVuwLHQF1zecoLqlbpa_Jo5nK14_2LdNf7bvarp30l6bMwwLM0lCEbh-ZgBFt4AIbfz6f34u9slg8HB7JPye6EMJnGlPxwHjRZP5jy8bWfSxeWwlP4oRkyO_x-6OoDytdVdunWMDkkRChtb595qdV5z5zvHbv2omrrlBcEjI3Ud0alCaK0jE4gXvfLp4ETVnqHDniqyBQ4gF_e9zAetVx3LjhOIgaehjWIX4sDhU01sVndBB8fw3Dvv851eoqTyWezGth_wjizE9Si6TkR-8BTfpX-cr7pXHACeeoD9f7-D5_ELlnZgauXJrHxrooYw7yGgCRI0-LoWDZE8Qery_jIonY5bZkXdJ60HTc63KajnXgpUVRqlRGA4N568crpPiwirpQQwnBvV0Uc2Prhqm3BRA-IGeVZpnQ_xLwps-GSSaFUVwC8e4zgmYeKJFo7Ql5wGFsY7w-8lMwhxplIniJ78sjcm6XcLhzsXHo3RIxu7cn51g-16VI2DarcKW582JX8uv_c3_bIsbzh2GTJRcqJd5KeBqaYnTdUs-_SUDLA5qajQ-w0hHC85aC-fE-0AQcvC-vH2Loo9vtE02DL4-AZvRT9AJaGFY0hZlp?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8ve6Y-005VTkrhlFR_N1m9fnMb7On3Ih2lhebH_yUD62tPTRro2qv0eKAHCzBYFs3FhWeKG_Qjd2txavC87mPRFR5NPb3xV8gDMwWsH0CiuDZJafQjMZwwtrL_8dmi3umFHO56WHGTziwc3dvQ7MTrWCKSHfFQU5TWmFlivauWzXedgeJJNc77&b64e=1&sign=dbe2e0b0ccc4a9360d0da5af4ec61edc&keyno=1'
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
                                        value: '280',
                                        currencyName: 'руб.',
                                        currencyCode: 'RUR'
                                    },
                                    dayFrom: 1,
                                    dayTo: 2,
                                    defaultLocalDelivery: true
                                }
                            ],
                            brief: 'в Москву — 280 руб., возможен самовывоз',
                            full: '',
                            priorityRegion: 213,
                            regionName: 'Москва',
                            userRegionName: 'Москва'
                        },
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/205818/market_XjOnYhZQrp1SyyLNJBaN-Q/orig',
                            width: 296,
                            height: 296
                        },
                        previewPhotos: [],
                        warranty: 1,
                        vendor: {
                            id: 15157809,
                            picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                            link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                            name: 'Hasbro games'
                        },
                        categoryId: 10682647,
                        category: {
                            id: 10682647,
                            type: 'GURU',
                            advertisingModel: 'HYBRID',
                            name: 'Настольные игры'
                        },
                        vendorId: 15157809
                    },
                    phone: {
                        country: '7',
                        city: '499',
                        number: '166-5874'
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
                            workingDaysTill: '7',
                            workingHoursFrom: '11:00',
                            workingHoursTill: '18:00'
                        }
                    ],
                    geo: {
                        geoId: 213,
                        longitude: '37.75397408',
                        latitude: '55.79052769'
                    },
                    pointId: '9487640',
                    pointName: 'Измайловское шоссе 73Б',
                    pointType: 'DEPOT',
                    localityName: 'Москва',
                    thoroughfareName: 'Измайловское шоссе',
                    premiseNumber: '73Б',
                    shopId: 57782
                },
                {
                    offer: {
                        id: 'yDpJekrrgZHrBFkD1H14hmUH48HeBLM921a_ykJ5iugDdNyCAbrRYHait14Bf71gspJReQFkDbHoN5dU7OXLwTwgPsl9pkP-7O7-Tk-CcABBvgwtFpKwAt8YFHLmjHxlSm-LLbpI75yCoTXffYpCe4-9M8sgqEMVVY6chPqWhFyZrMaMkSVfvNSg85PI_mq1OKIk6AwLiAqRoty39jciYJHiHCjtehPPZ-Xm-0OAmS5L2Z8O5VSGLLPRJG3fjyLUE5Kexq4s9vGgMsPQINVQw8yVIMGilBMtGLj9A-fueOM',
                        wareMd5: 'ARVKUEYLbMB16MArPJRj1w',
                        name: 'Монополия с банковскими картами (обновленная) настольная игра Hasbro B6677',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8meyofUjCL4PMINn2sk_MYY7EY7Dtt7-uHyIYdPnKiJkTD03zJ-wRMzC5XojOKYhDCQcQrJxjhSYCkCTmGkNyWIYJ6p_QAC1v5Yi2xzATXklwnjMB4nGqc5182hJ4f_fCDZJNilFH0z7oW_nuroB084yv2mcnR4qWJY9NZGDNoK6VxnELYuApMwR5GR2iaEBtgLdQQh_3ZNstuyhZEH7AU0Z0TL6Rtnl_Xi4PTDEXkHb7n30vFdVCPRNTvUtOoBEw4HK_dAsXHfNjKfnSme9-9cWnv9qQhy19Ax43ffXQgeI2RPEY2W_Fwuk6CYiZpKdNUORO0oQSWhsNVJ8f933p8P0GDeo3-SsNLpuxK1tbvevVAnoeqZjJDIm6xcZSM5-nqP4HwkCF0GjzDozAiHzYJ27h1Pq5h92i2Z9DSlR_Jyh8Ck5don_nGKI7WckaJtotcN8-wwMBBLeJUG21TC9gllzw-duxsB3kUclC-esCD9DdTAtmMy7Sz4gvGJtHwvG0XMT37yikrp2HsUndyrszTIQ8WpFXZUDQZ9nMAYjQHa56mRkIMRtAorLu1r1wfBYV5AgykD7XWZ8eZ9WophSt8yh1PSjzr20QLApD4XMswgpJe7eula1DOcR_5YzuAT5UAZkqAPXP_w6oxk-wC6PlbUkrwqrnlKQkiyY46KFjeK0NppkwbHPtbmfE8mlk2EWrxGyO3LhW2eLij9aQ6bo_v8WAsxTqvyGCA8hKUiBhn9bhrKfyQFY63oX6ukeRg0WGjHcZR5ONXHHNnzQxNM_6vxwDe_e96JrK01kD6jbVijgTP4F-Dvgrez-Q-9rnCQSemqOCq3B4CrcsfkyH_yBfj8SC-hXTqd-DaDPClNHUbTlt?data=QVyKqSPyGQwNvdoowNEPjTuL6UDLo_yLuah2yF3m1oAfIbQThK5PUnIWwtHpMyL3l_oL_yuB5Mt141_QeOTxGLYlFWQV8d1B06Sp7nUblTHExuMSvvjoFVRIIEASlFCyKl6WaGCbHg7hqNbSBCnD4Zi576SLigiAuur9ATmSLoM_Bs_dhNktNUpGgbOUuVXHCPjGOk1hjOHy9vHH8GJwhs3Rs8K2Yaz9NL4VwJBBZCpE3IzeCRthoFNYJyWTP2rJDXjV6bNa6LjMEOOTerTVxHXxz0pxfzOU89FHtH001Cd2n92xjGi8joOontHxzHctOXgLtfe2rZwyWZ_bp2cDu2hPunkdP8fWO3S1Mn2XB5h4pqOiZ7CeP86B7v9Om3_QbZuxGkRJAVCFUvf443Qe8Q,,&b64e=1&sign=91eca5565bc9549dfc7644f6468f812b&keyno=1',
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
                            gradeTotal: 238,
                            regionId: 213,
                            createdAt: '2015-10-08',
                            returnDeliveryAddress: 'Москва, Живописная, дом 5, корпус 6, офис 104, 123103'
                        },
                        phone: {},
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
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/216074/market_8XkBn7CJpuVMkj-ni289xQ/orig',
                            width: 600,
                            height: 600
                        },
                        previewPhotos: [],
                        warranty: 0,
                        description: 'Настольная игра Hasbro Монополия с банковскими карточками — одна из самых увлекательных настольных игр! Что может быть интереснее, чем зарабатывать миллионы, руководствуясь простыми правилами! Монополия с банковскими карточками — это обновленная версия Монополии, в которой нет бумажных игровых денег, но есть банковский аппарат! И у каждого игрока есть собственная пластиковая карта с безразмерным счетом, на котором будет денег столько, сколько сможете заработать на управлении недвижимостью.',
                        vendor: {
                            id: 15157809,
                            picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                            link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                            name: 'Hasbro games'
                        },
                        categoryId: 10682647,
                        cpa: 1,
                        cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgcYKPluJnen1N6F9ye0Q6cbFNQO2jsFFD6szxQGR9mr8wIAhbKpldMWzCj_5F--C85rSUsVFbuqGEbgJg9l4MR5n3P-sqXdy2ZhHYP4QuvfVeng3lkAU05AmsSMbtPxtmYnEdAfITdKghAXvWYDxvrH_nHPTUQst0rnnEFz-TI3CqNMQmzm_pEbOsETfgEpkYWSkP55g9WVLozknOICBSUHT564s6T71XhVMvIpHA29cMjkwSOQT-DGcoPB_ZyTajK1MGqTSJfFteK68qOM70VZjLXVSiLcg3rcKFlEyrjPL88-CpcwhW2SYR_gignjq4jzCu4VfFE3XVEw3mhPZEKDqtI3zl9M88O3JYVxXIk0l5420BEwuz6A_VYouSGxUgUseS5AUZ32WtMLCHiho-InUcljVrImb9nVxZ2oBUWJtUqDd8kXD8_psMSyvPY5x6jrpk-DpMgjZqw54ULezZSjfRsNC9sgGN60SOUmYTDB10xbeuDSGXpZ5ad-jXv-yq4Mc-ciJqDEApkMSZ80pzzyV3Ddj81pvmUlYJPGjak3IXgkpJz87LZee1nn-OnzZgwsAuWFpKl3D1AQox6WFKnwTdmhU2aTBwczlrrxbaOYkgJD1UXhOdcXWf4F0k4q2iT6ZS1ixp9X__aa1pUBKHY3UKkQDdqeGBmbfXnKdSZH5kL7prCq1lalyBPE9hxpq1669u2EV9xEER1sUbJTn2AUfeiylRW2zJ0bO_ALlHgHGDNUVrK7MjSHRkO4PdTQUmVnEZulN1Zl4e6qEpOYFD1rdjKxoZjwHe2BWHZIvRrQ85sctvT_jVqVW7Ci2sj8RviMGWQYdcQWwgkYjQeYVQIZR8txSYxeKFze2jThk7E0cmrwkh7lWXRkt7QAuymTN3sZzNciB4uLA?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-7H6SNSns_8EF05dDBJ_VoCBjbVmICni0R4r3KNa2PY6959gNE62fMwEQdqn0Xy1YqYJrDAoAuomLTfKSv04Rm_n3COHHxtwN6EmFncgfmZBVsall3ueEl7wQBXjaX16-LRmIvO5A2WEPOKe0v4DDvy_RVwYi1ThYWR-R7EMkokmMGyzCblReI69PScvDBcy-WMZRaOxrF-fHMiQz859kxBP9ziLNOaHUR8PcfII3_EGUly0AUCzH-qhcFo8QydOd8DxMhHJ0k9XMB5Px5Yj1jSpSE7UCljHkc5UVk7NgTN-li-rzLITi8ZZuKfC7ifLpr0bu-oh66C0g,,&b64e=1&sign=cd569e44f400fa3745b8c3d751afd3f7&keyno=1',
                        category: {
                            id: 10682647,
                            type: 'GURU',
                            advertisingModel: 'HYBRID',
                            name: 'Настольные игры'
                        },
                        vendorId: 15157809
                    },
                    phone: {
                        country: '7',
                        city: '905',
                        number: '599-9102'
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
                        longitude: '37.605499',
                        latitude: '55.767195'
                    },
                    pointId: '15052737',
                    pointName: 'ПВЗ Москва, Настасьинский переулок (м. Пушкинская)',
                    pointType: 'DEPOT',
                    localityName: 'Москва',
                    thoroughfareName: 'Настасьинский переулок',
                    premiseNumber: '8',
                    shopId: 314283,
                    building: '2'
                },
                {
                    offer: {
                        id: 'yDpJekrrgZHrBFkD1H14hmUH48HeBLM921a_ykJ5iugDdNyCAbrRYHait14Bf71gspJReQFkDbHoN5dU7OXLwTwgPsl9pkP-7O7-Tk-CcABBvgwtFpKwAt8YFHLmjHxlSm-LLbpI75yCoTXffYpCe4-9M8sgqEMVVY6chPqWhFyZrMaMkSVfvNSg85PI_mq1OKIk6AwLiAq7qXK6cKid8R2Qv-qjYQtiXlqkp_diHIp_u-Bri5rNx2XGtLVtIZ5MFstCJytdtpbNYfiPgC3iqYhCmFTk2WPdUZEmeonAJxA',
                        wareMd5: 'ARVKUEYLbMB16MArPJRj1w',
                        name: 'Монополия с банковскими картами (обновленная) настольная игра Hasbro B6677',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8meyofUjCL4PMINn2sk_MYY7EY7Dtt7-uHyIYdPnKiJkTD03zJ-wRMzC5XojOKYhDCQcQrJxjhSYCkCTmGkNyWIYJ6p_QAC1v5Yi2xzATXklwnjMB4nGqc5182hJ4f_fCDZJNilFH0z7oW_nuroB084yv2mcnR4qWJY9NZGDNoK6VxnELYuApMwRcfDSqhmKklm-Ncn-dY9jA6feGiteVs-Cvsk7jp4J4e9BEPsmAVp9ArfQSjQsvIArWNyl0549laUn2Cv912vT7Zw17AOCDkgVJpcLrwAe8_FGComWq7pYlzzimtvJEyLZBX-wcKVu42hs8QCdB6wVjrpW8C8whDSSF-96mRCZysOGfISdoxSjXEYqlshKNUzIaD08XGFRfuCxuXs6mx8zbynTv2O19i1T8XlGSA6hhLQNzVMRx0jRlzLDDriYTavJlkhIYo6lSVNBa61pJvU0IxlWLaz_5XM46Y5dDaTJXfQ8yE21-ZwYTzKzqSzvfLMmkD-awhQ4lZoFnB7av-fuQzJrFeqbGifNkmMubMZ9uICbSx2kj_Re0Old4-a0L-gEyHBJhZTckiQA0GwNOIGYblNl_yCGpwk271WH9a6Pu_mP9KTW5L_Cc8PjsxN0MfTWimhY2BBSDHwcHpNSVtt2BIp7vSurKXVEQ5mvhePBU2nVqxyekAE9MRt3fkZWsOxkm7qNZD7m2RApwOrXmdJUfGAT8VXfQPq2JRw425Q0pIz3vKsll9mwEv69aNKpWKmmGSQbc5oXnw4M50OwL-KO_3AHvAbteho4n4Xa3pnNqAO_nQManBq5gQvnViG5T4Uux5SPnIaO-aOV-0FyPU0lNkxXTk94Gt2dwzF-uAmE8A4cwAXo8ewFO?data=QVyKqSPyGQwNvdoowNEPjTuL6UDLo_yLuah2yF3m1oAfIbQThK5PUnIWwtHpMyL3l_oL_yuB5Mt141_QeOTxGLYlFWQV8d1B06Sp7nUblTHExuMSvvjoFVRIIEASlFCyKl6WaGCbHg7hqNbSBCnD4Zi576SLigiAuur9ATmSLoM_Bs_dhNktNUpGgbOUuVXHCPjGOk1hjOHy9vHH8GJwhs3Rs8K2Yaz9NL4VwJBBZCpE3IzeCRthoFNYJyWTP2rJDXjV6bNa6LjMEOOTerTVxHXxz0pxfzOU89FHtH001Cd2n92xjGi8joOontHxzHctOXgLtfe2rZwyWZ_bp2cDu2hPunkdP8fWO3S1Mn2XB5h4pqOiZ7CePxJi-efOv-neGXQE4uK607b8-gm2zAo0ww,,&b64e=1&sign=fc86cbc632c1c9797f463e52e496f338&keyno=1',
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
                            gradeTotal: 238,
                            regionId: 213,
                            createdAt: '2015-10-08',
                            returnDeliveryAddress: 'Москва, Живописная, дом 5, корпус 6, офис 104, 123103'
                        },
                        phone: {},
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
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/216074/market_8XkBn7CJpuVMkj-ni289xQ/orig',
                            width: 600,
                            height: 600
                        },
                        previewPhotos: [],
                        warranty: 0,
                        description: 'Настольная игра Hasbro Монополия с банковскими карточками — одна из самых увлекательных настольных игр! Что может быть интереснее, чем зарабатывать миллионы, руководствуясь простыми правилами! Монополия с банковскими карточками — это обновленная версия Монополии, в которой нет бумажных игровых денег, но есть банковский аппарат! И у каждого игрока есть собственная пластиковая карта с безразмерным счетом, на котором будет денег столько, сколько сможете заработать на управлении недвижимостью.',
                        vendor: {
                            id: 15157809,
                            picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                            link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                            name: 'Hasbro games'
                        },
                        categoryId: 10682647,
                        cpa: 1,
                        cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgcYKPluJnen1N6F9ye0Q6cbFNQO2jsFFD6szxQGR9mr8wIAhbKpldMWzCj_5F--C85rSUsVFbuqGEbgJg9l4MR5n3P-sqXdy2ZhHYP4QuvfVeng3lkAU05AmsSMbtPxtmYnEdAfITdKghAXvWYDxvrH_nHPTUQst0rnnEFz-TI3CqNMQmzm_pEbOsETfgEpkYWSkP55g9WVLozknOICBSUG8AxaNzeJwp3rxQtCaz5ZOsOEA0iEjWZ8m0gQhy-ehCRJQ8cLOC9ootZIlGxZV0cMJHZNRvSyNkpQxSO-RCqo2rDaqsex36kXMt7G0LE-Y2P3lKJdrMxlhH4_X2ksJxITroVz_jCcGUV1f26_dseJPFWONxIaO6CXEpbj5NLrQUsxfUZ822RlLyv66Hp0RCiOjHVO3t6cZG0J_SzMuadCAGgaFR-syuakLihZtNv8KLLIgrUs9qYB65ozbJfGRkRv9VerVnubVtbeiy6j0-vd5o2cfSA0PhjjSCmw8e7kb8DH-PKhbMfNNZcgxPv_XAVInsKxzgdgtsmvF7ardrqQJdyGxqt4T5HvD-40muse_jziplSq2TYZjaA3YdOt5F4AeY-EoTAQabzQqHzEafT6Fx41DilboJVyXQL4GlVx6Vtdo_-aaq5VlCgYBaY9THqhhhuUzsM2R6hgn1SL3HVJfwSeiOK5B6IY5rnmbnfySyLNJ35UUjiHS-1FlNfcVMGOeSoYemY9q3nuhYQUgMZ1m22OAk3vDAhtYTcZLvV7hiBPUDBb8yqQa5830FZp6TQ61n1PB3sVQmo07m53PBGYHCsM0kRfrQdYgmBI2HQ59YHKmVWfRJKwxFZjbqtE8eQP-tufuuV-UbTatTOJRJF6jk-kOucrZz6TSWj9k_ynAm1qFRdtjcqN5?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-7H6SNSns_8EF05dDBJ_VoCBjbVmICni0R4r3KNa2PY6959gNE62fMwEQdqn0Xy1YqYJrDAoAuomLTfKSv04Rm_n3COHHxtwN6EmFncgfmZBVsall3ueEl7wQBXjaX16-LRmIvO5A2WEPOKe0v4DDvy2x0KVDOYeQ8EJWc0aFXEWGXHlZ6BfMiUJqBGnnA6yBZpCwnAJhYHJGRsWcQWzG8ugKUMHl4Cs5ql-wBCP2tFTT8sjEPkHATdhMJqPMHQLdg3PDitPo8SreBVVbJKxYXIY_6cb-3D3bZtJ-gHJdUqa4e7QNXaFw9ncbaEzG2z14c-e6MQz6qc-Q,,&b64e=1&sign=0c2f20eea91a138f11c034c7a34e1e2b&keyno=1',
                        category: {
                            id: 10682647,
                            type: 'GURU',
                            advertisingModel: 'HYBRID',
                            name: 'Настольные игры'
                        },
                        vendorId: 15157809
                    },
                    phone: {
                        country: '7',
                        city: '915',
                        number: '154-4595'
                    },
                    schedule: [
                        {
                            workingDaysFrom: '1',
                            workingDaysTill: '6',
                            workingHoursFrom: '11:00',
                            workingHoursTill: '19:00'
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
                {
                    offer: {
                        id: 'yDpJekrrgZE2mYNSBviHbuggeSk_v1fNKY8eKo9KRDgCiuPPejC8LWgxqEyqeYs8M28cZ-M5tIxJtvhUHiA_RtkVQljPC5ValSXq_3rrccQ14Dy4NTDuRSzXeS6DEpF7ZpjiCM27KlyCVqfE85lWLH21U9RiyPhlxE3w7W-EdKLJtoJl8dJNGJL2zhib82r4Lo222n9JbKj-fIU4jWWGnfw8JY_RNBOJfIkCr9XFIouDj47evK_6cxiDVtLqKU5LYOtKAYmjSmRoKlSCJzPys0tIZizFJ-gDAHsaVNaGXP8',
                        wareMd5: '39QCk2xf8SQCn5k1akAJcA',
                        name: 'Настольная игра Монополия с банковскими картами + батарейки ААА',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdzEzHTpi0PjUP-JX_vivmG_MTcJKXVO_ye4QLhW4uxhUZN5Q0YQYdMHeMuE4FrrsyREBTafARbLc6NTpLGdKsSvCKiwv8EgXE__UJW1KDbaKeYeSV8cjRrLIie_2y55VcvmEyOwJFmVcAtogmb0z0d55PMDCdDsE_yEEQ7olLuC4YrFbd0msNa6iLVljt_Eug3DEspX6fdQ4pICyexgESNPgYM-UcOFhmvEbLVlWRy3xScYB_9qUlwGrpVcURva2xzDmJ3B66KruTU9xOuQO986NoRDtRW0A06wDErn982Z1-NyKvW3u_M9CgL993EAS5lSI6BY3hzswdyYRX1nFtpXwPTNNSenSfkGXbujepJCRtbs6x-uwE4a1eFL7vXgOYKj-N4oz02bzJTm7U9N0LcFlKvJQwVEw4PO_-yuZ71Tyumvy8h2oY-XEpGImxizbPMTqRbvBVj7iiAmrEYLD3xoQ_-4Qs4h1iB3hWTyU69OMHnijKINQ5AvOJpV7FCkHTMyB92rkK5zl_5wTqvn41ZP5rymbcATO3aVUIma6wLciIUesgl6LMW1Wu6NZhj_mmmQBefPNZCWiGVdm9gkq2__DMzTpW3m4oLsMraiG7lI5nzbYadfpW9rmib7zTyuWjeOiriXNspSC9iLN7XHwNG07FA6DtTws6y2xsUGbaDn9D09aZltiLa0gw1Ls85DU_kie2yGwpAuH3SMdZ3Hog8-1PNOXBgkJOlcQ_Pk0vGDwffitScAnJpJrAREBnH3_Xv7hv-vktQ4cgujVR5mh9uPFAdcmvrDYRn95PJFqFrnXo_ux1Z-nCMVXCBzwXwkMc9QU-sovRRvEPKi0umyWTBY80xRbZTrMwKuIr3hYJ9-?data=QVyKqSPyGQwNvdoowNEPjWDqp7eX5kyi0uGW3LKi_s3I9FtEbo-WmTpxp7w_pMxlhsjF5uAjdro1qTMS-7jUbHz7sxW5s84qkqZ6fsspp4GV-yU9DgNmTMvHktQLRy99BWnvGti3An65XmYrzyOVFq3LmDPd6NiaFbe2tVSt-X8_Rq9kwM-zdGweUZ3trtDZenCYX1mPA-P08UCwam9rhIQWOD4JgyeHu-UWDIupjKu4i7ZU4V6vj7tkjGNppG0vP47SO4cgR5nKJJkrCEzOadsG79xn7iQrBi-x5rqyUFs8rg_1_zqAl0wq5YdK0dth12jNwPHqA3M4pEf19lagglmfGQf6kU0yTzxG5YCFJW0YXW2DloJPDCTM0OtRRlNAx3FNKK7ez4-ln2AxSQBmq8c-tvj-uGPSus9wAiskICaHWLvrTvuhpseEAwCJPNV7pezKkIhbH1yRdiID0Dy3IPWH_E439OZv_qe0q-qLI3qiVnphadijh9LRuENVai-Ck_SD22ALhOfOyh4EDOpOw63ACGt239NIT6WZmrlUy1jEMN_eYJWOLl7grEXL_uKF5zfJm_gUB2aMZApGJmLM6jmFce7c9XVh0A0lBLIuy9k,&b64e=1&sign=10e6d4e1cfc6234161c96cf20509d8cb&keyno=1',
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
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdzEzHTpi0PjUP-JX_vivmG_MTcJKXVO_ye4QLhW4uxhUZN5Q0YQYdMHeMuE4FrrsyREBTafARbLc6NTpLGdKsSvCKiwv8EgXE__UJW1KDbaKeYeSV8cjRrLIie_2y55VcvmEyOwJFmVcAtogmb0z0d55PMDCdDsE_yEEQ7olLuC4YrFbd0msNa8LwwyA6ggrHxXGBvNQaHXlPavGNrseXFVY4WQFb6Hu0w-K6jKmhCfcWziSxo2KHyrk3oZPFhA9w_atpTo-kPyyT1hZVTR16V7JkKRvBYSUHgjaEU-xESwCgKu-myDyiqW4Iii4bwTq_ZPWPpScOxNV7nBDzbnJZN4fYcOXZNcY7m9-KDxVb3nm7XzIFoc1lJx-AGOAOkosQGnuPJg2hCV-pqU8h2TXMRVkSnfcbvXNsicDr4608ag6vfzaIu4u8c7J6WXBTKPMg128WXsswVhjWsxJvv9WPWWaE1w3aAChG6AETSRCSS9v5YwBIDfQWfEfpZdLPp2wKMdnHkCMsqKed50rvuYGCK1909m1Ux-EkXSbS_EkxR75iuAXZByw-Q1tOgntva43RpflCn2viVbY8PC7NESAb8M7jD8VxBnxhSpvXmOYV78PTATxU54k76SZGFrpYOnZIIZq2JND4GLWqQYxYhyfEVaL7tlM2rjrnNLYtcgEO2BILWA9jUqhtap0GJXF-dyp27Cbw-S6HZbwTypJQeUkBkTSm1HvvAR8wUuNLzCxandnZ6V6Q44QjATtr_3EoG0TiT2dEzxaNKVcOA1ixt2caAqJ7V1TB4X0VZRZpzoOVJVVjncenSSth-R_X0OnNhyU9lBTj5hQlepg5CpbdtVW51NWK94awc46YRK1M5gUdyu?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8V2DbUaXB5zpqyEg2D0gex8Rj_OE_EbAkcIEWYEBWaYSSkmNc5981oohK8FqyDcIRKUWg8KwFfSVycwizvMX-1ki-r9wfDLmPLRU9e0MKTFMI9iCCyJ6NlH7pnUGGKDNHtWOMRK-XdRUZ8HsUIq6HFgvSkG1LmJqW9vrdmp7iD3w,,&b64e=1&sign=d8b904bf3afc10077cf23141740639a5&keyno=1'
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
                                    dayTo: 4,
                                    defaultLocalDelivery: true
                                }
                            ],
                            brief: 'в Москву — 149 руб., возможен самовывоз',
                            full: '',
                            priorityRegion: 213,
                            regionName: 'Москва',
                            userRegionName: 'Москва'
                        },
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/167132/market_Y7OkIqNg4tIiAFR8D15WBg/orig',
                            width: 190,
                            height: 180
                        },
                        previewPhotos: [],
                        warranty: 0,
                        description: 'Покупайте и продавайте недвижимость, стройте дома и отели, собирайте арендную плату, платите налоги... Вас ждёт успех! От классической Монополии эта версия отличается наличием пластиковых карточек и банковского терминала. Все расчёты между игроками ведутся без бумажных «денег».',
                        vendor: {
                            id: 15157809,
                            picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                            link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                            name: 'Hasbro games'
                        },
                        categoryId: 10682647,
                        category: {
                            id: 10682647,
                            type: 'GURU',
                            advertisingModel: 'HYBRID',
                            name: 'Настольные игры'
                        },
                        vendorId: 15157809
                    },
                    phone: {
                        country: '7',
                        city: '495',
                        number: '668-0608'
                    },
                    schedule: [
                        {
                            workingDaysFrom: '1',
                            workingDaysTill: '5',
                            workingHoursFrom: '10:00',
                            workingHoursTill: '21:00'
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
                {
                    offer: {
                        id: 'yDpJekrrgZE2mYNSBviHbuggeSk_v1fNKY8eKo9KRDgCiuPPejC8LWgxqEyqeYs8M28cZ-M5tIxJtvhUHiA_RtkVQljPC5ValSXq_3rrccQ14Dy4NTDuRSzXeS6DEpF7ZpjiCM27KlyCVqfE85lWLH21U9RiyPhlxE3w7W-EdKLJtoJl8dJNGJL2zhib82r4Lo222n9JbKgjP_cKKs1BYrOYy93Ar8dtK9y4bslIlBSFrIolrfEdf7Jv3nDK8nIhJ1DTpy1oSpr0qwBpC_3YO0N_tJmaKwfq4vUrA_JVwpk',
                        wareMd5: '39QCk2xf8SQCn5k1akAJcA',
                        name: 'Настольная игра Монополия с банковскими картами + батарейки ААА',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdzEzHTpi0PjUP-JX_vivmG_MTcJKXVO_ye4QLhW4uxhUZN5Q0YQYdMHeMuE4FrrsyREBTafARbLc6NTpLGdKsSvCKiwv8EgXE__UJW1KDbaKeYeSV8cjRrLIie_2y55VcvmEyOwJFmVcAtogmb0z0d55PMDCdDsE_yEEQ7olLuC4YrFbd0msNZTkbjvGkd4FdiNKD3USiOefWxap2WgyXxAAo5WllWhGNevd49xDEOCzgbxTBNhxvYWUWWeFY0qESMGIbohCHavBiWOR7bkelEc0ZTSW4O0YID4NZ69IUqhVLW-OQwgnK3_bv4rGqdOHkpMZ4sMO7SP9ldTgjpC7FV8Eh13hxkNtHkNKx3DkNV2bqsjJNVBanVUtW18-5m886l09GFma8og87txkhEO4fZDH1IsHNhZkohFR6FjyKUwcG43mR2Lfq_sMtbgTT6DmYLyt00ViLhoUQokkkMFuHXFLCy06l_X97-XrNG1E87-zhBWkf9WNJkg7I1DLskYKqAX5_PM1pSQYNXf5KUXnUYO3kTVsRcAfzwTKB_kuikClhNF8CmoGROE1bzjkrTbuQ--qIUJj9EZrOyxUwr7PK9Ap_U2OOnRQ887wr3m3Ui35pwOJCsPmHKnvm2RGP-xHHBfF-mGe9eAacxk_fCn-8q1VjWx-og4vmN168bahtgjtTSbnW5tvaojkhHlt8sAVeelXfUnYWkZE0oMJ7iUPMdwYXDYLoFgPOb7lHioKfHEsIS1-mt2P3BKtRYo-cC4uXX93uAPKef8m_TzMaImXKwP5YVwPWKIarxDYnVQze6uMZ0s4sdn1G0s4YHWG-R9dR7M9ZUyCvQRSC6RMKTYmLMtkkcQ9k4rFjK9QdyABQTK?data=QVyKqSPyGQwNvdoowNEPjWDqp7eX5kyi0uGW3LKi_s3I9FtEbo-WmTpxp7w_pMxlhsjF5uAjdro1qTMS-7jUbHz7sxW5s84qkqZ6fsspp4GV-yU9DgNmTMvHktQLRy99BWnvGti3An65XmYrzyOVFq3LmDPd6NiaFbe2tVSt-X8_Rq9kwM-zdGweUZ3trtDZenCYX1mPA-P08UCwam9rhIQWOD4JgyeHu-UWDIupjKu4i7ZU4V6vj7tkjGNppG0vP47SO4cgR5nKJJkrCEzOadsG79xn7iQrBi-x5rqyUFs8rg_1_zqAl0wq5YdK0dth12jNwPHqA3M4pEf19lagglmfGQf6kU0yTzxG5YCFJW0YXW2DloJPDCTM0OtRRlNAx3FNKK7ez4-ln2AxSQBmq8c-tvj-uGPSus9wAiskICaHWLvrTvuhpseEAwCJPNV7pezKkIhbH1yRdiID0Dy3IPWH_E439OZv_qe0q-qLI3qiVnphadijh9LRuENVai-Ck_SD22ALhOfOyh4EDOpOw63ACGt239NIT6WZmrlUy1jEMN_eYJWOLl7grEXL_uKF5zfJm_gUB2bk4Mj9_ZSQmpKSuvC13X_UZ7JEy-E6rDc,&b64e=1&sign=cc0071d4ee6647711778a3c17c4ec387&keyno=1',
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
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdzEzHTpi0PjUP-JX_vivmG_MTcJKXVO_ye4QLhW4uxhUZN5Q0YQYdMHeMuE4FrrsyREBTafARbLc6NTpLGdKsSvCKiwv8EgXE__UJW1KDbaKeYeSV8cjRrLIie_2y55VcvmEyOwJFmVcAtogmb0z0d55PMDCdDsE_yEEQ7olLuC4YrFbd0msNYFq3DJ41f1BOQF7Dj8A2cMCSr4p1LiQiu2lw60i0-ixPGBxbdz7CgOfqikrEsrC5hWdy8XBDTaW-cTOWMbxCZWN5COxs1JwxnBYOPN-o95cQfxdHY9-wCLVZgqu5sOo9OO_PH6J8Yxr-75GOmEgM75PCTdRciV3geCctp-GFrX9tV_UQZBsG4m3JMUzaqZaoTmpEPMgDyfGEZggFryNJASaLoANnMqOnSQvWcOCtrR6QeXGnY026xxhYMZdE44mICuN7t0G4_ZF2xNnjpPHvWlTq8ZTCVtpg8K3VNrYWUCIoWliiI-VLBu6WHy36WBcj8N8b8Q1wmeRy7lGuTOOX-noNJfzSpXwQhJhnojQkKhM1muZYW5H9Zy-ED4smQDpXH5qSWDmWqeiYO9rhWtcCCWAtGCk1SJLp-i5phZGOrtW82HqEr0YgrpykRAyIuC6Sd6s05kupUA-E6BDEUNNk9qXmyixmH1FzqrAE4ChglarnuAF_SCIZv8jSxpktACkGgSlUcxG7-pLkvMyC2-SuWwjnkg-n2LwDvIYMtEPKYINl6uVDpQLoP5F11seTMkRaAFH7eMMP1-TW1f3Psv_v_vuVN0W-Pob1pvG_vD6Oqx3JrArzh8vwRQMcguernWd89y_DSD0BriwH6veW9FF1LHev9YihVw0Pvm4GPrMU_ATAMdXiH6mO3-?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8V2DbUaXB5zpqyEg2D0gex8Rj_OE_EbAkcIEWYEBWaYSSkmNc5981oohK8FqyDcIRKUWg8KwFfSVycwizvMX-1ki-r9wfDLmPLRU9e0MKTFMI9iCCyJ6NlH7pnUGGKDNHtWOMRK-XdRUZ8HsUIq6HFgvSkG1LmJqW9vrdmp7iD3w,,&b64e=1&sign=6ff43469f17e290f7ff9ca5e56fea614&keyno=1'
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
                                    dayTo: 4,
                                    defaultLocalDelivery: true
                                }
                            ],
                            brief: 'в Москву — 149 руб., возможен самовывоз',
                            full: '',
                            priorityRegion: 213,
                            regionName: 'Москва',
                            userRegionName: 'Москва'
                        },
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/167132/market_Y7OkIqNg4tIiAFR8D15WBg/orig',
                            width: 190,
                            height: 180
                        },
                        previewPhotos: [],
                        warranty: 0,
                        description: 'Покупайте и продавайте недвижимость, стройте дома и отели, собирайте арендную плату, платите налоги... Вас ждёт успех! От классической Монополии эта версия отличается наличием пластиковых карточек и банковского терминала. Все расчёты между игроками ведутся без бумажных «денег».',
                        vendor: {
                            id: 15157809,
                            picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                            link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                            name: 'Hasbro games'
                        },
                        categoryId: 10682647,
                        category: {
                            id: 10682647,
                            type: 'GURU',
                            advertisingModel: 'HYBRID',
                            name: 'Настольные игры'
                        },
                        vendorId: 15157809
                    },
                    phone: {
                        country: '7',
                        city: '495',
                        number: '668-0608'
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
                        longitude: '37.598079',
                        latitude: '55.751467'
                    },
                    pointId: '663183',
                    pointName: 'Магазин настольных игр Игровед (Москва)',
                    pointType: 'MIXED',
                    localityName: 'Москва',
                    thoroughfareName: 'Арбат',
                    premiseNumber: '9',
                    shopId: 4014,
                    building: '1'
                },
                {
                    offer: {
                        id: 'yDpJekrrgZEGqvpvMGyhJsOY3lroaO4xOzk2g1PKSc5xit7Oqre4kg',
                        wareMd5: 'j1Io0n6j-rpRKvfdSEmJQw',
                        name: 'Настольная игра Hasbro Monopoly B6677 Монополия с банковскими картами (обновленная)',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRudsi_rLy0uCq6VFAm5gECugie2Mb_CEB_jOe8_Hw5qlmGaufW7Q7GBeo6BVkWH5COR20igejMAtmetB0V9USyQuYEcgJgxIsEC94BTkGIlBM4bV260Fzi2Nv8IhJQM-gytmVIH9Zo6p6K4OuKuodSyVBOS_dV0nDMrli9Jli-2o-ZkjXhnB7nrSxrg1eIBcC1HVDKgc7rFyjJpYqW3u0RSal-zBTm0xMLj5sdmjemeyWxwzE-vbLlJNdCtC1gyO95TODreBzfkMzBHtruqCPaUQ0ggspvMagh_3Gniuvo23TSk0loeI2O7Dguwd60ezcU7j3qzqgLMyo8DoLrFX3OOdX1HNf4148gG3RB6d06v3of7Tr0AjUG6iOJoaONVObWkxnobxuxqzfZkzxZCU5Ov8zxDHG68G61P5_jFusG09IgyjqAl283_nSEKaL5lFhuOwrJIZ00y6X62nFNlKr30MkyR6zPBxwY6yYJcJunUv0iJIVGy0-5TRlVOfLZk5hjMeXIJf280sE1knN-e4OQ7wcdIzpi8t-oZBgvJ6hRCDPdt1t1yrWz2S4tCWS6e5-QHeSXzERXpf2fOT6GOWGLiUHJOSUZOjbm0erX_nDaAfcSNxr-p_FXy77L5FA6jVoJcv3YAanDejy5zNR9e2YDwuwRNyGUWq_okdShu0DYbjW8gqbf5rX_D-ntX3MAP_qgYoAa-g1UHqwAfkQcPaGu_YVjIokUNFWY8fw38aGwvF0KVKl65NpaIkUgiAXcCD8xdEicBhRSCrmnrueLp0lrtD3y-P-06qqlhx1mdhPmbYAfJ6sirpGHjgpBmNXeFPrQ1iuSN6PxNqA65BgX050Eq8JLUqBO3y1Fmr5N1WZYgoHtSCogqoQQ,?data=QVyKqSPyGQwNvdoowNEPjRcd1X5IkopGx8TOY7lHsB2IzdZCVgSOE4bzcsD5Zw5aRVUE3FYAfxuvVRDDe5Dkm263fkWIh555MhjCV2krHHOdzkugNJDnXlpKKbmZsbE2VXQdtOFa_Xr445lYQmqJcO-dQaCuUIZnpg4fT0QYdP5PPdZQMLD2TpU6jadqKUFhYt1sI_x41vrT8JAdJKudmTtfvRFNCAakJelLNDRUEtl-fuuPs3Ux-og5t0Z5hyOjdfOgMQvFf8CrSgXgsLvBmVM9bn2OB7nsAdpMG9WT36Pa_-FLbQfwg08aMGlLH5X1IlgWjpYzt6RqIh8ELcTWQLMrPUyZZMGwmx_c26sqvV0ipizdXs9eARvhEPQ8evDg2oSkGx-zN8fJZlJzYTIk3HXEFr3LR9CspnZ853VIc28wbXIi6UaQ3qH2xlKfVXy5mV-lOwAalhGTAlb26p9WG8FWspQAdwcuQSt3s7HhtjhMb8ijubnzSKjjePiMPhJNTxaV4NOCvx2gVvCSeOPgDRUMpPAPWws3iDzx_-cqcqgoWvjrbkpJcuGWXTiabCTdUFM1BzTi5cievODhhoM2_WKHord9jpHM&b64e=1&sign=03a61475eb242945c1dfa558aeb31a9b&keyno=1',
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
                            gradeTotal: 5424,
                            regionId: 213,
                            createdAt: '2011-02-16',
                            returnDeliveryAddress: 'Москва, Профсоюзная, дом 118, ТЦ \"Тропа\", 117437'
                        },
                        phone: {
                            number: '+7 (495) 215-22-44',
                            sanitizedNumber: '+74952152244',
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRudsi_rLy0uCq6VFAm5gECugie2Mb_CEB_jOe8_Hw5qlmGaufW7Q7GBeo6BVkWH5COR20igejMAtmetB0V9USyQuYEcgJgxIsEC94BTkGIlBM4bV260Fzi2Nv8IhJQM-gytmVIH9Zo6p6K4OuKuodSyVBOS_dV0nDMrli9Jli-2o-ZkjXhnB7kTwLNAkbfoZrynjjaSGobAVbAYN7JLZb5mqdp2JvIgMMOGkBbD5NiaXLaUbqc89hTjB2-bOd0G63ciO3cmM0vIW-2kVdxrSzgP3pjkz_ol2ZzLXtK-eunsSjkfnP2LTmr50EFn_NB5e4iv4cSQoJRutm46tSslnzmgcvBBMvZMPG2EEH9GHzOKu9RWRkOn-HqYlQFMchTX7WC2_HBgGZbZTz7LxpbNR3zqZtJzGN9GFZ5WgISh_BrvFl0ubJPcOTTk8Nm-_yEYmzDVwJS85bPlETEDCO7IJtIXAU4vqIOU_YZPulTpUQ83hHaDXGNoJ0x8HG3zmhJ5ZUQXWXt1hCWCrjQQ3QXC_5EDandFnhdypjeSKHWzUN5xVxW99BWf4t9d0sh7LgRcPHexa6-2W4m91jaVwMMQZtB4zxbJUN_Q0d_g2SPuzTYCPwg1IWhGudXJ7bK94q4Zwi85jmy8XhCfYRsmFmjn3O_y2doVZkVno-sWsExdZ8dCMd1AqwcDctMKa6lT_cGqlhu1goTyRrS54q7SgSm0SbUsaO52gK8qyrTpQFnRVZt6Y7VRwCwiRSTAZbMD4h9gP3oTTK79_A0iflcTZU3F6hpPVwOLqQJvA_3COkfoFqqMXS95Iv6UdXGum_9Bp7y9uTReqIWWq60s0C0FQwEh4RwONuI1z7g_GE070Wo9iiebdgWDbr8_M-A,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8OuHeDkJTcsCf_LUOLG-9WaYJgkP2nUT8sIzJW0sfhp9ZOvAapB7Kxy6kU_HhZskDF8ZM7l2yHkX2-RZo1xQ-XS3YNOZNf-LG4Hm_9qtI5cipxuxUPVKqArCx04Mx0p8v7L4sdSNLna76F8lmUZmW-uP2FEk4YmyGJwVqFG93QoWAQVkHqKQN_&b64e=1&sign=d6181f185de6ba0e4b59e488432a1651&keyno=1'
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
                                    dayFrom: 4,
                                    dayTo: 4,
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
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/174581/market_W8f3HiEC3gNz0zdMup3wDA/orig',
                            width: 600,
                            height: 600
                        },
                        previewPhotos: [],
                        warranty: 1,
                        description: 'Представляем вашему вниманию легендарную настольную игру - Монополия от Hasbro обновленной версии! Производитель окончательно отказался от бумажных денег: теперь расплачиваться за покупки нужно банковскими картами! В целом же суть игры осталась прежней - скупайте дома и недвижимость, стройте дома и отели, зарабатывайте деньги и выигрывайте! Монополия - замечательная экономическая стратегия, которая способствует развитию логики и аналитических способностей, а также просто станет отличным поводом собраться в',
                        vendor: {
                            id: 15157809,
                            picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig',
                            link: 'https://market.yandex.ru/brands/15157809?pp=1002&clid=2210590&distr_type=4',
                            name: 'Hasbro games'
                        },
                        categoryId: 10682647,
                        recommended: 1,
                        category: {
                            id: 10682647,
                            type: 'GURU',
                            advertisingModel: 'HYBRID',
                            name: 'Настольные игры'
                        },
                        vendorId: 15157809
                    },
                    phone: {
                        country: '8',
                        city: '800',
                        number: '500-2370'
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
                }
            ],
            page: 1,
            total: 1434,
            count: 10
        }
    }
};

module.exports = new ApiMock(host, pathname, query, result);
