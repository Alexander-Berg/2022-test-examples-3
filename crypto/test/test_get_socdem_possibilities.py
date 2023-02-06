import os.path

import pytest
import six
import sympy
import tabulate
import yatest.common

from crypta.profile.services.socdem_expressions_for_direct import lib


GENDER_MALE = sympy.symbols(u'gender_male')
GENDER_FEMALE = sympy.symbols(u'gender_female')

AGE_18_MINUS = sympy.symbols(u'age_18_minus')
AGE_18_24 = sympy.symbols(u'age_18_24')
AGE_25_34 = sympy.symbols(u'age_25_34')
AGE_35_44 = sympy.symbols(u'age_35_44')
AGE_45_54 = sympy.symbols(u'age_45_54')
AGE_55_PLUS = sympy.symbols(u'age_55_plus')

INCOME_A = sympy.symbols(u'income_a')
INCOME_B1 = sympy.symbols(u'income_b1')
INCOME_B2 = sympy.symbols(u'income_b2')
INCOME_C1 = sympy.symbols(u'income_c1')
INCOME_C2 = sympy.symbols(u'income_c2')

E1 = sympy.symbols(u'export-1')
E2 = sympy.symbols(u'export-2')
E3 = sympy.symbols(u'export-3')

TEST_CASES = [
    pytest.param(E1 | E2 & ~E3, 3, id='socdem_free'),
    pytest.param(AGE_45_54, 3, id='identical'),
    pytest.param(~INCOME_B2, 3, id='negation'),
    pytest.param(GENDER_MALE & INCOME_A, 3, id='conjunction'),
    pytest.param(GENDER_FEMALE & GENDER_MALE, 3, id='conjunction_same_group'),
    pytest.param(GENDER_MALE | INCOME_A, 3, id='disjunction'),
    pytest.param(GENDER_FEMALE | GENDER_MALE, 3, id='disjunction_same_group'),
    pytest.param(GENDER_MALE | ~GENDER_FEMALE, 3, id='gender'),
    pytest.param(~(AGE_18_MINUS | AGE_55_PLUS) & (AGE_18_24 | AGE_25_34 | AGE_35_44 | AGE_45_54), 3, id='age'),
    pytest.param((INCOME_B1 | INCOME_B2 | INCOME_C1 | INCOME_C2) & ~INCOME_C2 & ~INCOME_A, 3, id='income'),
    pytest.param(~GENDER_FEMALE | ((INCOME_C2 | INCOME_C1) & ~(AGE_45_54 | AGE_55_PLUS)), 3, id='socdem_mix'),
    pytest.param(
        ~INCOME_C2 & GENDER_MALE & (E1 | E2 & ~E3) | INCOME_C2 & ~GENDER_MALE & ~(E1 | E2 & ~E3),
        3,
        id='mix_with_socdem_free',
    ),
    pytest.param(AGE_18_24 & GENDER_MALE & E1 & E2 & E3, 2, id='not_fully_evaluated_exact_socdem'),
    pytest.param(AGE_18_24 & GENDER_MALE & E1 & ~E1 & E2 & E3, 2, id='not_fully_evaluated_wider_socdem'),
    pytest.param(E1 | (INCOME_A | INCOME_B1) & GENDER_FEMALE & E2, 3, id='all'),
]


@pytest.mark.parametrize('expression, max_vars_to_evaluate', TEST_CASES)
def test_get_socdem_possibilities(expression, max_vars_to_evaluate):
    result = lib.get_socdem_possibilities(expression, max_vars_to_evaluate)

    output_file = os.path.join(yatest.common.work_path(), 'result')
    with open(output_file, 'w') as fout:
        six.print_('expression = {}'.format(expression), file=fout)
        six.print_('fully_evaluated = {}'.format(result.fully_evaluated), file=fout)

        table = [[symbol.name for symbol in result.socdem_symbols] + ['expression_value']]
        for values, possibility in six.iteritems(result.possibilities):
            table.append(list(values) + [possibility])

        six.print_(tabulate.tabulate(table, headers='firstrow'), file=fout)

    return yatest.common.canonical_file(output_file, local=True)
