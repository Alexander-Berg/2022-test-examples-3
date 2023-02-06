# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock
from collections import namedtuple
from datetime import datetime

import hamcrest
import pytest
import pytz
from django.conf import settings
from django.test.client import Client

from common.models.currency import Price
from common.tester.factories import create_station
from common.tester.transaction_context import transaction_fixture
from travel.rasp.wizards.suburban_wizard_api.lib.direction.tariffs_cache import TariffsCache
from travel.rasp.wizards.suburban_wizard_api.lib.serialization.proto_direction import load_query
from travel.rasp.wizards.suburban_wizard_api.views import proto_direction
from travel.rasp.wizards.wizard_lib.cache import Translations
from travel.rasp.wizards.wizard_lib.protobuf_models.direction_response_pb2 import TDirectionResponse

pytestmark = pytest.mark.dbuser


@pytest.fixture
def m_get_tariff():
    with mock.patch.object(TariffsCache, 'get_tariff', return_value=Price(123)) as m_get_tariff:
        yield m_get_tariff


@pytest.fixture
def m_find_segments():
    with mock.patch.object(proto_direction.schedule_cache, 'find_segments', autospec=True) as m_find_segments:
        yield m_find_segments


_DefaultQuery = namedtuple('_DefaultQuery', 'query departure_station arrival_station')


@pytest.fixture
@transaction_fixture
def default_query(request, fixed_now):
    departure_station = create_station(title='some_departure_station')
    arrival_station = create_station(title='some_arrival_station')
    return _DefaultQuery(
        query={
            'departure_point_key': departure_station.point_key,
            'arrival_point_key': arrival_station.point_key,
            'departure_date': '2000-01-01'
        },
        departure_station=departure_station,
        arrival_station=arrival_station
    )


def test_segments_not_found(m_find_segments, default_query):
    m_find_segments.return_value = ()
    response = Client().get('/api/proto_direction/', default_query.query)
    assert response.status_code == 204

    response = Client().get('/api/proto_direction/', dict(default_query.query, **{'json': 1}))
    assert response.status_code == 200
    assert response.json() == {'error': 'segments not found'}


def test_thread_predicate(default_query):
    test_query = load_query(dict(default_query.query, **{'thread_express_type': 'aeroexpress'}))
    thread_predicate = proto_direction._make_thread_predicate(test_query)
    assert thread_predicate is not None
    assert thread_predicate(mock.Mock(express_type='aeroexpress'))
    assert thread_predicate(mock.Mock(number='123', express_type='aeroexpress'))
    assert not thread_predicate(mock.Mock(express_type=''))
    assert not thread_predicate(mock.Mock(express_type='express'))


def test_thread_predicate_in_view(m_find_segments, default_query):
    m_find_segments.return_value = ()
    Client().get('/api/proto_direction/', default_query.query)
    thread_predicate = m_find_segments.call_args[1]['thread_predicate']

    assert thread_predicate is None

    Client().get('/api/proto_direction/', dict(default_query.query, **{'thread_number': '123'}))
    thread_predicate = m_find_segments.call_args[1]['thread_predicate']

    assert thread_predicate is not None
    assert thread_predicate(mock.Mock(number='123'))
    assert not thread_predicate(mock.Mock(number='456'))

    Client().get('/api/proto_direction/', dict(default_query.query, **{'thread_express_type': 'aeroexpress'}))
    thread_predicate = m_find_segments.call_args_list[-2][1]['thread_predicate']

    assert thread_predicate is not None
    assert thread_predicate(mock.Mock(express_type='aeroexpress'))
    assert not thread_predicate(mock.Mock(express_type=''))

    Client().get('/api/proto_direction/', dict(default_query.query, **{
        'thread_number': '123', 'thread_express_type': 'aeroexpress'
    }))
    thread_predicate = m_find_segments.call_args_list[-2][1]['thread_predicate']

    assert thread_predicate is not None
    assert thread_predicate(mock.Mock(number='123', express_type='aeroexpress'))
    assert not thread_predicate(mock.Mock(number='123', express_type=''))
    assert not thread_predicate(mock.Mock(number='456', express_type='aeroexpress'))


def test_segments_found(m_find_segments, m_get_tariff, rur, default_query):
    departure_station = default_query.departure_station
    arrival_station = default_query.arrival_station
    m_find_segments.return_value = (
        mock.Mock(**{
            'thread_start_dt': datetime(2000, 1, 1),
            'thread.express_type': None,
            'thread.uid': 'some_uid',
            'thread.number': 'some_number',
            'thread.title': Translations(**{lang: 'title' for lang in settings.MODEL_LANGUAGES}),
            'thread.t_subtype_id': None,
            'departure_dt': datetime(2000, 1, 1, 3, 4, tzinfo=pytz.UTC),
            'departure_station': departure_station,
            'arrival_dt': datetime(2000, 1, 1, 5, 6, tzinfo=pytz.UTC),
            'arrival_station': arrival_station,
        }),
    )

    response = Client().get('/api/proto_direction/', default_query.query)
    assert response.status_code == 200
    assert response['content-type'] == 'application/x-suburban-direction'

    proto_direction = TDirectionResponse()
    proto_direction.ParseFromString(response.content)
    hamcrest.assert_that(proto_direction, hamcrest.has_properties(
        Segments=hamcrest.contains(
            hamcrest.has_properties(Thread=hamcrest.has_properties(Number='some_number'))
        ),
        Total=1,
    ))

    response = Client().get('/api/proto_direction/', dict(default_query.query, **{'json': 1}))
    assert response.status_code == 200
    hamcrest.assert_that(response.json(), hamcrest.has_entries({
        'Segments': hamcrest.contains(
            hamcrest.has_entry('Thread', hamcrest.has_entry('Number', 'some_number'))
        ),
        'Total': 1
    }))
