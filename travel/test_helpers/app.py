# encoding: utf-8

import requests
import time
import os
import json
import shutil
from jinja2 import Template
from builtins import range
from google.protobuf import text_format

import yatest


class HttpApp(object):
    def __init__(self, port):
        self.port = port

    def get(self, path, params=None, **kwargs):
        return requests.get('http://localhost:%s/%s' % (self.port, path), params=params, **kwargs)

    def checked_get(self, path, params=None, **kwargs):
        resp = self.get(path, params, **kwargs)
        resp.raise_for_status()
        return resp

    def wait_ready(self, method='ping'):
        for x in range(300):
            time.sleep(0.1)
            try:
                self.checked_get(method)
                break
            except Exception as e:
                print('Exc while waiting for startup: %s' % str(e))
        else:
            raise Exception('Failed to wait app is active')


class ConfigApp(object):
    def __init__(self, service_name):
        self.service_name = service_name
        self.config_path = os.path.join(yatest.common.work_path(), 'config', self.service_name)
        self.base_config = None

    def prepare_config(self, config_type, **params):
        if not os.path.exists(self.config_path):
            os.makedirs(self.config_path)
        orig_config_path = yatest.common.source_path('travel/hotels/devops/config/{0}/{0}-app.config'.format(self.service_name))
        target_config_name = os.path.join(self.config_path, os.path.basename(orig_config_path))
        if os.path.exists(target_config_name):
            raise Exception('Config file "{}" already exists'.format(target_config_name))
        shutil.copy(orig_config_path, self.config_path)
        self.templatize(**params)

        with open(orig_config_path) as f:
            self.base_config = config_type()
            text_format.Parse(f.read(), self.base_config)

    def templatize(self, **params):
        in_file = yatest.common.source_path('travel/hotels/{0}/tests/template.{0}.config'.format(self.service_name))
        out_file = os.path.join(self.config_path, '{0}-app-testing.config'.format(self.service_name))
        template = Template(open(in_file).read())
        result = template.render(**params)
        with open(out_file, 'w') as f:
            f.write(result)
        return out_file


class YtApp(object):
    @staticmethod
    def write_json_to_yt(yt_stuff, json_path, table_path):
        with open(yatest.common.source_path(json_path)) as f:
            YtApp.write_data_to_yt(yt_stuff, json.load(f), table_path)

    @staticmethod
    def write_data_to_yt(yt_stuff, rows, table_path):
        yt_stuff.yt_client.create('map_node', os.path.dirname(table_path), recursive=True, ignore_existing=True)
        yt_stuff.yt_client.write_table(table_path, rows, raw=False)


class BaseApp(HttpApp, ConfigApp, YtApp):
    def __init__(self, service_name, http_port):
        HttpApp.__init__(self, http_port)
        ConfigApp.__init__(self, service_name)
        YtApp.__init__(self)

    def start(self, *params):
        bin_path = yatest.common.binary_path('travel/hotels/{0}/bin/{0}'.format(self.service_name))
        p = [bin_path, '-c', self.config_path, '-e', 'testing', '-d']
        p.extend(params)
        exec_obj = yatest.common.execute(p, wait=False)
        self.exec_obj = exec_obj

    def stop(self):
        self.get('shutdown')
        self.exec_obj.wait()
