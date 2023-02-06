from unittest import TestCase

from datetime import datetime

from travel.avia.price_index.lib.indexer.index_builder.airport_index_builder import AirportIndexBuilder
from travel.avia.price_index.models.flight import Flight


class AirportIndexBuilderTest(TestCase):
    def setUp(self):
        self._builder = AirportIndexBuilder()

    def _generate_flight(self, from_id, to_id):
        return Flight(
            departure=datetime(2017, 9, 10),
            departure_utc=datetime(2017, 9, 10),
            arrival=datetime(2017, 9, 1),
            arrival_utc=datetime(2017, 9, 1),
            airline_id=10,
            from_id=from_id,
            to_id=to_id,
            number='su {}/{}'.format(from_id, to_id),
        )

    def test_direct_flight_without_backward(self):
        index = self._builder.index(
            forward_flights=(self._generate_flight(from_id=1, to_id=2),), backward_flights=tuple()
        )

        assert index.forward.departure == 1
        assert index.forward.transfer == set()
        assert index.forward.arrival == 2

        assert index.backward.departure is None
        assert index.backward.transfer == set()
        assert index.backward.arrival is None

    def test_direct_flight(self):
        index = self._builder.index(
            forward_flights=(self._generate_flight(from_id=1, to_id=2),),
            backward_flights=(self._generate_flight(from_id=10, to_id=20),),
        )

        assert index.forward.departure == 1
        assert index.forward.transfer == set()
        assert index.forward.arrival == 2

        assert index.backward.departure == 10
        assert index.backward.transfer == set()
        assert index.backward.arrival == 20

    def test_flight_with_transfer(self):
        index = self._builder.index(
            forward_flights=(self._generate_flight(from_id=1, to_id=2), self._generate_flight(from_id=3, to_id=4)),
            backward_flights=(
                self._generate_flight(from_id=10, to_id=20),
                self._generate_flight(from_id=30, to_id=40),
            ),
        )

        assert index.forward.departure == 1
        assert index.forward.transfer == {2, 3}
        assert index.forward.arrival == 4

        assert index.backward.departure == 10
        assert index.backward.transfer == {20, 30}
        assert index.backward.arrival == 40
