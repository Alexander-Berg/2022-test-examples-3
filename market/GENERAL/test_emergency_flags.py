#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

import datetime
import os
import time

from core.emergency_flags import Expression
from core.matcher import Not, Equal, NotEmpty
from core.paths import SRCROOT
from core.testcase import TestCase, main
from core.types import Offer, RtyOffer, Shop


def strftime(timestamp):
    return datetime.datetime.utcfromtimestamp(timestamp).strftime("%Y-%m-%dT%H:%M:%SZ")


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.rty_qpipe = True
        cls.settings.wait_rty_started = False

        cls.index.creation_time = int(time.time()) // 60 * 60
        cls.index.shops += [Shop(fesh=1, priority_region=213, name='test_shop')]

    def update_flags(self, wait_rty_started=True, **kwargs):
        self.stop_report()
        self.emergency_flags.reset()
        self.emergency_flags.add_flags(**kwargs)
        self.emergency_flags.save()
        self.restart_report()
        if wait_rty_started:
            while True:
                if self.rty_controller.status() == 'Active':
                    break
                time.sleep(0.1)

    @classmethod
    def prepare_test_disable_rty_server(cls):
        cls.index.offers += [Offer(fesh=1, title='iphone', feedid=1, offerid='disable_rty_server_offer', price=100)]

    def test_disable_rty_server(self):
        """Check that rty server not started"""

        expected_qdata = {
            "qdata": {
                "prices": {
                    "value": 100,
                    "modification time": self.index.creation_time,
                    "modification date": strftime(self.index.creation_time),
                    "source": "generation",
                }
            }
        }

        self.update_flags(wait_rty_started=False, disable_rty_server=1)

        offer_id = 'disable_rty_server_offer'
        request_query = 'place=print_doc&feed_shoffer_id=1-{}&req_attrs=qdata'.format(offer_id)

        # data from generation
        response = self.report.request_json(request_query)
        self.assertFragmentIn(response, expected_qdata)

        # refresh data though try
        self.rty.offers += [
            RtyOffer(
                feedid=1, offerid=offer_id, price=200, modification_time=int(time.time()), changed_states=['feed_price']
            )
        ]
        self.rty.flush()

        # check that rty data not accepted
        response = self.report.request_json(request_query)
        self.assertFragmentIn(response, expected_qdata)
        self.assertFragmentNotIn(response, {"qdata": {"prices": {"value": 200, "source": "rty"}}})

        # check that rty server not started
        assert self.rty_controller.status() != 'Active'
        self.base_logs_storage.common_log.expect('Rty server is emergency disabled').once()

    @classmethod
    def prepare_test_disable_rty_indexing(cls):
        cls.index.offers += [Offer(fesh=1, title='iphone', feedid=1, offerid='disable_rty_indexing_offer', price=100)]

    def test_disable_rty_indexing(self):
        """Check that rty not read any documents"""

        expected_qdata = {
            "qdata": {
                "prices": {
                    "value": 100,
                    "modification time": self.index.creation_time,
                    "modification date": strftime(self.index.creation_time),
                    "source": "generation",
                }
            }
        }

        self.update_flags(disable_rty_server=0, disable_rty_indexing=1)

        # check that rty already started
        assert self.rty_controller.status() == 'Active'

        offer_id = 'disable_rty_indexing_offer'
        request_query = 'place=print_doc&feed_shoffer_id=1-{}&req_attrs=qdata'.format(offer_id)

        # data from generation
        response = self.report.request_json(request_query)
        self.assertFragmentIn(response, expected_qdata)

        # refresh data though try
        self.rty.offers += [
            RtyOffer(
                feedid=1, offerid=offer_id, price=200, modification_time=int(time.time()), changed_states=['feed_price']
            )
        ]
        self.rty.flush()

        # check that rty data not accepted
        response = self.report.request_json(request_query)
        self.assertFragmentIn(response, expected_qdata)
        self.assertFragmentNotIn(response, {"qdata": {"prices": {"value": 200, "source": "rty"}}})

    @classmethod
    def prepare_test_ignore_request_rearr_flags(cls):
        cls.index.offers += [
            Offer(fesh=1, title='apple', feedid=1, offerid='test_ignore_request_rearr_flags_offer', price=100)
        ]

    def test_ignore_request_rearr_flags(self):
        """Check ignore rearr flags"""

        # just reset flags
        self.update_flags()

        request_query = 'place=prime&text=apple&debug=da&rids=213&rearr-factors=market_search_mn_algo=TESTALGO_combined'
        response = self.report.request_json(request_query)
        self.assertFragmentIn(
            response, {"entity": "offer", "titles": {"raw": "apple"}, "debug": {"rankedWith": "TESTALGO_combined"}}
        )

        self.update_flags(ignore_request_rearr_flags=1)

        response = self.report.request_json(request_query)
        self.assertFragmentIn(
            response,
            {"entity": "offer", "titles": {"raw": "apple"}, "debug": {"rankedWith": Not(Equal("TESTALGO_combined"))}},
        )

    def test_enable_report_safe_mode(self):
        """Check rewriting flag if report safe mode enabled"""

        response = self.report.request_plain('admin_action=versions')
        self.assertIn('<report-safe-mode>0</report-safe-mode>', str(response))

        self.update_flags(
            wait_rty_started=False, disable_rty_server=0, ignore_request_rearr_flags=0, enable_report_safe_mode=1
        )

        # check status
        response = self.report.request_plain('admin_action=versions')
        self.assertIn('<report-safe-mode>1</report-safe-mode>', str(response))

        # check that flags were rewritten
        self.common_log.expect('Emergency flag disable_rty_server was set, value: 1').once()
        self.common_log.expect('Emergency flag ignore_request_rearr_flags was set, value: 1').once()
        # nothing extra in logs
        self.common_log.expect('was changed, value: 1').never()

        # check ignoring of rearr flag
        response = self.report.request_json(
            'place=prime&text=apple&debug=da&rids=213&rearr-factors=market_search_mn_algo=TESTALGO_combined'
        )
        self.assertFragmentIn(
            response,
            {"entity": "offer", "titles": {"raw": "apple"}, "debug": {"rankedWith": Not(Equal("TESTALGO_combined"))}},
        )

        # check that rty server not started
        assert self.rty_controller.status() != 'Active'
        self.base_logs_storage.common_log.expect('Rty server is emergency disabled').once()

    def test_restart_report(self):
        """Check shutdown report"""

        # check that report still working if ts is less than report start time (timestmap = 0 by default)
        expression = Expression(default_value=1)
        self.emergency_flags.add_flags(restart_report=expression)
        self.emergency_flags.save()
        self.common_log.wait_line('Emergency flag restart_report was set')
        assert self.server.alive()

        # check that report still working if ts is set to future
        expression = Expression(timestamp=int(time.time()) + 100, default_value=1)
        self.emergency_flags.add_flags(restart_report=expression)
        self.emergency_flags.save()
        self.common_log.wait_line('Emergency flag restart_report was changed')
        assert self.server.alive()

        # check report shutdown
        delay_time = 5
        set_flag_timestamp = int(time.time())
        expression = Expression(timestamp=set_flag_timestamp + delay_time, default_value=1)
        self.emergency_flags.add_flags(restart_report=expression)
        self.emergency_flags.save()
        self.common_log.wait_line('Restart report (SIGTERM) by emergency flag')
        assert int(time.time()) >= set_flag_timestamp + delay_time
        while self.server.alive():
            time.sleep(0.1)

        # for suppressing check of report alive on tearDown
        self.emergency_flags.reset()
        self.emergency_flags.save()
        self.restart_report()

    def test_auto_mem_lock_quota(self):
        """Проверяем флаги для управления квотой на залоченую под индекс память"""

        self.update_flags(rty_index_memory_lock_quota_gb=10, index_memory_lock_quota_gb=20)

        self.common_log.expect('Setting memory lock quota for id 0 to 20GiB').once()
        self.common_log.expect('Setting memory lock quota for id 1 to 10GiB').once()

    def test_get_available_flags(self):
        """Check flag and lua info from admin_action=remote_flags_info handle"""

        def read_flag_list(flag_file_path):
            flags = []
            flags_path = os.path.join(SRCROOT, flag_file_path)
            with open(flags_path) as flags_file:
                for flag in flags_file:
                    if flag.strip():
                        flags.append({'name': flag.strip()})
            return flags

        emergency_flags = read_flag_list('market/report/library/emergency_flags/emergency_flags')
        experiment_flags = read_flag_list('market/report/library/experiment_flags/experiment_flags')
        expected_data = {
            'version': NotEmpty(),
            'experiment_flags': experiment_flags,
            'emergency_flags': emergency_flags,
            'predicates': [
                {
                    'name': 'IS_API',
                    'descr': "Является 'api'.",
                    'type': 'var',
                    'value_type': 'bool',
                    'args': '',
                },
                {
                    'name': 'is_in',
                    'descr': NotEmpty(),
                    'type': 'function',
                    'value_type': 'bool',
                    'args': 'element, list',
                },
            ],
        }
        response = self.report.request_json('admin_action=remote_flags_info')
        self.assertFragmentIn(response, expected_data)


if __name__ == '__main__':
    main()
