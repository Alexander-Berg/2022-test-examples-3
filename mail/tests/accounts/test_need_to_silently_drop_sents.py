import pytest
from fan.accounts.get import need_to_silently_drop_sents


pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def enable_feature(enable_silently_drop_sents_for_new_untrusty_accounts):
    pass


def test_on_new_unknown_account(new_unknown_account):
    assert need_to_silently_drop_sents(new_unknown_account) == True


def test_on_new_untrusty_account(new_untrusty_account):
    assert need_to_silently_drop_sents(new_untrusty_account) == True


def test_on_new_trusty_account(new_trusty_account):
    assert need_to_silently_drop_sents(new_trusty_account) == False


def test_on_old_unknown_account(old_unknown_account):
    assert need_to_silently_drop_sents(old_unknown_account) == False


def test_on_old_untrusty_account(old_untrusty_account):
    assert need_to_silently_drop_sents(old_untrusty_account) == False


def test_on_old_trusty_account(old_trusty_account):
    assert need_to_silently_drop_sents(old_trusty_account) == False
