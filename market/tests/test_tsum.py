import json
from unittest import TestCase

from lib import tsum
from lib.tsum import Resource
from lib.tsum import TsumReport
from lib.tsum import TsumReportItem


class TestTsumReport(TestCase):

    def test_get_report_data(self):
        report = TsumReport()
        report.service_name = 'testing_market_light_matcher_fol'
        report.service_id = 'testing_market_light_matcher_fol-1493723214109'
        report.environment_tag = 'a_ctype_testing'
        report.host = 'some-host.search.yandex.net'

        resource = Resource({'resourceType': 'MARKET_TEST_APP', 'taskId': 12345})

        file_check_sums = [
            {'path': '/first', 'md5': '11111'},
            {'path': '/second', 'md5': '222222'}
        ]

        report_item = TsumReportItem(resource, file_check_sums)

        report.report_items.append(report_item)

        expected = {
            'confId': {
                'service': 'testing_market_light_matcher_fol',
                'id': 'testing_market_light_matcher_fol-1493723214109'
            },
            'environmentTag': 'a_ctype_testing',
            'reportItems': [
                {
                    'checksums': [
                        {'md5': '11111', 'path': '/first'},
                        {'md5': '222222', 'path': '/second'}
                    ],
                    'resource': {
                        'resourceType': 'MARKET_TEST_APP',
                        'taskId': 12345
                    }
                }
            ],
            'updatedMillis': 0,
            'host': 'some-host.search.yandex.net',
            'deployInProgress': False
        }

        self.assertDictEqual(report.get_report_data(), expected)

    def test_extract_validation_info(self):
        tsum_response = json.loads('''{
  "files": [{
    "resource": {
      "resourceType": "MARKET_MATCHER_APP",
      "taskId": "112402153"
    },
    "instanceFile": "market-matcher.tar",
    "filesToCheck": [
      "mbi-partner-1.1.61-SNAPSHOT/lib/mbi-partner-1.1.61-SNAPSHOT.jar",
      "mbi-partner-1.1.61-SNAPSHOT/lib/mbi-bidding-client-1.1.61-SNAPSHOT.jar"
    ]
  }]
}''')

        expected_resource = {"resourceType": "MARKET_MATCHER_APP", "taskId": "112402153"}
        expected_files = [
            "mbi-partner-1.1.61-SNAPSHOT/lib/mbi-partner-1.1.61-SNAPSHOT.jar",
            "mbi-partner-1.1.61-SNAPSHOT/lib/mbi-bidding-client-1.1.61-SNAPSHOT.jar"
        ]

        info = tsum.extract_validation_info(tsum_response)
        self.assertEqual(expected_resource, info[0].resource.get_report_data())
        self.assertEqual(expected_files, info[0].files_to_check)
