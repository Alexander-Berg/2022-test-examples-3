from travel.library.python.tools import replace_args


def test_replace_secrets_from_env():
    replaces = {'a1': 'b2',
                'd3': 'e4'}
    args = ['a1', 'c', 'd3', 'a1', 'a1=c', 'z=d3', 'cd3']
    result = replace_args(args, replaces)
    assert result == ['b2', 'c', 'e4', 'b2', 'b2=c', 'z=e4', 'ce4']
