from extsearch.geo.meta.tests.requester.request import Request, Locale


def test_search_toponym_with_no_lang(pb_searcher):
    text = 'москва'

    response = pb_searcher.execute(Request().set_text(text).set_locale(Locale.ru_RU))
    assert response.is_non_empty()

    response = pb_searcher.execute(Request().set_text(text))
    assert response.is_non_empty()
