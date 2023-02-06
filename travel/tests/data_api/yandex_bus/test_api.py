# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta

import mock
import pytest
from requests.exceptions import ConnectionError, Timeout

from common.data_api.yandex_bus import api
from common.data_api.yandex_bus.api import (
    AVAILABLE_STATUS_ID, DATE_FORMAT, SEARCH_TEMPLATE,
    Segment, YandexBusSegmentSchema, StationAdapter, get_yandex_buses_results, BusApiError, BusConnectionError
)
from common.data_api.yandex_bus.factories import create_segment
from common.models.currency import Price
from common.tester.factories import create_station, create_settlement
from common.tester.utils.replace_setting import replace_dynamic_setting, replace_setting
from common.utils.date import MSK_TZ


def setup_function():
    api.yandex_bus_breaker._state_storage._fail_counter = 0


class TestSegmentSchema(object):
    def setup(self):
        self.schema = YandexBusSegmentSchema()

    @pytest.mark.dbuser
    def test_collect_points(self):
        point1 = create_station()
        point2 = create_station()
        point3 = create_settlement()
        self.schema.collect_points([
            {'from': point1.point_key, 'to': point2.point_key},
            {'from': point1.point_key, 'to': point2.point_key},
            {'from': point3.point_key},
            {'to': point3.point_key}
        ])
        for station in (point1, point2, point3):
            assert station.point_key in self.schema.context['points']

    @pytest.mark.dbuser
    def test_prepare_single(self):
        station = create_station()
        assert self.schema.prepare(
            {'from': station.point_key}, False) == {'from': station.point_key}
        assert station.point_key in self.schema.context['points']

    @pytest.mark.dbuser
    def test_prepare_many(self):
        station = create_station()
        result = self.schema.prepare(
            [{'from': station.point_key, 'status': {'id': AVAILABLE_STATUS_ID}}], many=True
        )
        assert result == [{'from': station.point_key, 'status': {'id': AVAILABLE_STATUS_ID}}]
        assert station.point_key in self.schema.context['points']

    def test_station_by_pk_no_context(self):
        assert self.schema.point_by_key('s10') is None

    def test_station_by_pk_no_station_in_context(self):
        self.schema.context['points'] = {'s10': 'station'}
        assert self.schema.point_by_key('foo') is None

    def test_station_by_pk_with_context(self):
        self.schema.context['points'] = {'s10': 'station'}
        assert self.schema.point_by_key('s10') == 'station'

    def test_filter_data(self):
        result = self.schema.filter_data([
            {'status': {'id': AVAILABLE_STATUS_ID}},
            {'status': {'id': 2}}
        ])
        assert result == [{'status': {'id': AVAILABLE_STATUS_ID}}]

    def test_add_price(self):
        result = self.schema.add_price({'currency': 'RUB', 'price': 100})
        assert result == {
            'currency': 'RUB',
            'price': {
                'currency': 'RUB',
                'value': 100
            }
        }

    @pytest.mark.dbuser
    def test_number(self):
        station_from = create_station()
        station_to = create_station()
        default_segment = {
            'departure': '2016-12-22T06:00:00',
            'from': station_from.point_key,
            'to': station_to.point_key,
            'currency': 'RUB',
            'price': 100
        }

        assert self.schema.load(dict(default_segment, **{'number': 123})).data.number == '123'
        assert self.schema.load(dict(default_segment, **{'number': '456'})).data.number == '456'


