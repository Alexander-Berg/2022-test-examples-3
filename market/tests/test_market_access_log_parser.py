import itertools
import pytest

from yamarec_metarouter import loaders

from yamarec_log_parsers import exceptions
from yamarec_log_parsers import log_parser
from yamarec_log_parsers import market_access_log_parser


@pytest.fixture
def parser():
    loader = loaders.FolderLoader(["resfs/file/market/yamarec/log-parsers/data/metahosts/"])
    return market_access_log_parser.MarketAccessLogParser(loader)


@pytest.mark.parametrize("query, expected_event", [
    ("/?ncrnd=7016&hid=2417", ("view", "page", "main_page")),

    ("/product/10405?hid=2417", ("view", "page", "model_card", {"model_id": "10405"})),
    pytest.param("/product/10405d?hid=2417", (), marks=pytest.mark.xfail),
    pytest.param("/product/010405?hid=2417", (), marks=pytest.mark.xfail),
    ("/product/10405/spec/?hid=2417", ("view", "page", "model_card", {"model_id": "10405"})),
    ("/product--salmo-diamond-bolognese-light-2227-600/12173197",
     ("view", "page", "model_card", {"model_id": "12173197"})),
    ("/product--salmo-diamond-bolognese-light-2227-600/10405/reviews/?hid=2417", ("view", "page", "model_card", {"model_id": "10405"})),
    ("/product--salmo-diamond-bolognese-light-2227-600/10405/reviews", ("view", "page", "model_card", {"model_id": "10405"})),
    ("/product--salmo-diamond-bolognese-light-2227-600/10405/reviews?hid=123", ("view", "page", "model_card", {"model_id": "10405"})),
    ("/product--salmo-diamond-bolognese-light-2227-600/10405/reviews/", ("view", "page", "model_card", {"model_id": "10405"})),
    ("/product--salmo-diamond-bolognese-light-2227-600/10405/geo?hid=123", ("view", "page", "model_card", {"model_id": "10405"})),
    pytest.param("/product/10405/reviews/add", (), marks=pytest.mark.xfail),
    ("/geo.xml?modelid=13225790&hid=91529",
     ("view", "page", "model_card", {"model_id": "13225790", "category_id": "91529"})),

    ("/my/wishlist", ("view", "page", "wishlist", {})),
    ("/my/wishlist?track=cart", ("view", "page", "wishlist", {"track": "cart"})),

    ("/catalog/45113", ("view", "page", "category", {"ncategory_id": "45113"})),
    ("/catalog--telefony-i-aksessuary-k-nim/54437", ("view", "page", "category", {"ncategory_id": "54437"})),
    ("/catalog/45113/list?hid=1079",
     ("view", "page", "category",
      {"ncategory_id": "45113", "category_id": "1079", "leaf": True})),
    ("/catalog?nid=544189", ("view", "page", "category", {"ncategory_id": "544189"})),
    pytest.param("/catalog/04544/list?glfilter=5085116", (), marks=pytest.mark.xfail),
    pytest.param("/catalog/4544d/list?glfilter=5085116", (), marks=pytest.mark.xfail),
    pytest.param("/catalog/54544/list?hid=0100&glfilter=5085116", (), marks=pytest.mark.xfail),
    pytest.param("/catalog/54544/list?hid=100d&glfilter=5085116", (), marks=pytest.mark.xfail),

    ("/compare.xml", ("view", "page", "compare", {})),
    ("/compare.xml?hid=9149&CMD=-CMP%3D12859252",
     ("view", "page", "compare", {"category_id": "9149", "cmd": "-CMP%3D12859252"})),
    ("/compare/7MxLpV1qybhAUjdTRQXEcfcCJsk?hid=90575&id=33213001&id=31021016",
     ("view", "page", "compare", {"category_id": "90575"})),

    ("/search.xml?=Blue&cvredirect=2&presizer=56&text=obsgf%22obsgf",
     ("view", "page", "serp", {"text": "obsgf%22obsgf"})),
    ("/search.xml", ("view", "page", "serp", {})),
    ("/search.xml?=Blue&cvredirect=2&presizer=56&text=obsgf%22obsgf",
     ("view", "page", "serp", {"text": "obsgf%22obsgf"})),
    ("/search.xml?=Blue&cvredirect=2&presizer=56&text=obsgf%22obsgf",
     ("view", "page", "serp", {"text": "obsgf%22obsgf"})),
    ("/search?=Blue&cvredirect=2&presizer=56&text=obsgf%22obsgf",
     ("view", "page", "serp", {"text": "obsgf%22obsgf"})),
    ("/search-models.xml?list_id=22530640&model_ids=6377521%2C7012977%2C4768680",
     ("view", "page", "serp", {"model_ids": "6377521%2C7012977%2C4768680"})),
    pytest.param(
        "/search?&amp;glfilter=1222085:12220861&amp;hid=7286125&amp;nid=56319", (), marks=pytest.mark.xfail),

    ("/offer/n5bl9P12gdv5LmxtLse4mA?hid=91463&modelid=411045",
     ("view", "page", "offer_card",
      {"model_id": "411045", "category_id": "91463", "offer_id": "n5bl9P12gdv5LmxtLse4mA"}))
])
def test_desktop_log_parsing(parser, query, expected_event):
    expected_event = _market_event(*expected_event)
    log_record = _create_log_record(query, desktop=True, blue=False)
    event = parser.parse(log_record).next()
    _check_event(expected_event, event)
    assert _get_host(desktop=True, blue=False) == event.context.get("host")


