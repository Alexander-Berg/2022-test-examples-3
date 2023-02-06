# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from travel.avia.library.python.common.models.geo import CodeSystem
from travel.avia.library.python.tester.factories import (
    create_settlement, create_station, create_station_code, create_station2settlement
)
from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches

from travel.avia.ticket_daemon.ticket_daemon.api.models_utils.station import _settlement_ids_by_airport_id


class SettlementIdsByAirportIdCacheTestCase(TestCase):
    def setUp(self):
        reset_all_caches()

        self.iata_code_system, _ = CodeSystem.objects.get_or_create(code='iata')

    def test_settlement_ids_by_airport_id(self):
        settlement1 = create_settlement(id=1, iata='SET1')
        settlement2 = create_settlement(id=2, iata='SET2')
        station1 = create_station(id=1, settlement_id=1)
        create_station_code(code='STA1', system_id=self.iata_code_system.id, station_id=1)
        create_station2settlement(station=station1, settlement=settlement2)

        settlement3 = create_settlement(id=3, iata='SET3')
        station2 = create_station(id=2)
        create_station_code(code='STA2', system_id=self.iata_code_system.id, station_id=2)
        create_station2settlement(station=station2, settlement=settlement3)

        actual_settlement_ids_by_airport_id = _settlement_ids_by_airport_id()

        assert actual_settlement_ids_by_airport_id.get(station1.id) == (settlement1.id, settlement2.id)
        assert actual_settlement_ids_by_airport_id.get(station2.id) == (settlement3.id,)
