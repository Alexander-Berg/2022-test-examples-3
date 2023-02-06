# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime, timedelta

import pytest
from hamcrest import assert_that, has_entries, contains

from common.apps.facility.factories import create_suburban_facility
from common.apps.facility.models import SuburbanThreadFacility
from common.models.factories import create_tariff_group, create_tariff_type, create_aeroex_tariff
from common.models.schedule import RTStation
from common.models.transport import TransportType
from common.tester.factories import create_station, create_thread, create_station_schedule, create_train_schedule_plan
from travel.rasp.library.python.common23.date.run_mask import RunMask

from common.tester.utils.datetime import replace_now
from travel.rasp.export.tests.v3.helpers import api_get_json


pytestmark = [pytest.mark.dbuser('module'), pytest.mark.mongouser('module')]


class TestThreadBase(object):
    def test_stops(self):
        station = create_station()
        thread = create_thread(
            __={'calculate_noderoute': True},
            schedule_v1=[[None, 0, station], [0, None]])
        rtstation = RTStation.objects.get(thread=thread, station=station)

        response = api_get_json('/v3/suburban/thread/{}'.format(thread.uid))
        assert response['stops'] is None

        schedule = create_station_schedule(
            station=rtstation.station,
            route=thread.route,
            thread=thread,
            rtstation=rtstation,
            stops_translations='')

        response = api_get_json('/v3/suburban/thread/{}'.format(thread.uid))
        assert response['stops'] is None

        schedule.stops_translations = json.dumps({'ru': 'ru_stops'})
        schedule.save()
        response = api_get_json('/v3/suburban/thread/{}'.format(thread.uid))
        assert response['stops'] == 'ru_stops'

    def test_train_schedule_plan(self):
        plan = create_train_schedule_plan(start_date=datetime.now() + timedelta(days=2))
        current_plan, next_plan = plan.get_current_and_next(datetime.now().date())

        thread = create_thread(schedule_plan=plan, translated_days_texts=u'[{}, {"ru": "ежедневно"}]')
        response = api_get_json('/v3/suburban/thread/{}'.format(thread.uid))
        assert response['days'] == u'ежедневно ' + next_plan.L_appendix()

    @replace_now(datetime(2001, 2, 2))
    def test_today(self):
        station = create_station()
        thread = create_thread(
            year_days=[datetime(2001, 2, 2)],
            uid="*uid*",
            __={'calculate_noderoute': True},
            schedule_v1=[[None, 0, station],
                         [0, None]])

        response = api_get_json('/v3/suburban/thread_on_date/{}'.format(thread.uid), {'date': 'today'})
        assert response['days'] == u'только 2 февраля'

        response = api_get_json('/v3/suburban/thread_on_date/{}'.format(thread.uid), {'date': 'tomorrow'},
                                response_status_code=404)
        assert response['error']['text'] == u'Рейс *uid* не ходит 2001-02-03'

        thread_2 = create_thread(
            year_days=[datetime(2001, 2, 3)],
            uid="*uid2*",
            __={'calculate_noderoute': True},
            schedule_v1=[[None, 0, station],
                         [0, None]])
        response = api_get_json('/v3/suburban/thread_on_date/{}'.format(thread_2.uid), {'date': 'tomorrow'})
        assert response['days'] == u'только 3 февраля'

    @replace_now(datetime(2019, 7, 2))
    def test_facilities(self):
        thread = create_thread(
            year_days=[datetime(2019, 7, 1), datetime(2019, 7, 2)],
            t_type=TransportType.SUBURBAN_ID
        )
        facility1 = create_suburban_facility(title_ru='Ынтырнет', code='internet')
        facility2 = create_suburban_facility(title_ru='Пыво', code='beer')
        thread_facilitiy = SuburbanThreadFacility.objects.create(
            thread=thread,
            year_days=str(RunMask(days=[datetime(2019, 7, 2)]))
        )
        thread_facilitiy.facilities.add(facility1)
        thread_facilitiy.facilities.add(facility2)

        response = api_get_json('/v3/suburban/thread_on_date/{}'.format(thread.uid), {'date': '2019-07-01'})
        assert not('facilities' in response)

        response = api_get_json('/v3/suburban/thread_on_date/{}'.format(thread.uid), {'date': '2019-07-02'})
        assert 'facilities' in response
        assert_that(response['facilities'], contains(
            has_entries({
                'title': 'Ынтырнет',
                'code': 'internet'
            }),
            has_entries({
                'title': 'Пыво',
                'code': 'beer'
            })
        ))

        thread2 = create_thread(year_days=[datetime(2019, 7, 1)], t_type=TransportType.SUBURBAN_ID)

        response = api_get_json('/v3/suburban/thread_on_date/{}'.format(thread2.uid), {'date': '2019-07-01'})
        assert not ('facilities' in response)

    def test_suburban_thread_tariffs(self):
        station_from = create_station(__={'codes': {'esr': '100'}})
        station_to = create_station(__={'codes': {'esr': '101'}})

        group = create_tariff_group()
        tariff_type = create_tariff_type(code='type1', category='usual', is_main=True, order=1,
                                         title='tariff1', __={'tariff_groups': [group]})
        create_aeroex_tariff(id=701, station_from=station_from, station_to=station_to, tariff=100, type=tariff_type)

        create_thread(
            uid='thread_uid',
            schedule_v1=[
                [None, 0, station_from],
                [50, None, station_to],
            ],
            tariff_type=tariff_type
        )

        response = api_get_json('/v3/suburban/thread/thread_uid/?station_from=100&station_to=101')

        assert 'tariffs' in response
        assert len(response['tariffs']) == 1
        assert_that(response['tariffs'][0], has_entries({
            'category': 'usual',
            'title': 'tariff1',
            'code': 'type1',
            'order': 1,
            'is_main': True,
            'price': has_entries({'currency': 'RUR', 'value': 100}),
            'id': 701,
        }))

        assert 'tariff' in response
        assert response['tariff']['value'] == 100
        assert response['tariff']['currency'] == 'RUR'
