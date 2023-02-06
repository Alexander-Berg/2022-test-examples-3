# -*- coding: utf-8 -*-
# flake8: noqa
import requests
import ujson as json
import zlib
from urllib import urlencode
from urlparse import urljoin

import contextlib2
import mock
import pytest
import requests_mock
from freezegun import freeze_time
from django.conf import settings
from django.test import override_settings

from travel.avia.library.python.avia_data.models import NationalVersion
from travel.avia.library.python.common.models.geo import StationType
from travel.avia.library.python.common.models.partner import DefaultClickPrice
from travel.avia.library.python.common.models.service import Service
from travel.avia.library.python.common.models.team import Team

from travel.avia.ticket_daemon_api.jsonrpc.lib.flights import Variant, IATAFlight
from travel.avia.library.python.tester.factories import (
    get_model_factory, create_dohop_vendor, create_partner, create_station, create_settlement,
)
from travel.avia.ticket_daemon_api.jsonrpc.lib.result.collector.variants_fabric import (
    ApiVariants,
    fill_fare_family,
    fill_fare_family_from_variants_cache,
)
from travel.avia.ticket_daemon_api.jsonrpc.lib.internal_daemon_client import RedirectData
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon_api.tests.daemon_tester import create_query
from travel.avia.ticket_daemon_api.tests.fixtures.api_variants import (
    get_api_variant,
    get_default_cheap_variants,
    get_default_expensive_and_cheap_variants,
    get_default_flights,
    get_default_variants_with_and_without_baggage,
)
from travel.avia.ticket_daemon_api.tests.handlers.v3.utils import ApiTestCase, get_client_app


def get_internal_daemon_url(route):
    return reduce(urljoin, [settings.TICKET_DAEMON_URL, '/api/1.0/', route])


@freeze_time('2018-03-21')
class TestWizardResults(ApiTestCase):
    def tearDown(self):
        reset_all_caches()

    def test_wizard_results_empty(self):
        query_search_params = {
            'point_from': 'c213',
            'point_to': 'c2',
            'lang': 'ru',
            'service': 'ticket',
            'national': 'ru',
            'geo_id': 54,
            'mobile': False,
            'klass': 'economy',
            'date_forward': '2018-04-19',
            'date_backward': '2018-04-27',
            'adults': 1,
            'children': 0,
            'infants': 0
        }

        variants_by_partner = {}
        qid = None

        with mock.patch(
            'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.resultlib.collect_wizard_variants',
            return_value=(variants_by_partner, qid)
        ) as collect_variants:
            r = get_client_app().get(
                'jsendapi/v3/wizard_results?{}'.format(urlencode(query_search_params))
            )

        self.assertStatusOK(r)
        data = json.loads(r.data)['data']
        assert data['qid'] is None

    def test(self):
        query_search_params = {
            'point_from': 'c213',
            'point_to': 'c2',
            'lang': 'ru',
            'service': 'ticket',
            'national': 'ru',
            'geo_id': 54,
            'mobile': False,
            'klass': 'economy',
            'date_forward': '2018-04-19',
            'date_backward': '2018-04-27',
            'adults': 1,
            'children': 0,
            'infants': 0
        }
        test_partner = create_partner(code='test_partner', site_url='site_url')
        create_station(id=9600215, settlement_id=2, title='Station from',
                       station_type_id=StationType.AIRPORT_ID)
        create_station(id=9600366, settlement_id=213, title='Station to',
                       station_type_id=StationType.AIRPORT_ID)
        variants_by_partner = {test_partner.code: get_api_variant(test_partner)[1]}
        test_qid = 'test_qid'

        with mock.patch(
            'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.resultlib.collect_wizard_variants',
            return_value=(variants_by_partner, test_qid)
        ), mock.patch(
            'travel.avia.ticket_daemon_api.jsonrpc.lib.feature_flags.fill_fare_family_enabled', return_value=True,
        ):
            r = get_client_app().get(
                'jsendapi/v3/wizard_results?{}'.format(
                    urlencode(query_search_params))
            )

        self.assertStatusOK(r)
        data = json.loads(r.data)['data']
        assert data['variants']['fares'][0]['prices'][0]['fare_families'] == [[None], []]
        assert data['variants']['fares'][0]['prices'][0]['fare_families_hash'] == 'usual_fare'
        assert data['qid'] == test_qid


def get_redirect_query(**kwargs):
    query = dict(
        partner='test_partner',
        qid='180209-094810-812.ticket.plane.c2_c213_2018-04-02_None_economy_1_0_0_ru.ru',
        forward='UT 489.2018-04-10T00:50',
        backward='',
        service='ticket',
        lang='ru',
        point_from='c2',
        point_to='c213',
        date_forward='2018-04-02',
        user_info=json.dumps(
            {
                "yandexuid": "123", "passportuid": "0",
                "userip": "192.168.0.1",
                "yauser": {"uid": "1", "login": "yndx"},
                "django_user": {"username": "Vasya.Pupkin", "is_staff": False}
            },
        ),
    )

    query.update(kwargs)
    return query


