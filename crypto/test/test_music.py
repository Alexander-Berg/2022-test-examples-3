import yatest.common

import pytest

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.runners.segments.lib.constructor_segments.daily_rule_processors import music


def get_albums_schema():
    return schema_utils.yt_schema_from_dict({
        "album_id": "int64",
        "main_artist": "any",
    })


def get_tracks_schema():
    return schema_utils.yt_schema_from_dict({
        "track_id": "int64",
        "main_artist": "any",
    })


def get_events_schema():
    return schema_utils.yt_schema_from_dict({
        "uid": "int64",
        "item": "any",
    })


@pytest.fixture
def liked_artist_id_to_rule_lab_id():
    return {
        1: {"rule-1", "rule-2"},
        2: {"rule-2", "rule-3"},
        3: {"rule-4"},
    }


@pytest.fixture
def rule_revision_ids():
    return {1, 2, 3, 4}


@pytest.fixture
def task(date, rule_revision_ids, liked_artist_id_to_rule_lab_id):
    return music.GetStandardSegmentsByMusicLikes(
        date=date,
        liked_artist_id_to_rule_lab_id=liked_artist_id_to_rule_lab_id,
        rule_revision_ids=rule_revision_ids,
    )


def test_music_likes(clean_local_yt, patched_config, task):
    return tests.yt_test_func(
        yt_client=clean_local_yt.get_yt_client(),
        func=task.run,
        data_path=yatest.common.test_source_path('data'),
        input_tables=[
            (tables.get_yson_table_with_schema('albums.yson', task.input()["album_metadata"].table, get_albums_schema()), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema('events.yson', task.input()["like_events"].table, get_events_schema()), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema('tracks.yson', task.input()["track_metadata"].table, get_tracks_schema()), tests.TableIsNotChanged()),
        ],
        output_tables=[
            (tables.YsonTable(
                'rule_ids.yson',
                task.output().table,
                yson_format='pretty',
                on_read=tables.OnRead(sort_by=["id", "id_type"]),
            ), (tests.Diff())),
        ]
    )
