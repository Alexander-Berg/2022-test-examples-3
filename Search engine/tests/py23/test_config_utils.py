# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.martylib.config_utils import get_clean_hostname
from search.martylib.test_utils import TestCase


class TestCleanHostname(TestCase):
    # noinspection SpellCheckingInspection
    def test_yp_hostname(self):
        self.assertEqual(
            get_clean_hostname('cod7zvzf7xxwwk5y.sas.yp-c.yandex.net'),
            'cod7zvzf7xxwwk5y.sas'
        )

    def test_gencfg_hostname(self):
        self.assertEqual(
            get_clean_hostname('sas1-2345.search.yandex.net'),
            'sas1-2345'
        )
