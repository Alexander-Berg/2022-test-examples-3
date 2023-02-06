# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from mock import patch

from travel.rasp.bus.db.models.matching import PointType, PointMatching
from travel.rasp.bus.db.tests.factories import PointMatchingFactory, SupplierFactory, AdminUserFactory
from travel.rasp.bus.scripts.automatcher import AutoMatcher
from travel.rasp.bus.scripts.automatcher.policy import TypePolicy
from travel.rasp.bus.scripts.automatcher.scenarios.base import BaseMatcher, BaseUnmatcher


MARKED_POINT_KEY = 'marked_point_key'
MARKED_SUPPLIER_POINT_ID = 'marked_point_id'
MATCHING_RESULT = 'automatched'


class DummyScenario(BaseMatcher):
    name = 'DummyTest'
    point_type_policy = TypePolicy.TYPE_STATION

    def _run(self, point):
        if point.supplier_point_id == MARKED_SUPPLIER_POINT_ID:
            return True, MATCHING_RESULT
        return False, None


class DummySupplierScenario(BaseMatcher):
    name = 'DummySupplierTest'
    point_type_policy = TypePolicy.TYPE_STATION
    supplier = 'etraffic'

    def _run(self, point):
        if point.supplier_point_id == MARKED_SUPPLIER_POINT_ID:
            return True, MATCHING_RESULT
        return False, None


class DummyCityScenario(BaseMatcher):
    name = 'DummyCityTest'
    point_type_policy = TypePolicy.TYPE_CITY

    def _run(self, point):
        if point.supplier_point_id == MARKED_SUPPLIER_POINT_ID:
            return True, MATCHING_RESULT
        return False, None


class DummyUnmatcher(BaseUnmatcher):
    name = 'DummyUnmatcherTest'
    point_type_policy = TypePolicy.TYPE_ALL

    def _run(self, point):
        if point.point_key == MARKED_POINT_KEY and point.supplier_point_id == MARKED_SUPPLIER_POINT_ID:
            return True, None
        return False, None


def test_supplier_policy(session):
    s1 = SupplierFactory(code='etraffic')
    s2 = SupplierFactory()
    s3 = SupplierFactory()

    user = AdminUserFactory(login='automatcher')

    test_scenarios = [
        DummySupplierScenario(),
    ]

    PointMatchingFactory(supplier=s1, type=PointType.STATION, point_key=None,
                         supplier_point_id=MARKED_SUPPLIER_POINT_ID)
    PointMatchingFactory(supplier=s2, type=PointType.STATION, point_key=None,
                         supplier_point_id=MARKED_SUPPLIER_POINT_ID)
    PointMatchingFactory(supplier=s3, type=PointType.STATION, point_key=None,
                         supplier_point_id=MARKED_SUPPLIER_POINT_ID)

    with patch.object(AutoMatcher, 'load_scenarios', return_value=test_scenarios):
        automatcher = AutoMatcher(['dummy_supplier'], [], {},  user.login)
        automatcher.run()

    stats = automatcher.get_stats().get('automatcher')
    expected_matcher = (s1.code, MARKED_SUPPLIER_POINT_ID, 'DummySupplierTest', None, MATCHING_RESULT)
    assert automatcher.run_all is False
    assert len(stats) == 2
    assert expected_matcher in stats


def test_point_type_policy(session):
    test_scenarios = [
        DummyCityScenario(),
    ]
    s1 = SupplierFactory()
    s2 = SupplierFactory()

    user = AdminUserFactory(login='automatcher')

    PointMatchingFactory(supplier=s1, type=PointType.STATION, point_key=None,
                         supplier_point_id=MARKED_SUPPLIER_POINT_ID)
    PointMatchingFactory(supplier=s2, type=PointType.CITY, point_key=None,
                         supplier_point_id=MARKED_SUPPLIER_POINT_ID)

    with patch.object(AutoMatcher, 'load_scenarios', return_value=test_scenarios):
        automatcher = AutoMatcher(['dummy_city'], [], {}, user.login, dry_run=False)
        automatcher.run()

    stats = automatcher.get_stats().get('automatcher')
    expected_matcher = (s2.code, MARKED_SUPPLIER_POINT_ID, 'DummyCityTest', None, MATCHING_RESULT)

    point_matching = session.query(PointMatching).\
        filter(PointMatching.supplier_point_id == MARKED_SUPPLIER_POINT_ID, PointMatching.supplier_id == s2.id).one()

    assert point_matching.point_key == MATCHING_RESULT
    assert automatcher.dry_run is False
    assert automatcher.run_all is False
    assert len(stats) == 2
    assert expected_matcher in stats


