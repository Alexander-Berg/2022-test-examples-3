# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import date, datetime
from urllib import urlencode

import mock
import pytest
from django.test import Client
from freezegun import freeze_time
from hamcrest import has_entries, has_entry, assert_that, contains, contains_inanyorder
from pytz import UTC

import travel.rasp.morda_backend.morda_backend.tariffs.daemon.service
import travel.rasp.morda_backend.morda_backend.tariffs.daemon.views
from common.data_api.min_prices.api import MinPriceStorage
from common.data_api.min_prices.factory import create_min_price
from common.data_api.ticket_daemon.factories import create_segment
from common.data_api.ticket_daemon.query import QueryQidPoll
from common.models.transport import TransportType
from common.tester import transaction_context
from common.tester.factories import create_settlement, create_station
from common.tester.utils.datetime import replace_now
from common.tester.utils.mongo import tmp_collection
from common.utils.date import smart_localize
from common.utils.iterrecipes import product_by_key
from travel.rasp.morda_backend.morda_backend.tariffs.daemon.serialization import DaemonQuery


def get_default_transport_types():
    return [TransportType.get_plane_type()]


@pytest.fixture(scope='module')
@transaction_context.transaction_fixture
def points(request):
    moscow = create_settlement(longitude=37.619899, latitude=55.753676)
    piter = create_settlement(longitude=30.315868, latitude=59.939095)
    return moscow, piter


def _check_response(path, query, matcher, poll=False):
    if poll:
        path += 'poll/'

    response = Client().get('{}?{}'.format(path, urlencode(query, doseq=True)))
    assert response.status_code == 200
    if matcher is not None:
        assert_that(json.loads(response.content), matcher)


def check_tariffs_response(query, matcher=None, poll=False):
    _check_response('/ru/segments/tariffs/', query, matcher, poll)


def check_min_tariffs_response(query, matcher=None, poll=False):
    _check_response('/ru/segments/min-tariffs/', query, matcher, poll)


@pytest.yield_fixture
def m_collect_segments():
    with mock.patch.object(
        travel.rasp.morda_backend.morda_backend.tariffs.daemon.views, 'collect_segments',
        return_value={'querying': False, 'segments': []}
    ) as m_collect_segments:
        yield m_collect_segments


@pytest.yield_fixture
def m_collect_daemon_segments():
    with mock.patch.object(
        travel.rasp.morda_backend.morda_backend.tariffs.daemon.views, 'collect_daemon_segments',
        return_value={'querying': False, 'segments': []}
    ) as m_collect_daemon_segments:
        yield m_collect_daemon_segments


@pytest.mark.dbuser
@replace_now(datetime(2000, 1, 1))
def test_blank_query_parsing(m_collect_segments, points):
    point_from, point_to = points

    # передает минимальный набор параметров в collect_segments
    check_tariffs_response({
        'pointFrom': point_from.point_key,
        'pointTo': point_to.point_key,
        'national_version': 'ru'
    })

    m_collect_segments.assert_called_once_with(mock.ANY, send_query=True)
    daemon_query_arg = m_collect_segments.call_args[0][0]
    assert daemon_query_arg._replace(transport_types=None) == DaemonQuery(point_from=point_from, point_to=point_to,
                                                                          dates=[date(2000, 1, 1)])
    assert set(daemon_query_arg.transport_types) == {t for t in get_default_transport_types()}


@pytest.mark.dbuser
def test_drop_plane_parsing(m_collect_segments, points):
    point_from, point_to = points
    dates = [date(2016, 6, 1), date(2017, 1, 1)]

    # передает полный набор параметров в collect_segments
    check_tariffs_response({
        'pointFrom': point_from.point_key,
        'pointTo': point_to.point_key,
        'date': map(str, dates),
        'transportType': ['plane'],
        'national_version': 'ru'
    })

    daemon_query = DaemonQuery(**{
        'point_from': point_from,
        'point_to': point_to,
        'dates': dates,
        'transport_types': [TransportType.objects.get(id=TransportType.PLANE_ID)],
        'national_version': 'ru'
    })
    m_collect_segments.assert_called_once_with(daemon_query, send_query=True)


