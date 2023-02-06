import decimal

from ..core.client import StubCalculatorClient


class CalculatorTestHelper:

    def __init__(self, tc):
        self._tc = tc

    def setUp(self):
        self.set_carsharing_base_default_tariff(
            ride_cost_per_minute=5,
            parking_cost_per_minute=2,
        )
        self._tc.addCleanup(StubCalculatorClient.clear)

    def set_carsharing_base_default_tariff(self, ride_cost_per_minute, parking_cost_per_minute):
        StubCalculatorClient.set_carsharing_base_default_tariff(
            ride_cost_per_minute=decimal.Decimal(ride_cost_per_minute),
            parking_cost_per_minute=decimal.Decimal(parking_cost_per_minute),
        )

    def set_carsharing_base_tariff_per_car(self, car,
                                           ride_cost_per_minute, parking_cost_per_minute):
        StubCalculatorClient.set_carsharing_base_tariff_per_car(
            car=car,
            ride_cost_per_minute=decimal.Decimal(ride_cost_per_minute),
            parking_cost_per_minute=decimal.Decimal(parking_cost_per_minute),
        )
