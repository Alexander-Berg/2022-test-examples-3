# coding: utf-8

from google.protobuf.json_format import MessageToDict
import os
import yatest.common

from market.idx.yatf.resources.indexarc import IndexArc
from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.test_envs.base_env import BaseEnv

from market.proto.indexer.indexarc_pb2 import EAgeUnit as AgeUnits
from market.proto.indexer.indexarc_pb2 import TArchiveEntry

assert AgeUnits


def _STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'generation', 'yatf',
        'resources',
        'arc_indexer',
        'stubs',
    )


class IndexarcTestEnv(BaseEnv):

    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        self._STUBS.update({
            name: FileResource(os.path.join(_STUBS_DIR(), filename))
            for name, filename in {
                'indexarc': 'indexarc'
            }.items()
        })
        super(IndexarcTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'indexarc_converter'

    @property
    def parsed_entry(self):
        indexarc = self.outputs['indexarc']
        attrs = indexarc.load_doc_proto_data()
        entry = TArchiveEntry()
        entry.ParseFromString(attrs)
        return entry

    @property
    def all_parsed_entries(self):
        indexarc = self.outputs['indexarc']
        indexarc.load()

        for doc_id in indexarc.doc_ids:
            arc_proto = indexarc.load_doc_proto_data_by_id(doc_id)
            entry = TArchiveEntry()
            entry.ParseFromString(arc_proto)
            yield entry

    @property
    def all_documents(self):
        indexarc = self.outputs['indexarc']
        indexarc.load()

        for doc_id in indexarc.doc_ids:
            arc_proto = indexarc.load_doc_proto_data_by_id(doc_id)
            entry = TArchiveEntry()
            entry.ParseFromString(arc_proto)
            result = MessageToDict(entry, preserving_proto_field_name=True)

            description = indexarc.load_doc_description(doc_id)
            result.update(description)
            result['id'] = doc_id
            yield result

    def execute(self, path=None):
        if path is None:
            relative_path = os.path.join(
                'market', 'idx', 'generation',
                'indexarc-converter',
                'indexarc-converter',
            )
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        from_arc = os.path.join(self.input_dir, 'indexarc')
        to_arc = os.path.join(self.output_dir, 'indexarc')
        to_dir = os.path.join(self.output_dir, 'indexdir')

        cmd = [
            path,
            '--from-arc', from_arc,
            '--to-arc', to_arc,
            '--to-dir', to_dir
        ]
        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=False
        )
        self.outputs.update({
            'indexarc': IndexArc(os.path.join(self.output_dir, 'indexarc')),
            'indexarc.words': FileResource(os.path.join(self.output_dir, 'indexarc.words')),
            'indexdir': FileResource(os.path.join(self.output_dir, 'indexdir'))
        })
