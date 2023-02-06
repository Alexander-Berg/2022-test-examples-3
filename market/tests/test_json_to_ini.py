import configparser
import logging
import os
import shutil
import tempfile
import time
import unittest

from sepelib.util.fs import atomic_write

from market.idx.pylibrary.its_monitor import ItsMonitor, JsonToIniConfigHandler


class TestNoReload(unittest.TestCase):
    def setUp(self):
        self.log = logging.getLogger()
        self.work_dir = tempfile.mkdtemp()
        self.input_path = os.path.join(self.work_dir, "input.json")
        self.output_path = os.path.join(self.work_dir, "output.json")
        open(self.input_path, 'a').close()
        open(self.output_path, 'a').close()

    def tearDown(self):
        shutil.rmtree(self.work_dir)

    def test_simple(self):
        handler = JsonToIniConfigHandler(self.input_path, self.output_path, self.log)
        with ItsMonitor(self.input_path, handler, self.log):
            with open(self.input_path, 'w') as f:
                f.write('{"restart_service": false, "values": {"test": {"this": "abc"}}}')
            time.sleep(0.1)
        output_config = configparser.ConfigParser()
        output_config.read(self.output_path)
        self.assertEqual(output_config.sections(), ['test'])
        self.assertEqual(len(output_config['test']), 1)
        self.assertEqual(output_config['test']['this'], 'abc')

    def test_rewrite(self):
        handler = JsonToIniConfigHandler(self.input_path, self.output_path, self.log)
        with ItsMonitor(self.input_path, handler, self.log):
            with open(self.input_path, 'w') as f:
                f.write('{"restart_service": false, "values": {"test": {"this": "abc"}}}')
            time.sleep(0.1)
            with open(self.input_path, 'w') as f:
                f.write('{"restart_service": false, "values": {"test2": {"this2": "def"}}}')
            time.sleep(0.1)
        output_config = configparser.ConfigParser()
        output_config.read(self.output_path)
        self.assertEqual(output_config.sections(), ['test2'])
        self.assertEqual(len(output_config['test2']), 1)
        self.assertEqual(output_config['test2']['this2'], 'def')

    def test_empty_values(self):
        handler = JsonToIniConfigHandler(self.input_path, self.output_path, self.log)
        with ItsMonitor(self.input_path, handler, self.log):
            with open(self.input_path, 'w') as f:
                f.write('{"restart_service": false, "values": {}}')
                time.sleep(0.1)
        output_config = configparser.ConfigParser()
        output_config.read(self.output_path)
        self.assertEqual(output_config.sections(), [])

    def test_empty_file(self):
        handler = JsonToIniConfigHandler(self.input_path, self.output_path, self.log)
        with ItsMonitor(self.input_path, handler, self.log):
            with open(self.input_path, 'w') as f:
                f.write('')
                time.sleep(0.1)
        output_config = configparser.ConfigParser()
        output_config.read(self.output_path)
        self.assertEqual(output_config.sections(), [])

    def test_bad_json(self):
        handler = JsonToIniConfigHandler(self.input_path, self.output_path, self.log)
        with ItsMonitor(self.input_path, handler, self.log):
            with open(self.input_path, 'w') as f:
                f.write('{"restart_service": false, "values": {"something_wrong"=1}}')
                time.sleep(0.1)
        output_config = configparser.ConfigParser()
        output_config.read(self.output_path)
        self.assertEqual(output_config.sections(), [])

    def test_write_atomic(self):
        handler = JsonToIniConfigHandler(self.input_path, self.output_path, self.log)
        with ItsMonitor(self.input_path, handler, self.log):
            atomic_write(self.input_path, '{"restart_service": false, "values": {"test": {"this": "abc"}}}')
            time.sleep(1)
            output_config = configparser.ConfigParser()
            output_config.read(self.output_path)
            self.assertEqual(output_config.sections(), ['test'])
            self.assertEqual(len(output_config['test']), 1)
            self.assertEqual(output_config['test']['this'], 'abc')

            atomic_write(self.input_path, '{"restart_service": false, "values": {"test1": {"this1": "abc1"}}}')
            time.sleep(1)
            output_config = configparser.ConfigParser()
            output_config.read(self.output_path)
            self.assertEqual(output_config.sections(), ['test1'])
            self.assertEqual(len(output_config['test1']), 1)
            self.assertEqual(output_config['test1']['this1'], 'abc1')

            atomic_write(self.input_path, '{"restart_service": false, "values": {"test2": {"this2": "abc2"}}}')
            time.sleep(1)
            output_config = configparser.ConfigParser()
            output_config.read(self.output_path)
            self.assertEqual(output_config.sections(), ['test2'])
            self.assertEqual(len(output_config['test2']), 1)
            self.assertEqual(output_config['test2']['this2'], 'abc2')


if __name__ == '__main__':
    unittest.main()
