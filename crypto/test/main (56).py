import os

from google.protobuf import (
    json_format,
    message,
)
from mapreduce.yt.interface.protos import extension_pb2
import pytest
import yatest.common

from crypta.lib.proto.user_data.user_data_pb2 import TUserData
from crypta.lib.python import (
    templater,
    time_utils,
    yaml_config,
)
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    cypress,
    files,
    tables,
    tests,
)
from crypta.lookalike.lib.python import test_utils
from crypta.lookalike.proto.yt_node_names_pb2 import TYtNodeNames
from crypta.lookalike.services.user_dssm_applier.proto.config_pb2 import TConfig


VERSION = "1584085850"
ARCADIA_REVISION_ATTR = "arcadia_revision"
ARCADIA_REVISION = "100500"
LAST_UPDATE_DATE_ATTR = "_last_update_date"
LAST_UPDATE_DATE = "2020-03-18"
OLD_LAST_UPDATE_DATE = "2020-03-17"

YT_NODE_NAMES = TYtNodeNames()


def get_name(descriptor):
    return descriptor.GetOptions().Extensions[extension_pb2.column_name] if descriptor.GetOptions().HasExtension(extension_pb2.column_name) else descriptor.name


def get_value(value):
    return value.SerializeToString() if isinstance(value, message.Message) else value


def row_transformer(row):
    proto = json_format.ParseDict(row, TUserData())
    return {
        get_name(descriptor): get_value(value)
        for descriptor, value in proto.ListFields()
    }


def get_user_data_schema():
    return schema_utils.get_schema_from_proto(TUserData)


@pytest.fixture(scope="function")
def config_file(yt_stuff):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/lookalike/services/user_dssm_applier/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": yt_stuff.get_server(),
            "scope": "direct",
        },
    )

    return config_file_path


def get_user_data_on_write():
    return tables.OnWrite(attributes={"schema": get_user_data_schema(), LAST_UPDATE_DATE_ATTR: LAST_UPDATE_DATE}, row_transformer=row_transformer)


def get_dssm_model_file_on_write():
    return files.OnWrite(attributes={ARCADIA_REVISION_ATTR: ARCADIA_REVISION})


def test_basic(yt_stuff, config_file):
    config = yaml_config.parse_config(TConfig, config_file)
    versions_dir = cypress.CypressNode(config.VersionsDir)

    def get_versioned_path(node):
        return os.path.join(versions_dir.cypress_path, VERSION, node)

    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/lookalike/services/user_dssm_applier/bin/crypta-lookalike-user-dssm-applier"),
        args=[
            "--config", config_file
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable("user_data.yson", config.UserDataTable, on_write=get_user_data_on_write()), tests.TableIsNotChanged()),
            (files.YtFile(yatest.common.work_path("dssm_lal_model.applier"), get_versioned_path(YT_NODE_NAMES.DssmModelFile), on_write=get_dssm_model_file_on_write()), tests.Exists()),
            (files.YtFile(yatest.common.work_path("segments_dict"), get_versioned_path(YT_NODE_NAMES.SegmentsDictFile), on_write=files.OnWrite()), tests.Exists()),
        ],
        output_tables=[
            (
                versions_dir,
                [tests.DiffUserAttrs()],
            ),
            (
                tables.YsonTable("user_embeddings.yson", get_versioned_path(YT_NODE_NAMES.UserEmbeddingsTable), yson_format="pretty", on_read=test_utils.embeddings_on_read()),
                [tests.Diff(), tests.AttrEquals(LAST_UPDATE_DATE_ATTR, LAST_UPDATE_DATE)]
            ),
            (
                tables.YsonTable("errors.yson", os.path.join(config.ErrorsDir, VERSION), yson_format="pretty"),
                [tests.IsAbsent()]
            ),
        ],
        env={"YT_TOKEN": "FAKE", time_utils.CRYPTA_FROZEN_TIME_ENV: VERSION},
    )


