# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from common.views.currency import CurrencyInfo
from common.tester.factories import create_currency


class RequestStub(object):
    def __init__(self):
        self.national_version = "ru_RU"
        self.client_city = None
        self.tld = "ru"


class QueryPointsStub(object):
    def __init__(self, point_from, point_to, departure_date=None):
        self.point_from = point_from
        self.point_to = point_to
        self.departure_date = departure_date


class SegmentStub(object):
    def __init__(self, station_from, station_to):
        self.station_from = station_from
        self.station_to = station_to


def get_stub_currency_info():
    return CurrencyInfo(
        selected="RUS",
        set_preferred=True,
        country_base="RUR",
        src=None,
        rates={"RUR": 1},
        available={"RUR"},
        currencies={create_currency(code="RUR")}
    )
