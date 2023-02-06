from __future__ import print_function

import itertools
import os
import shutil
import socket  # noqa
import subprocess
import sys
from datetime import datetime

import pytest

from Crypto.PublicKey import RSA
import yt.wrapper as yt

from crypta.graph.data_import.stream.lib.tasks.base import _mount_processed_table
from mapreduce.yt.python.yt_stuff import YtConfig, YtStuff  # noqa
import crypta.graph.v1.tests.libs.graph_runner  # noqa


pytest_plugins = "crypta.graph.v1.tests.libs.graph_runner"


@pytest.fixture(scope="module")
def stream_import_dyntable(request, ytlocal):
    tbl = "//home/crypta-tests/dyntables/processed"
    _mount_processed_table(ytlocal, tbl)

    stream_logs = []

    def fill_day(dt):
        access_log_task = {"AccessLogImportTask": 1566495245, "SoupTask": 1566495245}
        watch_log_task = {
            "WatchLogImportTask": 1566495245,
            "SoupTask": 1566495245,
            "AddIdStorageYandexuid": 1566495245,
            "AddIdStorageIcookie": 1566495245,
        }
        metrika_task = {"SoupTask": 1566495245, "AppMetrikaTask": 1566495245}
        redir_log_task = {"SoupTask": 1566495245, "RedirLogImportTask": 1566495245}
        bar_navig_task = {"SoupTask": 1566495245, "BarNavigImportTask": 1566495245}
        eal_task = {"SoupTask": 1566495245, "EalImportTask": 1566495245}
        rtb_task = {"SoupTask": 1566495245, "RTBLogTask": 1566495245}
        postback_task = {"SoupTask": 1566495245, "PostbackLogTask": 1566495245}

        stream_logs.extend(
            [
                {
                    "log_source": "access",
                    "path": "//statbox/access-log/{dt}".format(dt=dt),
                    "process_time": access_log_task,
                },
                {
                    "log_source": "wl",
                    "path": "//logs/bs-watch-log/1d/{dt}".format(dt=dt),
                    "process_time": watch_log_task,
                },
                {
                    "log_source": "mm",
                    "path": "//logs/appmetrica-yandex-events/stream/5min/{dt}T23:55:00".format(dt=dt),
                    "process_time": metrika_task,
                },
                {
                    "log_source": "mm",
                    "path": "//logs/appmetrica-external-events/stream/5min/{dt}T23:55:00".format(dt=dt),
                    "process_time": metrika_task,
                },
                {
                    "log_source": "mm",
                    "path": "//logs/browser-metrika-mobile-log/stream/5min/{dt}T23:55:00".format(dt=dt),
                    "process_time": metrika_task,
                },
                {
                    "log_source": "mm",
                    "path": "//logs/superapp-metrika-mobile-log/stream/5min/{dt}T23:55:00".format(dt=dt),
                    "process_time": metrika_task,
                },
                {
                    "log_source": "bs-rtb-log",
                    "path": "//logs/bs-rtb-log/stream/30min/{dt}T23:30:00".format(dt=dt),
                    "process_time": rtb_task,
                },
                {
                    "log_source": "postback-log",
                    "path": "//logs/bs-uniform-postback-log/stream/5min/{dt}T23:55:00".format(dt=dt),
                    "process_time": postback_task,
                },
                {
                    "log_source": "redir",
                    "path": "//logs/common-redir-log/stream/5min/{dt}T23:55:00".format(dt=dt),
                    "process_time": redir_log_task,
                },
                {
                    "log_source": "bar",
                    "path": "//logs/bar-navig-log/stream/5min/{dt}T23:55:00".format(dt=dt),
                    "process_time": bar_navig_task,
                },
                {
                    "log_source": "eal",
                    "path": "//logs/export-access-log/30min/{dt}T23:30:00".format(dt=dt),
                    "process_time": eal_task,
                },
            ]
        )

    for dt in ("2016-04-09", "2016-04-10", "2016-04-11"):
        fill_day(dt)

    ytlocal.insert_rows(tbl, stream_logs)


