import datetime
import os

import pytest
import yatest.common
import yt.wrapper as yt

from crypta.dmp.yandex.bin.common.python import (
    config_fields,
    statistics_schema
)
from crypta.lib.python import yaml_config
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tables,
    tests
)


STATISTICS_DIR = "//dmp/statistics"
PROCESSED_STATISTICS_DIR = "//dmp/processed_statistics"
STATISTICS_TTL = datetime.timedelta(days=1)


@pytest.fixture
def config(yt_stuff, local_smtp_server):
    smtp_host, smtp_port = local_smtp_server.local_address
    return {
        config_fields.YT_PROXY: yt_stuff.get_server(),
        config_fields.YT_POOL: "pool",
        config_fields.SMTP_HOST: smtp_host,
        config_fields.SMTP_PORT: smtp_port,
        config_fields.SMTP_EMAIL_FROM: "data-partners@yandex-team.ru",
        config_fields.STATISTICS_EMAILS_CC: ["data-partners@yandex-team.ru"],
        config_fields.STATISTICS_EMAILS_BCC: ["crypta-dmp@yandex-team.ru"],
        config_fields.SEND_STATISTICS_EMAILS: True,
        config_fields.STATISTICS_EMAILS: ["xxx@local.local", "zzz@local.local"],
        config_fields.STATISTICS_DIR: STATISTICS_DIR,
        config_fields.PROCESSED_STATISTICS_DIR: PROCESSED_STATISTICS_DIR,
        config_fields.STATISTICS_TTL_DAYS: STATISTICS_TTL.days,
        config_fields.DMP_LOGIN: "dmp-xxx"
    }


def get_input_table(name):
    return tables.get_yson_table_with_schema("{}.yson".format(name), yt.ypath_join(STATISTICS_DIR, name), statistics_schema.get())


def test_statistics_mail(yt_stuff, local_smtp_server, config):
    output_files = [
        "mail_0.txt",
        "mail_1.txt"
    ]

    schema_test = tests.SchemaEquals(statistics_schema.get())
    expiration_time_test = tests.ExpirationTime(STATISTICS_TTL)
    diff = tests.Diff()

    with local_smtp_server:
        yt_output_files = tests.yt_test(
            yt_client=yt_stuff.get_yt_client(),
            binary=yatest.common.binary_path("crypta/dmp/yandex/bin/send_statistics_mail/bin/crypta-dmp-yandex-send-statistics-mail"),
            args=["--config", yaml_config.dump(config)],
            data_path=yatest.common.test_source_path("data"),
            input_tables=[
                (get_input_table("1400000000_1500000000"), None),
                (get_input_table("1400001000_1500001000"), None),
                (get_input_table("1400002000_1500002000"), None)
            ],
            output_tables=[(
                cypress.CypressNode(PROCESSED_STATISTICS_DIR),
                [
                    tests.TestNodesInMapNode([expiration_time_test, schema_test, diff], tag="processed_statistics"),
                ]
            )],
            env={
                "YT_TOKEN": "yttoken"
            }
        )

    assert not yt_stuff.yt_wrapper.list(STATISTICS_DIR)
    assert not os.path.exists("mail_2.txt")

    return [yatest.common.canonical_file(output_file, local=True) for output_file in output_files] + yt_output_files
