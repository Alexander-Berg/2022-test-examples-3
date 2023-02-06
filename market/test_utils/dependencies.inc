DEPENDS(
    contrib/tools/xsltproc
    market/idx/generation/feedlog-merger
    market/idx/generation/feedlog-stats
    market/idx/marketindexer/bin/mindexer_clt
    market/pylibrary/mindexerlib/test_utils/bigdata
)

DATA(
    arcadia/market/idx/generation/packages/yandex-market-offers/scripts/PlatformVersionStats.xslt
    arcadia/market/idx/marketindexer/test_utils/data
    arcadia/market/idx/marketindexer/tests/datasources.conf
    arcadia/market/idx/marketindexer/tests/zookeeper.conf
    arcadia/market/idx/miconfigs/etc/master/conf-available/development.stratocaster.ini
    arcadia/market/idx/miconfigs/etc/feature/conf-available/testing.stratocaster.ini
    arcadia/market/idx/miconfigs/etc/glue/testing.stratocaster.json
    arcadia/market/idx/miconfigs/etc/feature/common.ini
)
