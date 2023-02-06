# coding: utf-8

from mail.pypg.pypg.types.db_enums import DBEnum
import pytest


class Bugs(DBEnum):
    blocker = 'blocker'
    critical = 'critical'
    minor = 'minor'

    def name_in_db(self):
        return 'should.fix'


def test_to_pg_put():
    assert Bugs.blocker.getquoted() == b"'blocker'::should.fix"


def test_equality_check():
    assert Bugs.blocker == 'blocker'
    assert Bugs.blocker != 'critical'
    assert Bugs.blocker == Bugs.blocker
    assert Bugs.blocker != Bugs.critical


class EnumWithoutName(DBEnum):
    field = 'field'


def test_enum_should_emplement_name_in_db():
    with pytest.raises(NotImplementedError):
        EnumWithoutName.field.getquoted()
