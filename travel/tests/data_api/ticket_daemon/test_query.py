# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import date

import mock
import pytest
from django.conf import settings
from requests.exceptions import Timeout
from six.moves.queue import Queue

from common.data_api.ticket_daemon.query import Query, QueryQidPoll
from common.data_api.ticket_daemon.serialization.segment import Segment
from common.data_api.ticket_daemon.serialization.variant import Variant
from common.models.transport import TransportType
from common.tester.factories import create_partner, create_settlement, create_station
from common.tester.transaction_context import transaction_fixture


def test_field_defaults():
    with pytest.raises(TypeError) as excinfo:
        Query(foo=1)

    assert excinfo.value[0].endswith("got an unexpected keyword argument 'foo'")

    query = Query()

    assert query.national_version is None
    assert query.service is None
    assert query.point_from is None
    assert query.point_to is None
    assert query.date_forward is None
    assert query.date_backward is None
    assert query.klass is None
    assert query.passengers is None
    assert query.t_code is None
    assert query.partners is None
    assert query.user_settlement is None


def register_jsendapi(httpretty, path, responses, status_code=200):
    httpretty.register_uri(
        httpretty.GET, '{}/jsendapi/{}'.format(settings.TICKET_DAEMON_API_AVIA_HOST, path),
        responses=[
            httpretty.Response(body=json.dumps({'status': 'success', 'data': response_data}))
            for response_data in responses
        ],
        status=status_code,
        content_type='application/json'
    )


@pytest.fixture
@transaction_fixture
def dummy_query(request):
    return Query(
        service='service_code',
        point_from=create_settlement(),
        point_to=create_settlement(),
        date_forward=date(2000, 1, 1),
        passengers={'adults': 5},
        t_code='plane'  # all tests for planes
    )


def test_query_all(httpretty, dummy_query):
    register_jsendapi(httpretty, path='init_search/', responses=[{'qid': 'search qid'}])

    assert dummy_query.query_all() == 'search qid'
    assert httpretty.last_request.querystring == {
        'national': ['ru'],
        'service': ['service_code'],
        'lang': ['ru'],
        'point_from': [dummy_query.point_from.point_key],
        'point_to': [dummy_query.point_to.point_key],
        'date_forward': ['2000-01-01'],
        'adults': ['5'],
        'ignore_cache': ['False'],
        't_code': ['plane']
    }

    dummy_query.query_all(ignore_cache=True)
    assert httpretty.last_request.querystring['ignore_cache'] == ['True']


def test_collect_no_content(httpretty, dummy_query):
    register_jsendapi(httpretty, path='rasp/results_by_search_params/', responses=[{}], status_code=204)
    variants, statuses = dummy_query.collect_variants()
    assert len(variants) == 0
    assert len(statuses) == 0


STATION_FROM_ID = 10001
STATION_TO_ID = 10002


@pytest.mark.dbuser
def test_collect_variants_blank(httpretty, dummy_query):
    register_jsendapi(httpretty, path='rasp/results_by_search_params/', responses=(
        {'status': 'status value', 'variants': [], 'reference': {}},
        {
            'status': 'status value',
            'variants': [{
                'partner': 'some_partner_code',
                'forward': 'itinerary key',
                'backward': None,
                'order_data': None,
                'tariff': {'value': 100},
                'raw_seats': None,
                'raw_tariffs': None,
                'raw_is_several_prices': None
            }],
            'reference': {
                'itineraries': {
                    'itinerary key': ['segment key']
                },
                'flights': [
                    {
                        'key': 'segment key',
                        'station_from': STATION_FROM_ID,
                        'station_to': STATION_TO_ID,
                        'url': 'ya.test.url',
                    }
                ],
                'partners': [
                    {
                        'code': 'some_partner_code',
                        'title': 'some_partner_title',
                        'logoSvg': 'some_partner_logo'
                    }
                ]
            }
        },
    ))

    assert dummy_query.collect_variants() == ({}, 'status value')
    assert httpretty.last_request.querystring == {
        'national': ['ru'],
        'service': ['service_code'],
        'lang': ['ru'],
        'point_from': [dummy_query.point_from.point_key],
        'point_to': [dummy_query.point_to.point_key],
        'date_forward': ['2000-01-01'],
        'adults': ['5'],
        't_code': ['plane']
    }

    create_partner(code='partner value')
    create_station(id=STATION_FROM_ID)
    create_station(id=STATION_TO_ID)

    variants, _statuses = dummy_query.collect_variants()

    assert variants.keys() == ['some_partner_code']
    variant = variants.values()[0][0]
    assert isinstance(variant, Variant)
    assert isinstance(variant.forward.segments[0], Segment)
    assert variant.forward.segments[0].url == 'ya.test.url'


def _create_settlement_with_iata():
    settlement = create_settlement(iata='test')
    create_station(settlement=settlement, t_type=TransportType.objects.get(pk=TransportType.PLANE_ID))
    return settlement


def _create_settlement_with_sirena():
    settlement = create_settlement(sirena_id='test')
    create_station(settlement=settlement, t_type=TransportType.objects.get(pk=TransportType.PLANE_ID))
    return settlement


def _create_station_with_iata():
    station = create_station(t_type=TransportType.objects.get(pk=TransportType.PLANE_ID))
    station._iata = 'test'
    return station


def _create_station_with_sirena():
    return create_station(
        t_type=TransportType.objects.get(pk=TransportType.PLANE_ID),
        sirena_id='test'
    )


