#!/usr/bin/env python
# -*- coding: utf-8 -*-

import extsearch.images.robot.scripts.cm.semidup2.increment as increment

import fake_factory


def test_start():
    data = {
        'branches': {
            'latest': {
                'base_state': 'B20',
                'full_list': ['F21', 'F22']
            }
        },
        'active_increment_request': None,
        'increment_queue': [{'state': 'F21'}, {'state': 'F22'}],
        'reglue_queue': [],
        'reglue_states': []
    }
    settings = {'state_path': 'states'}
    factory = fake_factory.FakeFactory(settings, data)
    pipeline = increment.IncrementPipeline(factory)
    pipeline.wait_request()

    r = data['active_increment_request']
    assert r['state'] == 'F21'
    assert r['base_state'] == 'B20'
    assert len(data['increment_queue']) == 1


def test_finish():
    data = {
        'branches': {
            'production': {
                'base_state': 'S0',
                'full_list': ['S1', 'S2', 'S3']
            },
            'latest': {
                'base_state': 'S1',
                'full_list': ['S2', 'S3']
            }
        },
        'active_increment_request': {'state': 'S2', 'base_state': 'S1', 'human_id': 'inc S2 base S1'},
        'increment_queue': [],
        'concat_queue': [],
        'post_process_queue': [],
        'reglue_queue': [],
        'reglue_states': []
    }
    settings = {'state_path': 'states'}
    factory = fake_factory.FakeFactory(settings, data)
    pipeline = increment.IncrementPipeline(factory)
    pipeline.finish()

    branches = data['branches']
    assert len(branches) == 2
    assert branches['latest']['base_state'] == 'S2'
    assert branches['latest']['full_list'] == ['S3']
    assert branches['production']['base_state'] == 'S0'
    assert branches['production']['full_list'] == ['S1', 'S2', 'S3']

    assert len(data['post_process_queue']) == 1
    post_request = data['post_process_queue'][-1]
    assert post_request['state'] == 'S2'

    assert len(data['reglue_queue']) == 0
    assert len(data['reglue_states']) == 1
    assert data['reglue_states'][0] == 'S2'

    assert data['active_increment_request'] is None

    assert factory.get_trash().is_empty()


def test_finish_with_reglue():
    data = {
        'branches': {
            'production': {
                'base_state': 'S0',
                'full_list': ['S1', '20190101-101500', 'S3']
            },
            'latest': {
                'base_state': 'S1',
                'full_list': ['20190101-101500', 'S3']
            }
        },
        'active_increment_request': {'state': '20190101-101500', 'base_state': 'S1', 'human_id': 'inc base S1'},
        'increment_queue': [],
        'concat_queue': [],
        'post_process_queue': [],
        'reglue_queue': [],
        'reglue_states': ['S1']
    }
    settings = {'state_path': 'states', 'reglue_period': 2}
    factory = fake_factory.FakeFactory(settings, data)
    pipeline = increment.IncrementPipeline(factory)
    pipeline.finish()

    assert len(data['concat_queue']) == 0

    assert len(data['post_process_queue']) == 1
    post_request = data['post_process_queue'][-1]
    assert post_request['state'] == '20190101-101500'

    assert len(data['reglue_queue']) == 1
    reglue_request = data['reglue_queue'][-1]
    assert reglue_request['source_state'] == '20190101-101500'
    assert reglue_request['target_state'] == '20190101-101501'
    assert reglue_request['states'] == ['S1', '20190101-101500']

    assert len(data['reglue_states']) == 0

    assert data['active_increment_request'] is None

    assert factory.get_trash().is_empty()


def test_finish_auto_commit():
    data = {
        'branches': {
            'production': {
                'base_state': 'S0',
                'full_list': ['S1', 'S2']
            },
            'latest': {
                'base_state': 'S1',
                'full_list': ['S2', 'S3']
            }
        },
        'active_increment_request': {'state': 'S2', 'base_state': 'S1', 'human_id': 'inc S2 base S1'},
        'increment_queue': [],
        'post_process_queue': [],
        'reglue_queue': [],
        'reglue_states': []
    }
    settings = {'state_path': 'states', 'auto_commit_latest_base': True}
    factory = fake_factory.FakeFactory(settings, data)
    pipeline = increment.IncrementPipeline(factory)
    pipeline.finish()

    assert data['branches']['latest']['base_state'] == 'S2'
    assert data['branches']['latest']['full_list'] == ['S3']
    assert data['branches']['production']['base_state'] == 'S2'
    assert data['branches']['production']['full_list'] == ['S3']

    assert len(data['post_process_queue']) == 1
    post_request = data['post_process_queue'][-1]
    assert post_request['state'] == 'S2'

    assert len(data['reglue_queue']) == 0
    assert len(data['reglue_states']) == 1
    assert data['reglue_states'][0] == 'S2'

    assert data['active_increment_request'] is None

    assert factory.get_trash().is_empty()
