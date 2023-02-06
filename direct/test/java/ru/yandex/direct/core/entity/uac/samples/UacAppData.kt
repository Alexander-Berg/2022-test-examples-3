package ru.yandex.direct.core.entity.uac.samples

const val IMAGE_HASH = "kDZ1kMIazmTxwMKQyELoYQ"

val CONTENT_IMAGE_META = defaultContentImageMeta()

fun defaultContentImageMeta(imageHash: String = IMAGE_HASH): String {
    return "{\n" +
        "  \"ColorWizBack\": \"#FEFEFE\",\n" +
        "  \"ColorWizButton\": \"#D8D8D8\",\n" +
        "  \"ColorWizButtonText\": \"#000000\",\n" +
        "  \"ColorWizText\": \"#62635F\",\n" +
        "  \"avatars_thumb_size\": \"thumb\",\n" +
        "  \"crc64\": \"A0EDB296A3629F59\",\n" +
        "  \"direct_image_hash\": \"$imageHash\",\n" +
        "  \"direct_mds_meta\": {\n" +
        "    \"x150\": {\n" +
        "      \"height\": 150,\n" +
        "      \"path\": \"/get-direct/4437/$imageHash/x150\",\n" +
        "      \"smart-centers\": {\n" +
        "        \"16:5\": {\n" +
        "          \"h\": 47,\n" +
        "          \"w\": 150,\n" +
        "          \"x\": 0,\n" +
        "          \"y\": 80\n" +
        "        }\n" +
        "      },\n" +
        "      \"width\": 150\n" +
        "    }\n" +
        "  },\n" +
        "  \"orig-animated\": false,\n" +
        "  \"orig-format\": \"image/jpeg\",\n" +
        "  \"orig-size\": {\n" +
        "    \"height\": 850,\n" +
        "    \"width\": 850,\n" +
        "    \"originalWidth\": 950,\n" +
        "    \"originalHeight\": 950\n" +
        "  },\n" +
        "  \"orig-size-bytes\": 15022,\n" +
        "  \"sizes\": {\n" +
        "    \"s16x9\": {\n" +
        "      \"height\": 478,\n" +
        "      \"smart-center\": null,\n" +
        "      \"smart-centers\": null,\n" +
        "      \"width\": 850\n" +
        "    },\n" +
        "    \"thumb\": {\n" +
        "      \"height\": 850,\n" +
        "      \"smart-center\": {\n" +
        "        \"h\": 847,\n" +
        "        \"w\": 374,\n" +
        "        \"x\": 213,\n" +
        "        \"y\": 0\n" +
        "      },\n" +
        "      \"smart-centers\": {\n" +
        "        \"16:5\": {\n" +
        "          \"h\": 266,\n" +
        "          \"w\": 849,\n" +
        "          \"x\": 0,\n" +
        "          \"y\": 413\n" +
        "        }\n" +
        "      },\n" +
        "      \"width\": 850\n" +
        "    },\n" +
        "    \"wx1080\": {\n" +
        "      \"height\": 607,\n" +
        "      \"smart-center\": {\n" +
        "        \"h\": 607,\n" +
        "        \"w\": 374,\n" +
        "        \"x\": 213,\n" +
        "        \"y\": 0\n" +
        "      },\n" +
        "      \"smart-centers\": {\n" +
        "        \"16:5\": {\n" +
        "          \"h\": 266,\n" +
        "          \"w\": 849,\n" +
        "          \"x\": 0,\n" +
        "          \"y\": 292\n" +
        "        },\n" +
        "        \"16:9\": {\n" +
        "          \"h\": 478,\n" +
        "          \"w\": 849,\n" +
        "          \"x\": 0,\n" +
        "          \"y\": 56\n" +
        "        },\n" +
        "        \"1:1\": {\n" +
        "          \"h\": 607,\n" +
        "          \"w\": 608,\n" +
        "          \"x\": 181,\n" +
        "          \"y\": 0\n" +
        "        },\n" +
        "        \"26:9\": {\n" +
        "          \"h\": 295,\n" +
        "          \"w\": 849,\n" +
        "          \"x\": 0,\n" +
        "          \"y\": 270\n" +
        "        },\n" +
        "        \"3:4\": {\n" +
        "          \"h\": 607,\n" +
        "          \"w\": 456,\n" +
        "          \"x\": 159,\n" +
        "          \"y\": 0\n" +
        "        },\n" +
        "        \"4:3\": {\n" +
        "          \"h\": 607,\n" +
        "          \"w\": 809,\n" +
        "          \"x\": 20,\n" +
        "          \"y\": 0\n" +
        "        }\n" +
        "      },\n" +
        "      \"width\": 850\n" +
        "    }\n" +
        "  }\n" +
        "}"
}

