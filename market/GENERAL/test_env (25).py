# coding: utf-8

import os
import yatest.common


from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv


class YtStreamsTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(YtStreamsTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'collect_streams'

    def execute(self, yt_stuff, is_models=False, path=None,  dont_prepare_web=False):
        if path is None:
            relative_path = os.path.join('market', 'idx', 'streams',
                                         'src', 'collect_streams',
                                         'src', 'collect_streams')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        proxy = yt_stuff.get_server()
        output = self.resources['output']
        input = self.resources['input']

        cmd = [
            path,
            '--proxy', proxy,
            '--web-streams-path', output.streams_path,
            '--working-path', output.working_path,
            '--web-streams-table', input.web_streams_path,
            '--input-path', input.input_path,
            '--results-path', output.output_path,
            '--parts-count', str(output.parts_cnt),
            '--with-ann-data',
        ]

        if dont_prepare_web:
            cmd.append('--dont-prepare-web')

        if is_models:
            cmd.append('--is-models')

        if input.model_alias_input_path:
            cmd.extend(['--model-alias-input-path', input.model_alias_input_path])

        if input.titles_input_path:
            cmd.extend(['--titles-input-path', input.titles_input_path])

        if input.marketing_descriptions_input_path:
            cmd.extend(['--model-marketing-descriptions-input-path', input.marketing_descriptions_input_path])

        if input.blue_model_marketing_descriptions_input_path:
            cmd.extend(['--blue-model-marketing-description-input-path', input.blue_model_marketing_descriptions_input_path])

        if input.blue_micro_model_descriptions_input_path:
            cmd.extend(['--blue-micro-model-description-input-path', input.blue_micro_model_descriptions_input_path])

        if input.micro_model_descriptions_input_path:
            cmd.extend(['--micro-model-descriptions-input-path', input.micro_model_descriptions_input_path])

        if input.msku_offer_titles_input_path:
            cmd.extend(['--msku-offer-titles-input-path', input.msku_offer_titles_input_path])

        if input.msku_offer_search_texts_input_path:
            cmd.extend(['--msku-offer-search-texts-input-path', input.msku_offer_search_texts_input_path])

        if input.cpa_queries_input_path:
            cmd.extend(['--cpa-query-input-path', input.cpa_queries_input_path])

        if input.custom_streams_input_paths:
            for path in input.custom_streams_input_paths:
                cmd.extend(['--custom-streams-input-path', path])

        if input.custom_streams_input_paths_no_optimization:
            for path in input.custom_streams_input_paths_no_optimization:
                cmd.extend(['--custom-streams-input-path-no-optimization', path])

        if input.offers_with_glue_path:
            cmd.extend(['--glue-streams-path', input.offers_with_glue_path])

        self.exec_result = self.try_execute_under_gdb(cmd)

        self.outputs.update(
            {
                "result_tables": output.load_streams_tables(yt_stuff)
            }
        )

    @property
    def result_tables(self):
        return self.outputs.get('result_tables')


class YtPrepareWebStreamsTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, yt_stuff, **resources):
        super(YtPrepareWebStreamsTestEnv, self).__init__(**resources)
        self.yt_stuff = yt_stuff

    @property
    def description(self):
        return 'prepare_web_streams'

    def execute(self, yt_stuff, merge_blue_features_with_white=False):
        relative_path = os.path.join('market', 'idx', 'streams',
                                     'src', 'prepare_web_streams',
                                     'src', 'prepare_web_streams')
        path = yatest.common.binary_path(relative_path)

        proxy = yt_stuff.get_server()
        output = self.resources['output']
        input = self.resources['input']

        cmd = [
            path,
            '--proxy', proxy,
            '--web-streams-table', input.web_streams_path,
            '--input-path', input.offers_path,
            '--parts-count', str(input.parts_count),
            '--working-path', output.working_dir,
            '--all'
        ]

        if merge_blue_features_with_white:
            cmd.append('--merge-blue-features-with-white')

        yt = self.yt_stuff.get_yt_client()
        if not yt.exists(output.working_dir):
            yt.create(type="map_node", path=output.working_dir)

        self.exec_result = self.try_execute_under_gdb(cmd)

        self.outputs.update(
            {
                "beru_filtered_normalized": output.load_blue_table(yt_stuff),
                "web_filtered_normalized": output.load_white_table(yt_stuff),
                "model_filtered_normalized": output.load_model_table(yt_stuff),
            }
        )

    def __exit__(self, *args):
        super(YtPrepareWebStreamsTestEnv, self).__exit__(*args)
        yt = self.yt_stuff.get_yt_client()
        yt.remove(self.resources['input'].web_streams_path, recursive=True, force=True)
        yt.remove(self.resources['input'].offers_path, recursive=True, force=True)
        yt.remove(self.resources['input'].hosts_table, recursive=True, force=True)
        yt.remove(self.resources['output'].working_dir, recursive=True, force=True)

    @property
    def beru_filtered_normalized(self):
        return self.outputs.get('beru_filtered_normalized')

    @property
    def web_filtered_normalized(self):
        return self.outputs.get('web_filtered_normalized')

    @property
    def model_filtered_normalized(self):
        return self.outputs.get('model_filtered_normalized')


class YtPrepareWebStreamsMergeTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, yt_stuff, **resources):
        super(YtPrepareWebStreamsMergeTestEnv, self).__init__(**resources)
        self.yt_stuff = yt_stuff

    @property
    def description(self):
        return 'prepare_web_streams'

    def execute(self, yt_stuff, is_models=False, is_blue=False):
        relative_path = os.path.join('market', 'idx', 'streams',
                                     'src', 'prepare_web_streams',
                                     'src', 'prepare_web_streams')
        path = yatest.common.binary_path(relative_path)

        proxy = yt_stuff.get_server()
        output = self.resources['output']
        input = self.resources['input']

        cmd = [
            path,
            '--proxy', proxy,
            '--parts-count', str(input.parts_count),
            '--working-path', input.working_dir,
            '--results-path', output.results_dir,
            '--merge-web',
        ]

        if is_models:
            cmd.append('--is-models')

        if is_blue:
            cmd.append('--is-blue')

        self.exec_result = self.try_execute_under_gdb(cmd)

        self.outputs.update(
            {
                "result_tables": output.load_results_tables(yt_stuff)
            }
        )

    def __exit__(self, *args):
        super(YtPrepareWebStreamsMergeTestEnv, self).__exit__(*args)
        yt = self.yt_stuff.get_yt_client()
        yt.remove(self.resources['input'].working_dir, recursive=True, force=True)
        yt.remove(self.resources['output'].results_dir, recursive=True, force=True)

    @property
    def result_tables(self):
        return self.outputs.get('result_tables')
