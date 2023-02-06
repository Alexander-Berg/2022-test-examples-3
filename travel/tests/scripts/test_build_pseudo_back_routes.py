# -*- coding: utf-8 -*-

from common.models.schedule import RThread, RThreadType
from common.models.transport import TransportType
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage
from travel.rasp.admin.scripts.build_pseudo_back_routes import PseudoRouteBuilder
from tester.testcase import TestCase
from tester.factories import create_station, create_thread, create_supplier


class TestBuildPseudoBackRoutes(TestCase):
    def test_simple_creation(self):
        t_type = TransportType.objects.get(id=TransportType.BUS_ID)
        create_supplier(code='pseudo')

        supplier = create_supplier(code='test')
        tsi_package = TwoStageImportPackage.objects.create(allow_back_pseudo_routes=True, supplier=supplier)

        def make_thread(station1, station2, number):
            thread = create_thread(
                schedule_v1=[
                    [None, 0, station1],
                    [100, None, station2],
                ],
                import_uid=number,
                uid=number,
            )
            thread.route.two_stage_package = tsi_package
            thread.route.t_type = t_type
            thread.route.save()
            return thread

        station_a = create_station(title='A')
        station_b = create_station(title='B')

        make_thread(station_a, station_b, '1')
        PseudoRouteBuilder().run()
        assert RThread.objects.filter(type_id=RThreadType.PSEUDO_BACK_ID).count() == 1

        make_thread(station_b, station_a, '2')
        PseudoRouteBuilder().run()
        assert RThread.objects.filter(type_id=RThreadType.PSEUDO_BACK_ID).count() == 0
