# -*- coding: utf-8 -*-

import os
import pytest

from market.pylibrary.mi_util import util
from market.pylibrary.yatestwrap.yatestwrap import source_path
from market.pylibrary.compress_dist.compress_dist import (
    compress,
    decompress,
    _get_archive_ext,
    is_dist_archive,
    get_all_codecs,
)


BUILDROOT = os.environ.get('ARCADIA_BUILD_ROOT')
DATA_DIR = source_path('market/pylibrary/compress_dist/tests/data')


@pytest.fixture(scope='module')
def uc_bin_path():
    return os.path.join(BUILDROOT, 'tools/uc/uc')


def test_get_archive_ext():
    tar, codec = _get_archive_ext('dist.tar.gz')
    assert(tar == 'tar' and codec == 'gz')
    tar, codec = _get_archive_ext('dist')
    assert(tar is None and codec is None)
    tar, codec = _get_archive_ext('/root/lala/dist.tar.zstd_10')
    assert(tar == 'tar' and codec == 'zstd_10')
    tar, codec = _get_archive_ext('dist.gz')
    assert(tar is None and codec == 'gz')


def test_decompress_tar_gz(tmpdir, uc_bin_path):
    arc = os.path.join(DATA_DIR, 'sample_dist.tar.gz')
    dest_dir = str(tmpdir)
    decompress(arc, dest_dir, bin_path=uc_bin_path)
    assert(os.path.exists(os.path.join(dest_dir, 'file1')))
    assert(os.path.exists(os.path.join(dest_dir, 'file2')))


def test_decompress_tar_zstd_10(tmpdir, uc_bin_path):
    arc = os.path.join(DATA_DIR, 'sample_dist.tar.zstd_10')
    dest_dir = str(tmpdir)
    decompress(arc, dest_dir, bin_path=uc_bin_path)
    assert(os.path.exists(os.path.join(dest_dir, 'file1')))
    assert(os.path.exists(os.path.join(dest_dir, 'file2')))


@pytest.mark.skipif(not util.which('pigz'),
                    reason='need pigz to unzip tar.gz')
def test_compress_tar_gz(tmpdir, uc_bin_path):
    source_dir = os.path.join(DATA_DIR, 'sample_dist')
    archive = os.path.join(str(tmpdir), 'sample_dist.tar.gz')
    compress(source_dir, archive, codec='gz', bin_path=uc_bin_path)
    assert(os.path.exists(archive))
    decompressed_dir = os.path.join(str(tmpdir), 'uncompressed')
    decompress(archive, decompressed_dir, bin_path=uc_bin_path)
    assert(os.path.exists(os.path.join(decompressed_dir, 'file1')))
    assert(os.path.exists(os.path.join(decompressed_dir, 'file2')))


def test_compress_tar_zstd_10(tmpdir, uc_bin_path):
    source_dir = os.path.join(DATA_DIR, 'sample_dist')
    archive = os.path.join(str(tmpdir), 'sample_dist.tar.zstd_10')
    compress(source_dir, archive, codec='zstd_10', bin_path=uc_bin_path)
    assert(os.path.exists(archive))
    decompressed_dir = os.path.join(str(tmpdir), 'uncompressed')
    decompress(archive, decompressed_dir, bin_path=uc_bin_path)
    assert(os.path.exists(os.path.join(decompressed_dir, 'file1')))
    assert(os.path.exists(os.path.join(decompressed_dir, 'file2')))


def test_compress_exclude(tmpdir, uc_bin_path):
    source_dir = os.path.join(DATA_DIR, 'sample_dist')
    archive = os.path.join(str(tmpdir), 'sample_dist.tar.zstd_10')
    compress(source_dir, archive, codec='zstd_10', exclude=['file1'],
             bin_path=uc_bin_path)
    assert(os.path.exists(archive))
    decompressed_dir = os.path.join(str(tmpdir), 'uncompressed')
    decompress(archive, decompressed_dir, bin_path=uc_bin_path)
    assert(not os.path.exists(os.path.join(decompressed_dir, 'file1')))
    assert(os.path.exists(os.path.join(decompressed_dir, 'file2')))


def test_is_dist_archive(uc_bin_path):
    assert(is_dist_archive('cool_dist.tar.gz', bin_path=uc_bin_path))
    assert(is_dist_archive('cool_dist.tar.zstd_10', bin_path=uc_bin_path))
    assert(not is_dist_archive('cool_dist.tar.bad_ext', bin_path=uc_bin_path))
    assert(not is_dist_archive('not_an_archive', bin_path=uc_bin_path))


def test_get_all_codecs(uc_bin_path):
    codecs = get_all_codecs(uc_bin_path)
    assert('gz' in codecs)
    assert('zstd_10' in codecs)


def test_alt_dest_dir(tmpdir, uc_bin_path):
    archive = os.path.join(DATA_DIR, 'test_dist.tar.zstd_1')
    dest_dir = os.path.join(str(tmpdir), 'dest_dir')
    alt_dest_dir = os.path.join(str(tmpdir), 'alt_dest_dir')
    alt_source_regex_list = [
        r'cmagic_id\.c2n\.wad',
        r'.+\.c2n',
        r'index\.catm',
        r'index\.catm_doc2vec',
        r'index\.catm_big_vectors',
        r'index\.catm_hyper2vec',
        r'index\.catm_ids_mapping',
        r'index\.catm_small_vectors',
    ]
    decompress(archive, dest_dir, bin_path=uc_bin_path, alt_source_regex_list=alt_source_regex_list, alt_dest_dir=alt_dest_dir)
    assert(os.path.exists(os.path.join(dest_dir, 'bids-timestamps.fb')))

    for file_name in (
        'cmagic_id.c2n.wad',
        'ferrykey.c2n',
        'hyper_ts_blue.c2n',
        'index.catm',
        'index.catm_big_vectors',
        'index.catm_doc2vec',
        'index.catm_hyper2vec',
        'index.catm_ids_mapping',
        'index.catm_small_vectors',
        'maliases.c2n',
        's.c2n',
    ):
        assert(os.path.isfile(os.path.join(alt_dest_dir, file_name)))
        assert(os.path.islink(os.path.join(dest_dir, file_name)))
        assert(os.readlink(os.path.join(dest_dir, file_name)) == os.path.join(alt_dest_dir, file_name))
