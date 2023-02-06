# -*- coding: utf-8 -*-

import collections

import pytest
from hamcrest import assert_that, all_of, equal_to

from pyb.plugin.marketsearch.copiers import IndexCopier
from .common import make_full_generation


GENERATION = "20190101_0101"
Config = collections.namedtuple('Config', [
    "backup_dir",
    "download_dir",
    "dists_root_dir",
    "dists",
    "uc_bin",
    "experimental_unpack",
    "cpu_limit",
    "clean_recipes_xml",
    "index_unpacker",
    "experiment_flags_reader"
])


@pytest.fixture(params=[True, False], ids=["split_search_part", "single_search_part"])
def with_split(request):
    dists_with_dirs = None
    if request.param:
        dists_with_dirs = [
            ("search-part-base-0", 'index/part-0'),
            ("search-part-additions-0", 'index/part-0'),

            ("search-part-additions-1", 'index/part-1'),
            ("search-part-base-1", 'index/part-1'),
        ]
    else:
        dists_with_dirs = [
            ("search-part-0", 'index/part-0'),
            ("search-part-1", 'index/part-1'),
        ]
    return request.param, dists_with_dirs


@pytest.fixture(params=[False, True], ids=["without_experimental_unpack", "with_experimental_unpack"])
def copy_search_part(request, tmpdir, with_split):
    split_search_part, dists_with_dirs = with_split
    make_full_generation(tmpdir, GENERATION, completed=True, split_search_part=split_search_part)
    config = Config(
        backup_dir=str(tmpdir / 'search/backup'),
        download_dir=str(tmpdir / 'search/marketsearch'),
        dists_root_dir=str(tmpdir / 'search'),
        dists=dists_with_dirs,
        uc_bin=None,
        cpu_limit=1,
        experimental_unpack=request.param,
        clean_recipes_xml=False,
        index_unpacker=None,
        experiment_flags_reader=None
    )
    copier = IndexCopier(config, GENERATION)
    copier.run()


def test_copy_search_part(copy_search_part, tmpdir):
    """Проверяем, что поисковый индекса разложился в обоих случаях
    """
    assert_that(all_of(
        # base files
        (tmpdir / 'search/index/part-0/indexinv').read(), equal_to(GENERATION),
        (tmpdir / 'search/index/part-0/indexkey').read(), equal_to(GENERATION),
        # additions files
        (tmpdir / 'search/index/part-0/indexfactorann').read(), equal_to(GENERATION),

        # base files
        (tmpdir / 'search/index/part-1/indexinv').read(), equal_to(GENERATION),
        (tmpdir / 'search/index/part-1/indexkey').read(), equal_to(GENERATION),
        # additions files
        (tmpdir / 'search/index/part-1/indexfactorann').read(), equal_to(GENERATION),
    ))
