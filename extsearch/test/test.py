import codecs
import yatest.common
from extsearch.geo.kernel.pymod.pytest_bdd_patched import scenarios_from_resfile

scenarios_from_resfile('resfs/file/math.feature')


def test_codecs():
    p = yatest.common.source_path('extsearch/geo/kernel/pymod/pytest_bdd_patched/test/file.txt')
    with codecs.open(p, encoding='utf-8') as fd:
        assert fd.read() == 'hello\n'
