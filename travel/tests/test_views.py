# -*- coding: utf-8 -*-
import time
import json
from datetime import datetime, timedelta

import pytest

from django.conf import settings
from django.utils.http import urlencode

from travel.avia.avia_api.ant.custom_types import HttpSearchIdentificator
from travel.avia.avia_api.avia.api.daemon_api import Chance


def say_hello(app):
    params = {
        'uuid': '1234567890abcdef1234567890abcdef',
        'push_token': 'PUSH_TOKEN.1',
        'app_key': settings.APIKEYS_DEBUG_YKEY,
        'timestamp': '2014-12-12 12:12:12',
        'national_version': 'ru',
    }

    r = app.post('v1.0/hello/?' + urlencode(params))

    print r.get_data()
    assert r.status_code == 200


@pytest.yield_fixture(scope='session')
def app(request):
    from travel.avia.avia_api.avia.application import create_app

    flask_app = create_app({'LOGIN_DISABLED': True})

    app = flask_app.test_client()

    say_hello(app)

    ctx = flask_app.app_context()
    ctx.push()

    yield app

    ctx.pop()


def test_info_por(app):
    d = fetch_jsend(app, 'v1.0/info/por/?lang=ru')

    type_ = d['airports'][0]['station_type_id']
    type_dict = {j['id']: j for j in d['station_type']}
    assert type_dict[type_]['title']

    majority_id = d['cities'][0]['majority_id']
    maj_dict = {j['id']: j for j in d['city_majority']}
    assert maj_dict[majority_id]['title']


def test_order(app):
    r = app.get('v1.0/info/por/?lang=ru')
    assert r.status_code == 200


def fetch_jsend(app, url, status_code=200):
    r = app.get(url)

    assert r.status_code == status_code
    return json.loads(r.data)['data']


@pytest.mark.downloadable
def test_full_cycle(app):
    when = datetime.now() + timedelta(10)

    answer = fetch_jsend(
        app,
        '/v1.0/search/?when=' + when.strftime('%Y-%m-%d') +
        '&to=c2&from=c23'
    )

    search_id = HttpSearchIdentificator.create_from_crypted(answer['search_id'])

    # Забирать результаты через семь секунд после инициации запроса
    time.sleep(7)

    results_url = 'v1.0/search/results/?' + urlencode(dict(
        lang='ru', search_id=search_id.encrypt()
    ))
    answer = fetch_jsend(app, results_url)
    tags = [
        v['vtag'] for v in answer['variants'] or []
    ][:2]
    assert tags

    order_url = 'v1.0/order/?' + urlencode(dict(
        lang='ru', search_id=search_id.encrypt(),
        vtags=','.join(map(str, tags))
    ))
    answer = fetch_jsend(app, order_url)
    variant = answer['variants'][0]

    assert 'tag' not in variant
    assert 'vtag' not in variant
    assert 'chance' in variant

    chance = Chance.create_from_crypted(variant['chance'])
    assert getattr(chance, 'order_data', False)

    order_redirect_url = 'v1.0/order/redirect/?' + urlencode({
        'chance': variant['chance']
    })
    answer = fetch_jsend(app, order_redirect_url)
    assert 'url' in answer
    assert answer['url']


@pytest.mark.downloadable
def test_search_by_sirena(app):
    when = datetime.now() + timedelta(10)

    answer = fetch_jsend(
        app,
        '/v1.0/search/?' + urlencode({
            'when': when.strftime('%Y-%m-%d'),
            'to': 'MOW',
            'from': 'ВРЯ',
        }),
        status_code=200
    )

    assert 'description' not in answer


@pytest.mark.downloadable
def test_search_by_iata(app):
    when = datetime.now() + timedelta(10)

    answer = fetch_jsend(
        app,
        '/v1.0/search/?' + urlencode({
            'when': when.strftime('%Y-%m-%d'),
            'to': 'MOW',
            'from': 'svx',
        }),
        status_code=200
    )

    assert 'description' not in answer


@pytest.mark.downloadable
def test_several_params_validation_error(app):
    when = datetime.now() + timedelta(10)

    answer = fetch_jsend(
        app,
        '/v1.0/search/?' + urlencode({
            'when': when.strftime('%Y-%m-%d'),
            'to': 'c2',
            # 'fromid': 'c23',  # One point should be missed
        }),
        status_code=400
    )

    assert 'errors' in answer['description']
