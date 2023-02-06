# -*- coding: utf-8 -*-

import contextlib
import jinja2
import os
import sys
from cStringIO import StringIO

import yatest.common

import common


def generate_monitoring_config(port=17050, is_snippet=False):
    common.forget_config()

    CONFIG_NAME = 'monitoring.conf'
    config_path = os.path.join(yatest.common.work_path(), CONFIG_NAME)
    jinja_loader = jinja2.FileSystemLoader(yatest.common.source_path('market/report/runtime_cloud/juggler_bundle/zeus_tmpl/'))
    jinja_env = jinja2.Environment(loader=jinja_loader, undefined=jinja2.StrictUndefined)
    jinja_template = jinja_env.get_template(CONFIG_NAME)
    work_path = yatest.common.output_path()
    context = {
        'rc_env': {
            'paths': {
                'root': work_path,
                'bin': work_path,
                'conf': work_path,
                'data': work_path,
                'pdata': work_path,
                'search': work_path,
                'logs': work_path,
                'state': work_path,
                'pstate': work_path,
                'index_storage': work_path,
            },
            'report': {
                'role': 'market-report',
                'subrole': 'market',
                'is_snippet': is_snippet,
            },
            'ports': {
                'root': port,
            },
            'host': {
                'environment': 'production',
                'location': 'sas',
            },
        },
    }
    with open(config_path, 'w') as f:
        f.write(jinja_template.render(context))
    return config_path


@contextlib.contextmanager
def colorize_cluster_in_indigo():
    common.forget_indigo()
    lockdown_path = os.path.join(common.config().pdata_directory, 'lock', 'market_report_lockdown')
    lockdown_path_dir = os.path.dirname(lockdown_path)
    if not os.path.exists(lockdown_path_dir):
        os.makedirs(lockdown_path_dir)
    with open(lockdown_path, 'wt') as f:
        f.write('user\n')
    try:
        yield
    finally:
        common.forget_indigo()
        os.remove(lockdown_path)


class OutputCapture(object):
    def __init__(self):
        self._stdout = None
        self._stderr = None
        self._strout = StringIO()
        self._strerr = StringIO()

    def __enter__(self):
        self._stdout = sys.stdout
        self._stderr = sys.stderr
        sys.stdout = self._strout
        sys.stderr = self._strerr
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        sys.stdout = self._stdout
        sys.stderr = self._stderr

    def get_stdout(self):
        return self._strout.getvalue()

    def get_stderr(self):
        return self._strerr.getvalue()
