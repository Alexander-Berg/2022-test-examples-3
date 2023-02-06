#!/usr/bin/env python
# -*- coding: utf-8 -*-

import pytest
import uuid

from market.idx.yatf.test_envs.yql_env import YqlTestEnv
from market.idx.yatf.resources.yql_resource import YtResource, YqlRequestResource
from yt.wrapper import ypath_join


TABLE_PATH = ypath_join('//home', str(uuid.uuid4()))


def create_offer(id='1', description=''):
    return {'offer_id': id, 'description': description}


@pytest.yield_fixture(scope='module')
def data(yt_server):
    yt_client = yt_server.get_yt_client()
    path = TABLE_PATH
    yt_client.create('table', path, attributes=dict(
        schema=[
            dict(type='string', name='offer_id'),
            dict(type='string', name='description'),
        ]
    ))

    yt_client.write_table(path, [create_offer('1', 'hello'), create_offer('2', 'world')])


@pytest.yield_fixture(scope="module")
def workflow1(yt_server):
    resources = {
        'yt': YtResource(yt_stuff=yt_server),
        'request': YqlRequestResource('SELECT description FROM `%s`' % TABLE_PATH)
    }
    with YqlTestEnv(syntax_version=1, **resources) as test_env:
        test_env.execute()
        yield test_env


def test_yql1(data, workflow1):
    results = workflow1.yql_results
    assert results.is_success, [str(error) for error in results.errors]
    table = list(results)[0]  # get 1st element from a generator
    assert table.rows == [('hello',), ('world',)]


@pytest.yield_fixture(scope="module")
def workflow2(yt_server):
    resources = {
        'yt': YtResource(yt_stuff=yt_server),
        'request': YqlRequestResource('SELECT offer_id, description FROM `%s` WHERE offer_id=="1"' % TABLE_PATH)
    }
    with YqlTestEnv(syntax_version=1, **resources) as test_env:
        test_env.execute()
        yield test_env


def test_yql2(data, workflow2):
    results = workflow2.yql_results
    assert results.is_success, [str(error) for error in results.errors]
    table = list(results)[0]  # get 1st element from a generator
    assert table.rows == [('1', 'hello')]
