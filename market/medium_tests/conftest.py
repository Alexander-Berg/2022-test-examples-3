# coding: utf-8

pytest_plugins = [
    'market.pylibrary.mindexerlib.test_utils.mysql',
    'market.idx.marketindexer.test_utils.utils.mindexer_clt_fixture',
    'market.idx.yatf.utils.zookeeper',
]
