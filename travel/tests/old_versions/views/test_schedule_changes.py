# coding: utf8

import json
from datetime import date, datetime
from copy import deepcopy

import mock
import pytest
from django.test.client import Client

from common.models.factories import create_teaser
from common.models.geo import CodeSystem, ExternalDirection, ExternalDirectionMarker, StationCode
from common.utils.date import RunMask

from common.tester.factories import create_thread, create_station, create_station_code
from common.tester.testcase import TestCase
from common.tester.utils.datetime import replace_now

from travel.rasp.export.export.views.schedule_changes import (
    get_next_date, gen_threads_info, gen_thread_info, get_thread_changes, stops_diff, get_direction_threads,
    generate_direction_changes
)


@replace_now('2016-08-08 00:00:00')
def test_get_nexdate():
    assert get_next_date(0) == date(2016, 8, 15)
    for day in range(1, 8):
        assert get_next_date(day) == date(2016, 8, 8 + day)


class TestThreadsInfo(TestCase):
    @replace_now('2001-01-01 00:00:00')
    def test_gen_threads_info(self):
        rt1, rt2, rt3 = create_thread(id=11), create_thread(id=12), create_thread(id=5)
        rt4 = create_thread(id=13, basic_thread=rt3)
        today = date(2001, 1, 1)

        with mock.patch('travel.rasp.export.export.views.schedule_changes.gen_thread_info', autospec=True, return_value=None) as m_create_thread:
            threads_info = gen_threads_info({'direction': {'forward': {today: {'basic': [5], 'assignment': [11, 12]}}}})
            assert threads_info == {i: None for i in [5, 11, 12, 13]}
            assert len(m_create_thread.call_args_list) == 4
            assert {m_create_thread.call_args_list[i][0][0] for i in range(4)} == {rt1, rt2, rt3, rt4}
            assert m_create_thread.call_args_list[0][0][1] == today

    @replace_now('2001-01-01 00:00:00')
    def test_gen_thread_info(self):
        station_from, station_to = create_station(title='from'), create_station(title='to')
        code_system = CodeSystem.objects.get(code='esr')
        create_station_code(station=station_from, system=code_system, code='esr_from')
        create_station_code(station=station_to, system=code_system, code='esr_to')
        esr_codes = StationCode.StationCodeByStationIdGetter(code_system.id, [station_from.id, station_to.id])

        th1 = create_thread(uid='thread_uid', title='t_ru', number='thread_number',
                            schedule_v1=[[None, 10, station_from],
                                         [70, None, station_to]])
        today = date(2001, 1, 1)
        thread_info = gen_thread_info(th1, today, esr_codes)

        assert thread_info == {
            'uid': 'thread_uid',
            'title': 't_ru',
            'stations':
                [{'arrival': None,
                  'departure': '10',
                  'esr': 'esr_from',
                  'title': 'from'},
                 {'arrival': '70',
                  'departure': None,
                  'esr': 'esr_to',
                  'title': 'to'}],
            'number': 'thread_number',
            'arrival_to': '01:10',
            'departure_from': '00:10'
        }

    @replace_now('2001-01-01 00:00:00')
    def test_add_thread_changes(self):
        base_rt = create_thread()
        create_thread(id=112, basic_thread=base_rt, uid='cancel_uid', title='cancel_title',
                      type='cancel', year_days=RunMask.range(datetime(2001, 1, 3), datetime(2001, 1, 10)))
        create_thread(id=113, basic_thread=base_rt, uid='change_uid', title='change_title',
                      type='change', year_days=RunMask.range(datetime(2001, 1, 1), datetime(2001, 1, 5)))

        thread_changes = get_thread_changes(base_rt.id, date(2001, 1, 2))
        assert thread_changes == {
            'change_id': 113,
            'change_title': 'change_title',
            'change_type': 'change',
            'change_uid': 'change_uid'
        }

        thread_changes = get_thread_changes(base_rt.id, date(2001, 1, 7))
        assert thread_changes == {
            'change_id': 112,
            'change_title': 'cancel_title',
            'change_type': 'cancel',
            'change_uid': 'cancel_uid'
        }


