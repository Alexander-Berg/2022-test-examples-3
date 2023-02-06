# -*- coding: utf-8 -*-
from datetime import datetime
import mock

from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.ticket_daemon.ticket_daemon.api import flights
from travel.avia.ticket_daemon.ticket_daemon.daemon.utils import fill
from travel.avia.ticket_daemon.ticket_daemon.daemon.variants_saver import unify_variants
from travel.avia.ticket_daemon.ticket_daemon.lib import feature_flags
from travel.avia.ticket_daemon.ticket_daemon.lib.currency import Price


@mock.patch.object(flights.feature_flags, 'store_min_tariff_per_fare_code', return_value=True)
class TestVariantsSaver(TestCase):

    def test_unify_variants_lesser_price(self, _):
        assert feature_flags.store_min_tariff_per_fare_code()
        base_variant = self._create_variant('SU 100', datetime(2021, 1, 1), True, 'Y', 150.00)
        new_variant = self._create_variant('SU 100', datetime(2021, 1, 1), True, 'Y', 139.38)
        result = unify_variants(
            variants=[new_variant],
            previous=[base_variant],
        )
        assert result == [new_variant]

    def test_unify_variants_greater_price(self, _):
        base_variant = self._create_variant('SU 100', datetime(2021, 1, 1), True, 'Y', 150.00)
        result = unify_variants(
            variants=[self._create_variant('SU 100', datetime(2021, 1, 1), True, 'Y', 179.78)],
            previous=[base_variant],
        )
        assert result == []

    def test_unify_variants_diff_by_flight(self, _):
        base_variant = self._create_variant('SU 100', datetime(2021, 1, 1), True, 'Y', 150.00)
        new_variant = self._create_variant('SU 101', datetime(2021, 1, 1), True, 'Y', 139.78)
        result = unify_variants(
            variants=[base_variant, new_variant],
            previous=[base_variant],
        )
        assert result == [new_variant]

    def test_unify_variants_diff_by_date(self, _):
        base_variant = self._create_variant('SU 100', datetime(2021, 1, 1), True, 'Y', 150.00)
        new_variant = self._create_variant('SU 100', datetime(2021, 1, 2), True, 'Y', 139.78)
        result = unify_variants(
            variants=[base_variant, new_variant],
            previous=[base_variant],
        )
        assert result == [new_variant]

    def test_unify_variants_diff_by_baggage(self, _):
        base_variant = self._create_variant('SU 100', datetime(2021, 1, 1), True, 'Y', 150.00)
        new_variant = self._create_variant('SU 100', datetime(2021, 1, 1), False, 'Y', 139.78)
        result = unify_variants(
            variants=[base_variant, new_variant],
            previous=[base_variant],
        )
        assert result == [new_variant]

    def test_unify_variants_diff_by_fare_code(self, _):
        base_variant = self._create_variant('SU 100', datetime(2021, 1, 1), True, 'Y', 150.00)
        new_variant = self._create_variant('SU 100', datetime(2021, 1, 1), True, 'M14K', 139.78)
        result = unify_variants(
            variants=[base_variant, new_variant],
            previous=[base_variant],
        )
        assert result == [new_variant]

    def _create_variant(self, flight_number, flight_date, with_baggage, fare_code, price):
        variant = flights.Variant()
        variant.forward.segments = [
            fill(
                flights.IATAFlight(),
                number=flight_number,
                local_departure=flight_date,
                baggage=with_baggage,
                fare_code=fare_code,
            )
        ]
        variant.tariff = Price(currency='RUR', value=price)

        return variant
