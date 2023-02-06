# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, datetime
from urlparse import parse_qs, urlsplit

import mock
import pytest
from hamcrest import (
    all_of, assert_that, contains, contains_inanyorder,
    has_entry, has_entries, has_key, has_properties, instance_of, is_not
)
from mock import sentinel

import travel.rasp.morda_backend.morda_backend.tariffs.daemon.service
from common.data_api.ticket_daemon import query as ticket_query
from common.data_api.ticket_daemon.factories import create_segment, create_variant
from common.models.currency import Price
from common.models.transport import TransportType
from common.tester.factories import create_partner, create_settlement, create_station, create_thread
from common.utils.date import MSK_TZ, UTC_TZ, FuzzyDateTime
from common.utils.title_generator import DASH
from travel.rasp.morda_backend.morda_backend.tariffs.daemon.serialization import DaemonQuery
from travel.rasp.morda_backend.morda_backend.tariffs.daemon.service import (
    get_better_class, get_plane_tariffs_classes, add_order_urls, collect_segments,
    create_daemon_queries, get_tariffs_classes, iter_variants_segments, make_daemon_key, make_route_key,
    parse_variants_segments, merge_segments, init_plane_tariff_queries, create_poll_tariff_query
)


def get_default_transport_types():
    return TransportType.get_plane_type()


@pytest.mark.dbuser
@mock.patch('common.data_api.ticket_daemon.query.Query', side_effect=[
    sentinel.ticket_query_1, sentinel.ticket_query_2,
    sentinel.ticket_query_3, sentinel.ticket_query_4
])
def test_create_daemon_queries(m_Query):
    """
    Query для списка дат и типов транспорта
    """
    plane = TransportType.objects.get(id=TransportType.PLANE_ID)
    bus = TransportType.objects.get(id=TransportType.BUS_ID)
    yandexuid = 1234567890987654321
    queries = list(create_daemon_queries(DaemonQuery(
        point_from=sentinel.from_point, point_to=sentinel.to_point, dates=[sentinel.date_1, sentinel.date_2],
        transport_types=[plane, bus], client_settlement=sentinel.client_settlement,
        national_version=sentinel.national_version, yandexuid=yandexuid
    )))

    assert queries == [sentinel.ticket_query_1, sentinel.ticket_query_2,
                       sentinel.ticket_query_3, sentinel.ticket_query_4]
    assert m_Query.call_args_list == [
        mock.call(
            user_settlement=sentinel.client_settlement,
            point_from=sentinel.from_point,
            point_to=sentinel.to_point,
            date_forward=sentinel.date_1,
            date_backward=None,
            passengers={'adults': 1},
            klass='economy',
            national_version=sentinel.national_version,
            t_code='plane',
            yandexuid=yandexuid
        ),
        mock.call(
            user_settlement=sentinel.client_settlement,
            point_from=sentinel.from_point,
            point_to=sentinel.to_point,
            date_forward=sentinel.date_2,
            date_backward=None,
            passengers={'adults': 1},
            klass='economy',
            national_version=sentinel.national_version,
            t_code='plane',
            yandexuid=yandexuid
        ),
        mock.call(
            user_settlement=sentinel.client_settlement,
            point_from=sentinel.from_point,
            point_to=sentinel.to_point,
            date_forward=sentinel.date_1,
            date_backward=None,
            passengers={'adults': 1},
            klass='economy',
            national_version=sentinel.national_version,
            t_code='bus',
            yandexuid=yandexuid
        ),
        mock.call(
            user_settlement=sentinel.client_settlement,
            point_from=sentinel.from_point,
            point_to=sentinel.to_point,
            date_forward=sentinel.date_2,
            date_backward=None,
            passengers={'adults': 1},
            klass='economy',
            national_version=sentinel.national_version,
            t_code='bus',
            yandexuid=yandexuid
        ),
    ]


