# coding: utf-8

import os
import yatest.common


from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv, COMMON_STUBS_DIR
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.resources.yt_table_resource import YtTableResource

from saas.protos.rtyserver_pb2 import TMessage


class YtPrepareArticlesIndexTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        self._STUBS = {
            name: FileResource(os.path.join(COMMON_STUBS_DIR(), filename))
            for name, filename in list({
                'hard2_dssm_model': os.path.join('dssm', 'hard2_doc_embedding.adssm'),
            }.items())
        }
        super(YtPrepareArticlesIndexTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'prepare_articles_index'

    def execute(self, yt_stuff, result_path, path=None):
        if path is None:
            relative_path = os.path.join('market', 'idx', 'ugc', 'prepare_articles_index', 'src', 'prepare_articles_index')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        proxy = yt_stuff.get_server()
        input = self.resources['input']

        cmd = [
            path,
            '--yt-proxy', proxy,
            '--dssm-model-path', self.resources['hard2_dssm_model'].path,
            '--cms-pages-table', input.cms_pages_table_path,
            '--cms-compiled-table', input.cms_compiled_table_path,
            '--result-path', result_path,
            '--cms-pages-generation', 'test-pages',
            '--cms-compiled-generation', 'test-compiled',
        ]

        self.exec_result = self.try_execute_under_gdb(cmd)

        try:
            self.outputs.update({
                "result_table": YtTableResource(yt_stuff, result_path, load=True),
            })
        except:
            pass

        self.outputs.update({
            "result_saas_docs": self.create_result_saas_properties(),
        })

    def create_result_saas_properties(self):
        result_saas_page_id_to_props = {}
        for doc in self.result_table.data:
            saas_msg = TMessage()
            saas_msg.ParseFromString(doc["saas_message"])
            result_saas_props = {}
            result_saas_props["url"] = saas_msg.Document.Url
            for prop in saas_msg.Document.DocumentProperties:
                result_saas_props[prop.Name] = prop.Value
            result_saas_page_id_to_props[int(result_saas_props["page_id"])] = result_saas_props
        return result_saas_page_id_to_props

    @property
    def result_table(self):
        return self.outputs.get('result_table')

    @property
    def result_saas_docs(self):
        return self.outputs.get('result_saas_docs')
