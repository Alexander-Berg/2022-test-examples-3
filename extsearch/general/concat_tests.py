#!/usr/bin/env python
# -*- coding: utf-8 -*-

from extsearch.images.robot.scripts.cm.semidup2.concat import ConcatPipeline

import fake_factory


def test_wait_request():
    data = {
        'concat_queue': [{'base_state': 'B0', 'full_state_list': ['S1', 'S2'], 'short_state': None, 'branch': 'b1', 'tail_state': 'full S2'}],
        'last_published_state': 'P5',
        'active_concat_request': None
    }
    settings = {'state_path': 'states'}
    factory = fake_factory.FakeFactory(settings, data)
    pipeline = ConcatPipeline(factory)
    pipeline.wait_request()

    assert len(data['concat_queue']) == 0
    assert 'state' in data['active_concat_request']


def test_finish():
    data = {
        'active_concat_request': {
            'base_state': 'B0',
            'full_state_list': ['S1', 'S2'],
            'short_state': None,
            'branch': 'B0-0',
            'state': '20180101-220000',
            'parent_state': 'P1',
            'human_id': 'concat finish'
        },
        'short_autoremove': [],
        'concat_autoremove': [],
        'trash_queue': []
    }
    settings = {'state_path': 'states', 'autoremove': {'short': {'max_length': 5, 'queue_name': 'short_autoremove'}, 'concat': {'max_length': 5, 'queue_name': 'concat_autoremove'}}}
    factory = fake_factory.FakeFactory(settings, data)
    pipeline = ConcatPipeline(factory)
    pipeline.finish()

    assert data['active_concat_request'] is None

    assert len(data['concat_autoremove']) == 1
    assert len(data['trash_queue']) == 0
    assert factory.get_trash().is_empty()


def test_finish_with_short():
    data = {
        'active_concat_request': {
            'base_state': 'B0',
            'full_state_list': [],
            'short_state': 'S1',
            'branch': 'B0-0',
            'state': '20180101-230000',
            'parent_state': 'P1',
            'human_id': 'concat finish with short'
        },
        'concat_queue': [],
        'short_autoremove': [],
        'concat_autoremove': [],
        'trash_queue': []
    }
    settings = {'state_path': 'states', 'autoremove': {'short': {'max_length': 5, 'queue_name': 'short_autoremove'}, 'concat': {'max_length': 5, 'queue_name': 'concat_autoremove'}}}
    factory = fake_factory.FakeFactory(settings, data)
    pipeline = ConcatPipeline(factory)
    pipeline.finish()

    assert data['active_concat_request'] is None
    assert len(data['short_autoremove']) == 1
    assert data['short_autoremove'][0] == 'states/short/S1'
    assert len(data['trash_queue']) == 0
    assert factory.get_trash().is_empty()


def test_finish_with_used_short():
    data = {
        'active_concat_request': {
            'base_state': 'B0',
            'full_state_list': [],
            'short_state': 'S1',
            'branch': 'B0-0',
            'state': '20180101-230000',
            'parent_state': '20180101-010000',
            'human_id': 'concat finish with used short'
        },
        'short_state': 'S2',
        'concat_queue': [{'base_state': 'B', 'short_state': 'S1'}],
        'short_autoremove': [],
        'concat_autoremove': [],
        'trash_queue': []
    }
    settings = {'state_path': 'states', 'autoremove': {'short': {'max_length': 5, 'queue_name': 'short_autoremove'}, 'concat': {'max_length': 5, 'queue_name': 'concat_autoremove'}}}
    factory = fake_factory.FakeFactory(settings, data)
    pipeline = ConcatPipeline(factory)
    pipeline.finish()

    assert len(data['short_autoremove']) == 0
    assert len(data['trash_queue']) == 0
    assert factory.get_trash().is_empty()
