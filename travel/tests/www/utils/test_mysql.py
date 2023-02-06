# coding: utf-8

from __future__ import unicode_literals

import pytest


from common.apps.facility.factories import create_suburban_facility
from common.apps.facility.models import SuburbanThreadFacility
from common.models.schedule import RThread, Route, TrainPurchaseNumber
from common.utils.date import RunMask
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage
from route_search.models import ZNodeRoute2
from tester.factories import create_supplier, create_thread, create_route
from travel.rasp.admin.www.utils.mysql import (fast_delete_supplier_threads_and_routes, fast_delete_package_threads_and_routes,
                             fast_delete_threads, fast_delete_threads_with_routes, fast_delete_routes,
                             assert_has_no_live_changes, HasLiveChangesError)
from tester.testcase import TestCase


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


@pytest.mark.dbuser
def test_fast_delete_supplier_threads_and_routes():
    supplier = create_supplier(code='supplier')

    create_thread(
        supplier=supplier,
        route={'supplier': supplier, 'script_protected': False, 'two_stage_package': None},
        __={'calculate_noderouteadmin': True}
    )

    fast_delete_supplier_threads_and_routes(supplier=supplier)

    assert not Route.objects.filter(supplier=supplier, two_stage_package=None).exists()
    assert not RThread.objects.filter(supplier=supplier, route__two_stage_package=None).exists()
    assert not ZNodeRoute2.objects.filter(supplier=supplier, two_stage_package=None).exists()


@pytest.mark.dbuser
def test_fast_delete_supplier_threads_and_routes_with_another_thread():
    """ Проверяем, что удаление не затронет другие нитки """
    supplier = create_supplier(code='supplier')
    other_supplier = create_supplier(code='other_supplier')

    create_thread(
        supplier=supplier,
        route={'supplier': supplier, 'script_protected': False, 'two_stage_package': None},
        __={'calculate_noderouteadmin': True}
    )
    create_thread(
        supplier=other_supplier,
        route={'supplier': other_supplier, 'script_protected': False, 'two_stage_package': None},
        __={'calculate_noderouteadmin': True}
    )
    package = TwoStageImportPackage.objects.create(title=u'Test Package', supplier=other_supplier)
    create_thread(
        supplier=supplier,
        route={'supplier': supplier, 'script_protected': False, 'two_stage_package': package},
        __={'calculate_noderouteadmin': True}
    )

    fast_delete_supplier_threads_and_routes(supplier=supplier)

    assert not Route.objects.filter(supplier=supplier, two_stage_package=None).exists()
    assert not RThread.objects.filter(supplier=supplier, route__two_stage_package=None).exists()
    assert not ZNodeRoute2.objects.filter(supplier=supplier, two_stage_package=None).exists()

    assert Route.objects.filter(supplier=other_supplier, two_stage_package=None).exists()
    assert RThread.objects.filter(supplier=other_supplier, route__two_stage_package=None).exists()
    assert ZNodeRoute2.objects.filter(supplier=other_supplier, two_stage_package=None).exists()

    assert Route.objects.filter(supplier=supplier, two_stage_package=package).exists()
    assert RThread.objects.filter(supplier=supplier, route__two_stage_package=package).exists()
    assert ZNodeRoute2.objects.filter(supplier=supplier, two_stage_package=package).exists()


@pytest.mark.dbuser
def test_fast_delete_package_threads_and_routes():
    supplier = create_supplier(code='supplier')
    package = TwoStageImportPackage.objects.create(title=u'Test Package', supplier=supplier)

    create_thread(
        supplier=supplier,
        route={'supplier': supplier, 'script_protected': False, 'two_stage_package': package},
        __={'calculate_noderouteadmin': True}
    )

    fast_delete_package_threads_and_routes(two_stage_package=package)

    assert not Route.objects.filter(supplier=supplier, two_stage_package=package).exists()
    assert not RThread.objects.filter(supplier=supplier, route__two_stage_package=package).exists()
    assert not ZNodeRoute2.objects.filter(supplier=supplier, two_stage_package=package).exists()


