from logging import Logger
from mock import Mock
from typing import cast

from travel.avia.stat_admin.data.models import Settlement, Airport
from travel.avia.stat_admin.lib.avia_importers import TitleModelImporter
from travel.avia.stat_admin.tester.testcase import TestCase


class TestSettlementImporter(TestCase):
    def setUp(self):
        self._importer = TitleModelImporter(
            Model=Settlement,
            logger=cast(Logger, Mock())
        )

    def test_ignore_duplicates(self):
        settlement = Settlement.objects.create(avia_id=1, title='some')

        info = self._importer.import_models([{
            'id': settlement.avia_id,
            'title': settlement.title
        }])

        assert info == {
            'new': 0,
            'changed': 0,
            'ignored': 1
        }
        assert Settlement.objects.count() == 1

    def test_update_settlement(self):
        settlement = Settlement.objects.create(avia_id=1, title='some')

        info = self._importer.import_models([{
            'id': settlement.avia_id,
            'title': 'other'
        }])

        assert info == {
            'new': 0,
            'changed': 1,
            'ignored': 0
        }
        assert Settlement.objects.get(avia_id=1).title == 'other'

    def test_add_new_settlement(self):
        info = self._importer.import_models([{
            'id': 123,
            'title': 'other'
        }])

        assert info == {
            'new': 1,
            'changed': 0,
            'ignored': 0
        }
        assert Settlement.objects.count() == 1
        s = Settlement.objects.all()[0]
        assert s.title == 'other'
        assert s.avia_id == 123


class TestAirportImporter(TestCase):
    def setUp(self):
        self._importer = TitleModelImporter(
            Model=Airport,
            logger=cast(Logger, Mock())
        )

    def test_ignore_duplicate(self):
        airport = Airport.objects.create(avia_id=1, title='some')

        info = self._importer.import_models([{
            'id': airport.avia_id,
            'title': airport.title
        }])

        assert info == {
            'new': 0,
            'changed': 0,
            'ignored': 1
        }
        assert Airport.objects.count() == 1

    def test_update_settlement(self):
        airport = Airport.objects.create(avia_id=1, title='some')

        info = self._importer.import_models([{
            'id': airport.avia_id,
            'title': 'other'
        }])

        assert info == {
            'new': 0,
            'changed': 1,
            'ignored': 0
        }
        assert Airport.objects.get(avia_id=1).title == 'other'

    def test_add_new_airport(self):
        info = self._importer.import_models([{
            'id': 123,
            'title': 'other'
        }])

        assert info == {
            'new': 1,
            'changed': 0,
            'ignored': 0
        }
        assert Airport.objects.count() == 1
        s = Airport.objects.all()[0]
        assert s.title == 'other'
        assert s.avia_id == 123
