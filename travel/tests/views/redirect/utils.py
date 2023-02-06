# -*- encoding: utf-8 -*-
import json

import flask
from requests import Response

from travel.avia.library.python.tester.factories import create_partner
from travel.avia.ticket_daemon.ticket_daemon.application import create_app

TEST_APP_CONFIG = {}
TEST_PARTNER = 'test_partner'
TEST_PARTNER_MODULE = '__test_partner_module'


def get_client_app():
    return create_app(TEST_APP_CONFIG).test_client()


def create_test_partner_with_module(module=TEST_PARTNER_MODULE):
    create_partner(
        code=TEST_PARTNER,
        query_module_name=module,
    )


def api_cook_redirect(data):
    #  type: (dict) -> flask.Response
    return get_client_app().post(
        '/api/1.0/cook_redirect/',
        headers={'Content-type': 'application/json'},
        data=json.dumps(data)
    )


def get_response(status=200, json_content=None, content=None):
    response = Response()
    response.status_code = status

    if json:
        content = json.dumps(json_content)

    if content:
        response._content = content
    return response
