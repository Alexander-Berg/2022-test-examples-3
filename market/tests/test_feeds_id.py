# -*- coding: utf-8 -*-

import flask
import pytest
import six
from hamcrest import (
    assert_that,
    contains_string,
)

from utils import (
    is_error_response,
    is_success_response,
)

from market.idx.api.backend.idxapi import create_flask_app


class StorageMock(object):
    def get_feed_by_id(self, feed_id):
        feeds = [
            {
                'feed': 1069,
                'status': 'system',
                'mbi': six.b(
                    "is_cpa_partner\ttrue\n"
                    "shop_grades_count\t34\n"
                    "local_delivery_cost\t4400\n"
                    "promo_cpa_status\tno\n"
                    "prefix\tHTTP_AUTH='basic:*:marketdatabuild:123'\n"
                    "shop_id\t774\n"
                    "urlforlog\ttest.yandex.ru\n"
                    "priority_regions\t10740\n"
                    "delivery_src\tWEB\n"
                    "is_offline\ttrue\n"
                    "price_scheme\t10;9=5;\n"
                    "phone_display_options\t-\n"
                    "regions\t99073;99072;121344;99075;11010;99074;11012;99077;99076;99079;99078;119046;\n"
                    "priority_region_original\t10740\n"
                    "promo_cpc_status\treal\n"
                    "is_enabled\ttrue\n"
                    "shipping_full_text\t\xD0\x92 \xD0\xBF\xD1\x80\xD0\xB5\xD0\xB4\xD0\xB5\xD0\xBB\xD0\xB0\xD1\x85"
                    " \xD0\xA1\xD0\xBE\xD0\xBB\xD0\xBD\xD0\xB5\xD1\x87\xD0\xBD\xD1\x8B\xD0\xB9 \xD0\xA1\xD0\xB8\xD1"
                    "\x81\xD1\x82\xD0\xB5\xD0\xBC\xD1\x8B \xD0\xB7\xD0\xB0 1 \xD0\xB4\xD0\xB6\xD0\xB0\xD0\xB3\xD0\xBE\xD0\xBD905\n"
                    "shop_delivery_currency\tRUR\n"
                    "prepay_enabled\ttrue\n"
                    "is_cpa_prior\ttrue\n"
                    "shop_currency\tRUR\n"
                    "free\ttrue\n"
                    "home_region\t225\n"
                    "phone\t+7 (095) 1234568\n"
                    "client_id\t325076\n"
                    "is_online\ttrue\n"
                    "shop_cluster_id\t11405\n"
                    "return_delivery_address\t\xD0\xA1\xD0\xB0\xD0\xBD\xD0\xBA\xD1\x82-\xD0\x9F\xD0\xB5\xD1\x82\xD0\xB5"
                    "\xD1\x80\xD0\xB1\xD1\x83\xD1\x80\xD0\xB3, \xD0\x9F\xD0\xB8\xD1\x81\xD0\xBA\xD0\xB0\xD1\x80\xD0\xB5"
                    "\xD0\xB2\xD1\x81\xD0\xBA\xD0\xB8\xD0\xB9 \xD0\xBF\xD1\x80\xD0\xBE\xD1\x81\xD0\xBF\xD0\xB5\xD0\xBA"
                    "\xD1\x82, \xD0\xB4\xD0\xBE\xD0\xBC 2, \xD0\xBA\xD0\xBE\xD1\x80\xD0\xBF\xD1\x83\xD1\x81 2, \xD1\x81"
                    "\xD1\x82\xD1\x80\xD0\xBE\xD0\xB5\xD0\xBD\xD0\xB8\xD0\xB5 3, \xD0\x91\xD0\xA6 \xD0\x91\xD0\xB5\xD0"
                    "\xBD\xD1\x83\xD0\xB0, 195027\n"
                    "tariff\tCLICKS\n"
                    "show_premium\ttrue\n"
                    "shopname\t\xD0\xAF \xD0\xA2\xD0\xB5\xD1\x81\xD1\x82\xD0\xBE\xD0\xB2\xD1\x8B\xD0\xB9 \xD1\x88\xD0\xBE\xD0\xBF\xD0\xBF\n"
                    "is_discounts_enabled\ttrue\n"
                    "url\thttps://svn.yandex.ru:443/market/market/trunk/testshops/testdontdelete.xml\n"
                    "cpa\tREAL\n"
                    "cpc\tREAL\n"
                    "use_open_stat\ttrue\n"
                    "quality_rating\t4\n"
                    "datafeed_id\t1069\n"
                    "datasource_name\ttest.yandex.ru\n"
                ),
            },
            {
                'feed': 10002,
            },
        ]
        for feed in feeds:
            if feed['feed'] == int(feed_id):
                return feed

    def get_all_feed_sessions(self, feed_id):
        return []

    def get_feed_in_index_ts(self, feed_id):
        return []


@pytest.fixture(scope="module")
def test_app():
    return create_flask_app(StorageMock())


