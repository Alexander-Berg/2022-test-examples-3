# -*- coding: utf-8 -*-
import yt.wrapper as yt

import extsearch.ymusic.scripts.reindex.gendocs as gd

artist_test_data = {
    'available': 'true',
    'composer': False,
    'id': '1',
    'modified': '2019-08-11T15:28:53+03:00',
    'name': [
        'Mack Gordon',
        'ã\x83\x9eã\x83\x83ã\x82¯ã\x83»ã\x82´ã\x83¼ã\x83\x89ã\x83³',
        'MACK GORDON',
        'ã\x83\x9eã\x83\x83ã\x82¯ã\x82´ã\x83¼ã\x83\x89ã\x83³',
        'ï¼\xadï½\x81ï½\x83ï½\x8bï¼§ï½\x8fï½\x92ï½\x84ï½\x8fï½\x8e',
        'Mack  Gordon'
    ],
    'region-data': [
        {
            'also-album-count': '1',
            'direct-album-count': '0',
            'has-recent-albums': 'false',
            'popular-tracks': ['6488971'],
            'radio-is-available': False,
            'region': ['983', '10099', '983mp', '10099mp'],
            'track-count': '1'
        },
        {
            'also-album-count': '43',
            'cover': [{
                'image-uri': 'avatars.yandex.net/get-music-content/28589/2e0bd28e.a.3712151-1/%%',
                'type': 'from-album-cover'
            }],
            'direct-album-count': '2',
            'has-recent-albums': 'false',
            'popular-tracks': [
                '53022215',
                '53022266',
                '53022237',
                '53022291',
                '45873849',
                '20543094',
                '18159763',
                '35604621',
                '30687310',
                '54960412'
            ],
            'radio-is-available': False,
            'region': [
                '225mp',
                '159',
                '168mp',
                '209mp',
                '167mp',
                '171mp',
                '171',
                '170',
                '208mp',
                '149mp',
                '187',
                '207',
                '209',
                '208',
                '170mp',
                '149',
                '159mp',
                '207mp',
                '168',
                '169',
                '187mp',
                '225',
                '167',
                '169mp'],
            'track-count': '39'
        },
        {
            'also-album-count': '23',
            'direct-album-count': '0',
            'has-recent-albums': 'false',
            'popular-tracks': [
                '53022215',
                '53022266',
                '53022237',
                '53022291',
                '18159763',
                '54960412',
                '4905087',
                '54949218',
                '6488971',
                '14504109'],
            'radio-is-available': False,
            'region': ['181mp', '181'],
            'track-count': '20'
        }
    ],
    'russian-tracks-fraction': [{}],
    'various': False
}

artist_test_data_short = {
    'id': artist_test_data['id'],
    'name': artist_test_data['name'],
    'various': artist_test_data['various'],
    'region-data': artist_test_data['region-data'],
    'popularity': 0.05,
}


def test__join_artist_used(base_dir):
    yj = yt.ypath_join
    yw = yt.write_table
    yr = yt.read_table

    data_dir = yj(base_dir, 'data')
    yw(yj(data_dir, 'artist.clean'), [{
        'key': '1',
        'subkey': '',
        'value': artist_test_data,
    }])
    yw(yj(data_dir, 'album.artist.id'), [{
        'key': '1', 'subkey': 'album', 'value': '1',
    }])
    yw(yj(data_dir, 'album.first-tracks.artist.id'), [{
        'key': '1', 'subkey': 'album', 'value': '100',
    }])
    yw(yj(data_dir, 'track.artist.id'), [{
        'key': '1', 'subkey': 'track', 'value': '200',
    }])
    yw(yj(data_dir, 'playlist.artist.id'), [{
        'key': '1', 'subkey': 'playlist-artists', 'value': '100:200',
    }])
    yw(yj(data_dir, 'artist.similar-artists.artist.id'), [{
        'key': '1', 'subkey': 'similar-artist', 'value': '300',
    }])
    yw(yj(data_dir, 'performer.popularity'), [{
        'key': '1', 'subkey': 'pop', 'value': 0.05,
    }])
    yw(yj(data_dir, 'playlist.auto.artist.id'), [{
        'key': '1', 'subkey': 'autoplaylist', 'value': '1000',
    }])

    gd.JoinArtistUsed(
        mr_prefix=base_dir + '/',
        base_mr_prefix=base_dir + '/',
    ).run()

    out = list(yr(yj(data_dir, 'artist.albums')))
    assert len(out) == 1
    assert out[0]['key'] == '1'
    assert out[0]['subkey'] == 'albums'
    assert sorted(out[0]['value']) == ['1', '100']

    out = list(yr(yj(data_dir, 'playlist.auto.artist')))
    assert len(out) == 1
    assert out[0]['key'] == '1000:1'
    assert out[0]['subkey'] == 'artist'
    assert out[0]['value'] == artist_test_data_short

    out = list(yr(yj(data_dir, 'track.artists')))
    assert len(out) == 1
    assert out[0]['key'] == '200'
    assert out[0]['subkey'] == '2'
    assert out[0]['value'] == {"1": artist_test_data_short}

    out = list(yr(yj(data_dir, 'album.artists')))
    assert len(out) == 2
    assert out[0]['key'] in {'1', '100'}
    assert out[0]['subkey'] == 'artists'
    assert out[0]['value'] == {"1": artist_test_data_short}
    assert out[1]['key'] in {'1', '100'}
    assert out[1]['key'] != out[0]['key']
    assert out[1]['subkey'] == 'artists'
    assert out[1]['value'] == {"1": artist_test_data_short}

    out = list(yr(yj(data_dir, 'playlist.artists')))
    assert len(out) == 1
    assert out[0]['key'] == '100:200'
    assert out[0]['subkey'] == '3'
    assert out[0]['value'] == {"1": artist_test_data_short}

    out = list(yr(yj(data_dir, 'artist.similar-artists')))
    assert len(out) == 1
    assert out[0]['key'] == '300'
    assert out[0]['subkey'] == 'similar-artists'
    assert out[0]['value'] == {"1": artist_test_data_short}
