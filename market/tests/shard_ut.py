from unittest import TestCase
import report_config.shard


ROLE = 'marketsearch3'


class SimpleTestShard(report_config.shard.Shard):
    _index_relative_dir = 'simple_dir'
    _base_collection_id = 'simple'


class SplitTestShard(report_config.shard.PartShard):
    _index_relative_dir = 'split_dir'
    _base_collection_id = 'split'


class TestShard(TestCase):
    def test_simple_shard(self):
        shard = SimpleTestShard(ROLE)
        self.assertEqual(shard.get_path('index'), 'index/simple_dir')
        self.assertEqual(shard.collection_id, 'simple')

    def test_split_shard(self):
        shard = SplitTestShard(4, ROLE)
        self.assertEqual(shard.get_path('index'), 'index/split_dir/part-4')
        self.assertEqual(shard.collection_id, 'split-4')

    def test_search_shard(self):
        shard = report_config.shard.SearchShard(3, ROLE)
        self.assertEqual(shard.get_path('index'), 'index/part-3')
        self.assertEqual(shard.collection_id, 'basesearch16-3')

    def test_model_shard(self):
        shard = report_config.shard.ModelShard(2, ROLE)
        self.assertEqual(shard.get_path('somewhere'), 'somewhere/model/part-2')
        self.assertEqual(shard.collection_id, 'basesearch-model-2')

    def test_book_shard(self):
        shard = report_config.shard.BookShard(5, ROLE)
        self.assertEqual(shard.get_path('index'), 'index/book/part-5')
        self.assertEqual(shard.collection_id, 'basesearch-book-5')

    def test_diff_shard(self):
        shard = report_config.shard.DiffShard(1, ROLE)
        self.assertEqual(shard.get_path('index'), 'index/diff-part-1')
        self.assertEqual(shard.collection_id, 'basesearch-diff16-1')
