import os
import pytest
import tarfile
import yatest.common


@pytest.fixture(scope='module')
def context():
    class Context(object):
        def __init__(self):
            self.queries = './requests.txt'
            self.printer = yatest.common.binary_path('extsearch/geo/base/calc_factors/web_annotation/printer/printer')
            self.index_prefix = './indexann'
            with tarfile.open("resource.tar.gz") as tar:
                tar.extractall()
            os.remove("resource.tar.gz")

    return Context()


def test_printfactors(context):
    with open('factors.txt', 'w') as fd:
        yatest.common.execute([context.printer, '-i', context.index_prefix, '-q', context.queries], stdout=fd)
    return yatest.common.canonical_file('factors.txt')
