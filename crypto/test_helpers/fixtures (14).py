import os

import mock
import pytest
import yatest.common

from crypta.lib.python import secret_manager
from crypta.profile.utils.config import (
    config,
    secrets,
)
from crypta.profile.utils import yt_utils


pytest_plugins = [
    'crypta.lib.python.juggler.test_utils.fixtures',
    'crypta.lib.python.yql.test_helpers.fixtures',
]


@pytest.fixture
def patched_config(local_yt, local_yt_and_yql_env, mock_juggler_server):
    config.CRYPTA_YT_PROXY = local_yt.get_server()

    os.environ.update(local_yt_and_yql_env)

    old_root = config.LOCAL_STORAGE_DIRECTORY
    new_root = yatest.common.test_output_path('storage')

    for const in config.__dict__:
        if isinstance(config.__dict__[const], basestring):
            config.__dict__[const] = config.__dict__[const].replace(old_root, new_root)

    secrets.SECRETS_SINGLETON = secret_manager.SecretManager(
        {},
        secrets_by_name={
            'YT_TOKEN': 'TOKEN',
            'YQL_TOKEN': 'TOKEN',
            'CRYPTA_PROFILE_TVM_SECRET': '',
        },
    )
    yt_utils.yt_instances.clear()

    config.JUGGLER_API_URL = mock_juggler_server.events_url

    with mock.patch('crypta.profile.utils.yt_utils.get_config', return_value={"spec_defaults": {"pool": "default"}}):
        yield config


@pytest.fixture
def lb_patched_config(patched_config, logbroker_config):
    patched_config.CRYPTA_PROFILES_TOPIC_NAME = logbroker_config.topic
    patched_config.LOGBROKER_HOSTNAME = logbroker_config.host
    patched_config.LOGBROKER_PORT = logbroker_config.port
    patched_config.LOGBROKER_PROFILES_JSON_LOG_PARTITIONS_NUMBER = 1
    patched_config.LOGBROKER_TESTING_SAMPLING_RATE = 1.
    yield patched_config
