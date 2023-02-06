# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import json
import mock
import pytest
from django.conf import settings
from hamcrest import assert_that, contains_inanyorder, has_entries, has_item

from common.data_api.platforms.client import get_dynamic_platform_collection
from common.models.geo import CodeSystem
from common.models.schedule import PlatformRepresentation
from common.tester.factories import create_station, create_station_code
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.tasks.platforms import update_dynamic_platforms
from travel.rasp.tasks.platforms.update_dynamic_platforms import run, send_unparsed_track_numbers

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


@replace_now('2019-02-01')
def test_run():
    stations = [create_station(id=120), create_station(id=121), create_station(id=122)]
    dzv_code_system = CodeSystem.objects.get(code='dzv')
    create_station_code(station=stations[0], system=dzv_code_system, code='20')
    create_station_code(station=stations[1], system=dzv_code_system, code='21')
    create_station_code(station=stations[2], system=dzv_code_system, code='_22')  # w/o updating
    for s in stations:
        PlatformRepresentation.objects.create(station=s, reg_exp=r'^(\d+)$', representation='{}')
    dzv_test_data = {
        20: {
            'arrival': [
                {
                    'train_number': '1',
                    'start_station': 'Станция0',
                    'end_station': 'Станция1',
                    'track_number': '9',
                    'event_dt': datetime(2019, 2, 1, 0, 6, 15),
                    'train_type': 0
                }
            ],
            'departure': [
                {
                    'train_number': '1',
                    'start_station': 'Станция0',
                    'end_station': 'Станция1',
                    'track_number': '10',
                    'event_dt': datetime(2019, 2, 1, 0, 6, 15),
                    'train_type': 0
                },
                {
                    'train_number': '2',
                    'start_station': 'Станция0',
                    'end_station': 'Станция1',
                    'track_number': '0',  # number is not for import
                    'event_dt': datetime(2019, 2, 1, 0, 6, 15),
                    'train_type': 0
                }
            ]
        },
        21: {
            'arrival': [],
            'departure': [
                {
                    'train_number': '10',
                    'start_station': 'Станция0',
                    'end_station': 'Станция1',
                    'track_number': '',  # empty tracks should be missed correctly
                    'event_dt': datetime(2019, 2, 1, 0, 5, 15),
                    'train_type': 0
                },
                {
                    'train_number': '1',
                    'start_station': 'Станция0',
                    'end_station': 'Станция1',
                    'track_number': '11',
                    'event_dt': datetime(2019, 2, 1, 0, 6, 15),
                    'train_type': 0
                }
            ]
        },
    }

    def test_dzv_get_platforms(term_id):
        return dzv_test_data[term_id]

    with mock.patch.object(update_dynamic_platforms, 'dzv', autospec=True) as m_dzv, \
         mock.patch.object(update_dynamic_platforms, 'send_unparsed_track_numbers', autospec=True) as m_sutn:
        m_dzv.get_platforms = mock.MagicMock(side_effect=test_dzv_get_platforms)
        run()

    m_sutn.assert_not_called()
    coll = get_dynamic_platform_collection()

    indexes = [idx for idx in coll.list_indexes()]
    assert_that(indexes, has_item(
        has_entries({'key': has_entries({'date': 1, 'station_id': 1, 'train_number': 1})})
    ))

    docs = [doc for doc in coll.find({})]
    assert_that(docs, contains_inanyorder(
        has_entries({
            'station_id': 120L,
            'train_number': '1',
            'date': '2019-02-01',
            'arrival_platform': '9',
            'departure_platform': '10',
        }),
        has_entries({
            'station_id': 121L,
            'train_number': '1',
            'date': '2019-02-01',
            'departure_platform': '11',
        }),
    ))


