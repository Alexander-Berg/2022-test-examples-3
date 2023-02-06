#!/usr/bin/env python
# -*- coding: utf-8 -*-

from extsearch.images.robot.scripts.cm.semidup2.pipeline import Pipeline

import fake_factory


def test_wait_request():
    data = {
        'test_queue': [{'id': 'R1'}, {'id': 'R2'}],
        'active_test_request': None
    }
    settings = {'state_path': 'states'}
    factory = fake_factory.FakeFactory(settings, data)
    pipeline = Pipeline(factory, 'test')
    wait_result = pipeline.wait_request()

    assert wait_result
    assert len(data['test_queue']) == 1
    assert data['test_queue'][0]['id'] == 'R2'
    assert data['active_test_request']['id'] == 'R1'


def test_wait_active_request():
    data = {
        'test_queue': [{'id': 'R2'}],
        'active_test_request': {'id': 'R1'}
    }
    settings = {'state_path': 'states'}
    factory = fake_factory.FakeFactory(settings, data)
    pipeline = Pipeline(factory, 'test')
    wait_result = pipeline.wait_request()

    assert wait_result
    assert len(data['test_queue']) == 1
    assert data['test_queue'][0]['id'] == 'R2'
    assert data['active_test_request']['id'] == 'R1'


class StartDisabledPipeline(Pipeline):
    def __init__(self, factory):
        Pipeline.__init__(self, factory, 'test')

    def test_start_condition(self, vars):
        return False


def test_wait_disabled_request():
    data = {
        'test_queue': [{'id': 'R1'}, {'id': 'R2'}],
        'active_test_request': None
    }
    settings = {'state_path': 'states', 'request_pooling_period': 0.5}
    factory = fake_factory.FakeFactory(settings, data)
    pipeline = StartDisabledPipeline(factory)
    wait_result = pipeline.wait_request(1)

    assert not wait_result
    assert len(data['test_queue']) == 2
    assert data['active_test_request'] is None


def test_wait_disabled_active_request():
    data = {
        'test_queue': [{'id': 'R2'}],
        'active_test_request': {'id': 'R1'}
    }
    settings = {'state_path': 'states', 'request_pooling_period': 1}
    factory = fake_factory.FakeFactory(settings, data)
    pipeline = StartDisabledPipeline(factory)
    wait_result = pipeline.wait_request(1)

    assert wait_result
    assert len(data['test_queue']) == 1
    assert data['active_test_request']['id'] == 'R1'


def test_process_trash_queue():
    data = {
        'trash_queue': [{'path': 'X'}, {'path': 'Y'}]
    }
    settings = {'state_path': 'states'}
    factory = fake_factory.FakeFactory(settings, data)
    pipeline = Pipeline(factory, 'test')
    pipeline.process_trash_queue()

    assert len(data['trash_queue']) == 0
    trash_content = factory.get_trash().get_content()
    assert len(trash_content) == 2
    assert trash_content[0] == 'X'
    assert trash_content[1] == 'Y'
