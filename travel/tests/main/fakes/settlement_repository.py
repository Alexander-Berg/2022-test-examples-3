from __future__ import absolute_import

from travel.avia.backend.repository.settlement import SettlementRepository


class FakeSettlementRepository(SettlementRepository):
    def __init__(self, all_cities):
        self._by_id = {
            s.pk: s for s in all_cities
        }
        self._by_geo_id = {
            s.geo_id: s for s in all_cities
        }

    def get_all(self):
        return self._by_id.values()

    def get(self, pk):
        return self._by_id.get(pk)
