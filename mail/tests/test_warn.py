from cpp import is_warning
from mail.catdog.catdog.src.parser import RecipientAddress
from mail.catdog.catdog.tests.mock_context import MockContext


def test_common_off_false():
    assert False is is_warning(RecipientAddress(), MockContext())


def test_common_on_good_false():
    ctx = MockContext()
    ctx.check_warn = True
    assert False is is_warning(RecipientAddress(local='a'), ctx)


def test_common_on_noreply_true():
    ctx = MockContext()
    ctx.check_warn = True
    assert True is is_warning(RecipientAddress(local='noreply'), ctx)
