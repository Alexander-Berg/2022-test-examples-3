# coding: utf-8

import os
import yatest.common

from market.idx.yatf.resources.indexaa import IndexAa
from market.idx.yatf.resources.indexarc import IndexArc
from market.idx.generation.yatf.resources.wizard_indexer.wizard_conf import WizardConf
from market.pylibrary.memoize.memoize import memoize
from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.test_envs.base_env import BaseEnv


def _STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'generation', 'yatf',
        'resources',
        'wizard_indexer',
        'stubs',
    )


class WizardIndexerTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def path_to(self, resource):
        return os.path.join(
            self.input_dir,
            self.resources[resource].filename
        )

    def __init__(self, **resources):
        self._STUBS = {
            name: FileResource(os.path.join(_STUBS_DIR(), filename))
            for name, filename in {
                'parser_config': 'PARSER_CONFIG',
                'tovar_tree_pb': 'tovar-tree.pb',
                'wizard_cfg': 'wizard.cfg',
            }.items()
        }
        super(WizardIndexerTestEnv, self).__init__(**resources)
        wizard_cfg = WizardConf(
            yandex_categories_tree=self.resources['tovar_tree_pb'].path
        )
        self.resources['wizard_cfg'] = resources.get('wizard_cfg', wizard_cfg)

    @property
    def description(self):
        return 'wizard_indexer'

    @property
    def index_dir(self):
        return self.resources['wizard_cfg'].index_dir

    @property
    @memoize()
    def categories(self):
        indexarc = self.outputs['indexarc']
        indexaa = self.outputs['indexaa']

        outputs = [
            indexarc,
            indexaa
        ]

        for output_file in outputs:
            output_file.load()

        categories = dict()
        for doc_id in indexarc.doc_ids:
            categories[doc_id] = indexarc.load_doc_description(doc_id)
            categories[doc_id].update(indexaa.get_group_attributes(doc_id))
        return categories

    def execute(self, path=None):
        if path is None:
            relative_path = os.path.join(
                'market', 'idx', 'generation', 'wizard_indexer', 'wizard_indexer'
            )
            path = yatest.common.binary_path(relative_path)

        cmd = [
            path, '-Config={}'.format(self.path_to('wizard_cfg')),
        ]

        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=False
        )

        self.outputs.update({
            'indexarc': IndexArc(os.path.join(self.index_dir, 'indexarc')),
            'indexaa': IndexAa(os.path.join(self.index_dir, 'indexaa')),
        })