def test_matching_policy(session):
    test_scenarios = [
        DummyScenario(),
        DummyUnmatcher(),
    ]
    s1 = SupplierFactory()
    s2 = SupplierFactory()

    user = AdminUserFactory(login='automatcher')

    PointMatchingFactory(supplier=s1, type=PointType.STATION, point_key=None,
                         supplier_point_id=MARKED_SUPPLIER_POINT_ID)
    PointMatchingFactory(supplier=s1, type=PointType.STATION, point_key=None,
                         supplier_point_id=MARKED_SUPPLIER_POINT_ID + '1')
    PointMatchingFactory(supplier=s2, type=PointType.STATION, point_key=MARKED_POINT_KEY,
                         supplier_point_id=MARKED_SUPPLIER_POINT_ID + '1')
    PointMatchingFactory(supplier=s2, type=PointType.CITY, point_key=MARKED_POINT_KEY,
                         supplier_point_id=MARKED_SUPPLIER_POINT_ID)

    with patch.object(AutoMatcher, 'load_scenarios', return_value=test_scenarios):
        automatcher = AutoMatcher(['dummy', 'dummy_unmatcher'], [], {}, user.login)
        automatcher.run()

    stats = automatcher.get_stats().get('automatcher')
    expected_unmatcher = (s2.code, MARKED_SUPPLIER_POINT_ID, 'DummyUnmatcherTest', MARKED_POINT_KEY, None)
    expected_matcher = (s1.code, MARKED_SUPPLIER_POINT_ID, 'DummyTest', None, MATCHING_RESULT)
    assert automatcher.run_all is False
    assert len(stats) == 3
    assert expected_unmatcher in stats
    assert expected_matcher in stats


def test_try_all(session):
    test_scenarios = [
        DummyUnmatcher(),
        DummyScenario(),
    ]
    s1 = SupplierFactory()
    s2 = SupplierFactory()
    s3 = SupplierFactory()

    user = AdminUserFactory(login='automatcher')

    PointMatchingFactory(supplier=s1, type=PointType.STATION, point_key=None,
                         supplier_point_id=MARKED_SUPPLIER_POINT_ID)
    PointMatchingFactory(supplier=s1, type=PointType.STATION, point_key=None,
                         supplier_point_id=MARKED_SUPPLIER_POINT_ID + '1')
    PointMatchingFactory(supplier=s2, type=PointType.STATION, point_key=MARKED_POINT_KEY,
                         supplier_point_id=MARKED_SUPPLIER_POINT_ID)
    PointMatchingFactory(supplier=s3, type=PointType.CITY, point_key=MARKED_POINT_KEY,
                         supplier_point_id=MARKED_SUPPLIER_POINT_ID)

    with patch.object(AutoMatcher, 'load_scenarios', return_value=test_scenarios):
        automatcher = AutoMatcher(['dummy', 'dummy_unmatcher'], [], {}, user.login, dry_run=False, run_all=True)
        automatcher.run()

    stats = automatcher.get_stats().get('automatcher')
    expected_unmatcher1 = (s2.code, MARKED_SUPPLIER_POINT_ID, 'DummyUnmatcherTest', MARKED_POINT_KEY, None)
    expected_unmatcher2 = (s3.code, MARKED_SUPPLIER_POINT_ID, 'DummyUnmatcherTest', MARKED_POINT_KEY, None)
    expected_matcher1 = (s1.code, MARKED_SUPPLIER_POINT_ID, 'DummyTest', None, MATCHING_RESULT)
    expected_matcher2 = (s2.code, MARKED_SUPPLIER_POINT_ID, 'DummyTest', MARKED_POINT_KEY, MATCHING_RESULT)
    assert automatcher.dry_run is True
    assert len(stats) == 5
    assert expected_unmatcher1 in stats
    assert expected_unmatcher2 in stats
    assert expected_matcher1 in stats
    assert expected_matcher2 in stats
