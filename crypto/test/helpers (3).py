import os

import yatest.common
import yt.wrapper as yt

from crypta.dmp.common.data.python import (
    bindings,
    meta
)
from crypta.dmp.yandex.bin.common.python import (
    config_fields,
    errors_schema
)
from crypta.lib.python import (
    time_utils,
    yaml_config,
)
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tables,
    tests
)

import cases


DATA_PATH = yatest.common.test_source_path("data")

INPUT_DIR = "//dmp/input"
OUTPUT_DIR = "//dmp/output"
QUARANTINE_DIR = "//dmp/quarantine"


def execute_binary(yt_stuff, test_name, drop_input, must_be_execution_error, owner_login=None):
    diff = tests.Diff()
    meta_tests = [tests.SchemaEquals(meta.get_schema()), diff]
    bindings_tests = [tests.SchemaEquals(bindings.get_ext_id_schema()), diff]
    output_test = tests.TestNodesInMapNodeChildren(
        tests_getter=lambda x: meta_tests if x.cypress_basename == "meta" else bindings_tests,
        tag="output"
    )

    config = get_config(yt_stuff, drop_input, owner_login)
    output_files = tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/yandex/bin/parse_segments/bin/crypta-dmp-yandex-parse-segments"),
        args=["--config", yaml_config.dump(config)],
        data_path=DATA_PATH,
        input_tables=list(get_inputs(yt_stuff, test_name)),
        output_tables=[
            (cypress.CypressNode(INPUT_DIR), tests.TestNodesInMapNodeChildren([diff], tag="input")),
            (cypress.CypressNode(OUTPUT_DIR), output_test),
            (cypress.CypressNode(QUARANTINE_DIR), tests.TestNodesInMapNodeChildren([tests.SchemaEquals(errors_schema.get()), diff], tag="quarantine"))
        ],
        env={
            time_utils.CRYPTA_FROZEN_TIME_ENV: "1500000000"
        },
        must_be_execution_error=must_be_execution_error
    )
    result = {os.path.basename(output_file["file"]["uri"]): output_file for output_file in output_files}

    # TODO: Списки папок тут, чтобы проверить, что не лежат пустые папки.
    # Если добавить в TestNodesInMapNodeChildren ключ on_empty_children, то это можно будет убрать
    result["input_dir"] = yt_stuff.yt_wrapper.list(INPUT_DIR, absolute=False)

    if yt_stuff.yt_wrapper.exists(OUTPUT_DIR):
        result["output_dir"] = yt_stuff.yt_wrapper.list(OUTPUT_DIR, absolute=False)

    if yt_stuff.yt_wrapper.exists(QUARANTINE_DIR):
        result["quarantine_dir"] = yt_stuff.yt_wrapper.list(QUARANTINE_DIR, absolute=False)

    return result


def get_config(yt_stuff, drop_input, owner_login):
    config = {
        config_fields.YT_PROXY: yt_stuff.get_server(),
        config_fields.YT_POOL: "pool",
        config_fields.RAW_SEGMENTS_DIR: INPUT_DIR,
        config_fields.PARSED_SEGMENTS_DIR: OUTPUT_DIR,
        config_fields.QUARANTINE_DIR: QUARANTINE_DIR,
        config_fields.SHOULD_DROP_INPUT: drop_input,
    }

    if owner_login is not None:
        config[config_fields.OWNER_LOGIN] = owner_login

    return config


def get_inputs(yt_stuff, test_name):
    for dirname, content in cases.CASES[test_name].iteritems():
        yt_dirpath = yt.ypath_join(INPUT_DIR, dirname)
        yt_stuff.yt_wrapper.mkdir(yt_dirpath, recursive=True)
        for tablename, local_filename in content.iteritems():
            yt_path = yt.ypath_join(yt_dirpath, tablename)
            yield tables.YsonTable(local_filename, yt_path, yson_format="pretty"), None