def test_stops_diff():
    tr1_attrs = {'stations': [{'arrival': None,
                               'departure': '0',
                               'esr': 'esr_from',
                               'title': 'from'},
                              {'arrival': '10',
                               'departure': '10',
                               'esr': 'esr_change',
                               'title': 'changed_station'},
                              {'arrival': '30',
                               'departure': None,
                               'esr': 'esr_to',
                               'title': 'to'}]}

    tr2_attrs = deepcopy(tr1_attrs)
    tr2_attrs['stations'][1]['departure'] = '20'

    canceled, added = stops_diff(tr1_attrs, tr2_attrs)
    assert added == ['changed_station'] and canceled == []

    canceled, added = stops_diff(tr2_attrs, tr1_attrs)
    assert added == [] and canceled == ['changed_station']


class TestSchedule(TestCase):
    today = date(2001, 1, 1)
    threads_info = {
        1: {
            'uid': 'base_uid',
            'title': 'base_title',
            'stations': [],
            'number': 'base_number',
            'arrival_to': '00:20',
            'departure_from': '00:10'
        },
        2: {
            'uid': 'change_uid',
            'title': 'base_title',
            'stations': [],
            'number': 'base__number',
            'arrival_to': '00:30',
            'departure_from': '00:10'
        }
    }
    thread_changes = {
        'change_id': 2,
        'change_title': 'change_title',
        'change_type': 'cancel',
        'change_uid': 'change_uid'
    }

    @replace_now('2001-01-01 00:00:00')
    def test_get_directions(self):
        threads_info, thread_changes = deepcopy(self.threads_info), deepcopy(self.thread_changes)

        with mock.patch('travel.rasp.export.export.views.schedule_changes.get_thread_changes', autospec=True, return_value=thread_changes) as m_add_thread_changes:
            changes = generate_direction_changes(self.threads_info,  {'assignment': [], 'basic': [1]}, self.today)
            m_add_thread_changes.assert_called_once_with(1, self.today)
            assert changes == [{'base_arrival': '00:20',
                                 'base_departure': '00:10',
                                 'base_title': 'base_title',
                                 'color': 'red',
                                 'number': 'base_number',
                                 'subtext': u'Отменен',
                                 'type': 'cancel'}]

        thread_changes['change_type'] = 'change'
        with mock.patch('travel.rasp.export.export.views.schedule_changes.get_thread_changes', autospec=True, return_value=thread_changes):
            changes = generate_direction_changes(self.threads_info, {'assignment': [], 'basic': [1]}, self.today)
            assert changes == [{'base_arrival': '00:20',
                                 'base_departure': '00:10',
                                 'arrival': '00:30',
                                 'departure': '00:10',
                                 'base_title': 'base_title',
                                 'number': 'base_number',
                                 'subtext': u'Поезд проследует изменённым расписанием',
                                 'subtype': 'timetable',
                                 'type': 'change'}]

        threads_info[2]['title'] = 'change_title'
        with mock.patch('travel.rasp.export.export.views.schedule_changes.get_thread_changes', autospec=True, return_value=thread_changes):
            changes = generate_direction_changes(threads_info, {'assignment': [], 'basic': [1]}, self.today)
            assert changes == [{'base_arrival': '00:20',
                                 'base_departure': '00:10',
                                 'arrival': '00:30',
                                 'departure': '00:10',
                                 'base_title': 'base_title',
                                 'title': 'change_title',
                                 'number': 'base_number',
                                 'subtext': u'Поезд проследует изменённым маршрутом',
                                 'subtype': 'changeroute',
                                 'type': 'change'}]

        threads_info[1]['stations'] = [{}]
        with mock.patch('travel.rasp.export.export.views.schedule_changes.get_thread_changes', autospec=True, return_value=thread_changes):
            changes = generate_direction_changes(threads_info, {'assignment': [], 'basic': [1]}, self.today)
            assert changes == [{'base_arrival': '00:20',
                                 'base_departure': '00:10',
                                 'arrival': '00:30',
                                 'departure': '00:10',
                                 'base_title': 'base_title',
                                 'title': 'change_title',
                                 'number': 'base_number',
                                 'subtext': u'Поезд проследует укороченным маршрутом',
                                 'subtype': 'reduceroute',
                                 'type': 'change'}]

    def test_assignment_thread(self):
        changes = generate_direction_changes(self.threads_info, {'assignment': [1], 'basic': []}, self.today)
        assert changes == [{'arrival': '00:20',
                             'color': 'navy',
                             'departure': '00:10',
                             'number': 'base_number',
                             'subtext': u'Назначается',
                             'subtype': 'assignment',
                             'title': 'base_title',
                             'type': 'assignment'}]

    def test_get_directions_stations_text(self):
        thread_changes = deepcopy(self.thread_changes)
        thread_changes['change_type'] = 'change'

        with mock.patch('travel.rasp.export.export.views.schedule_changes.get_thread_changes', autospec=True, return_value=thread_changes), \
             mock.patch('travel.rasp.export.export.views.schedule_changes.stops_diff', autospec=True, return_value=[['cancel_1', 'cancel_2'], ['added_1', 'added_2']]):
            changes = generate_direction_changes(self.threads_info,  {'assignment': [], 'basic': [1]}, self.today)
            assert changes == [{'base_arrival': '00:20',
                                 'base_departure': '00:10',
                                 'arrival': '00:30',
                                 'departure': '00:10',
                                 'base_title': 'base_title',
                                 'number': 'base_number',
                                 'subtext': u'Поезд проследует изменённым расписанием',
                                 'stops_text': u'Отменены остановки: cancel_1, cancel_2. Назначены остановки: added_1, added_2.',
                                 'subtype': 'timetable',
                                 'type': 'change'}]


