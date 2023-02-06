# coding: utf-8

import os
import yatest.common

from market.idx.generation.yatf.resources.books_indexer.book_conf import BookConf
from market.idx.yatf.resources.indexaa import IndexAa
from market.idx.yatf.resources.indexarc import IndexArc
from market.idx.yatf.resources.indexkey_inv import Literals
from market.idx.yatf.matchers.env_matchers import HasExitCode, HasOutputFiles
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.test_envs.base_env import BaseEnv, COMMON_STUBS_DIR


def _STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'generation', 'yatf',
        'resources',
        'books_indexer',
        'stubs'
    )


class BooksIndexerTestEnv(BaseEnv):

    _MATCHERS = [
        HasExitCode(0),
        HasOutputFiles({
            'dssm.values.binary',
            'hard2_dssm.values.binary',
            'reformulation_dssm.values.binary',
            'bert_dssm.values.binary',
            'omni.wad',
            'assessment_binary.values.binary',
            'assessment.values.binary',
            'click.values.binary',
            'has_cpa_click.values.binary',
            'cpa.values.binary',
            'billed_cpa.values.binary',
        })
    ]

    def __init__(self, **resources):
        self._STUBS.update({
            name: FileResource(os.path.join(_STUBS_DIR(), filename))
            for name, filename in {
                'book_conf': 'book.conf',
                'tovar_categories': 'tovar-tree.pb',
                'navigation_categories': 'cataloger.navigation.xml',
                'navigation_categories_all': 'cataloger.navigation.all.xml',
                'dssm_model': 'doc_embedding.adssm',
                'thumbs_config': 'picrobot_thumbs.meta',
                'global_vendors_xml': 'global.vendors.xml',
            }.items()
        })

        self._STUBS.update({
            name: FileResource(os.path.join(COMMON_STUBS_DIR(), filename))
            for name, filename in {
                'hard2_dssm_model': os.path.join('dssm', 'hard2_doc_embedding.adssm'),
                'reformulation_dssm_model': os.path.join('dssm', 'reformulation_doc_embedding.adssm'),
                'bert_dssm_model': os.path.join('dssm', 'bert_doc_embedding.adssm'),
                'meta_multiclick_model': os.path.join('dssm', 'market_meta_multiclick.adssm'),
                'meta_dwelltime_model': os.path.join('dssm', 'market_meta_dwelltime.adssm'),
                'super_embed_model': os.path.join('dssm', 'superembed_doc.adssm'),
                'assessment_binary_model': os.path.join('dssm', 'assessment_binary.dssm'),
                'assessment_model': os.path.join('dssm', 'assessment.dssm'),
                'click_model': os.path.join('dssm', 'click.dssm'),
                'has_cpa_click_model': os.path.join('dssm', 'has_cpa_click.dssm'),
                'cpa_model': os.path.join('dssm', 'cpa.dssm'),
                'billed_cpa_model': os.path.join('dssm', 'billed_cpa.dssm'),
            }.items()
        })

        super(BooksIndexerTestEnv, self).__init__(**resources)
        self.resources['book_conf'] = resources.get('book_conf', BookConf())

    @property
    def description(self):
        return 'books_indexer'

    @property
    def index_dir(self):
        return self.resources['book_conf'].index_dir

    @property
    def literal_lemmas(self):
        result = self.outputs['literals']
        result.load()
        return result

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

    def path_to(self, resource):
        return os.path.join(
            self.input_dir,
            self.resources[resource].filename
        )

    def execute(self, path=None):
        if path is None:
            relative_path = os.path.join(
                'market', 'idx', 'books', 'new-books-indexer', 'src', 'new-books-indexer',
            )
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        cmd = [
            path,
            '--indexer-config', self.path_to('book_conf'),
            '--categories', self.path_to('tovar_categories'),
            '--navigation', self.path_to('navigation_categories'),
            '--navigation-all', self.path_to('navigation_categories_all'),
            '--dssm-model-path', self.path_to('dssm_model'),
            '--thumbs-config', self.path_to('thumbs_config'),
            '--models-dir-path', self.input_dir,
            '--enable-hard2-dssm-model-path',
            '--hard2-dssm-model-path', self.path_to('hard2_dssm_model'),
            '--enable-reformulation-dssm-model-path',
            '--reformulation-dssm-model-path', self.path_to('reformulation_dssm_model'),
            '--enable-bert-dssm-model-path',
            '--bert-dssm-model-path', self.path_to('bert_dssm_model'),
            '--enable-super-embed-model-path',
            '--super-embed-model-path', self.path_to('super_embed_model'),
            '--global-vendors', self.path_to('global_vendors_xml'),
            '--enable-assessment-binary-model-path',
            '--assessment-binary-model-path', self.path_to('assessment_binary_model'),
            '--enable-assessment-model-path',
            '--assessment-model-path', self.path_to('assessment_model'),
            '--enable-click-model-path',
            '--click-model-path', self.path_to('click_model'),
            '--enable-has-cpa-click-model-path',
            '--has-cpa-click-model-path', self.path_to('has_cpa_click_model'),
            '--enable-cpa-model-path',
            '--cpa-model-path', self.path_to('cpa_model'),
            '--enable-billed-cpa-model-path',
            '--billed-cpa-model-path', self.path_to('billed_cpa_model'),
        ]

        yt_book_stuff = self.resources.get('yt_book_stuff')
        if yt_book_stuff:
            cmd.extend([
                '--book-stuff-yt', yt_book_stuff.table_path,
                '--yt-proxy', yt_book_stuff.proxy
            ])

        model_reviews = self.resources.get('model_reviews')
        if model_reviews:
            cmd.extend(['--model-reviews', self.path_to('model_reviews')])

        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=False
        )

        self.outputs.update({
            'indexarc': IndexArc(os.path.join(self.index_dir, 'indexarc')),
            'indexaa': IndexAa(os.path.join(self.index_dir, 'indexaa')),
            'literals': Literals(self.index_dir, 'indexkey', 'indexinv'),
        })
