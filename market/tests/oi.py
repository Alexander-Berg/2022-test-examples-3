# -*- coding: utf-8 -*-

import os
import py
import pytest
import yatest
from string import Template

from market.pylibrary.yatestwrap.yatestwrap import source_path, binary_path

from market.idx.mif.mif.rpc.app import Config
from market.idx.mif.mif.rpc.oi import OffersIndexer
from market.idx.mif.mif.rpc.torrentserver import TorrentServer

DATA_DIR = source_path('market/idx/mif/tests/data')


class TorrentServerStub(TorrentServer):
    def __init__(self, metafiles_dir):
        self.bin_path = '/bin/echo'
        self.metafiles_dir = metafiles_dir

    def add_dist(self, dist, version, path, tracker_url=None, datacenter_list=None):
        meta_dir = py.path.local(self.metafiles_dir)
        torrent = meta_dir / '{}-{}.torrent'.format(dist, version)
        torrent.write(path)
        if datacenter_list:
            for dc in datacenter_list:
                torrent = meta_dir / '{}-{}-{}.torrent'.format(dist, version, dc)
                torrent.write(path)
        return 0

    def remove_dist(self, dist, version=None):
        meta_dir = py.path.local(self.metafiles_dir)
        pattern = dist + '-'
        if version:
            pattern += version
        pattern += '*'
        files = meta_dir.visit(pattern)
        if not files:
            return 1
        for f in files:
            f.remove()
        return 0


@pytest.fixture()
def offers_indexer(tmpdir):
    class Options(object):
        def __init__(self):
            self.logfile = None
            self.host = None
            self.port = None
            self.dirname = None
    with open(os.path.join(DATA_DIR, 'mifd_template.cfg'), 'r') as tf:
        mifd_template = Template(tf.read())
    mifd_path = str(tmpdir / 'mifd.cfg')
    with open(mifd_path, 'w') as mifd:
        mifd.write(mifd_template.safe_substitute(
            mif_dir=str(tmpdir / 'mif'),
            run_dir=str(tmpdir / 'run'),
            log_dir=str(tmpdir / 'log'),
            dist_dir_path=str(tmpdir / 'dists')
        ))

    os.environ['IC_CONFIG_PATH'] = yatest.common.source_path('market/idx/miconfigs/etc/feature/common.ini')
    os.environ['IL_CONFIG_PATH'] = yatest.common.source_path('market/idx/miconfigs/etc/feature/common.ini')

    config = Config(mifd_path, Options())
    torrents_dir = tmpdir / 'torrents'
    torrents_dir.ensure(dir=True)
    torrent_server = TorrentServerStub(str(torrents_dir))
    oi = OffersIndexer(config, torrent_server)
    oi.hostname = 'test_host'
    os.environ['UC_BIN_PATH'] = binary_path('tools/uc/uc')
    return oi


def test_make_dists(tmpdir, offers_indexer):
    work_dir = tmpdir / 'mif/offers'
    dists_dir = tmpdir / 'dists'
    generation_name = '20170505_2052'
    generation_dir = work_dir.mkdir(generation_name + '-0')
    generation_dir.ensure('workindex/indexarc')
    generation_dir.ensure('workindex/indexdir')
    generation_dir.ensure('workindex/indexinv')
    generation_dir.ensure('workindex1/indexarc')
    generation_dir.ensure('workindex1/indexdir')
    generation_dir.ensure('workindex1/indexinv')
    generation_dir.ensure('indexarc.v2/workindex/indexarc')
    generation_dir.ensure('indexarc.v2/workindex/indexdir')

    res = offers_indexer.make_dists(
        generation_name + '-0',
        generation_name,
        ['search-part-0', 'search-part-1'],
        'http://localhost:6970/announce',
        [0, 1])
    assert(res['returncode'] == 0)
    assert(set(res['uris']) == set([
        'rsync://test_host/oi/20170505_2052-0/torrents/search-part-0-20170505_2052.torrent',
        'rsync://test_host/oi/20170505_2052-0/torrents/search-part-1-20170505_2052.torrent'
    ]))
    meta_dir = py.path.local(offers_indexer.torrentserver.metafiles_dir)
    search_part0 = meta_dir / 'search-part-0-20170505_2052.torrent'
    search_part1 = meta_dir / 'search-part-1-20170505_2052.torrent'
    assert(search_part0.read() == str(dists_dir / generation_name / 'search-part-0'))
    assert(search_part1.read() == str(dists_dir / generation_name / 'search-part-1'))


