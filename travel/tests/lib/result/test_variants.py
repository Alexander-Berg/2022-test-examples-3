# -*- coding: utf-8 -*-
from datetime import datetime, timedelta
from itertools import product

import mock
from django.conf import settings
from pytz import UTC

from travel.avia.ticket_daemon_api.tests.daemon_tester import create_query
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views import _unprocessed_variant_match_route
from travel.avia.ticket_daemon_api.jsonrpc.lib.date import DateTimeDeserializer, get_utc_now, get_msk_now, naive_to_timestamp, aware_to_timestamp
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon_api.jsonrpc.lib.result import Statuses
from travel.avia.ticket_daemon_api.jsonrpc.lib.result.collector.variants_fabric import VariantsFabric, DeparturedVariantsFilter, SubQueryFilter


DATE_FORMAT = DateTimeDeserializer.FORMAT_DATE
NOW = datetime(2017, 1, 1, 0, 10, 0, tzinfo=UTC)
STORE_TIME = 60*20
date_time_deserializer = DateTimeDeserializer()


class ApiVariantsTestCase(TestCase):
    CREATED = datetime(2017, 1, 1, 0, 0, 0, tzinfo=UTC)

    def setUp(self):
        reset_all_caches()

    @staticmethod
    def assert_variant_is_equal(qid, variants_count, status, revision, actual):
        assert qid == actual.qid
        assert variants_count == len(actual.variants)
        assert status == actual.status
        assert revision == actual.revision

    def result(self, fares=None, flights=None, created=CREATED, qid=None):
        if fares is None:
            fares = {}
        if flights is None:
            flights = {}
        return {
            u'qid': qid,
            u'all_variants_count': len(fares),
            u'expire': aware_to_timestamp(created) + STORE_TIME,
            u'created': aware_to_timestamp(created),
            u'query_time': 1,
            u'fares': fares,
            u'flights': flights,
        }


def get_query(**kwargs):
    query_defaults = {'date_forward': (NOW + timedelta(days=7)).date()}
    query_defaults.update(kwargs)
    query = create_query(when=str(query_defaults['date_forward']))
    query.id = 'test_qid'
    return query


