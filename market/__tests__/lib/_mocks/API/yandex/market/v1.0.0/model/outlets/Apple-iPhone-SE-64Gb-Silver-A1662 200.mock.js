/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/model\/13584123\/outlets\.json/;

const query = {};

const result = {
    comment: 'model/13584123',
    status: 200,
    body: {
        outlets: {
            outlet: [
                {
                    offer: {
                        id: 'yDpJekrrgZHhpLQrPyL817Y7bVCuPkC-pnxnPrx7uZfjmHhHvhA79PtYqkp4J6rWK_IzauKFwA75_mO7T32-MiL6uxwR37V93s5_4mOXqPLpgO7n4Wt_W2MWgUFoebSAoAlvKyX6LpQgACWdo9we0Eejq0QLZblGRdX_J1F8WJF4liMW3WIRUWsR6fVQf4yN69ikK-AmBC4IuQLxue2GZX-I24-YyCyGYFM6jljqetu3Ictc6_1xIi2E7a_rDC-n4ISUC5ikN9EBNdlXZeuOS6j0LpJLljdC-35k2K5C_j4',
                        wareMd5: 'zsnjbboT8UC-dIcb9b5TDQ',
                        name: 'Мобильный телефон Apple iPhone SE 64Gb Gold',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86Pd-UdR2kB5Uwy1uh-rxov2-K_nhd8ENNuuLQ9FnweSeYu_26GHDhWNdPiUpXfDY3cBpG7M1bBUCwudY1LXZFPODrDuHdl9_QkRdRdYM6-fznZJir6bev1uJufFvWEBv6rm57l5KOlUQL0PZ9aw1AR0jukbp6sPDz27e9w9auzm3pmiLglznaJpZqlIGv82ITQqywRWmmBuRqgPQJrNnHWNGHMujXwpEoedV9Z1gnT5SRlSVDdLKwkSFj7KL2FQ_AKUwMq1Xq7eLCtKHmMvvaznW8Z2ckU2bpJsyWgKcGksF4R_YOwbxC-nPvOAi_-s07O4mox5VLbU7t4Z42pB7SKlK7eO1ZZtP1_6Xjuchk0fiqclQHmMRDNxQMPMc6W3p_GwqX8OgrDZLP-1TIixMU3EVE1ebz5-vyb9pMiVLWmr0QlhyiSGdM8qv4yL6u9DpsOrthJeni-kivJz2UVlfg75G9iSxJwx4q4OlDXZLjPk_WRkKaelKSAUHt0qC0SJ5iyGiwiOoFggwh_3rRaQng19jZ9JkCAdRdFZi3LmgSsc8paYSCyYAeJ5nLU_Qj0VJR7SD1johAdDrkmLZlgqNpWpEL_KUBRanpbkFw01tfcC3vkMVlBntSfZvmYbNerNbEhcTFRl7jk0hqDqZsJ13bkF2r4M6HY_BbiJWHHurmkX6BuebZRn6IEVEF6MTaYXthvH9qsZALw8qilNa9wvZXIQc5t-S2LD0tKnXrViVp6-KW9Ifg9X3k2WVfDmou4MKS72on6Jl9sNClCu1LTxQ7e4Royb6sq9tam-udZOHXKwZUJt7G5USV935S3GNYvrBxc3hCUq_9eqEt0PSkQzFayg,,?data=QVyKqSPyGQwwaFPWqjjgNg9J8lIJ2Oz9gk_rKEXyxA3xLCCap9MlHYLA9QmCyiEDOb9USHfWBqukiqjB5d1CJxeYsPsry0r7HNLe43liS8BPtnJZp7Pb7Xafx6vVulrKZhrMom7lkoUQxl-YJ-byAbUSIwfivGCldL77hVCxkK4vdswgpR_gQMlaB-vCLNzDlr0kJb_WCkhiVU1oEjH5MmxGzsLuSm3rqWRXcurpNU-1zmKgoBjfmcarKaf4x5yv7uhI55oZ75DeOzhIOUp0i8HL9MqfLnGssOmYaoGjy5DZJKb71sLrwcNGFZWy1765UJNpiGOoyywhRyFlXrcrrdhGQX0ZM9l_xaeLCq5UhZqVtvSDTKtJISjdTGJvC-Q_6JQNXIz8V6D8fnA7STtqdHyzir9tj5_DK69bVB3X2Da7qtjpZzq-2oknD8HUs_eGvvVan1FGUXFP7Q8FC-K6awP6rSVe85Roj_767NMGUZ1bWMi8652cJafk0m6exc4_Dk5OHSxf9pthS1DwfODxsZzYRBYVRim0j3Ksa1myE9wMpuFxsZjGHDr17cIp33veT4xRnK0wRe_xeqSvCx-6rqku9KNBO6hhUKU1I6yI5yuqeqrNbWqR51NbNqb_VY8Pan-AO11pDHbQ8qHu1sZxWfI9sXMqb_QHqYRJ0KgODL7Du7LRJ_W6Hb6XZuO5Lr9_3CC8ZA5593qgFsZTxrWfYw,,&b64e=1&sign=e17a7da556dbeaf99e354bc5b4fdd8ef&keyno=1',
                        price: {
                            value: '25600',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopInfo: {
                            id: 38615,
                            name: 'Mobilfunk.ru',
                            shopName: 'Mobilfunk.ru',
                            url: 'www.mobilfunk.ru',
                            status: 'actual',
                            rating: 5,
                            gradeTotal: 3861,
                            regionId: 213,
                            createdAt: '2010-05-18',
                            returnDeliveryAddress: 'Москва, Шоссе Энтузиастов, дом 31, строение 40, У выхода из метро, слева от Вас будет «Торговый Центр 31». Заходите в ТЦ, и проходя его насквозь, Вы выходите на задний двор. Как выйдите во двор, увидите вывеску «ИНТЕРНЕТ МАГАЗИН МОБИЛЬНЫХ УСТРОЙСТВ И АКСЕССУАРОВ». Далее следуйте по указателям на вывеске., 111123'
                        },
                        phone: {},
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
                            localDeliveryList: [
                                {
                                    price: {
                                        value: '390',
                                        currencyName: 'руб.',
                                        currencyCode: 'RUR'
                                    },
                                    dayFrom: 0,
                                    dayTo: 1,
                                    orderBefore: '23',
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
                            url: 'https://avatars.mds.yandex.net/get-marketpic/331110/market_X1iWnCeN6lBRRnRoxyb-sw/orig',
                            width: 320,
                            height: 320
                        },
                        previewPhotos: [],
                        warranty: 0,
                        description: 'Тип корпуса: классический | Тип сенсорного экрана: мультитач, емкостный | Функции камеры: автофокус | Аудио: MP3, AAC, WAV | Разъем для наушников: 3.5 мм | Спутниковая навигация: GPS/ГЛОНАСС | Диагональ: 4 дюйм. | Объем встроенной памяти: 64 Гб | Число пикселей на дюйм (PPI): 326 | Аккумулятор: несъемный | Размеры (ШxВxТ): 58.6x123.8x7.6 мм | Фронтальная камера: есть, 1.2 млн пикс.',
                        vendor: {
                            id: 153043,
                            site: 'http://www.apple.com/ru',
                            picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                            link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210364&distr_type=4',
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
                        city: '926',
                        number: '963-3384'
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
                            workingHoursTill: '17:00'
                        }
                    ],
                    geo: {
                        geoId: 213,
                        longitude: '37.7517',
                        latitude: '55.760003'
                    },
                    pointId: '291016',
                    pointName: 'Mobilfunk (м. Шоссе энтузиастов)',
                    pointType: 'DEPOT',
                    localityName: 'Москва',
                    thoroughfareName: 'Шоссе энтузиастов',
                    premiseNumber: '31',
                    shopId: 38615,
                    building: '40'
                },
                {
                    offer: {
                        id: 'yDpJekrrgZEoohSPJiZjFaTELJQD6bVN8wGQSHV9qEOV_p5w3qGu8zBTr2Mb5jsEIXVZzBzCbeecSmOQ66NplIsLA_4y_ccDp0J5_1Qr9ThGtWKv0xl4wiTtnPLBYdsim0xsj5OLzrTUn4h-XAS3pnTcrkxqygZJz5502sm4ym6_HekbSlgNqMSMUABaYGXMVRtsYBvqVYlDS5EQCRTLCgcTAHgm_0WL0cvOTtLZbqLv0iJ8ZmvHC8LxseLlnCBlqhNLKrQQ8f0DpIASjynK8WLp3jgVG1xsSMLPsgXfg4M',
                        wareMd5: 'DiE1do_ZyD7FdOqKi6uGdQ',
                        name: 'Apple iPhone SE 64Gb Gold (A1723)',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYo5ilgsL5ZPUJRG5QdEw73cxyt_OFwx-9_4mkb0760hw2-XsNI3kiR-WqSubengxORez6uL9gfxS-Sc9CiIoguCaXfAzhbEMkqGddg54IFMcSHbWZiOVUt8kdo0RpVlCLDm-voSvDWxaNC3nSBbCOhZoDPxHpJNH9e1BD6hrLKHo-BFha4hFLvplMdSfqBXm9bcyk4cMg-R1r9L4BLy0Eu7Tap5kZ_5KkKMlGFRwUiUlTHalb1Vavruhq5URmatYs4skSh1kKaXFGjYn5C_U_wY4mivMyI4zISxpDmWAFEVnkwFONPeIf-rRTCUhq2xFjZokKaH_Lek4jgNyGgU7BLaMquSxUWNS1qvTXMg2_BfvwTFmJH6OjtRco1oP68sLPkpeK-heYeFiCMWVjqqzPr6aEt2L3qa-93PCfpwiWE5Dr7vZBlayI9K4e_qmtyic4DeX8Wkj5nwmU3LTa7m_CwJmOA4L3aFlzF0QQajvjgj1oy73oo_UJer68qDjV4PaRMkYJwF4fZNUjSna39BT1NN6dch8WOsA7GsuHD4YJl2ck5kopActjvpj6S6M5V662aqUMp7y0mC7xpmf4qbpcRoGZMZgP4cg37417HEanZoRdG78oxGLGWBO2MWbENrrKHeU3FI6A6QZ5fhxiohfMmtOl3YLitOhciWBj_WNsDFG33QupmOJIeE_JEYDqIiVVmYdesropGtNX9BK6humYk_cotN02graJr5yy-069Wr_TbEvxjEgL2Hy1EIPuv-dT7VtrV9WBZ44a1pa4iBWNyrfJuqreqZXZ6bOh0tMMPI1xoRc7LrSHX_FCZv1nVYBoMzkqVdM0CxON7ctC2gJH8KRaMMJqrPfA,,?data=QVyKqSPyGQwwaFPWqjjgNq5ob6Jq70TJiGoeif5hpcbP8YccfY02ogWNdXvVXQnUtSG2m_TC7d2bPi2wXHRJMk1-9eyQ9C3OTTJWwFMvmQ01-BEw0MrTled0m_WVfjiSYJ62rves8ppfr5jGT0n0MC0_AZ5JGONj7cyI0xmWDfbRzIffhpRmspxbSSGI8jymTuqa5h7fxQntaHRkcGD0agwxS-lsiis8vHfx-nz9dmMgMBT286SvnIYj8C6lUAGLmHii7hqwT61OslzTbUGE33CJ5lEfMpKmqQPu9sawEloSQp7hAaCtOAOnupx83qhoGEfXX0XwrWpBlNyNWi1D1f8o1FYy9SrEGvrlcJNW_k0gzKVK6zXW3qHie2cmpRV_KEF2YcgADEKuQmLpjHGO-TXHIlwKlJ60GsfvtBUdMO17LYDgPARoWUa8vn-JtxrUyrQ6-HegeQKaUfpLyO6pbMYaWyhB3VO4wWDo3cEG2abfesCy78tAGg,,&b64e=1&sign=5e6038fa9d2bc39fffcee37c56bf7640&keyno=1',
                        price: {
                            value: '25990',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopInfo: {
                            id: 262,
                            name: '1CLICK',
                            shopName: '1CLICK',
                            url: 'www.1click.ru',
                            status: 'actual',
                            rating: 5,
                            gradeTotal: 4950,
                            regionId: 213,
                            createdAt: '2001-10-15',
                            returnDeliveryAddress: 'Москва, Садовнический проезд, дом 6, «Пассаж на Пятницкой», 1-й этаж, 4-й павильон, 115035'
                        },
                        phone: {
                            number: '+7(499)990-77-77',
                            sanitizedNumber: '+74999907777',
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYo5ilgsL5ZPUJRG5QdEw73cxyt_OFwx-9_4mkb0760hw2-XsNI3kiR-WqSubengxORez6uL9gfxS-Sc9CiIoguCaXfAzhbEMkqGddg54IFMcSHbWZiOVUt8kdo0RpVlCLDm-voSvDWxaNC3nSBbCOhZoDPxHpJNH9e1BD6hrLKHo-BFha4hFLt2Lq4HCz9UTEyacW0ZdFsgz41cJfiyT66UiV3Eyd1uYU26B46UwinyXK5eqBGhQ7h-e0sbB579E9I-Nhv1QwuzccQCmVCNJnzd1NeY_ZxZtMnGV-hFzbT_k5I0D9cCj0DnCpVrwnZJpTNFs1ezkFmfGYG2chNZBymVaenNEa7g7t5wA94HT3w4sYGQMEQ9KizU7ZiimsMoDx6Ao3Ez137ATJOMRY2NbYF3CNKjTZtAbD-fBjOxx7T45G10vBPVELT7SIQYHzLHyowxN8Bq4tNqDmYiA2e_GMs_zzUYXPH8eC29qRXA4V7EoX8mI8u2qn2htGkX2YtS2eh1L7M7GQBXcPUGQWmhsY1jluhfPLDe09ZeJ6l4gR0uZbe-YJy1s6fwsnH3HVbJDkQgDFHARHv9WBm5IgvtBuOD7KRBUDEwbhpBkj3JbwUGVv0lAAUYKI4EFmKYqnXkND_rZU9CZVMBQF9q2-39M_iCW0HkaF2umK_y4Ii8bupeRXCpB0SC9sWamG6NnAJoZ0pU5tCf9wPhoye0uFW8eYHrjVEUHKnzyC9CugM8Lk5zEv2hiAuYMZOfCsKwvY5BavY1WlZKzxaBqXvSwUPpcTsMj9LDLOXCpOZGrlSk3kUu_fBtD2iFSmqM5vETdoozybQz_-MmRnGuJ7iFgH43yLqNXUAJ7vvf5Q,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-pRl8l6wfTaB0iaihtSiBUBMwTBS6-ZdyipBBVnlrKqk6uxDL4nmGUbhrB_1g6rLerFwp-4TEB-3VYoqDE7t431xXemhUhPUVjUDJ5H_JJf9evd6nPm0Gx_wThhQuUpuRNsV65KH-7sVOHdtxSnlmtM_DgvJ8WDz3F28nHwvgypw,,&b64e=1&sign=e356c5671380eb3ebc85839a61eb7f13&keyno=1'
                        },
                        delivery: {
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
                            free: true,
                            deliveryIncluded: false,
                            downloadable: false,
                            priority: true,
                            localDeliveryList: [
                                {
                                    price: {
                                        value: '0',
                                        currencyName: 'руб.',
                                        currencyCode: 'RUR'
                                    },
                                    dayFrom: 11,
                                    dayTo: 11,
                                    orderBefore: '13',
                                    defaultLocalDelivery: true
                                },
                                {
                                    price: {
                                        value: '300',
                                        currencyName: 'руб.',
                                        currencyCode: 'RUR'
                                    },
                                    dayFrom: 0,
                                    dayTo: 0,
                                    orderBefore: '17',
                                    defaultLocalDelivery: false
                                }
                            ],
                            brief: 'в Москву — бесплатно, возможен самовывоз',
                            full: '',
                            priorityRegion: 213,
                            regionName: 'Москва',
                            userRegionName: 'Москва'
                        },
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/230492/market_sHz7wlGGYko3PmuDKCY3hA/orig',
                            width: 295,
                            height: 350
                        },
                        previewPhotos: [],
                        warranty: 1,
                        description: '«SE» в названии телефона означает «Special Edition», то есть «Специальная версия». 4-дюймовый iPhone SE рассчитан на пользователей, которым хочется получить компактный, но очень мощный девайс. Новинку можно сравнить с моделями Compact производства Sony. Миллионы фанатов Apple с нетерпением ожидали выхода современной версии «классического» iPhone 5s. Они, наконец, дождались. Дизайн модели почти 100% копирует легендарный дизайн «пятерки».',
                        vendor: {
                            id: 153043,
                            site: 'http://www.apple.com/ru',
                            picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                            link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210364&distr_type=4',
                            name: 'Apple'
                        },
                        categoryId: 91491,
                        cpa: 1,
                        cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgaAxVu81sz_2-H_g5uiP0Y6GSdcTkyprNaR8CWjgE8SR62tQpscdp0x0QbSbzJUG-sl0EdZ-p5lrmBB1rltTd85kw1sJVl6eEEo1bE2txSPss7BWbapA5Wf0GNydRrnIHf2_m2XpB33Rxb0OmZbojwZN37RhxYoREOXhL_K_dUF7rnnftW5Hoy1FJ8urkczAg4QtF--hjOCsdrq7aaHEjtIa_DoFCToUAlBcp0seTuEvH96RR6-r7M9-GZDh6I46Y75snviGaR180VVcsB5rD1yF_zoHABb-lZwmmlBmnCpQicNSANxv7pHfVV7d40mGJSApymrFYBWkXjjFc0RjGh5Xh55BjeaCK86eKjyQJ0QEFDBvzbY09Px2T9QX1Hp778EXlj4ey2Kksk3UDpyksnXGC0AxJNP0GvOcW1BIgCmK0u6ZfT68Abxx0QVJf-3bXH9MNfsw6JR3qWOOe6eZpOIu6cEdVh8GmLQ2U8-GWpheEZ9Ez3GairOU9jYnEKFQPIiybU65bZso-B386IS6viCm9QqdDtMIYM8RgtGdupNl0fkH5XUP-fE1oGyjJJFSvCGWpn742z1yHa4vATbqQi-xg3JvzEhg1kIs8Wfv2v_vurXZS5JLyQjJUjGG-7EzCDLXw8QPjchnbQgTN9Z3VEi5rwWdim2ilQYzl2j7VvEvz-FdLaXsM3no9zMFOgI5tVBgdGuTJpZRs2uKDktv3nSutY1GaKcdoFaMi4tPHq5bsXNiwwWyBJTBj80S99EtQ7bJigCm5es_4CjGlcl7MJV4xUzpIJSKy4Ow7tCVeHTiFoyiqCBiMEMMKqJ7w06rKBr_QyVKu_JHvAGibQUiEwMgdkEiiwgT1aHC9b64pz_94hEC-H2G-tkz-2LUrjdxnw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DENTjVaICh3FdvS9rtSpsPBE14GmavAxFtwsnde1F2jScXSvXAnEnMBOxigIQfQG3WjZl79-pcorWfKDSF8tGvl26bcv__3r-KYHChZ6P0Qp9trFzkmhhmPemFvK3UAkknUXo-5sOnrRG22WEBgwOTGptYsjS4vMlp2nssywZbybdGlcztq1ALdjk70rdtd8hdmXg5AIUXz8E93wIRkL_U2uvAUAUnZQ-nQZl1zmqvzHCQUVKwrJVgd3IJq7_0bgr2ROEO8kL3c6RoDOn2xMSvm6Bjwk1E6hGT28IZJfDBMWTNrHCOFrJ2XGL78VZYzoifq2OvUSwnhXApHXTVS7P8U5lVnJx2z2AB6py9YV12WQ,,&b64e=1&sign=e74bf31e14edab2dc5f4b5015a6a67af&keyno=1',
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
                        number: '990-7777'
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
                            workingHoursTill: '20:00'
                        }
                    ],
                    geo: {
                        geoId: 213,
                        longitude: '37.629206',
                        latitude: '55.741571'
                    },
                    pointId: '336309',
                    pointName: '1CLICK \'Новокузнецкая\'',
                    pointType: 'DEPOT',
                    localityName: 'Москва',
                    thoroughfareName: 'Садовнический проезд',
                    premiseNumber: '6',
                    shopId: 262
                },
                {
                    offer: {
                        id: 'yDpJekrrgZEu_MRT-2Xo-ouUyBQaCY062Qpc3Bdy88FwdcY9V_NYCpus5sH_6aMmUvTZIIbkNI6uvojo8xZ9pPnO6OYE25G4SN6792Del4mdfmppCOsCPMowREi7FRt7regr3CNwzMEDd5ftq21X12xh1bwZdDbmBD9HK4d-Nf5oKq2Q8NvUTdOCiLS6hv5jYrlafOt9nu18LCxsruCC3JImdMv2PN6PJf9OKYSkR8VMgtjXfJ0q9YOyi-aLB5XE7_B6-dK8w6oMnkvFBGVDNcH7gRhK7Wq0BMOzqp7iIbU',
                        wareMd5: 'xPoSNJwjZ_unaYxhjn29HA',
                        name: 'Сотовый телефон Apple iPhone SE 64Gb (A1723) Gold',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZg77bymqGBd8-2nWeYiuID4WieMm0lq5eSy04-0XiL2ab7CjIgPHXaiZqX91XwlbbLfmG3e3VgmYhJWJ4SjhkqZDXeSeH2jTt7HE_dwbR_pQxUNU2RTOUTVGe1e0cn2rGqzOSkJK75faMdajf5rrYq9_k3RJoGxsvqgrAJfUg2l9h-GVLfEuVH7qta9NEPj4_VsYAdWzVR86UTWsuTl-60DaT6qJwLms-x-DZBibszTaGjbzk6o5xrKhDzEz5WIUGs_AhlJJDysh5PbFwYUxFuCdxgV3eJbr-94s_Cw9SBJo3WBW1IbQ1HXD9nuGITRatDMNoa5K-y04z641kmOGwILZetgLkXazIwj6u5K6nRggvkQj1teUQqSpnNYQZf3CSao0s5XQNEJzgbEyIFaSoJkwAmywhrSH60c8vdgi-sujGxV4J57ON3JpmWBjLnTHJv4m8-dLmpZ2JBAtEqEMeQwv4HJ7XPL7oD500aOPDo6SzI8um2TR-d7LUHnq6R0MO5S88AfPIdPwbl9AjZWbUsUg_leyPq0KU2MKHO_4ECOx9rKErrQ9fMSC4wBSIo9F71igWHfu-mMXVZMZwTASJ7PWHveZiIWC5UKMCe8DJx0FHADhwV8U51g2J7x5-rjdoOv2vqbl8nFjKJVbEa8C7XVVEze9yZacVfj3-LJkOC52RTFt33vJziJRZkUFW9_nYXcLP92vzWUX74drU07lHEcXNEg9ErpqtADKYmTteuqCXwQ-U2EK5DmtyNmVqN_9itti9EHr90Ba2iUhoTnQPaUbNMP_HYnEIu-CuIqttwtpD1ZVRmg6JVSt7_kLZ2vE8I1Lqnqvupm8UUxP10wHIRoTeXm57zmRg,,?data=QVyKqSPyGQwNvdoowNEPjepPkxt1Vqcos9cWvPI22tg0wCjd75HgObocn-VkR-9ty9kgea7JlqOTwblPNbhRmYOk-VkKqKpb4elHvNXE3bpM8YPk6Wx-gFQSjsBxqO4kTqgEyfJvCKkLSmf9sCpoINsOI_ccfbIYZGBUqfFlioQnBuxMyaOeP9RR5SyfwzNK96RnWBUQM2Fmv_rUx_sYFZk7oPp0W9l0Trdwu7s4L6yTnvuxDkEJgUOyldm-57s9wgQP5tMtRDanSh9Yjh7IcxNLEnwRPWMOY_0xo24CLjUWgxvgsfy8SxYv7bDuxtbJYxUW08qEu7ng6-1kZqOtayNx7P86DsQpKnyPVS6VVkON7LOTxytAwozmE6V0l7k5czQzTquM-HLMunap1j__plBMgReXz49WHv40xOd6rDM,&b64e=1&sign=98d5b284f312d04e176af355600b84ab&keyno=1',
                        price: {
                            value: '25789',
                            currencyName: 'руб.',
                            currencyCode: 'RUR',
                            discount: '14',
                            base: '29990'
                        },
                        shopInfo: {
                            id: 6537,
                            name: 'БОЛТУН.РУ',
                            shopName: 'БОЛТУН.РУ',
                            url: 'boltyn.ru',
                            status: 'actual',
                            rating: 5,
                            gradeTotal: 6073,
                            regionId: 213,
                            createdAt: '2008-03-14',
                            returnDeliveryAddress: 'Москва, Марксистская, дом 3, строение 1, офис 413, 109147'
                        },
                        phone: {
                            number: '+7(495)545-4227',
                            sanitizedNumber: '+74955454227',
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZg77bymqGBd8-2nWeYiuID4WieMm0lq5eSy04-0XiL2ab7CjIgPHXaiZqX91XwlbbLfmG3e3VgmYhJWJ4SjhkqZDXeSeH2jTt7HE_dwbR_pQxUNU2RTOUTVGe1e0cn2rGqzOSkJK75faMdajf5rrYq9_k3RJoGxsvqgrAJfUg2l9h-GVLfEuVE7JojZ3tVLm1H5ovhGIcx-1neVdH3GIDtD7Z7u8iaiJ8MTN8NOYittZWMwXX6DHWKX1fgGIQtHOruOMDHBnhFjVQfXKIlUJ3CimWxknQVoRo3sC0eH-mMegxJfo9mA27d3pqBN26bfpW-TvQ30olA7mXyehz1SJpBghCIwS7cta3Wtg1ydwYRznUAbadacvQgO0qK3BiHf7Htk9sG7qKRpNHX0MNBE6RTLh0WCzDX68Rpaw3Tf_8GnuiiMGM9RAo_ed6pM4qeSctiZe4_Ld4T27pJm-HqJGK_1gSVUZWGPuvhdsIUHzMy3D7fLtf0H8PORWpwMz7eFJ7Q-8X-aheRrYE4ExyKNpT-Vu3PCdVK3LHiwWDgha9gD9ktQs468vxlQV5--rQIVBm1lc2Bk8zr_UMOmy1nc8x_H3hvp1FCS6W6-vxAcYNtZT4NQa_inel0UYmrVNthYBcmlNEvTpF64yhijwCpkqxw6CbYAUuoF_lh3ewnk-o4Ycb7_5HifgJecxxXznmowertDLYXtfdVQZPh4OK5HkWfnxa_58iFV-jn1fVA7sMKXqtz1Ywp2P47BufOwGFwn1VUPpBk1KI3PyvlHjdun5fLiXEa2AlqS2msjexKfNO4JfmoAFACcCIJJRtrIcOs7pT0kDcLo3c0ZPfihGKNRW5LxYUk-8bViZA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8XU6hWOR1FznyqR-6SyXigaZ8aHpKFCQQg9hxk2xZ5PFvmJe7aARF_-aY3frYyxm1F-5eoiSKhL09NHu7gV3aH_0goF7HKBG9f9Fmylb5mGHASpLiWiXGJ_ZnrfijzgZr_gPpFxejVL4SoOuGXLJJzkO1vuPHRngFF4vMs6j_Nsg,,&b64e=1&sign=d73c8c7a0b382b86cf2f1c78d79d1b62&keyno=1'
                        },
                        delivery: {
                            price: {
                                value: '399',
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
                            localDeliveryList: [
                                {
                                    price: {
                                        value: '399',
                                        currencyName: 'руб.',
                                        currencyCode: 'RUR'
                                    },
                                    dayFrom: 1,
                                    dayTo: 2,
                                    orderBefore: '13',
                                    defaultLocalDelivery: true
                                }
                            ],
                            brief: 'в Москву — 399 руб., возможен самовывоз',
                            full: '',
                            priorityRegion: 213,
                            regionName: 'Москва',
                            userRegionName: 'Москва'
                        },
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/247229/market_i5rkkFNik_YT5KfFOrgmNw/orig',
                            width: 591,
                            height: 538
                        },
                        previewPhotos: [],
                        warranty: 1,
                        description: 'iOS 9 Тип корпуса классический Управление механические кнопки Тип SIM-карты nano SIM Количество SIM-карт 1 Вес 113 г Размеры (ШxВxТ) 58.6x123.8x7.6 мм Экран Тип экрана цветной IPS, сенсорный Тип сенсорного экрана мультитач, емкостный Диагональ 4 дюйм. Размер изображения 1136x640 Число пикселей на дюйм (PPI) 326 Автоматический поворот экрана есть Мультимедийные возможности Фотокамера 12 млн пикс., встроенная вспышка Функции камеры автофокус Диафрагма F/2.2 Запись видеороликов есть Макс.',
                        vendor: {
                            id: 153043,
                            site: 'http://www.apple.com/ru',
                            picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                            link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210364&distr_type=4',
                            name: 'Apple'
                        },
                        categoryId: 91491,
                        cpa: 1,
                        cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOXVa_dnEzKn_M-0AEyIXvWpVj7V-5Ozhiaa8UeH02jopBbv6tRdrStKmoGp-clo3cyLgRc-5H_AGJraLb8dDnS8xa7-fZOVr8-DeeoEmcqPoeQ1t0HNrQCvxKSo0Dgv_9WgceJjgSZN9v8VVqCYjnzvKCbq5pTGjcZTwDKN7xmzBjYzmI4rgmprGRKUe4pwPKKv8IMtNP7QfjCGzQFATVejGRahcQ_4-DRMHmfoRaiDCK5eBo6JaSdU9ElVfH9GiPsKdei9pzN7V2g4uliHb-CvTcrMvafcaRK1v76Dqooe_e_ES43PcuXmDL35VjluspVHf__LoLxr28zwReyFZZasPziu82kE4nkzBFBCRGVmlVML5Sd0cFgXVUm1sFuly-iwWb1MWsemmGowvJ0o3JypQ9tA0hSlX9G77Kqt6vGSMIgU7ZwobMhVj17-DaJRS941L0y5fJLD4D3S2mpsIJjBYMGbQ6WO4fAk6C7uP2QtUaGGF5FLXyx_ndsBXI0YxdVr13w0AiAlPJ0NSZ7NwOxw8u-32ZfkVV6euIf9BVBy1qj7auBHXItrMcnIWXdBrFXiVH2mZkk8KmxkEcXomALC3eAxnPhiuN3pv_nmcxfBMdR24_NTiLApGa9uC3mSApUMynE7NBNUaEmTwsGGf-EqABJnZBJQN6n3KcQrgL9CW2QHj_fYTFLwSS2Go0f1gQyxqD9kuua3_Yg5pvD3ZNpwIVrtqN1Vw57xX-xf6eqv-brvHd-wgxnI-dB0Hsts9q8NsV2izPv7kyjzpX4nHXqsMKz56EZUPXpfsvMDfhX7AoDZD25dFEi4bWeJsXYenTg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DENTjVaICh3FdvS9rtSpsPBE14GmavAxGFCzyeNv4nPXzcJ5NNa0pOM8I9j6sujBh7a6FKxnY1rTVcqLMyKC-ZGXN6fmL7lEDRyS8LoU2mf6rXdgkSkQr2yNGMT9OueTohjZ5RBxNk7IE__TH42tjogiJvqJRwZpRRnFZWx0ZPtcxl3VqsFtQyUP6SvEXqkljI8wHlLvFuvVqDZx0etEzQFIpro0KTErmwjM667osIwzmaaSMDduONwWJEbCDdR97eFLTDg77QtqNj_aQpEd5qMyFdPYYeqpEJCmnpaCcK5ZbDv7Z6obyqRskMmC_e7XB4MeOQlNJ0nWTnmJs8NSH9ok5gAei5iFVpy-uY5uRHkw,,&b64e=1&sign=b7ff96c4f56f3f46877080c6958aa22e&keyno=1',
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
                        city: '910',
                        number: '000-5825'
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
                        longitude: '37.59796746',
                        latitude: '55.78306774'
                    },
                    pointId: '317341',
                    pointName: 'Самовывоз М Новослободская',
                    pointType: 'DEPOT',
                    localityName: 'Москва',
                    thoroughfareName: 'Новослободская',
                    premiseNumber: '26',
                    shopId: 6537,
                    building: '1'
                },
                {
                    offer: {
                        id: 'yDpJekrrgZEu_MRT-2Xo-ouUyBQaCY062Qpc3Bdy88FwdcY9V_NYCpus5sH_6aMmUvTZIIbkNI6uvojo8xZ9pPnO6OYE25G4SN6792Del4mdfmppCOsCPMowREi7FRt7regr3CNwzMEDd5ftq21X12xh1bwZdDbmBD9HK4d-Nf5oKq2Q8NvUTdOCiLS6hv5jYrlafOt9nu2p6WZIqKwZdXtmnLfAInapIIWrWSYumTwxIIIkv0ICQP5zgtDzFfmcypHqR5Eh0ciAmg-5TDgokn85RNROp52poz1Q0FCw5O4',
                        wareMd5: 'xPoSNJwjZ_unaYxhjn29HA',
                        name: 'Сотовый телефон Apple iPhone SE 64Gb (A1723) Gold',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZg77bymqGBd8-2nWeYiuID4WieMm0lq5eSy04-0XiL2ab7CjIgPHXaiZqX91XwlbbLfmG3e3VgmYhJWJ4SjhkqZDXeSeH2jTt7HE_dwbR_pQxUNU2RTOUTVGe1e0cn2rGqzOSkJK75faMdajf5rrYq9_k3RJoGxsvqgrAJfUg2l9h-GVLfEuVH7qta9NEPj4-4H_pWPCB-zO31JA4DPFbuhHdC5MuY3oz5y9GRVJeuBdmgLGgecNi42jVZzKhSk6SB3yHNuXCsJTOoHSdMnxc5_Z4NQw9T4_xkoo7h6OCYm4_58RU6aPNf8anj96FkX0U7J-x-aOdyrfaFPI-azspjlZIDxGXR9Q7jlBE2Wn-0Ak225aQQqMq730VA_HdglLVS-0YE1OxWnnQV6YxQQoT5D5NanTpw4EVV8ZTUFMr2soW6ULShRTWxhzTNoSoVn9pPaDEZSrHbsc-0Glhbnx3-q-uCbGE32tGnKlRbBgl7WwE59iowb5oU8znjCAjugO2aeBu_gj-2LejZOkM1n-R4f4MqhX9RZ0l0CrOuxUsJwIiuI158j4RR4zE2dwdJIxNtPqAejBYaX_e3A0zyo0EwxX9QNWUpn900_xrZeBWbX18lecxvz3Hg8Ad59K3zwbUbeJxHPGrbgIEZHfnY-b7Nlp1ArWINTAXYrt5Px57v_vPOEc4RWjxlAyDpNV3h5BqOb0dQwQE8L3oB1Gi2KZYEyT__smZZNDsVoLK6EvxOnff2nALm63HPPYzxMty_UmVst2aoIfKo2vgNMNykqALZx525vdKNClDpFrTgJU26tK1K01xUcPia0aAnO6-4fn-7Bwqzu4zc-s_Ab9J4SmhJz-27N9pOimw,,?data=QVyKqSPyGQwNvdoowNEPjepPkxt1Vqcos9cWvPI22tg0wCjd75HgObocn-VkR-9ty9kgea7JlqOTwblPNbhRmYOk-VkKqKpb4elHvNXE3bpM8YPk6Wx-gFQSjsBxqO4kTqgEyfJvCKkLSmf9sCpoINsOI_ccfbIYZGBUqfFlioQnBuxMyaOeP9RR5SyfwzNK96RnWBUQM2Fmv_rUx_sYFZk7oPp0W9l0Trdwu7s4L6yTnvuxDkEJgUOyldm-57s9wgQP5tMtRDanSh9Yjh7IcxNLEnwRPWMOY_0xo24CLjUWgxvgsfy8SxYv7bDuxtbJYxUW08qEu7ng6-1kZqOtayNx7P86DsQpKnyPVS6VVkON7LOTxytAwozmE6V0l7k5_ZpJBPmlSd_gsqWejsHYb8aLG8DCcnuwZM-KuVYI6pg,&b64e=1&sign=a8a0cdec92dc7c00d5e485d4278dacf6&keyno=1',
                        price: {
                            value: '25789',
                            currencyName: 'руб.',
                            currencyCode: 'RUR',
                            discount: '14',
                            base: '29990'
                        },
                        shopInfo: {
                            id: 6537,
                            name: 'БОЛТУН.РУ',
                            shopName: 'БОЛТУН.РУ',
                            url: 'boltyn.ru',
                            status: 'actual',
                            rating: 5,
                            gradeTotal: 6073,
                            regionId: 213,
                            createdAt: '2008-03-14',
                            returnDeliveryAddress: 'Москва, Марксистская, дом 3, строение 1, офис 413, 109147'
                        },
                        phone: {
                            number: '+7(495)545-4227',
                            sanitizedNumber: '+74955454227',
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZg77bymqGBd8-2nWeYiuID4WieMm0lq5eSy04-0XiL2ab7CjIgPHXaiZqX91XwlbbLfmG3e3VgmYhJWJ4SjhkqZDXeSeH2jTt7HE_dwbR_pQxUNU2RTOUTVGe1e0cn2rGqzOSkJK75faMdajf5rrYq9_k3RJoGxsvqgrAJfUg2l9h-GVLfEuVE7JojZ3tVLm0gd90_2RPr7FaWe5Lt0KSuIUBh1vxOZ_NonMOcijYS8dZIlczx-vKgbikOqcdxuSpsptUFXmUeF8bbQcpyLEkCQOqJU9v3PVzziyb8WOK3_jqX5iWc05iMMCrVxqKvD7BsA3-_UQtuQ2A8s6f1Z2LIS59RmnK3rjWYOrep9BecovR8vbI1rrrz80E9X48LhdMXd4Gucn9t4l2MmKUac4KEPVnZMLu1B3leOI00xREF6_eU0-I6MRql22pq5vlk8nIfUvrGuXUA4Xt6nv2yeyts8cH00KkJ4LlgofzsNCJUZJ3ARv18L-RIMH4KQXo3ogoITZixI0oBJbfbasIKINrSVTQxKzoEj2dsj1JQDmypOjyogyrDL0AF2ErdkIq7sMld3hOfCEnVChYPp3uqExRZQLBxrMjzVscGJgDTQ0awOfQby2o9fRZz96z62vgJsEB7mlDmARxFIzXb1A73asUGYqLb4QfohCJXKA7JQC1i4uKI_CofAxUNVQMFLhFKX1XiS_AzPfBdeTVA8TmV5H0gDLHu1DFa9ZFdrth_5or-OFLndkBY7Os4q7V4hke5RhCHZRc99en02HHksPB9In5gxE9b4g6evHnsP4tw14kcVX1c12BX3aC8HpT5X1I2AQiWwNUtizuVCZ4h25R7TwYU0vw1wxe_y8g,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8XU6hWOR1FznyqR-6SyXigaZ8aHpKFCQQg9hxk2xZ5PFvmJe7aARF_-aY3frYyxm1F-5eoiSKhL09NHu7gV3aH_0goF7HKBG9f9Fmylb5mGHASpLiWiXGJ_ZnrfijzgZr_gPpFxejVL4SoOuGXLJJzkO1vuPHRngFF4vMs6j_Nsg,,&b64e=1&sign=0ebe36b7db6eb95acd4dc6950e5650fa&keyno=1'
                        },
                        delivery: {
                            price: {
                                value: '399',
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
                            localDeliveryList: [
                                {
                                    price: {
                                        value: '399',
                                        currencyName: 'руб.',
                                        currencyCode: 'RUR'
                                    },
                                    dayFrom: 1,
                                    dayTo: 2,
                                    orderBefore: '13',
                                    defaultLocalDelivery: true
                                }
                            ],
                            brief: 'в Москву — 399 руб., возможен самовывоз',
                            full: '',
                            priorityRegion: 213,
                            regionName: 'Москва',
                            userRegionName: 'Москва'
                        },
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/247229/market_i5rkkFNik_YT5KfFOrgmNw/orig',
                            width: 591,
                            height: 538
                        },
                        previewPhotos: [],
                        warranty: 1,
                        description: 'iOS 9 Тип корпуса классический Управление механические кнопки Тип SIM-карты nano SIM Количество SIM-карт 1 Вес 113 г Размеры (ШxВxТ) 58.6x123.8x7.6 мм Экран Тип экрана цветной IPS, сенсорный Тип сенсорного экрана мультитач, емкостный Диагональ 4 дюйм. Размер изображения 1136x640 Число пикселей на дюйм (PPI) 326 Автоматический поворот экрана есть Мультимедийные возможности Фотокамера 12 млн пикс., встроенная вспышка Функции камеры автофокус Диафрагма F/2.2 Запись видеороликов есть Макс.',
                        vendor: {
                            id: 153043,
                            site: 'http://www.apple.com/ru',
                            picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                            link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210364&distr_type=4',
                            name: 'Apple'
                        },
                        categoryId: 91491,
                        cpa: 1,
                        cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOXVa_dnEzKn_M-0AEyIXvWpVj7V-5Ozhiaa8UeH02jopBbv6tRdrStKmoGp-clo3cyLgRc-5H_AGJraLb8dDnS8xa7-fZOVr8-DeeoEmcqPoeQ1t0HNrQCvxKSo0Dgv_9WgceJjgSZN9v8VVqCYjnzv5L_eJh7PZZ_d4Ye6KO11CSCvYO4G1kskJRcsyJSXtM-yxtd2NTSCRZLI6PJj7lufUwcr--VWf2mdjw-vDHkQ8MfpAvrhuj6YnVylyABA6VZzdR_-jioZT1v0w0d_5fLveTXZX8Q6ZyMzN1us11hqp-5Qzh-m_oDTk7GAW_i1SvaMpWoNjTCUoIPbRO7Y9lXC3OwAWSg5B4Ang61BpnVBy7uEZQfYd7fYlAy4bcDumg0gvGSC8eh-ggMnCEy7uJhWguOTcPR40FT9geeZZzzjowmleypHxCPv7j2zIpQJKPFcJNDA-2ZdAE9qu9Lo5lLVTqZmpDMoxK2V2ElzODXQIZ4BpC3Yz2lfiim7u_cLKAP-jER0iLHZa5UTzbVRghFwup8eilbnF6XB-jkgXFAzImC0rB9FMg7_cptf3TTitj13vplhgIqR5qEuJsr2BFkyJrbjBwj8HRQTxhFThoPlwdBP18wBbKRRgj2Bj_mJGYYKF8GJGd_4xoM-0_5eQKjHfYvaEt9RU3b3DdOfjYkijmwckPHbrmUOfPxb4ysxq01MMOT7iZmgPDBGtQWdEI3Si66wWhxQS0bhY2LEpRkXIDJ8GF1CF7HNA00Bb7ppanrVA75YEARo1CVcmsayOe0LEcP5cXynb5HdMdGp3rrUJDlVTldeXAhZcfC7NiNsTaA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DENTjVaICh3FdvS9rtSpsPBE14GmavAxGFCzyeNv4nPXzcJ5NNa0pOM8I9j6sujBh7a6FKxnY1rTVcqLMyKC-ZGXN6fmL7lEDRyS8LoU2mf6rXdgkSkQr2yNGMT9OueTohjZ5RBxNk7IE__TH42tjogiJvqJRwZpRRnFZWx0ZPtcxl3VqsFtQyn2aXgrKnHxJ1oZVO7BhXPSLqfVTDLVGuFCJH-ZersrHY1iD8dEzBWGAKiTMGCtdB9ImWd6aY7otZLEM1T9QHYo5V5-GZV5a8l6K8Itw_h83zTNPega8nJ5k6VeCgsjSthHvHGV2uUfAFXNqsULs4dwjmRz3LMD_FQa1As92DoZcagLDtmCUuxg,,&b64e=1&sign=492728d4c23ef73ffcb67ad9cabe53a5&keyno=1',
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
                        city: '910',
                        number: '000-5822'
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
                        longitude: '37.624705',
                        latitude: '55.660856'
                    },
                    pointId: '251383',
                    pointName: 'Метро Варшавская',
                    pointType: 'DEPOT',
                    localityName: 'Москва',
                    thoroughfareName: 'Каширский проезд',
                    premiseNumber: '5',
                    shopId: 6537
                },
                {
                    offer: {
                        id: 'yDpJekrrgZGeiJg8l1_Qna6q2QdpKoTHDJvvPv_1jKXCs0CBJDYFMCL5BEhS2B7DtZEkdmqn10zLTAZXWFhtxa5j7gh91wUZdv3S-nxKWkhkVzZ5gETcpzQ0mKyX8Gd9ZZWRv4tHh0NezjUfb1zveJRV4AjSgCjIXano_uEqHqfCLZFyVVmEowvxp-vqV8Ru0AIobcFoyI5kR0L5UFySVM5A8q2XKnK4vNivq0GwQiGx89KME5EmiSqYMIWVz3lO58I87KKPajIofsoxqVudxyVKlt_0fc5OHNS9ky-LPwc',
                        wareMd5: 'ii10gvYsQiXyUbs0k4PaXQ',
                        name: 'Apple iPhone SE 64Gb Silver',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYo5ilgsL5ZPwwJafGBLBjZrS9mb4fsg6l-iUTa8DbMSp7mcIYwwJNM0rQIfFLSQ8rlDv0a3DkGKkkEeEFkLt88I4O06uOBip7m6xl1zFsKbgj_En0tY1LDEGRXmmwU8Nk9cqCBOj9axGCQDZRdF1pouSb1DX0LAkFTD1Hm7d3Ogq3Igi-0RaB9Q7i-lm6pE_pbHRxLt-x0cnGspTTukhZx6U5zsswcf3yCITYEw8YLTWmruccAPZwjfYX5dmrYTCxGAyJLKhKiX9NM__JCzxVMyIjDGN4NjOAPPUq7eua9WXDRvN6OR2meeUescwXgnzA4IKKhvu5d4lZrh91MKwqwml13LDHDMMhkx580VtKDPLbpCTbnm0Ko8ETD6wjcPcX3S-JxDnhRbYWxsGa19DUhtvY35JJ8F5DLdB4UZ-sEbrNVJhTR9x5tyG7z7pkWKFa6PwdkcX3aU8GhEz5FQtlTsk5m0Z3b1rxF1PBLsjydWHZEvqjVEGXV88JHzen0bpsd5MkuiW2gdbDo65itgMxGHVfRGqJpO0a1T1RfgEOC8RPOq5iNDL6JO__6CO09G1ozj_FUrUHbK_7QSN0xG_livl3ng9Fyvm_u8Mr_fqNBnuIijfWCfO1XtQkqwxmZyu_z6Ifzf7ZQrLjIYERTwSYMbhMYbwZ_KsqAjB6TN0TEFKqXOCGf-G7VNBdDCXSARTg0SdNi9uGKxd9xPUsTL7iZvdRvRMOyIW1JYnEelaBpp42KO3bWe0AgqLafHeNomw842t0HIJmZdFQovEqhOZ7aEViQyFV2OJxCXaBTK3qKQtswsp7J_aBgBudHII-XUiUWYH_KKaAzaHQo5t01T6ZDg2Ex4r38JcA,,?data=QVyKqSPyGQwNvdoowNEPje0PAcg_xXLamMQAyNAGhbkNussUv14IVRoAKVqnrKOILCorrWMPkK2g7AsAsjNpaDjfFD4ASRjRl1SY6ojDUucdpmRpNUJOCYgz6BWU4mlGqgEVshbffqFZ2UJCdIJIoqZz99bUcBwXqXn4fP6pqq0,&b64e=1&sign=5a47215bc1e65b3d9e3e54000ac79c7c&keyno=1',
                        price: {
                            value: '27799',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopInfo: {
                            id: 32902,
                            name: 'TEHNIKALUX',
                            shopName: 'TEHNIKALUX',
                            url: 'www.tehnikalux.ru',
                            status: 'actual',
                            rating: 4,
                            gradeTotal: 1771,
                            regionId: 213,
                            createdAt: '2010-01-28',
                            returnDeliveryAddress: 'Москва, ул. Ленинская Слобода, дом 26, БЦ ОМЕГА-2 корпус С, 2-й этаж, офис 221, 115280'
                        },
                        phone: {
                            number: '+7 (499) 703-00-15',
                            sanitizedNumber: '+74997030015',
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYo5ilgsL5ZPwwJafGBLBjZrS9mb4fsg6l-iUTa8DbMSp7mcIYwwJNM0rQIfFLSQ8rlDv0a3DkGKkkEeEFkLt88I4O06uOBip7m6xl1zFsKbgj_En0tY1LDEGRXmmwU8Nk9cqCBOj9axGCQDZRdF1pouSb1DX0LAkFTD1Hm7d3Ogq3Igi-0RaB9pmWMD4R6jTj3a0U03XznhZB6muf30bOYrm9BZDjTfFu5xxXcjCsF92khgqavp4LB-LlUMcZDKsD3HTU6saSK5SRbITByz3xr57OGdOCvm7jG-f_33Sc4Xl-jTrxaGvwQvd77hFPKBFbjAv1BB95TUM5nrmbVX-VKSi9N2BQ4PcaAmRvqnZaZO_MLmIlcpl5urlzAfTxwj5nTMelIkUNsouOoDl6SkbIlvLUXEXs9s_XXeQpwT8ZSt9Bt98_vziLnYMj3Oos0p6K8UvSy-3u81gqXwGRUiCnskUHShL-F0TslqYtwjq1D3hYfWcWzPEDHWQEs8XnFZ6Bh9vkV8JpHkjPP4LHTmXAv-Upcuxf1FD54G7U6_YtlQNpAIjc_hPrQun1mVuyvYagtcTKsj9nDEhrqshgukzvIBZRtrlXQFsOOkNFFJc4FUgaL4WdcfOEAzTPwvInQqa32SEOMlZD5Mxq4cWW2HP6yAyUdR0Cp5qM4BfZAxXATWWtW_AQe19Aso5_d2IvmhIYpi1vhnBASn9vCfP7A-lcTzEqpSxxYdivQRQPwedabW751Hbj3K-3RKpuxHuT6Brltw6OIAVwM1hTbyvAnrm05QEMWguIjR_FIdAETamRbVfVbdd3AqWDSPLG2Y40_YnqwveRbIbkmOZKKlO-bg-kBC7M8V1j91Bw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_aFKE0fsIL5GA2-PM9WsIeKqpdLlR9rpJC4XLZxeiVIkXpHsKwRoocIJvqh0EbmjxwPt5Y49A_lsNU4hd099wrecoyCbuHRzuKUj5fTAjlXjlxhjoc6BYBnJ-jA9cm6SH3i9XYqrWB0FbxouSsZgFWzfk6uPi31TSFh1kQffdi1A,,&b64e=1&sign=626e0effc844dcb5045d30cbf1773346&keyno=1'
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
                            url: 'https://avatars.mds.yandex.net/get-marketpic/219743/market_Q_GPlcDIDgCNd8XvasWZug/orig',
                            width: 470,
                            height: 545
                        },
                        previewPhotos: [],
                        warranty: 1,
                        description: 'Apple iPhone SE 64Gb Silver самый мощный 4‑дюймовый смартфон с полюбившимся дизайном. Установлен тот же передовой процессор A9, что и на iPhone 6s, и камеру 12 Мп для съёмки невероятных фотографий и видео 4K.',
                        vendor: {
                            id: 153043,
                            site: 'http://www.apple.com/ru',
                            picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                            link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210364&distr_type=4',
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
                        number: '703-0015'
                    },
                    schedule: [
                        {
                            workingDaysFrom: '1',
                            workingDaysTill: '7',
                            workingHoursFrom: '11:00',
                            workingHoursTill: '19:30'
                        }
                    ],
                    geo: {
                        geoId: 213,
                        longitude: '37.65521773',
                        latitude: '55.70998443'
                    },
                    pointId: '285734',
                    pointName: 'ТехникаЛюкс в БЦ ОМЕГА-2',
                    pointType: 'DEPOT',
                    localityName: 'Москва',
                    thoroughfareName: 'ул. Ленинская Слобода',
                    premiseNumber: '26',
                    shopId: 32902
                },
                {
                    offer: {
                        id: 'yDpJekrrgZGZGWPAtlC0aqAL5ihHLj2hk3YbR50XzlZK-IjOr75SLmcq6jJXI8DZrx9K9eM3E2UpqhMWuUH8JUk7GWlX9M--aud5S1CsliEZIrPk1cArUA-r3MrR1VEt9NrXP6d_QASrz1ieF-zhnjwDidvTiaY9tROrtkvhkmyR7S8J0nQW6Va0xoWz7fDuzQ0n69ZI5zWBU9QfJStqvhj6__SpdaXB0AmiuBdzFf-OryfV_fOw2HGoMXdzd44sRFnmrsGa2Ub4Vsmd3yUKB1-AVjZRwWUvya2IzXPQobY',
                        wareMd5: 'nyiEPi-lA4kejgu9X_uhrQ',
                        name: 'Смартфон Apple iPhone SE 64Gb Silver A1662',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mf5fIUVrEGur84qC-PSK5TYHFQfwNL92WuhMrWHdRZCiuhlG3xsRNd4l4MYX6ONy1I9hDKtUnBmplsC9DXB_0Zes-RWXqu-9f6v1Q81c-cE3rOFeDHcPLIYoh1P5rF7pY0sKiWmSBJarEi05eePw7Fy6SB_mCTvKFb7ewOisGTGAP3yv5qJP_MRrukU97zmDr1gsIYEldY6Cab3D_6L2Ii8HYo0NcA7iuiynOefgtlGkdzk4mtXHKJXHPj1kzz8Scc3WH3wSbruRoLoazSEgDKDu-ZwJL9uKxvJvBwC3oOkBrcQ2JszOR50j5y4tzFdi7eXLezvLdG99MkJg_6bdAf2KjdFNiX9j-syZG53oDn48ujJToibtenenBF2sV_h-hx_Tw1HdjbSa7cy2vBvfqL3tIlTZ6w6EQzbpHwpBk9UdlcEZDkI6phIA46_L8IDg7OHUPRupt2zn5_6K_8MxoH1UJHTtllsUEjKrHXda3pxiGsCuxMbSXSyWUFxmTE5zYLcfQ1JGZTvO4CJXbw9y9pdq6p8h1XoG5IJM8_ROv39njwGJge5U7AxVDSwp8bko1SrJORkn_JIrD67ClXLKpsKEhtNarv31lywMK-LauJSCTtFHuCicKmbPD_ROSJ9n8BgK3JN8kUzxbsI2nX_SaPV5dB4by35rf-nPOIrnsp_FKYmoq77XWTROZUq97Tzjf5v1pXCMxjkIDFaZF8kQtn8_JC2qulyXr5MbDB64dggERDehkcRQ5VPPLC2alGhwfM3k6gJeDZB95wg6L6HjN6cO5Mw_7NL39-z0ecXMKcgO77SBoc2eYEf30HwG1kq0qtfetmzSLAwPqJNBCA2eekHWz4ipPF2Qeg,,?data=QVyKqSPyGQwwaFPWqjjgNt96Q-I4eLvRt1Jd-FahOC1oLxErrgxA7U8HsI7pvjzVNeHCAbnFcfTqF9YngERSVUOqAY-Mf0DS-16OjPY5CR5RPoQ_p6Jar-n89K2eNeFm6ucx1ZHN0m-QfYQaUI21UNxJSrzIgzpvMMxdlK23zs7LoGPzzVJBp-TR4_IfNaIed8fjCy7Wr58BPSHmfTcmgcIeBsm0-2qafOogzQ3FTFVC1w_gskwlX20NxQaNuHZR5mcF-_9bGxXt-EwL6MNdghc20J_VWkj0Izx74IiQKX9JojCQqwOZ88UXBXXWmEQcjmVUxO6A7eZYFXSgTG0MN13G9yB8uS9FJlpWkH0oFrLZ27TorkOtcg,,&b64e=1&sign=e7a9156ff87f8d3556bebed07873a51b&keyno=1',
                        price: {
                            value: '24450',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopInfo: {
                            id: 115587,
                            name: 'www.CULTGOODS.com',
                            shopName: 'www.CULTGOODS.com',
                            url: 'cultgoods.com',
                            status: 'actual',
                            rating: 5,
                            gradeTotal: 639,
                            regionId: 213,
                            createdAt: '2012-08-20'
                        },
                        phone: {
                            number: '+7 495 790 93 53',
                            sanitizedNumber: '+74957909353',
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mf5fIUVrEGur84qC-PSK5TYHFQfwNL92WuhMrWHdRZCiuhlG3xsRNd4l4MYX6ONy1I9hDKtUnBmplsC9DXB_0Zes-RWXqu-9f6v1Q81c-cE3rOFeDHcPLIYoh1P5rF7pY0sKiWmSBJarEi05eePw7Fy6SB_mCTvKFb7ewOisGTGAP3yv5qJP_MRW41_QuXgsl8lMByHxn3D6lkn6X-uEGNyEOQ-06OohWFU4MG-9jus02ZRFof0z1neFVpm7G6Vmp8WB_UWn7NSeXHv468xqujTFWuDCSnHjbhD111tcLzfkkD_yTUCvmeiQgHmfDNCY3npOzf0JldWq6DPAhhrdsULLjT5mytoDHP4BAN5w91fxVMsc_AyzX37AiSXzIdY6fvELtIUOwJ422v-yQb1FTmcrt3tzF-RvleDbik12L6NGGsh8zTyrZsXZIz09XVSqejqe3WxPFuVr8o-qbnu0r5Xo4Qx8AwGQhvjRZzUzcWJCjpoJnHKSPdyDnfs0LdDVhy-XiHPs9so0NRYL9ySgkJdbpeNogjxoqFRxucJdHaCPqX41dh9eDX4rHdUgOTBVmtchDsXCb8RGGkoj-L8CTZTzXMloIJcPJZdZozR0kksLPDETlmJk0ybzWSsNKr5Bw_UJkL1VYHv6QiNTIQnugP4vg-fyjboSqRliuokhDnfpu2d_yy_tN4V9_2CAmFlbHqZRa0soYnMWGDIyzoCXuAWmejU8VMipesHooQRxUwG2SF2LQtidB__8dBQAGXv-VwaDPYDlfO174cYK_BRZmspuEqj6nM2YUSQFqZR9Iv6_h1qVwvTXENo1MvCOm_pI9OaDmbLOcD29MRaDZHdzo-VwHaCj5I1dSA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_1zBEF_FUaqBDvKYWgG3LXZB_vwCwNG6Kd2d5LAifZ0rwBtkL5iI5WUfKBLeUhVzhEpdm1CcMPgay8YJkLOV69R0JwaxyEyzjH-7I-y7v7kIr32hAsFittfuAmEdv_Op41XHF7hJC4OmV3TExVAbhUjANAQeXjX0Nn3QEfenv02g,,&b64e=1&sign=ecc14bca0a66ed6503f2610318f29e97&keyno=1'
                        },
                        delivery: {
                            price: {
                                value: '399',
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
                                        value: '399',
                                        currencyName: 'руб.',
                                        currencyCode: 'RUR'
                                    },
                                    dayFrom: 0,
                                    dayTo: 12,
                                    defaultLocalDelivery: true
                                }
                            ],
                            brief: 'в Москву — 399 руб., возможен самовывоз',
                            full: '',
                            priorityRegion: 213,
                            regionName: 'Москва',
                            userRegionName: 'Москва'
                        },
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_NGvrwrZYAQkugW1DvZJolg/orig',
                            width: 96,
                            height: 200
                        },
                        previewPhotos: [],
                        warranty: 0,
                        description: 'iPhone SE - смартфон корпорации Apple. Представлен 21 марта2016 года. Внешне схож с iPhone 5s, однако имеет многие характеристики IPhone 6s. Работает на операционной системе iOS 9.3, содержит процессор Apple A9, со-процессор Apple M9 и сканер отпечатков пальцев (Touch ID), встроенный в кнопку Home чуть ниже экрана. Выполнен в четырёх цветовых решениях (серый космос, серебристый, золотой и розовое золото).',
                        vendor: {
                            id: 153043,
                            site: 'http://www.apple.com/ru',
                            picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                            link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210364&distr_type=4',
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
                        number: '790-0759'
                    },
                    schedule: [
                        {
                            workingDaysFrom: '1',
                            workingDaysTill: '6',
                            workingHoursFrom: '11:30',
                            workingHoursTill: '19:30'
                        }
                    ],
                    geo: {
                        geoId: 213,
                        longitude: '37.50094978',
                        latitude: '55.74185764'
                    },
                    pointId: '291784',
                    pointName: 'Офис выдачи предзаказов',
                    pointType: 'DEPOT',
                    localityName: 'Москва',
                    thoroughfareName: 'Багратионовский проезд',
                    premiseNumber: '7',
                    shopId: 115587
                },
                {
                    offer: {
                        id: 'yDpJekrrgZFztD6WyzUvHlunueMn-6Sa7O4BtaDo1tB5b6t08UmMWfcdpi8DiYfj647w6UTbQCPe3vm-HF05L0sfRiU94O18sP950juGh0deQc-LN57SKEsgOHLkRdFlGU6b7guvpNrABUfQ03BQ_KhqeFpa-xbWeggA86zpD_kg8VAplUi9Z3yC5GoGUSfC4xGoUlyXuLz2P5-0rOCHkiNUxc58f5nQ6k1leLyei62sFpb4pFKpJ9HL4_1ZXqai0i-asrL1CoqRvol6yfvRD6t5iHLpSGhikDXuqqMbzrY',
                        wareMd5: 'bpC1Q5JvtTdlTvx_Osr-Pg',
                        name: 'Apple iPhone SE 64Gb Rose Gold',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mf5fIUVrEGur84qC-PSK5TYHFQfwNL92WuhMrWHdRZCiuhlG3xsRNd4l4MYX6ONy1I9hDKtUnBmplsC9DXB_0Zes-RWXqu-9f6v1Q81c-cE3rOFeDHcPLIYoh1P5rF7pY0sKiWmSBJarEi05eePw7Fy6SB_mCTvKFb7ewOisGTGAP3yv5qJP_MSxKpLvjmGdyFL0M5t9PnKWQksFG5RhKzczrVDpDkHirWY_rl1j21QxHwgnJn7o5OoPqQ8Nm2zHZqPAIvKj8FxggK1p1nLmpWxex7EgenDkwaytTIHqL9RUW3nJG7Sp2kw_nMLDKcjGnfzXuSS0Qccrt1_HKcKEogB1ilVzOP9K3at8sIjhRX0Tp13Z-LhCo4tLrcTULUXRQ60uUj9EcjHetZw245nL59aua8IK5xVDa5iEVmgJufRgX2PRBt6jOq-mwVL8ZzLSwGN2oIAGVneOLemiD56P2myFettE5lYTKlFoonYEr1NFXaT2BXNmeyg1AKmBjDjrnjN4oUyHVWsRqgakDvwK6TOka7IjPAhNJsXyd1vd3MhO7p_fYHw-fWaXXGJLuOcx3lkCv3qgLE09Y5X0t3p_xjfHrv5lEzByF4NgVQ7GLTAGSJ0XNHTNlRtxwB9VWmnHWitgLJbXX5vmZLHvStOHWGe8wvISiQsWfWFYHAewYiOA98gZ4TnZIK6mk_K3rS-9ht5RjEKLroiFTsZugzWBgDpSlymaWvHmFzL-wudsqhTv7-Gssq6vRFyGKIndytY2pFGtGSd_EX8Y8lkRwT5Ajbjmq5b87fruNZkt8XOtiG-OXaCkCUk7imhEgqproTA0Tt6_qTJPQf33MBCGyUd-GWov4WHgqejnhA,,?data=QVyKqSPyGQwwaFPWqjjgNsR692ugY0jPYkvqWKcD46MDfdBSsZS6r2NcWc3E2dfmpeKYyybk09Oc9x1I4Y-3iOvzvgO6LlWhuVBAIflc9xp-FPkDEMEKrrxCZFkIYAk4U_wmZgoQtwVqnC0vSB3UmEGJvutiOSivrat0ExABP0LaKoBl8XiNEG2ci7ck3XHq6zxj3EJHsFfU8FPg5plof3bPCFCdtVx3zSGDb9_YBH5PJohOFuKzKKLpuYL2fyGZuVAsIVQEeRnMly1fnhBe6yieW9PX27V8tiLf-OBRaBx2192SSmaGHFtgdtdYAxdelZBGb4nkL7VkQmDp1cDrNWQGRqbO3RZpdIT1tDEZapYqDy43UDJUUAS0WKEOeiNqDQ28m3CY-nER_ONmQkvn20lJJxRIyLqw&b64e=1&sign=95d90eda64072b23237232db3c38182f&keyno=1',
                        price: {
                            value: '24870',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopInfo: {
                            id: 141122,
                            name: 'Цифроцентр',
                            shopName: 'Цифроцентр',
                            url: 'www.cifrocentr.ru',
                            status: 'actual',
                            rating: 3,
                            gradeTotal: 285,
                            regionId: 213,
                            createdAt: '2013-02-06',
                            returnDeliveryAddress: 'Москва, шоссе Энтузиастов, дом 31, строение 38, Пав Б2, 111123'
                        },
                        phone: {},
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
                            store: false,
                            delivery: true,
                            free: false,
                            deliveryIncluded: false,
                            downloadable: false,
                            priority: true,
                            localDeliveryList: [
                                {
                                    price: {
                                        value: '400',
                                        currencyName: 'руб.',
                                        currencyCode: 'RUR'
                                    },
                                    dayFrom: 0,
                                    dayTo: 1,
                                    orderBefore: '23',
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
                            url: 'https://avatars.mds.yandex.net/get-marketpic/364498/market_2pdfxuJ82BXGpqDQPVzApA/orig',
                            width: 484,
                            height: 484
                        },
                        previewPhotos: [],
                        warranty: 0,
                        vendor: {
                            id: 153043,
                            site: 'http://www.apple.com/ru',
                            picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                            link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210364&distr_type=4',
                            name: 'Apple'
                        },
                        categoryId: 91491,
                        cpa: 1,
                        cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOXVa_dnEzKn_M-0AEyIXvWpVj7V-5Ozhiaa8UeH02jopBbv6tRdrStKmoGp-clo3cyLgRc-5H_AGJraLb8dDnS8xa7-fZOVr8-DeeoEmcqPoeQ1t0HNrQCvxKSo0Dgv_9WgceJjgSZN9v8VVqCYjnzvXQ1gXbIbpsTU9TZe3HaNjslncgBGm1nTgdAo23K2mF9MS2TN5HnRquwW8rihmHgRiZUSqm2jgDEeJLHceNYfz4DvQrafOmZ797CkBfFuhPgKfmiQQexC2A-WVjrLNJT8I5-LELGL8iiuRaCy388bBvxvBBAd4nhOYZ4CZLBTGqb-_f--LuoMSwgALy1E5DZ7xYa_d38euwEhK2rKopz1utN8AejL7YRszElhwqc_wGYSMNpuxlqpzcFYRQcw2Y3gjKQWceM5n8UXN3df34RPFLL5dkd3wbhBUVAMtHGNemwtzlBdyB4iFqU10MzUFSos-gAxpERVIjHizhHZpoXY7zlRSTGepx0MZKpwNJTHw5LqQz_LU1JTV8b2klRzBJBpyEpSzuX1a9_Ajer1__aLm9Stl3pVV5dpZmHy_mdttpPvKNh2LDUm_KoEpuHcxYXybDcuVXrsKAlwSXo_YUrS_w3scLjSyil544MfcYGOxB7WYUDYZyH8W7mfB3oKoEtmAxZI95daABDn1GVYZBPty2tVkOBtqIUhQFGSsdsNmGqpehWohVa1DupCvrztUQbxTcxglsLTXgcj0rYuPn6X6S9NCDYuzpFYy8km33YqcYA3Vhx7v1yKhFtEXTyw8g3WGgQaKllTSBgx5HWsVsyoIHB-h6qZK4K6rALjwBILyqg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DENTjVaICh3FdvS9rtSpsPBE14GmavAxGFCzyeNv4nPXzcJ5NNa0pOuJxbCOHqDGYn3qCYy5ksTKNioCLPSip6ft7niXFOfOmJzSGNmSpl2jFfl4Hup2rH_Wo9hBzPKAx2vTTDo6VVxEFa9ar--HMGH11UpEsamdAD8XVxUeCJjtoOI6rbAgxLjzvi4ne0TUT9yKkXznTpKZ9zfzDpxgxOohSJErEOPBJidLqokgdPouOjCyjV_epVYi_NhL8ZDHDYw2jFuDh9mK9mHLz4lLa8qFjlORNC0ZRGXOSeOhqT17A9EBOH6SnUY16bgvwL1EttrouElBXrDrLE44iizeyHNA1-ifQ12lIxoL1A9uU7BQ,,&b64e=1&sign=06d13463784b80d0812d3b5fbf428993&keyno=1',
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
                        number: '137-5157'
                    },
                    schedule: [
                        {
                            workingDaysFrom: '1',
                            workingDaysTill: '5',
                            workingHoursFrom: '12:00',
                            workingHoursTill: '19:00'
                        },
                        {
                            workingDaysFrom: '6',
                            workingDaysTill: '6',
                            workingHoursFrom: '12:00',
                            workingHoursTill: '17:00'
                        }
                    ],
                    geo: {
                        geoId: 213,
                        longitude: '37.752293',
                        latitude: '55.759172'
                    },
                    pointId: '411853',
                    pointName: 'Пункт выдачи cifrocentr.ru на Шоссе Энтузиастов',
                    pointType: 'DEPOT',
                    localityName: 'Москва',
                    thoroughfareName: 'Шоссе Энтузиастов',
                    premiseNumber: '31',
                    shopId: 141122,
                    building: '38'
                },
                {
                    offer: {
                        id: 'yDpJekrrgZFhEeS4pWsCcLBEkLNFGMaPDbVR9JmyldRWEY5nEUA1Lg',
                        wareMd5: 'PuP3JUrrsKk8jsJPuzziSA',
                        name: 'Apple iPhone SE 64Gb Gold',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZ4H9_aAm-SdNbes7a894--DlntXG6RGWlwSEHOq88vzY3jKEm9h8PdQRbQms_-8nc82vveuKXCcbk_m7zj5YhxPDP3NgGNhoqE2MpY6TiR4Ub_Qr1ZjfRtf1Q_FNN2s6csez1BXFwFw4sw8K--MKYtesO3t3svRY4wIkUx9c-kQ3TRpFMhjxC67c3lGvNK0jDazpuweWtVH5f_dsvW8rwLeiFHx4Spul1kg5yRDinKrDyWnbGfydaVXJ1jgdxOdWCLp2E7xfSNCGB26c_P9643vN8UyuwtHxmUgvFAvz6z17wNczF70D2Zg2J1UJs_6gBIKmrmYSiJWfYEECk3ebzAZJJwq7JrgcJWxXVMr6Flc_Z6oJ2y31IM1GTITq9FvKe_yOngZMK4nOQGNwsBbxKjvJCgaWn9C3ER9uhLD4HcnM9xzIiQ4kwPffmbQGMEwnYtZlNiGMozp-FEBw2C3KqxFLZuIeMjtkegWX7ZVQL0WS3JYgB9AQk2pAIojVQgqIoOXguUBpXlH6Xsdw7ieoEq8Ok4Cdx-W-UWtNKOHDcQ-4At7ub3zN8QpuGE5rnOQ-TqXAvfDzSx7nlM8RTfpcpdUMzIKTUz3l3cyq_7NbmJKepDB6jZk3mYEsw1RKeXDaLtK7gC7Q8nz2NMbV742iwpe63k-f00J8nHDh2Hy_ZywjKkfv_v8Hf-ofp5WWxCgQG5Xlm_HJhtW_XgLlMz6qLlH9-JGPg36c-5zvWm-zVqsPDhowG6-spxRao5oX6oIto1W0KsbOfZc2uAypBr7tX-NCrgLa2gAXULueGJoBrHo6UP9JCGt1CWNwmBC6GivvbspMJ8I74_TNGLYXpEMbtyWtWaBHgHjLQ,,?data=QVyKqSPyGQwNvdoowNEPjQoU2CiJ2kh8q6CahX4QNdfUG5in7tF9eddnvy3FW5ZA7IW8rgTe7lq6mVUOatIyshYZN7UzAsqcrF9qldgKtMjDwMkkILG8Mh4scvm1GrSkm9HLsFMxIdtvvxJw7wZ4HTmBhfUdPnvyF08rpCUpXI00DAWX-u9ELyKiFTevjRWSWuv-TfPpIeKvwtCWvTcSNPH4o66r_AupZXEB_Bse59F9k-xcUrVJmkWgHYLpz9cNZ4edXCuKyjppl_4j8NPLXshO9Vm-B_UxnMRQQlVBurnVRmwDA2Tz9dCs2UfoG3ewKxMg0-FOiF5qU5RlJXZfEeoTkg7gy_yQ7mYNqLSUJjkGBRSOq8nSbtwFOkgtj2ymaKVobrQ91-O10kVWp7Uu9Q,,&b64e=1&sign=07db7b7a3a45730188483617b695b101&keyno=1',
                        price: {
                            value: '33990',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopInfo: {
                            id: 955,
                            name: 'ФОТОТЕХНИКА',
                            shopName: 'ФОТОТЕХНИКА',
                            url: 'foto-market.ru',
                            status: 'actual',
                            rating: 5,
                            gradeTotal: 2716,
                            regionId: 213,
                            createdAt: '2004-06-03',
                            returnDeliveryAddress: 'Москва, площадь киевского вокзала, дом 2, 121059'
                        },
                        phone: {},
                        delivery: {
                            price: {
                                value: '399',
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
                                        value: '399',
                                        currencyName: 'руб.',
                                        currencyCode: 'RUR'
                                    },
                                    dayFrom: 0,
                                    dayTo: 12,
                                    defaultLocalDelivery: true
                                }
                            ],
                            brief: 'в Москву — 399 руб., возможен самовывоз',
                            full: '',
                            priorityRegion: 213,
                            regionName: 'Москва',
                            userRegionName: 'Москва'
                        },
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/209514/market_Hpins67UX0M-6XZGr3SKzw/orig',
                            width: 246,
                            height: 300
                        },
                        previewPhotos: [],
                        warranty: 1,
                        vendor: {
                            id: 153043,
                            site: 'http://www.apple.com/ru',
                            picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                            link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210364&distr_type=4',
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
                        number: '110-4245'
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
                        longitude: '37.566072',
                        latitude: '55.744632'
                    },
                    pointId: '519403',
                    pointName: 'ФОТОМАГАЗИН',
                    pointType: 'MIXED',
                    localityName: 'Москва',
                    thoroughfareName: 'г. Москва, Киевская площадь, д.2 // ТЦ Европейский',
                    premiseNumber: '2',
                    shopId: 955
                },
                {
                    offer: {
                        id: 'yDpJekrrgZFNreN8PzivVIULyY87XMrlISSBEswhV_nZhPhoxidxTw',
                        wareMd5: 'tZ3YHTMSYU4eGhU_seWVUA',
                        name: 'Apple iPhone SE 64GB Silver',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcua9wx5Fu86kYcIf5I9Gslx8B-KNOS3Np_4Kbh-BcGodgejbBvy-9dNaQSAGnl_TaC5aRYxDgda2feIZRk1WKcOn6NdTOGeDVkUXbKkM8L5cXrjD6B6bccEo-iHzxvzl1cTBgxIurKt3-2uu73aPtSdYHwUcr5jTBAIdtVyIIx4_zRbOO-s1A8ngMf4SQT5lyG7AlcNYNvsmKAV2m3gCHvqOptF58OvyI_2Gip32LRF9vIBwU8OSXqgY8CpeAW8zgg_avQyylct0yVsAM6gXUUIwPZOi66XvSVEogT2XUL87ZoEf_zQ3DLqFJJUUVwfE8d6ooLTgcPqjDd47q4FB61NhSImkkSxGSrjb3FFlcnn0e9bY4IkDzt_IesEGVXjsKOd0oJlwuZxDQV8RLomFwR-igLr8LIdUcO6Qtdg0x8YPnWfuZA63xPhjHUM7E4qIa8bAKZf2Ho5Tm1XB0IfeGcoIZEtBHWii33VEGNjAex3bmyIz3t2fPip--zTNsLXqupSc2iewScKwWa0PO7YE1yV5VbNN6Lj6rn--4MIujKr8du8KaTSAU8dmiiGMQpBQDguQSa0UIZlFCBcPuRZ0mqMjjhSs59piJnk_jKcmFuEQHrsJbmhqKG2OYrewnAFHt-782fZoBOWdgePils_YgeTHyuWPJxYC121HPZg9NefhydT5HFod9t1F-Hza65jeya4NQyoPqMQyRILMyB926Bk9d_Vg72frBIofPnVLWr7le0K0y7fJPYfAPNXnwC7C1vkg2ykV59ursgsMDNl-J-E7cJyVEXQjN38rh5bBCdQwbGNIMhiDfe9yg7Gc4zapfmT_oOsgtrWVUHwicX9rdpe-jFVFtgXAg,,?data=QVyKqSPyGQwwaFPWqjjgNkzRjoOstLIO6IRK5gdDYhT07Y_eei45F3cETnPsMdv6wDwpUqDOrnIVrq-mYTcxlGbW_v96F1ybVzcFjlCGXHtJjKbCgiUy-Obnq_h9hI8TZnA4XLSYOUW-1-f5JGu7xjCJ8HFeW-2YzzldXw4YYu8Ulr3N1VyFonGfUzNbVH6zdn1fRndBDzxWkj-ma_kYcEsrVpBUagpCXqiSGSswHOI,&b64e=1&sign=d3824ca64af7dd8808c5bb07e50a9155&keyno=1',
                        price: {
                            value: '30500',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopInfo: {
                            id: 304604,
                            name: 'MrDroid.ru',
                            shopName: 'MrDroid.ru',
                            url: 'mrdroid.ru',
                            status: 'actual',
                            rating: 5,
                            gradeTotal: 287,
                            regionId: 213,
                            createdAt: '2015-08-17'
                        },
                        phone: {},
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
                                        value: '350',
                                        currencyName: 'руб.',
                                        currencyCode: 'RUR'
                                    },
                                    dayFrom: 0,
                                    dayTo: 0,
                                    orderBefore: '19',
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
                            url: 'https://avatars.mds.yandex.net/get-marketpic/212340/market_hGYpflGs6QSVv_ZJ3Kbc_g/orig',
                            width: 600,
                            height: 600
                        },
                        previewPhotos: [],
                        warranty: 0,
                        description: 'Apple iPhone SE 64GB Silver Эргономичность и мощность Айфон SE полностью идентичен своей предыдущей модели – iPhone 5S. Единственное внешнее отличие — это наличие цвета «розовое золото». Однако не стоит разочаровываться, потому что начинка у iPhone SE идентична с модель iPhone 6S. Стоит только взять айфон SE в руки и вы почувствуете на сколько он удобный, размеры у iPhone SE позволяют в совершенстве пользоваться им одной рукой.',
                        vendor: {
                            id: 153043,
                            site: 'http://www.apple.com/ru',
                            picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                            link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210364&distr_type=4',
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
                        number: '969-1129'
                    },
                    schedule: [
                        {
                            workingDaysFrom: '1',
                            workingDaysTill: '5',
                            workingHoursFrom: '10:30',
                            workingHoursTill: '20:00'
                        },
                        {
                            workingDaysFrom: '6',
                            workingDaysTill: '7',
                            workingHoursFrom: '11:00',
                            workingHoursTill: '19:30'
                        }
                    ],
                    geo: {
                        geoId: 213,
                        longitude: '37.592824',
                        latitude: '55.794143'
                    },
                    pointId: '433895',
                    pointName: 'MrDroid.ru на Савеловской',
                    pointType: 'DEPOT',
                    localityName: 'Москва',
                    thoroughfareName: 'Ул. Сущёвский вал',
                    premiseNumber: '5',
                    shopId: 304604,
                    building: '1а'
                },
                {
                    offer: {
                        id: 'yDpJekrrgZEu_MRT-2Xo-ouUyBQaCY062Qpc3Bdy88FwdcY9V_NYCpus5sH_6aMmUvTZIIbkNI6uvojo8xZ9pPnO6OYE25G4SN6792Del4mdfmppCOsCPMowREi7FRt7regr3CNwzMEDd5ftq21X12xh1bwZdDbmBD9HK4d-Nf5oKq2Q8NvUTdOCiLS6hv5jYrlafOt9nu2kwkiC4RE-CJAFY1njpUYHdYZGxuhGbVo7ZUa8HR__BLAAFfBIHsW4iChmdVH1FadUUQrTkVkKmwcTNN93NpyMH7AnQf_nocs',
                        wareMd5: 'xPoSNJwjZ_unaYxhjn29HA',
                        name: 'Сотовый телефон Apple iPhone SE 64Gb (A1723) Gold',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdy2Bmu3E-WyXK9kO_xqWKOaeLrqWdFgf1AiZXwMfqcA2WmxOesGL95P_Z3ny_p5jY1Ql8Ps8liSo_LBaeRw9L5h6mI-8Z3p8bO-lpMZSYe00jSLRmP6ufQglCyzcxoo4DfnPis3Mrf0BCOTQF2__gSTb5FIoWFtx9jRv9CigjkOTopeObG35PKjre4MSIKWZicuwdkcswQchZOVgUahZzda8CS3ODAzhhuUsAdAdFAY2WKgjwIFJI2Ncsf1cCO-NS-e7S_KpMWrLIei0bVM4mFIIMyjQmvUEcsfDu9DZ0rJTUAyi7bt6vCV4gA4kJ6Ad4Pm3AldtifUNDXQ5CjlyuvA8R5Gy-P2uNHByRtJIyT_mGn4WjYLGAzBZvUSNOcQeBm1BXxu3YJdPx0w-0TtcFMuDDm6KRuq52x_Uaxh8npJPQZubYo7GJE3cA2NE32W64TJ_FI4C1JY0O5890fEeCMDjpAt76OcTeOXGDIdCxxylrYbEsRKBa6xGP4DIdJm9P2Ee_LJE3ghziB6mZkK1olfv4zcueKDy5LunZzGvPmpdx47b2x3nsG2HE7PONm3wVMBHF6MIMFdVH0cieYZNhILsm1q0DgA2IzuZNenBASjeH3jusr4F32kEtlk23Vd_8bdYhgaFpGhrCLwtHV7nDnd8Eutffk7TgKGKEc-Z8OBapze0A5pol8pgyW0hOYbLjPHlGHkSLMm6CddmaAGFgY2QwHJfpFFptNkSJnM2-jDdYt_MxwuVykkzcq9yV1MuRAPJbs3t7_6iel0AzUvkqVKBp8MiV-SugniDI0auiztxFB3GBr3RimuK7g2MXOoGXJrPXoNUhE_QslGRFivOqpaq6IGvXeJLA,,?data=QVyKqSPyGQwNvdoowNEPjepPkxt1Vqcos9cWvPI22tg0wCjd75HgObocn-VkR-9ty9kgea7JlqOTwblPNbhRmYOk-VkKqKpb4elHvNXE3bpM8YPk6Wx-gFQSjsBxqO4kTqgEyfJvCKkLSmf9sCpoINsOI_ccfbIYZGBUqfFlioQnBuxMyaOeP9RR5SyfwzNK96RnWBUQM2Fmv_rUx_sYFZk7oPp0W9l0Trdwu7s4L6yTnvuxDkEJgUOyldm-57s9wgQP5tMtRDanSh9Yjh7IcxNLEnwRPWMOY_0xo24CLjUWgxvgsfy8SxYv7bDuxtbJYxUW08qEu7ng6-1kZqOtayNx7P86DsQpKnyPVS6VVkON7LOTxytAwozmE6V0l7k5lZgaoqKDGJpjnKnVO3IQjQf_dPeN8XQUoM3WxnLAj6c,&b64e=1&sign=dc4b317bf6e8ed88e09e5a91b4d946db&keyno=1',
                        price: {
                            value: '25789',
                            currencyName: 'руб.',
                            currencyCode: 'RUR',
                            discount: '14',
                            base: '29990'
                        },
                        shopInfo: {
                            id: 6537,
                            name: 'БОЛТУН.РУ',
                            shopName: 'БОЛТУН.РУ',
                            url: 'boltyn.ru',
                            status: 'actual',
                            rating: 5,
                            gradeTotal: 6073,
                            regionId: 213,
                            createdAt: '2008-03-14',
                            returnDeliveryAddress: 'Москва, Марксистская, дом 3, строение 1, офис 413, 109147'
                        },
                        phone: {
                            number: '+7(495)545-4227',
                            sanitizedNumber: '+74955454227',
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdy2Bmu3E-WyXK9kO_xqWKOaeLrqWdFgf1AiZXwMfqcA2WmxOesGL95P_Z3ny_p5jY1Ql8Ps8liSo_LBaeRw9L5h6mI-8Z3p8bO-lpMZSYe00jSLRmP6ufQglCyzcxoo4DfnPis3Mrf0BCOTQF2__gSTb5FIoWFtx9jRv9CigjkOTopeObG35PK2HSCTPkhON9iS_ygWuHBqbOM4R8mbhQacJCK5zl2627hIXpx32SJIg7TSrJHmNXWC6YqvyMc2b9did6-4AMExo-qQZqq9VNbHuIrjLeyEw2EbWXEUyNO4GWPkICguz3wtCrVqCIQqSN77LvI1Z_sZEAQmd9lEuUPJsLv275pLcY5HM8mJ6jjBPmN5rcN2CXR_whHBQXVjEQ1VQuy8L3GTeP4cOhtdLALCeQdrYgCKoisPAMlODittgyi2AULAV8nmxCsFg2foTqujl1FQi10POLh4ITFARxkL_afGCObsR0IRivF9SGI_Dvi4nvb3R7P4eLtpmuL4cHjLx8A6P9e7A9uLkrsBci0OQCQj5XFR-34Km5btPowcmL7dMp2GqEXbNSxsZBLS_bGwM82mGNwqesiOuAgIt5No_ErKRS2zSuizkdUPGX7IRj2wnq-ybQuz_g0XsMKegzXymmlsVFQndOrU78HIioTbJ82pj0OuNXup77MmG979tx7ha1UwMB9dwdENfxlzHcY70CizD1wwRsoYCKha4wlXSTPayJ19hcSLkGH8UyHAfpf7HE1C9TyJWmctOp5DP1b0k4vf4h77afhz5dyli8Yf14z97NRZlR3f40ROzT49K09EykLWxN3zEsgPNXNzBgn0dQ3klRhpot0LmutTF_EiBvHoPnGnWw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8XU6hWOR1FznyqR-6SyXigaZ8aHpKFCQQg9hxk2xZ5PFvmJe7aARF_-aY3frYyxm1F-5eoiSKhL09NHu7gV3aH_0goF7HKBG9f9Fmylb5mGHASpLiWiXGJ_ZnrfijzgZr_gPpFxejVL4SoOuGXLJJzkO1vuPHRngFF4vMs6j_Nsg,,&b64e=1&sign=9189194317d9f5273f48f147743b5f0d&keyno=1'
                        },
                        delivery: {
                            price: {
                                value: '399',
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
                            localDeliveryList: [
                                {
                                    price: {
                                        value: '399',
                                        currencyName: 'руб.',
                                        currencyCode: 'RUR'
                                    },
                                    dayFrom: 1,
                                    dayTo: 2,
                                    orderBefore: '13',
                                    defaultLocalDelivery: true
                                }
                            ],
                            brief: 'в Москву — 399 руб., возможен самовывоз',
                            full: '',
                            priorityRegion: 213,
                            regionName: 'Москва',
                            userRegionName: 'Москва'
                        },
                        photos: [],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/247229/market_i5rkkFNik_YT5KfFOrgmNw/orig',
                            width: 591,
                            height: 538
                        },
                        previewPhotos: [],
                        warranty: 1,
                        description: 'iOS 9 Тип корпуса классический Управление механические кнопки Тип SIM-карты nano SIM Количество SIM-карт 1 Вес 113 г Размеры (ШxВxТ) 58.6x123.8x7.6 мм Экран Тип экрана цветной IPS, сенсорный Тип сенсорного экрана мультитач, емкостный Диагональ 4 дюйм. Размер изображения 1136x640 Число пикселей на дюйм (PPI) 326 Автоматический поворот экрана есть Мультимедийные возможности Фотокамера 12 млн пикс., встроенная вспышка Функции камеры автофокус Диафрагма F/2.2 Запись видеороликов есть Макс.',
                        vendor: {
                            id: 153043,
                            site: 'http://www.apple.com/ru',
                            picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                            link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210364&distr_type=4',
                            name: 'Apple'
                        },
                        categoryId: 91491,
                        cpa: 1,
                        cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOXVa_dnEzKn_M-0AEyIXvWpVj7V-5Ozhiaa8UeH02jopBbv6tRdrStKmoGp-clo3cyLgRc-5H_AGJraLb8dDnS8xa7-fZOVr8-DeeoEmcqPoeQ1t0HNrQCvxKSo0Dgv_9WgceJjgSZN9v8VVqCYjnzukoxM90zxAKkhENawrOiTlAQvKnXtmwdvmMYvQ8Z168yY-Dd4ehbSScG3KiaIw_rrDLkpof4eeYwfBkas1_3o0oWDb9FHZTUAUoctHP2inun9TZht8CXZy5xTbSfQHJasQAenIefR_qHCPavVxOH8Ib-J_TUcZ66xOZE44Pc8WFRLirrjxO5q0Ry8TD8EUfQXsXpxl44dLBDJKQtyKWuQDKP0ZhTrMyZxd-xCPPtrvbH6J2P4lxDSwc9EoBiEU0LEX3eHBloWdCC7fp6LaP48YrnhLD6UAd3CvAbqwp6zHfXxQGWG_IetlTV3a5ApSq1OCJXMvkh90uSvAtaRvw5dtZWe2LnUTCVEcEKvbWfdV9fnsDasEfyamqm4kQfjzEdFyy3_Z0K_vjvMHLXJUS2yct8Nkn_cIX0LG88lBtrvdAhx61wd1P2AhjvADaaKZNQc0641JNF5HjcSUSZGZDPDYOx4SCZv5y2DI0TTgGtzTTlb9AvgJgbRvmef5iNlHItsKCdOemYRCHOcwIQJTnzRyfPtUFT-EMITYHDtylXuvJGs47KpLtQkKb6cq5YkrbjHGldwl5jQo0N2acULWTCxV-jASYILBxS3CqhH1K-rqN5qBUMISCxnOE8IicrISvB0I7nwLg0PICM-3vMaJT4vejO-uIdLrH3WmYJTZCQOHKA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DENTjVaICh3FdvS9rtSpsPBE14GmavAxGFCzyeNv4nPXzcJ5NNa0pOM8I9j6sujBh7a6FKxnY1rTVcqLMyKC-ZGXN6fmL7lEDRyS8LoU2mf6rXdgkSkQr2yNGMT9OueTohjZ5RBxNk7IE__TH42tjogiJvqJRwZpRRnFZWx0ZPtcxl3VqsFtQys5vRYtL-zbm1faXK-Keut_vccoQyq6c30TxgIJv6dzUL_Zu3wW77vQpFogWBe5I_NmYIZS8RG4GohDGajBSVc4vW28sEvza89gch7mBO7IxSd0Q74TBJBhavO3nHsrfXLkJh2ohKrSzMMxZ4LW0kv3_2fW19RQ4GPkeu5AGFReovBapIj7DTAA,,&b64e=1&sign=c1935e608143e6c9f8dc47f735f585be&keyno=1',
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
                        city: '910',
                        number: '000-5823'
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
                        longitude: '37.661635',
                        latitude: '55.738333'
                    },
                    pointId: '272300',
                    pointName: 'Метро Таганская. Марксистская БЦ Планета офис 413',
                    pointType: 'DEPOT',
                    localityName: 'Москва',
                    thoroughfareName: 'Марксистская',
                    premiseNumber: '3',
                    shopId: 6537,
                    building: '1'
                }
            ],
            page: 1,
            total: 12,
            count: 10
        }
    }
};

module.exports = new ApiMock(host, pathname, query, result);
