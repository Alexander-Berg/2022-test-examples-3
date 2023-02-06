# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from mock import patch

from travel.rasp.bus.db.models.matching import PointType
from travel.rasp.bus.db.tests.factories import PointMatchingFactory, SupplierFactory, AdminUserFactory
from travel.rasp.bus.scripts.automatcher import AutoMatcher
from travel.rasp.bus.scripts.automatcher.scenarios.etraffic_twins_stations import EtrafficTwinStations

MARKED_POINT_KEY = 'marked_point_key'


def test_etraffic_twins_correct_matching(session):
    s1 = SupplierFactory(id=3, code='etraffic')
    s2 = SupplierFactory()

    user = AdminUserFactory(login='automatcher')

    PointMatchingFactory(supplier=s1, type=PointType.STATION, point_key=None, title='title1',
                         supplier_point_id='1', region='1', extra_info='extra1')  # will be matched, group 1
    PointMatchingFactory(supplier=s1, type=PointType.STATION, point_key=None, title='title1',
                         supplier_point_id='2', region='1', extra_info='extra1')  # will be matched, group 1
    PointMatchingFactory(supplier=s1, type=PointType.STATION, point_key=None, title='title1',
                         supplier_point_id='3', region='2', extra_info='extra1')  # will be matched, group 1
    PointMatchingFactory(supplier=s1, type=PointType.STATION, point_key=MARKED_POINT_KEY, title='title1',
                         supplier_point_id='4', region='1', extra_info='extra1')  # source point key for group 1
    PointMatchingFactory(supplier=s1, type=PointType.CITY, point_key=None, title='title1',
                         supplier_point_id='5', region='1', extra_info='extra1')  # will NOT, wrong type 'CITY'
    PointMatchingFactory(supplier=s1, type=PointType.STATION, point_key=None, title='title2',
                         supplier_point_id='6', region='1', extra_info='extra2')  # will NOT, 2 source pointkeys
    PointMatchingFactory(supplier=s1, type=PointType.STATION, point_key=MARKED_POINT_KEY + '1', title='title2',
                         supplier_point_id='7', region='1', extra_info='extra2')  # pointkey 1 in group 2
    PointMatchingFactory(supplier=s1, type=PointType.STATION, point_key=MARKED_POINT_KEY + '2', title='title2',
                         supplier_point_id='8', region='1', extra_info='extra2')  # pointkey 2 in group 2
    PointMatchingFactory(supplier=s1, type=PointType.STATION, point_key=None, title='title2',
                         supplier_point_id='9', region='1', extra_info='extra3')  # will NOT, no pointkey in group
    PointMatchingFactory(supplier=s1, type=PointType.STATION, point_key=None, title='title2',
                         supplier_point_id='10', region='1', extra_info='extra3')  # will NOT, no pointkey in group
    PointMatchingFactory(supplier=s1, type=PointType.STATION, point_key=None, title='title3',
                         supplier_point_id='11', region='2', extra_info='-')  # will NOT matched, black list, gr 3
    PointMatchingFactory(supplier=s1, type=PointType.STATION, point_key=None, title='title3',
                         supplier_point_id='12', region='2', extra_info='-')  # will NOT matched, black list, gr 3
    PointMatchingFactory(supplier=s1, type=PointType.STATION, point_key=MARKED_POINT_KEY, title='title3',
                         supplier_point_id='13', region='2', extra_info='-')  # source point key for group 3
    PointMatchingFactory(supplier=s1, type=PointType.STATION, point_key=None, title='title3',
                         supplier_point_id='14', region='2', extra_info=None)  # will NOT matched, extra is None, gr 4
    PointMatchingFactory(supplier=s1, type=PointType.STATION, point_key=None, title='title3',
                         supplier_point_id='15', region='2', extra_info=None)  # will NOT matched, extra is None, gr 4
    PointMatchingFactory(supplier=s1, type=PointType.STATION, point_key=MARKED_POINT_KEY, title='title3',
                         supplier_point_id='16', region='2', extra_info=None)  # source point key for group 4
    PointMatchingFactory(supplier=s2, type=PointType.STATION, point_key=None, title='title1',
                         supplier_point_id='1', region='1', extra_info='extra1')  # will NOT, another supplier
    PointMatchingFactory(supplier=s2, type=PointType.STATION, point_key=None, title='title1',
                         supplier_point_id='2', region='1', extra_info='extra1')  # will NOT, another supplier

    test_scenarios = [
        EtrafficTwinStations(),
    ]

    with patch.object(AutoMatcher, 'load_scenarios', return_value=test_scenarios):
        automatcher = AutoMatcher(['etraffic_twin_stations'], [], {}, user.login, dry_run=False)
        automatcher.run()

    entire_report = automatcher.get_stats()
    stats = entire_report.get('automatcher')
    scenario_stats = entire_report.get('etraffic_twin_stations')
    expected_matched1 = (s1.code, '1', 'etraffic_twin_stations', None, MARKED_POINT_KEY)
    expected_matched2 = (s1.code, '2', 'etraffic_twin_stations', None, MARKED_POINT_KEY)
    expected_matched3 = (s1.code, '3', 'etraffic_twin_stations', None, MARKED_POINT_KEY)
    expected_bad_group2_row1 = (', '.join(['extra2', 'title2']), 'title2', 'extra2', None, '6')
    expected_bad_group2_row2 = (', '.join(['extra2', 'title2']), 'title2', 'extra2', MARKED_POINT_KEY + '1', '7')
    expected_bad_group2_row3 = (', '.join(['extra2', 'title2']), 'title2', 'extra2', MARKED_POINT_KEY + '2', '8')
    assert automatcher.run_all is False
    assert len(stats) == 4
    assert expected_matched1 in stats
    assert expected_matched2 in stats
    assert expected_matched3 in stats
    assert expected_bad_group2_row1 in scenario_stats
    assert expected_bad_group2_row2 in scenario_stats
    assert expected_bad_group2_row3 in scenario_stats
