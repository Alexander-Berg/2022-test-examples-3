from yamarec_metarouter.metahosts import Metahost
from yamarec_metarouter.metahosts import MetahostGroup
from yamarec_metarouter.resolver import Resolver


def test_hosts_get_resolved_correctly():
    desktop = Metahost("market\.yandex\.(by|kz|ru)")
    touch = Metahost("m\.market\.yandex\.(by|kz|ru)")
    market = MetahostGroup([desktop, touch])
    search = Metahost("yandex\.ru")
    yandex = MetahostGroup([market, search])
    resolver = Resolver([desktop, touch, market, search, yandex])
    assert resolver.resolve("market.yandex.kz") == desktop
    assert resolver.resolve("m.market.yandex.by") == touch
    assert resolver.resolve("yandex.ru") == search
    assert resolver.resolve("yandex.net") is None
    assert resolver.resolve("m.market.yandex.uk") is None
