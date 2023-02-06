/* eslint-disable max-len */

'use strict';

const ApiMock = require('../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v2\.1\.0\/search/;

const query = {
    text: 'Настольная игра MONOPOLY Монополия с банковскими картами, обновленная (B6677)'
};

const result = {
    comment: 'text = Настольная игра MONOPOLY Монополия с банковскими картами, обновленная (B6677)',
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
                total: 13,
                totalItems: 374
            },
            processingOptions: {
                checkSpelled: true,
                text: 'Настольная игра MONOPOLY Монополия с банковскими картами, обновленная (B6677)',
                actualText: 'Настольная игра MONOPOLY Монополия с банковскими картами, обновленная (B6677)',
                highlightedText: '',
                adult: false
            },
            id: '1517317294826/8ef8e409b69ab03dbd1c1b963933219b',
            time: '2018-01-30T16: 01: 36.366+03: 00',
            link: 'https://market.yandex.ru/search?hid=10682647&pricefrom=1150&priceto=2299&onstock=0&text=%D0%9D%D0%B0%D1%81%D1%82%D0%BE%D0%BB%D1%8C%D0%BD%D0%B0%D1%8F+%D0%B8%D0%B3%D1%80%D0%B0+MONOPOLY+%D0%9C%D0%BE%D0%BD%D0%BE%D0%BF%D0%BE%D0%BB%D0%B8%D1%8F+%D1%81+%D0%B1%D0%B0%D0%BD%D0%BA%D0%BE%D0%B2%D1%81%D0%BA%D0%B8%D0%BC%D0%B8+%D0%BA%D0%B0%D1%80%D1%82%D0%B0%D0%BC%D0%B8%2C+%D0%BE%D0%B1%D0%BD%D0%BE%D0%B2%D0%BB%D0%B5%D0%BD%D0%BD%D0%B0%D1%8F+%28B6677%29&free-delivery=0&how&pp=1002&clid=2210590&distr_type=4',
            marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4'
        },
        items: [
            {
                __type: 'model',
                id: 1730875803,
                name: 'Настольная игра Hasbro games Monopoly С банковскими картами (обновленная)',
                kind: 'настольная игра',
                type: 'MODEL',
                isNew: false,
                description: 'настольная игра, тип игры:  стратегическая, экономическая, от 8 лет, количество игроков:  6.00, материал:  картон, пластик',
                photo: {
                    width: 701,
                    height: 542,
                    url: 'https://avatars.mds.yandex.net/get-mpic/372220/img_id8335316585521998506.jpeg/orig'
                },
                photos: [
                    {
                        width: 701,
                        height: 542,
                        url: 'https://avatars.mds.yandex.net/get-mpic/372220/img_id8335316585521998506.jpeg/orig'
                    },
                    {
                        width: 701,
                        height: 511,
                        url: 'https://avatars.mds.yandex.net/get-mpic/199079/img_id1963262101063688307.jpeg/orig'
                    },
                    {
                        width: 606,
                        height: 540,
                        url: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id5127913622036598258.jpeg/orig'
                    },
                    {
                        width: 585,
                        height: 407,
                        url: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id6479697318596369358.jpeg/orig'
                    }
                ],
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                price: {
                    max: '4687',
                    min: '2194',
                    avg: '2799'
                },
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                rating: {
                    value: -1,
                    count: 0
                },
                link: 'https://market.yandex.ru/product/1730875803?hid=10682647&pp=1002&clid=2210590&distr_type=4',
                offerCount: 39,
                opinionCount: 1,
                reviewCount: 0,
                offer: {
                    id: 'yDpJekrrgZEG7IzynCyWDC_semcfO2F8FzSzFtWDWSlTLEzq6TScO4NN0a4a-G-LKJxf7vk32G4TM6Ri-LOpkGEEcPESVs62ok4rdaco5Muys0G7-Xzz7g8LjejVsQ_cIViIiq_JpDeyrtpAzTq1MXF8uFmiR0eQROuZRSx5LPtf768JuHw4voqdYpa7PusndwAbPb8n5R8fqncbkJUpFngnSWWNmpcmjgDbU68deLUgNB4Xuy7skQyZtAvgDeHgFTFbi4kkF8EnDMPjPvcNYbxpFnRX82zgi_4GG9tzkk8',
                    wareMd5: 'xwddqN8iiPWRCMoT51EGhQ',
                    name: 'Настольная игра Монополия с банковскими картами',
                    description: 'Монополия – это логическая игра, любимая детьми и их родителями. Возможность потренироваться в создании собственного бизнеса, работа с банковскими карточками, разорение конкурентов – здесь возможно все! Да, вы не ошиблись, эта версия Монополии отличается от своих предшественников, и в ней есть устройство «Банк без границ», работающее по принципу банкомата. Это намного более удобно, чем играть с большим количеством монет разного номинала. Попробуйте! Вам понравится!',
                    price: {
                        value: '2194'
                    },
                    promocode: true,
                    cpa: true,
                    model: {
                        id: 1730875803
                    },
                    phone: {
                        number: '8 495 988-29-15',
                        sanitized: '84959882915'
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
                        brief: 'в Москву — 350 руб., возможен самовывоз',
                        inStock: false,
                        global: false,
                        options: [
                            {
                                conditions: {
                                    price: {
                                        value: '350'
                                    },
                                    daysFrom: 4,
                                    daysTo: 4
                                },
                                default: true
                            }
                        ]
                    },
                    vendor: {
                        id: 15157809,
                        name: 'Hasbro games',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                    },
                    warranty: false,
                    recommended: false,
                    paymentOptions: {
                        canPayByCard: true
                    },
                    photo: {
                        width: 700,
                        height: 700,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_a79gMhhi9K8y829MqRq1mQ/orig'
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
                filters: {
                    filtersList: [

                    ]
                }
            },
            {
                __type: 'offer',
                id: 'yDpJekrrgZEG7IzynCyWDC_semcfO2F8FzSzFtWDWSlTLEzq6TScO4NN0a4a-G-LKJxf7vk32G4TM6Ri-LOpkGEEcPESVs62PK7WYr8intVLkKb3KK8mlOzOf9a8nwXpRpIIw5FCvKogzU_mFRdw-aiv6ZC6iynies8PkJqexEW-ZLdwSjVM5R-g5v36fWx727-cSm-T02irHIILC9SxN-IOtQKkNuFZaWc9XsDIuJshp7-x0W3c1ZvEi1fGtT3odpr_9H1K6SW4lTmifvH2IJ_w_RKlJ8X010nJamKNgTk',
                wareMd5: 'xwddqN8iiPWRCMoT51EGhQ',
                name: 'Настольная игра Монополия с банковскими картами',
                description: 'Монополия – это логическая игра, любимая детьми и их родителями. Возможность потренироваться в создании собственного бизнеса, работа с банковскими карточками, разорение конкурентов – здесь возможно все! Да, вы не ошиблись, эта версия Монополии отличается от своих предшественников, и в ней есть устройство «Банк без границ», работающее по принципу банкомата. Это намного более удобно, чем играть с большим количеством монет разного номинала. Попробуйте! Вам понравится!',
                price: {
                    value: '2194'
                },
                promocode: true,
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZ4H9_aAm-SdJZka3mj1m0rz4u_ObCN_7QeQqZu1hSPPwsxp1_ZJ4LNM90zDdg9WLTaCqqdgY6eVJ7hSd7-dSKYbNo_Ias1hQnHv3ZAAmwPg-qAokUR1N7H4_cVLsIybs4RA6hWuxwI-yG4iB74evaqVO8Wn3V2G8waHOSXpBh74RiN8fDn9UFc8hWJNKODlBAsmjjsJrNmxKKihR_4HEJi-5G8dE4KINtZ6J9j5hRDptZEf74lhT8zYkkLqVjJldWWJLHnBgoGYx8EzYaypqMgt9HrUWSWDU9-aikWQ-I-whuEbpHYdhd_LulNxeT1DI0I2cE5JyWAwnjC0trezLrBSsuqhbS16LTg2-ciBY1_f-dGTmDxy4S5bZ56ZFhakiFYROeYmyniH8aGECqzq8PSAzUmrH6E3BKKtXPE7OeAgHMkioCvxPLUDKbIM2_YILDNBdUNLeSMz0Qv7TBOYABUPgkj7Vqzxyl0Dem8oWUxtgP6MUqi2ER6LIZA7mp3BhiK3h2h1HgvrfH6wi8BvS-pc1qBD4fvpo55M8SMqex0WFidPHCEqz5ih7DE_VE1kHnxfwKXtWmTCWsKqNxHVQzRV5TMPK99gE6DJbYEaTUDoG7d_72SFDYGBKq7ulhmD0xvocFTukcrBLVp7WAQP2MIOWed5f2DKpJ2vm-ESDVHtsrgdAladypKndNOUsX76GpkHsgsn_VxjDYJ8k65tcWn8gKL8t6UGkXzyg5uMrEkKjMAFZb8jzIhpPB1SbEF9PKjTN_YXFoAoWJIr8ULZx_7nM2Y0zjHNy08CsS0Fdjrgxh2jRKrdVstnIadvbG3o3lfvfKqRrn7cxp7mTUlXilZN5O8cO-1ZGgqkZ6ZnEgfRNHmrt47kUXY,?data=QVyKqSPyGQwwaFPWqjjgNi-mXaTHyrM52t_arBPnUZxN-zwz-lEEfwwU7CeiTH92jwassm_aW1Sn89gDmu2_1qU2AgnTRCZFRZuYP7IGF_pX6IR-ZoK9x5eFzCQcp2OwoDdJkp98fBwMrtNWFpDPg9CzWamzEA6Af6TTx00xpKdRyrAjZSW_FTZQa4_vLKKkd2IHsKGUl9UamQ7Qlhk03yjGrKLkBzNgWkoVSpo5D1L2dBCB_BB7coFK8xU-ceHPHXGot3AH4QQT8a9j-zl0g3ZmaH1phDxkNoC880OEo_pSd_5vNyhsitqKHrle-_5m5WpfPv2OMYmaYYcTPKF9305GBgwfocV52ackWu9tnR3P9NTuj-gHLXt_Ey1mdT-wztCOPDtglSugEZ2rlCDXSUyVejULB5c37QJ5Y0iN_Sbm1ebPhqfa7yJayENc4pIHom17gfElA1rK77xjM7I-FP6ZD80Z3_WxCKz22YWqe2khYlEmLXMdN4B1CWrmfsf2_8ctiYHmLtrl7zbG9kegKjoVtql3Dy-Bbxo9XNr_c8AEShNll6gzwvMVmn8dfrvPDokjsQ_n5XjeNhPeHSSU1HcDgsqs_w72rdAJKQgP2to,&b64e=1&sign=859a6ba5b3d94157e4b3eb61df00c9a3&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iZQe0ObDqoYkrQR12z-eNo2fzbeUWAIp8FQlHvrPvXLUx8OAL4BpG9dNz2OJY5ghzel4n3slyipUVWG0OGy_4zAlm_CeCcOkC3Z8dlSwtlyy_fohEhJ72tccMzwHPigw7X1x_TBt260Uvwqr_hIqCdivgwJFEM5cunj6MDljwJO8XKznHJnRTw4FvKmmFYBDjuWmOsRKQt5wYsTqa3out_w_LvpVSXW7G9WOnTVKFfHkdQ4rmO-e5lc4RB1W4RA4UktCCkoKdgobvpeZkdc-j0JYVxPy_AIfz8KCFGGnUH08qFusOB_3oQnektG5UkSNdq1m4yODA2Fv8px41OFobqUNueWX0LqWyy8JRBH4vLFrANrLB0fJjg2q5sJZ_Z8VhDjs5886GlCcHJIWB8OsgGrTrfNQAeLGDTS3gMxWPYel2HlR9DKEOySw6iGisEswaQK_Ld-xIR3PHuPkSiWlbJOpn2mwQFo-WhLlaKpgnC0xhL8HxpexkqezyutKDJ-jlG6zifvvHV6rJ8hsnfHdf5cxkFMKQeZKI3-f8rBdL7i_ysSFMs2P5dHgVLEVvROX7ZdqQeRrCsi8R2fnHRwhYm8UbupGfiHKrauvHr3m93mWchV_tJ087o4zqJy9hhESjvioupa-YB-6doO2dAhPKXstMCV7JrL_gtqGTDVG8yHQrvgPjYWW3JujQ-y3DbT6-xbb7AvJhDtvVoYV4HyBj-pVqsWbKmgnthiCuYIRvDfhixgeuaBIDBpjznfCXqqFe5wXCO9ZQwbQRqwHsUObEtAVhEje4Vqfe5W-_Ded7aRHA35uC1_EvUmLGChj-D6T_AjgVPDQj4aNjXRIAsxo16DpWvlkVDQGNtU8wAtd54Du?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2R_nZUIr9DPdgJJ1H-OEAL_gJMRbq5N0hDYR0cqoMks0u_prwobL5fFLmmJ7jBJfGKhBqLIuflPeebT1pxQGfHnczlny3O5LMdOIR4rKlqOhwEiI3OARuFo,&b64e=1&sign=43f4ad49251d672b5689ccfbe538883f&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: 225
                    },
                    rating: {
                        value: 4,
                        count: 165,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'ИП Антонов Игорь Борисович',
                            ogrn: '317774600091423',
                            address: '125315, г. Москва, ул. Часовая дом 18 кв.72',
                            postalAddress: 'г. Москва, Щелковское шоссе, дом № 100, корпус 107',
                            type: 'IP',
                            contactUrl: 'www.abtoys.ru'
                        }
                    ],
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
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZ4H9_aAm-SdJZka3mj1m0rz4u_ObCN_7QeQqZu1hSPPwsxp1_ZJ4LNM90zDdg9WLTaCqqdgY6eVJ7hSd7-dSKYbNo_Ias1hQnHv3ZAAmwPg-qAokUR1N7H4_cVLsIybs4RA6hWuxwI-yG4iB74evaqVO8Wn3V2G8waHOSXpBh74RiN8fDn9UFd64Zbmbq0sbQW8l9wjaEyJ-lAHdSIawusOBO9VV9nkzVvY1PfNL52JHgUPG9MVFBT6r2q3l9_Ov47RLKbgU4gKZTSIRVWv6Fdy12_2RjxGFQOBM5NToY6YK65_NSHJpOWeuDDE0Lyq69qXuETwjNZLeg5GEKo1P2ZvIkFRLE6_-u-RmazSKGrD7a2hCfOascTDtqBqaj_8HB2uBtWPaG8kbz5J5fCUd4KXA3TLUBBe3shEoCOYvWtxcMqna2X2vY5r4h6gdQVUTrc52xKpx8kj2ToajtMr5TxD2DbRaopeaLwvlazU-c545nEmRkFkJBDxcFLIE8AGqD7HWEa_gH_7m1ScKNk6a5FW2WIH-OeBui7fqwI9A88gpHPYW2P4y2Rv8CXyMhTSuVbZBx4GGAuyWY4V61f_nEMQhRjl1fT6HasvvjZKU06Bpd-Fsu9vC2i9DFFzCqT_gA1RQyXm5aMLdfTOfc_wBK9m4UjI9oGkIfSDkZSRrh1Je0Mz5dwNs2nqtTLSQEaXJOKCTh6QgMSkFiwqHhzvnip6p51ek6CDXCr2r6nNfTnFb_DIyOjAnwikEv2F6SO_8gVpwea7Unn78Y-Glw1ZoMScBMVwcaA8vRiWEPLZHeZ9gi9RsWrCM6lTO94jKc2B5jI5hBPIYysNov-pzdG8vjx9vtsc1iz8mDAaLAC1rYwTp9lvru2b3-o,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8-HoOEA6DgyvI4tx0cM2hcVT2YubJOBb82dxSP87baUUoMnO9S5X4mpDPDyEEGTEBHB76JvlwZJXRxFT7pmn3kU08cbWOpLH-mPPNWh9kdltVLnKXHWlkHuc1AOvJt8fx-UjrbqeVYRUYTkLC63Ni0KvBVdjVRePQ9kbEK9po6DN0Pngh-lIBi&b64e=1&sign=08ceca4c7176ac032df8b6033a4c5e44&keyno=1'
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
                    brief: 'в Москву — 350 руб., возможен самовывоз',
                    inStock: false,
                    global: false,
                    options: [
                        {
                            conditions: {
                                price: {
                                    value: '350'
                                },
                                daysFrom: 4,
                                daysTo: 4
                            },
                            default: true
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
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/xwddqN8iiPWRCMoT51EGhQ?hid=10682647&model_id=1730875803&pp=1002&clid=2210590&distr_type=4&cpc=MLuS5mRTitazvXpnsq3cDU6xyvw6lKjNpKTJhfOAUkcRq_1WvDDpUgpai-E_rnrN264NMUSANbzNc8CTDhKoEEoAC1OrDU3txfM5FGXq-kk7Vq37l5m6ftOW1g_nfatKtLPt-pbL9h8%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabcR4TJfVe7AXprIo7-tdc2WyJe5q4eB-zdvYVBTW5u1gqgvI2279JTdjrE8yc0p1KV2c_xTWgAaVFPp41MJBNuSlbq1TXQ2PhyR97SZ_6I5z7hgHRBkbMIMhOBvHSLHCeMzo1OjUgxfDHp8gLSHpT-jjx5uyK6yk2tfPYTumK69_HzWV6HO0q2DYejN2VDwtC_7e3Q6StXt4b8T4m16Ge35NpkfFMsrr5i8iYE8c_zgpRBr2rQrL8wzWCtiVqkGCbQHEOODqk1ULBPYZy_TlY2NLuetzUlbt_Ouf5xfKhYF9CWTIKd2QNKhKVoMYRu86VoDtZdFZh5c-ujeTHmt9VMADzQeaqM6PH3lFhIn7RTmDNNhD1HfC3pfe9DoI6Ra-1PwQdbW-dFCoabLMpNwBVYZR3gzI0tGW3b_drV9CgF20WRu_pJhD51wiQ_HqtZgeS0RIO7gKUZhHzCWix2xosRlc786XnDPQq9QMMutymeD8d0YwQHhgK94EpkUd_p9nYybhSwb3SiRH__RGow1crM2vKLWSn0h9W0xfwpsoVDK0GyJ5dg2tvgbSvLNYfN4J2RilX3AqTXAZI6bQ1Juc_k5Jigp79PYSmVpm13DB_ypUr3n1Jwco7I4nVHLTUbhHMgXTF524ZrM6l0FVDtspn8xpN2Vs-4zLSUuIzOlmr_q9rHZrsFpGdMR2oqQixnEJ5DJhVWDYPPr-j9ihw36eDTKOCi-nSnRyZmcmiNEh1B7V3U4y3DzJBBcRXhM8SNmFyiiRmfGOEEExKF38K1OQ-2IIRcz-b6R9IvuASh4b_sx58u7D15v0FGLBY61pjVj0F-hhGzpCIzb7R7p5mjl0zi5f0045JbX7XtgWZ6FPmcpht3K3KrLFSm3arGYes2FEUnq6-UGwYudw,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXeYg_5DdXLbZIv7V-DAqh5ht49ofYKl2zRuR-WmYHuhrwoJ1T95yAk4R_T3JgyCpOFYl9b2C0v3cIiYDRhJzpH9kWLwvlnw-fRUkqb2Y-AM543tbZ3lxFnRKb2ZELCE6ulR1CdOkNNSPHlLIUSlWo0Uoz1JVbLcLqX2xWDQWmT9GmMtERlXmFc-5x-02gKRq-rVSSMCkC4gGoKU-Z80Oh4f3rxVZPKWOImXEfdg_-WZiqj0xn2B_ljGPiOSHsL7fcuFApq9EW4O0t9PobuZxnTn_iXkTGWvrGUbxyBYSCD4W76_Nqkx2sog,,&b64e=1&sign=b347a09d1ee278c448b2f46658af7bfe&keyno=1',
                paymentOptions: {
                    canPayByCard: true
                },
                photo: {
                    width: 700,
                    height: 700,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_a79gMhhi9K8y829MqRq1mQ/orig'
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
                __type: 'offer',
                id: 'yDpJekrrgZGvdSG4WBl3Zva7m0gZ8RDohljZnpNWsyLmg94V7m7XXoO6sX5_36d9MVse8-_wqT3EELF5LtCjtCMBkR8fJjXTAKgEfEHDaF5t4HcATJ6xUCu3I1WvkjF3X06ZQRyGVdm4B0kA5R1PeqW62dxCsaGoyyAzKjmPilSBmKmJ1wP6-zOhieVtDP4WcRmotvKyHpklNqu8PLK2zTHORRwPVi1OdW3KAUaRwOgB5DUkDbCeHlBV_iXmuKdrg_LTKVYcCgw6Z4oq98pb9RoQJS0LGc1_lrov9xFN9Xw',
                wareMd5: '6cl8Cj66gtEMxlPMNh2g1w',
                name: 'Настольная игра Monopoly с банковскими картами (обновленная)',
                description: 'Настольная игра Monopoly «Монополия с банковскими картами» - это обновленная версия полюбившейся стратегии. Яркий дизайн, широкий функционал никого не оставят равнодушными и подарят массу положительных эмоций. Особенности набора Суть игры состоит в том, чтобы участники собирали бонусы и преумножали свои накопления. Для этого необходимо кидать кубики, приобретать недвижимость и собирать ренту.',
                price: {
                    value: '2199',
                    discount: '19',
                    base: '2699'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mccPxeESdeTJete6-f5KDKAuXu9-aiRt59-6yBls20FMZaR3uUHwtBujZqQRkKbFczVFOMN_-4yUBd07pcZRGQloKCMkdhWv86xiiNU2Ac5LUAJ2Qz-UwofBoOXA-poA6_GpY1KKPSJ0yUs1_g9G09OOaupN4a4t_TVKqi0OTjmI73ziaX2K-jgiTEeHNaFdfpsM-vPfL1K5WwtkPtQNKsJva9ENiVGwDTos6iNHSdj7GcaaVensn8Rx8D56_6fuN9wG35qEOIbrucAjN5rNpCesp0Oq4Qi1ZkktbKDldzidjF5PVSSPbUIZ2Zsw0wFVqNjeTCiUKz8LTh4ybYBCmCnaZEpxfozqyw_Q0W5B6bAbmNKb3apuE-0vva7awOR6ahlrcb6BZitCxjI5z6RBOrYX7OdP6HmJG17XC7XzQD7ldg6yp3QYjJn1nBruCKzBzBzySzbDExNGjjTtyZvSmQVzDp4Ngi568vkqF5pHSbaRgdZumhmzr2jYF4QZZzaLSjYBOjFLQCJy-9AnW1V5N0UQhJWCX6T_1vD-03_thz-x3ny7lzQfQJlFWio0AXeZ9hKx_S9KCKV6DAsFsgo-q0j0RJkM5RCdm737QzbcsCD8NDZ5aJGhxM_Omygl3aEqIeGo2U9XEXqphUnYnNQ-voAWAOCMr4FMO1SsCALo2YNNDdGpm-jTuFQNGkG_6EBMi6UO33-QV3LWt7lqGYbWHZlTW5NJy5ulDzuH57x5X87N_-NKKxaP2w2g0fVe8XJlLHGhAfgyujXvNVhB3Uq0v5bfbHqmlwRA7BUxxtInVQMB4DIUubRsqR07Cp0Qad5gimDxcaEmJ71Usrwf3Le5J7Kgb5RnCC4T3ylf0Nklk7Umss6XzstrTXSIbhR6S6l2JQ,,?data=QVyKqSPyGQwwaFPWqjjgNrSTXEpR9O8Xwc9ZJ5kIj4KSmIfoqRylMqn7_T9axhLUOxi8ITmzhby_xqeLrNY3X37Ozc_zT0EzHhcLTzCMdvRin_MJfR-wcUEzrPQu-mJKsIREvp3DmQCEbxpy5DBYRr6smvgF3OmqV8wwAFn0cnU2AHVREcCv_hJTZKGqFRqSpoe3HfWYj6KP0vUjmYjswu6BfW3Rky2RDZZnLkI2O6UF2o4RQ6Tcbax6B8upRAykvAZti9rpkAMEsxRxCVtaXBuw4TZYLRttdZk-uVkdQq4NPD9Fa0lfTy2Wyp8p_B-azvJMeJznNsiupJVlaHJelcDKa1wNK-3Y&b64e=1&sign=35c2faa8254166b0024a02bccfeff9aa&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRufkc5G38e6HRzFPiNQl3IF7FEx662AOO9s1a8wpxLuuinRszKA3_3IYLt3cqcLI_Bp05Dxc-2nzLmRftYkm-_nq1dDAVJvCS1oD3RpthsY81zTswIeQlUCVxQuQq-vjUi3b6zBA3FsQKIZZBba03F3UWqAlKMMSp3zES2KoqKnI-UhWhwXy56hNDbxB_UN79YRO__sTPkDSUi5wr0PU3BgbiLF-Qh5L6cUtvtRwLdMURB0EySdktw4dyGA0x0CZp5U5MIScEWyPrbNsJYy2tWvMoaC31d5PHYjWIE5NMSUcWcMuYAZUk-5qBALaX-_8lw5VY9o6dLV2oY-UxNlqKaEovtIyjJ0nBk5HQPCDu9C36U4mmnOqmKQvWhcQ-9BX5x65vnzfORDuAaOxziiuVHsfTqFZa2OuTr0sDeDAJ-g3r1gCUc1KIE39oC9p5fqaD4Dn1GKVuBFxvktmU1D1meM2aHKQOU8DzRN-XUXcefJ_6fj8crRTnI-W0mhY3JqqY3R-xj8OHogY3sfoAtGVd0JPV3UfNC0iNR7KLOinfSwCYffxOhzF4i38ciSF-aYgKl0MfOeNmCBvv7IKnO2qmfOY6fpxCC8O0H5OIPuAkBlUc4Kc_ID3wWuy8tosYqoLBtU2TC0WjBJtLk4H11x9XqZ887bRyiJoGwoTKOW-UcYOF5ph5DVtwslv84IJCJkn3WAXuhlht8Te8kt7Rha_mRJhrrsPVD_67a7uV8tax6p6uFGclLkA4o1sWdZabyuP_QK2WenM0FPvrbr7LtJCZtDDwQxB597tfUY-O60QQFDU6TrUjf55Ds45fHMQI6gSnCys_1y4z8eY7zUMkxS0y2czAORSM3iKe9-2Q-FUSujxxfzhsv8eKpc2meSHd9JaWiyUQIf83Gd0g,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bn2u2rvQHKoAenKwamDlyUb7RcYihuB-8rndcOckvL4RBnZxuEHe4MhtZ0HLLhvyyoZZNKnYr1M1GsE284Wh6g3u9_GjxhlbPPComaNNfa1PPSfFRE9pw8,&b64e=1&sign=e93f5d68d011c957b5d186ee0dc07166&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: 225
                    },
                    rating: {
                        value: 4,
                        count: 9147,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'Волга',
                            ogrn: '5177746098844',
                            address: 'г. Москва,проспект Севастопольский, д. 56/40, строение 1, помещение I',
                            postalAddress: 'г. Москва, ул. Бакунинская д. 73 стр. 2',
                            type: 'OOO',
                            contactUrl: 'dochkisinochki.ru'
                        }
                    ],
                    id: 179876,
                    name: 'Дочки и Сыночки',
                    domain: 'dochkisinochki.ru',
                    registered: '2013-09-17',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Ангелов Переулок, дом 1, корпус 1, ТЦ "КУБ" 2-3 этаж, 125368',
                    opinionUrl: 'https://market.yandex.ru/shop/179876/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1947241737
                },
                onStock: true,
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
                    brief: 'в Москву — 150 руб., возможен самовывоз',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            conditions: {
                                price: {
                                    value: '150'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            orderBefore: '19',
                            default: true
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
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                warranty: false,
                recommended: true,
                link: 'https://market.yandex.ru/offer/6cl8Cj66gtEMxlPMNh2g1w?hid=10682647&model_id=1947241737&pp=1002&clid=2210590&distr_type=4&cpc=MLuS5mRTitbTNOKa2WGbva1Y-VzHNyGEJhpGVOhe9pspgN3VYXro6x4iIPvp4gl5Vr2hVWzpzPgJ5RXAyBhXtwjK4kuxenuEQNMZhdOeteqCjD_iKo-YKR50P2GNZ6CW&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 1000,
                    height: 1076,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/208616/market_YafVySBIwd6V7E_RcwvRaA/orig'
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
                __type: 'offer',
                id: 'yDpJekrrgZFQGnFf9MV0WXE6lNbhoG5lbOWoH3SjoYXdcZ93m3vXu_UMBTNy6r0ixaF42-QUOL1AJV5XmuJwGaURTkbSnoOwtu08ELv0oZ0tdCUYEqu3flVrUa4ZqdAL-zl2lRNnpcXF3QNt-_sceoRtISXq8YYXk79eJdL4TFLFlOY9t4QKpleJtlZlTaOGZHEMNVH-RQGUqJhIcttk9MG7uzFJs1v5O0EGbgpze92ajzNogfH7wdYXs998vNVdQFP2Sbp6UF0QVUIUonoiGiz9dmbhTb0A66oh4bf7Es0',
                wareMd5: '69M_RkfsMBQoIYdz0YgvHg',
                name: 'Настольная игра Monopoly Монополия с банковскими карточками B6677',
                description: 'Обновленная версия классической, всеми любимой настольной игры «Монополия» дополнена реалистично выполненными банковскими картами, с помощью которых можно проводить финансовые операции, также электронным устройством для считывания банковских карт. Нововведения, привнесенные в традиционную экономическую стратегию, сделали игровой процесс еще более современным, актуальным и максимально приближенным к условиям реальной жизни. Играть в монополию можно всей семьей или с друзьями.',
                price: {
                    value: '2190'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZ4H9_aAm-Sdzz0xCC3asiW2ugV_HXtxESaNIZWR_--H8Wey2h1oRyglOllJIuKkafqBbHTdp6m2qCs3Praw0eKTTM1G-XOosxhw8plij9V2VPp_Pm5ZZJcmK1KWA4IK8Zttpb_ahtBsYYfj3zO5JTtsuIsQD-MqYv-q-5_79BNdA7YcpIzsu74B3AiRTJOKa_tho1uQIkY80snaKH_b9EBIemENTbeGlUn4DEl2gHLAdZ4m-KTsf9eePf8p3kItfzvBxn_qdVNUoGKGEHbDG36sYsApjpj3Hj96klPb_0X_JtjtgzklVXuE2r30VRUUGvax6lkTX37IrfmIilkx7Qsqa0EwEUlOe_PFg-iqsMJcZX5ZSJSB8hJx0W3B-bJsQzuNnRJFaH71CzEaF7-292FI-r0SH-u4ye-zmD4MqnHRNtg_1CnGYfgbCckUmtFJQa_0FL1z64nkB3w4sS1Xm_anqZqccZesr1g9yONKus6mgRM3MYUDGh79Sxhc-WNKKJfFKk9DkGaM22ifKiExlUGqyeovuUwDhtAj1ybT1mL7TpAwc7v3FQumDP_CsTxNAY17T1vYnaKYVaHXBgx7lY2ivY9xakLw6qZzyN__IiRbpYhk1N27NqXyfAwaq-P3TeqitHiTRN15fFNYukNqxk3sz2TRC0TTwZpCFyHz_qDNUb4DM5r4kte5LlA9m2WoRxTPrHyWrKFHh_5d-3wK4X7vyTDfyiS_EHNW-LGgOQfLDtnxlshAjmK8t8cVOouVJ82lHMj2uTFIynLImYf6KeFQGZGsRC5xVum29O1Rg3_MZKTtS_1yY049HxFnEOB7-n0XL9nAerEavAgOK-aRxoQ,?data=QVyKqSPyGQwNvdoowNEPjYj7G5XZ8Q_52GzoqacCz5J8ivUdFcU6fMWYjjL5XNy3Z_I2Q81GvwohVg-PIqYq5yT9cVUNhaig8SjzsfRcZchE31fHJiVPhqvYcRchITZ1jw9ImEi8DXUnTB3RRmJw-A4mhuDrxVnXkftzWB0_t8RsF8MX0rC88DD3coIxfww_Ope8fik6zWGG6VIkrRBtL2KRSjZU5ex_bXgMDsoYs90zVBrLyUYeBzseFZTXqLLiIBm594QXyYxpd5HkrZaL2Gas05j2bKxXuyyHcrLH_vVTG6QwNxX52w3SXKmtW6oFu9YvNCkrR_-hr8EUhFPLNap0Hz1IgeR33cEhjWYKcKRFmk0lLA-CjvCSoBImjeYDs1TleH_3biq6qbD1CTzzSA_w7GLGmVgI0yi4DcFYqIyyscrD5DYu7XSkh5btSMRxgJRObDldTqo,&b64e=1&sign=c4d7ab1be323c6483535680e5d6516e6&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iZQe0ObDqoYkAPxexLrRWLSNNl86ar8HKjUE7H7NwMfyF2Xc-HpWYLcxSLOc0NeyCRXy_F07955XfTBgNHpYsAVdgTiku1RLHDkRP8l7C5u_RuglJQYRAx73ipyFmSWvjSRycD4FnL8nQVopF-KObyRvfnDH0Jwsm2l0iKx-OyUusZoV6d3wCJjSq0XKPIdCpeIJ70Kd4xWCs40Lqzp7TcYzxlfNl-BJsusPs9YfsVd71MIWMvZzyYf7ghReQYqrRSJEmlmO9uHEXWQnPor79H1SAMj3F8IolCpU_y48yIucjnHKAfTa64JOtdKcU0gn5zFEL_k7n1BwdKgM-U-FzjLFLG05ohXTZnBWXvYBSnfh9sVXaYY1kCW4oCDnGiPhZT99neYi1u3R1v3XV49HHtQTC7SqU2_FvSchJH9DsZgYsJraaXRU9DtVYrzWicj04v1TOdyRCaHVZSkQfaPSVoHWTcn8xlvhhHLmMHxe_Nc9g6cJvJkMsgiWhooKY9ipy_s70bKu0j16dswy8qOrzZXA2rNCNEEY0mbxed481YnSYBej2E1yYUndz0y07-4BaVd9KleY1wzpGHzZMMe-sXK_9zFsC9beonROPmL-dJ4Ab26DpxyptHs6QwjHR-0cyvRN9R8hMS0QRGrftHZX8TXxcRc990-l3PwObYNfjXDzun9RfR4-k_UK1jhi1hw6JUsQP5rAoPrZ_EgnJ8qnhThHkz2vYClpWtzyawllmteTgHCUyRSVw539qMHoD89y5ev0luNiqbTHFNdzDIYwtmmtLB-SkbfAXB0sIdFgezyyFK_Vsb2nZz9nIeF-07WJo660T7xsWOPYcdgv1uo_qaM,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2ckKS1jlcGo-BB9fD-lWa-thhX34L7u5SvEFYbOp9MG6ZvbDrO9luBC6lygImaZ8yLBdypZm4RYr0e5ukyXxP0fcnoZBhNgz5ZfhfnyZb84LlujyYzqwbSM,&b64e=1&sign=6f700e3f8ee40cdbe01af52ac6fbb5aa&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: 225
                    },
                    rating: {
                        value: 3,
                        count: 2432,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'Морозов Иван Анатольевич',
                            ogrn: '310774622200023',
                            address: '115404, Москва, ул.Бирюлевская, 11, 1, 61',
                            postalAddress: '115404, Москва, ул.Бирюлевская, 11, 1, 61',
                            type: 'IP',
                            contactUrl: 'fedotka.ru'
                        }
                    ],
                    id: 44785,
                    name: 'Fedotka.ru',
                    domain: 'fedotka.ru',
                    registered: '2010-08-26',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Барклая, дом 8, 4 этаж павильон №487, 121087',
                    opinionUrl: 'https://market.yandex.ru/shop/44785/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                onStock: true,
                phone: {
                    number: '+7 (495) 626-28-48',
                    sanitized: '+74956262848',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZ4H9_aAm-Sdzz0xCC3asiW2ugV_HXtxESaNIZWR_--H8Wey2h1oRyglOllJIuKkafqBbHTdp6m2qCs3Praw0eKTTM1G-XOosxhw8plij9V2VPp_Pm5ZZJcmK1KWA4IK8Zttpb_ahtBsYYfj3zO5JTtsuIsQD-MqYv-q-5_79BNdA7YcpIzsu77kKqxP_48KyfzxlOmFU7dkkqXlVsCSLJeDDQoLr_B7Z_AUkpJX8keoNLvG8Ci6ZIBBdQ-CnGhaF9_2A0m731SM5uhW4KQjWZsv9zQh6jcL0eRvl5Yue6lMUFk7dNoVahRW73L8OJPGDT_0ykCEgOIt9n4ruVfZDCvzBLOs3Zj3eKexUYfSH3ouzgy_h2xzERatX68JJzfgTVg_8x_dMLePEl4wPLLMsM0khPWtnCv7auN7l97NaE1L0DKh47v6U3iHWXUuD-hJMo7ty4uu3dbfnpjc2xHqORss9sLdGYOFUKybQTgma2WbrLgUZQF9tMf23T2MYg03e3vewv9y3eeXxQbp7xHE4QgIkCio-8eKyTY2TpyrZSqsvwSqsSKpddXEaKRCo1VazWd9mz9FLj2u6um_KFaLR5TUcqlDxAnLOmoN6YPPJqx4iqGcNcSQJ1leACaeSKKowDEVBfMt3edEpZPIa3SGoqELz_ZRJ7sfnwGogqIFbudtGBQ5iRU7iR3PZrojSbjM05QKVZSqHSjlcW2fUVxfnvWCGW5JoQ90TccpBNEuLXRBhShq-gdlkRpzF8nQlzhukxafJcR9z4sstB3T3O40aRQ--GfqkHIcFfA2JdQ-GEURPXfgIoPUJYcjjQneN3vnAUlBdS1OjVdRftSrXEiKt8g,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8D8SGc-zP6_1VyFMPG4lwPu3FubDAolq5auYkP6LRDf5S98vSQbnTVLRsKLbi4XlGObgx7-dgol1nAnOWpEMwsdP_2b5RRvsImyfuXDZlQtSd92TBlV9tAl001cPaPZo8TtyCi_JUkOQzmuGBHWmZn&b64e=1&sign=a80bbef56fa2e789407488b135760dab&keyno=1'
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
                    brief: 'в Москву — 250 руб., возможен самовывоз',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            conditions: {
                                price: {
                                    value: '250'
                                },
                                daysFrom: 2,
                                daysTo: 2
                            },
                            default: true
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
                vendor: {
                    id: 0
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/69M_RkfsMBQoIYdz0YgvHg?hid=10682647&pp=1002&clid=2210590&distr_type=4&cpc=MLuS5mRTitZAmy33EOvpt0lMxO7A-1WK7ivs4vkOqWZExvmT1nIFAuydbQD7SCVcB8yHwtft5MP_K0pH8GRvRSr_A_rvUkW-6Ef8GzdkLioKXVAd3BBI6wZAompnrypZ&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 540,
                    height: 540,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/246786/market_aQhCbAH9Ot_DFE7sj5wvmA/orig'
                },
                photos: [
                    {
                        width: 540,
                        height: 540,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/246786/market_aQhCbAH9Ot_DFE7sj5wvmA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/246786/market_aQhCbAH9Ot_DFE7sj5wvmA/120x160'
                    }
                ]
            },
            {
                __type: 'offer',
                id: 'yDpJekrrgZFX8RgqC8ubYOWhJxn2ouseEE0PyxSRKn-X2idcS49W4bDFKQHI9I7R-hpU40gK-TLVumz-b88C5cCzERosr8QbhQmCMPN7iPDbBZWtY28UJlCyFi8JdL0-i_SvnaP5ooXv5O4PnaMFNZneyeM8kGOtDhra33X8t_lZY19hWtgHfyxHllxQn2kns7eTAdnIG4Al1gnezAIJOWVk4isBphe_n85O288Ehhlw_hztqSURfZTIG5lfsvRY9QLVg2u6rF1NZzAV3QFPhO92zyjUJzhEkxiemkMSRx8',
                wareMd5: 'POim1R8D1Bk2jqlpfJxMQw',
                name: 'Настольная игра Монополия с банковскими карточками',
                description: 'Многие из нас с детства мечтали стать успешными бизнесменами, быть владельцами крупных корпораций, зарабатывать и тратить миллионы. Мечты смогут осуществиться прямо сегодня с помощью настольной игры Монополия. Захватывающие экономические противостояния будут непременно присутствовать в каждой игровой партии. Понять правила данной игры достаточно просто:  1. Дано поле с карточками фирм, кубики, игровая валюта; 2.',
                price: {
                    value: '1390'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdT7nvpTqws0WZx-3rO3Uj_5gX7Nxe-cMKPi53DNn2PHiexo3FBsIM32fvB5pyNRcNOrjmQ3oiozu7VW4R42geGa2lq5ZBbKDWK0ZD1np5bIwA4dR3HckqY-cB7FFwi4KstNHacs1RpU8eAYTJd2dR-HF-K3Dsg5BfGFItz3lED_c5-hjND176Cf4jQ-DphQ2u4TJaFrtuCTDiKOemB_HZM7E5dftH7Uqu7Ugs9CvArFNYSlJaxM3tsbM4GHm7Ib3TiNAp_i5nhICoyyKNqQI0akgFXvyt99mPNO66nCJW5lYLdH0pQJyx_lSjJqzIok7z89NgiBI0j_Vo_Hezm0dZ2ztboZxpVFGNC-jO7BGJpJakR50P6UhBy3zrd5KeTEC1lXQyRPilRVZQpReSj-bUiGWG3wYRop6LZFeSWdun7WTsDYVcPPtbT0e0zN6XJ_citVVssAwm_D4dPuKJXOeY6rg5fgDMjtYyiel99ZGGYP3MLo1P4Br131b5B7QhZAtexC2gGdELDwtGIwy8Uev7tA0SoWnfgbC3A9S13Emm7jK6FFvk7b1S0cy8i8PJtA_SgMQoFFLso9b4qTfY5bC1V0uZu-HzCupDtu_8I1vyBvO62RZ8GmG5M8xMCRb-DHdyG6gXc-NaWmwA0fpz4bS4pv3YkKKN7mNILPdukd4F67hySZ4TYmm6FNhfLYUdBJf9UaQ32aqW5v0lhl9m24QPX849tqxSYI8FBQeYgB6idhIjQWPMDQ5eXDT_ty-99mhQgJTEsptYqSdml70HT8J384Z2LH_7ubT_eiOTbefAtmpNb4qOkDuEq9obEssT1mIfFAStAYDe40FmzcImeDEtA,?data=QVyKqSPyGQwNvdoowNEPjeF_kTY_zec2skgL7a9kPOHEuRIPZZZ366baBM0cV_LYvm8nzbyzFq3LqNlgSNntRKK3npw9qfyOdElUArEfSwnxL_3-9g1TUswxWZT2FJl7tBQjmOJFWf0Bp4WRnFrFQFgjGzQb2og1IGe5OEvHwt0NL236pfCjzU8tfqrjBkgRvV9KmH77s2Elsnf3Zl7v-vZJKGE8RCgDyvtCRH3zCl03TrCbSjsCZOCzka3zruIAkpTIlGaerdJZ7oFYCJCRtdhkHKWEoqHSaXOKlEwi9qiaUj9D4S7AWW2fcNyhDpcgen-jvW_8aOcxz6u9HrhxWZQ6wYe1UNnvwu_-aW7e7clGdaG_M361-12FPPEuHEdVO20tIb-ypPwtPhWuCg3fJVxmnX4M0wjBerUa6CXIQ-FtvLZlKQLtBo4_nlDsQ6PuxAIWhtVX-RbWL7aCa6ZBg868xNm4KBd4zlZAAtusbLyR2E0b7miFVayAvGnQgArgeZolkuX7l7FpDeOJiuFPZ48oUbZKT1jRHD-8CMGQj5TdF9JreDSy9Q,,&b64e=1&sign=bddad641949dbe9309ed92df05bda9bc&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iTFh5K9sCqO8wXftqn3bxfncTG9E69bFOxXLqxRyowSH8tlx235CmDnvGvwewKiZoDxkzNnm90ZMGNHbdHXYJ3PFgoB9cU7_aGVHyd8M5ef41sByCg-0K2NQLwqUcu2ru0E6fKf2QffXqUeC-BAE5-E9eVHw_3ee8xl3m-9LK5_9fhgxLmJgWrFj7m9734ZY8n_uSaEJos3z5sVH4t7D71KLn3BakE_r1MmpIE3YaP5cRQJfEnUhoRG4dxxr8kIOEAV0GpljI-gw6T9zJdl3hJVufo5llQKe4fysiviaiUAbe6ufDEpZtyL6ZCvqDHA7qjQGemJm4zuNC2mepFRGuWNVY5nmd3wWrudhOaD4oUC7pMyFy7l8q23JNTt_tLVAGA2fX4yo6RmoMq0gMX8d7qmZxwCxCrLsv0_mQ-FrtlPmxSLPztR4nG3WFcQtlWRnKK5SfC5gbIL6nS4WEtx4W8kPTFUL-5ZwyEmS2cT-m9IvYbDGoocZuStsp8OJhUPXkRGX2_pO3V_4V_a-n3KyGv_NYlYE3Tc-2PKh0l94HGjv-VfKWZ3dm4I0fL_7iQtrQhNUH-Dk3ZLLnLhxLkI40HRNaCVGV4GWxBMK7G0F9dWd8gTitU32JLcvvnQIE2V-zj2tD7LGf__vkAVajGOxEnq8x3onH8jFQLa9p0bw1YFNyoB2U8hukcaA8cGSJe-ImPC1wgMXk6uZBbPlgmsXfN1C-kcTLoatys0Grwog7If5OdAmAYXeVsT4hUL5fJb4fd7iKzPXDS8oHk1bfbPyvFoCTdevVY2RrDuAbQ1F5kxRUdZFI7_L7COJ7Z7kQRi52671QZCMTFHVy5y8HYgV8h4,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2ayPFFsZ3ygJ-ZzJqil4oQ7XKkpkoOMsEROe6585T9PGyYD-3vNUTQOdzCjOsuRL7hm9wLj3kGLECDX952jrN8V6uYzV8Rs3imGENWTvTW58H-Gl02IvwoY,&b64e=1&sign=2facd5dba7de18404d869933d3515458&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: 225
                    },
                    rating: {
                        value: 4,
                        count: 17,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'Гаврилов Максим Юрьевич',
                            ogrn: '314774625101530',
                            address: 'Москва, ул.Азовская, д.7к2',
                            postalAddress: 'Москва, ул.Азовская, д.7к2',
                            type: 'IP',
                            contactUrl: 'ultimate-poker.ru'
                        }
                    ],
                    id: 82797,
                    name: 'Ultimate Poker',
                    domain: 'ultimate-poker.ru',
                    registered: '2011-11-23',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/82797/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                onStock: true,
                phone: {
                    number: '+7 (985) 4481488',
                    sanitized: '+79854481488',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdT7nvpTqws0WZx-3rO3Uj_5gX7Nxe-cMKPi53DNn2PHiexo3FBsIM32fvB5pyNRcNOrjmQ3oiozu7VW4R42geGa2lq5ZBbKDWK0ZD1np5bIwA4dR3HckqY-cB7FFwi4KstNHacs1RpU8eAYTJd2dR-HF-K3Dsg5BfGFItz3lED_c5-hjND176ATNjSp4VF00UiaYirYqjFWbEgjjYtjdIJFhNL_iQjMwWWJ3uC88oyMFRrx0DY2CR1DpxW8jigkrty3bUgaaYhjKGI7cM9Q8FJwsx3zi3dssjsphv0KoUnnML3vbMktz_rQr3j4Zcw0JuepquaYKQwDSpTH_BqHmEmQ5ct-dEf3LhkC7om7LdMtXIJdIzJhOTQyM0JD4M4oUEoZIqqRDvP--UTCpYUI-0baojmLdHbDIvg_7lMzn-jj5B7leEUFev1Y2W64v7BYpdE8zwUTJjKWzIt3RRadCJkMMk-K1xL6BZAYAtnATl0TKz-6G4s8R0olko65X5VdykHghjxsmMZ2_ao22Y5ZquoDzYl7ygNSZcg1fPDLzHODZGBoxEmJ0EraRpLoQ5bFZq754Nma6AtS5Zz475OQkB9-sgWC0BXnHaLQD_mo1xeBOH8XqLL6YtS-7X6NCitglJhD30L_7FzyVMNVwDNX0zTLCW9A81PtVABlLaqZv9NHrCGHRi_RH2O-OIiDbt7zDJ5dSsgxjD48B2todTJXiCUwZD94XDk-NsAsQeeYdPS-e0cQPwjKrGX8SYpvCmCErJGonsKL-0U9AOZ-o0Jx5_-0FyET3Rjv2kHdc1p45EIohc0zTV6Il1SjJTqD0r8vny-_uGwns4unY4-Su8o8zbI,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O91klYuXi1jeVWvofc7W9HVNeJdRGV-DQXOnWmYs332SUhrqO3y2LKHjz7Wi_O6gChCPV_4W-n8l1GSfYJni3rz_s-PqCFliPIY--gqFVKcMtKIU7uouJfIhyO6xhT30Z-nSAK0h_FrNxvtI1iFuQkN&b64e=1&sign=7fb077f8c20be7232c0fb97d23726a80&keyno=1'
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
                    brief: 'в Москву — 250 руб., возможен самовывоз',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            conditions: {
                                price: {
                                    value: '250'
                                },
                                daysFrom: 0,
                                daysTo: 2
                            },
                            default: true,
                            brief: 'на&nbsp;заказ'
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
                vendor: {
                    id: 0
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/POim1R8D1Bk2jqlpfJxMQw?hid=10682647&pp=1002&clid=2210590&distr_type=4&cpc=MykviWIDNYCRnunBjyo0OQ3UTBcDkKAYBVptAo4pVt4f-3eI_QZCi1rgX8HZ9SYycLlijTBX75VcxiyZM6SKEWVm_Y0G1V3ADrNrd4kD5RCeLjMuYEG2bvYWDWZ7Wmh2&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 100,
                    height: 100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_zKgMIeMaMrETz4em4nC42w/orig'
                },
                photos: [
                    {
                        width: 100,
                        height: 100,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_zKgMIeMaMrETz4em4nC42w/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 100,
                        height: 100,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_zKgMIeMaMrETz4em4nC42w/74x100'
                    }
                ]
            },
            {
                __type: 'offer',
                id: 'yDpJekrrgZFljaoy-oHp74FRnbwJsUvI2GoEXGoD0gxFo_7VN2dcUl801e6Fq5arhYHPkcKXdCDFXBleHegLZ7QQRfkaBz8Oq3uqcAzKV1b6UDPcUkJOv6ZoiiwhTprgVGU0dqGe5xpdwMbBKqln2drX5wxhYzxNjofENkUCCeyaIf5X0I_e6pTmD9ZIo8Xkf_Ybke3T4EfFoHen3ZKuPCLPX9bNakFq9Q18H0s5qwGzy2povO_upsNJomZRxOF_afNKdv7R_2rfMPfuJVAz-flzvH-pTsdyXpfkRATVW8o',
                wareMd5: '_OG5yx-BUUzB_pGKlruXfQ',
                name: 'Настольная игра Monopoly с банковскими картами (обновленная)',
                description: '',
                price: {
                    value: '1988'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86-g3JTlFDEBlO2eUTbDb9X1WfL0Z_Fg2meWTpwlBKLgaVqukgmEz195jNLlVVE0U2owh_nCq6HAuepSKj2rFYNQQ-2D2AacAEteEkZY8U1GvWpWDWb12Tpn_VTyIHxlKFTNs7jYzY6KbwRKbJg_fuGWpIhFZMwn8U14YmNjmQCK1t8SVFxC5ju_fbJiMOvwk2fEz_OUgxCWZBh0bQxnaqTTFtuNK1_mtDhd27mhTTcF97baE-W5UzjTJni4hfGAqVlORG-HB_QD9C9chHJTIVAtUp4hNiYb3-HqusRXE-jnAyhXjCCITRPIGhkqfxuOVk9oCmpbdANfOrgyS1ao2NILR_OI9cguTxyZM4SCC1pi_7PKHfxbrasAN3KYBiLXD1S1h625fEMe_uRvOEcs4VYU87wLJtmkSZvOoS1px9s6KniMR9oJvryN1gQF8LNG9wCWnLZyEy3tewhxThTII_--dNwv2qdqgJQS7atqEXY7EqA4agX7G4e5lwrbYLOIJxyMpPujawUsiqYwOvNuHP9-cmN8bT0i01XiDc32bAkHcVmeLYibMufcnSfsBrEdZ8npCpDnJEsqY_iyIkZqYuWlQBbE8bK9gbrAPhavTIi6vpmon1l6ZOw56C3bsfxkxvmDHK8LrC4jGw0tuZ7XdZzKNHmpIM4xsImHIHiTQyHN9xrNlXeO2fi5ICXFLqmKl6Ge1kEGDgMDs_riRod-txvQU5jYx-SG6CBUV0I2gvvU702cZLK23Blq1ip5skkxssSbDZ6b82c8IJ0A16yoWN2ZvgYTQnGqFHJ8Yv4riOhCXev0dW6dpPTHB8NTnOmltaVSNp61ik3bTqdKzx9iikxQ,,?data=QVyKqSPyGQwwaFPWqjjgNtHUTF9a1-3qWauNB89DI00fl0oiO0823c0a5M8DMolC_h6rZD10F3VwFhwK7bLn4HOn4khxKEiQSSB-Y0kaeUS3BbGcyEcI1WFFiJkbsIZIeH862LiUtgNUI6jU374XVWeLuouAR2nJxMLpwpXm2g1nwDRw66l6XZtv1EyGE_bLJKn7TUN_FGDOq81rP2oI6pH0XeJ5uLj-cqqA9CrcLlGE8nqCHpDGxRv8lJI9HI1_eW4qHJaF9BoT3qrwk55CGeBFEmcCTBGUqG9hA5k_u0VV68fQ-nBkBgs5fofj6xoMf0aF3mmlajowN41zk1fhnw,,&b64e=1&sign=1079a9e69095b33b05b5786a87562fd4&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: 225
                    },
                    rating: {
                        value: 2,
                        count: 7338,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'ЕСКАЙ ФУЛФИЛМЕНТ',
                            ogrn: '1177746006789',
                            address: '127018, Москва, ул. Складочная, д.3, к.7',
                            postalAddress: '127018, Москва, ул. Складочная, д.3, к.7',
                            type: 'OOO',
                            contactUrl: 'esky.ru'
                        }
                    ],
                    id: 32240,
                    name: 'ESKY.ru',
                    domain: 'esky.ru',
                    registered: '2010-01-18',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Складочная, дом 3, строение 7, 127018',
                    opinionUrl: 'https://market.yandex.ru/shop/32240/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                onStock: true,
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
                    brief: 'в Москву — 250 руб.',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            conditions: {
                                price: {
                                    value: '250'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            orderBefore: '19',
                            default: true
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
                vendor: {
                    id: 0
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/_OG5yx-BUUzB_pGKlruXfQ?hid=10682647&pp=1002&clid=2210590&distr_type=4&cpc=3iimguWzNiIR_Z-1K7FaDZNMGBl2hjCsgH1pfEYiOe5sh4NYSObUW3zPfNW2paQRBGBMXjn12DJvIc298zjUoJCz8EG6tpRAeewG0Y-C_pm6dNiGH3sGPX232u_PpSvP&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 1000,
                    height: 1076,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/235547/market_My7prxKTeAt41TurxmrJ0A/orig'
                },
                photos: [
                    {
                        width: 1000,
                        height: 1076,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/235547/market_My7prxKTeAt41TurxmrJ0A/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 185,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/235547/market_My7prxKTeAt41TurxmrJ0A/200x200'
                    }
                ]
            },
            {
                __type: 'offer',
                id: 'yDpJekrrgZFOa4rKyCy5QEYCc_Og473-uFBgm4qJPAVf-faGRwcV8DDp_kDldqzyOTEWLOka-wfxdspm1gI-jfMXwJ7DJhzOGERgXhEiNzf7hzylm5w1iL3rK3IUSOiP50RwOzFkWBoarreQF_nPKaYlT7su71WfRy46TX4AtEU5M1U6YejuktQea_99f7IDHzm-fNN2oiWhl5TXtg_mSu3IAIjWj1vi7fRzpGKkuQcXC1dsU_lZvmTEBJ0AQfXe0hIYzEdWZiisldntCHea7mKVMd3PVLGJlqIuzL0wMY0',
                wareMd5: 'SYxxN_44zWVVwif_jeYSzg',
                name: 'Настольная игра "Монополия с банковскими карточками"',
                description: 'Обновленная «Монополия» с банковскими картами — это всемирно известная экономическая настольная игра, которая популярна во многих странах мира, в том числе и в России. За что любят «Монополию»? За то, что можно разорить своего лучшего друга, почувствовать себя миллионером, заниматься скупкой недвижимости, транспортных компаний и прочего, а также за неповторимую атмосферу азарта и веселья!«Монополия» с банковскими карточками имеет предельно простую структуру, в которой будет легко разобраться.',
                price: {
                    value: '1390'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdT7nvpTqws0WZx-3rO3Uj_5gX7Nxe-cMKPi53DNn2PHiexo3FBsIM32fvB5pyNRcNOrjmQ3oiozu7VW4R42geGa2lq5ZBbKDWK0ZD1np5bIwA4dR3HckqY-cB7FFwi4KstNHacs1RpU8eAYTJd2dR-HF-K3Dsg5BfGFItz3lED_c5-hjND176A8YmSx_cOEsbIqcxx7-8ayNJvCdNkedyH2ina7REm9l-uo1n9hKZBcrO40kc9ADhjBYmaWivuX7OlpizyNIsfK4WWedTJWiMJaG7g-xok_LxWFzm3UJScbsroQdc7fWkDVZFZIy3BFvK97Ol0DuJvrn7uoGN0McLrAy3gDB7nQ-AwwhYafCrxWKvSsurCGczZI-QEaxW0qYrOHJ53AyXw-lmuypBvhvStsqUOakT9CQuFR4Rt1bT2yYHbGCELxsDuguzljTeMWT_LxrLpp7OrpU7Ul1TF3UR3IOGiRngY-EE3OIRh-eZkav9LdSToDj8kv0wUIzInh-5cO0XRLcis2qmiyiEp-XjJt0SV6UAFC9u9ZdUFqjSZf3rXwAaSibAXAugCD1fmDqtu9CzsvO51mk39PPfSNmkvzEkXnI1oYxZuirMF19Mt1a5kkDalFGbMJ71Y0dQXEYZYVM8pJDGPOM1JY7jC320lifYGBtWJxetCSabb_b6m2vDvtQYKjrZJeGwNYZjHN3PRb0WqMAfbDo3AkUpfrpNPOtDDDi0K5EAC3SmX44SuhVQ-mrejDC7R1ALldsrgbFAjTzNvV4jTvk1zG8tZJ02siIgrjWuaiDw4kaXFHOWOsQP2uCrMlivSsMGlPSUYCNMXMh6xsNUFzezZzVaFR8QU,?data=QVyKqSPyGQwwaFPWqjjgNgt7jW4qD9YytUwTY20syNCFRycsmK-x0BJ8cLcEbUMJfPKmHxJu5drG8RyQg1vCloTd_mliqlZ_OXr2_LYmdiS8j0z2Sqtfu3yeT_VsBAR-hogYZC7kEfQPJDBDC-IWMVU7rWlglEkhdrKkW7rr4lA_gFsiv0fbUrCKFvt02yOQ0fHO168M_qS4IlfCwJa_gNFd5ntYD-jPamu4Lmai9148LdRUNHmYM7I5cNQ2BV7xl_NXDxd0w3M,&b64e=1&sign=ae0ff6a77b093ffb0565f7a2ca6e6c2f&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iTFh5K9sCqO8wXftqn3bxfncTG9E69bFOxXLqxRyowSH8tlx235CmDnvGvwewKiZoDxkzNnm90ZMGNHbdHXYJ3PFgoB9cU7_aGVHyd8M5ef41sByCg-0K2NQLwqUcu2ru0E6fKf2QffXqUeC-BAE5-E9eVHw_3ee8xl3m-9LK5_9fhgxLmJgWrFVtkxJw2etVFT279GKCB-XIaWh6PmkU3nKXeLKDUuJpgQ0dbMZ4RYh8NPKgruCuY7QB90319hZVL5JgY_EfaYJYHYqH12jh4FBPZFnw4c3db_PUrqzkUy7sh0bc9Ww_OMPMMSLv4PC2dE2XfPJ-kmN7HbZxKdsQy6fJPHg9TsyAJ5vSXG0lXgmLIDcrBnDUG8sBRBPwreFhj5ABGHuGAwRDs-HsJ3k8Qu5lFZ0X11sW6vWgg0fSt4Y8Q90fSSu_-ZeUsIzUSN2umKhkQT6zbApE8LEyhblOdIOxkqrb0F8c-WHrbPJXboZjW3ViQYRXEWN8V2-_QzfN_kJnJUMYn9UMZ3uPtDtBON5y-x2sAACyB6qXooCqQUZaoPk57Tg0hLKOn6g9gGI2C4y-sl3PApCU7k-qLTfhpHf0w7uf2BONUM7B4fOGDqR1LhXMAytjihwpNMbGTygWQF9lSIqCgOMRyZAhbR7-R8SojyEzxtVMuodQsbLlXa__l_PluMizRn-WEJq5NGnxoawwMDnOzPCn1R0YD2bhQC2Na2Kzk3pGQWCGL2_YYx2ACApC6J0pyaWQOucw2dVj2DmvJ--xETQjbpfB9knwMnOB4X2GRNssDrpKYVzMmJVJYAKKoh1s4zsW8J6RRq9C-1ngGimeehKPfqwN9xn6wk,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2ftsEY8_f1J70Bt_gXVclF-u3Z5W1XOtaJfpsnmzy1kilfVRBd_EE8OoKSZgVVNA6fZz7VqX4ylEgPVwIcTRoDqgPYyWvsL4ZBkHmB_PXekU-oH-pNpQQp4,&b64e=1&sign=6cc44fa2c647334559dff580cab5bf59&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: 225
                    },
                    rating: {
                        value: 3,
                        count: 2,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'Трофимов Сергей Александрович',
                            ogrn: '313774608100114',
                            address: 'г. Москва Нагатинская набережная 34',
                            postalAddress: 'г. Москва Нагатинская набережная 34',
                            type: 'IP',
                            contactUrl: 'geniusmag.ru'
                        }
                    ],
                    id: 425772,
                    name: 'Geniusmag.ru',
                    domain: 'geniusmag.ru',
                    registered: '2017-07-04',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Проспект андропова, дом 30, 115487',
                    opinionUrl: 'https://market.yandex.ru/shop/425772/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                onStock: true,
                phone: {
                    number: '+7 495 297 06 12',
                    sanitized: '+74952970612',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdT7nvpTqws0WZx-3rO3Uj_5gX7Nxe-cMKPi53DNn2PHiexo3FBsIM32fvB5pyNRcNOrjmQ3oiozu7VW4R42geGa2lq5ZBbKDWK0ZD1np5bIwA4dR3HckqY-cB7FFwi4KstNHacs1RpU8eAYTJd2dR-HF-K3Dsg5BfGFItz3lED_c5-hjND176Cw2DN4c3LnQwQAhvmbsI1cgQsv4kLnFAALBdkTj4wC-ndmRb5pJ-mgmbiRq_-8Z8hecDByXGa8oKzIUDswxg1EOodezia3y06UYU2fNiw2KFnVkrUJsh2suecPlZ3t13xf3ueapOaRwt946yioLdM2kcyJixV2n9o1d5Tx_UNDj8UqgOwsgS4a18k85wSLgyYxTvcrHxSspu-EoM898N-d1-iM39jGo03-ei2Hi5tW1L7H3b_qIrDKzt_L4Hvd6zXB-L-yUEPZrHi3gDKRiswOCJhC2CrkAPtnTsAh_F0SisT3wMBh23slFqQC-JXURSNjsIoISOlbGqvEiKWgfcGeVqnSkqhmg9eQslxYl0puvzWLrqCjVu-O6nbBGdzRIryVJyBoiPpYiCwT6pKSn5k0q4TDHEFvKlmW3mSts9-Eg4ahNUJQBTVFUJaEtUb1T3vnwajt2p2LBw7_BSuUL4teu5zd0hTFw9dh0v7_cPEC7p9MgVLvnSsRdVukUqbt9QH5iajn-OFMExJbnlcFeN_KAQ_WctoZWdYEk6qkftAKAD4FGxHPT3CaTDHgd1msbDYs7pcOWrEndAwaxRLj0Oxh-jSbmgxMzPOdIXcdbyDH0HsrdjS5Kl5H_UltSUBEsTkaHgydgkc2lPjlUgZtpDSD_HxkKIbHS44,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-cGl2zxQQOHa5vsqS2F8ESR2enLXXB0O3qGKwLFC32kstd7MOH6nmI4YMhn5YxNn63oEdluB1PqxzXhHJJ3Pew__mLpnCUYHoR8q3q9wPyWvRJNt5RcoYDVOwRb-tb3cD9rB8dbA45SnogJK6RNneZ&b64e=1&sign=71fbcc150642025b7cfbb66503dbb1f8&keyno=1'
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
                    brief: 'в Москву — 300 руб., возможен самовывоз',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            conditions: {
                                price: {
                                    value: '300'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            orderBefore: '19',
                            default: true,
                            brief: 'на&nbsp;заказ'
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
                vendor: {
                    id: 0
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/SYxxN_44zWVVwif_jeYSzg?hid=10682647&pp=1002&clid=2210590&distr_type=4&cpc=MykviWIDNYCxZaoWGgFOqbF8tu8Q65f65g9Z1a_KP7TpyBYOLxCqwgMyX6W2H4FnwPBdn_u9TrElr6bV3WkEl_wag7Y5X94BKqSV32DmJ_yONrTPXjH5ypw_b101lwaX93F5pYO_-Qk%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97FPnilavHa5-xf7hNTOiAWBbU065RUWLqsifCMo5Ri739nGaNmnynZzvWRaW73c2sqWw4Hvdh3MgV2X1DBGVryGatY8Xwi87RKhMrg1XXyy2BjI7PaXcwIUNVI3RgHjB19OM5i5ZGn66zko2etkhxj6B3wr0mnlrVtwJlEhyc_LPrXIc7k0u8W-CSPRXL9egc0-fhQgNSM8b6P9X2x0Ih54Kzpe1PxYXDGAVVsw7JV5iITpoJ-wOqlOhr1SuqLDigKtPwtr7MY3ov5NAaLn-by6otXKwY35E2uTSLMHUXtv5TPZMv_XoicGQIxZXujNoCOqTmZMNG_9FCa5J0K271gWc71TM0jUmKh__5cKWu9PdMGmpsVWITO229wSvRcBfwubXhF5C4_y7G5jk6WjlX-nNgBe0HXDy8FA4wPoPA5DIRAXTaF9fM0eErCpPZABHHD4fLbuR8YkkblKXWbzECXbues7luTn3qDSp5qjrVAikRKVqQxXWL-MSeERTbokXbGT7EFmAHXmckZ7aWbY76WGzHdUpWpv9rDxq3aEJmU_Fh9u3y6vk1P0znBMF_xHxQjjauytFOZa3FFEUtgzv5-057X4WXNwkMv4r9zxrf_lfcL9nb8TQ5GJNY-Wd8MSzqCOCYwhu7ep7NaNmj1MbqxCW5E_0-8frkrIn1P_0U5HgtzN-nsRcDR4,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXZJ_8Ag0Z-2hsMHc0FkAI6t8qXKNIrOTCIEyWEO-fVvN9k7-HU01WkKeY2vKhVkgal6SkFx6IrexHzoxAtBnzLOw211fLdl4H6AZIz5OdARjE_oJoY6vEwOm_aBwX2WKZp0xNCdsQvq9E8IUUMX5tF4g4ws9KyYB0bkEb0XJboT8r2KXIA4yWAYmDmdbcRGmaXHr5geQ7Fh3iH2MQtq5DPfwKQHHaTcn_Si3cwAbNQbgFDMZqD4-49MRokt0PW00J77gpaHFZfcGxkibT1NZcC-eRdmdDOjWHXkyaxktqehvA5OGhXhm-JA,,&b64e=1&sign=fda0c235b3cbf00b4b04b22a48215db3&keyno=1',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/362766/market_dWwk5fKypf85STUA8J7T-g/orig'
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/362766/market_dWwk5fKypf85STUA8J7T-g/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/362766/market_dWwk5fKypf85STUA8J7T-g/120x160'
                    }
                ]
            },
            {
                __type: 'offer',
                id: 'yDpJekrrgZEKv3vj4-0-LFdnT1vcBcS-VHPrYxB1Yp29kwj2dOB_vA4LBwC5w6aFJRoCkyy9pQHNVDKRVtMNAoRFr8Rs1kAZVH5HUxS5tQlr2KT_Rv9y1yJrR6PbtVPuUZXmjZBY4s32O6MELqLbmDyAO_YP_mWYUgsyic9vCn_IVCcuNZxeNOqbOSQCB58xo8cdZx2cB7ckNUHINvWu4OBV51mOJ1pNpyHzckdQso8bzIQTQenf_rT3GIVmZHp7NIax8Fg9u4uWZ15U2QHQIE2fduJquOOj2T0u-umL5ZE',
                wareMd5: 'NNGyn_w3tabt9FKpodj3Bg',
                name: 'Игра Монополия с банковскими картами, 6141',
                description: '',
                price: {
                    value: '1750'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYo5ilgsL5ZPEISsv0O24o93Lezl-23peZSO327WNYm2J0BzwR7xOPIvK9FQZWrGWXHfdhUAqjzyMgcb2QxHrfWXds8v62bhQcAsn1Yl8x-chBGv73W1RVSCTmxzgvQjwZrSpidiXqCRpJ_QLkunxlZZLdXGqm01rQAAFQPXkIV0SJb-j7CiJrGqEv93RhFEVhMP19Q2RqXbtz-xVTzFoLPsrg6AdMt3gz1CIr4MJuUoVUBqXgju3i65hwoPTbKtp4I0IJdR6cR3RATLAZ03ee8oDKMVH69Gh4bZ0-B1g2QSxw8NXKu-g-1Sm7EFPWwqJ8zlUcJ1fFzcftF9KX-PyId5WWkrgvloMSNtvqMCZL0TidT_lI03Pn_D-92PbfofyJlv1_cQhdZLqyBg61QGLMLJO5OHQ9tR4YfTfQiNSpTtOx67jLirpZYhvr-ygXzIV9h-kpPwtwrEr3Log9zH2zn2pL9ev9ooWzOCtpkea4RDStfJ8KJY1CR3HE7qQ5vG4MwJC5JuwrXQs9uzDHgKhbl5eauIxhxCa8AN3naZDko6NNh4lC0RFtM1rhR0_AXWGKQvDepLpxcl-Beafec4EufN0DsQAXo5l7uNawnTxFWw0q3ysz-FCNzeDo_Yph7gLNNJ-pGuzJ3taH_CBaBuUibenjnJih3V4SBg8Ys8GBNjwnuoQU0_BEWDd4DITEZfsSrLpuJfDLL_vRuEOw8rBOON83cq7hDMaERzJUPlaXHktyFqOL2skoMr1cR4H3xaUoRECrdMpdVr3lb0f3EfKpfuAiGWky4o6favy-DMowIQsbJ9SisBHSlIlr4GkCpPGMlDhCmLC_5vAK8NwX7_qZxrCxPKa-2pKg,,?data=QVyKqSPyGQwwaFPWqjjgNk2Bf71HX7wC4_HfAGchWKF0PHkoxmve3As4wlGJ62d-XLTNSte4BS-6lVwE_CCSYI0LoaboPeLE5CviCODjpIqTQg_vvm4wmNyDZ0CH2YlqGWocoAiYfzZrT8OLlPjxGTXFx4uWt_ijW0rcJ6SQ4U7BXOwABc6JFyx3t1AJtl72Usnfj1JABqWKgRTMC9r5LTCh03-FudFnfEUZE4DIHA7FQCQoDquPNebX_oUpnGpkDE4hy0ro9XW53PgtHqzL957X6MpP-upVwh2jEK85eaVKTyL9Uhbeyk99sY5x6lseJWgBdEctBZfySunQBA93EqBUd3qUslrukdLhjswKQI2TUky4upITN-wlA1hMK5rdnfZJq1rbgdulTBTmLQcMryzxr5jhni9u5IIB29MtIa3HThCz59DgEfxVKVfJJyk7jZagXufb0oNu-Qle0tc1Nu2Ne9NvdRUi1wB2llaWBC1A4SEKE_Caw9OY9cDjAtZ_TX3vOajDGfxJiZH2AgkpSOHRvkhY2d_A&b64e=1&sign=1e237bcaf7bcbfcd2cda8c11b22388be&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iRww66jxVn7cbhcJyi2EHdtifAVWQ4v3bvxzmwGlT4TG7iZghye8YPxoHvrvEOh9fTOL5vcgmjXB9UgDkR4fYP6XXzRQmGV2CzheSbe8zO2-OZoT-MmxEaTRD909ajRBHs3eggT1heXPIUwXaKeDlb0IoU3ITnaCOOQ2R7r962d1fLwc3_PxjnGeeYELLWF1P6XiA91qk4lwFTlt5z7RcGfu1irZ99vdvjkbjzmUTy4ftOyUd6iUSfHJAc9cyxezpzNrC0MTGG5gvztM6tuXgrDC7_-_NScnaAALFzeiuH8OjcDsqwegIWcpUl-hrofQr9scaxGo_xmPxASzfFtZ-f1PTYpF_trF89N0cKq1v9auWrDA-prMzTrJLR4KDVbI_TgeJNTeKyV0FhCfahkgAs1HXZ6UU7TVt1LjxGGT3iRDRcejVoIHzUgIk61jftye11_XFYHrfk9MEmhhconOv88r4JpurP76mLjRp6uJk-GXoY8HoqmAKGsIssb3x7nbZb4WSL5yetbPmFJG9CYxv-ZBlFKpSD2gFXjkH6cJJNWevUxmIXyBY3YyEnhbfRDE3OigdOAVbomxRwmFLFBoYTgvIaFPe5XeA-v4DTsybn2C6HxyDxImlIU5zh7OP1baLqnlFaDh7SmsXDP35kDrs4VWlq9pT2bZTB1pu8Qoy1qPAxN0e8IiU8eLZeHuo8DbO-5LvQyzAD35UyeyQl5ipSzvic13K2hMeTPlE8O6sBULUt6BtEwuFU-ndiXd1U4pZlLTqbyrNAy-raSnlvuWUW03FPd91Daxq0A4nZNm4fVg0KADr3hrE2ZDwImXh2KgnNEzBhNtFDeNwnLAXOy9_Qc,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cinQdnP81zVL98JzoE2fIvo7KNqg3H5sUgSlB0JV_WhGwm26fv-U0nyYNe6zROI8mrZbp1f391Kb5Ql13lfXv2Ht8KDrJBYsyt4dS9w7pP8Bg6uJTfgn9U,&b64e=1&sign=094dba3bac8b4c3b3336125fe4330766&keyno=1',
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
                        count: 532,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'Степанова Марина Николаевна',
                            ogrn: '307770000205842',
                            address: '125480, г. Москва, ул. Героев Панфиловцев, 49-1-121',
                            postalAddress: 'г. Москва, ул. Дорожная, д. 8, корп. 1, оф № 127',
                            type: 'IP',
                            contactUrl: 'chiefandsheriff.ru'
                        }
                    ],
                    id: 199821,
                    name: 'Chief & Sheriff',
                    domain: 'chiefandsheriff.ru',
                    registered: '2013-12-20',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Дорожная, дом 8, корпус 1, Офис 127, 117545',
                    opinionUrl: 'https://market.yandex.ru/shop/199821/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                onStock: true,
                phone: {
                    number: '+7 495 317-11-20',
                    sanitized: '+74953171120',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYo5ilgsL5ZPEISsv0O24o93Lezl-23peZSO327WNYm2J0BzwR7xOPIvK9FQZWrGWXHfdhUAqjzyMgcb2QxHrfWXds8v62bhQcAsn1Yl8x-chBGv73W1RVSCTmxzgvQjwZrSpidiXqCRpJ_QLkunxlZZLdXGqm01rQAAFQPXkIV0SJb-j7CiJrGjCZyZs7DCs8CMbdkERMvl4NGU7rh4iZSl2-zqj5zQKyluetdl22CmA3VY-JhsQWB_AuBmmjXXwj7Vetpfpmz_kRvglvw0IUu0CLp1Dl-5Whtg79ka4-ZQKNgsOMcWBaxKsqTJHoG37FbuMYngmyB4Iy8voQLEbd5qfPfbAgIBbMhY0d_TmouVYF9Ni76Bx5S4k5jmK1VK3Pxgphn197xpz9YTTj-0SVdGU9s42zXekuJTKbZO23WYBHyUGRmdDHt95qApZBd-jbOvONRGVADuBSTiYwPTQRDbkKKTcgfnmp7hOmQLQVW8b0CgvyatRW-2fYIgISD24CKIxc811pVI4oW44kXn-9AapLel0YEsp_ozShPW7jSDDH8fycv8ifFTygORWplwVypbVy31fLxVjBZ-Db_Zstm-vpFpJlCEppBfW035sEbLAbX7vEzuQs74qb9nLpKFHKQ3ZTAgmveO_69TS3etaLp0ZA-6PYZYCye-ods-QywZBuIi3sUlnHOksDi3cNF0gceLKMtlphmL0xImsABaRVdtgfFK7g_4aF2p2kVJYUuX3dBI_p-oxSpRHAFuYTnF4wgGJNGxe_NNSOwDc5Gj1KOaE995rskQgBWUFMNYy-Fxq6jcq9Ubwm7TZNA9Ts3cQLLDOxYk1yKEPHUS7iywzWR11ljrgx86qA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9louDQ5aY0IPz502cfMFXaHBDr7JhmuNTcf1_UEedEfkhpIC-jeKGu1OnxQBtCvvtQtGRuJoBhYhhtD1veWQ9LFweE1j3x-MzEubjfPswechybHJmdl2TzLOR5KNI5oXVqSrVq9xaSdyc7vYKardNl&b64e=1&sign=5d85c14de8fb1329707cbadfd7ab4826&keyno=1'
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
                    brief: 'в Москву — 290 руб., возможен самовывоз',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            conditions: {
                                price: {
                                    value: '290'
                                },
                                daysFrom: 1,
                                daysTo: 2
                            },
                            orderBefore: '19',
                            default: true
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
                vendor: {
                    id: 0
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/NNGyn_w3tabt9FKpodj3Bg?hid=10682647&pp=1002&clid=2210590&distr_type=4&cpc=OTwDT4NSygeYedjikdLYcTy143iWtppqK4VaXWLRGwjtyfFiNGXKUp0AwCTpOt0GwhZpUZpBzX5HxV2ME5Pxp-NNU3JI1DuB6YmR_jylqziMQb59O6i38HKe9EPjrvcr&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 1048,
                    height: 713,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/210814/market_dIG6nQSgWsgRGDZrL4oNHg/orig'
                },
                photos: [
                    {
                        width: 1048,
                        height: 713,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/210814/market_dIG6nQSgWsgRGDZrL4oNHg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 129,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/210814/market_dIG6nQSgWsgRGDZrL4oNHg/190x250'
                    }
                ]
            },
            {
                __type: 'model',
                id: 1730814481,
                name: 'Настольная игра Hasbro games Monopoly Классическая (обновленная)',
                kind: 'настольная игра',
                type: 'MODEL',
                isNew: false,
                description: 'настольная игра, тип игры:  стратегическая, экономическая, от 8 лет, количество игроков:  10.00, партия игры:  180.00] минут, материал:  картон, пластик',
                photo: {
                    width: 701,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/331398/img_id4142497144179445882.jpeg/orig'
                },
                photos: [
                    {
                        width: 701,
                        height: 701,
                        url: 'https://avatars.mds.yandex.net/get-mpic/331398/img_id4142497144179445882.jpeg/orig'
                    },
                    {
                        width: 701,
                        height: 697,
                        url: 'https://avatars.mds.yandex.net/get-mpic/175985/img_id3608901441579811791.jpeg/orig'
                    },
                    {
                        width: 701,
                        height: 432,
                        url: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id3367274518195184784.jpeg/orig'
                    }
                ],
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                price: {
                    max: '2190',
                    min: '1450',
                    avg: '1819'
                },
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                rating: {
                    value: -1,
                    count: 0
                },
                link: 'https://market.yandex.ru/product/1730814481?hid=10682647&pp=1002&clid=2210590&distr_type=4',
                offerCount: 31,
                opinionCount: 0,
                reviewCount: 0,
                offer: {
                    id: 'yDpJekrrgZGfD0iQ-t_B-V4hH_L4dUE-0qVUYRRvyN-5HlUPevqOmseSJVUIRpxmvrAZvazXOnElSvrR7B8g4XWAvTYdwuuo9xyFeP-CRPbK4Mj5BLma5z_v8S4KIkJ5bdb65L9t27wLIsDiEXUmAzAHl-mZY8EIowM3SD_n9EeGdhk-NphGMWvD6Dq6nX7SG1UruazWGWNBTuFG2YcY4GyEke33ZAsBxxCiW7gqesiAviG86Y48tw1bAnDYy4ehpLBdmn-99_Ek92fGUJYPBRC0Rf7gAi-edg-fLI3oG9Q',
                    wareMd5: 'bsIa57ohfYgWvmBzmVYpVQ',
                    name: 'Настольная игра HASBRO C1009 Классическая Монополия. Обновленная',
                    description: 'Настольная игра Hasbro Games Монополия - одна из самых популярних в мире игр. Монополия - стратегическая игра для компании. В ходе игры участникам предстоит покупать и продавать недвижимость, сдавать ее в аренду, покупать отели или дома, платить налоги. Сначала все фишки ставятся в начало игрового поля, затем игроки передвигают их вперед в зависимости от количества очков, выпавшего на кубиках.',
                    price: {
                        value: '1450'
                    },
                    cpa: false,
                    model: {
                        id: 1730814481
                    },
                    onStock: true,
                    phone: {
                        number: '+7495968 83 58',
                        sanitized: '+74959688358'
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
                        localStore: true,
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
                        brief: 'в Москву — 250 руб., возможен самовывоз',
                        inStock: true,
                        global: false,
                        options: [
                            {
                                conditions: {
                                    price: {
                                        value: '250'
                                    },
                                    daysFrom: 1,
                                    daysTo: 2
                                },
                                orderBefore: '20',
                                default: true,
                                brief: 'на&nbsp;заказ'
                            }
                        ]
                    },
                    vendor: {
                        id: 15157809,
                        name: 'Hasbro games',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                    },
                    warranty: false,
                    recommended: false,
                    paymentOptions: {
                        canPayByCard: false
                    },
                    photo: {
                        width: 769,
                        height: 758,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168559/market_4Xhm1VwTFR6mmWoB3XVBeQ/orig'
                    },
                    photos: [
                        {
                            width: 769,
                            height: 758,
                            url: 'https://avatars.mds.yandex.net/get-marketpic/168559/market_4Xhm1VwTFR6mmWoB3XVBeQ/orig'
                        }
                    ],
                    previewPhotos: [
                        {
                            width: 190,
                            height: 187,
                            url: 'https://avatars.mds.yandex.net/get-marketpic/168559/market_4Xhm1VwTFR6mmWoB3XVBeQ/190x250'
                        }
                    ]
                },
                filters: {
                    filtersList: [

                    ]
                }
            },
            {
                __type: 'offer',
                id: 'yDpJekrrgZG8bJQ657MaBdsJfnGqIOEWgcT8s52InvXprGwSE72WzpQNzLPDHHSYAuNoaemxfbSM7ZmdQLAeFVEDu4wVzUuMhykzVOwHYlGs3nzURNiHPKsUSrLtuTDQTfduatgDn2A823c78KZsXojyF9flPPkd_YDmCBlD6EvM9wQoa3Kv4vQZpvYvDjzRULldCuDH9bP4KhvIcF3lBdt_kAuMdKXvueG9chOS8PwrjINIOWg3QyeEzXlYWwgqrlAy4whgpheNPCVoKrl2cTbuQAXXMHecn5D225y1x24',
                wareMd5: '2m7IY2kkVPI1Q9iNJBtUWQ',
                name: 'Настольная игра Монополия (Monopoly)',
                description: 'Если Монополия вам мила, рекомендуем брать более аутентичную и близкую сердцу россиян версию игры Монополия. Россия. В ней скупать и застраивать нам предстоит не улицы Москвы, а города России. Но! GaGaTeam предупреждает:  Монополия - одна из самых известных настольных игр в мире и, тем не менее, по мнению десятков тысяч игроков самого авторитетного игрового портала в этой области (www.boardgamegeek.com), по рейтингу Монополия не входит даже в первые 5000 лучших игр!',
                price: {
                    value: '1750'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mafhF-2nc785JH7LWsnDP732PPqg9i_kayl-BZHAS3Oj7Oof0YJzXVSW3qh8uP0X4ba7zjOS67mWQIjZ3KfHjFn_cKE7zZTQZBf5VQy3JnHgEdj-SH0T_FcgjPZFKg3CgndlQSVMeaI9Vd72BrfPbg_WzCn2nXCuOsGl6Lt8AAYY3nJNUCuulC0Qtm5XSirw3o7M_vh8SdHdQ2t7hmKKMfSqppPPzs-eAXh70k90PeYudxDJYFeUbnvVberpHxDNfu2rZrWMsPJx82MB9HRhlyYitxveaSrqTeBwA_vP16ILpLZR-hld7RUKXnAKtykTH0bY0vC8NkoDP63Gi18YnCCp844GfergF1_nWen_04XmXfngjswzQEVji6sBINbInyTNwt18doJm3pqjTlQJS8rb1VshirFNA5YuYhg6kd6WMDVSJrTO2t694qmFoO7YgKlvStpngRIluOXfBUuSuzRk_q5ysc6SnS4oA8ftJvjZ1axGW3AztNuInKOtU25FmiwNVdFDLtUjrECKWQxxYlXvxZLkALvTVWFnQHdLYh4Jm-XbkC5Tm6XI30Y-R61w15g7h0Cv9am69xhSRoryNvb7rYOZyx1H7DqJqgb1QczrRmm1CpL8_pOHkef8CFbkhawkGxvvFzlezOxXMnSp34AC-r9OED9uQBOH4cFJh_v5A_P5k3i0rkCBdlbO9QxERNmyEatduYOMrL4ic2KfbaxIpEeSQ_pfDEIBPqSAHwwmGSH3QdEx2HSKpJcxpIJVV53BpStNN7l0oUMU1qVJN9vVsipH6kxRww9CCOYV9Cfw4nfh4ST2Ne_INNwlLKpvLp8NfYc_s7pHpGEWDyPy1Ca4A80A-6_9Kw,,?data=QVyKqSPyGQwwaFPWqjjgNk4TDcigSSdg_Pqe3YV_80hk6VTw7mCt-6PvBTMpBvrgOdMTzS9_jewPbXXpnkBijx674_345QAY6PAS8QDiR5vB52E2PmIre5THRcAcLk2RMtfRDt4AZnAZVLIy1s3sJltNyH0LV9AoFK9hIPxkMU2kUTk40raoTzc4BqWjVa2K1RoNeoxVnM8,&b64e=1&sign=18952b715bbdf83059c460b02bb3ddc1&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRtNkb9mq-LhCPjdaOJL9l3OYKA1oXCWbeeVU4RNh0xZ88rNyFNP4PhQHiVqjSdjVjjenZLyEdqrUvFUhZHrw-s7wEdTHg-Qklh95-9l025HV_lJcXnNkcKDKiBqkfzM_AUIC1lR81IIzLjen6QYCtI3ttQQtIbB9WzplHvtC4urSal8PwLBXV6xmJ87BhDHXVSIJshwflfU4JNVqmgX1aMd5gJ55f0LWOLJ7Wm6njGH6nDtyNzh8yTRYr4ynqcxFp1ypcLvYeSdzOl89JKmqP7RxMi9LTwUwr7-GOYW_8HUCdKyjgIiNE1zw6DxjPWsYCouRB4CiYNxesu1yrafCQME7lIlIaEnAqEUolYqT0hVokNARMoR3bgzRcHrf2Zql-lf3QxVloMTnKGrbGt1KYkj4hTwqxXrpcx5Dz9kCpjbxmoKxo8XJzkqDzGmzGTfPP9eVq6Z272G4vxgy5VPeDutFjJLMaCT1Iev3ARzhSxUDnfGZPFlX7iEqnfuFVEuNtskObaLRi81vQUVZ1kl5djUGEdGMXLjsxkvvlAcou_DTqVVkhataM7A_899r-RPuaIIg3paJ8rdy94YfoF_fZ1jkpkWhZpmw6jbyGVrcPCBHMYNbT15S350wRbl0ssgt9qTbyO-2K6RxNlu84uVi5Xo-dAkew5mcp9SMzAd4szs3MkfwMndtjN7-FqBDhz64Ym83XJeIVeQ8Vbo_e6VkyaUBt_VJol79Vj2DLflS2qvoJvvQdwQtpJnW4NU6_7I77chSs8Lt8EOe_Yt5C4RuvhBja4ePOHWTUa9uhVppuAYpz6E-sayMnTv4BH7lxQUY2FvEDkHnwZbf7qM-SL5gbeTIRdKp3sp_Z-Y4mhHnntp7A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YrTP9JDehXRkYxfAUlByKO-S6546yZFNaCjXElt-_xRo98PRRvaKP2h1GuiTaD0n9WUoegV-UNgmjI5OoXigNI4dLevYNqz00PDkI0HF9CVVWuAy6STshg,&b64e=1&sign=bfae1232f5aba7e2164d0699defad2a4&keyno=1',
                shop: {
                    region: {
                        id: 2,
                        name: 'Санкт-Петербург',
                        type: 'CITY',
                        childCount: 18,
                        country: 225
                    },
                    rating: {
                        value: 5,
                        count: 365,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'ГаГа',
                            ogrn: '1117847174158',
                            address: 'Санкт-Петербург, Малоохтинский пр-кт, д.16, корп. 1, лит. А, пом. 26Н',
                            postalAddress: 'Санкт-Петербург, Невский пр., д.69',
                            type: 'OOO',
                            contactUrl: 'www.gaga.ru'
                        }
                    ],
                    id: 231012,
                    name: 'GaGa.ru',
                    domain: 'gaga.ru',
                    registered: '2014-05-26',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/231012/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                onStock: true,
                phone: {
                    number: '+7 812 313-26-44',
                    sanitized: '+78123132644',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mafhF-2nc785JH7LWsnDP732PPqg9i_kayl-BZHAS3Oj7Oof0YJzXVSW3qh8uP0X4ba7zjOS67mWQIjZ3KfHjFn_cKE7zZTQZBf5VQy3JnHgEdj-SH0T_FcgjPZFKg3CgndlQSVMeaI9Vd72BrfPbg_WzCn2nXCuOsGl6Lt8AAYY3nJNUCuulC0VTrmlCelYTeRh50cbMwRkDYIZA6P8S6CMdHENx9gMifniKF9TyvxVVKUtYj0EtbxQH14M46-snJLqZMdntY3XL0IYRioSkOLtzVry2ka7qH8Ugsfh74pZ4Y6JjtumD-lsa3G-8nMrZRFvzkZJuyAhmOJXplfZ85FeHjYlm21jIdHSRop2BYt6a_Nh8xC3BO82LvYu1Mv9EUv99Zp6PfpXG30oAa2VF7IMWAiBPIrVEvq-XXQ6_ZcH7VyhuykRZsNwL_pcu9vJ6ZzT7ACtsKCFX0Y5uPj2MIHNnzQtQroP3Nbqsf_c01CVFW807pTYdKxRCWa8GdCE_anhScPHyJf3Z_GC5f8dM6eiEhRrWx9IV8hH6kn77BpYTc2Ev77WZ5GEO5JWHMjy5eXjyBWY8PxO1dK3rxTqDEMfGB_9-KYK_BnjYuSch4Q1h_i2cjGoi290XRQwPaTUxKEoZMx3cgBb4x2JAS75gDrXDvyOIf5DYx1Bn4oDsL6Jr6yuRLT-p9fSf-lXxZhRFIzJ9zHlGiNb-f-t-VpY29ver1Ncwi0bUqVLtnz-DYLMY69fokuBh-sBFK7w8JXfhJqCyzPvrwAhXRPea07MOq2JaF0gpAzv72p0GQCY_cI22dJeVOKryx6mA3-GsEJc-JIP8jCZRTMCFDVV0Ab1x8Q0907HvTJKFA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_qimfTxeXRK4pVjJU6y3h9S8dTn1pmm9z2CVjfTFMjbLiuWhRof1jPsKxtYytUGo7aOVRfCpAAKCeFoNoSp_wszr8-CKGaAShvf8v9yL7lzzvT0LzFwKAfNZ-7S820Db10T-b0JD5qwQadlXU1NIgE&b64e=1&sign=941df545460019bc785822a471a65b7e&keyno=1'
                },
                delivery: {
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: false,
                    downloadable: false,
                    localStore: true,
                    localDelivery: false,
                    shopRegion: {
                        id: 2,
                        name: 'Санкт-Петербург',
                        nameRuGenitive: 'Санкт-Петербурга',
                        nameRuAccusative: 'Санкт-Петербург',
                        type: 'CITY',
                        childCount: 18,
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
                    brief: 'в Москву из Санкт-Петербурга',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            brief: 'на&nbsp;заказ'
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
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/2m7IY2kkVPI1Q9iNJBtUWQ?hid=10682647&pp=1002&clid=2210590&distr_type=4&cpc=RidRHVxmccgfF008QsGWzoRC0e4ib3H1zxYUkp3rL9q5t36juJZk74Zw5zLAtdMWfmIqu6v21SSjTIEScoMzoFRmZSIC-WOdBESF-eA6rShxNYHM1zjZbmpifkmBYnzj&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 3224,
                    height: 3469,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/165839/market_xU5Fkma_FWHsRz9FtgmjQQ/orig'
                },
                photos: [
                    {
                        width: 3224,
                        height: 3469,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165839/market_xU5Fkma_FWHsRz9FtgmjQQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 185,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165839/market_xU5Fkma_FWHsRz9FtgmjQQ/200x200'
                    }
                ]
            },
            {
                __type: 'model',
                id: 1730875295,
                name: 'Настольная игра Hasbro games Monopoly Империя (обновленная)',
                kind: 'настольная игра',
                type: 'MODEL',
                isNew: false,
                description: 'настольная игра, тип игры:  стратегическая, экономическая, от 8 лет, количество игроков:  6.00, материал:  картон, пластик, металл',
                photo: {
                    width: 574,
                    height: 470,
                    url: 'https://avatars.mds.yandex.net/get-mpic/466729/img_id67179822190905322.jpeg/orig'
                },
                photos: [
                    {
                        width: 574,
                        height: 470,
                        url: 'https://avatars.mds.yandex.net/get-mpic/466729/img_id67179822190905322.jpeg/orig'
                    },
                    {
                        width: 596,
                        height: 572,
                        url: 'https://avatars.mds.yandex.net/get-mpic/175985/img_id1898240457878744151.jpeg/orig'
                    },
                    {
                        width: 596,
                        height: 436,
                        url: 'https://avatars.mds.yandex.net/get-mpic/200316/img_id6780802059146258900.jpeg/orig'
                    },
                    {
                        width: 628,
                        height: 628,
                        url: 'https://avatars.mds.yandex.net/get-mpic/200316/img_id6020163999821048888.jpeg/orig'
                    },
                    {
                        width: 628,
                        height: 628,
                        url: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id7643226013459326128.jpeg/orig'
                    },
                    {
                        width: 628,
                        height: 628,
                        url: 'https://avatars.mds.yandex.net/get-mpic/466729/img_id6726291868686878177.jpeg/orig'
                    }
                ],
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                price: {
                    max: '2280',
                    min: '1763',
                    avg: '2280'
                },
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                rating: {
                    value: -1,
                    count: 0
                },
                link: 'https://market.yandex.ru/product/1730875295?hid=10682647&pp=1002&clid=2210590&distr_type=4',
                offerCount: 25,
                opinionCount: 0,
                reviewCount: 0,
                offer: {
                    id: 'yDpJekrrgZGhWoalF9mneI4YdXlP0iyx6mCAel15WioTbhJTeN-0hx4KrlTwZs5NvOoqmsfynybDOCMAh7QlunZ_whs9wBEa4p8Lzg6hAOm3i_jZeyODcABxpbIP2JwggIGJUDYQK0-WdGhNEPHHYgd5MHrdOZ2uEMJ-3bUv0CyzCrErvyMt9GrSeTANMDQ2lkJ6yZ6qYkoQOzpSi7bwUeJR5kdK9ceNoz-J3_mgDfPGmSIwRFW40npkSQUTiGgLI7o61Op9-XGCtmFruRrSaWwAEti1qDUSpf67J4_t_L4',
                    wareMd5: '5S28mKjRU_iWHAtVeA4QZA',
                    name: 'Настольная игра Монополия Империя',
                    description: 'Настольная игра Монополия Империя (обновленная версия) Завладей ведущими мировыми брендами с новой Монополией Империя, такими как Ebay, Nerf, Transformers, Razor, Nickelodeon и многими другими. Цель игры:  первым заполнить свою башню брендами и стать мировым Монополистом!',
                    price: {
                        value: '1763'
                    },
                    promocode: true,
                    cpa: true,
                    model: {
                        id: 1730875295
                    },
                    phone: {
                        number: '8 495 988-29-15',
                        sanitized: '84959882915'
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
                        brief: 'в Москву — 350 руб., возможен самовывоз',
                        inStock: false,
                        global: false,
                        options: [
                            {
                                conditions: {
                                    price: {
                                        value: '350'
                                    },
                                    daysFrom: 4,
                                    daysTo: 4
                                },
                                default: true
                            }
                        ]
                    },
                    vendor: {
                        id: 15157809,
                        name: 'Hasbro games',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                    },
                    warranty: false,
                    recommended: false,
                    paymentOptions: {
                        canPayByCard: true
                    },
                    photo: {
                        width: 300,
                        height: 300,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/240755/market_EwsTaS5peTdZ6s_BZ-5Z2A/orig'
                    },
                    photos: [
                        {
                            width: 300,
                            height: 300,
                            url: 'https://avatars.mds.yandex.net/get-marketpic/240755/market_EwsTaS5peTdZ6s_BZ-5Z2A/orig'
                        }
                    ],
                    previewPhotos: [
                        {
                            width: 160,
                            height: 160,
                            url: 'https://avatars.mds.yandex.net/get-marketpic/240755/market_EwsTaS5peTdZ6s_BZ-5Z2A/120x160'
                        }
                    ]
                },
                filters: {
                    filtersList: [

                    ]
                }
            },
            {
                __type: 'model',
                id: 1730875946,
                name: 'Настольная игра Hasbro games Monopoly Россия (новая версия)',
                kind: 'настольная игра',
                type: 'MODEL',
                isNew: false,
                description: 'настольная игра, тип игры:  стратегическая, экономическая, от 8 лет, количество игроков:  8.00, материал:  картон, пластик, металл',
                photo: {
                    width: 701,
                    height: 458,
                    url: 'https://avatars.mds.yandex.net/get-mpic/372220/img_id1294489312440712904.jpeg/orig'
                },
                photos: [
                    {
                        width: 701,
                        height: 458,
                        url: 'https://avatars.mds.yandex.net/get-mpic/372220/img_id1294489312440712904.jpeg/orig'
                    },
                    {
                        width: 701,
                        height: 467,
                        url: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id7321830368357016560.jpeg/orig'
                    },
                    {
                        width: 540,
                        height: 540,
                        url: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id1538984340650095198.jpeg/orig'
                    },
                    {
                        width: 575,
                        height: 380,
                        url: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id4661607332686309195.jpeg/orig'
                    },
                    {
                        width: 604,
                        height: 500,
                        url: 'https://avatars.mds.yandex.net/get-mpic/466729/img_id3048986982554127083.jpeg/orig'
                    },
                    {
                        width: 596,
                        height: 396,
                        url: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id3898701998878127283.jpeg/orig'
                    }
                ],
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                price: {
                    max: '2289',
                    min: '1560',
                    avg: '1990'
                },
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                rating: {
                    value: -1,
                    count: 0
                },
                link: 'https://market.yandex.ru/product/1730875946?hid=10682647&pp=1002&clid=2210590&distr_type=4',
                offerCount: 41,
                opinionCount: 0,
                reviewCount: 0,
                offer: {
                    id: 'yDpJekrrgZGty0pe7jjgK_bDSWHzsULqqkJ3j8xxG2XM5nbA04Lgjw',
                    wareMd5: 'EWLJ6DopR7Ovz39oRe33wg',
                    name: 'Hasbro Games Монополия Россия (новая уникальная версия) B7512',
                    description: 'Новая версия настольной игры "Монополия Россия" от компании Hasbro представляет собой специальное издание для российских поклонников этой легендарной экономической стратегии! Как и предыдущая версия игры, данное издание было создано с учетом мнения жителей России, что, конечно же, не останется без внимания.',
                    price: {
                        value: '1560'
                    },
                    cpa: false,
                    model: {
                        id: 1730875946
                    },
                    onStock: true,
                    phone: {
                        number: '+7 495 669-29-11',
                        sanitized: '+74956692911'
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
                        brief: 'в Москву — 290 руб., возможен самовывоз',
                        inStock: true,
                        global: false,
                        options: [
                            {
                                conditions: {
                                    price: {
                                        value: '290'
                                    },
                                    daysFrom: 0,
                                    daysTo: 2
                                },
                                default: true,
                                brief: 'на&nbsp;заказ'
                            }
                        ]
                    },
                    vendor: {
                        id: 15157809,
                        name: 'Hasbro games',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                    },
                    warranty: true,
                    recommended: false,
                    paymentOptions: {
                        canPayByCard: false
                    },
                    photo: {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/204557/market_jPscJ-L0CK8Z1fK-YoVz5g/orig'
                    },
                    photos: [
                        {
                            width: 600,
                            height: 600,
                            url: 'https://avatars.mds.yandex.net/get-marketpic/204557/market_jPscJ-L0CK8Z1fK-YoVz5g/orig'
                        }
                    ],
                    previewPhotos: [
                        {
                            width: 160,
                            height: 160,
                            url: 'https://avatars.mds.yandex.net/get-marketpic/204557/market_jPscJ-L0CK8Z1fK-YoVz5g/120x160'
                        }
                    ]
                },
                filters: {
                    filtersList: [

                    ]
                }
            },
            {
                __type: 'offer',
                id: 'yDpJekrrgZHEzqd7Iig_SGxtar-HBxW_49rwgRjQwLb7pXEZ6Uffag',
                wareMd5: 'RGBLfqm8FPSHtI2HnCLHxw',
                name: 'Настольная игра Hasbro Monopoly C1009 Классическая Монополия Обновленная',
                description: 'Монополия от компании Hasbro - необычайно популярная игра во всём мире. Это замечательная возможность провести вечер дома за увлекательным занятием в кругу семьи и друзей. Монополия развивает коммуникативные способности, учит следовать правилам, терпению, улучшает внимание, способствует сплочению семьи. По правилам игроки покупают и продают недвижимость, строят дома и отели, собирают арендную плату, платят налоги.',
                price: {
                    value: '1819'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYo5ilgsL5ZPEISsv0O24o93Lezl-23peZSO327WNYm2J0BzwR7xOPIvK9FQZWrGWXHfdhUAqjzyMgcb2QxHrfWXds8v62bhQZTF0WnHEEYY9sXL4_9__CUZe1kh3XoSnKxS1l9gmpmr8IrvVKWQVTdgqBpZvP4qIfZXY_wHhklIJvkIb0CLvxWlD1t1gQHrru_l3pQNP5FdMXST1rYf1xySJ73TO7b63BSTfO4NAZLym_it62f88afBTU3MHQfs2q9Rdm9CLfSw_PKRWgG53ooVVjZh6DszwBtZUy5nZ_4VIX1kl8umbAN7PlmCmJWBaFbwdpKDBtkcBkWLIeV2C0Z3UwScRUT4vx2MnOuQCOidPqvK1Tga3_XsRrze0xaEE8GwCge2KYckSL3-r2C3PuMW2hkByimzp8qWVdYw1aKX_FVdBuBiGE3dcHt5mplv57_nCAElS61gQx7n4sW3QLfNyHJVtwZErDQrQWucDC9aLZjlEPSu8ax4MzZp2rpX-ZCFt7BNNI3PxTZthPvWlNGDqLmfpESQi9x3GFxAPiXP3UnVXfgcsuD9I9qrRGcF_WhIu_pfVvhwRvhxWXFho66Bdy2tsO0-ahFqn3CKQHF_6KLLm0o5Go-TX8aLJEjT2-_yUPE2SvCnKMY4yATj_zy0XSuKfUVxFtyHjtvOIuQdMPeoEhcr-L7iRU2RdibYS7qXIKvkkY8CpMcqTN06jdM4SmiQesa7fTOJ45OGYS4ZbqW0P7xY8ASOo4e-yI-H0sbNqXd2ztp214QHG0vc7vazIOyq39tduf76V6WUhdXsaASsjCM2nA08BZaPhTyWn85z60UE5XM-lwFAayiQHURHEhK3PqBviunqzRUYoihaILG3-Kvv6fw,?data=QVyKqSPyGQwNvdoowNEPjRcd1X5IkopGx8TOY7lHsB2IzdZCVgSOE4bzcsD5Zw5aEtBCrKTn8-x1ThRBMjIKWWkAXhxrkJjLNzYyat_21cpag6wL9IEVL9gZelE0CSp73-Zz7GCsFmzkF5kcBaVUUCzmf5mtMHO79mii8aIdGS7oe2vxV81sPa0AeZ4NCI13vKc2VH5ZJKS4jyc4mspK_bxq1TyXjDc69xfGi163za90BOdHNhlP9lDVrO5ULSnssdhhUFQ6qq5kAyh4fLMunHk5dX1VkzbK3_69d_3kIVVm0RzLqF0K-NI10B_uIWkI0g104upSKPQjNUEq6krzFokz4vv8sy0-XgZ-D0_YkEipdUi4sSyeXtXW-EXL-yZHD21HmLLDIxSGCLmJUfWIMSCvexI_3A9WqtVUddxVVpICkcBhto_Qy-huEo2sWqv-rUUA4_lOvFRPu4Xc3J4dkqvLDlQorpk_E3cPN4BXBfc8OUCs4wYSpaUDZJ1DakLsH2vNePJ4fGrN4T3pn_Di9tEtxVhhLdIHSldsLQAkgZSnaiozNj9y74CJpL7fUWNJ&b64e=1&sign=b6b5f1f9e9948a3f307f3cfc827aa834&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iRww66jxVn7cbhcJyi2EHdtifAVWQ4v3bvxzmwGlT4TG7iZghye8YPxoHvrvEOh9fTOL5vcgmjXB9UgDkR4fYP5d7NKyKqYkIgusBiTwjFSrEmrz20VXY4p6vDv0fa6rQQCLP8rEci3oAu5_TPWsz43nn1YypeNKI6QXcCd6mjhlN5H2WjYztDze_j1-n3xWtdumtw1uYBKR3Dk-INcNW2gw6JnbTAhj7ZolfCTX_L-3B1YrqByKfOtMyVXob1tap3-BMCPKBKNZdEGT86-PNwM8NSCaoxQf5uNv-y9GBSQgvtASgvGD7Qk7VOTkJsX1Pfh81kyQSZsAzqdpr5Iecg3JS318G3NIkxpL7VFEtBX7IQeOT5qAdeTLsMWsJZ0gp6gLRKhlqbe_O9Y_AxvSOELeSaGDnQL1ostotlcda30clVtCvRwsGut370Gqe9Zy_JCfEJH4qoXNQ8Ehe7GFwwhysyc31lGGIB8v59Lf6OLD9YYaw6_AgG9uXE7ZMl-j7kVhQ8gcbezMZd0wqbESZcWENIteaAfMDdsbmS9UYh_L1ztLrHW6H5EOrqIqncxFLYXA8hiMQ0pinS3bhJNrzzP7_gISJHimwukDa0bzmKVo7kDP3GTTuMdDQRnAgai6r8jM6G4GkUgaWO17Ag86ebb2n6SPbLahfNScq0woKx97JVsRVnmAA-WWwAgT-NkmxgTbp0nPZS_teTy3dvsL82mEZqwf1rySRkaXPjnNuB6eqjp7Tgh2wSqXJ85K8PNsD_KKg5oUGQRC3Yr3wDWel079Y086GeH9ubt8zgIo89xY4vY6n1Vm-wPc-zQNRE5Kejo-yZ5s7vd8YP_jmApuJI3LZmm0WgKmzGhz40jfBjw_zFlynd-s0ho,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2btM1dgQVldwGzawYIo0XF_E1DGH_r98NFfdzquLNUMuowMEYQBV8zB4-PFHI1u0e_BgAAqVR7ab_KffMLybTUBgvrT2luHCqiskIS2kniMzfHDJzla8Vg4,&b64e=1&sign=1d4caa5e3ab8bfe3a325a1e50d8293fc&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: 225
                    },
                    rating: {
                        value: 4,
                        count: 5417,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'ТОЙ.РУ',
                            ogrn: '1165074050745',
                            address: '142111, Московская область, г.Подольск, ул. Вишневая, д.11а, комн. 305',
                            postalAddress: 'г. Москва, ул. Профсоюзная, д. 132, корпус 8, офис 300',
                            type: 'OOO',
                            contactUrl: 'toy.ru'
                        }
                    ],
                    id: 56564,
                    name: 'TOY.RU',
                    domain: 'toy.ru',
                    registered: '2011-02-16',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Профсоюзная, дом 118, ТЦ "Тропа", 117437',
                    opinionUrl: 'https://market.yandex.ru/shop/56564/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730814481
                },
                onStock: true,
                phone: {
                    number: '+7 (495) 215-22-44',
                    sanitized: '+74952152244',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYo5ilgsL5ZPEISsv0O24o93Lezl-23peZSO327WNYm2J0BzwR7xOPIvK9FQZWrGWXHfdhUAqjzyMgcb2QxHrfWXds8v62bhQZTF0WnHEEYY9sXL4_9__CUZe1kh3XoSnKxS1l9gmpmr8IrvVKWQVTdgqBpZvP4qIfZXY_wHhklIJvkIb0CLvxWDceIHOpQ5342NlVry7xjlUGQQWr30YouiBYvqumtlKyf_w3JN2uqoYq8kYPhlsWbwBaYM6AN2LYaC2LOwseeoiYpt3Ieb7kGraL0Kav2DxuVdudA7pNu41qeM7ZO2MVQDgoRA8dZiuyA1eunWnJ21xnULUrcCzy1RdZEQDxQP8yXZniq0TgAAl9_aSnnjUMT7igqS3-PvvT8Abw6uSBceFlUlPEKB_xCfgDpHSi3YD4z6oQo7rlkMTiATEMM7Sfg2ecvVVmKmPzo_cTU3KbV7p46q7m1Vm_Jd3xiiXF4uMUxFVum3qQFsejggjosqv7DdeYQAHqGZRzgiS9AOOuoQ2HjL22Ad9rxIRsPQNmvEch7tTX397saMMii32EQkkQ2HtGIgvSyX2vbugoQg4pLrPxea_hGsQzjA_mnzV3t7cZ1dgIkVMBo8qZQsdAkO_0JbLueDHUmh9mV9zC0X8tnya3CSGF0gdMcPPJe-5_f3iAVLxiDGQxoo0WKxQqVSajLzqu99S_AvX4nlvRhZmbjFvxmLuQM16fOx5kT44u8vpUjecHqC8AOesTp0t9pOPWDuvi7V7QhlOXsj-uOjmzpy4SssTdbR8iMkR4B_aAev6AdIQDUTjY8BLY5xsTYNQks6kWBvzDm9e4NB9bZQ4REUwHkGn0HWNK125Wcb52eUMbG2ZlCD6U3m8e_W-PbohRM,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8JiyBVjaZz0Ui6vWMVW7hqWOJCcqHZWBzQnwZx_aCQclNVfcVzkPgtv2JbErZzjvfQEmd4Yk5G71EjMpM63diuJvVVCVO_yhQD3xVFXmJPkhct8oBnPebeAOh4OOTYdMOUwaB4i2v5zGtdYK5BhYLxYHk88BY_G0jD1YqkSh-76sjIFCE2f0Yo&b64e=1&sign=1db2e82e685a8cd8fdf0dd9509c8b350&keyno=1'
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
                    brief: 'в Москву — 99 руб., возможен самовывоз',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            conditions: {
                                price: {
                                    value: '99'
                                },
                                daysFrom: 2,
                                daysTo: 2
                            },
                            default: true
                        },
                        {
                            conditions: {
                                price: {
                                    value: '299'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            default: false
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
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                warranty: true,
                recommended: true,
                link: 'https://market.yandex.ru/offer/RGBLfqm8FPSHtI2HnCLHxw?hid=10682647&model_id=1730814481&pp=1002&clid=2210590&distr_type=4&cpc=OTwDT4NSygdnmm_ZqWkFGdj6jURAitsOj9arjFZFT5moRN1kKxCmeOGKw6w6d7hc1vl8sZgldqA7etxGeLrBh6JewdEv5QEgjhd4n6pehfXWTnohELGyWEOHOl_ZIkhG&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/369934/market_q_86qmGJV6n9huz8y-hK_Q/orig'
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/369934/market_q_86qmGJV6n9huz8y-hK_Q/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/369934/market_q_86qmGJV6n9huz8y-hK_Q/120x160'
                    }
                ]
            },
            {
                __type: 'offer',
                id: 'yDpJekrrgZHmQ8mCI6aISSahq-8xZsXwy7CNo7Q21F35L31aoPsd8eES3gE7-c2cxCVM6gWf3DwXmXNVCjsW46jO6gwpYrh-iPqf-Nml4E_x4Dp7H62f3kElniagh4bC9xe8wee4cKAeLDv35IXACXKHo2a-q9sPoDTF8vRK2Kwli9GMO89dlcftQ9jGY68wuxewDbQN94tCJyehAvDH81ZolMPLNbMdGsdZ16-SBjuaM7jvAxm-7Vn1OYwGPcCx87NsdxgULQ9ay-UxVVaSZVkPUiIfhFWjGA6NBPjbUjc',
                wareMd5: 'KXK4sVywT7zraPRNLPILLw',
                name: 'Настольная игра Монополия с банковскими картами',
                description: 'Что в коробке Поле для игры Банковское устройство для карт 6 банковских карт 28 карт Владельца города 16 карт Общей казны 16 карт Шанса 32 Здания 12 Отелей 2 игральных кубика Простые правила игры Монополия с банковскими картами – это мега-интересная новинка в мире Монополии, где все бумажные деньги просто заменили современными карточками. Стать монополистом и истребить конкурентов стало еще более увлекательным заданием, ведь теперь мухлевать не получится. Минуточку! А куда делись деньги?',
                price: {
                    value: '1730'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mf5fIUVrEGur_sBDKtpRPCMdRvZ_9IBYLP_IkQ6-Qb2RwF1b3sXRDmyPh6WEMArs3TW4gwsfeZyECGmPzd16kqrA1bkyGNupuBLqQic1hIB0SYG-hOywojVG9Bgx0077u1DUHtTmTz6jphYsYe7QRZ9qUb4eoDPZcTbiRqz35in74igJ6HwOhkSah8852mNFTeIPkovQOqLgT2569tGLJvxzigeJPT_AHH2kTTV8sEk2MPxbIgA1Pb-Z5P1oMJEqvaORTDYdeELbi-4JQF1jJUU8ISBsfJY87ABYkxMUl1g7_abP2XG23HG1j_pI48GCPsnVQJqaruYtqqfjDtRM4vUwRgUn7WjBzkOCyJ1s4XSbj8N4HBcWHcYIaIHVUx-SOvrAwAIH_EFfGDm7tRByGIYtKhW0OvJKdiCJyCEQ7jsymgOOVuzCuOgvRoDVv6IJ5FKCS1_MF5j-WTca5zSP6Sq31D8HFMDRDYZtPC3rrVxSNLbtyQ4tPZFfvZRjaazUVUmDy-KJmxm7jLFWcAbD744DIl3u6i8AjcKpxpezb-F7hUXZ1kYxnI2cN4QVmGN6blOlB9ecm8RyKfAffE_zVnKOPv1_R3hqYuGg9amFz1OJa5KCELuAuZaKktdzqB1KE_EyBVpLTA0JL6f3u1PFN2dH6GIrg55LTz_qdwL5FjaW9VX9UlTDi5JmrFzO2Y9IcFxTfPc-befJWxaXxbPEqMablodPW65gJeOzwnmfbUmmrhMVczIekv4Qn95PDR4Y4WZSaIwT6_UARJhRVQaH9ZzBXecmD3kVcxCk5am0Gdmf-0iKU4a75gIec6aptJ1aV8cvZ5kkQDWDnUY1hQZ8m6Wj1Aq6nFKua-CEv-wzaKit8gsuzP_eoy1UUVFuXd3eMQ,,?data=QVyKqSPyGQwNvdoowNEPjVHrfv8540ymfcv8UgfDi1VeZLL-x8vkrw0-a6Tk2nW5Ec9oW5abo6iFXX9_4GqWM0pDQUQ1vHul_vw0y9usSMmDzdw3xe2koVCPGrvG7UUjkqh7iCRtPBrhySSGVV39_Cc4-ndLxw4f2VZkSUO6R3SmPr66qvlXgHDgGC4dSLliMM2jN6SEOGRfrwECi6xXIhyDvFpsO2Z5H3Eo4CJ-dVm4s5vXkLdCata2bx9ZmqIYLxqKJi43T-XUJxwtdWxFwJQv91uXOgNL0YXGzrnEozTIw0kv75qJezHUyqBL2hUIXPuc3pw6fs1TmIv2huje6tH5oDlGdqlc8wiIwAsB6lD1GmRGfN8_BLCrsNNU4EOIani1fKCNAcGi3Ll-zc7LoAzY-OmTDgE6vHxaTmOAFqY_TQatXBqMU7DXRcSqwd84Yrk_66nK8g9Qkc5xb1EA0PRS9OVKxcjbMblQgWmN405BppZglD_PwBewI3pY7vZqH8Q9bzL4YJzvpc-ewxapBn9Rha41Mm4eGKFuuN7N7jUvCdSsIApSnIfemajEItp04aq-gS7-F3-C87dSevJQuJ-4TaSD6_YhaIHVlMwfZtRZtny9yXA4FKRp35w0okrs&b64e=1&sign=5c356ff22c46306c36bd3330ebd82bb9&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iT04ZeOIZg2GWrnLLzkO2HxwCTNMLESXOPx7ccOhGYLdI3IqAnBWzpQmBa3aCtxNtWq8gaqxtIYRnne25yijojsI7T7hkKMedu4zylw_AMPLHfTEQkwTmT2zVE4TtfTLRKDlHGwbjKlTdbETsreaSAk192HMl6otrzk-46mmuIXj3O2508gPPCbFw1whDDUvf7BoUYL831ihP0N5g5j-Ul0TCI_u3Kx6vJwGQW4PL_t4-rbfPCAsL-nPezEW4T98J_IN3uxN4bv-Qtuo6RSk71s7Gg55kV0z4q7thx0vAPvr-wYO3jkKWPJaUOGeP6ckqwEK9z85r7sEOz02vCtc0lWUUhcONxWaua4OGKWbQYvXudKVER09JbhXGBp0obM1UrdBAzCQuapDzUTRHUnuQKAZoxArY0vRPBSUlspkivuUpyMQWWr2ccAFbjrjSg7vGu3UauT2w5yyeANmyO8Ahl3qhp6KFpeqybUSuh4T2OayoZkeQy1D2wmQL_66nemf9-OEJPk5B4F-FelCQZjcYGt6dHCzBfVEFWgOWrxb6Y7RkgKdjBKpmhSLU9apnM5qlOfesdxeWJ3aZfmGlLr7oLAa6xM5qMwHmCrHt2W0c6QbSiu1fWZC4scX4flM1be1Bz-47APzM5ssHK18kn1Fa_nm-52puoWVxjlvTb2-3UcjHzt3SHVouRDti3fh2fS07raNlP_argnTmDIyHN0Cyy0NcznHCs3Cq1ZcZwPhfWg6DzOaPm1wxj6RGBv2HESlvdrbIodI86SMzqpMsB-oV0dnV-jyYwC7cEXk0DpbprB973nY6cKQf_WQ128cvlFXFaytyzKL7gFsR4Qe_iYM5YM-mR6m8MiZbVllhNETl-zhhwkVwfk7Je8,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2TUZ-lWmt4dPS2Cbs3w-3N_ZXjxcw9dAgtOYoN6Ss_DcoiiPc3BO5ORxOiEDerntgD0WcAuiSEeEUKKfuIEY37vcmSgG5MjnYPEU_GPBmEHZY3broJbxItU,&b64e=1&sign=67bc9927230d51207c3d1f6e0c22d440&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: 225
                    },
                    rating: {
                        value: 4,
                        count: 9,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'Мастер-Класс',
                            ogrn: '1087746203973',
                            address: 'Москва ул. Верхоянская д. 18к2 цокольный этаж',
                            postalAddress: 'Москва ул. Верхоянская д. 18к2 цокольный этаж',
                            type: 'OOO',
                            contactUrl: 'obliseniu.net'
                        }
                    ],
                    id: 159416,
                    name: 'Эгегей!',
                    domain: 'obliseniu.net',
                    registered: '2013-05-20',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, ул. Верхоянская, дом 18, корпус 2, кабинет 5, 129344',
                    opinionUrl: 'https://market.yandex.ru/shop/159416/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1870965844
                },
                onStock: true,
                phone: {
                    number: '+7 (495) 204-16-33',
                    sanitized: '+74952041633',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mf5fIUVrEGur_sBDKtpRPCMdRvZ_9IBYLP_IkQ6-Qb2RwF1b3sXRDmyPh6WEMArs3TW4gwsfeZyECGmPzd16kqrA1bkyGNupuBLqQic1hIB0SYG-hOywojVG9Bgx0077u1DUHtTmTz6jphYsYe7QRZ9qUb4eoDPZcTbiRqz35in74igJ6HwOhkRmhSkWyhWXu56xpYAIImNYCG0720OSwn7oEph52RJ1dBgNkZAM4b3-dgA6uOQeBWSOgnAF6MVFlSthyPuzoyDkZRRoLvnwyCUN7BR1_gZ0wy9EEzOwkpx1bcicdTorbfG6emN4ick0HNeB8i2Fqya1Lmr8WKGEFYtKX2kG_nuPd4u4RIYECUCiNrq4AHk365jkxP9G4be7413ByHPSTYyQ5wSa3qYND4MWM86o3OR8pbgSPupVt1LMIAdfuAKPt93thoAMbYR8ebGPFKG-CTPxhwL3n7fG2VOU6KML7HGC7TYmx1igjh5XhJw3XsHnKpdoalSgQzJQ4_0lXcAuyw-ut04Q59UPN4a7Uf5IgQsUx8phkMJmmDcOh6fGVvDqF3S03duiHDzkDNIo45jyKLMupTGHZbOeAJWFI62L9VV5aqzvs_EuBgPJRsUreuuFeQM2p149Eex1Sonm5iBm-cnLVmaUDdfNROUwAAHWSjDwEJBlHrdnKN-mXgTnz-lxVUaQfOb_yHzIO8s-AHWiK9VeelxAaoCOpL_9uY4r4tjVxFYr2WigOdMlaf0o3c9GM3cqmBfnhPlXPqs3wYR0Mzkn09TFKnH4Q0gVkOZFuqpo60mrSD6ciNRTEuQZwqVj9WE9QVP1AXq9qaPoqzQ88p1IE_M2JIxTMl0RtS7RZvPw2PXNyPvqEuhxCNbI1hDMw2VrNMC7yI--Jg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9QPegW6lWiKj47lYjwVBb81ZBzfe1McFwgbXihhSuzbOKT3DP7j5p6yk9Da47PIAf_XmAQE-hGaBty9JmJRuA-7VuOra-PZBzex1Z85d-xT3TIHWJXfGZUeHb6uMkWSL76BrFCx00RjnZuLUsci1Rhg1XSHEIoAz00dEmBmac-cMiF5_a_CeO_&b64e=1&sign=5be2ff7dd8f64e4c3263e52d0e6e35df&keyno=1'
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
                    brief: 'в Москву — бесплатно, возможен самовывоз',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2
                            },
                            default: true,
                            brief: 'на&nbsp;заказ'
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
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/KXK4sVywT7zraPRNLPILLw?hid=10682647&model_id=1870965844&pp=1002&clid=2210590&distr_type=4&cpc=OTwDT4NSyge-euPr-9SXDOJZM7unu_GPzDFZYQH17Wk1IlepwuM23bpR5hyPRldhtGwE0a0sbRUZkLdk48afilF4dpY6b-TjU48iMrs4eYL7ZVPf6V7162JvttRc3vGQ&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 1048,
                    height: 713,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/237949/market_Ovzuuk1DOZkK0HcEfnEMJg/orig'
                },
                photos: [
                    {
                        width: 1048,
                        height: 713,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/237949/market_Ovzuuk1DOZkK0HcEfnEMJg/orig'
                    },
                    {
                        width: 472,
                        height: 331,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168879/market_O7HM3RBl7SL0DWlAsw-ggg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 129,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/237949/market_Ovzuuk1DOZkK0HcEfnEMJg/190x250'
                    },
                    {
                        width: 190,
                        height: 133,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168879/market_O7HM3RBl7SL0DWlAsw-ggg/190x250'
                    }
                ]
            },
            {
                __type: 'model',
                id: 1730873230,
                name: 'Настольная игра Hasbro games Monopoly',
                kind: 'настольная игра',
                type: 'MODEL',
                isNew: false,
                description: 'настольная игра, тип игры:  стратегическая, экономическая, от 8 лет, количество игроков:  8.00, семейная, материал:  картон, пластик, металл',
                photo: {
                    width: 569,
                    height: 541,
                    url: 'https://avatars.mds.yandex.net/get-mpic/331398/img_id8167736387850296343.jpeg/orig'
                },
                photos: [
                    {
                        width: 569,
                        height: 541,
                        url: 'https://avatars.mds.yandex.net/get-mpic/331398/img_id8167736387850296343.jpeg/orig'
                    },
                    {
                        width: 701,
                        height: 626,
                        url: 'https://avatars.mds.yandex.net/get-mpic/175985/img_id2846629008986078716.jpeg/orig'
                    },
                    {
                        width: 627,
                        height: 427,
                        url: 'https://avatars.mds.yandex.net/get-mpic/331398/img_id9204604579282942819.jpeg/orig'
                    },
                    {
                        width: 627,
                        height: 427,
                        url: 'https://avatars.mds.yandex.net/get-mpic/199079/img_id1929678115162906570.jpeg/orig'
                    }
                ],
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                price: {
                    max: '2280',
                    min: '1470',
                    avg: '1929'
                },
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                rating: {
                    value: -1,
                    count: 0
                },
                link: 'https://market.yandex.ru/product/1730873230?hid=10682647&pp=1002&clid=2210590&distr_type=4',
                offerCount: 49,
                opinionCount: 0,
                reviewCount: 0,
                offer: {
                    id: 'yDpJekrrgZER8wJwZh0RAO57QzAPR9klPfwtHn5cMHIDU0-lO6u_yumeO4yrhlkXs_LJgBavdjJK_GvCOezhQ3GYY5-_A9Ey-E3hvMt1uHDbfC8y1AY0_YbjD6fkoODQ1Bhb3Wo234k9mrHUaAHcgU8HupZ5xppXMJsgFiRPVSeVMii3ztn1Kiyas_yigVVRWg7ZdPMWFpG7RuX-jkZ6zLlRDZOJYBS5mnWteQDQvFyMP-HXA_5CaLQWEScTUFgJJeVEcAJUuyXDNnGZa4KPayhI1zZfw-ppqIPICZA27hk',
                    wareMd5: 'IatYqYfCHxw2G1FsxNnatQ',
                    name: 'Настольная игра Монополия',
                    description: 'Известнейшая игра во всем мире стала настоящей сенсацией! Передвигаясь по игровому полю, игроки покупают и продают недвижимость, возводят дома и отели, расплачиваются за аренду и получают зарплату, берут ссуду под залог имущества и заключают сделки друг с другом. Успех зависит от удачных сделок, дальновидных капиталовложений и разумных договоров. Победителем считается тот, кто первым разбогатеет настолько, что сможет, скупив всю недвижимость, приобрести собственную монополию.',
                    price: {
                        value: '1470'
                    },
                    promocode: true,
                    cpa: true,
                    model: {
                        id: 1730873230
                    },
                    phone: {
                        number: '8 495 988-29-15',
                        sanitized: '84959882915'
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
                        brief: 'в Москву — 350 руб., возможен самовывоз',
                        inStock: false,
                        global: false,
                        options: [
                            {
                                conditions: {
                                    price: {
                                        value: '350'
                                    },
                                    daysFrom: 4,
                                    daysTo: 4
                                },
                                default: true
                            }
                        ]
                    },
                    vendor: {
                        id: 15157809,
                        name: 'Hasbro games',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                    },
                    warranty: false,
                    recommended: false,
                    paymentOptions: {
                        canPayByCard: true
                    },
                    photo: {
                        width: 300,
                        height: 300,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/200038/market_3WLKpzhsPnVa4Vu6e_5lSQ/orig'
                    },
                    photos: [
                        {
                            width: 300,
                            height: 300,
                            url: 'https://avatars.mds.yandex.net/get-marketpic/200038/market_3WLKpzhsPnVa4Vu6e_5lSQ/orig'
                        }
                    ],
                    previewPhotos: [
                        {
                            width: 160,
                            height: 160,
                            url: 'https://avatars.mds.yandex.net/get-marketpic/200038/market_3WLKpzhsPnVa4Vu6e_5lSQ/120x160'
                        }
                    ]
                },
                filters: {
                    filtersList: [

                    ]
                }
            },
            {
                __type: 'offer',
                id: 'yDpJekrrgZEcH68tpNtFe9NuWrnrUXkTxJBSFgyOgktWhq11r46w7igMk5C0zkkI75Niavs_vcOxD1IG2O_0K93U7UOtv5zkRIq5B8O9NJ2zdrkso7kygCDMAG6qiPGdjMcwQNN8FDOYhqtiBsTTkbcSdlnDBYdomrxO6-8jtYKYrzJukTB8DfcWjIH1ztuN7CqK37_orqQs85ER4C7IlIQqF65zQNfQT2UQDANc13jew5fhqn7uw5XxeCBWvwk4QR1z5rNns3tF-Mwu81V6UrbsJofkk_15krJQ1K3Mw6k',
                wareMd5: 'Tj8nUMXrdf8fcXKTIRhtvw',
                name: 'Настольная игра "Монополия Империя" (обновленная) Hasbro',
                description: 'Обновленная версия настольной игры "Монополия Империя" от компании Hasbro несомненно порадует всех любителей знаменитой экономической стратегии, а также людей, неравнодушных к самым актуальным топовым брендам, мечтающим построить свою собственную мощную империю! Настольная игра Monopoly Empire - это переработанная и улучшенная версия всемирно известной игры, покорившей сердца миллионов.',
                price: {
                    value: '2140'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86Fag0qDFrcRS2xLI8PPWYhy-RW__e1QcvpqqZN9XFo_URMsMu4Js9o05heeDbtfvMIHlcqVjXC9rtZZTsrX0NNAHgeGYLFk9q8AOeoTNthSbW6At7JLnOAx8cpC52aMsNJpfLn-DKBsipUhdcnoZ4PwkRb34ffXtNfisNxKHhF_wwCZ5tJSSaocyfPabc29DxkIcxeo3yQ1jIVz8l0qVaiX2ixycZaqTvtM9-pZNlGikUktYHVaJYVWOhPLDPCc87pXyBGRCZy3s1hn7GsfQqttQDWOsezC2VYEKcPCYqj-cu8fuGF_2MHgFccfNfl8373AK8teWF8Xft3rk-UouhEkC7Cq4TmxjY2crIdn_7NyQdtetNFFZNG4Ji2_QrccDdjD6oAoIgCWwa7G8QHTtWBBgMTJm_nDRMQvmT8b_RepH_kyBlot0PV9jHaog5z7_xixZMVmgscoXsurki_ockPODRAjz014-9RCXZ-pjD4nS_uUDQzT7mreZ-QoexAdKWrTjHEGYMn0OZBaY1rsCyew8xOgKHvwpZ6NSiYwVh07qQrU-kQT2Qd6TOEahZs5i15mTF1BqVpm8xG55ciaYzMamexyOXfKxwM3VDEOxdxHXG_mCDLgd66me9kYX7ZdCkMiS-ElKR-8lvcw1S1QZCdJWOLwTdDxsbTmy5KZUxwsME5jgqo5SO_OE0dPvY4ZvGGzvx37tdRjN0jQTJfNNfsGQdiIweOAuvJwSVEvaWnZ0NJcqqQG-t9u8t26OaqkgmMyPmYveBiLpN5o6kilhxRCMQkH8QHKei_dEg-Kz9DAlfnLiAVdr5iR0JEJ-Ef2cCFZk7QRbIfIivlKD9Y0c2KG5Kvl5ly3hdAi45FyNZ5TA,?data=QVyKqSPyGQwwaFPWqjjgNl-TfeFnXmyUxowmb_qenor47tF3y6bBOLHoSuptdxC3NVkQcvUdP7prn6844t1ecnmzlluD_P2R7wkuPn-t6a7-YIRJLtF1kJ1tQNQZKG63biav4GVl7rgRTYwv5z8Td0qVwg3LouBlmm3qfnegWDHOTaMDDxO6FEHjfTcsg-URR3Z7tSZ75cvAbVnaEABxGl4SzHfq7ceU95M68pMj6U4OU-gPBKD9hw3ckBtb00s7MvGE2njpue1oDsrHR8MkSLYrzJu4p8P5&b64e=1&sign=8d7cac99c83de3d937e328bbe1ec1875&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iZfzb2Me2YRFMgHw9TR0SOVI9JEAKhItL9oisERXScK8b-r73zLyzKzz9kmhwDrNlJzeAKGWD3M3L6UCQhjil0vHn2ZumEvA8Gz4T5e_SZ28ZMOORRpHESrvJBRe2DSLLIkgT2idy6-K2BSf57Gskj464KWmquoJjK5BOQj_HfrkcBs3Guxoh4NofRlul-gL6tk_ExSXYG-6LJX7cnuE_sl-eRUiLJM4WA6aJ6q0PSfSFqMZqFrXeSS0GbIyXc9g0Tcx6r_9ZSF69wfQt85ozvuzkIY8YOFwF0cVaMj28C6djxDRjsB4-DDHwMtQXIRZ1LtQ7iayy6axJuYTu-2eCFvtKxXop5bMUfItGCJBDpXpXXCzyUDo7T6i_uAGopztkr5dzLBgLQ8YPEQXcdMvi72YIlyytsI0jfTlT6t5mIhp5tyjTeAh0Rz1o-HOooV0pknLYdZG0Xdo72ahi3Tq6-h0d_KQBFdLsuXIxf9yDxvRGWobCAaRao15OY5SDJU-mPrcjli6l8VzWqKROVjdNnxXjnklmSVJm7MC12Y_LSyCYZAiG4V5-OlWJUQVC6UcoOaGmToNROu7APmnAlUgANU6UIA0H9VLMi6_mk5b8fSUXquvdtgIo7vHqle4ONr01qS90DI6kTo4a6c1QHgJ1r6cHKR_8j4IW-kyd_BfyoM9GUes7gnWHrwi7gg4UQfUtSTPYIYd0XFqiM6N9Ir10r00L659j97raKHKjqwVsryCCxwh-QQ1rpp1Zq46sIqtx5_jRelBF7-_9ltDhkwk-J5xTOLbZum1Dg7ebAVJiUUuj2qYNK5LqehV-xPHMPBAmwHeOHEYYLlsgGFysLnPy4egeEkj7dCjwE2E9M1eVi5M?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2fMi_J6qWa5_xD76JZJapdhwQnm1CG2GwCeuo8vOxMkCIKPAROLe6js99MqeeQ__TC4u0pvfTF3WWVzmC8hs4OhcQbi_Enn8xoVZ75ncq9LBOmtN1yC9SYc,&b64e=1&sign=312cbbd719200b008075b63402704684&keyno=1',
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
                        count: 7929,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'Знаменский Кирилл Викторович',
                            ogrn: '311774631401123',
                            address: '105037, г.Москва, Заводской проезд, д.10, кв.25',
                            postalAddress: '115230, г.Москва, Электролитный проезд, д.3, с.2',
                            type: 'IP',
                            contactUrl: 'www.v3toys.ru'
                        }
                    ],
                    id: 432348,
                    name: 'V3Toys игрушки',
                    domain: 'msc.v3toys.ru',
                    registered: '2017-08-17',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Трубная, дом 32, корпус 4, Как добраться:  м. Цветной бульвар. Из метро повернуть направо и идти до светофора. Перейти по пешеходному переходу, далее идти по ул. Малый Сухаревский переулок. На углу дома 21, стр. 3 повернуть налево, пройти 150 метров и повернуть направо и Вы упретесь в д.32, с.4. Вход со двора, 127051',
                    opinionUrl: 'https://market.yandex.ru/shop/432348/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875295
                },
                phone: {
                    number: '+7 495 278-08-75',
                    sanitized: '+74952780875',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86Fag0qDFrcRS2xLI8PPWYhy-RW__e1QcvpqqZN9XFo_URMsMu4Js9o05heeDbtfvMIHlcqVjXC9rtZZTsrX0NNAHgeGYLFk9q8AOeoTNthSbW6At7JLnOAx8cpC52aMsNJpfLn-DKBsipUhdcnoZ4PwkRb34ffXtNfisNxKHhF_y8-gqc4AvJFRSgZty_5uGSgciOu790lfE414aaNWT_2Hz1W18fLqSGWxlZHvBbMnAGgoGK8hetSMBvhpKDuLEyBtHu-yDI3_y5-E2HdpClcEm122dQoIK0ARAb5xgWzbEE9MGx3MXP0IdCKjrUWUl0PAPCOusEok2Um3xQpyCshbYv9l4iNfT3U2WXhMW2R5zCPMqK76khDHuSdqj0Z6zRixSAyHwujd8c7p_9ue6bP-KKksxnzFIHEffiHgzZpwQ51FlBpndWckfdJohqKnS1ssiDSjiKjnSTIKxRC9PuyRmj_68yzNwHHDzJCKgSoj9NzTf71Npd5Dn_8yhIvJcRzSKhYtNqcbYCBLVzfoHOZuREw7nSaKFhEJMCh5xFAs78Xf65qPkDC9essVKJGOg5Yia6ZM5oAKv3AodO6whMr8z2kblUoGqZd1mNdvXtqtpcGF8dBmBJfZz2khsKYRfT0oT9bb-eSUJjINxg4AdVHOe-AE0RBrM0C3JjDYg0gylcpoCAwoL3DyzJeseYT9wjHX2RvUS8RbuuXAusfFeEMBDYrlW4CGJU2tzGepTccUeFsRldGhJCTTsqyqQknDxJCBaG2-CEaoRiIfJcZu20Cn5tG4R4Y_mkDaTDib-vcbUA5A6lXxTpWsohgz0tlti-t09N_DuPh_y7aj_7huHRNSK7GUFR3iSFBrUAPLVJlO4,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O87C6mya3S7GkySkJNlwtWbjecjyMaapiLooKve_hhbakRoLZcfcW4zu9mUHVaiA_ZA10PrMGQpSU-FmYSZw98OxilqN4ExevOSpAlNG_2Gc130f9WaBt6yESv1DW_a-ecnBvNY2WItlDT0WLzemE3JocrtDR62nr47r75AMyCotjmS3O7NXPEf&b64e=1&sign=c46d74712f6ba8a94594dd165674573e&keyno=1'
                },
                delivery: {
                    price: {
                        value: '199'
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
                    brief: 'в Москву — 199 руб., возможен самовывоз',
                    inStock: false,
                    global: false,
                    options: [
                        {
                            conditions: {
                                price: {
                                    value: '199'
                                },
                                daysFrom: 2,
                                daysTo: 4
                            },
                            default: true
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
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/Tj8nUMXrdf8fcXKTIRhtvw?hid=10682647&model_id=1730875295&pp=1002&clid=2210590&distr_type=4&cpc=HI3uXCIAUCPjaAc9vrpgr-1N7NZrVaoDfvFiU-1VQSzfTSTm6EyRT7Sb8QhpnI-JOtdE1yDhZZ_jab9-wxNQbcwzY6Q7Wi_VE7dqGVLFFreWnlUXusOwr1uHFcXP3HRa&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/169986/market_syRM3wFB80h2CPwT_yi5SA/orig'
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/169986/market_syRM3wFB80h2CPwT_yi5SA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/169986/market_syRM3wFB80h2CPwT_yi5SA/120x160'
                    }
                ]
            },
            {
                __type: 'model',
                id: 1730873505,
                name: 'Настольная игра Hasbro games Monopoly Миллионер',
                kind: 'настольная игра',
                type: 'MODEL',
                isNew: false,
                description: 'настольная игра, тип игры:  стратегическая, экономическая, от 8 лет, количество игроков:  6.00, семейная, материал:  картон, пластик',
                photo: {
                    width: 673,
                    height: 556,
                    url: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id472791615262683671.jpeg/orig'
                },
                photos: [
                    {
                        width: 673,
                        height: 556,
                        url: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id472791615262683671.jpeg/orig'
                    },
                    {
                        width: 692,
                        height: 556,
                        url: 'https://avatars.mds.yandex.net/get-mpic/466729/img_id1657182183095953525.jpeg/orig'
                    },
                    {
                        width: 580,
                        height: 468,
                        url: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id2076405131158579616.jpeg/orig'
                    },
                    {
                        width: 476,
                        height: 412,
                        url: 'https://avatars.mds.yandex.net/get-mpic/199079/img_id5233568731120126167.jpeg/orig'
                    },
                    {
                        width: 525,
                        height: 364,
                        url: 'https://avatars.mds.yandex.net/get-mpic/175985/img_id4738368564312648826.jpeg/orig'
                    }
                ],
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                price: {
                    max: '2291',
                    min: '1740',
                    avg: '2290'
                },
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                rating: {
                    value: -1,
                    count: 0
                },
                link: 'https://market.yandex.ru/product/1730873505?hid=10682647&pp=1002&clid=2210590&distr_type=4',
                offerCount: 31,
                opinionCount: 0,
                reviewCount: 0,
                offer: {
                    id: 'yDpJekrrgZGUe75y7aHnnVDeVIayaNBW6F-v449z35twHpNkhG3qo99qLz5FBcf2LRdf7kZlQ6XnZhzzJyE5v6d5Zb4VCg7aq-5hR4eZupT3iAB58zMFIPp9Z3dX3Evsp33dhawdcI2VvP31glw1FlZrpC-Wp0J_i6KYwNAUaw0gcdYzSgFOxJJTO0IgLOafb8n71oa2Nwv3SBE0m9pU624w8nUKyAIdOBHaRApHqODmBu1nMOkQ_aHfYr1pb25L6t28LmsGgmCtGpqaj-l8zT0wz3MXiUyWFfEymEUWNL8',
                    wareMd5: 'AeNgzoBzD8Eox60tZqL9CA',
                    name: 'Настольная игра Hasbro games Monopoly Миллионер 98838',
                    description: 'Monopoly Millionaire Бренд:  Hasbro Штрих код:  4602010469783 Возраст:  от 8 лет Для мальчиков и девочек Комплект',
                    price: {
                        value: '1742'
                    },
                    cpa: false,
                    model: {
                        id: 1730873505
                    },
                    onStock: true,
                    phone: {
                        number: '+7 495 191-89-09',
                        sanitized: '+74951918909'
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
                        brief: 'в Москву — 250 руб.',
                        inStock: true,
                        global: false,
                        options: [
                            {
                                conditions: {
                                    price: {
                                        value: '250'
                                    },
                                    daysFrom: 1,
                                    daysTo: 1
                                },
                                default: true
                            }
                        ]
                    },
                    vendor: {
                        id: 15157809,
                        name: 'Hasbro games',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                    },
                    warranty: true,
                    recommended: false,
                    paymentOptions: {
                        canPayByCard: false
                    },
                    photo: {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/203037/market_uVapZsP3fGSYr4UTAIj-7Q/orig'
                    },
                    photos: [
                        {
                            width: 600,
                            height: 600,
                            url: 'https://avatars.mds.yandex.net/get-marketpic/203037/market_uVapZsP3fGSYr4UTAIj-7Q/orig'
                        }
                    ],
                    previewPhotos: [
                        {
                            width: 160,
                            height: 160,
                            url: 'https://avatars.mds.yandex.net/get-marketpic/203037/market_uVapZsP3fGSYr4UTAIj-7Q/120x160'
                        }
                    ]
                },
                filters: {
                    filtersList: [

                    ]
                }
            },
            {
                __type: 'offer',
                id: 'yDpJekrrgZELTmI8Lcj2YSPdpxz6rDkrjBSSfDmMVlROjGXLO4R_bLqZiZ1-Boo8Hvh80sHF2oNLisHgLZlV5SEB3TBJ752aXrYqpFB990M6atThobNEyG-nP_F1jX4IJyfRxvpIqP9R3Vloi1i2bSaffQzeQp3ntOwMudwc1t7Xw2xHmfZ2sshvpvigdWZIOhrZQ-l-wiIwrZEZEeSQsa5nWDSz6x6NMW2cTbJt00QOlVOy_5DIME58qgTc7V1bh9-JY59Boau3QVYAfBYogVMDzRt8ZEXwE9wQHKt9HDU',
                wareMd5: 'cdWTCpFeVVGPTbJtq9OHjQ',
                name: 'настольная игра MONOPOLY',
                description: 'Торговая марка — MONOPOLY',
                price: {
                    value: '1999'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86Fag0qDFrcRS2xLI8PPWYhy-RW__e1QcvpqqZN9XFo_URMsMu4Js9o05heeDbtfvMIHlcqVjXC9rtZZTsrX0NNAHgeGYLFk9q8AOeoTNthSbW6At7JLnOAx8cpC52aMsNJpfLn-DKBsipUhdcnoZ4PwkRb34ffXtNfisNxKHhF_xRUIQe7GRZx9W6rBakjz2kXdggnXyLR0--gx-mTms1TmuKGzNKu5BMJ3ID91lPmUdJQVH-jIdK-UVOLqBMu9lTEwuFEHjlQz7SHVFqwu3qyCMwSY-LZomAVD7lI3elMxYbwM0IfArnYc8FLeLFzIO_IOqRHEvUMW1u-HnqtBw_oEN3BdLUbNe37UepHyD1iiYydWZt_S__Yk8TRL_1lvb00s7Bb0lesxkgWqbSvR6Sme1iNyoVcdMO8Z6PiEJCFip3DDeKDXuIaukMOEWiwwJfAxt0kIEkSJ4ouGhKFK40Aagd2Gts9c4WAsHQZVWuuHLL33xqMZYMWJ9Iy2gtZg7gJ6v008BTcHLQP7fbuHPkQ9pf25DFOHGPUt5tXgvMJ-zofg5LM3XdFWiaPSK2nbILKnshdHIU9rL0rIxslp4rUdTkbcSeckuMERsu-goYirGpYuIffWiBShmYpJ9BcSIswvJuGX3fY8bhN0RJi2zhXcZ-9sI5S0vXGtT5OuCncdT-TPK_sAlkog2WWSlBfwnCJpqcQfPm7hTemEife18Pvzc2s7_NdnTP3AwpWZrTtClMdAy7lHJUVZeCr6fFzQ5dmP9fKAm7zcuP80ybJK5qY-NubnTBD8gzkCLiHaR2T9STsy5syZh7kKiZbxwlA-CXad6-ZKsIvl2iTYboZ6HpWg,,?data=QVyKqSPyGQwNvdoowNEPjW_7ELrJbv5FvAxqUvXb3o1v-dCJwy0gkIGoI30lZNPJZNb6aRbaQW-a4Wd5p-0zgnrSI-OF6P5JMrJmU5ZTJk6Ua8X9VwMYf-NpCD6lHVODyiMkmctKuFZ_UsagodqcAH-CahFOJ27lgKnxJJFV_niYBPCSJVWzoH5oLnjHrm3o82Yvszi0yl60Fqiy7VeWJgV9zOzqNeu7oK07gFAMkP6ehWyd_ERwxOygo2eTDAGzweiICBiLNLbmvZXHfv2cB81B4q_9xEBuitYoSgDt-zHgckKfmEXcBZVAb9m9qZ7QITPRPB18VoO8VwqhQYmOZ5KDVUv6173rz-Jnl1EpWAjxf-vrOBbmbezYnkEws38zD5EIhTyFVSRH-m0ReKY4R90co3g0luTRTiqfBjlk-oCxmXnYvdXvXVi85Im1fqJHx8SwGmBrlgFtqz0_XsXTeg,,&b64e=1&sign=eb03cf17b70f050b10cfbef35c2b90be&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iZfzb2Me2YRFMgHw9TR0SOVI9JEAKhItL9oisERXScK8b-r73zLyzKzz9kmhwDrNlJzeAKGWD3M3L6UCQhjil0vHn2ZumEvA8Gz4T5e_SZ28ZMOORRpHESrvJBRe2DSLLIkgT2idy6-K2BSf57Gskj464KWmquoJjK5BOQj_HfrkcBs3Guxoh4PRC3Zab4N-1pKa_4shyBHjTm4RxEYHJkIMyw3qfdVXm_Smv095_EM-4QVIff8vd00O9Bzdb0S_8O6x4XDx2RDPQH1YopCcCixTwletsxa2lblA0B46cvBtY4AF91rIwwVlPE4VUW-Zg_jgp-Nkq4NJnwvt3VlV7KtDRjhpygFGcpfM-JsJWONtWcWxNWi3NtkJTWwNb0ecNWqHlT4SEA8yJBM0RtFgfvM80SOJSlL2RtpI81bUqSMpozBHzjU6f-3Kx5JBkfbniJv4NjciCRzLUPFwIhc0byJM3e07T1k7N9ivrS7EYUFRqKa2SsAfvWJpyueT0gXOVaCoc8BcGUtSX22JoIPUQ1J3devM5EN4qOX8OdUqR5gnrLAlo_Ee6bnKW4gHJ38oNcXRDgNuVw1Aks4jkAcSVoV6Hg5WojR3uFyeh1hZpjqKXu169TC34H5jt6FVa_B5yIIMXnOr8ruewrlbBrhsjJ40mOIrdhbTb5KW7xOTbXwQA0ZgsGUX7p9aIZ-sLdWSf27IIEXQmI1o4ycxQQxe2UqZa3DKLhP-t7kP0W2_ollVPbVqRx3E6lNocvElmgM6aTZE3nFxthS0CStaI-zB2AzkDYitAzePw5uDwePPr6FR_pWmSF_fTVN47sQMocw0yQMLWZRLJM3c7HLtnpl1-PiAbj4vVnRhqg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YL0Kud43qAL4UYw98j4Zh80kkmJlpOUpINe4Iy0Uo70Hc7S-s_XhghDxS2e6yPAXMH8s7NTaFeX5u43CoIGJ32_Xn1dMb4NiIDwD9RBcFmCKKrYqmBl3KU,&b64e=1&sign=942701a5d2ecf782110f2997a0f1c83f&keyno=1',
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
                        count: 192,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'КАРИ',
                            ogrn: '1117746491500',
                            address: '107140, г. Москва, ул. Русаковская, д.13',
                            postalAddress: '115432, г. Москва, пр-кт Андропова, д. 8',
                            type: 'OOO',
                            contactUrl: 'kari.com'
                        }
                    ],
                    id: 366769,
                    name: 'Kari',
                    domain: 'kari.com',
                    registered: '2016-07-08',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, ул. Автозаводская, дом 18, Ривьера, 115280',
                    opinionUrl: 'https://market.yandex.ru/shop/366769/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                onStock: true,
                phone: {
                    number: '8 800 200-10-63',
                    sanitized: '88002001063',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86Fag0qDFrcRS2xLI8PPWYhy-RW__e1QcvpqqZN9XFo_URMsMu4Js9o05heeDbtfvMIHlcqVjXC9rtZZTsrX0NNAHgeGYLFk9q8AOeoTNthSbW6At7JLnOAx8cpC52aMsNJpfLn-DKBsipUhdcnoZ4PwkRb34ffXtNfisNxKHhF_wtH0NIxvJQ810lC8vSt3deB1-fgSwygLmCCIosvp4aGhwflncQ7dlJdVDyurgRhvyROW2hFtw4b00NmsWAVj_LKqZ9Ji6lHMqNJm4TGL3YoecaMLoZe4h8hVLju6pwp0TAfIseGojg2WiztvOovfzNjCOIq7yT_TNxcPlW73c6hLRPvU8qHIrtbjJ2kmerdKOuiHLf0gpy7EKMATSiHcNOwaW1WcLKlyO6-BE2Bmu1W1OBvYTjDrwQQs5sv5YBGfbSlSM8_XxhTBpgocbQyBEtCpBpfncg5WDY6UtUrAsD-Ygwqz8aUWwRnDDB21SsftMUXCxZ19J_EBgh0-sqUml28a0qPldKa9fxFM5uwEPUUxzlJ6WB-JMTwWbLDHdkPEeGR2qXixryTG0g2hnNTfV3TnzAhuIHy0IRBW_TqgRO5BnIGcB0JW9QYROYMz0ojTO0rjp5ubWykcLX3rX7yEs9_X-aBvJKhKLqpi65OCGqHjQxqQQXqRSf48JDpWDHlGA2jRSJzYNPTUUFHZ9qfLpKJhwsGoStSc8W0Nn6FlN2_doq2u8W4Q0XE2LOvlosru4B1sKeGxbHN7WqxWxTHL7akG5gv6jOCRYb3A8m83VxwJFYjSboPwVU198UGxR6sFeDi7F1aPkPiFE0QquPSLvHo4xBaO_VyAePf2Q8Kf-T3Q,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_rYYjT_xXN7yoKLsxQ9GkLFJAJppgPDWcoOWvVddtLjTopo2KqJRElAon_TDqSVS_agtdZwhOEHJ6YNQ8T_Py7gGzOtCXmCsG2toIhLarES3yjkLYYpVlPwmsB3XFVjUxXkTKutE4g1LgdeRUOttTa&b64e=1&sign=fa15d66d978946871656a3bc9fb502a7&keyno=1'
                },
                delivery: {
                    free: false,
                    deliveryIncluded: false,
                    carried: false,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
                    localDelivery: false,
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
                    brief: 'не производится (самовывоз)',
                    inStock: true,
                    global: false
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
                vendor: {
                    id: 0
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/cdWTCpFeVVGPTbJtq9OHjQ?hid=10682647&pp=1002&clid=2210590&distr_type=4&cpc=HI3uXCIAUCMAdLTuqpE6WIdjCkm6pcEvy1zxsmLr1ert9GH_Y_hfYAxHf3P2bYT1tTS3__AuYfjmjpV2wT8nAFJL9zH3T0rwXc4PqQnP9TeTFeH8Z_F8_S_yoJUluzjp&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 762,
                    height: 1100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/169986/market_w4EtU4J5h5he75sB-gjhOg/orig'
                },
                photos: [
                    {
                        width: 762,
                        height: 1100,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/169986/market_w4EtU4J5h5he75sB-gjhOg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 173,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/169986/market_w4EtU4J5h5he75sB-gjhOg/190x250'
                    }
                ]
            },
            {
                __type: 'offer',
                id: 'yDpJekrrgZHi7VFft0m6pjuBOur2ty8h3sP_8K1MVIF0urZqURmCDQ',
                wareMd5: 'gSZbdK1hnmSZJBCB4uYs8g',
                name: 'Настольная игра Hasbro Monopoly 00009 Игра Монополия Классическая',
                description: 'Монополия от компании Hasbro - это классическая игра, обучающая торговле недвижимостью. Здесь Вы можете покупать, арендовать и продавать свою собственность! Эта игра с экономическим уклоном необычайно популярная во всём мире. Игроки покупают и продают недвижимость, строят дома и отели, собирают арендную плату, платят налоги. Главная задача игрока в Монополию - довести своих конкурентов до банкротства и остаться единоличным владельцем всего, что находится на игровом поле.',
                price: {
                    value: '1719'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mf5fIUVrEGur_sBDKtpRPCMdRvZ_9IBYLP_IkQ6-Qb2RwF1b3sXRDmyPh6WEMArs3TW4gwsfeZyECGmPzd16kqrA1bkyGNupuMW8wpShIJDV_Q_te_vyMdigKVhi4X5e09UGhZi4kfRy_OxSKAKCUUPOiohSaOoXc75CKbSQ3nC_greSlG_Vi89y9QPfN-2I4lca0XznBRrVH95raz7mYptO8qbz5vxpA4vuWPKN_HxSWuKbJwDIuBesaH1rNf9W97xYiC8P5a-bE3fb9re-fw_BJc8APY3RvWuCwhPFtqp6gn9QoRUqHmiuecHedFPigbrL79ylC3uHBIgp7rOsMklLvqcMsBrj9Q2wrJsd-ZYZ0u-uveh0a8r1V2rTEwXKbvuaxOItniKP3vQqC6_q9eK1CjSgAsO7yTj4JxwbDydAbHnMI06aLcRIP0atK-0vqdf0Y9nJLjmmKUeOl_9BEHkCRV26BjW1Ras6g5WOdMx7IOuOSHOO2gpyKNOa2ZKGNcki_qxtSz89xFcS6F7u0D5-tmuXyDLAXF9mISQXsUBKahZ5fRbt4j0U2hnJ-6L5duEpy5G-JjAGCzwlSDp-d4rwY5rIfHZUoO--FGEflwrBdJsogIJPH-arsmGCFDi_b3hluIfylEPhslBArbEyEiLciA9nwGLaP7ycbjIQlLLDBS-ntRSUwqRM1ToKJGxeIz1hqYOpVgY-c1eyxQjslNQRbZQmi0OyADX080upHHt5M5vPHRPYFHMzq_Y8L0ea8vMUOKQJ68kjQ9ZAwV1TTcZiRZOrqKVlrLaw_ZuyUsgeXIYs8T7NUHMrsBejG2yQRg9_S0A70uRjVNCFpHqdzuncrx6Cd4IcylkvO34Ko8BgGA4K6OYg2bk,?data=QVyKqSPyGQwNvdoowNEPjRcd1X5IkopGx8TOY7lHsB2IzdZCVgSOE4bzcsD5Zw5ak1NTM6n-0GSHScszjyWSkf78VJLrXKILY5M1dRyAeNEIXBWT85YsELESUXEywLFj1n0iH9yMEsGbxWktf92fM6UWmOHZ4EogzuyecGEmm-_P2DiM9pbk89ynjCm3bGBWJiiZdM2qTKSfonzWrOmWHi091jvlpk_EUKnUV6Y6HR3htxlij41Sdg_IKZGeuu3ZV2PUDQMx0VOWgvKR-wF9I9byHoCeiBB7fggjEm8EBS7-frbJY10APViliESSRjzQi-zMD24GcupXLqjbX6mjf9XErn6gN7wBF050dyC9bIu1ictMHW3Vq5Ji_bXHwLUAR3-8uz0agyLdOAwUzocSYU0DSg8JYlG6IUJIGeKl3bk-2BBAgBPlKSZFISS4wGI1tkgL-WoRvhvuup9Vxd9E1uMgLUXfIK8FL5uXx5QBv3FyW4l_l-zQfgSzrfdQsD9w2R9cCog-cfY,&b64e=1&sign=a5b09e965f6a6423dd312cb8259d296b&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iT04ZeOIZg2GWrnLLzkO2HxwCTNMLESXOPx7ccOhGYLdI3IqAnBWzpQmBa3aCtxNtWq8gaqxtIYRnne25yijoju_XlUaqPAyB8JelGNKwn9r0l_9YtUfu9c0VzhKN8BoqkEDXpsBJ9Ew8an2G4veGsejmrUajTUT9GlYLX2fOv63BDFJMOOKBOmfDzb_IiR01m9oGjqoyWueSZagFBk4iPeK1I0I9KXRGZ4TDi8S4flL5r9v6EgHlB_jX_rkaJnVMuCYAQyGagflSbjp2X438fGlVmz_w7psODZhV0aej5_3Cjwi_UY_7rLQ-Tvlc21VZZNQg9bz5PPRcD4MGbgL-uu0Ti84MSoZCLfZPN0QUXy0reDzjSo5FfRSmSoCrRW1oAS_8ofAFIqPHE0pAsMoFbf1Nuef7YcyZj32Je-_UPUUjs-u9VkO7G3e7Yotb93zlSspp1kxe2KU_OdEvl2o50N7-uzMuo9h1LYrUAW8-9Lj199KJB06YxtTfvtjpb0sAtDIrz7i6gxzTV0SQthkRkz0IpQE9lCgSqxWLwQgRkWfk1Dot1SUitNQP_O8yTPm6umnGd-0wFVxtYkompQnZ2XUQ5F-giXlLwDo4LFvNOU5MY70kmkONukB47vqJQ5Y003acAoESzmPyoFwDEMEl_BTv72cWqTPM1pmdTA1KUIkMJRpIVTTkgpoLklELjIg6hKkgaH7MwsVaLA21hN7Et2lFLsymVHQ2SpBcasA_BxdZHq4P64a36AsVVx-PAyyHJCUGCd9TPyRXIV7cw7fPnFCUrfUhUEXkydv0fbTJ5VDnSD01Ezuuhz2HKY-iN6uLeeKPn9YXgfpdqmeXA73K2vJIZpFFpdK_uN5hnl3Btbh1YB8iWqSLco,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2btM1dgQVldwGzawYIo0XF_E1DGH_r98NFfdzquLNUMuowMEYQBV8zAFxgGewFdbTj7E1X7fv5753ByIz_DZbo_RHXP_0gDGJxu_iv1-dfjjralQC792pQg,&b64e=1&sign=5d60b7741ca44c1c7835c206a8bacdec&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: 225
                    },
                    rating: {
                        value: 4,
                        count: 5417,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'ТОЙ.РУ',
                            ogrn: '1165074050745',
                            address: '142111, Московская область, г.Подольск, ул. Вишневая, д.11а, комн. 305',
                            postalAddress: 'г. Москва, ул. Профсоюзная, д. 132, корпус 8, офис 300',
                            type: 'OOO',
                            contactUrl: 'toy.ru'
                        }
                    ],
                    id: 56564,
                    name: 'TOY.RU',
                    domain: 'toy.ru',
                    registered: '2011-02-16',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Профсоюзная, дом 118, ТЦ "Тропа", 117437',
                    opinionUrl: 'https://market.yandex.ru/shop/56564/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730873230
                },
                onStock: true,
                phone: {
                    number: '+7 (495) 215-22-44',
                    sanitized: '+74952152244',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mf5fIUVrEGur_sBDKtpRPCMdRvZ_9IBYLP_IkQ6-Qb2RwF1b3sXRDmyPh6WEMArs3TW4gwsfeZyECGmPzd16kqrA1bkyGNupuMW8wpShIJDV_Q_te_vyMdigKVhi4X5e09UGhZi4kfRy_OxSKAKCUUPOiohSaOoXc75CKbSQ3nC_greSlG_Vi88s_zVBLz1og1Z6eOTWG_cvRx6xC-Dh9JtiLadwpDWcK1_Bjc8fD-D7pPU5G0Ukx8iOKbS9spHiyMu_qqUrIKmilAQUISZyolsLYyEgIqh0_E1i43GKT-9eRSWU3Ol4PLAyaEk2vC2PssmjmcTnsIhHlyheQY-91Hi3_EMejzG8O4QEhIMnsKj3j18k_SG3wHAMtGZJwFb3uh9tnu-Cda23zHj2wEiZkImYLv3dPJeEO3Rw2OqESOHtMa7xztpdDsZTpwuU6kyhWvjAzGrM37_e_19Z9W9UEvqhnqkrLfDCXzOo7zvonZtCqLv5xsfYxtLst9MJbjiyQiEI6b5B3sdW-VfarRvilq7__aOSCKdxvY37EXApwZ_ZlAAr-kHeOnZgXfHcf4ampHMbnVxsuanB8QVAPdtde-p6jm7xC7uL_Pn4DniwI2b_T408N_pOpuR0V4pXsDinQXzosw7hmTLfClw3YMsv7i7A5qE7VYzjybXWiZGX6B6z2Euy6fGP4sYaMxcrTbw7TEJSf8H10PmDeMEowduBshgrrpox64xtvb0f_sGYmnaa3y0AC1XHqu0QoAUBcvNK4hZJnlzF8j9CG2-DqSKYihgxMZregg5ZxeEbzHp7m1cjSuXAQRTL_LSTiikqvan8ntxk_pp8JcHg_qy9apl4JyynPhYpZrc6I7Rt3Z_zIDFOmSswA8klE4E,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9Kynyyyy1sW62pDdka7rBrE5bGKPGQWHGk5JycHaWzpKItBy7MTRVIB6F2oJi3iW9eiYZ69bnrUASsgZxngeA5MRsrKNVPzCXn5dSUuvApZj06lZKeKjmX8cOpmDrqXgUB6c1RhfVxTyggNJpwpkfIZ9AzrEHRkTFEoTS9S4kHi7QK-YNmOpiA&b64e=1&sign=5e2149d1c4609f3d257c47a65a078f44&keyno=1'
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
                    brief: 'в Москву — 99 руб., возможен самовывоз',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            conditions: {
                                price: {
                                    value: '99'
                                },
                                daysFrom: 2,
                                daysTo: 2
                            },
                            default: true
                        },
                        {
                            conditions: {
                                price: {
                                    value: '299'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            default: false
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
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                warranty: true,
                recommended: true,
                link: 'https://market.yandex.ru/offer/gSZbdK1hnmSZJBCB4uYs8g?hid=10682647&model_id=1730873230&pp=1002&clid=2210590&distr_type=4&cpc=OTwDT4NSygeAo1jgfybyaLm2M-uMeEgOaEOvxkceZ_4bnQzjzryX6fXN_wmmBOqoZ8xhYmcoRxvgMl7RAA_WRqjjzS8_rLVgv0yIf2gwFK4mlYSii0Tx8f8v9nmteOQ2&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/163651/market_ezTBulavOCe4W24AiT1qTQ/orig'
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/163651/market_ezTBulavOCe4W24AiT1qTQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/163651/market_ezTBulavOCe4W24AiT1qTQ/120x160'
                    }
                ]
            },
            {
                __type: 'offer',
                id: 'yDpJekrrgZG5Lnkywb_VlfrmkwQCUjS6qBbOSIDZsvXBMSlGh9rQJ1vSxoMFtTARlS04eF2gPhJqoyXVZ-cpFnbpmqnknrkKn_0KGzsaTIzuQbm-CK7tTf0S40cMLdWkHT7BmgsbAj1fUApkhV7B6RrbIJi0E7n4-24od1_CtuNU_l3zSvc7E2cYGAlVXUlTeGabgd-80j1IhNEqTRaj416C87OdS0rGmnO6MWiXMmTn_a4zlTnkVDvMgJSxkafkS0yDgWsII-0uBwyoV55TlqBZ9WvpoZ8p30TKDWlJId0',
                wareMd5: 'kwZOZjk_sgvZsrfZmPLVAQ',
                name: 'Hasbro Games Настольная игра Монополия Россия',
                description: 'Просто попросите своих знакомых перечислить известные им настольные игры:  наверняка, именно "Монополия" будет в начале этого списка. "Монополия" известна практически каждому и, казалось бы, не нуждается в дальнейших представлениях. Один и тот же классический принцип игры, не меняющийся уже десятилетиями, простота обучения, интересность и большие возможности для командной игры - казалось бы, что еще нужно, чтобы сделать хороший подарок, отлично провести вечер или поиграть в офисе?Настольная игра Hasbro Games',
                price: {
                    value: '2079'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86Fag0qDFrcRS2xLI8PPWYhy-RW__e1QcvpqqZN9XFo_URMsMu4Js9o05heeDbtfvMIHlcqVjXC9rtZZTsrX0NNIWGqehsuhlrqmvFYxPu5ZYJNM6K8Pz3PrMRQjREGifO5PyrqIuPUXKH8E-XZ5lfU65maZaogw1dlgwfiFx5fsFmd-NjIwwNZbprQ95zQrrgYvcMWs7TOHRER4Jneijn2kzKkuhIwebt7cJzooQ8X-67kH1X8wK88REQynYe-n62GYhUbk2n85pAWQGUfLIvagflCLJjT5Jim73jy3nsRdko8m6-QctqPHgOcT05Ym9Zc-vZ9CiATqpm_3wiG9D4T1tHbsVwvCcCU1aiB_6DLWKCd3E37VrLgcWlbaMiXgce6v5Cm6IFELc-ZQaKYYlikCYUMnrsA9XLhusY73qYzHwQbaJV88fE0pVFM_40KO6V4V2LWRRAVarte32O9lIGSKVkW3CcAmV4Jy7m86h9UKNWdaOvbWDB2a3eNQX33YhYurIB-tYDIseLXWYyMbr_5LQGH_qONzNQ-bHqB6wvpS_nUTJQvo2kI92PVkbIjJdPZuy6QLvcjIu3f7sKn6alipSfCY4NOofRZIz-IKNe6r8Io-T1nY55KhPLQx0yUQI8CC1TzxIhuYNhkgfB7Vc-hAmcrPnUEywcXGmhxdAdKnm2zEg3qMZ5UdNKasycNFh2li5SHOOqyOwstoRyd-KKLByVKFt6F2QT6Cj_NECf4MIh0tUqo233AO4E8wBzTx0UGwCAsed61_zoy_OzI787Y75Z4twG6TuVWeU2bVBpjh4-NXclQP2HDIPMfewDMLYTrOjzY_D7PXWFims7_MalGLvSNgmk1fA0X3tQjArH_9Q,?data=QVyKqSPyGQwNvdoowNEPjUO6Lp2FicSHwuLrkBRU9MB2L71swKP9DwSZnoHPlJhRZwJIdUCMFHTTfCPTkPqsEMctVPxh9q1ZfMFMtfft20z-iNgyliN2qiE1Umz4Clf0yKhlPh2jCRdCPfhkAbd9JC9lPVGdpruWr1LRm2Cf43BDtJpo5ilDRnLdBt0q1l9c0mMpKebxyBmuuDBtMqJB6O8py8xLUEEbgFRUOpIL4yZCFiCwEcRKDTGneLurvfOxk_ReSp_cvElrDPI4FEdltO7QRSHfu8cPo8snCn5lHxSFLEimR4wjSZKYE7XNtd-C5iFFH64KRsJok0jdrliTog,,&b64e=1&sign=1e2044988e5239151692987d359f1496&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iZfzb2Me2YRFMgHw9TR0SOVI9JEAKhItL9oisERXScK8b-r73zLyzKzz9kmhwDrNlJzeAKGWD3M3L6UCQhjil0vo9k5-s-7SEJLQvuRTakRdSGUTLsLfuGD5lPtXLIFXdqzf9UOaJwmIJRXXRPmbm2_JFXDNR8b1QkqPFuCtvY6deP3Su2Lv52JzJGqTOA2ZI97Mkm2pDe5OVPyZ3n7QalalbLZU4QMae2CbrexhZurfHaIzK71YWXatrRrKl0NoylJ5gxp6XjvCMShDaYJBR6R72U9wuqjNb-cZgXnTA2ylaBSjiDROk0ahUCoExFubX4wAPlYB_3eD1o7CsAYC_kH0bjosILR7t5Redn6L57N6BWxzDzYRDZUX30pEk0LWdncJGNTjMGFM91UwyRFYMZUuL3z29lQHcq6e22xOfxFPFV_3dUVz8h4Fp4coHUHizLT5gpqpLso14sUFGt_uZG0-W6eyYmF1gxG34s9CXEhcl5bS3ilHnxEfANsWOAFE4HqdQVDnhWYDzWBsWunqs5oqhffPkBo_ku1gMK1mp7ONMrTezqgsKcrdzYKicFNPE66E_hEmpljABaUqcggM29KDUK4i81iHHJUbSJWwka77_qXOhP4JrP-q8l_8_KO7IwPPaun599eoBgbS1dGUcRyyk_Kq4zC6y8J4AxfoIpAkBI_B07jZn0PmRMEvb_s_8DHLVeV44Nh8y6WYPHoWJ4_gBwuyAqHGCm5JioDR6xHU56AHZiM4EJVJqBn5ccOY0aXrF5POw2Zg-mtMMSN1Di75eIHXbk9T5iyFP2RIVFzkL35faj1xiKVKmSh6UYfFASpCek5276bVcIohTcPo-GcshFQEnVl5nUQLsBp9_H7NhEWCgvt6JgI,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cgu-yFWmVG6C0C3Dzlt1h4EUIlYLlPC9XmdQ6ZeuPlUZLTbk6341Z1K05xfAMqHDAQqVQSCMoxaGhOmvqOAJh09kiUKe1gZuz2CtXNZXj8ymdtOZ0vHJ9s,&b64e=1&sign=f28ad3fd272436411aa6fa1331059d5e&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: 225
                    },
                    rating: {
                        value: 2,
                        count: 34005,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: '"Интернет Решения"',
                            ogrn: '1027739244741',
                            address: '125252, г. Москва, Чапаевский пер., д. 14',
                            postalAddress: '125252, г. Москва, Чапаевский пер., д. 14',
                            type: 'OOO',
                            contactUrl: 'ozon.ru'
                        }
                    ],
                    id: 155,
                    name: 'OZON.ru',
                    domain: 'www.ozon.ru',
                    registered: '2000-12-22',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/155/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875946
                },
                onStock: true,
                phone: {
                    number: '+7 (495) 730 67 67',
                    sanitized: '+74957306767',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86Fag0qDFrcRS2xLI8PPWYhy-RW__e1QcvpqqZN9XFo_URMsMu4Js9o05heeDbtfvMIHlcqVjXC9rtZZTsrX0NNIWGqehsuhlrqmvFYxPu5ZYJNM6K8Pz3PrMRQjREGifO5PyrqIuPUXKH8E-XZ5lfU65maZaogw1dlgwfiFx5fsGtV-BQ2PvUYOCTg9980n06AueOcxG8M3xx9R4S72VSpm46awA6YAKgsOOI5AdiWsrg5kuIAFAMO7uRaYnVgDnv1Qx2zH_l6OCo_iUUYXWnSkNGb0k73lbU8EXIseDI2FoCgZo2wXxTwzu07-LIFyuoPpJJElmKxs2Xp3ll6bil1Kh4QWGL2iXeQbb-ddENYiEK4CjUaSESwyNk6aFwhOuRvJ5TOsMV2KJFmrIWB-VUfW0UiuKZ82yl4hvedsBPHXFjSZmTrkldyprI7O9VWqqwLKbdjAQl0_ggser1mXp0Js0YruxjUHupMAeXbKJ6_k_YE2raTP3EKpfnnobQDJHzVB9JvxizuVhu5szUcMd4jyh5kt_e2UnTeLglxij1HIU1JdbXVBhLLx4WspAt0WeabBuRJl5xOPSiWXUSTqXNCpNXZY68BJsb8b5-8NbO2AWzb7WgPKfcKGolDzxkESR_pfeJqeC3O1anqsCcXJn9dUTXR6rgVtkG_MNQ0nwo0d80OT6NqLCxCItJzqyqNs0M2kh7NgzkHJ176f-QIKQJ1KmuGOdB-XYq5HpsRyFt1Gp-5nl89_h28lEJ1Adg5IjnH-czJfCegegBCt45VNn-Xn3FAk2RplBo7p8G8726O1QKIpY-pQAjU3uEVOtf2-g1K4JpWkL_SdOpRtqoKaVo5uq3bKn70FwjTuYj8Mf3AAY,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8pcpV_Xz0-vW7sUztdsFMi1dEVuTQxRi6rqHhBaFDLXsbt4gAUdoUeWsncoGTYdzcs90pLwZjtgQB-LCbEbQgFZXP-dL8oJ-CI314o_DuD4YmLSnGMxFCK4VSSMjIIymM9Akk-z4GhlaP_KtEJ5Uf3ueUUBeBpJ8Nxdd1cwjSpDA,,&b64e=1&sign=60f22fb5de2863c661aed42c06a90c34&keyno=1'
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
                    brief: 'в Москву — 299 руб., возможен самовывоз',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            conditions: {
                                price: {
                                    value: '299'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            default: true
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
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                warranty: false,
                recommended: true,
                link: 'https://market.yandex.ru/offer/kwZOZjk_sgvZsrfZmPLVAQ?hid=10682647&model_id=1730875946&pp=1002&clid=2210590&distr_type=4&cpc=HI3uXCIAUCPc9F9Guc2RZIKoQTAQrCn_itO3ouZZymgYxqBZO159OTElTIP99UAsP9L-2x8ECXJLztbQq9V6V-zchCBKo-vIUlNqLbTelhg0RSHrAWyFiDhg9meBxcnq&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 1200,
                    height: 777,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/167558/market_dQEYVr9Ln7_v43Tfx35FjQ/orig'
                },
                photos: [
                    {
                        width: 1200,
                        height: 777,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/167558/market_dQEYVr9Ln7_v43Tfx35FjQ/orig'
                    },
                    {
                        width: 1200,
                        height: 803,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168879/market_mqUq5M_oG91ubV7fPbMN4Q/orig'
                    },
                    {
                        width: 1200,
                        height: 280,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/238524/market_kJUm_vUr0HOmgAEmarT_Tw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 123,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/167558/market_dQEYVr9Ln7_v43Tfx35FjQ/190x250'
                    },
                    {
                        width: 190,
                        height: 127,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168879/market_mqUq5M_oG91ubV7fPbMN4Q/190x250'
                    },
                    {
                        width: 190,
                        height: 44,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/238524/market_kJUm_vUr0HOmgAEmarT_Tw/190x250'
                    }
                ]
            },
            {
                __type: 'offer',
                id: 'yDpJekrrgZFxbbOlSRXXJSKxHEmIlSoNUkRDJkkCa484EQ6ONy76Jnr2c8ZbozgZeYoZvbQ7_Rc3d9y0rOTxq5BcEuOpEnA-yA33CmEkoAUeCvc_DPKOpW2SBJhyXBJPNrA7S4yPiHAIsR89wjBylLA2thWjVKXrlTJo50Hwzat9h2GtHmr1jddH3Ru4Cs7CqUD6qxn-vfnN_ZVU0ct8gg7gyvXuEzoN9M6yFcikeDou8GExIDHV09WKrXv8gsbL2z9AYjFBEx1j3JRvC7bL-U_dZ0qdjcyRIK3utM9jzyc',
                wareMd5: 'FkRfLSKANOs22usX0KqdpQ',
                name: 'Monopoly Настольная игра Классическая монополия',
                description: 'Настольная игра Monopoly "Классическая монополия" поможет собрать всех членов семьи. Ее суть заключается в том, чтобы приобретать собственность, вкладывать деньги в недвижимость и заключать сделки с партнерами. Игроки смогут почувствовать себя настоящими бизнесменами. Особенность этой версии игры состоит в возможности одновременного участия сразу восьми игроков. В процессе игры участники смогут развить логику, внимательность и стратегическое мышление.',
                price: {
                    value: '1789'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYo5ilgsL5ZPp4XmDLzTVOYMFndUAZ54OvqggGyCo8JWH6hDOmxfHYC59BPfo13hj4mkoJJBHIMpBe8_ESVnQzIse5hT2x72d9FHQflIvDRKmFFq4Z_dxDYgAhQDy8gxYD_O-mC7b2yfdztOQvte-h95Gk28FYJf1BlKGYrN8eIxAoG7F9kgWbPNOfusDnyEe3pW2fT8Ckg02nip7ZrNrrtt6iQgBRRLEkK_keAuZhTR8KCglonTVblSH8wdgpUmUmol9A2xpO7xaiY4CnXcqv6r6fAlr6hOAVd0_iZnXGWRm816rOo24IxvOVJDXnarJSUfdqpGxKFV5bpbLQBBX4ZCfC3XJ0Sx-R5ukiE0FSRjzEyth9YDePKJmSPUDllBAnYTXTsuaSbBOWxXRcVUXJR2dQjh0u8ULNmEBb97ZJz01VY3bJMDJb-dTc1-RFhj1bzGriEeW8gcfc-MFjpJeO2KYuNAKem2FJbqgUI6cJhKs80K-vXE76lfujy87-9agFup-QczkcHaWA3V1cti6bL5z5UOBT5ZRdus1hXblbOO0FwWcfqcao-yi7YbPEvKfHu0RO7Ko86zV6I0IQeqqCpYz0rXOLkT3EMj5R7_2_tX5XJCaPzU3T6v_EjaSrGe-O_DeyKKiXgk4PpyAOecMuXGZSlMv8VmteRSdCGTWCVS9EOCqEdMxpHhlHqM_S3cVZ851wubdmGb9oXWiFwMf_WNjeugsnjvxwXerYeDglklVTmC4ixWmXEl8skPAZG9tmGGDEZFQ8Z45BgjNcr8MYo3ttIWYcf8nE435J4oaM0KaRSOsGTOIsEQ5i1jl_Yee7XPgJ3K8wZv77zhTChloRd-w7izQYCHDg,,?data=QVyKqSPyGQwNvdoowNEPjUO6Lp2FicSHwuLrkBRU9MB2L71swKP9DwSZnoHPlJhRZwJIdUCMFHTaW1Tsuf2c1--tfpWUb7QxuOO2zUPNyi7jKGSmrwBOSxKqpGzDswPbVPMa3T91yq4iyAwQ4Ua8SKd13i51qnCFk4kIzbIlT9os12fH8bLvjcZSkJJuOFIGng1si2WDHucNOaNF6k3jMfQ6Z9vVLVM7i7opnebaupgSWNFMAvb4PqgA93R0oMi7AvcVDEbUIgxGJGmOWnUee3S0GnpbflTn7YbKNbD7fQNOmnoo101CaO4n0_odb_4zku_wUNHN7KoNRXwnMKL-aA,,&b64e=1&sign=d5ec3a4b8890ae1e1740d344466006a0&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iRww66jxVn7cFvvZ6uKnfVsfj2J0aZ36tCqsQlt7Do4sNaLCj0S-WDGf7MK4geeEsuOhPsk6ar6uShKUnAnfz_j6kl_RC4h2xqlRZDA9vVoBvyFvZ9dSn-4gGXETKuod3uNbgmV6T9UXB--1lPk4OVkiYIsp0EJb3MzlcpJXfkqOs6ei3lZE6OryRBGitYVs3s9Dqm5LwB0y_g9YLTevjGlN8dlRutyqgPGEeMhwc3VSU6lPvAI7uPylBA0of1EjPK0Vmgzbb77R0qr11SgZorqXlujBESzZMqHjAPG81f64RAFWYHaGUv_Qmn7ssUqMmybtbHjXbmgUXf9ZaM_P_siM7bKfbXJIeaQpaLWVfVOqhoRyIpcoavG3puvv-WSR0XjCuLUYDz1wAxIpPTminyHzZCWpVGBNqMInmFZloLNJVajmeP50Q1WsYVTVVVzSoNrYvta-Rck26vQVIjqyIVGytGZAb9NCO_1BelXBF0a4jvPXKz-6LwOZGVt0AtCy4cvJWBjaaAaV9PG7c0Ez1GKYyVXipVGha1ca3bttwD7PtTW1b-u4Gdz5zkTHnfI1ItzV1DMxFfSCzce8N1KL_tTXb3lTqnM5lGSNRRByCU71gTfJhQcx9MUh3sCypKW1hOLJefHZjNtLZOs7-MoLjw5O3BsFrJCvr3PDOzVaMqk0kS89oe7S5jsn_mP5toeH2dfxL4gO7Cs25UWluFADJNfcvMNRZZYeFPU5vDyI3iYIP7rTRQdsrpK3LI3kG2kxr7I-zdXLKY4YxZjII_SFYtRY0CGslarY7nXkXE9lwH9ENdA9xaoffm6oNUekOAQ7rOAWOlwbPf0-nZIyZyhF0rTp2NvkzVefSg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cgu-yFWmVG6C0C3Dzlt1h4EUIlYLlPC9XmdQ6ZeuPlUZLTbk6341Z0_O9mOmgSCI1mGEKIX8p5_dma65cGbmztTge0rB-Tr-h40-eqs7mZ7dKp5CeDFZrM,&b64e=1&sign=3380eda7b762baf4cb93ab5e32c39f0b&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: 225
                    },
                    rating: {
                        value: 2,
                        count: 34005,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: '"Интернет Решения"',
                            ogrn: '1027739244741',
                            address: '125252, г. Москва, Чапаевский пер., д. 14',
                            postalAddress: '125252, г. Москва, Чапаевский пер., д. 14',
                            type: 'OOO',
                            contactUrl: 'ozon.ru'
                        }
                    ],
                    id: 155,
                    name: 'OZON.ru',
                    domain: 'www.ozon.ru',
                    registered: '2000-12-22',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/155/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                onStock: true,
                phone: {
                    number: '+7 (495) 730 67 67',
                    sanitized: '+74957306767',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYo5ilgsL5ZPp4XmDLzTVOYMFndUAZ54OvqggGyCo8JWH6hDOmxfHYC59BPfo13hj4mkoJJBHIMpBe8_ESVnQzIse5hT2x72d9FHQflIvDRKmFFq4Z_dxDYgAhQDy8gxYD_O-mC7b2yfdztOQvte-h95Gk28FYJf1BlKGYrN8eIxAoG7F9kgWbPSTa_I2sG5h3-89So0fwb31-P1ZyvRqQmC5GJa2a_1J3q32rndDz57fGrzkCr2jMUceE7azANy8_iJCeiOlSYb_KshEAMZg5B1SyLqSiwiiYA7aWXik4uFpH8v7LFCerAtjno1nFySgd-DxFIxrfOmeKJVD-gawMdIdGmL9rmc9AzcM5aLvcvJ33ghjuRxfqkjuATiee7-YMawY8D1q8DFzL-tHi9VM6uGKVwX13k_f4dm8tF5Rf1ubVrAM-xMJEvghVl1DQZZvRNMk6w2wXARbaAvQNaSrh0fwaCAnmAz6kEto7ipa0ql4zWupO7vEst_trjfYpWocLktyBaWcih095iOVFsL2sRiaOLXRwLOchcHaZTGcOv763daoMsiAT6fgKoS3ynAstyGiTxdYVcU7NbLY3mkEJRaFoFRqBYaz2LgFm2Zw-XkC5YIxlZkelTIC5JUwMV48zCy8s0na33O7tW5HSOlXeahPzzZz6-RPif7Ps7KtI1BKxdJP5yWRvweU02ZobPMfUwaP5OQmTfMjGpMatOX0jhNBE3VoIqWHWbmgvsL72bmiGw9gO8U_ElV456lkYOx609-Fs_fQYUA0ge1gCTMyz3Lce3xQdE0a65NzzG6fUjpFC_6-1bRO0xwgd6GQlQv9CDZJnkEhdS6t3kTWOdjLhQkxL4NFr3GHw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8X4TXIwrH0XtmZlJpe6aCQtONFtJh6O7_EQhLH-qBeVoR0kIMnLwCPakmGX_d3ApfjIWFlx7pmXJ5scWwY0JSMfEyesMe-CuqAjJGpncJ7ENtioGhLVqUGwAJTpNcvWgfUCmnAOBGcdQ,,&b64e=1&sign=7dd90bb3a69e9d6accd9eaa12f9a8eda&keyno=1'
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
                    brief: 'в Москву — 299 руб., возможен самовывоз',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            conditions: {
                                price: {
                                    value: '299'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            default: true
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
                vendor: {
                    id: 0
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/FkRfLSKANOs22usX0KqdpQ?hid=10682647&pp=1002&clid=2210590&distr_type=4&cpc=OTwDT4NSygfoEy_4PU560yUMkdx-XG8p5-d9hafGf67T1vbWSKAe80AzxnFFS5SVG8-O7Hd_Pif-rGTmVjrB0GaoPvQeE8WWjwxN5pAi9rnI2lprB9Hyg-yenJzAo3Ql&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 1200,
                    height: 1076,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/228937/market_IRRxax3x5i2VxQ0yaevdQw/orig'
                },
                photos: [
                    {
                        width: 1200,
                        height: 1076,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/228937/market_IRRxax3x5i2VxQ0yaevdQw/orig'
                    },
                    {
                        width: 1200,
                        height: 746,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/247921/market_uPf3iwySgm4he09jt1qFxA/orig'
                    },
                    {
                        width: 1200,
                        height: 1200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/406938/market__GW4L3hQB_64BUN5vZlBIQ/orig'
                    },
                    {
                        width: 1200,
                        height: 1200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/237949/market_erVDYzuv39ZquM5ecUvXUQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 170,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/228937/market_IRRxax3x5i2VxQ0yaevdQw/190x250'
                    },
                    {
                        width: 190,
                        height: 118,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/247921/market_uPf3iwySgm4he09jt1qFxA/190x250'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/406938/market__GW4L3hQB_64BUN5vZlBIQ/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/237949/market_erVDYzuv39ZquM5ecUvXUQ/120x160'
                    }
                ]
            },
            {
                __type: 'offer',
                id: 'yDpJekrrgZENLdwbOz6qvZv-BO3wwMrSVX6bX8QGZ5SfRjecX85V2g',
                wareMd5: 'CR8Pdw-h7ASLor94uLZBkg',
                name: 'Настольная игра Hasbro Monopoly B2348 Всемирная монополия',
                description: 'Новая, весьма нестандартная версия всем известной игры «Монополия», позволяющая игрокам почувствовать себя истинным гражданином мира! В отличие от классической вариации экономической стратегии, всемирная монополия дает возможность путешествовать по всей планете, скупая не только дома, отели, предприятия и мировые достопримечательности, но и целые локации! В набор входят уникальные модельки архитектурных шедевров, символизирующих различные страны – детально проработанные Эйфелева башня, Колизей, статуя',
                price: {
                    value: '1499'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdT7nvpTqws0WZx-3rO3Uj_5gX7Nxe-cMKPi53DNn2PHiexo3FBsIM32fvB5pyNRcNOrjmQ3oiozu7VW4R42geHgGPco3TJVk1fC3W8JXvd0nvU7eqZD2l1QmLeIt1HmXQl6WuvfBoVqd7LYSUXJuWlBiz7PkXvpmvZA13uT6c6B-wlhe9e9E-4AV0QbWnH0UrS3HxX7iyJL79SKmrwT5BPnEM_DSUhDWgOAZhE87jTQPMdefBQRAV1ropdNdPvbKimxNwibpYwwFyUtbdvG_kbn3k9qC9susR_njfLAw6CYz0y656c2BiU6DuS_AIjM4YpxI2ZgyeWEWtHCTttW6Bjf9w6kzbJBynIBzJa_PiYeqFO7bOdLXWZ9lNCqjlx2bo09W03goe8Edz5DdaMZZ8tFrumxtG7bfXJDWshWbJAEHAQRoNW9YfP1ctUIx-kBp_zaELBq4TbLNPuJz6u8rrDlOn4y1U4r71xDUQ0ul9HRh3MR7LJWHatn_FBSlaYkYwhwehtxAqW4a_GsA8fc8_cpmiBQNwOMcufXNFbUfoR-rirOI7yKSCPBnrQNHD8TEYS_Bvxe0ub-z1jRo_ps1EU__vjET3YEFwhgxE_xXbAyEGprrX2y7VwwvhxXwhpI5RGbjyT2cKQ-wBKmcPuGKWfXB8QPsTvYllRyO7tFFOLZRDZAqDTo309XE4y1GFdV_yJ1G2G5Ut5ng82MTeixzQC4FlsFft9kaNw5z86_0AE05gt2wiruXpbyqmXN85PDGo91Wg0Q24MhAC_rwu1Xv3fGopxN2IBQNOwSah8T1zR5MKbj9mF5sDHAHabJ91SfFRJeE7rSwIlaUeM6HpG31LMgMEjP5NiS0ejMEmaUtsL7?data=QVyKqSPyGQwNvdoowNEPjRcd1X5IkopGx8TOY7lHsB2IzdZCVgSOE4bzcsD5Zw5ak1NTM6n-0GRTmpR_KJbuu8CKF8-pe41eMIl2mxZl9IL06QoyvyNkKPveKF9cEFOxkf-kH7aLxY-zVTaFYPiDIovPx07OoUz7S6tIilZJFAQp2beDMKm08nEuFGZk8Tc1-s7qDgdzxJdzMXSSLoDkBXZrGHXABnvTewDPujG9Bx2Gsu5iUd55pll-g5vkXY27ki0zLbm-rsCylcwzI75oFzTVY77qc3OJwsgPe3mO_tOkgrwZor9lTKrVXQmMaCf_dhYJ47fPRqP6GzacShWVaeJPW-8vQI1ukqfGmiYrslp9XDC8tkEbcPsGaZhnFf86TFUAwk82pEgQqFvoGjh23EUOvcNF7qQybd8sX5cmGamycgSK0NopUDk7W8iKT7BPn3ltbRym0uad8qCmbgOS3KcvHCT529N0kjp-8nX911c,&b64e=1&sign=020f3d14e56f78cd3302e678ab296bbe&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iTFh5K9sCqO8wXftqn3bxfncTG9E69bFOxXLqxRyowSH8tlx235CmDnvGvwewKiZoDxkzNnm90ZMGNHbdHXYJ3NOjMvTAo8G8LBJEzmaEUq5PDqR2VvTo7sZGTEb4vxWRCWPOTeOQING6Gn1uXLXxnDJ2GwcT64MJ0u8Q55YeQpqHfuzb6qEVyt8ye2Q1kWTIbSrDUPEmWLtUmyxk-pYnsgtUTnz5M-8Ub0QDKAq2N71DuarveLprCs03aEGGIHyOOK-uu1DSO9hRn9fjsc7k_TU6zfD5woXZxOB-KbIJSDqSP4c3lq4lVcqlRuKwwjerBTZ5uhUIH7dgZYhRZ1ecbMQuP9oeBwv7Jn0jz_xDM-5yaOmPF9ZQKU6Qb1KYtEka0XOFpimn2qMRNGKGVtZRlSxIRjnlwWALDDbCrt0RQDfR5g7Xc8-qQRMPvqpPhf5LfuyaUFkQLzJae_q7QSrvovY5pYmbo9WNCuUJJuxbgaLUQ-zumVUNqTosjMbqsJCDk3h5f_GmYzZkHDpjL2bFPwYIu4FtERef9GUXsdlT4L-o4WwitQZHTSw8X2izG5QOEGmvMWKg_mviSdWmDhx4_kxfJLzp42r1xOhMya3iWur4sbXvvLrqGH8TzF2H6INgjzB4EyRd8MRZnnM9uYvmowT6pefomKoaCNMmAMuPjuBKHh4hbBwFMWafQ6t_zFi6I17SW_Kqkm3rCDfsji1KfLJtEA-1MEQCpZGWE8krJA-sJwO8bItpmvphjuk8P32CHDmBpLXzzdrPtuCQwj7Sajp-Rv5MmpYLf8bqp2QiZS6By4_68le4nm7_niUHOoBkqq95_-xC3cGgSRNoxoFiHJVs-VkPlPKCen6VVr8VtdG?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2btM1dgQVldwGzawYIo0XF_E1DGH_r98NFfdzquLNUMuowMEYQBV8zD-vzTGIV1IOm70NQtIK-b4l0ktcvJLQFWUpFQQzeoMVpHPUJP5FFpP2TYzVsFLAYU,&b64e=1&sign=8b10bcd734464b3de50e33ba5314baff&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: 225
                    },
                    rating: {
                        value: 4,
                        count: 5417,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'ТОЙ.РУ',
                            ogrn: '1165074050745',
                            address: '142111, Московская область, г.Подольск, ул. Вишневая, д.11а, комн. 305',
                            postalAddress: 'г. Москва, ул. Профсоюзная, д. 132, корпус 8, офис 300',
                            type: 'OOO',
                            contactUrl: 'toy.ru'
                        }
                    ],
                    id: 56564,
                    name: 'TOY.RU',
                    domain: 'toy.ru',
                    registered: '2011-02-16',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Профсоюзная, дом 118, ТЦ "Тропа", 117437',
                    opinionUrl: 'https://market.yandex.ru/shop/56564/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875061
                },
                onStock: true,
                phone: {
                    number: '+7 (495) 215-22-44',
                    sanitized: '+74952152244',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdT7nvpTqws0WZx-3rO3Uj_5gX7Nxe-cMKPi53DNn2PHiexo3FBsIM32fvB5pyNRcNOrjmQ3oiozu7VW4R42geHgGPco3TJVk1fC3W8JXvd0nvU7eqZD2l1QmLeIt1HmXQl6WuvfBoVqd7LYSUXJuWlBiz7PkXvpmvZA13uT6c6B-wlhe9e9E-6VOaoIDo9cSxyFzXA0bOIISaE73b1ajG_SQsoPh4CNNIQExtghJyG7DAo5wJFDMsGXOMaEMDcXe5iTypW1PKwI1L2G9yeIN-onOuuKC_TJ1THBEktVi6ghNPDPpwsoWFXCfGBhgHmqbDBhSu9huNNx59HLB2USIuTIC_ptX-r4_KBJAF8GZlmaN-jlIgfJa5kN1IP0GRPnw3KOMDAUrhlTPRsS2ibMHZ258BSGJyOhKgzujyQauvCyqoZQJ2NekB5LS5wWDFyW1IulYisLe1l-5y6-3EU3Sbr_zvgd1qJ-MFOWk5U1t8CJGLslX6nZ3LStM-fvGBKc6XPMd9OKYstU8Bx0_jW2mU0xpPujnFJO6QwpnFOdHbCa6NMTxs3269yDqtGPdDVMw8vkpyN23Gmc9-Jy5-mPGneKC8PAth1I_VF_f3fS3aYYhMXdV8P4yVmkOr7FZIFRI0k5k-Q6XNdeta-pyGoKw3LqiD85h8V6lcgYRfzYonZhvarU_8puErTwCfkZuMP3D59fQmMPOt64ZQi-P0uBQAjiYZ16KSvQF3m2YLM1Vzs-83RZ7HB1Aryjkju4mNZOv_W-jjCSRH0sJtrdRgdvRB8Y10vbyuad2pKL4B7YMEeCfCclqt7SQMZI5s4pBvG2v5gc5Uk9F9Kb7VwKF2lArwFGoOOyhvarcfrHoFvpFgQj?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_7pzvXCgfU_kNFGm6iTszPsqJXazN1z30PtA_4gIrXGcdZ8DR7_YoEgYQ5QJENQyCykT3hL7puZ04eReeHOpRQTJeKQjThm8KLIerBWZgt1orYanpZ1Fi5x8wTi07kL9rhdVMAFoYR-Zvgfi1C7RlQE44A8RO9F6Nm9YFB4uZ7wvXG8uPBl1_H&b64e=1&sign=68ba874ceb42ba94ff31c0e4591bd3c5&keyno=1'
                },
                delivery: {
                    free: false,
                    deliveryIncluded: false,
                    carried: false,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
                    localDelivery: false,
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
                    brief: 'не производится (самовывоз)',
                    inStock: true,
                    global: false
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
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                warranty: true,
                recommended: true,
                link: 'https://market.yandex.ru/offer/CR8Pdw-h7ASLor94uLZBkg?hid=10682647&model_id=1730875061&pp=1002&clid=2210590&distr_type=4&cpc=MykviWIDNYA2OU96ojsf8XtvXHl22BzxDCLqtp8JJ-E1XC1ZOciSc_xxwC8nzGUoqCuHnKJiD9h4hQBiYJwVq0eAG5rSq2RSjmUjkavi3qvxo6PBS8620OuL364wzDsV&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/203934/market_LXZg6u35WJuBQGqp5aTXqg/orig'
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/203934/market_LXZg6u35WJuBQGqp5aTXqg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/203934/market_LXZg6u35WJuBQGqp5aTXqg/120x160'
                    }
                ]
            },
            {
                __type: 'offer',
                id: 'yDpJekrrgZFsGD9OHT4GfcIUD0dHmNB6CXjSepqrua0QyqmVBvJvFA',
                wareMd5: 'fAUfh8zuZS4YoenxFhkSJw',
                name: 'Настольная игра Hasbro Monopoly B7512 Настольная игра Монополия Россия (новая уникальная версия)',
                description: 'Данная версия игры от компании Hasbro, созданный специально для поклонников легендарной "Монополии" - новая, уникальная модификация Monopoly - Россия! Правила этой экономической стратегии остаются прежними:  нужно зарабатывать деньги, продавать и покупать недвижимость, зарабатывая тем самым очки. В комплект входит множество карточек с разнообразными заданиями, с которыми игра становится еще более интересной и увлекательной.',
                price: {
                    value: '1899'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYo5ilgsL5ZPOVWFQiGvGjVsGi0MI6ETgRLQf_Z8MzmGvkX4NcP6-rex8HFeLQGUnDgWEz_f_wXRPK01DpCk7tIpnmpP_BtTvHK_QsS8HMsUDzD3exjMZVBl9bu8e_s6aYzYqlspscozY_oGn8t9rAgvRfRMEk3d-keZ1GN8IbO1_jjyW3g3YC10uEG_xs-SvEM0QMUgvm11fxjksiVn3mE6ShOhM6JSJrr85yOcPCpmkwLT4QIEw1E1bWfmglXLaABJxrYuykyx4rETGowiKRIle9UBpIjW6MbrfuPLv5N9Blwcqp06_x4Yg8Ba7rnAT_osid8FME9D19ZwI3y-Scyi03pUnoe030u3n9kgAqKMpA1nHqyWnruUYwLuffCv_CRZ7PuanzdET8KDm_yEUk14Itn1h1_SbGdBKpsVdwE8hZOUW_RbaleW2_1qHplOJ3h-8I1_RBt-ayjU-LBDH20hBNYoWxcLE64xXFy_JrKKI9vEFerAYX6eYXiYff6LQySSip_Wu_40GRmOlHT7gZuSNREOBSmDQF5nQazyWt2GuCHIq-lwO0lZyiMAsnJ4Fr_MSe8BYFwmKDZvbUcQmqDC_sxEGjfqMX2JiO1d6NJqv-FJHpq5aYnBNyR2-Qs5at7EUISaUveiN9kF72q1t7PAQ4whEe0NKgNh4NdwO6xR7798_3IIqtcyLYXKeD9jLrByi_7Tnx5fOT73iBPYnjqt6UQ36WfhqjJh3w7WFnSyF1FqIIJv-uPFVtSjuVPW8vH8HqjCNDS-8TJ3bCqGGqCzfvN0H8BHSyrpbGpFgIFmMdO24Xz2exHnXq8pYSkfzXgoXN66wxm5Nysd5SxcaV4tE8inulV-egEoKiXMbSRBZPz1KMaDoGg,?data=QVyKqSPyGQwNvdoowNEPjRcd1X5IkopGx8TOY7lHsB2IzdZCVgSOE4bzcsD5Zw5ak1NTM6n-0GRTmpR_KJbuu2uIwNk3etQOozvZy7pN1aId2nTSCGn2rMm8GFebpp7wvmy7gcmZ62mddNtV7QjQl9Jo8CAIn-sliLdadkH7mLOF8pLL33-TGCqIZ9aYcZlpGbSPNosMFMaYE7dTQFo6dsiTPdCQRFIl9kD3A-LxR94POy9X46X0e5OC4L7BO2kkISyCO7DtNmYc9lBhGhp2On8lltpx_dfYyihVj-7mFBuSfTJYqV9sjpRvxSuUww2EKQSeFdNt_QGwxqsdg3fOsIaHYlZqL-IqdfQRc7wjo8Gcw9x4i9U2SNWe5pb4b2cmO6nPQ0s4SwbNqU9g-ykCrsi-zgleNHcKXcNumTtW1AcfvgulQZ5uSKr7XXNKQRnuBz3O_1iNHGS1hqUUQeq_DA2qGVtqih_1Huv13Gkoib7U4Ub-EoQqVEAbsh_Cz0D9QqqI1aOhpzYGcHogvcleqetu_YOpS7aMbE36Lql24l8ScpDCkG2Mnd9iISZ-bgbgSC52aFuu4XGy_y_SCPnSCAwZv-MTH40EtsdKfwNlqqlohJny0PSnOdcjWfg4goaEaqAOJGKtFdgBq6sz5AGbLg,,&b64e=1&sign=5e1773aed11493263e2c5e822b0a85cb&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iaaVb3bIajxF4ghPg7GRvFJJBpyDsgoRCnq51GS4UaK1b6jYs1Tnm6bDy3PNXGv88hbF5iSCUgkVNDHN-HkmIqtgDH5RZ2OHsB_xRkj5ZGRWCZMsvr6v3a95CVs4LuSmxukWhS5_opddLfwIj-zcKqv36wz5YJutf4RF3upOr05RHh5xh9BCh6Ge3ooLu_a1GJFHc8GXDdCLm_TFch95Xqf2Hi2avg8AXfRb875A7ZfCK8kNcr5cJFKCcU2dS5PgdHHHcji5xKDnhzBjVaHiUv565u_wfEvyRJyLOSQMxHVxhkKeMvJx3rWdpIXV4TL370HPhTZCVhqrOb7swcWz7684Gje-7t0KRRPCcBRtlgRjzkVoQf9MNXjEHd2DgiJpsaam3drV3z-wsqkQm_A5RJviVGlB81QqG4C6MzPRAps-NW_8jYKYqSaN0VpQ29HRP0t4O8LTxE0hCEwA1zLdAtCz2uWLtRmSnJJiFEOKLmTSAbwQGraltgCWWr7lRK5hMtlJLd5HZvOkI7_QxNyy7Ok-tDx8b-V1Zs1TVlU6Op9AKIHFa4WxF4_tzTJVb8XK4BJTewDzJe4yDKICgtwKvA8RiwGFZYzrtu711_FTCQ5JnwQev9B6u-grZZNo2lFIRlHAz_PB3z4hdG2WJWknoR09oJkVTmHRo9Y34nYZKKWdf1zWPXeEKdyOGJns0G4rF7U-f01yH0fothlvsoWcn0ksvVG5cuCpS4IsvsbnJvD2-T_dvrp1KpW-A-GyoUzLt0-lo6nsPrVj9VnV17FN8CaMTmNjrgT7ORi0SLqQ5UTwTFj_8QDEZsQyd60OrRpR4g3eYbDMj41I48TWh0tcrno9YPwDZuahvTTAVkaELI3pILxEQBTUWnM,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2btM1dgQVldwGzawYIo0XF_E1DGH_r98NFfdzquLNUMuowMEYQBV8zC3XjM-yEXoLZtr5WaMv9jcaCyw_JtNDFnUaa1Ewi1Sx71s1WoHOZfulL9frkTY2Ck,&b64e=1&sign=615a4c9ea9175a844f20d24b9d6c6e2c&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: 225
                    },
                    rating: {
                        value: 4,
                        count: 5417,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'ТОЙ.РУ',
                            ogrn: '1165074050745',
                            address: '142111, Московская область, г.Подольск, ул. Вишневая, д.11а, комн. 305',
                            postalAddress: 'г. Москва, ул. Профсоюзная, д. 132, корпус 8, офис 300',
                            type: 'OOO',
                            contactUrl: 'toy.ru'
                        }
                    ],
                    id: 56564,
                    name: 'TOY.RU',
                    domain: 'toy.ru',
                    registered: '2011-02-16',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Профсоюзная, дом 118, ТЦ "Тропа", 117437',
                    opinionUrl: 'https://market.yandex.ru/shop/56564/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875946
                },
                onStock: true,
                phone: {
                    number: '+7 (495) 215-22-44',
                    sanitized: '+74952152244',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYo5ilgsL5ZPOVWFQiGvGjVsGi0MI6ETgRLQf_Z8MzmGvkX4NcP6-rex8HFeLQGUnDgWEz_f_wXRPK01DpCk7tIpnmpP_BtTvHK_QsS8HMsUDzD3exjMZVBl9bu8e_s6aYzYqlspscozY_oGn8t9rAgvRfRMEk3d-keZ1GN8IbO1_jjyW3g3YC04s5Fuwt75HWp64h-cNeVFnLdIImgGMSihXXcec2_B9hydhneIFlMRGnhYuztIP9kKDaUZSJ61Cey6w6QqwA7SSXFUcbUA6lj7EaejyyqP34wZoJ3ajF2yzlgL6iHdEJXmyGKOVTKwACRkqN8wrX_4_aQBBeTmqIoF9_EKCJDIJNTHxYB-hEZE3YCm5eBjY_SDuGEkpJfbUFfUSZSulc2n-7CFUfHQTqwUoiw-_d03P38oew14FmIUAkY0xaHr7TbwYP22t6WsQbJIavZXmU-p14rgNFYW2gFF9aZf7VEUk_iUqbvyY7HOXGwwyn8GhQIdUcWrX14b6Z62oTRLH8ZYUD4M_eOoqYFiUp1gsolmTh0FSYPJ6oG_CfCf5oci6MFDWI1YhTUvZ0aq4RDcNNGUeEU0OKPPZ_69prueOy-R_iG3A3TZvaUDHDWtPjjBKNA18bLjzi9sIqUdh-unqIDUuxXjPMZLt6Uuw9c9_YAW5_3vi1ZeYNPuust-YpTQp4XKKQGC8ZMPPPknocKsbpA1ASCX6xEGWlH5ZGWJHd9rsIssggU3NTjSn0H5fzxhLyJqSzNnyZ2i6FeQtelDuRPj3STe9qHbXOjehtjCO-w8R3GARL6v1r_jC8kLCyhmhF6Sx06Y7vhj1dyDEGIX0W2ihlkQdWd3aCOxhRn_I2732ZQrYD1j9tC8r1paR9BRdRM,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-7a5Qp_RPtiAYqZQG7TD3qbqUfzGv4Izss5Be1mtkXMUL8uiFCtheXHBMdt7pPPxVrzU_Ghwic2pLdEQ67GQJ-3EGtFhu5qkC7VEp50T_mghxz6FdwrXg09X3sqSN_dnAMB8CZU72qSHq8DyjZHrPZPLePKHIWy19FFzUzKcki9__NvdcaurmX&b64e=1&sign=c39dd6856d7779ef3ac92812a198d81d&keyno=1'
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
                    brief: 'в Москву — 99 руб., возможен самовывоз',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            conditions: {
                                price: {
                                    value: '99'
                                },
                                daysFrom: 2,
                                daysTo: 2
                            },
                            default: true
                        },
                        {
                            conditions: {
                                price: {
                                    value: '299'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            default: false
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
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                warranty: true,
                recommended: true,
                link: 'https://market.yandex.ru/offer/fAUfh8zuZS4YoenxFhkSJw?hid=10682647&model_id=1730875946&pp=1002&clid=2210590&distr_type=4&cpc=3iimguWzNiIj0g95JrX0DyNKlp-P-ijOWP3cWqDHTh72VrfzqNPLmbS9fnl8g7Q426hrW-xRPrFijgM0I3wGpidlNBXsFhYtyMOjj6ZzqGYm_RXhBFaeGdpha6CjW9TG&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/217370/market_aw3B5gikBFW1-UYEg8y2Og/orig'
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/217370/market_aw3B5gikBFW1-UYEg8y2Og/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/217370/market_aw3B5gikBFW1-UYEg8y2Og/120x160'
                    }
                ]
            },
            {
                __type: 'offer',
                id: 'yDpJekrrgZEfrO997eV1R4rUZ2musTrAcQS_2mi3V3ADzGrRLzZuyA',
                wareMd5: 'WrgXoQ-kzE8YHlS9fHbi-Q',
                name: 'Настольная игра Hasbro Monopoly B5095 Настольная игра Монополия Империя (обновленная)',
                description: 'Обновленная версия настольной игры Монополия Империя от Hasbro теперь еще интереснее! Кидайте кубики и передвигайте фишки по полю. Покупайте бренды и богатейте с каждым ходом! Постройте самую большую рекламную вышку и Вас ждет успех! Выигрывает тот, чей пенал для фишек-брендов будет заполнен первым. Игра выполнена в роскошной чрено-золотой гамме. А бренды, которые Вы будете покупать самые настоящие! Все их фирменные логотипы знакомы нам и популярны во всем мире.',
                price: {
                    value: '2199'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZ4H9_aAm-SdJZka3mj1m0rz4u_ObCN_7QeQqZu1hSPPwsxp1_ZJ4LNM90zDdg9WLTaCqqdgY6eVJ7hSd7-dSKYbNo_Ias1hQnphBe5tlrycQXNEFGq-vDqR3Ye1zS4R_aRibvLUmmR_sIj9EqpE5v4LlnJuj5xv5hdGaPjXPqf91x8s1v1jth62Rk8e6g0EN0wiBbAVzBL6KCqJHVfixoEHRBVaQglUvkDjnekcfPuBY-EpSNXysCn3d8B3qXLke5xRPQdoGu8rlFL4zWXrOcXjYrI0_jZOrCFxD8hPt4SDdn40fW4wO71GRLdkSKIiNhJiB4K3VeSao6Cnyc84s7pOffx53QTEKD8GWBskON22ABuh19s3qGNDlrvQl4fdx6keO99R-MhKomPLKzvZI9GKqx6PWZ50hYWLnWxOQikIdcd_P1ckclhdXD83gSWzo55ILPJ0qrl_tk4iyQnce4AcjzYQ7EmCwNl9u3IYKFVgKDwo4sIbn-W_CD1dvUBMeQfSZCTTyaN7VhizxmuP9KwQvV7Yt6L0JwmD7FZD_20Gjt4mV1bpOdQaND2LZOGPRzwA85iy_mxD98g7KpvzoT36nWga7uZaRkoxISI81zdFrB3SOoepgtgd2l88wgglOFLDoflRuq7Mnga8o3Xopk-pxI4lnPbWoDhySMHFfXWo4AlYLsqOdc60Ic21uG_jFyA-mSy1Ic9p-S46mKq9E-QTbcM1CeKAEjuIJAXBCRKm9aGHpsahCvwDOVXE-ECaSvtPO1Yj129mEvh_lioDk61N-eoBIsEfeojBi6QI2Tfx8GLbuZOV7m6oro6xbwERKzDqvluHteQJFzW3RO_GdBxxpN6IhpxAVI9_k7-01EMS3Mh7uwV9yJo,?data=QVyKqSPyGQwNvdoowNEPjRcd1X5IkopGx8TOY7lHsB2IzdZCVgSOE4bzcsD5Zw5aRVUE3FYAfxuvVRDDe5Dkm-Tsgy5bCXEzgilNZ872xGJ9lv-lUJ62_2nXZs6CyI2oojsAIwpqH98JmTl4wa4DFRQwjQziLaK8suzuNdoUrJjCkpSU_A-tqRTWsEY8uDYmmeuJBdeEZ45ysXpBoDqAcRXt9Sw1MDsVUwLzV9iBxN0vvHjgBZhpLbq-eC5y5yPKpbe5O0OFsjlCcmOhPcMyEm7khyKdbAXJhlzLbEl0EMh06WzPLZubS9lBCGevLn7nwgYFXZ5kOY91d4DCiOP3WvRSI4_ML0VzjPemGeRxYeMUegkWc1UxUgf4mrLlARE3jRZh3bWSmUFwObw1cezG7SiyqxrCI5PFi7fJEKsHNXFWX3JwNghzcaIvNc-H0iSRNWvvI_dFWZctT5LeG7VMCKMGOvHg-NIKBKR0xAg_LjKjVecDPrJ7hWzdpDDiz0lR5-QH4GMgOxlNz8r6dr_ssFxjVpE2M23bF1tsyt0dgXsEm8LOzUXWeYxw6C2ekxyVjwBI3BdRJuURRsfb8RcpOP2XO_zHC2ltJPTB5AhjMnM,&b64e=1&sign=c5a0f5232ea74ffc4fe632e5fe20ac84&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iZQe0ObDqoYkrQR12z-eNo2fzbeUWAIp8FQlHvrPvXLUx8OAL4BpG9dNz2OJY5ghzel4n3slyipUVWG0OGy_4zCRPrrtVybX-xJSsJzieU28yxriC7-UeAyIjkfqvidlKSpbaw-p2S9kVKE4cHTZjOAFyDF5Z1m1bcqehccQ8U-yq51DMWdE9IuzZCoMdjzDYCw_nbrN-_9EqQvDrsPGoVE9epPM31-VM8NWUfoJnlJ7_Ysg_zUGgL1UqaePKuqLMcnLGG8h9Yfg87ezI4_rfRL3O2tQHk48PPfjbsG4uYgLwZEr7lQeenU5z1L4ejseoES0HEBjZ_6WMQk0mi-20TravJEkpkiu02HO0b0hm1oQH1iErpltrHmxaa-Y6ou4GQ3nnanJDrPh1ykBbmj4ady8xJNIvP01ipriyi7o9T6vDSQz0meKaq40GaOEJ7HoAZ9COjUAnHO7mKZt5zsgr4xEtPgU3QXskM16bs1noQgxM_I8SZDQlawRazIZ8K1F5yjqGyA9hzX-Leq5BN4KgdaBRBqSt0mTGLllDh4HYwKY8GhqyHDgQcLls4MPJQ4AxzH2Q4umZxVTJuX0vDrvnQsJRpC1c5f_PpZLSA6uAo9gmlBsRtvgAGwhiuBPfmEU_Ye4x42v5gqr-SV1iUl3bAIxTdMJIUVbskjhFoLF3sbF6UelDFsU3MKJIdZKttFOO6lVbBmonMk_rRu-iJEcoM9OVBKHneRkJ27802PqDhUntEpvFfji75wWQby3lV5WEenNDXxr5NQNm6qu3v9O8liMAvI_6zRSQamx6xxfb2kqTrqqCT3uMQuFuszJ7yNctRlK2mRa3GWLBo2l6Oy5zt6fAq7dRVZ6bdKiwe7t16xwxYkIrbsHVfs,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2btM1dgQVldwGzawYIo0XF_E1DGH_r98NFfdzquLNUMuowMEYQBV8zA-Wud3N6y6gv-1y6hgynoLumb2Z8S8tNtIf6p2u35PQCA4RQCdNVGJDOk7O799u7U,&b64e=1&sign=f4f2f1bb20590ea0c92d5a72fd7e9a3e&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: 225
                    },
                    rating: {
                        value: 4,
                        count: 5417,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'ТОЙ.РУ',
                            ogrn: '1165074050745',
                            address: '142111, Московская область, г.Подольск, ул. Вишневая, д.11а, комн. 305',
                            postalAddress: 'г. Москва, ул. Профсоюзная, д. 132, корпус 8, офис 300',
                            type: 'OOO',
                            contactUrl: 'toy.ru'
                        }
                    ],
                    id: 56564,
                    name: 'TOY.RU',
                    domain: 'toy.ru',
                    registered: '2011-02-16',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Профсоюзная, дом 118, ТЦ "Тропа", 117437',
                    opinionUrl: 'https://market.yandex.ru/shop/56564/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730875295
                },
                onStock: true,
                phone: {
                    number: '+7 (495) 215-22-44',
                    sanitized: '+74952152244',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZ4H9_aAm-SdJZka3mj1m0rz4u_ObCN_7QeQqZu1hSPPwsxp1_ZJ4LNM90zDdg9WLTaCqqdgY6eVJ7hSd7-dSKYbNo_Ias1hQnphBe5tlrycQXNEFGq-vDqR3Ye1zS4R_aRibvLUmmR_sIj9EqpE5v4LlnJuj5xv5hdGaPjXPqf91x8s1v1jth4TSE6KaPHT4SbV_8Wn0VknRsiJjR5fnqPWhRl1LQDFwPLhJkv3qzQ0T1g84ppPXu3JeMuH-i6HdaeXSRiFOMOMP0ZCukfdStqkOfkYRLS9TpFS2qhN1TEksfntVoX6Ztw13IavQeqOboLG0Dp7KlBEwa_FWM_6C4gAUBV5JKvBSQSQ-Lns2PMVfq6gKmtfndNE5rMP2MtTjgV8LwwHHR1bcpCo81_LO-SltPT5KTe24NpHI8Irlx-ke7pcI9NVQK8PyoPKg31paWGauJHHsjTkmMpGOz8Rje0UQn7T_qIVfRqXN7b4yfn3wWxk0GWhCjHLCn-eWuiAoqwk3vk_NDlIAPZzwdcKzYpui0CTLy0zv9U_6Ep9-APssOqoNK_MMzhsFoRBO6K6tncQsvIWHP8OqLSNOr4ocnUOyt6RDMiG0185F2ioeR8p1jzn88OXMj1WTDwRYa9k8b6zlbFi6yIszquZ0Ypa4wbZyIeTEAnad9-2g4p-XB7q3qwPjKldlaFYJxUcPaOOIvajJuvq5h6U5NE69mJerzMdnIci0RmIcJE7JXTQ2omND2MPWZdEUZ3MNSE7LnAy34b8CgPVe5XKWnl5LdAVbTa6APtM6UYO4bnx8bbo3CmTzYL-xLpNzFTTzhQKsxqg2aybNbZa7GEn96kmJ5mipAKpya8MMXglQ5W0uWcYt1P5ahw4zS_h7B8,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O95AsdNUlPD0oK0NvXawIHCpI-ClnKkLHQ18y1H6r5tG-LRMfm6qx_JPCrJhlrb2i-8bOXDuGPzw7Jq2KRDlNYBlFcyCe6KB-EaYX-GEAuGk7I181W3ZzJaLJTMZW6X427vkXpapER8Kz2W0-RECNRbyFUn9B4AzdNXEbRaDNm3xjTeLHozfE41&b64e=1&sign=7fa6e315c9b2562eecf179987dbaad9c&keyno=1'
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
                    brief: 'в Москву — 99 руб., возможен самовывоз',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            conditions: {
                                price: {
                                    value: '99'
                                },
                                daysFrom: 2,
                                daysTo: 2
                            },
                            default: true
                        },
                        {
                            conditions: {
                                price: {
                                    value: '299'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            default: false
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
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                warranty: true,
                recommended: true,
                link: 'https://market.yandex.ru/offer/WrgXoQ-kzE8YHlS9fHbi-Q?hid=10682647&model_id=1730875295&pp=1002&clid=2210590&distr_type=4&cpc=MLuS5mRTitYpQYRijxEk4RUbzv_35RlsEowihqjt-XlTayX5OWOtdDYPifD4_V6iFxwNNt3cGXjdoKe7MmdKDQKHx6mT1tgmDvWfbhbmj4BtvySWZdPlvD6UXk6Ifi-n&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/203037/market_-FCzVVkSF-YLoSh_QGaSwg/orig'
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/203037/market_-FCzVVkSF-YLoSh_QGaSwg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/203037/market_-FCzVVkSF-YLoSh_QGaSwg/120x160'
                    }
                ]
            },
            {
                __type: 'offer',
                id: 'yDpJekrrgZG69faTAzxA-BPrnew5rvcDhNFLzhBuGt7dBnX5cYmVQH4y6TYf8SeOS2zlFf4LoiS8CVG47txsXahxN2BkeTgH6cfmuM9ZldV-1nsi8DoMdPNaw8rIsFRTn_LeRhoQLEw-bySZkuoYfle2IrIROmNylTz3HSa9cLMklgLjSjFiyPeB3YBVqaFT2nCvaIEGQQjwB8CHYeKIXEdZASzDJYUaS4C6E14L-6TnEkfIBOkKalBAJBGMW_VG5s5LdVn1JwUDwljCogT7uApN_zS7P21cYdWIRXs5a7I',
                wareMd5: 'hlaSClQvzNxyUdCmUD_FJQ',
                name: 'Настольная игра "Моя первая монополия" Hasbro',
                description: 'Интересная, захватывающая игра, правилам которой несложно будет обучить даже самых маленьких деток! Отличный вариант для времяпровождения с пользой. В процессе игры в монополию дети становятся владельцами разных интересных позиций, что им, непременно, придется по вкусу! Подарите своим детям заряд положительных эмоций! В наборе есть фигурки животных, которые используются в качестве игровых фишек. Игра в монополию очень полезна:  она развивает экономическое мышление!',
                price: {
                    value: '1374'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdT7nvpTqws0WZx-3rO3Uj_5gX7Nxe-cMKPi53DNn2PHiexo3FBsIM32fvB5pyNRcNOrjmQ3oiozu7VW4R42geGa2lq5ZBbKDWK0ZD1np5bIwA4dR3HckqY-cB7FFwi4KstNHacs1RpU8eAYTJd2dR-HF-K3Dsg5BfGFItz3lED_c5-hjND176Cgs1nnN9wOHJoDp2Bff5BjBafFm6mLcmWzaBlgK5R8e-RSU1Q2ow7pAweWFWQJH08ysXQsIEjRvvr5DClFrIZZeKKxfuMcIJ6RddPo8CItQG93vT6kqGnK0XUhCctswIxJJHcKYl5_f_nOVZpIEz4T6aLNA1mc4RabHOGXeBtj8KQH9SGjiTYBle-onH8LLDHwF4yLh5P9w0F0N-1ggVAjOgb7kK_3aTYhSLf8ZI_-PryQyqR4C5I2FilFxxNFgpaR9M9OnDT3foSmPKBX06EEckSIbQhPqjNvyrrDNsTbuND2AjLD5H87gRWgh9KeX8MwutAJOCpzjfiVH03d4aPHz2dujwkOxYV2k_VTjH9kdSkXRmM9DeZJ07IAdMjM0jZWIBLNaEkvwuRiGGFWjX5a_yxppMuuYjWinmf557okpIEwvlZUuPR0eZWFDbXB9u_In5jnKfynBMP62zJGa-uyZ2gcfwZBK26NFX40gAlGj-xi338PO9MtAupJP_Oy5SysMNWjdH7s8ASra5IDpAiprjq6xeVU4-400nyTfYVyaTsV81QuAYTcRw9GaqyGWdgvBnhxjeTNH7ghx4l8EnLaFY29G8fN0vafk0Q6l_Tpy0L2vj5WdgHkvr0NCcvmZy0Vwax7wHzLuIcLgeoHAjoGxt56JmyNk3u0XfkGJ5GyUP-0ypN5VTU2?data=QVyKqSPyGQwwaFPWqjjgNl-TfeFnXmyUxowmb_qenor47tF3y6bBOLHoSuptdxC3NU6T-lmhggR_U4sG34UQ8KelhHqkGGTFAz582s7maYbC1wrfY7ekmLnV0LkoAH1RSPy-Cxcojgg_ilczhztlR6ud303WQm47QLlNxNhAB2nfso7sLiQUB3V4tmEMdTGuU4vHdc5cJNLVkQW19-BPZ_ydJ7enLnY2WTRnEchVl3d8syq-LTfLW811ZwmTcr5104CkRPDfe7TVOpVdvPU5SMrXnbZo1ztn&b64e=1&sign=d2a8dc47516ea6621380b87380a3b61e&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iTFh5K9sCqO8wXftqn3bxfncTG9E69bFOxXLqxRyowSH8tlx235CmDnvGvwewKiZoDxkzNnm90ZMGNHbdHXYJ3PFgoB9cU7_aGVHyd8M5ef41sByCg-0K2NQLwqUcu2ru0E6fKf2QffXqUeC-BAE5-E9eVHw_3ee8xl3m-9LK5_9fhgxLmJgWrHbKjr_O4vFgRmJZ7X5RsIKawzRtbOPgEf5ALw00KKefA6QNYg0pH3NGkwVHR5cFFQ6wgBjqzS2rLM1z4e3FlUkEaER9hcPPspgC3V_MyhjT3hDXdtUP6TqFfWsEjye8crNzWpVT2U_1-PIUXwQ5pbpkda1kuLIbN5vv_-YU7ItmHhswlXJEy4yR0w18I9qszoEbCcyvF8hxMMSu7t5QiNdDmk7cWjVC4LaBJmkhpztTLJw7ZxfgLQ-Y-UASTfqIf8df_nNWjqhKSGemWrcPkO483pfe0SuuUZk_EHRJnS3JgRUSD7sU4lr0Zd6BfyII7ezeWCal_nsyE5sRM5yVROyUynxhnYOcTbd8-0fbv_U3S3UBKD9i7jWWmKdA8hf4lZE3V1oLBUYINXfBCVxIAR2P6uqzagQvUyzNgyXwaqLmz2NB18nP_pPkPaqRk-p3k42OPWxhdpjMb0xB-5qFIdDXpl4GvH_5dAGJZhyNehQ-7cRKOPVa6QjxrN3Xf5BqSWfPIUazk928UQ8TWAMX4MfSdlc-Xp9KMq58K2fC8RaQndgH-PNXZkAlLmp6sHfAW9ej70cO2PyoeoDKW8tuNEOGN4TxL6_92xAmnlkPYVSMJErNSnUAr598oDXraH0cumFWZtDZlAtQK0s5vp6Uza5hR0IQVMgr-I_AFV1xXiCTABquV-gL7GU?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2fMi_J6qWa5_xD76JZJapdhwQnm1CG2GwCeuo8vOxMkCIKPAROLe6jsISCzstU_DXN6SP5fvohax_tNa3vf4bLgJsCd6CgXmImfSppwz8qACUbfPaFRaosw,&b64e=1&sign=cf92494fbf4d03b13d00b738e945412e&keyno=1',
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
                        count: 7929,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'Знаменский Кирилл Викторович',
                            ogrn: '311774631401123',
                            address: '105037, г.Москва, Заводской проезд, д.10, кв.25',
                            postalAddress: '115230, г.Москва, Электролитный проезд, д.3, с.2',
                            type: 'IP',
                            contactUrl: 'www.v3toys.ru'
                        }
                    ],
                    id: 432348,
                    name: 'V3Toys игрушки',
                    domain: 'msc.v3toys.ru',
                    registered: '2017-08-17',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Трубная, дом 32, корпус 4, Как добраться:  м. Цветной бульвар. Из метро повернуть направо и идти до светофора. Перейти по пешеходному переходу, далее идти по ул. Малый Сухаревский переулок. На углу дома 21, стр. 3 повернуть налево, пройти 150 метров и повернуть направо и Вы упретесь в д.32, с.4. Вход со двора, 127051',
                    opinionUrl: 'https://market.yandex.ru/shop/432348/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730874038
                },
                phone: {
                    number: '+7 495 278-08-75',
                    sanitized: '+74952780875',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdT7nvpTqws0WZx-3rO3Uj_5gX7Nxe-cMKPi53DNn2PHiexo3FBsIM32fvB5pyNRcNOrjmQ3oiozu7VW4R42geGa2lq5ZBbKDWK0ZD1np5bIwA4dR3HckqY-cB7FFwi4KstNHacs1RpU8eAYTJd2dR-HF-K3Dsg5BfGFItz3lED_c5-hjND176A3zlEtNoG8Pp0-RWakpKWhun4fcJmdCSY3Py2iemUI6uv_taJBcvS5ku1CZYZGUfULox6OcX-QceMer7llwW9osQXreE4XRkgujCGz_erlwafqS3_hQOF6NvPU6c5Bv-n67VE0JpCu9Cs41AOKNHkRWhA5XypTTme0BFX0GJpLs7SE9qDDnyM1B6la9pcwb2UYleS5HYOiGSsFy-dQin0sZUBYEt4zEuyoqrhl54xijpicOQO9zKh-O9mqTRDs_jUU_3gXcKvgkFkjThyyqp_MMACSkQWjRAXj7OQlT9_3-2c4jagi3ql8pc7YWCqzF8nV9jusBtZdh5puWmQ9c6ha7r85zvSqNXzirZ3_SB0XQhP0tjxnu4Ul2wtw7jzfQwGDJgtj4UZ7iTgI9YN6m118UEvk7Soye3X7cretVybRdm0tIxku6lUWZk54m8uYNvtmssNI-jM6Kf0m6Hf_eTuAMDqFB_qAhTmNluinE_YYSvlePPdN4Ax9fBNTNw5q0gIy29FiVXW7-J9E0vDQQB80v6WT9KvnO_vLg_KN5AlgN-eSbeBij5IPoNIO4x0aePiHd2FC0Wq9u8UpcOulfNfuNd6RjVvoWLsbqHI4ZliQEkZwC3rQZH13--6RiBZ-mM2cHbNRvOYMrvsK0Kd8bDV_XYugf-GefN8HjMRLlyCkc_CQCDLz1sVL?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_woUbmwT9JrpfbEC8pw4srQ6KKRzLKOreNWng5d0Xa1UdkSeUQxHR6Oz4KtTo8yl--lUsIoD0q4t9Nug0h-jnzbMj8rrW98HVN4rngesEpLBXpWyg2FstBgmmBYRQtZcTUdXB55FHFGjDtXYnUR6N0sniIiz9oheZEWZXJjpdzrNAxT-_fVTOK&b64e=1&sign=36a65c6ade99073d67c37255699c8dae&keyno=1'
                },
                delivery: {
                    price: {
                        value: '199'
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
                    brief: 'в Москву — 199 руб., возможен самовывоз',
                    inStock: false,
                    global: false,
                    options: [
                        {
                            conditions: {
                                price: {
                                    value: '199'
                                },
                                daysFrom: 2,
                                daysTo: 4
                            },
                            default: true
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
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/hlaSClQvzNxyUdCmUD_FJQ?hid=10682647&model_id=1730874038&pp=1002&clid=2210590&distr_type=4&cpc=MykviWIDNYCg1I09be7PfDAih7h2dyTWz4auVJkHlEVDD9HOxtXfL5PlAJlaPd4WdFlGXyqC6ixol5-gDxcmSfR0-JUi7KgMhcfPK0iSCl_TfMEvJlVeUL40bGRvv74x&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 640,
                    height: 480,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/207337/market_m7hwTDCLnNpkp0pF0hvePw/orig'
                },
                photos: [
                    {
                        width: 640,
                        height: 480,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/207337/market_m7hwTDCLnNpkp0pF0hvePw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 142,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/207337/market_m7hwTDCLnNpkp0pF0hvePw/190x250'
                    }
                ]
            },
            {
                __type: 'model',
                id: 1730873414,
                name: 'Настольная игра Hasbro games Monopoly Россия',
                kind: 'настольная игра',
                type: 'MODEL',
                isNew: false,
                description: 'настольная игра, тип игры:  стратегическая, экономическая, от 8 лет, количество игроков:  8.00, семейная, материал:  картон, пластик',
                photo: {
                    width: 534,
                    height: 405,
                    url: 'https://avatars.mds.yandex.net/get-mpic/175985/img_id4691619185427956188.jpeg/orig'
                },
                photos: [
                    {
                        width: 534,
                        height: 405,
                        url: 'https://avatars.mds.yandex.net/get-mpic/175985/img_id4691619185427956188.jpeg/orig'
                    }
                ],
                category: {
                    id: 10682647,
                    name: 'Настольные игры',
                    fullName: 'Настольные игры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'HYBRID',
                    viewType: 'GRID'
                },
                price: {
                    max: '2299',
                    min: '1600',
                    avg: '1850'
                },
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                rating: {
                    value: -1,
                    count: 0
                },
                link: 'https://market.yandex.ru/product/1730873414?hid=10682647&pp=1002&clid=2210590&distr_type=4',
                offerCount: 9,
                opinionCount: 0,
                reviewCount: 0,
                offer: {
                    id: 'yDpJekrrgZG2hPhKRpFOpwh7rheDYfM4dkbsAeIP8K1w8t9ZYlNnUYnD52voKtShxrJ3QBEOCfLtbwJcPvs7mMzyW_ylyijyKcOCXKTYNsAxYsTgaQEWKHE2OG1bG7IdCucIeG1-5VseAyK5ridkQDLcQmAKyevC6Su7wOw2DGoGxVzugjmSEc6L5QTmUxuO1i4d2d_Hi5p2tM4Z_XpBZRbRji9sOaDCef08w-iLN4PHdLSlnWhqH6IqhHu_jS1HSqtzZm_beWgh4WrMjw_fPEbsIYvH9jbBjQai4jPClzc',
                    wareMd5: '8asOfKukb0OUnn3q7M6p4A',
                    name: 'Настольная игра Монополия-Россия 01610',
                    description: 'Версия легендарной настольной игры Монополия, адаптированная по российский рынок. Вместо классического игрального поля с изображением улиц и домов, на карту нанесены близкие и родные каждому русскому человеку города России.',
                    price: {
                        value: '1600'
                    },
                    cpa: false,
                    model: {
                        id: 1730873414
                    },
                    onStock: true,
                    phone: {
                        number: '+7 495 177-22-33',
                        sanitized: '+74951772233'
                    },
                    delivery: {
                        price: {
                            value: '200'
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
                        brief: 'в Москву — 200 руб.',
                        inStock: true,
                        global: false,
                        options: [
                            {
                                conditions: {
                                    price: {
                                        value: '200'
                                    },
                                    daysFrom: 1,
                                    daysTo: 1
                                },
                                default: true,
                                brief: 'на&nbsp;заказ'
                            }
                        ]
                    },
                    vendor: {
                        id: 15157809,
                        name: 'Hasbro games',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                    },
                    warranty: false,
                    recommended: false,
                    paymentOptions: {
                        canPayByCard: false
                    },
                    photo: {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/373002/market_L-hp0UyaAq00DnUdfr7b2A/orig'
                    },
                    photos: [
                        {
                            width: 600,
                            height: 600,
                            url: 'https://avatars.mds.yandex.net/get-marketpic/373002/market_L-hp0UyaAq00DnUdfr7b2A/orig'
                        },
                        {
                            width: 600,
                            height: 600,
                            url: 'https://avatars.mds.yandex.net/get-marketpic/232366/market_KHCKYW2gkaEXevdWOiOPEA/orig'
                        },
                        {
                            width: 600,
                            height: 600,
                            url: 'https://avatars.mds.yandex.net/get-marketpic/168221/market_eOAEXFnP5RpYhqKL0iNlVA/orig'
                        }
                    ],
                    previewPhotos: [
                        {
                            width: 160,
                            height: 160,
                            url: 'https://avatars.mds.yandex.net/get-marketpic/373002/market_L-hp0UyaAq00DnUdfr7b2A/120x160'
                        },
                        {
                            width: 160,
                            height: 160,
                            url: 'https://avatars.mds.yandex.net/get-marketpic/232366/market_KHCKYW2gkaEXevdWOiOPEA/120x160'
                        },
                        {
                            width: 160,
                            height: 160,
                            url: 'https://avatars.mds.yandex.net/get-marketpic/168221/market_eOAEXFnP5RpYhqKL0iNlVA/120x160'
                        }
                    ]
                },
                filters: {
                    filtersList: [

                    ]
                }
            },
            {
                __type: 'offer',
                id: 'yDpJekrrgZGtIP5azjdDnAad4caWOeP_Vn1GfPm6bU3eW3K_HdMEMWDUrCukc3uXUaAvfQAoVOBmav1xZNdqVrG0QKXMFLQG6u3cJw9jzpAuN6y4j3pN5oic2tHonBJ4zw99xsy6jtt_PRngxNP_S2ysYseCCgyZGJXfQkksovuoimDrYc42AO4lmIE1dq3dvOHbqcWzxKsgtCdtos29PXNrSYpE-WB_gSVV6pI4LpgJOcKk8yHmdoE079WSA5Xy7ewcpfsZ9jAITEa2XxPMMUOWEXnULoAWXGzweYb0gew',
                wareMd5: 'iuPco8qAaYI_iLfJm2lq0A',
                name: 'Настольная игра Hasbro games Monopoly Классическая обновленная C1009',
                description: 'Monopoly Classic Бренд:  Hasbro Штрих код:  5010993414475 Возраст:  от 8 лет Для мальчиков и девочек Комплект',
                price: {
                    value: '1902'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYo5ilgsL5ZPOVWFQiGvGjVsGi0MI6ETgRLQf_Z8MzmGvkX4NcP6-rex8HFeLQGUnDgWEz_f_wXRPK01DpCk7tIpnmpP_BtTvHaPgqJiXkzv_U454NB63MF2oOh17fvt0LHbGL_Tq037doRSQBRPZ9aW3hpuUzDVN2mqpfGCtRHaQCFOKBXA2vQXnaQlyTs8DggguI8DBd8cUDn8poySGeQKQwxKKILtZFce7bHGDjjLX47ZIs5aoo2h_k5Jd9mcPbXlrZBJ_Fn0u14PR4mMkvbMC7wCPabfGBuWLihjbF6lf_CSC7qpWJyVFlFSEIbd0S4mIv8BRlW9K7DoVT86vswZJgx8fMkT4qDBQ41o-aLUlzgxKS1Sc-KQPMG9bIw3d7Sui-HL9nGpvPsPJFCrXHe3UyJSk8NWsWlM2_wNpgPChy5VRfsp6F8u0E6JceJtErg-Kp5UdHsu1pfFz6aYEeYByteAi1GXi7P9P89Iq2KUA9jp8XyJ76wKpj9gVd9-BM1X7LcPuxWjfJE8e6va1tNpUOwIF_cdokDhQXtIKPpgfASLbeu_I5JvFB9j8Icd2D1N9OPjGNlOHflwC-m_IKZpwP-hjNL1H1yCoT9xWbUn1tJsPubM1i5NG__w4zTZG-ofJ_SpGA_2W6NxJvHiipjZhbqkeISdNRJTmkBWwk44sTmGP0e5H1xDNjy0otzbnu0SmmNuofvDEzIUp0RY-bu9UNJvvVcXZr6k_NkxBwIIvs1vgH3QX5wDBDD-ezmMvRkYB90l97fTZsdPH3ge9EzMZuvnyt3byrG9xTo4TSJCGQWuTTiME-ZBfjLg9k_zuJ32cPKxLdAf-04s9X1oYoxujs-0s9EQ9s-Cz719Ruuk?data=QVyKqSPyGQwwaFPWqjjgNjnwQl5o6nBcWP6_wdMiM76pL8bA2jMgb0tDxSSQfYpVZP14_sv5K5kCGV5IzbKUbqeroGT9SGD9vty5opRIz94zeMP88lDKPzTIOUbiQUTdT7rvvOyN-lDGkKU2pqBo3WBQTlvfPLKkQnime4iosPVVp0Xzu6kr0g5vb_maEXNdO79Angws2UH0FLhbFwE0gUtnLpVQ5aVUsc3I6XXLARWJUaZFbAYQse2tzQbNvVUbbxXWKuBLE6k4HlhvxFAU_l1DsJJeX65jwHR-gOGUNXYQwYkHZ0dzyBia6czdYu0JQWAEyQXMglM1ixMXc1X_K0oi1RqIquCXFyo-l6c-i9wsWCyMmKQFRBj-unNme3XGMSC-lB4n2986rgtuqWbJqNxXqs1nwZcMgD1R7mtrmCGVzfrACBNfilV2nUTuNJaqxHYYDOuUPL7WH-nBNsbl--JSMcxrSyv8Yf-dFrr17TwotFg4pKXGBeZylWsE6055&b64e=1&sign=93a121ac08dd3b8dfcc489fb004c947a&keyno=1',
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
                        count: 11325,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'Кузьмина Марина Николаевна',
                            ogrn: '308333904500042',
                            address: 'город Александров улица Революции дом 40',
                            postalAddress: 'ввц 21 строение 2',
                            type: 'IP',
                            contactUrl: 'electroklad.ru'
                        }
                    ],
                    id: 1497,
                    name: 'ЭЛЕКТРОКЛАД',
                    domain: 'electroklad.ru',
                    registered: '2005-03-29',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, проспект Мира, дом 119, 129223',
                    opinionUrl: 'https://market.yandex.ru/shop/1497/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730814481
                },
                onStock: true,
                phone: {
                    number: '+7 495 191-89-09',
                    sanitized: '+74951918909',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYo5ilgsL5ZPOVWFQiGvGjVsGi0MI6ETgRLQf_Z8MzmGvkX4NcP6-rex8HFeLQGUnDgWEz_f_wXRPK01DpCk7tIpnmpP_BtTvHaPgqJiXkzv_U454NB63MF2oOh17fvt0LHbGL_Tq037doRSQBRPZ9aW3hpuUzDVN2mqpfGCtRHaQCFOKBXA2vRTJUF3Mc4IwdHlhq9qVDft_css28lBJ_VLqkMYZ5pKk0uPHF1QpXZ0N9Rt1xYJMlZWmeeGWoXeCIXsYIcJvezsrJinlbXAMTT10-1BJw87xeF3yoLhxMYLml98IH6qAMLN7leSML3Ee6c_8OM1wVbHG2tL4hZV0DY0pK4Zy6gzyO1XEmJFm9ESmUfKFq_MXaI4RPDuFFv_SoTM5sLvs3mCh20OhAyHw3csSbSUXGfLLj4zVfF5sC-IK8s_Kg5xbRGr5cwT1MJ5FAFP3wztz7xMpjehGASin_l5G_IT7Uah7zoESNOeArCQLojoRS20_bcvQK6qDuTENBz21p2wGTmeovZwVSE5BJT6xxyQsLaMH1E1C6YhyW905soG_C8T_W7hN4tfk_dADTLYoddgKTAPwh0tRVE6jlBFALIXjo8qYMZMCeKXvFL5D9zohqEVjXEhtOZnqUOmBARxZvC4MGaJ4fAK1SIk512_XBqJjXut3WanP5KR63cu3dWMZk-Fh3IsdKXyTq7DJEsmo5FpEIRKHmgoytQnbYXhT_WTSTQY8QJm4aI--5qDr6r5DxMRnDCyFtwgmMl8xwfILR160Xcunvp-dCqRtcE431KWf3FXlT_HUTpAAtM8AId-S_6LVoxH4BZ_d6GX9NpJ4_G96MSHpHh6rPb1h6HmaZjgg3pcOkRd_bgdwyYs?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-NjfhOLDLtV_LdkyjaAiY_ldaKWMFBA9Jg6JvJMcjFkuz3POCoOoFkiNxkKyoHJLi0KmE1SLAc6Yu2N9ASs62ln1-9bEQSNrc8PL_8I_ykO-QP8pdbcWA8tYOU628umS6iQYKJhUM6Zn5IjzVwZsxFvmZvDfTVrGb4VFZacvi1QQ,,&b64e=1&sign=fa4e51a9be07cfcd32818359782f83f1&keyno=1'
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
                    brief: 'в Москву — 250 руб.',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            conditions: {
                                price: {
                                    value: '250'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            default: true
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
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/iuPco8qAaYI_iLfJm2lq0A?hid=10682647&model_id=1730814481&pp=1002&clid=2210590&distr_type=4&cpc=3iimguWzNiJayoF2Ii91rQNNcQ3NWGoKg3vdPrmAkk5q2ofx4Id9QDiYgvJ0mTutUqkRnVE2FmKecOkzGUtj9fUp7J_nB0H2BKOdqfPRBCKDiwi02cEJA3K9m4_xoA5I&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/330747/market_9OsTuqpTyl_8cxcT3PNIHQ/orig'
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/330747/market_9OsTuqpTyl_8cxcT3PNIHQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/330747/market_9OsTuqpTyl_8cxcT3PNIHQ/120x160'
                    }
                ]
            },
            {
                __type: 'offer',
                id: 'yDpJekrrgZFfNU2VcaEEBFkeAw9uqcOCgAPP1W3AM5TRtfKbi1arUv1NU_Gvpcb6EiTYnxiIztQn6av9FS89T6D14PXiLDtTYB8-2JZbuGOjdhVDpfnzVND_-UXJBIwSQDXjFW_eR2v717DMzLYoj36T1DcwcQ_89Rx9_PeRNM_4kJ1DVmH81ABE9WXhwHMlmwXeKjxr4jGgBL491vmVNxWpxctd_tU0fcHa2UnT93Wkk8AepuE3VRUc6jl4ZsIbySOkojvZDIgbwMhywsNBGgDeOG2K5m150VHCA33jpzk',
                wareMd5: '-8Mkt6f9oLQEVApTarS7rQ',
                name: 'Настольная игра «Монополия Империя» обновленная Hasbro Gaming',
                description: 'Новая версия настольной игры «Монополия Империя» от компании Hasbro несомненно порадует всех любителей знаменитой экономической стратегии, а также людей, неравнодушных к самым актуальным топовым брендам, мечтающим построить свою собственную мощную империю! Настольная игра Monopoly Empire ― это обновленная версия всемирно известной игры, покорившей сердца миллионов.',
                price: {
                    value: '1979'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYlD7it9T5OfZxPv2afGus_w3IO0L7AdIvcW-oWa7n78_NQUYXgtaVI0SL0Wa2X5zaD9VhdizY3Kp5IQ9BKpB6SCg4zCMRAw1kwlvVhm3lW5hEBSWe5njN9t1ibqdbgPmMcJFrTRxqF37DtLQAB8dLkXKJD6j6gYnJhNLGWVTZlLXXJResTSepOMtnin0TPNLRPXH32p-vzFrALcNPJuKADzL4DunS0J93PvPI5_B6pDtVnoTbz-NGz_jaVNAGZZZlUMp5EKfIFzv3qMbdvyYaIXN6UWQzcr9E8YLBe7ri--Xaa95awUOxcBqZrDMwhFht0zIBYI-JBzoCHXpV-_MevKKsnfEUK2qTUyvSh_rlkF0hqyvzz0h8PmBcxxl5fZXBWv0Ld5X5PTpeqH3CItUGeA-kswRiKgtdsQtl3_rUrj3Evck2i4U9rEykDXOUgJ-GsRLQVN3_krgfAvq4lXv3lIJmQ16Diud-BhKVqO5qqu33mLZfngdpIvPluuwrM2QnBusEhfnFAxuRZ1mwLgElxVBZZsr1HBxCvhPdkZhDAlO8IwTlmqYIgRwvemymPgCsAIx2UlGuPhjScWSSFN2nnIqYr4M9nlVrX85ol5Mh3SQMEIaV3dnjvf8gssDT7MHfYST4dYpSH2mMXyB3sa-gXZDNAgg0ii7elj8fNtKlLWcx1dk80eRPlUli09aYCV4eFTp4KTHlyFoccXwGV-Pvcx9tX8hv0TaXdz9nAtVOlGsFtQ5tHEHKaktSzs_nqNitfsINxvrrEXgsB_Xv_J5hDehVFO6i3yho2gF-oyq-loTqTJdATk-dzDQU7QMoK7I3qGl13ROSssS21SUaAQEQPfD4E0LQ0skzD3Nxrw2A_3?data=QVyKqSPyGQwwaFPWqjjgNjRJCxuqRWIErIjtKT9XSwIhEfnzMLUa-ktnbqQaVgYpH2OoPNsh9MP-yO8_o7GUAD8AM97LaXQBOi6PobFhUk1Au6r50uFcT88P3OKQOnm6RiN_LfdO4nPVcvgdhwPXXl9jEXZIB9wNqF_8nEJ8dSSy9o2-fo17dwPyQwyFipuKbhYV4Nc43BgA5rxyWY82b79ZxEs79hJGdLA6fnjaPnHadGCV_QCN3Y7XkfsjGN7oxvWLqbkky5v03o_tCHp48hYJFN6cC3mUi9-E0tPusMc02YkkghIAkNXBdfmW3EVSmQen29goOeAKNXUnzt6dyhHS9T6bmysm-j6a2K_TZQZ4JMoHIKZYoY8ZVz7002DM3j6xcHTHBgOCujA2AFrEyQ,,&b64e=1&sign=4a0dff87582babcab4dd18d460b93695&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVLjC0hEVXKv2rDH88j7vejgIjX-9iXSEvnxKEmV6ec9kOWahHXkeuYpHErfv3r5_qxNbJvVDMC42l56OsDwT_THXyhGTYs9eUbd4QJVNF_ivpFzMTGZZKZEBYr2ZJ3ATfONKc_glDnn8Prt9u8BBHtqPiLuGLD3VYaI2851G9nNO7XlZspRZHzXq-WAr-WJ4h39wxFjAQKLvrUH5OzvJDPHy6GYuKdQnv6XfcqcWy2tBLk75IJwNepj5YN04oShlJBxD2rQ61hurEb14WJ3cbSbfv0VrWE2266p6bPdoE3EOxOpNLVSR_mbpAiidTLVsnF45reCMTnb3_V88wXi8jpxVUxwo5NzEiJRXUKmNBHfA_ET3jjsqIbiA3CmuqRXEDkB3l9J83afStT4u-UPy7HGMiOpbNyPsKuWVouXf9mbHq0WiOxQ9GhJoSEgu6qP0rGrp00O5EeR5L4Gyr_LamHHUabe1XSC0h37dGLd5J8SLO-FytiFgLtbrry0YJJoiuP_F5meOHYEiSRBnTN7lm6kY4ZZ-m-WxM4UfbLHgKdUownqg4Kn_cenZR-SY7rpOQ_eqh4VbCX6Ie0ZM0GH2bTGhTvYtYfma8WOrxCvI0dLxCGUV6-Rl8xL-xLdp8W9pLxPIuhvS4WuEgRkNO2mm7n0DnZG9Yx26eZvAm-PN2iPIYOVXZgMEGZA6T1v_4t-97up2ABgTLK2vRcXTSyoU-aj_JIz29MJz2CXFL-LecGz8Ejc8-qMRi6aUc_jdxTTgq2w5N9tieCylaFm5PSZ59bNrWcysz9ID17X_hkLBcnTH0PV2AdjYa0QRmrzdgXLmx91Ix8XcDrkXxgbQIYBdAU34T1HDCwKNf0aX0WoZvJ7t?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cHNrgkkLaLJSMR3JZU5i0jPKutUOkdjZF1hNPWJaKpDx3omqcvQeJXcrEk227VejF1OpG5Rmpu3HkGzdiFX1hZDHUfBFkxmKauiMtT5H-J7yEU5Fhunfsw,&b64e=1&sign=8b61ea7775bb166b5ac5d7b58fbdb989&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: 225
                    },
                    rating: {
                        value: 1,
                        count: 164,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'АШАН',
                            ogrn: '1027739329408',
                            address: '141014, Московская область, г. Мытищи, Осташковское шоссе, дом 1',
                            postalAddress: '107140, г. Москва, ул. Верхняя Красносельская, дом 3 «А»',
                            type: 'OOO',
                            contactUrl: 'www.auchan.ru'
                        }
                    ],
                    id: 175488,
                    name: 'Ашан',
                    domain: 'auchan.ru',
                    registered: '2013-09-24',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/175488/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                phone: {
                    number: '8 800 700-58-00',
                    sanitized: '88007005800',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYlD7it9T5OfZxPv2afGus_w3IO0L7AdIvcW-oWa7n78_NQUYXgtaVI0SL0Wa2X5zaD9VhdizY3Kp5IQ9BKpB6SCg4zCMRAw1kwlvVhm3lW5hEBSWe5njN9t1ibqdbgPmMcJFrTRxqF37DtLQAB8dLkXKJD6j6gYnJhNLGWVTZlLXXJResTSepMxDBJarksS4JAua1mj3xDVUQYAShtCuTRu8jJXewAI376ZEtOQ2Am3zBiSOd0j9_2bBz0Zuxymy5OLbCUTnOPmc0o64qNDMGzyUwOoLD02h2pdYe4daEVFcBub_cROB4saN7lI5FvaXfQfLjtZS3Pj56NA76ZCFVi4GpSvzhyrq6E2265yOeJEi3SnkKjKK2PgI84ThWsGXRPv7IBAUKvyOvWXt9jp2Pk1jBu9Zx3o4bPGsGWw_GnjaeFFSQMzzbXp5pOGdyU6zh8jsPbxzpIuX0ST7o9EtaN4yPpYhKQZCMAa28GbZgTSJFLzhmwV7TX7fXJdrUgCXUD301ywef4BXxgyv3jBhaPzNe03v6-cxp0KkLQYrUVMN2J46Y_hz-mfTzenMEyDEL2AGG8oOdDT_8tR9LNTzkbEMDq9Js0n48s718VO7xD48wXcwBwIapdQsFKmNqlwkik8EQuWednt6x-gmV6aVyZpHh3BX7tQXh-ikhAqvZbVYDbM_jn15F82ZfMQozpUh0BR-iDMAUM-kozVWrWjKeWSgWoQrsN7jL2dJgS2F_RUe5LWxk7XqxfmxGfm1dnwsPPMBAeagY3iWXJrAfnU1VZ36DmdPr_3dz88IM9cIvimwlz4ArW37ybA1wWhmIZTz_dqR3_m3dt-C0OZCqITI3mgynOHZElwESYZqgkOp26x?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O99aBTHunH-BNdhvf6NHIOKHzXa2IFjt7IQ4r1O16Q7o-y5vd31sMBQzJsl55weLnIDv2vY7nRAGzNlCELWEK-cXsFEDkN-48raZ88Ps0HbGLLfFBLWJzRI8EkWXCm0KD5CQbnmgICBrqI8oLvYwGol&b64e=1&sign=19388955d5168586459278da82ccc37f&keyno=1'
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
                    brief: 'в Москву — 249 руб., возможен самовывоз',
                    inStock: false,
                    global: false,
                    options: [
                        {
                            conditions: {
                                price: {
                                    value: '249'
                                },
                                daysFrom: 3,
                                daysTo: 3
                            },
                            default: true
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
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                warranty: false,
                recommended: true,
                link: 'https://market.yandex.ru/offer/-8Mkt6f9oLQEVApTarS7rQ?hid=10682647&pp=1002&clid=2210590&distr_type=4&cpc=c4AdY3HjVDZI8uO4MsF27uavO74giamWux56dZ2it_r1caRiHY3ngTt9CNWNagZtB2xzW9TQh2OVdvcaJ3fHjg_mov7QCAWwDsU9ZEqj89ZicWIGAzZ2YrI-x8PDjUGM&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 315,
                    height: 315,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/403468/market_joJ3JoAPaRSx1S4tgYZylg/orig'
                },
                photos: [
                    {
                        width: 315,
                        height: 315,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/403468/market_joJ3JoAPaRSx1S4tgYZylg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/403468/market_joJ3JoAPaRSx1S4tgYZylg/120x160'
                    }
                ]
            },
            {
                __type: 'offer',
                id: 'yDpJekrrgZEE1Tj3a5tRtbbcHRX7_YpAjS6kBG9CjmKPW6U5_sePriDVvQmJKlxqN3gIDTkNE7Bi8V4PUTep7ojGtTdgD-_l8rs6eVcFtDwNQQwSNAEhaQRqmGQuWE_M_YrvDrA0NMUgb47BBkmomZU-IS2J9rYxgAHsbm_4ylTTw1jxeF3LzgOj0Sw8Z3N5zqmTsfA8KHATdXlE1UwHpPdXFq06Kd136GZ-kI6rNS-h7o29_GNqSjlOlen5NIS68LYKayvcNqr3xDTdi1oLE4EzxoRFXbOjs6CtQzs65ww',
                wareMd5: 'Hx_EEe-Aj3N_X6ogetj5rg',
                name: 'Настольная игра Monopoly классическая',
                description: 'Настольная игра Monopoly Монополия классическая – одна из разновидностей этого увлекательного занятия. Модель создана из картона с деталями из полимера, бумаги и металла. В наборе есть дополнительные аксессуары для реалистичной игры. Подходит для активных и спокойных детей, мальчиков и девочек, подходит для малышей и всей семьи. Способствует расширению кругозора ребенка, улучшению памяти, развитию образного и логического мышления. В этой модели классические правила.',
                price: {
                    value: '1609',
                    discount: '30',
                    base: '2299'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mS2X5VJ_UneMKIMNcUu4q5D_gDN3bXUmuY46EhJnGjwdhqyjLN8CLVkhCI1IpGvdRHYe-yPqJGIk_FOqXmlcbUbE76431l-UQsBucCNAGodLQDJKLOg0Gy-L8PueYT8ORE8k72P9HZsTTJkBleZod8bt-jBCpCLpW49MotAf-usNLcKaQq7maS9HHj3pvRrlNYO7qu9w3A9NDONRrmH_neLOpkE0kpo-pYwvnhN_VBPE5hBSRNydUCvCIyLxMJdrq614qT6e0L8XSC5tsBoztsBzkcQOGOxoSISU3xL9h7rF-Jct8dj-bKv_Inq4d3QXCr8t--dEvAKmttuMRwPJ5vq423Nj-wC-tB0djBgECQIL1ALS_8AnVvgRZo3VwsfNKmJsXMH1wcztpIiHYbIgpWnTUA_XLl1bEQE4YEsaTFVStz_a59LaS_c-nzu5TF-BOzpbuCzTw73_7G8UHySd10uWbyWaM_Eq8Ndck5dLbmw2tUgN7PdW1OJjqDCOfs6U2mdCIQNrMnioi5MmuSM5hWO_483Bmu8R82NI6_z1q6MkqZ3qkbTREgsQTjeFr4J4l-gHcCYd-weE8Vl5woYPh9KBP5B7Z0XPHAUggSvRTt42onFrRmG2nix-Aj3eKialpMuF5rBJ8b5EiI7mOG8lhlJ9uDJWNOMp7ln4OHTk383L1jV2hVQW3QrGV-zyXPJkRQHXK0b8Dx4aPZgyEeX1iRnL5M3y5mIPfcrOe0_9Xfd7FIKp3LjUSfiVNmtBu02rKy6_IdsR-6EKQs5qer_I2TUwcGJ40NNWwWrl0ABvtlPhTXsqbGy5jqmTD7g9vhYKXAc6aRPz68EtjvpfFMfTLRlu5YJw6EN6CtKi-Zu-g909?data=QVyKqSPyGQwwaFPWqjjgNrSTXEpR9O8Xwc9ZJ5kIj4KSmIfoqRylMqn7_T9axhLUOxi8ITmzhby67YLsSh569hJkHxw-ObdMo5E-XC4quIHjULTdoRfFQbGkscqCuz7A9ftv8T4VN6qydoquTJcOPBSKWoRLquOXHsKzjGpxBeclXUekjAPmrE5NqGvE74P3S2vGhMMN8rZQtW1qKT6Nq2BKH9K0JY-LZkS6n8dZmXumTiHWFdjdSqjt7cT2Fwe445RN5EuaSRkpX3YWUQYiDe69Ofx7qIXA21LPExpn3Ww5KVWOElxuMEc8sY20Gq8Z4oO9-n1v5O5_clHWUnoN0Kcw6FiVfHSB&b64e=1&sign=fd3bffdb85ba183687dc211dc1183296&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRufkc5G38e6HdTFyaNhwfGNJ3-osY0BleCy83YieVZNreH-PhaI9u-Tac9YcLDOJoAJMyi7oy7J6O2aTBRUkrVtGfIlqCc7Dna4gaHb-5RH8U3gS8WoYT_1AhpfgKkHVJLQ0FTXxc_eVqsETHR6vgr9AMHNhVVXDXlJiJ-k3kP7sbhBcP0mV7sj_CtIWdc4uwwAq2AJKXtvepPBee2bAzvpvNzab2qHN3yikZART134XOwJ_wyuPwUZClllML8HDLTDx8pu3VRxDFEENJvXRIHH7etfNF-m5DwFSphk3j3zracl-gjWL44azPb1YxYpf_KBC_f9xpEsBdumKKlR4L3XmkSfccZrlgY4a_bVC1wccrdbD0AM-YPe3UutN7k5HDs8TBh6GeDjM9U0LTHKAwqBpIWYIKP8-HuWNOtVHGwn81TZOfiGRXGStnOrhElD3SaRjoToCDWFep0rneo7CifIxe0VyDYBJvEM0Gt39BBRWgiGJsikk7baftYj9WiRgStDYxjjy7F00py6_EVyn8SPs_lVqOWoq9NnupnK2SXRjy5sp-t2ilCSt0DsmgBP1gVDlT-yUbu6CwMva8lC5HFGxpLl1ENPeQCvv1DRqpPP3pcWYFAMuuygQW1iENOfwgpv0PBLDlPcraAzfuwWRWJehFSeYAaNYDi_twr5I5z1Dv6gXVaEGApVi1uYSlJmr25AMc0cflBLXSsQro30rnd2at_NTnf2ZjSvK6sW15kONr5mda6SjGZsf76i40A1Ygdy0XOsMQq6ofLcQAKg3KG4J3BYsnv2HtbWN9iKXaS8ZlYMFeyfaSXyVY8IRMCslXE_suCS8koSGuj8ohIh_GXBEOrQnX8sLVYnwFz7Eia4xA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bn2u2rvQHKoAenKwamDlyUb7RcYihuB-8rndcOckvL4RBnZxuEHe4PYG5KUYBRhpa1C6y1Pa9BlYzLKHqBRvvlvoQzbtmKTZfkouObdm-d07SANkRLeADI,&b64e=1&sign=8bcbb20a0fb3853dddbbfdcee4601d9e&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: 225
                    },
                    rating: {
                        value: 4,
                        count: 9147,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'Волга',
                            ogrn: '5177746098844',
                            address: 'г. Москва,проспект Севастопольский, д. 56/40, строение 1, помещение I',
                            postalAddress: 'г. Москва, ул. Бакунинская д. 73 стр. 2',
                            type: 'OOO',
                            contactUrl: 'dochkisinochki.ru'
                        }
                    ],
                    id: 179876,
                    name: 'Дочки и Сыночки',
                    domain: 'dochkisinochki.ru',
                    registered: '2013-09-17',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Ангелов Переулок, дом 1, корпус 1, ТЦ "КУБ" 2-3 этаж, 125368',
                    opinionUrl: 'https://market.yandex.ru/shop/179876/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                onStock: true,
                delivery: {
                    price: {
                        value: '250'
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
                    brief: 'в Москву — 250 руб., возможен самовывоз',
                    inStock: true,
                    global: false,
                    options: [
                        {
                            conditions: {
                                price: {
                                    value: '250'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            orderBefore: '19',
                            default: true
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
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                warranty: false,
                recommended: true,
                link: 'https://market.yandex.ru/offer/Hx_EEe-Aj3N_X6ogetj5rg?hid=10682647&pp=1002&clid=2210590&distr_type=4&cpc=xnhHMzM33HIeCji3Cn7GAyruTbjv2w4KsdIcdRMcQKs3aj7KA3EPT7FyzZC1TmoF3EfGK6soikqW95zBkNMxSdxluc3oIQZxVTi6W_3aGzptLWFw_7TSXmsgnHcplhKm&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 1000,
                    height: 1072,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/222975/market_Eiywqk_J9b9p3Id63S63Wg/orig'
                },
                photos: [
                    {
                        width: 1000,
                        height: 1072,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/222975/market_Eiywqk_J9b9p3Id63S63Wg/orig'
                    },
                    {
                        width: 1000,
                        height: 1072,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/250283/market_yXq0GnYpKzY-B6NnttpKZA/orig'
                    },
                    {
                        width: 1000,
                        height: 1072,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/240755/market_tSQL7lXee3dZxsDqy7tHjg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 186,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/222975/market_Eiywqk_J9b9p3Id63S63Wg/200x200'
                    },
                    {
                        width: 186,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/250283/market_yXq0GnYpKzY-B6NnttpKZA/200x200'
                    },
                    {
                        width: 186,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/240755/market_tSQL7lXee3dZxsDqy7tHjg/200x200'
                    }
                ]
            },
            {
                __type: 'offer',
                id: 'yDpJekrrgZHH7fudQzYJR_yBNud0_mGCVp_5shvO_Yvs8OVHEGGGGQ',
                wareMd5: 'JTBZS4z5ScsQO981S1lfOA',
                name: 'Настольная игра HASBRO C1009 Классическая Монополия. Обновленная',
                description: '',
                price: {
                    value: '2010'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86yK_6dbq1Uik5QP2R-O0jGX-usmjEVfEtc3m4V2MdirPyCA6opEv3ONEb76sImV9SyiDPdnDL6tp7Fv3B29wVo7IPMB6BzlKs7nxEjHo9P5dftnPrcFLKqr7HFjf34cOlmfSu919HTMjNUxXQ6mFxaNjNgVWbUnsqLC9XWqv1823QbbgpO-ktbxXoE6Ea79_6S4yttqbCpb_zIBJAAIe9tLR46_deSMRZKn9A-dQTsa-eUqtS5aCngN0_ZLHcb2FxZGbP4NGuAk122PwtXk2un-Yl_-rfo6wsMvGD7Vl_ox8pDJwn6qB9Y0RZgkz3Wx_v-UG_Ez43fXNgqZ25b69DklVCpSFHo8IhjGscB9Wrlcdz94-KXavfE5vxEW8oUxAD6I5eWXSx9YjGgyy1_tp8rpYhyUnqCzn6aX7Ez0Uo6qd9kNjEN7GoquHdj9EFXzNlOZoA4jnO1g_sUtJtv0WNOXIV7CP8CE8F-aNY72EPTkXbgXuzAu2sW2-nPS6QOfuoEVCmryuFH0zFlXIq3NtBdDct1Orjnmz15D_zV4u-U2OYC8j0KPPVVqrs7HKkKFj9l657zrLKoNmOJZJWBwcqO3kBDOMcvgb8iuu6liU9zsoeAXYq8yHuDCKbi1AunaiXU4BLWsWsF-nrg6dLgQ6X5gmruzLkum4S6Gg9diCmKX9nx3BmbCqyrmFQktDWzGr-tqdwbNsga1-zMXQbCfIk4x64PIOJ0TUQZrmmNiZ3j_9TRnpbiRHmDAXyGsqVvmU_Gdha7mj4S229E99-Ekasg_EDDGMbFbrbCbRzwp16ni8_nWUxMPfYvlTB1mMve6DwrOCJwd_YcplW6R8j4E7rYdylXGZI2nf-?data=QVyKqSPyGQwNvdoowNEPjeHCAo-a2AEEMgvw6Gv7rodwqTifDSzaDcFSJWWZewOk9OWrt7uVlCZZ_4i1TW6Jhv7iu4X03FjGPjeookZBQ7CFuQ1Hc2j6nznh6s5M2j-SldiEl799qwHWj797aZgLbdAx2oqWkTWT0oED8ynkPx8b7jkS4622NnYqdz4yuwJvKFCAs2MK1zAfMtlbJN5R7wHGgI3bpWQCZk_DdeH4NU6skFu1dXHFpgGDB1ZAOp98L6vA-5WzQB3z6Au7tUuuGxEzc1doz_CElOL2GllfD6z06mGvsbyLPPwDuqWc_OgRU86vrjl_1XJHhraGjCA2Xbs-9gcfknVQ0SCKA9g8mZGI8yYR0mtFEbJnXINm8CsIvsj8cVYjsYXQp08ZhFbUp1mDyjjZIfCLm41SHagMwZT-DyWz-0AK2VI-myEJlmXP0EkGhTzVxLTK6DgkuC4RI7domX4YqJOArqJ-zu1kfy1GeA2ErEjs1ONhdichS5pdGLqW2AcvLD_2_6v_B7VmHNRm79tkbJcCVd8DqytCh5S0EKya9GUleKM8byW0XuG5Mm4TdTUBISpLzbgb0AOq0J5p3_u3SJwMjxmZkXkWsuTcxMoZRE5u-pjW0-AVuZJROmzXeJyOHlS37uA9f16-dGZaBBvTNWx2oX7rCG4UA91zwh3F7K7GZspxfV9UFRv_uUZMnYToVZB8VLiYbwQWen0DbOUeH84K730OfUnKZ_I,&b64e=1&sign=0362759233f0e07c2aca766252553d7e&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iZfzb2Me2YRFAtDx7WnxbhGdLQHnIkHSDOSgUPm818e75E744kYU3wQx-3Q1xHJ_rd31AAgsJGmltcU0BPqDCn436Ew-SFBkYgHm2Pch1I6ca1Jz8aPx8ZvEhR3UTZIt9v78ignGUNxY1K8Tzxrj9U0EEVSOsmXYUi302RD-QsFsQ2tsSZ7Uf8tO1WO1fcHtppkuNkU-R4WHfIfXErxIlU17itOhl_UZFxmKCvf0Uthv0QMt6CPbXoJroN-9baMDcujupVehjGCepWPzhh1Jnw2Pan1Ko_6l-RL9BuuG44ACWsBTkZtkDMFDyo_fqWqMWke7g_pNQVy08epaf8dEGay-k97iLcLuvPxOo78PGxJGulJ0jTmpu-3dVOpf76LgOaW2GOMQZBIPfTtEHKiGlUYMShRDDjZtsg7jBQXW_CSFDNAnqL6B7h8W46pR5BeNk-clshgmjhlEP_a8mBZm9ZNeeBPLwLtE_YiGHQ4xSx3tRzWedKvvh1jCuiYOGPF2L6MAlXeP2p5R3uO9Y-5-oCx2ooksyT1miiHuDeaZTiDD2HZWu_-mBgUp0UHOEJFTtunPYAF7aelz2CKs4nALYHrMDCRiN_Zu4Tj_UAJ5B6m0rA0TbZxpPePRJD1GqFPxulVRgvYMh9blvIotgqqu2Gs2KlDyLXLQxG7ZUYHtkyC8Ij3tkYX5NuRcxJVLBWEAS3oLOl_c5S5gAW64vcEr0liKrsEvrh0MknNVRAR1LE-fkDic5d40GTP9FE-kEB0IRWbEBNfanl0kV-jDv9NAnkNCz2231ks_9CgumOAsigIW274w4HUzxndn1rrUA2OnTvwM2ucJH340JykecDCifYjejCf8lHVBO4Ep1_jI1qEx?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2UaWiSVZ7Yv2fDnW3KYsBTjPn7vdqsEDhDbCPCCT8pznaNEVdINzm8fAtsTk9MlUXI0hEY_2ggqLLhVYr2I11JrOd3SJk36ElusT72yJ2xaTJ-WxIw8hgm8,&b64e=1&sign=653755e454c52fbcbd544c9cfd6ce735&keyno=1',
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
                        count: 67561,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        }
                    },
                    organizations: [
                        {
                            name: 'ОнЛайн Трейд',
                            ogrn: '1027700458543',
                            address: 'Москва, Ленинградский проспект дом 80, корпус 17',
                            postalAddress: 'Москва, ул. Щукинская дом 2, подъезд 8',
                            type: 'OOO',
                            contactUrl: 'www.onlinetrade.ru'
                        }
                    ],
                    id: 255,
                    name: 'ОНЛАЙН ТРЕЙД.РУ',
                    domain: 'www.onlinetrade.ru',
                    registered: '2001-09-07',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Ленинградский проспект, дом 80, корпус 17, вход в магазин с Балтийской улицы, ОНЛАЙН ТРЕЙД.РУ на Соколе, 125190',
                    opinionUrl: 'https://market.yandex.ru/shop/255/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1730814481
                },
                phone: {
                    number: '+7 495 225-95-22',
                    sanitized: '+74952259522',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86yK_6dbq1Uik5QP2R-O0jGX-usmjEVfEtc3m4V2MdirPyCA6opEv3ONEb76sImV9SyiDPdnDL6tp7Fv3B29wVo7IPMB6BzlKs7nxEjHo9P5dftnPrcFLKqr7HFjf34cOlmfSu919HTMjNUxXQ6mFxaNjNgVWbUnsqLC9XWqv1822Aaa8LBABUJm86GMXOux9l8r-_cA6qCXzx1jJSR-YzQcWbsLbsq8pRTfUQJccl-FqtakQeZdo9RtjVtlWRVsvHtwZruHNaOXqUqjoL7_6fXlAUtWWRVzj4YfB7YAaI5aHltmyR6wBPZFwSmV1akNtD8b1le6Da4nyZPwJqiaukJMbMNt0uIrkax2O6WJKYj-NuMqqW4ebX1VvaccNLUhSupXlJtHVgPjLwOlPMmKIQ7BnxYqnnlJxCcrXlw3BznXyuwRRQENstkf7Lj7zR1JrWqHAEuVFR3k98uhaCpnVxlniizJy3nYOsRWE8B28S9qKhqs-aGzecWtaLB7NEvV5AvsYbkSG7nr90RiDNkvTlJUyVDtS9qjLEpIfpo_I5FpCe5GKOYVuGpynMy3oGo3X8BRgO7iuwJqRPXRkmMXnz4Y-4G6MFu4GPdHjRAnpQd-cXXe1iuSvn3ct1j9EiKFjjhFVx5hq-ZEqGOxZwv6Wxu0QKA5FHw7vSkejdUon3poFCAqXUxStg78XYfUgTbm9CBaHvdtqHJPLWDP3m-EIUCcJJNnZinv6uWDui-VDnX65DrR7JPYM_lbp6pRS8fE8I23xX6FwjEYvrVL8aGvkXjV2APhJiIBGqOzbhC1z9viHunb0PkzWxo1YZ-i3heHoFRaar3ugSxGad1VL-dvgIRVvLGGCsKs7F?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O98n-bxmpJOvl9DU76yVKRqwntAMncYKiBD4Jb9xjRecaxlKJ4YBmErP-RxaP3QKXGZpXICXhtfWOMn1DvkhYYke8LxVXamNqgF8eOvNwilLdjbHImmjvAtMowLFu8wmer_5cWtNHEoMj73Fu1qfiEoGEyrxKNKOGn6TJ90nrSyfg,,&b64e=1&sign=673c0109123a8b1c64b0fb854364c851&keyno=1'
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
                    brief: 'в Москву — 300 руб., возможен самовывоз',
                    inStock: false,
                    global: false,
                    options: [
                        {
                            conditions: {
                                price: {
                                    value: '300'
                                },
                                daysFrom: 2,
                                daysTo: 3
                            },
                            default: true
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
                vendor: {
                    id: 15157809,
                    name: 'Hasbro games',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1030151991370941974.png/orig'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/JTBZS4z5ScsQO981S1lfOA?hid=10682647&model_id=1730814481&pp=1002&clid=2210590&distr_type=4&cpc=HI3uXCIAUCOn2oLXqGe4YUXICZNrJ2oBUgQn9ugZqapymhNNuFTIjr-EzkMyT49iSqe9C9BqnVEGcGnKPChtMyqngE0BPyzGInM8MxwdsrVSI9C2kEEl49fdplgO5HHd&lr=213',
                paymentOptions: {
                    canPayByCard: false
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/201646/market_b34zTHC5BPC9WezYTRaAaA/orig'
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/201646/market_b34zTHC5BPC9WezYTRaAaA/orig'
                    },
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/237949/market_TUX0vjR4Fog9JW886edl8g/orig'
                    },
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/403468/market_HMuw_4aG7WsRzkRkRefTKQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/201646/market_b34zTHC5BPC9WezYTRaAaA/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/237949/market_TUX0vjR4Fog9JW886edl8g/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/403468/market_HMuw_4aG7WsRzkRkRefTKQ/120x160'
                    }
                ]
            }
        ],
        categories: [
            {
                id: 10682647,
                name: 'Настольные игры',
                childCount: 0,
                findCount: 374
            }
        ]
    }
};

module.exports = new ApiMock(host, pathname, query, result);
