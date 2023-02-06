import hashlib
import pytest
import shutil
from pathlib2 import Path

from search.geo.tools.production.downloader.config import CommonOptions
from search.geo.tools.production.downloader.downloader import run, ChecksumError
from search.geo.tools.production.downloader.sky import SkynetError

from .mocks import (
    FakeSandboxChecker,
    FakeSkynetFetcher,
    sky_share
)


def test_update():
    sb_checker = FakeSandboxChecker()
    sky_fetcher = FakeSkynetFetcher()
    opts = CommonOptions()

    files_config = [{
        'resource': 'MAPS_DATABASE_SLOW_FEATURES',
        'path': './test_update',
    }]

    run(opts, files_config, sb_checker, sky_fetcher)
    assert Path('test_update').is_dir()

    sb_checker.add_resource({
        'type': 'MAPS_DATABASE_SLOW_FEATURES',
        'skynet_id': sky_share({
            'slow_features.mms': 'v1'
        })
    })
    run(opts, files_config, sb_checker, sky_fetcher)
    assert Path('test_update/slow_features.mms').is_file()
    assert Path('test_update/slow_features.mms').read_text() == 'v1'
    assert not Path('test_update/.complete').is_file()

    sb_checker.add_resource({
        'type': 'MAPS_DATABASE_SLOW_FEATURES',
        'skynet_id': sky_share({
            'slow_features.mms': 'v2'
        })
    })
    run(opts, files_config, sb_checker, sky_fetcher)
    assert Path('test_update/slow_features.mms').read_text() == 'v2'


def test_glob():
    sb_checker = FakeSandboxChecker()
    sky_fetcher = FakeSkynetFetcher()
    opts = CommonOptions()

    files_config = [{
        'resource': 'MAPS_DATABASE_SLOW_FEATURES',
        'path': './test_glob',
        'wildcards': '*/*/*',
    }]

    sb_checker.add_resource({
        'type': 'MAPS_DATABASE_SLOW_FEATURES',
        'skynet_id': sky_share({
            'index/0001/slow_features.mms.0': 'shard0',
            'index/0001/slow_features.mms.1': 'shard1',
            'index/0001/slow_features.mms.2': 'shard2',
        })
    })
    run(opts, files_config, sb_checker, sky_fetcher)
    assert Path('test_glob/slow_features.mms.0').is_file()
    assert Path('test_glob/slow_features.mms.1').is_file()
    assert Path('test_glob/slow_features.mms.2').is_file()


def test_sharded():
    sb_checker = FakeSandboxChecker()
    sky_fetcher = FakeSkynetFetcher()
    opts = CommonOptions()
    opts.shard_id = 15

    files_config = [{
        'resource': 'MAPS_DATABASE_SLOW_FEATURES_SHARD',
        'path': './test_sharded',
        'sharded': True
    }]

    for i in range(18):
        sb_checker.add_resource({
            'type': 'MAPS_DATABASE_SLOW_FEATURES_SHARD',
            'skynet_id': sky_share({
                'slow_features.mms.{}'.format(i): 'shard{}'.format(i),
            }),
            'attributes': {'shard_id': str(i)}
        })

    run(opts, files_config, sb_checker, sky_fetcher)
    assert not Path('test_sharded/slow_features.mms.0').is_file()
    assert Path('test_sharded/slow_features.mms.15').is_file()
    assert Path('test_sharded/slow_features.mms.15').read_text() == 'shard15'


def test_broken_skynet():
    sb_checker = FakeSandboxChecker()
    sky_fetcher = FakeSkynetFetcher()
    opts = CommonOptions()
    opts.shard_id = 15

    files_config = [{
        'resource': 'MAPS_DATABASE_SLOW_FEATURES',
        'path': './test_broken_skynet_1',
    }, {
        'resource': 'PLAIN_TEXT',
        'path': './test_broken_skynet_2',
    }]

    sb_checker.add_resource({
        'type': 'MAPS_DATABASE_SLOW_FEATURES',
        'skynet_id': 'this_rbtorrent_is_invalid',
    })

    with pytest.raises(SkynetError):
        run(opts, files_config, sb_checker, sky_fetcher)
    with pytest.raises(SkynetError):
        run(opts, files_config, sb_checker, sky_fetcher)
    run(opts, files_config, sb_checker, sky_fetcher, stop_on_fail=False)

    sb_checker.add_resource({
        'type': 'PLAIN_TEXT',
        'skynet_id': sky_share({'hello.txt': 'world'}),
    })
    run(opts, files_config, sb_checker, sky_fetcher, stop_on_fail=False)
    assert Path('test_broken_skynet_2/hello.txt').read_text() == 'world'


def test_data_verification():
    sb_checker = FakeSandboxChecker()
    sky_fetcher = FakeSkynetFetcher()
    opts = CommonOptions()
    opts.shard_id = 15

    files_config = [{
        'resource': 'MAPS_DATABASE_SLOW_FEATURES',
        'path': './test_broken_file',
        'verify_checksum': True,
    }]

    sb_checker.add_resource({
        'type': 'MAPS_DATABASE_SLOW_FEATURES',
        'skynet_id': sky_share({'hello.txt': 'world'}),
        'md5': hashlib.md5('world').hexdigest(),
        'file_name': 'hello.txt',
    })

    run(opts, files_config, sb_checker, sky_fetcher)
    assert Path('test_broken_file/hello.txt').read_text() == 'world'

    shutil.rmtree('test_broken_file')

    sky_fetcher.corrupt_data = True
    with pytest.raises(ChecksumError):
        run(opts, files_config, sb_checker, sky_fetcher)