@pytest.mark.dbuser
def test_make_route_key():
    """
    Ключ для сегмента с номером и без номера
    Ключ для автобусного сегмента без номера со скрытым номерм в нитке
    Ключ для автобусного сегмента без номера и скрытого номера в нитке от swdfactory
    """
    assert make_route_key(mock.Mock(number='foo 1')) == 'foo-1'
    assert make_route_key(mock.Mock(number='')) == ''

    bus = TransportType.objects.get(id=TransportType.BUS_ID)
    assert make_route_key(mock.Mock(
        number='',
        t_type=bus,
        thread=create_thread(hidden_number='bar 2'))
    ) == 'bar-2'
    assert make_route_key(mock.Mock(
        number='',
        t_type=bus,
        thread=None,
        supplier_code='swdfactory',
        departure=datetime(2000, 1, 1, 12, 15)
    )) == 'time1215'


@pytest.mark.dbuser
@mock.patch('travel.rasp.morda_backend.morda_backend.tariffs.daemon.service.make_route_key', return_value='route_key')
def test_make_key(m_make_route_key):
    """
    Ключ для обычного сегмента
    Ключ для самолетного сегмента с данными
    Ключ для самолетного сегмента без данных
    Ключ для самолетного сегмента с данными и FuzzyDateTime
    Ключ для самолетного сегмента без данных и FuzzyDateTime
    """
    assert make_daemon_key(mock.Mock(departure=datetime(2000, 1, 30))) == 'daemon route_key 0130'

    segment = mock.Mock(
        departure=datetime(2000, 1, 30, 1, 2),
        arrival=datetime(2000, 1, 30, 3, 4),
        t_type=TransportType.objects.get(id=TransportType.PLANE_ID),
        data='segment_data'
    )
    assert make_daemon_key(segment) == 'daemon route_key 0130 segment_data'

    del segment.data
    assert make_daemon_key(segment) == 'daemon route_key 0130'

    segment = mock.Mock(
        departure=FuzzyDateTime(datetime(2000, 1, 20, 5, 6)),
        arrival=FuzzyDateTime(datetime(2000, 1, 20, 7, 8)),
        t_type=TransportType.objects.get(id=TransportType.PLANE_ID),
        data='segment_data'
    )
    assert make_daemon_key(segment) == 'daemon route_key 0120 segment_data'

    del segment.data
    assert make_daemon_key(segment) == 'daemon route_key 0120'


def test_get_tariffs_classes():
    """
    Тарифы для обычного сегмента с классом кода транспорта
    Тарифы для сегмента с сырыми тарифами и местами
    Тарифы для сегмента с сырыми тарифами, местами и несколькими ценами
    """
    assert get_tariffs_classes(create_variant(tariff=sentinel.tariff, seats=sentinel.seats), 'default_class') == {
        'default_class': {'price': sentinel.tariff, 'seats': sentinel.seats}
    }

    assert get_tariffs_classes(
        create_variant(
            raw_tariffs={'class 1': sentinel.tariff_1, 'class 2': sentinel.tariff_2},
            raw_seats={'class 1': sentinel.seats_1},
            raw_is_several_prices={}
        ),
        'default_class'
    ) == {
        'class 1': {'price': sentinel.tariff_1, 'seats': sentinel.seats_1, 'several_prices': False},
        'class 2': {'price': sentinel.tariff_2, 'seats': 1, 'several_prices': False}
    }

    assert get_tariffs_classes(
        create_variant(
            raw_tariffs={'class 1': sentinel.tariff_1, 'class 2': sentinel.tariff_2},
            raw_seats={'class 1': sentinel.seats_1},
            raw_is_several_prices={'class 2': sentinel.is_several_prices}
        ),
        'default_class'
    ) == {
        'class 1': {'price': sentinel.tariff_1, 'seats': sentinel.seats_1, 'several_prices': False},
        'class 2': {'price': sentinel.tariff_2, 'seats': 1, 'several_prices': sentinel.is_several_prices}
    }


