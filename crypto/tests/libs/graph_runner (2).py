from __future__ import print_function
import json
import logging
import os
import sys

import allure
import luigi
import pytest
import yt.wrapper as yt

from datetime import datetime
from logging.handlers import SysLogHandler

from crypta.graph.v1.tests.libs.yt_trace_reader import TraceReader, LuigiReader
from crypta.graph.v1.tests.testdata.testdata_writer import (
    create_log,
    create_stream_data,
    create_log_data,
    create_dump_data,
)

# don't change this, it'a not a paskhalka
KEY_FOR_CHECK_LUIGI_SUMMARY = "This progress looks :)"


@pytest.fixture(scope="module")
def graph():
    return Graph()


@pytest.fixture(scope="module")
def graph_soup():
    return Graph(root_task="SoupIsReadyFast")


class Graph(object):
    def __init__(self, root_task="GraphV2PostprocTask"):
        self.root_task = root_task
        self.run_status = None
        self.yt = self._configure()
        self._start_loging()
        self.report = self._run()

    def _configure(self):
        from crypta.graph.v1.python.rtcconf import config
        from yatest.common import build_path as build
        from yatest.common import binary_path as binary

        yt.config.set_proxy(config.MR_SERVER)  # assume it's set to localhost
        yt.config["tabular_data_format"] = yt.YsonFormat(control_attributes_mode="row_fields")

        config.YT_TARGET_EMPTY_TABLE_OK = True
        config.YT_TRANSACTION_ACTIVATE_TESTS_WORKAROUND = True
        config.LOCAL_GEODATA4_BIN = build("crypta/graph/v1/tests/sandbox-data/geodata4.bin")
        config.BB_UPLOAD_DRY_RUN = True
        config.IS_UPLOAD_DRY_RUN = True

        config.YQL_MRJOB = binary("yql/tools/mrjob/mrjob")
        config.YQL_UDF_RESOLVER = binary("yql/tools/udf_resolver/udf_resolver")
        config.YQL_UDF_DIR = build("crypta/graph/v1/tests/yql_udfs")

        config.STREAMING_MAX_WAIT_SECONDS = 60
        config.STREAMING_SLEEP_PERIOD = 1

        return yt

    def _start_loging(self):
        logging.basicConfig(level=logging.INFO)
        logger = logging.getLogger()
        handler = logging.StreamHandler(sys.stdout)
        handler.setFormatter(
            logging.Formatter("%(name)s %(levelname)s:%(filename)s:%(process)d:%(thread)d %(asctime)s %(message)s")
        )
        handler.setLevel(logging.INFO)
        logger.addHandler(handler)
        handler_luigi = SysLogHandler(address="/dev/log", facility="local3")
        handler_luigi.setFormatter(
            logging.Formatter(
                "luigi-log: %(levelname)s:%(filename)s:%(process)d:%(thread)d %(asctime)s error_message: %(message)s"
            )
        )
        handler_luigi.setLevel(logging.WARNING)
        logger.addHandler(handler_luigi)
        logger.setLevel(logging.INFO)
        logging.getLogger("yt.packages.requests.packages.urllib3.connectionpool").setLevel(logging.WARN)
        logging.getLogger("luigi-interface").setLevel(logging.INFO)
        logging.getLogger("sh.command").setLevel(logging.INFO)
        logging.getLogger("sh.stream_bufferer").setLevel(logging.INFO)

    def _run(self):
        from crypta.graph.v1.python.lib import yt_trace
        import crypta.graph.v1.python.rtcconf.config as config

        yt_trace.setup_trace()

        dt = "2016-04-11"
        create_log(self.yt, dt)
        create_stream_data(self.yt)
        create_log_data(self.yt)
        create_dump_data(self.yt)

        self.yt.run_sort("//crypta/production/state/graph/dicts/yuid_with_all", sort_by="yuid")
        self.yt.run_sort("//crypta/production/state/graph/dicts/income_data_with_dev_info", sort_by="key")
        self.yt.run_sort("//crypta/production/state/graph/dicts/puid_yuid_yt", sort_by="puid")
        self.yt.run_sort("//crypta/production/state/graph/dicts/kinopoisk", sort_by="id_value")
        self.yt.run_sort("//crypta/production/state/graph/dicts/profiles", sort_by="id_value")
        self.yt.run_sort("//crypta/production/state/graph/dicts/passport/puid_login", sort_by="id_value")
        self.yt.run_sort("//crypta/production/state/extras/reference-bases/audi-mvideo-emails", sort_by="id_value")
        self.yt.run_sort("//crypta/production/state/extras/reference-bases/sberbank_phones_hash", sort_by="id_value")

        self.yt.create(
            "table",
            "//crypta/production/ids_storage/email/email_organization_score",
            attributes=dict(
                schema=[
                    dict(name="id", type="string"),
                    dict(name="id_type", type="string"),
                    dict(name="is_org_score", type="string"),
                ],
                optimize_for="scan",
            ),
        )
        self.yt.run_sort(
            "//crypta/production/ids_storage/email/email_org_classification",
            "//crypta/production/ids_storage/email/email_organization_score",
            sort_by="id",
        )
        self.yt.remove("//crypta/production/ids_storage/email/email_org_classification")

        start_time = datetime.now()
        today = str(datetime.now().date())

        yt.mkdir(config.ACCESS_LOGS_FOLDER, recursive=True)
        yt.link("//statbox/access-log", os.path.join(config.ACCESS_LOGS_FOLDER, "some-access-log"))

        rtcrypta_pystarter_log_folder = os.environ.get("RTCRYPTA_PYSTARTER_LOG_FOLDER", "/var/log/crypta-pystarter")
        trace_reader = TraceReader(start_datetime=start_time, log_folder=rtcrypta_pystarter_log_folder)
        with yt.Transaction(), yt.TempTable() as tmp_dummy_table:  # because we do not have this table on YT local
            _ = tmp_dummy_table  # and this causes errors on tesing graph. So it is the fix
            pass

        print("Running graph ... ")
        from crypta.graph.v1.python.graph_v2 import MainV2Task  # noqa
        from crypta.graph.v1.python.v2.v2_task_status import SoupIsReadyAll  # noqa
        from crypta.graph.v1.python.v2.v2_task_status import SoupIsReadyFast  # noqa
        from crypta.graph.v1.python.v2.table_register import FlattenTasks  # noqa

        arguments = [self.root_task, "--date", dt, "--workers", "10", "--local-scheduler", "--no-lock"]
        if self.root_task == "GraphV2PostprocTask":
            arguments.extend(["--name", "postrock"])
        run_result = luigi.interface._run(arguments, detailed_summary=True)

        FlattenTasks(SoupIsReadyFast(dt)).mark_tables()
        print("Graph Status")
        check_run_status = run_result.status in (
            luigi.execution_summary.LuigiStatusCode.SUCCESS,
            luigi.execution_summary.LuigiStatusCode.SUCCESS_WITH_RETRY,
        )
        check_mon_status = check_monrun_status(start_time.date())
        check_sum_status = check_summary_status(run_result.worker)
        print(run_result)
        print(check_run_status)
        print(check_mon_status)
        print(check_sum_status)
        if check_run_status and check_mon_status and check_sum_status:
            print("Graph success")
            add_monrun_to_report(start_time.date())
            self.run_status = True
        else:
            print("Graph fail, left yt running")
            self.run_status = False
            try:
                report = trace_reader.parse_log()
                allure.attach("YT errors", (json.dumps(report.errors, sort_keys=True, indent=4)))
                add_error_info_to_report(yt, report)
                add_launch_info_to_report(report)
            except Exception as e:
                with pytest.allure.step("YT Errors data"):
                    allure.attach("ERROR can't parse yt_trace log", trace_reader.log_folder + "\n" + str(e))
            add_luigi_summary_to_report(run_result.worker)
            add_monrun_to_report(start_time.date())
            print(
                "Taking log from path",
                os.path.join(rtcrypta_pystarter_log_folder, today + ".syslog"),
                rtcrypta_pystarter_log_folder,
                os.environ.get("RTCRYPTA_PYSTARTER_LOG_FOLDER"),
            )
            luigi_reader = LuigiReader(start_datetime=start_time, log_folder=rtcrypta_pystarter_log_folder)
            luigi_logs = {
                record.split("error_message:")[-1]: record.split("error_message:")[0]
                for record in luigi_reader.parse_log()
            }
            for message, metadata in luigi_logs.iteritems():
                allure.attach(str(metadata), str(message))
        add_luigi_summary_to_report(run_result.worker)
        report = trace_reader.parse_log()
        add_launch_info_to_report(report)
        return report


