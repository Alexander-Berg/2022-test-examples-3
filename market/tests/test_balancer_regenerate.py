# coding: utf8


import os
import yaml
import importlib
from flask import Response
from market.sre.services.balancer_api.lib.request_handler import SharedReqHandler


def test_balancer_regenerate(client):
    """ Demofslb должен генерировать валидный конфиг балансера """
    # Create one test service
    response = client.post(
        '/add',
        json={
            "vhost": "front-blue-unified--marketfrontech-701api",
            "host": "2a02:6b8:c0d:2987:10c:de5d:0:5f6e",
            "web_sockets": False,
            "port": 24430,
        }
    )
    assert response.status_code == 200, response.data

    # Generate and vaildate config
    response = client.get('/restore_state')
    assert response.status_code == 200


def test_shared_req_handler(client, app):
    json_doc = {
        "vhost": "front-market--m-1438229e801ec748ffb34facfac39d6faa-dsk",
        "host": "2a02:6b8:c08:bc19:10b:11e5:5a47:0",
        "port": 80,
        "web_sockets": True,
        "backend_proto": "https",
        "service_name": "market"
    }

    # тестируем добавление конфига по json-данным из запроса
    with app.app_context():
        req_handler = SharedReqHandler(json_doc, app.config)
        response = Response(req_handler.add_or_update())

    # Проверяем, что у нас генерится правильное имя конфига
    nginx_config_name = 'front-market--m-1438229e801ec748ffb34facfac39d6faa-dsk.market_slb-front-testing.yaml'
    nginx_config_path = os.path.join(app.config['VALUES_NGINX_DIR'], nginx_config_name)
    assert SharedReqHandler.get_nginx_conf_path(json_doc['vhost'], app.config) == nginx_config_path

    assert response.status_code == 200
    assert len(list(req_handler.db.config.find({"vhost": req_handler.vhost}))) == 1

    # через /restore_state проверяем возможность сгенерировать конфиг
    response = client.get('/restore_state')
    assert response.status_code == 200

    # проверяем валидность сгенерированного yaml-файла
    with open(req_handler.nginx_conf_path, 'r') as stream:
        values_config = yaml.safe_load(stream)

    # валидируем схему yaml-файла
    schema = importlib.import_module('market.sre.conf.slb-nginx.common.tests.primer_config_schema').config_schema
    schema.validate(values_config)

    # проверяем, что конфиг удаляется
    with app.app_context():
        req_handler = SharedReqHandler(json_doc, app.config)
        response = req_handler.delete()

    assert response.status_code == 200
    assert list(req_handler.db.config.find({"vhost": req_handler.vhost})) == []
    assert list(req_handler.db.journal.find({"vhost": req_handler.vhost}))[-1]["action"] == "delete"


def test_init_db_from_local_state(client, app):
    conf_with_fallback = {
        u'backend_proto': u'https',
        u'service_name': u'partner',
        u'fallback_pool': u'partner-front--marketpartner-14128-auto-assort-x5',
        u'vhost': u'partner-front--marketpartner-14128-auto-assort',
        u'host': u'2a02:6b8:c1b:27a0:10b:11e5:ef93:0',
        u'web_sockets': False,
        u'port': 80
    }

    conf_without_fallback = {
        u'vhost': u'front-market--marketfront-4877-api',
        u'host': u'2a02:6b8:c1b:2c19:10b:11e5:4262:0',
        u'port': 80,
        u'web_sockets': True,
        u'backend_proto': u'https',
        u'service_name': u'market'
    }

    resp_fb = client.post(
        path='/add',
        method='POST',
        json=conf_with_fallback
    )
    resp = client.post(
        path='/add',
        method='POST',
        json=conf_without_fallback
    )

    assert resp_fb.status_code == 200
    assert resp.status_code == 200

    # через /restore_state генерим положенные до этого в базу конфиги
    response_restore = client.get('/restore_state')
    assert response_restore.status_code == 200

    # через /init_db чистим базу, и загружаем в нее данные из конфигов
    response_init_db = client.get('/init_db')
    assert response_init_db.status_code == 200

    # сверяем результаты в базе с теми, которые были изначально
    db = app.config.db
    assert len([doc for doc in db.config.find({})]) == 2
    with_fallback = db.config.find_one({'vhost': 'partner-front--marketpartner-14128-auto-assort'})
    without_fallback = db.config.find_one({'vhost': 'front-market--marketfront-4877-api'})
    del with_fallback['_id']
    del without_fallback['_id']

    assert with_fallback[u'service_name'] == u'market'
    conf_with_fallback[u'service_name'] = u'market'

    assert conf_with_fallback == with_fallback
    assert conf_without_fallback == without_fallback
