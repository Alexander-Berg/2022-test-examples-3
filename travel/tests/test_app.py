# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

try:
    from unistat_aggregator.app import MetricsStorage
except ImportError:
    from travel.library.docker.tools.unistat_aggregator.unistat_aggregator.app import MetricsStorage


@pytest.fixture
def storage():
    return MetricsStorage()


def test_one_metric(storage):
    storage.add({
        'name': 'foo_xxxx',
        'tags': {
            'itype': 'someitype',
            'ctype': 'ctype10',
            '_priority': 22
        },
        'ttl': 30,
        'val': 3
    })
    assert storage.metrics_accumulator == {
        'foo_xxxx': 3
    }


def test_batch_metrics(storage):
    storage.add({
        'tags': {
            'itype': 'someitype',
            'ctype': 'ctype10',
            '_priority': 22
        },
        'ttl': 30,
        'values': [{
            'name': 'sig1_xxxx',
            'val': 1
        }, {
            'name': 'sig2_nnnn',
            'val': 2
        }]
    })
    assert storage.metrics_accumulator == {
        'sig1_xxxx': 1,
        'sig2_nnnn': 2
    }


def test_histogram(storage):
    storage.add({
        'tags': {
            'itype': 'someitype',
            'ctype': 'ctype10',
            '_priority': 22
        },
        'ttl': 30,
        'values': [{
            'name': 'sig1_ahhh',
            'val': [[0, 1]]
        }, {
            'name': 'sig1_ahhh',
            'val': [[0, 2], [2, 4], [6, 8]]
        }]
    })
    assert storage.metrics_accumulator == {
        'sig1_ahhh': {0: 3, 2: 4, 6: 8}
    }


def test_different_types(storage):
    storage.add({
        'tags': {
            'itype': 'someitype',
            'ctype': 'ctype10',
            '_priority': 22
        },
        'ttl': 30,
        'values': [{
            'name': 'sig1_ahhh',
            'val': [[0, 1]]
        }, {
            'name': 'sig1_ahhh',
            'val': [[0, 2], [2, 4], [6, 8]]
        }, {
            'name': 'sig2_xxxx',
            'val': 1
        }, {
            'name': 'sig2_xxxx',
            'val': 2
        }]
    })
    assert storage.metrics_accumulator == {
        'sig1_ahhh': {0: 3, 2: 4, 6: 8},
        'sig2_xxxx': 2
    }


def test_dump(storage):
    storage.add({
        'tags': {
            'itype': 'someitype',
            'ctype': 'ctype10',
            '_priority': 22
        },
        'ttl': 30,
        'values': [{
            'name': 'sig1_ahhh',
            'val': [[0, 1]]
        }, {
            'name': 'sig1_ahhh',
            'val': [[0, 2], [2, 4], [6, 8]]
        }, {
            'name': 'sig2_xxxx',
            'val': 1
        }, {
            'name': 'sig2_xxxx',
            'val': 2
        }]
    })
    assert sorted(storage.dump()) == [
        ['sig1_ahhh', [(0, 3), (2, 4), (6, 8)]],
        ['sig2_xxxx', 2]
    ]
