# -*- coding: utf-8 -*-

import pytest

from stationschedule.tester.factories import create_ztablo


@pytest.mark.dbuser
def test_merged_number():
    assert create_ztablo().merged_number is None
    assert create_ztablo({
        'number': 'number',
        'merged_flight': {'number': 'merged_number'}
    }).merged_number == 'number/merged_number'
