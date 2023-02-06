from search.martylib.test_utils import TestCase
from search.mon.workplace.src.libs.catalog.service import filter_records
from search.mon.workplace.protoc.structures.catalog_pb2 import (
    CatalogAbcService,
    RecordFilter,
    Record,
)


class TestWorkplaceListRecords(TestCase):
    objects = [
        Record(service='test', vertical='lol', id=1, product='web', abc=[]),
        Record(service='kek', vertical='mail', id=2, product='prod-mail', abc=[]),
        Record(service='pop3', vertical='mail', id=3, product='prod-mail', description='good service', abc=[]),
        Record(service='imap', vertical='mail', id=4, product='vertical:mail', abc=[]),
        Record(service='base', vertical='web', id=5, product='web', abc=[]),
        Record(service='middlesearch', vertical='web', id=6, product='web', abc=[]),
        Record(service='report', vertical='web', id=7, product='web', abc=[]),
    ]

    def test_filter_empty(self):
        filter = RecordFilter()
        self.assertEqual(
            filter_records(self.objects, filter),
            self.objects,
        )

    def test_filter_vertical(self):
        filter = RecordFilter()
        filter.vertical.append('lol')

        self.assertEqual(
            filter_records(self.objects, filter),
            [Record(service='test', vertical='lol', id=1, product='web', abc=[])]
        )

    def test_filter_vertical_and_service(self):
        filter = RecordFilter()
        filter.vertical.append('mail')
        filter.service = 'pop3'

        self.assertEqual(
            filter_records(self.objects, filter),
            [
                Record(service='kek', vertical='mail', id=2, product='prod-mail', abc=[]),
                Record(service='pop3', vertical='mail', id=3, product='prod-mail', description='good service', abc=[]),
                Record(service='imap', vertical='mail', id=4, product='vertical:mail', abc=[]),
            ]
        )

    def test_filter_nonexistent(self):
        filter = RecordFilter()
        filter.vertical.append('zora')

        self.assertEqual(
            filter_records(self.objects, filter),
            [],
        )

    def test_filter_by_name(self):
        self.assertEqual(
            filter_records(self.objects, RecordFilter(service='report')),
            [Record(service='report', vertical='web', id=7, product='web', abc=[])],
        )

    def test_filter_by_id(self):
        self.assertEqual(
            filter_records(self.objects, RecordFilter(id=1)),
            [Record(service='test', vertical='lol', id=1, product='web', abc=[])],
        )

    def test_filter_by_empty_vertical(self):
        self.assertEqual(
            filter_records(self.objects, RecordFilter(vertical=[])),
            self.objects,
        )

    def test_filter_by_slug(self):
        self.assertEqual(
            filter_records([
                Record(id=1, abc=[CatalogAbcService(id=1, slug='basesearch')]),
                Record(id=2, abc=[CatalogAbcService(id=2, slug='middlesearch')]),
                Record(id=3, abc=[CatalogAbcService(id=3, slug='ololo')]),
                Record(id=4, abc=[CatalogAbcService(id=1, slug='basesearch')]),
            ], RecordFilter(abc__slug='basesearch')),
            [
                Record(id=1, abc=[CatalogAbcService(id=1, slug='basesearch')]),
                Record(id=4, abc=[CatalogAbcService(id=1, slug='basesearch')]),
            ]
        )
