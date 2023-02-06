import json
import os

from copy import deepcopy
from collections import defaultdict

import allure
import pytest

import crypta.graph.v1.tests.testdata.testdata_helper as testdata_helper
from crypta.graph.v1.tests.testdata.market_proto import get_market_attribute

MASK_FOR_DETECT_TEST_DATA = "testdata_"


def select_test_data(source):
    return [source.__dict__.get(name) for name in dir(source) if name and MASK_FOR_DETECT_TEST_DATA in name]


def prepare_dirs(yt, dt, registry):
    # TODO: why can't we create dirs from test data
    paths = (
        "//crypta/production/classification/exact_socdem_storage",
        "//crypta/production/lal_manager",
        "//crypta/production/state/extras/reference-bases",
        "//crypta/production/state/graph/dicts/passport",
        "//crypta/production/state/graph/dicts/yamoney",
        "//crypta/production/state/graph/v2/soup/day/tmp",
        "//crypta/production/state/graph/v2/soup/dumps",
        "//crypta/production/state/graph/indevice/2016-04-09",
        "//crypta/production/state/graph/indevice/2016-04-10",
        "//crypta/production/state/graph/indevice/2016-04-11",
        "//crypta/production/state/iscrypta",
        "//home/freshness/crypta",
        "//home/logfeller/logs",
        "//home/market/production/crm/platform/facts",
        "//statbox/hypercube/mobile/reactivation/v2/by_device_id",
    )
    for path in paths:
        yt.mkdir(path, recursive=True)

    for log in registry:
        table_names = log.get_log().keys()
        for tbl in table_names:
            path = "/".join(tbl.split("/")[:-1])
            if not yt.exists(path):
                yt.mkdir(path, recursive=True)


def get_type(key, value, path):  # noqa
    """Simple schematize choices"""
    if any(
        ((path.endswith("rtb_log_apps") and key == "device_id"), (path.endswith("all_radius_ips") and key == "ip"))
    ):
        # rtb log has Yson as device_id field
        # radius ips log has Yson as ip field
        return "any"
    if key == "_logfeller_timestamp":
        # Error: incompatible WeakField types: Uint64?!=Int64?
        return "uint64"
    if key in {"gaids", "idfas", "oaids"}:
        return "any"
    if type(value) in (bool,):
        return "boolean"
    if isinstance(value, int):
        return "int64"
    if isinstance(value, float):
        return "double"
    if isinstance(value, (dict, list)):
        return "any"
    return "string"


def get_sort(key, value, path):
    """
    Insert order into schema for sorted table
    to fix error with 'requirement expectedSortedBy == realSortedBy failed'
    """
    if value == "yuid" and path.endswith("/yuid_with_all"):
        return True
    return False


def force_infer_schema(data, path):
    """Read all table rows and accomulate key, value pairs to make maximum full shema"""
    record = {}
    for item in data:
        record.update(item)
    schema = []
    for key, value in record.items():
        type_ = get_type(key, value, path)
        sort = get_sort(key, value, path)
        record = {"name": key, "type": type_}
        if sort:
            record["sort_order"] = "ascending"
        schema.append(record)
    return schema


