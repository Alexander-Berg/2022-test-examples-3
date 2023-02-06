# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import timedelta, datetime, date

import mock
import pytest
from hamcrest import assert_that, contains_inanyorder, contains, has_properties

from common.models.schedule import RThread, RThreadType
from common.models.transport import TransportType
from common.tester.factories import create_rthread_segment, create_thread, create_company, create_station
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting
from common.utils.date import RunMask, MSK_TZ, UTC_TZ
from travel.rasp.library.python.common23.date.environment import now_aware
from travel.rasp.morda_backend.morda_backend.search.merge_trains import (
    _choose_main_segment, _create_meta_train, _is_through_train, _parse_train_number,
    fill_train_numbers, get_possible_numbers, make_meta_trains, make_grouped_trains,
    SUBSEGMENTS_MAX_MINUTES_DIFFERENCE, TrainNumber
)
from travel.rasp.morda_backend.morda_backend.search.search.data_layer.backend import add_days_by_tz
from travel.rasp.morda_backend.morda_backend.tariffs.train.base.utils import make_segment_train_keys


pytestmark = [pytest.mark.dbuser]
create_thread = create_thread.mutate(t_type=TransportType.TRAIN_ID)


def test_is_through_train():
    segment = create_rthread_segment(thread=create_thread(type=RThreadType.THROUGH_TRAIN_ID))
    assert _is_through_train(segment)


def test_get_train_number():
    assert _parse_train_number('303М') == TrainNumber('303', 'М')
    assert _parse_train_number('303МЯ') == TrainNumber('303', 'МЯ')


def tests_choose_main_segment():
    now = now_aware()
    segment1 = create_rthread_segment(
        thread=create_thread(type=RThreadType.THROUGH_TRAIN_ID),
        number='303М', departure=now, arrival=now + timedelta(2)
    )
    segment2 = create_rthread_segment(thread=create_thread(), number='303Я')
    segment3 = create_rthread_segment(thread=create_thread(
        type=RThreadType.THROUGH_TRAIN_ID),
        number='303МЯ', departure=now, arrival=now + timedelta(1)
    )
    assert _choose_main_segment([segment1, segment2, segment3]) == segment2
    assert _choose_main_segment([segment1, segment3]) == segment3


def test_create_meta_train():
    company1 = create_company(id=556, short_title_en='test short title1')
    company2 = create_company(id=557, short_title_en='test short title2')
    segments = [
        create_rthread_segment(thread=create_thread(), number='303М', company=company1),
        create_rthread_segment(thread=create_thread(), number='303Я', company=company2),
        create_rthread_segment(thread=create_thread(), number='303МЯ', company=company1)
    ]
    fill_train_numbers(segments)
    meta_train = _create_meta_train(segments[1], segments)
    assert meta_train.number == '303МЯ'
    assert meta_train.letters == {'М', 'Я', 'МЯ'}
    assert meta_train.companies == {company1, company2}
    assert meta_train.company == company2
    assert segments[1].number == '303Я'
    assert segments[1].company == company2


def test_create_meta_train_mrja():
    segments = [create_rthread_segment(thread=create_thread(), number='303МЯ'),
                create_rthread_segment(thread=create_thread(), number='303РЯ'),
                create_rthread_segment(thread=create_thread(), number='303Я')]
    fill_train_numbers(segments)
    meta_train = _create_meta_train(segments[1], segments)
    assert meta_train.number == '303МРЯ'
    assert meta_train.letters == {'Я', 'МЯ', 'РЯ', 'МРЯ'}
    assert meta_train.companies == set()


def test_make_meta_trains():
    now = now_aware()
    through_segment = create_rthread_segment(
        thread=create_thread(type=RThreadType.THROUGH_TRAIN_ID),
        number='303М', arrival=now + timedelta(hours=1)
    )
    main_segment = create_rthread_segment(
        thread=create_thread(),
        number='303Я', arrival=now + timedelta(hours=2)
    )
    segments = [
        through_segment,
        main_segment,
        create_rthread_segment(
            thread=create_thread(),
            number='305Я', arrival=now + timedelta(hours=3)
        )
    ]
    fill_train_numbers(segments)
    segments = make_meta_trains(segments)
    assert len(segments) == 2
    assert segments[-1].number == '303МЯ'
    assert segments[-1].letters == {'М', 'Я', 'МЯ'}
    assert segments[-1].companies == set()
    assert segments[-1].sub_segments == [through_segment, main_segment]
    assert segments[-1].min_arrival == now + timedelta(hours=1)
    assert segments[-1].max_arrival == now + timedelta(hours=2)
    assert_that(
        make_segment_train_keys(segments[-1]),
        contains_inanyorder(
            'train 303МЯ 20150101_1000',
            'train 304МЯ 20150101_1000',
            'train 303М 20150101_1000',
            'train 304М 20150101_1000',
            'train 303Я 20150101_1000',
            'train 304Я 20150101_1000'
        )
    )


