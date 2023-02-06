# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function


def assert_model_set(expected_ids, objects):
    assert set([obj.id for obj in objects]) == set(expected_ids)
