# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date

import pytest

from common.tester.factories import create_settlement
from common.tester.utils.datetime import replace_now
from travel.rasp.wizards.proxy_api.lib.general.serialization import load_general_query
from travel.rasp.wizards.proxy_api.lib.general.models import GeneralQuery
from travel.rasp.wizards.wizard_lib.experiment_flags import ExperimentFlag


@pytest.mark.dbuser
@replace_now('2000-01-01')
def test_load_general_query():
    departure_settlement = create_settlement()

    assert load_general_query(
        departure_settlement,
        frozenset([ExperimentFlag.EXPERIMENTAL_SEARCH]),
        {'date': '2000-01-01', 'lang': 'uk', 'tld': 'com', 'transport': 'some_transport_code'}
    ) == GeneralQuery(
        departure_settlement=departure_settlement,
        transport_code='some_transport_code',
        departure_date=date(2000, 1, 1),
        language='uk',
        experiment_flags=frozenset([ExperimentFlag.EXPERIMENTAL_SEARCH]),
        intent=None,
        tld='com'
    )
