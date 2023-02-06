import operator
import yatest.common

from crypta.dmp.common.data.python import (
    bindings,
    meta
)
from crypta.dmp.yandex.bin.common.python import config_fields
from crypta.lib.python import (
    time_utils,
    yaml_config,
)
from crypta.lib.python.graphite.sender import parse_api_log
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests
)


class StdoutTest(tests.YtTest):
    def teardown(self, path, yt_stuff):
        return sorted(parse_api_log.parse_file(path, erase_timestamps=True), key=operator.itemgetter("group", "name", "value"))


def test_send_metrics(local_yt, local_yt_and_yql_env, mock_audience_server, mock_solomon_server, config):
    local_yt_and_yql_env.update({
        "CRYPTA_API_TOKEN": "CRYPTA_API_TOKEN",
        "CRYPTA_DMP_AUDIENCE_OAUTH_TOKEN": "CRYPTA_DMP_AUDIENCE_OAUTH_TOKEN",
        time_utils.CRYPTA_FROZEN_TIME_ENV: "1500086400"
    })

    yt_test_results = tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/yandex/bin/send_metrics/bin/crypta-dmp-yandex-send-metrics"),
        args=["--config", yaml_config.dump(config)],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (
                tables.YsonTable("ext_id_bindings.yson", config[config_fields.EXT_ID_BINDINGS_TABLE], on_write=tables.OnWrite(attributes={"schema": bindings.get_ext_id_schema()})),
                tests.TableIsNotChanged()
            ),
            (
                tables.YsonTable("yandexuid_bindings.yson", config[config_fields.YANDEXUID_BINDINGS_TABLE], on_write=tables.OnWrite(attributes={"schema": bindings.get_yandexuid_schema()})),
                tests.TableIsNotChanged()
            ),
            (
                tables.YsonTable("meta.yson", config[config_fields.OUT_META_TABLE], on_write=tables.OnWrite(attributes={"schema": meta.get_schema_with_sizes()})),
                tests.TableIsNotChanged()
            )
        ],
        output_tables=[],
        env=local_yt_and_yql_env,
        stdout_fname="stdout.txt",
        stdout_test=StdoutTest()
    )
    return yt_test_results, mock_solomon_server.dump_push_requests()
