# -*- coding: utf-8 -*-
import pytest
import socket
from yatest.common import network
from runtime_tests.util.predef import http
from runtime_tests.util.predef.handler.server.http import SimpleConfig


@pytest.mark.parametrize('family', [socket.AF_INET, socket.AF_INET6], ids=['IPv4', 'IPv6'])
def test_backend_manager(backend_manager, session_connection_manager, family):
    content = 'Led Zeppelin'
    with network.PortManager() as pm:
        port = pm.get_port()
        backend_manager.start(SimpleConfig(response=http.response.ok(data=content)), port, family)
        conn = session_connection_manager.create(port=port)
        resp = conn.perform_request(http.request.get())
        assert resp.data.content == content
