import pytest_factor.factortypes
def test_factor_import(testdir):
    p = testdir.makepyfile('''
    import pytest
    from pytest_factor.factortypes.config.factor import Factor
    from pytest_factor.factortypes.config.checker import Checker
    from pytest_factor.factortypes.storages import QueryStageStorage

    @pytest.fixture
    def answersdump():
        return QueryStageStorage()

    factor = Factor(name='factor', checkers=[Checker('1'), Checker('2'), Checker('3')])
    ''')
    plugin = __import__('pytest_factor.factortypes.factortest')
    factortest_plugin = plugin._factorpytest.factortest
    items, reprec = testdir.inprocess_run([p], plugins=[factortest_plugin])
    reprec.assertoutcome(passed=3, failed=0)
    pass


def test_factor_eval(testdir):
    p = testdir.makepyfile('''
    from pytest_factor.factortypes.config.factor import Factor
    from pytest_factor.factortypes.config.checker import Checker


    def evaluate(ans):
        return ans['a']


    def sum(query):
        return reduce(lambda a, b: a + b, query)


    def tf(queries, betas, data):
        for query in queries:
            if query > 10 : return False
        return True


    checker = Checker('simple', queryDataProcessor=sum, betaDataProcessor=sum, testFunction=tf)
    factor = Factor(name='factor', formula=evaluate, checkers=[checker])

    import pytest
    from pytest_factor.factortypes.storages import QueryStageStorage
    from pytest_factor.factortypes.query import Query
    from pytest_factor.factortypes.stage import Stage


    @pytest.fixture
    def answersdump():
        q1 = Query(text='q1', lr=1)
        q2 = Query(text='q2', lr=2)
        q3 = Query(text='q3', lr=3)
        q4 = Query(text='q4', lr=4)
        q5 = Query(text='q5', lr=5)
        s1 = Stage(host='y1.ru')
        s2 = Stage(host='y2.ru')
        qs = QueryStageStorage()
        qs.set_value(q1, s1, {'a': 1})
        qs.set_value(q2, s1, {'a': 2})
        qs.set_value(q3, s1, {'a': 3})
        qs.set_value(q4, s1, {'a': 4})
        qs.set_value(q5, s1, {'a': 5})
        qs.set_value(q1, s2, {'a': 5})
        qs.set_value(q2, s2, {'a': 4})
        qs.set_value(q3, s2, {'a': 3})
        qs.set_value(q4, s2, {'a': 2})
        qs.set_value(q5, s2, {'a': 1})

        return qs
    ''')
    plugin = __import__('pytest_factor.factortypes.factortest')._factorpytest.factortest
    items, reprec = testdir.inprocess_run([p], plugins=[plugin])
    reprec.assertoutcome(passed=1, failed=0)


