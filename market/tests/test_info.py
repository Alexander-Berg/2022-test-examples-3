# -*- coding: utf-8 -*-

import pytest

import flask
from mock import patch

from hamcrest import assert_that

from utils import (
    is_success_response,
    is_bad_response,
)

from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.marketindexer.storage.storage import Storage
from test_generations import FULL_GENERATIONS, LAST_GENERATION


@pytest.fixture(scope="module")
def test_app():
    return create_flask_app(Storage())


def test_info_deploy_without_query_param_role(test_app):
    '''
    этот тест проверяет, что если в запросе не указан параметр role,
    тот вернется 400 Bad Request
    '''
    with test_app.test_client() as client:
        resp = client.get('/v1/info/deploy?mitype=gibson')
        assert_that(resp, is_bad_response('400 Bad Request\nrequest must contains query parameters "role" and "mitype"'))


def test_info_deploy_without_query_param_mitype(test_app):
    '''
    этот тест проверяет, что если в запросе не указан параметр mitype,
    тот вернется 400 Bad Request
    '''
    with test_app.test_client() as client:
        resp = client.get('/v1/info/deploy?role=indexer')
        assert_that(resp, is_bad_response('400 Bad Request\nrequest must contains query parameters "role" and "mitype"'))


def test_info_deploy_with_bad_query_param_role(test_app):
    '''
    этот тест проверяет, что если в запросе указан параметр role,
    про который мы ничего не знаем,
    тот вернется 400 Bad Request
    '''
    with test_app.test_client() as client:
        resp = client.get('/v1/info/deploy?role=blablabla&mitype=gibson')
        assert_that(resp, is_bad_response('400 Bad Request\nundefined role="blablabla"'))


def test_info_deploy_with_bad_query_param_mitype(test_app):
    '''
    этот тест проверяет, что если в запросе указан параметр mitype,
    про который мы ничего не знаем,
    тот вернется 400 Bad Request
    '''
    with test_app.test_client() as client:
        resp = client.get('/v1/info/deploy?role=indexer&mitype=fender')
        assert_that(resp, is_bad_response('400 Bad Request\nundefined mitype="fender"'))


def test_info_deploy(test_app):
    '''
    этот тест проверяет, что работают корректные запросы
    '''
    patch_gen = patch('market.idx.api.backend.generations.get_generations', auto_spec=True, return_value=FULL_GENERATIONS)
    patch_last_gen = patch('market.idx.api.backend.generations.get_last_generation', auto_spec=True, return_value=LAST_GENERATION)
    with patch_gen, patch_last_gen:

        with test_app.test_client() as client:
            resp = client.get('/v1/info/deploy?role=indexer&mitype=stratocaster&cdt=2017-11-27%2018%3A40%3A10')
            assert_that(resp, is_success_response())

            data = flask.json.loads(resp.data)
            assert not data['safe-deploy']  # индексатор нельзя катить в лоб
            assert data['estimated-time-seconds'] == 657  # нужно подождать вот столько секунд пока дособерется поколение


def test_info_deploy_cancel_current_generation(test_app):
    '''
    этот тест проверяет, что можно катиться если прошло меньше 20 минут от начала сборки
    '''
    patch_gen = patch('market.idx.api.backend.generations.get_generations', auto_spec=True, return_value=FULL_GENERATIONS)
    patch_last_gen = patch('market.idx.api.backend.generations.get_last_generation', auto_spec=True, return_value=LAST_GENERATION)
    with patch_gen, patch_last_gen:

        with test_app.test_client() as client:
            # LAST_GENERATION start_date 2017-11-27 17:21:07
            resp = client.get('/v1/info/deploy?role=indexer&mitype=stratocaster&cdt=2017-11-27%2017%3A31%3A10')
            assert_that(resp, is_success_response())

            data = flask.json.loads(resp.data)
            assert data['safe-deploy']  # индексатор можно катить, прошло меньше 20 минут от начала сборки
            assert data['estimated-time-seconds'] == 4797  # Время до следующего поколения


def test_info_deploy_with_turbo_mitype(test_app):
    '''
    этот тест проверяет, что работает mitype=turbo.stratocaster
    '''
    patch_gen = patch('market.idx.api.backend.generations.get_generations', auto_spec=True, return_value=FULL_GENERATIONS)
    patch_last_gen = patch('market.idx.api.backend.generations.get_last_generation', auto_spec=True, return_value=LAST_GENERATION)
    with patch_gen, patch_last_gen:

        with test_app.test_client() as client:
            resp = client.get('/v1/info/deploy?role=indexer&mitype=turbo.stratocaster')
            assert_that(resp, is_success_response())


def test_info_configuration_without_query_param_role(test_app):
    '''
    этот тест проверяет, что если в запросе не указан параметр role,
    тот вернется 400 Bad Request
    '''
    with test_app.test_client() as client:
        resp = client.get('/v1/info/configuration?mitype=gibson')
        assert_that(resp, is_bad_response('400 Bad Request\nrequest must contains query parameters "role"'))


def test_info_configuration_with_bad_query_param_role(test_app):
    '''
    этот тест проверяет, что если в запросе указан параметр role,
    про который мы ничего не знаем,
    тот вернется 400 Bad Request
    '''
    with test_app.test_client() as client:
        resp = client.get('/v1/info/configuration?role=blablabla')
        assert_that(resp, is_bad_response('400 Bad Request\nundefined role="blablabla"'))


def test_info_configuration(test_app):
    '''
    этот тест проверяет, что работают корректные запросы
    '''
    patch_client = patch('market.idx.api.backend.tsum.Client', auto_spec=True)
    patch_master_type = patch('market.idx.api.backend.tsum.IndexerClient.get_current_master_type', return_value='stratocaster')
    with patch_client, patch_master_type:

        with test_app.test_client() as client:
            resp = client.get('/v1/info/configuration?role=indexer')
            assert_that(resp, is_success_response())

            data = flask.json.loads(resp.data)
            assert data["active"]["mitype"] == "stratocaster"
            assert data["reserved"]["mitype"] == "gibson"
