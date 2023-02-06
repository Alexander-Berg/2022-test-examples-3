# coding: utf-8

import os

import yatest.common
from market.idx.yatf.matchers.env_matchers import (
    HasExitCode
)
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.test_envs.base_env import BaseEnv


def _STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'yatf', 'resources', 'stubs', 'create_meta', 'snapshot.pbuf.sn'
    )


class CreateMetaTestEnv(BaseEnv):

    _MATCHERS = [
        HasExitCode(0)
    ]
    _SNAPSHOT_META_FILE_NAME = 'snapshot.meta'
    _MODELBIDS_SNAPSHOT_META_FILE_NAME = 'mb_snapshot.meta'

    def init_stubs(self):
        self._STUBS = {
            name: FileResource(os.path.join(_STUBS_DIR(), filename))
            for name, filename in {
                'snapshot_pbuf_sn': 'snapshot.pbuf.sn'
            }.items()
        }

    def __init__(self, modelbids=False, **resources):
        self.init_stubs()
        super(CreateMetaTestEnv, self).__init__(**resources)
        self._modelbids = modelbids

    @property
    def description(self):
        return 'create_meta'

    def execute(self, path=None):
        if path is None:
            relative_path = os.path.join('market', 'qpipe', 'qbid',
                                         'create_meta', 'qbid_create_meta')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        input_snapshot_filename = self.resources['snapshot_pbuf_sn'].filename
        output_meta_filename = self._SNAPSHOT_META_FILE_NAME
        bidtarget = 'offer'

        if self._modelbids:
            output_meta_filename = self._MODELBIDS_SNAPSHOT_META_FILE_NAME
            bidtarget = 'model'

        snapshot_pbuf_sn = os.path.join(
            self.input_dir,
            input_snapshot_filename
        )

        snapshot_meta = os.path.join(
            self.output_dir,
            output_meta_filename
        )

        cmd = [
            path,
            '--ifile', snapshot_pbuf_sn,
            '--ometa', snapshot_meta,
            '--bidtarget', bidtarget,
        ]
        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=False)

        self.outputs.update({
            'snapshot_meta': FileResource(os.path.join(self.output_dir,
                                                       output_meta_filename)),
        })
