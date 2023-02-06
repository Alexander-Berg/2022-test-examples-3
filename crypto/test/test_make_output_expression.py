import collections
import os.path

import six
import sympy
from sympy.logic import boolalg
import tabulate
import yatest.common

from crypta.profile.services.socdem_expressions_for_direct import lib


VAR_1 = sympy.symbols('var1')
VAR_2 = sympy.symbols('var2')
VAR_3 = sympy.symbols('var3')
VAR_4 = sympy.symbols('var4')
VAR_5 = sympy.symbols('var5')


CaseInfo = collections.namedtuple('CaseInfo', ['id', 'socdem_possibilities'])


def make_test_case(id, variables, possibilities):
    return CaseInfo(id, lib.SocdemPossibilities(variables, possibilities, True))


TEST_CASES = [
    make_test_case(
        id=u'identical',
        variables=(VAR_1,),
        possibilities={
            (False,): False,
            (True,): True,
        },
    ),
    make_test_case(
        id=u'negated',
        variables=(VAR_1,),
        possibilities={
            (False,): True,
            (True,): False,
        },
    ),
    make_test_case(
        id=u'negated_with_insignificant',
        variables=(VAR_1, VAR_2),
        possibilities={
            (False, False): True,
            (True, False): False,
            (False, True): True,
        },
    ),
    make_test_case(
        id=u'turns_to_true',
        variables=(VAR_1, VAR_2),
        possibilities={
            (False, False): True,
            (True, False): False,
            (False, True): True,
            (True, True): True,
        },
    ),
    make_test_case(
        id=u'and_not',
        variables=(VAR_1, VAR_2, VAR_3),
        possibilities={
            (False, False, False): False,
            (False, False, True): False,
            (False, True, False): True,
            (False, True, True): False,
            (True, False, False): False,
            (True, False, True): False,
            (True, True, False): True,
            (True, True, True): False,
        },
    ),
    make_test_case(
        id=u'and_of_ors',
        variables=(VAR_1, VAR_2, VAR_3),
        possibilities={
            (False, False, False): False,
            (False, False, True): False,
            (False, True, False): True,
            (False, True, True): True,
            (True, False, False): False,
            (True, False, True): True,
            (True, True, False): True,
            (True, True, True): True,
        },
    ),
    make_test_case(
        id=u'one_var_stays',
        variables=(VAR_1, VAR_2, VAR_3),
        possibilities={
            (False, False, False): False,
            (False, False, True): False,
            (False, True, False): True,
            (False, True, True): True,
            (True, False, False): False,
            (True, True, False): True,
        },
    ),
    make_test_case(
        id=u'or_and_not',
        variables=(VAR_1, VAR_2, VAR_3),
        possibilities={
            (False, False, False): False,
            (False, False, True): False,
            (False, True, False): True,
            (False, True, True): False,
            (True, False, False): True,
            (True, False, True): False,
            (True, True, False): True,
            (True, True, True): False,
        },
    ),
    make_test_case(
        id=u'long_example',
        variables=(VAR_1, VAR_2, VAR_3, VAR_4, VAR_5),
        possibilities={
            (False, False, False, False, False): False,
            (False, False, False, True, True): False,
            (False, False, True, True, False): False,
            (False, True, False, False, True): False,
            (True, False, False, False, False): True,
            (False, True, False, False, False): True,
            (False, False, True, False, False): True,
            (False, False, False, True, False): False,
            (False, False, False, False, True): False,
        },
    ),
    make_test_case(
        id=u'wider_socdem',
        variables=(VAR_1, VAR_2),
        possibilities={
            (False, False): False,
            (True, False): True,
            (False, True): True,
            (True, True): False,
        },
    ),
]


def test_make_output_expression():
    output_path = os.path.join(yatest.common.work_path(), 'result.txt')

    table = []
    for test_case in TEST_CASES:
        expression, fits_perfectly = lib.make_output_expression(test_case.socdem_possibilities)

        if isinstance(expression, boolalg.BooleanTrue):
            output_expression = u'True'
        else:
            output_expression = lib.convert_to_string(expression)

        table.append([test_case.id, fits_perfectly, output_expression])

    with open(output_path, 'w') as fout:
        six.print_(tabulate.tabulate(table, headers=['id', 'fits_perfectly', 'output_expression']), file=fout)

    return yatest.common.canonical_file(output_path, local=True)
