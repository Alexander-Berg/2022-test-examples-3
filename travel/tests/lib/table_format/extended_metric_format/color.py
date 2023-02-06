from travel.avia.country_restrictions.lib.table_format.extended_metrics_format import BannerColorEnum, \
    ExtendedMetricsFormat
from travel.avia.country_restrictions.lib.types.metric_type import ENTRANCE_FOR_RUSSIANS, EntranceForRussiansEnum, \
    FLIGHTS_AVAILABILITY_V2, FlightAvailabilityV2Enum, TOURISM_AVAILABILITY, QUARANTINE_REQUIRED, \
    AIRPORT_AVAILABILITY_IN_COUNTRY


def test_no_tourism():
    data = {
        TOURISM_AVAILABILITY.name: TOURISM_AVAILABILITY.generate_metric(None),
    }
    assert ExtendedMetricsFormat('', []).get_banner_color(data) == BannerColorEnum.GRAY


def test_closed_country():
    data = {
        TOURISM_AVAILABILITY.name: TOURISM_AVAILABILITY.generate_metric(False),
    }
    assert ExtendedMetricsFormat('', []).get_banner_color(data) == BannerColorEnum.RED


def test_has_quarantine():
    data = {
        TOURISM_AVAILABILITY.name: TOURISM_AVAILABILITY.generate_metric(True),
        QUARANTINE_REQUIRED.name: QUARANTINE_REQUIRED.generate_metric(True),
    }
    assert ExtendedMetricsFormat('', []).get_banner_color(data) == BannerColorEnum.GRAY


def test_green_banner():
    data = {
        TOURISM_AVAILABILITY.name: TOURISM_AVAILABILITY.generate_metric(True),
        QUARANTINE_REQUIRED.name: QUARANTINE_REQUIRED.generate_metric(False),
        FLIGHTS_AVAILABILITY_V2.name: FLIGHTS_AVAILABILITY_V2.generate_metric(FlightAvailabilityV2Enum.DIRECT_FLIGHTS),
        ENTRANCE_FOR_RUSSIANS.name: ENTRANCE_FOR_RUSSIANS.generate_metric(EntranceForRussiansEnum.HAS_RESTRICTIONS),
    }
    assert ExtendedMetricsFormat('', []).get_banner_color(data) == BannerColorEnum.GREEN


def test_green_banner_in_country_with_no_airports():
    data = {
        TOURISM_AVAILABILITY.name: TOURISM_AVAILABILITY.generate_metric(True),
        QUARANTINE_REQUIRED.name: QUARANTINE_REQUIRED.generate_metric(False),
        FLIGHTS_AVAILABILITY_V2.name: FLIGHTS_AVAILABILITY_V2.generate_metric(FlightAvailabilityV2Enum.NO_FLIGHTS),
        ENTRANCE_FOR_RUSSIANS.name: ENTRANCE_FOR_RUSSIANS.generate_metric(EntranceForRussiansEnum.HAS_RESTRICTIONS),
        AIRPORT_AVAILABILITY_IN_COUNTRY.name: AIRPORT_AVAILABILITY_IN_COUNTRY.generate_metric(False),
    }
    assert ExtendedMetricsFormat('', []).get_banner_color(data) == BannerColorEnum.GREEN
