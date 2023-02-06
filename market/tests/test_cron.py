#!/usr/bin/python
# -*- coding: utf-8 -*-

import unittest

from market.pylibrary.yatestwrap.yatestwrap import source_path

from getter import core


class Test(unittest.TestCase):
    def test(self):
        def get_service_name(line):
            return [field for field in line.split()[7:] if not field.startswith('-')][0]

        # MARKETINDEXER-38348: do not run mbo_stuff tasks from 21:00 to 5:00 to preserve server stability and respect SLA
        allowed_hours_for_service = {'mbo_stuff': '5-20'}

        def cron_ranges_to_hours(cron_ranges):
            def check_valid_hour(hour):
                self.assertTrue(0 <= hour <= 23, 'Invalid hour format')
            def check_valid_span(span):
                self.assertTrue(len(span) == 2 and span[0] < span[1], 'Invalid hour span')
                check_valid_hour(span[0])
                check_valid_hour(span[1])
            hours = [False for i in range(0, 24)]
            for hours_range in cron_ranges.split(','):
                span = [0, 23] if hours_range == '*' else [int(x) for x in hours_range.split('-')]
                if len(span) == 1:
                    hour = span[0]
                    check_valid_hour(hour)
                    hours[hour] = True
                elif len(span) == 2:
                    check_valid_span(span)
                    for i in range(span[0], span[1] + 1):
                        hours[i] = True
            return hours

        def get_hours(line):
            return cron_ranges_to_hours(line.split()[1])

        ignore_cron_names = ['forecaster', 'banned_top_irrel', 'web_features_prepare', 'web_features_download']

        cron_names = []
        cron_hours = []
        with open(source_path('market/getter/debian/cron.d')) as fobj:
            for s in fobj:
                s = s.rstrip()
                if s and not s.startswith('#') and not s.startswith('CRON'):
                    cron_names.append(get_service_name(s))
                    cron_hours.append(get_hours(s))

        root = core.create_root('tmp')
        mbo_names = ['mbo_stuff', 'mbo_clusters', 'mbo_fast', 'mbo_cms']
        root_names = root.keys() + mbo_names

        no_cron = set(root_names) - set(cron_names) - set(ignore_cron_names)
        no_root = set(cron_names) - set(root_names) - set(['all'])

        self.assertFalse(no_cron, 'Add this services to cron.d: %s' % ','.join(no_cron))
        self.assertFalse(no_root, 'No such services: %s' % ','.join(no_root))

        for i in range(len(cron_hours)):
            service_name = cron_names[i]
            if service_name in allowed_hours_for_service.keys():
                allowed_hours = cron_ranges_to_hours(allowed_hours_for_service[service_name])
                for hour in range(0, 24):
                    self.assertFalse(allowed_hours[hour] is False and cron_hours[i][hour] is True,
                                     'Task {} is planned outside of allowed hours'.format(service_name))


if __name__ == '__main__':
    unittest.main()