def get_redirect(query):
    return get_client_app().get(
        'jsendapi/v3/redirect?{}'.format(urlencode(query))
    )


class TestFareFamiliesFromCache(ApiTestCase):
    def setUp(self):
        super(TestFareFamiliesFromCache, self).setUp()

    def test_fare_families_from_cache(self):
        partner = create_partner(code='test_partner', site_url='site_url')
        query = create_query()
        cheap_fares = get_default_cheap_variants(partner, 'cheap').variants
        cheap_fares_map = {fare['tag']:fare for fare in cheap_fares}
        sample_fare_family = {
            'base_class': 'ECONOMY',
            'brand': 'OPTIMUM',
            'terms': [{
                'code': 'baggage',
                'rules': [{
                    'availability': '',
                    'places': 2,
                    'size': 158,
                    'weight': 23,
                }],
            }],
            'key': 'ff_key2',
        }
        fare_families_data = {
            'data': [{
                'fareFamilies': {
                    'ff_key': sample_fare_family,
                },
                'variantsMap': {
                    'cheap': {
                        'fare_families': [['ff_key'], []],
                        'fare_families_hash': '22dbb04331d1628e249aee92bc1aba20',
                    },
                },
            }],
        }
        fill_fare_family_from_variants_cache(fare_families_data, cheap_fares_map)
        assert cheap_fares_map['cheap']['fare_families'] == [[sample_fare_family],[]]


class TestFareFamiliesHash(ApiTestCase):
    def setUp(self):
        super(TestFareFamiliesHash, self).setUp()

    def get_default_variant(self, baggage, fare_family):
        return {
            'tag': {
                u'charter': None,
                u'route': [[u'1804100050UT489'], []],
                u'baggage': [[baggage], [baggage]],
                u'fare_codes': [[None], []],
                u'fare_families': [[None], [fare_family]],
                u'created': 1518149935,
                u'expire': 1518151435,
                u'tariff': {u'currency': u'RUR', u'value': 1234.0},
            }
        }

    def test_fill_fare_families_hash(self):
        flights = get_default_flights()
        query = create_query()

        variant = self.get_default_variant('0d1d0p', 'ЛАЙТ')
        fill_fare_family(variant, flights, query)
        assert variant['tag']['fare_families_hash'] == 'f8320b26d30ab433c5a54546d21f414c'

        # modify baggage but keep it at 0 ==> fare_families_hash should not change
        variant = self.get_default_variant('0d10d1p', 'ЛАЙТ')
        fill_fare_family(variant, flights, query)
        assert variant['tag']['fare_families_hash'] == 'f8320b26d30ab433c5a54546d21f414c'

        # modify baggage ==> fare_families_hash should change too
        variant = self.get_default_variant('1d1d10p', 'ЛАЙТ')
        fill_fare_family(variant, flights, query)
        assert variant['tag']['fare_families_hash'] == 'f827cf462f62848df37c5e1e94a4da74'

        # modify fare family ==> fare_families_hash should change too
        variant = self.get_default_variant('0d1d0p', 'МАКС')
        fill_fare_family(variant, flights, query)
        assert variant['tag']['fare_families_hash'] == 'f8320b26d30ab433c5a54546d21f414c'


