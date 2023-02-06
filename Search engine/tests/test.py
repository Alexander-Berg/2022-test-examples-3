import os

import yatest.common


def test():
    parser = yatest.common.binary_path('search/geo/tools/business_features_parser/parser/business_features_parser')
    compiler = yatest.common.binary_path('search/geo/tools/business_features_parser/compiler/business_features_compiler')

    yatest.common.execute([parser, 'features2.xml', 'output.gzt'])

    yatest.common.execute([compiler, 'output.gzt', 'output.gzt.bin'])
    assert os.path.isfile('output.gzt.bin')

    return yatest.common.canonical_file('output.gzt')
