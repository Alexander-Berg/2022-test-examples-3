# coding: utf-8

import os
import yatest.common

from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.matchers.env_matchers import HasExitCode, HasOutputFiles
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.generation.catengine.yatf.resources.idfs import Idfs
from market.idx.generation.catengine.yatf.resources.model2profile import Model2Profile

_STUBS_DIR = os.path.join(
    yatest.common.source_path(),
    'market', 'idx', 'generation', 'catengine', 'yatf', 'resources', 'stubs'
)


class CatEngineBuilderTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0),
        HasOutputFiles({
            'index.catm',
            'index.catm_big_vectors',
            'index.catm_doc2vec',
            'index.catm_hyper2vec',
            'index.catm_ids_mapping',
            'index.catm_small_vectors',
        }),
    ]

    _STUBS = {
        # Файл служит фильтром по моделям (модели из шарда индекса) при построении CatEngine-индекса.
        # В стабе сохранены идентификаторы моделей: 1, 2, 100, 101, 3.
        'indexaa': FileResource(os.path.join(_STUBS_DIR, 'indexaa'))
    }

    def __init__(self, yt_stuff, maxidf='20', index_from_yt=False,  type=None, **resources):
        self.yt_stuff = yt_stuff
        super(CatEngineBuilderTestEnv, self).__init__(**resources)

        self.resources['idfs'] = resources.get(
            'idfs',
            Idfs(self.yt_stuff, [])
        )
        self.resources['model2profile'] = resources.get(
            'model2profile',
            Model2Profile(os.path.join(self.index_dir, 'model2profile.mmap'), [])
        )

        self.maxidf = maxidf
        self.index_from_yt = index_from_yt
        self.type = type

    @property
    def description(self):
        return 'catengine_builder'

    @property
    def index_dir(self):
        relative_path = os.path.join('market', 'idx', 'generation', 'catengine')
        return yatest.common.binary_path(relative_path)

    @property
    def catengine_indexer(self):
        return os.path.join(self.index_dir, 'catengine_indexer')

    def execute(self):
        idfs = self.resources['idfs']
        model2profile = self.resources['model2profile']
        model2profile.dump()
        indexaa = self.resources['indexaa']

        cmd = [
            self.catengine_indexer,
            'build',
            '--idf', idfs.get_yt_path(),
            '--yt-proxy', self.yt_stuff.get_server(),
            '--model2profile', model2profile.path,
            '--maxidf', self.maxidf,
            '--result', self.index_dir,
        ]
        if self.index_from_yt:
            cmd.extend(['--index-from-yt', '--index', indexaa.table_path])
        else:
            cmd.extend(['--index', os.path.dirname(indexaa.path)])
        if self.type:
            for type in self.type:
                cmd.extend(['--type', type])

        self.exec_result = self.try_execute_under_gdb(cmd)
