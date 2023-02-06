from typing import Iterable

from search.metrics.ccc.ccc_lib.criteria.criterion import TwoSampleCriterion

CONSTANT_SEQUENCES_MESSAGES = {"exception": "The t-test cannot be applied to constant sequences"}


class PairedTTest(TwoSampleCriterion):
    """The paired t-test, used for intersected comparison. Wraps scipy.stats.ttest_rel."""

    CONSTANT_SEQUENCES_MESSAGES = CONSTANT_SEQUENCES_MESSAGES

    _name = "t-test_paired"

    def _calculate(self, left: Iterable[float], right: Iterable[float]) -> [float, float]:
        if len(set(left)) == 1 and len(set(right)) == 1:
            return None, None, self.CONSTANT_SEQUENCES_MESSAGES, False
        try:
            import scipy.stats as stats
            val, p = stats.ttest_rel(left, right)
            return val, p, None, True
        except Exception as e:
            return None, None, {"exception": str(e)}, False


class TTestInd(TwoSampleCriterion):
    """The T-test for the means of two independent samples. Wraps scipy.stats.ttest_ind."""

    CONSTANT_SEQUENCES_MESSAGES = CONSTANT_SEQUENCES_MESSAGES

    def __init__(self, equal_var: bool = True):
        if equal_var not in {True, False}:
            raise ValueError("`equal_var` should be one of True, False")
        self.equal_var = equal_var

    def name(self):
        return "t-test_ind" if self.equal_var else "t-test_welch"

    def description(self):
        return f"The T-test for the means of two independent samples. Wraps scipy.stats.ttest_ind (with equal_var={self.equal_var})."

    def _calculate(self, left, right) -> [float, float]:
        if len(set(left)) == 1 and len(set(right)) == 1:
            return None, None, self.CONSTANT_SEQUENCES_MESSAGES, False
        import scipy.stats as stats
        return stats.ttest_ind(left, right, equal_var=self.equal_var)
