# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest
from django.utils import translation
from hamcrest import has_entries, assert_that, anything

from common.models.factories import create_platform_translation
from common.models.geo import Country
from common.models.schedule import RThreadType
from common.models.transport import TransportType
from common.tester.factories import (
    create_station, create_rthread_segment, create_transport_model, create_transport_subtype,
    create_thread, create_deluxe_train, create_company, create_rtstation, create_supplier
)
from common.tester.testcase import TestCase
from common.utils.date import MSK_TZ, UTC_TZ, MSK_TIMEZONE, FuzzyDateTime

from travel.rasp.morda_backend.morda_backend.search.search.data_layer.backend import add_days_by_tz
from travel.rasp.morda_backend.morda_backend.serialization.segment import (
    RaspDBSearchMainSegmentSchema, SearchCompanySchema, RaspDBThreadSchema
)
from travel.rasp.morda_backend.morda_backend.serialization.segment_station import SegmentStationSchema
from travel.rasp.morda_backend.morda_backend.serialization.segment_transport import (
    TransportModelSchema, TransportSubTypeSchema, DeluxeTrainSchema
)


def _get_dump_result(schema, data):
    result, errors = schema.dump(data)
    assert not errors
    return result


class TestSegmentSchema(TestCase):
    def setUp(self):
        self.schema = RaspDBSearchMainSegmentSchema(context={'is_all_days_search': False})

    def test_dump_segment(self):
        segment = create_rthread_segment(thread=create_thread(translated_days_texts="""
            [{"ru": "ежедневно"}, {"ru": "ежедневно"}, {"ru": "ежедневно"}, {"ru": "ежедневно"}]
        """))
        add_days_by_tz([segment], [MSK_TZ], next_plan=None)
        result = _get_dump_result(self.schema, segment)
        assert_that(result, has_entries(
            title=anything(),
            arrival=segment.arrival.astimezone(UTC_TZ).isoformat(),
            departure=segment.departure.astimezone(UTC_TZ).isoformat(),
            duration=anything(),
            number=segment.thread.number,
            stationFrom=anything(),
            stationTo=anything(),
            transport=anything(),
            thread=anything(),
            company=anything(),
            isInterval=anything(),
            stops=anything(),
            url=anything(),
            daysByTimezone=has_entries({MSK_TIMEZONE: has_entries(text=anything())})
        ))

    def test_segment_title(self):
        segment = create_rthread_segment(thread=create_thread(schedule_v1=[
            [None, 0, {'title_ru': 'Киев', 'title_uk': 'Київ'}],
            [5, None, {'title_ru': 'Одесса', 'title_uk': 'Одеса'}]
        ]))
        with translation.override('uk'):
            result = _get_dump_result(self.schema, segment)
        assert result['title'] == 'Київ — Одеса'

    def test_dump_fuzzy_segment(self):
        t_type = TransportType.get_train_type()
        departure = datetime(2016, 1, 1, tzinfo=MSK_TZ)
        arrival = datetime(2016, 1, 2, tzinfo=MSK_TZ)

        segment = create_rthread_segment(t_type=t_type, departure=departure, arrival=arrival)
        result = _get_dump_result(self.schema, segment)
        assert result['isFuzzyFrom'] is None
        assert result['isFuzzyTo'] is None

        fuzzy_segment = create_rthread_segment(
            t_type=t_type,
            departure=FuzzyDateTime(departure), arrival=FuzzyDateTime(arrival), is_fuzzy_from=True, is_fuzzy_to=True
        )
        result = _get_dump_result(self.schema, fuzzy_segment)
        assert 'tariffsKeys' in result
        assert result['isFuzzyFrom']
        assert result['isFuzzyTo']

    def test_dump_segment_should_prepare_data(self):
        """
        При дампе должны выполняться следующие действия:
          - добавляем transport и tariffs_keys
          - приводим arrival и departure к UTC
          - если departure is None - сегмент должен помечаться как интервальный,
            при этом departure должен сериализоваться в None
        """
        t_type = TransportType.get_train_type()
        t_model = create_transport_model()
        departure = datetime(2016, 1, 1, tzinfo=MSK_TZ)
        arrival = datetime(2016, 1, 2, tzinfo=MSK_TZ)
        segment = create_rthread_segment(
            number='', t_type=t_type, t_model=t_model, departure=departure, arrival=arrival
        )

        result = _get_dump_result(self.schema, segment)
        assert result['arrival'] == arrival.astimezone(UTC_TZ).isoformat()
        assert result['departure'] == departure.astimezone(UTC_TZ).isoformat()
        assert 'transport' in result
        assert not result['isInterval']
        assert 'tariffsKeys' in result

        interval_segment = create_rthread_segment(
            number='', t_type=t_type, t_model=t_model, departure=None, arrival=None, duration=None
        )
        result = _get_dump_result(self.schema, interval_segment)
        assert result['isInterval']
        assert result['departure'] is None

    def test_build_tariffs_keys(self):
        t_type = TransportType.get_train_type()
        segment = create_rthread_segment(number='abc 123', t_type=t_type, departure=datetime(2016, 1, 1))
        assert 'daemon abc-123 0101' in RaspDBSearchMainSegmentSchema(context={'is_all_days_search': False}).build_tariffs_keys(segment)

        segment = create_rthread_segment(number='abc 123', t_type=t_type, departure=None)
        all_days_keys = RaspDBSearchMainSegmentSchema(context={'is_all_days_search': True}).build_tariffs_keys(segment)
        assert 'daemon abc-123 0101' not in all_days_keys

    def test_train_purchase_numbers(self):
        segment = create_rthread_segment(thread=create_thread(t_type=TransportType.SUBURBAN_ID))
        segment.train_purchase_numbers = ['666', '777']
        result, errors = RaspDBSearchMainSegmentSchema().dump(segment)
        assert not errors
        assert result['hasTrainTariffs']
        assert all(any(key in tk for tk in result['tariffsKeys'])
                   for key in ['train 665', 'train 666', 'train 777', 'train 778'])

    def test_build_station_from(self):
        lang = 'uk'
        station = create_station(title_ru='Екатеринбург', country_id=Country.RUSSIA_ID, time_zone='Asia/Yekaterinburg')
        rtstation = create_rtstation(station=station, thread=create_thread(), platform='новая платф.')
        create_platform_translation(platform='новая платф.', platform_uk='нова платф.')
        segment = create_rthread_segment(station_from=station, rtstation_from=rtstation,
                                         t_type=TransportType.objects.get(id=TransportType.SUBURBAN_ID))

        with translation.override(lang):
            station_json = self.schema.build_station_from(segment)
            assert has_entries(
                title='Екатеринбург',
                platform='нова платф.',
                railwayTimezone='Europe/Moscow',
                timezone='Asia/Yekaterinburg'
            ).matches(station_json)

    def test_build_station_to(self):
        lang = 'uk'
        station = create_station(title_ru='Екатеринбург', country_id=Country.RUSSIA_ID,
                                 time_zone='Asia/Yekaterinburg')
        rtstation = create_rtstation(station=station, thread=create_thread(), platform='новая платф.')
        create_platform_translation(platform='новая платф.', platform_uk='нова платф.')
        segment = create_rthread_segment(station_to=station, rtstation_to=rtstation,
                                         t_type=TransportType.objects.get(id=TransportType.SUBURBAN_ID))

        with translation.override(lang):
            station_json = self.schema.build_station_to(segment)
            assert has_entries(
                title='Екатеринбург',
                platform='нова платф.',
                railwayTimezone='Europe/Moscow',
                timezone='Asia/Yekaterinburg'
            ).matches(station_json)


