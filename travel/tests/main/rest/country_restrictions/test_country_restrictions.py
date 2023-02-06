# coding=utf-8
from __future__ import absolute_import

from collections import OrderedDict

import ujson
import mock

from travel.avia.backend.main.rest.country_restrictions.country_restrictions import (
    country_restrictions_view,
    countries_restrictions_view,
)


def test_empty_response_for_russia():
    result = country_restrictions_view._unsafe_process({
        'country_id': 225,
    })

    result = ujson.loads(result.response[0])
    assert result[u'status'] == u'ok'
    data = result[u'data']
    assert data is None


def test_completed_response_for_country():
    country_id = 123456789
    restrictions = {
        'isClosed': True,
    }
    with mock.patch(
            'travel.avia.backend.main.lib.covid_restrictions._cache',
            return_value=({}, {country_id: restrictions})
    ):
        result = country_restrictions_view._unsafe_process({
            'country_id': country_id,
        })

        result = ujson.loads(result.response[0])
        assert result[u'status'] == u'ok'
        data = result[u'data']
        assert data == {'countryId': country_id, 'isClosed': True}


def test_completed_response_for_countries():
    fake_restrictions = OrderedDict((
        (123456789, {'isClosed': True}),
        (987654321, {'isClosed': False}),
    ))
    with mock.patch(
            'travel.avia.backend.main.lib.covid_restrictions._cache',
            return_value=({}, fake_restrictions)
    ):
        result = countries_restrictions_view._unsafe_process({})

        result = ujson.loads(result.response[0])
        assert result[u'status'] == u'ok'
        data = result[u'data']
        assert isinstance(data, list)
        assert len(data) == len(fake_restrictions)
        assert data == [
            {'countryId': 123456789, 'isClosed': True},
            {'countryId': 987654321, 'isClosed': False}
        ]
