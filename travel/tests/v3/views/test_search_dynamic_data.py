# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime, timedelta

import pytest
from hamcrest import assert_that, has_entries, contains_inanyorder, contains

from common.apps.info_center.models import Info
from common.apps.suburban_events.factories import ThreadStationStateFactory
from common.models.geo import Settlement
from common.models.factories import create_external_direction, create_info

from travel.rasp.export.export.v3.core.errors import UserUseError
from travel.rasp.export.tests.v3.factories import create_station, create_thread
from travel.rasp.export.tests.v3.helpers import api_get_json
from travel.rasp.export.export.v3.views.search_dynamic_data import SearchDynamicDataSchema

pytestmark = [pytest.mark.dbuser('module'), pytest.mark.mongouser('module')]


class TestSearchDynamicData(object):
    def test_search_dynamic_data(self):
        direction = create_external_direction()
        stations = [
            create_station(id=121, __={'ext_directions': [direction]}),
            create_station(id=122, __={'ext_directions': [direction]}),
            create_station(id=123)
        ]
        msk = Settlement.objects.get(id=Settlement.MOSCOW_ID)

        start_dt_1 = datetime(2017, 12, 20, 12)
        start_dt_2 = datetime(2017, 12, 20, 16)

        thread_1 = create_thread(
            uid=u'uid_1',
            number=u'500',
            tz_start_time=start_dt_1.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [10, 15, stations[1]],
                [50, None, stations[2]],
            ],
        )
        th_1_rtstations = thread_1.path

        thread_2 = create_thread(
            uid=u'uid_2',
            number=u'600',
            tz_start_time=start_dt_2.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [15, 25, stations[1]],
                [40, None, stations[2]],
            ],
        )
        th_2_rtstations = thread_2.path

        ThreadStationStateFactory.create_from_rtstation(
            th_1_rtstations[0], start_dt_1,
            departure={
                'dt': start_dt_1 + timedelta(minutes=10),
                'minutes_from': 42,
                'minutes_to': 43,
            }
        )

        ThreadStationStateFactory.create_from_rtstation(
            th_1_rtstations[1], start_dt_1,
            arrival={
                'type': u'possible_delay',
                'minutes_from': 0,
                'minutes_to': 5
            },
            departure={'type': u'possible_delay'}
        )

        ThreadStationStateFactory.create_from_rtstation(
            th_2_rtstations[2], start_dt_2 + timedelta(minutes=40),
            arrival={
                'type': u'possible_delay',
                'minutes_from': None,
                'minutes_to': None
            }
        )

        ThreadStationStateFactory.create_from_rtstation(
            th_2_rtstations[1], start_dt_2,
            departure={
                'type': u'possible_delay',
                'minutes_from': 3,
                'minutes_to': 4
            }
        )

        create_info(
            services=[Info.Service.MOBILE_APPS],
            settlements=[msk],
            text='t1',
            text_short='short_t1',
            title='title_t1',
        )
        create_info(
            services=[Info.Service.MOBILE_APPS],
            stations=[stations[1]],
            text='t2',
            text_short='short_t2',
            title='title_t2',
            info_type=Info.Type.AHTUNG
        )
        create_info(
            services=[Info.Service.MOBILE_APPS],
            external_directions=[direction],
            text='t3',
            text_short='short_t3',
            title='title_t3',
            info_type=Info.Type.SPECIAL
        )

        params = {
            'segments_keys': json.dumps([
                {
                    'departure': '500__121___2017-12-20T12:00:00___121___None___0___None___None',
                    'arrival': '500__121___2017-12-20T12:00:00___122___10___15___None___None'
                },
                {
                    'departure': '600__121___2017-12-20T16:00:00___121___None___0___None___None',
                    'arrival': '600__121___2017-12-20T16:40:00___123___40___None___None___None'
                },
                {
                    'departure': '600__121___2017-12-20T16:00:00___122___15___25___None___None'
                }
            ]),
            'search_context': json.dumps({
                'point_from': 'c213',
                'point_to': 's122',
                'segments': [{
                    'station_from': 121,
                    'station_to': 122
                }]
            })
        }

        response = api_get_json('/v3/suburban/search_dynamic_data/', params, method='post')

        assert_that(response,  has_entries({
            'segments_states': {
                'departure': {
                    '500__121___2017-12-20T12:00:00___121___None___0___None___None': {
                        'fact_time': '2017-12-20T12:10:00+03:00',
                        'key': u'500__121___2017-12-20T12:00:00___121___None___0___None___None',
                        'type': u'fact',
                        'minutes_from': 42,
                        'minutes_to': 43,
                    },
                    '600__121___2017-12-20T16:00:00___121___None___0___None___None': {
                        'key': '600__121___2017-12-20T16:00:00___121___None___0___None___None',
                        'type': 'undefined'
                    },
                    '600__121___2017-12-20T16:00:00___122___15___25___None___None': {
                        'type': 'possible_delay',
                        'key': '600__121___2017-12-20T16:00:00___122___15___25___None___None',
                        'minutes_from': 3,
                        'minutes_to': 4
                    }
                },
                'arrival': {
                    '500__121___2017-12-20T12:00:00___122___10___15___None___None': {
                        'key': u'500__121___2017-12-20T12:00:00___122___10___15___None___None',
                        'minutes_from': 0,
                        'minutes_to': 5,
                        'type': u'possible_delay'
                    },
                    '600__121___2017-12-20T16:40:00___123___40___None___None___None': {
                        'key': u'600__121___2017-12-20T16:40:00___123___40___None___None___None',
                        'minutes_from': None,
                        'minutes_to': None,
                        'type': u'possible_delay'
                    }
                },
                'departure_state': {
                    '500__121___2017-12-20T12:00:00___122___10___15___None___None': {
                        'type': 'possible_delay',
                        'key': '500__121___2017-12-20T12:00:00___122___10___15___None___None',
                        'minutes_from': 7,
                        'minutes_to': 7
                    }
                },
                'arrival_state': {
                    '600__121___2017-12-20T16:00:00___122___15___25___None___None': {
                        'minutes_to': 1,
                        'type': 'fact',
                        'key': '600__121___2017-12-20T16:00:00___122___15___25___None___None',
                        'minutes_from': 1,
                        'fact_time': '2017-03-20T17:11:00+03:00'
                    }
                }
            },
            'teasers': contains_inanyorder(
                has_entries({
                    'content': 't1',
                    'mobile_content': 'short_t1',
                    'selected': True,
                    'title': 'title_t1',

                }),
                has_entries({
                    'content': 't2',
                    'mobile_content': 'short_t2',
                    'selected': True,
                    'title': 'title_t2',
                }),
                has_entries({
                    u'content': u't3',
                    u'mobile_content': u'short_t3',
                    u'selected': True,
                    u'title': u'title_t3'
                }),
            )
        }))

        response = api_get_json(
            '/v3/suburban/search_dynamic_data/', {'segments_keys': params['segments_keys']}, method='post')
        assert response.keys() == ['segments_states']

        response = api_get_json(
            '/v3/suburban/search_dynamic_data/', {'search_context': params['search_context']}, method='post')
        assert response.keys() == ['teasers']

        response = api_get_json('/v3/suburban/search_dynamic_data/', {}, method='post')
        assert response == {}

        params = {'search_context': json.dumps({'point_from': 'c213'})}
        response = api_get_json('/v3/suburban/search_dynamic_data/', params, method='post')
        assert_that(response['teasers'], contains(
            has_entries({
                'content': 't1',
                'mobile_content': 'short_t1'
            })
        ))

        params = {'search_context': json.dumps({'point_to': 's122'})}
        response = api_get_json('/v3/suburban/search_dynamic_data/', params, method='post')
        assert_that(response['teasers'], contains(
            has_entries({
                'content': 't2',
                'mobile_content': 'short_t2'
            })
        ))

        params = {'search_context': json.dumps({
            'segments': [{
                'station_from': 121,
                'station_to': 122
            }]
        })}
        response = api_get_json('/v3/suburban/search_dynamic_data/', params, method='post')
        assert_that(response['teasers'], contains(
            has_entries({
                'content': 't3',
                'mobile_content': 'short_t3'
            })
        ))