DEFAULT_TITLES = {'title_en': 'Test', 'title_ru': 'Тест'}


@pytest.mark.dbuser
def test_station_schema(with_language):
    """ Параметр title должен быть локализован """
    schema = SegmentStationSchema()
    station = create_station(popular_title_en='test popular title', **DEFAULT_TITLES)
    with_language('en')
    result = _get_dump_result(schema, station)
    assert result['title'] == DEFAULT_TITLES['title_en']
    assert result['popularTitle'] == 'test popular title'


@pytest.mark.dbuser
def test_transport_model_schema(with_language):
    """ Параметр title должен быть локализован """
    schema = TransportModelSchema()
    t_model = create_transport_model(title='Тест', title_en='Test')
    with_language('en')
    result = _get_dump_result(schema, t_model)
    assert result['title'] == 'Test'


@pytest.mark.dbuser
def test_transport_sub_type_schema(with_language):
    """ Параметр title должен быть локализован """
    data = DEFAULT_TITLES
    data['t_type_id'] = 10
    schema = TransportSubTypeSchema()
    t_sub_type = create_transport_subtype(**data)
    with_language('en')
    result = _get_dump_result(schema, t_sub_type)
    assert result['title'] == DEFAULT_TITLES['title_en']


@pytest.mark.dbuser
def test_thread_schema_title(with_language):
    """ Параметр title должен быть локализован. """
    schema = RaspDBThreadSchema()
    thread = create_thread()
    with_language('en')
    result = _get_dump_result(schema, thread)
    assert result['title'] == thread.L_title(lang='en')


@pytest.mark.dbuser
def test_thread_schema_deluxe_train():
    """
    Если фирменного поезда нет - не должно быть аттрибута 'deluxeTrain'
    """
    schema = RaspDBThreadSchema()
    thread = create_thread()
    result = _get_dump_result(schema, thread)
    assert 'deluxeTrain' not in result


