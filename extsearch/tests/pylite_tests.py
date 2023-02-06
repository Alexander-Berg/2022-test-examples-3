from extsearch.audio.tools.vh_tools import pylite_operations as pylite

import inspect
import pytest
import vh


def maybe_unused(*args):
    pass


def remove_comments(code):
    is_comment = lambda x: x.lstrip().startswith('#')
    return [line for line in code if not is_comment(line) and line != '']


def check_generator(expected):

    def inner(expected):
        for e in expected:
            x = yield
            assert x == e
        yield
        assert False
    ret = inner(expected)
    next(ret)
    return ret


class PytTestExecuter(object):
    def __init__(self, inputs, expected):
        self.inputs = inputs
        self.check = check_generator(expected)

    def __call__(self, **kwargs):
        code = kwargs['_options']['transformation']
        input = input1 = self.inputs
        output = lambda x: self.check.send(x)
        maybe_unused(input, input1, output)
        exec(code)


class CubicMock(object):
    def __call__(self, *args, **kwargs):
        return args, kwargs


class Replace(pylite.FunctionArgument):
    def __init__(self, replacement):
        self.replacement = replacement

    def __repr__(self):
        return self.replacement

    def render(self, value):
        return self

    def make_param(self, value):
        return pylite.CubicOptions(inputs={'inputs': value})


class Forward(pylite.FunctionArgument):
    def render(self, value):
        return value

    def need_option_expr(self, value):
        return value


class Configurator(pylite.Configurator):
    def Return(self, value):
        return 'r({})'.format(value)

    def Yield(self, value):
        return 'y({})'.format(value)


@pytest.fixture()
def noargs_function():
    def function():
        pass
    return function


@pytest.fixture()
def args_function():
    def function(a, b, c=20):
        return 1
    return function


@pytest.fixture()
def noargs_generator():
    def generator():
        yield 1
    return generator


def test_delete_leading_spaces():
    function = [
        '   def function():',
        '       pass'
    ]
    right = [
        'def function():',
        '    pass'
    ]
    assert pylite.delete_leading_spaces(function) == right


def test_delete_decor():
    decor = [
        '@decorator(',
        '    some=values',
        ')'
    ]
    function = [
        'def function():',
        '    pass'
    ]
    assert pylite.delete_decor(decor + function) == function


def test_get_function_source(noargs_function):
    source_code = [
        'def function():',
        '    pass'
    ]
    assert remove_comments(pylite.get_function_source(noargs_function)) == source_code


def test_calling_string_no_args(noargs_function):

    npy = pylite.NirvanaPython(noargs_function, CubicMock(), dict())

    npy._result_holder = 'result'
    npy._remote_args = 'args'
    npy._remote_kwargs = 'kwargs'
    npy._remote_it = 'i'

    expected_call_string = [
        'args = ()',
        'kwargs = {}',
        'result = function(*args, **kwargs)'
    ]
    assert npy.get_calling_string() == expected_call_string


def test_calling_string(args_function):

    npy = pylite.NirvanaPython(
        args_function,
        CubicMock(),
        arguments={
            'a': Replace('input'), 'b': Forward(), 'c': Forward()
        }
    )
    npy._result_holder = 'result'
    npy._remote_args = 'args'
    npy._remote_kwargs = 'kwargs'
    npy._remote_it = 'i'

    expected_call_string = [
        "args = (input, 'string', 20)",
        "kwargs = {}",
        'result = function(*args, **kwargs)'
    ]
    assert npy.get_calling_string(None, "string") == expected_call_string


def test_get_result_handle_stirng(noargs_function):
    npy = pylite.NirvanaPython(noargs_function, CubicMock(), dict(), configurator=Configurator())
    npy._result_holder = 'result'
    npy._remote_args = 'args'
    npy._remote_kwargs = 'kwargs'
    npy._remote_it = 'i'

    result = [
        'if result is not None:',
        '    r(result)'
    ]

    assert npy.get_result_handle_string() == result


