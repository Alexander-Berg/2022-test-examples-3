# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta

import mock
import pytest

from common.apps.train_order.enums import CoachType
from common.models.currency import Price
from travel.rasp.train_api.tariffs.train.base import models as models_module
from travel.rasp.train_api.tariffs.train.base.models import TrainSegment, TrainTariff
from travel.rasp.train_api.train_purchase.core.models import ClientContract
from travel.rasp.train_api.train_purchase.utils.fee_calculator import TicketCost


class TestTrainSegment(object):
    @pytest.mark.dbuser
    def test_durations(self):
        segment = TrainSegment()
        segment.departure = datetime(2000, 1, 1)
        segment.arrival = datetime(2000, 1, 2)

        assert segment.duration == timedelta(days=1)
        assert segment.get_duration() == 1440

        segment = TrainSegment()
        segment.departure = datetime(2000, 1, 1)
        segment.arrival = datetime(2000, 1, 1, 1, 10, 59)

        assert segment.duration == timedelta(hours=1, minutes=10, seconds=59)
        assert segment.get_duration() == 70


class TestTrainTariff(object):
    @pytest.mark.parametrize('tariff, expected_call, expected_fee', (
        (TrainTariff(CoachType.PLATZKARTE, Price(1000), Price(100)), True, Price(100)),
        (TrainTariff(CoachType.COMPARTMENT, Price(2000), Price(200)), True, Price(200)),
        (TrainTariff(CoachType.SITTING, Price(500), Price(0)), True, Price(50)),
        (TrainTariff(CoachType.COMMON, Price(0), Price(0)), False, Price(0)),
    ))
    @mock.patch.object(
        models_module, 'calculate_ticket_cost', autospec=True,
        side_effect=lambda contract, coach_type, tariff, service_tariff, yandex_uid: TicketCost(
            100, 0, 10, tariff * 0.1, 0,
        )
    )
    def test_calculate_fee(self, m_get_tariff_with_fee, tariff, expected_call, expected_fee):
        contract = ClientContract()
        tariff.calculate_fee(contract)

        assert tariff.fee == expected_fee
        if expected_call:
            m_get_tariff_with_fee.assert_called_once_with(
                contract,
                tariff.coach_type,
                tariff.ticket_price.value,
                tariff.service_price.value,
                None,
            )
        else:
            assert not m_get_tariff_with_fee.called

    def test_total_price(self):
        tariff = TrainTariff(CoachType.PLATZKARTE, Price(1000), Price(100))

        with pytest.raises(AttributeError):
            tariff.total_price

        tariff.fee = Price(10)
        assert tariff.total_price == Price(1010)
