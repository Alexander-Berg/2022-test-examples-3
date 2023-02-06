# -*- coding: utf-8 -*-
import mock
from freezegun import freeze_time

from travel.avia.ticket_daemon_api.jsonrpc.query import Query
from travel.avia.library.python.tester.factories import create_partner, create_dohop_vendor, create_amadeus_merchant
from travel.avia.ticket_daemon_api.tests.handlers.v3.utils import ApiTestCase, get_client_app


TEST_DATE = '2019-12-01'


class ResultsMetaTest(ApiTestCase):
    def setUp(self):
        super(ResultsMetaTest, self).setUp()
        self.partner_code = None
        self.cache_ttl = None
        self.partner = None
        self.client = get_client_app()

    def create_partner(self, cache_ttl=100):
        self.partner_code = 'test_partner'
        self.cache_ttl = cache_ttl
        self.partner = create_partner(code=self.partner_code, variant_cache_ttl=self.cache_ttl)

    def create_dohop_partner(self, cache_ttl=100):
        self.partner_code = 'dohop_123'
        self.cache_ttl = cache_ttl
        create_partner(code='dohop')
        self.partner = create_dohop_vendor(dohop_id=123, dohop_cache_ttl=self.cache_ttl)

    def create_amadeus_partner(self, cache_ttl=100):
        self.partner_code = 'amadeus_123'
        self.cache_ttl = cache_ttl
        create_partner(code='amadeus')
        self.partner = create_amadeus_merchant(merchant_id=123, variant_cache_ttl=self.cache_ttl)

    def test_valid_response(self):
        self.create_partner()
        response = self.do_patched_request()

        self.assertStatusOK(response)
        data = self.parseData(response)
        assert self.partner_code in data, data

    def test_partner(self):
        self.create_partner()
        self.do_test_meta_for_partner()

    def test_dohop(self):
        self.create_dohop_partner()
        self.do_test_meta_for_partner()

    def test_amadeus(self):
        self.create_amadeus_partner()
        self.do_test_meta_for_partner()

    def test_with_specified_cache_ttl(self):
        self.create_partner(cache_ttl=1200)
        self.do_test_meta_for_partner()

    def do_test_meta_for_partner(self):
        response = self.do_patched_request()
        self.assertStatusOK(response)
        data = self.parseData(response)
        self.assert_partner_results_meta(data[self.partner_code])

    @freeze_time(TEST_DATE)
    def do_patched_request(self):
        with mock.patch.object(
            Query, 'get_enabled_partner_codes', return_value=[self.partner_code],
        ), mock.patch(
            'travel.avia.ticket_daemon_api.jsonrpc.lib.ydb.cache.ServiceCache.get_many', return_value=self.get_ydb_response(),
        ):
            return self.do_request(TEST_DATE)

    def do_request(self, date_forward):
        url = 'jsendapi/v3/results_meta'
        data = dict(
            point_from="c213",
            point_to="c2",
            date_forward=date_forward,
            lang="ru",
            service="ticket",
        )
        return self.client.get(url, data=data)

    def get_ydb_response(self):
        return [{
            'partner_code': self.partner_code,
            'expires_at': self.instant_search_expiration_time,
            'created_at': self.created,
        }]

    def assert_partner_results_meta(self, meta):
        assert meta == {
            'created': self.created,
            'expire': self.expire,
            'instant_search_expiration_time': self.instant_search_expiration_time,
        }

    @property
    def created(self):
        return 1556530056

    @property
    def instant_search_expiration_time(self):
        return 1556530056

    @property
    def expire(self):
        return self.created + (self.cache_ttl + 5) * 60
