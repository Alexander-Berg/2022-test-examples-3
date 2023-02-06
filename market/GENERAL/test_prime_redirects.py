#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa


from core.types import (
    BlueOffer,
    CardCategoryVendor,
    CardNavCategory,
    CardVendor,
    FormalizedParam,
    GLType,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MnPlace,
    Model,
    NavCategory,
    Offer,
    Promo,
    PromoType,
    QueryEntityCtr,
    RedirectWhiteListRecord,
    ReportState,
    ReqwExtMarkupMarketShop,
    ReqwExtMarkupToken,
    ReqwExtMarkupTokenChar,
    Shop,
    Suggestion,
    Vendor,
)
from core.testcase import TestCase, main
from core.types.card import CardCategory
from core.types.vendor import PublishedVendor
from core.matcher import Contains, ElementCount, EmptyList, LikeUrl, NoKey, NotEmpty
from core.types.hypercategory import ADULT_CATEG_ID

import urllib

from unittest import skip


class T(TestCase):
    @classmethod
    def prepare(cls):
        """
        nid = {200;299}
        vendor id = {1;30}
        hid = {20;40}
        """
        cls.settings.default_search_experiment_flags += [
            'market_disable_redirect_to_model_by_formula=0'  # по умолчанию в проде выключены редиректы в модель
        ]
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.speller.on_default_request().respond()

        # test_virtual_nid data
        cls.index.cards += [CardNavCategory(nid=213)]

        cls.index.navtree += [NavCategory(nid=213, name='good', short_name='marvellous', uniq_name='awesome')]

        # test_ex_nonvisual_multivendor data
        cls.index.cards += [
            CardVendor(vendor_id=10),
            CardVendor(vendor_id=11),
            CardVendor(visual=True, vendor_id=99),
            CardVendor(visual=True, vendor_id=12),
            CardVendor(visual=True, vendor_id=1337),
        ]

        cls.index.vendors += [
            Vendor(vendor_id=9, aliases=['samsung'], name='samsung'),
            Vendor(vendor_id=10, aliases=['Somsung', 'Samsung'], name='Samsung'),
            Vendor(vendor_id=11, aliases=['samsung toshiba', 'toshiba samsung'], name='toshiba samsung'),
            Vendor(vendor_id=12, name='acoola kids'),
            Vendor(vendor_id=1337, name='ananas'),
            Vendor(vendor_id=99, name='yandex', is_fake=True),
        ]

        cls.speller.on_request(text='sumsung').respond(
            originalText='s<fix>u</fix>msung', fixedText='s<fix>a</fix>msung', reliability=10000
        )

        # фигово исправляет опечатку - пользователю не понравится
        cls.speller.on_request(text='toshiba samsung').respond(
            originalText='toshiba samsung', fixedText='tohiba s<fix>u</fix>msung', reliability=10000
        )

        cls.index.offers += [
            Offer(title='samsung'),
            Offer(title='acoola kids'),
            Offer(title='tohiba china'),
            Offer(title='toshiba japan'),
        ]

        # test_ex_visual_category_redirect data
        cls.index.offers += [
            Offer(title='clothes1', visual=True, hid=30),
            Offer(title='clothes2', visual=True, hid=30),
        ]
        cls.index.hypertree += [HyperCategory(hid=30, name="clothes", visual=True)]

        # test_ex_non_visual_category_redirects
        cls.index.offers += [
            Offer(title='bracelet 1', visual=False, hid=32),
            Offer(title='bracelet 2', visual=False, hid=32),
        ]

        cls.index.hypertree += [HyperCategory(hid=32, name="bracelets", visual=False)]

        # create card, hyper and navigation nodes for test_redirect_to_catalogmodel
        cls.index.hypertree += [
            HyperCategory(hid=309, output_type=HyperCategoryType.GURU, visual=False, name="electrocars"),
            HyperCategory(hid=310, output_type=HyperCategoryType.GURU, visual=False, name="mobile phones"),
        ]

        cls.index.gltypes += [
            GLType(param_id=310001, hid=310, gltype=GLType.ENUM, values=[1, 2]),
            GLType(param_id=310002, hid=310, gltype=GLType.BOOL),
        ]

        cls.formalizer.on_default_request().respond()
        cls.formalizer.on_request(hid=310, query="mobile phones").respond(
            formalized_params=[
                FormalizedParam(param_id=310001, value=2, is_numeric=False, value_positions=(0, 6)),
                FormalizedParam(param_id=310002, value=1, is_numeric=False, value_positions=(7, 10)),
            ]
        )

        cls.index.navtree += [
            NavCategory(nid=255, hid=309, is_blue=0),
            NavCategory(nid=256, hid=310, is_blue=0),
            NavCategory(nid=8979, hid=520, is_blue=0),
            NavCategory(nid=12825, hid=524, is_blue=0),
            NavCategory(nid=12826, hid=525, is_blue=0),
            NavCategory(nid=12827, hid=526, is_blue=0),
        ]

        cls.index.cards += [
            CardCategory(hid=309),
            CardCategory(hid=310),
        ]

        # create a category and a model in it for test_redirect_to_model
        cls.index.hypertree += [HyperCategory(hid=311, name='iphones'), HyperCategory(hid=511, name='tablets')]
        cls.index.models += [
            Model(hyperid=501, title='Apple iphone 6s 64G', hid=511),
            Model(hyperid=400, title='Apple iphone 6s 64G', hid=311),
        ]

        cls.speller.on_request(text='Apple ifone 6s 64G').respond(
            originalText='Apple i<fix>f</fix>one 6s 64G', fixedText='Apple i<fix>ph</fix>one 6s 64G', reliability=10000
        )

        cls.index.offers += [
            Offer(title='Apple iphone 6s 64G M1', hyperid=400),
            Offer(title='Apple iphone 6s 64G M2', hyperid=400),
            Offer(title='Apple iphone 6s 64G M3', hyperid=400),
            Offer(title='Apple iphone 6s 64G M4', hyperid=400),
            Offer(title='Apple iphone 6s 64G M5', hyperid=400),
            Offer(title='Apple iphone 6s 64G M6', hyperid=400),
            Offer(title='Apple iphone 6s 64G M7', hyperid=400),
            Offer(title='Apple iphone 6s 64G M8', hyperid=400),
        ]

        # guru category and vendor and cv-card for test_redirect_to_ex_guru_category_vendor
        cls.index.hypertree += [
            HyperCategory(hid=312, name="suchkorezy", visual=False, output_type=HyperCategoryType.GURU)
        ]

        cls.index.vendors += [
            Vendor(vendor_id=30, name='newApple'),
        ]

        cls.index.cards += [CardCategoryVendor(vendor_id=30, hid=312)]

        # a non-leaf guru-category...
        cls.index.hypertree += [
            HyperCategory(
                hid=555,
                visual=False,
                output_type=HyperCategoryType.GURU,
                name='pyrotechnics',
                uniq_name='pyrotechnics',
                children=[
                    HyperCategory(hid=556, visual=False, output_type=HyperCategoryType.GURU, name='fireworks'),
                    HyperCategory(hid=557, visual=False, output_type=HyperCategoryType.GURU, name='salutes'),
                ],
            )
        ]

        # ...and matching offers in all subcategories for test_redirect_to_ex_guru_catalog
        cls.index.offers += [
            Offer(title='pyrotechnics firework', hid=556),
            Offer(title='pyrotechnics salute', hid=557),
        ]

        # a non-leaf non-guru-category...
        cls.index.hypertree += [
            HyperCategory(
                hid=565,
                visual=False,
                output_type=HyperCategoryType.GURULIGHT,
                name='food',
                uniq_name='food',
                children=[
                    HyperCategory(hid=566, visual=False, output_type=HyperCategoryType.GURULIGHT, name='dog food'),
                    HyperCategory(hid=567, visual=False, output_type=HyperCategoryType.GURULIGHT, name='cat food'),
                ],
            )
        ]

        # CTR very strong feature for category redirects, so we can set it for desired output.
        cls.index.ctr.msearch.categ += [
            QueryEntityCtr('food', 565, 900, 1000),
            QueryEntityCtr('pyrotechnics', 555, 900, 1000),
        ]

        # ...and matching offers in both categories for test_redirect_to_ex_non_guru_catalog
        cls.index.offers += [
            Offer(title='food pedigree', hid=566),
            Offer(title='food wiskas', hid=567),
        ]

        # guru-category...
        cls.index.hypertree += [
            HyperCategory(
                hid=503, visual=False, output_type=HyperCategoryType.GURU, name='engine oil', uniq_name='engine oil'
            ),
        ]

        # ...and an offer for test_redirect_to_leaf_ex_guru_category_with_text
        cls.index.offers += [Offer(title='engine oil for diesel', hid=503)]

        # test_articles data
        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='how to choose a mobile phone', url='/articles/kak-vybrat-mobilnyj-telefon'),
            RedirectWhiteListRecord(
                query='how to choose a cool mobile phone', url='/articles/7-kak-vybrat-mobilnyj-telefon'
            ),
            RedirectWhiteListRecord(
                query='how to choose a nice mobile phone', url='/articles/7-KAK-vybrat-mobilnyj-telefon'
            ),
            RedirectWhiteListRecord(
                query='how to choose a beautiful mobile phone', url='/articles/KAK-vybrat-mobilnyj-telefon'
            ),
            RedirectWhiteListRecord(query='samsung', url='/brands.xml?brand=10'),
            RedirectWhiteListRecord(query='acoola kids', url='/brands/12'),
        ]

        # test_collections data
        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='gift for a girlfriend', url='/collections/v-podarok-ljubimomu'),
        ]

        # test_promo data
        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='paw patrol', url='/promo/pawpatrol_special')
        ]

        # test_special data
        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='nezabudni', url='/special/nezabudni-flash')
        ]

        # test_groceries data
        cls.index.redirect_whitelist_records += [RedirectWhiteListRecord(query='еда', url='/groceries')]

        # test_redirests_to_suggest data
        # catalogmodels
        cls.suggester.on_request(part='telephones').respond(
            suggestions=[
                Suggestion(part='beautiful telephones', url='/catalog/91491/list?gfilter=1801946:1871375&suggest=1'),
                Suggestion(
                    part='telephones', url='/catalogmodels.xml?hid=520&CAT_ID=5201&suggest=1&suggest_text=mobile+phone'
                ),
                Suggestion(part='telephones for BABUSHKA', url='/catalog/54613/list?suggest=1&hid=91303'),
            ]
        )

        cls.index.hypertree += [
            HyperCategory(hid=520, output_type=HyperCategoryType.GURU, visual=False),
        ]

        # catalog with explicit guru hid
        cls.suggester.on_request(part='cameras').respond(
            suggestions=[
                Suggestion(part='videocameras', url='/catalogmodels.xml?hid=90635&CAT_ID=105092&suggest_text=camera'),
                Suggestion(part='cameras', url='/catalog/666?hid=521&suggest_text=camera'),
            ]
        )

        cls.index.hypertree += [
            HyperCategory(hid=521, output_type=HyperCategoryType.GURU, visual=False),
        ]

        # catalog with explicit gurulight hid
        cls.suggester.on_request(part='jeans').respond(
            suggestions=[Suggestion(part='jeans', url='/catalog/666?hid=522&suggest_text=jeans')]
        )

        cls.index.hypertree += [
            HyperCategory(hid=522, output_type=HyperCategoryType.GURULIGHT, visual=True),
        ]

        # catalog with implicit hid
        cls.suggester.on_request(part='monitors').respond(
            suggestions=[Suggestion(part='monitors', url='/catalog/523/list?how=dpop&suggest_text=monitor')]
        )

        cls.index.hypertree += [HyperCategory(hid=523, output_type=HyperCategoryType.GURU, visual=False)]

        # search.xml
        cls.suggester.on_request(part='lightning for house').respond(
            suggestions=[
                Suggestion(
                    part='lightning for house',
                    url='/search.xml?hid=524&glfilter=1:2&text=lightning+for+house&suggest=1',
                )
            ]
        )

        cls.index.hypertree += [HyperCategory(hid=524, output_type=HyperCategoryType.GURU)]

        # search
        cls.suggester.on_request(part='male trunks').respond(
            suggestions=[Suggestion(part='male trunks', url='/search?hid=525&suggest=1&suggest_text=male+trunks')]
        )

        cls.index.hypertree += [HyperCategory(hid=525, output_type=HyperCategoryType.GURULIGHT, visual=True)]

        # visual brand
        cls.suggester.on_request(part='baon').respond(
            suggestions=[Suggestion(part='baon', url='/brands/111?suggest=1&suggest_text=baon+clothes')]
        )

        # non-visual brand
        cls.suggester.on_request(part='bosch').respond(
            suggestions=[Suggestion(part='bosch', url='/brands.xml?brand=222&suggest=1&suggest_text=Bosch+LLC')]
        )

        # model
        cls.suggester.on_request(part='bosch msm 67pe').respond(
            suggestions=[Suggestion(part='bosch msm 67pe', url='/product/333?hid=526&suggest=1')]
        )

        cls.index.hypertree += [HyperCategory(hid=526, output_type=HyperCategoryType.GURU, visual=False)]

        # shops-opinions
        cls.suggester.on_request(part='pyatyorochka reviews').respond(
            suggestions=[Suggestion(part='pyatyorochka reviews', url='/shop-opinions.xml?shop_id=123&suggest=1')]
        )

        # non-existent
        cls.suggester.on_default_request().respond()

        # test_simple_white_list_redirect data
        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='swiss chocolate', url='/search.xml?hid=527&text=swiss+chocolate')
        ]

        # test_unparsed_raw_url_redirect data
        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='unparsed url', url='/unparsed/12345?param=some_value'),
        ]

        cls.index.hypertree += [HyperCategory(hid=527, output_type=HyperCategoryType.GURU, visual=False)]

        cls.index.navtree += [NavCategory(nid=299, hid=527)]

        # test_orig_text_white_list_redirect data
        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='origText', url='/search.xml?hid=527&text=newText')
        ]

        # test redirect with 'adult' param
        cls.index.offers += [
            Offer(title='вибратор 1', hid=31),
            Offer(title='вибратор 2', hid=31),
            Offer(title='вибратор 3', hid=ADULT_CATEG_ID, adult=True),
            Offer(title='вибратор 4', hid=ADULT_CATEG_ID, adult=True),
            Offer(title='вибратор 5', hid=ADULT_CATEG_ID, adult=True),
            Offer(title='вибратор 6', hid=ADULT_CATEG_ID, adult=True),
            Offer(title='вибратор 7', hid=ADULT_CATEG_ID, adult=True),
            Offer(title='вибратор 8', hid=ADULT_CATEG_ID, adult=True),
        ]
        cls.index.hypertree += [HyperCategory(hid=31), HyperCategory(hid=ADULT_CATEG_ID)]

        cls.index.published_vendors += [
            PublishedVendor(vendor_id=10),
            PublishedVendor(vendor_id=12),
            PublishedVendor(vendor_id=99),
            PublishedVendor(vendor_id=153043),
            PublishedVendor(vendor_id=152987),
            PublishedVendor(vendor_id=1337),
        ]

        cls.reqwizard.on_request('купить apple').respond(
            qtree='cHicvVExS8NAGP3eJbVH6BAqgTYg1C4NQiV0Ki4tTh2Lg5RMWhQCOpS6FEEQpBIVQXTTzVJwKlIoFKzaUQUh_TtOXpImg-ns3XCP7_ve43vvlG0lxUmlDM-RwUxKJ3ZbrcN9nfLIrVGJNlI8IbokumRSlWpUpwbtkD0p3YDuQY8IGEPQK0icD5CLPZPK2LwFh0p6MJAnAyZqqN8xXwDtqhJ0VGTgqZdpC47F-oPmkmCRsdzk4mWFI0NqJlWmIwCko2BINnVYez2uwJyBhX5cwZs_ZpxZI4mT5i2q9ySl8cc9n1277-K-iABo7l-O-7cRmo8Ii_yPme8_mplHwOq9MIILKFFXZcIEfBOSM_2y2NMgHob7LDII0CRCYx8xnc26IXJHAikqdNl9c4d-FRHHmzwLa7MrEQ46aJ8v3kV2Hqaf_7mI-CXYyVbyFCRSzsveBxnyvHKJVcWvpLMcusahad2DyspP9ruck4rF4UnF9Fi_YJWg5w,,'  # noqa
        )
        cls.reqwizard.on_request('подарок на 8 марта').respond(
            qtree='cHicvVRPSBRRGP--N7Pj47nosLqwDQjTQjhJwtBpiMqKDpIE0kkWIV2CtkMX6xBCYEmwqKTgJeqWaNZhWbRARVfpEOkh3tChjh2CIOhmh6DA9-afs9NEnZrDm--99_39_b7vsUssSzO6VgATLWJDDgwoQg-chFNZCjqIc7DAhnOZ_swgDMEIVHAO4THCU4QVhC0E8b1F4HjNWCXsOvPNqDCT7tr4Nm_wl7zuToj_hoFm5L0l5h36QXqvzP1oD70nLBOxbHDwwhZS1MFIaBbBQhsHF4if7dg8soRGwQvqwBWlurNXIsu1ElmslZlODJVv8LqllKlwS_hmJK15EjGI-yCU-CtPwuCWCUkVzlei083IZtK7J979htVRgTs4TihOIIhijbvscgI0dCROaAZAoQcUNgH1WQ1xQicNmiIlInF0iq129DkeLgGJ4xCEf6ewq4n4jG9KqNz7vC66Icojk0LYzH5EWMwqLaNPxCMrppUkahIZDW8TFC3VypowhoiQutXxzyT9jRrvzJ22FEnMWDUtC7X6ZGfX75T_mEoPi6FVwMNctne9jvXzSPRT0Z9nYqpWxpY7ohOxU-ROElH6SSjkJTnG17R5XeEN9547IwI3-HrsMUgb10dfFpTDeW2yTGuB_XBemzSTbbAn57VJI6IBp0rKi9p2SXkmlyW5PBeLulwTTaIs1naCMau7swLMrJA1Yb_KGylDu-7RhnHaAgokGQ_DkXUnY_J0zP7PZEsKZ30KY7SokghLLX1ESvM34MRF4w2yoQT82s3RsduVUQOPF9EKgNdSgF_bGwhxD0zS8B7x4A4UApjnQ5j7WHBxiG413lV-cd23REktojj0BfGmdHulEVEaiUpTddWSD8oUHmWyUDV3hKKRp9m8Ndx-tuv8h4ZjQq9Mz_5Npy3_zRru61obcJM6aqSj5Gdffz_d1Xrs_Rkz07v_q7NP6piBnwLNljsZpXpO2mgGpZqQNJ2K5A4Afo6wzg,,'  # noqa
        )
        cls.reqwizard.on_default_request().respond()

        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='apple', url='/brands/153043'),
            RedirectWhiteListRecord(query='nokia', url='/brands/152987'),
        ]

        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='шелковая ночнушка', url='/search?text=шелковая+ночнушка'),
            RedirectWhiteListRecord(query='кружевное белье', url='/search?text=кружевное%20белье'),
            RedirectWhiteListRecord(
                query='мохнатые наручники',
                url='/search?text=%D0%BF%D1%83%D1%88%D0%B8%D1%81%D1%82%D1%8B%D0%B5%20%D0%BD%D0%B0%D1%80%D1%83%D1%87%D0%BD%D0%B8%D0%BA%D0%B8',
            ),
        ]

        cls.index.hypertree += [HyperCategory(hid=10861, children=[HyperCategory(hid=10862), HyperCategory(hid=10863)])]

        cls.index.offers += [
            Offer(
                title='навоз 01',
                hid=10862,
                promo=Promo(promo_type=PromoType.PROMO_CODE, key='navoz1', discount_value=50),
            ),
            Offer(title='навоз 02', hid=10862),
            Offer(title='сено 03', hid=10862),
            Offer(title='сено 04', hid=10863),
        ]

        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='подарок на 8 марта', url='/collections/podarki-dlya-nee-8-marta')
        ]

        cls.settings.ignore_qtree_decoding_failed_in_error_log = True

        cls.recommender.on_request_models_of_interest(user_id="yandexuid:", item_count=1000).respond({"models": []})

    def test_whitelist_with_stopwords(self):
        response = self.report.request_json('place=prime&text=подарок+на+8+марта&cvredirect=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of(
                        "/collections/podarki-dlya-nee-8-marta?was_redir=1&rt=10&suggest_text=подарок на 8 марта"
                    ),
                    "params": NoKey("params"),
                }
            },
        )

    def test_redirect_to_alone_category(self):
        # With disabled flag shouldn't be any redirect
        response = self.report.request_json(
            'place=prime&text=навоз&cvredirect=1&rearr-factors=market_category_redirect_treshold=0.5;market_redirect_to_alone_category=0'
        )
        self.assertFragmentNotIn(response, {"redirect": {}})

        # Without flag (enabled by default) should be redirect if all offers from one category
        response = self.report.request_json(
            'place=prime&text=навоз&cvredirect=1&rearr-factors=market_category_redirect_treshold=0.5'
        )
        self.assertFragmentIn(response, {"redirect": {"params": {"hid": ["10862"]}}})

        # If we found offers from several leafs categories we should't do redirect to any
        response = self.report.request_json(
            'place=prime&text=сено&cvredirect=1&rearr-factors=market_category_redirect_treshold=0.5'
        )
        self.assertFragmentNotIn(response, {"redirect": {}})

    @classmethod
    def prepare_cateogory_redirect_thresholds(cls):
        cls.index.hypertree += [
            HyperCategory(hid=112233),
            HyperCategory(hid=112234),
            HyperCategory(hid=112235),
            HyperCategory(hid=112236),
        ]
        cls.index.mskus += [
            MarketSku(sku=3, hyperid=2233, title="туалетный утёнок", hid=112233, blue_offers=[BlueOffer()]),
            MarketSku(sku=4, hyperid=2234, title="гадкий утёнок", hid=112234, blue_offers=[BlueOffer()]),
            MarketSku(sku=5, hyperid=2235, title="боевой нож", hid=112235, blue_offers=[BlueOffer()]),
            MarketSku(sku=6, hyperid=2236, title="боевой корабль", hid=112236, blue_offers=[BlueOffer()]),
        ]
        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 112233).respond(0.9)
        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 112234).respond(0.8)
        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 112235).respond(0.5)
        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 112236).respond(0.2)

        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 112233).respond(0.2)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 112234).respond(0.1)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 112235).respond(0.9)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 112236).respond(0.8)

    def test_category_redirect_threshold(self):

        for rgb in ['', '&rgb=blue']:
            request = 'place=prime&text=боевой+утёнок&cvredirect=1&debug=da' + rgb

            # don't redirect if top category redirect score is less than threshold
            response = self.report.request_json(request + '&rearr-factors=market_category_redirect_treshold=0.95')
            self.assertFragmentIn(
                response, {'logicTrace': [Contains('Redirect formula full_mode_f', 'score:0.8999', 'threshold:0.95')]}
            )
            self.assertFragmentNotIn(response, {"redirect": {}})

            # redirect if top category redirect score is greater than threshold
            response = self.report.request_json(request + '&rearr-factors=market_category_redirect_treshold=0.1')
            self.assertFragmentIn(
                response, {'logicTrace': [Contains('Redirect formula full_mode_f', 'score:0.8999', 'threshold:0.1')]}
            )
            self.assertFragmentIn(
                response,
                {
                    "redirect": {
                        "params": {
                            "hid": ["112233"],
                        }
                    }
                },
            )

    def test_whitelist_in_empty_category(self):
        # Don't do redirect, if categories is empty
        response = self.report.request_json("place=prime&text=вибратор&cvredirect=1")
        self.assertFragmentIn(response, {"redirect": {"params": {"hid": ["31"]}}})

    def test_white_list_cgi_text(self):
        '''Фиксируем что получится при разном указании cgi-параметра text
        "+" заменяется на пробел
        %20 заменяется на пробел
        текст декодируется
        '''

        def redirect(text):
            return {"redirect": {"params": {"text": [text]}}}

        response = self.report.request_json("place=prime&text=шелковая ночнушка&cvredirect=1&non-dummy-redirects=1")
        self.assertFragmentIn(response, redirect('шелковая ночнушка'))

        response = self.report.request_json("place=prime&text=шелковая+ночнушка&cvredirect=1&non-dummy-redirects=1")
        self.assertFragmentIn(response, redirect('шелковая ночнушка'))

        response = self.report.request_json("place=prime&text=мохнатые наручники&cvredirect=1&non-dummy-redirects=1")
        self.assertFragmentIn(response, redirect('пушистые наручники'))

    def market_do_not_add_intents_to_rs(self):
        """Интенты категорий не добавляются в параметр rs"""
        response = self.report.request_json('place=prime&text=food&cvredirect=0')
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {"name": "food", "hid": 565},
                        "intents": [
                            {"category": {"name": "dog food", "hid": 566}, "intents": NoKey("intents")},
                            {"category": {"name": "cat food", "hid": 567}, "intents": NoKey("intents")},
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

        # Получаем rs для того, чтобы его подставить в запрос после редиректа
        response = self.report.request_json('place=prime&text=food&cvredirect=1')
        rs = response.root['redirect']['params']['rs'][0]

        # Проверяем, что в листовой категории с указанным rs список категорий совпадает со списком интентов если rs не указать вовсе
        response = self.report.request_json('place=prime&text=food&cvredirect=0&hid=566&rs={}'.format(rs))
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "ownCount": 0,  # Проверяем, что для корневого узла количество собственных документов равно 0
                        "category": {"name": "food", "hid": 565},
                        "intents": [{"category": {"name": "dog food", "hid": 566}, "intents": NoKey("intents")}],
                    }
                ]
            },
            allow_different_len=False,
        )

        # Проверяем, что если rs не указывать, то список категорий будет точно таким же
        response = self.report.request_json('place=prime&text=food&cvredirect=0&hid=566')
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "ownCount": 0,
                        "category": {"name": "food", "hid": 565},
                        "intents": [{"category": {"name": "dog food", "hid": 566}, "intents": NoKey("intents")}],
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_brand_redirect_with_stopwords(self):
        response = self.report.request_json('place=prime&text=купить+apple&cvredirect=1&non-dummy-redirects=1')
        self.assertFragmentIn(response, {"brand-id": ["153043"]})

        response = self.report.request_json('place=prime&text=купить+nokia&cvredirect=1&non-dummy-redirects=1')
        self.assertFragmentNotIn(response, {"brand-id": ["152987"]})

    def test_alice_redirects(self):

        response = self.report.request_json('place=prime&text=купить+apple&cvredirect=1&alice=1')
        self.assertFragmentNotIn(response, {"brand-id": ["153043"]})

    @classmethod
    def prepare_alice_product_whitelist_redirect(cls):
        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(
                query="айфон бурый",
                url="/product--iphone-buriy/12345",
            ),
        ]

    def test_alice_product_whitelist_redirect(self):
        response = self.report.request_json('place=prime&text=айфон бурый&cvredirect=1&alice=1&non-dummy-redirects=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "modelid": ["12345"],
                    },
                    "target": "product",
                }
            },
        )

    @classmethod
    def prepare_alice_category_whitelist_redirect(cls):
        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(
                query="приспособления для ног",
                url="/catalog--prisposoblenia-dlya-nog/5100/list?hid=5000",
            ),
        ]
        cls.index.hypertree += [
            HyperCategory(hid=5000),
        ]
        cls.index.navtree += [
            NavCategory(nid=5100, hid=5000),
        ]

    def test_alice_category_whitelist_redirect(self):
        response = self.report.request_json(
            'place=prime&text=приспособления для ног&cvredirect=1&alice=1&non-dummy-redirects=1'
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "hid": ["5000"],
                        "nid": ["5100"],
                    },
                    "target": "catalog",
                }
            },
        )

    def test_redirect_to_vendor_with_fix_misspell(self):
        '''Запрос [sumsung] приводит к исправлению опечатки, т.к. по данному запросу ничего не найдено
        При перезапросе, исправленный текст samsung вызывает редирект на бренд
        Проверяем что редирект содержит данные об исправления опечатки в rs
        При передаче rs в плейс
        vendor_offers_models, bestdeals или top_categories мы получим информацию об испрваленной опечатке
        '''

        # Запрос [samsung] приводит к брендовому редиректу по вайт-листу (в редиректе нет rs)
        response = self.report.request_json('place=prime&text=samsung&cvredirect=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of("/brands.xml?brand=10&was_redir=1&rt=5&suggest_text=samsung"),
                    "params": NoKey("params"),
                }
            },
        )

        # Запрос [sumsung] приводит к тому же брендовому редиректу по вайт-листу
        # (но в редиректе есть rs, т.к. есть опечатка)
        response = self.report.request_json('place=prime&text=sumsung&cvredirect=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of("/brands.xml?brand=10&was_redir=1&rt=5&suggest_text=samsung"),
                    "params": NoKey("params"),
                }
            },
        )

        # Запрос [sumsung] с non-dummy-redirects=1 (так ходит апи)
        response = self.report.request_json('place=prime&text=sumsung&cvredirect=1&non-dummy-redirects=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "vendor",
                    "params": {
                        "brand": ["10"],
                        "rt": ["5"],
                        "was_redir": ["1"],
                        "suggest_text": ["samsung"],
                        "rs": [NotEmpty()],
                    },
                    "isVisual": False,
                }
            },
            preserve_order=True,
        )
        rs = response.root['redirect']['params']['rs'][0]

        expect = {"search": NotEmpty(), "spellchecker": {"old": "sumsung", "new": {"raw": "samsung"}}}

        response = self.report.request_json('place=vendor_offers_models&vendor_id=10&rs=' + rs)
        self.assertFragmentIn(response, expect)

        response = self.report.request_json('place=top_categories&vendor_id=10&numdoc=5&rs=' + rs)
        self.assertFragmentIn(response, expect)

        response = self.report.request_json('place=bestdeals&vendor_id=10&rs=' + rs)
        self.assertFragmentIn(response, expect)

        self.error_log.ignore('Personal category config is not available')

    def test_ex_visual_brand_redirects(self):
        """Редирект в бренд по вайт-листу"""
        response = self.report.request_json('place=prime&text=acoola+kids&cvredirect=1&non-dummy-redirects=1')

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "vendor",
                    "params": {"rt": ["5"], "brand-id": ["12"], "was_redir": ["1"], "suggest_text": ["acoola kids"]},
                    "isVisual": True,
                }
            },
            preserve_order=True,
        )

    def test_ex_visual_category_redirects(self):
        # note that ex-visual category redirects by card are not supported in prime
        # as they were the part of visual redirects logic; however they are replaced
        # by guru-light category redirects

        response = self.report.request_json('place=prime&text=clothes&cvredirect=1')

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {"was_redir": ["1"], "rt": ["9"], "srnum": ["2"], "hid": ["30"], "text": ["clothes"]},
                }
            },
            preserve_order=True,
        )
        self.assertFragmentNotIn(response, {"isVisual": True}, preserve_order=True)
        self.assertFragmentNotIn(response, {"isVisual": False}, preserve_order=True)

        self.access_log.expect(redirect_info=NotEmpty())

    def test_ex_non_visual_category_redirects(self):
        response = self.report.request_json('place=prime&text=bracelet&cvredirect=1')

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {"was_redir": ["1"], "rt": ["9"], "srnum": ["2"], "hid": ["32"], "text": ["bracelet"]},
                }
            },
            preserve_order=True,
        )
        self.assertFragmentNotIn(response, {"isVisual": True}, preserve_order=True)
        self.assertFragmentNotIn(response, {"isVisual": False}, preserve_order=True)

        self.access_log.expect(redirect_info=NotEmpty())

    @skip('We delete model redirect')
    def test_redirect_to_model(self):
        response = self.report.request_json(
            'place=prime&text=Apple+iphone+6s+64G&cvredirect=1&rearr-factors=market_model_redirect_treshold=-1'
        )

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "product",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["4"],
                        "hid": ["311"],
                        "text": ["Apple iphone 6s 64G"],
                        "modelid": ["400"],
                    },
                }
            },
        )
        self.assertFragmentNotIn(response, {"isVisual": True}, preserve_order=True)
        self.assertFragmentNotIn(response, {"isVisual": False}, preserve_order=True)

        self.access_log.expect(redirect_info=NotEmpty())

    @skip('We delete model redirect')
    def test_redirect_to_model_blue(self):
        """
        Проверяем, что на синем маркете нет модельного редиректа
        """
        response = self.report.request_json('place=prime&text=Apple+iphone+6s+64G&cvredirect=1&rgb=blue')
        self.assertFragmentNotIn(
            response,
            {
                'redirect': {
                    "target": "product",
                }
            },
        )
        self.access_log.expect(redirect_info="")

    @skip('We delete model redirect')
    def test_redirect_to_model_with_fix_misspell(self):
        '''Проверяем, что при нахождении модели по исправленному запросу в редиректе передается rs
        При передаче данного rs в productoffers или в modelinfo, результат содержит данные spellchecker-а
        '''

        response = self.report.request_json(
            'place=prime&text=Apple+ifone+6s+64G&cvredirect=1&rearr-factors=market_model_redirect_treshold=-1'
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "product",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["4"],
                        "hid": ["311"],
                        "modelid": ["400"],
                        "text": ["Apple iphone 6s 64G"],  # исправленный текст
                        "rs": [NotEmpty()],  # содержит rs с данными опечаточника
                    },
                }
            },
            allow_different_len=False,
            preserve_order=False,
        )
        rs = response.root['redirect']['params']['rs'][0]

        expected_spellchecker = {
            "search": NotEmpty(),
            "spellchecker": {"probablyTypo": True, "old": "Apple ifone 6s 64G", "new": {"raw": "Apple iphone 6s 64G"}},
        }

        response = self.report.request_json('place=productoffers&hyperid=400&hid=311&rs=' + rs)
        self.assertFragmentIn(response, expected_spellchecker)

        response = self.report.request_json('place=modelinfo&hyperid=400&hid=311&rids=0&rs=' + rs)
        self.assertFragmentIn(response, expected_spellchecker)

    def test_redirect_to_ex_non_guru_catalog(self):
        response = self.report.request_json('place=prime&text=food&cvredirect=1')

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "catalog",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["2"],
                        "srnum": ["2"],
                        "hid": ["565"],
                        # request is in suggest_text-field instead of text-field due to https://st.yandex-team.ru/MARKETOUT-16594
                        "suggest_text": ["food"],
                    },
                }
            },
            preserve_order=True,
        )
        self.assertFragmentNotIn(response, {"isVisual": True}, preserve_order=True)
        self.assertFragmentNotIn(response, {"isVisual": False}, preserve_order=True)

        self.access_log.expect(redirect_info=NotEmpty())

    def test_redirect_to_leaf_ex_guru_category_with_text(self):
        response = self.report.request_json('place=prime&text=engine+oil+for+diesel&cvredirect=1')

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["9"],
                        "srnum": ["1"],
                        "hid": ["503"],
                        "text": ["engine oil for diesel"],
                    },
                }
            },
            preserve_order=True,
        )
        self.assertFragmentNotIn(response, {"isVisual": True}, preserve_order=True)
        self.assertFragmentNotIn(response, {"isVisual": False}, preserve_order=True)

        self.access_log.expect(redirect_info=NotEmpty())

    def test_articles(self):
        response = self.report.request_json(
            'place=prime&text=how+to+choose+a+cool+mobile+phone&cvredirect=1&non-dummy-redirects=1'
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "articles",
                    "params": {
                        "article-id": ["7-kak-vybrat-mobilnyj-telefon"],
                        "was_redir": ["1"],
                        "suggest_text": ["how to choose a cool mobile phone"],
                    },
                }
            },
            preserve_order=True,
        )

        self.access_log.expect(redirect_info=NotEmpty())

        response = self.report.request_json(
            'place=prime&text=how+to+choose+a+nice+mobile+phone&cvredirect=1&non-dummy-redirects=1'
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "articles",
                    "params": {
                        "article-id": ["7-KAK-vybrat-mobilnyj-telefon"],
                        "was_redir": ["1"],
                        "suggest_text": ["how to choose a nice mobile phone"],
                    },
                }
            },
            preserve_order=True,
        )

        self.access_log.expect(redirect_info=NotEmpty())

        response = self.report.request_json(
            'place=prime&text=how+to+choose+a+beautiful+mobile+phone&cvredirect=1&non-dummy-redirects=1'
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "articles",
                    "params": {
                        "article-id": ["KAK-vybrat-mobilnyj-telefon"],
                        "was_redir": ["1"],
                        "suggest_text": ["how to choose a beautiful mobile phone"],
                    },
                }
            },
            preserve_order=True,
        )

        self.access_log.expect(redirect_info=NotEmpty())

        response = self.report.request_json(
            'place=prime&text=how+to+choose+a+mobile+phone&cvredirect=1&non-dummy-redirects=1'
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "articles",
                    "params": {
                        "article-id": ["kak-vybrat-mobilnyj-telefon"],
                        "was_redir": ["1"],
                        "suggest_text": ["how to choose a mobile phone"],
                    },
                }
            },
            preserve_order=True,
        )

        self.access_log.expect(redirect_info=NotEmpty())

    def test_articles_dummy_redirects(self):
        """То же, что test_articles, но без параметра &non-dummy-redirects=1.
        Проверяем, что работает временное решение формировать урл для
        whitelist редиректов на стороне репорта (https://st.yandex-team.ru/MARKETOUT-11794#1501590606000).
        Удалить тест в https://st.yandex-team.ru/MARKETOUT-14076
        """
        response = self.report.request_json('place=prime&text=how+to+choose+a+cool+mobile+phone&cvredirect=1')

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of(
                        "/articles/7-kak-vybrat-mobilnyj-telefon?was_redir=1&suggest_text=how to choose a cool mobile phone&rt=10"
                    ),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )

        self.access_log.expect(redirect_info=NotEmpty())

        response = self.report.request_json('place=prime&text=how+to+choose+a+nice+mobile+phone&cvredirect=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of(
                        "/articles/7-KAK-vybrat-mobilnyj-telefon?was_redir=1&rt=10&suggest_text=how to choose a nice mobile phone"
                    ),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )

        self.access_log.expect(redirect_info=NotEmpty())

        response = self.report.request_json('place=prime&text=how+to+choose+a+beautiful+mobile+phone&cvredirect=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of(
                        "/articles/KAK-vybrat-mobilnyj-telefon?was_redir=1&rt=10&suggest_text=how to choose a beautiful mobile phone"
                    ),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )

        self.access_log.expect(redirect_info=NotEmpty())

        response = self.report.request_json('place=prime&text=how+to+choose+a+mobile+phone&cvredirect=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of(
                        "/articles/kak-vybrat-mobilnyj-telefon?was_redir=1&rt=10&suggest_text=how to choose a mobile phone"
                    ),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )

        self.access_log.expect(redirect_info=NotEmpty())

    def test_collections(self):
        """редирект на коллекцию по белому списку редиректов"""

        response = self.report.request_json('place=prime&text=gift+for+a+girlfriend&cvredirect=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of(
                        "/collections/v-podarok-ljubimomu?was_redir=1&rt=10&suggest_text=gift for a girlfriend"
                    ),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )

        response = self.report.request_json('place=prime&text=gift+for+a+girlfriend&cvredirect=1&non-dummy-redirects=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "collections",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["10"],
                        "suggest_text": ["gift for a girlfriend"],
                        "collection-id": ["v-podarok-ljubimomu"],
                    },
                }
            },
            preserve_order=True,
        )

        self.access_log.expect(redirect_info=NotEmpty())

    def test_promo_redirects(self):
        """Проверяем парсинг урлов для редиректа на https://market.yandex.ru/promo/pawpatrol_special
        https://st.yandex-team.ru/MARKETOUT-43133"""

        response = self.report.request_json('place=prime&text=paw patrol&cvredirect=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of("/promo/pawpatrol_special?was_redir=1&rt=10&suggest_text=paw patrol"),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )

        response = self.report.request_json('place=prime&text=paw patrol&cvredirect=1&non-dummy-redirects=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "promo",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["10"],
                        "suggest_text": ["paw patrol"],
                        "promo-id": ["pawpatrol_special"],
                    },
                }
            },
            preserve_order=True,
        )

        self.access_log.expect(redirect_info=NotEmpty())

    def test_special_redirects(self):
        """Проверяем парсинг урлов для редиректа на https://market.yandex.ru/special/nezabudni-flash
        https://st.yandex-team.ru/MARKETOUT-43133"""

        response = self.report.request_json('place=prime&text=nezabudni&cvredirect=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of("/special/nezabudni-flash?was_redir=1&rt=10&suggest_text=nezabudni"),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )

        response = self.report.request_json('place=prime&text=nezabudni&cvredirect=1&non-dummy-redirects=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "special",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["10"],
                        "suggest_text": ["nezabudni"],
                        "special-id": ["nezabudni-flash"],
                    },
                }
            },
            preserve_order=True,
        )

        self.access_log.expect(redirect_info=NotEmpty())

    def test_groceries_redirects(self):
        """Проверяем парсинг урлов для редиректа на https://market.yandex.ru/groceries
        https://st.yandex-team.ru/MSSUP-1120"""

        response = self.report.request_json('place=prime&text=еда&cvredirect=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of("/groceries?was_redir=1&rt=10&suggest_text=еда"),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )

        response = self.report.request_json('place=prime&text=еда&cvredirect=1&non-dummy-redirects=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "groceries",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["10"],
                        "suggest_text": ["еда"],
                    },
                }
            },
            preserve_order=True,
        )

        self.access_log.expect(redirect_info=NotEmpty())

    def test_redirects_to_suggest(self):

        state = ReportState.create()
        state.search_state.no_reask_speller = True
        rs = ReportState.serialize(state)  # eJwzYgpgBAABcwCG

        # catalogmodels
        response = self.report.request_json(
            'place=prime&cvredirect=1&text=telephones&rearr-factors=market_report_blender_premium_ios_text_redirect=0'
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of(
                        "/catalogmodels.xml?hid=520&CAT_ID=5201&suggest=1&suggest_text=mobile+phone&was_redir=1&rt=12&nid=8979&rs="
                        + rs,
                        ignore_len=False,
                    ),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )
        self.assertFalse("target" in str(response))
        self.access_log.expect(redirect_info=NotEmpty())

        # catalog with explicit guru hid
        response = self.report.request_json(
            'place=prime&cvredirect=1&text=cameras&rearr-factors=market_report_blender_premium_ios_text_redirect=0'
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of(
                        "/catalog/666?hid=521&suggest_text=camera&was_redir=1&rt=12&nid=666&rs=" + rs, ignore_len=False
                    ),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )
        self.assertFalse("target" in str(response))
        self.access_log.expect(redirect_info=NotEmpty())

        # catalog with explicit gurulight hid
        response = self.report.request_json(
            'place=prime&cvredirect=1&text=jeans&rearr-factors=market_report_blender_premium_ios_text_redirect=0'
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of(
                        "/catalog/666?hid=522&suggest_text=jeans&was_redir=1&rt=12&nid=666&rs=" + rs, ignore_len=False
                    ),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )
        self.assertFalse("target" in str(response))
        self.access_log.expect(redirect_info=NotEmpty())

        # catalog with implicit hid
        response = self.report.request_json(
            'place=prime&cvredirect=1&text=monitors&rearr-factors=market_report_blender_premium_ios_text_redirect=0'
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of(
                        "/catalog/523/list?how=dpop&suggest_text=monitor&was_redir=1&rt=12&nid=523&rs=" + rs,
                        ignore_len=False,
                    ),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )
        self.assertFalse("target" in str(response))
        self.access_log.expect(redirect_info=NotEmpty())

        # search.xml
        response = self.report.request_json(
            'place=prime&cvredirect=1&text=lightning+for+house&rearr-factors=market_report_blender_premium_ios_text_redirect=0'
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of(
                        "/search.xml?hid=524&glfilter=1:2&text=lightning+for+house&suggest=1&was_redir=1&rt=12&nid=12825&rs="
                        + rs,
                        ignore_len=False,
                    ),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )
        self.assertFalse("target" in str(response))
        self.access_log.expect(redirect_info=NotEmpty())

        # search
        response = self.report.request_json(
            'place=prime&cvredirect=1&text=male+trunks&rearr-factors=market_report_blender_premium_ios_text_redirect=0'
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of(
                        "/search?hid=525&suggest=1&suggest_text=male+trunks&was_redir=1&rt=12&nid=12826&rs=" + rs,
                        ignore_len=False,
                    ),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )
        self.assertFalse("target" in str(response))
        self.access_log.expect(redirect_info=NotEmpty())

        # visual brands
        response = self.report.request_json(
            'place=prime&cvredirect=1&text=baon&rearr-factors=market_report_blender_premium_ios_text_redirect=0'
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of(
                        "/brands/111?suggest=1&suggest_text=baon+clothes&was_redir=1&rt=12&rs=" + rs, ignore_len=False
                    ),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )
        self.assertFalse("target" in str(response))
        self.access_log.expect(redirect_info=NotEmpty())

        # non-visual brands
        response = self.report.request_json(
            'place=prime&cvredirect=1&text=bosch&rearr-factors=market_report_blender_premium_ios_text_redirect=0'
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of(
                        "/brands.xml?brand=222&suggest=1&suggest_text=Bosch+LLC&was_redir=1&rt=12&rs=" + rs,
                        ignore_len=False,
                    ),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )
        self.assertFalse("target" in str(response))
        self.access_log.expect(redirect_info=NotEmpty())

        # shops-opinions
        response = self.report.request_json(
            'place=prime&cvredirect=1&text=pyatyorochka+reviews&rearr-factors=market_report_blender_premium_ios_text_redirect=0'
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of(
                        "/shop-opinions.xml?shop_id=123&suggest=1&was_redir=1&rt=12&suggest_text=pyatyorochka+reviews&rs="
                        + rs,
                        ignore_len=False,
                    ),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )
        self.assertFalse("target" in str(response))
        self.access_log.expect(redirect_info=NotEmpty())

        # non-existent query
        response = self.report.request_json(
            'place=prime&cvredirect=1&text=daladna&rearr-factors=market_report_blender_premium_ios_text_redirect=0'
        )
        self.assertFalse("redirect" in str(response))
        self.access_log.expect(redirect_info='')

        # &non-dummy-redirects
        response = self.report.request_json(
            'place=prime&cvredirect=1&text=lightning+for+house&non-dummy-redirects=0&rearr-factors=market_report_blender_premium_ios_text_redirect=0'
        )

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of(
                        "/search.xml?hid=524&glfilter=1:2&text=lightning+for+house&suggest=1&was_redir=1&rt=12&nid=12825&rs="
                        + rs,
                        ignore_len=False,
                    ),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=prime&cvredirect=1&text=lightning+for+house&non-dummy-redirects=1&rearr-factors=market_report_blender_premium_ios_text_redirect=0'
        )

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "suggest": ["1"],
                        "hid": ["524"],
                        "text": ["lightning for house"],
                        "glfilter": ["1:2"],
                        "rs": [rs],
                    },
                }
            },
            preserve_order=True,
        )

        self.assertFragmentNotIn(response, {"url": NotEmpty()}, preserve_order=False)

    def test_disable_simple_white_list_redirect(self):
        """Флагом market_disable_whitelist_redirect=1 можно отключить редиректы по вайтлисту
        https://st.yandex-team.ru/MARKETOUT-16618
        """
        # без флага есть редирект
        response = self.report.request_json('place=prime&cvredirect=1&text=swiss+chocolate')
        self.assertFragmentIn(
            response, {"redirect": {"url": LikeUrl.of("/search.xml?hid=527&text=swiss+chocolate&was_redir=1&rt=10")}}
        )

        # с флагом редиректа нет
        response = self.report.request_json(
            'place=prime&cvredirect=1&text=swiss+chocolate&rearr-factors=market_disable_whitelist_redirect%3D1'
        )
        self.assertFragmentIn(response, {"search": {}})

    def test_simple_white_list_redirect(self):
        response = self.report.request_json('place=prime&cvredirect=1&text=swiss+chocolate')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of("/search.xml?hid=527&text=swiss+chocolate&was_redir=1&rt=10"),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )

        response = self.report.request_json('place=prime&cvredirect=1&text=swiss+chocolate&non-dummy-redirects=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["10"],
                        "nid": ["299"],
                        "hid": ["527"],
                        "text": ["swiss chocolate"],
                        "suggest_text": NoKey("suggest_text"),
                    },
                }
            },
            preserve_order=True,
        )
        self.access_log.expect(redirect_info=NotEmpty())

    def test_orig_text_white_list_redirect(self):
        """Редиректы по white-листу
        text для фильтрации берется из урла вайт-листа
        suggest_text берется из запроса пользователя
        """

        response = self.report.request_json('place=prime&cvredirect=1&text=origText')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of("/search.xml?hid=527&text=newText&was_redir=1"),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )

        response = self.report.request_json('place=prime&cvredirect=1&text=origText&non-dummy-redirects=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "nid": ["299"],
                        "hid": ["527"],
                        "text": ["newText"],
                        "suggest_text": NoKey("suggest_text"),  # нет suggest_text т.к. задан text
                    },
                }
            },
            preserve_order=True,
        )
        self.access_log.expect(redirect_info=NotEmpty())

    def test_white_list_redirect_to_unparsed_raw_url(self):
        """Проверяем что white-list поддерживает урлы которые не парсятся
        фронт при этом получает редирект по сырому урлу
        апи с параметром &non-dummy-redirects=1 не получает редиректа
        """

        response = self.report.request_json('place=prime&cvredirect=1&text=unparsed+url')
        self.assertFragmentIn(
            response, {"redirect": {"url": LikeUrl.of("/unparsed/12345?param=some_value&was_redir=1")}}
        )

        response = self.report.request_json('place=prime&cvredirect=1&text=unparsed+url&non-dummy-redirects=1')
        self.assertFragmentIn(response, {'search': {'total': 0}})

    def test_adult_category_redirect_with_no_adult_param(self):
        response = self.report.request_json("place=prime&text=вибратор&cvredirect=1")
        self.assertFragmentIn(response, {"redirect": {"params": {"hid": ["31"]}}})

    def test_adult_category_redirect_with_adult_eq_1(self):
        response = self.report.request_json("place=prime&text=вибратор&adult=1&cvredirect=1")
        self.assertFragmentIn(response, {"redirect": {"params": {"hid": [str(ADULT_CATEG_ID)]}}})

    def test_adult_category_redirect_with_adult_eq_0(self):
        response = self.report.request_json("place=prime&text=вибратор&adult=0&cvredirect=1")
        self.assertFragmentIn(response, {"redirect": {"params": {"hid": ["31"]}}})

    @classmethod
    def prepare_redirect_by_word_with_apostrophe(cls):
        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query="апост'офические последствия", url="/collections/v-podarok-ljubimomu"),
        ]

    def test_redirect_by_word_with_apostrophe(self):
        response = self.report.request_json(
            "place=prime&text=апост'офические+последствия&cvredirect=1&non-dummy-redirects=1"
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "was_redir": ["1"],
                    }
                }
            },
        )

    @classmethod
    def prepare_redirect_whitelist_punctuation(cls):
        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='подарок детям', url='/catalog/68795'),
            RedirectWhiteListRecord(query='подарок детям!', url='/catalog/12345'),
        ]

    def test_redirect_whitelist_punctuation(self):
        response = self.report.request_json('place=prime&cvredirect=1&text=подарок+детям&non-dummy-redirects=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "was_redir": ["1"],
                        "nid": ["68795"],
                    }
                }
            },
        )

        response = self.report.request_json('place=prime&cvredirect=1&text=подарок+детям!&non-dummy-redirects=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "was_redir": ["1"],
                        "nid": ["12345"],
                    }
                }
            },
        )

    def test_noreask_in_redirects(self):
        """Проверяем, что если запрос пришел с noreask=1 то этот параметр окажется в редиректе"""
        response = self.report.request_json('place=prime&text=toshiba samsung&noreask=1&cvredirect=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "text": ["toshiba samsung"],
                        "noreask": ["1"],
                    }
                }
            },
        )

        # запрос без noreask приводит к исправлению опечатки
        response = self.report.request_json('place=prime&text=toshiba samsung&cvredirect=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "text": ["tohiba sumsung"],
                        "noreask": NoKey("noreask"),
                    }
                }
            },
        )

    # MARKETOUT-15389
    @classmethod
    def prepare_reviews_redirects(cls):
        # эмулируем наличие слова "отзывы" в реальном репорте
        # таким образом проверяем, что при наличии в stop-words.dat слова "отзывы" всё будет всё равно работать
        cls.index.stop_words += ['купить', 'отзывы']
        cls.index.hypertree += [
            HyperCategory(hid=1538902, name="shtuki", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=1538903, name="nakolki", visual=True),
            HyperCategory(hid=1538904, name="gitari", output_type=HyperCategoryType.GURULIGHT),
        ]

        cls.suggester.on_request(part='shtuka').respond(
            suggestions=[
                Suggestion(part='shtuka', url='/catalog/1538901?hid=1538902&suggest_text=shtuka'),
            ]
        )

        # non-guru no-redirect, репорт ничего не добавляет
        cls.suggester.on_request(part='отзывы на nakolka').respond(
            suggestions=[
                Suggestion(part='отзывы на nakolka', url='/catalog/1538901?hid=1538903&suggest_text=nakolka'),
            ]
        )
        # non-guru no-redirect, репорт ничего не добавляет
        cls.suggester.on_request(part='отзывы на gitara').respond(
            suggestions=[
                Suggestion(part='отзывы на gitara', url='/catalog/1538901?hid=1538904&suggest_text=gitara'),
            ]
        )

        # специально НЕ указываем в урле &show-reviews=1, репорт должен сам добавить
        cls.suggester.on_request(part='отзывы на shtuka').respond(
            suggestions=[
                Suggestion(part='отзывы на shtuka', url='/catalog/1538901?hid=1538902&suggest_text=shtuka'),
            ]
        )

    def test_suggest_reviews(self):
        """
        Делаем запрос
        Саджест отдает нам ссылку на каталог
        Репорт видит в запросе слово "отзывы" и добавляет параметр show-reviews=1
        """
        response = self.report.request_json('place=prime&text=отзывы на shtuka&cvredirect=1')
        self.assertFragmentIn(
            response, {"redirect": {"url": LikeUrl.of("/catalog/1538901?hid=1538902&was_redir=1&show-reviews=1")}}
        )

    def test_no_suggest_reviews_in_nonguru_or_gurulite(self):
        """
        Делаем запрос
        Саджест отдает нам ссылку на каталог
        Репорт видит в запросе слово "отзывы", но не добавляет show-reviews=1 поскольку это не-гуру или гуру-лайт категории
        """
        for name, hid in {
            "nakolka": 1538903,
            "gitara": 1538904,
        }.items():  # simple and guru-lite category (always without reviews)
            response = self.report.request_json("place=prime&text=отзывы на {}&cvredirect=1".format(name))

            # редирект есть
            self.assertFragmentIn(
                response, {"redirect": {"url": LikeUrl.of("/catalog/1538901?hid={}&was_redir=1".format(hid))}}
            )

            # но show-reviews=1 не добавлялось
            self.assertFragmentNotIn(
                response,
                {"redirect": {"url": LikeUrl.of("/catalog/1538901?hid={}&was_redir=1&show-reviews=1".format(hid))}},
            )

    def test_suggest_reviews_disabled(self):
        """
        Делаем запрос
        Саджест отдает нам ссылку на каталог
        Репорт видит в запросе слово "отзывы" и НЕ добавляет параметр show-reviews=1
        Почему не добавляет? Да потому что мы отключили эту фичу с помощью rearr-флага market_disable_reviews_redir=1
        """
        response = self.report.request_json(
            'place=prime&text=отзывы на shtuka&cvredirect=1&rearr-factors=market_disable_reviews_redir=1'
        )
        self.assertFragmentIn(response, {"redirect": {"url": LikeUrl.of("/catalog/1538901?hid=1538902&was_redir=1")}})
        self.assertFragmentNotIn(
            response, {"redirect": {"url": LikeUrl.of("/catalog/1538901?hid=1538902&was_redir=1&show-reviews=1")}}
        )

    def test_suggest_without_otzyvy_word(self):
        """
        Делаем запрос
        Но без слова отзывы
        Мы не должны попасть на хаб отзывов
        """
        response = self.report.request_json(
            'place=prime&text=shtuka&cvredirect=1&rearr-factors=market_disable_reviews_redir=1'
        )
        self.assertFragmentIn(response, {"redirect": {"url": LikeUrl.of("/catalog/1538901?hid=1538902&was_redir=1")}})
        self.assertFragmentNotIn(
            response, {"redirect": {"url": LikeUrl.of("/catalog/1538901?hid=1538902&was_redir=1&show-reviews=1")}}
        )

    @classmethod
    def prepare_clid_forwarding(cls):
        """
        MARKETOUT-16925
        Подготовка данных для прооверки прокидывания параметра clid в редирект
        """

        # создаем белосписочные записи для редиректа на категорию и модель
        # и категорию для сооствествующей записи

        cls.index.hypertree += [
            HyperCategory(hid=7101, name="category for redirect", output_type=HyperCategoryType.GURU),
        ]
        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='product redirect with clid', url='/product/7001'),
            RedirectWhiteListRecord(query='category redirect with clid', url='/search?hid=7101'),
        ]

    def test_clid_forwarding(self):
        """Прооверка прокидывания параметра clid в редирект
        https://st.yandex-team.ru/MARKETOUT-17784
        """

        # задаем запрос с текстом из редиректа на модель и clid-параметром
        # проверям, что в параметрах редиректа есть clid

        response = self.report.request_json('place=prime&cvredirect=1&text=product+redirect+with+clid&clid=123')
        self.assertFragmentIn(response, {'redirect': {'url': LikeUrl.of('/product/7001?was_redir=1&clid=123')}})

        response = self.report.request_json(
            'place=prime&cvredirect=1&text=product+redirect+with+clid&clid=123&non-dummy-redirects=1'
        )
        self.assertFragmentIn(response, {'redirect': {'params': {'clid': ['123']}}})

        # задаем запрос с текстом из редиректа в категорию и clid-параметром
        # проверям, что в параметрах редиректа есть clid

        response = self.report.request_json('place=prime&cvredirect=1&text=category+redirect+with+clid&clid=123')
        self.assertFragmentIn(response, {'redirect': {'url': LikeUrl.of('/search?hid=7101&was_redir=1&clid=123')}})

        response = self.report.request_json(
            'place=prime&cvredirect=1&text=category+redirect+with+clid&clid=123&non-dummy-redirects=1'
        )
        self.assertFragmentIn(response, {'redirect': {'params': {'clid': ['123']}}})

        # задаем такие же запросы, но без clid
        # проверям, что ничего не ломается
        response = self.report.request_json('place=prime&cvredirect=1&text=category+redirect+with+clid')
        self.assertFragmentIn(response, {'redirect': NotEmpty()})
        response = self.report.request_json('place=prime&cvredirect=1&text=category+redirect+with+clid')
        self.assertFragmentIn(response, {'redirect': NotEmpty()})

    @classmethod
    def prepare_custom_suggest_service_url(cls):
        """
        MARKETOUT-16999, MARKETOUT-32435
        Подгтовка данных для проверки флага кастомизации url
        сервиса синхронизации саджестов и редиректов
        """

        # задаем ответ сервиса саджестов с кастомной частью url (ручкой)
        cls.suggester.on_custom_url_request(part='custom suggest service', location='custom-suggest').respond(
            suggestions=[
                Suggestion(part='custom suggest service', url='/category/9001'),
            ]
        )
        cls.suggester.on_custom_url_request(part='no model redirect', location='custom-suggest').respond(
            suggestions=[Suggestion(part='no model redirect', url='/product/9999')]
        )

    def test_custom_suggest_service_url(self):
        """
        MARKETOUT-16999, MARKETOUT-32435
        Проверка флага кастомизации url
        сервиса синхронизации саджестов и редиректов
        """

        # проверяем что по запросу с текстом из заданного саджеста
        # и флагом, задающим кастомный url для сервиса саджестов
        # происходит редирект
        response = self.report.request_json(
            'place=prime&cvredirect=1'
            '&text=custom+suggest+service'
            '&rearr-factors='
            'market_suggest_redirect_service_url=custom-suggest;'
            'market_use_new_suggest_for_redirect=0'
        )

        self.assertFragmentIn(response, {'redirect': {'url': Contains('/category/9001')}})

        # редиректы по модельным саджестам были отключены в MARKETOUT-25652
        response = self.report.request_json(
            'place=prime&cvredirect=1'
            '&text=no+model+redirect'
            '&rearr-factors='
            'market_suggest_redirect_service_url=custom-suggest;'
            'market_use_new_suggest_for_redirect=0'
        )
        self.assertFragmentNotIn(response, {'redirect': {'url': Contains('/model/9999')}})

        # проверяем, что саджестный редирект не происходит без rearr-flag
        response = self.report.request_json(
            'place=prime&cvredirect=1'
            '&text=custom+suggest+service'
            '&rearr-factors=market_use_new_suggest_for_redirect=0'
        )

        self.assertFragmentNotIn(response, {'redirect': {'url': Contains('/category/9001')}})

    def test_category_with_discount_redirect(self):
        '''проверяем промокодные и промо редиректы в категории'''
        # скидка ок
        response = self.report.request_json("place=prime&cvredirect=1&text=скидки на навоз&debug=1")
        self.assertFragmentIn(response, {"redirect": {"params": {"promo-type": ["market"]}}})

        # промокод ок
        response = self.report.request_json("place=prime&cvredirect=1&text=купон на навоз")
        self.assertFragmentIn(response, {"redirect": {"params": {"promo-type": ["promo-code"]}}})

        # промокод побеждает акцию и скидку
        response = self.report.request_json("place=prime&cvredirect=1&text=купоны и скидки на навоз")
        self.assertFragmentIn(response, {"redirect": {"params": {"promo-type": ["promo-code"]}}})

        # нет промо-слов - нет редиректа
        response = self.report.request_json("place=prime&cvredirect=1&text=котики на навоз")
        self.assertFragmentNotIn(response, {"redirect": {"params": {"promo-type": NotEmpty()}}})

        # промо офферов в сене нет - поэтому редирект отменится
        response = self.report.request_json("place=prime&cvredirect=1&text=купоны и скидки на сено")
        self.assertFragmentNotIn(response, {"redirect": {"params": {"promo-type": NotEmpty()}}})

        # без флага - не работаем
        response = self.report.request_json(
            "place=prime&cvredirect=1&text=купоны и скидки на навоз&rearr-factors=market_enable_prime_promo_redirects=0"
        )
        self.assertFragmentNotIn(response, {"redirect": {"params": {"promo-type": ["promo-code"]}}})

    @classmethod
    def prepare_shop_with_discount_redirect(cls):
        cls.index.shops += [
            Shop(fesh=10, name='nasdaq'),  # магазин со скидками
            Shop(fesh=11, name='nyse'),  # магазин без скидок
        ]

        nasdaq_shop_position = ReqwExtMarkupToken(
            begin=0, end=1, data=ReqwExtMarkupMarketShop(shop_id=10, alias_type='NAME', is_good_for_matching=True)
        )
        nyse_shop_position = ReqwExtMarkupToken(
            begin=0, end=1, data=ReqwExtMarkupMarketShop(shop_id=11, alias_type='NAME', is_good_for_matching=True)
        )

        cls.reqwizard.on_request('nasdaq+купоны').respond(
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=0, end_char=6),
                ReqwExtMarkupTokenChar(begin_char=7, end_char=13),
            ],
            found_shop_positions=[nasdaq_shop_position],
        )
        cls.reqwizard.on_request('nasdaq+скидки').respond(
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=0, end_char=6),
                ReqwExtMarkupTokenChar(begin_char=7, end_char=13),
            ],
            found_shop_positions=[nasdaq_shop_position],
        )
        cls.reqwizard.on_request('nyse+скидки').respond(
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=0, end_char=4),
                ReqwExtMarkupTokenChar(begin_char=5, end_char=11),
            ],
            found_shop_positions=[nyse_shop_position],
        )

        cls.index.offers += [
            Offer(
                title='nasdaq offer',
                promo=Promo(promo_type=PromoType.PROMO_CODE, key='anvil1', discount_value=50),
                fesh=10,
            ),
            Offer(title='nyse offer', fesh=11),
        ]

    def test_shop_with_discount_redirect(self):
        experiment_flag_disabled = '&rearr-factors=market_enable_prime_promo_redirects=0'
        response_base = 'place=prime&cvredirect=1&text=%s'

        # работают редиректы на купоны ...
        response = self.report.request_json(response_base % 'nasdaq купоны')
        self.assertFragmentIn(response, {"redirect": {"params": {"fesh": ["10"], "promo-type": ["promo-code"]}}})

        # ... и скидки
        response = self.report.request_json(response_base % 'nasdaq скидки')
        self.assertFragmentIn(response, {"redirect": {"params": {"fesh": ["10"], "promo-type": ["market"]}}})

        # без промок в магазине не работают ...
        response = self.report.request_json(response_base % 'nyse скидки')
        self.assertFragmentNotIn(response, {"redirect": {"params": {"promo-type": NotEmpty()}}})

        # ... и c выключенным флагам не работают
        response = self.report.request_json(response_base % 'nasdaq скидки' + experiment_flag_disabled)
        self.assertFragmentNotIn(response, {"redirect": {"params": {"promo-type": NotEmpty()}}})

    @classmethod
    def prepare_brand_with_discount_redirect(cls):
        # отрезаем здесь стоп-слова ("со скидкой"), чтобы получить брендовый редирект
        cls.reqwizard.on_request('samsung со скидкой').respond(
            qtree='cHicdZCxS8NAFMbfu5T2CKWUltAaKIYsRqESnIKDFXFwLE6SSYvQLIq0iNKlLUUIBUFwc1MpOIVSERXSIriIIFz-Bf8CFzdBryEJGPSW-96973vH74mmmKaQhSJVQCM65FLNnb3m4X5dRkWFBViC5TRN8T7wPuiwChtQhS3YButdOkO4QLjCMDNCGCPw84LAcFcHA9eqFLMghw4VNNSxek78CdhYFMNO0R9vwCaxHRMHtSRPgZavUX6TuaYmWHBMWoQS85lQu9NBqVUw1mWHiFaMION12ZC5XtfrsTG7lXFeRe1_EuvLnQlBYtG_eO7RB4o5A67rkOsUxZgj4hPsp1eT3DgmGTi_KdlQEwLlRurBV0Qm3kmo2J2vMOiKXCXYhI2iVzfK9Py9Id8bdhA4ppqY7koDXvVxVvSrXIGinKcofX63K6V61zMUoXw0aVfijqR0eTBeKT1-vBkKlKeL6WM6cCRopjH94AeAvpF-'  # noqa
        )
        cls.reqwizard.on_request('acoola kids со скидкой').respond(
            qtree='cHicvVTPaxNBFH5vdttMp2mJCdWwWrqsh66BYvBUpFgVD8WDlAoqQbBtEppobCApWArSFBViBRG8SIUiSsFTKm2FIm2pN39ctv-C4MmLggdP4szs7KbkFxbEvezbmfd9-8333jx2kQVpV6g7CibaJA5hMMCCGJyC00EKIeDrYEMczraNtI3CNRiHDD5BWEJ4ibCGsI3Anw8IDqaMJcKuMhdGOUzQtU8k8_nchIGmz9q-jxVGQLBmfq32eawKUcMdh0E8v4EUQ2CoDAtsjOPoU-KqKgwwtRFFvgGDMEbKlQSuTPIfGmBHJil_k_6irWVwlhSO16Vr5ZVKArx0mTRcl4TlBFmp1HNOBkLEQDcAA_vdv8wRSkoI_GzGMmGXa8zRb2VTRWENmsobvYE33-c8a2R-I2O2XGPkfq0tQ0zjy1w_VPWvKv1avf5IVT9wA865aPDQDxP6i8r6jo_v4G8tlU7JkBhadnpKcumSS1IcY1JXFJSDVYfpLFQNsrrofKiEUWJqdlscYp81NqL80pVfzXrTc6p5Z_4kkoyECOdvTaYavQXZfWTjSlmHSdw2T-anZ9KzkrLdmiqk09NNe90lVgje7256o7IeFv8QnLKi6vbNgWqoMrJUCxnUSuYmisVs8m-F-IADS7FcFcTUBX8cB9FdF0g3in3Ff1jNx8hu1py883Y-lc7dSOZz-YKqwp1MdiYt2Tubsu-HiVJIzIHPXz2niBLrGi2XStjz_lnsgrGsses1154675xNZ2NvwcATFtotrv6XH3HPBB_USN06kfffz1EyX3kzgHesv-dPMr38fPdj_TBz3nhjwdnyo00ZEYPsPfAi5y2PWAgN3dlx1uQq-hiRec9b23skhyEWyo1lLO9-SpDXlf8phY8dVTFLF1PBFpVcxD6mi0qFj1A0IhR7Fr4dGu7Nzl8ZMoMDR0_eHXYzeL7KCPQglM70_g5sD5kwIIqyiBHJQcKdlI4FKIa1S6PpRQwqZp12F8D9FDTyk6v4AxJxbJI,'  # noqa
        )
        cls.index.offers += [
            Offer(fesh=10, hyperid=566, title='samsung телефон', price=1000, discount=50),
        ]

    def test_brand_with_discount_redirect(self):
        experiment_flag_disabled = '&rearr-factors=market_enable_prime_promo_redirects=0'
        response_base = 'place=prime&cvredirect=1&text=%s'

        # работают скидки на бренда
        response = self.report.request_json(response_base % 'samsung со скидкой')
        self.assertFragmentIn(
            response, {"redirect": {"params": {"text": ["samsung со скидкой"], "promo-type": ["market"]}}}
        )

        # не работают скидки на бренда при пустой выдаче с ними
        response = self.report.request_json(response_base % 'acoola kids со скидкой')
        self.assertFragmentNotIn(response, {"redirect": {"params": {"promo-type": NotEmpty()}}})

        # не работают скидки на бренд с выключенным экспериментам
        response = self.report.request_json(response_base % 'samsung со скидкой' + experiment_flag_disabled)
        self.assertFragmentNotIn(response, {"redirect": {"params": {"promo-type": NotEmpty()}}})

    def test_discount_redirect(self):
        '''
        Проверяем наличие редиректа по слову "скидка", должен добавиться переход на скидки
        '''
        response = self.report.request_json(
            "place=prime&cvredirect=1&rearr-factors=enable_promo_redirect=1&text=скидки samsung"
        )
        self.assertFragmentIn(response, {'redirect': {'params': {'was_redir': ['1'], 'filter-discount-only': ['1']}}})

    def test_price_redirect(self):
        '''
        Проверяем наличие редиректа по слову "дешево", должна добавиться сортировка по цене
        '''
        response = self.report.request_json(
            "place=prime&cvredirect=1&rearr-factors=enable_promo_redirect=1&text=дешево samsung"
        )
        self.assertFragmentIn(response, {'redirect': {'params': {'was_redir': ['1'], 'filter-discount-only': ['1']}}})

    def test_discount_and_price_redirect(self):
        '''
        Проверяем наличие редиректа по словам "дешево", "скидка", должны добавиться
        сортировка по цене и фильтр по скидкам
        '''
        response = self.report.request_json(
            "place=prime&cvredirect=1&rearr-factors=enable_promo_redirect=1&text=дешево samsung со скидкой"
        )
        self.assertFragmentIn(response, {'redirect': {'params': {'was_redir': ['1'], 'filter-discount-only': ['1']}}})

    def test_discount_redirect_empty_query(self):
        '''
        Проверяем отсутствие редиректа при пустом запросе
        '''
        response = self.report.request_json(
            "place=prime&cvredirect=1&rearr-factors=enable_promo_redirect=1&text=со скидкой"
        )
        self.assertFragmentNotIn(
            response,
            {
                'redirect': {
                    'params': {
                        'was_redir': ['1'],
                    }
                }
            },
        )

    def test_discount_and_price_no_redirect(self):
        '''
        Проверяем отсутствие редиректа при выключенном эксперименте
        '''
        response = self.report.request_json("place=prime&cvredirect=1&text=дешево samsung со скидкой")
        self.assertFragmentIn(
            response,
            {
                'redirect': {
                    'params': {
                        'was_redir': ['1'],
                    }
                }
            },
        )

        self.assertFragmentNotIn(
            response, {'redirect': {'params': {'was_redir': ['1'], 'how': ['aprice'], 'filter-discount-only': ['1']}}}
        )

    @classmethod
    def prepare_to_licensor_redirect(cls):
        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='Licensor promotional text', url='/licensor/123')
        ]

    def test_to_licensor_redirect(self):
        """
        Проверяем, что редиректы на страницы лицензиаров из белого списка:
        1) вообще случаются
        2) урл в редиректе правильный
        """
        response = self.report.request_json("place=prime&cvredirect=1&text=licensor promotional text")
        self.assertFragmentIn(response, {'redirect': {'url': LikeUrl.of('/licensor/123')}})

    @classmethod
    def prepare_rerequest_if_empty_serp(cls):
        """Подготовка данных для проверки перезапросов под флагами market_rerequest_soft_relevance_threshold и market_rerequest_no_relevance_threshold
        при пустом серпе и наличии параметра clid
        https://st.yandex-team.ru/MARKETOUT-18367
        """
        cls.index.offers += [
            Offer(title='Empty serp rerequest 1', ts=1001),
            Offer(title='Empty serp rerequest 2', ts=1002),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1001).respond(0.55)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1002).respond(0.2)

    def test_rerequest_if_empty_serp(self):
        """Проверка перезапросов под флагами market_rerequest_soft_relevance_threshold и market_rerequest_no_relevance_threshold
        при пустом серпе и наличии параметра clid
        https://st.yandex-team.ru/MARKETOUT-18367
        https://st.yandex-team.ru/MARKETOUT-18814
        https://st.yandex-team.ru/MARKETOUT-18864
        """
        request = 'place=prime&text=empty+serp+rerequest'
        rearr = '&rearr-factors=market_relevance_formula_threshold=0.6'
        empty_response = {'search': {'total': 0, 'results': EmptyList()}}

        # При запросе с порогом 0.6 офферы не проходят по порогу
        # Получаем пустой SERP
        response = self.report.request_json(request + rearr)
        self.assertFragmentIn(response, empty_response)

        # С флагом market_rerequest_soft_relevance_threshold=0.2, но без clid перезапрос не происходит
        # Получаем пустой SERP
        response = self.report.request_json(request + rearr + ';market_rerequest_soft_relevance_threshold=0.2')
        self.assertFragmentIn(response, empty_response)

        # С clid но без флага market_rerequest_soft_relevance_threshold=0.2 перезапрос не происходит
        # Получаем пустой SERP
        response = self.report.request_json(request + '&clid=545' + rearr)
        self.assertFragmentIn(response, empty_response)

        # С флагом market_rerequest_soft_relevance_threshold=0.2, и clid происходит перезапрос
        # Находятся 2 оффера
        response = self.report.request_json(
            request + '&clid=545' + rearr + ';market_rerequest_soft_relevance_threshold=0.2'
        )
        self.assertFragmentIn(response, {'search': {'results': ElementCount(2)}})

        # С флагом market_rerequest_soft_relevance_threshold=0.3, и clid происходит перезапрос
        # Находится 1 оффер
        response = self.report.request_json(
            request + '&clid=545' + rearr + ';market_rerequest_soft_relevance_threshold=0.3'
        )
        self.assertFragmentIn(response, {'search': {'results': ElementCount(1)}})

        # С флагом market_rerequest_no_relevance_threshold, но без clid перезапрос не происходит
        # Получаем пустой SERP
        response = self.report.request_json(request + rearr + ';market_rerequest_no_relevance_threshold=1')
        self.assertFragmentIn(response, empty_response)

        # С флагом market_rerequest_no_relevance_threshold, и clid происходит перезапрос без порога
        # Находятся 2 оффера
        response = self.report.request_json(request + '&clid=545' + ';market_rerequest_no_relevance_threshold=1')
        self.assertFragmentIn(response, {'search': {'results': ElementCount(2)}})

    @classmethod
    def prepare_unknown_redirect_type_for_non_dummy_redirects(cls):
        cls.suggester.on_request(part='unknown redirect type').respond(
            suggestions=[
                Suggestion(
                    part='unknown redirect type',
                    url='/unknown_type?hid=521&suggest=1&suggest_text=unknown+redirect+type',
                )
            ]
        )

    def test_unknown_redirect_target(self):
        """Нераспаршенные урлы могут отдаваться только как сырые урлы
        С параметром non-dummy-redirect=1 отдаем поиск по таким редиректам
        """
        response = self.report.request_json('place=prime&cvredirect=1&text=unknown redirect type')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of("/unknown_type?hid=521&suggest=1&suggest_text=unknown+redirect+type&was_redir=1")
                }
            },
        )

        response = self.report.request_json('place=prime&cvredirect=1&text=unknown redirect type&non-dummy-redirects=1')
        self.assertFragmentNotIn(response, {"redirect": NotEmpty()})

        self.error_log.not_expect(code=3713)

    @classmethod
    def prepare_rerequest_if_empty_serp_on_blue_market(cls):
        """Подготовка данных для проверки перезапросов под флагами market_rerequest_soft_relevance_threshold и market_rerequest_no_relevance_threshold
        при пустом серпе и наличии параметра clid на Синем Маркете.
        https://st.yandex-team.ru/MARKETOUT-18972
        """

        cls.index.models += [
            Model(hyperid=101777, hid=1777, title='skreblo 1'),
            Model(hyperid=102777, hid=1777, title='skreblo 2'),
        ]
        cls.index.shops += [Shop(fesh=1777, fulfillment_virtual=True, virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE)]
        sku_offer_1 = BlueOffer(ts=2001)
        sku_offer_2 = BlueOffer(ts=2002)
        cls.index.mskus += [
            MarketSku(sku='{}'.format(1000101), title="skreblo 1", hyperid=101777, blue_offers=[sku_offer_1]),
            MarketSku(sku='{}'.format(1000102), title="skreblo 2", hyperid=102777, blue_offers=[sku_offer_2]),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2001).respond(0.55)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2002).respond(0.2)

    def test_rerequest_if_empty_serp_on_blue_market(self):
        """Проверка перезапросов под флагами market_rerequest_soft_relevance_threshold и market_rerequest_no_relevance_threshold
        при пустом серпе и наличии параметра clid на Синем Маркете.
        https://st.yandex-team.ru/MARKETOUT-18972
        """

        request = 'place=prime&text=skreblo&rgb=BLUE'
        rearr = '&rearr-factors=market_relevance_formula_threshold=0.6;'
        empty_response = {'search': {'total': 0, 'results': EmptyList()}}

        # При запросе с порогом 0.6 офферы не проходят по порогу
        # Без clid перезапрос с дефолтным порогом 0.53 не происходит
        # Получаем пустой SERP
        response = self.report.request_json(request + rearr)
        self.assertFragmentIn(response, empty_response)

        # С флагом market_rerequest_soft_relevance_threshold=0.1, но без clid перезапрос не происходит
        # Получаем пустой SERP
        response = self.report.request_json(request + rearr + ';market_rerequest_soft_relevance_threshold=0.1')
        self.assertFragmentIn(response, empty_response)

        # С clid но без флага market_rerequest_soft_relevance_threshold=0.1 перезапрос не происходит
        # Получаем пустой SERP
        response = self.report.request_json(request + '&clid=545' + rearr)
        self.assertFragmentIn(response, empty_response)

        # С флагом market_rerequest_soft_relevance_threshold=0.1, и clid происходит перезапрос
        # Находятся 2 оффера
        response = self.report.request_json(
            request + '&clid=545' + rearr + ';market_rerequest_soft_relevance_threshold=0.1'
        )
        self.assertFragmentIn(response, {'search': {'results': ElementCount(2)}})

        # С флагом market_rerequest_soft_relevance_threshold=0.4, и clid происходит перезапрос
        # Находится 1 оффер
        response = self.report.request_json(
            request + '&clid=545' + rearr + ';market_rerequest_soft_relevance_threshold=0.4'
        )
        self.assertFragmentIn(response, {'search': {'results': ElementCount(1)}})

        # С флагом market_rerequest_no_relevance_threshold, но без clid перезапрос не происходит
        # Получаем пустой SERP
        response = self.report.request_json(request + rearr + ';market_rerequest_no_relevance_threshold=1')
        self.assertFragmentIn(response, empty_response)

        # С флагом market_rerequest_no_relevance_threshold, и clid происходит перезапрос без порога
        # Находятся 2 оффера
        response = self.report.request_json(
            request + '&clid=545' + rearr + ';market_rerequest_no_relevance_threshold=1'
        )
        self.assertFragmentIn(response, {'search': {'results': ElementCount(2)}})

    @classmethod
    def prepare_blue_category_redirect(cls):
        cls.index.models += [
            Model(hyperid=7000, hid=7000, title='blue_nid_model'),
            Model(hyperid=7001, hid=7001, title='non_blue_nid_model'),
            Model(hyperid=7002, hid=7000, title='blue_nid_model2'),
            Model(hyperid=7003, hid=7001, title='non_blue_nid_model2'),
            Model(hyperid=7004, hid=7002, title='blue_nid_model3'),
        ]

        cls.index.mskus += [
            MarketSku(sku='7100', title="realBlueNid", hyperid=7000, blue_offers=[BlueOffer()]),
            MarketSku(sku='7102', title="realBlueNid2", hyperid=7002, blue_offers=[BlueOffer()]),
            MarketSku(sku='7101', title="nonBlueNid", hyperid=7001, blue_offers=[BlueOffer()]),
            MarketSku(sku='7103', title="nonBlueNid", hyperid=7003, blue_offers=[BlueOffer()]),
            MarketSku(sku='7104', title="anotherRealBlueNid3", hyperid=7004, blue_offers=[BlueOffer()]),
        ]
        cls.index.hypertree += [
            HyperCategory(
                hid=7000,
                name='A',
                children=[
                    HyperCategory(hid=7001, name='AA'),
                ],
            ),
            HyperCategory(hid=7004, name='B'),
        ]

        cls.index.navtree += [
            NavCategory(nid=7000, hid=7000, is_blue=True),
            NavCategory(nid=7001, hid=7001, is_blue=False),
            NavCategory(nid=7002, hid=7004, is_blue=True),
        ]

    def test_blue_category_redirect_with_non_blue_nid(self):
        """
        Что проверяем: категорийный редирект на синем маркете может осуществляться в белый нид
        Проверяем также, что на синем отключена логика редиректа в единственную категорию
        """
        response = self.report.request_json(
            'place=prime&text=realBlueNid&cvredirect=1&rgb=blue&rearr-factors=market_category_redirect_treshold=-2'
        )

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["9"],
                        "hid": ["7000"],
                        "nid": ["7000"],
                        "text": ["realBlueNid"],
                    },
                }
            },
            preserve_order=True,
        )

        """
        Логика редиректа в единственную категорию работает также как на белом
        """
        response = self.report.request_json(
            'place=prime&text=anotherRealBlueNid3&cvredirect=1&rgb=blue&rearr-factors=market_category_redirect_treshold=-1&debug=da'
        )

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {"hid": ["7002"], "rt": ["9"], "text": ["anotherRealBlueNid3"]},
                    "target": "search",
                }
            },
        )

        """
        Редирект в белое дерево даже если указано rgb=blue
        """
        for query in ['place=prime&text=nonBlueNid&cvredirect=1&rgb=blue', 'place=prime&text=nonBlueNid&cvredirect=1']:

            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "redirect": {
                        "target": "search",
                        "params": {
                            "was_redir": ["1"],
                            "rt": ["9"],
                            "hid": ["7001"],
                            "nid": ["7001"],
                            "text": ["nonBlueNid"],
                        },
                    }
                },
                preserve_order=True,
            )

    @classmethod
    def prepare_shop_reviews(cls):
        cls.index.shops += [Shop(fesh=1779, name='ozon.ru')]

        cls.suggester.on_request(part='ozon.ru отзывы').respond(
            suggestions=[Suggestion(part='ozon.ru отзывы', url='/shop--ozon-ru/155/reviews?suggest=1')]
        )
        qtree = 'cHic44rl4uFgEGCQ4FBg0GAyYBBiz6_Kz9MrKpViVFBi0GIwYrDi4WAHyjMA5RkMGBwYPBgCGCIYEhgy5ky7t5ZpAiPDLEaGRYwwbZsYGfYyMgDBCUaGC4wpBgwWjE72HIwCDFIwFUoMGowGjAETWMCGMBbJc8FkJEAyDBYMQQxJbEANDBrCGRwVDFUMDYwMXYzyXCwgdwqJczBKCXMwiiamPbOTZVRXtVFg0AXZh66CQ1RwHbu97J5cA1u4Ch6oChYOviIGoMsBv9sqwQ,,'  # noqa
        cls.reqwizard.on_request("ozon.ru отзывы").respond(qtree=qtree)

    def test_shop_reviews_redirect(self):
        response = self.report.request_json('place=prime&cvredirect=1&text=ozon.ru+отзывы')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of(
                        "/shop--ozon-ru/155/reviews?suggest=1&was_redir=1&show-reviews=1&rt=12&suggest_text=ozon.ru отзывы"
                    ),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_franchise_redirect(cls):
        cls.suggester.on_request(part='амням').respond(
            suggestions=[
                Suggestion(
                    part='амням', url='/franchise--am-niam/14022575?suggest_text=амням&suggest=1&suggest_type=franchise'
                )
            ]
        )

    def test_franchise_redirect(self):
        """MARKETOUT-25365 Проверяем, что при парсинге урлов редиректов проставлен валидный таргет"""
        response = self.report.request_json('place=prime&text=амням&cvredirect=1&non-dummy-redirects=1')
        self.assertFragmentIn(
            response, {"redirect": {"target": "franchise", "params": {"rt": ["12"]}}}, allow_different_len=False
        )

    @classmethod
    def prepare_shop_redirect(cls):
        cls.suggester.on_request(part='ситилинк').respond(
            suggestions=[
                Suggestion(
                    part='ситилинк', url='/shop--sitilink/17436?suggest_text=ситилинк&suggest=1&suggest_type=shop'
                )
            ]
        )

    def test_shop_redirect(self):
        """MARKETOUT-25365 Проверяем, что при парсинге урлов редиректов проставлен валидный таргет"""
        response = self.report.request_json('place=prime&text=ситилинк&cvredirect=1&non-dummy-redirects=1')
        self.assertFragmentIn(
            response, {"redirect": {"target": "shop", "params": {"rt": ["12"]}}}, allow_different_len=False
        )

    @classmethod
    def prepare_vendor_suggest_redirect(cls):
        cls.suggester.on_request(part='dyson').respond(
            suggestions=[
                Suggestion(part='dyson', url='/brands--dyson/206928?suggest_text=dyson&suggest=1&suggest_type=vendor')
            ]
        )

    def test_vendor_suggest_redirect(self):
        """MARKETOUT-33907 Проверяем, что при выставленном флаге market_enable_vendor_suggest_redirect редирект по брендовому саджесту включен"""
        response = self.report.request_json(
            'place=prime&text=dyson&cvredirect=1&non-dummy-redirects=1&rearr-factors=market_enable_vendor_suggest_redirect=1'
        )
        self.assertFragmentIn(response, {"redirect": {"params": {"suggest_type": ["vendor"]}, "target": "vendor"}})

        """Проверяем, что без флага market_enable_vendor_suggest_redirect редирект по брендовому саджесту выключен"""
        response = self.report.request_json('place=prime&text=dyson&cvredirect=1&non-dummy-redirects=1')
        self.assertFragmentNotIn(response, {"redirect": {}})

    @classmethod
    def prepare_new_suggest_redirect(cls):
        cls.index.hypertree += [HyperCategory(hid=91491, name="smartphone", visual=True)]

        cls.suggester.on_custom_url_request(part="smartphone", location="suggest-market-new").respond(
            suggestions=[Suggestion(part="smartphone", url="/catalog/54726/list?hid=91491", type="ecom-category")]
        )

    def test_new_suggest_redirect(self):
        """https://st.yandex-team.ru/SUGGEST-3045"""

        response = self.report.request_json(
            'place=prime&text=smartphone&cvredirect=1&non-dummy-redirects=1&rearr-factors=market_use_new_suggest_for_redirect=0'
        )
        self.assertFragmentNotIn(response, {"redirect": {}})

        response = self.report.request_json(
            'place=prime&text=smartphone&cvredirect=1&non-dummy-redirects=1&rearr-factors=market_use_new_suggest_for_redirect=1'
        )
        self.assertFragmentIn(response, {"redirect": {}})

    @classmethod
    def prepare_category_redirect_to_department(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=333125,
                uniq_name="Enterprise for children",
                children=[
                    HyperCategory(
                        hid=433125,
                        children=[
                            HyperCategory(hid=533125, uniq_name="Enterprise for girls"),
                            HyperCategory(hid=533126, uniq_name="Enterprise for boys"),
                        ],
                    ),
                ],
            ),
            HyperCategory(hid=333126, uniq_name="Enterprise for women"),
            HyperCategory(hid=333128, uniq_name="Enterprise for men"),
        ]
        cls.index.mskus += [
            MarketSku(sku=15, hyperid=2333, title="enterprise 1", hid=533125, blue_offers=[BlueOffer()]),
            MarketSku(sku=16, hyperid=2334, title="enterprise 2", hid=533126, blue_offers=[BlueOffer()]),
            MarketSku(sku=18, hyperid=2336, title="enterprise 4", hid=333128, blue_offers=[BlueOffer()]),
        ]
        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 533125).respond(0.9)
        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 533126).respond(0.8)
        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 333128).respond(0.2)

        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 533125).respond(0.2)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 533126).respond(0.1)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 333128).respond(0.8)

        cls.matrixnet.on_place(MnPlace.SEQUENTIAL_CATEGORY_REDIRECT, 533125).respond(0.2)
        cls.matrixnet.on_place(MnPlace.SEQUENTIAL_CATEGORY_REDIRECT, 533126).respond(0.65)
        cls.matrixnet.on_place(MnPlace.SEQUENTIAL_CATEGORY_REDIRECT, 333128).respond(0.8)

    def test_category_redirect_to_department(self):

        request = 'place=prime&text=enterprise&cvredirect=1&debug=da'
        # нету редиректа
        response = self.report.request_json(
            request + '&rearr-factors=market_category_redirect_treshold=0.95;market_use_sequential_category_redirect=0'
        )
        self.assertFragmentIn(
            response, {'logicTrace': [Contains('Redirect formula full_mode_f', 'score:0.8999', 'threshold:0.95')]}
        )
        self.assertFragmentNotIn(response, {"redirect": {}})

        # редирект в департамент
        response = self.report.request_json(
            request
            + '&rearr-factors=market_category_redirect_treshold=0.95;market_category_redirect_to_department_treshold=0.1;market_use_sequential_category_redirect=0'
        )
        self.assertFragmentIn(
            response, {'logicTrace': [Contains('Redirect formula full_mode_f', 'score:0.8999', 'threshold:0.95')]}
        )

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "hid": ["333125"],
                    }
                }
            },
        )

        # проверяем логгирование
        response = self.report.request_json(
            request
            + '&rearr-factors=market_category_redirect_treshold=0.95;market_category_redirect_to_department_treshold=0.1;'
            + 'market_use_sequential_category_redirect=0;market_write_category_redirect_features=20&debug=da'
        )

        self.assertFragmentIn(
            response,
            "position=1\\thid=533125\\tcategory_name=Enterprise for girls\\tdepartment_hid=333125\\tdepartment_name=Enterprise for children",
        )

    def test_sequential_redirect_to_department(self):

        request = 'place=prime&text=enterprise&cvredirect=1'
        # нету редиректа
        response = self.report.request_json(
            request + '&rearr-factors=market_category_redirect_treshold=0.95;market_use_sequential_category_redirect=0'
        )
        self.assertFragmentNotIn(response, {"redirect": {}})

        # последовательный редирект
        response = self.report.request_json(request + '&rearr-factors=market_category_redirect_treshold=0.95&debug=1')

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "hid": ["333128"],
                    }
                },
            },
        )
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains("CheckCategoryRedirection", "Sequential redirect score: 0.8", "threshold: 0.6286789775")
                ]
            },
        )

    def test_full_name_match_for_department_redirects(self):
        """Если происходит полное совпадение имени категории и запроса то делаем редирект на бестекст"""

        # редирект в листовую категорию, название совпадает
        response = self.report.request_json('place=prime&text=enterprise+for+girls&cvredirect=1&debug=da')
        self.assertFragmentIn(response, "Request is full matched to category hid=533125 Enterprise for girls;")
        self.assertFragmentIn(
            response,
            {
                "redirect": {"params": {"text": NoKey("text"), "hid": ["533125"]}, "target": "search"},
            },
        )

        # редирект в листовую категорию, название не совпадает
        response = self.report.request_json('place=prime&text=enterprise+for+little+girls&cvredirect=1&debug=da')
        self.assertFragmentIn(response, "Request is not full matched to category hid=533125 Enterprise for girls;")
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {"text": ["enterprise for little girls"], "hid": ["533125"]},
                    "target": "search",
                },
            },
        )

        # редирект в департамент, название совпдает
        response = self.report.request_json(
            'place=prime&text=enterprise+for+children&cvredirect=1&debug=da'
            '&rearr-factors=market_category_redirect_treshold=0.95;market_category_redirect_to_department_treshold=0.1;market_use_sequential_category_redirect=0'
        )
        self.assertFragmentIn(response, "Request is full matched to category hid=333125 Enterprise for children;")
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {"hid": ["333125"], "text": NoKey("text")},
                    "target": "catalog",  # нелистовая категория - попадаем на каталожную выдачу
                }
            },
        )

        # редирект в департамент, название не совпдает
        response = self.report.request_json(
            'place=prime&text=enterprise+for+little+children&cvredirect=1&debug=da'
            '&rearr-factors=market_category_redirect_treshold=0.95;market_category_redirect_to_department_treshold=0.1;market_use_sequential_category_redirect=0'
        )
        self.assertFragmentIn(response, "Request is not full matched to category hid=333125 Enterprise for children;")
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {"hid": ["333125"], "text": ["enterprise for little children"]},
                    "target": "search",
                }
            },
        )

    def test_category_redirect_top_categories(self):
        """
        Проверям, что при категорийном редиректе в rs добавляется топ-1 категория
        https://st.yandex-team.ru/MARKETOUT-44677
        """

        def parse_rs_categories(rs):
            result = []
            rs = urllib.unquote(rs)
            common_report_state = ReportState.parse(rs)
            for category in common_report_state.search_state.top_categories:
                if category.hid:
                    result.append(category.hid)
            return result

        request = 'place=prime&text=enterprise&cvredirect=1'

        # считаем top_categories
        response = self.report.request_json(
            request + '&rearr-factors=market_category_redirect_treshold=0.5;market_use_sequential_category_redirect=0'
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "rs": [NotEmpty()],
                    }
                }
            },
        )
        rs = response.root['redirect']['params']['rs'][0]
        categories = parse_rs_categories(rs)
        self.assertEquals(categories[0], 333128)

    @classmethod
    def prepare_category_redirect_rise(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=444125,
                children=[
                    HyperCategory(
                        hid=444126,
                        children=[
                            HyperCategory(hid=444127),
                            HyperCategory(hid=444128),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(title='sky 1', hid=444127),
            Offer(title='sky 2', hid=444128),
            Offer(title='sky 3', hid=444126),
            Offer(title='sky 4', hid=444126),
        ]

        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 444127).respond(0.9)
        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 444128).respond(0.8)
        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 444126).respond(0.7)

    def test_category_redirect_rise(self):

        '''
        MARKETOUT-45835
        Проверяем подъем в редиректах
        '''

        request = 'place=prime&text=sky&cvredirect=1&debug=da'

        # редирект в hid 444127
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "hid": ["444127"],
                    }
                }
            },
        )

        # редиректа в hid 444127 нету, так как в нем найден 1 оффер, редирект в hid 444126, так как в нем найдено 4 оффера
        response = self.report.request_json(request + '&rearr-factors=market_redirect_rise_threshold=0.5')

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "hid": ["444126"],
                    }
                }
            },
        )


if __name__ == '__main__':
    main()
