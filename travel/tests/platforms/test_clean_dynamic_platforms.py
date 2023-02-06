# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from hamcrest import assert_that, has_entries, only_contains

from common.data_api.platforms.client import get_dynamic_platform_collection
from common.tester.utils.datetime import replace_now
from travel.rasp.tasks.platforms.clean_dynamic_platforms import run


@pytest.mark.dbuser
@pytest.mark.mongouser
@replace_now('2019-02-10')
def test_run():
    """
    Presumes that the default timespan of dynamic platforms is 5 days
    """
    coll = get_dynamic_platform_collection()
    coll.insert_many([
        {
            'date': '2019-02-04',
            'station_id': 123,
            'train_number': '4321',
            'departure_platform': '14'
        },
        {
            'date': '2019-02-04',
            'station_id': 223,
            'train_number': '4321',
            'departure_platform': '24'
        },
        {
            'date': '2019-02-05',
            'station_id': 123,
            'train_number': '4321',
            'departure_platform': '15'
        },
        {
            'date': '2019-02-06',
            'station_id': 123,
            'train_number': '4321',
            'departure_platform': '16'
        },
    ])

    run()

    docs = [doc for doc in coll.find({})]
    assert_that(docs, only_contains(
        has_entries({
            'date': '2019-02-05',
            'departure_platform': '15',
        }),
        has_entries({
            'date': '2019-02-06',
            'departure_platform': '16',
        }),
    ))
