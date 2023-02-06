import os

import pytest
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
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests
)


@pytest.fixture(scope="function")
def config(local_yt, local_smtp_server):
    smtp_host, smtp_port = local_smtp_server.local_address

    return {
        config_fields.YT_PROXY: local_yt.get_server(),
        config_fields.YT_POOL: "pool",
        config_fields.YT_TMP_DIR: "//tmp",
        config_fields.DMP_LOGIN: "dmp-xxx",
        config_fields.SMTP_HOST: smtp_host,
        config_fields.SMTP_PORT: smtp_port,
        config_fields.SMTP_EMAIL_FROM: "data-partners@yandex-team.ru",
        config_fields.METRICS_EMAILS_BCC: ["crypta-dmp@yandex-team.ru"],
        config_fields.METRICS_EMAILS_CC: [],
        config_fields.SEND_METRICS_EMAILS: True,
        config_fields.COVERAGE_METRICS_DAYS_FOR_EMAILS: [0, 1],
        config_fields.STATISTICS_EMAILS: ["statistics@dmp-xxx.local"],
        config_fields.ERRORS_EMAILS: ["errors-xxx@local.local", "errors-zzz@local.local"],
        config_fields.QUARANTINE_EMAILS_CC: ["errors-data-partners@yandex-team.ru"],
        config_fields.QUARANTINE_EMAILS_BCC: ["errors-crypta-dmp@yandex-team.ru"],
    }


def test_send_metrics_mail(local_yt, local_yt_and_yql_env, local_smtp_server, config):
    local_yt_and_yql_env.update({
        time_utils.CRYPTA_FROZEN_TIME_ENV: "1500086400"
    })

    out_meta_table = "//dmp/out/meta_1"
    ext_id_bindings_table = "//dmp/ext_id_bindings_1"
    yandexuid_bindings_table = "//dmp/yandexuid_bindings_1"

    result_config = {
        config_fields.OUT_META_TABLE: out_meta_table,
        config_fields.EXT_ID_BINDINGS_TABLE: ext_id_bindings_table,
        config_fields.YANDEXUID_BINDINGS_TABLE: yandexuid_bindings_table,
    }
    result_config.update(config)

    with local_smtp_server:
        tests.yt_test(
            yt_client=local_yt.get_yt_client(),
            binary=yatest.common.binary_path("crypta/dmp/yandex/bin/send_metrics_mail/bin/crypta-dmp-yandex-send-metrics-mail"),
            args=["--config", yaml_config.dump(result_config)],
            data_path=yatest.common.test_source_path("data"),
            input_tables=[
                (tables.get_yson_table_with_schema("meta.yson", out_meta_table, meta.get_schema_with_sizes()), tests.TableIsNotChanged()),
                (tables.get_yson_table_with_schema("ext_id_bindings.yson", ext_id_bindings_table, bindings.get_ext_id_schema()), tests.TableIsNotChanged()),
                (tables.get_yson_table_with_schema("yandexuid_bindings.yson", yandexuid_bindings_table, bindings.get_yandexuid_schema()), tests.TableIsNotChanged()),
            ],
            env=local_yt_and_yql_env
        )

    assert not os.path.exists("mail_1.txt")
    return yatest.common.canonical_file("mail_0.txt", local=True)


def test_send_metrics_mail_zero_coverage(local_yt, local_yt_and_yql_env, local_smtp_server, config):
    local_yt_and_yql_env.update({
        time_utils.CRYPTA_FROZEN_TIME_ENV: "1500086400"
    })

    out_meta_table = "//dmp/out/meta_2"
    ext_id_bindings_table = "//dmp/ext_id_bindings_2"
    yandexuid_bindings_table = "//dmp/yandexuid_bindings_2"

    result_config = {
        config_fields.OUT_META_TABLE: out_meta_table,
        config_fields.EXT_ID_BINDINGS_TABLE: ext_id_bindings_table,
        config_fields.YANDEXUID_BINDINGS_TABLE: yandexuid_bindings_table,
    }
    result_config.update(config)

    with local_smtp_server:
        tests.yt_test(
            yt_client=local_yt.get_yt_client(),
            binary=yatest.common.binary_path("crypta/dmp/yandex/bin/send_metrics_mail/bin/crypta-dmp-yandex-send-metrics-mail"),
            args=["--config", yaml_config.dump(result_config)],
            data_path=yatest.common.test_source_path("data"),
            input_tables=[
                (tables.get_yson_table_with_schema("meta.yson", out_meta_table, meta.get_schema_with_sizes()), tests.TableIsNotChanged()),
                (tables.get_yson_table_with_schema("ext_id_bindings_empty.yson", ext_id_bindings_table, bindings.get_ext_id_schema()), tests.TableIsNotChanged()),
                (tables.get_yson_table_with_schema("yandexuid_bindings.yson", yandexuid_bindings_table, bindings.get_yandexuid_schema()), tests.TableIsNotChanged()),
            ],
            env=local_yt_and_yql_env
        )

    return {
        "stats_mail": yatest.common.canonical_file("mail_0.txt", local=True),
        "zero_coverage_mail": yatest.common.canonical_file("mail_1.txt", local=True),
    }
