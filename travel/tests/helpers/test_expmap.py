# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from hamcrest import assert_that, equal_to

from common.tester.testcase import TestCase
from travel.rasp.train_api.helpers.expmap import ExpMap


class TestExpMap(TestCase):
    def test_exp_map_error_duplicate_key(self):
        with pytest.raises(Exception):
            ExpMap("1001111:fix11;1001111:k-armed")

    def test_invalid_exp_map(self):
        with pytest.raises(Exception):
            ExpMap("1001111:::fix11;1001112:k-armed")

    def test_create_exp_map(self):
        em = ExpMap("1001111:fix11;1001112:k-armed")
        assert_that(em._exp_map, equal_to({"1001111": "fix11", "1001112": "k-armed"}))

    def test_get_exp_value_from_exp_boxes(self):
        em = ExpMap("1001111:fix11;1001112:k-armed")
        v = em.get_exp_value_from_exp_boxes("240483,0,-1;240487,0,-1;1001111,0,1")
        assert_that(v, equal_to("fix11"))

    def test_multiple_value_error(self):
        em = ExpMap("1001111:fix11;1001112:k-armed")
        with pytest.raises(ValueError):
            em.get_exp_value_from_exp_boxes("240483,0,-1;1001112,0,-1;1001111,0,1")