@pytest.mark.dbuser
def test_get_results(httpretty):
    dummy_host = 'http://test.com'
    settlement_from = create_settlement()
    settlement_to = create_settlement()
    station_from = create_station(settlement=settlement_from)
    station_to = create_station(settlement=settlement_to)
    date = datetime(2001, 1, 1)
    url = SEARCH_TEMPLATE.format(
        url=dummy_host, point_from=settlement_from.point_key,
        point_to=settlement_to.point_key, date=date.strftime(DATE_FORMAT)
    )

    response_body = '''[{}, {}, {}, {}]'''.format(
        _make_ybus_response_segment(station_from.point_key, station_to.point_key),
        _make_ybus_response_segment(settlement_from.point_key, station_to.point_key),
        _make_ybus_response_segment(station_from.point_key, settlement_to.point_key),
        _make_ybus_response_segment(settlement_from.point_key, settlement_to.point_key),
    )
    httpretty.register_uri(httpretty.GET, url, body=response_body)
    with replace_setting('YANDEX_BUS_API_URL', dummy_host):
        result, querying = get_yandex_buses_results(station_from, station_to, date)
        assert querying is False
        assert len(result) == 2
        assert result[0] == _make_ybus_check_segment(station_from, station_to)
        assert result[1] == _make_ybus_check_segment(station_from, settlement_to)


@pytest.mark.dbuser
def test_get_results_settlement_keys(httpretty):
    settlement_from = create_settlement(id=444)
    settlement_to = create_settlement(id=445)
    station_from = create_station(settlement=settlement_from, id=444)
    station_to = create_station(settlement=settlement_to, id=445)
    date = datetime(2022, 1, 1)

    dummy_host = 'http://test.com'
    response_body = '''[{}]'''.format(_make_ybus_response_segment(station_from.point_key, station_to.point_key))

    httpretty.register_uri(httpretty.GET, '{}/api/search/rasp'.format(dummy_host), body=response_body)
    with replace_setting('YANDEX_BUS_API_URL', dummy_host):
        get_yandex_buses_results(station_from, station_to, date)
        assert httpretty.last_request.querystring == {'date': ['2022-01-01'], 'from-id': ['c444'], 'to-id': ['c445']}

        # settlement_keys=False
        get_yandex_buses_results(station_from, station_to, date, settlement_keys=False)
        assert httpretty.last_request.querystring == {'date': ['2022-01-01'], 'from-id': ['s444'], 'to-id': ['s445']}
        get_yandex_buses_results(settlement_from, station_to, date, settlement_keys=False)
        assert httpretty.last_request.querystring == {'date': ['2022-01-01'], 'from-id': ['c444'], 'to-id': ['s445']}
        get_yandex_buses_results(station_from, settlement_to, date, settlement_keys=False)
        assert httpretty.last_request.querystring == {'date': ['2022-01-01'], 'from-id': ['s444'], 'to-id': ['c445']}


def _make_ybus_check_segment(point_from, point_to):
    return create_segment(
        title=u'Noemi Mahlum',
        price=Price(currency=u'RUR', value=100.0),
        seats=40,
        departure=MSK_TZ.localize(datetime(2016, 12, 22, 6, 0)),
        company_name=u'has arrived',
        station_to=point_to,
        station_from=point_from,
        id=u'23647:23650:MjM0MDJfTVRBd01EUjhNVEF3TURGOE1qSXVNVEl1TWpBeE5pQXdPakF3T2pBd2ZIeEhSRk14ZkRFd05URT01'
    )


def _make_ybus_response_segment(from_key, to_key):
    return '''{
        "arrival": null,
        "bus": null,
        "carrier": "has arrived",
        "currency": "RUR",
        "departure": "2016-12-22T06:00:00",
        "fee": 50.0,
        "freeSeats": 40,
        "from": "%s",
        "id": "23647:23650:MjM0MDJfTVRBd01EUjhNVEF3TURGOE1qSXVNVEl1TWpBeE5pQXdPakF3T2pBd2ZIeEhSRk14ZkRFd05URT01",
        "name": "Noemi Mahlum",
        "number": null,
        "price": 100.0,
        "refundConditions": "No way.",
        "status": {
            "id": 1,
            "name": "sale"
        },
        "ticketLimit": 5,
        "to": "%s"
    }''' % (from_key, to_key)


