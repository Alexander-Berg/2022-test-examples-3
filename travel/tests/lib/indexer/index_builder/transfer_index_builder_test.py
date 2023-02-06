from datetime import datetime

from unittest import TestCase

from travel.avia.price_index.lib.indexer.index_builder.transfer_index_builder import TransferIndexBuilder
from travel.avia.price_index.models.flight import Flight


class TransferIndexBuilderTest(TestCase):
    def setUp(self):
        self._builder = TransferIndexBuilder()

    def test_count_without_transfer_and_backward(self):
        index = self._builder.index((self._generate_flight(1, 2),), tuple())

        assert index.count == 0

    def test_count_without_transfer(self):
        index = self._builder.index((self._generate_flight(1, 2),), (self._generate_flight(2, 1),))

        assert index.count == 0

    def test_count(self):
        index = self._builder.index(
            (self._generate_flight(1, 2), self._generate_flight(1, 2)), (self._generate_flight(2, 1),)
        )

        assert index.count == 1

        index = self._builder.index(
            (
                self._generate_flight(1, 2),
                self._generate_flight(2, 4),
            ),
            (
                self._generate_flight(1, 2),
                self._generate_flight(2, 3),
                self._generate_flight(3, 4),
            ),
        )

        assert index.count == 2

    def test_transfer_duration_for_direct_flight(self):
        index = self._builder.index(
            (self._generate_flight(1, 2, departure=1, arrival=11),),
            (self._generate_flight(1, 2, departure=2, arrival=22),),
        )

        assert index.duration is None

        index = self._builder.index((self._generate_flight(1, 2, departure=1, arrival=11),), tuple())

        assert index.duration is None

    def test_transfer_duration(self):
        index = self._builder.index(
            (
                self._generate_flight(1, 2, departure=1, arrival=11),
                self._generate_flight(2, 3, departure=12, arrival=20),
                # transfer duration is 60 minutes
            ),
            (
                self._generate_flight(3, 2, departure=1, arrival=11),
                self._generate_flight(2, 1, departure=14, arrival=20)
                # transfer duration is 180 minutes
            ),
        )

        assert index.duration == 60

        index = self._builder.index(
            (
                self._generate_flight(1, 2, departure=1, arrival=11),
                self._generate_flight(2, 3, departure=18, arrival=20),
                # transfer duration is 420 minutes
            ),
            (
                self._generate_flight(3, 2, departure=1, arrival=11),
                self._generate_flight(2, 1, departure=14, arrival=20)
                # transfer duration is 180 minutes
            ),
        )

        assert index.duration == 180

    def test_has_night_transfer_in_direct_flight(self):
        index = self._builder.index(
            (self._generate_flight(1, 2, departure=1, arrival=11),),
            (self._generate_flight(1, 2, departure=2, arrival=22),),
        )
        assert index.has_night_transfer is False

        index = self._builder.index((self._generate_flight(1, 2, departure=1, arrival=11),), tuple())
        assert index.has_night_transfer is False

    def test_has_night_transfer_for_very_long_departure(self):
        index = self._builder.index(
            (
                self._generate_flight(1, 2, departure=1, arrival=2),
                self._generate_flight(1, 2, departure=10, arrival=11),
            ),
            tuple(),
        )
        assert index.has_night_transfer is True

    def test_has_night_transfer_for_arrival_in_night(self):
        index = self._builder.index(
            (
                self._generate_flight(1, 2, departure=1, arrival=6),
                self._generate_flight(1, 2, departure=11, arrival=11),
            ),
            tuple(),
        )
        assert index.has_night_transfer is True

    def test_does_not_have_airport_change_in_direct_flight(self):
        index = self._builder.index((self._generate_flight(1, 2),), (self._generate_flight(2, 1),))
        assert index.has_airport_change is False

    def test_has_airport_change(self):
        index = self._builder.index(
            (
                self._generate_flight(1, 2),
                self._generate_flight(3, 4),
            ),
            (
                self._generate_flight(1, 2),
                self._generate_flight(2, 3),
            ),
        )
        assert index.has_airport_change is True

        index = self._builder.index(
            (
                self._generate_flight(1, 2),
                self._generate_flight(2, 3),
            ),
            (
                self._generate_flight(1, 2),
                self._generate_flight(3, 4),
            ),
        )
        assert index.has_airport_change is True

    def _generate_flight(self, from_id, to_id, departure=0, arrival=0):
        return Flight(
            departure=datetime(2017, 9, 10),
            arrival=datetime(2017, 9, 10),
            departure_utc=datetime(2017, 9, 1, departure),
            arrival_utc=datetime(2017, 9, 1, arrival),
            airline_id=10,
            from_id=from_id,
            to_id=to_id,
            number='su {}/{}'.format(from_id, to_id),
        )
