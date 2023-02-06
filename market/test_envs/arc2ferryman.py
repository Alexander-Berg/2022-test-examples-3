# coding: utf-8

from google.protobuf.json_format import MessageToDict
import os
import six
import yatest.common
import yt.yson

from market.pylibrary.memoize.memoize import memoize
from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.yt_token_resource import YtTokenResource
from market.idx.yatf.test_envs.base_env import BaseEnv

from saas.protos.rtyserver_pb2 import TMessage


class Arc2FerrymanTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, yt_stuff, archive_type, archive_dir, yt_output_table, **resources):
        super(Arc2FerrymanTestEnv, self).__init__(**resources)
        self.yt_stuff = yt_stuff
        self.yt_client = yt_stuff.get_yt_client()
        self.archive_type = archive_type
        self.archive_dir = archive_dir
        self.yt_output_table = yt_output_table
        resources_stubs = {
            'yt_token': YtTokenResource(),
        }
        for name, val in resources_stubs.items():
            if name not in self.resources:
                self.resources[name] = val

    @property
    def description(self):
        return 'arc2ferryman'

    def execute(self, path=None):
        if path is None:
            relative_path = os.path.join(
                'market', 'idx', 'generation', 'arc2ferryman', 'arc2ferryman'
            )
            path = yatest.common.binary_path(relative_path)

        cmd = [
            path,
            '--type', self.archive_type,
            '--working-dir', self.archive_dir,
            '--yt-output-table-path', self.yt_output_table,
            '--yt-proxy', self.yt_stuff.yt_proxy,
            '--yt-token-path', self.resources['yt_token'].path,
        ]

        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=False
        )

    def get_yt_table_data(self, table_path):
        yt = self.yt_stuff.get_yt_client()
        if not yt.exists(table_path):
            return []
        is_dynamic = yt.get_attribute(table_path, 'dynamic')
        if is_dynamic:
            yt.freeze_table(table_path, sync=True)
        data = yt.read_table(table_path)
        if is_dynamic:
            yt.unfreeze_table(table_path, sync=True)
        return data

    def convert_to_dict(self, row):
        proto = TMessage()
        value = row['value']
        if isinstance(value, (six.string_types, six.text_type, six.binary_type)):
            proto.ParseFromString(six.ensure_binary(value))
        elif isinstance(value, yt.yson.yson_types.YsonStringProxy):
            proto.ParseFromString(yt.yson.get_bytes(value))
        else:
            raise RuntimeError(type(value), value)
        row['value'] = MessageToDict(proto)
        return row

    @property
    @memoize()
    def rows(self):
        return list(map(
            self.convert_to_dict,
            self.get_yt_table_data(self.yt_output_table)
        ))
