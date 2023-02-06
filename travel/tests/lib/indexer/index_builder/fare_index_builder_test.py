from datetime import datetime

from mock import Mock
from typing import cast
from unittest import TestCase

from travel.avia.price_index.lib.indexer.index_builder import FareIndexBuilder
from travel.avia.price_index.lib.indexer.index_builder.airlines_index_builder import AirlineIndexBuilder
from travel.avia.price_index.lib.indexer.index_builder.airport_index_builder import AirportIndexBuilder
from travel.avia.price_index.lib.indexer.index_builder.time_index_builder import TimeIndexBuilder
from travel.avia.price_index.lib.indexer.index_builder.transfer_index_builder import TransferIndexBuilder
from travel.avia.price_index.models.flight import Flight
from travel.avia.price_index.models.index_data import AirportIndex, TimeIndex, TransferIndex, AirportIndexByDirection


class FareIndexBuilderTest(TestCase):
    def test(self):
        airport_index = AirportIndex(
            forward=AirportIndexByDirection(arrival=1, transfer={2}, departure=3),
            backward=AirportIndexByDirection(arrival=4, transfer={5}, departure=6),
        )
        time_index = TimeIndex(forward_arrival=10, forward_departure=20, backward_arrival=30, backward_departure=40)
        transfer_index = TransferIndex(count=100, duration=200, has_airport_change=True, has_night_transfer=False)
        airline_index = {42}

        airport_index_builder = Mock()
        airport_index_builder.index = Mock(return_value=airport_index)
        time_index_builder = Mock()
        time_index_builder.index = Mock(return_value=time_index)
        transfer_index_builder = Mock()
        transfer_index_builder.index = Mock(return_value=transfer_index)

        airline_index_builder = Mock()
        airline_index_builder.index = Mock(return_value=airline_index)

        self._builder = FareIndexBuilder(
            airport_index_builder=cast(AirportIndexBuilder, airport_index_builder),
            time_index_builder=cast(TimeIndexBuilder, time_index_builder),
            transfer_index_builder=cast(TransferIndexBuilder, transfer_index_builder),
            airline_index_builder=cast(AirlineIndexBuilder, airline_index_builder),
        )

        index = self._builder.index((self._generate_flight(),), tuple())

        assert index.airport_index is airport_index
        assert index.time_index is time_index
        assert index.transfer_index is transfer_index
        assert index.airline_index is airline_index

    def _generate_flight(self):
        return Flight(
            departure=datetime(2017, 9, 10),
            arrival=datetime(2017, 9, 10),
            departure_utc=datetime(2017, 9, 1),
            arrival_utc=datetime(2017, 9, 1),
            airline_id=10,
            from_id=1,
            to_id=2,
            number='su 42',
        )
