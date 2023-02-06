# coding=utf-8
from __future__ import unicode_literals

from travel.avia.shared_flights.data_importer.flights_data_importer import FlightsDataImporter
from travel.proto.dicts.rasp.carrier_pb2 import TCarrier


class TestFlightsDataImporter:

    def test_replace_carrier_iata_with_id(self):
        carrier = TCarrier()
        carrier.Id = 26
        carrier.Iata = 'SU'

        carriers = {}
        carriers['SU'] = carrier

        # no exceptions on empty string
        assert FlightsDataImporter.replace_carrier_iata_with_id(carriers, '') == ''
        # not enough parts
        assert FlightsDataImporter.replace_carrier_iata_with_id(carriers, 'SU.2') == 'SU.2'
        # too many parts
        assert FlightsDataImporter.replace_carrier_iata_with_id(carriers, 'SU.2.3.4') == 'SU.2.3.4'
        # unknown carrier
        assert FlightsDataImporter.replace_carrier_iata_with_id(carriers, 'AK.2.3') == 'AK.2.3'
        # finally, when all is good
        assert FlightsDataImporter.replace_carrier_iata_with_id(carriers, 'SU.2.3') == '26.2.3'