class TestVariantsCollectorLogic(ApiVariantsTestCase):
    def revision_result(self, variant_revisions, **kwargs):
        fares = {
            tag: {
                u'charter': None, u'baggage': [[], []],
                u'tariff': {u'currency': u'RUR', u'value': 1000},
                u'route': [[], []],
                u'fare_codes': [[], []],
                u'created': naive_to_timestamp(revision),
                u'expire': naive_to_timestamp(
                    revision + timedelta(seconds=STORE_TIME)
                ),
            } for tag, revision in variant_revisions.iteritems()
        }
        return self.result(fares=fares, **kwargs)

    def test_outdated_empty_first_run(self):
        status = Statuses.QUERYING

        actual = VariantsFabric.create(
            self.revision_result({}, qid='qid1'), status=status, query=create_query(), partner_code='test-partner', last_revision=0
        )
        ApiVariantsTestCase.assert_variant_is_equal(
            'qid1', 0, Statuses.OUTDATED, aware_to_timestamp(self.CREATED), actual
        )

    def test_outdated_empty_two_runs(self):
        """
        Статус querying, пока не получили свежих вариантов от партнера, при первом
        обращении отдаем старые варианты, при втором возвращаем, что идет опрос партнера
        :return:
        """
        status = Statuses.QUERYING

        query = get_query()
        # при первом вызове last_revision=0
        actual1 = VariantsFabric.create(
            self.revision_result({}, qid='qid2'),
            status=status,
            query=query,
            partner_code='test-partner',
            last_revision=0,
        )
        self.assert_variant_is_equal(
            'qid2', 0, Statuses.OUTDATED, aware_to_timestamp(self.CREATED), actual1
        )
        # при повторном вызове берем last_revision из continuation
        actual2 = VariantsFabric.create(
            self.revision_result({}, qid='qid2'),
            status=status, query=query,
            partner_code='test-partner',
            last_revision=actual1.revision,
        )
        self.assert_variant_is_equal(
            'qid2', 0, Statuses.QUERYING, aware_to_timestamp(self.CREATED), actual2
        )

    def test_filter_by_revision_on_querying(self):
        status = Statuses.QUERYING
        checked_revision = get_utc_now().replace(microsecond=0)
        outdated_revision = checked_revision.replace(tzinfo=None) - timedelta(
            minutes=30)
        actual_revision = checked_revision.replace(tzinfo=None) + timedelta(
            minutes=1)

        variant = self.revision_result(
            {1: outdated_revision, 2: actual_revision},
            created=checked_revision,
            qid='qid3',
        )
        actual = VariantsFabric.create(
            variant, status=status, query=get_query(), partner_code='test-partner', last_revision=0
        )
        self.assert_variant_is_equal('qid3', 1, Statuses.QUERYING, naive_to_timestamp(actual_revision), actual)
        assert naive_to_timestamp(actual_revision) == actual.variants[0]['created']

    def test_filter_by_revision_when_done(self):
        status = Statuses.DONE
        checked_revision = get_utc_now().replace(microsecond=0)
        outdated_revision = checked_revision.replace(tzinfo=None) - timedelta(minutes=30)
        actual_revision = checked_revision.replace(tzinfo=None) + timedelta(minutes=1)

        variant = self.revision_result(
            {1: outdated_revision, 2: actual_revision},
            created=checked_revision,
            qid='qid4'
        )
        actual = VariantsFabric.create(
            variant, status=status, query=get_query(), partner_code='test-partner', last_revision=0
        )
        self.assert_variant_is_equal('qid4', 1, Statuses.DONE, naive_to_timestamp(actual_revision), actual)
        assert naive_to_timestamp(actual_revision) == actual.variants[0]['created']

    def test_outdated_full(self):
        status = Statuses.QUERYING
        created_in_past = get_utc_now().replace(microsecond=0) - timedelta(minutes=30)
        checked_revision = created_in_past.replace(tzinfo=None)

        variant = self.revision_result(
            {1: checked_revision, 2: checked_revision},
            created=created_in_past,
            qid='qid5'
        )
        query = get_query()
        actual1 = VariantsFabric.create(
            variant, status=status, query=query, partner_code='test-partner', last_revision=0
        )
        self.assert_variant_is_equal('qid5', 2, Statuses.OUTDATED, naive_to_timestamp(checked_revision), actual1)

        actual2 = VariantsFabric.create(
            variant, status=status, query=query, partner_code='test-partner', last_revision=actual1.revision
        )
        self.assert_variant_is_equal('qid5', 0, Statuses.QUERYING, naive_to_timestamp(checked_revision), actual2)

    def test_filter_old_revisions(self):
        """
        Удаляем варианты, которые сохранены не в диапазоне PARTNER_QUERY_TIMEOUT от
        последнего
        """
        status = Statuses.QUERYING
        checked_revision = get_utc_now().replace(microsecond=0)
        very_outdated_revision = (
            checked_revision.replace(tzinfo=None) -
            timedelta(seconds=(settings.PARTNER_QUERY_TIMEOUT + 1))
        )
        actual_outdated_revision = checked_revision.replace(tzinfo=None)

        variant = self.revision_result(
            {1: very_outdated_revision, 2: actual_outdated_revision},
            created=checked_revision,
            qid='qid6'
        )
        actual = VariantsFabric.create(
            variant, status=status, query=get_query(), partner_code='test-partner', last_revision=0
        )
        self.assert_variant_is_equal('qid6', 1, Statuses.QUERYING, naive_to_timestamp(actual_outdated_revision), actual)

    def test_not_filter_old_revisions_with_mode_all(self):
        """
        Не удаляем варианты, которые сохранены не в диапазоне PARTNER_QUERY_TIMEOUT от
        последнего, когда запрашиваем все варианты
        """
        status = Statuses.QUERYING
        checked_revision = get_utc_now().replace(microsecond=0)
        very_outdated_revision = (
            checked_revision.replace(tzinfo=None) -
            timedelta(seconds=(settings.PARTNER_QUERY_TIMEOUT + 1))
        )
        actual_outdated_revision = checked_revision.replace(tzinfo=None)

        variant = self.revision_result(
            {1: very_outdated_revision, 2: actual_outdated_revision},
            created=checked_revision,
            qid='qid7'
        )
        actual = VariantsFabric.create(
            variant, status=status, query=get_query(), partner_code='test-partner', last_revision=0, mode='all'
        )
        self.assert_variant_is_equal('qid7', 2, Statuses.QUERYING, naive_to_timestamp(actual_outdated_revision), actual)

    def test_not_filter_old_revisions(self):
        """
        Не удаляем варианты, которые сохранены в диапазоне PARTNER_QUERY_TIMEOUT от
        последнего
        """
        status = Statuses.QUERYING
        checked_revision = get_utc_now().replace(microsecond=0)

        not_very_outdated_revision = (
            checked_revision.replace(tzinfo=None) -
            timedelta(seconds=(settings.PARTNER_QUERY_TIMEOUT - 1))
        )
        actual_outdated_revision = checked_revision.replace(tzinfo=None)

        variant = self.revision_result(
            {1: not_very_outdated_revision, 2: actual_outdated_revision},
            created=checked_revision,
            qid='qid8'
        )
        actual = VariantsFabric.create(
            variant, status=status, query=get_query(), partner_code='test-partner', last_revision=0
        )
        self.assert_variant_is_equal('qid8', 2, Statuses.QUERYING, naive_to_timestamp(actual_outdated_revision), actual)

    def test_get_actual(self):
        """
        При InstantSearch хранятся и варианты из предыдущих ответов,
        и актуальные из последнего ответа, возвращаем только актуальные
        """
        status = Statuses.DONE
        checked_revision = get_utc_now().replace(microsecond=0)

        very_outdated_revision = (
            checked_revision.replace(tzinfo=None) -
            timedelta(seconds=(settings.PARTNER_QUERY_TIMEOUT + 1))
        )
        actual_revision = checked_revision.replace(tzinfo=None)

        variant = self.revision_result(
            {1: very_outdated_revision, 2: actual_revision},
            created=checked_revision,
            qid='qid9'
        )
        actual = VariantsFabric.create(
            variant, status=status, query=get_query(), partner_code='test-partner', last_revision=0, mode='actual'
        )
        self.assert_variant_is_equal(
            'qid9', 1, Statuses.DONE, naive_to_timestamp(actual_revision), actual
        )

    def test_get_actual_in_outdated(self):
        """
        При запросе актуальных - возвращаем пустой результат, если актуальных нет

        """
        status = Statuses.DONE
        checked_revision = get_utc_now().replace(microsecond=0)

        outdated_revision = (
            checked_revision.replace(tzinfo=None) -
            timedelta(seconds=(settings.PARTNER_QUERY_TIMEOUT + 1))
        )

        variant = self.revision_result(
            {1: outdated_revision, 2: outdated_revision},
            created=checked_revision,
            qid='qid10'
        )
        actual = VariantsFabric.create(
            variant, status=status, query=get_query(), partner_code='test-partner', last_revision=0, mode='actual'
        )
        self.assert_variant_is_equal(
            'qid10', 0, Statuses.DONE, aware_to_timestamp(checked_revision), actual
        )

    def test_filter_by_age(self):
        """
        Если указан параметр max_age, возвращаем варианты не старше %max_age% часов.
        :return:
        """

        status = Statuses.DONE
        actual_revision = get_utc_now().replace(microsecond=0)
        too_old_revision = (get_utc_now() - timedelta(hours=20)).replace(microsecond=0)

        variant = self.revision_result(
            {1: actual_revision.replace(tzinfo=None), 2: too_old_revision.replace(tzinfo=None)},
            created=too_old_revision
        )

        variants = VariantsFabric.create(
            variant, status, get_query(), partner_code='test-partner', mode='all', max_age=15
        )

        assert len(variants) == 1

    @mock.patch(
        'travel.avia.ticket_daemon_api.jsonrpc.lib.feature_flags.replace_search_to_station_with_search_to_city', return_value=True,
    )
    def test_filter_subquery_stations(self, *mocks):
        """Фильтруем варианты не до станций поиска, при поисках до станций после обобщения до поиска в город"""
        query = create_query(from_is_settlement=False, to_is_settlement=False)

        fares = [
            {u'route': [[fwd_key], [bwd_key]]}
            for fwd_key, bwd_key in product(
                [u'fwd_query_airport', u'fwd_not_query_airport'], [u'bwd_query_airport', u'bwd_not_query_airport']
            )
        ]
        flights = {
            u'fwd_query_airport': {
                'from': query.point_from.id,
                'to': query.point_to.id,
            },
            u'fwd_not_query_airport': {
                'from': query.point_from.id + 1,
                'to': query.point_to.id + 1,
            },
            u'bwd_query_airport': {
                'from': query.point_to.id,
                'to': query.point_from.id,
            },
            u'bwd_not_query_airport': {
                'from': query.point_to.id + 1,
                'to': query.point_from.id + 1,
            },
        }
        actual = list(SubQueryFilter.filter(fares, flights, query.queries[0]))
        assert len(actual) == 1


