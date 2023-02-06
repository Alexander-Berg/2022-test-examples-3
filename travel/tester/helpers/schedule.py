# -*- coding: utf-8 -*-


from datetime import time

import yaml

from travel.avia.library.python.common.utils import environment
from travel.avia.library.python.common.utils.date import RunMask

from travel.avia.library.python.common.models.geo import Settlement, Station
from travel.avia.library.python.common.models.schedule import RThread, Route, RTStation, Supplier
from travel.avia.library.python.common.models.transport import TransportType

from travel.avia.library.python.tester.helpers.mask_description import make_dates_from_mask_description


def load_schedule_fixture(data):
    dd = yaml.load(data)
    today = environment.today()

    suppliers = {}
    settlements = {}
    stations = {}
    routes = {}

    for title, supplier_info in dd.get('suppliers', {}).items():
        supp = Supplier()
        supp.title = title
        supp.code = supplier_info['code']
        supp.save()

        suppliers[title] = supp

    for title, settlement_info in dd.get('settlements', {}).items():
        sett = Settlement()
        sett.title = title
        sett.majority_id = settlement_info.get('majority', 2)
        sett.time_zone = settlement_info['timezone']
        sett.save()

        settlements[title] = sett

    for title, station_info in dd.get('stations', {}).items():
        st = Station()
        if 'id' in station_info:
            st.id = station_info['id']

        st.title = title
        st.majority_id = station_info.get('majority', 2)
        st.time_zone = station_info['timezone']

        st.t_type = TransportType.objects.get(code=station_info['t_type'])
        st.settlement = get_object(Settlement, settlements, station_info['settlement'])

        st.save()

        for code_system, code in station_info.get('codes', {}).items():
            code_system = unicode(code_system)
            code = unicode(code)
            st.set_code(code_system, code)

        stations[title] = st

    for number, route_info in dd.get('routes', {}).items():
        number = unicode(number)
        route = Route()
        route.number = number
        route.t_type = TransportType.objects.get(code=route_info['t_type'])
        route.supplier = get_object(Supplier, suppliers, route_info['supplier'])

        threads = {}
        for number_postfix, thread_info in route_info.get('threads', {}).items():
            thread = RThread()
            thread.type_id = 1
            thread.number = route.number + number_postfix
            thread.uid = thread_info['uid']
            thread.time_zone = thread_info['timezone']
            thread.tz_start_time = time(*map(int, thread_info['start_time'].split(':')))
            thread.year_days = RunMask(
                today=today, days=make_dates_from_mask_description(thread_info['mask']))

            thread.rtstations = []
            for rts_info in thread_info['rtstations']:
                rts = RTStation()
                rts.tz_arrival = rts_info[0]
                rts.tz_departure = rts_info[1]
                rts.station = get_object(Station, stations, rts_info[2])
                rts.time_zone = rts_info[3]

                thread.rtstations.append(rts)

            threads[number_postfix] = thread

        route.threads = list(threads.values())
        route.route_uid = route_info['route_uid']

        route.save()

        for thread in threads.values():
            thread.route = route
            thread.save()

            for rts in thread.rtstations:
                rts.thread = thread
                rts.save()

        routes[number] = route

        route.thread_by_postfix = threads

    return {
        'settlements': settlements,
        'stations': stations,
        'suppliers': suppliers,
        'routes': routes
    }


def get_object(klass, loaded, ref):
    if ref in loaded:
        return loaded[ref]

    return klass.objects.get(pk=ref)