def test_get_result_handle_stirng_generator(noargs_generator):
    npy = pylite.NirvanaPython(noargs_generator, CubicMock(), dict(), configurator=Configurator())
    npy._result_holder = 'result'
    npy._remote_args = 'args'
    npy._remote_kwargs = 'kwargs'
    npy._remote_it = 'i'

    result = [
        'for i in result:',
        '    y(i)'
    ]

    assert npy.get_result_handle_string() == result


def test_need_option_expr(args_function):
    npy = pylite.NirvanaPython(
        args_function,
        CubicMock(),
        arguments={
            'a': Forward(), 'b': Forward(), 'c': Forward()
        }
    )

    sig = inspect.signature(args_function)

    assert not npy.need_option_expr(sig.bind(False, False, False))
    assert npy.need_option_expr(sig.bind(False, True, False))


def test_nirvana_python_call(args_function):
    npy = pylite.NirvanaPython(
        args_function,
        CubicMock(),
        arguments={
            'a': Replace('input'), 'b': Forward(), 'c': Forward()
        },
        cubic_options=pylite.CubicOptions(options={'options': 'value'}),
        configurator=Configurator()
    )

    npy._result_holder = 'result'
    npy._remote_args = 'args'
    npy._remote_kwargs = 'kwargs'
    npy._remote_it = 'i'

    v = 'parameter'
    return npy(v, 1, 2)


def test_pyt_input():
    inp = pylite.PytInput('json')
    inp.set_number(1)
    a = vh.File('test.txt')
    assert repr(inp.render(a)) in ('input', 'input1')

    inp2 = pylite.PytInput('tsv')
    inp2.set_number(2)
    assert repr(inp2.render(a)) == 'input2'

    return [inp.make_param('value').make(), inp2.make_param('value').make()]


def test_transformer():
    @pylite.transformer(
        a=pylite.PytInput('text'), b=pylite.PytInput('json'),
        ttl=10,
        max_ram=20,
        max_disk=30,
        output_format='tsv',
        output2_format='json'
    )
    def function(a, b):
        pass

    expected_cubic_options = {
        '_options': {
            'ttl': 10, 'max-ram': 20, 'max-disk': 30, 'output-format': 'tsv', 'output2-format': 'json'
        },
        '_inputs': {},
        '_name': None
    }

    assert function._cubic_options.make() == expected_cubic_options
    assert set(function._arguments.keys()) == {'a', 'b'}
    assert isinstance(function._arguments['a'], pylite.PytInput)
    assert isinstance(function._arguments['b'], pylite.PytInput)

    assert function._arguments['a']._number in (1, 2) and function._arguments['b']._number in (1, 2)
    assert function._arguments['a']._number != function._arguments['b']._number


def test_transformer_code_execution():
    @pylite.transformer(a=pylite.PytInput('text'))
    def function(a):
        for i in a:
            yield i + ' world'

    function._cubic = PytTestExecuter(['Hello', 'Goodbye'], ['Hello world', 'Goodbye world'])
    function(a=['Hello', 'Goodbye'])


def test_check_global_option():
    with vh.Graph():
        option = vh.add_global_option('option', type=int, default=10)
        assert pylite.check_global(option)
        assert not pylite.check_global('totally not a global')


def test_option():
    with vh.Graph():
        option = pylite.Option(str)
        global_option = vh.add_global_option('option', type=int, default=10)
        assert option.render(global_option) == "${global.option}"
        assert option.render(pylite.Option.workflow_url()) == "${meta.workflow_url}"
        assert option.render("Hello world") == "Hello world"

        assert option.need_option_expr(global_option)
        assert option.need_option_expr(pylite.Option.workflow_url())
        assert not option.need_option_expr('string')
        assert option.need_option_expr(vh.OptionExpr('${global.option}'))


def test_mr_mapper():
    @pylite.mr_mapper(yt_token='token', row=pylite.TableRow())
    def function(row):
        yield row

    function._cubic = CubicMock()
    return function('table')
