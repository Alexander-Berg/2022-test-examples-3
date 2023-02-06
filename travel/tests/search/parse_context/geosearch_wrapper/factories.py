# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.tester.factories import create_settlement
from geosearch.views.pointlist import PointList
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper import InputSearchContext


def create_testing_input_search_context():
    return InputSearchContext(None, None, None, None, None, None, None, None)


def create_testing_point_list(point=None):
    if not point:
        point = create_settlement()
    return PointList(point)
