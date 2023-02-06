# coding: utf-8

import os
import yatest.common

from market.idx.yatf.resources.indexaa import IndexAa
from market.idx.yatf.resources.indexarc import IndexArc
from market.idx.models.yatf.resources.models_indexer.model_vcluster_index import (
    ModelVclusterIndexConf,
)
from market.idx.yatf.resources.indexkey_inv import Literals
from market.idx.yatf.matchers.env_matchers import (
    HasExitCode,
    HasOutputFiles,
)
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.test_envs.base_env import BaseEnv, COMMON_STUBS_DIR
from market.idx.yatf.utils.mmap.mmapviewer import json_view


def _STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'models', 'yatf',
        'resources',
        'models_indexer',
        'stubs',
    )


class MrModelsIndexerTestEnv(BaseEnv):

    _MATCHERS = [
        HasExitCode(0),
        HasOutputFiles({
            'indexarc',
            'indexaa',
            'dssm.values.binary',
            'hard2_dssm.values.binary',
            'reformulation_dssm.values.binary',
            'bert_dssm.values.binary',
            'omni.wad',
            'gl_models.mmap',
            'hyper_ts.c2n',
            'indexdir',
            'indexinv',
            'indexkey',
            'maliases.c2n',
            'assessment_binary.values.binary',
            'assessment.values.binary',
            'click.values.binary',
            'has_cpa_click.values.binary',
            'cpa.values.binary',
            'billed_cpa.values.binary',
        })
    ]

    def __init__(self, only_with_msku=False, **resources):
        self._STUBS = {
            name: FileResource(os.path.join(_STUBS_DIR(), filename))
            for name, filename in {
                'model_vcluster_index': 'model_vcluster_index.cfg',
                'parser_config': 'PARSER_CONFIG',
                'tovar_categories': 'tovar-tree.pb',
                'navigation_categories': 'cataloger.navigation.xml',
                'navigation_categories_all': 'cataloger.navigation.all.xml',
                'models': 'models_90592.pb',
                'parameters': 'parameters_90592.pb',
                'doc_embedding': 'doc_embedding.adssm',
                'global_vendors_xml': 'global.vendors.xml',
            }.items()
        }

        self._STUBS.update({
            name: FileResource(os.path.join(COMMON_STUBS_DIR(), filename))
            for name, filename in {
                'hard2_doc_embedding': os.path.join('dssm', 'hard2_doc_embedding.adssm'),
                'reformulation_doc_embedding': os.path.join('dssm', 'reformulation_doc_embedding.adssm'),
                'bert_doc_embedding': os.path.join('dssm', 'bert_doc_embedding.adssm'),
                'super_embed_embedding': os.path.join('dssm', 'superembed_doc.adssm'),
                'assessment_binary_doc_embedding': os.path.join('dssm', 'assessment_binary.dssm'),
                'assessment_doc_embedding': os.path.join('dssm', 'assessment.dssm'),
                'click_doc_embedding': os.path.join('dssm', 'click.dssm'),
                'has_cpa_click_doc_embedding': os.path.join('dssm', 'has_cpa_click.dssm'),
                'cpa_doc_embedding': os.path.join('dssm', 'cpa.dssm'),
                'billed_cpa_doc_embedding': os.path.join('dssm', 'billed_cpa.dssm'),
            }.items()
        })

        resources['model_vcluster_index'] = resources.get(
            'model_vcluster_index', ModelVclusterIndexConf()
        )
        super(MrModelsIndexerTestEnv, self).__init__(**resources)
        self.mbo_preview_mode = False
        self.only_with_msku = only_with_msku

    @property
    def description(self):
        return 'models_indexer'

    @property
    def index_dir(self):
        return self.resources['model_vcluster_index'].index_dir

    @property
    def offers(self):
        indexarc = self.outputs['indexarc']
        indexaa = self.outputs['indexaa']

        outputs = [
            indexarc,
            indexaa
        ]

        for output_file in outputs:
            output_file.load()

        offers = dict()
        for doc_id in indexarc.doc_ids:
            offers[doc_id] = indexarc.load_doc_description(doc_id)
            offers[doc_id].update(indexaa.get_group_attributes(doc_id))
        return offers

    @property
    def literal_lemmas(self):
        result = self.outputs['literals']
        result.load()
        return result

    def path_to(self, resource):
        return os.path.join(
            self.input_dir,
            self.resources[resource].filename
        )

    def execute(self, path=None):
        if path is None:
            relative_path = os.path.join(
                'market', 'idx', 'models', 'bin',
                'mr-models-indexer',
                'mr-models-indexer',
            )
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        indexer_cmd = [
            path,
            '--categories', self.path_to('tovar_categories'),
            '--navigation', self.path_to('navigation_categories'),
            '--navigation-all', self.path_to('navigation_categories_all'),
            '--indexer-config', self.path_to('model_vcluster_index'),
            '--input-dir', self.input_dir,
            '--dssm-model-path', self.path_to('doc_embedding'),
            '--hard2-dssm-model-path', self.path_to('hard2_doc_embedding'),
            '--reformulation-dssm-model-path', self.path_to('reformulation_doc_embedding'),
            '--bert-dssm-model-path', self.path_to('bert_doc_embedding'),
            '--super-embed-model-path', self.path_to('super_embed_embedding'),
            '--assessment-binary-model-path', self.path_to('assessment_binary_doc_embedding'),
            '--assessment-model-path', self.path_to('assessment_doc_embedding'),
            '--click-model-path', self.path_to('click_doc_embedding'),
            '--has-cpa-click-model-path', self.path_to('has_cpa_click_doc_embedding'),
            '--cpa-model-path', self.path_to('cpa_doc_embedding'),
            '--billed-cpa-model-path', self.path_to('billed_cpa_doc_embedding'),
            '--global-vendors', self.path_to('global_vendors_xml'),

            '--calc-dssms-for-models',
            '--enable-click',
            '--enable-cpa',
            '--enable-hard2-dssm-for-models',
            '--enable-billed-cpa',
            '--enable-reformulation-dssm-for-models',
            '--enable-super-embed',
            '--enable-has-cpa-click',
            '--enable-assessment',
            '--enable-assessment-binary',
            '--enable-bert-dssm',
        ]

        if 'cluster_pictures_mmap' in self.resources:
            cluster_pictures_mmap = os.path.join(
                self.input_dir,
                self.resources['cluster_pictures_mmap'].filename
            )
            indexer_cmd.append('--pictures-mmap')
            indexer_cmd.append(cluster_pictures_mmap)

        if 'snapshot_pbuf_sn' in self.resources:
            indexer_cmd.append('--mbi-modelbids-path')
            indexer_cmd.append(self.input_dir)

        if 'contex_experiments' in self.resources:
            indexer_cmd.append('--contex-data')
            indexer_cmd.append(
                os.path.join(
                    self.input_dir,
                    self.resources['contex_experiments'].filename
                )
            )

        if 'barcodes' in self.resources:
            indexer_cmd.append('--model-barcodes')
            indexer_cmd.append(
                os.path.join(
                    self.input_dir,
                    self.resources['barcodes'].filename
                )
            )

        if 'blue_offer_models' in self.resources:
            indexer_cmd.append('--blue-offer-models')
            indexer_cmd.append(
                os.path.join(
                    self.input_dir,
                    self.resources['blue_offer_models'].filename
                )
            )

        if 'cluster_desc' in self.resources:
            indexer_cmd.append('--cluster-desc-path')
            indexer_cmd.append(
                os.path.join(
                    self.input_dir,
                    self.resources['cluster_desc'].filename
                )
            )

        if 'cms_promo' in self.resources:
            indexer_cmd.append('--cms-promo')
            indexer_cmd.append(
                os.path.join(
                    self.input_dir,
                    self.resources['cms_promo'].filename
                )
            )

        model_transitions = self.resources.get('model_transitions')
        if model_transitions:
            indexer_cmd.extend([
                '--model-transitions-pb', model_transitions.path
            ])

        model2msku = self.resources.get('model2msku')
        if model2msku:
            indexer_cmd.extend([
                '--is-prepare-model2sku-csv-white',
                '--model2msku-path', model2msku.path,
            ])

        if self.mbo_preview_mode:
            indexer_cmd.append('--mbo-preview-mode')

        if self.only_with_msku:
            indexer_cmd.append('--store-models-with-msku-only')

        self.exec_result = self.try_execute_under_gdb(
            indexer_cmd,
            cwd=self.output_dir,
            check_exit_code=False,
        )

        self.outputs.update({
            'indexarc': IndexArc(os.path.join(self.index_dir, 'indexarc')),
            'indexaa': IndexAa(os.path.join(self.index_dir, 'indexaa')),
            'dssm_values_binary': FileResource(os.path.join(
                self.index_dir, 'dssm.values.binary')
            ),
            'hard2_dssm_values_binary': FileResource(os.path.join(
                self.index_dir, 'hard2_dssm.values.binary')
            ),
            'gl_models_mmap': FileResource(os.path.join(
                self.index_dir, 'gl_models.mmap')
            ),
            'gl_models_mmap_data': json_view(os.path.join(
                self.index_dir, 'gl_models.mmap')
            ),
            'hyper_ts_c2n': FileResource(os.path.join(
                self.index_dir, 'hyper_ts.c2n')
            ),
            'indexdir': FileResource(os.path.join(
                self.index_dir, 'indexdir')
            ),
            'indexinv': FileResource(os.path.join(
                self.index_dir, 'indexinv')
            ),
            'indexkey': FileResource(os.path.join(
                self.index_dir, 'indexkey')
            ),
            'maliases_c2n': FileResource(os.path.join(
                self.index_dir, 'maliases.c2n')
            ),
            'gl_models_pbuf_sn': FileResource(os.path.join(
                self.output_dir, 'gl_models.pbuf.sn')
            ),
            'model_warnings_mmap': FileResource(os.path.join(
                self.output_dir, 'model_warnings.mmap')
            ),
            'reformulation_dssm_values_binary': FileResource(os.path.join(
                self.index_dir, 'reformulation_dssm.values.binary')
            ),
            'literals': Literals(self.index_dir, 'indexkey', 'indexinv'),
            'vendor_values_binary': FileResource(os.path.join(
                self.index_dir, 'vendor.values.binary')
            ),
        })


