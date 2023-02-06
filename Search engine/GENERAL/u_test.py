from search.metrics.ccc.ccc_lib.criteria.criterion import TwoSampleCriterion


class UTest(TwoSampleCriterion):
    """The Mann-Whitney rank test. Wraps scipy.stats.mannwhitneyu."""

    def __init__(self, alternative="two-sided"):
        if alternative not in {None, "two-sided", "less", "greater"}:
            raise ValueError("`alternative` should be one of None, 'two-sided', 'less', 'greater'")
        self.alternative = alternative

    def name(self):
        return f"u-test_{self.alternative}" if self.alternative else "u-test"

    def description(self):
        return f"The Mann-Whitney rank test. Wraps scipy.stats.mannwhitneyu (with alternative={self.alternative})."

    def _calculate(self, *sequences) -> [float, float]:
        import scipy.stats as stats
        return stats.mannwhitneyu(*sequences, alternative=self.alternative)