@pytest.mark.dbuser
def test_full_query_parsing(m_collect_segments, points):
    point_from, point_to = points
    client_settlement = create_settlement()
    dates = [date(2016, 6, 1), date(2017, 1, 1)]

    # передает полный набор параметров в collect_segments
    check_tariffs_response({
        'pointFrom': point_from.point_key,
        'pointTo': point_to.point_key,
        'date': map(str, dates),
        'transportType': 'plane',
        'clientSettlementId': client_settlement.id,
        'national_version': 'us'
    })

    daemon_query = DaemonQuery(**{
        'point_from': point_from,
        'point_to': point_to,
        'dates': dates,
        'transport_types': [TransportType.objects.get(id=TransportType.PLANE_ID)],
        'client_settlement': client_settlement,
        'national_version': 'us'
    })
    m_collect_segments.assert_called_once_with(daemon_query, send_query=True)


@pytest.mark.dbuser
def test_query_parsing_errors(points):
    point = points[0]

    # при ошибке разбора запроса сериализует ошибку и не вызывает получение данных
    check_tariffs_response(
        {'pointFrom': point.point_key, 'pointTo': point.point_key},
        has_entries(errors=has_entries(_schema=[{'same_points': 'same_points'}]))
    )


@pytest.mark.dbuser
@replace_now(datetime(2000, 1, 1))
def test_only_polling_if_too_many_dates(m_collect_segments, points):
    point_from, point_to = points
    client_settlement = create_settlement()
    dates = [date(2016, 6, 1), date(2017, 1, 1), date(2017, 1, 2)]

    # передает полный набор параметров в collect_segments
    check_tariffs_response({
        'pointFrom': point_from.point_key,
        'pointTo': point_to.point_key,
        'date': map(str, dates),
        'transportType': 'plane',
        'clientSettlementId': client_settlement.id,
        'national_version': 'us'
    })

    daemon_query = DaemonQuery(**{
        'point_from': point_from,
        'point_to': point_to,
        'dates': dates,
        'transport_types': [
            TransportType.objects.get(id=TransportType.PLANE_ID)],
        'client_settlement': client_settlement,
        'national_version': 'us'
    })
    m_collect_segments.assert_called_once_with(daemon_query, send_query=False)


def get_default_segment_kwargs():
    return dict(
        number='',
        thread={},
        departure=smart_localize(datetime(2016, 1, 1), UTC),
        arrival=smart_localize(datetime(2016, 1, 1), UTC),
        station_from=create_station(),
        station_to=create_station(),
    )


@pytest.mark.dbuser
def test_tariffs_format(m_collect_segments, points):
    point_from, point_to = points
    segment_kwargs = get_default_segment_kwargs()
    m_collect_segments.return_value = {
        'querying': True,
        'segments': [
            create_segment(key='key 1', display_t_code='plane', **segment_kwargs),
            create_segment(key='key 2', display_t_code='plane', **segment_kwargs),
            create_segment(key='key 3', display_t_code='plane', **segment_kwargs)
        ]
    }

    check_tariffs_response(
        {'pointFrom': point_from.point_key, 'pointTo': point_to.point_key, 'national_version': 'ru'},
        has_entries({
            'querying': True,
            'segments': contains(
                has_entry('key', 'key 1'),
                has_entry('key', 'key 2'),
                has_entry('key', 'key 3')
            )
        })
    )


