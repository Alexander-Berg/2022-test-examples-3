# -*- coding: utf-8 -*-


def assert_model_set(expected_ids, objects):
    assert set([obj.id for obj in objects]) == set(expected_ids)
