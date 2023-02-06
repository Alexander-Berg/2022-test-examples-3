# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.library.python.api_clients.ticket_daemon.jsend import (
    Error, Fail, InvalidResponse, process_errors, read
)


def test_no_status():
    with pytest.raises(InvalidResponse):
        process_errors({}, 200)


def test_no_payload():
    with pytest.raises(InvalidResponse):
        process_errors({'status': 'success'}, 200)

    with pytest.raises(InvalidResponse):
        process_errors({'status': 'fail'}, 200)

    with pytest.raises(InvalidResponse):
        process_errors({'status': 'error'}, 200)


def test_error():
    with pytest.raises(Error) as excinfo:
        process_errors({'status': 'error', 'message': 'error message'}, 200)

        assert excinfo.value.message == 'error message'
        assert excinfo.value.code is None
        assert excinfo.value.data is None

    with pytest.raises(Error) as excinfo:
        process_errors(
            {'status': 'error', 'message': 'error message', 'code': 'error code', 'data': 'error data'}, 200
        )

        assert excinfo.value.message == 'error message'
        assert excinfo.value.code == 'error code'
        assert excinfo.value.data == 'error data'


def test_process_fail_error():
    with pytest.raises(Fail) as excinfo:
        process_errors({'status': 'fail', 'data': 'response data'}, 500)

        assert excinfo.value.data == 'response data'

    process_errors({'status': 'fail', 'data': 'response data'}, 400)


def test_process_fail_read():
    with pytest.raises(Fail) as excinfo:
        read({'status': 'fail', 'data': 'response data'})

        assert excinfo.value.data == 'response data'


def test_success():
    assert read({'status': 'success', 'data': 'response data'}) == 'response data'


def test_invalid_status():
    with pytest.raises(InvalidResponse):
        read({'status': 'invalid_status'})
