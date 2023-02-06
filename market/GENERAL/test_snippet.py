#!/usr/bin/env python
# -*- coding: utf-8 -*-
import runner  # noqa

import sys
import time
import json

from core.types import (
    Book,
    CardNavCategory,
    CardVendor,
    HyperCategory,
    HyperCategoryType,
    Model,
    ModelGroup,
    NavCategory,
    Offer,
    VCluster,
)
from core.logs import ErrorCodes
from core.testcase import TestCase, main
from core.types.offer import OfferDimensions
from core.types.card import CardCategory, CardCategoryVendor
from core.types.catalog import Category
from core.types.vendor import Vendor
from core.matcher import Contains, GreaterEq, NoKey


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.use_external_snippets = False
        cls.index.offers += [
            Offer(title='iphone', title_snippet='snippet-phone'),
            Offer(title='samsung', title_snippet='snippet-samsung', allow_ext_snippets=False),
            Offer(title='cisco', title_snippet='snippet-samsung', allow_ext_snippets=False),
            Offer(title='not_pruned', title_snippet='snippet-not_pruned'),
            Offer(title='pruned', title_snippet='snippet-pruned'),
        ]

        cls.index.models += [
            Model(title='nokia', title_snippet='snippet-model-nokia'),
        ]

        cls.index.vclusters += [
            VCluster(title='jeans', title_snippet='snippet-model-jeans'),
        ]

        # NB! Books are not suppoerted on indexer yet! @see MARKETINDEXER-24067
        cls.index.books += [Book(title='cpp_dlya_chainikov', title_snippet='snippet-book-cpp_dlya_chainikov')]

        cls.index.model_groups += [
            ModelGroup(
                hid=40, title='asus notebook groupspec', title_snippet='asus notebook groupspec snippet', hyperid=4000
            )
        ]

        cls.index.models += [
            Model(title='asus notebook model', title_snippet='asus notebook model snippet'),
        ]

        cls.index.creation_time = 120

    @staticmethod
    def common_snippets_exps():
        for bulk in ('', 'ext_snippet_bulk=1'):
            for compress in ('', 'ext_snippet_compress=pcgzip'):
                yield ";".join((bulk, compress))

    def test_full_offer(self):
        response = self.report.request_json('place=print_doc&text=iphone')
        self.assertFragmentIn(response, {'title': 'iphone'})

        def action():
            self.report.reset_unistats()
            for exp in self.common_snippets_exps():
                query = 'place=print_doc&text=iphone&rearr-factors=ext_snippet=1;{}'.format(exp)
                response = self.report.request_json(query)
                self.assertFragmentIn(response, {'title': 'snippet-phone'})

                if self.snippets.global_cache_size == 0:
                    response = self.report.request_tass()
                    self.assertIn("Market_snippets_2xx_dmmm", response)
                self.report.reset_unistats()

        self.__retry_action(action)

    def test_full_model(self):
        response = self.report.request_json('place=print_doc&text=nokia')
        self.assertFragmentIn(response, {'title': 'nokia'})

        def action():
            self.report.reset_unistats()
            for exp in self.common_snippets_exps():
                response = self.report.request_json(
                    'place=print_doc&text=nokia&rearr-factors=ext_snippet=1;{}'.format(exp)
                )
                self.assertFragmentIn(response, {'title': 'snippet-model-nokia'})

                if self.snippets.global_cache_size == 0:
                    response = self.report.request_tass_or_wait(wait_hole='Market_snippets_2xx_dmmm')
                    self.assertIn("Market_snippets_2xx_dmmm", response)
                self.report.reset_unistats()

        self.__retry_action(action)

    def test_full_vcluster(self):
        response = self.report.request_json('place=print_doc&text=jeans')
        self.assertFragmentIn(response, {'title': 'jeans'})

        def action():
            self.report.reset_unistats()
            for exp in self.common_snippets_exps():
                response = self.report.request_json(
                    'place=print_doc&text=jeans&rearr-factors=ext_snippet=1;{}'.format(exp)
                )
                self.assertFragmentIn(response, {'title': 'snippet-model-jeans'})

                if self.snippets.global_cache_size == 0:
                    response = self.report.request_tass()
                    self.assertIn("Market_snippets_2xx_dmmm", response)
                self.report.reset_unistats()

        self.__retry_action(action)

    def test_multi_doc(self):
        response = self.report.request_json('place=print_doc&text=iphone | nokia | jeans')
        self.assertFragmentIn(response, {'title': 'iphone'})
        self.assertFragmentIn(response, {'title': 'nokia'})
        self.assertFragmentIn(response, {'title': 'jeans'})

        for exp in self.common_snippets_exps():
            response = self.report.request_json(
                'place=print_doc&text=iphone | nokia | jeans&rearr-factors=ext_snippet=1;{}'.format(exp)
            )
            self.assertFragmentIn(response, {'title': 'snippet-phone'})
            self.assertFragmentIn(response, {'title': 'snippet-model-nokia'})
            self.assertFragmentIn(response, {'title': 'snippet-model-jeans'})

    # NB! Books are not suppoerted on indexer yet! @see MARKETINDEXER-24067
    def test_full_books(self):
        response = self.report.request_json('place=print_doc&text=cpp_dlya_chainikov')
        self.assertFragmentIn(response, {'title': 'Book Writer "cpp_dlya_chainikov"'})

        self.report.reset_unistats()

        def action():
            for exp in self.common_snippets_exps():
                response = self.report.request_json(
                    'place=print_doc&text=cpp_dlya_chainikov&rearr-factors=ext_snippet=1;{}'.format(exp)
                )
                self.assertFragmentIn(response, {'title': 'snippet-book-cpp_dlya_chainikov'})
                if self.snippets.global_cache_size == 0:
                    response = self.report.request_tass()
                    self.assertIn("Market_snippets_2xx_dmmm", response)
                self.report.reset_unistats()

        self.__retry_action(action)

    def test_group_models(self):
        response = self.report.request_json('place=print_doc&text=asus')
        self.assertFragmentIn(response, {'title': 'asus notebook model'})
        self.assertFragmentIn(response, {'title': 'asus notebook groupspec'})

        def action():
            self.report.reset_unistats()
            for exp in self.common_snippets_exps():
                response = self.report.request_json(
                    'place=print_doc&text=asus&rearr-factors=ext_snippet=1;{}'.format(exp)
                )
                self.assertFragmentIn(response, {'title': 'asus notebook model snippet'})
                self.assertFragmentIn(response, {'title': 'asus notebook groupspec snippet'})
                if self.snippets.global_cache_size == 0:
                    response = self.report.request_tass()
                    self.assertIn("Market_snippets_2xx_dmmm", response)
                self.report.reset_unistats()

        self.__retry_action(action)

    def test_mirror(self):
        for exp in self.common_snippets_exps():
            response = self.report.request_json(
                'place=print_doc&text=iphone&rearr-factors=ext_snippet=3;{}'.format(exp)
            )
            self.assertFragmentIn(response, {'title': 'iphone'})
            # note: unstable because async
            # response = self.report.request_tass()
            # self.assertIn('market_snippets_prop_total_dmmm', response)
            # self.assertIn('market_snippets_prop_value_diff__Title_dmmm', response)

    def test_fallback(self):
        """
        Делаем два запроса: в одном из них получаем документ из внешних снипетов и показываем его,
        в другом -- документа из внешних снипетов нет, тогда показываем документ со снипетного
        """

        def action():
            self.report.reset_unistats()
            for exp in self.common_snippets_exps():
                response = self.report.request_json(
                    'place=print_doc&text=iphone&rearr-factors=ext_snippet=2;{}'.format(exp)
                )
                self.assertFragmentIn(response, {'title': 'snippet-phone'})

                response = self.report.request_json(
                    'place=print_doc&text=samsung&rearr-factors=ext_snippet=2;{}'.format(exp)
                )
                self.assertFragmentIn(response, {'title': 'samsung'})

                response = self.report.request_tass()
                self.assertIn("market_snippets_fallback_dmmm", response)
                self.report.reset_unistats()

        if self.snippets.global_cache_size == 0:
            self.__retry_action(action)

    @classmethod
    def prepare_property_fallback(cls):
        cls.index.offers += [Offer(title='sony', allow_ext_snippets=True, no_snippet_title=True)]

    def test_property_fallback(self):
        def action():
            self.report.reset_unistats()
            response = self.report.request_json('place=print_doc&text=sony')
            self.assertFragmentIn(response, {'title': 'sony'})
            response = self.report.request_tass()
            self.assertNotIn("market_snippets_prop_absent__Title_dmmm", response)

            response = self.report.request_json('place=print_doc&text=sony&rearr-factors=ext_snippet=2')
            self.assertFragmentIn(response, {'title': 'sony'})
            response = self.report.request_tass()
            self.assertEqual(response.get("market_snippets_prop_absent__Title_dmmm"), 1)

            response = self.report.request_json('place=print_doc&text=sony&rearr-factors=ext_snippet=1')
            self.assertFragmentIn(response, {'title': NoKey('title')})

        if self.snippets.global_cache_size == 0:
            self.__retry_action(action)

    def test_tass(self):
        def action():
            self.report.reset_unistats()
            self.report.request_json('place=print_doc&text=sony&rearr-factors=ext_snippet=1')
            response = self.report.request_tass_or_wait(wait_hole='Market_snippets_2xx_dmmm')
            self.assertEqual(response.get("Market_snippets_2xx_dmmm"), 1)
            self.assertIn("Market_snippets_resp_size_dmmm", response)
            self.assertIn("Market_snippets_query_count_dmmm", response)

        if self.snippets.global_cache_size == 0:
            self.__retry_action(action)

    @classmethod
    def prepare_skip_without_snippets(cls):
        cls.index.offers += [
            Offer(title='xiaomi one', allow_ext_snippets=True, fesh=538401),
            Offer(title='xiaomi two', allow_ext_snippets=False, fesh=538401),
        ]

    def test_skip_without_snippets(self):
        """
        Создаем два оффера: для первого есть ассоциированный внешний сниппет, для второго нет
        Проверяем, что в режиме без использования внешних сниппетов находятся два оффера,
        а с включением использования внешних сниппетом -- только один (первый)
        """

        # без внешних сниппетов
        response = self.report.request_json('place=print_doc&text=xiaomi')
        self.assertFragmentIn(response, {'title': 'xiaomi one'})
        self.assertFragmentIn(response, {'title': 'xiaomi two'})

        # со внешними сниппетами
        for exp in self.common_snippets_exps():
            response = self.report.request_json(
                'place=print_doc&text=xiaomi&rearr-factors=ext_snippet=1;{}'.format(exp)
            )
            self.assertFragmentIn(response, {'title': 'xiaomi one'})
            self.assertFragmentNotIn(response, {'title': 'xiaomi two'})

    def test_skip_without_snippets_shopoffers(self):
        """
        Создаем два оффера: для первого есть ассоциированный внешний сниппет, для второго нет
        Проверяем, что в режиме без использования внешних сниппетов находятся два оффера,
        а с включением использования внешних сниппетом -- только один (первый)
        """

        # без внешних сниппетов
        response = self.report.request_xml('place=shopoffers&fesh=538401&shop-offers-chunk=1')
        self.assertFragmentIn(response, 'xiaomi one')
        self.assertFragmentIn(response, 'xiaomi two')

        # со внешними сниппетами
        for exp in self.common_snippets_exps():
            response = self.report.request_xml(
                'place=shopoffers&fesh=538401&shop-offers-chunk=1&rearr-factors=ext_snippet=1;{}'.format(exp)
            )
            self.assertFragmentIn(response, 'xiaomi one')
            self.assertFragmentNotIn(response, 'xiaomi two')

    def test_skip_without_snippets_trace_check(self):
        """
        Проверяем, что если сниппет не нашёлся и документ исключили из выдачи,
        то будет запись в логе об этом
        """
        # без внешних сниппетов
        response = self.report.request_json('place=print_doc&text=xiaomi&debug=1')
        self.assertFragmentNotIn(response, {'logicTrace': [Contains('empty_ext_snippet')]})

        # со внешними сниппетами
        response = self.report.request_json('place=print_doc&text=xiaomi&rearr-factors=ext_snippet=1&debug=1')
        self.assertFragmentIn(response, {'logicTrace': [Contains('empty_ext_snippet')]})

    @classmethod
    def prepare_ext_snippets_dimensions(cls):
        cls.index.offers += [
            Offer(
                title='yota phone',
                allow_ext_snippets=True,
                weight=5.001,
                dimensions=OfferDimensions(length=20.002, width=30.003, height=10.001, ext_snippet=True),
            )
        ]

    def test_ext_snippets_dimensions(self):
        """
        Проверяем, что использование внешних сниппетов не приводит к потере данных ВГ
        """

        for exp in self.common_snippets_exps():
            response = self.report.request_json('place=print_doc&text=yota&rearr-factors=ext_snippet=1;{}'.format(exp))
            assert response.root['documents_count'] == 1

            self.assertFragmentIn(
                response,
                {
                    'title': 'yota phone',
                    'properties': {'weight': '5.001', 'length': '20.002', 'width': '30.003', 'height': '10.001'},
                },
            )

    def test_zero_docs(self):
        for rearr in self.common_snippets_exps():
            response = self.report.request_json(
                'place=prime&pp=18&text=notfound&rearr-factors=ext_snippet=1;{}'.format(rearr)
            )
            self.assertFragmentIn(response, {'total': 0})

    @classmethod
    def prepare_lazy_snippets(cls):
        cls.index.offers += [
            Offer(title='lazy one', allow_ext_snippets=True),
            Offer(title='lazy two', allow_ext_snippets=True),
            Offer(title='lazy three', allow_ext_snippets=True),
            Offer(title='lazy four', allow_ext_snippets=True),
            Offer(title='lazy five', allow_ext_snippets=True),
        ]

    def test_lazy_snippets(self):
        """
        Делаем два запроса с пейджингом на 2ю страницу: один с жадным походом за сниппетами, другой -- с ленивым.
        Проверяем, что кол-во запрошенных в первом случае сниппетов больше, чем во втором
        """

        def action():
            self.report.reset_unistats()
            common_part = 'place=miprime&text=lazy&page=2&numdoc=3&rearr-factors=ext_snippet=1'
            response = self.report.request_json(common_part)
            self.assertEqual(2, response.count({"entity": "offer"}))

            if self.snippets.global_cache_size == 0:
                response = self.report.request_tass()
                self.assertEqual(response.get('Market_snippets_key_requested_dmmm'), 5)

            response = self.report.request_json(common_part + ';ext_snippet_lazy=1')
            self.assertEqual(2, response.count({"entity": "offer"}))

            if self.snippets.global_cache_size == 0:
                response = self.report.request_tass()
                self.assertEqual(response.get('Market_snippets_key_requested_dmmm'), 7)

        self.__retry_action(action)

    def test_lazy_snippets_page_gte(self):
        """
        Делаем два запроса с пейджингом на 2ю страницу: один с жадным походом за сниппетами, другой -- с ленивым.
        Проверяем, что кол-во запрошенных в первом случае сниппетов больше, чем во втором
        """

        def action():
            self.report.reset_unistats()
            common_part = 'place=miprime&text=lazy&page=2&numdoc=3&rearr-factors=ext_snippet=1'
            response = self.report.request_json(common_part)
            self.assertEqual(2, response.count({"entity": "offer"}))

            if self.snippets.global_cache_size == 0:
                response = self.report.request_tass()
                self.assertEqual(response.get('Market_snippets_key_requested_dmmm'), 5)

            response = self.report.request_json(common_part + ';ext_snippet_lazy_on_page_gte=2')
            self.assertEqual(2, response.count({"entity": "offer"}))
            if self.snippets.global_cache_size == 0:
                response = self.report.request_tass()
                self.assertEqual(response.get('Market_snippets_key_requested_dmmm'), 7)

        self.__retry_action(action)

    def test_lazy_snippets_and_prefetch_combined(self):
        """
        Делаем запрос с ext_snippet_lazy=1 и проверяем, что делается 3 запроса
        Делаем запрос с комбинированным режимом ext_snippet_lazy_bulk_mode_on_print_doс=1 и должен быть один запрос
        """

        def action():
            self.report.reset_unistats()
            response = self.report.request_json(
                'place=print_doc&text=iphone | nokia | jeans&rearr-factors=ext_snippet=1;ext_snippet_lazy=1'
            )
            self.assertFragmentIn(response, {'title': 'snippet-phone'})
            self.assertFragmentIn(response, {'title': 'snippet-model-nokia'})
            self.assertFragmentIn(response, {'title': 'snippet-model-jeans'})

            if self.snippets.global_cache_size == 0:
                response = self.report.request_tass()
                self.assertEqual(response.get('Market_snippets_query_count_dmmm'), 3)

            self.report.reset_unistats()

        self.__retry_action(action)

    def test_prefetch_request_size(self):
        """
        1. Запрос, когда все ключи запрашиваются в одном запросе в SaaS;
        2. Выставлем ext_snippet_bulk_size=2 - тогда в одном запросе в SaaS должно быть не более 2 ключей.
           Всего у нас 3 документа, поэтому суммарно мы должны сделать 2 запроса.
        """

        def action():
            self.report.reset_unistats()
            response = self.report.request_json(
                'place=print_doc&text=iphone | nokia | jeans&rearr-factors=ext_snippet=3;ext_snippet_bulk=1'
            )
            self.assertFragmentIn(response, {'title': 'iphone'})
            self.assertFragmentIn(response, {'title': 'nokia'})
            self.assertFragmentIn(response, {'title': 'jeans'})

            if self.snippets.global_cache_size == 0:
                response = self.report.request_tass()
                self.assertEqual(response.get('Market_snippets_query_count_dmmm'), 1)

            response = self.report.request_json(
                'place=print_doc&text=iphone | nokia | jeans&rearr-factors=ext_snippet=3;ext_snippet_bulk=1;ext_snippet_bulk_size=2'
            )
            self.assertFragmentIn(response, {'title': 'iphone'})
            self.assertFragmentIn(response, {'title': 'nokia'})
            self.assertFragmentIn(response, {'title': 'jeans'})

            if self.snippets.global_cache_size == 0:
                response = self.report.request_tass()
                self.assertEqual(
                    response.get('Market_snippets_query_count_dmmm'), 3
                )  # 1 от прошлого запроса + 2 сейчас

        self.__retry_action(action)

    def test_prefetch_not_found_saas_docs(self):
        """
        1. Запросим из SaaSKV несколько несуществющих там документов, с включенным bulk
        2. Убедимся, что счетчик ошибок не вырос, а NotFound вырос
        """

        def action():
            self.report.reset_unistats()
            for ext_snippet in (1, 2, 3):
                self.report.request_json(
                    'place=print_doc&text=cisco | samsung&rearr-factors=ext_snippet={};ext_snippet_bulk=1'.format(
                        ext_snippet
                    )
                )
                if self.snippets.global_cache_size == 0:
                    response = self.report.request_tass_or_wait(wait_hole='Market_snippets_2xx_dmmm')
                    self.assertIn("Market_snippets_2xx_dmmm", response)
                    self.assertNotIn("market_snippets_error_dmmm", response)

        self.__retry_action(action)

    @classmethod
    def prepare_disable_arc_snippets(cls):
        cls.index.offers += [Offer(title='arc_offer')]

    def test_disable_arc_snippets(self):
        """
        Делаем три запроса:
        1. первый делает запрос в архив за сниппетами,
        2. второй пытается не делать запрос в архив, но у него не получается,
        3. третий не делает запрос в архив.
        После каждого запроса проверяем ходилили ли в архив
        """

        def action():
            self.report.reset_unistats()
            self.report.request_json('place=print_doc&text=arc_offer&rearr-factors=no_snippet_arc=0;ext_snippet=0')
            response = self.report.request_tass()
            self.assertGreater(response.get('unanswer-snippets-denom_dmmm'), 0)
            self.report.reset_unistats()

            self.report.request_json('place=print_doc&text=arc_offer&rearr-factors=no_snippet_arc=1;ext_snippet=0')
            response = self.report.request_tass()
            # Мы все равно идем в архив, потому что no_snippet_arc=1 не работает без ext_snippets=1
            self.assertGreater(response.get('unanswer-snippets-denom_dmmm'), 0)
            self.error_log.expect(code=ErrorCodes.EXTREQUEST_SAASKV_SNIPPETS_WRONG_SETTINGS).once()
            self.report.reset_unistats()

            self.report.request_json('place=print_doc&text=arc_offer&rearr-factors=no_snippet_arc=1;ext_snippet=1')
            response = self.report.request_tass()
            self.assertNotIn('unanswer-snippets-denom_dmmm', response)

        if self.snippets.global_cache_size == 0:
            self.__retry_action(action)

    @classmethod
    def prepare_cards_snippets(cls):
        cls.index.navtree += [
            NavCategory(nid=213, name='good', short_name='marvellous', uniq_name='awesome'),
            NavCategory(nid=255, hid=309, is_blue=0),
        ]

        cls.index.cards += [
            CardNavCategory(
                nid=213, snippet_data={'description': 'snippet_descr_awesome', 'site_url': 'snippet_url_awesome'}
            ),
            CardVendor(
                vendor_id=10, snippet_data={'description': 'snippet_descr_somsung', 'site_url': 'snippet_url_somsung'}
            ),
            CardVendor(
                visual=True,
                vendor_id=1337,
                snippet_data={'description': 'snippet_descr_ananas', 'site_url': 'snippet_url_ananas'},
            ),
            CardVendor(visual=True, vendor_id=99),
            CardCategory(
                hid=309,
                snippet_data={'description': 'snippet_descr_electrocars', 'site_url': 'snippet_url_electrocars'},
            ),
            CardCategory(
                hid=310, snippet_data={'description': 'snippet_descr_spacex', 'site_url': 'snippet_url_spacex'}
            ),
            CardCategoryVendor(
                vendor_id=11,
                hid=311,
                snippet_data={'description': 'snippet_descr_lego', 'site_url': 'snippet_url_lego'},
            ),
            CardCategoryVendor(
                vendor_id=12,
                hid=312,
                snippet_data={'description': 'snippet_descr_mixer', 'site_url': 'snippet_url_mixer'},
            ),
        ]

        cls.index.vendors += [
            Vendor(vendor_id=10, aliases=['Somsung', 'Samsung'], name='Samsung'),
            Vendor(vendor_id=11, name='lego'),
            Vendor(vendor_id=12, name='mixer'),
            Vendor(vendor_id=1337, name='ananas'),
            Vendor(vendor_id=99, name='yandex', is_fake=True),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=309, output_type=HyperCategoryType.GURU, visual=False, name='electrocars'),
            HyperCategory(hid=310, output_type=HyperCategoryType.GURU, visual=True, name='spacex'),
            HyperCategory(hid=311, output_type=HyperCategoryType.GURU, visual=False, name='technic'),
            HyperCategory(hid=312, output_type=HyperCategoryType.GURU, visual=True, name='rexim'),
        ]

    def test_cards_snippets(self):
        """
        Проверяем, что данные из карточной колекции приходят из внешних сниппетов
        """

        # guru vendor card
        query = 'place=print_doc&text=Somsung'
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'doc_type': 'vendor',
                'title': 'Samsung',
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'site_url': 'snippet_url_somsung',
                'description': 'snippet_descr_somsung',
            },
        )

        query = 'place=print_doc&text=Somsung&rearr-factors=ext_snippet=1'
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'doc_type': 'vendor',
                'title': 'Samsung',
                'properties': {
                    'site_url': 'snippet_url_somsung',
                    'description': 'snippet_descr_somsung',
                },
            },
        )

        # visual vendor card
        query = 'place=print_doc&text=ananas'
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'doc_type': 'visual_card',
                'title': 'ananas',
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'site_url': 'snippet_url_ananas',
                'description': 'snippet_descr_ananas',
            },
        )

        query = 'place=print_doc&text=ananas&rearr-factors=ext_snippet=1'
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'doc_type': 'visual_card',
                'title': 'ananas',
                'properties': {
                    'site_url': 'snippet_url_ananas',
                    'description': 'snippet_descr_ananas',
                },
            },
        )

        # guru category card
        query = 'place=print_doc&text=electrocars'
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'doc_type': 'category',
                'title': 'electrocars',
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'site_url': 'snippet_url_electrocars',
                'description': 'snippet_descr_electrocars',
            },
        )

        query = 'place=print_doc&text=electrocars&rearr-factors=ext_snippet=1;'
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'doc_type': 'category',
                'title': 'electrocars',
                'properties': {'site_url': 'snippet_url_electrocars', 'description': 'snippet_descr_electrocars'},
            },
        )

        # visual category card
        query = 'place=print_doc&text=spacex'
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'doc_type': 'visual_card',
                'title': 'spacex',
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'site_url': 'snippet_url_spacex',
                'description': 'snippet_descr_spacex',
            },
        )

        query = 'place=print_doc&text=spacex&rearr-factors=ext_snippet=1;'
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'doc_type': 'visual_card',
                'title': 'spacex',
                'properties': {
                    'site_url': 'snippet_url_spacex',
                    'description': 'snippet_descr_spacex',
                },
            },
        )

        # guru category-vendor card
        query = 'place=print_doc&text=lego'
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'doc_type': 'category_vendor',
                'title': 'lego technic',
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'site_url': 'snippet_url_lego',
                'description': 'snippet_descr_lego',
            },
        )

        query = 'place=print_doc&text=lego&rearr-factors=ext_snippet=1;'
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'doc_type': 'category_vendor',
                'title': 'lego technic',
                'properties': {
                    'site_url': 'snippet_url_lego',
                    'description': 'snippet_descr_lego',
                },
            },
        )

        # visual category-vendor card
        query = 'place=print_doc&text=mixer'
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'doc_type': 'visual_card',
                'title': 'mixer rexim',
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'site_url': 'snippet_url_mixer',
                'description': 'snippet_descr_mixer',
            },
        )

        query = 'place=print_doc&text=mixer&rearr-factors=ext_snippet=1;'
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'doc_type': 'visual_card',
                'title': 'mixer rexim',
                'properties': {
                    'site_url': 'snippet_url_mixer',
                    'description': 'snippet_descr_mixer',
                },
            },
        )

        # navigation card
        query = 'place=print_doc&text=awesome'
        response = self.report.request_json(query)
        self.assertFragmentIn(response, {'doc_type': 'navigation_virtual_card'})
        self.assertFragmentNotIn(
            response,
            {
                'site_url': 'snippet_url_awesome',
                'description': 'snippet_descr_awesome',
            },
        )

        query = 'place=print_doc&text=awesome&rearr-factors=ext_snippet=1;'
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'doc_type': 'navigation_virtual_card',
                'properties': {
                    'site_url': 'snippet_url_awesome',
                    'description': 'snippet_descr_awesome',
                },
            },
        )

    @classmethod
    def prepare_wizard_snippets(cls):
        cls.index.categories += [
            Category(
                hyper_id=1000,
                name='tomatos',
                snippet_data={
                    'site_url': 'snippet_url_tomatos',
                    '_Title': 'snippet_title_tomatos',
                },
            ),
            Category(
                hyper_id=1001,
                name='potatos',
                visual=True,
                snippet_data={
                    'site_url': 'snippet_url_potatos',
                    '_Title': 'snippet_title_potatos',
                },
            ),
        ]

    def test_wizard_snippets(self):
        # non-visual catalog card
        query = 'place=print_doc&text=tomatos'
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'doc_type': 'wizard',
                'title': 'tomatos',
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'site_url': 'snippet_url_tomatos',
            },
        )

        query = 'place=print_doc&text=tomatos&rearr-factors=ext_snippet=1;'
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'doc_type': 'wizard',
                'title': 'snippet_title_tomatos',
                'properties': {
                    'site_url': 'snippet_url_tomatos',
                },
            },
        )

        # visual catalog card
        query = 'place=print_doc&text=potatos'
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'doc_type': 'wizard',
                'title': 'potatos',
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'site_url': 'snippet_url_potatos',
            },
        )

        query = 'place=print_doc&text=potatos&rearr-factors=ext_snippet=1;'
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'doc_type': 'wizard',
                'title': 'snippet_title_potatos',
                'properties': {
                    'site_url': 'snippet_url_potatos',
                },
            },
        )

    def test_pruning_no_robot(self):
        rules = [{'conditions': ['is_antirobot_degradation=1'], 'actions': ['prun_snippets=0']}]
        rearr = "rearr-factors=ext_snippet=1;graceful_degradation_rules={}".format(json.dumps(rules))

        self.report.request_json('place=prime&text=not_pruned&' + rearr)
        self.access_log.expect(
            external_snippet_stall_time_ms=GreaterEq(0),
            snippet_requests_made=1,
            snippets_fetched=2,
        )

    def test_pruning_robot(self):
        rules = [{'conditions': ['is_antirobot_degradation=1'], 'actions': ['prun_snippets=0']}]
        rearr = "rearr-factors=ext_snippet=1;graceful_degradation_rules={}".format(json.dumps(rules))
        antirobot_degradation_headers = {'X-Yandex-Antirobot-Degradation': '1.0'}

        self.report.request_json('place=prime&text=pruned&' + rearr, headers=antirobot_degradation_headers)
        self.access_log.expect(
            snippet_requests_made=0,
            snippets_fetched=0,
        )

        if self.snippets.global_cache_size == 0:
            rules = [{'conditions': ['is_antirobot_degradation=1'], 'actions': ['prun_snippets=1']}]
            rearr = "rearr-factors=ext_snippet=1;graceful_degradation_rules={}".format(json.dumps(rules))

            self.report.request_json('place=prime&text=pruned&' + rearr, headers=antirobot_degradation_headers)
            self.access_log.expect(
                snippet_requests_made=1,
                snippets_fetched=1,
            )

            rules = [{'conditions': ['is_antirobot_degradation=1'], 'actions': ['prun_snippets=3']}]
            rearr = "rearr-factors=ext_snippet=1;graceful_degradation_rules={}".format(json.dumps(rules))

            self.report.request_json('place=prime&text=pruned&' + rearr, headers=antirobot_degradation_headers)
            self.access_log.expect(
                snippet_requests_made=1,
                snippets_fetched=2,
            )

    def test_pruning_quality(self):
        rules = [{'conditions': ['snippets_quality_from=10000'], 'actions': ['prun_snippets=0']}]
        rearr = (
            "rearr-factors=ext_snippet=1;ext_snippet_fetch_force_quality=10000;graceful_degradation_rules={}".format(
                json.dumps(rules)
            )
        )

        self.report.request_json('place=prime&text=pruned&' + rearr)
        self.access_log.expect(
            snippet_requests_made=0,
            snippets_fetched=0,
        )
        if self.snippets.global_cache_size == 0:
            rearr = (
                "rearr-factors=ext_snippet=1;ext_snippet_fetch_force_quality=500;graceful_degradation_rules={}".format(
                    json.dumps(rules)
                )
            )

            self.report.request_json('place=prime&text=pruned&' + rearr)
            self.access_log.expect(
                snippet_requests_made=1,
                snippets_fetched=2,
            )

    def test_reduce_retry_robot(self):
        rules = [{'conditions': ['is_antirobot_degradation=1'], 'actions': ['snippets_retry=0']}]
        rearr = "rearr-factors=ext_snippet=1;ext_snippet_retry_count=2;graceful_degradation_rules={}".format(
            json.dumps(rules)
        )
        antirobot_degradation_headers = {'X-Yandex-Antirobot-Degradation': '1.0'}

        response = self.report.request_json('place=prime&text=iphone&' + rearr, headers=antirobot_degradation_headers)
        self.assertFragmentIn(response, {'raw': 'snippet-phone'})

        response = self.report.request_json('place=prime&text=iphone&' + rearr)
        self.assertFragmentIn(response, {'raw': 'snippet-phone'})

    def test_pruning_stopkran(self):
        rearr = "rearr-factors=ext_snippet=1;ext_snippet_fetch_prun=0"
        self.report.request_json('place=prime&text=pruned&' + rearr)
        self.access_log.expect(
            snippet_requests_made=0,
            snippets_fetched=0,
        )
        if self.snippets.global_cache_size == 0:
            rearr = "rearr-factors=ext_snippet=1;ext_snippet_fetch_prun=1"
            self.report.request_json('place=prime&text=pruned&' + rearr)
            self.access_log.expect(
                snippet_requests_made=1,
                snippets_fetched=1,
            )

            rearr = "rearr-factors=ext_snippet=1;ext_snippet_fetch_prun=3"
            self.report.request_json('place=prime&text=pruned&' + rearr)
            self.access_log.expect(snippet_requests_made=1, snippets_fetched=2)

    def test_pruning_stopkran_antirobot(self):
        rearr = "rearr-factors=ext_snippet=1;ext_snippet_fetch_prun_antirobot=0"
        antirobot_degradation_headers = {'X-Yandex-Antirobot-Degradation': '1.0'}
        self.report.request_json('place=prime&text=pruned&' + rearr, headers=antirobot_degradation_headers)
        self.access_log.expect(
            snippet_requests_made=0,
            snippets_fetched=0,
        )
        if self.snippets.global_cache_size == 0:
            rearr = "rearr-factors=ext_snippet=1;ext_snippet_fetch_prun_antirobot=1"
            self.report.request_json('place=prime&text=pruned&' + rearr, headers=antirobot_degradation_headers)
            self.access_log.expect(
                snippet_requests_made=1,
                snippets_fetched=1,
            )

            rearr = "rearr-factors=ext_snippet=1;ext_snippet_fetch_prun_antirobot=3"
            self.report.request_json('place=prime&text=pruned&' + rearr, headers=antirobot_degradation_headers)
            self.access_log.expect(snippet_requests_made=1, snippets_fetched=2)

    def test_access_log_stats(self):
        '''Проверяем, что в access.log пишется статистика по снипетам'''
        self.report.request_json('place=print_doc&text=nokia&rearr-factors=ext_snippet=1')
        self.access_log.expect(
            external_snippet_stall_time_ms=GreaterEq(0),
            snippet_requests_made=1,
            snippets_fetched=1,
        )

    def test_trace_log(self):
        if self.snippets.global_cache_size == 0:
            '''Проверяем, что в external_services_trace.log пишется source_module'''
            self.report.request_json('place=print_doc&text=nokia&rearr-factors=ext_snippet=1')
            self.external_services_trace_log.expect(source_module="market-report")

    @staticmethod
    def __retry_action(action, count=3, wait_time=0.4):
        for retry_num in range(0, count):
            try:
                return action()
            except Exception as exc:
                sys.stderr.write('Exception in __retry_action: {}\n'.format(str(exc)))
                if retry_num == count - 1:
                    raise
                time.sleep(wait_time)


if __name__ == '__main__':
    main()
