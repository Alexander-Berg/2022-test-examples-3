# -*- coding: utf-8 -*-

import os
import subprocess
import base64
import json
import uuid

from market.idx.yatf.common import get_binary_path
from market.idx.yatf.test_envs.base_env import BaseEnv


class PqToolTestEnv(BaseEnv):
    def __init__(self, **kwargs):
        super(PqToolTestEnv, self).__init__()

        relative_bin_path = os.path.join('market', 'tools', 'pqtool', 'src', 'pqtool')
        self.binary = get_binary_path(relative_bin_path)

        self.host = str(kwargs.get('host', 'localhost'))
        self.port = kwargs['port']
        self.topic = str(kwargs['topic'])
        # если не указано, то будет читать/писать из всех партиций сразу
        self.partition = kwargs.get('partition', None)
        self.timeout = int(kwargs.get('timeout', 30))

    def __enter__(self):
        super(PqToolTestEnv, self).__enter__()

    def __exit__(self, *args):
        super(PqToolTestEnv, self).__exit__(*args)

    def get_base_cmd_args(self):
        args = [
            self.binary,
            '--host', self.host,
            '--port', str(self.port),
            '--topic', self.topic,
        ]

        if self.partition:
            args.extend(['--partition', str(self.partition)])

        return args


class PqToolWriterEnv(PqToolTestEnv):
    def __init__(self, **kwargs):
        super(PqToolWriterEnv, self).__init__(**kwargs)
        self.source_id = str(kwargs.get('source_id', 'test_source_id' + str(uuid.uuid4())))
        self.data = kwargs['data']

    def __enter__(self):
        super(PqToolWriterEnv, self).__enter__()
        args = self.get_base_cmd_args() + [
            '--source-id', self.source_id,
            '--mode', 'write'
        ]
        self.exec_result = self.try_execute_under_gdb(
            args,
            cwd=self.output_dir,
            check_exit_code=True,
            wait=False,
            stdin=subprocess.PIPE,
        )
        self.exec_result.process.communicate('\n'.join(self.data), timeout=self.timeout)
        self.exec_result.wait()
        return self

    @property
    def description(self):
        return 'logbroker_writer'


class PqToolReaderEnv(PqToolTestEnv):
    def __init__(self, **kwargs):
        super(PqToolReaderEnv, self).__init__(**kwargs)
        self.read_count = int(kwargs.get('read_count', 0))
        self.no_commit = bool(kwargs.get('no_commit', False))
        self.results = []
        self.is_run = False
        self.lb_client_id = 'test'

    def __enter__(self):
        super(PqToolReaderEnv, self).__enter__()
        args = self.get_base_cmd_args() + [
            '--lb-client-id', self.lb_client_id,
            '--read-count', str(self.read_count),
            '--mode', 'read',
            '--output-format', 'json_base64',
        ]
        if self.no_commit:
            args += ['--no-commit']

        self.exec_result = self.try_execute_under_gdb(
            args,
            cwd=self.output_dir,
            check_exit_code=True,
            wait=False,
        )
        self.is_run = True
        return self

    def wait(self):
        self.is_run = False
        self.exec_result.wait(timeout=self.timeout)
        self.results = []
        lines = [line for line in self.exec_result.std_out.split('\n') if line]
        self.results = [base64.b64decode(json.loads(line)['data']) for line in lines]

    def __exit__(self, *args):
        super(PqToolReaderEnv, self).__exit__(*args)
        if self.is_run:
            self.wait()

    @property
    def description(self):
        return 'logbroker_reader'