const val CONTENT_VIDEO_META = "{\n" +
    "  \"creative_id\": 2147318391,\n" +
    "  \"creative_type\": \"video\",\n" +
    "  \"formats\": [],\n" +
    "  \"id\": \"60a249ed9ac0cf06d6f40f04\",\n" +
    "  \"is_tgo\": false,\n" +
    "  \"mime_type\": \"video/mp4\",\n" +
    "  \"orig-size\": {\n" +
    "    \"height\": 720,\n" +
    "    \"width\": 1280\n" +
    "  },\n" +
    "  \"preset_id\": \"15\",\n" +
    "  \"status\": \"converting\",\n" +
    "  \"thumb\": {\n" +
    "    \"height\": 720,\n" +
    "    \"preview\": {\n" +
    "      \"height\": 479,\n" +
    "      \"url\": \"https://avatars.mds.yandex.net/get-canvas-test/2003583/2a0000017979f0c73d1872d061b65d915f5a/preview480p\",\n" +
    "      \"width\": 852\n" +
    "    },\n" +
    "    \"url\": \"https://avatars.mds.yandex.net/get-canvas-test/2003583/2a0000017979f0c73d1872d061b65d915f5a/orig\",\n" +
    "    \"width\": 1280\n" +
    "  },\n" +
    "  \"vast\": \"\"\n" +
    "}"


const val CONTENT_VIDEO_NON_SKIPPABLE_META = "{\n" +
    "  \"creative_id\": 2147318391,\n" +
    "  \"creative_type\": \"non_skippable_cpm\",\n" +
    "  \"formats\": [],\n" +
    "  \"id\": \"60a249ed9ac0cf06d6f40f04\",\n" +
    "  \"is_tgo\": false,\n" +
    "  \"mime_type\": \"video/mp4\",\n" +
    "  \"orig-size\": {\n" +
    "    \"height\": 720,\n" +
    "    \"width\": 1280\n" +
    "  },\n" +
    "  \"preset_id\": \"15\",\n" +
    "  \"status\": \"converting\",\n" +
    "  \"thumb\": {\n" +
    "    \"height\": 720,\n" +
    "    \"preview\": {\n" +
    "      \"height\": 479,\n" +
    "      \"url\": \"https://avatars.mds.yandex.net/get-canvas-test/2003583/2a0000017979f0c73d1872d061b65d915f5a/preview480p\",\n" +
    "      \"width\": 852\n" +
    "    },\n" +
    "    \"url\": \"https://avatars.mds.yandex.net/get-canvas-test/2003583/2a0000017979f0c73d1872d061b65d915f5a/orig\",\n" +
    "    \"width\": 1280\n" +
    "  },\n" +
    "  \"vast\": \"\"\n" +
    "}"

const val CONTENT_HTML5_META = "{\n" +
    "  \"creative_id\": 271980,\n" +
    "  \"creative_type\": \"html5\",\n" +
    "  \"id\": \"source_id\",\n" +
    "  \"orig-size\": {\n" +
    "    \"height\": 900,\n" +
    "    \"width\": 1600\n" +
    "  },\n" +
    "  \"creative_type\": \"html5\",\n" +
    "  \"url\": \"http://example.com/source_url.zip\",\n" +
    "  \"screenshot_url\": \"https://avatars.mds.yandex.net/get-html5/42/name/orig\",\n" +
    "  \"preview_url\": \"http://example.com/preview.jpeg\"\n" +
    "}"

