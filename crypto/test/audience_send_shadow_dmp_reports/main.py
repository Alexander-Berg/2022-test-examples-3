import os

import yatest.common
from yt.wrapper import ypath

from crypta.buchhalter.services.main.lib.audience_send_shadow_dmp_reports.config_pb2 import TConfig
from crypta.buchhalter.services.main.lib.common import report_generator
from crypta.buchhalter.services.main.lib.common.proto.shadow_dmp_index_pb2 import TShadowDmpIndex
from crypta.lib.python import (
    time_utils,
    yaml_config,
)
from crypta.lib.python.smtp.test_helpers import mail_canonizers
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    cypress,
    files,
    tables,
    tests
)


EMPTY_REPORTS = [
    ("audience_login_1", "2020-01-01.xlsx"),
    ("audience_login_2", "2019-11.xlsx"),
]
DAILY_REPORTS = {
    "audience_login_1": [
        "2019-12-31.xlsx",
        "2020-01-01.xlsx",
        "2020-01-02.xlsx",
    ],
    "audience_login_2": [
        "2019-12-31.xlsx",
        "2020-01-02.xlsx",
    ],
}
MONTHLY_REPORTS = {
    "audience_login_1": [
        "2019-10.xlsx",
        "2019-12.xlsx",
    ],
    "audience_login_2": [
        "2019-10.xlsx",
        "2019-11.xlsx",
    ],
}


def get_input_report(directory, sdmp_dirname, filename):
    local_path = "{}_{}".format(sdmp_dirname, filename)
    cypress_path = ypath.ypath_join(directory, sdmp_dirname, filename)
    on_write = files.OnWrite(attributes={
        report_generator.EMPTY_REPORT_ATTRIBUTE: (sdmp_dirname, filename) in EMPTY_REPORTS,
    })
    return files.YtFile(local_path, cypress_path, on_write=on_write)


def test_basic(yt_stuff, local_smtp_server, config_file, frozen_time):
    config = yaml_config.parse_config(TConfig, config_file)
    shadow_dmp_index_schema = schema_utils.get_schema_from_proto(TShadowDmpIndex)
    with local_smtp_server:
        yt_output_files = tests.yt_test(
            yt_client=yt_stuff.get_yt_client(),
            binary=yatest.common.binary_path("crypta/buchhalter/services/main/bin/crypta-buchhalter"),
            args=[
                "--config", config_file,
                "audience_send_shadow_dmp_reports",
            ],
            data_path=yatest.common.test_source_path("data"),
            input_tables=[
                (tables.get_yson_table_with_schema("index.yson", config.ShadowDmpIndexTable, shadow_dmp_index_schema), [tests.TableIsNotChanged()]),
            ] + [
                (get_input_report(config.DailyReportDir, sdmp_dirname, filename), tests.Exists())
                for sdmp_dirname, filenames in DAILY_REPORTS.iteritems()
                for filename in filenames
            ] + [
                (get_input_report(config.MonthlyReportDir, sdmp_dirname, filename), tests.Exists())
                for sdmp_dirname, filenames in MONTHLY_REPORTS.iteritems()
                for filename in filenames
            ] + [
                (tables.YsonTable("track_table_{}.yson".format(sdmp_dirname), ypath.ypath_join(config.ProcessedDailyReportDir, sdmp_dirname)), [])
                for sdmp_dirname in DAILY_REPORTS
            ],
            output_tables=[
                (cypress.CypressNode(config.ProcessedDailyReportDir), tests.TestNodesInMapNode([tests.Diff()], tag="output_daily_track_table")),
                (cypress.CypressNode(config.ProcessedMonthlyReportDir), tests.TestNodesInMapNode([tests.Diff()], tag="output_monthly_track_table")),
            ],
            env={
                "YT_TOKEN": "FAKE_YT_TOKEN",
                time_utils.CRYPTA_FROZEN_TIME_ENV: str(frozen_time),
            },
        )

    output_files = ["mail_0.txt", "mail_1.txt", "mail_2.txt", "mail_3.txt"]
    assert not os.path.exists("mail_4.txt")

    result = mail_canonizers.canonize_mails(output_files, key=lambda mail: "{}_{}".format(mail["To"], mail["Subject"]))

    for yt_output_file in yt_output_files:
        result[os.path.basename(yt_output_file["file"]["uri"])] = yt_output_file

    return result
