#!/usr/bin/python

import unittest
import os
import common

class TestMshIntConfig(unittest.TestCase):

    def test_msh_int_generation(self):
        out = common.generate_config('market-report', 'template.cfg', 'msh-int01zz.market.yandex.net.cfg',
                TARGET_HOST_NAME='msh-int01hp.market.yandex.net',
                ENV_TYPE='production')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertEqual(mr['WizClient'], 'market-main-report-int')
        self.assertEqual(mr['ReqWizardTimeoutMs'], '150')