const val ANDROID_APP_INFO_DATA = "{\n" +
    "    \"adult\": \"3+\",\n" +
    "    \"author\":\n" +
    "    {\n" +
    "        \"email\": \"yashmanjrekar3@gmail.com\",\n" +
    "        \"id\": \"6714123194300223411\",\n" +
    "        \"name\": \"Stark Designs\"\n" +
    "    },\n" +
    "    \"name\": \"Cornerstone Round Icon Pack\",\n" +

    "    \"price\": {\"currency\":\"KZT\",\"value\":300},\n" +
    "    \"description\": \"Attention: You require custom launcher like Nova\",\n" +
    "    \"html_description\": \"<b>Attention:</b> You require\",\n" +
    "    \"rating\": {\"value\":4.8,\"votes\":21},\n" +
    "    \"categories\":\n" +
    "    [\n" +
    "        \"APPLICATION\",\n" +
    "        \"GAMES\"\n" +
    "    ],\n" +
    "    \"subcategory_eng\":\n" +
    "    [\n" +
    "        \"GAME_ACTION\",\n" +
    "        \"GAMES\"\n" +
    "    ],\n" +
    "    \"date_release\": \"2018 ж. 3 желтоқсан\",\n" +
    "    \"date_release_ts\": \"0\",\n" +
    "    \"icon\": \"http://avatars.mds.yandex.net/get-google-play-app-icon/1961900/76ad1ebdfb38bcc079af8962193477c1/orig\"\n" +
    "}"

const val ANDROID_APP_INFO_WITHOUT_CATEGORIES_DATA = "{\n" +
    "    \"adult\": \"3+\",\n" +
    "    \"author\":\n" +
    "    {\n" +
    "        \"email\": \"yashmanjrekar3@gmail.com\",\n" +
    "        \"id\": \"6714123194300223411\",\n" +
    "        \"name\": \"Stark Designs\"\n" +
    "    },\n" +
    "    \"name\": \"Cornerstone Round Icon Pack\",\n" +

    "    \"price\": {\"currency\":\"KZT\",\"value\":300},\n" +
    "    \"description\": \"Attention: You require custom launcher like Nova\",\n" +
    "    \"html_description\": \"<b>Attention:</b> You require\",\n" +
    "    \"rating\": {\"value\":4.8,\"votes\":21},\n" +
    "    \"categories\":\n" +
    "    [\n" +
    "    ],\n" +
    "    \"date_release\": \"2018 ж. 3 желтоқсан\",\n" +
    "    \"date_release_ts\": \"0\",\n" +
    "    \"icon\": \"http://avatars.mds.yandex.net/get-google-play-app-icon/1961900/76ad1ebdfb38bcc079af8962193477c1/orig\"\n" +
    "}"

