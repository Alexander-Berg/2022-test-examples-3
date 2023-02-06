from StringIO import StringIO

import pytest
import yatest.common


@pytest.fixture(scope='module')
def fixtures_dir():  # type: () -> unicode
    return yatest.common.source_path('market/sre/tools/logradaptor/tests/fixtures')


@pytest.fixture
def logrotate_status(fixtures_dir):
    # type: (unicode) -> StringIO
    content = (
        'logrotate state -- version 2',
        '"{}/logs/nginx/error.log" 2018-4-10-0:15:43',
        '"{}/logs/push-client/*.log" 2018-3-14-18:0:0',
        '"{}/logs/nginx/content-api-access-tskv.log" 2018-4-10-0:15:43',
        '"{}/logs/nginx/content-api-access.log" 2018-4-10-0:15:43',
        '"{}/logs/push-client/market_health.log" 2018-4-10-0:15:43',
    )
    return StringIO('\n'.join(l.format(fixtures_dir) for l in content))


@pytest.fixture
def logrotate_status_file_paths(fixtures_dir):
    file_paths = [
        '{}/logs/push-client/market_health.log',
        '{}/logs/nginx/error.log',
        '{}/logs/nginx/content-api-access.log',
        '{}/logs/nginx/content-api-access-tskv.log',
    ]
    return [f.format(fixtures_dir) for f in file_paths]


@pytest.fixture
def logrotate_status_file_paths_with_gz(fixtures_dir):
    file_paths = [
        '{}/logs/push-client/market_health.log',
        '{}/logs/nginx/content-api-access-tskv.log.gz',
    ]
    return [f.format(fixtures_dir) for f in file_paths]


@pytest.fixture
def gz_file_paths(fixtures_dir):
    file_paths = [
        '{}/logs/push-client/market_health.log.2018-04-06.gz',
        '{}/logs/nginx/content-api-access.log.2018-04-06.gz',
    ]
    return [f.format(fixtures_dir) for f in file_paths]


@pytest.fixture
def improperly_compressed_gz_files(fixtures_dir):
    file_paths = [
        '{}/logs/nginx/content-api-access.log.2018-04-06.gz',
    ]
    return [f.format(fixtures_dir) for f in file_paths]
