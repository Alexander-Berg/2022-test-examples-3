# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

from common.models.geo import Settlement
from common.tester.testcase import TestCase
from common.utils.date import MSK_TZ, FuzzyDateTime
from travel.rasp.train_api.helpers.serialization import get_js_datetime


class TestSerializationJsDatetime(TestCase):
    def test_get_js_datetime(self):
        city = Settlement(id=Settlement.KIEV_ID, title=u'Киев', time_zone=u'Europe/Kiev')
        dt = MSK_TZ.localize(datetime(2015, 9, 11, 14, 30))

        time_js = get_js_datetime(city, dt)
        assert time_js == {
            'date': u'2015-09-11T14:30:00+03:00',
            'timezones': {
                'local': u'Europe/Kiev'
            },
            'fuzzy': False
        }

    def test_get_js_fuzzy_datetime(self):
        city = Settlement(id=Settlement.KIEV_ID, title=u'Киев', time_zone=u'Europe/Kiev')
        dt = MSK_TZ.localize(datetime(2015, 9, 11, 14, 30))
        fuzzy_datetime = FuzzyDateTime(dt)

        time_js = get_js_datetime(city, fuzzy_datetime)
        assert time_js == {
            'date': u'2015-09-11T14:30:00+03:00',
            'timezones': {
                'local': u'Europe/Kiev'
            },
            'fuzzy': True
        }
