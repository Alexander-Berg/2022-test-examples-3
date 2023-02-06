/* eslint-disable max-len */

/**
 * Search
 * Model
 */

'use strict';

const ApiMock = require('./../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/search\.json/;

const query = {
    text: 'Комплект постельного Лисички, 1,5-спальный, наволочка 50*70, хлопок, Letto',
    category_id: 12894020
};

const result = {
    comment: 'text = "Комплект постельного Лисички, 1,5-спальный, наволочка 50*70, хлопок, Letto"',
    status: 200,
    body: {
        'searchResult': {
            'page': 1,
            'count': 30,
            'total': 2792,
            'requestParams': {
                'text': 'Комплект постельного Лисички, 1,5-спальный, наволочка 50*70, хлопок, Letto',
                'actualText': 'Комплект постельного Лисички, 1,5-спальный, наволочка 50*70, хлопок, Letto',
                'checkSpelling': false
            },
            'results': [
                {
                    'offer': {
                        'id': 'yDpJekrrgZHPlcVFxxvFe-5G-Nd34-k2WywDfrlOv_ILha8S3L_VzDAh2BxtN0R9Vx85YIfiaTAc4SsiluJmM87TnbON0ttIiFSeNj8jDmPOrtFEG0b0ucJ-39slFhfR2dYMSBzGdoCxhox6s1ap4rYsrolNIVpkIg1FXsmc5bfuN7XjqKIjZZ6jJbI71-UAEcrOnYEsFlGT0hsbNyJmTIY9Alw5Nygv-7GbuGECux0xY1LbPT5eN6Y7op0pf6KXO29Dng3PfGxi9OqZGTAnnSbpAef0fvHwfW1Hzx6St0o',
                        'wareMd5': 'lXNJfVnZpBj0LIRTYOyaoQ',
                        'name': 'Комплект детского постельного белья Letto \'Сова\', 1,5 спальный, наволочка 50 x 70 см, цвет: желтый',
                        'onStock': 1,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mc2aORABufwe9msAwzf5U4dAG02-R_VLC3JA-qv3u51X9ee_VVWs1S2Ce3om_p2l1Sq9JNeMqcTYYHzPBiGOiHxvDpwi-9j-IJsG1oFcefwtlRQaWgn9ggPJKL-mahkXWxgY6aVH9UwWGJpZGje-j0GBQVr-hLPNJy5wtRZpf6Abpge8LNL_2H_A0JTVIzdAAB-bTo4_ZnPD7jnc0DZlexxwhN2_x_r2XGjeWpBQCLHAwhYGC6CCjBvV1VCkW36AcvzEt6U7BWButOm1m4SIp7wUvp41B1V0AyaolFGl1BvIOJTWwCPFOMpImabjGuQyKFJalUevige5bbUq7JQC6QJN4UWri4aZI5og7bfg3vYltkLSlMRF2YfbA8-AHzJbpRQ0pi9vI9aTM-KB2GhFkNVFkB7lwnPO2giUHGySXAavJikxp55ATR5REjQvIL81OIlpGb_sX9pOgifSIAhQ5OSJcPqiv09mQjV0Tui31tWyRZnkLsyOLFbpBI4705-8jt2GUIJCfZHZABUUmrZ67XmMyarJmn5-O8w4Drg8bfN3pt_544fVHzv6a9DhzWQSNvU-Trp_ArGKf_xYqAt7nlKu2SprG2AucbtFw0yHS5tFU9kwPi_0OKx_W8kr71fXK0IkHUZx1KATAsCnwHX7r9I_wKEOr8ac3dhyE8OpTA649_bQbL2sR7Ak2DEJ6QrtbWf0LW1o8yzSMocxWlF-lSndxIDRnGRYDdLDm3PYlC_5ejvFdtg2tljlnwMsA6XT0zkftJjDFOEsXZPZDfJdAwSsad7WOOkX2wTMQgbKprfzHZHT9L6Y2DvvlU5ggCpUNbF4kZcud6rWxFDxTds5M-RwouWeOx3G2A,,?data=QVyKqSPyGQwNvdoowNEPjUO6Lp2FicSHwuLrkBRU9MB2L71swKP9DwSZnoHPlJhRZwJIdUCMFHR0y3lKlYVurrysCxPPcqiRohzLH635TMvUpPy-tgVZYl3zRDz0uJ1dQKILpNELEwyUckpuD-Lq4_FeYqDrl-dJ3sDp0g4fuHYv0XBjnI1EobbW5ug468do2p2LDnqcHZfNfYFoS-mi8vzqV5k5pUwLvoTib6UoSHbPpMq8oUIcOWc-Mdj-wOUDTN3lDmSZBdNbCZNBn-HsYvpvaR5pJEy527awB4Xh4uWFSPsxqE9tsrKPnXqs38TALPjLVb3mlmt9-V9oWXs6Vg,,&b64e=1&sign=7e1fedf0258fcf9171ad8fbe746f1ff3&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 155,
                            'name': 'OZON.ru',
                            'shopName': 'OZON.ru',
                            'url': 'ozon.ru',
                            'status': 'actual',
                            'rating': 2,
                            'gradeTotal': 34059,
                            'regionId': 213,
                            'createdAt': '2000-12-22'
                        },
                        'phone': {
                            'number': '+7 (495) 730 67 67',
                            'sanitizedNumber': '+74957306767',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mc2aORABufwe9msAwzf5U4dAG02-R_VLC3JA-qv3u51X9ee_VVWs1S2Ce3om_p2l1Sq9JNeMqcTYYHzPBiGOiHxvDpwi-9j-IJsG1oFcefwtlRQaWgn9ggPJKL-mahkXWxgY6aVH9UwWGJpZGje-j0GBQVr-hLPNJy5wtRZpf6Abpge8LNL_2H8BFHUSrG-lOR-1ij8bsfePC_fIm55BiXH8rFMLrojoYVB-tYSH5xb9NPxQAR0mk3Pp4MuF5b0yr2I_kXIW7KbLkj-HaJQnTfZfqcusZeZ8rk51cQKaYhrxqaACiR5zRDWIgeVJLGcAAQqJouruwA79WM9FCxYsrNwpTlfazxrQLozjp66SLdEKvYJjUpyU_euLiQcXkYPJOyxx2A7ydw5QuTtSkjC-0IH6MIL39Nz__FCG5CNuUooMK1Cbl0KB9I0levmR7YCtfujTakIb3-b6FLE1e7V1RKFxgL61wyhTu7JIkSTIVTLOnb_sZTtxmRzFXGnLVdWXfSy-CmwEFlvqopbiiQS5toE1pjpth51uK2nwyc_oPn3x5YJh3pRCihAdyTrXx14F9rQe82E1X5LX2g6nyXbPO_WxEJTy_ICfiaV9ETlnkgg51nAJ123UzyIZ_8zFgFeR1DyVOOntRTTs_yX4ukpM7vE9Amw0_s4abJWtIlAhfpjffj7VMaFy4UZQcCSRKGT8wo6m66u8IZy2B1uCF1lUfiR5dC4-inJL5AoIZj1jeJN-56nqHGWaswTgAODpdqwi0je8_YSNPE60GAVRxNGM_ZKMhz1Rjn9k952HcuEdawvgnmTwtb1HnxtKONyX0-xCvS_kWzQwMDgqChtPMaa8_liq0r5-7KW_xw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-3vyopj3aB1rzne8wmo2InJBM48HNVgZF2J2HyAN1t6a6EJCBcbovQCAZ6kUu5vxcS5a0WrwNdDc4tczEEcXih3DBrxVrR2MXnLcvTD2TtUDv5DacO0OGJNXj_JpaVLHuGGR89ITdQfg,,&b64e=1&sign=aa61c80d3f60a74b327cc563856cec3b&keyno=1'
                        },
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 1,
                                    'dayTo': 1,
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 299 руб., возможен самовывоз',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/480326/market_9Ruo369FbCeib2p_4yez1w/orig',
                                'width': 1000,
                                'height': 1000
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/480326/market_9Ruo369FbCeib2p_4yez1w/orig',
                            'width': 1000,
                            'height': 1000
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/480326/market_9Ruo369FbCeib2p_4yez1w/120x160',
                                'width': 160,
                                'height': 160
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '495',
                                'number': '730-6767'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '5',
                                    'workingHoursFrom': '9:00',
                                    'workingHoursTill': '20:30'
                                },
                                {
                                    'workingDaysFrom': '6',
                                    'workingDaysTill': '7',
                                    'workingHoursFrom': '10:00',
                                    'workingHoursTill': '20:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.604592',
                                'latitude': '55.76172'
                            },
                            'pointId': '430165',
                            'pointName': 'Пункт выдачи заказов Ozon.ru  м. Тверская',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Большой Гнездниковский пер',
                            'premiseNumber': '3',
                            'shopId': 155
                        },
                        'warranty': 0,
                        'description': 'Яркий комплект постельного белья в хлопковом исполнении и с хорошими устойчивыми красителями - по очень доступной цене! Эта модель произведена из традиционной российский бязи, плотного плетения. Такое белье прослужит долго и выдержит много стирок. Рекомендуется перед первым использованием постирать, но не пересушивать. Применение кондиционера при стирке сделает такое постельное белье мягче и комфортней. Пододеяльник на молнии.',
                        'outletCount': 1,
                        'vendor': {
                            'id': 10738511,
                            'site': 'http://www.domashniy-textil.ru',
                            'link': 'https://market.yandex.ru/brands/10738511?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Letto'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/lXNJfVnZpBj0LIRTYOyaoQ?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=U9xqPzGtajIkxQOj0kAthF7qzNzfkFxIhthK2kaZVeJgHab65tcR3xNp_DJXXSwnmVRonIR2bCXzlStMyFjMTrsr_T3nqPyqi2l--jsr7c1JHxgywbAR3W1oK_bV454a&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 10738511
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZFQ7RzJB4YYNA6FUUqSQkWw8bplEtCRkjWO58jOFDL7Wm6-EFkULXTT6ivuRBDNnS8wjAkmBl5fLGq-MmyiV9vW8QTajfhfMoymEmzV7g5C77ASUxIub-wdm6pPCYw2oYo88t2zdXHhcOEEjE2JtwKdoNNWLLw88arhKx2TweNfERVt-nAYv-ZzDAbqEKR6R2-tulVVYUXyV1kFvOtmJAJ2LxOr4J8_KdIlswOTCwtCJ1T1BYx6cnO_rNH8sMwW0A7ZVWvB2zRucLt7JfGomNluneUdRACXfII',
                        'wareMd5': '3q-r-m6WQqEoN-USyQdT3g',
                        'name': 'Комплект детского постельного белья Letto \'Игрушки\', 1,5 спальный, наволочка 50 x 70 см, цвет: голубой',
                        'onStock': 1,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mb8Vru18OkvgUPN77MW2zoxiFva35EF97s81iE5OEjXoSBWN6O2IiDjPDjaQdSLMHiqjE4CnQIEjAg4XPY64cbGJmu1kgvh9ATrtdtOaTyw4rJMqNTz_pcjl1zJXgY2YeSEZKjULWJA4mHQn9qX2EIN6LJD_DFa0_Sytn7hNSrc1P9zNt6HUmm2llb2LuNw7ejfGUjirpJiJ7wiinTi5E6A7OUNQYmosqCJ0FqPbG6Qdj363cj026tijGKqAxl49sRxX98b9Sv-V0t1SZcAz4bcZ_cXnV0NsUUKqkV4FlZHSe5NUODKhVMCW0FqORfaWfzk5aTqMBJ9R5ThUvQWL6wvQ6bBYCumQIUj2l00DCAcvdUpMancUn8DiI0WI9GhJyRBjn3La4YxA2Z3xiUsmvPQ55zjj6GFcskmTHT2DObsMKbY2wtLrXXmLRnqFl7rN_me6Ro2NG_VQXkjuj1cniGUQa-GF5IP49ThY9yk34WumkxAZifGhy7KOGlpg26cRHZgkOQYGoIIR6kSrbQgez850gRyZasGeXz-IM92FmwKZGaoET8z7aJewWS_j81DzSY7dRtQQCns9udXxa3F_5FwpRbrZCgrCTr6eZ5g1x3qFHdFFjDsjHVEmwH3vW9c5xVED8euoCubAmkQQpi0_NnhcAUR_DqPI8lvnGd7hy4V6arcsmgEKuyrpcb2hQIkeC1q4HFxrCrt0sCb-qJRM4M3y1re5RQQT_Bm_DlcHUhNBQpzVVwq3uuMnr2cWR3y0QTnCg4Q385Fj0YJkRAx4a9Witdqqdvhrc0HejaaTZw4BB-48p6CXwhDVFU44vLwjOOP0PW6ySu0FgYg4h6xh4-2HW1KNthFELw,,?data=QVyKqSPyGQwNvdoowNEPjUO6Lp2FicSHwuLrkBRU9MB2L71swKP9DwSZnoHPlJhRZwJIdUCMFHSSKqs2QrmAxvUA-sJcm3BZP0K79JffA74QO4qZc4Hryp5Lawix6j0z0VDlx7JQTBywmbB6HBQ-iq_wzlvsfMsbw98z48D_AE4z4tC4rb_MhfSlhdpvONosvIhAS1ReyOhlOvzUSvIzhfAvO9SPjzvTSKbmJPuoX-5MRmqGc9QdWVR08w227ST3yeeIraFSu86RFwm9z7fIJ2pq6mYBMN9HOlMUImgUbo7qE9DIEDGgQ4ySVfgvJzxmI4DnphPLPD6bBgSmDoFb_Q,,&b64e=1&sign=0aeb7b1173cf8642823a43b66d1996bd&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 155,
                            'name': 'OZON.ru',
                            'shopName': 'OZON.ru',
                            'url': 'ozon.ru',
                            'status': 'actual',
                            'rating': 2,
                            'gradeTotal': 34059,
                            'regionId': 213,
                            'createdAt': '2000-12-22'
                        },
                        'phone': {
                            'number': '+7 (495) 730 67 67',
                            'sanitizedNumber': '+74957306767',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mb8Vru18OkvgUPN77MW2zoxiFva35EF97s81iE5OEjXoSBWN6O2IiDjPDjaQdSLMHiqjE4CnQIEjAg4XPY64cbGJmu1kgvh9ATrtdtOaTyw4rJMqNTz_pcjl1zJXgY2YeSEZKjULWJA4mHQn9qX2EIN6LJD_DFa0_Sytn7hNSrc1P9zNt6HUmm1opkydmH90UMgaElSTOH7IBaxLVx5uhsmhlw20Cce09oWNIT-_VzOzn4LgG46pujal4p78KqlxKAPNPC6P-kcGJyWbPiYSfULminTADWe3NO5N-e9BKDT4i-jSlI59EDhMc22Bi715Hc6qpl2tRElh8Pv-hLya_74GnLye7XyrunWDr1BnPQU1eqKNa5JBxOhsmOk9krL_U2UEXLphFCbBWuayHYoHFUJl1aEEemyHDPS9uwVX9vEtfc84lyFVh8STmWYqmkmsQ-mEPxn9YDtUnxQTdGy_cRwLumXvuU5QcumJ-LIgGaaBjxoyPOi4xnKZ0PtfArxYvLMa3-c-dxnK2aniC10miVboQXwoShOYux4cBLV1TYmXmJutZGEFGblNof-MrDRlWjyH-ES5YJ3adnE_FfiSmq7lz6zOOEmlF2Jt2BYde1zpMI6_FqRKNhOvFPpBkl8ZRNMliiFVNHLDPigFWI0dYRnlzZp3vBJJuUKyV-_lMUqF99IVG3sFrUCjUdLnPIj4-tUue81F7GQHLEEhDqpQDCAC7hPOVLzF3BTlmHTHHxBp1g4ZZn0BQqgxdwWnlSwdqy7_EhWXZ2zAS9G5kDQZSFNZnV5DNDG6QPH6LU4cALp4HnlozNE7Lxia1aiaYYcPoBm5g3Q7ix6WMEwXvFXMSk7U3MgJDSVwMQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_gDBF4moW-bsKP8qPPmzwH3zg3LFoVGchb_S1UhNxeMIBuiVUdbb-_FxcfR3v866wj2k4Mlj7lzZdhxGc9hSlfHUoOc8iSXHMnLvamY9Nvjqr3wYFYyMdmTAbevfFADL_S-RorczURbQ,,&b64e=1&sign=4e8b0cb943e097c1f4463f09399cb901&keyno=1'
                        },
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 1,
                                    'dayTo': 1,
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 299 руб., возможен самовывоз',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/171655/market_pIaIvCtkP9dcm1O6zgRilA/orig',
                                'width': 900,
                                'height': 900
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/171655/market_pIaIvCtkP9dcm1O6zgRilA/orig',
                            'width': 900,
                            'height': 900
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/171655/market_pIaIvCtkP9dcm1O6zgRilA/120x160',
                                'width': 160,
                                'height': 160
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '495',
                                'number': '730-6767'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '5',
                                    'workingHoursFrom': '9:00',
                                    'workingHoursTill': '20:30'
                                },
                                {
                                    'workingDaysFrom': '6',
                                    'workingDaysTill': '7',
                                    'workingHoursFrom': '10:00',
                                    'workingHoursTill': '20:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.604592',
                                'latitude': '55.76172'
                            },
                            'pointId': '430165',
                            'pointName': 'Пункт выдачи заказов Ozon.ru  м. Тверская',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Большой Гнездниковский пер',
                            'premiseNumber': '3',
                            'shopId': 155
                        },
                        'warranty': 0,
                        'description': 'Симпатичный комплект постельного белья, украшенный ярким анималистическим принтом. Эта модель произведена из плотного хлопка, полотняного плетения, группы \'перкаль\', с использованием современных устойчивых и в то же время, гипоаллергенных красителей. Такое белье прослужит долго и выдержит много стирок. Рекомендуется перед первым использованием постирать, но не пересушивать. Применение кондиционера при стирке сделает такое постельное белье мягче и комфортней. Пододеяльник на молнии.',
                        'outletCount': 1,
                        'vendor': {
                            'id': 10738511,
                            'site': 'http://www.domashniy-textil.ru',
                            'link': 'https://market.yandex.ru/brands/10738511?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Letto'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/3q-r-m6WQqEoN-USyQdT3g?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=weCKmFGAvajf2TlQ59qU087QKWSq7suWd_pV0s4EDO1LCsUmCaI8n7k3HBq_Ssj3fAYPbE_J0p6e_NasCC7kab_lMRgI3cPKs9d9jff694pQSRmdv-jXx2GDRNCgl-j2&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 10738511
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZF3toVihjCxPpw1-Tx3uVWOXO9mDIBaWyTA0KT8mxy7G4jBl5tUrvBQEsJMKXak2wSqx2SDgtpQfTqwcEgRXHJg0bQ0OwgLcazCzqNvpMhJQgvfqrY0zcnAza4xUw6dV2-VN0CPLyG8_ktoTOfHhMLW3TG7TccL8s30IV1qLe0G4uBlUjgVRYS4wvpnMKrkwILU2Pz6pg-l-CXZYHFqXrth3uo4x0GqZGy5yg39mBF9jbNIbxAj7wESJy7SgACbMjVG_ssPtibon96ame9nJlomunUaF3vcR0M',
                        'wareMd5': '2Sk09zhsVb-n2Mg5KhKX7Q',
                        'name': 'Комплект детского постельного белья Letto \'Каляка\', 1,5-спальный, наволочка 50x70, цвет: белый',
                        'onStock': 1,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mb8Vru18OkvgUPN77MW2zoxiFva35EF97s81iE5OEjXoSBWN6O2IiDjPDjaQdSLMHiqjE4CnQIEjAg4XPY64cbGJmu1kgvh9ATrtdtOaTyw4rJMqNTz_pcjl1zJXgY2YeSEZKjULWJA4mHQn9qX2EIN6LJD_DFa0_Sytn7hNSrc1P9zNt6HUmm2PO9V90yNDcGl-a1A64Q24b0QS9eS6p8kJARyMK6hByu2fCUHGlqew5tBFwY4YOanqQtw4bp0LFYMGyu3A8oXofXPGxwWwcT5UaBGOFEd0t2PVkGHXNVCBqfElvSsD9K84BXbtaWUhsMAn_wuHisiCq4v4qyykLHiuf1ld4G-eXaPVdQw8vCO_zYN3-R4mnXjd5gJk7cmIXRslt3tyaW2ziPUY_nnLUF_xlKmI4fjmCFIni5B1GgCBJ4wV9M3fQbg9vnYe05D-0oI_x9KhNnoC0ux29p6wRfpdmy1Ezt5DalzkmkuFyQ8pw9w6YW-9uAZF4JDAMAinyrTGElBhfhzZ2yXQJW0MIYW36dkMnCgpEYT6mxS7puhH4PmUmuokt5Tp3fOKz5E2WMUqM6IQQuVuMahjuLWJvtWevd9zlorrzL3QT9H1vk4uj4T7RMTcUhGNaaAR4XRh404G-MfWxZV2YnzEetSWxFQ8F9YF60BykNNvu0AaO9QtlqjT9eAlpunh-IOhidg5Cb0q7CPKf4V_s44BW9XW7Nnt1Q5imH5VpVtNLFwYZE4B8b4-jafjxkkGzotqwFiJQfLhF943xcH97jL5qh4i7xG7vmVSqnZj4CT80D36Y5-d_ndTSWnI9Ye2JTJlJigxWTMa7s3kdfVtZzlFVzArEpVQtQ-xFqFc-w,,?data=QVyKqSPyGQwNvdoowNEPjUO6Lp2FicSHwuLrkBRU9MB2L71swKP9DwSZnoHPlJhRZwJIdUCMFHSPmkslk36SuO2yPq2Xp3DUZW1F6CyjWQDhtIVn7eBaS7Q_DkfY00TtZMRvvIpZqqh3HCILmL0-YJzor1LUOOSgtvzTLjrpVF81ZTOPhqB3YVvLvddHIKlR3BuUAOkYjYoS4K6dE6DaRKelXpr64o5N0-DV7WhoryyZGg-HT8Icy6MFA2udQbSi84uIqSW1WYEWnSGqAPIpxyI0CLDX3U8wAf-5g9tDu2BnLGa8KkOSL8kZ1XW6GLpv7b5Pd4yeI3RhaOMHyCO6Yw,,&b64e=1&sign=b3d39c71d3f9d3d911c90a1ffd75beef&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 155,
                            'name': 'OZON.ru',
                            'shopName': 'OZON.ru',
                            'url': 'ozon.ru',
                            'status': 'actual',
                            'rating': 2,
                            'gradeTotal': 34059,
                            'regionId': 213,
                            'createdAt': '2000-12-22'
                        },
                        'phone': {
                            'number': '+7 (495) 730 67 67',
                            'sanitizedNumber': '+74957306767',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mb8Vru18OkvgUPN77MW2zoxiFva35EF97s81iE5OEjXoSBWN6O2IiDjPDjaQdSLMHiqjE4CnQIEjAg4XPY64cbGJmu1kgvh9ATrtdtOaTyw4rJMqNTz_pcjl1zJXgY2YeSEZKjULWJA4mHQn9qX2EIN6LJD_DFa0_Sytn7hNSrc1P9zNt6HUmm2jZPURsv-E07zWdJoDXYT_z71LMILHLR6WVxlZg6k09N7sjRMVHzbxTahe_7rIVCYZOg9fxaayeas1fZtcZn16XJW45syhJKR6Y8TJboKo2Bcloii2lTqjL5ZIrzEyPvM46KVdmLcuzQmc3AD0CHw3P5m-zKpp03C7ZXIY6XivuOyV5gLc6AtPX2dudZ3I1z8ENpZwMJEyeTg_0PlInFRQF7TD8MyicCRIQaK5kH-cXSnndeedjXrZRtxm8Ev5B1ZMFN6vZKcItuKn9_pwkHe8lEaajW7uHzTgGwBtXJQHxTrjcjiAExHFDVqsKarCAv65J3ajoQo78vjRUUk0GLz92rUmBePQLabXha9zSHSS0O2Xz5TGnovsQSxGeInCvOwdk-u6xNfnlSMdfnw7nI7EhbLUPf59wlwn6iAvkzNRFvGGq7zFCswcWqlIV6UsS7WjI-Cf1UQTkd7QHkGaidjAkHbBo8IX7YnamOit03Q2E8sq-IrRdqr_ZI6yrps2geRqFIJaf-O8suf30GpGr-iljhKDcYHu5TXdWic1QaCZWBkuJeu9yENA57mWef34R6JaScDpzEwQAu51AZi6_JmQkS_8QHnRTYSMZkGUgM1CZpA5Dkd42EhEbLbe5P329vxymWxn8Sw5O7W3yBXKUSGPip9HOVCIVJA63a_yx9eN3A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8QDR2ZSbC85JGI44Q5OlQLoFczgM5kH1fl-_qDmLPalZcfl1vAz2yEVwFzOzjx0JEZKua02iMxL2JB9auWPtCIQbx56bptlYDPFnIty10acwCXapV4vgeLJKRcfE2aNbAX_29fEwZGpg,,&b64e=1&sign=0ee771cce36ea828125061abbe3ef5e5&keyno=1'
                        },
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 1,
                                    'dayTo': 1,
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 299 руб., возможен самовывоз',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/364506/market_QN4jnkg9i_hmYbXeM0-ExQ/orig',
                                'width': 1200,
                                'height': 1200
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/364506/market_QN4jnkg9i_hmYbXeM0-ExQ/orig',
                            'width': 1200,
                            'height': 1200
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/364506/market_QN4jnkg9i_hmYbXeM0-ExQ/120x160',
                                'width': 160,
                                'height': 160
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '495',
                                'number': '730-6767'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '5',
                                    'workingHoursFrom': '9:00',
                                    'workingHoursTill': '20:30'
                                },
                                {
                                    'workingDaysFrom': '6',
                                    'workingDaysTill': '7',
                                    'workingHoursFrom': '10:00',
                                    'workingHoursTill': '20:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.604592',
                                'latitude': '55.76172'
                            },
                            'pointId': '430165',
                            'pointName': 'Пункт выдачи заказов Ozon.ru  м. Тверская',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Большой Гнездниковский пер',
                            'premiseNumber': '3',
                            'shopId': 155
                        },
                        'warranty': 0,
                        'description': 'Яркий комплект постельного белья \'Letto\' произведен из бязи, плотного плетения. Такое белье прослужит долго и выдержит много стирок. Рекомендуется перед первым использованием постирать, но не пересушивать. Применение кондиционера при стирке сделает такое постельное белье мягче и комфортней. В комплекте: пододеяльник на молнии, простыня и наволочка. Обращаем внимание, что расцветка наволочек может отличаться от представленной на фото.',
                        'outletCount': 1,
                        'vendor': {
                            'id': 10738511,
                            'site': 'http://www.domashniy-textil.ru',
                            'link': 'https://market.yandex.ru/brands/10738511?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Letto'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/2Sk09zhsVb-n2Mg5KhKX7Q?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=weCKmFGAvajBuYxvnt2s8tqiIh9wM6JfmMYubZowO9Y0iAQ6hz3Ewo7FduXZqfxGe-hwgMnUAL-N86w78LKze0f4lZrbozalnLLfB8HvTV9oMyRqV-K1n9Yc2wkX9VZ3&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 10738511
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZFsQJgfR0WrNOZLET3hrhJHB-Sw_IRBzwnNEZgzDKXEMoTsyKcjsrWXpiw_wzPBleSjCXVU9Flcg4Kc8F5sFFBFJFQiJPkY1Eb2Wew5rvg-UEPf2gr0kEnV7vxJm-mjwocRIbF74M3q9XuHc5i8qm-ua9w6b223zMaWXYaLUWRqeqi2nNuGz4XTe-IFlPOOTcfRiEo2mpfxxUQ3TA6nrtwlBl1AFAJ2ZIQNgen2b6-zryzSFWnhoP82slHs7YVbLeEqwkP16BFC50oSRiKLHn8jcUP2cCyb4FI',
                        'wareMd5': 'VXyfQSCeRjMBgxyEC70Iew',
                        'name': 'Комплект детского постельного белья Letto \'Шрифт\', 1,5-спальный, наволочка 50х70',
                        'onStock': 1,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mc2aORABufwe9msAwzf5U4dAG02-R_VLC3JA-qv3u51X9ee_VVWs1S2Ce3om_p2l1Sq9JNeMqcTYYHzPBiGOiHxvDpwi-9j-IJsG1oFcefwtlRQaWgn9ggPJKL-mahkXWxgY6aVH9UwWGJpZGje-j0GBQVr-hLPNJy5wtRZpf6Abpge8LNL_2H9HmAKX00osdq7UQOpVlHbNyz3e_oPVye0FeZXjOxTJxvRO2_SlmOyt4IyVsAS04Vc6OoemsYRXB0E_iseXRkMniJhQ_JH7ksXG15my11BsLHZ7ghG8yBmJ5CQD4j2Iw_0NjDXO8xI57Yy4l2DODDg6B16j5wuMMGv6WRZpH36P2Tu6kPnW0nxxn5dKDzE28vyNUyx9Fmp_xkFBEjYX4FdWJR6lJnvM3ugztF5LZVJXO3ql2M1m7-1u1O-ZTadFwEn4BmVwAaE6VwRqFKmzWDRVpzZnpNltv188FfgXFlicCdwFcF_MDSD1QWWkp2y0QZALEGTn-6LKdDpKv8gwKOKXKnMWsIM3DiRX8luXP_-N7VnVRqHEgii2rszBsE0Ese6SZ9v6fd-fE_rPPn_Mo0BB_1-DnUmzdG5oDhd9f31jCjgLAwnHbHjQ13euYOAPzeBJ9KZc4UfOhqB2eyIIcm47ePaJWm6LEfMC6VxWaZg-D0YYqjnsCUI3C1rm8rkkvw8uwni3rxPCxDCNzQnOlg7aQ6plO4-qI2GmpbSR0kL1Q-DipihI6sf9LA6c-eDqMT-FXcvsoU89iN-G0lFMDvjTKc9Vw7HoeBzk1sPA1Iu0RHlyxPcUimiDJriY74kMyN5V9pZgjpjqLe-xNBxAFhBAu3obPyr0CHhB8lsqoTnwZA,,?data=QVyKqSPyGQwNvdoowNEPjUO6Lp2FicSHwuLrkBRU9MB2L71swKP9DwSZnoHPlJhRZwJIdUCMFHR_LOATmalrf-S0TbEXFxlgaDe8gW9GAfe-YYFzNFsk_jqq1a_mMWNtEmt9J90QV7XxtNsIKpZ7uxstdJguundKAWVLyKmV7M2Z9U3sOHgROJbavd-NWCPu8u2LMxMR1pVGhux9KQqSpISBNqYO3sqclw-YE_aahYRkNjhm24yNHLQ6adpz6rYN5oOk6p7CuFKiU5RQ3sKJP2c1FW6L2jQ7PI58d8kz3TU2Q7p6qwEAEhb8LjLR6pCNdAHbXuYpA3ISuY2v7rcAKg,,&b64e=1&sign=08b84c8419aa3bdda47fdc6464f5087d&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 155,
                            'name': 'OZON.ru',
                            'shopName': 'OZON.ru',
                            'url': 'ozon.ru',
                            'status': 'actual',
                            'rating': 2,
                            'gradeTotal': 34059,
                            'regionId': 213,
                            'createdAt': '2000-12-22'
                        },
                        'phone': {
                            'number': '+7 (495) 730 67 67',
                            'sanitizedNumber': '+74957306767',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mc2aORABufwe9msAwzf5U4dAG02-R_VLC3JA-qv3u51X9ee_VVWs1S2Ce3om_p2l1Sq9JNeMqcTYYHzPBiGOiHxvDpwi-9j-IJsG1oFcefwtlRQaWgn9ggPJKL-mahkXWxgY6aVH9UwWGJpZGje-j0GBQVr-hLPNJy5wtRZpf6Abpge8LNL_2H9Tvq-AGdjIyDXLTU3gKyXLyMeTnrssbIkqhd6mmDaF2VrDUTs4eFfJOh3GceccybPDFjsoomQCyXHQ9NUJvYfYAE6HKgvwhLAanSsxMVto8QtMsw2G95vz8FqMtYyXhg8II3LakKES7CK0tGOgrFzvasvHQvg1WC-bWmql9OnFge1DcdpSCi99DciHbOiC7VsHmbmvGjO5s5veViRwBzK_dA7xylF45cDBp2KxHh-9iuKFqVD381SG5GAzcq46NbLpi25VHWIyFW2fQhtIC5kVZ9sMf4UwCpBgTRiOvyd6ACgJ8lz_57wuHa-vKiyqeF4gbMuBTwVeeBZGjTb4O94GjahHu8aLRLptyW1Qd-a1zTh26vKRlCGuxsU0nFKYpmuOoovRbK1LDaASihUxOaV8elf0ion6K4xmcNdFEDUE4FCxDB2wN6spm5rN0udsbnyXOu_-cgxpb95IboKm80CQ_0ZwnPWOZzPiEWEm_8S5REE3ZWAkjLUr5rD7iqcGvmhmMhOYcAqdv6hKcuqR8WWLN_5Wg1SIo00d42Rro9Tsl0bvifnkU5vXFsS7nTh1M5O7zcD_NXhOtZl0MO3xMOi_-qVO90A-AVSCz1UBBYYaXpcy7xq5RJdkTF2BUz7TaVgB4Bfv5HCPLuL5g_t7cqjux2ZQUIm2hfpShkBWGjr4Jw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O__bqedZT6Pcf1jhbUUSbVEczpDoNdzhZ-Ftv_SKSb5hrva0ZWmVf_KEaRT_jhnb9iwX96c5PfUHLuiNtGGo69CyFcHTkSxLrfai9aDguaHPWuQLj47G_uJ44eqRCs0RLtmfMXnsXB5hQ,,&b64e=1&sign=792194d2c98963fa6ac94c3a14b3361e&keyno=1'
                        },
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 1,
                                    'dayTo': 1,
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 299 руб., возможен самовывоз',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/165151/market_QgKuOP6XmM8V-zinmM5wdw/orig',
                                'width': 1200,
                                'height': 1200
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/165151/market_QgKuOP6XmM8V-zinmM5wdw/orig',
                            'width': 1200,
                            'height': 1200
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/165151/market_QgKuOP6XmM8V-zinmM5wdw/120x160',
                                'width': 160,
                                'height': 160
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '495',
                                'number': '730-6767'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '5',
                                    'workingHoursFrom': '9:00',
                                    'workingHoursTill': '20:30'
                                },
                                {
                                    'workingDaysFrom': '6',
                                    'workingDaysTill': '7',
                                    'workingHoursFrom': '10:00',
                                    'workingHoursTill': '20:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.604592',
                                'latitude': '55.76172'
                            },
                            'pointId': '430165',
                            'pointName': 'Пункт выдачи заказов Ozon.ru  м. Тверская',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Большой Гнездниковский пер',
                            'premiseNumber': '3',
                            'shopId': 155
                        },
                        'warranty': 0,
                        'description': 'Яркий комплект постельного белья в хлопковом исполнении и с хорошими устойчивыми красителями - по очень доступной цене! Эта модель произведена из традиционной российский бязи, плотного плетения. Такое белье прослужит долго и выдержит много стирок. Рекомендуется перед первым использованием постирать, но не пересушивать. Применение кондиционера при стирке сделает такое постельное белье мягче и комфортней. Пододеяльник на молнии.',
                        'outletCount': 1,
                        'vendor': {
                            'id': 10738511,
                            'site': 'http://www.domashniy-textil.ru',
                            'link': 'https://market.yandex.ru/brands/10738511?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Letto'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/VXyfQSCeRjMBgxyEC70Iew?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=U9xqPzGtajJzrlc2toFAA4tQxKExWCjKyzwRnLBXVyOh7ukylHPss4TzGdQ8eCihGFicgVweZrIoXrdo9DeVLlR-SGDhIkYuJqdRG8eC9BEv9I8_cn8Y5ybLuL-zvT_7&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 10738511
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZGLaNRps4NuryFAWCKYMgzDrmCd6BSARD04vZKhrQeC72r--tAOHiQ84KA-Uq2R6KrcOVb_y-RWoeB17yXigwtftf4By7bSjznwOjT-0OWRaUVpNNJZb2xd7PoU3kKY0SsEgU4dLV9nX4Ji80Wn3RluCOxvB4PFtQqgwf7MGga_P_wBt0RR9c4Qge8hzpk6PpJUMRW3GOH5fFtl7hNXEqmCQL7m1aSdkIZQ_r0XgL--lnSVrildvt2Qk7hLx8b45_TZDoQ35VdiFLJOOGLJumLKZP-vHEbEph4',
                        'wareMd5': 'EwDzFfrc6sqFNLsAe-7BOA',
                        'name': 'Комплект детского постельного белья Letto Home Textile \'Пчела\', 1,5-спальный, наволочка 50x70',
                        'onStock': 1,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mb8Vru18OkvgUPN77MW2zoxiFva35EF97s81iE5OEjXoSBWN6O2IiDjPDjaQdSLMHiqjE4CnQIEjAg4XPY64cbGJmu1kgvh9ATrtdtOaTyw4rJMqNTz_pcjl1zJXgY2YeSEZKjULWJA4mHQn9qX2EIN6LJD_DFa0_Sytn7hNSrc1P9zNt6HUmm25wWJpk1KZ5JuZUuQ_FeSO5PkJzdxR3Hir5q8puBysUtLv2LVBLh2LFXeBKw6zfX4AeXLx11-oZ12tNqynMZu8sjLvBl7Gg_fSlmm7RrfRyxJNVuTjD6Ch9POdGF1HXWX5g-ov_1LxZ8ly41G596-iWzTpiXuZvraVFwYQnsXyKLEneAj6Rmajyzadz5mIGwIOu4dGhAOB-JyNrKMB0dBTCY9jTt8602-rkGZBSNjW_YzejQDPSWli3-3TII8fyWaZX0JgVe_j6CgBxoQ1nWfM2fPvR9MpWN1bqpTdsShSvK6MtKixorCpka3x0IXzzUNSLGF19579YW2Rxxy4WHll89ftDRjsxW7GRlaRDy475HP01byML4nR5j7eipiQuHLxq8uLajDbjN_eVTLAeFV8wMhSXhxAoqfihB3r0lkLRXdobhw95EthVx2ta4gJLsDYhtYu4dhstRLx1sUC6FcTl8b9aFWS5Rh5Eltta4yxEdJZBLyCbZRO5KbEr8Tl7iqsvgBXhCz3bqoQVUXYyXHPzZ1f5XWUcD93PWibA7pRAZ_ZqCw2UmoPHZUt0bmANkKhiEJd8nujQIfW9Fu9eGucPjULH9lgagoIVsmCTFvKupckmeKNPk1eQTzi9Jp7PFh4YtppzZtW3E1XMe9BP5xPv3RN4-3xllf7tvW3MSEN5w,,?data=QVyKqSPyGQwNvdoowNEPjUO6Lp2FicSHwuLrkBRU9MB2L71swKP9DwSZnoHPlJhRZwJIdUCMFHSsUeYf2G7V9-tr-m_aahXY9eD0Imqs2K9x2ZsNNFd-GgmPJFJXGeRpoUmWeJIbXXpVUijbjpuOe5GrvbK9x_azv5whFXdNIrnWgC7thCW0ysaxZ6b8ExDbyHarHmBkJk40U8N1p0dbDOXqJeY076k7Tz386lORDAmGI2FIdtiu7g533pyoNpLYew3VeOVO47xAb_Juchzoj785qWdSjDPVyFtNVUeyZTsBWvj00Z8i2ZtVaFyr3EEBFoh3VHupWdz9VYhDwWP6DQ,,&b64e=1&sign=274ce1bd8578fefb0ac16264ca974b9b&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 155,
                            'name': 'OZON.ru',
                            'shopName': 'OZON.ru',
                            'url': 'ozon.ru',
                            'status': 'actual',
                            'rating': 2,
                            'gradeTotal': 34059,
                            'regionId': 213,
                            'createdAt': '2000-12-22'
                        },
                        'phone': {
                            'number': '+7 (495) 730 67 67',
                            'sanitizedNumber': '+74957306767',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mb8Vru18OkvgUPN77MW2zoxiFva35EF97s81iE5OEjXoSBWN6O2IiDjPDjaQdSLMHiqjE4CnQIEjAg4XPY64cbGJmu1kgvh9ATrtdtOaTyw4rJMqNTz_pcjl1zJXgY2YeSEZKjULWJA4mHQn9qX2EIN6LJD_DFa0_Sytn7hNSrc1P9zNt6HUmm2nMu0jXSizKSr1TETAhuoh4cfQtbcgWCRkCRkDYx1T5EBlKeKgn85dvxG1BCmtFamN8qaRRNU60JtLB1I0zS9PLSb9FJRdUyoJ1LNStfIlgYcd3tlafqD4jSoPMDTaejR3mVTMAZqpC-dVJaPN7WZL_gji6OjoQogW6PuByV6HbYTedOMl6IYVtEKvSi6T16NvzpF-OBlQHowuzP3R_h6BmnCQhu73JtW-ZphFhMTntRyEvwcvGRPd4pSLMZ7xqIeWj_JL-trEk0C87wRB2xFUzPx8IecfpE5uVDNL2sVxB8IVBdb-ntARjTevDghdcBuXp7J3We4xwQfeNba2JNWFuD41EeThTCHJ2p0He4OWM3l-MF45sxmtotE9oh0N5RiUyLD6uAzb0kDdqjrUg3sHcgTDOXAVNfs6aRBiQjQWD83N3WdNaEyWmu2ArYZBPKwqNtbG6pAJlyWB1NCz9ybsF7lmTQPvAXLphCGgmt9EA4l9K6xF8f5tV8CQ-ClOx0o8418JZHZyqcDaPNBgy27KjSiky0PeyAAri-EGtQKTc8FrMXg1nmeSPlwo4qQCCay0qR5j8jfzMhJDfc_f898WMHgAAWS80xrJxgwgDEwqmsAW017jjcnydv5g1DK9YuEBS9yrgVey_X4DrQroJ1WGGRLyHyLjAJjF1vM3_kbo7Q,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_Tdd_cy5xK-450ptuOpp_iHT7sFgldwevz3PMu51wmIEtW3g2MgsjxO710BFJ2yfPqvhb9VKbI1qAEPEQGf1zAedcoEvzKkk1y0EKeU8cKStgexMJs3Bre4X3YM66XkanLZp4iKLjh1Q,,&b64e=1&sign=d349cb6411587ed3ccbdce737f3b0ea1&keyno=1'
                        },
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 1,
                                    'dayTo': 1,
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 299 руб., возможен самовывоз',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/331110/market_2hAmY4cBP6u-9hI0DzIOPg/orig',
                                'width': 1200,
                                'height': 1200
                            },
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/173932/market_-l_8xp8XxIz22Cy6d7bVKQ/orig',
                                'width': 800,
                                'height': 1200
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/331110/market_2hAmY4cBP6u-9hI0DzIOPg/orig',
                            'width': 1200,
                            'height': 1200
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/331110/market_2hAmY4cBP6u-9hI0DzIOPg/120x160',
                                'width': 160,
                                'height': 160
                            },
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/173932/market_-l_8xp8XxIz22Cy6d7bVKQ/190x250',
                                'width': 166,
                                'height': 250
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '495',
                                'number': '730-6767'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '5',
                                    'workingHoursFrom': '9:00',
                                    'workingHoursTill': '20:30'
                                },
                                {
                                    'workingDaysFrom': '6',
                                    'workingDaysTill': '7',
                                    'workingHoursFrom': '10:00',
                                    'workingHoursTill': '20:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.604592',
                                'latitude': '55.76172'
                            },
                            'pointId': '430165',
                            'pointName': 'Пункт выдачи заказов Ozon.ru  м. Тверская',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Большой Гнездниковский пер',
                            'premiseNumber': '3',
                            'shopId': 155
                        },
                        'warranty': 0,
                        'description': 'Детский комплект постельного белья Letto Home Textile \'Пчела\' состоит из наволочки, пододеяльника и простыни. Такой комплект идеально подойдет для кроватки вашего малыша и обеспечит ему здоровый сон. Он изготовлен из натурального 100% хлопка, дарящего малышу непревзойденную мягкость. Натуральный материал не раздражает даже самую нежную и чувствительную кожу ребенка, обеспечивая ему наибольший комфорт.',
                        'outletCount': 1,
                        'vendor': {
                            'id': 10738511,
                            'site': 'http://www.domashniy-textil.ru',
                            'link': 'https://market.yandex.ru/brands/10738511?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Letto'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/EwDzFfrc6sqFNLsAe-7BOA?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=weCKmFGAvahCP1s7VUtZyNo97XstRUc-a-0ge4nZ_HVZiIdX6A-T0iuiTnLLFgL3MhG98XQXaE-TnJw40RiuIuOcyNRmTZERFBIdvvPdf99lX91A9s8hNfez5K0dXbV-&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 10738511
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZGzksmCTOi9dXG4o0yVYwTiU8b0DM8QPYewZ16VmRfdqw',
                        'wareMd5': 'wQtRh-lx1RKh2IvKtt2knQ',
                        'name': 'Комплект постельного белья Самойловский текстиль Утро, 1,5 спальный с наволочками 50х70 (714289)',
                        'onStock': 1,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mb8Vru18OkvgGEDA24nV1boULOw2WwEtL_MnB4-dQ2gR4UqG1C38OvU2j6e7mcwH3Vb5BBVxaff6-Fs8e3XBdXb4UI4gJNvQ_YyMeAXF71CdzRmh_p05akEjmN4gxpH_Th4N5D2A-c-9HJS6Hw3s80jJ0vS5Zjbl6PP8xTd7l8h0YMalm0iZsOyZQw7yCpuu-MFDKP2lXhUjG4avDqJ2CzuQAyufdMV2pB3qyO2xBLpXI3irYyZYxG2jOFqIpXQxYGrGwNQIkb76RechRb3Rbc0kXgKC_DbvRP8oJsM3CoupdK19uKAAt_Go8RSyb0MkIu1Mv0Lh3jAjN6fDYssQhpd1Jqv_KRu1i6TEyNb5x3vRojalkL90vNneo8Ps8MgdPG460FRAgc6iapFYeFcejmuJAdYzyP69OYYSMn2WRwlsSsxO-ALrFNkKxc_W7WX35cg8t6jOmd4oM0Lx9p-3RRpqTBrhySu9Z8XqVeJr-1x7qzWojofRbPsvYqL9JpI21qBOFOun5TvnqkOrpGeBvBXIyKyOiny1Pn1SZsA9pWmt6_8ZkpoN39ifrjYh-mErwr68JbQMPG0hBsGFtqF0AwjIoTJOIHbWPPXLY3j9GnmxhNh6MCpdNqKxzvrv1WFmJ9cxissj1xGJBatuB9KDiNU1XLIkW8H6Fxw1SLtZ54aeldRB3Ln-hmmVyNpnDeQXl5HjXoDiNiq9duI6FtK-IbFoWIog4xVkgZLsNU7awa8B9oiEy5tEfTWV4f0bg0P25V7Po4yO-YFMBzS4HaTR8nRu9oXz-q8Ixn732g6hLjYV1PQ_ISZsRwry-hfYlbEzhttL_MbMPOVzNOmj8g-X85sngNd7d52Kpw,,?data=QVyKqSPyGQwNvdoowNEPjeHCAo-a2AEEMgvw6Gv7rodwqTifDSzaDcFSJWWZewOkjKyWEkn-sMIK18K46MxP11czXbgrCXD8DfqH5mvJwvr5-4oss7zBdn_xpCFagzZhTsOCPAD0Xvca8qYUcoAINCS76oXQ_0zfXykLPCMhwMWPRsMYTeg1NPW-SxxTfAKin--aUG72G4cbQ65hdqZoa7RxY8fmIGetsluBCW-XOBq-jpuP2FF3KBTVLogyaaCo5pMYMPItl8AKLXvovs40i2AmgYGzgSrgPTuKVaxoMzbnsmCvW0MCDHuhUPoj6_bGCQskzErFXvbkRXu7627mNWUAaCyhiJpVUPsoSngSqEOdOvtzIP0FgPOq_NpFG5xaDCVxDtLFH4mb7w9q3aeMuSjvc_Ufrx5AtwPgJbDNApZ88skfi0zMGba7FACpiev_h0eMaZ1viYISqUA03g-9X8gDkRnUROLjNrTiLJaid8ynfob5wShVYGiAmEXpAKq9c5sjHPIPu64JmAJrL3GVhla4ZIeq4d49lMOBsH_VmJ8htlY-auoYOjR4mTo_yLHxFyDuXFCYk6MoJSq08kVP5V_VPLeofWQHSZqiNtQlp6AIXJnf9kv5ycBr9bSaYCqnKNW5NUpIuaw4GzvsWcLREzHEcZwSguM4LV_nRua3A-wIDPWGAGiDXR17VOULJQfVqpCa00qGpOUaR5p0fAS1Xyg0Pm9nQ86gkgk233Fp7IgZsYPtAYDLB_OnnYbtfLNcrfBYg4sfwgJzoNJOjslHpOqCsFDSscx0liVBkTJNPesMmONRCoH7eyYiqDzemmk6WuNwBKfqUSRJiYX76weqviDmpiFfTgSlG1qeHp1acqr_-zArv7kmhnRJqidHlNrUq1gZj3YGFo4,&b64e=1&sign=e8f948ee05036e648d88d0ac96563cb9&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 255,
                            'name': 'ОНЛАЙН ТРЕЙД.РУ',
                            'shopName': 'ОНЛАЙН ТРЕЙД.РУ',
                            'url': 'www.onlinetrade.ru',
                            'status': 'actual',
                            'rating': 5,
                            'gradeTotal': 68002,
                            'regionId': 213,
                            'createdAt': '2001-09-07',
                            'returnDeliveryAddress': 'Москва, Ленинградский проспект, дом 80, корпус 17, вход в магазин с Балтийской улицы, ОНЛАЙН ТРЕЙД.РУ на Соколе, 125190'
                        },
                        'phone': {
                            'number': '+7 495 225-95-22',
                            'sanitizedNumber': '+74952259522',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mb8Vru18OkvgGEDA24nV1boULOw2WwEtL_MnB4-dQ2gR4UqG1C38OvU2j6e7mcwH3Vb5BBVxaff6-Fs8e3XBdXb4UI4gJNvQ_YyMeAXF71CdzRmh_p05akEjmN4gxpH_Th4N5D2A-c-9HJS6Hw3s80jJ0vS5Zjbl6PP8xTd7l8h0YMalm0iZsOzVsP2oLVngEZrc6z1rhlZGmGva_MMLVJkjY9aRCM22J7h2MmgsIJLK9ygXlkC2GZasSIOGOjbvpy7cAPnLVPhvRXNbzaeTkleL5ApPYGon2E6mCLcaW8f-y8kuTC33IJexj48RZd109MlrpRrTaUXxi_PyFfsSPNUZVtUGYVrBokCK4trO0dBgCiN3n2U59sdPx1lyT_guLdh1Ag8aqSP-xA8QGhenR456nToUJKlweRDoWyDmRPVwNQvrgqQE5wt6501got12eSFgq7tKVqBrN6vPM_Y_a4RHqOxJ9k8-ImhJPfMaEAzzvRKbqTb_UqBj4N-h2cCIYDqfMO20W-V-WgkPa5qXYMWA4U77wlQYCV6yR5DVT-8szWtAKdDGJ4GroDkgb-7aMY-QhQNXcaN73IotdsZV5GbG1DvnHbx0y7FH2m4JafbUVqxUOa4Ey9RdJ84O95lxJMc0v745aXfskNw3K4zyyMVB_NEZn1zJaEoPhP1pf7PP-her5f94fKmvPxsBX_HN32JjKjepOH6D428mqlancmXJrFXC8v1JdHznIalzpHNLIXK2fwO6UflFcw47JnKzR3xzN5BWs3f_Yp56LAXgP1zhjcu52jtPnTqzRFEbg5Qd7qKxlGNjC9nEhPkPvHp9igVJyVAsVIqhLOGaLMStA-V5HQIoSgK9yA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-yJvgMK1AbW9nSz28S-4_BKxb6_Os1-CF_BWCodgoa5r0MWc3j9rMCJneZJKf6BJs7ESKdAugAG0tAJWP3Yech3u9Ry6YvhHLDYEv9zfv9JnuWhFSGN_s5Hud4GRwulkyHyWwYMyKezg,,&b64e=1&sign=92f018d0b463c1cf0ac5266cae55b8dd&keyno=1'
                        },
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': true,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 1,
                                    'dayTo': 2,
                                    'orderBefore': '17',
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 300 руб., возможен самовывоз',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/231668/market_ehvhK6p8qza92tmjjo2tMA/orig',
                                'width': 800,
                                'height': 680
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/231668/market_ehvhK6p8qza92tmjjo2tMA/orig',
                            'width': 800,
                            'height': 680
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/231668/market_ehvhK6p8qza92tmjjo2tMA/190x250',
                                'width': 190,
                                'height': 161
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '495',
                                'number': '225-9522'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '7',
                                    'workingHoursFrom': '10:00',
                                    'workingHoursTill': '21:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.50502496',
                                'latitude': '55.67525905'
                            },
                            'pointId': '8560154',
                            'pointName': 'ОНЛАЙН ТРЕЙД.РУ (м. Проспект Вернадского)',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Проспект Вернадского',
                            'premiseNumber': '39',
                            'shopId': 255
                        },
                        'warranty': 0,
                        'outletCount': 51,
                        'vendor': {
                            'id': 10718078,
                            'site': 'http://samoylovo-textile.ru',
                            'link': 'https://market.yandex.ru/brands/10718078?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Самойловский текстиль'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/wQtRh-lx1RKh2IvKtt2knQ?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=weCKmFGAvajkWLjGrxpP6p47tetltO6echcaTTMMVinW6l0ltHpBos1p5_lYXJPjtOH8f98nIEXkBRF8sv5MNev8G-a_4bhT273rGLXnWVkpOHFfJyfzT0vA8Tkprt42&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 10718078
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZG39at_NVwMNGMBFsS6hIvCXdjyNs5STx6KniN9SBdDd7W1lbPau77L0dJoTnnR5js3fWZZ-c70m3j_xc0uMDkyePrmrOtkac8AJKEENYaK5ZXrNW_IjXW7uobZ3ChmgIeYHgwyzNlb1yNZhaud1qs4MZpr9T_2XnSJ7MtnaDEOaQGO92jpTZBBYj77AwN1nUYBZFV3MgG46E9OfzichFhxVsT77UjXBqmHwDI4HdssdH9fgqBQ0bmvQ5NqWCANp9rRX4naxXNrm2MSb5X8s9kThOgO1X27N1Q',
                        'wareMd5': 'OGCS15AOxFKb1Q2W4ogNFg',
                        'name': 'Голубой комплект \'Мишки\' 1,5-спальный (наволочка 50х70см), Letto',
                        'onStock': 0,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mc2aORABufweTneUYlmG2F3NFBd4TmsEndROIsD_mX8kLNrWlHuhVLek_hcgTuzaE0SxRRlaJBZbAwRcZHFiN_kZsFuZjcEtLlArChPMzRA29F2dtoaTxBITW2Rf2TdJWKToWAweYXKg7og1QkBcSBvfDqKKBNSwnsEQbO-wfM6RLX6e2I1si0fKSs01IwTIch4062iHK0m9J2FB-6qAPVI8GXSTdfXLTDHzYrqW1vObPkXddmMXV5wL5XF4JipQdnh0CU-fG-n7M2ubXYuI5gmDdqb_OPGADwMitLqX9jWt4vtBXUn3tPY4JirKUVkmSolDJGPKN5gzWyetf7C_2RnLtfEWd8tvH9BsSdvlc9zhoTyp0OuAoE6oA_H562-UIL_YVF2kSMFjisIp1L1Z-4rZApHYgRh6_cfs8-nglf6rD_gQZapCq1u7JO1_x2kL1_ezWBUGO1W0uoZO4Otd95BkK014chOUF0ypRQGsbtXQ2L6brASEosvcYyVNswQQIfez_od0JN34VYgyvx_rr8nMpf5YFGN6FdxMGKpLiBoCHNtfugaa-KAfNRxqY9ihxgcXpr-htk8IzBQRutUc4XQR5sjRQFAWLGLI28EJkQpw1MJ4xIcfrtSZjvpjEg4Vpx_bTL7xeHBB20KLrOz8BHZEBjipHvZazYkLNm8MWuewr8CTw9_-V6_ZmPQr-jF3X0c_uBkukVrKPzYUH_c9V1xDMkfA_5SiJgBJDgZg0_qQsW__WZG7QLw6laV-hlSPSnxi55gsi9ZZaL0nExu6LYjO5YLtc402LIcF0kAaUItMD5OGa4SGLmOi6bBDIaA1Fi65AYTSksIyNTj_Re_tTBZX1sOofawFag,,?data=QVyKqSPyGQwwaFPWqjjgNgCY46WVtBfQeEDH1Tly179UZxrHZ0D9eoIQ2O2xBdJvty7jl3OQnFM6emanWq0sl7Y3agLPO3SV28SO6lsip-Pq6979INl7az-pWFYNIISMFzxzCE8lTtSKgsmyqmImZP31x7RqmYeGeo4OX1RRuMLT3FQEv43N_4QIAdWgKY-wA_kce_Y0VcFfPpmdA0OG_cJuJu_TS0nafn_Lo_pz2T2thdhsoFpyGZlDYbAlB4m8jiZvTJs5uyEca0yX4cwmbvIugZO-oNYu6HtbDTrVwFsxbyqYa8ohSJlu1qbMWexfNip9a2wIbwo7NgSPSjRUPKhIoKqW1TMMXC003E6peeTpOfDnee7FCWTAbn2mHMd7vgFzy8Hj82MTvWc-bccPO7pM7YLTmgKEfMBfQHnD0QI,&b64e=1&sign=449b4082dc715c813afcf12b4503800b&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 51471,
                            'name': 'MyToys.ru',
                            'shopName': 'MyToys.ru',
                            'url': 'www.mytoys.ru',
                            'status': 'actual',
                            'rating': 5,
                            'gradeTotal': 11034,
                            'regionId': 213,
                            'createdAt': '2010-12-02'
                        },
                        'phone': {
                            'number': '8 800 775-24-23',
                            'sanitizedNumber': '88007752423',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mc2aORABufweTneUYlmG2F3NFBd4TmsEndROIsD_mX8kLNrWlHuhVLek_hcgTuzaE0SxRRlaJBZbAwRcZHFiN_kZsFuZjcEtLlArChPMzRA29F2dtoaTxBITW2Rf2TdJWKToWAweYXKg7og1QkBcSBvfDqKKBNSwnsEQbO-wfM6RLX6e2I1si0dJ9wBn3H9TTVcgqeXEk9WkQCPHpJzz2rp3fynSDeEwc8C5iilzpnRAiirFAECuv4YwTLHj0M3dU3H6ixBjEvSGiR_ILABSb_cnIs4d56HON4DIlVmjzsr8XwlIl0oJBPpwfSpFtt4EFnUUaYJgphfwxsFsSZC4YEHfqBwEHDskT5ZUZJQli7rXdwYh3pEWFKm3B4XSAjYlBq9zVSAVj2CTZYcf9SwDif9VJcxDG9CdjrppXclzhj3h0m7BjwaSSobHNbPU2jjYc2omELafR8gyz3xuicT2USDC00in_bnmJjXLYCEjLLZ03PhRagk69oA45wvolGxUqbhKEUmHYfj0EwalHTYqyQ9CnMQd9JeCPDgJzS46K5z49FYv2doLS-u1UEQMlqwpnzuBANdNbkO1YVB9LUD9EhaDA-xvwdN28Ar0enr3WefJt-PWJzUQacuheVLKITLsWn091zHTmHeOOl25PbR8Zc-9E2lQyAAE7GAubjIhDD6E7zRPhB54Gp5sis4ywjxTWPs4TdfBCQThUk-PYBAMTx1E0m1vCG3RTEKojc0Cl_k1ZRJWiDSEdt79QLhponnpnT79tUAMfG1XJk6A4xKK7-Vzela9JJO-k22VqhMpc--sPNMXx07D9IpPEqd_9rv2C9scpXbdXh__ln36_7pKG5QAAWY_wNAs-Q,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8WMrrtVLGQeVxtuf1Ht7gGIPII_IzENyj9-pFIAO2cIPlUeBxc6w6QpanOJ4EH1sgH26Pgo-rrnJm8oD04g-gqZCCzI6Cb_EZvcOM0WZT-WPI5B4_0RFG9TvKCgmE-ptLni3U9V-HLGeESBAz1jZgL&b64e=1&sign=26f359b6125d38bb3ae609efd64e4d66&keyno=1'
                        },
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'methods': [
                                {
                                    'serviceId': 99,
                                    'serviceName': 'Собственная служба доставки'
                                }
                            ],
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 200 руб., возможен самовывоз',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/247356/market_18ehz5v7mpL7-uoXk7UOGg/orig',
                                'width': 600,
                                'height': 600
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/247356/market_18ehz5v7mpL7-uoXk7UOGg/orig',
                            'width': 600,
                            'height': 600
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/247356/market_18ehz5v7mpL7-uoXk7UOGg/120x160',
                                'width': 160,
                                'height': 160
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '800',
                                'number': '775-2423'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '5',
                                    'workingHoursFrom': '8:00',
                                    'workingHoursTill': '20:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.669639',
                                'latitude': '55.739676'
                            },
                            'pointId': '374904',
                            'pointName': 'Boxberry',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Таганская',
                            'premiseNumber': '31/22',
                            'shopId': 51471
                        },
                        'warranty': 1,
                        'description': 'С комплектом постельного белья \'Мишки\', Letto, Ваш ребенок с удовольствием будет укладываться в свою кроватку и видеть чудесные сказочные сны. Комплект выполнен в приятных голубых тонах и украшен изображениями очаровательных плюшевых мишек Тедди. Материал представляет собой качественную плотную бязь, очень комфортную и приятную на ощупь. Ткань отвечает всем экологическим нормам безопасности, дышащая, гипоаллергенная, не нарушает естественные процессы терморегуляции.',
                        'outletCount': 4,
                        'vendor': {
                            'id': 10738511,
                            'site': 'http://www.domashniy-textil.ru',
                            'link': 'https://market.yandex.ru/brands/10738511?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Letto'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/OGCS15AOxFKb1Q2W4ogNFg?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=U9xqPzGtajKhSpOAxogvkfSvFpygpBFKpQ-N2ZNb9uDIQIZOkasvp-eSjKsKkSKXnGUIzZEA0dedLS_ZoCFKAWc_B1wXMKzkf12b6A2PhtCBvDDMPJqf4vXiQ4HdOPBz&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 10738511
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZEvtDJq6_FrUQHKAWEbOIvLAsKlC4lFkH0Xbh9c4_iIjA',
                        'wareMd5': '9A4DMQO2P3vaazcpEV_cug',
                        'name': 'Комплект постельного белья Самойловский текстиль Калейдоскоп, 1,5 спальный с наволочками 50х70 (714089)',
                        'onStock': 1,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mb8Vru18OkvgGEDA24nV1boULOw2WwEtL_MnB4-dQ2gR4UqG1C38OvU2j6e7mcwH3Vb5BBVxaff6-Fs8e3XBdXb4UI4gJNvQ_YyMeAXF71CdzRmh_p05akEjmN4gxpH_Th4N5D2A-c-9HJS6Hw3s80jJ0vS5Zjbl6PP8xTd7l8h0YMalm0iZsOy_M66Dy_tdrLrCzk3JDQamvK2c4IBROBErYBlOF9dgUFObF0Akxldd6N_c9XIlFXRalp_4TrtjIr2YnNbWLO8df-LLgDeZZKT0kLmd0R_8rd93RsIRNrWnPBqUT8OKDIeR26FMtLZyWoRzpnJJNcCrr4m4JGojHBaViFj8ad7z-Do8aCnbfTGTLQhdLqI8y4bybBKBQNfz5k-K_XTaS1H8GbW1zbUdtSrX3oJlyN1IcaWVbKLNzAQRBetpvAwzRF54CO01tZZbwZKpxAvfPgRH0mZtBiBOGI99d8YC7cV05dymrDrbYr_4S-qI1YYZBAGNoyJctwNJ-8rfyidDkHALxoLjtP9b7DeyXdUH-WGAS8mL1cor6cSUUINJIEqeXykgddo1suZgBUxFXtSsxuDYfVxxMXBo5HMF2DUGsDqOB7WatEnFyyNqgTX9SFXdFjTFYqU5g3TTJ-2t7HoQhR0lEqjoN9DqiJRFWXk3r7RU3nM8-w6YwCVcSIjAk2EyWIbuvO9ROQi450Zmb6N_uN59L9Rm20COC46zxQNX-wt-L3jvhCfarwcKouU3rCxQNNV18ZSKSe07LsWfwRaAQOhWFPuLFx3q3bW1Cv56lzTY8-NdcP8QU47eItCvU-TBOXbT_IWMfbYZMIHEmVx6Jw6A1X_FhsYQlEBY7rMZ0PGHxQ,,?data=QVyKqSPyGQwNvdoowNEPjeHCAo-a2AEEMgvw6Gv7rodwqTifDSzaDcFSJWWZewOkjKyWEkn-sMIK18K46MxP11czXbgrCXD8DfqH5mvJwvr5-4oss7zBdn_xpCFagzZhTsOCPAD0Xvca8qYUcoAINCS76oXQ_0zfXykLPCMhwMWPRsMYTeg1NPW-SxxTfAKin--aUG72G4dQFxbKQmAZ5ZL_KSVxjc8O2x1eoUqTgXYyhcV2kFF-7xnWpE9klIeFTCDYLXr99imXvXVWVahdQZZ3NJTEE_awWrnHIw_itHXt9vBtXwP8xHgLhazjBmAjyzu0VzgLQppRV9KHpSCqF5WJWy-exg2rccf00z51MMfgUdfzwKd3JwhkEYu9Ip9T6f5IrsXtw3PrXm4wUpMpessoeDtc6T4TMKboLr1MQ3uQX50-AZtZh5p7MBQFPNOnVWJyNnL-I9xCEb2E2ftbt-QWim0cP3Z8mYkSjlXUxsjk2emSUqtg6PkeBTfZvHSqpCOnUbQlDvHbBaK2z9k9J9EUZYc_xU4tfBu367ItuCrEjCTYiNrwA11-AbMONbjQxuAqMU6uT9OkYecDy0EMPAELJbgllMV3c6pXbeWIxy_empc3KhIr4ePDq--hMGCkHWnqYstL6gFJrr59-aIceOy6uJ0RKHaHtoPYUfirJl9NlNE6KTkhjy0aA6DM0Nl6piaPOgoV36gfc45ENMGg4V8SPh3z-TDs3qFJ_UrUNQ0Xa9DEa1MMvH2EHxghKKlv12CajNezYZ1eTkf3AkbPIcHbjo7uvFx9NNdxlHx_fJw5EgMeWis1uSJ1yunJyq4XtjHTWGjvR2kjWP96AJYVCcTlNzO-UqgBG-PIqyVnX6Th1uykl7bLq5MoknjnRC47PDVMx-HActnaTBUlbeLRYWlq8Xxoja3wAj4si8GZMRs,&b64e=1&sign=9ba51a3bc6b35b663578249795f0f645&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 255,
                            'name': 'ОНЛАЙН ТРЕЙД.РУ',
                            'shopName': 'ОНЛАЙН ТРЕЙД.РУ',
                            'url': 'www.onlinetrade.ru',
                            'status': 'actual',
                            'rating': 5,
                            'gradeTotal': 68002,
                            'regionId': 213,
                            'createdAt': '2001-09-07',
                            'returnDeliveryAddress': 'Москва, Ленинградский проспект, дом 80, корпус 17, вход в магазин с Балтийской улицы, ОНЛАЙН ТРЕЙД.РУ на Соколе, 125190'
                        },
                        'phone': {
                            'number': '+7 495 225-95-22',
                            'sanitizedNumber': '+74952259522',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mb8Vru18OkvgGEDA24nV1boULOw2WwEtL_MnB4-dQ2gR4UqG1C38OvU2j6e7mcwH3Vb5BBVxaff6-Fs8e3XBdXb4UI4gJNvQ_YyMeAXF71CdzRmh_p05akEjmN4gxpH_Th4N5D2A-c-9HJS6Hw3s80jJ0vS5Zjbl6PP8xTd7l8h0YMalm0iZsOzXThOuqPhhQCvMiiGCHoII1YTUTe41JUThLSvsEOZYt0faCIpkuwryiOYLG_Jzwzu6f3Rm7DKQG7-4UxuFr6KtSdWMJ7xJpcZarp-nubkeUxdp9xqsDjWK-6ABMCiLj-YR3yyQm9i44owJQEwFi_E1n4PoQXQ6jzDnZ5Y0SuPMbtq_Ylojce8KZCziZff7R3e5uZa4UUhrvB2mycXnCdNMU8Vsa7FvS12kNNHMGDc5qq7gzEPkNpCoAWbucq5RuAo9F43UtY3KAJVwusIBDsPEhCfrOaeQMHzcc7dDKU8-IpCAjitynyRBt1ydVJhoMDmKXZxtAmunvYZ1MmhD55HrASOC8QlOt2hfr550SR-wbguTy4sd73P0FVngrgKNwiIBTRpf0G09-06M2b8X50c_tXRcDplH7io90jZ-iqXGRiKWAw4pOdsSz_Xhx3xbCFXx8knE4i_JazIQl7ywui5OK5HEiRGwT6PgT8wLPUSLF5adE0t7jKVizs2I373aBSUygvqj7OK1QNa9SY4z9eGXmShNYUvlWeaSPoVLbN1MwZtziP6y085aQiuaxpKgN11_JQOGRGgQ6jz3u3Pi2zkeCZNxijebHB8hLYxRcNO0LtgvM6mT7_tKiIEvhXqIJxcKEAsF73A96wmQ3_PQqCQcs9_1G_DazxGQqgwue5EYBQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8VN2JY_lK3AYItvVHNtvHhh5VqsCde17pJ9642XqkUAiLNXeuzYLXPP3KA2ioof8GMMKUlfimKqOAFiedEEKb87V9WI_7CBT0X9jkvI5XYjCjYwP1jWCHyhmoHKFoS3K1Z7LKez-d0Eg,,&b64e=1&sign=c25acd547bf70c687d79aca116deed1a&keyno=1'
                        },
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': true,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 1,
                                    'dayTo': 2,
                                    'orderBefore': '17',
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 300 руб., возможен самовывоз',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/209514/market_mUMuLjRMH0Ult-tZyDnVow/orig',
                                'width': 800,
                                'height': 680
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/209514/market_mUMuLjRMH0Ult-tZyDnVow/orig',
                            'width': 800,
                            'height': 680
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/209514/market_mUMuLjRMH0Ult-tZyDnVow/190x250',
                                'width': 190,
                                'height': 161
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '495',
                                'number': '225-9522'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '7',
                                    'workingHoursFrom': '10:00',
                                    'workingHoursTill': '21:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.50502496',
                                'latitude': '55.67525905'
                            },
                            'pointId': '8560154',
                            'pointName': 'ОНЛАЙН ТРЕЙД.РУ (м. Проспект Вернадского)',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Проспект Вернадского',
                            'premiseNumber': '39',
                            'shopId': 255
                        },
                        'warranty': 0,
                        'outletCount': 51,
                        'vendor': {
                            'id': 10718078,
                            'site': 'http://samoylovo-textile.ru',
                            'link': 'https://market.yandex.ru/brands/10718078?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Самойловский текстиль'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/9A4DMQO2P3vaazcpEV_cug?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=weCKmFGAvah16w4lTRHj9gvczPFe8RSieCVGnHIupmOCPfJbZsHi63QvYWhRg9WMqzdAlRx6qRsXwpp8O02Z-nEnmUR1cI7pQ_ClmV1KPLO6afjaEIV7JWdiWjuypowJ&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 10718078
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZHG942O9XUxLUp63zfd7hzWXm5mSDjNHmaMhm6UOajSnsAmBQSygguGqQ3jpNFc60w-WNkntUHYhjTByICxANNj7SApjdavAjswfyh4zCrZnXVeEl1Gk_i78tHlJMc1O_lB_on7R_iECyX_x-tDI_4c1bgEkp2gCy5qRoq7PKvpsW_kq_Usf-yqLWFBeGVCA5W6dPUqVsQ4M73rK1jRFFXDmdBgfO6LwNuS10-DbPSkRvGUSqmpW6sXPsjr_StHTT6B4qI69L-pTGwRmHzXCZK2qVBAuj9IaWU',
                        'wareMd5': 'Nhq52RNyWKlKE8rEPbWrpQ',
                        'name': 'Голубой комплект \'Барни\' 1,5-спальный (наволочка 50х70см), Letto',
                        'onStock': 0,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mc2aORABufweTneUYlmG2F3NFBd4TmsEndROIsD_mX8kLNrWlHuhVLek_hcgTuzaE0SxRRlaJBZbAwRcZHFiN_kZsFuZjcEtLlArChPMzRA29F2dtoaTxBITW2Rf2TdJWKToWAweYXKg7og1QkBcSBvfDqKKBNSwnsEQbO-wfM6RLX6e2I1si0cLwOjxHXpnS7A297pOqduo950DgQ6xnOaSAiK60ewU49KHMjkyZHh4VtW99B7R4p77UuEedSppx7ZPR1g6-YZhR5Bt298tiIVPncFRC73AJDQZhlBlAEhRkV4wt3wKy9pOP3jasuKEORQK9sSSYF6ZicQBliLNmr6mO0ijuCv7Dret6UIeCNYS54vxRFKnBYUzBYy16H_VjlsHinxV3_TCcOhIZx6B5uMQJHtKCt2vOzNt42qiyZAKqcRvrqncGpPVIxqCfVRUcuMnjQ7QcKAdYaqml85E05rSwilZCwnnv2VM7OKShBfqA-EwV4jEt6oYwB-uULTJDXi5Om-ZxsR2dR6ZIAEXnZfgRmK-5kNh-BFoXBkYBYuAymXn_K6BLz2dK4D28J1KDj83FTFKGNmS3YC6JmIdw17i2LkAbUjBV38vf-Bl2uZbdutpdx4S9CMJYTD3zXizD_91zcyL3usucmBy-6SDY4eriUNvZbcdAgLA9uvtX_RjVW3GpFJ57yKtHArS9r_-MpoQ7K39S1-HGCEYxMvaXglyVTDYoM00ELDk-pynQCZKj52p_rNkHywtIZT6QKWzjJoNJo7S83r_-Dc1AHT9OpSGP2fIZmUqAaKnwx_xHUF_VPxPg1q2HH9EoxBQw6qQOaY1DViA7y1vxbBoY-VdFD5AcWc7t3yefQ,,?data=QVyKqSPyGQwwaFPWqjjgNgCY46WVtBfQeEDH1Tly179UZxrHZ0D9eoIQ2O2xBdJvA7Giukjw-TuF2nsanMgt17Mhg1pI9bE8KtjV_82tOkfERwQfEumOqUxjfRTdVabKA8fqVzII3_eI0d7Ho_qUVtL0JP0o9rCt1YSxV2BgNxPgltyCvAcz_beC6axJibHOmFgMWVZOypDh44DPSM3QKdL6RzdJXQu5K20ngJGezKbcilWBan8MCmDN8ZEE4eT_5snnQCIH9gC-wkC-Tx9GQYoRHRwkgZN_Hnh49C6mkYaNIkRSPRLD_xf0qwDbjn7NzeaxyJabSycNCNYytxrDIHtAEnYacgUjNpm1IZXkXRD162F263NU8SRnproJk1UglJyj80ApKWs6o9JIY4gYz2-exeZw0emJSUczWxxwEVY,&b64e=1&sign=1fe1872763e958a499cec7c1a5f81784&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 51471,
                            'name': 'MyToys.ru',
                            'shopName': 'MyToys.ru',
                            'url': 'www.mytoys.ru',
                            'status': 'actual',
                            'rating': 5,
                            'gradeTotal': 11034,
                            'regionId': 213,
                            'createdAt': '2010-12-02'
                        },
                        'phone': {
                            'number': '8 800 775-24-23',
                            'sanitizedNumber': '88007752423',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mc2aORABufweTneUYlmG2F3NFBd4TmsEndROIsD_mX8kLNrWlHuhVLek_hcgTuzaE0SxRRlaJBZbAwRcZHFiN_kZsFuZjcEtLlArChPMzRA29F2dtoaTxBITW2Rf2TdJWKToWAweYXKg7og1QkBcSBvfDqKKBNSwnsEQbO-wfM6RLX6e2I1si0c3SzbM5DL7hXorHdZiY-vwZcLhgemN3SVY6TOhoGcgBe-Rgickvx74DEfZgZ39aJOIZlhklX3xWITGf-lPFYxSS-11sUojmQBiZhzSf6c0OnAG3cNmQ15KO9LhhbNzBWm0ZZrUw0tjfJhRV8zEGsJta2K7cXaNWE3PTlqTdsaWfRPMdtmjWrgYCUdu2xAak5kA9sBekvrrBffENoDaMYSb-h98M8i-SUXaJQmlYgsPl9YysmGAl6061EsOjUxyhuGoSW-GF4BUr0u7scZWtkonWxGoHLYgzG0U8KRoSu4g5q-hY09fup73Dd3d6cU_l4NTxIoX6gLN6pGfpJWEhCBNe0dfmOcaZiDahMkTZVP7ebO5NqoTzCq3v0eUPc6BtKQhdRMD_Yl20Caetibxmngv-VffHNUMHW8VrZxvKcnjy3OwqrRLdAvT2D5aGEighPaJSYrh9II3YQ5blFLO33WqlSxMjRgyngqybhXRKD9qWcWP8WInN2WAEkLOU9prEf7kbS1BCWgFRfhzOQO58vYCbNYXd21K35QGKH_3ex1iYIwGZAJzKdEsAl3JdIIrd_3_9MkAUHY0E8JS5Iqw6YG-spFz7hI5YBh7om875oBdjxYWj9HUhoSi6METJoWp5VL2G44h-loGyGUtIRT9KeiEq7xLM-FTt9E0UBlYdhF2HA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-BiM_3_uSDFU_Tj_Xjudw-Kr3p1ocTsljsdfchZb14F41TwN6tLJrTWD0cSka0tPMTlW5GMFdh1tGm1SP5DAZUQ6SVi5XaKMyQ_QjKazCDxA_TsccHyGKP08FLQ4MHeRMNnPJRWnzw1J_z8dedZRX4&b64e=1&sign=a6641235a1fdc573eb785504dc1be675&keyno=1'
                        },
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'methods': [
                                {
                                    'serviceId': 99,
                                    'serviceName': 'Собственная служба доставки'
                                }
                            ],
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 200 руб., возможен самовывоз',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/233556/market_tOofNEbH4Y3wPah3yFFZnA/orig',
                                'width': 600,
                                'height': 600
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/233556/market_tOofNEbH4Y3wPah3yFFZnA/orig',
                            'width': 600,
                            'height': 600
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/233556/market_tOofNEbH4Y3wPah3yFFZnA/120x160',
                                'width': 160,
                                'height': 160
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '800',
                                'number': '775-2423'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '5',
                                    'workingHoursFrom': '8:00',
                                    'workingHoursTill': '20:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.669639',
                                'latitude': '55.739676'
                            },
                            'pointId': '374904',
                            'pointName': 'Boxberry',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Таганская',
                            'premiseNumber': '31/22',
                            'shopId': 51471
                        },
                        'warranty': 1,
                        'description': 'С комплектом постельного белья \'Барни\', Letto, Ваш ребенок с удовольствием будет укладываться в свою кроватку и видеть чудесные сказочные сны. Комплект выполнен в приятных голубых тонах и украшен изображениями очаровательных плюшевых мишек Барни. Материал представляет собой качественную плотную бязь, очень комфортную и приятную на ощупь. Ткань отвечает всем экологическим нормам безопасности, дышащая, гипоаллергенная, не нарушает естественные процессы терморегуляции.',
                        'outletCount': 4,
                        'vendor': {
                            'id': 10738511,
                            'site': 'http://www.domashniy-textil.ru',
                            'link': 'https://market.yandex.ru/brands/10738511?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Letto'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/Nhq52RNyWKlKE8rEPbWrpQ?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=U9xqPzGtajJQX2mvBOYE6JbWOUB_EWNBmrEK5bg75TVfxsI7JrqIwrdlWTKma25qDYl2Z6iCvtg8uhOV3gh5OCntcEpct_nyqoCPx8jgOeR4Nvs54ZCaq_f2rFitJt-i&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 10738511
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZEH7siuJLb8rBpEque5QybHuUSaj9jczj9FngM-PbZ46g',
                        'wareMd5': 'aNxjbO0a8N7AIpTB0X7BGQ',
                        'name': 'Комплект постельного белья Sova-&-Javoronok Индира 1,5 спальное 50-70, 100% хлопок',
                        'onStock': 1,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZ4H9_aAm-SdP69geEvrHAW-blXpEpbP7mMvSiqE0wHXNcbjFOAauooLfzyCp20WqsuZAHmxkCoODEykLWmE9JXtRLbu3OrhL3snBTAHOvO6ZoeviqvkNa-cDYVyzqD23EoH7epndmBWJVVh1Y301EBerTx4cn-cVlW8lrsl29RxOYFP-YOcJojdMoC-6Kj_lQOjoVWFHNIgmEjMpxLWMKOeR6MQUNTm28gdIyE9UASspDVYInSaHr4R3_F28Qv0R8HS-hNXCdBHS6U_Uoi-PIDMmgIYqipPb3n7gRkCu1ZcdwVuWvB7z8d7L8d17p-PSVO4XI4COU0PAj-Uyg5fl0Cj7xBJdaraEIN8qJDh7xUcXxdJQBihGOPMcUVwnGDXY1Co0dWs8Tb1p1rWtZ0I9pbeeFZMUgFXKUrhwZgtOdm9RfJmWNClPssGd_mBkbnjBflTyii0RPNc1AVFvKPcCh6YRJn46IrZfZGklTJuZ2dNm0CtxRBgAhYDRfTL5rC_T3NSLaj5WInqU20_2UuTYhzbU6cYDlzA49jkICPzY7SfT2_KFoRi9NrrSFAuVNsWBilTswVHylRmn_AwkDJ6LoDKH2NaHQoK_30T61e1V1qFW6rXyxLOJJuihf0BVaW85KI9H0BDN5Ijei98otp3wlv8bmO9-cR1K28Mb4Z0csCTP6uBWGr4camLtXwZC8Re6rMciKjYp6LZ09vmWH5WwqFzT4-1z7EbaOULKkq52X9vxmVfDQF57GQhc4iT9kLJJWk29XogQRM_Clnu9NDJCc8qOnmZMPuVhGpRfuDYAP3YCAKBdJ-huJUzCf6oWeKnpPUDDFvkViyfkLf1hgGNNSw1twI96ixkEA,,?data=QVyKqSPyGQwwaFPWqjjgNtwFDITzDmMAdbMKRCiaGZLbhcbaDUDzUfO_avUV0GI5uOzzdfO3LfDaxZNrbVUogd0bAfYlBLzQTF7y8Encoxxqe68tYB_uAGsjD-I4Hxa9zbfiAZuuEWyDiU9EUIjB1Ufoi_h1nU7YwU67P4wGUBYYyuMbzfkb4w5M39nu0nQGZYJNi7f3dLkJpHrYtUXJppWZV_HGEzxkvz0rkzYWqmFxdIcQp3Ix3TXZPWr7IFIhA0sc-xidnVdWv7UOZxnwLQjtF1wFhaCbwZWIhUG-9JscmBepIJs-SNSMxt81qP4ORvJYW2FrqMbF077wqewTDK-a28SnDXyEYKk8bdaOzivhHePyZyGxQXTLcX0bl9sTG0x1ZYLAGBI7rCDLVq4zIt2cIvJcjAYc&b64e=1&sign=73fc6e31b0c8d08a854cffda7626455c&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 25017,
                            'name': 'CompYou',
                            'shopName': 'CompYou',
                            'url': 'compyou.ru',
                            'status': 'actual',
                            'rating': 5,
                            'gradeTotal': 4577,
                            'regionId': 213,
                            'createdAt': '2009-07-17',
                            'returnDeliveryAddress': 'Москва, ул. Трубная, дом 25, строение 2, Прием заявлений только в личном присутствии с паспортом, вход с обратной стороны здания, 127051'
                        },
                        'phone': {},
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 1,
                                    'dayTo': 2,
                                    'orderBefore': '20',
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 350 руб., возможен самовывоз',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/247921/market_YDpmP-hCaxz1Fhis9JzrPQ/orig',
                                'width': 500,
                                'height': 375
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/247921/market_YDpmP-hCaxz1Fhis9JzrPQ/orig',
                            'width': 500,
                            'height': 375
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/247921/market_YDpmP-hCaxz1Fhis9JzrPQ/190x250',
                                'width': 190,
                                'height': 142
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '495',
                                'number': '565-3485'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '7',
                                    'workingHoursFrom': '10:00',
                                    'workingHoursTill': '21:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.6235673',
                                'latitude': '55.7715595'
                            },
                            'pointId': '321317',
                            'pointName': 'Пункт выдачи CompYou.ru на м. Трубная / Цветной бульвар',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Трубная',
                            'premiseNumber': '25',
                            'shopId': 25017,
                            'building': '2'
                        },
                        'warranty': 1,
                        'description': 'Тип: Комплект постельного белья; Классификация комплекта: 1,5-спальный; Состав комплекта: 1 пододеяльник, 1 простыня, 2 наволочки; Состав ткани: 100% хлопок; Количество предметов в комплекте: 4 шт; Простыня: 1 шт; Размер(ы): 145x220 см; Наволочка: 2 шт.; Размер(ы): 50x70 см; Размер(ы): 143 х 215 см',
                        'outletCount': 1,
                        'vendor': {
                            'id': 10738845,
                            'link': 'https://market.yandex.ru/brands/10738845?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Sova & Javoronok'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/aNxjbO0a8N7AIpTB0X7BGQ?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=U9xqPzGtajJAeRP6f-FEOpscj-A0NJHh2BKSKl7QINSecE8m3tMmkBJYNF9h8e2LfygY5LCHLm5suiKNF0z4yvNV3Zt2dTdzYL8IB5b9woFgIddlS1PThQPrFnAN7vQB&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 10738845
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZHDBDIeuKdQueRT44BmabSQ_bGehY5AL_WafkSGrc75wDCKDFu9424tN4qQK6KBl_vkckQlm_9R2LJZatirfgIbmymXBLZPLj99MyPD5UIMFftiUvpde_aeY7ltXQzmtI8OKK9PBNEH0iJWQmt813u4CCWKSpom1g18g8gYtuCW_9kkuE3qfQaFjh8FNg6Upe-LAqgdastUWmeexYLSO6qXTHPE8-y-i-5-0ng2L-gmtEnp4swiZyH2khs4jCImoFfCiz3pUOe8xt4kGTrB4Wr-12YUyJz1aKM',
                        'wareMd5': 'gmGHk8FIQeUGgdBQeet9Ag',
                        'name': 'Комплект постельного белья Disney «Олаф Зима», 1,5-спальное, наволочка 70х70 см',
                        'onStock': 0,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYo5ilgsL5ZPbZMzv0UrYO1OanMxAMdawAofxaF8a-PJnyAZaL8hivm4jugFOOJ0U1PgTPIbc_Xa_jI_7F3cu09TF0RbH-_j20WTlLg2ObpeDPqMiTTiLpu9YtdrOVHYAHsupmcNY7-rhr5L-DHNhHTWERcCG1jnTuvwZYVXApRX0yJz0aPHK_2qRhmOO6TZwl1xWTYc0u_Zb3LDHH7pfrF-tctRniSP8LPALHz9RAnFbLTwISXgqtkiFRETHIu03234aHIIYWu3OzfEhIoJMzcGALHLtcgCZPBpQ3CFOCjgzFIrVLfe4EFK6hGFoYqJgdTneU_9bZp6-r-7XxwDf_gRLcbrNetyr1jMpGf7mvcCPc6XTjd39tJFHFGuB7h1y1uWSreENpHcCqsbJ112c1kdrBJddzzG4gnqyNG7WAxO3TdsNV-ktt-5JfTt3toG7gpJyg3WlDh-cK-kuSXdU-GXyyuyttvxgo6TWq2iMF-Q2Z5DvZgP4m7FznsiELeBQGzY-pIQslwIrR_MrWWN0wwZIESRQCwHngFPEcW09htH-6CVVAzcBuIAJQEGgnplFMELHawBviMjzUG8MYzkESoH0BUA8p9weju2zk5Qf591Cw6oHHN8EunKE7Kr88HbhnFm1CnoiP-bxkpy5keZXn9uuJgiSWm9CZJApJnYraB_uvJaTkdj3ZIOwBRwy0Xz05LGTl2LB2QBlfuskNYlj_KtqSQFuAeE7_Ux6ST4D16acZel9aio3JLULiiQ5hKmI9HeFXrgJsFLe9ZSNz9XGl-Jj0FZXVtm_FRtXK_iAH_bSfF4NHWTZg6a2eapCkg81dWevQFs1XZYustwQUNR-T4,?data=QVyKqSPyGQwwaFPWqjjgNjRJCxuqRWIErIjtKT9XSwIhEfnzMLUa-nr81VHmG6VifoOyjRJGMeqB2N0qmQU9N4ls4c7k7jNo94hH2Nsl7q7E2CPz7-kYqFg2cZKVFiI18dlhvlzTyUSIRVU_URBuuHfIVM8bLsgroS16DW4q9EkULTaUvtWQPvVz9n9AbPoA-JIVeYIBESIaQl7a33ekKqBqKHN1jBYHI0ZTvos3aePb43yCxP32_4IXGJiDHShQQm4UHUfEeN1ftMWjtLmvZkan2Kd4QiP9-Gmp6Ry8ejVTrZRzRU_7cHFjf3JCLbhzp4R4E3BFY66KS6jkdqpkQxVLfaEradh1MVf2ayX8R3oglAYgsRB4LkTu7EowV4Fwycf-p3BycSYlapRozgwazA,,&b64e=1&sign=aea79c2c5434aa13b4aa4ab3f8abd0c9&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 175488,
                            'name': 'Ашан',
                            'shopName': 'Ашан',
                            'url': 'www.auchan.ru',
                            'status': 'actual',
                            'rating': 1,
                            'gradeTotal': 173,
                            'regionId': 213,
                            'createdAt': '2013-09-24'
                        },
                        'phone': {
                            'number': '8 800 700-58-00',
                            'sanitizedNumber': '88007005800',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYo5ilgsL5ZPbZMzv0UrYO1OanMxAMdawAofxaF8a-PJnyAZaL8hivm4jugFOOJ0U1PgTPIbc_Xa_jI_7F3cu09TF0RbH-_j20WTlLg2ObpeDPqMiTTiLpu9YtdrOVHYAHsupmcNY7-rhr5L-DHNhHTWERcCG1jnTuvwZYVXApRX0yJz0aPHK_0Ghryomjq-gucOIvEQO2lSv2RtcTcfrKMS2Ne3CCxYjv01BxECs6RGRDijVqrhuzNS_ASjcQg72TBib6l0sPFGjpeZSqzwFEhSQ3XPPyK2T6pkeQyiBbc0vfLtDDlLhogGonMUXovka_qBaTKjVPqLbyyzOND3o5qCxgH3rJQsRpHQFqji4iBuCZQ8PIFsRjzeXf_OATNCpThiiIPM_7y7cM5RnBoszKgxxPwYiiky4hxl3QTijv7TXsPHpD42zy5CefWARxSBWpaOlYHR-_ahE5A1FdeQnl4qOClofyFrqLR8O9qdMng5Dr7ExX8MftAFWp_Gpfq3dJOIsI81qJuyx1VEOt5WXRFNHp6I76ESoyUJU9OcAHhSVMqClFJufUhe7lXAzcZ6h13ZaS1CkY7bezMHcJGuHp6N-VxwbzqrVVcgBAs66hlD7ee-iRoHtbAJv4Cov6XtFwmCJTor7svviHiGdpuhV2KusxIMiu0JK30Wy5OLHEDT2vZo3YpqvfTGWXTZN90nWXoJwJjN-ckKkMqGT0-lDA1mKD_kM_hDacgtG90hfuRwngAnQknGiehMN9htSaDrSA6lLN3uPNVOTJbc3hlHs0jNdgkxrEE5HTShxmaiEL8kqL8_IYowZ0dXOrPo2uBETIz1hVu7QeYSMHOR79IB6dQ,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-tT4VxbE_9ZgVPseTBs304zsgCpXsCt388rGutza9AhQnvc75z17hFQXQaH6og_4E75TeFIp8tPaT9D52gLRKQRB_OPhi3MD6bNugTsMBE_C7at7YpqmuohUIHGDqyTyiCaVh70vXwmzs0vujMswXO&b64e=1&sign=7e72b234e9857f0d825207469f7bbee9&keyno=1'
                        },
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 3,
                                    'dayTo': 3,
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 249 руб., возможен самовывоз',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/165839/market_AQNKRF5cVU3lF1LPwzlfNQ/orig',
                                'width': 1345,
                                'height': 1200
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/165839/market_AQNKRF5cVU3lF1LPwzlfNQ/orig',
                            'width': 1345,
                            'height': 1200
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/165839/market_AQNKRF5cVU3lF1LPwzlfNQ/190x250',
                                'width': 190,
                                'height': 169
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '800',
                                'number': '700-5800'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '7',
                                    'workingHoursFrom': '8:30',
                                    'workingHoursTill': '23:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.592096',
                                'latitude': '55.706883'
                            },
                            'pointId': '411207',
                            'pointName': 'Ашан ( ТРЦ \'Гагаринский\')',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Вавилова',
                            'premiseNumber': '3',
                            'shopId': 175488
                        },
                        'warranty': 0,
                        'description': 'Спальный комплект Disney «Олаф» — детское лицензионное постельное бельё из натуральных природных материалов. Забавный снеговичок Олаф, герой мультфильма «Холодное сердце», непременно порадует ребенка и привнесет в детскую комнату новые яркие краски. Детское постельное белье выполнено из 100% хлопковой ткани ранфорс — бязи нового поколения с плотным плетением и гладкой, шелковистой поверхностью. Постельное бельё «Олаф» обладает высокими гигиеническими свойствами, а также отличается долговечностью.',
                        'outletCount': 18,
                        'vendor': {
                            'id': 0,
                            'link': 'https://market.yandex.ru/brands/0?pp=1002&clid=2210590&distr_type=4'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/gmGHk8FIQeUGgdBQeet9Ag?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=U9xqPzGtajIyROjbL9_EejrlFL-kk4KyFT_LKSQOf63jqBSb0tRHcM_naxioB0sWzsRpbSXFgAGvFDh8ABKTI2_0kgZRFNDi9679EyJgXCxnPfo5JuuyiKHD65vnKTKP&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 0
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZHxOi1qDGohRJrC4a9wBkqgB1HSFh0ZsrfznwO1pCPYTIAFJjxzKYt0_pS62ducRwOehhVHZPFdJPaQ18fE3fVe40MJok7emoOC-_hThahxdhyHH2GpGxVushdz4TcS4a1ODtGY2z25OPR_OPg6ulL7IctNeQx0NbhzJkwF4byq9a0s3HP7dVG_T7xP99W2mZCttwf_NwtlttEvF9Wb3IOSPLszxF3iAl7RTQ5lxCs5TeTUfHQAm9r37exG6IMvA8VdqBkklv_Vv1Gr-QIZ9dWOJO0SQ8XxbgM',
                        'wareMd5': 'Y0DePPsnzgwICcbL4G4Kbw',
                        'name': 'Комплект постельного белья Byblos (Валенсия) (1,5 спальный, наволочки 50*70)',
                        'onStock': 1,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRudsi_rLy0uw6nn3a8ae2SOV30JztW9HaKOr5BPx6fcm5YTH1xYfzeGrz1a2PVHyqBmks-AAfK9sawJ4vqLed4GPaFKgT2S43ZJV8yMTrOlVUlz-nxqTUpXLaB3NLwpvaYPUh7zfsSwknyhd0azQl0l9k4v5_iyjUm5Q88s-ElPw0ruMcb-HeqSkBoaFNQ-JGXylmpClEvm7OSnlb6ewtDUX5780IP49NWAzNxltvqlC6zaXFC46MjIE4VCFXtq6OY1_kdun1b1Afp9lL4PMW75fqUQYIwMbJ1yYWoSNSMo4PwWuyl0cxPnfiNSFr_REhI_R8LJZMYmuOFnJuoek4digqRha5yShWgqi3ULYK47efgqZjqcG0AxNWuXklwU6d_wdXXfZ64PgnzpzZVBw-4nXkT6CQE-ixV7eIzZaj28xkPhp6QB-d2JrleyF-KDvDkTJDvfLe2cUxgx88stqK1nsT68cfzyeAWn8yiLKTNbT26i4tp6UrlFaVYCm1QGKJoebSw106g9hzjsAbJygIIg5O4iX0b3LRHBbkquco92h7PraDa8Mr1mX4RTBpDBLhxats18d6oquHfyzKmCLke57_wlOtkVG_Al1WEny3gFtkyji7jk_4iPq_50x1dRyJHUfWQ8MR7cP27ZbSnbfRkfcQJxyJtnajUWph0zwONSIjSymTmQ857tH-UP-oV7-D-IvQPZ2n35WUCAE1PKd2SzLGANobvjzKyAvdJwaQiWJCgdivilcO-OkYlapAuPSw5DbsQGypeUe1MPSJvG_wx0pMU3xywI3fTMU28-YVQYP3_7kKvF_4HxyR6CJ1mFPyplmilcR4o_SJ4IMnEYpjKamlCZAeF7BA,,?data=QVyKqSPyGQwwaFPWqjjgNgKDqdEy18IjRD5AWDiSZP-BAJj1aOtD45lTxevVUJOBE6eAGvVfZGfuqavfWAJQPbF5j6S3hT6PgtIFWvIvPCTu0wI8BfdHlLjLswvF6uh77HWK3qtzSSXqvCEtcZmA6u3n0Eig6OajeOIcy-RIx8gQpijcD2_ukZFsN5Ea51t7UafcnppI2sJGerdKJPCtKctivlX0rCbx08BCX3lC7OxGgtfq6eghjDlze_fgo2_wU1bRHIR3hoa08tKf4VnqIztGS1oCGC-V1Opx_ZRgWkNYJyubb7S8K56gG8-rCIpa2IyAOVmRcNS17Rs6ihxdhiDXf4jgYtb2&b64e=1&sign=befded139b02754be9f64dd1a6cc80ae&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 305787,
                            'name': 'www.gite-line.ru',
                            'shopName': 'www.gite-line.ru',
                            'url': 'gite-line.ru',
                            'status': 'actual',
                            'rating': 4,
                            'gradeTotal': 0,
                            'regionId': 213,
                            'createdAt': '2015-08-19'
                        },
                        'phone': {
                            'number': '+7 977 162-80-60',
                            'sanitizedNumber': '+79771628060',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRudsi_rLy0uw6nn3a8ae2SOV30JztW9HaKOr5BPx6fcm5YTH1xYfzeGrz1a2PVHyqBmks-AAfK9sawJ4vqLed4GPaFKgT2S43ZJV8yMTrOlVUlz-nxqTUpXLaB3NLwpvaYPUh7zfsSwknyhd0azQl0l9k4v5_iyjUm5Q88s-ElPw0ruMcb-HepVs7y9H7qtLIhzZjbAWWvkIHQtpg-0nLiK-9vdoWzmiNqmq08P806_Of3MMRy9nCiQYbGXvZzZF6n9vphHeh6Ss-9l4Q6xoFgU4woQHUu2jJZlZaNcFigy8FfSZHHZm9ZrbxwSZ_Ji97nF09H-F0jX1kn8wDs9GFFlfCkuxp0Uslg_cqaA2DcnV_JYuetNOlMKVbttwL4e9ouTBz_xpJhYik3xoznjIeCKqWorVwfOhka1f3AA7VU_G2JvpxlPBQGrP3toQc_PphntFZ_HO3YOtQ7NoM2qe_lF6Gm8GB-G64x1w837huN4ezXxhYE4bNPQD8fYrpNbZ08ZtzAWyU4EzSpPx13Cm14w70HQ-_MnaxJyZD7IONtRToWBttc2EzNdUUs-MBThDT7Im5rWWNOk68nHhdubP7vRKWEkckdJqZ1-by8nXIed7-Ss5woE8QFPRJdj5zltwUH1viMxD_LyLzMIos8YNk-eDCe9wCl9BEciOzacn4uT_mhMonEnBb7dIiLn5PyTkJlJiLZqew3btF4f2CK_osik6DtE2CEYsfsfoDAuvf_cQ39rQ7d9riOn77XbZf_34EfP4Si6XU9UdxQEGN8X7LzoUePF_IONgV0p9LuqsYcbCpUYD5rd_1TT2sL5u3YYBqiLfR18_aKdF5Q5t5ShOKMFTojwFkhOIw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8LAacy0MqmYFHbo2k6Dgn7EhUW4ae4WQfXGZmLDcEzsak_YLV8f9Q5-I5UPlk456rbqrD9m515qO5XqdmJY9jud4IRMEtq4CESSKDDEE41q_V_8vXFsE7UCV-AtF6-0UOs_FnorNkYtlypB1IDJ5GC&b64e=1&sign=9d42b6321d479ac04850f6cb2f520aaf&keyno=1'
                        },
                        'delivery': {
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': false,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': false,
                            'brief': 'не производится (самовывоз)',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/168221/market_w_tx5KYZ9K878Yx0aK2A-w/orig',
                                'width': 704,
                                'height': 662
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/168221/market_w_tx5KYZ9K878Yx0aK2A-w/orig',
                            'width': 704,
                            'height': 662
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/168221/market_w_tx5KYZ9K878Yx0aK2A-w/190x250',
                                'width': 190,
                                'height': 178
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '977',
                                'number': '162-8060'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '5',
                                    'workingHoursFrom': '10:00',
                                    'workingHoursTill': '20:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.648438',
                                'latitude': '55.852612'
                            },
                            'pointId': '14229624',
                            'pointName': 'Снежная',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Снежная',
                            'premiseNumber': '20',
                            'shopId': 305787
                        },
                        'warranty': 0,
                        'description': 'Комплект постельного белья Byblos из коллекции Валенсия, выполнен из ткани перкаль (100% хлопок).Производитель COTTON DREAMS Россия.В Комплект входят:Полуторка: пододеяльник 150*215, простыня 160*215, наволочки 2шт.Двушка: пододеяльник 175*215, простыня 220*240, наволочки 2шт.Евро: пододеяльник 200*220, простыня 220*240, наволочки 2шт.Дуэт: пододеяльник 150*215 2шт., простыня 220*240, наволочки 2шт.Уход: бережная стирка.',
                        'outletCount': 1,
                        'vendor': {
                            'id': 15321205,
                            'link': 'https://market.yandex.ru/brands/15321205?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Byblos'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/Y0DePPsnzgwICcbL4G4Kbw?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=EDtYJ40NqptRiDOFtBGEYVQXVt68LjcKaaBIOKHYJNU72EuwlMEjl0CdCnw6IOux_SjOC8i9q-Z9RJGkT_-jTpS5VplW2kP4_WZYw3SQnWR8CcdCvcsB0J6YVOfDvh3y&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 15321205
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZFLrEAoEEkG8J0iCY23mqGN6YhEDrB7OhZxLEXnOUkSpm3O_OY_pXxnsRlvewOG-4JZilj8xbuxeNeOSMyydFCu6rB5RRNnSMaktAFSoqRcbldvNoLvI5ko0z_jQoSN1LX2s9XMgNCquPuKCkLyBNED0V6enQ9e9YhhuiN0W6uPanpCljYCCB6F0dcZLs9MsHDjYurs2B5zOe-E7A_94S-RixvZoBAO8RayRrP3rFb_HYZRtJ3V_vzIR3W3GDO1riAvoW3W9r7MqUpQN3bY1H6w11lohgu7D2c',
                        'wareMd5': 'Sh84Vxh1iPTgAUMGvuJeRg',
                        'name': 'Комплект постельного белья Fillippi (Валенсия) (1,5 спальный, наволочки 50*70)',
                        'onStock': 1,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRudsi_rLy0uw6nn3a8ae2SOV30JztW9HaKOr5BPx6fcm5YTH1xYfzeGrz1a2PVHyqBmks-AAfK9sawJ4vqLed4GPaFKgT2S43ZJV8yMTrOlVUlz-nxqTUpXLaB3NLwpvaYPUh7zfsSwknyhd0azQl0l9k4v5_iyjUm5Q88s-ElPw0ruMcb-HeqfOg4M5N5jicrH7uQ0GfEhI5_FNIdUcwwAYbDJnJsISBZbbBlPSpS5GdcLAU1Fp7Iq5uUkdBxv9udnI-dda_m9Wjori30DvyT24PAh4iTlL2Nm7sWzbiVQsumtQLmW_ku1IPpk6c-n7TqXKszZn5y_mbvATIWjkm57xjqIeiAyMemJJOg60he-rxfuwVDD9-PepzDEVz0VTrZHGC1o6ZnqMmW8x84HGDg6UxP4hWutlxmdV2S3uxY-GyXjz1Ua270EwNKkFyGqQUAcK-056w4pAuajUG8x7iqymSaYuNZSWLCk0pSUg3syd7oEpoRJXqXzcth2AlS4B9fuqvIpr4ytOIuyUNXK_0hX1Gn57r32ZWlCXkr-6JRgAH6NbUTfpPkx0wTDpwbL4EXg0LgUhy8W2c1iIlooaZYZOEPd_6kXxnQ3GQ2xZlYUxqnqVfv_9z3Z4DCzY1dDjq2gGKHRpAKUhaHLg2G8FL6ZMh6znxjUmTc0tzqjJGR63AoHQSSPPqFL055ITS-o2rqhZ6UvCiV05YCmDhdhJzm4H_uC37aildjArZdy342jrGl8cmmTycWS4PKFwg1wyghhpz6gk3VRCw7XmWAUOdBjPylLh7AR5WGtWxIDylSDQ99wnaYTINjgjy2OEforTGAsLg97HOsPxazcVPie8mA,?data=QVyKqSPyGQwwaFPWqjjgNgKDqdEy18IjRD5AWDiSZP-BAJj1aOtD45lTxevVUJOBE6eAGvVfZGfYHLf9KQTjMQkKEsNf4LJcNNeCFeDzg0EJGDx56es0WZWRGzbJY5_p4Z61iflryf-EHIGnSKMF089SdwsodgFAwcO5Cu_g0Wz_1lBr4y23J3dBvd23_3PfluIchvFshVx06VR4aT3UDi_5ZaB3GwY-oUxb5tCortO3XmVtzURGrLhmnPOWl9TVVk3WJSdElbMUOLd0YV7fDU_kIH7Nr5eazVi8o3tgwn3gbT9quGp-DKQNHdQa_oG-cMig6FhG-QeS_h3Z89GtQg0PFCmUiLF1&b64e=1&sign=27c454bacc1425c7032e169664358364&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 305787,
                            'name': 'www.gite-line.ru',
                            'shopName': 'www.gite-line.ru',
                            'url': 'gite-line.ru',
                            'status': 'actual',
                            'rating': 4,
                            'gradeTotal': 0,
                            'regionId': 213,
                            'createdAt': '2015-08-19'
                        },
                        'phone': {
                            'number': '+7 977 162-80-60',
                            'sanitizedNumber': '+79771628060',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRudsi_rLy0uw6nn3a8ae2SOV30JztW9HaKOr5BPx6fcm5YTH1xYfzeGrz1a2PVHyqBmks-AAfK9sawJ4vqLed4GPaFKgT2S43ZJV8yMTrOlVUlz-nxqTUpXLaB3NLwpvaYPUh7zfsSwknyhd0azQl0l9k4v5_iyjUm5Q88s-ElPw0ruMcb-HerjueoNU2u7_Hb4xkHq8fV5Nn89hRxYWf_Pq_Y0Kw3nPdOONQTTjleP3FQ0MStZ596C-3wpfQppfCYJlFnOHxrBmBotbv8Ogp9ztQYdZg18njPBfe3TmnRlcyP2BtsohVJEf-cySmU528Ld7V3kQQ2c9nLt0R3a_cTl0XB38fZoRmzo69KwiYrZ6zCMOdamEHhYGTy0mvjZDqvCI39XxSds8cyebCl84ZMsc-ciMNgr_eoxAkGvLmGUT1maMSmc5toFEJOMefnvB6QkvBM2JXfSrR7jmitmJUQWFoLk8-fgqvqC-BVffiDqiLMLkYkmMdXi2I312QK2Ny3SV7YNbNfEqbEICsx9aJ5wZtHum7VcvfJI6TyCUC34boOY5_kmIPIQBHjL6ju3zjmwCgPlNx4x_kiZTNv-1y8dgXHN0uCpLy2pffpN4LcEwi2JCFhso4Y1XIHf6zmIGPUcKMHyKI1CJS222EZ6phcRW-xiRBj7P_NqNbej7vVTEnyROPS5nmMNk_Xn3A7RpUYqCbTL1cWa1BbDDXJj6sdma2JMpXQT9yvk1W-Bi88qQCzPBa868p9TuIKPfbRj4YPHeew_lpaDuw3yl8u7MpnShhC0e9x1hnUFjvhxiV9nkE47p6YmHz9jxJjZv2MBLEAO0J2qc9cIO30pqIuvrMc,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8Fine2kFy84XyziRscwqn6nQY93LBsv2QwcMZZd9HgVQvEBoHd6Hy0wwGZ-P0CJWwWCJ3PyEtUz5NqrtNbAmVPUGMnMDRPjR_wVyet2QBa9R9FA1Ay2riRKPd0e41zLjzSQoQCdC2f4ZrOg04ZaQQ8&b64e=1&sign=3f6ec521a8234c98264d81dea2ddac48&keyno=1'
                        },
                        'delivery': {
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': false,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': false,
                            'brief': 'не производится (самовывоз)',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/175127/market_ALmuLanrJq1fuf2HDl0MVg/orig',
                                'width': 707,
                                'height': 666
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/175127/market_ALmuLanrJq1fuf2HDl0MVg/orig',
                            'width': 707,
                            'height': 666
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/175127/market_ALmuLanrJq1fuf2HDl0MVg/190x250',
                                'width': 190,
                                'height': 178
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '977',
                                'number': '162-8060'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '5',
                                    'workingHoursFrom': '10:00',
                                    'workingHoursTill': '20:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.648438',
                                'latitude': '55.852612'
                            },
                            'pointId': '14229624',
                            'pointName': 'Снежная',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Снежная',
                            'premiseNumber': '20',
                            'shopId': 305787
                        },
                        'warranty': 0,
                        'description': 'Комплект постельного белья Fillippi из коллекции Валенсия, выполнен из ткани перкаль (100% хлопок).Производитель COTTON DREAMS Россия.В Комплект входят:Полуторка: пододеяльник 150*215, простыня 160*215, наволочки 2шт.Двушка: пододеяльник 175*215, простыня 220*240, наволочки 2шт.Евро: пододеяльник 200*220, простыня 220*240, наволочки 2шт.Дуэт: пододеяльник 150*215 2шт., простыня 220*240, наволочки 2шт.Уход: бережная стирка.',
                        'outletCount': 1,
                        'vendor': {
                            'id': 0,
                            'link': 'https://market.yandex.ru/brands/0?pp=1002&clid=2210590&distr_type=4'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/Sh84Vxh1iPTgAUMGvuJeRg?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=EDtYJ40Nqpuo40FPERHYhHz4T_AcNDmeyg43DLM9EgGE6sySNSBZuBrVunHlLNDvvYl964CWxfxHRDKM6R3xpVAn1Q-zXlMHYBdvihIY0QtSwh6AB65xiQQUEwqVQtLr&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 0
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZGVfbCe9d0RB4F_Cs-nzOW2dbtxrdvdmmykOSc0N9N79qi3HBJxiup3rzUbt1yDBLHjhVmoWf_4WMZNx9MH_cE4NjulfkAGr5LVvMfsUshxn_YVTYnlvHRNHgXIBkGqYR640bEq35T1MWchOuRu7xNtDa4WtbLzkzqotkQMDlMWco2jDXua6j3gSl50rVI45FoHG_6Q0RGNk2QbbSY-MtB8D6EEKiAG3qPw6OeZV_TJRscX6dVNCa7mwSoRuWorniHJpVvhWwCsXBRLoJerwoaOhDKaGYjvxLI',
                        'wareMd5': 'ZQB4738K_agrCBZhpda1eg',
                        'name': 'Комплект постельного белья Ginza (Валенсия) (1,5 спальный, наволочки 50*70)',
                        'onStock': 1,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRudsi_rLy0uw6nn3a8ae2SOV30JztW9HaKOr5BPx6fcm5YTH1xYfzeGrz1a2PVHyqBmks-AAfK9sawJ4vqLed4GPaFKgT2S43ZJV8yMTrOlVUlz-nxqTUpXLaB3NLwpvaYPUh7zfsSwknyhd0azQl0l9k4v5_iyjUm5Q88s-ElPw0ruMcb-HeoqFrqH51FpdETdIIDSHdrDrR5XoUbLVclCqWJFT906eIXoHki1AWuoAzqkWulpJS4qEj0Yeyuv7rdqHyXMM6X5hJF1H2VLqEVmpS1RV71rA80E8Nc-HhLtkOzYhyh7vNwM1056sJ4AHfL-HkLHPkgR_b8dv5npGaspre6MypFGJdYFKfXcC5LZferDRyaeTdrQnzQAKNeJc94rqNiqJGnHzFfKgendu6X7u7E2HyxEUUBv1-IBTDivwCDisrp9UjzdeR8WvxzVV7WYvfVnS5pqkZfvnl8JYnD9YpOdVnv6oPo0SOuB29eNx4_HjR4AqrOg7_DD1jSAQP5N0uYJxWpZlRQq3JFFC-DIw2OVc0oEAommU3uAQpxdxwhcGU2lV1CRSyF7AKW6r-KFL652svwqbsvOZO_chImabjiP1cbatLIvGX7F03YLtjdYsfWRoB92lsEdGjGv9mcCRguw90AA0m2Kx78FSHEYxkrrVPZmNZL4AUP0_tKFTTKXios-gn_ako_c6MKlafUJ4oorTInM2xnPT36RqWbwp9bpVxr8n9ikJmBkVPXBhEziKcFpAAkD8jKTvUx3OaEvtL2VyB4QJvfUkL0gOcLS4DKn7jzFxOxpVZqDNRtoUsIBlh-0xgCodPTbw5BEqq6aPFHOEcVSRUibix8niVo,?data=QVyKqSPyGQwwaFPWqjjgNgKDqdEy18IjRD5AWDiSZP-BAJj1aOtD45lTxevVUJOBE6eAGvVfZGfJGwG_xFyJs3mAk-ktEcscF31Tw1hE7pN3KermGCJvORl_--Nel7XRFhc8U8hdwivUkReAEv3m2ypU2q4hlns5U8jM3WDFzHM_5NORN_2TDJ26OQbFR4qYciJPq4JpWoqsJZpUQ7kxkO2k0eYP1FHfXGLLgvXzL4V3NuQqGoLqRvCEQp8eVJC9zV9Mffp0WKxN4hziuDeoMFjFRNPT94DxccCCnCgt2Fdyii78Mfn5VihxytoCRn2bR7vipbrk5gFlKJbNBUMguQ,,&b64e=1&sign=1ef0901744b3bd0aa30e4d385f1779b7&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 305787,
                            'name': 'www.gite-line.ru',
                            'shopName': 'www.gite-line.ru',
                            'url': 'gite-line.ru',
                            'status': 'actual',
                            'rating': 4,
                            'gradeTotal': 0,
                            'regionId': 213,
                            'createdAt': '2015-08-19'
                        },
                        'phone': {
                            'number': '+7 977 162-80-60',
                            'sanitizedNumber': '+79771628060',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRudsi_rLy0uw6nn3a8ae2SOV30JztW9HaKOr5BPx6fcm5YTH1xYfzeGrz1a2PVHyqBmks-AAfK9sawJ4vqLed4GPaFKgT2S43ZJV8yMTrOlVUlz-nxqTUpXLaB3NLwpvaYPUh7zfsSwknyhd0azQl0l9k4v5_iyjUm5Q88s-ElPw0ruMcb-Hep1P-D42hvkL-P9OucJS7e1LO6yPrYYhG-ktnPW0jktHRh2CCQ-w-9HrEtaCLOCAnNpwDSWgvWPy_Yx7KBL_qNUHM9EFxsbAtTxKvFuS-kld7b_Gfi7HAA8Y_h2s9mGjM1pHSBNTOzQj2AAA8M9aaoBaBnG3tLlDsVro8AKUO0BA0UFITdgDq6r4_vzqFIUyEJTqVxvtTBRu9nZjNJHTxuYbWIpZajUEcv3tOQncri9EIu1Kx2rJlrqiYYIvxjBRE4Hz4_ecTQF1CfReB_aqg1qskPsnZ0jd8NEukC0ChTZXbeJBWp0kPU6QMobLO3EYkXkj60zs9eudNDfCyISjpZ7ncKPVq3J-L4CBkr74Ef7gMvl2ZcxAXdtHyp0irKgJBz-HCyP1oR7grAf1tIkIrtB5TzPmlUCN1q8fMPmyVc6KxmvXVuzRVfA1DCxlhlQOeHr6b6SpQV6mg4hSVPne2RDGLwyi_ava2FWcYJakb5OPKXdfgsxfMXRL856DaMPvkxVFluJGjWJoHwVXOQw80eUMWX9ET7e6K7Zxpr8LSmqSa0vSCel8JcjsYeL5wvvuN5KJ0D4Gq6Z2m2lJxavoRVp5D_ITvht4e2ItIhtTnMV3bfJP7i39gH8g1X3Oqe5uFvAIOSWxrup-ZM_NWuiYc7w7m9H_4CYe7w,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-xScO1lB0J419SWiTKpxqBaDTBjoRZpd_AabY30iCtSrDrv8bbOndR6tBBLQKfXNpllhVVWCTvhOefMXFmGKuyVHzwrRpOizBh3LKQ8EBrVTbP0CefNmZg4G-fNV7XTaFT2JLxVj1Wb5KU_h6ikmKn&b64e=1&sign=c8a54ee70c5d6ae19ab67ff4d4015241&keyno=1'
                        },
                        'delivery': {
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': false,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': false,
                            'brief': 'не производится (самовывоз)',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/175127/market_c0aIQDzrTcNlEJewLyXAFg/orig',
                                'width': 706,
                                'height': 679
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/175127/market_c0aIQDzrTcNlEJewLyXAFg/orig',
                            'width': 706,
                            'height': 679
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/175127/market_c0aIQDzrTcNlEJewLyXAFg/190x250',
                                'width': 190,
                                'height': 182
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '977',
                                'number': '162-8060'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '5',
                                    'workingHoursFrom': '10:00',
                                    'workingHoursTill': '20:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.648438',
                                'latitude': '55.852612'
                            },
                            'pointId': '14229624',
                            'pointName': 'Снежная',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Снежная',
                            'premiseNumber': '20',
                            'shopId': 305787
                        },
                        'warranty': 0,
                        'description': 'Комплект постельного белья Ginza из коллекции Валенсия, выполнен из ткани перкаль (100% хлопок).Производитель COTTON DREAMS Россия.В Комплект входят:Полуторка: пододеяльник 150*215, простыня 160*215, наволочки 2шт.Двушка: пододеяльник 175*215, простыня 220*240, наволочки 2шт.Евро: пододеяльник 200*220, простыня 220*240, наволочки 2шт.Дуэт: пододеяльник 150*215 2шт., простыня 220*240, наволочки 2шт.Уход: бережная стирка.',
                        'outletCount': 1,
                        'vendor': {
                            'id': 0,
                            'link': 'https://market.yandex.ru/brands/0?pp=1002&clid=2210590&distr_type=4'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/ZQB4738K_agrCBZhpda1eg?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=EDtYJ40NqpuQLQp372oqu65Enq7Ol_YStjl7Lqrkw6QJaFSXMvFdcvDnZgx8R2mrhubptMN8ctKkEc9Y4ohdL5M4huzOsemE0OF_2rtjH9McyJiZF3EYuefFZP8OLt4s&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 0
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZH0hYGybx-3iYjQazaCegUvd7Nr0-CyVjlv7tPqiIfAEIOQdVlemSMNltv79aKI7WDnv1_QtmG9RY2Jf2iVs3l746mnwTm8-kMxKk6ku6x6Wsk7Y93YDJGb3ImItoAIrF0BRxCf9pvscd9svQmHhiNtTHCezTgKcrl6uu3be2z4-oY0IGISGyV9Mfi04gtvJbqBeURSR5Undl9D3wCuZrcjKDmyBQ-8diSayy7YbNhCwNvclOndMH7tqwf-V434pMaOdK9v5Q7l-xmdr2lWYPpUET-pC0X8ZcA',
                        'wareMd5': 'Xkq8lqWPoMbVk_kCq1hACA',
                        'name': 'Комплект постельного белья Rossini (Валенсия) (1,5 спальный, наволочки 50*70)',
                        'onStock': 1,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRudsi_rLy0uw6nn3a8ae2SOV30JztW9HaKOr5BPx6fcm5YTH1xYfzeGrz1a2PVHyqBmks-AAfK9sawJ4vqLed4GPaFKgT2S43ZJV8yMTrOlVUlz-nxqTUpXLaB3NLwpvaYPUh7zfsSwknyhd0azQl0l9k4v5_iyjUm5Q88s-ElPw0ruMcb-HeqVfrW-9EIhrpRMeHwKN9AZRueavkc7rGYKm6Ay42911o0zn-WGCSte_-65AFdGZGKqAGQk6mFYDpZ70OKf4y4Ngxoh2IkclmGApiTFmITD8hFo7YbyLxqL2_f5-grUv7YUeK4nfc7mYMadCMSq7D7gHLDYYrORTfcyMnDJzZStIrgt9nWtBMJxvJ4eXVl_sudsy2_9Kdgctq-7yG6BzMYIyMAqCNuGrEYGnxTwAEyjhI3cieldML8SK1qnGScQynfB_dqJlGgEzVX1fcbs1XKRqb9vBFFIy35FJ49s-UAgbtDPnQ4WQUkyOHdijMoe2nMSnC2a8YBy3_1FrJX_1E-b-U9a1miqgg5RIAekj38zNWtFfEyoRjiUQSgsAM5hDR4GXP1mcfblUt-MiZ6WRcpKFzw_e09wgAVjMXFO7IaD1-ZMOxTJlG4JYXr399SxF7tQN9YRa5yEE1CX45EzdJSFB3iSMEKmEXsFh0wWzGTnUZWKnJcKC2mNJ4qP1Tty7DgA_Pcc7JKco8LlkNL-pLpzGjmpPn-hXAgSTK1_2NCngWOyKPEFiHE8GYEfG3ef_bTBsA1kk8aYvscOJgTdjdeMRXG_woXg4qw2Ag6At9wsZH1TmQ83-TFIKwiIAgfr-iaF83BxkmSiYiBpkCMvhWuLYx7vKll-mrAtdRfP07bKhw,,?data=QVyKqSPyGQwwaFPWqjjgNgKDqdEy18IjRD5AWDiSZP-BAJj1aOtD45lTxevVUJOBE6eAGvVfZGd3Rno2BR5f65uU_GReJVREyIDGzKZZ3B9jX6oP-ZX5zLhpLvmLRfFzz-VgUEMPpngSnhjcxy7Pb4x6nqn14KP66JmfDpi1skb-VkLBx6nQ5WfoTb3yD9wj0_iwctcSh2zd99TmUnspgpmPagsJOpNIcygkW18lQ2gl8S3buZmGYrAtYZo7MG_iyhL0hyyzKMnsePHZtPmwQlngF2zqkUofHaJ0ItE9GtvyEPkY0tYQVLLrE9qyNuc-0Kgnfu30_kqBCcEMkQJTdqxxqomT5RNFLLrWfF15RajKJfV_VgpzOw,,&b64e=1&sign=b09b4c4410566f9b5a893a50f13c7c1e&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 305787,
                            'name': 'www.gite-line.ru',
                            'shopName': 'www.gite-line.ru',
                            'url': 'gite-line.ru',
                            'status': 'actual',
                            'rating': 4,
                            'gradeTotal': 0,
                            'regionId': 213,
                            'createdAt': '2015-08-19'
                        },
                        'phone': {
                            'number': '+7 977 162-80-60',
                            'sanitizedNumber': '+79771628060',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRudsi_rLy0uw6nn3a8ae2SOV30JztW9HaKOr5BPx6fcm5YTH1xYfzeGrz1a2PVHyqBmks-AAfK9sawJ4vqLed4GPaFKgT2S43ZJV8yMTrOlVUlz-nxqTUpXLaB3NLwpvaYPUh7zfsSwknyhd0azQl0l9k4v5_iyjUm5Q88s-ElPw0ruMcb-Hep4rIBIjTYscVlxeofi-me0sTVaewMNwQzpCrjcjpGrGqQWiraJVPL6KILU-BYpRD6fQDZr9DRw1_05UtZg_qcBHMI2sJcoYC36jN7mKCZng0yoPb2kt1YdgK6j_23WPpALSZr8MgvflGmT9c49rRvc8XoNnLfxL573cJu223rCtjpTeegPFnJnpKpWZnYLzd8GpNy0FLP-r-HnJxS_h60_Qau8ADydZypkTN7xn44orownBq_n_edKzcnG-4zAM0ZL1xwVB8lv7o4ZYQZ08n4oqKfDTelvsiihAuBWndkiuEaxRB2VIbx6u8nVW7KKfJKYtno05zBQr6nHebLCOK0pCm3DBqyyZH2JDxt6tCgKZ9mrD0U8VkquUWvmkiVHldXdeduKTZfAhZ_5SLeYD7pS_Ch_Dnsw7Fa8Ymev6bfCUuJiDw4S_pBYrDOvKFsKNeo2k91JC4G2w_zg6yeHU3Vpub6SD1C_i8L0D47qQXfx9ZkpelMRSHaTh3tGOyUhooi19uJ02GxES_plj6WjZNSmGAd1OImNr_TdjkA9nGk8Y-2DRpiCWHnchFnzYLxfGozS8he10aVSnuhlwlX_EsVhg7u47ojDNxT_l6IkJ56A9FZX09zl6OGvJ2eiqDYVaYgKW0w_67qm24g4hVNwWnsAG9fdQp1DhyksiX5pEtOogg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9Y7YX3Yd1ww7rUSDJmhmJWXp1hmSsKi9itk8gE3PAc7i2PcHPW9yzwYIw78BbXKGbih-mDZ_YMOg0FuH0o2uGUMBwP2XL78uUGaV0VXeXpCbBFx3lv1enxVFHq8LbBle4BqRsW9uQ69KNm8Bjs-kv3&b64e=1&sign=0a140d18e3536456c84a06d0d646c8d0&keyno=1'
                        },
                        'delivery': {
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': false,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': false,
                            'brief': 'не производится (самовывоз)',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/331110/market_c8_pwvTcbKmOq-jVWWAvOw/orig',
                                'width': 709,
                                'height': 671
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/331110/market_c8_pwvTcbKmOq-jVWWAvOw/orig',
                            'width': 709,
                            'height': 671
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/331110/market_c8_pwvTcbKmOq-jVWWAvOw/190x250',
                                'width': 190,
                                'height': 179
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '977',
                                'number': '162-8060'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '5',
                                    'workingHoursFrom': '10:00',
                                    'workingHoursTill': '20:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.648438',
                                'latitude': '55.852612'
                            },
                            'pointId': '14229624',
                            'pointName': 'Снежная',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Снежная',
                            'premiseNumber': '20',
                            'shopId': 305787
                        },
                        'warranty': 0,
                        'description': 'Комплект постельного белья Rossini из коллекции Валенсия, выполнен из ткани перкаль (100% хлопок).Производитель COTTON DREAMS Россия.В Комплект входят:Полуторка: пододеяльник 150*215, простыня 160*215, наволочки 2шт.Двушка: пододеяльник 175*215, простыня 220*240, наволочки 2шт.Евро: пододеяльник 200*220, простыня 220*240, наволочки 2шт.Дуэт: пододеяльник 150*215 2шт., простыня 220*240, наволочки 2шт.Уход: бережная стирка.',
                        'outletCount': 1,
                        'vendor': {
                            'id': 0,
                            'link': 'https://market.yandex.ru/brands/0?pp=1002&clid=2210590&distr_type=4'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/Xkq8lqWPoMbVk_kCq1hACA?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=EDtYJ40NqpsP0ydkOiD5dHxSzqmwjx8M7K8Rg-OkHaTDQHbs1KD8ewVX1tkRN2nV_ZakmvTsxeDSn8HGzTHngjHEQjs70XP09SM0h3mmpbjKprHhdfqLaqaz5A6Ny9OZ&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 0
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZEcBXC-D6bfMhUmntg6FCx848F29QMOM0RA-HTZYjIQ4bqOyP6wyIvcOR-pDkVAkXelCQ0viXncBc_BQXkydEBvB6HvDhzes8v1GuM7vvX0Kv9gXqpX0UlxBxgpRArfQi3DnF6JkufUiozPeKcZN0GLI1MVrcpkzQzg__q6qqUrH3AN2YyJHbur4QD089eN6tpgNcrZ3fJ8Nvr3bM0hBNJvqjOs2lXXTpt9r5V-23SlWRUzqgpO6xs4E4jqjGDcrfgz9pgOcTmVMBI4ulkZoIbdB-PFiCKFpE0',
                        'wareMd5': '2dEpGNJg8DlhESucoFOV4g',
                        'name': 'Комплект постельного белья 1,5-спальный \'самойловский текстиль. утро\', с наволочками 50х70 см',
                        'onStock': 0,
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 415176,
                            'name': 'Xlplaza.ru',
                            'shopName': 'Xlplaza.ru',
                            'url': 'xlplaza.ru',
                            'status': 'actual',
                            'rating': 3,
                            'gradeTotal': 88,
                            'regionId': 213,
                            'createdAt': '2017-04-22',
                            'returnDeliveryAddress': 'Москва, Большая Семеновская, дом 40, корпус 1, 107023'
                        },
                        'phone': {
                            'number': '+7(495)7402172',
                            'sanitizedNumber': '+74957402172'
                        },
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': false,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'methods': [
                                {
                                    'serviceId': 99,
                                    'serviceName': 'Собственная служба доставки'
                                }
                            ],
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 1,
                                    'dayTo': 5,
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 600 руб.',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/208477/market_bwmdmd8ZyHkJlH4_kplkBw/orig',
                                'width': 200,
                                'height': 170
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/208477/market_bwmdmd8ZyHkJlH4_kplkBw/orig',
                            'width': 200,
                            'height': 170
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/208477/market_bwmdmd8ZyHkJlH4_kplkBw/190x250',
                                'width': 190,
                                'height': 161
                            }
                        ],
                        'warranty': 1,
                        'description': 'Комплект постельного белья 1,5-спальный \'самойловский текстиль. утро\', с наволочками 50х70 см Постельное белье \'Самойловский текстиль\' – отличный подарок себе и близким. Качественное, удобное и красивое постельное белье подарит Вам неподдельный комфорт во время сна и отдыха. Поможет изменить интерьер спальни без особых финансовых вложений и затрат. Материал: бязь (100% хлопок). Комплектация: 1 простынь (145х220 см), 1 пододеяльник (145х215 см), 2 наволочки (50х70 см).',
                        'outletCount': 0,
                        'vendor': {
                            'id': 10718078,
                            'site': 'http://samoylovo-textile.ru',
                            'link': 'https://market.yandex.ru/brands/10718078?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Самойловский текстиль'
                        },
                        'categoryId': 12894020,
                        'cpa': 1,
                        'link': 'https://market.yandex.ru/offer/2dEpGNJg8DlhESucoFOV4g?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=XPYJt-MlmuvX4JdNlBZi_iGD9gnfmRweI9LzWbLsK_uT5w_6n72V_OnlOh9a_0zUi7BxvZ0X2El7G-epJ0XxiKkAhgrzjNDEevgDaOaZ-pjyLv64JGzqH37Tfz7Y20jgfZGTI74EA-g%2C&lr=213',
                        'cartLink': 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97BC4mr88tmrA3S4sJYzmw4gGWEoRvniq2vI0jSJsS3DwquLjNEIkxszBr-91PFymy3h9iv34ZtRPm3uMH1so5auCv6klnJTY9QCBV8umh42_USXV5FcveSk9uCUfaJ6LOKKFdwZFFelObIBzR4yBgs4YM_70jsa0_K9iQm1oZkmaQ8ephx0rFO2UDb_FDi_dBvZ2puW9TLky1CqYiHM72q5U4W7X0YILh22K5czFP9qwy1BWtnGxHhgqEsJ86SWuqtlTKQnF5QA4uKdzZ9_9NNzR6jw3pNIUXFTtIG0Fdidt74adqEGEivTYkoR-UNWzk9NbGZvEjR220o0AYxwJF54rjZaRFMOrSn5VCUOTJd6d1dnOlljf-mj_ZS-Ty0phFswNGVCBjH3xCN_bL2qkxLDbJy-DvTMO131ntK6oA6HEHR7fNaKnAfq8kZP0B1np8joa7G2RsNNxw2eBb8O41E5aK6SgQD8HJh5DEY0OLOw18NcumtFw2uljNRwO4ynZTj0aYX_yNhZbeZK4LUWtsfph4KmBe4nhZ30NhNf_Fhk0Z18hHCnsiEPMZ5XeJ_ungmEt-AmCu9sqagoJZGL0qx33VrwO-J-OZbDGgUgi_EuhDugsB7EtkNx_m7Ya3i3TVfj1uZXQLiOVk0tALtifU8ilswd1Hfpc-uWY2Jyx6XZ_0VKYSYya_ASaIfb21efq23Hr8arCobpm?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXzjqrdVr_OgOKNi9F8doNw3JiPfc45SMPPHi2u2N2lO3q_vmIkRsf5L1B1mbtcPajltRdPuKjqqEUma4tbrkgyEl3k-jFw0J299Ou1ovy2jJoeAd-7Kg_2i_hFRLC0ynX6ORM3yxfU7e4C2ciPg3HofT2Ewb6Dl5oiQjfw2fiFHfr6OWwtkGm1J5XX8kQiDdpy7S-Mpdy71Vua7sq405SbdoqdYmvGKgH7PqPig3gT_naYlJk70I3s9fZ1LWPXoGw65r_ofkQK6WIZw6qCLrSOLYWIQlFkLUjAtq_c2pAUDDIM4BeZv62ew,,&b64e=1&sign=4c85012c0623293cc1612a01d4959502&keyno=1',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 10718078
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZFWMia7BiQe0AEw-o9c7YApueI43r0oBnQZVUKAmPv-_Q8wl6qT5mlsQTkwC40DLgSiYXd_uQsNgeJo3ppazPu9Zb8FH6pX27v1sT0h4isGIICBzPAeSrEdpbkBf1ExUkG_70m_eP7lQuWy0cTfMBFFYe-1MsdTmHPwiG5WjsXzJ4g6oKzMuV-noFHZHVvly6E0a8bvyIfEFld5Luznq5MTUTm07vLHRjJVdGFxp4XEnhg8IGLl0A140l-b3WXc_WJ6_DgR3qCFWVwYOQI2JHGUyl5IJlLwDuE',
                        'wareMd5': 'QJ-HYoQcr_1VV__u5to6UQ',
                        'name': 'Комплект постельного белья Lou, 1.5-спальное, сатин, наволочки 50 х 70 (2шт)',
                        'onStock': 0,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdzEzHTpi0PjSPa5iYUiqMKWKnnrKONqiUBMR8DUd8wvjfX_KNlyXmoC-QzvdZUWKUsKwrazLc5IujGz09pM8C6ziCpLyp8iTbGNDP4IPu43Yzd3mwzUca8zrbYJo1cFrpAw7yKutUz-uaMbv8XCdJyxQtGmCKRnSSNneJ-2RF48V8vHjTqDT-fQAYjsW9eVWKq6AGeO7iKD1wYkXuIKg0HEIXlTdryMHxHWvLaBEPOQbz8lSC3s3AtLXHsNosydsurE3W3EEAzjd-OLk_LTw_v1Ux2UHJFJT-ORS66kOxDoDGJaY-W9qK_mbLB81hOkMBfIccGcV3GJpNDfvfah_kiksAEXNVflj5qNvqxhZjJDSyq0ZlfAkINY7U0EBGKVGV7TCvvTw0yNkw2RSVFiR6b4Roa3gqrCyyQoEWScIo_GJ3UjQ0-2EpDeqIkP8jrFnRSSMtpD_7bcn6WY2vuQjaOLxITXvfgb8ovz3Eun3wlvk9-0lyAys14eU5KfFUOR7Ofdj78FcuGYijFiRPOpC1T0MIgsd5GB7jbToxwdBw6ne4-PppkfrCvhSLQXWg91SBPdngSQftP8e7q-aIopGRYq8Gmn5oYn_Dti1gGyZgEKZqAeQoJczp0MYlhy8CVHZrOXYmqgmMTJ9aMEIARb1Snmj3H3ORUIj6CLoZBJY9DAoRYrRDzSRw_wrqqIr7pOpICilwTIDkVsjCu5EIcjDZMvsrhHfcnBXo-Twh8CbIujsZ4jAYZDHXPDhphJXYFzOQqIPaGoUUVOlCbwOOgFVtw1gV-JaZ-8NClQ0zy6aOPC4ObWUGIVspiFUyQGjBfdjbUoMKx4GYBL5sX5yac_7Z4pe4Ku-D6FGnBA8b8jeIVa?data=QVyKqSPyGQwwaFPWqjjgNjRJCxuqRWIErIjtKT9XSwIhEfnzMLUa-naPDRRTRjRGkTbbqb7C1-p70AIynafu0IpadrF5jYZOkyz7CF8Ij1O5Vf4L1b3jEZsXCttChAO9djBfxtBOSJhV3fUIgRh_OM_WREg0xQ9rZj6X51w5iQrxnyFs37BgdO3lz6CnKQWcuWC0ALK2Ey43zzoakLBa0om1UR5Ocvv9QQ_YR4gvwO12TEkKeBWhcJ1hK3JDpejlPOHpSSd2aaOScYd3EuJN2otvcET1tyCfWFY3RbARadPZxDyh30yeawP_cIkBwzkfbyXRA_Y9PjDVYiKWOPzszajYhDVkxFZrlV-7WErenoqDvXsv8rbtkSUwL7wey8ndUXIXU8rIhuXmpGRBUPLSww,,&b64e=1&sign=7e9d82148d94f0b02b82106fc7c2f77d&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 175488,
                            'name': 'Ашан',
                            'shopName': 'Ашан',
                            'url': 'www.auchan.ru',
                            'status': 'actual',
                            'rating': 1,
                            'gradeTotal': 173,
                            'regionId': 213,
                            'createdAt': '2013-09-24'
                        },
                        'phone': {
                            'number': '8 800 700-58-00',
                            'sanitizedNumber': '88007005800',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdzEzHTpi0PjSPa5iYUiqMKWKnnrKONqiUBMR8DUd8wvjfX_KNlyXmoC-QzvdZUWKUsKwrazLc5IujGz09pM8C6ziCpLyp8iTbGNDP4IPu43Yzd3mwzUca8zrbYJo1cFrpAw7yKutUz-uaMbv8XCdJyxQtGmCKRnSSNneJ-2RF48V8vHjTqDT-db7ugY6PZ_LwnSs2NGzMqw9bFx_S6zY2Lfifp9FFmnNvTTS6jQgowAwlZR0L8V5Kn5Htf9RoGukZAfruYOhfuRu8W1ZFvVj0Pr5_1erj-9W9iRS3rKD-xZ3oJ_Ea9dBQZeagFBkrMCstKhJ1drrH-Bkhm28o0yj3U_uj9s9KuVskouc7PGZKbHH6cE6h73-eaFnTCUuCl1TkyLETvsa0wxTpI5O0OnFqEM0VO4_WDufcVKe-FC_IjhWQRz6xi3kJ7BX5ZkekAa6z2nnFHK2bjFovP6oK7Us82qLtJrhbMDTM0_x6wfkBRS3OQ4vVGZzraOAFk702NHCP1EJVvZRqCj2XzLvnhs0UL8IvblLyPPVdmcwzrUfn4o2CHfq7ScGw6ChRUMSOhgn6_dGp93pKXLiIAyjPW1iiGMQ7RSaWITXa_H1tRGZFBfdChYrMimCYW7-O3SchNtKvag2Acuc_NinU2P4vhBjIGCPAt1NgyUnIyjwMeyKr_mJmz19sK1ihGpOwqzfHx5NY6f2LdeJNqHA5HbNXvtaL2xZKdlhBkt0AeV6OF6Dk1wPq9vL7jD4rJ6chA4DkO8XUQq_-MxP1k7_8IIqi3r3Iv0n6CLaqQmJBsEqkrGtCR_YCuqQnPXbGmguemGtbop5d7fWlb5qFx_KWop9_VIhw8uVyN6G5TCTHKzzJ2Wc-r7?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8fn6JxVUkC1fTmxCj7jD7NWn3uKbiQ2T9ueFrZOo-5qOmpEgzLzfAOSvDzwrF9MKGUoh4IDABs13SDmfGUxejeBY8lduZxt8Qq7rJigU_4Ui3Vlmf7TdRVqHCyhqRMew67Pl9sFEMHs3cDh81XGvI6&b64e=1&sign=9e03a2d2fa21e8b441a48d8fdd367ee5&keyno=1'
                        },
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 3,
                                    'dayTo': 3,
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 249 руб., возможен самовывоз',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/169403/market_k5bbKmSGr4Ej7cPLG05AmA/orig',
                                'width': 840,
                                'height': 624
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/169403/market_k5bbKmSGr4Ej7cPLG05AmA/orig',
                            'width': 840,
                            'height': 624
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/169403/market_k5bbKmSGr4Ej7cPLG05AmA/190x250',
                                'width': 190,
                                'height': 141
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '800',
                                'number': '700-5800'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '7',
                                    'workingHoursFrom': '8:30',
                                    'workingHoursTill': '23:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.592096',
                                'latitude': '55.706883'
                            },
                            'pointId': '411207',
                            'pointName': 'Ашан ( ТРЦ \'Гагаринский\')',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Вавилова',
                            'premiseNumber': '3',
                            'shopId': 175488
                        },
                        'warranty': 0,
                        'description': 'Постельное белье Сатин 100% хлопок. Реальный цвет может отличаться от представленного на сайте, ввиду различных настроек монитора.',
                        'outletCount': 18,
                        'vendor': {
                            'id': 10738434,
                            'site': 'http://www.jardin-textile.ru',
                            'link': 'https://market.yandex.ru/brands/10738434?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Jardin'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/QJ-HYoQcr_1VV__u5to6UQ?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=nnG72R7-3Ag3klNDl5RyiuYbMawjBj-jyrdP-6A27vWa7k-G4M7lbOUkW5vZQaNDGo0ViGQZcaTol7nINJmsOr2XIfX4edVIOkMtnUv__72cmY2ongfZZ7J7TSDiioI_&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 10738434
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZHi5otIKD5aXme8QG6Ybmq_HaNrY_l2QneUGBQgLeyE33yj-ilvV9r5xx1kU7fgPQlhZXAUHZ4Nk9EUENrFJ6C9cv4MemorC7VDoiIwRdVcFqnb4hzH201sFi-MPdbnOgTkaN1xICOjS0n0-iZSyUOKpkg-joI1HCwmuq8UIzs8eKoiwCdQmhiktQs70XAfuE45Z58uRx-gpasoHtvnGdFTDM4NbAyfwpziT2gJpq-mCKG-pcby0tKV9ijkEqF2h5qLawbZDfNohFklD-S8EeHERrrawildaGM',
                        'wareMd5': '0-L2dLMTKM8p1FCP8-VDuQ',
                        'name': 'Комплект постельного белья 1,5-спальный \'самойловский текстиль. июнь\', с наволочками 50х70 см',
                        'onStock': 0,
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 415176,
                            'name': 'Xlplaza.ru',
                            'shopName': 'Xlplaza.ru',
                            'url': 'xlplaza.ru',
                            'status': 'actual',
                            'rating': 3,
                            'gradeTotal': 88,
                            'regionId': 213,
                            'createdAt': '2017-04-22',
                            'returnDeliveryAddress': 'Москва, Большая Семеновская, дом 40, корпус 1, 107023'
                        },
                        'phone': {
                            'number': '+7(495)7402172',
                            'sanitizedNumber': '+74957402172'
                        },
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': false,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'methods': [
                                {
                                    'serviceId': 99,
                                    'serviceName': 'Собственная служба доставки'
                                }
                            ],
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 1,
                                    'dayTo': 5,
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 600 руб.',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/248114/market_ssoYrSPM-lGm-x8Q49ZO2A/orig',
                                'width': 200,
                                'height': 170
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/248114/market_ssoYrSPM-lGm-x8Q49ZO2A/orig',
                            'width': 200,
                            'height': 170
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/248114/market_ssoYrSPM-lGm-x8Q49ZO2A/190x250',
                                'width': 190,
                                'height': 161
                            }
                        ],
                        'warranty': 1,
                        'description': 'Комплект постельного белья 1,5-спальный \'самойловский текстиль. июнь\', с наволочками 50х70 см Постельное белье \'Самойловский текстиль\' – отличный подарок себе и близким. Качественное, удобное и красивое постельное белье подарит Вам неподдельный комфорт во время сна и отдыха. Поможет изменить интерьер спальни без особых финансовых вложений и затрат. Материал: бязь (100% хлопок). Комплектация: 1 простынь (145х220 см), 1 пододеяльник (145х215 см), 2 наволочки (50х70 см).',
                        'outletCount': 0,
                        'vendor': {
                            'id': 10718078,
                            'site': 'http://samoylovo-textile.ru',
                            'link': 'https://market.yandex.ru/brands/10718078?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Самойловский текстиль'
                        },
                        'categoryId': 12894020,
                        'cpa': 1,
                        'link': 'https://market.yandex.ru/offer/0-L2dLMTKM8p1FCP8-VDuQ?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=XPYJt-MlmutvosE-zohWS9Ad2GsnA-URlrdoByKzhpwcc6xGNIvdG16PRPHSoZbZob1aQVZxC4UjP_jJVfMF_XvUX89MwHzPUD3799PCGt73QVrFRNP_RISgnF5ix2mceLyVaf_hcow%2C&lr=213',
                        'cartLink': 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97BC4mr88tmrA3S4sJYzmw4jVSCd5sv3vd1hfvdAFtIDUIdwsgIAT6dIYE-X5vb9oGcebzAmWojL_Y2YxauqhLpQLtGTBZE7SnpOcFrU1wTDdQm3dDcSgt0hiN5BvSFd1F_X_Pzb3SBlhl7jK6hA_A6z2YNGESuqb5S0Cu9N6ihwcZAiU_rLGNwDlk7y9PvG0CeE-bVZk4vNr5yqDWESFLQPmXZbJOam502awuZGRaomIoS-yXEwEr487v-Vlf1Kdj-3qO5F0J8y6HBZ0Sxw00AftVZeqMtKEfQj--4t77JPiO4Nvyz57AbOun1r4vudpz0ha1T2w8s_goI352yXJg4tkEf7-WrzazVQE5zL4UduOHo3FBhhCez3cpue_34JsAPwIAuAdU36nfMNYzEnmHXFsX4UrewPBF5RK-ynsBv4WLxcPo96_nq51iqXahHsm3A2_urwlMGGhcHgbi8UX_j3bqg2kVSJ-nfuTjgdHlKwJkEDiMrsvofuHJfKwyIGOvUo1FG7ziio8xAjaeLqG7saxO7VWGWluhqzF46pBM8rydzqfuLHvEpso9gUa1giT2kWfYQQUuiPcWZu6apeaV084Q5ce-UxQW2ny9p7hNUY4lLMl5YEblyaP4VniFKnSmxkA6paMjE238xMoPhRXH_Cm9r4uH-aUozpT4-YgSQ452stJsW7JUPOs_kCrT569CrjrQjI25t7h?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXzjqrdVr_OgOKNi9F8doNw3JiPfc45SMPPHi2u2N2lO2bR3W4BQG3HEMUIF9Ab9sUYTP6EqiZxkgT8icJZ3xkN6Dm-R-UXwj-cS0Cy65109OWV5t6BCHGUHDLxZTYSrF2Zu7OMhECUW8aQLjj53UNeIL9AC1AaezYIFhG8Q_ZmDTDQF9menSx4YK9JElmFn108wBIpLDZ--kt5vNrbtH3uQQPVBwkh87EElZnIjW3cBWC9M5gPTOjAEe-SgaRTDMiJ5W-QRzlwTlPA-7NFOTpzJs_2GmHHw7wDvB-NIuewci3MklySIiLdA,,&b64e=1&sign=da67da5756cf620db3b3f33aed184908&keyno=1',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 10718078
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZFChZSrie3fvbDumjVK_331Q582OtSOs0fiJ875YrPuJXGBSWJBZcLTjc0FAUG5w_DUlno8YmnepnT-TMLHuNFf_oQq2p_BD9rCjeU2EFCw1WfyGSVzByfHE2S0rW0gkn-zlBPPD9jJoAQtVuZUiHb5RE_YuoXBbCximnPisPgsKVcST9tL4UoOvU9Uk9-W_0HhlpK9bLa-MjwtfJzDnHXmF4-55oUVugaAoygV_i0_RkIcXLtsV4ysoWqbTThAh5i2IrZ8_7dmwlLlloKmwJEx_xUVpwPn54M',
                        'wareMd5': 'AwKKeEIwsUqk_XDklZBSCA',
                        'name': 'Комплект постельного белья Carioca (Валенсия) (1,5 спальный, наволочки 50*70)',
                        'onStock': 1,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRudsi_rLy0uw6nn3a8ae2SOV30JztW9HaKOr5BPx6fcm5YTH1xYfzeGrz1a2PVHyqBmks-AAfK9sawJ4vqLed4GPaFKgT2S43ZJV8yMTrOlVUlz-nxqTUpXLaB3NLwpvaYPUh7zfsSwknyhd0azQl0l9k4v5_iyjUm5Q88s-ElPw0ruMcb-Heqh4tpDZlcg90XVFCpaZKwP2P5ZbWKkbFCmEpwkmKHkSjjDt-jzkcuXLdQiPgnfPSvPKfcM8cFtroqqfkfwS8UCImwjuVXrKUSy_Aqn7XXP92xQ5cQl4KTTSnhxr6Dtx2alwvy1pEE_G_PzKllZO2kFftgYQM4DdwxLqP-_D99mM6GTgqSHZiW4rJ75cfc5d5TcMobWiem_xrfEtlz_G6r2cpDe8d0FWBn6-56UC0vVGUbva5fVC-K_PXqtf8A5Dv8cflu1ZPhJyX7_x2YLiKNFlVdgk3FExqQ-y41dLVCUEpIlU1SmVt7uvQvIgK04GTYuInFabd272J7lY9hbuf7-tzdcSjGMDG9pLkMTuOQHHF62sM1fJPzqakCcaYfEPnYN-Y0k9L7dX-7t5KTlvkwDUr_bLgmibLyshCvas3RCFCR1ypdS20QIPPbhKn3rHCGyM9wvByoSQgxMt_m_z76QNGgtRFuomaYkcHSDudXkVc6b45hgq08gmvUtuO28YqQQPAAmjTHDgqW4qFkXCxg1A2__8Grf-JgatJxjBpQ7WtJGzEd1LPSWrBIYa2eL4cLpGk5YmzmEan63poVV7UtWvgq03MjgdTRPptwrpT0qhhMexSoJ65iR5NZsdz9OvEiQu_-Chp1FLJ8cJpHZL8-P536lwy4vekY,?data=QVyKqSPyGQwwaFPWqjjgNgKDqdEy18IjRD5AWDiSZP-BAJj1aOtD45lTxevVUJOBE6eAGvVfZGcTxWl26w6dfvaKlwFAK_VI1EYLAI7kFu32YNZg9GVMOhktPLC6D3amX1p29AOxlHWhS5c9vVzFX7YD5YPozoSLGdiXTDtiP0i99HcrtdV3MLBtMXDkK1lB2bKfDxbRDw9vyplqw_FnaJG8RaMZ0RlocKZngCdqLT1k6x2EBID6EqNz0n0nGPz1jPR-Ppp6ky6StMcchwFVr7CtJcsPI-je85THvoJpVvt9Tao-Opl99bmFksMoecXfKWKYZ0w_yoX_4KBl_vJM7Q,,&b64e=1&sign=3ae107bbbfee0aa77102224fd97e9a5b&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 305787,
                            'name': 'www.gite-line.ru',
                            'shopName': 'www.gite-line.ru',
                            'url': 'gite-line.ru',
                            'status': 'actual',
                            'rating': 4,
                            'gradeTotal': 0,
                            'regionId': 213,
                            'createdAt': '2015-08-19'
                        },
                        'phone': {
                            'number': '+7 977 162-80-60',
                            'sanitizedNumber': '+79771628060',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRudsi_rLy0uw6nn3a8ae2SOV30JztW9HaKOr5BPx6fcm5YTH1xYfzeGrz1a2PVHyqBmks-AAfK9sawJ4vqLed4GPaFKgT2S43ZJV8yMTrOlVUlz-nxqTUpXLaB3NLwpvaYPUh7zfsSwknyhd0azQl0l9k4v5_iyjUm5Q88s-ElPw0ruMcb-HeohuFXqLO95akihssdnmU9ICJq-6GocDynBlrYuTKRDnVlJApck3pHYejZUrtCsvqtdJmv7Mvj7L5P8dGwn7OxkXdkpW0zFjeqWA4m0-D4jo_qJ3treduiZdyPTcGhCMeTyvT24aUzNayMBjuL2WvegfHSgBC7Z-Sbk_NhlPdU7j688-5QNiOA8cmtvr5Yhc9E0NVcf5U0xdhsllBG5cKK62Qjluw1QqVBQz6psXMV8LUcOAptUp7js87klbUfzv_lHu5Vlt8oXdZRUTZnhS82BAFs3yn-tsE_ZrTkfR1b1ZaeO2bc9kc_uMk790EcnSc8isAMXXHg1eQNaD-Mu5UsXXTTCsUlM5h0XJueOvTrnecvDMqVc9NCgZE1Bkl0XoGvMUKIKZ_FF02pBId0MMKahYi-yDEVIa3oQ3hu1jVteDt6D5SZepWhCKP6W2jxz06On_WZX0pwemxqFq6I5VTXkZ-4CSkfViaf9mz4sLQnolnpL5Cj-1aK-5lq3yf58dTeXv-WYlKljgMj9dg8YQCSxt6qhkXcARaWMaOnilPYKTkAmjrbU-yL8KPeHhQx3kYmp3eoMgDoyhPLffsxXWDRWMM4iOD7WEfdQCoERuZE_SotBntUJVni29o8GznMvpdh5kSXyka5E3QB88UEGfRzPpUjagZc4LrE,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8zdOj0Eob6s-FGNnsyO1hwXZGMng94FRb0Jaqr5FaAPavLisTQSptnIwntfSs2SD-ISPFG68cxZLv2JpRSPn6VLbeGHR_uBw77Dq3v69e6P9pTpTmUtS0Rv6-yfLO_6ddWfrPxi51YN-8lrggA84qy&b64e=1&sign=19fecce98386fe820edf0794219fd96d&keyno=1'
                        },
                        'delivery': {
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': false,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': false,
                            'brief': 'не производится (самовывоз)',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/168221/market_f5RXOQnccXefrwYwNlVICw/orig',
                                'width': 496,
                                'height': 470
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/168221/market_f5RXOQnccXefrwYwNlVICw/orig',
                            'width': 496,
                            'height': 470
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/168221/market_f5RXOQnccXefrwYwNlVICw/190x250',
                                'width': 190,
                                'height': 180
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '977',
                                'number': '162-8060'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '5',
                                    'workingHoursFrom': '10:00',
                                    'workingHoursTill': '20:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.648438',
                                'latitude': '55.852612'
                            },
                            'pointId': '14229624',
                            'pointName': 'Снежная',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Снежная',
                            'premiseNumber': '20',
                            'shopId': 305787
                        },
                        'warranty': 0,
                        'description': 'Комплект постельного белья Carioca из коллекции Валенсия, выполнен из ткани перкаль (100% хлопок).Производитель COTTON DREAMS Россия.В Комплект входят:Полуторка: пододеяльник 150*215, простыня 160*215, наволочки 2шт.Двушка: пододеяльник 175*215, простыня 220*240, наволочки 2шт.Евро: пододеяльник 200*220, простыня 220*240, наволочки 2шт.Дуэт: пододеяльник 150*215 2шт., простыня 220*240, наволочки 2шт.Уход: бережная стирка.',
                        'outletCount': 1,
                        'vendor': {
                            'id': 0,
                            'link': 'https://market.yandex.ru/brands/0?pp=1002&clid=2210590&distr_type=4'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/AwKKeEIwsUqk_XDklZBSCA?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=EDtYJ40Nqpto5hJiAAEmbndOk-l_0893tFkcB8rEMuBTWJgdj1HhUjNudXFj97UvKVlrozjOx65dW3IOGiORPe6ljHEZy7jjr48AtJLqu5YJPwki0QoTliE_gpSRRIZF&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 0
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZEx1KWDhNni-BDtoj10aaSrp9Lr_1mKPKeajWAqDxi4Bg',
                        'wareMd5': 'XXr2iVVLJ9N79iS9zMghLQ',
                        'name': 'Комплект постельного белья Sova-&-Javoronok Личи 1,5 спальное 50-70, (поплин)',
                        'onStock': 1,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86JG8YFRHK21GM9EkAgkJZoFabTjahDvxEeSFXJt6pI66VajSAmFKa8375m1rPFdNZtz9qRUVqLDPERnyxgJl9UaGLxYDjK2lPwQw26VVTK3-RZvtM8YfGTIAqGSFRlSraHNFt4m2wZk0M6KiE7QInuzEHLdWRrz1TZTywUvqAbnMW0lJT6CEpejviK3TH3qsF9wb0Z4hM57nMNvLihkJbnsbbmdFlIvLafVd68iZHcHonAdHSqNQrq2APG6DHQxsW_NefUouUoD5I6RgGjo5V8ooytyMRIomtu0B7nsJ1UX7rFex49fcfd6GY4Rzf3HiWptdhHa6VOyHG4hYdmXSzYA_RRig7rVC2_Vfwr1J4TdWjpT3cyEPBqCk8nd93lhe2LLV8WSVTkgvQSzMCvtNG2_2diJWv2ow_d0HU8JdXdvlllse7JH-7sCETRUM7ce-qgBjYvc--lY-iwa6ephUMrkdMYxdQBNTVaQvhDbk7Nu_qnKqDTqF1epSTDCCkYo1FPIGY32ar3Cvvkxyr24BQgy7ZlWg6XIxVuzKw7fnE9qfAFiIO7aqqfhThWXgrKSw2e8R_Le0Qp-TQoyWva5GWziqsqJ9TzEbi8ZirNBJTghGKiDJxuWv5bUDGKnX6NTy3xTaKJFsnCfi2t7VfPkYTOlq23-bX0p4wU_D3_2vvJ48TxeAS5Td2nx_hC4SBSQojyRTX6AWQhu8qKYzCa7cJgOYd8BUcQdTkxs0Me9XiJa3qXNvJTpcixzKPqfFNKypX4tfht0uBTs-5jg-JkZoykCBr6wfJHXURNWGVXEvoCrvx2SyESqIZgOAduGt-SVhGAm6FK_9lNPRgECAOxnT5Eg,,?data=QVyKqSPyGQwwaFPWqjjgNtwFDITzDmMAdbMKRCiaGZLbhcbaDUDzUQ39qLv0FTeGK78yhRrEtvwiiOHi98JR0igCAo8oEmv03qZmt5ySlT1irl9msVAsVpWZkoV8vUvYc6C0nB1YrEeg1lcJFte_DrJOfJ_jy7TnWF2PGVU-qLdWBwQ5y3_X8GkP2qhwm7ZXQMWbK2uUabWDsSbHBXHRaP_Zk063Q1GD6KHbconh2iZFpQ71ywaXIsly8CnEKfkgcNE9Vo7Xra6LYVnrq_8WbL8y8xlZn8V45MkhUAPRJhyzMOW09bYfqQKFXm-3Wbr6769ySveb__hVG1-gbMxfyZHHLqV73Uo6djmuzGtdwg2-c-XK5khK6ZBMEc_fksrWy51YjzL4zO0sSJU_8dpgKg,,&b64e=1&sign=bba937874b80ea9872ce98dbfa30cbad&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 25017,
                            'name': 'CompYou',
                            'shopName': 'CompYou',
                            'url': 'compyou.ru',
                            'status': 'actual',
                            'rating': 5,
                            'gradeTotal': 4577,
                            'regionId': 213,
                            'createdAt': '2009-07-17',
                            'returnDeliveryAddress': 'Москва, ул. Трубная, дом 25, строение 2, Прием заявлений только в личном присутствии с паспортом, вход с обратной стороны здания, 127051'
                        },
                        'phone': {},
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 1,
                                    'dayTo': 2,
                                    'orderBefore': '20',
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 350 руб., возможен самовывоз',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/202381/market_NU1DIZ4PnntKPUXI4aW_sA/orig',
                                'width': 500,
                                'height': 375
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/202381/market_NU1DIZ4PnntKPUXI4aW_sA/orig',
                            'width': 500,
                            'height': 375
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/202381/market_NU1DIZ4PnntKPUXI4aW_sA/190x250',
                                'width': 190,
                                'height': 142
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '495',
                                'number': '565-3485'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '7',
                                    'workingHoursFrom': '10:00',
                                    'workingHoursTill': '21:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.6235673',
                                'latitude': '55.7715595'
                            },
                            'pointId': '321317',
                            'pointName': 'Пункт выдачи CompYou.ru на м. Трубная / Цветной бульвар',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Трубная',
                            'premiseNumber': '25',
                            'shopId': 25017,
                            'building': '2'
                        },
                        'warranty': 1,
                        'description': 'Тип: Комплект постельного белья; Классификация комплекта: 1,5-спальный; Состав комплекта: 1 пододеяльник, 1 простыня, 2 наволочки; Состав ткани: поплин; Количество предметов в комплекте: 4 шт; Простыня: 1 шт; Размер(ы): 145x220 см; Наволочка: 2 шт.; Размер(ы): 50x70 см; Размер(ы): 143 х 215 см',
                        'outletCount': 1,
                        'vendor': {
                            'id': 10738845,
                            'link': 'https://market.yandex.ru/brands/10738845?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Sova & Javoronok'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/XXr2iVVLJ9N79iS9zMghLQ?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=U9xqPzGtajIbzE6ez9_3xTjiPnwpk3FQ6qmRL_vPDxc4npMZzblj4iWsA-DbDt9QYUltNQ5yWHlC_w1IlTtM6iok2cAofBichnv-Onjvfe8DGchI5yip4rBeNZtxsRm2&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 10738845
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZHIfNUkUlH1A0BHtJjtpqlPR9IU_beLXtYOEQq-xnm8hQ',
                        'wareMd5': 'TUjkF6dUxFuodjwW0ZQFxg',
                        'name': 'Комплект постельного белья Sova & Javoronok Индира 1,5 спальное 50-70',
                        'onStock': 0,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYo5ilgsL5ZPi4uDDLbCcWpAssiXJlN03zI3dFH7Fv2Y8ImGHPNjNXFK8F98qvg7ejr9DCQCzPZ3dq3Fi6Ct25bSFBmZwhqYFmGzq_P_1Nl87DQV7BfBIgNrOoe9sI_RrtB8xBXx08Q7Zjn1fHtiBpkYWSY3JJnUehHfDBtTwJq36WIw5mejCAftNbK_0SVcEg3EWI5Qb3L0PQIQQ01g8-YxCR2TagbiMUJbpeGenoD93jBBDelZGJLjJB1QXtLnXdTVUUTPRLsjxy-GKA5RbDWl1C3TDebjRR__2HriHWCqpup1hPV-nz2HL6hPxCblFx8ARvKIcW0FaF7g0poQHUI0IKRbXShEBM61vlZ65fZyV8lE8VWthBfL1VMzj4S3vtO8ZzoKq6f36wpvx1Ciako0zDdf8O7EyPk0MBu7KrDoWNSschrqqbU1cSsyCgqri1GOkZ0DAyBuATK1RWZAqwI1IYple7myPOEkDbCFgQD9BIypWhiWzEBmi_dCh0VB_1HIEKZ8JtnoRhctxHugy6ETIo95-t4PejwGg2r0I8WADYw_FYFiIqACOnTaAt4t8wc6uCQiHbgK9OkzT9jwZmX7Wf5p43eJnmmXQFv-x5SkvoICtBSSR2MV00wbjVph9qfGaJ-ULZ6KiIjc2gPwu9-zZfG-A4ZmkuLMZ8jQKkGodYgBYYZhsxZhE1z3WVCw864dtXgCQnu6V-QRNLuYq00s0h2ZoIlrHuC19UUnSsTNo8A-alIxn4E1qWePTtKW1jW9r8eziJBzTK8w-vCUdWA6qfdvcVBeNOiz-DTo7reQr5V0Iev0LfTv7f6BB0bf5IscNLumKx5WK-MhG8ECHUW1CBzfVsOGjA,,?data=QVyKqSPyGQwNvdoowNEPjbv5NDGkDw-huUpGHotaEKMse75hWj8mPB4MjqoIf2_LAKOWg2WaIDG3CiIUoLl9r6sQqK2vFx8YOqAigyjcw99phD1XKz_UwgLh8P0oLMDcuKmScuySjEMSQKkMqtGCV4YdY8yGjd1OcSwbqp7NUvUFWqKy7bZUMGRaBCIJFubkoZ_2WFr0pnGYI2dfaqs4tAXmWwc0uPi7W1sREeXlj3QI98Y6r4GnUkV6STLxUFNJ72mRKcV1UTGCrjmqowNN6SVGYYczTMc5Dw1HZ16Cw5ZUPGBIBPe_rF8TeyyfZiHa3i2uzH9ogiNtPBKi1Vnx--37xGMprs3EicevmYJ9KSR332q3i_Nl5buSHQd1I7bjekrFky1azDroAxvx2AQVNfECkpejLY9OUdQxZANzRGe-cencT8GvQ3-4V-h6pLE6M1nlTRbeIB5sUt8JHB8OHTH9Mk_uS6bOi1UgTFN-Ke4g9dM-MtbwqgrvWwUEMtFPdi6mnoXAc4y-os5EF-8k5WeWHes4tt3GC-R5hcjeqS5qS0OvlNZVOQ,,&b64e=1&sign=cf36964a3b43b1b34baebf848c732dce&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 5205,
                            'name': 'TopComputer.RU',
                            'shopName': 'TopComputer.RU',
                            'url': 'topcomputer.ru',
                            'status': 'actual',
                            'rating': 5,
                            'gradeTotal': 10681,
                            'regionId': 213,
                            'createdAt': '2007-08-21',
                            'returnDeliveryAddress': 'Москва, Гостиничный проезд, дом 4А, строение 1, Белое двухэтажное здание (Медицинский Центр), боковой вход (цокольное помещение), 127106'
                        },
                        'phone': {
                            'number': '+7 (495) 9262641',
                            'sanitizedNumber': '+74959262641',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYo5ilgsL5ZPi4uDDLbCcWpAssiXJlN03zI3dFH7Fv2Y8ImGHPNjNXFK8F98qvg7ejr9DCQCzPZ3dq3Fi6Ct25bSFBmZwhqYFmGzq_P_1Nl87DQV7BfBIgNrOoe9sI_RrtB8xBXx08Q7Zjn1fHtiBpkYWSY3JJnUehHfDBtTwJq36WIw5mejCAdcQZybmTOmzRzebZsLymmI85nQd0uJi9UBolQnnXrlJ8WYXc5hzJp2p0ZLHVxIEsVPDkJK97oO7goHOg49f_ZKVJDq6BNn9XatuDr4GFjM5yapgbW2hlWedUZpIF-Au6vXT5WD_W0CBvwM6vtq7-6-hG-IIF1S3RIgiehVDaHLHfk8YycRBtEf6dIxCTbyAnGvzPJMmmQtLsdhC8qD76zeCV62qDPlQ7TyjgjgJI-ALI3b7Lw5kVm9Cux94-xZz5da3PYyLxkJjdSMvFhyIeMYIgdgN-QfyYk9F9kHea7vyssEJcCfZfoveXnZ7W97xc_dT67kQknIi9dQfhhwgcyDGxXh2Yamx_BuCx5TyWNGEW2uT02FRSTWag9TM8U77YbbETiUsKtN2IJ-62Kv8dQp9rPH6BQUQCWDYgxNPP2sfG_G40ybvfOPBlMPLDixZ1Q9WJzHY0SXXEtI06mvU4cXgaSc2W9NtUGCxIXJBvMOqUhJEay87dEBa98ngYdEwy0nT3rVXnwaAL_KWQaTZxww71YscBmAyM-CS-fE2FvZCIh2GlPJw9-9k16c94Dwm0mw3mWCiy_5yNYBSocS4avuPqia_xhRYFqGhfV_xvOU1gUV0twvKcn0bkdZ8q_3kItT4Zd-ImyjrWJXDPo6chCaUM0a_VxP2BprGkvjo2DO7g,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_10Im1mODOIyaY3oy9mjuaNg7FM0n3zttrqSLZ6MmSkExHuJQzSYgfjnR1oU2k-2i5RHPr_8iVgxRNaqhukt2IujNL7O5kHhd1xLy1WeQ7F4Zx44AqsmOBux7QGS5VU8myw5Lo6TdpaPZlotIUdCtR&b64e=1&sign=89d246b9668dee61cb576cb99dae48ea&keyno=1'
                        },
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 3,
                                    'dayTo': 5,
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 490 руб., возможен самовывоз',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/203934/market_sbWnr7AnT58yOTrYl9mOGw/orig',
                                'width': 900,
                                'height': 900
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/203934/market_sbWnr7AnT58yOTrYl9mOGw/orig',
                            'width': 900,
                            'height': 900
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/203934/market_sbWnr7AnT58yOTrYl9mOGw/120x160',
                                'width': 160,
                                'height': 160
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '495',
                                'number': '926-2641'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '5',
                                    'workingHoursFrom': '9:00',
                                    'workingHoursTill': '21:00'
                                },
                                {
                                    'workingDaysFrom': '6',
                                    'workingDaysTill': '7',
                                    'workingHoursFrom': '10:00',
                                    'workingHoursTill': '21:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.57461397',
                                'latitude': '55.76007516'
                            },
                            'pointId': '193208',
                            'pointName': 'TopComputer.RU',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Дружинниковская',
                            'premiseNumber': '15',
                            'shopId': 5205
                        },
                        'warranty': 1,
                        'description': 'Тип: Комплект постельного белья. Классификация комплекта: 1,5-спальный. Состав комплекта: 1 пододеяльник, 1 простыня, 2 наволочки. Размер(ы): 50x70 см. Размер(ы): 143 х 215 см. Размер(ы): 145x220 см. Состав ткани: 100% хлопок. Наволочка: 2 шт.. Количество предметов в комплекте: 4 шт. Простыня: 1 шт.',
                        'outletCount': 2,
                        'vendor': {
                            'id': 10738845,
                            'link': 'https://market.yandex.ru/brands/10738845?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Sova & Javoronok'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/TUjkF6dUxFuodjwW0ZQFxg?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=U9xqPzGtajLbDzlBAZlYoJTVDukL5ERm9l2oMyBxKZbiWnk0nAeudD3vR_E50bX7rTF--HxqunUBbBI1OW9b1Z_mUreO1yxnmhxJ8Df-qmQ9vQDfmMUqTEVMxloPPN0F&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 10738845
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZFGUUq4mFI9HlSJGhZvBlDzBl4Cs8izf41m7fuDb_alIA',
                        'wareMd5': 'ubOQdw_Ov4gRVf5tZhZXSA',
                        'name': 'Комплект постельного белья Palermo 1.5-спальный Персик в шоколаде поплин, хлопок, 1.5-спальный',
                        'onStock': 0,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mY2KuJPeRWaXUVBpbZqxIwOCmbKh4g-pQZ-RL8zSEy5sZs4ZVyaazK0F-AeifhOwUInnG_ulEk96eP6gbnEMdTQSA6DQ33gvqwvXo7W9dLqKtJXLDSxzEBwBNKO1Dp14ToCKI9gN-JI7-G65nEFAKuEchOCOX69YnadrgtX5xdvzsYnOcMBuDleDcaiUujCczUA-BsxnfXP2hbEoGe0W2UjbQsOEmoJED1NRIhXEQ4KiJ0-DbI0-OOg-dGDRp3m8706HsQ7B87TeHfbKAjxgL1C7CjRicF2PHkHY8Xn_GbCbqtVATHF7_Ifz8_bqB0wo6pq5HnQVVbO_Qs2UNLNMr1lro1WVlF983nDPla3Pu3jk4vmupbgFyYzlXgJ1A2iwMYRGa4vPPLuxyAcHI3LEdwwayeBU8lSYXVdI94NrFvFv6LROdg9dGweUHJ7Fw-Pm2QGpL08kBkg8wkVB9dY717mGn_JZgWtelU61oaQdB5mRPSUjI8_PQzdnlDEwh1B_hvEGEXQ2e5K9_ggKBtqWfCJDyMD5SoXdm_RE56mFru22YH3yu8zfAR4GzIjZiwcJgE-lXuY89N1Dcup9_3ob8qB-IVL338OETwu4HepNG--nd0ks6xL__b0wiex__4jbXvX4JiVFbLzavSDNPFJAh33KSqisei1ZKD01VvAafuiRvW7I8Ux8ohljuNA5sax8fr6fBKo3LOqeSvtQkEUbSXQAf4LKafaymkidF08itrXkh-Dkuh7BExkKsNAaD94MZzZjTsMN5K6LXoUFBNeyH-deEfUuBtzFsHbdZUKPbrW6KOdirZHR1m7MjDAUsQgFY2WDYKbgSpNI0dgxKkid7UaKviBpjeC04Q,,?data=QVyKqSPyGQwwaFPWqjjgNq131UrpwgN1G1cWD7LVF0H8OH_j_xcX4ruOV9szLWjKwVBR73B3LmvGEd7SR6SQdV_ZoUxJDUGDyrgm1x5X1WgpmxW12q4p5-lWizyfnMWZi7qbdt7cbT5L8P8qO9KnNcR945eLQqVRPOhsia9PMqHsw62s_7htLVhaEWs9EWsCZzA5d-7njwqDI7eICtOGAReUH7WcvRGpkH6IE12H_CfgR_bKw-O8FkZVQkgz586qcGKPqJTlo8F5gzGXIXY67QtTOqIH7TED6ewmex_TFQ3Y9pR-tGMjAQ,,&b64e=1&sign=4b240e587dd3fef6a25ca925959b1ff1&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 252831,
                            'name': 'Оксар.ру',
                            'shopName': 'Оксар.ру',
                            'url': 'www.oksar.ru',
                            'status': 'actual',
                            'rating': 5,
                            'gradeTotal': 1444,
                            'regionId': 213,
                            'createdAt': '2014-09-30',
                            'returnDeliveryAddress': 'Москва, Складочная, дом 1, строение 13, 127018'
                        },
                        'phone': {
                            'number': '+7 (495) 777-18-44',
                            'sanitizedNumber': '+74957771844',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mY2KuJPeRWaXUVBpbZqxIwOCmbKh4g-pQZ-RL8zSEy5sZs4ZVyaazK0F-AeifhOwUInnG_ulEk96eP6gbnEMdTQSA6DQ33gvqwvXo7W9dLqKtJXLDSxzEBwBNKO1Dp14ToCKI9gN-JI7-G65nEFAKuEchOCOX69YnadrgtX5xdvzsYnOcMBuDle6sAFbXiK1z5f0Rj_MMoB3D5-kfBL2AgnhOpqQvBqvt4RJBdGtbR1Y8WOWBh-tsMeIGQg-ksXj9GJy-AHkqEbngPBvURvTawRWJke8aMn9UXX5GqQMOHI1DE1DBb-Zk0g13gfvyXUuYrINPA1-Kn2qjd3Z93fweDkYtEl1JZaT0FnZxmU_vHXbKryIqFPgd4iRpPYDZKKmNTWoOfCxJv6BEH7f7xNKKHPJOjzBAd0csVzx0oRDv2bJXZkebLeecPJXIL3or9kLH7ajq4Ah1ZkWSEAxjBboM93OmDK0bs01DHQGQK_S6wLYXfgEfz_-9RPKbiAtBsOM0MV8FINlo4hAAuNjySTPb9XHo5lSGrqhlu_RmC8atMOQ073YOrxDXR5gIzDq6egxnk8KWbSiITlx0ON4ZANz6QFnx7m8vfjSUONQrOovV6l8ua3wlfw4W62epP-rr5JFmAzpk66OehAo2vdECV45zeEce_Ge1GlgoTeYDdGJ70sBEpUmVZ-dfd4vYsCUABU7wmuWqC6-TOTkk7DlTOB9MqRwmDJ4A65PFk3jpw3JN4fRJZOrQLAHiNOA5jcQUF-xiWFk51prxXrKo_mviOJMeQBcocKkLdrLgo2nRxchX1PqPPgANeSy1Qs3k-vKT4wh2ssZByZ6y6HcEkTNs-9td3eplUQgKFTbqA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9M9uoWy1Sl6S0PX-gO_nGw89NYoEFCIoTTt6MzzZcQ2I5DD6tTOUzowsmm66mI8zeN5K1IWdrHmIW9qAMC5P8kPKGsxBdNddFwZYMTqUCzTthq3TgyJgZ5o5ivX2dcU1G347eHh8tlfeJyEa814ImX&b64e=1&sign=872953ad7146a2da1d9dbd1358f2dfde&keyno=1'
                        },
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 4,
                                    'dayTo': 5,
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 250 руб., возможен самовывоз',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/174398/market_PxTUUGfiUqMvKd_krrsOmQ/orig',
                                'width': 640,
                                'height': 488
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/174398/market_PxTUUGfiUqMvKd_krrsOmQ/orig',
                            'width': 640,
                            'height': 488
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/174398/market_PxTUUGfiUqMvKd_krrsOmQ/190x250',
                                'width': 190,
                                'height': 144
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '495',
                                'number': '777-1844'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '5',
                                    'workingHoursFrom': '8:00',
                                    'workingHoursTill': '18:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.59261595',
                                'latitude': '55.80333688'
                            },
                            'pointId': '361769',
                            'pointName': 'Оксар.ру',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Складочная',
                            'premiseNumber': '1',
                            'shopId': 252831,
                            'building': '13'
                        },
                        'warranty': 0,
                        'description': 'Материал: поплин, хлопок, Размерность: 1.5-спальный, Пододеяльник: 150x215 см, Наволочка: 70x70 см',
                        'outletCount': 1,
                        'vendor': {
                            'id': 15214350,
                            'link': 'https://market.yandex.ru/brands/15214350?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Palermo'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/ubOQdw_Ov4gRVf5tZhZXSA?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=DO4mhPEWbp9eejpOaoapBKO7R6HXBjyqrIeWu1DXI20lmqXOETHeh0asl91NVLkrAdmBi0clj5TQ4372rLFmv8kaD-yQl5eJBY7IIuqbXUjxcoCJU8lREg7SdfxKuSGe&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 15214350
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZFDrCC5Q5jdQZj75d-uwqPVn47mbWkXO4IKkOBIV5zTgw',
                        'wareMd5': 'WMluum70_b6CVMis__iWgg',
                        'name': 'Комплект постельного белья Sova Javoronok Лаванада, 1,5-спальное (50х70 см)',
                        'onStock': 0,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86QFlVnO7dMxx3vM_Kx_5VBMS9nRbiLiZ1RTuDRhz_tZFNiamAugigG76pBJlk0fC6lfkZMGd8F_x31yOD1hhi3jSxG0J9qFCqfbxbeT174J0I4tSyC9HebsS2Cf6FTWNQ4IoNoeb5Oj6hc99uzRmM96esTxbiWiW7h42v6hJc0S2qCpZX6CU3hil8LqHXkUsiezlX8fKsbSgSOiiYF66AHeH_Pc-wY9ixc8-ub7KrWDKflflbTej8CJjrC6QKGuvVdgZGpHEN9McwguTTWjigVgf_CkB4edYg2IMFHICGeQhjs4etGyhKLGCjudnYbc88ak2MrGPXIZJKkbnCgOBdR50q-uludI8Wc7BAi11lhJsD-cPDVxh2fdNUIpBym5HaBgGdwYoJunDV5lqf2AwYe5swSFmdxeb8yc6OswbKHu3rOm6GPbTiHO61cd4EXqBrUFBNsbyG2Oma4ZQLOK99n7mUNO22fotSPdwyT7wfhg_ZJNTuABN5TDD1eyYHc9tvvHWwrdmguHx3XDFASQov-rVsCsrNxYeHJlFKijhHNlRIGu-rds7YBsNTCCBLIIwJPVkBCZfyx-rSlX-IHHyNMB3_xT7nhpUVaF_XGccwFOFoi_DUqZMjDvb9-h926JXqz1CL79LlYwTNlf_ATQXVpMxr3At5o1C-9QWtuPGNEmPAaDJac9rNcdLMo1dZtzuz3U7cZdVo3wa6bEjdQNufJXgULyGSnEiITwW4wHf6hviN11PdTYnVVdZp1ZjxvW4isR77PRxEb48wHHZN_5L2gyghbs3TA5eDeOSV3C3BDSNH6eIG_d8dO1oufHT0xFeY23bgNf9v9QhL5e51OPx_cQ,,?data=QVyKqSPyGQwNvdoowNEPjbv5NDGkDw-huUpGHotaEKMse75hWj8mPC1mTf8qoHro4GyRwRjcyjYwOqkrAX0DpCPnpaDOvSlui3aLjaI80YaDcf1eQWFcCMH8EpUqP6g6J9uWTUoGDWgnVPbnY7JaVfszOCVBcnZBqWX4MHmmaktlg08_DoHVCAPGIiVRrrWzYlTTQgPN4Izk5rHVuHmPLhfVXeQhCDSdZokHrxLXfEJZ-WwCyo0l_T7DuPcD8BYpnhc5yEtJ8QESgj-gxAglek913wviWgmz9FRz_gqKf2uzjTZH7U_SFG7kMroadln969hRCRz9Mu4YJQUQ95kNn0Rj1AHK4PTux3jR9pDN3i2KRVeapOMh04gecSY3Zw9-Bhg4ofmegmh4P4Cegd_Hh-Fa9s94wFQTZIQ5hAmJ96AhALsbqIwPBu2CT8JwTdartRSLq4kpBeVi5eAuqVpLJbnm7rwQTZ_1rUlAgyAGWi9i584hSnu6tTsNuARTGTtWA2AGEbyTyCLib09kCVys2H2hINezvfvGZiUBaIrKh4c1IKFDNju4vK9ri9uolNx01j15RF8QeWU,&b64e=1&sign=321cbcc25dc4b511b2452f6f265c20f6&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 5205,
                            'name': 'TopComputer.RU',
                            'shopName': 'TopComputer.RU',
                            'url': 'topcomputer.ru',
                            'status': 'actual',
                            'rating': 5,
                            'gradeTotal': 10681,
                            'regionId': 213,
                            'createdAt': '2007-08-21',
                            'returnDeliveryAddress': 'Москва, Гостиничный проезд, дом 4А, строение 1, Белое двухэтажное здание (Медицинский Центр), боковой вход (цокольное помещение), 127106'
                        },
                        'phone': {
                            'number': '+7 (495) 9262641',
                            'sanitizedNumber': '+74959262641',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86QFlVnO7dMxx3vM_Kx_5VBMS9nRbiLiZ1RTuDRhz_tZFNiamAugigG76pBJlk0fC6lfkZMGd8F_x31yOD1hhi3jSxG0J9qFCqfbxbeT174J0I4tSyC9HebsS2Cf6FTWNQ4IoNoeb5Oj6hc99uzRmM96esTxbiWiW7h42v6hJc0S3CcfNiDZJr2Rhx8qaL3tZb0dMIO4855hs677EGfnrYilFHq9S92Rw_x7SySCg-XcD7ylk3FtmL-j9OHMiWrBp4aJl0LrN6AGiu5JN_169dmW2awZyiCHXngBsy6ocZmkph1CS91dXDTxIoUn0wnoYidojA32jCF1kfzKgahaSJqbZjIdlYMt6H1ElDSJ355fwridbWzpQKV2mTpEl6c9D5iBx-BqLSStPbSPeAH3liSWwCLKmoutJarGKApivgp9XvdChyFJfKkjEiLN-x5lRRUMTRGMe6K-3h9hqnZ9w_6GMNsR2R6DaghBdKs2gqvWLsBxCF8bI8wnT3zQHSMHSFdhuTRiRLQCCcy87ABMsCLEK3qRJOYqZZDWBdRkIYSvUB9i8_2rtCNQL_fLKvvaWGWuWe_Uh5istBQhrvvHcHryJQlfIvm9u6G-HRHQI9Q7oZ0sM6eiw2sFyy8ptGE0z_Nk_NEoASGN-TRY3voSAhxT2gddZin4A_A4mnhZjzIzkSrvGZhNqf8SsaYMc7EjxpLX0fe6l5_uGAeTC0ZoRDjnwzW2U7Vj4Mgw1YIpB4ftNSReIXeFcHzKssFJo1KRkIyOMz-N1UrerrKWrpH5JOAA0ODWMw1BOnSZyo0qS0qlDpUo1xcVAvfkL_1ql1eDcq9bBqM1o4LOv5-LHj5GzYSg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_2_65KQet8JGE37EIn2Nt-MX-0MROWFxf4ncabuHBWYwM4rLOlaHUiwHuGD0q818qVR601jWegmMzUxwiZKv3QTOwUXvmHM46ZzIQhZ6WyQte6UKgpfc6vmxtozlWBir5zc-zgWzYbeTc0O-Cn6xpH&b64e=1&sign=4468af133351973ee8280600d2004d2c&keyno=1'
                        },
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 3,
                                    'dayTo': 5,
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 490 руб., возможен самовывоз',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/246786/market_79q_F6kJbjEYrFlh-phYlw/orig',
                                'width': 450,
                                'height': 678
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/246786/market_79q_F6kJbjEYrFlh-phYlw/orig',
                            'width': 450,
                            'height': 678
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/246786/market_79q_F6kJbjEYrFlh-phYlw/190x250',
                                'width': 165,
                                'height': 250
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '495',
                                'number': '926-2641'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '5',
                                    'workingHoursFrom': '9:00',
                                    'workingHoursTill': '21:00'
                                },
                                {
                                    'workingDaysFrom': '6',
                                    'workingDaysTill': '7',
                                    'workingHoursFrom': '10:00',
                                    'workingHoursTill': '21:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.57461397',
                                'latitude': '55.76007516'
                            },
                            'pointId': '193208',
                            'pointName': 'TopComputer.RU',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Дружинниковская',
                            'premiseNumber': '15',
                            'shopId': 5205
                        },
                        'warranty': 1,
                        'description': 'Тип: Комплект постельного белья. Классификация комплекта: полуторный. Размер(ы): 50 x 70 см. Размер(ы): 143 х 215 см. Размер(ы): 145x220 см. Состав ткани: 100% хлопок. Наволочка: 2 шт.. Дополнительно: количество - 1 шт.. Количество предметов в комплекте: 4 шт. Простыня: 1 шт. Тип ткани: Поплин набивной.',
                        'outletCount': 2,
                        'vendor': {
                            'id': 10738845,
                            'link': 'https://market.yandex.ru/brands/10738845?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Sova & Javoronok'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/WMluum70_b6CVMis__iWgg?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=U9xqPzGtajJIKYEGbFTL2VabckbPeDWiKAo1cKL29F6DlDtYU2uoz8xkLuxn7ZIcobeMc9RHpWtDoabeNF5zbj7ibJhQJ_bYjjdu2siDsjczzaEHnIPWh6QlT9PfFF3s&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 10738845
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZH-nTdhFKm4hSlKcvG_wr-HNdfhij_1kvzjiccUb2q-ZBq4CKiShqBZKRLkI0Y2mb4PGl17tD4fjcVdzsK00P_ENxKlE5KoYNoM0MkjziHnvO8mFWNmdIVvN1MqC2T16bMIQqCObaFLYXet4Q1Ao_8o7qUV8U3_lORLT_4LbQRiFrZ100GU6dHKN0suux4gmQIYTNFogml1T027IX0c_jzjmHdLA_7xp-B2xsUH-Dzu53UsAQgtkDhD_2THLTdiZSz4TKBHh3BoPQ5p62mVGr73mDgZy1qjDt4',
                        'wareMd5': 'pguR1_47bjmes1p6k_6H1A',
                        'name': 'Комплект постельного белья 1,5-спальный \'самойловский текстиль. капучино\', с наволочками 50х70 см',
                        'onStock': 0,
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 415176,
                            'name': 'Xlplaza.ru',
                            'shopName': 'Xlplaza.ru',
                            'url': 'xlplaza.ru',
                            'status': 'actual',
                            'rating': 3,
                            'gradeTotal': 88,
                            'regionId': 213,
                            'createdAt': '2017-04-22',
                            'returnDeliveryAddress': 'Москва, Большая Семеновская, дом 40, корпус 1, 107023'
                        },
                        'phone': {
                            'number': '+7(495)7402172',
                            'sanitizedNumber': '+74957402172'
                        },
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': false,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'methods': [
                                {
                                    'serviceId': 99,
                                    'serviceName': 'Собственная служба доставки'
                                }
                            ],
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 1,
                                    'dayTo': 5,
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 600 руб.',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/163900/market_TD1v5VxIwlI3mSm_bCf54w/orig',
                                'width': 200,
                                'height': 170
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/163900/market_TD1v5VxIwlI3mSm_bCf54w/orig',
                            'width': 200,
                            'height': 170
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/163900/market_TD1v5VxIwlI3mSm_bCf54w/190x250',
                                'width': 190,
                                'height': 161
                            }
                        ],
                        'warranty': 1,
                        'description': 'Комплект постельного белья 1,5-спальный \'самойловский текстиль. капучино\', с наволочками 50х70 см Постельное белье \'Самойловский текстиль\' – отличный подарок себе и близким. Качественное, удобное и красивое постельное белье подарит Вам неподдельный комфорт во время сна и отдыха. Поможет изменить интерьер спальни без особых финансовых вложений и затрат. Материал: бязь (100% хлопок). Комплектация: 1 простынь (145х220 см), 1 пододеяльник (145х215 см), 2 наволочки (50х70 см).',
                        'outletCount': 0,
                        'vendor': {
                            'id': 10718078,
                            'site': 'http://samoylovo-textile.ru',
                            'link': 'https://market.yandex.ru/brands/10718078?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Самойловский текстиль'
                        },
                        'categoryId': 12894020,
                        'cpa': 1,
                        'link': 'https://market.yandex.ru/offer/pguR1_47bjmes1p6k_6H1A?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=XPYJt-MlmusUBW-VTEX3Hw917ioLHWkoiouiOnO4-wXvS3r2mwx5K1ECVuJw3eraJdX4vt4C5b27Pgo2lYfLfmCbvuDKLzBwfzFwXqwjyZ8xWo7gdW6I0KpqD4K7hXn25Xm0EcUbsxU%2C&lr=213',
                        'cartLink': 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97BC4mr88tmrA3S4sJYzmw4isyoS6PEOS55wO1feIc8tMT7MJz43s1-6lBuwtTjYDcVEEHHvJQ5B9nhvVrvrt9RYh85g8uwTeQjXTjZG0fsmSoniL6nyLf3-sgmWYZd2ukEuIJSikFGB1bsUD4yOlwu2-w5geOFiPQVTLjt66MtiUa7e-UEpz4R3Vt_x0yoEWt4-LL_qtroFfEj8zMBPEH0JwPUBhZ55NIQgdtRIL3Ge9TR76PQUrHEbAixPNPzrkqRYfkY3MO7b3jEWX8fw3xAEiiFAISErCtrCQDs3ax576TGOCbVV1OmVHkJh5d9Oc14mKA_8FboV8-7AAYHFq_ndIWW2kan6lPcTG_kQ49zET_ay6NVWsmW1RtSXypD4M2WpCBGj9oCxakxXYvn9MqxWH72YM5lUNyw33uwaDHniasBA8wRaelwkgHdUkZphN63SKCDBtO0yaPZHtHCpuF6572wE628tJSAkyCbuqye_brvaXILJ2Pu-9_9Fz63Me5hVWVA6egi61ne4nqxZe20x1xGJ85yOAANt9b4aFiIFvnUT-C3FB8MSxtx7kde9EoPkXmUEh_WJ90aOQOU4mIed0HLIAqk_nd0ECrzNDPnyEWv35pt8aVDUg-vhY1Ya3mBqxniwPZgwOwcfmE-ICRYU9wNtwJmoi0PHedWEuXTVuNyt4mn4CvR63qeNNB7f5ogV0tja1Ile3?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXzjqrdVr_OgOKNi9F8doNw3JiPfc45SMPPHi2u2N2lO35u1qWB1L8meseCTBMAVrINJyQACeLJwnEXq6CveSzXKrqAOzhNfCsH1owavKque3j2qsSZxkUUNmEYIRNsutKa9vyxMwAXnBPzdVEFtnWxbKHYlY_H6yWNU4RgaosB73Ps7Y_yzg9i98P0FGbHZanaXKbAuInXA0lZ59PAq1eViUgqNaaYT0if8oi1V1AlbvTkQLD96vFM7I_WNl3X60agen_lQfueCnFS6Cn3-IShB2IPvWMqeKTJdIg53ShR_kBjlXmCbZtpQ,,&b64e=1&sign=e4f9c81c78952f664c857bd4a4a4158f&keyno=1',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 10718078
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZERoRiNuGA2AfQUFgeR1jWuh9v4487SMphrNP69kKZ5kdSj0dyEDC_jq2k7H8-3_O8TEBVqjBTZudNiVH-H_xhp7me0uIr_JwK1qpeiYSxdv3qsIOGQxUSJG_9PIZciHItolXY5v_lLvuyErlX591NFsosw3zCsfnT6fES0jKeVQjA6nCZgRAddHnHh0Ky28p0TYq5HwM10BhZ5Kr2-USQ4L2XFyWMGJqeuZCErrsKuQSEIyVP69e8LqVoeqLTJ8V8hazi2hneahRW3E_eDfi6iy-TzZAJCYx8',
                        'wareMd5': 'vuxmYbhFfbCJlCxZe5YYZA',
                        'name': 'Комплект постельного белья «Коллекция», 1.5-спальный, поплин, наволочки 50 х 70 (2шт)',
                        'onStock': 0,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mUM4HZAZE8nwWYsP11N3iFlTDMW-U8tpnXq0C_w3zbDitvWqdC7Hkq30ioPYBhIiGd8tFSlg_t7Purpx6C3DCkHVLMiiP1ha-J5HjE4rZqG7n2vDpy-V_SWQ-WNYirJnpLmfxmwhnvK1bLcGqf-aokX0x24hfpRgyEwXlsTpzIUxfWMNf4J-jZPzWR417wtUWs74vv5-CYgH4tkH-BVHs0AWVnSTgut-fFCJxeczScE2P5ro9cEtLc0m34O1quCdI2eBnhQKpeWJNSWezEYVzWAhWkrvAS5pduau8n-DrRjwkpcjgW62kHosRhXIwhY1Q9yk5ssHbUAc2ZDn8mgelKJD3HJKpK0ICWG8CSANbDLZ3uu-BWMn397QWfZD6Tf82U0RCMG-5IBGo6f8H2KtJ5mMHNJo-VwGncvmcz-IH6Gnq5j3KMkOS7zSrcObiBiQG5UVDLhaniUkWbCgRiujOSWIk3QeFvnc94XUjxGoMO60tllwWxzzfh5puKD94H3HFHPMvFEBUsuEcm7KJNlrEmPez3KLgPW97sXGOSck2Mp7Uf2g7u5RBcoBRigtfdv_sSBiHnjyeb8jTDtByW63Dr8lkMUxtBnqIsgoda_NqARFyU_iVl0T4kKF3vZOIrb1nL4I3SFHqOpOExLA8Uk1-_h4mNucSAXZ_Gq2KpKFh8rpq3fv5xRlPezT25AzkRvzQalsvIBCCDR9VL3VTF9cuha48J8FBnbrds7kgzzqPiYti8-Yk_8-pKb_Cty-49Ck0Wd3mlH8MKhReWG3S-fst_QS1tb4L8VcsL7YCe4bQ3YvhmFbMbw7kDwXwTcBBURAC-E0ZXDpGttKOjRHkgFE91xdUMcpachP_w,,?data=QVyKqSPyGQwwaFPWqjjgNjRJCxuqRWIErIjtKT9XSwIhEfnzMLUa-nr81VHmG6ViirCVM06UKWfNAO17ywxGHYAbqFcPaekr_FSv6qppoixpSACPhs9JB3efij2GE0VwY4twhh8amUqx3kovcJCnK2XTsj-_DqnhRwUR0udfBmKd_Kr5R2Nw6HeXZvkl3tpCYLbSvHQ6zA7GHOeoW8NHTYNOs1Xu1xnZd2RpF7CYaQew4sdBtlBaGj9oXGQu8pP0mjY9diOgLGa90PhYkLrMWUoozNA2x2TrNC4PYwXKvyWtgPfQJo_joqpZBXKc26z7z8LC8oosi5gQzIszEd1-3d7BXbjXbtiuxVsRZ-YAzOvyRWWgAXYlMMPJdUayBXswNXSAFh9jcqU,&b64e=1&sign=635258655061f29adfe17f0658b5ce83&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 175488,
                            'name': 'Ашан',
                            'shopName': 'Ашан',
                            'url': 'www.auchan.ru',
                            'status': 'actual',
                            'rating': 1,
                            'gradeTotal': 173,
                            'regionId': 213,
                            'createdAt': '2013-09-24'
                        },
                        'phone': {
                            'number': '8 800 700-58-00',
                            'sanitizedNumber': '88007005800',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mUM4HZAZE8nwWYsP11N3iFlTDMW-U8tpnXq0C_w3zbDitvWqdC7Hkq30ioPYBhIiGd8tFSlg_t7Purpx6C3DCkHVLMiiP1ha-J5HjE4rZqG7n2vDpy-V_SWQ-WNYirJnpLmfxmwhnvK1bLcGqf-aokX0x24hfpRgyEwXlsTpzIUxfWMNf4J-jZMC4HA8i2ZM6bNkT3HBra-R_ng8KgVoZqTVnY4XPeHPw0ypjtb81F0GiDGHw8XGec-s8d1bajCdjnEPu5F9x6X7QcC4QUUwnNuC6gXpkslOPcoxI-g7IHa7Ai9vG2IiDTiUGFHLt02gcAYNJpFZi_21K04SHbw-k4q-hCM11Phzv6M3kAelAhnJ5Yp9TDAc9xjP3J7AEa03WJ2hvGyxf2TrDbOEUd0HGw_wP5QbFPAn36jtq55nly0BbqfhJ3G0L2ZYaQVEfors-6-jI3bZcefKCveeAeknd9FVlZvaR95KlUqwyqhoxjnE7HgpYfuNLA4hHQM1D4h-xj0hwSjk-S9DfeLqn56C53Gf_zhL2cKEIh92mB6x0kdz5-L4sLqZWbYggEjBI9hW8czLpet3eeOb82bq4aKMp3YR_17hVp7Jew2yZK11636iSSkaajDGpYjUBGsRxJczuzwDKZglSn1xyltLZar5yzH8uYC0ntSmDr0HpDi2l_Mhwuty7O347MScqNBLGsyc1aHqYeN6Y0MVyhudGLxsvNMY75PiAe3Yue38GhYHqUTJP2rCLB-rTDVusMYCmfAGUl1X2vn-QsX08Nu8kate74oprupyZVCqEa62O9Sy3m67Ja6hWLRtEhWT48DUAeG6lWNe7-27f6v1WWPFQStBoKtYKIqDYaO2rw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_vw2KYBFNoaA-3lG0zDlMqFNzcEPshIpK33X7MjoJfhaeT7btRXtX25s2V3_2PtkHU58JEa9QSdrwtgOMKPFh-4ucp8x8NkJrP-zP7hpKmpC-ELUFTDYHFDoHo2m2-Khr6ucaBYL4XmcjUkqkr25yx&b64e=1&sign=63d80ad5410f6410d3f9227af2716d60&keyno=1'
                        },
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 3,
                                    'dayTo': 3,
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 249 руб., возможен самовывоз',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/218908/market__vfllzI5wnQU8HFOuNmnVA/orig',
                                'width': 788,
                                'height': 630
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/218908/market__vfllzI5wnQU8HFOuNmnVA/orig',
                            'width': 788,
                            'height': 630
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/218908/market__vfllzI5wnQU8HFOuNmnVA/190x250',
                                'width': 190,
                                'height': 151
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '800',
                                'number': '700-5800'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '7',
                                    'workingHoursFrom': '8:30',
                                    'workingHoursTill': '23:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.592096',
                                'latitude': '55.706883'
                            },
                            'pointId': '411207',
                            'pointName': 'Ашан ( ТРЦ \'Гагаринский\')',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Вавилова',
                            'premiseNumber': '3',
                            'shopId': 175488
                        },
                        'warranty': 0,
                        'description': 'КПБ из Поплина, прочная ткань, которая обеспечивает долгий срок службы белья – его можно стирать множество раз, не боясь повредить или деформировать, приятный на ощупь материал, согревающий в холода и дарящий прохладу летом, устойчивость к внешним воздействиям – белье почти не мнется, его цвет не меркнет с течением времени, сохраняя эстетичный внешний вид в течение всего срока использования. Реальный цвет может отличаться от представленного на сайте, ввиду различных настроек монитора.',
                        'outletCount': 18,
                        'vendor': {
                            'id': 13083016,
                            'link': 'https://market.yandex.ru/brands/13083016?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Коллекция'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/vuxmYbhFfbCJlCxZe5YYZA?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=s7bfB431B66P1z-AVsB3wHiTwphexKUhjhzvD9rq59Cu74A_BCwR5t8m-OOJste2-L_4JlM8LBnWOsECw_TwAe6Z7-XaaMf-bNx7QUyPJerG-NmDh9aA5s8and1gxR3r&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 13083016
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZGeYdp_nL1MRG9CV5rMv1xkjValOc-d6Jde794ddEAEfTp06Zt2jL5wjyRk1GPPHJAT8AaDpmqzNwnB7sbKa2Iuhg6JHSp67-92k0kk_GmBUNohIGG8F1HSh9nYJIFbeY8kvoIucSgOZGZcICNwmGnrnhDj6QDhv2OeS9J6Nvfaq7nQfG6pY9hhQNauM9_0mUqtSE2nT43NvEhkdX_iNuFnWCtpH_z0vGWdJG-Vwz739KOeiJVX3tV_4jbmJpr7mOk8S3XrLKq_HPBDGgY5WfU_sIqCPbGGQ4A',
                        'wareMd5': 'e2rFubwobBPEV4m2ZXzhlA',
                        'name': 'Комплект постельного белья 1,5-спальный \'самойловский текстиль. этюд\', с наволочками 50х70 см',
                        'onStock': 0,
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 415176,
                            'name': 'Xlplaza.ru',
                            'shopName': 'Xlplaza.ru',
                            'url': 'xlplaza.ru',
                            'status': 'actual',
                            'rating': 3,
                            'gradeTotal': 88,
                            'regionId': 213,
                            'createdAt': '2017-04-22',
                            'returnDeliveryAddress': 'Москва, Большая Семеновская, дом 40, корпус 1, 107023'
                        },
                        'phone': {
                            'number': '+7(495)7402172',
                            'sanitizedNumber': '+74957402172'
                        },
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': false,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'methods': [
                                {
                                    'serviceId': 99,
                                    'serviceName': 'Собственная служба доставки'
                                }
                            ],
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 1,
                                    'dayTo': 5,
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 600 руб.',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/169660/market_896gcRzAFqX8cdyq2yjOBw/orig',
                                'width': 200,
                                'height': 170
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/169660/market_896gcRzAFqX8cdyq2yjOBw/orig',
                            'width': 200,
                            'height': 170
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/169660/market_896gcRzAFqX8cdyq2yjOBw/190x250',
                                'width': 190,
                                'height': 161
                            }
                        ],
                        'warranty': 1,
                        'description': 'Комплект постельного белья 1,5-спальный \'самойловский текстиль. этюд\', с наволочками 50х70 см Постельное белье \'Самойловский текстиль\' – отличный подарок себе и близким. Качественное, удобное и красивое постельное белье подарит Вам неподдельный комфорт во время сна и отдыха. Поможет изменить интерьер спальни без особых финансовых вложений и затрат. Материал: бязь (100% хлопок). Комплектация: 1 простынь (145х220 см), 1 пододеяльник (145х215 см), 2 наволочки (50х70 см).',
                        'outletCount': 0,
                        'vendor': {
                            'id': 10718078,
                            'site': 'http://samoylovo-textile.ru',
                            'link': 'https://market.yandex.ru/brands/10718078?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Самойловский текстиль'
                        },
                        'categoryId': 12894020,
                        'cpa': 1,
                        'link': 'https://market.yandex.ru/offer/e2rFubwobBPEV4m2ZXzhlA?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=XPYJt-MlmuuA2l07_oZoHdi-jpZQZj8oKJ4yeAP8nMS6CGclcFz7vxCgaemDLW3rF1MO-gnKD2wYkz_5quMwebqCSpZEf_NgWSUcaZZwEg-pfGrYIRmAePLB6moLZjSIbiHTJxdYofQ%2C&lr=213',
                        'cartLink': 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97BC4mr88tmrA3S4sJYzmw4i56KeTkEbaL8WriHcktOYv51lBeUrPxFkbeZrRCqvJfpkTRk2QVg-QpChZcf5-kspjUBlI8NINJey-j1JbeYShSahxhJ2rLsDySoBj-mSZxwl5G2bSeMBgMcdGqOYbqJAWIsRufYKdRW8hkseck8JU-BWnhCg53296rL0O_ph3iekVkqtQgiZ78VHv9G1dHOX_yOF-njiK66JeevE2wC2tX1CVdkleNS0FJtEVrdkZ2H-wDLM_LtsJrkTTDWYlpmnNefFtNuGH166CD0IKH8cZTeMw9-KGM9coFQ6FQvoiOLUyUZYXLcfXBZkyU9fSCC_NI5eXPfScQ_m36xScFR9FFPUtlplflvKBUQRsSWUpejuPx3LaXTQsv1hhrV_WOi-J8H4le5qcWg7rEnnnXT4AeM8xbs2LU8g1PcJowKmv09tHX7XcqAWnyr-NC_cO_gRJBsMKqf0tK3uw18MWvOW5z-TQfKyZfC_vNJd2r1ilPSrk4L-wsc_tMY9aWKEj8qLry6EUaYghzAH2ZfmywaAGo_CWZ6iFjIS08SJ7nLCLNXpkCeG7xvZmm6X4XqNY5mKsNjABhaZ4eK41zR3j-0ZdSlZHAgG6CECfV19w9gVqSCk-aqmFDlVfFXF_M9dknO77VDxG_mP8uXcBsS-hDxsiyrnfqqUoQKlDboKMhA4m95XwmtBr8pJ3?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXzjqrdVr_OgOKNi9F8doNw3JiPfc45SMPPHi2u2N2lO3FAkf3mO6u7iPOXh_ryPpuYItl9Zdbg91nBKsQKT0EUSS9hySqoZjsbaAr3Nkx-EcvL015W0hjSk4jGaitbNc6emazoa1ODCSsOarFYGlyAyAjhyGk4SeCJm7nV4G5Kk4wPQ5f_Mf1C1mJqaqqGVK_UjhEo2Mk2N0Be8DFxilt0u1-dy0aoqXh3QsjHtGWWXr3mlomuxANf1e3bQBInljVedm9Mqzr1aCf2NFT28-6fGrD92xR48JIUfXMGiajfTrhN6N_uZVpvg,,&b64e=1&sign=2dc5440e2d007d07cd6170545ee5e036&keyno=1',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 10718078
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZF8BQPVLGkdtRB7GS3sDekgE_V2Or_g2q4YOfKRC11VJA',
                        'wareMd5': '9PiLQsvKVmEFDk3pFussFA',
                        'name': 'Комплект постельного белья Quelle Tete-a-Tete 1011045 1,5сп, 50х70*2',
                        'onStock': 0,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYlD7it9T5OfI7AmsNd7bI_uJK-Bl6_zgxPad62y6UyH3HUluxp1WGuSUf98_dml1oTBwZoKD7CA0WX5iCH8ClCOGd_t4xXCF-gpN8INnmMyhkXi2jS-GQDEZOUnD23v214X6lFuwnGttfmBTPoEO3NbK8vwn1QSGsTHf7a94bGskCnEGpayTpUd8NS0GzADKwwjZL3jOB1S6CZ8qNxx8ir6xrwWoTldnaNjKqi3UOskt0uwznhk8btlw7aVW01T0udBDCrOdWU6ch9CTUW76fM4VVpvY0jlQAJofoTaNYqBTTyPoleEPkhVt_nUZ3_91vtOqGm-zY6_gNiZqdldxiCAkc-gHaZMxoDqCxVE02nmUHMUwvlwrgMbg8w3w7MTBGQyaUfCXYat5vbn1BE3DnJc4VM5-MSDdaBtc5xv0U9PZ7GXlrqXIVOLsonURUgPMB_ECPtVTGs9km5XC-BDt4iiIMYyZoEK10Hv0KJvxPATkZM0ZjEBQ--MoXH4pc8bMteSRPxXUxh1-Yy6ul50dmtLVMv06U6_OBs736PEAQz2HhvuUoR2gcBcoTbDioq-RXmivq9fVk4naBYcWo6nJgfHB4-xnVddA7dLM68UfQse2e-Z6MLOdMHMK9ldTEJek0Dfo_8fRB-_7forRvK8b2wk-3UatZqsAdvqfTGrlaCDKfrqoYaZYWu00jNNducg2FYolFd9Hdoa-WHsMEtq2dqBMJBYW_XB7QCgiTRVXHxN0DDVINQ4qx2pJFTp0gnN2Vbhwx4zmtyEhZbBVWWRpEd0MLDmUejioCK3ZoVDntKNy2TjnM8UUjI2jgtgguTWlpUS6WYXb1E63O2W2H5TLq_59R_wS6__46UnGuwt-zEB?data=QVyKqSPyGQwNvdoowNEPjcIHFlOTuUeNy19gJ3SCFk6BgC6z7UWuWOcdSItWDkEjXB6wgFch1fxTnWJgI3f4pBXp4sGoV7OKrlkdDbN8HkIX8LhBHJ27bvOlq-gPXVsBu4L_qVVBeEQRyhiXs7z__xx1G1atCPtKZjik4yGVTwG7GLVgxY1wOzsUemkX_Aml6Q4Exm2pZSBzUekcZ8VEMr-OR8iK_izqBZrKNz_LlXpH3KQnsglPot-adNwQh9wRL1VHmUZOqsL7V7MCCI5AEFbewIdrzn6-P8a65iy_LgZLaPOh9z5URF41ETazrboxOc7AA55tanESVIn4BC7oxDYJUhzNzfje-Dh_jd7t77F40eCY43rNM1cyhdVS500r_bszg96Rhr_DdL43lhDQnA,,&b64e=1&sign=d846a98a4dfe6efbab035bb38b63fa5b&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR',
                            'discount': '20',
                            'base': '3899'
                        },
                        'shopInfo': {
                            'id': 105713,
                            'name': 'QUELLE',
                            'shopName': 'QUELLE',
                            'url': 'quelle.ru',
                            'status': 'actual',
                            'rating': 5,
                            'gradeTotal': 8734,
                            'regionId': 213,
                            'createdAt': '2012-05-25'
                        },
                        'phone': {
                            'number': '+7 495 995-55-77',
                            'sanitizedNumber': '+74959955577',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYlD7it9T5OfI7AmsNd7bI_uJK-Bl6_zgxPad62y6UyH3HUluxp1WGuSUf98_dml1oTBwZoKD7CA0WX5iCH8ClCOGd_t4xXCF-gpN8INnmMyhkXi2jS-GQDEZOUnD23v214X6lFuwnGttfmBTPoEO3NbK8vwn1QSGsTHf7a94bGskCnEGpayTpW0YALWGmEtk8-cRhrPaqP3q9ACcPTyeNAoi8naMujSw77ADX8dQ__Q3dt_Cyx30yaWlQ57VIIDL_rarYdYLEbVk6d8e35-dkQJogAcRuq0BVROZmULEVQIIENq9mUbIci8VQ0OwqprLDbMEExnoxmxR3-1DVxLx5rL7J5PFOW9qxWT8hrqwBt2dZeioSq2IAKH734b7fmAw9SFgWszHNORlWEgHFhkWbhIJHVFa3Ex3ZoENIoJ4oc-HH0u8s-2cbBDUIDHrI18gkNq2l3npdtuI8H6N2UFC2_1qhWsO-tyHL1Sum0x4dddePG41JkAoe_cmRVzcFE_JqF7lrguZLtViJ73CN9TNm9Rdu0xK0fWey7MfKIyVl5RyedP3yfOibZ5SMrQoG9xl5ScLvLO_IUigM5daJJq5bCF6SjxHcKnKpT2s8cKXynaPYJzfIY1XWGnfepZ52Hgx_mKKnL2b5O-e66D-Gr4nRrv08hSwJBjB3zgbJ7MV-9JiT7XLk2nUYY1JWha4nrFztvDnPLTKYq1TWYXVTLOH9JOp5Dnh749Y3HYChOalWnFVRNfruotAO8J4YxtIw0DpVIn1BS8adBD50Q1Lo3SMSrfQ48bV8BCQWhwJCUQd9Wn4FJiDcRc_qRifnPHJyxkbeGZ3EGnhkbGX2Uf5277CZjCrod_9VYop-6ZWKSGGxH-?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8YL2sUbCz5_yCYsupgvINIafzWcV0e0ZXrKqmznhwVTBFB4jvGt7NMj4qPTDVZy_bvrO7HRbUzbVaoLrPvzwRDB2aMFJ9iWQoOdjGV9E1ZmegIPc7aAfKehPaKzHb2Bgo-a-iQy4FIholazT3hq5wF&b64e=1&sign=b5e448ff7ad50ff21bc6fa0056a3164b&keyno=1'
                        },
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 479 руб., возможен самовывоз',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/365133/market_P9Tc1dJmW8eixluvUjRffg/orig',
                                'width': 450,
                                'height': 640
                            },
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/203248/market_oSvFCwm-LcZqw3HlbzhziQ/orig',
                                'width': 450,
                                'height': 640
                            },
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/205271/market_rY3QAIlrjpueOPStRve5BA/orig',
                                'width': 450,
                                'height': 640
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/365133/market_P9Tc1dJmW8eixluvUjRffg/orig',
                            'width': 450,
                            'height': 640
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/365133/market_P9Tc1dJmW8eixluvUjRffg/190x250',
                                'width': 175,
                                'height': 250
                            },
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/203248/market_oSvFCwm-LcZqw3HlbzhziQ/190x250',
                                'width': 175,
                                'height': 250
                            },
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/205271/market_rY3QAIlrjpueOPStRve5BA/190x250',
                                'width': 175,
                                'height': 250
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '495',
                                'number': '995-5577'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '7',
                                    'workingHoursFrom': '9:00',
                                    'workingHoursTill': '21:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.659775',
                                'latitude': '55.775454'
                            },
                            'pointId': '405467',
                            'pointName': 'Пункт выдачи заказов Hermes-DPD',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Комсомольская пл',
                            'premiseNumber': '6',
                            'shopId': 105713,
                            'building': '1'
                        },
                        'warranty': 0,
                        'description': 'Комплект постельного белья выполнен из мягкого хлопка с приятной шелковистой фактурой и декорирован элегантным рисунком. Наволочки с отельной застежкой. Размеры комплектов: 1,5-спальный: пододеяльник (150х215 см), простыня (160х215 см), наволочка (50х70 см) - 2шт. 2-спальный: пододеяльник (175х215 см), простыня (200х220 см), наволочка (50х70 см) - 2шт. евро: пододеяльник (200х215 см), простыня (220х220 см), наволочка (50х70 см) - 2шт.',
                        'outletCount': 1,
                        'vendor': {
                            'id': 8466802,
                            'site': 'http://quelle.ru',
                            'picture': 'https://avatars.mds.yandex.net/get-mpic/175985/img_id2975311022267417503/orig',
                            'link': 'https://market.yandex.ru/brands/8466802?pp=1002&clid=2210590&distr_type=4',
                            'name': 'QUELLE'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/9PiLQsvKVmEFDk3pFussFA?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=IPSktU8qIwVBL97wC98Gdyw05xSkNzjOn2ozSOOLuQ7m0TLLifAhCdHaC_68CGXYfHbqoVVqmaFR239GxiOR9U0L6T7Pr0gkD3007g3zhJUnkYwPacNuTSnDWfi6Q11Z&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 8466802
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZHGrNb2g8NcFtgO8zQdHRLPWdMptMgRs-xi4Q5yQkT5Kw',
                        'wareMd5': 'FcBO4cMO3ko4YbrDi8cXMA',
                        'name': 'Комплект постельного белья Sova-&-Javoronok Барбарис, 1,5 спальный (50х70 см), поплин',
                        'onStock': 1,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZ4H9_aAm-SdP69geEvrHAW-blXpEpbP7mMvSiqE0wHXNcbjFOAauooLfzyCp20WqsuZAHmxkCoODEykLWmE9JXtRLbu3OrhL3snBTAHOvO6ZoeviqvkNa-cDYVyzqD23EoH7epndmBWJVVh1Y301EBerTx4cn-cVlW8lrsl29RxOYFP-YOcJoiv6QXSO0NrCJHyrKD7dP7XsUyYzrxYOQddDP8LGQ8eYz5HBG0zs7WGGLPoiPRUNvQy63LAaSVoUFy7rRgZUA69rc8iEYnb-SvwRHDS2eYiINhVB385a4Ujgsf9dmsmQWUQBonfizZv2w5HHTri6UO18QnnDP_KZ2_8tK_40Mze_6eKLAVUu1m9YApMSTX7iFpX5sg_B09Xp3HoHKd27vOsL600ZeYgD0h-5k5ZPAwIK0hV_Y-LCoG6nPj3FvNCN-hMs4VX9zqSNty85LQl1sFxhqpVs6FkDLWk7SXvMPgxNiSqf4yqwgPjgGFs1mHu-D7EozGz18ZDr8Z4ExXE6Dr3UXP9SwAOolcgON-MvpW3Zkhe6dUpecIDOHTCZ2-CEJ3pS3_jKSojfT0KurEN56SRPBXSmhrvTE08lBtxzwM6QnLJwsRMdxPpnGgiWS2HfrGiKbUp56meEFf7PJydJ2o12oa7l_LI7tmgh_TGQiXyx1ZzjXiafaWuZX-sDavCNTPVJADKMwrKZiLnlfLOkTXZtuvwQsKh7LXSo2dlhC19qihgddpfXQ60lfN7l36yJy1ICWIm1jHpAQHW1Rn_mneP-Y0GasI10E36L5zE0EjdoYdG07TfLYyktAQeHjvkpQQ9E2tnaW6mRHNdKZhTKffcubbEOdhczTrJbEi88Ppp9g,,?data=QVyKqSPyGQwwaFPWqjjgNtwFDITzDmMAdbMKRCiaGZLbhcbaDUDzUfB-qw-IwcM2sLKjRtlOH-KEmOFVoEP6txawBWUNiccvYTaSrhpIPgJ2N8cxBht4RxBD6ye98igpeQllNel28h4CF-vfXxFwebqNPFB_6AaZ-jhAjZeEwolhddFvBv-4uXAvLdpEYevcJBgxbnasgsCvf867KnNeKR31jbj0aanlxCHtX5rpbbHMQodayqyvxbzpn0ySkvRPZ51aw9X8Kqwcrp3F0YqjLd9RLVCasayO-gHhPaVNkff6IQ16uIUAgO-lVvgMuoExJBkO_pAYKAJhCtf4BgSt0_Ks2EOsJWkzVFcmBHHLxJDqK6iKlopCMQj1NishgU0DfJh0M6_tddd50qT35YdgXDLXrzZ9mVer&b64e=1&sign=052c2d1cb46559c75627fc3874c68d2f&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 25017,
                            'name': 'CompYou',
                            'shopName': 'CompYou',
                            'url': 'compyou.ru',
                            'status': 'actual',
                            'rating': 5,
                            'gradeTotal': 4577,
                            'regionId': 213,
                            'createdAt': '2009-07-17',
                            'returnDeliveryAddress': 'Москва, ул. Трубная, дом 25, строение 2, Прием заявлений только в личном присутствии с паспортом, вход с обратной стороны здания, 127051'
                        },
                        'phone': {},
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 1,
                                    'dayTo': 2,
                                    'orderBefore': '20',
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 350 руб., возможен самовывоз',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/238524/market_4cquDMl4A26EObgQYr9djw/orig',
                                'width': 500,
                                'height': 375
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/238524/market_4cquDMl4A26EObgQYr9djw/orig',
                            'width': 500,
                            'height': 375
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/238524/market_4cquDMl4A26EObgQYr9djw/190x250',
                                'width': 190,
                                'height': 142
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '495',
                                'number': '565-3485'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '7',
                                    'workingHoursFrom': '10:00',
                                    'workingHoursTill': '21:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.6235673',
                                'latitude': '55.7715595'
                            },
                            'pointId': '321317',
                            'pointName': 'Пункт выдачи CompYou.ru на м. Трубная / Цветной бульвар',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Трубная',
                            'premiseNumber': '25',
                            'shopId': 25017,
                            'building': '2'
                        },
                        'warranty': 1,
                        'description': 'Тип: Комплект постельного белья; Классификация комплекта: 1,5-спальный; Состав комплекта: 1 пододеяльник, 1 простыня, 2 наволочки; Состав ткани: 100% хлопок; Тип ткани: Поплин набивной; Количество предметов в комплекте: 4 шт; Размер(ы): 145x220 см; Размер(ы): 50 x 70 см; Размер(ы): 143 х 215 см',
                        'outletCount': 1,
                        'vendor': {
                            'id': 10738845,
                            'link': 'https://market.yandex.ru/brands/10738845?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Sova & Javoronok'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/FcBO4cMO3ko4YbrDi8cXMA?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=U9xqPzGtajINHJJ75ixnEfcfv7f7Iur2pDGUMribshWb0dBUV4KpObpKiUCabdZT6LfmdmYTXDVIzbm_KqRJBBgBS8AUOWc2OVxBJXJ6M2V1pKAjwqXmAPU1dtn9pv6W&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 10738845
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZEgVKPKYpVobuDAr0Ncq3aT3T9e_VpG3SQ7zDlABINg5w',
                        'wareMd5': 'ilC_DJusXfEyvF4c-uuzGQ',
                        'name': 'Комплект постельного белья Sova & Javoronok Зеленый чай 1,5 спальное 50-70',
                        'onStock': 0,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86QFlVnO7dMxx3vM_Kx_5VBMS9nRbiLiZ1RTuDRhz_tZFNiamAugigG76pBJlk0fC6lfkZMGd8F_x31yOD1hhi3jSxG0J9qFCqfbxbeT174J0I4tSyC9HebsS2Cf6FTWNQ4IoNoeb5Oj6hc99uzRmM96esTxbiWiW7h42v6hJc0S15vbYZ5ZC1QBWEXzDau7a6D29MfESIsm2TKrVgO9dHSLGM_mMKeqMRCGKEQNI23qJaxPCwMncMP8GNSAnGToFzDqTfwUs-TCFxRprcnkZRu-kohvXGv4u2yQ72x97ZIVxi6Puo1NjXmZNemsrfRS-ttrDLx-sHVirGKqnF0Tp0qN10aTNpCBdGxE9TNZGpIGbKwv9QYg4RUliTpExLAfcZEBxrczLMlST2iJfSC5k1bfoQk_ytINKCxK51Anmnr383fMxiGjI-q_7agputTtIWqm9Mmj2E1GS-dI2YaGc92YIRNnEPIbKKdKmBGbdPl8a4XelKa7HFrj53-0oxouiuvCl6OXnljxY_OMJezR9zlyrqVQMsKHYfipfMA2z5HK8CCz2Nntd1Hfo7Cp8ayFmzRHTI9iaNKKODo9OBOtA1ugHA1tgPgyabH9W-cKLveDrsmqqkK55EIksJ2W5xguHFk--85NU7KDV-QneacNecEJH8tgH1u4oyQ4HUVGvlz2e-gQ62k5vZghMNyNd7TjahcLaq_EyaobKcC5iq7D6aW5k87rbPurj0VqMIX0D6mNJAFj9CPpJ3e5mdxbmgAei7BKhpSv2IQkD2vJTsmhkXI91XJMEGukfIgAi8w6QJ_V-gox4wK0ur_zjWnubjh2ffy6GIt7llWTdxhACI0PAd8Q,,?data=QVyKqSPyGQwNvdoowNEPjbv5NDGkDw-huUpGHotaEKMse75hWj8mPAYGATnpK2NOjzy8yvOm6LAJnw_xx7RieSSWu69sqAp5lz6XUjHkhcBWb5u0qr5fI1Kx-V1wlO02VW2EiR5CVeTqjCz51mx0xv9EfPmb_oUNTxijywtrzF6hHwajXdvvbsmclomD_RvPtGzXOkWO61ousCggo572QzYmKnkuk0wJOmYMbqzIrQYXTQRYk2O9XVYAp3nY4kUhXzkqBP6BZZehXE_JYEprVB97-T7JtKMIzwfgq_5wREdpDVYBlS9rIIskjgu32BdqEOsYiTJgtouWZCAE17X9F0FU1yWFujy1zy97zvwRxQDfFeo3iuxUuLawRbtMxFRjP9BWooZVMsP8EFvY2e8ijH901KNxPeECNmtwRxJi2rJ1qsBnUHVpjK0Uf-maQizjd6u_xEJKJsnDIwIH0EaviC_Cpsz0F5nJNZpetMuTLI6U1HDjnAFHU0QpHdgd0lOz3sy0jZEkS-45YAOi-dEy2XC6bzUygz_uMt1k8B_Rd84bDe2MOEv0BY7eZccx1Wj0vxzdRGagsq0,&b64e=1&sign=c3d645532a4e57025500e2a6bd87b263&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 5205,
                            'name': 'TopComputer.RU',
                            'shopName': 'TopComputer.RU',
                            'url': 'topcomputer.ru',
                            'status': 'actual',
                            'rating': 5,
                            'gradeTotal': 10681,
                            'regionId': 213,
                            'createdAt': '2007-08-21',
                            'returnDeliveryAddress': 'Москва, Гостиничный проезд, дом 4А, строение 1, Белое двухэтажное здание (Медицинский Центр), боковой вход (цокольное помещение), 127106'
                        },
                        'phone': {
                            'number': '+7 (495) 9262641',
                            'sanitizedNumber': '+74959262641',
                            'call': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86QFlVnO7dMxx3vM_Kx_5VBMS9nRbiLiZ1RTuDRhz_tZFNiamAugigG76pBJlk0fC6lfkZMGd8F_x31yOD1hhi3jSxG0J9qFCqfbxbeT174J0I4tSyC9HebsS2Cf6FTWNQ4IoNoeb5Oj6hc99uzRmM96esTxbiWiW7h42v6hJc0S1HC3d-v8wZq7relHpmXWddJcXvxkXk_zJ2gqXuuAH_el5MOExX97q6l4cTRJEdApfsR_ghEhYB59iEpI--L_9PurcGnpk_ApgbvbgvV_nVyKg8kObtC_WhBpzam2xuMIRIiMlHmyc9AMd0BjRVtkdI7zIcGTWUNoFG42yqJsU2pIMD0HvvrdWnpAonxE6zqOfVxFsHOI8NJigOsey_p5Sa2z68rL-1xgBPhpgbDC-IUpYJdwUi-A7RkfpTpxXtuiK4W3G1yiagfaTfeJnsK4u34xTVSY88WzwbEas8KSJ8SK3G35SH3y6yI84PLQhxKEyPwQ2Reo6JURCLyUDqwvP28PGaTSrJkFIrmDcsLlTgB2FvwzEBJwIwEtvaKRp0aslLKk3fvskNbIRdyWVEShILZ5UwKkqPMybMP5Fs6E1tjrT61ALCitmSJM3ZaCGTnr6eXcDdfRjXyvTYp_pH2HA05U3cIhkaNud-Up4snPOzYNtySI8I5TA500E8Cnfs_MOmdu7R7BSsDaB5-t6IleTzizlJNkOO1gE7FNi8ot7MQhPbQMp4YVuC9IIABGgUvlz6yY0NSPQsorgf5Uhke34hUdWXAZLHV8EZM5RDOg3XSy3rQp4z2UruOSt9Cka6eaCPyRd3In3F_xMU1AYYGgEyGxP94ynW1OwflIjh6suyCA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_0gibxLINa5GLB6OWRcIBR9wtdSLE6xhwf5gWoXw0hQv02bEXlxAVKvgUX5e_4ZNqoQh79xeYlTB7NFNeCj-3A4nCI-GEiuRcqtNWPd1NmhFftlfe2aHmV8vlvUr3n0rjX6jqcq8NhuImgfo0cn6JE&b64e=1&sign=8cf77c380aa7154aded78007a0fd0de2&keyno=1'
                        },
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 3,
                                    'dayTo': 5,
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 490 руб., возможен самовывоз',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/245020/market_s5v4fEXxV9-IGR4_pSxMgg/orig',
                                'width': 674,
                                'height': 668
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/245020/market_s5v4fEXxV9-IGR4_pSxMgg/orig',
                            'width': 674,
                            'height': 668
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/245020/market_s5v4fEXxV9-IGR4_pSxMgg/190x250',
                                'width': 190,
                                'height': 188
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '495',
                                'number': '926-2641'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '5',
                                    'workingHoursFrom': '9:00',
                                    'workingHoursTill': '21:00'
                                },
                                {
                                    'workingDaysFrom': '6',
                                    'workingDaysTill': '7',
                                    'workingHoursFrom': '10:00',
                                    'workingHoursTill': '21:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.57461397',
                                'latitude': '55.76007516'
                            },
                            'pointId': '193208',
                            'pointName': 'TopComputer.RU',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Дружинниковская',
                            'premiseNumber': '15',
                            'shopId': 5205
                        },
                        'warranty': 1,
                        'description': 'Тип: Комплект постельного белья. Классификация комплекта: 1,5-спальный. Состав комплекта: 1 пододеяльник, 1 простыня, 2 наволочки. Размер(ы): 50x70 см. Размер(ы): 143 х 215 см. Размер(ы): 145x220 см. Состав ткани: поплин. Наволочка: 2 шт.. Количество предметов в комплекте: 4 шт. Простыня: 1 шт.',
                        'outletCount': 2,
                        'vendor': {
                            'id': 10738845,
                            'link': 'https://market.yandex.ru/brands/10738845?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Sova & Javoronok'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/ilC_DJusXfEyvF4c-uuzGQ?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=U9xqPzGtajIVUcdeYlC63sNsMCZV4LlVbw3qd9RHSOm_DdH6VMpzNGzyz7itZNWjDJkofadt3-XlpnKSj165RU_Yx3l-C8hXnEElgvUE22sR6Hdk9HZYpPHMjLhxOVub&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 10738845
                    }
                },
                {
                    'offer': {
                        'id': 'yDpJekrrgZEm2CD4oYt61oFvdKGonqTZKUqLsjwtvdE7UYdqojpexw',
                        'wareMd5': 'yBDgFg-hShxshOc8Ou-KMA',
                        'name': 'Комплект постельного белья Sova-&-Javoronok Лаванада, 1,5-спальное (50х70 см), поплин',
                        'onStock': 1,
                        'url': 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZ4H9_aAm-SdP69geEvrHAW-blXpEpbP7mMvSiqE0wHXNcbjFOAauooLfzyCp20WqsuZAHmxkCoODEykLWmE9JXtRLbu3OrhL3snBTAHOvO6ZoeviqvkNa-cDYVyzqD23EoH7epndmBWJVVh1Y301EBerTx4cn-cVlW8lrsl29RxOYFP-YOcJoglH2g9SMKfHx-i6RTVxdkNrDsvjV-fBBve0N75TKkDZNoK_VApgkjiPjn_-JuSp-5cPq1-ELxxHTq6eE0VQIPDKfUoGtPxgGfsT9BrsM4PIg6dvpWyiYQB5MRhD0FWbs-MflMfeEnkzPDAmIKkiVuhNQUoK-523HGGGLnTA0ACN2gMlwJAkJHIvlM46hq_IkKSasnomYIPDtMlXHbBGy0Ui3sBu_WO56knB2fNgR0scYQIfG5d0Me712c50SI16J2oi1bmwnrpsbpm4enYj5b2-esgnS_GAwJnNNQAWL7TK0qFO2lbL22zE7tpA8fEwMkTHd5ytNUGkVG4m5z_konhA1Y21p0VI8FmMXdRtUEN6hk_qUXkzehXxbUro7_iaeBvTfhgfeMZuruedHiFxzkaKTxDsHheMQZaYG2BnPsXXkA-MA5NpxJ7QTyO8oFAJXCczHCBQsUfUUqxbkcxvLBQ4Na6dh-C5uvZyyLiV3mnnFp4Z47y-sSaXKzWoS_RJ3ykSdul_hMQgIJTz1ojiyGOVptIMEDI3VLwsUENMtbrYqbvjHrmP_V5MyegSO1cQUS9q6kadwv6p6h9TrT_NplhnSZcyD0uqsU0Ihg_kShagoG_2G1ziJaJwZU8iEiSffM9Yyk9u-99dCOgG9WL3RWhq1yMrWJ0LrjDKnGWxM_S6g,,?data=QVyKqSPyGQwwaFPWqjjgNtwFDITzDmMAdbMKRCiaGZLbhcbaDUDzUb0muejS_JPJv8G_Zwl0TknylXOc6GetzBeX01p3nVwQyTDxawwlL1pYMi2ajJrVFAbQu5EKCS_c5UVuf33WGLpuPvX1g9D16udNMaxpo0CN6Ab8NBSrF5IrgBHQnhxVPd9mJPED95faofxSrnbDbfCH9EqOPrKTC71u-qzSTJLvXJTDQ8ArlXoo9BEUxbxfCZtLd2j-_SY_nZ6xMrBfMoFRqgC45fv49EtGO0PuyxeEcIrwguTu4f5RfS_lA8YxEmQKz0UEGWQbO_AJlzkbyBIimcc9e755gMGpswX2mHvdNmm1nQL8HDLyFcSF5LdKPnVA4HWBlXLiWgMN8pcyMIytREWTkuHTTDuTM40mR8W6&b64e=1&sign=db7bd0aaecc80431e8d037dcfde8907d&keyno=1',
                        'price': {
                            'value': '50000',
                            'currencyName': 'руб.',
                            'currencyCode': 'RUR'
                        },
                        'shopInfo': {
                            'id': 25017,
                            'name': 'CompYou',
                            'shopName': 'CompYou',
                            'url': 'compyou.ru',
                            'status': 'actual',
                            'rating': 5,
                            'gradeTotal': 4577,
                            'regionId': 213,
                            'createdAt': '2009-07-17',
                            'returnDeliveryAddress': 'Москва, ул. Трубная, дом 25, строение 2, Прием заявлений только в личном присутствии с паспортом, вход с обратной стороны здания, 127051'
                        },
                        'phone': {},
                        'delivery': {
                            'price': {
                                'value': '50000',
                                'currencyName': 'руб.',
                                'currencyCode': 'RUR'
                            },
                            'shopRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'userRegion': {
                                'id': 213,
                                'name': 'Москва',
                                'parentId': 1,
                                'childrenCount': 14,
                                'type': 'CITY',
                                'country': 225,
                                'synonyms': [
                                    'Moskau',
                                    'Moskva'
                                ],
                                'population': 12380664,
                                'main': true,
                                'fullName': 'Москва (Москва и Московская область)',
                                'nameRuGenitive': 'Москвы',
                                'nameRuAccusative': 'Москву'
                            },
                            'pickup': true,
                            'store': false,
                            'delivery': true,
                            'free': false,
                            'deliveryIncluded': false,
                            'downloadable': false,
                            'priority': true,
                            'localDeliveryList': [
                                {
                                    'price': {
                                        'value': '50000',
                                        'currencyName': 'руб.',
                                        'currencyCode': 'RUR'
                                    },
                                    'dayFrom': 1,
                                    'dayTo': 2,
                                    'orderBefore': '20',
                                    'defaultLocalDelivery': true
                                }
                            ],
                            'brief': 'в Москву — 350 руб., возможен самовывоз',
                            'full': '',
                            'priorityRegion': 213,
                            'regionName': 'Москва',
                            'userRegionName': 'Москва'
                        },
                        'photos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/173412/market_YrwNL3cwDMiFdfPVKg3nNw/orig',
                                'width': 500,
                                'height': 375
                            }
                        ],
                        'bigPhoto': {
                            'url': 'https://avatars.mds.yandex.net/get-marketpic/173412/market_YrwNL3cwDMiFdfPVKg3nNw/orig',
                            'width': 500,
                            'height': 375
                        },
                        'previewPhotos': [
                            {
                                'url': 'https://avatars.mds.yandex.net/get-marketpic/173412/market_YrwNL3cwDMiFdfPVKg3nNw/190x250',
                                'width': 190,
                                'height': 142
                            }
                        ],
                        'outlet': {
                            'phone': {
                                'country': '7',
                                'city': '495',
                                'number': '565-3485'
                            },
                            'schedule': [
                                {
                                    'workingDaysFrom': '1',
                                    'workingDaysTill': '7',
                                    'workingHoursFrom': '10:00',
                                    'workingHoursTill': '21:00'
                                }
                            ],
                            'geo': {
                                'geoId': 213,
                                'longitude': '37.6235673',
                                'latitude': '55.7715595'
                            },
                            'pointId': '321317',
                            'pointName': 'Пункт выдачи CompYou.ru на м. Трубная / Цветной бульвар',
                            'pointType': 'DEPOT',
                            'localityName': 'Москва',
                            'thoroughfareName': 'Трубная',
                            'premiseNumber': '25',
                            'shopId': 25017,
                            'building': '2'
                        },
                        'warranty': 1,
                        'description': 'Тип: Комплект постельного белья; Классификация комплекта: полуторный; Состав ткани: 100% хлопок; Тип ткани: Поплин набивной; Количество предметов в комплекте: 4 шт; Наволочка: 2 шт.; Размер(ы): 50 x 70 см; Простыня: 1 шт; Размер(ы): 145x220 см; Размер(ы): 143 х 215 см; Дополнительно: количество - 1 шт',
                        'outletCount': 1,
                        'vendor': {
                            'id': 10738845,
                            'link': 'https://market.yandex.ru/brands/10738845?pp=1002&clid=2210590&distr_type=4',
                            'name': 'Sova & Javoronok'
                        },
                        'categoryId': 12894020,
                        'link': 'https://market.yandex.ru/offer/yBDgFg-hShxshOc8Ou-KMA?hid=12894020&pp=1002&clid=2210590&distr_type=4&cpc=U9xqPzGtajJEzVLgxd8I5QjXgUdZVFt4YJ_MWfjfcePIcwoVbbG_sw4Fcmiw_Ouok_RN30QsnMldhuNWQxbmHGKf7CWwBBieXroHDJ5qEZtH8UUAOjqZMRToFJMy7xsk&lr=213',
                        'category': {
                            'id': 12894020,
                            'type': 'VISUAL',
                            'advertisingModel': 'HYBRID',
                            'name': 'Комплекты'
                        },
                        'vendorId': 10738845
                    }
                }
            ],
            'categories': [
                {
                    'id': '12894020',
                    'name': 'Комплекты',
                    'count': '2792',
                    'visual': false,
                    'uniq_name': 'Комплекты постельного белья'
                }
            ]
        }
    }
};

module.exports = new ApiMock(host, pathname, query, result);