@pytest.mark.dbuser
@replace_dynamic_setting('YBUS_DONT_ASK_BETWEEN_SAME_POINT', True)
def test_dont_ask_in_one_settlement(httpretty):
    dummy_host = 'http://test.com'
    settlement = create_settlement()
    station_from = create_station(settlement=settlement)
    station_to = create_station(settlement=settlement)
    date = datetime(2001, 1, 1)
    url = SEARCH_TEMPLATE.format(
        url=dummy_host, point_from=settlement.point_key,
        point_to=settlement.point_key, date=date.strftime(DATE_FORMAT)
    )
    httpretty.register_uri(httpretty.GET, url, body='[]')
    with replace_setting('YANDEX_BUS_API_URL', dummy_host):
        result, querying = get_yandex_buses_results(station_from, station_to, date)
        assert querying is False
        assert not result

    assert not httpretty.latest_requests


@pytest.mark.dbuser
def test_get_results_partial_results(httpretty):
    dummy_url = 'http://test.com'
    station_from = create_station()
    station_to = create_station()
    date = datetime(2001, 1, 1)
    url = SEARCH_TEMPLATE.format(
        url=dummy_url, point_from=station_from.point_key,
        point_to=station_to.point_key, date=date.strftime(DATE_FORMAT)
    )
    httpretty.register_uri(httpretty.GET, url, status=206, body='''[{
        "carrier": "has arrived",
        "currency": "RUR",
        "departure": "2016-12-22T06:00:00",
        "from": "%s",
        "id": "23647:23650:MjM0MDJfTVRBd01EUjhNVEF3TURGOE1qSXVNVEl1TWpBeE5pQXdPakF3T2pBd2ZIeEhSRk14ZkRFd05URT01",
        "name": "Noemi Mahlum",
        "price": 100.0,
        "freeSeats": 0,
        "status": {
            "id": 1
        },
        "to": "%s"
    }]''' % (station_from.point_key, station_to.point_key))
    with replace_setting('YANDEX_BUS_API_URL', dummy_url):
        result, querying = get_yandex_buses_results(station_from, station_to, date)
        assert querying is True
        assert len(result) == 1
        assert result[0] == create_segment(
            title=u'Noemi Mahlum',
            price=Price(currency=u'RUR', value=100.0),
            seats=0,
            departure=MSK_TZ.localize(datetime(2016, 12, 22, 6, 0)),
            company_name=u'has arrived',
            station_to=station_to,
            station_from=station_from,
            id=u'23647:23650:MjM0MDJfTVRBd01EUjhNVEF3TURGOE1qSXVNVEl1TWpBeE5pQXdPakF3T2pBd2ZIeEhSRk14ZkRFd05URT01'
        )


@pytest.mark.dbuser
def test_get_results_no_data_request_initiated(httpretty):
    dummy_url = 'http://test.com'
    station_from = create_station()
    station_to = create_station()
    date = datetime(2001, 1, 1)
    url = SEARCH_TEMPLATE.format(
        url=dummy_url, point_from=station_from.point_key,
        point_to=station_to.point_key, date=date.strftime(DATE_FORMAT)
    )
    httpretty.register_uri(httpretty.GET, url, status=202, body='''[]''')
    with replace_setting('YANDEX_BUS_API_URL', dummy_url):
        result = get_yandex_buses_results(station_from, station_to, date)
        assert result == ([], True)


@pytest.mark.dbuser
def test_get_results_no_data(httpretty):
    dummy_url = 'http://test.com'
    station_from = create_station()
    station_to = create_station()
    date = datetime(2001, 1, 1)
    url = SEARCH_TEMPLATE.format(
        url=dummy_url, point_from=station_from.point_key,
        point_to=station_to.point_key, date=date.strftime(DATE_FORMAT)
    )
    httpretty.register_uri(httpretty.GET, url, body='''[]''')
    with replace_setting('YANDEX_BUS_API_URL', dummy_url):
        result = get_yandex_buses_results(station_from, station_to, date)
        assert result == ([], False)


