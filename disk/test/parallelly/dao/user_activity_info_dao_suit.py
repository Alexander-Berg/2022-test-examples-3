# coding: utf-8
import datetime
from datetime import date

import mock
import pytest
from nose_parameterized import parameterized

from mpfs.core.user_activity_info.utils import ErrorInfoContainer
from test.base import DiskTestCase
from test.conftest import INIT_USER_IN_POSTGRES
from mpfs.core.user_activity_info.dao import UserActivityInfoDAO, UserActivityDAOItem
from mpfs.metastorage.postgres.schema import PlatformType
from mpfs.metastorage.postgres.services import Sharpei


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='postgres test')
class UserActivityInfoDAOTestCase(DiskTestCase):

    dao = UserActivityInfoDAO()

    def _sorted_pg_dicts(self, pg_dicts):
        return sorted(pg_dicts, key=lambda d: (
            d.get('platform_type'), d.get('first_activity'), d.get('last_activity')
        ))

    def test_base(self):
        error_info_container = ErrorInfoContainer()
        list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': self.uid, 'platform_type': 'android', 'first_activity': '2019-01-01','last_activity': '2019-01-01'},
            {'uid': self.uid, 'platform_type': 'ios', 'first_activity': '2019-01-01', 'last_activity': '2019-01-01'},
        ], error_info_container,))

        assert not error_info_container.sharpei_errors_count
        assert not error_info_container.missing_uids_count
        activity_data = self.dao.find_by_uid(self.uid)
        assert activity_data
        raw_pg_activity_data = [dao_item.as_raw_pg_dict() for dao_item in activity_data]
        assert self._sorted_pg_dicts(raw_pg_activity_data) == self._sorted_pg_dicts([
            {
                'uid': int(self.uid),
                'platform_type': 'android',
                'first_activity': date(2019, 1, 1),
                'last_activity': date(2019, 1, 1)
            },
            {
                'uid': int(self.uid),
                'platform_type': 'ios',
                'first_activity': date(2019, 1, 1),
                'last_activity': date(2019, 1, 1)
            },
        ])

    def test_bulk_update_activity_dates_returns_missing_uids(self):
        missing_uid = '123'

        # добавляем в шарпей уид, которого заведомо нет в базе
        Sharpei().create_user(missing_uid)

        error_info_container = ErrorInfoContainer()
        list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': missing_uid, 'platform_type': 'web', 'first_activity': '2019-01-01', 'last_activity': '2019-01-01'},
            {'uid': missing_uid, 'platform_type': 'ios', 'first_activity': '2019-01-01', 'last_activity': '2019-01-01'},
            {'uid': self.uid, 'platform_type': 'ios', 'first_activity': '2019-01-01', 'last_activity': '2019-01-01'},
        ], error_info_container))

        assert not error_info_container.sharpei_errors_count
        assert error_info_container.missing_uids_count == 1
        activity_data = self.dao.find_by_uid(self.uid)
        raw_pg_activity_data = [dao_item.as_raw_pg_dict() for dao_item in activity_data]
        assert self._sorted_pg_dicts(raw_pg_activity_data) == self._sorted_pg_dicts([
            {
                'uid': int(self.uid),
                'platform_type': 'ios',
                'first_activity': date(2019, 1, 1),
                'last_activity': date(2019, 1, 1)
            },
        ])

    def test_bulk_update_activity_dates_counts_sharpei_errors(self):
        error_info_container = ErrorInfoContainer()
        with mock.patch('mpfs.metastorage.postgres.services.Sharpei.get_shard', side_effect=Exception):
            list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
                {'uid': self.uid, 'platform_type': 'ios', 'first_activity': '2019-01-01', 'last_activity': '2019-01-01'}
            ], error_info_container))

        assert error_info_container.sharpei_errors_count == 1
        assert not error_info_container.missing_uids_count
        activity_data = self.dao.find_by_uid(self.uid)
        assert not activity_data

    def test_correctly_bulk_update_activity_dates(self):
        error_info_container = ErrorInfoContainer()
        list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': self.uid, 'platform_type': 'android', 'first_activity': '2019-01-01','last_activity': '2019-01-01'},
            {'uid': self.uid, 'platform_type': 'ios', 'first_activity': '2019-01-01', 'last_activity': '2019-01-01'},
        ], error_info_container))

        assert not error_info_container.sharpei_errors_count
        assert not error_info_container.missing_uids_count
        activity_data = self.dao.find_by_uid(self.uid)
        assert activity_data
        raw_pg_activity_data = [dao_item.as_raw_pg_dict() for dao_item in activity_data]
        assert self._sorted_pg_dicts(raw_pg_activity_data) == self._sorted_pg_dicts([
            {
                'uid': int(self.uid),
                'platform_type': 'android',
                'first_activity': date(2019, 1, 1),
                'last_activity': date(2019, 1, 1)
            },
            {
                'uid': int(self.uid),
                'platform_type': 'ios',
                'first_activity': date(2019, 1, 1),
                'last_activity': date(2019, 1, 1)
            },
        ])

        error_info_container = ErrorInfoContainer()
        list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': self.uid, 'platform_type': 'android', 'first_activity': '2017-06-11', 'last_activity': '2018-11-01'},
            {'uid': self.uid, 'platform_type': 'ios', 'first_activity': '2019-07-01', 'last_activity': '2019-08-07'},
            {'uid': self.uid, 'platform_type': 'windows', 'first_activity': '2018-02-07', 'last_activity': '2018-03-05'},
        ], error_info_container))

        assert not error_info_container.sharpei_errors_count
        assert not error_info_container.missing_uids_count
        activity_data = self.dao.find_by_uid(self.uid)
        assert activity_data
        raw_pg_activity_data = [dao_item.as_raw_pg_dict() for dao_item in activity_data]
        assert self._sorted_pg_dicts(raw_pg_activity_data) == self._sorted_pg_dicts([
            {
                'uid': int(self.uid),
                'platform_type': 'android',
                'first_activity': date(2017, 6, 11),
                'last_activity': date(2019, 1, 1)
            },
            {
                'uid': int(self.uid),
                'platform_type': 'ios',
                'first_activity': date(2019, 1, 1),
                'last_activity': date(2019, 8, 7)
            },
            {
                'uid': int(self.uid),
                'platform_type': 'windows',
                'first_activity': date(2018, 2, 7),
                'last_activity': date(2018, 3, 5)
            },
        ])

    def test_base_update_activity_dates(self):
        dao_item = UserActivityDAOItem()
        dao_item.uid = self.uid
        dao_item.platform_type = PlatformType('ios')
        dao_item.first_activity = date(2017, 1, 1)
        dao_item.last_activity = date(2019, 1, 1)

        self.dao.update_activity_dates(dao_item)

        activity_data = self.dao.find_by_uid(self.uid)
        assert len(activity_data) == 1
        assert dao_item.as_raw_pg_dict() == activity_data[0].as_raw_pg_dict()

    def test_update_activity_dates_correctly_operates_with_ranges(self):
        initial_dao_item = UserActivityDAOItem()
        initial_dao_item.uid = self.uid
        initial_dao_item.platform_type = PlatformType('ios')
        initial_dao_item.first_activity = date(2017, 1, 1)
        initial_dao_item.last_activity = date(2019, 1, 1)

        self.dao.update_activity_dates(initial_dao_item)

        activity_data = self.dao.find_by_uid(self.uid)
        assert len(activity_data) == 1
        assert initial_dao_item.as_raw_pg_dict() == activity_data[0].as_raw_pg_dict()

        dao_item = UserActivityDAOItem()
        dao_item.uid = self.uid
        dao_item.platform_type = PlatformType('ios')
        dao_item.first_activity = date(2017, 2, 1)
        dao_item.last_activity = date(2018, 2, 1)

        self.dao.update_activity_dates(dao_item)

        activity_data = self.dao.find_by_uid(self.uid)
        assert len(activity_data) == 1
        assert initial_dao_item.as_raw_pg_dict() == activity_data[0].as_raw_pg_dict()  # dates doesnt changed

        dao_item = UserActivityDAOItem()
        dao_item.uid = self.uid
        dao_item.platform_type = PlatformType('ios')
        dao_item.first_activity = date(2017, 2, 1)
        dao_item.last_activity = date(2019, 12, 11)

        self.dao.update_activity_dates(dao_item)

        activity_data = self.dao.find_by_uid(self.uid)
        assert len(activity_data) == 1
        assert activity_data[0].as_raw_pg_dict() == {  # last_date changed
            'uid': int(self.uid),
            'platform_type': 'ios',
            'first_activity': date(2017, 1, 1),
            'last_activity': date(2019, 12, 11)
        }

        dao_item = UserActivityDAOItem()
        dao_item.uid = self.uid
        dao_item.platform_type = PlatformType('ios')
        dao_item.first_activity = date(2016, 2, 1)
        dao_item.last_activity = date(2018, 2, 11)

        self.dao.update_activity_dates(dao_item)

        activity_data = self.dao.find_by_uid(self.uid)
        assert len(activity_data) == 1
        assert activity_data[0].as_raw_pg_dict() == {  # first_date changed
            'uid': int(self.uid),
            'platform_type': 'ios',
            'first_activity': date(2016, 2, 1),
            'last_activity': date(2019, 12, 11)
        }

        dao_item = UserActivityDAOItem()
        dao_item.uid = self.uid
        dao_item.platform_type = PlatformType('ios')
        dao_item.first_activity = date(2014, 1, 1)
        dao_item.last_activity = date(2020, 1, 1)

        self.dao.update_activity_dates(dao_item)

        activity_data = self.dao.find_by_uid(self.uid)
        assert len(activity_data) == 1
        assert activity_data[0].as_raw_pg_dict() == {  # both dates changed
            'uid': int(self.uid),
            'platform_type': 'ios',
            'first_activity': date(2014, 1, 1),
            'last_activity': date(2020, 1, 1)
        }

    def test_update_activity_dates_and_no_users_to_give_discounts_to(self):
        list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': self.uid, 'platform_type': 'android', 'first_activity': '2019-01-02',
             'last_activity': '2019-01-02'},
            {'uid': self.uid, 'platform_type': 'ios', 'first_activity': '2019-01-01', 'last_activity': '2019-01-02'},
        ], ErrorInfoContainer(), ))

        uids = list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': self.uid, 'platform_type': 'android', 'first_activity': '2019-01-01',
             'last_activity': '2019-01-03'},
            {'uid': self.uid, 'platform_type': 'ios', 'first_activity': '2019-01-01', 'last_activity': '2019-04-01'},
        ], ErrorInfoContainer(), ))

        correct_answer = [{
            'uid': self.uid,
            'activity_after_update': datetime.date(2019, 1, 3),
            'activity_before_update': datetime.date(2019, 1, 2),
        }]
        assert uids == correct_answer

    def test_update_activity_dates_new_records_have_older_time_no_discount_given(self):
        list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': self.uid, 'platform_type': 'android', 'first_activity': '2019-01-02',
             'last_activity': '2020-01-02'},
            {'uid': self.uid, 'platform_type': 'ios', 'first_activity': '2019-01-01', 'last_activity': '2020-01-02'},
        ], ErrorInfoContainer(), ))

        uids = list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': self.uid, 'platform_type': 'android', 'first_activity': '2019-01-03',
             'last_activity': '2019-01-03'},
            {'uid': self.uid, 'platform_type': 'ios', 'first_activity': '2019-01-01', 'last_activity': '2019-03-01'},
        ], ErrorInfoContainer(), ))

        correct_answer = [{
            'uid': self.uid,
            'activity_after_update': datetime.date(2020, 1, 2),
            'activity_before_update': datetime.date(2020, 1, 2),
        }]
        assert uids == correct_answer

    @parameterized.expand([
        ('2019-04-01', ),
        ('2020-01-01', ),
    ])
    def test_update_activity_dates_and_have_users_with_old_activity_date(self, new_last_activity):
        self.create_user(self.user_1.uid)
        list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': self.uid, 'platform_type': 'ios', 'first_activity': '2019-01-01', 'last_activity': '2019-01-01'},
        ], ErrorInfoContainer(), ))

        uids = list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': self.uid, 'platform_type': 'ios', 'first_activity': '2019-01-01', 'last_activity': new_last_activity},
        ], ErrorInfoContainer(), ))

        correct_answer = [{
            'uid': self.uid,
            'activity_after_update': datetime.datetime.strptime(new_last_activity, '%Y-%m-%d').date(),
            'activity_before_update': datetime.date(2019, 1, 1),
        }]
        assert uids == correct_answer

    def test_update_activity_dates_and_have_users_with_old_activity_date_different_platforms(self):
        self.create_user(self.user_1.uid)
        list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': self.uid, 'platform_type': 'ios', 'first_activity': '2019-01-01', 'last_activity': '2019-01-01'},
        ], ErrorInfoContainer(), ))

        uids = list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': self.uid, 'platform_type': 'android', 'first_activity': '2019-01-01', 'last_activity': '2019-07-01'},
        ], ErrorInfoContainer(), ))

        correct_answer = [{
            'uid': self.uid,
            'activity_after_update': datetime.date(2019, 7, 1),
            'activity_before_update': datetime.date(2019, 1, 1),
        }]
        assert uids == correct_answer

    def test_update_activity_dates_and_have_users_with_no_activity_date(self):
        uids = list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': self.uid, 'platform_type': 'android', 'first_activity': '2019-01-01',
             'last_activity': '2019-01-03'},
            {'uid': self.uid, 'platform_type': 'ios', 'first_activity': '2019-01-01', 'last_activity': '2019-01-01'},
        ], ErrorInfoContainer(), ))

        correct_answer = [{
            'uid': self.uid,
            'activity_after_update': datetime.date(2019, 1, 1),
            'activity_before_update': None,
        }]
        assert uids == correct_answer

    def test_update_activity_dates_and_have_user_with_multiple_activities_and_one_old_enough(self):
        self.create_user(self.user_1.uid)
        list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': self.uid, 'platform_type': 'ios', 'first_activity': '2019-01-01', 'last_activity': '2019-01-02'},
            {'uid': self.user_1.uid, 'platform_type': 'ios', 'first_activity': '2019-01-01', 'last_activity': '2019-01-02'},
        ], ErrorInfoContainer(), ))

        uids = list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': self.uid, 'platform_type': 'ios', 'first_activity': '2019-01-01', 'last_activity': '2019-01-03'},
            {'uid': self.user_1.uid, 'platform_type': 'ios', 'first_activity': '2019-01-01', 'last_activity': '2019-04-02'},
        ], ErrorInfoContainer(), ))

        correct_answer = [
            {
                'uid': self.uid,
                'activity_after_update': datetime.date(2019, 1, 3),
                'activity_before_update': datetime.date(2019, 1, 2),
            },
            {
                'uid': self.user_1.uid,
                'activity_after_update': datetime.date(2019, 4, 2),
                'activity_before_update': datetime.date(2019, 1, 2),
            },
        ]
        assert sorted(uids, key=lambda x: x['uid']) == sorted(correct_answer, key=lambda x: x['uid'])
