from __future__ import absolute_import

from travel.avia.backend.repository.region import RegionRepository


class FakeRegionRepository(RegionRepository):
    def __init__(self, all_regions):
        self._by_id = {
            s.pk: s for s in all_regions
        }

    def get_all(self):
        return self._by_id.values()

    def get(self, pk):
        return self._by_id.get(pk)