def create_log(yt, dt):  # C901 # noqa
    import data_set

    prepare_dirs(yt, dt, testdata_helper.BaseLog.registry)

    test_data = select_test_data(data_set)

    with pytest.allure.step("Test data created"):
        for d in test_data:
            one_log_data = defaultdict(list)
            for k, v in d.get_log().iteritems():
                one_log_data[k].extend(v)

            for path, rows in one_log_data.iteritems():
                allure.attach(str(path), json.dumps(rows, sort_keys=True, indent=4))
                attributes = deepcopy(d.attributes)
                if "schema" not in attributes:
                    if any(
                        (
                            key in path
                            for key in (
                                "logs",
                                "statbox",
                                "logfeller",
                                "all_radius_ips",
                                "dicts/devid_hash",
                                "dicts/yuid_with_all",
                                "profiles/export",
                                "crypta-tests/stuff/state",
                                "v2",
                                "crypta/production/ids_storage",
                            )
                        )
                    ):
                        # make schema for logs
                        attributes["schema"] = force_infer_schema(rows, path)

                yt.create("table", yt.TablePath(path, append=d.append), recursive=True, attributes=attributes)
                yt.write_table(yt.TablePath(path, append=d.append), [record for record in rows], raw=False)

        # partners data
        for log in testdata_helper.BaseLog.registry:
            if isinstance(log, testdata_helper.SingleTableLog):
                # create folders for single log tables
                yt.mkdir(log.folder_path, recursive=True)

                one_log_data = defaultdict(list)

                for k, v in log.get_log().iteritems():
                    one_log_data[k].extend(v)

                for path, rows in one_log_data.iteritems():
                    allure.attach(
                        "Filling YT table {} with {} records".format(str(log.folder_path), len(rows)),
                        json.dumps(rows, sort_keys=True, indent=4),
                    )
                    yt.write_table(yt.TablePath(path, append=log.append), [record for record in rows], raw=False)

        # market orders
        attribute = get_market_attribute()
        yt.create(
            "table",
            "//home/market/production/crm/platform/facts/Order",
            recursive=True,
            attributes={"schema": [{"name": "fact", "type": "string"}, {"name": "timestamp", "type": "uint64"}]},
        )
        yt.set_attribute("//home/market/production/crm/platform/facts/Order", "_yql_proto_field_fact", attribute)
        yt.write_table(
            "//home/market/production/crm/platform/facts/Order",
            [record for record in data_set.market_orders],
            raw=False,
        )

        paths = (
            "//crypta/production/state/graph/v2/matching/edges_by_crypta_id",
            "//crypta/production/state/graph/v2/matching/direct_yandexuid_by_id_type_and_id",
            "//crypta/production/ids_storage/yandexuid/yuid_with_all_info",
            "//crypta/production/state/graph/v2/matching/vertices_no_multi_profile_by_id_type",
            "//crypta/production/profiles/stages/raw-yandexuid/2019-01-17",
            "//crypta/production/profiles/stages/raw-yandexuid/2019-01-18",
            "//crypta/production/state/graph/shared/heuristic_desktop_yuids/2016-04-10",
        )

        data = (
            data_set.edges_by_crypta_id,
            data_set.direct_yandexuid_by_id_type_and_id,
            data_set.yuid_with_all_info,
            data_set.vertices_no_multi_profile_by_id_type,
            data_set.profile_2019_01_17,
            data_set.profile_2019_01_18,
            data_set.heuristic_shared_desktop_yuids,
        )

        for path, rows in zip(paths, data):
            attributes = {"schema": force_infer_schema(rows, path)}
            yt.create("table", path, recursive=True, attributes=attributes)
            yt.write_table(path, rows)

        def mk_yql_schema(str_fields=None, list_fields=None):
            str_fields = str_fields or []
            list_fields = list_fields or []
            typelist = []
            result = {"StrictSchema": True, "Type": ["StructType", typelist]}
            for f in str_fields:
                typelist.append([f, ["DataType", "String"]])
            for f in list_fields:
                typelist.append([f, ["ListType", ["DataType", "String"]]])
            return result

        # Set yql schema for soup tables
        tbls = yt.search("//crypta/production/state/graph/v2/soup", node_type="table", depth_bound=1)
        for t in tbls:
            yt.set_attribute(
                t,
                "_yql_row_spec",
                mk_yql_schema(
                    str_fields=["id1", "id1Type", "id2", "id2Type", "sourceType", "logSource"], list_fields=["dates"]
                ),
            )


