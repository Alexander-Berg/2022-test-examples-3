# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import timedelta

import pytest
from hamcrest import assert_that, contains_inanyorder

from common.models.schedule import RThreadType
from common.models.transport import TransportType
from common.tester.factories import create_rthread_segment, create_thread, create_company
from travel.rasp.library.python.common23.date.environment import now_aware

from travel.rasp.train_api.tariffs.train.base.utils import make_segment_train_keys
from travel.rasp.train_api.tariffs.train.segment_builder.helpers.merge_trains import (
    is_through_train, parse_train_number, TrainNumber, choose_main_segment, fill_train_numbers,
    create_meta_train, make_meta_trains
)

pytestmark = [pytest.mark.dbuser('module')]


def test_is_through_train():
    segment = create_rthread_segment(thread=create_thread(t_type=TransportType.TRAIN_ID,
                                                          type=RThreadType.THROUGH_TRAIN_ID))
    assert is_through_train(segment)


def test_get_train_number():
    assert parse_train_number('303М') == TrainNumber('303', 'М')
    assert parse_train_number('303МЯ') == TrainNumber('303', 'МЯ')


def tests_choose_main_segment():
    now = now_aware()
    segment1 = create_rthread_segment(thread=create_thread(t_type=TransportType.TRAIN_ID,
                                                           type=RThreadType.THROUGH_TRAIN_ID), number='303М',
                                      departure=now, arrival=now + timedelta(2))
    segment2 = create_rthread_segment(thread=create_thread(t_type=TransportType.TRAIN_ID), number='303Я')
    segment3 = create_rthread_segment(thread=create_thread(t_type=TransportType.TRAIN_ID,
                                                           type=RThreadType.THROUGH_TRAIN_ID), number='303МЯ',
                                      departure=now, arrival=now + timedelta(1))
    assert choose_main_segment([segment1, segment2, segment3]) == segment2
    assert choose_main_segment([segment1, segment3]) == segment3


def test_create_meta_train():
    company1 = create_company(id=556, short_title_en='test short title1')
    company2 = create_company(id=557, short_title_en='test short title2')
    segments = [create_rthread_segment(thread=create_thread(t_type=TransportType.TRAIN_ID),
                                       number='303М', company=company1),
                create_rthread_segment(thread=create_thread(t_type=TransportType.TRAIN_ID),
                                       number='303Я', company=company2),
                create_rthread_segment(thread=create_thread(t_type=TransportType.TRAIN_ID),
                                       number='303МЯ', company=company1)]
    fill_train_numbers(segments)
    meta_train = create_meta_train(segments[1], segments)
    assert meta_train.number == '303МЯ'
    assert meta_train.letters == {'М', 'Я', 'МЯ'}
    assert meta_train.companies == {company1, company2}
    assert meta_train.company == company2
    assert segments[1].number == '303Я'
    assert segments[1].company == company2


def test_create_meta_train_mrja():
    segments = [create_rthread_segment(thread=create_thread(t_type=TransportType.TRAIN_ID), number='303МЯ'),
                create_rthread_segment(thread=create_thread(t_type=TransportType.TRAIN_ID), number='303РЯ'),
                create_rthread_segment(thread=create_thread(t_type=TransportType.TRAIN_ID), number='303Я')]
    fill_train_numbers(segments)
    meta_train = create_meta_train(segments[1], segments)
    assert meta_train.number == '303МРЯ'
    assert meta_train.letters == {'Я', 'МЯ', 'РЯ', 'МРЯ'}
    assert meta_train.companies == set()


def test_make_meta_trains():
    now = now_aware()
    through_segment = create_rthread_segment(thread=create_thread(t_type=TransportType.TRAIN_ID,
                                                                  type=RThreadType.THROUGH_TRAIN_ID),
                                             number='303М',
                                             arrival=now + timedelta(hours=1))
    main_segment = create_rthread_segment(thread=create_thread(t_type=TransportType.TRAIN_ID), number='303Я',
                                          arrival=now + timedelta(hours=2))
    segments = [through_segment,
                main_segment,
                create_rthread_segment(thread=create_thread(t_type=TransportType.TRAIN_ID), number='305Я',
                                       arrival=now + timedelta(hours=3))]
    segments = make_meta_trains(segments)
    assert len(segments) == 2
    assert segments[-1].number == '303МЯ'
    assert segments[-1].letters == {'М', 'Я', 'МЯ'}
    assert segments[-1].companies == set()
    assert segments[-1].sub_segments == [through_segment, main_segment]
    assert segments[-1].min_arrival == now + timedelta(hours=1)
    assert segments[-1].max_arrival == now + timedelta(hours=2)
    assert_that(make_segment_train_keys(segments[-1]), contains_inanyorder('train 303МЯ 20150101_1000',
                                                                           'train 304МЯ 20150101_1000',
                                                                           'train 303М 20150101_1000',
                                                                           'train 304М 20150101_1000',
                                                                           'train 303Я 20150101_1000',
                                                                           'train 304Я 20150101_1000'))