@pytest.mark.parametrize('from_factory,to_factory,t_code,expected', [
    (create_station, create_settlement, 'bus', True),
    (create_station, create_settlement, 'plane', False),

    (_create_settlement_with_iata, _create_settlement_with_sirena, 'plane', True),
    (create_settlement, _create_settlement_with_iata, 'plane', False),
    (_create_settlement_with_sirena, create_settlement, 'plane', False),

    (_create_station_with_iata, _create_settlement_with_sirena, 'plane', True),
    (create_station, _create_station_with_iata, 'plane', False),
    (_create_station_with_sirena, create_station, 'plane', False),
])
@pytest.mark.dbuser
def test_query_is_valid(from_factory, to_factory, t_code, expected):
    point_from = from_factory()
    point_to = to_factory()
    query = Query(
        point_from=point_from,
        point_to=point_to,
        t_code=t_code,
        date_forward=date(2000, 1, 1),
        passengers={'adults': 5}
    )
    assert query.is_valid() == expected


def test_query_execute():
    variants = ['foo']
    statuses = [1]

    query = Query()
    query.query_all = mock.MagicMock()
    query.collect_variants = mock.MagicMock(return_value=(variants, statuses))

    queue = Queue()
    query.execute(queue, True)

    query.query_all.assert_called_once()
    query.collect_variants.assert_called_once()

    assert not queue.empty()
    result = queue.get()
    assert result == (query, variants, statuses, None)
    assert queue.empty()


@pytest.mark.parametrize('ex', [
    Exception('bar'),
    Timeout('bar'),
])
def test_query_execute_collect_exception_happens(dummy_query, ex):
    ex = Exception('bar')
    dummy_query.collect_variants = mock.MagicMock(side_effect=ex)

    queue = Queue()
    dummy_query.execute(queue, False)

    assert not queue.empty()
    result = queue.get()
    assert result == (dummy_query, [], [], ex)
    assert queue.empty()


@pytest.mark.parametrize('ex', [
    Exception('bar'),
    Timeout('bar'),
])
def test_query_execute_collect_query_all_happens(dummy_query, ex):
    dummy_query.query_all = mock.MagicMock(side_effect=ex)

    queue = Queue()
    dummy_query.execute(queue, True)

    assert not queue.empty()
    result = queue.get()
    assert result == (dummy_query, [], [], ex)
    assert queue.empty()


@pytest.mark.dbuser
@pytest.mark.parametrize('poll_query', [
    QueryQidPoll(qid='111', skip_partners=['Org1', 'Org2']),
    QueryQidPoll(qid='222', skip_partners=[]),
    QueryQidPoll(qid='333'),
])
def test_poll_collect_variants(httpretty, poll_query):
    register_jsendapi(httpretty, path='rasp/results_by_qid/', responses=(
        {'status': 'status value', 'variants': [], 'reference': {}},
        {
            'status': 'status value',
            'variants': [{
                'partner': 'some_partner_code',
                'forward': 'itinerary key',
                'backward': None,
                'order_data': None,
                'tariff': {'value': 100},
                'raw_seats': None,
                'raw_tariffs': None,
                'raw_is_several_prices': None
            }],
            'reference': {
                'itineraries': {
                    'itinerary key': ['segment key']
                },
                'flights': [
                    {
                        'key': 'segment key',
                        'station_from': STATION_FROM_ID,
                        'station_to': STATION_TO_ID,
                        'url': 'ya.test.url',
                    }
                ],
                'partners': [
                    {
                        'code': 'some_partner_code',
                        'title': 'some_partner_title',
                        'logoSvg': 'some_partner_logo'
                    }
                ]
            }
        },
    ))

    assert poll_query.collect_variants() == ({}, 'status value')
    expected_avia_query = {
        'qid': [poll_query.qid],
        'lang': ['ru'],
    }
    if poll_query.skip_partners is not None:
        expected_avia_query['skip_partners'] = [json.dumps(poll_query.skip_partners)]
    assert httpretty.last_request.querystring == expected_avia_query

    create_partner(code='partner value')
    create_station(id=STATION_FROM_ID)
    create_station(id=STATION_TO_ID)

    variants, _statuses = poll_query.collect_variants()

    assert variants.keys() == ['some_partner_code']
    variant = variants.values()[0][0]
    assert isinstance(variant, Variant)
    assert isinstance(variant.forward.segments[0], Segment)
    assert variant.forward.segments[0].url == 'ya.test.url'


def test_poll_collect_no_content(httpretty):
    poll_query = QueryQidPoll(qid='qid_111')
    register_jsendapi(httpretty, path='rasp/results_by_qid/', responses={}, status_code=204)

    variants, statuses = poll_query.collect_variants()

    assert len(variants) == 0
    assert len(statuses) == 0


def test_poll_execute_exeption():
    from common.data_api.ticket_daemon.query import log
    poll_query = QueryQidPoll(qid='qid_111', yandexuid=12345)
    poll_query.collect_variants = mock.MagicMock(side_effect=Exception('exception message'))

    with mock.patch.object(log, 'exception') as m_log_exc:
        queue = Queue()
        poll_query.execute(queue)

    assert m_log_exc.call_count == 1
    call_msg = m_log_exc.mock_calls[0][1][0]
    call_args = m_log_exc.mock_calls[0][1][1]
    assert call_args['qid'] == 'qid_111'
    assert call_args['yandexuid'] == 12345
    assert call_msg.format(call_args)  # can format with args