@pytest.mark.mongouser
@pytest.mark.dbuser
class TestScheduleChanges(object):

    @replace_now('2000-01-01 00:00:00')
    def test_schedule_changes(self):
        geo_id = 213

        def get_schedule(esr_code, direction, dates):
            return {day: {'basic': [], 'assignment': []} for day in dates}

        directions_dict = {
            213: {
                u'Горьковское направление':
                    ['191602', u'Москва-Курская', u'Горьковское направление', u'туда', 5,
                     '193735', u'Серп и Молот', u'на Москву', u'обратно', 7]
            }
        }

        direction = ExternalDirection.objects.create(full_title=u'Горьковское направление', title=u'Горьковское', id=10)
        ExternalDirectionMarker.objects.create(station=create_station(), external_direction=direction, order=0)
        create_teaser(content='teaser_content', is_active_export=True, external_directions=[direction])

        with mock.patch('travel.rasp.export.export.views.schedule_changes.get_direction_threads', autospec=True, side_effect=get_schedule), \
             mock.patch.dict('travel.rasp.export.export.views.schedule_changes.direction_stations_by_geoid', directions_dict):

            response = Client().get('/export/v2/suburban/schedule_changes/', {'geo_id': geo_id,
                                                                              'start_date': '2016-08-31',
                                                                              'end_date': '2016-09-01'})
            assert response.status_code == 200
            result = json.loads(response.content)

            assert result['geo_id'] == 213
            assert result['name'] == u'Москва'
            assert len(result.get('directions')) == 1

            for propagation in [u'backward', u'forward']:
                assert set(obj[u'date'] for obj in result['directions'][0][propagation]) == {u'2016-08-31', u'2016-09-01'}
            assert result['directions'][0][u'id'] == 10
            assert result['directions'][0][u'name'] == u'Горьковское направление'
            assert result['directions'][0][u'texts'] == [u'teaser_content']


def test_get_direction_threads():
    today = date(2001, 1, 1)
    next_day = date(2001, 1, 2)
    schedule = {
        1: {'id':  1,
            'uid': 'uid1',
            'type': 'basic',
            'direction': 'dir',
            'except': '01.01'},
        2: {'id': 2,
            'uid': 'uid2',
            'type': 'assignment',
            'direction': 'dir',
            'days': [today]}
    }
    with mock.patch('travel.rasp.export.export.views.schedule_changes.get_schedule', autospec=True, return_value=schedule):
        assert get_direction_threads('esr_code', 'dir', [today]) == {today: {'basic': [1], 'assignment': [2]}}
        assert get_direction_threads('esr_code', 'dir', [next_day]) == {next_day: {}}