@replace_now('2019-02-01')
def test_run_fault():
    stations = [
        create_station(id=120, title='Станция0'),
        create_station(id=121, title='Станция1'),
        create_station(id=122, title='Станция2')
    ]
    dzv_code_system = CodeSystem.objects.get(code='dzv')
    create_station_code(station=stations[0], system=dzv_code_system, code='20')  # w/o regexp
    create_station_code(station=stations[1], system=dzv_code_system, code='21')
    create_station_code(station=stations[2], system=dzv_code_system, code='22')  # w/o regexp
    PlatformRepresentation.objects.create(station=stations[1], reg_exp=r'^(\d+)$', representation='{}')
    dzv_test_data = {
        20: {
            'arrival': [],
            'departure': [
                {
                    'train_number': '1',
                    'start_station': 'Станция0',
                    'end_station': 'Станция1',
                    'track_number': '10',
                    'event_dt': datetime(2019, 2, 1, 0, 6, 15),
                    'train_type': 0
                }
            ]
        },
        21: {
            'arrival': [],
            'departure': [
                {
                    'train_number': '1',
                    'start_station': 'Станция0',
                    'end_station': 'Станция1',
                    'track_number': '11',
                    'event_dt': datetime(2019, 2, 1, 0, 6, 15),
                    'train_type': 0
                }
            ]
        },
        22: {
            'arrival': [
                {
                    'train_number': '1',
                    'start_station': 'Станция0',
                    'end_station': 'Станция1',
                    'track_number': '12',
                    'event_dt': datetime(2019, 2, 1, 0, 6, 15),
                    'train_type': 0
                }
            ],
            'departure': []
        },
    }

    def test_dzv_get_platforms(term_id):
        return dzv_test_data[term_id]

    with mock.patch.object(update_dynamic_platforms, 'dzv', autospec=True) as m_dzv, \
         mock.patch.object(update_dynamic_platforms, 'send_unparsed_track_numbers', autospec=True) as m_sutn:
        m_dzv.get_platforms = mock.MagicMock(side_effect=test_dzv_get_platforms)
        run()

    m_sutn.assert_called_once()

    coll = get_dynamic_platform_collection()
    docs = [doc for doc in coll.find({})]
    assert_that(docs, contains_inanyorder(
        has_entries({
            'station_id': 121L,
            'train_number': '1',
            'date': '2019-02-01',
            'departure_platform': '11',
        }),
    ))


def test_send_unparsed_track_numbers(httpretty):
    host = 'test_host'
    accounts = ['test_account_{}'.format(i) for i in range(2)]

    campaign = settings.RASP_DYNAMIC_PLATFORMS_ERROR_CAMPAIGN
    httpretty.register_uri(
        httpretty.POST,
        'https://{}/api/0/{}/transactional/{}/send'.format(host, accounts[0], campaign),
        body='{"result": {"status": "OK"}}'
    )
    stations = [create_station(id=120 + i, title='Станция{}'.format(i)) for i in range(10)]

    with replace_setting('YASENDR_HOST', host), replace_setting('YASENDR_ACCOUNT', accounts[0]):
        send_unparsed_track_numbers([(stations[i], str(i)) for i in range(len(stations))])

    request = httpretty.last_request
    assert request.parsed_body['args'][0] == json.dumps({
        'errors': [
            {
                'station_id': stations[i].id,
                'station_title': stations[i].title,
                'text': str(i)
            } for i in range(len(stations))
        ]
    })
    assert request.querystring['to_email'][0] == settings.RASP_DYNAMIC_PLATFORMS_ERROR_EMAIL

    httpretty.register_uri(
        httpretty.POST,
        'https://{}/api/0/{}/transactional/{}/send'.format(host, accounts[1], campaign),
        body='{"result": {"status": "ERROR"}}'
    )
    with replace_setting('YASENDR_HOST', host), replace_setting('YASENDR_ACCOUNT', accounts[1]), \
         mock.patch.object(update_dynamic_platforms, 'log', autospec=True) as m_log:
        m_log.exception = mock.MagicMock()
        send_unparsed_track_numbers([(stations[i], str(i)) for i in range(len(stations))])

    m_log.exception.assert_called_once_with("Can't send a letter to {}: {}".format(
        settings.RASP_DYNAMIC_PLATFORMS_ERROR_EMAIL,
        json.dumps([
            {
                'station_id': stations[i].id,
                'station_title': stations[i].title,
                'text': str(i)
            } for i in range(len(stations))
        ])
    ))
