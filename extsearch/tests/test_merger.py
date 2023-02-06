import logging
import yt.wrapper as yt

from extsearch.ymusic.indexer.rt_indexer.lib import merger


logger = logging.getLogger(__name__)


def test__merge_tracks(yt_test_dir):
    master_table = yt.ypath_join(yt_test_dir, 'master')
    input_tables = [
        yt.ypath_join(yt_test_dir, '1'),
        yt.ypath_join(yt_test_dir, '2'),
    ]
    new_docs_table = yt.ypath_join(yt_test_dir, 'new_docs')
    yt.write_table(master_table, [
        {'trackId': 1, 'data': 0, 'deleted': False},
        {'trackId': 4, 'data': 0, 'deleted': False},
        {'trackId': 5, 'data': 5, 'deleted': False},
    ])
    yt.write_table(input_tables[0], [
        {'trackId': 1, 'data': 1, 'deleted': False},
        {'trackId': 2, 'data': 2, 'deleted': False},
    ])
    yt.write_table(input_tables[1], [
        {'trackId': 1, 'data': 3, 'deleted': False},
        {'trackId': 3, 'data': 4, 'deleted': False},
        {'trackId': 4, 'deleted': True},
    ])

    op = merger.merge_tracks_async(input_tables, yt.TablePath(master_table), new_docs_table)
    op.wait()

    new_docs = sorted(yt.read_table(new_docs_table), key=lambda d: d['trackId'])
    assert len(new_docs) == 2
    assert new_docs[0]['trackId'] == 2
    assert new_docs[1]['trackId'] == 3
    master_docs = sorted(yt.read_table(master_table), key=lambda d: d['trackId'])
    assert len(master_docs) == 4
    assert master_docs[0]['trackId'] == 1
    assert master_docs[0]['data'] == 3
    assert master_docs[1]['trackId'] == 2
    assert master_docs[2]['trackId'] == 3
    assert master_docs[3]['trackId'] == 5


def test__merge_playlists(yt_test_dir):
    ugc_master = yt.ypath_join(yt_test_dir, 'ugc')
    verified_master = yt.ypath_join(yt_test_dir, 'verified')
    new_docs_table = yt.ypath_join(yt_test_dir, 'new_docs')
    ugc_input = [
        yt.ypath_join(yt_test_dir, 'ugc1'),
        yt.ypath_join(yt_test_dir, 'ugc2'),
    ]
    verified_input = [
        yt.ypath_join(yt_test_dir, 'verified1'),
        yt.ypath_join(yt_test_dir, 'verified2'),
    ]
    __populate_playlists_tables(ugc_master, verified_master, ugc_input, verified_input)

    op = merger.merge_playlists_async(
        ugc_input,
        verified_input,
        ugc_master,
        verified_master,
        new_docs_table,
    )
    try:
        op.wait()
    except yt.YtOperationFailedError:
        import sys
        for failed_job in op.get_jobs_with_error_or_stderr(True):
            print(failed_job['stderr'], file=sys.stderr)
        raise

    ugc_docs = sorted(yt.read_table(ugc_master), key=lambda d: d['id'])
    verified_docs = sorted(yt.read_table(verified_master), key=lambda d: d['id'])
    new_docs = sorted(yt.read_table(new_docs_table), key=lambda d: d['id'])

    for doc in ugc_docs:
        logger.error(doc)
    logger.error('-' * 20)
    for doc in verified_docs:
        logger.error(doc)
    logger.error('-' * 20)
    for doc in new_docs:
        logger.error(doc)

    assert {doc['id'] for doc in ugc_docs} == {'1:1', '1:2', '3:1', '5:2'}
    assert {doc['id'] for doc in verified_docs} == {'2:2', '3:2', '4:1', '4:2', '5:1'}
    assert {doc['id'] for doc in new_docs} == {'2:2', '3:1', '5:1', '5:2'}


def __populate_playlists_tables(ugc_master, verified_master, ugc_input, verified_input):
    assert len(ugc_input) == 2
    assert len(verified_input) == 2
    yt.write_table(ugc_master, [
        _create_playlist(id_, False)
        for id_ in ['1:1', '1:2', '2:1', '2:2']
    ])
    yt.write_table(verified_master, [
        _create_playlist(id_, True, login=login)
        for id_, login in [('3:1', 'a'), ('3:2', 'a'), ('4:1', 'b'), ('4:2', 'b')]
    ])
    yt.write_table(ugc_input[0], [
        _create_playlist('1:1', False, data=1),
        _create_deleted_playlist('2:1', False),
        _create_playlist('5:1', False),
    ])
    yt.write_table(ugc_input[1], [
        _create_playlist('3:1', False, lastModified=2, data=1),
        _create_playlist('5:2', False, lastModified=2),
    ])
    yt.write_table(verified_input[0], [
        _create_playlist('3:1', True, data=2, login='a'),
        _create_playlist('3:2', True, data=1, login='a'),
    ])
    yt.write_table(verified_input[1], [
        _create_playlist('2:2', True, data=1, lastModified=2, login='c'),
        _create_playlist('5:1', True, data=1, lastModified=2, login='d'),
    ])


def _create_playlist(id_, verified, **extra_fields):
    modified = extra_fields.pop('lastModified', 1)
    return {
        'id': id_,
        'data': 0,
        'deleted': False,
        'verified': verified,
        'likesCount': 0,
        'uidFactor': abs(hash(id_)),
        'lastModified': modified,
        **extra_fields,
    }


def _create_deleted_playlist(id_, verified):
    return {
        'id': id_,
        'deleted': True,
        'uidFactor': abs(hash(id_)),
        'verified': verified,
    }
