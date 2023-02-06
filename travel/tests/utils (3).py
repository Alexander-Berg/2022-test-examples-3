# -*- encoding: utf-8 -*-
import random

from travel.avia.travelers.application.lib.feature_flag_storage import FeatureFlagStorage

TEST_FLAG = 'TEST'


def get_test_flag(storage: FeatureFlagStorage) -> bool:
    return storage.flag_by_code(TEST_FLAG)


def enable_test_flag(client):
    client.update_flags({TEST_FLAG})


def update_params(func):
    def wrapped(kwargs):
        params = func()
        params.update(kwargs)
        return params
    return wrapped


def update_params_for_method(func):
    def wrapped(self, kwargs):
        params = func(self)
        params.update(kwargs)
        return params
    return wrapped


def random_enum(enum_type):
    return random.choice(list(enum_type))