@pytest.mark.dbuser
def test_iter_variants_segments():
    station_from = create_station()
    station_to = create_station()
    default_datetime = datetime(2000, 1, 1)
    segment = create_segment(station_from=station_from, station_to=station_to,
                             departure=default_datetime, arrival=default_datetime)
    variant = create_variant(segments=[segment])
    partner = 'partner'

    assert list(iter_variants_segments({partner: [variant]})) == [(partner, variant, segment)]


@pytest.mark.dbuser
def test_parse_variants_segments():
    station_from = create_station()
    station_to = create_station()
    plane = TransportType.objects.get(id=TransportType.PLANE_ID)
    query = next(create_daemon_queries(DaemonQuery(point_from=station_from, point_to=station_to,
                                                   dates=[date.today()], transport_types=[plane])))
    segment_kwargs = dict(
        number='123',
        station_from=station_from,
        station_to=station_to,
        departure=datetime(2000, 1, 1, tzinfo=UTC_TZ),
        arrival=datetime(2000, 1, 2, tzinfo=UTC_TZ),
    )

    # форматирует сегменты самолетов без электронного билета
    plane_segment = create_segment(
        display_t_code='plane',
        electronic_ticket=False,
        **segment_kwargs
    )

    partner = 'partner1'

    assert_that(
        list(parse_variants_segments(query, [(partner, create_variant(segments=[plane_segment]), plane_segment)])),
        contains(has_properties(
            key='daemon 123 0101',
            tariffs=has_entries({
                'partner': partner,
                'electronic_ticket': False,
                'classes': has_entries({'economy': instance_of(dict)})
            })
        ))
    )


@pytest.mark.dbuser
def test_parse_variants_segments_order_urls():
    """
    Заполняет order_url
    """
    station_from = create_station()
    station_to = create_station()
    plane = TransportType.objects.get(id=TransportType.PLANE_ID)
    query = next(create_daemon_queries(DaemonQuery(point_from=station_from, point_to=station_to,
                                                   dates=[date.today()], transport_types=[plane])))
    segment_kwargs = dict(
        number='123',
        station_from=station_from,
        station_to=station_to,
        departure=datetime(2016, 1, 1, tzinfo=UTC_TZ),
        arrival=datetime(2016, 1, 2, tzinfo=UTC_TZ)
    )
    plane_segment = create_segment(display_t_code='plane', **segment_kwargs)
    bus_segment = create_segment(display_t_code='bus', **segment_kwargs)

    partner = 'partner1'

    segments = parse_variants_segments(query, [
        (partner, create_variant(segments=[plane_segment]), plane_segment),
        (partner, create_variant(segments=[bus_segment]), bus_segment),
    ])
    for segment in segments:
        classes = segment.tariffs['classes']
        for tariff in classes.values():
            assert 'order_url' in tariff


