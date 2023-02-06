import pytest

from mail.nwsmtp.tests.lib.dkim_domains import DKIMDomain


@pytest.fixture
def external_dkim_domain() -> DKIMDomain:
    return DKIMDomain("external.ru", "selector")


@pytest.fixture
def disabled_dkim_domain() -> DKIMDomain:
    return DKIMDomain("disabled.ru", "disabled-selector", is_enabled=False)


@pytest.fixture
def wrong_dkim_domain() -> DKIMDomain:
    return DKIMDomain("wrong.ru", "wrong-selector", is_incorrect=True)