const val IOS_APP_INFO_DATA = "{\n" +
    "    \"artistId\": 1164853369,\n" +
    "    \"artistName\": \"IngeniQue Corp.\",\n" +
    "    \"bundleId\": \"com.parkingtelecom.parkingtelecom\",\n" +
    "    \"trackName\": \"Parking Telecom\",\n" +
    "    \"averageUserRating\": 0.0,\n" +
    "    \"userRatingCount\": 0,\n" +
    "    \"contentAdvisoryRating\": \"4+\",\n" +
    "    \"currency\": \"USD\",\n" +
    "    \"description\": \"Park mobile\",\n" +
    "    \"formattedPrice\": \"Free\",\n" +
    "    \"icon\": \"https://is3-ssl.mzstatic.com/image/thumb/Purple114/v4/e0/14/3f/e0143fb1-dc71-c089-235a-f6965d0844f8/source/512x512bb.jpg\",\n" +
    "    \"icon_mds\": \"http://avatars.mds.yandex.net/get-itunes-icon/40548/5fa2e7f717c4874c3cb61187a2ca04df/orig\",\n" +
    "    \"minimumOsVersion\": \"9.0\",\n" +
    "    \"price\": 0,\n" +
    "    \"screenshotUrls\":\n" +
    "    [\n" +
    "        \"http://avatars.mds.yandex.net/get-itunes-screens/1872293/ea0356a6bbb34253dcde17c07e26a7e5/orig\",\n" +
    "        \"http://avatars.mds.yandex.net/get-itunes-screens/1965941/91a44ca19bdc579400f3812be3e839f7/orig\",\n" +
    "        \"http://avatars.mds.yandex.net/get-itunes-screens/1925733/2e8a22eab1d1638c50738df4150618e8/orig\",\n" +
    "        \"http://avatars.mds.yandex.net/get-itunes-screens/1965941/c46a0007820ed89003a85e9bc3cc9245/orig\",\n" +
    "        \"http://avatars.mds.yandex.net/get-itunes-screens/1925733/9f99098dc862e8d4615697dd487b0a85/orig\",\n" +
    "        \"http://avatars.mds.yandex.net/get-itunes-screens/1986935/cd4fda63c78bc1eca227471a403561ee/orig\"\n" +
    "    ],\n" +
    "    \"trackContentRating\": \"4+\",\n" +
    "    \"trackId\": 1164853370,\n" +
    "    \"version\": \"1.2.6\",\n" +
    "    \"genreIds\":[\"6002\",\"6007\"]\n" +
    "}"

const val IOS_APP_INFO_DATA_2 = "{\n" +
    "    \"artistId\": 1339587139,\n" +
    "    \"artistName\": \"GameClub\",\n" +
    "    \"bundleId\": \"com.pixeljam.snowballMobile\",\n" +
    "    \"contentAdvisoryRating\": \"4+\",\n" +
    "    \"currency\": \"USD\",\n" +
    "    \"description\": \"An avalanche\",\n" +
    "    \"trackName\": \"Snowball!! - GameClub\",\n" +
    "    \"averageUserRating\": 4.57692,\n" +
    "    \"userRatingCount\": 26,\n" +
    "    \"formattedPrice\": \"Free\",\n" +
    "    \"icon\": \"https://is4-ssl.mzstatic.com/image/thumb/Purple124/v4/c3/8b/ab/c38bab21-edca-62f7-cc7f-80ed31727ae3/source/512x512bb.jpg\",\n" +
    "    \"icon_mds\": \"https://avatars.mds.yandex.net/get-itunes-icon/24055/346b40331f0f8b963e18dade58d899a2/orig\",\n" +
    "    \"minimumOsVersion\": \"10.0\",\n" +
    "    \"price\": 0,\n" +
    "    \"screenshotUrls\":\n" +
    "    [\n" +
    "        \"http://avatars.mds.yandex.net/get-itunes-screens/1873571/c4d249fb3dc249562c8cb59cdb17c07e/orig\",\n" +
    "        \"http://avatars.mds.yandex.net/get-itunes-screens/1956266/233429e450170bd4148c2cfa3f434269/orig\",\n" +
    "        \"http://avatars.mds.yandex.net/get-itunes-screens/118612/990a72c8869a6661ce902571489ddbdb/orig\",\n" +
    "        \"http://avatars.mds.yandex.net/get-itunes-screens/1541308/a84e9386cdc0efff2d18e0c441c4fcf1/orig\",\n" +
    "        \"http://avatars.mds.yandex.net/get-itunes-screens/1873571/53dfe3d09813ba541c8e3bcde64c72ea/orig\"\n" +
    "    ],\n" +
    "    \"trackContentRating\": \"4+\",\n" +
    "    \"trackId\": 1174489793,\n" +
    "    \"version\": \"1.2.22\"\n" +
    "}"

