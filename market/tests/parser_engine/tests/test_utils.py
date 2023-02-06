# -*- coding: utf-8 -*-
from datetime import datetime
import unittest
import logging

from six.moves.configparser import ConfigParser

from market.idx.datacamp.parser.lib.parser_engine import utils
from market.idx.pylibrary.feed.utils import feedparser_shopsdat_options, mbi_fp_params_differs


class PathCreator(object):
    def __init__(self):
        self.dirs = []

    def __call__(self, dir):
        self.dirs.append(dir)


class TestUtils(unittest.TestCase):
    prefix = '[2013-04-08 06:47:34]'

    @staticmethod
    def format_with_prefix(lines):
        return [i.format(TestUtils.prefix) for i in lines]

    def test_good_log_filtering(self):
        goods = TestUtils.format_with_prefix([
            '{0} (Message) bla',
            '{0} (Error) bla',
            '{0} (Warning) 123 bla',
            '{0} (Warning) 310 bla',
            '{0} (Warning) 355 Invalid barcode',
            '{0} (Warning) 35Z Too long Shop',
            '{0} (Warning) 35h Bla',
        ])
        for good in goods:
            self.assertEqual(utils.filter_log(good), good + '\n')

    def test_bad_log_filtering(self):
        bads = TestUtils.format_with_prefix([
            '{0} (Message) 25a Cannot determine vendor',
            '{0} (Message) 25b Cannot determine vendor',
            '{0} (Message) 25c Model matched',
            '{0} (Message) 25d Model name not full',
            '{0} (Message) 25e Price conflict',
            '{0} (Message) 25f Price anomaly',
            '{0} (Message) 25h Can\'t find value for major parameter',
        ])
        for bad in bads:
            self.assertEqual(utils.filter_log(bad), '')

    def test_log_delivery_filtering(self):
        """Проверяем, что при настройке в конфиге максимального количества сообщений
        часть сообщений об отсутствии доставки отбрасывается"""
        config = ConfigParser()
        config.add_section('general')
        config.set('general', 'log_limit_delivery_errors', '1')

        lines = [
            '{0} (Error) 49A 1',
            '{0} (Error) 49B 2',
            '{0} (Error) 490',
        ]
        log = "\n".join(TestUtils.format_with_prefix(lines)) + '\n'

        allowed = [
            lines[0],
            lines[2],
        ]
        expected = "\n".join(TestUtils.format_with_prefix(allowed)) + '\n'

        filtered = utils.filter_log(log, None, config)
        self.assertEqual(filtered, expected)

    def test_saas_limit_log_filtering(self):
        lines0 = [
            '{0} (Message) bla',
            '{0} (Error) bla',
            '{0} (Warning) 123 bla',
            '{0} (Warning) 310 bla',
            '{0} (Warning) 355 Invalid barcode',
            '{0} (Warning) 35Z Too long Shop',
            '{0} (Warning) 35h Bla',
        ]
        lines_saas = ['{0} (Message) Erros while send doc to saas:offer' for i in range(150)]

        def test_lines(lines):
            log_lines = TestUtils.format_with_prefix(lines)
            log = "\n".join(log_lines)
            filtered = utils.filter_log(log)
            logging.debug(filtered)
            self.assertEqual(len(filtered.split('\n')), len(lines0) + 100 + 1)

        test_lines(lines0 + lines_saas)
        test_lines(lines_saas + lines0)

    def test_session_formatting(self):
        dt = datetime(2004, 12, 7, 23, 59)
        self.assertEqual(utils.get_session_name(dt), '20041207_2359')

        dt = datetime(2034, 2, 17, 3, 9)
        self.assertEqual(utils.get_session_name(dt), '20340217_0309')

    def test_get_log_name(self):
        self.assertEqual(utils.get_log_name('zx'), 'process_zx.log')

    def test_split_path(self):
        pathCreator = PathCreator()
        utils.create_path(pathCreator, '/path')
        self.assertEqual(len(pathCreator.dirs), 1)
        self.assertEqual(pathCreator.dirs[0], '/path')

        pathCreator = PathCreator()
        utils.create_path(pathCreator, '/path1/a/b/path2')
        self.assertEqual(len(pathCreator.dirs), 4)
        self.assertEqual(pathCreator.dirs[0], '/path1')
        self.assertEqual(pathCreator.dirs[1], '/path1/a')
        self.assertEqual(pathCreator.dirs[2], '/path1/a/b')
        self.assertEqual(pathCreator.dirs[3], '/path1/a/b/path2')

    def test_mbi_fp_params_differs_ff_program(self):
        current_mbi_dict = {}
        publish_mbi_dict = {}

        for key in feedparser_shopsdat_options:
            current_mbi_dict[key] = key
            publish_mbi_dict[key] = key

        # check that ff_program in feedparser_shopsdat_options
        self.assertTrue(current_mbi_dict.get('ff_program') is not None)
        self.assertTrue(current_mbi_dict.get('enable_auto_discounts') is not None)

        # check that reparsing happens at change of the field
        self.assertFalse(mbi_fp_params_differs(current_mbi_dict, publish_mbi_dict))
        current_mbi_dict['ff_program'] = 'new_value'
        self.assertTrue(mbi_fp_params_differs(current_mbi_dict, publish_mbi_dict))

    def test_mbi_fp_params_differs_sells_jewelry(self):
        current_mbi_dict = {}
        publish_mbi_dict = {}

        for key in feedparser_shopsdat_options:
            current_mbi_dict[key] = key
            publish_mbi_dict[key] = key

        # check that sells_jewelry in feedparser_shopsdat_options
        self.assertTrue(current_mbi_dict.get('sells_jewelry') is not None)

        # check that reparsing happens at change of the field
        self.assertFalse(mbi_fp_params_differs(current_mbi_dict, publish_mbi_dict))
        current_mbi_dict['sells_jewelry'] = 'new_value'
        self.assertTrue(mbi_fp_params_differs(current_mbi_dict, publish_mbi_dict))


if '__main__' == __name__:
    unittest.main()
