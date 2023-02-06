# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from ylog.context import log_context

from travel.rasp.export.export.v3.selling.train_api import build_train_api_params


class TestTrainApi(object):

    @pytest.mark.parametrize("u_id,s_id,expected", [['u', '', 'u'], ['', 's', 's'], ['u', 's', 'u'], ['', '', '']])
    def test_query_with_rid(self, u_id, s_id, expected):
        query = {
            'point_from': 's666',
            'point_to': 's667',
            'date': '2018-09-10'
        }

        with log_context(service_rid=s_id, user_rid=u_id):
            params = build_train_api_params(query)

        assert params['_rid'] == expected
