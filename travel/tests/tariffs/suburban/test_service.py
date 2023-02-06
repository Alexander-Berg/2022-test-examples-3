# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from itertools import product

import pytest
from hamcrest import contains, assert_that

from common.models.factories import create_tariff_type, create_aeroex_tariff, create_tariff_group
from common.models.tariffs import AeroexTariff, TariffType
from common.models.transport import TransportType
from common.tester.factories import create_settlement, create_station, create_rthread_segment, create_thread
from travel.rasp.morda_backend.morda_backend.tariffs.suburban.service import (
    get_suburban_tariffs, iter_sorted_tariffs_by_direction, make_suburban_tariff_key, make_suburban_segment_keys
)
from travel.rasp.morda_backend.morda_backend.tariffs.train.base.utils import make_suburban_express_keys


@pytest.mark.dbuser
def test_get_suburban_tariffs():
    station_from = create_station()
    station_to = create_station()
    create_tariff = create_aeroex_tariff.mutate(station_from=station_from, station_to=station_to)

    group_a = create_tariff_group(title='Group A', id=101)
    group_b = create_tariff_group(title='Group B', id=102)
    group_c = create_tariff_group(title='Group C', id=103)

    usual = TariffType.USUAL_CATEGORY
    special = TariffType.SPECIAL_CATEGORY
    usual_a = create_tariff(tariff=1, type={'order': 1, 'category': usual, '__': {'tariff_groups': [group_a]}})
    special_a = create_tariff(tariff=2, type={'order': 2, 'category': special, '__': {'tariff_groups': [group_a]}})
    usual_b = create_tariff(tariff=3, type={'order': 3, 'category': usual, '__': {'tariff_groups': [group_b]}})
    special_b = create_tariff(tariff=4, type={'order': 4, 'category': special, '__': {'tariff_groups': [group_b]}})
    create_tariff(tariff=5, type={'order': 5, 'category': special, '__': {'tariff_groups': [group_c]}})

    tariffs, groups = get_suburban_tariffs(station_from, station_to)

    # В тарифах должны быть только обычные тарифы, причем в правильном порядке.
    assert len(tariffs) == 2
    assert tariffs[0]['classes']['suburban'] == usual_a
    assert tariffs[1]['classes']['suburban'] == usual_b

    # В тарифах в категориях должны быть только тарифы из тех же групп, что и основные
    assert_that(tariffs[0]['suburban_categories'][TariffType.USUAL_CATEGORY], contains(usual_a))
    assert_that(tariffs[0]['suburban_categories'][TariffType.SPECIAL_CATEGORY], contains(special_a))
    assert_that(tariffs[1]['suburban_categories'][TariffType.USUAL_CATEGORY], contains(usual_b))
    assert_that(tariffs[1]['suburban_categories'][TariffType.SPECIAL_CATEGORY], contains(special_b))

    # В группах должны быть только группы с основными тарифами. И порядок групп должен быть по id.
    assert len(groups) == 2
    assert_that(groups[0]['categories'][TariffType.USUAL_CATEGORY], contains(usual_a))
    assert_that(groups[0]['categories'][TariffType.SPECIAL_CATEGORY], contains(special_a))
    assert_that(groups[1]['categories'][TariffType.USUAL_CATEGORY], contains(usual_b))
    assert_that(groups[1]['categories'][TariffType.SPECIAL_CATEGORY], contains(special_b))


@pytest.mark.dbuser
def test_replace_tariff():
    station_from = create_station()
    station_to = create_station()
    station_far = create_station()
    group = create_tariff_group()

    type_base = create_tariff_type(
        code='b', title='Base', order=1, is_main=False,
        category=TariffType.USUAL_CATEGORY, __=dict(tariff_groups=[group])
    )
    type_cppk = create_tariff_type(
        code='c', title='CPPK', order=2, is_main=True,
        category=TariffType.USUAL_CATEGORY, __=dict(tariff_groups=[group])
    )
    type_troyka = create_tariff_type(
        code='t', title='Troyka', order=3, is_main=True,
        category=TariffType.USUAL_CATEGORY, __=dict(tariff_groups=[group])
    )

    create_aeroex_tariff(
        station_from=station_from, station_to=station_to, tariff=20, type=type_base, replace_tariff_type=type_cppk
    )
    create_aeroex_tariff(station_from=station_from, station_to=station_to, tariff=10, type=type_troyka)
    create_aeroex_tariff(station_from=station_from, station_to=station_far, tariff=30, type=type_base)

    tariffs, _ = get_suburban_tariffs(station_from, station_to)

    assert len(tariffs) == 2
    categories = tariffs[0]['suburban_categories'][TariffType.USUAL_CATEGORY]
    assert len(categories) == 2

    assert categories[0].type == type_base
    assert categories[0].replace_tariff_type == type_cppk
    assert categories[0].price.value == 20

    assert categories[1].type == type_troyka
    assert categories[1].replace_tariff_type is None
    assert categories[1].price.value == 10

    tariffs, _ = get_suburban_tariffs(station_from, station_far)

    assert len(tariffs) == 1
    categories = tariffs[0]['suburban_categories'][TariffType.USUAL_CATEGORY]
    assert len(categories) == 1

    assert categories[0].type == type_base
    assert categories[0].replace_tariff_type is None
    assert categories[0].price.value == 30