const val IOS_APP_INFO_DATA_3 = "{\n" +
    "    \"artistId\": 1339587139,\n" +
    "    \"artistName\": \"GameClub\",\n" +
    "    \"bundleId\": \"com.pixeljam.snowballMobile\",\n" +
    "    \"contentAdvisoryRating\": \"4+\",\n" +
    "    \"currency\": \"USD\",\n" +
    "    \"description\": \"An avalanche\",\n" +
    "    \"trackName\": \"Snowball!! - GameClub\",\n" +
    "    \"averageUserRating\": 4.57692,\n" +
    "    \"userRatingCount\": 26,\n" +
    "    \"formattedPrice\": \"Free\",\n" +
    "    \"icon\": \"https://is4-ssl.mzstatic.com/image/thumb/Purple124/v4/c3/8b/ab/c38bab21-edca-62f7-cc7f-80ed31727ae3/source/512x512bb.jpg\",\n" +
    "    \"icon_mds\": \"https://avatars.mds.yandex.net/get-itunes-icon/24055/346b40331f0f8b963e18dade58d899a2/orig\",\n" +
    "    \"minimumOsVersion\": \"10.0\",\n" +
    "    \"price\": 0,\n" +
    "    \"screenshotUrls\":\n" +
    "    [\n" +
    "        \"http://avatars.mds.yandex.net/get-itunes-screens/1873571/c4d249fb3dc249562c8cb59cdb17c07e/orig\",\n" +
    "        \"http://avatars.mds.yandex.net/get-itunes-screens/1956266/233429e450170bd4148c2cfa3f434269/orig\",\n" +
    "        \"http://avatars.mds.yandex.net/get-itunes-screens/118612/990a72c8869a6661ce902571489ddbdb/orig\",\n" +
    "        \"http://avatars.mds.yandex.net/get-itunes-screens/1541308/a84e9386cdc0efff2d18e0c441c4fcf1/orig\",\n" +
    "        \"http://avatars.mds.yandex.net/get-itunes-screens/1873571/53dfe3d09813ba541c8e3bcde64c72ea/orig\"\n" +
    "    ],\n" +
    "    \"trackContentRating\": \"4+\",\n" +
    "    \"trackId\": 1174489793,\n" +
    "    \"version\": \"1.2.22\",\n" +
    "    \"genreIds\":[\"11002\",\"9008\",\"6006\",\"26\"]\n" +
    "}"

const val APPGALLERY_APP_INFO_DATA = "{\n" +
    "    \"appKinds\": {\n" +
    "        \"kindTypeId\": \"13\",\n" +
    "        \"kindTypeName\": \"Приложения\",\n" +
    "        \"lang\": \"RU_KZ\"\n" +
    "    },\n" +
    "    \"appName\": \"Цифровое ТВ 20 каналов бесплатно\",\n" +
    "    \"countries\": [\n" +
    "        \"RU\"\n" +
    "    ],\n" +
    "    \"dependGms\": \"0\",\n" +
    "    \"description\": \"description\",\n" +
    "    \"icon\": \"https://appimg-drru.dbankcdn.com/application/icon144/10169/8971b86e12c74722abfe8bf9ec9f19e1.png\",\n" +
    "    \"imgtag\": \"0\",\n" +
    "    \"language\": \"ru_kz\",\n" +
    "    \"minAge\": 12,\n" +
    "    \"pkgName\": \"limehd.ru.ctv\",\n" +
    "    \"rateNum\": 0,\n" +
    "    \"region\": \"DEFAULT\",\n" +
    "    \"releaseDate\": \"2021-04-21\",\n" +
    "    \"screenShots\": [\n" +
    "        \"https://appimg-drru.dbankcdn.com/application/screenshut1/10169/8971b86e12c74722abfe8bf9ec9f19e1.jpg\",\n" +
    "        \"https://appimg-drru.dbankcdn.com/application/screenshut2/10169/8971b86e12c74722abfe8bf9ec9f19e1.jpg\",\n" +
    "        \"https://appimg-drru.dbankcdn.com/application/screenshut3/10169/8971b86e12c74722abfe8bf9ec9f19e1.jpg\",\n" +
    "        \"https://appimg-drru.dbankcdn.com/application/screenshut4/10169/8971b86e12c74722abfe8bf9ec9f19e1.jpg\"\n" +
    "    ],\n" +
    "    \"stars\": 0\n" +
    "}"
