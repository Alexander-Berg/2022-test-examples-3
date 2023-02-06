# -*- coding: utf-8 -*-
from contextlib import contextmanager

import feature_flag_client
import mock
import pytest
from flask import jsonify, url_for

from travel.avia.ticket_daemon_api.jsonrpc.application import create_app
from travel.avia.ticket_daemon_api.jsonrpc.lib.feature_flags import flag_enabled, feature_flags_storage

TEST_APP_CONFIG = {}
TEST_FLAG = 'TEST_AB_FLAG'


def get_test_app():
    app = create_app(TEST_APP_CONFIG)

    @app.route('/get_flag_value', methods=['GET'])
    def get_flag_value():
        return jsonify({'result': flag_enabled(TEST_FLAG)})

    return app


def get_url(ab_flag_value):
    if ab_flag_value is None:
        params = {}
    else:
        params = {'back_flags': '%s=%s' % (TEST_FLAG, ab_flag_value)}
    return url_for('get_flag_value', **params)


@contextmanager
def patch_flag_client(flags=None, ab_flags=None):
    with mock.patch(
            'travel.avia.ticket_daemon_api.jsonrpc.lib.feature_flags._feature_flags_client.create_context',
            return_value=feature_flag_client.Context(
                flags=flags or set(),
                ab_flags=ab_flags or set(),
            ),
    ):
        yield


@pytest.mark.parametrize('flags, ab_flags, ab_flag_value, result', [
    # флаг однозначно выключен
    (None, None, None, False),
    (None, None, 0, False),
    (None, None, 1, False),

    # флаг однозначно включен
    ({TEST_FLAG}, None, None, True),
    ({TEST_FLAG}, None, 0, True),
    ({TEST_FLAG}, None, 1, True),

    # флаг зависит от АБ
    (None, {TEST_FLAG}, None, False),
    (None, {TEST_FLAG}, 0, False),
    (None, {TEST_FLAG}, 1, True),
])
def test_enabled_ab_flag(flags, ab_flags, ab_flag_value, result):
    feature_flags_storage.reset_context()
    app = get_test_app()
    with patch_flag_client(flags=flags, ab_flags=ab_flags):
        with app.test_request_context():
            response = app.test_client().get(get_url(ab_flag_value))

    assert response.status_code == 200, response.text
    assert response.json['result'] == result
