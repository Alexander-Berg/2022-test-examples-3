def test_loader_loads_test_data(loader):
    loader.load()
    metahosts = loader.metahosts
    metaroutes = loader.metaroutes

    assert len(metahosts) == 4
    assert len(metaroutes) == 7

    metahost = metahosts["/market/desktop"]
    assert metahost.pattern.match("market.yandex.kz")
    assert not metahost.pattern.match("m.market.yandex.kz")
    patterns = metaroutes["/market/desktop/iskander"].patterns
    assert patterns[metahost].match("/iskander?isawesome=true&islazy=false")
    assert not patterns[metahost].match("/iskander?isawesome=false")
    patterns = metaroutes["/market/desktop/market_search"].patterns
    assert patterns[metahost].match("/search.xml?text=lol")
    patterns = metaroutes["/market/desktop/market_product"].patterns
    assert patterns[metahost].match("/product/123/offers?test=1")
    patterns = metaroutes["/market/desktop/model"].patterns
    assert patterns[metahost].match("/product/123/offers?test=1")
    assert not patterns[metahost].match("/model/123/offers?test=1")
    patterns = metaroutes["/market/model"].patterns
    assert patterns[metahost].match("/product/123/offers?test=1")
    assert not patterns[metahost].match("/model/123/offers?test=1")

    metahost = metahosts["/market/touch"]
    assert metahost.pattern.match("m.market.yandex.kz")
    assert not metahost.pattern.match("market.yandex.kz")
    patterns = metaroutes["/market/touch/market-touch_model"].patterns
    assert patterns[metahost].match("/model/123/offers?test=1")
    patterns = metaroutes["/market/touch/model"].patterns
    assert patterns[metahost].match("/model/123/offers?test=1")
    assert not patterns[metahost].match("/product/123/offers?test=1")
    patterns = metaroutes["/market/model"].patterns
    assert patterns[metahost].match("/model/123/offers?test=1")
    assert not patterns[metahost].match("/product/123/offers?test=1")

    metahost = metahosts["/market"]
    assert metahost.pattern.match("market.yandex.kz")
    assert metahost.pattern.match("m.market.yandex.kz")
