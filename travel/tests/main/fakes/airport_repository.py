from __future__ import absolute_import

from travel.avia.backend.repository.station import StationRepository


class FakeAirportRepository(StationRepository):
    def __init__(self, all_airports):
        self._by_id = {
            s.pk: s for s in all_airports
        }

    def get_all(self):
        return self._by_id.values()

    def get(self, pk):
        return self._by_id.get(pk)
