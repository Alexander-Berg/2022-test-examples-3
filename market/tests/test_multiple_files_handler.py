import configparser
import json
import logging
import os
import shutil
import tempfile
import time
import unittest

from sepelib.util.fs import atomic_write

from market.idx.pylibrary.its_monitor import ItsMonitor, MultipleConfigHandler


class TestNoReload(unittest.TestCase):
    def setUp(self):
        self.log = logging.getLogger()
        self.work_dir = tempfile.mkdtemp()
        self.input_path = os.path.join(self.work_dir, "input.json")
        self.second_input_path = os.path.join(self.work_dir, "input2.json")
        self.output_path_json = os.path.join(self.work_dir, "output.json")
        self.output_path_ini = os.path.join(self.work_dir, "output.ini")
        open(self.input_path, 'a').close()
        open(self.second_input_path, 'a').close()
        open(self.output_path_json, 'a').close()
        open(self.output_path_ini, 'a').close()

    def tearDown(self):
        shutil.rmtree(self.work_dir)

    def test_simple(self):
        handler = MultipleConfigHandler(self.input_path, self.log)
        with ItsMonitor(self.input_path, handler, self.log):
            with open(self.input_path, 'w') as f:
                data = {'restart_service': False, 'files': []}
                data['files'].append({'path': self.output_path_ini, 'format': 'ini', 'values': {'test': {'this': 'abc'}}})
                data['files'].append({'path': self.output_path_json, 'format': 'json', 'values': {'test2': {'this2': 'abc2'}}})
                f.write(json.dumps(data))
            time.sleep(0.1)
        output_config = configparser.ConfigParser()
        output_config.read(self.output_path_ini)
        self.assertEqual(output_config.sections(), ['test'])
        self.assertEqual(len(output_config['test']), 1)
        self.assertEqual(output_config['test']['this'], 'abc')
        with open(self.output_path_json) as f:
            output_config = json.load(f)
            self.assertTrue(len(output_config) == 1)
            self.assertTrue(len(output_config['test2']) == 1)
            self.assertEqual(output_config['test2']['this2'], 'abc2')

    def test_rewrite(self):
        handler = MultipleConfigHandler(self.input_path, self.log)
        with ItsMonitor(self.input_path, handler, self.log):
            data = {'restart_service': False, 'files': []}
            data['files'].append({'path': self.output_path_ini, 'format': 'ini', 'values': {'test': {'this': 'abc'}}})
            data['files'].append({'path': self.output_path_json, 'format': 'json', 'values': {'test2': {'this2': 'abc2'}}})
            with open(self.input_path, 'w') as f:
                print(json.dumps(data))
                f.write(json.dumps(data))
            time.sleep(0.1)
            with open(self.input_path, 'w') as f:
                data['files'][0]['values'] = {'test1': {'this1': 'def'}}
                data['files'][1]['values'] = {'test3': {'this3': 'abc3'}}
                print(json.dumps(data))
                f.write(json.dumps(data))
            time.sleep(0.1)
        output_config = configparser.ConfigParser()
        output_config.read(self.output_path_ini)
        self.assertEqual(output_config.sections(), ['test1'])
        self.assertEqual(len(output_config['test1']), 1)
        self.assertEqual(output_config['test1']['this1'], 'def')
        with open(self.output_path_json) as f:
            output_config = json.load(f)
            self.assertTrue(len(output_config) == 1)
            self.assertTrue(len(output_config['test3']) == 1)
            self.assertEqual(output_config['test3']['this3'], 'abc3')

    def test_empty_values(self):
        handler = MultipleConfigHandler(self.input_path, self.log)
        with ItsMonitor(self.input_path, handler, self.log):
            with open(self.input_path, 'w') as f:
                data = {'restart_service': False, 'files': []}
                data['files'].append({'path': self.output_path_ini, 'format': 'ini', 'values': {}})
                data['files'].append({'path': self.output_path_json, 'format': 'json', 'values': {}})
                f.write(json.dumps(data))
            time.sleep(0.1)
        output_config = configparser.ConfigParser()
        output_config.read(self.output_path_ini)
        self.assertEqual(output_config.sections(), [])
        with open(self.output_path_json) as f:
            output_config = f.read()
            self.assertEqual(output_config, '{}')

    def test_empty_file(self):
        handler = MultipleConfigHandler(self.input_path, self.log)
        with ItsMonitor(self.input_path, handler, self.log):
            with open(self.input_path, 'w') as f:
                f.write('')
            time.sleep(0.1)
        output_config = configparser.ConfigParser()
        output_config.read(self.output_path_ini)
        self.assertEqual(output_config.sections(), [])
        self.assertEqual(os.path.getsize(self.output_path_json), 0)

    def test_bad_json(self):
        handler = MultipleConfigHandler(self.input_path, self.log)
        with ItsMonitor(self.input_path, handler, self.log):
            with open(self.input_path, 'w') as f:
                f.write('{"restart_service": false, "values": {"something_wrong"=1}}')
            time.sleep(0.1)
        output_config = configparser.ConfigParser()
        output_config.read(self.output_path_ini)
        self.assertEqual(output_config.sections(), [])
        self.assertEqual(os.path.getsize(self.output_path_json), 0)

    def test_write_atomic(self):
        handler = MultipleConfigHandler(self.input_path, self.log)
        with ItsMonitor(self.input_path, handler, self.log):
            data = {'restart_service': False, 'files': []}
            data['files'].append({'path': self.output_path_ini, 'format': 'ini', 'values': {'testIni': {'thisIni': 'abcIni'}}})
            data['files'].append({'path': self.output_path_json, 'format': 'json', 'values': {'testJson': {'thisJson': 'abcJson'}}})
            atomic_write(self.input_path, json.dumps(data))
            time.sleep(1)
            output_config = configparser.ConfigParser()
            output_config.read(self.output_path_ini)
            self.assertEqual(output_config.sections(), ['testIni'])
            self.assertEqual(len(output_config['testIni']), 1)
            self.assertEqual(output_config['testIni']['thisIni'], 'abcIni')
            with open(self.output_path_json) as f:
                output_config = json.load(f)
                self.assertTrue(len(output_config) == 1)
                self.assertTrue(len(output_config['testJson']) == 1)
                self.assertEqual(output_config['testJson']['thisJson'], 'abcJson')

            data['files'][0]['values'] = {'testIni1': {'thisIni1': 'abcIni1'}}
            data['files'][1]['values'] = {'testJson1': {'thisJson1': 'abcJson1'}}
            atomic_write(self.input_path, json.dumps(data))
            time.sleep(1)
            output_config = configparser.ConfigParser()
            output_config.read(self.output_path_ini)
            self.assertEqual(output_config.sections(), ['testIni1'])
            self.assertEqual(len(output_config['testIni1']), 1)
            self.assertEqual(output_config['testIni1']['thisIni1'], 'abcIni1')
            with open(self.output_path_json) as f:
                output_config = json.load(f)
                self.assertTrue(len(output_config) == 1)
                self.assertTrue(len(output_config['testJson1']) == 1)
                self.assertEqual(output_config['testJson1']['thisJson1'], 'abcJson1')

            data['files'][0]['values'] = {'testIni2': {'thisIni2': 'abcIni2'}}
            data['files'][1]['values'] = {'testJson2': {'thisJson2': 'abcJson2'}}
            atomic_write(self.input_path, json.dumps(data))
            time.sleep(1)
            output_config = configparser.ConfigParser()
            output_config.read(self.output_path_ini)
            self.assertEqual(output_config.sections(), ['testIni2'])
            self.assertEqual(len(output_config['testIni2']), 1)
            self.assertEqual(output_config['testIni2']['thisIni2'], 'abcIni2')
            with open(self.output_path_json) as f:
                output_config = json.load(f)
                self.assertTrue(len(output_config) == 1)
                self.assertTrue(len(output_config['testJson2']) == 1)
                self.assertEqual(output_config['testJson2']['thisJson2'], 'abcJson2')

    def test_multiple_inputs(self):
        handler = MultipleConfigHandler([self.input_path, self.second_input_path], self.log)
        with ItsMonitor([self.input_path, self.second_input_path], handler, self.log):
            data = {'restart_service': False, 'files': []}
            data['files'].append({'path': self.output_path_ini, 'format': 'ini', 'values': {'testIni': {'thisIni': 'abcIni'}}})
            data['files'].append({'path': self.output_path_json, 'format': 'json', 'values': {'testJson': {'thisJson': 'abcJson'}}})
            atomic_write(self.input_path, json.dumps(data))
            data['files'][1]['values']['testJson']['thisJson'] = 'defJson'
            data['files'].append({'path': self.output_path_ini, 'format': 'ini', 'values': {'testIni': {'thisIni': 'abcIni'}}})
            data['files'].append({'path': self.output_path_json, 'format': 'json', 'values': {'testJson': {'thisJson': 'defJson'}, 'testJson2': 'abcJson'}})
            atomic_write(self.second_input_path, json.dumps(data))
            time.sleep(1)
            output_config = configparser.ConfigParser()
            output_config.read(self.output_path_ini)
            self.assertEqual(output_config.sections(), ['testIni'])
            self.assertEqual(len(output_config['testIni']), 1)
            self.assertEqual(output_config['testIni']['thisIni'], 'abcIni')
            with open(self.output_path_json) as f:
                output_config = json.load(f)
                self.assertTrue(len(output_config) == 2)
                self.assertTrue(len(output_config['testJson']) == 1)
                self.assertEqual(output_config['testJson']['thisJson'], 'defJson')
                self.assertEqual(output_config['testJson2'], 'abcJson')

    def test_reload_from_source(self):
        handler = MultipleConfigHandler([self.input_path, self.second_input_path], self.log, True)
        data = {'restart_service': True, 'files': []}
        data['files'].append({'path': self.output_path_ini, 'format': 'ini', 'values': {'testIni': {'thisIni': 'abcIni'}}})
        data['files'].append({'path': self.output_path_json, 'format': 'json', 'values': {'testJson': {'thisJson': 'defJson'}, 'testJson2': 'abcJson'}})
        data['files'][1]['values']['testJson']['thisJson'] = 'defJson'
        atomic_write(self.second_input_path, json.dumps(data))
        with ItsMonitor([self.input_path, self.second_input_path], handler, self.log):
            data = {'restart_service': False, 'files': []}
            data['files'].append({'path': self.output_path_ini, 'format': 'ini', 'values': {'testIni': {'thisIni': 'abcIni'}}})
            data['files'].append({'path': self.output_path_json, 'format': 'json', 'values': {'testJson': {'thisJson': 'abcJson'}}})
            atomic_write(self.input_path, json.dumps(data))
            time.sleep(1)
            output_config = configparser.ConfigParser()
            output_config.read(self.output_path_ini)
            self.assertEqual(output_config.sections(), ['testIni'])
            self.assertEqual(len(output_config['testIni']), 1)
            self.assertEqual(output_config['testIni']['thisIni'], 'abcIni')
            with open(self.output_path_json) as f:
                output_config = json.load(f)
                self.assertTrue(len(output_config) == 2)
                self.assertTrue(len(output_config['testJson']) == 1)
                self.assertEqual(output_config['testJson']['thisJson'], 'defJson')
                self.assertEqual(output_config['testJson2'], 'abcJson')

if __name__ == '__main__':
    unittest.main()
