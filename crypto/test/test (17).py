import json
import os
import subprocess as sp
import tarfile
import sys
import yatest
import yatest.common.runtime as runtime
from crypta.lib.env.jdk.java_runtime import java_path
from pytest import fixture, yield_fixture

from yt.wrapper import YtClient

from mapreduce.yt.python.table_schema import extract_column_attributes

LOCAL_YQL_CLUSTER = "plato"

NO_TOKEN = "1"


def log(msg):
    sys.stderr.write(msg)


def source_path(relative_path):
    return os.path.join(yatest.common.source_path("crypta/graph/soup/prepare/test"), relative_path)


@yield_fixture(scope="function")
def local_yt_and_yql(tmpdir, yql_api, mongo, yt):
    log("Prepared yt and yql...")
    yield yt, yql_api


@fixture(scope="function")
def yt_client(local_yt_and_yql):
    local_yt, _ = local_yt_and_yql

    yt_client = YtClient(proxy=local_yt.get_server(), token="")

    # temporary fix for YT-8463
    yt_client.mkdir("//tmp/yt_wrapper/file_storage/new_cache", recursive=True)

    return yt_client


@yield_fixture(scope="function")
def run(local_yt_and_yql):
    local_yt, local_yql = local_yt_and_yql

    # for some reason libsoup-model_java.so is packed in tar
    native_libs_tar = tarfile.open(yatest.common.build_path("crypta/graph/soup/prepare/prepare-soup.tar"))
    native_libs_tar.extractall(path=yatest.common.work_path())
    native_libs_tar.close()

    env = dict(
        YT_TOKEN=NO_TOKEN,
        YT_PROXY=local_yt.get_server(),
        YT_POOL="pool",
        YT_JAVA_BINARY=os.path.abspath(java_path()),
        YQL_TOKEN=NO_TOKEN,
        YQL_URL="{}:{}".format("localhost", local_yql.port),
        YQL_YT_CLUSTER=LOCAL_YQL_CLUSTER,
        CRYPTA_GRAPH_CRYPTA_HOME="//home/crypta/production",
    )

    def get_classpath_jar(jar):
        # project is packed as single uberjar
        if jar == "prepare":
            return runtime.binary_path(os.path.join("crypta/graph/soup/prepare", "prepare-soup.jar"))
        elif jar == "dynamic":
            return runtime.binary_path(os.path.join("crypta/graph/soup/prepare_dynamic", "prepare-soup-dynamic.jar"))
        else:
            raise Exception("Incorect jar {}".format(jar))

    def _execute(jar, *args):
        cmd = [java_path(), "-Djava.library.path=.", "-classpath", get_classpath_jar(jar)]
        p = sp.Popen(cmd + list(args), stdout=sys.stderr, stderr=sys.stderr, env=env)
        out, err = p.communicate()
        rc = p.wait()
        if rc != 0:
            raise Exception(err)

    yield _execute


def read_tar_json_dump(filename):
    tar = tarfile.open(filename, "r:gz")
    for member in tar.getmembers():
        f = tar.extractfile(member)
        if f is not None:
            print(member)
            for idx, line in enumerate(f):
                if idx % 100000 == 0:
                    print(idx)

                line = line.strip()
                yield line


def read_arcadia_json_dump_recs(filename):
    # assumes that recs are in json array
    with open(source_path(filename)) as f:
        for rec in json.load(f):
            yield rec


@fixture(scope="function")
def input_tables(yt_client):
    socdem_storage_table = "//home/crypta/production/profiles/external-profiles/sources/puid/passport"
    yt_client.create("table", socdem_storage_table, recursive=True)
    yt_client.write_table(socdem_storage_table, read_arcadia_json_dump_recs("test_data/socdem_storage_table.json"))

    uuids_table = "//home/crypta/production/ids_storage/uuid/app_metrica_month"
    yt_client.create("table", uuids_table, recursive=True)

    device_ids_table = "//home/crypta/production/ids_storage/device_id/app_metrica_month"
    yt_client.create("table", device_ids_table, recursive=True)
    yt_client.write_table(device_ids_table, read_arcadia_json_dump_recs("test_data/device_id-app_metrica_month.json"))

    yandexuids_table = "//home/crypta/production/ids_storage/yandexuid/yuid_with_all_info"
    yt_client.create("table", yandexuids_table, recursive=True)
    yt_client.write_table(yandexuids_table, read_arcadia_json_dump_recs("test_data/yandexuid-yuid_with_all_info.json"))

    one_of_soup_tables = "//home/crypta/production/state/graph/v2/soup/mm_device_id_mac_app-metrica_mm"
    yt_client.create("table", one_of_soup_tables, recursive=True)
    yt_client.write_table(
        one_of_soup_tables, read_arcadia_json_dump_recs("test_data/soup-device_id_mac_app-metrica_mm.json")
    )

    shared_tables = "//home/crypta/production/ids_storage/shared/common_shared"
    yt_client.create("table", shared_tables, recursive=True)
    yt_client.write_table(shared_tables, read_arcadia_json_dump_recs("test_data/shared-common_shared.json"))

    # TODO: remove tmp
    outliers_table = "//home/crypta/production/state/graph/v2/soup/likelihood/outliers_tmp"
    yt_client.create("table", outliers_table, recursive=True)
    yt_client.write_table(outliers_table, read_arcadia_json_dump_recs("test_data/outliers.json"))


def test_run(run, yt_client, input_tables):
    log("Running java binary...")

    test_path = "ru.yandex.crypta.graph2.soup.PrepareSoupMain"
    run("prepare", test_path, "--config=" + source_path("test_config.yaml"))

    log("Test {} completed".format(test_path))

    out_vertices_properties_table = "//home/crypta/production/state/graph/v2/soup/cooked/vertices_properties"
    out_vertices_properties_recs = list(yt_client.read_table(out_vertices_properties_table))
    schema = yt_client.get_attribute(out_vertices_properties_table, "schema")
    schema = extract_column_attributes(schema)
    shared_recs = [rec for rec in out_vertices_properties_recs if rec["source"] == "common_shared"]
    assert len(shared_recs) == 2

    assert len(out_vertices_properties_recs) > 0
    assert {"required": False, "type": "int64", "name": "main_region"} in schema

    test_path = "ru.yandex.crypta.graph2.soup.dynamic.PrepareSoupDynamicMain"
    run("dynamic", test_path, "--config=" + source_path("test_dynamic_config.yaml"))

    log("Test Dyn {} completed".format(test_path))

    out_vertices_properties_dyn_table = "//home/crypta/production/state/graph/v2/soup/cooked/vertices_properties_dynamic"
    out_vertices_properties_dyn_recs = list(yt_client.read_table(out_vertices_properties_dyn_table))

    assert len(out_vertices_properties_dyn_recs) == 9