@pytest.mark.dbuser
def test_dont_use_value_of_suburban_search_for_groups():
    """RASPFRONT-2135: Не учитывать галку «В поиске электричек» для верхнего блока про цены электричек"""
    tariff = create_aeroex_tariff(suburban_search=False, type={'category': TariffType.USUAL_CATEGORY})
    tariffs, groups = get_suburban_tariffs(tariff.station_from, tariff.station_to)

    assert len(groups) == len(tariffs) == 1


@pytest.mark.dbuser
@pytest.mark.parametrize('segment_t_type, expected_has_tariff', (
    ('suburban', True),
    ('train', False),
    ('bus', False),
))
def test_make_suburban_key(segment_t_type, expected_has_tariff):
    """
    Проверяем генерацию ключей для сегмента и тарифа.
    1. Ключ генерируется только для нужных сегментов.
    2. Ключ для сегмента и ключ для тарифа совпадают.
    """
    station_from = create_station()
    station_to = create_station()
    segment = create_rthread_segment(station_from=station_from, station_to=station_to,
                                     thread=create_thread(t_type=segment_t_type))
    tariff_key = make_suburban_tariff_key(station_from.id, station_to.id, TariffType.DEFAULT_ID)
    segment_has_tariff = tariff_key in make_suburban_segment_keys(segment)

    assert segment_has_tariff == expected_has_tariff


@pytest.mark.dbuser
def test_make_suburban_key_with_express():
    """
    Проверяем наличие всех ключей для электричек-экспресов с дублями из поездов
    """
    station_from = create_station()
    station_to = create_station()
    segment = create_rthread_segment(station_from=station_from, station_to=station_to,
                                     thread=create_thread(t_type='suburban'), train_purchase_numbers=['666'])
    tariff_key = make_suburban_tariff_key(station_from.id, station_to.id, TariffType.DEFAULT_ID)
    express_keys = make_suburban_express_keys(segment.departure, ['666'])
    segment_keys = make_suburban_segment_keys(segment)

    assert tariff_key in segment_keys
    assert all(k in segment_keys for k in express_keys)


@pytest.mark.dbuser
def test_get_suburban_tariffs_usual():
    station_from = create_station()
    station_to = create_station()

    # в направлениях без обычного тарифа нет результата
    create_aeroex_tariff(
        station_from=station_from, station_to=station_to,
        type=TariffType.objects.create(code='special code', category=TariffType.SPECIAL_CATEGORY)
    )
    tariffs, _groups = get_suburban_tariffs(station_from, station_to)
    assert not tariffs

    # в направлениях с обычным тарифом есть результаты
    create_aeroex_tariff(
        station_from=station_from, station_to=station_to,
        type=TariffType.objects.create(code='usual code', category=TariffType.USUAL_CATEGORY)
    )
    tariffs, _groups = get_suburban_tariffs(station_from, station_to)
    assert tariffs


