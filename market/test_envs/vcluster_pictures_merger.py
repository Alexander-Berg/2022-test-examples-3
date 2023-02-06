# coding: utf-8

import os
import yatest.common

from market.pylibrary.snappy_protostream import PbsnDataFile
from market.proto.indexer.VisualClusterPicturesDump_pb2 import Record as Cluster

from market.idx.yatf.matchers.env_matchers import (
    HasExitCode,
    HasOutputFiles,
)
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.test_envs.base_env import BaseEnv


def _STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'generation', 'yatf',
        'resources',
        'vcluster_pictures_merger',
        'stubs'
    )


class VClusterPicturesMergerTestEnv(BaseEnv):

    _MATCHERS = [
        HasExitCode(0),
        HasOutputFiles({
            'cluster_pictures.pbuf.sn',
            'cluster_desc.csv'
        })
    ]

    def __init__(self, **resources):
        self._STUBS = {
            name: FileResource(os.path.join(_STUBS_DIR(), filename))
            for name, filename in {
                'thumbs_meta': 'picrobot_thumbs.meta'
            }.items()
        }
        super(VClusterPicturesMergerTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'vcluster_pictures_merger'

    @property
    def clusters(self):
        return self.outputs["clusters"]

    @property
    def index_dir(self):
        return self.output_dir

    def execute(self, path=None):
        path = self.__calc_path(path)

        thumbs_meta = self.resources['thumbs_meta'].path

        pictures_input = sum((['-i', pic.path] for pic in self.resources['pictures']), [])

        cmd = [
            path,
            '-t', thumbs_meta,
            '-o', self.index_dir
        ] + pictures_input
        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=False
        )

        with PbsnDataFile(os.path.join(self.output_dir, "cluster_pictures.pbuf.sn"), "PICC") as data:
            self.outputs["clusters"] = [c for c in data.reader(Cluster)]

    def __calc_path(self, path):
        if path is not None:
            return path
        relative_path = os.path.join(
            'market', 'idx', 'generation',
            'vcluster_pictures_merger',
            'vcluster_pictures_merger',
        )
        return yatest.common.binary_path(relative_path)
