# -*- coding: utf-8 -*-
from __future__ import absolute_import

from travel.avia.backend.main.api_types.partnersData import _CPCSource


class TestCPCSourceEmptyCaseForCompatibility:
    source = _CPCSource('')

    def test_get_field_name__empty_case(self):
        assert self.source.get_field_name() == 'eCPC'

    def test_get_api_field_name(self, faker):
        expected = national_version = faker.pystr()
        assert self.source.get_api_field_name(national_version) == expected


class TestCPCSource:
    source = _CPCSource('some_source')

    def test_get_field_name(self):
        assert self.source.get_field_name() == 'eCPC_some_source'

    def test_get_api_field_name(self):
        national_version = 'national'
        expected = 'some_source_national'
        assert self.source.get_api_field_name(national_version) == expected
