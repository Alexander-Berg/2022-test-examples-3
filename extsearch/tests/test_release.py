from extsearch.audio.deepdive.tools.deep_dive_release import release

import yatest.common as yc
from mapreduce.yt.python.yt_stuff import YtConfig
from unittest.mock import patch
import pytest
import uuid


CYPRESS_DIR = 'extsearch/audio/deepdive/tools/deep_dive_release/tests/cypress_dir'


@pytest.fixture(scope='module')
def yt_config(request):
    return YtConfig(
        local_cypress_dir=yc.source_path(CYPRESS_DIR)
    )


def make_uuid(init_value=0):
    uuid_count = init_value

    def inner():
        nonlocal uuid_count
        uuid_count += 1
        return uuid.UUID(int=uuid_count)

    return inner


def test_detect_films_by_prefix():
    finish_operation = {
        'id1': {'film_id': '100-1-1', 'uuid': 'id1', 'timestamp': 1},
        'id2': {'film_id': '100-1-1', 'uuid': 'id2', 'timestamp': 2},
        'id3': {'film_id': '200-1-1', 'uuid': 'id3', 'timestamp': 1},
        'id4': {'film_id': '200-1-1', 'uuid': 'id4', 'timestamp': 2},
        'id5': {'film_id': '200-1-2', 'uuid': 'id5', 'timestamp': 1},
    }
    assert release.detect_films_by_prefix('100', 'id1', finish_operation) == [('100-1-1', 'id1')]
    expected = sorted([
        ('200-1-1', 'id4'),
        ('200-1-2', 'id5')
    ])
    assert sorted(release.detect_films_by_prefix('200-1', None, finish_operation)) == expected


def test_finish_operations(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    return release.finish_operations(yt_client, '//data/control_table')


def test_get_inputs(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    expected = sorted([
        '//data/quarantine_ann_full',
        '//data/quarantine_ann/100-1-1_id1',
        '//data/quarantine_ann/100-1-2_id2',
    ])
    assert sorted(release.get_inputs(yt_client, '//data/quarantine_ann')) == expected


@patch('uuid.uuid4', make_uuid(2))
@patch('time.time', lambda: 1.0)
def test_release_items(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    with yt_client.Transaction():
        ctrl = release.release_items(
            yt_client,
            [
                ("100-1-1", "id1-100-1-1"),
                ("400", "id1-400"),
                ("403", "id1-403")
            ],
            '//data/quarantine_ann',
            '//data/merged',
            '//data/production',
            '//data/testing'
        )

    tables = {}
    tables['//data/merged'] = list(yt_client.read_table('//data/merged'))
    production_list = sorted(yt_client.list('//data/production', absolute=True))
    testing_list = sorted(yt_client.list('//data/testing', absolute=True))
    for t in production_list + testing_list:
        tables[t] = sorted(list(yt_client.read_table(t)), key=str)

    return {
        'tables': tables,
        'control': ctrl
    }


def test_add_to_released_list(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    with yt_client.Transaction():
        class Args:
            def __init__(self):
                self.testing_released_list = '//data/released_testing'
                self.production_released_list = '//data/released_production'
                self.testing_dir = '//data/testing'
                self.production_dir = '//data/production'

        args = Args()
        control = [
            {'film_id': '100', 'uuid': 'id-100'},
            {'film_id': '200', 'uuid': 'id-200'}
        ]

        release.add_to_released_list(yt_client, args, control)

    return [
        sorted(yt_client.read_table('//data/released_testing'), key=str),
        sorted(yt_client.read_table('//data/released_production'), key=str),
    ]
