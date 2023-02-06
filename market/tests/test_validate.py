import market.dynamic_pricing.tools.validate_exception_list.lib.validate as test_module


def test_validate():
    assert(test_module.find_ssku_with_problem([["1.1"], ["1.1"]]) == ["1.1"])
    assert(test_module.find_ssku_with_problem([["1.1"], ["1.2"]]) == [])


def test_drop_duplicates():
    assert(test_module.drop_lines([["1", "1.1"], ["2", "1.1"]], mode="drop-new") == [["1", "1.1"]])
    assert(test_module.drop_lines([["1", "1.1"], ["2", "1.1"]], mode="drop-old") == [["2", "1.1"]])