@pytest.fixture(scope="module")
def yt_config(request):
    proxy_port = getattr(request.module, "PROXY_PORT", 9013)

    master_config = getattr(request.module, "MASTER_CONFIG", None)
    get_scheduler_config_func = getattr(request.module, "get_scheduler_config", None)
    get_node_config_func = getattr(request.module, "get_node_config", None)
    proxy_config = getattr(request.module, "PROXY_CONFIG", None)

    prepare_cypress_dir_func = getattr(request.module, "prepare_cypress", None)

    if prepare_cypress_dir_func is not None:
        cypress_root = prepare_cypress_dir_func()
    else:
        cypress_root = None

    if get_node_config_func is not None:
        node_config = get_node_config_func()
    else:
        node_config = None

    if get_scheduler_config_func is not None:
        scheduler_config = get_scheduler_config_func()
    else:
        scheduler_config = None

    return YtConfig(
        proxy_port=proxy_port,
        local_cypress_dir=cypress_root,
        master_config=master_config,
        node_config=node_config,
        scheduler_config=scheduler_config,
        proxy_config=proxy_config,
        node_count=5,
    )


@pytest.fixture(scope="module")
def ytlocal(request, yt_config, yt_stuff):
    get_file_uploads_func = getattr(request.module, "get_file_uploads", None)

    yt.config.set_proxy("localhost:" + str(yt_config.proxy_port))

    if get_file_uploads_func is not None:
        file_uploads = get_file_uploads_func()
        for fu in file_uploads:
            fpath, ytpath = fu
            print("Uploading %s to %s" % (fpath, ytpath))
            yt.smart_upload_file(fpath, destination=ytpath, placement_strategy="replace")

    return yt_stuff.get_yt_client()


def get_env_from_script(path):
    blacklist = set(["PYTHONPATH"])
    output = subprocess.check_output(["bash", "-c", "source %s && env" % path])
    result = dict()
    for line in output.split("\n"):
        line = line.rstrip()
        parts = line.split("=")
        if parts[0] not in blacklist:
            result[parts[0]] = "=".join(parts[1:])
    return result


def gen_metrica_rsa_key(dst):
    key = RSA.generate(1024)
    with open(dst, "w") as f:
        f.write(key.exportKey("PEM"))


def copy_udf_so(build, binary):
    # udfs
    yql_udfs = build("crypta/graph/v1/tests/yql_udfs")
    if not os.path.exists(yql_udfs):
        os.makedirs(yql_udfs)

    udfs = [
        "../../ydb/library/yql/udfs/common/datetime/libdatetime_udf.so",
        "../../ydb/library/yql/udfs/common/datetime2/libdatetime2_udf.so",
        "../../ydb/library/yql/udfs/common/digest/libdigest_udf.so",
        "../../ydb/library/yql/udfs/common/histogram/libhistogram_udf.so",
        "../../ydb/library/yql/udfs/common/hyperloglog/libhyperloglog_udf.so",
        "../../ydb/library/yql/udfs/common/hyperscan/libhyperscan_udf.so",
        "../../ydb/library/yql/udfs/common/math/libmath_udf.so",
        "../../ydb/library/yql/udfs/common/re2/libre2_udf.so",
        "../../ydb/library/yql/udfs/common/set/libset_udf.so",
        "../../ydb/library/yql/udfs/common/stat/libstat_udf.so",
        "../../ydb/library/yql/udfs/common/string/libstring_udf.so",
        "../../ydb/library/yql/udfs/common/topfreq/libtopfreq_udf.so",
        "../../ydb/library/yql/udfs/common/yson2/libyson2_udf.so",
        "../../ydb/library/yql/udfs/logs/dsv/libdsv_udf.so",
        "common/python/python_arc_small/libpython_arc_udf.so",
        "crypta/identifiers/libcrypta_identifier_udf.so",
        "crypta/ipreq/libcrypta_ipreq_udf.so",
        "crypta/soup/libcrypta_soup_udf.so",
        "metrika/libmetrika_udf.so",
        "file",
        "geo",
        "ip",
        "protobuf",
        "unicode",
        "url",
        "user_agent",
    ]

    for udf in udfs:
        if udf.endswith(".so"):
            # exact path udf template
            udf_path = "yql/udfs/{full_name}".format(full_name=udf)
        else:
            # common udf name template
            udf_path = "yql/udfs/common/{name}/lib{name}_udf.so".format(name=udf)
        shutil.copy(binary(udf_path), yql_udfs)


