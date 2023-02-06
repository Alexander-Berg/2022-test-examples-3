#!/usr/bin/python
# -*- coding: utf-8 -*-

import context
import os
import logging
import unittest

from reductor import reductor
reductor.IS_DRY_RUN = True

logging.basicConfig(filename='/dev/null')


def create_good_reductor(config_path):
    class GoodBackend(reductor.Backend):
        def __init__(self, hostname, datacenter=None, port=None, timeout=None):
            reductor.Backend.__init__(self, hostname, datacenter, port)

        def do(self, command):
            return 'ok'

    return reductor.Reductor(config_path, GoodBackend)


def create_bad_reductor(config_path):
    class BadBackend(reductor.Backend):
        def __init__(self, hostname, datacenter=None, port=None, timeout=None):
            reductor.Backend.__init__(self, hostname, datacenter, port)

        def do(self, command):
            return '! ok'

    return reductor.Reductor(config_path, BadBackend)


class Test(unittest.TestCase):
    def test(self):
        reductor = create_good_reductor(
            os.path.join(context.DATA_DIR, 'marketsearch.json'))
        group = 'market_search@myt'
        generation = '20150414_1122'
        group_and_generation = ' '.join([group, generation])
        method_arg_list = [
            #  group
            ('do_upload_group', group_and_generation),
            ('do_switch_group', group),
            ('do_reload_group', group),
            #  main
            ('do_upload', generation),
            ('do_switch', ''),
            ('do_reload', ''),
            ('do_simple_restart', ''),
            #  marketsearch
            ('do_reload_marketsearch', ''),
            #  marketsearchdiff
            ('do_revert_to_base', '')
        ]
        for method, arg in method_arg_list:
            result = getattr(reductor, method)(arg)
            self.assertTrue(result is None, method)

    def test_bad(self):
        reductor = create_bad_reductor(
            os.path.join(context.DATA_DIR, 'marketsearch.json'))
        self.assertEqual(reductor.do_simple_restart(''), 33)


if __name__ == '__main__':
    context.main()
