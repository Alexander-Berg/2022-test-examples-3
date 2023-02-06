// jscs:disable maximumLineLength
var stubs = require('@yandex-int/gemini-serp-stubs');

module.exports = {
    type: 'snippet',
    request_text: 'приложения для чтения',
    data_stub: {
        "num": 0,
        "snippets": {
            "full": {
                "applicable": 1,
                "counter_prefix": "/snippet/app_search_list_view/",
                "data": {},
                "serp_info": {
                    "counter_prefix": "/snippet/app_search_list_view/",
                    "flat": "1",
                    "format": "json",
                    "type": "app_search_list_view"
                },
                "slot": "full",
                "slot_rank": 0,
                "template": "app_search_list_view",
                "type": "app_search_list_view",
                "types": {
                    "all": [
                        "snippets",
                        "app_search_list_view"
                    ],
                    "kind": "wizard",
                    "main": "app_search_list_view"
                },
                "viewport": {
                    "@layout": "relatedappscard:small",
                    "@type": "AppSearchListView",
                    "appListCard": {
                        "apps": [
                            {
                                "@actions": {
                                    "buy": {
                                        "data": "https://play.google.com/store/apps/details?id=ebook.epub.download.reader&hl=ru&gl=ru",
                                        "intent": "buy"
                                    },
                                    "view": {
                                        "data": "https://play.google.com/store/apps/details?id=ebook.epub.download.reader&hl=ru&gl=ru",
                                        "intent": "view"
                                    }
                                },
                                "@id": "$.appListCard.apps[1]",
                                "@type": "AppBadgeBlock",
                                "description": {
                                    "@id": "$.appListCard.apps[1].description",
                                    "@type": "TextCell",
                                    "text": "Это должна быть программа для вашего телефона.\u0007[Для\u0007] \u0007[чтения\u0007] электронных книг является одним из лучших инструментов \u0007[чтения\u0007]."
                                },
                                "icon": {
                                    "@id": "$.appListCard.apps[1].icon",
                                    "@type": "ImageCell",
                                    "imageId": "//avatars.mds.yandex.net/get-google-play-app-icon/15229/ccbff2382b102b310788d2f9f074f2a5/%s",
                                    "src": stubs.imageUrlStub(72, 72)
                                },
                                "icon_small": {
                                    "@id": "$.appListCard.apps[1].icon_small",
                                    "@type": "ImageCell",
                                    "imageId": "//avatars.mds.yandex.net/get-google-play-app-icon/15229/ccbff2382b102b310788d2f9f074f2a5/%s",
                                    "src": stubs.imageUrlStub(72, 72)
                                },
                                "path": {
                                    "@id": "$.appListCard.apps[1].path",
                                    "@type": "TextCell",
                                    "text": "play.google.com"
                                },
                                "price": {
                                    "@id": "$.appListCard.apps[1].price",
                                    "@type": "MoneyCell",
                                    "unit": "RUR",
                                    "value": 0
                                },
                                "rating": {
                                    "@id": "$.appListCard.apps[1].rating",
                                    "@type": "RatingCell",
                                    "value": 4.1738224029541,
                                    "votes": 66775
                                },
                                "setups": {
                                    "@id": "$.appListCard.apps[1].setups",
                                    "@type": "TextCell",
                                    "text": "1000000–5000000"
                                },
                                "store": {
                                    "@id": "$.appListCard.apps[1].store",
                                    "@type": "TextCell",
                                    "text": "GOOGLE_PLAY"
                                },
                                "title": {
                                    "@id": "$.appListCard.apps[1].title",
                                    "@type": "TextCell",
                                    "text": "Для чтения электронных книг"
                                }
                            },
                            {
                                "@actions": {
                                    "buy": {
                                        "data": "https://play.google.com/store/apps/details?id=org.geometerplus.zlibrary.ui.android&hl=ru&gl=ru",
                                        "intent": "buy"
                                    },
                                    "view": {
                                        "data": "https://play.google.com/store/apps/details?id=org.geometerplus.zlibrary.ui.android&hl=ru&gl=ru",
                                        "intent": "view"
                                    }
                                },
                                "@id": "$.appListCard.apps[2]",
                                "@type": "AppBadgeBlock",
                                "description": {
                                    "@id": "$.appListCard.apps[2].description",
                                    "@type": "TextCell",
                                    "text": "FBReader -- программа \u0007[для\u0007] \u0007[чтения\u0007] электронных книг.Новый (более совеременный и фукнциональный)..."
                                },
                                "icon": {
                                    "@id": "$.appListCard.apps[2].icon",
                                    "@type": "ImageCell",
                                    "imageId": "//avatars.mds.yandex.net/get-google-play-app-icon/24685/191afce3ea2615944fcdbce855e32ab6/%s",
                                    "src": stubs.imageUrlStub(72, 72)
                                },
                                "icon_small": {
                                    "@id": "$.appListCard.apps[2].icon_small",
                                    "@type": "ImageCell",
                                    "imageId": "//avatars.mds.yandex.net/get-google-play-app-icon/24685/191afce3ea2615944fcdbce855e32ab6/%s",
                                    "src": stubs.imageUrlStub(72, 72)
                                },
                                "path": {
                                    "@id": "$.appListCard.apps[2].path",
                                    "@type": "TextCell",
                                    "text": "play.google.com"
                                },
                                "price": {
                                    "@id": "$.appListCard.apps[2].price",
                                    "@type": "MoneyCell",
                                    "unit": "RUR",
                                    "value": 0
                                },
                                "rating": {
                                    "@id": "$.appListCard.apps[2].rating",
                                    "@type": "RatingCell",
                                    "value": 4.54475402832031,
                                    "votes": 186486
                                },
                                "setups": {
                                    "@id": "$.appListCard.apps[2].setups",
                                    "@type": "TextCell",
                                    "text": "10000000–50000000"
                                },
                                "store": {
                                    "@id": "$.appListCard.apps[2].store",
                                    "@type": "TextCell",
                                    "text": "GOOGLE_PLAY"
                                },
                                "title": {
                                    "@id": "$.appListCard.apps[2].title",
                                    "@type": "TextCell",
                                    "text": "FBReader"
                                }
                            },
                            {
                                "@actions": {
                                    "buy": {
                                        "data": "https://play.google.com/store/apps/details?id=org.coolreader&hl=ru&gl=ru",
                                        "intent": "buy"
                                    },
                                    "view": {
                                        "data": "https://play.google.com/store/apps/details?id=org.coolreader&hl=ru&gl=ru",
                                        "intent": "view"
                                    }
                                },
                                "@id": "$.appListCard.apps[3]",
                                "@type": "AppBadgeBlock",
                                "description": {
                                    "@id": "$.appListCard.apps[3].description",
                                    "@type": "TextCell",
                                    "text": "Программа \u0007[для\u0007] \u0007[чтения\u0007] электронных книг. Форматы: fb2, epub (без DRM), txt, doc, rtf, html, chm, tcr, pdb, prc, mobi (без DRM), pml."
                                },
                                "icon": {
                                    "@id": "$.appListCard.apps[3].icon",
                                    "@type": "ImageCell",
                                    "imageId": "//avatars.mds.yandex.net/get-google-play-app-icon/21534/279e0242c160b53a86f805d6555759f6/%s",
                                    "src": stubs.imageUrlStub(72, 72)
                                },
                                "icon_small": {
                                    "@id": "$.appListCard.apps[3].icon_small",
                                    "@type": "ImageCell",
                                    "imageId": "//avatars.mds.yandex.net/get-google-play-app-icon/21534/279e0242c160b53a86f805d6555759f6/%s",
                                    "src": stubs.imageUrlStub(72, 72)
                                },
                                "path": {
                                    "@id": "$.appListCard.apps[3].path",
                                    "@type": "TextCell",
                                    "text": "play.google.com"
                                },
                                "price": {
                                    "@id": "$.appListCard.apps[3].price",
                                    "@type": "MoneyCell",
                                    "unit": "RUR",
                                    "value": 0
                                },
                                "rating": {
                                    "@id": "$.appListCard.apps[3].rating",
                                    "@type": "RatingCell",
                                    "value": 4.4789719581604,
                                    "votes": 231598
                                },
                                "setups": {
                                    "@id": "$.appListCard.apps[3].setups",
                                    "@type": "TextCell",
                                    "text": "10000000–50000000"
                                },
                                "store": {
                                    "@id": "$.appListCard.apps[3].store",
                                    "@type": "TextCell",
                                    "text": "GOOGLE_PLAY"
                                },
                                "title": {
                                    "@id": "$.appListCard.apps[3].title",
                                    "@type": "TextCell",
                                    "text": "Cool Reader"
                                }
                            },
                            {
                                "@actions": {
                                    "buy": {
                                        "data": "https://play.google.com/store/apps/details?id=yong.reader.pdf.xps.viewer&hl=ru&gl=ru",
                                        "intent": "buy"
                                    },
                                    "view": {
                                        "data": "https://play.google.com/store/apps/details?id=yong.reader.pdf.xps.viewer&hl=ru&gl=ru",
                                        "intent": "view"
                                    }
                                },
                                "@id": "$.appListCard.apps[4]",
                                "@type": "AppBadgeBlock",
                                "description": {
                                    "@id": "$.appListCard.apps[4].description",
                                    "@type": "TextCell",
                                    "text": "Скачать один из лучших Android книгу читателям сейчас ! \" \u0007[Для\u0007] \u0007[чтения\u0007] электронных книг Pro \" является полным бесплатный..."
                                },
                                "icon": {
                                    "@id": "$.appListCard.apps[4].icon",
                                    "@type": "ImageCell",
                                    "imageId": "//avatars.mds.yandex.net/get-google-play-app-icon/15316/74c2e0c9077364f9e7ff648a5146b2cb/%s",
                                    "src": stubs.imageUrlStub(72, 72)
                                },
                                "icon_small": {
                                    "@id": "$.appListCard.apps[4].icon_small",
                                    "@type": "ImageCell",
                                    "imageId": "//avatars.mds.yandex.net/get-google-play-app-icon/15316/74c2e0c9077364f9e7ff648a5146b2cb/%s",
                                    "src": stubs.imageUrlStub(72, 72)
                                },
                                "path": {
                                    "@id": "$.appListCard.apps[4].path",
                                    "@type": "TextCell",
                                    "text": "play.google.com"
                                },
                                "price": {
                                    "@id": "$.appListCard.apps[4].price",
                                    "@type": "MoneyCell",
                                    "unit": "RUR",
                                    "value": 0
                                },
                                "rating": {
                                    "@id": "$.appListCard.apps[4].rating",
                                    "@type": "RatingCell",
                                    "value": 4.09642124176025,
                                    "votes": 23501
                                },
                                "setups": {
                                    "@id": "$.appListCard.apps[4].setups",
                                    "@type": "TextCell",
                                    "text": "1000000–5000000"
                                },
                                "store": {
                                    "@id": "$.appListCard.apps[4].store",
                                    "@type": "TextCell",
                                    "text": "GOOGLE_PLAY"
                                },
                                "title": {
                                    "@id": "$.appListCard.apps[4].title",
                                    "@type": "TextCell",
                                    "text": "для чтения электронных книг"
                                }
                            },
                            {
                                "@actions": {
                                    "buy": {
                                        "data": "https://play.google.com/store/apps/details?id=la.droid.qr&hl=ru&gl=ru",
                                        "intent": "buy"
                                    },
                                    "view": {
                                        "data": "https://play.google.com/store/apps/details?id=la.droid.qr&hl=ru&gl=ru",
                                        "intent": "view"
                                    }
                                },
                                "@id": "$.appListCard.apps[5]",
                                "@type": "AppBadgeBlock",
                                "description": {
                                    "@id": "$.appListCard.apps[5].description",
                                    "@type": "TextCell",
                                    "text": "Превратите свой смартфон в мощный сканер QR-кодов, штрихкодов и таблиц данных. Легко и быстро импортируйте, создавайте..."
                                },
                                "icon": {
                                    "@id": "$.appListCard.apps[5].icon",
                                    "@type": "ImageCell",
                                    "imageId": "//avatars.mds.yandex.net/get-google-play-app-icon/15229/fce1be88ce2c0ef86e3cd59c96a8f8a0/%s",
                                    "src": stubs.imageUrlStub(72, 72)
                                },
                                "icon_small": {
                                    "@id": "$.appListCard.apps[5].icon_small",
                                    "@type": "ImageCell",
                                    "imageId": "//avatars.mds.yandex.net/get-google-play-app-icon/15229/fce1be88ce2c0ef86e3cd59c96a8f8a0/%s",
                                    "src": stubs.imageUrlStub(72, 72)
                                },
                                "path": {
                                    "@id": "$.appListCard.apps[5].path",
                                    "@type": "TextCell",
                                    "text": "play.google.com"
                                },
                                "price": {
                                    "@id": "$.appListCard.apps[5].price",
                                    "@type": "MoneyCell",
                                    "unit": "RUR",
                                    "value": 0
                                },
                                "rating": {
                                    "@id": "$.appListCard.apps[5].rating",
                                    "@type": "RatingCell",
                                    "value": 4.1374077796936,
                                    "votes": 346443
                                },
                                "setups": {
                                    "@id": "$.appListCard.apps[5].setups",
                                    "@type": "TextCell",
                                    "text": "50000000–100000000"
                                },
                                "store": {
                                    "@id": "$.appListCard.apps[5].store",
                                    "@type": "TextCell",
                                    "text": "GOOGLE_PLAY"
                                },
                                "title": {
                                    "@id": "$.appListCard.apps[5].title",
                                    "@type": "TextCell",
                                    "text": "QR Droid Code Scanner"
                                }
                            },
                            {
                                "@actions": {
                                    "buy": {
                                        "data": "https://play.google.com/store/apps/details?id=com.androidlord.barcodescanner&hl=ru&gl=ru",
                                        "intent": "buy"
                                    },
                                    "view": {
                                        "data": "https://play.google.com/store/apps/details?id=com.androidlord.barcodescanner&hl=ru&gl=ru",
                                        "intent": "view"
                                    }
                                },
                                "@id": "$.appListCard.apps[6]",
                                "@type": "AppBadgeBlock",
                                "description": {
                                    "@id": "$.appListCard.apps[6].description",
                                    "@type": "TextCell",
                                    "text": "Сканер QR-кодов и штрихкодов предназначен для считывания штрихкодов и QR-кодов. Его можно использовать для считывания..."
                                },
                                "icon": {
                                    "@id": "$.appListCard.apps[6].icon",
                                    "@type": "ImageCell",
                                    "imageId": "//avatars.mds.yandex.net/get-google-play-app-icon/15229/c0072f1df89cb04e759d0f852170d4d6/%s",
                                    "src": stubs.imageUrlStub(72, 72)
                                },
                                "icon_small": {
                                    "@id": "$.appListCard.apps[6].icon_small",
                                    "@type": "ImageCell",
                                    "imageId": "//avatars.mds.yandex.net/get-google-play-app-icon/15229/c0072f1df89cb04e759d0f852170d4d6/%s",
                                    "src": stubs.imageUrlStub(72, 72)
                                },
                                "path": {
                                    "@id": "$.appListCard.apps[6].path",
                                    "@type": "TextCell",
                                    "text": "play.google.com"
                                },
                                "price": {
                                    "@id": "$.appListCard.apps[6].price",
                                    "@type": "MoneyCell",
                                    "unit": "RUR",
                                    "value": 0
                                },
                                "rating": {
                                    "@id": "$.appListCard.apps[6].rating",
                                    "@type": "RatingCell",
                                    "value": 3.62515807151794,
                                    "votes": 5533
                                },
                                "setups": {
                                    "@id": "$.appListCard.apps[6].setups",
                                    "@type": "TextCell",
                                    "text": "500000–1000000"
                                },
                                "store": {
                                    "@id": "$.appListCard.apps[6].store",
                                    "@type": "TextCell",
                                    "text": "GOOGLE_PLAY"
                                },
                                "title": {
                                    "@id": "$.appListCard.apps[6].title",
                                    "@type": "TextCell",
                                    "text": "Сканер QR-кодов и штрихкодов"
                                }
                            },
                            {
                                "@actions": {
                                    "buy": {
                                        "data": "https://play.google.com/store/apps/details?id=com.prestigio.ereader&hl=ru&gl=ru",
                                        "intent": "buy"
                                    },
                                    "view": {
                                        "data": "https://play.google.com/store/apps/details?id=com.prestigio.ereader&hl=ru&gl=ru",
                                        "intent": "view"
                                    }
                                },
                                "@id": "$.appListCard.apps[7]",
                                "@type": "AppBadgeBlock",
                                "description": {
                                    "@id": "$.appListCard.apps[7].description",
                                    "@type": "TextCell",
                                    "text": "eReader Prestigio - программа \u0007[для\u0007] \u0007[чтения\u0007] электронных книг и PDF файловОсновные форматы: fb2 (и fb2.zip), ePub..."
                                },
                                "icon": {
                                    "@id": "$.appListCard.apps[7].icon",
                                    "@type": "ImageCell",
                                    "imageId": "//avatars.mds.yandex.net/get-google-play-app-icon/40549/7f840523a2e588a5b9d5fc4cd2d47b3e/%s",
                                    "src": stubs.imageUrlStub(72, 72)
                                },
                                "icon_small": {
                                    "@id": "$.appListCard.apps[7].icon_small",
                                    "@type": "ImageCell",
                                    "imageId": "//avatars.mds.yandex.net/get-google-play-app-icon/40549/7f840523a2e588a5b9d5fc4cd2d47b3e/%s",
                                    "src": stubs.imageUrlStub(72, 72)
                                },
                                "path": {
                                    "@id": "$.appListCard.apps[7].path",
                                    "@type": "TextCell",
                                    "text": "play.google.com"
                                },
                                "price": {
                                    "@id": "$.appListCard.apps[7].price",
                                    "@type": "MoneyCell",
                                    "unit": "RUR",
                                    "value": 0
                                },
                                "rating": {
                                    "@id": "$.appListCard.apps[7].rating",
                                    "@type": "RatingCell",
                                    "value": 4.56971836090088,
                                    "votes": 147199
                                },
                                "setups": {
                                    "@id": "$.appListCard.apps[7].setups",
                                    "@type": "TextCell",
                                    "text": "10000000–50000000"
                                },
                                "store": {
                                    "@id": "$.appListCard.apps[7].store",
                                    "@type": "TextCell",
                                    "text": "GOOGLE_PLAY"
                                },
                                "title": {
                                    "@id": "$.appListCard.apps[7].title",
                                    "@type": "TextCell",
                                    "text": "eReader Prestigio: Читалка"
                                }
                            },
                            {
                                "@actions": {
                                    "buy": {
                                        "data": "https://play.google.com/store/apps/details?id=com.google.android.apps.books&hl=ru&gl=ru",
                                        "intent": "buy"
                                    },
                                    "view": {
                                        "data": "https://play.google.com/store/apps/details?id=com.google.android.apps.books&hl=ru&gl=ru",
                                        "intent": "view"
                                    }
                                },
                                "@id": "$.appListCard.apps[8]",
                                "@type": "AppBadgeBlock",
                                "description": {
                                    "@id": "$.appListCard.apps[8].description",
                                    "@type": "TextCell",
                                    "text": "Вашему вниманию предлагаются миллионы книг из Google Play, среди которых вы найдете новинки, бестселлеры \"Нью-Йорк Таймс\"..."
                                },
                                "icon": {
                                    "@id": "$.appListCard.apps[8].icon",
                                    "@type": "ImageCell",
                                    "imageId": "//avatars.mds.yandex.net/get-google-play-app-icon/15229/8bc342402bb9777d61d61c9a413d1a74/%s",
                                    "src": stubs.imageUrlStub(72, 72)
                                },
                                "icon_small": {
                                    "@id": "$.appListCard.apps[8].icon_small",
                                    "@type": "ImageCell",
                                    "imageId": "//avatars.mds.yandex.net/get-google-play-app-icon/15229/8bc342402bb9777d61d61c9a413d1a74/%s",
                                    "src": stubs.imageUrlStub(72, 72)
                                },
                                "path": {
                                    "@id": "$.appListCard.apps[8].path",
                                    "@type": "TextCell",
                                    "text": "play.google.com"
                                },
                                "price": {
                                    "@id": "$.appListCard.apps[8].price",
                                    "@type": "MoneyCell",
                                    "unit": "RUR",
                                    "value": 0
                                },
                                "rating": {
                                    "@id": "$.appListCard.apps[8].rating",
                                    "@type": "RatingCell",
                                    "value": 3.88185381889343,
                                    "votes": 1283716
                                },
                                "setups": {
                                    "@id": "$.appListCard.apps[8].setups",
                                    "@type": "TextCell",
                                    "text": "1000000000–5000000000"
                                },
                                "store": {
                                    "@id": "$.appListCard.apps[8].store",
                                    "@type": "TextCell",
                                    "text": "GOOGLE_PLAY"
                                },
                                "title": {
                                    "@id": "$.appListCard.apps[8].title",
                                    "@type": "TextCell",
                                    "text": "Google Play Книги"
                                }
                            },
                            {
                                "@actions": {
                                    "buy": {
                                        "data": "https://play.google.com/store/apps/details?id=com.flyersoft.moonreaderp&hl=ru&gl=ru",
                                        "intent": "buy"
                                    },
                                    "view": {
                                        "data": "https://play.google.com/store/apps/details?id=com.flyersoft.moonreaderp&hl=ru&gl=ru",
                                        "intent": "view"
                                    }
                                },
                                "@id": "$.appListCard.apps[9]",
                                "@type": "AppBadgeBlock",
                                "description": {
                                    "@id": "$.appListCard.apps[9].description",
                                    "@type": "TextCell",
                                    "text": "*Why choose Moon+ Reader Pro:● The #1 paid ebook reader in Google Play● The best rating (4.7)..."
                                },
                                "icon": {
                                    "@id": "$.appListCard.apps[9].icon",
                                    "@type": "ImageCell",
                                    "imageId": "//avatars.mds.yandex.net/get-google-play-app-icon/24685/283c727356e70588fbbe9582ca35f30d/%s",
                                    "src": stubs.imageUrlStub(72, 72)
                                },
                                "icon_small": {
                                    "@id": "$.appListCard.apps[9].icon_small",
                                    "@type": "ImageCell",
                                    "imageId": "//avatars.mds.yandex.net/get-google-play-app-icon/24685/283c727356e70588fbbe9582ca35f30d/%s",
                                    "src": stubs.imageUrlStub(72, 72)
                                },
                                "path": {
                                    "@id": "$.appListCard.apps[9].path",
                                    "@type": "TextCell",
                                    "text": "play.google.com"
                                },
                                "price": {
                                    "@id": "$.appListCard.apps[9].price",
                                    "@type": "MoneyCell",
                                    "unit": "RUR",
                                    "value": 299
                                },
                                "rating": {
                                    "@id": "$.appListCard.apps[9].rating",
                                    "@type": "RatingCell",
                                    "value": 4.66033983230591,
                                    "votes": 87514
                                },
                                "setups": {
                                    "@id": "$.appListCard.apps[9].setups",
                                    "@type": "TextCell",
                                    "text": "500000–1000000"
                                },
                                "store": {
                                    "@id": "$.appListCard.apps[9].store",
                                    "@type": "TextCell",
                                    "text": "GOOGLE_PLAY"
                                },
                                "title": {
                                    "@id": "$.appListCard.apps[9].title",
                                    "@type": "TextCell",
                                    "text": "Moon+ Reader Pro"
                                }
                            },
                            {
                                "@actions": {
                                    "buy": {
                                        "data": "https://play.google.com/store/apps/details?id=com.flyersoft.moonreader&hl=ru&gl=ru",
                                        "intent": "buy"
                                    },
                                    "view": {
                                        "data": "https://play.google.com/store/apps/details?id=com.flyersoft.moonreader&hl=ru&gl=ru",
                                        "intent": "view"
                                    }
                                },
                                "@id": "$.appListCard.apps[10]",
                                "@type": "AppBadgeBlock",
                                "description": {
                                    "@id": "$.appListCard.apps[10].description",
                                    "@type": "TextCell",
                                    "text": "☆ Функциональное \u0007[приложение\u0007] \u0007[для\u0007] \u0007[чтения\u0007] электронных книг и документов с большим количеством настроек и удобным интерфейсом..."
                                },
                                "icon": {
                                    "@id": "$.appListCard.apps[10].icon",
                                    "@type": "ImageCell",
                                    "imageId": "//avatars.mds.yandex.net/get-google-play-app-icon/24685/db2b10062a4b474b7725df18d191ebcf/%s",
                                    "src": stubs.imageUrlStub(72, 72)
                                },
                                "icon_small": {
                                    "@id": "$.appListCard.apps[10].icon_small",
                                    "@type": "ImageCell",
                                    "imageId": "//avatars.mds.yandex.net/get-google-play-app-icon/24685/db2b10062a4b474b7725df18d191ebcf/%s",
                                    "src": stubs.imageUrlStub(72, 72)
                                },
                                "path": {
                                    "@id": "$.appListCard.apps[10].path",
                                    "@type": "TextCell",
                                    "text": "play.google.com"
                                },
                                "price": {
                                    "@id": "$.appListCard.apps[10].price",
                                    "@type": "MoneyCell",
                                    "unit": "RUR",
                                    "value": 0
                                },
                                "rating": {
                                    "@id": "$.appListCard.apps[10].rating",
                                    "@type": "RatingCell",
                                    "value": 4.42431020736694,
                                    "votes": 221972
                                },
                                "setups": {
                                    "@id": "$.appListCard.apps[10].setups",
                                    "@type": "TextCell",
                                    "text": "10000000–50000000"
                                },
                                "store": {
                                    "@id": "$.appListCard.apps[10].store",
                                    "@type": "TextCell",
                                    "text": "GOOGLE_PLAY"
                                },
                                "title": {
                                    "@id": "$.appListCard.apps[10].title",
                                    "@type": "TextCell",
                                    "text": "Moon+ Reader"
                                }
                            }
                        ],
                        "showmore": {
                            "@actions": {
                                "view": {
                                    "data": "https://play.google.com/store/search?q=%D0%BF%D1%80%D0%B8%D0%BB%D0%BE%D0%B6%D0%B5%D0%BD%D0%B8%D1%8F%2B%D0%B4%D0%BB%D1%8F%2B%D1%87%D1%82%D0%B5%D0%BD%D0%B8%D1%8F&c=apps",
                                    "intent": "view"
                                }
                            },
                            "@id": "$.appListCard.showmore",
                            "@type": "ShowMoreBlock",
                            "path": {
                                "@id": "$.appListCard.showmore.path",
                                "@type": "TextCell",
                                "text": "play.google.com"
                            }
                        },
                        "title": {
                            "@actions": {
                                "view": {
                                    "data": "https://play.google.com/store/search?q=%D0%BF%D1%80%D0%B8%D0%BB%D0%BE%D0%B6%D0%B5%D0%BD%D0%B8%D1%8F%2B%D0%B4%D0%BB%D1%8F%2B%D1%87%D1%82%D0%B5%D0%BD%D0%B8%D1%8F&c=apps",
                                    "intent": "view"
                                }
                            },
                            "@id": "$.appListCard.title",
                            "@type": "AppSearchListTitleBlock",
                            "path": {
                                "@id": "$.appListCard.title.path",
                                "@type": "TextCell",
                                "text": "play.google.com"
                            },
                            "store": {
                                "@id": "$.appListCard.title.store",
                                "@type": "TextCell",
                                "text": "GOOGLE_PLAY"
                            },
                            "title": {
                                "@id": "$.appListCard.title.title",
                                "@type": "TextCell",
                                "text": "приложения для чтения"
                            }
                        }
                    }
                }
            }
        }
    }
};

