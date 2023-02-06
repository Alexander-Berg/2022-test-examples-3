from yamarec_metarouter.metahosts import Metahost
from yamarec_metarouter.metahosts import MetahostGroup


def test_metahost_can_act_as_key():
    currencies = {
        Metahost("market\.yandex\.ru"): "RUR",
        Metahost("market\.yandex\.by"): "BYR",
    }
    assert currencies[Metahost("market\.yandex\.ru")] == "RUR"
    assert currencies[Metahost("market\.yandex\.by")] == "BYR"
    assert currencies.get(Metahost("market\.yandex\.kz")) is None


def test_metahost_forms_correct_pattern():
    pattern = Metahost("market\.yandex\.(ru|by)").pattern
    assert pattern.match("market.yandex.ru")
    assert pattern.match("market.yandex.by")
    assert not pattern.match("market.yandex.kz")
    assert not pattern.match("market!yandex!ru")
    assert not pattern.match("market.yandex.ru:443")


def test_metahost_group_forms_correct_pattern():
    ru = Metahost("(m\.)?market\.yandex\.ru")
    kz = Metahost("m\.market\.yandex\.kz")
    pattern = MetahostGroup([ru, kz]).pattern
    assert pattern.match("market.yandex.ru")
    assert pattern.match("m.market.yandex.ru")
    assert pattern.match("m.market.yandex.kz")
    assert not pattern.match("market.yandex.kz")
    assert not pattern.match("m.market.yandex.kz:443")
