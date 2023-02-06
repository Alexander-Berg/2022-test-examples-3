#  -*- coding: utf-8 -*-

from mock import patch
from namedlist import namedlist
import os
import pytest

from market.idx.pylibrary.mindexer_core.market_collections.market_collections import OffersCollection


GENERATION = '20210531_0910'


@pytest.fixture
def indexer_dir(tmpdir):
    return tmpdir / 'indexer'


@pytest.fixture
def config(indexer_dir):
    Config = namedlist('Config', [
        ('calc_dist_unsuitable_files_filter', ''),
        ('calc_dist_master_path_filter', ''),
        ('working_dir', str(indexer_dir)),
    ])
    return Config()


@pytest.fixture
def generation_files(indexer_dir):
    os.makedirs(str(indexer_dir / 'offers' / GENERATION))
    os.makedirs(str(indexer_dir / GENERATION / 'stats'))
    with open(str(indexer_dir / GENERATION / 'stats/total-stats.txt'), 'w') as f:
        f.write('\n'.join(('num_offers: 10', 'num_blue_offers: 20', 'num_clusters: 30')))

    os.makedirs(str(indexer_dir / GENERATION / 'offers-processor'))
    with open(str(indexer_dir / GENERATION / 'offers-processor/model_count_total.csv'), 'w') as f:
        f.write('num_models: 40\n')

    os.makedirs(str(indexer_dir / GENERATION / 'offers-processor_blue_on_white'))
    with open(str(indexer_dir / GENERATION / 'offers-processor_blue_on_white/offers_summary.tsv'), 'w') as f:
        f.write('num_accepted_offers\t50\n')


def test_dist_statistics(indexer_dir, generation_files, config):
    """ Test that dist_stats.tsv is composed of multiple sources """

    # act
    collection = OffersCollection(str(indexer_dir), GENERATION)
    collection.calc_dist_statistic(config)

    # assert
    with open(str(indexer_dir / GENERATION / 'stats' / 'dist_stats.tsv')) as f:
        stats = list(sorted(line.strip().split('\t') for line in f))

    assert stats == [
        ['num_blue_offers', '20'],
        ['num_clusters', '30'],
        ['num_models', '40'],
        ['num_offers', '10'],
        ['num_offers_in_blue_shard', '50'],
    ]


def test_copy_shards_to_index(indexer_dir, generation_files, config):
    """ Test sharded copying logic """

    # arrange
    idx_name = 'model-vclusters-index'
    published_name = 'model_filters_inverted_intersector.mmap'
    stats_dir = os.path.join(str(indexer_dir), GENERATION, 'stats')
    model_vclusters_path = os.path.join(stats_dir, 'offer_filters_for_models_stat', 'model-vcluster-index')
    os.makedirs(model_vclusters_path)

    shards_num = 2
    shards_paths = [os.path.join(model_vclusters_path, 'offer_filters_for_models_stat-{}.mmap'.format(i)) for i in range(shards_num)]
    for path in shards_paths:
        with open(path, 'w') as f:
            f.write('anything')

    # act
    collection = OffersCollection(str(indexer_dir), GENERATION)

    with patch('market.idx.marketindexer.miconfig.default', lambda: config):
        collection.copy_shards_to_index(shards_paths, idx_name, published_name)

    # assert
    assert os.path.exists(os.path.join(str(indexer_dir), GENERATION, idx_name, 'workindex', published_name))
    assert os.path.exists(os.path.join(str(indexer_dir), GENERATION, idx_name, 'workindex1', published_name))
