from __future__ import absolute_import

from travel.avia.backend.repository.national_version import NationalVersionRepository


class FakeNationalVersionRepository(NationalVersionRepository):
    def __init__(self, all_nv):
        self._by_id = {
            s.pk: s for s in all_nv
        }
        self._code_to_model = {
            s.code: s for s in all_nv
        }

    def get_all(self):
        return self._by_id.values()
