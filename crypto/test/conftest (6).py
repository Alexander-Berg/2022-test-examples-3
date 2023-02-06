import os

import pytest
import yatest.common

from crypta.lib.python import (
    templater,
    time_utils,
)


pytest_plugins = [
    'crypta.lib.python.test_utils.fixtures',
    'crypta.lib.python.yql.test_helpers.fixtures',
    'crypta.lib.python.yt.test_helpers.fixtures',
]


@pytest.fixture
def frozen_time():
    result = '1650000000'
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = result
    yield result


def render_config_file(template_file, local_yt):
    config_file_path = yatest.common.test_output_path('config.yaml')

    templater.render_file(
        yatest.common.source_path(template_file),
        config_file_path,
        {
            'environment': 'qa',
            'yt_proxy': local_yt.get_server(),
            'geocube_table_names': ['maps'],
            'matching_yandexuids': ['yandexuid'],
            'matching_devids': ['gaid'],
        },
    )
    return config_file_path


@pytest.fixture(scope='function')
def config_file(local_yt):
    return render_config_file(
        'crypta/affinitive_geo/services/orgvisits/bundle/config.yaml',
        local_yt,
    )
