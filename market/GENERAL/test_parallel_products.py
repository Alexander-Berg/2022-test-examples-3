#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import json
from market.pylibrary.lite.matcher import NotEmptyList
import urllib

from core.types import (
    BlueOffer,
    Book,
    CardCategoryVendor,
    HyperCategory,
    MarketSku,
    MnPlace,
    Model,
    Offer,
    Region,
    RegionalModel,
    Shop,
    Stream,
    StreamName,
    Vendor,
)
from core.testcase import TestCase, main

from core.matcher import NotEmpty, NoKey, LikeUrl, Contains, Round, Greater, EmptyList


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.settings.use_apphost = True
        cls.settings.enable_experimental_panther = True
        cls.settings.default_search_experiment_flags += ['market_write_category_redirect_features=20']

        cls.index.regional_models += [
            RegionalModel(hyperid=500, rids=[213]),
            RegionalModel(hyperid=501, rids=[213]),
            RegionalModel(hyperid=502, rids=[202]),
        ]
        cls.index.shops += [Shop(fesh=1, category='A&B')]
        cls.index.models += [
            Model(hyperid=500, title='iphone 0', vendor_id=22, hid=21),
            Model(hyperid=501, title='iphone 1', vendor_id=22, hid=21),
            Model(hyperid=502, title='iphone 2', vendor_id=22, hid=21),
            Model(hyperid=503, title='phone 3', vendor_id=22, hid=21),
            Model(hyperid=504, title='toy phone', vendor_id=23, hid=24),
        ]
        cls.index.offers += [
            Offer(hyperid=500, title='iphone 10'),
            Offer(hyperid=501, title='iphone 11'),
            Offer(hyperid=502, title='iphone 12'),
            Offer(
                title='"название" xml_quote', descr='"описание" xml_quote', comment='"комментарий" к xml_quote', fesh=1
            ),
        ]
        cls.index.cards += [CardCategoryVendor(vendor_id=22, hid=21, hyperids=[500, 501, 502])]
        cls.index.hypertree += [
            HyperCategory(hid=21, name='telephoni'),
        ]
        cls.index.vendors += [Vendor(vendor_id=22, name='apple')]

    @classmethod
    def prepare_books_test(cls):

        for i in xrange(1, 10):
            cls.index.offers += [Offer(hyperid=1001, title='the best book ever')]

        cls.index.books += [Book(hid=30, hyperid=1001, title="the best book ever harry potter", isbn="5779310157")]

    # @see MARKETOUT-6043
    def test_contextual_wizard_for_error_message(self):
        # If bug is present, then fails in common.log with
        # (Error) [CODE 3012] ContextualWizard.cpp:789
        # market/report/src/Template.cpp:695: found fewer (0) sections [model]
        # than the template requires (1);
        self.report.request_bs('place=parallel_products&text=apple&rids=202')

    def test_has_books_in_result(self):
        """
        Check we do request book collection and render book model wizard
        if request contains ISBN.
        And we DO  NOT ask book collection if request in not ISBN (ISBN checksuming not passed)
        """
        result = self.report.request_bs('place=parallel_products&text=5779310157&ignore-mn=1&debug=1')
        self.assertTrue(result.contains("market_model")[0])
        self.assertTrue(result.contains("basesearch-book")[0])

        # request with invalid ISBN - last digit changed
        result = self.report.request_bs('place=parallel_products&text=5779310158&ignore-mn=1&debug=1')
        self.assertFalse(result.contains('basesearch-book')[0])

    def test_tbs_and_prun(self):
        response = self.report.request_bs('place=parallel_products&text=iphone&debug=1')
        self.assertTrue(response.contains('pron=pruncount334')[0])
        self.assertTrue(response.contains('pron=tbs17000')[0])
        self.assertTrue(response.contains('pron=versiontbs1')[0])
        self.assertTrue(response.contains('pron=mul_tbs_to_tbh10')[0])
        response = self.report.request_bs(
            'place=parallel_products&text=iphone&rearr-factors=prun-count=1000;tbs-value=1000;tbs-version=0;tbs-to-tbh-mul=2.5&debug=1'
        )
        self.assertTrue(response.contains('pron=pruncount667')[0])
        self.assertTrue(response.contains('pron=tbs1000')[0])
        self.assertTrue(response.contains('pron=versiontbs0')[0])
        self.assertTrue(response.contains('pron=mul_tbs_to_tbh2.5')[0])

    def test_wizard_rules_and_empty_qtree4market(self):
        '''
        Проверяем что запросы с wizard-rules без Market->qtree4market возвращают ошибку.
        '''
        self.error_log.expect(code=3661, message='Market->qtree4market is not present in ReqWizard answer')
        response = self.report.request_bs(
            'place=parallel_products&text=test&rearr-factors=market_parallel_wizard=1&wizard-rules={"Market":null}',
            strict=False,
        )
        self.assertTrue(str(response) == '')

    def test_product_request_classifier(self):
        """Проверка работы классификатора продуктовых запросов
        https://st.yandex-team.ru/MARKETOUT-18326
        https://st.yandex-team.ru/MARKETOUT-18418
        https://st.yandex-team.ru/MARKETOUT-19770
        https://st.yandex-team.ru/MARKETOUT-25442
        https://st.yandex-team.ru/MARKETOUT-28752
        https://st.yandex-team.ru/MARKETOUT-29097
        """
        # Формула классификатора продуктовых запросов возращает значение 0.7 (замокана по дефолту)
        request = 'place=parallel_products&text=iphone'

        # Проверяем дефолтную формулу - fml_formula_286493
        # Проверяем случай, когда значение формулы выше порога.
        # Должен вернуться непустой результат
        response = self.report.request_bs(request + '&reqid=1001&rearr-factors=market_product_request_threshold=0.1')
        self.assertTrue(str(response) != '')
        self.assertFragmentIn(response, {"market_factors": [{"ProductRequestClassifier": Round(0.7)}]})
        self.access_log.expect(reqid=1001, product_request=1)

        # Проверяем случай, когда значение формулы ниже порога.
        # Должен вернуться пустой результат.
        response = self.report.request_bs(request + '&reqid=1002&rearr-factors=market_product_request_threshold=0.9')
        self.assertTrue(str(response) == '')
        self.access_log.expect(reqid=1002, product_request=0)

        # Проверяем формулы
        for formula in ['fml_formula_468655', 'fml_formula_480200', 'fml_formula_MSA_866', 'fml_formula_MSA_957']:
            # Проверяем случай, когда значение формулы выше порога.
            # Должен вернуться непустой результат
            response = self.report.request_bs(
                request + '&rearr-factors=market_product_request_mn_algo={};'
                'market_product_request_threshold=0.1&debug=1'.format(formula)
            )
            self.assertTrue(str(response) != '')
            self.assertIn(
                'Product request classifier formula = {}, mnValue = 0.7, threshold = 0.1'.format(formula), str(response)
            )
            # Проверяем случай, когда значение формулы ниже порога.
            # Должен вернуться пустой результат.
            response = self.report.request_bs(
                request + '&rearr-factors=market_product_request_mn_algo={};'
                'market_product_request_threshold=0.9'.format(formula)
            )
            self.assertTrue(str(response) == '')

        # Если порог не задан, то используется порог по дефолту 0.24
        # Должен вернуться непустой результат
        response = self.report.request_bs(request + '&debug=1')
        self.assertTrue(str(response) != '')
        self.assertIn(
            'Product request classifier formula = fml_formula_286493, mnValue = 0.7, threshold = 0.24', str(response)
        )
        # Проверяем дефолтные порог и формулу для десктопа
        response = self.report.request_bs(request + '&rearr-factors=device=desktop&debug=1')
        self.assertTrue(str(response) != '')
        self.assertIn(
            'Product request classifier formula = fml_formula_MSA_866, mnValue = 0.7, threshold = 0.62', str(response)
        )

        # Порог равный 0 отключает классификатор
        # Должен вернуться непустой результат
        response = self.report.request_bs(request + '&rearr-factors=market_product_request_threshold=0&debug=1')
        self.assertTrue(str(response) != '')
        self.assertIn('Product request classifier formula disabled', str(response))

        # Если задана несуществующая формула, то значение формулы не вычисляется.
        # Должен вернуться непустой результат
        response = self.report.request_bs(
            request + '&rearr-factors=market_product_request_mn_algo=not_existing_formula;'
            'market_product_request_threshold=0.1&debug=1'
        )
        self.assertTrue(str(response) != '')
        self.assertIn(
            'Product request classifier formula &quot;not_existing_formula&quot; doesn&apos;t exist', str(response)
        )

        # Если задан флаг &debug=1, то выводится значение формулы.
        response = self.report.request_bs(request + '&rearr-factors=market_product_request_threshold=0.1&debug=1')
        self.assertIn(
            'Product request classifier formula = fml_formula_286493, mnValue = 0.7, threshold = 0.1', str(response)
        )

    def test_non_product_request_threshold_experiment_empty_request(self):
        """
        Проверка, что на пустом запросе с параметром non_product_request_threshold
        репорт не возвращает ошибку (https://st.yandex-team.ru/MARKETINCIDENTS-1562)
        """
        request = (
            'place=parallel_products&rearr-factors=non_product_request_threshold=0.03;market_parallel_wizard=1&text='
        )
        response = self.report.request_bs(request)
        self.assertEqual(200, response.code)

    def test_debug_category_factors(self):
        """Проверяем наличие категорийных факторов в дебаг-выдаче"""
        response = self.report.request_bs(
            'place=parallel_products&text=iphone&debug=1&rearr-factors=market_categ_wiz_with_redirect_formula=1'
        )
        self.assertIn('<categories-ranking><category-info>', str(response))

    def test_category_relevance_and_redirect_factors(self):
        """Независимо от количества подходящих категорий, всегда будут факторы RedirectScore и Relevance для трех.
        Поэтому истинное количество можно определить, отсеивая значения по-умолчанию, равные -10
        """

        response = self.report.request_bs(
            'place=parallel_products&text=iphone&rearr-factors=market_categ_wiz_with_redirect_formula=1'
        )
        # подходит всего одна категория
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "CategoriesRedirectScoreMedian": Round(-1.98751),
                        "CategoriesRedirectScoreMin": Round(-1.98751),
                        "CategoriesRedirectScoreMax": Round(-1.98751),
                        "CategoriesRelevanceMedian": Round(-1.98751),
                        "CategoriesRelevanceMin": Round(-1.98751),
                        "CategoriesRelevanceMax": Round(-1.98751),
                        "Category0RedirectScore": Round(-1.98751),
                        "Category1RedirectScore": -10,
                        "Category2RedirectScore": -10,
                        "Category0Relevance": Round(-1.98751),
                        "Category1Relevance": -10,
                        "Category2Relevance": -10,
                    },
                ]
            },
        )

        response = self.report.request_bs(
            'place=parallel_products&text=phone&rearr-factors=market_categ_wiz_with_redirect_formula=1'
        )
        # подходит две категории
        # TODO: MSSUP-763 - переход на новые факторы приводит к константному значению тестовой формулы по умолчанию
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "CategoriesRedirectScoreMedian": Round(-1.98751),
                        "CategoriesRedirectScoreMin": Round(-1.98751),
                        "CategoriesRedirectScoreMax": Round(-1.98751),
                        "CategoriesRelevanceMedian": Round(-1.98751),
                        "CategoriesRelevanceMin": Round(-1.98751),
                        "CategoriesRelevanceMax": Round(-1.98751),
                        "Category0RedirectScore": Round(-1.98751),
                        "Category1RedirectScore": Round(-1.98751),
                        "Category2RedirectScore": -10,
                        "Category0Relevance": Round(-1.98751),
                        "Category1Relevance": Round(-1.98751),
                        "Category2Relevance": -10,
                    },
                ]
            },
        )

    @classmethod
    def prepare_additional_blender_factors_data(cls):
        cls.settings.use_factorann = True

        cls.index.shops += [
            Shop(fesh=2, priority_region=213),
        ]

        cls.index.offers += [
            Offer(
                title='Детский паяльник',
                fesh=2,
                factor_streams=[
                    Stream(name=StreamName.NHOP, region=225, annotation='паяльники не только для пыток', weight=0.72),
                ],
            ),
        ]

    def test_additional_blender_factors(self):
        """Проверяем наличие дополнительных факторов для блендера
        в market_factors при включенных флагах
        (не проверяем DSSM, т.к. нет в LITE)
        https://st.yandex-team.ru/MARKETOUT-14356
        """
        response = self.report.request_bs(
            'place=parallel_products&text=iphone&rearr-factors=market_parallel_calc_factors_aggr=0'
        )
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "base.user_distance_to_moscow_max": NoKey("base.user_distance_to_moscow_max"),
                    },
                ]
            },
        )
        response = self.report.request_bs(
            'place=parallel_products&text=iphone&rearr-factors=market_calc_region_factors=1;market_parallel_calc_factors_aggr=0'
        )
        self.assertFragmentIn(response, {"market_factors": [{"base.user_distance_to_moscow_max": NotEmpty()}]})

        response = self.report.request_bs(
            'place=parallel_products&rids=213&text=паяльник+детский&rearr-factors=market_parallel_calc_factors_aggr=0'
        )
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "base.nhop_is_final_all_wcm_max_prediction_max": NoKey(
                            "base.nhop_is_final_all_wcm_max_prediction_max"
                        ),
                    },
                ]
            },
        )
        response = self.report.request_bs(
            'place=parallel_products&rids=213&text=паяльник+детский'
            '&rearr-factors=market_calc_text_machine_factors=1;market_parallel_calc_factors_aggr=0'
        )
        self.assertFragmentIn(
            response, {"market_factors": [{"base.nhop_is_final_all_wcm_max_prediction_max": NotEmpty()}]}
        )

    def test_json_format(self):
        """Проверяем как выглядит выдача в json-формате"""

        response = self.report.request_json(
            'place=parallel_products&rids=213&text=паяльник+детский&bsformat=2&debug=da&trace-wizards=1&debug-doc-count=10'
        )
        self.assertFragmentIn(
            response,
            {
                'wizards': {
                    'market_factors': NotEmpty(),
                    'market_offers_wizard': NotEmpty(),
                    'offerFactors': NotEmpty(),
                    'request_factors': NotEmpty(),
                },
                'documents': {'main_dsrcid': [{'entity': 'offer'}]},
                'debug': {
                    'brief': {'reqwizardText': NotEmpty(), 'counters': NotEmpty()},
                    'basesearch': {'documents': NotEmpty()},
                },
            },
        )

    def test_calc_query_classifier_factors(self):
        """Проверяем наличие факторов запросного классификатора
        при включенном флаге market_parallel_calc_query_classifier_factors
        https://st.yandex-team.ru/MARKETOUT-31933
        """
        query = 'place=parallel_products&text=iphone'

        response = self.report.request_bs(query)
        self.assertFragmentIn(
            response, {"market_factors": [{"QueryModelGroupClassifier": NoKey("QueryModelGroupClassifier")}]}
        )

        response = self.report.request_bs(query + '&rearr-factors=market_parallel_calc_query_classifier_factors=1')
        self.assertFragmentIn(response, {"market_factors": [{"QueryModelGroupClassifier": NotEmpty()}]})

    def test_use_text_search_factor_filler(self):
        """Проверяем наличие праймовых факторов
        в market_factors при включенном флаге market_parallel_use_text_search_factor_filler
        https://st.yandex-team.ru/MARKETOUT-27188
        """
        request = 'place=parallel_products&text=iphone'

        response = self.report.request_bs(request)
        self.assertFragmentIn(
            response, {"market_factors": [{"base.doc_embedding_hard2_0_max": NoKey("base.doc_embedding_hard2_0_max")}]}
        )

        response = self.report.request_bs(request + '&rearr-factors=market_parallel_use_text_search_factor_filler=1')
        self.assertFragmentIn(response, {"market_factors": [{"base.doc_embedding_hard2_0_max": NotEmpty()}]})
        self.assertFragmentIn(response, {"market_factors": [{"base.dssm_hard2_max": NotEmpty()}]})

    @classmethod
    def prepare_model_blender_factors_data(cls):
        cls.index.models += [
            Model(title='Model with factors 1'),
            Model(title='Model with factors 2'),
            Model(title='Model with factors 3'),
            Model(title='Model with factors 4'),
        ]

    def test_model_blender_factors(self):
        """Проверяем наличие модельных факторов для блендера
        в market_factors при включенном market_parallel_fill_model_factors
        https://st.yandex-team.ru/MARKETOUT-17713
        """
        query = 'place=parallel_products&text=model+with+factors&rearr-factors=market_parallel_calc_factors_aggr=0'

        response = self.report.request_bs(query)
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "base.model.bm15_w_body_max": NoKey("base.model.bm15_w_body_max"),
                    },
                ]
            },
        )
        response = self.report.request_bs(
            query + '&rearr-factors=market_parallel_fill_model_factors=1;market_parallel_calc_factors_aggr=0'
        )
        self.assertFragmentIn(response, {"market_factors": [{"base.model.bm15_w_body_max": NotEmpty()}]})

    @classmethod
    def prepare_doc_factors(cls):
        """Создаем модели и оферы для проверки их в выдаче
        для обучения формулы.
        """
        doc_count = 30
        for i in range(doc_count):
            ts = 600 + i
            cls.index.models += [
                Model(
                    hyperid=600 + i, title='learn formula model {}'.format(i), hid=60 + i, ts=ts, has_blue_offers=True
                )
            ]
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(0.1 + 0.01 * (doc_count - i))

        for i in range(doc_count):
            ts = 600 + doc_count + i
            cls.index.offers += [
                Offer(
                    title='learn formula offer {}'.format(i),
                    url='http://learn-formula.ru/{}'.format(i),
                    hid=60 + i,
                    waremd5='{:03}learn_formula_offeg'.format(i),
                    ts=ts,
                )
            ]
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(0.1 + 0.01 * (doc_count - i))

        for i in range(doc_count):
            ts = 600 + 2 * doc_count + i
            cls.index.mskus += [
                MarketSku(
                    title='special blue offer {}'.format(i),
                    sku=600 + i,
                    blue_offers=[BlueOffer(ts=ts, hid=60 + i, waremd5='Sku1Pr{0:04}-IiLVm1Goleg'.format(i))],
                )
            ]
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(0.1 + 0.01 * (doc_count - i))

        shop_count = 10
        for shop_id in range(shop_count):
            for i in range(doc_count / shop_count):
                cls.index.offers += [
                    Offer(
                        fesh=600 + shop_id,
                        offerid=600 + shop_id * 10 + i,
                        price=100,
                        title='Direct offer {} in shop {}'.format(i, shop_id + 600),
                        is_direct=True,
                        hid=60,
                        url='http://direct_shop_{}.ru/offers?id={}'.format(shop_id + 600, i),
                    )
                ]

    def test_doc_factors(self):
        """Проверяем, что в дебаге добавляются топ оферов, топ моделей, топ синих моделей, топ схлопнутых моделей
        с факторами для обучения формулы. Количесто можно задавать
        через &debug-doc-count=, по умолчанию 20 для оферов, 10 для моделей.
        """
        request = 'place=parallel_products&text=learn+formula&debug=da'

        # 1. offerFactors, modelFactors, blueModelFactors, hyperModelFactors
        for offer_count, model_count, hyper_model_count, numdoc_str in [
            (20, 10, 10, ''),
            (20, 20, 20, '&debug-doc-count=20'),
            (5, 5, 10, '&debug-doc-count=5'),
            (1, 1, 10, '&debug-doc-count=1'),
            (1, 20, 10, '&rearr-factors=market_parallel_models_max_count=20&debug-doc-count=1'),
            (1, 1, 20, '&rearr-factors=market_parallel_collapsing_count=20&debug-doc-count=1'),
        ]:
            # bsformat=1
            response = self.report.request_bs(request + numdoc_str)
            self.assertFragmentIn(
                response,
                {
                    'offerFactors': [
                        {
                            'title': 'learn formula offer {}'.format(i),
                            'url': LikeUrl.of('http://learn-formula.ru/{}'.format(i)),
                            'hid': 60 + i,
                            'factors': NotEmpty(),
                        }
                        for i in range(offer_count)
                    ],
                    'modelFactors': [
                        {
                            'title': 'learn formula model {}'.format(i),
                            'url': LikeUrl.of('http://market.yandex.ru/product/{}'.format(600 + i)),
                            'hid': 60 + i,
                            'factors': NotEmpty(),
                            'model_id': 600 + i,
                        }
                        for i in range(model_count)
                    ],
                    'hyperModelFactors': [
                        {
                            'title': 'learn formula model {}'.format(i),
                            'url': LikeUrl.of('http://market.yandex.ru/product/{}'.format(600 + i)),
                            'hid': 60 + i,
                            'factors': NotEmpty(),
                            'model_id': 600 + i,
                        }
                        for i in range(hyper_model_count)
                    ],
                },
                allow_different_len=False,
            )

            # bsformat=7
            response = self.report.request_bs_pb(request + numdoc_str)
            searcher_props = response.get_searcher_props()

            self.assertIn('Market.Debug.offerFactors', searcher_props)
            offer_factors = json.loads(searcher_props['Market.Debug.offerFactors'])
            self.assertEqual(offer_count, len(offer_factors))
            for i in range(offer_count):
                self.assertDictContainsSubset(
                    {
                        'title': 'learn formula offer {}'.format(i),
                        'url': 'http://learn-formula.ru/{}'.format(i),
                        'hid': 60 + i,
                        'ware_md5': '{:03}learn_formula_offeg'.format(i),
                    },
                    offer_factors[i],
                )
                self.assertIn('factors', offer_factors[i])

            self.assertIn('Market.Debug.modelFactors', searcher_props)
            model_factors = json.loads(searcher_props['Market.Debug.modelFactors'])
            self.assertEqual(model_count, len(model_factors))
            for i in range(model_count):
                self.assertDictContainsSubset(
                    {
                        'title': 'learn formula model {}'.format(i),
                        'url': 'http://market.yandex.ru/product/{}'.format(600 + i),
                        'hid': 60 + i,
                        'model_id': 600 + i,
                    },
                    model_factors[i],
                )
                self.assertNotIn('ware_md5', model_factors[i])
                self.assertIn('factors', model_factors[i])

            self.assertIn('Market.Debug.hyperModelFactors', searcher_props)
            hyper_model_factors = json.loads(searcher_props['Market.Debug.hyperModelFactors'])
            self.assertEqual(hyper_model_count, len(hyper_model_factors))
            for i in range(hyper_model_count):
                self.assertDictContainsSubset(
                    {
                        'title': 'learn formula model {}'.format(i),
                        'url': 'http://market.yandex.ru/product/{}'.format(600 + i),
                        'hid': 60 + i,
                        'model_id': 600 + i,
                    },
                    hyper_model_factors[i],
                )
                self.assertNotIn('ware_md5', hyper_model_factors[i])
                self.assertIn('factors', hyper_model_factors[i])

        # 2. blueExtraOfferFactors
        request = (
            'place=parallel_products&text=special+blue+offer&debug=da&rearr-factors=market_cpa_offers_incut_count=0;'
        )
        for blue_extra_offer_count, numdoc_str in [
            (3, 'market_offers_wizard_blue_offers_count=3'),
            (10, 'market_offers_wizard_blue_offers_count=3&debug-doc-count=10'),
            (20, 'market_offers_wizard_blue_offers_count=20&debug-doc-count=10'),
        ]:
            # bsformat=1
            response = self.report.request_bs(request + numdoc_str)
            self.assertFragmentIn(
                response,
                {
                    'blueExtraOfferFactors': [
                        {
                            'title': 'special blue offer {}'.format(i),
                            'url': LikeUrl.of(
                                'https://pokupki.market.yandex.ru/product/{0}?offerid=Sku1Pr{1:04}-IiLVm1Goleg'.format(
                                    600 + i, i
                                )
                            ),
                            'hid': 60 + i,
                            'factors': NotEmpty(),
                        }
                        for i in range(blue_extra_offer_count)
                    ],
                },
                allow_different_len=False,
            )

            # bsformat=7
            response = self.report.request_bs_pb(request + numdoc_str)
            searcher_props = response.get_searcher_props()

            self.assertIn('Market.Debug.blueExtraOfferFactors', searcher_props)
            blue_extra_offer_factors = json.loads(searcher_props['Market.Debug.blueExtraOfferFactors'])
            self.assertEqual(blue_extra_offer_count, len(blue_extra_offer_factors))
            for i in range(blue_extra_offer_count):
                self.assertDictContainsSubset(
                    {
                        'title': 'special blue offer {}'.format(i),
                        'url': 'https://pokupki.market.yandex.ru/product/{0}?offerid=Sku1Pr{1:04}-IiLVm1Goleg'.format(
                            600 + i, i
                        ),
                        'hid': 60 + i,
                        'ware_md5': 'Sku1Pr{:04}-IiLVm1Goleg'.format(i),
                    },
                    blue_extra_offer_factors[i],
                )
                self.assertIn('factors', blue_extra_offer_factors[i])

        # 3. directOffersFactors
        request = 'place=parallel_products&text=direct&debug=da&rearr-factors=market_enable_direct_offers_wizard=1'
        response = self.report.request_bs(request)
        self.assertFragmentIn(
            response,
            {
                'directOffersFactors': [
                    {
                        'title': 'Direct offer {} in shop {}'.format(id, shop_id),
                        'url': LikeUrl.of('http://direct_shop_{}.ru/offers?id={}'.format(shop_id, id)),
                        'hid': 60,
                        'factors': NotEmpty(),
                    }
                    for id in range(3)
                    for shop_id in range(600, 610)
                ],
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_disabled_doc_factors(cls):
        cls.index.mskus += [MarketSku(title='disabled sku', sku=600, blue_offers=[BlueOffer(ts=601, hid=21)])]
        cls.index.offers += [Offer(title='disabled', is_direct=True)]

    def test_disabled_doc_factors(self):
        """
        Проверяем отключение факторов документов из дебажной выдачи при установке флага market_disable_document_factors=1.
        см. https://st.yandex-team.ru/MARKETOUT-38933
        """
        factors_types = [
            "offerFactors",
            "modelFactors",
            "blueExtraOfferFactors",
            "hyperModelFactors",
            "directOffersFactors",
        ]
        request = 'place=parallel_products&text=iphone disabled&debug=da&rearr-factors=market_enable_direct_offers_wizard=1;market_disable_document_factors={}'
        response = self.report.request_bs(request.format(1))
        self.assertFragmentIn(response, {factor_type: [{"factors": EmptyList()}] for factor_type in factors_types})
        response = self.report.request_bs(request.format(0))
        self.assertFragmentIn(response, {factor_type: [{"factors": NotEmptyList()}] for factor_type in factors_types})

    @classmethod
    def prepare_use_correct_aggrs(cls):
        """Создаем оферы и книги (категория 90829).
        Некоторые книги добавляем в оффлайн-магазин
        (будут выфильтрованы на базовом ПП).
        """
        cls.index.shops += [
            Shop(fesh=3, priority_region=213, online=False),
        ]

        cls.index.offers += [
            Offer(title='simple offer 1'),
            Offer(title='simple offer 2'),
            Offer(title='simple book 1', hid=90829),
            Offer(title='simple book 2', hid=90829),
            Offer(title='simple book 3', hid=90829, fesh=3),
            Offer(title='simple book 4', hid=90829, fesh=3),
        ]

    def test_response_error(self):
        """Проверяем, что репорт возвращает ошибки парсинга CGI
        в запрашиваемом формате, а не в XML.
        """
        response = self.report.request_xml('place=parallel_products&text=%F0', strict=False)
        self.assertFragmentIn(
            response,
            '''
            <error code="1010">
            </error>''',
        )

        response = self.report.request_json('place=parallel_products&text=%F0', strict=False)
        self.assertFragmentIn(
            response,
            {
                'error': {
                    'code': 1010,
                    'description': Contains('Invalid symbol(s) in text query (IsUtf==false)'),
                }
            },
        )

        response = self.report.request_bs_pb('place=parallel_products&text=%F0', strict=False)
        report = response.get_report()
        self.assertEqual(report.ErrorInfo.GotError, 1)
        self.assertEqual(report.ErrorInfo.Code, 1010)
        self.assertIn('Invalid symbol(s) in text query (IsUtf==false)', report.ErrorInfo.Text)

        response = self.report.request_plain('place=parallel_products&text=%F0&bsformat=7&hr=json', strict=False)
        response_json = json.loads(str(response))
        self.assertEqual(response_json['ErrorInfo']['GotError'], 1)
        self.assertEqual(response_json['ErrorInfo']['Code'], 1010)
        self.assertIn('Invalid symbol(s) in text query (IsUtf==false)', response_json['ErrorInfo']['Text'])

        self.error_log.expect(code=1010, message='Invalid symbol(s) in text query').times(4)

    @classmethod
    def prepare_offer_incut_autobroker(cls):
        for seq in range(1, 20):
            cls.index.shops += [Shop(fesh=1000 + seq, priority_region=213)]
            cls.index.offers += [
                Offer(
                    title='kawabunga',
                    fesh=1000 + seq,
                    bid=20 + 10 * seq,
                    waremd5='{:02}DMtqGjKyWUCE8NpmhhXQ'.format(seq),
                ),
            ]

    def test_offer_incut_autobroker(self):
        """
        Проверяем, что автоброкер во врезке работает корректно.
        """
        _ = self.report.request_bs_pb('place=parallel_products&text=kawabunga&rids=213')
        self.show_log.expect(pp=405, bid=210, click_price=201)

    def test_offer_incut_feature_log(self):
        _ = self.report.request_bs_pb(
            'place=parallel_products&text=kawabunga&rids=213&reqid=123'
            '&rearr-factors=market_parallel_feature_log_rate=1;market_offers_wizard_incut_url_type=External'
        )

        for i in range(9):
            self.feature_log.expect(
                pp=405,
                position=i + 1,
                offer_number=9,
                ware_md5='{:02}DMtqGjKyWUCE8NpmhhXQ'.format(19 - i),
                shop_id=1000 + 19 - i,
                shop_priority_region=213,
                req_id=123,
                other={'request_all_lower': 1, 'has_all_words_ex_full': 1},
            )

        self.feature_log.expect(from_blue_stats=1).never()

    def test_offer_incut_bg(self):
        dssm_markup = {
            "Market": {
                "qtree4market": "cHicrVVBaxNBFH5vshu30xxCa2tYFGMUGouFoJfQg5XaQwUPIoiyF9skS7PaJpoWjD0FbSVVlKLgQQpKtVALprW1oNBKQRBRD1s8Ct70F3ivs7OzabKbpkbMJbMz733vve99b4ZepAEFghBSwhAlMWjxG9fS2YyuQgQ64Th0BxQ_OwV2CjE4Bf1wDi7BAKRvnZhGeIIwi8JjGWEdgf0-IZiY6vzio_0CWxLYUIEKXtQ0OpDgAlN_Ew5GgiREdgGT--VdwCaQDojMmsLErjqZzYzpeQ7pjwzldD2zU-0CWHiw8m1zV5AYxLG33YphYUYxhoI3HIcCAoNQi0hTddJQIsnhwdFRI_m3iZQdGk4lYmdBwpKFH8M42vuWp73q_In_sZsPkF5xVd48kk3pw5eT2eFsTnThRtoY0zl6847olW5WK7hPw_Vv18ltZ4iCzFjoWrg8IrZProuKgxCyA4jDeVIsaTiX8FsBoq0Jhf2TjtGoL415kjvsMfcV50oaOObcqMdjhEWNzJW8mIk9QaKivQAVO0SUQ16AgivGaa_JlEaerWj4uhyGY6Y4ONpRmlg4n5EZsuOME4Vob4hSLBSw7cxid5_6lNBB1w1CzSXz3eaE-d5cV_FoBKNCI3KNLr74tUIdmVT41ergIvKuVFiJzjx3OjOFtOI0xCNxvjc-a2S-5CXUXGIV2qu18uotXxGVbE46K3OVrSijRGLAy3wXyz6W5W1nb_MeIwryyIgSiopIFjlR0D5IyrRFG2zc7FNLUnmcQEzAv99na0jPunqAhorhMhzWYN4MOHho1KI7wdlGwy3_KEWjTC25_7Ba-YyCEZ1TQHL7qWSM6FdDIORmydHH5ajkj1lKEvfgHUIvuNKXnTcIw3XE8_Vlu1ODXOsJ4nXM2rKRa87ySSo7IwGNjp1VYq_Hv7GRAnukhFKCytaWL1goSPwmlmMV95JQkaR9F8PHVfSRlF8Qh7nA9gCYq_Uf8fTM4x-viMNglWMtIr_ZRFbZuSdwst4ELji8-hqevtbGpi93hFalWSVBEBIEz4xK7OsuHqT8q2Wfgmqr4m-7vjfTc2B-wYyHocuihNn_AXmc3Z4,"  # noqa
            }
        }

        response = self.report.request_bs(
            "place=parallel_products&text=iphone&debug=1&rearr-factors=market_parallel_wizard=1&wizard-rules={}&relev-factors=bgfactors=FeXQkj-1AT0Klz_AAQGSCgUVPQoHQA,,".format(
                urllib.quote(json.dumps(dssm_markup))
            )
        )
        self.assertFragmentIn(
            response,
            {
                "request_factors": {
                    "factors": {
                        "bg.dmoz_query_themes": 1.146999955,
                        "bg.non_commercial_query": 1,
                        "bg.query_commerciality_mx": 1.179999948,
                        "bg.random_log_word_factors_random_log_word_max_is_lj": 2.109999895,
                    }
                }
            },
        )

    @classmethod
    def prepare_fixed_qtree_on_misspell(cls):
        cls.index.offers += [Offer(title="kiyanka rezinovaya")]

    def test_fixed_qtree_on_misspell(self):
        """В случае наличия в ответе от визарда информации об опечатке используем Misspell->MarketQtree вместо Market->qtree4market.
        Работает под флагом market_parallel_use_fixed_tree_on_misspell и при уровне надежности опечатки не ниже заданного в market_parallel_misspell_threshold порога.

        https://st.yandex-team.ru/MARKETOUT-17183
        """

        def make_request(text, qtree_markup, use_fixed_tree=True, misspell_threshold=None):
            request = 'place=parallel_products&text={0}'.format(text)
            request += '&wizard-rules={0}'.format(json.dumps(qtree_markup))

            request += '&rearr-factors=market_parallel_wizard=1'
            if use_fixed_tree:
                request += ';market_parallel_use_fixed_tree_on_misspell=1'
            if misspell_threshold:
                request += ';market_parallel_misspell_threshold={0}'.format(misspell_threshold)

            return request

        text = 'kyianka+reiznovaya'

        # дерево для запроса 'kyianka reiznovaya'
        bad_tree = "cHicjZCxS8NAGMXfd2nNcVYJyRJuCll6iEh0Kk7FqThI6SR1McVCSqFCBGm72LF0EgsO4iR0tIO7u6N_gbv_g4MXm5TQCnrLHfe99-N9TxyLEocFFx5TLIANCR87OMBh9g-FANVirVjHKc4R0S3hgfBEeCG8EvR5I7zThfwicZbiuLYlOLM76IS9bijJW2LNHBY1JNjo8f7jmWXgzLTCD1ChozviZEFmEh-KAqpP2SJaXBXZxCU9QQUNGjfZbN7a0C4op8X1zcpXymiZFpO0eEBSWRkR9Vm8t05g43mTZuuERD9knI0IOrr8JBGurC_idmfYu7wOB6Hulby0AfFXAznfbyUMfzrIiVZr8AXPhi7SLbAIPgiVE_H-brwvcoCl6n-7-lv8xhqRyzxDFQMkofRgQo4o6P2Zvcl5w-RkGyf19oRK6W-Bb8eJ-xtcmnXl"  # noqa
        # дерево для запроса 'kiyanka rezinovaya'
        good_tree = "cHicjZCxS8NAGMXfd0nNcRYJ7RJuCll6OEh0Kk7FqThI6SR1McFCSqFCBGk6dSydRMFBnISOdnB3d_QvcPd_EOrFJiW0gt5yx733_XjfE8eizGHDgcsU81GBhIddHOAw_4eCj0apWWrhFOeI6IbwQHgivBBeCfq8Ed7pQn6ROMtwXI-lOKvfS4JBP5DkrrBWAYsmUmz0eP_xzHJwPrTG91Gno1viZEPmFg-KfGrdsWW0uCFyxSGtoI42TTpsNg-39BRUNeT6ZrUrZYSWzSQtH5BUU0ZEQxbvbRLYZN6h2SYh9Y8YZ2OCji4_SQRr64u4O-oNLq-DJNC9kps1IP5qoDD3Wwmjnw4KpvUaPMFz0UG2BZbBk0BVIz5EvC8KgJXrf7t6Nl8sDHs8Nh3mmqrkI82ltSlVhakrYJVtztsWp4px0upOqZz9mnwnTgHfEGt3wg,,"  # noqa

        # По запросу с деревом, соответствующим тексту с опечаткой, оффер не находится
        # и колдунщик не формируется
        qtree_markup = {"Market": {"qtree4market": bad_tree}}
        response = self.report.request_bs(make_request(text, qtree_markup))
        self.assertFragmentNotIn(response, {"market_offers_wizard": []})

        # При добавлении в разметку дерева исправленного запроса и информации об опечатке
        # c уровнем надежности не меньше порога поиск использует исправленное дерево
        # и колдунщик формируется
        qtree_markup = {
            "Market": {
                "qtree4market": bad_tree,
            },
            "Misspell": {
                "IsMisspell": "1",
                "Relev": "5999",
                "MarketQtree": good_tree,
            },
        }
        response = self.report.request_bs(make_request(text, qtree_markup, misspell_threshold=5999))
        self.assertFragmentIn(response, {"market_offers_wizard": []})

        # При повышении порога используется дерево с опечаткой и колдунщика нет
        response = self.report.request_bs(make_request(text, qtree_markup, misspell_threshold=6000))
        self.assertFragmentNotIn(response, {"market_offers_wizard": []})

        # При отключении флага market_parallel_use_fixed_tree_on_misspell тоже используется дерево с опечаткой и колдунщика нет
        response = self.report.request_bs(
            make_request(text, qtree_markup, use_fixed_tree=False, misspell_threshold=5999)
        )
        self.assertFragmentNotIn(response, {"market_offers_wizard": []})

        # При отсутствии исправленного дерева Misspell->MarketQtree отката на Market->qtree4market
        # не происходит и в лог пишется соответствующая ошибка, колдунщика нет
        qtree_markup = {
            "Market": {
                "qtree4market": bad_tree,
            },
            "Misspell": {
                "IsMisspell": "1",
                "Relev": "5000",
            },
        }
        response = self.report.request_bs(make_request(text, qtree_markup))
        self.error_log.expect(code=3661, message='Misspell->MarketQtree is not present in ReqWizard answer')
        self.assertFragmentNotIn(response, {"market_offers_wizard": []})

    def test_search_in_top_kishka(self):
        """Проверяем как взаимодействуют флаги
        parallel_allow_panther
        panther_parallel_tpsz
        parallel_smm
        """

        _ = self.report.request_bs(
            'place=parallel_products&text=kawabunga&rids=213&debug=da&trace_wizard=1'
            '&rearr-factors=parallel_allow_panther=0;'
        )

        _ = self.report.request_bs('place=parallel_products&text=kawabunga&rids=213&debug=da&trace_wizard=1')

        _ = self.report.request_bs(
            'place=parallel_products&text=kawabunga&rids=213&debug=da&trace_wizard=1' '&rearr-factors=parallel_smm=1.0'
        )

        self.access_log.expect(total_documents_processed=19).times(3)

    def test_search_in_top_kishka_2(self):
        """Пантерный индекс + smm приводит к уменьшению обрабатываемых документов

        Этот тест не работает с rearr-factors=use_external_panther_docs=1.
        Точнее не работает smm<0.3 c внешней пантерой.
        Для исправления надо разбить кишку на 2 части.
        https://a.yandex-team.ru/arc/trunk/arcadia/search/panther/runtime/searcher/panther_searcher.cpp?blame=true&rev=r7725373#L351
        Первая с IsPruned() = false, вторая с IsPruned() = true
        Делать это не стал, так как при работе с внешней пантеров лучше делать короткие кишки
        на стороне П-шки, что бы меньше нагружать сеть
        """

        _ = self.report.request_bs(
            'place=parallel_products&text=kawabunga&rids=213&debug=da&trace_wizard=1' '&rearr-factors=parallel_smm=0.1'
        )

        self.access_log.expect(total_documents_processed=10).times(1)

    @classmethod
    def prepare_bids_and_formula_in_output(cls):
        cls.index.offers += [
            Offer(title="talkysh kaleve", bid=40, vbid=6),
        ]

    def test_bids_and_formula_in_output(self):
        """Информация о ставках и значении формулы в выдаче

        https://st.yandex-team.ru/MARKETOUT-17325
        """

        response = self.report.request_bs_pb('place=parallel_products&text=talkysh+kaleve&debug=da')
        searcher_props = response.get_searcher_props()

        offer_factors = json.loads(searcher_props['Market.Debug.offerFactors'])

        # Инфа о ставках
        self.assertDictContainsSubset(
            {
                # https://st.yandex-team.ru/MARKETOUT-17325
                'bid': 40,
                'min_bid': 1,
                # https://st.yandex-team.ru/MARKETOUT-23896
                'full_bid_info': 'MinimalBid:1 RankingMinimalBid:1 ShopBid:40 VendorBid:0 MinimalVendorBid:0 RankingMinimalVendorBid:0 BidType:mbid Fee:0 MinimalFee:0 OfferVendorFee:0 ReservePriceFee:0 UeToPrice:0 NeedLocalBoost:0',  # noqa
            },
            offer_factors[0],
        )

        # Инфа формуле и значении
        self.assertIn('ranked_with', offer_factors[0])
        self.assertIn('formula', offer_factors[0]['ranked_with'][0])
        self.assertIn('value', offer_factors[0]['ranked_with'][0])

    @classmethod
    def prepare_fixed_base_formula(cls):
        for i in xrange(1, 12):
            ts = 17407 + i
            cls.index.offers.append(
                Offer(
                    title='gvozdoder {}'.format(i),
                    ts=ts,
                    url='http://gvozdoder-shop.ru/{}'.format(i),
                    waremd5='3QVfU6RXAv4F5EnDCsgN-Q' if i == 11 else None,
                )
            )
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(i * 0.01)
            cls.matrixnet.on_place(MnPlace.FIXED_PARALLEL_BASE_FORMULA, ts).respond(0.23 - i * 0.01)

    def test_fixed_base_formula(self):
        """Считаем факторы-статистики по фиксированной формуле
        https://st.yandex-team.ru/MARKETOUT-17407
        """
        # market_offers_incut_threshold=0.0 нужен, чтобы врезка не отфильтровывалась по топ-4
        response = self.report.request_bs(
            'place=parallel_products&text=gvozdoder&rearr-factors=market_offers_incut_threshold=0.0;'
        )

        # Во врезку попадут офферы gvozdoder 3-11
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "title": {"text": {"__hl": {"text": "gvozdoder 11", "raw": True}}}
                                },  # BASE_SEARCH = 0.11, FIXED_PARALLEL_BASE_FORMULA = 0.12
                                {
                                    "title": {"text": {"__hl": {"text": "gvozdoder 10", "raw": True}}}
                                },  # BASE_SEARCH = 0.10, FIXED_PARALLEL_BASE_FORMULA = 0.13
                                {
                                    "title": {"text": {"__hl": {"text": "gvozdoder 9", "raw": True}}}
                                },  # BASE_SEARCH = 0.09, FIXED_PARALLEL_BASE_FORMULA = 0.14
                                {
                                    "title": {"text": {"__hl": {"text": "gvozdoder 8", "raw": True}}}
                                },  # BASE_SEARCH = 0.08, FIXED_PARALLEL_BASE_FORMULA = 0.15
                                {
                                    "title": {"text": {"__hl": {"text": "gvozdoder 7", "raw": True}}}
                                },  # BASE_SEARCH = 0.07, FIXED_PARALLEL_BASE_FORMULA = 0.16
                                {
                                    "title": {"text": {"__hl": {"text": "gvozdoder 6", "raw": True}}}
                                },  # BASE_SEARCH = 0.06, FIXED_PARALLEL_BASE_FORMULA = 0.17
                                {
                                    "title": {"text": {"__hl": {"text": "gvozdoder 5", "raw": True}}}
                                },  # BASE_SEARCH = 0.05, FIXED_PARALLEL_BASE_FORMULA = 0.18
                                {
                                    "title": {"text": {"__hl": {"text": "gvozdoder 4", "raw": True}}}
                                },  # BASE_SEARCH = 0.04, FIXED_PARALLEL_BASE_FORMULA = 0.19
                                {
                                    "title": {"text": {"__hl": {"text": "gvozdoder 3", "raw": True}}}
                                },  # BASE_SEARCH = 0.03, FIXED_PARALLEL_BASE_FORMULA = 0.20
                            ]
                        }
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Статистики будут считаться по офферам gvozdoder 2-11
        # Соответствующие им значения FIXED_PARALLEL_BASE_FORMULA по убыванию: 0.21, 0.20, 0.19, 0.18, 0.17, 0.16, 0.15, 0.14, 0.13, 0.12
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "WinningFourWeight": 0.7400000095,  # 0.21 + 0.20 + 0.19 + 0.18
                        "MaxRelevance": 0.200000003,
                        "MinRelevance": 0.1199999973,
                        "MedianRelevance": 0.1599999964,  # (0.16 + 0.17) / 2
                    },
                ]
            },
        )

    def test_current_base_formula(self):
        """Проверяем вывод значения суммы ТОП-4 офферов по базовой формуле
        https://st.yandex-team.ru/MARKETOUT-26431
        """
        # market_offers_incut_threshold=0.0 нужен, чтобы врезка не отфильтровывалась по топ-4
        response = self.report.request_bs(
            'place=parallel_products&text=gvozdoder&rearr-factors=market_offers_incut_threshold=0.0;'
        )

        # Статистики будут считаться по офферам gvozdoder 2-11
        # Соответствующие им значения BASE_SEARCH формулы по убыванию: 0.11, 0.10, 0.09, 0.08, 0.07, 0.06, 0.05, 0.04, 0.03, 0.02, 0.01
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "WinningFourWeightCurrent": Round(0.38),  # 0.11 + 0.10 + 0.09 + 0.08
                    }
                ]
            },
        )

    def test_request_factors(self):
        """Проверка вывода запросных факторов в выдаче на параллельном
        https://st.yandex-team.ru/MARKETOUT-17875
        """
        response = self.report.request_bs('place=parallel_products&text=kiyanka+rezinovaya&debug=1')
        self.assertFragmentIn(response, {"request_factors": {"text": "kiyanka rezinovaya", "factors": NotEmpty()}})

    def test_request_factors_feature_log(self):
        """Проверка вывода запросных факторов и времени выполнения запроса в feature.log
        https://st.yandex-team.ru/MARKETOUT-18930
        https://st.yandex-team.ru/MARKETOUT-19924
        """
        for _ in range(20):  # Факторы пишутся только на каждый 10 запрос
            self.report.request_bs_pb('place=parallel_products&text=iphone&reqid=12345')
        self.feature_log.expect(
            document_type=9,
            req_id=12345,
            other={'elapsed': Greater(0), 'factors': NotEmpty(), 'factors_names': NotEmpty()},
        ).times(2)

    @classmethod
    def prepare_reject_stop_request(cls):
        """Подготовка для проверки неответа на стоп-запрос
        https://st.yandex-team.ru/MARKETOUT-27716
        https://st.yandex-team.ru/MARKETOUT-40384
        """
        cls.index.parallel_stop_queries += ['маркет', 'запрещенный запрос']
        cls.index.parallel_block_queries += ['маркет', 'запрещенный запрос']

    def test_reject_stop_request(self):
        """Проверка неответа на запросы из стоп-списка parallel-stop-queries.dat
        https://st.yandex-team.ru/MARKETOUT-27716
        https://st.yandex-team.ru/MARKETOUT-40384
        """
        response = self.report.request_bs('place=parallel_products&text=маркет')
        self.assertFragmentNotIn(response, {})

        # несколько слов
        response = self.report.request_bs('place=parallel_products&text=запрещенный+запрос')
        self.assertFragmentNotIn(response, {})

        # работает только на точное соответствие запросу
        response = self.report.request_bs('place=parallel_products&text=маркет+яндекс')
        self.assertFragmentIn(response, {})
        response = self.report.request_bs('place=parallel_products&text=запрещенный')
        self.assertFragmentIn(response, {})

    def test_stop_list_disabling(self):
        """Проверка работы флага market_parallel_stop_list_disable
        который отключает стоп листы на параллельном
        https://st.yandex-team.ru/MARKETOUT-29940
        """
        response = self.report.request_bs('place=parallel_products&text=запрещенный+запрос')
        self.assertFragmentNotIn(response, {})

        response = self.report.request_bs(
            'place=parallel_products&text=запрещенный+запрос&rearr-factors=market_parallel_stop_list_disable=1'
        )
        self.assertFragmentIn(response, {})

    @classmethod
    def prepare_family_banned(cls):
        cls.index.offers += [
            Offer(title='porn offer'),
            Offer(title='usual offer'),
            Offer(title='drug offer'),
            Offer(title='игрушечная херня'),
        ]
        cls.index.family_stop_queries += ["porn", "drug", "херня"]

    def test_family_banned_requests(self):
        '''
        Проверяем, что при переданном флаге family=2 запросы, помеченные как стоп- для family возвращают
        пустую выдачу
        '''

        # слово из запроса есть в списке, пустой ответ
        response = self.report.request_bs_pb('place=parallel_products&text=porn+stuff&family=2')
        self.assertFragmentNotIn(response, {})

        # словоформа из запроса есть в списке, пустой ответ
        response = self.report.request_bs_pb('place=parallel_products&text=купить+херню&family=2')
        self.assertFragmentNotIn(response, {})

        # нет family слов в запросе, есть ответ
        response = self.report.request_bs_pb('place=parallel_products&text=usual&family=2')
        self.assertFragmentIn(response, {})

        # family отключен, не смотрим список, есть ответ
        response = self.report.request_bs_pb('place=parallel_products&text=some+drug')
        self.assertFragmentIn(response, {})

    @classmethod
    def prepare_reject_heavy_request(cls):
        """Подготовка для проверки неответа на тяжелые запросы
        https://st.yandex-team.ru/MARKETOUT-18861
        """
        cls.index.parallel_final_stop_words += ['что', 'кто']

    def test_reject_heavy_request(self):
        """Проверка неответа на тяжелые запросы под флагом market_parallel_reject_heavy_requests
        https://st.yandex-team.ru/MARKETOUT-18861
        https://st.yandex-team.ru/MARKETOUT-19772
        """
        # Под флагом market_parallel_reject_heavy_requests если все слова запроса после вырезания стоп слов
        # содержатся в списке parallel_final_stop_words, то нет ответа
        for flag in [1, 2]:
            response = self.report.request_bs(
                'place=parallel_products&text=что+кто'
                '&rearr-factors=market_parallel_reject_heavy_requests={}'.format(flag)
            )
            self.assertFragmentNotIn(response, {})

        # Флаг market_parallel_reject_heavy_requests=0 отключает проверку, ответ есть
        response = self.report.request_bs(
            'place=parallel_products&text=что+кто' '&rearr-factors=market_parallel_reject_heavy_requests=0'
        )
        self.assertFragmentIn(response, {})

        # По дефолту флаг market_parallel_reject_heavy_requests включен, нет ответа
        response = self.report.request_bs('place=parallel_products&text=что+кто')
        self.assertFragmentNotIn(response, {})

        # По дефолту флаг market_parallel_reject_heavy_requests включен,
        # не все слова запроса содержатся в списке parallel_final_stop_words, ответ есть
        response = self.report.request_bs('place=parallel_products&text=что+кто+киянка')
        self.assertFragmentIn(response, {})

        # Проверка по количеству слов
        flags = [
            ('market_parallel_max_word_count=5', 5),
            ('market_parallel_max_word_count=20', 20),
            ('', 10),
        ]  # Дефолтное значение

        for flag, count in flags:
            # Если запрос после вырезания стоп слов содержит слов больше,
            # чем задано флагом market_parallel_max_word_count, то нет ответа
            response = self.report.request_bs(
                'place=parallel_products&text={}&rearr-factors=;{}'.format('киянка+' * (count + 1), flag)
            )
            self.assertFragmentNotIn(response, {})

            # Если запрос после вырезания стоп слов содержит слов не больше,
            # чем задано флагом market_parallel_max_word_count, то ответ есть
            response = self.report.request_bs(
                'place=parallel_products&text={}&rearr-factors=;{}'.format('киянка+' * count, flag)
            )
            self.assertFragmentIn(response, {})

        # С флагом market_parallel_reject_heavy_requests=0, запросы по количеству слов после вырезания стоп слов,
        # не отбрасываются, ответ есть
        response = self.report.request_bs(
            'place=parallel_products&rearr-factors=market_parallel_reject_heavy_requests=0'
            '&text={}'.format('киянка+' * 15)
        )
        self.assertFragmentIn(response, {})

    def test_rejecting_requests_with_long_words(self):
        """Не отвечать на запросы, которые содержат длинные слова
        https://st.yandex-team.ru/MARKETOUT-20115
        """
        max_length = 30
        short_words = '{}+short+words'.format('a' * max_length)
        long_words = '{}+long+words'.format('a' * (max_length + 1))

        # Без флага market_parallel_max_request_word_length запросы, содержащие слова длиннее 30 символов
        # не обрабатываются. Ответа нет.
        response = self.report.request_bs('place=parallel_products&debug=1&text=' + long_words)
        self.assertFragmentNotIn(response, {})

        # Без флага market_parallel_max_request_word_length запросы, содержащие слова меньше 30 символов
        # обрабатываются. Ответ есть.
        response = self.report.request_bs('place=parallel_products&debug=1&text=' + short_words)
        self.assertFragmentIn(response, {})

        # С флагом market_parallel_max_request_word_length=0 обрабатываются запросы с любой длинной слов.
        response = self.report.request_bs(
            'place=parallel_products&debug=1&text='
            + short_words
            + '&rearr-factors=market_parallel_max_request_word_length=0'
        )
        self.assertFragmentIn(response, {})

        response = self.report.request_bs(
            'place=parallel_products&debug=1&text='
            + long_words
            + '&rearr-factors=market_parallel_max_request_word_length=0'
        )
        self.assertFragmentIn(response, {})

    @classmethod
    def prepare_hid_filtering(cls):
        cls.index.models += [
            Model(hyperid=660, title='tank 0', hid=55),
            Model(hyperid=661, title='tank 1', hid=54),
            Model(hyperid=662, title='tank 2', hid=54),
            Model(hyperid=663, title='tank 3', hid=55),
        ]

        cls.index.offers += [
            Offer(title='real tank 0', hyperid=661, hid=54, price=100),
            Offer(title='real tank 1', hyperid=661, hid=54, price=110),
            Offer(title='real tank 2', hyperid=662, hid=54, price=120),
            Offer(title='real tank 3', hyperid=662, hid=54, price=130),
            Offer(title='toy tank 0', hyperid=660, hid=55, price=100),
            Offer(title='toy tank 1', hyperid=660, hid=55, price=110),
            Offer(title='toy tank 2', hyperid=663, hid=55, price=120),
            Offer(title='toy tank 3', hyperid=663, hid=55, price=130),
        ]

    def test_hid_filtering(self):
        """Проверяем, что hid влияет на поиск
        https://st.yandex-team.ru/MARKETOUT-27274
        """
        request = 'place=parallel_products&text=tank&rid=1&rearr-factors=market_offers_incut_align=1'

        response = self.report.request_bs(request)
        self.assertFragmentIn(
            response,
            {
                'market_implicit_model': [
                    {
                        'showcase': {
                            'items': [
                                {'title': {'text': {'__hl': {'text': 'tank 0'}}}},
                                {'title': {'text': {'__hl': {'text': 'tank 1'}}}},
                                {'title': {'text': {'__hl': {'text': 'tank 2'}}}},
                                {'title': {'text': {'__hl': {'text': 'tank 3'}}}},
                            ]
                        }
                    }
                ],
                'market_offers_wizard': [
                    {
                        'showcase': {
                            'items': [
                                {'title': {'text': {'__hl': {'text': 'real tank 0'}}}},
                                {'title': {'text': {'__hl': {'text': 'real tank 1'}}}},
                                {'title': {'text': {'__hl': {'text': 'real tank 2'}}}},
                                {'title': {'text': {'__hl': {'text': 'real tank 3'}}}},
                                {'title': {'text': {'__hl': {'text': 'toy tank 0'}}}},
                                {'title': {'text': {'__hl': {'text': 'toy tank 1'}}}},
                                {'title': {'text': {'__hl': {'text': 'toy tank 2'}}}},
                                {'title': {'text': {'__hl': {'text': 'toy tank 3'}}}},
                            ]
                        }
                    }
                ],
            },
        )

        response = self.report.request_bs(request + '&hid=54')
        self.assertFragmentIn(
            response,
            {
                'market_implicit_model': [
                    {
                        'showcase': {
                            'items': [
                                {'title': {'text': {'__hl': {'text': 'tank 1'}}}},
                                {'title': {'text': {'__hl': {'text': 'tank 2'}}}},
                            ]
                        }
                    }
                ],
                'market_offers_wizard': [
                    {
                        'showcase': {
                            'items': [
                                {'title': {'text': {'__hl': {'text': 'real tank 0'}}}},
                                {'title': {'text': {'__hl': {'text': 'real tank 1'}}}},
                                {'title': {'text': {'__hl': {'text': 'real tank 2'}}}},
                                {'title': {'text': {'__hl': {'text': 'real tank 3'}}}},
                            ]
                        }
                    }
                ],
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'market_implicit_model': [
                    {
                        'showcase': {
                            'items': [
                                {'title': {'text': {'__hl': {'text': 'tank 0'}}}},
                                {'title': {'text': {'__hl': {'text': 'tank 3'}}}},
                            ]
                        }
                    }
                ],
                'market_offers_wizard': [
                    {
                        'showcase': {
                            'items': [
                                {'title': {'text': {'__hl': {'text': 'toy tank 0'}}}},
                                {'title': {'text': {'__hl': {'text': 'toy tank 1'}}}},
                                {'title': {'text': {'__hl': {'text': 'toy tank 2'}}}},
                                {'title': {'text': {'__hl': {'text': 'toy tank 3'}}}},
                            ]
                        }
                    }
                ],
            },
        )

        response = self.report.request_bs(request + '&hid=55')
        self.assertFragmentIn(
            response,
            {
                'market_implicit_model': [
                    {
                        'showcase': {
                            'items': [
                                {'title': {'text': {'__hl': {'text': 'tank 0'}}}},
                                {'title': {'text': {'__hl': {'text': 'tank 3'}}}},
                            ]
                        }
                    }
                ],
                'market_offers_wizard': [
                    {
                        'showcase': {
                            'items': [
                                {'title': {'text': {'__hl': {'text': 'toy tank 0'}}}},
                                {'title': {'text': {'__hl': {'text': 'toy tank 1'}}}},
                                {'title': {'text': {'__hl': {'text': 'toy tank 2'}}}},
                                {'title': {'text': {'__hl': {'text': 'toy tank 3'}}}},
                            ]
                        }
                    }
                ],
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'market_implicit_model': [
                    {
                        'showcase': {
                            'items': [
                                {'title': {'text': {'__hl': {'text': 'tank 1'}}}},
                                {'title': {'text': {'__hl': {'text': 'tank 2'}}}},
                            ]
                        }
                    }
                ],
                'market_offers_wizard': [
                    {
                        'showcase': {
                            'items': [
                                {'title': {'text': {'__hl': {'text': 'real tank 0'}}}},
                                {'title': {'text': {'__hl': {'text': 'real tank 1'}}}},
                                {'title': {'text': {'__hl': {'text': 'real tank 2'}}}},
                                {'title': {'text': {'__hl': {'text': 'real tank 3'}}}},
                            ]
                        }
                    }
                ],
            },
        )

    @classmethod
    def prepare_base_search_pre_filter(cls):
        cls.index.models += [
            Model(title='PreFilter model 1', hyperid=201, ts=101),
            Model(title='PreFilter model 2', hyperid=202, ts=102),
            Model(title='PreFilter model 3', hyperid=203, ts=103),
            Model(title='PreFilter model 4', hyperid=204, ts=104),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH_PRE_FILTER, 101).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH_PRE_FILTER, 102).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH_PRE_FILTER, 103).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH_PRE_FILTER, 104).respond(0.6)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 102).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 103).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 104).respond(0.9)

        cls.index.offers += [
            Offer(title='PreFilter offer 1', hyperid=201, ts=201),
            Offer(title='PreFilter offer 2', hyperid=202, ts=202),
            Offer(title='PreFilter offer 3', hyperid=203, ts=203),
            Offer(title='PreFilter offer 4', hyperid=204, ts=204),
            Offer(title='PreFilter offer 5', ts=205),
            Offer(title='PreFilter offer 6', ts=206),
            Offer(title='PreFilter offer 7', ts=207),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH_PRE_FILTER, 201).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH_PRE_FILTER, 202).respond(0.2)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH_PRE_FILTER, 203).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH_PRE_FILTER, 204).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH_PRE_FILTER, 205).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH_PRE_FILTER, 206).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH_PRE_FILTER, 207).respond(0.7)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 201).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 202).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 203).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 204).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 205).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 206).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 207).respond(0.3)

    def test_base_search_pre_filter(self):
        """Проверяем работу пре-фильтра в поиске на базовых
        https://st.yandex-team.ru/MARKETOUT-26377
        """
        request = 'place=parallel_products&text=PreFilter&rearr-factors=market_parallel_pre_filter_mn_algo=pre_filter_mn_algo;'

        # Проверяем офферы
        # Под флагом market_parallel_pre_filter_threshold=0.35 порог пре-фильтра проходят только 4 оффера
        response = self.report.request_bs_pb(request + 'market_parallel_pre_filter_threshold=0.35')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "offer_count": 4,
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "PreFilter offer 4", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "PreFilter offer 5", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "PreFilter offer 6", "raw": True}}}},
                        ]
                    },
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Под флагом market_parallel_pre_filter_top_count=5 префильтр проходят только Топ-5 офферов
        response = self.report.request_bs_pb(request + 'market_parallel_pre_filter_top_count=5;')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "offer_count": 5,
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "PreFilter offer 3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "PreFilter offer 4", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "PreFilter offer 5", "raw": True}}}},
                        ]
                    },
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Проверяем модели
        # Под флагом market_parallel_pre_filter_threshold=0.65 порог пре-фильтра проходят только 3 модели
        response = self.report.request_bs_pb(request + 'market_parallel_pre_filter_threshold=0.65')
        self.assertFragmentIn(
            response,
            {
                'market_implicit_model': {
                    "model_count": "3",
                    'showcase': {
                        'items': [
                            {"title": {"text": {"__hl": {"text": "PreFilter model 3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "PreFilter model 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "PreFilter model 1", "raw": True}}}},
                        ]
                    },
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Под флагом market_parallel_pre_filter_top_count=2 префильтр проходят только Топ-2 модели
        response = self.report.request_bs_pb(request + 'market_parallel_pre_filter_top_count=2;')
        self.assertFragmentIn(
            response,
            {
                'market_implicit_model': {
                    "model_count": "2",
                    'showcase': {
                        'items': [
                            {"title": {"text": {"__hl": {"text": "PreFilter model 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "PreFilter model 1", "raw": True}}}},
                        ]
                    },
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Проверяем, что в debug пишется название формулы пре-фильтра и значение
        response = self.report.request_bs(request + 'market_parallel_pre_filter_threshold=0.35&debug=1')
        self.assertFragmentIn(response, '<formula-info name="pre_filter_mn_algo" tag="BasePreFilter" value="0.7"/>')

    @classmethod
    def prepare_multitoken_factors(cls):
        cls.index.offers += [
            Offer(title="filter fz-a60mfe"),
            Offer(title="filter fz-c100mfe"),
        ]

    def test_multitoken_factors(self):
        """Проверяем, что под флагом market_parallel_boost_completely_match_multitoken_coef
        для документов считается фактор MULTITOKEN_COMPLETELY_MATCH_RATIO и бустится значение базовой формулы
        https://st.yandex-team.ru/MARKETOUT-31295
        """
        qtree = {
            'Market': {
                'qtree4market': 'cHic7VS_q9NgFL3nJi3hs0h58qAElFKXIDwIDrUIgnQqovh803uZGtFHF5c3lU6FJ1JExV8Iios_OlrKA8HFwc0x4O7g4Oq_4P2SL2mShgeC4GK23HO_85177knUFdVwqEktarPHPm2QSx06R-fpYlonj3y6XBvUtmmXhjTCY9Ar0FvQEegzSJ6voAi33OdQ1w2dI8c0He9PXGxljJxjpAFpxtHPH42UUbpLnD710L_goEmugB3y4GP7GSdCDlwlxZYuUo926GZd2sg7NXLGPGGHpyBhdt9D7Sl9N2ey6mHXv7N_W2bFlpFWr5D2-sX3D5yKM2eqBF6KBZoGI_KpbUSeUQaoFEoTSmR2EoXctnSXpg0Oa8502tzUl7m_bDX4s1Uds6iPUFdLi0KYNwMVZnyyUz6E6ybY_T05BRdheUmnFcJWTCWj86MoWI1PYxZ7BIaBrdl8UcT1Hocwm5xCXSsHrOvnbEjyhYLuaBWvrr8uvNY_K8fgCtg54adP14-HMImfUCbhC9SNkgQrSVKqwarw7sHLJ9k2rMoQcX8Y-2etEpQ52Fa6KiYl8eHZIsA8MwliYqkDs4Dni3xH3sZi0tZzZ7psXfcoeJNL4cPaX0zht4ptRsscXVUKjzazbUbLqm9xnPwsoqVx8R3vYshxDqUYBxE93sFhIWdr8L3j4btFWOz9n9F_llFb3u6joWw98obtnDzQ4G_fTxRY'  # noqa
            }
        }

        request = "place=parallel_products&text=filter+fz-a60mfe&wizard-rules={}&debug=1".format(
            urllib.quote(json.dumps(qtree))
        )

        # 1. Без флага market_parallel_boost_completely_match_multitoken_coef фактор MULTITOKEN_COMPLETELY_MATCH_RATIO всегда равен 1
        response = self.report.request_bs(request)
        self.assertFragmentIn(
            response,
            {
                "market_factors": [{"request.contains_multitoken": 1}],
                "offerFactors": [
                    {
                        "title": "filter fz-a60mfe",
                        "factors": [
                            {"REQUEST_CONTAINS_MULTITOKEN": 1},
                            {"MULTITOKEN_COMPLETELY_MATCH_RATIO": 1},
                        ],
                        "ranked_with": [{"value": 0.3}],
                    },
                    {
                        "title": "filter fz-c100mfe",
                        "factors": [
                            {"REQUEST_CONTAINS_MULTITOKEN": 1},
                            {"MULTITOKEN_COMPLETELY_MATCH_RATIO": 1},
                        ],
                        "ranked_with": [{"value": 0.3}],
                    },
                ],
            },
        )

        # 2. Под флагом market_parallel_boost_completely_match_multitoken_coef считается фактор MULTITOKEN_COMPLETELY_MATCH_RATIO
        # для оффера "filter fz-a60mfe" есть совпадение по мультитокену, значение базовой формулы бустится
        response = self.report.request_bs(
            request + '&rearr-factors=market_parallel_boost_completely_match_multitoken_coef=2'
        )
        self.assertFragmentIn(
            response,
            {
                "market_factors": [{"request.contains_multitoken": 1}],
                "offerFactors": [
                    {
                        "title": "filter fz-a60mfe",
                        "factors": [
                            {"REQUEST_CONTAINS_MULTITOKEN": 1},
                            {"MULTITOKEN_COMPLETELY_MATCH_RATIO": 1},
                        ],
                        "ranked_with": [{"value": 0.6}],
                    },
                    {"title": "filter fz-c100mfe", "ranked_with": [{"value": 0.3}]},
                ],
            },
        )

        # для оффера "filter fz-c100mfe" совпадение по мультитокену нет
        self.assertFragmentNotIn(
            response,
            {
                "offerFactors": [
                    {
                        "title": "filter fz-c100mfe",
                        "factors": [
                            {"MULTITOKEN_COMPLETELY_MATCH_RATIO": 1},
                        ],
                    }
                ]
            },
        )

    @classmethod
    def prepare_nearest_city_region(cls):
        cls.index.regiontree += [
            Region(
                rid=116705,
                name='Городской округ Балашиха',
                region_type=Region.SUBJECT_FEDERATION_DISTRICT,
                chief=10716,
                children=[
                    Region(
                        rid=10716,
                        name='Балашиха',
                        region_type=Region.CITY,
                        locative='Балашихе',
                        preposition='в',
                        latitude=55.796339,
                        longitude=37.938199,
                    ),
                    Region(
                        rid=121861, name='Чёрное', region_type=Region.VILLAGE, latitude=55.750910, longitude=38.068976
                    ),
                ],
            ),
            Region(
                rid=21622,
                name='Железнодорожный',
                region_type=Region.CITY,
                locative='Железнодорожном',
                preposition='в',
                latitude=55.746436,
                longitude=38.009049,
            ),
        ]

        cls.index.shops += [
            Shop(fesh=101, priority_region=21622),
            Shop(fesh=102, priority_region=21622),
            Shop(fesh=103, priority_region=21622),
            Shop(fesh=104, priority_region=21622),
        ]

        cls.index.offers += [
            Offer(title='Миксер кухонный 1', fesh=101, ts=301),
            Offer(title='Миксер кухонный 2', fesh=102, ts=302),
            Offer(title='Миксер кухонный 3', fesh=103, ts=303),
            Offer(title='Миксер кухонный 4', fesh=104, ts=304),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 301).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 302).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 303).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 304).respond(0.6)

    def test_nearest_city_region(self):
        """Проверяем, что если запрос задан из населенного пункта типа село,
        то для поиска используется регион ближайшего города
        https://st.yandex-team.ru/MARKETOUT-32665
        """
        request = 'place=parallel_products&text=Миксер+кухонный&rids=121861'

        # Поиск ближайшего города идет по координатам
        # Находится ближайший город Железнодорожный (lr=21622)
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=миксер+кухонный&lr=21622&clid=545"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=миксер+кухонный&lr=21622&clid=708"),
                    "adGUrl": LikeUrl.of("//market.yandex.ru/search?text=миксер+кухонный&lr=21622&clid=913"),
                    "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=миксер+кухонный&lr=21622&clid=919"),
                    "title": "\7[Миксер кухонный\7] в Железнодорожном",
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "Миксер кухонный 1", "raw": True}},
                                    "offercardUrl": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=offercard", "/geo_id=21622/"
                                    ),
                                },
                                "thumb": {
                                    "offercardUrl": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=offercard", "/geo_id=21622/"
                                    )
                                },
                            }
                        ]
                    },
                }
            },
        )

    def test_prime_auction_on_parallel(self):
        """Проверяем, что под флагом market_use_prime_auction_on_parallel
        используется SAT_HYBRID_FAIR-аукцион вместо SAT_CPC.
        https://st.yandex-team.ru/MARKETOUT-33408
        """
        request = 'place=parallel_products&text=iphone&rids=213&debug=1'

        response = self.report.request_bs(request)
        debug_response = response.extract_debug_response()
        self.assertFragmentIn(
            debug_response,
            '''
            <how><parallel>.*search_auction_params.*type: SAT_CPC.*</parallel></how>
        ''',
            use_regex=True,
        )
        self.assertFragmentIn(
            debug_response,
            '''
            <how><parallel>.*search_auction_params.*cpc.*alpha: 0.34.*beta: 0.04.*gamma: 1.*</parallel></how>
        ''',
            use_regex=True,
        )

        response = self.report.request_bs(request + '&rearr-factors=market_use_prime_auction_on_parallel=1')
        debug_response = response.extract_debug_response()
        self.assertFragmentIn(
            debug_response,
            '''
            <how><parallel>.*search_auction_params.*type: SAT_CPC.*</parallel></how>
        ''',
            use_regex=True,
        )
        self.assertFragmentIn(
            debug_response,
            '''
            <how><parallel>.*search_auction_params.*cpc.*alpha: 0.61.*beta: 0.01.*gamma: 2.742.*</parallel></how>
        ''',
            use_regex=True,
        )

    def test_use_delivery_for_parallel_relevance(self):
        """Проверяем, что под флагом market_use_delivery_for_parallel_relevance
        используется поле DELIVERY_TYPE перед CPM в релевантности.
        https://st.yandex-team.ru/MARKETOUT-33408
        """
        request = 'place=parallel_products&text=iphone&rids=213&debug=1&debug-doc-count=1'

        response = self.report.request_bs(request)
        debug_response = response.extract_debug_response()
        self.assertFragmentIn(
            debug_response,
            '''
            <document>
                <rank>
                    <MODEL_TYPE />
                    <CPM />
                    <ONSTOCK />
                    <RANDX />
                </rank>
            </document>
        ''',
        )
        self.assertFragmentNotIn(
            debug_response,
            '''
            <document>
                <rank>
                    <DELIVERY_TYPE />
                </rank>
            </document>
        ''',
        )

        response = self.report.request_bs(request + '&rearr-factors=market_use_delivery_for_parallel_relevance=1')
        debug_response = response.extract_debug_response()
        self.assertFragmentIn(
            debug_response,
            '''
            <document>
                <rank>
                    <MODEL_TYPE />
                    <DELIVERY_TYPE />
                    <CPM />
                    <ONSTOCK />
                    <RANDX />
                </rank>
            </document>
        ''',
        )

    def test_disable_offers_incut_auction(self):
        """Проверяем отключение аукциона на параллельном под флагом market_disable_auction_on_parallel=1
        https://st.yandex-team.ru/MARKETOUT-37252
        """
        # Под флагом market_disable_auction_on_parallel=1 во врезке не должно быть офферов, у которых click_price становится равным 0
        response = self.report.request_bs_pb(
            'place=parallel_products&text=gvozdoder&rearr-factors=market_offers_incut_threshold_disable=1;market_disable_auction_on_parallel=1'
        )
        self.assertFragmentNotIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "offercardUrl": Contains("//market-click2.yandex.ru/redir/dtype=offercard/cp=0/")
                                }
                            }
                        ]
                    }
                }
            },
        )

        # Проверяем для модельного колдунщика
        response = self.report.request_bs_pb(
            'place=parallel_products&text=iphone&rearr-factors=market_disable_auction_on_parallel=1'
        )
        self.assertFragmentNotIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "offercardUrl": Contains("//market-click2.yandex.ru/redir/dtype=offercard/cp=0/")
                                }
                            }
                        ]
                    }
                }
            },
        )

    def test_disable_offers_incut_url_encryption(self):
        """Проверяем отключение шифрования ссылок на параллельном под флагом market_disable_url_encryption_on_parallel=1
        https://st.yandex-team.ru/MARKETOUT-37257
        """
        query = 'place=parallel_products&text=gvozdoder&rearr-factors=market_offers_incut_threshold_disable=1;'

        # 1. Ведем в магазин
        # Без флага market_disable_url_encryption_on_parallel=1 ссылка шифрованная
        response = self.report.request_bs_pb(query + 'market_offers_wizard_incut_url_type=External')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "gvozdoder 11", "raw": True}},
                                    "urlForCounter": Contains("//market-click2.yandex.ru/redir/dtype=market/"),
                                }
                            }
                        ]
                    }
                }
            },
        )

        # Под флагом market_disable_url_encryption_on_parallel=1 прямая ссылка в магазин
        response = self.report.request_bs_pb(
            query + 'market_offers_wizard_incut_url_type=External;market_disable_url_encryption_on_parallel=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "gvozdoder 11", "raw": True}},
                                    "urlForCounter": LikeUrl.of("http://gvozdoder-shop.ru/11"),
                                }
                            }
                        ]
                    }
                }
            },
        )

        # 2. Ведем на КО
        # Без флага market_disable_url_encryption_on_parallel=1 ссылка шифрованная
        response = self.report.request_bs_pb(query + 'market_offers_wizard_incut_url_type=OfferCard')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "gvozdoder 11", "raw": True}},
                                    "urlForCounter": Contains("//market-click2.yandex.ru/redir/dtype=offercard/"),
                                }
                            }
                        ]
                    }
                }
            },
        )

        # Под флагом market_disable_url_encryption_on_parallel=1 прямая ссылка на КО
        response = self.report.request_bs_pb(
            query + 'market_offers_wizard_incut_url_type=OfferCard;market_disable_url_encryption_on_parallel=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "gvozdoder 11", "raw": True}},
                                    "urlForCounter": LikeUrl.of(
                                        "//market.yandex.ru/offer/3QVfU6RXAv4F5EnDCsgN-Q?text=gvozdoder&lr=0&clid=545"
                                    ),
                                }
                            }
                        ]
                    }
                }
            },
        )

    def test_disable_offercard_encryption(self):
        """Под флагом market_disable_url_encryption_for_images_in_offercard_only при формировании оферной врезки
        для картинок, не шифруем offercard урлы
        этот флаг приоритетнее market_disable_url_encryption_on_parallel, который отключает шифрование всех ссылок
        https://st.yandex-team.ru/MARKETOUT-37975
        """

        rearr = ['market_offers_wizard_for_images=1', 'market_offers_wizard_incut_url_type=OfferCard']
        # без флага шифрованный урл в urlForCounter
        request = 'place=parallel_products&text=gvozdoder&wprid=1123456789&rearr-factors={}'.format(';'.join(rearr))
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "urlForCounter": Contains("//market-click2.yandex.ru/redir/dtype=offercard/")
                                },
                                "thumb": {
                                    "urlForCounter": Contains("//market-click2.yandex.ru/redir/dtype=offercard/"),
                                },
                            }
                        ]
                    }
                }
            },
        )

        # не шифрованный урл, только в urlForCounter
        response = self.report.request_bs_pb(request + ';market_disable_offercard_url_encryption_on_parallel=1')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "offercardUrl": Contains("//market-click2.yandex.ru/redir/dtype=offercard/"),
                                    "urlForCounter": LikeUrl(url_path="/offer/3QVfU6RXAv4F5EnDCsgN-Q"),
                                },
                                "thumb": {
                                    "urlForCounter": LikeUrl.of("//market.yandex.ru/offer/3QVfU6RXAv4F5EnDCsgN-Q"),
                                },
                            }
                        ]
                    }
                }
            },
        )
        # нет clid и wprid в урле
        self.assertFragmentNotIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "offercardUrl": Contains("//market-click2.yandex.ru/redir/dtype=offercard/"),
                                    "urlForCounter": LikeUrl(
                                        url_path="/offer/3QVfU6RXAv4F5EnDCsgN-Q",
                                        url_params={"clid": 545, "wprid": "1123456789"},
                                    ),
                                },
                            }
                        ]
                    }
                }
            },
        )

        # если ссылка External, урл шифрованный
        rearr = [
            'market_offers_wizard_for_images=1',
            'market_offers_wizard_incut_url_type=External',
            'market_disable_offercard_url_encryption_on_parallel=1',
        ]
        request = 'place=parallel_products&text=gvozdoder&rearr-factors={}'.format(';'.join(rearr))
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"urlForCounter": Contains("//market-click2.yandex.ru/redir/dtype=market/")},
                            }
                        ]
                    }
                }
            },
        )

    def test_parallel_bigb_factors(self):
        '''Использование BigB факторов для parallel
        https://st.yandex-team.ru/MARKETOUT-38199
        '''

        params = {
            "place": "parallel",
            "text": "iphone",
            "bsformat": "1",
            "debug": "1",
            "rearr-factors": ["market_parallel_calc_personal_factors=1"],
            "items": [
                {
                    "keyword_id": 174,
                    "weighted_values": [{"value_id": 0, "value_weight": 550}, {"value_id": 1, "value_weight": 650}],
                },
                {
                    "keyword_id": 175,
                    "weighted_values": [
                        {"value_id": 0, "value_weight": 10},
                        {"value_id": 1, "value_weight": 11},
                        {"value_id": 2, "value_weight": 12},
                        {"value_id": 3, "value_weight": 13},
                        {"value_id": 4, "value_weight": 14},
                    ],
                },
                {
                    "keyword_id": 543,
                    "weighted_values": [
                        {"value_id": 0, "value_weight": 10},
                        {"value_id": 1, "value_weight": 11},
                        {"value_id": 2, "value_weight": 12},
                        {"value_id": 3, "value_weight": 13},
                        {"value_id": 4, "value_weight": 14},
                        {"value_id": 5, "value_weight": 15},
                    ],
                },
                {
                    "keyword_id": 614,
                    "weighted_values": [
                        {"value_id": 0, "value_weight": 10},
                        {"value_id": 1, "value_weight": 11},
                        {"value_id": 2, "value_weight": 12},
                        {"value_id": 3, "value_weight": 13},
                        {"value_id": 4, "value_weight": 14},
                    ],
                },
            ],
            "counters": [
                {"counter_id": 256, "key": [500], "value": [7]},
                {"counter_id": 257, "key": [500], "value": [10]},
                {"counter_id": 258, "key": [21], "value": [9]},
                {"counter_id": 259, "key": [21], "value": [12]},
                {"counter_id": 260, "key": [22], "value": [11]},
                {"counter_id": 261, "key": [22], "value": [13]},
            ],
        }
        response = self.report.request_bigb_apphost(params)

        self.assertFragmentIn(response, "USER_AGE_0")
        self.assertFragmentIn(response, "USER_AGE_1")
        self.assertFragmentIn(response, "USER_AGE_2")
        self.assertFragmentIn(response, "USER_AGE_3")
        self.assertFragmentIn(response, "USER_AGE_4")
        self.assertFragmentIn(response, "USER_GENDER_MALE")
        self.assertFragmentIn(response, "USER_GENDER_FEMALE")
        self.assertFragmentIn(response, "USER_AGE6_0")
        self.assertFragmentIn(response, "USER_AGE6_1")
        self.assertFragmentIn(response, "USER_AGE6_2")
        self.assertFragmentIn(response, "USER_AGE6_3")
        self.assertFragmentIn(response, "USER_AGE6_4")
        self.assertFragmentIn(response, "USER_AGE6_5")
        self.assertFragmentIn(response, "USER_REVENUE5_0")
        self.assertFragmentIn(response, "USER_REVENUE5_1")
        self.assertFragmentIn(response, "USER_REVENUE5_2")
        self.assertFragmentIn(response, "USER_REVENUE5_3")
        self.assertFragmentIn(response, "USER_REVENUE5_4")
        self.assertFragmentIn(response, "USER_PERIOD_MODEL_NO_ORDER")
        self.assertFragmentIn(response, "USER_COUNT_MODEL_ORDER")
        self.assertFragmentIn(response, "USER_RATIO_MODEL_ORDER")
        self.assertFragmentIn(response, "USER_PERIOD_CATEGORY_NO_ORDER")
        self.assertFragmentIn(response, "USER_COUNT_CATEGORY_ORDER")
        self.assertFragmentIn(response, "USER_RATIO_CATEGORY_ORDER")
        self.assertFragmentIn(response, "USER_PERIOD_VENDOR_NO_ORDER")
        self.assertFragmentIn(response, "USER_COUNT_VENDOR_ORDER")
        self.assertFragmentIn(response, "USER_RATIO_VENDOR_ORDER")

    def test_parallel_bigb_factors_default(self):
        '''Использование BigB факторов для parallel
        в дефолтном режиме
        '''

        params = {
            "place": "parallel",
            "text": "iphone",
            "bsformat": "1",
            "debug": "1",
            "items": [
                {
                    "keyword_id": 174,
                    "weighted_values": [{"value_id": 0, "value_weight": 550}, {"value_id": 1, "value_weight": 650}],
                },
                {
                    "keyword_id": 175,
                    "weighted_values": [
                        {"value_id": 0, "value_weight": 10},
                        {"value_id": 1, "value_weight": 11},
                        {"value_id": 2, "value_weight": 12},
                        {"value_id": 3, "value_weight": 13},
                        {"value_id": 4, "value_weight": 14},
                    ],
                },
                {
                    "keyword_id": 543,
                    "weighted_values": [
                        {"value_id": 0, "value_weight": 10},
                        {"value_id": 1, "value_weight": 11},
                        {"value_id": 2, "value_weight": 12},
                        {"value_id": 3, "value_weight": 13},
                        {"value_id": 4, "value_weight": 14},
                        {"value_id": 5, "value_weight": 15},
                    ],
                },
                {
                    "keyword_id": 614,
                    "weighted_values": [
                        {"value_id": 0, "value_weight": 10},
                        {"value_id": 1, "value_weight": 11},
                        {"value_id": 2, "value_weight": 12},
                        {"value_id": 3, "value_weight": 13},
                        {"value_id": 4, "value_weight": 14},
                    ],
                },
            ],
            "counters": [
                {"counter_id": 256, "key": [500], "value": [7]},
                {"counter_id": 257, "key": [500], "value": [10]},
                {"counter_id": 258, "key": [21], "value": [9]},
                {"counter_id": 259, "key": [21], "value": [12]},
                {"counter_id": 260, "key": [22], "value": [11]},
                {"counter_id": 261, "key": [22], "value": [13]},
            ],
        }
        response = self.report.request_bigb_apphost(params)

        self.assertFragmentIn(response, "USER_AGE_0")
        self.assertFragmentIn(response, "USER_AGE_1")
        self.assertFragmentIn(response, "USER_AGE_2")
        self.assertFragmentIn(response, "USER_AGE_3")
        self.assertFragmentIn(response, "USER_AGE_4")
        self.assertFragmentIn(response, "USER_GENDER_MALE")
        self.assertFragmentIn(response, "USER_GENDER_FEMALE")
        self.assertFragmentIn(response, "USER_AGE6_0")
        self.assertFragmentIn(response, "USER_AGE6_1")
        self.assertFragmentIn(response, "USER_AGE6_2")
        self.assertFragmentIn(response, "USER_AGE6_3")
        self.assertFragmentIn(response, "USER_AGE6_4")
        self.assertFragmentIn(response, "USER_AGE6_5")
        self.assertFragmentIn(response, "USER_REVENUE5_0")
        self.assertFragmentIn(response, "USER_REVENUE5_1")
        self.assertFragmentIn(response, "USER_REVENUE5_2")
        self.assertFragmentIn(response, "USER_REVENUE5_3")
        self.assertFragmentIn(response, "USER_REVENUE5_4")

    def test_disable_market_url_encryption(self):
        """
        Проверяем, что под флагом market_offers_wizard_incut_url_type=OfferCard мы пишем offercard url
        Проверяем, что под флагами market_images_add_params_to_url=1 и market_images_add_icookie_to_url=1
        добавляем доп. параметры в урл
        https://st.yandex-team.ru/MARKETOUT-37975
        """

        rearr = [
            'market_offers_wizard_for_images=1',
            'market_offers_wizard_incut_url_type=External',
            'market_images_add_params_to_url=1',
        ]
        # с флагом шифрованный урл в urlForCounter с доп. параметрами
        request = 'place=parallel_products&src_pof=971&text=special+blue+offer&x-yandex-encrypted-icookie=1123456&wprid=cb8f7a995444ab4dc3215cad33fcd6bb&rearr-factors={}'
        response = self.report.request_bs_pb(request.format(';'.join(rearr)))
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "urlForCounter": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=market/",
                                        "src_pof%3D971%26wprid%3Dcb8f7a995444ab4dc3215cad33fcd6bb%26utm_source_service%3Dimg",
                                    )
                                },
                                "thumb": {
                                    "urlForCounter": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=market/",
                                        "src_pof%3D971%26wprid%3Dcb8f7a995444ab4dc3215cad33fcd6bb%26utm_source_service%3Dimg",
                                    ),
                                },
                            }
                        ]
                    }
                }
            },
        )

        rearr = [
            'market_offers_wizard_for_images=1',
            'market_offers_wizard_incut_url_type=External',
            'market_images_add_params_to_url=1',
            'market_images_add_icookie_to_url=1',
        ]
        # с флагом шифрованный урл в urlForCounter с доп. параметрами
        request = 'place=parallel_products&src_pof=971&text=special+blue+offer&x-yandex-encrypted-icookie=1123456&wprid=cb8f7a995444ab4dc3215cad33fcd6bb&rearr-factors={}'
        response = self.report.request_bs_pb(request.format(';'.join(rearr)))
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "urlForCounter": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=market/",
                                        "src_pof%3D971%26wprid%3Dcb8f7a995444ab4dc3215cad33fcd6bb%26icookie%3D1123456%26utm_source_service%3Dimg",
                                    )
                                },
                                "thumb": {
                                    "urlForCounter": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=market/",
                                        "src_pof%3D971%26wprid%3Dcb8f7a995444ab4dc3215cad33fcd6bb%26icookie%3D1123456%26utm_source_service%3Dimg",
                                    ),
                                },
                            }
                        ]
                    }
                }
            },
        )

        rearr = [
            'market_offers_wizard_for_images=1',
            'market_offers_wizard_incut_url_type=OfferCard',
            'market_images_add_params_to_url=1',
            'market_disable_offercard_url_encryption_on_parallel=1',
        ]
        # расшифрованный урл, только в urlForCounter
        response = self.report.request_bs_pb(request.format(';'.join(rearr)))
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "offercardUrl": Contains("//market-click2.yandex.ru/redir/dtype=offercard/"),
                                    "urlForCounter": LikeUrl.of("//market.yandex.ru/offer/Sku1Pr0000-IiLVm1Goleg"),
                                },
                                "thumb": {
                                    "urlForCounter": LikeUrl.of("//market.yandex.ru/offer/Sku1Pr0000-IiLVm1Goleg"),
                                },
                            }
                        ]
                    }
                }
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "urlForCounter": LikeUrl.of("//market.yandex.ru/offer/Sku1Pr0000-IiLVm1Goleg?clid"),
                                },
                            }
                        ]
                    }
                }
            },
        )

    def test_disable_model_shard(self):
        """Проверяем, что флаг market_parallel_disable_model_shard отключает хождение в модельный шард
        https://st.yandex-team.ru/MARKETOUT-40387
        """
        request = 'place=parallel_products&text=iphone&trace_wizard=1&debug=1'

        debug = '''
            <report>
                <context>
                    <request-params>
                        <collection name="MODEL"></collection>
                        <collection name="PREVIEW_MODEL"></collection>
                    </request-params>
                </context>
            </report>
            <metasearch>
                <nodes>
                    <node descr="MODEL"></node>
                </nodes>
            </metasearch>
        '''

        # Под флагом market_parallel_disable_model_shard=1 модельный шард отключен
        response = self.report.request_bs(request + '&rearr-factors=market_parallel_disable_model_shard=1')
        self.assertFragmentIn(
            response, '29 1 Группировка по принадлежности к модели (yg) : 0 документов были объединены в 0 групп'
        )
        self.assertFragmentNotIn(response.extract_debug_response(), '<debug name="TParallel">{}</debug>'.format(debug))
        self.assertFragmentNotIn(
            response.extract_debug_response(), '<debug name="TModelWizardsOffers">{}</debug>'.format(debug)
        )

        # Без флага market_parallel_disable_model_shard=1 модельный шард включен
        response = self.report.request_bs(request)
        self.assertFragmentIn(
            response, '29 1 Группировка по принадлежности к модели (yg) : 3 документа были объединены в 1 группу'
        )
        self.assertFragmentIn(response.extract_debug_response(), '<debug name="TParallel">{}</debug>'.format(debug))
        self.assertFragmentIn(
            response.extract_debug_response(), '<debug name="TModelWizardsOffers">{}</debug>'.format(debug)
        )

    @classmethod
    def prepare_zero_prices_filter(cls):
        cls.index.offers += [
            Offer(title='Lego Technic Porsche 911 RSR 42096'),
            Offer(title='LEGO Technic 42099 Экстремальный внедорожник'),
            Offer(title='LEGO City 60228 Ракета для запуска в далекий космос и пульт управления запуском'),
            Offer(title='4000014-2  The Legoland Train - LEGO Fan Weekend Exclusive Edition', price=0),
        ]

    def test_zero_prices_filter(self):
        """Проверяем что под флагом будут удаляться офферы с нулевой ценой
        https://st.yandex-team.ru/ECOMQUALITY-168
        """
        response = self.report.request_json(
            'place=parallel_products&text=lego&rearr-factors=market_parallel_filter_zero_prices=1&debug=1&bsformat=2'
        )
        self.assertFragmentIn(
            response,
            [
                {'title': {'text': {'__hl': {'text': "Lego Technic Porsche 911 RSR 42096"}}}},
            ],
        )
        self.assertFragmentIn(
            response,
            [
                {'title': {'text': {'__hl': {'text': "LEGO Technic 42099 Экстремальный внедорожник"}}}},
            ],
        )
        self.assertFragmentIn(
            response,
            [
                {
                    'title': {
                        'text': {
                            '__hl': {
                                'text': "LEGO City 60228 Ракета для запуска в далекий космос и пульт управления запуском"
                            }
                        }
                    }
                },
            ],
        )
        self.assertFragmentNotIn(
            response,
            [
                {
                    'title': {
                        'text': {'__hl': {'text': "4000014-2  The Legoland Train - LEGO Fan Weekend Exclusive Edition"}}
                    }
                },
            ],
        )

        self.assertFragmentIn(response, {'debug': {'brief': {'filters': {'ZERO_OFFER_PRICE': 1}}}})


if __name__ == '__main__':
    main()
