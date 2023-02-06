# coding: utf-8

import os
import yatest.common

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.matchers.env_matchers import HasExitCode, HasOutputFiles
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.generation.catengine.yatf.resources.cat_engine_profile_table import CatEngineProfileTable
from market.idx.generation.catengine.yatf.resources.model2profile import Model2Profile


class CatEnginePreprocessorTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0),
        HasOutputFiles({
            'model2profile.mmap',
        }),
    ]

    def __init__(self, yt_stuff, **resources):
        self.yt_stuff = yt_stuff
        super(CatEnginePreprocessorTestEnv, self).__init__(**resources)

        self.resources['profiles'] = resources.get(
            'profiles',
            CatEngineProfileTable(self.yt_stuff, os.path.join(get_yt_prefix(), 'in', 'profiles'), [])
        )

    @property
    def description(self):
        return 'catengine_preprocessing'

    @property
    def index_dir(self):
        relative_path = os.path.join('market', 'idx', 'generation', 'catengine')
        return yatest.common.binary_path(relative_path)

    @property
    def model2profile(self):
        return os.path.join(self.index_dir, 'model2profile.mmap')

    @property
    def catengine_indexer(self):
        return os.path.join(self.index_dir, 'catengine_indexer')

    def execute(self):
        profiles = self.resources['profiles']

        cmd = [
            self.catengine_indexer,
            'preprocess',
            '--profiles', profiles.table_path,
            '--yt-proxy', self.yt_stuff.get_server(),
            '--model2profile', self.model2profile,
        ]

        self.exec_result = self.try_execute_under_gdb(cmd)

        self.outputs.update({'model2profile': Model2Profile(self.model2profile, [])})
