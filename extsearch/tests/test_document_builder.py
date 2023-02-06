import extsearch.ymusic.indexer.rt_indexer.lib.document_builder as db

import pytest


@pytest.fixture()
def documents():
    return db.Documents(use_spellchecker=False)


def test_build_playlist_doc(documents):
    playlist_data = {
        'aliases': ['a1', 'a2'],
        'created': '2016-09-28T14:35:43.085Z',
        'deleted': False,
        'dup': 'some-long-hash',
        'haveCover': False,
        'haveDescription': False,
        'id': '12345:1000',
        'lastTrackAdded': '2018-11-13T14:57:15.756Z',
        'likesCount': 30,
        'revision': 5,
        'rights': ['225', '225mp'],
        'subscriptionsCount': 0,
        'title': 'favorite playlist',
        'trackIds': [
            '1:1',
            '2:2',
        ],
        'uidFactor': 100500,
    }
    non_personal_document, personal_document = documents.create_ugc_playlist_docs(playlist_data)
    assert non_personal_document['action'] == 'modify'
    assert len(non_personal_document['docs']) == 1
    assert non_personal_document['docs'][0]['url'] == 'http://music.yandex.ru/ugc/users/uid12345/playlists/1000?from=serp'
    assert non_personal_document['docs'][0]['MusicLinkRank']['type'] == '#f'
    assert int(non_personal_document['docs'][0]['MusicLinkRank']['value'] * 1000) == 35

    assert non_personal_document['prefix'] != personal_document['prefix']


def test_build_deleted_playlist_doc(documents):
    data = {
        'id': '12345:1000',
        'uidFactor': 123,
        'deleted': True,
    }
    doc_url = 'http://music.yandex.ru/ugc/users/uid12345/playlists/1000?from=serp'
    document, personal_document = documents.create_ugc_playlist_docs(data)
    assert_deleted_doc(document, doc_url)
    assert_deleted_doc(personal_document, doc_url)


def assert_deleted_doc(doc, expected_url):
    assert doc['action'] == 'delete'
    assert len(doc['docs']) == 1
    assert doc['docs'][0]['url'] == expected_url


def test_build_track_doc(documents):
    data = {
        'trackId': 'abcd-ef12-3456',
        'uid': 1,
        'uidFactor': 54321,
        'title': 'title',
        'aliases': [],
        'album': 'album',
        'artist': 'artist',
        'artistXml': '<artist></artist>',
        'length': 100000,
        'rights': ['225', '225mp'],
        'deleted': False,
    }
    document, personal_document = documents.create_track_doc(data)
    assert document['action'] == 'modify'
    assert len(document['docs']) == 1
    assert document['docs'][0]['url'] == 'http://music.yandex.ru/track/abcd-ef12-3456?from=serp'
    assert document['prefix'] != personal_document['prefix']
