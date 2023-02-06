from math import nan

from search.metrics.ccc.ccc_lib.criteria.criterion import CriterionResult


def test_criterion_result_post_init():
    criterion_result = CriterionResult(name="foo_test", value=nan, p_value=nan)
    assert not criterion_result
    assert criterion_result.value is None
    assert criterion_result.p_value is None
    assert criterion_result.messages
    assert criterion_result.messages["val_exception"] == "value = nan, p-value = nan, cannot have nan"
    assert len(criterion_result.messages) == 1


def test_criterion_result_post_init_messages_with_messages():
    criterion_result = CriterionResult(name="foo_test", value=nan, p_value=nan, messages={"error": "input too red"})
    assert not criterion_result
    assert criterion_result.value is None
    assert criterion_result.p_value is None
    assert criterion_result.messages["val_exception"] == "value = nan, p-value = nan, cannot have nan"
    assert criterion_result.messages["error"] == "input too red"
    assert len(criterion_result.messages) == 2


def test_criterion_result_post_init_ok():
    criterion_result = CriterionResult(name="foo_test", value=1, p_value=0.5)
    assert criterion_result
    assert criterion_result.value == 1.0
    assert criterion_result.p_value == 0.5
    assert not criterion_result.messages