@pytest.mark.dbuser
def test_get_suburban_tariffs_keys():
    settlement_from = create_settlement()
    stations_from = [
        create_station(settlement=settlement_from, t_type=TransportType.TRAIN_ID),
        create_station(settlement=settlement_from, t_type=TransportType.TRAIN_ID)
    ]
    settlement_to = create_settlement()
    stations_to = [
        create_station(settlement=settlement_to, t_type=TransportType.TRAIN_ID),
        create_station(settlement=settlement_to, t_type=TransportType.TRAIN_ID)
    ]
    tariff_type = TariffType.objects.get(code='etrain')
    for station_from, station_to in product(stations_from, stations_to):
        AeroexTariff.objects.create(
            station_from=station_from, station_to=station_to,
            type=tariff_type, precalc=False,
            tariff=10, suburban_search=False, reverse=False
        )

    # station - station
    tariffs, groups = get_suburban_tariffs(stations_from[0], stations_to[0])
    keys = [tariff['key'] for tariff in tariffs]
    expected_keys = [make_suburban_tariff_key(stations_from[0].id, stations_to[0].id, tariff_type.id)]
    assert len(keys) == len(expected_keys)
    assert set(keys) == set(expected_keys)

    # station - settlement
    tariffs, groups = get_suburban_tariffs(stations_from[0], settlement_to)
    keys = [tariff['key'] for tariff in tariffs]
    expected_keys = [make_suburban_tariff_key(station_from.id, station_to.id, tariff_type.id)
                     for station_from, station_to in product([stations_from[0]], stations_to)]
    assert len(keys) == len(expected_keys)
    assert set(keys) == set(expected_keys)

    # settlement - station
    tariffs, groups = get_suburban_tariffs(settlement_from, stations_to[0])
    keys = [tariff['key'] for tariff in tariffs]
    expected_keys = [make_suburban_tariff_key(station_from.id, station_to.id, tariff_type.id)
                     for station_from, station_to in product(stations_from, [stations_to[0]])]
    assert len(keys) == len(expected_keys)
    assert set(keys) == set(expected_keys)

    # settlement - settlement
    tariffs, groups = get_suburban_tariffs(settlement_from, settlement_to)
    keys = [tariff['key'] for tariff in tariffs]
    expected_keys = [make_suburban_tariff_key(station_from.id, station_to.id, tariff_type.id)
                     for station_from, station_to in product(stations_from, stations_to)]
    assert len(keys) == len(expected_keys)
    assert set(keys) == set(expected_keys)


@pytest.mark.dbuser
def test_iter_tariffs_by_direction_directions():
    station_from = create_station()
    station_to = create_station()
    forward_price = 42
    forward_type = TariffType.objects.get(code='etrain')
    tariff = AeroexTariff.objects.create(
        station_from=station_from, station_to=station_to,
        type=forward_type, precalc=False,
        tariff=forward_price, suburban_search=False, reverse=False
    )
    any_direction_price = 100500
    any_direction_type = TariffType.objects.get(code='express')
    any_tariff = AeroexTariff.objects.create(
        station_from=station_from, station_to=station_to,
        type=any_direction_type, precalc=False,
        tariff=any_direction_price, suburban_search=False, reverse=True
    )
    forward_direction = (station_from.id, station_to.id)
    backward_direction = (station_to.id, station_from.id)

    assert_that(
        list(iter_sorted_tariffs_by_direction(station_from, station_to)),
        contains((forward_direction, [tariff, any_tariff]))
    )

    assert_that(
        list(iter_sorted_tariffs_by_direction(station_to, station_from)),
        contains((backward_direction, [any_tariff]))
    )


@pytest.mark.dbuser
def test_iter_tariffs_by_direction_precalc_priority():
    station_from = create_station()
    station_to = create_station()
    tariff_type = TariffType.objects.create(code='TariffType code 0', order=0)

    # предрасчитанный тариф попадает в результат
    precalc_tariff = create_aeroex_tariff(
        station_from=station_from, station_to=station_to, type=tariff_type, precalc=True
    )

    assert list(iter_sorted_tariffs_by_direction(station_from, station_to)) == [
        ((station_from.id, station_to.id), [precalc_tariff])
    ]

    # ручной тариф другого типа тоже попадает
    other_type_tariff = create_aeroex_tariff(
        station_from=station_from, station_to=station_to,
        type=TariffType.objects.create(code='TariffType code 1', order=1)
    )

    assert list(iter_sorted_tariffs_by_direction(station_from, station_to)) == [
        ((station_from.id, station_to.id), [precalc_tariff, other_type_tariff])
    ]

    # ручной тариф того же типа заменяет предрасчитанный
    tariff = create_aeroex_tariff(
        station_from=station_from, station_to=station_to, type=tariff_type
    )

    assert list(iter_sorted_tariffs_by_direction(station_from, station_to)) == [
        ((station_from.id, station_to.id), [tariff, other_type_tariff])
    ]


@pytest.mark.dbuser
def test_iter_tariffs_by_direction_order():
    station_from = create_station()
    station_to = create_station()

    # тарифы сортируются по order в типе
    tariff_1 = create_aeroex_tariff(
        station_from=station_from, station_to=station_to,
        type=TariffType.objects.create(code='TariffType code 1', order=1)
    )
    tariff_2 = create_aeroex_tariff(
        station_from=station_from, station_to=station_to,
        type=TariffType.objects.create(code='TariffType code 2', order=2)
    )

    assert list(iter_sorted_tariffs_by_direction(station_from, station_to)) == [
        ((station_from.id, station_to.id), [tariff_1, tariff_2])
    ]
