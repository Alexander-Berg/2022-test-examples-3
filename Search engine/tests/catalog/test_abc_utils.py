from . import parse_abc_slug_valid_urls, parse_abc_slug_invalid_urls
from search.mon.workplace.src.libs.catalog.abc_utils import parse_abc_slug


class Test():
    def test_parse_abc_slug(self):
        for url, slug in iter(parse_abc_slug_valid_urls.items()):
            assert parse_abc_slug(url) == slug

        for url, slug in iter(parse_abc_slug_invalid_urls.items()):
            assert parse_abc_slug(url) != slug