@pytest.mark.parametrize("query, expected_event", [
    ("/product/10405?hid=2417", ("view", "page", "sku_card", {"sku_id": "10405"})),
    pytest.param("/product/10405d?hid=2417", (), marks=pytest.mark.xfail),
    pytest.param("/product/010405?hid=2417", (), marks=pytest.mark.xfail),
    ("/product--salmo-diamond-bolognese-light-2227-600/12173197",
        ("view", "page", "sku_card", {"sku_id": "12173197"})),
    ("/product/karta-pamiati-samsung-mb-mc64ga/1725463574?show-uid=15441956667686714351200000",
        ("view", "page", "sku_card", {"sku_id": "1725463574"})),
    ("/product/10405/spec/?hid=2417", ("view", "page", "sku_card", {"sku_id": "10405"})),
    ("/product/10405/spec", ("view", "page", "sku_card", {"sku_id": "10405"})),
    ("/product/10405/spec?hid=123", ("view", "page", "sku_card", {"sku_id": "10405"})),
    ("/product/10405/spec/", ("view", "page", "sku_card", {"sku_id": "10405"})),
    ("/product--salmo-diamond-bolognese-light-2227-600/10405/spec/?hid=2417",
        ("view", "page", "sku_card", {"sku_id": "10405"})),
    ("/product/karta-pamiati-samsung-mb-mc64ga/1725463574/spec",
        ("view", "page", "sku_card", {"sku_id": "1725463574"})),
    ("/product/10405/reviews/?hid=2417", ("view", "page", "sku_card", {"sku_id": "10405"})),
    ("/product/10405/reviews", ("view", "page", "sku_card", {"sku_id": "10405"})),
    ("/product/10405/reviews?hid=123", ("view", "page", "sku_card", {"sku_id": "10405"})),
    ("/product/10405/reviews/", ("view", "page", "sku_card", {"sku_id": "10405"})),
    ("/product--salmo-diamond-bolognese-light-2227-600/10405/reviews/?hid=2417",
        ("view", "page", "sku_card", {"sku_id": "10405"})),
    ("/product/karta-pamiati-samsung-mb-mc64ga/1725463574/reviews",
        ("view", "page", "sku_card", {"sku_id": "1725463574"})),
    pytest.param("/product/10405/reviews/add", (), marks=pytest.mark.xfail)
])
def test_blue_desktop_log_parsing(parser, query, expected_event):
    expected_event = _market_event(*expected_event)
    log_record = _create_log_record(query, desktop=True, blue=True)
    event = parser.parse(log_record).next()
    _check_event(expected_event, event)
    assert _get_host(desktop=True, blue=True) == event.context.get("host")


