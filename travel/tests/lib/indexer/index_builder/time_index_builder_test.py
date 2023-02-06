from datetime import datetime

from unittest import TestCase

from travel.avia.price_index.lib.indexer.index_builder.time_index_builder import TimeIndexBuilder
from travel.avia.price_index.models.flight import Flight
from travel.avia.price_index.models.index_data import TimeIndex


class TimeIndexBuilderTest(TestCase):
    def setUp(self):
        self._builder = TimeIndexBuilder()

    def _generate_flight(self, arrival_time, backward_time):
        return Flight(
            departure=datetime(2017, 9, 10, arrival_time[0], arrival_time[1]),
            arrival=datetime(2017, 9, 10, backward_time[0], backward_time[1]),
            arrival_utc=datetime(2017, 9, 1),
            departure_utc=datetime(2017, 9, 1),
            airline_id=10,
            from_id=1,
            to_id=10,
            number='su {}/{}'.format(arrival_time, backward_time),
        )

    def test_flight_without_backward(self):
        index = self._builder.index(forward_flights=(self._generate_flight((1, 0), (7, 0)),), backward_flights=tuple())
        assert index.forward_departure == TimeIndex.NIGHT
        assert index.forward_arrival == TimeIndex.MORNING

        assert index.backward_departure == TimeIndex.UNKNOWN
        assert index.backward_arrival == TimeIndex.UNKNOWN

    def test_flight(self):
        index = self._builder.index(
            forward_flights=(self._generate_flight((1, 0), (7, 0)),),
            backward_flights=(self._generate_flight((13, 0), (19, 0)),),
        )
        assert index.forward_departure == TimeIndex.NIGHT
        assert index.forward_arrival == TimeIndex.MORNING

        assert index.backward_departure == TimeIndex.DAY
        assert index.backward_arrival == TimeIndex.EVENING

    def test_flight_with_transfer(self):
        index = self._builder.index(
            forward_flights=(self._generate_flight((1, 0), (5, 0)), self._generate_flight((5, 0), (7, 0))),
            backward_flights=(
                self._generate_flight((13, 0), (14, 0)),
                self._generate_flight((14, 0), (19, 0)),
            ),
        )
        assert index.forward_departure == TimeIndex.NIGHT
        assert index.forward_arrival == TimeIndex.MORNING

        assert index.backward_departure == TimeIndex.DAY
        assert index.backward_arrival == TimeIndex.EVENING
