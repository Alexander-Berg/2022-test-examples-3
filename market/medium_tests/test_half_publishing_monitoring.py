# -*- coding: utf-8 -*-

import json
import os
import pytest
import six

from market.idx.pylibrary.mindexer_core.zkmaster.zkmaster import ZkMaster
from async_publishing import (
    Client as AsyncPublisherClient,
    AsyncPublishingMode,
    GroupConfig,
    GenerationMeta
)


def is_half_publishing_ok(mindexer_clt):
    monitoring_result = mindexer_clt.execute('check_half_publishing')
    return (
        monitoring_result.exit_code == 0 and
        six.ensure_str(monitoring_result.std_out).startswith('0;')
    )


def is_half_publishing_crit(mindexer_clt):
    monitoring_result = mindexer_clt.execute('check_half_publishing')
    return (
        monitoring_result.exit_code == 0 and
        six.ensure_str(monitoring_result.std_out).startswith('2;')
    )


@pytest.fixture()
def mindexer_clt(mindexer_clt, request, reusable_mysql, reusable_zk):
    """Добавляем поколения: полное - 20180101_0101, половинчатое - 20180101_0201
       Добавляем в конфиг редуктора 1 группу
    """

    mindexer_clt.env_type = 'production'
    mindexer_clt.make_local_config({
        ('blue', 'indexation'): 'true',
    })
    mindexer_clt.add_generation_to_super('20180101_0101')
    mindexer_clt.add_generation_to_super('20180101_0201', half=True)
    os.makedirs(os.path.dirname(mindexer_clt.config.reductor_config_path))
    with open(mindexer_clt.config.reductor_config_path, 'w') as f:
        s = json.dumps({
            'dcgroups': {
                'group_name': {}
            }
        })
        f.write(s)
    return mindexer_clt


def test_crit_on_half_full_generation(mindexer_clt, reusable_zk):
    assert is_half_publishing_ok(mindexer_clt)

    mindexer_clt.execute('async_publish_full', '20180101_0101')
    assert is_half_publishing_ok(mindexer_clt)

    mindexer_clt.execute('async_publish_full', '20180101_0201', '--force-half')
    assert is_half_publishing_crit(mindexer_clt)


def test_crit_on_half_group_config_generation(mindexer_clt, reusable_zk):
    assert is_half_publishing_ok(mindexer_clt)

    def write_group_config(group, generation, cowboy=False):
        with ZkMaster(mindexer_clt.config) as zk:
            client = AsyncPublisherClient(
                zk_client=zk.raw_client,
                prefix=mindexer_clt.config.async_publish_root_prefix,
                generations_prefix=mindexer_clt.config.async_publish_generations_prefix
            )
            additional_params = {}
            if cowboy:
                additional_params['config'] = AsyncPublisherClient.cowboy_config_key
            client.write_group_config(
                group,
                GroupConfig(
                    simultaneous_restart=1,
                    failures_threshold=1,
                    hosts=[],
                    reload_timeout=1,
                    async_publishing=AsyncPublishingMode.enabled,
                    min_alive=1,
                    full_generation_meta=GenerationMeta(name=generation, not_for_publishing=False)
                ),
                **additional_params
            )

    for cowboy in (False, True):
        write_group_config('group_name', '20180101_0101', cowboy=cowboy)
        assert is_half_publishing_ok(mindexer_clt)

    write_group_config('group_name', '20180101_0201', cowboy=False)
    assert is_half_publishing_crit(mindexer_clt)

    write_group_config('group_name', '20180101_0101', cowboy=False)
    assert is_half_publishing_ok(mindexer_clt)
    write_group_config('group_name', '20180101_0201', cowboy=True)
    assert is_half_publishing_crit(mindexer_clt)