class TestApiVariants(ApiVariantsTestCase):
    TZ_INFO = 'Europe/Moscow'

    def setUp(self):
        super(TestApiVariants, self).setUp()
        self.msk_now = get_msk_now()
        self.status = Statuses.QUERYING
        self.variant = self.result(flights=self._flights(), fares=self._variants())

    def test_get_actual_routes(self):
        before_departure_minutes = 45
        actual_routes = DeparturedVariantsFilter(before_departure_minutes)._get_actual_routes(self._flights())
        self.assertItemsEqual(['far_route'], actual_routes)

    def test_filter_departured_variants(self):
        """Удаляем улетевшие самолеты"""
        query = get_query(date_forward=self.msk_now.date())

        actual = VariantsFabric.create(
            self.variant, status=self.status, query=query, partner_code='test-partner', last_revision=0
        )
        assert 1 == len(actual.variants)
        assert 'far_variant' == actual.variants[0]['tag']
        assert 1 == len(actual.flights)
        assert 'far_route' in actual.flights

    def test_filter_departured_variants__wrong_tz(self):
        query = get_query(date_forward=self.msk_now.date())
        query.point_from = query.point_from._replace(time_zone='fake-tz')

        actual = VariantsFabric.create(
            self.variant, status=self.status, query=query, partner_code='test-partner', last_revision=0
        )
        assert 1 == len(actual.variants)
        assert 'far_variant' == actual.variants[0]['tag']

    def test_filter_departured_variants__wrong_tz__but_far_query(self):
        query = get_query(date_forward=(self.msk_now + timedelta(days=2)).date())
        query.point_from = query.point_from._replace(time_zone='fake-tz')

        actual = VariantsFabric.create(
            self.variant, status=self.status, query=query, partner_code='test-partner', last_revision=0
        )
        assert 2 == len(actual.variants)
        assert {'far_variant', 'near_variant'} == {v['tag'] for v in actual.variants}

    def _flights(self):
        return {
            'far_route': self._flight(timedelta(hours=6)),
            'near_route': self._flight(timedelta(minutes=10)),
        }

    def _flight(self, departure_local):
        """
        :param departure_local: смещение времени вылета относительно msk_now
        """
        return {
            'departure': {
                'local': (self.msk_now + departure_local).isoformat()[:19],
                'tzname': 'Europe/Moscow',
                'offset': 180.0,
            },
        }

    def _variants(self):
        return {
            'far_variant': self._create_variant('far_route'),
            'near_variant': self._create_variant('near_route'),
        }

    @staticmethod
    def _create_variant(route):
        return {
            'expire': aware_to_timestamp(NOW),
            'created': aware_to_timestamp(NOW),
            'route': [[route], []],
            'fare_codes': [[None], []],
        }


def test_unprocessed_variant_match_route():
    params = {
        'forward': [{
            'departure_datetime': datetime(2019, 1, 2, 3, 15),
            'number': 'AA 99',
            # 'arrival_datetime': datetime(2019, 1, 2, 5, 40),
        }, {
            'departure_datetime': datetime(2019, 1, 2, 8, 35),
            'number': 'AA 101',
            # 'arrival_datetime': datetime(2019, 1, 2, 9, 55),
        }],
        'backward': [{
            'departure_datetime': datetime(2019, 1, 4, 3, 20),
            'number': 'AA 100',
            # 'arrival_datetime': datetime(2019, 1, 4, 6, 40),
        }],
    }
    prefilter = _unprocessed_variant_match_route(params)
    variant = {
        'route': [
            ['1901020315AA991901020540', '1901020835AA1011901020955'],
            ['1901040320AA1001901040640'],
        ]
    }
    assert prefilter(variant)
