# coding: utf-8
from lxml import etree
import os
import yatest.common
import csv
import sys

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.resources.yt_table_resource import YtTableResource


class YtAwapsModelsTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, yt_stuff, bin_flags=None, xml_output=True, output_file_name_suffixes=None, partition_config=None, **resources):
        super(YtAwapsModelsTestEnv, self).__init__(**resources)
        self.yt_stuff = yt_stuff
        self.bin_flags = bin_flags
        self.xml_output = xml_output
        self.output_file_name = 'offers.{}'.format('xml' if xml_output else 'csv')
        self.output_file_name_suffixes = output_file_name_suffixes if output_file_name_suffixes else ['']
        if partition_config:
            self.partition_config_path = 'partition_config.json'
            os.makedirs(self.output_dir)
            with open(os.path.join(self.output_dir, self.partition_config_path), 'w') as config_file:
                config_file.write(partition_config)
        else:
            self.partition_config_path = None

    @property
    def description(self):
        return 'market_banner_models'

    def execute(self, path=None, ex=False):
        if path is None:
            suffix = ('-ex' if ex else '')
            relative_path = os.path.join('market', 'idx', 'export', 'awaps',
                                         'market-banner-models',
                                         'bin' + suffix, 'market-banner-models' + suffix)
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        cmd = [
            path,
            '--proxy', self.yt_stuff.get_server(),
            '--output', os.path.join(self.output_dir, self.output_file_name)
        ]
        if self.bin_flags:
            cmd.extend(self.bin_flags)
        if self.partition_config_path:
            cmd.extend(['--partition-config', self.partition_config_path])

        sys.stderr.write('CMD: ' + repr(cmd) + '\n')

        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=True
        )
        if self.exec_result.returncode != 0:
            sys.stderr.write('STD_ERR: ' + repr(self.exec_result.std_err).replace('\\n', '\n') + '\n')
        self.update_output()

    def __exit__(self, *args):
        super(YtAwapsModelsTestEnv, self).__exit__(*args)

        yt = self.yt_stuff.get_yt_client()
        for res in self.resources.values():
            if isinstance(res, YtTableResource):
                yt.remove(res.table_path)

    def update_output(self):
        for suffix in self.output_file_name_suffixes:
            path, ext = os.path.splitext(os.path.join(self.output_dir, self.output_file_name))
            new_path = path + suffix + ext
            with open(new_path) as fd:
                self.outputs.update({
                    'offers' + suffix: etree.fromstring(fd.read()) if self.xml_output else [i for i in csv.reader(fd)]
                })
