from __future__ import absolute_import

from travel.avia.backend.main.lib.display_code_manager import DisplayCodeManager


class FakeDisplayCodeManager(DisplayCodeManager):
    def __init__(self, airport_index=None, settlement_iata_index=None,
                 settlement_code_index=None):
        self._airport_index = airport_index or {}
        self._settlement_iata_index = settlement_iata_index or {}
        self._settlement_code_index = settlement_code_index or {}