@freeze_time('2018-03-21')
class TestRedirect(ApiTestCase):
    def setUp(self):
        super(TestRedirect, self).setUp()
        create_partner(code='dohop', site_url='site_url')
        self.dohop_partner = create_dohop_vendor(id=123456, dohop_id=1234, enabled=True)
        self.common_partner = create_partner(code='test_partner', site_url='site_url')
        self.covid19_partner = create_partner(
            code=next(iter(settings.COVID19_ZERO_REDIRECT_PRICE_PARTNERS)),
            site_url='site_url',
        )

        create_station(id=9600215, settlement_id=2, title='Station from',
                       station_type_id=StationType.AIRPORT_ID)
        create_station(id=9600366, settlement_id=213, title='Station to',
                       station_type_id=StationType.AIRPORT_ID)

        click_price_factory = get_model_factory(DefaultClickPrice)
        nv = NationalVersion.objects.get(code='ru')
        click_price_factory(billing_price=100, national_version=nv)

        self.fake_redirect_data = {
            u'query_source': u'ticket',
            u'order_data': {u'url': u'https://ya.ru'}
        }

    def run_redirect_click_price(self, partner, avia_brand, click_price):
        if avia_brand is not None:
            query = get_redirect_query(partner=partner.code, avia_brand=avia_brand.code)
        else:
            query = get_redirect_query(partner=partner.code)

        tag, variant = get_api_variant(partner)
        with mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.fetcherlib.get_variants_and_redirect_data',
                    return_value=[(
                        partner.code,
                        {
                            'redirect_data': pack({'variants': {tag: self.fake_redirect_data}}),
                            'variants': {partner.code: variant},
                        },
                    )],
                ), \
                mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.lib.result.collector.variants.collect_variants_v3',
                    return_value=(
                        {partner.code: variant}, {partner.code: u'done'}, None
                    )
                ), \
                mock.patch('travel.avia.ticket_daemon_api.jsonrpc.query.Query.get_enabled_partner_codes', return_value=[partner.code]),\
                mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.internal_daemon_client'
                ) as internal_daemon_client_mock, \
                mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.resultlib.collect_statuses',
                    return_value={partner.code: {'status': u'done'}}):
            internal_daemon_client_mock.redirect = mock.Mock(
                return_value=RedirectData(url='https://ya.ru', query_source='test')
            )
            r = get_redirect(query)
            response_data = json.loads(r.data)

            self.assertStatusOK(r)
            assert response_data['status'] == 'success'
            assert response_data['data']['click_price']['price'] == click_price
            assert response_data['data']['click_price']['settlement_from_id'] == 2
            assert response_data['data']['click_price']['settlement_to_id'] == 213

    def test_redirect_click_price(self):
        """
        Проверяем, что для дохопа проставляется цена клика 0, а для всех остальных
        дефолтная в фишках
        """
        self.run_redirect_click_price(self.dohop_partner, None, 0.)

        settings.COVID19_ZERO_REDIRECT_PRICE = True
        self.run_redirect_click_price(self.common_partner, None, 1.)
        self.run_redirect_click_price(self.covid19_partner, None, 0.)
        settings.COVID19_ZERO_REDIRECT_PRICE = False
        self.run_redirect_click_price(self.common_partner, None, 1.)

        settings.BANNED_PARTNERS_FOR_RESULTS = set([self.common_partner.code])
        self.run_redirect_click_price(self.common_partner, self.common_partner, 0.)
        self.run_redirect_click_price(self.common_partner, self.dohop_partner, 1.)

        settings.BANNED_PARTNERS_FOR_RESULTS = set()
        self.run_redirect_click_price(self.common_partner, self.common_partner, 1.)
        self.run_redirect_click_price(self.common_partner, self.dohop_partner, 1.)

    def run_redirect_platform_test(self, service):
        redirect_data = {
            'url': 'https://ya.ru',
            'm_url': 'https://m.ya.ru',
            'query_source': 'test'
        }
        expected_url = redirect_data['m_url'] if service.is_mobile else redirect_data['url']
        partner = self.common_partner
        query = get_redirect_query(service=service.code)

        self.fake_redirect_data = {
            u'query_source': u'ticket',
            u'order_data': redirect_data,
        }
        tag, variant = get_api_variant(partner)
        with mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.fetcherlib.get_variants_and_redirect_data',
                    return_value=[(
                        partner.code,
                        {
                            'redirect_data': pack({'variants': {tag: self.fake_redirect_data}}),
                            'variants': {partner.code: variant},
                        },
                    )],
                ), \
                mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.lib.result.collector.variants.collect_variants_v3',
                    return_value=(
                        {partner.code: variant}, {partner.code: u'done'}, None
                    ),
                ), \
                mock.patch('travel.avia.ticket_daemon_api.jsonrpc.query.Query.get_enabled_partner_codes',
                            return_value=[partner.code]), \
                mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.internal_daemon_client',
                ) as internal_daemon_client_mock, \
                mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.resultlib.collect_statuses',
                    return_value={partner.code: {'status': u'done'}}
                ):
            internal_daemon_client_mock.redirect = mock.Mock(
                return_value=RedirectData(**redirect_data)
            )
            r = get_redirect(query)
            response_data = json.loads(r.data)

            self.assertStatusOK(r)
            assert response_data['data']['url'].startswith(expected_url)

    def test_redirect_platform(self):
        """
        Проверяем, что тип платформы определяется по переданному сервису
        и возвращаются нужные ссылки на редирект
        """
        team = get_model_factory(Team).create_model(code='test_team')
        team.save()
        m_avia = get_model_factory(Service).create_model(
            code='m_avia_test', team=team, is_mobile=True
        )
        m_avia.save()
        ticket = get_model_factory(Service).create_model(
            code='ticket_test', team=team, is_mobile=False
        )
        ticket.save()
        self.run_redirect_platform_test(ticket)
        self.run_redirect_platform_test(m_avia)

    @requests_mock.mock()
    def test_boy_redirect(self, req_mock):
        query = get_redirect_query(partner=self.common_partner.code)
        with contextlib2.ExitStack() as stack:
            self.patch_modules_for_test_boy(stack)

            book_redirect = u'https://book.ru'
            req_mock.post(
                get_internal_daemon_url('book_redirect/'),
                json={u'url': book_redirect},
            )

            r = get_redirect(query)
            response_data = json.loads(r.data)

            self.assertStatusOK(r)
            assert response_data['data']['url'].startswith(book_redirect)

            assert req_mock.call_count == 1
            assert req_mock.request_history[0].url == get_internal_daemon_url('book_redirect/')

    def test_404_response_on_404_boy_response(self):
        boy_status_code = 404
        with contextlib2.ExitStack() as stack:
            self.patch_modules_for_test_boy(stack)

            boy_response = {
                'message': 'Booking is not available',
                'content': 'Some error',
                'status_code': boy_status_code,
            }

            req_mock = stack.enter_context(requests_mock.mock())
            req_mock.post(
                get_internal_daemon_url('book_redirect/'),
                json=boy_response,
                status_code=400,
            )

            query = get_redirect_query(partner=self.common_partner.code)
            r = get_redirect(query)

            self.assertStatusOK(r, status_code=boy_status_code)
            response_data = self.parseData(r)
            assert response_data == boy_response
            assert req_mock.call_count == 1

    def test_fallback_to_cook_redirect_if_boy_unavailable(self):
        for boy_status_code in [400, 500, 502, 503, 504]:
            with contextlib2.ExitStack() as stack:
                self.patch_modules_for_test_boy(stack)

                req_mock = stack.enter_context(requests_mock.mock())
                req_mock.post(
                    get_internal_daemon_url('book_redirect/'),
                    json={
                        'message': 'some error message',
                        'content': 'error',
                        'status_code': boy_status_code,
                    },
                    status_code=400,
                )

                self.do_fallback_check_for_book_redirect(req_mock)

    @requests_mock.mock()
    def test_fallback_to_cook_redirect_if_boy_timeout(self, req_mock):
        with contextlib2.ExitStack() as stack:
            self.patch_modules_for_test_boy(stack)

            req_mock.post(
                get_internal_daemon_url('book_redirect/'),
                text='Booking service request timeout',
                status_code=408,
            )

            self.do_fallback_check_for_book_redirect(req_mock)

    @requests_mock.mock()
    def test_fallback_to_cook_redirect_if_server_timeout(self, req_mock):
        with contextlib2.ExitStack() as stack:
            self.patch_modules_for_test_boy(stack)

            req_mock.post(
                get_internal_daemon_url('book_redirect/'),
                exc=requests.exceptions.Timeout,
            )

            self.do_fallback_check_for_book_redirect(req_mock)

    def do_fallback_check_for_book_redirect(self, req_mock):
        internal_redirect = u'https://internal.ru'
        query = get_redirect_query(partner=self.common_partner.code)

        req_mock.post(get_internal_daemon_url('cook_redirect/'), json={u'url': internal_redirect})

        r = get_redirect(query)
        response_data = json.loads(r.data)

        self.assertStatusOK(r)

        assert response_data['data']['url'].startswith(internal_redirect)

        assert req_mock.call_count == 2
        assert req_mock.request_history[0].url == get_internal_daemon_url('book_redirect/')
        assert req_mock.request_history[1].url == get_internal_daemon_url('cook_redirect/')

    def patch_modules_for_test_boy(self, stack):
        partner = self.common_partner
        tag, variant = get_api_variant(partner)
        stack.enter_context(
            mock.patch(
                'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.fetcherlib.get_variants_and_redirect_data',
                return_value=[(
                    partner.code,
                    {
                        'redirect_data': pack({'variants': {tag: self.fake_redirect_data}}),
                        'variants': {partner.code: variant},
                    },
                )],
            )
        )
        variants_result = ({partner.code: variant}, {partner.code: u'done'}, None)
        stack.enter_context(mock.patch(
            'travel.avia.ticket_daemon_api.jsonrpc.lib.result.collector.variants.collect_variants_v3',
            return_value=variants_result,
        ))

        stack.enter_context(mock.patch(
            'travel.avia.ticket_daemon_api.jsonrpc.query.Query.get_enabled_partner_codes', return_value=[partner.code],
        ))

        stack.enter_context(mock.patch(
            'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.resultlib.collect_statuses',
            return_value={partner.code: {'status': u'done'}}
        ))

        stack.enter_context(mock.patch(
            'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.is_boy_enabled_for_variant',
            return_value=True,
        ))

        stack.enter_context(mock.patch(
            'travel.avia.ticket_daemon_api.jsonrpc.lib.feature_flags.return_404_status_code_on_boy_404_response',
            return_value=True,
        ))


