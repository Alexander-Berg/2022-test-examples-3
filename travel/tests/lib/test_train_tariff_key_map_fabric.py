# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytz
from unittest import TestCase
from django.utils.datetime_safe import datetime

from travel.rasp.touch.touch.core.lib.train_number import TrainNumberParser, TrainNumberReverser
from travel.rasp.touch.touch.core.lib.train_tariff_key_map_fabric import TrainTariffKeyMapFabric


class TestTrainTariffKeyMapFabric(TestCase):
    def setUp(self):
        self._fabric = TrainTariffKeyMapFabric(
            train_number_parser=TrainNumberParser(),
            train_number_reverser=TrainNumberReverser()
        )

        self._departure_dt = pytz.UTC.localize(datetime(2018, 9, 1, 13, 45))

    def test_keys_for_incorrect_number(self):
        assert self._fabric.generate_map_keys(self._departure_dt, 'XXX') == {
            'train XXX 20180901_12',
            'train XXX 20180901_1345',
            'train XXX 20180901_14',
        }

    def test_keys_for_correct_number_with_many_letters(self):
        assert self._fabric.generate_map_keys(self._departure_dt, '42XXX') == {
            'train 042X 20180901_12',
            'train 042X 20180901_1345',
            'train 042X 20180901_14',
            'train 041X 20180901_12',
            'train 041X 20180901_1345',
            'train 041X 20180901_14',
        }

    def test_keys_for_correct_number_with_one_letters(self):
        assert self._fabric.generate_map_keys(self._departure_dt, '42T') == {
            'train 042T 20180901_12',
            'train 042T 20180901_1345',
            'train 042T 20180901_14',
            'train 041T 20180901_12',
            'train 041T 20180901_1345',
            'train 041T 20180901_14',
        }

