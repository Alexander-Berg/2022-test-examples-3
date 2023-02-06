# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import datetime

import pytest
import mock

from travel.buses.connectors.tests. yabus.common.library.test_utils import converter_patch
from yabus.sks.entities import Ride


DUMMY_RIDE = {
    "arrival": datetime(2000, 1, 1, 13),
    "carrier": "СКСавто",
    "departure": datetime(2000, 1, 1, 12),
    "duration": "03:35",
    "freeSeatsCount": "3,7,11",
    "from": "415",
    "id": "5157",
    "is_international": "0",
    "name": "СПб, АВ Обводный, 36 - Лодейное Поле, АС",
    "number": "1622d",
    "price": {
        "parts": [
            {"type": "adult", "value": 650},
            {"type": "child", "value": 325},
            {"type": "bag", "value": 50},
            {"type": "adult_roundtrip", "value": 1300},
            {"type": "child_roundtrip", "value": 650},
            {"type": "infant_roundtrip", "value": 650},
        ]
    },
    "refund_with_charges": [
        {"return_percent": "95%", "time_from": "", "time_till": "120"},
        {"return_percent": "85%", "time_from": "120", "time_till": "0"},
        {"return_percent": "75%", "time_from": "0", "time_till": "-180"},
        {"return_percent": "", "time_from": "-180", "time_till": ""},
    ],
    "rt_from": "415",
    "rt_to": "541",
    "rtname": "СПб, АВ Обводный, 36 - Пудож, АС",
    "to": "426",
}


@pytest.fixture(autouse=True, scope="module")
def patch_sks_ride():
    # fmt: off
    with mock.patch.object(Ride, "carrier_matcher"), \
         mock.patch.object(Ride, "supplier_provider"), \
         converter_patch("sks"):
        yield
    # fmt: on


class TestRide(object):
    def test_refund_with_charges(self):
        # fmt: off
        assert Ride.init(DUMMY_RIDE.copy())["refundConditions"] == """\
Сумма возврата зависит от времени:
• более 2 ч до отправления: 95% от тарифа
• менее 2 ч до отправления: 85% от тарифа
• менее 3 ч после отправления: 75% от тарифа"""
        # fmt: on

    def test_refund_with_charges_missing_defaults(self):
        raw_ride = DUMMY_RIDE.copy()
        del raw_ride["refund_with_charges"]
        assert Ride.init(raw_ride)["refundConditions"] == Ride.refund_conditions

    @pytest.mark.parametrize('conditions', (
        [],

        # no return_percent
        [{"return_percent": "", "time_from": "60", "time_till": "0"}],

        # no borders
        [{"return_percent": "50%", "time_from": "", "time_till": "0"}],
        [{"return_percent": "50%", "time_from": "0", "time_till": ""}],

        # out of order intervals
        [{"return_percent": "85%", "time_from": "60", "time_till": "120"}],
        [{"return_percent": "85%", "time_from": "60", "time_till": "60"}],

        # no zero
        [
            {"return_percent": "85%", "time_from": "120", "time_till": "60"},
            {"return_percent": "50%", "time_from": "60", "time_till": "-120"},
        ],

        # discontinuous intervals
        [
            {"return_percent": "85%", "time_from": "", "time_till": "120"},
            {"return_percent": "50%", "time_from": "60", "time_till": ""},
        ],
    ))
    def test_refund_with_charges_defaults(self, conditions):
        assert Ride.init(dict(DUMMY_RIDE, **{"refund_with_charges": conditions}))["refundConditions"] == Ride.refund_conditions
