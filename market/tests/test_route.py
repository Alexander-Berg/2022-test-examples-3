# coding: utf-8

import pytest
import logging
import flask

from hamcrest import assert_that

from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.semver import SemVer, get_current_version
from market.idx.api.backend.blueprints.common import Formatter, handle_request_versions, handle_request_with_formatter, add_self_url_for_page
from market.idx.api.backend.marketindexer.storage.storage import Storage

from utils import (
    is_bad_response,
    is_success_response,
)

logger = logging.getLogger('idxapi.route_test_logger')


def make_url_endpoint(version):
    return '/v{0}/endpoint'.format(version)


@pytest.fixture(scope="module")
def test_page():
    test_page = flask.Blueprint('test_page', __name__)

    @test_page.route('/v<semver:v>/endpoint', methods=['GET'])
    @handle_request_with_formatter(Formatter(), logger)
    @handle_request_versions('1.0.2', '3.0.0')
    @add_self_url_for_page('test_page')
    def versioned_endpoint(v):
        return {
            'current_version': str(flask.current_app.config['VERSION']),
            'requested_version': str(v)
        }

    return test_page


@pytest.fixture(scope="module")
def test_app(test_page):
    app = create_flask_app(Storage())
    app.register_blueprint(test_page)
    return app


def test_app_version(test_app):
    assert test_app.config['VERSION'] == get_current_version()


@pytest.mark.parametrize('version', [
    ('x'),
    ('x.y'),
    ('1.x'),
    ('1.1.z'),
    ('1.1.-1'),
    ('1.1.1.1'),
])
def test_undef_version(test_app, version):
    with test_app.test_client() as client:
        url = make_url_endpoint(version)
        resp = client.get(url)
        assert_that(resp, is_bad_response('400 Bad Request\nrequested undef version'))


@pytest.mark.parametrize('version, expected', [
    ('1', '1.*.*'),
    ('1.0', '1.0.*'),
    ('1.1', '1.1.*'),
    ('1.1.1', '1.1.1'),
])
def test_version_endpoint(test_app, version, expected):
    with test_app.test_client() as client:
        url = make_url_endpoint(version)
        resp = client.get(url)
        assert_that(resp, is_success_response())

        data = flask.json.loads(resp.data)
        assert data['current_version'] == str(test_app.config['VERSION'])
        assert data['requested_version'] == expected
        assert data['self_url'] == 'http://localhost:29334' + url


@pytest.mark.parametrize('version', [
    ('0'),
    ('0.99'),
    ('0.9999.9999'),
    ('1.0.0'),
    ('1.0.1'),
])
def test_not_implemented_version_endpoint(test_app, version):
    with test_app.test_client() as client:
        url = make_url_endpoint(version)
        resp = client.get(url)
        assert_that(resp, is_bad_response('400 Bad Request\nmethod implemented in "1.0.2" version, requested "{0}" version'.format(str(SemVer(version)))))


@pytest.mark.parametrize('version', [
    ('3'),
    ('3.0'),
    ('3.0.0'),
    ('4'),
    ('4.0'),
    ('4.5.5'),
])
def test_deprecated_version_endpoint(test_app, version):
    with test_app.test_client() as client:
        url = make_url_endpoint(version)
        resp = client.get(url)
        assert_that(resp, is_bad_response('400 Bad Request\nmethod deprecated in "3.0.0" version, requested "{0}" version'.format(str(SemVer(version)))))


@pytest.mark.parametrize('version', [
    ('2.2'),
    ('2.9.5'),
])
def test_request_future_version_endpoint(test_app, version):
    with test_app.test_client() as client:
        url = make_url_endpoint(version)
        resp = client.get(url)
        assert_that(resp, is_bad_response('400 Bad Request\nrequest future version "{0}", current version "{1}"'.format(str(SemVer(version)), get_current_version())))
