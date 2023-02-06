#!/usr/bin/env python
# -*- coding: utf-8 -*-

import pytest
import mock

from context import ReductorForTests
from reductor.reductor import BACKCTLD_PORT


CONFIG = {
    'dcgroups': {
        'market_search-testing-trusty@dummy_dc': {
            "simultaneous_restart": 10,
            "hosts": {
                "msh04et.market.yandex.net": {
                    "redundancy": 1,
                    "name": "msh04et.market.yandex.net",
                    "service": "updater",
                    "cluster": 0,
                    "dists": {
                        "qindex-delta-part-0": {},
                        "qindex-delta-part-8": {},
                        "qpipe-delta-part-0": {},
                        "qpipe-delta-part-8": {},
                        "qbid-delta-part-0": {},
                    }
                },
                "msh01blue.market.yandex.net": {
                    "redundancy": 1,
                    "name": "msh01blue.market.yandex.net",
                    "service": "updater",
                    "cluster": 1,
                    "dists": {
                        "qpipe-delta-part-blue": {},
                        "qbid-delta-part-0": {},
                    }
                }
            }
        },
        'market_search-bag-group@dummy_dc': {
            "is_always_successful": True,
            "simultaneous_restart": 1,
            "hosts": {
                "bad01ht.market.yandex.net": {
                    "redundancy": 1,
                    "name": "bad01ht.market.yandex.net",
                    "service": "updater",
                    "cluster": 0,
                    "dists": {
                        "qindex-delta-part-0": {},
                        "qindex-delta-part-8": {},
                        "qpipe-delta-part-0": {},
                        "qpipe-delta-part-8": {},
                        "qbid-delta-part-0": {},
                    }
                },
                "bad02ht.market.yandex.net": {
                    "redundancy": 1,
                    "name": "bad02ht.market.yandex.net",
                    "service": "updater",
                    "cluster": 0,
                    "dists": {
                        "qindex-delta-part-1": {},
                        "qindex-delta-part-9": {},
                        "qpipe-delta-part-1": {},
                        "qpipe-delta-part-9": {},
                        "qbid-delta-part-0": {},
                    }
                },
            }
        }
    }
}

BACKEND_WHITE_KEY = 'msh04et.market.yandex.net@dummy_dc:{}'.format(BACKCTLD_PORT)
BACKEND_BAD1_KEY = 'bad01ht.market.yandex.net@dummy_dc:{}'.format(BACKCTLD_PORT)
BACKEND_BAD2_KEY = 'bad02ht.market.yandex.net@dummy_dc:{}'.format(BACKCTLD_PORT)
BACKEND_BLUE_KEY = 'msh01blue.market.yandex.net@dummy_dc:{}'.format(BACKCTLD_PORT)


@pytest.fixture()
def good_reductor():
    reductor = ReductorForTests(CONFIG)
    for key, mock_func in reductor.backend_mocks.iteritems():
        if key.startswith('bad'):
            mock_func.do.return_value = '! time out'
        else:
            mock_func.do.return_value = 'ok'
    return reductor


def update_service(reductor, pipeline, url, download_only):
    cmd = '{} {}'.format(pipeline, url)
    if download_only:
        cmd += ' --download-only'
    reductor.do_update_service(cmd)


@pytest.mark.parametrize('pipeline, url, download_only', [
    ('fulfillment', 'http://localhost/fulfillment', True),
    ('fulfillment', 'http://localhost/fulfillment', False),
    ('market_dynamic', 'http://localhost/market_dynamic', True),
    ('market_dynamic', 'http://localhost/market_dynamic', False),
    ('vendor_model_bids_cutoff', 'http://localhost/vendor_model_bids_cutoff', True),
    ('vendor_model_bids_cutoff', 'http://localhost/vendor_model_bids_cutoff', False),
])
def test_update_service_no_shards(good_reductor, pipeline, url, download_only):
    update_service(good_reductor, pipeline, url, download_only)
    expected_cmd = 'updater update_service {} {}{}'.format(
        pipeline, url, ' --download-only' if download_only else '')
    good_reductor.backend_mocks[BACKEND_WHITE_KEY].do.assert_any_call(expected_cmd)