def test_do_not_update(yt_stuff, config_file):
    config = yaml_config.parse_config(TConfig, config_file)
    versions_dir = cypress.CypressNode(config.VersionsDir)

    def get_versioned_path(node):
        return os.path.join(versions_dir.cypress_path, VERSION, node)

    embeddings_on_write = tables.OnWrite(attributes={LAST_UPDATE_DATE_ATTR: LAST_UPDATE_DATE})

    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/lookalike/services/user_dssm_applier/bin/crypta-lookalike-user-dssm-applier"),
        args=[
            "--config", config_file
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable("user_data.yson", config.UserDataTable, on_write=get_user_data_on_write()), tests.TableIsNotChanged()),
            (tables.YsonTable("user_embeddings.yson", get_versioned_path(YT_NODE_NAMES.UserEmbeddingsTable), on_write=embeddings_on_write), tests.TableIsNotChanged()),
            (files.YtFile(yatest.common.work_path("dssm_lal_model.applier"), get_versioned_path(YT_NODE_NAMES.DssmModelFile), on_write=get_dssm_model_file_on_write()), tests.Exists()),
            (files.YtFile(yatest.common.work_path("segments_dict"), get_versioned_path(YT_NODE_NAMES.SegmentsDictFile), on_write=files.OnWrite()), tests.Exists()),
        ],
        output_tables=[],
        env={"YT_TOKEN": "FAKE", time_utils.CRYPTA_FROZEN_TIME_ENV: VERSION},
    )


@pytest.mark.parametrize("embeddings_attributes", [
    {},
    {LAST_UPDATE_DATE_ATTR: OLD_LAST_UPDATE_DATE},
])
def test_update_old_embeddings(yt_stuff, config_file, embeddings_attributes):
    config = yaml_config.parse_config(TConfig, config_file)
    versions_dir = cypress.CypressNode(config.VersionsDir)

    def get_versioned_path(node):
        return os.path.join(versions_dir.cypress_path, VERSION, node)

    embeddings_on_write = tables.OnWrite(attributes=embeddings_attributes)

    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/lookalike/services/user_dssm_applier/bin/crypta-lookalike-user-dssm-applier"),
        args=[
            "--config", config_file
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (
                tables.YsonTable("user_data.yson", config.UserDataTable, on_write=get_user_data_on_write()),
                tests.TableIsNotChanged()
            ),
            (
                tables.YsonTable(
                    "user_embeddings.yson",
                    get_versioned_path(YT_NODE_NAMES.UserEmbeddingsTable),
                    on_write=embeddings_on_write,
                ),
                None,
            ),
            (
                files.YtFile(yatest.common.work_path("dssm_lal_model.applier"), get_versioned_path(YT_NODE_NAMES.DssmModelFile), on_write=get_dssm_model_file_on_write()),
                tests.Exists()
            ),
            (
                files.YtFile(yatest.common.work_path("segments_dict"), get_versioned_path(YT_NODE_NAMES.SegmentsDictFile), on_write=files.OnWrite()),
                tests.Exists()
            ),
        ],
        output_tables=[
            (
                versions_dir,
                [tests.DiffUserAttrs()],
            ),
            (
                tables.YsonTable("user_embeddings.yson", get_versioned_path(YT_NODE_NAMES.UserEmbeddingsTable), yson_format="pretty", on_read=test_utils.embeddings_on_read()),
                [tests.Diff(), tests.AttrEquals(LAST_UPDATE_DATE_ATTR, LAST_UPDATE_DATE)]
            ),
            (
                tables.YsonTable("errors.yson", os.path.join(config.ErrorsDir, VERSION), yson_format="pretty"),
                [tests.IsAbsent()]
            ),
        ],
        env={"YT_TOKEN": "FAKE", time_utils.CRYPTA_FROZEN_TIME_ENV: VERSION},
    )
