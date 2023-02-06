#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import HyperCategory, Model, Offer, VCluster
from core.testcase import TestCase, main
from core.matcher import NoKey


def expected_results(dlmtrs, is_price=True):
    def value(min, max):
        _ = "%sv%s" % (min or "", max or "")

        kmin = "priceMin" if is_price else "min"
        kmax = "priceMax" if is_price else "max"

        if is_price:
            return {
                kmin: NoKey(kmin) if min is None else {"value": str(min)},
                kmax: NoKey(kmax) if max is None else {"value": str(max)},
                "checked": NoKey("checked"),
            }
        else:
            return {
                kmin: NoKey(kmin) if min is None else str(min),
                kmax: NoKey(kmax) if max is None else str(max),
                "checked": NoKey("checked"),
            }

    fname = "glprice_discrete" if is_price else "glprice"
    if not dlmtrs:
        return {"filters": [{"id": fname}]}

    dlm = [None] + dlmtrs + [None]
    values = [value(dlm[i], dlm[i + 1]) for i in range(len(dlm) - 1)]
    kvalues = "values" if is_price else "presetValues"
    return {"filters": [{"id": fname, kvalues: values}]}


def result57(is_price=True):
    # prices == range(57, 1057, 10)
    return expected_results([200, 500], is_price)


