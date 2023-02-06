# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import hamcrest
import pytest

from common.models.currency import Price
from common.tester.factories import create_currency, create_station
from common.tester.transaction_context import transaction_fixture
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.wizards.wizard_lib.protobuf_models.thread_pb2 import TThread
from travel.rasp.wizards.wizard_lib.serialization.direction import DirectionQuery
from travel.rasp.wizards.wizard_lib.serialization.proto_direction import dump_segments
from travel.rasp.wizards.wizard_lib.serialization.limit import DEFAULT_SEGMENTS_LIMIT
from travel.rasp.wizards.wizard_lib.serialization.thread_express_type import ThreadExpressType
from travel.rasp.wizards.wizard_lib.tests_utils import make_dummy_segment

pytestmark = pytest.mark.dbuser


@pytest.fixture
@transaction_fixture
def rur(request):
    return create_currency(template_whole=u'%d руб.')


def make_query(**kwargs):
    kwargs.setdefault('experiment_flags', frozenset())
    return DirectionQuery(*(kwargs.get(field) for field in DirectionQuery._fields))


@replace_setting('MORDA_HOST_BY_TLD', {'ru': 'rasp.yandex.ru'})
@replace_setting('TOUCH_HOST_BY_TLD', {'ru': 't.rasp.yandex.ru'})
def test_dump_segments(rur):
    departure_station = create_station()
    arrival_station = create_station()
    proto_direction = dump_segments(
        segments=(
            make_dummy_segment(departure_station, arrival_station, price=Price(100), transport_subtype_id=123),
        ),
        query=make_query(departure_point=departure_station, arrival_point=arrival_station, language='ru')
    )
    expected_thread_path = (
        '/thread/some_thread_uid/?departure_from=2000-01-01+00%3A00%3A00&'
        'point_from={}&point_to={}&station_from={}&station_to={}'
        .format(departure_station.point_key, arrival_station.point_key, departure_station.id, arrival_station.id)
    )

    hamcrest.assert_that(proto_direction, hamcrest.has_properties(
        Segments=hamcrest.contains(
            hamcrest.has_properties(
                DepartureStationId=departure_station.id,
                ArrivalStationId=arrival_station.id,
                DepartureTimestamp=hamcrest.has_properties(Timestamp=946674000, UtcOffset=10800),
                ArrivalTimestamp=hamcrest.has_properties(Timestamp=946760400, UtcOffset=10800),
                Thread=hamcrest.has_properties(
                    Number='some_number', StartDate=730120, Title='some_title', TransportSubtypeId=123
                ),
                Price=hamcrest.has_properties(Value=100, CurrencyId=rur.id),
                Urls=hamcrest.has_properties(
                    Desktop='https://rasp.yandex.ru' + expected_thread_path,
                    Mobile='https://t.rasp.yandex.ru' + expected_thread_path
                )
            ),
        ),
        Total=1
    ))


def test_dump_segments_thread_express_type(rur):
    departure_station = create_station()
    arrival_station = create_station()
    proto_direction = dump_segments(
        segments=(
            make_dummy_segment(departure_station, arrival_station),
            make_dummy_segment(departure_station, arrival_station, thread_express_type=ThreadExpressType.EXPRESS),
        ),
        query=make_query(departure_point=departure_station, arrival_point=arrival_station, language='ru')
    )

    hamcrest.assert_that(proto_direction, hamcrest.has_properties(
        Segments=hamcrest.contains(
            hamcrest.has_properties(Thread=hamcrest.has_properties(ExpressType=TThread.NONE)),
            hamcrest.has_properties(Thread=hamcrest.has_properties(ExpressType=TThread.EXPRESS)),
        )
    ))


def test_dump_segments_price(rur):
    departure_station = create_station()
    arrival_station = create_station()
    proto_direction = dump_segments(
        segments=(
            make_dummy_segment(departure_station, arrival_station, price=Price(100)),
            make_dummy_segment(departure_station, arrival_station),

        ),
        query=make_query(departure_point=departure_station, arrival_point=arrival_station, language='ru')
    )

    assert proto_direction.Segments[0].HasField('Price')
    assert not proto_direction.Segments[1].HasField('Price')


def test_dump_segments_total(rur):
    departure_station = create_station()
    arrival_station = create_station()
    dummy_segments = [make_dummy_segment(departure_station, arrival_station)] * 50
    query = make_query(
        departure_point=departure_station,
        arrival_point=arrival_station,
        language='ru',
        limit=DEFAULT_SEGMENTS_LIMIT
    )
    proto_direction = dump_segments(segments=dummy_segments, query=query)

    assert len(proto_direction.Segments) == DEFAULT_SEGMENTS_LIMIT
    assert proto_direction.Total == 50
