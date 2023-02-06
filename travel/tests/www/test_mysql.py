# -*- coding: utf-8 -*-

from travel.avia.library.python.common.models.schedule import Route
from travel.avia.library.python.tester.factories import create_route, create_supplier
from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.admin.www.utils.mysql import fast_delete_routes


class MysqlUtilsTest(TestCase):
    def testFastDeleteRoutes(self):
        create_route(supplier={'code': 'supplier_1'})
        create_route(supplier={'code': 'supplier_2'})

        fast_delete_routes(Route.objects.filter(supplier__code='supplier_1'))

        assert not Route.objects.filter(supplier__code='supplier_1').exists()
        assert Route.objects.filter(supplier__code='supplier_2').exists()

    def testFastDeleteRoutesWithRtstationFilter(self):
        supplier = create_supplier(code='supplier')
        route_500 = create_route(supplier=supplier, __={'threads': [
            {'schedule_v1': [[None, 0], [500, None]]}
        ]})
        route_200 = create_route(supplier=supplier, __={'threads': [
            {'schedule_v1': [[None, 0], [200, None]]}
        ]})

        fast_delete_routes(Route.objects.filter(supplier__code='supplier', rthread__rtstation__tz_arrival=500))

        assert not Route.objects.filter(id=route_500.id).exists()
        assert Route.objects.filter(id=route_200.id).exists()
