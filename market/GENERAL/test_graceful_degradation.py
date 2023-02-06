#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import base64
import json
from core.matcher import Contains, Round, NotEmpty
from core.types import HyperCategory, Offer, Model, Shop, RtyOffer
from core.testcase import TestCase, main

ERROR_MESSAGE = {
    "error": {"code": "TOO_MANY_REQUESTS", "message": "Graceful degradation: Too many requests, response 429"}
}


def get_expected_smm(value, offer_value=None):
    param_list = ["smm_" + str(value)]
    if offer_value:
        offer_list = ["smm_" + str(offer_value)]
    else:
        offer_list = param_list
    return {
        "debug": {
            "report": {
                "context": {
                    "collections": {
                        "BOOK": {"pron": param_list},
                        "MODEL": {"pron": param_list},
                        "PREVIEW_MODEL": {"pron": param_list},
                        "SHOP": {"pron": offer_list},
                    }
                }
            }
        }
    }


def get_expected_collection_smm(value, collection='*'):
    param_list = ["smm_" + str(value)]
    return {
        "debug": {
            "report": {
                "context": {
                    "collections": {
                        collection: {"pron": param_list},
                    }
                }
            }
        }
    }


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.rty_qpipe = True
        cls.settings.quoter_enabled = True
        cls.settings.memcache_enabled = True
        cls.reqwizard.on_default_request().respond()

        cls.index.shops += [
            Shop(fesh=1, priority_region=213),
        ]

        cls.index.offers += [
            Offer(title='lenovo laptop', hid=21, fesh=1),
            Offer(title='nokia', hid=13, fesh=1),
            Offer(title='iphone', hid=44, fesh=1, feedid=100, offerid=1000, price=300),
            Offer(title='express', hid=45, cpa=Offer.CPA_REAL, is_express=True),
            Offer(title='surface laptop 1', hid=21, hyperid=211, fesh=1),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=21),
            HyperCategory(hid=13),
            HyperCategory(hid=44),
            HyperCategory(hid=45),
        ]

        cls.index.models += [
            Model(title='surface laptop', hid=21, hyperid=211),
        ]

    def _wait_error_429(self, query, headers=None):
        try:
            self.report.request_json(query, headers=headers)
        except RuntimeError as e:
            self.error_log.expect(code=3044)
            self.assertTrue(str(e).startswith('Server error: 429'))
        else:
            raise AssertionError('Error 429 not caught')

    def _add_clients_to_quoter(self, service_name, client_list):
        for client_name in client_list:
            self.quoter.get_client().update_usage_batch(
                service_name=service_name,
                client_name=client_name,
                items=[{'resource': 'cpu', 'current_usage': 0}, {'resource': 'rps', 'current_usage': 0}],
            )
        self.quoter.get_client().sync()

    def test_get_quotas_by_flag(self):
        rules = [{"conditions": ["overuse=1,failed_client,rps"], "actions": ["error=429"]}]

        self._add_clients_to_quoter('test_market', ["failed_client"])

        rearr = "rearr-factors=graceful_degradation_rules={}".format(json.dumps(rules))
        response = self.report.request_json('client=failed_client&place=prime&text=laptop&debug=da&{}'.format(rearr))
        self.assertFragmentNotIn(response, ERROR_MESSAGE)

        rearr = "rearr-factors=graceful_degradation_rules={};use_quoter=1".format(json.dumps(rules))
        query = 'client=failed_client&place=prime&text=laptop&debug=da&{}'.format(rearr)
        self._wait_error_429(query)

    def test_update_usage_by_flag(self):
        def get_rps():
            self.quoter.get_client().sync()
            return (
                self.quoter.get_client()
                .get_quota(service_name='test_market', client_name='succeed_client', resource_name='rps')
                .root['total_usage']
            )

        rules = [{"conditions": ["overuse=1,succeed_client,rps"], "actions": ["error=429"]}]

        self._add_clients_to_quoter('test_market', ['succeed_client'])
        initial_rps = get_rps()

        rearr = "rearr-factors=graceful_degradation_rules={}".format(json.dumps(rules))
        self.report.request_json('client=succeed_client&place=prime&text=laptop&debug=da&{}'.format(rearr))
        current_rps = get_rps()
        self.assertLessEqual(current_rps, initial_rps)

        rearr = "rearr-factors=graceful_degradation_rules={};use_quoter=1".format(json.dumps(rules))
        self.report.request_json('client=succeed_client&place=prime&text=laptop&debug=da&{}'.format(rearr))
        current_rps = get_rps()
        self.assertLess(initial_rps, current_rps)

    def test_quota_overused(self):
        def make_query(client, rules):
            rearr = "rearr-factors=graceful_degradation_rules={};use_quoter=1".format(json.dumps(rules))
            return 'client={}&place=prime&text=laptop&debug=da&{}'.format(client, rearr)

        rps_rules = [
            {"conditions": ["overuse=1,failed_client,rps"], "actions": ["error=429"]},
            {"conditions": ["overuse=1,succeed_client,rps"], "actions": ["error=429"]},
        ]

        cpu_rules = [
            {"conditions": ["overuse=1,failed_client,cpu"], "actions": ["error=429"]},
            {"conditions": ["overuse=1,succeed_client,cpu"], "actions": ["error=429"]},
        ]

        wide_rules = [
            {"conditions": ["overuse=1,failed_client"], "actions": ["error=429"]},
            {"conditions": ["overuse=1,succeed_client"], "actions": ["error=429"]},
        ]

        wildcard_client_rules = [{"conditions": ["overuse=1,*"], "actions": ["error=429"]}]

        self._add_clients_to_quoter('test_market', ["failed_client", "succeed_client"])

        for i, rules in enumerate([rps_rules, cpu_rules, wide_rules, wildcard_client_rules]):
            query = make_query('failed_client', rules)
            self._wait_error_429(query)

            response = self.report.request_json(make_query('succeed_client', rules))
            self.assertFragmentNotIn(response, ERROR_MESSAGE)

    def test_getting_quoter_client_from_resource_meta(self):
        def make_query(rules):
            rearr = "rearr-factors=graceful_degradation_rules={};use_quoter=1".format(json.dumps(rules))
            return 'place=prime&text=laptop&debug=da&{}'.format(rearr)

        rules = [
            {"conditions": ["overuse=1,failed_client,rps"], "actions": ["error=429"]},
        ]

        self._add_clients_to_quoter('test_market', ["failed_client", "succeed_client"])
        headers_with_failed_client = {'resource-meta': '{"client": "failed_client"}'}
        headers_with_succeed_client = {'resource-meta': '{"client": "succeed_client"}'}
        query = make_query(rules)
        self._wait_error_429(query, headers=headers_with_failed_client)

        response = self.report.request_json(make_query(rules), headers=headers_with_succeed_client)
        self.assertFragmentNotIn(response, ERROR_MESSAGE)

    def test_low_priority(self):
        rules = [{"conditions": ["client=failed_client"], "actions": ["low_priority_request=1"]}]

        low_priority_fragment = {"debug": {"report": {"logicTrace": [Contains("Set low priority for this request")]}}}
        docs_per_thread = {"debug": {"report": {"logicTrace": [Contains("Specified docsPerThread=32768")]}}}

        rearr = "rearr-factors=graceful_degradation_rules={}".format(json.dumps(rules))
        response = self.report.request_json('client=failed_client&place=prime&text=laptop&debug=da&{}'.format(rearr))
        self.assertFragmentIn(response, low_priority_fragment)
        self.assertFragmentIn(response, docs_per_thread)

        rearr = "rearr-factors=graceful_degradation_rules={}".format(json.dumps(rules))
        response = self.report.request_json('client=succeed_client&place=prime&text=laptop&debug=da&{}'.format(rearr))
        self.assertFragmentNotIn(response, low_priority_fragment)

        rules_empty = [{"conditions": ["client="], "actions": ["low_priority_request=1"]}]
        rearr = "rearr-factors=graceful_degradation_rules={}".format(json.dumps(rules_empty))
        response = self.report.request_json('place=prime&text=laptop&debug=da&{}'.format(rearr))
        self.assertFragmentIn(response, low_priority_fragment)

        rules_page_id = [
            {"conditions": ["client_page_id=failed_client_page_id"], "actions": ["low_priority_request=1"]},
            {"conditions": ["client_page_id="], "actions": ["low_priority_request=1"]},
            {"conditions": ["client_scenario=bad_client_scenario"], "actions": ["low_priority_request=1"]},
            {"conditions": ["client_scenario="], "actions": ["low_priority_request=1"]},
        ]
        rearr = "rearr-factors=graceful_degradation_rules={}".format(json.dumps(rules_page_id))
        response = self.report.request_json('place=prime&text=laptop&debug=da&{}'.format(rearr))
        self.assertFragmentIn(response, low_priority_fragment)

        response = self.report.request_json(
            'place=prime&text=laptop&debug=da&{}'.format(rearr),
            headers={
                'resource-meta': '%7B%22client%22%3A%22pokupki.touch%22%2C%22pageId%22%3A%22failed_client_page_id%22%2C%22scenario%22%3A%22fetchSkusWithProducts%22%7D'
            },
        )
        self.assertFragmentIn(response, low_priority_fragment)

        response = self.report.request_json(
            'place=prime&text=laptop&debug=da&{}'.format(rearr),
            headers={
                'resource-meta': '%7B%22client%22%3A%22pokupki.touch%22%2C%22pageId%22%3A%22blue-market_product%22%2C%22scenario%22%3A%22bad_client_scenario%22%7D'
            },
        )
        self.assertFragmentIn(response, low_priority_fragment)

        response = self.report.request_json(
            'place=prime&text=laptop&debug=da&{}'.format(rearr),
            headers={'resource-meta': '%7B%22client%22%3A%22pokupki.touch%22%2C%22pageId%22%3A%22client_page_id%22%7D'},
        )
        self.assertFragmentIn(response, low_priority_fragment)

        response = self.report.request_json(
            'place=prime&text=laptop&debug=da&{}'.format(rearr),
            headers={
                'resource-meta': '%7B%22client%22%3A%22pokupki.touch%22%2C%22scenario%22%3A%22fetchSkusWithProducts%22%7D'
            },
        )
        self.assertFragmentIn(response, low_priority_fragment)

        response = self.report.request_json(
            'place=prime&text=laptop&debug=da&{}'.format(rearr),
            headers={
                'resource-meta': '%7B%22client%22%3A%22pokupki.touch%22%2C%22pageId%22%3A%22blue-market_product%22%2C%22scenario%22%3A%22fetchSkusWithProducts%22%7D'
            },
        )
        self.assertFragmentNotIn(response, low_priority_fragment)

        rules_sub_query = [{"conditions": ["sub_query=debug%3Dda"], "actions": ["low_priority_request=1"]}]
        rearr = "rearr-factors=graceful_degradation_rules={}".format(json.dumps(rules_sub_query))
        response = self.report.request_json('place=prime&text=laptop&debug=da&{}'.format(rearr))
        self.assertFragmentIn(response, low_priority_fragment)

        rearr = "rearr-factors=graceful_degradation_rules={}".format(json.dumps(rules_sub_query))
        response = self.report.request_json('place=prime&text=laptop&debug=1&{}'.format(rearr))
        self.assertFragmentNotIn(response, low_priority_fragment)

    def test_smm(self):
        response = self.report.request_json('place=prime&text=laptop&debug=da')
        self.assertFragmentIn(response, get_expected_smm(1))

        rules = [
            {
                "name": "rule1",
                "conditions": ["is_text=1", "level_from=1", "level_to=15"],
                "actions": ["smm=1,0,0.1,0.5,0.0888"],
            },
            {
                "name": "rule2",
                "conditions": ["is_text=0", "level_from=1", "level_to=15"],
                "actions": ["smm=1,0,0.2,0.5,0.0888"],
            },
            {
                "name": "rule3",
                "conditions": ["is_text=0", "hid=13", "level_from=2"],
                "actions": ["smm=1,0,0.3,0.5,0.0888"],
            },
            {
                "name": "rule4",
                "conditions": ["client=someclient", "level_from=1", "level_to=15"],
                "actions": ["smm=1,0,0.3,0.1,0.0888"],
            },
            {
                "name": "rule5",
                "conditions": ["client=someclient", "level_from=1", "level_to=15"],
                "actions": ["smm=1,1,0.7,0.1,0.0888"],
            },
            {
                "name": "rule6",
                "conditions": ["client=base", "base_level_from=1"],
                "actions": ["smm=1,1,0.65,0.1,0.0888"],
            },
            {
                "name": "rule7",
                "conditions": ["is_suspicious=1", "level_from=1", "level_to=15"],
                "actions": ["smm=1,1,0.8,0.1,0.0888"],
            },
            {
                "name": "rule8",
                "conditions": ["is_antirobot_degradation=1", "level_from=1", "level_to=15"],
                "actions": ["smm=1,1,0.8,0.1,0.0888"],
            },
            {
                "name": "rule9",
                "conditions": ["is_text=1", "level_from=3", "level_to=15"],
                "actions": ["smm=1,0,0.4,0.1,0.0888"],
            },
        ]
        rearr = "rearr-factors=graceful_degradation_rules={}".format(json.dumps(rules))
        response = self.report.request_json('place=prime&text=laptop&debug=da&{}'.format(rearr))
        self.assertFragmentIn(response, get_expected_smm(1))

        # text request
        rearr = "rearr-factors=graceful_degradation_force_level=2&rearr-factors=graceful_degradation_rules={}".format(
            json.dumps(rules)
        )
        response = self.report.request_json('place=prime&text=laptop&debug=da&{}'.format(rearr))
        self.assertFragmentIn(response, get_expected_smm(0.8))  # 0.8=1-2*0.1

        # non-text request
        response = self.report.request_json('place=prime&hid=21&how=aprice&debug=da&{}'.format(rearr))
        self.assertFragmentIn(response, get_expected_collection_smm(0.6))  # 0.6=1-2*0.2

        # non-text request, separate rule for category
        response = self.report.request_json('place=prime&hid=13&how=aprice&debug=da&{}'.format(rearr))
        self.assertFragmentIn(response, get_expected_collection_smm(0.5))  # 0.5=max(1-2*0.3,0.5)

        # filter by client=someclient
        response = self.report.request_json('place=prime&text=laptop&debug=da&client=someclient&{}'.format(rearr))
        self.assertFragmentIn(response, get_expected_smm(0.3))  # 0.3=1-(2-1)*0.7

        # filter by client=base, "base_level_from" - must be used
        response = self.report.request_json('place=prime&text=laptop&debug=da&client=base&{}'.format(rearr))
        self.assertFragmentIn(response, get_expected_smm(0.35))  # 0.35=1-(2-1)*0.65

        # filter by is_suspicious
        suspicious_headers = {'X-Antirobot-Suspiciousness-Y': '1.0'}
        response = self.report.request_json(
            'place=prime&text=laptop&debug=da&{}'.format(rearr), headers=suspicious_headers
        )
        self.assertFragmentIn(response, get_expected_smm(0.2))  # 0.2=1-(2-1)*0.8

        # filter by is_antirobot_degradation
        antirobot_degradation_headers = {'X-Yandex-Antirobot-Degradation': '1.0'}
        response = self.report.request_json(
            'place=prime&text=laptop&debug=da&{}'.format(rearr), headers=antirobot_degradation_headers
        )
        self.assertFragmentIn(response, get_expected_smm(0.2))

        # check interation with rearr-factors=smm=1.0, must be minimum value
        # rearr = "rearr-factors=graceful_degradation_force_level=2&rearr-factors=graceful_degradation_rules={}".format(json.dumps(rules_0))
        response = self.report.request_json('place=prime&text=laptop&debug=da&rearr-factors=smm=1.0&{}'.format(rearr))
        self.assertFragmentIn(response, get_expected_smm(0.8))  # 0.8=1-2*0.1
        response = self.report.request_json('place=prime&text=laptop&debug=da&rearr-factors=smm=0.7&{}'.format(rearr))
        self.assertFragmentIn(response, get_expected_smm(0.7))

        # level from 4
        rules_0 = [{"conditions": ["is_text=1", "level_from=4", "level_to=15"], "actions": ["smm=1,0,0.1,0.5,0.0888"]}]
        rearr = "rearr-factors=graceful_degradation_force_level=3&rearr-factors=graceful_degradation_rules={}".format(
            json.dumps(rules_0)
        )
        response = self.report.request_json('place=prime&text=laptop&debug=da&{}'.format(rearr))
        self.assertFragmentIn(response, get_expected_smm(1))

        rearr = "rearr-factors=graceful_degradation_force_level=16&rearr-factors=graceful_degradation_rules={}".format(
            json.dumps(rules_0)
        )
        response = self.report.request_json('place=prime&text=laptop&debug=da&{}'.format(rearr))
        self.assertFragmentIn(response, get_expected_smm(1))

    def test_gracefull_degradation_patch(self):
        rules = [
            {"conditions": ["is_suspicious=1", "level_from=1", "level_to=15"], "actions": ["smm=1,1,0.8,0.1,0.0888"]},
            {
                "conditions": ["is_antirobot_degradation=1", "level_from=1", "level_to=15"],
                "actions": ["smm=1,1,0.8,0.1,0.0888"],
            },
        ]

        rules_patch = [
            {
                "conditions": ["is_antirobot_degradation=1", "level_from=1", "level_to=15"],
                "actions": ["smm=1,1,1,1,1"],
            },
        ]

        rearr = "rearr-factors=graceful_degradation_force_level=2&rearr-factors=graceful_degradation_rules={}".format(
            json.dumps(rules)
        )

        # filter by is_antirobot_degradation
        antirobot_degradation_headers = {'X-Yandex-Antirobot-Degradation': '1.0'}
        response = self.report.request_json(
            'place=prime&text=laptop&debug=da&{}'.format(rearr), headers=antirobot_degradation_headers
        )
        self.assertFragmentIn(response, get_expected_smm(0.2))

        # check we can patch graceful_degradation_rules
        rules_patch_str = base64.b64encode(json.dumps(rules_patch))
        rearr_patched = rearr + "&rearr-factors=graceful_degradation_rules_patch_base64={}".format(rules_patch_str)
        response = self.report.request_json(
            'place=prime&text=laptop&debug=da&{}'.format(rearr_patched), headers=antirobot_degradation_headers
        )
        self.assertFragmentIn(response, get_expected_smm(1))

        # check that is_suspicious still works
        suspicious_headers = {'X-Antirobot-Suspiciousness-Y': '1.0'}
        response = self.report.request_json(
            'place=prime&text=laptop&debug=da&{}'.format(rearr_patched), headers=suspicious_headers
        )
        self.assertFragmentIn(response, get_expected_smm(0.2))  # 0.2=1-(2-1)*0.8

    def test_error(self):
        rules = [
            {"conditions": ["is_text=1", "level_from=5"], "actions": ["error=200"]},
            {"conditions": ["is_text=0", "level_from=5"], "actions": ["error=429"]},
            {"conditions": ["is_text=0", "level_from=5"], "actions": ["error=503"]},
            {"conditions": ["is_text=1", "level_from=10"], "actions": ["error=404"]},
        ]
        rearr = "rearr-factors=graceful_degradation_rules={}".format(json.dumps(rules))
        response = self.report.request_json('place=prime&text=nokia&debug=da&{}'.format(rearr))
        self.assertFragmentIn(response, {"search": {"total": 1}})

        # text request
        rearr = "rearr-factors=graceful_degradation_force_level=7&rearr-factors=graceful_degradation_rules={}".format(
            json.dumps(rules)
        )
        query = 'place=prime&text=laptop&debug=da&{}'.format(rearr)
        response = self.report.request_json(query)
        self.error_log.expect(code=3044).once()
        self.assertFragmentIn(response, ERROR_MESSAGE)

        # non-text request
        try:
            response = self.report.request_json('place=prime&hid=21&debug=da&{}'.format(rearr))
            self.assertTrue(False)
        except RuntimeError as e:
            self.error_log.expect(code=3044)
            self.assertTrue(str(e).startswith('Server error: 429'))

    def test_prun_count(self):
        def get_expected_collection_prun_count(value, collection='*'):
            value = (value + 1) * 2 // 3
            param_list = ["pruncount" + str(value)]
            return {"debug": {"report": {"context": {"collections": {collection: {"pron": param_list}}}}}}

        rules = [
            {
                "conditions": ["is_text=0", "level_from=3", "level_to=10"],
                "actions": ["prun_count=1000,0,100,100,0.0888"],
            },
            {"conditions": ["is_text=0", "level_from=11"], "actions": ["prun_count=100,10,10,1,0.0888"]},
            {
                "conditions": ["numdoc_from=100"],
                "actions": ["prun_count=10000,0,100,100,0.0888"],
            },
            {
                "conditions": ["page_from=2"],
                "actions": ["prun_count=20000,0,100,100,0.0888"],
            },
            {
                "conditions": ["total_numdoc_from=1000"],
                "actions": ["prun_count=5000,0,100,100,0.0888"],
            },
            # Правила с кривыми условиями, которые никогда не должны быть выбраны
            {"priority": 100, "conditions": ["level_from=3", "is_text"], "actions": ["prun_count=1961,10,10,1,0.0888"]},
            {
                "priority": 100,
                "conditions": ["level_from=3", "is_test=0"],
                "actions": ["prun_count=1971,10,10,1,0.0888"],
            },
        ]

        rearr = "rearr-factors=graceful_degradation_rules={}".format(json.dumps(rules))

        response = self.report.request_json(
            'place=prime&hid=21&debug=da&rearr-factors=graceful_degradation_force_level=3&{}'.format(rearr)
        )
        self.assertFragmentIn(response, get_expected_collection_prun_count(700))

        response = self.report.request_json(
            'place=prime&hid=21&debug=da&rearr-factors=graceful_degradation_force_level=10&{}'.format(rearr)
        )
        self.assertFragmentIn(response, get_expected_collection_prun_count(100))

        response = self.report.request_json(
            'place=prime&hid=21&debug=da&rearr-factors=graceful_degradation_force_level=12&{}'.format(rearr)
        )
        self.assertFragmentIn(response, get_expected_collection_prun_count(80))

        response = self.report.request_json('place=prime&hid=21&debug=da&numdoc=200&{}'.format(rearr))
        self.assertFragmentIn(response, get_expected_collection_prun_count(10000))

        response = self.report.request_json('place=prime&hid=21&debug=da&page=3&{}'.format(rearr))
        self.assertFragmentIn(response, get_expected_collection_prun_count(20000))

        response = self.report.request_json('place=prime&hid=21&debug=da&numdoc=200&page=10&{}'.format(rearr))
        self.assertFragmentIn(response, get_expected_collection_prun_count(5000))

    def test_product_classifier(self):
        rules = [
            {
                "conditions": ["level_from=3", "level_to=10", "device=touch_or_tablet"],
                "actions": ["classifier_threshold=0.3,5,-0.1,0.8,0"],
            },
            {
                "conditions": ["level_from=13", "level_to=20", "device=desktop"],
                "actions": ["classifier_threshold=0.3,15,-0.1,0.8,0"],
            },
        ]

        rearr_rules = "rearr-factors=graceful_degradation_rules={}".format(json.dumps(rules))
        request = 'place=parallel&text=iphone&{}&rearr-factors=market_product_request_threshold=0.1'.format(rearr_rules)

        # Проверяем случай, когда значение формулы выше порога.
        # Должен вернуться непустой результат
        response = self.report.request_bs(request + '&reqid=1001')
        self.assertTrue(str(response) != '')
        self.assertFragmentIn(response, {"market_factors": [{"ProductRequestClassifier": Round(0.7)}]})
        self.access_log.expect(reqid=1001, product_request=1)

        # Исчезает
        response = self.report.request_bs(
            request + '&reqid=1011&rearr-factors=graceful_degradation_force_level=10&rearr-factors=device=touch'
        )
        self.assertTrue(str(response) == '')
        self.access_log.expect(reqid=1011, product_request=0)

        # не должен исчезнуть
        response = self.report.request_bs(
            request + '&reqid=1012&rearr-factors=graceful_degradation_force_level=10&rearr-factors=device=desktop'
        )
        self.assertTrue(str(response) != '')
        self.access_log.expect(reqid=1012, product_request=1)

        # не должен исчезнуть
        response = self.report.request_bs(
            request + '&reqid=1021&rearr-factors=graceful_degradation_force_level=20&rearr-factors=device=touch'
        )
        self.assertTrue(str(response) != '')
        self.access_log.expect(reqid=1021, product_request=1)

        # Исчезает
        response = self.report.request_bs(
            request + '&reqid=1022&rearr-factors=graceful_degradation_force_level=20&rearr-factors=device=desktop'
        )
        self.assertTrue(str(response) == '')
        self.access_log.expect(reqid=1022, product_request=0)

    def test_log(self):
        rules = [{"conditions": ["client=failed_client"], "actions": ["log=1"]}]

        rearr = "rearr-factors=graceful_degradation_rules={}".format(json.dumps(rules))
        _ = self.report.request_json('client=succeed_client&place=prime&text=laptop&debug=da&{}'.format(rearr))
        self.error_log.not_expect(code=3044)

        rearr = "rearr-factors=graceful_degradation_rules={}".format(json.dumps(rules))
        _ = self.report.request_json('client=failed_client&place=prime&text=laptop&debug=da&{}'.format(rearr))
        self.error_log.expect(code=3044).once()

    def test_rty_disable(self):
        rules = [{"conditions": ["client=failed_client"], "actions": ["disable_rty=1"]}]
        rearr = "rearr-factors=graceful_degradation_rules={}".format(json.dumps(rules))

        def check_price(response, price):
            self.assertFragmentIn(response, {'prices': {'currency': 'RUR', 'value': str(price)}})

        self.rty.offers += [RtyOffer(feedid=100, offerid=1000, price=200)]

        response = self.report.request_json('client=succeed_client&place=prime&text=iphone&{}'.format(rearr))
        check_price(response, 200)

        response = self.report.request_json('client=failed_client&place=prime&text=iphone&{}'.format(rearr))
        check_price(response, 300)

        headers = {'resource-meta': '%7B%22client%22%3A%22failed_client%22%2C%22pageId%22%3A%22client_page_id%22%7D'}
        response = self.report.request_json('place=prime&text=iphone&{}'.format(rearr), headers=headers)
        check_price(response, 300)

        headers = {'resource-meta': '%7B%22client%22%3A%22succeed_client%22%2C%22pageId%22%3A%22client_page_id%22%7D'}
        response = self.report.request_json('place=prime&text=iphone&{}'.format(rearr), headers=headers)
        check_price(response, 200)

        response = self.report.request_json('place=prime&text=iphone&{}'.format(rearr))
        check_price(response, 200)

    def test_disable_category_ranking(self):
        rules = [{"conditions": ["client=failed_client"], "actions": ["disable_category_ranking=1"]}]
        rearr = "rearr-factors=graceful_degradation_rules={}".format(json.dumps(rules))

        response = self.report.request_json(
            'client=succeed_client&no-search-filters=1&no-intents=1&debug=da&' 'place=prime&hid=13&{}'.format(rearr)
        )
        self.assertFragmentNotIn(response, 'Disable CategoryRanking features logging')

        response = self.report.request_json(
            'client=failed_client&no-search-filters=1&no-intents=1&debug=da&' 'place=prime&hid=13&{}'.format(rearr)
        )
        self.assertFragmentIn(response, 'Disable CategoryRanking features logging')

        self.access_log.expect(disable_category_ranking=1)

    def test_disable_rearranging(self):
        rules = [{"conditions": ["level_from=3"], "actions": ["disable_rearranging=1"]}]
        rearr = "rearr-factors=graceful_degradation_force_level=4;graceful_degradation_rules={}".format(
            json.dumps(rules)
        )
        response = self.report.request_json("place=prime&hid=21&debug=1&{}".format(rearr))
        self.assertFragmentIn(response, {'logicTrace': [Contains('No rearrange on meta because: optimized out')]})
        self.access_log.expect(disable_rearranging=1)

    def test_disable_models_stats(self):
        rules = [{"conditions": ["level_from=3"], "actions": ["disable_models_stats=1"]}]
        rearr = "rearr-factors=optimization_info_in_access_logs=1;graceful_degradation_rules={}".format(
            json.dumps(rules)
        )
        for use_d_offers in [0, 1]:
            _ = self.report.request_json(
                "place=prime&hid=21&use-default-offers={use_d_offers}&{rearr}".format(
                    use_d_offers=use_d_offers, rearr=rearr + ";graceful_degradation_force_level=4"
                ),
                headers={"X-Market-Req-ID": "degr_req"},
            )
            _ = self.report.request_json(
                "place=prime&hid=21&use-default-offers={use_d_offers}&{rearr}".format(
                    use_d_offers=use_d_offers, rearr=rearr
                )
            )

        for sub_place in [
            "NMarketReport::NProductOffers::TProductDefaultOffer",
            "NMarketReport::TModelStatisticsPlace",
        ]:
            # без деградации подзапросы должны быть
            self.subplace_access_log.expect(sub_place=sub_place).once()
            # с деградацией не должно быть подзапросов за ДО или модельной статистикой
            self.subplace_access_log.expect(x_market_req_id=Contains("degr_req"), sub_place=sub_place).never()

    def test_sleep(self):
        time = 100
        rules = [{"conditions": ["client=failed_client"], "actions": ["sleep={}".format(time)]}]

        common_sleep_fragment = {"debug": {"report": {"logicTrace": [Contains("Sleep for")]}}}
        exact_sleep_fragment = {"debug": {"report": {"logicTrace": [Contains("Sleep for {}".format(time))]}}}

        rearr = "rearr-factors=graceful_degradation_rules={}".format(json.dumps(rules))

        response = self.report.request_json('client=failed_client&place=prime&text=laptop&debug=da&{}'.format(rearr))
        self.assertFragmentIn(response, exact_sleep_fragment)

        response = self.report.request_json('client=succeed_client&place=prime&text=laptop&debug=da&{}'.format(rearr))
        self.assertFragmentNotIn(response, common_sleep_fragment)

    def test_background_rule(self):
        rules = [
            {"conditions": ["client=failed_client"], "actions": ["disable_express=1"], "priority": -1},
            {"conditions": ["client=failed_client"], "actions": ["disable_rearranging=1"], "priority": 2},
            {"conditions": ["client=failed_client"], "actions": ["disable_category_ranking=1"], "priority": 1},
        ]
        rearr = "rearr-factors=graceful_degradation_rules={}".format(json.dumps(rules))

        response = self.report.request_json('client=succeed_client&place=prime&hid=45&{}'.format(rearr))
        self.assertFragmentIn(response, {"search": {"totalOffers": 1}})

        response = self.report.request_json("client=failed_client&place=prime&hid=45&debug=da&{}".format(rearr))
        self.assertFragmentIn(response, 'Disable Express Action was set')
        self.assertFragmentNotIn(response, 'Disable CategoryRanking features logging')
        self.assertFragmentIn(response, {"search": {"totalOffers": 0}})
        self.assertFragmentIn(response, {'logicTrace': [Contains('No rearrange on meta because: optimized out')]})
        self.access_log.expect(disable_rearranging=1)

    def test_disabling_express(self):
        rules = [{"conditions": ["client=failed_client"], "actions": ["disable_express=1"]}]
        rearr = "rearr-factors=graceful_degradation_rules={}".format(json.dumps(rules))

        response = self.report.request_json('client=succeed_client&place=prime&hid=45&{}'.format(rearr))
        self.assertFragmentIn(response, {"search": {"totalOffers": 1}})

        response = self.report.request_json('client=failed_client&place=prime&hid=45&debug=da&{}'.format(rearr))
        self.assertFragmentIn(response, 'Disable Express Action was set')
        self.assertFragmentIn(response, {"search": {"totalOffers": 0}})

    def test_disabling_reqwizard(self):
        rules = [{"conditions": ["client=failed_client"], "actions": ["disable_reqwizard=1"]}]
        rearr = "rearr-factors=graceful_degradation_rules={}".format(json.dumps(rules))

        response = self.report.request_json('client=succeed_client&place=prime&text=iphone&debug=da&{}'.format(rearr))
        self.assertFragmentIn(response, {"logicTrace": [Contains('/wizard?')]})

        response = self.report.request_json('client=failed_client&place=prime&text=iphone&debug=da&{}'.format(rearr))
        self.assertFragmentIn(response, 'Disable Reqwizard Action was set')
        self.assertFragmentNotIn(response, {"logicTrace": [Contains('/wizard?')]})

    def __get_cached_items(self):
        memcached_client = self.memcached.get_client()
        return int(memcached_client.get_stats()[0][1]['curr_items'])

    def test_caching(self):
        rules = [{'conditions': ['is_antirobot_degradation=1'], 'actions': ['cache=ugly_parser']}]
        rearr = '&rearr-factors=bot_cache=8;graceful_degradation_rules={}'.format(json.dumps(rules))
        query = 'place=prime&hid=21&debug=da{rearr}'.format(rearr=rearr)

        cached_items_before = self.__get_cached_items()

        # conditions not met, no caching
        response = self.report.request_json(query)
        self.assertFragmentNotIn(
            response, {'debug': {'report': {'logicTrace': [Contains('cache with key "bot-cacher-tag=')]}}}
        )
        self.assertEqual(cached_items_before, self.__get_cached_items())

        # filter by is_antirobot_degradation
        antirobot_degradation_headers = {'X-Yandex-Antirobot-Degradation': '1.0'}

        # check saving to the bot cache
        response = self.report.request_json(query, headers=antirobot_degradation_headers)
        self.assertEqual(cached_items_before + 1, self.__get_cached_items())

        # check getting from the bot cache
        cached_items_before = self.__get_cached_items()
        response = self.report.request_json(query, headers=antirobot_degradation_headers)
        self.assertFragmentIn(
            response,
            {'debug': {'report': {'logicTrace': [Contains('Get response from cache with key "ugly_parser"')]}}},
        )
        self.assertEqual(cached_items_before, self.__get_cached_items())

    def test_disable_cpa_shop_incut(self):
        """Проверяем что не вызываетя подзапрос с cpa_shop_incut"""

        rules = [{'conditions': ['is_antirobot_degradation=1'], 'actions': ['disable_cpa_shop_incut=1']}]
        gd_rearr = '&rearr-factors=graceful_degradation_rules={}'.format(json.dumps(rules))
        incuts = (
            '&supported-incuts={"1"%3A[1%2C4%2C6%2C5%2C12%2C13%2C7%2C16%2C15%2C21]%2C"2"%3A[2%2C3%2C8%2C11%2C18]%2C"101"%3A[8%2C20]}'
            '&blender=1&client=frontend&platform=desktop&pp=7&rearr-factors=market_blender_cpa_shop_incut_enabled=1'
        )

        query = 'place=prime&hid=21&debug=da' + incuts

        antirobot_degradation_headers = {'X-Yandex-Antirobot-Degradation': '1.0'}

        # для обычных пользователей ничего не меняется - идет запрос в cpa_shop_incut
        response = self.report.request_json(query + gd_rearr)
        self.assertFragmentIn(response, {'search': {'results': NotEmpty()}})
        self.assertFragmentIn(response, {'metasearch': {'name': 'cpa_shop_incut'}})
        self.error_log.ignore(message='Failed to get response from vresochnik')

        # для роботов этого подзапрса нет выдача непустая
        response = self.report.request_json(query + gd_rearr, headers=antirobot_degradation_headers)
        self.assertFragmentIn(response, {'search': {'results': NotEmpty()}})
        self.assertFragmentNotIn(response, {'metasearch': {'name': 'cpa_shop_incut'}})

        # заставляем запрос идти через прайм чтобы документы попадали в мимикрию
        rearr_prime = (
            '&rearr-factors='
            "market_report_mimicry_in_serp_pattern=2;"  # спонсорские позиции: [2, 3, 6, 7, 11, 12, ...]
            "market_buybox_auction_search_sponsored_places_web=1;"
            "market_premium_ads_incut_get_docs_through_prime=1;"
            "market_premium_ads_in_search_sponsored_places_web=1;"
            "market_white_search_auction_cpa_fee=1;"
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop=1;"
            "market_premium_ads_in_search_sponsored_places_allow_duplicates=1"
        )
        # для обычных пользователей ничего не меняется - идет запрос в cpa_shop_incut
        response = self.report.request_json(query + gd_rearr + rearr_prime)
        self.assertFragmentIn(response, {'search': {'results': NotEmpty()}})
        self.assertFragmentIn(response, {'metasearch': {'name': 'cpa_shop_incut'}})
        self.assertFragmentIn(response, 'Request Premium Ads incut docs through prime')

        # для роботов этого подзапрса нет выдача непустая
        response = self.report.request_json(query + gd_rearr + rearr_prime, headers=antirobot_degradation_headers)
        self.assertFragmentIn(response, {'search': {'results': NotEmpty()}})
        self.assertFragmentNotIn(response, {'metasearch': {'name': 'cpa_shop_incut'}})
        self.assertFragmentNotIn(response, 'Request Premium Ads incut docs through prime')

    def test_metadoc_pruncount(self):
        """Проверяем что выставляется ограничение на metadoc_total_pruncount и metadoc_effective_pruncount"""

        rules = [
            {
                'conditions': ['is_antirobot_degradation=1', 'is_text=0'],
                'actions': ['metadoc_total_pruncount=10000,0,500,2000,0', 'metadoc_effective_pruncount=1'],
            },
            {
                'conditions': ['is_antirobot_degradation=1', 'is_text=1'],
                'actions': ['metadoc_total_pruncount=5000,0,500,1000,0', 'metadoc_effective_pruncount=1'],
            },
        ]

        gd_rearr = '&rearr-factors=graceful_degradation_rules={}'.format(json.dumps(rules))
        antirobot_degradation_headers = {'X-Yandex-Antirobot-Degradation': '1.0'}

        query = 'place=prime&hid=21&debug=da&cpa=real'
        response = self.report.request_json(query + gd_rearr)
        self.assertFragmentIn(
            response,
            {
                'how': [
                    {
                        'args': Contains(
                            'metadoc_search: true', 'metadoc_total_pruncount: 25000', 'metadoc_effective_pruncount: 3'
                        )
                    }
                ]
            },
        )

        response = self.report.request_json(query + gd_rearr, headers=antirobot_degradation_headers)
        self.assertFragmentIn(
            response,
            {
                'how': [
                    {
                        'args': Contains(
                            'metadoc_search: true', 'metadoc_total_pruncount: 10000', 'metadoc_effective_pruncount: 1'
                        )
                    }
                ]
            },
        )

        query = 'place=prime&text=iphone&debug=da&cpa=real'
        response = self.report.request_json(query + gd_rearr)
        self.assertFragmentIn(
            response,
            {
                'how': [
                    {
                        'args': Contains(
                            'metadoc_search: true', 'metadoc_total_pruncount: 10000', 'metadoc_effective_pruncount: 3'
                        )
                    }
                ]
            },
        )

        response = self.report.request_json(query + gd_rearr, headers=antirobot_degradation_headers)
        self.assertFragmentIn(
            response,
            {
                'how': [
                    {
                        'args': Contains(
                            'metadoc_search: true', 'metadoc_total_pruncount: 5000', 'metadoc_effective_pruncount: 1'
                        )
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
