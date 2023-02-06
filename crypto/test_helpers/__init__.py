import xlrd

from crypta.buchhalter.services.main.lib.common import report_generator
from crypta.lib.python.yt.test_helpers import tests


class CanonizeExcelData(tests.YtTest):
    def __init__(self, excel_report_row_class):
        super(CanonizeExcelData, self).__init__()
        self.excel_report_row_class = excel_report_row_class

    def teardown(self, yt_file, yt_client):
        yt_file.read_from_local(yt_client)
        workbook = xlrd.open_workbook(yt_file.file_path)
        sheet = workbook.sheet_by_index(0)

        data = []
        for index, row in enumerate(sheet.get_rows()):
            if not index:
                assert self.excel_report_row_class.titles == [x.value for x in row]
            else:
                data.append(dict(zip(self.excel_report_row_class.titles, [x.value for x in row])))

        return [{"local_path": yt_file.file_path, "data": data}]


class CheckReportEmptyAttribute(tests.YtTest):
    def __init__(self, expected_getter):
        super(CheckReportEmptyAttribute, self).__init__()
        self.expected_getter = expected_getter

    def teardown(self, yt_file, yt_client):
        assert self.expected_getter(yt_file) == report_generator.is_report_empty(yt_file.cypress_path, yt_client)
