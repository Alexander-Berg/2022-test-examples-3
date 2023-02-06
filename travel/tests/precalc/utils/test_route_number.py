# -*- coding: utf-8 -*-

import pytest
from django.db import connection
from django.test.utils import CaptureQueriesContext

from common.models.schedule import Company
from precalc.utils.route_number import get_airlines


@pytest.mark.dbuser
def test_get_airlines():
    c1 = Company.objects.create(iata=u'AAA')
    c2 = Company.objects.create(sirena_id=u'AAA')

    assert {c1, c2} == set(get_airlines(u'AAA'))

    with Company.objects.using_precache(), CaptureQueriesContext(connection) as captured_queries:
        assert {c1, c2} == set(get_airlines(u'AAA'))
        assert len(captured_queries) == 0
