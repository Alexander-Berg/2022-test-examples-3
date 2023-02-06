import yatest.common

from crypta.dmp.common.data.python import meta
from crypta.dmp.crypta.services.upload_meta.proto import config_pb2
from crypta.lib.python import (
    templater,
    yaml_config,
)
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests
)


def test_upload_meta(yt_stuff):
    config_file_path = yatest.common.test_output_path("config.yaml")
    templater.render_file(
        yatest.common.source_path("crypta/dmp/crypta/services/upload_meta/config/config.yaml"),
        config_file_path,
        {
            "exports": {
                "export-1": 0,
                "export-2": 0,
                "export-3": 1,
            },
            "custom_descriptions": {"export-2": {"en": "Replace description"}},
            "yt_proxy": yt_stuff.get_server(),
        }
    )
    config = yaml_config.parse_config(config_pb2.TConfig, config_file_path)

    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/crypta/services/upload_meta/bin/crypta-dmp-crypta-upload-meta"),
        args=["--config", config_file_path],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable("lab_export.yson", config.LabExport), tests.TableIsNotChanged()),
            (tables.YsonTable("lab_segments.yson", config.LabSegments), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema("meta.yson", config.DmpMetaPath, meta.get_schema_internal()), None),
        ],
        output_tables=[
            (
                tables.YsonTable("meta.yson", config.DmpMetaPath, yson_format="pretty"),
                (tests.SchemaEquals(meta.get_schema_internal()), tests.Diff())
            ),
        ],
    )
