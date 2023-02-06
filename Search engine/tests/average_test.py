import pytest

from average import AverageMetric
from weight import WeightComponentMetric
from test_utils import create_serp, create_failed_serp, create_component, SCALE

JUDGED_P_WEIGHT = WeightComponentMetric(SCALE, {'RELEVANT_PLUS': 1.0, 'RELEVANT_MINUS': 0.5}, True)
JUDGED_IMAGES_NORMALIZED_P10 = AverageMetric(JUDGED_P_WEIGHT, 10)
JUDGED_IMAGES_NORMALIZED_P10_STRICT = AverageMetric(JUDGED_P_WEIGHT, 10, strict=True)


def test_10_rel_plus():
    serp = create_serp([create_component('RELEVANT_PLUS')] * 10)
    assert JUDGED_IMAGES_NORMALIZED_P10(serp) == 1.0
    assert JUDGED_IMAGES_NORMALIZED_P10_STRICT(serp) == 1.0


def test_rel_plus_minus():
    serp = create_serp([create_component('RELEVANT_MINUS'), create_component('RELEVANT_PLUS')])
    assert JUDGED_IMAGES_NORMALIZED_P10(serp) == 0.75
    assert JUDGED_IMAGES_NORMALIZED_P10_STRICT(serp) == 0.15


def test_rel_plus_none_minus():
    serp = create_serp([create_component('RELEVANT_MINUS'), create_component(), create_component('RELEVANT_PLUS')])
    assert JUDGED_IMAGES_NORMALIZED_P10(serp) == 0.75
    assert JUDGED_IMAGES_NORMALIZED_P10_STRICT(serp) == 0.15


def test_rel_plus_unknown():
    serp = create_serp([create_component('RELEVANT_PLUS'), create_component('UNKNOWN')])
    assert JUDGED_IMAGES_NORMALIZED_P10(serp) == 0.5
    assert JUDGED_IMAGES_NORMALIZED_P10_STRICT(serp) == 0.1


def test_failed_serp():
    serp = create_failed_serp()
    assert JUDGED_IMAGES_NORMALIZED_P10(serp) == 0.0
    assert JUDGED_IMAGES_NORMALIZED_P10_STRICT(serp) == 0.0


def test_invalid_arguments():
    with pytest.raises(ValueError):
        AverageMetric(JUDGED_P_WEIGHT, strict=True)