@freeze_time('2018-03-21')
class RedirectShownTariff(ApiTestCase):
    def setUp(self):
        super(RedirectShownTariff, self).setUp()
        create_station(id=9600215, settlement_id=2, title='Station from',
                       station_type_id=StationType.AIRPORT_ID)
        create_station(id=9600366, settlement_id=213, title='Station to',
                       station_type_id=StationType.AIRPORT_ID)

    def test_redirect_shown_tariff(self):
        partner = create_partner(code='test_partner', site_url='site_url')
        query = get_redirect_query(partner=partner.code)
        query['tariff_sign'] = '5620|RUR|9949cf55cffdcd1066eed8352c14b4c9|1586506627'

        click_price_factory = get_model_factory(DefaultClickPrice)
        nv = NationalVersion.objects.get(code='ru')
        click_price_factory(billing_price=100, national_version=nv)
        fake_redirect_data = {
            u'query_source': u'ticket',
            u'order_data': {u'url': u'https://ya.ru'}
        }
        tag, variant = get_api_variant(partner)
        with mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.fetcherlib.get_variants_and_redirect_data',
                    return_value=[(
                        partner.code,
                        {
                            'redirect_data': pack({'variants': {tag: fake_redirect_data}}),
                            'variants': {partner.code: variant},
                        },
                    )],
                ), \
                mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.lib.result.collector.variants.collect_variants_v3',
                    return_value=(
                        {partner.code: variant}, {partner.code: u'done'}, None
                    )
                ) as collect_variants_mock, \
                mock.patch('travel.avia.ticket_daemon_api.jsonrpc.query.Query.get_enabled_partner_codes', return_value=[partner.code]),\
                mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.internal_daemon_client'
                ) as internal_daemon_client_mock, \
                mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.resultlib.collect_statuses',
                    return_value={partner.code: {'status': u'done'}}), \
                mock.patch('travel.avia.ticket_daemon_api.jsonrpc.lib.tariff_serializer.TariffSerializer.deserialize', return_value={
                    'value': 5620,
                    'currency': 'RUR',
                    'price_unixtime': '1586506627',
                }):
            internal_daemon_client_mock.redirect = mock.Mock(
                return_value=RedirectData(url='https://ya.ru',
                                          query_source='test')
            )
            r = get_client_app().get(
                'jsendapi/v3/redirect?{}'.format(urlencode(query))
            )
            response_data = json.loads(r.data)

            self.assertStatusOK(r)
            assert response_data['status'] == 'success'
            assert response_data['data']['shown_tariff']['value'] == 5620
            assert response_data['data']['shown_tariff']['currency'] == 'RUR'
            assert response_data['data']['shown_tariff']['price_unixtime'] == '1586506627'

    def test_redirect_selects_minimal_tariff(self):
        partner = create_partner(code='test_partner', site_url='site_url')
        query = get_redirect_query(partner=partner.code)
        query['tariff_sign'] = 'tariff-sign'

        click_price_factory = get_model_factory(DefaultClickPrice)
        nv = NationalVersion.objects.get(code='ru')
        click_price_factory(billing_price=100, national_version=nv)
        fake_redirect_data_normal = {
            u'query_source': u'ticket',
            u'order_data': {u'url': u'https://ya.ru/normal'}
        }
        fake_redirect_data_cheap = {
            u'query_source': u'ticket',
            u'order_data': {u'url': u'https://ya.ru/cheap'}
        }
        fake_redirect_data_expensive = {
            u'query_source': u'ticket',
            u'order_data': {u'url': u'https://ya.ru/expensive'}
        }
        cheap_tag = 'cheap'
        normal_tag = 'normal'
        expensive_tag = 'expensive'
        variants = get_default_expensive_and_cheap_variants(partner, cheap_tag, normal_tag, expensive_tag)

        with mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.fetcherlib.get_variants_and_redirect_data',
                    return_value=[(
                        partner.code,
                        {
                            'redirect_data': pack(
                                {
                                    'variants': {
                                        normal_tag: fake_redirect_data_normal,
                                        cheap_tag: fake_redirect_data_cheap,
                                        expensive_tag: fake_redirect_data_expensive,
                                    },
                                },
                            ),
                            'variants': {
                                partner.code: variants
                            },
                        },
                    )],
                ), \
                mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.lib.result.collector.variants.collect_variants_v3',
                    return_value=(
                        {partner.code: variants}, {partner.code: u'done'}, None
                    )
                ) as collect_variants_mock, \
                mock.patch('travel.avia.ticket_daemon_api.jsonrpc.query.Query.get_enabled_partner_codes', return_value=[partner.code]),\
                mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.internal_daemon_client'
                ) as internal_daemon_client_mock, \
                mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.lib.feature_flags.store_min_tariff_per_fare_code', return_value=True,
                ), \
                mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.resultlib.collect_statuses',
                    return_value={partner.code: {'status': u'done'}}):

            internal_daemon_client_mock.redirect = mock.Mock(
                side_effect=daemon_mirror_response_redirect
            )

            r = get_client_app().get(
                'jsendapi/v3/redirect?{}'.format(urlencode(query))
            )
            response_data = json.loads(r.data)

            self.assertStatusOK(r)
            assert response_data['status'] == 'success'
            response_fares = response_data['data']['variants']['fares']
            assert len(response_fares) > 0
            response_prices = response_fares[0]['prices']
            assert len(response_prices) > 0
            assert response_prices[0]['tariff']['value'] == 1950
            assert response_prices[0]['tariff']['currency'] == 'RUR'
            assert response_data['data']['url'].startswith(fake_redirect_data_cheap['order_data']['url'])

    def test_redirect_by_fare_family_hash(self):
        partner = create_partner(code='test_partner', site_url='site_url')
        query = get_redirect_query(partner=partner.code)
        query['tariff_sign'] = 'tariff-sign'

        click_price_factory = get_model_factory(DefaultClickPrice)
        nv = NationalVersion.objects.get(code='ru')
        click_price_factory(billing_price=100, national_version=nv)
        fake_redirect_data_normal = {
            u'query_source': u'ticket',
            u'order_data': {u'url': u'https://ya.ru/normal'}
        }
        fake_redirect_data_cheap = {
            u'query_source': u'ticket',
            u'order_data': {u'url': u'https://ya.ru/cheap'}
        }
        fake_redirect_data_expensive = {
            u'query_source': u'ticket',
            u'order_data': {u'url': u'https://ya.ru/expensive'}
        }
        cheap_tag = 'cheap'
        normal_tag = 'normal'
        expensive_tag = 'expensive'
        variants = get_default_expensive_and_cheap_variants(partner, cheap_tag, normal_tag, expensive_tag)

        with mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.fetcherlib.get_variants_and_redirect_data',
                    return_value=[(
                        partner.code,
                        {
                            'redirect_data': pack(
                                {
                                    'variants': {
                                        normal_tag: fake_redirect_data_normal,
                                        cheap_tag: fake_redirect_data_cheap,
                                        expensive_tag: fake_redirect_data_expensive,
                                    },
                                },
                            ),
                            'variants': {
                                partner.code: variants
                            },
                        },
                    )],
                ), \
                mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.lib.result.collector.variants.collect_variants_v3',
                    return_value=(
                        {partner.code: variants}, {partner.code: u'done'}, None
                    )
                ) as collect_variants_mock, \
                mock.patch('travel.avia.ticket_daemon_api.jsonrpc.query.Query.get_enabled_partner_codes', return_value=[partner.code]),\
                mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.internal_daemon_client'
                ) as internal_daemon_client_mock, \
                mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.lib.feature_flags.store_min_tariff_per_fare_code', return_value=True,
                ), \
                mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.resultlib.collect_statuses',
                    return_value={partner.code: {'status': u'done'}}):

            internal_daemon_client_mock.redirect = mock.Mock(
                side_effect=daemon_mirror_response_redirect
            )

            query['fare_families_hash'] = 'expensive_fare'
            r = get_client_app().get(
                'jsendapi/v3/redirect?{}'.format(urlencode(query))
            )
            response_data = json.loads(r.data)

            self.assertStatusOK(r)
            assert response_data['status'] == 'success'
            response_fares = response_data['data']['variants']['fares']
            assert len(response_fares) > 0
            response_prices = response_fares[0]['prices']
            assert len(response_prices) > 0
            assert response_prices[0]['tariff']['value'] == 1999
            assert response_prices[0]['tariff']['currency'] == 'RUR'
            assert response_data['data']['url'].startswith(fake_redirect_data_expensive['order_data']['url'])

    def test_redirect_filters_baggage(self):
        partner = create_partner(code='test_partner', site_url='site_url')
        query = get_redirect_query(partner=partner.code)
        query['tariff_sign'] = 'tariff-sign'
        query['with_baggage'] = 'True'

        click_price_factory = get_model_factory(DefaultClickPrice)
        nv = NationalVersion.objects.get(code='ru')
        click_price_factory(billing_price=100, national_version=nv)
        fake_redirect_data_normal_no_baggage = {
            u'query_source': u'ticket',
            u'order_data': {u'url': u'https://ya.ru/normal'}
        }
        fake_redirect_data_cheap_no_baggage = {
            u'query_source': u'ticket',
            u'order_data': {u'url': u'https://ya.ru/cheap'}
        }
        fake_redirect_data_expensive_with_baggage = {
            u'query_source': u'ticket',
            u'order_data': {u'url': u'https://ya.ru/with_baggage'}
        }
        cheap_tag = 'cheap_no_baggage'
        normal_tag = 'normal_no_baggabe'
        with_baggage_tag = 'expensive_with_baggage'
        variants = get_default_variants_with_and_without_baggage(partner, cheap_tag, normal_tag, with_baggage_tag)

        with mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.fetcherlib.get_variants_and_redirect_data',
                    return_value=[(
                        partner.code,
                        {
                            'redirect_data': pack(
                                {
                                    'variants': {
                                        normal_tag: fake_redirect_data_normal_no_baggage,
                                        cheap_tag: fake_redirect_data_cheap_no_baggage,
                                        with_baggage_tag: fake_redirect_data_expensive_with_baggage,
                                    },
                                },
                            ),
                            'variants': {
                                partner.code: variants
                            },
                        },
                    )],
                ), \
                mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.lib.result.collector.variants.collect_variants_v3',
                    return_value=(
                        {partner.code: variants}, {partner.code: u'done'}, None
                    )
                ) as collect_variants_mock, \
                mock.patch('travel.avia.ticket_daemon_api.jsonrpc.query.Query.get_enabled_partner_codes', return_value=[partner.code]),\
                mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.internal_daemon_client'
                ) as internal_daemon_client_mock, \
                mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.lib.feature_flags.store_min_tariff_per_fare_code', return_value=True,
                ), \
                mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.resultlib.collect_statuses',
                    return_value={partner.code: {'status': u'done'}}):

            internal_daemon_client_mock.redirect = mock.Mock(
                side_effect=daemon_mirror_response_redirect
            )

            r = get_client_app().get(
                'jsendapi/v3/redirect?{}'.format(urlencode(query))
            )
            response_data = json.loads(r.data)

            self.assertStatusOK(r)
            assert response_data['status'] == 'success'
            response_fares = response_data['data']['variants']['fares']
            assert len(response_fares) > 0
            response_prices = response_fares[0]['prices']
            assert len(response_prices) > 0
            assert response_prices[0]['tariff']['value'] == 2099
            assert response_prices[0]['tariff']['currency'] == 'RUR'
            assert response_data['data']['url'].startswith(fake_redirect_data_expensive_with_baggage['order_data']['url'])


