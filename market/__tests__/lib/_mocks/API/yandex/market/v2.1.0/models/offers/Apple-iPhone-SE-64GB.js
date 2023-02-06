/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v2\.1\.0\/models\/13584123\/offers/;

const query = {};

const result = {
    comment: 'models/13584123',
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
                total: 1
            },
            processingOptions: {
                adult: false
            },
            id: '1516654323114/a5272a0646bb31d954235954e40ac13b',
            time: '2018-01-22T23: 52: 03.363+03: 00',
            marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4'
        },
        offers: [
            {
                id: 'yDpJekrrgZE_ZPb9iynT9eQYjZxb9LzWyaE-N7qVDN5iuL1VSkVIXBFeLRfOG3QMNAl9_XVmx_lEdk7wbFMgh6apixESjx8KHwCso3qaD3lgNuSTnNlmxX3eCP7f0qEvDozVFRSPd051Wkh12fI9rUpkqXE2hYNIPIoUEPtiknNizlCmvWGRZMcegK2Xeu9iA615G6ToFGiE5yscGfhfmA4G0yTA8MU7i28VjW9CvqEucaKQEQqRziNzySLVUo07hRrn3fwxEQ2SRJBBApFWhpRWCkXy9adiXImt3Rj-3xQ',
                wareMd5: '5Vy8D520U6SejpLO8bEX6A',
                name: 'Сотовый телефон Apple iPhone SE 64Gb (A1723) Silver',
                description: 'iOS 9 Тип корпуса классический Управление механические кнопки Тип SIM-карты nano SIM Количество SIM-карт 1 Вес 113 г Размеры (ШxВxТ) 58.6x123.8x7.6 мм Экран Тип экрана цветной IPS, сенсорный Тип сенсорного экрана мультитач, емкостный Диагональ 4 дюйм. Размер изображения 1136x640 Число пикселей на дюйм (PPI) 326 Автоматический поворот экрана есть Мультимедийные возможности Фотокамера 12 млн пикс., встроенная вспышка Функции камеры автофокус Диафрагма F/2.2 Запись видеороликов есть Макс.',
                price: {
                    value: '24989',
                    discount: '17',
                    base: '29990'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mU5nTBAevNZ1V2-RPrysyEU8eG8LYNMi5TBUoJx_qT4pM1kp2xkvX_Yu0pEPV3hkVRhVLyrffKy1SujNx6kAQjat6LNvf-HIf9ADGDJE-zlAzFcXNrlVtIhKGgSUeJoMdSloYDcN3HszS5rCv6tPBt6EGMdqIU73hO0M793PubblG-Tute-P_DRbNlT8IGFmptIolEz74iwETZMxmyRVpo-snYtUvwEf1_bLusP8taTG7CxWs-CxPXniKoLnjdAC4XTpQ-ySyTbZezDCXiY7OMbQfhA5pfipVELJch5nZpNWSUAWuae2vg7145NZvxdRTdIuOSCw8PDeIvyQXNvCOjxDCxsXZe1ZiqoOulkKAwsVhYUgeAxwn7nK0LWvxzrNKVTmHuaXJE3AL_1YN8uneAgYIkW9EqnTdKAP9ke8CEsJUbJfdlJIkUiI4Rhm93Y3tH_GEjY6QYH9HmIxmOZoQnu2GGFrDKepsY7Q33nVIGfSGXqmUW60caxJgtvBOgCaO1-O-DH63rOi9lEv0KQ4gCXr4gFIYRZizpSqxv41BqZc8-j4cScxRRnV-NI0uraRA736OpwUHsolxYIgWK40GqOwUCqIQekaAfqEPh-xul5m-ryFOj_bcJ0BL0XTL5juqAQkTupuR-xppT2sdTCfGmfrLOxrGGDLmRCcqWivyjeleLiZ2-qGZccMonxBx7VwrC0yURXiuuEjDmQ8HSewh2zpeA-wj666YOZAW7s3aCNPj6D0kjSQnZhCeNy8z0SGiJfzE-gEEZMGjdAe3SpJhp5hwriDPyNutPzXb7IUeM2ywsOOkC1My-AjfIcbOjNCe_Cp88b510tMqAd32SeD8hvB9mL_ib0frg,,?data=QVyKqSPyGQwNvdoowNEPjepPkxt1Vqcos9cWvPI22tg0wCjd75HgObocn-VkR-9ty9kgea7JlqOTwblPNbhRmYOk-VkKqKpb4elHvNXE3boWYYl22Sovbf2q80Qu9LaHm9KhGHIXoOrORCCysx2qMdmb1lFy_JkvI3T3qnL1PRuxpuHW0OcmM-8TeVDu1yiTttTfI3KtDh9iIvIl7G2uXdb-9DFsfECRdi1xk_ISrXo1W5zFOan9PdDchtwYuumYoHHmullgBbNrm0UnJyZh4h6A-4nXCJqSbYKS8-kWjLagaD0GeoaJ556J3fc8UAZLyFLk6lBgT8gwFpx-jKRzXybZzpb-oyI9zTPIXyFmiF3nFr56B15rPzGFIvZ9D479ieUW7BJiXkzLMpEA69txAmsE-6FizubrTHTRdfJjAeM,&b64e=1&sign=bc56bb7a864a9bc678ecf4d744635315&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRuJcQHw7yz67A17CYre6qdpN87Ck1lCf2wINnorVSpduTSMJQfqC09d8MKlCPbMDx8uKyk_D4JpoHFrgS9fVfnQ9LG2Nak9p353Fubx3f8-cX3sEKR0fNK-NiV-h1deyZ-igDlNZZfoE17roBbp4zDucKYiL8mw5eBeYuIhuqNhnqEV9C2X42OCqQHXhJ4FohLPIkv9QWpGPhhF7ZZ8pInL8pkS5JD-9rH9z_LHDA3PUBy63_qCqkiuSCAltJEDY0WlJYzOWTHGVfT5IFbZr1MxE383s5XXOu75_QaAtUzKJnE9V4A2XruCeZ9j6sHkKRS6vCpTCDXTsy5FyQmqNn9g3dxne6nXHRYOdZqrPwNq89OsSSPNeZJdkRbpCN5lZ9bXEk4IDEwNj4fVoFlVUhqMRHgrspEyhOxvqNrxruWsMAffeh9DZqQjaeM2g7-pgL-ochGJk9hgcvaK8V2PrpGgbG64c0Ia5Bgy_yVGw9CgtXx3Lri_Zqh-3IfaNn9k_K9ucGKPOhk8Q_hUAOrsiHeMch8E6xg3oXlfZYF274cQxTUQhDclL6JdGmrpSxTCahJJrGB8upjt3n5x-6jYIZl5xFqJfS2Ud0W67BeKzdUCiVT0-3031QA9QOtDhaNXJZmCOn_-Smzmz8WTWCseFPimz8UjtiAMNZibMH3_yHEUu0WiIkXkqtS1ZEpwaEQpZxOyjj5g0rgnIt6cvS7p2I7ioDJ_QGehvrq4YEfxjD9mbfk3Et1bkr_xqFi41STDctMBFAEAYzXvYTM2AB6Y8l9W6gpwamMYb7e9uI6EuGS8E9kwjMZ0XJkUm-aoEtpdkStsr7HtmMHHHMSAiH6XJLf_umfj1PJi7F2eBtToVaue5Q,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2WevCIXpjIc5u3bZ9aDdMPRD-Gd5VeIhtwhU1jWcz08MYHswn9lAojc4Va4_YpmJt572SiqjSQezqKWONNeWDAcyhi3dsRweRotLSpvVZPZn3ECFMEtlQwI,&b64e=1&sign=59e90dca80d5fbb509abbd757b94a0d1&keyno=1',
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
                        count: 6155,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 265,
                                percent: 4
                            },
                            {
                                value: 2,
                                count: 77,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 69,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 371,
                                percent: 6
                            },
                            {
                                value: 5,
                                count: 5374,
                                percent: 87
                            }
                        ]
                    },
                    id: 6537,
                    name: 'БОЛТУН.РУ',
                    domain: 'boltyn.ru',
                    registered: '2008-03-14',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Марксистская, дом 3, строение 1, офис 413, 109147',
                    opinionUrl: 'https://market.yandex.ru/shop/6537/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                phone: {
                    number: '+7(495)545-4227',
                    sanitized: '+74955454227',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mU5nTBAevNZ1V2-RPrysyEU8eG8LYNMi5TBUoJx_qT4pM1kp2xkvX_Yu0pEPV3hkVRhVLyrffKy1SujNx6kAQjat6LNvf-HIf9ADGDJE-zlAzFcXNrlVtIhKGgSUeJoMdSloYDcN3HszS5rCv6tPBt6EGMdqIU73hO0M793PubblG-Tute-P_DR9ct5zJWIOacLbcN_BtohfILuFKw_gtKLO1GquRnOqqn_wySOGJsX_9U4HNAguWWKUCd2vTdlrarthsW_5fNYIs0y8oBab3LiVTlpre63d5IwGs-Qrhmc4wJWZhGI-gVDCG0Xy-TbyoC0d0-TvvoTMBtI5AlXK7JRwSt8LYv_UUSYCaGCi5O1DKA0-60dzKzMHvGeeOxrUefRifOzmlkijNqPRc7VCTq1Y-CvyKWb7wz8otvJMdVfjLlAFCJFc4dh6oTq7PiX9CM6jeo8FmM2Trb4tskRu0V3UWHQxae8qOcmem802W4IzOq4jJ0nRj0z6XCCdgqGjPGhhhhn7aZ1Cumi45ZfpDkou_zNxRvGXKahj7hoknA1qCPw7M9eqDYuc8rMk67KwuaEaOlD-pSHVXzm-1vIqRs5G3K1PfcdVlj743szCoDdYHfOHrmJcZyDR21_fsWZTN-CkkZll2hdaPUDMkCxb9HG-3Yk0Uw2dFCg_pvlB7d2zzb2ZJ5oHFpKQ9FRvyhm0MQyFdQxVhdm3UO6TB2OIzgD99n1vAEIVStTj5IZUO3LT29WdX8sAJ1nzxTiwPNgmHIAceY-R5hbdL6UDy5TylcnMztcVqzoStqN7V7iffcVNOHITj3Jv_JgNDQQHu6I5WkWL_iRnWro9ppjU8fse0vOfEWQQNzSCGw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8EEdLNMAQxAV8FpSrWRnlHGIx9XpK8adYmoHqfJAKF8gjPTbkN86TUdBXLdEy5psaiqbpzsQhpEYxGrYu9WGKae3xAlvl_dMoGlydD6buNynfkat2Ex5QjxKM0rPSEDvjG5qmGX-hjBJxszKfGanNc_-m77R5w-3k1JhQCMf3SkQ,,&b64e=1&sign=6a891928b1eae1a03c7a3b9c98eba569&keyno=1'
                },
                photo: {
                    width: 246,
                    height: 373,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/167181/market_LtMPbl0BIpQ8y-Ckyur9SQ/orig'
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
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 399 руб., возможен самовывоз',
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
                            brief: 'до&nbsp;2 дней • 5 пунктов магазина',
                            outletCount: 5
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
                                    value: '399'
                                },
                                daysFrom: 2,
                                daysTo: 3
                            },
                            brief: '2-3 дня'
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
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/5Vy8D520U6SejpLO8bEX6A?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2Su4WkRaXrHduSWI3lmfyFi16wO39QWyodFeJhA0PIdHLVhdYcsjM2UR-6cRziThIA8sQxQZopDlvVguc_jhgcqThnID1X1WSA3l29jc8_xquJzQpcbT-D2GJfaqKr-47g%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgSLVv_43qd3B3UtHG8dgRsTLLZyt6rDCejcBikQT7RoTYLcISnVNEGV5Kt9LFLRb4YtJMPJ9jYn64Gv-5PLVUZlztkBE8yDpXLosCbaWNoEOMFj2efOPpTugo9d0mCAbspl66t4__32KQN6QVB6OkryJh8SQ9Wb5GEPwiS86rtl9khBlG8CeQxlKlBgXBgFs4Y8psF8WxLa5gTalbMXei-13InBrt1TSbyBg8C1Qzux22-poE36MzA2K9CeRlnVCAxZ8GJGO79KO4prthlQpvIAXQ7X4HOGCoU3AWFFpppN9wGb6vmVLpXXcFKC0QeEaM4URVbwiMDujkdnWkMkxKKCbY_u4ZLBEGW0-u5-rx8swG-uIqSUzBls0iXxKcxOeFq5mh-HLOrXuTVJDdCwwIjXmaw5Ndf96Ag4rGXz-imiCMh-Zds-ETnXPDE0Hjx84o6mSyIlH9sDRpC63UqyQr5GdF4KQMZkwt9VIwsJJ_V37pDL_fZFfSCNj9SCTqzW-vV0wIQ1y6b2CSt7tlXaP-1CWufXis69_5lHCdKjKI4cdYWfq3B26muYm9rbYxObMbuF4skjfT-8ozZDGZc2HGqZWFpwNxEL47YECMbqPbTN8nE9WZPNqE-Hge0r4UArcawoKD2If327odGt3Ie7232cnGDWTkH4FUsXekithUTuRTb95ZCwuubZCUYRg7npaskS6nTL1vuz5BejxBY67RvmqsB-nL92P8KbyzmmffJMAkv1uyjAW_gzBwXLBXT55b62xtM7sRv6k-Mgtq1Zca-Fu0tZw4d7WMIUo-0TT2mW8h0sNYZB0Q6yyAVHMkotInTY9Wskt4GJk_m0yHFbnsD_qVnc9oEdAp9EwXvX8Ye_MmJLkqFfwBeGuJqBnbdAQIw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-4ug6gqQXMwmLLaPALVZxgMHCCTOo52LfbYwzBykU2nE-H40MozVJnyd7xEJ2idnWh11dtlCiQkd4UjyCS0POOLbN1mUC2BKv8GAvTwEOxBFfJXHZac7rJPB1IRXJyZgGqYDRiP47_PPbbMKEYTWtlb3yf8DjNN2EcSuSnLM3wLH3N-cnYU0xP_AVkmDKspgdAPyKTLWEUyG8a5_WWHjdwY3Sg9CYFEYHD5qIMgTCT-A2P6RW9_WtwuC-DAcZ939V9cbcIA-9JsDwHCti1x4oypa35EOqnZAsEvLi1SaMFgWrS6w6Ae7vVT5gDofBWqnfLdc_0lD5C34g,,&b64e=1&sign=c849e5b5695ca5754144117ccfd8a593&keyno=1',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=6537',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 246,
                        height: 373,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/167181/market_LtMPbl0BIpQ8y-Ckyur9SQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 164,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/167181/market_LtMPbl0BIpQ8y-Ckyur9SQ/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEoohSPJiZjFaTELJQD6bVN8wGQSHV9qEOV_p5w3qGu89HXTjPSIPikM6bCBb9jTBtVGQo04fioxP3mxpFw6tcEieFbNBnt938pxSZw4RxE975msFDJcp1uWxH5k904zC98FJrZMVAwFDT-eDoPF0kZhov0K3w6as6TZ5iA059skexPB1_R_Q-UZDSpk_6x9xjUKgZEVXXSWegylLVFqR-rJ1yd6aN1GBHgiDbO7dKXDnBvKwog6ssiS21K7rppv6gz2evGUp3lQaYhYUlInTXpkssM4undSgM',
                wareMd5: 'DiE1do_ZyD7FdOqKi6uGdQ',
                name: 'Apple iPhone SE 64Gb Gold (A1723)',
                description: '«SE» в названии телефона означает «Special Edition», то есть «Специальная версия». 4-дюймовый iPhone SE рассчитан на пользователей, которым хочется получить компактный, но очень мощный девайс. Новинку можно сравнить с моделями Compact производства Sony. Миллионы фанатов Apple с нетерпением ожидали выхода современной версии «классического» iPhone 5s. Они, наконец, дождались. Дизайн модели почти 100% копирует легендарный дизайн «пятерки».',
                price: {
                    value: '24990'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mSM0RpjCpVmDQR05-4bX9uCKf0BV7PLXDg_Lo1TMjviqAQu2zdfIS78Pd0Vr42WQ99YbkCk-YHhj0zCG4WjLE0u80QhYuQiyYch0mYD8X1oYYaB38HBYU17sfsmFNuUaP0SDK5AeMWAI0BCZhG-YGZ8N9sompdieSaSo4Yu5e9bQd7j_BEggyiE6YxK0M6epFNMReN-W3Mqs8_gsm7xqXyEvgqhspeU8UjM0h9XwJUqQrTGRI5c1niB6nuQGVkwMb-CKFzT7FbW0QzP5cpN59Gb7ujT9qDpulmk3N1wWdnHooPDtYl_iPmQltmhvmBzNNJ2qji-OO2MhBuJhW6FHTrLfPt0A3oSwRipWLzDFnTyhL9hM7RC7hp_hCPmibHi5ujnUVYOX52y_YOIkO0RnkrqokkfHIl3KUbX98SYuV621FoVy2ueIg9L1xWvAJmdLAYZAZjUcsDRzcXt46tJ6N66dlvAc7FvEOBn2DmcYAwALDTb5hxJTUi3GCojfc4iDAcgs3-R2bIIJTQ_C6TOYVWHNB_9L6-V332OSHZ4a_bBgj5tYrAqvx0cQumazs5xNAbYnaJ9BYSjrTSxfl7uOqqrEJaYt4r_wpf3zc9SGi_UUOML2eQOYGvUvw4bZ-BCsIdZnxcwDrdK_xXEUQG7QiShR5wGs-evhAVw86vroHp_xGss0iQt6WkWzMP9y0BDZw4n2U3RMcNh9zo0JQK_hu3jttxj_dDg9S-wlTXSia-I2ySRscvrHrZBNmcWcFFjnAIW9Cm0L94pjfnsGnBXb60yUOtU13P5ZpxyQre9Haqc0W9M2bKTmXKffIhOuG7AwZZrlU10j3VFk3f0OkoFRrrc68WbwiEH60A,,?data=QVyKqSPyGQwwaFPWqjjgNq5ob6Jq70TJiGoeif5hpcbP8YccfY02ogWNdXvVXQnUtSG2m_TC7d2bPi2wXHRJMk1-9eyQ9C3OTTJWwFMvmQ01-BEw0MrTled0m_WVfjiSYJ62rves8ppfr5jGT0n0MC0_AZ5JGONj7cyI0xmWDfbRzIffhpRmspxbSSGI8jymTuqa5h7fxQntaHRkcGD0agwxS-lsiis8vHfx-nz9dmMgMBT286SvnIYj8C6lUAGLmHii7hqwT61OslzTbUGE33CJ5lEfMpKmqQPu9sawEloSQp7hAaCtOAOnupx83qhoGEfXX0XwrWpBlNyNWi1D1f8o1FYy9SrEGvrlcJNW_k0gzKVK6zXW3qHie2cmpRV_KEF2YcgADEKuQmLpjHGO-TXHIlwKlJ60GsfvtBUdMO0v9GiNVGSf7n0HIHgcEcQS0JatXSC2xqQKdHOYHFa20sktZgaDkZs2wN7mLWq-qou3GfYVHgCExg,,&b64e=1&sign=b58d1a20fb64c62276771f69600c60b5&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt3rmxb1rHsC9ABXfp76-ela1bcuKzSy7cU2Gpji_ihrxAmH0_J-SgG_YDnp3UgtqiUl34WS-JsEVgV0KIczEGmzUm-VtrQ7eorgOeWYRjpJ69_DtRi4AdEVHwBTJJNLJp2no36dR2qQTw80UdZrXXKOi9u0z2IW-yiNfWhSa9m6SbyH3PlJ-il4p8pjrkIZzTxBruAYC1WTs2Tr3yJ0reyEXOUSBNqZcv2ITg9Ryds5YjcpdjQ3PpzHOrAS9Xiq1TEy_b-G8fJQL_EdYbHANGq4GnyemG9EMp-bSSy8yzXwuIozcVIJxKqh_T-pxV6k0E77nTcS2JA-mRZmSpK6xGLMoBFjMy6nc_D0qaMW2SdpdVYf6RAyN0ZfX1pFcOPWSMlQ2eDWmHU3xjuWESmaEY11hQLJEnfYcfR-l0lBmcPZL2W3xsUd06PVUNaiTF9tUyMpP1eimoFSTbTAsC5BgEqnpo2JVaiW2ENeJ5mw7KuPzcY6ckpCTGbexNKGTwUTrzafrdFIlwHobrXt1nf0ienC_HO4R1cX-TCN4DWtoU6CQz6BZQOHUz7qWX4Ma6OfnHt_mlw-Zv5GkfqCDviiNn_LC2cSS3yuRkRzYl21Sjm97gNR2Mkj_eFZ0KUyV4OUvrJx1hgh4QjdwgMhfEg4sNZn3qhdrGenz31zNJVo7ortQCYfUoWr7-sh3dmASa3x6bYX0TxJc6bw_POKXbhuejfhGNYpTu9x4-AvlW1HviHf5LUXl52dMb-wLaVdc18CVdqlE2aBB5MUQVxZ9J0tJxVQ6HH1MZWlucqYW-NHEkv8YNRcFQY1q-k3BWXesHg_jvoD6Kfkl1YIhHg9gJOujf7ociMbWcvlKOl1ZTo88L23w,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YtduRh6BN2pHixvlE0VEivK1RYRTgNLzEOjbbrFiS4JceM5hSFPnTqMTrMyxWswOkm4OCnWshVIrEmdeLGtCTxYpHtTXkbTopBssO6HOKdmR4-GVDjg2lY,&b64e=1&sign=3412714165167760bef9128314abdc50&keyno=1',
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
                        count: 5007,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 160,
                                percent: 3
                            },
                            {
                                value: 2,
                                count: 56,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 63,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 378,
                                percent: 8
                            },
                            {
                                value: 5,
                                count: 4350,
                                percent: 87
                            }
                        ]
                    },
                    id: 262,
                    name: '1CLICK',
                    domain: 'www.1click.ru',
                    registered: '2001-10-15',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Садовнический проезд, дом 6, «Пассаж на Пятницкой», 1-й этаж, 4-й павильон, 115035',
                    opinionUrl: 'https://market.yandex.ru/shop/262/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                onStock: true,
                phone: {
                    number: '+7(499)990-77-77',
                    sanitized: '+74999907777',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mSM0RpjCpVmDQR05-4bX9uCKf0BV7PLXDg_Lo1TMjviqAQu2zdfIS78Pd0Vr42WQ99YbkCk-YHhj0zCG4WjLE0u80QhYuQiyYch0mYD8X1oYYaB38HBYU17sfsmFNuUaP0SDK5AeMWAI0BCZhG-YGZ8N9sompdieSaSo4Yu5e9bQd7j_BEggyiGVMdB1cxDrDexJL--WtG6_mVaevcLa_4BDTY-10FRu0K5ijmUKk1leU3lgHQGj_5Cn6mhs4el20Ff8mj0R9KI4-Qlt7ykH4FB0BPGWrizrvbazEy3JtzYi-sRCS9LMEHrcE-KQ1UPlhA7-LToF3RPioN7B_XuMWVEYjsUvuQzOVD6ttFDs90aexKWFTLfV1BOEUvqvpN2wXKt015ZAkv3azCWxUssa2G54RSdcjctxXjBF1YmaoFYUsHcP1DdQtKKqnkmXnvymvHMLoF2SIX5CTzQzTvoe4vFaMPqgrCQAuTY3iaiJtPjBcOYXXticI4tYS3uzzbkN_D6ROVHRg32FbjofnUDgpOYnBAVQhz-KiPbeejMSjdPCSDzDZ0JzJe3cKSJjucovVg1FoYrN5ygv_q9g81N3b3Fdi8T3ekUjs81QINpUMxaMbwT2D098Lxlsc1mT6kNXs-tKytgeVU8xfsUKNH8s5gtGsbE-cnbxwqwZSIvmvxwuYlfHfJgkN7-l1H-E70JIjsJ7sdusDzOLley0Cpp_DVzYv4rM59eJSaDrIrXGw_9jbmBgqGs-vlk8QQJtW4iB48Z65bRRulGuyhOyd6XvZdcz_cTXGoH40EW9kVix4BXpA0AmneW4rKF6as7y-eXvRSe8pBea0fL14XET5dSgv0FBYk6IoHEvzA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-pRl8l6wfTaB0iaihtSiBUBMwTBS6-ZdyipBBVnlrKqk6uxDL4nmGUbhrB_1g6rLerFwp-4TEB-3VYoqDE7t431xXemhUhPUVjUDJ5H_JJf9evd6nPm0Gx_wThhQuUpuRNsV65KH-7sSW_XCMtAhmzSqORx7bfbBP6ugLwTfm4Nw,,&b64e=1&sign=c2d6c9ffbc5b5472c76e2ea8006190b2&keyno=1'
                },
                photo: {
                    width: 295,
                    height: 350,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/230492/market_sHz7wlGGYko3PmuDKCY3hA/orig'
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
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                                    value: '0'
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
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/DiE1do_ZyD7FdOqKi6uGdQ?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2Qef2XEkZD6ObgsNZpxGMDzZw2NkjN4uVfeMgm3vnVr9z6upGV35yOGb4YrWKy6eB1oEedyUIxl2w2uzfGZR_WEaCi1mS2M4xFvjcC2Vmx-22HIH3k6s86n5NwbFk9ElKE%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgfSMe4d6bUvSO-I-MA4lJtKt93aSL0_unQdl5Wh4dzDQj9idnn7oqao13PfUJ3NDxFmqsLCIiiOjUyTxRmYrWFeXcV5Hir1Wnf31IdcOrcpRSzKZEclYxfmXoq8piPyyFKgK1DLmIVVZpviN931amcXroXwOHrBRiVYGEPAwl7kDibRTch9qNFIhUhfUI-A1GS8D8QDv0CIPXDJ3uLcOmw3dqIzP8R80qC-2UofjfCAC3HtQhanMHqq4-RRBu5ZPwRfxKVWVha-E6RGPJn8dXY_oPHi4gkfChOez_X91cYexTGDuO9TQ1Qfn5VQ5Xsf1lRPBBAM3-B3qHN_UQ6Y0uSzGaa19ldElHlwuj5qbIySWADJuxUc0RWyXXHVaHdJ2if8xxdpzXemWteBUkOMw18wPqqKbTntPv3Lv34obQ4zRMsri7Y8Sw-DWrf1EksgmB-xsJxGfqZIzs-mLm5DWhzBb0uX7_ACPM4pVrbDL-3ZfSAGG8w4mp2EGiqw0c4dZJ87Drhl7bb8_buICzWU2jW5o2Kg4fhhV74a53eS3PKu6hem0IMr7lqk_kC4IP6M7Z-LX3uXK38riZOrzdDlX65OEE42Ye-r2h8RXDojKM3yF6VwcYp7kFYPIJwtUV5J1a4bYr57DxKIHuUsEXi0nq_lbCeVt1gGnF2Qw3ZskV1YtycZ-cUQKkE5rVCzcYMhvcvoz-gwoZVUvs3tU16jNEf_bonmThxI8UreH2SQissIw13PrAYj2bLHs17ltOQ2R3XDinnlQ_uvnPBiJwVZoRdzJRphUnW73Xi0miXzPh--R5xZXSClnsSJ2XvTKJ9LtnRloGLFOZRV6PlySBd2tV1cids9-yFpg0VxVrBqfzt771w6dhHriUsC8bordni30PA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-57eCy49sOCSUZ1daZblqRWGA_cLVB4Rhnu6xMpW1ztoEQNesM4PBGJ4Z-JJgwM3hqbSyeyioTC-Vo76UHkA5mxr3HmwgpiBmKjyKhvB5dipFcCGkoZ5KKRGJ8LR2o-wr35_Q3O8_d5ja9H8oQ7wNuL1DbMXPSAZmMdX8PMmr4EcyO-FdC2hxbSwBsoZUS_7sqKJkZvm0btnBFEA5XXQBdyJtgRX3V5rjORUr1wSfQsueqd5cjkYGvtcqitvJD9s4lKavqeKUteOz3sQMGJKV-ODXY_fgMEvs2vKxPUzxYf7-MF8hxRKXdXVh1Cl4vC-hyXwas3bDV_XQ,,&b64e=1&sign=0d2eb37f80e7125e77c23b61d0554cdd&keyno=1',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=262',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 295,
                        height: 350,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/230492/market_sHz7wlGGYko3PmuDKCY3hA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 168,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/230492/market_sHz7wlGGYko3PmuDKCY3hA/200x200'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZHJZHjgW5L1MMm0U6iRYwm-lR4Fpt3UVOyMfe08lw3q1vfjfyxhbHlY6GD_vlBYHgVjdNt6WyxzdZltN01mQjwAfBAhzwdGAJezYOaRVNG9f53nOCbwkPwwQqXUR3vOEP3hhb5V_5Q1wfYMfQAA1oSGgMcxGJ8ybBfy7KAUlTJW0a6Q0SBwuYbuiRgndSt55xMilxM0JhGSn2KtF75xIxcgy6fXxpqbmaWae3DEhtStPelHdIhEyOCUo7j3El-u3jc98UIcd4OUXbjlL0tUNOGbzT-K1VMZL1U',
                wareMd5: 'qJDcaYI2IjouIxk2-qGiog',
                name: 'Мобильный телефон Apple iPhone SE 64Gb Rose Gold',
                description: 'Тип корпуса: классический | Тип сенсорного экрана: мультитач, емкостный | Функции камеры: автофокус | Аудио: MP3, AAC, WAV | Разъем для наушников: 3.5 мм | Спутниковая навигация: GPS/ГЛОНАСС | Диагональ: 4 дюйм. | Объем встроенной памяти: 64 Гб | Число пикселей на дюйм (PPI): 326 | Аккумулятор: несъемный | Размеры (ШxВxТ): 58.6x123.8x7.6 мм | Фронтальная камера: есть, 1.2 млн пикс.',
                price: {
                    value: '24480'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mUz8wgiS2_KMLNrDgZ2KpunF1NfG2RiQoqkgA-Ob6OOQX3mh6x7KGr1eKTJLLfNEVFNwQJf0OYODayy6fEkEs_D2fKc5fET0ASp6sVvyAGzSenFODLYiyLzAjve2KtPRZdg2PpQEFCJT0YSxSUxduSfdxx9UwfUJueNs8TVEPtJ1Jk0q_CopkhT57n_KevI-KmxjWZC-95W7lUi-aCT-aAZmay5bt3QCzsgjmCVRfxYRNv6Wd7O9Dp_6vn7KOrD5pIheCTi7cZn8Cjq3tYJNBd7apC3Znla_NAP730eZi2AHulMaDuHtnaHMAQN3KZlnDdpLHNeyL2XWgXr6zV2D7A02PVjpzGdY5BebmkKzWPo1TZGobk9PeSkke-mpJaOwZPi2lBzSy4dH7qHHtF7rtvd1r6Xxb6rg6WKLJ-gJHE53fiqHs8N33TeAeF0_nETnkSOGjm1ItJMU8g05yXlti9OJgpvppHXinJjJQ--751JcjguVP9SeBJmaqERcCHzJZ4V9dTLHY0oqIA9OdaB431lGlFqsnpwdbQSjgtvI6g5ToGMS6tutbPN8WeUz7-6m00CjQKwK6OOHk50JtBdplFgwmSo4pRcQ9gU3bQj8VDtpB8NbsLiCxNHsu6AYjEIMFdSxEOz4MaRsJ2-QdJZLMNMZZNWGpbwptCNsbgI1b0ge4Oc_mCDScm9P-MnKudnLfniv-cZ6-81MEoyTX35BpvxcvfSy-Hboub_j6zqPqKRQmlHb3IGgEdxRTLdn55bB_e6l1SkRmVXtbCzxtl0V0UZ6VKhKLHIOussLSC37qVLrvGFkRjNvI40KHRqgemKPOFuZs8CEbTOCTulPraXSvaMEue_jCarE6A,,?data=QVyKqSPyGQwwaFPWqjjgNg9J8lIJ2Oz9gk_rKEXyxA3xLCCap9MlHYLA9QmCyiEDOb9USHfWBqukiqjB5d1CJxeYsPsry0r7HNLe43liS8BPtnJZp7Pb7Xafx6vVulrKZhrMom7lkoUQxl-YJ-byAbUSIwfivGCldL77hVCxkK4vdswgpR_gQMlaB-vCLNzDp1s144xF-zL0Ip5cwhK5kzgeW6ISs4mgNc95iUHvf8k5l8KOlCDtvVgaK6e9o5KhYskQBT9g0MsMSyRL0n3jCaGh65xDXnlM_0yD-C8k-DNJmNcrZ70N8oGPNpjJjiE7zwmeuS4l92eRiTI-IXcQVbO_fp8WfB5feraX4YgKsYx3-cS6DL7ot48Yx_KsrtQGm6CIb9wCZuWgudVirEnNohkYc6cw8rVMkv6Yw6c-ZvZ7ERzWeuoiS-PpPpJOx4oMcBr4VFfXFHHTsb5lBiMTSom3XXMBvfW_dGXKtTPi1N0bkzBiTJ8T0E6aOvRAViCWE4Bft-0x2jpRm7I2pGNMvibL6lK20BQmhBgbADAnfmcX3HScTzZT_tV02NcICEHnEqAc2warFQfibvyEv36QvFiaeV2pe8Y7oyQn6q_HuKVUj0XD_ErLQY8ShoW48uRlzjGm3nz8Ev2XRyNj2Cyt0hH4mPLK40k_d4LT6Kvbzi7qtGiQsJqfSpyMAMc9vo3Mrg4mKJA8bDIPIT4mw5A3uW-JZsMQoUgA&b64e=1&sign=65bb88fcb4af0424063ed6acdd37cd7d&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRu4Y2f_crk7D3BAKE8SpemYMKmJR_QhGPZ_v57flysA0werlZoMqW1JTNJFuqrffy8K_SYbmgaLARqhVwEMwcJ7LmwtrBW-oWULod9CYSdiGIVSip2yrz9RdQEsJ9f4cMl4GVl5viti0dyItnPQrcVEWfXPHNnNIaPLTNJ_lZxat9Cm4WwC9hHbbNXZSd2ch-FMGXbU0ojGBrMr1Pj18_mVTBmPbblQHEi-88u31wMzT2v86tlH3FBxRZwvdnQu3RXRkbj27VVmm6TDzWLIxjSaPHL3b-BVsVxfebZ0d-zwiukIGnnJPBT6tCR3letu3prJnydzcX9r_VEZOZv8TSnkL23XvodPAOet7mm2LmQw52QA9bzAImMhLZUopa8vRJZbuA8cf_1cxrpx6i2FCfxeyVNl53MiHqAo7wPsFo3tJw-OFftaWKa35pzkAfelKlFBLpnjGHZSYiWQtmah99uuC7Yffnoltt_0TcrGvYzu4DJ2__7I4ExTWWkW9gJ7CZFvXPUMH7x8UAuSNsXBEyijoaXBqJQMFWOyeukWQNYOOlmo9H7vAB1aqRWvxTbEVJFXpKzRFoHwtOEG4zxbXsjCY99mIHoX917SdHidoeJ6BGbvAkC5MFAg2j_TY_66fIw8Jt9gLviNzoNlwd6VaHHhmBDBqVGwTGgXGLgYEhKMgokoF-Y-xjpHZ2UpL4VQpSvBkS88How9CSjc_BciFOXRftAUsS2CaExPagaPfE9pqrcmtUJzsrYlINg4wnSORa5dqkVLzTnCTg6nD8IGCqJ-PR5UZ463T7NTKrzM9yQZrKrxNSpb-ixF9Ki_75oN1cbbZL-Pc3N1DTLQgVei_SOrtwuYAtJOeAvVzoPFFs_x0A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bS9usFV6M-QGrPW7gb580ayJLCsP84ld6dkOmwWnJ17rzV-L9oSZByW_1MJbe1Zvhy_bKDdzXfcgm4WhuOFrgjY4kNjSOFAhJkr9PL54P6rXeC_FnPzmPM,&b64e=1&sign=dde6bd12c5c40c57be974f7929c0d727&keyno=1',
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
                        count: 3879,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 454,
                                percent: 12
                            },
                            {
                                value: 2,
                                count: 96,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 97,
                                percent: 3
                            },
                            {
                                value: 4,
                                count: 532,
                                percent: 14
                            },
                            {
                                value: 5,
                                count: 2701,
                                percent: 70
                            }
                        ]
                    },
                    id: 38615,
                    name: 'Mobilfunk.ru',
                    domain: 'mobilfunk.ru',
                    registered: '2010-05-18',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Шоссе Энтузиастов, дом 31, строение 40, У выхода из метро, слева от Вас будет «Торговый Центр 31». Заходите в ТЦ, и проходя его насквозь, Вы выходите на задний двор. Как выйдите во двор, увидите вывеску «ИНТЕРНЕТ МАГАЗИН МОБИЛЬНЫХ УСТРОЙСТВ И АКСЕССУАРОВ». Далее следуйте по указателям на вывеске., 111123',
                    opinionUrl: 'https://market.yandex.ru/shop/38615/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                onStock: true,
                photo: {
                    width: 484,
                    height: 484,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/250456/market_TG0s89BbigdbzxuQqRT-rw/orig'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 390 руб., возможен самовывоз',
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
                                daysTo: 1,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;завтра • 1 пункт магазина',
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
                                    value: '390'
                                },
                                daysFrom: 1,
                                daysTo: 2
                            },
                            brief: '1-2 дня'
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
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/qJDcaYI2IjouIxk2-qGiog?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2RG8WUl6YHGNJzpPW00oJxXiY7quzU4WCZbzMlmNeIz6t9YOVSoKp7FnWxEuZf6B8HDixPVmZU4GpcoWV0pyPQpr-A8MMVfSDAqA_BzTjl2CdZWx4CEbNL3owk6SVZi94M%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97NnHWpoDlF6OTQVhRVb-QlhiKezb_fiE3PJSD0SbC1NYaNf31NJMFN5OKjhnNU8JloX5jg-D4xLY9C2VUJYc5JOE-9nDCDg0Q_nCgeU2pgKvaID2aP8oC6DrP0Zd3csGsKtJIXRYn91GgR4ZSjyS4fpnps-8Aa_fyTQgvCarJ7ALDZ4qyVCXyFEbxlRGSYrPq_KeFgPxocGzU8d7J5lddRT4UeMJ8s6E3msxnloZlerufT6csYP6Duoy11XJtVa8MgkfhM3dOTfNK-v1gRFk2Lb6BHk3k4AItNhZcjx0VGoa95168y5grFlvTT8o-VImWyGX0QCO0R_yR_Qb2GItepkeuJ6Hq2HMRQiHA9KVGNyj9e6pP4dhV-QhZaTIngBgHnMyBkBvoBKP_XPvlX78WBr8F2i8lqPozVktNPtAVZc_rNsMI2PYpq371dPZ0FgaXHlsaaUPr8C-ut2n6ltmUbQR2Uk2Mv7afHOSDVHPOYhUbhOf9hBXIZ1LimU9T0AqkvUonPW2KF1KZD53_xAATE7iTEx4MO_gYRsWFVIfBR850gSRJ01n0wcuYSEC323Imb4jHc9y-AAuhfEq8m-WWh3HrZG23e6jGXpZinTo81lGrjShXC3dIGypwE4f8rI2F67dJA-XBp7DaT-suY7K198pW3VwP3i3E1UPh7vsIjaA88BhNDLHIhkNe_mAityLmfeLcEqAU1g1?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsX9qAF9dpoHbEADsiN2QdOj3RqhvlAqw8gpNd1PcH2HoJh1MvepAIILRp4odHr9MhJCsHckXwnXCN3hjRBBlt6e_wgO-hxgcXbqTrplxdcLlAY7djmDp6OfbxCtGCwDE_5OsjCWns9592x9JCeKvbfNWdWrA9SSVUTHU9f2VQwJHrdzul2IYR_i3eI-PdxkGc0SUuw6aA09x-zbfiF-vBk3HWqB8ZpQmDWjQO60w9pJZUD1vKXy3rHTpyvo0XixTHwdEPGDy1JTad30U_NuYqch7jQK7iuNER-lye-KHsHlsGJSDHV8nFGGg,,&b64e=1&sign=54946f87b8c60d5f0ccd766174c5a41f&keyno=1',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=38615',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 484,
                        height: 484,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/250456/market_TG0s89BbigdbzxuQqRT-rw/orig'
                    },
                    {
                        width: 400,
                        height: 400,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/223477/market_ycdnoW6eyUs8ooV3FWF1bw/orig'
                    },
                    {
                        width: 400,
                        height: 400,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/362766/market_qmcDEmsad_h6nGUrSJJOsA/orig'
                    },
                    {
                        width: 302,
                        height: 567,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/247272/market_UcX9mVdXpMuCOXaFzRnurg/orig'
                    },
                    {
                        width: 1046,
                        height: 568,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/176166/market_KFElJsWvtEh2C2og5vJi_A/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/250456/market_TG0s89BbigdbzxuQqRT-rw/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/223477/market_ycdnoW6eyUs8ooV3FWF1bw/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/362766/market_qmcDEmsad_h6nGUrSJJOsA/120x160'
                    },
                    {
                        width: 133,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/247272/market_UcX9mVdXpMuCOXaFzRnurg/190x250'
                    },
                    {
                        width: 190,
                        height: 103,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/176166/market_KFElJsWvtEh2C2og5vJi_A/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZHhpLQrPyL817Y7bVCuPkC-pnxnPrx7uZfjmHhHvhA79PtYqkp4J6rWK_IzauKFwA72JnEjRUORzO4t9LiwTXY2G6BM660Z0GFgLmekAIEvlFobbdCiIJHYvtDsQR1CVTW2o-qnmGQeYst1xvtAEJLl8a7msvKXzbUV7gvKr-tAcEnkxtfIgqnvqZsh6lyeJ0i_DMwr3DWWYSeIMcIBOMJsfkKnSoa6NYm4nsb1Tb8qJCJ9bJqbK3mY2Il3njPL6N2lUI1LIe8ikLEnLJtCnC8MP3lzK5pzMjI',
                wareMd5: 'zsnjbboT8UC-dIcb9b5TDQ',
                name: 'Мобильный телефон Apple iPhone SE 64Gb Gold',
                description: 'Тип корпуса: классический | Тип сенсорного экрана: мультитач, емкостный | Функции камеры: автофокус | Аудио: MP3, AAC, WAV | Разъем для наушников: 3.5 мм | Спутниковая навигация: GPS/ГЛОНАСС | Диагональ: 4 дюйм. | Объем встроенной памяти: 64 Гб | Число пикселей на дюйм (PPI): 326 | Аккумулятор: несъемный | Размеры (ШxВxТ): 58.6x123.8x7.6 мм | Фронтальная камера: есть, 1.2 млн пикс.',
                price: {
                    value: '24480'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mUz8wgiS2_KMLNrDgZ2KpunF1NfG2RiQoqkgA-Ob6OOQX3mh6x7KGr1eKTJLLfNEVFNwQJf0OYODayy6fEkEs_D2fKc5fET0ASp6sVvyAGzSenFODLYiyLzAjve2KtPRZdg2PpQEFCJT0YSxSUxduSfdxx9UwfUJueNs8TVEPtJ1Jk0q_CopkhTuu__irLvbauDywKoJ7xObEgRfQ3LBcXPODN2Dla1y91NpAYWx-1WjBODwCTo-7WSJGEKA2jUfJxhwRgCthme0X8ozrMbmbZNTII6kFqrlej_AOwMAxVbqVgLyeisM8n8KjA4XKVIpxmuf3VbvHK_jIpwtXpR8y3U93jQn_gs6peO2xqZGQ0_sWZEYH7UJZw1r3v6Irm8umMl71GASpSQJVgr5ktWqUkcsHriigkF5PrH8HT7H84i4jkuy6TX9x5VwylNCSyCMYL0VGE68EquF7o44_d4a6BQcDvCTa3YHFwU17XojGxGLTGNkrIY-Sl44aVEqWEBW-TMkJNvERFMEQkt2ughd8jdzuWtrGtB2HhnTIR_JIH2y5BPzvohZ99tVZSfzTcPKax5IFWPUE8-Bgis6IiPVchgCryNIN09e47Wvp_OIvt1fZnJnIAqXvpkzTkUQTnsWsmxl5PRxlBNdYkPKPlvzBxL2reCV2Yl-QlktodwgNxMMVHNtqMMJX9dw_DlHqW4bmEJCBLLS42QbsV2wJZBJ5FVk1xeqgfK4_7AeHeqZ8N9Rvac00l_3dq82MTiikoxB7Z1_qSeDsMkpxZVGIYCOs2-Sio7Rfw9Svse3vexMpgQ9mwP61brw4pXgPvYVCgREh6as2FQH_uZSUvtVumroCAl2lJfTaLuauQ,,?data=QVyKqSPyGQwwaFPWqjjgNg9J8lIJ2Oz9gk_rKEXyxA3xLCCap9MlHYLA9QmCyiEDOb9USHfWBqukiqjB5d1CJxeYsPsry0r7HNLe43liS8BPtnJZp7Pb7Xafx6vVulrKZhrMom7lkoUQxl-YJ-byAbUSIwfivGCldL77hVCxkK4vdswgpR_gQMlaB-vCLNzDlr0kJb_WCkhiVU1oEjH5MmxGzsLuSm3rqWRXcurpNU-1zmKgoBjfmcarKaf4x5yv7uhI55oZ75DeOzhIOUp0i8HL9MqfLnGssOmYaoGjy5DZJKb71sLrwcNGFZWy1765UJNpiGOoyywhRyFlXrcrrdhGQX0ZM9l_xaeLCq5UhZqVtvSDTKtJISjdTGJvC-Q_6JQNXIz8V6D8fnA7STtqdHyzir9tj5_DK69bVB3X2Da7qtjpZzq-2oknD8HUs_eGvvVan1FGUXFP7Q8FC-K6awP6rSVe85Roj_767NMGUZ1bWMi8652cJafk0m6exc4_Dk5OHSxf9pthS1DwfODxsZzYRBYVRim0j3Ksa1myE9wMpuFxsZjGHDr17cIp33veT4xRnK0wRe_xeqSvCx-6rqku9KNBO6hhUKU1I6yI5yuqeqrNbWqR51NbNqb_VY8Pan-AO11pDHaQKbm5IpNSawoDKgMi4oMBswqeFc9NF8lHNq87E-4Zbx_xlmR_-fjiiwHqYy1y_BKKFlcHsVj40g,,&b64e=1&sign=c49e9487b297af14759ee620cdc75c0b&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRu4Y2f_crk7D3BAKE8SpemYMKmJR_QhGPZ_v57flysA0werlZoMqW1JTNJFuqrffy8K_SYbmgaLARqhVwEMwcJ7LmwtrBW-oWULod9CYSdiGIVSip2yrz9RdQEsJ9f4cMl4GVl5viti0dyItnPQrcVEWfXPHNnNIaPLTNJ_lZxat9Cm4WwC9hHbbNXZSd2ch-HlSDbuvLOn3PU5hItRqAUxcOsUkHqSz7yaHQsrLFWloBY8FJMcMMBBd0v9ubJNtxpyy58w0ofwi1MbD2m9aLdRxvhfgKLCUKhrSS_gNazJql13yBJcaH9MpMH-B-qvN2VJh3H3Hi5Uu16YtYBO4kKGXxfnwgiLx_eRfLN2ccXNH7Qehw2te46bggk3t_GVuEQLoGKd6fBlc09rapJXMuaJ8dqZFSWgNUmFegUzKaOIUyVggyfL3xzx1kYef2ySLHxA3NPmGLdUDUVz8BEGdBmZ6FX8gm5483fCDTwZcU04oJvbsr-wKsggnzrvMiBqp9Nh0cqm4wnsZmpydQR-bz05wJRxudTRv_5QSRvGi-zfwuCF5SIOW4SZgAGkravwKeRcQPb6mqKFg5e-Q7QmOnhRo3RgGybe1R0RMwDwxAYX-nLlT_H721CWEcBQmmr7WfPduT32FNpVYEIicfj3HTs3yxm--y21X5ZVsLTtpwRjsk06sxZc__g3rI2KDUdSNot_CGRfLZg5IycX6TzvPvBhnuuPRWacD7Ud2pkNKoR90amTe0H-IUcBWq9UwkdwXbOZn8-FM7BjmrXfeNNP-e3LWLyYWiyzxYrhFMPyzbwhcvhBT8N_mm6OJcMPHaLDXh8HQ0zKJIZOBNHMLWmNkDo2rh_iTJS54mKg7Vb4Zlt8VA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bS9usFV6M-QGrPW7gb580ayJLCsP84ld6dkOmwWnJ17rzV-L9oSZBw-Ej826kIAZT4U6YyMhXk1AK4UtGhchcw5cdVM-recNTctxiTykSQwIkBug19nybE,&b64e=1&sign=86b1bcc6d805b9ea583059ca443ac08f&keyno=1',
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
                        count: 3879,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 454,
                                percent: 12
                            },
                            {
                                value: 2,
                                count: 96,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 97,
                                percent: 3
                            },
                            {
                                value: 4,
                                count: 532,
                                percent: 14
                            },
                            {
                                value: 5,
                                count: 2701,
                                percent: 70
                            }
                        ]
                    },
                    id: 38615,
                    name: 'Mobilfunk.ru',
                    domain: 'mobilfunk.ru',
                    registered: '2010-05-18',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Шоссе Энтузиастов, дом 31, строение 40, У выхода из метро, слева от Вас будет «Торговый Центр 31». Заходите в ТЦ, и проходя его насквозь, Вы выходите на задний двор. Как выйдите во двор, увидите вывеску «ИНТЕРНЕТ МАГАЗИН МОБИЛЬНЫХ УСТРОЙСТВ И АКСЕССУАРОВ». Далее следуйте по указателям на вывеске., 111123',
                    opinionUrl: 'https://market.yandex.ru/shop/38615/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                onStock: true,
                photo: {
                    width: 320,
                    height: 320,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/331110/market_X1iWnCeN6lBRRnRoxyb-sw/orig'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 390 руб., возможен самовывоз',
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
                                daysTo: 1,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;завтра • 1 пункт магазина',
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
                                    value: '390'
                                },
                                daysFrom: 1,
                                daysTo: 2
                            },
                            brief: '1-2 дня'
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
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/zsnjbboT8UC-dIcb9b5TDQ?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2QGGIIIyejG2Bvv8GpM_pa_MuLHl9CAYTBNVdA91-Dz0Jo2_mzFeR33yABe2W4MUQSa86DYeFxXlipGvZevvaKwf-PhVaI32t2uZuxiEDzDwLvccA1TJiay2i8iaTu34lQ%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97NnHWpoDlF6OTQVhRVb-QlgoWKzKKrfcE2TzZAytFinp57zGOaDETTHX_cAvZoIKobYKLwxmpYPbaiG3jgFt0FriND0BSdcxhjpVz54Q6TJj3qKkRDBVKsYnsp1gnkLNjpepex4kWoGMf5UiXSw-y1yHRpbG0LIq0O1cl9PE80RVOnmj0dgox3IIE0WMyKcK3KebuMEqlpWH5fZ7c_ip9szbddKMBfg6jiYy_qPnw-sgayQYCeg0KZWgrxILhdk64bsMexAkK7XphfuSoVoEInTEsOvv0Zwi0ZZqBZBbqTHJ6F_lmjrE8mr0quBesji-MwkryH59ERfAMcrZ0ynjyty7SRFSLfHZb-PhsRGyIptxKF9pBzqZCa_Hweh4cHLrWr9m0XI_G1sUzjKPbdB2FMbF1uZgUcWloYARAXa0jBFq8b6IvnXvtRnljjEA12I7rP3E7oNDlv5VGJkTg1F38yjqkisolNT83eboiIMvMuaEI3zr81hz6gLGqPh1TYY95QgDuo2t_SujjKmb_VvgJbWkYxwwyi2gwFxBvj4fkb0ut3STNT8NLRIjMDRqnWCxQ8a2HjlM7uaTKsYRC2rtfu8U5Q2g1bLZlmI1lefKfYv6sFl4-8hKSNSY2aswPlTigj9A0EUFM2VrPJdYFKFCOFSC-Ud-lSnYwkBYvaFOt8P8WehfnTmYIlSySvFuMJxJI_5eKe5ZiKCN?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsX9qAF9dpoHbEADsiN2QdOj3RqhvlAqw8gpNd1PcH2HoJNlcVwXWYZg-plEn-rZ71kWqme2c2DRu6LzzUP4wowZPdrEmu6RGHsuxjya6e3wroft7BJpx-IxTIXpXKkUxHJAYy0IVy8PRfLxdjrpDn10BtSlPPwkqfX7c7pNe08jWz5YrWl9MSGu1Xw1rrxPqLbIbnaXCSdjDHicSdGut45uwAWtpT1bay1ClkT9c2q_wQJACm7ojasNRX2TW9DwYlG4JsVvLI_LfPaBFWHzqoZoR1Kegkjshsow_Qu7JmoN8KnCbHnmeST9Q,,&b64e=1&sign=47945615f696732d1fd9dc21b5c271b9&keyno=1',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=38615',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 320,
                        height: 320,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/331110/market_X1iWnCeN6lBRRnRoxyb-sw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/331110/market_X1iWnCeN6lBRRnRoxyb-sw/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEH3k02tkFS08MHx6tQtvdcxRGD2Uigyn1OAx0ZlWQbD8xMVDkebSTvRKWqQxuuAuJpRuIVE9aTWZB10pnVIP47_aTF3_HxOjl6n0GzOPsronGCGAVLclS3LKsz_-4Y1pca6SGEBpYE8Mg8EII49nKVcI1-hgEmDK-mq4QOGivp2wwAiIN6Nt6sP6tiSmM_GlshN4ip0mvCmFktoROoIcBx-JLCf4swPrWneurd00w-WSByDrrQQW1VWXLeyptXKR03eubcwWd9eCPLABLHlRdCzEXEio1JFK8',
                wareMd5: 'ry8BI_OLmSTLdpF27v2oyA',
                name: 'Мобильный телефон Apple iPhone SE 64Gb Silver',
                description: 'Тип корпуса: классический | Тип сенсорного экрана: мультитач, емкостный | Функции камеры: автофокус | Аудио: MP3, AAC, WAV | Разъем для наушников: 3.5 мм | Спутниковая навигация: GPS/ГЛОНАСС | Диагональ: 4 дюйм. | Объем встроенной памяти: 64 Гб | Число пикселей на дюйм (PPI): 326 | Аккумулятор: несъемный | Размеры (ШxВxТ): 58.6x123.8x7.6 мм | Фронтальная камера: есть, 1.2 млн пикс.',
                price: {
                    value: '24480'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ3K2Z5qmepr_vPH9hBQaNT37Xpin8Ivwg06RO0qcysUAo428kz5RCGp-yCpu1mgxT0sRsMo6CNCdhopwRa7CcPXPDDpH2lY30CVw3MdDlnEVv6fQ0n0iIJqm55wKMmfHmvJvYxggtkycVNzX7HP6hwdzSDZDqNX4w9Z-MluxQ-GGAni6oHkYUJqP1jUkN51f11MQa7ib57Qt8Zq8h1ygYk96gM1d0IYbP8Oz47qNNuoDU06VbyoMIhJ9C5noQt8GQOIwuPqqdi5RgK1vkV-G3vE-6hnV5JZZ8mr5qocoL4C1xK3GfKpEFRIcuNLxqGNkN_PiHTG7coY0aRaqq32Zz2du19QDi-SKvpTxt0pac4yUf8lgVpRC5QVbiXpFz00e7SI8Wjn4dqme5ZAoYsB23wuGrXfvrAgYmaKOjIyAOszYEFDJWfabMo5Jd6gRYsvOoVpEpyUYQR2wWKxHnCSyREcsi-amhTdU6WuZ0YpW_JWzqoaSvE8YfBqbNbKR2xNri5lEvnSscJs28xBCHpPAnMqkYdVddSxQMN-93zTMSnU9v0E7I3OnJfffh-OgR79isE8cQLX6vgpsP5Qd6PTI4kD3WP_JHotFGohNqeeureGXNkXjZF-mVBoXb_o6A40l4PIBt_0mS0Q8Ul204bqN0WvguHgie4gmujY1zps0Bfeqw,,?data=QVyKqSPyGQwwaFPWqjjgNg9J8lIJ2Oz9gk_rKEXyxA3xLCCap9MlHYLA9QmCyiEDOb9USHfWBqukiqjB5d1CJxeYsPsry0r7HNLe43liS8BPtnJZp7Pb7Xafx6vVulrKZhrMom7lkoUQxl-YJ-byAbUSIwfivGCldL77hVCxkK4vdswgpR_gQMlaB-vCLNzDf5_5UGXuig65Lvg2HyBekGp2zcZMx63Q6pycfJtil3GqBKqmddZDXVZVTprUjs3p7nmSx4jM2ZN7of9HsbzDXiEmlsvm9JGcJ4lxzjGSH78rH6XDUnxWbGOrioaxp3H3WFm4mA4V4NiJOl3dsDNizjeVQyPIDh0ds9qprMTMkGn9RDla0AUmeYt9vdlA7ZMTbezBiLZfuP0Np83voMHT8q2Zaf0cmkHJyR-PxgpI0wJggXqbK2qYW843ZYW5Yzmv_2-YF7ShUlCeQBTgsMElPpp0gIEnc7xwH9xM8M8n1uZw_-MzGYlHupAiq2edquo2nV_soIIU_JdoBZh0JsgselZZNh4i7adju8Lffh28xXvYVbvbMDDEgvWCYurAh4ZRR9vBxVZT5S05oTV5xJypJOXCsdvBi9uhqqkF1dARNgWUbpm6fP1mOHWD9QVDl0K53xMrz00oHw6r7NSFcVKnkH3CG-rveh_zhwYfBlbfn9yC1KG1_5QHN8HMeq3uA9okQoyjgvzu5XCpXW0ujIOLnA,,&b64e=1&sign=6d41a1dc51bbadb2f43fb01b88481149&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVL74jCRl2732D8LkfebSn32A-hl5Kwa9nrSjHmOkdB2Y8ZYNVNND7XM89u7fBg2vxu0CFX7WGqo4WwR_Hv8_0lVqp7okEtgnZ3zKp2WcilxJRXGsnFv2HGCCEWpSLlYC-lkwn9fYxU5Ba4fMNHd4ZswiGTSweKU3DsXs6glWweykyqI0gPFXwowP2aTkplh04EMv8L6gN1O8bYEpDQxihRwtgnrIijApj4eilkAxMx59iPLzLbGqNfj7ApP4rxsJpd9EUcz6VqquSp_YKsW9hQXstsW948KvRQ9xET20x5dAEJVuFmMzUNUIKXhmkzDX_A2rCkCOv2my62fRo9MuaOCwRScHfKyweNI7999BpsxhmV632Xl1Br43hgpjZlpgp9rzgNqwWpCKFquC77gfcTttEDTL-ZVWv2IRwWJMDvDIo25D-QKq5jHvWrOJzbC8-8aM5nHguBwXdJAbfK1501UTA30FXSZ5VbYAFtrtb0uck3Zgwh5YwTMfKASYJNdtPsHAXp9O0K6-sFzTQFqY9uxR31OyEUAmc80q3xfTONLnO5aixhZzCPoiUe7WMgz3wN1ghLGdPAmKVZmCblDeXmDmSItJ7IND_plN-Wu1oM6tf6F5OtNJ__xJgGQnGUdy6GISez9xwFRWxuuoB1Uf-vfacmd2g92G3oWg2ENZok4rUeyrO7zA83gmCJnwPRW-m0gHVI-iUK3acMCCeBkO7DLa1Ls2ruw1eXv88yp2jafIUVDf7Hk-gOKVHEeeuQpofpAYo77m9xeJZ2TiquVCPLgBpOMUFb789ZHDJxKgww8fo2krxbxOq7xAWNcK2fo3v3T_J5r5YD0hWg563asEvJ_vk33T3EAvOQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bS9usFV6M-QGrPW7gb580ayJLCsP84ld6dkOmwWnJ17rzV-L9oSZBwTzcju0wLp1yjlQgah3lsK98WQYVLub4OwW4VDOzl97GE__b-l-_Lty9wwKeJ-Nnk,&b64e=1&sign=1b952cb5aea066766b546dd9ba487f07&keyno=1',
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
                        count: 3879,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 454,
                                percent: 12
                            },
                            {
                                value: 2,
                                count: 96,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 97,
                                percent: 3
                            },
                            {
                                value: 4,
                                count: 532,
                                percent: 14
                            },
                            {
                                value: 5,
                                count: 2701,
                                percent: 70
                            }
                        ]
                    },
                    id: 38615,
                    name: 'Mobilfunk.ru',
                    domain: 'mobilfunk.ru',
                    registered: '2010-05-18',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Шоссе Энтузиастов, дом 31, строение 40, У выхода из метро, слева от Вас будет «Торговый Центр 31». Заходите в ТЦ, и проходя его насквозь, Вы выходите на задний двор. Как выйдите во двор, увидите вывеску «ИНТЕРНЕТ МАГАЗИН МОБИЛЬНЫХ УСТРОЙСТВ И АКСЕССУАРОВ». Далее следуйте по указателям на вывеске., 111123',
                    opinionUrl: 'https://market.yandex.ru/shop/38615/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                onStock: true,
                photo: {
                    width: 330,
                    height: 316,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/219360/market_vnoByxgUVlNHap-edJkRFg/orig'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 390 руб., возможен самовывоз',
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
                                daysTo: 1,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;завтра • 1 пункт магазина',
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
                                    value: '390'
                                },
                                daysFrom: 1,
                                daysTo: 2
                            },
                            brief: '1-2 дня'
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
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/ry8BI_OLmSTLdpF27v2oyA?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2Skzxscbq55sa6MhAOc-QWVA47r8eJEcEMHDRp0L31hcWs4IqbUq81K7Faeh_vfArcCd3fYKt72ZrAKI1KuUlfVLIADYaQ2wEzHu9EfdRjTatz0VjQnOdRFsSxQbxvW0TU%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97NnHWpoDlF6OTQVhRVb-QljIl-8zjTTqZBfdgis5q9jcSRxUqPhodxEpmVniX6NQKanKNWmCLhKvgZUi3idBlO7tL7OKJAU5kb3dG5h6syI1YG0sImpvBq1b1L8-qS2uHRME4PWQLdIVdwysjzEKrzml1R0M2VzBowhTpbq404hr-raXZdmKMD5Bt28Hon6XzZY2Ehf4jRvECtEWUafUFuEJs6NLbh1rONkghlC7GoWz8IIEX5ekf0DypXTmun2iDCo8H2uh1RxN-7vI5wFWZMLY2h679ckSk84QkcedQpPTPWP5YAVkFQ9O6lUdjGnNsTFs7B4lqPt6rTxzDdOba8wd6rBElC0yoFiQEaQbPGVE6fX4Ge07Mw7VWo_RUX-YHDDi1_M5GZNIlXktucdAA1F7V-95aWvXIKkHZIu9WvJw_ud3Fn9X4NGrXPt-k6dmuWDQC5pmhk5sgQxA93YYdE3YIjnvkQGsy-aPdg6pG8KGdc3LD8FvAglWyMBtN5TagYCWq2WlxXFR2xSte3w1KG9K5ZQ9akw8ZkScJRwmnk5cF2FLtOmVHbwqnfdAy3u_VA_MyE41i_dYCCEuneOgoezMjALa_lfkYVm8orvLxFojorfXBIyX9YF5Gmf38XEJx2Wb6ZmmKVHEAvRf7sqmY9r_IFHeTMqPsXm-9-vLnop68x9v1mKddUz-OAq8jEake-j9L13TgnKy?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsX9qAF9dpoHbEADsiN2QdOj3RqhvlAqw8gpNd1PcH2HoJCt09ylRKGozYE24MfePxUwpwDPv0XFlfnTZhzbMPXF-0Ot9Fj86VcxVYWo45UyjYdkBUuSvg327XKSR_ycrkSnDrEZaREYq6AX1y3sBDQCajcImeGaRGJrEbxFuVfvoBTp34WpD8YCV_B2PkyFKw7RGvzX5LgfdpTRnGazL9YI_tx63c9PCvXezdnYXT-BlJ-l0m83nOr28g3EMe0uYYyWckYQIol0RMyQgDK26on6HCKcxGpDWvVPRFXjG1bZn7H_C-5cWJCJA,,&b64e=1&sign=3e9acda67271a0fb320ad96215e30436&keyno=1',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=38615',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 330,
                        height: 316,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/219360/market_vnoByxgUVlNHap-edJkRFg/orig'
                    },
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/486815/market_M3I3u0GJM1GNzBsb6n7vBA/orig'
                    },
                    {
                        width: 800,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168221/market_33wIp9OTqXi0b5ccyp4Z2A/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 181,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/219360/market_vnoByxgUVlNHap-edJkRFg/190x250'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/486815/market_M3I3u0GJM1GNzBsb6n7vBA/120x160'
                    },
                    {
                        width: 190,
                        height: 142,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168221/market_33wIp9OTqXi0b5ccyp4Z2A/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEsIuDO8TypEkR9xljiJNvRcIav2udWGbIOF0eC16yp_Q',
                wareMd5: 'fM_XSG94y86-mc48HgYh6A',
                name: 'Apple iPhone SE 64Gb Silver',
                description: '',
                price: {
                    value: '33990'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ0k--D8eCoMeKz179a0flZIcmHYgDfdGE-vwmy4_JjsWh-VT8PjlvRNCOYPf5jAs4HD5W0EU0p-VPIP56dn7j8vdF7ujggmv_z9WZtlwfvIBzsacn7ynS5-ztkxLm8Z9n5ipuupA4R9lSEkPHjqs_EmRGpWNW0HwmC9D-vYeFTSCKWh3xK9uqAWtWa_A4uoys09_nY_KRZbMJBy7lat6V6jP-I5fV14gr_Y_BZZJLV7ykMkjzBM-kHi3Y2FlnGUhIHUBbaGgJrrfCxIpguNoFanxrW3mTlTDOPxoIZhLgCkS4b8ZZfdas1ty3c0R4uB6a69HKoW-liOkWkn5xLGsSh1bPKmvy1FAV1EH7BbLpkHQimxHiUnR3_nURDpMHaU8XjZ2fAYEyO7BjNkzPGeIbCHs3eQLA_a7x8Yf3PCvuKcxDIICjR5vFoeSpkttorWt6JVZhSlysvYvwpfmlR0BF-bv9HeaIIlI1YXotknjlsVHYHVKn7iFjgzbt-HUBj2K3j7silLcSg2UeyLmQDz_EsvMIg5ZMbMkaYmyEhJVNTOxA0MUGs0FDo9Y2_L08_xy9Q7Y_V-UIgMIwfgGeFGuxXREcuP_mTviI0IJ_yQc9lb2gjogj7w3QIxmJxiksNhX4ffqNCyqhvXUJJ6P0s1c8iMByqf7Uwl8Lgrtkrb71mPEw,,?data=QVyKqSPyGQwNvdoowNEPjQoU2CiJ2kh8q6CahX4QNdfUG5in7tF9eddnvy3FW5ZA7IW8rgTe7lq6mVUOatIyshYZN7UzAsqcUTIgTPjpsk6Ub4kWtTQzKEV6YzhvYTIYZTFS4yttsHvjL46jwC8PU_KN--9MfOvM3xmyEWdeGcnEtv_OSkmP9sV5G93rXibhxKPUf4FK0HcIYPBTIV2M0cWsT_etnrD7NvncHSWYiWfBiFQwXdiy7iGTdCmjdQCEmXvOvJVRc8lrygI1JBL0Bs3HDaeOvgkwyysBLjFkr2aTTYcOea2zy1NNyxbfHxmKkQUnXRghbJOAuuYudiy5V5lYVgGUzRt9nkXv_fY41Ke7rEe0bn5XjBLxUsQf2AC1I7vbxLNGwG7rpTXklvZgow,,&b64e=1&sign=cce5f2bcc74066a0a121f385a0544459&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVL74jCRl2732D8LkfebSn32A-hl5Kwa9nrSjHmOkdB2Y8ZYNVNND7XM89u7fBg2vxu0CFX7WGqo4WwR_Hv8_0lVqp7okEtgnZ3zKp2WcilxJRXGsnFv2HGCCEWpSLlYC-lkwn9fYxU5Ba4fMNHd4ZswiGTSweKU3DsXs6glWweykyqI0gPFXwozwaiXbak4mq9acxzI783wVBj7a9M7gVNwt1KDwtFNMhTI89o7avyNAnB2NCYir59JpDsQTDNF8qB14sLc_CDWVutzCD5JwcDa6KQ-3I7HJQx7x2TW2Zjexj0rbiX5Zh8Mf5E1um1-XzFuuu19eYTCpDfUo8cSQPTa7wDucecEvI48J4fPmehVSJTtTeBCPvHnvcOQJ_Z8uH2IEl4Bei8EGjLo3-IR7rquJu1VmVcjolt6-mfwq-LijGqE21OdmKEKMUx4rM-RkCyV4ZZqWy7HTMZ6Tr6ShhpqFvRPzD7fDqVOSRVbttVEAGy3Bhgo7giU2s4GE7lV84f08yml6wwiNvI9UEznp1Rx8BtUchvjGC0ZNzrddmaUFVGf6m6OBOWpoydkwERw97NzjGZw6QAI7QYOKpTmEHhuyUe5jMNR9a7PFdWY11_0-1LWtooYuBE9fSQsdZq4hxFHeOwYjs9e5SWIOW_2o4AhXDoUzX9ibrSlW05wyzBSuGz5suxpZXoZYRDa9DPMUL7tTj_R3UYxuboaUmGZ4BeD2NKES7nHrl7h2UAygOqRzFlLRJMjLpdgmnPGw8_k0uA2jEqBX5eIhUbD-XC1jswzY4n3Mv2pb935nHlTOQxOb65AIHaYtCWozU5zP_UYts79MlZP6nlLArzuslMefG4fRKTRSBsso2w,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2e74MoAVqw7BcGXvC7jsB-dFyDEsTYAZ0csnQPCAPOZnPnSmtTDkGqTOs6Dm42hm4WcNvV7_Z44uKxvOUzPpqAU_4swO8aA0mPYl2ShfhMDOKQg0GsuY64w,&b64e=1&sign=3b218b988c07313f24494f71ede33528&keyno=1',
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
                        count: 2722,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 514,
                                percent: 19
                            },
                            {
                                value: 2,
                                count: 86,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 104,
                                percent: 4
                            },
                            {
                                value: 4,
                                count: 303,
                                percent: 11
                            },
                            {
                                value: 5,
                                count: 1715,
                                percent: 63
                            }
                        ]
                    },
                    id: 955,
                    name: 'ФОТОТЕХНИКА',
                    domain: 'foto-market.ru',
                    registered: '2004-06-03',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, площадь киевского вокзала, дом 2, 121059',
                    opinionUrl: 'https://market.yandex.ru/shop/955/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                onStock: true,
                photo: {
                    width: 300,
                    height: 300,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/225310/market_xAD_zcC2eWWdY8bfuIaYTw/orig'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 399 руб., возможен самовывоз',
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
                                daysTo: 3,
                                orderBefore: 24
                            },
                            brief: '1-3 дня • 1 пункт магазина',
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
                                    value: '399'
                                },
                                daysFrom: 0,
                                daysTo: 2
                            },
                            brief: 'до&nbsp;2 дней'
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
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/fM_XSG94y86-mc48HgYh6A?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2RmkxWthU91YCayZ-AS0TIPnuJtZo7Ung-WniEDUEYhbO_f_lpgKeF2jRiAbzVWDaZOyiyT2AO32HG1RZzsZQ7LQwK5rsrS4U2tTFEnZ2UBJh9X24zX1GTp&lr=213',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=955',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 300,
                        height: 300,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/225310/market_xAD_zcC2eWWdY8bfuIaYTw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/225310/market_xAD_zcC2eWWdY8bfuIaYTw/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFNreN8PzivVIULyY87XMrlISSBEswhV_nZhPhoxidxTw',
                wareMd5: 'tZ3YHTMSYU4eGhU_seWVUA',
                name: 'Apple iPhone SE 64GB Silver',
                description: 'Apple iPhone SE 64GB Silver Эргономичность и мощность Айфон SE полностью идентичен своей предыдущей модели – iPhone 5S. Единственное внешнее отличие — это наличие цвета «розовое золото». Однако не стоит разочаровываться, потому что начинка у iPhone SE идентична с модель iPhone 6S. Стоит только взять айфон SE в руки и вы почувствуете на сколько он удобный, размеры у iPhone SE позволяют в совершенстве пользоваться им одной рукой.',
                price: {
                    value: '30500'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ3qYkMI_9BhEqK85bJu8EG2wGrVobnBxsVsFVxuAWuhlugQ_9HLcvCAmB3dvKDbNiHU05jU8wFR5k5shtO4ZUzTv1-z8O_WQm3AS5Cl5XatgHLmzsxZVYG7WdjEvEBJfNiRsGf5YWsWIYTJdMYn9yMtu6UvkwGXCphODzkcOHIAq_rT6TxshBoG4VDlc-vYdTTmcKvlOjr78f4XPZ7wC7_x8RVTJuxD7OSLrrs6ahQh0eEI9ZnP6veq_5SNEj9pq4P96LbItGtlXRWUC7z6Xci2xQLtQ9-eKqwdbYv6CdHHCap-5Rw00XnVNOy8tVXFkin8y747anQnbi2XVGMBcDE4XenlZEgKFokn3Jd6bQZfUoJ89ePhGVfMFx4CYY2iH_3Q8HDKk0U1VbPNbgyB-J8ExN5SH4cm-W9JH0iYR7ZDj1kqAqejXZz3Nra6qbkXkamAf0kXncCfv0FSGrdlniulwJ0DkD5jREovOuFQHQLu3czQuBnVhP6dD4liDofB6n74D-LK0kk8E56SKkdJ6xAKk7LiNfsaAeoNpxthe3ToV6g7Z3XkPmG9L1823xuLwi15-eR9447Vy7RI_yrZf5I4MoXfeM4hpvpsrnkqyJWz6cc_2-ePvSHnbGSivgeF0pQZU7U5Qa2FsPXPZirkx5GzGLqjOg9LCENmqS00F_ucSw,,?data=QVyKqSPyGQwwaFPWqjjgNkzRjoOstLIO6IRK5gdDYhT07Y_eei45F3cETnPsMdv6wDwpUqDOrnIVrq-mYTcxlGbW_v96F1ybVzcFjlCGXHtJjKbCgiUy-Obnq_h9hI8TZnA4XLSYOUW-1-f5JGu7xjCJ8HFeW-2YSUjZLb1KiOWCC6ROvivkUIHcoWC2Jcq__aBZ8vggNu79aPimcl90djJUUKePgzepfuazEUEfYlM,&b64e=1&sign=845af7f15667842f39e1f86e3d26fafa&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVL74jCRl2732D8LkfebSn32A-hl5Kwa9nrSjHmOkdB2Y8ZYNVNND7XM89u7fBg2vxu0CFX7WGqo4WwR_Hv8_0lVqp7okEtgnZ3zKp2WcilxJRXGsnFv2HGCCEWpSLlYC-lkwn9fYxU5Ba4fMNHd4ZswiGTSweKU3DsXs6glWweykyqI0gPFXwoyRS3l6oy12hVmxBrPt-odnxxeYOYAGozsC37lqCSEPwZpR0PsvNa_nLLy465KUKTmUF8K1l5Ipa3R90zUE_LmYzBn98_IPwjEnAbPIL0lcwyiq9jFBqtmAUrLFoiGC6Ed0icGUXno9X9ii8iZsiK31mZEI4XkMkZUP1qwOguNxvkGuz1xkvrKtUSDIdq-XSCrekKFigRboZE103sk3ZRXI6j9rxxEgMa9S0PPrZrG9QBtRXvCNFyWvLEdmlsUZOwCux-UQK7E-9xXKhW_ncQecDfNlUG31CFQegwbcdXr6KHJjhZfu7YpptsMN4HXcECxS8vOXNvbNqmPXoAHDjf5qy66UZ5YkHp3rzoxUCrppSY2I1MOki9iCypB3czam3eRj5pAh_3vt8SE1YF2BovXHHczirzcYw90zNVx5f_6OPZHh-dKtpGmPqead0QyuYzBGw4X88Gd2BpjLyb_J4Ym0kYZc8lgxQyvz8-c0ju2PMNOwENRkULIw9KlxLsr5Ex3ROgKK7NgbyQgYrhgyKtZ1IlPXxlObewbQc_8sx-7P6fE3ryeLxHt2ebC8lVl8bbU_VSqxEBaIqW_-TyvrZjiLM_ZUcXgQXbbLYUh54UR3nv2qb5sBWI9fTDpUVLD94A0q_h-6kDCTxy6FmtiTW9eFuY2R6FPIH20kxT1UG0N0Fw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2apgzDshmROWj4cQmlpSFZExa8kTOj9PM8CPO7sv8uyEBS7YiqALT10qBbrPlkWz4WW-AJaTZBehcP2QQK0uS2Zuehxn6ifnvPE288eDR1hi0Gv_qI95w4w,&b64e=1&sign=bf30ad2186c7106f5ed5b1299b462fc1&keyno=1',
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
                        count: 301,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 9,
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
                                count: 8,
                                percent: 3
                            },
                            {
                                value: 5,
                                count: 280,
                                percent: 93
                            }
                        ]
                    },
                    id: 304604,
                    name: 'MrDroid.ru',
                    domain: 'mrdroid.ru',
                    registered: '2015-08-17',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/304604/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                onStock: true,
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/212340/market_hGYpflGs6QSVv_ZJ3Kbc_g/orig'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
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
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/tZ3YHTMSYU4eGhU_seWVUA?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2RdT5DHFWXF1nEjRz_NwmRY3xCbgMPGuFJ-G3GdoSC6_vRyeiWFoTCJuHrskLq-Z5ChWmcmkAeR7kEZDfuDqtwzsB5fN6yUNaky46X3RhyAq5ctN4n8383y&lr=213',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=304604',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/212340/market_hGYpflGs6QSVv_ZJ3Kbc_g/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/212340/market_hGYpflGs6QSVv_ZJ3Kbc_g/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZH_gNmuUF13OJCXvhy6Q_LReOEnrsb5pkXjiqiKtO9xfoMjCSLpwAHrQOyGD2dNZJOR7_aXw11qOMn-iGpaL5is6cTrgpKYvuF4fovuEpWBaelg8vgH83bF0c1qUFF7Z7JWgrwiSHWpkvq-Vrz6f9uEmoSK31dnhJg8epeIR-9iZYMeKMbdtLQbs9aWdcCf3Z46xpxCANRg1mDyAdl80OgM4kT7a0J2ypXkQBGnqFoq8jpdwSmGVWu9R-BG-8Rjg7GjfjFBIBL7ugisSogVLVhjKMQ5RvO2xPw',
                wareMd5: 'Ryud3HEeVa3nQRQz4uT1Gw',
                name: 'Смартфон Apple iPhone SE 64Gb Silver (A1723)',
                description: 'Малая форма в своей лучшей форме. Представляем iPhone SE — самый мощный 4?дюймовый смартфон в истории. Чтобы создать его, мы взяли за основу полюбившийся дизайн и полностью поменяли содержание. Установили тот же передовой процессор A9, что и на iPhone 6s, и камеру 12 Мп для съёмки невероятных фотографий и видео 4K. А благодаря Live Photos любой ваш снимок буквально оживёт. Результат? Небольшой iPhone с огромными возможностями. Дизайн Любимый дизайн. И новые причины его полюбить.',
                price: {
                    value: '24390'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ3oRJ88EEQV4nB2JDwcJV2uIj03yInIm2ThHrkuE5wMWO9SJ8oR_CfIAHzCMHG0aPi_q0ifsjDGVAKYhtWRm6d503BYup27k00PIoV3Rdfbm_wZZTB19vUH7iAO2wCGVrEmP-DM5Oh3thVmreZiXq7GPK_T5OGKVZbInkwgHjjlQEWV18p-ruFodEXBIEQ9I8L1qkAZS4kmUiDZF8QJGX4AfpkmBpox-RZoiMDWMZbbLc-IYsMZG_EhHa460kkl67RuSO0VPIGv1Fx3_2g0wWAVU9bgcgVJTyJnhgqaqnwygPN8eNQXpYKfibYZkNtW35KDYbl-vhH8uzZu38buPXOr3mqrrk2Fl3xknfJDMj1v2W2_ST3lr2XTI3cgNvRtgE7Eel-PFb7BnRT8C0m9p94iNCQzvccRKPa3cgQgDn0jTP35FH1PZUkMSIfXea92XhxjOAeoShvijtL8GEJUUrTh3IrlbpzXeKbi8SWxpdoEB2ceeQLBC4Emq2SIkhlW9_gC0mEf2huwcGk1MnNe_qehzHR_ZnYOXlXrug2fgNG7_5AqX6EwGqV5hvEnWpXTr4iUKwNaXC5ZDCVJ-zBKUUFnnEii82hBjACXozA5XLtQdw0ENynpIkMy6NcJ-2Uw2gF2fjrxShVHonAyPX8LCdcv9KPgRwW4LrdA36dpqZQhmQ,,?data=QVyKqSPyGQwwaFPWqjjgNkvrW6IDRVeCS9U2yzp7SIufwwMLTzstXVrPYXsS8a7Vi1aKCbjNbck_HBLDpdde7DTaXslPLbe7qzPWY2OZM2nlZUzPTc8OKoJ9TaQqHFSAUrbkaib8ItsJZ2Dhvgh8dcVT3GRvjlntX23OPdv4fb5v-eBf1gr0XQqG0n_JQp57FW55ZxFteqhSxPuiCx2ky9LVQ0hYDAiLB0wk5ZzgjuaEK1M-PG2uTxgawh3uK9jO&b64e=1&sign=4bb66301b6e2fae77231f271e9a1d8c9&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVL74jCRl2732D8LkfebSn32A-hl5Kwa9nrSjHmOkdB2Y8ZYNVNND7XM89u7fBg2vxu0CFX7WGqo4WwR_Hv8_0lVqp7okEtgnZ3zKp2WcilxJRXGsnFv2HGCCEWpSLlYC-lkwn9fYxU5Ba4fMNHd4ZswiGTSweKU3DsXs6glWweykyqI0gPFXwowwcG9DOEV3bEA6Hf3ZwmSYWa51zv9h8ufnxbL0puKaJBjney_zow-c23uuIgaVxI_ZlOItinHGm6aWdS-8KftaNpxoAWrMAlFu-3nbKKdHocHeDWzF9niYH4OKfljZz9hmQUw8fTm-QeqmDe1167-KfanI3LM6toszQGjEphbLdczhaQw4LlUIiSBjbaI3GD9FqXAAdQVfVxmVtHDJ8OdKut_q025r9fckHPWE87IHN-vvMblvFfrkKIakTeVOtRWOVaCod496QCkhD5Gj2Lx2JCMnOm8qifR5d8VJcDd0Vv_25_t4p_or41UwIDOxEkJVkL5Hovx32NXZANe417MdRZk0MacfIitjlMebwMyqGU1DyzZNwS6RmqHc8yKRS-CkETmpBTyScFYSFHUunC79L1Bqc4BIq-lloCrrwJS-o4Id3DQ5_17fyGd0kPS4HvYl5S4yLNr9ZvzVLyfviYQjIum6RPxcNtXOsyzLxe6c8p4NoFI6bE6e_qE3KLX8qTfin1KP1HtQBwQxbw2lvmdhgHkDaHfQz6cxlCTVRBFEBJ79AA5F1RqgHsE3F2MLeLmQ_iqgYMGxSh_eqJozwCx8lc79qprIikzPsoktDrwTB5RX5chedzlaL0KFPommvcdgMW0Erj01ab1v-1q_aw3EwpCUQ8QT-ly0mCklxvxeTQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2TN8CrNhws8QWgcz6ay8Ox-zIglLDfJWbwD4lSUdB6cXZDMXhnv1mBlWnMKiAs8wapsHkZIZpIfRHcbIU6dFF4mWMqxziCuY8fXYT6vdiwrpClx6FsLcTW4,&b64e=1&sign=d998df6591aefee77ec0b2a1188bd4f6&keyno=1',
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
                        count: 1303,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 68,
                                percent: 5
                            },
                            {
                                value: 2,
                                count: 23,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 29,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 96,
                                percent: 7
                            },
                            {
                                value: 5,
                                count: 1086,
                                percent: 83
                            }
                        ]
                    },
                    id: 29295,
                    name: 'NaviToys.Ru',
                    domain: 'navitoys.ru',
                    registered: '2009-11-23',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/29295/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                onStock: true,
                photo: {
                    width: 1200,
                    height: 1200,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_RnSRR1-VOgfW6hZ7abwsFQ/orig'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/Ryud3HEeVa3nQRQz4uT1Gw?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2SLrnnMIdR6knhw1OKw0DnBjGnjDy5tQQZxDZkjfa8MZ1oBJStI-qYwZE5bBahDhd-SEIUDso5rNZs-BtzM_0Z7wbTOUdURppKrY12MM54opzq_ubPF6gB6&lr=213',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=29295',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 1200,
                        height: 1200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_RnSRR1-VOgfW6hZ7abwsFQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_RnSRR1-VOgfW6hZ7abwsFQ/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFLpwT8yLuu0w0kwJJCI-I6MLLdDPaePBdlUPevuF-Jpw',
                wareMd5: 'bQ0xpvL5M6nFc6wW9g_aVQ',
                name: 'Apple iPhone SE 64Gb Rose Gold',
                description: '',
                price: {
                    value: '33990'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ0YuBs0q72-vYxB1v-1xGBALgTZql3DH25flOxZsbs0-rt3n2BUCTMfAJt8AlEBMFpRx-Guq2H8Zon-hlMvF-pfkL59BaotH9GW72_xw1mbchZoUhz4sV9Q604ckSlm4euNOtQV3C36SgDDFJjDueQkQJRSXC3NRuHjxe3Y3kaVHE0rwFz_8-kq3eK7rxdf1d4JViEEFtGTiy-3N6dBk_oXE1nAYDL3gWvr2BynzN5y6XqmuDW02tQ-Jeneme_AiZDMIOUb4L6YjK5DXBc9EdHEVk8CUMeKovXZbEm6tGTn3c8ic1wlg3FOkGtTzaiNtTd7aZoDz9YQnmwoGrqkGZGTDqouEue4WqVDlEia7zwr_8tEtV15wpkI6jGDY4MOMi2y0hN66-S5mhjG30k9kAbxRk7dMaLx60t7GbIgZrGWWZFouNpO0AidWnL7LAU5qajs-r9Ck9mezwkiZuMzVp2-zut25ggH5VVgpJ3E3wL2gKQhSl2S2s75xb6DzLHlGec0GzdMINvfMbfQz_JRcBUSnchZQfsKpyUD5XGdTuVZLOjmZ9CC1MQCNeJAMMgnXFvp0mDPvlj7HHzLJ0GLwqBDwZCXhRDpdjDTi6gZMyFRvOtQtNXPV1Je7kpuX6yGmgraIfOV5cqzz9cLt38isFAbex3aetlPY7g2UMQLO4wVyg,,?data=QVyKqSPyGQwNvdoowNEPjQoU2CiJ2kh8q6CahX4QNdfUG5in7tF9eddnvy3FW5ZA7IW8rgTe7lq6mVUOatIyshYZN7UzAsqcNaQHX_r42HxhARqHoqbbr-_8OFaM4c5_J0v5t3EOhA0VSPpjqj0SxS2jJOonYPVVwlbvlycwT0q-NE3vlTDAhpM3YX101QpKu9DmMDkaz3sGiPkZINlDccdUhf4QhfhtPdebYnNR-co63OhM2SdNv9TRnpK_zsLAhNhI-Pj6l0MbYMPriCIF9HQ5xCtXD6VrsK2Du6w_pLL3tzU7Tn9vhHfky0gHXNlqhpWfUtnTsUz8y9W_yH-6cTHhoxtE_v5mBJCXO5YYgVgLM-xOz5hWuC3Ev_yUZ_Aurd_-Eicl-l6pANNje1PkX3cJHBl5dpr9&b64e=1&sign=fe8f87b22da4c265a840d1c778b4e306&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVL74jCRl2732D8LkfebSn32A-hl5Kwa9nrSjHmOkdB2Y8ZYNVNND7XM89u7fBg2vxu0CFX7WGqo4WwR_Hv8_0lVqp7okEtgnZ3zKp2WcilxJRXGsnFv2HGCCEWpSLlYC-lkwn9fYxU5Ba4fMNHd4ZswiGTSweKU3DsXs6glWweykyqI0gPFXwoxSB1lSVVvA1dNkwUT33_DkLtsddW_B5XI8Cb2mb6ystV67c_q0SQC7zDEh0JXZqUOA9NiFd5EUWFqxrtZfYdyYffIXjM3iUWC3F8VMdySlNd39lEupD93L82Jq2SkEXmkbZvGF67-EjuSgD05FpA4y-5g72woAjenbdg8coCvA2YykM9TbjCW0YAezX98NO1wqohWsOr-wY7cdJTB8DnTh_zQyeM24b7a55NOAxElhDPdF8VdzH2xpzSMLxrMJsSjrBg4t88LtRlr-I3qccWZBqyLM0WBbjNSL_I3jfVd2nMx24mLHCdpC1OnwWal5aSfQ6IZgC8DoaZOTiCvb5VuW0_oiyHfAlI2YQcEENXra9HplDkQl7Ht5ikueQ3BczVWKFVZYKnbLYeEVrBm0dRv2-CZPUlpEqvRBW6Wp_WCc6m4jV_bvWvYnyjBFeIQ6wpxJd200PiJVOrgF8QpZzMO7bGzv1rRybl_3xS3Nu-VGUGjmopu_6qG96Goy9rAMLXBjvYhoSL9IwVtGeDFtElpaSCpcrMy2IrHMiJWtziAobU2CDtB1WFcp_PcIax37xQ7BSMtyGT7qXgPeGkA8qSP50R1DwZuN7VWw9Y6ZcATzs4oq7En__E7K_NqqcQawU7TbhJ15WaOIEDzP0E5p1N0dkgj4KbEQ5tFIVU42lChJXw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2e74MoAVqw7BcGXvC7jsB-dFyDEsTYAZ0csnQPCAPOZnPnSmtTDkGqQX4kzgMtsH473jakEa39ENSU2Ip981SMiStxUMr3SpmLMs6wbRMLygO6-UmSkgozE,&b64e=1&sign=4a45fe00fec07b67ecba1f6b16d48c87&keyno=1',
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
                        count: 2722,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 514,
                                percent: 19
                            },
                            {
                                value: 2,
                                count: 86,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 104,
                                percent: 4
                            },
                            {
                                value: 4,
                                count: 303,
                                percent: 11
                            },
                            {
                                value: 5,
                                count: 1715,
                                percent: 63
                            }
                        ]
                    },
                    id: 955,
                    name: 'ФОТОТЕХНИКА',
                    domain: 'foto-market.ru',
                    registered: '2004-06-03',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, площадь киевского вокзала, дом 2, 121059',
                    opinionUrl: 'https://market.yandex.ru/shop/955/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                onStock: true,
                photo: {
                    width: 246,
                    height: 300,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/175578/market_vCiCjNLQZuaLHb-Z50YTbg/orig'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 399 руб., возможен самовывоз',
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
                                daysTo: 3,
                                orderBefore: 24
                            },
                            brief: '1-3 дня • 1 пункт магазина',
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
                                    value: '399'
                                },
                                daysFrom: 0,
                                daysTo: 2
                            },
                            brief: 'до&nbsp;2 дней'
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
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/bQ0xpvL5M6nFc6wW9g_aVQ?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2S-oJcNGkqZqkkQtG3T8r3YAx-ROnP5VSBce_3eDST0zp0v5B4G4rWmf2T3wg16WMUNqXOcIdA0dghzNtg-UaXbSNvSRjO2ORIQd6a-vRoJX8-6qKmByu3J&lr=213',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=955',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 246,
                        height: 300,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175578/market_vCiCjNLQZuaLHb-Z50YTbg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 164,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175578/market_vCiCjNLQZuaLHb-Z50YTbg/200x200'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZGmV8ITbotlZXhMTkYMwxz2yeVTj2W-np-Ww8KxzO7-H3Y-QXtaHnJsNu5doqOEU_j7_FKFLFd7pLFkYnWdeMZeOfkmsTek9sZAyheRfSCQGwFR4uBd4JVsz3l_7tjvsS0ihLwziZ_dyLVnVMwDIjIeDFnYpLagkUYkXN5eQLAsjj4y7r2tQQ6-AxwZsyrrQg-3rE2r2ZULWG4eq9VhaYdQGRgHKdFOESlJqbxD9ux76q8aVOs7qXv_pDhslGccjPqMRYhpgY63nEY3wLdMxXSQBp5hCIRKLbI',
                wareMd5: 'sxtdiXhb9dSmJWIM5ul6Ow',
                name: 'Смартфон Apple iPhone SE 64Gb Gold (A1723)',
                description: 'Малая форма в своей лучшей форме. Представляем iPhone SE — самый мощный 4?дюймовый смартфон в истории. Чтобы создать его, мы взяли за основу полюбившийся дизайн и полностью поменяли содержание. Установили тот же передовой процессор A9, что и на iPhone 6s, и камеру 12 Мп для съёмки невероятных фотографий и видео 4K. А благодаря Live Photos любой ваш снимок буквально оживёт. Результат? Небольшой iPhone с огромными возможностями. Дизайн Любимый дизайн. И новые причины его полюбить.',
                price: {
                    value: '24490'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ0Lh7mLSA2t1tW4z-pNh8DfHlOEWhA-Kc9eI73zIX6RAwlFuot4eiAfBh4-wP9nECJIUj1z8Tx9NZ8G70QHZNpIs-8WjWZwLQagquD76sQMqL4r2Kgb7FQHJZwHO_xxOUw-T3wMJDrpk0X4CQM9ns5GfCpbx-wwG6VqYFCDtuCJvIn16sdDC_zs991xFPEfE82jUTi15o1Yjaw1EJdXVuNUEZi7x8fc7IHApJh6K0BLZZgei9Uh3GfHU9FsBWTqub8R1Lr7Ueq2CRFv2RVtDyhK-0SZwQiaUB_-wQBSIDZIKvrLzLNvjvuCUYC2HMoc8SuMNKSdwvJx516OqfspxIHwADUfO-qytGZl90c2kvdP5x-Rr_onRp_JzYUUDav-SKV_6_FX0wCiRPZP_AKd3bUl7JJ7fLCn5TtfKVgeW-SiOVhfxqhoBEj9EwzzlAnG4-dSEl8uNrtDwo_eEe429FxKY1ncr_To1rcfK6GCquEEENJe0vk02X7dmbWbMmumBw57zU6QHXXl6nTOLbZOAI_BJfsU2Xb9wO6cDAa2eCeMd4hGNkuCEItqdTqbsUgQHNiftXTrzzb_stfWxIQksYTWzqx65TqafBSHof2ukJSgvuqjMZH1h0jAy-95b3k_S6Xhh13S2J95Y4uJ5XasqPv9D0-Jrlp6NhTpZUdCbhjVrA,,?data=QVyKqSPyGQwwaFPWqjjgNkvrW6IDRVeCS9U2yzp7SIufwwMLTzstXVrPYXsS8a7Vi1aKCbjNbck_HBLDpdde7Ds9s1q_o-gVUnwwnfoVdMK75fLeUBTJtwKy_gkilYK3OwrFcOqyfzvHhHSCiOrr09N130OaTMHu8yrFOk8GK87QCtYFpXnQTVdmji-n480LqWXGMNv3zOkqvh6M0YeLFx_wFI5fe2qysTrOOPfYd5nf8xwvJhq8Sg,,&b64e=1&sign=8711b8f10981b61c2c7586d561fa29b4&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVL74jCRl2732D8LkfebSn32A-hl5Kwa9nrSjHmOkdB2Y8ZYNVNND7XM89u7fBg2vxu0CFX7WGqo4WwR_Hv8_0lVqp7okEtgnZ3zKp2WcilxJRXGsnFv2HGCCEWpSLlYC-lkwn9fYxU5Ba4fMNHd4ZswiGTSweKU3DsXs6glWweykyqI0gPFXwozuXpkA6pvzNHuCeoYab8W6hltcMqtWhZSQ_EwIsosOYQeMV7MxQYiek2dYUFS62H6Wfgpb-QhizyYtm2Jvd82seJ-moYIp6217sghjBC_6T6wxOK09MiumftbWkB0zhRim1M7_1n6Y6N_xw1NT4nuPG2ExN6OfaUfNVFWNCMiK09AJz_w1NgU8DrNnS7toXRNfh-b7RgXO_04YcEcLvBa_QzznUuLUPFCXZfdB70gA1HGDRwmJAU5VSZL7UG0m0jM8DYNvvoQJd8mDPeGGyiUeCw0XEHt9GMHf9zmFDQRaLBbBoBkbR-r2I-Lnboo1dEQPKzZ1AivZp65wGlZh8z36Gpq4w6XegqMQTfTaeQvimXT7RqX45k2lWwXfBjMwaij9QuPdryN85kaDM0ZP5RXE8RfEL6vuVokqhSXGpWTbKSpOacbN81AkfkdBbYC2lqI1iHc3sJWIlbzdds1w8OtOwf0lq6GdlREyQAufn9PsY_DsoN197MsNkDifrZiiguWa6qbBwkUvb50Z0wRGzQjucOejfJJN5qUDXJOMHjRSO0L8tRBmaM0w0Egf-etxpHuXjtw3M7JgycgGTLu3TttNQT2wyIfaTDEl568sjXUspK8Kmd_a92-7VU-dP6u1aGWNnI8lp3OaNido8T81c5eFO411AclPd47If5MIA1WEQg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2TN8CrNhws8QWgcz6ay8Ox-zIglLDfJWbwD4lSUdB6cXZDMXhnv1mBl94cWOSefcPPOjCA6i00GOT2p6LLNKT4tHCKtoKnPFPLsD9sCZke7mqXZReYTf5Mg,&b64e=1&sign=a34fe0a6052ab0e0d3933cf0d0ba59e8&keyno=1',
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
                        count: 1303,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 68,
                                percent: 5
                            },
                            {
                                value: 2,
                                count: 23,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 29,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 96,
                                percent: 7
                            },
                            {
                                value: 5,
                                count: 1086,
                                percent: 83
                            }
                        ]
                    },
                    id: 29295,
                    name: 'NaviToys.Ru',
                    domain: 'navitoys.ru',
                    registered: '2009-11-23',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/29295/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                onStock: true,
                photo: {
                    width: 1200,
                    height: 1200,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/214785/market_KHGMBp134qZWC0tW6EbhYg/orig'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/sxtdiXhb9dSmJWIM5ul6Ow?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2TukbaYiMwVCJU78KMx2xjCypDz3yAnfMcTB-gQyi1zBlXQgcqt9jj1icjJ-pdQTpqJZYJdpQcOQBt8vhTV_qx8pq-SB2ZGourn2HNfEB59ekE0rQ4ZaqP-&lr=213',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=29295',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 1200,
                        height: 1200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/214785/market_KHGMBp134qZWC0tW6EbhYg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/214785/market_KHGMBp134qZWC0tW6EbhYg/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZGlyISw5EjDlXXAY9W1rlDaPI5N6E0DVKQbukLmo1KaAg',
                wareMd5: 'JlsdWwC9Tpjq6I2rYnS1AA',
                name: 'Apple iPhone SE 64Gb Space Grey',
                description: '',
                price: {
                    value: '33990'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ16AnPqSzLZF-mrhj5p0JMs08By_7pMA6nKRenSvehC0iYrMj4Q-jylfSOc6P8sM_x9FD7EDO1qYLyyb4Ya5cWfvEeYAPY7e-m7dh4v7t9iuqrFTnzHZePaWOJAiyiLzwUIS6CVrJIVZRJRAKvnhMTowW0HvVDWIXInqI7RxUQN5fGnREtA49rFyoPYoF68jvz50PY7QKZxRrLbbwMKZwwtsBKveoQAZTunAwafCCsrT9p2kWg6P0V7RNhQOHarm84s2JIv0YE36CI_dZM8rRkm0WmF3NYW3oho-pDFw8k4tHgsckdZOeyKMy9-ILITbSC4hbLNMgvFK-asH0M6S9SEIVO7vIHag3P2JgZq5t2ZtmASRQdDucFcWgMj6p1jiR_K78hAL4BIOGXuAuoo98W_Lzmm93TVsfnSA3VikTh79nWEX48jHgMvB9Bfi4EHU27xgev0KkuZwBhJqr7Pi2QfJh3Vtes_tR21ALP3bHUV6KBb_QxKNrnlluc9YNybPX3CA_uIYK3nNdjfSM4HTHzreRXUpo0YWYhf3yy8M06LBvZiftcNSMJjhg7mMta38khWzODU4QDy4F8GfuzlUvGUqJHwig0pAVFxGycsApcLR_l6okq6xmDWkB5PWxnI9oaPUu2T-1j3fWp4Nti2GQUXNJRLdfy8u3jSa9Ab7-Ne0Q,,?data=QVyKqSPyGQwNvdoowNEPjQoU2CiJ2kh8q6CahX4QNdfUG5in7tF9eddnvy3FW5ZA7IW8rgTe7lq6mVUOatIyshYZN7UzAsqcK3KUjt6jlR2GoH_qzAo87BIvR8TX-T5HvFeOWy5HVvwi2dXd3fUJW3SFP1ogZNf-zxe4nEymf96815iOtKxXjrYKAmo0aSGSSi_Qe2nMOzbfKwWr6vLst65DwKnG90SMHSZYzoDha87Dn5b2w2UcbGZt8PSh-NA0jjWndN-YrDGLVEnS2EgLk4UzeQDQEV4fCVEpvStM7URv0JWiUm-dp69Kfh7AOvsemBcJuaxdgHoIzEH26qABr6lbxFuXeLjuOWFAYZl-RGBeDdxS4LNKLSBpRYnqbYnngsU0IwwBQuDErVMcgjcVIy-aXh2e86J4&b64e=1&sign=19e426bb0fd06ed403b557c42c8d3842&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVL74jCRl2732D8LkfebSn32A-hl5Kwa9nrSjHmOkdB2Y8ZYNVNND7XM89u7fBg2vxu0CFX7WGqo4WwR_Hv8_0lVqp7okEtgnZ3zKp2WcilxJRXGsnFv2HGCCEWpSLlYC-lkwn9fYxU5Ba4fMNHd4ZswiGTSweKU3DsXs6glWweykyqI0gPFXwoywI4U1umeOVDk0JLQqqU4EZp68b5QBQB0yFmsZ1t2-u3ue5jypo16ErcOrnwRBYhAFcNIfTAL_SVMSia_0zY7eIKjfzjQRlThMAV7RowykDuuA4us-nzm7niQt1gA5wuobiOFm6yAnncAvdVg_iImlecDI5BWQvwkCt6S-5_66zcJP-33prNsHyZmw8ds2A8i1PiusYxqfjWakpJeF7baT1AJ9WhUOGkI_7SJ9RvbofgPGhMtSEGRuXIHOfIU2cZswI3bfBwAA8uSGeykjZ2Un0Bov7LhkTMv0i5d2DghcAcNAC9qQatKq-Lf8G0wfoWVuS1O3MyUmaRBsNZQ0CqDs-zp3t63OyEbMuahuR6t6lCGi7Xb2ErtCzOphivMCJ2C_yV7gCdAF_vK7z5Qi8ZA2hMS196Az88eF13cNUy_CrfqycgX9j74S5crs9VxGE8C_JbpaNWl8r41FwE2Bbn295Zy4RvKitgGwOMYjlXpo-4_hpr8vI21utuKe6chalb8G4K_RyuIPcwA4NXuBEeHvM0QllTDa3ehkSHqP81e2J2S323HnmkBo1Hhl1Yun3WTGI-mhy8p1ErJfuedtMTDBfRk7MYobbf1E4BV1mL3NStHPgin10E7jUIjLD3kvhY5aNY_aK6MKHiPmYFrplT0SmVIalJ2SIYJ_5V0TkiGfXg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2e74MoAVqw7BcGXvC7jsB-dFyDEsTYAZ0csnQPCAPOZnPnSmtTDkGqSiEmgbUXYgb8RSLFwXHKYQNyW07XPsiQXqqC6kLujYrWnP468Nk7EEfpUQxjuMlPg,&b64e=1&sign=1bb728ae16f08326da9f7b7f4f0f07c8&keyno=1',
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
                        count: 2722,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 514,
                                percent: 19
                            },
                            {
                                value: 2,
                                count: 86,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 104,
                                percent: 4
                            },
                            {
                                value: 4,
                                count: 303,
                                percent: 11
                            },
                            {
                                value: 5,
                                count: 1715,
                                percent: 63
                            }
                        ]
                    },
                    id: 955,
                    name: 'ФОТОТЕХНИКА',
                    domain: 'foto-market.ru',
                    registered: '2004-06-03',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, площадь киевского вокзала, дом 2, 121059',
                    opinionUrl: 'https://market.yandex.ru/shop/955/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                onStock: true,
                photo: {
                    width: 230,
                    height: 230,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/364087/market_F2bSYNWC3iANjYtsAmS4Sw/orig'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 399 руб., возможен самовывоз',
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
                                daysTo: 3,
                                orderBefore: 24
                            },
                            brief: '1-3 дня • 1 пункт магазина',
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
                                    value: '399'
                                },
                                daysFrom: 0,
                                daysTo: 2
                            },
                            brief: 'до&nbsp;2 дней'
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
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/JlsdWwC9Tpjq6I2rYnS1AA?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2Qv4Ejij_tLimLNKkG6sNCfF7b17Ypfd6d2wi-H0ANen2u7cs3uJHExImUpDFi7nEA1YS_gGMU_1p2Wl4eE1U10lHQlGE5MLm02Uc_cUiBkvNww3LvLKR6q&lr=213',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=955',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 230,
                        height: 230,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/364087/market_F2bSYNWC3iANjYtsAmS4Sw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/364087/market_F2bSYNWC3iANjYtsAmS4Sw/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFmMxDJ8si-aA8tj5ujzhpyXhWtDRPTKf9YEa-yTTl1Dedvl4s3GDjFzhxuAFQeG0VblfgFAiSE77FUY1NoZWzneDJFonIH1wsmDgMbg7Bek4aDJQI7F7nP-eDwMsrYNDhFU0K6Nz8ZJkWpzUtV94UV5Sqf_dUiAec17W034uhqge7ixth9VvD4VUQ-JiBzkdTDcObi2lB1phsb5MsykHr9ol9yeAHJ8yrazZqIHlF_gG6Mx6dnDbF48mhNcwb_MwUEhhyB3DdbeYpf1M_RTHWAbmN4fLqpxYI',
                wareMd5: 'BhMCbdx6fEPpi1bNE9mXPA',
                name: 'Смартфон Apple iPhone SE 64Gb Rose Gold (A1723)',
                description: 'Малая форма в своей лучшей форме. Представляем iPhone SE — самый мощный 4?дюймовый смартфон в истории. Чтобы создать его, мы взяли за основу полюбившийся дизайн и полностью поменяли содержание. Установили тот же передовой процессор A9, что и на iPhone 6s, и камеру 12 Мп для съёмки невероятных фотографий и видео 4K. А благодаря Live Photos любой ваш снимок буквально оживёт. Результат? Небольшой iPhone с огромными возможностями. Дизайн Любимый дизайн. И новые причины его полюбить.',
                price: {
                    value: '24490'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ1Ts-rHvjh2B58g4aOKtpeRTccdItB-cxnizR7W2lL1CMf-Ag4njWOl58ejdPSvlXumFLx5RiilbPQKl6nTfpusBMYMYJnbUUHJTf9kRGiLV95RnAgTNaKos7AqjGhkX5McZO5IS2Q5SAvKHN6VnaIYzLhMPH9XJfFJVfkEPgO02x2hi9hTx9tbDHwrxCtzlhs6CzNJD2ucjUQXRaXHdECUkOPCAWCplbA8kt48ccWoqOE3Y9-gbOAXO65zoEI0wLhR74XPLmtpIX6H6HRQTGMRXydgTiwuMtzcGE3HQRmZUj5iPc8iUc-7IUAnkmRyEVd811L1rUsVJsG2eZEw38Gtc1ifF1Hwk2ovNBgIP_mefzCQdw4_F5yunCzFzdaFXHuRxE-vL5Uu-cu_9QiP-gUIISWrXqfKAA1R7mwSslJx5sLsiW6Z9nfy7VSPitGvwOMPBAq34tMCJ9YRH2U0azwvKUMXiINbGOjocHHKCFP-YaiSMa5L-YhmMhKYHdCge7cesi602l-ETCKUubKx5i2oNmj5-JcSMa3-6IZ0wiW0kZpJJkQqcKYOk6jJQOx8XX0iv6koYkbP-eExUNRFTA8cF1JDLBwSTZz3u3r738VhGmkzZQzVMymaTkd39iPPOp3DFMd9d4mYOF9VS6HJNHZIHMvJgte0MZ8NWMci9h5DzQ,,?data=QVyKqSPyGQwwaFPWqjjgNkvrW6IDRVeCS9U2yzp7SIufwwMLTzstXVrPYXsS8a7Vi1aKCbjNbck_HBLDpdde7Gz4f1OVJmo3SiVTBxgDKBjEl9nb7_w3xVrMtOUz7QUtq-RnztcirD17A4UblvTbyqoc-lHkATFWZH15n2t_dYt6DCgkMtgzjazFmjM42dhNF7idxJJu9hiMJeMXighbOIBxZGmCZiToR3JiEC3wfJgHyNdHujqLJw,,&b64e=1&sign=37f420843aabd088f3d6068def29dd6c&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVL74jCRl2732D8LkfebSn32A-hl5Kwa9nrSjHmOkdB2Y8ZYNVNND7XM89u7fBg2vxu0CFX7WGqo4WwR_Hv8_0lVqp7okEtgnZ3zKp2WcilxJRXGsnFv2HGCCEWpSLlYC-lkwn9fYxU5Ba4fMNHd4ZswiGTSweKU3DsXs6glWweykyqI0gPFXwoyDrIexv5fGBjODhBo3j5VlJvlrAowtcqfIXtKLCID9evEVMNmjDKLYgqvCwQcZK7raXobw0Pt9lp0PLF6aPLhtyGt4iEkUpXA924PwPev8EPXsIYFYRk6g_hmXcw2QLdDAHA9x97z7A7Y5HPshHkMCo2bxygOKh3Q8tH310st9BpS0cb5ONcfU2YFaGWE6jP32GMq9nggwSBjQt0a-zJCKXGKvxZ8281ENxwlBVUtIPDJDYeDirTLzMPNoV0FwJ3sFo-oDLsNWG0OsZgbQgYwQqPvmXlRVVcEY_baWZMrDAto0xX_3uCDSMwLlOCEEMG1jR68nSmuIkS5tG-0sEO74dmezMwISUVpmevuhVZfmG5W8BNE7bT1YITBa1mAfZEOMCYwfNYQny8bm7fW3NHTlCx2yDIYlb9SVoeOp1srcf_4Q25ZZ_h3HQ9o0MK_cSS9sB9D0fu780efFXcQFa9uD_dcpPdDitRHkAyw_I3rhkcVUZdfBT4cA7bS1XJpt2XmzitWfKRisTRKXW2UNq5APt38qKS15zuiom9P-7MIsWhdkJB98yGEtghvMUpreech0lCViDpFvjpufRXOO3IqsWorH9EiTmfRVvwpiSAMo3DZ29qAdvn8CKPqZxTo9iqTBDtCiPvR3yhG9Bzfph2kKGWeUMxnYQ8njL7qMIibidg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2TN8CrNhws8QWgcz6ay8Ox-zIglLDfJWbwD4lSUdB6cXZDMXhnv1mBln5UnX4KsJL-8PrZPIK9PcmS9fXXtaCsjPNaHq18wymKks149cw8ajt113qSRvIUs,&b64e=1&sign=735eed54e18741f0b75f513061c60aea&keyno=1',
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
                        count: 1303,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 68,
                                percent: 5
                            },
                            {
                                value: 2,
                                count: 23,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 29,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 96,
                                percent: 7
                            },
                            {
                                value: 5,
                                count: 1086,
                                percent: 83
                            }
                        ]
                    },
                    id: 29295,
                    name: 'NaviToys.Ru',
                    domain: 'navitoys.ru',
                    registered: '2009-11-23',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/29295/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                onStock: true,
                photo: {
                    width: 1200,
                    height: 1208,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/230492/market_1VmoQhg13miZpWnnwgwzYg/orig'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/BhMCbdx6fEPpi1bNE9mXPA?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2SUpvTb2NGJXcDnYmDPEITkiAMoKLA-u9vMfX0Jt5_PB0qakBK9_18snR2F2YVWqdwjhQGLIzomuU21-jU-N_O6SlrETFBan7nEo15rfm97o1c4dJyk7avt&lr=213',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=29295',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 1200,
                        height: 1208,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/230492/market_1VmoQhg13miZpWnnwgwzYg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 158,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/230492/market_1VmoQhg13miZpWnnwgwzYg/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFhEeS4pWsCcLBEkLNFGMaPDbVR9JmyldRWEY5nEUA1Lg',
                wareMd5: 'PuP3JUrrsKk8jsJPuzziSA',
                name: 'Apple iPhone SE 64Gb Gold',
                description: '',
                price: {
                    value: '33990'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ2RlUYeS2rWw1H_j1sv6aG0TofiqsJuKCqcFeRhjCJZDUD21daH0mcuQWJQau3A2sFhqy1ZlpV70Q8qMvNmEZoUP6tPNFS3g0NZ-RWrxlZ-7GKTcvNe-ZYD563NSnJ7ay6JSx6Q0HnCl-jn_R1kVFd_WvHi8391GO68rCcXi-7hykvvh6RPvMl_-fzZtsXGZjzXwlEuZycXrEvHeq_4qybrvrD2ZW2OLiy_0SvkTPytDdkIbvo1sjpNCliIzNzB6cVtshnVERNTw89qC-oox0ZhlQoKelYTuW48HQce6gdtyO5bFPxKYW-xamK9-HipfGPJyX97zbq4xoGP6ta0JimJu0Oqr_Wk5RU8NkxXiVt7-Zp0xNqgfjrCX_yD2wjOcg7mtEy4u80AnGK1N8knRxzjk9Aq52wk6s5mfeU_AvORnLGtaqkVYNxEA7rVyB665r2U8_aZDfgLQfkh_YW-P_dcD272sERp_iLIGtQdEdm-kiS_OyGUgrpn9unhpQ-W7PxUX42BooCdG0au9pqBefuVAZ7sgRGo0nbc3kNn0mxO9OE5F9ojDwf0vvrI9mtwwsJ4lw9eHKs2n58N5YtJUnnU71Yzg_Uqd-JY8eAvOohjuDy7brzSlm41l1IP6w2myA85TCHZ0KrHd42j7M_d3ue6Kw-41Ifqxsx0suuJlahWtg,,?data=QVyKqSPyGQwNvdoowNEPjQoU2CiJ2kh8q6CahX4QNdfUG5in7tF9eddnvy3FW5ZA7IW8rgTe7lq6mVUOatIyshYZN7UzAsqcrF9qldgKtMjDwMkkILG8Mh4scvm1GrSkm9HLsFMxIdtvvxJw7wZ4HTmBhfUdPnvyF08rpCUpXI00DAWX-u9ELyKiFTevjRWSWuv-TfPpIeKvwtCWvTcSNPH4o66r_AupZXEB_Bse59F9k-xcUrVJmkWgHYLpz9cNZ4edXCuKyjppl_4j8NPLXshO9Vm-B_UxnMRQQlVBurnVRmwDA2Tz9dCs2UfoG3ewKxMg0-FOiF5-Z5_P9Ro6G6ZtWw0iyga-0UaaRztyE5j6rmQlPPjlw0r1LU5ddh4WJ1wJJbPauepgehrIGfaPAw,,&b64e=1&sign=3c6845c2de5f3d06ad26eaeec3c2250b&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVL74jCRl2732D8LkfebSn32A-hl5Kwa9nrSjHmOkdB2Y8ZYNVNND7XM89u7fBg2vxu0CFX7WGqo4WwR_Hv8_0lVqp7okEtgnZ3zKp2WcilxJRXGsnFv2HGCCEWpSLlYC-lkwn9fYxU5Ba4fMNHd4ZswiGTSweKU3DsXs6glWweykyqI0gPFXwowGoOb1OnoBs8NKmYqEdv48A5xbQHyy2aM1qlx_2OAqldKkdQl-SbeOzcx7Br1fYJXpnqUaNpFPgq47Z93swoG7K1IA2Wzr1lvA_VLcvyRip-U_cn8ClaZCmZN1NP02coTnLk9Pa_kWtnV_PUjQ9gzi_-MBvVSsTZzZctW1lLSzu3yw2YYztBCmh9UN1_B1JQna06dEkKyNrhXhGKW2rCf-FjOh1yFDN3BE3fLNl4IQGK0bVj9mfY6yC6yVg5Yeb7wJUxWgAd064nDDPOHPOaHfQnTC0c0G1CN-n2Jh4RPUS6U0arOC_02ocwHonyO78nKleNxyeL9Fmsnb4O3WgCw_tIvurmRrym3J1aIvu9mO8b14UzJUqkIirOOSXBq7zegtF9lhPI6zcDn3WuU1rJe2UT8XTY1lb24b6VU8_nHN9ghtpmVsgg-GcOyv2Gf6ZnN1bCd2QKTnoBWN3G5n6NYKG7rmVVvAp_EXJhRVg8ufXt4tcvMLWwUdPi8aVyByJeCU2SOh_y9AOGD0xVCI9GgLEw5e8N3-dJuX4UeCFBi7Bsn5b_UdIftc5jMoRIEmDZwGaxQBbG4-f4wBFVPimyEmoiIed7qwwkCb4kLGcJRwJmGummqUguUpOLeu9yyF7OlcLWI-bwF6_8fHfNG2rpMMnGzB2J07j-ZlGhVAKbDadQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2e74MoAVqw7BcGXvC7jsB-dFyDEsTYAZ0csnQPCAPOZnPnSmtTDkGqSznw2CbFTecgxZyPUcpJrqppmllR1Ki0MiKqW6ZOCVjn5SGDz0xEqlebdcP9dLjFQ,&b64e=1&sign=43d4b155871fc0ffe882908aa40b87a3&keyno=1',
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
                        count: 2722,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 514,
                                percent: 19
                            },
                            {
                                value: 2,
                                count: 86,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 104,
                                percent: 4
                            },
                            {
                                value: 4,
                                count: 303,
                                percent: 11
                            },
                            {
                                value: 5,
                                count: 1715,
                                percent: 63
                            }
                        ]
                    },
                    id: 955,
                    name: 'ФОТОТЕХНИКА',
                    domain: 'foto-market.ru',
                    registered: '2004-06-03',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, площадь киевского вокзала, дом 2, 121059',
                    opinionUrl: 'https://market.yandex.ru/shop/955/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                onStock: true,
                photo: {
                    width: 246,
                    height: 300,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/209514/market_Hpins67UX0M-6XZGr3SKzw/orig'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 399 руб., возможен самовывоз',
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
                                daysTo: 3,
                                orderBefore: 24
                            },
                            brief: '1-3 дня • 1 пункт магазина',
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
                                    value: '399'
                                },
                                daysFrom: 0,
                                daysTo: 2
                            },
                            brief: 'до&nbsp;2 дней'
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
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/PuP3JUrrsKk8jsJPuzziSA?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2SJ7AHnoKZ7EJVm8_tTTNq21JEsghfvo1G-BwZZAKbeXT9PoOXTh34vidFMw51Hb1_AuT6S0lcZtKNz5by3pIwowBzEevdUi_BAOhn7vE5D4sXwMPIjDi-h&lr=213',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=955',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 246,
                        height: 300,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/209514/market_Hpins67UX0M-6XZGr3SKzw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 164,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/209514/market_Hpins67UX0M-6XZGr3SKzw/200x200'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZGRbERLRqbg6nPCAR-gkDrOvkNj6iamrj6QYmikNLNr5KERttJjcg3G_xPEUiS6WtmO2H6Gv_BkeUcwSfap8FzHPUbZge12NL87zY61NclDlyS-ur0qwYl9NpIOF96rgtsXAUuzh86DMBQSsbXOcwkuSfd9Qrm8qnETQphtE86blQdma4lWv6MKpdofrJUhZL2xrqelkMke4R9eXeFfX7YHpdL2wFbvRAuWrGf1dZ9c__YIOSFJpwlu453omkHHP-a2YKgEAzTA9z-folPlL_6D8D-ew9MH5qc',
                wareMd5: '8l2gb60bQYw7htnU17uhaA',
                name: 'Apple iPhone SE 64Gb Space grey',
                description: 'Apple iPhone SE 64Gb Space grey самый мощный 4‑дюймовый смартфон с полюбившимся дизайном. Установлен тот же передовой процессор A9, что и на iPhone 6s, и камеру 12 Мп для съёмки невероятных фотографий и видео 4K.',
                price: {
                    value: '29750'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ2qtg4LuROLi0ojsSo30-6smAZta_oLogb5weIcp5ginrN0PJd3RrpgUeLevWyghqyHylgEyqVkby_2ZY9Ciegs5Bk51hH_xzooDp2YGjXhwVHpNAypxWwWpUfGvzG9m85cAMIK0qWiAQ8dn3MszSLia074vK9OyBPtyUFxfDk-oV39RSu-JLityRFXRL4Nc0yKhyV71GQ-EyohmDcfbhpaHGTZk3Oj0ApdxSQqcBm3GAaUe6fbdPYQLl9McZ7sS_qBi6ZFR584TuwtWTrwPNTp49E4Vm97DQzxq6-RFXoBSDuObci7nER4eH32vh34kD3vypsV9_OUsPicXuVMLwrui1tJfua9Zdi1oKXJ3LWStldxjAtfyulLmlEqDh2tAmfAzQnrNQjoq8-TSW_hUAwc9ouItLxaN-Z4EZPi2DD5yKOCbuNETROYv-Wi2mJRhVm0WIawctq1KbYuwF-6hWMQ0c9rCU7cBs-rLJdKUOrrvWQx-CoD0HNZyS0ffmc71Gp97q6IbunQ-YBFpz4sZNUWmg2a6cAdzQneU8kn_WRrRxt9o9NJOEQAM1ULElwP5F9Vfm92H8_cXuDElWeU4e6_pLtmVgJUuQGu1bgxdP2CLAm_L3LIbw9dx22gq8VZTYP63dmJId7o67czf73t4hqjHPlWeOcNjIItn-aLFADoxw,,?data=QVyKqSPyGQwNvdoowNEPje0PAcg_xXLamMQAyNAGhbkNussUv14IVRoAKVqnrKOILCorrWMPkK1dQIzotqJ1bvU_S5WBaWJukrrnVcB0WtRoX05Kpl85Vqb-0DBAdI1ADI7kDeqLGnH1lE33lklGW4b0XorGYqGjrXL0Innu-2k,&b64e=1&sign=8e0e9fbc728137bb63fee5795323380c&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVL74jCRl2732D8LkfebSn32A-hl5Kwa9nrSjHmOkdB2Y8ZYNVNND7XM89u7fBg2vxu0CFX7WGqo4WwR_Hv8_0lVqp7okEtgnZ3zKp2WcilxJRXGsnFv2HGCCEWpSLlYC-lkwn9fYxU5Ba4fMNHd4ZswiGTSweKU3DsXs6glWweykyqI0gPFXwoyqDkfYij1WkgRdihZtaYHjZQi9umjy6pw8IZzUqLq_ir9bSRk4pFQUwpWhVP_aQLTu_EfywqzgH3BQ6uAyMJ6Z7SHAZ3GGnUBOWX75skCUOVmjT-BTJlsB6tot9zt-Ubk6t3jEVox1XJ26l_sitHlTaI2ixmSfU7iz3wsClJ6_6rAB2fm44ONyqDPDqaPUBVpD5-dXYwXVMlfzlC1ZHiPifY6xp2eODyU-i4rETuQNTCLvSD4aJh661y5JTUkNCV7W3UpEeO84jpDfFDtbWhXREcGTzwXNynIIPIQCARKE95CY57YKZu0kOxZh4Wt-S7OVkKCqKLPlQ8dQQMRw4ouAt16NR_9NWDBaT9uCnDP9p-6rKVg8Vp6fnh986Gb7T7rUMJMQS23vIW36_8JzpUed7Rli4y11YBa6TrG51Qr85Embb9QLLbuZzCBmh2Q7_EOhFZTGVIumUEdrob5b_RBrkLlFu-PVamF4bqIaY0Onj2F2xWYmSl38YnLGVM1Z921dDspTYtDX0nPd-mCN97yhZiBdMST3-UEFUZM1fU91z7IsaqEHk-BugNaNP3MYuI5tS5-RvAq0x4mNdwTm6m6FFylcUfUgt18FfXao3cljVWI2Y1v4XR300IFRULj7ekw6-b-lmAjKWqPB5uvQBTU3Iuz6Wc9GE2MibSIEHrozYA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2Zyf3IAZrgr1AM9ENJed7Xb2Mf6-TUnJrpCayQKahqS4ZHoq_23a-oyNpPpV6s2gW0Of1JZ5rO_UiNhGIy0fMIb8cz5EPw1ZX44aBfblLvl9EvNVCTUWhmM,&b64e=1&sign=9b1b22176be938d3f696b213c3b3fa40&keyno=1',
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
                        count: 1821,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 210,
                                percent: 12
                            },
                            {
                                value: 2,
                                count: 43,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 27,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 92,
                                percent: 5
                            },
                            {
                                value: 5,
                                count: 1449,
                                percent: 80
                            }
                        ]
                    },
                    id: 32902,
                    name: 'TEHNIKALUX',
                    domain: 'tehnikalux.ru',
                    registered: '2010-01-28',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, ул. Ленинская Слобода, дом 26, БЦ ОМЕГА-2 корпус С, 2-й этаж, офис 221, 115280',
                    opinionUrl: 'https://market.yandex.ru/shop/32902/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                onStock: true,
                phone: {
                    number: '+7 (499) 703-00-15',
                    sanitized: '+74997030015',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ3MwS9gAWImZdrZmcGVPvxziGYpC69wMIRwR-Lo6Qk-DkcNF3lDN_r-7x4yugnlkuc3E-4ZbgNwsg5Q77-7qR-0EP6zBkKUBFV3x1u6AlL2oWciBQVZT_2flospfVUXe6va2IZdZ26PwlAUbfvNxcc4d3Iv1pSpsPF30DDHiYb7EYlRebFpKK_K0U8HwykZ340NjIdzpUS4dYtjYWY0iQNWdUqw-P0paB8F5Dy0e_anliNxJGaElFX32r5ypgKR0vZkO1pFT_MaS6EBgbaf3sQ4wS_9wm7WXjNyseNVA8qnXhpAE0yQaWDRaLte8sn0p9JmfV8zkzklEm8FNzumZMQLy1SGVfUsuZXZcfda490oVVlW_J-04dCtJJZxiBwa3VQgZse9Kgcdhxvt2Ymmziz5cL1JSNMmMsrjlxUXvWmWfXIINRLP8Hua5bAiczPXhEkZaZnRTIfp0jk5fbcF7xhLN2QADSomgUDg7EHl9Vll_FPSHhQH6g6IKEVR1r-PmY0WJmo-De5DCPa6m4lJMqkWRc0M-YWJjqndx0NQBrT3gJ__va42sEK2lgbgYB0i8qbxSDmGNLKIHetaOhbDsFXhTcAhzznbb1Onaqrw_F1a7okdtpao2cxY_1VRvVu_Drka-IllEyxZXhBCqmqD4pwCye32UFHp0aL_WoOuAsqnag,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8R2HlPunt9uIeFDUHwLpKEDT72Ri7-zuRlxd-iBsOz4RIWz_gQEIWBleeI2rZhjVhXPGMm4SqGGF_zApPuJZW517V1G58Vwe1m8Ht3Wfe-rbAiTPWssMiPP3DFCyVz8EmxbwfBrgnEV5mB0LT0lUYjJ3dibJZ8RQnn4VDs_PB91w,,&b64e=1&sign=41e3ce23cda81e561db73b9deb3d2301&keyno=1'
                },
                photo: {
                    width: 470,
                    height: 546,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/203248/market_QSmgR3rXlnz1Dk8Z8TMNpA/orig'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 390 руб., возможен самовывоз',
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
                                    value: '390'
                                },
                                daysFrom: 0,
                                daysTo: 2
                            },
                            brief: 'до&nbsp;2 дней'
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
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/8l2gb60bQYw7htnU17uhaA?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2TiX2tKlzdi5B0RPUCXyC4GkXH7X2TnuatdsxtAyE3FaLfkKh5VNeAZaer6PPTBFzEOGSwryT_Xgzrib6NlyI4XuMyzHk6iHzNnm7ykwSfHNzPKUviA1VJX&lr=213',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=32902',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 470,
                        height: 546,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/203248/market_QSmgR3rXlnz1Dk8Z8TMNpA/orig'
                    },
                    {
                        width: 688,
                        height: 529,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/177631/market_oC9mO-a4qxCyXCwuO1uHEA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 172,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/203248/market_QSmgR3rXlnz1Dk8Z8TMNpA/200x200'
                    },
                    {
                        width: 190,
                        height: 146,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/177631/market_oC9mO-a4qxCyXCwuO1uHEA/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZGeiJg8l1_Qna6q2QdpKoTHDJvvPv_1jKXCs0CBJDYFMCL5BEhS2B7DtZEkdmqn10zLTAZXWFhtxVTyNVFhrqtY-dijvcEQnk5tfVvXEXqd3k8VDUnt3EUixhR96yf0c8X5TnA-UCRUQewL4rBsodCX5zBNjaIU4c7eGV73cwgAPiTgLkefmsycc-OkIUWoXkcDZ8r7IsojgvA7z_85zpWLkohRmPt80oT0abIZXg6SpRpODVO0vFQKSmjtCOLt4ln2WJK48h6U5xt33DQo-GVg0npXUSy1bsk',
                wareMd5: 'ii10gvYsQiXyUbs0k4PaXQ',
                name: 'Apple iPhone SE 64Gb Silver',
                description: 'Apple iPhone SE 64Gb Silver самый мощный 4‑дюймовый смартфон с полюбившимся дизайном. Установлен тот же передовой процессор A9, что и на iPhone 6s, и камеру 12 Мп для съёмки невероятных фотографий и видео 4K.',
                price: {
                    value: '27799'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ2fxKoAMcxauknTEpTmBnJPJlRCsSJxd1sXOhpoPu6CBw-hbuV4t69vChbosAO1u3XjDEQ5rHbHAqsAlG3Rq8JM-OhLuL5qssP5kZeya51mBC7KcU1CjIGBoMcZD9fbKfIwz5st05hqqlZMCSXIWL6DWP3tAbEucYr4vwr3fNpdlm2dnKtD96KPn-d8kxaZk0mqa1Bi8njNOqv98EXddyz1Q2eNeq9TZirY_ooix6w1yGbikqitfozH-8vNzXSrNCMTGWfMHX3FVe7URxs8JDLhRsG2R1EAvVDSYkua7bvkN3RQLyuR6dOhFlZit2LgnlybYmITUuXE2gpKd7nReJTS1tU6nan4AJTz4EjN7trMgVUfdOuBuNUPi00QySjtAxHrhggNz2aWqurvxgblTYdITJ0WfDFYQm1NfQlaDSY5IM_nUy_Jzkw5KXq3HZRs3WvUuJKkyqRuEpljc4Lu74KjlO5n6RPANT86AM3sDRzFzaEZFDHf5ey0k7RMcQJKG6iMK9NEIPbJ3edZaMCwNiBmid9mwWtXLQZN-9Bv1m4rXdcS8RecjNw3_yUljM0Reb6prIlc5K3Zrg1uPREZhSca7GK2QIVdrqvRavLC5UECXXdHpI_zEYXpS79rdFuORVuimfq1lbsutu5HGXeO_Wexeu8iox7LPY4YSapYGfJDJA,,?data=QVyKqSPyGQwNvdoowNEPje0PAcg_xXLamMQAyNAGhbkNussUv14IVRoAKVqnrKOILCorrWMPkK2g7AsAsjNpaDjfFD4ASRjRl1SY6ojDUucWqA-8K8TlmNBR0RwOsRhLA_l2QDKabrqEj6wU62KhNJOMweGznWcSomqV0VH1N3I,&b64e=1&sign=296766886d109a5c57a5903dfebb7299&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVL74jCRl2732D8LkfebSn32A-hl5Kwa9nrSjHmOkdB2Y8ZYNVNND7XM89u7fBg2vxu0CFX7WGqo4WwR_Hv8_0lVqp7okEtgnZ3zKp2WcilxJRXGsnFv2HGCCEWpSLlYC-lkwn9fYxU5Ba4fMNHd4ZswiGTSweKU3DsXs6glWweykyqI0gPFXwoxY34qPPi68gDh7SfjUVdbIt65vzphl0I7PJGI4fqDso9xXLRPJ_G72-7o7eFwd2vrhMRLu4xYajRHpqNqDPqw7LAgG5q-_OrlQ4VFP7GELKwJNFhwj5xno7u_a43lkkb01nROOtKXV30IssRVLLUgeO8oVbcGXdqGOaoOycBoHS1EdtIT_DRdY6UwTx3gTZ6MKZXGk_u-sqKmscIIOWbyh1F7msERsSI9q4UZ2RbcGm9jjrGovjcCok41CYvMgjHnHnBAV4b2InvY90x6chqkEGADzrUZQZHPJDR8jpu77RIVW7svcoMPl2DcSTQSAdBzgT-Z3oF5l8MsVVv-3rxbxqp40LxRb40fuosvCe47xOjcEML79o5ToBVb5iOLzuvWKSjRWqGrbtJJVg-UnOOwoLF0G8zHX_3SmnQI7u2W1i7YZGgAAwHRuWfMDupo1OS4dPP7c_bUZ6sp6in0xSUNBV2ZcGNSA2smK-56tPh7UFmLYcwKH2wRagUAhBfqGf46g9BRkH0QB5N5XdzSc3U-Mm0ZP8EJE7VO01Gk4n0o5HmIpDa7NsmoHnBwQJYvT9On63Ez1f__yuTbc5BLIModoDge9yLCbF131aYu8q528SmFePt1tTlEdWplkr1dH9xuLIJ6UDc8oK3g0IJOMUAdmMKHd6X1SjcjP-NSB3pZq6Q,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2Zyf3IAZrgr1AM9ENJed7Xb2Mf6-TUnJrpCayQKahqS4ZHoq_23a-oxu04zuZoLifLkf-D1JjWM5lYRreOzvPFN0LIEaCw8u74mdbaos3b3v3q1WjF4aXSI,&b64e=1&sign=21c56f2570ea36a714fd541890c79dce&keyno=1',
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
                        count: 1821,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 210,
                                percent: 12
                            },
                            {
                                value: 2,
                                count: 43,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 27,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 92,
                                percent: 5
                            },
                            {
                                value: 5,
                                count: 1449,
                                percent: 80
                            }
                        ]
                    },
                    id: 32902,
                    name: 'TEHNIKALUX',
                    domain: 'tehnikalux.ru',
                    registered: '2010-01-28',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, ул. Ленинская Слобода, дом 26, БЦ ОМЕГА-2 корпус С, 2-й этаж, офис 221, 115280',
                    opinionUrl: 'https://market.yandex.ru/shop/32902/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                onStock: true,
                phone: {
                    number: '+7 (499) 703-00-15',
                    sanitized: '+74997030015',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ1xPspH7qFdhpfQ3KIx4ujFF3QroMZMli7YhP5dSZ7Metxc_QG3z4A5sjWTM1GIfqC4meK08Kzxj8nEnrjGjBcwL7mHCFAqU7yHjoFV1SEJvSGLk7INB-x_rAK7ZXtGrA6OPLD7eLXj3SNdITLrQr4YPFIWO7vI6Vfy-y7eBFBYLsqtDWYOklD9YHTMZf_0m8Qie_HIsPGFXNhtCy9SpENGQPQWJJ9jtu_osI4q7St0gnZW9o5wPsnmwLqyWf-5wlA2Rdxpx9iMiLzhiK_ug5z1Bh_0z3jrKsG43X2X4VvL7JOxw1im43hYly0bSQwCMFUlZYAR7nC9Qm-e-hGgbqx5kmOMeMQWZETiiU2kxl_VBghAvpXrDCmUbjLHopiiKrLP0GHksAXoAN_szPk3nspM15X88aP2PtK8QmLXXooa96QF1NwHwrTp31lfxx3uP-LUU9L4ca6v76fQNwdB_0OlrdO1Ifjm1AlY3O2Nr42-TMzqhAd2y3BFhOqymtmEr5ff23rOWNFxIZE_nXonoZYPghgOJIzb53y0L3ozQVgU6vCEIeEXq3D7R37IYfBHj7T_urF_AR5Oh5VTHdVoV5Oj6mZ6KqNHINjSZkUfIpkIM-0PBToXHc28RQsCFA9rwkcPehKTPf6FoXkz6Yv254i8XKpSaozu26sZyMDJQfcdhA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_aFKE0fsIL5GA2-PM9WsIeKqpdLlR9rpJC4XLZxeiVIkXpHsKwRoocIJvqh0EbmjxwPt5Y49A_lsNU4hd099wrecoyCbuHRzuKUj5fTAjlXjlxhjoc6BYBnJ-jA9cm6SH3i9XYqrWB0FbxouSsZgFWLFP64qo_aTNVYtW2fSbI8w,,&b64e=1&sign=ee598c57fc3aee7d22354840f9724405&keyno=1'
                },
                photo: {
                    width: 470,
                    height: 545,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/219743/market_Q_GPlcDIDgCNd8XvasWZug/orig'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 390 руб., возможен самовывоз',
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
                                    value: '390'
                                },
                                daysFrom: 0,
                                daysTo: 2
                            },
                            brief: 'до&nbsp;2 дней'
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
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/ii10gvYsQiXyUbs0k4PaXQ?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2R9tvbwSlDRI274H2Kbl4taFHdYzYUL-YM6e-ljE3j0GHdgwOdWqywaDJxT2xneEteanqdaDfCaAzv4seJG9QAHxZKFYfiiOR_pwpdswIhVgqXv9DWvvcJx&lr=213',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=32902',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 470,
                        height: 545,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/219743/market_Q_GPlcDIDgCNd8XvasWZug/orig'
                    },
                    {
                        width: 688,
                        height: 529,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_9gfWcIpZ-Cym5HK1WU7wIQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 172,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/219743/market_Q_GPlcDIDgCNd8XvasWZug/200x200'
                    },
                    {
                        width: 190,
                        height: 146,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_9gfWcIpZ-Cym5HK1WU7wIQ/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZHQhrZaafbd7Mc1eSXZVpQpynILQF4pIdJU6hhFr8lax9yj6HL3UOMQ0ZPbQuKJz0EoYRZIbZulkeHS2k78Lbcf_zw6N3B4e6iLQcIebwxeO3k_wPoBL7QTTlnna3i4tl6TKsmPR01eIImZB6OD9njCIgSQup75ibMtHjJ2ReiDHiO2Q67FIsPD_mYo1oax0Fb7mXsGjgXbbOWzQgmIXZkyvVI63e4eFR2qY1rq20vBvk5ABjiUuCEiZ9nBpLP6S2Zhsux_fSxSoFFNQ2LQxnVcNzVF5ppx3p4',
                wareMd5: '0tsmcTdO2LFjqrn_hdUEZw',
                name: 'Apple iPhone SE 64Gb Space Gray',
                description: '',
                price: {
                    value: '34000'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ2VcOsPaI_IVbOSGBtQdd591v_OaQBQD1BaVhJgHZTOyaVo3DLavKGASyYByxNJM5rgo_pSLLAqOZcVsy9tO6qJBT5pIefq7MQamM1AxyU6Ei67RJYzABXuHX0xYTeoNAFapAAy9RhcHrBYJX0wcNlSBN_CcCPttq6-0iMPcmaLsfkf3175WeKTFGYlSUqjPjlQAnqDlEF5Il9QllfC4go8l0qRVUZdrnD78uY6D-4qDdgDjYICoftAcV1peEMY9MzYbOJFMDEBDb02qcSs5LwZSoHGZ8MTy6xAyDEmBlzIWOWEQrGaM6GZ7-7B5TEgd0YWkvZMFKnbfbnvtFgT7Efw6OnJTpYYjxk_oWvGtO2iPetMlk3PFi1DARi-1bbIvoSgDRvgGhcCB7-aKu373cNa4lW4Cnqwrxeb0M1cAiG3GDKuveMXXf4kJ_STOLI9C3ZTD4Yd1HOfvo7FgyG7kio5RvWr0bmN0O341nSn09vTGZQKih8c1f3W29gMlz8pLD7UWJjAjddAeB8HGq5SIRnEYbXlxjbKoplF6QjdnRKZJSSrFZkYxd6dWkFGIEPGv5CeuXkQtYDWTHDG3J0ZbEt-56sNfCL_0HWeW9mJVESIHSVxrHrbplZfy4EAc_kIW1BDjojk1RbF7MPfHVMZdE3_afqEclrvMirhywdKEK3_kQ,,?data=QVyKqSPyGQwwaFPWqjjgNv6TYGEmp4xrtfnkudyEdZNuunQ-_LXA3c4YUz7OsmQZp926dm-H345GWgLm2cteaKNIG79QP05U8pO3La9uiO1psg3Pk4qvBM4YfIsOrOb-6vZl19nkHlVV5Kcu9UuvXDjwQKS6KbQhPxXh6f1I8MWba0fa_Hvt8UtGs9074EBha76WiLV6-TqBdj8tMO_If50aarx5tVB9&b64e=1&sign=e7fd0d5d4c91d0720007ba0eac3bf058&keyno=1',
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
                        count: 30,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 5,
                                percent: 17
                            },
                            {
                                value: 2,
                                count: 1,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 0,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 3,
                                percent: 10
                            },
                            {
                                value: 5,
                                count: 21,
                                percent: 70
                            }
                        ]
                    },
                    id: 375225,
                    name: 'xtradehouse',
                    domain: 'xtradehouse.ru',
                    registered: '2016-09-01',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Малая Юшуньская, дом 1, корпус 1, Деловой центр «Берлин»., 117303',
                    opinionUrl: 'https://market.yandex.ru/shop/375225/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                onStock: true,
                phone: {
                    number: '+7 495 294-77-82',
                    sanitized: '+74952947782',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ0P3zjVw8PbrygjkgkwDdmLaGJ1RqSjwyPg4BMk7Ol61dFpMw_sOwnhHRXOphi6RjrlF8YIhLVyzHvzdSuSG1t0sgYsHXetOAI7VTT03J2Xh1yjQ6yw5mg5VsZcPCkk5aVkQ5e8ycg__vR30EswUitQGCpUltICp4aBh2U8_69iLpw2s9VDKf7DwhRyC6MYszVEkXpIq9ynMalhTspRy71thgX7jts82llwe5_eKCM2thB4JUYeqfSsWk7t-E7-jq7lhXf4a24TWrywpvoGAgRLZLvTHHrq3Cu23rQ3wHlLIJOquIYWRxMaNjfgWr3m37P4LXntgnNUZFv0CRB5knyU0KcW0zbNtw7yS1eHJcPBi69o7p__5PnFf6-kdpxS2QIL4sxL5QJRpyOm9-UEY-_ySWjzFMXYyLzKsaVi_nm8wbxSzlEdpYjflxb3GkVZlKMsIKo_TOOZefD_oFWM2aVDP3GNTYm_ztrGqzUjpnogviVCbVOUPj4G8qU44ftYErjCPisFZkI76naRnAZkX-1Ht13jEot3FfiQdHjVGsUGniAszfoQ7MiGEYTT_rBvlnqk4b0FpdBhlEKyz7Cd8NvwNWO1KUjayXLQRIHmBxfrXgPhyoGgoVYkK6N_aMvuLxxlZv41xRzfK2egMbjFCGCguHzOeohHSINOY4ZGGzjlUA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9IzMAAF66ZgCXXO9afIrC1W6AKAImZa7zResuMOsAzoB854N86n5Rm5oMj25666bAm5YfWuJPzMU20_lxYRwBNKpYZAsIXxP4gX0PcklA9T4RbwNG5v5J1fg6qNM5ZWedWerD8UbndFNtQuv7GHJgh7UV2oz3U7e-z79VuuF0Hrw,,&b64e=1&sign=eca665af16fa2b9436100d51c3e705d2&keyno=1'
                },
                photo: {
                    width: 800,
                    height: 420,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/480326/market_XdNv9IOZFpof0S1apKb4lw/orig'
                },
                delivery: {
                    price: {
                        value: '300'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 300 руб.',
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
                                daysFrom: 0,
                                daysTo: 2
                            },
                            brief: 'до&nbsp;2 дней'
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
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/0tsmcTdO2LFjqrn_hdUEZw?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2TbXlZREzGdJVT-KCI48CVrcFP79A47SYSdkLTdYPl-g9iazBWpVAxpD2NxuNF2Qwq7sSXqr_aIg5ky0AY0m_QYXzMBNAl4mIbjRVaDCfVPsOSie3JnAkaI&lr=213',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=375225',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 800,
                        height: 420,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/480326/market_XdNv9IOZFpof0S1apKb4lw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 99,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/480326/market_XdNv9IOZFpof0S1apKb4lw/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFPYykbafvgK0GPi8VMdtoEXJBEoOXsc3-uPb0X5wM0Aw',
                wareMd5: '_rNpjQfmfi8mHcROAyeOBw',
                name: 'Apple iPhone SE 64Gb Rose Gold',
                description: 'Купив смартфон Apple iPhone SE 64Gb Rose Gold в магазине Phone-zone.ru, Вы получите 365 дней гарантии, 14 дней обмен/возврат, 20 пунктов выдачи в Москве и более 70 по всей России, высококачественный сервис и многое другое.',
                price: {
                    value: '46990'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ12SPSMuUeD9-ZRtBKLS2jGj0U-3UhtGTI6vFWpnBRKjfcoJ4tDVGw5YU4kiq6huz4WNcQCtyEdKcdISvbfFCIjlkMXV0tUbyIplVT87LZeUt6oBBzdeq1rLrxNkBsqMSw8d9QTbAQ0mXEBls_v2uX6QKTJmEuyaBAZGjYJf8aO0G62tT08SZVmXY9Z5xUvGowQJkHrOmrg1hvH2YAmXeuqM1x463N1FRTwgBx-QQJll37vkTq5IjhYdVJT5zEdx8Zffp5LkrVncKKfmvE9V594S4XITtJpxcwFsf3zZqSqfSXA1dsjuXJePGYIcl3a0sWFDgsmsKV2TYp7DWClNgLz6TSCMz8fhb76eNT0GulOT0jGDRpG5J3Y3FjOL3IOm6gJdwj-l_SLEKEeOiAo5VocegMMNE7ILtK7Qk6CjYD3w6Hf8qEejbhIQdtBOpgBiueikfCRyKFdQTmMcmuH9D9OVzKZkMaD79dcdJhxcHTKG0YW2pdBH6mMKK9qRtH28rZtRpcGtpRakj4dx9mvQi5ZCcg-W8x6J1L5wumXDjYeh8coPEGCZaHR0fVYiDogh_AbOUJsI6Opco5GIO8ILxs6hCCLZCs6GDckaog3472qvqzRQeJ4-Fk6xXecsD_K0g3vCim5nyPhcoUW7HAQqChnN3qBK_2q7TlgEx-pYQ7AScZy0M03aau9?data=QVyKqSPyGQwwaFPWqjjgNiwG2d0GeXPj8YyKqTMME98_Toe1rB6Z3xmZVqnXly7Q8UfLkTrnh6ZkeXkwTQnPh9Zza1uVNOJTCz7BY0XgZj_pNLA_iPBnvZDuGeF08fxHYNtltqL5ElnFYLUCrLoAnVpS0yPfRptt_9_PhSRN3MEH_-CJHAL3AlvXZdXNyimv60rF-H2Mi2EeZhqDZnKmGH_4sXM8CScJSTG0BCyAtWZy_SqESZvSJvScdYV7VigLsmTmVS2fZ-BJjibzsC_4xJ5QzvNKHlw5tvtl63RxEmQIoHIrX3zKi_rQcCGTrRo-&b64e=1&sign=1641f7a07d9e471beda68b23fc7ea242&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVL74jCRl2732D8LkfebSn32A-hl5Kwa9nrSjHmOkdB2Y8ZYNVNND7XM89u7fBg2vxu0CFX7WGqo4WwR_Hv8_0lVqp7okEtgnZ3zKp2WcilxJRXGsnFv2HGCCEWpSLlYC-lkwn9fYxU5Ba4fMNHd4ZswiGTSweKU3DsXs6glWweykyqI0gPFXwowYrrvT_FDa1nKfqjc9LhIjTU555u-7mlY8VVfvaFZiTTLc2ph5mvEoLjC0ukymohswoQ7Eqc-jRV-cS8z3oKNDW1sNoM05W-Y2eCgYMxFifSgnuq_WgtNvti1eCIecOAG_TJzLqzYNdh0u_-ZteuHPmKc_HABBwLiSDpOYydmoPMqdZraK26zcRogIkeijRtqUszMiletpH7Q7YaaazUIsqWfUQXPG0d0ybgJV7FGlJMgLJesekgOyPbvacKPXUwdFOMvrkpFDMPrKrB80HIkuA5CGOja8ySh-jeblwqH6q6qf2HRT98MDqV0jzzcelHXs3MO0E22Xg9B5IDtrIpAHWuMosYaDVrlZH6FHo7xRNgbvLNNhQsUcHqjZ6UJiwTLQc4Xo3gUUD5370-hOR_t0XGLt1S7gBz5A99SfmvlX3JNxGm5zeNm2FNHGadA0nZqtr2HAh3d_vAE3QlFiN4jEby0P1maS742l2rUtFMqXbavYZmG9cDq15CmnxmXcecZLt8xk6o5Sfj7OIFdy15Ck7fyUmwK_7rpogGd-7oUm6Unx8c3QBOLQYRe_7cX61FBpsPYctRuPuqT66nsUAnRdoFIAHCGhgCLrO4euAdUvLRt410XBfLbnD_RNMBahAFeNWWGN324G-PdFdiOKuR0FurLt6BYoGHdVVr0U0P-4IA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2W6htYalT8grrBLtMOaXyEq2VayTVB31bqMTiMa-cEM6lG2HOOUoE36kXS9wlroxalIfqlzq8CRZSwweiA-9Au_6o_NHGis_xtRnwyZ8JOCp02Kgk5tw_60,&b64e=1&sign=3ac743858e401844ac71a23e163dde97&keyno=1',
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
                        count: 640,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 19,
                                percent: 3
                            },
                            {
                                value: 2,
                                count: 6,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 13,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 35,
                                percent: 5
                            },
                            {
                                value: 5,
                                count: 567,
                                percent: 89
                            }
                        ]
                    },
                    id: 296652,
                    name: 'Phone-zone.ru',
                    domain: 'phone-zone.ru',
                    registered: '2015-06-10',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Пятницкое шоссе, дом 24, строение 1, 125310',
                    opinionUrl: 'https://market.yandex.ru/shop/296652/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                phone: {
                    number: '+7 495 055-25-35',
                    sanitized: '+74950552535',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ3CPnBHK6hwCSpdX5e7CLQLYCWlNEazugP5TFsC2QIS8B4OE5glVOWOOypMpyb_KDjt3S7PbUmvHWehDs3QneDyAA1vmtyDJ1S6HJmFTA-25qnDsYqbGyysrG7NbqkeYGu4BfeVqtAXyi9u1UEUSJiJ58ShUVYKHlWBtCYmu0ogBEj6e-u3QQwTUIvmVWnX0KQo3Mv2GzyYdaqLjChFAKE1IlfyTrOVcHXCcXMkKUWwCE8fhmKyRRjRo_qISN1dHs3hPJ49gCr_m_q0FkblIvoZEHy7Yh_s7Wj_vFZ_0nRAFFRHDbtq-e4mjZTUwItwNrOrnNOI5g6k_eJhy2LNOc1miyBDYHJfUzSvR61z6bLSs6evICmk9n5AqR4xSZsW0flLfjTenVy7ZlqOXOTOuk13F4hj3eW0TwMdAQGhJUARB2nl_a9VJ4l6zaM-lAElFAPEuTv4WmbPFoFEihAC2mTpBplai68vbYliyGrut7a3Nz9wXRLWGI7yfchSxkT9Voc9cljrQ79jc_FixMrcpXIeG43j1JNjaxBXtNioXibeb0jhHJudPfZnWRMad2UaqJzInE3yfGtUjiJjFuOr-fYLD6T4G6lMvekICecRkCYAUcuVH4fjL8kMQUuFIwJo2K6ytX6PEswEv4a1WualKo5OXiQ8oOQCtgrerSrvOZw5ETa4Vsc5hYHr?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8_IwI-XQltjuU9Hql92Oq6VuqWydSBfr_G6QrILtO2QzB6YGGj1WYYUBQdyZF-kpVjoBX8Ion3gXhV-dCWHt2RMGmqoIsYuLL_CSH9avtzktHnCq88GUiBlTR70DaU7s6rmUxGjvvnEMzDftbtAfuoyUu8oGYVQK3EIjBXqAisyg,,&b64e=1&sign=fa6c5dc4300a915987e13c800429d250&keyno=1'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                                    value: '299'
                                }
                            },
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
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/_rNpjQfmfi8mHcROAyeOBw?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2Qu30Ieet3Z4MeifnzzH5ViOl2KZ1WlMdN084wXMqDYaYKczfvQOUIv88h53ogHxy2eqlJfRw-hZxcLhqtQy6QryOa3vRsfB0OlNhXlIXbdX-j2ZMxnb-jO&lr=213',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=296652',
                paymentOptions: {
                    canPayByCard: false
                }
            },
            {
                id: 'yDpJekrrgZEs9bytUE_fjjcY-6rM6A-5458FiriJv0TMEaGZ8j-r1g',
                wareMd5: '2sUyPvFRlHeSkRkhQ7viNg',
                name: 'Apple iPhone SE 64Gb Gold',
                description: 'Купив смартфон Apple iPhone SE 64Gb Gold в магазине Phone-zone.ru, Вы получите 365 дней гарантии, 14 дней обмен/возврат, 20 пунктов выдачи в Москве и более 70 по всей России, высококачественный сервис и многое другое.',
                price: {
                    value: '46990'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mUz8wgiS2_KMLNrDgZ2KpunF1NfG2RiQoqkgA-Ob6OOQX3mh6x7KGr1eKTJLLfNEVFNwQJf0OYODayy6fEkEs_D2fKc5fET0ASp6sVvyAGzSenFODLYiyLzAjve2KtPRZdg2PpQEFCJT0YSxSUxduSfdxx9UwfUJueNs8TVEPtJ1Jk0q_CopkhS4GkHWFiDshC8R3Z8hYqJcSgUoWJ91VPbpkHoGMmqKMb_VNysGQDqVyNqRT1v1E4Ao2D9xv7Kc8mc2TyI6Loyt7rr-mQmBCr0z4IoU7JZUS2l3IGK6I262JHyKhByGvQZFf9LdRdAcjRzneAniTi5NRutdAVBmiNQ-ww1VwcW6MDPCG1jYflLnuOqiKQp13zukDSdj1p39VDRKY-zQi37DaMFWXngiW9b5hi5bxy_OW6kHwEPXEONJCyQSNGyVXyfJaCdT5mhBi-0txquq_Hv48Gq0HI6Qk6y_Vl0Q1ddzP7GkJywBIuzj6NnDaKdD79baZsy_VrO_gkDrDjZVzDuSkaY-jXdc-p9CD80fFqLYKBPfofmgKlClYnRHRn-XXaCr0ob41daiCu6FHbtRKoQgBVN9WSol3GL3wO_WmfY68673YIRIxLCrWI6Rk52aL2JiZMK2vIQxcRkL8STAbd3qerGPPyYUe9yWS8uipAizOOFWmyt0gwDjdYpPB37Tsh2OGH77v22ylt8sBqlGrhE7DYosak-OWwxKALoo36O5exgRs6z_yelWIAp71VjxK09rT_KorsAgjqORLJIo4bq-yH9L9x8hMeb3U2p_k-2U385C5e4YHDgkJ0BqY-9eu-24FzqunTffoGVexnGr9WhL3l-qnBAUDRrN7Kkr2iNQPjfvSGLtp-ty?data=QVyKqSPyGQwwaFPWqjjgNiwG2d0GeXPj8YyKqTMME98_Toe1rB6Z3xmZVqnXly7Q8UfLkTrnh6ZkeXkwTQnPh9Zza1uVNOJTCz7BY0XgZj_pNLA_iPBnvc8pZXpUoqCqQn1b6qwpIldjK_pvVQeEyE7AAqi5zSJbIan4NSOS0PP98eQE43impDyVxQCUs-i9CHh7rgKmuGQVKHGIylYT93KxKtW1hDww9HJ0eZkL3pRxp6HPlLJxJoL44NsnCaXBJP9iblf_0tH3eaR6JmbAoJvRsH77EmRpdiFEWGYCEz1OqNvbxeOw4w,,&b64e=1&sign=184ba1c8ff799acf22f3224423b2039f&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRu4Y2f_crk7D3BAKE8SpemYMKmJR_QhGPZ_v57flysA0werlZoMqW1JTNJFuqrffy8K_SYbmgaLARqhVwEMwcJ7LmwtrBW-oWULod9CYSdiGIVSip2yrz9RdQEsJ9f4cMl4GVl5viti0dyItnPQrcVEWfXPHNnNIaPLTNJ_lZxat9Cm4WwC9hHbbNXZSd2ch-ESC78NNYOld4fKJaYTTXSFoxnVN4Jgkg0L-2gbLWqAObVh8iR9kMdMZBVsNlYma8FJuPXrb43TstFo7pb-lqSJUurfs1oxkHIyJh2_Ba5_obA4pB3DE304zDif8y397GriAjEAcPZqUUZpcccrv9LMRpSj2VKmGFbPhDDtKq6auYbO4-7Ik8hycza0h9Th251tWXknsBujiawBEZWt_37c9HXsZB6IejhOQjU_A9aSERBXLLMcQXPuYcUpdEox6EZwOCqMVnikDvUgDZkDMO6tOJ7rtNTIIuh2Wx8MAS_IK4AC9Kj_200ukavF0pJgoBzOCFlH5oLiDlKeqiFhdl5v5P2-5XagdpANPHRdtI5qzinybmPk7h0LBUfEI_O0LPIXs692Xp5pvG11mjFiWfM-KMYC4rBs0IZHViweKMwRbqi7pao8He3yROLV43JZahsi1BlScoxUbzdQbMv7hKFFH_lI7y2SEQPCurJ_zIfC8B_QZQ4tPx7nwaVnDX_6wNwLQiIgxwgCKJpOh9ctJJKkkrIlV4-_NTNm0HwCGQjzmqar1G8NZ-lUulP33QSYp9I-F-s9khE2zZy4fnE8EhVqn59FWU-rt0tGrZphtDRSVYqaX0BuYCCv9i15wrldLRKZYZ2I-0P7N8zfFPg3lHR55Dd3DQshLpPSzediOHNYng,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2W6htYalT8grrBLtMOaXyEq2VayTVB31bqMTiMa-cEM6lG2HOOUoE35K1FRmMSlmQHpcNAaeDHQds63aaa2b5j3ORH1yWcnBiJ5w0yvRT7bdj2k6xuN-llw,&b64e=1&sign=b75d0549b7d2c3386ad1f55fe9369e29&keyno=1',
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
                        count: 640,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 19,
                                percent: 3
                            },
                            {
                                value: 2,
                                count: 6,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 13,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 35,
                                percent: 5
                            },
                            {
                                value: 5,
                                count: 567,
                                percent: 89
                            }
                        ]
                    },
                    id: 296652,
                    name: 'Phone-zone.ru',
                    domain: 'phone-zone.ru',
                    registered: '2015-06-10',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Пятницкое шоссе, дом 24, строение 1, 125310',
                    opinionUrl: 'https://market.yandex.ru/shop/296652/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                phone: {
                    number: '+7 495 055-25-35',
                    sanitized: '+74950552535',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mUz8wgiS2_KMLNrDgZ2KpunF1NfG2RiQoqkgA-Ob6OOQX3mh6x7KGr1eKTJLLfNEVFNwQJf0OYODayy6fEkEs_D2fKc5fET0ASp6sVvyAGzSenFODLYiyLzAjve2KtPRZdg2PpQEFCJT0YSxSUxduSfdxx9UwfUJueNs8TVEPtJ1Jk0q_CopkhQFXBlBToNeQR1wh0e_s3x3PDAnyQp0HscdrdrV35pAxJzSQ1MYg_CkQ-Z-B3PzF7R49VtQoUENo73iRo5-9_nTESEieb4IKF3TrVSUQ3l_WaE_RJaM8-k76byIHuyyGnruT4WO8gU6xyj6WgqUc1c9bzIFDjGHfk6CyQ6xWe6qtIauDfaxDVIDXoOvvk-MCa0OJ7rHheSVd_EzldpKokMj0dp2PrydfHCd62RJbn6tZuwn69QkPHEwvPB4L8mJSH41FLhtMFMDX3d3-mhcwDlAipZj38HvmCKmGxOvDicel5rg5UyRwkfSQsnPO_L7srKaS43iR7XXkCQQ-ovj7qAyRzR6-tYdWiRJ4R_BrSFiU0LL-Ds9qQT30Nz8XdU4jlpnW9XlmUaAMCcE5Edhv-NpDQRq9g3HxwG-S6ogxVx9lRRJWDZYUK1FDBIiE4oCkbFsD9Nf1NKEMaDHWtlPdx8Qu18pVe7ilAYDp6b4gJRFLGDtzCQSZT1qr4qSy12ufwutl3eAtJEdGyOM-5lro8IJJxJJJgvcWSId1MndFu1F2bYHIzmxIf8wvdJuEOHkl5UpFLZr0TUWtuCtMwjilDyLBdpvY0DW9IrEkfsBXzT6pT7-FGWVo-c_9YJifXVRUVjuoJxM72hr_E0X8jqEsXOQkVYpHPwG_vbB2vZ-I5sC4ahGci5UKt17?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-pWL55tEEEr_VicQhWPHz0Vl5pNJce2dhIGskTVOb_C_Olj0ePo31eHNmJjeb9SNqi54sRmScUYzV-aGTw4aOY76WiclKofLXW-ZUh7DC3RYcb-NQORJXRJ_cuiGk0XK67LljOEtJaXpTAx4NrGnvjYsb4AZaBp06H4-YrBtDavw,,&b64e=1&sign=35ed6995aafa2afd7949191a121db393&keyno=1'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                                    value: '299'
                                }
                            },
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
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/2sUyPvFRlHeSkRkhQ7viNg?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2QsGHH-bRmGNl78VIB8nbA6ysA4mHexiL3wlO-53LKqjlJl-fbvFVpkVSx80W5wf3Csmche7Qew4VMRLAYPjcCTDGB-Y5ekHiRxInAfah9H6IhoFDMEgZ3k&lr=213',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=296652',
                paymentOptions: {
                    canPayByCard: false
                }
            },
            {
                id: 'yDpJekrrgZHaCEkQ9tbba35BGgv4jSDMUSReN1gZGMp_g2G-hkwI3w',
                wareMd5: 'fnKMk0fFKAtbQhUGZ6Xk9A',
                name: 'Apple iPhone SE 64gb Silver',
                description: 'Лучшее воплощение малой формы. iPhone SE – самый производительный 4-дюймовый смартфон в мире, аналогов которому пока что нет. Обладая уже полюбившимся многим дизайном, iPhone SE демонстрирует кардинально иное содержание: передовой мощный процессор А9 (аналогично модели 6s) и отличную 12-мегапиксельную камеру, позволяющую снимать панорамные фото, а также видео в качестве 4К. Функция Live Photos оживляет ваши снимки, создавая эффект присутствия.',
                price: {
                    value: '25890',
                    discount: '16',
                    base: '30990'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8meCLpgL1CiaCKqPkEYO9TM30kkfmPjuFvfJpH9b2wRk_uKg1LrJaC0LSwwmzOhiEFaXT9U9RDnTtsyNcvpDjoSsqkqgnHH1vFffGi1wPc09fKmCQT7EZxGHfx-MGZdj6Xu4lXAtxuje4FPSRopIywTahVcvH9VZd3mhScPNSIee8DKQ8H6bCAj25gh78RYZGjpyIVWE9k2L9Jp9sJpujuuPaqDWmw0vSot2isnEDoVTJv8n8NhrnYwFR2Y29HBCV8L0kIbUVsY8XnbN77cqGusi78V0PO-Knxa_1wuILsIemxaRoyhRNXtByTs_7qF8JhDhiHcH09BQQhMRxK1zMtB4fROcn61ZL8UdGyadyhC5z5wRhjV6dhtefBxKLfUHFh8qLo_YtdhP91pQHcLJHkAXyZiq06KhKndfofHl_pMlFpGwDTzXuPYPDdE3BWunyezJBREGeZILbRmjc23Wqob33weJ-dtRo53F4lJzQ_UAWWRwc_vc73e68ELI2lIdjGCD515qi2tEU3Bi1Anh2f9zDK0FwbLhbjwO5oKBPgCo6KlV1iXaieQV3Rw3jv0_NlXBjT5BJoqQpayqY3EM7jxE8N-BQ2p4f2PGt2HMuSRmBvtpUa31-ksAmzMn27QVdh2i2qYe3bhio9Q7miG2KQ563DyVTI0V_0R-X02cBhJb0WG99jzU03N4wEpoXhw5ihdjlqGY6xzP7M2nEhEeOyoP5BXy7XyNurjagTBV8f4UwdweddWNQUqdKf-D1-_xJgq93F3YZUvAmuNUi0gedQOAmruDilz3IUmT57YYE1A_yDdITpVGXGwnSz6uo0XqDtDuTWDMO2jonwCHHZsksayrlNv4P3K0Qjg,,?data=QVyKqSPyGQwNvdoowNEPjdMnGw2AwFdOUTgKDAUMZB9IiYRvnvVReaH_cPLMUCsK1c29vJUyS9p5okK2gAu6Q_ltPUTStrO9Y5woUY4wVKahZbE0PHdHN30woONpruISLnBEINHMlghO2DjT1E0d9EOnbswvMge4&b64e=1&sign=c33f317d1c6e284275afc7fa30d908ca&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRtNkb9mq-LhCEcX5B0AAD_jcCZ4pPf-gxJVL3-gDUSnrEhjFs-ShCqSZT9ANpxekkDKGY_8W3QP28_gOZvKh60Vx-p9gMyx5qI8VLG9wTvcg1bogmin1FbGuny06aQ_WG7uwV0OXN61fGipTgALFomyBoELHMIW1Wk5TN2BbnCOJo4oZqc_JPGZHC_ZF7-IQAnSQXE5PbS8dWzQCqtIEitZ8SRAAnpWeKhhGYqAmtQQxXlVQbKkezKPzkF4ohVlfi69OGSkbvG9wspYHo8CsPBT3iypXYxsXmoz3oQfrzctlbVtgEvNezc5sjN2xqz5Vx5zsrVcCugaa2mz-hZqLYkhbmECSUr_NUkKjerbHKoenholV8ublLnKq2gzbFKpVzjEQCn4NMeV-cjctgRCovAluHXGGKZ1i9pscHnuolOL_B9RzEevwc9jF1D-aqaO1KjA13QuKN-0S54qBtLratjrdetRLLp6tOhoJVTMyLD4y5XxtjBE3ULCs7__UUmqZzlhikTbUOcpkrvhks8ujWiWsnaEQA3jIQ_YkLzy_enrvDVQmZhLYH5lCRfcWxzz6_ROB3LlxrHpu0K_ki4gemMFigKISwypQtb2BIV2rngo-r9WNN7APFUGLsdZX6ugDhdgMEnBHqwzroYezJpBGMWlljJUjVswi_8bHU0BzQ4rzKMyJlXcXr4d6-PhEUaxxeq_XEEq0t41QuWgWu4dRSgM1vDWwIw6GhsYRxfN1nNm6o82VoT-nO3gATVxthhljFRnSV80luIgUFm6Un_SAK7QsEtfXjijTBp3M4-lLbnF5YLYecazha3JYEjncTl45QNeq-twLkyTGWczguiOe_FqQ0lFvy2arW1JXXfL30Z_WA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2SHJCIWXcrAiRKRsunOIfOrS-vsK5QHx3kim2QX_EvzJBoTJlQqQ2Se0dC7BKBp4IaSiyvo3Ar5UGA1lMKCbCsdwNdY_wCOvFqsdlVYcCLCqs8UAa4pVqXk,&b64e=1&sign=7bfd35fa2136f7955479218428e35660&keyno=1',
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
                        count: 642,
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
                                count: 6,
                                percent: 1
                            },
                            {
                                value: 5,
                                count: 636,
                                percent: 99
                            }
                        ]
                    },
                    id: 77535,
                    name: 'save and sale',
                    domain: 'savensale.ru',
                    registered: '2011-10-09',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Буденного проспект, дом 53, строение 2, павильон Д-19, 105275',
                    opinionUrl: 'https://market.yandex.ru/shop/77535/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                phone: {
                    number: '+7 495 664 67 77',
                    sanitized: '+74956646777',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8meCLpgL1CiaCKqPkEYO9TM30kkfmPjuFvfJpH9b2wRk_uKg1LrJaC0LSwwmzOhiEFaXT9U9RDnTtsyNcvpDjoSsqkqgnHH1vFffGi1wPc09fKmCQT7EZxGHfx-MGZdj6Xu4lXAtxuje4FPSRopIywTahVcvH9VZd3mhScPNSIee8DKQ8H6bCAj31lJ30JVHi-44CZtxhHwJVNy6rpkTvDAhHQYj1ZYnNch4z--XcoZBOZmNgKBQm8JkvuBIiFMxCcvChR-C9X6rltC7UbiiwS5fC_Vh0C_6foyw7XsM2rH9sw2bqP91BxhuHpyARzA6eCRwDACIzxLcwT_oKg_4xCwW2S_jMw3y5CrVczWaAthHC89QWKYmtmmacxdlzMrbY6eLzruwfk8NHw27wR4d4KJbX2yagP6tPX6WWwxtQw31o9yrl7wd8Jupx8k0JpTLct5-1TF17YTUQ7nPLExzo7Jqx3vLwG-TsX0HA2Wx3QODh_xeHvZcfj64f6UrqC3_z9DhJYLoGuFyOJroCfR-ivnpPLHQ09kOyZ3hL7PR9I2uDS0kvhRW7pJK4-wJfmHCnfRgvTXNQyIkAx-BDBWnVfftQM89Tbws_Lg_SypnTREC6GIl4wk8IjuD6GePbXC3QeBLGFzZvhA1OEnN-l5duT-Scc4Qk-lnZZTbqpsMFiZQfgapcXfHLtcQsscq20sHZTKq1N00DCDyYOFonScXzPHHzNBWE4-dNN7zQ3Efahiiu_ULk2o9v95gaoGcKYo1f5DJHjl-L8AdUClbnvBz8yV2TRDqJQuUgAr-qci9Rmx5y-c4ZrInkVSv5G7JhscspPSVx2OtVXrH7qzOVheQu2J7gAm8eaXCieA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9VGYiuXntfyv2CcB6boRPsI5X-tXp9JTFEIvGvxKkg6n-XMn7ctxjTx1t5uiSM-lqLsGSnMRm0j_veV8GpAIehC2Ur2ZSoyeQJ2_DJcF5aEK2cGNMQYku3FLzQseigyLZbaVYjlrZeU2RQsYot69rsjQo7H5DhNUnb5RdkxQ1wqQ,,&b64e=1&sign=7d73e1985c78bbfdb2eb4b90679f8548&keyno=1'
                },
                photo: {
                    width: 100,
                    height: 100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/203248/market_eFn1mZPw1RfwDQBphdZAoA/orig'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                                    value: '300'
                                }
                            },
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
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/fnKMk0fFKAtbQhUGZ6Xk9A?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2R1EqOGOQYrquSZWej1fZXGMMGBDjFGRm4Uiae7lvQmrAua3ukUBVkhb0K6EBNHNj2GRS9X7hfTJMbHw9cfgNn0GRPyizgPpwtaClVDOwWhSwF2QixP7vcw&lr=213',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=77535',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 100,
                        height: 100,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/203248/market_eFn1mZPw1RfwDQBphdZAoA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 100,
                        height: 100,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/203248/market_eFn1mZPw1RfwDQBphdZAoA/74x100'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZE3wEazCwq2aVHyVKc8a1FJrIxx-1TJRx51jUfO3iEwaQ',
                wareMd5: 'ZlVkthqbqfnJu-YBZcK-cg',
                name: 'Apple iPhone SE 64Gb Space Grey',
                description: 'Купив смартфон Apple iPhone SE 64Gb Space Gray в магазине Phone-zone.ru, Вы получите 365 дней гарантии, 14 дней обмен/возврат, 20 пунктов выдачи в Москве и более 70 по всей России, высококачественный сервис и многое другое.',
                price: {
                    value: '46990'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mUz8wgiS2_KMLNrDgZ2KpunF1NfG2RiQoqkgA-Ob6OOQX3mh6x7KGr1eKTJLLfNEVFNwQJf0OYODayy6fEkEs_D2fKc5fET0ASp6sVvyAGzSenFODLYiyLzAjve2KtPRZdg2PpQEFCJT0YSxSUxduSfdxx9UwfUJueNs8TVEPtJ1Jk0q_CopkhTA9mIwEY24ad7ePuGrQh0FF2dbTCRWfTz3D7of8iUY6OSikfsEcdMu_v-wedXC06-ArcRCHeup3_0lPIf3v1Onjd1WsFAOg-GfPbsq7-2IYl7O5el1sNHBLefBEep9CP7VrwIwwaq58BCtVij_WHbcfEMrUApuPbYUDi4-tdDaPkOUy0y8ZPSBy-r6l5EfK4oWHT5seXMW7WTanU8_PTvh8uSdGSKswDSvJIGgDgF9cOwYuK9eo7TEnxQUkJSjuwF4xbN-RubL3vMSJagp80KY18af8JImqj9TKFBEQ4fpms_7PQ_2wHI1jBR75erp-Y0njo2h-wqAmD09Erzl0wBzkrxKAI7BI1YLdBr4ONQpWSPHUuc9RwFPFhQy4g5enyt4rVq9Ik7k_h_kyXUMWix4RhxqdbJxWiHs4dPtVolVK5S2zpNC1fauvyjyIYSUf6h9ItMbZRqP89YP0KE-X_cw1cuw0Xl3lYiJopxMeLojVJc9zYOwJDkjn-rCJ119SUxKrLaf--mbOEj38EiaaoOlthFzqksEJ5vhxTa-gNDgAiakAALCvsEaiUV8b5_2RET0FcegH-lO_-vBX_bMX0wCVLBHvu3CvWQBQyzYW9hBzalmK4SlfBCXUWoVEu23aG3pyrRB7KBWmcd8UfCz9-YFSkUQ1LsnwD_gg9jc0nIKKJOyYKioetjB?data=QVyKqSPyGQwwaFPWqjjgNiwG2d0GeXPj8YyKqTMME98_Toe1rB6Z3xmZVqnXly7Q8UfLkTrnh6ZkeXkwTQnPh9Zza1uVNOJTCz7BY0XgZj_pNLA_iPBnvX6nRjSEwVjMVgvL6iAgj-rjC17rlTfmPqmEMd2Koqq9NHuA-izKVHlT5sDICquU2E_nk__vCylSin6UFvY0378xinRe6nVJNc3SM16FrY9G9uYkwfGaocILCLKmHQbwFBdFyimKNdibZpGjwDkbksdAQy9K-fHuEWgdI5jNHbJnodLCh8b8saTp9Z8CdBy2a6QdKOCiYp_C&b64e=1&sign=5cd293b8f82c177066027d45c108bd9e&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRu4Y2f_crk7D3BAKE8SpemYMKmJR_QhGPZ_v57flysA0werlZoMqW1JTNJFuqrffy8K_SYbmgaLARqhVwEMwcJ7LmwtrBW-oWULod9CYSdiGIVSip2yrz9RdQEsJ9f4cMl4GVl5viti0dyItnPQrcVEWfXPHNnNIaPLTNJ_lZxat9Cm4WwC9hHbbNXZSd2ch-FFEgaJFnDNlTMxxAIwBTT2yau7d81rieFhb7s0w7X2rauLU4Hr8ooVdTUGUQ6seVVNnZtvKijq2TQWcYT143PBtY9iyY5MUk-YHaWRCCcIeU7ZSAl_chlvJDtddmsrhmGU4glcSKYmTgiwQs0PWx8Vgk5O0Ki23ucubFRJ4xaa8xEQYc4yXZaBjIb0m99do3kH2eB67zer2OWUKOH95LLRjW0ojZDyl6pUQ99jqrI1oQmp0q4XpHy8ZwUd6ivF2oly6Nf0o1rs-8YxLAQb1tXldngxG1hfTn-EYzI3UL4i6bqFYnc4obxXUjeKHsDJsJ5UC45we1Mz3gPvE0p54PlJZ6UrEkphCCTWwnte7-edQyh2_5m-7tSW-mFWU-dZ9zKC3PJc5rBmsrhVhQUi2F0geTFvgV_XwnLfKyJyPIQ7GY0GXGFoKvoPNiFe7ODHTbv8RkP6hJnTY_5gc8TQBgirbM2rSKv223GbS4p8eKMG7ulv9UwtsGm5sMbwE_Esbl8Jc263vQInjuJE5nGUjkLezEOh-Dn30r64_1W5GcyPLr5lJ27WN9VDnAdYYErzuy4r4azkn9KGB5OeQ0UiWP1at5sR1yKEvm4zc-oeLOsj_VIadSzT4pCmnGjPj8KDbog-y2R9tvJbLTIupgFEnrLlI_HmVBlS6SqelQKM2nAlXg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2W6htYalT8grrBLtMOaXyEq2VayTVB31bqMTiMa-cEM6lG2HOOUoE37uquG3J36cYL6YDbAkUeEjz4LSYfO8rwtbVneiksKRchAiDyB71VtFceVYgsXgPAU,&b64e=1&sign=115e1b480d2e835799b2499e87fc9116&keyno=1',
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
                        count: 640,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 19,
                                percent: 3
                            },
                            {
                                value: 2,
                                count: 6,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 13,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 35,
                                percent: 5
                            },
                            {
                                value: 5,
                                count: 567,
                                percent: 89
                            }
                        ]
                    },
                    id: 296652,
                    name: 'Phone-zone.ru',
                    domain: 'phone-zone.ru',
                    registered: '2015-06-10',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Пятницкое шоссе, дом 24, строение 1, 125310',
                    opinionUrl: 'https://market.yandex.ru/shop/296652/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                phone: {
                    number: '+7 495 055-25-35',
                    sanitized: '+74950552535',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mUz8wgiS2_KMLNrDgZ2KpunF1NfG2RiQoqkgA-Ob6OOQX3mh6x7KGr1eKTJLLfNEVFNwQJf0OYODayy6fEkEs_D2fKc5fET0ASp6sVvyAGzSenFODLYiyLzAjve2KtPRZdg2PpQEFCJT0YSxSUxduSfdxx9UwfUJueNs8TVEPtJ1Jk0q_CopkhQfdb4GXZXmAAvKhgV45f71BI3jpWzubrKdW-LI_iod9j2-DTgqhjn8XQhh-lFd05vGwTd-k_snTyquD8oOeMxyi-F0naBxrNrcly9mHUpWvT36QsuH7HkDOQ5poswKcAUmYWQJ8CzAcYjr6fDbX5-49N43uAHGEum3_C_VAs21f3txrUf236dyaeY_76KvAX4UBZdKDSbc75hn5VhEprZ74CzPJ_R14ySE2HM40jR9wfmsWFY888hE-PXw5yN6_H-jOqEbGuFUNDr-eigLibgUmYgGLoL8nJWpnMzm-SfCGD5Syfp4w0akgosOr7xFto_Vo_iexC66YebJ_QH4NwI-oHe2925loRKT3JRtZdu7oUMW0Z-AWzkEODqY_XKHjxean-TYreI1WuXb4i3Q-zgVF5WcO4HmcsnRBRe09eqnaMF0qfrwrQNcdI13_flhajRukoRMXWN3xDok6GHyj6edWiZxZAxELFOtmL96akXG6-8P1DM5l7zgRU5fQqkMyfXtI85fhKRt2w9rlrAQbfaQu7NtCEAbXbakys8Qlra22JAqFhbnWTRqK5mHn_17rvvVHMI1UugWoYo-vYSG7diw_18SF-dXYFO0hKxjkkpbqIgi_OFVTtH_lTfOMMrVJ758mfPzeA55d6tzaqzjFHz8R7ieFUAXDZnDGxlbVzZH-b4hF_0BTs3r?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_VkSnzCQTnO5sjvWo4Sxd7mk9Eg-WcS06dUs9ixAa36y0HRclYTD4XiuD0qjJx1XdLWh3G4HXWeoXwE6HdsUbHiDBe1ROZHW_YSwwrcgxd9EpN59Ttpul5i9e5UnQYQ89psUUR_X_2p8_y-MVomwm5A9RscbiPzCdfll7q2hNutA,,&b64e=1&sign=9cc3586e60cb258b95321186ad5a514e&keyno=1'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                                    value: '299'
                                }
                            },
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
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/ZlVkthqbqfnJu-YBZcK-cg?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2SWuh5Y5tNwdoo-ajIO4gvsrAK_sDvcPj-zVbQtRkGCnACL-8Lm_q_TtH9IFqJckP5WDkxpB55o94YXXYBIhweOPjh2Y2yKeGIhhgfURaYnC3lPwT5os7Jn&lr=213',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=296652',
                paymentOptions: {
                    canPayByCard: false
                }
            },
            {
                id: 'yDpJekrrgZEdObf0oA0hTC3DL_I8VmM-hvOZDKXzY4RqUiYtibkxbg',
                wareMd5: '21vmjv_LsWALnBX1s9ggWA',
                name: 'Apple iPhone SE 64Gb Silver',
                description: 'Купив смартфон Apple iPhone SE 64Gb Silver в магазине Phone-zone.ru, Вы получите 365 дней гарантии, 14 дней обмен/возврат, 20 пунктов выдачи в Москве и более 70 по всей России, высококачественный сервис и многое другое.',
                price: {
                    value: '46990'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mSW6qkPQe0DX9PQOdppKoeuw9Z5q7RRE8Svx-WqqbtnadNhS_QnpwGJcZBHzbNygqL9mRLnbrSp_sRG9c6YrcUzJ-G1fPIWFlvTL_0RxWWOocGcxDwLaX4LJfwkD4A2ScyamMS0yRo3XezVfQWJ74ObDt6A2IKY525q6JOTHvxofA6Fdg7Smbz_mMYaiqQg9GfPgzoTBLgaK43iUKOeNfwD5FxRBE0CcYBr-odeHVQKho2Cdqbrxvezhmv8LxShFC3rK5PikleU0tIcKR-Gm-8N8y9Nh82146dVakhEAvlJJlPskFyfhMaLDDV2tq6jwTkU0IS_oM7Jmsw7ICF0TnPkSaHf7w1UNMNilNFz1cZlg_aVcMt2kHD-01CXy-Xq2aUhVHGI_s_xZRWOee7n2TKlKjBdqJkxGbF_9y981EnNH8QRtyTjnlH9jrgY_-rAkbTdYcw1coy1hIok7ilZHqy7x-YZWZ91Bb49Fr5d4X3RG__81PWsLUoX38oU3q8R18aIXdb9NLnVnlMDu0DGl-IpkFU5n8_3UDWesagMjTkL3jS-lwSOE8NApy6flVVG3L4xx84K5ThhVT_Filtkms0Iel8EaYSDVAgRcTjbsELV1dhCZMVjlngsy8Hycf806_-kiAYJZ0qWl5BfYXwFPYuqUljJBtzZJXjwMTEdlhNVWWKpfIiw2yvt8PybhDSu_aQdmzy95vTpvqwyFlqyxOwu40NrIddbEcmJNz8w48Kft6ur8YwUp3I8qEXq_zF0tbIk7dNypK3zv2JOmqZOkrz1faXT1eok8ZD9oKHLsqfSyf3mF8LpsaQx5Rh5cpzMDyJJDjBBiUXCEuEEeTIoZdex6qv6OubU_PETbuY1Dt8rP?data=QVyKqSPyGQwwaFPWqjjgNiwG2d0GeXPj8YyKqTMME98_Toe1rB6Z3xmZVqnXly7Q8UfLkTrnh6ZkeXkwTQnPh9Zza1uVNOJTCz7BY0XgZj_pNLA_iPBnvXU8rXkhUMl-beR_IqctBq_kppwrw_B9aNeAM2nt1r4251cdibdX7vGvHS9ZsFv48h9C-pdQJ8pjUkiOLHZGSgQ2YfmGNXqqmh07F2Vf83Gv4JPw5KwCGDOqpNjfjbCsrrAlfuG4YwS5RoZXaYWDm_2OskEERrdzpBhG6j5j2ofsqToGRoQ8gHi3MSeGag_dGMxmTYb6zGRX&b64e=1&sign=5e64402649bab5b66e51a83477d83df7&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVKLFtGOBfLKEMRHTj_bl67Y2DS6Pxb1wTMv-D-srVnNPNbppyjGQWLAtivbSerU_GUpyJONu48e5WsaeiY0myco5e1CNKHOIZbQRKQfT47eh0dHrewQR6hD4pINCSraCV0FYROZcvujmeoOrZ5GoG9VhC8bP7b_fky4qqWH7oUllPs5k-7KtDTPgRztgkQTaBKryPegY4PkGlVoHnSnlzZCfB64L7d5w73N9XSD6GDnqdLoHn8XFk6ojP6P92QUcZiMGZnN7QMwf9mJbpvPg8OXGYixvgT6Y_crPM8TtJvuEie1noK1QXhb5roiyvN4Ivz4Mbc7viVaiHLW_bZKPXbvhj-e-JqeVTVJvug8Us_kpW3rSdXAsW4l_OauvR0_nrf2wz-99iKrTuxbsgVJutkeJ8sdEB2HwYx-56VE7DWpI5fvqyuYXkdbgfZj323N-sYiRpsNjD3dstNOSxhqHqGeBr8SqjCaoD8E3w3g-crM8sN6Teccn0TAkhMq08HrYdiMr8U8bug5rvjThn5CQbb4pEiUufZjH9K4OasMlGmnf8oNfwkOLRupSXCDbVHYGsAzuMpR_-CkT5r06JgxcJa5dfwdcCCnxXq-xMvraUWgk6HkSg5uSR4TO2AM8IQr7lo2B595dUXKoEuKFPAoz6Jyj2FVaBDmnkbXbuWKJ4DAyhsKUTSuzNLYZrgUfAqLKlcWNV0iMnyJx_-LdBzKZ1bUUtpsweO5Cmmu_iYUoatQBg03KyeBsjLt0dc1LXervoDaw_CYMXOFf4c7U-alCdFZDnYgSSyjfOXorLtn9871m5bHxAtHI4T21yV2MkpkZmpe1vcsIKi237hHpE3zsYQ1AIuzOo_8brg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2W6htYalT8grrBLtMOaXyEq2VayTVB31bqMTiMa-cEM6lG2HOOUoE35Z7VcxuS_S-9IDrrPBaCgoo0ck2lB1_0Axcor2twtjM6hpyVJBl-eMtPqyE0KwwgI,&b64e=1&sign=cdcd475e9706e2870d443fbfa3c6a15b&keyno=1',
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
                        count: 640,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 19,
                                percent: 3
                            },
                            {
                                value: 2,
                                count: 6,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 13,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 35,
                                percent: 5
                            },
                            {
                                value: 5,
                                count: 567,
                                percent: 89
                            }
                        ]
                    },
                    id: 296652,
                    name: 'Phone-zone.ru',
                    domain: 'phone-zone.ru',
                    registered: '2015-06-10',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Пятницкое шоссе, дом 24, строение 1, 125310',
                    opinionUrl: 'https://market.yandex.ru/shop/296652/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                phone: {
                    number: '+7 495 055-25-35',
                    sanitized: '+74950552535',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mSW6qkPQe0DX9PQOdppKoeuw9Z5q7RRE8Svx-WqqbtnadNhS_QnpwGJcZBHzbNygqL9mRLnbrSp_sRG9c6YrcUzJ-G1fPIWFlvTL_0RxWWOocGcxDwLaX4LJfwkD4A2ScyamMS0yRo3XezVfQWJ74ObDt6A2IKY525q6JOTHvxofA6Fdg7Smbz-2gNih0hdHgSx3SMF6UQIgJOdDFnsHKySRuWdVYVuFGasEqUOJMMokBCpBaXzRfK-jFuVcS6t6INSzS-zcxtdErG59_abl4NSoG2FIxfUv0s-TS4sdUFzjU0HJ9L-GIvwUD_2jIo2tZQstPa82_HC1IjESZUonln6gLOCA0VcIrSbyGUaTmWcUyst5i7KkZkzABKnsHmZVPCsAtq1Kq4EWi8B8oZ2tUs7doJwslGw0Qv4IJTV4OM5fa6Gi9Ub9pQXaG01YKEUNvXAdc8pbKiz4IfwMvNZru2Fuhij6eFe_KT8HjnfFsT0Nme1E1xH1hLhaZRPU8rLBiWeo7NA4hNmzctdqX4YfHY9-FqG_yYv5oYSEji0VEJP1jmT0cNuspA_L8CDT-k50UMARMC4EgwVrx4SR9JRFLMSj6vbQVPB0Zkp9b7lArNNUXvTjvdlldxdugIoSISlOWB_jlH2Z7AkADGdN2RrhVbOK0f_0Te7h9JSePObFGanTOwsXC2PtAthotJ1vz8EHnB8Hf-QV23sAmaPnMh8j6falb7AfuvoTvRJiMg15W2j_GmUxngfrgO5djrmYS3hiKQdQrPA5ADAb1tOYUE4MQio_uvDKZpiyXV73CRVlcZoypCm7N-7ANt_NcvXMi8QRD8zOlqdDoQIH59w-uDi0Yssbg91GibIXHpqoPEiGRNUI?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8rMdg5pCqa76jYFUZKUalQvJFTENKMJu5UpNFI8Foj598yzsuqAVf8_OJXhKk74EJO9B5tYgnZR9m4orTeiSmkeu8ik16rDwADfDit_6lb0YlHOBQbFahnbHfHGuXFk9aqMGDm8AxX5jOpoTDLXLd21L2dthleganmViemeA8VOw,,&b64e=1&sign=9736f6484b42d5a2db7e7f8607273b03&keyno=1'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                                    value: '299'
                                }
                            },
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
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/21vmjv_LsWALnBX1s9ggWA?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2QaDoBvlTNhf075AgIP9Hj7bLTorB463fUfiGiQPZUFcV8hHXO7s0VmugtgDpren51x5GsF_7T3dsONfGT_XU7X5UETQk8V1behzoyjbVVPPaXBTy-PXOdX&lr=213',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=296652',
                paymentOptions: {
                    canPayByCard: false
                }
            },
            {
                id: 'yDpJekrrgZFztD6WyzUvHlunueMn-6Sa7O4BtaDo1tB5b6t08UmMWfcdpi8DiYfj647w6UTbQCPKQ6Vfhef0rwO6VumcQxukaraAj1wvV1ft9QhogjhOZ_ZaH8hUGy_MJ4r8z-mJ-s7pEp5y74DWdkOcEfmm02rlT4J6Md0Jp3IttabvnD8Lt2ySPOi6ChrM4A1MM0XNbr3FWyojsV5HC6B6TV4ItVtJDTKwwJAgMxN6eqllPvyxgNZE4M8tcxe-C79I7w9nQZC2q3RS_6X7fH6GERbhQpz-LsG9LeIw_HA',
                wareMd5: 'bpC1Q5JvtTdlTvx_Osr-Pg',
                name: 'Apple iPhone SE 64Gb Rose Gold',
                description: '',
                price: {
                    value: '23990'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ1IE35NHsdlDPyOysEo0Q65PZXFDBsqFMblaYU7oO1mIxERG8nVi0uGTJI0s_SpAjq8fgQWNN8u5cl4zQkLj2d0Y_9_6kPzotTXr3WsJzr-6ARWvSzkemy5tJiv69FgZYN4tMuPCkn0tqypZrUVHQLlmfzKmJPI5e_SeR3nUUlsqKfya8UVWPK9egTh-bNl5iPeuw3OjMKrWkCSM6UPoi7nWiWota4BrPkHxfFm8GladuCbYFcxcYF0mlteyvcokvqy18GvoI4leta4FQQqKWdIah_t89VxMZZ3SyRhc9N9PiBGO9RV8mgwJQGDc_jICl9jouvEaDbprG3NXW3GLhGuzA7G8Q_vGBj_DVpC5r7seFDq5sUclQBT1mvO2N1UnBg5Qy03_pB0ACdX9vk_Kgu3tNdqk5LJ2HflyIEEtvRU9kaBr7WgWS2uF8QiCsgABdefeMshD6S59dHsnYI3uxsAWmpepA31Jx88uGY60El1dZIs6tlR2UXXeFk71wDcLxYfrluiggNFraimbXCeJQTqnQGZAqsEHJ7kOHB_x5GudiCRB65wntLQqqsyuk-gWeU62ZQXg7kbJA6wVc84330fVQu8HhdF4MIk0xCKG3rB700l2QKSl-RsbZZWXq7Zjz8ojEkDhSa-nfsDav_npwVKE373ybnmpuMNED97ONLLCA,,?data=QVyKqSPyGQwwaFPWqjjgNsR692ugY0jPYkvqWKcD46MDfdBSsZS6r2NcWc3E2dfmpeKYyybk09Oc9x1I4Y-3iOvzvgO6LlWhuVBAIflc9xp-FPkDEMEKrrxCZFkIYAk4U_wmZgoQtwVqnC0vSB3UmEGJvutiOSivrat0ExABP0LaKoBl8XiNEG2ci7ck3XHq6zxj3EJHsFfU8FPg5plof3bPCFCdtVx3zSGDb9_YBH5PJohOFuKzKKLpuYL2fyGZuVAsIVQEeRnMly1fnhBe6yieW9PX27V8tiLf-OBRaBx2192SSmaGHFtgdtdYAxdelZBGb4nkL7VkQmDp1cDrNWQGRqbO3RZp_H-dxG9lcLHXayDGQAXLfYHStnOPlsPKe-yV_dMgyJsTNwO6aE0uefIpaHfckjzI&b64e=1&sign=fa5996b705d2cd6c4eb356e3785069c0&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVL74jCRl2732D8LkfebSn32A-hl5Kwa9nrSjHmOkdB2Y8ZYNVNND7XM89u7fBg2vxu0CFX7WGqo4WwR_Hv8_0lVqp7okEtgnZ3zKp2WcilxJRXGsnFv2HGCCEWpSLlYC-lkwn9fYxU5Ba4fMNHd4ZswiGTSweKU3DsXs6glWweykyqI0gPFXwoy66O1ZhwuKpkp1RPhzaPp9YPsXntBADvyi3ehzOB0QQkmNvsWwZMujUjvV-RuEsDtysrCCIDQ-u-5RxwIz-rjF_yAGvrXbvNSw9A7k9fYpRuR9_GE3RPiOHfiSUfAwJgM64iZ000nsj-sHJCWhXpAmwkaxFPzGujgGOBLnL4YwAbjAnAm2cn_T8qmpzy4WnY2gsbnGlC0uZiyQ9V2K4FbifH5ljjK8EncRrDKoEEKnugw5kRvi_gdmIFDmxUPbr5hbrlVfToqef7c7_7Dh2WqFUV-9WewqP6O3ylpwufIMURMvWgfepMadKTDYWCAiUjD6eGAoCc-g8zDN1bPVr4GC0HvpDBhcSnyJnGYUsG-zunQiesnmki2a2xg6GaE42j1rsoVclNFSVKo90iu4ybO4m7VGy5oqMGeEKmIkog3FVArV0OSEBebRzny8THhuGxWC5IXfNQvZcsovH4dw9UaPBV45T1UT0-mQzRDZHXzYxySKbneRr9Xib4V9awknoHmeCPmv9sOTOxzxd04G7XpTg8YENS-HxQmtl6WMWEc8sB0Jv7hKHmJ8C3Tni1pqPMoqVAl6bzPIEwLoPudMWidMiP_MEmHyZlBk8eNvX2aO55dbcdD1ZunOzm-u7575W5zMm_11H3W-y-wC4SchYd3wXmdv47NDf0YgHK6d6ux9ug,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2SytbGIpluO5JxuTt8fle2sOp30CaA8zLLS_5IuLx1FKA1_T7UJy2WuX1bnYWN8vo1dbL0mwDT1qDDF_-hXMwZ415zTxvlInzk2YXfZT3JYeeqS5OvML3ZY,&b64e=1&sign=9ea922c0d3d29875133fff08de204558&keyno=1',
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
                        count: 295,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 40,
                                percent: 13
                            },
                            {
                                value: 2,
                                count: 7,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 6,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 31,
                                percent: 10
                            },
                            {
                                value: 5,
                                count: 214,
                                percent: 72
                            }
                        ]
                    },
                    id: 141122,
                    name: 'Цифроцентр',
                    domain: 'cifrocentr.ru',
                    registered: '2013-02-06',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, шоссе Энтузиастов, дом 31, строение 38, Пав Б2, 111123',
                    opinionUrl: 'https://market.yandex.ru/shop/141122/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                onStock: true,
                photo: {
                    width: 484,
                    height: 484,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/364498/market_2pdfxuJ82BXGpqDQPVzApA/orig'
                },
                delivery: {
                    price: {
                        value: '400'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 400 руб., возможен самовывоз',
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
                                    value: '400'
                                },
                                daysFrom: 1,
                                daysTo: 2
                            },
                            brief: '1-2 дня'
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
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/bpC1Q5JvtTdlTvx_Osr-Pg?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2S785hdTB3YzqKWMEbX-ugBqcIZws5yZDRiRrP8NLgvyS9je6bGzof2L5yDfyZ8jHosCqcHkS0TgRbc45JcC282H8EWyNKOU0PD_rzw98XxNdr70PY0zqK7wRlOu9QrK-E%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97NnHWpoDlF6OTQVhRVb-QlhxrohHTOuy8iHCbEIw4DUr8n3X64Qc0LkoInRYzCye-BlhGYlVb5yftWq0a2Cjwwa_uKSJCCMzXo7w7jYaQiQLMpDgE9hsPfLexgwNGgcHKBHIHd09q1NqPkrafCGO8cFizSWsgoY0l700YmjzumfmPZ7E2QB_a34ExpFl2eGgnzDV3_Y1zwvSmyJiXQ9AEF6Yw6BjtcFrI_iVOrkc2Ql8dOpkq0j7Jhc5OZ9Hq06PCAS_LB_uDVPYgrRK-d715wcZVMh4E2WT82WrvGnyEZ2VW24HFtpIeF59UucSJblpBF-hj5MGgRWC7-PRbqpI7w02JggKc_ytzGo-P2SVObOmxv6Z728vKuL-CJw8G418ZPquqIzTlJXdPtdFPm5HSqxmdvACWcDVwD3ERMQBIBB5LAsD2OgfvZrB4XharSEWzAeZl3X8vyO2258Yw08v-TwsLUU1ffujAPirbvXDYq86dA-6fr6ht0RcCdvnNz8J-R69JAZob1FIRJCokjINtL3_Mhp1djxfw9dB9tL4sNEAKPDN-HSPNgs7p894vLN-OHVpXNUo-0CXsKEl0kmLcMf5-XPJTLMrjEOYhme2DTqgs3GkwFZusKcB_mGUBgeOgjc2h07Itq01rWyCC-3bigNSSCIG7WxFwx_GwmDTGupB7BwVewJbOeE3SkXW8d280FINsYE8ML6o?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXrUBGVkBF1IPEcMc2AGCM55KX1iclf7ePSYehaiLla1rhPO0COfEbAS-tXo5acTAdUVapSDzY4Eme2stzM-qoleupjkEwsqhVYPlyyi6mTlq1FjM6vQqwH1vG5fpRuc1BGPXyrsX8OHPF3zSxGmqbAXJa3HRo6PgOwynThMjbvdQQMGK9WVsbrcAunRrbKNQup5GnktOtSsIU7ti-kVSDlrNFy7E7BIuTFKUY6sz6EPEgvfrTLihoJfbMwjJ-iBhTr_LX8PQO9ryKcCql-5s4p1LkK62BpKE_pXCPXAk_qvsvvGcq1-TkNA,,&b64e=1&sign=e25fd5c8cf4e82906c7cc179c1d536f4&keyno=1',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=141122',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 484,
                        height: 484,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/364498/market_2pdfxuJ82BXGpqDQPVzApA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/364498/market_2pdfxuJ82BXGpqDQPVzApA/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEPZYoiCieoBQnSpIioPe3QqDM1Hr2WoAkVlYMRLd3OUzKiLW7qlZiel_e4RcIWHceFRax46qNBLgBZHz5_H_G07zl0xxvuKEiDsBvG5WULSzq7N4I_FzyUpcpPZXYzLufb4Mz6I8jQmlDjAyGVIjEYtQNpe7iJNdLlNxl68Ux_BASlf6VQdFLPhRAHfnpB0r1u7XTapYmIf6Vyv6D4CjK__UTZR-9_AWWHIGjsjL1jX-ho3ZJbJcfxKdZgC_r1AzySzT8Ty3UJ8F1u-tPJ74CizyuU331ZCeY',
                wareMd5: 'M0AXoypoDU4UGkbEipghGA',
                name: 'Apple iPhone SE 64Gb Gold',
                description: '',
                price: {
                    value: '23990'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ2ejezKwLQ0JLT46NvhIou9czgziSHuVK8Ah0BBz2Ie-5CV-WMe9o6yGdxy23FKvi0nch84lsOcBnDx0xkq3bl3IF3V-7yhuCRLS18uy8LUqep-fBZc3Yn7kK_zOYcovzOv6b_0LuTLAO5EKqPKLv3OEPe6T4gB7jlAODFZdCYNrADZUbqGyyMT3dGwqGYJ_vDvL9b7qHvvfwVz9pF9Xdopmfq7Ytxl7BKmnfP_lZy5aUyIc0-CSHQPwMJPCosYcZiq-pB40Tl_ox-DrmEPTZXQG8S6PKvQ2McAjCEa2OnbBo1j0umAWh8pkI4c3Sk-Aeyo_eu1rCtJMChTj5OpQ35lyEaZgbKSIp_BsJGEYEAMtcJU_aL66puas1bZ8kz-5wFKIVIj6I4JKbQ_1Mji1jzSH7EEwP9T8NSGusGLGte4qKXXoiYOtK3b6UGn1hPhXbr1YswBsWiCr5eG-WoSrlYyvgBzcmjv_VVm8LoilaZU71AeZiRjdKhYh3Jq5xpNspkBKsIdliKy7n6y3lsxy05s4xXhR2RSkY8SbFrTmcEKUv6bn9pbDYgIosqcEDGWGhsmSbKVf_RTNNJOYVO4GYq9HiFB87KbMjm4lVGaOo0oIMGWvVnRKbjDnhUHa14hPFxnHMa3VmJLeCgBGs7_MN0VxgXCR1HpfteHsBU1-2z-CQ,,?data=QVyKqSPyGQwwaFPWqjjgNsR692ugY0jPYkvqWKcD46MDfdBSsZS6r2NcWc3E2dfmpeKYyybk09Oc9x1I4Y-3iOvzvgO6LlWhuVBAIflc9xr-0ABbTIVd8B4Hl0QiyyktOlFwXDWR3gIx0PkLfZEFvknTHeszcpoY5RZEWAeYB1WD51WdS4cwlQB9vmh4JAFP68MKlcclJ4GgIDoj_U92BQDGu6YQI3CAdI-HxnXKWXnULXU8P4tt0UD14lbgMpEZbV6UfJJWB6gUly_wGRiFi2arN2js6RGquxdwxr7nA_Z9SmexZFkyK6NXKakn9WewQhLZShtXvM5bvuo1czG16GDi7QVUjP-RyqpeRcXc0Lr6j11XmS9SemwftddfiEVdoY12gjzYifKiVmIxQzyw_w,,&b64e=1&sign=6eedc0095ca15d7be23f5b0cfe36eedd&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVL74jCRl2732D8LkfebSn32A-hl5Kwa9nrSjHmOkdB2Y8ZYNVNND7XM89u7fBg2vxu0CFX7WGqo4WwR_Hv8_0lVqp7okEtgnZ3zKp2WcilxJRXGsnFv2HGCCEWpSLlYC-lkwn9fYxU5Ba4fMNHd4ZswiGTSweKU3DsXs6glWweykyqI0gPFXwozXtED5pYWxtxE0AIArWBU9eANemTNJf0uNv86UOg2INmc4lvkZH0VlNU7JT_Z1uv3oU3eq8Hy-a9l_rc9sluh0gxmdAAGris-uVnYJhzVvFJF1QtSmGfNmWYEFvEr5W7BG3aYms8dnZebVpFajXjdxNHoA0jLCPrHZAe4bsHB4nHJJEjPloFaiNq8gfGFTTTvkTMHP8qCZwhe9vk1sRH_bFUoBzLMq3Qhs6Zt0nKVgZQU63HZ5baTIXR-s01NPTZcs3jY2KMY_X-JOPCPENEHkzWDeTWRQtPE-wX7us1OfBpuRhLNfU-4Ug5r3rurjVz91EJmNa8tkPnDEvSZf_brp30luzYu8Wi-TaOFo9BGfDsdRYWJwcVYq1tfaELxA2i0gFaO9t89CES8j5wC2LdXqZhbQ5CFy5xAO0ht7uIO_H88YRZXiJqA6rGCBoX8ec5Hm3CWUhF3g6ukAgXZCx33xXIzrim7bsGqg11j2oSE8wRYoaFVaHTawvb-QLEJ4IqtCQ072gUXmjRkuyWMuhrj9X-XoSD0-FLI55tC-NFvX1xKtKLFm0FQbNBh0ZNqaBvQW5KsmUnUX1BHuAx2JIZya9n13DcmFmeCUNvKHP1klnPlkzJ6SjlonxQbbbfAgJom0i1tzSmf3LLuIYk_I-I3WXp4buViy2aruz57l-MSTpg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2SytbGIpluO5JxuTt8fle2sOp30CaA8zLLS_5IuLx1FKA1_T7UJy2WuX8Qm07iLM4s1IiKLLQzx0oIc3dtvBUKIqxoUsqRP8aj29a99ELLZz9mffIt-BCYs,&b64e=1&sign=267a8f12485e8e2215ad358d064e6b1f&keyno=1',
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
                        count: 295,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 40,
                                percent: 13
                            },
                            {
                                value: 2,
                                count: 7,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 6,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 31,
                                percent: 10
                            },
                            {
                                value: 5,
                                count: 214,
                                percent: 72
                            }
                        ]
                    },
                    id: 141122,
                    name: 'Цифроцентр',
                    domain: 'cifrocentr.ru',
                    registered: '2013-02-06',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, шоссе Энтузиастов, дом 31, строение 38, Пав Б2, 111123',
                    opinionUrl: 'https://market.yandex.ru/shop/141122/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                onStock: true,
                photo: {
                    width: 320,
                    height: 320,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_UKPUrTkS3Vcum2mh4WVIcQ/orig'
                },
                delivery: {
                    price: {
                        value: '400'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 400 руб., возможен самовывоз',
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
                                    value: '400'
                                },
                                daysFrom: 1,
                                daysTo: 2
                            },
                            brief: '1-2 дня'
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
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/M0AXoypoDU4UGkbEipghGA?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2TQXzMz_zsLpVPUIfCoDdStgepRIMtcBb1RhsulZAYbb2wf9uYaDgKM09Vju_vRoa3tYmWsh4EjCaNaBFiEzvewrFO1lsOKmrmehSCs7qQgxlR9ucR4XDP0MI5sqewCwYE%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97NnHWpoDlF6OTQVhRVb-QljouIQGeYi09H_MiZNuWaCMAcvoNow34Rb7n-j961FoagnTeFZ7zeVe3rnHk8XGpggSIpzqwPjpoBY_Z5MPAMHLgfKHkK-VbeEqKscOuCABSoSdVPYNPCvH6gBaLvIyv_T_4k4-qS4lvJskd5hmvEN11n8NtxBy9THHz4G8ZMWYn_y_WAwfOm1gwRnVKlNaLIvgwI_wRGpZTKlhIjecw-7Y-fK_29xvx6eE1jUd7E55Fr1aTQEZK12eucswntjosXZ2I_riO4GYrqMzEAKMXmALiOROhkQQBStEjoszEaTkSzR1-9F-JQw59gBneyeGroQXIUCslapcJE2s5N15XXSxs82peL2Ui_dSYtSm459VED5xY2Liu5PwJVs8CVm7mUkpODc4g2wdOaXBwNkHNZ2BPp3HLhG1LNaiMOizziLUZ9gz5YX4i5AEsyBnspCuSB7jqhMCBmDyQiwaTncSZNUDXV0sXSRR5EaFgZwIph5x9kR2Rq-TlIAmCFFhyZ7Y-D3ZdM2jxVQmJqMgxjXRhX3PnwevX32-v5rTvk9RdBKaP84f7UKKYCShqtosO9IzgD9Y1BIkmcF3CiykWpNNm2YEQQPPedkyDCbAlFqVMtAoqDKm78cTq2Y6f142PlHviv1fAL56GCeZxldzMYCwOKhM779Y54IixkR4f8-TAygDXmB2qtZS5IHn?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXrUBGVkBF1IPEcMc2AGCM55KX1iclf7ePSYehaiLla1q0I7OyA6c0aES0yVEaeINgAfMthVreIdzxQE6iRMxqwnwVGQMqly6qZiwrTcQFGR_XUDSFiLzhg_2qfPlTAxKi9NRAnzLBh2DAJs60TS8zZyYZIvSj-G_rMihluZMAtVpKmFQYXX6qhEwrK67EFWQqRGSyBnNfStzJgJxBvpZe1wAG2Owu2tweHakN8XswiblvkuWbMAk_lkmdWuBdx5sxvADZijciDh1_X0Qtm_wD2B1NNmxdJzltG1Z0iIWbp3u0ab-Fi6nZHA,,&b64e=1&sign=d197ebede00cbde9a912f4d6fe25c601&keyno=1',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=141122',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 320,
                        height: 320,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_UKPUrTkS3Vcum2mh4WVIcQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_UKPUrTkS3Vcum2mh4WVIcQ/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFo7Op07wJ8ed8BPbb6GxcMe1qpdQfl9FXyuAHZvDwwlkDgGVRKwieU4fF-u8wM2CBO7cKU4jZ0YVGXz-SBFy32Mc4Y52M8j4jD1LstMjDi0awf15fd_PWzLyZ1GV4vnjX3nYse6_s14tnJ4eYxkfKJT9cs6V4zAb-EaCww80ONMyiB1-ZHlGRNJq2bthRyGYJvnUVaMHGj8ZkdCkNpGAN9eQuAH7eSmtePWGoHbXB9F5i3JQPBlJpPr9QTVsLyyKhKC4vAxQYIPGLTBFUUCeY4Pde6Fklolfk',
                wareMd5: '4Kz1VQZ39OrRT9eRFhQMUQ',
                name: 'Apple iPhone SE 64Gb Silver',
                description: '',
                price: {
                    value: '23990'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ3K8cVRHqVS9SgQtOkJdwT3eDVb6rbX7Ur2IR9bdwsn2ye12YrwC5tAIKXljejiW4FXbEyvWpcO_3fCtnUtDeDNXoogiXoaB-Ug2DitP7fx4QsRB94ZCdmy0pCm0Z_J7ua-1KQ8TMDe7QMlERW5rxP0qejBSTlKWqxv1qE0Y3fqBrqgw2BH_VgQvcNKrUL8UrEwQi9Byu3xJr8VCPjFT2sd3sdMzl9Xa664zSQP9rSCYwhb3S-gOG5vMiLX6edQCNXsQf2Wol3IK9LESJOGH8cTI0kk-T-Hrve2zWEBO05DwhyofRL9MJ4YGGRm3_HrR4DbP-U0c7wLuVsuclbM52VYS8ujHS1L51jvaRgFav9qxaoNJtRCuWpZrSxDEvLSx2nIpsLch-kKeguqV2Suf7ggzd64bshViIb0nhp5RrQ_bBTDXxCgLdjsP60kvdN_1RzQzNXOjXD7Lh_iW30ymQ8wZMJC20rUFUxAflyO3ojyWeGU1hPwnVO_Wm3FkYrPPTJ7RCWI23Jo8w8HYIIIQmegbHO4Ty32hTQaz8lRPdeWvrTUszixcrLh4CSWO46_35w9FGOmcp727vszohsw61TDNkiqUIU_fcYLkFaZQnIO5QmwN7g-brKhHMluCGWPvc-JW889gz_Gooc4dVTsGId59lJ9au1_Z3cEsPjUsO-tGQ,,?data=QVyKqSPyGQwwaFPWqjjgNsR692ugY0jPYkvqWKcD46MDfdBSsZS6r2NcWc3E2dfmpeKYyybk09Oc9x1I4Y-3iOvzvgO6LlWhuVBAIflc9xorIGbwIyoPlgXAQFALLyQE34qQXzDp8SXWKayi072FRx3sA-2mz4ZAAUcAqw-KqCv6xzE0hOt6oPHAkNy0-dtidnNk4_TJXXwrOMeHiSkmrDkKNN8U46f_yfHKHAfCsFYzhyLa1IRvxZ4XsDL3NnjgawdFpqIB_uhjKOr_B44rsapuWBHQ-TwkZKQrk8oWC200XclQrp4x4TqmHGxKCJEhlkrVJ5cNSCN2nDlyWpfwgrKYJ8CTw-1DhSA8qTz5rksleLqfpQEBM99rd-Ry_OkA1MxXfLzzQiiaM6CKUj6Xv4tHQY_YV-k9&b64e=1&sign=fd6d931ab10cbfe8cebca636406e10ba&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVL74jCRl2732D8LkfebSn32A-hl5Kwa9nrSjHmOkdB2Y8ZYNVNND7XM89u7fBg2vxu0CFX7WGqo4WwR_Hv8_0lVqp7okEtgnZ3zKp2WcilxJRXGsnFv2HGCCEWpSLlYC-lkwn9fYxU5Ba4fMNHd4ZswiGTSweKU3DsXs6glWweykyqI0gPFXwozYuIx-h-FB5tBRL4S5iTB0tlg-e5uiiDl4oTaX3aFL-QnMnIEBPGO9XjkeLt8UV8Njt_Qx6GlB8aq160z-ZlXXtuxe0VI3pawvDfPnprBN_zhpjrqAh1pCeXxyxY7qhV7z4vO5H0-wRNbNXxemFr9RnhnIv3uxe5ys4ABs0QxxE1jOx4GB7H-ryhxe70iF8dWW_ZqeWgXwjJqmyH7GAePoGvuDiy-NnhvIgSWoOEqH0c8a30aOfZ4vIcJCSQ1ASoS5BQ3IiOP1Lvr90OSCFoGz32CrqmH-6g03ARjvqwgo22A1G6Hc0-xP7dhtlCrjllhOmlnErpR7XQarz4ly7ruNqgILBqitnIYAU7XNtXRQPPEkHgzKfgUppjL_KfhjqarpHB-pjqg4eeHrnGtL90q9pNrm3R3iQ4wc0udpWwYeL80SY3TFdeYlrF0i7bXzYmlRfu7l0XBUomqycO_aiAhPoww9STjQ6L4NnUlnq8UnMgs8gsaxANYBCUEVpnBIgeFq6HyW_r0amD-gmdzJbPCOeIUFVGVj_FsEkqfll3QgFkxr7ekiL6YuurjB85mbwL1L1bXY6sVI-E6u2-ifR-lbou7BtSwMMLakbDP9vszwasf6td1oSZYuys4NAjBX32cr2ka8r_3HzgdExsE_sIMlKOUgNQxeybl_Wf24Gt9wuQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2SytbGIpluO5JxuTt8fle2sOp30CaA8zLLS_5IuLx1FKA1_T7UJy2WsJDkOPprzic485oz1c0lgGJtEKgv1uKIzp05mRGAJ9HPYEBRgG381hr_zYATFaWtw,&b64e=1&sign=cd150642055be0df27ce7c2bf68c2641&keyno=1',
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
                        count: 295,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 40,
                                percent: 13
                            },
                            {
                                value: 2,
                                count: 7,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 6,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 31,
                                percent: 10
                            },
                            {
                                value: 5,
                                count: 214,
                                percent: 72
                            }
                        ]
                    },
                    id: 141122,
                    name: 'Цифроцентр',
                    domain: 'cifrocentr.ru',
                    registered: '2013-02-06',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, шоссе Энтузиастов, дом 31, строение 38, Пав Б2, 111123',
                    opinionUrl: 'https://market.yandex.ru/shop/141122/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                onStock: true,
                photo: {
                    width: 320,
                    height: 320,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/223477/market_8d_B0wTKS7sUZEwhOy97pw/orig'
                },
                delivery: {
                    price: {
                        value: '400'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 400 руб., возможен самовывоз',
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
                                    value: '400'
                                },
                                daysFrom: 1,
                                daysTo: 2
                            },
                            brief: '1-2 дня'
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
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/4Kz1VQZ39OrRT9eRFhQMUQ?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2R1TsSQLZ5In2kzKUtb7-gsFxTWy9dKvpI9fTIdQmNKL1d_lQv0nfbVDqrbx-fc9YjIh6Rmc1EW2mm4uEEg41LVwpRZjwJ0BN7wRM5xAGbiVuyLUYZ-3Q0TxVLk9k4q_4Y%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97NnHWpoDlF6OTQVhRVb-QljQp7-Fzm0yUY2kim_iwTBDZFWE3Q-e0LdZh8oRo9rffgNFFhQdL6nIGY0cpy5dRxY7aDassIUpajOO8YKFzMKFGaQRjtOcCtcg9XVGX5vOdvpyXZS3AxAfbOmWoHOnZOy7HMUkFqgI3h_W7-82r7lLC1TahgSSh6sXPbL3Mq9SxQeYNChNTuYkF6KJHZLobTi-RjlRna0jjUldThIInxIfs2YgnUYiob0v-XU2LUI1O_0Vm3JtWHzCS7jBo9dG0-GYKzy8bTqG1XvYGM2MdgIHs27zhowx2MZXdFK3N7FWwX4vCtZ9enPFfn1niFxmt2OIawj2tGkwsuuT-KAeEYR5cUAvXwNkaEOHw6PTCytJpYTRJekrd2gQqn4WNV3UmbP9Lept0kCcBMfqlK1jlxN4VnaQHkp0fQefSsvtbza9YfTFDdr2LzkIKEBRuOsxJu_WmIJghNJPmBQT5OS-3_wYteEU6NjUwz5d2S1KsGuK_SJx1Mc54pq3lr0LKFdbcKETXIHx81km12QP_rsO58w70QVtyct1tc29t9TqEtyRA2z2AKy0O6IeDborkvIxpikMUIQyEtKUfbDrINIQGdroE82jL_nVpwEj9kIuUvrhZ_BY9X19RRukr6SMrL5W9AFNuN7caaK433cRCad6wlnWdgB1a-igk95PRB8z12fp8v3s8-QPaL_n?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXrUBGVkBF1IPEcMc2AGCM55KX1iclf7ePSYehaiLla1rdz21nqBvMR-X7YmutcDkmS2oIWJMPp0AL_2ZBqQ-cANc9Iut0LVf5LBr5cQDpeKYiO9u1C1qIUGYikdUqwh-YJRGnUDIXfLmllMTrf6zHx_enFab1RDygWheT5VG2hr1BkhGm7SWB3TsFkxzmY67XbdzpT9m2dhcA0Bt6OkoB1Je4ekfhOd2Us4ssMLllhUG8JQOxyrHf4bwXixJo30ZK_YwomZ0pQZgYmRaY60MlBSHNaSypN3U-cRpQ3rAkmwUidX3CydqYSg,,&b64e=1&sign=d3663061fbf1e716cabc77b17792dca7&keyno=1',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=141122',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 320,
                        height: 320,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/223477/market_8d_B0wTKS7sUZEwhOy97pw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/223477/market_8d_B0wTKS7sUZEwhOy97pw/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZE3iq_o3qJAQG8GrIisMpCh01uYCMEzhqTJfjLYDtr3nFbyA4AK6h_bniZuh9uzDQvEKV7fCLcPfGIGPHB1uW7_Z2COHdLP_OBN8_-e4bXHveNxKVlovBuadz8Xs7Bq4pBSILxqdfNDPflm2k7LCPIq_NNzZjFNffiURESCXKS9QQ06KIgcj2irlt2G7MKBwxrQncKyNbqbN7deX6oYYhXD6EVBi3QZiAC8pJUSJO-OP_YZUhDZPZAHcIQtLwJQ6zxeGDe655bRUurFNws4wzyFLSiASnaYjcI',
                wareMd5: 'G46__opwksExEXLy_ZDPmw',
                name: 'Apple iPhone SE 64Gb Silver (A1723)',
                description: '«SE» в названии телефона означает «Special Edition», то есть «Специальная версия». 4-дюймовый iPhone SE рассчитан на пользователей, которым хочется получить компактный, но очень мощный девайс. Новинку можно сравнить с моделями Compact производства Sony. Миллионы фанатов Apple с нетерпением ожидали выхода современной версии «классического» iPhone 5s. Они, наконец, дождались. Дизайн модели почти 100% копирует легендарный дизайн «пятерки».',
                price: {
                    value: '24990'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mUz8wgiS2_KMLNrDgZ2KpunF1NfG2RiQoqkgA-Ob6OOQX3mh6x7KGr1eKTJLLfNEVFNwQJf0OYODayy6fEkEs_D2fKc5fET0ASp6sVvyAGzSenFODLYiyLzAjve2KtPRZdg2PpQEFCJT0YSxSUxduSfdxx9UwfUJueNs8TVEPtJ1Jk0q_CopkhSZcxtyHAhM7OolbwnzQL2VWJrspDBYYWiJ5FGnJ2nXQ2fcmTto2GMrUFmYftuOkhis4iJGbnGVjvEYZeI7uEqz2a-j5PJuaCY0EVsg70Su2QmOu00MoawPrDr7l5UtSPksN9U89iFZAyXjYvVl6hPtWLDKtibfIMZkL1pnntpX4wMxARlHVNwnOwzOxAO8VW4Zl2zYK2MYP5Uv3o-3_ByOyvk9_mOeb2Z6AOcOkUImYwAJXdCyyTdD3NJmWBrdfpY2ltZQ7399Gx5m3p8ilHzAe6UuyhTXhIhPdXtkB-WlvH_VIEsxd5aTncZwLz4e6ADcIoZKndJ9PhjKzTqJq0Q_goUK2BtqZpYjRvh5J9toTFyaw978DfqHzFqrg1xio5Mxjm5DNq3Osbzgn16WGdUj1Dqge3Kgy3OdUkMtBtnEts64ZBDFX7R-xP52dnLEce38gv4IAm8-bOkE6KWbX7xrwNeU7Ds9sMWnsDSDtrN_maryNH_AvDtZ7cadGV_01H8cLeJICiDwzFPFm35hW8SUEWfA_wZvRnpQRbqyWNPeZ4B9SoBBVtbdWoCAIPXRS66i6WIl4k4xKv1dVXTaE6ztSPVmB5gq9W17Gb_D_eNXapMPXuCD4VB6gxGx9bLu7oIDVFE0WOfinGAKBMq2HlEXPkrpz1uNG6BdMrPXADppgQ,,?data=QVyKqSPyGQwwaFPWqjjgNq5ob6Jq70TJiGoeif5hpcbP8YccfY02ogWNdXvVXQnUtSG2m_TC7d2bPi2wXHRJMk1-9eyQ9C3OTTJWwFMvmQ01-BEw0MrTled0m_WVfjiSVwOF86SBDA8lN_JsB5njgmM1-nARgWBwZ9yUSKHWN0pRJLI3hJa_wu3-oTSAYamnWH3U8SSveRF3VM8T7MV4nKPt-qP3nXt9Om16S7ONyIPdm-2uGjsHVGxyMrlEvi2OeuqmEW8AKPSpCaFDxLxRqko_8by2Y6dSXqyt1JuCScNo-rZ-EMqul8ZA8xzh8YRJvlWcH0hqh-Rqp0WTJnWcsgUZy85aHOK7n24cHpuQd63qOWtkIgyydV68b98Fw1VtbuGfCNHtpzBgMGl7kb8EUAV0aCNfkU8e-EbVvfc6pltogFaUVGP8xRGNG886K25uI4GQusNM-2Kdq75HWblBav9WNOAjOOEjmKrD2v42webAOjRPrdbjdA,,&b64e=1&sign=afe6d955bb2f055bc365b500872be1bf&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRu4Y2f_crk7D3BAKE8SpemYMKmJR_QhGPZ_v57flysA0werlZoMqW1JTNJFuqrffy8K_SYbmgaLARqhVwEMwcJ7LmwtrBW-oWULod9CYSdiGIVSip2yrz9RdQEsJ9f4cMl4GVl5viti0dyItnPQrcVEWfXPHNnNIaPLTNJ_lZxat9Cm4WwC9hHbbNXZSd2ch-Gt2xXNEd0V6fFOIKEjbN-FvZu4Fc09AlsKDHMo6Pf4abwxgXM9uiwTdc6evDPtlxT0hKgzTQEjmT92CtPMExoJMxV0oDDWFfFQyOjQDsCpzHDNskVC9RoylPUJh7wX45chjnHhQP4c89e2dD-AEoHVZkqekF-YeFcksEgaMDChvlWiGUK0PQxJzvDbAradz3h_MyOlb3GM8aRIR4kulE6Ljki429AUlfh9_WBYHBZ7B2RH2ozfpOzhU_jUhzVd0bmRrJJXPlQ2W62W_LtxfCE2GkWEhe2djLAL0Uv3e-ggzEab0QOPAqiuZb9TRDOjaQUy4NGq2i5f6uY5RKW7sEUlo49xNzlYarDnG4ejt7iDhLo3l0v_oJWwpzLbqwSLGMdro4hJc30S5WOCcLGh_ZFe5rX4NP3-65v-IT2ENuVfeA6FdkZ59IWWY8_O1RX8HLsfTjeCbliQ-_vL0RjX8iZYmieYNTT0PWpGnudTxaocLt-6_XZ9Md8XCkVT4rwKDIAQVKLH4Q0LUZnOeBHpJkAXtd-ZLzujZnotowVh9io_m_wlQd9C0RL2a0WRzJOtOEVLxWqRraPeXF_qUMKlVZK52pMD2EwzVUrZ8EoL9KGCPZQqE1zktklLYEUOr-0M3h0_oaL2Xkig1_KO3Z07ddTiRHYwh0yFqIFcIyrHRjGILw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YtduRh6BN2pHixvlE0VEivK1RYRTgNLzEOjbbrFiS4JceM5hSFPnTrWkdnmqD64OfyRg6SDhwpPixuds9rjObYbnISQeZXeCMsZXTMTi7uY1s6wOXgrTEs,&b64e=1&sign=a6de022d7d3d7d2714692282e1c69b6f&keyno=1',
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
                        count: 5007,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 160,
                                percent: 3
                            },
                            {
                                value: 2,
                                count: 56,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 63,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 378,
                                percent: 8
                            },
                            {
                                value: 5,
                                count: 4350,
                                percent: 87
                            }
                        ]
                    },
                    id: 262,
                    name: '1CLICK',
                    domain: 'www.1click.ru',
                    registered: '2001-10-15',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Садовнический проезд, дом 6, «Пассаж на Пятницкой», 1-й этаж, 4-й павильон, 115035',
                    opinionUrl: 'https://market.yandex.ru/shop/262/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                onStock: true,
                phone: {
                    number: '+7(499)990-77-77',
                    sanitized: '+74999907777',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mUz8wgiS2_KMLNrDgZ2KpunF1NfG2RiQoqkgA-Ob6OOQX3mh6x7KGr1eKTJLLfNEVFNwQJf0OYODayy6fEkEs_D2fKc5fET0ASp6sVvyAGzSenFODLYiyLzAjve2KtPRZdg2PpQEFCJT0YSxSUxduSfdxx9UwfUJueNs8TVEPtJ1Jk0q_CopkhSE2mdtaY79vhXgtzDCYQfExAe04qghwE5kfWhj0U0OSKdNJQ40h4Dxl_OOhcF5y4FQA2QkLcaFvydJcADrOyO7gBzrojbhGYK8lrSUd5H6wqDFh16JlBEIIgPbkz3E0rcJNZcG6iQ_fo-aEAksaKv-pQ88FyXHUmpgIhsJjTCh31faC3OoiYSSdqh4uS_c6X6mC_FofyltN8DzFrxNbi8ABzbhXMlyL7_D5d4FGaDPYrWdqNQYlO6eSIWLJzmeMcXYQcz2LrLtZEt7s7hXQZxMlx1x4kpBcxztSiheeiaawuH4Gs9z2hLCSDzore3hI7jJ_uvBjQHkRgiGU57LP4LQlxWfhsL8qjQ9CpnP6FqJa38MF0qrTwYVHXkzvRcPPAeJSGI79ldW_GKWCCHwtVODwMl5ZG_QNXZeQ4M2wOVkPwRcTfeRtCt39fqlvj3PI8ccd5_k9__06XY4AOsLmZRFY26dNAuss9-L6Kn-wy30LQG5jBssqcFE34h9TdMcwaLLo0D0Homf-rWqjVewcVQb5vqFjfoluOmVCsES490QHFbSzo76WvINr-Juf1GYRtBG5Na3Qs7BujVuQMbXF0uiO2xLwoPCtOABVHiUhMsUJz0_OufCAjsrR6lZJcW-C7GynCOjyBnedpIR1AoXAuS294DWhq43OIiD4xzOyMunMg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9Ra0y2fm_Tdg5aYg5h9fw_aeC8eynkOBJ5YKckgzOPygzwwxgfO5pRJACQNxkEvx_3sc5Hp6y6_f6Pb982001zs_r2itoBICLK1bZknOjdfVFGZ-mhWaYA-IxlBGOUzIrDjvyjeyh1V6ZpJ2hP1DXh_0sfHDfNuFVpt05GlyZnIg,,&b64e=1&sign=23c7e037dfbe6043d1c3b2b608cfa51d&keyno=1'
                },
                photo: {
                    width: 295,
                    height: 350,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/218271/market_YnBj3uMRkv1C05aPeIzyAw/orig'
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
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                                    value: '0'
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
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/G46__opwksExEXLy_ZDPmw?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2Sq-n673gVGRs-xTPyfEFxBX7wP2JBbLu3rGLnwnZv4w9uuC-zQCu7Yf3I6Kdbl0C_M7HUAiw5VYcsRYPjy2gMLDwPFlLNU64IcJ-DBkuE2A1oOFYGWH5NLzxhIf9LtC3I%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97NnHWpoDlF6OTQVhRVb-QlhCCmSNgqBZIFGPOggvhG2TzAIYkTUJOIUTaEUaAvvMw8pPhnTg7TtekA1yS2Iw7AprLEk1h0OOzz3VV0Yu7xmlCJIQm-BgwdFhyBWSGUd3hFK8GAleqQzSF2LNw36LyocnasoDBBpnd371YPagJ6FZ-XFhaciFGpgZxUNypULKAHBny8XXgPw4ABGAWgdDtJFkkbdHbcPF3P1KUxyz3Y8ajowVtxb62mrm_XUp437ADLsF1DhPdmZZh0HVXijcDecL1w8sOJsb_i1DQ-cdoaedWfLhbgKMaPf-X9i12S17Y1slJxfcpmCwSHkYTZTUcMF4N_XXM9raJEY3aGBIcNsv3E0vS7TclsBayTwn9KS75_A4WdN0wYDKv-Xhx11hvTZe8UcmEUzhH9fYXmfFe4VfeaDiCuGuBQ9JGylGAojur5gk7oFimpnZwvPlpjCmJj-05U0R-46UxQWw2kZ6HpReVK2ARdZnkXKIHui2mEVjq3PsE4BS2SyjSBUSCTHEeKmoTNayLW1bXYptW6Zgd5p9j9Rt1Y6ygXCgP_P4UKWhGXJeGg85aw2Hf2GCCOU5ZYeNeFiZAJ9N7OYofofVGbEQ1B3y5IGkKj-kHrK2Wsh3NdwFcc0DAfN_TqDL65ULCS_FEuM2e2a8ydm7_Xl03sDQkNvrnZ8_eAk2eejIJ-_Uyw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXBj6TWasY0Ft7zhIobFmhwZ_vxmCLMVEc6FPulZm92BpdCpJEw5__XT2EyHQm46734ix0FqxPGSxSNSEjZgvLs99FX8b1vzKJtRmOo8M_2lZbObmx8J-sy3HO6x78bH875ejSzV1jzM1iXyGZBwF3jEFOVsIODQiRymaX2twA9fn4JI6Pg2aQkvSiej_0nFWS-B9IuV_UQWzhPkLrchz7UzY8FJzkijw-gqy2dSHvB8cUtCZvWd8ahHC7tg-hpCCzgTpeepOz4kaJFbqDIE7f9L0OSZBN1gw1aslyUuOMcSglQjhkhEbqSw,,&b64e=1&sign=2d58e89aa91266e8fb49adc89174a2bc&keyno=1',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=262',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 295,
                        height: 350,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/218271/market_YnBj3uMRkv1C05aPeIzyAw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 168,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/218271/market_YnBj3uMRkv1C05aPeIzyAw/200x200'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZECk438qxZ4vUGsbC_jGMfkTZKcDOqescZnlzB5UwdPZvNvjK0uK9cGtYN2FnOYkR1lWYkeh_OEHbnLnXm1MszaoUmSPGGmwvZVfKDVbu_D4bNwp1_akTnrv3IakHyyqpP3uj4VaXQlF4-cZN64_B26GHziOrEoiCUyEeLdYlB9QA6iCirf14LkfS1Ihyi8ge2QdiDoge6gcTiFIM1LhdEvKyfPMrlD4lD1DQ2hwd156mMDz3SNr9JSs7bi8BHm7a6KmcAzuZTp4ThQNGgd7uTk3W_8s8_wWlE',
                wareMd5: 'oq4yFHWmxCStXfvKn9rTxQ',
                name: 'Apple iPhone SE 64Gb Rose Gold (A1723)',
                description: '«SE» в названии телефона означает «Special Edition», то есть «Специальная версия». 4-дюймовый iPhone SE рассчитан на пользователей, которым хочется получить компактный, но очень мощный девайс. Новинку можно сравнить с моделями Compact производства Sony. Миллионы фанатов Apple с нетерпением ожидали выхода современной версии «классического» iPhone 5s. Они, наконец, дождались. Дизайн модели почти 100% копирует легендарный дизайн «пятерки».',
                price: {
                    value: '24990'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ39LbMepLH3esTilmXs-AxT4rEcEsiw1fepeDP6ubG3Q_4WhoXSX_vAabkOOl5SyBJQCP4LFTMMxoX6FyZ77A1gOEhlkVJht5cQKFnpdvm1M-Vk25UBAXR8mvj0JcZdugvwTWaLsrPb4vLQ7Ynu4oDPNNW0nf4gwChZWtXrJeSFVFvRZwhL_bA5m27byNddTN23Q6XJcNRDw8VKwR0QTmHIc7Egc4jFeGt3DeVbRDP3GPoUFupN41qFnu3D41XPO7ys8rHW2z8VTd7_xxRedoQ6IOdMcTwyfK1GB6oDKAZpyYkcAWpndM8CKkUWXt6wcTGwjJycUdqFevv4feVuOkjJUU3Hkx7ASrvjOqL9c1FSaZc_j-RNjxLYpX1eycO0Eud4M384uo8fGyyes7NszIZOU9d7QwG66rOlTVXQ33l_CmKx-uf7ufIEdRGnD1bNAJTp2CXgpZAr3UUKwP68BVyyvWOmdwEdbjhj9mIJkpmAJiGexmRCEzwxnGhZRSRlYnvGTl06E5QjexyeV-YVjEFNniU6dK6IyTaEaAOgVqpAyxvU-nxlkjdXjJJIDzlgFXcTJYUG9tD1znaZp4jVrCm2ONBeyoAdkKWDiPWMkBqAapazsdQcqb-FtRAKhEtYsycFX9uZ-dPODUqHXCIOaGDE432r1Nlnb1jgP2GTKFkuiw,,?data=QVyKqSPyGQwwaFPWqjjgNq5ob6Jq70TJiGoeif5hpcbP8YccfY02ogWNdXvVXQnUtSG2m_TC7d2bPi2wXHRJMk1-9eyQ9C3OTTJWwFMvmQ01-BEw0MrTlY6CxAEgw8RdxGP2nHyr7GVRlKSB8_z_rJnW6pE-SjV2Ie-64ToY1dlPzv3uO9CaSb22kn0Un6iBlKW7JSvdnDd9sVSnZYlZXTKOS141TcJwTsi0ERszxplh_cOsjrur97NtqTusCMXvPGz7SEFVOqicxfoNrLs9hg56_QLnXP8k9CJk_DfstV1SDh3XCwTnlwM1NOKcjruUqsVwC_y7X38RTuQ63L4rPdU87xYR4aw91HNGF2oHYB4dcu0GbuqaP6ay-1Q_60r1GzpAJBhmn3gDNrg-At--RQnyrj2I2Z0gA1s12qdyYJ-hVEmKfPqa43UcnnxIsQQm9NL2bU-NNryiGHPSIrw-QGCWfkQKTdAdjUgoimdzvTTLqlyLowUAGw,,&b64e=1&sign=7ad78fb7fc21279428ae227d319b9a03&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVL74jCRl2732D8LkfebSn32A-hl5Kwa9nrSjHmOkdB2Y8ZYNVNND7XM89u7fBg2vxu0CFX7WGqo4WwR_Hv8_0lVqp7okEtgnZ3zKp2WcilxJRXGsnFv2HGCCEWpSLlYC-lkwn9fYxU5Ba4fMNHd4ZswiGTSweKU3DsXs6glWweykyqI0gPFXwoxO_asGWEKEmwQdWuiWenbF9liXx10xEMrAZqBWg_4uhK5IhKJ6-yAPMMJnXgmUtNp2uu0RYNMTcOKycOPWW5v4HDhv0gcl3V9wzk6dYDzxAQ3Bs5wqIeKtQn2z7h11V5d3RokF5tkRdHo81dnmYf76Sz-Jg8d4m7cR3HhSJhSIUmTFeHSRYrdwBoMGl6Snt6sitqEHVvtvsK4YvQsxe8eWNKBDeHeUXG64ETrDhHrImdnoXBfu8JmxbNvQwasPEVAp0NTFsKypIhU4232wlqqnzBUryXEMuTH_l5WqTvBLAc026AX2GN0N56mAMVBr-PzPCcw-wBCHxDGOSv4M_80XyktZIkXb5-SphxpD52JfQELIS5-dynirqvVtLEaK15GHRboEYWs6tgCBiicK0XJfvam7gy9onbvlYmsg5A-UdpI4kBzr_Tfswp0aDjW3J4RdnlShlo6RZuNOqhcGEl1azM6vFuOi_eD3Zz3dfPlIJAYbcN_6oxsB55mkw3aGSpyU7h1hS_FQEWKc6drzsXTAQ7_0Nj-lry5WBBVSCGjJDbNS4UAiNg3zW5NwhmDTY4URlZXyRsGhvJF2IySeOSBfqzaAIeoRAjCFtY1isIIg6GlNU_Q9VtInX2NBvh_t56e95iMwIEkjY_45kFZ0kXSDF8UZkBn9RQnMujv_-GbdDg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YtduRh6BN2pHixvlE0VEivK1RYRTgNLzEOjbbrFiS4JceM5hSFPnTrAPPjzEh-KXEPdfKVx0muw1jmXsUxXLDcOcYDoJk9SRzhZDQ9ZsvPHevwKmv_b-OQ,&b64e=1&sign=6fbd3ef2dd10362b52adde3d22707723&keyno=1',
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
                        count: 5007,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 160,
                                percent: 3
                            },
                            {
                                value: 2,
                                count: 56,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 63,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 378,
                                percent: 8
                            },
                            {
                                value: 5,
                                count: 4350,
                                percent: 87
                            }
                        ]
                    },
                    id: 262,
                    name: '1CLICK',
                    domain: 'www.1click.ru',
                    registered: '2001-10-15',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Садовнический проезд, дом 6, «Пассаж на Пятницкой», 1-й этаж, 4-й павильон, 115035',
                    opinionUrl: 'https://market.yandex.ru/shop/262/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                onStock: true,
                phone: {
                    number: '+7(499)990-77-77',
                    sanitized: '+74999907777',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDoKuC6BbmSq-A0W9KDy5tpPn2JHzUiwzGySH3SnSowD05wjxOywXUFmx1fjO3o1aJ3CRfDfdlWMO1huoJCdMQ855FzG0Atcrnlz_rHfV0eQZQhedHCp3elN3Ig1yplQ8kJ5nS7Ca2jc2X9hkwg6asvzX0rFA0ONEEecHc_H5bbu6Aa8_NOcEUBITaH02WiagB0W0UMCnx02m1jL9HZrv5gLXpnDcME90aStA5TcR3iDhjgHDbcrhNZD918lfKvhPuQ_paV63m24aXj8F1DP92nUi27fdAr-AwiPO6YfibXwTfJktVwT7cCM_eTBRu5tqXp9FaT4p6vkzLmyBUCAThgeK7foo3fRFzGvTLKmi6ZY2u5DVNoCv_nqD2Q0LhmDj0IgX5O4_Wh8ktPojAh_CXjY9qH-vpXH3mcPT3YpewvmA6lm6i1_uJiyO4zKyJKbclH-idhy0v8nEWYGG2ziuyekMrLp6d9jzs3xKhqX-npfbyKgn11PQObWfhZ-koqOoAEzoF4kECW6jUNYjgCroFVD1aDcsM4J8cS_0BcPl9MBomCOggHX7_f-UIdbgYfBVuNjXcoZ-IIwW9Sc_EMD-RQ4x2jLQAnJXtgp5C9Fns1S_6Y_HnHxNjYZXOU4TFsK8g4jzDrFC7y9-Fsa9FTXAoX7uzLG_D0xChRQPcH_hW9FbO0Ur60aae1b4dv642tsU3EdcRSjqbOodbo11kii-WJqMYQYroz_t8l5ryjsMfwHgw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O846F0fkzhfSJgsIl1s_MbpLtt57SheKhEK5GXmHufRpngAsu48iPRDjykrk6XJX1thr53OFOdSxrsWxZRud0bkWj9uT18SsT_aUNSEJ3axfobkDBaCGrmOjDjUTM65h_7Y-kmcQYnQkSuAOdihB5WA-aHTso1h193A-D30M9mqfA,,&b64e=1&sign=aaa64232268429cb50fd177da1a99af0&keyno=1'
                },
                photo: {
                    width: 295,
                    height: 350,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/245136/market_J1y3KRsqpjqn3ZgqscjxTg/orig'
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
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                                    value: '0'
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
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/oq4yFHWmxCStXfvKn9rTxQ?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=q-nj60tVs2SNbxfg2LAUDDwKi08YeHKUiVxg0jx2w-mimBTX1uJT4MgON8N6lg5F_4SKBWeVoeTJJbP7xBmApRyFj9t4DGXLQh0VVXv1O0MgXKo0NWzf3LFoI3ro6le3p_qjbda99pA%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97NnHWpoDlF6OTQVhRVb-QljMqDBtfe3-2bfrRG28D0B6DmzcEs5fT_1-BvOVc9saVLjSCS-DhvAsiagSCy_zgGKHfYVVRWZrHVI4NL3FJfLwnkBd-I0DyoYwlmBQUD47i4jO2xpOBwq3PrvBrBHPjVN4THw2YGnF9_5FgQKG_2-OxNATGL2XG8inWaEHvSj7aZwfIJ37MYgOX9G8hXPcRK93zppCdrckv0_KanNchviiXUr1VSE0qLvP5gLYn8iliSW4eTTS0N_rcL-lwZY5xjh4-_a7K-oo7d_RN22ijzH1fWGB3-9udK5MjLO8fkURNX3dyUJENBgnFWvFqnQfidwclvoaRRRgbHnhvwQQr-rm0yVajddqnTdNdKAb7uhB_3AuEK4lYNrzqTcLQQj5apjQx57Q2nRndFsACMCqdhaWt6vMx7ThE092nlslA1BuuKMWVhCYw6G6QdTbfkOzDCyBpcr3ro2QK5zXbOS2W-DuPJboIv5hE8lYS1bH67ObA9y4OcnsyRvF7txkFKG8t50Qpivv5AmoBhOEnXV3VWo91SJ88jzx0HmHU-IDERw2CFbIwj-J3LzD5z_MFoL4B42SKFsiPt4dECt_75IlnY39gGeUQrzKZWjYU88PQLWh81xuWatWYJGYMOEAEki1x1ANEJPlzbker-fePgEdobj0F3RvDwrtcW6MdadTn4tdnw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXBj6TWasY0Ft7zhIobFmhwZ_vxmCLMVEc6FPulZm92BpcVmfl7s8ZG5gJXBVq20WX2x1obsjI7k3xfAr8D-LzFqV_6SOkaLyJmG-qNuYzKlXf3SKb3cSPmZ4pJEWdRkzSDBbwlRTZehyEoaxsXTepHP-wkOTRcvCCaqIh2dBEphyr91gxbCmR174xzbSxqq96bnTbY7XwwtLDcJExJDXPxKERJtXMjrYpLOmQGQgISOsJXaQC407qWgK_ZcM75cyxtkhQpTwj_ggrRsefhgUIGpQtXGlk1e3TBC7Uv6sN0fz0UgtQayVv1w,,&b64e=1&sign=2d4dd16b7a8951025d0b8ea7e2355be6&keyno=1',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=262',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 295,
                        height: 350,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/245136/market_J1y3KRsqpjqn3ZgqscjxTg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 168,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/245136/market_J1y3KRsqpjqn3ZgqscjxTg/200x200'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFY20LZTrGK_JyRsd4GW_eenj2V40aqiBD9vkdv6oJXi9D21Y6jbJr_MFRW-hPhsKUXjWXFn2YhP-IsjiVF38rfJbjO3TXvicsVqc57AuuPmjmy4D5JcKT-65UmpFmd9yRsT9j18CIw5t6FNROgO61QSHuYyZ1yqOKI_JcTMIT_zoMzgGlHS-vSV3hdB4ZrFAFIOdqI7oUXlSCcSKIUU1kSM52QPfPunW2Os00Xta_BAaQAaNfdasNm2sOr0xTkZI2C81Vhrn3vSjpN-6KEEmaCExMg16ufsso',
                wareMd5: '3xe-aHwrGgAS5r4K1pX_rw',
                name: 'Apple iPhone SE 64Gb (Gold)',
                description: '',
                price: {
                    value: '28500'
                },
                cpa: true,
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRsjluDpMb_Hru0q5J1EF_19gRjd89HiDxzT1uVTA9Vwu7t6P4EeOP9tH-i5Lw_hXufqW1m3Hud-6IIOZGDEsTwcVu7tdt3Ypbg-b7ZLxSA8xglEeXs8BqGfP1mBFS9lMGOd0CAXEwGj4O4qoan_f9hT3AA9KZTZ_1tLH5j79P3TL5zFr0U4nUMJr5Y8QheYz8CRT9YIikZJSbYSbkKia7fH34pRbLHoRFXCWln1CPVuw47p7aBS-86j2Cr1oMDrghVnOx319F-B72A0YXCxLe_TcoIaxuPaEcAtEAEN481K5TqaWIp_GLTUAhJYHZisYdritk5WuFusrTDdLrfXTMmRg1fQwweFUVSyWN5FMHu2c4IwEkD7wtpJTS8KbMjfmGErleXt2i0m43_Dk9GJH8AI_0GhQGUTNAe7R-2kzX-_mwQIhNvVw1D-iI8KXOfR7dHd5yc1Zb-d5P2zuAHQscp4EKs131_LJjuMUcchj_8d8ZtK7tsd3xdtJZ-SWZqoAlrdlbe924ReY0bmltORvqG32tfsCPvlH2gRgOcj3zEhQAluRvwTG05pFxBuaquWXnO5SE3ofz38i182ZExif2Ug5PHFaZQptoF03dlBWsCxRNdR1M7Zu2eLz68_EIVNoVHNJlSFrKFaZtRsNFx2nvebV390b5mJpjuzXgYZfJcIxVnLmYFUVpRAnNLN9oHWDPMIgiccGaZJPGUzdwCgoDpnmko1RX-ynrf7L7z_hm2WYpCV2HpG0bVUIuUviC2DXACHONx1AuHLbmsPvHxY2_lRmSkqNTzu5RbF8RHblovOk0VjCnbMLTdi70EKm_au9YE9-FwFERfSVf_6zgXBOqTWB7WfsEzetDkUtAbUczlubA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2VsywDcs0YSynH08m7c2lyJOjcgNizY-cZdF4DQ3SHWNbkcKi1aYYRkdLzsYFz45SRkBGl4FLjRL3LG1i2FnRXjPsPYR-zgOFiuHwQhXeVNdFg0SDQwfqGI,&b64e=1&sign=ede56c0c03fef4c41187a946f10100b0&keyno=1',
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
                        count: 2311,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 80,
                                percent: 3
                            },
                            {
                                value: 2,
                                count: 20,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 6,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 57,
                                percent: 2
                            },
                            {
                                value: 5,
                                count: 2148,
                                percent: 93
                            }
                        ]
                    },
                    id: 53944,
                    name: 'HDhouse.ru',
                    domain: 'hdhouse.ru',
                    registered: '2011-01-12',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, проспект Буденого, дом 53, строение 2, Торговый центр "Буденовский" павильон В-6, 105118',
                    opinionUrl: 'https://market.yandex.ru/shop/53944/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                phone: {
                    number: '+7 (495) 646-04-39',
                    sanitized: '+74956460439'
                },
                photo: {
                    width: 192,
                    height: 192,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/174581/market_319l2dnvJ9_V7944K23ccg/orig'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                                daysTo: 3,
                                orderBefore: 24
                            },
                            brief: '1-3 дня • 1 пункт магазина',
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
                                daysFrom: 5,
                                daysTo: 5
                            },
                            brief: '5&nbsp;дней'
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
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/3xe-aHwrGgAS5r4K1pX_rw?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=YQYAOJes3c3mwcYfO7q7g973k07nKJEqKsbkoCYeMuTKXFbuymwq3C4_z9ln8AenqIr3jfo5b_G6RGvQVhF6xp4EV0CWQfZwE86X6oOQcfBQhq_bYQLY52N409kgt6cOZfcJQap1fjo%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97NnHWpoDlF6OTQVhRVb-Qlh1O1wgLhLoAEbABJah5iIHnxZ9ONA3YuCmIuo2zr_1TE6EG22iYsfEnB8tLfsbumpysiYcIe2YkBFNXur_axa_4ZwBx-OEZCg1rT0fhjXFVkV9tVsIqqaDwiiNp1CGyN8JGcjVU0VbDyvbVbSiw3DVk_VZziaoGFQQfKbbeYyvjUxlvhG2dBcz3nflIPCCBf15TzS3pCR2RWgH8r0YxJh5T4AikglT6E8WNHJSbJ-y4mfUFOx7hCQqpe8WG4j0H2b_DoxPUtI5DjO-7s4QkvSiVv8EbIhXfc81VuHvXuoO_2vuYLaMTPn8Rbq5ORnmZrta6JhK4heNke-9i8IUsvxyVwDLPO9-oZRvckmMBghRlKYcdL0-Dulr9dSy7Rj1t_5WHV9JeyhUvxRufufbvZdorL6KQ-HQ8IybUGz-ALQYMwXO6XCd2tA6lotHj3QCKEEKFydZOOz5ZYixDlqSyRBVi9gJDhg5eagWWhrQ9qh14x04GlvO2ed4Nm295h_NBYW5Yj5M7xdv2CX2PbXIGQptBpOBBVPkNxWu4kHEFBdvDK_WOS2he8kS4nX543FHYeOZOjvoj5FK9GvEOXuV4kCjs-pGU__WyQ1S5JzUijciDidwtbNjQJf6fvqzOOD2dQSfmt3I718spClbfUVYh88VnwZBDtxiehEh6gbKNP07eA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsX5pqRXgTfrR-7-n--AkKExiU8ayh4-1eX7wxBMyXJFNZIonlmg7b2z0X7Mp8TbQ6Wg2AC0CJdBaF1D9eWbBiyBPRwUXJnI7YveQcvnhzva2YiLKKSZA3PilRfvppPmrWmZFhEvojHVor7H3DTfWX3JpVIAG6PLBntaCQNIOPY12cGfxhC-NN5jjIl2UKkmyVbe_UCDtGToTEpY5eUtrSO49vOYrhC3dsSbv7bgcapbesDjdl-HTTfVKhRrrFBf84641MvKa-DJVcGEvdH1uM_5onjnQPf1f9RiE2WRszj7sy86ws36WBJUQ,,&b64e=1&sign=38e33017385a451ac7438237c0642f92&keyno=1',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=53944',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 192,
                        height: 192,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/174581/market_319l2dnvJ9_V7944K23ccg/orig'
                    },
                    {
                        width: 192,
                        height: 192,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/216195/market_rk0QJcgefQyTNpg-whLjvg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/174581/market_319l2dnvJ9_V7944K23ccg/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/216195/market_rk0QJcgefQyTNpg-whLjvg/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFN2GtsHWG68yh5wo_6tcphOAEeoiDBJ-esa31ph0V08urNjswP5iOEmDv_LlvEO7ka2cVVk8Txlp_5CHfYkcFYua1QM1KrYHs9boLUVQoJxyYzYwVughw826IC-tVSeDUwfJm3S7Ypc5cTMPD5lBh9-vvXpzYSF1fzZ-T4s4lACCb2BenniHX5UYQWD8wylExIm5R7xbIDK_4ca0VCd1H6HYZQRTkqcfT-qgFS7iHph6hkVG2FrphGEXWd3Xt1bDI8dnkt0Kj3AqInyY-38XgLBEISFkcrRgo',
                wareMd5: '8Z8gXr71lGxRci-dwMxNzQ',
                name: 'Apple iPhone SE 64Gb (Rose Gold)',
                description: '',
                price: {
                    value: '28500'
                },
                cpa: true,
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRsjluDpMb_Hru0q5J1EF_19gRjd89HiDxzT1uVTA9Vwu7t6P4EeOP9tH-i5Lw_hXufqW1m3Hud-6IIOZGDEsTwcVu7tdt3Ypbg-b7ZLxSA8xglEeXs8BqGfP1mBFS9lMGOd0CAXEwGj4O4qoan_f9hT3AA9KZTZ_1tLH5j79P3TL5zFr0U4nUMJr5Y8QheYz8ARbBw7SUx5NR4yZWxUX8jQVeAij7nrCslRgeKj-ovbS_upUAXKCqAXPtwFZwL8w05XKd2eUbEoOMcnwWAFrPBXnA6GvBnyGjmEIcZ-f0K_ZqMQdMhscAnU3MbznpsIvdPghgb9rXXIwBB40spfRuZm7yejvtiV-AOZhQnw5yDy4NkohnNatZPwMJBQ-763RKg-PC7EkKSyKt581_MeYQhxXDWfqnsxnVs1EQFTfH4wkgdJOFgo07CjyiLDkRLr0wFxo7bYO4FZZggtcpHhYa7EpTDTv81UKIgLoDHPOtDIBUUMB8Kg4DM_nhDbNY1CAKSyw91K70tG7j5T6Zm9b4aswxYWuxMkBv3or5DvdeAQlmEBlHD0MWv1r-CUuN1J9JWv1Ua0_NK1Xol9LmUMeSinc4BJdLThEaR6HzTmF-isqcb8U8dEvINaQkrbbq24WZvfDj5C_h-l2Poddk5NFjG0Axt9ILXMSom6KLM9I-8b2KYMVDiuCEmldTENT8VPIb5GgCra12VaKAD4XJ2RvOr6MJJu-hnfKg3FkMARNllM2h1IkTQ4z9kXk9i3vA047WH1d_f7IJHNfAu9PY_U54PV69F0XF1i5O_9BSUMqMfeCQUMSWkJkptk4qIjnsyNfS1-ijZAPuQAVWhjFUqmbipdjOiKX4V9GPDRCUBllrjfpQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2VsywDcs0YSynH08m7c2lyJOjcgNizY-cZdF4DQ3SHWNbkcKi1aYYRl4zO3q6TWmxFM0fycpchb1ybcsS3BFIwQkQqA2pu97C9s3QcpdhITzTVh8OiX6OjY,&b64e=1&sign=61a4037cece4a4b4eb6aedd6dd1ffa32&keyno=1',
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
                        count: 2311,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 80,
                                percent: 3
                            },
                            {
                                value: 2,
                                count: 20,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 6,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 57,
                                percent: 2
                            },
                            {
                                value: 5,
                                count: 2148,
                                percent: 93
                            }
                        ]
                    },
                    id: 53944,
                    name: 'HDhouse.ru',
                    domain: 'hdhouse.ru',
                    registered: '2011-01-12',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, проспект Буденого, дом 53, строение 2, Торговый центр "Буденовский" павильон В-6, 105118',
                    opinionUrl: 'https://market.yandex.ru/shop/53944/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                phone: {
                    number: '+7 (495) 646-04-39',
                    sanitized: '+74956460439'
                },
                photo: {
                    width: 192,
                    height: 192,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/203934/market_hb_z4M055YMzYTymktGb0Q/orig'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                                daysTo: 3,
                                orderBefore: 24
                            },
                            brief: '1-3 дня • 1 пункт магазина',
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
                                daysFrom: 5,
                                daysTo: 5
                            },
                            brief: '5&nbsp;дней'
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
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/8Z8gXr71lGxRci-dwMxNzQ?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=YQYAOJes3c3XA6IX7GTrzlWSp321CjSuddBNyi_VWy3XeGSpJixrHG0fvg1xXe3vcBJF623OFE50GSI6_YYTz9mjs4LBkBH2Pey4uU4EKacVjYfr5Uaa6IQV---Z5v7cVaRUXb7Ma5g%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97NnHWpoDlF6OTQVhRVb-QliNuraBaHMtHYb_bMdcFCw5kEMvLJ7h6lsGmE93txuw0AJ6c_Psbffk1rV0E_4J61QmxL5RdN6elzEoGYCNaZwoNVRs0NMqapr3PCW37eFEVvUBDeVjMnwEyXe2_YuCcjeoJKDN_rX_pYcX-KDAwoX-j-WWQEzlzvWUxUjpRHVMYDNsq7RBw4xNR13cvUS8gkP2AIBYXPQiRl9fGA3iMjnMVP28l6vOiXLP1Gwtr34YKv4-YYG4UBr6a2Twa3MAO8a4bWbc1NB9t11PI7dnWbpKG4gTvxq1YcAqPxiFhQLNxzVNsUBndE_DUjveJ4vXTEIyc834WkMGiZSK6BXq0MuoNGliM9NpxkQaGCE488niwrA4lOG27LehKEZjULoRaAJai_qn0heGdtbPghf2lPJ0HnX1tLTqAM_49FLwW67CgS6yWptJf_fw5pqZ7pBpOJm2rDZ-JsOCiB05uRmEG5nYUGgP_xCMGIqnyQs4Fyi_d7uBHAdGlLyCEjem7zBfM2h7dQm90sfFYVMbG0mzf1SqzCg7xi7dFcefTagmI25DkCfwd33Ew7YkQ5-azgXHXa279Kmvv7goQqz0s_qNuyc28B1_VoQ9u8G-GXjWJURjtQZ4Esi5Eob4Yfp1NuC_x6PePs19oKq0cIQUGHh-DgalejkgHCyh96vBJfox-CgIIg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsX5pqRXgTfrR-7-n--AkKExiU8ayh4-1eX7wxBMyXJFNb9Nq9liP1v-BHIygIjICW7H1tv88MnBAcvTVsgkn1SWRV81JulU-CQPxGzcOHkfrBopbhU9QWp5BlbpPoMxR1avOlT4WiTKYmI-D_5gPEtgfiSpQoLlCEK3u6a1D_P6fvBDIxqJl8VIcOLrjo7NnUIvl5S-jPELO1xkqrF8i1GTkAgTexAIo-69BKY6B72cB3JLJNfh2k_nl4Bpn-wtD344kFBrUz29V6cxCRMesV0EpRBX7zqMJEu1tlo3NWa-6_Q7pwhM7teCQ,,&b64e=1&sign=071d47a21c3f2dd3df04d44fe07268ff&keyno=1',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=53944',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 192,
                        height: 192,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/203934/market_hb_z4M055YMzYTymktGb0Q/orig'
                    },
                    {
                        width: 192,
                        height: 192,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/204557/market_m-ttrws-0NCygUOJrr-skQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/203934/market_hb_z4M055YMzYTymktGb0Q/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/204557/market_m-ttrws-0NCygUOJrr-skQ/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFwXfShqAXvesRIV9DQF_FqX8ePivr7JAqiIcpoxvNWGycL-YZzrXC8tWlik2EN91okxRtmxRywLSZ1EaI7vTbKXexHPZMj5-e4TLovrbr4fVwsFG3Ev0QhtFUUc-OZXX4igkxhzOhPuVuBqFIDCnehcH6GWVC4LMKNrNWlaTw6ApsZB9RaE54jjN9YK7mRWdOjgOiYAeI4Hg6DD5Lu2Qd7h74y4Hg53HfB2NkyOHTONsNvDQTWBpwaPe87oatDD863WzQuqzkNQW-m_xQKiV7ztrCghM0BB8g',
                wareMd5: 'SDImBlqfXiS6lXmU8AU-vQ',
                name: 'Apple iPhone SE 64Gb (Siver)',
                description: '',
                price: {
                    value: '28500'
                },
                cpa: true,
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRsjluDpMb_Hru0q5J1EF_19gRjd89HiDxzT1uVTA9Vwu7t6P4EeOP9tH-i5Lw_hXufqW1m3Hud-6IIOZGDEsTwcVu7tdt3Ypbg-b7ZLxSA8xglEeXs8BqGfP1mBFS9lMGOd0CAXEwGj4O4qoan_f9hT3AA9KZTZ_1tLH5j79P3TL5zFr0U4nUMJr5Y8QheYz8AqjCdQIpaX95bhw3MQT8m_zkI4phKXsVyIjCE43pwdwjNcvj1nh0mPD239qxIxcHYfq2mgfk80_1x-dv-Wyd3ihqAd4bn-y7Bvog2WKqi0NvtGXWSDh16gtl-Y33zvcpVDyZWiMmXpag5EE9-KXbjCm4NZ3N8ug_ea2VwWVN7ebQG8UR-Of-2EwpMWojzYKbH_J9GLqYhqSmAoUa91-GMOYD3F7SzRfw5Mnu82_aQbcn89Dlho00TAtvuOinCb29e0AXb00IqqUJGMNeO1XKa0_pXlWyHPnCNSMVTppVfOYonoxDHrqOX7OQocc8iUDjhAhwdDQU6hRhsoa5xFyEZwuZvySFEFLggUpTg4okQNA3Ly971L6w2Wt4ckJe3LX2sWIa5nN-aiTeqT9gms2KtaYcb2fdtAFtzQnfCAzwJBiN1yy-DluV33XdOB4sn1oYPsyydGSjJ_0LRQSOcDiLh5ywpoDwHvbgqkW4GwenGZ0zFRe0-5DadTfWc1-nn5M3fuVXFrpmL3ow0myPeqaZl0RbO2DZFF_T2V8kZiDi6frzaNneYJvqu3YrY55caqpi40cOcyJegmgMIBv0S_5XNs5OaCYUM5YeJUdYYpX1P2U3e4BKt68csCVYF66b_hYY-gt_kc1wisAG4vizo7sobVi6n5fOMa2z1gkVz0_EtweQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2VsywDcs0YSynH08m7c2lyJOjcgNizY-cZdF4DQ3SHWNbkcKi1aYYRmzx0Cw66s1E5r2PBI-SNeoedUNa6TVDR0JwS1ZWgB8ymgoHnw6u9LuwpCY4PkwqQE,&b64e=1&sign=80c7f4de1254ef1d158f66184429c44b&keyno=1',
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
                        count: 2311,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 80,
                                percent: 3
                            },
                            {
                                value: 2,
                                count: 20,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 6,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 57,
                                percent: 2
                            },
                            {
                                value: 5,
                                count: 2148,
                                percent: 93
                            }
                        ]
                    },
                    id: 53944,
                    name: 'HDhouse.ru',
                    domain: 'hdhouse.ru',
                    registered: '2011-01-12',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, проспект Буденого, дом 53, строение 2, Торговый центр "Буденовский" павильон В-6, 105118',
                    opinionUrl: 'https://market.yandex.ru/shop/53944/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                phone: {
                    number: '+7 (495) 646-04-39',
                    sanitized: '+74956460439'
                },
                photo: {
                    width: 192,
                    height: 192,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/225051/market_dWJAug_Rew1c0z1pqTIOzA/orig'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                                daysTo: 3,
                                orderBefore: 24
                            },
                            brief: '1-3 дня • 1 пункт магазина',
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
                                daysFrom: 5,
                                daysTo: 5
                            },
                            brief: '5&nbsp;дней'
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
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/SDImBlqfXiS6lXmU8AU-vQ?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=YQYAOJes3c1DXFZLE5wWsjZ3D7Mwli8kaQQWBZT5Tl7RHxxVrqgMI3BI656MUyuAvcpExyOWrCchgvPFzBoH36tH1uUzkUzcmceH60AByZLt95OPpW_Z6WUfygXlfnpZRJHiHyWoEfs%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97NnHWpoDlF6OTQVhRVb-QlhOwScVQycgbXzwhjr6OMtHyiNa0eYJQz_JUSPbj9wuUC07A2qX51NkgIU8T27VqA4RzvBaFp1DZK2NcMMF_yQfCB41TVmhVxrrKUkrxNLmgVJMEykZ09goDz3OFLP-sDfOiKy-f0u2F6GCcSuC6BCexvE7_cjXUCHhhgg3tfaW2GKubZ-qpFP06J0txNYGtHJGCRn6jPlpyDmRoDobtkhjUHiEdiXu1MSjIp_8pRkNrD10w3sGHrdQ4tluBd96nbkWSKQ60hFOwEn9QXmpN0Ox8xQQFuTRWOYUR2nm60fe--E4EX6ualnwQW0uutWud1kYGw_DjprrqtgwRuETqv1CMno5ynR1YEDt9mMl4_uKfz2RjB_VkNj93DFGYZUpfpL-1u9G_5N9HyJuaX6H7frwxEO8XG70QDxu_QJlke26nQQGbsCMW4U6TiTuj-Xl_YBg9Vtb5oyLkdVvFEjlBOmCg5btAaL25qdnx39BoBCqDDZmtnAD3lGI3ph3RWN8AgiPgB6h5yQViV-_ddmRCQ4jcbzjzCPRxUduqTQtxm93vy9iGNFDFIm9THOWiZ7c7NqMISJqI-nqDy1x-8ddsUzv0KbmR5yXC0wOUP24M8GNauM8r4f7ktlujTM3K7vF3iEVMzgEhkXifa92NogAEySVAeN4Dz2n7B8C5PCke5G3AQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsX5pqRXgTfrR-7-n--AkKExiU8ayh4-1eX7wxBMyXJFNaOVS674EDxAhCo1MDTm1FzqhDVFTP7GWHCIqyj3qr88TMg9sEAsK5e09hOMMrtyWGq9fBdHK1XZfVsF1YGua0MAeu8r0QwFGzlV_Zsn4BNWH5zjMw7gFCN54O-jI9ZccqDymBUKYLmLQY5_urOMGJRltD0-TOPYFuGphaCAVtWG0WX55qbQE8OUluXOWGQBgFuwiUQ7Q5h8b1qpYEFmmceam8JZYiECYRXuS1PSa03IRjdg4oYFjxSSf9BdwLPd0iUDvK8TiFb3g,,&b64e=1&sign=7a6ac8026c6e18df92df98ef3deec111&keyno=1',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=53944',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 192,
                        height: 192,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/225051/market_dWJAug_Rew1c0z1pqTIOzA/orig'
                    },
                    {
                        width: 192,
                        height: 192,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/245136/market_p__Ir6TXxZ-P-_3z3hoSPw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/225051/market_dWJAug_Rew1c0z1pqTIOzA/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/245136/market_p__Ir6TXxZ-P-_3z3hoSPw/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFJvtP45Bz7U87tfknRq5lBsIXvjfr3u2j2xLlMOnovmIJVhP15nkxehkhrYSCdR_IOU-Y5cjHZFP9VN_iqdtgQqbViMpNuCv8L8mfIpzhwHvlG7m6tY56TH41Ulxov2nSOzNf8-yle5njtre5ir_S-6JcIthdS8QAKoV0Ld7ryR6MN0dPAR0h1Ew6npuqLdb2lF7yamZTq8UdUq2Ul1Us1FFRVai_P_9Wi11fV1evtMCSbiVSQboj81dYxnHmwaoUDHznyrmp8JosA0ZRPmesZycXDf5UAhxQ',
                wareMd5: '70NhnJ-c0Znag9MQdyBE9w',
                name: 'Apple iPhone SE 64Gb (Space Gray)',
                description: '',
                price: {
                    value: '28500'
                },
                cpa: true,
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRsjluDpMb_Hru0q5J1EF_19gRjd89HiDxzT1uVTA9Vwu7t6P4EeOP9tH-i5Lw_hXufqW1m3Hud-6IIOZGDEsTwcVu7tdt3Ypbg-b7ZLxSA8xglEeXs8BqGfP1mBFS9lMGOd0CAXEwGj4O4qoan_f9hT3AA9KZTZ_1tLH5j79P3TL5zFr0U4nUMJr5Y8QheYz8BFgUo2fZ5GK3jb7aniDPewiEZ39LfvVnk7RffPNZivKgDshwePKPdeuxXjqM9jPt7AysGrhVX-LtG2dk_NJ3ac3mLtZ-aVPRlckEz424fXs38xRVlJ2WKChi7y6Hn97zjWLcFrToCbj0QvCOcxtvALoZNfSXMyCth8qlOyhkX5TeZKONyhF64heCTlvspYYXroba6nkNhQKrkr2DvFQlZxHLSjy3irKa1e5NtcJQZxNPZVpQlJ98BC4vzGjh_MuzCXCyADmbB9v0CelQq6ydpq6utpRIuR4JFPYAgWs7Yj08tWYi2GpO4P-VFCi662Jn-8z16kOn4AKE4rP7uNR1XBmtjQI8G77EuwZwLcI0lWUEiqRTtTosVFh4NSddt727MEXciJxPATC9QlzIMaETJMG0ZkwisbtE8raYCv_aqQ0wQQE35zSh8_ca7zHwEn71XME6ZTE4l2kSgYKd2kJagYCpfoODLhBrcUDOGb-0rGDFH2ykcS_F4S7uHdwz8xCbFyd8ZkfL6IcGUPY1745fHDg4f0PoEUiCE0GQZ_CMAbgJisKIoJdqvQywY5CKawxyaAi-SRIocFSwVWNZRvQa0j5BsZ4tfnfWsgunNvOrb4O1Xg7OLm9IR4T81WcWXzl97HYSGAj7uHzEQmyUwnl3V_mhRQeX2SiWdGn8_x11NXeQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2VsywDcs0YSynH08m7c2lyJOjcgNizY-cZdF4DQ3SHWNbkcKi1aYYRmLcwSwntplC00OCZmp2KZi1XQjl2stKdgsgvT5UCClpQ5_j0sECuRyLThpYLP3IPI,&b64e=1&sign=a07362c9e4f350f005d864ddb885e9d3&keyno=1',
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
                        count: 2311,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 80,
                                percent: 3
                            },
                            {
                                value: 2,
                                count: 20,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 6,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 57,
                                percent: 2
                            },
                            {
                                value: 5,
                                count: 2148,
                                percent: 93
                            }
                        ]
                    },
                    id: 53944,
                    name: 'HDhouse.ru',
                    domain: 'hdhouse.ru',
                    registered: '2011-01-12',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, проспект Буденого, дом 53, строение 2, Торговый центр "Буденовский" павильон В-6, 105118',
                    opinionUrl: 'https://market.yandex.ru/shop/53944/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 13584123
                },
                phone: {
                    number: '+7 (495) 646-04-39',
                    sanitized: '+74956460439'
                },
                photo: {
                    width: 192,
                    height: 192,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/216195/market_jDnW-XLhQ5EojXZ29sxdkg/orig'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
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
                                daysTo: 3,
                                orderBefore: 24
                            },
                            brief: '1-3 дня • 1 пункт магазина',
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
                                daysFrom: 5,
                                daysTo: 5
                            },
                            brief: '5&nbsp;дней'
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
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/70NhnJ-c0Znag9MQdyBE9w?hid=91491&model_id=13584123&pp=1002&clid=2210590&distr_type=4&cpc=YQYAOJes3c1yFlq799ZglAkCFQaoVhXMoRTF0mCgLkkAy6iVW1Qkp73WA9AGuJq0-RGw7VMLi2p7J-l9v3s7UTekp8nP1xMzooOVgWrqnPbWCXXjUFVXvwYw3ZhexeFe5JoGnPZzJ_Q%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97NnHWpoDlF6OTQVhRVb-QlgFCM6IHWyU5sMMm9E9gsXNqp5Q0jzp0HQpZbVoYFtSjA1qVFnN_3EbPZ2YVqnb-KzKXyNzoeVPOeDaBBjFHCeFZxy18e8L7KWeW37KGFcQNGuFb8awj-nuHHztIAFLj6Djvdc6TDgfXri9XgNnZ52f2b7skas764Tr7uCVfj7OJsB1Gxe5YxMoTBYQFrz_yqv_0hWmWHJ-51BnB6zivUL4HTiVbx0hzlX8BzZIAekzUK8iy2MJSu7zDyO87QMZa60gzQYOuKmWRkzBOVY-mHXd7jtBEUUhV3AF_4vv69gI66v3UJT1ACK-Xff77Xp7zRvKl3rtWYyXeRgsexDQwd3lSFbN4Zw7VtG5oWIdQ-Zw5tUsMkg2d9r-dooKFFURnKmZcfeHw8zeBLg-UCSmPi1OfGvyOXQnrzNyI47rliKHtjaX3F-VRsN-tJ3tZrB2y9hi1BpojBkNr-OipKNjSWRlL3jrH_nVi_dqaXoKpfNVYDrf7Zl3nkC_J6vJXIpCikw4rqb9iux9_ef6my4MRuti-KtE0EEDMkatUPBcD29OKFyiHajF3tfiQjEoFKrLPJHvYiTYzZ3fR7zpFwtf9TX18kSWyOyWCdWp0YicfR5zwkD61E3lqFw--glW_-5OQ3_aiHJgkuBXGXFRaFklD2WpMEmdNh1bOilQkH98P9Mx7A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsX5pqRXgTfrR-7-n--AkKExiU8ayh4-1eX7wxBMyXJFNbQGCDEndGKNu4O6mq7lk4HA_lxoDZ0MnE3iD8u1BNTX_qcjNEIXA7lmdrszAOIMXtY_MVlPdS1RcjvzrZ3lt1ioJTqQMDoeZ-iYEHat5MZaAfQmx1ualNsvXQVe_tyjUak_IN6wzSRVdF2fjVhToo_FT_zYaSO3yvYNsZFX3HmP87YsKOftRNzs3yOvmwB-XphUZ8W_fUNXHkUByYvaJUvInglq9wpzKkU7rfNM_tnyUmIQ4Xokq5gjJDeiklcJkzlWpdLhS4P5Q,,&b64e=1&sign=d208c4d28c0deb2e346fe928ab1a04b4&keyno=1',
                offersLink: 'https://market.yandex.ru/product/13584123/offers?pp=1002&clid=2210590&distr_type=4&fesh=53944',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 192,
                        height: 192,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/216195/market_jDnW-XLhQ5EojXZ29sxdkg/orig'
                    },
                    {
                        width: 192,
                        height: 192,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/207337/market_wun-J9rKNAkq9Lw57YbASQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/216195/market_jDnW-XLhQ5EojXZ29sxdkg/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/207337/market_wun-J9rKNAkq9Lw57YbASQ/120x160'
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
