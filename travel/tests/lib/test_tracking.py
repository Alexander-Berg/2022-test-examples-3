# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from travel.rasp.wizards.proxy_api.lib.tracking import TrackingParameter, add_tracking_parameter, get_tracking_query_item


def test_get_tracking_query_item():
    assert get_tracking_query_item(TrackingParameter.DIRECTION) == (b'from', b'wrasppp')


def test_add_tracking_parameter():
    assert add_tracking_parameter(
        'http://foo/bar', TrackingParameter.DIRECTION
    ) == b'http://foo/bar?from=wrasppp'
    assert add_tracking_parameter(
        'http://foo/bar?baz=1', TrackingParameter.DIRECTION
    ) == b'http://foo/bar?baz=1&from=wrasppp'
