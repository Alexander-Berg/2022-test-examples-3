import yatest.common
import pytest
import os


@pytest.fixture(scope='module')
def context():
    class Context(object):
        def __init__(self):
            self.raw_data = yatest.common.source_path(
                'extsearch/geo/indexer/web_annotation/tests/download/raw_annotations.txt'
            )
            self.indexer = yatest.common.binary_path('extsearch/geo/indexer/web_annotation/indexer-web-annotation')
            self.print_sent_length = yatest.common.binary_path('tools/sent_index_print/sent_index_print')
            self.print_indexann_data = yatest.common.binary_path(
                'extsearch/geo/tools/userdata/userdata_view/userdata_view'
            )
            self.index_prefix = os.path.join('index', 'indexann')
            yatest.common.execute([self.indexer, '-i', self.raw_data, '-o', self.index_prefix, '-f', 'text'])

    return Context()


def test_sent_length(context):
    with open('sent_length.txt', 'w') as fd:
        yatest.common.execute([context.print_sent_length, context.index_prefix + 'sent'], stdout=fd)

    return yatest.common.canonical_file('sent_length.txt', local=True)


def test_indexanndata(context):
    with open('indexanndata.txt', 'w') as fd:
        yatest.common.execute([context.print_indexann_data, 'indexann', context.index_prefix], stdout=fd)

    return yatest.common.canonical_file('indexanndata.txt', local=True)
