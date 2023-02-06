from unittest import TestCase

from travel.avia.library.python.avia_data.models import CompanyTariff
from travel.avia.ticket_daemon.ticket_daemon.api.flights import IATAFlight
from travel.avia.ticket_daemon.ticket_daemon.lib.baggage_corrector import BaggageCorrector, NumberRangeRule
from travel.avia.ticket_daemon.ticket_daemon.lib import baggage


class TestBaggageCorrector(TestCase):
    def setUp(self):
        self._baggage_corrector = BaggageCorrector()
        self._rule_baggage = baggage.Baggage.from_db(1, 1, 50)
        self._baggage_corrector.add_rule(NumberRangeRule('AA 1234', 'AA 2000', self._rule_baggage))

    def test_with_correction(self):
        flight = IATAFlight()
        flight.number = 'AA 1236'
        flight.company_tariff = CompanyTariff(
            mask='',
            baggage_allowed=True,
            baggage_norm=100,
            baggage_pieces=2,
            published=True,
        )
        corrected_baggage = self._baggage_corrector.correct_baggage(flight)

        assert corrected_baggage.as_dict() == self._rule_baggage.as_dict()

    def test_without_correction(self):
        flight = IATAFlight()
        flight.number = 'AA 5000'
        flight.company_tariff = CompanyTariff(
            mask='',
            baggage_allowed=True,
            baggage_norm=100,
            baggage_pieces=2,
            published=True,
        )
        tariff_baggage = baggage.Baggage.from_airline_tariff(flight.company_tariff)
        corrected_baggage = self._baggage_corrector.correct_baggage(flight)
        assert corrected_baggage.as_dict() == tariff_baggage.as_dict()

    def test_do_not_correct_not_default_airline_tariffs(self):
        flight = IATAFlight()
        flight.number = 'AA 1236'
        flight.company_tariff = CompanyTariff(
            mask='OLOLO',
            baggage_allowed=True,
            baggage_norm=100,
            baggage_pieces=2,
            published=True,
        )
        tariff_baggage = baggage.Baggage.from_airline_tariff(
            flight.company_tariff)
        corrected_baggage = self._baggage_corrector.correct_baggage(flight)

        assert corrected_baggage.as_dict() == tariff_baggage.as_dict()

    def test_correct_None_airline_tariffs(self):
        flight = IATAFlight()
        flight.number = 'AA 1236'
        flight.company_tariff = None
        corrected_baggage = self._baggage_corrector.correct_baggage(flight)

        assert corrected_baggage.as_dict() == self._rule_baggage.as_dict()