@pytest.mark.dbuser
def test_get_results_throtling(httpretty):
    dummy_url = 'http://test.com'
    station_from = create_station()
    station_to = create_station()
    date = datetime(2001, 1, 1)
    url = SEARCH_TEMPLATE.format(
        url=dummy_url, point_from=station_from.point_key,
        point_to=station_to.point_key, date=date.strftime(DATE_FORMAT)
    )
    httpretty.register_uri(httpretty.GET, url, status=429, body='''[{
        "trash_data": "true"
    }]''')
    with replace_setting('YANDEX_BUS_API_URL', dummy_url):
        result, querying = get_yandex_buses_results(station_from, station_to, date)
        assert querying is False
        assert result == []


@pytest.mark.dbuser
def test_get_results_not_found(httpretty):
    dummy_url = 'http://test.com'
    station_from = create_station()
    station_to = create_station()
    date = datetime(2001, 1, 1)
    url = SEARCH_TEMPLATE.format(
        url=dummy_url, point_from=station_from.point_key,
        point_to=station_to.point_key, date=date.strftime(DATE_FORMAT)
    )
    httpretty.register_uri(httpretty.GET, url, status=404, body='''{
        {"error": {"message": "NOT FOUND", "code": null}}
    '''.strip())
    with replace_setting('YANDEX_BUS_API_URL', dummy_url):
        result, querying = get_yandex_buses_results(station_from, station_to, date)
        assert querying is False
        assert result == []


@pytest.mark.dbuser
def test_get_results_http_error(httpretty):
    dummy_url = 'http://test.com'
    station_from = create_station()
    station_to = create_station()
    date = datetime(2001, 1, 1)
    url = SEARCH_TEMPLATE.format(
        url=dummy_url, point_from=station_from.point_key,
        point_to=station_to.point_key, date=date.strftime(DATE_FORMAT)
    )
    httpretty.register_uri(httpretty.GET, url, status=500, body='''{
        {"error": {"message": "NOT FOUND", "code": null}}
    '''.strip())
    with replace_setting('YANDEX_BUS_API_URL', dummy_url):
        with pytest.raises(BusApiError):
            get_yandex_buses_results(station_from, station_to, date)


@pytest.mark.dbuser
@pytest.mark.parametrize('exception', [ConnectionError(), Timeout()])
def test_get_results_connection_error(exception):
    date = datetime(2001, 1, 1)
    with mock.patch('common.data_api.yandex_bus.api.requests.get', side_effect=exception):
        with pytest.raises(BusConnectionError):
            get_yandex_buses_results(create_station(), create_station(), date)


class TestStationAsapter(object):

    @pytest.mark.dbuser
    def test_settlement_property_with_station(self):
        settlement = create_settlement()
        point = create_station(settlement=settlement)
        adapter = StationAdapter(point)
        assert adapter.settlement == settlement

    @pytest.mark.dbuser
    def test_settlement_property_with_settlement(self):
        point = create_settlement()
        adapter = StationAdapter(point)
        assert adapter.settlement == point


class TestSegment(object):

    @pytest.mark.parametrize('segment, expected', (
        (Segment(), None),
        (Segment(departure=datetime(2000, 1, 1)), None),
        (Segment(departure=datetime(2000, 1, 1), arrival=datetime(2000, 1, 2)), timedelta(days=1))
    ))
    def test_duration(self, segment, expected):
        assert segment.duration == expected

    @pytest.mark.parametrize('segment, expected', (
        (Segment(), None),
        (Segment(departure=datetime(2000, 1, 1)), None),
        (Segment(departure=datetime(2000, 1, 1), arrival=datetime(2000, 1, 2)), 1440),
        (Segment(departure=datetime(2000, 1, 1), arrival=datetime(2000, 1, 1, 1, 0, 59)), 60)
    ))
    def test_get_duration(self, segment, expected):
        assert segment.get_duration() == expected
