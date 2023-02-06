# coding: utf-8

import os
import yatest.common

from market.idx.generation.yatf.resources.cards_indexer.cards_conf import CardsConf
from market.idx.yatf.resources.indexaa import IndexAa
from market.idx.yatf.resources.indexarc import IndexArc
from market.pylibrary.memoize.memoize import memoize
from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.resource import (
    FileResource,
    DirectoryFileResource,
)
from market.idx.yatf.test_envs.base_env import BaseEnv


def _STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'generation', 'yatf',
        'resources',
        'cards_indexer',
        'stubs',
    )


class CardsIndexerTestEnv(BaseEnv):
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
                'cataloger_navigation_xml': 'cataloger.navigation.xml',
                'category_clicks_txt': 'category-clicks.txt',
                'gl_mbo_pbuf_sn': 'gl_mbo.pbuf.sn',
                'global_vendors_xml': 'global.vendors.xml',
                'guru_categories_pbuf_sn': 'guru-categories.pbuf.sn',
                'market_cards_cfg': 'market-cards.cfg',
                'parser_config': 'PARSER_CONFIG',
                'tovar_tree_pb': 'tovar-tree.pb',
                'visual_vendor_aliases_xml': 'visual-vendor-aliases.xml',
            }.items()
        }
        self._STUBS.update({
            'models': DirectoryFileResource(os.path.join(_STUBS_DIR(), 'models')),
        })
        super(CardsIndexerTestEnv, self).__init__(**resources)
        cards_cfg = CardsConf(
            gl_mbo_pb_file=self.resources['gl_mbo_pbuf_sn'].path,
            gloval_vendors=self.resources['global_vendors_xml'].path,
            guru_data_light=self.resources['guru_categories_pbuf_sn'].path,
            navigation_info=self.resources['cataloger_navigation_xml'].path,
            popularity_map=self.resources['category_clicks_txt'].path,
            vendor_aliases=self.resources['visual_vendor_aliases_xml'].path,
            visual_stuff_dir=self.resources['models'].path,
            yandex_categories_tree=self.resources['tovar_tree_pb'].path
        )
        self.resources['market_cards_cfg'] = resources.get('market_cards_cfg', cards_cfg)

    @property
    def description(self):
        return 'cards_indexer'

    @property
    def index_dir(self):
        return self.resources['market_cards_cfg'].index_dir

    @property
    def vendor_aliases(self):
        return self.resources['visual_vendor_aliases_xml'].path

    @property
    @memoize()
    def cards(self):
        indexarc = self.outputs['indexarc']
        indexaa = self.outputs['indexaa']

        outputs = [
            indexarc,
            indexaa
        ]

        for output_file in outputs:
            output_file.load()

        cards = dict()
        for doc_id in indexarc.doc_ids:
            cards[doc_id] = indexarc.load_doc_description(doc_id)
            cards[doc_id].update(indexaa.get_group_attributes(doc_id))
        return cards

    def execute(self, path=None):
        if path is None:
            relative_path = os.path.join(
                'market', 'idx', 'cards', 'cards_indexer', 'cards_indexer'
            )
            path = yatest.common.binary_path(relative_path)

        cmd = [
            path, '-Config={}'.format(self.path_to('market_cards_cfg')),
        ]

        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=False,
        )

        self.outputs.update({
            'indexarc': IndexArc(os.path.join(self.index_dir, 'indexarc')),
            'indexaa': IndexAa(os.path.join(self.index_dir, 'indexaa')),
        })
