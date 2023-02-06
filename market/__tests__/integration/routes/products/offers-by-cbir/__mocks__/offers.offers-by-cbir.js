/* eslint-disable max-len */

const HOST = 'https://api.content.market.yandex.ru';

const ROUTE = /\/v[0-9.]+\/offers/;

const RESPONSE = {
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
                childCount: 10,
            },
        },
        currency: {
            id: 'RUR',
            name: 'руб.',
        },
        id: '1571860682863/cc6b3eeaeba14b998773a35599950500',
        time: '2019-10-23T22:58:02.98+03:00',
        marketUrl: 'https://market.yandex.ru?pp=492&clid=2210590&distr_type=4',
    },
    offers: [
        {
            id: 'yDpJekrrgZGNpLQTfBjKFJuqn_nWIzNZ0EDlvihTZ037_HG4SYQeDw',
            wareMd5: 'Gka30W292kXjFe6pV8FnMg',
            skuType: 'market',
            name: 'Сумка женская Guess HW VG74 39060 BLACK',
            description:
                'Сумка женская Guess HW VG74 39060 BLACK; Страна: США; Пол: Женские; Тип: Ручная, Через плечо; Цвет: Черный; Название цвета: Black; Материал: Экокожа.',
            price: {
                value: '10600',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-C-PgZL5EU1ufxL73mkZ9s8wsQQlyON8oru_i__k_I8eyYgDRie2WMFgwLw7_eLxL9lqv1NSbtv1OtkQbKs6wyiRzyGuCxJEArc0soLW2OZ7zMsH8RbKyb2RIWO9oJsf5mY-eRNLxQ6bBGrrh-Ybq0qWSAR1g8aTShJ7MK0E-dAEnQKj_K9BzObtzSq2p2cmv8HC64oZ2X4pfj4eB12mMz7jm6A1u5SOMzdq3xUgab4_9eQ94QMpPfMZa8A8iIz8GRzeYxutqNjwf0RPp_5jE71DBwvvGoQaZ0Oiq1fFq5sINq35R4wLYj7GZRKt-SYf51BArdzt6MEcS2eux-nnwLEc4Pp-4TN0aMinyY7h5t7VWQXvPzEXnCDzXhs7X32yZTLlmfU7SOd5ve8HD8z9JINAzFz2QxZBz3rHf3-lXYWOSye5MG_hNhgRDvNE3Ks8BbrlBBa27Tv9GRj-dHEtlXOC1YMuVP5iahoLAnnyV5x5nMofsBhy3pnl58NaMVo9bEcshLbD2YoCIY95D396Jds12npVzD1PO6tgAMKFbpQ9kQxuzSFrEPKqXWwFok-bZC8taJpAnFWGnHbPHXStX9vrKRzbSFcXu40PUS_2VvYNrzKW9F5KS1Ts29NEgYpq3KKHvZ_l-uqQvZGXwCiTIchZ5eY8ggngVH3N_KxiOMa9Z7_ykbDjmLj-sl5n2rT-lzcRiXhrfeSZHT-G0_czqBQKQ-nrm17w3emDMgoCJCVU9V6jlF20-ijLga48XfXbj60T5FATFB1m?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywnyvgcC1ZLYPAornkUtZrJIlJy2uww5P5e3E3VfmgAsAM0k_Un_RjJ4g,,&b64e=1&sign=148eaac83d767608049e8bb0a062a0b5&keyno=1',
            urls: {
                492: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-C-PgZL5EU1ufxL73mkZ9s8wsQQlyON8oru_i__k_I8eyYgDRie2WMFgwLw7_eLxL9lqv1NSbtv1OtkQbKs6wyiRzyGuCxJEArc0soLW2OZ7zMsH8RbKyb2RIWO9oJsf5mY-eRNLxQ6bBGrrh-Ybq0qWSAR1g8aTShJ7MK0E-dAEnQKj_K9BzObtzSq2p2cmv8HC64oZ2X4pfj4eB12mMz7jm6A1u5SOMzdq3xUgab4_9eQ94QMpPfMZa8A8iIz8GRzeYxutqNjwf0RPp_5jE71DBwvvGoQaZ0Oiq1fFq5sINq35R4wLYj7GZRKt-SYf51BArdzt6MEcS2eux-nnwLEc4Pp-4TN0aMinyY7h5t7VWQXvPzEXnCDzXhs7X32yZTLlmfU7SOd5ve8HD8z9JINAzFz2QxZBz3rHf3-lXYWOSye5MG_hNhgRDvNE3Ks8BbrlBBa27Tv9GRj-dHEtlXOC1YMuVP5iahoLAnnyV5x5nMofsBhy3pnl58NaMVo9bEcshLbD2YoCIY95D396Jds12npVzD1PO6tgAMKFbpQ9kQxuzSFrEPKqXWwFok-bZC8taJpAnFWGnHbPHXStX9vrKRzbSFcXu40PUS_2VvYNrzKW9F5KS1Ts29NEgYpq3KKHvZ_l-uqQvZGXwCiTIchZ5eY8ggngVH3N_KxiOMa9Z7_ykbDjmLj-sl5n2rT-lzcRiXhrfeSZHT-G0_czqBQKQ-nrm17w3emDMgoCJCVU9V6jlF20-ijLga48XfXbj60T5FATFB1m?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywnyvgcC1ZLYPAornkUtZrJIlJy2uww5P5e3E3VfmgAsAM0k_Un_RjJ4g,,&b64e=1&sign=148eaac83d767608049e8bb0a062a0b5&keyno=1',
            },
            directUrl:
                'https://slepayakurica.ru/sumki/sumka-zhenskaya-guess-hw-vg74-39060-black/?utm_source=market&utm_medium=cpc&utm_content=7885&deduplication_channel=market',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5sLIE6enaT4LCdp9PDmdRdZiOlllSS_6Bx3AvwjKMzfp_x11w0CUQ1GPn8W6-03pjuC-iAsjad0bOwViRjgugLlgGNIled9mgaOmQka8nboCxRzkVtLhA-4CICs7z_wucalviHSAkXcuKc7fXUVTDH1J12ZtOZGbTMgS6Mo-V-rRo2kp2Y-zF721DSswf4V7CkuSM-DrbNEVm3TOxfCk8hZY4fIwi9-NksE2tgLHSQARXLFhgvZGnCRvmdzzk5JypdIddKCpgGd7ImswflDUVHDmEZG08IRzDAuX6YZx3AdUJ2S61xw7qDkL_u-ADAOGR3hxiG3HaCQromY6Xakaaw4by-M3kV1lZEREYuoZJg1VKroh0mzdAqxstKqBnw79Hn3-_1L2GP72wjnkqf1eQ6lnaiVfCf-RoBnBzBgLDCjqQZCti82NbsRZgaIJ6Zs-Qu5sEuiyyP7VT4Pqh0w43kK3bB4Nf7dk414iME41O0ZNAqJePvE8Hg1700HUUmYL5jA4P6WT7xT2UncaYOoXVLDn67He8H1ktaE1H-_IZVaXXcYCrt7NY7WcwFoFGTsOKCri1L3VogehCnghZWJF1-Doyi6hkTCv29pqxzBAxp8lrOF85ieSz_Bg2pCJTS5OhOz20iO6le7nht5lmQQU5Or3LWvYu_m7bnqFqRC2ed6zkIEHJ_Loi86VdccAYhmWLUHly1E9DAMyzLLGVvwbR6majRP2zeL7jb-qm7nq_rZOkHbdLxjWPl_z-aOs6Q7Fmg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_Mh10p3rkVEwce0QVWd9XunfvxEX_MDr10MhMvH3u0JecsH5Qw0O-JDZ0na_j2fFuSWenDqZv8XjwsAEAU5BJ9E,&b64e=1&sign=6953a17085ee9413814abc93825039dd&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5sLIE6enaT4LM1eWSFemhAw6XaDzFyX6ONqENv0tbWiCBiPEbkOp4G9BA7iZakvprHekqNkLLY0ifsiyrGxHKYCgG4rLEZqpNGcJ4A6XsmpKZp8alDEmk2lxiylxcD8RSA3wdmbh_QgjgO6ORjEHvWOmCOX_uuwckFlh3c27Zp4BK80BEkQ0gKfEMwGt6nfsyUe-h3jAMV_llrT90HWtydQ7FcooO7QFzDqjjEmCJlrb6etLFHIDd4WV2PubE7O-KVB4pJOxpRoPscwtmto3_-AiLMpsmLudhgsmelirt0M8mwSPvQHmMavqPH8kldo2lVrKRAVWubML6Ux7OY6I5Xk3IKWjy7VixzXgk4G1LWVu0ZUJW_4bSZghAE8bj-9cvHO70iaiMddvW_-eRZyFt2lQUVWi6tLqBW9w6gQGIWXOIFE1KJDN_WKR977ZEr6Mqp7JZYYNd0l9BMtEVmpBqj4tdn4zkRtOsazYXbhz7PP3csGV8TdhDnsZdoBylEb7hsUFL3-PfVygHj8atR9O_VWvhltph-p3TQuV75XY8A8qtlEKsPYyocpvPdUs1rD6ZyX9sDyAFRL_mJiIPLzFw0xAlEraJORMovY1VJEinrjygVOho-q5sIvLDShN5Bas3bRUhy3RW2CpVe2Gu3QGegHgOq2BJCYOYaf7rGcflLDMZP7Kjshta-qe1Yi6Q8tMPBK88zVg5gKix1_YxEEMwY3T8mMI_VsFogBjwCy5AJ0Vj0dodx9AniWZEp1g678NUQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_CzfxhuRiCDkt2MBTYFlJ8Y51YReytA8GUtAxGYyUGB-q8MapEQpj6Dp1zApgZhN6GulTqW6W-fi5Mdb4AsujqY,&b64e=1&sign=c37d02b1ad74e42b962af8dc8888cdaa&keyno=1',
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
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.9,
                    count: 101,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 2,
                            count: 1,
                            percent: 1,
                        },
                        {
                            value: 3,
                            count: 1,
                            percent: 1,
                        },
                        {
                            value: 4,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 5,
                            count: 93,
                            percent: 98,
                        },
                    ],
                },
                id: 476640,
                name: 'Слепая курица',
                domain: 'slepayakurica.ru',
                registered: '2018-05-04',
                type: 'DEFAULT',
                opinionUrl:
                    'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=492&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 494192821,
            },
            phone: {
                number: '8 800 500-53-29',
                sanitized: '88005005329',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-C-PgZL5EU1ufxL73mkZ9s9MIpRltEtBU1Oo34cCSwRD-y2Ab5D5PFar3zctLH2szZSnygg_18U4pdvtkAY4Evpi9JsB3f1_FJZ918XwZ_BONTxJgs62SeI81MM7t-TXA48kGucepxWpr0JUb6ONMmWTTSLVcXwWb8a6m3jiv0ZBYxH4Gwd2d1Glud6TI-_fWMRFj-rDfNVNr5LJZ6QqH5R6pHlA9cIunONa-_OIoQQVSxtSr9zyI7lVgDt1rPBycA58csIrnFqxAPZ5bh-otthXwPvq19vELYt7a7SOFddxZRI4ewEwwVHMYanYceCDfbylQZLw-8GOd_8_uh2BL3ZGQ9rIXD1JMWSnUebspuPid62OA2nI3mcV8511WmNN5jBzYFyfDZnHTmXRHZJJBb_KifQadZyIx3xmBKH251b-5ry70iTb9vldA473aui_h4TVh0gG7_Nf5sDGGejN3mjHjduuSJ23D6yq4JOdh2ZDn871UAucU_4D60JTYyXzbL8rQnIBjd-7hk2k4Efjw4ndkuFGrpPuYQaPYIIwZS8H9jfJWProarlkVrWXJXB_OG62oMSmlD-nQl5xmyjrdPF5ZRfUMJJpK0fA7gnOCjmaFm5jQZEOrTE1OSJzzs5sK5Zr631kzXm6mAxCQRBr8FWyj98mGbKJ7H24wcFIhdyf1kbhJFw38XEj3s6q58g_4EZEbpBn6xIJ9yMiD-4xNx5llv7lT9DZmkcpbdhs01GlrERRx96150bZG0iMp9qkfot8w0n_AIQL?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_OQERUebGnkiKHPWbrDR6MrPYeDFBtIvzbS0PE8qSjR6xdwr_Kh0t6kcykcmBqtYH6rjkj0rYC87XRJRHpheSDJ0pcJI6zCu-SgtIrPBeKc_joiUihS72JMFDSoesnm1mDgR2nnIANme4HcT3NR8JJOpRSxChBTc6P5uU_Zf6OQQ,,&b64e=1&sign=0c0078466fb067e962397bc6d5fdcaff&keyno=1',
            },
            photo: {
                width: 569,
                height: 575,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1577758/market_szPrToKUpEKDwHrxT-DFAA/orig',
            },
            delivery: {
                price: {
                    value: '0',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — бесплатно, возможен самовывоз',
                pickupOptions: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                        },
                        brief: 'Срок уточняйте при заказе • 2 пункта магазина',
                        outletCount: 2,
                    },
                ],
                inStock: false,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 2,
                            daysTo: 6,
                        },
                        brief: '2-6 дней',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            category: {
                id: 7812201,
                name: 'Сумки',
                fullName: 'Сумки',
                type: 'GURU',
                link:
                    'https://market.yandex.ru/catalog--sumki/7812201/list?hid=7812201&onstock=1&pp=492&clid=2210590&distr_type=4',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 3732918,
                name: 'GUESS',
                site: 'http://www.guesswatches.com',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id781654316530114328/orig',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/Gka30W292kXjFe6pV8FnMg?model_id=494192821&hid=7812201&pp=492&clid=2210590&distr_type=4&cpc=OKGXw_tnfhyF1GB5pB03E7qHo6o9-J-y6oDh8QNhHP4IFi5jA9CfeUFQ8TJoD0YZZ5By-NWQHdW2RAilq6v985ukZU19G-NGBN58zH1sA2GQQA0v6pdwLKCeq9bGCjCwq9tm2sv4ZUsuGrKHniUgdXydw2l7e6NB&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1608,
                    SHOP_CTR: 0.005549263675,
                    NUMBER_OFFERS: 5,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_Trivial',
                        value: '1',
                    },
                ],
            },
            photos: [
                {
                    width: 569,
                    height: 575,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1577758/market_szPrToKUpEKDwHrxT-DFAA/orig',
                },
            ],
        },
        {
            id: 'yDpJekrrgZFTXPN8ehFbQURqfFgWCXtDUxZtnvU-YuLmlbPdHkWaFA',
            wareMd5: 'OClKHhAYbOD_GF7n4nQ0Sw',
            skuType: 'market',
            name: 'Сумки Mano 19512 black',
            description:
                'Сумка на молнии. Внутри: 2 основных отделения, 2 кармана на молнии, 3 держателя для пишущих инструментов, отделение для мобильного телефона и 4 кармашка для кредитных карт. В комплекте съемный регулируемый плечевой ремень.',
            price: {
                value: '23072',
                discount: '20',
                base: '28840',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZlP-eyQ4XphjPbrbYvPNHee6foEgTsr0sVgBurjLa1_5y6r63yC9-cPwWgJlWK5WaBsEfodXg1dS9mwzrSKWk-SWy1IegQzh2iQKmy72au7dUUhQKx8HHHfoN6pRMI4SYMfG8GDkHCyS9o0yVkHxKAOy6EZwlxIZc5CD3Yzw7_imGR_lkRB3ZyCQEnMbp3Ct5ZSxJ85YCg_HpRIJiqwBJspZa12QA5JL8ke73bLJ3RJJVdKL3IPPzno7sBJcl5lasmKnrB-9T_cjHkFkjy3wV-lri5bzAkT4HRcc8p6IetEVLKdxZC_vo3__3YOuyqKWXlCeAhmWLjbeFcngfS48OU9axZeq5Jq0Ve2Wz0nCkr_0fGHscLaP6zySuyKS7pjltZfbBSwgM--4Psvd0jl5oabYU9-ope5isNSCiRkkS4NO5QbsfnGlbH51EKd3Cj1r5HIwZkfDQst5lMqTwfQRrvsIXRODZWIzhEBd6nRGLcnPi2hikKaOc9nK1TTKQs8_4a5yRYUNr-fvLpvidvcydzExe-mGZtp9MXoaYnZ9c5zCOBysiWuTLzsuUEUK2JKgDzBQ6LWxQXGW-aLtHcjSxImWL8PC6eaL20ab96obUuMJlK76aT6-y0ZSQcnsi-mCI9jkQ_hfs49F6wzK_IaZAT54V5ojrICVv579u9YfAFkYk5ssY8dlHKYdrJ4NbNNLEZpCMPXz3VMsm_PKNlIC-26SOAbrHd_M4vXwt5N-8289tcBzAcm7yVKQ7NRzk84s_nOrCCKMW6johgCC3YHNv2O5s6EGYDw5YVhsGG6J8EsFQLjFz54metm6Pt8Fuj0GJVFMTHoOYHsRQIrIuMtaY-2Va19CDIRpGCd4OoB0Tl8mLhMW_FSq7MWCBXk80Is8Xw3ORTpnh7y9tEDlpJsnBX1nQhJi41Qrb1EysczQj9TKHv7JbXeYIBxr_F4YKJvr0WwhiN1lO1WLkfKd75b32z7_8nbPabu-xJuQ8baMjBYM?data=QVyKqSPyGQwNvdoowNEPjUjLRGpPKgWn7nU3wqtVVDM5EDED_p6M1c6XjPnuKpUGFp6S1_vADe-cOssYEmdLQyfvS_z0BLRvtyXKpoX-PVCSO9spEDMI97is7kvTMEWLLq9mqxSLZDYJsfHmp2sH06XwFxFVj-UdXjVqfTRraz27Gwtugl79aQSrys4v00TbF24KLJS04fwhaTyGOUe8R_3oYFewenFpPGDS-V81UMrD-KUfm4JwBkKTpdzvl6u0jNuFT-8ZwezfLb-o1Q9oZ499ymY23MRjwvXY0TcY8-9RxJBzCk098W2TAHJXkLJfTQAF9GLZy5DDmrOoo9MnLa7inQCRgwuUlIcRt4XYYfs,&b64e=1&sign=a663f4ea042f6aceca6c4cde12185d5b&keyno=1',
            urls: {
                492: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZlP-eyQ4XphjPbrbYvPNHee6foEgTsr0sVgBurjLa1_5y6r63yC9-cPwWgJlWK5WaBsEfodXg1dS9mwzrSKWk-SWy1IegQzh2iQKmy72au7dUUhQKx8HHHfoN6pRMI4SYMfG8GDkHCyS9o0yVkHxKAOy6EZwlxIZc5CD3Yzw7_imGR_lkRB3ZyCQEnMbp3Ct5ZSxJ85YCg_HpRIJiqwBJspZa12QA5JL8ke73bLJ3RJJVdKL3IPPzno7sBJcl5lasmKnrB-9T_cjHkFkjy3wV-lri5bzAkT4HRcc8p6IetEVLKdxZC_vo3__3YOuyqKWXlCeAhmWLjbeFcngfS48OU9axZeq5Jq0Ve2Wz0nCkr_0fGHscLaP6zySuyKS7pjltZfbBSwgM--4Psvd0jl5oabYU9-ope5isNSCiRkkS4NO5QbsfnGlbH51EKd3Cj1r5HIwZkfDQst5lMqTwfQRrvsIXRODZWIzhEBd6nRGLcnPi2hikKaOc9nK1TTKQs8_4a5yRYUNr-fvLpvidvcydzExe-mGZtp9MXoaYnZ9c5zCOBysiWuTLzsuUEUK2JKgDzBQ6LWxQXGW-aLtHcjSxImWL8PC6eaL20ab96obUuMJlK76aT6-y0ZSQcnsi-mCI9jkQ_hfs49F6wzK_IaZAT54V5ojrICVv579u9YfAFkYk5ssY8dlHKYdrJ4NbNNLEZpCMPXz3VMsm_PKNlIC-26SOAbrHd_M4vXwt5N-8289tcBzAcm7yVKQ7NRzk84s_nOrCCKMW6johgCC3YHNv2O5s6EGYDw5YVhsGG6J8EsFQLjFz54metm6Pt8Fuj0GJVFMTHoOYHsRQIrIuMtaY-2Va19CDIRpGCd4OoB0Tl8mLhMW_FSq7MWCBXk80Is8Xw3ORTpnh7y9tEDlpJsnBX1nQhJi41Qrb1EysczQj9TKHv7JbXeYIBxr_F4YKJvr0WwhiN1lO1WLkfKd75b32z7_8nbPabu-xJuQ8baMjBYM?data=QVyKqSPyGQwNvdoowNEPjUjLRGpPKgWn7nU3wqtVVDM5EDED_p6M1c6XjPnuKpUGFp6S1_vADe-cOssYEmdLQyfvS_z0BLRvtyXKpoX-PVCSO9spEDMI97is7kvTMEWLLq9mqxSLZDYJsfHmp2sH06XwFxFVj-UdXjVqfTRraz27Gwtugl79aQSrys4v00TbF24KLJS04fwhaTyGOUe8R_3oYFewenFpPGDS-V81UMrD-KUfm4JwBkKTpdzvl6u0jNuFT-8ZwezfLb-o1Q9oZ499ymY23MRjwvXY0TcY8-9RxJBzCk098W2TAHJXkLJfTQAF9GLZy5DDmrOoo9MnLa7inQCRgwuUlIcRt4XYYfs,&b64e=1&sign=a663f4ea042f6aceca6c4cde12185d5b&keyno=1',
            },
            directUrl:
                'https://mpwatch.ru/izdeliya_iz_kozhi/sumki/mano-19512-black/?utm_source=market.yandex.ru&utm_term=28524&utm_campaign=msk',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRsbvjkerVTAqMvZKkQkgb3AQBj6BEkX7sOobEY6VjigNVf9W74fdPZyOj7Vd8ONj9M-7iLD8rT2OAC3_amQfBVUD59u80CfzsjpoMCABdzuaTwlZbZBcBMc9vEcpKXCumRUSYFY9BYu33IvhDw1qsfVAuFbqkSSN2xrHU_5iPjYK-KFIreNjDgDlSmpKuJVr1EwAB5PFcSjAsOMFM3v1E-GvC5ioIKeC35N_iSHP0c2SIqBBQQOgC2OVSX5jRwXWfKfSCG6-gsjYF9YTeQpNRafbmDKPMScLAcgYX62W0lTUTVrRrpAem_kJn83EUIHDSyGJ6vP1Ohl6d4NTdB53VsOD8GSypmEj9li_C2RtZnG1hWufPhO12s_ciGZgoMmp3OYjOtMXaKZ0WRfRYiDT0oGH2gBsQ2NeBhPpcMcvysRu_6IXvuxRZTKwZTsrEbkWtrGWjDJLn88c5VW3k5TgkAhoAWWOrkd3XMMuMqkWCeNW7GIBDCGSmPlhbDaohZueIWFdgqboJa28hwl_JyCYMcSLQYabHZbKxvIIblA4HSy0HVk3bROFmbDESw96OXTRkJ3ZGJAdsRbrfWsejib73M-ZMQyj4xhoNhAvXyNtM_CITlnU5iDk7bbOD0s3b4p3pBMJeJ_7hmwHIs6ZH7soKeIy_RS21UWpShfLEOLDIQejXY_AD71w51Xv7v3qjeUOm76AMVrIG3coYt1OKf8jJr1fPUckYY9uX7gwNRWRYW-oCAbePpG2uSQFYXMzxAvGnscKMC0cEgr8UWZECybeW-_y7wTv8p9RbNxqPjBIDQ4jb5-_NUisphVMfdleq-O5Y1GFu8Hh6vRW_jkRhmddFaNxc8p3Hy54zIXT51R7CaCOHwuADHTVV332ops_2jJBkM86-qixKaF8P6zVx3l4bN2QETbSPFHIynVStBu4ypyatZ93NtEkWyvWVCI9MnTHVni8pFWXHPgUbEV5fRs5a1dlZT9HhY9U6Yeemb5dqsySQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2f8tipy53eId5gNfQ1soFjisxj3oS7wJk0veqauLWDKX1KJG9jjeuRYHgvZNdti6wABrGzs3jjOs2TteokvvVsbR85wvj33xFzRceNFGgKEwTPo54nfeKvo,&b64e=1&sign=6aa36cc2c0352f1816aac0556973b308&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRsbvjkerVTAqMvZKkQkgb3AQBj6BEkX7sOobEY6VjigNVf9W74fdPZyOj7Vd8ONj9M-7iLD8rT2OAC3_amQfBVUD59u80CfzsjpoMCABdzuaTwlZbZBcBMc9vEcpKXCumRUSYFY9BYu33IvhDw1qsfVAuFbqkSSN2xrHU_5iPjYK-KFIreNjDgDlSmpKuJVr1EwAB5PFcSjAsOMFM3v1E-GvC5ioIKeC361Ww8-PxQ5fRVs8JDnscDy5Av0j31CX6bj6j4WGe7AUzKoMOdT3e-0kzoGb9fDTBoL5QpR4chq87GoRxGK6tMBvfUebYO4_Lj3RSPi8nPGOxZQadOt04dk8X0W0J_QCbG4nXzZfPGlqN9NkrlilcjaXfrHS0ue88SRKWsqa2zRWQLulYbU-pXMJIZqiture_ydlfIElh9r5JOxpgpBHEL0bjF7B7ZMs5AekBaOzCiUi5t93tnLNUGJC8SYJC44DpGd0Z2lF3U-5ktRE7JQPcxIjD2uKWBU6o4FHlq9SL-ORzCNZ_3Jh4Gzh1fxtGoxvbjZe4qU8L7X4Vf3xY-u-Uivrd4rDQHBh02glP0L_ZUYTQ_C4XQqW5Q6blLV_MKSXCCpaogz9t7c4LcalkS-AwJSpFi4iWFDYOVlAFDzZygcmeh4c9qjZ_WwRWBV2Re6eSkpHAr6y4fzNUQJMYeKcVYphm3c_HxavpDk6KRVrd8VvWI4v6rH7u4XZesf045z-YZ9uRJScrDePxhy_SSfPwzVxWGRtWKL5ahgYLA097NLtJoZ9Vt1cWkPBuD0Ny1MyZLl8HC-FXRch2LM4iQGdT3f1umyDc9VI4MGutBmDn7NgRMvoasT7_QJXdCcEAKtVfgNFzwPnt4FAKnwr1FC7HPgqW-PfCn6DidblTrSylePwHuQcI7C4bbPqdhtGpGWDuTK4o-YgLxNXWIDRs86DA5vu8LiWlFNcHtZXROJSC4YyjPhD-GHaxfIppA9_YFu6g2QWHaEuRsjYw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2f8tipy53eId5gNfQ1soFjisxj3oS7wJk72VNypSwE_ne9VVItrVncUvW0hBfqjlLMk2jsivXRCEtYvkuZTVWb8H9pnfLCICPmn2KVrekVfZ6omju3alNIY,&b64e=1&sign=fdf25abdd3f9dd50b926a1bc8236379e&keyno=1',
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
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.6,
                    count: 3342,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 70,
                            percent: 3,
                        },
                        {
                            value: 2,
                            count: 15,
                            percent: 1,
                        },
                        {
                            value: 3,
                            count: 33,
                            percent: 1,
                        },
                        {
                            value: 4,
                            count: 139,
                            percent: 6,
                        },
                        {
                            value: 5,
                            count: 1969,
                            percent: 88,
                        },
                    ],
                },
                id: 256507,
                name: 'MPWatch.ru',
                domain: 'mpwatch.ru',
                registered: '2014-10-21',
                type: 'DEFAULT',
                returnDeliveryAddress: 'Москва, 2-я Машиностроения, дом 17, строение 1, 115088',
                opinionUrl: 'https://market.yandex.ru/shop--mpwatch-ru/256507/reviews?pp=492&clid=2210590&distr_type=4',
                outlets: [],
            },
            photo: {
                width: 1200,
                height: 1200,
                url: 'https://avatars.mds.yandex.net/get-marketpic/226517/market_342f7P3D-CiiWiLUu0wQrw/orig',
            },
            delivery: {
                price: {
                    value: '0',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — бесплатно, возможен самовывоз',
                pickupOptions: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 2,
                            daysTo: 6,
                            orderBefore: 24,
                        },
                        brief: '2-6 дней • 1 пункт магазина',
                        outletCount: 1,
                    },
                ],
                inStock: false,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 2,
                            daysTo: 6,
                        },
                        brief: '2-6 дней',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            category: {
                id: 7920819,
                name: 'Портфели',
                fullName: 'Портфели',
                type: 'GURU',
                link:
                    'https://market.yandex.ru/catalog--portfeli/7920819/list?hid=7920819&onstock=1&pp=492&clid=2210590&distr_type=4',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 8339322,
                name: 'Mano',
                isFake: false,
            },
            warranty: true,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/OClKHhAYbOD_GF7n4nQ0Sw?hid=7920819&pp=492&clid=2210590&distr_type=4&cpc=lk2-67nO0FE2qKAsJlAiXTek-1GXp3Wkw-nkXNHz2FXSgQNvCCpX1RulrHq5lfCga8X0B1hc4vu7lbk9frVLOT9wX-GxftFoigUySSLor136ieIgDqgxH3OtBcLS1OQ9SuCEmwOfdnOtKtEdYRDo38cSzYlPpnNd&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 217,
                    SHOP_CTR: 0.002974258969,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_Trivial',
                        value: '1',
                    },
                ],
            },
            photos: [
                {
                    width: 1200,
                    height: 1200,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/226517/market_342f7P3D-CiiWiLUu0wQrw/orig',
                },
            ],
        },
        {
            id: 'yDpJekrrgZHepZ8Up8vjM6SNbnnk4cD_j3hAmW2DNinGSY6BFcAYuQ',
            wareMd5: 'OWV-X8mWCFyi3HEe7yIy_w',
            skuType: 'market',
            name: 'Сумки Furla FURLA ALBA L TOTE',
            description:
                '8 (800) 700-88-66 - Сумка FURLA: кожа натуральная, id 1992243, страна италия. Сертификаты качества, удобная оплата и доставка, бесплатный ремонт.',
            price: {
                value: '20430',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZiAUWOVA7bw4mcIyiD2olafm8QV4czzEnaBA9tKbp-FSEbWaIdtf8BgNGG9mcZiLAcTZ8LL81nCqhz2KLob-O5Zy89DK44l4yWHAchKrU-6GROcmFVdYAnB58rZSI7sM3nDqnui1tNARLcPWH9863AY0MC2ZAz0IAYv5wR8KU_1boBN7zS_hWVC6LpXBtCgDN1MgeB39aD1sC4_WSdEAEIMsiaVdkZo-K3nDKa_HLtVlILv1qw_fcyE_M60P9PFkOOXOUYa95_UA3B8yUqBpn8eNk-CPoWnUcUDS97mSlPWkaKCU-gMFUJvQKXWLFN9h7Mhali_J7oJ_gD4Rnz6KDQbqfpQ_VXk1PfEmXXvlrtguKg2zFdSOLkKijJQs764_KU8FCEk2xeEohpR6307bU90fdEIZKclPa9-W9C9J-mZ8agl61EGy9NUJE_wJlAhZ4P4fnqu22lhXs1tJXbqBLpo_byPr4S_PCp1PRzbm2DcENUb9AMcVviefCsul92kVfKTjTNsWZLB73ujr3vyM_27QTF4UDxalD_jcHQ-ZhjdhxEY3ZRqBFVbLHxFDFh3amym_zKueZOPpGIa3ebUd_g7hzB7jKlvodU5tXnT5Zpk3sdo9DhqIx3B-AtnPr79Qr3h3qcrisvWJpUjR12GPGGZhHVao9rIy31iMy7Do70UfHDtu1zxf06BZT-I3S43QKWnURlSCOHxKMj_jdInFijrEysuTE2TZiXRvKs1deawWdETzBK3JeiMGzWtfea5G2AV2g-c4DolJOMD5eUCfGUbQTVO-unav43FhhqEEnF63Ji827f-O_C0WT1IYa5gGL97wEGGbantoLmOt39-rk8O1d1T8jTLn9ThdPrHWisWmQTGo280TdbQ3sFgRiMdu6VpDYWK4p-bYY5gz_CL0cMzKW7b4h9q9Ji523fjTz8ErXgpemXv1wolBs0X6ekixsUcuGBvWeeJKSBkSOfYG4cU,?data=QVyKqSPyGQwNvdoowNEPjUBu8iKId2aDgsY-_EtYZ-C4XHVdNPOuhJapGD7K_63Dv7WnP6dIqInaITB3213XP8H9h0ogFLoXgM3WHNZqMBQjfTfoQ5jhTr5UjaUKuPOJ6tZD9xT-Jx3zU2AKSYUrhDMIhtGvryrJgBZvnwbqcgrARdC4nM2BaeekJ0WYHWeagA35tx_hRtADTDBpFLxl4VhhyLHQ4GKP6hX6bqavlcu_rOM3QEJVzPMyeCOEghUildKwp8ae9dT7HCxlpvvpJ9EJc9QXvJFdhIqIDrxzUWxewQgSJMTAGx6r5JYZ5jsKlPz5Xe8auZo9JgOijeaV2B5oxQlLZ5YaR4PdbdtSdJU0OvJ2OU1LMVag-d45okHoaoGTlAGvgUrHFIFrN7tFnV9nV6Gfq0AXRjDTTP1MHeUrqC4qlQebLiUetWtnP517r8XCX_TqEwZiIzAiR3B5NY3FRPKyYBAlErO_vAxayNUG2cM9LRk94zzg6bYiQC9vRtCd-lNjbvlpPDC2OOfz0n2_vsaAfmKhqxiph_AwAP0VHK9roIXLgdQnGkp8pkul&b64e=1&sign=74f2ed0e094bd1e40f223ef3594531ca&keyno=1',
            urls: {
                492: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZiAUWOVA7bw4mcIyiD2olafm8QV4czzEnaBA9tKbp-FSEbWaIdtf8BgNGG9mcZiLAcTZ8LL81nCqhz2KLob-O5Zy89DK44l4yWHAchKrU-6GROcmFVdYAnB58rZSI7sM3nDqnui1tNARLcPWH9863AY0MC2ZAz0IAYv5wR8KU_1boBN7zS_hWVC6LpXBtCgDN1MgeB39aD1sC4_WSdEAEIMsiaVdkZo-K3nDKa_HLtVlILv1qw_fcyE_M60P9PFkOOXOUYa95_UA3B8yUqBpn8eNk-CPoWnUcUDS97mSlPWkaKCU-gMFUJvQKXWLFN9h7Mhali_J7oJ_gD4Rnz6KDQbqfpQ_VXk1PfEmXXvlrtguKg2zFdSOLkKijJQs764_KU8FCEk2xeEohpR6307bU90fdEIZKclPa9-W9C9J-mZ8agl61EGy9NUJE_wJlAhZ4P4fnqu22lhXs1tJXbqBLpo_byPr4S_PCp1PRzbm2DcENUb9AMcVviefCsul92kVfKTjTNsWZLB73ujr3vyM_27QTF4UDxalD_jcHQ-ZhjdhxEY3ZRqBFVbLHxFDFh3amym_zKueZOPpGIa3ebUd_g7hzB7jKlvodU5tXnT5Zpk3sdo9DhqIx3B-AtnPr79Qr3h3qcrisvWJpUjR12GPGGZhHVao9rIy31iMy7Do70UfHDtu1zxf06BZT-I3S43QKWnURlSCOHxKMj_jdInFijrEysuTE2TZiXRvKs1deawWdETzBK3JeiMGzWtfea5G2AV2g-c4DolJOMD5eUCfGUbQTVO-unav43FhhqEEnF63Ji827f-O_C0WT1IYa5gGL97wEGGbantoLmOt39-rk8O1d1T8jTLn9ThdPrHWisWmQTGo280TdbQ3sFgRiMdu6VpDYWK4p-bYY5gz_CL0cMzKW7b4h9q9Ji523fjTz8ErXgpemXv1wolBs0X6ekixsUcuGBvWeeJKSBkSOfYG4cU,?data=QVyKqSPyGQwNvdoowNEPjUBu8iKId2aDgsY-_EtYZ-C4XHVdNPOuhJapGD7K_63Dv7WnP6dIqInaITB3213XP8H9h0ogFLoXgM3WHNZqMBQjfTfoQ5jhTr5UjaUKuPOJ6tZD9xT-Jx3zU2AKSYUrhDMIhtGvryrJgBZvnwbqcgrARdC4nM2BaeekJ0WYHWeagA35tx_hRtADTDBpFLxl4VhhyLHQ4GKP6hX6bqavlcu_rOM3QEJVzPMyeCOEghUildKwp8ae9dT7HCxlpvvpJ9EJc9QXvJFdhIqIDrxzUWxewQgSJMTAGx6r5JYZ5jsKlPz5Xe8auZo9JgOijeaV2B5oxQlLZ5YaR4PdbdtSdJU0OvJ2OU1LMVag-d45okHoaoGTlAGvgUrHFIFrN7tFnV9nV6Gfq0AXRjDTTP1MHeUrqC4qlQebLiUetWtnP517r8XCX_TqEwZiIzAiR3B5NY3FRPKyYBAlErO_vAxayNUG2cM9LRk94zzg6bYiQC9vRtCd-lNjbvlpPDC2OOfz0n2_vsaAfmKhqxiph_AwAP0VHK9roIXLgdQnGkp8pkul&b64e=1&sign=74f2ed0e094bd1e40f223ef3594531ca&keyno=1',
            },
            directUrl:
                'https://www.rendez-vous.ru/catalog/bags/sumka/furla_furla_alba_l_tote_temno_siniy-1992243/?utm_source=yandexmarket&utm_content=1992244&utm_term=29&utm_medium=cpc&partner_id=yad-feed&utm_campaign=mgc_yandexmarketnew',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRtLGWKVOj-gnhHHKY6gla__o7r08ttUyKz5gM6sbsIzHoT2eKxI8b_C133_sd7-gyHu5b4Mq7QrG1rA4pyXmpp887pWL50-uF8h8h9mbdATsnQ8s31w04uLNqWh2Nwk4bQVUJFNe-POQxga3WpkrvM2W4R02Ef3O29EFMtPxtxuKdtN22DYfPlAEnMKVJ1JSdCIV1HOA9gpjvp9fflE5Ft0RhHFQFtLsMesJcUZ6RigNMka-zj43bs5AjfK82Ai5Dv1NvHcrK4v-oOx8xLjhWCccry83Ut-8X7inkblXJ04q4dtIB1JieGcxJ3enKD9Gc-nNh0KW-FeEdAFf01QHjB_RBdTC_cz88qlnPivnQ-gsLEYAtm3XAflnE_s6mV6EtHxOEm-3sIqCYZkLjiRAZtJtrKX1TknxuX5ErkcatsMCKvY_Nxm3VjVtTYgz4o0icnuvhq3lfHpf2YOJ2BASqavGWfhCYlUNcdVkxazElInHVmoxyJTQxaRR4rV7Wcp2119xK-OV0PbsUgKOREdM2pG-JJWZ4746NBS8qvYtH3AdCBhXmT-XoPXqbHYFW9JI8pZ-8mMry8anxarLTdhDvpKbFDN2Q9Ae-O7KhX9y6Y54UfLot2lgzOK23c-5n9qSiQrlQeA5E4u76GGX-lkLR-FIB6a0L96Kj5S-b5BU8t47H_s9cnDgRVlynfhvd_4hG-vnCTs1XsrtsFOe_jLBNbvxyXbTnRGA2jbbzwQPDORHC1VZKZxOYCJiSAivUOYiOBzL_KaJHZ4u6GmnCzFO-dOUQEnVtpWvQlrO7CqsmJp3xPDMX8ekub7Qc3TdcIghT4chXxnEHgyuN5RwQAoV6KAhwuUEnUhe7tXmrW3TFivWWfnIiobwKJMVkGWOhvICLoF7Gwi6Yn5APEIIjxFDoO5gz6RIFigBmKa2_hExopyXAcx3z1xxl_2gI4qtS4f_oHuTQBNtl7hRgaIRiQLtncr?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2csi6vJDhYfXrofNhT7aXCz3miSxdSmWajM9Lo-mJ9ONpflp54urpH55UCyAc0Y3N9v_JYMtnhIoug_Yu9eAQqtvAcX_72LXGcaAmcppO8iRVwyMnCZIwz4,&b64e=1&sign=e2ae5dc61648aa2488d3f580eb8ebbf1&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRtLGWKVOj-gnhHHKY6gla__o7r08ttUyKz5gM6sbsIzHoT2eKxI8b_C133_sd7-gyHu5b4Mq7QrG1rA4pyXmpp887pWL50-uF8h8h9mbdATsnQ8s31w04uLNqWh2Nwk4bQVUJFNe-POQxga3WpkrvM2W4R02Ef3O29EFMtPxtxuKdtN22DYfPlAEnMKVJ1JSdCIV1HOA9gpjvp9fflE5Ft03pAZWznhz69gMz3o6Wv5ltgcj4jwneYOlFonDjF0A6D10UBG6KWu20IZGamvUpeCU8SeQWZOJ2Q3iT4om3wSROzQu_68qRGWQUmRU3nhFb1nDgCdlf9POm4_AhYk40Dp-NRVKAwjqJbFO9C5NTMdZnhuiNdqZ-xSQAf32Z_3tl9czmtttUsXbnowNMa0Ya3HEMME-iWVsRGuCAJId_3DVUsnC-s3-ABQeg9Ojl1dhQX8Hq17v-3qYuGSmPvAhTVIfpOcZUK2UDCLpjFt315rGgR0OWC7KiVIuMswE5L4qWxpNgN8-LdVpFGWoCHEjqkXu5KQlYiA_Qt8pUNiHPRdrgqONLaw5NOgHU3GMH9V4UEbIhqFY8Fi-K43R8-4oka707nzkXCuHEZ7oHVNuto8eJ2EY6Q3OVVVnWMe2rW1u90j59MYHpQU5XT9Chy-41TwJEKg41CeTIhFbFPy1ss02E_evNMZnH4e0U4nbJKfINKMsRN6W4qJrcIFE4rCKm2djaFCRbh8CFwX_loYRHxGGh5Nlh8qocN_0MRQQsl1VNUA5ih8qYJGNGXRsw6hKC3Jjz3JRNSFl7_kA_Z8fWoiScXMQ-gFYwU8kjj1kB7AsifOp1SQkOGv6bRqJFCv7CLroWihMu7QeWWNXztxYyM87BWlxWYG7wpC1beNjFuYP6vaHfNBeqiAAu0smDixXve1uwRGhmbGzpIySwvp0yrSAKYcZYvk23kKqYkcfZap7ATBvsl4fSdWk1Hhklfc0sTw?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2csi6vJDhYfXrofNhT7aXCz3miSxdSmWam1PWQcJHLEExycdMVSmVKLNSgAge4nWA1hA56fBiYxupsbaEiKvLsDGNY8LhnHP7YIKSOKsfmtwMAHGt_38d3s,&b64e=1&sign=e18a8a09ce54efd57c11a8855b1711bd&keyno=1',
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
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.7,
                    count: 5161,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 127,
                            percent: 5,
                        },
                        {
                            value: 2,
                            count: 19,
                            percent: 1,
                        },
                        {
                            value: 3,
                            count: 19,
                            percent: 1,
                        },
                        {
                            value: 4,
                            count: 112,
                            percent: 4,
                        },
                        {
                            value: 5,
                            count: 2468,
                            percent: 90,
                        },
                    ],
                },
                id: 216270,
                name: 'Rendez-Vous',
                domain: 'www.rendez-vous.ru',
                registered: '2014-03-24',
                type: 'DEFAULT',
                opinionUrl:
                    'https://market.yandex.ru/shop--rendez-vous/216270/reviews?pp=492&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 524312661,
            },
            onStock: true,
            phone: {
                number: '8 800 7008866',
                sanitized: '88007008866',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZiAUWOVA7bw4mcIyiD2olafm8QV4czzEnaBA9tKbp-FSEbWaIdtf8BgNGG9mcZiLAcTZ8LL81nCqhz2KLob-O5Zy89DK44l4yWHAchKrU-6GROcmFVdYAnB58rZSI7sM3nDqnui1tNARLcPWH9863AY0MC2ZAz0IAYv5wR8KU_1boBN7zS_hWVC6LpXBtCgDN1MgeB39aD1sC4_WSdEAEIODcRkZk49HNWKdP-Kfop4bkYrKPImjQam7MbPGk9wusiCZ3A19QolfGyu5ZRVOhXRDv0qIRfGm31szsrU0yF41L9AeKGSx7V5Rfbu2BjgALwXcDiNCDmu42a5G66X3B1hWVEsj_AW7iTI3_QrhslMtDZ4cOR-xGmth3ea5BEa4pBHKgUNwmVR81cIm1lH19foj8UKY6DgULI4zWnSr5hX2k1zuvrC0Q4o69ZPga8BjQOSfUi6eUErzldKjm00lu9W8kwUoVku3i3RXfAWkBH-0zTeBWcTMpqv8wkX1iRzhUBc9Kv1OLFQlOs161uLBoZxEMeaZBj-ce3jre_zS6K7olacH5GFNbQm0UX3-Y-_sjI9YW8YIpQVBI3vh1pZ_6DXnpRa5Ww2nt7SZ6K_nbU-N9yIxBYrFIrIGqCNRy0GbBoiop_vmz3VcHkeuc4IYW67mu84b9aTELkit0auzbwKJp1M3AJv7q_4N3vKsFq44picrVBKLaUCKTr67zAX4_HM6AHWZOZq39mj2KS-bNJ7DXnGEWOCKvbQbUNe3i_vKt3vzVVbDG53JH42OpyX0SL2ePjBmrakb0E1odGVq_6gVel_xHiz0mFQbepRmOq1elUw6y70bIf8rUSq6qlwX-Qpl4Wqz99nn093r1_lVJQiG0VpZTy22389wRsa76DCuiAJGfKFk9aJr9lbmZl0sAYAhWrnGiYxfQ1OVyEjasJvOR7keLuEQAIitLH_22mC-rBWuJcecPxOManfdZYopSEk,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O80OlznaUNmNltAW6sYKL7Lh4QqXTjNS-mhYlgRuxmRLDiPtiRXITQPYv_x0Ak4bfcsdttOq08borAjuGB5cOCIC8gIzFWqCLDBqST_9SHCkJ0-4hEMa62DFLKvW896VgU42R7UL_bZFOIY7Hv3BUx-3tvcnH0QBOlSF2CmFJ-p3A,,&b64e=1&sign=43be9a2181a2a350bec508971e510a2e&keyno=1',
            },
            photo: {
                width: 256,
                height: 256,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1597794/market_kfhcB5j62lRXYYIuBFMirA/orig',
            },
            delivery: {
                price: {
                    value: '0',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — бесплатно, возможен самовывоз',
                pickupOptions: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 0,
                            daysTo: 2,
                            orderBefore: 24,
                        },
                        brief: 'до&nbsp;2 дней • 31 пункт магазина',
                        outletCount: 31,
                    },
                ],
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 0,
                            daysTo: 2,
                        },
                        brief: 'до&nbsp;2 дней',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            category: {
                id: 7812201,
                name: 'Сумки',
                fullName: 'Сумки',
                type: 'GURU',
                link:
                    'https://market.yandex.ru/catalog--sumki/7812201/list?hid=7812201&onstock=1&pp=492&clid=2210590&distr_type=4',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 8339245,
                name: 'FURLA',
                site: 'http://www.furla.com',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8542306112941055643/orig',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/OWV-X8mWCFyi3HEe7yIy_w?model_id=524312661&hid=7812201&pp=492&clid=2210590&distr_type=4&cpc=I4gnUO_5dKGbVUySJeUix-k78PXdNeGCNVg1WncX4A92VweITnCIWXD30mWc1CtSLuBoiAXHHiHe3Mogb51d4yXYKBBd-vY0xxXqnYsNWZoGr4-Yt1WH9NbPOorCspkIbrj-N6Lt0VbJGkiCg5LHHRJB8V9fPMIh&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1608,
                    SHOP_CTR: 0.01420719735,
                    NUMBER_OFFERS: 12,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_Trivial',
                        value: '1',
                    },
                ],
            },
            photos: [
                {
                    width: 256,
                    height: 256,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1597794/market_kfhcB5j62lRXYYIuBFMirA/orig',
                },
            ],
        },
        {
            id: 'yDpJekrrgZG2Ivs8ZtXxOI0teP4t96yjzy6uI0hR03gDXvRIUDufxA',
            wareMd5: 'DglzSFfIBjnLxtpf9jwygQ',
            skuType: 'market',
            name: 'Женская сумка Marina Creazioni 3920',
            description:
                'Два отделения, между ними разделение на молнии, внутренние карманы,сзади наружный карман на молнии.',
            price: {
                value: '11900',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZov7eMkNsPWYi6Fucpb2U8fOUcGiiAhTg3K28B3B85ECl7GhNKMvUpSubyAtP63Ge8lghJlBMw5suxzMRMn9V7BG0oRVsBNY9-mcYDEvqBcIGXlVeXKx91jEnY6pEZJ-FGhzkVHbrWYZYAti004QtJI0Q8X1ADN2x_hg38DwFVrkyTf8_ao9SGhacp3IAQOQDDOkJlAd9AnbJIlpARUZc8zX6xLLi9QUmgGq2bL5KNpR0nTKJsjrHZNXu1Vk6amqx5jPQF5WA5vZdUbp25_PmQ2wRs4xbGA52VW1h2-pFsaHiKhFHQPS5kRouXRd0gM41C9b0QykEByC7wvi_MlNAEsgvDHY7o_cCseMvikO2YkivBzsJ6r6MCC5pquwXz_oRBGXvd3USX3BHnkpO8geQpW5IF_cAsUySOTc8FyrhM3VCkmEFZx_qjFPOMGwl3AqHTCigqx_Vc2HB1v5AAlPZC9nKH9Ihkd-FPTCpE4L8nj6ZgJ2bJa53MuBbFJ_u7JLRm5o7xa1z0Hf2jP4GCelqUQQD90DW8Dwn6XG9pIVKksBSQZgh3du0Rg08JPfwddLcZDcnmCBldCnTbSPtEDnct7x2ldmmrZiZrPYKovwpEyrI0juU5-AeobqrJ_axXil_HKKw3VBbCZVbsxO47JWY8805u8cvNhNY4Js9b2k5x18NQXe7e-RoJd3ddchVFvt7J4XTfoht5yDaremCaW0Hn5b2LMTQClNkB28ExtmPdyuQ9VLckUf3OMFutet9bq20xpquv7rgRg8EWi_ZDBAd7YDPQ_ffccT_zzV2AhLhp0h6mVuaupaAkrcr10z54fqIWlF-snEGoCr9kQLcDlHCjJiPaMKhTXoOTYO4xjho5nDb3FJuA1hRjhsSQ4o31aHxha3kYjgnOGMMn-sBa45txQ9AYW0PDEflQ9jk_mfW4aex0t2Jlv0MoDIGSkB82XqVLl9DqTe7jcTa0awPP6A2lE,?data=QVyKqSPyGQwNvdoowNEPjdfjutIUN9PYZ2Zo821gi27GNpgaoSvbITFdPS8t0txTFVAOMwgDkFakGVSWYP7UB5jTkpVjEBE-n7Xt0wVeO1FqUfk4B4TGmWA46E6eq5XNheH6pFGT0nmL-iN9qwoFIrIUDDMR4cKa_h9pq9DnLNXHovGnendnRImrR2Hr4PFTXgIdKo8b0MZlmOLrw5rkrGm1WjF4QUrSHWqhYSR3EUN8F11iI7KDAX6p2vjx3BVOmIYuv7DEI_6T7rfOcHXVVULnYDt0LaMp&b64e=1&sign=e0880773562c202d69ac50b3de5a16db&keyno=1',
            urls: {
                492: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZov7eMkNsPWYi6Fucpb2U8fOUcGiiAhTg3K28B3B85ECl7GhNKMvUpSubyAtP63Ge8lghJlBMw5suxzMRMn9V7BG0oRVsBNY9-mcYDEvqBcIGXlVeXKx91jEnY6pEZJ-FGhzkVHbrWYZYAti004QtJI0Q8X1ADN2x_hg38DwFVrkyTf8_ao9SGhacp3IAQOQDDOkJlAd9AnbJIlpARUZc8zX6xLLi9QUmgGq2bL5KNpR0nTKJsjrHZNXu1Vk6amqx5jPQF5WA5vZdUbp25_PmQ2wRs4xbGA52VW1h2-pFsaHiKhFHQPS5kRouXRd0gM41C9b0QykEByC7wvi_MlNAEsgvDHY7o_cCseMvikO2YkivBzsJ6r6MCC5pquwXz_oRBGXvd3USX3BHnkpO8geQpW5IF_cAsUySOTc8FyrhM3VCkmEFZx_qjFPOMGwl3AqHTCigqx_Vc2HB1v5AAlPZC9nKH9Ihkd-FPTCpE4L8nj6ZgJ2bJa53MuBbFJ_u7JLRm5o7xa1z0Hf2jP4GCelqUQQD90DW8Dwn6XG9pIVKksBSQZgh3du0Rg08JPfwddLcZDcnmCBldCnTbSPtEDnct7x2ldmmrZiZrPYKovwpEyrI0juU5-AeobqrJ_axXil_HKKw3VBbCZVbsxO47JWY8805u8cvNhNY4Js9b2k5x18NQXe7e-RoJd3ddchVFvt7J4XTfoht5yDaremCaW0Hn5b2LMTQClNkB28ExtmPdyuQ9VLckUf3OMFutet9bq20xpquv7rgRg8EWi_ZDBAd7YDPQ_ffccT_zzV2AhLhp0h6mVuaupaAkrcr10z54fqIWlF-snEGoCr9kQLcDlHCjJiPaMKhTXoOTYO4xjho5nDb3FJuA1hRjhsSQ4o31aHxha3kYjgnOGMMn-sBa45txQ9AYW0PDEflQ9jk_mfW4aex0t2Jlv0MoDIGSkB82XqVLl9DqTe7jcTa0awPP6A2lE,?data=QVyKqSPyGQwNvdoowNEPjdfjutIUN9PYZ2Zo821gi27GNpgaoSvbITFdPS8t0txTFVAOMwgDkFakGVSWYP7UB5jTkpVjEBE-n7Xt0wVeO1FqUfk4B4TGmWA46E6eq5XNheH6pFGT0nmL-iN9qwoFIrIUDDMR4cKa_h9pq9DnLNXHovGnendnRImrR2Hr4PFTXgIdKo8b0MZlmOLrw5rkrGm1WjF4QUrSHWqhYSR3EUN8F11iI7KDAX6p2vjx3BVOmIYuv7DEI_6T7rfOcHXVVULnYDt0LaMp&b64e=1&sign=e0880773562c202d69ac50b3de5a16db&keyno=1',
            },
            directUrl: 'https://www.borsavita.ru/women/women-marina-creazioni/bag-marina-creazioni-3920',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRsWXKnRPZqS9SowcB0W9iVI1Cks8ipRnn6LZmoV2Tcj6ydB4amXOobuPfPA9eYyKTTUqXk0sRId_lASankW-S-HrATOTSOoHVpQQt6dMd8sTPq5k2RwRROBeNIxUVrDZFJuTJwqhzFfoonKNMM5eORJt2bt0hfF_F4RAhxg92zcFFRvcIZ8aLGdudivJ3KmuNGUSAuvdDIN2sKicTlrzWqJSXPiW7QfP5gibgcCF4yl5s0JTLkg8uabcebmhDfVNY7uRLlUvMv572WPJtaXGfypDotfEBiWJ9ePOpuP9qCzTD_nFlBhmHBH3bZat0srZW5d6lXe7Slu4iKJQt2_gs9dEX1D7hbv1m13DG30f5n24-NtrfrtzDYqxHs2goTan1wz120BbRAp9nw3kGCx8FGy0toqsSRgAfK-vS5Amcx1s3lXsj9Md3MiiCCoGoCo1zqRqNw6lVo9n2ftybb4IcrORBZn2YGPtpPaUA0ecpHKPmA0ELrh-2CrLRQORzxh4pHGd654c2zKu9dHvSNr-timy3IDZPLYwaf7hXO3yxLubCEhz-MElz7q_1apf6MJ-oopqCzLBFOSykYPF7K4hisn-mRTDU6WoF-hLVBNYwexarNhrZ_hnpLeU6pztbEFPvV7f-2I_jCzfIomGesoxd0fGlDhJASj_kSIxbpDm14idCyfQnZ1lLV43SgRbhOwoKFb1uQ_JqnCQGvFHzS1B6wc1H_3aiwE-XK8LVB5NQIs8Z8ZDunEYv6OGVP6NTcoH9g70kmOoDXPTk85QBx4qtty5gXzdgNgJErP29NyaNk72ARuS25QxxJSQiVbxfB1dh-Em6uF3pQUOLn49bREVG8IW9KQNouUIa0ovm08-ss-Vl8xtXEPoJUc8C7fy3ldbnWXwhDEOw8JVhxXViEIHRCMmw-WPncnlM8234eQSHlPQ7inXhvkZYljOdpzFZc0dg2o7rAypi4wRw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XRR_g-d99UPuUl5R9sxSsYM_rJb1TSSRN-0f863sPmQSToz-QU0rrmF58j7fOOohrSlXgxiqQtjuBGmDtPx1ADsKg4x1yQgWKqODIQ3orvAw5tUB0axRrk,&b64e=1&sign=b8339432c25a4ba2f1137c0796aa24a4&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRsWXKnRPZqS9SowcB0W9iVI1Cks8ipRnn6LZmoV2Tcj6ydB4amXOobuPfPA9eYyKTTUqXk0sRId_lASankW-S-HrATOTSOoHVpQQt6dMd8sTPq5k2RwRROBeNIxUVrDZFJuTJwqhzFfoonKNMM5eORJt2bt0hfF_F4RAhxg92zcFFRvcIZ8aLGdudivJ3KmuNGUSAuvdDIN2sKicTlrzWqJSXPiW7QfP5jm1WfgnQhjhXODn5ou4JKJaIqVT4zFLzhGRe4nMXYRgZoT9u8ORS0T1sIfzYYzwkFHzp-svkJMkuuxS2vpS9jh4PmbZ25MuSbt6VXbZm9ZFenWjQ05nLOCt3WN1gJ7qe9qjRSTvjZOtUgxrsTmGY7d6IJyOBfzlm9IWQTy8iezcYNMRwrNKq6IWX1aFfnRFJ4CkTpTrFK9pvfHtfxjYKDQX7b3nHQMAP1kVvSq5u7KPWMNJdIipOtburEUgKRO7uImcLnRjD70cX7drRN79KFsNrbwvIvGHuDjY7p6ZWcvPC239sXd5XmI2qeUU44sQAKOto05K2goOFI0v0utdUS4TlbcjnTbBFXekeP65rogsr6VMzAZTldTKLsTQiNnjVswxPz1POsaIQufkwedf7YkGrwEpL7OldZLiha7TPgAGJnYlfit0mS_7dWjCmJbsykdUoWzgIaO7wckvwujjyRVavqVG2mRNzOrUWwJYL6xFK-hUc5_s0VsljvNB0L30uXcZ-Pr8paQTvUvXo-g7CqLQUK21LdyGFwuuFShpAwf6PYs1Zt8xhaWYgsHdPyQ2V-QT8YB2EHNDjxIvxETrJVG5d4TI5PMezSjsm5xVJKSDMdFAT83EjCzFbqFSZZFWVRuHV2E2SWtxH-ZDRwfHZ4__0uXm1J5Nkr77y_5eTt1wB8ZcavvYWej-JWfTwRCfksStG62sbu8Jf3IOjvlOHEyhJz_1v4FlB2n4ITnwz_fMA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XRR_g-d99UPuUl5R9sxSsYM_rJb1TSSRC7FvUNchBIm4LB8kgK1RbholRk7g5p5qy8pNvmOUfaT5CMHxg8Ga8wW03UB9BLAW3s-q79460NQBmBPIhe9TSk,&b64e=1&sign=19f59775a2cc07a17260897226a85ef2&keyno=1',
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
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4,
                    count: 16,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 2,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 3,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 4,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 5,
                            count: 14,
                            percent: 100,
                        },
                    ],
                },
                id: 442204,
                name: 'Borsavita',
                domain: 'www.borsavita.ru',
                registered: '2017-10-18',
                type: 'DEFAULT',
                returnDeliveryAddress: 'Москва, Ленинская слобода, дом 19, БЦ Омега Плаза, 115280',
                opinionUrl: 'https://market.yandex.ru/shop--borsavita/442204/reviews?pp=492&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 470464732,
            },
            onStock: true,
            photo: {
                width: 600,
                height: 600,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1855678/market_QrDObw5MHN_lUeOdqbeTOQ/orig',
            },
            delivery: {
                price: {
                    value: '300',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 300 руб., возможен самовывоз',
                pickupOptions: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                        },
                        brief: 'Срок уточняйте при заказе • 1 пункт магазина',
                        outletCount: 1,
                    },
                ],
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '300',
                            },
                            daysFrom: 2,
                            daysTo: 2,
                        },
                        brief: '2&nbsp;дня',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            category: {
                id: 7812201,
                name: 'Сумки',
                fullName: 'Сумки',
                type: 'GURU',
                link:
                    'https://market.yandex.ru/catalog--sumki/7812201/list?hid=7812201&onstock=1&pp=492&clid=2210590&distr_type=4',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 8337445,
                name: 'Marina Creazioni',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/DglzSFfIBjnLxtpf9jwygQ?model_id=470464732&hid=7812201&pp=492&clid=2210590&distr_type=4&cpc=oopaWONKHegiPnUzh8PuS4heHi1KZLrvRnawmbJN9D4u1OjP4wrv6W89WRLGh0nGa2-IDtFEanavqusZlqnZFZW0VoyutG3vfd6y6AQNX3KhS9M0E2WPdaJG3TIK4vxKWd6JcWBWUFdFJ0ifdfoyd1v7WrHP3uKc&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1608,
                    SHOP_CTR: 0.006857142784,
                    NUMBER_OFFERS: 1,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_Trivial',
                        value: '1',
                    },
                ],
            },
            photos: [
                {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1855678/market_QrDObw5MHN_lUeOdqbeTOQ/orig',
                },
                {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1600927/market_s_PG7ZMyJIH3JaYWHk-GkA/orig',
                },
                {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1659832/market_8iAftVl0oN13QqnU93fBWA/orig',
                },
                {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1652547/market_3uLSbWxnrryuvkdRNXNyPA/orig',
                },
                {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1492460/market_pCaOilCSbNIRD_ABvduzDw/orig',
                },
            ],
        },
        {
            id: 'yDpJekrrgZG73qjRDgrtJrBiS4MB0dsaMEY8ohpCldFkDqI3tA5zlw',
            wareMd5: 'sMiHmPJKoEoN6LYDUmyFTA',
            skuType: 'market',
            name: 'Premium Goods Сумка из кожи морского ската',
            description:
                'Сумка из натуральной кожи морского ската премиум класса Страна производитель: Япония Внутренняя отделка: натуральная кожа Цвет изделия: серый с перламутровым отливом Плечевой ремень из натуральной кожи Размеры: Ширина: 24,5 см Высота: 18 см Толщина: 12 см Длина ручки: 9,5 см',
            price: {
                value: '18700',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZj_xoa-55fk0NfAVRlT0Xn_qjDipnqM5tIfbmw3f5Cfdfh8AQZyDi19FhwpEM8fcmy6QBvXW1MnqFkAEaIzIzAp2n6pJg5j2OcnY4bVg4s3zvhoD98TD981KhWsgMuZ4vuTcXlAOTSkxsfY3kWAOvo_RDoOzQpfkBZ5Ixq_Pnk8cFRLYDzwuJqC1oolGK9P_TfCPLcsGtBrfm2YXkVsSc3MB0SxjLoeLaRBefuKVhF56cqlyQMgElZOXTb5t9rjXOSEDTd-DmDOIm5sef1P4a2JrNKmVhOOOcx6GALJfL_IFpjJwjWpzDJuQufUO1OW2KR9ylmKM_rfUaDXRfxQj3CcLcUicJ-tzR6U1xmDCYjGxzYTPFYtFeb0-UIqOeySZB_N4bhAAuZL_xZstqdpuMMDADKfNMXHX-MCs5mDsA-yD3STCp-VsfJVpC_trnC5XjYSSrFgn2uCSOjDbb8fbqh0RCNzghudDnNOJ_XaJIRAh3J8V19TDgX0OnFchmwabnePER33dxmj0qeiLriI0BuA5esD9uiinQnjVlNEvruM5BfWhYJLRIxqbWW5OUa9c27F0KZdJD9AUNenhNl6lAVA8NpaXhjWnb-RGmqsPn59ocO3cYKwgX4n8Epr8-SdBRe8HB2ddAjsd2XzE5UPSfbxP9Qao6inotZ-CCpU2RdVCoqcWmIBRZvO7RtOHXKU837M51HbXnyt0VYOVzfBMDn7BYlU40juyufMDqOIXd_DadQ6TYcPw_5sQtWsEHvxIF2q3vJJdtClTy59SjuNnH4BxKwYbIbSMsubYWq49uWlWTow47zfEtez80Aaxr16YU7DoOhULgaUOGyUAV9pVTD0663WQ9Z-oATgH6oTd-CUMzHeaOey5W27ECr79uhowmPuCwEHRNgAju7p4GtGfWGP25GytcEhy9Xn0KsSFv8-sX3JC-qmRYfv5YHE1S1yo5NO2S8mJ1SLEknnYXOzH2iM,?data=QVyKqSPyGQwwaFPWqjjgNmhlC19WF9L10H-3pJnUUOtJpGBzhc3hFLKyAD7Vqh_Np13PHeAWl7E6wY5A3GlqSKhc9MjvjXElFCV6J6Nv9Ba90yRthBMzQOPMzQyQIH0XzVBD1Ze7uWB0ChMpxVpbIXwO_dPWITzpk87-DSK9p51nM29aXLpsEHGfzHY6lWJi6yyoQT35pYPM7DEDt3RE1cr_LuS5lpEAgkWNjGMYw9GCiEIYVevRhuk7TY7CHbBzT9fXh7mob0g,&b64e=1&sign=a4caedd31609ed55509b81bdeed3739f&keyno=1',
            urls: {
                492: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZj_xoa-55fk0NfAVRlT0Xn_qjDipnqM5tIfbmw3f5Cfdfh8AQZyDi19FhwpEM8fcmy6QBvXW1MnqFkAEaIzIzAp2n6pJg5j2OcnY4bVg4s3zvhoD98TD981KhWsgMuZ4vuTcXlAOTSkxsfY3kWAOvo_RDoOzQpfkBZ5Ixq_Pnk8cFRLYDzwuJqC1oolGK9P_TfCPLcsGtBrfm2YXkVsSc3MB0SxjLoeLaRBefuKVhF56cqlyQMgElZOXTb5t9rjXOSEDTd-DmDOIm5sef1P4a2JrNKmVhOOOcx6GALJfL_IFpjJwjWpzDJuQufUO1OW2KR9ylmKM_rfUaDXRfxQj3CcLcUicJ-tzR6U1xmDCYjGxzYTPFYtFeb0-UIqOeySZB_N4bhAAuZL_xZstqdpuMMDADKfNMXHX-MCs5mDsA-yD3STCp-VsfJVpC_trnC5XjYSSrFgn2uCSOjDbb8fbqh0RCNzghudDnNOJ_XaJIRAh3J8V19TDgX0OnFchmwabnePER33dxmj0qeiLriI0BuA5esD9uiinQnjVlNEvruM5BfWhYJLRIxqbWW5OUa9c27F0KZdJD9AUNenhNl6lAVA8NpaXhjWnb-RGmqsPn59ocO3cYKwgX4n8Epr8-SdBRe8HB2ddAjsd2XzE5UPSfbxP9Qao6inotZ-CCpU2RdVCoqcWmIBRZvO7RtOHXKU837M51HbXnyt0VYOVzfBMDn7BYlU40juyufMDqOIXd_DadQ6TYcPw_5sQtWsEHvxIF2q3vJJdtClTy59SjuNnH4BxKwYbIbSMsubYWq49uWlWTow47zfEtez80Aaxr16YU7DoOhULgaUOGyUAV9pVTD0663WQ9Z-oATgH6oTd-CUMzHeaOey5W27ECr79uhowmPuCwEHRNgAju7p4GtGfWGP25GytcEhy9Xn0KsSFv8-sX3JC-qmRYfv5YHE1S1yo5NO2S8mJ1SLEknnYXOzH2iM,?data=QVyKqSPyGQwwaFPWqjjgNmhlC19WF9L10H-3pJnUUOtJpGBzhc3hFLKyAD7Vqh_Np13PHeAWl7E6wY5A3GlqSKhc9MjvjXElFCV6J6Nv9Ba90yRthBMzQOPMzQyQIH0XzVBD1Ze7uWB0ChMpxVpbIXwO_dPWITzpk87-DSK9p51nM29aXLpsEHGfzHY6lWJi6yyoQT35pYPM7DEDt3RE1cr_LuS5lpEAgkWNjGMYw9GCiEIYVevRhuk7TY7CHbBzT9fXh7mob0g,&b64e=1&sign=a4caedd31609ed55509b81bdeed3739f&keyno=1',
            },
            directUrl: 'http://pgsale.ru/products/3032-sumka-iz-kozhi-morskogo-skata-/',
            shop: {
                region: {
                    id: 2,
                    name: 'Санкт-Петербург',
                    type: 'CITY',
                    childCount: 18,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 3.8,
                    count: 184,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 2,
                            percent: 2,
                        },
                        {
                            value: 2,
                            count: 1,
                            percent: 1,
                        },
                        {
                            value: 3,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 4,
                            count: 1,
                            percent: 1,
                        },
                        {
                            value: 5,
                            count: 129,
                            percent: 97,
                        },
                    ],
                },
                id: 418904,
                name: 'Premium Goods',
                domain: 'pgsale.ru',
                registered: '2017-05-16',
                type: 'DEFAULT',
                returnDeliveryAddress: 'Санкт-Петербург, Ораниенбаумская, дом 27, 197110',
                opinionUrl:
                    'https://market.yandex.ru/shop--premium-goods/418904/reviews?pp=492&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 444889198,
            },
            onStock: true,
            phone: {
                number: '+7 921 776-63-81',
                sanitized: '+79217766381',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZj_xoa-55fk0NfAVRlT0Xn_qjDipnqM5tIfbmw3f5Cfdfh8AQZyDi19FhwpEM8fcmy6QBvXW1MnqFkAEaIzIzAp2n6pJg5j2OcnY4bVg4s3zvhoD98TD981KhWsgMuZ4vuTcXlAOTSkxsfY3kWAOvo_RDoOzQpfkBZ5Ixq_Pnk8cFRLYDzwuJqC1oolGK9P_TfCPLcsGtBrfm2YXkVsSc3MjXhEQTyDbGOf6yf3a8y4r3rg642Dcv06XN-0M4IeNCoGPfqsmdXVRYhK6LjbGpMLJPwBGwv38LDVNqdK4jRy9rtRBGyy6LU0fFTopR9KZVx2qAEhUfT_HJ8VwuCtPAr4mPmO-k3M5efIs8K9whkHTb7I8e28cI854_dFxe8qF6ecx6KqUMVcT-vhEOyKO_Oz-cJn-TxxTI0h_YBkzHlxGCccHb5UWC0TxF1fOd5YQA3KxDC2W-zTf-L2OT5fZ_1dqJNkXqYaVEZURx77toBL2Lu_eoNhw84llNlK2eHpDIrsjcjzcODXB-g6CX0tjD88jxUKbCgV6M1Zd7_ml_dB5UJpTh5vFgH0iRnGC0j7k-5eLcyl3HJMT1jJJaEhAGVBG-ay2ziMuYjG6ScSKup10hQRZG_HsEHc-OD8RIWBD-_APudDMvx-LcvfHq_9Ke7LgRsfdN9m14WDm5tS05NkkHevpx56nK7MPcM3kLZHWRKNMpY9YRYehkvph97UmdW4sqwAaOqv22BSimELNKc38Pd4xwE9w9s-PUT3G8dUlVJdF95pg-AVrHYxBIkxL8tx8fA7BgZRHruiXBsecrvoDGvHewkl3RLvNE0sB7u29sHLgvDW9uQejiyZPgbcxcKDC3qi1G3LchoGW3IMh1I2ZvPuh7mZ53ayX2k8QhLUYO5tBUKI76P0OxkCrbwf3Si-YwsbV0zJKtDtG9j7aYolhNe0kBTCDSmAClwuvFH2r8qLJH2ymK3GQo2W9K-epB_Q,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-qcJigylEcsWsQ3J45uMlk3Ei-cNrMfiVibjUdNeSCP3lgqggioI3z5OmOQGRskqY7DvvbYjIvxL371xK75brVChpFxTcTttjaEAWoj8XHxR6iA5bT3fUgMICe2afTGQkV9TwcN7Tm1d0F6v-c71XXT1jFe79K_v44Edmuuyz-0A,,&b64e=1&sign=6a426cff1f12d14922a459d485734df5&keyno=1',
            },
            photo: {
                width: 1700,
                height: 1521,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1662088/market_i20JRBVS6uCzRv7PLAzhvA/orig',
            },
            delivery: {
                price: {
                    value: '0',
                },
                free: true,
                deliveryIncluded: false,
                carried: true,
                pickup: false,
                downloadable: false,
                localStore: false,
                localDelivery: false,
                shopRegion: {
                    id: 2,
                    name: 'Санкт-Петербург',
                    type: 'CITY',
                    childCount: 18,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Санкт-Петербург',
                    nameGenitive: 'Санкт-Петербурга',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву из Санкт-Петербурга',
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 2,
                            daysTo: 2,
                        },
                        brief: '2&nbsp;дня',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            category: {
                id: 7812201,
                name: 'Сумки',
                fullName: 'Сумки',
                type: 'GURU',
                link:
                    'https://market.yandex.ru/catalog--sumki/7812201/list?hid=7812201&onstock=1&pp=492&clid=2210590&distr_type=4',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 15043096,
                name: 'Premium Goods',
                site: 'https://pgsale.ru',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/sMiHmPJKoEoN6LYDUmyFTA?model_id=444889198&hid=7812201&pp=492&clid=2210590&distr_type=4&cpc=IA8a5QAMBGl3aECKh3Qdq-41qJSPM0ZUmtTW1Vz3iJ-cCqTO21Xg9lzii9jBe11R5s-72AEwOSqsfoygLOFyiPcjcRsD7JdSMZszg2J6r-Pc7nI3P_0fvSlhJKS4sd2D_MGU6Zkd5eeTBvhkVijxxGQXIfgtrNSp&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1608,
                    SHOP_CTR: 0.006535947789,
                    NUMBER_OFFERS: 1,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_Trivial',
                        value: '1',
                    },
                ],
            },
            photos: [
                {
                    width: 1700,
                    height: 1521,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1662088/market_i20JRBVS6uCzRv7PLAzhvA/orig',
                },
                {
                    width: 1500,
                    height: 1322,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1384246/market_uGR0LDCtdngAoDVDZEdW6A/orig',
                },
                {
                    width: 1600,
                    height: 1351,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1864559/market_7yeG4ZPa_Bki9DvDddj8GA/orig',
                },
                {
                    width: 1700,
                    height: 1383,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/172627/market_Hjq624htQLc5KVTDYFHnZA/orig',
                },
                {
                    width: 1500,
                    height: 1054,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1533973/market_O4liTgA7t8T7Jc3tWkiDWQ/orig',
                },
                {
                    width: 1500,
                    height: 1000,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1588061/market_TUQ6QWRglkjzme_c01j3Fw/orig',
                },
                {
                    width: 1500,
                    height: 1000,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1059059/market_2EaYxNkP9ZdiEI6JV2VUqA/orig',
                },
                {
                    width: 1500,
                    height: 1000,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1573229/market_yz0sebXwLq0assRX1foAwQ/orig',
                },
                {
                    width: 1500,
                    height: 1500,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1769719/market_qpZu1G8VN7igcHg1dMG-dA/orig',
                },
            ],
        },
        {
            id: 'yDpJekrrgZHlMOSPOqMt_Z8XDBC75rmIZC62F_O7gZkN3hxgbxnOKA',
            wareMd5: 'q6WXuA2Jbxt_hjE7d67nQQ',
            skuType: 'market',
            name: 'Premium Goods Сумка из кожи морского ската',
            description:
                'Сумка женская из натуральной кожи морского ската Ручная работа Страна производитель: Таиланд Цвет изделия: черный Внутренняя отделка: эко-замша Плечевой ремень из натуральной кожи Размер: Ширина: 22,5 см Высота: 14,5 см Толщина: 12 см Длина ручки: 11 см',
            price: {
                value: '9900',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-O3pIYj7W7R4Dq4dsa9_A3tCMhFFY_EZ38_MdE6-sE6XbZ6V9FmCkXHNJPChQu7TTxp3fiHUZC-6WnlFHSW9KNF1VgN5jBxn2txGXDIt3gkAogUMoVEDywa7OXihGDgxSQET5HYRYXfqPelApNaS4pnvlzLADWiKoEkh81QPayuO4KXqF0c3UHiGHuSPpdqa8dJAWsAMyIU8fwtiaEcYo2Tedxm2sUuGd4NSs9FFs72ypnXeVCNZ9hgs_SgO8WH3r-NAW4n9HZ-tal4K1-6X82KwfUPisilIvknCJQpIVSb0oYSlGZTnOIer7hM0DVAUsRGw9Py-XWF1-c6v7ytJqGK2GHOeQAtTrVpyPBAQOFwCQRNMIsj63ZudD0G3I-vgB8w2Plpg6xl4PvsOkUS_Ae3PjSYik1LvkGyMkJsfQYAbJpmj20b8VdsyEObs0VXiJMx4DkDgwHfSSZgPJzGMNa14CJhLrBTiy8NdamJFwjJQius_nYpKCk_d4rS31Nxa6A0J48HbOBAAIZ8yJszstQ9T--jwGySu9WJ22_6Yu7v4AMNO6mHph9cTJEnhSoKhbqrea3wCuO0CCG7gFeOyXHim8Kuu-045-KNDCCLbj4PB4PAPct2ElOu37C_mKUeNaYS8s5x0CKCHHVgGu8WDAYRdmOJIFe1VN5R8-asqZtXnQv4tWtxpDH4q0wYDvu7oEGnD59M062535pmx7Z5szMxfJH3eIMktA9q7YWDRn7XdNhjKb1db16wcq0v7zDrl6SqbBNmfwtXq?data=QVyKqSPyGQwwaFPWqjjgNmhlC19WF9L10H-3pJnUUOtJpGBzhc3hFMA1KwXqTzXFBza_z6LqBbpm6_RZZe0AKLxvkS7i1y3D7SPmwcgJ_XE1t-Eh9N1BsdjU9CXHQqNKNs1aKp6uzB3Mxz6I51asX4dKd0Kok2W9zke8VkSP0v6RroRgAbBA1g_zGoO_cJ2hVq82ZCAUB8WjhMHp0kCP3yBoAgT3pW5t_3PVGK76WbVP5isg3fnq5DZmuHruMEJH-eR8zj0MUT8,&b64e=1&sign=1080b3e3a4b5752b151ead987d177ff3&keyno=1',
            urls: {
                492: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-O3pIYj7W7R4Dq4dsa9_A3tCMhFFY_EZ38_MdE6-sE6XbZ6V9FmCkXHNJPChQu7TTxp3fiHUZC-6WnlFHSW9KNF1VgN5jBxn2txGXDIt3gkAogUMoVEDywa7OXihGDgxSQET5HYRYXfqPelApNaS4pnvlzLADWiKoEkh81QPayuO4KXqF0c3UHiGHuSPpdqa8dJAWsAMyIU8fwtiaEcYo2Tedxm2sUuGd4NSs9FFs72ypnXeVCNZ9hgs_SgO8WH3r-NAW4n9HZ-tal4K1-6X82KwfUPisilIvknCJQpIVSb0oYSlGZTnOIer7hM0DVAUsRGw9Py-XWF1-c6v7ytJqGK2GHOeQAtTrVpyPBAQOFwCQRNMIsj63ZudD0G3I-vgB8w2Plpg6xl4PvsOkUS_Ae3PjSYik1LvkGyMkJsfQYAbJpmj20b8VdsyEObs0VXiJMx4DkDgwHfSSZgPJzGMNa14CJhLrBTiy8NdamJFwjJQius_nYpKCk_d4rS31Nxa6A0J48HbOBAAIZ8yJszstQ9T--jwGySu9WJ22_6Yu7v4AMNO6mHph9cTJEnhSoKhbqrea3wCuO0CCG7gFeOyXHim8Kuu-045-KNDCCLbj4PB4PAPct2ElOu37C_mKUeNaYS8s5x0CKCHHVgGu8WDAYRdmOJIFe1VN5R8-asqZtXnQv4tWtxpDH4q0wYDvu7oEGnD59M062535pmx7Z5szMxfJH3eIMktA9q7YWDRn7XdNhjKb1db16wcq0v7zDrl6SqbBNmfwtXq?data=QVyKqSPyGQwwaFPWqjjgNmhlC19WF9L10H-3pJnUUOtJpGBzhc3hFMA1KwXqTzXFBza_z6LqBbpm6_RZZe0AKLxvkS7i1y3D7SPmwcgJ_XE1t-Eh9N1BsdjU9CXHQqNKNs1aKp6uzB3Mxz6I51asX4dKd0Kok2W9zke8VkSP0v6RroRgAbBA1g_zGoO_cJ2hVq82ZCAUB8WjhMHp0kCP3yBoAgT3pW5t_3PVGK76WbVP5isg3fnq5DZmuHruMEJH-eR8zj0MUT8,&b64e=1&sign=1080b3e3a4b5752b151ead987d177ff3&keyno=1',
            },
            directUrl: 'http://pgsale.ru/products/2880-sumka-iz-kozhi-morskogo-skata/',
            shop: {
                region: {
                    id: 2,
                    name: 'Санкт-Петербург',
                    type: 'CITY',
                    childCount: 18,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 3.8,
                    count: 184,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 2,
                            percent: 2,
                        },
                        {
                            value: 2,
                            count: 1,
                            percent: 1,
                        },
                        {
                            value: 3,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 4,
                            count: 1,
                            percent: 1,
                        },
                        {
                            value: 5,
                            count: 129,
                            percent: 97,
                        },
                    ],
                },
                id: 418904,
                name: 'Premium Goods',
                domain: 'pgsale.ru',
                registered: '2017-05-16',
                type: 'DEFAULT',
                returnDeliveryAddress: 'Санкт-Петербург, Ораниенбаумская, дом 27, 197110',
                opinionUrl:
                    'https://market.yandex.ru/shop--premium-goods/418904/reviews?pp=492&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 296676829,
            },
            onStock: true,
            phone: {
                number: '+7 921 776-63-81',
                sanitized: '+79217766381',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-O3pIYj7W7R4Dq4dsa9_A3s8FbO0e1DIm4ABziF7ulZusflIRpmsyBmtxdEOzlJNS1q-qn8ejmNtxFVwA6vxUhyb-YHHh33SLk78QaNEz5YohQusPpADwirDiRw5pPVIE6-zpqr-Fj39vjHTQE_LGH0z6dt9BeYF5gC4hH6K25pxgbbhGu4EyGeECS7lZm3_JtOPD4YT2m8fF0Ya-bNqLQQkPuZMUovN4q6TcPnB633gaXjNe1kr-gQjD-rOmOHFoVO1ScQPQtVy3d1Ch7MBeVP9V0gQeO3Yg8imVmrXEghlhaNtBNWgN-S4dS_2FlgDjsCGdvhUF1ugxAfqm0Vc1rH7MpjuH9lVMc5FWkdleXNdj1QRf_P7gOq5bJDfqlNy6SMlHuWhAmCcj4qsKpMiZ5lgQbHPJIm3A6sXictMQnxM8-fObkb4Wub9-373MuttvDBlNPbspf2iOLGlpF47vwjfULlcBul5w4FhEHIMu9dGR0hAkttJq3vCLN28WbGzIlkY4X_HvqB7a5KvhdaG9fopj7EjVXizGddASIOKUR2L8VMF3RmQ4C6KSexd3KzOJpRzcAGsDu1epeZnsdvsJxAbuwcqm82bZVoS1hQ1Z5c2w1xvipd99RGhvJ6ZJU_fjZrtNIRrVSHWWRVS0q3bE9nuh_DDp_NRfZ2pou2kMN-yrazozJv3eABHBe0eUrMrQT-euTanMRagqAzjYVGaHoX2fwUjOLcW_iq2Leh8bwYbl16u0N0tJARp9optikUytJX_XnEe9iq3?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9tUGb7w9G-hOXufw56frAz7O6MlWw-CxFB_fDNaTbOP5QFF34HOjxvTtLixbVayWWig91-YKvupYZs1NaVgESvkUMgt2SUjKM9q_71K_pKdUPU0zwRWoLlPIOL9VWQVQm182ZV-GUgtxK6tvBCs-6ANw9oLyNvYOgn8qdrvoWwHA,,&b64e=1&sign=e867a7ac7a7990af4bfdd6b4d7fec8a9&keyno=1',
            },
            photo: {
                width: 1100,
                height: 872,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1587799/market_kHdvZeZt0-nvfn8lGZ4MYA/orig',
            },
            delivery: {
                price: {
                    value: '0',
                },
                free: true,
                deliveryIncluded: false,
                carried: true,
                pickup: false,
                downloadable: false,
                localStore: false,
                localDelivery: false,
                shopRegion: {
                    id: 2,
                    name: 'Санкт-Петербург',
                    type: 'CITY',
                    childCount: 18,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Санкт-Петербург',
                    nameGenitive: 'Санкт-Петербурга',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву из Санкт-Петербурга',
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 2,
                            daysTo: 2,
                        },
                        brief: '2&nbsp;дня',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            category: {
                id: 7812201,
                name: 'Сумки',
                fullName: 'Сумки',
                type: 'GURU',
                link:
                    'https://market.yandex.ru/catalog--sumki/7812201/list?hid=7812201&onstock=1&pp=492&clid=2210590&distr_type=4',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 15043096,
                name: 'Premium Goods',
                site: 'https://pgsale.ru',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/q6WXuA2Jbxt_hjE7d67nQQ?model_id=296676829&hid=7812201&pp=492&clid=2210590&distr_type=4&cpc=OKGXw_tnfhzvlwUJGSjU0fyGAkXuOH6E1hz2k6jAxkvF_ItO7UrF68quSTGIFYV2hte_ay7s8DWJQ4WXUqT6YKitMrRZghRiF2kfAXanBYY2KSJ131G_U2-z9TrXZQvZXMf2QPl4YN6OZ1KVuqtV1Nu7xAtOGBz6&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1608,
                    SHOP_CTR: 0.006535947789,
                    NUMBER_OFFERS: 2,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_Trivial',
                        value: '1',
                    },
                ],
            },
            photos: [
                {
                    width: 1100,
                    height: 872,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1587799/market_kHdvZeZt0-nvfn8lGZ4MYA/orig',
                },
                {
                    width: 1000,
                    height: 794,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1670364/market_0tnaSgFrxR0lNB5HxqtCUw/orig',
                },
                {
                    width: 1000,
                    height: 667,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1060124/market_MQR0FbVyIFnxT26XfunaLQ/orig',
                },
                {
                    width: 1000,
                    height: 717,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/363663/market_MkLhy6DHLb0cNCreQFvMkg/orig',
                },
                {
                    width: 1000,
                    height: 667,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1659832/market_tY3czfNqOSskDUpAZYv8Dw/orig',
                },
                {
                    width: 1000,
                    height: 667,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1672187/market_zgHHP0yPUO300ZETsJJP6A/orig',
                },
                {
                    width: 1000,
                    height: 667,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1703792/market_HiruZnyt09MOqhffhAI8Mw/orig',
                },
                {
                    width: 1000,
                    height: 667,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1769565/market_IlzdB3--75mkgEllD8oJcQ/orig',
                },
                {
                    width: 1000,
                    height: 1000,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1060124/market_6kS5zEjgHiaosz0cd0bzPg/orig',
                },
            ],
        },
        {
            id: 'yDpJekrrgZFeGHR63ICRVtPAcS13guwEB8BRDIClvLmj0Rmx3BbbcQ',
            wareMd5: 'm01JQXVX70bb8IlLGwXVMQ',
            skuType: 'market',
            name: 'Сумка Palio MPL 11182 черный',
            description: 'Длина ручек 12 см, наплечного ремня есть. Внутри 3 отдела и кармашки. Закрывается на молнию.',
            price: {
                value: '6790',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjl__bphPujMcqAmNFlMeVAQmOCeka7vdNramtc1h8QIPBXSb_S4ijGxTX7h6zvNzhvYags5bOyQh7aBrPFmwzbLLyKmdzaJ5S0pVHaLLA4iwodAZAGbo_6mgqhqJR5_q9Zv6oKbOA-O1OMFMblwKeBbuXsoMlAkIHbeSOPSAsBhoKgJTMwrCA-3UHuTTZNe8lXCCiBbaa15HnTVY7-89MO8T49shwBYlsrfAukP_1_MR019TEJTvs8nGhLkL8VappL8Qr1mpVVOhUmR8R3uaNrDDX_eDUdE31lHJMuIXpKosw2pWpKW3N4ygOfzlgOT1e6kg3tx4RT0wDKRXBtriaW0CdyXnyXZ6k5ebMkT9CZ5qwp9kf4kEmNcaZHUxD3g8eYDbxO899lg54wkvu7L5BLfOrxgZE-H4EA2jZLTleFIffwXvoClfeor3UPDH-_F3VaZiqkh2pQjzk938ETKyuhHypoxApYNx8vis7hqSh4oA-8AiBsedF3xEIBIV7aeckLodrG6dbCKCgPWle63_MJv2xa2ypvKI4sb_4WzEFjAnLdIHhz1nz6fO_r-v9KTiOPHE61gVNJ57G_LoxR7xonSZBDgvpEkxU3zX0Th9M1_bpOR5XMtEouO4UlwgdY5zReLRBhCaI4GpJVG5R_7Q8a-xBlo4Bl1e-edHXcIvihUZYxwI0t29J6MTsne9DHkjdmcmekY_XZUVL7mSlzhqKSlyC1yr73FQkWLBQaiEMhq5P_fj8VLMj5DAawgw63KPd_STZXpUVb3QddrFA6haQls5hDEwSvzhwrO77uofi3mZqSlmYzLGv7qg_znK6EYoiQaZo2EvmCwlOcIpbyb5yY8BhEk0EMfiJ42-Sg16P8cWZkwx4rDEJjum4RYffwCZ6Y0XA4vLUhA6oF9V89kgE3GYzKCefl7lzznO84QR_TXUCh3ot49d-IlRPgeyal4Ullw5fhMSn0p0dtmkWlm7_4,?data=QVyKqSPyGQwNvdoowNEPjYcA4Bk7rPDi8lUvfS7ikngijEjgIxvtzsAU8mVYYlDZ2poq8av9cZNbQ-b_oEMjrUZy926od9AU1UuIORmlYH9_GwlUbq2F5LD1kzSEbLJZa7jFD2ssriplJvNLh1zycjNJ2DjDdY9ggWGIMcz8B-_hf87e_2_FgD7MvFQBKn023b_jK98LZj9_RvBEWOTYkV5eYZeoG54QutWQT5c5HVYakpSC_m9wUk0RK2LYdpQvYHiyAaUD1qU8aGWOB6eedBoysK_k5asQUU4PsVrL09VMLl64Ig1-bM6fWerRMFbUhXXlUQQg-yVXR2O_6tac-zrodCBquGUl9p7SK_zQaGIUmJbZIgYpIqy-hxB3cfWcYVoFVo2TFVFNFlKUUXZ2fj4hYLjwqjis1uSumFgavousyWhGcafqFzH4c6blZ38bxo6_GTbjN2KGRIuPqyBG4m9GD9qxquShnY-b_f2YqoEMYO6sRAw6J_S0nAEAqfTN1vFmai7isjE,&b64e=1&sign=71ce8d81b7f1d9b6b062c67622eb7f10&keyno=1',
            urls: {
                492: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjl__bphPujMcqAmNFlMeVAQmOCeka7vdNramtc1h8QIPBXSb_S4ijGxTX7h6zvNzhvYags5bOyQh7aBrPFmwzbLLyKmdzaJ5S0pVHaLLA4iwodAZAGbo_6mgqhqJR5_q9Zv6oKbOA-O1OMFMblwKeBbuXsoMlAkIHbeSOPSAsBhoKgJTMwrCA-3UHuTTZNe8lXCCiBbaa15HnTVY7-89MO8T49shwBYlsrfAukP_1_MR019TEJTvs8nGhLkL8VappL8Qr1mpVVOhUmR8R3uaNrDDX_eDUdE31lHJMuIXpKosw2pWpKW3N4ygOfzlgOT1e6kg3tx4RT0wDKRXBtriaW0CdyXnyXZ6k5ebMkT9CZ5qwp9kf4kEmNcaZHUxD3g8eYDbxO899lg54wkvu7L5BLfOrxgZE-H4EA2jZLTleFIffwXvoClfeor3UPDH-_F3VaZiqkh2pQjzk938ETKyuhHypoxApYNx8vis7hqSh4oA-8AiBsedF3xEIBIV7aeckLodrG6dbCKCgPWle63_MJv2xa2ypvKI4sb_4WzEFjAnLdIHhz1nz6fO_r-v9KTiOPHE61gVNJ57G_LoxR7xonSZBDgvpEkxU3zX0Th9M1_bpOR5XMtEouO4UlwgdY5zReLRBhCaI4GpJVG5R_7Q8a-xBlo4Bl1e-edHXcIvihUZYxwI0t29J6MTsne9DHkjdmcmekY_XZUVL7mSlzhqKSlyC1yr73FQkWLBQaiEMhq5P_fj8VLMj5DAawgw63KPd_STZXpUVb3QddrFA6haQls5hDEwSvzhwrO77uofi3mZqSlmYzLGv7qg_znK6EYoiQaZo2EvmCwlOcIpbyb5yY8BhEk0EMfiJ42-Sg16P8cWZkwx4rDEJjum4RYffwCZ6Y0XA4vLUhA6oF9V89kgE3GYzKCefl7lzznO84QR_TXUCh3ot49d-IlRPgeyal4Ullw5fhMSn0p0dtmkWlm7_4,?data=QVyKqSPyGQwNvdoowNEPjYcA4Bk7rPDi8lUvfS7ikngijEjgIxvtzsAU8mVYYlDZ2poq8av9cZNbQ-b_oEMjrUZy926od9AU1UuIORmlYH9_GwlUbq2F5LD1kzSEbLJZa7jFD2ssriplJvNLh1zycjNJ2DjDdY9ggWGIMcz8B-_hf87e_2_FgD7MvFQBKn023b_jK98LZj9_RvBEWOTYkV5eYZeoG54QutWQT5c5HVYakpSC_m9wUk0RK2LYdpQvYHiyAaUD1qU8aGWOB6eedBoysK_k5asQUU4PsVrL09VMLl64Ig1-bM6fWerRMFbUhXXlUQQg-yVXR2O_6tac-zrodCBquGUl9p7SK_zQaGIUmJbZIgYpIqy-hxB3cfWcYVoFVo2TFVFNFlKUUXZ2fj4hYLjwqjis1uSumFgavousyWhGcafqFzH4c6blZ38bxo6_GTbjN2KGRIuPqyBG4m9GD9qxquShnY-b_f2YqoEMYO6sRAw6J_S0nAEAqfTN1vFmai7isjE,&b64e=1&sign=71ce8d81b7f1d9b6b062c67622eb7f10&keyno=1',
            },
            directUrl:
                'https://www.kupivip.ru/product/w18101657869/palio-sumka?reff=ymarket_msk&utm_source=ymarket_cpc&utm_medium=partner_ban&utm_campaign=ymarket_cpc_msk&utm_content=ymarket_msk&from=ya_msk&partner=ymarket_msk&utm_term=market',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRss-dtxIkN93Q95zjXbZyvIqjSHtfebD8arwB0q3hoSmRVdKvKtACf3JDBnlMenTVoTtmrO-zcS9eK6Cqf47OscT-bCAAY-Dcv2gw_ycsEzoeHDhMWyQB2Tbdxsz-Zu7BpuzHl7wrNyRvdN2f2TCRglesCRmQtDkegFsR46tmJI1MswFOnBLMIWzpXwv5NMTgAT_vtelWg1JfbqPMsjnQ9CmNvmPlNrdF6lMFbwpxT_mu8n2cV6HeYz3WSg1kQ2ln8MXhSFrK3UO3P5KrYw6AWcHL-_BFeSmONAR0bGr444RUFzaJt_4ByGyqXl2cjNqgJ864ZLd1omcD_W2OodmERijF9eTuXBv7YvlFSMo7zLbDEEMjeLoh2zs-DQkSHtLubsbWEJKy0NXe4gMtGYIhPbR4h2emo4Zwjj_-kUS7Y-IaUraGAk_u3kwZ5fNbZoW7rpPOKRUxTuzGWUc7FpH7ErFx-nrj44Ci_ennbJ-8QoeyXNabC4elIrU1-kxM47SaV9TozWmWHnq1hPK1zQhZS_t451T8UoTLU58cbdF230BywG1dIWbtGpFwYVt6-kjFWqrNiwJeqzuGa2Ez1EaNbCDD2m5Rq8mrrDFfZqDBx5IdthOkjAv0MK3fTQpSlWgVKT9Fjy8N7R33lWUyCEKhywkFWd7Pahd-yCSAJqmlujr96PgGAvg0NyFB2v9aTiYl0i5mhoIq7wyyVT8op2egXOo9yrpq018Cp-ujO4njz8GemGii3U5X6ynvRozL9L3yKMRchDsS5QwKZhIe5S2_pUtiytf7cQfmdoK1fk4B0MXzfioNHRfcSRJYKDBC7ZHYFVvJfUA93B-1vyLx5WWz_IV2Zwk4uZ8n86W-HlPIB7e3gJeNp6mpdTL1rAPGwjMY0p3X4z3QmmWgosEppgmzQlsxkXT1JzWtkr8xUEYeHIOxWYgsbwvXAvsYvjBbV6XW6HmCtKp8ZJXPag4m5gOcjV?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2QfG-0UIlgwZuWhPMLly9pjB_Xi6B_bh1Q-LmG4UMjDH3jXp39HCnDdCb8ciJhhaxdcMxPU7NN_l_Udl6URmPBsTZ-rEAfEj_jyPwIKoZETCBRsG27xE60Y,&b64e=1&sign=af493c5333c7638d8110dcd1bedc5b2c&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRss-dtxIkN93Q95zjXbZyvIqjSHtfebD8arwB0q3hoSmRVdKvKtACf3JDBnlMenTVoTtmrO-zcS9eK6Cqf47OscT-bCAAY-Dcv2gw_ycsEzoeHDhMWyQB2Tbdxsz-Zu7BpuzHl7wrNyRvdN2f2TCRglesCRmQtDkegFsR46tmJI1MswFOnBLMIWzpXwv5NMTgAT_vtelWg1JfbqPMsjnQ9CmNvmPlNrdF51Sv_8FCRLFXBgNh9ClspAVurWXyIlmwGY044LEMK2ENUjO6_dQpxpZzcr_EA4IUsrddCy1N-jP0KaG2ZYoi2ZA4hxaRJRkMe8F7oXEO5zZokOV5x6GMBDiL3P3kRfAJfDWv7Sb4znx3dE_G7g_TE3xWjlbvMVmfqdDj09wsPSpC2JX6QSZv4fLxSBUwERAkzyTvO1UAB1FpD3Cawz68pXGqkk9HEatVb_hDJJiA2PrpWtkobVnLKiBVE1JP8YL4qbPFtsLNGOUmh8WOrNi9ggSuFMOovSuwO5cX2sKIQGqqxobenRozsg18XWi9AuN_I0vHFvSW-9ppuBlakQmemipABJQfufLAFHB9wWXITE-Ak5wJzDVuH2o3kw8oOdVtyM3YFGFk48SmOWSr82ulBP2Z6d25JgckcRyGiI_huMeQxShuFz0vcsZM7fv__fmhYzygE3Xi8gwNqCShg-8f7WLLfAvl4jN6U20Vid7o3hAFdOi0oUryuwi2VOhF2pBFZvk_Yfew65FyyHxg6l64BZhDpiPGGANey5rKx1t4yEDYewh-rODr645FaAtjl86hW-uKn_AYeatwT5y6WX8svTVjlPdCw3nAvodpJBPk6oTvuQsg8etOJz2rgB7KRmtYXIylQsYPkPPxespuUWt8AGpQhmKwchlJ0BfitXSTWG5Icr8-JlA3Phdb-wdi3pccOdWlgzJ5uhk_8EkhUXLEK8rK9_M_F4hM3Pyv_m5VlzXFV-QsmR4fLH?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2QfG-0UIlgwZuWhPMLly9pjB_Xi6B_bh1X0NRySNqfLqcm4jdWBd6_YNDiCIB_pE9EunYd3fUB3aGGvpLsOhJDZOcIx1pKzdrcw4UX2Mpeb6gnGDoTlubxw,&b64e=1&sign=04d069e2d950b8cf054498b7498f0d0e&keyno=1',
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
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.4,
                    count: 20292,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 699,
                            percent: 6,
                        },
                        {
                            value: 2,
                            count: 223,
                            percent: 2,
                        },
                        {
                            value: 3,
                            count: 385,
                            percent: 3,
                        },
                        {
                            value: 4,
                            count: 1675,
                            percent: 14,
                        },
                        {
                            value: 5,
                            count: 8671,
                            percent: 74,
                        },
                    ],
                },
                id: 302690,
                name: 'KUPIVIP.RU',
                domain: 'kupivip.ru',
                registered: '2015-07-24',
                type: 'DEFAULT',
                opinionUrl: 'https://market.yandex.ru/shop--kupivip-ru/302690/reviews?pp=492&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 228668657,
            },
            phone: {
                number: '+7 (800) 100-44-99',
                sanitized: '+78001004499',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjl__bphPujMcqAmNFlMeVAQmOCeka7vdNramtc1h8QIPBXSb_S4ijGxTX7h6zvNzhvYags5bOyQh7aBrPFmwzbLLyKmdzaJ5S0pVHaLLA4iwodAZAGbo_6mgqhqJR5_q9Zv6oKbOA-O1OMFMblwKeBbuXsoMlAkIHbeSOPSAsBhoKgJTMwrCA-3UHuTTZNe8lXCCiBbaa15HnTVY7-89MOWoLpw5smOinNPpzyO37TZT8Ie6A3Dnku9KC5JuYp87BY5-dECNf4Qy8zXm5nPX1HRSIuh3ha4BdqManWE3IX29Bm1I76OAn4C-9qhqVoRkAwMJfa9t2maj_6HkBf_egSx-bWtfr4RSjTU-Upg6eD40Z8xXZLNn63id9EuY9nfQx8dz82FsFeE3QWXu_E-JAHAmBDaYbF4vNpBt3g3u3_kOTuFOnKzqXZfBNcj1WMXj3mO7pMh8syz0JCJhbb-NcqvqT_kjAZZTFXOgCZvZPjTT_e2Ar7jpV7oRKHwk3Mk4KUKCy4k0AzdK4rIZocCJMwmEbFzUfa46AIOfiZsDTeDqxnFVO5KJN7_AQdEvmYEjWdqTr1TqjWYYYGnOBdD4isSrci85BG_M9PtagDqNINnnJwM-FpoFo9sZ2ciGNwK7fanCbkpj2RRFFg7PQ8-IH1B4gbag6rQRqO6Q5ZB6eBfrNgEiUslhFtEDr-jegkHik95DJgPHgHadB8NSxz7-D5nyzGB4qXnOetrH2R3fPfvgwpvCkNBelsnmnnRIvpiMVYhe833AeyuE2_pMgvH0HuxULHO8lKw_4AepqbwmWUFKfk8lbLcYhUAnH5hUfM2cLc2p78hqwp4RMBbvzsnLV5RxDX_hbAxisl62n1tsVV1DgXXmnpNSP8qI2X5SGI-mN2r3MtYtX_U46X-OmQ8Ij201uV65zpO0S3nrfdUmjHMxw1XYKRtXGlMWcfqE2V3aFDD5eocVCjDnzMM0hN1Mf0,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8dbILqCw_ATDYZKcYjWHe_yzOSVA50NzDTdr8vbEbooszusC_t1xyyhCf6kNS0nX32ZJss47vHyYGsz5AtEHu99SxGS3nMeiyzAtYeog-jI3M4PZBDBES6912jb3PesL-iXNYMp8dEYwCixWSyGurouBJ_BqxRwGU_Um7ZnQ8owQ,,&b64e=1&sign=0fb0cb53c5d7d6acec3a3be3793bfc3f&keyno=1',
            },
            photo: {
                width: 533,
                height: 800,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1907258/market_5c3ZZGkgkCyTgc-Vw48Bfg/orig',
            },
            delivery: {
                price: {
                    value: '350',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 350 руб., возможен самовывоз',
                pickupOptions: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '199',
                            },
                        },
                        brief: 'Срок уточняйте при заказе • 13 пунктов магазина',
                        outletCount: 13,
                    },
                ],
                inStock: false,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '350',
                            },
                        },
                        brief: 'на&nbsp;заказ',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            category: {
                id: 7812201,
                name: 'Сумки',
                fullName: 'Сумки',
                type: 'GURU',
                link:
                    'https://market.yandex.ru/catalog--sumki/7812201/list?hid=7812201&onstock=1&pp=492&clid=2210590&distr_type=4',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 8338901,
                name: 'Palio',
                site: 'http://palio-bags.ru',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id1029701690616546514/orig',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/m01JQXVX70bb8IlLGwXVMQ?model_id=228668657&hid=7812201&pp=492&clid=2210590&distr_type=4&cpc=DbqRVu-FSYd86VDEyJ8m75-tFe5lLKlBqv4rggZxmoOXky_x5kYN287KfjbDUF2eGrwG4vSjS3HbDqrtSJvSD3EGJJYFDmpMtrBl6XteuwvIWLe79ZDDtBkqxT_lWyNmxEAUiOQQkp8RUxEyoNAk7xcT_nudUjpc&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1608,
                    SHOP_CTR: 0.004952660296,
                    NUMBER_OFFERS: 1,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_Trivial',
                        value: '1',
                    },
                ],
            },
            photos: [
                {
                    width: 533,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1907258/market_5c3ZZGkgkCyTgc-Vw48Bfg/orig',
                },
                {
                    width: 533,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1588061/market_16quFtDA_sSMWhZk9NE9zw/orig',
                },
            ],
        },
        {
            id: 'yDpJekrrgZFyXAZx7TPzdguM22ynljUebR3qVRTlyWAVp5XWSTAXtQ',
            wareMd5: 'XrFvmS6n1KJquP5yhk-0sQ',
            skuType: 'market',
            name: 'Сумка женская всесезонная Daniele Patrici',
            description: 'Торговая марка — Daniele Patrici',
            price: {
                value: '999',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZsTN2y0pz5PTZzLPVOlVCdBuHmcpZoKsHDUw64_QhFNXVitsg7dYb_38-LIg2R8kQ774MMuu-cbb8uGkjrnu-aGscaVF3J_Ydqr-KXTRDJVeo-8Q-sIDImXiiQMtcis6Jp5vq7xb4RkEI3g4wjxvliE2wUq6KhXRf5-lkRHrLehJZ319O6cialMPi9VDgsT9dSXw4wxFQqoCNOoeP2BMTZvfGUTQDu7jiGLwv6_JpYmwEBVS9iag1xVWqaueWmufOGGXZTceUUTMAPWA-UuN4wYwTRN0MV1Y-L5U1f5XfGk6IF7XdmORPtaOIhlGItPnFtWzj70gmWqqy9EGm8n5gveKqsG84XLRu8TJBaa6YQHQx5Fb8mnWyYSyOuqn6DrYXmKksO5JawkYdDtKxrb43_IYfBgFKssGdACq3wWYMR72xiEhZHWjN_ouPPZueZALHxvI6Dg52LSAPlup_kyVv_3lfcP5cRB2d3Kzr6ULmYPBciuVVOAaDw2iVhwMuGdbfeQPyzog9K8q1iknF7fhdV2v5hUxIA1v2142bA5fjbRkj9RQPiR6CofNJjXH8MQnvIB4hk0P30C85Zlle0pQRi_sQoKjA7GIOxSqLEoBfg24Idreuqo4kifE5VAaJffpa7TZDX0Q3SgeSuVUgZVDaObg3_Kt9uW5wps-mL5E04XML5OwyFrW4yhUCYWKGcbIx0HPzkzR3k4eSGVPQfchQMnbEv5qRyFjNFkZR75kLOIVaiiJ0Rw4SareACZCRGSwn8TeRcWBY9KVQEsod0fu8heVTO0QUaRHTkxpXu4D_9SG3vA54vHAsKx4nUYgUks83Ktwb6-kt9XX1ltfjns_K60GVmkzOmh5rWP5mq0uBuDCieB9HKEkDvQb1wn5ozZeONwvpfanu5kZntO7jF4gForcrhXcwVsspPCpoIgvsea3LSiUIs2f8mH31s1qMuamvi5E2xw2cX5N?data=QVyKqSPyGQwNvdoowNEPjW_7ELrJbv5FvAxqUvXb3o0nQm4g717uNS9miWQlrPKEd3cuMuWB8t74MU0Fab9iZOdvv3jrD3AtaeeQeOO-0I4WNoiEZwSHV_aP3Ry19Ya6lyI5nWgyjKBZYsblgqCqg5rw4xyrZ_p7pwzH_gfQpHlohniW6Q7IKrnQuwzHDTQg0SQw2hCo-vGx7xB-d_vpDt5hXhvKUJVWkoLoK7GT6MPBtcc1Pv9yWmqcT8M4E30l_phwSwM1UKZ_t-ptDkmilBQtUV1Tv-JiAP8y7DCJCxiFwSDSQCncoNuxs4s8cEFy6BP6N6XmXCfCqKArch-gMWn0XlCJnSGT_PDCAPefWUwkOEd6QY_Kflq_i98uCGJB1GJ4karVTl1k_fI1FDYG80HXWmoTff-IVm8RCrcehcEHYRB9JAdssI3Tf7bgEbt_v3GmbqGLevu-u_iLKraoT6UNv6XtQAQhHsgVG0JynU742RTJZwvwBw,,&b64e=1&sign=1431061c3bc4ed29feb508df8cec16f0&keyno=1',
            urls: {
                492: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZsTN2y0pz5PTZzLPVOlVCdBuHmcpZoKsHDUw64_QhFNXVitsg7dYb_38-LIg2R8kQ774MMuu-cbb8uGkjrnu-aGscaVF3J_Ydqr-KXTRDJVeo-8Q-sIDImXiiQMtcis6Jp5vq7xb4RkEI3g4wjxvliE2wUq6KhXRf5-lkRHrLehJZ319O6cialMPi9VDgsT9dSXw4wxFQqoCNOoeP2BMTZvfGUTQDu7jiGLwv6_JpYmwEBVS9iag1xVWqaueWmufOGGXZTceUUTMAPWA-UuN4wYwTRN0MV1Y-L5U1f5XfGk6IF7XdmORPtaOIhlGItPnFtWzj70gmWqqy9EGm8n5gveKqsG84XLRu8TJBaa6YQHQx5Fb8mnWyYSyOuqn6DrYXmKksO5JawkYdDtKxrb43_IYfBgFKssGdACq3wWYMR72xiEhZHWjN_ouPPZueZALHxvI6Dg52LSAPlup_kyVv_3lfcP5cRB2d3Kzr6ULmYPBciuVVOAaDw2iVhwMuGdbfeQPyzog9K8q1iknF7fhdV2v5hUxIA1v2142bA5fjbRkj9RQPiR6CofNJjXH8MQnvIB4hk0P30C85Zlle0pQRi_sQoKjA7GIOxSqLEoBfg24Idreuqo4kifE5VAaJffpa7TZDX0Q3SgeSuVUgZVDaObg3_Kt9uW5wps-mL5E04XML5OwyFrW4yhUCYWKGcbIx0HPzkzR3k4eSGVPQfchQMnbEv5qRyFjNFkZR75kLOIVaiiJ0Rw4SareACZCRGSwn8TeRcWBY9KVQEsod0fu8heVTO0QUaRHTkxpXu4D_9SG3vA54vHAsKx4nUYgUks83Ktwb6-kt9XX1ltfjns_K60GVmkzOmh5rWP5mq0uBuDCieB9HKEkDvQb1wn5ozZeONwvpfanu5kZntO7jF4gForcrhXcwVsspPCpoIgvsea3LSiUIs2f8mH31s1qMuamvi5E2xw2cX5N?data=QVyKqSPyGQwNvdoowNEPjW_7ELrJbv5FvAxqUvXb3o0nQm4g717uNS9miWQlrPKEd3cuMuWB8t74MU0Fab9iZOdvv3jrD3AtaeeQeOO-0I4WNoiEZwSHV_aP3Ry19Ya6lyI5nWgyjKBZYsblgqCqg5rw4xyrZ_p7pwzH_gfQpHlohniW6Q7IKrnQuwzHDTQg0SQw2hCo-vGx7xB-d_vpDt5hXhvKUJVWkoLoK7GT6MPBtcc1Pv9yWmqcT8M4E30l_phwSwM1UKZ_t-ptDkmilBQtUV1Tv-JiAP8y7DCJCxiFwSDSQCncoNuxs4s8cEFy6BP6N6XmXCfCqKArch-gMWn0XlCJnSGT_PDCAPefWUwkOEd6QY_Kflq_i98uCGJB1GJ4karVTl1k_fI1FDYG80HXWmoTff-IVm8RCrcehcEHYRB9JAdssI3Tf7bgEbt_v3GmbqGLevu-u_iLKraoT6UNv6XtQAQhHsgVG0JynU742RTJZwvwBw,,&b64e=1&sign=1431061c3bc4ed29feb508df8cec16f0&keyno=1',
            },
            directUrl:
                'https://kari.com/ru/zhenshchinam/sumki-i-ryukzaki/sumki/sumka-zhenskaya-vsesezonnaya/06405210/?utm_campaign=Yandex_Market_Moskva&utm_content=1955628&utm_medium=price&utm_source=Yandex_market&utm_term=1955628',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRtUN0LWbP4xzDJ_wAf7VCOppv_TMIGBl6wcohp0ubCEOuf5LrJzesQe7UPGCFYBFfrTr_Bb815Frnep4HmA9tC7zW45GQLTt0DP87nI77v_tmno04sOT26oE2vuaCkrdXU1MrEBCjF7fAh3M4FiV_tesByVpe33G4NsyVtsxirqJJVy09KMiIbr25v5MD1Xtx65wjTcnSnMdmWkohJumNAmNTwQtt1eU5Rk421jF3IdctjvtFpbXivOdp_kEBOvmBtrR3mT5l3X_UQyZYrSdxbXW5AnKbThtOhJ7PK8goKp3EW_PHmMRk_XdRxv7KrZaAgLcPMHbRFoTptQRml-xybfRUzomN_jH6R-T6iyWyuN5wZS-ZvJ56ZFzZIbqzZmsVcKXasSJI3sp2ZjUc1J7WHeQTZJMZTbFiUS3T-kxZ3Ce9G75lAjieg6UoZ71MBDLGA26xaZBsDuwsj79-f3rEMlS6jGm1qgM0AfU66X-ePlkgdKozK7IXTUXKKRP7ekT86vAkuE8Tcr9H2ubB-zS-UlSiUhVGAj2eLmsF5hxZh4_RY7L5Qlp7EbI3cTrzX7qTZBfwIxcgTHBrrg92tqUHD88AX_XybY99WFcews9_HhsM0RwDNdLv9zPQaXz_jxRFs1seM9i4kwtJFRn4BEuTFdIXv2hi3WKClIQa7eWNV4yedYsMuhZbkJVsZhnlNHtF2KAR8pZNbkKJ44iDX7A-FxSnL7aLT1tCESBx_JoEBVauzLRArLH9ZgAByFhTduUORCIsAaU3U5iqrBh0wy-1D8GzieOOpvydSaO3aOGtFC-ki2oM40M68pLZkt8aQ5-cwdBkT8lLXDi7mVnamzyR7-OADFkJqBLlzUmJ2S7luoZvhAlmmr-H0Ig00sa9mlU6H3HDkJUWEI0Fn8C4XfHnU12ujr7miUsuSPNYQjdXqtEouOonyyB-v4uFXxYrTJmpJtTGEDBbo1-g,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YL0Kud43qAL4UYw98j4Zh80kkmJlpOUpINe4Iy0Uo70Hc7S-s_XhgiLwGzJLfm9vTGqmQSm5u4qrVV1l9k2c8NxhaXi8kJujyE5kxQBaVECaJPX7Mpfj4U,&b64e=1&sign=b7a172d5dec05be041b9dad615a14a44&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRtUN0LWbP4xzDJ_wAf7VCOppv_TMIGBl6wcohp0ubCEOuf5LrJzesQe7UPGCFYBFfrTr_Bb815Frnep4HmA9tC7zW45GQLTt0DP87nI77v_tmno04sOT26oE2vuaCkrdXU1MrEBCjF7fAh3M4FiV_tesByVpe33G4NsyVtsxirqJJVy09KMiIbr25v5MD1Xtx65wjTcnSnMdmWkohJumNAm_ix2w6cC_ql_zu8MXC3MTO9HFVzTaA3fpGGJx2EDXUMZpuHYQOAJTRahjd_bqQ09QNJJs30yDdlUGCVfrlJOJ9SInvZkIQm_t9kDYW0VR1XHi9ei67VgP0VvAzCbzjzQN_QU4n6phCcPVY3M9q8cd7e0Tc6LGOII--b3xtzicWb9Oq5t5h2h1jRaV7lr7EeR3ObmwxFy6dnMkd2xOfwrjB9MRdYAf-D0ZW9p8XVjWVblUMjVjoEej3NGwxy6l33ADBggL6x7v0n9EvjRoqrfau2t9x0rhybNFxBDzSH_HBjZneAU651RmFX6AfF5EyxcsNys0fAjnSYikMg6mvJN9KSSbdEalz-FaJ6-bo6rkLBJbxrBWDAdhsoEH37XNbKxZb6Z-XvHjU_ZX6fcG1UXn7ltc1S_GbZePHsVd079WwMZhr37zIdZDKEvUYuKsoB4o2ljbUn9pMcl852onkZRWYOWBPnebmj2ZdMRDDGgi3h3tWL5K5vyn5ezCPwBwr0VTh5qYuz5wXD3vZCamb0mZswlrWVEgXn4KNugbV88QJXvlf26OIDhY1Co5vx2a2F92otkrSArqWQkiYK_bI3tMKGyb6HkIapOsLh0PLIgGg6PuolSt5qD3aPEUQPatFlu1TMinX9vj7Bqy3RIwXcomh40z5-UjRqOwM4us8QlrplsUyeVb1GLCma-MCAOk5AHUIW6kPjbULuHvIFCumKFe77VYSan8H2l9l0xqg7iGphYF6OcAy-ibA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YL0Kud43qAL4UYw98j4Zh80kkmJlpOUpGUR2aoo1oLym7NQ9GoxJ0tkluHpUnqI7XoNgnv16PcML9NOPE25REpQRhC3Ob5K5Qb5lfJSNGQyNM1GDJi5Ta8,&b64e=1&sign=faddbf7b3e130bb3269614bf4c1f2a57&keyno=1',
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
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.2,
                    count: 3706,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 83,
                            percent: 11,
                        },
                        {
                            value: 2,
                            count: 29,
                            percent: 4,
                        },
                        {
                            value: 3,
                            count: 40,
                            percent: 5,
                        },
                        {
                            value: 4,
                            count: 79,
                            percent: 10,
                        },
                        {
                            value: 5,
                            count: 527,
                            percent: 70,
                        },
                    ],
                },
                id: 366769,
                name: 'Kari',
                domain: 'kari.com',
                registered: '2016-07-08',
                type: 'DEFAULT',
                returnDeliveryAddress: 'Москва, ул. Автозаводская, дом 18, Ривьера, 115280',
                opinionUrl: 'https://market.yandex.ru/shop--kari/366769/reviews?pp=492&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 481720517,
            },
            onStock: true,
            phone: {
                number: '8 800 200-10-63',
                sanitized: '88002001063',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZsTN2y0pz5PTZzLPVOlVCdBuHmcpZoKsHDUw64_QhFNXVitsg7dYb_38-LIg2R8kQ774MMuu-cbb8uGkjrnu-aGscaVF3J_Ydqr-KXTRDJVeo-8Q-sIDImXiiQMtcis6Jp5vq7xb4RkEI3g4wjxvliE2wUq6KhXRf5-lkRHrLehJZ319O6cialMPi9VDgsT9dSXw4wxFQqoC8XrvtZtvDyR5YbeaB8jsa5leSb64vkywjL2ESLDobOviGvxBtpsbcsuzQlj9cN75Jm47zTK8qyEtEdYkgSYlYLTPYlrpbRp3wkerUXrVZqYMP7zKsTlkMdvdsBODWGboj2NZLIPWDZyx7Gwe9ie-qcv0OeQfLrW-Y5YLFYMDC1BUMvvC5cSDlgwyxvRToZ9NU8ihclh6l5x31vYgvCvaZ-uHmf3wsZpHpQuFIxowRpMAPJkd7_mPGh8inkTXxjq2cKidPmLNj_RivDAdycOgmX0-psiU0SWCCv-lBdLzUcICgHpO8B9q6GbO2hyOFNt7CvchRfZ_IHjHxqaYOdsn-h2RW04hZRyAvQUDVnk9vsUruWUS7AqgNBulpl_e_GiJr4FfyBGZNRw8naonoRF0QX-eXW7khUL_t61_n8UUr8I2u3-92doxSnMUnbigCr33S-QKvNpmbIDVwWn0zjkGfJGu-ey3cKMREuxiQ7d2ERSYB9paKqNWfjSnIGJ6yIXvIC2gnOgoF6BIyiy4qf2fMiyFkYBJ43gQIm-UPLL2ssHfDijyxvmDBKpDzq5X9jPxOfTy9TL95gpu1P3slu4DVVgbI_ybxQw7HjelSRIT1SkM5Oo6qUDCs8pVNNhCRfeVXkCG-pwLTDhRLSN0l7kSgEGQtZGGuiI-QgVs4m2h4gojcdmIts_6D-u2R0VFwUBYjdBT4ye2witmWlLskLmyxtZ-OMd9h2sVb92V19DrGCCt2aGmGwchTN9dTa7rR2cq?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_BmFpwYkjDcQU8IJUjKyn0GXhWYuUoYyt8IevyciL4UbmMcGSca5_f4FZl1B0U7cddPlLWSj4GE4Koun4-M0c9vbi54F26niEcPrM0vjMPPWzQauuuH4TNaHtrP4lku81V5XfnH_irgm6ABmsgX552kBMpSK0cDDU01V1gzNtsaA,,&b64e=1&sign=9f60dd7bde2807514fe5a4f7cea43209&keyno=1',
            },
            photo: {
                width: 762,
                height: 1100,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1446479/market_egH2lfecDtxPFGgVaVwW3g/orig',
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
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'не производится (самовывоз)',
                pickupOptions: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 0,
                            daysTo: 1,
                            orderBefore: 24,
                        },
                        brief: 'до&nbsp;завтра • 28 пунктов магазина',
                        outletCount: 28,
                    },
                ],
                inStock: true,
                global: false,
                post: false,
                options: [],
                deliveryPartnerTypes: [],
            },
            category: {
                id: 7812201,
                name: 'Сумки',
                fullName: 'Сумки',
                type: 'GURU',
                link:
                    'https://market.yandex.ru/catalog--sumki/7812201/list?hid=7812201&onstock=1&pp=492&clid=2210590&distr_type=4',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 15134534,
                name: 'Daniele Patrici',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/XrFvmS6n1KJquP5yhk-0sQ?model_id=481720517&hid=7812201&pp=492&clid=2210590&distr_type=4&cpc=CSlm-yMUShtOePFq5AMAZFwFgLpVmFaHcQKJd9abMUz9cLXCVHyTj6JNuWRUEspKq2QoSHPMXpolcAaXvn-CbZd_HRC4rmAquvwr3mRp_9b5LypXaXOgdRclZeyV-S7NBpogmsaXVDzhxPGwg3A-sqS8RAYz7XDz&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1608,
                    SHOP_CTR: 0.003537813202,
                    NUMBER_OFFERS: 57,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_Trivial',
                        value: '1',
                    },
                ],
            },
            photos: [
                {
                    width: 762,
                    height: 1100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1446479/market_egH2lfecDtxPFGgVaVwW3g/orig',
                },
                {
                    width: 762,
                    height: 1100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_xcpeISech4hpwqYX0ovcKA/orig',
                },
                {
                    width: 762,
                    height: 1100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/406938/market_p31xhD62QBlv44t5V9QD8Q/orig',
                },
                {
                    width: 762,
                    height: 1100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1062628/market_bCfxFer2t3eD3ioksANZUg/orig',
                },
                {
                    width: 762,
                    height: 1100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/937598/market_8qt_X3FP29WBEiJh1xHZtQ/orig',
                },
                {
                    width: 762,
                    height: 1100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/937598/market_ndHD2CrIDf3T1KIrwYSwrg/orig',
                },
                {
                    width: 762,
                    height: 1100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1109055/market_QVmGxTvs3FHZRuJtH916fw/orig',
                },
                {
                    width: 762,
                    height: 1100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/932147/market_ldH3ewZg9YxunRJaXEu30w/orig',
                },
                {
                    width: 762,
                    height: 1100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/365133/market__PE8kD-W9nIGCL0wbpfeUA/orig',
                },
            ],
        },
        {
            id: 'yDpJekrrgZEDX4mB6mJel93OCP7y5Psd6iV7cWEA9mSM7zLaHCAmkQ',
            wareMd5: 'j33GVNBZCD9nrfc6Rnho7A',
            skuType: 'market',
            name: 'Женская сумка Pola 84511 7.5 черный',
            description:
                'Классическая сумка POLA выполнена из экокожи и прекрасно подходит женщинам, которые предпочитают строгий, деловой стиль. Основное отделение закрывается на пластиковую молнию, внутри оно разделено карманом на молнии для более комфортного расположения вещей. Сзади сумки есть миниатюрный кармашек на молнии. Данную модель можно носить в руках , с помощью двух тонуких ручек для переноски, высота которых составляет 13 см., так и на плече, с помощью съёмного регулируемого ремня, длина которого составляет 118 см.',
            price: {
                value: '2340',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZva1CmpLxsdY8oiSVULF2Euy2e1zpdjCNcCNB_EAO7voKgLn_11NklA-T5Ctuh5kgh6ipqIC5iWtM-Cku8RGm0uMG6AqbDPHdYZ38XugaO34pXsYpCyYBSLbR-ndoZEcLJQNDxpfpNq80w5bJPpccy2aocXXhkTN3l06Unr7EPNxSKB9bg0BYsgIhkp1QK585XnbclWqYzzI6Ajzwy984SW16AtIBecQXbcM4ksJNKhj_ZvnJgRm9HnTvePI0P3kv9ut8wXV6UqopLpa3ug_tlmxdKPKgl9WaBdQRpkJtZqXNRsvLzIsJp2PpCa0Lh6Rn2wMP-Yoe28c5yKY3-fK2qiGTmL7G-nNMPor8UlMToSWcZo2BnkOq5kY0o-oI4OycpBbLyu87GIAZbd6p3K1iSgkNaFeuHkT-gl9XPuNTNgGPHTf-krIm8Uol5mGjx7zfpUh85mDDSiXmutEgDGdhQZrXi0-d6yQm3fQVonCF7eC0C7a37Gz8OLQSp_-ivLpLoF9s_35EaKVKMySzB1vNs_aMCDMzPtJUq6OVi66ims3pEixpuLa7yZwhsQV94tn5y16771yOGaNe5yRHjy0eaJHY0QJ7sT9o-MCAHSuZPhvv1NIbATY-KkTvUlWAwrwfr700XJpiD54Hx8xNzj6c42apBdP3H2piGpcpMQoT_a3W2vdoqv5LmAgCbiinOFpeY0DOekLUWpJBoesXtjv_avlc6tEh0XuXInsrJwFwXgkO0mr21elQsawAhKhjC5qHn7o06YRiXuKF1gm3nryHdQwpe1DIdCaCgrOWKwLoqjbRDsZmsFdGBAmfECtnxn1FJQX21tQAIrfdH9rRzdAJsNN4zRkgWqYa3jHivmY0PcZQqaBYR5mILoPHK-S_tLvmpQtpWxpenCKLI6JGj9V5OzNzAznua-TDdLX1GaUMvsJSxyOBaqVphhCAAc9rHWjp4wmucwk7wFi?data=QVyKqSPyGQwNvdoowNEPjZKvmaNzDxVfvL4qplb48ktUpdEvAsGcQ1Jg2B7g7Fpa1qkV3O6Ay3eLzgCQuGxzQWw63dNvTS1YkUWkVRZTm6mrHe4jWvVxV20JzkryaEWRvPC7pAn5E_thFlh5mTdxatRHY1dfoQ5sVYVBpfMibGDyyeHyKXjF_6hCA9Msec9FQCGse9EGm0m_7ypQyHu_CPLIXKDWdNgnIvsrGwKqlkWIR7tXZ6g7bcZyh5l0nAt9zzXe2EyXH-hEL1NmN4_4t0rbAL06xQomWbN5TfFXM244wUyrFDF-C1KHOA_w1h-SaaJzwKKTjLBda5UxXj4xpTQc-09QdZaopHRpdvWr2zpexbF6FqZMx5JjZdhBHkbjmsL94uKG1x52ImzeCPks6Y4-1Gj0e1W_WRwsx_vIy7lGFZKls_gcaLBXnki0Ghu8&b64e=1&sign=b07d4a63c941a64a629f90a731678dc5&keyno=1',
            urls: {
                492: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZva1CmpLxsdY8oiSVULF2Euy2e1zpdjCNcCNB_EAO7voKgLn_11NklA-T5Ctuh5kgh6ipqIC5iWtM-Cku8RGm0uMG6AqbDPHdYZ38XugaO34pXsYpCyYBSLbR-ndoZEcLJQNDxpfpNq80w5bJPpccy2aocXXhkTN3l06Unr7EPNxSKB9bg0BYsgIhkp1QK585XnbclWqYzzI6Ajzwy984SW16AtIBecQXbcM4ksJNKhj_ZvnJgRm9HnTvePI0P3kv9ut8wXV6UqopLpa3ug_tlmxdKPKgl9WaBdQRpkJtZqXNRsvLzIsJp2PpCa0Lh6Rn2wMP-Yoe28c5yKY3-fK2qiGTmL7G-nNMPor8UlMToSWcZo2BnkOq5kY0o-oI4OycpBbLyu87GIAZbd6p3K1iSgkNaFeuHkT-gl9XPuNTNgGPHTf-krIm8Uol5mGjx7zfpUh85mDDSiXmutEgDGdhQZrXi0-d6yQm3fQVonCF7eC0C7a37Gz8OLQSp_-ivLpLoF9s_35EaKVKMySzB1vNs_aMCDMzPtJUq6OVi66ims3pEixpuLa7yZwhsQV94tn5y16771yOGaNe5yRHjy0eaJHY0QJ7sT9o-MCAHSuZPhvv1NIbATY-KkTvUlWAwrwfr700XJpiD54Hx8xNzj6c42apBdP3H2piGpcpMQoT_a3W2vdoqv5LmAgCbiinOFpeY0DOekLUWpJBoesXtjv_avlc6tEh0XuXInsrJwFwXgkO0mr21elQsawAhKhjC5qHn7o06YRiXuKF1gm3nryHdQwpe1DIdCaCgrOWKwLoqjbRDsZmsFdGBAmfECtnxn1FJQX21tQAIrfdH9rRzdAJsNN4zRkgWqYa3jHivmY0PcZQqaBYR5mILoPHK-S_tLvmpQtpWxpenCKLI6JGj9V5OzNzAznua-TDdLX1GaUMvsJSxyOBaqVphhCAAc9rHWjp4wmucwk7wFi?data=QVyKqSPyGQwNvdoowNEPjZKvmaNzDxVfvL4qplb48ktUpdEvAsGcQ1Jg2B7g7Fpa1qkV3O6Ay3eLzgCQuGxzQWw63dNvTS1YkUWkVRZTm6mrHe4jWvVxV20JzkryaEWRvPC7pAn5E_thFlh5mTdxatRHY1dfoQ5sVYVBpfMibGDyyeHyKXjF_6hCA9Msec9FQCGse9EGm0m_7ypQyHu_CPLIXKDWdNgnIvsrGwKqlkWIR7tXZ6g7bcZyh5l0nAt9zzXe2EyXH-hEL1NmN4_4t0rbAL06xQomWbN5TfFXM244wUyrFDF-C1KHOA_w1h-SaaJzwKKTjLBda5UxXj4xpTQc-09QdZaopHRpdvWr2zpexbF6FqZMx5JjZdhBHkbjmsL94uKG1x52ImzeCPks6Y4-1Gj0e1W_WRwsx_vIy7lGFZKls_gcaLBXnki0Ghu8&b64e=1&sign=b07d4a63c941a64a629f90a731678dc5&keyno=1',
            },
            directUrl:
                'https://www.polashop.ru/katalog/sumki-zhenskie/84511-black-zhenskaya-sumka.html?utm_source=yandex&utm_medium=xml',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRtVZ03GO3ZYTLAjFIRfA-VTPS4ad7qNCDVe5wddSwBGeze7FscfYe-Z3bwWwPFR_RLSLbQU-DZgz2047eFYbLKvHpTHRX3VJeLdOYn5O92vUgXLVZ-0zz4FQRO2vN_GU--p7X38GLeyOmY8Th0puLanCrKagNvlm_MKAUneQ9lC9_ZVCCpn1QPYTyqo_BOHB3J9aks329vRoj59aEgXj24zihw1xbU6aOGSlLZuZj8pg5VCFUAxwyaz7WcOrepyZcGQceG65jXEgO497zVBMEpJSIoR6wfwHksPtJzS69b8LtTs64ssUxEmIjN_ba2jgs5tBFEYxPHGDjEx-82dv2SVV9NUDh5JiPPjymUblhzLUFEMMOJ7evFoTeOc5PKOpgSzrBq_2P4CYIGjx7hg0iDmOcsiJFa3wxxkTUh927ta7p_GOYX0BPj1RQB61PqFxk-_laM8xdoKiF9J_b4MEM4SMsXpgJ2w_3aAmt6iRwEupYM9frIhjSdRYxGxb6W5lPj69VoOISNsToLAG6cGH5JtU_RdLNCRqjI5iWX5cuf2UqTGHujcpBPqFLmOX4YisXCVz4bVIbn4D1Elnha-XHSLscBve3PGVk1_Wz_NKvcuZEpKh5hrPg0_sMBXyfSbx9twBaM-ut0K0AuNBtaKFfFry6nE7FfNi5arkvr5fdnsHLubvmpjoS0WSYhKfUCxkeJZ4y341bJqGC5AsMHcqF22zYFpG6kDRVWAnzpWZxfaxrNSKsMd2VwyIfp-6THoZ0vSJnVGCqG8Vl_lZ2UeqK0ZC4vJIYQGUgYCZgXqU0riu7xzC3V4F5IDR_Lr9SkS0NOma4Q13HWMgOaRHo_YHXEsVD4qznv3xWbLeLlJNIGhZxST3RbNEoyBaTGKNr02jL2DkdUKqpDKniiy3400S4htzNxxbzb3tVN5p0YE-ebRsfV3TjVqvL68heP8QZXci_3SLnJyWqS0ZQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2Xy5ff19OOJ3pj8LWcZu1ES4U7vbKcPM9DyPjAzwfKYRDLWQFFQKU-9D4cXPulCHLHcLy0RivC_bNriD7HaZZYTZwgQkTqU2wTMppxPiL-k2hR_hvuZXemk,&b64e=1&sign=3190c99b4a976777a5c95ae4a40a02b1&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRtVZ03GO3ZYTLAjFIRfA-VTPS4ad7qNCDVe5wddSwBGeze7FscfYe-Z3bwWwPFR_RLSLbQU-DZgz2047eFYbLKvHpTHRX3VJeLdOYn5O92vUgXLVZ-0zz4FQRO2vN_GU--p7X38GLeyOmY8Th0puLanCrKagNvlm_MKAUneQ9lC9_ZVCCpn1QPYTyqo_BOHB3J9aks329vRoj59aEgXj24zW8zqaGhoASZo1pPhMSAR63KkSNkoQ03dFi9tXm1XVTU1YJJy_DkpWZA9Qy8P9MGPHK0bbB_A3gefg3n2OwWuPxuK18PloUP0DU57B6-c4ManiOZaXnl1qj1MG7DQ6OB3HkAa7GNluGv828iIBt-TaFW2Kem9UZUrsWbe4_RF2Y54i-BlTGJLUGUocwOgRe05FyoQXV8aR9Y903XhqiKbAOcF4qsRLXm_ZdkzR9IsMpjIZBTQa-jJnSl8Hx8E_dWoOiGLeCOb2UjamPuvC1s6OdwCgEr7gGYyIVmizEuw8TIFVYFGh_Ch5I_XSugnhAvTFew0D6ERGKTLSruTr2vsjCzj3807VZinbPOZ240MkeRrihGFMPa9GRJPwsULnnMgmAQauF20a1HMvxRVPuvfuMacxa1w6IKyFS4VGBd3h7yXVPm1-ARmizKnKFC_pOyWA7YIoz3Bvp2FQEhPvaFAoty1UhVfZLZ-juEoO43pMQCBHMGqh-RoCafEknZJV-865jq0sT-Hwd3B49633z6BGIsPKX_mta5qy4Vfv1Gdnv5VFD5VLJ29xI3uM0dKLSeobFlZgBYJSXOK6C2spPXQI-V-JLRRvss424c3a9WeT-CKtUUTP5CDW3FPUco9KPbwKMayOzELhxjahoecPzN1Z0Ri3IuZy5VVdQ5dKCSe3CqKzEI07PDKwhqcXWpW19eyM8Bz-IbXUX9IHSb0YOZmOWIEvX8jtgcTNms7bg15bhZ3Uw3pk7RiKA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2Xy5ff19OOJ3pj8LWcZu1ES4U7vbKcPM9MhnC1CBKB59lO2fAFRhAb7cTmfcwTVAK3NLDbEpx5OJ6WdcHxdy43c9w937MLaXCT33iXkv8i7_KyiCCuCCXGE,&b64e=1&sign=675648c859a8e40d2f4de29e79b3e395&keyno=1',
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
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.9,
                    count: 100,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 2,
                            count: 4,
                            percent: 14,
                        },
                        {
                            value: 3,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 4,
                            count: 1,
                            percent: 3,
                        },
                        {
                            value: 5,
                            count: 24,
                            percent: 83,
                        },
                    ],
                },
                id: 83591,
                name: 'Официальный магазин Polar и Pola',
                domain: 'www.polashop.ru',
                registered: '2011-11-29',
                type: 'DEFAULT',
                returnDeliveryAddress: 'Москва, Зорге, дом 9, 125252',
                opinionUrl:
                    'https://market.yandex.ru/shop--ofitsialnyi-magazin-polar-i-pola/83591/reviews?pp=492&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 448213101,
            },
            photo: {
                width: 2000,
                height: 2000,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1690708/market_p2-fix8D_BjWhUgNMA0cjg/orig',
            },
            delivery: {
                price: {
                    value: '0',
                },
                free: true,
                deliveryIncluded: false,
                carried: true,
                pickup: false,
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — бесплатно',
                pickupOptions: [],
                inStock: false,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 2,
                            daysTo: 5,
                        },
                        brief: '2-5 дней',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            category: {
                id: 7812201,
                name: 'Сумки',
                fullName: 'Сумки',
                type: 'GURU',
                link:
                    'https://market.yandex.ru/catalog--sumki/7812201/list?hid=7812201&onstock=1&pp=492&clid=2210590&distr_type=4',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 8339526,
                name: 'Pola',
                site: 'https://www.pola.ru/',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8429979043094011428/orig',
                isFake: false,
            },
            warranty: true,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/j33GVNBZCD9nrfc6Rnho7A?model_id=448213101&hid=7812201&pp=492&clid=2210590&distr_type=4&cpc=t2BvcdQIEvA5M0E4Ok8KS1sytJmEXjnpwSL6hDnhmP2F8C9fT7b7c0P8gtdYjSST_AALTXB0_V8n_Fgg-C6zltsyeAp6CbA_qRGr1mwsW6xVvHpEOKPdVbuNL6fPy_58E4N9w_BlsGQ9gnpEDWmP6eY5Z03MlsZL&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1608,
                    SHOP_CTR: 0.03534777835,
                    NUMBER_OFFERS: 9,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_Trivial',
                        value: '1',
                    },
                ],
            },
            photos: [
                {
                    width: 2000,
                    height: 2000,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1690708/market_p2-fix8D_BjWhUgNMA0cjg/orig',
                },
                {
                    width: 2000,
                    height: 2000,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1717829/market_qJD9c-IC3CTDZ9GycKoqqQ/orig',
                },
                {
                    width: 2000,
                    height: 2000,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1690708/market_5rZIQvKl79na5IFBgi7YPA/orig',
                },
                {
                    width: 2000,
                    height: 2000,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1724337/market_tOFI-jtT4hyN8_Ca7sAcUg/orig',
                },
                {
                    width: 2000,
                    height: 2000,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1578323/market_Odjoo0oI8IIArdSJiox0Og/orig',
                },
                {
                    width: 2000,
                    height: 1999,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1857979/market_IbdfNguOkmuWLi48kCGIpQ/orig',
                },
                {
                    width: 2000,
                    height: 2000,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1578323/market_V6UfNKmFdvTe712AQtdUBQ/orig',
                },
                {
                    width: 2000,
                    height: 2000,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1716597/market_7C36RkUGDtJY2r87fkTJqw/orig',
                },
            ],
        },
        {
            id: 'yDpJekrrgZHSZJU_l7rzlSO3tie_CNLphNFGSIORs_rTgVT0yWkBow',
            wareMd5: 'Ou_dyEZkz4N4ytiHqbbsiw',
            skuType: 'market',
            name: 'Сумка женская Fabretti (черный; натуральная кожа; GS20070 Nero-1017)',
            description:
                'В этом сезоне все модные дома предлагают аксессуары в темных благородных оттенках, поэтому если Вы не готовы экспериментировать с цветовой гаммой, советуем обратить внимание на женскую сумку от итальянского бренда Fabretti, выполненную из натуральной пористой кожи. Сочетание классического черного цвета и фурнитуры под серебро, а также лаконичная форма и дизайнерский бант — похоже, такая модель станет идеальным спутником как на деловых встречах, так и на прогулках с друзьями! Сумка имеет одно внутреннее отделение, которое закрывается на прочную молнию. Дизайнеры позаботились и об удобстве аксессуара: на внутренних боковых стенках они разместили карманы для различных женских мелочей, а также включили большой отсек на молнии. Аксессуар вмещает формат A4, поэтому вы сможете носить с собой любимые гаджеты и важные документы. В комплекте имеется длинный плечевой ремень, с помощью которого сумку Fabretti можно трансформировать в кросс-боди.Габариты: 15,5*22,5*30см.Высота с ручкой: 34,5см.',
            price: {
                value: '7990',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZt7xf36mpd7qQudpaB655C2K02LsZ1btRkFgPrbkqOwUnmSjYlAidU8kSNkcLhDfElPoYsUf_1HAtq6Z50oPhes0ESp-KeehlkJ-3tzoq0GNTF-oK3aMSyF7Cq7EuNcpI3r2J6__X3LaaXjX9lrZO-9oYcuivqzOPFaOEEtOt9jwh0BHQnFbAUb6ycvYJAn0zLTTvI67KjOasmM1avyfqI4InMOZVLIqEsW5uzoJ1PcKVpuiyCwlusePi9cI0rMRJA7X53A5Zm_Anp2C3FzVpYeDm6QAAIGg9TBcqkLNuxBULwWFRJdFWi25HreG0_pQ4u_h77fxl3lIvUiWeMkng3VJ2_ABXr-MYhm3HqlTBJpx9eCXLZ5TESzltPmtaahPgme_X680e36CdykpTiEFCoyjkMp6f3GpBKt6edUizlJEHp0cThgRo3XRZ7yYrS926h8q7-kAC6OkuYatnfhXbhLDHinEVpQwgZ9IR31neYiuJslbpgMe0ymbfmhsp_6-gMyD7Kmcg3707ZD6OJxfRiEAN3sdEjFyvW_2Uef8MAKBvMrkNthR0j8bzKKWIVST5mQPltuZjevcBM1GYsvOeeopZ7s_He-E0zNkOvCzXwDuXQshfHGpaqEh3DOxKWFhzwnp2dXhSSpOxtSXkuImUz8JZTn1l7EVThdcsG3o-Zg28q4Pkn10jZm6Fi7lJ9H1BuT2kJ-p6qZ_8G9JTb5uUixyee6NeEyV0nFZvS49GalEGLydjmlFXmzfohl0kzcWodzjyqabUKJf-Fzq0JSW6JkXHMFZfFxhZh0WvdnI1Ad-6nWy4v4T11l8mxJVrESwQMLnV4pWSAJ1yVrTGcr8dfF2aTC4OuBINW562yPAC8Oq7hPBL4kFFxjv9ThhWPn-2X_VnOR_cI6D3qfuB2pDnkS-3iSlcGnu84VBpOolBKFxgi5ACMBTAd-Mr61MTDGOS6eGuw2LwODdFJcFimag4Hk,?data=QVyKqSPyGQwNvdoowNEPje69LwUqciwSPEaaJtqLiULkwkKNNR8urZbQL2lhYNbCGDng2May3HCfJy3kzdB0AtDxgu7krFSQDY3KNyrcYIVi7MGqgx4pKjhgI87LJGQ-Cicw4pz0eWRUk71zSgmdY5INH3MoOUu0Qn5hdTAdOf60qB7BUtoEeipGzxG_ONEXwWwUbwb2tkUzDA8la71QLnlJ5uJWqpexUtplUvHUFmegklanyiwqx9Hi3Rtvm3POW7ygk1tM3Okaekoo3SkmiHHhZL0iaTf9f2Wb5sqY5ZxCy_FJO_pLTh-beo1BF5_luqs2m6hgNLrv_jgQSgQ5nNHDAPXnTJaM4aiNKslJqg82xgJznh1GbMwnkfVtXk8hyOZaKaXKGmflXE3IVqQg-RBK7-Ke8EU7HGfP4-qQLOiFKRogWrhWCA0nb4ES19ludBZflpLSB5yr-Po2U5RXfkoj6RZ_ThCx3nh1Y-QZKHVI7pP0FtT3jxP6QPrmU9GwuftYmLROCpUj3o9hYwI8kBrIPWQZWGqy6xj6NVPiB32KnqI-2m4AytaUCJdMPBKBAh7K_q_N2A6xn8OCVowjA8sci3SmlURZtNtCZY1YwSxnmcLv7lk8QWuRziiYMR5RtICL1hhY_SS4j0nF8zZCqxR5VLeBOu8Gof_pkaaZRiH5xA3tqneFdgm5bFvO8f5yMxQ5d2UYVNyCDggv-YKEWnJW6ejsAlE8zfvCQpfqM47-fTkzcrbvEx_18jhyGCWW&b64e=1&sign=fbcdcecbcecc36cb0eeb66506f44ae5d&keyno=1',
            urls: {
                492: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZt7xf36mpd7qQudpaB655C2K02LsZ1btRkFgPrbkqOwUnmSjYlAidU8kSNkcLhDfElPoYsUf_1HAtq6Z50oPhes0ESp-KeehlkJ-3tzoq0GNTF-oK3aMSyF7Cq7EuNcpI3r2J6__X3LaaXjX9lrZO-9oYcuivqzOPFaOEEtOt9jwh0BHQnFbAUb6ycvYJAn0zLTTvI67KjOasmM1avyfqI4InMOZVLIqEsW5uzoJ1PcKVpuiyCwlusePi9cI0rMRJA7X53A5Zm_Anp2C3FzVpYeDm6QAAIGg9TBcqkLNuxBULwWFRJdFWi25HreG0_pQ4u_h77fxl3lIvUiWeMkng3VJ2_ABXr-MYhm3HqlTBJpx9eCXLZ5TESzltPmtaahPgme_X680e36CdykpTiEFCoyjkMp6f3GpBKt6edUizlJEHp0cThgRo3XRZ7yYrS926h8q7-kAC6OkuYatnfhXbhLDHinEVpQwgZ9IR31neYiuJslbpgMe0ymbfmhsp_6-gMyD7Kmcg3707ZD6OJxfRiEAN3sdEjFyvW_2Uef8MAKBvMrkNthR0j8bzKKWIVST5mQPltuZjevcBM1GYsvOeeopZ7s_He-E0zNkOvCzXwDuXQshfHGpaqEh3DOxKWFhzwnp2dXhSSpOxtSXkuImUz8JZTn1l7EVThdcsG3o-Zg28q4Pkn10jZm6Fi7lJ9H1BuT2kJ-p6qZ_8G9JTb5uUixyee6NeEyV0nFZvS49GalEGLydjmlFXmzfohl0kzcWodzjyqabUKJf-Fzq0JSW6JkXHMFZfFxhZh0WvdnI1Ad-6nWy4v4T11l8mxJVrESwQMLnV4pWSAJ1yVrTGcr8dfF2aTC4OuBINW562yPAC8Oq7hPBL4kFFxjv9ThhWPn-2X_VnOR_cI6D3qfuB2pDnkS-3iSlcGnu84VBpOolBKFxgi5ACMBTAd-Mr61MTDGOS6eGuw2LwODdFJcFimag4Hk,?data=QVyKqSPyGQwNvdoowNEPje69LwUqciwSPEaaJtqLiULkwkKNNR8urZbQL2lhYNbCGDng2May3HCfJy3kzdB0AtDxgu7krFSQDY3KNyrcYIVi7MGqgx4pKjhgI87LJGQ-Cicw4pz0eWRUk71zSgmdY5INH3MoOUu0Qn5hdTAdOf60qB7BUtoEeipGzxG_ONEXwWwUbwb2tkUzDA8la71QLnlJ5uJWqpexUtplUvHUFmegklanyiwqx9Hi3Rtvm3POW7ygk1tM3Okaekoo3SkmiHHhZL0iaTf9f2Wb5sqY5ZxCy_FJO_pLTh-beo1BF5_luqs2m6hgNLrv_jgQSgQ5nNHDAPXnTJaM4aiNKslJqg82xgJznh1GbMwnkfVtXk8hyOZaKaXKGmflXE3IVqQg-RBK7-Ke8EU7HGfP4-qQLOiFKRogWrhWCA0nb4ES19ludBZflpLSB5yr-Po2U5RXfkoj6RZ_ThCx3nh1Y-QZKHVI7pP0FtT3jxP6QPrmU9GwuftYmLROCpUj3o9hYwI8kBrIPWQZWGqy6xj6NVPiB32KnqI-2m4AytaUCJdMPBKBAh7K_q_N2A6xn8OCVowjA8sci3SmlURZtNtCZY1YwSxnmcLv7lk8QWuRziiYMR5RtICL1hhY_SS4j0nF8zZCqxR5VLeBOu8Gof_pkaaZRiH5xA3tqneFdgm5bFvO8f5yMxQ5d2UYVNyCDggv-YKEWnJW6ejsAlE8zfvCQpfqM47-fTkzcrbvEx_18jhyGCWW&b64e=1&sign=fbcdcecbcecc36cb0eeb66506f44ae5d&keyno=1',
            },
            directUrl:
                'https://www.gosso.ru/collection/kozhanye-zhenskie-sumki/product/sumka-zhenskaya-fabretti-chernyy-naturalnaya-kozha-gs20070-nero-1017?utm_source=yandex_market&utm_medium=cpc&utm_campaign=gosso_market&utm_term=94534166',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRsF_Zj3BDwyRcEutL0x6JDd9qyQXV5uIQaN7ucz3i2J0O1bCznwSFEONZsTKdYZ-xnxE_PqQrTxtZ0mK8I_6mclKpueX5GZc2Coykn5RJ6qGsu9rnBrpS0UYaovOqw-AjaRKOiBgaRWQh8fVJBrpUXLUToKCALynRNF9vD1-jVTwCvPp_fjbxh00stzwaEZy48dpHpvlkdKK5zPQK2_8ijhK0mzrLv4XtS_L1T7RPDm3nutxXpORLJF91cTUhOWr07ByKNLpul_3g0bMALs84wrLL9qJH9vKGjQbYjD6Twe-CXzLwCA8VOSkJjZUT-gZXbsrSsAFD-CkXDvsqiC-eoIOwyQOVUWVA-QHrZnYIMHZt3I8FD7Yr2pddvIOJEQjg1tYgRJ6mqY8fXIwoCD8SnJ4nER1oa3ENdaAGam9FhM4lWVIdOVJq6DtQ0udMH9tLnj1c3jtJaVjj1Ed078pS8SltpCX5cK-qFtgw9ulzyy__y6F8DVsgkqRlpokmvcazL188ssFVX2GgZiNKSjD98XGTNhmLOkTah_pKfet11lxHNwPJ3b-MrAFnjbcy19NJL43pjjYaHyEtYziCEr0xTjU3oPHtCz2PrcV24-eI2S1Pm9WD5kirc99uWC5StKC1lC6TNfpXJAGnNdLua71ozmSto61kTAi41TPw724x0RocJ0lyGDztRaVY5Y7khJ3hVE7hbntMrVokn2qJmVQwCHHKsIrf3SPaHzQYyz7AIq4u864Qau7IXwkyuLnR4IIpsQE_cPRDhpTkN7Fd8_28-9545uoTiFB6IY_B_DoS4_MryrTihdWbMcwg9-jLzvFgSTDsXPQfPj3iEEOCOlDoee3b-6BiUZNRGGIWumJuE1g87GSdDvZy6DZcPJW5yUp9JPblH3FYgdFfBlOAoOPjBx4QBIAuyAnup1AeTnUAgZTqVsldYW7DaKmY4u7trVGy4heVL-YgJjAfjNFUviOfeQ?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2TLavwZG44YD-YpgAtKJbcdi3DKexZygYTY6CLESnDAFP_rV_KKTgbhy9u5NhpqrBWt7LRk6fRfecVZd7BKANTjPaHjIOQtR_vHxrkS-sIAEByGMpiSeWU0,&b64e=1&sign=bcae2924f53602516efebb5d3e9f4125&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRsF_Zj3BDwyRcEutL0x6JDd9qyQXV5uIQaN7ucz3i2J0O1bCznwSFEONZsTKdYZ-xnxE_PqQrTxtZ0mK8I_6mclKpueX5GZc2Coykn5RJ6qGsu9rnBrpS0UYaovOqw-AjaRKOiBgaRWQh8fVJBrpUXLUToKCALynRNF9vD1-jVTwCvPp_fjbxh00stzwaEZy48dpHpvlkdKK5zPQK2_8ijhRHvjfAnlZMvpxxalV2mCZHS0gNaapwfpdBBft8hBJ80tlH4IiD8-DEv6Se6r2RBHeJ1W4vw1w5Qqrh3-E-ix2_J2zrJ2LgsiJOD7uhU5o61ZxTjhaoMBWz-AYvJPft-O9_DI9QH45y9c324UeaX0XYrZcma0pjjH_tYakQ6H9A2D8H91Y0Roy6zSZ1sB3tJwEzkj7Hlz6jADNsIxndLKCSyi1J6qz1B7582jEFD8feJ-JAfJ5gnVljlN9Cn_fhL3IXwTdoXnNWFEcjxeLlMj4fi2emPQ44iW8V0dDn7sV674hrlk5NwkgI5xSF-1BA_BPe0G6KjNlV2eiVdh96JR2nz2GWVHaDKAR7ey6ayyq0abDCulMvSPDXkVWn1jypByQwICkjOulBf_QO48oKQsAcrB0lkPA_a8JQOy1AgKFsUCWIyAfVcGjYlVGMiJ5jiOYeX2FyodQsGtK6oIoEVrBb6Gl54ikaRfVX9Gdk02twTlo_04fXxquHAmTazqFRd8yF1fO7jkuVYm6lKN6Ty3aUCh1NgXf9tPKFkr_8xz_EL9zYHBQWLUnNnQm6wEwJXFu1r2eBO4IDkf_0PvjgSxV1-kMOGTKrGKyozJlq5h_sz8G9MdBGwVW4NC_wz2styBQkp7tG2drClmUwyYJZ1F3119Ob5KMK7ZrEN3I_YZJCRgnmAvQCOJQ_MeRzj8b2rtl_BRaOayijz1qk0QgDqV5mSXCfuALY1Ulxuxtshmb-hI_jY223LQlLGCOwL4sC68?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2TLavwZG44YD-YpgAtKJbcdi3DKexZygYYsOqjZ-fdV5masobxrUkzPevNgtGXeQmQ0lP-KYkW2G2dtV8cJ_ZVCDNjO0ZIRNspqAhQf4i7FvGH_fCchiqPk,&b64e=1&sign=709a8a84d0efa014b44c62e518f25853&keyno=1',
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
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.6,
                    count: 4118,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 141,
                            percent: 4,
                        },
                        {
                            value: 2,
                            count: 73,
                            percent: 2,
                        },
                        {
                            value: 3,
                            count: 97,
                            percent: 3,
                        },
                        {
                            value: 4,
                            count: 284,
                            percent: 9,
                        },
                        {
                            value: 5,
                            count: 2741,
                            percent: 82,
                        },
                    ],
                },
                id: 58201,
                name: 'Gosso Design',
                domain: 'www.gosso.ru',
                registered: '2011-03-10',
                type: 'DEFAULT',
                returnDeliveryAddress: 'Москва, Башиловская, дом 1, корпус 1, а/я 43, 127220',
                opinionUrl:
                    'https://market.yandex.ru/shop--gosso-design/58201/reviews?pp=492&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 437317990,
            },
            onStock: true,
            photo: {
                width: 600,
                height: 400,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1401054/market_ahMHL4pOb-8jjr9ujRAE9w/orig',
            },
            delivery: {
                price: {
                    value: '0',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — бесплатно, возможен самовывоз',
                pickupOptions: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 1,
                            daysTo: 5,
                            orderBefore: 24,
                        },
                        brief: '1-5 дней • 23 пункта магазина',
                        outletCount: 23,
                    },
                ],
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 0,
                            daysTo: 2,
                        },
                        brief: 'до&nbsp;2 дней',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            category: {
                id: 7812201,
                name: 'Сумки',
                fullName: 'Сумки',
                type: 'GURU',
                link:
                    'https://market.yandex.ru/catalog--sumki/7812201/list?hid=7812201&onstock=1&pp=492&clid=2210590&distr_type=4',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 14933679,
                name: 'FABRETTI',
                site: 'https://fabretti.ru/',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/Ou_dyEZkz4N4ytiHqbbsiw?model_id=437317990&hid=7812201&pp=492&clid=2210590&distr_type=4&cpc=BhysRDWBbu3LS0T8CPIGSGhpVdnVpzHMBdO74LAqvDYWJhgBGaXZKIlu0WJ3OqEg53wS_R_qZudSSwv6PDY7-hpCONiOLhU7ac78PM7DBz_TKmA27YwtbImQUGBwFER59tVXLwUgR0COBuZMbBQY9bezbtJtM3SI&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1608,
                    SHOP_CTR: 0.0004985425039,
                    NUMBER_OFFERS: 1,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_Trivial',
                        value: '1',
                    },
                ],
            },
            photos: [
                {
                    width: 600,
                    height: 400,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1401054/market_ahMHL4pOb-8jjr9ujRAE9w/orig',
                },
                {
                    width: 600,
                    height: 400,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1063985/market_1wud1jBBPnCI6tEfCB9wcA/orig',
                },
                {
                    width: 600,
                    height: 400,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/372596/market_X0b4T2jBUMkkZkDjMczNAA/orig',
                },
                {
                    width: 600,
                    height: 400,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/940013/market_GRwYfsDJXTsOEnAgX1TZvA/orig',
                },
            ],
        },
        {
            id: 'yDpJekrrgZEcEyer99wntdbzEZXpiuykFZs9DRDxnTY3CZxD6L_pOg',
            wareMd5: 'enUjB62VeEFRvKNtxrllNw',
            skuType: 'market',
            name: 'Сумка Vera Victoria Vito 34-613-1 черный',
            description:
                'Закрывается на молнию и небольшой клапан. Внутри удобных два отделения, разделительный карман на молнии и кармашки для мелочей. Поместятся документы формата А4.',
            price: {
                value: '4150',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZvpUslSO5Res_bo_mmZr6G4r5gQfN5oCt3bGe6yiXwKlp6ZsFpFxdSgXdMrO0clpmg9ufV3zYfFx4agqmG9K_AsWhB8j-THLBfT2gXrbWFHr2t0X9O_89KgcPlNzAjdk45OgVkxOLRGUTsiE-WJGQwA3G-6rFtlkR0ogK7XwNzR4uKaQhIajfz0LAcRihnU9uNlqFWx6IWtHE_UJWN6_EvDxQIwTvvC0oUYsFB9TlXdI9hq4Vv8KDyq5k2TNKX4JsmvTIhVmGJKPM6sO9iUrxQJZLruoaREtmYwYDEX-mcNlskpYLKzGd3SJpg2LSxYiJCcZq2MeuBizD9fDvlwiimJJ7Ntb-zHbQ7cjNbPXEhVfpeIBOTcF5pj-tHV9RYtWNfmMwuy7fqFv2WBSh6Nm5QaBlbS7iidwjfaPa0-V0LUayqJ9B2bIAuCVIzNry9lPgXwszDQyD8mYgdYXIkO8Jv10dxr7gYCvyEMIjXYp4gH7aZmMUf98JmndJU21eN32JCUmcRJf0CI68pomEiHPS9eIzRGGsU1QmiKNh7-hLmVMfBdy2dnJY6SA0erf9fZEAqoaAM9JchB-0j_fViBEz-s9atrMbJPhSX3lB8n8scmmFz0juw2Z9dBk6yQRHOKcHtp1-nFMyb5-xYx6R7CwVSZfHx3wbVxmlK0gSB8lFehOfP0Q8VZapETumnUWJbkKdEHCPsQKJFOk1yAyjNQ2hbiDeHAxIaNeFeJgHPD4BOd-e9wpwMYlNxEP6XKKvm-xotm3RZxGPJCNGAWSygCPJbX09HTNwTFWK2QZ5jVUhmUUtVL5KE6bzX1wkuDh6PlQCo3lzNRxXqESKVQysuqhAGRn0x3jwhcnbYD_OEssrz8zW5AtNd8C0HBcgNBy0XwJRuqapt-EXnDtvqN7FJZ2VV-yBRfFsFH_6CST0iWGXvlCxwnsoijvDio7Eb-LoCNO4wA2DkgGhmrAiJJw43YmTsc,?data=QVyKqSPyGQwNvdoowNEPjYcA4Bk7rPDi8lUvfS7ikngijEjgIxvtzhZOKP5aBhHMSQBjOdb2vSfEkXOB8XDf51UXTyWIhvefOTBzaBaqwpHeoAc0I16mmMR9wNYQQBmP6S8oPIAx9ZlY2X4yVymXiBqhKFC3SvRrewLjeohJWwqbhGgGmT006Mvm72mvPPkhbS5vZD5kKLdMEtYso11Nnm1gKgwD4yrZECGfbR45Ds0IsYLvQqHlET9WzadU1c8yMCpiwQJNbNlnev0lScOw31ti-nHDnqMQm-ae9Hhv-9eAfaOGU_mGtZfKZlPNLxZ2V2GAl0M5McU1eGovTl0y6XFc4mdGOWaCMTyFH3OzrugnhUM4aOEuiiJvrfpBp9w3UbsLkD3JCsTkeQfs9hJttuJxOc8i1rZzssXmmrjRHZoooQnoqQlaFg12tESPoO1E7ZfecWbA_V4XY16-ZC0-CHFBwdRBkr5aFmwPZo-OxtUgHfXfdeQ1v7upp1mvLh-c0ZWIINy8WY8w8E9G6IrmiQ,,&b64e=1&sign=4d7c2a086b40cbc6a7fa44d504f0d290&keyno=1',
            urls: {
                492: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZvpUslSO5Res_bo_mmZr6G4r5gQfN5oCt3bGe6yiXwKlp6ZsFpFxdSgXdMrO0clpmg9ufV3zYfFx4agqmG9K_AsWhB8j-THLBfT2gXrbWFHr2t0X9O_89KgcPlNzAjdk45OgVkxOLRGUTsiE-WJGQwA3G-6rFtlkR0ogK7XwNzR4uKaQhIajfz0LAcRihnU9uNlqFWx6IWtHE_UJWN6_EvDxQIwTvvC0oUYsFB9TlXdI9hq4Vv8KDyq5k2TNKX4JsmvTIhVmGJKPM6sO9iUrxQJZLruoaREtmYwYDEX-mcNlskpYLKzGd3SJpg2LSxYiJCcZq2MeuBizD9fDvlwiimJJ7Ntb-zHbQ7cjNbPXEhVfpeIBOTcF5pj-tHV9RYtWNfmMwuy7fqFv2WBSh6Nm5QaBlbS7iidwjfaPa0-V0LUayqJ9B2bIAuCVIzNry9lPgXwszDQyD8mYgdYXIkO8Jv10dxr7gYCvyEMIjXYp4gH7aZmMUf98JmndJU21eN32JCUmcRJf0CI68pomEiHPS9eIzRGGsU1QmiKNh7-hLmVMfBdy2dnJY6SA0erf9fZEAqoaAM9JchB-0j_fViBEz-s9atrMbJPhSX3lB8n8scmmFz0juw2Z9dBk6yQRHOKcHtp1-nFMyb5-xYx6R7CwVSZfHx3wbVxmlK0gSB8lFehOfP0Q8VZapETumnUWJbkKdEHCPsQKJFOk1yAyjNQ2hbiDeHAxIaNeFeJgHPD4BOd-e9wpwMYlNxEP6XKKvm-xotm3RZxGPJCNGAWSygCPJbX09HTNwTFWK2QZ5jVUhmUUtVL5KE6bzX1wkuDh6PlQCo3lzNRxXqESKVQysuqhAGRn0x3jwhcnbYD_OEssrz8zW5AtNd8C0HBcgNBy0XwJRuqapt-EXnDtvqN7FJZ2VV-yBRfFsFH_6CST0iWGXvlCxwnsoijvDio7Eb-LoCNO4wA2DkgGhmrAiJJw43YmTsc,?data=QVyKqSPyGQwNvdoowNEPjYcA4Bk7rPDi8lUvfS7ikngijEjgIxvtzhZOKP5aBhHMSQBjOdb2vSfEkXOB8XDf51UXTyWIhvefOTBzaBaqwpHeoAc0I16mmMR9wNYQQBmP6S8oPIAx9ZlY2X4yVymXiBqhKFC3SvRrewLjeohJWwqbhGgGmT006Mvm72mvPPkhbS5vZD5kKLdMEtYso11Nnm1gKgwD4yrZECGfbR45Ds0IsYLvQqHlET9WzadU1c8yMCpiwQJNbNlnev0lScOw31ti-nHDnqMQm-ae9Hhv-9eAfaOGU_mGtZfKZlPNLxZ2V2GAl0M5McU1eGovTl0y6XFc4mdGOWaCMTyFH3OzrugnhUM4aOEuiiJvrfpBp9w3UbsLkD3JCsTkeQfs9hJttuJxOc8i1rZzssXmmrjRHZoooQnoqQlaFg12tESPoO1E7ZfecWbA_V4XY16-ZC0-CHFBwdRBkr5aFmwPZo-OxtUgHfXfdeQ1v7upp1mvLh-c0ZWIINy8WY8w8E9G6IrmiQ,,&b64e=1&sign=4d7c2a086b40cbc6a7fa44d504f0d290&keyno=1',
            },
            directUrl:
                'https://www.kupivip.ru/product/w19020766342/vera_victoria_vito-sumka?reff=ymarket_msk&utm_source=ymarket_cpc&utm_medium=partner_ban&utm_campaign=ymarket_cpc_msk&utm_content=ymarket_msk&from=ya_msk&partner=ymarket_msk&utm_term=market',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRszj1hXVw4aeKeX5CE5Hdee5PWH2vAChTXMHuXoIA-ZagdCgYNp9f5TKFkFRy_j8s7PaCdhZ2t4vnMnV1r8jFXyTRkrNHru6Rjtu9h40E2VwbLzh3AYspdVwUQzR-Tjy2Z8EvQ3LeuZj4pMAovUMwvaL9pAJ7PSjtt7VL9kw0VwOkX_QCotFCcqUvJDE_feNNNnXBTMmD6yJX-kqumhYhCAmJpGZ_vrPfhWnt_PffRGXL09iFcvalQHxODOBksStUXCU29UgX3GR3dRBcCNn6L1SJ301binj7-Z9uV-_mZbHXXhpd96fpFR_-eJCP24K8mJCiTb0CrwDARPectRhkHz2mIrQEsnI3cR8uqrQiqgcInFIWGr-6FjWoa7x6KbarzpgZQ8N__DUBNp6gPM_9OH-FDRVCzpjF3M3geJnNUHk-eQM4x6zoOq6AspUAaLkub7apAx0OGX8lh6DJEZvITvMGlUmKeBYfOT7EsZNkuADrdMu34J-jdpL81RFcRZmZoMq6N0RAywhxbsy6vL5sf1jJaSXlREQzKQH88Mke5lX6INrF2cCnvozx4k-oFMxgvU4cJ7sb837eURESVeKbPMuNSRGgDuQzkjih1sNl8ID9qnPSL-5Rk8xbH2wcqnkwRd9bovUkb-bzMqcQftfTJZ7mWmwrqYI3HrdHgIyV4Yfh2-aSpp8Lt_zpJej_5I9skXvF0V2Qb972tVEGL78vfcJFyhF5z_LsVDkzNiHykBzSKK-FwJy_Mo5AAyn0dPwRTXC6NlvHLNo_sf6TXqWWPG3UxxOX4wE-c6eq6nd4HWzy2iuV3SDJ1vutWGSTYe5cz7DUnOuD2tASdXQ2gElrlJxt4rbhiY-VDmiHBv6GQY8oyFgZJQZkhbO11bh-SxR-g8n8XGCxZAihe3Q39YcyIW09Orf53lG2LFKypKE-ulq01rmKbxcim72DeSd_ippFrWRLFWJB71RxXlpFMdzBzA?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2QfG-0UIlgwZuWhPMLly9pjB_Xi6B_bh1Q-LmG4UMjDH3jXp39HCnDc-JJ4jNvMlQew4k8frX1TYRzGOACHMYGzLt454PJxy3AkK7p_2WjQUYw1LTGAhmaQ,&b64e=1&sign=3f6ed362ce0caddb63bcecf4b874cd80&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRszj1hXVw4aeKeX5CE5Hdee5PWH2vAChTXMHuXoIA-ZagdCgYNp9f5TKFkFRy_j8s7PaCdhZ2t4vnMnV1r8jFXyTRkrNHru6Rjtu9h40E2VwbLzh3AYspdVwUQzR-Tjy2Z8EvQ3LeuZj4pMAovUMwvaL9pAJ7PSjtt7VL9kw0VwOkX_QCotFCcqUvJDE_feNNNnXBTMmD6yJX-kqumhYhCAwFge10max_tm31gDaUtv__sYd--EuEJzPDMNie8LqTwFv8ZjNdi75QvdksVb9W8GbxWubq3ZfbGJNmVQQhqAeoXZz7DMfJABWPC8dWfu_iycgbpK3h5nDiRgi-uFlgWCzJaT7pYwiZsiq9PVQvd0V38JKpwZrHbnSeMHlNNFyfm7-4vleXRT9QpQMXQOXTbhcJrU2gzhLg16782wBsE_VJlfKtErCFmNxm1p9v8J8_BBMEewIU2ldj40TXPRYp40aZlKHVH8dULRm3wDN6h3fSqPVkZM_8ZBMjj0Pd2xC91Pp6TVXs9Zy7G5qh_UYyGz6CP-LVhto1KnMddK-43C4JaYV2w2tM3uty4SfXqFEUhabCP8pGTL7Zopwo0c_7z3_-taM-lP_JylT6D3kheu5uU6EtVqPSIAoAVNWNq34S3NEF_fIccl6hH3MpM_MTR3SFvsh99BiRJw_ZUzg-gD0HEbyoSxcM7Z4sMzHJ1xU52JKfB5NFq9fUlUzf_hqQcR-Qrr1Hku_OGvLAQXoc6TCpRaZV1F5QP85nlidp84fiGQDpL89wkKzx9ZM0BLAhuccXnuChunvt8hxwTx5-pam0VF1Itj_W4-4zDsho2d7Gtu_PMD3VSPpJ8gvdCFfKpSOS0q_hQWo2uzOjPLY5W5d6BBhCiTeZi0OokaJyKv9QG4jgB3DZnG74GUh4p5baYllp5REl26etMak3ATlVnZ1Tpko6GnFUHMxWoOhYZZDgpuxz1494wJgm9xKtOfsKPR?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2QfG-0UIlgwZuWhPMLly9pjB_Xi6B_bh1X0NRySNqfLqcm4jdWBd6_abvR2viCxFAekTnEGr2nQGi45TaBwLxo7T-LKBDox1-nszZ45llBFpukzdQCy7piw,&b64e=1&sign=91147025bec45772b740c92f4d618731&keyno=1',
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
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.4,
                    count: 20292,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 699,
                            percent: 6,
                        },
                        {
                            value: 2,
                            count: 223,
                            percent: 2,
                        },
                        {
                            value: 3,
                            count: 385,
                            percent: 3,
                        },
                        {
                            value: 4,
                            count: 1675,
                            percent: 14,
                        },
                        {
                            value: 5,
                            count: 8671,
                            percent: 74,
                        },
                    ],
                },
                id: 302690,
                name: 'KUPIVIP.RU',
                domain: 'kupivip.ru',
                registered: '2015-07-24',
                type: 'DEFAULT',
                opinionUrl: 'https://market.yandex.ru/shop--kupivip-ru/302690/reviews?pp=492&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 463172767,
            },
            phone: {
                number: '+7 (800) 100-44-99',
                sanitized: '+78001004499',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZvpUslSO5Res_bo_mmZr6G4r5gQfN5oCt3bGe6yiXwKlp6ZsFpFxdSgXdMrO0clpmg9ufV3zYfFx4agqmG9K_AsWhB8j-THLBfT2gXrbWFHr2t0X9O_89KgcPlNzAjdk45OgVkxOLRGUTsiE-WJGQwA3G-6rFtlkR0ogK7XwNzR4uKaQhIajfz0LAcRihnU9uNlqFWx6IWtHE_UJWN6_EvBJ5bW9EBBO8Iu661pH7OHxk_nxA7hAaTFuAZq105cADuEU9vmWZMjwqxpUDEFZkXtByAilfKLNVxzSVe-c1C5MQ9A4Zkf2cytHSdcFOClT_WoPPA2sJP9gyAX1xsv-rxJA8ZFGmt5qvYOL89ejnqHs34MLwGuTzPh__r9gRfO9JzxfAOljwU2MkjVrW_M_RlsRJ7zOncC6SWJUqTXUSdzZJRKCx3x9AEqmJENPCaCKV5FaIltWhHfctuyMJzMbBRn0H-Ao8fSi-xX95jSuZAsu3sQd4Ur7lCKXoPMmnSIyhK15boEOd10DkXuky7MHVzLsBlEHfgHt0MmToPEHHY0ZPBleal_tzyWDlA_hQCmMSk3KJo4cesyHWbWDnjcailQisb7L3uXIVgT0OE6s-vxJHSizEB0g0708yGszo_zBEUZrC0gePqBGeEy6G56X-IHOVFuhEO7Z9BT1Chy3iXK51_2qiu1UZItBIhL9-5seAdkpO5NBVa49y0hKZ9Ccxlsdf4W_lR3qOEHylR8EKhTt3fQDTqM4pbp_rstzJl3E2xJHc_NfHEa1vEhwBLGFviHCWlOVgmQ_JQdq0cVY2kDCiZFp71Y6drz_muawGs34YrZtvsHEY0w-Y_rJsBO_lCJxmwfMzqhlk2xbEwZP9vvvHvrY2DezcbKfcwYK6wouTdO1g_BMjghZsdea1dvZZxOyHD_hnamQgoPDT4nnB37NljVYvm82NAgjauS9jzH36wH29saIEyYtnJ3slq0v0Tg,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-RS4yHk8oFow6AvlbCfkvy9alQc5_jtnA8f0Sct0rPhG8OlCbV84rhTTKXwoWQrcMGg4v73PWIfB5n_-HkDLPefdqGJ9A8UvV3KiZpT_deWPCzFRjfKTU8onTkGlVqkjiuj9Ar6DSXQELGODduAl1b0ByMk5F6rc1d-pumf4pigw,,&b64e=1&sign=feb6fb8e52e24332c690c2df077d8e21&keyno=1',
            },
            photo: {
                width: 533,
                height: 800,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1886598/market_4uEEmJD_UFT_4WAxDmUQ0A/orig',
            },
            delivery: {
                price: {
                    value: '350',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 350 руб., возможен самовывоз',
                pickupOptions: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '199',
                            },
                        },
                        brief: 'Срок уточняйте при заказе • 13 пунктов магазина',
                        outletCount: 13,
                    },
                ],
                inStock: false,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '350',
                            },
                        },
                        brief: 'на&nbsp;заказ',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            category: {
                id: 7812201,
                name: 'Сумки',
                fullName: 'Сумки',
                type: 'GURU',
                link:
                    'https://market.yandex.ru/catalog--sumki/7812201/list?hid=7812201&onstock=1&pp=492&clid=2210590&distr_type=4',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 9368760,
                name: 'Vera Victoria Vito',
                site: 'http://vv-vito.ru',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/enUjB62VeEFRvKNtxrllNw?model_id=463172767&hid=7812201&pp=492&clid=2210590&distr_type=4&cpc=qGjEUbQF4Qc-jwWvGK7O9rxziCNMV8WFCjuxoR9LbF5XEB6MWtX0mvQv_JXTRbp1-93Bvf7HtrQQzUKYCEM5q9rTubiR1obyruEfiRY1_1R4qpsbPYoD___EJsAwDzTfdzHKhT_-r5T0UL3Sg0IkjlJSDFU-S1KF&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1608,
                    SHOP_CTR: 0.004952660296,
                    NUMBER_OFFERS: 2,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_Trivial',
                        value: '1',
                    },
                ],
            },
            photos: [
                {
                    width: 533,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1886598/market_4uEEmJD_UFT_4WAxDmUQ0A/orig',
                },
                {
                    width: 533,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1617404/market_XeS83DGwgmnyrx7OdA53HA/orig',
                },
                {
                    width: 533,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/988047/market_54_fmDg68vwG80DfjZdhlw/orig',
                },
                {
                    width: 533,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1774236/market_Ay25u2UJJCYl0SZHNm7EjA/orig',
                },
                {
                    width: 533,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1844764/market_WLQGSfgxWC7-N8gm_oVlSw/orig',
                },
            ],
        },
        {
            id: 'yDpJekrrgZHIY_1P--jUuw9SPZMDoIyXiEZyH8xqzEhwXj61zyhnMA',
            wareMd5: '6wN5FBHE34eAKtd1x7yH3g',
            skuType: 'market',
            name: 'Сумка Beverly Hills Polo club 657BHP0603 DARK BLUE',
            description: 'Сумка',
            price: {
                value: '4990',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZrU4-UvCX22fQE9WeVOxN7_TPq3vgKXc2_MgrYuzcZzHvk51pk2atJo8O6awOsx1TawOjToxFMLxwU81Rcy40Lgql8s4NBGmTr7x2qa4J38FqyFZKT-NnNZ4QzT3BCNf9UmM_p6jIOhiIKn7HHL0vFDy7J5Z4kPbgGxTNK4QiSjiHcn3C0z33UZHmLh_qh8Z7XPTVq_QY5ap64B3_YaSd4G7PMsIVnOl2Psv_zusC-BAm9vU2rUpw0NCxqu3G0_n3gAus4K4Q2OCBHi-Sr_cGfWca-kswt3qk_zo0mHbtnUwxh0LtUxz6QCfBdKoIoCTLc6q0yXmTNMAsC18-LZUrYuzLuk7RNknZqSzLeVAneoCeK8nyGZYkkJ82V4nvp83rTfAxRD3W5o-yIZI64keUzqnJQ2lxmCKuUbJNJ0wAkjXf53bQA2h6XSY9J5ose1zylTjm_t3NOkuUGKvmsNkc-HzUBajI_BxnXHMRod6PFOTu8UxIirEO_KwMKYyhatWWugAi94RKiLD-DctZJRA0Mf5YWfBDZNsPfboKJXkTqGLi2tLutJbI4e8s7xw8ZlXE6lC1XlWwbWv8-G6NMJHK9vXHK5-ZI3Y-5RnPv-rl3aRp6vuYN_ZvbEuxoZDHPPWmbeoiIxR6u0_k3lIVgTOH3HhMCLyBFVyFyl-ztR6a6qu8KycAbb7RZE5uAJiSsi8Hcae2ZVKdWgvNmIBM9xzjKDOLwid5dLSwGzT-Lr_4OMAoKJ0GLKQt9M4JwsTYpe_rH27hIP2WwYh3H2_bTsHYplFT8qnqFy95v_hhGN_2eaABWWO9ad91CixyDlXVSRkUX5ey1xv29nAVYL1w-mbjj9VRN-xU8od6OC1ELGDT8hsu1AgpWobHGO0eUzUIAa7vL-81qLdCzNTUypxthKrjU8Iq8ejM1ZRZvJZFsQ-iDo4M31c7YdykBx-LkqxZK6r4IQeW7SMIRexSSvnrTx7BvE,?data=QVyKqSPyGQwNvdoowNEPjYcA4Bk7rPDi8lUvfS7ikngijEjgIxvtzhKsqkPTS5gDSAEggh5GZTWeWnLZ7vRHZpEcGW7HOQEn7HmsjtQAIIBbyb-ednMzJmvRGC6qucm3kw73TxHrEVAw4xjXsKsELJUV7P2nw0Y9vdq41wkIMqTh_NhoKdmTuvUNqV-sPhdWKlfjuxY98dGNWFY4Q5fi5L6STuSWSU-dt1OdSNRQvDerKyXzbM4LFzhDfkgZs6nqI4kyz86KZuJ_3CMnGDcId502Zc_5nSYWkWLVF1lFT9R4hIRu2LA0K6R9Mt2jhpuBF3r-fBnmVLxQywAfYRLl7w1u9aLjeggAAszBSRYGus74W_wbiT13P8zaLp7fos-4HCwbva8aeLu2M7shpQlon5HrPdFkYzE5ClZ-uYRmMh1P3T0pNkrShMV7rOLJ98dTi-TqrvhUB8l2qW0Xyg_1ermrfIDjdF4db5-buFOe_8bhysYSTwdrsmzwYt7Y9uHGhdwxoWTNWPuY7cAiuXQcee2RJ-TtivzI&b64e=1&sign=618781ceda03de3b8c10a08e8db52c66&keyno=1',
            urls: {
                492: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZrU4-UvCX22fQE9WeVOxN7_TPq3vgKXc2_MgrYuzcZzHvk51pk2atJo8O6awOsx1TawOjToxFMLxwU81Rcy40Lgql8s4NBGmTr7x2qa4J38FqyFZKT-NnNZ4QzT3BCNf9UmM_p6jIOhiIKn7HHL0vFDy7J5Z4kPbgGxTNK4QiSjiHcn3C0z33UZHmLh_qh8Z7XPTVq_QY5ap64B3_YaSd4G7PMsIVnOl2Psv_zusC-BAm9vU2rUpw0NCxqu3G0_n3gAus4K4Q2OCBHi-Sr_cGfWca-kswt3qk_zo0mHbtnUwxh0LtUxz6QCfBdKoIoCTLc6q0yXmTNMAsC18-LZUrYuzLuk7RNknZqSzLeVAneoCeK8nyGZYkkJ82V4nvp83rTfAxRD3W5o-yIZI64keUzqnJQ2lxmCKuUbJNJ0wAkjXf53bQA2h6XSY9J5ose1zylTjm_t3NOkuUGKvmsNkc-HzUBajI_BxnXHMRod6PFOTu8UxIirEO_KwMKYyhatWWugAi94RKiLD-DctZJRA0Mf5YWfBDZNsPfboKJXkTqGLi2tLutJbI4e8s7xw8ZlXE6lC1XlWwbWv8-G6NMJHK9vXHK5-ZI3Y-5RnPv-rl3aRp6vuYN_ZvbEuxoZDHPPWmbeoiIxR6u0_k3lIVgTOH3HhMCLyBFVyFyl-ztR6a6qu8KycAbb7RZE5uAJiSsi8Hcae2ZVKdWgvNmIBM9xzjKDOLwid5dLSwGzT-Lr_4OMAoKJ0GLKQt9M4JwsTYpe_rH27hIP2WwYh3H2_bTsHYplFT8qnqFy95v_hhGN_2eaABWWO9ad91CixyDlXVSRkUX5ey1xv29nAVYL1w-mbjj9VRN-xU8od6OC1ELGDT8hsu1AgpWobHGO0eUzUIAa7vL-81qLdCzNTUypxthKrjU8Iq8ejM1ZRZvJZFsQ-iDo4M31c7YdykBx-LkqxZK6r4IQeW7SMIRexSSvnrTx7BvE,?data=QVyKqSPyGQwNvdoowNEPjYcA4Bk7rPDi8lUvfS7ikngijEjgIxvtzhKsqkPTS5gDSAEggh5GZTWeWnLZ7vRHZpEcGW7HOQEn7HmsjtQAIIBbyb-ednMzJmvRGC6qucm3kw73TxHrEVAw4xjXsKsELJUV7P2nw0Y9vdq41wkIMqTh_NhoKdmTuvUNqV-sPhdWKlfjuxY98dGNWFY4Q5fi5L6STuSWSU-dt1OdSNRQvDerKyXzbM4LFzhDfkgZs6nqI4kyz86KZuJ_3CMnGDcId502Zc_5nSYWkWLVF1lFT9R4hIRu2LA0K6R9Mt2jhpuBF3r-fBnmVLxQywAfYRLl7w1u9aLjeggAAszBSRYGus74W_wbiT13P8zaLp7fos-4HCwbva8aeLu2M7shpQlon5HrPdFkYzE5ClZ-uYRmMh1P3T0pNkrShMV7rOLJ98dTi-TqrvhUB8l2qW0Xyg_1ermrfIDjdF4db5-buFOe_8bhysYSTwdrsmzwYt7Y9uHGhdwxoWTNWPuY7cAiuXQcee2RJ-TtivzI&b64e=1&sign=618781ceda03de3b8c10a08e8db52c66&keyno=1',
            },
            directUrl:
                'https://www.kupivip.ru/product/g18050303286/beverly_hills_polo_club-sumka?reff=ymarket_msk&utm_source=ymarket_cpc&utm_medium=partner_ban&utm_campaign=ymarket_cpc_msk&utm_content=ymarket_msk&from=ya_msk&partner=ymarket_msk&utm_term=market',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuE8uVOTZFhETV-J3k6ko9Jzumy1dKrEIaRdTxzDDj7z2dV2ZVYqZfW-4i7gXwhaKy2fWYInd205OwU_BHQUHLPTZGdxtD6WTLiBkS706z8IQ1l8L_rGT4Zk4oWMorPMgr3xYLwqcm0_ufQJ2dWgHbrym590nLb-_9oKVnkvuZH4NKvEDblFLycRRwxpwwp80V2msO6u0yGyaJVgd-sqXVlwpOqVkCE93zVrCsElMGiDSnMHrtOfS2KpdSliSOrRf9B5eiOTYzYtfMLq8GjLdAuQpBSnY6UxdwFHAgwZeLbrsT8KKXtjGGCLYL4AoGmCy298y0dMAjLLGHMr0yXIyqdZgb16-m5UROLyhhTsoI5R_yb_HRGAt25z47Z9r8_4WJ6UEOrCKG30nPBY9eTE-mjBZ0lDJomEadbX1TQ2-caEEU1gJrF0V1iZkUFBuMo4GDlU0cAzQmWNgIU7JcDGLTgQ4WMax1Mrw50T6wokmoz_heMx1dBQ57G9oC7OKuRxJ8d_8Ac8oNDA3cv1leRKgeGRdflgxpKrxuoxaoRB5Ebpi0z5S7z-s3F9pZtZHdWYMc69y3DKElcNIr30pvtkV0xOWhnvkmgyEhQtLiWf6vXsuEIERuf9kQCg9B_8ipIUz1dp7e2REhLefj9jiCrFq_1ZOKr7q8qmvXQnZWFB0tTISSvlGeFPZACwQ1YEvao4c6d7C_TMK2yrAtgBmDacxg3-5bcKkPpfZAsMfRVxnz0BauKEtIq_pAW_xiO4Wo1iQnhEWbhv-XLXRkdApdtxp5AYQAfYhN-G3qPPlz2dnQU7YHmPbIIsAIrgtD0gFx73Fz50Wd6otJ7hr83Dv_p4pumE4XZOds4fDQaVTOGSmpUTDRfAvA6UqByzzW2vNlLOzmwCWo7yl4yCgBHrhEmZS2buxVeMwAfstMg-TSfJqQZ133wFDgD0WZsueRo179g8iv0uJdmSI0jZWzYi3EZhEUA?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2QfG-0UIlgwZuWhPMLly9pjB_Xi6B_bh1Q-LmG4UMjDH3jXp39HCnDfjgAzQlNJg6pLCY6a4r27DGa4t0Pel-BkGBfL5wfhsTDCt38_qkFhLV-eCj3TQ5_Q,&b64e=1&sign=ebe42ab18aba41bf0654a3a09bb2870f&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuE8uVOTZFhETV-J3k6ko9Jzumy1dKrEIaRdTxzDDj7z2dV2ZVYqZfW-4i7gXwhaKy2fWYInd205OwU_BHQUHLPTZGdxtD6WTLiBkS706z8IQ1l8L_rGT4Zk4oWMorPMgr3xYLwqcm0_ufQJ2dWgHbrym590nLb-_9oKVnkvuZH4NKvEDblFLycRRwxpwwp80V2msO6u0yGyaJVgd-sqXVlUeepzezwE5XLFRZmBRmiQY01KY6KxOdourc6OaNax__C_0nGDATSc8H-TRWD_l2abGLFJSntlhovQwzOFGvC0jcfUHR1bdG6Wrbo0PmrRddZhtIqvQ6TNRLFbRpRv6LWzhsGHHqHUmTftcMHaFuEe4VgQo6bnfRH-7DCCcBW9rO1HD_uRx1Jlw-M6zM3SUDY2aB9U5l5wqqxK_IFcI4pbXcPHwXA8eSQ6j65-TJooZm87Alb5i5AiG9yh7u50iEJHTZCaXaA8t9RvOOmQtPpw4pPQat4ZAZ9a1t84v8OFYY8wM9_z7daxslvJCinN9yOyICs_ve4tggy_G8JmB6S-UdUAUM4yPj3ByfKimsCWdnLX3nHPAaYm3P-zpcrCAyG5e_cgzZCnoDZGwThZqKhGzr2BvMUXLvXfSqQDIYlmDNSjh86pDFOa44qCQtLxJws2vJOxxtD4a-WuyXu9KzOj8qVUrVPIWReNxceAR5sgdnphR8DZXsTzf0vwlPa5MS9gbBOKc5UJCinE6qOuOytxdJR-srEq7-xNPjU6-0JZFE1DDx2E5cRjXadVL31Q6WSRPg78aH5n-7MpjWL8m9Upthg4zlcO6ZYwlscMs9zUnfMVIgnH8e05Uf_QWxS_8_XDzkfup_RY70tDJfsWD1NTz5P3bKtLnnAqsYBbmJw7O3FoWUAI14o1OQp_T_4MuajsFydx4ZEcdfnonfIufe6jJbGGSWMRNtzRPPrxkLQOaup0HV_A5pERXtfhaduDgVO?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2QfG-0UIlgwZuWhPMLly9pjB_Xi6B_bh1X0NRySNqfLqcm4jdWBd6_ZQHvmR1VaLwCniGcAIjVMC-62coLAMVmdSjFYZ3cE6KkInxv0wfy7_lfRuVV7Ev8Y,&b64e=1&sign=73df2610efec21beb6f76953c1cbd28a&keyno=1',
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
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.4,
                    count: 20292,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 699,
                            percent: 6,
                        },
                        {
                            value: 2,
                            count: 223,
                            percent: 2,
                        },
                        {
                            value: 3,
                            count: 385,
                            percent: 3,
                        },
                        {
                            value: 4,
                            count: 1675,
                            percent: 14,
                        },
                        {
                            value: 5,
                            count: 8671,
                            percent: 74,
                        },
                    ],
                },
                id: 302690,
                name: 'KUPIVIP.RU',
                domain: 'kupivip.ru',
                registered: '2015-07-24',
                type: 'DEFAULT',
                opinionUrl: 'https://market.yandex.ru/shop--kupivip-ru/302690/reviews?pp=492&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 537012366,
            },
            phone: {
                number: '+7 (800) 100-44-99',
                sanitized: '+78001004499',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZrU4-UvCX22fQE9WeVOxN7_TPq3vgKXc2_MgrYuzcZzHvk51pk2atJo8O6awOsx1TawOjToxFMLxwU81Rcy40Lgql8s4NBGmTr7x2qa4J38FqyFZKT-NnNZ4QzT3BCNf9UmM_p6jIOhiIKn7HHL0vFDy7J5Z4kPbgGxTNK4QiSjiHcn3C0z33UZHmLh_qh8Z7XPTVq_QY5ap64B3_YaSd4FJ0k1XeMMOH4WpNUIkYf7ipa7aDzZkWEXmQwMk7UOd3zxxhGKlR975EB240LcVmuF8CNJr_67Ofy4x6VF-klFxtNdUOTHjX49HnzABtPHYMcX1PYIYNwnib4Ulr8XyFfPklTgTWC_GloDJWreEFH-sCBsc9FI1UZlgpoLgqRGhqbWORAvMjzuZR-lESsBQFrM4i8EX3MC8UMTvw7Pai9lOT7baLJuYkIexc-4tJ-XZ3GMzMzZNpQZN_SLtdXUBIad57Ru1GWkKK6J-VC4hlyKgxflMbP8u9GLzetac3hEoH1TeIb4N8uayQjHZapTgd0pDw_9cI6HfAwR3a7Pd7oECPGJp3YPd_y1pXh1IrswUmUBmUbuAWWOa9us2ZROld4g363JwUoF-ZmwPjlIsJ7_Gx_ILQxq4zV1HVGWjhgBr5w3H9wEYx2kRh1BaZ4Yd8i97pYhubb0429cVgTvH0y58KaLV5YDOkV_j62uPRYZ574qwRRRldL12Qaxn_weIGeg8FzBbwGCn3U-sqQG5CDB3k546_41eWXCTquv8EGQKeHwXkVQG63CkIsIN6LI2Y2XSlmx3M8QZ41MqnhiuqZfY9t7GmFwmIodQbUOlw8hzJOJUOHl02WkZ9wvQBawZwNPPCyF1fan9b1KkAVTUNhvqx2D4fsup0R095wpBISF9P9Eo1MyGUOtcD2T-UYmhfInJWE3jhIfGgpDeacJQS6qTO8_x0O3fLxZJQDNVDKPMXFuXFpDbeUJzHhgj-isma0g,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8ZXnPvTt6DTHZRwVgqFiQR5SJxYcEMrJNwgVz5t275mdtbqZIh9V60tfUHpJoHy73xGKsBgvG6I7FRCz2UTYAnedjlwq2g77jvi6__6GxXkmRHETCXoMfm56Ohvkc3Inm8dEJPOIWevEFoAaL7eMSsRsUgfY569CzYqa_wQCLP4Q,,&b64e=1&sign=75b6ac9185c44c43faf93f30da08e941&keyno=1',
            },
            photo: {
                width: 533,
                height: 800,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1488950/market_uirw9UsG7JGhqP9a5XfemA/orig',
            },
            delivery: {
                price: {
                    value: '350',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 350 руб., возможен самовывоз',
                pickupOptions: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '199',
                            },
                        },
                        brief: 'Срок уточняйте при заказе • 13 пунктов магазина',
                        outletCount: 13,
                    },
                ],
                inStock: false,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '350',
                            },
                        },
                        brief: 'на&nbsp;заказ',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            category: {
                id: 7812201,
                name: 'Сумки',
                fullName: 'Сумки',
                type: 'GURU',
                link:
                    'https://market.yandex.ru/catalog--sumki/7812201/list?hid=7812201&onstock=1&pp=492&clid=2210590&distr_type=4',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 8444586,
                name: 'Beverly Hills Polo Club',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/6wN5FBHE34eAKtd1x7yH3g?model_id=537012366&hid=7812201&pp=492&clid=2210590&distr_type=4&cpc=NoDs-hOevCb147vNj8XO1tL2U1Osd8r08pVuQnyu0B0tqIPCQdLEpn13NeSxhh4CWLC4IjF4_dgIw7MPavBRuU2_kbjbhCWax9CB_BaHPcZouai96Cby9pdtkVGh4jvs5JyilNFTbJvnCcqVlftDz8r71JTQSjQp&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1608,
                    SHOP_CTR: 0.004952660296,
                    NUMBER_OFFERS: 2,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_Trivial',
                        value: '1',
                    },
                ],
            },
            photos: [
                {
                    width: 533,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1488950/market_uirw9UsG7JGhqP9a5XfemA/orig',
                },
                {
                    width: 533,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1904635/market_m1JePKunlzWGCl8YwJH5Kg/orig',
                },
                {
                    width: 533,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1867110/market_dJSyADug5vZ4wB9TsL_KXw/orig',
                },
                {
                    width: 533,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1885123/market_qfFmiJkrHyXwjS-2QW7BTg/orig',
                },
                {
                    width: 533,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1451847/market_GC5ZOON4UkTRzVVWmlTlBg/orig',
                },
                {
                    width: 533,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1769837/market_SjeQF6RtkkA_5_B5qckeTA/orig',
                },
            ],
        },
        {
            id: 'yDpJekrrgZG2ViOnWTSikFCIUSLYpLpL48fhf2M0bsqA9HTsoFR6Yw',
            wareMd5: 'ohzfmRzSMZfo1eAtuh2LrQ',
            skuType: 'market',
            name: 'Сумка из натуральной кожи A.Valentino - 439 темно-коричневая',
            description:
                'Материал: натуральная кожа Подкладка: синтетический материал с пропиткой Размер (д*ш*в): 35*13*24 см Застежка: молния Отделения: одно Карманы: Снаружи на задней стенке 1 карман на молнии Внутри сумки карманы: 2 - на молнии, 1 - для мобильного телефона, 1 - для мелочей Высота ручки от верхнего края сумки: 14 см Съёмный ремень: нет Доп. информация: дно жесткое Страна дизайна: Италия Производство: Россия',
            price: {
                value: '5890',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjmVx6S3mCjDCTO2xaX39eWXdmGJuYJJT8kAgFJUoeT7MTD6JVrSMewDmxS06gG2QJRGf6PXHdVRn2617UWJrbGZVznv2gE5rjeBAEpizxakh9CMcxmjQk5lqm0w0zysxoY2mPVtYOd14wjN4--ov6VWFH2zf7baQL6sHuq55in1xS7lTQZvpxPpEe0CXbJxeesTOik3wHXR0a4Buen5Lmdt1lwc_9Q0Dk2ugtGXx1nJZW4eo0Yhuo3IRZFrQszaKY4qCBV878gA2czpPT6o4mIiVCKBz26KgVNjKxe2KIYFlzoIpEi5BQ5RlTjFXqWt3pKK80dYuc5gY-71Db9REGupeaUOQ5oHcp8ZGGToUML1H_vlcIov9Neav9VN4oXnVTWkQBvoUO803WXiLb6ILbzJOQOD-iVn4M6qCQLm6z5j75ZfXuTOcGCUWH8VFTL1Nti0HdvbTFqD-4qm5GUu-Dh0KP0Ft5xRPPqhk0ufKy7EAdgLnkIqNshTcSpO4Pgo93kkkfpbq8zCE-bWDyrGXsVeE3vPZJ6nqFxo7xFgOzP6r6_T7XQJBairHJjepnyQxc2w_1xFGtWS3rmXox3003CBvPsRi2YSnCWzu8otOdpZMD2lHfBoh-ZPN8Vcc8mMHYrw2Cfn_SW_0VMAK49uhpCSy2alq2ieftoDlQ158yvN7tyIugq34kO9E3KMzCEHO3OnHaapIEIjIp8ivLdXzzGKg4iERLa0GsALLkOhIOlSvuXIt4gHmqubDeL6CLh5imaNpYuguF1HaFr6_M0p9Hd_1TtCBGo_tfInzefy7tqF9haVTsm3ypwoY4r15nHBTnKnxrnUkKqiVPZRdMtx4tfXquZklMUTBSUg4ivWLQTOkwEVfZuLWfJUtbtIxF55hz2_MSCbzrhs5jUjBb0h5vt83g7qORYeGfnpX88NsvU7sa3NnRzwiHzJmTevsoDcj0LKPKdIv3UE?data=QVyKqSPyGQwNvdoowNEPjW5vphJhZMGAN0z-jIfd6E5zDa_FpMcRZhfSHyT2RZqj6A_JiLdsqc-x6jADiqaA4dcUqR8h3REd1VAqzW1wG26nPYNpiss_VuLVjTOWdsttkxtLRm6-3wySZkEX24GnK2bES3elQB2lXNijDKllyEhN7zZUhngjdGZLa0fYC6npn7q0xSFBLeAIHu2uLk9_B6TspyYoMnoVNm6nZYVkALdrS5jVFItL5cxFiSzbVZoidwpQFZiPSVOAWrXT7jpNIvSA2KrfBUVZMg6CRkdvrE1GKM8w4So6T8aMkkomPsIu0fIxaHcqhw7NGzzZIK5elquz72fz-yKk4zOopChinbbqpHxHfzO_Y5Fr2rXj7sB4_tj-FCvTngeT21Qz2t5E1jfgL7dT1d_6dnAUs9K2wR_fLkfw1mHm1-xibEqAHZPlDvUReuFWkR35mqBdOyjk_cpNWZvCTrPGjo8sCpGvrWSQfd1_nuld4YXpRQbaqD8nDU6uaV5GpYq0EWMBLrhda5J1P1s9VPvO58hbT8FfqMMpZKa2erTmGDIuphmrRIyRTZ4ve8Gkz2HSfGnrCDJ4lPBHDei6qI0YvEjsoiWdwLD3aCf5I6q7LEY5vZ2M90mLqwWnK9LddxOQwvEgNGp9WA,,&b64e=1&sign=026595dcd505cfeebe774ea4586038af&keyno=1',
            urls: {
                492: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjmVx6S3mCjDCTO2xaX39eWXdmGJuYJJT8kAgFJUoeT7MTD6JVrSMewDmxS06gG2QJRGf6PXHdVRn2617UWJrbGZVznv2gE5rjeBAEpizxakh9CMcxmjQk5lqm0w0zysxoY2mPVtYOd14wjN4--ov6VWFH2zf7baQL6sHuq55in1xS7lTQZvpxPpEe0CXbJxeesTOik3wHXR0a4Buen5Lmdt1lwc_9Q0Dk2ugtGXx1nJZW4eo0Yhuo3IRZFrQszaKY4qCBV878gA2czpPT6o4mIiVCKBz26KgVNjKxe2KIYFlzoIpEi5BQ5RlTjFXqWt3pKK80dYuc5gY-71Db9REGupeaUOQ5oHcp8ZGGToUML1H_vlcIov9Neav9VN4oXnVTWkQBvoUO803WXiLb6ILbzJOQOD-iVn4M6qCQLm6z5j75ZfXuTOcGCUWH8VFTL1Nti0HdvbTFqD-4qm5GUu-Dh0KP0Ft5xRPPqhk0ufKy7EAdgLnkIqNshTcSpO4Pgo93kkkfpbq8zCE-bWDyrGXsVeE3vPZJ6nqFxo7xFgOzP6r6_T7XQJBairHJjepnyQxc2w_1xFGtWS3rmXox3003CBvPsRi2YSnCWzu8otOdpZMD2lHfBoh-ZPN8Vcc8mMHYrw2Cfn_SW_0VMAK49uhpCSy2alq2ieftoDlQ158yvN7tyIugq34kO9E3KMzCEHO3OnHaapIEIjIp8ivLdXzzGKg4iERLa0GsALLkOhIOlSvuXIt4gHmqubDeL6CLh5imaNpYuguF1HaFr6_M0p9Hd_1TtCBGo_tfInzefy7tqF9haVTsm3ypwoY4r15nHBTnKnxrnUkKqiVPZRdMtx4tfXquZklMUTBSUg4ivWLQTOkwEVfZuLWfJUtbtIxF55hz2_MSCbzrhs5jUjBb0h5vt83g7qORYeGfnpX88NsvU7sa3NnRzwiHzJmTevsoDcj0LKPKdIv3UE?data=QVyKqSPyGQwNvdoowNEPjW5vphJhZMGAN0z-jIfd6E5zDa_FpMcRZhfSHyT2RZqj6A_JiLdsqc-x6jADiqaA4dcUqR8h3REd1VAqzW1wG26nPYNpiss_VuLVjTOWdsttkxtLRm6-3wySZkEX24GnK2bES3elQB2lXNijDKllyEhN7zZUhngjdGZLa0fYC6npn7q0xSFBLeAIHu2uLk9_B6TspyYoMnoVNm6nZYVkALdrS5jVFItL5cxFiSzbVZoidwpQFZiPSVOAWrXT7jpNIvSA2KrfBUVZMg6CRkdvrE1GKM8w4So6T8aMkkomPsIu0fIxaHcqhw7NGzzZIK5elquz72fz-yKk4zOopChinbbqpHxHfzO_Y5Fr2rXj7sB4_tj-FCvTngeT21Qz2t5E1jfgL7dT1d_6dnAUs9K2wR_fLkfw1mHm1-xibEqAHZPlDvUReuFWkR35mqBdOyjk_cpNWZvCTrPGjo8sCpGvrWSQfd1_nuld4YXpRQbaqD8nDU6uaV5GpYq0EWMBLrhda5J1P1s9VPvO58hbT8FfqMMpZKa2erTmGDIuphmrRIyRTZ4ve8Gkz2HSfGnrCDJ4lPBHDei6qI0YvEjsoiWdwLD3aCf5I6q7LEY5vZ2M90mLqwWnK9LddxOQwvEgNGp9WA,,&b64e=1&sign=026595dcd505cfeebe774ea4586038af&keyno=1',
            },
            directUrl:
                'https://modasumok.ru/sumka-iz-naturalnoy-kozhi-a-valentino-439-temno-korichnevaya/?utm_source=yandexmarket&utm_medium=cpc&utm_campaign=Modasumok',
            shop: {
                region: {
                    id: 65,
                    name: 'Новосибирск',
                    type: 'CITY',
                    childCount: 10,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 5,
                    count: 14,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 2,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 3,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 4,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 5,
                            count: 7,
                            percent: 100,
                        },
                    ],
                },
                id: 306354,
                name: 'ModaSumok.ru',
                domain: 'www.modasumok.ru',
                registered: '2015-08-19',
                type: 'DEFAULT',
                opinionUrl:
                    'https://market.yandex.ru/shop--modasumok-ru/306354/reviews?pp=492&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 1812754746,
            },
            phone: {
                number: '+7 962 829-51-93',
                sanitized: '+79628295193',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjmVx6S3mCjDCTO2xaX39eWXdmGJuYJJT8kAgFJUoeT7MTD6JVrSMewDmxS06gG2QJRGf6PXHdVRn2617UWJrbGZVznv2gE5rjeBAEpizxakh9CMcxmjQk5lqm0w0zysxoY2mPVtYOd14wjN4--ov6VWFH2zf7baQL6sHuq55in1xS7lTQZvpxPpEe0CXbJxeesTOik3wHXR0a4Buen5Lmfg7uCAibSojJDTzfEoi2PYgzfMIbWdHhpDXPcDErHPeBtoS_t3CsfTgBbbxx5GKH39NNymXFG7QbW1N7Yx-kMlHo0nRG_8fRZhSbwdDn_bp3r_SdoFu1wc3ttQwrz6AR5EJd0X2kDAOhNmhezcl9f4eihGSWvk4bM1NgeEuU7GRuVg_XvkhD6RrWTQOMqaHspuExI6VYSKGviLS6_4iT6Ag5qhYrJc1XUhIhCSmDdO0rJwZ556MXKy75tplEWoXhp5IsPuEh4q7ILKQcMXnxghCK7raIi8uszaiGIIWtj0AmXIoOyZaf_3zL92PVstaaOYlDBB0CJF2p2k_5Q9DGR5RIgQ_fahKHk7YBFbOGhZw6EEkkqpkioegC7WeQRFTFD_nVCk9PXMc5QuiW_K5Voel6Qj1RTXJN2XGk1DGEBV7NbcLNm6YvR2MEGEYBikC13c-xAYrCHubJZ_hBamLT0i-pXuEEye_Cc6kdsl72fH9q4URi6jcMihCz8JlLr5LkBZPb0mh-FRf77L-vvFQ-pFv_ZF41pzArHqUxwQHkxfKI5erjYc0bnBX__HaqRm4mtHVZXUOSAekZr8mLvvcoM4MkgCzdebnNJOwvR5W2XH2Xj5nAfszpiQz_k2gg4xfnWSZ2oRoD6rXpPpf_mRr9jUMjhWSecCm0X9O0Bu2pvrIH5P-hLIo33CfVabZ2QiUN7U9x2_kf2gGqlKPeMUemnjIC8pui1IoixAUJF2sAI69kNrQHLacvrw?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O91TVWUeaa9zoWDS-W4Dl_RpB8wuxiYH-3cwFKDuoB6JqLk9mv-az4z9gxDCnVmjG5JM1Dsarjf-EpdBftswKvcaiSMNXcF1AYPKaSdX98UuG6cV7KgJYDn8i_ayTRTp_wNK-9GJ8aEBd-SMGBAHiY1fHK8u1OIDDCx34ufv-54Q8tiymJsURcv&b64e=1&sign=7d966f81eb8596b8e19e381d92737446&keyno=1',
            },
            photo: {
                width: 550,
                height: 550,
                url: 'https://avatars.mds.yandex.net/get-marketpic/207337/market_gaSNX0SyIok88nKCK6tbhw/orig',
            },
            delivery: {
                price: {
                    value: '0',
                },
                free: true,
                deliveryIncluded: false,
                carried: true,
                pickup: false,
                downloadable: false,
                localStore: false,
                localDelivery: false,
                shopRegion: {
                    id: 65,
                    name: 'Новосибирск',
                    type: 'CITY',
                    childCount: 10,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Новосибирск',
                    nameGenitive: 'Новосибирска',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву из Новосибирска',
                inStock: false,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 5,
                            daysTo: 12,
                            orderBefore: 9,
                        },
                        brief: '5-12 дней при заказе до 9:00',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            category: {
                id: 7812201,
                name: 'Сумки',
                fullName: 'Сумки',
                type: 'GURU',
                link:
                    'https://market.yandex.ru/catalog--sumki/7812201/list?hid=7812201&onstock=1&pp=492&clid=2210590&distr_type=4',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 9283290,
                name: 'A.Valentino',
                isFake: false,
            },
            warranty: true,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/ohzfmRzSMZfo1eAtuh2LrQ?model_id=1812754746&hid=7812201&pp=492&clid=2210590&distr_type=4&cpc=1Zw8kDkxe6Gaqo1_CC1mDiky8xm_4BaPKZ567pjydbNRDUO3Gqq-rokDS1Rhu3MkqsAMYcqkklrsiBs-EVhNwhZa40qKYkV5iIvzfL1jHBT95d9yU0LYF-2PwWxKux29yjlI89DtkljvTH88QcGGFYSCF58fzhLC&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1608,
                    SHOP_CTR: 0.01079136692,
                    NUMBER_OFFERS: 4,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_Trivial',
                        value: '1',
                    },
                ],
            },
            photos: [
                {
                    width: 550,
                    height: 550,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/207337/market_gaSNX0SyIok88nKCK6tbhw/orig',
                },
                {
                    width: 550,
                    height: 550,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1077406/market_1tzmSbmO17hYgNqPpvVlfw/orig',
                },
                {
                    width: 550,
                    height: 550,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1530810/market_dNm1Yfx-ZXiuzW4gp6BzQw/orig',
                },
                {
                    width: 550,
                    height: 550,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1530810/market_6M3gcfsfx-H_TbNYTmJc7Q/orig',
                },
            ],
        },
        {
            id: 'yDpJekrrgZFZbAgtiL6bAOetwqWrEgP99LYZ9zBiY4YpfFTmHRlzJg',
            wareMd5: '__qNByUR6vkBnMgUM1Bmfg',
            skuType: 'market',
            name: 'Сумка женская всесезонная Daniele Patrici',
            description: 'Торговая марка — Daniele Patrici',
            price: {
                value: '999',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZiCzW2fAsmmQ_GI1VpluQfg9G-cnW1l_wMabThZlGyV0WBXxMbMPerPsfinHe8YLQ1B-QSKMAzyC1V5GDmiI8XChzOuphPFW8JW3hA-SG1eq1SvfyFTgipH74kp4y-UDudO8gk3OumJkKdtufBsDGKfG-SwkyokzBZsJm-jlpFv-hvdZyrYBftym6qYqUKJgg1SbI7CKuzHNgpw_ge9pk3J78xOZbYiJ3yjKSYxIuk05buam3DclLdNig_XXXmIGdnABqKXflg2hElVDTLZSUbWRpdnCkYJqbYWUkM2W64UNdRay6QpX8MKYPZMkHH5urBQFHaesDy3UmaUpSURs1ST-DgrtpHzXe1rN7g-B6BXSjGmueu8cVMmdXhJ9nDiKyxr_TuF0JBdZRV8OypZLFK3yD7TJyIpHPOaFE-ZpxeS3uoirGJyTxgiHVltWbYtliOGLDTrS5Ew0QvIy-R8lXhsOFH2UviOYjXiXK44tGw2_gemjDFsAeEC8IWUvpJmqL2jEly1C-wCUOHPFDGntfeTfXSI6AKwRRogZm6OHBilBp9Mlo-TDH9KSQ1GebY_wyUNQKvV4H8EJeriO7sP7RQEpBHhQZTP7YwD8BBTlVLQZvr0fSVZXaDEBacM8I3RXATifuVZJo8hwviHrhCRtPY62SenaCeLeihvVtwpH_7Azpq_36Q-CW8OkjFFgAIu9byPs0ZFUtxaWNa8YWqUqCy3J-k47tBnsTLKtwTq4FyB3zRnGw_V9IDghRNF0taWVriiiQhzrzT2NT8XqM_GoG3fJ5ST1V9aAydKr6G2_QBcu24KT-2pOr-HeXVPCy5_ARnkX3s2yOPpUjE7M50Js3EtqyXaBbh88KxeDurUOeDKJrs76gMZwEZwxzv5gPzbO5Dif0DxZmFw_b8S255CT1FuyPa3RiK96f-CaBH2LAm_8noe706NAB1zpADnYDTZU2IMAs4LyXhm4?data=QVyKqSPyGQwNvdoowNEPjW_7ELrJbv5FvAxqUvXb3o0nQm4g717uNS9miWQlrPKEd3cuMuWB8t74MU0Fab9iZOdvv3jrD3AtaeeQeOO-0I4WNoiEZwSHV_aP3Ry19Ya6lyI5nWgyjKBZYsblgqCqgzlYSUFhqideBi7m_eZTW4_HxXd-7wjfkHL4XcHZHZySRJL5ghq3anWVXeh9b9ALYhzi-dRj9QlhYbocGN4YgZSINANi_CfQaV_x5K4L8-WNpQfRhEaeLaoYeE6b1a5p7N8Bk9t8e8lfjz553FXjJ_ZGNahr5bBC29E_sj-6Qrkb13uo8QMzh5nrn9mWXZrs37SUHlmYtrmgobwqGfci5r8HCxqrCz-R-qncYn06SVnV6byJcr8-78HwpHU0aaPZDcPnsa8Wt8A6rNqxqyVN7fCZ7oJpuW8UC0eIT06hRZc6xekIyMvDcZDv6bwm7r6e7McqNPh0_u32pxGkRf65u5sqrkeaqBeTtw,,&b64e=1&sign=61a851483324619003826fc86f7abe51&keyno=1',
            urls: {
                492: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZiCzW2fAsmmQ_GI1VpluQfg9G-cnW1l_wMabThZlGyV0WBXxMbMPerPsfinHe8YLQ1B-QSKMAzyC1V5GDmiI8XChzOuphPFW8JW3hA-SG1eq1SvfyFTgipH74kp4y-UDudO8gk3OumJkKdtufBsDGKfG-SwkyokzBZsJm-jlpFv-hvdZyrYBftym6qYqUKJgg1SbI7CKuzHNgpw_ge9pk3J78xOZbYiJ3yjKSYxIuk05buam3DclLdNig_XXXmIGdnABqKXflg2hElVDTLZSUbWRpdnCkYJqbYWUkM2W64UNdRay6QpX8MKYPZMkHH5urBQFHaesDy3UmaUpSURs1ST-DgrtpHzXe1rN7g-B6BXSjGmueu8cVMmdXhJ9nDiKyxr_TuF0JBdZRV8OypZLFK3yD7TJyIpHPOaFE-ZpxeS3uoirGJyTxgiHVltWbYtliOGLDTrS5Ew0QvIy-R8lXhsOFH2UviOYjXiXK44tGw2_gemjDFsAeEC8IWUvpJmqL2jEly1C-wCUOHPFDGntfeTfXSI6AKwRRogZm6OHBilBp9Mlo-TDH9KSQ1GebY_wyUNQKvV4H8EJeriO7sP7RQEpBHhQZTP7YwD8BBTlVLQZvr0fSVZXaDEBacM8I3RXATifuVZJo8hwviHrhCRtPY62SenaCeLeihvVtwpH_7Azpq_36Q-CW8OkjFFgAIu9byPs0ZFUtxaWNa8YWqUqCy3J-k47tBnsTLKtwTq4FyB3zRnGw_V9IDghRNF0taWVriiiQhzrzT2NT8XqM_GoG3fJ5ST1V9aAydKr6G2_QBcu24KT-2pOr-HeXVPCy5_ARnkX3s2yOPpUjE7M50Js3EtqyXaBbh88KxeDurUOeDKJrs76gMZwEZwxzv5gPzbO5Dif0DxZmFw_b8S255CT1FuyPa3RiK96f-CaBH2LAm_8noe706NAB1zpADnYDTZU2IMAs4LyXhm4?data=QVyKqSPyGQwNvdoowNEPjW_7ELrJbv5FvAxqUvXb3o0nQm4g717uNS9miWQlrPKEd3cuMuWB8t74MU0Fab9iZOdvv3jrD3AtaeeQeOO-0I4WNoiEZwSHV_aP3Ry19Ya6lyI5nWgyjKBZYsblgqCqgzlYSUFhqideBi7m_eZTW4_HxXd-7wjfkHL4XcHZHZySRJL5ghq3anWVXeh9b9ALYhzi-dRj9QlhYbocGN4YgZSINANi_CfQaV_x5K4L8-WNpQfRhEaeLaoYeE6b1a5p7N8Bk9t8e8lfjz553FXjJ_ZGNahr5bBC29E_sj-6Qrkb13uo8QMzh5nrn9mWXZrs37SUHlmYtrmgobwqGfci5r8HCxqrCz-R-qncYn06SVnV6byJcr8-78HwpHU0aaPZDcPnsa8Wt8A6rNqxqyVN7fCZ7oJpuW8UC0eIT06hRZc6xekIyMvDcZDv6bwm7r6e7McqNPh0_u32pxGkRf65u5sqrkeaqBeTtw,,&b64e=1&sign=61a851483324619003826fc86f7abe51&keyno=1',
            },
            directUrl:
                'https://kari.com/ru/zhenshchinam/sumki-i-ryukzaki/sumki/sumka-zhenskaya-vsesezonnaya/06405940/?utm_campaign=Yandex_Market_Moskva&utm_content=1989530&utm_medium=price&utm_source=Yandex_market&utm_term=1989530',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRu3ggMogyaCTfleH3CbjT6oKIScE_jQR9-7Skoi6mC08-YUvup1VwXWTIdjymeS8gfpeTXP_K2ftn1ISZrE2w3WquY1D9rfuMCKSakRDHS1-B1Mgk3rHXF2fusVORGjz9gK444gHYjBbXMZyzyOw5EN7CM6J3oqRy7iPuI-OoM0mIy_XCn52pkg4a8SEnsrriCKV89LsmS2Z-yQfUqA4ZT2l4KveLArdB0NuZIY2Fk0J4ArMMeZhr1miTlFh074-e9TivpodnyIFXdeB_qA8xS5UsuGC64O90zQDeUxInQZI0ulCcPmdb2P595zr4fiM2kisoyLClx2jNyC_Bzqu9DcsD3SvGJK8cz1k_LRqDw2D5hsy41Nc_2k4Bpr8OdBnPhuIkSJmr5J3DW1rxpeM-GIZgGyZlG-QRZYOCaar1ba9N8LRTtdzYOOQe6alaDKUZjipMdKkEFq9p2VbPhLwhOqyy-NFN2lZAub6rqjGSt8lhZgtT982FrNEMfiB0YLAANcYXQxE8-cd_PguFx4h_2P0tM9neyqa8V7ZBxm0ocL5DIdwb-7rOT3OLx8QZMRZhxeOOED2ywO7nAcKaGtUjp1DrKuOjeLErRUcKpaoRZo4YsgKjrL-IQjoiv9vqIPWP0Dfckk3_vVaeFGLvKLX7676ZsBjAKMIE_ww7pP1ggNzgTxyHauvouH-QSYHXftsRd0T9FR47q3XDZaQ6EJjv1He9Nf6tSkaJlxzq66fp9FF28r0scKWGAyeCEOxW9HQSu3ncpRQ_J0Uqeu1EcVK8vh0-9XYzIHaYcNdxmvmG3mFGQ5ibJ4Wn4ObSnMWVNwwaF25XcHptLOA97CUNCtuzlYl-a4AOuHOmBoCP-EGuf6ADYFUxcwwswWoee0xkuKSc3UQ8Av4Uq9Ch11JqfXr6VLLnHH-sUv5H6kV3RKwgabys9M-yPQCq_tcT0_H1wsf4n8cdn5e__Pkg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YL0Kud43qAL4UYw98j4Zh80kkmJlpOUpINe4Iy0Uo70Hc7S-s_XhgjN2vmrZGoz0LgLaKeDoUTipnJYb2UtkwuwZyVKOklX8po9K9F5IcataPaFfMIvcTc,&b64e=1&sign=2db93eb42a6fce6a1658c83a75273720&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRu3ggMogyaCTfleH3CbjT6oKIScE_jQR9-7Skoi6mC08-YUvup1VwXWTIdjymeS8gfpeTXP_K2ftn1ISZrE2w3WquY1D9rfuMCKSakRDHS1-B1Mgk3rHXF2fusVORGjz9gK444gHYjBbXMZyzyOw5EN7CM6J3oqRy7iPuI-OoM0mIy_XCn52pkg4a8SEnsrriCKV89LsmS2Z-yQfUqA4ZT2G7M2D4KOoWAfXIjxvacXOCd5ZNGHujgAbG4n3yKCnO1Jed4RbM3olsvLhHSZWuRhy_IUdgmOfXDQnaHdYkgCw5OSCXUeuM12JpOvwfX2W06smNVwJOL9j34XtdbUPe7NLtBPRyrM_p5ZX172foOWrSGklzQWssuM_7GVygTbJmo3fjP0C7999aa-En4msGY9SatjmQ8aZCHTvF0TT7gCF_tI2sYPADZm8H9kE2xjTzq9nxSDWO3dnYJdZy1Y-a_bAn72HLTSt1-y3G5eovuhyJmvFIVxZel4_X2uKu9z6jM_TtpYLE_T_Y2qJc-VhO8hSAHHvCHEUqWx2vsON4vJRGtfEUqkl5JFfh64Zi7owM411oEJLwk557BW6I_hroTpmiygPbocCZiQHmmkjmS-hS8hmHzvHCY2ruugqhAbv3TuTi8uv1orgIHZ-5No2bbwossHI--nBasUuYrILKEQ7Ngsz0lN27dfVzz-pat8FkCzDdyGw71AvQ-YxkMS3jbUSL8LmeEsL_ztdvXvrB0FILL358B8KKnvag8Gd0ufNofEFXEoTwO6YpZ7udUyBq_EGiFhSXB2wK2xiZnRa3dYtUaSuwk-GMfK6AohZLeJM3TJPh9dUnsPIH-Dc81dXrhWICUrq_L3Kk7qh4MY4k846jaHMFDye-1Rx-KltP1ukO9toiYOOslpB6EoAL09UUELgyXZ7GZz7OB33NJweZaLFBvudPOdXjU93K6LbyMT3l58FHsV6RJncw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YL0Kud43qAL4UYw98j4Zh80kkmJlpOUpGUR2aoo1oLym7NQ9GoxJ0t4Paae1wLpBOjVIRVC2QN4JZ0QTU2iiBtNfRW9MowUR6nFFCv16Vs7TEjF_WHKO_Y,&b64e=1&sign=0bad48f099cf51dce5465e8c0f244a8a&keyno=1',
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
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.2,
                    count: 3706,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 83,
                            percent: 11,
                        },
                        {
                            value: 2,
                            count: 29,
                            percent: 4,
                        },
                        {
                            value: 3,
                            count: 40,
                            percent: 5,
                        },
                        {
                            value: 4,
                            count: 79,
                            percent: 10,
                        },
                        {
                            value: 5,
                            count: 527,
                            percent: 70,
                        },
                    ],
                },
                id: 366769,
                name: 'Kari',
                domain: 'kari.com',
                registered: '2016-07-08',
                type: 'DEFAULT',
                returnDeliveryAddress: 'Москва, ул. Автозаводская, дом 18, Ривьера, 115280',
                opinionUrl: 'https://market.yandex.ru/shop--kari/366769/reviews?pp=492&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 466122825,
            },
            phone: {
                number: '8 800 200-10-63',
                sanitized: '88002001063',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZiCzW2fAsmmQ_GI1VpluQfg9G-cnW1l_wMabThZlGyV0WBXxMbMPerPsfinHe8YLQ1B-QSKMAzyC1V5GDmiI8XChzOuphPFW8JW3hA-SG1eq1SvfyFTgipH74kp4y-UDudO8gk3OumJkKdtufBsDGKfG-SwkyokzBZsJm-jlpFv-hvdZyrYBftym6qYqUKJgg1SbI7CKuzHNrCmHefdJY1iNRlWrM_Hpe9Y8ZMyLadrTpJL-u6nmfj6ENU_1iTiz1srDtlSotDyl05lBvoCbgNlf4i5rVeOxkvnRchbmmWew8rYnyBmKxjups11VtVq6jpkVvBDNHy00IGyCJKWllOTeBAnZpUPU0blX_dGzhOHiTdmUXLa6eFmiNAkEHFppJdJvIoK1Ss593mel8255OjDKKeUT1RCmn4KgL4rll-EC8X98ikgKVvePFdZx02fU_AEApRPAfm2RTaUqDPz0wtnKpxvWiEwokYIyF-K-ttLaw5dI4Av22xEUAH_Vk9sTtJU2855Cz3VXrKMGWi6Gosz2NOTLvp4TLIOQu6PU96IcdRW0JCt8KTLxZelD3itDi19rJLlrr6fiPqnVuuEjz6ZofNcamqM55muCdrSoosdnmQshw7vASg4wGN4IXti-3V-6iSzzj-y8fJrxDeVKWcPUnQAc7e6UO6e18SAdAUbshTGgzGkt43lcwpMIp1A_isji--xasBuFjBD0p2l7vWM1u4eMfuwQxHYuKB7LI0_1G0Mx4wdqBS6TnVobwgal9CsIPeBNKYQ-8WswHEgb6KH8xumcJ_2MWB6bSW00FW7TVlRk1ErEFfNBOk3UPVLMLG_ccuFi_xyvWZ4CX5SmF6m3Y9o2THrXqWB1rDbpv7aENSpj4m97tnnjFhj3u9XHoBAAwVGhZzQljryf10gMqAhQmvzqEo8Zh10Xi2pNdnXwloZqC6TBn0ovq8Vn_M70MhgHo2U5At1P?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9KBD-aHrZxUsaIuaqAmI1JB1JH5qchDkgwLmtjlWL_p2hmqGhPoCOgQPlVpisuQHsUKN5mbdtzUUNEDSBYFGH5iSImwPn0YDOqxWpgwIs4_uxNkF8t-ELP0UveanlGVFDgz9sam3L28AtS2tgHg1iTSw9OWSMmLEspIQSun5Jn0w,,&b64e=1&sign=999e7a11f96e552e1b5b5b7217de27fa&keyno=1',
            },
            photo: {
                width: 762,
                height: 1100,
                url: 'https://avatars.mds.yandex.net/get-marketpic/169986/market_oJX1ur3OallNjkYhQ3nzLg/orig',
            },
            delivery: {
                price: {
                    value: '250',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 250 руб., возможен самовывоз',
                pickupOptions: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 0,
                            daysTo: 1,
                            orderBefore: 24,
                        },
                        brief: 'до&nbsp;завтра • 28 пунктов магазина',
                        outletCount: 28,
                    },
                ],
                inStock: false,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '250',
                            },
                            daysFrom: 2,
                            daysTo: 6,
                        },
                        brief: '2-6 дней',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            category: {
                id: 7812201,
                name: 'Сумки',
                fullName: 'Сумки',
                type: 'GURU',
                link:
                    'https://market.yandex.ru/catalog--sumki/7812201/list?hid=7812201&onstock=1&pp=492&clid=2210590&distr_type=4',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 15134534,
                name: 'Daniele Patrici',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/__qNByUR6vkBnMgUM1Bmfg?model_id=466122825&hid=7812201&pp=492&clid=2210590&distr_type=4&cpc=mqTIP1zM0o4wIzs93VTBT4A5vC7s7X4T8GYqJ4ojAu5AEpOjsrMb3u0P6jRPusxrU8D5IIec5Olokh3UBjQtTpCGD2RyJP05wC2CUBNOt1XzCKzRSbecVwos8KJtdT5umiQfAlWIzBPmtrQLUcV2vxV3de6KKZLf&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1608,
                    SHOP_CTR: 0.003537813202,
                    NUMBER_OFFERS: 43,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_Trivial',
                        value: '1',
                    },
                ],
            },
            photos: [
                {
                    width: 762,
                    height: 1100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/169986/market_oJX1ur3OallNjkYhQ3nzLg/orig',
                },
                {
                    width: 762,
                    height: 1100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1055125/market_LRfO0jsSsswwM21Ceq49gg/orig',
                },
                {
                    width: 762,
                    height: 1100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/176166/market_o6kMknIQigdEdbqBWlegJQ/orig',
                },
                {
                    width: 762,
                    height: 1100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/239743/market_BU3z9omK3yExO-YiwdZagA/orig',
                },
                {
                    width: 762,
                    height: 1100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1059059/market_crP_m31eujMj4GYhnA7YMQ/orig',
                },
                {
                    width: 762,
                    height: 1100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/908728/market_WgTOuu31fuPnzhqWZhhsnw/orig',
                },
                {
                    width: 762,
                    height: 1100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/908728/market_XRvXWzd3kglqn1QTPu2BxA/orig',
                },
                {
                    width: 762,
                    height: 1100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1059059/market_agLhtsG0X34LnQz4LVUgeg/orig',
                },
            ],
        },
        {
            id: 'yDpJekrrgZH1JvzZ5n47zGWzDCs2Z5yGEd7FzgSyKOXFNJoETBgTBw',
            wareMd5: 'qD5OkpIs6X4nPMch_E9NJQ',
            skuType: 'market',
            name: 'Черная Сумка Майкл Корс Mercer 30F6GM9T2L Black',
            description: '',
            price: {
                value: '15900',
                discount: '12',
                base: '18000',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZpuK9_UkayY8ormDWpetUP8redL-n8Tfdemj9-ueqloqC12LWuxoCGPuxjflk2Wg56ZCs15cy9JiGy_hc5wg5GKd7ng2Z3Am0KjUf6_TVjEKkc9aTbW4TVtA7nDlmkHx_gmEoliK4fjsQtGNLdw7IXF-9boE_svNOsKye5wgFp6bo5kuBKCFVjz3qdI-S3DMKdWPtJdHcnwHQyt6PKL-IzQMF6xUiw9PWSJO7TlS38aoyperKchvCOmKVM799r_w_rlfBYc922psRGXCVyPBOFl5aRgxmux8vMPeSwT5KpadD-GG02mzdt4Va5ykXV1xW_fp9DMwLrQpGhzj8IkqgPOgnlABy9h29F--hITOYTiKsrdZKoFx9U5y6QLyEkWsR8D1xeBMJ1rVXdQv2guXYNs-ppGt-fxf_MmIQkC-8-YzirwugIgbZyV3trc5gv1nVoG6SUwZNNXrB3B037NPTXNCNUvEoQMz5BFeboT1cc9BYa3yUOdlV-i7eRgUOtM7r3wi9EYg32K29-rLlYAwmEev-FAfyZyy5yHwMTFXdZ6LQoGne6IpEySyNRyKO0s74ZbZTZzJDIY9Af5ildJFRq66bF2DyQA-bSIkpWJTAsY939E3EGqBXN8d2-H3c9g0hG5NpBxe61nmq3Bz7RspY5W2wuLyf-sWsYXFo-stITANJax5Mp9jJiBTdF8w_F2OB1-88jI8tZA4Vhka7jOHSd9AUJ8LavSEaSTVBUJdmTGYoUl2Uj9lHyAsnJH5Vzzq-l1ZBT8WJlGLyD4YS4fBj3NJt7ef2kV9fFeGlYrjUeA8JSTeV3Mv3LKwp6nzqrhn9_GU-Mk8IOclnlnM5VuvvLh9KPnCy6YkzL5RDv53MkF3TZTo5FF6wAzaXOrHi0ehBF1tGBfGwPYbKZOqc45v6SWLb54UEcmrjlQcDmiTKVpy_hMST9rlojkt8nEzlacstMOyWqdxaenZzdzQ64Qr5RgW5TWRK-EbWo7Qu9KyPuhS?data=QVyKqSPyGQwNvdoowNEPjcQ0pklRPEquNGW4QaqY5ls5YyjdzouxHIn-JZuWW1VGtVmFKT5PKVjlrUfT1Ck10oG4lO9R9hxnXTBukF37GZGJwMP-WKu-FbL3Mn1VKcWTUc5bFOgoMf2IiASSPZuJyzaue7dsDpVfZTHAxTUuWCfGlWgiT02K2hoeljewTJep1WFecSiFtFynEH8ePKtqXmrxR8ZDVIiYdgbJ1Zoj3hyYJa7VlBHoQs85xzi3h5eViMgX7LK6IOqwN_CDb7PMtYHMOjJl4-z80TUGgNh9ZiFlGkUSlAmoIh0N3OATmB_02seYHmKR2O7EZyjyTPhx6GljQNV4xzrYiPW2zFY-9xwIeVC-cOgnTeP6zlQ6-bHOzbibHBS0CmnmLEioDcvqaS4tx4iPX6kslPTZQH5kmnRB41uHEfhlNFGKxi4PNCOkz8v6VAPQfDYFVaRSv8IPzl8rx10xBBW_DxacyjMJRmtmqZU_ysAnLzk4nxPtYerLUP8-dyqqMKvQ1uBbPsFxOn6LQ2rKT3u9xAzn2wqLlRg,&b64e=1&sign=a07f3ac39aab165281dec460fc1b9d30&keyno=1',
            urls: {
                492: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZpuK9_UkayY8ormDWpetUP8redL-n8Tfdemj9-ueqloqC12LWuxoCGPuxjflk2Wg56ZCs15cy9JiGy_hc5wg5GKd7ng2Z3Am0KjUf6_TVjEKkc9aTbW4TVtA7nDlmkHx_gmEoliK4fjsQtGNLdw7IXF-9boE_svNOsKye5wgFp6bo5kuBKCFVjz3qdI-S3DMKdWPtJdHcnwHQyt6PKL-IzQMF6xUiw9PWSJO7TlS38aoyperKchvCOmKVM799r_w_rlfBYc922psRGXCVyPBOFl5aRgxmux8vMPeSwT5KpadD-GG02mzdt4Va5ykXV1xW_fp9DMwLrQpGhzj8IkqgPOgnlABy9h29F--hITOYTiKsrdZKoFx9U5y6QLyEkWsR8D1xeBMJ1rVXdQv2guXYNs-ppGt-fxf_MmIQkC-8-YzirwugIgbZyV3trc5gv1nVoG6SUwZNNXrB3B037NPTXNCNUvEoQMz5BFeboT1cc9BYa3yUOdlV-i7eRgUOtM7r3wi9EYg32K29-rLlYAwmEev-FAfyZyy5yHwMTFXdZ6LQoGne6IpEySyNRyKO0s74ZbZTZzJDIY9Af5ildJFRq66bF2DyQA-bSIkpWJTAsY939E3EGqBXN8d2-H3c9g0hG5NpBxe61nmq3Bz7RspY5W2wuLyf-sWsYXFo-stITANJax5Mp9jJiBTdF8w_F2OB1-88jI8tZA4Vhka7jOHSd9AUJ8LavSEaSTVBUJdmTGYoUl2Uj9lHyAsnJH5Vzzq-l1ZBT8WJlGLyD4YS4fBj3NJt7ef2kV9fFeGlYrjUeA8JSTeV3Mv3LKwp6nzqrhn9_GU-Mk8IOclnlnM5VuvvLh9KPnCy6YkzL5RDv53MkF3TZTo5FF6wAzaXOrHi0ehBF1tGBfGwPYbKZOqc45v6SWLb54UEcmrjlQcDmiTKVpy_hMST9rlojkt8nEzlacstMOyWqdxaenZzdzQ64Qr5RgW5TWRK-EbWo7Qu9KyPuhS?data=QVyKqSPyGQwNvdoowNEPjcQ0pklRPEquNGW4QaqY5ls5YyjdzouxHIn-JZuWW1VGtVmFKT5PKVjlrUfT1Ck10oG4lO9R9hxnXTBukF37GZGJwMP-WKu-FbL3Mn1VKcWTUc5bFOgoMf2IiASSPZuJyzaue7dsDpVfZTHAxTUuWCfGlWgiT02K2hoeljewTJep1WFecSiFtFynEH8ePKtqXmrxR8ZDVIiYdgbJ1Zoj3hyYJa7VlBHoQs85xzi3h5eViMgX7LK6IOqwN_CDb7PMtYHMOjJl4-z80TUGgNh9ZiFlGkUSlAmoIh0N3OATmB_02seYHmKR2O7EZyjyTPhx6GljQNV4xzrYiPW2zFY-9xwIeVC-cOgnTeP6zlQ6-bHOzbibHBS0CmnmLEioDcvqaS4tx4iPX6kslPTZQH5kmnRB41uHEfhlNFGKxi4PNCOkz8v6VAPQfDYFVaRSv8IPzl8rx10xBBW_DxacyjMJRmtmqZU_ysAnLzk4nxPtYerLUP8-dyqqMKvQ1uBbPsFxOn6LQ2rKT3u9xAzn2wqLlRg,&b64e=1&sign=a07f3ac39aab165281dec460fc1b9d30&keyno=1',
            },
            directUrl:
                'https://neva-time-mk.ru/chernaya-sumka-majkl-kors-mercer-30f6gm9t2l-black-original/?rs=yamarket11_21575179_13323',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuE8uVOTZFhEciGgB6H3GZ8GOpoc6_KIlspPJPROLAL8wApHD5q1z8x9k8mW7z44v8WsuA1zoh9EM9TZQRhgKZhKGcs4lgvm3pJu-3axJYseMzny4puT5TOwB6iZkKj-lKdSFLkUHCAL8LHtN7wI0R4NV-GsysANGuNR2akkLG01sMpJEd5JMhyXU-PquLE2acGLK3qqe0r9fznWGOTBNjSFI24nB9lk37Ggk0KxdtTV8Uer0wTJB8A_EsUQ12AlbaV2Zp1B9dyUAUY7KW_T_X6Gvwh5Lx5x1KsuNY5qSDOcDoxE2DtJR4WBHRQmtWKXN0blb0f8WmaeTDYCZNeqvjHpT5oSobNblBq5c6s3CRNyWJXi4pKV6Zop1wo01vMMHrvwg-fJycPvElI0VG7ONtVZcrVqw6rbJIFX0I6XrKCaMzz06Q0MHKissWqzE8mHHjvjwIFTxDcSb28F-KVWyL-sEsh6USaOA7Tpi9ks1yZKiITLq-LLGHR5IiSv_zBA5fV14nQxEavhv-H6eiaglyJWS81Dmrab3bEx4YSn8bUTiKqh8YwhgONLxhz1mWvNAY-H6tx9z-1IGwN2n6_-A1AKrsbEYSGQbsEpApgX7CRBBTLSWA5UbZD3VT4VerbAQGhCbWKEQL8hZdsD2Rrn68jJPenCUtBzZxPl9crhcpp_eVCewEQ5ADo2v-jlpOhv88JFDy1rdSoYxDE0P3LBmyEDfoEl3xWEhuKEN0PVzGCzjPT8gafZg7_vxr4RLWyMjZWSo4JgcPomLwfk6Fca8W0tHUmRGPa_bav4EtSNWWFPF_Pb9ha6a-Eh_EDgs_7WLflEw8wziCTSF9QlLkHYtsVmZ6Y_jn1NxZoHa_Dn0l-LN9ddshE2aitWSb1blocId5O2kw83xWkMoWGtQGxbckf8vS3oSo0CkiBPdYmir7-pOfAOIwlny51aGH2IhvEK3Ee7_KLqFkKRqiFHDiSBoXQ_UZXXx4YykbHixSu6CBaww,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2S5jV2SUbsysfKrqnNsCyOoSaSl91GZvXLTLXMxpvujlLW5rCvjzgKVQElMqS0lhv8NEQCiq7WlyLiUkskTv_2rCUDvXwwjGxUq_iMIrmYW5SfaJkRWW3j0,&b64e=1&sign=41e973f2c2619b961f8a731310a407a1&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuE8uVOTZFhEciGgB6H3GZ8GOpoc6_KIlspPJPROLAL8wApHD5q1z8x9k8mW7z44v8WsuA1zoh9EM9TZQRhgKZhKGcs4lgvm3pJu-3axJYseMzny4puT5TOwB6iZkKj-lKdSFLkUHCAL8LHtN7wI0R4NV-GsysANGuNR2akkLG01sMpJEd5JMhyXU-PquLE2acGLK3qqe0r9fznWGOTBNjSsTK_9pX96ELbdGovlOcgbU2t4p8xrl3-Ulwqc6prC6I_nfabf5tjOpf11ayJ2Ng_diN2Bw2YETrE294GS8LCGXiP5-sjh_qyWNxAkFQ4hkiHZApehR_2cxUchfyc81UpnuEPX3Y5lZsyTlqhR2plu89ZqtScYaAZu8Bs2dNJ7xHWaU6T4DXSku8b-RNUy3WIKGPAt_ov7J-1U4U7lU9Q1dlyGZlRDRY1iBAfMW0a_at16Y64iK397TM9afhy7pXpq2lSDkd9r4QPrlOWti5s6EFeF9ZiVhuhormaU3T-O8_a6EHoPnS8-l6RazAAi6sw29tkN-yCh13EUY0MJHE5EJME2h0O6sCPlun95ieeukUg8aGW2Nti_0rYyVTtcCQdKUX4lSiv9XjUmdKCsPGqODvAMRf1-qjKt_70JxzlgnxW77ltzCQb4RH450XIExPVEUMOTPbSD6k1QRSFdkFD-cnJDDIfh6L8VVdoJfXpj_0vqcObylBku7DORFNIS_KrhIBWlJsUTpsYjfVUjVJ0h4IeoDNY_y4of9v20EKwqIObv2A_tboysnPdNSKJBSYjJgSPbmpZSGBWFsAR21_8r6R-nRZu1txJwhXNzqd9tCxpfKFfO5R-kmEWXSd7fxwgnYcDc4UKcnezIYLD3Le7U0Hs5GdYRJdVvalhxJ02zC5XPmta-XkuO-asCRaB8ne-g61W6wh_bBA2R4NIQOvnzGO8APpPP6OFhK6JGfrYBq_ucUMX9MhQVVRZtJnrrnM99HD-QbBWPfIEZW7KAJ4paw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2S5jV2SUbsysfKrqnNsCyOoSaSl91GZvXK8O82-kGvj1d-26dk4N51lxbfLI4D76oeF0hJQstoKV0p5s63NIrVtH7C9eKBG3UPCfDJaUYw9xZWDQVwv65Nk,&b64e=1&sign=bb984df5309b1da5157efff060b3804e&keyno=1',
            shop: {
                region: {
                    id: 2,
                    name: 'Санкт-Петербург',
                    type: 'CITY',
                    childCount: 18,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.6,
                    count: 448,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 2,
                            percent: 1,
                        },
                        {
                            value: 2,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 3,
                            count: 2,
                            percent: 1,
                        },
                        {
                            value: 4,
                            count: 13,
                            percent: 3,
                        },
                        {
                            value: 5,
                            count: 366,
                            percent: 96,
                        },
                    ],
                },
                id: 341257,
                name: '«НЕВА-ТАЙМ»',
                domain: 'neva-time-mk.ru',
                registered: '2016-02-24',
                type: 'DEFAULT',
                opinionUrl: 'https://market.yandex.ru/shop--neva-taim/341257/reviews?pp=492&clid=2210590&distr_type=4',
                outlets: [],
            },
            onStock: true,
            phone: {
                number: '8 800 775-80-31',
                sanitized: '88007758031',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZpuK9_UkayY8ormDWpetUP8redL-n8Tfdemj9-ueqloqC12LWuxoCGPuxjflk2Wg56ZCs15cy9JiGy_hc5wg5GKd7ng2Z3Am0KjUf6_TVjEKkc9aTbW4TVtA7nDlmkHx_gmEoliK4fjsQtGNLdw7IXF-9boE_svNOsKye5wgFp6bo5kuBKCFVjz3qdI-S3DMKdWPtJdHcnwHQyt6PKL-IzTVH4oqlr1wsLbdmZnoq9OBFwnlc8Vuup2gB8nyM9r6XlOlY553MC71ifghcy3gDuizHbcVf3BMXYEbvn0i0uq68IqHgRwN5X0WVMIVEQRBuvQsmPMCQ0ZUhZhBcUYBQqKOkeIUJa2Z0i2yXneEClPKGddaaiQKO8jAnzD5A7v_VLbDvJnB1w4BjxMGmmcLlU7y8r_uMWQA_GKMnfpDxNn_yuHCwcNsM8DeCUEwCaM470Go7HOU77mVu1RAH-W7_3Ntbzju482BGw-LQlQacunQ_6QvFuOzApD0uB7rVt69ndzcbXomrktuDarCA4tbsHnigkj6qnpWlbjrTARzlr_rMUerOj-iJ60EB3_7XKiF5mwQi_-M4RVBDLTpqli2pHRfDky7HcvapIX6_q3Duzl5gu3_eEt9GkY2_Sfno6V0gZdWTXvKRhPFq50KDt8i5cLlyEqxk6UKU4X7mG-cUeshnnBtFBuu7s_1siZnu5mPd2CxkN3AFNxbTkXCma9mvIZS1CLV1HVcuOW1MuIhdDCsms4Tl8gTgNc6P8JfROHBo480aUbJyWectrzORILU1LfmjgrVzZPN_yU2w0f3NWmm3QcOpDAzSNB0mfm5kIuvyYnJzMUfoSZBOjq-YTWHMcxtp8iIIhMhLtNw7oBBpy7W0JR5S6djs7Qa3AejY86Ulk_-O8tGbtbFitm989Yt8Vwgn0NbqgzDeH_VGVDRCoqFzzes9F2xPyUbMXFf10QkmNwmIlDihx__QDiKs71sdHMCdrhuo0WpWazfE5bu9rrW?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8VXpUX2UI5t8wwOMasS0VEgyxmCsxM6J73KE5OAs-XJNwammGTTT_CzP6NkYL_vaRNKyewOjV7Flh3-KDDgBTg1kDUpTCRfspOYfxi0NMajMtX6U9uQcJeRSG1duXEtBxku9TMIZZpro2rZlozLb-I&b64e=1&sign=61bb81dccc9dea2760299a0300e3d401&keyno=1',
            },
            photo: {
                width: 800,
                height: 800,
                url: 'https://avatars.mds.yandex.net/get-marketpic/931398/market_nLA7lFNAty9R4c0OI0lg3A/orig',
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
                    id: 2,
                    name: 'Санкт-Петербург',
                    type: 'CITY',
                    childCount: 18,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Санкт-Петербург',
                    nameGenitive: 'Санкт-Петербурга',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'не производится (самовывоз)',
                pickupOptions: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 1,
                            daysTo: 2,
                            orderBefore: 24,
                        },
                        brief: '1-2 дня • 1 пункт магазина',
                        outletCount: 1,
                    },
                ],
                inStock: true,
                global: false,
                post: false,
                options: [],
                deliveryPartnerTypes: [],
            },
            category: {
                id: 7812201,
                name: 'Сумки',
                fullName: 'Сумки',
                type: 'GURU',
                link:
                    'https://market.yandex.ru/catalog--sumki/7812201/list?hid=7812201&onstock=1&pp=492&clid=2210590&distr_type=4',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/qD5OkpIs6X4nPMch_E9NJQ?hid=7812201&pp=492&clid=2210590&distr_type=4&cpc=NoDs-hOevCYqi2_FmP2b1WC2IahkmpjD73wVunVs-xxGS6x5Sw8xVepn5nFpt7l5dHYirjI-3zzXi7Yb0ltWJBhyrwYMQoWVRuMbxeD346z5rUK-NtRCnyfGuHOxMnTeYOnHu62TIyVsIFLbtdOyKmXXbxweHsPg&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1608,
                    SHOP_CTR: 0.002239548834,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_Trivial',
                        value: '1',
                    },
                ],
            },
            photos: [
                {
                    width: 800,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/931398/market_nLA7lFNAty9R4c0OI0lg3A/orig',
                },
            ],
        },
        {
            id: 'yDpJekrrgZGvh6O205x4ZNuYIqRV2rpHraf-uSzTXGiJz4fiPFECvw',
            wareMd5: 'zAz8Z43Dp2lYR9PeOpH4tA',
            skuType: 'market',
            name: 'GR01-00028305 Сумка на плечо eleganzza',
            description: '',
            price: {
                value: '21850',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZldAiEK-xeweGIsCsUosIHnln_eknAGEpum07DX3KtW-BTo_a41qgiu3fxOnHmVcuMHEBE59d4HutG-BOEQUW48x3UnN9hocKiPiuYivlG9TEe1rPDMjLk3TR9zTM-UPxiqaZxsTFF_YQKI9gfAJ_9MCOMiuf5LjYWyj1G01GG178AAMdbGtMC1Zw-0rHrSfXy4yYDn5R32E4UXW4Gb82lNgVt5dp8ZhFjXFUPfpzu5_5zF6g-f1F5-2QsZz8kna7SF44Q_LV2PuRlfAs9yZlnf_H6LFRG04Za7qnxtIgvA9hYJ6rOvtKIQqM_1Oy6SwIxScxEOlJ2MsZ3jh5FeWH8nIsQ58pIiQAWf_r2F4E6R7isgKcvzy74C99H7CKYVcB9Kp8xrV_GUvWeLELsfrezZKgb_ZDYMZ5Z2NDuZbC7_GvDKdJCEf0cs2vsnhsT5KA6Aar5GONovQXYEG5U31VrqtNFhX8vaXjYCMgVdlKaF0ETO2bTpLTlNJKtyOkcVYDDla-u20vYgm_taNMIpXdbnMAdwdte_4ug8jRJcOEcwL7nws9iK0rAmZHRZgg4ydy4Vd8xiNcgQoySe2xdb60IQLnl_x4zVvCeWROsgKEvXgaQH2dp48OKqoAamrPJCrgNHm2Jq09esUN9r1c48OSmKBja1YO2HRljUgAgv6Hyi4okQxySQhWthemDPoIWcqkB1D53WrzQ4LufIOJi9loI_LRpXIcxXdZtaXHOXnjZ4PvyU6L4SCyL3fp-XMIsZhgDPgmEsBMS10Ck_nfumKyU7H6I08C7bzucHjOUcs4pqD2cd27TklLshFuFiqtxdVEGxweXN1WYM-bMgY-8xQYYo4JrsqvXJfSHm3S0sznBhIzGfxjoolLkvqog7tNJz9MfG0g_I9zEl7OWlnmSBVRjK_L_usqHAQ-AYmFiquuzI_bk7r9X1WKF_dLAcg92lNoKnkX_XjUU2OWy5KzpU67y8,?data=QVyKqSPyGQwNvdoowNEPjTZ8syOZQLnueZ9Kud86H5E4EwMo1gcBh4nkjQJu49gyxAr-gGxPo1uzk1qOwyA07850zHlpIzrJXMlYdDS1tJTynH5QF94dW_i6-Z4LPFT9TrsbRXnQod0mRevtuWpdyS6s-aj0PIkTReWUfUjbxKYKhxI-YaWoHg17gUOFIbi1A_CZmWiaBNoCkAVEs2s2z1vQnmfa7T23&b64e=1&sign=65d0455c74fc05812ef3749334ae6453&keyno=1',
            urls: {
                492: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZldAiEK-xeweGIsCsUosIHnln_eknAGEpum07DX3KtW-BTo_a41qgiu3fxOnHmVcuMHEBE59d4HutG-BOEQUW48x3UnN9hocKiPiuYivlG9TEe1rPDMjLk3TR9zTM-UPxiqaZxsTFF_YQKI9gfAJ_9MCOMiuf5LjYWyj1G01GG178AAMdbGtMC1Zw-0rHrSfXy4yYDn5R32E4UXW4Gb82lNgVt5dp8ZhFjXFUPfpzu5_5zF6g-f1F5-2QsZz8kna7SF44Q_LV2PuRlfAs9yZlnf_H6LFRG04Za7qnxtIgvA9hYJ6rOvtKIQqM_1Oy6SwIxScxEOlJ2MsZ3jh5FeWH8nIsQ58pIiQAWf_r2F4E6R7isgKcvzy74C99H7CKYVcB9Kp8xrV_GUvWeLELsfrezZKgb_ZDYMZ5Z2NDuZbC7_GvDKdJCEf0cs2vsnhsT5KA6Aar5GONovQXYEG5U31VrqtNFhX8vaXjYCMgVdlKaF0ETO2bTpLTlNJKtyOkcVYDDla-u20vYgm_taNMIpXdbnMAdwdte_4ug8jRJcOEcwL7nws9iK0rAmZHRZgg4ydy4Vd8xiNcgQoySe2xdb60IQLnl_x4zVvCeWROsgKEvXgaQH2dp48OKqoAamrPJCrgNHm2Jq09esUN9r1c48OSmKBja1YO2HRljUgAgv6Hyi4okQxySQhWthemDPoIWcqkB1D53WrzQ4LufIOJi9loI_LRpXIcxXdZtaXHOXnjZ4PvyU6L4SCyL3fp-XMIsZhgDPgmEsBMS10Ck_nfumKyU7H6I08C7bzucHjOUcs4pqD2cd27TklLshFuFiqtxdVEGxweXN1WYM-bMgY-8xQYYo4JrsqvXJfSHm3S0sznBhIzGfxjoolLkvqog7tNJz9MfG0g_I9zEl7OWlnmSBVRjK_L_usqHAQ-AYmFiquuzI_bk7r9X1WKF_dLAcg92lNoKnkX_XjUU2OWy5KzpU67y8,?data=QVyKqSPyGQwNvdoowNEPjTZ8syOZQLnueZ9Kud86H5E4EwMo1gcBh4nkjQJu49gyxAr-gGxPo1uzk1qOwyA07850zHlpIzrJXMlYdDS1tJTynH5QF94dW_i6-Z4LPFT9TrsbRXnQod0mRevtuWpdyS6s-aj0PIkTReWUfUjbxKYKhxI-YaWoHg17gUOFIbi1A_CZmWiaBNoCkAVEs2s2z1vQnmfa7T23&b64e=1&sign=65d0455c74fc05812ef3749334ae6453&keyno=1',
            },
            directUrl: 'https://www.inpresent.ru/good/sumki-na-plecho/sumka-na-plecho-eleganzza-gr01-00028305/',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuZ7CPw_gekvtBvUOmPx563YrWE1rL-9UXzQVZy89lHe4acVjWO7TNXKfVEcyDf8ZTYIGTeNfJ4Y06HfBcQFMf8XV5sosayshWiPBuUxnsgNgRXxMHHKdW04Zjugz-_4dMF6C-ibgkAPi_M78Vq_PjJPvFFPyOtC5EheMOq0xcsT79QB7q2KnE5wfD5hHALoVeM0kpTdRVmeGzmZ2NSP3gOzucnn6b9ikL_NIYn_C2tbAFJHxUwdk4VDaDPp3wkbxUtwYoY9UIzZL6UVLrFY91graJSy3YDj-9wdxn_QZuaTx7bpBdB80oAK5SONEBZGlLLkRjCzgH3-_pOM6eaXSEsbcTBZpGLxVeP9Oq6-ckrT08-fi428OoR6DO95nFhYc59Ii_Ir-Mc9nl3pr9w2m_6-ZR8k2tkwYBPehhB0ftYMrNkh-IJHq3e2iqUd1rxAqBsyqMvi-lo06aHT7OmVP_tWWphRZ4FtXuiGMxpGQoRNIlDb-rAB0-hHYs65_dZCJB8TwthLZn9DsHGBPIpivliRJ6eJ1ig-p5VGdstFA0Dml_tLFabRmtbfyXeOYIj31J9_rEh_oRZ3hUzfa_wItj7CxtF157wi9-WA7HqM5kfE4-kuOcpZARME5fvMzpOwYFbgq_X831i54nDrk5kj3E-vQuGoXjOpZfPcxiPdJldVw-UcZwmlk_cA4omkpyab1CLZUrfXspD2dJYZFs-DFMR7DhP1oE4zF7_jRmg_vom3YN14LWeIC0E24LnqoHuj0WOtUWbCcF8CQecrryqKwvym5zijz_FXWZWyTXZA_4X2Qm-3twiPUN43x3FsqDjWnNWAmuIPMHjlYRBxlLMBm9bi6Qtq4wN0q9NRKEoF4GLzpmnzFw_9iXqDg4amDvmhQ_eAHSRajyMw6UHdFM6fAJHqvJF_K0Ftve7oi12RDUjESTzCEYvrk-wtHkYPcsXXeUYlm5o2z2eBw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2ZcKLcWSSTtsDtzu313Xt-jEX91kTXvF3F3QL_hqMs9EACF8Y_N591vPEmHE-AbkbkXGdNlf1gM1g5_Yx31KupNlPZiltsEoHilPGK8xg0IOH324c1EyOdU,&b64e=1&sign=89406fa9e99275aa119d903a2504d527&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuZ7CPw_gekvtBvUOmPx563YrWE1rL-9UXzQVZy89lHe4acVjWO7TNXKfVEcyDf8ZTYIGTeNfJ4Y06HfBcQFMf8XV5sosayshWiPBuUxnsgNgRXxMHHKdW04Zjugz-_4dMF6C-ibgkAPi_M78Vq_PjJPvFFPyOtC5EheMOq0xcsT79QB7q2KnE5wfD5hHALoVeM0kpTdRVmeGzmZ2NSP3gOzucnn6b9ikKwOBPmvQI1TtmCokf5rzdGcnuK9qg7BVCusdhnLM2keZzaLUVw3sePNHDPyv9xn-nvgb8vhGmAnNgSYZxC0EAQNa04iNyDmg4MBNqZCOMn-s2rotEbk_mSuQtEzXMegr_PAf8o_tfEJ5AjfYHWJFAyeagIiqirL1iRjRZ5vKGZZXJRfSUL5pphIIEFAYgUmjhfOUGRUF77Y_p6A5Rz4oefkipbzl8LwbMlfa-Z-uTb4GfOdOrdqyaE4PSstDaqZd32hdk92pQ7kbgzkzBK3QA_fw9z_4XlYxV-W7i73FnWIDOM27kiLiilrXmdqEbbfyPYvXfbBSlXsx4Uu45uZt5oTMGOcmurBPGzKH9SJq__CWn7t4AnGCEB2tN3W9-TBT5RyesskNOMDCySzvQRMhq0Yp8kMsFMN6C_VeZyGjDf4jkz9iPW61fn1881JfG_DgHB0RLPpooMaSkFOcmq_0LOBd-t6yXHr_sRX9Ofzlp4r7aGMAStSs2YQjPdbdRZIWoab5-foCs0Hj6bqpxZ12ZA2PmgjqGc68WZ9uWV25h41ltA5tUBDQ1pcX_g0X7AB1U9cagRo5_tzE4pA9VU2WyeQXfluntNavwwCb1kmzG2EAXuGVyk33JMimmQxylgwnZFIUfGa_AcZtKoSipRSlcAVQDCSNhvQShXGK-V7q_z3zZOESiDjemFwqrdO1SmFddVecj1jfOLFXolk9BDLuh048VpSJymmOrbwuLWo_Do8w,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2ZcKLcWSSTtsDtzu313Xt-jEX91kTXvF3GRtqUft-Bmd2kw6KemvwXhpjtmUq4idle9bk2bquBkK8JzqdlnrtU6s5nBfdYs1KMuJDDhABz7M_4UJehVNZxI,&b64e=1&sign=bd843f70e7a729d80b112fd083a6d30f&keyno=1',
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
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.4,
                    count: 281,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 6,
                            percent: 3,
                        },
                        {
                            value: 2,
                            count: 1,
                            percent: 1,
                        },
                        {
                            value: 3,
                            count: 2,
                            percent: 1,
                        },
                        {
                            value: 4,
                            count: 16,
                            percent: 8,
                        },
                        {
                            value: 5,
                            count: 167,
                            percent: 87,
                        },
                    ],
                },
                id: 3779,
                name: 'InPresent.ru',
                domain: 'inpresent.ru',
                registered: '2006-10-31',
                type: 'DEFAULT',
                opinionUrl: 'https://market.yandex.ru/shop--inpresent-ru/3779/reviews?pp=492&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 533463641,
            },
            photo: {
                width: 492,
                height: 700,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1060124/market_Lhe2vjfG3d3sgPlkdLP7bA/orig',
            },
            delivery: {
                price: {
                    value: '0',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — бесплатно, возможен самовывоз',
                pickupOptions: [
                    {
                        service: {
                            id: 122,
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 2,
                            daysTo: 6,
                            orderBefore: 24,
                        },
                        brief: '2-6 дней • 1 пункт',
                        outletCount: 1,
                    },
                ],
                inStock: false,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 1,
                            daysTo: 5,
                        },
                        brief: '1-5 дней',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            category: {
                id: 7812201,
                name: 'Сумки',
                fullName: 'Сумки',
                type: 'GURU',
                link:
                    'https://market.yandex.ru/catalog--sumki/7812201/list?hid=7812201&onstock=1&pp=492&clid=2210590&distr_type=4',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 8339083,
                name: 'ELEGANZZA',
                site: 'https://www.eleganzza.ru',
                picture: 'https://avatars.mds.yandex.net/get-mpic/199079/img_id6881441624180787991/orig',
                isFake: false,
            },
            warranty: true,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/zAz8Z43Dp2lYR9PeOpH4tA?model_id=533463641&hid=7812201&pp=492&clid=2210590&distr_type=4&cpc=JIGWPUvXtowhz3rNkpO3_PkF1UmvMpyDuPty3Cb-hgUFnhGpi-9LPzSn5PTA5Ng2Tqddkb8-iBZJdhPagb2OsZ30sdPpW6PkE61KWSRY-HwW5gy5DCh3C-0iATJRYwl3ZAb0CPbZdujK1b6HzJgTC1On4Koam-B1&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1608,
                    SHOP_CTR: 0.001572561334,
                    NUMBER_OFFERS: 5,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_Trivial',
                        value: '1',
                    },
                ],
            },
            photos: [
                {
                    width: 492,
                    height: 700,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1060124/market_Lhe2vjfG3d3sgPlkdLP7bA/orig',
                },
                {
                    width: 473,
                    height: 700,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1617999/market_Pi3Wmw5bgLq2OpGzZOaW0g/orig',
                },
                {
                    width: 494,
                    height: 700,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/208616/market_3L3mYf_9O8zOo425TsdM_w/orig',
                },
                {
                    width: 525,
                    height: 700,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1402605/market_CO3r7yy5euuuYMIiwNmZ2Q/orig',
                },
            ],
        },
        {
            id: 'yDpJekrrgZHaZYm96Uje2390wLVikbUw7pkG4Zo_VzbfnuC1DOmRXw',
            wareMd5: 'UoUNM_r3YWV_xcppttjM0w',
            skuType: 'market',
            name: "Сумка Jane's Story",
            description:
                'Стильная сумка из натуральной кожи, прекрасно сочетающейся с фурнитурой серебристого цвета. Основное отделение на молнии, внутри два открытых кармана, один-перегородка и потайной. С обратной стороны карман на молнии, длинный плечевой ремень прилагается.',
            price: {
                value: '4565',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZhVZAW__Zx8WZncb81V4LJGfrvW6fV9VohpQLJSEJMwiyLun7DNCGqPjPRSwPl0cJeET84ZdUsZDHIwBIBNzt1OLqZ8Mog63PsTsoNJWuQwktIocHNjV0WuFp35cyIkIkjkNxoY2Putlf-VYWnGaGHY3S3dY2aL8ydkheOyJEG_pbhUeA7fuJxI_Eli6M4drQXpf_0k0X_mlfgBobmVdiWoEJ6fjae88-HKuJa9Pzg6PBQ6nNTgJ6o9NeVeq-jU5tRRVD34k40gJBB3w4JDKrMEGYIN0fu1KqIV-uT0_CDuqmCkC0IZuOB3Op0T0U_fTHDPiHOARJzIEqFY4Xp-WJ2zUzf52xLpmo-Xq1s96kKGl0twwHS_ypq7ZcU9S4Q1BOD7QIkliYOpXB5YUwnStyvqwZL2aAIhpcYHF6BXSBL90W7eTDxWh4XrYUZrq6hC1Xnyeby7tcKRmOe3o4k7eDLG7492Xv4NeE0_8_N2DS6BZrXRqCI0b_IMq4IdzQnrxJhbtNmuzG5W3RDIUHUk3kQSiSwnRvzG-q9ZyTH98FIrntCSucpu17roUt98KBxD5x6BebKbBWuCfSvue_0ReNvcnwifDZuGif2wVn7GeHCJUXe-A_LXE32ehfUb0UOl-Nu4StAxfT1W_pA1zkxsGLJ53lUmPbq9xZeIPWsT81__QwFB04I_tyyj7h-vyOnu6g2W9LNOBT-pJUZ6lY6bfSEF5rmek6tPY8wH0sDvdF2HFUCL8ZGnqvcRToz7EV0T9SX5SpAdhNVsbZsWvn6jUmq07eIegMzyrU9HlN01dh7xbJ-s3DFpgUrP9MH2dyk-mmnmMaacRKbZJn1oeTQ8Fl9waS0H-NVawJG3I-kKJ6KpQ964XaOQzzdOT4UKBEVi5ZOL73xrux0IYg_Yi6zJief2TO1a6rLNrNfz_26RUCcHtApYvzSw1v8YnWLMdySvMKFQqZYPN7lNueq77lEV_ZPA,?data=QVyKqSPyGQwNvdoowNEPjUO6Lp2FicSHwuLrkBRU9MB2L71swKP9DwSZnoHPlJhRZwJIdUCMFHT-BoifbhtQiPoLGIwiBvqoalssJF3toomceMRcCcAxwwyWQBXh1-c5u4D23h2k-9J5--dWy_dzyfdvjsM8I_eGkqutj0fgiXNbL5zYI9F8QQeAY8pvjaP4PfsNtqJM70QQ2lYmIgc0-8gPMyBt3Zs8Km2DbaLP4Yf2qkRRLKHPLT8qm3D0wcXENKPREV8z0ifCjjErxT3QFWZisiB4b8gu3ysdt4ifiOUEgXM-rkWwGt0yzMtPO_uWdF8KM7e0JdxtV6oMElP6cw,,&b64e=1&sign=0c01045ac898a2f8dff5c9a61623b874&keyno=1',
            urls: {
                492: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZhVZAW__Zx8WZncb81V4LJGfrvW6fV9VohpQLJSEJMwiyLun7DNCGqPjPRSwPl0cJeET84ZdUsZDHIwBIBNzt1OLqZ8Mog63PsTsoNJWuQwktIocHNjV0WuFp35cyIkIkjkNxoY2Putlf-VYWnGaGHY3S3dY2aL8ydkheOyJEG_pbhUeA7fuJxI_Eli6M4drQXpf_0k0X_mlfgBobmVdiWoEJ6fjae88-HKuJa9Pzg6PBQ6nNTgJ6o9NeVeq-jU5tRRVD34k40gJBB3w4JDKrMEGYIN0fu1KqIV-uT0_CDuqmCkC0IZuOB3Op0T0U_fTHDPiHOARJzIEqFY4Xp-WJ2zUzf52xLpmo-Xq1s96kKGl0twwHS_ypq7ZcU9S4Q1BOD7QIkliYOpXB5YUwnStyvqwZL2aAIhpcYHF6BXSBL90W7eTDxWh4XrYUZrq6hC1Xnyeby7tcKRmOe3o4k7eDLG7492Xv4NeE0_8_N2DS6BZrXRqCI0b_IMq4IdzQnrxJhbtNmuzG5W3RDIUHUk3kQSiSwnRvzG-q9ZyTH98FIrntCSucpu17roUt98KBxD5x6BebKbBWuCfSvue_0ReNvcnwifDZuGif2wVn7GeHCJUXe-A_LXE32ehfUb0UOl-Nu4StAxfT1W_pA1zkxsGLJ53lUmPbq9xZeIPWsT81__QwFB04I_tyyj7h-vyOnu6g2W9LNOBT-pJUZ6lY6bfSEF5rmek6tPY8wH0sDvdF2HFUCL8ZGnqvcRToz7EV0T9SX5SpAdhNVsbZsWvn6jUmq07eIegMzyrU9HlN01dh7xbJ-s3DFpgUrP9MH2dyk-mmnmMaacRKbZJn1oeTQ8Fl9waS0H-NVawJG3I-kKJ6KpQ964XaOQzzdOT4UKBEVi5ZOL73xrux0IYg_Yi6zJief2TO1a6rLNrNfz_26RUCcHtApYvzSw1v8YnWLMdySvMKFQqZYPN7lNueq77lEV_ZPA,?data=QVyKqSPyGQwNvdoowNEPjUO6Lp2FicSHwuLrkBRU9MB2L71swKP9DwSZnoHPlJhRZwJIdUCMFHT-BoifbhtQiPoLGIwiBvqoalssJF3toomceMRcCcAxwwyWQBXh1-c5u4D23h2k-9J5--dWy_dzyfdvjsM8I_eGkqutj0fgiXNbL5zYI9F8QQeAY8pvjaP4PfsNtqJM70QQ2lYmIgc0-8gPMyBt3Zs8Km2DbaLP4Yf2qkRRLKHPLT8qm3D0wcXENKPREV8z0ifCjjErxT3QFWZisiB4b8gu3ysdt4ifiOUEgXM-rkWwGt0yzMtPO_uWdF8KM7e0JdxtV6oMElP6cw,,&b64e=1&sign=0c01045ac898a2f8dff5c9a61623b874&keyno=1',
            },
            directUrl:
                'https://www.ozon.ru/context/detail/id/148042515/?utm_content=id_148042515%7Ccatid_17002&utm_source=cpc_yandex_market&utm_medium=cpc&utm_campaign=msk_clothes_mp',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuE8uVOTZFhEU1sQ-R7--brXzA00GrvbMAT_qnOBHBHE3xQwzKlPTw_VpgWwhyjLC7U9hKs8YK3sxx9XHxvsTfgS1BqhwM889CDxG5C7Rtytv2kZp3HIl2SnfZZXBozG7mVSI4DtJcu3GtP42o67l_DorZcTwxvz6pslh8m_f4yro464kD9hs812HHNdqx346rth0na-TiFqIzxGRFRr7AjNdEIrIujPPcZuZlYa5NP41cm80n5i6xV5hvTFlJkL0UkxaO_Bdpix5h373YEDHn9DSH6ind7eKO75VK2WDjgPkaz7SaD2FoBGPl3qlFHs7HgIj4_y7jCSyCIL7wYVvZhvZN2SjhxF8IIuXogKuWOexk_TxZytLg7zUjDfaDILcHWqlYt1G8zui6wTkG6jI0xEocafiMQCyGkD2IebX2ocWhqFrG4hP2AMBTYpXwfTxb5-arIPlwG3zkZdmvYLFYgZX53wJRW1lVXIoCthXHt50DAncY-pOl8VC1TAzz4B1tfiTf8VZfZTZMbkgzu2N-oD6HD530USqQBM7qs15RBTeGNTw1srx6ojcUurvCJ1rGtOKkvWlTmxuaGP4gQWOVGo7j6E6VPg-sao4FCECzgO4jzjbvNdXmrF6huUG8C_RJlrdZ8iw0A7sC6oMsy7VNhSaE3clzNuIoIlA50KYmIapl_9mJ6g1-vRRgM-ImTHSgLTG-SRW-IqjGSgLHkE2cxKPwNdS28P7hW0wJLNC3USiCuQdu-e_PyLJnpkBI7FQz4xMvZP-gqlkQ3Dll5yM8o0A8tLe9VHMcK1cZJY6jubv44Qy3-_EwzW-_muL_Qh0yXgYcqqzMO6skeofSWKTZrUYU1UrGwetuerfteIBENOKi1E9oV8rfiUiguTGCY1JT21z_--26HT9iaxEOU4SPUSLySfcFODUlNZ2OD77sbEMRiYxEYvkhOoVUdn2IyHT0PFV8PTC_tAH8d957P8Nhj?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cgu-yFWmVG6C0C3Dzlt1h4EUIlYLlPC9XmdQ6ZeuPlUZLTbk6341Z3GXyLlXhYTc_ObxkJaECM2JA6kdmzihBdWC_7qM8UYVaTwRK_VozatbRbBvEmhdzw,&b64e=1&sign=2973377c992403326935254e90422c12&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuE8uVOTZFhEU1sQ-R7--brXzA00GrvbMAT_qnOBHBHE3xQwzKlPTw_VpgWwhyjLC7U9hKs8YK3sxx9XHxvsTfgS1BqhwM889CDxG5C7Rtytv2kZp3HIl2SnfZZXBozG7mVSI4DtJcu3GtP42o67l_DorZcTwxvz6pslh8m_f4yro464kD9hs812HHNdqx346rth0na-TiFqIzxGRFRr7AjQPmURDA7Kov26uGCCAqJaJkyOoO-kYNoJu017o5uQiVdGx8AHkcXv5UpVnbiYmVzMged6YPQtKFa5-Kbm3HPrRUKTP1zh69xEyrx_KMKxQwdYGmDQCUAGdGTaFJN2tSQtpMKcNilJH3jcKKGxe5V8ogExgYfwwrKctJwPA7u4SoxGjMAipLylJt5qZGZRQxT-d6OVrnFv-Z9nLXbXIVWe9_HHrZqMBdtz3FvVco1owGMpd5gOjVcdgE3svkzWiqRbZsaCsXCgJ99xsuGWWfFMmb8y4mlF3HBBbI6zfAeCeqJQxknOURAlKEQ4jybzgrr8fqy54RgoqR_xC7V22zfKg-wmP8NIaFpB6pyI8CeVmbE9TlsRJ9BW3E0z8Ws7nhBg4QzZwn_gvLgMbLMbTocgZF83Qmqny8q7LrTyeM-lR64sngI1HbOHJPLed-E0nLaCtip8XRWhsNZ2iAe8VOigO2bsA42W8nMfTnYcTX0EED35ZVALbsf3wQ4LOgghghw7AHoAnGb0zanMZJf_jQ_rpMn1-88ALl6zYGcC1VZvvO6BfSmc__QWhIDX71Cnu-8XKXRV0J5iE31i1Q0V2Bze8S4-7mNKo45JDcCiLfpY7nYEGT9Nh2KZp8Y9bMsvo0BvZgFbmO3obHaJWj0oPL3WB52t4knBMwj4rO5Vs22JFKVzsl5KiiALyt5ufmN69Lj130dbMFWIU4W3ZRoK113XIPXGahTi3vnAPw_fBADx3FG2k8-HMc7nbtYZ0H_1llU?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cgu-yFWmVG6C0C3Dzlt1h4EUIlYLlPC9aHDWT50pGdYCiP05-ho4WgcmnTysi3chzllgur2kETju-oOOBkkIF2GT___PzpCPDhKrZE9ZSTqSMPm7m-dRUc,&b64e=1&sign=b3253f13f0ba9d48844b626e8ee4edfc&keyno=1',
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
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4,
                    count: 92553,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 10950,
                            percent: 17,
                        },
                        {
                            value: 2,
                            count: 3041,
                            percent: 5,
                        },
                        {
                            value: 3,
                            count: 2814,
                            percent: 4,
                        },
                        {
                            value: 4,
                            count: 8336,
                            percent: 13,
                        },
                        {
                            value: 5,
                            count: 39346,
                            percent: 61,
                        },
                    ],
                },
                id: 155,
                name: 'OZON.ru',
                domain: 'ozon.ru',
                registered: '2000-12-22',
                type: 'DEFAULT',
                opinionUrl: 'https://market.yandex.ru/shop--ozon-ru/155/reviews?pp=492&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 545569103,
            },
            photo: {
                width: 1000,
                height: 1000,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1107271/market_nVMokC8pmD2ixA7uMNKeow/orig',
            },
            delivery: {
                price: {
                    value: '0',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — бесплатно, возможен самовывоз',
                pickupOptions: [
                    {
                        service: {
                            id: 116,
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 1,
                            daysTo: 2,
                            orderBefore: 24,
                        },
                        brief: '1-2 дня • 10 пунктов',
                        outletCount: 10,
                    },
                ],
                inStock: false,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 3,
                            daysTo: 3,
                        },
                        brief: '3&nbsp;дня',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            category: {
                id: 7812201,
                name: 'Сумки',
                fullName: 'Сумки',
                type: 'GURU',
                link:
                    'https://market.yandex.ru/catalog--sumki/7812201/list?hid=7812201&onstock=1&pp=492&clid=2210590&distr_type=4',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 12349901,
                name: "Jane's Story",
                site: 'https://janesstory.ru/',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/UoUNM_r3YWV_xcppttjM0w?model_id=545569103&hid=7812201&pp=492&clid=2210590&distr_type=4&cpc=NoDs-hOevCb-6uzRQe03eb9i7Y1nsXyFPWFz8ca15fxPMDZA52M9B8ltOx1a3dhC-UInuIdLj4kFy2egcwB0-JL5Jlj4bfgO3etm-dppHQxGepHYbXllSBLSdSX5T8noSoDok-amc--SiwvOqBzwd1m863TjPccs&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1608,
                    SHOP_CTR: 0.003947885241,
                    NUMBER_OFFERS: 1,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_Trivial',
                        value: '1',
                    },
                ],
            },
            spasibo: {
                receive: {
                    points: 136,
                    percent: 3,
                },
            },
            photos: [
                {
                    width: 1000,
                    height: 1000,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1107271/market_nVMokC8pmD2ixA7uMNKeow/orig',
                },
                {
                    width: 1000,
                    height: 1000,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1860579/market_F3XTFu97w_sp3xnhSX4toA/orig',
                },
                {
                    width: 1000,
                    height: 1000,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1855404/market_Jm9Zwfo9cVN1IaaW4Rjcew/orig',
                },
            ],
        },
        {
            id: 'yDpJekrrgZG-0BViUPvk11eG4SAhE1qRXF3YOVYU_WXaSn6rvndsFg',
            wareMd5: '0uHbg_D33J5DJiedBt5KTA',
            skuType: 'market',
            name: 'Сумка Mellizos',
            description:
                'Сумка испанского бренда Mellizos. Детали: один внутренний карман на молнии, два внутренних кармана без застежки; три отделения внутри.',
            price: {
                value: '8551',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZj_xoa-55fk0NfAVRlT0Xn_qjDipnqM5tIfbmw3f5Cfdfh8AQZyDi19FhwpEM8fcmy6QBvXW1MnqFkAEaIzIzAp2n6pJg5j2OcnY4bVg4s3zvhoD98TD981KhWsgMuZ4vuTcXlAOTSkxsfY3kWAOvo_RDoOzQpfkBZ5Ixq_Pnk8cFRLYDzwuJqC1oolGK9P_TT9slK5tX3Q-mihabOzf1BGHf-IV4cPXRgWHKynQPOdPIXvqGB4vaZ42rQvOgdXGpX8n2laah8e_3qjgzyW1dctq1_ULoGwexcanKuXvfV8TJBzT3_88lbV_4D6vAktjIeZN1U7XDEoPGaPNmub9wfrVUX3hXxxdpmAgZGUk3bx9d9faCxHxskuHwMb5z-ePKKMXMwLx8QVkJXCKoBBH2axWmYm5k5AbJ5KH8sX3AbwU6VIgogXEKoj-aqmymCl-88n126aWipXzhZhEbgY1JhLVOus_6domuDqCqxEnFHDIvcblR9DO-p0251Qtholm-Dj0FoX0H7dWIGXEdLt2-FK4ijSqvZrSSVI3fCEnUJ__ijrZZ8mncjotvYDhCNN7KYN_mMCKvfYJMBt-ag23gytjwK4ytZjoJTapJPY4gXSeiW3mFQO8wT94ALri0tO9OE5-NCYzaRR1GOxk5LEfdWK3KVGpZldZPHnW6XxcgeVuas9g5uPRlS9tgPU_jGX6Zy0t654R7S96KD-nYCfWOfFIhq3JCcWW9MT2K_R5xCZGfcxaKQl-IzeO6duosTSISVbvfG8rbvY0qNc0GmWXqii2D7p2bTPeKFuRF2uFPtKDlJ_g6Aj67Bigyv5vgYsJFAyGE9lJEBYCsoqwTZieFDr_GpLaIoYkzGomSm75RkRf9ZspS6iCMd7QC6MjYkxyns-9jgVx6j91qQeTjmJp2c5U4pYh6HF8Hzna4LgG5ZjInRvNTq4f5mhL5e1d-pk8SFNjfjWzmegqhv5HQ7usttM,?data=QVyKqSPyGQwNvdoowNEPjUO6Lp2FicSHwuLrkBRU9MB2L71swKP9DwSZnoHPlJhRZwJIdUCMFHR1OVMpdfp20SklXfCizF78e3ItWvwhMqzgeMQJzfix0R1X7o8JMlWg3CLjzO9MqbOjGwo9KtLJlsOo_UXvy147U3rIbQ45r2ZWLSkZKa82XaM7t1rnBopcnmhShq3V7uhl66kzzAcZbLDjB-AutDPSQ3YAlkvnzp5EhaFDCyzUGIDx8fXjYplfCJCTFGageyyhEW1-LT6ygvd_vocixh4i2I3fy4BvDgzU3tD9nt0K-4m3Hd0k0BpYd4LZ6t5kzE01_iVE5bieHA,,&b64e=1&sign=ded11eab114bfd75b71edd970f9453f9&keyno=1',
            urls: {
                492: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZj_xoa-55fk0NfAVRlT0Xn_qjDipnqM5tIfbmw3f5Cfdfh8AQZyDi19FhwpEM8fcmy6QBvXW1MnqFkAEaIzIzAp2n6pJg5j2OcnY4bVg4s3zvhoD98TD981KhWsgMuZ4vuTcXlAOTSkxsfY3kWAOvo_RDoOzQpfkBZ5Ixq_Pnk8cFRLYDzwuJqC1oolGK9P_TT9slK5tX3Q-mihabOzf1BGHf-IV4cPXRgWHKynQPOdPIXvqGB4vaZ42rQvOgdXGpX8n2laah8e_3qjgzyW1dctq1_ULoGwexcanKuXvfV8TJBzT3_88lbV_4D6vAktjIeZN1U7XDEoPGaPNmub9wfrVUX3hXxxdpmAgZGUk3bx9d9faCxHxskuHwMb5z-ePKKMXMwLx8QVkJXCKoBBH2axWmYm5k5AbJ5KH8sX3AbwU6VIgogXEKoj-aqmymCl-88n126aWipXzhZhEbgY1JhLVOus_6domuDqCqxEnFHDIvcblR9DO-p0251Qtholm-Dj0FoX0H7dWIGXEdLt2-FK4ijSqvZrSSVI3fCEnUJ__ijrZZ8mncjotvYDhCNN7KYN_mMCKvfYJMBt-ag23gytjwK4ytZjoJTapJPY4gXSeiW3mFQO8wT94ALri0tO9OE5-NCYzaRR1GOxk5LEfdWK3KVGpZldZPHnW6XxcgeVuas9g5uPRlS9tgPU_jGX6Zy0t654R7S96KD-nYCfWOfFIhq3JCcWW9MT2K_R5xCZGfcxaKQl-IzeO6duosTSISVbvfG8rbvY0qNc0GmWXqii2D7p2bTPeKFuRF2uFPtKDlJ_g6Aj67Bigyv5vgYsJFAyGE9lJEBYCsoqwTZieFDr_GpLaIoYkzGomSm75RkRf9ZspS6iCMd7QC6MjYkxyns-9jgVx6j91qQeTjmJp2c5U4pYh6HF8Hzna4LgG5ZjInRvNTq4f5mhL5e1d-pk8SFNjfjWzmegqhv5HQ7usttM,?data=QVyKqSPyGQwNvdoowNEPjUO6Lp2FicSHwuLrkBRU9MB2L71swKP9DwSZnoHPlJhRZwJIdUCMFHR1OVMpdfp20SklXfCizF78e3ItWvwhMqzgeMQJzfix0R1X7o8JMlWg3CLjzO9MqbOjGwo9KtLJlsOo_UXvy147U3rIbQ45r2ZWLSkZKa82XaM7t1rnBopcnmhShq3V7uhl66kzzAcZbLDjB-AutDPSQ3YAlkvnzp5EhaFDCyzUGIDx8fXjYplfCJCTFGageyyhEW1-LT6ygvd_vocixh4i2I3fy4BvDgzU3tD9nt0K-4m3Hd0k0BpYd4LZ6t5kzE01_iVE5bieHA,,&b64e=1&sign=ded11eab114bfd75b71edd970f9453f9&keyno=1',
            },
            directUrl:
                'https://www.ozon.ru/context/detail/id/150882159/?utm_content=id_150882159%7Ccatid_17002&utm_source=cpc_yandex_market&utm_medium=cpc&utm_campaign=msk_clothes_mp',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvYuCawvv6yrTOD-1uj_pOXTtJpmY0umcYku1k9-grpl7cA0bV--LOP8xhKDbrcRs6G3j8PYB-o6MTFhmKiK4Rk06lSnA55Zk5hnbpAdABsQOTtygqtmuWAs7nBFmaLS-fYEifPz_64mOxkZizV-zwXKIb_TJEtlq0e4YXD6sYtKGe0eoGsl91N0Yis7gt8H0CTC4nWmsIj1wTqAineLAEStvrAN-eLtz_K7qNPJVKsLpo993HuZvQyo3TYIFkuAnOSr_MhYAMK7COM7wDFEBfwMlyZMu9M11Xj6D0aWO2U6nkWi2kCBxJY7bZnxqV7a-Mqoh6uTh9w8oc8toeQNw2a37DYIh4ErScAmUMyl--AclKUpq2hfsnPkXyD-biwuEEMcjVvvdUeuIfj0rb55U2Gl1B_GIEdHoskmzGS3XDBTr5H4HbQOlKCDEbkW-2mHxB4r2-zsBROK4UXxHLk25tJS8-TIwf89cLjpadXJunM0mkP6onN10A6hzNikBQBNJFXC46VCj0ylbiscvDB-aQfkek_KIyiIRhcFoszPgl2ulsztS3F7THBrflQvaYStCBOkQl5d254jH5Io87S43O6dzxDfYcpPAC35DVrhBm8h6uuc1mUjqiWXy2OjdCA5ZO7vtBDaGrMHxz4tFW5Q6BYVQGPvtLWNLeeZE3pWRQw5GS83UgyWfcY9rC4PyIqequiUChIHQ6JEXDR46SmUClonna21l3tcMnQJe-SAJ1XWWRll4efG8cPJrD-v3WSzkzPI3l9TgeKCIa-6GjspirIfsct76ddjCta0_Vmc0Ndv-iWSn3oXf7nTk9jgWKH3EL7xX1_GaZ5siUDEIFDVGSVU-6rw-wQ5xjPvEOekOcKdJ-GFxyl2nwuZcbfEK6A2vhVUsf4SVVQKjGHyZVQnPC_tQjrkmoTRHjThqxdgCW1KCM1vQqL3PzqZTbiH_PnFNDFFEPY7z6DUqqhebDfl580?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cgu-yFWmVG6C0C3Dzlt1h4EUIlYLlPC9XmdQ6ZeuPlUZLTbk6341Z0LgUsTiK17kKUCffawV7mKd4WBU-PTCRDf1c6_4ChUSRmvEgkG-0o6eogI5V_WrUA,&b64e=1&sign=ad7a08b0b07b85c58a8625bb9cda3e1d&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvYuCawvv6yrTOD-1uj_pOXTtJpmY0umcYku1k9-grpl7cA0bV--LOP8xhKDbrcRs6G3j8PYB-o6MTFhmKiK4Rk06lSnA55Zk5hnbpAdABsQOTtygqtmuWAs7nBFmaLS-fYEifPz_64mOxkZizV-zwXKIb_TJEtlq0e4YXD6sYtKGe0eoGsl91N0Yis7gt8H0CTC4nWmsIj1wTqAineLAESkPCjRr23rQo4DL1jzE1jO7brbjryP8wSayrQyAiBFPGOLuc0P9PUX9l9qcf0VWI8Z-yCpyuuoIMAW1vUKO-2k6xJG2xKZvIieBPJMuJkgiSmbHGVZG8KirK6KIgIRs4-j7w4S2mW__jHiQev2_jpM7MydeT6_cmwDzp9HkmZF6jtceSPIYmvSPB5_e-GuoKn_HPzjzUjeb_TqWrGBCLpjzhXJxoqvVVaODMTWmTkOZk-DaZRU52qGR5wy2jGMbI2KoyRFGC0gc-w-DRI2VWB99uJOhsSW9sWN7Dp--FedOV3gHI26mt-J_zmIQQ6nL43LpQXB_g_RfaMllouiTpa7RS_9E2bVMIpWXJxHdY8wd-dD7bwRTu8xvetSSQWoRWGHfC5nngKNPbPx3VIxQu1HwpYh_yeKuI_p04wTiVBLOPg85IWz2eksRuqQJwkiWa2ixWJBi2Uy5gf33V0WXOnBb0e2wBvK8moiHQDrT-Pr7mzPZDH_0plz2lyOi_ujymry4in6YsgJGdc6zu0Hghi5OHvji6YOtHrhJiLE--jttIOZFevhS-jMnlsXeSyb16qLkg-nfZNgqalbm86X4YT5v91JOBUydQRDqFssQ60GS0K_t7dLl2eLcne9NEWzEs33ofIhwfFnZ3YA7UyqnSXiFi8QcZZE09voJOjkM6LcGIPtSVpNgdb7kyfwlgaDAYnO2Zc1OZqGq8tNZuKv3oOhtEzkSzoHT-6nYAeJ5T_Gg_vgt-EfaTb5FAJnpKPt9xv?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cgu-yFWmVG6C0C3Dzlt1h4EUIlYLlPC9aHDWT50pGdYCiP05-ho4WgqKpIx_Jjmg0-C7E3LdwrOFRBoJmOTozTXq0GUfMSanOrsf-OknuWikE3bL60n2VA,&b64e=1&sign=4ffe0b0f83391ae6af4177a5aa5a6091&keyno=1',
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
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4,
                    count: 92553,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 10950,
                            percent: 17,
                        },
                        {
                            value: 2,
                            count: 3041,
                            percent: 5,
                        },
                        {
                            value: 3,
                            count: 2814,
                            percent: 4,
                        },
                        {
                            value: 4,
                            count: 8336,
                            percent: 13,
                        },
                        {
                            value: 5,
                            count: 39346,
                            percent: 61,
                        },
                    ],
                },
                id: 155,
                name: 'OZON.ru',
                domain: 'ozon.ru',
                registered: '2000-12-22',
                type: 'DEFAULT',
                opinionUrl: 'https://market.yandex.ru/shop--ozon-ru/155/reviews?pp=492&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 568526908,
            },
            photo: {
                width: 762,
                height: 1100,
                url: 'https://avatars.mds.yandex.net/get-marketpic/167181/market_uT6HZ-QN8hqTj8h__ZcnMw/orig',
            },
            delivery: {
                price: {
                    value: '0',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
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
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — бесплатно, возможен самовывоз',
                pickupOptions: [
                    {
                        service: {
                            id: 116,
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 1,
                            daysTo: 2,
                            orderBefore: 24,
                        },
                        brief: '1-2 дня • 10 пунктов',
                        outletCount: 10,
                    },
                ],
                inStock: false,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 3,
                            daysTo: 3,
                        },
                        brief: '3&nbsp;дня',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            category: {
                id: 7812201,
                name: 'Сумки',
                fullName: 'Сумки',
                type: 'GURU',
                link:
                    'https://market.yandex.ru/catalog--sumki/7812201/list?hid=7812201&onstock=1&pp=492&clid=2210590&distr_type=4',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 13074379,
                name: 'Mellizos',
                site: 'http://www.mellizos-acc.com',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/0uHbg_D33J5DJiedBt5KTA?model_id=568526908&hid=7812201&pp=492&clid=2210590&distr_type=4&cpc=IA8a5QAMBGkl7_CCvs1sXeoixKhxm1_Yrp_880WfunDBlRFAajlHHiNzZTXlBjwdqSbDcLWVzVRmxUGwLlwbowbRyoqFTExDP_yaPksPyA0ScaacJmd-ryE_5m-fnR3iSXi27F7gcwL2dNkNywFlRNt1Zr5mvYCw&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1608,
                    SHOP_CTR: 0.003947885241,
                    NUMBER_OFFERS: 3,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_Trivial',
                        value: '1',
                    },
                ],
            },
            spasibo: {
                receive: {
                    points: 256,
                    percent: 3,
                },
            },
            photos: [
                {
                    width: 762,
                    height: 1100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/167181/market_uT6HZ-QN8hqTj8h__ZcnMw/orig',
                },
                {
                    width: 762,
                    height: 1100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/331110/market_Hd9H0UYqBdG2S_m4wLRrng/orig',
                },
                {
                    width: 762,
                    height: 1100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1779757/market_FxVSMnHIKNElHPmT4hA2nQ/orig',
                },
            ],
        },
    ],
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