def create_stream_data(yt):  # C901 # noqa
    root = os.getenv("SOURCE_ROOT")
    for fixture in (
        "{root}/graph/v1/tests/testdata/fixtures/stream_postback_extra_data.json".format(root=root),
        "{root}/graph/v1/tests/testdata/fixtures/stream_soup.json".format(root=root),
        "{root}/graph/v1/tests/testdata/fixtures/stream_metrika.json".format(root=root),
        "{root}/graph/v1/tests/testdata/fixtures/stream_rtblog.json".format(root=root),
    ):

        with open(fixture, "r") as ifile:
            data = json.loads(ifile.read())

        for path, content in data.iteritems():
            yt.create(
                "table",
                path=path,
                recursive=True,
                ignore_existing=True,
                attributes={"schema": force_infer_schema(content, path)},
            )
            yt.write_table(path, content)

            if "am_log_table" in path:
                name = "am_log_table"
            elif "dev_info" in path:
                name = "dev_info_yt"
            elif "uuid_info" in path:
                name = "uuid_info_yt"
            elif "fuzzy2" in path:
                name = "fuzzy2_metrica"
            elif "stream/soup" in path:
                name = "soup"
            elif "ssp_apps_info_table" in path:
                name = "rtb_extra_data"
            elif "postback_apps_table" in path:
                name = "postback_extra_data"
            else:
                continue

            if name == "soup":
                yt.set_attribute(
                    path,
                    "_yql_row_spec",
                    {
                        "UseTypeV2": False,
                        "Type": [
                            "StructType",
                            [
                                ["dates", ["ListType", ["DataType", "String"]]],
                                ["id1", ["OptionalType", ["DataType", "String"]]],
                                ["id1Type", ["OptionalType", ["DataType", "String"]]],
                                ["id2", ["OptionalType", ["DataType", "String"]]],
                                ["id2Type", ["OptionalType", ["DataType", "String"]]],
                                ["logSource", ["OptionalType", ["DataType", "String"]]],
                                ["sourceType", ["OptionalType", ["DataType", "String"]]],
                            ],
                        ],
                        "StrictSchema": True,
                    },
                )
            else:
                spec_json = "{root}/graph/data_import/app_metrica_month/tests/fixtures/{name}.spec.json".format(
                    root=root, name=name
                )

                with open(spec_json) as ifile:
                    spec = json.loads(ifile.read())
                yt.set_attribute(path, "_yql_row_spec", spec)


def create_fixtures_data(yt, data_path, log_path):
    root = os.getenv("SOURCE_ROOT")
    content = []
    with open(data_path.format(root=root), "r") as ifile:
        for row in ifile:
            content.append(json.loads(row))

    try:
        with open(data_path.format(root=root).replace(".json", ".spec.json")) as ifile:
            spec = json.loads(ifile.read())
    except:
        with open(data_path.format(root=root).replace(".json", ".attrs.json")) as ifile:
            spec = json.loads(ifile.read())

    yt.create("table", path=log_path, recursive=True, ignore_existing=True, attributes=spec)
    yt.write_table(log_path, content)


def create_fixtures_data_by_days(yt, data_path, log_path):
    for fixture, dt in ((data_path, "2016-04-09"), (data_path, "2016-04-10"), (data_path, "2016-04-11")):
        dated_log_path = log_path.format(dt=dt)
        create_fixtures_data(yt, fixture, dated_log_path)


def create_log_data(yt):
    create_fixtures_data_by_days(
        yt,
        data_path="{root}/graph/data_import/webvisor/tests/fixtures/webvisor.json",
        log_path="//home/logfeller/logs/metrika-reduced-webvisor-to-crypta-log/1d/{dt}",
    )
    create_fixtures_data_by_days(
        yt,
        data_path="{root}/graph/data_import/fp_parser/tests/fixtures/reqans_log.json",
        log_path="//home/logfeller/logs/search-proto-reqans-log/1d/{dt}",
    )
    create_fixtures_data_by_days(
        yt,
        data_path="{root}/graph/data_import/fp_parser/tests/fixtures/reqans_log.json",
        log_path="//home/logfeller/logs/search-report-alice-log/1d/{dt}",
    )
    create_fixtures_data_by_days(
        yt,
        data_path="{root}/graph/data_import/passport/tests/fixtures/passport.json",
        log_path="//home/passport/production/userdata/{dt}",
    )


def create_dump_data(yt):
    create_fixtures_data(
        yt,
        data_path="{root}/graph/data_import/metrika_user_params/tests/fixtures/param_owners_01.json",
        log_path="//home/metrika/userparams/param_owners_01",
    )
    create_fixtures_data(
        yt,
        data_path="{root}/graph/data_import/metrika_user_params/tests/fixtures/mobmet_app_id_and_domains.json",
        log_path="//home/metrica-analytics/firstsvet/MOBMET/applications_and_sites/app_id_and_domains",
    )
    create_fixtures_data(
        yt,
        data_path="{root}/graph/data_import/metrika_user_params/tests/fixtures/mobmet_counters_and_domains.json",
        log_path="//home/metrica-analytics/firstsvet/MOBMET/applications_and_sites/counters_and_domains",
    )
