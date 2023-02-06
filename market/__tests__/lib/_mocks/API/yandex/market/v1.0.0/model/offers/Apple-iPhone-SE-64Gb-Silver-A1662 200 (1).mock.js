/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/model\/13584123\/offers\.json/;

const query = {
    text: 'Смартфон Apple iPhone SE 64Gb Silver A1662',
    category_id: '91491',
    price_min: '12225',
    price_max: undefined
};

const result = {
    comment: 'text = "Смартфон Apple iPhone SE 64Gb Silver A1662"\ncategory_id = "91491"\nprice_min = "12225"',
    status: 200,
    body: {
        offers: {
            items: [
                {
                    id: 'yDpJekrrgZE_ZPb9iynT9eQYjZxb9LzWyaE-N7qVDN5iuL1VSkVIXFCycRGFYhSznhbhC77Bkqw3Q2sfikKAgC0_8jMCjqodZorXk0XQfxWTpHEZYZur9tHD8-wsX-LUrKkX940BchDbCUAr2f_wf67dT_jciVRTYT5SYydCbbVYOnAWsMJxiC5DsIDuOPIs0EMAx_fvwheWtn-CX0u9ZPmXWoN2eR2ddiYUBhMMwrT3rRarquxldCl-XOz7Zd-p2QoOBNhiuAOzOwahG7oAPdzYijoDqIH4VDzufUJadF0',
                    wareMd5: '5Vy8D520U6SejpLO8bEX6A',
                    modelId: 13584123,
                    name: 'Сотовый телефон Apple iPhone SE 64Gb (A1723) Silver',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mVIzMrWDMnE_032y1FM_cUTgn6cxdLuWnff-i1QZKLoiv10qgkk5pXB5TTvSDSY48ZmkolRmgZ60-PWPPDIJHaAozhbzHeRqMlv9mK7YFrMDoGA0quVXV9ULyXbBuLmPs2YmwY_mRtI6i5fHRu99iJVdBYbz70L90s38m8okrhP1ghHrKxRuDbnbMqm6VoQ80vZaM1tlVF2oacE328wdeOTM7CjLcVgMt-8m30q8QrHHLEp3O6mv4JJcgHfc5VIqx40r-Z7HrGyTdCZqj46GAUoDjC_8rMjb_F0BCgzxegevC-9Mft4KC13nuBJvOsRSvXSX7fWa1WGLSj0pEBxPWfOlnHvIjPTdmunOIVS2N_fmfSYDwK1c9h55QFD2ZymX22L9YOmrQ5HhX6vtNjF2QADT24RDyt7SLieFZWjohUDHsHWAOoJCu4Rqc22b6rAHEIynY8aYNuv4rnKqmpicgy6CbkJBx4Cwos4fC-KVZYuTAICAs-xsBEpCSdVqIBsKgeDZw-SJAlAh2WWn_Be-G5tarcIcMBNmJnKEGkSb967b3q0HPvZvHdHuTQvkK3d5_CPQIRiNNodzFhiwIC-bQe-jtPv2Cp6VM844PZzBWx6l5oR1tzNxd-M60vtT808VfXuxDMtyRXruTJYuXkdQnnjYHa0YGMVPYHmcE3HAMdNN50ZDjO6hZUobqCIMa7H-rKP3F-pIz_PxHhvgkG_P9jzKQ7nopoHzXxLoKRrhU0J-MxGJt93zjmjFeaRhoxFM0iMgMXH00OsKXw2-r6tv0QX6Nc7mKFxRxNxtY5BDcZwZwVM9BuzuD1gKolJ_9OgzZ8YmiLvyvrDat-GIW_FjDGh4lX855yTHkw,,?data=QVyKqSPyGQwNvdoowNEPjepPkxt1Vqcos9cWvPI22tg0wCjd75HgObocn-VkR-9ty9kgea7JlqOTwblPNbhRmYOk-VkKqKpb4elHvNXE3boWYYl22Sovbf2q80Qu9LaHm9KhGHIXoOrORCCysx2qMdmb1lFy_JkvI3T3qnL1PRuxpuHW0OcmM-8TeVDu1yiTttTfI3KtDh9iIvIl7G2uXdb-9DFsfECRdi1xk_ISrXo1W5zFOan9PdDchtwYuumYoHHmullgBbNrm0UnJyZh4h6A-4nXCJqSbYKS8-kWjLagaD0GeoaJ556J3fc8UAZLyFLk6lBgT8gwFpx-jKRzXybZzpb-oyI9zTPIXyFmiF2f_GJDeYNErz_mi4m6KzTdcaBjYkgKGuoKSVT_2DyOegq1UE2D0VxFFK-FE-INCbk,&b64e=1&sign=7696c16a626ecf195c6a78c12cd2847f&keyno=1',
                    price: {
                        value: '25989',
                        currencyName: 'руб.',
                        currencyCode: 'RUR',
                        discount: '13',
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
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mVIzMrWDMnE_032y1FM_cUTgn6cxdLuWnff-i1QZKLoiv10qgkk5pXB5TTvSDSY48ZmkolRmgZ60-PWPPDIJHaAozhbzHeRqMlv9mK7YFrMDoGA0quVXV9ULyXbBuLmPs2YmwY_mRtI6i5fHRu99iJVdBYbz70L90s38m8okrhP1ghHrKxRuDbm9QTHgujvb-bJzrYRFpRT2eEBTOkkiyBes1HncLDGcURhYkcvYkNDBetP8yaAIw1fsupjZzyYf0Stqv_eDW2A5tU2P37dr-tOb0wx1qb5BRIwuCJ_WbHMExfwXeY6kFmUQyWQ-0A1eHlTnw5SjyEUMi5AkbxXI9G9b3rQ5u9Q49AC9TZ6rIrIqC_YezFpR0J2PdZ4KmXbCupSqP5sWHElyPBTgpWqW1TnXITD9adgTBZzacJ6ouGrQ4xhBnhnNLz4dWoulmWhMG_zyM_NdCqdTB_DF3Om8bvlczbGnBHgiY32-fOfl2h-qI3K_Y0APHkoHCvIAmgN1O2rQ1vJ3GlUKoRMFrnkRBroPuaUw4xFc8WQAz8UDXyAZnV6d06y6Z-nJ7-L76jmN31b51iIen6riUqViICxzv7xPDwvSfjWERRN5Q6w1Y-EDR9F8Ts8ON2wChwXqleMjgWlrSNQHnuv9KJ7TXm6sBDRvRKnkyvznkj_PvaiDUr0DPKFYbdMuOFD8M8ieuOqGaSi0v332D-u_M--AcfKts3zKEfohZFzfrxUtO63jEX6rimWrRDN4Y7dGvr8lAtIFZKe33Oq-bOZaiik_WTHfMQfIAmb9fRl9r1nq48YIzvahWukuyH9KsZJsOW8GO7-o72ghfB2QI9Lhv7Fnn7VSYSsDHjMifyqx8w,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8EEdLNMAQxAV8FpSrWRnlHGIx9XpK8adYmoHqfJAKF8gjPTbkN86TUdBXLdEy5psaiqbpzsQhpEYxGrYu9WGKae3xAlvl_dMoGlydD6buNynfkat2Ex5QjxKM0rPSEDvjG5qmGX-hjBJmdloF648Uju3KEv6QzpMV3FDwWAnKviw,,&b64e=1&sign=09f83211a11ec974c75cba45a8f36083&keyno=1'
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
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва',
                        brief: 'в Москву — 399 руб., возможен самовывоз',
                        full: ''
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/167181/market_LtMPbl0BIpQ8y-Ckyur9SQ/orig',
                            width: 246,
                            height: 373
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/167181/market_LtMPbl0BIpQ8y-Ckyur9SQ/orig',
                        width: 246,
                        height: 373
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/167181/market_LtMPbl0BIpQ8y-Ckyur9SQ/190x250',
                            width: 164,
                            height: 250
                        }
                    ],
                    outlet: {
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
                    warranty: 1,
                    description: 'iOS 9 Тип корпуса классический Управление механические кнопки Тип SIM-карты nano SIM Количество SIM-карт 1 Вес 113 г Размеры (ШxВxТ) 58.6x123.8x7.6 мм Экран Тип экрана цветной IPS, сенсорный Тип сенсорного экрана мультитач, емкостный Диагональ 4 дюйм. Размер изображения 1136x640 Число пикселей на дюйм (PPI) 326 Автоматический поворот экрана есть Мультимедийные возможности Фотокамера 12 млн пикс., встроенная вспышка Функции камеры автофокус Диафрагма F/2.2 Запись видеороликов есть Макс.',
                    outletCount: 5,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210364&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/5Vy8D520U6SejpLO8bEX6A?hid=91491&model_id=13584123&pp=1002&clid=2210364&distr_type=4&cpc=l9kwelcELdrTrAeiJfSlhpAlb7f4wndsDa5A0fZGll7kSNobfjJomKQR0la1g6eD_7s9rCMphqRpxED0YR9C5F-gYK0RA6JG1akh2osvQeXKkmmfXW-4IVmeC6CvHZuXSXzToh-dJn8%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgQsQ_BvbAzlpx2U-mctosCef14_I9e0VCsa1Unvf-6E_8cXwNDuZWOFDWW2sQ8PK5nn2g4DRpcw8uAp10ZWihmB4stxlKi0btYTtZkOgxWkgYjOZ8HxD87r3CylxBSCnqzOhGCcPONdCeRf2VYApFXAzwKHI_W5b4As9xuefiXxkYEEQT854YdGUtJsj27HKWLGxDJGBRKbY1H1HVcvskMVmmXx4HTRvKpa1NXT-1BI5I0VPwGtUpTiLgf-o5d-yWOTNkTiTqP01rr-ZpKqPR_gK5Cl-jwdK6uFXlbSbR-dXD4vGPDmuDCQvkpWQSqzl663u5Lue8bAIo8T_6Vmi9GU5stL8VAQkY1e6cpwwL9JXYg-0cJJqKBSihWvE6He8xELJn9WsRZXX514mtv6VA3x0J34Hi8k4EXcXklhoyAr_OicfLYOVfSXV1NYZRx8Bib3IY7Csajc5Nmy1xrVBVcSTqIc9kdp37u4Ocl6pYxZDkzC4mO8izf57iAcbXqcujWarGclTgl7ru3jCXg7cs7wkkNlvM9kaDgL3sOJbcwhAZ_u46ALo0-BodmT-1CK4QUyt0QukF6c9aKCoVKyf5k3Eg26UBKyw8r2gAhvOY7fMp6DThjCLW2Jd6l1dQKk-vt64QmGqRu_KAfLc_f_rCwq70rPgfX7fr8UdA6gyGzSxGHftViqAlXoVAfdrsmSeSeHAYftfBK0_SlrzlHAQCcvwJ2ZwRE1Rk0ZGvz0lq4VsfcnrRUmlOcFGfx4VADXpnKsTmhnJFNMbZ4tgo5aihGyGARuF1aw6XHPqDQB1nlSEV8zog-SFw3iu0mM4Kh07PhxYsvS8Tz6PM9oRs2aJfQZJN86nnG5Gm7YoOt_4tKlIC-YEqE_m4jFhaSh0RGPY6w,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DENTjVaICh3FdvS9rtSpsPBE14GmavAxGVWvzEJgPwltXPLPT4wchqAUmBXyDyma3DKq3azHlZqD2ppnFWvb2IXgI-td3wENqq1ZW2l2lGpUhGSJ-hlBGrIRZAO7MIm3bN9Moq1lYalkEVmwJuzvjhK17mtOiWvObTOdfqpjsGRgqnSdU1byb75MrF2l9mlLXgHi-vkD_DyD0DrX4LJ4XCk1L1sVmnlGZRo6FHcTLzBiTcWkhMHFo-U9l__yWMFcVr-VSm_QvOuRd0tVjbWNsLtVn7EZRrx2gwT9Mlcf06rIwyH42Ww0a_Zq1UvIPqWsPJ0Ebvgfn-iaqncMEKkBBfDsLGmRfJ8cNb7qDFvEdTnw,,&b64e=1&sign=a38b83f28b6af2405b1e7b9b74e225f5&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZHhpLQrPyL817Y7bVCuPkC-pnxnPrx7uZfjmHhHvhA79PtYqkp4J6rWK_IzauKFwA75_mO7T32-Mt3fxYiu4nXiOcTW9zQeIkJ6y79zmVKZCu0Fwpqxq9uyBB0KUpy70trUoDFzSppsTq2W08P91ZB_xXTt0LkxaiW1h0oNNmEnnyLWYOoK5uTPgnQC8VUJxMdNnHLzZ6T2T41weVwyZMrxIcqEEclbcjkTFAqLgOCOnEMBaJTTq2vtp3Y6Ntvn6x3lpMIuDgim50oS8TweRl6hyyeL8ldv8Xo',
                    wareMd5: 'zsnjbboT8UC-dIcb9b5TDQ',
                    modelId: 13584123,
                    name: 'Мобильный телефон Apple iPhone SE 64Gb Gold',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDrVkVBf3oEggdn5pxzG81KdDpiPLxSinj2TDFFWrmRuaQ-6a7A-pJzxZIdYt-dIEQGsZ5rXGgfws_A1xqyezdLg7hb5B9AvO0RUD8n-5LD7qSAkeZX986WPC2p_aCEoRzP-sNWr7zUpRkiNWjMDBof2znmi7n2mx_lZe-as5OlCn0RA9rFIspTK6W59wTL1p867hL5OJ_zrl70R835Z30iZu-t5kEMTS8NJtHFR02u9QNXsowXZBs2REYhRmsbbVpqNo1d06Eh7DVg8UzcpuVEcngg4GUYdNr8hKO0gCv2ljyx_C90QlvF9tLkNDGtE3FnxSPk0oSacsSKQGJTZ2rMDHf3ZX69BJ0JUS6kkACYCTxl-dZh6w4tFISH5wAH_yhpA84YZKaj1Jc1yaOUkU54o2_rPixT1lERBRW4EuEH97JfjYHwQF2oELeJ0YZBLer3dAmei5FDCjECnQO5KOSKoc1IV2PE4VOXHhpXMpI2_NJMR2DvEJv0QZ13f6QWw-EbP0ZmW8IIWfKaw1VRr4-5tyRuebtWP2vCVmvkc-sETAKRqwnOLPiP51uc7cchAa-kzF28oSBdNrOw0PTg-3kSiJZEKSBRXQ8qgPbFl6NRTAOv4XhhCAXULHeWN_qeupLLJyDEFPyyTWGOMocUFywP7pvWE0SZL-4fz3LJjCCbBUs7gQY9B6n-7lY3IIxxLjaCphWR9-mZvzear9ALYu1LfuUYvpjgLdO3iEGxduVXeJw,,?data=QVyKqSPyGQwwaFPWqjjgNg9J8lIJ2Oz9gk_rKEXyxA3xLCCap9MlHYLA9QmCyiEDOb9USHfWBqukiqjB5d1CJxeYsPsry0r7HNLe43liS8BPtnJZp7Pb7Xafx6vVulrKZhrMom7lkoUQxl-YJ-byAbUSIwfivGCldL77hVCxkK4vdswgpR_gQMlaB-vCLNzDlr0kJb_WCkhiVU1oEjH5MmxGzsLuSm3rqWRXcurpNU-1zmKgoBjfmcarKaf4x5yv7uhI55oZ75DeOzhIOUp0i8HL9MqfLnGssOmYaoGjy5DZJKb71sLrwcNGFZWy1765UJNpiGOoyywhRyFlXrcrrdhGQX0ZM9l_xaeLCq5UhZqVtvSDTKtJISjdTGJvC-Q_6JQNXIz8V6D8fnA7STtqdHyzir9tj5_DK69bVB3X2Da7qtjpZzq-2oknD8HUs_eGvvVan1FGUXFP7Q8FC-K6awP6rSVe85Roj_767NMGUZ1bWMi8652cJafk0m6exc4_Dk5OHSxf9pthS1DwfODxsZzYRBYVRim0j3Ksa1myE9wMpuFxsZjGHDr17cIp33veT4xRnK0wRe_xeqSvCx-6rqku9KNBO6hhUKU1I6yI5yuqeqrNbWqR51NbNqb_VY8Pan-AO11pDHbQ8qHu1sZxWcb0xtfS-wjzmJEb6DKKMUYQ-ec48qPlmhY4fDvhw5Ko2AZHkycPXvfd1Mp2XaOngQ,,&b64e=1&sign=0864e4e092662ba069ce07fe28c98f3c&keyno=1',
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
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва',
                        brief: 'в Москву — 390 руб., возможен самовывоз',
                        full: ''
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/331110/market_X1iWnCeN6lBRRnRoxyb-sw/orig',
                            width: 320,
                            height: 320
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/331110/market_X1iWnCeN6lBRRnRoxyb-sw/orig',
                        width: 320,
                        height: 320
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/331110/market_X1iWnCeN6lBRRnRoxyb-sw/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
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
                    warranty: 0,
                    description: 'Тип корпуса: классический | Тип сенсорного экрана: мультитач, емкостный | Функции камеры: автофокус | Аудио: MP3, AAC, WAV | Разъем для наушников: 3.5 мм | Спутниковая навигация: GPS/ГЛОНАСС | Диагональ: 4 дюйм. | Объем встроенной памяти: 64 Гб | Число пикселей на дюйм (PPI): 326 | Аккумулятор: несъемный | Размеры (ШxВxТ): 58.6x123.8x7.6 мм | Фронтальная камера: есть, 1.2 млн пикс.',
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210364&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 3,
                    categoryId: 91491,
                    link: 'https://market.yandex.ru/offer/zsnjbboT8UC-dIcb9b5TDQ?hid=91491&model_id=13584123&pp=1002&clid=2210364&distr_type=4&cpc=l9kwelcELdrDrSoAlTSGoFLANYnfz9sFwv-bmJhxFw0-vB66Cta3RW_0r-rKh8UOmg8OOUg4I9zdtEErcSHyuDNgkUK6ip5QjrXFvanRa0RV9cw9T6PxnNDhhX9ocIj-&lr=213',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZFhEeS4pWsCcLBEkLNFGMaPDbVR9JmyldRWEY5nEUA1Lg',
                    wareMd5: 'PuP3JUrrsKk8jsJPuzziSA',
                    modelId: 13584123,
                    name: 'Apple iPhone SE 64Gb Gold',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDrVkVBf3oEggdn5pxzG81KdDpiPLxSinj2TDFFWrmRuaQ-6a7A-pJzxZIdYt-dIEQHIAqWs8LbPu_3277ldKcfSSobyJrp_q7hLMEX0V-mpgyF6OH4aAE_Zv45QdlGsxKRPH2a3BRoPFwfXU42wMhxCijTIwUaVFC7YX4MjAMc0KZri0KIPSdT9zs79E_B7vRtBpnCph-qu1rHG8HlR1udQ1apk-WoMgUCXVEP6UgjRoBkn_K2ZVg9DRtGRSBYe6fPCDt6zA-B2j_SuXDz3uJM-F_eqxXi8tokEogKuRoEWckVb55TZiOrPTHps2py0zSyTOb2jNa3VCXlg_B5T9eMyx7Do3txjgoJCUVj4jkB9jP-QaxvBbNGxG774S0Icd4EyDSr964zSLUCvWEhqwz49At1LgmNjKsBouFykXVbzUh0YCMZi0_qTXtD9sEmkywtytkMQdHw9iYQqEqZmIybbs4xsKkERMSu_hMoA_kxzbs6OKaJMb1D3fzXXEcrVDIxWQ3c8DMAps6stEpi28ha_rK44rQnNM9tqGUo-3g7fRxeY_vhT91son9wztw41_iX0oQgvsDL6G23xetyUOke2S8pXO6Cl3pBAW3I2SvwO0EXUFeON1oyEO91KFshXM05hDblSsN7Hz7R2GlJ0u6e6YuoOeloaIlpgvn-JejRgeeWM9pjoDeQKRd0mXGEjwqrG2ZB_2pefnTZmxAe0B7kkcNyr9RA4qimRFPvl9gfmpw,,?data=QVyKqSPyGQwNvdoowNEPjQoU2CiJ2kh8q6CahX4QNdfUG5in7tF9eddnvy3FW5ZA7IW8rgTe7lq6mVUOatIyshYZN7UzAsqcrF9qldgKtMjDwMkkILG8Mh4scvm1GrSkm9HLsFMxIdtvvxJw7wZ4HTmBhfUdPnvyF08rpCUpXI00DAWX-u9ELyKiFTevjRWSWuv-TfPpIeKvwtCWvTcSNPH4o66r_AupZXEB_Bse59F9k-xcUrVJmkWgHYLpz9cNZ4edXCuKyjppl_4j8NPLXshO9Vm-B_UxnMRQQlVBurnVRmwDA2Tz9dCs2UfoG3ewKxMg0-FOiF5qU5RlJXZfEYkPkMSygdD6z9F-grHg1N7V0NajZ385Cy1BPzQLsBI3VNRsEmqI0xJWNOmUnsbetQ,,&b64e=1&sign=f9e681382e758e04a7a9448f34c67549&keyno=1',
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
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва',
                        brief: 'в Москву — 399 руб., возможен самовывоз',
                        full: ''
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/209514/market_Hpins67UX0M-6XZGr3SKzw/orig',
                            width: 246,
                            height: 300
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/209514/market_Hpins67UX0M-6XZGr3SKzw/orig',
                        width: 246,
                        height: 300
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/209514/market_Hpins67UX0M-6XZGr3SKzw/200x200',
                            width: 164,
                            height: 200
                        }
                    ],
                    outlet: {
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
                    warranty: 1,
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210364&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 4,
                    categoryId: 91491,
                    link: 'https://market.yandex.ru/offer/PuP3JUrrsKk8jsJPuzziSA?hid=91491&model_id=13584123&pp=1002&clid=2210364&distr_type=4&cpc=l9kwelcELdrMD7zPvjjWb48jrgoQASgVW9CqzdCNB6gPbBDTKoybZvBheDIwfU-_XTQPNG9PInQVoULHBIXNm-5Zu2iYrWhwW3dzhi7XpwgRmHUrYjRGO0ivXk20mvn0&lr=213',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZGeiJg8l1_Qna6q2QdpKoTHDJvvPv_1jKXCs0CBJDYFMCL5BEhS2B7DtZEkdmqn10zLTAZXWFhtxVRkstn2BLHMOFsh-8rACnLdUr-cpPJgr5af-7Rp83xBPuX1T-f6i4xjMah2NwNV3rcTcKhS-mLdw57bCqcO8BUcD1bnGRbbRkblwndvbJciy1qiqBAiWP_uDg3Cv7hL3qBgFMylDIrI9Nx63PjnR0F3Nb79JjDepJWEYg0XWyv1Y2jZUs4fa3fqX9hpsOlwOOfDQ078oGbUvtd5nUXfXX4',
                    wareMd5: 'ii10gvYsQiXyUbs0k4PaXQ',
                    modelId: 13584123,
                    name: 'Apple iPhone SE 64Gb Silver',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDrVkVBf3oEggdn5pxzG81KdDpiPLxSinj2TDFFWrmRuaQ-6a7A-pJzxZIdYt-dIEQENWYfGsjok4nRwDUgtUoLU_CU_tpUCk8B2TyJ_yt6GIh2DXUPSTgIrmOFI-l7WFtyD9EDmI30bVb0TSyx3RUYNi4KWb89FJ5cKgNm7Bzy2C_4mFRW2EQYqRlnk5OV6TtZhVLnkyEDas9QfqfjVXeg24AAU1psnpJA2_5Ci2TiUE7TgJ_2ZgmiMEPWW2O1gycIdaYlAktXuf_6Vt-Nlrb-F3yJ-aXkRdGF_gUlCqHgWwi0scqTNQ01jJ2EifQwJ_y8N04Ldgrgp7V0P_UGIxDW93ULiKlYdegMHpN4PzRdFDssYDaaX0FAje9FSkbXAmWFGORu4YuMfFR8iDaOI1GMR3feaSp6vUiw5PAYy3PAuEHU9WGBtP2NpzC0wKBc2XbnYGVFFWCqjNCSfZNprWv2nrQX5cEJoaKn9Scu9i7ATLOwZxFGSMndLgAslg22km9rJvueWvY4F5jHc8ziEsjmy__sG2nTPzUWeYCYS3YLUeOB4aO7ZJxEff0nfcw_6G5hOlZfWKu2bJutyrtX5BLbm81xEljiaV8xdZcwtxwtfFo3jUmn8Stg6Q_vcaPwkRbwt3yOpPJwQ8LwSMQmqh9C3sUHnO1Cbfg9BTpvKkLUDQqZQvyffjOqHkY0oAtlottPqH7zWrKLNR4C2bL9RemcI3Jxfe_F2_L6Yk4rONNc9JQ,,?data=QVyKqSPyGQwNvdoowNEPje0PAcg_xXLamMQAyNAGhbkNussUv14IVRoAKVqnrKOILCorrWMPkK2g7AsAsjNpaDjfFD4ASRjRl1SY6ojDUufuqHW3j1A4I-CQR5_qeIZWlusQPU0pGKu0powuYT161uRyqdzMRDbKhIKE-4f1qPk,&b64e=1&sign=0c6b063b6def11177687f1895d0e471f&keyno=1',
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
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDrVkVBf3oEggdn5pxzG81KdDpiPLxSinj2TDFFWrmRuaQ-6a7A-pJzxZIdYt-dIEQHPHyTMyzRmeytjAUMffvrEX3PMBCebjDT2MV-CUOxW-NB87VWz-f3xIANHW54qW4Mc6U4v_ozeALsUvYMkjO4SsTfGFcF5jWe4lPqp9qB4XoUEoLMBAHoeMfGmUgtHcbEDdbe9QRuWec4f--eNDLL06koOifXLWv09rFO_Jhr8vDM616GUYwb046q7uQM5SFNy-erBHjJMQrpG38qxhlGzFATCmi19xHyJ98kpDwNREtSqzOLf43VUxkiHh4ynuBaN-CD6C40QHV9q1cp7qeIDULX6xm4nVOCLrcQRVE5CxuhTgv_OZgXsMNgBAy9zrHxR0vmWJOQsY1Xnw_XnMXtetaFb6Kwhgbq2QwQ52Luuwlmxh7OGkLHtP4CB_TFfskXxHJkCHlqTx6OBdDzXGkEaPcJQnVpWdid3DyZDd8JTZWdsSS8gMiztp4kYvI7Di7mKpldDXyk8iPPJ7xHuZpUwV83ujrOcqBPD4ep388Ai9p_I6ey83CUMsNC5XKm7mP-eshmlJ8hsNYuPnsDFuYsMCHq52k59N_1p8rRUY7Ji9XWbENk_UlvcEjpnNDi1i7KQYnlRwulH9DT3FRgSGi1emxtoCmiuX02xexMrWQ9nyANEOrSgzVTgbnuhkqVotdDxJJbj1Dp11KPNKwwgpK2S_KPHbGL3ZSzQ7eyISZaiGw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_aFKE0fsIL5GA2-PM9WsIeKqpdLlR9rpJC4XLZxeiVIkXpHsKwRoocIJvqh0EbmjxwPt5Y49A_lsNU4hd099wrecoyCbuHRzuKUj5fTAjlXjlxhjoc6BYBnJ-jA9cm6SH3i9XYqrWB0FbxouSsZgFWcjNCSCoHptsJda6YcZXMcw,,&b64e=1&sign=8c1f8af5fab21f03bc0a8c967ca45008&keyno=1'
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
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва',
                        brief: 'в Москву — 390 руб., возможен самовывоз',
                        full: ''
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/219743/market_Q_GPlcDIDgCNd8XvasWZug/orig',
                            width: 470,
                            height: 545
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_9gfWcIpZ-Cym5HK1WU7wIQ/orig',
                            width: 688,
                            height: 529
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/219743/market_Q_GPlcDIDgCNd8XvasWZug/orig',
                        width: 470,
                        height: 545
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/219743/market_Q_GPlcDIDgCNd8XvasWZug/200x200',
                            width: 172,
                            height: 200
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_9gfWcIpZ-Cym5HK1WU7wIQ/190x250',
                            width: 190,
                            height: 146
                        }
                    ],
                    outlet: {
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
                    warranty: 1,
                    description: 'Apple iPhone SE 64Gb Silver самый мощный 4‑дюймовый смартфон с полюбившимся дизайном. Установлен тот же передовой процессор A9, что и на iPhone 6s, и камеру 12 Мп для съёмки невероятных фотографий и видео 4K.',
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210364&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    link: 'https://market.yandex.ru/offer/ii10gvYsQiXyUbs0k4PaXQ?hid=91491&model_id=13584123&pp=1002&clid=2210364&distr_type=4&cpc=l9kwelcELdpoLRJFK_C6OHb6Loq3Vyy_jN4eMmi0qcwfBNT0S5pJ0S1bU1SLfhQIz1gd8uD6nYzP64qv2WGvTY_o2Qir6IxAiRBxGn6zuT-pqpuzH2Cea-xWG6r-u20t&lr=213',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZEoohSPJiZjFaTELJQD6bVN8wGQSHV9qEOV_p5w3qGu86cSim5M5O9ACe9d7A231UooJwAs1oj1DUj6a4DCohBroOaexVwIo2jJ1_le1RPkn-D3l4HfGOdlVTdHu4NlPv3XS8-2W4o1FTagngRzuTniIXNUodTNWBMcCaSSvkJFafvuh2gXZV-qfLWPtsfvwFH5ew_UxcJ4O7EIpWJDKLG9y8oUZciIfhD5PAVL20qWeQhuHzuMFZx2wdcvwjQBaB4zD6M8wrstyAOsey_uG6PyvqCutLaPJ6c',
                    wareMd5: 'DiE1do_ZyD7FdOqKi6uGdQ',
                    modelId: 13584123,
                    name: 'Apple iPhone SE 64Gb Gold (A1723)',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYjdGxZOSJSsGnCHSvLFvUhnfEoj75wqz9r7ijlwvBPJB-u8a0SVMOws3R8ko1RldBY9w-dlo9SDyBPJYTxFRLdQ3pnHpdB7StQGmzLzxap85bRe4UBdwOrB597VI0v9wuMQJEpBVXuToao3vGS-EgixjqUM4eyQQxKpHi-spVZ8o5pn85xFg6wbMlLvFLTK2bBVS03T3ttGspnBtTdlYuaNhXOIbVpXdVCtl5dbq-GRGlZEMrwPfH77ip5Mn_yZb_5tRz3if9cJB6T-Zb7x516H59klNxGGWqlYvefvepT81ALjlUvc9N1NU3zCLs6-hrtBAhbVEiJ8YmdO1_DQZVgSczHBLELj6SOLVRG2Pfa-kqyIle5LOzOqFur63s2lOkpuvGGEJdm5SgCePAq4A-9PCPaQv55rahEk7K_bkpX1Kb4aWLNv4v6OMBgRL7cSuH5Rej-JBWVbZvHraTpkkFDPWsLIMCVkjvi-5UgKQrpf8_HE4B33XkTqARvoJELRdGXM25p_IKLSJclvX26jY_RQwcKVz-tYPWz4Lb4soqorcdPExVpuPQ5Zs3oyHPoB0JqiF6CPQV7H6rOoDnLWbbFlYPpt9XiDXIzNDjy0uCwl9a13H15E2e4Qgu-6Rhs1tseA7jrl5cNe_IB9VdcE3dToeXa_WQuCrwW2jrcoE6FqvnpWkcFyJOxkqF7O2YC5Dc3Bn_o60FHmht3jL6dSwTf-I2X4JCiK1MthF26D3uqR9y4Dd_1BjGxAdd2JRMKM8ioai5O8QMWMsgA5V01b39jUYtOcQSTKsW_kCa-FCNmhYVJGSulSmoveCelTatcGG45bc1oe0CLBC9mw3TdS2zPvbQ3O94JRbw,,?data=QVyKqSPyGQwwaFPWqjjgNq5ob6Jq70TJiGoeif5hpcbP8YccfY02ogWNdXvVXQnUtSG2m_TC7d2bPi2wXHRJMk1-9eyQ9C3OTTJWwFMvmQ01-BEw0MrTled0m_WVfjiSYJ62rves8ppfr5jGT0n0MC0_AZ5JGONj7cyI0xmWDfbRzIffhpRmspxbSSGI8jymTuqa5h7fxQntaHRkcGD0agwxS-lsiis8vHfx-nz9dmMgMBT286SvnIYj8C6lUAGLmHii7hqwT61OslzTbUGE33CJ5lEfMpKmqQPu9sawEloSQp7hAaCtOAOnupx83qhoGEfXX0XwrWpBlNyNWi1D1f8o1FYy9SrEGvrlcJNW_k0gzKVK6zXW3qHie2cmpRV_KEF2YcgADEKuQmLpjHGO-TXHIlwKlJ60GsfvtBUdMO17LYDgPARoWSDRKtpXhb9I51i-92HFRdSOrWC9CjqOu4kKNxeObyLhz3P1VffsZWRqTAxAjHzuXw,,&b64e=1&sign=00ea18e4d27c78cd5249a133fe567b3b&keyno=1',
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
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYjdGxZOSJSsGnCHSvLFvUhnfEoj75wqz9r7ijlwvBPJB-u8a0SVMOws3R8ko1RldBY9w-dlo9SDyBPJYTxFRLdQ3pnHpdB7StQGmzLzxap85bRe4UBdwOrB597VI0v9wuMQJEpBVXuToao3vGS-EgixjqUM4eyQQxKpHi-spVZ8o5pn85xFg6zv5ueIkqn1vnFI0uKilU0dK-9KSn8Opx49zbU3kyGLvPdiGKF8QNrXoQRp7BMbM9ZbD-SV7sMnarcCv5aAwf5TTPSyotNEkYNoCJvkOJ0daZ2CebUe2HLH_eQfO-Y4IP9VAAXeBRjxmuyM7GGoluTUlly38PNj_-upNU2hfX_Hop4I5vgwd1V1J4Jmi2meRv-sFqh2X2KULLUfkX1nVqqh2V4y6QmBAtYAQWMJt-GkaL9IkJvOQTQ7Qy0tGpc7k8BhkZbMBGVeTWJVP3I7aX3NqUcD76kACHJe8_Dfm79PQCWTTZECFhDEnNuyXQndS3XWB_KqXjqL6qchPoNb4yGEKW9wiKqGfIR_OD0MUziaN6U58MHc0dz5wWTTimm6a6XILCCCdVA-hpovsl8TGgwZ9pDvN3hxz_AAs4FGBh2awa715Uk2N2-lBmHhuJbbccBtPtDZqV-eGuI7KPlDrG03p7Pbc4neH2IfrqDhc8fY-u6erMDUChyu0QtjYNBROLy-zzUTg-gXCh4vNcTDvU2VBbihJFZLO6hDSiCuaoMb3AI7nF5ek66KbRzQ-cXdctIYqJBlvup-XZxVKS2rdxRZCZSGY5LzcEkFZIdErzyDazH-wiKbboKDUEvhOy6HxF4D9v-PfpVL1DF2uTVH7WyPTcWnoJCVB9u6Bc23_-g3Fg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-pRl8l6wfTaB0iaihtSiBUBMwTBS6-ZdyipBBVnlrKqk6uxDL4nmGUbhrB_1g6rLerFwp-4TEB-3VYoqDE7t431xXemhUhPUVjUDJ5H_JJf9evd6nPm0Gx_wThhQuUpuRNsV65KH-7sVOHdtxSnlmtUU7CG-G9bXETDW-YeOzBew,,&b64e=1&sign=5ccae08bff5bc2fe8c477ade71383ac4&keyno=1'
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
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва',
                        brief: 'в Москву — бесплатно, возможен самовывоз',
                        full: ''
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/230492/market_sHz7wlGGYko3PmuDKCY3hA/orig',
                            width: 295,
                            height: 350
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/230492/market_sHz7wlGGYko3PmuDKCY3hA/orig',
                        width: 295,
                        height: 350
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/230492/market_sHz7wlGGYko3PmuDKCY3hA/200x200',
                            width: 168,
                            height: 200
                        }
                    ],
                    outlet: {
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
                    warranty: 1,
                    description: '«SE» в названии телефона означает «Special Edition», то есть «Специальная версия». 4-дюймовый iPhone SE рассчитан на пользователей, которым хочется получить компактный, но очень мощный девайс. Новинку можно сравнить с моделями Compact производства Sony. Миллионы фанатов Apple с нетерпением ожидали выхода современной версии «классического» iPhone 5s. Они, наконец, дождались. Дизайн модели почти 100% копирует легендарный дизайн «пятерки».',
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210364&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 3,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/DiE1do_ZyD7FdOqKi6uGdQ?hid=91491&model_id=13584123&pp=1002&clid=2210364&distr_type=4&cpc=l9kwelcELdo6iGO2JBF2rhQMQbXALb1HmiHvklwwDPkQloVTmWBJ7ti7l5dd-G_QzSGs7KKv51D6RUwpmd2F2UF7jqx-8wA_-XkLyoKUd45DA-0thjqy9lLafKuchTyRfaS2x47PrY8%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mG_vtfl3b9ijZJJ-FVatgK6pyluxR9AOqesI6LAJFJb_baCPWF1dIPWEgaJgSpwFEYMcEUtiVcb1NH48hX9ZRXTwaksNQEKxuSxtkoIRTBkpArBtm7AgbmJ13XpvIr5a1ZFpx8JAq2Nof_zcEyCGwVaWMNUj88v-MWezB8c1QMfJC6d7Su3RfD5P6BW5Cz5hdZexZtZVf607_9mZUUAdQe83FhhwxI1yjTK49F3hnwbepnU6mNa610URoVLv5d4Yz_4sLNmphRsQJhax29uS5aLoRcG3z41SnNPR7W0OZeKSGrQfENcYF2oXe3y7zB7WP8eLGFSSIvPYMW7eZjmqxt5epUiiFElzTqwp2y2-EnSHfH9r51nszQuP4WK7uV0qh9m5EdXD5pK74nRxUGK2CBdWADiJetogfoGLusyl_fhiSL0BB57Xv0DHLVjSvDGLOwIl63mc-8JREy1siCiCesx611xmqw8u1PiZu2QrFzHJ5Lnk3QXMTF-CCqrAqTo5eJF28QXgcg48Luxv7SsVxX2Tob1z8o_8GTCYOqdckR8koc0wKCb7OEbKy7OLdWJydrGR23UAhTDP1_rsP_F32nTExE7IF2Y1M572CDLcAz54B3wIZdTybnOwmbR9nC33Pc4PfKvErs1msJzDWBoRypMjRAM8kOBo_MwcBL37-f5FqfOuseDINEKUYEBC-inOnlo8GFl2CjZor7jAOAe-0NasGl-_K3qazTVkqgfYmex9TIoRh8LmKQtC41HUD392cbk_G0rBr1s6X7zxWWDYQGJbqy3e1jEayuyFWaxXYHrIYPO8xclG2eyQAetbbhttwoiI3rUjRVawkoP5P9Es2uxQkH_M5Hip6xNNV7YPKRSTw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DENTjVaICh3FdvS9rtSpsPBE14GmavAxGFCzyeNv4nPXzcJ5NNa0pOSjWWdE_34uaSjrrbIDWFdgNsLfZx8Xrv4dZ1obH5QART6jyrlaPluk0bCMg8bo6B_PaAJelIo2YcYC1KICECnzAfSZ0f3JthJRG3pn3CZrxcw0_q2DPDhmgPTxRtqcJ22GvyjawaW5TvyOPEOyaAvRr4JcIN8oVOO2Sl73f7geYqd5F0bT4JfaKiWIQb2yr2qyuP9Ia9bgL-5Gb4vfIh_ivePsBE3v7iUmYTRhvE43EvcIIyuWuW7BJUZKAIAKBVFI_HJ3jKf5MRKDxsntAXCyjLTtbBymGTBh44NLjs2FNTP7onVjO-iA,,&b64e=1&sign=dab78b0fc81889530ab297f42d95e38e&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZFztD6WyzUvHlunueMn-6Sa7O4BtaDo1tB5b6t08UmMWfcdpi8DiYfj647w6UTbQCPe3vm-HF05L0BpkjhZ2lkDkpNZnrinv5T16P_2I-rvM0QzZzYgzWjo-yBNOEK5UyXhGxmeqXiwLWIoHNpsiIkCjNryDoRsuKjLaWs98mVr5Zik3yDJwryzkh8uLoiUjyRUqL-5zNvcEKOkoEtu72H0puN9CRRz-tO8Q2ZVCCULE-yZV9EvMRIxJWVUw3VoTDKFC57AeTS2SXPXdc9b8WZH7yAez55mBgY',
                    wareMd5: 'bpC1Q5JvtTdlTvx_Osr-Pg',
                    modelId: 13584123,
                    name: 'Apple iPhone SE 64Gb Rose Gold',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDrVkVBf3oEggdn5pxzG81KdDpiPLxSinj2TDFFWrmRuaQ-6a7A-pJzxZIdYt-dIEQG3umzS8GlL4SLFSM13NE0Ewbc0UaMw7PoMQP-a9EaFhHRqyV5y4-6M027JcwNWftEx4jeMraL7_nn4d6VSFe5A3sua37s06MQjgboKSFhZS-wH7j0ZBXCNcGlyRA-gtK6Ov5LP5zwo_WwD47oRoCmmMImP_Jw38ps55FnuBZxNRdIwuJ3ZxWwKhnVKLxhK3viuUuGn8pc0PE2Fghv3aaVvqdiqJOgc7jJJ-qIhdmyNhLGnPl5cuLk8nEfImbo-9h8qflgeBohuvLN9elz-aUyr1FqMkhamw9u6PnHLDzJm2UBoqaF-aqVAf5acNop9Eubd2Kx_upB5RXgyrVNzd7mJyrrtfNDUnu1i9oGRvoHr7szLN9MG3uAS3uOCQfNjv4YDCea9LEA-_azLO3G2mL0QaK-noXX3sUZI2ARCyZ8NpPzqegevGAeq5BaiPYCSC3HIAOEQ6y7nsfv0LCKEeOjk5nivUiiKAXj5-jx0bgL87CLXFGdT6-jcOUkElmYDv2ihIcaPgsI2df2vZv2TuVJUjQ7BJIBYAeC_ELbvIKxqNVrX_BpX-4SQhYAo1x8sMxUogvABJ9GhXa9ZSXoyDDTbPzG9pXSFOOe-n44Gfn2QQrDWviXlUcqsSAJGH4GqPQYYTqZb3ncHWJlU8yTHl3RlujwckDi4QAqHooB02_j99w,,?data=QVyKqSPyGQwwaFPWqjjgNsR692ugY0jPYkvqWKcD46MDfdBSsZS6r2NcWc3E2dfmpeKYyybk09Oc9x1I4Y-3iOvzvgO6LlWhuVBAIflc9xp-FPkDEMEKrrxCZFkIYAk4U_wmZgoQtwVqnC0vSB3UmEGJvutiOSivrat0ExABP0LaKoBl8XiNEG2ci7ck3XHq6zxj3EJHsFfU8FPg5plof3bPCFCdtVx3zSGDb9_YBH5PJohOFuKzKKLpuYL2fyGZuVAsIVQEeRnMly1fnhBe6yieW9PX27V8tiLf-OBRaBx2192SSmaGHFtgdtdYAxdelZBGb4nkL7VkQmDp1cDrNWQGRqbO3RZpdIT1tDEZapawvoCKje29Y99KfxViLF0g-N6w30y94oa4YEiRvYhnZ3ToiE2l6-I8&b64e=1&sign=e75b7a70db27c8876f1a1680bc6569cb&keyno=1',
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
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва',
                        brief: 'в Москву — 400 руб., возможен самовывоз',
                        full: ''
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/364498/market_2pdfxuJ82BXGpqDQPVzApA/orig',
                            width: 484,
                            height: 484
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/364498/market_2pdfxuJ82BXGpqDQPVzApA/orig',
                        width: 484,
                        height: 484
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/364498/market_2pdfxuJ82BXGpqDQPVzApA/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
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
                    warranty: 0,
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210364&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 3,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/bpC1Q5JvtTdlTvx_Osr-Pg?hid=91491&model_id=13584123&pp=1002&clid=2210364&distr_type=4&cpc=l9kwelcELdpHvbdZs8t-4zwcqDCbcro_ehIpE1E2-3Ne0epsOb-yDoyqG1FmVqcWCZ5NaaDZl7Vs4-kvf4oEMTGzxqjZdDggWbfTBeqUyhEbuaGw5etAzShs2k3eDO-c6Z2ui0Dfz-w%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOXVa_dnEzKn_M-0AEyIXvWpVj7V-5Ozhiaa8UeH02jopBbv6tRdrStKmoGp-clo3cyLgRc-5H_AGJraLb8dDnS8xa7-fZOVr8-DeeoEmcqPoeQ1t0HNrQCvxKSo0Dgv_9R4-CbM9ObitPk0K2W5HiqDYII-jgWTxJNyE4WlJ7rUronHsb1xYSPDdJN-9R2KLOSdVXLYDj1i7o8HiHf1CWEHGEy6al-Xt6Dg7T-gG4sF03lavi3vNpGSsbuK1BXZx8NY0UVStvw7oELImSfqtZnD4nAKJxmADvLIB0a0crxZUqCfwlgbLgUoaSr1-CEBBihOcl_xYn5RcXlCOInCnI9NZyVjf8zW0bv94VLmV1HI6rDry79w7R64Suf__4EH_OCrwQNQKaSliEWZaMqqAxpmiqLeJU4YUamC0CgW_7mL-BKHvsy_DAlxZKSj6Opg2DL4kv5XKrNCFHByrdUad9PCbgF4-etFqO2iLPl3uvuO4jv0yVdIdEt_foEcDsOJcX-RekNJ2bw5pS-XZwa2tBBVXPUQk-8Szbej3MOvvIYzjZ8EE12rfKXYzs_jAuA38-Z7y7wijY3NHj41cQ5efAebJC60GfD8oVlZYJsYMb91Of9vWiEtQ5WYGL6I6_-pc2IwPxlYueCrlXrKgf5b_H45WkV-lWiw_Amni_6_400ArldJqg5MludjBvmOH5mF46gz7Jy0_Yr-Camvo843cSd-kKUCEdcgOCKPV8weQ6nAGRF_6oOvbRSdBV4RmKz1zC6yYCGrDrNZpwhdN0S_0yVIx3NnS1u3tm0z-NrkGCbFkyiGjQY7CVbCc0iyBXKQ4Ug,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DENTjVaICh3FdvS9rtSpsPBE14GmavAxGFCzyeNv4nPXzcJ5NNa0pOuJxbCOHqDGaSwLkUI0M4RVWO-zyrRBG2NEgvrw5bt3goXpDKXApHytbzWfRiw-Qx3Ti5m-g8phITHgjnBmdfEcH2AWoZk0Egw-S9UeVARP6PT_Euf8yK436ex299dcv5SsYtdUeWoxpv8vdujUYHw8sN1vn8NW6RvXz-CsQVofF9a4iWPCiUUTNuVJvrdCezSeywyBs3bP5mrmenNo_pqY1_OETgmJx9MNFeRSByJPhHm11Q05zb2Kxu8mxZJnm1LyTIKhYkFCnvCzCITjjF0nH4OMf9oiGrjaShiq-mlub7rWoMPxpZlQ,,&b64e=1&sign=ca8a20a54856dc05756d5d6c2119ec05&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZFNreN8PzivVIULyY87XMrlISSBEswhV_nZhPhoxidxTw',
                    wareMd5: 'tZ3YHTMSYU4eGhU_seWVUA',
                    modelId: 13584123,
                    name: 'Apple iPhone SE 64GB Silver',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYmRJ7QyZlO8wiUlW-RmCVZ9Gdik3MA50a6FhfvpKa7-Cv_okYkubtx19TgPUMDki_OWHEfLfzbZMc6z-pNWk8SuS0rz0XnokLlH_F8IJ3LtZQLr01lLMDrVkVBf3oEggdn5pxzG81KdDpiPLxSinj2TDFFWrmRuaQ-6a7A-pJzxZIdYt-dIEQHN62NXt8rjB7NUDks3UMLT51hfvA7vduHu0UpH_841NrK92_tRYO2XT2Z--RLGbkMYwA_6pgAQ-2Qo_KoEbjS5pkwZVmDn49-QfPEckSIEkNVepWw8L9K_xW3WngGjGIKsMzSPUjvNghyvzJJi2e3_KB2byZGytRKYde5cZ11_5zZOCMiwU4v5aibyR6aC19a07MwM7b8WVEVY7rytbJ6pu-KEL_snK8i9pmM0DDUsRsjN8IsxoxY-E_jzmFOG7jeXSgmg23ndTVLEsyaLSwFnVDh2wPUjv1nZyg2PWILgaWYbswzUhkbDuJAKZ87OdgyCiixpc_RUzvwA1xoydXTTf4dr8IraYZdQMXZghKGiGH437XUAoLJCbN11rRbr7dhVHpLElZZW1kdSYQoEQ1G9O-0cEPa-HOeBKeMz3mmn_nEhSNQtee3t4T_-Me597tjq3aRzVVzM9OLKkubnh60R8N16dniPD-jjsT_Pci5D1ZtONJ0sAKB3cp6kjJQB7s2RjrZiIVVo-nFN7ZjAyU5AWPRVHDo17-r5iVoieuI98PmQHLZMUDPuP2SMJgEsjlBXgtgYcvUCADMn4VQ_PvC7HUdZKk9woxRfb2ngtfmsaBeDBfeS3USFEioPbpTHdZJ5HvF3-ao7hSSz39PGz7yJL2-Sux1wvpLHSKiF7yuypw,,?data=QVyKqSPyGQwwaFPWqjjgNkzRjoOstLIO6IRK5gdDYhT07Y_eei45F3cETnPsMdv6wDwpUqDOrnIVrq-mYTcxlGbW_v96F1ybVzcFjlCGXHtJjKbCgiUy-Obnq_h9hI8TZnA4XLSYOUW-1-f5JGu7xjCJ8HFeW-2YzzldXw4YYu8RyJtk-SYvc12Px_d6ItSg0rUTjwIlh5frm0l-rwJ6lqqDOuXjSwfm4tZmsOURlZM,&b64e=1&sign=cb4629e1ebccd9c06a07dbe1cd74d2ff&keyno=1',
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
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва',
                        brief: 'в Москву — 350 руб., возможен самовывоз',
                        full: ''
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/212340/market_hGYpflGs6QSVv_ZJ3Kbc_g/orig',
                            width: 600,
                            height: 600
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/212340/market_hGYpflGs6QSVv_ZJ3Kbc_g/orig',
                        width: 600,
                        height: 600
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/212340/market_hGYpflGs6QSVv_ZJ3Kbc_g/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
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
                    warranty: 0,
                    description: 'Apple iPhone SE 64GB Silver Эргономичность и мощность Айфон SE полностью идентичен своей предыдущей модели – iPhone 5S. Единственное внешнее отличие — это наличие цвета «розовое золото». Однако не стоит разочаровываться, потому что начинка у iPhone SE идентична с модель iPhone 6S. Стоит только взять айфон SE в руки и вы почувствуете на сколько он удобный, размеры у iPhone SE позволяют в совершенстве пользоваться им одной рукой.',
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210364&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 1,
                    categoryId: 91491,
                    link: 'https://market.yandex.ru/offer/tZ3YHTMSYU4eGhU_seWVUA?hid=91491&model_id=13584123&pp=1002&clid=2210364&distr_type=4&cpc=l9kwelcELdq2y6PTbiNPNxaxQuC8eftg6R-jQ1uiIYA0ErMKS5HUf4cVguqYn-eQ-V6VsGHNQCi0TFl3pIU9Iw64tesvU4qOdtkZB7VXbSjFy2AtFPQTwdU3KxE5EYH1&lr=213',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                }
            ],
            page: 1,
            total: 7,
            count: 7
        }
    }
};

module.exports = new ApiMock(host, pathname, query, result);
