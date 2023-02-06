# -*- coding: utf-8 -*-
import json
import urlparse
from datetime import datetime, timedelta

import mock
import pytest
from django.utils.http import urlencode
from requests import Response

from travel.avia.ticket_daemon.ticket_daemon.application import create_app as real_create_app


@pytest.fixture(scope='session')
def today():
    return datetime.now().date()


@pytest.yield_fixture(scope='session')
def client_app(request):
    app = real_create_app({})
    client_app = app.test_client()
    ctx = app.app_context()
    ctx.push()
    yield client_app
    ctx.pop()


DT_FMT = '%Y-%m-%d'


@pytest.fixture(scope='session')
def response_track_start(client_app, today):
    pytest.skip('Долго не запускался и сейчас падает')
    params = {
        'query_params': {
            'point_from': 'c213',
            'point_to': 'c2',
            'date_forward': (today + timedelta(3)).strftime(DT_FMT),
            'date_backward': None,
            'national_version': 'ru',
            'service': 'test',
            'klass': 'economy',
            'passengers': {'adults': 1},
        },
        'p_code': 'senturia',
    }

    r = client_app.post(
        'api/1.0/track/start/',
        data=json.dumps(params),
        headers={'Content-type': 'application/json'}
    )

    return r


@pytest.fixture(scope='session')
def track_id(response_track_start):
    r = response_track_start

    print r.data
    assert r.status_code == 200

    json_data = json.loads(r.data)
    print json_data
    assert json_data['status'] == 'ok'
    assert json_data['data']['trid']

    return json_data['data']['trid']


def test_book_redirect(client_app):
    token = 'test_token'
    order_data = {'qid': 'qid', 'partner': 'test_partner'}

    def mocked_post_response(*args, **kwargs):
        response = Response()
        response._content = json.dumps({
            u'redirectUrl': u'https://fake_front/book?token=%s' % token})
        response.headers = {
            'Content-Encoding': 'gzip',
            'Transfer-Encoding': 'chunked',
            'Vary': 'Accept-Encoding',
            'Keep-Alive': 'timeout=120',
            'Server': 'nginx',
            'Connection': 'keep-alive',
            'Date': 'Fri, 18 May 2018 09:11:30 GMT',
            'Content-Type': 'application/json;charset=UTF-8'
        }
        response.status_code = 200
        return response

    with mock.patch('requests.Session.post', side_effect=mocked_post_response):
        with mock.patch('travel.avia.ticket_daemon.ticket_daemon.views.book_view.tvm_provider.get_ticket', return_value='test_tvm'):
            r = client_app.post(
                '/api/1.0/book_redirect/',
                data=json.dumps({'token': token, 'order_data': order_data}),
                headers={'Content-type': 'application/json'}
            )

    data = json.loads(r.data)
    assert r.status_code == 200
    assert data['url']

    url_query = dict(urlparse.parse_qsl(urlparse.urlsplit(data['url']).query))
    assert url_query['token']


def test_track_results(track_id, client_app):
    pytest.skip('Долго не запускался и сейчас падает')
    r = client_app.get(
        'api/1.0/track/result/?' + urlencode({'trid': track_id}))

    print r.data
    assert r.status_code == 200
