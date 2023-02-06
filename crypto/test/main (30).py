import datetime

import pytest
import yatest.common
import yt.wrapper as yt

from crypta.dmp.yandex.bin.common.python import (
    config_fields,
    errors_schema
)
from crypta.lib.python import yaml_config
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tables,
    tests
)


QUARANTINE_DIR = "//dmp/quarantine"
PROCESSED_QUARANTINE_DIR = "//dmp/processed_quarantine"
QUARANTINE_TTL = datetime.timedelta(days=1)


@pytest.fixture
def config(yt_stuff, local_smtp_server):
    smtp_host, smtp_port = local_smtp_server.local_address
    return {
        config_fields.YT_PROXY: yt_stuff.get_server(),
        config_fields.YT_POOL: "pool",
        config_fields.SMTP_HOST: smtp_host,
        config_fields.SMTP_PORT: smtp_port,
        config_fields.SMTP_EMAIL_FROM: "data-partners@yandex-team.ru",
        config_fields.QUARANTINE_EMAILS_CC: ["data-partners@yandex-team.ru"],
        config_fields.QUARANTINE_EMAILS_BCC: ["crypta-dmp@yandex-team.ru"],
        config_fields.SEND_QUARANTINE_EMAILS: True,
        config_fields.ERRORS_EMAILS: ["xxx@local.local", "zzz@local.local"],
        config_fields.QUARANTINE_DIR: QUARANTINE_DIR,
        config_fields.PROCESSED_QUARANTINE_DIR: PROCESSED_QUARANTINE_DIR,
        config_fields.ERRORS_AMOUNT: 4,
        config_fields.QUARANTINE_TTL_DAYS: QUARANTINE_TTL.days,
        config_fields.DMP_LOGIN: "dmp-xxx"
    }


def get_input_table(archive):
    return tables.get_yson_table_with_schema("{}.yson".format(archive), yt.ypath_join(QUARANTINE_DIR, archive, "errors"), errors_schema.get())


def test_mail_about_quarantine(yt_stuff, local_smtp_server, config):
    output_files = [
        "mail_0.txt",
        "mail_1.txt",
        "mail_2.txt"
    ]

    schema_test = tests.SchemaEquals(errors_schema.get())
    expiration_time_test = tests.ExpirationTime(QUARANTINE_TTL)
    diff = tests.Diff()

    with local_smtp_server:
        yt_output_files = tests.yt_test(
            yt_client=yt_stuff.get_yt_client(),
            binary=yatest.common.binary_path("crypta/dmp/yandex/bin/send_quarantine_mail/bin/crypta-dmp-yandex-send-quarantine-mail"),
            args=["--config", yaml_config.dump(config)],
            data_path=yatest.common.test_source_path("data"),
            input_tables=[
                (get_input_table("segments-xxx.tar"), None),
                (get_input_table("segments-1400000000.tar.gz"), None),
                (get_input_table("segments-1500000000.tar.gz"), None)
            ],
            output_tables=[(
                cypress.CypressNode(PROCESSED_QUARANTINE_DIR),
                [
                    tests.TestNodesInMapNode([expiration_time_test], tag=None),
                    tests.TestNodesInMapNodeChildren([schema_test, diff], tag="processed_quarantine")
                ]
            )],
            env={
                "YT_TOKEN": "yttoken"
            }
        )

    assert not yt_stuff.yt_wrapper.list(QUARANTINE_DIR)

    return [yatest.common.canonical_file(output_file, local=True) for output_file in output_files] + yt_output_files