def test_make_dists_only_indexarc(tmpdir, offers_indexer):
    work_dir = tmpdir / 'mif/offers'
    dists_dir = tmpdir / 'dists'
    generation_name = '20170505_2052'
    generation_dir = work_dir.mkdir(generation_name + '-0')
    generation_dir.ensure('workindex/indexarc')
    generation_dir.ensure('workindex/indexdir')
    generation_dir.ensure('workindex/indexinv')
    generation_dir.ensure('workindex1/indexarc')
    generation_dir.ensure('workindex1/indexdir')
    generation_dir.ensure('workindex1/indexinv')
    res = offers_indexer.make_dists(
        generation_name + '-0',
        generation_name,
        ['search-snippet-0', 'search-snippet-1'],
        'http://localhost:6970/announce',
        [0, 1],
        only_indexarc=True)
    assert(res['returncode'] == 0)
    assert(set(res['uris']) == set([
        'rsync://test_host/oi/20170505_2052-0/torrents/search-snippet-0-20170505_2052.torrent',
        'rsync://test_host/oi/20170505_2052-0/torrents/search-snippet-1-20170505_2052.torrent'
    ]))
    meta_dir = py.path.local(offers_indexer.torrentserver.metafiles_dir)
    search_snippet0 = meta_dir / 'search-snippet-0-20170505_2052.torrent'
    search_snippet1 = meta_dir / 'search-snippet-1-20170505_2052.torrent'
    assert(search_snippet0.read() == str(dists_dir / generation_name / 'search-snippet-0'))
    assert(search_snippet1.read() == str(dists_dir / generation_name / 'search-snippet-1'))


def touch(dir, relative_filepath):
    filepath = os.path.join(str(dir), relative_filepath)
    basedir = os.path.dirname(filepath)
    if not os.path.exists(basedir):
        os.makedirs(basedir)
    open(filepath, 'w')


def test_prepare_search_part_stage(tmpdir, offers_indexer):
    work_dir = tmpdir / 'mif/offers'
    generation_name = '20170505_2052'
    generation_dir = work_dir.mkdir(generation_name + '-0')
    touch(generation_dir, 'workindex/indexkey')
    touch(generation_dir, 'workindex/indexinv')
    touch(generation_dir, 'workindex/somedir/somefile')
    touch(generation_dir, 'input/snapshot.meta')
    with open(os.path.join(str(generation_dir), 'input/snapshot.meta'), 'w') as snap:
        snap.write('EXS_TIME 1')

    offers_indexer.prepare_search_part_dist_base(generation_name + '-0', [0])

    touch(generation_dir, 'workindex/erf')
    touch(generation_dir, 'workindex/herf')

    offers_indexer.prepare_search_part_dist_additions(generation_name + '-0', [0])

    assert(not os.path.exists(str(generation_dir / 'wi0_search_part_base/indexkey')))
    assert(not os.path.exists(str(generation_dir / 'wi0_search_part_base/indexinv')))
    # файл который не должен попасть в базовый шард, но создался до вызова prepare_search_part_dist_base
    assert(not os.path.exists(str(generation_dir / 'wi0_search_part_base/somedir/somefile')))

    assert(os.path.exists(str(generation_dir / 'wi0_search_part_additions/indexkey')))
    assert(os.path.exists(str(generation_dir / 'wi0_search_part_additions/indexinv')))
    assert(os.path.exists(str(generation_dir / 'wi0_search_part_additions/somedir/somefile')))
    assert(os.path.exists(str(generation_dir / 'wi0_search_part_additions/erf')))
    assert(os.path.exists(str(generation_dir / 'wi0_search_part_additions/herf')))


