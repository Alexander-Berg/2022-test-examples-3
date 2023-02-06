import numpy as np
import pytest

from crypta.lib.python import classification_thresholds
from crypta.lib.python.classification_thresholds.test import data_for_test


@pytest.mark.parametrize('table,segments,needed_recalls,needed_recall,constant_for_full_coverage,expected', [
    (data_for_test.first_table, ['male', 'female'], np.array([0.5, 0.5]), 1.0, 0.5, [0.4, 0.2]),
    (data_for_test.second_table, ['A', 'B1', 'B2', 'C1', 'C2'], np.array([0.08, 0.38, 0.38, 0.15, 0.01]), 0.9, 1.0,
     [0.1, 0.4, 0.3, 0.4, 0.2]),
])
def test_find_thresholds(table, segments, needed_recalls, needed_recall, constant_for_full_coverage, expected):
    assert np.allclose(classification_thresholds.find_thresholds(
        table, segments, needed_recalls, needed_recall, constant_for_full_coverage), expected)
