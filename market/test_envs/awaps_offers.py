# coding: utf-8

import os
import yatest.common

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.resources.yt_table_resource import YtTableResource

_STUBS_DIR = os.path.join(
    yatest.common.source_path(),
    'market', 'idx', 'export', 'awaps', 'yatf', 'resources', 'stubs'
)


class YtAwapsOffersTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    _STUBS = {
        name: FileResource(os.path.join(_STUBS_DIR, filename))
        for name, filename in {
            'currency_rates.xml': 'currency_rates.xml',
            'global_vendors_xml': 'global.vendors.xml',
        }.items()
    }

    def __init__(self, use_op, **resources):
        super(YtAwapsOffersTestEnv, self).__init__(**resources)
        self.use_op = use_op

    @property
    def result_table(self):
        return self.outputs.get('result_table')

    def execute(self, yt_stuff, input_table, output_table, path=None):
        if path is None:
            relative_path = os.path.join('market', 'idx', 'export', 'awaps',
                                         'market-awaps-offers',
                                         'bin', 'market-awaps-offers')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        currency_xml = os.path.join(self.input_dir, self.resources['currency_rates.xml'].filename)
        global_vendors = os.path.join(self.input_dir, self.resources['global_vendors_xml'].filename)

        cmd = [
            path,
            '--use-yt', 'True',
            '--genlog-table', input_table,
            '--currency', currency_xml,
            '--global-vendors', global_vendors,
            '--output-table', output_table,
            '--yt-proxy', yt_stuff.get_server(),
            '--use-op', str(self.use_op)
        ]
        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=False
        )

        self.outputs.update({
            'result_table': YtTableResource(yt_stuff, output_table, load=True)
        })
