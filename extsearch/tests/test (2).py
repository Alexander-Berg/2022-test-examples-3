import yatest.common
import pytest


@pytest.fixture(scope='module')
def context():
    class Context(object):
        def __init__(self):
            self.raw_data = 'raw_annotations.txt'
            self.queries = yatest.common.source_path(
                'extsearch/geo/base/calc_factors/web_annotation/tests/download/queries.txt'
            )
            self.indexer = yatest.common.binary_path('extsearch/geo/indexer/web_annotation/indexer-web-annotation')
            self.printer = yatest.common.binary_path('extsearch/geo/base/calc_factors/web_annotation/printer/printer')
            self.index_prefix = 'index/index'
            yatest.common.execute([self.indexer, '-i', self.raw_data, '-o', self.index_prefix, '-f', 'text'])

    return Context()


def test_printfactors(context):
    with open('factors.txt', 'w') as fd:
        yatest.common.execute([context.printer, '-i', context.index_prefix, '-q', context.queries], stdout=fd)
    return yatest.common.canonical_file('factors.txt', local=True)
