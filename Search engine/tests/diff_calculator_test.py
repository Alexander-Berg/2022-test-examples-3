from typing import Iterable

from pytest import approx

from search.metrics.ccc.ccc_lib.calculators.diff_calculator import DiffResult
from search.metrics.ccc.ccc_lib.ccc_helper import get_calculator_by_name


def test_diff_intersected_calculator():
    intersected_calculator = get_calculator_by_name("intersected")
    left = [312, 242, 388, 340, 296, 254, 391, 402, 290]
    right = [300, 201, 232, 312, 220, 256, 328, 330, 231]
    diff_result: DiffResult = intersected_calculator(
        left,
        right
    )
    assert diff_result
    assert diff_result.type == "intersected"
    _check_means_and_diff(diff_result, left, right)
    assert diff_result.left_count == diff_result.left_count == len(left) == len(right)
    expected_p_value = 0.006501814952
    assert diff_result.p_value == approx(expected_p_value)
    assert not diff_result.messages
    asdict = diff_result.asdict()
    assert asdict["p_value"] == approx(expected_p_value)
    assert "messages" not in asdict


def test_diff_intersected_calculator_mismatched_lengths():
    intersected_calculator = get_calculator_by_name("intersected")
    left = [312, 242, 388]
    right = [300, 201]
    diff_result: DiffResult = intersected_calculator(
        left,
        right
    )
    assert not diff_result
    assert not diff_result.ok
    _check_means_and_diff(diff_result, left, right)
    assert diff_result.p_value is None
    assert diff_result.messages
    assert diff_result.messages['exception'] == "unequal length arrays"
    assert "messages" in diff_result.asdict()


def test_diff_intersected_calculator_constant_sequences():
    intersected_calculator = get_calculator_by_name("intersected")
    left = [0, 0, 0]
    right = [0, 0, 0]
    diff_result: DiffResult = intersected_calculator(
        left,
        right
    )
    assert not diff_result
    assert not diff_result.ok
    _check_means_and_diff(diff_result, left, right)
    assert diff_result.p_value is None
    assert diff_result.messages == intersected_calculator.test.CONSTANT_SEQUENCES_MESSAGES
    assert "messages" in diff_result.asdict()


def _check_means_and_diff(diff_result: DiffResult, left: Iterable[float], right: Iterable[float]) -> None:
    expected_left, expected_right = _naive_average(left), _naive_average(right)
    assert diff_result.left_mean == approx(expected_left)
    assert diff_result.right_mean == approx(expected_right)
    assert diff_result.diff == approx(expected_left - expected_right)


def _naive_average(seq: Iterable[float]) -> float:
    assert seq, "Average is not defined for empty sequences"
    return sum(seq) / len(seq)
