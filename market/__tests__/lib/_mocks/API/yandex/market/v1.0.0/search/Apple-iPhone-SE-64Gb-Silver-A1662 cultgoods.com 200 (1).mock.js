/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/search\.json/;

const query = {
    text: 'url:"cultgoods.com/products/smartfon-apple-iphone-se-64gb-silver-a1662*"'
};

const result = {
    comment: 'text = "url:"cultgoods.com/products/smartfon-apple-iphone-se-64gb-silver-a1662*""',
    status: 200,
    body: {
        searchResult: {
            page: 1,
            count: 1,
            total: 1,
            requestParams: {
                text: 'url:"cultgoods.com/products/smartfon-apple-iphone-se-64gb-silver-a1662*"',
                actualText: 'url:"cultgoods.com/products/smartfon-apple-iphone-se-64gb-silver-a1662*"',
                checkSpelling: false
            },
            results: [
                {
                    offer: {
                        id: 'yDpJekrrgZGZGWPAtlC0aqAL5ihHLj2hk3YbR50XzlZK-IjOr75SLmcq6jJXI8DZrx9K9eM3E2UpqhMWuUH8JVz6tlHoSA5xRXBxPcRcOcGQ4Hal3OYv8EXB5rqNoUKct8Bq4ezEm5-LjhJhib543eHpqL2vJXWqgEomoWIluvFrIwiNkrat2y87dsvxEVSVqCLC7dcUGZ1FhNzg4GqQobf5_3NBNYK2C05IlWEFFWpt_AvLOMk9QdqpxT1lwqR3g6yd3zFmFJuErfNBU-ORiCVsU-AlPyH8wZyFrz2Fq2o',
                        wareMd5: 'nyiEPi-lA4kejgu9X_uhrQ',
                        modelId: 13584123,
                        name: 'Смартфон Apple iPhone SE 64Gb Silver A1662',
                        onStock: 1,
                        url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mf5fIUVrEGur84qC-PSK5TYHFQfwNL92WuhMrWHdRZCiuhlG3xsRNd4l4MYX6ONy1I9hDKtUnBmplsC9DXB_0Zes-RWXqu-9f6v1Q81c-cE3rOFeDHcPLIYoh1P5rF7pY0sKiWmSBJarEi05eePw7Fy6SB_mCTvKFRAtIiqXpykuv2aAx-uS7Qm1lD2hJRxUeJFv25JwraUculEzJtwVueTPMiy-mVynsz9C5Yf8-WZk8IUXONC4w4AjAihrUlz8zjzMyYcHgVUTmggzIF_YTdWaQt85OevhCL_WZUUoYoja6VmygYMmbTjGyjsSDIF9yyLTDMiIsJ7UVaIjwwjjUgLmaoMtBUEq3IimWzEtc3TZtJAHwtGpV9Yn1aQrAw-nFJN5zXUUUOEJ5HTrav4sZ3gC2-VEQESCXT8ELJPpYo9yvgG6UyKPl3l__l77juuO4PmElufqUb7tYuNJXH9kFXSe-VgMbASOjBoLGcXWfyPs8cRsEIvdbmc0frMyOgOns_0D-RBeRBmVCRQuM_I9mFvhDpkzaf85jPYaKi1NJ6ObFBldZWes0Vt4QUOTdeUr9jcn8plBxoE7x8J8yBKKjJJSFwqWnV_nthRiHyBQ2G4eh5cH4_mGEknYqQBytJ-P4paITaYBxlEKCn6qDwCvV9NFgttXku2Ci4C4KxiU3dRIq7Q2h1PO8BJDfUae4FEyhB9quaN1lukXOeKFQlB5ECy4x5KW2R5mvjfmXbPxKrZvSIPfxryzwCiTPPUYDUYbCAnuhpf_zMdbuZ_6Nx7kwFlOxgy7xWgSJ26tXwBN3MInL3XaUzfzJt1CMHsLThL0qAgPFK2aP2yOFyHMz_ONIwtxLZgS4UhyaA,,?data=QVyKqSPyGQwwaFPWqjjgNt96Q-I4eLvRt1Jd-FahOC1oLxErrgxA7U8HsI7pvjzVNeHCAbnFcfTqF9YngERSVUOqAY-Mf0DS-16OjPY5CR5RPoQ_p6Jar-n89K2eNeFm6ucx1ZHN0m-QfYQaUI21UNxJSrzIgzpvMMxdlK23zs7LoGPzzVJBp-TR4_IfNaIed8fjCy7Wr58BPSHmfTcmgcIeBsm0-2qafOogzQ3FTFVC1w_gskwlX20NxQaNuHZR5mcF-_9bGxXt-EwL6MNdghc20J_VWkj0Izx74IiQKX9JojCQqwOZ85tAkhIKQxGIWggDMol_ILm9X29J5NCsEFejkQa7nxWik7e8MWxB0jduOpFvbX3BAw,,&b64e=1&sign=d1163966ff75ba234cf4a421d4466024&keyno=1',
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
                            call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mf5fIUVrEGur84qC-PSK5TYHFQfwNL92WuhMrWHdRZCiuhlG3xsRNd4l4MYX6ONy1I9hDKtUnBmplsC9DXB_0Zes-RWXqu-9f6v1Q81c-cE3rOFeDHcPLIYoh1P5rF7pY0sKiWmSBJarEi05eePw7Fy6SB_mCTvKFRAtIiqXpykuv2aAx-uS7QnO356-u-oa7hMHsINGarwmHq4BWE88cOVLU2G33z8sUyvZ2YPAW48mTqw_-_DdMIgygk7AshtNeaeEoD69A77vQZvNrta1bFdvqbx-CAAmcPLjwEFheJ459Fq2ij3NtnqM8yGYsvPvfz97wHK9oPK05zfnAsBncbu6pU5o_1VTNfecfObIu4EIiExwja6pahMyFf5SAWUBPAf7gds9mCyY5Tlb1K01jMdpZiwzV_yMzdeoSMcCyRwHwCnqPy_rMsygxupJo9E8yqPXfp-hPQVhanaffojsrtQyHM_4UjGU0mA73YmbJwQX-GcMtNmLDlqLwje0csRHQtOk0IdarpKi27D3X192hPJVJ1y7vB7S-ljKGzhA-Mz1AtG60WQTYTjfOfy6ImLPd07-ttixsxFJ98MIkdzRAqyXdkB_dq-wNhbL21aTu2RreXClSMGaY8KMWvlqR8e5SwhVf6JzT1OgE_TFPy3alK5ikb_SbehpZoosMNmiSSjCWE97ceex41699paUQQBkOCon1UNYzdrG-WtG2c2xbHGOKnRviLsY4tm4ujL6424hlcTYaGC-gErfHkT86BIV34HdDr_tAHpgf2WY12CE-RX1B6KJ1S2m78egNQ0lsAJPamLiwhFGt3NWZc76bdlSE087rZChI1Hi0-ikqMEg_2ObbIe8SlT6mg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_1zBEF_FUaqBDvKYWgG3LXZB_vwCwNG6Kd2d5LAifZ0rwBtkL5iI5WUfKBLeUhVzhEpdm1CcMPgay8YJkLOV69R0JwaxyEyzjH-7I-y7v7kIr32hAsFittfuAmEdv_Op41XHF7hJC4OmV3TExVAbhUh8s7T0tDLp1A6CwbLaUc8w,,&b64e=1&sign=81e269ffc901a169d17bbba17a38485f&keyno=1'
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
                        photos: [
                            {
                                url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_NGvrwrZYAQkugW1DvZJolg/orig',
                                width: 96,
                                height: 200
                            }
                        ],
                        bigPhoto: {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_NGvrwrZYAQkugW1DvZJolg/orig',
                            width: 96,
                            height: 200
                        },
                        previewPhotos: [
                            {
                                url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_NGvrwrZYAQkugW1DvZJolg/200x200',
                                width: 96,
                                height: 200
                            }
                        ],
                        outlet: {
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
                        warranty: 0,
                        description: 'iPhone SE - смартфон корпорации Apple. Представлен 21 марта2016 года. Внешне схож с iPhone 5s, однако имеет многие характеристики IPhone 6s. Работает на операционной системе iOS 9.3, содержит процессор Apple A9, со-процессор Apple M9 и сканер отпечатков пальцев (Touch ID), встроенный в кнопку Home чуть ниже экрана. Выполнен в четырёх цветовых решениях (серый космос, серебристый, золотой и розовое золото).',
                        outletCount: 1,
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
                    }
                }
            ],
            categories: [
                {
                    id: '91491',
                    name: 'Мобильные телефоны',
                    count: '1',
                    visual: false,
                    uniq_name: 'Мобильные телефоны'
                }
            ]
        }
    }
};

module.exports = new ApiMock(host, pathname, query, result);
