# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import cPickle
from datetime import date

import mock
import pytest
import yt.wrapper as yt
from hamcrest import assert_that, contains_inanyorder

from common.models.geo import Station2Settlement
from common.models.transport import TransportType
from precalc.prices import (
    SEGMENT_GROUP_KEYS, dump_prices, get_settlement_ids, get_yt_table_path, iter_train_places, iter_direction_keys,
    iter_point_keys, iter_price_rows, open_yt_min_prices, save_prices
)
from precalc.utils.db import serialize_raw_data
from tester.factories import create_station, create_settlement
from tester.utils.datetime import replace_now
from tester.utils.replace_setting import replace_setting


@pytest.yield_fixture
def get_settlement_ids_cache_clear():
    get_settlement_ids.cache_clear()
    try:
        yield
    finally:
        get_settlement_ids.cache_clear()


@pytest.yield_fixture
def yt_mock():
    with mock.patch.object(yt, 'update_config', autospec=True), \
            mock.patch.object(yt, 'exists', autospec=True, return_value=True), \
            mock.patch.object(yt, 'TempTable',
                              **{'return_value.__enter__.return_value': mock.sentinel.tmp_table}), \
            mock.patch.object(yt, 'run_sort', autospec=True), \
            mock.patch.object(yt, 'read_table', autospec=True, return_value=mock.sentinel.yt_min_prices):
        yield yt


@pytest.mark.dbuser
def test_get_settlement_ids_no_station(get_settlement_ids_cache_clear):
    assert not get_settlement_ids(0)


@pytest.mark.dbuser
def test_get_settlement_ids_no_settlements(get_settlement_ids_cache_clear):
    station = create_station(settlement=None)

    assert not get_settlement_ids(station.id)


@pytest.mark.dbuser
def test_get_settlement_ids_settlement(get_settlement_ids_cache_clear):
    station = create_station(settlement={})
    settlement = create_settlement()
    Station2Settlement.objects.create(station=station, settlement=settlement)
    Station2Settlement.objects.create(station=station, settlement=station.settlement)

    assert get_settlement_ids(station.id) == {station.settlement_id, settlement.id}


@pytest.mark.dbuser
@pytest.mark.parametrize('object_type, expected', (
    ('Country', []),
    ('Settlement', ['c1']),
    ('SettlementTuple', ['c1']),
    ('Station', ['s1']),
    ('StationTuple', ['s1']),
))
def test_iter_point_keys(object_type, expected):
    assert list(iter_point_keys(object_type, 1)) == expected


@pytest.mark.dbuser
def test_iter_point_keys_station_extending():
    station = create_station(settlement={})
    settlement = create_settlement()
    Station2Settlement.objects.create(station=station, settlement=settlement)

    assert_that(list(iter_point_keys('Station', station.id)),
                contains_inanyorder(station.point_key, station.settlement.point_key, settlement.point_key))


@pytest.mark.dbuser
def test_iter_direction_keys():
    station_1 = create_station(settlement={})
    station_2 = create_station(settlement={})
    assert_that(list(iter_direction_keys('Station', station_1.id, 'Station', station_2.id)),
                contains_inanyorder('{}-{}'.format(station_1.point_key, station_2.point_key),
                                    '{}-{}'.format(station_1.point_key, station_2.settlement.point_key),
                                    '{}-{}'.format(station_1.settlement.point_key, station_2.point_key),
                                    '{}-{}'.format(station_1.settlement.point_key, station_2.settlement.point_key)))


DUMMY_YT_RECORD = {'timestamp': '2000-01-01 12:00:00',
                   'type': 'bus', 'class': '', 'route_uid': '', 'date_forward': '2000-06-01', 'price': 123,
                   'object_from_type': 'Settlement', 'object_from_id': 1,
                   'object_to_type': 'Settlement', 'object_to_id': 2}


def make_yt_record(record_data):
    return dict(DUMMY_YT_RECORD, **record_data)


def test_iter_train_places():
    assert tuple(iter_train_places((
        make_yt_record({'type': 'train', 'class': 'compartment', 'seats': 10, 'price': 1234}),
        make_yt_record({'type': 'train', 'class': 'invalid', 'seats': 5, 'price': 1234}),
        make_yt_record({'type': 'train', 'class': 'platzkarte', 'seats': 20, 'price': 123}),
    ))) == (('compartment', 10, 1234.), ('platzkarte', 20, 123.))


@pytest.mark.dbuser
def test_iter_price_rows():
    assert tuple(iter_price_rows((
        make_yt_record({'type': 'bus', 'date_forward': '2000-06-02', 'price': 123,
                        'object_from_type': 'Settlement', 'object_from_id': 1,
                        'object_to_type': 'Station', 'object_to_id': 2}),
        make_yt_record({'type': 'plane', 'date_forward': '2000-06-03', 'price': 4567, 'route_uid': 'AB-123',
                        'object_from_type': 'Station', 'object_from_id': 1,
                        'object_to_type': 'Settlement', 'object_to_id': 2}),
    ))) == (
        ('c1-s2', date(2000, 6, 2).toordinal(), TransportType.BUS_ID, '', 123, None),
        ('s1-c2', date(2000, 6, 3).toordinal(), TransportType.PLANE_ID, 'AB-123', 4567, None),
    )


