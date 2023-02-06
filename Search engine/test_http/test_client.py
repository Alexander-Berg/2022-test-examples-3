# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import unittest

from search.martylib.http.client import AbstractHttpClient


class TestAbstractHttpClient(unittest.TestCase):
    """
    `AbstractHttpClient` functionality tests.
    """

    def test_session_adapter(self):
        test_params = (
            (AbstractHttpClient(), 10),
            (AbstractHttpClient(adapter_pool_maxsize=100), 100),
        )

        for client, expected_pool_maxsize in test_params:
            pm = client.session.adapters['https://'].poolmanager
            self.assertEqual(pm.connection_pool_kw.get('maxsize'), expected_pool_maxsize)
