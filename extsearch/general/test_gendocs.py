from __future__ import print_function

import datetime as dt
import pytest

import extsearch.ymusic.scripts.reindex.gendocs as gendocs


def test__track__fix_lyrics__removed():
    lyrics = 'a' * 60 * 1024
    track = gendocs.Track({'lyrics': [lyrics]})
    track.fix_lyrics()
    assert 'lyrics' not in track


def test__track__fix_lyrics__ok():
    lyrics = 'a' * 40 * 1024
    track = gendocs.Track({'lyrics': [lyrics]})
    track.fix_lyrics()
    assert track['lyrics'][0] == lyrics


def test__album__fix_album_type__fixed():
    album = gendocs.Album({
        'id': 123,
        'genre': [{'id': 'fairytales'}],
    })
    album.fix_album_type()
    assert album['albumType'] == 'fairy_tale'


def test__album__fix_album_type__not_fixed():
    album = gendocs.Album({
        'id': 123,
        'genre': [{'id': 'some genre'}],
        'albumType': 'some',
    })
    album.fix_album_type()
    assert album['albumType'] == 'some'


def test__popular_tracks__determine_common_album_types():
    popular_tracks = gendocs.PopularTracks({
        'tracks': {
            1: {
                'region-data': [
                    {'album': [1]}
                ],
            },
            2: {
                'region-data': [
                    {'album': [2]}
                ]
            },
        },
        'albums': {
            1: {'albumType': 'fairy_tale'},
            2: {'albumType': 'fairy_tale'},
        },
    }, include_album=False)
    assert popular_tracks.determine_common_album_types() == {'fairy_tale'}


def test__popular_tracks__determine_common_album_types__not_allowed_type():
    popular_tracks = gendocs.PopularTracks({
        'tracks': {
            1: {
                'region-data': [
                    {'album': [1]}
                ],
            },
            2: {
                'region-data': [
                    {'album': [2]}
                ]
            },
        },
        'albums': {
            1: {'albumType': 'unknown'},
            2: {'albumType': 'unknown'},
        },
    }, include_album=False)
    assert len(popular_tracks.determine_common_album_types()) == 0


def test__popular_tracks__determine_common_album_types__multiple_albums():
    popular_tracks = gendocs.PopularTracks({
        'tracks': {
            1: {
                'region-data': [
                    {'album': [1, 2]}
                ],
            },
            2: {
                'region-data': [
                    {'album': [3, 4]}
                ]
            },
        },
        'albums': {
            1: {'albumType': 'fairy_tale'},
            2: {'albumType': 'unknown'},
            3: {'albumType': 'fairy_tale'},
            4: {'albumType': 'other_type'},
        },
    }, include_album=False)
    assert popular_tracks.determine_common_album_types() == {'fairy_tale'}


@pytest.mark.parametrize("albums_freshness,expected_freshness", [
    ([0.0], 0.0),
    ([0.933], 0.933),
    ([0.7, 0.8], 0.7)
])
def test__compute_freshness__track(albums_freshness, expected_freshness):
    now = dt.datetime(2020, 8, 1)
    albums = {i: {'freshness': fr} for i, fr in enumerate(albums_freshness)}
    track = gendocs.Track({'albums': albums})
    assert_float_number_is(expected_freshness, track.compute_freshness(now))


@pytest.mark.parametrize("release_date,expected_freshness", [
    ("2020-08-01", 1.0),
    ("2020-07-26", 0.8),
    ("2019-08-01", 0.0),
    ("2020-08-30", 1.0),
])
def test__compute_freshness__album(release_date, expected_freshness):
    now = dt.datetime(2020, 8, 1)
    album = gendocs.Album({'release-date': release_date})
    assert_float_number_is(expected_freshness, album.compute_freshness(now))


@pytest.mark.parametrize("albums_freshness,expected_freshness", [
    ([0.0], 0.0),
    ([0.5, 0.7], 0.7),
    ([0.3], 0.3),
])
def test__compute_freshness__artist(albums_freshness, expected_freshness):
    now = dt.datetime(2020, 8, 1)
    albums = {i: {'freshness': fr} for i, fr in enumerate(albums_freshness)}
    artist = gendocs.Artist({'popular-tracks': {'albums': albums}})
    assert_float_number_is(expected_freshness, artist.compute_freshness(now))


@pytest.mark.parametrize("albums_freshness,expected_freshness", [
    ([0.0], 0.0),
    ([0.5, 0.7], 0.5),
    ([0.3], 0.3),
])
def test__compute_freshness__playlist(albums_freshness, expected_freshness):
    now = dt.datetime(2020, 8, 1)
    albums = {i: {'freshness': fr} for i, fr in enumerate(albums_freshness)}
    playlist = gendocs.Playlist({'tracks': {'albums': albums}})
    assert_float_number_is(expected_freshness, playlist.compute_freshness(now))


def assert_float_number_is(expected, actual, epsilon=0.001):
    assert expected - epsilon <= actual <= expected + epsilon


@pytest.mark.parametrize("text,expected_result", [
    ("sample text", "sample text"),
    ("\\a\tb\vc\nd==e", "\\\\a\\tb\\vc\\nd\\e\\ee"),
    ("\\abc=foo", "\\\\abc\\efoo")
])
def test__escape_unescape__string_types(text, expected_result):
    escaped = gendocs.escape(text)
    assert expected_result == escaped
    assert text == gendocs.unescape(escaped)


def test__escape__not_string_types():
    with pytest.raises(Exception, match='Can escape only str or unicode.'):
        gendocs.escape(42)


def test__unescape__not_string_types():
    with pytest.raises(Exception, match='Can escape only str or unicode.'):
        gendocs.unescape(42)