def copy_files(build, ram, source, work):
    # geodata for yql geo udf
    print("Start to copy files into workdirs...", file=sys.stderr)

    def _copy(src_path_fun, base, files):
        for fname, output_path_fun in itertools.product(files, (work, ram)):
            dst_path = output_path_fun(fname)
            print("Copy {0!r} to {1!r}".format(fname, dst_path), file=sys.stderr)
            if dst_path is None:
                continue
            shutil.copy(src_path_fun(os.path.join(base, fname)), dst_path)

    _copy(build, "crypta/graph/v1/tests/sandbox-data", ("geodata6.bin", "UrlToGroups.yaml"))
    # uatraits data
    _copy(source, "metrika/uatraits/data", ("browser.xml", "profiles.xml"))


@pytest.fixture(scope="module")
def crypta_env(request):
    from yatest.common import (
        binary_path as binary,
        build_path as build,
        output_path as out,
        ram_drive_path as ram,
        source_path as source,
        work_path as work,
    )

    os.environ["TEST_FIXTURES_PATH"] = source("crypta/graph/v1/tests/testdata/fixtures")
    os.environ["SOURCE_ROOT"] = source("crypta")
    os.environ["VERBOSE"] = "1"

    os.environ["BASE_SECRETS_PATH"] = out("secrets") + "/"
    os.environ["SECDIST_USER"] = "crypta"
    path = "%s/users/%s" % (os.getenv("BASE_SECRETS_PATH"), os.getenv("SECDIST_USER"))
    if not os.path.exists(path):
        os.makedirs(path)

    os.environ["CRYPTA_GRAPH_CRYPTA_HOME"] = "//crypta/production"
    os.environ["CRYPTA_NOVOSIB_HOME"] = "//crypta/production"
    os.environ["CRYPTA_YT_HOME"] = "//crypta/production"
    os.environ["CRYPTA_PROFILES_EXPORT_DIR"] = "//crypta/production/profiles/export"
    os.environ["CRYPTA_ENV"] = "development"

    env_vars = get_env_from_script(source("crypta/graph/v1/packages/rtcrypta-config/set_crypta_env.sh.template"))
    for k, v in env_vars.iteritems():
        try:
            os.environ[k] = v
        except Exception as e:
            print("Can't set env var {0!r}={1!r} :: ({2!s})".format(k, v, e), file=sys.stderr)

    os.environ["INDEVICE_UNPERFECT_MODEL"] = source("crypta/graph/v1/tests/indevice.info")
    os.environ["RTCRYPTA_MAPREDUCE_EXEC"] = binary("yt/python/yt/wrapper/bin/mapreduce-yt_make/mapreduce-yt")
    os.environ["MX_OPS_BIN"] = binary("quality/relev_tools/mx_ops/mx_ops")
    os.environ["MONRUN_FOLDER"] = out("monrun") + "/"
    os.environ["MONRUN_DATE_FOLDER"] = out(datetime.now().strftime("monrun/%Y-%m-%d")) + "/"
    os.environ["ENV_TYPE"] = "development"
    os.environ["MOBILE_TMP_FOLDER"] = out("mobile_tmp") + "/"
    os.environ["GRAPH_STORE_DAYS"] = "3"
    os.environ["GRAPH_LOCAL_OUTPUT_FOLDER"] = out("graph_local_output") + "/"
    os.environ["METRICA_RSA_KEY_PATH"] = out("metrica_rsa_key")
    os.environ["YT_JOB_MAX_MEMORY_BYTES"] = "2147483648"
    os.environ["LOCAL_CLUSTERING_FOLDER"] = "/tmp"
    os.environ["GRAPH_STREAM_FOLDER"] = "//home/crypta-tests/stuff/state/graph/stream"
    os.environ["RTCRYPTA_MR_SERVER"] = "localhost:9013"
    os.environ["YT_PROXY"] = os.getenv("RTCRYPTA_MR_SERVER")

    print("Environ variables are set", file=sys.stderr)

    dirs = ["MONRUN_FOLDER", "MONRUN_DATE_FOLDER", "MOBILE_TMP_FOLDER", "GRAPH_LOCAL_OUTPUT_FOLDER"]
    for d in dirs:
        if not os.path.exists(os.getenv(d)):
            os.makedirs(os.getenv(d))

    gen_metrica_rsa_key(os.getenv("METRICA_RSA_KEY_PATH"))
    copy_files(build, ram, source, work)
    copy_udf_so(build, binary)
