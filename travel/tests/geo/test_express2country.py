# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.db import connection
from django.test.utils import CaptureQueriesContext
from django.utils import six

from travel.rasp.library.python.common23.models.core.geo.express2country import Express2Country
from travel.rasp.library.python.common23.models.core.geo.country import Country


@pytest.mark.dbuser
def test_to_str():
    assert six.text_type(
        Express2Country(code_re=r'20\d+', country_id=Country.RUSSIA_ID, time_zone='Asia/Yekaterinburg')
    )
    assert six.text_type(
        Express2Country(code_re=r'20\d+', country_id=Country.RUSSIA_ID)
    )


@pytest.mark.dbuser
def test_russia_tz():
    e2c = Express2Country.objects.create(code_re=r'20\d+', country_id=Country.RUSSIA_ID)

    assert Express2Country.get('2000001') == e2c
    assert Express2Country.get_tz('2000001') == 'Europe/Moscow'
    assert Express2Country.get_country('2000001').id == Country.RUSSIA_ID


@pytest.mark.dbuser
def test_override_timezone():
    Express2Country.objects.create(code_re=r'20\d+', country_id=Country.RUSSIA_ID, time_zone='Asia/Yekaterinburg')

    assert Express2Country.get_tz('2000001') == 'Asia/Yekaterinburg'


def test_empty_express_code():
    assert Express2Country.get('') is None
    assert Express2Country.get_tz('') is None
    assert Express2Country.get(None) is None
    assert Express2Country.get_tz(None) is None


@pytest.mark.dbuser
def test_unknown_express_code():
    Express2Country(code_re=r'000\d+', country_id=Country.RUSSIA_ID)

    assert Express2Country.get('200') is None
    assert Express2Country.get_tz('200') is None
    assert Express2Country.get_country('2000001') is None


@pytest.mark.dbuser
def test_russia_tz_with_precache():
    e2c = Express2Country.objects.create(code_re=r'20\d+', country_id=Country.RUSSIA_ID)

    with Express2Country.using_precache():
        assert Express2Country.get('2000001') == e2c
        assert Express2Country.get_tz('2000001') == 'Europe/Moscow'
        assert Express2Country.get_country('2000001').id == Country.RUSSIA_ID


@pytest.mark.dbuser
def test_express2country_precache():
    e2c = Express2Country.objects.create(code_re=r'20\d*', country_id=Country.RUSSIA_ID)

    with Express2Country.using_precache(), CaptureQueriesContext(connection) as queries:
        assert Express2Country.get('2000001') == e2c
        assert Express2Country.get('20') == e2c

        assert len(queries) == 0

        with Express2Country.using_precache():
            pass

        assert len(queries) == 0

    with CaptureQueriesContext(connection) as queries:
        assert Express2Country.get('2000001') == e2c
        assert Express2Country.get('20') == e2c

        assert len(queries) == 2
