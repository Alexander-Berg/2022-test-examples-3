# coding=utf-8
from __future__ import unicode_literals

from travel.avia.shared_flights.diff_builder.utils import convert_to_latin


class TestConvertToLatin:
    def test_convert_to_latin(self):
        assert convert_to_latin('ТСРОНМКЕВАФ') == 'TCPOHMKEBAФ'
