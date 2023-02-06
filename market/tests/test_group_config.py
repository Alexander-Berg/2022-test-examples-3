# -*- coding: utf-8 -*-
import copy
import json

import pytest

from async_publishing.group_config import (
    GroupConfig,
    AsyncPublishingMode,
    TwoPhaseReloadMetaConfig,
    TwoPhaseReloadMode
)
from async_publishing.generation_meta import GenerationMeta, PackageMeta

from hamcrest import (
    assert_that,
    equal_to,
    has_properties,
    none,
)


@pytest.fixture()
def group_config_json():
    return copy.deepcopy({
        "simultaneous_restart": 2,
        "failures_threshold": 3,
        "reload_timeout": 650,
        "async_publishing": "enabled",
        "hosts": {
            "0": [
                {
                    "key": "key:iva-0001",
                    "fqdn": "iva-0001",
                    "port": 1111,
                    "datacenter": "iva",
                },
                {
                    "key": "key:iva-0002",
                    "fqdn": "iva-0002",
                    "port": 2222,
                    "datacenter": "iva",
                }
            ],
            "1": [
                {
                    "key": "key:sas-1001",
                    "fqdn": "sas-1001",
                    "port": 3333,
                    "datacenter": "sas",
                },
                {
                    "key": "key:sas-1002",
                    "fqdn": "sas-1002",
                    "port": 4444,
                    "datacenter": "sas",
                }
            ]
        },
        "min_alive": {
            "sas": 1,
            "iva": 0,
        },
        "full_generation": {
            "name": "20180101_1010",
            "torrent_server_host": "mi01h.market.yandex.net",
            "torrent_server_port": 80,
        },
        "packages": {
            'testing': [
                {
                    "name": "report",
                    "torrent_server_host": "mi01h.market.yandex.net",
                    "torrent_server_port": 80,
                    "version": "2018.3.85.0"
                },
                {
                    "name": "dsm",
                    "torrent_server_host": "mi01h.market.yandex.net",
                    "torrent_server_port": 80,
                    "version": "0.3673241"
                }
            ]
        },
        "two_phase_reload": 'enabled',
        "first_phase_nclusters": 1,
        "first_phase_num_candidates": 2,
        "two_phase_reload_meta": {
            "enabled": True,
            "first_phase_dc": ["man", "vla"],
        },
    })


def test_parse_group_config_without_min_alive_and_dc(group_config_json):
    del group_config_json['min_alive']
    for hosts in group_config_json['hosts'].values():
        for host in hosts:
            del host['datacenter']
    group_config = GroupConfig.from_str(json.dumps(group_config_json))
    assert_that(
        group_config,
        has_properties({
            "simultaneous_restart": 2,
            "failures_threshold": 3,
            "reload_timeout": 650,
            "async_publishing": AsyncPublishingMode.enabled,
            "hosts": {
                GroupConfig.Host('iva-0001', 1111, '0', None, 'key:iva-0001'),
                GroupConfig.Host('iva-0002', 2222, '0', None, 'key:iva-0002'),
                GroupConfig.Host('sas-1001', 3333, '1', None, 'key:sas-1001'),
                GroupConfig.Host('sas-1002', 4444, '1', None, 'key:sas-1002')
            },
            "min_alive": {},
            "two_phase_reload": TwoPhaseReloadMode.enabled,
            "first_phase_nclusters": 1,
            "first_phase_num_candidates": 2,
        }))


def test_parse_group_config(group_config_json):
    group_config = GroupConfig.from_str(json.dumps(group_config_json))
    assert_that(
        group_config,
        has_properties({
            "simultaneous_restart": 2,
            "failures_threshold": 3,
            "reload_timeout": 650,
            "async_publishing": AsyncPublishingMode.enabled,
            "hosts": {
                GroupConfig.Host('iva-0001', 1111, '0', 'iva', 'key:iva-0001'),
                GroupConfig.Host('iva-0002', 2222, '0', 'iva', 'key:iva-0002'),
                GroupConfig.Host('sas-1001', 3333, '1', 'sas', 'key:sas-1001'),
                GroupConfig.Host('sas-1002', 4444, '1', 'sas', 'key:sas-1002'),
            },
            "min_alive": {'sas': 1, 'iva': 0},
            "two_phase_reload": TwoPhaseReloadMode.enabled,
            "first_phase_nclusters": 1,
            "first_phase_num_candidates": 2,
            "full_generation": GenerationMeta(name="20180101_1010", torrent_server_host="mi01h.market.yandex.net"),
            "packages": {
                'testing': [
                    PackageMeta(name="report", version='2018.3.85.0', torrent_server_host="mi01h.market.yandex.net"),
                    PackageMeta(name="dsm", version='0.3673241', torrent_server_host="mi01h.market.yandex.net")
                    ]
                },
            "two_phase_reload_meta": TwoPhaseReloadMetaConfig(enabled=True, first_phase_dc=['vla', 'man']),
        }))


def test_parse_group_config_without_full_generation(group_config_json):
    del group_config_json['full_generation']
    group_config = GroupConfig.from_str(json.dumps(group_config_json))

    assert_that(group_config.full_generation, none())


def test_parse_group_config_without_packages(group_config_json):
    del group_config_json['packages']
    group_config = GroupConfig.from_str(json.dumps(group_config_json))

    assert_that(group_config.packages, none())


def test_parse_group_config_without_two_phase_reload(group_config_json):
    del group_config_json['two_phase_reload']
    group_config = GroupConfig.from_str(json.dumps(group_config_json))

    assert_that(group_config.two_phase_reload, equal_to(TwoPhaseReloadMode.disabled))


def test_setialize_group_config():
    meta = GroupConfig(
        simultaneous_restart=2,
        failures_threshold=1,
        reload_timeout=100,
        async_publishing=AsyncPublishingMode.enabled,
        hosts=[
            GroupConfig.Host(key='key:iva-001', fqdn='iva-001', datacenter='iva', port=1111, cluster_id='0'),
            GroupConfig.Host(key='key:iva-002', fqdn='iva-002', datacenter='iva', port=1111, cluster_id='0'),
            GroupConfig.Host(key='key:vla-001', fqdn='vla-001', datacenter='vla', port=1111, cluster_id='1')
        ],
        min_alive={
            'iva': 1,
            'vla': 1,
        },
        full_generation_meta=GenerationMeta(name='20180730_1543', torrent_server_host='mi01ht.market.yandex.net', not_for_publishing=False),
        packages_meta={
            'testing': [
                PackageMeta(name='report', version='2014.44.31.3', torrent_server_host='somehost.market.yandex.net'),
                PackageMeta(name='dsm', version='0.12345', torrent_server_host='somehost.market.yandex.net'),
            ]
        },
        two_phase_reload_meta=TwoPhaseReloadMetaConfig(enabled=True, first_phase_dc=['sas', 'man']),
    )

    assert_that(meta, equal_to(GroupConfig.from_str(str(meta))))
