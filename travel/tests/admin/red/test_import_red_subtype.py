# -*- coding: utf-8 -*-

from travel.rasp.admin.admin.red.metaimport import RedMetaRouteImporter
from travel.rasp.admin.admin.red.models import Package, MetaRoute, MetaRouteStation

from common.models.schedule import RThread
from common.models.transport import TransportType, TransportSubtype
from travel.rasp.admin.lib.unittests.check_thread_mixin import CheckThreadMixin
from tester.factories import create_supplier, create_station
from tester.testcase import TestCase


class TestImportRedMetarouteSubtype(TestCase, CheckThreadMixin):
    def setUp(self):
        self.t_subtype = TransportSubtype.objects.create(t_type_id=TransportType.BUS_ID,
                                                         code='gazel', title_ru=u'Газель')
        self.t_subtype2 = TransportSubtype.objects.create(t_type_id=TransportType.BUS_ID,
                                                          code='gazel-23', title_ru=u'МММ Газел')

    def import_metapackage(self, package_subtype=None, route_subtype=None):
        red_package = Package.objects.create(title=u'Красный тестовый пакет', t_subtype=package_subtype)
        red_metaroute = MetaRoute.objects.create(
            package=red_package,
            title=u'Красный тестовый рейс',
            scheme=u'7.00',
            t_type_id=TransportType.BUS_ID,
            t_subtype=route_subtype,
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

    def test_import_package(self):
        self.import_metapackage()

        thread = RThread.objects.get()
        assert thread.t_subtype is None

    def test_global_subtype(self):
        self.import_metapackage(package_subtype=self.t_subtype)

        thread = RThread.objects.get()
        assert thread.t_subtype == self.t_subtype

    def test_local_subtype(self):
        self.import_metapackage(route_subtype=self.t_subtype)

        thread = RThread.objects.get()
        assert thread.t_subtype == self.t_subtype

    def test_local_override_package_subtype(self):
        self.import_metapackage(package_subtype=self.t_subtype2, route_subtype=self.t_subtype)

        thread = RThread.objects.get()
        assert thread.t_subtype == self.t_subtype
