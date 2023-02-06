# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from common.tester.factories import create_settlement

from travel.rasp.morda_backend.morda_backend.search.search.data_layer.rasp_db_search import (
    RaspDbAllDaysSearch, RaspDbNearestSearch, RaspDbOneDaySearch
)
from travel.rasp.morda_backend.morda_backend.search.search.data_layer.baris_search import (
    BarisAllDaysSearch, BarisNearestSearch, BarisOneDaySearch
)
from travel.rasp.morda_backend.morda_backend.search.search.data_layer.all_types_search import (
    AllTypesNearestSearch, AllTypesAllDaysSearch, AllTypesOneDaySearch
)
from travel.rasp.morda_backend.morda_backend.search.search.views import _choose_searcher
from travel.rasp.morda_backend.morda_backend.search.search.serialization.request_serialization import ContextQuerySchema


pytestmark = pytest.mark.dbuser


def _check_searcher(params_dict, searcher_type):
    params_dict['point_from'] = 'c54'
    params_dict['point_to'] = 'c55'
    context, errors = ContextQuerySchema().load(params_dict)
    searcher = _choose_searcher(context)
    assert isinstance(searcher, searcher_type)


def test_choose_searcher():
    create_settlement(id=54)
    create_settlement(id=55)

    _check_searcher({}, AllTypesAllDaysSearch)
    _check_searcher({'nearest': True}, AllTypesNearestSearch)
    _check_searcher({'when': '2020-01-01'}, AllTypesOneDaySearch)
    _check_searcher({'when': 'tomorrow'}, AllTypesOneDaySearch)

    _check_searcher({'transportType': 'plane'}, BarisAllDaysSearch)
    _check_searcher({'transportType': 'plane', 'nearest': True}, BarisNearestSearch)
    _check_searcher({'transportType': 'plane', 'when': '2020-01-01'}, BarisOneDaySearch)
    _check_searcher({'transportType': 'plane', 'when': 'tomorrow'}, BarisOneDaySearch)

    _check_searcher({'transportType': 'bus'}, RaspDbAllDaysSearch)
    _check_searcher({'transportType': 'bus', 'nearest': True}, RaspDbNearestSearch)
    _check_searcher({'transportType': 'bus', 'when': '2020-01-01'}, RaspDbOneDaySearch)
    _check_searcher({'transportType': 'bus', 'when': 'tomorrow'}, RaspDbOneDaySearch)

    _check_searcher({'transportType': 'train'}, RaspDbAllDaysSearch)
    _check_searcher({'transportType': 'train', 'nearest': True}, RaspDbNearestSearch)
    _check_searcher({'transportType': 'train', 'when': '2020-01-01'}, RaspDbOneDaySearch)
    _check_searcher({'transportType': 'train', 'when': 'tomorrow'}, RaspDbOneDaySearch)

    _check_searcher({'transportType': 'suburban'}, RaspDbAllDaysSearch)
    _check_searcher({'transportType': 'suburban', 'nearest': True}, RaspDbNearestSearch)
    _check_searcher({'transportType': 'suburban', 'when': '2020-01-01'}, RaspDbOneDaySearch)
    _check_searcher({'transportType': 'suburban', 'when': 'tomorrow'}, RaspDbOneDaySearch)
