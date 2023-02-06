from __future__ import absolute_import

from travel.avia.backend.repository.s2s import GeoRelationsRepository


class FakeGeoRelationsRepository(GeoRelationsRepository):
    def __init__(self, settlement_to_airports_ids):
        self._settlement_to_airports_ids = settlement_to_airports_ids

    def get_airport_ids_for(self, settlement_pk):
        return self._settlement_to_airports_ids.get(settlement_pk, [])
