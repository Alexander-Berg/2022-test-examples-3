# coding: utf-8

import os
import yatest.common
from yt.wrapper import ypath_join

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.generation.yatf.utils.fixtures import glue_config

from market.idx.generation.yatf.utils.mr_mindexer_fixtures import (
    create_genlog_table,
    shops_dat,
    tovar_tree,
)
from market.idx.yatf.resources.indexarc import IndexArc
from market.idx.yatf.resources.indexaa import IndexAa
from market.idx.generation.yatf.resources.mr_mindexer.mr_mindexer_helpers import MrMindexerBuildOptions

from market.idx.yatf.resources.indexkey_inv import Literals


def _IDX_STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'offers', 'yatf',
        'resources',
        'offers_indexer',
        'stubs',
    )


def mr_mindexer_build_options(generation):
    yt_prefix = get_yt_prefix()
    return MrMindexerBuildOptions(
        parts_cnt=1,
        yt_index_portions_path=ypath_join(yt_prefix, generation, 'portions'),
        yt_tmp_path=ypath_join(yt_prefix, generation, 'index_tmp')
    )


class MrMindexerBuildTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    GENERATION = '20200527_1807'

    def __init__(self, **resources):
        self._STUBS = {}

        all_resources = {
            'build_options': mr_mindexer_build_options(self.GENERATION),
            'shops_utf8_dat': shops_dat(),
            'tovar_tree_pb': tovar_tree(),
            'glue_config': glue_config()
        }
        all_resources.update(**resources)

        super(MrMindexerBuildTestEnv, self).__init__(**all_resources)

    @property
    def description(self):
        return 'mr_mindexer_build'

    def execute(self, yt_stuff, input_genlog_yt_path, binary_path=None):
        if binary_path is None:
            relative_path = os.path.join('market', 'idx', 'generation', 'mr', 'mr_mindexer')
            absolute_path = yatest.common.binary_path(relative_path)
            binary_path = absolute_path

        build_options = self.resources['build_options']
        cmd = [
            binary_path, 'build',
            '--proxy', yt_stuff.get_server(),
            '--generation', self.GENERATION,
            '--genlog-path', input_genlog_yt_path,
            '--categories-path', self.resources['tovar_tree_pb'].path,
            '--shopsdat-path', self.resources['shops_utf8_dat'].path,
            '--glue-config-path', self.resources['glue_config'].path,

            '--parts-count', str(build_options.parts_cnt),
            '--index-portions-table', build_options.yt_index_portions_table,
            '--yt-tmp-path', build_options.yt_tmp_path,
        ]

        if 'is_cpc' in self.resources:
            cmd.append('--is-cpc')
        if build_options.with_ann:
            cmd.append('--with-ann')
        if build_options.with_arch:
            cmd.append('--with-arch')
        if build_options.with_aa:
            cmd.append('--with-aa')

        cmd.extend([
            '--docs-in-portion', '10',
            '--portion-size-mb', '3',
            '--buffer-size-mb', '3',
            '--docs-in-annportion', '10',
            '--annportion-size-mb', '3',
            '--reduce-regions-for-fake-msku',
            '--set-has-adv-bid-literal',
        ])

        self.exec_result = self.try_execute_under_gdb(cmd)

    def execute_from_offers_list(self, yt_stuff, input_feed, binary_path=None):
        genlog_dir = ypath_join(get_yt_prefix(), self.GENERATION, 'genlog')
        table_path = ypath_join(genlog_dir, '0000')

        offers = []
        seq_num = 0
        for offer in input_feed:
            if 'shard_id' in offer and offer['shard_id'] != 0:
                raise ValueError("Cannot init yatf test using execute_from_offers_list with shard_id" + str(offer['shard_id']))
            offer['shard_id'] = 0
            offer['sequence_number'] = seq_num
            offers.append(offer)
            seq_num += 1

        genlog_table_entity = create_genlog_table(yt_stuff, table_path, offers)
        genlog_table_entity = genlog_table_entity
        return self.execute(yt_stuff, genlog_dir, binary_path)

    @property
    def yt_index_portions_path(self):
        return self.resources['build_options'].yt_index_portions_table


class MrMindexerMergeTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(MrMindexerMergeTestEnv, self).__init__(**resources)
        self.index_type = self.resources['merge_options'].index_type

    @property
    def description(self):
        return 'mr_mindexer_merge_' + self.index_type

    def execute(self, yt_stuff, path=None):
        if path is None:
            relative_path = os.path.join('market', 'idx', 'generation', 'mr', 'mr_mindexer')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        merge_options = self.resources['merge_options']
        cmd = [
            path, merge_options.merge_cmd,
            '--proxy', yt_stuff.get_server(),
            '--yt-index-portions', merge_options.input_path,
            '--dst-path', os.path.join(self.output_dir, merge_options.index_prefix),
        ]

        if merge_options.is_ann and not merge_options.is_arch:
            cmd.append('--is-ann')

        if merge_options.is_key_inv and merge_options.with_sent:
            cmd.append('--with-sent-file')

        if merge_options.is_aa:
            cmd.extend(['--tmp-dir', self.output_dir])

        self.exec_result = self.try_execute_under_gdb(cmd)

        if merge_options.is_arch:
            self.outputs.update({
                'indexarc': IndexArc(os.path.join(self.output_dir, 'indexarc')),
                'indexdir': FileResource(os.path.join(self.output_dir, 'indexdir')),
            })

        if merge_options.is_aa:
            self.outputs.update({
                'indexaa': IndexAa(os.path.join(self.output_dir, 'indexaa')),
                'cmagic_id_c2n': FileResource(os.path.join(self.output_dir, 'cmagic_id.c2n')),
                'forbidden_regions_c2n': FileResource(os.path.join(self.output_dir, 'forbidden_regions.c2n')),
                'hyper_ts_c2n': FileResource(os.path.join(self.output_dir, 'hyper_ts.c2n')),
            })

        if merge_options.is_key_inv:
            self.outputs.update({
                'indexinv': FileResource(os.path.join(self.output_dir, 'indexinv')),
                'indexkey': FileResource(os.path.join(self.output_dir, 'indexkey')),
                'indexsent': FileResource(os.path.join(self.output_dir, 'indexsent')),
                'literals': Literals(self.output_dir, 'indexkey', 'indexinv'),
            })

    @property
    def literal_lemmas(self):
        result = self.outputs['literals']
        result.load()
        return result