@freeze_time('2016-09-09')
@pytest.mark.dbuser
@pytest.mark.parametrize("request_params,expected_prices", [
    # фильтруем по pointFrom и pointTo
    (
        {'pointFrom': 'c2', 'pointTo': 'c213', 'national_version': 'ru'},
        product_by_key({
            'object_from_id': [2],
            'object_to_id': [213],
            'type': ['train', 'bus', 'plane'],
            'class': ['economy', 'like_a_boss'],
            'price': [1000]
        })
    ),
    # добавляем фильтр по типу транспорта
    (
        {
            'pointFrom': 'c2',
            'pointTo': 'c213',
            'transportType': ['train', 'bus'],
            'national_version': 'ru'
        },
        product_by_key({
            'object_from_id': [2],
            'object_to_id': [213],
            'type': ['train', 'bus'],
            'class': ['economy', 'like_a_boss'],
            'price': [1000]
        })
    )
])
def test_min_tariffs_filtered_by_points(request_params, expected_prices):
    expected_entries = [
        has_entries(
            key=_format_key(item),
            classes={
                item['class']: {
                    'price': {
                        'currency': 'RUB',
                        'value': 1000
                    }
                }
            },
        ) for item in expected_prices
    ]
    with tmp_collection('min_prices_test') as col:
        storage = MinPriceStorage(col)
        with mock.patch('travel.rasp.morda_backend.morda_backend.tariffs.daemon.service.min_price_storage', storage):
            populate_min_prices(col)
            check_min_tariffs_response(
                request_params,
                has_entry('tariffs', contains_inanyorder(*expected_entries))
            )


def populate_min_prices(collection):
    for id_ in [1, 2, 3]:
        create_settlement(id=id_)

    test_data = product_by_key({
        'object_from_id': [1, 2],
        'object_to_id': [213, 3],
        'date_forward': ['2016-09-10'],
        'type': ['train', 'bus', 'plane'],
        'class': ['economy', 'like_a_boss'],
        'price': [1000, 2000, 3000]
    })
    for item in test_data:
        item['route_uid'] = _format_key(item)
        create_min_price(collection, item)


def _format_key(item):
    return '{object_from_id}-{object_to_id}-{type}-{class}'.format(**item)


def get_tariffs_init_response(query, lang='ru'):
    qs = urlencode(query, True)
    response = Client().get('/{lang}/tariffs/plane/?{qs}'.format(
        lang=lang,
        qs=qs
    ))
    return response.status_code, json.loads(response.content)


@pytest.mark.dbuser
def test_tariffs_init(points):
    dates = [date(2018, 12, 1), date(2018, 12, 2)]
    point_from, point_to = points
    client_settlement = create_settlement()
    query = {
        'pointFrom': point_from.point_key,
        'pointTo': point_to.point_key,
        'date': map(str, dates),
        'clientSettlementId': client_settlement.id,
        'national_version': 'us',
        'yandexuid': 1234567890987654321,
    }
    with mock.patch.object(
        travel.rasp.morda_backend.morda_backend.tariffs.daemon.views, 'init_plane_tariff_queries',
        return_value={'qids': ['111', '222']}
    ):
        status_code, response = get_tariffs_init_response(query)

    assert status_code == 200
    assert_that(response, has_entry('qids', ['111', '222']))


def get_tariffs_poll_response(query, lang='ru'):
    qs = urlencode(query, True)
    response = Client().get('/{lang}/tariffs/plane/poll/?{qs}'.format(
        lang=lang,
        qs=qs
    ))
    return response.status_code, json.loads(response.content)


@pytest.mark.dbuser
def test_tariffs_poll(m_collect_daemon_segments):
    qid = '111'
    skip_partners = ['org1', 'org2', 'org3']
    query = {
        'qid': qid,
        'skip_partners': json.dumps(skip_partners),
        'yandexuid': 1234567890987654321,
    }

    segment_kwargs = get_default_segment_kwargs()
    m_collect_daemon_segments.return_value = {
        'querying': True,
        'segments': [
            create_segment(key='key 1', display_t_code='plane', **segment_kwargs),
            create_segment(key='key 2', display_t_code='plane', **segment_kwargs),
            create_segment(key='key 3', display_t_code='plane', **segment_kwargs)
        ]
    }

    status_code, response = get_tariffs_poll_response(query)

    assert status_code == 200
    assert_that(response, has_entries({'querying': True,
                                       'segments': contains(has_entry('key', 'key 1'),
                                                            has_entry('key', 'key 2'),
                                                            has_entry('key', 'key 3'))}))

    expected_queries = [QueryQidPoll(qid=qid, skip_partners=skip_partners, yandexuid=1234567890987654321)]
    m_collect_daemon_segments.assert_called_once_with(expected_queries)