@pytest.mark.parametrize("query, expected_event", [
    ("/?ncrnd=7016", ("view", "page", "main_page", {})),

    ("/model/10405", ("view", "page", "model_card", {"model_id": "10405"})),
    ("/model?m=2417&modelid=10405", ("view", "page", "model_card", {"model_id": "10405"})),
    pytest.param("/model?m=2417&model_card=10405", (), marks=pytest.mark.xfail),
    ("/model/10405?modelid=01040d", ("view", "page", "model_card", {"model_id": "10405"})),
    pytest.param("/model?m=2417&modelid=010405", (), marks=pytest.mark.xfail),
    pytest.param("/model?m=2417&modelid=1040d", (), marks=pytest.mark.xfail),
    ("/model.xml?m=2417&modelid=10405", ("view", "page", "model_card", {"model_id": "10405"})),

    ("/product/10405", ("view", "page", "model_card", {"model_id": "10405"})),
    ("/product/10405/reviews/?hid=2417", ("view", "page", "model_card", {"model_id": "10405"})),
    ("/product/10405/reviews", ("view", "page", "model_card", {"model_id": "10405"})),
    ("/product/10405/reviews?hid=123", ("view", "page", "model_card", {"model_id": "10405"})),
    ("/product/10405/reviews/", ("view", "page", "model_card", {"model_id": "10405"})),
    ("/product/10405/geo?hid=123", ("view", "page", "model_card", {"model_id": "10405"})),
    ("/product--salmo-diamond-bolognese-light-2227-600/10405/reviews/?hid=2417", ("view", "page", "model_card", {"model_id": "10405"})),
    ("/product--salmo-diamond-bolognese-light-2227-600/10405/reviews", ("view", "page", "model_card", {"model_id": "10405"})),
    ("/product--salmo-diamond-bolognese-light-2227-600/10405/reviews?hid=123", ("view", "page", "model_card", {"model_id": "10405"})),
    ("/product--salmo-diamond-bolognese-light-2227-600/10405/reviews/", ("view", "page", "model_card", {"model_id": "10405"})),
    ("/product--salmo-diamond-bolognese-light-2227-600/10405/geo?hid=123", ("view", "page", "model_card", {"model_id": "10405"})),

    ("/catalog/12345/?hid=1079", ("view", "page", "category", {"category_id": "1079"})),
    ("/catalog--televizory/12345/?hid=1079", ("view", "page", "category", {"category_id": "1079"})),

    ("/category?hid=544239", ("view", "page", "category", {"category_id": "544239"})),

    ("/search?text=%D1%84%D0%B5%D1%82%D1%80&hid=6439639",
     ("view", "page", "serp", {"category_id": "6439639", "text": "%D1%84%D0%B5%D1%82%D1%80"})),
    ("/search.xml?=Blue&cvredirect=2&presizer=56&text=obsgf%22obsgf",
     ("view", "page", "serp", {"text": "obsgf%22obsgf"})),

    ("/offer/vKq40vJMmCUfqa-m9SKjxw?shop_id=720&hid=90586&modelid=116727&nid=54967",
     ("view", "page", "offer_card",
      {"category_id": "90586", "model_id": "116727", "offer_id": "vKq40vJMmCUfqa-m9SKjxw"}))
])
def test_touch_log_parsing(parser, query, expected_event):
    expected_event = _market_event(*expected_event)
    log_record = _create_log_record(query, desktop=False, blue=False)
    event = parser.parse(log_record).next()
    _check_event(expected_event, event)
    assert _get_host(desktop=False, blue=False) == event.context.get("host")