@pytest.mark.dbuser
@mock.patch.object(ticket_query.Query, 'collect_variants', return_value=[{}, {}])
@mock.patch.object(ticket_query.Query, 'query_all')
def test_collect_segments_with_exception(m_query_all, m_collect_variants):
    station_from = create_station()
    station_to = create_station()
    create_partner(enabled_in_rasp_ru=True)
    daemon_query = DaemonQuery(point_from=station_from, point_to=station_to, dates=[date(2000, 1, 1)],
                               transport_types=[TransportType.objects.get(pk=TransportType.PLANE_ID),
                                                TransportType.objects.get(pk=TransportType.BUS_ID)])

    # вызывает query_all когда send_query=True
    collect_segments(daemon_query, send_query=True)

    m_query_all.assert_called()
    m_query_all.reset_mock()

    # не вызывает query_all когда send_query=False
    collect_segments(daemon_query, send_query=False)

    assert not m_query_all.called

    from common.data_api.ticket_daemon.query import log

    exception = Exception('Error When Querying')
    m_query_all.side_effect = exception

    with mock.patch.object(log, 'exception') as m_log_exc:
        assert_that(
            collect_segments(daemon_query, send_query=True),
            {'querying': False}
        )

        assert m_log_exc.call_count == 1
        call_args = m_log_exc.mock_calls[0][1][1]
        assert call_args['point_to'] == station_to.point_key
        assert call_args['point_from'] == station_from.point_key
        assert call_args['t_code'] == 'bus'

    m_query_all.side_effect = None

    # возвращает {'querying': True} когда есть статус 'querying'
    m_collect_variants.return_value = [{}, {'partner_1': 'done', 'partner_2': 'querying', 'partner_3': 'done'}]

    assert_that(
        collect_segments(daemon_query, send_query=True),
        has_entries('querying', True)
    )

    # возвращает {'querying': False} когда нет статуса 'querying'
    m_collect_variants.return_value = [{}, {'partner_1': 'done', 'partner_2': 'done', 'partner_3': 'done'}]

    assert_that(
        collect_segments(daemon_query, send_query=True),
        has_entries('querying', False)
    )


@pytest.mark.dbuser
@mock.patch.object(ticket_query.Query, 'collect_variants', return_value=[{}, {}])
@mock.patch.object(ticket_query.Query, 'query_all')
def test_collect_segments(m_query_all, m_collect_variants):
    station_from = create_station()
    station_to = create_station()
    base_daemon_query = DaemonQuery(point_from=station_from, point_to=station_to,
                                    transport_types=get_default_transport_types())

    # передает в create_daemon_queries список типов транспорта по-умолчанию когда нет аргумента transport_types
    with mock.patch(
        'travel.rasp.morda_backend.morda_backend.tariffs.daemon.service.create_daemon_queries', return_value=()
    ) as m_create_daemon_queries:
        with_dates_query = base_daemon_query._replace(dates=sentinel.dates)
        collect_segments(with_dates_query)

        m_create_daemon_queries.assert_called_once_with(with_dates_query)

    # передает в create_daemon_queries типы транспорта
    with mock.patch(
        'travel.rasp.morda_backend.morda_backend.tariffs.daemon.service.create_daemon_queries', return_value=()
    ) as m_create_daemon_queries:
        with_transport_types_query = base_daemon_query._replace(
            dates=sentinel.dates,
            transport_types=TransportType.objects.filter(id__in=(TransportType.BUS_ID, TransportType.WATER_ID))
        )

        collect_segments(with_transport_types_query)

        m_create_daemon_queries.assert_called_once_with(with_transport_types_query)

    # возвращает сегменты
    create_partner(enabled_in_rasp_ru=True)
    segment = create_segment(
        number='123',
        station_from=station_from,
        station_to=station_to,
        departure=datetime(2000, 1, 1, tzinfo=UTC_TZ),
        display_t_code='plane',
        arrival=datetime(2000, 1, 2, tzinfo=UTC_TZ)
    )
    m_collect_variants.return_value = [{'partner': [create_variant(segments=[segment])]}, {'partner': 'done'}]

    assert_that(
        collect_segments(base_daemon_query._replace(
            dates=[date(2000, 1, 1)],
            transport_types=[TransportType.objects.get(id=TransportType.BUS_ID)]
        )),
        has_entries('segments', contains(has_properties(key=instance_of(basestring), tariffs=instance_of(dict))))
    )


@pytest.mark.dbuser
def test_get_plane_tariffs_classes():
    segment = create_variant(from_company=False, deep_url='some_deep_url')
    assert get_plane_tariffs_classes(segment) == {
        'economy': {
            'query_time': None,
            'order_url': 'test_order_link',
            'price': Price(100),
            'from_company': False,
            'deep_url': None,
            'seats': 1
        }
    }


