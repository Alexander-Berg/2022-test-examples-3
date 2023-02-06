from travel.avia.country_restrictions.lib.types.metric_type import FLIGHTS_AVAILABILITY, FLIGHTS_AVAILABILITY_V2
from travel.avia.country_restrictions.aggregator.metric_postprocess.remove_flights_availability_if_russia import \
    processor

from travel.avia.country_restrictions.tests.mocks.geo_format_manager import MockGeoFormatManager


geo_format_manager = MockGeoFormatManager(
    connections=[
        ('l225', 225),
        ('r1', 1),
    ],
    titles={
        'l225': 'Россия',
    },
    parents={
        'r1': ['l225'],
    }
)


def helper(point_key, initial):
    result = processor('l225', initial, geo_format_manager)
    assert FLIGHTS_AVAILABILITY.name not in result
    assert FLIGHTS_AVAILABILITY_V2.name not in result


def test_both_flight_fields():
    for point_key in ['r1', 'l225']:
        helper(
            point_key,
            {
                FLIGHTS_AVAILABILITY.name: FLIGHTS_AVAILABILITY.generate_metric(True),
                FLIGHTS_AVAILABILITY_V2.name: FLIGHTS_AVAILABILITY_V2.generate_metric(True),
            },
        )


def test_empty():
    for point_key in ['r1', 'l225']:
        helper(point_key, {})


def test_not_russia():
    initial = {
        FLIGHTS_AVAILABILITY.name: FLIGHTS_AVAILABILITY.generate_metric(True),
        FLIGHTS_AVAILABILITY_V2.name: FLIGHTS_AVAILABILITY_V2.generate_metric(True),
    }

    result = processor('l1', initial, geo_format_manager)
    assert FLIGHTS_AVAILABILITY.name in result
    assert FLIGHTS_AVAILABILITY_V2.name in result
