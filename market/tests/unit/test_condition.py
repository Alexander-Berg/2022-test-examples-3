import pytest
from sympy.logic import boolalg as sympyboolalg

import edera.condition

from edera import Condition


class AlwaysTrue(Condition):

    def check(self):
        return True

    @property
    def name(self):
        return "AlwaysTrue"


class AlwaysFalse(Condition):

    def check(self):
        return False

    @property
    def name(self):
        return "AlwaysFalse"


class SometimesTrue(Condition):

    def check(self):
        return False  # you wish

    @property
    def name(self):
        return "SometimesTrue"


def test_condition_is_abstract():
    with pytest.raises(TypeError):
        Condition()


def test_condition_is_free_of_constraints_by_default():
    true = AlwaysTrue()
    assert true.invariants is None
    assert true.expression is None
    assert true.symbol.name == true.name


def test_condition_can_be_recovered_by_symbol():
    true = AlwaysTrue()
    assert true.from_symbol(true.symbol) == true


def test_condition_can_be_negated():
    true = AlwaysTrue()
    negation = ~true
    assert negation.check() == False
    assert negation.name == "~AlwaysTrue"
    assert negation.expression == ~true.symbol


def test_conditions_can_be_conjuncted():
    true = AlwaysTrue()
    false = AlwaysFalse()
    conjunction = true & false
    assert conjunction.check() == False
    assert conjunction.name == "(AlwaysTrue & AlwaysFalse)"
    assert conjunction.expression == true.symbol & false.symbol


def test_consecutive_conjunctions_fold_into_one():
    true = AlwaysTrue()
    false = AlwaysFalse()
    random = SometimesTrue()
    conjunction = true & (false & random)
    assert conjunction.check() == False
    assert conjunction.name == "(AlwaysTrue & AlwaysFalse & SometimesTrue)"
    assert conjunction.expression == sympyboolalg.And(true.symbol, false.symbol, random.symbol)


def test_conditions_can_be_disjuncted():
    true = AlwaysTrue()
    false = AlwaysFalse()
    disjunction = true | false
    assert disjunction.check() == True
    assert disjunction.name == "(AlwaysTrue | AlwaysFalse)"
    assert disjunction.expression == true.symbol | false.symbol


def test_consecutive_disjunctions_fold_into_one():
    true = AlwaysTrue()
    false = AlwaysFalse()
    random = SometimesTrue()
    disjunction = true | (false | random)
    assert disjunction.check() == True
    assert disjunction.name == "(AlwaysTrue | AlwaysFalse | SometimesTrue)"
    assert disjunction.expression == sympyboolalg.Or(true.symbol, false.symbol, random.symbol)


def test_conditions_can_be_exclusively_disjuncted():
    true = AlwaysTrue()
    false = AlwaysFalse()
    exclusive_disjunction = true ^ false
    assert exclusive_disjunction.check() == True
    assert exclusive_disjunction.name == "(AlwaysTrue ^ AlwaysFalse)"
    assert exclusive_disjunction.expression == true.symbol ^ false.symbol


def test_consecutive_exclusive_disjunctions_fold_into_one():
    true = AlwaysTrue()
    false = AlwaysFalse()
    random = SometimesTrue()
    disjunction = true ^ (false ^ random)
    assert disjunction.check() == True
    assert disjunction.name == "(AlwaysTrue ^ AlwaysFalse ^ SometimesTrue)"
    assert disjunction.expression == sympyboolalg.Xor(true.symbol, false.symbol, random.symbol)


def test_condition_can_imply_another_one():
    true = AlwaysTrue()
    false = AlwaysFalse()
    implication = true >> false
    assert implication.check() == False
    assert implication.name == "(AlwaysTrue >> AlwaysFalse)"
    assert implication.expression == true.symbol >> false.symbol
    reverse_implication = true << false
    assert reverse_implication.check() == True
    assert reverse_implication.name == "(AlwaysFalse >> AlwaysTrue)"
    assert reverse_implication.expression == false.symbol >> true.symbol


def test_conditions_constraint_gets_derived_correctly():

    class TestCondition(Condition):

        def check(self):
            return True

        @property
        def name(self):
            return self.__class__.__name__

    class DatabaseExists(TestCondition):

        pass

    class MainTableExists(TestCondition):

        @property
        def invariants(self):
            yield self >> DatabaseExists()

    class TemporaryTableExists(TestCondition):

        @property
        def invariants(self):
            yield self >> DatabaseExists()

    class MainTableIsEmpty(TestCondition):

        @property
        def invariants(self):
            yield ~self >> MainTableExists()

    class TemporaryTableIsEmpty(TestCondition):

        @property
        def invariants(self):
            yield ~self >> TemporaryTableExists()

    targets = [
        DatabaseExists(),
        MainTableExists(),
        TemporaryTableExists(),
        ~TemporaryTableIsEmpty(),
        ~MainTableIsEmpty(),
        ~TemporaryTableExists(),
    ]
    constraint = edera.condition.derive_constraint(targets)
    assert sympyboolalg.is_cnf(constraint)
    expected_constraint = (
        (MainTableExists().symbol >> DatabaseExists().symbol)
        & (~(~TemporaryTableExists()).symbol >> DatabaseExists().symbol)
        & ((~MainTableIsEmpty()).symbol >> MainTableExists().symbol)
        & ((~TemporaryTableIsEmpty()).symbol >> TemporaryTableExists().symbol)
        & sympyboolalg.Equivalent(~TemporaryTableExists().symbol, (~TemporaryTableExists()).symbol)
    )
    assert sympyboolalg.simplify_logic(sympyboolalg.Equivalent(constraint, expected_constraint)) == True
