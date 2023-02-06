import datetime
import logging
import os

import yatest.common
from yt.wrapper import ypath

from crypta.buchhalter.services.main.lib.common import (
    report_generator,
    test_helpers,
)
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tables,
    tests,
    utils,
)


INPUT_TABLES = {
    ypath.ypath_join("//input", x): "{}.yson".format(x)
    for x in ("table_1", "table_2")
}
OUTPUT_DIR = "//output"
REPORT_DATE = datetime.datetime(year=2021, month=7, day=1)
NAME_FORMAT = "%Y-%m-%d"
REPORT_NAME = REPORT_DATE.strftime(NAME_FORMAT)
SEGMENT_OWNERS_WITH_EMPTY_REPORT = ["segment_owner_5"]
SEGMENT_OWNERS_WITHOUT_EMPTY_REPORT = [
    "segment_owner_1",
    "segment_owner_2",
    "segment_owner_3",
    "segment_owner_4",
]
SEGMENT_OWNERS = SEGMENT_OWNERS_WITHOUT_EMPTY_REPORT + SEGMENT_OWNERS_WITH_EMPTY_REPORT
SEGMENT_OWNER_TO_DIRNAME = {x: "{}_dirname".format(x) for x in SEGMENT_OWNERS}
logger = logging.getLogger()


def test_report_generator(yt_stuff):
    yt_client = yt_stuff.get_yt_client()

    ttl_timedelta = datetime.timedelta(days=utils.get_unexpired_ttl_days_for_daily(REPORT_NAME))
    generator = report_generator.ReportGenerator(OUTPUT_DIR, SEGMENT_OWNER_TO_DIRNAME, ExcelReportRow, NAME_FORMAT, ttl_timedelta, logger)
    canonize_excel_data = test_helpers.CanonizeExcelData(ExcelReportRow)
    check_report_empty_attribute = test_helpers.CheckReportEmptyAttribute(lambda x: any(segment_owner in x.cypress_path for segment_owner in SEGMENT_OWNERS_WITH_EMPTY_REPORT))
    expiration_time_test = tests.ExpirationTimeByTableName(ttl_timedelta, "{}.xlsx".format(NAME_FORMAT))

    output_files = tests.yt_test_func(
        yt_client=yt_client,
        func=lambda: generator.generate_reports(yt_client, INPUT_TABLES.keys(), REPORT_DATE),
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable(local_filename, yt_path), [tests.TableIsNotChanged()])
            for yt_path, local_filename in INPUT_TABLES.iteritems()
        ],
        output_tables=[
            (cypress.CypressNode(OUTPUT_DIR), tests.TestNodesInMapNodeChildren([canonize_excel_data, check_report_empty_attribute, expiration_time_test], tag="output")),
        ],
        return_result=False,
    )

    return {
        os.path.basename(item.get("uri", item.get("local_path"))): item if "uri" in item else item["data"]
        for item in output_files
    }


class ExcelReportRow(report_generator.ExcelReportRowBase):
    class Field(object):
        x = "X"
        y = "Y"

    titles = [Field.x, Field.y]
    column_width = {
        Field.x: 10,
        Field.y: 10,
    }

    def __init__(self, item):
        self.x = item["x"]
        self.y = item["y"]

    @property
    def _mapping(self):
        return {
            self.Field.x: self.x,
            self.Field.y: self.y,
        }

    def valid(self):
        return self.y != 15