def test_check_files_in_workindex_single_shard_ok(tmpdir, offers_indexer):
    work_dir = tmpdir / 'mif/offers'
    generation_name = '20180102_1010'
    generation_dir = work_dir.mkdir(generation_name + '-0')
    workindex_dir = os.path.join(str(generation_dir), 'workindex')

    touch(workindex_dir, 'some_file_0')
    touch(workindex_dir, 'some_file_1')

    relative_file_pathds = ['some_file_0', 'some_file_1']
    generation = generation_name + '-0'
    o = offers_indexer.check_files_in_workindex(relative_file_pathds, generation, [0])
    assert(o['returncode'] == 0)
    assert(len(o['absent_files']) == 0)


def test_check_files_in_workindex_single_shard_file_absent(tmpdir, offers_indexer):
    work_dir = tmpdir / 'mif/offers'
    generation_name = '20180102_1010'
    generation_dir = work_dir.mkdir(generation_name + '-0')
    workindex_dir = os.path.join(str(generation_dir), 'workindex')

    touch(workindex_dir, 'some_file_0')
    touch(workindex_dir, 'some_file_1')

    relative_file_pathds = ['some_file_0', 'some_file_1', 'some_dir/absent_file_0', 'absent_file_1']
    generation = generation_name + '-0'
    o = offers_indexer.check_files_in_workindex(relative_file_pathds, generation, [0])
    assert(o['returncode'] == 0)
    assert(len(o['absent_files']) == 2)

    assert os.path.join(workindex_dir, 'some_dir/absent_file_0') in o['absent_files']
    assert os.path.join(workindex_dir, 'absent_file_1') in o['absent_files']


def test_check_files_in_workindex_multiple_shards_ok(tmpdir, offers_indexer):
    work_dir = tmpdir / 'mif/offers'
    generation_name = '20180102_1010'
    generation_dir = work_dir.mkdir(generation_name + '-0')
    workindex0_dir = os.path.join(str(generation_dir), 'workindex')
    workindex1_dir = os.path.join(str(generation_dir), 'workindex1')

    touch(workindex0_dir, 'some_file_0')
    touch(workindex0_dir, 'some_file_1')
    touch(workindex1_dir, 'some_file_0')
    touch(workindex1_dir, 'some_file_1')

    relative_file_pathds = ['some_file_0', 'some_file_1']
    generation = generation_name + '-0'
    o = offers_indexer.check_files_in_workindex(relative_file_pathds, generation, [0, 1])

    assert(o['returncode'] == 0)
    assert(len(o['absent_files']) == 0)


def test_check_files_in_workindex_multiple_shards_file_absent(tmpdir, offers_indexer):
    work_dir = tmpdir / 'mif/offers'
    generation_name = '20180102_1010'
    generation_dir = work_dir.mkdir(generation_name + '-0')
    workindex0_dir = os.path.join(str(generation_dir), 'workindex')
    workindex1_dir = os.path.join(str(generation_dir), 'workindex1')

    touch(workindex0_dir, 'some_file_0')
    touch(workindex0_dir, 'some_file_1')
    # touch(workindex1_dir, 'some_file_0')
    touch(workindex1_dir, 'some_file_1')

    relative_file_pathds = ['some_file_0', 'some_file_1']
    generation = generation_name + '-0'
    o = offers_indexer.check_files_in_workindex(relative_file_pathds, generation, [0, 1])

    assert(o['returncode'] == 0)
    assert(len(o['absent_files']) == 1)
    assert(o['absent_files'][0] == os.path.join(workindex1_dir, 'some_file_0'))
