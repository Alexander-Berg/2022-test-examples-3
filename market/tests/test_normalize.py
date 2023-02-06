from market.dynamic_pricing.pricing.library.normalize_url_utils import cut_query, remove_subdomain


class __YaTestEnv:  # for "ya make -t ..."
    def __init__(self):
        import yatest.common
        self.python_bin = yatest.common.python_path()
        self.work_dir = yatest.common.work_path()
        self.binary_path = yatest.common.binary_path
        self.source_path = yatest.common.source_path


_env = __YaTestEnv()


def test_cut_query():
    assert cut_query("https://a.b.c.ya.ru/any;param1=val1") == "https://a.b.c.ya.ru/any"
    assert cut_query("https://a.b.c.ya.ru/any?param1=val1") == "https://a.b.c.ya.ru/any"
    assert cut_query("https://a.b.c.ya.ru/any#fragment") == "https://a.b.c.ya.ru/any"
    assert cut_query("http://a.b.c.ya.ru/any;param1=val1?param1=val1#fragment") == "http://a.b.c.ya.ru/any"
    assert cut_query("a.b.c.ya.ru/any;param1=val1?param1=val1#fragment") == "https://a.b.c.ya.ru/any"


def test_remove_subdomain():
    assert remove_subdomain("https://a.b.c.ya.ru/any") == "https://ya.ru/any"
    assert remove_subdomain("https://a.b.c.ya.ru/any", 3) == "https://c.ya.ru/any"
    assert remove_subdomain("https://a.b.c.ya.ru/any", 2, "www") == "https://www.ya.ru/any"
