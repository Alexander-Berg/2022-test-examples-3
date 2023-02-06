# -*- coding: utf-8 -*-

from travel.rasp.admin.admin.red.metaimport import RedMetaRouteImporter
from travel.rasp.admin.admin.red.models import Package, MetaRoute, MetaRouteStation

from common.models.schedule import RThread
from common.models.transport import TransportType
from tester.factories import create_supplier, create_station
from tester.testcase import TestCase
from tester.utils.datetime import replace_now


class TestRedEndDate(TestCase):
    def import_metapackage(self, scheme):
        red_package = Package.objects.create(title=u'Красный тестовый пакет')
        red_metaroute = MetaRoute.objects.create(
            package=red_package,
            title=u'Красный тестовый рейс',
            scheme=scheme,
            t_type_id=TransportType.BUS_ID,
            supplier=create_supplier(),
        )
        MetaRouteStation.objects.create(
            metaroute=red_metaroute,
            station=create_station(),
            arrival=None,
            departure=0,
            order=0,
        )
        MetaRouteStation.objects.create(
            metaroute=red_metaroute,
            station=create_station(),
            arrival=10,
            departure=None,
            order=1,
        )

        importer = RedMetaRouteImporter(red_metaroute)
        importer.import_metaroute()

    def test_extrapolatable(self):
        """ Проверяем, что без указания даты окончания периода хождения экстраполяция разрешена. """
        self.import_metapackage(scheme='5:00')

        thread = RThread.objects.get()
        assert thread.has_extrapolatable_mask

    @replace_now('2016-01-01 00:00:01')
    def test_not_extrapolatable(self):
        """ Проверяем, что при указании даты окончания периода хождения экстраполяция запрещена. """
        self.import_metapackage(scheme=u'5:00 (до 2016-02-11,1234567)')

        thread = RThread.objects.get()
        assert not thread.has_extrapolatable_mask
