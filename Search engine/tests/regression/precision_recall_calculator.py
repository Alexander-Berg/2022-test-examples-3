import numpy as np


class PrecisionRecallCalculator:
    def __init__(self, test_name="regression test"):
        self.true_positive = 0
        self.true_negative = 0
        self.false_positive = 0
        self.false_negative = 0
        self.test_name = test_name

    # expected: bool
    def expected(self, expected):
        return PRProxy(self, expected)

    def add_true_positive(self):
        self.true_positive += 1

    def add_true_negative(self):
        self.true_negative += 1

    def add_false_positive(self):
        self.false_positive += 1

    def add_false_negative(self):
        self.false_negative += 1

    def recall(self):
        if self.true_positive + self.false_negative == 0:
            return np.inf
        else:
            return float(self.true_positive) / (self.true_positive + self.false_negative)

    def precision(self):
        if self.true_positive + self.false_positive == 0:
            return np.inf
        else:
            return float(self.true_positive) / (self.true_positive + self.false_positive)

    def fscore(self):
        p = self.precision()
        r = self.recall()
        if not p or not r:
            return np.inf
        else:
            return 2. * p * r / (p + r)

    def __str__(self):
        str = \
        "Statistics for {0}\n".format(self.test_name) +\
        "-" * len(self.test_name) + "\n" +\
        "{0:20s}: {1:.5f}\n".format("Precision ",        self.precision()) +\
        "{0:20s}: {1:.5f}\n".format("Recall ",           self.recall()) +\
        "{0:20s}: {1:.5f}\n".format("FScore ",           self.fscore()) +\
        "{0:20s}: {1:d}\n".format("True positive ",    self.true_positive) +\
        "{0:20s}: {1:d}\n".format("True negative ",    self.true_negative) +\
        "{0:20s}: {1:d}\n".format("False positive ",   self.false_positive) +\
        "{0:20s}: {1:d}\n".format("False negative ",   self.false_negative)

        return str


class PRProxy:
    # prctor: PrecisionRecallCalculator
    # expected: bool
    def __init__(self, prctor, expected):
        self.expected = expected
        self.prctor = prctor

    # found: bool
    def found(self, found):
        if self.expected:
            if found:
                self.prctor.add_true_positive()
            else:
                self.prctor.add_false_negative()
        else:
            if found:
                self.prctor.add_false_positive()
            else:
                self.prctor.add_true_negative()
