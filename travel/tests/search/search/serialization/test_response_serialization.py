# coding: utf8
from __future__ import unicode_literals

import pytest
from hamcrest import assert_that, has_entries

from common.tester.factories import create_station

from travel.rasp.morda_backend.morda_backend.search.search.serialization.response_serialization import (
    ResponseSearchPointSchema
)


@pytest.mark.dbuser
def test_response_search_point_schema():
    station = create_station(
        title_ru='Екатеринбург',
        title_ru_accusative='Екатеринбург (accusative)',
        title_ru_genitive='Екатеринбурга (genitive)',
        title_ru_locative='Екатеринбурге (locative)',
        title_ru_preposition_v_vo_na='в',
        popular_title_ru='Екатеринбург (popular)',
        short_title_ru='Екатеринбург (short)',
        time_zone='Asia/Yekaterinburg',
        slug='ekb'
    )
    result, errors = ResponseSearchPointSchema().dump(station)
    assert not errors
    assert_that(result, has_entries({
        'key': station.point_key,
        'title': 'Екатеринбург',
        'titleAccusative': 'Екатеринбург (accusative)',
        'titleGenitive': 'Екатеринбурга (genitive)',
        'titleLocative': 'Екатеринбурге (locative)',
        'preposition': 'в',
        'popularTitle': 'Екатеринбург (popular)',
        'shortTitle': 'Екатеринбург (short)',
        'slug': 'ekb'
    }))
