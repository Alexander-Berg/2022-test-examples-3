import os
import yatest.common

from yt.wrapper.ypath import ypath_join

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.resource import FileResource, DirectoryFileResource
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.yt_table_resource import YtFileResource
from market.idx.yatf.resources.yt_token_resource import YtTokenResource
from market.idx.yatf.test_envs.base_env import BaseEnv, COMMON_STUBS_DIR


def _IDX_STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'offers', 'yatf',
        'resources',
        'offers_indexer',
        'stubs',
    )


class DssmMapperTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, yt_stuff, **resources):
        self._STUBS = {
            name: FileResource(os.path.join(_IDX_STUBS_DIR(), filename))
            for name, filename in {
                'dssm_model': 'doc_embedding.adssm',
                'tovar_categories': 'tovar-tree.pb',
            }.items()
        }

        self._STUBS.update({
            name: FileResource(os.path.join(COMMON_STUBS_DIR(), filename))
            for name, filename in {
                'hard2_dssm_model': os.path.join('dssm', 'hard2_doc_embedding.adssm'),
                'reformulation_dssm_model': os.path.join('dssm', 'reformulation_doc_embedding.adssm'),
                'bert_dssm_model': os.path.join('dssm', 'bert_doc_embedding.adssm'),
                'super_embed_model': os.path.join('dssm', 'superembed_doc.adssm'),
                'click_sim_trie': os.path.join('dssm', 'click_sim.trie'),
                'assessment_binary_model': os.path.join('dssm', 'assessment_binary.dssm'),
                'assessment_model': os.path.join('dssm', 'assessment.dssm'),
                'click_model': os.path.join('dssm', 'click.dssm'),
                'has_cpa_click_model': os.path.join('dssm', 'has_cpa_click.dssm'),
                'cpa_model': os.path.join('dssm', 'cpa.dssm'),
                'billed_cpa_model': os.path.join('dssm', 'billed_cpa.dssm'),
            }.items()
        })

        self._STUBS.update({
            'catalogia_dir_path': DirectoryFileResource(yatest.common.runtime.work_path('catalogia_neural_data')),
        })

        super(DssmMapperTestEnv, self).__init__(**resources)

        default_output_path = ypath_join(get_yt_prefix(), 'dssm')

        self.yt_stuff = yt_stuff
        self.yt_client = yt_stuff.get_yt_client()
        self.yt_dssm_dir = resources.get('output', YtFileResource(yt_stuff, default_output_path))

        resources_stubs = {
            'yt_token': YtTokenResource(),
        }
        for name, val in resources_stubs.items():
            if name not in self.resources:
                self.resources[name] = val

    @property
    def description(self):
        return 'dssm_mapper'

    def execute(self,
                enable_hard2_dssm=False,
                enable_reformulation_dssm=False,
                enable_bert=False,
                enable_super_embed=False,
                enable_assessment_binary=False,
                enable_assessment=False,
                enable_click=False,
                enable_has_cpa_click=False,
                enable_cpa=False,
                enable_billed_cpa=False,
                enable_click_sim=False,
                enable_catalogia=False,
                path=None):
        if path is None:
            relative_path = os.path.join(
                'market', 'idx', 'generation',
                'dssm-mapper',
                'dssm-mapper',
            )
            path = yatest.common.binary_path(relative_path)

        cmd = [
            path,
            '--yt-proxy', self.yt_stuff.get_server(),
            '--yt-token-path', self.resources['yt_token'].path,
            '--input', self.resources['genlog'].path,
            '--output', self.yt_dssm_dir.path,
            '--dssm-table-path', self.yt_dssm_dir.path,
            '--dssm-model-path', self.resources['dssm_model'].path,
        ]

        model_transitions = self.resources.get('model_transitions')
        if model_transitions:
            cmd.extend([
                '--model-transitions-path', model_transitions.path
            ])

        if enable_hard2_dssm:
            cmd.extend([
                '--hard2-dssm-model-path', self.resources.get('hard2_dssm_model').path
            ])

        if enable_reformulation_dssm:
            cmd.extend([
                '--reformulation-dssm-model-path', self.resources.get('reformulation_dssm_model').path
            ])

        if enable_bert:
            cmd.extend([
                '--bert-dssm-model-path', self.resources.get('bert_dssm_model').path
            ])

        if enable_super_embed:
            cmd.extend([
                '--super-embed-model-path', self.resources.get('super_embed_model').path,
            ])

        if enable_assessment_binary:
            cmd.extend([
                '--assessment-binary-model-path', self.resources.get('assessment_binary_model').path,
                '--categories', self.resources.get('tovar_categories').path,
            ])

        if enable_assessment:
            cmd.extend([
                '--assessment-model-path', self.resources.get('assessment_model').path,
                '--categories', self.resources.get('tovar_categories').path,
            ])

        if enable_click:
            cmd.extend([
                '--click-model-path', self.resources.get('click_model').path,
                '--categories', self.resources.get('tovar_categories').path,
            ])

        if enable_has_cpa_click:
            cmd.extend([
                '--has-cpa-click-model-path', self.resources.get('has_cpa_click_model').path,
                '--categories', self.resources.get('tovar_categories').path,
            ])

        if enable_cpa:
            cmd.extend([
                '--cpa-model-path', self.resources.get('cpa_model').path,
                '--categories', self.resources.get('tovar_categories').path,
            ])

        if enable_billed_cpa:
            cmd.extend([
                '--billed-cpa-model-path', self.resources.get('billed_cpa_model').path,
                '--categories', self.resources.get('tovar_categories').path,
            ])

        if enable_click_sim:
            cmd.extend([
                '--click-sim-trie', self.resources.get('click_sim_trie').path
            ])

        if enable_catalogia:
            cmd.extend([
                '--catalogia-dir-path', self.resources.get('catalogia_dir_path').path
            ])

        self.exec_result = self.try_execute_under_gdb(
            cmd,
            check_exit_code=False
        )

    @property
    def output_dssm_table(self):
        dssm_table_name = self.yt_client.list(ypath_join(self.yt_dssm_dir.path))[0]
        dssm_table_path = ypath_join(self.yt_dssm_dir.path, dssm_table_name)
        return [row for row in self.yt_client.read_table(dssm_table_path)]
