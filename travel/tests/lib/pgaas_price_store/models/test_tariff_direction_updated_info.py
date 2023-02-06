# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest
from pytz import UTC

from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.models.tariff_direction_updated_info import (
    TariffDirectionUpdatedInfo, TariffDirectionUpdatedInfoRecord
)


class TestTariffDirectionUpdatedInfo(object):
    def test_get_updated_at_without_info(self):
        assert TariffDirectionUpdatedInfo(records=()).get_updated_at(datetime(2017, 1, 1, 12, tzinfo=UTC)) is None

    def test_get_updated_at_with_right_border(self):
        updated_info = TariffDirectionUpdatedInfo(records=(TariffDirectionUpdatedInfoRecord(
            left_border=datetime(2017, 1, 1, tzinfo=UTC),
            right_border=datetime(2017, 1, 2, tzinfo=UTC),
            updated_at=datetime(2018, 1, 1)
        ),),)
        assert updated_info.get_updated_at(datetime(2017, 1, 1, 12, tzinfo=UTC)) == datetime(2018, 1, 1)

    @pytest.mark.parametrize('dt', (
        datetime(2016, 1, 1, tzinfo=UTC),
        datetime(2019, 1, 1, tzinfo=UTC)
    ))
    def test_get_updated_at_without_right_border(self, dt):
        updated_info = TariffDirectionUpdatedInfo(records=(TariffDirectionUpdatedInfoRecord(
            left_border=datetime(2017, 1, 1, tzinfo=UTC),
            right_border=datetime(2017, 1, 2, tzinfo=UTC),
            updated_at=datetime(2018, 1, 1)
        ),),)
        assert updated_info.get_updated_at(dt) is None
