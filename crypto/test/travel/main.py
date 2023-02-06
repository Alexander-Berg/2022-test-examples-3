import yatest.common
from yt.wrapper import ypath

from crypta.lib.python import time_utils
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tables,
    tests,
)


def test_travelers(local_yt, local_yt_and_yql_env, config_file, config):
    diff = tests.Diff()
    test = tests.TestNodesInMapNode(tests_getter=[diff], tag="output")

    local_yt_and_yql_env[time_utils.CRYPTA_FROZEN_TIME_ENV] = "1623099600"  # 2021-06-08 00:00:00

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/profile/services/precalculate_tables/bin/crypta-profile-precalculate-tables"),
        args=[
            "--config", config_file,
            "travel",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (_crypta_id_matching(config), [tests.TableIsNotChanged()]),
            (_crypta_id_regions(config), [tests.TableIsNotChanged()]),
            (_rasp_settlment(config), [tests.TableIsNotChanged()]),
            (_rasp_station(config), [tests.TableIsNotChanged()]),
            (_rasp_users_search_log(config), [tests.TableIsNotChanged()]),
            (_hotels_table(config), [tests.TableIsNotChanged()]),
        ],
        output_tables=[
            (cypress.CypressNode(config.TravelCountriesDir), [test]),
            (cypress.CypressNode(config.TravelCitiesDir), [test]),
            (cypress.CypressNode(config.TravelRegionsDir), [test]),
            (cypress.CypressNode(config.TypesOfTripDir), [test]),
            (tables.YsonTable("processed_tables.yson", config.RaspUsersSearchLog.TrackTable, yson_format="pretty"), [diff]),
            (tables.YsonTable("future_trips.yson", config.FutureTripsTable, yson_format="pretty"), [diff]),
        ],
        env=local_yt_and_yql_env,
    )


def _crypta_id_matching(config):
    schema = schema_utils.get_strict_schema([
        {"name": "id", "type": "string"},
        {"name": "id_type", "type": "string"},
        {"name": "cryptaId", "type": "string"},
    ])
    return tables.get_yson_table_with_schema("crypta_id_matching.yson", config.CryptaIdMatchingTable, schema)


def _crypta_id_regions(config):
    schema = schema_utils.get_strict_schema([
        {"name": "crypta_id", "type": "string"},
        {"name": "main_region_country", "type": "int32"},
    ])
    return tables.get_yson_table_with_schema("crypta_id_regions.yson", config.CryptaIdRegionsTable, schema)


def _rasp_settlment(config):
    schema = schema_utils.get_strict_schema([
        {"name": "Id", "type": "int32"},
        {"name": "GeoId", "type": "int32"},
    ])
    return tables.get_yson_table_with_schema("rasp_settlement.yson", config.RaspSettlementTable, schema)


def _rasp_station(config):
    schema = schema_utils.get_strict_schema([
        {"name": "Id", "type": "int32"},
        {"name": "SettlementId", "type": "int32"},
    ])
    return tables.get_yson_table_with_schema("rasp_station.yson", config.RaspStationTable, schema)


def _rasp_users_search_log(config):
    schema = schema_utils.get_strict_schema([
        {"name": "yandexuid", "type": "string"},
        {"name": "to_id", "type": "string"},
        {"name": "when", "type": "string"},
        {"name": "children", "type": "string"},
        {"name": "adults", "type": "string"},
        {"name": "infants", "type": "string"},
    ])
    return tables.get_yson_table_with_schema("rasp_user_search_log.yson", ypath.ypath_join(config.RaspUsersSearchLog.SourceDir, "2021-06-08"), schema)


def _hotels_table(config):
    schema = schema_utils.get_strict_schema([
        {"name": "uid", "type": "string"},
        {"name": "uuid", "type": "string"},
        {"name": "passport_uid", "type": "string"},
        {"name": "crypta_id", "type": "string"},
        {"name": "hotel_geo_id", "type": "int32"},
        {"name": "checkin_date", "type": "string"},
        {"name": "datefield", "type": "string"},
        {"name": "occupancy", "type": "string"},
        {"name": "check_out", "type": "string"},
    ])
    return tables.get_yson_table_with_schema("hotels_table.yson", config.HotelsTable, schema)
