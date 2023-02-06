from yamarec_metarouter.metahosts import Metahost
from yamarec_metarouter.metaroutes import MetarouteGroup
from yamarec_metarouter.metaroutes import Route
from yamarec_metarouter.pattern import Pattern


def test_route_is_simple_metaroute():
    route = Route(Metahost("market\.yandex\.ru"), Pattern("/product/[0-9]+"))
    assert len(route.patterns) == 1
    assert Metahost("market\.yandex\.ru") in route.patterns
    assert route.patterns[Metahost("market\.yandex\.ru")] == Pattern("/product/[0-9]+")


def test_metaroute_group_forms_correct_patterns():
    desktop = Metahost("market\.yandex\.ru")
    touch = Metahost("m\.market\.yandex\.ru")
    desktop_product = Route(desktop, Pattern("/product/(?P<id>[0-9]+)"))
    desktop_model = Route(desktop, Pattern("/model\.xml\?id=(?P<id>[0-9]+)"))
    touch_model = Route(touch, Pattern("/model/(?P<id>[0-9]+)"))
    model = MetarouteGroup([desktop_product, desktop_model, touch_model])
    assert len(model.patterns) == 2
    assert model.patterns[desktop].extract("/product/123")["id"] == "123"
    assert model.patterns[desktop].extract("/model.xml?id=123")["id"] == "123"
    assert model.patterns[touch].extract("/model/123")["id"] == "123"
    assert not model.patterns[desktop].match("/model/123")
    assert not model.patterns[touch].match("/product/123")


def test_metaroute_induces_correctly():
    desktop = Metahost("market\.yandex\.ru")
    touch = Metahost("m\.market\.yandex\.ru")
    desktop_model = Route(desktop, Pattern("/product/(?P<id>[0-9]+)"))
    touch_model = Route(touch, Pattern("/model/(?P<id>[0-9]+)"))
    model = MetarouteGroup([desktop_model, touch_model])
    assert model.induce(desktop).extract("/product/123")["id"] == "123"
    assert not model.induce(desktop).match("/model/123")
    assert model.induce(touch).extract("/model/123")["id"] == "123"
    assert not model.induce(touch).match("/product/123")
