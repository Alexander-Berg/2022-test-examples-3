# -*- coding: utf-8 -*-

from test_utils import TestParser


class TestYandexSearchResultWizCommonTransportJSONParser(TestParser):

    def avia(self):
        return self.parse_file('avia.json')

    def rasp(self):
        return self.parse_file('rasp.json')

    def bus(self):
        return self.parse_file('bus.json')

    def test_component_count(self):
        avia_components = self.avia()['components']
        rasp_components = self.rasp()['components']
        bus_components = self.bus()['components']
        assert 1 == len(avia_components)
        assert 1 == len(rasp_components)
        assert 1 == len(bus_components)

    def test_winner(self):
        avia_component = self.avia()['components'][0]
        rasp_component = self.rasp()['components'][0]
        bus_component = self.bus()['components'][0]
        assert 'avia' == avia_component['winner']
        assert 'rasp' == rasp_component['winner']
        assert 'bus' == bus_component['winner']
