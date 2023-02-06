# -*- coding: utf-8 -*-

from datetime import datetime, timedelta

import pytest

from travel.rasp.admin.admin.red.models import Package, MetaRoute
from common.models.schedule import RThread
from common.models.transport import TransportType
from travel.rasp.library.python.common23.date import environment
from common.utils.date import daterange, RunMask
from travel.rasp.admin.scripts.z_station_schedule import BusProcessor, MTABusProcessor, RedBusProcessor, extrapolate_and_gen_textmask
from tester.factories import create_thread, create_supplier, create_route
from tester.utils.datetime import replace_now


@pytest.mark.dbuser
@replace_now(datetime(2016, 1, 1))
def test_bus_no_extrapolation():
    """
    https://st.yandex-team.ru/RASPADMIN-1055
    Проверяем, что автобусные рейсы, загруженные через электричечный формат, не экстраполируются
    """
    thread_days = list(daterange(environment.today(), environment.today() + timedelta(days=40)))
    supplier = create_supplier(code='not_mta')
    create_thread(
        t_type='bus', year_days=RunMask(days=thread_days, today=environment.today()), supplier=supplier,
        route=create_route(supplier=supplier, t_type='bus'), changed=True
    )
    extrapolate_and_gen_textmask(processors=[BusProcessor, MTABusProcessor, RedBusProcessor])

    thread = RThread.objects.get()
    assert thread.year_days == str(RunMask(days=thread_days, today=environment.today()))


@pytest.mark.dbuser
@replace_now(datetime(2016, 1, 1))
def test_red_bus_extrapolation():
    """
    https://st.yandex-team.ru/RASPADMIN-1055
    Проверяем, что автобусные рейсы из КГА могут экстраполироваться
    """
    thread_days = list(daterange(environment.today(), environment.today() + timedelta(days=40)))
    supplier = create_supplier(code='not_mta')
    red_package = Package.objects.create(title=u'Красный тестовый пакет')
    red_metaroute = MetaRoute.objects.create(package=red_package, title=u'Красный тестовый рейс',
                                             supplier=supplier, t_type_id=TransportType.BUS_ID)
    route = create_route(supplier=supplier, t_type='bus', red_metaroute=red_metaroute)
    create_red_thread = create_thread.mutate(
        t_type='bus', year_days=RunMask(days=thread_days, today=environment.today()),
        supplier=supplier, route=route, changed=True
    )
    create_red_thread(number='extrapolatable', has_extrapolatable_mask=True, ordinal_number=1)
    create_red_thread(number='not_extrapolatable', has_extrapolatable_mask=False, ordinal_number=2)
    extrapolate_and_gen_textmask(processors=[BusProcessor, MTABusProcessor, RedBusProcessor])

    not_extrapolatable_thread = RThread.objects.get(number='not_extrapolatable')
    assert not_extrapolatable_thread.year_days == str(RunMask(days=thread_days, today=environment.today()))

    extrapolatable_thread = RThread.objects.get(number='extrapolatable')
    extrapolated_days = RunMask(mask=extrapolatable_thread.year_days, today=environment.today()).dates()
    assert all(day in extrapolated_days for day in thread_days)
    assert len(extrapolated_days) > len(thread_days)
