# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import sys

import pytest


reload(sys)
sys.setdefaultencoding('utf-8')  # FIXME


@pytest.fixture(scope='session')
def app():
    import connector
    from app import app, api

    app.config.from_object('config')

    for module in (connector, ):
        resources = module.get_resources()
        for resource in resources:
            api.add_resource(resource, *resource.routes)

    return app


@pytest.fixture
def app_client(app):
    with app.test_client() as client:
        yield client