@pytest.mark.dbuser
def test_fast_delete_package_threads_and_routes_with_another_thread():
    """ Проверяем, что удаление не затронет другие нитки """
    supplier = create_supplier(code='supplier')
    package = TwoStageImportPackage.objects.create(title=u'Test Package', supplier=supplier)
    other_package = TwoStageImportPackage.objects.create(title=u'Other Test Package', supplier=supplier)

    create_thread(
        supplier=supplier,
        route={'supplier': supplier, 'script_protected': False, 'two_stage_package': package},
        __={'calculate_noderouteadmin': True}
    )
    create_thread(
        supplier=supplier,
        route={'supplier': supplier, 'script_protected': False, 'two_stage_package': other_package},
        __={'calculate_noderouteadmin': True}
    )
    create_thread(
        supplier=supplier,
        route={'supplier': supplier, 'script_protected': False, 'two_stage_package': None},
        __={'calculate_noderouteadmin': True}
    )

    fast_delete_package_threads_and_routes(two_stage_package=package)

    assert not Route.objects.filter(two_stage_package=package).exists()
    assert not RThread.objects.filter(route__two_stage_package=package).exists()
    assert not ZNodeRoute2.objects.filter(two_stage_package=package).exists()

    assert Route.objects.filter(two_stage_package=other_package).exists()
    assert RThread.objects.filter(route__two_stage_package=other_package).exists()
    assert ZNodeRoute2.objects.filter(two_stage_package=other_package).exists()

    assert Route.objects.filter(two_stage_package=None).exists()
    assert RThread.objects.filter(route__two_stage_package=None).exists()
    assert ZNodeRoute2.objects.filter(two_stage_package=None).exists()


@pytest.mark.dbuser
def test_fast_delete_threads():
    create_thread()
    thread = create_thread()
    thread_facility = SuburbanThreadFacility.objects.create(year_days=RunMask.ALL_YEAR_DAYS, thread=thread)
    thread_facility.facilities.add(create_suburban_facility())
    TrainPurchaseNumber.objects.create(thread=thread, number='7099')

    fast_delete_threads(RThread.objects.all())

    assert SuburbanThreadFacility.objects.count() == 0
    assert RThread.objects.count() == 0
    assert TrainPurchaseNumber.objects.count() == 0


@pytest.mark.dbuser
class TestFastDeleteThreadsWithRoutes(object):
    def test_delete_all_threads(self):
        route = create_route()
        thread1 = create_thread(route=route, ordinal_number=1)
        thread2 = create_thread(route=route, ordinal_number=2)

        fast_delete_threads_with_routes(RThread.objects.filter(id__in=[thread1.id, thread2.id]))

        assert not Route.objects.filter(id=route.id).exists()
        assert not RThread.objects.filter(id__in=[thread1.id, thread2.id]).exists()

    def test_delete_part_of_threads(self):
        route = create_route()
        thread1 = create_thread(route=route, ordinal_number=1)
        thread2 = create_thread(route=route, ordinal_number=2)

        fast_delete_threads_with_routes(RThread.objects.filter(id__in=[thread1.id]))

        assert Route.objects.filter(id=route.id).exists()
        assert not Route.objects.filter(id=thread1.id).exists()
        assert RThread.objects.filter(id=thread2.id).exists()

    def test_script_protected_route(self):
        route = create_route(script_protected=True)
        thread1 = create_thread(route=route, ordinal_number=1)
        thread2 = create_thread(route=route, ordinal_number=2)

        fast_delete_threads_with_routes(RThread.objects.filter(id__in=[thread1.id, thread2.id]))

        assert Route.objects.filter(id=route.id).exists()
        assert not RThread.objects.filter(id__in=[thread1.id, thread2.id]).exists()

        thread1 = create_thread(route=route, ordinal_number=1)
        thread2 = create_thread(route=route, ordinal_number=2)

        fast_delete_threads_with_routes(RThread.objects.filter(id__in=[thread1.id, thread2.id]),
                                        keep_empty_script_protected_routes=False)

        assert not Route.objects.filter(id=route.id).exists()
        assert not RThread.objects.filter(id__in=[thread1.id, thread2.id]).exists()


@pytest.mark.dbuser
def test_assert_has_no_live_changes():
    basic_thread = create_thread()
    thread = create_thread(basic_thread=basic_thread)

    assert_has_no_live_changes([thread.id])
    assert_has_no_live_changes([thread.id, basic_thread.id])

    with pytest.raises(HasLiveChangesError):
        assert_has_no_live_changes([basic_thread.id])
