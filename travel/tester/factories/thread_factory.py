# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from future import standard_library
standard_library.install_aliases()
from builtins import map
import json
import six
from six.moves.collections_abc import Iterable
from datetime import time, date
from itertools import combinations

from django.db.models.fields.related import ForeignObjectRel

from travel.rasp.library.python.common23.date.date_const import MSK_TIMEZONE
from travel.rasp.library.python.common23.models.transport.transport_type import TransportType
from travel.rasp.library.python.common23.models.core.schedule.rthread_type import RThreadType
from travel.rasp.library.python.common23.date.run_mask import RunMask
from travel.rasp.library.python.common23.models.core.geo.title_generator import build_simple_title_common, TitleGenerator
from travel.rasp.library.python.common23.models.core.schedule.rthread import RThread
from travel.rasp.library.python.common23.tester.factories.base_factory import ModelFactory, factories
from travel.rasp.library.python.common23.tester.factories.factories import (
    create_supplier, create_transport_type, create_rtstation
)
from travel.rasp.library.python.common23.tester.factories.station_factory import create_station
from travel.rasp.library.python.common23.tester.factories.route_factory import create_route

from travel.rasp.library.python.common23.tester.helpers.mask_description import run_mask_from_mask_description


class ThreadFactory(ModelFactory):
    Model = RThread

    default_kwargs = {
        'year_days': '1' * RunMask.MASK_LENGTH,
        'tz_start_time': time(0),
        't_type': 'bus',
        'type': 'basic',
        'supplier': {},
        'schedule_v1': [
            [None, 0],
            [10, None],
        ],
        'ordinal_number': 1,
        'number': 'НомерНитки',
        'time_zone': MSK_TIMEZONE,
        'import_uid': lambda: ThreadFactory.gen_thread_uuid(),
        'uid': lambda: ThreadFactory.gen_thread_uid(),
    }

    @classmethod
    def gen_thread_uuid(cls):
        cls.thread_import_uid_counter = getattr(cls, 'thread_import_uid_counter', 0) + 1
        return 'THREAD_IMPORT_UUID_{}'.format(cls.thread_import_uid_counter)

    @classmethod
    def gen_thread_uid(cls):
        cls.uid_counter = getattr(cls, 'uid_counter', 0) + 1
        return 'THREAD_UID_{}'.format(cls.uid_counter)

    def create_object(self, kwargs):
        extra_params = kwargs.pop('__', None) or {}
        calculate_noderoute = extra_params.get('calculate_noderoute', False)
        calculate_noderouteadmin = extra_params.get('calculate_noderouteadmin', False)

        # Example: stops_translations = {'ru': 'остановки!'}
        stops_translations = extra_params.get('stops_translations')
        stops_translations_str = json.dumps(stops_translations) if stops_translations else ''

        kwargs['t_type'] = create_transport_type(kwargs.pop('t_type', None))
        kwargs['supplier'] = create_supplier(kwargs.pop('supplier', None))
        kwargs['year_days'] = self.build_year_days(kwargs.pop('year_days', None))
        kwargs['tz_start_time'] = self.build_time(kwargs.pop('tz_start_time', None))
        kwargs['begin_time'] = self.build_time(kwargs.pop('begin_time', None))
        kwargs['end_time'] = self.build_time(kwargs.pop('end_time', None))

        rtstations = []

        if 'schedule_v1' in kwargs:
            rtstation_infos = kwargs.pop('schedule_v1')
            for rts_info in rtstation_infos:
                if len(rts_info) == 2:
                    (tz_arrival, tz_departure), station, rts_kwargs_update = rts_info, {}, {}
                elif len(rts_info) == 3:
                    (tz_arrival, tz_departure, station), rts_kwargs_update = rts_info, {}
                elif len(rts_info) == 4:
                    tz_arrival, tz_departure, station, rts_kwargs_update = rts_info
                else:
                    raise ValueError(rts_info)

                rts_kwargs = {
                    'tz_arrival': tz_arrival,
                    'tz_departure': tz_departure,
                    'station': create_station(station),
                }
                rts_kwargs.update(rts_kwargs_update)

                rtstations.append(rts_kwargs)

        route_kwargs = kwargs.pop('route', {
            '__': None,
            't_type': kwargs['t_type'],
            'supplier': kwargs['supplier']
        })
        route = create_route(route_kwargs)
        kwargs['route'] = route

        if 'number' in kwargs:
            if ('reversed_number' not in kwargs) or not kwargs['reversed_number']:
                kwargs['reversed_number'] = kwargs['number'][::-1]

        title_strategy = kwargs.pop('title_strategy', None)
        generate_title = kwargs.pop('generate_title', None)

        thread = super(ThreadFactory, self).create_object(kwargs)

        created_rtstations = []
        for rts_kwargs in rtstations:
            rts_kwargs['thread'] = thread

            created_rtstations.append(create_rtstation(rts_kwargs))

        # Много логики в наших библиотеках (типа route_search и stationschedule) расчитывает, что
        # у любой нитки есть как минимум 2 станции, и что у первой станции arrival = None,
        # а у последней departure = None. Всё это проверяется на уровне импортов.
        # В тестах, соответственно, необходимо создать такие же условия.
        assert len(created_rtstations) >= 2
        assert created_rtstations[0].tz_arrival is None
        assert created_rtstations[-1].tz_departure is None

        def get_point_for_title(rts):
            return rts.station.settlement or rts.station

        thread.title_common = build_simple_title_common(
            thread.t_type, [get_point_for_title(created_rtstations[0]), get_point_for_title(created_rtstations[-1])])

        if generate_title and thread.title is None:
            generator = TitleGenerator(thread, strategy=title_strategy)
            generator.generate()
            thread.title = generator.title

        thread.save()

        if calculate_noderoute:
            from route_search.models import ZNodeRoute2
            for rts_from, rts_to in combinations(thread.rtstation_set.all(), 2):
                ZNodeRoute2(
                    route_id=thread.route_id,
                    thread=thread,
                    t_type_id=thread.t_type_id,
                    settlement_from=rts_from.station.settlement,
                    station_from_id=rts_from.station_id,
                    rtstation_from=rts_from,
                    settlement_to=rts_to.station.settlement,
                    station_to_id=rts_to.station_id,
                    rtstation_to=rts_to,
                    stops_translations=stops_translations_str
                ).save()

        if calculate_noderouteadmin:
            from route_search.models import ZNodeRoute2
            for rts_from, rts_to in combinations(thread.rtstation_set.all(), 2):
                ZNodeRoute2(
                    route_id=thread.route_id,
                    thread=thread,
                    t_type_id=thread.t_type_id,
                    settlement_from=rts_from.station.settlement,
                    station_from_id=rts_from.station_id,
                    rtstation_from=rts_from,
                    settlement_to=rts_to.station.settlement,
                    station_to_id=rts_to.station_id,
                    rtstation_to=rts_to,
                    stops_translations=stops_translations_str,
                    supplier=thread.supplier,
                    two_stage_package=route.two_stage_package
                ).save()

        return thread

    @classmethod
    def build_year_days(cls, year_days_param):
        if isinstance(year_days_param, six.string_types):
            if year_days_param.isdigit() and len(year_days_param) == RunMask.MASK_LENGTH:
                return year_days_param

            return run_mask_from_mask_description(year_days_param)

        if isinstance(year_days_param, RunMask):
            return six.text_type(year_days_param)
        if isinstance(year_days_param, Iterable):
            dates = list(year_days_param)
            if all(isinstance(d, date) for d in dates):
                return six.text_type(RunMask(days=list(year_days_param)))

        raise ValueError('Bad year_days parameter {!r}'.format(year_days_param))

    def build_time(self, time_param):
        if time_param is None:
            return None
        if isinstance(time_param, time):
            return time_param
        if isinstance(time_param, six.string_types):
            return time(*list(map(int, time_param.split(':'))))

        raise ValueError('Bad time parameter {!r}'.format(time_param))


