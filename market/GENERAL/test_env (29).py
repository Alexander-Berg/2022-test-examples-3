# coding: utf-8

import os
import yatest.common

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv

from market.idx.yatf.resources.yt_table_resource import YtTableResource, YtFileResource

_STUBS_DIR = os.path.join(yatest.common.source_path(), 'market', 'idx', 'yatf', 'resources', 'stubs', 'getter', 'geobase')


class GrsBannerConverterTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, yt_stuff,  **resources):
        super(GrsBannerConverterTestEnv, self).__init__(**resources)

        self.resources = {
            name: YtFileResource(yt_stuff, os.path.join(_STUBS_DIR, filename))
            for name, filename in {
                'geotree': 'geo.c2p',
                'geoinfo': 'geobase.xml'
            }.items()
        }

    @property
    def description(self):
        return 'grs4banner_converter'

    def execute(self, yt_stuff, output_table, input_table, regions, min_offers=2):
        relative_path = os.path.join(
            'market',
            'tools',
            'grs4banner-converter',
            'src',
            'grs4banner-converter'
        )
        absolute_path = yatest.common.binary_path(relative_path)

        proxy = yt_stuff.get_server()

        command = [
            absolute_path,
            '--input', input_table,
            '--output', output_table,
            '--proxy', proxy,
            '--min-offers', str(min_offers),
            '--geoc2p', self.resources['geotree'].get_yt_path(),
            '--geo-xml', self.resources['geoinfo'].get_yt_path()
        ]

        for region in regions:
            command.append('--region')
            command.append(str(region))

        self.exec_result = self.try_execute_under_gdb(
            command,
            cwd=self.output_dir,
            check_exit_code=True
        )

        self.outputs.update(
            {
                "result_table": YtTableResource(yt_stuff, output_table, load=True)
            }
        )