def daemon_mirror_response_redirect(
    order_data,
    user_info,
    utm_source,
    query_source,
    additional_data,
    timeout,
):
    return RedirectData(
        url=order_data['url'],
        query_source=query_source,
    )


@freeze_time('2018-03-21')
class RequestWithoutDepartureDT(ApiTestCase):
    def setUp(self):
        super(RequestWithoutDepartureDT, self).setUp()
        self.redirect_url = 'https://ya.ru'
        self.number = 'UT 489'

    def send(self, url):
        partner = create_partner(code='test_partner', site_url='')
        create_station(id=9600215, settlement_id=2, title='Station from',
                       station_type_id=StationType.AIRPORT_ID)
        create_station(id=9600366, settlement_id=213, title='Station to',
                       station_type_id=StationType.AIRPORT_ID)
        forward_tags = [IATAFlight.make_flight_tag(None, self.number)]
        tag = Variant.make_tag(
            forward_tags, [], 'economy', partner.code, False
        )

        route = self.number.replace(' ', '')
        flights = {route: {
            u'arrival': {u'local': u'2018-04-10T02:15:00',
                         u'tzname': u'Europe/Moscow',
                         u'offset': 180.0},
            u'from': 9600215, u'company': 29, u'aviaCompany': 29,
            u'number': self.number,
            u'departure': None,
            u'to': 9600366,
            u'companyTariff': 58, u'key': route
        }}
        fares = [{
            u'charter': None,
            u'route': [[route], []], u'baggage': [[u'0d1d0p'], []],
            u'fare_codes': [[None], []],
            u'fare_families': [[None], []],
            'tag': tag,
            u'created': 1518149935, u'expire': 1518151435,
            u'tariff': {u'currency': u'RUR', u'value': 1990.0}
        }]

        variants = ApiVariants(
            qid='QID',
            flights=flights,
            fares=fares,
            status='done',
            query_time=1,
            revision=0,
        )
        fake_redirect_data = {
            'order_data': {'url': self.redirect_url},
            'query_source': 'ticket',
        }
        with mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.fetcherlib.get_variants_and_redirect_data',
                    return_value=[(
                        partner.code,
                        {
                            'redirect_data': pack({'variants': {tag: fake_redirect_data}}),
                            'variants': {partner.code: variants},
                        },
                    )],
                ):
            with mock.patch(
                'travel.avia.ticket_daemon_api.jsonrpc.lib.result.collector.variants.collect_variants_v3',
                return_value=(
                    {partner.code: variants}, {partner.code: u'done'}, None
                )
            ) as collect_variants_mock:
                with mock.patch(
                    'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.internal_daemon_client'
                ) as internal_daemon_client_mock:
                    with mock.patch(
                        'travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views._get_click_price',
                        return_value={'click_price': {}}
                    ):
                        with mock.patch('travel.avia.ticket_daemon_api.jsonrpc.query.Query.get_enabled_partner_codes', return_value=[partner.code]):
                            with mock.patch('travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.resultlib.collect_statuses',
                                            return_value={partner.code: {'status': u'done'}}):
                                internal_daemon_client_mock.redirect = mock.Mock(
                                    return_value=RedirectData(url=self.redirect_url, query_source='test')
                                )
                                return get_client_app().get(url)

    def test_redirect(self):
        query = get_redirect_query(forward='%s.' % self.number)
        r = self.send('jsendapi/v3/redirect?{}'.format(urlencode(query)))

        self.assertStatusOK(r)
        response = json.loads(r.data)['data']
        assert 'url' in response and response['url'].startswith(self.redirect_url)
        assert response['reference']['flights']
        assert response['variants']

    def test_order(self):
        query = get_redirect_query(forward='%s.' % self.number)
        r = self.send('jsendapi/v3/order?{}'.format(urlencode(query)))
        self.assertStatusOK(r)
        response = json.loads(r.data)['data']
        assert response['reference']['flights']
        assert response['variants']


