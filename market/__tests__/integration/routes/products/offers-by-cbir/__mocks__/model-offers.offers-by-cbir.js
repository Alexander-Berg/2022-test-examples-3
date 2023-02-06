/* eslint-disable max-len */

const HOST = 'https://api.content.market.yandex.ru';

const ROUTE = /\/v[0-9.]+\/models\/offers\?ids/;

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
        page: {
            number: 1,
            count: 1,
            total: 1,
        },
        processingOptions: {
            adult: false,
        },
        id: '1571860682843/f19da843c97abff2c626a35599950500',
        time: '2019-10-23T22:58:02.902+03:00',
        marketUrl: 'https://market.yandex.ru?pp=482&clid=2210590&distr_type=4',
    },
    offers: [
        {
            id: 'yDpJekrrgZGNpLUTfBjKFJuqn_nWIzNZ0EDlvihTZ037_HG4SYQeDw',
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
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            urls: {
                482: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            },
            directUrl:
                'https://slepayakurica.ru/sumki/sumka-zhenskaya-guess-hw-vg74-39060-black/?utm_source=market&utm_medium=cpc&utm_content=7885&deduplication_channel=market',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZnCMIP_BgY_pMYKSkOCUpFV2Vdz10QvW1-I5T_d21cXSSMp68RMdWuJv_cGK4qhRYHSOVxlUV7dyn1DvOeVzAxrlx5F136EeGglInY6zuYFNFyBbfjxYWPzukZCRxMB9j3UF9RV2vdhw-WlnkVDveN5OhrkKe4J4XdHkK2HouIxEOmYrk1SuWQ2ODeitzDRESSXgMIt7t2g1CcTchR3uuMgL23A1D6XJnblEMynvTWshcJcHIS-OlC5RgkNVTmraQwylskrVtT-NHhyjBY6n_3dqrlnd6K04gH7as2ZbT7R0RlYhkJlDuWr4NPM-g7B7h9iUrGpPd_Yng2RHszcMPxYMEAq2yGJJTlswv4Qi_qegzhke2KO42WIfQiT4KE0I0enyJdwwT4dsefvS1fr_NcKBewnYkBVsJ7mhlgJjD-ag7Q1yQQIRUxZ_jS_yMG-ky1r-46YIFN-I5kuU-hgGCUu7n7EFdAnwVspVP_DseHQR03pOQJ5zgjrGH3ueeI5aoj-i9JI9sQSfXqj-9_PiCeBFdwRvN_dDKBdP5E_l7KFjUDtGmv41D_9KMV6pSwJ1KxfkTdnenWK4dCgBUvKM-JR0zWFWLBAam11j_RRxVlgKY47SgZ46HDuxx2Z4z8UcrUCH_kjvn0D-1H4hWMbFq1rdbykxYOyRCSXrCGTR9csy1s8tQKzUrTi8xyLHSPgneHzCnjW8NOCHAhbtKoR2N9rqXzfJiLOXsFszg-0opILghki2U0jej4g,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_Mh10p3rkVEwce0QVWd9XunfvxEX_MDr10MhMvH3u0JecsH5Qw0O-JDZ0na_j2fFuSWenDqZv8XjwsAEAU5BJ9E,&b64e=1&sign=df71c3f91df5b34446c202e2e71a8877&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZSihUHLaU1GXvp6LSIuPeI5_dVW9YLeXntIKqZ9HlH1Cu589kewjMYpsBJIe1XT0FEnhFF-cbqPYDBa0o6H5Y0HxvuegAWwZoMsp03qqBnH5rz-7bjTzpZSva89FqVbBL5lJOBBbPVEY_wbegbtA6aFZnyd88frSvwX2PisvBbBvabHmHAkqZJhfO12vYZHPy0xo4iqumVqxPTSEz-qtnfeSL9L2axi--IoWC0gmcSO6arpdQBl54yzrZqCaydg3yFsd9hjfYdC9uqsgj8QbcIeopXAD71E-se_h9z3SbOKcUflDJnjRXHzguQi-K0dCUQH4c2xTAXql1aJVWSTfdomWn8mN7wzJG1UniBiA1LLQVMGzHB_yGcYjkrYpxOvSBYPt8F8JEz8j9CcLHMJeiqna84il-hwnrO5-_hgRBIErVAfs8mbL7GB4ydwtUzXMoLlAbKWBoR1UCC7VHwjGJk28KNA_evYYYAEmjL7IPmkecDd-bFJPfV2qU5Yl-YR5fZSgauEajTyv19rMAMsxGEzz5lTLkJj1pi5xiClT6uR0y4AgP7rfJz-iTOvmh1ADwEW_eOVg6IiFccN8Uxu-fK9doYFUTXcVOcVAH16JSArJbnFBWkkz7m5tXLXQX9mNAm38IvXe0vl96R0mgVvpXTMzCbTjJ7fje6fPpYsafwEYpds4Whl-Oai8MR0llV0uYRa5gPS0EGylxyb5l_5r7enmT6td6T7INXMc6lDAmJZoADaIOHnH-1A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_CzfxhuRiCDkt2MBTYFlJ8Y51YReytA8GUtAxGYyUGB-q8MapEQpj6Dp1zApgZhN6GulTqW6W-fi5Mdb4AsujqY,&b64e=1&sign=b618bbc8981b0a086e1f9a99bdb86603&keyno=1',
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
                    'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 494192821,
            },
            outletCount: 2,
            pickupCount: 0,
            localStoreCount: 2,
            phone: {
                number: '8 800 500-53-29',
                sanitized: '88005005329',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbnNgSTWltlSV-vcM82s2rlzi5zT6__WwG2rv0CfFbD67EeEawlsRbx5697ettYI1Y740IegvR5DmLPAQsm5vQPEEFjBu8u_TFV1lezHRrXTIk4tEO-mpXv6hF74-bVg-HoEhH5Sw4DGnQBuDfm_Spcnt0yDa-Wsxk3RaibYCSkfZxmYBn-bQ_w90_KDbHnxzPRufE5br16kifQuEKEM0t-HAkxceD0bl7vsNGEf0uo7HG4pClAc6qJd9EhbyX6TfeEPWuCvUEnUSj7QHdm_4iyxit71uSEfQ7L0TTxP3PjJOwisgwNxCaWT6iGwxkRuNFXjHjNbBGo-EQNOuFG9Hfpw0tFhRMSBGWQLn91-SSesfCw8f_rM7q5noB1l0mC8ZGZp6DwZ8kbITALdVX6QVtogJaKrcjNLjJi0TUnwAYnic4_f4DYHlHjS6zMKz6PxyQaJCsiShbLF0Wq_1aBsf3rNcp2hwv5hIrOukNoId5oxlsmsxcinTRbwqgg1hnvOQsNXJ3gkF4aHGQpPcY2QAb0iwDamrmGdt43xT3H0pepY7Uui5JHKktyh_CG9MLT4l9eQbiVU13C5Tamnkg70MTwYi6lskufbmm8Tirb5YFMJ2o3Z2CT5yBJiar4BxYgIn35WjIhAWtISYMlwvyMAdDva3glibKf9p0evz3H7mhefS1CL2D-AAaGzuSk6AS03ZA6QGXgl8kzAoSbHen0NwSeT7bpA7AJuV6NN749c0uxBeg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_OQERUebGnkiKHPWbrDR6MrPYeDFBtIvzbS0PE8qSjR6xdwr_Kh0t6kcykcmBqtYH6rjkj0rYC87XRJRHpheSDJ0pcJI6zCu-SgtIrPBeKc_joiUihS72JMFDSoesnm1mDgR2nnIANme4HcT3NR8JJOpRSxChBTc6P5uU_Zf6OQQ,,&b64e=1&sign=98033d2633bebe83e4e4110958b245a8&keyno=1',
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
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 3732918,
                name: 'GUESS',
                site: 'http://www.guesswatches.com',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id781654316530114328/orig',
                link: 'https://market.yandex.ru/brands--guess/3732918?pp=482&clid=2210590&distr_type=4',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            outlet: {
                id: '27486755',
                name: 'Салон оптики "Слепая курица" Москва ТРК "Афимолл Сити"',
                type: 'mixed',
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
                        'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                    outlets: [],
                },
                address: {
                    regionId: 213,
                    locality: 'Москва',
                    thoroughfare: 'Пресненская набережная',
                    premiseNumber: '2',
                    fullAddress: 'Москва, Пресненская набережная, д. 2',
                    geoPoint: {
                        coordinates: {
                            latitude: 55.74906,
                            longitude: 37.539347,
                        },
                    },
                },
                phones: [
                    {
                        number: '+7 (495) 120-3427',
                        sanitized: '74951203427',
                    },
                ],
                schedule: [
                    {
                        daysFrom: '7',
                        daysTill: '7',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '1',
                        daysTill: '1',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '2',
                        daysTill: '2',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '3',
                        daysTill: '3',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '4',
                        daysTill: '4',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '5',
                        daysTill: '5',
                        from: '10:00',
                        till: '23:00',
                    },
                    {
                        daysFrom: '6',
                        daysTill: '6',
                        from: '10:00',
                        till: '23:00',
                    },
                ],
            },
            link:
                'https://market.yandex.ru/offer/Gka30W292kXjFe6pV8FnMg?model_id=494192821&hid=7812201&pp=482&clid=2210590&distr_type=4&cpc=OKGXw_tnfhyF1GB5pB03E7qHo6o9-J-y6oDh8QNhHP4IFi5jA9CfeWjcJMhf3sL9pqrpUitxY3pU5BQEiDlsX_IvPnv-EuPHDvxr7bWuvszwBnKzuX4Xl3ypyTPCGeIntsUe4omMtr1gxBGDWrN-_f6N0kIzqpHr&lr=213',
            variationCount: 5,
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'CpaBuy',
                        name: 'MNA_DefaultOffer_2680',
                        value: '0.0902174',
                    },
                    {
                        tag: 'CpcClick',
                        name: 'MNA_HybridAuctionCpcCtr2430',
                        value: '0.299896',
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
            previewPhotos: [
                {
                    width: 158,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1577758/market_szPrToKUpEKDwHrxT-DFAA/120x160',
                },
            ],
            activeFilters: [
                {
                    id: '14871214',
                    name: 'Цвет товара',
                    type: 'PHOTO_PICKER',
                    subType: 'IMAGE_PICKER',
                    values: [
                        {
                            id: '14899090',
                            name: 'черный',
                            color: '#000000',
                        },
                    ],
                },
            ],
        },
        {
            id: 'yDpJekrrgZGNpLQTfBjKFJuqn_nWIzNZ0EDlvyhTZ037_HG4SYQeDw',
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
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            urls: {
                482: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            },
            directUrl:
                'https://slepayakurica.ru/sumki/sumka-zhenskaya-guess-hw-vg74-39060-black/?utm_source=market&utm_medium=cpc&utm_content=7885&deduplication_channel=market',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZnCMIP_BgY_pMYKSkOCUpFV2Vdz10QvW1-I5T_d21cXSSMp68RMdWuJv_cGK4qhRYHSOVxlUV7dyn1DvOeVzAxrlx5F136EeGglInY6zuYFNFyBbfjxYWPzukZCRxMB9j3UF9RV2vdhw-WlnkVDveN5OhrkKe4J4XdHkK2HouIxEOmYrk1SuWQ2ODeitzDRESSXgMIt7t2g1CcTchR3uuMgL23A1D6XJnblEMynvTWshcJcHIS-OlC5RgkNVTmraQwylskrVtT-NHhyjBY6n_3dqrlnd6K04gH7as2ZbT7R0RlYhkJlDuWr4NPM-g7B7h9iUrGpPd_Yng2RHszcMPxYMEAq2yGJJTlswv4Qi_qegzhke2KO42WIfQiT4KE0I0enyJdwwT4dsefvS1fr_NcKBewnYkBVsJ7mhlgJjD-ag7Q1yQQIRUxZ_jS_yMG-ky1r-46YIFN-I5kuU-hgGCUu7n7EFdAnwVspVP_DseHQR03pOQJ5zgjrGH3ueeI5aoj-i9JI9sQSfXqj-9_PiCeBFdwRvN_dDKBdP5E_l7KFjUDtGmv41D_9KMV6pSwJ1KxfkTdnenWK4dCgBUvKM-JR0zWFWLBAam11j_RRxVlgKY47SgZ46HDuxx2Z4z8UcrUCH_kjvn0D-1H4hWMbFq1rdbykxYOyRCSXrCGTR9csy1s8tQKzUrTi8xyLHSPgneHzCnjW8NOCHAhbtKoR2N9rqXzfJiLOXsFszg-0opILghki2U0jej4g,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_Mh10p3rkVEwce0QVWd9XunfvxEX_MDr10MhMvH3u0JecsH5Qw0O-JDZ0na_j2fFuSWenDqZv8XjwsAEAU5BJ9E,&b64e=1&sign=df71c3f91df5b34446c202e2e71a8877&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZSihUHLaU1GXvp6LSIuPeI5_dVW9YLeXntIKqZ9HlH1Cu589kewjMYpsBJIe1XT0FEnhFF-cbqPYDBa0o6H5Y0HxvuegAWwZoMsp03qqBnH5rz-7bjTzpZSva89FqVbBL5lJOBBbPVEY_wbegbtA6aFZnyd88frSvwX2PisvBbBvabHmHAkqZJhfO12vYZHPy0xo4iqumVqxPTSEz-qtnfeSL9L2axi--IoWC0gmcSO6arpdQBl54yzrZqCaydg3yFsd9hjfYdC9uqsgj8QbcIeopXAD71E-se_h9z3SbOKcUflDJnjRXHzguQi-K0dCUQH4c2xTAXql1aJVWSTfdomWn8mN7wzJG1UniBiA1LLQVMGzHB_yGcYjkrYpxOvSBYPt8F8JEz8j9CcLHMJeiqna84il-hwnrO5-_hgRBIErVAfs8mbL7GB4ydwtUzXMoLlAbKWBoR1UCC7VHwjGJk28KNA_evYYYAEmjL7IPmkecDd-bFJPfV2qU5Yl-YR5fZSgauEajTyv19rMAMsxGEzz5lTLkJj1pi5xiClT6uR0y4AgP7rfJz-iTOvmh1ADwEW_eOVg6IiFccN8Uxu-fK9doYFUTXcVOcVAH16JSArJbnFBWkkz7m5tXLXQX9mNAm38IvXe0vl96R0mgVvpXTMzCbTjJ7fje6fPpYsafwEYpds4Whl-Oai8MR0llV0uYRa5gPS0EGylxyb5l_5r7enmT6td6T7INXMc6lDAmJZoADaIOHnH-1A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_CzfxhuRiCDkt2MBTYFlJ8Y51YReytA8GUtAxGYyUGB-q8MapEQpj6Dp1zApgZhN6GulTqW6W-fi5Mdb4AsujqY,&b64e=1&sign=b618bbc8981b0a086e1f9a99bdb86603&keyno=1',
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
                    'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 494192821,
            },
            outletCount: 2,
            pickupCount: 0,
            localStoreCount: 2,
            phone: {
                number: '8 800 500-53-29',
                sanitized: '88005005329',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbnNgSTWltlSV-vcM82s2rlzi5zT6__WwG2rv0CfFbD67EeEawlsRbx5697ettYI1Y740IegvR5DmLPAQsm5vQPEEFjBu8u_TFV1lezHRrXTIk4tEO-mpXv6hF74-bVg-HoEhH5Sw4DGnQBuDfm_Spcnt0yDa-Wsxk3RaibYCSkfZxmYBn-bQ_w90_KDbHnxzPRufE5br16kifQuEKEM0t-HAkxceD0bl7vsNGEf0uo7HG4pClAc6qJd9EhbyX6TfeEPWuCvUEnUSj7QHdm_4iyxit71uSEfQ7L0TTxP3PjJOwisgwNxCaWT6iGwxkRuNFXjHjNbBGo-EQNOuFG9Hfpw0tFhRMSBGWQLn91-SSesfCw8f_rM7q5noB1l0mC8ZGZp6DwZ8kbITALdVX6QVtogJaKrcjNLjJi0TUnwAYnic4_f4DYHlHjS6zMKz6PxyQaJCsiShbLF0Wq_1aBsf3rNcp2hwv5hIrOukNoId5oxlsmsxcinTRbwqgg1hnvOQsNXJ3gkF4aHGQpPcY2QAb0iwDamrmGdt43xT3H0pepY7Uui5JHKktyh_CG9MLT4l9eQbiVU13C5Tamnkg70MTwYi6lskufbmm8Tirb5YFMJ2o3Z2CT5yBJiar4BxYgIn35WjIhAWtISYMlwvyMAdDva3glibKf9p0evz3H7mhefS1CL2D-AAaGzuSk6AS03ZA6QGXgl8kzAoSbHen0NwSeT7bpA7AJuV6NN749c0uxBeg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_OQERUebGnkiKHPWbrDR6MrPYeDFBtIvzbS0PE8qSjR6xdwr_Kh0t6kcykcmBqtYH6rjkj0rYC87XRJRHpheSDJ0pcJI6zCu-SgtIrPBeKc_joiUihS72JMFDSoesnm1mDgR2nnIANme4HcT3NR8JJOpRSxChBTc6P5uU_Zf6OQQ,,&b64e=1&sign=98033d2633bebe83e4e4110958b245a8&keyno=1',
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
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 3732918,
                name: 'GUESS',
                site: 'http://www.guesswatches.com',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id781654316530114328/orig',
                link: 'https://market.yandex.ru/brands--guess/3732918?pp=482&clid=2210590&distr_type=4',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            outlet: {
                id: '27486755',
                name: 'Салон оптики "Слепая курица" Москва ТРК "Афимолл Сити"',
                type: 'mixed',
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
                        'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                    outlets: [],
                },
                address: {
                    regionId: 213,
                    locality: 'Москва',
                    thoroughfare: 'Пресненская набережная',
                    premiseNumber: '2',
                    fullAddress: 'Москва, Пресненская набережная, д. 2',
                    geoPoint: {
                        coordinates: {
                            latitude: 55.74906,
                            longitude: 37.539347,
                        },
                    },
                },
                phones: [
                    {
                        number: '+7 (495) 120-3427',
                        sanitized: '74951203427',
                    },
                ],
                schedule: [
                    {
                        daysFrom: '7',
                        daysTill: '7',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '1',
                        daysTill: '1',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '2',
                        daysTill: '2',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '3',
                        daysTill: '3',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '4',
                        daysTill: '4',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '5',
                        daysTill: '5',
                        from: '10:00',
                        till: '23:00',
                    },
                    {
                        daysFrom: '6',
                        daysTill: '6',
                        from: '10:00',
                        till: '23:00',
                    },
                ],
            },
            link:
                'https://market.yandex.ru/offer/Gka30W292kXjFe6pV8FnMg?model_id=494192821&hid=7812201&pp=482&clid=2210590&distr_type=4&cpc=OKGXw_tnfhyF1GB5pB03E7qHo6o9-J-y6oDh8QNhHP4IFi5jA9CfeWjcJMhf3sL9pqrpUitxY3pU5BQEiDlsX_IvPnv-EuPHDvxr7bWuvszwBnKzuX4Xl3ypyTPCGeIntsUe4omMtr1gxBGDWrN-_f6N0kIzqpHr&lr=213',
            variationCount: 5,
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'CpaBuy',
                        name: 'MNA_DefaultOffer_2680',
                        value: '0.0902174',
                    },
                    {
                        tag: 'CpcClick',
                        name: 'MNA_HybridAuctionCpcCtr2430',
                        value: '0.299896',
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
            previewPhotos: [
                {
                    width: 158,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1577758/market_szPrToKUpEKDwHrxT-DFAA/120x160',
                },
            ],
            activeFilters: [
                {
                    id: '14871214',
                    name: 'Цвет товара',
                    type: 'PHOTO_PICKER',
                    subType: 'IMAGE_PICKER',
                    values: [
                        {
                            id: '14899090',
                            name: 'черный',
                            color: '#000000',
                        },
                    ],
                },
            ],
        },
        {
            id: 'yDpJekrrgZGNpLQTfBjKFJuqn_nWIzNZ0EDlvihTZ037_HG4SpQeDw',
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
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            urls: {
                482: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            },
            directUrl:
                'https://slepayakurica.ru/sumki/sumka-zhenskaya-guess-hw-vg74-39060-black/?utm_source=market&utm_medium=cpc&utm_content=7885&deduplication_channel=market',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZnCMIP_BgY_pMYKSkOCUpFV2Vdz10QvW1-I5T_d21cXSSMp68RMdWuJv_cGK4qhRYHSOVxlUV7dyn1DvOeVzAxrlx5F136EeGglInY6zuYFNFyBbfjxYWPzukZCRxMB9j3UF9RV2vdhw-WlnkVDveN5OhrkKe4J4XdHkK2HouIxEOmYrk1SuWQ2ODeitzDRESSXgMIt7t2g1CcTchR3uuMgL23A1D6XJnblEMynvTWshcJcHIS-OlC5RgkNVTmraQwylskrVtT-NHhyjBY6n_3dqrlnd6K04gH7as2ZbT7R0RlYhkJlDuWr4NPM-g7B7h9iUrGpPd_Yng2RHszcMPxYMEAq2yGJJTlswv4Qi_qegzhke2KO42WIfQiT4KE0I0enyJdwwT4dsefvS1fr_NcKBewnYkBVsJ7mhlgJjD-ag7Q1yQQIRUxZ_jS_yMG-ky1r-46YIFN-I5kuU-hgGCUu7n7EFdAnwVspVP_DseHQR03pOQJ5zgjrGH3ueeI5aoj-i9JI9sQSfXqj-9_PiCeBFdwRvN_dDKBdP5E_l7KFjUDtGmv41D_9KMV6pSwJ1KxfkTdnenWK4dCgBUvKM-JR0zWFWLBAam11j_RRxVlgKY47SgZ46HDuxx2Z4z8UcrUCH_kjvn0D-1H4hWMbFq1rdbykxYOyRCSXrCGTR9csy1s8tQKzUrTi8xyLHSPgneHzCnjW8NOCHAhbtKoR2N9rqXzfJiLOXsFszg-0opILghki2U0jej4g,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_Mh10p3rkVEwce0QVWd9XunfvxEX_MDr10MhMvH3u0JecsH5Qw0O-JDZ0na_j2fFuSWenDqZv8XjwsAEAU5BJ9E,&b64e=1&sign=df71c3f91df5b34446c202e2e71a8877&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZSihUHLaU1GXvp6LSIuPeI5_dVW9YLeXntIKqZ9HlH1Cu589kewjMYpsBJIe1XT0FEnhFF-cbqPYDBa0o6H5Y0HxvuegAWwZoMsp03qqBnH5rz-7bjTzpZSva89FqVbBL5lJOBBbPVEY_wbegbtA6aFZnyd88frSvwX2PisvBbBvabHmHAkqZJhfO12vYZHPy0xo4iqumVqxPTSEz-qtnfeSL9L2axi--IoWC0gmcSO6arpdQBl54yzrZqCaydg3yFsd9hjfYdC9uqsgj8QbcIeopXAD71E-se_h9z3SbOKcUflDJnjRXHzguQi-K0dCUQH4c2xTAXql1aJVWSTfdomWn8mN7wzJG1UniBiA1LLQVMGzHB_yGcYjkrYpxOvSBYPt8F8JEz8j9CcLHMJeiqna84il-hwnrO5-_hgRBIErVAfs8mbL7GB4ydwtUzXMoLlAbKWBoR1UCC7VHwjGJk28KNA_evYYYAEmjL7IPmkecDd-bFJPfV2qU5Yl-YR5fZSgauEajTyv19rMAMsxGEzz5lTLkJj1pi5xiClT6uR0y4AgP7rfJz-iTOvmh1ADwEW_eOVg6IiFccN8Uxu-fK9doYFUTXcVOcVAH16JSArJbnFBWkkz7m5tXLXQX9mNAm38IvXe0vl96R0mgVvpXTMzCbTjJ7fje6fPpYsafwEYpds4Whl-Oai8MR0llV0uYRa5gPS0EGylxyb5l_5r7enmT6td6T7INXMc6lDAmJZoADaIOHnH-1A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_CzfxhuRiCDkt2MBTYFlJ8Y51YReytA8GUtAxGYyUGB-q8MapEQpj6Dp1zApgZhN6GulTqW6W-fi5Mdb4AsujqY,&b64e=1&sign=b618bbc8981b0a086e1f9a99bdb86603&keyno=1',
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
                    'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 494192821,
            },
            outletCount: 2,
            pickupCount: 0,
            localStoreCount: 2,
            phone: {
                number: '8 800 500-53-29',
                sanitized: '88005005329',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbnNgSTWltlSV-vcM82s2rlzi5zT6__WwG2rv0CfFbD67EeEawlsRbx5697ettYI1Y740IegvR5DmLPAQsm5vQPEEFjBu8u_TFV1lezHRrXTIk4tEO-mpXv6hF74-bVg-HoEhH5Sw4DGnQBuDfm_Spcnt0yDa-Wsxk3RaibYCSkfZxmYBn-bQ_w90_KDbHnxzPRufE5br16kifQuEKEM0t-HAkxceD0bl7vsNGEf0uo7HG4pClAc6qJd9EhbyX6TfeEPWuCvUEnUSj7QHdm_4iyxit71uSEfQ7L0TTxP3PjJOwisgwNxCaWT6iGwxkRuNFXjHjNbBGo-EQNOuFG9Hfpw0tFhRMSBGWQLn91-SSesfCw8f_rM7q5noB1l0mC8ZGZp6DwZ8kbITALdVX6QVtogJaKrcjNLjJi0TUnwAYnic4_f4DYHlHjS6zMKz6PxyQaJCsiShbLF0Wq_1aBsf3rNcp2hwv5hIrOukNoId5oxlsmsxcinTRbwqgg1hnvOQsNXJ3gkF4aHGQpPcY2QAb0iwDamrmGdt43xT3H0pepY7Uui5JHKktyh_CG9MLT4l9eQbiVU13C5Tamnkg70MTwYi6lskufbmm8Tirb5YFMJ2o3Z2CT5yBJiar4BxYgIn35WjIhAWtISYMlwvyMAdDva3glibKf9p0evz3H7mhefS1CL2D-AAaGzuSk6AS03ZA6QGXgl8kzAoSbHen0NwSeT7bpA7AJuV6NN749c0uxBeg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_OQERUebGnkiKHPWbrDR6MrPYeDFBtIvzbS0PE8qSjR6xdwr_Kh0t6kcykcmBqtYH6rjkj0rYC87XRJRHpheSDJ0pcJI6zCu-SgtIrPBeKc_joiUihS72JMFDSoesnm1mDgR2nnIANme4HcT3NR8JJOpRSxChBTc6P5uU_Zf6OQQ,,&b64e=1&sign=98033d2633bebe83e4e4110958b245a8&keyno=1',
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
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 3732918,
                name: 'GUESS',
                site: 'http://www.guesswatches.com',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id781654316530114328/orig',
                link: 'https://market.yandex.ru/brands--guess/3732918?pp=482&clid=2210590&distr_type=4',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            outlet: {
                id: '27486755',
                name: 'Салон оптики "Слепая курица" Москва ТРК "Афимолл Сити"',
                type: 'mixed',
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
                        'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                    outlets: [],
                },
                address: {
                    regionId: 213,
                    locality: 'Москва',
                    thoroughfare: 'Пресненская набережная',
                    premiseNumber: '2',
                    fullAddress: 'Москва, Пресненская набережная, д. 2',
                    geoPoint: {
                        coordinates: {
                            latitude: 55.74906,
                            longitude: 37.539347,
                        },
                    },
                },
                phones: [
                    {
                        number: '+7 (495) 120-3427',
                        sanitized: '74951203427',
                    },
                ],
                schedule: [
                    {
                        daysFrom: '7',
                        daysTill: '7',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '1',
                        daysTill: '1',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '2',
                        daysTill: '2',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '3',
                        daysTill: '3',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '4',
                        daysTill: '4',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '5',
                        daysTill: '5',
                        from: '10:00',
                        till: '23:00',
                    },
                    {
                        daysFrom: '6',
                        daysTill: '6',
                        from: '10:00',
                        till: '23:00',
                    },
                ],
            },
            link:
                'https://market.yandex.ru/offer/Gka30W292kXjFe6pV8FnMg?model_id=494192821&hid=7812201&pp=482&clid=2210590&distr_type=4&cpc=OKGXw_tnfhyF1GB5pB03E7qHo6o9-J-y6oDh8QNhHP4IFi5jA9CfeWjcJMhf3sL9pqrpUitxY3pU5BQEiDlsX_IvPnv-EuPHDvxr7bWuvszwBnKzuX4Xl3ypyTPCGeIntsUe4omMtr1gxBGDWrN-_f6N0kIzqpHr&lr=213',
            variationCount: 5,
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'CpaBuy',
                        name: 'MNA_DefaultOffer_2680',
                        value: '0.0902174',
                    },
                    {
                        tag: 'CpcClick',
                        name: 'MNA_HybridAuctionCpcCtr2430',
                        value: '0.299896',
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
            previewPhotos: [
                {
                    width: 158,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1577758/market_szPrToKUpEKDwHrxT-DFAA/120x160',
                },
            ],
            activeFilters: [
                {
                    id: '14871214',
                    name: 'Цвет товара',
                    type: 'PHOTO_PICKER',
                    subType: 'IMAGE_PICKER',
                    values: [
                        {
                            id: '14899090',
                            name: 'черный',
                            color: '#000000',
                        },
                    ],
                },
            ],
        },
        {
            id: 'yDpJekrrgZGNpLQTfBjKFJuqn_nWIzNZ0EDlvihTZ037_HG4SYueDw',
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
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            urls: {
                482: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            },
            directUrl:
                'https://slepayakurica.ru/sumki/sumka-zhenskaya-guess-hw-vg74-39060-black/?utm_source=market&utm_medium=cpc&utm_content=7885&deduplication_channel=market',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZnCMIP_BgY_pMYKSkOCUpFV2Vdz10QvW1-I5T_d21cXSSMp68RMdWuJv_cGK4qhRYHSOVxlUV7dyn1DvOeVzAxrlx5F136EeGglInY6zuYFNFyBbfjxYWPzukZCRxMB9j3UF9RV2vdhw-WlnkVDveN5OhrkKe4J4XdHkK2HouIxEOmYrk1SuWQ2ODeitzDRESSXgMIt7t2g1CcTchR3uuMgL23A1D6XJnblEMynvTWshcJcHIS-OlC5RgkNVTmraQwylskrVtT-NHhyjBY6n_3dqrlnd6K04gH7as2ZbT7R0RlYhkJlDuWr4NPM-g7B7h9iUrGpPd_Yng2RHszcMPxYMEAq2yGJJTlswv4Qi_qegzhke2KO42WIfQiT4KE0I0enyJdwwT4dsefvS1fr_NcKBewnYkBVsJ7mhlgJjD-ag7Q1yQQIRUxZ_jS_yMG-ky1r-46YIFN-I5kuU-hgGCUu7n7EFdAnwVspVP_DseHQR03pOQJ5zgjrGH3ueeI5aoj-i9JI9sQSfXqj-9_PiCeBFdwRvN_dDKBdP5E_l7KFjUDtGmv41D_9KMV6pSwJ1KxfkTdnenWK4dCgBUvKM-JR0zWFWLBAam11j_RRxVlgKY47SgZ46HDuxx2Z4z8UcrUCH_kjvn0D-1H4hWMbFq1rdbykxYOyRCSXrCGTR9csy1s8tQKzUrTi8xyLHSPgneHzCnjW8NOCHAhbtKoR2N9rqXzfJiLOXsFszg-0opILghki2U0jej4g,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_Mh10p3rkVEwce0QVWd9XunfvxEX_MDr10MhMvH3u0JecsH5Qw0O-JDZ0na_j2fFuSWenDqZv8XjwsAEAU5BJ9E,&b64e=1&sign=df71c3f91df5b34446c202e2e71a8877&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZSihUHLaU1GXvp6LSIuPeI5_dVW9YLeXntIKqZ9HlH1Cu589kewjMYpsBJIe1XT0FEnhFF-cbqPYDBa0o6H5Y0HxvuegAWwZoMsp03qqBnH5rz-7bjTzpZSva89FqVbBL5lJOBBbPVEY_wbegbtA6aFZnyd88frSvwX2PisvBbBvabHmHAkqZJhfO12vYZHPy0xo4iqumVqxPTSEz-qtnfeSL9L2axi--IoWC0gmcSO6arpdQBl54yzrZqCaydg3yFsd9hjfYdC9uqsgj8QbcIeopXAD71E-se_h9z3SbOKcUflDJnjRXHzguQi-K0dCUQH4c2xTAXql1aJVWSTfdomWn8mN7wzJG1UniBiA1LLQVMGzHB_yGcYjkrYpxOvSBYPt8F8JEz8j9CcLHMJeiqna84il-hwnrO5-_hgRBIErVAfs8mbL7GB4ydwtUzXMoLlAbKWBoR1UCC7VHwjGJk28KNA_evYYYAEmjL7IPmkecDd-bFJPfV2qU5Yl-YR5fZSgauEajTyv19rMAMsxGEzz5lTLkJj1pi5xiClT6uR0y4AgP7rfJz-iTOvmh1ADwEW_eOVg6IiFccN8Uxu-fK9doYFUTXcVOcVAH16JSArJbnFBWkkz7m5tXLXQX9mNAm38IvXe0vl96R0mgVvpXTMzCbTjJ7fje6fPpYsafwEYpds4Whl-Oai8MR0llV0uYRa5gPS0EGylxyb5l_5r7enmT6td6T7INXMc6lDAmJZoADaIOHnH-1A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_CzfxhuRiCDkt2MBTYFlJ8Y51YReytA8GUtAxGYyUGB-q8MapEQpj6Dp1zApgZhN6GulTqW6W-fi5Mdb4AsujqY,&b64e=1&sign=b618bbc8981b0a086e1f9a99bdb86603&keyno=1',
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
                    'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 494192821,
            },
            outletCount: 2,
            pickupCount: 0,
            localStoreCount: 2,
            phone: {
                number: '8 800 500-53-29',
                sanitized: '88005005329',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbnNgSTWltlSV-vcM82s2rlzi5zT6__WwG2rv0CfFbD67EeEawlsRbx5697ettYI1Y740IegvR5DmLPAQsm5vQPEEFjBu8u_TFV1lezHRrXTIk4tEO-mpXv6hF74-bVg-HoEhH5Sw4DGnQBuDfm_Spcnt0yDa-Wsxk3RaibYCSkfZxmYBn-bQ_w90_KDbHnxzPRufE5br16kifQuEKEM0t-HAkxceD0bl7vsNGEf0uo7HG4pClAc6qJd9EhbyX6TfeEPWuCvUEnUSj7QHdm_4iyxit71uSEfQ7L0TTxP3PjJOwisgwNxCaWT6iGwxkRuNFXjHjNbBGo-EQNOuFG9Hfpw0tFhRMSBGWQLn91-SSesfCw8f_rM7q5noB1l0mC8ZGZp6DwZ8kbITALdVX6QVtogJaKrcjNLjJi0TUnwAYnic4_f4DYHlHjS6zMKz6PxyQaJCsiShbLF0Wq_1aBsf3rNcp2hwv5hIrOukNoId5oxlsmsxcinTRbwqgg1hnvOQsNXJ3gkF4aHGQpPcY2QAb0iwDamrmGdt43xT3H0pepY7Uui5JHKktyh_CG9MLT4l9eQbiVU13C5Tamnkg70MTwYi6lskufbmm8Tirb5YFMJ2o3Z2CT5yBJiar4BxYgIn35WjIhAWtISYMlwvyMAdDva3glibKf9p0evz3H7mhefS1CL2D-AAaGzuSk6AS03ZA6QGXgl8kzAoSbHen0NwSeT7bpA7AJuV6NN749c0uxBeg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_OQERUebGnkiKHPWbrDR6MrPYeDFBtIvzbS0PE8qSjR6xdwr_Kh0t6kcykcmBqtYH6rjkj0rYC87XRJRHpheSDJ0pcJI6zCu-SgtIrPBeKc_joiUihS72JMFDSoesnm1mDgR2nnIANme4HcT3NR8JJOpRSxChBTc6P5uU_Zf6OQQ,,&b64e=1&sign=98033d2633bebe83e4e4110958b245a8&keyno=1',
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
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 3732918,
                name: 'GUESS',
                site: 'http://www.guesswatches.com',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id781654316530114328/orig',
                link: 'https://market.yandex.ru/brands--guess/3732918?pp=482&clid=2210590&distr_type=4',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            outlet: {
                id: '27486755',
                name: 'Салон оптики "Слепая курица" Москва ТРК "Афимолл Сити"',
                type: 'mixed',
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
                        'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                    outlets: [],
                },
                address: {
                    regionId: 213,
                    locality: 'Москва',
                    thoroughfare: 'Пресненская набережная',
                    premiseNumber: '2',
                    fullAddress: 'Москва, Пресненская набережная, д. 2',
                    geoPoint: {
                        coordinates: {
                            latitude: 55.74906,
                            longitude: 37.539347,
                        },
                    },
                },
                phones: [
                    {
                        number: '+7 (495) 120-3427',
                        sanitized: '74951203427',
                    },
                ],
                schedule: [
                    {
                        daysFrom: '7',
                        daysTill: '7',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '1',
                        daysTill: '1',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '2',
                        daysTill: '2',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '3',
                        daysTill: '3',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '4',
                        daysTill: '4',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '5',
                        daysTill: '5',
                        from: '10:00',
                        till: '23:00',
                    },
                    {
                        daysFrom: '6',
                        daysTill: '6',
                        from: '10:00',
                        till: '23:00',
                    },
                ],
            },
            link:
                'https://market.yandex.ru/offer/Gka30W292kXjFe6pV8FnMg?model_id=494192821&hid=7812201&pp=482&clid=2210590&distr_type=4&cpc=OKGXw_tnfhyF1GB5pB03E7qHo6o9-J-y6oDh8QNhHP4IFi5jA9CfeWjcJMhf3sL9pqrpUitxY3pU5BQEiDlsX_IvPnv-EuPHDvxr7bWuvszwBnKzuX4Xl3ypyTPCGeIntsUe4omMtr1gxBGDWrN-_f6N0kIzqpHr&lr=213',
            variationCount: 5,
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'CpaBuy',
                        name: 'MNA_DefaultOffer_2680',
                        value: '0.0902174',
                    },
                    {
                        tag: 'CpcClick',
                        name: 'MNA_HybridAuctionCpcCtr2430',
                        value: '0.299896',
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
            previewPhotos: [
                {
                    width: 158,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1577758/market_szPrToKUpEKDwHrxT-DFAA/120x160',
                },
            ],
            activeFilters: [
                {
                    id: '14871214',
                    name: 'Цвет товара',
                    type: 'PHOTO_PICKER',
                    subType: 'IMAGE_PICKER',
                    values: [
                        {
                            id: '14899090',
                            name: 'черный',
                            color: '#000000',
                        },
                    ],
                },
            ],
        },
        {
            id: 'yDpJekrrgZGNpLQTfBjKFJuqn_nWIzNZ0EDlvihTZ037_HG4SYQeDe',
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
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            urls: {
                482: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            },
            directUrl:
                'https://slepayakurica.ru/sumki/sumka-zhenskaya-guess-hw-vg74-39060-black/?utm_source=market&utm_medium=cpc&utm_content=7885&deduplication_channel=market',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZnCMIP_BgY_pMYKSkOCUpFV2Vdz10QvW1-I5T_d21cXSSMp68RMdWuJv_cGK4qhRYHSOVxlUV7dyn1DvOeVzAxrlx5F136EeGglInY6zuYFNFyBbfjxYWPzukZCRxMB9j3UF9RV2vdhw-WlnkVDveN5OhrkKe4J4XdHkK2HouIxEOmYrk1SuWQ2ODeitzDRESSXgMIt7t2g1CcTchR3uuMgL23A1D6XJnblEMynvTWshcJcHIS-OlC5RgkNVTmraQwylskrVtT-NHhyjBY6n_3dqrlnd6K04gH7as2ZbT7R0RlYhkJlDuWr4NPM-g7B7h9iUrGpPd_Yng2RHszcMPxYMEAq2yGJJTlswv4Qi_qegzhke2KO42WIfQiT4KE0I0enyJdwwT4dsefvS1fr_NcKBewnYkBVsJ7mhlgJjD-ag7Q1yQQIRUxZ_jS_yMG-ky1r-46YIFN-I5kuU-hgGCUu7n7EFdAnwVspVP_DseHQR03pOQJ5zgjrGH3ueeI5aoj-i9JI9sQSfXqj-9_PiCeBFdwRvN_dDKBdP5E_l7KFjUDtGmv41D_9KMV6pSwJ1KxfkTdnenWK4dCgBUvKM-JR0zWFWLBAam11j_RRxVlgKY47SgZ46HDuxx2Z4z8UcrUCH_kjvn0D-1H4hWMbFq1rdbykxYOyRCSXrCGTR9csy1s8tQKzUrTi8xyLHSPgneHzCnjW8NOCHAhbtKoR2N9rqXzfJiLOXsFszg-0opILghki2U0jej4g,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_Mh10p3rkVEwce0QVWd9XunfvxEX_MDr10MhMvH3u0JecsH5Qw0O-JDZ0na_j2fFuSWenDqZv8XjwsAEAU5BJ9E,&b64e=1&sign=df71c3f91df5b34446c202e2e71a8877&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZSihUHLaU1GXvp6LSIuPeI5_dVW9YLeXntIKqZ9HlH1Cu589kewjMYpsBJIe1XT0FEnhFF-cbqPYDBa0o6H5Y0HxvuegAWwZoMsp03qqBnH5rz-7bjTzpZSva89FqVbBL5lJOBBbPVEY_wbegbtA6aFZnyd88frSvwX2PisvBbBvabHmHAkqZJhfO12vYZHPy0xo4iqumVqxPTSEz-qtnfeSL9L2axi--IoWC0gmcSO6arpdQBl54yzrZqCaydg3yFsd9hjfYdC9uqsgj8QbcIeopXAD71E-se_h9z3SbOKcUflDJnjRXHzguQi-K0dCUQH4c2xTAXql1aJVWSTfdomWn8mN7wzJG1UniBiA1LLQVMGzHB_yGcYjkrYpxOvSBYPt8F8JEz8j9CcLHMJeiqna84il-hwnrO5-_hgRBIErVAfs8mbL7GB4ydwtUzXMoLlAbKWBoR1UCC7VHwjGJk28KNA_evYYYAEmjL7IPmkecDd-bFJPfV2qU5Yl-YR5fZSgauEajTyv19rMAMsxGEzz5lTLkJj1pi5xiClT6uR0y4AgP7rfJz-iTOvmh1ADwEW_eOVg6IiFccN8Uxu-fK9doYFUTXcVOcVAH16JSArJbnFBWkkz7m5tXLXQX9mNAm38IvXe0vl96R0mgVvpXTMzCbTjJ7fje6fPpYsafwEYpds4Whl-Oai8MR0llV0uYRa5gPS0EGylxyb5l_5r7enmT6td6T7INXMc6lDAmJZoADaIOHnH-1A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_CzfxhuRiCDkt2MBTYFlJ8Y51YReytA8GUtAxGYyUGB-q8MapEQpj6Dp1zApgZhN6GulTqW6W-fi5Mdb4AsujqY,&b64e=1&sign=b618bbc8981b0a086e1f9a99bdb86603&keyno=1',
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
                    'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 494192821,
            },
            outletCount: 2,
            pickupCount: 0,
            localStoreCount: 2,
            phone: {
                number: '8 800 500-53-29',
                sanitized: '88005005329',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbnNgSTWltlSV-vcM82s2rlzi5zT6__WwG2rv0CfFbD67EeEawlsRbx5697ettYI1Y740IegvR5DmLPAQsm5vQPEEFjBu8u_TFV1lezHRrXTIk4tEO-mpXv6hF74-bVg-HoEhH5Sw4DGnQBuDfm_Spcnt0yDa-Wsxk3RaibYCSkfZxmYBn-bQ_w90_KDbHnxzPRufE5br16kifQuEKEM0t-HAkxceD0bl7vsNGEf0uo7HG4pClAc6qJd9EhbyX6TfeEPWuCvUEnUSj7QHdm_4iyxit71uSEfQ7L0TTxP3PjJOwisgwNxCaWT6iGwxkRuNFXjHjNbBGo-EQNOuFG9Hfpw0tFhRMSBGWQLn91-SSesfCw8f_rM7q5noB1l0mC8ZGZp6DwZ8kbITALdVX6QVtogJaKrcjNLjJi0TUnwAYnic4_f4DYHlHjS6zMKz6PxyQaJCsiShbLF0Wq_1aBsf3rNcp2hwv5hIrOukNoId5oxlsmsxcinTRbwqgg1hnvOQsNXJ3gkF4aHGQpPcY2QAb0iwDamrmGdt43xT3H0pepY7Uui5JHKktyh_CG9MLT4l9eQbiVU13C5Tamnkg70MTwYi6lskufbmm8Tirb5YFMJ2o3Z2CT5yBJiar4BxYgIn35WjIhAWtISYMlwvyMAdDva3glibKf9p0evz3H7mhefS1CL2D-AAaGzuSk6AS03ZA6QGXgl8kzAoSbHen0NwSeT7bpA7AJuV6NN749c0uxBeg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_OQERUebGnkiKHPWbrDR6MrPYeDFBtIvzbS0PE8qSjR6xdwr_Kh0t6kcykcmBqtYH6rjkj0rYC87XRJRHpheSDJ0pcJI6zCu-SgtIrPBeKc_joiUihS72JMFDSoesnm1mDgR2nnIANme4HcT3NR8JJOpRSxChBTc6P5uU_Zf6OQQ,,&b64e=1&sign=98033d2633bebe83e4e4110958b245a8&keyno=1',
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
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 3732918,
                name: 'GUESS',
                site: 'http://www.guesswatches.com',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id781654316530114328/orig',
                link: 'https://market.yandex.ru/brands--guess/3732918?pp=482&clid=2210590&distr_type=4',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            outlet: {
                id: '27486755',
                name: 'Салон оптики "Слепая курица" Москва ТРК "Афимолл Сити"',
                type: 'mixed',
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
                        'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                    outlets: [],
                },
                address: {
                    regionId: 213,
                    locality: 'Москва',
                    thoroughfare: 'Пресненская набережная',
                    premiseNumber: '2',
                    fullAddress: 'Москва, Пресненская набережная, д. 2',
                    geoPoint: {
                        coordinates: {
                            latitude: 55.74906,
                            longitude: 37.539347,
                        },
                    },
                },
                phones: [
                    {
                        number: '+7 (495) 120-3427',
                        sanitized: '74951203427',
                    },
                ],
                schedule: [
                    {
                        daysFrom: '7',
                        daysTill: '7',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '1',
                        daysTill: '1',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '2',
                        daysTill: '2',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '3',
                        daysTill: '3',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '4',
                        daysTill: '4',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '5',
                        daysTill: '5',
                        from: '10:00',
                        till: '23:00',
                    },
                    {
                        daysFrom: '6',
                        daysTill: '6',
                        from: '10:00',
                        till: '23:00',
                    },
                ],
            },
            link:
                'https://market.yandex.ru/offer/Gka30W292kXjFe6pV8FnMg?model_id=494192821&hid=7812201&pp=482&clid=2210590&distr_type=4&cpc=OKGXw_tnfhyF1GB5pB03E7qHo6o9-J-y6oDh8QNhHP4IFi5jA9CfeWjcJMhf3sL9pqrpUitxY3pU5BQEiDlsX_IvPnv-EuPHDvxr7bWuvszwBnKzuX4Xl3ypyTPCGeIntsUe4omMtr1gxBGDWrN-_f6N0kIzqpHr&lr=213',
            variationCount: 5,
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'CpaBuy',
                        name: 'MNA_DefaultOffer_2680',
                        value: '0.0902174',
                    },
                    {
                        tag: 'CpcClick',
                        name: 'MNA_HybridAuctionCpcCtr2430',
                        value: '0.299896',
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
            previewPhotos: [
                {
                    width: 158,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1577758/market_szPrToKUpEKDwHrxT-DFAA/120x160',
                },
            ],
            activeFilters: [
                {
                    id: '14871214',
                    name: 'Цвет товара',
                    type: 'PHOTO_PICKER',
                    subType: 'IMAGE_PICKER',
                    values: [
                        {
                            id: '14899090',
                            name: 'черный',
                            color: '#000000',
                        },
                    ],
                },
            ],
        },
        {
            id: 'yDpJekrrgZGNpLQTfBjKFJuqn_nWIzNZ0EDlvihTZ037_HG4TYQeDw',
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
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            urls: {
                482: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            },
            directUrl:
                'https://slepayakurica.ru/sumki/sumka-zhenskaya-guess-hw-vg74-39060-black/?utm_source=market&utm_medium=cpc&utm_content=7885&deduplication_channel=market',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZnCMIP_BgY_pMYKSkOCUpFV2Vdz10QvW1-I5T_d21cXSSMp68RMdWuJv_cGK4qhRYHSOVxlUV7dyn1DvOeVzAxrlx5F136EeGglInY6zuYFNFyBbfjxYWPzukZCRxMB9j3UF9RV2vdhw-WlnkVDveN5OhrkKe4J4XdHkK2HouIxEOmYrk1SuWQ2ODeitzDRESSXgMIt7t2g1CcTchR3uuMgL23A1D6XJnblEMynvTWshcJcHIS-OlC5RgkNVTmraQwylskrVtT-NHhyjBY6n_3dqrlnd6K04gH7as2ZbT7R0RlYhkJlDuWr4NPM-g7B7h9iUrGpPd_Yng2RHszcMPxYMEAq2yGJJTlswv4Qi_qegzhke2KO42WIfQiT4KE0I0enyJdwwT4dsefvS1fr_NcKBewnYkBVsJ7mhlgJjD-ag7Q1yQQIRUxZ_jS_yMG-ky1r-46YIFN-I5kuU-hgGCUu7n7EFdAnwVspVP_DseHQR03pOQJ5zgjrGH3ueeI5aoj-i9JI9sQSfXqj-9_PiCeBFdwRvN_dDKBdP5E_l7KFjUDtGmv41D_9KMV6pSwJ1KxfkTdnenWK4dCgBUvKM-JR0zWFWLBAam11j_RRxVlgKY47SgZ46HDuxx2Z4z8UcrUCH_kjvn0D-1H4hWMbFq1rdbykxYOyRCSXrCGTR9csy1s8tQKzUrTi8xyLHSPgneHzCnjW8NOCHAhbtKoR2N9rqXzfJiLOXsFszg-0opILghki2U0jej4g,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_Mh10p3rkVEwce0QVWd9XunfvxEX_MDr10MhMvH3u0JecsH5Qw0O-JDZ0na_j2fFuSWenDqZv8XjwsAEAU5BJ9E,&b64e=1&sign=df71c3f91df5b34446c202e2e71a8877&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZSihUHLaU1GXvp6LSIuPeI5_dVW9YLeXntIKqZ9HlH1Cu589kewjMYpsBJIe1XT0FEnhFF-cbqPYDBa0o6H5Y0HxvuegAWwZoMsp03qqBnH5rz-7bjTzpZSva89FqVbBL5lJOBBbPVEY_wbegbtA6aFZnyd88frSvwX2PisvBbBvabHmHAkqZJhfO12vYZHPy0xo4iqumVqxPTSEz-qtnfeSL9L2axi--IoWC0gmcSO6arpdQBl54yzrZqCaydg3yFsd9hjfYdC9uqsgj8QbcIeopXAD71E-se_h9z3SbOKcUflDJnjRXHzguQi-K0dCUQH4c2xTAXql1aJVWSTfdomWn8mN7wzJG1UniBiA1LLQVMGzHB_yGcYjkrYpxOvSBYPt8F8JEz8j9CcLHMJeiqna84il-hwnrO5-_hgRBIErVAfs8mbL7GB4ydwtUzXMoLlAbKWBoR1UCC7VHwjGJk28KNA_evYYYAEmjL7IPmkecDd-bFJPfV2qU5Yl-YR5fZSgauEajTyv19rMAMsxGEzz5lTLkJj1pi5xiClT6uR0y4AgP7rfJz-iTOvmh1ADwEW_eOVg6IiFccN8Uxu-fK9doYFUTXcVOcVAH16JSArJbnFBWkkz7m5tXLXQX9mNAm38IvXe0vl96R0mgVvpXTMzCbTjJ7fje6fPpYsafwEYpds4Whl-Oai8MR0llV0uYRa5gPS0EGylxyb5l_5r7enmT6td6T7INXMc6lDAmJZoADaIOHnH-1A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_CzfxhuRiCDkt2MBTYFlJ8Y51YReytA8GUtAxGYyUGB-q8MapEQpj6Dp1zApgZhN6GulTqW6W-fi5Mdb4AsujqY,&b64e=1&sign=b618bbc8981b0a086e1f9a99bdb86603&keyno=1',
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
                    'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 494192821,
            },
            outletCount: 2,
            pickupCount: 0,
            localStoreCount: 2,
            phone: {
                number: '8 800 500-53-29',
                sanitized: '88005005329',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbnNgSTWltlSV-vcM82s2rlzi5zT6__WwG2rv0CfFbD67EeEawlsRbx5697ettYI1Y740IegvR5DmLPAQsm5vQPEEFjBu8u_TFV1lezHRrXTIk4tEO-mpXv6hF74-bVg-HoEhH5Sw4DGnQBuDfm_Spcnt0yDa-Wsxk3RaibYCSkfZxmYBn-bQ_w90_KDbHnxzPRufE5br16kifQuEKEM0t-HAkxceD0bl7vsNGEf0uo7HG4pClAc6qJd9EhbyX6TfeEPWuCvUEnUSj7QHdm_4iyxit71uSEfQ7L0TTxP3PjJOwisgwNxCaWT6iGwxkRuNFXjHjNbBGo-EQNOuFG9Hfpw0tFhRMSBGWQLn91-SSesfCw8f_rM7q5noB1l0mC8ZGZp6DwZ8kbITALdVX6QVtogJaKrcjNLjJi0TUnwAYnic4_f4DYHlHjS6zMKz6PxyQaJCsiShbLF0Wq_1aBsf3rNcp2hwv5hIrOukNoId5oxlsmsxcinTRbwqgg1hnvOQsNXJ3gkF4aHGQpPcY2QAb0iwDamrmGdt43xT3H0pepY7Uui5JHKktyh_CG9MLT4l9eQbiVU13C5Tamnkg70MTwYi6lskufbmm8Tirb5YFMJ2o3Z2CT5yBJiar4BxYgIn35WjIhAWtISYMlwvyMAdDva3glibKf9p0evz3H7mhefS1CL2D-AAaGzuSk6AS03ZA6QGXgl8kzAoSbHen0NwSeT7bpA7AJuV6NN749c0uxBeg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_OQERUebGnkiKHPWbrDR6MrPYeDFBtIvzbS0PE8qSjR6xdwr_Kh0t6kcykcmBqtYH6rjkj0rYC87XRJRHpheSDJ0pcJI6zCu-SgtIrPBeKc_joiUihS72JMFDSoesnm1mDgR2nnIANme4HcT3NR8JJOpRSxChBTc6P5uU_Zf6OQQ,,&b64e=1&sign=98033d2633bebe83e4e4110958b245a8&keyno=1',
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
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 3732918,
                name: 'GUESS',
                site: 'http://www.guesswatches.com',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id781654316530114328/orig',
                link: 'https://market.yandex.ru/brands--guess/3732918?pp=482&clid=2210590&distr_type=4',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            outlet: {
                id: '27486755',
                name: 'Салон оптики "Слепая курица" Москва ТРК "Афимолл Сити"',
                type: 'mixed',
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
                        'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                    outlets: [],
                },
                address: {
                    regionId: 213,
                    locality: 'Москва',
                    thoroughfare: 'Пресненская набережная',
                    premiseNumber: '2',
                    fullAddress: 'Москва, Пресненская набережная, д. 2',
                    geoPoint: {
                        coordinates: {
                            latitude: 55.74906,
                            longitude: 37.539347,
                        },
                    },
                },
                phones: [
                    {
                        number: '+7 (495) 120-3427',
                        sanitized: '74951203427',
                    },
                ],
                schedule: [
                    {
                        daysFrom: '7',
                        daysTill: '7',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '1',
                        daysTill: '1',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '2',
                        daysTill: '2',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '3',
                        daysTill: '3',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '4',
                        daysTill: '4',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '5',
                        daysTill: '5',
                        from: '10:00',
                        till: '23:00',
                    },
                    {
                        daysFrom: '6',
                        daysTill: '6',
                        from: '10:00',
                        till: '23:00',
                    },
                ],
            },
            link:
                'https://market.yandex.ru/offer/Gka30W292kXjFe6pV8FnMg?model_id=494192821&hid=7812201&pp=482&clid=2210590&distr_type=4&cpc=OKGXw_tnfhyF1GB5pB03E7qHo6o9-J-y6oDh8QNhHP4IFi5jA9CfeWjcJMhf3sL9pqrpUitxY3pU5BQEiDlsX_IvPnv-EuPHDvxr7bWuvszwBnKzuX4Xl3ypyTPCGeIntsUe4omMtr1gxBGDWrN-_f6N0kIzqpHr&lr=213',
            variationCount: 5,
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'CpaBuy',
                        name: 'MNA_DefaultOffer_2680',
                        value: '0.0902174',
                    },
                    {
                        tag: 'CpcClick',
                        name: 'MNA_HybridAuctionCpcCtr2430',
                        value: '0.299896',
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
            previewPhotos: [
                {
                    width: 158,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1577758/market_szPrToKUpEKDwHrxT-DFAA/120x160',
                },
            ],
            activeFilters: [
                {
                    id: '14871214',
                    name: 'Цвет товара',
                    type: 'PHOTO_PICKER',
                    subType: 'IMAGE_PICKER',
                    values: [
                        {
                            id: '14899090',
                            name: 'черный',
                            color: '#000000',
                        },
                    ],
                },
            ],
        },
        {
            id: 'yDpJekrrgZGNpLQTfBjKFJuqn_nWIzNZ0EDlviYTZ037_HG4SYQeDw',
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
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            urls: {
                482: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            },
            directUrl:
                'https://slepayakurica.ru/sumki/sumka-zhenskaya-guess-hw-vg74-39060-black/?utm_source=market&utm_medium=cpc&utm_content=7885&deduplication_channel=market',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZnCMIP_BgY_pMYKSkOCUpFV2Vdz10QvW1-I5T_d21cXSSMp68RMdWuJv_cGK4qhRYHSOVxlUV7dyn1DvOeVzAxrlx5F136EeGglInY6zuYFNFyBbfjxYWPzukZCRxMB9j3UF9RV2vdhw-WlnkVDveN5OhrkKe4J4XdHkK2HouIxEOmYrk1SuWQ2ODeitzDRESSXgMIt7t2g1CcTchR3uuMgL23A1D6XJnblEMynvTWshcJcHIS-OlC5RgkNVTmraQwylskrVtT-NHhyjBY6n_3dqrlnd6K04gH7as2ZbT7R0RlYhkJlDuWr4NPM-g7B7h9iUrGpPd_Yng2RHszcMPxYMEAq2yGJJTlswv4Qi_qegzhke2KO42WIfQiT4KE0I0enyJdwwT4dsefvS1fr_NcKBewnYkBVsJ7mhlgJjD-ag7Q1yQQIRUxZ_jS_yMG-ky1r-46YIFN-I5kuU-hgGCUu7n7EFdAnwVspVP_DseHQR03pOQJ5zgjrGH3ueeI5aoj-i9JI9sQSfXqj-9_PiCeBFdwRvN_dDKBdP5E_l7KFjUDtGmv41D_9KMV6pSwJ1KxfkTdnenWK4dCgBUvKM-JR0zWFWLBAam11j_RRxVlgKY47SgZ46HDuxx2Z4z8UcrUCH_kjvn0D-1H4hWMbFq1rdbykxYOyRCSXrCGTR9csy1s8tQKzUrTi8xyLHSPgneHzCnjW8NOCHAhbtKoR2N9rqXzfJiLOXsFszg-0opILghki2U0jej4g,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_Mh10p3rkVEwce0QVWd9XunfvxEX_MDr10MhMvH3u0JecsH5Qw0O-JDZ0na_j2fFuSWenDqZv8XjwsAEAU5BJ9E,&b64e=1&sign=df71c3f91df5b34446c202e2e71a8877&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZSihUHLaU1GXvp6LSIuPeI5_dVW9YLeXntIKqZ9HlH1Cu589kewjMYpsBJIe1XT0FEnhFF-cbqPYDBa0o6H5Y0HxvuegAWwZoMsp03qqBnH5rz-7bjTzpZSva89FqVbBL5lJOBBbPVEY_wbegbtA6aFZnyd88frSvwX2PisvBbBvabHmHAkqZJhfO12vYZHPy0xo4iqumVqxPTSEz-qtnfeSL9L2axi--IoWC0gmcSO6arpdQBl54yzrZqCaydg3yFsd9hjfYdC9uqsgj8QbcIeopXAD71E-se_h9z3SbOKcUflDJnjRXHzguQi-K0dCUQH4c2xTAXql1aJVWSTfdomWn8mN7wzJG1UniBiA1LLQVMGzHB_yGcYjkrYpxOvSBYPt8F8JEz8j9CcLHMJeiqna84il-hwnrO5-_hgRBIErVAfs8mbL7GB4ydwtUzXMoLlAbKWBoR1UCC7VHwjGJk28KNA_evYYYAEmjL7IPmkecDd-bFJPfV2qU5Yl-YR5fZSgauEajTyv19rMAMsxGEzz5lTLkJj1pi5xiClT6uR0y4AgP7rfJz-iTOvmh1ADwEW_eOVg6IiFccN8Uxu-fK9doYFUTXcVOcVAH16JSArJbnFBWkkz7m5tXLXQX9mNAm38IvXe0vl96R0mgVvpXTMzCbTjJ7fje6fPpYsafwEYpds4Whl-Oai8MR0llV0uYRa5gPS0EGylxyb5l_5r7enmT6td6T7INXMc6lDAmJZoADaIOHnH-1A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_CzfxhuRiCDkt2MBTYFlJ8Y51YReytA8GUtAxGYyUGB-q8MapEQpj6Dp1zApgZhN6GulTqW6W-fi5Mdb4AsujqY,&b64e=1&sign=b618bbc8981b0a086e1f9a99bdb86603&keyno=1',
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
                    'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 494192821,
            },
            outletCount: 2,
            pickupCount: 0,
            localStoreCount: 2,
            phone: {
                number: '8 800 500-53-29',
                sanitized: '88005005329',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbnNgSTWltlSV-vcM82s2rlzi5zT6__WwG2rv0CfFbD67EeEawlsRbx5697ettYI1Y740IegvR5DmLPAQsm5vQPEEFjBu8u_TFV1lezHRrXTIk4tEO-mpXv6hF74-bVg-HoEhH5Sw4DGnQBuDfm_Spcnt0yDa-Wsxk3RaibYCSkfZxmYBn-bQ_w90_KDbHnxzPRufE5br16kifQuEKEM0t-HAkxceD0bl7vsNGEf0uo7HG4pClAc6qJd9EhbyX6TfeEPWuCvUEnUSj7QHdm_4iyxit71uSEfQ7L0TTxP3PjJOwisgwNxCaWT6iGwxkRuNFXjHjNbBGo-EQNOuFG9Hfpw0tFhRMSBGWQLn91-SSesfCw8f_rM7q5noB1l0mC8ZGZp6DwZ8kbITALdVX6QVtogJaKrcjNLjJi0TUnwAYnic4_f4DYHlHjS6zMKz6PxyQaJCsiShbLF0Wq_1aBsf3rNcp2hwv5hIrOukNoId5oxlsmsxcinTRbwqgg1hnvOQsNXJ3gkF4aHGQpPcY2QAb0iwDamrmGdt43xT3H0pepY7Uui5JHKktyh_CG9MLT4l9eQbiVU13C5Tamnkg70MTwYi6lskufbmm8Tirb5YFMJ2o3Z2CT5yBJiar4BxYgIn35WjIhAWtISYMlwvyMAdDva3glibKf9p0evz3H7mhefS1CL2D-AAaGzuSk6AS03ZA6QGXgl8kzAoSbHen0NwSeT7bpA7AJuV6NN749c0uxBeg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_OQERUebGnkiKHPWbrDR6MrPYeDFBtIvzbS0PE8qSjR6xdwr_Kh0t6kcykcmBqtYH6rjkj0rYC87XRJRHpheSDJ0pcJI6zCu-SgtIrPBeKc_joiUihS72JMFDSoesnm1mDgR2nnIANme4HcT3NR8JJOpRSxChBTc6P5uU_Zf6OQQ,,&b64e=1&sign=98033d2633bebe83e4e4110958b245a8&keyno=1',
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
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 3732918,
                name: 'GUESS',
                site: 'http://www.guesswatches.com',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id781654316530114328/orig',
                link: 'https://market.yandex.ru/brands--guess/3732918?pp=482&clid=2210590&distr_type=4',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            outlet: {
                id: '27486755',
                name: 'Салон оптики "Слепая курица" Москва ТРК "Афимолл Сити"',
                type: 'mixed',
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
                        'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                    outlets: [],
                },
                address: {
                    regionId: 213,
                    locality: 'Москва',
                    thoroughfare: 'Пресненская набережная',
                    premiseNumber: '2',
                    fullAddress: 'Москва, Пресненская набережная, д. 2',
                    geoPoint: {
                        coordinates: {
                            latitude: 55.74906,
                            longitude: 37.539347,
                        },
                    },
                },
                phones: [
                    {
                        number: '+7 (495) 120-3427',
                        sanitized: '74951203427',
                    },
                ],
                schedule: [
                    {
                        daysFrom: '7',
                        daysTill: '7',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '1',
                        daysTill: '1',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '2',
                        daysTill: '2',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '3',
                        daysTill: '3',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '4',
                        daysTill: '4',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '5',
                        daysTill: '5',
                        from: '10:00',
                        till: '23:00',
                    },
                    {
                        daysFrom: '6',
                        daysTill: '6',
                        from: '10:00',
                        till: '23:00',
                    },
                ],
            },
            link:
                'https://market.yandex.ru/offer/Gka30W292kXjFe6pV8FnMg?model_id=494192821&hid=7812201&pp=482&clid=2210590&distr_type=4&cpc=OKGXw_tnfhyF1GB5pB03E7qHo6o9-J-y6oDh8QNhHP4IFi5jA9CfeWjcJMhf3sL9pqrpUitxY3pU5BQEiDlsX_IvPnv-EuPHDvxr7bWuvszwBnKzuX4Xl3ypyTPCGeIntsUe4omMtr1gxBGDWrN-_f6N0kIzqpHr&lr=213',
            variationCount: 5,
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'CpaBuy',
                        name: 'MNA_DefaultOffer_2680',
                        value: '0.0902174',
                    },
                    {
                        tag: 'CpcClick',
                        name: 'MNA_HybridAuctionCpcCtr2430',
                        value: '0.299896',
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
            previewPhotos: [
                {
                    width: 158,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1577758/market_szPrToKUpEKDwHrxT-DFAA/120x160',
                },
            ],
            activeFilters: [
                {
                    id: '14871214',
                    name: 'Цвет товара',
                    type: 'PHOTO_PICKER',
                    subType: 'IMAGE_PICKER',
                    values: [
                        {
                            id: '14899090',
                            name: 'черный',
                            color: '#000000',
                        },
                    ],
                },
            ],
        },
        {
            id: 'yDpJekrrgZGTpLQTfBjKFJuqn_nWIzNZ0EDlvihTZ037_HG4SYQeDw',
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
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            urls: {
                482: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            },
            directUrl:
                'https://slepayakurica.ru/sumki/sumka-zhenskaya-guess-hw-vg74-39060-black/?utm_source=market&utm_medium=cpc&utm_content=7885&deduplication_channel=market',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZnCMIP_BgY_pMYKSkOCUpFV2Vdz10QvW1-I5T_d21cXSSMp68RMdWuJv_cGK4qhRYHSOVxlUV7dyn1DvOeVzAxrlx5F136EeGglInY6zuYFNFyBbfjxYWPzukZCRxMB9j3UF9RV2vdhw-WlnkVDveN5OhrkKe4J4XdHkK2HouIxEOmYrk1SuWQ2ODeitzDRESSXgMIt7t2g1CcTchR3uuMgL23A1D6XJnblEMynvTWshcJcHIS-OlC5RgkNVTmraQwylskrVtT-NHhyjBY6n_3dqrlnd6K04gH7as2ZbT7R0RlYhkJlDuWr4NPM-g7B7h9iUrGpPd_Yng2RHszcMPxYMEAq2yGJJTlswv4Qi_qegzhke2KO42WIfQiT4KE0I0enyJdwwT4dsefvS1fr_NcKBewnYkBVsJ7mhlgJjD-ag7Q1yQQIRUxZ_jS_yMG-ky1r-46YIFN-I5kuU-hgGCUu7n7EFdAnwVspVP_DseHQR03pOQJ5zgjrGH3ueeI5aoj-i9JI9sQSfXqj-9_PiCeBFdwRvN_dDKBdP5E_l7KFjUDtGmv41D_9KMV6pSwJ1KxfkTdnenWK4dCgBUvKM-JR0zWFWLBAam11j_RRxVlgKY47SgZ46HDuxx2Z4z8UcrUCH_kjvn0D-1H4hWMbFq1rdbykxYOyRCSXrCGTR9csy1s8tQKzUrTi8xyLHSPgneHzCnjW8NOCHAhbtKoR2N9rqXzfJiLOXsFszg-0opILghki2U0jej4g,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_Mh10p3rkVEwce0QVWd9XunfvxEX_MDr10MhMvH3u0JecsH5Qw0O-JDZ0na_j2fFuSWenDqZv8XjwsAEAU5BJ9E,&b64e=1&sign=df71c3f91df5b34446c202e2e71a8877&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZSihUHLaU1GXvp6LSIuPeI5_dVW9YLeXntIKqZ9HlH1Cu589kewjMYpsBJIe1XT0FEnhFF-cbqPYDBa0o6H5Y0HxvuegAWwZoMsp03qqBnH5rz-7bjTzpZSva89FqVbBL5lJOBBbPVEY_wbegbtA6aFZnyd88frSvwX2PisvBbBvabHmHAkqZJhfO12vYZHPy0xo4iqumVqxPTSEz-qtnfeSL9L2axi--IoWC0gmcSO6arpdQBl54yzrZqCaydg3yFsd9hjfYdC9uqsgj8QbcIeopXAD71E-se_h9z3SbOKcUflDJnjRXHzguQi-K0dCUQH4c2xTAXql1aJVWSTfdomWn8mN7wzJG1UniBiA1LLQVMGzHB_yGcYjkrYpxOvSBYPt8F8JEz8j9CcLHMJeiqna84il-hwnrO5-_hgRBIErVAfs8mbL7GB4ydwtUzXMoLlAbKWBoR1UCC7VHwjGJk28KNA_evYYYAEmjL7IPmkecDd-bFJPfV2qU5Yl-YR5fZSgauEajTyv19rMAMsxGEzz5lTLkJj1pi5xiClT6uR0y4AgP7rfJz-iTOvmh1ADwEW_eOVg6IiFccN8Uxu-fK9doYFUTXcVOcVAH16JSArJbnFBWkkz7m5tXLXQX9mNAm38IvXe0vl96R0mgVvpXTMzCbTjJ7fje6fPpYsafwEYpds4Whl-Oai8MR0llV0uYRa5gPS0EGylxyb5l_5r7enmT6td6T7INXMc6lDAmJZoADaIOHnH-1A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_CzfxhuRiCDkt2MBTYFlJ8Y51YReytA8GUtAxGYyUGB-q8MapEQpj6Dp1zApgZhN6GulTqW6W-fi5Mdb4AsujqY,&b64e=1&sign=b618bbc8981b0a086e1f9a99bdb86603&keyno=1',
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
                    'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 494192821,
            },
            outletCount: 2,
            pickupCount: 0,
            localStoreCount: 2,
            phone: {
                number: '8 800 500-53-29',
                sanitized: '88005005329',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbnNgSTWltlSV-vcM82s2rlzi5zT6__WwG2rv0CfFbD67EeEawlsRbx5697ettYI1Y740IegvR5DmLPAQsm5vQPEEFjBu8u_TFV1lezHRrXTIk4tEO-mpXv6hF74-bVg-HoEhH5Sw4DGnQBuDfm_Spcnt0yDa-Wsxk3RaibYCSkfZxmYBn-bQ_w90_KDbHnxzPRufE5br16kifQuEKEM0t-HAkxceD0bl7vsNGEf0uo7HG4pClAc6qJd9EhbyX6TfeEPWuCvUEnUSj7QHdm_4iyxit71uSEfQ7L0TTxP3PjJOwisgwNxCaWT6iGwxkRuNFXjHjNbBGo-EQNOuFG9Hfpw0tFhRMSBGWQLn91-SSesfCw8f_rM7q5noB1l0mC8ZGZp6DwZ8kbITALdVX6QVtogJaKrcjNLjJi0TUnwAYnic4_f4DYHlHjS6zMKz6PxyQaJCsiShbLF0Wq_1aBsf3rNcp2hwv5hIrOukNoId5oxlsmsxcinTRbwqgg1hnvOQsNXJ3gkF4aHGQpPcY2QAb0iwDamrmGdt43xT3H0pepY7Uui5JHKktyh_CG9MLT4l9eQbiVU13C5Tamnkg70MTwYi6lskufbmm8Tirb5YFMJ2o3Z2CT5yBJiar4BxYgIn35WjIhAWtISYMlwvyMAdDva3glibKf9p0evz3H7mhefS1CL2D-AAaGzuSk6AS03ZA6QGXgl8kzAoSbHen0NwSeT7bpA7AJuV6NN749c0uxBeg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_OQERUebGnkiKHPWbrDR6MrPYeDFBtIvzbS0PE8qSjR6xdwr_Kh0t6kcykcmBqtYH6rjkj0rYC87XRJRHpheSDJ0pcJI6zCu-SgtIrPBeKc_joiUihS72JMFDSoesnm1mDgR2nnIANme4HcT3NR8JJOpRSxChBTc6P5uU_Zf6OQQ,,&b64e=1&sign=98033d2633bebe83e4e4110958b245a8&keyno=1',
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
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 3732918,
                name: 'GUESS',
                site: 'http://www.guesswatches.com',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id781654316530114328/orig',
                link: 'https://market.yandex.ru/brands--guess/3732918?pp=482&clid=2210590&distr_type=4',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            outlet: {
                id: '27486755',
                name: 'Салон оптики "Слепая курица" Москва ТРК "Афимолл Сити"',
                type: 'mixed',
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
                        'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                    outlets: [],
                },
                address: {
                    regionId: 213,
                    locality: 'Москва',
                    thoroughfare: 'Пресненская набережная',
                    premiseNumber: '2',
                    fullAddress: 'Москва, Пресненская набережная, д. 2',
                    geoPoint: {
                        coordinates: {
                            latitude: 55.74906,
                            longitude: 37.539347,
                        },
                    },
                },
                phones: [
                    {
                        number: '+7 (495) 120-3427',
                        sanitized: '74951203427',
                    },
                ],
                schedule: [
                    {
                        daysFrom: '7',
                        daysTill: '7',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '1',
                        daysTill: '1',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '2',
                        daysTill: '2',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '3',
                        daysTill: '3',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '4',
                        daysTill: '4',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '5',
                        daysTill: '5',
                        from: '10:00',
                        till: '23:00',
                    },
                    {
                        daysFrom: '6',
                        daysTill: '6',
                        from: '10:00',
                        till: '23:00',
                    },
                ],
            },
            link:
                'https://market.yandex.ru/offer/Gka30W292kXjFe6pV8FnMg?model_id=494192821&hid=7812201&pp=482&clid=2210590&distr_type=4&cpc=OKGXw_tnfhyF1GB5pB03E7qHo6o9-J-y6oDh8QNhHP4IFi5jA9CfeWjcJMhf3sL9pqrpUitxY3pU5BQEiDlsX_IvPnv-EuPHDvxr7bWuvszwBnKzuX4Xl3ypyTPCGeIntsUe4omMtr1gxBGDWrN-_f6N0kIzqpHr&lr=213',
            variationCount: 5,
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'CpaBuy',
                        name: 'MNA_DefaultOffer_2680',
                        value: '0.0902174',
                    },
                    {
                        tag: 'CpcClick',
                        name: 'MNA_HybridAuctionCpcCtr2430',
                        value: '0.299896',
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
            previewPhotos: [
                {
                    width: 158,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1577758/market_szPrToKUpEKDwHrxT-DFAA/120x160',
                },
            ],
            activeFilters: [
                {
                    id: '14871214',
                    name: 'Цвет товара',
                    type: 'PHOTO_PICKER',
                    subType: 'IMAGE_PICKER',
                    values: [
                        {
                            id: '14899090',
                            name: 'черный',
                            color: '#000000',
                        },
                    ],
                },
            ],
        },
        {
            id: 'yDpJekrrgZGNpLQTfBjKFTuqn_nWIzNZ0EDlvihTZ037_HG4SYQeDw',
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
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            urls: {
                482: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            },
            directUrl:
                'https://slepayakurica.ru/sumki/sumka-zhenskaya-guess-hw-vg74-39060-black/?utm_source=market&utm_medium=cpc&utm_content=7885&deduplication_channel=market',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZnCMIP_BgY_pMYKSkOCUpFV2Vdz10QvW1-I5T_d21cXSSMp68RMdWuJv_cGK4qhRYHSOVxlUV7dyn1DvOeVzAxrlx5F136EeGglInY6zuYFNFyBbfjxYWPzukZCRxMB9j3UF9RV2vdhw-WlnkVDveN5OhrkKe4J4XdHkK2HouIxEOmYrk1SuWQ2ODeitzDRESSXgMIt7t2g1CcTchR3uuMgL23A1D6XJnblEMynvTWshcJcHIS-OlC5RgkNVTmraQwylskrVtT-NHhyjBY6n_3dqrlnd6K04gH7as2ZbT7R0RlYhkJlDuWr4NPM-g7B7h9iUrGpPd_Yng2RHszcMPxYMEAq2yGJJTlswv4Qi_qegzhke2KO42WIfQiT4KE0I0enyJdwwT4dsefvS1fr_NcKBewnYkBVsJ7mhlgJjD-ag7Q1yQQIRUxZ_jS_yMG-ky1r-46YIFN-I5kuU-hgGCUu7n7EFdAnwVspVP_DseHQR03pOQJ5zgjrGH3ueeI5aoj-i9JI9sQSfXqj-9_PiCeBFdwRvN_dDKBdP5E_l7KFjUDtGmv41D_9KMV6pSwJ1KxfkTdnenWK4dCgBUvKM-JR0zWFWLBAam11j_RRxVlgKY47SgZ46HDuxx2Z4z8UcrUCH_kjvn0D-1H4hWMbFq1rdbykxYOyRCSXrCGTR9csy1s8tQKzUrTi8xyLHSPgneHzCnjW8NOCHAhbtKoR2N9rqXzfJiLOXsFszg-0opILghki2U0jej4g,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_Mh10p3rkVEwce0QVWd9XunfvxEX_MDr10MhMvH3u0JecsH5Qw0O-JDZ0na_j2fFuSWenDqZv8XjwsAEAU5BJ9E,&b64e=1&sign=df71c3f91df5b34446c202e2e71a8877&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZSihUHLaU1GXvp6LSIuPeI5_dVW9YLeXntIKqZ9HlH1Cu589kewjMYpsBJIe1XT0FEnhFF-cbqPYDBa0o6H5Y0HxvuegAWwZoMsp03qqBnH5rz-7bjTzpZSva89FqVbBL5lJOBBbPVEY_wbegbtA6aFZnyd88frSvwX2PisvBbBvabHmHAkqZJhfO12vYZHPy0xo4iqumVqxPTSEz-qtnfeSL9L2axi--IoWC0gmcSO6arpdQBl54yzrZqCaydg3yFsd9hjfYdC9uqsgj8QbcIeopXAD71E-se_h9z3SbOKcUflDJnjRXHzguQi-K0dCUQH4c2xTAXql1aJVWSTfdomWn8mN7wzJG1UniBiA1LLQVMGzHB_yGcYjkrYpxOvSBYPt8F8JEz8j9CcLHMJeiqna84il-hwnrO5-_hgRBIErVAfs8mbL7GB4ydwtUzXMoLlAbKWBoR1UCC7VHwjGJk28KNA_evYYYAEmjL7IPmkecDd-bFJPfV2qU5Yl-YR5fZSgauEajTyv19rMAMsxGEzz5lTLkJj1pi5xiClT6uR0y4AgP7rfJz-iTOvmh1ADwEW_eOVg6IiFccN8Uxu-fK9doYFUTXcVOcVAH16JSArJbnFBWkkz7m5tXLXQX9mNAm38IvXe0vl96R0mgVvpXTMzCbTjJ7fje6fPpYsafwEYpds4Whl-Oai8MR0llV0uYRa5gPS0EGylxyb5l_5r7enmT6td6T7INXMc6lDAmJZoADaIOHnH-1A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_CzfxhuRiCDkt2MBTYFlJ8Y51YReytA8GUtAxGYyUGB-q8MapEQpj6Dp1zApgZhN6GulTqW6W-fi5Mdb4AsujqY,&b64e=1&sign=b618bbc8981b0a086e1f9a99bdb86603&keyno=1',
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
                    'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 494192821,
            },
            outletCount: 2,
            pickupCount: 0,
            localStoreCount: 2,
            phone: {
                number: '8 800 500-53-29',
                sanitized: '88005005329',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbnNgSTWltlSV-vcM82s2rlzi5zT6__WwG2rv0CfFbD67EeEawlsRbx5697ettYI1Y740IegvR5DmLPAQsm5vQPEEFjBu8u_TFV1lezHRrXTIk4tEO-mpXv6hF74-bVg-HoEhH5Sw4DGnQBuDfm_Spcnt0yDa-Wsxk3RaibYCSkfZxmYBn-bQ_w90_KDbHnxzPRufE5br16kifQuEKEM0t-HAkxceD0bl7vsNGEf0uo7HG4pClAc6qJd9EhbyX6TfeEPWuCvUEnUSj7QHdm_4iyxit71uSEfQ7L0TTxP3PjJOwisgwNxCaWT6iGwxkRuNFXjHjNbBGo-EQNOuFG9Hfpw0tFhRMSBGWQLn91-SSesfCw8f_rM7q5noB1l0mC8ZGZp6DwZ8kbITALdVX6QVtogJaKrcjNLjJi0TUnwAYnic4_f4DYHlHjS6zMKz6PxyQaJCsiShbLF0Wq_1aBsf3rNcp2hwv5hIrOukNoId5oxlsmsxcinTRbwqgg1hnvOQsNXJ3gkF4aHGQpPcY2QAb0iwDamrmGdt43xT3H0pepY7Uui5JHKktyh_CG9MLT4l9eQbiVU13C5Tamnkg70MTwYi6lskufbmm8Tirb5YFMJ2o3Z2CT5yBJiar4BxYgIn35WjIhAWtISYMlwvyMAdDva3glibKf9p0evz3H7mhefS1CL2D-AAaGzuSk6AS03ZA6QGXgl8kzAoSbHen0NwSeT7bpA7AJuV6NN749c0uxBeg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_OQERUebGnkiKHPWbrDR6MrPYeDFBtIvzbS0PE8qSjR6xdwr_Kh0t6kcykcmBqtYH6rjkj0rYC87XRJRHpheSDJ0pcJI6zCu-SgtIrPBeKc_joiUihS72JMFDSoesnm1mDgR2nnIANme4HcT3NR8JJOpRSxChBTc6P5uU_Zf6OQQ,,&b64e=1&sign=98033d2633bebe83e4e4110958b245a8&keyno=1',
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
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 3732918,
                name: 'GUESS',
                site: 'http://www.guesswatches.com',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id781654316530114328/orig',
                link: 'https://market.yandex.ru/brands--guess/3732918?pp=482&clid=2210590&distr_type=4',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            outlet: {
                id: '27486755',
                name: 'Салон оптики "Слепая курица" Москва ТРК "Афимолл Сити"',
                type: 'mixed',
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
                        'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                    outlets: [],
                },
                address: {
                    regionId: 213,
                    locality: 'Москва',
                    thoroughfare: 'Пресненская набережная',
                    premiseNumber: '2',
                    fullAddress: 'Москва, Пресненская набережная, д. 2',
                    geoPoint: {
                        coordinates: {
                            latitude: 55.74906,
                            longitude: 37.539347,
                        },
                    },
                },
                phones: [
                    {
                        number: '+7 (495) 120-3427',
                        sanitized: '74951203427',
                    },
                ],
                schedule: [
                    {
                        daysFrom: '7',
                        daysTill: '7',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '1',
                        daysTill: '1',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '2',
                        daysTill: '2',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '3',
                        daysTill: '3',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '4',
                        daysTill: '4',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '5',
                        daysTill: '5',
                        from: '10:00',
                        till: '23:00',
                    },
                    {
                        daysFrom: '6',
                        daysTill: '6',
                        from: '10:00',
                        till: '23:00',
                    },
                ],
            },
            link:
                'https://market.yandex.ru/offer/Gka30W292kXjFe6pV8FnMg?model_id=494192821&hid=7812201&pp=482&clid=2210590&distr_type=4&cpc=OKGXw_tnfhyF1GB5pB03E7qHo6o9-J-y6oDh8QNhHP4IFi5jA9CfeWjcJMhf3sL9pqrpUitxY3pU5BQEiDlsX_IvPnv-EuPHDvxr7bWuvszwBnKzuX4Xl3ypyTPCGeIntsUe4omMtr1gxBGDWrN-_f6N0kIzqpHr&lr=213',
            variationCount: 5,
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'CpaBuy',
                        name: 'MNA_DefaultOffer_2680',
                        value: '0.0902174',
                    },
                    {
                        tag: 'CpcClick',
                        name: 'MNA_HybridAuctionCpcCtr2430',
                        value: '0.299896',
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
            previewPhotos: [
                {
                    width: 158,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1577758/market_szPrToKUpEKDwHrxT-DFAA/120x160',
                },
            ],
            activeFilters: [
                {
                    id: '14871214',
                    name: 'Цвет товара',
                    type: 'PHOTO_PICKER',
                    subType: 'IMAGE_PICKER',
                    values: [
                        {
                            id: '14899090',
                            name: 'черный',
                            color: '#000000',
                        },
                    ],
                },
            ],
        },
        {
            id: 'yDpJekrrgZGNpLQIfBjKFJuqn_nWIzNZ0EDlvihTZ037_HG4SYQeDw',
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
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            urls: {
                482: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            },
            directUrl:
                'https://slepayakurica.ru/sumki/sumka-zhenskaya-guess-hw-vg74-39060-black/?utm_source=market&utm_medium=cpc&utm_content=7885&deduplication_channel=market',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZnCMIP_BgY_pMYKSkOCUpFV2Vdz10QvW1-I5T_d21cXSSMp68RMdWuJv_cGK4qhRYHSOVxlUV7dyn1DvOeVzAxrlx5F136EeGglInY6zuYFNFyBbfjxYWPzukZCRxMB9j3UF9RV2vdhw-WlnkVDveN5OhrkKe4J4XdHkK2HouIxEOmYrk1SuWQ2ODeitzDRESSXgMIt7t2g1CcTchR3uuMgL23A1D6XJnblEMynvTWshcJcHIS-OlC5RgkNVTmraQwylskrVtT-NHhyjBY6n_3dqrlnd6K04gH7as2ZbT7R0RlYhkJlDuWr4NPM-g7B7h9iUrGpPd_Yng2RHszcMPxYMEAq2yGJJTlswv4Qi_qegzhke2KO42WIfQiT4KE0I0enyJdwwT4dsefvS1fr_NcKBewnYkBVsJ7mhlgJjD-ag7Q1yQQIRUxZ_jS_yMG-ky1r-46YIFN-I5kuU-hgGCUu7n7EFdAnwVspVP_DseHQR03pOQJ5zgjrGH3ueeI5aoj-i9JI9sQSfXqj-9_PiCeBFdwRvN_dDKBdP5E_l7KFjUDtGmv41D_9KMV6pSwJ1KxfkTdnenWK4dCgBUvKM-JR0zWFWLBAam11j_RRxVlgKY47SgZ46HDuxx2Z4z8UcrUCH_kjvn0D-1H4hWMbFq1rdbykxYOyRCSXrCGTR9csy1s8tQKzUrTi8xyLHSPgneHzCnjW8NOCHAhbtKoR2N9rqXzfJiLOXsFszg-0opILghki2U0jej4g,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_Mh10p3rkVEwce0QVWd9XunfvxEX_MDr10MhMvH3u0JecsH5Qw0O-JDZ0na_j2fFuSWenDqZv8XjwsAEAU5BJ9E,&b64e=1&sign=df71c3f91df5b34446c202e2e71a8877&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZSihUHLaU1GXvp6LSIuPeI5_dVW9YLeXntIKqZ9HlH1Cu589kewjMYpsBJIe1XT0FEnhFF-cbqPYDBa0o6H5Y0HxvuegAWwZoMsp03qqBnH5rz-7bjTzpZSva89FqVbBL5lJOBBbPVEY_wbegbtA6aFZnyd88frSvwX2PisvBbBvabHmHAkqZJhfO12vYZHPy0xo4iqumVqxPTSEz-qtnfeSL9L2axi--IoWC0gmcSO6arpdQBl54yzrZqCaydg3yFsd9hjfYdC9uqsgj8QbcIeopXAD71E-se_h9z3SbOKcUflDJnjRXHzguQi-K0dCUQH4c2xTAXql1aJVWSTfdomWn8mN7wzJG1UniBiA1LLQVMGzHB_yGcYjkrYpxOvSBYPt8F8JEz8j9CcLHMJeiqna84il-hwnrO5-_hgRBIErVAfs8mbL7GB4ydwtUzXMoLlAbKWBoR1UCC7VHwjGJk28KNA_evYYYAEmjL7IPmkecDd-bFJPfV2qU5Yl-YR5fZSgauEajTyv19rMAMsxGEzz5lTLkJj1pi5xiClT6uR0y4AgP7rfJz-iTOvmh1ADwEW_eOVg6IiFccN8Uxu-fK9doYFUTXcVOcVAH16JSArJbnFBWkkz7m5tXLXQX9mNAm38IvXe0vl96R0mgVvpXTMzCbTjJ7fje6fPpYsafwEYpds4Whl-Oai8MR0llV0uYRa5gPS0EGylxyb5l_5r7enmT6td6T7INXMc6lDAmJZoADaIOHnH-1A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_CzfxhuRiCDkt2MBTYFlJ8Y51YReytA8GUtAxGYyUGB-q8MapEQpj6Dp1zApgZhN6GulTqW6W-fi5Mdb4AsujqY,&b64e=1&sign=b618bbc8981b0a086e1f9a99bdb86603&keyno=1',
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
                    'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 494192821,
            },
            outletCount: 2,
            pickupCount: 0,
            localStoreCount: 2,
            phone: {
                number: '8 800 500-53-29',
                sanitized: '88005005329',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbnNgSTWltlSV-vcM82s2rlzi5zT6__WwG2rv0CfFbD67EeEawlsRbx5697ettYI1Y740IegvR5DmLPAQsm5vQPEEFjBu8u_TFV1lezHRrXTIk4tEO-mpXv6hF74-bVg-HoEhH5Sw4DGnQBuDfm_Spcnt0yDa-Wsxk3RaibYCSkfZxmYBn-bQ_w90_KDbHnxzPRufE5br16kifQuEKEM0t-HAkxceD0bl7vsNGEf0uo7HG4pClAc6qJd9EhbyX6TfeEPWuCvUEnUSj7QHdm_4iyxit71uSEfQ7L0TTxP3PjJOwisgwNxCaWT6iGwxkRuNFXjHjNbBGo-EQNOuFG9Hfpw0tFhRMSBGWQLn91-SSesfCw8f_rM7q5noB1l0mC8ZGZp6DwZ8kbITALdVX6QVtogJaKrcjNLjJi0TUnwAYnic4_f4DYHlHjS6zMKz6PxyQaJCsiShbLF0Wq_1aBsf3rNcp2hwv5hIrOukNoId5oxlsmsxcinTRbwqgg1hnvOQsNXJ3gkF4aHGQpPcY2QAb0iwDamrmGdt43xT3H0pepY7Uui5JHKktyh_CG9MLT4l9eQbiVU13C5Tamnkg70MTwYi6lskufbmm8Tirb5YFMJ2o3Z2CT5yBJiar4BxYgIn35WjIhAWtISYMlwvyMAdDva3glibKf9p0evz3H7mhefS1CL2D-AAaGzuSk6AS03ZA6QGXgl8kzAoSbHen0NwSeT7bpA7AJuV6NN749c0uxBeg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_OQERUebGnkiKHPWbrDR6MrPYeDFBtIvzbS0PE8qSjR6xdwr_Kh0t6kcykcmBqtYH6rjkj0rYC87XRJRHpheSDJ0pcJI6zCu-SgtIrPBeKc_joiUihS72JMFDSoesnm1mDgR2nnIANme4HcT3NR8JJOpRSxChBTc6P5uU_Zf6OQQ,,&b64e=1&sign=98033d2633bebe83e4e4110958b245a8&keyno=1',
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
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 3732918,
                name: 'GUESS',
                site: 'http://www.guesswatches.com',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id781654316530114328/orig',
                link: 'https://market.yandex.ru/brands--guess/3732918?pp=482&clid=2210590&distr_type=4',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            outlet: {
                id: '27486755',
                name: 'Салон оптики "Слепая курица" Москва ТРК "Афимолл Сити"',
                type: 'mixed',
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
                        'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                    outlets: [],
                },
                address: {
                    regionId: 213,
                    locality: 'Москва',
                    thoroughfare: 'Пресненская набережная',
                    premiseNumber: '2',
                    fullAddress: 'Москва, Пресненская набережная, д. 2',
                    geoPoint: {
                        coordinates: {
                            latitude: 55.74906,
                            longitude: 37.539347,
                        },
                    },
                },
                phones: [
                    {
                        number: '+7 (495) 120-3427',
                        sanitized: '74951203427',
                    },
                ],
                schedule: [
                    {
                        daysFrom: '7',
                        daysTill: '7',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '1',
                        daysTill: '1',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '2',
                        daysTill: '2',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '3',
                        daysTill: '3',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '4',
                        daysTill: '4',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '5',
                        daysTill: '5',
                        from: '10:00',
                        till: '23:00',
                    },
                    {
                        daysFrom: '6',
                        daysTill: '6',
                        from: '10:00',
                        till: '23:00',
                    },
                ],
            },
            link:
                'https://market.yandex.ru/offer/Gka30W292kXjFe6pV8FnMg?model_id=494192821&hid=7812201&pp=482&clid=2210590&distr_type=4&cpc=OKGXw_tnfhyF1GB5pB03E7qHo6o9-J-y6oDh8QNhHP4IFi5jA9CfeWjcJMhf3sL9pqrpUitxY3pU5BQEiDlsX_IvPnv-EuPHDvxr7bWuvszwBnKzuX4Xl3ypyTPCGeIntsUe4omMtr1gxBGDWrN-_f6N0kIzqpHr&lr=213',
            variationCount: 5,
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'CpaBuy',
                        name: 'MNA_DefaultOffer_2680',
                        value: '0.0902174',
                    },
                    {
                        tag: 'CpcClick',
                        name: 'MNA_HybridAuctionCpcCtr2430',
                        value: '0.299896',
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
            previewPhotos: [
                {
                    width: 158,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1577758/market_szPrToKUpEKDwHrxT-DFAA/120x160',
                },
            ],
            activeFilters: [
                {
                    id: '14871214',
                    name: 'Цвет товара',
                    type: 'PHOTO_PICKER',
                    subType: 'IMAGE_PICKER',
                    values: [
                        {
                            id: '14899090',
                            name: 'черный',
                            color: '#000000',
                        },
                    ],
                },
            ],
        },
        {
            id: 'yDiJekrrgZGNpLQTfBjKFJuqn_nWIzNZ0EUlvihTZ037_HG4SYQeDw',
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
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            urls: {
                482: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            },
            directUrl:
                'https://slepayakurica.ru/sumki/sumka-zhenskaya-guess-hw-vg74-39060-black/?utm_source=market&utm_medium=cpc&utm_content=7885&deduplication_channel=market',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZnCMIP_BgY_pMYKSkOCUpFV2Vdz10QvW1-I5T_d21cXSSMp68RMdWuJv_cGK4qhRYHSOVxlUV7dyn1DvOeVzAxrlx5F136EeGglInY6zuYFNFyBbfjxYWPzukZCRxMB9j3UF9RV2vdhw-WlnkVDveN5OhrkKe4J4XdHkK2HouIxEOmYrk1SuWQ2ODeitzDRESSXgMIt7t2g1CcTchR3uuMgL23A1D6XJnblEMynvTWshcJcHIS-OlC5RgkNVTmraQwylskrVtT-NHhyjBY6n_3dqrlnd6K04gH7as2ZbT7R0RlYhkJlDuWr4NPM-g7B7h9iUrGpPd_Yng2RHszcMPxYMEAq2yGJJTlswv4Qi_qegzhke2KO42WIfQiT4KE0I0enyJdwwT4dsefvS1fr_NcKBewnYkBVsJ7mhlgJjD-ag7Q1yQQIRUxZ_jS_yMG-ky1r-46YIFN-I5kuU-hgGCUu7n7EFdAnwVspVP_DseHQR03pOQJ5zgjrGH3ueeI5aoj-i9JI9sQSfXqj-9_PiCeBFdwRvN_dDKBdP5E_l7KFjUDtGmv41D_9KMV6pSwJ1KxfkTdnenWK4dCgBUvKM-JR0zWFWLBAam11j_RRxVlgKY47SgZ46HDuxx2Z4z8UcrUCH_kjvn0D-1H4hWMbFq1rdbykxYOyRCSXrCGTR9csy1s8tQKzUrTi8xyLHSPgneHzCnjW8NOCHAhbtKoR2N9rqXzfJiLOXsFszg-0opILghki2U0jej4g,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_Mh10p3rkVEwce0QVWd9XunfvxEX_MDr10MhMvH3u0JecsH5Qw0O-JDZ0na_j2fFuSWenDqZv8XjwsAEAU5BJ9E,&b64e=1&sign=df71c3f91df5b34446c202e2e71a8877&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZSihUHLaU1GXvp6LSIuPeI5_dVW9YLeXntIKqZ9HlH1Cu589kewjMYpsBJIe1XT0FEnhFF-cbqPYDBa0o6H5Y0HxvuegAWwZoMsp03qqBnH5rz-7bjTzpZSva89FqVbBL5lJOBBbPVEY_wbegbtA6aFZnyd88frSvwX2PisvBbBvabHmHAkqZJhfO12vYZHPy0xo4iqumVqxPTSEz-qtnfeSL9L2axi--IoWC0gmcSO6arpdQBl54yzrZqCaydg3yFsd9hjfYdC9uqsgj8QbcIeopXAD71E-se_h9z3SbOKcUflDJnjRXHzguQi-K0dCUQH4c2xTAXql1aJVWSTfdomWn8mN7wzJG1UniBiA1LLQVMGzHB_yGcYjkrYpxOvSBYPt8F8JEz8j9CcLHMJeiqna84il-hwnrO5-_hgRBIErVAfs8mbL7GB4ydwtUzXMoLlAbKWBoR1UCC7VHwjGJk28KNA_evYYYAEmjL7IPmkecDd-bFJPfV2qU5Yl-YR5fZSgauEajTyv19rMAMsxGEzz5lTLkJj1pi5xiClT6uR0y4AgP7rfJz-iTOvmh1ADwEW_eOVg6IiFccN8Uxu-fK9doYFUTXcVOcVAH16JSArJbnFBWkkz7m5tXLXQX9mNAm38IvXe0vl96R0mgVvpXTMzCbTjJ7fje6fPpYsafwEYpds4Whl-Oai8MR0llV0uYRa5gPS0EGylxyb5l_5r7enmT6td6T7INXMc6lDAmJZoADaIOHnH-1A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_CzfxhuRiCDkt2MBTYFlJ8Y51YReytA8GUtAxGYyUGB-q8MapEQpj6Dp1zApgZhN6GulTqW6W-fi5Mdb4AsujqY,&b64e=1&sign=b618bbc8981b0a086e1f9a99bdb86603&keyno=1',
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
                    'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 494192821,
            },
            outletCount: 2,
            pickupCount: 0,
            localStoreCount: 2,
            phone: {
                number: '8 800 500-53-29',
                sanitized: '88005005329',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbnNgSTWltlSV-vcM82s2rlzi5zT6__WwG2rv0CfFbD67EeEawlsRbx5697ettYI1Y740IegvR5DmLPAQsm5vQPEEFjBu8u_TFV1lezHRrXTIk4tEO-mpXv6hF74-bVg-HoEhH5Sw4DGnQBuDfm_Spcnt0yDa-Wsxk3RaibYCSkfZxmYBn-bQ_w90_KDbHnxzPRufE5br16kifQuEKEM0t-HAkxceD0bl7vsNGEf0uo7HG4pClAc6qJd9EhbyX6TfeEPWuCvUEnUSj7QHdm_4iyxit71uSEfQ7L0TTxP3PjJOwisgwNxCaWT6iGwxkRuNFXjHjNbBGo-EQNOuFG9Hfpw0tFhRMSBGWQLn91-SSesfCw8f_rM7q5noB1l0mC8ZGZp6DwZ8kbITALdVX6QVtogJaKrcjNLjJi0TUnwAYnic4_f4DYHlHjS6zMKz6PxyQaJCsiShbLF0Wq_1aBsf3rNcp2hwv5hIrOukNoId5oxlsmsxcinTRbwqgg1hnvOQsNXJ3gkF4aHGQpPcY2QAb0iwDamrmGdt43xT3H0pepY7Uui5JHKktyh_CG9MLT4l9eQbiVU13C5Tamnkg70MTwYi6lskufbmm8Tirb5YFMJ2o3Z2CT5yBJiar4BxYgIn35WjIhAWtISYMlwvyMAdDva3glibKf9p0evz3H7mhefS1CL2D-AAaGzuSk6AS03ZA6QGXgl8kzAoSbHen0NwSeT7bpA7AJuV6NN749c0uxBeg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_OQERUebGnkiKHPWbrDR6MrPYeDFBtIvzbS0PE8qSjR6xdwr_Kh0t6kcykcmBqtYH6rjkj0rYC87XRJRHpheSDJ0pcJI6zCu-SgtIrPBeKc_joiUihS72JMFDSoesnm1mDgR2nnIANme4HcT3NR8JJOpRSxChBTc6P5uU_Zf6OQQ,,&b64e=1&sign=98033d2633bebe83e4e4110958b245a8&keyno=1',
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
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 3732918,
                name: 'GUESS',
                site: 'http://www.guesswatches.com',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id781654316530114328/orig',
                link: 'https://market.yandex.ru/brands--guess/3732918?pp=482&clid=2210590&distr_type=4',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            outlet: {
                id: '27486755',
                name: 'Салон оптики "Слепая курица" Москва ТРК "Афимолл Сити"',
                type: 'mixed',
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
                        'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                    outlets: [],
                },
                address: {
                    regionId: 213,
                    locality: 'Москва',
                    thoroughfare: 'Пресненская набережная',
                    premiseNumber: '2',
                    fullAddress: 'Москва, Пресненская набережная, д. 2',
                    geoPoint: {
                        coordinates: {
                            latitude: 55.74906,
                            longitude: 37.539347,
                        },
                    },
                },
                phones: [
                    {
                        number: '+7 (495) 120-3427',
                        sanitized: '74951203427',
                    },
                ],
                schedule: [
                    {
                        daysFrom: '7',
                        daysTill: '7',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '1',
                        daysTill: '1',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '2',
                        daysTill: '2',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '3',
                        daysTill: '3',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '4',
                        daysTill: '4',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '5',
                        daysTill: '5',
                        from: '10:00',
                        till: '23:00',
                    },
                    {
                        daysFrom: '6',
                        daysTill: '6',
                        from: '10:00',
                        till: '23:00',
                    },
                ],
            },
            link:
                'https://market.yandex.ru/offer/Gka30W292kXjFe6pV8FnMg?model_id=494192821&hid=7812201&pp=482&clid=2210590&distr_type=4&cpc=OKGXw_tnfhyF1GB5pB03E7qHo6o9-J-y6oDh8QNhHP4IFi5jA9CfeWjcJMhf3sL9pqrpUitxY3pU5BQEiDlsX_IvPnv-EuPHDvxr7bWuvszwBnKzuX4Xl3ypyTPCGeIntsUe4omMtr1gxBGDWrN-_f6N0kIzqpHr&lr=213',
            variationCount: 5,
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'CpaBuy',
                        name: 'MNA_DefaultOffer_2680',
                        value: '0.0902174',
                    },
                    {
                        tag: 'CpcClick',
                        name: 'MNA_HybridAuctionCpcCtr2430',
                        value: '0.299896',
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
            previewPhotos: [
                {
                    width: 158,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1577758/market_szPrToKUpEKDwHrxT-DFAA/120x160',
                },
            ],
            activeFilters: [
                {
                    id: '14871214',
                    name: 'Цвет товара',
                    type: 'PHOTO_PICKER',
                    subType: 'IMAGE_PICKER',
                    values: [
                        {
                            id: '14899090',
                            name: 'черный',
                            color: '#000000',
                        },
                    ],
                },
            ],
        },
        {
            id: 'yDpJekrrgZGNpLQTfBjKFJuqn_nWIzNZ0EDlviOTZ037_HG4SYQeDw',
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
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            urls: {
                482: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            },
            directUrl:
                'https://slepayakurica.ru/sumki/sumka-zhenskaya-guess-hw-vg74-39060-black/?utm_source=market&utm_medium=cpc&utm_content=7885&deduplication_channel=market',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZnCMIP_BgY_pMYKSkOCUpFV2Vdz10QvW1-I5T_d21cXSSMp68RMdWuJv_cGK4qhRYHSOVxlUV7dyn1DvOeVzAxrlx5F136EeGglInY6zuYFNFyBbfjxYWPzukZCRxMB9j3UF9RV2vdhw-WlnkVDveN5OhrkKe4J4XdHkK2HouIxEOmYrk1SuWQ2ODeitzDRESSXgMIt7t2g1CcTchR3uuMgL23A1D6XJnblEMynvTWshcJcHIS-OlC5RgkNVTmraQwylskrVtT-NHhyjBY6n_3dqrlnd6K04gH7as2ZbT7R0RlYhkJlDuWr4NPM-g7B7h9iUrGpPd_Yng2RHszcMPxYMEAq2yGJJTlswv4Qi_qegzhke2KO42WIfQiT4KE0I0enyJdwwT4dsefvS1fr_NcKBewnYkBVsJ7mhlgJjD-ag7Q1yQQIRUxZ_jS_yMG-ky1r-46YIFN-I5kuU-hgGCUu7n7EFdAnwVspVP_DseHQR03pOQJ5zgjrGH3ueeI5aoj-i9JI9sQSfXqj-9_PiCeBFdwRvN_dDKBdP5E_l7KFjUDtGmv41D_9KMV6pSwJ1KxfkTdnenWK4dCgBUvKM-JR0zWFWLBAam11j_RRxVlgKY47SgZ46HDuxx2Z4z8UcrUCH_kjvn0D-1H4hWMbFq1rdbykxYOyRCSXrCGTR9csy1s8tQKzUrTi8xyLHSPgneHzCnjW8NOCHAhbtKoR2N9rqXzfJiLOXsFszg-0opILghki2U0jej4g,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_Mh10p3rkVEwce0QVWd9XunfvxEX_MDr10MhMvH3u0JecsH5Qw0O-JDZ0na_j2fFuSWenDqZv8XjwsAEAU5BJ9E,&b64e=1&sign=df71c3f91df5b34446c202e2e71a8877&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZSihUHLaU1GXvp6LSIuPeI5_dVW9YLeXntIKqZ9HlH1Cu589kewjMYpsBJIe1XT0FEnhFF-cbqPYDBa0o6H5Y0HxvuegAWwZoMsp03qqBnH5rz-7bjTzpZSva89FqVbBL5lJOBBbPVEY_wbegbtA6aFZnyd88frSvwX2PisvBbBvabHmHAkqZJhfO12vYZHPy0xo4iqumVqxPTSEz-qtnfeSL9L2axi--IoWC0gmcSO6arpdQBl54yzrZqCaydg3yFsd9hjfYdC9uqsgj8QbcIeopXAD71E-se_h9z3SbOKcUflDJnjRXHzguQi-K0dCUQH4c2xTAXql1aJVWSTfdomWn8mN7wzJG1UniBiA1LLQVMGzHB_yGcYjkrYpxOvSBYPt8F8JEz8j9CcLHMJeiqna84il-hwnrO5-_hgRBIErVAfs8mbL7GB4ydwtUzXMoLlAbKWBoR1UCC7VHwjGJk28KNA_evYYYAEmjL7IPmkecDd-bFJPfV2qU5Yl-YR5fZSgauEajTyv19rMAMsxGEzz5lTLkJj1pi5xiClT6uR0y4AgP7rfJz-iTOvmh1ADwEW_eOVg6IiFccN8Uxu-fK9doYFUTXcVOcVAH16JSArJbnFBWkkz7m5tXLXQX9mNAm38IvXe0vl96R0mgVvpXTMzCbTjJ7fje6fPpYsafwEYpds4Whl-Oai8MR0llV0uYRa5gPS0EGylxyb5l_5r7enmT6td6T7INXMc6lDAmJZoADaIOHnH-1A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_CzfxhuRiCDkt2MBTYFlJ8Y51YReytA8GUtAxGYyUGB-q8MapEQpj6Dp1zApgZhN6GulTqW6W-fi5Mdb4AsujqY,&b64e=1&sign=b618bbc8981b0a086e1f9a99bdb86603&keyno=1',
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
                    'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 494192821,
            },
            outletCount: 2,
            pickupCount: 0,
            localStoreCount: 2,
            phone: {
                number: '8 800 500-53-29',
                sanitized: '88005005329',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbnNgSTWltlSV-vcM82s2rlzi5zT6__WwG2rv0CfFbD67EeEawlsRbx5697ettYI1Y740IegvR5DmLPAQsm5vQPEEFjBu8u_TFV1lezHRrXTIk4tEO-mpXv6hF74-bVg-HoEhH5Sw4DGnQBuDfm_Spcnt0yDa-Wsxk3RaibYCSkfZxmYBn-bQ_w90_KDbHnxzPRufE5br16kifQuEKEM0t-HAkxceD0bl7vsNGEf0uo7HG4pClAc6qJd9EhbyX6TfeEPWuCvUEnUSj7QHdm_4iyxit71uSEfQ7L0TTxP3PjJOwisgwNxCaWT6iGwxkRuNFXjHjNbBGo-EQNOuFG9Hfpw0tFhRMSBGWQLn91-SSesfCw8f_rM7q5noB1l0mC8ZGZp6DwZ8kbITALdVX6QVtogJaKrcjNLjJi0TUnwAYnic4_f4DYHlHjS6zMKz6PxyQaJCsiShbLF0Wq_1aBsf3rNcp2hwv5hIrOukNoId5oxlsmsxcinTRbwqgg1hnvOQsNXJ3gkF4aHGQpPcY2QAb0iwDamrmGdt43xT3H0pepY7Uui5JHKktyh_CG9MLT4l9eQbiVU13C5Tamnkg70MTwYi6lskufbmm8Tirb5YFMJ2o3Z2CT5yBJiar4BxYgIn35WjIhAWtISYMlwvyMAdDva3glibKf9p0evz3H7mhefS1CL2D-AAaGzuSk6AS03ZA6QGXgl8kzAoSbHen0NwSeT7bpA7AJuV6NN749c0uxBeg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_OQERUebGnkiKHPWbrDR6MrPYeDFBtIvzbS0PE8qSjR6xdwr_Kh0t6kcykcmBqtYH6rjkj0rYC87XRJRHpheSDJ0pcJI6zCu-SgtIrPBeKc_joiUihS72JMFDSoesnm1mDgR2nnIANme4HcT3NR8JJOpRSxChBTc6P5uU_Zf6OQQ,,&b64e=1&sign=98033d2633bebe83e4e4110958b245a8&keyno=1',
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
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 3732918,
                name: 'GUESS',
                site: 'http://www.guesswatches.com',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id781654316530114328/orig',
                link: 'https://market.yandex.ru/brands--guess/3732918?pp=482&clid=2210590&distr_type=4',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            outlet: {
                id: '27486755',
                name: 'Салон оптики "Слепая курица" Москва ТРК "Афимолл Сити"',
                type: 'mixed',
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
                        'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                    outlets: [],
                },
                address: {
                    regionId: 213,
                    locality: 'Москва',
                    thoroughfare: 'Пресненская набережная',
                    premiseNumber: '2',
                    fullAddress: 'Москва, Пресненская набережная, д. 2',
                    geoPoint: {
                        coordinates: {
                            latitude: 55.74906,
                            longitude: 37.539347,
                        },
                    },
                },
                phones: [
                    {
                        number: '+7 (495) 120-3427',
                        sanitized: '74951203427',
                    },
                ],
                schedule: [
                    {
                        daysFrom: '7',
                        daysTill: '7',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '1',
                        daysTill: '1',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '2',
                        daysTill: '2',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '3',
                        daysTill: '3',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '4',
                        daysTill: '4',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '5',
                        daysTill: '5',
                        from: '10:00',
                        till: '23:00',
                    },
                    {
                        daysFrom: '6',
                        daysTill: '6',
                        from: '10:00',
                        till: '23:00',
                    },
                ],
            },
            link:
                'https://market.yandex.ru/offer/Gka30W292kXjFe6pV8FnMg?model_id=494192821&hid=7812201&pp=482&clid=2210590&distr_type=4&cpc=OKGXw_tnfhyF1GB5pB03E7qHo6o9-J-y6oDh8QNhHP4IFi5jA9CfeWjcJMhf3sL9pqrpUitxY3pU5BQEiDlsX_IvPnv-EuPHDvxr7bWuvszwBnKzuX4Xl3ypyTPCGeIntsUe4omMtr1gxBGDWrN-_f6N0kIzqpHr&lr=213',
            variationCount: 5,
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'CpaBuy',
                        name: 'MNA_DefaultOffer_2680',
                        value: '0.0902174',
                    },
                    {
                        tag: 'CpcClick',
                        name: 'MNA_HybridAuctionCpcCtr2430',
                        value: '0.299896',
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
            previewPhotos: [
                {
                    width: 158,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1577758/market_szPrToKUpEKDwHrxT-DFAA/120x160',
                },
            ],
            activeFilters: [
                {
                    id: '14871214',
                    name: 'Цвет товара',
                    type: 'PHOTO_PICKER',
                    subType: 'IMAGE_PICKER',
                    values: [
                        {
                            id: '14899090',
                            name: 'черный',
                            color: '#000000',
                        },
                    ],
                },
            ],
        },
        {
            id: 'yDpJekrrgZGNpLQTfBjKFJuqn_nWIzNZ0ODlvihTZ037_HG4SYQeDw',
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
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            urls: {
                482: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            },
            directUrl:
                'https://slepayakurica.ru/sumki/sumka-zhenskaya-guess-hw-vg74-39060-black/?utm_source=market&utm_medium=cpc&utm_content=7885&deduplication_channel=market',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZnCMIP_BgY_pMYKSkOCUpFV2Vdz10QvW1-I5T_d21cXSSMp68RMdWuJv_cGK4qhRYHSOVxlUV7dyn1DvOeVzAxrlx5F136EeGglInY6zuYFNFyBbfjxYWPzukZCRxMB9j3UF9RV2vdhw-WlnkVDveN5OhrkKe4J4XdHkK2HouIxEOmYrk1SuWQ2ODeitzDRESSXgMIt7t2g1CcTchR3uuMgL23A1D6XJnblEMynvTWshcJcHIS-OlC5RgkNVTmraQwylskrVtT-NHhyjBY6n_3dqrlnd6K04gH7as2ZbT7R0RlYhkJlDuWr4NPM-g7B7h9iUrGpPd_Yng2RHszcMPxYMEAq2yGJJTlswv4Qi_qegzhke2KO42WIfQiT4KE0I0enyJdwwT4dsefvS1fr_NcKBewnYkBVsJ7mhlgJjD-ag7Q1yQQIRUxZ_jS_yMG-ky1r-46YIFN-I5kuU-hgGCUu7n7EFdAnwVspVP_DseHQR03pOQJ5zgjrGH3ueeI5aoj-i9JI9sQSfXqj-9_PiCeBFdwRvN_dDKBdP5E_l7KFjUDtGmv41D_9KMV6pSwJ1KxfkTdnenWK4dCgBUvKM-JR0zWFWLBAam11j_RRxVlgKY47SgZ46HDuxx2Z4z8UcrUCH_kjvn0D-1H4hWMbFq1rdbykxYOyRCSXrCGTR9csy1s8tQKzUrTi8xyLHSPgneHzCnjW8NOCHAhbtKoR2N9rqXzfJiLOXsFszg-0opILghki2U0jej4g,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_Mh10p3rkVEwce0QVWd9XunfvxEX_MDr10MhMvH3u0JecsH5Qw0O-JDZ0na_j2fFuSWenDqZv8XjwsAEAU5BJ9E,&b64e=1&sign=df71c3f91df5b34446c202e2e71a8877&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZSihUHLaU1GXvp6LSIuPeI5_dVW9YLeXntIKqZ9HlH1Cu589kewjMYpsBJIe1XT0FEnhFF-cbqPYDBa0o6H5Y0HxvuegAWwZoMsp03qqBnH5rz-7bjTzpZSva89FqVbBL5lJOBBbPVEY_wbegbtA6aFZnyd88frSvwX2PisvBbBvabHmHAkqZJhfO12vYZHPy0xo4iqumVqxPTSEz-qtnfeSL9L2axi--IoWC0gmcSO6arpdQBl54yzrZqCaydg3yFsd9hjfYdC9uqsgj8QbcIeopXAD71E-se_h9z3SbOKcUflDJnjRXHzguQi-K0dCUQH4c2xTAXql1aJVWSTfdomWn8mN7wzJG1UniBiA1LLQVMGzHB_yGcYjkrYpxOvSBYPt8F8JEz8j9CcLHMJeiqna84il-hwnrO5-_hgRBIErVAfs8mbL7GB4ydwtUzXMoLlAbKWBoR1UCC7VHwjGJk28KNA_evYYYAEmjL7IPmkecDd-bFJPfV2qU5Yl-YR5fZSgauEajTyv19rMAMsxGEzz5lTLkJj1pi5xiClT6uR0y4AgP7rfJz-iTOvmh1ADwEW_eOVg6IiFccN8Uxu-fK9doYFUTXcVOcVAH16JSArJbnFBWkkz7m5tXLXQX9mNAm38IvXe0vl96R0mgVvpXTMzCbTjJ7fje6fPpYsafwEYpds4Whl-Oai8MR0llV0uYRa5gPS0EGylxyb5l_5r7enmT6td6T7INXMc6lDAmJZoADaIOHnH-1A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_CzfxhuRiCDkt2MBTYFlJ8Y51YReytA8GUtAxGYyUGB-q8MapEQpj6Dp1zApgZhN6GulTqW6W-fi5Mdb4AsujqY,&b64e=1&sign=b618bbc8981b0a086e1f9a99bdb86603&keyno=1',
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
                    'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 494192821,
            },
            outletCount: 2,
            pickupCount: 0,
            localStoreCount: 2,
            phone: {
                number: '8 800 500-53-29',
                sanitized: '88005005329',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbnNgSTWltlSV-vcM82s2rlzi5zT6__WwG2rv0CfFbD67EeEawlsRbx5697ettYI1Y740IegvR5DmLPAQsm5vQPEEFjBu8u_TFV1lezHRrXTIk4tEO-mpXv6hF74-bVg-HoEhH5Sw4DGnQBuDfm_Spcnt0yDa-Wsxk3RaibYCSkfZxmYBn-bQ_w90_KDbHnxzPRufE5br16kifQuEKEM0t-HAkxceD0bl7vsNGEf0uo7HG4pClAc6qJd9EhbyX6TfeEPWuCvUEnUSj7QHdm_4iyxit71uSEfQ7L0TTxP3PjJOwisgwNxCaWT6iGwxkRuNFXjHjNbBGo-EQNOuFG9Hfpw0tFhRMSBGWQLn91-SSesfCw8f_rM7q5noB1l0mC8ZGZp6DwZ8kbITALdVX6QVtogJaKrcjNLjJi0TUnwAYnic4_f4DYHlHjS6zMKz6PxyQaJCsiShbLF0Wq_1aBsf3rNcp2hwv5hIrOukNoId5oxlsmsxcinTRbwqgg1hnvOQsNXJ3gkF4aHGQpPcY2QAb0iwDamrmGdt43xT3H0pepY7Uui5JHKktyh_CG9MLT4l9eQbiVU13C5Tamnkg70MTwYi6lskufbmm8Tirb5YFMJ2o3Z2CT5yBJiar4BxYgIn35WjIhAWtISYMlwvyMAdDva3glibKf9p0evz3H7mhefS1CL2D-AAaGzuSk6AS03ZA6QGXgl8kzAoSbHen0NwSeT7bpA7AJuV6NN749c0uxBeg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_OQERUebGnkiKHPWbrDR6MrPYeDFBtIvzbS0PE8qSjR6xdwr_Kh0t6kcykcmBqtYH6rjkj0rYC87XRJRHpheSDJ0pcJI6zCu-SgtIrPBeKc_joiUihS72JMFDSoesnm1mDgR2nnIANme4HcT3NR8JJOpRSxChBTc6P5uU_Zf6OQQ,,&b64e=1&sign=98033d2633bebe83e4e4110958b245a8&keyno=1',
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
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 3732918,
                name: 'GUESS',
                site: 'http://www.guesswatches.com',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id781654316530114328/orig',
                link: 'https://market.yandex.ru/brands--guess/3732918?pp=482&clid=2210590&distr_type=4',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            outlet: {
                id: '27486755',
                name: 'Салон оптики "Слепая курица" Москва ТРК "Афимолл Сити"',
                type: 'mixed',
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
                        'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                    outlets: [],
                },
                address: {
                    regionId: 213,
                    locality: 'Москва',
                    thoroughfare: 'Пресненская набережная',
                    premiseNumber: '2',
                    fullAddress: 'Москва, Пресненская набережная, д. 2',
                    geoPoint: {
                        coordinates: {
                            latitude: 55.74906,
                            longitude: 37.539347,
                        },
                    },
                },
                phones: [
                    {
                        number: '+7 (495) 120-3427',
                        sanitized: '74951203427',
                    },
                ],
                schedule: [
                    {
                        daysFrom: '7',
                        daysTill: '7',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '1',
                        daysTill: '1',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '2',
                        daysTill: '2',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '3',
                        daysTill: '3',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '4',
                        daysTill: '4',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '5',
                        daysTill: '5',
                        from: '10:00',
                        till: '23:00',
                    },
                    {
                        daysFrom: '6',
                        daysTill: '6',
                        from: '10:00',
                        till: '23:00',
                    },
                ],
            },
            link:
                'https://market.yandex.ru/offer/Gka30W292kXjFe6pV8FnMg?model_id=494192821&hid=7812201&pp=482&clid=2210590&distr_type=4&cpc=OKGXw_tnfhyF1GB5pB03E7qHo6o9-J-y6oDh8QNhHP4IFi5jA9CfeWjcJMhf3sL9pqrpUitxY3pU5BQEiDlsX_IvPnv-EuPHDvxr7bWuvszwBnKzuX4Xl3ypyTPCGeIntsUe4omMtr1gxBGDWrN-_f6N0kIzqpHr&lr=213',
            variationCount: 5,
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'CpaBuy',
                        name: 'MNA_DefaultOffer_2680',
                        value: '0.0902174',
                    },
                    {
                        tag: 'CpcClick',
                        name: 'MNA_HybridAuctionCpcCtr2430',
                        value: '0.299896',
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
            previewPhotos: [
                {
                    width: 158,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1577758/market_szPrToKUpEKDwHrxT-DFAA/120x160',
                },
            ],
            activeFilters: [
                {
                    id: '14871214',
                    name: 'Цвет товара',
                    type: 'PHOTO_PICKER',
                    subType: 'IMAGE_PICKER',
                    values: [
                        {
                            id: '14899090',
                            name: 'черный',
                            color: '#000000',
                        },
                    ],
                },
            ],
        },
        {
            id: 'yDpJekrrgZGNpLQTfBjKFJuqn_nWIzNZ0EDlvihTZ037_HG4SYQIDw',
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
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            urls: {
                482: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            },
            directUrl:
                'https://slepayakurica.ru/sumki/sumka-zhenskaya-guess-hw-vg74-39060-black/?utm_source=market&utm_medium=cpc&utm_content=7885&deduplication_channel=market',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZnCMIP_BgY_pMYKSkOCUpFV2Vdz10QvW1-I5T_d21cXSSMp68RMdWuJv_cGK4qhRYHSOVxlUV7dyn1DvOeVzAxrlx5F136EeGglInY6zuYFNFyBbfjxYWPzukZCRxMB9j3UF9RV2vdhw-WlnkVDveN5OhrkKe4J4XdHkK2HouIxEOmYrk1SuWQ2ODeitzDRESSXgMIt7t2g1CcTchR3uuMgL23A1D6XJnblEMynvTWshcJcHIS-OlC5RgkNVTmraQwylskrVtT-NHhyjBY6n_3dqrlnd6K04gH7as2ZbT7R0RlYhkJlDuWr4NPM-g7B7h9iUrGpPd_Yng2RHszcMPxYMEAq2yGJJTlswv4Qi_qegzhke2KO42WIfQiT4KE0I0enyJdwwT4dsefvS1fr_NcKBewnYkBVsJ7mhlgJjD-ag7Q1yQQIRUxZ_jS_yMG-ky1r-46YIFN-I5kuU-hgGCUu7n7EFdAnwVspVP_DseHQR03pOQJ5zgjrGH3ueeI5aoj-i9JI9sQSfXqj-9_PiCeBFdwRvN_dDKBdP5E_l7KFjUDtGmv41D_9KMV6pSwJ1KxfkTdnenWK4dCgBUvKM-JR0zWFWLBAam11j_RRxVlgKY47SgZ46HDuxx2Z4z8UcrUCH_kjvn0D-1H4hWMbFq1rdbykxYOyRCSXrCGTR9csy1s8tQKzUrTi8xyLHSPgneHzCnjW8NOCHAhbtKoR2N9rqXzfJiLOXsFszg-0opILghki2U0jej4g,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_Mh10p3rkVEwce0QVWd9XunfvxEX_MDr10MhMvH3u0JecsH5Qw0O-JDZ0na_j2fFuSWenDqZv8XjwsAEAU5BJ9E,&b64e=1&sign=df71c3f91df5b34446c202e2e71a8877&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZSihUHLaU1GXvp6LSIuPeI5_dVW9YLeXntIKqZ9HlH1Cu589kewjMYpsBJIe1XT0FEnhFF-cbqPYDBa0o6H5Y0HxvuegAWwZoMsp03qqBnH5rz-7bjTzpZSva89FqVbBL5lJOBBbPVEY_wbegbtA6aFZnyd88frSvwX2PisvBbBvabHmHAkqZJhfO12vYZHPy0xo4iqumVqxPTSEz-qtnfeSL9L2axi--IoWC0gmcSO6arpdQBl54yzrZqCaydg3yFsd9hjfYdC9uqsgj8QbcIeopXAD71E-se_h9z3SbOKcUflDJnjRXHzguQi-K0dCUQH4c2xTAXql1aJVWSTfdomWn8mN7wzJG1UniBiA1LLQVMGzHB_yGcYjkrYpxOvSBYPt8F8JEz8j9CcLHMJeiqna84il-hwnrO5-_hgRBIErVAfs8mbL7GB4ydwtUzXMoLlAbKWBoR1UCC7VHwjGJk28KNA_evYYYAEmjL7IPmkecDd-bFJPfV2qU5Yl-YR5fZSgauEajTyv19rMAMsxGEzz5lTLkJj1pi5xiClT6uR0y4AgP7rfJz-iTOvmh1ADwEW_eOVg6IiFccN8Uxu-fK9doYFUTXcVOcVAH16JSArJbnFBWkkz7m5tXLXQX9mNAm38IvXe0vl96R0mgVvpXTMzCbTjJ7fje6fPpYsafwEYpds4Whl-Oai8MR0llV0uYRa5gPS0EGylxyb5l_5r7enmT6td6T7INXMc6lDAmJZoADaIOHnH-1A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_CzfxhuRiCDkt2MBTYFlJ8Y51YReytA8GUtAxGYyUGB-q8MapEQpj6Dp1zApgZhN6GulTqW6W-fi5Mdb4AsujqY,&b64e=1&sign=b618bbc8981b0a086e1f9a99bdb86603&keyno=1',
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
                    'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 494192821,
            },
            outletCount: 2,
            pickupCount: 0,
            localStoreCount: 2,
            phone: {
                number: '8 800 500-53-29',
                sanitized: '88005005329',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbnNgSTWltlSV-vcM82s2rlzi5zT6__WwG2rv0CfFbD67EeEawlsRbx5697ettYI1Y740IegvR5DmLPAQsm5vQPEEFjBu8u_TFV1lezHRrXTIk4tEO-mpXv6hF74-bVg-HoEhH5Sw4DGnQBuDfm_Spcnt0yDa-Wsxk3RaibYCSkfZxmYBn-bQ_w90_KDbHnxzPRufE5br16kifQuEKEM0t-HAkxceD0bl7vsNGEf0uo7HG4pClAc6qJd9EhbyX6TfeEPWuCvUEnUSj7QHdm_4iyxit71uSEfQ7L0TTxP3PjJOwisgwNxCaWT6iGwxkRuNFXjHjNbBGo-EQNOuFG9Hfpw0tFhRMSBGWQLn91-SSesfCw8f_rM7q5noB1l0mC8ZGZp6DwZ8kbITALdVX6QVtogJaKrcjNLjJi0TUnwAYnic4_f4DYHlHjS6zMKz6PxyQaJCsiShbLF0Wq_1aBsf3rNcp2hwv5hIrOukNoId5oxlsmsxcinTRbwqgg1hnvOQsNXJ3gkF4aHGQpPcY2QAb0iwDamrmGdt43xT3H0pepY7Uui5JHKktyh_CG9MLT4l9eQbiVU13C5Tamnkg70MTwYi6lskufbmm8Tirb5YFMJ2o3Z2CT5yBJiar4BxYgIn35WjIhAWtISYMlwvyMAdDva3glibKf9p0evz3H7mhefS1CL2D-AAaGzuSk6AS03ZA6QGXgl8kzAoSbHen0NwSeT7bpA7AJuV6NN749c0uxBeg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_OQERUebGnkiKHPWbrDR6MrPYeDFBtIvzbS0PE8qSjR6xdwr_Kh0t6kcykcmBqtYH6rjkj0rYC87XRJRHpheSDJ0pcJI6zCu-SgtIrPBeKc_joiUihS72JMFDSoesnm1mDgR2nnIANme4HcT3NR8JJOpRSxChBTc6P5uU_Zf6OQQ,,&b64e=1&sign=98033d2633bebe83e4e4110958b245a8&keyno=1',
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
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 3732918,
                name: 'GUESS',
                site: 'http://www.guesswatches.com',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id781654316530114328/orig',
                link: 'https://market.yandex.ru/brands--guess/3732918?pp=482&clid=2210590&distr_type=4',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            outlet: {
                id: '27486755',
                name: 'Салон оптики "Слепая курица" Москва ТРК "Афимолл Сити"',
                type: 'mixed',
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
                        'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                    outlets: [],
                },
                address: {
                    regionId: 213,
                    locality: 'Москва',
                    thoroughfare: 'Пресненская набережная',
                    premiseNumber: '2',
                    fullAddress: 'Москва, Пресненская набережная, д. 2',
                    geoPoint: {
                        coordinates: {
                            latitude: 55.74906,
                            longitude: 37.539347,
                        },
                    },
                },
                phones: [
                    {
                        number: '+7 (495) 120-3427',
                        sanitized: '74951203427',
                    },
                ],
                schedule: [
                    {
                        daysFrom: '7',
                        daysTill: '7',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '1',
                        daysTill: '1',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '2',
                        daysTill: '2',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '3',
                        daysTill: '3',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '4',
                        daysTill: '4',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '5',
                        daysTill: '5',
                        from: '10:00',
                        till: '23:00',
                    },
                    {
                        daysFrom: '6',
                        daysTill: '6',
                        from: '10:00',
                        till: '23:00',
                    },
                ],
            },
            link:
                'https://market.yandex.ru/offer/Gka30W292kXjFe6pV8FnMg?model_id=494192821&hid=7812201&pp=482&clid=2210590&distr_type=4&cpc=OKGXw_tnfhyF1GB5pB03E7qHo6o9-J-y6oDh8QNhHP4IFi5jA9CfeWjcJMhf3sL9pqrpUitxY3pU5BQEiDlsX_IvPnv-EuPHDvxr7bWuvszwBnKzuX4Xl3ypyTPCGeIntsUe4omMtr1gxBGDWrN-_f6N0kIzqpHr&lr=213',
            variationCount: 5,
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'CpaBuy',
                        name: 'MNA_DefaultOffer_2680',
                        value: '0.0902174',
                    },
                    {
                        tag: 'CpcClick',
                        name: 'MNA_HybridAuctionCpcCtr2430',
                        value: '0.299896',
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
            previewPhotos: [
                {
                    width: 158,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1577758/market_szPrToKUpEKDwHrxT-DFAA/120x160',
                },
            ],
            activeFilters: [
                {
                    id: '14871214',
                    name: 'Цвет товара',
                    type: 'PHOTO_PICKER',
                    subType: 'IMAGE_PICKER',
                    values: [
                        {
                            id: '14899090',
                            name: 'черный',
                            color: '#000000',
                        },
                    ],
                },
            ],
        },
        {
            id: 'yDpYekrrgpGNpLQTfBjKFJuqn_nWIzNZ0EDlvihTZ037_HG4SYQeDw',
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
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            urls: {
                482: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            },
            directUrl:
                'https://slepayakurica.ru/sumki/sumka-zhenskaya-guess-hw-vg74-39060-black/?utm_source=market&utm_medium=cpc&utm_content=7885&deduplication_channel=market',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZnCMIP_BgY_pMYKSkOCUpFV2Vdz10QvW1-I5T_d21cXSSMp68RMdWuJv_cGK4qhRYHSOVxlUV7dyn1DvOeVzAxrlx5F136EeGglInY6zuYFNFyBbfjxYWPzukZCRxMB9j3UF9RV2vdhw-WlnkVDveN5OhrkKe4J4XdHkK2HouIxEOmYrk1SuWQ2ODeitzDRESSXgMIt7t2g1CcTchR3uuMgL23A1D6XJnblEMynvTWshcJcHIS-OlC5RgkNVTmraQwylskrVtT-NHhyjBY6n_3dqrlnd6K04gH7as2ZbT7R0RlYhkJlDuWr4NPM-g7B7h9iUrGpPd_Yng2RHszcMPxYMEAq2yGJJTlswv4Qi_qegzhke2KO42WIfQiT4KE0I0enyJdwwT4dsefvS1fr_NcKBewnYkBVsJ7mhlgJjD-ag7Q1yQQIRUxZ_jS_yMG-ky1r-46YIFN-I5kuU-hgGCUu7n7EFdAnwVspVP_DseHQR03pOQJ5zgjrGH3ueeI5aoj-i9JI9sQSfXqj-9_PiCeBFdwRvN_dDKBdP5E_l7KFjUDtGmv41D_9KMV6pSwJ1KxfkTdnenWK4dCgBUvKM-JR0zWFWLBAam11j_RRxVlgKY47SgZ46HDuxx2Z4z8UcrUCH_kjvn0D-1H4hWMbFq1rdbykxYOyRCSXrCGTR9csy1s8tQKzUrTi8xyLHSPgneHzCnjW8NOCHAhbtKoR2N9rqXzfJiLOXsFszg-0opILghki2U0jej4g,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_Mh10p3rkVEwce0QVWd9XunfvxEX_MDr10MhMvH3u0JecsH5Qw0O-JDZ0na_j2fFuSWenDqZv8XjwsAEAU5BJ9E,&b64e=1&sign=df71c3f91df5b34446c202e2e71a8877&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZSihUHLaU1GXvp6LSIuPeI5_dVW9YLeXntIKqZ9HlH1Cu589kewjMYpsBJIe1XT0FEnhFF-cbqPYDBa0o6H5Y0HxvuegAWwZoMsp03qqBnH5rz-7bjTzpZSva89FqVbBL5lJOBBbPVEY_wbegbtA6aFZnyd88frSvwX2PisvBbBvabHmHAkqZJhfO12vYZHPy0xo4iqumVqxPTSEz-qtnfeSL9L2axi--IoWC0gmcSO6arpdQBl54yzrZqCaydg3yFsd9hjfYdC9uqsgj8QbcIeopXAD71E-se_h9z3SbOKcUflDJnjRXHzguQi-K0dCUQH4c2xTAXql1aJVWSTfdomWn8mN7wzJG1UniBiA1LLQVMGzHB_yGcYjkrYpxOvSBYPt8F8JEz8j9CcLHMJeiqna84il-hwnrO5-_hgRBIErVAfs8mbL7GB4ydwtUzXMoLlAbKWBoR1UCC7VHwjGJk28KNA_evYYYAEmjL7IPmkecDd-bFJPfV2qU5Yl-YR5fZSgauEajTyv19rMAMsxGEzz5lTLkJj1pi5xiClT6uR0y4AgP7rfJz-iTOvmh1ADwEW_eOVg6IiFccN8Uxu-fK9doYFUTXcVOcVAH16JSArJbnFBWkkz7m5tXLXQX9mNAm38IvXe0vl96R0mgVvpXTMzCbTjJ7fje6fPpYsafwEYpds4Whl-Oai8MR0llV0uYRa5gPS0EGylxyb5l_5r7enmT6td6T7INXMc6lDAmJZoADaIOHnH-1A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_CzfxhuRiCDkt2MBTYFlJ8Y51YReytA8GUtAxGYyUGB-q8MapEQpj6Dp1zApgZhN6GulTqW6W-fi5Mdb4AsujqY,&b64e=1&sign=b618bbc8981b0a086e1f9a99bdb86603&keyno=1',
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
                    'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 494192821,
            },
            outletCount: 2,
            pickupCount: 0,
            localStoreCount: 2,
            phone: {
                number: '8 800 500-53-29',
                sanitized: '88005005329',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbnNgSTWltlSV-vcM82s2rlzi5zT6__WwG2rv0CfFbD67EeEawlsRbx5697ettYI1Y740IegvR5DmLPAQsm5vQPEEFjBu8u_TFV1lezHRrXTIk4tEO-mpXv6hF74-bVg-HoEhH5Sw4DGnQBuDfm_Spcnt0yDa-Wsxk3RaibYCSkfZxmYBn-bQ_w90_KDbHnxzPRufE5br16kifQuEKEM0t-HAkxceD0bl7vsNGEf0uo7HG4pClAc6qJd9EhbyX6TfeEPWuCvUEnUSj7QHdm_4iyxit71uSEfQ7L0TTxP3PjJOwisgwNxCaWT6iGwxkRuNFXjHjNbBGo-EQNOuFG9Hfpw0tFhRMSBGWQLn91-SSesfCw8f_rM7q5noB1l0mC8ZGZp6DwZ8kbITALdVX6QVtogJaKrcjNLjJi0TUnwAYnic4_f4DYHlHjS6zMKz6PxyQaJCsiShbLF0Wq_1aBsf3rNcp2hwv5hIrOukNoId5oxlsmsxcinTRbwqgg1hnvOQsNXJ3gkF4aHGQpPcY2QAb0iwDamrmGdt43xT3H0pepY7Uui5JHKktyh_CG9MLT4l9eQbiVU13C5Tamnkg70MTwYi6lskufbmm8Tirb5YFMJ2o3Z2CT5yBJiar4BxYgIn35WjIhAWtISYMlwvyMAdDva3glibKf9p0evz3H7mhefS1CL2D-AAaGzuSk6AS03ZA6QGXgl8kzAoSbHen0NwSeT7bpA7AJuV6NN749c0uxBeg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_OQERUebGnkiKHPWbrDR6MrPYeDFBtIvzbS0PE8qSjR6xdwr_Kh0t6kcykcmBqtYH6rjkj0rYC87XRJRHpheSDJ0pcJI6zCu-SgtIrPBeKc_joiUihS72JMFDSoesnm1mDgR2nnIANme4HcT3NR8JJOpRSxChBTc6P5uU_Zf6OQQ,,&b64e=1&sign=98033d2633bebe83e4e4110958b245a8&keyno=1',
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
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 3732918,
                name: 'GUESS',
                site: 'http://www.guesswatches.com',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id781654316530114328/orig',
                link: 'https://market.yandex.ru/brands--guess/3732918?pp=482&clid=2210590&distr_type=4',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            outlet: {
                id: '27486755',
                name: 'Салон оптики "Слепая курица" Москва ТРК "Афимолл Сити"',
                type: 'mixed',
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
                        'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                    outlets: [],
                },
                address: {
                    regionId: 213,
                    locality: 'Москва',
                    thoroughfare: 'Пресненская набережная',
                    premiseNumber: '2',
                    fullAddress: 'Москва, Пресненская набережная, д. 2',
                    geoPoint: {
                        coordinates: {
                            latitude: 55.74906,
                            longitude: 37.539347,
                        },
                    },
                },
                phones: [
                    {
                        number: '+7 (495) 120-3427',
                        sanitized: '74951203427',
                    },
                ],
                schedule: [
                    {
                        daysFrom: '7',
                        daysTill: '7',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '1',
                        daysTill: '1',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '2',
                        daysTill: '2',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '3',
                        daysTill: '3',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '4',
                        daysTill: '4',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '5',
                        daysTill: '5',
                        from: '10:00',
                        till: '23:00',
                    },
                    {
                        daysFrom: '6',
                        daysTill: '6',
                        from: '10:00',
                        till: '23:00',
                    },
                ],
            },
            link:
                'https://market.yandex.ru/offer/Gka30W292kXjFe6pV8FnMg?model_id=494192821&hid=7812201&pp=482&clid=2210590&distr_type=4&cpc=OKGXw_tnfhyF1GB5pB03E7qHo6o9-J-y6oDh8QNhHP4IFi5jA9CfeWjcJMhf3sL9pqrpUitxY3pU5BQEiDlsX_IvPnv-EuPHDvxr7bWuvszwBnKzuX4Xl3ypyTPCGeIntsUe4omMtr1gxBGDWrN-_f6N0kIzqpHr&lr=213',
            variationCount: 5,
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'CpaBuy',
                        name: 'MNA_DefaultOffer_2680',
                        value: '0.0902174',
                    },
                    {
                        tag: 'CpcClick',
                        name: 'MNA_HybridAuctionCpcCtr2430',
                        value: '0.299896',
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
            previewPhotos: [
                {
                    width: 158,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1577758/market_szPrToKUpEKDwHrxT-DFAA/120x160',
                },
            ],
            activeFilters: [
                {
                    id: '14871214',
                    name: 'Цвет товара',
                    type: 'PHOTO_PICKER',
                    subType: 'IMAGE_PICKER',
                    values: [
                        {
                            id: '14899090',
                            name: 'черный',
                            color: '#000000',
                        },
                    ],
                },
            ],
        },
        {
            id: 'yDpJekrrgZGNpLQTfBjKFJuqn_nWIzNZ0EDlvihTZ087_HG4SYQeDw',
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
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            urls: {
                482: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbkwumrzNw6pzqRB-TI_0Yrn0sEqeHVrauSbxKLTWtGdhR9N1Yn9LZV5ccn_jYipSjLF11_isAYk87zW-imHiYzWoPpV-3ikCs_UVVYJSgOq6iJz8v0SXNT5QzBWqBt_G1DFo6Rot-FF73LI1gqFFnyC9B1-DNd_23wrpFF36GAY1fx0Tm2YjMo1G5Gwp66c7NIyJleSHaKrYP7kOLEeAt7jSAsvtsO-vEZGnOzzHXlOVyjqLzt8ehyEmAgxOD_Fv58BdKrNsMolQDHP-tjW0h4lPT6BPk2cKlO8nVJDrsSmr99H6Z3XIvcickB8e1Zu_SHLXj6Sjp0EwlkzYsgrGcPHakVNfl4s7-qKEzCX6Nx7wzz0qRyRXApPTZGU6TnjQD9gaZAJNjdgkn7ibmD36mID0Qgb_c5FX4AgmqxVdXEkGLsl-WFRhhtYESfplO3SutVMaS_etBtuhnEFMrCGrFUYAXxT_5jjiW89gv-bSlK3EQNqZnGVnw4eaf7KbCzqkXeol2Q1dQEQ1vlIeTdA79JoKYSpjGwVJLjBTRfdFOs1gQlfYoeZ_hB-kq_-pRYfBrf-7ZtpRFXQjr_1khQ9JtV4eRm2goA5Aw7lhGEoKWaaZJyCksO7MoodzPj5ul1IgeLB7tiEtGx3_jlpuUyF0_nMau5CtEnncQOTx-Mu7xM6CHW334xhLpoYHvuJjHjqDflg92c8xaRJPOn5lUaCTAAMVKAg0sfXqDf-ZtbLN92-jA,,?data=QVyKqSPyGQwNvdoowNEPjejCEj4qgnn8_RqEyPeHz6w8r5TFgg1ESeu2ntWWXJhsQJf0vzh3ELqHmIOCmwLg0Mcy-bkzhhoOSzcvE7eXRBo5xViyGKfiEaZDwxx3xm9mIJKhe3s_Q-WPr1FPSlEpeOlWkOJeee5dnUfGuUDSp6uiDWbmTX6blcttvGTPuWHJFsqLQYgjeaTDckrtDNh4iTvvPjMOypUyccyvBWB1-J1uyMjMarQf8816TSEwhAJmINwBy8kjOPvqTEKicSFeP4nv0vgFIFdhtyvZ5INvCOqvaaRmk0wO5Jd6bnu03iG4q5MFSdLMpqpZnHVszehE5sZfnN_CGywncwsvXqDEUKl7DgNt5cyH7GO4IIqhQjArilslhjqluOMLp3SwsIBAlQ,,&b64e=1&sign=929d191ad0db281b4355609e03be2fd2&keyno=1',
            },
            directUrl:
                'https://slepayakurica.ru/sumki/sumka-zhenskaya-guess-hw-vg74-39060-black/?utm_source=market&utm_medium=cpc&utm_content=7885&deduplication_channel=market',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZnCMIP_BgY_pMYKSkOCUpFV2Vdz10QvW1-I5T_d21cXSSMp68RMdWuJv_cGK4qhRYHSOVxlUV7dyn1DvOeVzAxrlx5F136EeGglInY6zuYFNFyBbfjxYWPzukZCRxMB9j3UF9RV2vdhw-WlnkVDveN5OhrkKe4J4XdHkK2HouIxEOmYrk1SuWQ2ODeitzDRESSXgMIt7t2g1CcTchR3uuMgL23A1D6XJnblEMynvTWshcJcHIS-OlC5RgkNVTmraQwylskrVtT-NHhyjBY6n_3dqrlnd6K04gH7as2ZbT7R0RlYhkJlDuWr4NPM-g7B7h9iUrGpPd_Yng2RHszcMPxYMEAq2yGJJTlswv4Qi_qegzhke2KO42WIfQiT4KE0I0enyJdwwT4dsefvS1fr_NcKBewnYkBVsJ7mhlgJjD-ag7Q1yQQIRUxZ_jS_yMG-ky1r-46YIFN-I5kuU-hgGCUu7n7EFdAnwVspVP_DseHQR03pOQJ5zgjrGH3ueeI5aoj-i9JI9sQSfXqj-9_PiCeBFdwRvN_dDKBdP5E_l7KFjUDtGmv41D_9KMV6pSwJ1KxfkTdnenWK4dCgBUvKM-JR0zWFWLBAam11j_RRxVlgKY47SgZ46HDuxx2Z4z8UcrUCH_kjvn0D-1H4hWMbFq1rdbykxYOyRCSXrCGTR9csy1s8tQKzUrTi8xyLHSPgneHzCnjW8NOCHAhbtKoR2N9rqXzfJiLOXsFszg-0opILghki2U0jej4g,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_Mh10p3rkVEwce0QVWd9XunfvxEX_MDr10MhMvH3u0JecsH5Qw0O-JDZ0na_j2fFuSWenDqZv8XjwsAEAU5BJ9E,&b64e=1&sign=df71c3f91df5b34446c202e2e71a8877&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvPe64w_rNbu2rBK8db6B9nqtKFsvXcNxYVYhzXpFXkbJrNpBW_JTL9n9Gg0s9dFzTDgn6rPCpcIS9634GbygINVVx9KRijAJHewZEZ_l6F1ORHGUhsUhkshImUIO-0MdN10u9bXjdHDfJiD2sngrKfud-9FR_j1PpnI2-J3viL5ESyrFB7qddYaODN7DIirabvcRzDfTsf5gfvnFpj1DKZSihUHLaU1GXvp6LSIuPeI5_dVW9YLeXntIKqZ9HlH1Cu589kewjMYpsBJIe1XT0FEnhFF-cbqPYDBa0o6H5Y0HxvuegAWwZoMsp03qqBnH5rz-7bjTzpZSva89FqVbBL5lJOBBbPVEY_wbegbtA6aFZnyd88frSvwX2PisvBbBvabHmHAkqZJhfO12vYZHPy0xo4iqumVqxPTSEz-qtnfeSL9L2axi--IoWC0gmcSO6arpdQBl54yzrZqCaydg3yFsd9hjfYdC9uqsgj8QbcIeopXAD71E-se_h9z3SbOKcUflDJnjRXHzguQi-K0dCUQH4c2xTAXql1aJVWSTfdomWn8mN7wzJG1UniBiA1LLQVMGzHB_yGcYjkrYpxOvSBYPt8F8JEz8j9CcLHMJeiqna84il-hwnrO5-_hgRBIErVAfs8mbL7GB4ydwtUzXMoLlAbKWBoR1UCC7VHwjGJk28KNA_evYYYAEmjL7IPmkecDd-bFJPfV2qU5Yl-YR5fZSgauEajTyv19rMAMsxGEzz5lTLkJj1pi5xiClT6uR0y4AgP7rfJz-iTOvmh1ADwEW_eOVg6IiFccN8Uxu-fK9doYFUTXcVOcVAH16JSArJbnFBWkkz7m5tXLXQX9mNAm38IvXe0vl96R0mgVvpXTMzCbTjJ7fje6fPpYsafwEYpds4Whl-Oai8MR0llV0uYRa5gPS0EGylxyb5l_5r7enmT6td6T7INXMc6lDAmJZoADaIOHnH-1A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XhcJwKnmSxxvWGVWTgIG3rubEssb19f_CzfxhuRiCDkt2MBTYFlJ8Y51YReytA8GUtAxGYyUGB-q8MapEQpj6Dp1zApgZhN6GulTqW6W-fi5Mdb4AsujqY,&b64e=1&sign=b618bbc8981b0a086e1f9a99bdb86603&keyno=1',
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
                    'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 494192821,
            },
            outletCount: 2,
            pickupCount: 0,
            localStoreCount: 2,
            phone: {
                number: '8 800 500-53-29',
                sanitized: '88005005329',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZgJEIzMOISwSBV1KsJhQC5Ay6ugpW6Ul5zvb-ma3NKEf0yhheYQBQ8p9vXDW5zZTG_qgIvInTtoPoqFe-i2l6iNwIXApsZF1gfEXVBkUkOVLA1kpTDs_JFYjEzP_0IDbAh6JnZWTDU1Z69Liu2rTKGrcJZ0TwP0RgDQcGf7XRNuIjuY_X5QSqm8JAV3Cr24h-E-LLkB67o6gBggimDnNPbnNgSTWltlSV-vcM82s2rlzi5zT6__WwG2rv0CfFbD67EeEawlsRbx5697ettYI1Y740IegvR5DmLPAQsm5vQPEEFjBu8u_TFV1lezHRrXTIk4tEO-mpXv6hF74-bVg-HoEhH5Sw4DGnQBuDfm_Spcnt0yDa-Wsxk3RaibYCSkfZxmYBn-bQ_w90_KDbHnxzPRufE5br16kifQuEKEM0t-HAkxceD0bl7vsNGEf0uo7HG4pClAc6qJd9EhbyX6TfeEPWuCvUEnUSj7QHdm_4iyxit71uSEfQ7L0TTxP3PjJOwisgwNxCaWT6iGwxkRuNFXjHjNbBGo-EQNOuFG9Hfpw0tFhRMSBGWQLn91-SSesfCw8f_rM7q5noB1l0mC8ZGZp6DwZ8kbITALdVX6QVtogJaKrcjNLjJi0TUnwAYnic4_f4DYHlHjS6zMKz6PxyQaJCsiShbLF0Wq_1aBsf3rNcp2hwv5hIrOukNoId5oxlsmsxcinTRbwqgg1hnvOQsNXJ3gkF4aHGQpPcY2QAb0iwDamrmGdt43xT3H0pepY7Uui5JHKktyh_CG9MLT4l9eQbiVU13C5Tamnkg70MTwYi6lskufbmm8Tirb5YFMJ2o3Z2CT5yBJiar4BxYgIn35WjIhAWtISYMlwvyMAdDva3glibKf9p0evz3H7mhefS1CL2D-AAaGzuSk6AS03ZA6QGXgl8kzAoSbHen0NwSeT7bpA7AJuV6NN749c0uxBeg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_OQERUebGnkiKHPWbrDR6MrPYeDFBtIvzbS0PE8qSjR6xdwr_Kh0t6kcykcmBqtYH6rjkj0rYC87XRJRHpheSDJ0pcJI6zCu-SgtIrPBeKc_joiUihS72JMFDSoesnm1mDgR2nnIANme4HcT3NR8JJOpRSxChBTc6P5uU_Zf6OQQ,,&b64e=1&sign=98033d2633bebe83e4e4110958b245a8&keyno=1',
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
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'GRID',
            },
            vendor: {
                id: 3732918,
                name: 'GUESS',
                site: 'http://www.guesswatches.com',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id781654316530114328/orig',
                link: 'https://market.yandex.ru/brands--guess/3732918?pp=482&clid=2210590&distr_type=4',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            outlet: {
                id: '27486755',
                name: 'Салон оптики "Слепая курица" Москва ТРК "Афимолл Сити"',
                type: 'mixed',
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
                        'https://market.yandex.ru/shop--slepaia-kuritsa/476640/reviews?pp=482&clid=2210590&distr_type=4',
                    outlets: [],
                },
                address: {
                    regionId: 213,
                    locality: 'Москва',
                    thoroughfare: 'Пресненская набережная',
                    premiseNumber: '2',
                    fullAddress: 'Москва, Пресненская набережная, д. 2',
                    geoPoint: {
                        coordinates: {
                            latitude: 55.74906,
                            longitude: 37.539347,
                        },
                    },
                },
                phones: [
                    {
                        number: '+7 (495) 120-3427',
                        sanitized: '74951203427',
                    },
                ],
                schedule: [
                    {
                        daysFrom: '7',
                        daysTill: '7',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '1',
                        daysTill: '1',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '2',
                        daysTill: '2',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '3',
                        daysTill: '3',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '4',
                        daysTill: '4',
                        from: '10:00',
                        till: '22:00',
                    },
                    {
                        daysFrom: '5',
                        daysTill: '5',
                        from: '10:00',
                        till: '23:00',
                    },
                    {
                        daysFrom: '6',
                        daysTill: '6',
                        from: '10:00',
                        till: '23:00',
                    },
                ],
            },
            link:
                'https://market.yandex.ru/offer/Gka30W292kXjFe6pV8FnMg?model_id=494192821&hid=7812201&pp=482&clid=2210590&distr_type=4&cpc=OKGXw_tnfhyF1GB5pB03E7qHo6o9-J-y6oDh8QNhHP4IFi5jA9CfeWjcJMhf3sL9pqrpUitxY3pU5BQEiDlsX_IvPnv-EuPHDvxr7bWuvszwBnKzuX4Xl3ypyTPCGeIntsUe4omMtr1gxBGDWrN-_f6N0kIzqpHr&lr=213',
            variationCount: 5,
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'CpaBuy',
                        name: 'MNA_DefaultOffer_2680',
                        value: '0.0902174',
                    },
                    {
                        tag: 'CpcClick',
                        name: 'MNA_HybridAuctionCpcCtr2430',
                        value: '0.299896',
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
            previewPhotos: [
                {
                    width: 158,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1577758/market_szPrToKUpEKDwHrxT-DFAA/120x160',
                },
            ],
            activeFilters: [
                {
                    id: '14871214',
                    name: 'Цвет товара',
                    type: 'PHOTO_PICKER',
                    subType: 'IMAGE_PICKER',
                    values: [
                        {
                            id: '14899090',
                            name: 'черный',
                            color: '#000000',
                        },
                    ],
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
