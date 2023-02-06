# -*- coding: utf-8 -*-

import pytest

from common.models.factories import create_aeroex_tariff
from common.tester.transaction_context import transaction_fixture
from common.tester.factories import create_rthread_segment
from travel.rasp.morda.morda.tariffs.views import get_suburban_tariffs_for_segments, get_tariffs_for_segments


@pytest.fixture(scope='module')
@transaction_fixture
def segment_with_byr_tariff(request):
    segment = create_rthread_segment()
    create_aeroex_tariff(station_from=segment.station_from, station_to=segment.station_to, currency='BYR', type=1)
    return segment


@pytest.mark.dbuser
def test_byr_tariffs(segment_with_byr_tariff):
    tariffs = get_tariffs_for_segments([segment_with_byr_tariff])
    assert all(tariff.tariff.currency == 'BYR' for tariff in tariffs.values())


@pytest.mark.dbuser
def test_suburban_byr_tariffs(segment_with_byr_tariff):
    price, _prices_by_type = get_suburban_tariffs_for_segments([segment_with_byr_tariff])
    assert price.currency == 'BYR'
