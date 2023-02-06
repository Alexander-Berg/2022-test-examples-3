# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from travel.rasp.train_api.scripts.fill_displayed_coach_owner import fill_chunk, calc_stat
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def test_ok():
    order = TrainOrderFactory(coach_owner='ФПК ДАЛЬНЕВОСТОЧНЫЙ', displayed_coach_owner=None)

    assert fill_chunk()

    order.reload()
    assert order.coach_owner == 'ФПК ДАЛЬНЕВОСТОЧНЫЙ'
    assert order.displayed_coach_owner == 'ФПК'


def test_allready_filled():
    order = TrainOrderFactory(coach_owner='ФПК ДАЛЬНЕВОСТОЧНЫЙ', displayed_coach_owner='ФПК33')

    assert not fill_chunk()

    order.reload()
    assert order.coach_owner == 'ФПК ДАЛЬНЕВОСТОЧНЫЙ'
    assert order.displayed_coach_owner == 'ФПК33'


def test_can_not_fill():
    order = TrainOrderFactory(coach_owner='НЕСУЩЕСТВУЮЩИЙ ПЕРЕВОЗЧИК', displayed_coach_owner=None)

    assert not fill_chunk()

    order.reload()
    assert order.coach_owner == 'НЕСУЩЕСТВУЮЩИЙ ПЕРЕВОЗЧИК'
    assert order.displayed_coach_owner is None


def test_stat():
    TrainOrderFactory(coach_owner='П1', displayed_coach_owner=None)
    TrainOrderFactory(coach_owner='П1', displayed_coach_owner='П1')
    TrainOrderFactory(coach_owner='П2', displayed_coach_owner=None)
    TrainOrderFactory(coach_owner='П2', displayed_coach_owner=None)
    TrainOrderFactory(coach_owner='П3', displayed_coach_owner='П3')

    result = calc_stat()

    assert set(result.keys()) == {'П1', 'П2'}
    assert result['П1'] == 1
    assert result['П2'] == 2
