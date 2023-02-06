from src.common.alert import SolomonChart


def _assert_program(given_program: str, expected: str, operation: str):
    alert = SolomonChart(program=given_program, project='')
    assert alert._make_operation_program(operation) == expected


def test_program():
    _assert_program('1;2;3;pr', '1;2;3;sum(pr)', 'sum')
    _assert_program('1;2;3;pr', '1;2;3;avg(pr)', 'avg')
    _assert_program('1;2;3;pr', '1;2;3;int(pr)', 'int')

    _assert_program('1', 'f(1)', 'f')
    _assert_program('1;2', '1;f(2)', 'f')
    _assert_program('1; 3 2', '1;f( 3 2)', 'f')
