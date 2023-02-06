from travel.avia.country_restrictions.lib.types.metric_type import FlightAvailabilityV2Enum, FLIGHTS_AVAILABILITY_V2
from travel.avia.country_restrictions.parsers.assessors_main.subparsers.parse_flight_availability_v2 import parser


def test_simple():
    values = [
        ('flight_with_transfers', FlightAvailabilityV2Enum.TRANSFER_FLIGHTS),
        ('no_flights', FlightAvailabilityV2Enum.NO_FLIGHTS),
        ('direct_flights', FlightAvailabilityV2Enum.DIRECT_FLIGHTS),
    ]
    for assessors_value, internal_value in values:
        row = {
            'avia': assessors_value,
        }

        actual = parser(context={}, row=row).get(FLIGHTS_AVAILABILITY_V2.name, None)
        expected = FLIGHTS_AVAILABILITY_V2.generate_metric(internal_value)
        assert actual == expected
        assert actual.value == internal_value.value.value
