import unittest
import os
import json
from toloka_matching_acc.prepare_honeypots import do_prepare_honeypots
from toloka_matching_acc.prepare_honeypots import DataChecker


class PrepareTasksTest(unittest.TestCase):
    def setUp(self):
        THIS_DIR = os.path.dirname(os.path.abspath(__file__))
        TEST_DATA_DIR = os.path.join(THIS_DIR, os.pardir, 'test_data/matching_acc/')
        self.msku_urls_file_path = os.path.join(TEST_DATA_DIR, 'msku_url.json')
        self.skus_info_file_path = os.path.join(TEST_DATA_DIR, 'skus_info.json')

    def test_data_checker(self):
        checker = DataChecker()
        checker.add(1, "http://some", True)
        checker.add(2, "http://some", True)

        checker.ensure_not_corrupted(1, "http://some", True)

        self.assertTrue(checker.is_duplicate(1, "http://some"))

        self.assertRaises(Exception, checker.ensure_not_corrupted, 1, "http://some", False)

    def test_prepare(self):
        with open(self.msku_urls_file_path) as msku_urls_file:
            msku_urls = json.load(msku_urls_file)

        with open(self.skus_info_file_path) as skus_info_file:
            skus_info = json.load(skus_info_file)

        tasks = do_prepare_honeypots(self.msku_urls_file_path, self.skus_info_file_path)

        urls = map(lambda task: task["inputValues"]["url"], tasks)
        msku_ids = map(lambda task: task["inputValues"]["msku_id"], tasks)

        input_msku_ids = map(lambda sku_info: sku_info["msku_id"], skus_info)
        input_urls = map(lambda msku_url: msku_url["url"], msku_urls)

        self.assertItemsEqual(list(set(msku_ids)), input_msku_ids)

        # Remove "http://some7.com" from input_urls because it for not found sku 1714984441.
        input_urls.remove("http://some7.com")
        self.assertItemsEqual(input_urls, urls)
