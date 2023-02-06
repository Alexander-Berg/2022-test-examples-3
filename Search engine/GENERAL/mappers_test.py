import yatest.common

from search.geo.tools.personal_pois.lib.extract_info_mappers import (
    OrgvisitsMapper,
    OrgvisitsInfoValidateMapper,
    AltayRubricMapper,
    AltayCompanyMapper,
    AltayInfoValidateMapper,
    BaseMapMapper,
    BaseMapInfoValidateMapper,
    CommonAddressesMapper,
    CommonAddressesInfoValidateMapper,
    RegulargeoMapper,
    MapsLogsMapper,
    MapsLogsInfoValidateMapper
)

from search.geo.tools.personal_pois.lib.mappers import (
    FeaturesMapper,
    RubricZoomMapper,
    SpaceOidInfoMapper,
    SpacePuidInfoMapper,
)

from search.geo.tools.personal_pois.lib.ut.mappers_rows import (
    altay_rubric_mapper_rows,
    altay_company_mapper_rows,
    altay_info_validate_mapper_rows,
    orgvisits_mapper_rows,
    orgvisits_info_validate_mapper_rows,
    base_map_mapper_rows,
    base_map_info_validate_mapper_rows,
    common_addresses_mapper_rows,
    common_addresses_info_validate_mapper_rows,
    regular_geo_mapper_rows,
    maps_logs_mapper_rows,
    maps_logs_info_validate_mapper_rows,

    feature_mapper_rows,
    rubric_zoom_mapper_rows,
    space_oid_info_mapper_rows,
    space_puid_info_mapper_rows,
)


def mapper_test(mapper, rows):
    for row in rows:
        result = list(mapper(row['input']))
        assert result == row['output']


def test_orgvisits_mapper():
    mapper_test(OrgvisitsMapper(), orgvisits_mapper_rows)


def test_orgvisits_info_validate_mapper():
    mapper_test(OrgvisitsInfoValidateMapper(), orgvisits_info_validate_mapper_rows)


def test_altay_rubric_mapper():
    mapper_test(AltayRubricMapper(), altay_rubric_mapper_rows)


def test_altay_company_mapper():
    mapper_test(AltayCompanyMapper(), altay_company_mapper_rows)


def test_altay_info_validate_mapper():
    mapper_test(AltayInfoValidateMapper(), altay_info_validate_mapper_rows)


def test_base_map_mapper():
    mapper_test(BaseMapMapper(), base_map_mapper_rows)


def test_base_map_info_validate_mapper():
    mapper_test(BaseMapInfoValidateMapper(), base_map_info_validate_mapper_rows)


def test_common_addresses_mapper():
    mapper_test(CommonAddressesMapper(), common_addresses_mapper_rows)


def test_common_addresses_info_validate_mapper():
    mapper_test(CommonAddressesInfoValidateMapper(), common_addresses_info_validate_mapper_rows)


def test_regular_geo_mapper():
    mapper_test(RegulargeoMapper(), regular_geo_mapper_rows)


def test_maps_logs_mapper():
    geodata_path = yatest.common.data_path('geo/geodata5.bin')
    mapper = MapsLogsMapper(geodata_path)
    mapper.start()
    mapper_test(mapper, maps_logs_mapper_rows)


def test_maps_logs_info_validate_mapper():
    mapper_test(MapsLogsInfoValidateMapper(), maps_logs_info_validate_mapper_rows)


def test_feature_mapper():
    mapper_test(
        FeaturesMapper(['key'], [lambda info: {'my_feature': info}]),
        feature_mapper_rows
    )


def test_rubric_zoom_mapper():
    mapper = RubricZoomMapper(['key'], lambda x: x['condition'], '', min_zoom=15)
    mapper.rubric_to_zoom = {'0': 15, '1': 14, '2': 16, '3': 17}
    mapper_test(mapper, rubric_zoom_mapper_rows)


def test_space_oid_info_mapper():
    mapper_test(SpaceOidInfoMapper(), space_oid_info_mapper_rows)


def test_space_puid_info_mapper():
    mapper_test(SpacePuidInfoMapper(), space_puid_info_mapper_rows)
