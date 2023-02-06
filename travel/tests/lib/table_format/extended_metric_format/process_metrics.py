from travel.avia.country_restrictions.lib.table_format.extended_metrics_format import ExtendedMetricsFormat
from travel.avia.country_restrictions.lib.types.metric_type import FLIGHTS_AVAILABILITY_V2, FlightAvailabilityV2Enum, \
    METRICS_FOR_EXTENDED_BANNER, TOURISM_AVAILABILITY, VISA_REQUIRED, PCR_EXPIRATION_PERIOD_IN_HOURS_OR_REQUIRED, \
    QUARANTINE_REQUIRED


def test_case_1():
    data = {
        TOURISM_AVAILABILITY.name: TOURISM_AVAILABILITY.generate_metric(True),
        FLIGHTS_AVAILABILITY_V2.name: FLIGHTS_AVAILABILITY_V2.generate_metric(FlightAvailabilityV2Enum.DIRECT_FLIGHTS),
        VISA_REQUIRED.name: VISA_REQUIRED.generate_metric(False),
    }
    assert ExtendedMetricsFormat('', METRICS_FOR_EXTENDED_BANNER).process_metrics(data) == [
        FLIGHTS_AVAILABILITY_V2.generate_extended_metric(
            FLIGHTS_AVAILABILITY_V2.generate_metric(FlightAvailabilityV2Enum.DIRECT_FLIGHTS),
        ),
        PCR_EXPIRATION_PERIOD_IN_HOURS_OR_REQUIRED.try_generate_extended_none_metric(),
        QUARANTINE_REQUIRED.try_generate_extended_none_metric(),
        VISA_REQUIRED.generate_extended_metric(VISA_REQUIRED.generate_metric(False)),
    ]
