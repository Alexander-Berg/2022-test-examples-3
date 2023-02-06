import extsearch.ymusic.scripts.reindex.podcasts as podcasts

import pytest


@pytest.mark.parametrize('podcast_data,track_ids,expected_order,what', [
    ({}, [1, 5, 4, 3, 2], [5, 4, 3, 2, 1], "no episodes info"),
    (
        {
            'podcast_episodes': [
                {'track_id': 1, 'episode_id': 1, 'pub_date': '2018-11-02T10:37:36.000+03:00'},
                {'track_id': 2, 'episode_id': 2, 'pub_date': '2018-10-26T16:12:17.000+03:00'},
                {'track_id': 3, 'episode_id': 3, 'pub_date': '2018-11-30T16:37:27.000+03:00'},
                {'track_id': 4, 'episode_id': 4, 'pub_date': '2018-11-23T10:41:57.000+03:00'},
                {'track_id': 5, 'episode_id': 5, 'pub_date': '2018-11-09T12:32:11.000+03:00'},
            ]
        },
        [1, 2, 3, 4, 5],
        [3, 4, 5, 1, 2],
        "all episodes info",
    ),
    (
        {
            'podcast_episodes': [
                {'track_id': 1, 'episode_id': 1, 'pub_date': None},
                {'track_id': 2, 'episode_id': 2, 'pub_date': '2018-10-26T16:12:17.000+03:00'},
                {'track_id': 3, 'episode_id': 3, 'pub_date': '2018-11-30T16:37:27.000+03:00'},
                {'track_id': 4, 'episode_id': 4},
                {'track_id': 5, 'episode_id': 5, 'pub_date': '2018-11-09T12:32:11.000+03:00'},
            ]
        },
        [1, 2, 3, 4, 5],
        [3, 5, 2, 4, 1],
        "missing some pub dates",
    ),
    (
        {
            'podcast_episodes': [
                {'track_id': 1, 'episode_id': 1, 'pub_date': None},
                {'track_id': 2, 'episode_id': 2, 'pub_date': '2018-10-26T16:12:17.000+03:00'},
                {'track_id': 4, 'episode_id': 4, 'pub_date': '2018-11-23T10:41:57.000+03:00'},
                {'track_id': 5, 'episode_id': 5},
                {'track_id': 6, 'episode_id': 6, 'pub_date': '2020-12-23T10:41:57.000+03:00'},
                {'track_id': 7, 'episode_id': 7, 'pub_date': '2019-12-23T10:41:57.000+03:00'},
            ]
        },
        [1, 2, 3, 4, 5],
        [3, 4, 2, 5, 1],
        "missing some episodes and some pub dates"
    ),
    (
        {
            'podcast_episodes': [
                {'track_id': 2, 'episode_id': 2, 'pub_date': '2018-10-26T16:12:17.000+03:00'},
                {'track_id': 3, 'episode_id': 3, 'pub_date': '2018-11-30T16:37:27.000+03:00'},
                {'track_id': 4, 'episode_id': 4, 'pub_date': '2018-11-23T10:41:57.000+03:00'},
            ]
        },
        [1, 2, 3, 4, 5],
        [5, 1, 3, 4, 2],
        "missing some episodes"
    )
])
def test__podcast__get_sorted_episodes_track_ids(podcast_data, track_ids, expected_order, what):
    podcast = podcasts.Podcast(podcast_data)
    assert_track_ids_reordered(podcast, track_ids, expected_order, what)


def assert_track_ids_reordered(podcast, track_ids, expected_order, what):
    assert podcast.get_sorted_episodes_track_ids(track_ids) == expected_order, what
