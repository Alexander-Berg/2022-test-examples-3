#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import os
from core.types import Offer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.enable_exec_stats_log = True

    def test_with_log(self):
        # Для проверки что что-то реально пишется (или нет) в лог.
        def get_log_sizes():
            log_path = os.path.join(self.meta_paths.logs, 'exec-stats.log')
            self.assertFragmentIn(self.report.request_xml('admin_action=flushlogs'), '<status>Logs flushed ok</status>')
            size_before = os.path.getsize(log_path)
            self.report.request_json('place=prime&text=iphone')
            self.assertFragmentIn(self.report.request_xml('admin_action=flushlogs'), '<status>Logs flushed ok</status>')
            size_after = os.path.getsize(log_path)
            return size_before, size_after

        response = self.report.request_xml('admin_action=execstats')
        self.assertFragmentIn(
            response,
            '''
        <admin-action>
            Execution stats log is enabled. Export to Golovan is disabled.
        </admin-action>
        ''',
        )

        response = self.report.request_xml('admin_action=execstats&enable=0')
        self.assertFragmentIn(
            response,
            '''
        <admin-action>
            Execution stats log is disabled. Export to Golovan is disabled.
        </admin-action>
        ''',
        )

        size_before, size_after = get_log_sizes()
        self.assertEqual(size_before, size_after)

        response = self.report.request_xml('admin_action=execstats&enable=1')
        self.assertFragmentIn(
            response,
            '''
        <admin-action>
            Execution stats log is enabled. Export to Golovan is disabled.
        </admin-action>
        ''',
        )

        size_before, size_after = get_log_sizes()
        self.assertNotEqual(size_before, size_after)

    @classmethod
    def prepare_signals(cls):
        cls.index.offers += [Offer(title='someoffer')]

    def test_signals(self):
        def extract_golovan_metric_value(json_root, metric_name):
            for record in json_root:
                if record[0] == metric_name:
                    return record[1]
            return None

        def get_glovan_metrics():
            self.report.request_json('place=print_doc&text=someoffer')
            self.assertFragmentIn(self.report.request_xml('admin_action=flushlogs'), '<status>Logs flushed ok</status>')
            tass_data = self.report.request_tass()
            metric_count = tass_data.get('es_TMarketRelevance_count_dmmm')
            metric_time = tass_data.get('es_TMarketRelevance_time_dmmm')
            self.assertIsNotNone(metric_count)
            self.assertIsNotNone(metric_time)
            return metric_count, metric_time

        response = self.report.request_xml('admin_action=execstats&enable=1&golovan=1')
        self.assertFragmentIn(
            response,
            '''
        <admin-action>
            Execution stats log is enabled. Export to Golovan is enabled.
        </admin-action>
        ''',
        )
        metric_count1, _ = get_glovan_metrics()

        response = self.report.request_xml('admin_action=execstats&enable=0&golovan=1')
        self.assertFragmentIn(
            response,
            '''
        <admin-action>
            Execution stats log is disabled. Export to Golovan is enabled.
        </admin-action>
        ''',
        )
        metric_count2, _ = get_glovan_metrics()
        self.assertGreater(metric_count2, metric_count1)

        # Возвращаем значение обратно, чтобы не влиять на другие тесты.
        response = self.report.request_xml('admin_action=execstats&enable=1&golovan=0')


if __name__ == '__main__':
    main()
