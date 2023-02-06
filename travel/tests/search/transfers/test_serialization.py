# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.utils import translation

from common.tester.factories import create_settlement, create_station
from travel.rasp.morda_backend.morda_backend.search.transfers.serialization import (
    TransferSettlementSchema, TransferStationSchema
)


@pytest.mark.dbuser()
def test_transfer_settlement_title():
    settlement = create_settlement(
        title_ru='title_ru',
        title_ru_genitive='title_ru_genitive',
        title_ru_accusative='title_ru_accusative',
        title_ru_locative='title_ru_locative',
        title_ru_preposition_v_vo_na='title_ru_preposition_v_vo_na',
        title_uk='title_uk'
    )

    with translation.override('ru'):
        result = TransferSettlementSchema().dump(settlement)
        assert not result.errors
        assert result.data['title'] == settlement.title_ru
        assert result.data['titleGenitive'] == settlement.title_ru_genitive
        assert result.data['titleAccusative'] == settlement.title_ru_accusative
        assert result.data['titleLocative'] == settlement.title_ru_locative
        assert result.data['preposition'] == settlement.title_ru_preposition_v_vo_na

    with translation.override('uk'):
        result = TransferSettlementSchema().dump(settlement)
        assert not result.errors
        assert result.data['title'] == settlement.title_uk
        assert result.data['titleGenitive'] is None
        assert result.data['titleAccusative'] is None
        assert result.data['titleLocative'] is None
        assert result.data['preposition'] is None


@pytest.mark.dbuser()
def test_transfer_station_title():
    station = create_station(
        title_ru='title_ru',
        title_ru_genitive='title_ru_genitive',
        title_ru_accusative='title_ru_accusative',
        title_ru_locative='title_ru_locative',
        title_ru_preposition_v_vo_na='title_ru_preposition_v_vo_na',
        popular_title_ru='popular_title_ru',
        title_uk='title_uk'
    )

    with translation.override('ru'):
        result = TransferStationSchema().dump(station)
        assert not result.errors
        assert result.data['title'] == station.title_ru
        assert result.data['titleGenitive'] == station.title_ru_genitive
        assert result.data['titleAccusative'] == station.title_ru_accusative
        assert result.data['titleLocative'] == station.title_ru_locative
        assert result.data['preposition'] == station.title_ru_preposition_v_vo_na
        assert result.data['popularTitle'] == station.popular_title_ru

    with translation.override('uk'):
        result = TransferStationSchema().dump(station)
        assert not result.errors
        assert result.data['title'] == station.title_uk
        assert result.data['titleGenitive'] is None
        assert result.data['titleAccusative'] is None
        assert result.data['titleLocative'] is None
        assert result.data['preposition'] is None
        assert result.data['popularTitle'] is None
