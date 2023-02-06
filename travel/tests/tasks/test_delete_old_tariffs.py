# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, date, timedelta

from hamcrest import assert_that, has_properties, contains
from mongoengine import Q

from common.tester.utils.datetime import replace_now

from travel.rasp.suburban_selling.selling.im.factories import ImTariffsFactory
from travel.rasp.suburban_selling.selling.im.models import ImTariffs
from travel.rasp.suburban_selling.selling.tasks.delete_old_tariffs import (
    delete_old_im_tariffs, DAYS_TO_KEEP_OLD_MOVISTA_TARIFFS, DAYS_TO_KEEP_OLD_IM_TARIFFS, delete_old_movista_tariffs
)
from travel.rasp.suburban_selling.selling.movista.models import MovistaTariffs
from travel.rasp.suburban_selling.selling.movista.tariffs import TariffFromToKey
from travel.rasp.suburban_selling.selling.movista.factories import MovistaTariffsFactory


@replace_now('2021-01-29 10:00:00')
def test_delete_old_movista_tariffs():
    tariff_key1 = TariffFromToKey(date=date(2021, 1, 29), station_from=42, station_to=43)
    tariff_key2 = TariffFromToKey(date=date(2021, 1, 30), station_from=42, station_to=43)

    MovistaTariffsFactory(
        date=date(2021, 1, 29),
        station_from=42,
        station_to=43,
        updated=datetime.utcnow() - timedelta(days=DAYS_TO_KEEP_OLD_MOVISTA_TARIFFS + 1)
    )

    MovistaTariffsFactory(
        date=date(2021, 1, 30),
        station_from=42,
        station_to=43,
        updated=datetime.utcnow() - timedelta(days=DAYS_TO_KEEP_OLD_MOVISTA_TARIFFS - 1)
    )

    tariffs_query = Q()
    for key in {tariff_key1, tariff_key2}:
        tariffs_query |= Q(
            date=key.date,
            station_from=key.station_from,
            station_to=key.station_to
        )

    tariffs = list(MovistaTariffs.objects.filter(tariffs_query))
    assert len(tariffs) == 2

    delete_old_movista_tariffs()

    tariffs = list(MovistaTariffs.objects.filter(tariffs_query))

    assert len(tariffs) == 1
    assert_that(tariffs, contains(has_properties(
        date=date(2021, 1, 30),
        station_from=42,
        station_to=43
    )))


@replace_now('2021-01-29 10:00:00')
def test_delete_old_im_tariffs():
    tariff_key1 = TariffFromToKey(date=date(2021, 1, 29), station_from=42, station_to=43)
    tariff_key2 = TariffFromToKey(date=date(2021, 1, 30), station_from=42, station_to=43)

    ImTariffsFactory(
        date=date(2021, 1, 29),
        station_from=42,
        station_to=43,
        updated=datetime.utcnow() - timedelta(days=DAYS_TO_KEEP_OLD_IM_TARIFFS + 1)
    )

    ImTariffsFactory(
        date=date(2021, 1, 30),
        station_from=42,
        station_to=43,
        updated=datetime.utcnow() - timedelta(days=DAYS_TO_KEEP_OLD_IM_TARIFFS - 1)
    )

    tariffs_query = Q()
    for key in {tariff_key1, tariff_key2}:
        tariffs_query |= Q(
            date=key.date,
            station_from=key.station_from,
            station_to=key.station_to
        )

    tariffs = list(ImTariffs.objects.filter(tariffs_query))
    assert len(tariffs) == 2

    delete_old_im_tariffs()

    tariffs = list(ImTariffs.objects.filter(tariffs_query))

    assert len(tariffs) == 1
    assert_that(tariffs, contains(has_properties(
        date=date(2021, 1, 30),
        station_from=42,
        station_to=43
    )))
