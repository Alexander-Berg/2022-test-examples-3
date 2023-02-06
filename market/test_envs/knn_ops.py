# coding: utf-8

import os
import yatest.common

from mapreduce.yt.python.yt_stuff import YtConfig

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.resources.yt_table_resource import YtTableResource

from yt.wrapper import ypath_join


def StartupCustomizedLocalYt(
        RequiredCpus=1,
        RequiredMemoryGb=25):
    node_config_patch = {
        "resource_limits": {
            "cpu": RequiredCpus,
            "memory": RequiredMemoryGb * (1024 * 1024 * 1024),
        },
        "exec_agent": {
            "job_controller": {
                "resource_limits": {
                    "cpu":  RequiredCpus,
                    "memory": RequiredMemoryGb * (1024 * 1024 * 1024),
                }
            }
        },
        "enable_cgroups": False
    }
    return YtConfig(node_config=node_config_patch)


class KnnOpsTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(KnnOpsTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'knn_ops_build'

    def execute(self, yt_stuff, yt_config, result_path, path=None):
        if path is None:
            relative_path = os.path.join('quality', 'relev_tools', 'knn', 'knn_ops', 'bin', 'knn_ops')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        proxy = yt_stuff.get_server()
        build_options = self.resources['build_options']

        cmd = [
            path, 'build',
            '-s', proxy,
            '-i', build_options.yt_prepared_path,
            '-o', result_path,
            '--input-shard-column', build_options.shard_column,
            '--input-docid-column', build_options.docid_column,
            '--input-embeds-column', build_options.embeds_column,
            '--saas-message-column', build_options.saas_message_column,
            '--input-embeds-format', build_options.embeds_format,
            '--cpu-limit', '4',
            '--preset-shards', str(build_options.shards_number),
        ]
        self.exec_result = self.try_execute_under_gdb(cmd)
        self.update_output(yt_stuff, result_path)

    def update_output(self, yt_stuff, result_path):
        self.outputs.update(
            {
                "index": YtTableResource(yt_stuff, result_path),
                "index_meta": YtTableResource(yt_stuff, result_path+'.meta'),
                "index_meta_build_cachelog": YtTableResource(yt_stuff, result_path+'.meta_build_cachelog'),
            }
        )

    @property
    def index_table(self):
        return self.outputs.get('index')

    @property
    def index_meta_table(self):
        return self.outputs.get('index_meta')

    @property
    def index_data(self):
        self.index_table.load()
        return self.index_table.data

    @property
    def index_meta_data(self):
        self.index_meta_table.load()
        return self.index_meta_table.data

    @property
    def index_meta_build_cachelog_table(self):
        return self.outputs.get('index_meta_build_cachelog')


class KnnOpsPublishTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(KnnOpsPublishTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'knn_ops_publish'

    def execute(self, yt_stuff, build_index_path, result_path, path=None):
        if path is None:
            relative_path = os.path.join('quality', 'relev_tools', 'knn', 'knn_ops', 'bin', 'knn_ops')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        proxy = yt_stuff.get_server()
        cmd = [
            path, 'publish',
            '-s', proxy,
            '-i', build_index_path,
            '-o', result_path,
            '-n', 'market_ugc_knn',
        ]
        self.exec_result = self.try_execute_under_gdb(cmd)
        self.update_output(yt_stuff, result_path)

    def update_output(self, yt_stuff, result_path):
        self.outputs.update(
            {
                "published": YtTableResource(yt_stuff, ypath_join(result_path, 'blob')),
                "meta_data_dir": ypath_join(result_path, 'publishing')
            }
        )

    @property
    def published_table(self):
        return self.outputs.get('published')

    @property
    def meta_data_dir(self):
        return self.outputs.get('meta_data_dir')


class KnnOpsSearchTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(KnnOpsSearchTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'knn_ops_search'

    def execute(self, yt_stuff, index_path, path=None):
        if path is None:
            relative_path = os.path.join('quality', 'relev_tools', 'knn', 'knn_ops', 'bin', 'knn_ops')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        proxy = yt_stuff.get_server()
        build_index_options = self.resources['build_options']
        search_index_options = self.resources['search_options']
        cmd = [
            path, 'small_knn',
            '-s', proxy,
            '-i', search_index_options.yt_input_query_table_path,
            '-o', search_index_options.yt_output_result_table_path,
            '--index', index_path,
            '--input-embeds-column', build_index_options.embeds_column,
            '--input-embeds-format', build_index_options.embeds_format,
            '--shard-top-size', str(search_index_options.shard_top_size),
            '--shard-search-size', str(search_index_options.shard_search_size),
        ]
        self.exec_result = self.try_execute_under_gdb(cmd)
        self.update_output(yt_stuff, search_index_options.yt_output_result_table_path)

    def update_output(self, yt_stuff, result_path):
        self.outputs.update(
            {
                "search_result": YtTableResource(yt_stuff, result_path),
            }
        )

    @property
    def search_result_table(self):
        return self.outputs.get('search_result')

    @property
    def search_result_data(self):
        self.search_result_table.load()
        return self.search_result_table.data