@pytest.mark.parametrize("query, expected_event", [
    ("/product/10405", ("view", "page", "sku_card", {"sku_id": "10405"})),
    pytest.param("/product?m=2417&skuid=10405", (), marks=pytest.mark.xfail),
    pytest.param("/product?m=2417&sku_card=10405", (), marks=pytest.mark.xfail),
    ("/product/10405?skuid=01040d", ("view", "page", "sku_card", {"sku_id": "10405"})),
    ("/product/karta-pamiati-samsung-mb-mc64ga/1725463574?show-uid=15441956667686714351200000",
        ("view", "page", "sku_card", {"sku_id": "1725463574"})),
    pytest.param("/product?m=2417&skuid=010405", (), marks=pytest.mark.xfail),
    pytest.param("/product?m=2417&skuid=1040d", (), marks=pytest.mark.xfail),
    pytest.param("/product.xml?m=2417&skuid=10405", (), marks=pytest.mark.xfail),
    ("/product/10405/reviews/?hid=2417", ("view", "page", "sku_card", {"sku_id": "10405"})),
    ("/product/10405/reviews", ("view", "page", "sku_card", {"sku_id": "10405"})),
    ("/product/10405/reviews?hid=123", ("view", "page", "sku_card", {"sku_id": "10405"})),
    ("/product/10405/reviews/", ("view", "page", "sku_card", {"sku_id": "10405"})),
    ("/product--salmo-diamond-bolognese-light-2227-600/10405/reviews/?hid=2417",
        ("view", "page", "sku_card", {"sku_id": "10405"})),
    ("/product/karta-pamiati-samsung-mb-mc64ga/1725463574/reviews",
     ("view", "page", "sku_card", {"sku_id": "1725463574"})),
])
def test_blue_touch_log_parsing(parser, query, expected_event):
    expected_event = _market_event(*expected_event)
    log_record = _create_log_record(query, desktop=False, blue=True)
    event = parser.parse(log_record).next()
    _check_event(expected_event, event)
    assert _get_host(desktop=False, blue=True) == event.context.get("host")


@pytest.mark.parametrize("query, expected_events", [
    ("/?ncrnd=7016", [("view", "page", "main_page")]),
    ("/product/10405?hid=2417",
     [
         ("view", "page", "model_card", {"model_id": "10405"}),
         ("view", "model", "10405", {})
     ])
])
def test_additional_events(parser, query, expected_events):
    log_record = _create_log_record(query)
    expected_events = map(lambda args: _market_event(*args), expected_events)
    for expected_event, event in itertools.izip_longest(expected_events, parser.parse(log_record)):
        _check_event(expected_event, event)


def test_invalid_user_id(parser):
    log_record = _create_log_record("/product/10405?hid=2417", yandexuid="03049364681458552732")
    with pytest.raises(exceptions.InvalidFormatError):
        parser.parse(log_record).next()


def test_context(parser):
    log_record = _create_log_record("/?ncrnd=7016&hid=2417")
    event = parser.parse(log_record).next()
    assert event.context.get("category_id") is not None
    assert event.context.get("ncrnd") is not None

    log_record = _create_log_record("/product/10405671/offers?hid=2417247&utm_source=email")
    event = parser.parse(log_record).next()
    assert event.timestamp == 1463034297
    assert event.user["puid"] == "1462949522"
    assert event.user["yandexuid"] == "3049364681458552732"
    assert \
        event.context["referer"] == \
        "https://aqua.yandex-team.ru/report/57337d98e4b08948c/index.html"
    assert event.context["req_id"] == "475e91a2aa801df99cc50acf41b10321"
    assert event.context.get("parent_reqid_seq") is None
    assert event.context["category_id"] == "2417247"
    assert event.context.get("utm_source") is None


def _get_host(desktop=True, blue=False):
    if blue:
        return "pokupki.market.yandex.ru:443" if desktop else "m.pokupki.market.yandex.ru"
    else:
        return "market.yandex.ru:443" if desktop else "m.market.yandex.ru"


def _create_log_record(request, desktop=True, blue=False, yandexuid=None):
    return {
        "vhost": _get_host(desktop=desktop, blue=blue),
        "timestamp": "2016-05-12T09:24:57",
        "timezone": "+0300",
        "request": request,
        "referer": "https://aqua.yandex-team.ru/report/57337d98e4b08948c/index.html",
        "req_id": "475e91a2aa801df99cc50acf41b10321",
        "yandexuid": yandexuid or "3049364681458552732",
        "puid": "1462949522"
    }


def _market_event(action, object_type, object_id, context=None):
    return log_parser.MarketEvent(
        action=action, object_type=object_type, object_id=object_id, context=context)


def _check_event(expected_event, event):
    assert expected_event.action == event.action
    assert expected_event.object_type == event.object_type
    assert expected_event.object_id == event.object_id
    for key, value in expected_event.context.iteritems():
        assert value == event.context.get(key)