@pytest.mark.parametrize('pipeline, download_only, shards', [
    ('qindex', False, [0, 8]),
    ('qindex', True, [0, 8]),
    ('qbid', False, [0]),
    ('qbid', True, [0]),
    ('qpipe', True, [0, 8]),
    ('qpipe', False, [0, 8]),
])
def test_update_service_shards(good_reductor, pipeline, download_only, shards):
    update_service(good_reductor, pipeline, 'mds-url', download_only)
    expected_cmd = 'updater update_service {} mds-url{}'.format(
        pipeline, ' --download-only' if download_only else '')
    for shard in shards:
        expected_cmd += ' --shard {}'.format(shard)
    good_reductor.backend_mocks[BACKEND_WHITE_KEY].do.assert_any_call(expected_cmd)


@pytest.mark.parametrize('pipeline, bad1_shards, bad2_shards', [
    ('qbid', [0], [0]),
    ('qpipe', [0, 8], [1, 9]),
])
def test_update_service_stops_on_first_failure_in_cluster(good_reductor, pipeline, bad1_shards, bad2_shards):
    """
    В кластера 2 дохлых бекенда, проверяем, что вызываем команду на обновление только на одном из них.
    На втором мы не должны даже пытаться ее звать, для экономии времени.
    """
    update_service(good_reductor, pipeline, 'mds-url', False)
    expected_cmd1 = expected_cmd2 = 'updater update_service {} mds-url'.format(pipeline)
    for shard in bad1_shards:
        expected_cmd1 += ' --shard {}'.format(shard)
    for shard in bad2_shards:
        expected_cmd2 += ' --shard {}'.format(shard)
    # != тут следует читать как xor.
    # Мы не знаем с какого из бекендов начнем,но знаем, что должны поторогать только 1 из них
    assert (
        (mock.call(expected_cmd1) in good_reductor.backend_mocks[BACKEND_BAD1_KEY].do.mock_calls) !=
        (mock.call(expected_cmd2) in good_reductor.backend_mocks[BACKEND_BAD2_KEY].do.mock_calls)
    )


@pytest.mark.parametrize('pipeline, download_only, shards', [
    ('qbid', False, [0]),
    ('qbid', True, [0]),
    ('qpipe', True, ['blue']),
    ('qpipe', False, ['blue']),
])
def test_update_service_blue_shards(good_reductor, pipeline, download_only, shards):
    update_service(good_reductor, pipeline, 'mds-url', download_only)
    expected_cmd = 'updater update_service {} mds-url{}'.format(
        pipeline, ' --download-only' if download_only else '')
    for shard in shards:
        expected_cmd += ' --shard {}'.format(shard)
    good_reductor.backend_mocks[BACKEND_BLUE_KEY].do.assert_any_call(expected_cmd)


@pytest.mark.parametrize('pipeline, generations', [
    ('qindex', ''),
    ('qindex', '20171010_101010'),
    ('qindex', '20171010_101010 20171010_101010'),
    ('qbid', ''),
    ('qbid', '20171010_101010'),
    ('qbid', '20171010_101010 20171010_101010'),
])
def test_remove(good_reductor, pipeline, generations):
    cmd = '{} {}'.format(pipeline, generations).strip()
    good_reductor.do_remove_mds_generations(cmd)
    expected_cmd = 'updater remove {} {}'.format(
        pipeline, generations).strip()
    call = mock.call(expected_cmd)
    if generations:
        assert call in good_reductor.backend_mocks[BACKEND_WHITE_KEY].do.mock_calls
    else:
        assert call not in good_reductor.backend_mocks[BACKEND_WHITE_KEY].do.mock_calls


@pytest.mark.parametrize('pipeline', ['qpromos'])
def test_rollback(good_reductor, pipeline):
    good_reductor.do_rollback_pipeline_data(pipeline)
    expected_cmd = 'updater rollback {}'.format(pipeline).strip()
    call = mock.call(expected_cmd)
    assert call in good_reductor.backend_mocks[BACKEND_WHITE_KEY].do.mock_calls