@freeze_time('2018-03-21')
class ResultsTest(ApiTestCase):
    def setUp(self):
        super(ResultsTest, self).setUp()
        self.number = 'UT 489'
        self.partner = create_partner(code='test_partner', site_url='example-partner.com')

    def send(self, url, status=u'done'):
        forward_tags = [IATAFlight.make_flight_tag(None, self.number)]
        tag = Variant.make_tag(
            forward_tags, [], 'economy', self.partner.code, False
        )
        create_station(id=9600215, settlement_id=2, title='Station from',
                       station_type_id=StationType.AIRPORT_ID)
        create_station(id=9600366, settlement_id=213, title='Station to',
                       station_type_id=StationType.AIRPORT_ID)
        create_settlement(id=2, iata='LED')
        create_settlement(id=213, iata='MOW')
        route = self.number.replace(' ', '')
        flights = {route: {
            u'arrival': {u'local': u'2018-04-10T02:15:00',
                         u'tzname': u'Europe/Moscow',
                         u'offset': 180.0},
            u'from': 9600215, u'company': 29, u'aviaCompany': 29,
            u'number': self.number,
            u'departure': None,
            u'to': 9600366,
            u'companyTariff': 58, u'key': route
        }}
        fares = [{
            u'charter': None,
            u'route': [[route], []], u'baggage': [[u'0d1d0p'], []],
            u'fare_codes': [[None], []],
            u'fare_families': [[None], []],
            'tag': tag,
            u'created': 1518149935, u'expire': 1518151435,
            u'tariff': {u'currency': u'RUR', u'value': 1990.0}
        }]

        variants = ApiVariants(
            qid='QID',
            flights=flights,
            fares=fares,
            status=status,
            query_time=1,
            revision=0,
        )
        with mock.patch(
                'travel.avia.ticket_daemon_api.jsonrpc.lib.result.collector.variants.collect_variants_v3',
                return_value=(
                    {self.partner.code: variants}, {self.partner.code: status}, None
                )
        ):
            with mock.patch('travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.internal_daemon_client'):
                with mock.patch('travel.avia.ticket_daemon_api.jsonrpc.query.Query.get_enabled_partner_codes', return_value=[self.partner.code]):
                    with mock.patch('travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.resultlib.collect_statuses',
                                    return_value={self.partner.code: {'status': status}}):
                        return get_client_app().get(url)

    def test_results(self):
        qid = '180209-094810-812.ticket.plane.c2_c213_2018-06-02_None_economy_1_0_0_ru.ru'
        r = self.send('jsendapi/v3/results/{}/0/0?service={}'.format(qid, 'yeah'))
        self.assertStatusOK(r)
        response = json.loads(r.data)['data']
        assert response['reference']['flights']
        assert response['reference']['settlements']
        assert all(s['code'] for s in response['reference']['settlements'])
        assert response['variants']
        assert response['cont'] is None

    def test_results_yak(self):
        qid = '180209-094810-812.ticket.plane.c2_c213_2018-06-02_None_economy_1_0_0_ru.ru'
        url = 'jsendapi/v3/results/{}/0/0?service={}'.format(qid, 'yeah')

        variants = ApiVariants(
            qid='QID',
            flights={},
            fares=[],
            status='done',
            query_time=1,
            revision=0,
        )
        with mock.patch(
                'travel.avia.ticket_daemon_api.jsonrpc.lib.result.collector.variants.collect_variants_v3',
                return_value=(
                    {self.partner.code: variants}, {self.partner.code: u'done'}, None
                )
        ):
            with mock.patch('travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.internal_daemon_client'):
                with mock.patch('travel.avia.ticket_daemon_api.jsonrpc.query.Query.get_enabled_partner_codes', return_value=[self.partner.code]):
                    with mock.patch('travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views.resultlib.collect_statuses',
                                    return_value={self.partner.code: {'status': u'done'}}):
                        with mock.patch('travel.avia.ticket_daemon_api.jsonrpc.lib.feature_flags.new_results_statuses', return_value=True):
                            response = get_client_app().get(url)
        assert response.status_code == 204
        assert not response.data

    def test_results_in_progress(self):
        qid = '180209-094810-812.ticket.plane.c2_c213_2018-06-02_None_economy_1_0_0_ru.ru'
        with mock.patch('travel.avia.ticket_daemon_api.jsonrpc.lib.result.continuation.default_cache.set'), \
             mock.patch('travel.avia.ticket_daemon_api.jsonrpc.lib.feature_flags.new_results_statuses', return_value=True):
            r = self.send('jsendapi/v3/results/{}/0/0?service={}'.format(qid, 'yeah'), status=u'querying')
        self.assertStatusOK(r, 206)
        response = json.loads(r.data)['data']
        assert response['reference']['flights']
        assert response['variants']
        assert response['cont'] == 1

    def test_reference_partners(self):
        qid = '210118-094810-812.ticket.plane.c2_c213_2018-06-02_None_economy_1_0_0_ru.ru'
        r = self.send('jsendapi/v3/results/{}/0/0?service={}'.format(qid, 'yeah'))
        self.assertStatusOK(r)
        response = json.loads(r.data)['data']
        assert response['reference']['partners'][0]['siteUrl'] == 'example-partner.com'


def pack(data):
    return zlib.compress(json.dumps(data))
