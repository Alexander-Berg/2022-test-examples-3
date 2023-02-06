#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa
from core.testcase import TestCase, main
from core.types import GLType, HyperCategory, HyperCategoryType, NavCategory

from core.bots import BOT_USER_AGENTS


def pruning(collection, *pron):
    return {"debug": {"report": {"context": {"collections": {collection: {"pron": list(pron)}}}}}}


def pruning_sub(collection, *pron):
    return {
        "debug": {
            "metasearch": {
                "subrequests": [{"report": {"context": {"collections": {collection: {"pron": list(pron)}}}}}]
            }
        }
    }


class T(TestCase):
    """Здесь собраны тесты которые проверяют какие топы (прюнинг, tbs, smm) будет установлен для разных шардов при разных входных параметрах
    тесты в большинстве своем не должны зависеть от наличия документов в индексе"""

    @classmethod
    def prepare(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=13333,
                output_type=HyperCategoryType.SIMPLE,
                children=[
                    HyperCategory(
                        hid=1333,
                        output_type=HyperCategoryType.SIMPLE,
                        children=[
                            HyperCategory(
                                hid=133,
                                output_type=HyperCategoryType.SIMPLE,
                                children=[HyperCategory(hid=13, output_type=HyperCategoryType.GURU, has_groups=True)],
                            )
                        ],
                    )
                ],
            )
        ]

        cls.index.gltypes += [
            GLType(param_id=300, cluster_filter=True, hid=13, gltype=GLType.ENUM, values=[11, 13, 15]),
            GLType(param_id=301, cluster_filter=True, hid=13, gltype=GLType.NUMERIC),
            GLType(param_id=302, hid=13, gltype=GLType.ENUM, values=[19, 21]),
            GLType(param_id=7893318, hid=13, gltype=GLType.ENUM, values=[1, 2, 3, 4, 5, 6], vendor=True),
        ]

    def test_default_tbs(self):
        response = self.report.request_json('text=laptop&hid=13&place=prime&debug=1')
        self.assertFragmentIn(response, pruning('*', 'tbs200000', 'pruncount53334'))

        response = self.report.request_json(
            'place=prime&text=laptop&rearr-factors=tbs-value=1000;tbs-version=1;tbs-to-tbh-mul=2.5&debug=da'
        )
        self.assertFragmentIn(response, pruning('*', 'versiontbs1', 'mul_tbs_to_tbh2.5', 'tbs1000'))

    def test_smm(self):
        def get_expected_smm(value):
            return {
                "debug": {
                    "report": {
                        "context": {
                            "collections": {
                                "BOOK": {"pron": ["smm_" + str(value)]},
                                "MODEL": {"pron": ["smm_" + str(value)]},
                                "PREVIEW_MODEL": {"pron": ["smm_" + str(value)]},
                                "SHOP": {"pron": ["smm_" + str(value)]},
                            }
                        }
                    }
                }
            }

        response = self.report.request_json('place=prime&text=laptop&debug=da')
        self.assertFragmentIn(response, get_expected_smm(1))
        response = self.report.request_json('place=prime&text=laptop&debug=da&rearr-factors=smm=1.0')
        self.assertFragmentIn(response, get_expected_smm(1))
        response = self.report.request_json('place=prime&text=laptop&debug=da&rearr-factors=smm=0.8')
        self.assertFragmentIn(response, get_expected_smm(0.8))
        response = self.report.request_json('place=prime&hid=13&debug=da&rearr-factors=smm=0.5')
        self.assertFragmentIn(response, pruning("*", "smm_0.5"))

        response = self.report.request_json(
            'place=prime&text=laptop&debug=da&rearr-factors=smm=0.7&rearr-factors=market_smm_filters=text_all'
        )
        self.assertFragmentIn(response, get_expected_smm(0.7))
        response = self.report.request_json(
            'place=prime&text=laptop&debug=da&rearr-factors=smm=0.7&rearr-factors=market_smm_filters=notext_all'
        )
        self.assertFragmentIn(response, get_expected_smm(1))
        response = self.report.request_json(
            'place=prime&hid=13&debug=da&rearr-factors=smm=0.7&rearr-factors=market_smm_filters=notext_all'
        )
        self.assertFragmentIn(response, pruning("*", "smm_0.7"))

        response = self.report.request_json(
            'place=prime&text=laptop&debug=da&rearr-factors=smm=0.7&rearr-factors=market_smm_filters=qqw,3232,dsd'
        )
        self.assertFragmentIn(response, get_expected_smm(1))
        response = self.report.request_json(
            'place=prime&text=laptop&debug=da&rearr-factors=smm=0.7&rearr-factors=market_smm_filters=qqw,dsd,prime,asa'
        )
        self.assertFragmentIn(response, get_expected_smm(0.7))

        headers = {'X-Antirobot-Suspiciousness-Y': '1.0'}
        response = self.report.request_json(
            'place=prime&text=laptop&debug=da&rearr-factors=smm=0.7&rearr-factors=market_smm_filters=suspicious',
            headers=headers,
        )
        self.assertFragmentIn(response, get_expected_smm(0.7))

        headers = {'X-Yandex-Antirobot-Degradation': '1.0'}
        response = self.report.request_json(
            'place=prime&text=laptop&debug=da&rearr-factors=smm=0.7&rearr-factors=market_smm_filters=antirobot_degradation',
            headers=headers,
        )
        self.assertFragmentIn(response, get_expected_smm(0.7))

    def test_pruning_for_departments(self):
        # прюнинг для департаментов (MARKETOUT-31706)
        pruning_departments = pruning("*", "prune", "pruncount667")  # 1000 * 2 / 3

        # департамент, нет текста
        # прюнинг включается
        response = self.report.request_json('place=prime&hid=13333&debug=da')
        self.assertFragmentIn(response, pruning_departments)

        # департамент, есть текст
        # прюнинг НЕ включается
        response = self.report.request_json('place=prime&hid=13333&debug=da&text=iphone')
        self.assertFragmentNotIn(response, pruning_departments)

        # не департамент, нет текста
        # прюнинг НЕ включается
        response = self.report.request_json('place=prime&hid=1333&debug=da&text=iphone')
        self.assertFragmentNotIn(response, pruning_departments)

    def test_forcing_pruning_for_non_leaf_categories(self):
        # прюнинг для нелистовых (MARKETOUT-31772)
        pruning_non_leaf_force = pruning("*", "prune", "pruncount667")  # 1000 * 2 / 3
        pruning_non_leaf_force2000 = pruning("*", "prune", "pruncount1334")  # 2000 * 2 / 3

        # нелистовая, флаг = 1000
        # прюнинг включается
        response = self.report.request_json(
            'place=prime&hid=1333&text=hello&debug=da&rearr-factors=market_force_non_leaf_prun_count=1000'
        )
        self.assertFragmentIn(response, pruning_non_leaf_force)

        # нелистовая, флаг = 2000
        # прюнинг включается
        response = self.report.request_json(
            'place=prime&hid=1333&text=hello&debug=da&rearr-factors=market_force_non_leaf_prun_count=2000'
        )
        self.assertFragmentIn(response, pruning_non_leaf_force2000)

        # листовая, флаг не указан
        # прюнинг НЕ включается
        response = self.report.request_json('place=prime&hid=13&text=hello&debug=da')
        self.assertFragmentNotIn(response, pruning_non_leaf_force)

        # листовая, флаг = 0
        # прюнинг НЕ включается
        response = self.report.request_json(
            'place=prime&hid=13&text=hello&debug=da&rearr-factors=market_force_non_leaf_prun_count=0'
        )
        self.assertFragmentNotIn(response, pruning_non_leaf_force)

        # листовая, флаг = 1000
        # прюнинг НЕ включается
        response = self.report.request_json(
            'place=prime&hid=13&text=hello&debug=da&rearr-factors=market_force_non_leaf_prun_count=1000'
        )
        self.assertFragmentNotIn(response, pruning_non_leaf_force)

    def test_forcing_pruning_for_leaf_categories(self):
        # прюнинг для нелистовых (MARKETOUT-31799)
        pruning_leaf_force = pruning("*", "prune", "pruncount600")  # 900 * 2 / 3
        pruning_leaf_force2000 = pruning("*", "prune", "pruncount1334")  # 2000 * 2 / 3

        # Дефолтное значение для прюнинга не листовой категории
        pruning_non_leaf = pruning("*", "prune", "pruncount667")  # 1000 * 2 / 3

        # листовая, флаг не указан
        # прюнинг НЕ включается
        response = self.report.request_json('place=prime&hid=13&debug=da')
        self.assertFragmentNotIn(response, pruning_leaf_force)

        # листовая, флаг = 0
        # прюнинг НЕ включается
        response = self.report.request_json('place=prime&hid=13&debug=da&rearr-factors=market_force_leaf_prun_count=0')
        self.assertFragmentNotIn(response, pruning_leaf_force)

        # листовая, флаг = 900
        # прюнинг включается
        response = self.report.request_json(
            'place=prime&hid=13&debug=da&rearr-factors=market_force_leaf_prun_count=900'
        )
        self.assertFragmentIn(response, pruning_leaf_force)

        # листовая, флаг = 2000
        # прюнинг включается
        response = self.report.request_json(
            'place=prime&hid=13&debug=da&rearr-factors=market_force_leaf_prun_count=2000'
        )
        self.assertFragmentIn(response, pruning_leaf_force2000)

        # не листовая, флаг не указан
        # прюнинг дефолтный
        response = self.report.request_json('place=prime&hid=1333&debug=da')
        self.assertFragmentIn(response, pruning_non_leaf)

        # не листовая, флаг = 0
        # прюнинг дефолтный
        response = self.report.request_json(
            'place=prime&hid=1333&debug=da&rearr-factors=market_force_leaf_prun_count=0'
        )
        self.assertFragmentIn(response, pruning_non_leaf)

        # не листовая, флаг = 2000
        # прюнинг дефолтный
        response = self.report.request_json(
            'place=prime&hid=1333&debug=da&rearr-factors=market_force_leaf_prun_count=2000'
        )
        self.assertFragmentIn(response, pruning_non_leaf)

    def test_pruning_not_rkub(self):
        # прюнинг зарубежных IP https://st.yandex-team.ru/MARKETOUT-31773
        pruning_rkub334 = pruning("*", "prune", "pruncount334")  # 500 * 2 / 3

        # kubr=0:

        # флага нет
        # прюнинг НЕ включается
        response = self.report.request_json('place=prime&hid=13&debug=da&ip=fake_foreign_ip')
        self.assertFragmentNotIn(response, pruning_rkub334)

        # флаг = 0
        # прюнинг НЕ включается
        response = self.report.request_json(
            'place=prime&hid=13&debug=da&ip=fake_foreign_ip&rearr-factors=market_foreign_ips_prun_count=0'
        )
        self.assertFragmentNotIn(response, pruning_rkub334)

        # флаг = 500
        # прюнинг включается
        response = self.report.request_json(
            'place=prime&hid=13&debug=da&ip=fake_foreign_ip&rearr-factors=market_foreign_ips_prun_count=500'
        )
        self.assertFragmentIn(response, pruning_rkub334)

        # флаг = 500 + гугл бот
        # прюнинг НЕ включается
        response = self.report.request_json(
            'place=prime&hid=13&debug=da&ip=fake_foreign_ip&rearr-factors=market_foreign_ips_prun_count=500',
            headers={"User-Agent": "Mozilla/5.0 (compatible; Googlebot/2.0; +http://www.google.com/bot.html)"},
        )
        self.assertFragmentNotIn(response, pruning_rkub334)

        # kubr=1: прюнинг не включается
        # флага нет
        # прюнинг НЕ включается
        response = self.report.request_json('place=prime&hid=13&debug=da&ip=74.125.205.100')
        self.assertFragmentNotIn(response, pruning_rkub334)

        # флаг = 500
        # прюнинг НЕ включается
        response = self.report.request_json(
            'place=prime&hid=13&debug=da&ip=74.125.205.100&rearr-factors=market_foreign_ips_prun_count=500'
        )
        self.assertFragmentNotIn(response, pruning_rkub334)

        # kubr не указан - поведение как с kubr=1
        # флага нет
        # прюнинг НЕ включается
        response = self.report.request_json('place=prime&hid=13&debug=da')
        self.assertFragmentNotIn(response, pruning_rkub334)

        # флаг = 500
        # прюнинг НЕ включается
        response = self.report.request_json(
            'place=prime&hid=13&debug=da&rearr-factors=market_foreign_ips_prun_count=500'
        )
        self.assertFragmentNotIn(response, pruning_rkub334)

    def test_pruning_for_early_gl_filtering(self):
        """Если в бестекстовом запросе указан fesh или glfilter
        Или в текстовом запросе указан fesh или glfilter=vendor
        то делается 2 запроса:
        один за документами другой за фильтрами
        Запрос за документами идет с прюнингом
        Запрос за фильтрами идет без прюнинга
        """

        _ = pruning("*", "prune")
        prunging_docs = 'Pruning recommend no more then: 5000 selected from: 5000 - subrequest for documents (main) in case separate filters'
        pruning_enabled3334 = pruning("*", "prune", "pruncount3334")  # 5000 * 2/3

        # прюнинг market_prime_prun_count_when_using_prime_filters=500 когда в запросе есть fesh
        response = self.report.request_json('place=prime&text=laptop&fesh=1&debug=da')
        self.assertFragmentIn(response, 'Make additional request for glfilters')
        self.assertFragmentIn(response, prunging_docs)
        self.assertFragmentIn(response, pruning_enabled3334)

        response = self.report.request_json('place=prime&fesh=1&hid=13&debug=da')
        self.assertFragmentIn(response, 'Make additional request for glfilters')
        self.assertFragmentIn(response, prunging_docs)
        self.assertFragmentIn(response, pruning_enabled3334)

        # прюнинг market_prime_prun_count_when_using_prime_filters=500 когда в запросе есть glfilter=vendor
        response = self.report.request_json('place=prime&text=laptop&hid=13&glfilter=7893318:1&debug=da')
        self.assertFragmentIn(response, 'Make additional request for glfilters')
        self.assertFragmentIn(response, prunging_docs)
        self.assertFragmentIn(response, pruning_enabled3334)

        response = self.report.request_json('place=prime&hid=13&glfilter=7893318:1&debug=da')
        self.assertFragmentIn(response, 'Make additional request for glfilters')
        self.assertFragmentIn(response, prunging_docs)
        self.assertFragmentIn(response, pruning_enabled3334)

        # прюнинг market_prime_prun_count_when_using_prime_filters=500 когда в запросе есть glfilter (только на бестексте)
        response = self.report.request_json('place=prime&text=laptop&hid=13&glfilter=300:11&debug=da')
        # ранняя фильтрация может быть включена на тексте но прюнинг не включится
        self.assertFragmentIn(response, 'Make additional request for glfilters')
        self.assertFragmentIn(response, prunging_docs)
        self.assertFragmentIn(response, pruning_enabled3334)

        response = self.report.request_json('place=prime&hid=13&glfilter=300:11&debug=da')
        self.assertFragmentIn(response, 'Make additional request for glfilters')
        self.assertFragmentIn(response, prunging_docs)
        self.assertFragmentIn(response, pruning_enabled3334)

        # для запроса содержащего только fesh введен прюнинг как на prime
        response = self.report.request_json('place=prime&fesh=1&debug=da')
        self.assertFragmentNotIn(response, 'Make additional request for glfilters')
        self.assertFragmentIn(response, 'Pruning recommend no more then: 5000 selected from: 5000 - search by fesh')
        self.assertFragmentIn(response, pruning_enabled3334)

        # для запросов содержащих только hid подзапрос за фильтрами не используется
        response = self.report.request_json('place=prime&hid=13&pp=18&debug=da')
        self.assertFragmentNotIn(response, 'Make additional request for glfilters')
        self.assertFragmentNotIn(response, prunging_docs)

        response = self.report.request_json('place=prime&hid=13&pp=18&debug=da&rgb=blue')
        self.assertFragmentNotIn(response, 'Make additional request for glfilters')
        self.assertFragmentNotIn(response, prunging_docs)

        # флаг market_prime_prun_count_when_using_prime_filters=100 выставляет прюнинг для запроса за документами (основного)
        # флаг market_prime_prun_count_for_prime_filters_subrequest=10000 выставляет прюнинг для запроса за фильтрами (подзапроса с isPrimeFiltersLogic=True)
        response = self.report.request_json('place=prime&hid=13&glfilter=300:11&debug=da')
        self.assertFragmentIn(response, 'Make additional request for glfilters')
        self.assertFragmentIn(response, prunging_docs)
        self.assertFragmentIn(response, pruning_enabled3334)
        # подзапрос за фильтрами не прюнится по дефолту

        pruning_100 = pruning("*", "prune", "pruncount67")  # 100 * 2/3
        pruning_10000_sub = pruning_sub("*", "prune", "pruncount6667")  # 10000 * 2/3
        r = '&rearr-factors=market_prime_prun_count_when_using_prime_filters=100;market_prime_prun_count_for_prime_filters_subrequest=10000'
        response = self.report.request_json('place=prime&hid=13&glfilter=300:11&debug=da' + r)
        self.assertFragmentIn(response, 'Make additional request for glfilters')
        self.assertFragmentIn(response, pruning_100)
        self.assertFragmentIn(
            response,
            'Pruning recommend no more then: 100 selected from: 100 - subrequest for documents (main) in case separate filters',
        )
        self.assertFragmentIn(response, pruning_10000_sub)
        self.assertFragmentIn(
            response,
            'Pruning recommend no more then: 10000 selected from: 10000 - subrequest for filters in case separate filters',
        )

        # флаг market_panther_coef_when_using_prime_filters умножает все panther_top_size пропорционально для запроса за документами (основного)
        # market_panther_coef_for_prime_filters_subrequest умножает все panther_top_size пропорционально для запроса за фильтрами (подзапроса)
        # оба флага могут принимать несколько значений для 0 gl-фильтров, 1, 2, 3+ и т.д

        panther250 = pruning("SHOP", "panther_top_size_=250")
        panther250_sub = pruning("SHOP", "panther_top_size_=250")
        response = self.report.request_json(
            'place=prime&text=laptop&hid=13&glfilter=300:11&debug=da'
            '&rearr-factors=market_early_pre_early_gl_filtering=1'
        )
        self.assertFragmentIn(response, 'Make additional request for glfilters')
        self.assertFragmentIn(response, panther250)
        self.assertFragmentIn(response, panther250_sub)

        r = '&rearr-factors=market_panther_coef_when_using_prime_filters=0.5;market_panther_coef_for_prime_filters_subrequest=2.0'
        panther125 = pruning("SHOP", "panther_top_size_=125")
        panther500_sub = pruning_sub("SHOP", "panther_top_size_=500")
        response = self.report.request_json(
            'place=prime&text=laptop&hid=13&glfilter=300:11&debug=da'
            '&rearr-factors=market_early_pre_early_gl_filtering=1' + r
        )
        self.assertFragmentIn(response, 'Make additional request for glfilters')
        self.assertFragmentIn(response, panther125)
        self.assertFragmentIn(response, panther500_sub)

        r = '&rearr-factors=market_panther_coef_when_using_prime_filters=1.0,0.5,0.25;market_panther_coef_for_prime_filters_subrequest=2.0,2.0,4.0'
        panther62 = pruning("SHOP", "panther_top_size_=62")
        panther1000_sub = pruning_sub("SHOP", "panther_top_size_=1000")

        # 0 фильтров
        response = self.report.request_json(
            'place=prime&text=laptop&hid=13&fesh=1&debug=da' '&rearr-factors=market_early_pre_early_gl_filtering=1' + r
        )
        self.assertFragmentIn(response, 'Make additional request for glfilters')
        self.assertFragmentIn(response, panther250)
        self.assertFragmentIn(response, panther250_sub)

        # 1 фильтр
        response = self.report.request_json(
            'place=prime&text=laptop&hid=13&fesh=1&glfilter=300:11&debug=da'
            '&rearr-factors=market_early_pre_early_gl_filtering=1' + r
        )
        self.assertFragmentIn(response, 'Make additional request for glfilters')
        self.assertFragmentIn(response, 'Gurulight filters except vendor in request: 1')
        self.assertFragmentIn(response, panther125)
        self.assertFragmentIn(response, panther500_sub)

        # 2 фильтра
        response = self.report.request_json(
            'place=prime&text=laptop&hid=13&fesh=1&glfilter=300:11&glfilter=301:4~&debug=da'
            '&rearr-factors=market_early_pre_early_gl_filtering=1' + r
        )
        self.assertFragmentIn(response, 'Make additional request for glfilters')
        self.assertFragmentIn(response, 'Gurulight filters except vendor in request: 2')
        self.assertFragmentIn(response, panther62)
        self.assertFragmentIn(response, panther1000_sub)

        # 3+ фильтров
        response = self.report.request_json(
            'place=prime&text=laptop&hid=13&fesh=1&glfilter=300:11&glfilter=301:4~&glfilter=302:19&debug=da'
            '&rearr-factors=market_early_pre_early_gl_filtering=1' + r
        )
        self.assertFragmentIn(response, 'Make additional request for glfilters')
        self.assertFragmentIn(response, 'Gurulight filters except vendor in request: 3')
        self.assertFragmentIn(response, panther62)
        self.assertFragmentIn(response, panther1000_sub)

    def test_pruning(self):
        """надо распилить этот тест на отдельные"""
        pruning_enabled = pruning("*", "prune")

        pruning_enabled667 = pruning("*", "prune", "pruncount667")  # 1000 * 2/3
        pruning_enabled6667 = pruning("*", "prune", "pruncount6667")  # 10000 * 2/3

        pruning_enabled3334 = pruning("*", "prune", "pruncount3334")  # 5000 * 2/3

        pruning_enabled66667 = pruning("*", "prune", "pruncount53334")  # 80000 * 2 / 3

        pruning_enabled_for_non_leaf_category = pruning_enabled66667
        pruning_default = pruning_enabled66667

        response = self.report.request_json('place=prime&text=laptop&debug=da')
        self.assertFragmentIn(response, pruning_enabled_for_non_leaf_category)

        # на следующих запросах действует только market_prime_prun_count_upper_limit=100000 по умолчанию
        r = '&rearr-factors=market_prime_prun_count_upper_limit=0'  # его можно отключить передав 0
        for query in [
            'place=prime&hid=13&text=laptop&debug=da',
            'place=prime&text=laptop&how=aprice&debug=da',
            'place=prime&hid=13&text=laptop&how=aprice&debug=da',
            'place=prime&hid=13&how=aprice&debug=da',
            'place=prime&offerid=K1FqLT-6OAgdBarkPIoY-Q&text=laptop&debug=da',
            'place=prime&hyperid=4000&text=laptop&debug=da',
            'place=prime&vclusterid=1369268158&text=блузка&debug=da',
        ]:

            response = self.report.request_json(query)
            self.assertFragmentIn(response, pruning_default)
            response = self.report.request_json(query + r)
            self.assertFragmentNotIn(response, pruning_enabled)

        response = self.report.request_json('place=prime&hid=13&debug=da&prun-count=10000')
        self.assertFragmentIn(response, pruning_enabled6667)

        # прюнинг при текстовом поиске на нелистовых категориях по умолчанию 100K
        response = self.report.request_json('place=prime&hid=133&text=текст&debug=da')
        self.assertFragmentIn(response, pruning_enabled66667)

        # флаг market_hard_prun_non_leaf_category_on_empty_search=1000 задает прюнинг на нелистовых категориях
        # (только на бестексте если запрос идет без литералов по магазину, поставщику, или експресс-доставке)
        # на белом он включен по умолчанию
        response = self.report.request_json('place=prime&hid=133&debug=da')
        self.assertFragmentIn(response, pruning_enabled667)
        response = self.report.request_json('place=prime&hid=1333&debug=da')
        self.assertFragmentIn(response, pruning_enabled667)
        response = self.report.request_json('place=prime&hid=13333&debug=da')
        self.assertFragmentIn(response, pruning_enabled667)

        r = '&rearr-factors=market_hard_prun_non_leaf_category_on_empty_search=1000'
        response = self.report.request_json('place=prime&hid=133&debug=da' + r)
        self.assertFragmentIn(response, pruning_enabled667)
        response = self.report.request_json('place=prime&hid=1333&debug=da' + r)
        self.assertFragmentIn(response, pruning_enabled667)
        response = self.report.request_json('place=prime&hid=13333&debug=da' + r)
        self.assertFragmentIn(response, pruning_enabled667)
        response = self.report.request_json('place=prime&hid=133&debug=da&rgb=blue' + r)
        self.assertFragmentIn(response, pruning_enabled667)
        response = self.report.request_json('place=prime&hid=1333&debug=da&rgb=blue' + r)
        self.assertFragmentIn(response, pruning_enabled667)
        response = self.report.request_json('place=prime&hid=13333&debug=da&rgb=blue' + r)
        self.assertFragmentIn(response, pruning_enabled667)

        # флаг market_hard_prun_departaments_on_empty_search=5000 задает прюнинг только нелистовых категориях первых 2х уровнях вложенности
        pd = '&rearr-factors=market_hard_prun_departaments_on_empty_search=5000'
        response = self.report.request_json('place=prime&hid=133&debug=da' + pd)
        self.assertFragmentIn(response, pruning_enabled667)
        # слишком глубокая категория
        response = self.report.request_json('place=prime&hid=1333&debug=da' + pd)
        self.assertFragmentIn(response, pruning_enabled3334)
        response = self.report.request_json('place=prime&hid=13333&debug=da' + pd)
        self.assertFragmentIn(response, pruning_enabled3334)

        # флаги market_hard_prun_non_leaf_category_on_empty_search и market_hard_prun_departaments_on_empty_search не работают на тексте
        response = self.report.request_json('place=prime&hid=133&text=текст&debug=da' + r)
        self.assertFragmentIn(response, pruning_enabled66667)
        response = self.report.request_json('place=prime&hid=133&text=текст&debug=da' + pd)
        self.assertFragmentIn(response, pruning_enabled66667)

        # флаги market_hard_prun_non_leaf_category_on_empty_search и market_hard_prun_departaments_on_empty_search не работают
        # если есть литералы по магазину, поставщику или экспресс доставке
        response = self.report.request_json('place=prime&hid=133&debug=da&fesh=10' + r)
        self.assertFragmentIn(
            response,
            'Pruning recommend no more then: 5000 selected from: 5000 - subrequest for documents (main) in case separate filters',
        )
        self.assertFragmentIn(
            response, pruning_enabled3334
        )  # здесь действует прюнинг по раздельному запросу за документами
        response = self.report.request_json('place=prime&hid=133&debug=da&supplier-id=513' + r)
        self.assertFragmentIn(response, pruning_default)
        response = self.report.request_json('place=prime&hid=133&debug=da&filter-express-delivery=1' + r)
        self.assertFragmentIn(response, pruning_enabled66667)
        self.assertFragmentIn(response, pruning("*", "tbs200000"))  # при экспресс доставке не делим tbs-value

        # prime-prune-count=75000 меньше market_prime_prun_count_upper_limit=80000
        pruning_upper_limit_50000 = pruning("*", "prune", "pruncount50000")  # 75k * 2 / 3)
        pruning_upper_limit_40000 = pruning("*", "prune", "pruncount40000")  # 60k * 2 / 3)

        # market_prime_prun_count_upper_limit=60000 должен перекрыть prime-prune-count=80000
        response = self.report.request_json(
            'place=prime&hid=13&debug=da&prime-prune-count=80000&rearr-factors=market_prime_prun_count_upper_limit=60000'
        )
        self.assertFragmentIn(response, pruning_upper_limit_40000)

        response = self.report.request_json(
            'place=prime&hid=13&debug=da&prime-prune-count=75000&rearr-factors=market_prime_prun_count_upper_limit=80000'
        )
        self.assertFragmentIn(response, pruning_upper_limit_50000)

        # market_max_prun_count должен перекревать все прюнинги
        response = self.report.request_json('place=prime&hid=13&debug=da&rearr-factors=market_max_prun_count=60000')
        self.assertFragmentIn(response, pruning_upper_limit_40000)

        response = self.report.request_json(
            'place=prime&hid=13&debug=da&prime-prune-count=80000&rearr-factors=market_max_prun_count=60000'
        )
        self.assertFragmentIn(response, pruning_upper_limit_40000)

        response = self.report.request_json(
            'place=prime&hid=91597&debug=1&rearr-factors=market_prime_heavy_categories_prun_count=80000&rearr-factors=market_max_prun_count=60000'
        )
        self.assertFragmentIn(response, pruning_upper_limit_40000)

        response = self.report.request_json(
            'place=prime&hid=13&debug=da&prime-prune-count=60000&rearr-factors=market_max_prun_count=90000'
        )
        self.assertFragmentIn(response, pruning_upper_limit_40000)

    def test_capi_pruning(self):

        pruning_enabled_for_capi_parser = pruning("*", "prune", "pruncount2000")  # 3000 * 2 / 3
        pruning_enabled667 = pruning("*", "prune", "pruncount667")  # 1000 * 2/3

        pruning_enabled_40000 = pruning("*", "prune", "pruncount40000")  # 60k * 2 / 3)

        query = 'place=prime&hid=13&debug=da&prime-prune-count=60000'
        response = self.report.request_json(query)
        self.assertFragmentIn(response, pruning_enabled_40000)

        response = self.report.request_json(query + '&capi-parser=1')
        self.assertFragmentIn(response, pruning_enabled_for_capi_parser)

        response = self.report.request_json(query + '&capi-parser=1&rearr-factors=market_capi_prun_with_filters=0')
        self.assertFragmentIn(response, pruning_enabled_40000)

        # prime-prine-count идет раньше вем capi-parser=1, но дает меньшый прюнинг, прюн по капи-парсеру эффекта не дает
        query = 'place=prime&hid=13&debug=da&prime-prune-count=1000'
        response = self.report.request_json(query)
        self.assertFragmentIn(response, pruning_enabled667)

        response = self.report.request_json(query + '&capi-parser=1')
        self.assertFragmentIn(response, pruning_enabled667)

    def test_bot_pruning(self):
        # прюнинг для роботов
        pruning_bots = pruning("*", "prune", "pruncount2000")  # 3000 * 2 / 3
        pruning_prime = pruning("*", "prune", "pruncount4000")  # 6000 * 2 / 3

        # Should prune bots by default
        for user_agent in BOT_USER_AGENTS:
            response = self.report.request_json("hid=13&place=prime&debug=da", headers={"User-Agent": user_agent})
            self.assertFragmentIn(response, pruning_bots)

        # Should use prime-prune-count if specified
        for user_agent in BOT_USER_AGENTS:
            response = self.report.request_json(
                "hid=13&place=prime&debug=da&prime-prune-count=6000", headers={"User-Agent": user_agent}
            )
            self.assertFragmentIn(response, pruning_prime)

    @classmethod
    def prepare_express_pruning(cls):
        cls.index.navtree += [
            NavCategory(nid=23272130),  # Root express NID
        ]

    def test_pruning_express_nid(self):
        pruning_enabled133334 = pruning("*", "prune", "pruncount133334")  # 200000 * 2 / 3
        pruning_tbs9000000 = pruning("*", "tbs9000000")

        response = self.report.request_json(
            'place=prime&debug=1&nid=23272130&use-nid-for-search=1&filter-express-delivery=1'
            '&rearr-factors=market_express_request_for_intents_use_relaxed_pruning=0'
            ';market_express_request_for_intents_use_relaxed_tbs=0'
        )

        self.assertFragmentNotIn(response, pruning_enabled133334)
        self.assertFragmentNotIn(response, pruning_tbs9000000)

        response = self.report.request_json(
            'place=prime&debug=1&nid=23272130&use-nid-for-search=1&filter-express-delivery=1'
        )

        self.assertFragmentIn(response, pruning_enabled133334)
        self.assertFragmentIn(response, pruning_tbs9000000)

    # MARKETOUT-24622 prime pruning for heavy categories
    @classmethod
    def prepare_prime_pruning_on_heavy_categories(cls):
        cls.index.hypertree += [
            HyperCategory(hid=91011),
            HyperCategory(
                hid=10030,
                children=[
                    HyperCategory(hid=1003092, name="Матрасы"),
                    HyperCategory(hid=91498, name="Чехлы для мобильных телефонов"),
                ],
            ),
            HyperCategory(hid=91259),
            HyperCategory(hid=90710),
            HyperCategory(hid=345, children=[HyperCategory(hid=567)]),
            HyperCategory(hid=91597),
            HyperCategory(hid=90533),
        ]

        cls.index.navtree += [
            NavCategory(
                nid=300,
                children=[
                    NavCategory(nid=301, hid=10030),  # нелистовая категория у которой тяжелый ребенок
                    NavCategory(nid=302, hid=345),
                ],
            ),
            NavCategory(nid=400, children=[NavCategory(nid=402, hid=345)]),
        ]

    def test_prime_pruning_on_heavy_categories(self):

        default_pruning = pruning("*", "prune", "pruncount53334")  # 80'000 * 2 / 3
        non_leaf_pruning = pruning("*", "prune", "pruncount53334")  # 80'000 * 2 / 3
        non_leaf_pruning_web = pruning("*", "prune", "pruncount667")  # 1'000 * 2 / 3
        heavy_pruning = pruning("*", "prune", "pruncount667")  # 1'000 * 2 / 3
        nid_pruning_thematic_landing = pruning("*", "prune", "pruncount667")  # 1'000 * 2 / 3

        response = self.report.request_json('place=prime&hid=345&debug=1')
        self.assertFragmentNotIn(response, default_pruning)

        response = self.report.request_json(
            'place=prime&hid=91597&debug=1&rearr-factors=market_prime_heavy_categories_prun_count=1000'
        )
        self.assertFragmentIn(response, heavy_pruning)
        response = self.report.request_json(
            'place=prime&hid=567&debug=1&rearr-factors=market_prime_heavy_categories_prun_count=1000'
        )
        self.assertFragmentIn(response, default_pruning)

        # в новой реализации можно задавать список тяжелых категорий, при этом тяжелые захардкоженные по дефолту тоже остаются
        response = self.report.request_json(
            'place=prime&hid=567&debug=1&rearr-factors=market_prime_heavy_categories_prun_count=1000;market_prime_heavy_categories=567'
        )
        self.assertFragmentIn(response, heavy_pruning)
        response = self.report.request_json(
            'place=prime&hid=91597&debug=1&rearr-factors=market_prime_heavy_categories_prun_count=1000;market_prime_heavy_categories=567'
        )
        self.assertFragmentIn(response, heavy_pruning)
        response = self.report.request_json(
            'place=prime&hid=567&rgb=blue&debug=1&rearr-factors=market_prime_heavy_categories_prun_count=1000;market_prime_heavy_categories=567'
        )
        self.assertFragmentIn(response, heavy_pruning)
        # также это работает для нелистовых hid и nid содержащих тяжелую категорию
        response = self.report.request_json(
            'place=prime&hid=10030&debug=1&rearr-factors=market_prime_heavy_categories_prun_count=1000'
        )
        self.assertFragmentIn(response, heavy_pruning)

        # на приложении для нелистовых категорий прюнинг 100К
        response = self.report.request_json('place=prime&nid=400&rgb=blue&debug=1&client=IOS')
        self.assertFragmentIn(response, non_leaf_pruning)
        response = self.report.request_json('place=prime&nid=300&rgb=blue&debug=1&tl=1&client=IOS')
        self.assertFragmentIn(response, nid_pruning_thematic_landing)
        response = self.report.request_json(
            'place=prime&nid=400&rgb=blue&debug=1&rearr-factors=market_prime_nid_pruning=0&client=IOS'
        )
        self.assertFragmentIn(response, non_leaf_pruning)

        custom_pruning = pruning("*", "prune", "pruncount80")
        response = self.report.request_json(
            'place=prime&nid=300&rgb=blue&debug=1&rearr-factors=market_prime_nid_pruning=120&client=IOS'
        )
        self.assertFragmentIn(response, custom_pruning)  # 120 * 2 / 3

        # на вебе 1000
        response = self.report.request_json('place=prime&nid=400&debug=1')
        self.assertFragmentIn(response, non_leaf_pruning_web)
        response = self.report.request_json('place=prime&nid=300&rgb=blue&debug=1&tl=1')
        self.assertFragmentIn(response, non_leaf_pruning_web)  # более строгий прюнинг чем у landing thematic
        response = self.report.request_json(
            'place=prime&nid=400&rgb=blue&debug=1&rearr-factors=market_prime_nid_pruning=0'
        )
        self.assertFragmentIn(response, non_leaf_pruning_web)
        response = self.report.request_json(
            'place=prime&nid=300&rgb=blue&debug=1&rearr-factors=market_prime_nid_pruning=120&client=IOS'
        )
        self.assertFragmentIn(response, custom_pruning)  # 120 * 2 / 3

    def test_docs_per_thread(self):
        """На кластерах белого
        при rgb=blue обработка документов распараллеливается по 200 документов на поток
        при запросе в белый обрабатывается по 2000 документов на поток
        """
        response = self.report.request_json(
            'place=prime&text=text&rgb=blue&debug=da' '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, "Specified docsPerThread=200")

        response = self.report.request_json('place=prime&text=text&debug=da' '&rearr-factors=market_metadoc_search=no')
        self.assertFragmentIn(response, "Specified docsPerThread=200")


if __name__ == '__main__':
    main()
