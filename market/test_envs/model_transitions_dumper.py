import os
import yatest.common

from market.idx.models.yatf.resources.model_transitions.model_transitions_pb import (
    ModelTransitionsPb,
)
from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.yt_token_resource import YtTokenResource
from market.idx.yatf.test_envs.base_env import BaseEnv


class ModelTransitionsDumperTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0),
    ]

    def __init__(self, yt_stuff, **resources):
        super(ModelTransitionsDumperTestEnv, self).__init__(**resources)

        self.yt_stuff = yt_stuff
        self.yt_client = yt_stuff.get_yt_client()

        resources_stubs = {
            'yt_token': YtTokenResource(),
        }
        for name, val in resources_stubs.items():
            if name not in self.resources:
                self.resources[name] = val

    @property
    def description(self):
        return 'model_transitions_dumper'

    def execute(self, path=None, drop_long_ids=False):
        if path is None:
            relative_path = os.path.join(
                'market', 'idx', 'models', 'bin', 'model_transitions_dumper', 'src',
                'model_transitions_pb',
            )
            path = yatest.common.binary_path(relative_path)

        dst_pb_path = os.path.join(self.output_dir, 'model_transitions.pb')
        cmd = [
            path,
            '--proxy', self.yt_stuff.get_server(),
            '--token-path', self.resources['yt_token'].path,
            '--yt-input-path', self.resources['yt_input_table'].get_path(),
            '--dst-path', dst_pb_path
        ]

        model_ids_file = self.resources.get('model_ids')
        if model_ids_file:
            cmd.extend([
                '--entity-ids-gz-path', model_ids_file.path,
            ])

        if drop_long_ids:
            cmd.extend([
                '--drop-long-ids',
            ])

        self.exec_result = self.try_execute_under_gdb(
            cmd,
            check_exit_code=False
        )

        self.outputs.update({
            'model_transitions': ModelTransitionsPb(dst_pb_path)
        })

    @property
    def transitions(self):
        return self.outputs['model_transitions']
