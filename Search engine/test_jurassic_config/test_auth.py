# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from jurassic_config import auth

DATA = {
    'CERTS': {
        'host': 'secret',
    },
    'KEYS': {
        'host': 'secret',
    }
}


class TestSetAuthFromFile(object):
    @staticmethod
    def setup_method(method):
        auth.reset_auth()

    def test_removes_spaces_in_values(self):
        auth.set_auth_from_dict(DATA)
        expected = {
            'host': 'secret',
        }
        assert auth.KEYS == expected
        assert auth.CERTS == expected