class ModelsIndexerTestEnv(BaseEnv):

    _MATCHERS = [
        HasExitCode(0),
        HasOutputFiles({
            'indexarc',
            'indexaa',
            'dssm.values.binary',
            'hard2_dssm.values.binary',
            'reformulation_dssm.values.binary',
            'bert_dssm.values.binary',
            'omni.wad',
            'gl_models.mmap',
            'hyper_ts.c2n',
            'indexdir',
            'indexinv',
            'indexkey',
            'maliases.c2n',
            'assessment_binary.values.binary',
            'assessment.values.binary',
            'click.values.binary',
            'has_cpa_click.values.binary',
            'cpa.values.binary',
            'billed_cpa.values.binary',
        })
    ]

    def __init__(self, only_with_msku=False, **resources):
        self._STUBS = {
            name: FileResource(os.path.join(_STUBS_DIR(), filename))
            for name, filename in {
                'model_vcluster_index': 'model_vcluster_index.cfg',
                'parser_config': 'PARSER_CONFIG',
                'tovar_categories': 'tovar-tree.pb',
                'navigation_categories': 'cataloger.navigation.xml',
                'navigation_categories_all': 'cataloger.navigation.all.xml',
                'models': 'models_90592.pb',
                'parameters': 'parameters_90592.pb',
                'doc_embedding': 'doc_embedding.adssm',
                'global_vendors_xml': 'global.vendors.xml',
            }.items()
        }

        self._STUBS.update({
            name: FileResource(os.path.join(COMMON_STUBS_DIR(), filename))
            for name, filename in {
                'hard2_doc_embedding': os.path.join('dssm', 'hard2_doc_embedding.adssm'),
                'reformulation_doc_embedding': os.path.join('dssm', 'reformulation_doc_embedding.adssm'),
                'bert_doc_embedding': os.path.join('dssm', 'bert_doc_embedding.adssm'),
                'super_embed_embedding': os.path.join('dssm', 'superembed_doc.adssm'),
                'assessment_binary_doc_embedding': os.path.join('dssm', 'assessment_binary.dssm'),
                'assessment_doc_embedding': os.path.join('dssm', 'assessment.dssm'),
                'click_doc_embedding': os.path.join('dssm', 'click.dssm'),
                'has_cpa_click_doc_embedding': os.path.join('dssm', 'has_cpa_click.dssm'),
                'cpa_doc_embedding': os.path.join('dssm', 'cpa.dssm'),
                'billed_cpa_doc_embedding': os.path.join('dssm', 'billed_cpa.dssm'),
            }.items()
        })

        resources['model_vcluster_index'] = resources.get(
            'model_vcluster_index', ModelVclusterIndexConf()
        )
        super(ModelsIndexerTestEnv, self).__init__(**resources)
        self.mbo_preview_mode = False
        self.only_with_msku = only_with_msku

    @property
    def description(self):
        return 'models_indexer'

    @property
    def index_dir(self):
        return self.resources['model_vcluster_index'].index_dir

    @property
    def offers(self):
        indexarc = self.outputs['indexarc']
        indexaa = self.outputs['indexaa']

        outputs = [
            indexarc,
            indexaa
        ]

        for output_file in outputs:
            output_file.load()

        offers = dict()
        for doc_id in indexarc.doc_ids:
            offers[doc_id] = indexarc.load_doc_description(doc_id)
            offers[doc_id].update(indexaa.get_group_attributes(doc_id))
        return offers

    @property
    def literal_lemmas(self):
        result = self.outputs['literals']
        result.load()
        return result

    def path_to(self, resource):
        return os.path.join(
            self.input_dir,
            self.resources[resource].filename
        )

    def execute(self, path=None):
        if path is None:
            relative_path = os.path.join(
                'market', 'idx', 'models', 'bin',
                'new-models-indexer',
                'new-models-indexer',
            )
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        indexer_cmd = [
            path,
            '--categories', self.path_to('tovar_categories'),
            '--navigation', self.path_to('navigation_categories'),
            '--navigation-all', self.path_to('navigation_categories_all'),
            '--indexer-config', self.path_to('model_vcluster_index'),
            '--input-dir', self.input_dir,
            '--dssm-model-path', self.path_to('doc_embedding'),
            '--hard2-dssm-model-path', self.path_to('hard2_doc_embedding'),
            '--reformulation-dssm-model-path', self.path_to('reformulation_doc_embedding'),
            '--bert-dssm-model-path', self.path_to('bert_doc_embedding'),
            '--super-embed-model-path', self.path_to('super_embed_embedding'),
            '--assessment-binary-model-path', self.path_to('assessment_binary_doc_embedding'),
            '--assessment-model-path', self.path_to('assessment_doc_embedding'),
            '--click-model-path', self.path_to('click_doc_embedding'),
            '--has-cpa-click-model-path', self.path_to('has_cpa_click_doc_embedding'),
            '--cpa-model-path', self.path_to('cpa_doc_embedding'),
            '--billed-cpa-model-path', self.path_to('billed_cpa_doc_embedding'),
            '--global-vendors', self.path_to('global_vendors_xml'),

            '--calc-dssms-for-models',
            '--enable-click',
            '--enable-cpa',
            '--enable-hard2-dssm-for-models',
            '--enable-billed-cpa',
            '--enable-reformulation-dssm-for-models',
            '--enable-super-embed',
            '--enable-has-cpa-click',
            '--enable-assessment',
            '--enable-assessment-binary',
            '--enable-bert-dssm',
        ]

        if 'cluster_pictures_mmap' in self.resources:
            cluster_pictures_mmap = os.path.join(
                self.input_dir,
                self.resources['cluster_pictures_mmap'].filename
            )
            indexer_cmd.append('--pictures-mmap')
            indexer_cmd.append(cluster_pictures_mmap)

        if 'snapshot_pbuf_sn' in self.resources:
            indexer_cmd.append('--mbi-modelbids-path')
            indexer_cmd.append(self.input_dir)

        if 'contex_experiments' in self.resources:
            indexer_cmd.append('--contex-data')
            indexer_cmd.append(
                os.path.join(
                    self.input_dir,
                    self.resources['contex_experiments'].filename
                )
            )

        if 'barcodes' in self.resources:
            indexer_cmd.append('--model-barcodes')
            indexer_cmd.append(
                os.path.join(
                    self.input_dir,
                    self.resources['barcodes'].filename
                )
            )

        if 'blue_offer_models' in self.resources:
            indexer_cmd.append('--blue-offer-models')
            indexer_cmd.append(
                os.path.join(
                    self.input_dir,
                    self.resources['blue_offer_models'].filename
                )
            )

        if 'cluster_desc' in self.resources:
            indexer_cmd.append('--cluster-desc-path')
            indexer_cmd.append(
                os.path.join(
                    self.input_dir,
                    self.resources['cluster_desc'].filename
                )
            )

        if 'cms_promo' in self.resources:
            indexer_cmd.append('--cms-promo')
            indexer_cmd.append(
                os.path.join(
                    self.input_dir,
                    self.resources['cms_promo'].filename
                )
            )

        model_transitions = self.resources.get('model_transitions')
        if model_transitions:
            indexer_cmd.extend([
                '--model-transitions-pb', model_transitions.path
            ])

        model2msku = self.resources.get('model2msku')
        if model2msku:
            indexer_cmd.extend([
                '--is-prepare-model2sku-csv-white',
                '--model2msku-path', model2msku.path,
            ])

        if self.mbo_preview_mode:
            indexer_cmd.append('--mbo-preview-mode')

        if self.only_with_msku:
            indexer_cmd.append('--store-models-with-msku-only')

        self.exec_result = self.try_execute_under_gdb(
            indexer_cmd,
            cwd=self.output_dir,
            check_exit_code=False,
        )

        self.outputs.update({
            'indexarc': IndexArc(os.path.join(self.index_dir, 'indexarc')),
            'indexaa': IndexAa(os.path.join(self.index_dir, 'indexaa')),
            'dssm_values_binary': FileResource(os.path.join(
                self.index_dir, 'dssm.values.binary')
            ),
            'hard2_dssm_values_binary': FileResource(os.path.join(
                self.index_dir, 'hard2_dssm.values.binary')
            ),
            'gl_models_mmap': FileResource(os.path.join(
                self.index_dir, 'gl_models.mmap')
            ),
            'gl_models_mmap_data': json_view(os.path.join(
                self.index_dir, 'gl_models.mmap')
            ),
            'hyper_ts_c2n': FileResource(os.path.join(
                self.index_dir, 'hyper_ts.c2n')
            ),
            'indexdir': FileResource(os.path.join(
                self.index_dir, 'indexdir')
            ),
            'indexinv': FileResource(os.path.join(
                self.index_dir, 'indexinv')
            ),
            'indexkey': FileResource(os.path.join(
                self.index_dir, 'indexkey')
            ),
            'maliases_c2n': FileResource(os.path.join(
                self.index_dir, 'maliases.c2n')
            ),
            'gl_models_pbuf_sn': FileResource(os.path.join(
                self.output_dir, 'gl_models.pbuf.sn')
            ),
            'model_warnings_mmap': FileResource(os.path.join(
                self.output_dir, 'model_warnings.mmap')
            ),
            'reformulation_dssm_values_binary': FileResource(os.path.join(
                self.index_dir, 'reformulation_dssm.values.binary')
            ),
            'literals': Literals(self.index_dir, 'indexkey', 'indexinv'),
            'vendor_values_binary': FileResource(os.path.join(
                self.index_dir, 'vendor.values.binary')
            ),
        })
