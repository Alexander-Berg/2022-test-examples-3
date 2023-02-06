# -*- coding: utf-8 -*-

from test_utils import TestParser
from base_parsers import JSONSerpParser

ComponentTypes = JSONSerpParser.MetricsMagicNumbers.ComponentTypes
Alignments = JSONSerpParser.MetricsMagicNumbers.Alignments


class TestYandexImagesClothesJSONParser(TestParser):

    def test_parse(self):
        components = self.parse_file('images-apphost_alice.json')['components']
        assert len(components) == 33

        true_component_market = {
            'type': 'COMPONENT',
            'text.title': 'Павлопосадский платок "Русское раздолье" 1619-15',
            'componentInfo': {
                'type': ComponentTypes.SEARCH_RESULT,
                'alignment': Alignments.LEFT
            },
            'componentUrl': {
                'pageUrl': 'https://market.yandex.ru/product--pavloposadskii-platok-russkoe-razdole-1619-15/1731473796'
            },
            'imageadd': {
                'url': 'https://avatars.mds.yandex.net/get-marketpic/1118677/market_aciT5tDF173usNJeA4e83Q/190x250',
                'candidates': [
                    'https://avatars.mds.yandex.net/get-marketpic/1118677/market_aciT5tDF173usNJeA4e83Q/190x250',
                    'https://avatars.mds.yandex.net/get-marketpic/1584748/market_1Bi0hw3v6uK9A5PN8Qocpg/orig'
                ]
            },
            "json.serpData": {
                "data": {
                    "relevance": 0.6998843551,
                    "avg_model_price": "4690",
                    "market_height": "250",
                    "model_opinions_count": None,
                    "Warnings": None,
                    "index_image": "https://avatars.mds.yandex.net/get-marketpic/1584748/market_1Bi0hw3v6uK9A5PN8Qocpg/orig",
                    "currency": "RUR",
                    "IsAdult": False,
                    "shop_domain": "imatreshki.ru",
                    "min_model_price": "4690",
                    "market_width": "250",
                    "price": "4690",
                    "market_id":
                        {
                            "type": "model",
                            "id": "1731473796"
                        },
                    "market_link": "//market.yandex.ru/product--pavloposadskii-platok-russkoe-razdole-1619-15/1731473796",
                    "url": "https://avatars.mds.yandex.net/get-marketpic/1584748/market_1Bi0hw3v6uK9A5PN8Qocpg/orig",
                    "thmb_href": "//im0-tub-ru.yandex.net/i?id=fee641bf5d119c6e5418b14e617c01d6",
                    "market_image": "//avatars.mds.yandex.net/get-marketpic/1118677/market_aciT5tDF173usNJeA4e83Q/190x250",
                    "title": "Павлопосадский платок \"Русское раздолье\" 1619-15",
                    "market_slug": "pavloposadskii-platok-russkoe-razdole-1619-15",
                    "model_rating": None,
                    "docid": "9-4-Z31BF6E5C46974273",
                    "shop_url": "//market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZq9kRuT99ezVZwCehTNY7JOyIffukJe_FWmOE0gRHqcygOih"
                                "F0VKsnxB1fb9Z7pvnYuyQ4Kj8zVsYUfYDo6EdIQ_qpHhWJSAJWCch15iS_SRMGvcjz1KnBMOoECpVUWD7FPLZX128mg5GdicbHh7"
                                "wvs1gLjHJyfRsNowreTA1oNb-U0oaDTcjcv1JWypW0ldacuOPVy7ylBJFyH6XWg-EY0Wu2iMOpStuaLL4tN7AFok1WB6nAjBOhLZbsOPkRbxnHvmW8QO_qKZwtH7ymrx908v-M"
                                "azwTiVQxXV7zAADAnKzTmNhIwpPBl4VkInomt3vgHv71cnQpf8jeqVzDo9ui94JczRiv2orlrVpfrsgCCP8BtlJkMDcR9P_VVErUq"
                                "mxVElE5hGzo33savh4cvier_Z1Bblm6011OjZPZAkR_RN9DeIva8sKZ6oNl__2NRT_MD8gSeifJ9XEphJeLLM-sHxFE2MktRcOV6mGFUy4epCue0LTJxgpN-aHpxiUOeK8GkBW"
                                "n8nBuLJyoLUn6nZpogQgXAoFd8PJCjDFqHl-nk0gJG5laTpJfWssPXFvn_y2r67hXCFSpHCck0x3csXdB6nHPsSGaMCFyAK_ZAGOoM"
                                "z6S3EujuCbm6G7BCTgHHnKndumyJJychLDXNXbDYbM9i1QTpSDYKwDuaKAOkzWhoRbu_KvFKCGrh2-aXHA29oV6A_7P0UIrqrqL8qMcsN_56AMk9_6zJtCHYgi7W-aWkrCxFvY"
                                "Ti7vulmLhGY7j2o9rmgda4pRXoDWbwYMxvvkkNCfeVG3f0VYAWcAYBwRZYiShUSO-DUOkU3bV-1OGTJ41ydUtgDjkeNObU43VtLCN3"
                                "8PHgPhVXrn_QqFp9croG7wsMYob0LHXDWyMYKBs9Sm0d0cA1wJkOXpQpNuXfksN90TNE983vgpq9I9nRggc-nXX9kd3adX4QXTXTpyoe1d5O7FywNDDJP7rFWedZhwvBPqaFTr"
                                "UNOMg,,?data=QVyKqSPyGQwNvdoowNEPjR0qK4lv4-l31Z2CT0AyIbtPaO80MXWYUJ-LXo26bPM7d5CAxqkE_kV_ICrVfTUFk4vPN"
                                "bKNspxuTssGPn9U3nu7WrSCJp7xjWIhGOBhhW61cQxpWe-I7vD8DhzIHcmJFPhMe1MB81XHd6IpyasJWCtLKG7-tAm-51nspkeb1L_Unifl21u63BcpLBj4XMtx2is8U4KFfwMW"
                                "_3yI_H_54ZS_vutX1qNG6LCDAWOffR_BDI5e41LPnLQbz6fwDll20bfbYroLDW9S_Af3b-mOP52d0ZT_n36myB45dSTB8iIZF8JFmu"
                                "YtKHnQCoKdSEltw10KSiwR_GwJppgWRxbLu4jta8KvSkUnkRKTDpH8wC_luWvsdfM-pGPVMBiTtfD4zg4zV8nF5z1RtgTInYXm_jKhF7mSpyfaPkSiDGXHoMU8Pcplwy0xiYeSDu"
                                "MU8usBozLsfpsAHlef4aZjrt01O20ceMSsd0bbx2OHnceKXaHyfvb0nxr-xggcI1r0wX9bwMrAEKM-ccRg3ux77xdjkv0,&b64e=1&sign=41989790ce42260060818ec2745efcba&keyno=1"
                },
                "type": "clothes"}
        }
        true_component_market_inactive = {
            'type': 'COMPONENT',
            'text.title': 'Кукла Barbie Стиль от Ирис Апфель № 2, FWJ28',
            'componentInfo': {
                'type': ComponentTypes.SEARCH_RESULT,
                'alignment': Alignments.LEFT
            },
            'componentUrl': {
                'pageUrl': 'https://market.yandex.ru/product--kukla-barbie-stil-ot-iris-apfel-2-fwj28/308375365'
            },
            'imageadd': {
                'url': 'https://avatars.mds.yandex.net/get-mpic/1657306/img_id1972534063548181508.jpeg/orig',
                'candidates': [
                    'https://avatars.mds.yandex.net/get-mpic/1657306/img_id1972534063548181508.jpeg/orig',
                    'https://diamondelectric.ru/images/3200/3199983/kykla_mattel_iris_apfel_doll_6.jpg'
                ]
            },
            "json.serpData": {
                "data": {
                    "relevance": 0.7554746866,
                    "avg_model_price": None,
                    "market_height": 701,
                    "model_opinions_count": None,
                    "Warnings": None,
                    "index_image": "https://diamondelectric.ru/images/3200/3199983/kykla_mattel_iris_apfel_doll_6.jpg",
                    "currency": None,
                    "IsAdult": False,
                    "shop_domain": None,
                    "min_model_price": None,
                    "market_width": 254,
                    "price": None,
                    "market_id":
                        {
                            "type": "model",
                            "id": "308375365"
                        },
                    "market_link": "//market.yandex.ru/product--kukla-barbie-stil-ot-iris-apfel-2-fwj28/308375365",
                    "url": "https://diamondelectric.ru/images/3200/3199983/kykla_mattel_iris_apfel_doll_6.jpg",
                    "thmb_href": "//im0-tub-ru.yandex.net/i?id=486e3144f3612dd94c100756d16b3be5",
                    "market_image": "//avatars.mds.yandex.net/get-mpic/1657306/img_id1972534063548181508.jpeg/orig",
                    "title": "Кукла Barbie Стиль от Ирис Апфель № 2, FWJ28",
                    "market_slug": "kukla-barbie-stil-ot-iris-apfel-2-fwj28",
                    "model_rating": None,
                    "docid": "2-2-ZA1D926F2267945C1",
                    "shop_url": None
                },
                "type": "clothes"}
        }
        true_component_schemaorg = {
            "type": "COMPONENT",
            "text.title": "Minacelentano – Le Migliori Black LP",
            "componentInfo": {
                "type": 1,
                "alignment": 3
            },
            "componentUrl": {
                "pageUrl": "https://doctorhead.ru/product/minacelentano_le_migliori_black_lp/"
            },
            "imageadd": {
                "url": "https://doctorhead.ru/upload/iblock/5c1/R_9409563_1488657056_9890.jpeg.jpg",
                "candidates": [
                    "https://doctorhead.ru/upload/iblock/5c1/R_9409563_1488657056_9890.jpeg.jpg",
                    "https://sun9-34.userapi.com/c854428/v854428800/167718/pgMJIT5VnDo.jpg"
                ]
            },
            "json.serpData": {
                "data": {
                    "relevance": 0.795096457,
                    "avg_model_price": None,
                    "market_height": "598",
                    "model_opinions_count": None,
                    "Warnings": None,
                    "index_image": "https://sun9-34.userapi.com/c854428/v854428800/167718/pgMJIT5VnDo.jpg",
                    "currency": "RUB",
                    "IsAdult": False,
                    "shop_domain": "doctorhead.ru",
                    "min_model_price": None,
                    "market_width": "600",
                    "price": "3590",
                    "market_id":
                        {
                            "type": "schema.org",
                            "id": "a4614c3d6e474b6248917dd2a6ac4b40"
                        },
                    "market_link": None,
                    "url": "https://sun9-34.userapi.com/c854428/v854428800/167718/pgMJIT5VnDo.jpg",
                    "thmb_href": "//im0-tub-ru.yandex.net/i?id=74e8074e791ffbed64e9d55431f9f2d3",
                    "market_image": "https://doctorhead.ru/upload/iblock/5c1/R_9409563_1488657056_9890.jpeg.jpg",
                    "title": "Minacelentano – Le Migliori Black LP",
                    "model_rating": None,
                    "docid": "20-11-Z27D783D853BCD8D2",
                    "shop_url": "https://doctorhead.ru/product/minacelentano_le_migliori_black_lp/"
                },
                "type": "clothes"}
        }

        assert components[0] == true_component_schemaorg
        assert components[1] == true_component_market_inactive
        assert components[3] == true_component_market

        components = self.parse_file('detected-objects.json')['components']
        assert len(components) == 22

        true_component_market = {
            'type': 'COMPONENT',
            'text.title': 'Брюки A2929653',
            'componentInfo': {
                'type': ComponentTypes.SEARCH_RESULT,
                'alignment': Alignments.LEFT
            },
            'componentUrl': {
                'pageUrl': 'https://market.yandex.ru/offer/88iwd2xfHc9Ci_4UGgeMoA'
            },
            'imageadd': {
                'url': 'https://avatars.mds.yandex.net/get-marketpic/1457492/market_Tb7hSgmsQdeW-k9Iu7suvQ/190x250',
                'candidates': [
                    'https://avatars.mds.yandex.net/get-marketpic/1457492/market_Tb7hSgmsQdeW-k9Iu7suvQ/190x250',
                    'https://trikotazh.by/uploads/kind/thumb/2000_3000_95/b5-3570-4_3.jpg',
                    'https://trikotazh.by/uploads/kind/thumb/2000_3000_95/b5-3570-4_3.jpg',
                    'http://xn--80aae0ashccrq6m.xn--p1ai/images/product_images/info_images/2615277-3.jpg'
                ]
            },
            "json.serpData": {
                "data": {
                    "dups_info":
                        [
                            {
                                "html_href": "https://trikotazh.by/shop/kind/view/id/171738",
                                "global_img_id": "5c07498e766c56a2b3b229fea0d6460d",
                                "img_size": "111KiB",
                                "smart_crop": "0x0+99x99",
                                "crc": "17332748470138186068",
                                "img_h": 1200,
                                "thmb_w_orig": 213,
                                "img_w": 800,
                                "text": "110 см. 96-98. ",
                                "thmb_h": 150,
                                "orientation": "portrait",
                                "img_href": "https://trikotazh.by/uploads/kind/thumb/2000_3000_95/b5-3570-4_3.jpg",
                                "img_size_bytes": "114060",
                                "bdr": None,
                                "html_host": "trikotazh.by",
                                "thmb_h_orig": 320,
                                "u": "https://trikotazh.by/uploads/kind/thumb/2000_3000_95/b5-3570-4_3.jpg",
                                "thmb_href": "//im0-tub-ru.yandex.net/i?id=5c07498e766c56a2b3b229fea0d6460d",
                                "thmb_w": 99,
                                "disp_w": 266,
                                "uh": "https://trikotazh.by/shop/kind/view/id/171738",
                                "score": 0.495867,
                                "smart_crop_noaspect": "22x0+59x99",
                                "title": "Брюки Wisell арт. Б5-3570/4",
                                "disp_h": 400,
                                "img_type": "JPG"
                            },
                            {
                                "html_href": "https://trikotazh.by/shop/kind/view/id/171738?mobile=false",
                                "global_img_id": "5c07498e766c56a2b3b229fea0d6460d",
                                "img_size": "111KiB",
                                "smart_crop": "0x0+99x99",
                                "crc": "17332748470138186068",
                                "img_h": 1200,
                                "thmb_w_orig": 213,
                                "img_w": 800,
                                "text": "Модные брюки-шаровары из текстильного полотна.Перед изделия сверху дополнен поясом с застежкой на пуговицу и молнию по переду. ",
                                "thmb_h": 150,
                                "orientation": "portrait",
                                "img_href": "https://trikotazh.by/uploads/kind/thumb/2000_3000_95/b5-3570-4_3.jpg",
                                "img_size_bytes": "114060",
                                "bdr": None,
                                "html_host": "trikotazh.by",
                                "thmb_h_orig": 320,
                                "u": "https://trikotazh.by/uploads/kind/thumb/2000_3000_95/b5-3570-4_3.jpg",
                                "thmb_href": "//im0-tub-ru.yandex.net/i?id=5c07498e766c56a2b3b229fea0d6460d",
                                "thmb_w": 99,
                                "disp_w": 266,
                                "uh": "https://trikotazh.by/shop/kind/view/id/171738?mobile=false",
                                "score": 0.49181,
                                "smart_crop_noaspect": "22x0+59x99",
                                "title": "Брюки Wisell арт. Б5-3570/4",
                                "disp_h": 400,
                                "img_type": "JPG"
                            },
                            {
                                "html_href": "http://xn--80aae0ashccrq6m.xn--p1ai/print_product_info.php?products_id=2138515",
                                "global_img_id": "da5719f041866f876e32b9442b37e40e",
                                "img_size": "21.7KiB",
                                "smart_crop": "0x0+99x99",
                                "crc": "9259625287038076765",
                                "img_h": 700,
                                "thmb_w_orig": 320,
                                "img_w": 700,
                                "text": "52. 34см × 10см × 39см. Wisell. ",
                                "thmb_h": 150,
                                "orientation": "landscape",
                                "img_href": "http://xn--80aae0ashccrq6m.xn--p1ai/images/product_images/info_images/2615277-3.jpg",
                                "img_size_bytes": "22234",
                                "bdr": None,
                                "html_host": "xn--80aae0ashccrq6m.xn--p1ai",
                                "thmb_h_orig": 320,
                                "u": "http://xn--80aae0ashccrq6m.xn--p1ai/images/product_images/info_images/2615277-3.jpg",
                                "thmb_href": "//im0-tub-ru.yandex.net/i?id=da5719f041866f876e32b9442b37e40e",
                                "thmb_w": 150,
                                "disp_w": 400,
                                "uh": "http://xn--80aae0ashccrq6m.xn--p1ai/print_product_info.php?products_id=2138515",
                                "score": 0.44994,
                                "smart_crop_noaspect": "21x0+59x99",
                                "title": "Брюки женские, размер 52 2138515 - Брюки - Оптом и в розницу",
                                "disp_h": 400,
                                "img_type": "JPG"
                            }
                        ],
                    "market_info":
                        {
                            "Offer": "88iwd2xfHc9Ci_4UGgeMoA",
                            "IsSchemaOrg": False,
                            "Title": "Брюки A2929653",
                            "ImageWidth": "166",
                            "ImageUrl": "//avatars.mds.yandex.net/get-marketpic/1457492/market_Tb7hSgmsQdeW-k9Iu7suvQ/190x250",
                            "IsInactiveMarket": False,
                            "Price": "3060",
                            "IsInternal": False,
                            "IsAdult": False,
                            "Rating": None,
                            "ModelId": None,
                            "ImageHeight": "250",
                            "ClusterId": None,
                            "MarketRelevance": None,
                            "Slug": None,
                            "ShopDomain": "lady-xl.ru",
                            "Source": "clothes",
                            "ImageRelevance": None,
                            "ShopUrl": "//market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mXz5pNq7h5pm5Rc2IVvC1L5G6ckNiKbzVOaNNRAKc-8IQiVHDbH9NSE6"
                            "jVruCbUiEiwie4XpFkO5Pl8p3v66s3xI5nzzdfkTfboLUFNNiUomeVeA0d2a4iWwvCa_5oDgRj9jd7dWNO7vKUQJX53LNPzcLbZrmp1AfxtwjAuosxbB-wY"
                            "Noosp8rrFAxpGfnTsvFmtiGwLpPUgCTU4uH7-LFdGY_yYcmSafvsHVJxFqif0nin_1C8xz3QvV5ELEUlDnNo0o4loRvqKASiuEnJNlBOlmDn97oXxIxqxPC"
                            "VHkSjdz4YCiLtqloaYqT_WaRlekOrxwn29tX3QIGSPqok3l3EPlRbL5Zq7KU6-1DL6CiReYWHYcMJtJiQqwy400JSDi3UhYtQwULXkn5p2RYsrWWdiTqox5"
                            "As8QKhdHICO8zA5L5rJ7IW4KvJwgLmKKotuRvBAnzrTU7Rio0ZXG_svnfmpH7vqQFAfEhzs7aSjPheIZWoulP5C5fHYflSczkdC8bsEEMiz_v-Q35qiPpzA"
                            "bxpBHdkTip7rXKwP0fLbGTl9Ujb68OZ6k8DwAzN2k4x4FMhptprBej27j3DZxVoifNojy2qN7a3CKNVcNzkfm4P0IjDaRbaZZ4S6jq48dzX78akMetYWNq7z"
                            "HckvVBdJqqPT4ONiVuWMsmLmV2uM3CVNsgPJtsbCAcbjH26gsT-tJ0wbd8bCpdlQYYopQ2gOIyOhq5LMphH8jNo7o33EwyHtZDdZ45AT3yeMxlueXkEzUAsgF"
                            "GFMd-Lkpe4mPe18H2KcQmHNtblTAKl73oWkOteiVITHORkNxweujOFc_eRaFjo2zrHT6HuDsAo72hE6jd295CZXpWNSWPQbeVT56i0h8nTnVPjbBf0iOaFrqz"
                            "qRKt-mRTwmjZ2cRGhEf10W8Oe0C863D5_L9v50VkhbJ1gYKNnUaRsLTq1c1fdihumQ1bvMG3mX_32QnRwMt3DzfSZsywaN4UI6ig,,?data=QVyKqSPyGQwNv"
                            "doowNEPjWZeuYdlYpNnpgo_jEi-3RoarJkth5ZSNLCr_bl0yf-sAoUjKhxBmRU_Dz-KUzB0aUxUVwUHSOWU2eksN4iWr09PeGHrfYHriJX6SaWTTSm5YxKe4-"
                            "13wxP3aJhvvz3efpTOb_lZO5Tu-KL2jkUx73qmXiAP_v80aCCrVJjP9a88uQ0hPHBC6z7kGPvSAtGwuN-NGzYOi5iYvOSb2uZlUe0yNA7CBaEjLI_ltl1oKGgu"
                            "2yYtcLPMpy1s59cRDAwCetPT9vxfwAHhgYGeJp7WR3JAM51uK8rr1-LDPe_q4VcJ-0WJLI6FPA37-3nk8AhOkjQZmOdNx67_VDd0nH1QR8uD7AT_WjOMviOc-P"
                            "RpYt4DdtrMBp9z17neWj8V2Ht1oVrS_vJHTTrkl6vOz9MoFyYJ18PPs2Jf5m3t8hjgnw0klMJCkQPMSSxiMvkVzBSaFmuFqEyCq_2e3L1gilbAVo8,&b64e=1"
                            "&sign=a54e2300afa6c7e8ebbe5c6da885da91&keyno=1",
                            "Category": "7811903",
                            "Currency": "RUR",
                            "MinModelPrice": None,
                            "MarketUrl": "//market.yandex.ru/offer/88iwd2xfHc9Ci_4UGgeMoA",
                            "ImageDocId": "21-8-ZA7324E3AD2A744CE",
                            "Id": "88iwd2xfHc9Ci_4UGgeMoA",
                            "AvgModelPrice": None,
                            "OpinionsCount": None,
                            "Url": "https://m.trikotazh.by/uploads/kind/thumb/2000_3000_95/b5-3570-4_3.jpg",
                            "Relevance": 3769663337
                        }
                },
                "type": "clothes"}
        }
        true_component_market_inactive = {
            'type': 'COMPONENT',
            'text.title': 'Леггинсы Bossa Nova Angry Birds 471АБ-167',
            'componentInfo': {
                'type': ComponentTypes.SEARCH_RESULT,
                'alignment': Alignments.LEFT
            },
            'componentUrl': {
                'pageUrl': 'https://market.yandex.ru/product--legginsy-bossa-nova-angry-birds-471ab-167/562276081'
            },
            'imageadd': {
                'url': 'https://avatars.mds.yandex.net/get-mpic/1937077/img_id8502563652950022104.jpeg/orig',
                'candidates': [
                    'https://avatars.mds.yandex.net/get-mpic/1937077/img_id8502563652950022104.jpeg/orig',
                    'https://wadoo.ru/upload/iblock/e0a/e0ae2281835baba7f58baf7e27e5c700.jpg',
                    'https://nn.wadoo.ru/upload/iblock/9f7/9f78b60626b40c6f9523b3f8e5ae5c4d.jpg',
                    'https://nn.wadoo.ru/upload/iblock/48a/48ada97c6d89da186922087a4331e65e.jpg'
                ]
            },
            "json.serpData": {
                "data": {
                    "dups_info":
                        [
                            {
                                "html_href": "https://wadoo.ru/catalog/bryuki1/legginsy-bossa-nova-angry-birds-471ab167-razm-226669112/",
                                "global_img_id": "c9b67f8f30a42d2683861ea284bbb72d",
                                "img_size": "21.8KiB",
                                "smart_crop": None,
                                "crc": "7242614058310748804",
                                "img_h": 701,
                                "thmb_w_orig": 148,
                                "img_w": 325,
                                "text": "Леггинсы Bossa Nova Angry Birds 471АБ-167 размер 128, сиреневый в Москве, СПб, Новосибирске. title. ",
                                "thmb_h": 150,
                                "orientation": "portrait",
                                "img_href": "https://wadoo.ru/upload/iblock/e0a/e0ae2281835baba7f58baf7e27e5c700.jpg",
                                "img_size_bytes": "22369",
                                "bdr": None,
                                "html_host": "wadoo.ru",
                                "thmb_h_orig": 320,
                                "u": "https://wadoo.ru/upload/iblock/e0a/e0ae2281835baba7f58baf7e27e5c700.jpg",
                                "thmb_href": "//im0-tub-ru.yandex.net/i?id=c9b67f8f30a42d2683861ea284bbb72d",
                                "thmb_w": 69,
                                "disp_w": 185,
                                "uh": "https://wadoo.ru/catalog/bryuki1/legginsy-bossa-nova-angry-birds-471ab167-razm-226669112/",
                                "score": 0.433629,
                                "smart_crop_noaspect": None,
                                "title": "Леггинсы Bossa Nova Angry Birds 471АБ-167 размер 128, сиреневый купить в Москве, СПб, Новосибирске по низкой цене - Интернет-маг",
                                "disp_h": 400,
                                "img_type": "JPG"
                            },
                            {
                                "html_href": "https://nn.wadoo.ru/catalog/bryuki1/legginsy-bossa-nova-angry-birds-471ab167-razm/",
                                "global_img_id": "c9b67f8f30a42d2683861ea284bbb72d",
                                "img_size": "21.8KiB",
                                "smart_crop": None,
                                "crc": "7242614058310748804",
                                "img_h": 701,
                                "thmb_w_orig": 148,
                                "img_w": 325,
                                "text": "без утепления; силуэт: зауженный; линия талии: стандартная; застежка: отсутствует; ",
                                "thmb_h": 150,
                                "orientation": "portrait",
                                "img_href": "https://nn.wadoo.ru/upload/iblock/9f7/9f78b60626b40c6f9523b3f8e5ae5c4d.jpg",
                                "img_size_bytes": "22369",
                                "bdr": None,
                                "html_host": "nn.wadoo.ru",
                                "thmb_h_orig": 320,
                                "u": "https://nn.wadoo.ru/upload/iblock/9f7/9f78b60626b40c6f9523b3f8e5ae5c4d.jpg",
                                "thmb_href": "//im0-tub-ru.yandex.net/i?id=c9b67f8f30a42d2683861ea284bbb72d",
                                "thmb_w": 69,
                                "disp_w": 185,
                                "uh": "https://nn.wadoo.ru/catalog/bryuki1/legginsy-bossa-nova-angry-birds-471ab167-razm/",
                                "score": 0.420966,
                                "smart_crop_noaspect": None,
                                "title": "Леггинсы Bossa Nova Angry Birds 471АБ-167 размер 110, сиреневый купить в Нижнем Новгороде по низкой цене - Интернет-магазин WADO",
                                "disp_h": 400,
                                "img_type": "JPG"
                            },
                            {
                                "html_href": "https://nn.wadoo.ru/catalog/bryuki-i-shorty/leginsy-bossa-nova-angry-birds-471ab167-razme/",
                                "global_img_id": "c9b67f8f30a42d2683861ea284bbb72d",
                                "img_size": "21.8KiB",
                                "smart_crop": None,
                                "crc": "7242614058310748804",
                                "img_h": 701,
                                "thmb_w_orig": 148,
                                "img_w": 325,
                                "text": "title. ",
                                "thmb_h": 150,
                                "orientation": "portrait",
                                "img_href": "https://nn.wadoo.ru/upload/iblock/48a/48ada97c6d89da186922087a4331e65e.jpg",
                                "img_size_bytes": "22369",
                                "bdr": None,
                                "html_host": "nn.wadoo.ru",
                                "thmb_h_orig": 320,
                                "u": "https://nn.wadoo.ru/upload/iblock/48a/48ada97c6d89da186922087a4331e65e.jpg",
                                "thmb_href": "//im0-tub-ru.yandex.net/i?id=c9b67f8f30a42d2683861ea284bbb72d",
                                "thmb_w": 69,
                                "disp_w": 185,
                                "uh": "https://nn.wadoo.ru/catalog/bryuki-i-shorty/leginsy-bossa-nova-angry-birds-471ab167-razme/",
                                "score": 0.420966,
                                "smart_crop_noaspect": None,
                                "title": "Легинсы Bossa Nova Angry Birds 471аб-167 размер 86, сиреневый купить в Нижнем Новгороде по низкой цене - Интернет-магазин WADOO.",
                                "disp_h": 400,
                                "img_type": "JPG"
                            }
                        ],
                    "market_info":
                        {
                            "Offer": None,
                            "IsSchemaOrg": False,
                            "Title": "Леггинсы Bossa Nova Angry Birds 471АБ-167",
                            "ImageWidth": 322,
                            "ImageUrl": "//avatars.mds.yandex.net/get-mpic/1937077/img_id8502563652950022104.jpeg/orig",
                            "IsInactiveMarket": True,
                            "Price": None,
                            "IsInternal": False,
                            "IsAdult": False,
                            "Rating": 4,
                            "ModelId": "562276081",
                            "ImageHeight": 701,
                            "ClusterId": None,
                            "MarketRelevance": None,
                            "Slug": "legginsy-bossa-nova-angry-birds-471ab-167",
                            "ShopDomain": None,
                            "Source": "clothes",
                            "ImageRelevance": None,
                            "ShopUrl": None,
                            "Category": 7812075,
                            "Currency": None,
                            "MinModelPrice": None,
                            "MarketUrl": "//market.yandex.ru/product--legginsy-bossa-nova-angry-birds-471ab-167/562276081",
                            "ImageDocId": "9-2-Z1CEF826484CADCEE",
                            "Id": "562276081",
                            "AvgModelPrice": None,
                            "OpinionsCount": 1,
                            "Url": "https://nn.wadoo.ru/upload/iblock/48a/48ada97c6d89da186922087a4331e65e.jpg",
                            "Relevance": 3425633832
                        }
                },
                "type": "clothes"
            }
        }
        true_component_schemaorg = {
            "type": "COMPONENT",
            "text.title": "Брюки Lassie (розовый)",
            "componentInfo": {
                "type": 1,
                "alignment": 3
            },
            "componentUrl": {
                "pageUrl": "https://www.esky.ru/dress/product/bryuki-lassie-rozovyy-1840670/"
            },
            "imageadd": {
                "url": "https://www.esky.ru/upload/cimg_cache/682/682285832725910f00c61db18db01b43/es_gl000827588_001.jpg",
                "candidates": [
                    "https://www.esky.ru/upload/cimg_cache/682/682285832725910f00c61db18db01b43/es_gl000827588_001.jpg",
                    "https://teleporto.ru/images/detailed/16662/GL000827588_001_000115472232905c38c0fa82847.jpg?t=1547223290",
                    "https://static.dochkisinochki.ru/upload/img_loader/43/a5/8b/GL000827588_001_0001.jpg",
                    "https://static.dochkisinochki.ru/upload/img_loader/43/a5/8b/GL000827588_001_0001.jpg"
                ]
            },
            "json.serpData": {
                "data": {
                    "dups_info":
                        [
                            {
                                "html_href": "https://teleporto.ru/bryuki-lassie-cvet-rozovyy-4606141-ru-16.html",
                                "global_img_id": "5cb40c89ef410cd7b4c2897697afc567",
                                "img_size": "292KiB",
                                "smart_crop": None,
                                "crc": "13436975980373911922",
                                "img_h": 1072,
                                "thmb_w_orig": 299,
                                "img_w": 1000,
                                "text": "Брюки Lassie , цвет: розовый 4606141 :: 9754200 ",
                                "thmb_h": 150,
                                "orientation": "portrait",
                                "img_href": "https://teleporto.ru/images/detailed/16662/GL000827588_001_000115472232905c38c0fa82847.jpg?t=1547223290",
                                "img_size_bytes": "298903",
                                "bdr": None,
                                "html_host": "teleporto.ru",
                                "thmb_h_orig": 320,
                                "u": "https://teleporto.ru/images/detailed/16662/GL000827588_001_000115472232905c38c0fa82847.jpg?t=1547223290",
                                "thmb_href": "//im0-tub-ru.yandex.net/i?id=5cb40c89ef410cd7b4c2897697afc567",
                                "thmb_w": 140,
                                "disp_w": 373,
                                "uh": "https://teleporto.ru/bryuki-lassie-cvet-rozovyy-4606141-ru-16.html",
                                "score": 0.572487,
                                "smart_crop_noaspect": None,
                                "title": "Брюки Lassie , цвет: розовый 4606141 :: 9754200",
                                "disp_h": 400,
                                "img_type": "JPG"
                            },
                            {
                                "html_href": "https://kostroma.dochkisinochki.ru/icatalog/products/9754197/",
                                "global_img_id": "5cb40c89ef410cd7b4c2897697afc567",
                                "img_size": "292KiB",
                                "smart_crop": None,
                                "crc": "13436975980373911922",
                                "img_h": 1072,
                                "thmb_w_orig": 299,
                                "img_w": 1000,
                                "text": "Брюки Lassie , цвет: розовый ",
                                "thmb_h": 150,
                                "orientation": "portrait",
                                "img_href": "https://static.dochkisinochki.ru/upload/img_loader/43/a5/8b/GL000827588_001_0001.jpg",
                                "img_size_bytes": "298903",
                                "bdr": None,
                                "html_host": "kostroma.dochkisinochki.ru",
                                "thmb_h_orig": 320,
                                "u": "https://static.dochkisinochki.ru/upload/img_loader/43/a5/8b/GL000827588_001_0001.jpg",
                                "thmb_href": "//im0-tub-ru.yandex.net/i?id=5cb40c89ef410cd7b4c2897697afc567",
                                "thmb_w": 140,
                                "disp_w": 373,
                                "uh": "https://kostroma.dochkisinochki.ru/icatalog/products/9754197/",
                                "score": 0.423073,
                                "smart_crop_noaspect": None,
                                "title": "Брюки Lassie , цвет: розовый, размер: 110, артикул: 7227015580110 - купить в Дочки-Сыночки в Костроме",
                                "disp_h": 400,
                                "img_type": "JPG"
                            },
                            {
                                "html_href": "https://ulan-udje.dochkisinochki.ru/icatalog/products/9754215/",
                                "global_img_id": "5cb40c89ef410cd7b4c2897697afc567",
                                "img_size": "292KiB",
                                "smart_crop": None,
                                "crc": "13436975980373911922",
                                "img_h": 1072,
                                "thmb_w_orig": 299,
                                "img_w": 1000,
                                "text": "Брюки Lassie , цвет: розовый в наличии в Улан-Удэ по лучшей цене, быстрая доставка, оплата при получении, гарантия качества! ",
                                "thmb_h": 150,
                                "orientation": "portrait",
                                "img_href": "https://static.dochkisinochki.ru/upload/img_loader/43/a5/8b/GL000827588_001_0001.jpg",
                                "img_size_bytes": "298903",
                                "bdr": None,
                                "html_host": "ulan-udje.dochkisinochki.ru",
                                "thmb_h_orig": 320,
                                "u": "https://static.dochkisinochki.ru/upload/img_loader/43/a5/8b/GL000827588_001_0001.jpg",
                                "thmb_href": "//im0-tub-ru.yandex.net/i?id=5cb40c89ef410cd7b4c2897697afc567",
                                "thmb_w": 140,
                                "disp_w": 373,
                                "uh": "https://ulan-udje.dochkisinochki.ru/icatalog/products/9754215/",
                                "score": 0.418836,
                                "smart_crop_noaspect": None,
                                "title": "Брюки Lassie , цвет: розовый, размер: 92, артикул: 7227015580092 - купить в Дочки-Сыночки в Улан-Удэ",
                                "disp_h": 400,
                                "img_type": "JPG"
                            }
                        ],
                    "market_info":
                        {
                            "Offer": "4d59f158c9e89f5dab849da67836c1a5",
                            "IsSchemaOrg": True,
                            "Title": "Брюки Lassie (розовый)",
                            "ImageWidth": "408",
                            "ImageUrl": "https://www.esky.ru/upload/cimg_cache/682/682285832725910f00c61db18db01b43/es_gl000827588_001.jpg",
                            "IsInactiveMarket": False,
                            "Price": "2198",
                            "IsInternal": False,
                            "IsAdult": False,
                            "Rating": None,
                            "ModelId": None,
                            "ImageHeight": "408",
                            "ClusterId": None,
                            "MarketRelevance": None,
                            "Slug": None,
                            "ShopDomain": "www.esky.ru",
                            "Source": "clothes",
                            "ImageRelevance": None,
                            "ShopUrl": "https://www.esky.ru/dress/product/bryuki-lassie-rozovyy-1840670/",
                            "Category": None,
                            "Currency": "RUB",
                            "MinModelPrice": None,
                            "MarketUrl": None,
                            "ImageDocId": "13-6-Z424EE252001072C9",
                            "Id": "4d59f158c9e89f5dab849da67836c1a5",
                            "AvgModelPrice": None,
                            "OpinionsCount": None,
                            "Url": "https://static.dochkisinochki.ru/upload/img_loader/43/a5/8b/GL000827588_001_0001.jpg",
                            "Relevance": 3776958686
                        }
                },
                "type": "clothes"
            }
        }

        assert components[0] == true_component_schemaorg
        assert components[11] == true_component_market_inactive
        assert components[1] == true_component_market

        components = self.parse_file('detected-objects_images_alice_response.json')['components']
        assert len(components) == 22

        true_component_market = {
            'type': 'COMPONENT',
            'text.title': 'Брюки A2929653',
            'componentInfo': {
                'type': ComponentTypes.SEARCH_RESULT,
                'alignment': Alignments.LEFT
            },
            'componentUrl': {
                'pageUrl': 'https://market.yandex.ru/offer/88iwd2xfHc9Ci_4UGgeMoA'
            },
            'imageadd': {
                'url': 'https://avatars.mds.yandex.net/get-marketpic/1457492/market_Tb7hSgmsQdeW-k9Iu7suvQ/190x250',
                'candidates': [
                    'https://avatars.mds.yandex.net/get-marketpic/1457492/market_Tb7hSgmsQdeW-k9Iu7suvQ/190x250',
                    'https://m.trikotazh.by/uploads/kind/thumb/2000_3000_95/b5-3570-4_3.jpg'
                ]
            },
            "json.serpData": {
                "data": {
                    "relevance": 3769663337,
                    "avg_model_price": None,
                    "market_height": "250",
                    "Warnings": None,
                    "model_opinions_count": None,
                    "index_image": "https://m.trikotazh.by/uploads/kind/thumb/2000_3000_95/b5-3570-4_3.jpg",
                    "currency": "RUR",
                    "IsAdult": False,
                    "category": "7811903",
                    "shop_domain": "lady-xl.ru",
                    "min_model_price": None,
                    "market_width": "166",
                    "price": "3060",
                    "market_id":
                        {
                            "type": "offer",
                            "id": "88iwd2xfHc9Ci_4UGgeMoA"
                        },
                    "market_link": "//market.yandex.ru/offer/88iwd2xfHc9Ci_4UGgeMoA",
                    "url": "//market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mXz5pNq7h5pm5Rc2IVvC1L5G6ckNiKbzVOaNNRAKc-"
                            "8IQiVHDbH9NSE6jVruCbUiEiwie4XpFkO5Pl8p3v66s3xI5nzzdfkTfboLUFNNiUomeVeA0d2a4iWwvCa_5oDgRj9jd7dWNO7vKUQ"
                            "JX53LNPzcLbZrmp1AfxtwjAuosxbB-wYNoosp8rrFAxpGfnTsvFmtiGwLpPUgQ0uYwbCXsZgUiIisMf5lgJeVP8k5z293yE3U-p-Xz"
                            "Znld-7aely58ZmfDISGrVeaC1jQD8nw94qieDTGEgjlshvLDZlOBGYBKYb9r4zuUF2dL6vOZphgmmbPPbhGInihAbOlPQvBNzac4As"
                            "58JWoJvVvzr-gVeyVU07V6N4lZUQ2_7cAUfKldRRPeeduB8bdpNSEuyrlk5EauWrUrCDmx0YN7w-xSr97ETLiufFsm0N9NyrVja3Nj"
                            "vY83JTfyHV2jY1dlDNOF9QBlbvM_avCaIv0UlDf6GkyqL9IN3NP23ag4QbPimF9iK-_lBdyOo71yB3dpiSDLt1BKIiEpvmyyM0IkQK"
                            "m2jfhJb3q2E_SZTrrPunFhUJD0R9yxm-NkZYjRk8QTxdHG6lwY-YI9G5YasXe7CFr8h17U-hw3nCQN6BDltjuubnWCVMv-RPQWahkl"
                            "GURxq6YaqzDgg0WmY9UKL8WfRw-Q9tf9DZqOWBm9CkG3H3MZ9YZBgL2mbFwZPfbZWdY-lSO-qwPJoTu3WPA0cNpfMpFbuJIp30vIS8"
                            "1ao7qzr6mKZy4TeKvUOwc7GbpL7vRh570WOj_jeKNBGrUKC5ytthmN3mgqgv_h791EMXcNaeJm5CMSx85bY2-K9vd9xsr6_QVFnGNx"
                            "iFSCJ962Tpzb4Bb_OvhRuvN0ZWc266oXPTm32p9VkyVZfejCFbSDdwdcN5muQUXDci6oE6jk9Vbnl3TcOUSFoykhlzCCWg9eH3pQG-"
                            "1HummgMardIr_ep5n5XDlsZW0vDFJBECn1g,,?data=QVyKqSPyGQwNvdoowNEPjWZeuYdlYpNnpgo_jEi-3RoarJkth5ZSNLCr_bl"
                            "0yf-sAoUjKhxBmRU_Dz-KUzB0aUxUVwUHSOWU2eksN4iWr09PeGHrfYHriJX6SaWTTSm5YxKe4-13wxP3aJhvvz3efpTOb_lZO5Tu-"
                            "KL2jkUx73qmXiAP_v80aCCrVJjP9a88uQ0hPHBC6z7kGPvSAtGwuN-NGzYOi5iYvOSb2uZlUe0yNA7CBaEjLI_ltl1oKGgu2yYtcLP"
                            "Mpy1s59cRDAwCetPT9vxfwAHhgYGeJp7WR3JAM51uK8rr1-LDPe_q4VcJ-0WJLI6FPA37-3nk8AhOkjQZmOdNx67_VDd0nH1QR8uD7"
                            "AT_WjOMviOc-PRpYt4DdtrMBp9z17neWj8V2Ht1oVrS_vJHTTrkl6vOz9MoFyYbyvVRupUwDqscuKVWL93DU3h64L6cdH4kcLi1nsy"
                            "Fun-evQe0j70d-PMkvFlm-TY,&b64e=1&sign=4ef43f724180b79bcd9c009ccc0e31d4&keyno=1",
                    "thmb_href": "//im0-tub-ru.yandex.net/i?id=5c07498e766c56a2b3b229fea0d6460d",
                    "market_image": "//avatars.mds.yandex.net/get-marketpic/1457492/market_Tb7hSgmsQdeW-k9Iu7suvQ/190x250",
                    "title": "Брюки A2929653",
                    "model_rating": None,
                    "docid": "21-8-ZA7324E3AD2A744CE",
                    "shop_url": "//market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mXz5pNq7h5pm5Rc2IVvC1L5G6ckNiKbzVOaNNRAKc-"
                                "8IQiVHDbH9NSE6jVruCbUiEiwie4XpFkO5Pl8p3v66s3xI5nzzdfkTfboLUFNNiUomeVeA0d2a4iWwvCa_5oDgRj9jd7dWNO7vKUQ"
                                "JX53LNPzcLbZrmp1AfxtwjAuosxbB-wYNoosp8rrFAxpGfnTsvFmtiGwLpPUgQ0uYwbCXsZgUiIisMf5lgJeVP8k5z293yE3U-p-Xz"
                                "Znld-7aely58ZmfDISGrVeaC1jQD8nw94qieDTGEgjlshvLDZlOBGYBKYb9r4zuUF2dL6vOZphgmmbPPbhGInihAbOlPQvBNzac4As"
                                "58JWoJvVvzr-gVeyVU07V6N4lZUQ2_7cAUfKldRRPeeduB8bdpNSEuyrlk5EauWrUrCDmx0YN7w-xSr97ETLiufFsm0N9NyrVja3Nj"
                                "vY83JTfyHV2jY1dlDNOF9QBlbvM_avCaIv0UlDf6GkyqL9IN3NP23ag4QbPimF9iK-_lBdyOo71yB3dpiSDLt1BKIiEpvmyyM0IkQK"
                                "m2jfhJb3q2E_SZTrrPunFhUJD0R9yxm-NkZYjRk8QTxdHG6lwY-YI9G5YasXe7CFr8h17U-hw3nCQN6BDltjuubnWCVMv-RPQWahkl"
                                "GURxq6YaqzDgg0WmY9UKL8WfRw-Q9tf9DZqOWBm9CkG3H3MZ9YZBgL2mbFwZPfbZWdY-lSO-qwPJoTu3WPA0cNpfMpFbuJIp30vIS8"
                                "1ao7qzr6mKZy4TeKvUOwc7GbpL7vRh570WOj_jeKNBGrUKC5ytthmN3mgqgv_h791EMXcNaeJm5CMSx85bY2-K9vd9xsr6_QVFnGNx"
                                "iFSCJ962Tpzb4Bb_OvhRuvN0ZWc266oXPTm32p9VkyVZfejCFbSDdwdcN5muQUXDci6oE6jk9Vbnl3TcOUSFoykhlzCCWg9eH3pQG-"
                                "1HummgMardIr_ep5n5XDlsZW0vDFJBECn1g,,?data=QVyKqSPyGQwNvdoowNEPjWZeuYdlYpNnpgo_jEi-3RoarJkth5ZSNLCr_bl"
                                "0yf-sAoUjKhxBmRU_Dz-KUzB0aUxUVwUHSOWU2eksN4iWr09PeGHrfYHriJX6SaWTTSm5YxKe4-13wxP3aJhvvz3efpTOb_lZO5Tu-"
                                "KL2jkUx73qmXiAP_v80aCCrVJjP9a88uQ0hPHBC6z7kGPvSAtGwuN-NGzYOi5iYvOSb2uZlUe0yNA7CBaEjLI_ltl1oKGgu2yYtcLP"
                                "Mpy1s59cRDAwCetPT9vxfwAHhgYGeJp7WR3JAM51uK8rr1-LDPe_q4VcJ-0WJLI6FPA37-3nk8AhOkjQZmOdNx67_VDd0nH1QR8uD7"
                                "AT_WjOMviOc-PRpYt4DdtrMBp9z17neWj8V2Ht1oVrS_vJHTTrkl6vOz9MoFyYbyvVRupUwDqscuKVWL93DU3h64L6cdH4kcLi1nsy"
                                "Fun-evQe0j70d-PMkvFlm-TY,&b64e=1&sign=4ef43f724180b79bcd9c009ccc0e31d4&keyno=1"
                },
                "type": "clothes"
            }
        }
        true_component_market_inactive = {
            'type': 'COMPONENT',
            'text.title': 'Леггинсы Bossa Nova Angry Birds 471АБ-167',
            'componentInfo': {
                'type': ComponentTypes.SEARCH_RESULT,
                'alignment': Alignments.LEFT
            },
            'componentUrl': {
                'pageUrl': 'https://market.yandex.ru/product--legginsy-bossa-nova-angry-birds-471ab-167/562276081'
            },
            'imageadd': {
                'url': 'https://avatars.mds.yandex.net/get-mpic/1937077/img_id8502563652950022104.jpeg/orig',
                'candidates': [
                    'https://avatars.mds.yandex.net/get-mpic/1937077/img_id8502563652950022104.jpeg/orig',
                    'https://nn.wadoo.ru/upload/iblock/48a/48ada97c6d89da186922087a4331e65e.jpg'
                ]
            },
            "json.serpData": {
                "data": {
                    "relevance": 3425633832,
                    "avg_model_price": None,
                    "market_height": 701,
                    "Warnings": None,
                    "model_opinions_count": None,
                    "index_image": "https://nn.wadoo.ru/upload/iblock/48a/48ada97c6d89da186922087a4331e65e.jpg",
                    "currency": None,
                    "IsAdult": False,
                    "category": None,
                    "shop_domain": None,
                    "min_model_price": None,
                    "market_width": 322,
                    "price": None,
                    "market_id":
                        {
                            "type": "model",
                            "id": "562276081"
                        },
                    "market_link": "//market.yandex.ru/product--legginsy-bossa-nova-angry-birds-471ab-167/562276081",
                    "url": None,
                    "thmb_href": "//im0-tub-ru.yandex.net/i?id=c9b67f8f30a42d2683861ea284bbb72d",
                    "market_image": "//avatars.mds.yandex.net/get-mpic/1937077/img_id8502563652950022104.jpeg/orig",
                    "title": "Леггинсы Bossa Nova Angry Birds 471АБ-167",
                    "market_slug": "legginsy-bossa-nova-angry-birds-471ab-167",
                    "model_rating": None,
                    "docid": "9-2-Z1CEF826484CADCEE",
                    "shop_url": None
                },
                "type": "clothes"
            }
        }
        true_component_schemaorg = {
            "type": "COMPONENT",
            "text.title": "Брюки Lassie (розовый)",
            "componentInfo": {
                "type": 1,
                "alignment": 3
            },
            "componentUrl": {
                "pageUrl": "https://www.esky.ru/dress/product/bryuki-lassie-rozovyy-1840670/"
            },
            "imageadd": {
                "url": "https://www.esky.ru/upload/cimg_cache/682/682285832725910f00c61db18db01b43/es_gl000827588_001.jpg",
                "candidates": [
                    "https://www.esky.ru/upload/cimg_cache/682/682285832725910f00c61db18db01b43/es_gl000827588_001.jpg",
                    "https://static.dochkisinochki.ru/upload/img_loader/43/a5/8b/GL000827588_001_0001.jpg"
                ]
            },
            "json.serpData": {
                "data": {
                    "relevance": 3776958686,
                    "avg_model_price": None,
                    "market_height": "408",
                    "Warnings": None,
                    "model_opinions_count": None,
                    "index_image": "https://static.dochkisinochki.ru/upload/img_loader/43/a5/8b/GL000827588_001_0001.jpg",
                    "currency": "RUB",
                    "IsAdult": False,
                    "category": None,
                    "shop_domain": "www.esky.ru",
                    "min_model_price": None,
                    "market_width": "408",
                    "price": "2198",
                    "market_id":
                        {
                            "type": "schema.org",
                            "id": "4d59f158c9e89f5dab849da67836c1a5"
                        },
                    "market_link": None,
                    "url": "https://www.esky.ru/dress/product/bryuki-lassie-rozovyy-1840670/",
                    "thmb_href": "//im0-tub-ru.yandex.net/i?id=5cb40c89ef410cd7b4c2897697afc567",
                    "market_image": "https://www.esky.ru/upload/cimg_cache/682/682285832725910f00c61db18db01b43/es_gl000827588_001.jpg",
                    "title": "Брюки Lassie (розовый)",
                    "model_rating": None,
                    "docid": "13-6-Z424EE252001072C9",
                    "shop_url": "https://www.esky.ru/dress/product/bryuki-lassie-rozovyy-1840670/"
                },
                "type": "clothes"
            }
        }

        assert components[0] == true_component_schemaorg
        assert components[11] == true_component_market_inactive
        assert components[1] == true_component_market