class TestSearchDynamicDataSchema(object):
    def test_search_dynamic_data_schema(self):
        station_from_1 = create_station(id=421)
        station_from_2 = create_station(id=422)
        point_to = create_station(id=500)
        point_from = Settlement.objects.get(id=Settlement.MOSCOW_ID)
        key_1 = '6676__2000002___2018-02-08T00:10:00___9600681___30___31___None___None'
        key_2 = '6676__2000002___2018-02-08T00:10:00___2000002___None___0___None___None'

        segments_keys = [{'arrival': key_1, 'departure': key_2}]
        search_context = {
            'segments': [
                {'station_from': station_from_1.id, 'station_to': point_to.id},
                {'station_from': station_from_2.id, 'station_to': point_to.id},
                {'station_from': station_from_1.id, 'station_to': point_to.id},
            ],
            'point_from': point_from.point_key,
            'point_to': point_to.point_key
        }
        params = {
            'segments_keys': json.dumps(segments_keys),
            'search_context': json.dumps(search_context)
        }
        query, errors = SearchDynamicDataSchema().load(params)
        assert query['segments_keys'] == [{
            'arrival': key_1,
            'departure': key_2,
            'thread_key': None
        }]
        assert_that(query['search_context'], has_entries({
            'point_from': point_from,
            'point_to': point_to,
            'segments': contains_inanyorder(
                has_entries({
                    'station_from': station_from_1,
                    'station_to': point_to,
                }),
                has_entries({
                    'station_from': station_from_2,
                    'station_to': point_to,
                }))
        }))

        params = {'search_context': json.dumps({'segments': [{'station_from': station_from_1.point_key}]})}
        with pytest.raises(UserUseError):
            SearchDynamicDataSchema().load(params)

        params = {'search_context': json.dumps(
            {'segments': [{'station_to': 5421, 'station_from': station_from_1.point_key}]})
        }
        with pytest.raises(UserUseError):
            SearchDynamicDataSchema().load(params)

        params = {'search_context': json.dumps({'point_from': '213', 'point_to': point_to.point_key})}
        with pytest.raises(UserUseError):
            SearchDynamicDataSchema().load(params)