@pytest.mark.dbuser
@pytest.mark.parametrize('segment, expected', ((
    {
        'number': '',
    },
    all_of(
        has_entries({'number': ['']}),
        is_not(has_key('et')),
        is_not(has_key('thread'))
    )
), (
    {
        'number': 'segment number',
        'electronic_ticket': False,
    },
    all_of(
        has_entries({'number': ['segment number']}),
        is_not(has_key('et')),
        is_not(has_key('thread'))
    )
), (
    {
        'number': '',
        'electronic_ticket': True,
        'thread': {'hidden_number': 'thread hidden_number', 'uid': 'thread uid'},
    },
    has_entries({'number': ['thread hidden_number'], 'thread': ['thread uid'], 'et': ['t']}),
)))
def test_add_order_urls(segment, expected):
    """
    Добавление ссылок в классы тарифов
    """
    classes = {'foo': {'price': Price(100)}, 'bar': {'price': Price(300, 'USD')}}
    point_from = create_settlement()
    point_to = create_settlement()
    station_from = create_station(title='Station 1')
    station_to = create_station(title='Station 2')
    segment = create_segment(
        station_from=station_from,
        station_to=station_to,
        departure=datetime(2016, 1, 1, 12, tzinfo=MSK_TZ),
        arrival=datetime(2016, 1, 1, 22, tzinfo=MSK_TZ),
        display_t_code='bus',
        **segment
    )
    add_order_urls(classes, point_from, point_to, segment)

    for tariff_class, tariff in classes.items():
        assert 'order_url' in tariff
        url_parts = urlsplit(tariff['order_url'])
        assert url_parts[:3] == ('', '', '/buy/')
        query = parse_qs(url_parts.query, keep_blank_values=True)
        assert_that(query, has_entries({
            'point_from': [point_from.point_key],
            'point_to': [point_to.point_key],
            'station_from': [str(station_from.id)],
            'station_to': [str(station_to.id)],
            'departure': ['2016-01-01 12:00:00'],
            'arrival': ['2016-01-01 22:00:00'],
            'title': ['Station 1 {} Station 2'.format(DASH).encode('utf-8')],
            'date': ['2016-01-01'],
            't_type': ['bus'],
            'cls': [tariff_class],
            'tariff': [str(tariff['price'].value)]
        }))
        assert_that(query, expected)


@pytest.mark.dbuser
@pytest.mark.parametrize('current, new, expected', ((
    {'classes': {}},
    {'classes': {}},
    has_properties(tariffs=has_key('classes'))
), (
    {'classes': {}},
    {'classes': {'economy': {'price': Price(100)}}},
    has_properties(
        tariffs=has_entries(
            classes=has_entries(
                economy=has_entries(
                    price=has_properties(
                        value=100)))))
), (
    {'classes': {'economy': {'price': Price(200)}}},
    {'classes': {'economy': {'price': Price(100)}}},
    has_properties(
        tariffs=has_entries(
            classes=has_entries(
                economy=has_entries(
                    price=has_properties(
                        value=100)))))
), (
    {'classes': {'premium': {'price': Price(200)}}},
    {'classes': {'economy': {'price': Price(100)}}},
    has_properties(
        tariffs=has_entries(
            classes=has_entries(
                economy=has_entries(
                    price=has_properties(
                        value=100)),
                premium=has_entries(
                    price=has_properties(
                        value=200)))))
)))
def test_merge_segment(current, new, expected):
    if current:
        current_segment = create_segment(tariffs=current, display_t_code='plane')
    else:
        current_segment = None
    new_segment = create_segment(tariffs=new, display_t_code='plane')
    assert_that(merge_segments(current_segment, new_segment), expected)


