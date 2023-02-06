import travel.avia.flight_status_fetcher.settings.sources.region as region_settings
import travel.avia.flight_status_fetcher.sources.region_airports as region_airports


def test_regional_airports_search_segment_configuration():
    for code_group, values in region_airports.airport_code_groups.items():
        assert values, 'region airports airport_code_groups for %s is not filled' % code_group

    codes = set(k.lower() for k in region_airports.airport_code_groups.keys())
    airports = set(k.lower() for k in region_settings.REGION_AIRPORT_IATA.keys())

    for airport in airports:
        assert airport in codes, 'region airport %s is not found in the code groups config' % airport
