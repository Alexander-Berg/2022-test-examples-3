# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from hamcrest import assert_that, contains_inanyorder, has_entries, has_properties

from common.models.geo import Station
from common.tester.factories import create_settlement, create_station
from travel.rasp.trains.scripts.generate_crosslinks.generate_crosslinks import Runner, RouteItem


pytestmark = [pytest.mark.dbuser]


def test_crosslinks():
    from_c_1 = create_settlement(slug='from_c_1', latitude=52.607, longitude=38.491, id=251)
    from_c_2 = create_settlement(slug='from_c_2', latitude=52.707, longitude=38.591, id=252)
    from_c_3 = create_settlement(slug='from_c_3', latitude=52.999, longitude=38.666, id=253)
    to_c_1 = create_settlement(slug='to_c_1', latitude=52.718, longitude=38.491, id=261)
    to_c_2 = create_settlement(slug='to_c_2', latitude=52.818, longitude=38.491, id=262)
    to_c_3 = create_settlement(slug='to_c_3', latitude=52.919, longitude=38.491, id=263)
    to_c_4 = create_settlement(slug='to_c_4', id=264, latitude=None, longitude=None)  # check errors
    adler = create_station(id=Station.ADLER_ID, slug='ad_slug', latitude=52.609, longitude=38.491)

    sitemap = [
        ('from_c_1', 'to_c_1'),
        ('from_c_1', 'to_c_2'),
        ('from_c_1', 'ad_slug'),
        ('from_c_1', 'to_c_3'),

        ('from_c_2', 'to_c_2'),
        ('from_c_2', 'to_c_3'),

        ('from_c_3', 'to_c_1'),
        ('from_c_3', 'to_c_4'),
    ]

    with mock.patch.object(Runner, 'get_sitemap', autospec=True, return_value=sitemap):
        runner = Runner(
            '', '',
            links_count=6, unviewed_links_count=2,
            proximity_relevance=1, distance_additive=0.1,
            stat={}
        )
        result = runner.generate_from_sitemap()

        assert_that(result, has_entries({
            RouteItem(from_c_1, to_c_1, 0): contains_inanyorder(
                has_properties({'route': has_properties({'from_key': 'c251', 'to_key': 'c262'})}),
                has_properties({'route': has_properties({'from_key': 'c251', 'to_key': 'c263'})}),
                has_properties({'route': has_properties({'from_key': 'c251', 'to_key': 's{}'.format(Station.ADLER_ID)})}),
            ),
            RouteItem(from_c_1, to_c_2, 0): contains_inanyorder(
                has_properties({'route': has_properties({'from_key': 'c251', 'to_key': 'c261'})}),
                has_properties({'route': has_properties({'from_key': 'c251', 'to_key': 'c263'})}),
                has_properties({'route': has_properties({'from_key': 'c251', 'to_key': 's{}'.format(Station.ADLER_ID)})}),
            ),
            RouteItem(from_c_1, to_c_3, 0): contains_inanyorder(
                has_properties({'route': has_properties({'from_key': 'c251', 'to_key': 'c261'})}),
                has_properties({'route': has_properties({'from_key': 'c251', 'to_key': 'c262'})}),
                has_properties({'route': has_properties({'from_key': 'c251', 'to_key': 's{}'.format(Station.ADLER_ID)})}),
            ),
            RouteItem(from_c_1, adler, 0): contains_inanyorder(
                has_properties({'route': has_properties({'from_key': 'c251', 'to_key': 'c261'})}),
                has_properties({'route': has_properties({'from_key': 'c251', 'to_key': 'c262'})}),
                has_properties({'route': has_properties({'from_key': 'c251', 'to_key': 'c263'})}),
            ),

            RouteItem(from_c_2, to_c_2, 0): contains_inanyorder(
                has_properties({'route': has_properties({'from_key': 'c252', 'to_key': 'c263'})}),
            ),
            RouteItem(from_c_2, to_c_3, 0): contains_inanyorder(
                has_properties({'route': has_properties({'from_key': 'c252', 'to_key': 'c262'})}),
            ),

            RouteItem(from_c_3, to_c_1, 0): [],
            RouteItem(from_c_3, to_c_4, 0): []
        }))

        assert len(runner.errors) == 2