@pytest.mark.dbuser
def test_iter_price_rows_min_price_of_segment():
    assert tuple(iter_price_rows((
        make_yt_record({'price': 456}),
        make_yt_record({'price': 123})
    ))) == (
        ('c1-c2', date(2000, 6, 1).toordinal(), TransportType.BUS_ID, '', 123, None),
    )


@pytest.mark.dbuser
def test_iter_price_rows_recent_record():
    assert tuple(iter_price_rows((
        make_yt_record({'price': 123, 'timestamp': '2000-01-01 12:00:00'}),
        make_yt_record({'price': 456, 'timestamp': '2000-01-02 12:00:00'})
    ))) == (
        ('c1-c2', date(2000, 6, 1).toordinal(), TransportType.BUS_ID, '', 456, None),
    )


@pytest.mark.dbuser
def test_iter_price_rows_train_places():
    price_rows = tuple(iter_price_rows((
        make_yt_record({'type': 'train', 'class': 'compartment', 'seats': 10, 'price': 1234}),
    )))

    assert len(price_rows) == 1
    assert cPickle.loads(str(price_rows[0][-1])) == {'train_places': (('compartment', 10, 1234.),)}


@pytest.mark.dbuser
def test_iter_price_rows_skip_ignored_classes():
    assert tuple(iter_price_rows((
        make_yt_record({'class': 'first', 'type': 'plane', 'price': 12345678}),
        make_yt_record({'class': 'business', 'type': 'plane', 'price': 123456}),
        make_yt_record({}),
    ))) == (
        ('c1-c2', date(2000, 6, 1).toordinal(), TransportType.BUS_ID, '', 123, None),
    )


@pytest.mark.dbuser
def test_iter_price_rows_skip_errors():
    assert tuple(iter_price_rows((
        make_yt_record({'type': 'invalid'}),
        make_yt_record({'date_forward': 'invalid'}),
        make_yt_record({'price': 'invalid'}),
        make_yt_record({'type': 'train', 'class': 'compartment'}),
        make_yt_record({'type': 'train', 'class': 'compartment', 'seats': 'invalid'}),
        make_yt_record({'type': 'train', 'class': 'compartment', 'price': 'invalid', 'seats': 10}),
        make_yt_record({'object_from_id': 'invalid'}),
        make_yt_record({'object_to_id': 'invalid'}),
        make_yt_record({})
    ))) == (
        ('c1-c2', date(2000, 6, 1).toordinal(), TransportType.BUS_ID, '', 123, None),
    )


@replace_now('2000-01-01')
@replace_setting('YT_ROOT_PATH', 'yt_tmp_path')
@replace_setting('ENVIRONMENT', 'test')
def test_get_yt_table_path():
    assert get_yt_table_path() == 'yt_tmp_path/test/rasp-min-prices-by-routes/1999-12-31'


@replace_setting('YT_TOKEN', mock.sentinel.yt_token)
@replace_setting('YT_PROXY', mock.sentinel.yt_proxy)
def test_open_yt_min_prices(yt_mock):
    with open_yt_min_prices('table_path') as yt_min_prices:
        assert yt_min_prices == mock.sentinel.yt_min_prices

    yt_mock.update_config.assert_called_once_with({
        'token': mock.sentinel.yt_token,
        'proxy': {'url': mock.sentinel.yt_proxy}
    })
    yt_mock.exists.assert_called_once_with('table_path')
    yt_mock.TempTable.assert_called_once_with()
    yt_mock.run_sort.assert_called_once_with('table_path', mock.sentinel.tmp_table,
                                             sort_by=SEGMENT_GROUP_KEYS + ('timestamp',))
    yt_mock.read_table.assert_called_once_with(mock.sentinel.tmp_table, format=yt.DsvFormat(), raw=False)

    yt_mock.exists.return_value = False

    with pytest.raises(RuntimeError):
        with open_yt_min_prices():
            pass


def test_save_prices(precalc_db_mock):
    price_rows = (('c1-s2', date(2000, 1, 1).toordinal(), TransportType.TRAIN_ID, '123П', 100500, None),
                  ('s2-c2', date(2000, 1, 2).toordinal(), TransportType.PLANE_ID, 'AB-123', 200300, None),
                  ('s1-s2', date(2000, 1, 3).toordinal(), TransportType.BUS_ID, '', 123, None))
    save_prices(price_rows)
    saved_rows = precalc_db_mock.open_db('prices').execute('SELECT * FROM price').fetchall()

    assert_that(saved_rows, contains_inanyorder(*price_rows))


def test_save_prices_segment_data(precalc_db_mock):
    segment_data = {'train_places': (('compartment', 10, 1234.),)}
    save_prices((
        ('c1-s2', date(2000, 1, 1).toordinal(), TransportType.TRAIN_ID, '123П', 100500,
         serialize_raw_data(segment_data)),
    ))
    saved_rows = precalc_db_mock.open_db('prices').execute('SELECT * FROM price').fetchall()

    assert len(saved_rows) == 1
    assert cPickle.loads(str(saved_rows[0][-1])) == segment_data


@pytest.mark.dbuser
def test_dump_prices(yt_mock, precalc_db_mock):
    yt_mock.read_table.return_value = [make_yt_record({})]
    dump_prices()
    saved_rows = precalc_db_mock.open_db('prices').execute('SELECT * FROM price').fetchall()

    assert len(saved_rows) == 1
    assert saved_rows[0] == ('c1-c2', date(2000, 6, 1).toordinal(), TransportType.BUS_ID, '', 123, None)
