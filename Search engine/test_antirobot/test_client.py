# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.martylib.antirobot import AntirobotClient


class TestAntirobotClient(object):
    def test_from_endpoint_uses_admin_port(self):
        endpoint = 'localhost:1000'
        expected = 'localhost:1002'
        client = AntirobotClient.from_endpoint(endpoint)
        session = client.session
        assert session.base_url == 'http://{}'.format(expected)
