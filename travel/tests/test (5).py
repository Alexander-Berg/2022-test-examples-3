# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from pytest import raises

from travel.library.python.yandex_vault import get_filled_args, resolve_secrets


class FakeClient(object):

    def __init__(self, data):
        self.data = data

    def get_version(self, key):
        return {'value': self.data[key]}


def test_resolve_secrets():
    with raises(ValueError):
        resolve_secrets([])


def test_get_filled_args():

    client = FakeClient({
        'sec-0': {'value': 'secret_value'},
        'sec-1': {'token': 'token_value'},
        'sec-2': {
            'login': 'login_value',
            'password': 'password_value',
        },
    })

    assert ['secret_value'] == get_filled_args(['sec-0'], client)
    assert ['secret_value'] == get_filled_args(['sec-0.value'], client)

    assert ['token_value'] == get_filled_args(['sec-1.token'], client)

    assert ['login_value'] == get_filled_args(['sec-2.login'], client)
    assert ['password_value'] == get_filled_args(['sec-2.password'], client)

    with raises(ValueError):
        get_filled_args(['sec-0.level1.level2'], client)
