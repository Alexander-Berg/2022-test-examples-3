import os
import subprocess as sp
from datetime import datetime

import sys
import tarfile
import yatest
import yatest.common.runtime as runtime
from crypta.lib.env.jdk.java_runtime import java_path
from mapreduce.yt.python.yt_stuff import YtStuff
from pytest import fixture, yield_fixture
from yt.wrapper import YtClient

from test_utils import arcadia_local_source_path, read_arcadia_local_json_dump
from yt_dumper import YtCypressDumper

LOCAL_YQL_CLUSTER = "whatever"
NO_TOKEN = "whatever"


def log(msg):
    # logging.info(msg)
    sys.stderr.write(msg)


def dump_yt(yt, yt_path):
    yt_dump_dir = yatest.common.output_path("yt_dump")
    yt_dumper = YtCypressDumper(yt, yt_dump_dir)
    yt_dumper.dump_json(yt_path)


@yield_fixture(scope="session")
def local_yt():
    yt = YtStuff()
    yt.start_local_yt()
    yt_client = YtClient(proxy=yt.get_server(), token="")

    # temporary fix for YT-8463
    yt_client.mkdir("//tmp/yt_wrapper/file_storage/new_cache", recursive=True)

    try:
        yield yt_client
    finally:
        dump_yt(yt_client, "//home")
        yt.stop_local_yt()


@fixture(scope="session")
def soup_dump(local_yt):
    log("Reading soup dump...")
    cur_date = datetime.date(datetime.now())

    test_soup_table = "//home/crypta/production/state/graph/v2/soup/cooked/soup_edges"
    local_yt.create("table", test_soup_table, recursive=True)
    local_yt.write_table(test_soup_table, read_arcadia_local_json_dump("test_data/soup_edges.json"))

    local_yt.run_sort(test_soup_table, sort_by=["id1Type", "id2Type", "id1", "id2"])
    local_yt.set_attribute(test_soup_table, "generate_date", str(cur_date))

    test_vp_table = "//home/crypta/production/state/graph/v2/soup/cooked/vertices_properties"
    local_yt.create("table", test_vp_table, recursive=True)

    local_yt.write_table(test_vp_table, read_arcadia_local_json_dump("test_data/shared_vertices.json"))

    local_yt.run_sort(test_vp_table, sort_by=["id", "id_type"])


@yield_fixture(scope="function")
def run_java(local_yt):
    # project is packed as single uberjar
    classpath_jar = runtime.binary_path(os.path.join("crypta/graph/matching/human", "human-matching.jar"))

    native_libs_tar = tarfile.open(yatest.common.build_path("crypta/graph/matching/human/human-matching.tar"))
    native_libs_tar.extractall(path=yatest.common.work_path())
    native_libs_tar.close()

    cmd = [java_path(), "-Djava.library.path=.", "-classpath", classpath_jar]

    env = dict(
        YT_TOKEN=NO_TOKEN,
        YT_PROXY=local_yt.config["proxy"]["url"],
        YT_POOL="pool",
        YT_JAVA_BINARY=os.path.abspath(java_path()),
        YQL_TOKEN=NO_TOKEN,
        YQL_URL="{}:{}".format("localhost", "123"),
        YQL_YT_CLUSTER=LOCAL_YQL_CLUSTER,
        CRYPTA_GRAPH_CRYPTA_HOME="//home/crypta/production",
    )

    def _execute(*args):
        p = sp.Popen(cmd + list(args), stdout=sys.stderr, stderr=sys.stderr, env=env)
        out, err = p.communicate()
        rc = p.wait()
        if rc != 0:
            raise Exception(err)

    yield _execute


def test_run(run_java, local_yt, soup_dump):
    def matching_iteration(iter_n):
        log("Running matching test iter %d..." % iter_n)
        run_java(
            "ru.yandex.crypta.graph2.matching.human.HumanMatchingMain",
            "--config=" + arcadia_local_source_path("test_config.yaml"),
            "--copyToDict=true",
        )

    matching_yt_dir = "//home/crypta/production/state/graph/v2/matching"

    matching_iteration(0)
    print_yt_dir_tables(local_yt, matching_yt_dir)
    check_result(local_yt, matching_yt_dir)

    matching_iteration(1)
    print_yt_dir_tables(local_yt, matching_yt_dir)
    check_result(local_yt, matching_yt_dir)
    #
    # matching_iteration(2)
    # print_yt_dir_tables(local_yt, matching_yt_dir)
    # check_result(local_yt, matching_yt_dir)


def check_result(yt, matching_yt_dir):
    out_vertices_table = os.path.join(matching_yt_dir, "vertices_no_multi_profile")
    out_edges_table = os.path.join(matching_yt_dir, "edges_by_crypta_id")

    out_vertices_recs = list(yt.read_table(out_vertices_table))
    out_edges_recs = list(yt.read_table(out_edges_table))

    assert len(out_vertices_recs) > 0
    assert len(out_edges_recs) > 0

    assert yt.get_attribute(out_vertices_table, "generate_date") is not None
    assert yt.get_attribute(out_edges_table, "generate_date") is not None


def print_yt_dir_tables(yt, yt_path):
    assert yt.exists(yt_path)

    log("Content of %s:" % yt_path)

    child_nodes = yt.list(yt_path)
    for child_node in child_nodes:
        child_path = os.path.join(yt_path, child_node)

        if yt.get_attribute(yt_path, "type") == "table":
            recs_count = yt.row_count(child_path)
            log("%s:%d" % (child_node, recs_count))