def test_unversioned_redirect(test_app):
    with test_app.test_client() as client:
        resp = client.get('/feeds/10')
        assert_that(resp, is_error_response(code=404))


def test_get_feed_by_id_10_not_found(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/10')
        assert_that(resp, is_error_response('404 Not Found', 404))


def test_get_feed_by_id_10002(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/10002')
        assert_that(resp, is_success_response())

        data = flask.json.loads(resp.data)
        assert data['feed'] == 10002
        assert data['self_url'] == 'http://localhost:29334/v1/feeds/10002'


def test_get_feed_by_id_1069(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069')
        assert_that(resp, is_success_response())

        data = flask.json.loads(resp.data)
        assert data['feed'] == 1069
        assert data['status'] == 'system'
        assert data['self_url'] == 'http://localhost:29334/v1/feeds/1069'

        assert data['mbi']['shop_id'] == '774'
        assert data['mbi']['url'] == 'https://svn.yandex.ru:443/market/market/trunk/testshops/testdontdelete.xml'

        assert data['mbi']
        assert data['mbi']['client_id'] == '325076'
        assert data['mbi']['cpa'] == 'REAL'
        assert data['mbi']['cpc'] == 'REAL'
        assert data['mbi']['datafeed_id'] == '1069'
        assert data['mbi']['datasource_name'] == 'test.yandex.ru'
        assert data['mbi']['delivery_src'] == 'WEB'
        assert data['mbi']['free'] == 'true'
        assert data['mbi']['home_region'] == '225'
        assert data['mbi']['is_cpa_partner'] == 'true'
        assert data['mbi']['is_cpa_prior'] == 'true'
        assert data['mbi']['is_discounts_enabled'] == 'true'
        assert data['mbi']['is_enabled'] == 'true'
        assert data['mbi']['is_offline'] == 'true'
        assert data['mbi']['is_online'] == 'true'
        assert data['mbi']['local_delivery_cost'] == '4400'
        assert data['mbi']['phone'] == '+7 (095) 1234568'
        assert data['mbi']['phone_display_options'] == '-'
        assert data['mbi']['prefix'] == "HTTP_AUTH='basic:*:marketdatabuild:123'"
        assert data['mbi']['prepay_enabled'] == 'true'
        assert data['mbi']['price_scheme'] == '10;9=5;'
        assert data['mbi']['priority_region_original'] == '10740'
        assert data['mbi']['priority_regions'] == '10740'
        assert data['mbi']['promo_cpa_status'] == 'no'
        assert data['mbi']['promo_cpc_status'] == 'real'
        assert data['mbi']['quality_rating'] == '4'
        assert data['mbi']['regions'] == '99073;99072;121344;99075;11010;99074;11012;99077;99076;99079;99078;119046;'
        assert data['mbi']['return_delivery_address'] == six.ensure_text('Санкт-Петербург, Пискаревский проспект, дом 2, корпус 2, строение 3, БЦ Бенуа, 195027')
        assert data['mbi']['shipping_full_text'] == six.ensure_text('В пределах Солнечный Системы за 1 джагон905')
        assert data['mbi']['shop_cluster_id'] == '11405'
        assert data['mbi']['shop_currency'] == 'RUR'
        assert data['mbi']['shop_delivery_currency'] == 'RUR'
        assert data['mbi']['shop_grades_count'] == '34'
        assert data['mbi']['shop_id'] == '774'
        assert data['mbi']['shopname'] == six.ensure_text('Я Тестовый шопп')
        assert data['mbi']['show_premium'] == 'true'
        assert data['mbi']['tariff'] == 'CLICKS'
        assert data['mbi']['url'] == 'https://svn.yandex.ru:443/market/market/trunk/testshops/testdontdelete.xml'
        assert data['mbi']['urlforlog'] == 'test.yandex.ru'
        assert data['mbi']['use_open_stat'] == 'true'


def test_parse_format_json(test_app):
    with test_app.test_request_context('/v1/feeds/1069?format=json'):
        assert flask.request.path == '/v1/feeds/1069'
        assert flask.request.args['format'] == 'json'


def test_parse_format_xml(test_app):
    with test_app.test_request_context('/v1/feeds/1069?format=xml'):
        assert flask.request.path == '/v1/feeds/1069'
        assert flask.request.args['format'] == 'xml'


def test_response_headers_default_json(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069')
        assert_that(resp, is_success_response(content_type='application/json; charset=utf-8'))


def test_response_headers_format_json(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069?format=json')
        assert_that(resp, is_success_response(content_type='application/json; charset=utf-8'))


def test_response_headers_format_xml(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069?format=xml')
        assert_that(
            resp,
            is_success_response(
                data=contains_string('<?xml version="1.0" encoding="utf-8"?>'),
                content_type='application/xml; charset=utf-8'
            )
        )


def test_response_headers_format_undef(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069?format=someformat')
        assert_that(
            resp,
            is_error_response('406 Not Acceptable\nrequest mime type is not implemented: someformat', 406)
        )
