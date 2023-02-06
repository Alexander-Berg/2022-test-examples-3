def deltaTest(delta=0.01):
    def test(queries, betas, value):
        assert value < delta

    return test

def thresholdTest(threshold=0.05):
    def test(queries, betas, value):
        assert threshold is None or value < threshold, 'error rate out of expected range (%s < %s)' % (value, threshold)

    return test


def greaterThenTest(limit=0):
    def test(queries, betas, value):
        assert value > limit, 'value is less then expected limit(%s > %s)' % (value, limit)

    return test