def add_launch_info_to_report(report):
    with pytest.allure.step("Information of launch"):
        allure.attach("10 slowest operations", "\n".join(map(str, report.operations_list[-10:])))
        allure.attach("Count operations", str(len(report.operations_list)))
        allure.attach("No start count", str(report.no_start_count))
        allure.attach("No end count", str(report.no_end_count))
        allure.attach("Sum execution time all operation", str(report.sum_time))
        allure.attach("Execution time", str(report.exec_time))


def add_error_info_to_report(yt, report):
    def read_error_data(yt, name, error_paths):
        for path in error_paths:
            try:
                rows = [record for record in yt.read_table(path, raw=False)]
                allure.attach(name + path, str(rows))
            except Exception as e:
                allure.attach("ERROR can't read path", path + "\n" + str(e))

    with pytest.allure.step("YT Errors data"):
        for error_data in report.errors_data:
            with pytest.allure.step(error_data["name"]):
                read_error_data(yt, "INPUT ", error_data["inputs"])
                read_error_data(yt, "OUTPUT ", error_data["outputs"])
                allure.attach("reduce by ", str(error_data["reduce_by"]))


def add_luigi_summary_to_report(worker):
    summary = luigi.execution_summary.summary(worker)
    with pytest.allure.step("Luigi summary"):
        allure.attach("Result", str(summary))


def check_summary_status(worker):
    summary = luigi.execution_summary.summary(worker)
    return KEY_FOR_CHECK_LUIGI_SUMMARY in summary


def get_monrun_folder(date):
    return os.getenv("MONRUN_FOLDER") + str(date)


def add_monrun_to_report(date):
    err_code = "2; "
    path = get_monrun_folder(date) + "/"
    with pytest.allure.step("MONRUN errors"):
        for fname in os.listdir(path):
            full_path = path + fname
            if os.path.isfile(full_path):
                with open(full_path) as f:
                    text = f.read()
                    if err_code in text:
                        allure.attach(fname, text)


def check_monrun_status(date):
    pass_code = "0; "
    path = get_monrun_folder(date) + "/"
    for fname in os.listdir(path):
        full_path = path + fname
        if os.path.isfile(full_path):
            with open(full_path) as f:
                text = f.read()
                if pass_code not in text:
                    return False
    return True
