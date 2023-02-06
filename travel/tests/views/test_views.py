# -*- coding: utf-8 -*-
import json

import pytest
from flask import Response

from travel.avia.ticket_daemon_api.jsonrpc.application import create_app
from travel.avia.ticket_daemon_api.jsonrpc.lib.internal_daemon_client import InternalDaemonException
from travel.avia.ticket_daemon_api.jsonrpc.views import jsend_view


def _raise_internal_daemon_exc_view_mock(message='message', code=400, response='Error'):
    def view():
        raise InternalDaemonException(
            message=message, code=code, response=response
        )

    return view


@pytest.mark.parametrize(
    ('internal_daemon_code', 'expected_code'), [
        (400, 400),
        (403, 500),
        (500, 500),
        (502, 500),
    ]
)
def test_internal_daemon_exception_on_jsend_view(internal_daemon_code, expected_code):
    view = jsend_view(_raise_internal_daemon_exc_view_mock(code=internal_daemon_code))

    with create_app({}).app_context():
        response = view()
        assert isinstance(response, Response)
        assert response.status_code == expected_code
        data = json.loads(response.data)
        assert data['status'] == 'fail'
        assert data['data']['error'] == 'Error'
        assert data['data']['description']


def test_jsend_view_ok():
    view = jsend_view(lambda: 'OK')
    with create_app({}).app_context():
        response = view()
        assert isinstance(response, Response)
        assert response.status_code == 200
        data = json.loads(response.data)
        assert data['status'] == 'success'
        assert data['data'] == 'OK'
