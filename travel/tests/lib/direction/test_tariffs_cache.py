# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock
import pytest

from common.models_abstract.schedule import ExpressType
from common.models.currency import Price
from common.models.factories import create_aeroex_tariff, create_tariff_type
from common.models.tariffs import TariffType
from common.tester.factories import create_station
from travel.rasp.wizards.suburban_wizard_api.lib.direction.tariffs_cache import TariffsCache

pytestmark = pytest.mark.dbuser

create_default_tariff = create_aeroex_tariff.mutate(type=TariffType.DEFAULT_ID)


def test_special_category_ignore():
    departure_station = create_station()
    arrival_station = create_station()
    special_tariff = create_tariff_type(category=TariffType.SPECIAL_CATEGORY)
    create_aeroex_tariff(station_from=departure_station, station_to=arrival_station, type=special_tariff)

    with TariffsCache.using_precache():
        assert TariffsCache.get_tariff(mock.Mock(
            arrival_station=arrival_station,
            departure_station=departure_station,
            thread=mock.Mock(tariff_type_id=special_tariff.id),
        )) is None


def test_currency():
    departure_station = create_station()
    arrival_station = create_station()
    create_default_tariff(
        station_from=departure_station,
        station_to=arrival_station,
        tariff=100,
        currency='BTC'
    )

    with TariffsCache.using_precache():
        assert TariffsCache.get_tariff(mock.Mock(
            arrival_station=arrival_station,
            departure_station=departure_station,
            thread=mock.Mock(tariff_type_id=TariffType.DEFAULT_ID),
        )) == Price(100, 'BTC')


def test_bidirectional():
    departure_station = create_station()
    arrival_station = create_station()
    create_default_tariff(station_from=departure_station, station_to=arrival_station, reverse=True, tariff=100)

    with TariffsCache.using_precache():
        assert TariffsCache.get_tariff(mock.Mock(
            arrival_station=arrival_station,
            departure_station=departure_station,
            thread=mock.Mock(tariff_type_id=TariffType.DEFAULT_ID),
        )) == TariffsCache.get_tariff(mock.Mock(
            arrival_station=departure_station,
            departure_station=arrival_station,
            thread=mock.Mock(tariff_type_id=TariffType.DEFAULT_ID),
        )) == Price(100)


def test_precalc_directional_priority():
    departure_station = create_station()
    arrival_station = create_station()
    create_default_tariff(station_from=departure_station, station_to=arrival_station, tariff=100)
    create_default_tariff(station_from=departure_station, station_to=arrival_station, precalc=True, tariff=200)

    with TariffsCache.using_precache():
        assert TariffsCache.get_tariff(mock.Mock(
            arrival_station=arrival_station,
            departure_station=departure_station,
            thread=mock.Mock(tariff_type_id=TariffType.DEFAULT_ID),
        )) == Price(100)


def test_precalc_bidirectional_priority():
    departure_station = create_station()
    arrival_station = create_station()
    create_default_tariff(
        station_from=departure_station,
        station_to=arrival_station,
        precalc=False,
        reverse=False,
        tariff=100
    )
    create_default_tariff(
        station_from=departure_station,
        station_to=arrival_station,
        precalc=True,
        reverse=True,
        tariff=200
    )

    with TariffsCache.using_precache():
        assert TariffsCache.get_tariff(mock.Mock(
            arrival_station=arrival_station,
            departure_station=departure_station,
            thread=mock.Mock(tariff_type_id=TariffType.DEFAULT_ID),
        )) == Price(100)
        assert TariffsCache.get_tariff(mock.Mock(
            arrival_station=departure_station,
            departure_station=arrival_station,
            thread=mock.Mock(tariff_type_id=TariffType.DEFAULT_ID),
        )) == Price(200)


def test_express():
    departure_station = create_station()
    arrival_station = create_station()
    create_aeroex_tariff(
        station_from=departure_station,
        station_to=arrival_station,
        tariff=100,
        type=TariffType.EXPRESS_ID
    )
    create_default_tariff(
        station_from=departure_station,
        station_to=arrival_station,
        tariff=200
    )

    with TariffsCache.using_precache():
        assert TariffsCache.get_tariff(mock.Mock(
            arrival_station=arrival_station,
            departure_station=departure_station,
            thread=mock.Mock(tariff_type_id=None, express_type=ExpressType.EXPRESS),
        )) == TariffsCache.get_tariff(mock.Mock(
            arrival_station=arrival_station,
            departure_station=departure_station,
            thread=mock.Mock(tariff_type_id=None, express_type=ExpressType.AEROEXPRESS),
        )) == Price(100)
        assert TariffsCache.get_tariff(mock.Mock(
            arrival_station=departure_station,
            departure_station=arrival_station,
            thread=mock.Mock(tariff_type_id=None, express_type=ExpressType.COMMON),
        )) == Price(200)
