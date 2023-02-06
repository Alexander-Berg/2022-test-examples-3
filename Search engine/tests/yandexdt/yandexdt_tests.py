from search.martylib.test_utils import TestCase
from search.mon.workplace.src.libs.yandexdt.yandexdt import YandexDtService
from search.mon.workplace.protoc.structures.ydt_spec_pb2 import (
    Query,
)
from search.mon.workplace.src.libs.yandexdt.yandexdt_exceptions import (
    BrokenQueryFormat,
    InvalidDateFormat,
)


class TestWorkplaceYandexDt(TestCase):

    def test_empty_query(self):
        with self.assertRaises(BrokenQueryFormat):
            YandexDtService.create_query(Query())

    def test_typical_query(self):
        query = Query(queue='TEST', start_date='01.01.1970', end_date='01.01.2019', sre_area=['test', 'test1'],
                      support_line='marty', vertical='web', services=['base', 'middle'])

        response = ' '.join([
            'Queue: TEST AND Components: web AND Support_line: "marty" AND',
            '("SRE Area":"test" OR "SRE Area":"test1") AND (Tags: "service:BASE" OR Tags: "service:MIDDLE") AND',
            'Created: >= 01.01.1970 AND Created: <= 01.01.2019 AND',
            '("Yandex  Downtime": > 0 OR "Vertical  Downtime": > 0)',
        ])

        self.assertEqual(YandexDtService.create_query(query), response)

    def test_periods_valid(self):
        with self.assertRaises(InvalidDateFormat):
            YandexDtService.periods_is_valid('abcdee', 'lol')

        with self.assertRaises(InvalidDateFormat):
            YandexDtService.periods_is_valid('31.12.2019', '01.01.2019')

        with self.assertRaises(InvalidDateFormat):
            YandexDtService.periods_is_valid('2019.01.01', '2019.05.01')

        self.assertEqual(YandexDtService.periods_is_valid('01.01.2019', '01.05.2019'), None)
