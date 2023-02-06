#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import MnPlace
import urllib

from core.types import (
    CardCategoryVendor,
    CardNavCategory,
    CardVendor,
    FormalizedParam,
    GLParam,
    GLType,
    HyperCategory,
    HyperCategoryType,
    Model,
    NavCategory,
    Offer,
    QueryEntityCtr,
    RedirectWhiteListRecord,
    ReqwExtMarkupMarketShop,
    ReqwExtMarkupMarketTiresKeyword,
    ReqwExtMarkupMarketTiresMark,
    ReqwExtMarkupMarketTiresModel,
    ReqwExtMarkupToken,
    ReqwExtMarkupTokenChar,
    Shop,
    Suggestion,
    VCluster,
    Vendor,
)
from core.testcase import TestCase, main
from core.types.card import CardCategory
from core.types.vendor import PublishedVendor
from core.matcher import Contains, LikeUrl, NoKey, NotEmpty
from core.types.hypercategory import ADULT_CATEG_ID
from core.types.autogen import Const

from unittest import skip


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        """
        vendor id = {1;20}
        nid = {200;299}
        hid = {300;599}
        hyperid = {400;499}
        """

        cls.settings.default_search_experiment_flags += [
            'market_categ_dssm_factor_fast_calc=0',
            'market_skip_broken_category_factors=0',  # TODO: MSSUP-763: последние два параметра включены в проде, но меняют результаты редиректов в тестах
        ]

        cls.index.cards += [CardNavCategory(nid=213), CardVendor(vendor_id=11), CardVendor(vendor_id=10)]

        cls.index.navtree += [NavCategory(nid=213, name='good', short_name='marvellous', uniq_name='awesome')]

        cls.index.vendors += [
            Vendor(vendor_id=9, aliases=['samsung'], name='samsung'),
            Vendor(vendor_id=10, aliases=['Somsung', 'Samsung'], name='Samsung'),
            Vendor(vendor_id=11, aliases=['samsung toshiba', 'toshiba samsung'], name='toshiba samsung'),
        ]

        cls.index.offers += [Offer(title='samsung')]

        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='gift for a girlfriend', url='/collections/v-podarok-ljubimomu'),
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
        ]

        # test_visual_brand_redirects data
        cls.index.offers += [
            # offers with vendor that has vendor card
            Offer(title='adidas 1', visual=True, vendor_id=20),
            Offer(title='adidas 2', visual=True, vendor_id=20),
            # offers with category and vendor that has vendor card
            Offer(title='nike 1', visual=True, vendor_id=21, hid=20),
            Offer(title='nike 2', visual=True, vendor_id=21, hid=20),
            # offers with category and vendor that has category-vendor card
            Offer(title='puma 1', visual=True, vendor_id=22, hid=21),
            Offer(title='puma 2', visual=True, vendor_id=22, hid=21),
        ]

        cls.index.vendors += [
            Vendor(vendor_id=20, name='adidas'),
            Vendor(vendor_id=21, name='nike'),
            Vendor(vendor_id=22, name='puma'),
        ]

        cls.index.cards += [
            CardVendor(vendor_id=20, visual=True),
            CardVendor(vendor_id=21, visual=True),
            CardCategoryVendor(vendor_id=22, hid=21),
        ]

        # test_visual_category_redirects data
        cls.index.offers += [
            Offer(title='clothes1', visual=True, hid=30),
            Offer(title='clothes2', visual=True, hid=30),
        ]

        cls.index.hypertree += [HyperCategory(hid=30, name="clothes", visual=True)]

        cls.index.cards += [CardCategory(hid=30)]

        # test_visual_redirect_bad_vcluster* data
        cls.index.hypertree += [
            HyperCategory(hid=111, visual=True),
            HyperCategory(hid=222, visual=True),
            HyperCategory(hid=333, visual=False),
        ]

        cls.index.offers += [
            Offer(title='MARKETOUT-7351-1-1', hid=111, vclusterid=1000000001),
            Offer(title='MARKETOUT-7351-1-2', hid=111, vclusterid=1000000002),
            Offer(title='MARKETOUT-7351-1-3', hid=111),
            Offer(title='MARKETOUT-7351-1-4', hid=111),
            Offer(title='MARKETOUT-7351-2', hid=222),
            Offer(title='MARKETOUT-7351-3-1', hid=333),
            Offer(title='MARKETOUT-7351-3-2', hid=333),
        ]

        cls.index.vclusters = [
            VCluster(hid=111, vclusterid=1000000001),
            VCluster(hid=111, vclusterid=1000000002),
        ]

        # test_non_visual_category_redirects
        cls.index.offers += [
            Offer(title='bracelet 1', visual=False, hid=32),
            Offer(title='bracelet 2', visual=False, hid=32),
        ]

        cls.index.hypertree += [HyperCategory(hid=32, name="bracelets", visual=False)]

        # a non-visual offer for test_redirect_from_visual_to_main
        cls.index.offers += [
            Offer(title="kiyanka", visual=False),
        ]

        # visual offers for test_redirect_from_main_to_visual
        cls.index.offers += [
            Offer(title="wardrobe shoes Adidas 1234", visual=True),
            Offer(title="wardrobe t-shirts Nike 5678", visual=True),
        ]

        # create card, hyper and navigation nodes for test_redirect_by_category_card
        cls.index.hypertree += [
            HyperCategory(hid=310, name="mobile phones", visual=False, output_type=HyperCategoryType.GURU)
        ]

        cls.index.navtree += [NavCategory(nid=256, hid=310)]

        cls.index.cards += [CardCategory(hid=310)]

        # create a category and a model in it for test_redirect_to_model
        cls.index.hypertree += [HyperCategory(hid=311, name='iphones')]
        cls.index.models += [Model(hyperid=400, title='Apple iphone 6s 64GB', hid=311)]

        # create 3 models with the same group model...
        cls.index.models += [
            Model(hyperid=401, group_hyperid=499, title='Nokia lumia 720 white'),
            Model(hyperid=402, group_hyperid=499, title='Nokia lumia 720 black'),
        ]

        # ...and an offer for test_redirect_to_groupmodel
        cls.index.offers += [Offer(title='Nokia lumia 720 bw', hyperid=499)]

        # guru category and vendor and cv-card for test_redirect_to_guru_category_vendor
        cls.index.hypertree += [
            HyperCategory(hid=312, name="suchkorezy", visual=False, output_type=HyperCategoryType.GURU)
        ]

        cls.index.vendors += [
            Vendor(vendor_id=30, name='apple'),
        ]

        cls.index.cards += [CardCategoryVendor(vendor_id=30, hid=312)]

        # a category, a cluster and an offer for test_redirect_to_visual_cluster
        cls.index.hypertree += [HyperCategory(hid=444, visual=True)]

        cls.index.vclusters += [
            VCluster(hid=444, vclusterid=1000000003),
        ]

        cls.index.offers += [
            Offer(title="green cardigan", hid=444, vclusterid=1000000003),
        ]

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

        # ...and matching offers in all subcategories for test_redirect_to_guru_catalog
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

        # ...and matching offers in both categories for test_redirect_to_non_guru_catalog
        cls.index.offers += [
            Offer(title='food pedigree', hid=566),
            Offer(title='food wiskas', hid=567),
        ]

        # CTR very strong feature for category redirects, so we can set it for desired output.
        cls.index.ctr.msearch.categ += [
            QueryEntityCtr('food', 565, 900, 1000),
            QueryEntityCtr('pyrotechnics', 555, 900, 1000),
            QueryEntityCtr('level', 86009, 900, 1000),
        ]

        # guru-category...
        cls.index.hypertree += [
            HyperCategory(
                hid=503, visual=False, output_type=HyperCategoryType.GURU, name='engine oil', uniq_name='engine oil'
            ),
        ]

        # ...and an offer for test_redirect_to_leaf_guru_category_with_text
        cls.index.offers += [Offer(title='engine oil for diesel', hid=503)]

        # test_redirests_to_suggest data
        # catalogmodels
        cls.suggester.on_request(part='telephones').respond(
            suggestions=[
                Suggestion(
                    part='beautiful telephones',
                    url='/catalog/91491/list?gfilter=1801946:1871375&suggest=1',
                    type='ecom-category',
                ),
                Suggestion(
                    part='telephones',
                    url='/catalogmodels.xml?hid=520&CAT_ID=5201&suggest=1&suggest_text=telephones',
                    type='ecom-category',
                ),
                Suggestion(
                    part='telephones for BABUSHKA', url='/catalog/54613/list?suggest=1&hid=91303', type='ecom-category'
                ),
            ]
        )

        cls.suggester.on_request(part='днс').respond(
            suggestions=[
                Suggestion(part='днс', url='/shop--dns/3118001?was_redir=1&rt=19'),
            ]
        )

        cls.index.hypertree += [
            HyperCategory(hid=520, output_type=HyperCategoryType.GURU, visual=False),
        ]
        cls.index.navtree += [
            NavCategory(nid=1520, hid=520),
        ]

        # catalog with explicit guru hid
        cls.suggester.on_request(part='cameras').respond(
            suggestions=[
                Suggestion(
                    part='videocameras',
                    url='/catalogmodels.xml?hid=90635&CAT_ID=105092&suggest_text=videocameras',
                    type='ecom-category',
                ),
                Suggestion(part='cameras', url='/catalog/1521?hid=521&suggest_text=cameras', type='ecom-category'),
            ]
        )

        cls.index.hypertree += [
            HyperCategory(hid=521, output_type=HyperCategoryType.GURU, visual=False),
        ]
        cls.index.navtree += [
            NavCategory(nid=1521, hid=521),
        ]

        # catalog with explicit gurulight hid
        cls.suggester.on_request(part='jeans').respond(
            suggestions=[Suggestion(part='jeans', url='/catalog/1522?hid=522&suggest_text=jeans')]
        )

        cls.suggester.on_request(part='boots').respond(
            suggestions=[Suggestion(part='boots', url='/catalog/522?suggest=1&suggest_text=boots')]
        )

        cls.index.hypertree += [
            HyperCategory(hid=522, output_type=HyperCategoryType.GURULIGHT, visual=True),
        ]
        cls.index.navtree += [
            NavCategory(nid=1522, hid=522),
        ]

        # catalog with implicit hid
        cls.suggester.on_request(part='monitors').respond(
            suggestions=[Suggestion(part='monitors', url='/catalog/523/list?how=dpop&suggest_text=monitors')]
        )

        cls.index.hypertree += [HyperCategory(hid=523, output_type=HyperCategoryType.GURU, visual=False)]
        cls.index.navtree += [
            NavCategory(nid=1523, hid=523),
        ]

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
        cls.index.navtree += [
            NavCategory(nid=1524, hid=524),
        ]

        # search
        cls.suggester.on_request(part='male trunks').respond(
            suggestions=[
                Suggestion(
                    part='male trunks', url='/search?hid=525&suggest=1&suggest_text=male+trunks', type='ecom-category'
                )
            ]
        )

        cls.index.hypertree += [HyperCategory(hid=525, output_type=HyperCategoryType.GURULIGHT, visual=True)]
        cls.index.navtree += [
            NavCategory(nid=1525, hid=525),
        ]

        # visual brand
        cls.suggester.on_request(part='baon').respond(
            suggestions=[Suggestion(part='baon', url='/brands/111?suggest=1&suggest_text=baon')]
        )

        # non-visual brand
        cls.suggester.on_request(part='bosch').respond(
            suggestions=[Suggestion(part='bosch', url='/brands.xml?brand=222&suggest=1&suggest_text=bosch')]
        )

        # model
        cls.suggester.on_request(part='bosch msm 67pe').respond(
            suggestions=[
                Suggestion(part='bosch msm 67pe', url='/product/333?hid=526&suggest=1&suggest_text=bosch+msm+67pe')
            ]
        )

        cls.index.hypertree += [HyperCategory(hid=526, output_type=HyperCategoryType.GURU, visual=False)]
        cls.index.navtree += [
            NavCategory(nid=1526, hid=526),
        ]

        # shops-opinions
        cls.suggester.on_request(part='pyatyorochka reviews').respond(
            suggestions=[
                Suggestion(
                    part='pyatyorochka reviews',
                    url='/shop-opinions.xml?shop_id=123&suggest=1&suggest_text=pyatyorochka',
                )
            ]
        )

        # search suggest
        cls.suggester.on_request(part='search_query').respond(
            suggestions=[
                Suggestion(
                    part='search_query',
                    url='/search.xml?text=search_query&suggest=2&cvredirect=2&suggest_text=search_query',
                )
            ]
        )

        # non-existent
        cls.suggester.on_default_request().respond()

        cls.index.hypertree += [HyperCategory(hid=527, output_type=HyperCategoryType.GURU, visual=False)]

        cls.index.hypertree += [
            HyperCategory(
                hid=86000,
                visual=False,
                name='CatRoot',
                uniq_name='CatRoot',
                children=[
                    HyperCategory(
                        hid=86001,
                        visual=False,
                        name='Cat1',
                        uniq_name='Cat1',
                        children=[
                            HyperCategory(hid=86002, visual=False, name='Cat2', uniq_name='Cat2'),
                            HyperCategory(hid=86003, visual=False, name='Cat3', uniq_name='Cat3'),
                        ],
                    ),
                    HyperCategory(
                        hid=86004,
                        visual=False,
                        name='Cat4',
                        uniq_name='Cat4',
                        children=[
                            HyperCategory(hid=86005, visual=False, name='Cat5', uniq_name='Cat5'),
                            HyperCategory(hid=86006, visual=False, name='Cat6', uniq_name='Cat6'),
                        ],
                    ),
                    HyperCategory(
                        hid=86007,
                        visual=False,
                        name='Cat7',
                        uniq_name='Cat7',
                        children=[
                            HyperCategory(
                                hid=86008,
                                visual=False,
                                name='Cat8',
                                uniq_name='Cat8',
                                children=[HyperCategory(hid=86009, visual=False, name='Cat9', uniq_name='Cat9')],
                            )
                        ],
                    ),
                    HyperCategory(
                        hid=86010,
                        visual=False,
                        name='Cat10',
                        uniq_name='Cat10',
                        children=[
                            HyperCategory(
                                hid=86011,
                                visual=False,
                                name='Cat11',
                                uniq_name='Cat11',
                                children=[HyperCategory(hid=91498, visual=False, name='Cat12', uniq_name='Cat12')],
                            )
                        ],
                    ),
                ],
            )
        ]

        cls.index.offers += [
            Offer(title='CatRoot Cat1 Offer1', hid=86001),
            Offer(title='CatRoot Cat1 Cat2 Offer2', hid=86002),
            Offer(title='Common CatRoot Cat1 Cat3 Offer3', hid=86003),
            Offer(title='CatRoot Cat4 Offer1', hid=86004),
            Offer(title='CatRoot Cat4 Cat5 Offer2', hid=86005),
            Offer(title='Common CatRoot Cat4 Cat6 Offer3', hid=86006),
            Offer(title='Level', hid=86008),
            Offer(title='Level', hid=86009),
            Offer(title='Level', hid=86009),
            Offer(title='Level', hid=86009),
            Offer(title='OfferInCat12', hid=91498),
            Offer(title='OfferInCat12', hid=91498),
            Offer(title='OfferInCat12', hid=91498),
        ]

        # test_orig_text_white_list_redirect data
        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='origText', url='/search.xml?hid=527&text=newText'),
        ]

        # test redirect with 'adult' param
        cls.index.offers += [
            Offer(title='вибратор 1', hid=350),
            Offer(title='вибратор 2', hid=350),
            Offer(title='вибратор 3', hid=ADULT_CATEG_ID, adult=True),
            Offer(title='вибратор 4', hid=ADULT_CATEG_ID, adult=True),
            Offer(title='вибратор 5', hid=ADULT_CATEG_ID, adult=True),
            Offer(title='вибратор 6', hid=ADULT_CATEG_ID, adult=True),
            Offer(title='вибратор 7', hid=ADULT_CATEG_ID, adult=True),
            Offer(title='вибратор 8', hid=ADULT_CATEG_ID, adult=True),
            Offer(title='сорочка 1', hid=351),
            Offer(title='сорочка 2', hid=351),
            Offer(title='сорочка 3', hid=352, vclusterid=1000000101, adult=True),
            Offer(title='сорочка 4', hid=352, vclusterid=1000000101, adult=True),
            Offer(title='сорочка 5', hid=352, vclusterid=1000000101, adult=True),
            Offer(title='сорочка 6', hid=352, vclusterid=1000000101, adult=True),
            Offer(title='сорочка 7', hid=352, vclusterid=1000000101, adult=True),
            Offer(title='сорочка 8', hid=352, vclusterid=1000000101, adult=True),
        ]
        cls.index.hypertree += [
            HyperCategory(hid=350, name="ordinary_category"),
            HyperCategory(hid=351, name="ordinary_category_1", visual=True),
            HyperCategory(hid=ADULT_CATEG_ID, name="adult_category"),
            HyperCategory(hid=352, name="non_ordinary_category", visual=True),
        ]
        cls.index.vclusters += [
            VCluster(hid=352, vclusterid=1000000101, title="сорочка"),
        ]

        cls.index.published_vendors += [PublishedVendor(vendor_id=10)]

        cls.settings.ignore_qtree_decoding_failed_in_error_log = True

    @classmethod
    def prepare_suggest_redirect_to_urls_without_slash(cls):
        cls.suggester.on_request(part='no slash').respond(
            suggestions=[
                Suggestion(
                    part='no slash',
                    url='catalog/54432?hid=111&suggest_text=%D0%BE%D0%B4%D0%B5%D0%B6%D0%B4%D0%B0&suggest=1&suggest_type=category',
                    type='ecom-category',
                )
            ]
        )

        cls.suggester.on_request(part='collection without slash').respond(
            suggestions=[
                Suggestion(part='collection without slash', url='collections/no-slash-collection', type='ecom-category')
            ]
        )

    def test_alice_redirects(self):
        # non leaf category
        response = self.report.request_json('place=prime&cvredirect=1&text=Cat1&alice=1')
        self.assertFragmentNotIn(response, {"redirect": {}})

        # leaf category
        response = self.report.request_json('place=prime&cvredirect=1&text=Level&alice=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "was_redir": ["1"],
                        "hid": ["86009"],
                        "text": ["Level"],
                        "slug": ["cat9"],
                    },
                    "target": "search",
                }
            },
        )

    def test_handmade_formula(self):
        # It works like on basesearch and has 4 components:
        # <1>FULL_MATCH
        # <2>GOOD_FOR_REDIRECT
        # <7>LEVEL_RATIO
        # <10>OFFERS_COUNT_RATIO

        # Category with FULL_MATCH should be the first
        # id="catalog" if it has children else id="search"
        response = self.report.request_json('place=prime&cvredirect=1&text=Cat1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "was_redir": ["1"],
                        "hid": ["86001"],
                        # request is in suggest_text-field instead of text-field due to https://st.yandex-team.ru/MARKETOUT-16594
                        "suggest_text": ["Cat1"],
                    },
                    "target": "catalog",
                }
            },
        )

        # Choose category by formula.
        response = self.report.request_json('place=prime&cvredirect=1&text=Level')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "was_redir": ["1"],
                        "hid": ["86009"],
                        "text": ["Level"],
                        "slug": ["cat9"],
                    },
                    "target": "search",
                }
            },
        )

        # Choose category by formula.
        response = self.report.request_json('place=prime&cvredirect=1&text=OfferInCat12')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "was_redir": ["1"],
                        "hid": ["91498"],
                        "text": ["OfferInCat12"],
                        "slug": ["cat12"],
                    },
                    "target": "search",
                }
            },
        )

    def test_redirect_to_search_suggest(self):
        response = self.report.request_json('place=prime&text=search_query&cvredirect=1')
        self.assertFragmentNotIn(response, {"redirect": {}})

    def test_suggest_redirect_to_urls_without_slash(self):
        '''
        Для редиректов по саджесту вычисляется корректный id, даже если в урле нет слеша в начале.
        Т.ж. проверяем, что для коллекций и статей в этом случае корректно попарсится id.
        https://st.yandex-team.ru/MARKETOUT-10162
        '''
        response = self.report.request_json('place=prime&text=no+slash&cvredirect=1')
        self.assertFragmentIn(response, {"redirect": {"url": Contains("catalog")}})

        response = self.report.request_json('place=prime&text=collection+without+slash&cvredirect=1')
        self.assertFragmentIn(response, {"redirect": {"url": Contains("collection")}})

    @classmethod
    def prepare_suggest_params_in_white_list_redirect(cls):
        """Подготовка для проверки прокидывания параметров саджестов при редиректе по белому списку
        https://st.yandex-team.ru/MARKETOUT-13624
        """

        # Создание категории для проверки редиректа в категорию
        cls.index.hypertree += [HyperCategory(hid=90402)]
        # Создание белого списка редиректа
        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='iphone 7', url='/search.xml?text=iphone+7'),
            RedirectWhiteListRecord(query='авто', url='/catalog/54418?hid=90402'),
        ]
        cls.index.offers += [
            Offer(title='test redirect offer', hid=10862),
        ]

    @classmethod
    def prepare_white_list_record_with_placeholder(cls):
        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='билеты поезд', url='/search.xml?text={}'),
            RedirectWhiteListRecord(query='детские наушники', url='/search?text={}&cvredirect=0'),
        ]

        # для приложения запрос переопределен в белом списке
        cls.index.redirect_whitelist_app_records += [
            RedirectWhiteListRecord(
                query='детские наушники', url='/product--naushniki-jbl-jr310/858525?sku=3828274930'
            ),
        ]

        cls.reqwizard.on_request('билеты на поезд').respond(
            qtree='cHicrZE7SwNBFIXPnWRlGCIsCeo6IC7BYhGExUpsFCuxEkEQG0UtUkqqIEiCIvgoFDstfUCq-MCg4LNUq9nKxh_hT3Amm91sgqXbzOXOueee-VbMiAy37C4HLnnMRxYSeQxjFOMZDhu6'
            'Dw8-Jq1paxYLWEaBjginhDPCLeGZoL93gqI1-cHEqgjHuB4zdhl1ox7Uo6oHW8GBJDf27kp4YxrGu_BdZZF321zHHh9jNPVFnGzINl0eHvk0e87CnMU9EqJ17zSWjWEutfv2uciqV4vs8m'
            'pF55DwUitcn0xdx9VTXD00KiZZsBNVqq4rYZNMqxd12-hSPGOU21EvOPByBZSoOCTaojomqgmDZoJcgZewwThVCBqCPGZiqYOlUK96XV3dqzv9k8htkrT-IPnz2BuRTEz9xfEi5JhQdVIs'
            'i8RlJ8SahliLnvD_EA26FpR8Ny_bFXKYm_Is3-QwF_s0KNIGVLaPk8zx_p75w6mJAaAy4WLEPDdUMJs1FU7Pyfpmm0Kb_wIfCsHI'
        )

        cls.reqwizard.on_request('детские наушники').respond(
            qtree='cHic7VddaBxVFD7nzuzm9rqEYWNwGaiuQWQpCIsgBEEiwYfYBwkBQQes7aKSKBZZEaL0YWKQro26YlFBpIKSEFC7a9vAJDaxvpk8zbyKlEbwzTfRN63nzt_evTubXWzpi1lY9t6dc849'
            '9_u-c-4dcVwUeM7Kl6CMFVaFItgwAcfgYXi0wMEC-h8qUIXHczO5WXgGTsI8fojwGcKXCJcQthHo8xOCjy_YPxviJRG5cXKT4Ub9K_5G8Haw5G_5nr9hYzmNPqJEhxmQ0ecv5JLgmqO2VB'
            'Umcfo3xtECW7OcgApWcfYrFiVbf0SoBu2gWUK55CTMGY0f9hy23nLYaqsmLGab0r8yNg-LWH9QaHFLMq50g1qeVgWy44tQ_wU1w800PrqOud7y9hxjteXVCrRA3m_7l_2dilHjNGNBMxn5'
            'XjgCGlECNEIabYb_ydHV9KmX-r6TPFWiyMiC7Ex1FYpo9I8YLKcRPwi9CYZgWY4lDG8RxC4CkWJ_YYqXNXItf5sAXQ4a9OvJzZN2sBzTyzPo_Wc0obfHNYvgNSMkuMdWp_h9yUGXjcLxtQ'
            '7HEW8JlO101B_ebvg8okbQyPR3_EsalCyCMtTOuwfms3tHc3le9MCnJNPac3DdwdU4VHvA8tqi3QqZKHDD4iVWNiq5qlwAnb8YX3JdHIdrbz5h7zPxnCYgEVWOv-FvKm0nl6GcG88mylF8'
            'sjTzJ4aaUax0tUwI5WGfqv6aGAxcMviOvtu0260SdDEoFbXWqlnWiLpUWLpJORcIqHzwnn81nAnLoMJqxgBjDOvdVs5WF9qKlYAx1wnYXbzGvYqS6qHAlPBWTOdHk7uNBPhvTPHigLYMt6'
            '8re8YwXfksDteWkzJR2vPQPTGBKe16WR3usNH301AjpxTvH6aYEcmNgA1zUzjgnrCK4mlNkHmC9vugqd4PjAwlziYxY_teAZrT06H-YoOO7vAkI93dL-IHYT3jJJvDs06H6O4j71cmThzY'
            'sdLD7rBl_eeWZfGbNw3LdU06OcyKEZ0cmhQ_MhUp_m2IVzVWilQGbcqJDjgqz-XADVYUcR7JIGfn3oScDN8skpaitpZhrZPVRDpyNavbfR8ZcPLHdK3Ed9mPUfTk7bczGu7axTgvvMW8ul'
            'tuv7yO9aYVrGTLu6s_MYuRKHaRN1JRXMGeUj1y-tQbr8-fXnhlYYAWPv_k-rcskUPHK0sFM6EIOjYx9-cT7h8QnWfpRuh46OwFFxnthWl7WVIF_rshzmh7KcmS9LcIqZ3u25yytcKgrfUN'
            'krXT_ejlqq-PrvpPUfS1JaWhqn6psosJIrdwAx5YB4Q21s8PmdnuncwqQ8-X1TvbBVMsDPG2BYcvW__Tl61YNTQ7h_eJ8KAs3sPRHuPG-JPeiamjAO5UGR6SLEcWZB9b5McXHjs-dXT_tT'
            'Mdi7EwBivexfncCMei8dTsqXNYiCObfLQO0VSGCaeUxb8RlU4I'
        )

    def test_white_lists_redirect_with_placeholder(self):
        """Проверяем что при нахождениии записи в white-list'е, {} заменяется
        на оригинальный текст запроса
        https://st.yandex-team.ru/MARKETOUT-24478
        """
        response = self.report.request_json('place=prime&text=билеты+на+поезд&cvredirect=1')
        self.assertFragmentIn(
            response, {"redirect": {"url": LikeUrl.of('/search.xml?text=билеты%20на%20поезд&was_redir=1&rt=10')}}
        )

        response = self.report.request_json('place=prime&text=детские%20наушники&cvredirect=1')
        self.assertFragmentIn(
            response, {"redirect": {"url": LikeUrl.of('/search?text=детские%20наушники&cvredirect=0')}}
        )

    def test_no_white_lists_redirect_for_b2b(self):
        """
        Проверяем, что для b2b оферов редиректы по белому списку не работают
        https://st.yandex-team.ru/MARKETOUT-47349
        """
        b2b_flag = '&available-for-business=1'
        response = self.report.request_json('place=prime&text=детские%20наушники&cvredirect=1' + b2b_flag)
        self.assertFragmentNotIn(response, {"redirect": NotEmpty()})

    def test_white_lists_with_placeholder_non_dummy_redirects(self):
        """Проверяем, что при &non-dummy-redirect=1 не происходит повторное
        кодирование текста запроса
        https://st.yandex-team.ru/MARKETOUT-24478
        """
        response = self.report.request_json('place=prime&text=билеты+на+поезд&cvredirect=1&non-dummy-redirects=1')
        self.assertFragmentIn(response, {"redirect": {"params": {"text": ["билеты на поезд"]}}})

    def test_white_list_redirects_for_app(self):
        """Проверяем что если для приложения редирект переопределяется то используется в первую очередь редирект из файла
        redirect-white-list-app.db
        """

        response = self.report.request_json('place=prime&text=детские%20наушники&cvredirect=1')
        self.assertFragmentIn(
            response, {"redirect": {"url": LikeUrl.of('/search?text=детские%20наушники&cvredirect=0')}}
        )

        response = self.report.request_json('place=prime&text=детские%20наушники&cvredirect=1&client=ANDROID')
        self.assertFragmentIn(
            response, {"redirect": {"url": LikeUrl.of('/product--naushniki-jbl-jr310/858525?sku=3828274930')}}
        )

        # по умолчанию для приложения modelid заменяется на makret-sku
        response = self.report.request_json(
            'place=prime&text=детские%20наушники&cvredirect=1&client=ANDROID&non-dummy-redirects=1&rgb=blue'
        )
        self.assertFragmentIn(response, {"redirect": {"params": {"modelid": ["3828274930"], "sku": ["3828274930"]}}})

        # под флагом market_support_market_sku_in_product_redirects
        # остаются оба параметра modelid и makret-sku
        response = self.report.request_json(
            'place=prime&text=детские%20наушники&cvredirect=1&client=ANDROID&non-dummy-redirects=1'
            '&rearr-factors=market_support_market_sku_in_product_redirects=1'
        )
        self.assertFragmentIn(response, {"redirect": {"params": {"modelid": ["858525"], "sku": ["3828274930"]}}})

    def test_suggest_params_in_white_list_redirect(self):
        """Проверка прокидывания параметров саджестов при редиректе
        https://st.yandex-team.ru/MARKETOUT-13624
        для сырых урлов в белом маркете пока не реализовано
        """
        sp = '&suggest=2&suggest_type=type&suggest_reqid=reqid1&suggest_reqid=reqid2'
        request_api = 'place=prime&cvredirect=1&text={0}&non-dummy-redirects=1&suggest_text=text' + sp
        request_front = 'place=prime&cvredirect=1&text={0}&suggest_text=text' + sp

        # Проверка редиректа по белому списку

        # фронт получает сырой урл, suggest_text берется из исходного запроса
        response = self.report.request_json(request_front.format('iphone+7'))
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of("/search.xml?text=iphone+7&was_redir=1&suggest_text=text" + sp),
                    "params": NoKey("params"),
                }
            },
        )

        # апи получает non-dummy редирект
        response = self.report.request_json(request_api.format('iphone+7'))
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "text": ["iphone 7"],
                        "was_redir": ["1"],
                        "suggest": ["2"],
                        "suggest_text": ["text"],
                        "suggest_type": ["type"],
                        "suggest_reqid": ["reqid1", "reqid2"],
                    },
                }
            },
        )

        # Проверка редиректа по белому списку в категорию
        response = self.report.request_json(request_front.format('авто'))
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of("/catalog/54418?hid=90402&was_redir=1&suggest_text=text" + sp),
                    "params": NoKey("params"),
                }
            },
        )

        response = self.report.request_json(request_api.format('авто'))
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "catalog",
                    "params": {
                        "hid": ["90402"],
                        "was_redir": ["1"],
                        "suggest": ["2"],
                        "suggest_text": ["text"],
                        "suggest_type": ["type"],
                        "suggest_reqid": ["reqid1", "reqid2"],
                    },
                }
            },
        )

        # Проверка редиректа не по белому списку
        # фронт и апи будут получать одинаковые редиректы
        response = self.report.request_json(request_front.format('test+redirect+offer'))
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "text": ["test redirect offer"],
                        "hid": ["10862"],
                        "was_redir": ["1"],
                        "suggest": ["2"],
                        "suggest_text": ["text"],
                        "suggest_type": ["type"],
                        "suggest_reqid": ["reqid1", "reqid2"],
                        "slug": ["hid-10862"],
                    },
                }
            },
        )

    @classmethod
    def prepare_catalog_redirect(cls):
        """Создаем записи для редиректа вида /catalog/123
        с &hid= и без.
        """
        cls.index.hypertree += [
            HyperCategory(hid=91282),
        ]
        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='подарок ребенку', url='/catalog/68795?hid=91282'),
            RedirectWhiteListRecord(query='подарок детям', url='/catalog/68795'),
        ]

    def test_catalog_redirect(self):
        """Проверяем, что редирект в без hid и с hid
        отрабатывают корректно.
        """

        # Запрос [подарок ребенку] редиректит по белому списку редиректов на /catalog/68795?hid=91282
        # ответ белого и синего маркета
        for rgb in ['&rgb=green', '&rgb=blue']:
            response = self.report.request_json('place=prime&cvredirect=1&text=подарок+ребенку' + rgb)
            self.assertFragmentIn(
                response,
                {
                    "redirect": {
                        "url": LikeUrl.of("/catalog/68795?hid=91282&was_redir=1&suggest_text=подарок ребенку"),
                        "params": NoKey("params"),
                    }
                },
                allow_different_len=False,
            )

            # ответ API с флагом non-dummy-redirects=1
            response = self.report.request_json(
                'place=prime&cvredirect=1&text=подарок+ребенку&non-dummy-redirects=1' + rgb
            )
            self.assertFragmentIn(
                response,
                {
                    "redirect": {
                        "target": "catalog",
                        "params": {
                            "suggest_text": ["подарок ребенку"],
                            "was_redir": ["1"],
                            "hid": ["91282"],
                            "nid": ["68795"],
                        },
                    }
                },
                allow_different_len=False,
            )

            # запрос [подарок детям] редиректит по белому списку редиректов на /catalog/68795 без указания hid
            # ответ белого и синего маркета
            response = self.report.request_json('place=prime&cvredirect=1&text=подарок+детям' + rgb)
            self.assertFragmentIn(
                response,
                {
                    "redirect": {
                        "url": LikeUrl.of("/catalog/68795?was_redir=1&suggest_text=подарок детям"),
                        "params": NoKey("params"),
                    }
                },
                allow_different_len=False,
            )
            # ответ API с флагом non-dummy-redirects=1
            response = self.report.request_json(
                'place=prime&cvredirect=1&text=подарок+детям&non-dummy-redirects=1' + rgb
            )
            self.assertFragmentIn(
                response,
                {
                    "redirect": {
                        "target": "catalog",
                        "params": {
                            "suggest_text": ["подарок детям"],
                            "was_redir": ["1"],
                            "hid": NoKey("hid"),
                            "nid": ["68795"],
                        },
                    }
                },
                allow_different_len=False,
            )

    @classmethod
    def prepare_tires_redirect(cls):
        cls.index.hypertree += [
            HyperCategory(hid=Const.TIRES_HID),
        ]

        cls.reqwizard.on_default_request().respond()

        # данные для редиректа только по марке
        cls.reqwizard.on_request('shini+na+honda').respond(
            tires_mark='Honda',
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=0, end_char=5),
                ReqwExtMarkupTokenChar(begin_char=6, end_char=8),
                ReqwExtMarkupTokenChar(begin_char=9, end_char=14),
            ],
            found_tires_keyword_positions=[ReqwExtMarkupToken(begin=0, end=1, data=ReqwExtMarkupMarketTiresKeyword())],
            found_tires_mark_positions=[
                ReqwExtMarkupToken(begin=2, end=3, data=ReqwExtMarkupMarketTiresMark(mark_name='Honda'))
            ],
        )
        # данные для редиректа по марке+модели
        cls.reqwizard.on_request('shini+na+skoda+octavia').respond(
            tires_mark='Skoda',
            tires_model='Octavia',
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=0, end_char=5),
                ReqwExtMarkupTokenChar(begin_char=6, end_char=8),
                ReqwExtMarkupTokenChar(begin_char=9, end_char=14),
                ReqwExtMarkupTokenChar(begin_char=15, end_char=22),
            ],
            found_tires_keyword_positions=[ReqwExtMarkupToken(begin=0, end=1, data=ReqwExtMarkupMarketTiresKeyword())],
            found_tires_mark_positions=[
                ReqwExtMarkupToken(begin=2, end=3, data=ReqwExtMarkupMarketTiresMark(mark_name='Skoda'))
            ],
            found_tires_model_positions=[
                ReqwExtMarkupToken(begin=3, end=4, data=ReqwExtMarkupMarketTiresModel(model_name='Octavia'))
            ],
        )
        # то же самое, но запрос не полностью поматчен
        cls.reqwizard.on_request('shini+na+skoda+octavia+krasivye').respond(
            tires_mark='Skoda',
            tires_model='Octavia',
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=0, end_char=5),
                ReqwExtMarkupTokenChar(begin_char=6, end_char=8),
                ReqwExtMarkupTokenChar(begin_char=9, end_char=14),
                ReqwExtMarkupTokenChar(begin_char=15, end_char=22),
                ReqwExtMarkupTokenChar(begin_char=23, end_char=31),
            ],
            found_tires_keyword_positions=[ReqwExtMarkupToken(begin=0, end=1, data=ReqwExtMarkupMarketTiresKeyword())],
            found_tires_mark_positions=[
                ReqwExtMarkupToken(begin=2, end=3, data=ReqwExtMarkupMarketTiresMark(mark_name='Skoda'))
            ],
            found_tires_model_positions=[
                ReqwExtMarkupToken(begin=3, end=4, data=ReqwExtMarkupMarketTiresModel(model_name='Octavia'))
            ],
        )

        # фильтры для проверки работы фильтров в редиректе марка+модель
        cls.index.gltypes += [
            GLType(param_id=5065221, hid=Const.TIRES_HID, gltype=GLType.ENUM, values=[25, 45]),
            GLType(param_id=5065222, hid=Const.TIRES_HID, gltype=GLType.NUMERIC),
            GLType(param_id=5065223, hid=Const.TIRES_HID, gltype=GLType.BOOL),
        ]

        # офферы для проверки работы фильтров в редиректе марка+модель
        # во всех запросах от запроса отстается только 'na', поэтому оно есть в названиях офферов,
        # а остальные слова неважны
        cls.index.offers += [
            # пройдет все фильтры и будет в результате
            Offer(
                title='шины na шкоду октавию',
                hid=Const.TIRES_HID,
                glparams=[
                    GLParam(param_id=5065221, value=25),
                    GLParam(param_id=5065222, value=145),
                    GLParam(param_id=5065223, value=1),
                ],
            ),
            # отфильтруется
            Offer(
                title='шины na что угодно, но не шкоду октавию',
                hid=Const.TIRES_HID,
                glparams=[
                    GLParam(param_id=5065221, value=45),
                    GLParam(param_id=5065222, value=151),
                    GLParam(param_id=5065223, value=0),
                ],
            ),
        ]

    def test_tires_redirect(self):
        """При матчинге марки или марки+модели автомобиля редиректим в шинную категорию
        на соответсвующий лендинг. При этом сами марка и модель автомобиля из запроса вырезаются.

        https://st.yandex-team.ru/MARKETOUT-14187 (флаг market_redirect_to_tires)
        https://st.yandex-team.ru/MARKETOUT-14809 (выкатка на 100%)
        https://st.yandex-team.ru/MARKETOUT-27740 (выпиливание флага)
        """

        # 1. Запрос с маркой без модели
        # 1.1 Задаем запрос и получаем редирект в шины по марке без модели
        request = "cvredirect=1&place=prime&text=shini+na+honda"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "catalog",
                    "params": {
                        "text": ["shini na honda"],
                        "hid": ["90490"],
                        "was_redir": ["1"],
                        "rt": ["14"],
                        "mark": ["honda"],
                        "model": NoKey("model"),
                    },
                }
            },
        )

        # 1.2 Задаем запрос с полученным редиректом и проверяем, что запрос обрезан верно
        # Фильтры не спрашиваем, в этой ситуации данных для подборщика ещё недостаточно,
        # чтобы сформировать все фильтры. Проверим фильтры позже в запросе с маркой и моделью.
        rs = response.root['redirect']['params']['rs'][0]
        request = "place=prime&hid=90490&rs={0}".format(rs)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "isParametricSearch": True,
                },
                "query": {
                    "highlighted": [
                        {"value": "shini", "highlight": True},
                        {"value": " na ", "highlight": NoKey("highlight")},
                        {"value": "honda", "highlight": True},
                    ]
                },
            },
        )

        # 2. Запрос с маркой и моделью
        # 2.1 Задаем запрос и получаем редирект в шины с маркой и моделью
        request = "cvredirect=1&place=prime&text=shini+na+skoda+octavia"
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "catalog",
                    "params": {
                        "text": ["shini na skoda octavia"],
                        "hid": ["90490"],
                        "was_redir": ["1"],
                        "rt": ["14"],
                        "mark": ["skoda"],
                        "model": ["octavia"],
                    },
                }
            },
        )

        # 2.2 Задаем запрос с полученным редиректом, а также с фильтрами,
        # в которые преобразует фронт шинные данные через шинный подборщик.
        # Проверяем, что запрос обрезан верно и что в выдаче правильные фильтры и
        # результат отфильтрован
        rs = response.root['redirect']['params']['rs'][0]
        glfilters = ["5065221:25", "5065222:140,150", "5065221:1"]
        glfilters_query = '&'.join(['glfilter=' + g for g in glfilters])
        request = "place=prime&hid=90490&rs={0}&{1}".format(rs, glfilters_query)
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "isParametricSearch": True,
                    "results": [{"entity": "offer", "titles": {"raw": "шины na шкоду октавию"}}],
                },
                "query": {
                    "highlighted": [
                        {"value": "shini", "highlight": True},
                        {"value": " na ", "highlight": NoKey("highlight")},
                        {"value": "skoda", "highlight": True},
                        {"value": " ", "highlight": NoKey("highlight")},
                        {"value": "octavia", "highlight": True},
                    ]
                },
                "filters": [
                    {"id": "5065221", "isParametric": NoKey("isParametric"), "values": [{"id": "25", "checked": True}]},
                    {
                        "id": "5065222",
                        "isParametric": NoKey("isParametric"),
                        "values": [{"min": "140", "max": "150", "checked": True}],
                    },
                    {
                        "id": "5065223",
                        "isParametric": NoKey("isParametric"),
                    },
                ],
            },
        )

        # 2.3 Проверяем, что при неполном матчинге шинный работает точно так же
        request = "cvredirect=1&place=prime&text=shini+na+skoda+octavia+krasivye"
        for rearr_factors in ('', '&rearr-factors=market_cut_query_in_parametric_search_for_non_whole_match=1'):
            response = self.report.request_json(request + rearr_factors)
            self.assertFragmentIn(
                response,
                {
                    "redirect": {
                        "target": "catalog",
                        "params": {
                            "text": ["shini na skoda octavia krasivye"],
                            "hid": ["90490"],
                            "was_redir": ["1"],
                            "rt": ["14"],
                            "mark": ["skoda"],
                            "model": ["octavia"],
                        },
                    }
                },
            )

    def test_promo_redirect_dont_affect_others(self):
        """проверка, что промо-редирект не влезает в другие редиректы
        по мотивам проблем https://a.yandex-team.ru/arc/commit/4711087
        """

        response = self.report.request_json("cvredirect=1&place=prime&text=shini+na+honda")
        self.assertFragmentIn(response, {"redirect": {}})
        self.assertFragmentNotIn(response, {"redirect": {"params": {"promo-type": NotEmpty()}}})

    @classmethod
    def prepare_shop_redirect(cls):
        cls.reqwizard.on_default_request().respond()

        cls.reqwizard.on_request('unitaz+eldorado').respond(
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=0, end_char=6),
                ReqwExtMarkupTokenChar(begin_char=7, end_char=15),
            ],
            found_shop_positions=[
                # Попадет в редирект, т.к. alias_type='URL'
                ReqwExtMarkupToken(
                    begin=1,
                    end=2,
                    data=ReqwExtMarkupMarketShop(shop_id=346231, alias_type='URL', is_good_for_matching=False),
                ),
                # Попадет в редирект, т.к. is_good_for_matching=True
                ReqwExtMarkupToken(
                    begin=1,
                    end=2,
                    data=ReqwExtMarkupMarketShop(shop_id=346232, alias_type='FROM_URL', is_good_for_matching=True),
                ),
                ReqwExtMarkupToken(
                    begin=1,
                    end=2,
                    data=ReqwExtMarkupMarketShop(shop_id=346233, alias_type='URL', is_good_for_matching=True),
                ),
                # Не попадет в редирект, т.к. is_good_for_matching=False и alias_type!='URL'
                ReqwExtMarkupToken(
                    begin=1,
                    end=2,
                    data=ReqwExtMarkupMarketShop(shop_id=346234, alias_type='NAME', is_good_for_matching=False),
                ),
            ],
        )

        # Оффер найдется текстовым поиском, но отвалится при фильтрациях
        cls.index.offers += [
            Offer(title='unitaz eldorado'),
        ]

        cls.reqwizard.on_request('mobila+svyaznoy').respond(
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=0, end_char=6),
                ReqwExtMarkupTokenChar(begin_char=7, end_char=15),
            ],
            found_shop_positions=[
                ReqwExtMarkupToken(
                    begin=1,
                    end=2,
                    data=ReqwExtMarkupMarketShop(shop_id=346235, alias_type='URL', is_good_for_matching=True),
                ),
                ReqwExtMarkupToken(
                    begin=1,
                    end=2,
                    data=ReqwExtMarkupMarketShop(shop_id=346236, alias_type='URL', is_good_for_matching=True),
                ),
            ],
        )
        # запрос с предлогом "в" перед магазином
        # предлог должен будет вырезаться вместе с магазином и поиск будет как по запросу [mobila svyaznoy], т.е. по [mobila]
        cls.reqwizard.on_request('mobila+в+svyaznoy').respond(
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=0, end_char=6),
                ReqwExtMarkupTokenChar(begin_char=7, end_char=8, text='в'),
                ReqwExtMarkupTokenChar(begin_char=9, end_char=17),
            ],
            found_shop_positions=[
                ReqwExtMarkupToken(
                    begin=2,
                    end=3,
                    data=ReqwExtMarkupMarketShop(shop_id=346235, alias_type='URL', is_good_for_matching=True),
                ),
                ReqwExtMarkupToken(
                    begin=2,
                    end=3,
                    data=ReqwExtMarkupMarketShop(shop_id=346236, alias_type='URL', is_good_for_matching=True),
                ),
            ],
        )

        cls.index.offers += [
            Offer(title='mobila 1', fesh='346235'),
            Offer(title='mobila 2', fesh='346236'),
            Offer(title='mobila 3', fesh='346237'),  # не наматчен такой магазин в запросе, отфильтруется
        ]

    def test_shop_redirect(self):
        """При матчинге магазина в запросе пытаемся средиректить
        на поиск с проставленным фильтром магазина.
        В случае пустого серпа как и в параметрическом редирект отменяется.

        Если на запрос матчится несколько магазинов, то пытаемся поставить фильтр
        по всем магазинам на какой-то определенной позиции


        https://st.yandex-team.ru/MARKETOUT-13758 (флаг market_redirect_to_shop)
        https://st.yandex-team.ru/MARKETOUT-15800 (выкатка на 100%)
        https://st.yandex-team.ru/MARKETOUT-27784 (выпиливание флага)
        """

        def make_search_request_after_redirect(redirect_response):
            rs = redirect_response.root['redirect']['params']['rs'][0]
            text = redirect_response.root['redirect']['params']['text'][0].encode('utf-8')
            shops = redirect_response.root['redirect']['params']['fesh']
            shops_query = '&'.join(['fesh=' + fesh for fesh in shops])
            request = "place=prime&was_redir=1&rs={0}&text={1}&{2}".format(rs, text, shops_query)
            return request

        # 1. Запрос с пустым серпом
        request = "cvredirect=1&place=prime&text=unitaz+eldorado&debug=da"
        # отключаем market_redirect_to_alone_category
        request += '&rearr-factors=market_redirect_to_alone_category=0;'
        request += 'market_category_redirect_treshold=3;market_sequential_category_redirect_threshold=3'  # борьба с категорийным редиректом
        response = self.report.request_json(request)

        # Проверяем, что редирект не сформировался, потому что серп после редиректа был бы пустой
        self.assertFragmentNotIn(response, {"redirect": NotEmpty()})
        response_without_redirect = {
            "search": {
                "total": 1,
                "isParametricSearch": False,
                "results": [{"entity": "offer", "titles": {"raw": "unitaz eldorado"}}],
            },
        }
        self.assertFragmentIn(response, response_without_redirect)
        self.assertFragmentIn(response, {"logicTrace": [Contains(Const.DEBUG_CANCELED_PARAM_MSG)]})

        # 2. Запрос с пустым серпом до и непустым серпом после формализации
        # Проверяем среди прочего, что при неполном матчинге чисто магазинный редирект работает точно так же

        request = "cvredirect=1&place=prime&text=mobila+svyaznoy"
        # отключаем market_redirect_to_alone_category
        request += '&rearr-factors=market_redirect_to_alone_category=0;'
        request += 'market_category_redirect_treshold=3;market_sequential_category_redirect_threshold=3'  # борьба с категорийным редиректом

        # 2.1 Запрос за редиректом
        response = self.report.request_json(request)

        # Проверяем, что в выдаче появился соотвествующий редирект
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "text": ["mobila svyaznoy"],
                        "was_redir": ["1"],
                        "rt": ["1"],
                        "fesh": ["346235", "346236"],
                    },
                }
            },
            allow_different_len=False,
        )

        # 2.2 Задаем запрос после редиректа
        new_request = make_search_request_after_redirect(response)
        response = self.report.request_json(new_request + '&debug=da')

        # Проверяем, что выдача стала непустой и там офферы только правильных магазинов
        # и правильно подсвеченный запрос
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "isParametricSearch": True,
                    "results": [
                        {"entity": "offer", "titles": {"raw": "mobila 1"}},
                        {"entity": "offer", "titles": {"raw": "mobila 2"}},
                    ],
                },
                "query": {
                    "highlighted": [
                        {"value": "mobila ", "highlight": NoKey("highlight")},
                        {"value": "svyaznoy", "highlight": True},
                    ]
                },
                "filters": [
                    {
                        "id": "fesh",
                        "isParametric": True,
                        "values": [
                            {
                                "id": "346235",
                                "checked": True,
                            },
                            {
                                "id": "346236",
                                "checked": True,
                            },
                        ],
                    }
                ],
            },
        )

        # 3. Запрос с предлогом "в" перед магазином.
        # https://st.yandex-team.ru/MARKETOUT-15542
        # 3.1 Задаем запрос с флагом (включен по умолчанию в MARKETOUT-15800)
        request = 'cvredirect=1&place=prime&text=mobila+в+svyaznoy'
        # отключаем market_redirect_to_alone_category
        request += '&rearr-factors=market_redirect_to_alone_category=0;'
        request += 'market_category_redirect_treshold=3;market_sequential_category_redirect_threshold=3'  # борьба с категорийным редиректом
        response = self.report.request_json(request)

        # Проверяем, что в выдаче появился соотвествующий редирект
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "text": ["mobila в svyaznoy"],
                        "was_redir": ["1"],
                        "rt": ["1"],
                        "fesh": ["346235", "346236"],
                    },
                }
            },
            allow_different_len=False,
        )

        # 3.2 Задаем запрос после редиректа
        request = make_search_request_after_redirect(response)
        response = self.report.request_json(request)

        # Проверяем, что выдача такая же, как и в п. 2 (т.е. предлог не влияет), и запрос правильно подсвечен
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "isParametricSearch": True,
                    "results": [
                        {"entity": "offer", "titles": {"raw": "mobila 1"}},
                        {"entity": "offer", "titles": {"raw": "mobila 2"}},
                    ],
                },
                "query": {
                    "highlighted": [
                        {"value": "mobila ", "highlight": NoKey("highlight")},
                        {"value": "в svyaznoy", "highlight": True},
                    ]
                },
                "filters": [
                    {
                        "id": "fesh",
                        "isParametric": True,
                        "values": [
                            {
                                "id": "346235",
                                "checked": True,
                            },
                            {
                                "id": "346236",
                                "checked": True,
                            },
                        ],
                    }
                ],
            },
        )

        # 4. Для синего маркета магазинный редирект выключен
        response = self.report.request_json(request + '&rgb=blue')

        # Проверяем, что редиректа нет
        self.assertFragmentNotIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                    },
                }
            },
        )

    @classmethod
    def prepare_shops_in_category_and_parametric_redirects(cls):
        cls.index.hypertree += [
            HyperCategory(hid=5, output_type=HyperCategoryType.GURU),
        ]
        cls.index.gltypes += [
            GLType(param_id=101, hid=5, gltype=GLType.NUMERIC),
        ]

        cls.index.offers += [Offer(title="plate 30 mm", hid=5, glparams=[GLParam(param_id=101, value=30)], fesh=76348)]
        cls.formalizer.on_default_request().respond()

        # Для категорийного редиректа
        cls.reqwizard.on_request('plates+obi').respond(
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=0, end_char=6),
                ReqwExtMarkupTokenChar(begin_char=7, end_char=10),
            ],
            found_shop_positions=[
                ReqwExtMarkupToken(
                    begin=1,
                    end=2,
                    data=ReqwExtMarkupMarketShop(shop_id=76348, alias_type='NAME', is_good_for_matching=True),
                ),
            ],
        )

        # Для параметрического редиректа
        cls.formalizer.on_request(hid=5, query="plates 30 mm obi").respond(
            formalized_params=[
                FormalizedParam(
                    param_id=101, value=30.0, is_numeric=True, value_positions=(7, 9), unit_positions=(10, 12)
                )
            ]
        )
        cls.reqwizard.on_request('plates+30+mm+obi').respond(
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=0, end_char=6),
                ReqwExtMarkupTokenChar(begin_char=7, end_char=9),
                ReqwExtMarkupTokenChar(begin_char=10, end_char=12),
                ReqwExtMarkupTokenChar(begin_char=13, end_char=16),
            ],
            found_shop_positions=[
                ReqwExtMarkupToken(
                    begin=3,
                    end=4,
                    data=ReqwExtMarkupMarketShop(shop_id=76348, alias_type='NAME', is_good_for_matching=True),
                ),
            ],
        )

    def test_shops_in_category_and_parametric_redirects(self):
        """Матчинг магазинов, их проставление в фильтры
        в категорийном и параметрическом редиректах

        https://st.yandex-team.ru/MARKETOUT-15431
        """

        def make_search_request_after_category_redirect(redirect_response):
            rs = redirect_response.root['redirect']['params']['rs'][0]
            text = redirect_response.root['redirect']['params']['text'][0]
            hid = redirect_response.root['redirect']['params']['hid'][0]
            shops = redirect_response.root['redirect']['params']['fesh']
            shops_query = '&'.join(['fesh=' + fesh for fesh in shops])
            request = "place=prime&was_redir=1&rs={0}&text={1}&{2}&hid={3}".format(rs, text, shops_query, hid)
            return request

        # 1. В категорийном редиректе
        text = 'plates obi'

        # 1.1 Проверяем редирект
        request = 'place=prime&text={}&cvredirect=1'.format(text)

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "text": [text],
                        "was_redir": ["1"],
                        "hid": ["5"],
                        "rt": ["9"],
                        "fesh": ["76348"],
                        "slug": ["hid-5"],
                    },
                }
            },
            allow_different_len=False,
        )

        # 1.2 Запрос после редиректа
        request = make_search_request_after_category_redirect(response)
        response = self.report.request_json(request + "&debug=da")

        # Проверяем, что фильтр проставился
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "isParametricSearch": True,
                },
                "query": {
                    "highlighted": [
                        {"value": "plates ", "highlight": NoKey("highlight")},
                        {"value": "obi", "highlight": True},
                    ]
                },
                "filters": [
                    {
                        "id": "fesh",
                        "isParametric": True,
                        "values": [
                            {
                                "id": "76348",
                                "checked": True,
                            },
                        ],
                    }
                ],
            },
        )

        # 2. В параметрическом редиректе
        def make_search_request_after_parametric_redirect(redirect_response):
            rs = redirect_response.root['redirect']['params']['rs'][0]
            text = redirect_response.root['redirect']['params']['text'][0]
            hid = redirect_response.root['redirect']['params']['hid'][0]
            shops = redirect_response.root['redirect']['params']['fesh']
            glfilters = redirect_response.root['redirect']['params']['glfilter']
            shops_query = '&'.join(['fesh=' + fesh for fesh in shops])
            glfilters_query = '&'.join(['glfilter=' + glfilter for glfilter in glfilters])
            request = "place=prime&was_redir=1&rs={0}&text={1}&{2}&hid={3}&{4}".format(
                rs, text, shops_query, hid, glfilters_query
            )
            return request

        text = 'plates 30 mm obi'

        # 2.1 Проверяем редирект
        request = 'place=prime&text={}&cvredirect=1'.format(text)
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["11"],
                        "hid": ["5"],
                        "text": [text],
                        "fesh": ["76348"],
                        "glfilter": ["101:30,30"],
                        "slug": ["hid-5"],
                    },
                }
            },
            preserve_order=False,
        )

        # 2.2 Запрос после редиректа

        request = make_search_request_after_parametric_redirect(response)
        response = self.report.request_json(request + "&debug=da")

        # Проверяем, что фильтр проставился и запрос подсветился
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "isParametricSearch": True,
                },
                "query": {
                    "highlighted": [
                        {"value": "plates ", "highlight": NoKey("highlight")},
                        {"value": "30", "highlight": True},
                        {"value": " ", "highlight": NoKey("highlight")},
                        {"value": "mm", "highlight": True},
                        {"value": " ", "highlight": NoKey("highlight")},
                        {"value": "obi", "highlight": True},
                    ]
                },
                "filters": [
                    {
                        "id": "fesh",
                        "isParametric": True,
                        "values": [
                            {
                                "id": "76348",
                                "checked": True,
                            },
                        ],
                    }
                ],
            },
        )

    @classmethod
    def prepare_model_redirect_data(cls):
        cls.index.hypertree += [HyperCategory(hid=91031, name='Видеокарты')]
        cls.index.models += [Model(hyperid=500, title='Geforce GTX 1060', hid=91031)]

    @skip('We delete model redirect')
    def test_model_redirect_disabled(self):
        """По умолчанию редирект в модель отключен полностью"""

        request = 'debug=1&debug-doc-count=10&place=prime&cvredirect=1&text=geforce+gtx&debug=da'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "text": ["geforce gtx"],
                        "hid": ["91031"],
                        "rt": ["9"],  # Redirect to category
                        "was_redir": ["1"],
                        "srnum": ["1"],
                        "slug": ["videokarty"],
                    },
                    "target": "search",
                }
            },
        )
        self.assertFragmentIn(response, "Cannot redirect to model page")

    @skip('We delete model redirect')
    def test_model_redirect(self):
        """Проверка включения модельного редиректа для заданных категорий
        указанием флагов market_model_redirect_by_category_disable=0 и market_redirect_to_alone_model=1
        https://st.yandex-team.ru/MARKETOUT-16819
        https://st.yandex-team.ru/MARKETOUT-17069
        https://st.yandex-team.ru/MARKETOUT-26233
        """
        request = 'debug=1&debug-doc-count=10&place=prime&cvredirect=1&text=geforce+gtx&rearr-factors=market_redirect_to_alone_model=1;market_disable_redirect_to_model_by_formula=0'
        # Без указания флага market_model_redirect_by_category_disable редирект в модель отключен
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "text": ["geforce gtx"],
                        "hid": ["91031"],
                        "rt": ["9"],  # Redirect to category
                        "was_redir": ["1"],
                        "srnum": ["1"],
                        "slug": ["videokarty"],
                    },
                    "target": "search",
                }
            },
        )

        # С явным выключением флага market_model_redirect_by_category_disable=1 редирект в модель включается
        response = self.report.request_json(request + ";market_model_redirect_by_category_disable=0")
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "text": ["geforce gtx"],
                        "hid": ["91031"],
                        "modelid": ["500"],
                        "rt": ["4"],  # Redirect to model
                        "was_redir": ["1"],
                        "srnum": ["1"],
                    },
                    "target": "product",
                }
            },
        )

    @classmethod
    def prepare_suggest_redirect_no_suggest_text(cls):
        cls.suggester.on_request(part='jeans no suggest text').respond(
            suggestions=[Suggestion(part='jeans no suggest text', url='/catalog/1522?hid=522', type='ecom-category')]
        )

    def test_suggest_redirect_no_suggest_text(self):
        """Проверяем, что suggest_text подставляется для саджестового редиректа.
        https://st.yandex-team.ru/MARKETOUT-17028
        """
        response = self.report.request_json('place=prime&cvredirect=1&text=jeans+no+suggest+text')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of("/catalog/1522?hid=522&was_redir=1&rt=12&suggest_text=jeans no suggest text"),
                    "params": NoKey("params"),
                }
            },
        )

    @classmethod
    def prepare_suggest_canonization_redirect(cls):
        cls.suggester.on_request(part='telephone').respond(
            suggestions=[
                Suggestion(
                    part='telephones',
                    url='/catalogmodels.xml?hid=520&CAT_ID=5201&suggest=1&suggest_text=telephone',
                    type='ecom-category',
                )
            ]
        )

    def test_suggest_canonization_redirect(self):
        """https://st.yandex-team.ru/MARKETOUT-16594
        При полном совпадении с запросом синхронизируем с саджестом (пишем запрос в suggest_text вместо text)
        """

        response = self.report.request_json('place=prime&cvredirect=1&text=telephone')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of(
                        "/catalogmodels.xml?hid=520&CAT_ID=5201&suggest=1&suggest_text=telephone&was_redir=1&rt=12&suggest_text=telephone"
                    ),
                    "params": NoKey("params"),
                }
            },
        )

    def test_category_canonization_redirect(self):
        """https://st.yandex-team.ru/MARKETOUT-16594"""

        response = self.report.request_json('place=prime&cvredirect=1&text=Cat1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {"was_redir": ["1"], "hid": ["86001"], "suggest_text": ["Cat1"]},
                    "target": "catalog",
                }
            },
        )

    def test_escaping_url(self):
        """Проверка эскейпинга кириллицы"""
        # Синий маркет плохо работает с кириллицей, поэтому эскейпим ее
        # Проверка не через LikeUrl, так как он делает Unescape и работает с незаэскейпленным вариантом
        response = self.report.request_json('place=prime&cvredirect=1&text=подарок+детям')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": Contains("suggest_text={0}".format(urllib.quote("подарок+детям", safe='+'))),
                    "params": NoKey("params"),
                }
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_slug_redirects(cls):
        cls.index.navtree += [
            NavCategory(nid=123123, hid=111),
            NavCategory(nid=123124, hid=90411),
        ]

        cls.suggester.on_request(part='турка').respond(
            suggestions=[
                Suggestion(
                    part='турка',
                    url='/catalog--turka/123123?hid=111&gfilter=1801946:1871375&suggest=1',
                    type='ecom-category',
                ),
            ]
        )

        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='зеленый слоник', url='/product--zelenyi-slonik/777'),
            RedirectWhiteListRecord(query='слоны', url='/catalog--slony/54423?hid=90411'),
        ]

    def test_slug_in_whitelist_redirect(self):
        """Если из whitelist redirects приходит чпу, репорт должен возвращать slug при non-dummy-redirects=1"""
        response = self.report.request_json('place=prime&cvredirect=1&text=зеленый слоник')
        self.assertFragmentIn(response, {"redirect": {"url": LikeUrl.of("/product--zelenyi-slonik/777")}})

        response = self.report.request_json('place=prime&cvredirect=1&text=зеленый слоник&non-dummy-redirects=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "slug": ["zelenyi-slonik"],
                    },
                },
            },
        )

        response = self.report.request_json('place=prime&cvredirect=1&text=слоны')
        self.assertFragmentIn(response, {"redirect": {"url": LikeUrl.of("/catalog--slony/54423?hid=90411")}})

        response = self.report.request_json('place=prime&cvredirect=1&text=слоны&non-dummy-redirects=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "slug": ["slony"],
                        "hid": ["90411"],
                    },
                },
            },
        )

    def test_slug_in_suggest_redirect(self):
        """Если от suggest-market приходит чпу, репорт должен возвращать slug при non-dummy-redirects=1"""
        response = self.report.request_json('place=prime&cvredirect=1&text=турка')
        self.assertFragmentIn(
            response,
            {"redirect": {"url": LikeUrl.of("/catalog--turka/123123?hid=111&gfilter=1801946:1871375&suggest=1")}},
        )

        response = self.report.request_json('place=prime&cvredirect=1&text=турка&non-dummy-redirects=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "slug": ["turka"],
                        "nid": ["123123"],
                        "hid": ["111"],
                        "suggest_text": ["турка"],
                    },
                },
            },
        )

    @skip('We delete model redirect')
    def test_slug_in_model_redirect(self):
        """При несаджестовых редиректах для моделей должен передаваться slug"""
        response = self.report.request_json(
            "place=prime&cvredirect=1&text=geforce+gtx"
            "&rearr-factors=market_model_redirect_by_category_disable=0;"
            "market_redirect_to_alone_model=1;market_disable_redirect_to_model_by_formula=0"
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "text": ["geforce gtx"],
                        "hid": ["91031"],
                        "modelid": ["500"],
                        "rt": ["4"],  # Redirect to model
                        "was_redir": ["1"],
                        "srnum": ["1"],
                        "slug": ["geforce-gtx-1060"],
                    },
                    "target": "product",
                }
            },
        )

    def test_slug_in_category_redirect(self):
        """При несаджестовых редиректах для каталога должен передаваться slug"""
        response = self.report.request_json('place=prime&cvredirect=1&text=geforce+gtx')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "text": ["geforce gtx"],
                        "hid": ["91031"],
                        "rt": ["9"],
                        "was_redir": ["1"],
                        "srnum": ["1"],
                        "slug": ["videokarty"],
                    },
                    "target": "search",
                }
            },
        )

    def test_exact_match(self):
        """exact-match=1 должен пробрасываться в редиректы на search"""
        text_request_search = ["билеты+на+поезд", "male+trunks"]
        ans_search = ["/search.xml?", "/search?"]
        text_request_nosearch = [
            "telephones",
            "cameras",
            "baon",
            "bosch",
            "pyatyorochka+reviews",
            "зелёный слоник",
            "подарок+детям",
            "gift for a girlfriend",
            "how to choose a mobile phone",
        ]
        ans_nosearch = [
            "/catalogmodels.xml?",
            "/catalog/1521?",
            "/brands/111?",
            "/brands.xml?",
            "/shop-opinions.xml?",
            "/product--zelenyi-slonik/777?",
            "/catalog/68795?",
            "/collections/v-podarok-ljubimomu?",
            "/articles/kak-vybrat-mobilnyj-telefon?",
        ]
        for exact_match in (0, 1):
            # redirect в CheckRedirect
            response = self.report.request_json(
                "place=prime&cvredirect=1&text=geforce+gtx+1060&exact-match={}".format(exact_match)
            )
            self.assertFragmentIn(
                response,
                {
                    "redirect": {
                        "params": {
                            "exact-match": ["1"] if exact_match else NoKey("exact-match"),
                        },
                        "target": "search",
                    }
                },
            )
            # redirect в CheckBeforeSearchRedirect
            for i in range(len(text_request_search)):
                response = self.report.request_json(
                    "place=prime&cvredirect=1&text={}&exact-match={}".format(text_request_search[i], exact_match)
                )
                self.assertFragmentIn(
                    response,
                    {
                        "redirect": {
                            "url": LikeUrl.of(ans_search[i] + "exact-match=1")
                            if exact_match
                            else LikeUrl.of(ans_search[i], no_params=["exact-match"])
                        }
                    },
                )
        # проверим что в не search exact-match не пробрасывается
        for i in range(len(text_request_nosearch)):
            response = self.report.request_json(
                "place=prime&cvredirect=1&exact-match=1&text=" + text_request_nosearch[i]
            )
            self.assertFragmentIn(
                response, {"redirect": {"url": LikeUrl.of(ans_nosearch[i], no_params=["exact-match"])}}
            )

    def test_adult_category_redirect_with_no_adult_param(self):
        '''
        Проверяем, что без указания adult редирект будет в обычную категорию
        '''
        response = self.report.request_json('place=prime&text=вибратор&cvredirect=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "text": ["вибратор"],
                        "hid": ["350"],
                    }
                }
            },
        )

    def test_adult_category_redirect_skip_if_adult_with_no_adult_param(self):
        '''
        Проверяем, что в эксперименте без указания adult редиректа  не будет
        (при наличии большого количетсва adult-офферов в релевантности)
        '''
        for text in ['вибратор', 'сорочка']:
            response = self.report.request_json(
                'place=prime&text={}&cvredirect=1&rearr-factors=market_skip_redirect_with_adult_offers_and_unknown_adult=1'.format(
                    text
                )
            )
            self.assertFragmentIn(response, {"search": {"results": NotEmpty()}})

    def test_adult_category_redirect_with_adult_eq_1(self):
        '''
        Проверяем, что с adult=1 редирект будет во взрослую категорию
        '''
        for rearr_flags in ['', '&rearr-factors=market_skip_redirect_with_adult_offers_and_unknown_adult=1']:
            response = self.report.request_json('place=prime&text=вибратор&adult=1&cvredirect=1{}'.format(rearr_flags))
            self.assertFragmentIn(
                response,
                {
                    "redirect": {
                        "params": {
                            "text": ["вибратор"],
                            "hid": [str(ADULT_CATEG_ID)],
                        }
                    }
                },
            )

    def test_adult_category_redirect_with_adult_eq_0(self):
        '''
        Проверяем, что c adult=0 или family=1 редирект будет в обычную категорию
        '''
        for rearr_flags in ['', '&rearr-factors=market_skip_redirect_with_adult_offers_and_unknown_adult=1']:
            for adult in ['&adult=0', '&family=1']:
                response = self.report.request_json(
                    'place=prime&text=вибратор{}&cvredirect=1{}'.format(adult, rearr_flags)
                )
                self.assertFragmentIn(
                    response,
                    {
                        "redirect": {
                            "params": {
                                "text": ["вибратор"],
                                "hid": ["350"],
                            }
                        }
                    },
                )

    @classmethod
    def prepare_shop_opinions_redirect(cls):
        cls.reqwizard.on_default_request().respond()

        cls.reqwizard.on_request('DNS+отзывы').respond(
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=0, end_char=4),
                ReqwExtMarkupTokenChar(begin_char=5, end_char=10),
            ],
            found_shop_positions=[
                # Попадет в редирект, т.к. alias_type='URL'
                ReqwExtMarkupToken(
                    begin=0,
                    end=1,
                    data=ReqwExtMarkupMarketShop(shop_id=3118001, alias_type='URL', is_good_for_matching=False),
                ),
            ],
        )

        cls.reqwizard.on_request('DNS').respond(
            token_chars=[ReqwExtMarkupTokenChar(begin_char=0, end_char=3)],
            found_shop_positions=[
                # Попадет в редирект, т.к. alias_type='URL'
                ReqwExtMarkupToken(
                    begin=0,
                    end=0,
                    data=ReqwExtMarkupMarketShop(shop_id=3118001, alias_type='URL', is_good_for_matching=False),
                ),
            ],
        )

        cls.reqwizard.on_request('DNS+товары+отзывы').respond(
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=0, end_char=4),
                ReqwExtMarkupTokenChar(begin_char=5, end_char=10),
                ReqwExtMarkupTokenChar(begin_char=12, end_char=17),
            ],
            found_shop_positions=[
                # Попадет в редирект, т.к. alias_type='URL'
                ReqwExtMarkupToken(
                    begin=0,
                    end=1,
                    data=ReqwExtMarkupMarketShop(shop_id=3118001, alias_type='URL', is_good_for_matching=False),
                ),
            ],
        )

        cls.index.shops += [
            Shop(fesh=3118001, business_fesh=12345, business_name="дынысы", name='DNS'),
        ]

    def test_shop_opinions_redirect(self):
        """Проверяем редиректы на отзывы о магазине"""
        response = self.report.request_json(
            'place=prime&cvredirect=1&text=DNS+отзывы&rearr-factors=enable_business_id=0'
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of("/shop--dns/3118001/reviews?was_redir=1&rt=19"),
                    "params": NoKey("params"),
                }
            },
        )

        response = self.report.request_json(
            'place=prime&cvredirect=1&text=DNS+отзывы&rearr-factors=enable_business_id=1'
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of("/shop--dynysy/12345/reviews?was_redir=1&rt=19"),
                    "params": NoKey("params"),
                }
            },
        )

        # если есть "лишние" слова в запросе, то нет редиректа
        response = self.report.request_json('place=prime&cvredirect=1&text=DNS+товары+отзывы')
        self.assertFragmentNotIn(
            response,
            {
                "redirect": {},
            },
        )

    def test_allow_shop_in_shop_redirect(self):
        response = self.report.request_json(
            'place=prime&cvredirect=1&text=DNS&rearr-factors=allow_shop_in_shop_redirect=1;enable_business_id=0'
        )
        fragment = {"redirect": {"url": LikeUrl.of("/shop--dns/3118001?was_redir=1&rt=20")}}
        self.assertFragmentIn(response, fragment)

        response = self.report.request_json('place=prime&cvredirect=1&text=DNS')
        self.assertFragmentNotIn(response, fragment)

        response = self.report.request_json(
            'place=prime&cvredirect=1&text=DNS&rearr-factors=allow_shop_in_shop_redirect=1;enable_business_id=1'
        )
        fragment = {"redirect": {"url": LikeUrl.of("/shop--dynysy/12345?was_redir=1&rt=20")}}
        self.assertFragmentIn(response, fragment)

    def test_disable_shop_suggest_redirect(self):
        response = self.report.request_json('place=prime&cvredirect=1&text=днс')
        fragment = {"redirect": {"url": LikeUrl.of("/shop--dns/3118001?was_redir=1&rt=19")}}
        self.assertFragmentIn(response, fragment)

        response = self.report.request_json(
            'place=prime&cvredirect=1&text=днс&rearr-factors=disable_shop_suggest_redirect=1'
        )
        self.assertFragmentNotIn(response, fragment)

    @classmethod
    def prepare_singular_suggest_redirect(cls):
        cls.suggester.on_request(part="игрушка для детей").respond(
            suggestions=[
                Suggestion(
                    part='игрушки для дитя',
                    url='/category/321321',
                    singular_name='игрушка для дитя',
                    type='ecom-category',
                ),
                Suggestion(
                    part='игрушки для детей',
                    url='/category/345345',
                    singular_name='игрушка для детей',
                    type='ecom-category',
                ),
            ]
        )

        cls.suggester.on_request(part='телефоны').respond(
            suggestions=[
                Suggestion(part='телефоны для всех', url='/category/322322', type='ecom-category'),
                Suggestion(part='телефоны', url='/category/346346', type='ecom-category'),
            ]
        )

        cls.suggester.on_request(part='глобус').respond(
            suggestions=[
                Suggestion(part='очень красивые глобусы', url='/category/1111', type='ecom-category'),
                Suggestion(part='глобусы', url='/category/2222', singular_name='глобус', type='ecom-category'),
            ]
        )

        cls.suggester.on_custom_url_request(
            part='точно не микроволновка', location='suggest-market-rich-for-redirect'
        ).respond(
            suggestions=[
                Suggestion(
                    part='микроволновка',
                    url='/category/1212',
                    singular_name='точно не микроволновка',
                ),
            ]
        )

    def test_singular_suggest_redirect(self):
        """
        @see: https://st.yandex-team.ru/MARKETOUT-34663
        Для категорийно-вендорных и категорийных саджестов в json, возвращаемый от саджестера, было добавлено
        поле с формой единственного числа данного саджеста. Теперь саджестовый редирект происходит и в том случае,
        когда запрос пользователя совпадает с этим полем.
        """

        # проверяем, что описанная выше логика работает
        response = self.report.request_json('place=prime&cvredirect=1&text=игрушка+для+детей&debug=da')
        self.assertFragmentIn(response, {'redirect': {'url': Contains('/category/345345')}})
        self.assertFragmentIn(
            response,
            {'logicTrace': [Contains('Got suggest redirect using words canonization')]},
        )

        # проверяем, что отсутствие поля в выдаче саджестера ничего не меняет и редирект происходт по матчингу имени
        response = self.report.request_json('place=prime&cvredirect=1&text=телефоны')
        self.assertFragmentIn(response, {'redirect': {'url': Contains('/category/346346')}})

        # проверяем, что новая логика работает с флагом market_suggest_use_shiny_http
        response = self.report.request_json(
            'place=prime&cvredirect=1&text=глобус&rearr-factors=market_suggest_use_shiny=1&debug=da'
        )
        self.assertFragmentIn(response, {'redirect': {'url': Contains('/category/2222')}})
        self.assertFragmentIn(
            response,
            {'logicTrace': [Contains('Got suggest redirect using words canonization')]},
        )

        # проверяем на искуственном примере, что новая логика действительно сравнивает с полем, которое приходит от саджестера
        response = self.report.request_json(
            'place=prime&cvredirect=1&text=точно+не+микроволновка&rearr-factors=market_use_new_suggest_for_redirect=0&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains(
                        'Got singular suggest redirect. SingularName "точно не микроволновка" matched with request text'
                    )
                ]
            },
        )
        self.assertFragmentIn(response, {'redirect': {'url': Contains('/category/1212')}})

    @classmethod
    def prepare_blue_suggest_redirects_on_white_cpa(cls):
        cls.suggester.on_custom_url_request(part='кроссовки', location='suggest-market-rich-for-redirect').respond(
            suggestions=[Suggestion(part='кроссовки', url='/category/white')]
        )
        cls.suggester.on_request(part='кроссовки').respond(
            suggestions=[Suggestion(part='кроссовки', url='/category/white')]
        )
        cls.suggester.on_custom_url_request(part='кроссовки', location='suggest-market-rich-blue').respond(
            suggestions=[Suggestion(part='кроссовки', url='/category/blue')]
        )

    def test_blue_suggest_redirects_on_white_cpa(self):
        """
        Проверяем, что в редиректах на белом с галкой для синхронизации
        используется синий саджестер
        """

        request = 'place=prime&cvredirect=1&text=кроссовки' '&rearr-factors=market_use_new_suggest_for_redirect=0'
        flag = '&rearr-factors=market_blue_suggest_redirects_for_white_cpa=0'

        for use_shiny in ('', '&rearr-factors=market_suggest_use_shiny=0', '&rearr-factors=market_suggest_use_shiny=1'):

            for rgb in ('', '&rgb=green', '&rgb=green_with_blue'):

                cpa = '&cpa=no'
                response = self.report.request_json(request + use_shiny + rgb + cpa)
                self.assertFragmentIn(response, {'redirect': {'url': Contains('/category/white')}})

                response = self.report.request_json(request + use_shiny + rgb + cpa + flag)
                self.assertFragmentIn(response, {'redirect': {'url': Contains('/category/white')}})

                cpa = '&cpa=real'
                response = self.report.request_json(
                    request + use_shiny + rgb + cpa + '&rearr-factors=market_use_new_suggest_for_redirect=0'
                )
                self.assertFragmentIn(response, {'redirect': {'url': Contains('/category/blue')}})

                response = self.report.request_json(request + use_shiny + rgb + cpa + flag)
                self.assertFragmentIn(response, {'redirect': {'url': Contains('/category/white')}})

    def test_disable_suggest_redirect(self):
        """
        Проверяем, что под флагом не делаются саджестовые редиректы
        """
        request = 'place=prime&cvredirect=1&text=кроссовки'
        flag = '&rearr-factors=market_disable_suggest_redirect=1'

        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'redirect': {'url': Contains('/category/white')}})

        response = self.report.request_json(request + flag)
        self.assertFragmentNotIn(response, {'redirect': {'url': Contains('/category/white')}})

    @classmethod
    def prepare_blue_whitelist_redirects(cls):

        cls.index.navtree += [
            NavCategory(hid=392001, nid=392001),
            NavCategory(hid=392002, nid=392002),
            NavCategory(hid=392003, nid=392003),
            NavCategory(hid=392004, nid=392004),
        ]

        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(
                query="blue on white cpa white list", url="/catalog--white-by-white-list/392001/list?hid=392001"
            ),
        ]

        cls.suggester.on_custom_url_request(
            part='blue on white cpa suggest', location='suggest-market-rich-for-redirect'
        ).respond(
            suggestions=[
                Suggestion(part='blue on white cpa suggest', url='/catalog--white-by-suggest/392003/list?hid=392003')
            ]
        )
        cls.suggester.on_custom_url_request(
            part='blue on white cpa suggest', location='suggest-market-rich-blue'
        ).respond(
            suggestions=[
                Suggestion(part='blue on white cpa suggest', url='/catalog/blue-by-suggest/392004/list?hid=392004')
            ]
        )

    def test_blue_suggest_in_case_cpa_real(self):
        """
        Проверяем, что в саджестовых редиректах
        на белом с галкой для синхронизации саджестов и редиректов используется синие саджесты
        при этом вайтлист используется белый
        """

        request = (
            'place=prime&cvredirect=1&text=blue on white cpa suggest'
            '&rearr-factors=market_use_new_suggest_for_redirect=0'
        )
        flag = '&rearr-factors=market_blue_suggest_redirects_for_white_cpa=0'
        cpa = '&cpa=real'

        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'redirect': {'url': Contains('/catalog--white-by-suggest')}})

        response = self.report.request_json(request + flag)
        self.assertFragmentIn(response, {'redirect': {'url': Contains('/catalog--white-by-suggest')}})

        response = self.report.request_json(request + cpa)
        self.assertFragmentIn(response, {'redirect': {'url': Contains('/catalog/blue-by-suggest', 'cpa=1')}})

        response = self.report.request_json(request + cpa + flag)
        self.assertFragmentIn(response, {'redirect': {'url': Contains('/catalog--white-by-suggest', 'cpa=1')}})

        # редиректы из белого списка не переключаются флагами
        request = 'place=prime&cvredirect=1&text=blue on white cpa white list'
        flag = '&rearr-factors=market_blue_suggest_redirects_for_white_cpa=1'
        cpa = '&cpa=real'

        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'redirect': {'url': Contains('/catalog--white-by-white-list')}})

        response = self.report.request_json(request + flag)
        self.assertFragmentIn(response, {'redirect': {'url': Contains('/catalog--white-by-white-list')}})

        response = self.report.request_json(request + cpa)
        self.assertFragmentIn(response, {'redirect': {'url': Contains('/catalog--white-by-white-list', 'cpa=1')}})

        response = self.report.request_json(request + cpa + flag)
        self.assertFragmentIn(response, {'redirect': {'url': Contains('/catalog--white-by-white-list', 'cpa=1')}})

    def test_force_white_on_redirects(self):
        """
        Под флагами market_force_white_on=60;market_blue_suggest_redirects_for_white_cpa=0
        на запросы с rgb=blue отдаются белые саджестовые редиректы.
        Под флагом market_force_white_on=60 на запросы с rgb=blue отдаются белые whitelist редиректы.
        """

        request = (
            'place=prime&cvredirect=1&text=blue on white cpa suggest&rgb=blue&cpa=real'
            '&rearr-factors=market_use_new_suggest_for_redirect=0'
        )
        white_suggest = '&rearr-factors=market_blue_suggest_redirects_for_white_cpa=0'
        force = '&rearr-factors=market_force_white_on=60'

        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'redirect': {'url': Contains('/catalog/blue-by-suggest')}})

        response = self.report.request_json(request + force)
        self.assertFragmentIn(response, {'redirect': {'url': Contains('/catalog/blue-by-suggest')}})

        response = self.report.request_json(request + force + white_suggest)
        self.assertFragmentIn(response, {'redirect': {'url': Contains('/catalog--white-by-suggest')}})

        request = 'place=prime&cvredirect=1&text=blue on white cpa white list&rgb=blue&cpa=real'
        white_suggest = '&rearr-factors=market_blue_suggest_redirects_for_white_cpa=0'
        force = '&rearr-factors=market_force_white_on=60'

        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'redirect': {'url': Contains('/catalog--white-by-white-list')}})

    def test_force_white_on_redirects_non_dummy_redirects(self):
        """
        Под флагами market_force_white_on=60;market_blue_suggest_redirects_for_white_cpa=0
        на запросы с rgb=blue отдаются белые саджестовые редиректы.
        Под флагом market_force_white_on=60 на запросы с rgb=blue отдаются белые whitelist редиректы.
        Все урлы нормально парсятся и в распаршенные редиректы добавляется cpa=1.
        """

        request = (
            'place=prime&cvredirect=1&text=blue on white cpa suggest&rgb=blue&cpa=real&non-dummy-redirects=1'
            '&rearr-factors=market_use_new_suggest_for_redirect=0'
        )
        white_suggest = '&rearr-factors=market_blue_suggest_redirects_for_white_cpa=0'

        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'redirect': {'params': {'slug': ['blue-by-suggest'], 'cpa': ['1']}}})

        response = self.report.request_json(request + white_suggest)
        self.assertFragmentIn(response, {'redirect': {'params': {'slug': ['white-by-suggest'], 'cpa': ['1']}}})

        request = 'place=prime&cvredirect=1&text=blue on white cpa white list&rgb=blue&cpa=real&non-dummy-redirects=1'
        white_suggest = '&rearr-factors=market_blue_suggest_redirects_for_white_cpa=0'

        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'redirect': {'params': {'slug': ['white-by-white-list'], 'cpa': ['1']}}})

        response = self.report.request_json(request + white_suggest)
        self.assertFragmentIn(response, {'redirect': {'params': {'slug': ['white-by-white-list'], 'cpa': ['1']}}})

    @classmethod
    def prepare_medicine_suggest_redirects(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=8475840,
                name='Товары для здоровья',
                children=[
                    HyperCategory(
                        hid=15754673,
                        name='Лекарственные препараты и БАДы',
                        children=[HyperCategory(hid=15756503, name='Средства для лечения боли')],
                    ),
                ],
            ),
        ]

        cls.suggester.on_request(part='нурофен').respond(
            suggestions=[
                Suggestion(
                    part='нурофен',
                    url='/catalog--boleutoliaiushchie-preparaty-v-ekaterinburge/72466/list?text=Нурофен&hid=15756503&suggest_type=recipe',
                ),
            ]
        )

    def test_disable_medicine_suggest_redirects(self):
        response = self.report.request_json(
            'place=prime&cvredirect=1&text=нурофен&rearr-factors=market_disable_medicine_recipe_suggest_redirect=0'
        )
        fragment = {
            "redirect": {
                "url": LikeUrl.of(
                    "/catalog--boleutoliaiushchie-preparaty-v-ekaterinburge/72466/list?text=Нурофен&hid=15756503&suggest_type=recipe"
                )
            }
        }
        self.assertFragmentIn(response, fragment)

        response = self.report.request_json('place=prime&cvredirect=1&text=нурофен')
        self.assertFragmentNotIn(response, fragment)

    @classmethod
    def prepare_category_redirect_to_non_leaf_category(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=738925,
                name='Корень',
                children=[
                    HyperCategory(
                        hid=738926,
                        name='Березовый ствол',
                        children=[
                            HyperCategory(hid=738928, name='Листики'),
                        ],
                    )
                ],
            ),
            HyperCategory(hid=738930, name='Офисная бумага'),
        ]
        cls.index.offers += [Offer(hid=738928, title="березовые листики"), Offer(hid=738930, title="бумажные листики")]

        # Здесь мы должны средиректить в нелиствую категорию
        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 738926).respond(0.8)
        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 738928).respond(-2)

    def test_category_redirect_by_formula_on_non_leaf_category(self):
        """Проверяем что мы можем по формуле средиректить на нелистовую категорию"""
        response = self.report.request_json('place=prime&text=листики&cvredirect=1')
        self.assertFragmentIn(
            response,
            {'redirect': {'params': {'hid': ['738926'], 'rt': ['9'], 'text': ['листики']}, 'target': 'search'}},
        )


if __name__ == '__main__':
    main()