@pytest.mark.dbuser
def test_thread_schema_express():
    """
    Если тип экспресса у RThread - "express" - признак is_express должен быть True
    """
    schema = RaspDBThreadSchema()
    thread = create_thread()
    result = _get_dump_result(schema, thread)
    assert result['isExpress'] is False

    thread = create_thread(express_type='express')
    result = _get_dump_result(schema, thread)
    assert result['isExpress'] is True


@pytest.mark.dbuser
@pytest.mark.parametrize('displace_yabus, t_type, expected', [
    (True, TransportType.BUS_ID, True),
    (False, TransportType.BUS_ID, False),
    (True, TransportType.HELICOPTER_ID, None)
])
def test_thread_displace_yabus(displace_yabus, t_type, expected):
    """
    Проверяем у поставщика признак displace_yabus для автобусов
    """
    schema = RaspDBThreadSchema()
    thread = create_thread(supplier=create_supplier(displace_yabus=displace_yabus), t_type=t_type)
    result = _get_dump_result(schema, thread)
    assert result['displaceYabus'] is expected


@pytest.mark.dbuser
def test_thread_schema_aeroexpress():
    """
    Если тип экспресса у RThread - "aeroexpress" - признак is_aeroexpress должен быть True
    """
    schema = RaspDBThreadSchema()
    thread = create_thread()
    result = _get_dump_result(schema, thread)
    assert result['isAeroExpress'] is False

    thread = create_thread(express_type='aeroexpress')
    result = _get_dump_result(schema, thread)
    assert result['isAeroExpress'] is True


@pytest.mark.dbuser
def test_thread_schema_is_basic():
    schema = RaspDBThreadSchema()
    thread = create_thread(type=RThreadType.BASIC_ID)
    result = _get_dump_result(schema, thread)
    assert result['isBasic']

    thread = create_thread(type=RThreadType.CANCEL_ID)
    result = _get_dump_result(schema, thread)
    assert not result['isBasic']


@pytest.mark.dbuser
@pytest.mark.parametrize('comment', ('', u'Комментарий к нитке'))
def test_thread_schema_comment(comment):
    schema = RaspDBThreadSchema()
    thread = create_thread(type=RThreadType.BASIC_ID, comment=comment)
    result = _get_dump_result(schema, thread)
    assert result['comment'] == comment


@pytest.mark.dbuser
def test_deluxe_train_schema(with_language):
    """
    Аттрибуты title и short_title должны быть локализованы.
    Аттрибут deluxe должен быть превращен в isDeluxe.
    """
    schema = DeluxeTrainSchema()
    train = create_deluxe_train(deluxe=True)
    with_language('en')
    result = _get_dump_result(schema, train)
    assert has_entries(
        id=train.id,
        title=train.L_title(lang='en'),
        shortTitle=train.L_title_short(lang='en'),
        isDeluxe=True
    ).matches(result)


@pytest.mark.dbuser
def test_company_schema(with_language):
    schema = SearchCompanySchema()
    company = create_company(id=456, url='company.net', short_title_en='test short title', **DEFAULT_TITLES)
    with_language('en')
    result = _get_dump_result(schema, company)
    assert result['id'] == 456
    assert result['title'] == DEFAULT_TITLES['title_en']
    assert result['shortTitle'] == 'test short title'
    assert result['url'] == 'company.net'


@pytest.mark.dbuser
def test_thread_canonical_uid():
    schema = RaspDBThreadSchema()
    thread = create_thread(canonical_uid='123')
    result = _get_dump_result(schema, thread)
    assert result['canonicalUid'] == '123'

    thread = create_thread()
    result = _get_dump_result(schema, thread)
    assert result['canonicalUid'] is None


@pytest.mark.dbuser
def test_thread_cancels():
    schema = RaspDBThreadSchema()
    stations = [create_station(popular_title_ru_genitive='pt_gen_{}'.format(i)) for i in range(4)]
    thread = create_thread(
        schedule_v1=[
            [None, 0, stations[0]],
            [10, 15, stations[1]],
            [25, 30, stations[2]],
            [40, None, stations[3]]
        ])
    result = _get_dump_result(schema, thread)
    assert not ('cancelled' in result or 'cancelledSegments' in result)

    thread.fully_cancelled = False
    result = _get_dump_result(schema, thread)
    assert result['cancelled'] is False

    thread.fully_cancelled = True
    result = _get_dump_result(schema, thread)
    assert result['cancelled'] is True

    thread.cancelled_segments = [{
        'station_from': stations[0],
        'station_to': stations[-1]
    }]
    result = _get_dump_result(schema, thread)
    assert result['cancelledSegments'] == [{
        'fromTitleGenitive': stations[0].L_popular_title(case='genitive', fallback=True),
        'toTitleGenitive': stations[-1].L_popular_title(case='genitive', fallback=True)
    }]
