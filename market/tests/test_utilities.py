import re

import yamarec_metarouter.utilities


def test_caching_decorator_works():

    class Calculator(object):

        def __init__(self):
            self.call_count = 0

        @yamarec_metarouter.utilities.cached
        def calculate(self, x, y):
            self.call_count += 1
            return x**2 + y**2

    calculator = Calculator()
    assert calculator.call_count == 0
    assert calculator.calculate(3, 4) == 25
    assert calculator.call_count == 1
    assert calculator.calculate(3, 4) == 25
    assert calculator.call_count == 1
    assert calculator.calculate(1, 2) == 5
    assert calculator.call_count == 2
    assert calculator.calculate(3, 4) == 25
    assert calculator.call_count == 2
    assert calculator.calculate(1, 2) == 5
    assert calculator.call_count == 2
    assert calculator.calculate(-1, 0) == 1
    assert calculator.call_count == 3


def test_fusion_leaves_single_pattern_as_it_is():
    pattern = yamarec_metarouter.utilities.fuse(["market\.yandex\.kz"])
    assert pattern == "market\.yandex\.kz"


def test_patterns_get_fused_correctly():
    pattern = yamarec_metarouter.utilities.fuse(["market\.yandex\.by", "market\.yandex\.kz"])
    assert re.match(pattern, "market.yandex.by")
    assert re.match(pattern, "market.yandex.kz")
    assert re.match(pattern, "market.yandex.ru") is None


def test_patterns_with_common_prefix_get_fused_efficiently():
    by = "market\.yandex\.by"
    kz = "market\.yandex\.kz"
    assert len(yamarec_metarouter.utilities.fuse([by, kz])) < len(by) + len(kz)


def test_common_prefix_gets_determined_correctly():
    re.compile(yamarec_metarouter.utilities.fuse(["xabc", "xa?bd"]))
    re.compile(yamarec_metarouter.utilities.fuse(["xyz\\a", "xyz\\b"]))


def test_common_suffix_gets_determined_correctly():
    pattern = yamarec_metarouter.utilities.fuse(["a(b)?$", "x(y)?$"])
    assert re.match(pattern, "a")
    assert re.match(pattern, "ab")
    assert re.match(pattern, "abc") is None
    assert re.match(pattern, "x")
    assert re.match(pattern, "xy")
    assert re.match(pattern, "xyz") is None


def test_slash_is_handled_correctly():
    pattern = yamarec_metarouter.utilities.fuse(["abc\n", "abc?n"])
    assert re.match(pattern, "abc\n")
    assert re.match(pattern, "abn")


def test_items_get_grouped_correctly():
    items = [
        ("father", 2),
        ("mother", 3),
        ("blahblahok", 2),
        ("whatthefuc", 5),
        ("seventeencharacte", 17)
    ]
    divisor_sums = yamarec_metarouter.utilities.group(
        items,
        grouper=len,
        mapper=(lambda _, value: value),
        reducer=sum)
    assert divisor_sums == {6: 5, 10: 7, 17: 17}
