from __future__ import absolute_import

from travel.avia.backend.repository.country import CountryRepository


class FakeCountryRepository(CountryRepository):
    def __init__(self, all_countries):
        self._by_id = {
            s.pk: s for s in all_countries
        }
        self._geo_id_index = {
            s.geo_id: s for s in all_countries
        }

    def get_all(self):
        return self._by_id.values()

    def get(self, pk):
        return self._by_id.get(pk)