def test_get_possible_numbers():
    segment = create_rthread_segment(
        thread=create_thread(),
        number='303МЯ', letters={'М', 'Я', 'МЯ'},
        train_number=_parse_train_number('303МЯ')
    )
    assert_that(get_possible_numbers(segment), contains_inanyorder('303МЯ', '303М', '303Я', 'НомерНитки'))


def test_get_possible_numbers_none():
    segment = create_rthread_segment(
        thread=create_thread(),
        number='303МЯ', letters={'М', 'Я', 'МЯ'},
        train_number=None
    )
    assert_that(get_possible_numbers(segment), contains_inanyorder('НомерНитки'))
    segment = create_rthread_segment(
        thread=create_thread(),
        number='303МЯ', letters=None,
        train_number=_parse_train_number('303МЯ')
    )
    assert_that(get_possible_numbers(segment), contains_inanyorder('НомерНитки'))


@replace_dynamic_setting('TRAIN_PURCHASE_P2_TRAIN_NUMBERS_MAP', {'7003': ['803И', '803Х']})
def test_get_possible_numbers_cppk():
    segment = create_rthread_segment(thread=create_thread(number='803И'))
    assert_that(get_possible_numbers(segment), contains_inanyorder('7003', '803И'))


@replace_now('2021-06-10')
def test_make_grouped_trains():
    station_from = create_station()
    station_to = create_station()
    another_station_from = create_station()
    another_station_to = create_station()

    # 4 дня за два месяца, 6 дней всего
    normal_days = (
        RunMask.range(datetime(2021, 6, 1), datetime(2021, 6, 3), include_end=True) |
        RunMask(days=[datetime(2021, 7, 1), datetime(2021, 8, 1), datetime(2022, 1, 1)])
    )

    # 5 дней за два месяца
    greater_2_month_days = RunMask.range(datetime(2021, 6, 1), datetime(2021, 6, 5), include_end=True)

    # 4 дня за два месяца, 7 дней всего
    greater_days = (
        RunMask.range(datetime(2021, 6, 1), datetime(2021, 6, 4), include_end=True) |
        RunMask.range(datetime(2022, 1, 1), datetime(2022, 1, 3), include_end=True)
    )

    twin_days_1 = RunMask.range(datetime(2021, 6, 28), datetime(2021, 6, 30), include_end=True)

    early_days = RunMask(days=[datetime(2021, 5, 29)])

    merged_days_1 = greater_2_month_days | twin_days_1

    twin_days_2 = (
        RunMask.range(datetime(2021, 9, 1), datetime(2021, 9, 3), include_end=True) |
        RunMask(days=[datetime(2021, 9, 29), datetime(2022, 1, 1), datetime(2022, 1, 31)])
    )

    merged_days_2 = normal_days | twin_days_2

    # Не поезд
    bus_segment = create_rthread_segment(
        thread=create_thread(t_type=TransportType.BUS_ID),
    )

    # Уходит с другой станции
    segment_another_from = create_rthread_segment(
        thread=create_thread(number='101Y', year_days=normal_days),
        station_from=another_station_from,
        station_to=station_to,
        start_date=date(2021, 6, 1)
    )

    # Приходит на другую станцию
    segment_another_to = create_rthread_segment(
        thread=create_thread(number='101Y', year_days=normal_days),
        station_from=station_from,
        station_to=another_station_to,
        start_date=date(2021, 6, 1)
    )

    # Другой номер поезда
    segment_another_number = create_rthread_segment(
        thread=create_thread(number='102Y', year_days=normal_days),
        station_from=station_from,
        station_to=station_to,
        start_date=date(2021, 6, 1)
    )

    dt = datetime(2021, 6, 10, 10, 0, tzinfo=UTC_TZ)
    # Главный сегмент 1-й группы
    main_segment_1 = create_rthread_segment(
        thread=create_thread(number='101Y', year_days=greater_2_month_days),
        station_from=station_from,
        station_to=station_to,
        departure=dt + timedelta(minutes=SUBSEGMENTS_MAX_MINUTES_DIFFERENCE - 2),
        start_date=date(2021, 6, 1)
    )

    # Другие сегменты 1-й группы
    segment_1_1 = create_rthread_segment(
        thread=create_thread(number='101Y', year_days=greater_days),
        station_from=station_from,
        station_to=station_to,
        departure=dt,
        start_date=date(2021, 6, 1)
    )

    segment_1_2 = create_rthread_segment(
        thread=create_thread(number='101Z', year_days=normal_days),
        station_from=station_from,
        station_to=station_to,
        departure=dt + timedelta(minutes=2 * SUBSEGMENTS_MAX_MINUTES_DIFFERENCE - 4),
        start_date=date(2021, 6, 1)
    )

    # Сегмент с другим именем
    segment_1_3 = create_rthread_segment(
        thread=create_thread(number='101Y', year_days=early_days, title='another'),
        station_from=station_from,
        station_to=station_to,
        departure=dt + timedelta(minutes=SUBSEGMENTS_MAX_MINUTES_DIFFERENCE - 2),
        start_date=date(2021, 5, 29)
    )

    # Сегмент, склеиваемый с main_segment_1
    main_segment_1_twin = create_rthread_segment(
        thread=create_thread(number='101Y', year_days=twin_days_1),
        station_from=station_from,
        station_to=station_to,
        departure=dt + timedelta(minutes=SUBSEGMENTS_MAX_MINUTES_DIFFERENCE - 2),
        start_date=date(2021, 6, 28)
    )

    # Главный сегмент 2-й группы
    main_segment_2 = create_rthread_segment(
        thread=create_thread(number='101Y', year_days=greater_days),
        station_from=station_from,
        station_to=station_to,
        departure=dt + timedelta(minutes=4 * SUBSEGMENTS_MAX_MINUTES_DIFFERENCE + 3),
        start_date=date(2021, 6, 1)
    )

    # Другие сегменты 2-й группы
    segment_2_1 = create_rthread_segment(
        thread=create_thread(number='101Z', year_days=normal_days),
        station_from=station_from,
        station_to=station_to,
        departure=dt + timedelta(minutes=3 * SUBSEGMENTS_MAX_MINUTES_DIFFERENCE + 5),
        start_date=date(2021, 6, 1)
    )

    # Сегмент, склеиваемый с main_segment_2
    segment_2_1_twin = create_rthread_segment(
        thread=create_thread(number='101Z', year_days=twin_days_2),
        station_from=station_from,
        station_to=station_to,
        departure=dt + timedelta(minutes=3 * SUBSEGMENTS_MAX_MINUTES_DIFFERENCE + 5),
        start_date=date(2021, 9, 1)
    )

    segments = make_grouped_trains([
        bus_segment, segment_another_from, segment_another_to, segment_another_number,
        segment_1_1, main_segment_1, segment_1_3, main_segment_1_twin, segment_1_2,  # Упорядочены по возрастанию времени отправления
        segment_2_1, main_segment_2, segment_2_1_twin
    ])

    assert len(segments) == 6

    assert_that(segments, contains(
        bus_segment,
        segment_another_from,
        segment_another_to,
        segment_another_number,
        has_properties({
            'thread': has_properties({'id': main_segment_1.thread.id}),
            'sub_segments': contains(
                segment_1_1,
                segment_1_3,
                segment_1_2
            )
        }),
        has_properties({
            'thread': has_properties({'id': main_segment_2.thread.id}),
            'sub_segments': contains(
                segment_2_1
            )
        }),
    ))

    assert main_segment_1.thread.year_days == str(merged_days_1)
    assert main_segment_1.force_recalculate_days_text
    assert main_segment_1_twin.is_twin_segment
    assert main_segment_1.start_date == date(2021, 6, 1)
    assert main_segment_1_twin.start_date == date(2021, 6, 28)

    assert segment_2_1.thread.year_days == str(merged_days_2)
    assert segment_2_1.force_recalculate_days_text
    assert segment_2_1_twin.is_twin_segment
    assert main_segment_2.start_date == date(2021, 6, 1)

    with mock.patch.object(RThread, 'L_days_text_dict') as mock_L_days_text_dict_1:
        add_days_by_tz([main_segment_1], [MSK_TZ], next_plan=None)
    assert mock_L_days_text_dict_1.call_count == 4
    assert (
        [kwargs['force_recalculate_days_text'] for (_, kwargs) in mock_L_days_text_dict_1.call_args_list] ==
        [True, False, False, False]
    )

    with mock.patch.object(RThread, 'L_days_text_dict') as mock_L_days_text_dict_2:
        add_days_by_tz([main_segment_2], [MSK_TZ], next_plan=None)
    assert mock_L_days_text_dict_2.call_count == 2
    assert (
        [kwargs['force_recalculate_days_text'] for (_, kwargs) in mock_L_days_text_dict_2.call_args_list] ==
        [False, True]
    )
