import pytest
from six.moves.urllib.parse import urlsplit, parse_qs

from travel.avia.library.python.urls.errors import MalformedUrlParameterValue
from travel.avia.library.python.urls.search import C, S, TravelAviaSearch, TravelAviaPrefilledFields


def assert_url(url, netloc, path, query_params):
    split = urlsplit(url)
    assert split.netloc == netloc
    assert split.path == path
    parsed_qs = parse_qs(split.query)
    assert parsed_qs == query_params


@pytest.mark.parametrize(('url_class', 'host', 'url_path'), [
    (TravelAviaSearch, 'travel-test.yandex.ru', '/avia/search/result/'),
    (TravelAviaSearch, 'travel.yandex.ru', '/avia/search/result/'),
    (TravelAviaPrefilledFields, 'travel-test.yandex.ru', '/avia/'),
    (TravelAviaPrefilledFields, 'travel.yandex.ru', '/avia/'),
])
def test_search(url_class, host, url_path):
    assert_url(
        url_class(host).url(213, 2, '2020-01-02', return_date=None),
        host,
        url_path,
        dict(
            adult_seats=['1'], children_seats=['0'], infant_seats=['0'], klass=['economy'],
            fromId=['c213'], toId=['c2'], when=['2020-01-02'],
        ),
    )

    assert_url(
        url_class(host).url(213, 2, '2020-01-02', return_date='2020-02-02'),
        host,
        url_path,
        dict(
            adult_seats=['1'], children_seats=['0'], infant_seats=['0'], klass=['economy'],
            fromId=['c213'], toId=['c2'], when=['2020-01-02'], return_date=['2020-02-02'],
        ),
    )

    assert_url(
        url_class(host).url(213, 2, '2020-01-02'),
        host,
        url_path,
        dict(
            adult_seats=['1'], children_seats=['0'], infant_seats=['0'], klass=['economy'],
            fromId=['c213'], toId=['c2'], when=['2020-01-02'],
        ),
    )

    assert_url(
        url_class(host).url(C(213), S(608002), '2020-01-02'),
        host,
        url_path,
        dict(
            adult_seats=['1'], children_seats=['0'], infant_seats=['0'], klass=['economy'],
            fromId=['c213'], toId=['s608002'], when=['2020-01-02'],
        ),
    )

    assert_url(
        url_class(host).url(213, 2),
        host,
        url_path,
        dict(
            adult_seats=['1'], children_seats=['0'], infant_seats=['0'], klass=['economy'],
            fromId=['c213'], toId=['c2'],
        ),
    )

    assert_url(
        url_class(host).url(213, 2, ''),
        host,
        url_path,
        dict(
            adult_seats=['1'], children_seats=['0'], infant_seats=['0'], klass=['economy'],
            fromId=['c213'], toId=['c2'],
        ),
    )

    assert_url(
        url_class(host).url(
            213, 2, '2020-01-02', adults=2, children=3, infants=4, klass='business',
        ),
        host,
        url_path,
        dict(
            adult_seats=['2'], children_seats=['3'], infant_seats=['4'], klass=['business'],
            fromId=['c213'], toId=['c2'], when=['2020-01-02'],
        ),
    )

    assert_url(
        url_class(host).url(
            '-me', 2, '2020-01-02', adults=2, children=3, infants=4, klass='business',
        ),
        host,
        url_path,
        dict(
            adult_seats=['2'], children_seats=['3'], infant_seats=['4'], klass=['business'],
            fromId=['-me'], toId=['c2'], when=['2020-01-02'],
        ),
    )

    assert_url(
        url_class(host).url(
            213, '-me', '2020-01-02', adults=2, children=3, infants=4, klass='business',
        ),
        host,
        url_path,
        dict(
            adult_seats=['2'], children_seats=['3'], infant_seats=['4'], klass=['business'],
            fromId=['c213'], toId=['-me'], when=['2020-01-02'],
        ),
    )

    with pytest.raises(MalformedUrlParameterValue):
        url_class(host).url(None, 2, '2020-01-02')

    with pytest.raises(MalformedUrlParameterValue):
        url_class(host).url(213, None, '2020-01-02')
