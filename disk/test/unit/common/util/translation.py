#!/usr/bin/python
# -*- coding: utf-8 -*-
from unittest import TestCase

from mpfs.common.util.translation import unixtime_to_localized_date_string


class TranslationTestCase(TestCase):
    def test_unixtime_to_localized_date_string(self):
        assert u'5 января 2018' == unixtime_to_localized_date_string(1515153600, 'ru')
        assert u'5 January 2018' == unixtime_to_localized_date_string(1515153600, 'en')
        assert u'5 Ocak 2018' == unixtime_to_localized_date_string(1515153600, 'tr')
        assert u'5 січня 2018' == unixtime_to_localized_date_string(1515153600, 'uk')
        assert u'5 січня 2018' == unixtime_to_localized_date_string(1515153600, 'ua')
        assert u'05.01.2018' == unixtime_to_localized_date_string(1515153600, 'aa')