class T(TestCase):
    @classmethod
    def prepare_simple(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.hypertree += [
            HyperCategory(hid=1, visual=True),
            HyperCategory(hid=2),
            HyperCategory(hid=3),
        ]

        for i in range(100):
            vcluster_id = 1000000100 + i
            cls.index.vclusters += [
                VCluster(vclusterid=vcluster_id, hid=1),
            ]

            model_id = 1000 + i
            cls.index.models += [
                Model(hyperid=model_id, hid=2),
            ]

            price = 57 + 10 * i
            cls.index.offers += [
                Offer(vclusterid=vcluster_id, fesh=201, price=price, hid=1),
                Offer(hyperid=model_id, fesh=202, price=price, hid=2),
                Offer(fesh=203, price=price, hid=3),
            ]

    def test_price_simple(self):
        req = 'place=prime&rearr-factors=market_discrete_price_filter=1'

        # кластеры
        response = self.report.request_json(req + '&hid=1')
        self.assertFragmentIn(response, result57(True))
        self.assertFragmentIn(response, result57(False))

        # модели
        response = self.report.request_json(req + '&hid=2')
        self.assertFragmentIn(response, result57(True))
        self.assertFragmentIn(response, result57(False))

        # офферы
        response = self.report.request_json(req + '&hid=3')
        self.assertFragmentIn(response, result57(True))
        self.assertFragmentIn(response, result57(False))

    def test_price_simple_checked(self):
        # границы не совпадают с имеющимися
        # нет выбранного значения
        req = 'place=prime&rearr-factors=market_discrete_price_filter=1&mcpricefrom=100&mcpriceto=500'
        expected = result57(True)
        expected2 = result57(False)

        # кластеры
        response = self.report.request_json(req + '&hid=1')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # модели
        response = self.report.request_json(req + '&hid=2')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # офферы
        response = self.report.request_json(req + '&hid=3')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # границы совпадают с имеющимися
        req = 'place=prime&rearr-factors=market_discrete_price_filter=1&mcpricefrom=200&mcpriceto=500'
        expected = result57(True)
        expected2 = result57(False)
        expected["filters"][0]["values"][1]["checked"] = True
        expected2["filters"][0]["presetValues"][1]["checked"] = True

        # кластеры
        response = self.report.request_json(req + '&hid=1')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # модели
        response = self.report.request_json(req + '&hid=2')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # офферы
        response = self.report.request_json(req + '&hid=3')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # нижней границы нет, верхняя - первая
        req = 'place=prime&rearr-factors=market_discrete_price_filter=1&mcpriceto=200'
        expected = result57(True)
        expected2 = result57(False)
        expected["filters"][0]["values"][0]["checked"] = True
        expected2["filters"][0]["presetValues"][0]["checked"] = True

        # кластеры
        response = self.report.request_json(req + '&hid=1')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # модели
        response = self.report.request_json(req + '&hid=2')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # офферы
        response = self.report.request_json(req + '&hid=3')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # нижняя граница == 0, верхняя - первая
        req = 'place=prime&rearr-factors=market_discrete_price_filter=1&mcpricefrom=0&mcpriceto=200'
        expected = result57(True)
        expected2 = result57(False)
        expected["filters"][0]["values"][0]["checked"] = True
        expected2["filters"][0]["presetValues"][0]["checked"] = True

        # кластеры
        response = self.report.request_json(req + '&hid=1')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # модели
        response = self.report.request_json(req + '&hid=2')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # офферы
        response = self.report.request_json(req + '&hid=3')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # нижней границы нет, верхняя - не первая
        # нет выбранного значения
        req = 'place=prime&rearr-factors=market_discrete_price_filter=1&mcpriceto=500'
        expected = result57(True)
        expected2 = result57(False)

        # кластеры
        response = self.report.request_json(req + '&hid=1')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # модели
        response = self.report.request_json(req + '&hid=2')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # офферы
        response = self.report.request_json(req + '&hid=3')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # нижняя граница == 0, верхняя - не первая
        # нет выбранного значения
        req = 'place=prime&rearr-factors=market_discrete_price_filter=1&mcpricefrom=0&mcpriceto=500'
        expected = result57(True)
        expected2 = result57(False)

        # кластеры
        response = self.report.request_json(req + '&hid=1')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # модели
        response = self.report.request_json(req + '&hid=2')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # офферы
        response = self.report.request_json(req + '&hid=3')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # верхней границы нет, нижняя - последняя
        req = 'place=prime&rearr-factors=market_discrete_price_filter=1&mcpricefrom=500'
        expected = result57(True)
        expected2 = result57(False)
        expected["filters"][0]["values"][2]["checked"] = True
        expected2["filters"][0]["presetValues"][2]["checked"] = True

        # кластеры
        response = self.report.request_json(req + '&hid=1')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # модели
        response = self.report.request_json(req + '&hid=2')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # офферы
        response = self.report.request_json(req + '&hid=3')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # верхней границы нет, нижняя - не последняя
        # нет выбранного значения
        req = 'place=prime&rearr-factors=market_discrete_price_filter=1&mcpricefrom=200'
        expected = result57(True)
        expected2 = result57(False)

        # кластеры
        response = self.report.request_json(req + '&hid=1')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # модели
        response = self.report.request_json(req + '&hid=2')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # офферы
        response = self.report.request_json(req + '&hid=3')
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

    def test_price_simple_checked_noexp(self):
        """
        без эксперимента нет glprice_discrete, но есть presetValues у glprice

        границы совпадают с имеющимися
        """
        req = 'place=prime&mcpricefrom=200&mcpriceto=500'
        expected = result57(False)
        expected["filters"][0]["presetValues"][1]["checked"] = True

        # кластеры
        response = self.report.request_json(req + '&hid=1')
        self.assertFragmentNotIn(response, {"id": "glprice_discrete"})
        self.assertFragmentIn(response, expected)

        # модели
        response = self.report.request_json(req + '&hid=2')
        self.assertFragmentNotIn(response, {"id": "glprice_discrete"})
        self.assertFragmentIn(response, expected)

        # офферы
        response = self.report.request_json(req + '&hid=3')
        self.assertFragmentNotIn(response, {"id": "glprice_discrete"})
        self.assertFragmentIn(response, expected)

    @classmethod
    def prepare_big_price(cls):
        cls.index.hypertree += [
            HyperCategory(hid=5),
        ]

        for i in range(25):
            model_id = 100 + i
            cls.index.models += [
                Model(hyperid=model_id, hid=5),
            ]

            price = 50 + 2**i
            cls.index.offers += [
                Offer(hyperid=model_id, fesh=202, price=price, hid=5),
            ]

    def test_price_simple_checked_big_price(self):
        """
        проверяем, что фильтры выбираются и на больших значениях цен
        """
        req = 'place=prime&rearr-factors=market_discrete_price_filter=1&hid=5&mcpricefrom=50000&mcpriceto=500000'
        expected = expected_results([1000, 5000, 50000, 500000], True)
        expected2 = expected_results([1000, 5000, 50000, 500000], False)
        expected["filters"][0]["values"][3]["checked"] = True
        expected2["filters"][0]["presetValues"][3]["checked"] = True

        response = self.report.request_json(req)
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # 500000.01 - мимо
        req = 'place=prime&rearr-factors=market_discrete_price_filter=1&hid=5&mcpricefrom=50000&mcpriceto=500000.01'
        expected = expected_results([1000, 5000, 50000, 500000], True)
        expected2 = expected_results([1000, 5000, 50000, 500000], False)

        response = self.report.request_json(req)
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

        # 50000.01 - мимо
        req = 'place=prime&rearr-factors=market_discrete_price_filter=1&hid=5&mcpricefrom=50000.01&mcpriceto=500000'
        expected = expected_results([1000, 5000, 50000, 500000], True)
        expected2 = expected_results([1000, 5000, 50000, 500000], False)

        response = self.report.request_json(req)
        self.assertFragmentIn(response, expected)
        self.assertFragmentIn(response, expected2)

    @classmethod
    def prepare_one_model(cls):
        cls.index.hypertree += [
            HyperCategory(hid=10),
        ]

        model_id = 2000
        cls.index.models += [
            Model(hyperid=model_id, hid=10),
        ]

        for i in range(100):
            cls.index.offers += [
                Offer(hyperid=model_id, fesh=210, price=57 + 10 * i, hid=10),
            ]

    def test_price_one_model(self):
        req = 'place=prime&rearr-factors=market_discrete_price_filter=1'

        response = self.report.request_json(req + '&hid=10')
        self.assertFragmentIn(response, result57(True))
        self.assertFragmentIn(response, result57(False))

    @classmethod
    def prepare_some_models_many_offers(cls):
        cls.index.hypertree += [
            HyperCategory(hid=20),
            HyperCategory(hid=21),
            HyperCategory(hid=22),
        ]

        for i in range(20):
            model_id = 2020 + i
            cls.index.models += [
                Model(hyperid=model_id, hid=20),
            ]
            for j in range(5):
                cls.index.offers += [
                    Offer(hyperid=model_id, fesh=220, price=57 + i * 50 + j, hid=20),
                ]

        for i in range(5):
            model_id = 2050 + i
            cls.index.models += [
                Model(hyperid=model_id, hid=21),
            ]
            for j in range(20):
                cls.index.offers += [
                    Offer(hyperid=model_id, fesh=221, price=57 + i * 200 + j, hid=21),
                ]

        cnt = 0
        for i in range(7):
            model_id = 2100 + i
            cls.index.models += [
                Model(hyperid=model_id, hid=22),
            ]
            maxj = 1 << i
            if i == 0:
                maxj = 6
            elif i == 6:
                maxj = 32
            for j in range(maxj):
                cls.index.offers += [
                    Offer(hyperid=model_id, fesh=222, price=57 + cnt * 10, hid=22),
                ]
                cnt += 1
        assert cnt == 100

    def test_price_some_models_many_offers(self):
        req = 'place=prime&rearr-factors=market_discrete_price_filter=1'

        response = self.report.request_json(req + '&hid=20')
        self.assertFragmentIn(response, result57(True))
        self.assertFragmentIn(response, result57(False))

        response = self.report.request_json(req + '&hid=21')
        self.assertFragmentIn(response, result57(True))
        self.assertFragmentIn(response, result57(False))

        response = self.report.request_json(req + '&hid=22')
        self.assertFragmentIn(response, result57(True))
        self.assertFragmentIn(response, result57(False))

    @classmethod
    def prepare_no_filter_data(cls):
        cls.index.hypertree += [
            HyperCategory(hid=30),
        ]

        for i in range(100):
            model_id = 2200 + i
            cls.index.models += [
                Model(hyperid=model_id, hid=30),
            ]
            cls.index.offers += [
                Offer(hyperid=model_id, fesh=220, price=107 + i, hid=30),
            ]

    def test_price_no_filter_data(self):
        req = 'place=prime&rearr-factors=market_discrete_price_filter=1'

        response = self.report.request_json(req + '&hid=30')
        self.assertFragmentNotIn(response, {"id": "glprice_discrete"})
        self.assertFragmentIn(response, {"id": "glprice", "presetValues": NoKey("presetValues")})

    @classmethod
    def prepare_one_delimiter_data(cls):
        cls.index.hypertree += [
            HyperCategory(hid=40),
        ]

        for i in range(80):
            model_id = 3000 + i
            cls.index.models += [
                Model(hyperid=model_id, hid=40),
            ]
            cls.index.offers += [
                Offer(hyperid=model_id, fesh=300, price=80 + i, hid=40),
            ]

    def test_price_one_delimiter_data(self):
        req = 'place=prime&rearr-factors=market_discrete_price_filter=1'

        response = self.report.request_json(req + '&hid=40')
        self.assertFragmentIn(response, expected_results([100], True))
        self.assertFragmentIn(response, expected_results([100], False))


if __name__ == '__main__':
    main()
