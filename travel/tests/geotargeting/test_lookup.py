# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import platform
import warnings

import mock
import pytest

from common.geotargeting import lookup
from common.geotargeting.lookup import get_city_by_ip
from common.tester.factories import create_settlement
from common.utils.geobase import get_geobase_lookup


@pytest.mark.dbuser
def test_get_city_by_ip():
    if platform.linux_distribution()[2] != 'trusty':
        pytest.skip('Tests for trusty only')

    settlement = create_settlement(_geo_id=2)
    geobase = get_geobase_lookup()
    orig_func = geobase.get_region_by_ip

    def get_region_by_ip(ip):
        region = orig_func(ip)
        if not region['id'] == 2:
            warnings.warn('You need to update geobase or test')
            region['id'] = 2
        return region

    with mock.patch.object(geobase, 'get_region_by_ip', get_region_by_ip), mock.patch.object(
        lookup, 'geobase', geobase
    ):
        # https://racktables.yandex-team.ru/index.php?andor=and&cft%5B%5D=14&cft%5B%5D=40&cfe=&page=ipv6space&tab=default
        # Этот ip взял из наших питерских сетей.
        assert (settlement, 2) == get_city_by_ip('2a02:6b8:0:2300::1')