@pytest.mark.dbuser
@pytest.mark.parametrize('current, other, t_type, is_better', (
    # если один из тарифов неизвестен, то побеждает другой
    (
        None,
        {'price': Price(200)},
        TransportType.objects.get(code='plane'),
        False
    ),
    (
        {'price': Price(200)},
        None,
        TransportType.objects.get(code='plane'),
        True
    ),
    # если один из тарифов меньше другого, то он побеждает
    (
        {'price': Price(100)},
        {'price': Price(200)},
        TransportType.objects.get(code='plane'),
        True
    ),
    (
        {'price': Price(200)},
        {'price': Price(100)},
        TransportType.objects.get(code='plane'),
        False
    ),
    # если
    # * оба тарифа одинаковые
    # * t_type==plane
    #
    # ,то побеждает тариф от компании
    (
        {'price': Price(100), 'from_company': True},
        {'price': Price(100), 'from_company': False},
        TransportType.objects.get(code='plane'),
        True
    ),
    (
        {'price': Price(100), 'from_company': False},
        {'price': Price(100), 'from_company': True},
        TransportType.objects.get(code='plane'),
        False
    ),
    # если
    # * оба тарифа одинаковые
    # * t_type==plane
    # * по компании не отсекли
    #
    # ,то побеждает первый ответивший
    (
        {'price': Price(100), 'from_company': False, 'query_time': 10},
        {'price': Price(100), 'from_company': False, 'query_time': 100},
        TransportType.objects.get(code='plane'),
        True
    ),
    (
        {'price': Price(100), 'from_company': False, 'query_time': 100},
        {'price': Price(100), 'from_company': False, 'query_time': 10},
        TransportType.objects.get(code='plane'),
        False
    ),
    (
        {'price': Price(100), 'from_company': True, 'query_time': 10},
        {'price': Price(100), 'from_company': True, 'query_time': 100},
        TransportType.objects.get(code='plane'),
        True
    ),
    (
        {'price': Price(100), 'from_company': True, 'query_time': 100},
        {'price': Price(100), 'from_company': True, 'query_time': 10},
        TransportType.objects.get(code='plane'),
        False
    ),
    # если случилось чудо
    # * оба тарифа одинаковые
    # * t_type==plane
    # * по компании не отсекли
    # * оба быстро ответили
    #
    # то берем текущий
    (
        {'price': Price(100), 'from_company': True, 'query_time': 100},
        {'price': Price(100), 'from_company': True, 'query_time': 100},
        TransportType.objects.get(code='plane'),
        True
    ),
    (
        {'price': Price(100), 'from_company': False, 'query_time': 100},
        {'price': Price(100), 'from_company': False, 'query_time': 100},
        TransportType.objects.get(code='plane'),
        True
    )
))
def test_get_better_class(current, other, t_type, is_better):
    better = get_better_class(current, other, t_type)
    if is_better:
        assert better is current
    else:
        assert better is other


def test_init_plane_tariff_queries():
    query = DaemonQuery()
    query_all_mocks = [mock.Mock(), mock.Mock()]
    query_all_mocks[0].query_all = mock.Mock(return_value='111')
    query_all_mocks[1].query_all = mock.Mock(return_value='222')
    with mock.patch.object(
        travel.rasp.morda_backend.morda_backend.tariffs.daemon.service, 'create_daemon_queries',
        return_value=query_all_mocks
    ) as mock_create_daemon_queries:
        response = init_plane_tariff_queries(query)

    mock_create_daemon_queries.assert_called_once()
    prepared_query = mock_create_daemon_queries.call_args_list[0][0][0]
    assert prepared_query.transport_types == [TransportType.get_plane_type()]

    assert_that(response, has_entry('qids', contains_inanyorder('111', '222')))


def test_create_poll_tariff_query():
    query = {'qid': '111', 'skip_partners': ['org1', 'org2'], 'yandexuid': 1234567890987654321}
    poll_query = create_poll_tariff_query(query)
    assert poll_query is not None
    assert poll_query.qid == query['qid']
    assert poll_query.skip_partners == query['skip_partners']
    assert poll_query.yandexuid == query['yandexuid']
