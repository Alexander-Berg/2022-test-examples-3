# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from mock import patch, Mock

from travel.rasp.bus.db.models.matching import PointType
from travel.rasp.bus.db.tests.factories import PointMatchingFactory, SupplierFactory, AdminUserFactory
from travel.rasp.bus.scripts.automatcher import AutoMatcher
from travel.rasp.bus.scripts.automatcher.scenarios.unitiki_update import UnitikiPointIdUpdate


def test_unitiki_update(session):
    s1 = SupplierFactory(id=6, code='unitiki-new')
    s2 = SupplierFactory()

    user = AdminUserFactory(login='automatcher')

    # existing base point
    PointMatchingFactory(supplier=s1, type=PointType.STATION, supplier_point_id='s1', point_key='s100')
    # should be matched: point to be updated from s1
    PointMatchingFactory(supplier=s1, type=PointType.STATION, supplier_point_id='s2', point_key=None)
    # should NOT be matched: wrong supplier
    PointMatchingFactory(supplier=s2, type=PointType.STATION, supplier_point_id='s2', point_key=None)
    # should NOT be matched: wrong point type
    PointMatchingFactory(supplier=s1, type=PointType.CITY, supplier_point_id='c2', point_key=None)
    # existing base point for transit test and 'after_id_not_found'
    PointMatchingFactory(supplier=s1, type=PointType.STATION, supplier_point_id='s3', point_key='s150')
    # existing base point for stat 'found_but_new_id_matched_too' testing, id_before
    PointMatchingFactory(supplier=s1, type=PointType.STATION, supplier_point_id='s4', point_key='s200')
    # existing base point for stat 'found_but_new_id_matched_too' testing, id_after
    PointMatchingFactory(supplier=s1, type=PointType.STATION, supplier_point_id='s5', point_key='s300')
    # should be matched: test transit matching 3 -> 10 -> 6
    PointMatchingFactory(supplier=s1, type=PointType.STATION, supplier_point_id='s6', point_key=None)
    # test 'found_but_not_matched' stat
    PointMatchingFactory(supplier=s1, type=PointType.STATION, supplier_point_id='s7', point_key=None)  # not found

    unitiki_merge_list = [
        {
            "action_id": 1,
            "datetime_merge": None,
            "station_id_before": 1,
            "station_id_after": 2,
        },
        {
            "action_id": 2,
            "datetime_merge": None,
            "station_id_before": 50,
            "station_id_after": 51,
        },
        {
            "action_id": 3,
            "datetime_merge": None,
            "station_id_before": 3,
            "station_id_after": 10,
        },
        {
            "action_id": 4,
            "datetime_merge": None,
            "station_id_before": 4,
            "station_id_after": 5,
        },
        {
            "action_id": 5,
            "datetime_merge": None,
            "station_id_before": 10,
            "station_id_after": 6,
        },
        {
            "action_id": 6,
            "datetime_merge": None,
            "station_id_before": 7,
            "station_id_after": 77,
        },
    ]

    mocked_s3 = Mock()
    mocked_s3.list_objects_v2 = Mock(return_value={'KeyCount': 0})
    mocked_s3.put_object = Mock(return_value={'ResponseMetadata': {'HTTPStatusCode': 200}})

    with patch.object(UnitikiPointIdUpdate, '_fetch_changes', return_value=unitiki_merge_list), \
            patch.object(UnitikiPointIdUpdate, '_connect_s3', return_value=mocked_s3):
        unitiki_scenario = UnitikiPointIdUpdate(common={'dry': False})
        test_scenarios = [
            unitiki_scenario,
        ]

        with patch.object(AutoMatcher, 'load_scenarios', return_value=test_scenarios):
            automatcher = AutoMatcher(['unitiki_update'], [], {}, user.login, dry_run=False)
            automatcher.run()

    entire_report = automatcher.get_stats()
    stats = entire_report.get('automatcher')
    scenario_stats = entire_report.get('unitiki_update')
    expected_matched1 = (s1.code, 's2', 'unitiki_update', None, 's100')
    expected_matched2 = (s1.code, 's6', 'unitiki_update', None, 's150')
    scenario_expected_report = [
        ('not_found', 1),
        ('found_but_not_matched', 1),
        ('after_id_not_found', 1),
        ('after_id_not_found', 1),
        ('found_and_saved', 2),
    ]

    assert automatcher.run_all is False
    assert len(stats) == 3
    assert expected_matched1 in stats
    assert expected_matched2 in stats
    assert unitiki_scenario.last_action_id == '6'
    assert all(row in scenario_stats for row in scenario_expected_report)
    unitiki_scenario.s3.put_object.assert_called_once_with(
        Body=b'6',
        Bucket=unitiki_scenario.S3_BUCKET,
        Key=unitiki_scenario.S3_LAST_ACTION_FN
    )
    unitiki_scenario.s3.list_objects_v2.assert_called_with(
        Bucket=unitiki_scenario.S3_BUCKET,
        MaxKeys=10
    )
