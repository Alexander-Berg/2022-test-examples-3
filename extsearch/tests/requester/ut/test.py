from extsearch.geo.meta.tests.requester import make_url


def test_make_url():
    assert make_url('bering.search.yandex.net:8031', {}) == 'http://bering.search.yandex.net:8031/yandsearch'
    assert make_url('localhost:8031', {'origin': 'test'}) == 'http://localhost:8031/yandsearch?origin=test'

    # escaping
    assert make_url('localhost:8031', {'text': 'hello world'}) == 'http://localhost:8031/yandsearch?text=hello%20world'
    assert (
        make_url('localhost:8031', {'text': 'привет'})
        == 'http://localhost:8031/yandsearch?text=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82'
    )

    # http:// prefix is allowed
    assert make_url('http://localhost:8031', {}) == 'http://localhost:8031/yandsearch'
    assert make_url('http://localhost:8031', {'origin': 'test'}) == 'http://localhost:8031/yandsearch?origin=test'
    assert make_url('http://localhost:8031/', {'origin': 'test'}) == 'http://localhost:8031/yandsearch?origin=test'

    # leave yandsearch as is
    assert (
        make_url('http://localhost:8031/yandsearch', {'origin': 'test'})
        == 'http://localhost:8031/yandsearch?origin=test'
    )
    assert make_url('http://localhost:8031/yandsearch?', {}) == 'http://localhost:8031/yandsearch'
    assert (
        make_url('http://localhost:8031/yandsearch?', {'origin': 'test'})
        == 'http://localhost:8031/yandsearch?origin=test'
    )

    # substitute yandsearch with search
    assert make_url('http://localhost:8031/yandsearch?', {}, path='/search') == 'http://localhost:8031/search'
    assert (
        make_url('http://localhost:8031/yandsearch?', {'origin': 'test'}, path='/search')
        == 'http://localhost:8031/search?origin=test'
    )

    # preserve already existing params (sources, etc.)
    assert (
        make_url('http://localhost:8031/yandsearch?source=foo&source=bar', {})
        == 'http://localhost:8031/yandsearch?source=foo&source=bar'
    )
    assert (
        make_url('http://localhost:8031/yandsearch?source=foo&source=bar', {'origin': 'test'})
        == 'http://localhost:8031/yandsearch?source=foo&source=bar&origin=test'
    )
    assert (
        make_url('http://localhost:8031/yandsearch?source=foo&source=bar', {'p': ['1', '2']})
        == 'http://localhost:8031/yandsearch?source=foo&source=bar&p=1&p=2'
    )
    assert (
        make_url('http://localhost:8031/yandsearch?source=foo&source=bar', {'origin': 'test'}, path='/new')
        == 'http://localhost:8031/new?source=foo&source=bar&origin=test'
    )

    # complex path
    assert (
        make_url('http://addrs-testing.search.yandex.net/search/addrs_upper_p7/yandsearch?', {})
        == 'http://addrs-testing.search.yandex.net/search/addrs_upper_p7/yandsearch'
    )
    assert (
        make_url('http://addrs-testing.search.yandex.net/search/addrs_upper_p7/yandsearch?', {}, path='/search')
        == 'http://addrs-testing.search.yandex.net/search/addrs_upper_p7/search'
    )
