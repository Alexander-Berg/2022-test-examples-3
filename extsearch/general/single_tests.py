#!/usr/bin/env python
# -*- coding: utf-8 -*-

import extsearch.images.robot.scripts.cm.semidup2.single as single

import fake_factory


def test_finish_full():
    data = {
        'branches': {
            'production': {
                'base_state': 'B0',
                'full_list': ['S0']
            },
            'latest': {
                'base_state': 'B1',
                'full_list': []
            }
        },
        'increment_queue': [{'state': 'S0'}],
        'single_queue': [{'state': 'S2', 'type': 'short'}],
        'concat_queue': [],
        'short_autoremove': [],
        'active_single_request': {'state': 'S1', 'type': 'full', 'human_id': 'full S1'}
    }
    settings = {'state_path': 'states', 'autoremove': {'short': {'max_length': 5, 'queue_name': 'short_autoremove'}}}
    factory = fake_factory.FakeFactory(settings, data)
    pipeline = single.SinglePipeline(factory)
    pipeline.finish()

    assert data['branches']['production']['full_list'] == ['S0', 'S1']
    assert data['branches']['latest']['full_list'] == ['S1']

    assert len(data['concat_queue']) == 1

    concat_request = data['concat_queue'][0]
    assert concat_request['base_state'] == 'B0'
    assert concat_request['short_state'] is None
    assert concat_request['full_state_list'] == ['S0', 'S1']

    assert len(data['increment_queue']) == 2
    inc_request = data['increment_queue'][-1]
    assert inc_request['state'] == 'S1'

    assert len(data['short_autoremove']) == 0
    assert factory.get_trash().is_empty()

    assert data['active_single_request'] is None


def test_finish_short():
    data = {
        'branches': {
            'production': {
                'base_state': 'B0',
                'full_list': ['S0']
            },
            'latest': {
                'base_state': 'B1',
                'full_list': []
            }
        },
        'increment_queue': [{'state': 'S0'}],
        'single_queue': [],
        'concat_queue': [],
        'short_autoremove': [],
        'active_single_request': {'state': 'S1', 'type': 'short', 'human_id': 'short S1'}
    }
    settings = {'state_path': 'states', 'autoremove': {'short': {'max_length': 5, 'queue_name': 'short_autoremove'}}}
    factory = fake_factory.FakeFactory(settings, data)
    pipeline = single.SinglePipeline(factory)
    pipeline.finish()

    assert data['branches']['production']['full_list'] == ['S0']
    assert data['branches']['latest']['full_list'] == []

    assert len(data['concat_queue']) == 1
    concat_request = data['concat_queue'][0]
    assert concat_request['base_state'] == 'B0'
    assert concat_request['short_state'] == 'S1'
    assert concat_request['full_state_list'] == ['S0']

    assert len(data['increment_queue']) == 1

    assert len(data['short_autoremove']) == 0
    assert factory.get_trash().is_empty()

    assert data['active_single_request'] is None


def test_finish_drop_unused_short_state():
    data = {
        'branches': {
            'production': {
                'base_state': 'B0',
                'full_list': []
            }
        },
        'single_queue': [],
        'concat_queue': [{'base_state': 'B0', 'full_state_list': [], 'short_state': 'S1', 'branch': 'production'}],
        'trash_queue': [],
        'short_autoremove': [],
        'active_single_request': {'state': 'S2', 'type': 'short', 'human_id': 'short S2'}
    }
    settings = {'state_path': 'states', 'autoremove': {'short': {'max_length': 5, 'queue_name': 'short_autoremove'}}}
    factory = fake_factory.FakeFactory(settings, data)
    pipeline = single.SinglePipeline(factory)
    pipeline.finish()

    assert len(data['concat_queue']) == 1
    concat_request = data['concat_queue'][0]
    assert concat_request['base_state'] == 'B0'
    assert concat_request['full_state_list'] == []
    assert concat_request['short_state'] == 'S2'

    assert len(data['short_autoremove']) == 1
    assert data['short_autoremove'][0] == 'states/short/S1'


def test_finish_keep_used_short_state():
    data = {
        'branches': {
            'production': {
                'base_state': 'B0',
                'full_list': ['B1']
            },
            'latest': {
                'base_state': 'B1',
                'full_list': []
            }
        },
        'concat_queue': [
            {'base_state': 'B0', 'full_state_list': ['B1'], 'short_state': 'S1', 'branch': 'production'},
            {'base_state': 'B1', 'full_state_list': [], 'short_state': 'S1', 'branch': 'special_branch_26'}
        ],
        'trash_queue': [],
        'short_autoremove': [],
        'active_single_request': {'state': 'S2', 'type': 'short', 'human_id': 'short S2'}
    }
    settings = {'state_path': 'states', 'autoremove': {'short': {'max_length': 5, 'queue_name': 'short_autoremove'}}}
    factory = fake_factory.FakeFactory(settings, data)
    pipeline = single.SinglePipeline(factory)
    pipeline.finish()

    assert len(data['concat_queue']) == 2
    concat_request_0 = data['concat_queue'][0]
    assert concat_request_0['short_state'] == 'S1'
    concat_request_1 = data['concat_queue'][1]
    assert concat_request_1['short_state'] == 'S2'

    assert len(data['short_autoremove']) == 0


def test_double_finish():
    data = {
        'active_single_request': None
    }
    settings = {'state_path': 'states'}
    factory = fake_factory.FakeFactory(settings, data)
    pipeline = single.SinglePipeline(factory)
    pipeline.finish()

    assert data['active_single_request'] is None
