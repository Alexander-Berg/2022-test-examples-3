# coding: utf-8
from __future__ import unicode_literals

import json

import freezegun
import mock
import pytest
from django.test import Client
from hamcrest import assert_that, contains_inanyorder, has_entries

from common.apps.facility.factories import create_suburban_facility
from common.apps.facility.models import SuburbanThreadFacility, SuburbanFacility
from common.models.transport import TransportType
from common.tester.factories import create_thread, create_station
from common.utils.date import RunMask


create_thread = create_thread.mutate(__={'calculate_noderoute': True})


@pytest.mark.dbuser
@mock.patch.object(SuburbanFacility._meta.get_field('icon').storage, 'base_url', 'https://static/')
@freezegun.freeze_time('2016-01-01')
def test_suburban_facilities():
    lezhanki_facility = create_suburban_facility(title_ru='Лежанки')
    spalniki_facility = create_suburban_facility(title_ru='Спальники')
    spalniki_facility.icon.name = 'some/file/path.svg'
    spalniki_facility.save()

    station_from = create_station()
    station_to = create_station()
    thread = create_thread(
        t_type=TransportType.SUBURBAN_ID,
        schedule_v1=[
            [None, 0, station_from],
            [10, None, station_to],
        ],
    )

    thread_facility = SuburbanThreadFacility.objects.create(year_days=RunMask.ALL_YEAR_DAYS, thread=thread)
    thread_facility.facilities.add(lezhanki_facility)
    thread_facility.facilities.add(spalniki_facility)

    response = Client().get('/{lang}/search/search/'.format(lang='ru'), {
        'pointFrom': station_from.point_key,
        'pointTo': station_to.point_key,
        'when': '2016-01-01',
        'transportType': 'suburban'
    })

    result = json.loads(response.content)

    assert_that(result['result']['segments'][0]['suburbanFacilities'], contains_inanyorder(
        has_entries(title='Лежанки', icon=''),
        has_entries(title='Спальники', icon='https://static/some/file/path.svg'),
    ))
