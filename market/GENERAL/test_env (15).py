# -*- coding: utf-8 -*-

import json
import os

import yatest.common

from market.idx.promos.yt_promo_indexer.yatf.resources import PromosResult
from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.yt_token_resource import YtTokenResource
from market.idx.yatf.test_envs.base_env import BaseEnv


class YtPromoIndexerTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, yt_stuff, use_collected_promo_details_table=None, **resources):
        super(YtPromoIndexerTestEnv, self).__init__(**resources)
        self._yt_stuff = yt_stuff
        self.yt_client = yt_stuff.get_yt_client()
        self.resources['yt_token'] = YtTokenResource()
        self.use_collected_promo_details_table = use_collected_promo_details_table

    @property
    def description(self):
        return 'yt_promo_indexer'

    @property
    def yt_promo_details(self):
        return [json.loads(promo) for promo in self.outputs['yt_promo_details']]

    def execute(self, mmap_name, path=None, cmd_args=None):
        if path is None:
            relative_path = os.path.join('market', 'idx', 'promos', 'yt_promo_indexer', 'bin', 'yt_promo_indexer')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path
        if cmd_args is None:
            cmd_args = {}

        if not os.path.exists(self.output_dir):
            os.makedirs(self.output_dir)

        args = [
            path,
            '--out-dir', self.output_dir,
            '--yt-server-name', self._yt_stuff.get_server(),
        ]
        if 'blue_secret_sale_table' in self.resources:
            args.extend([
                '--blue-secret-sale-table-name',
                self.resources['blue_secret_sale_table'].table_path
            ])
        if 'collected_promo_details_table' in self.resources:
            args.extend([
                '--collected-promo-details-table-name',
                self.resources['collected_promo_details_table'].table_path
            ])

        for cmd, val in list(cmd_args.items()):
            if val is not None:
                args.extend([cmd, val])
            else:
                args.append(cmd)

        self.exec_result = self.try_execute_under_gdb(
            args,
            cwd=self.output_dir,
            check_exit_code=False
        )

        mmap_file = os.path.join(self.output_dir, mmap_name)
        self.outputs.update({
            'yt_promo_details': PromosResult(mmap_file).result['promo_details']
        })