create_thread = ThreadFactory()
factories[RThread] = create_thread


def create_change_thread(basic_thread, year_days, changes=None, schedule_v1=None, **kwargs):
    """
    Create a CHANGE thread for a basic_thread, based on it's params.
    Function will also change a basic_thread.year_days so it will be in sync with year_days

    Example:

    thread = create_thread(
            schedule_v1=[
                [None, 0, st_from],
                [5, 10, st_mid],
                [15, None, st_to],
            ],
            year_days=[day1, day2, day3])

    ch_thread = create_change_thread(thread, [day1], changes={
        st_mid: {'cancelled': True},
        st_to: {'arrival': +100},
    })

    """

    if basic_thread.t_type_id != TransportType.SUBURBAN_ID:
        raise ValueError('Change threads are only allowed for suburban threads, not for {}'.format(basic_thread.t_type))

    year_days = ThreadFactory.build_year_days(year_days)
    new_mask = RunMask(year_days)

    # Ordinal numbers should be uniq from a basic_thread and its related threads to create new ordinal number.
    ordinal_numbers = [basic_thread.ordinal_number]
    for thread in RThread.objects.filter(basic_thread=basic_thread).only('ordinal_number', 'year_days'):
        ordinal_numbers.append(thread.ordinal_number)

        if RunMask(thread.year_days) & new_mask:
            raise ValueError('New mask interferes with mask of thread: {} {}'.format(thread.id, thread.number))

    if schedule_v1:
        schedule = schedule_v1
    else:
        # add schedule with defined changes
        schedule = []
        changes = changes or {}
        for rts in basic_thread.path:
            st_changes = changes.get(rts.station, {})
            if st_changes.get('cancelled'):
                continue

            arr = rts.tz_arrival + st_changes.get('arrival', 0) if rts.tz_arrival is not None else None
            dep = rts.tz_departure + st_changes.get('departure', 0) if rts.tz_departure is not None else None

            schedule.append([arr, dep, rts.station])

    # copy fields from basic thread
    fields = set(f.name for f in RThread._meta.get_fields() if not isinstance(f, ForeignObjectRel))
    exclude_fileds = {'id', 'uid', 'import_uid', 'year_days', 'basic_thread'}
    default_kwargs = {
        field: getattr(basic_thread, field, None)
        for field in (fields - exclude_fileds)
    }
    for field in ['id', 'uid', 'import_uid', 'year_days', 'basic_thread']:
        default_kwargs.pop(field, None)

    # set changed fields
    default_kwargs.update({
        'year_days': year_days,
        'schedule_v1': schedule,
        'type': RThreadType.CHANGE_ID,
        'basic_thread': basic_thread,
        'ordinal_number': max(ordinal_numbers) + 1
    })
    default_kwargs.update(kwargs)

    # update basic_thread mask, because it shouldn't interfere with new thread mask
    basic_thread.year_days = RunMask(basic_thread.year_days) - new_mask
    basic_thread.save()

    return create_thread(**default_kwargs)
