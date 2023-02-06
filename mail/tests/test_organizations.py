import pytest

import mail.catdog.catdog.src.organizations as org
from mail.catdog.catdog.src.parser import RecipientAddress
from cpp import shift, hasOrganizationColor, getOrganizationColor


def test_is_organization_positive():
    assert org.is_organization(RecipientAddress(domain='1c.ru')) is True


def test_is_organization_negative():
    assert org.is_organization(RecipientAddress(domain='vasya.name')) is False


def test_has_organization_color_positive():
    assert hasOrganizationColor(RecipientAddress(domain='1c.ru').domain) is True


def test_has_organization_color_negative():
    assert hasOrganizationColor(RecipientAddress(domain='vasya.name').domain) is False


def test_has_organization_color_none():
    assert hasOrganizationColor(RecipientAddress(domain='1jur.com').domain) is False


def test_get_organization_color():
    assert getOrganizationColor(RecipientAddress(domain='1c.ru').domain) == '#F8F8B0'


@pytest.mark.parametrize(('color', 'shifted'), [
    ('#000000', '#1A1A1A'),  # black lightens
    ('#191919', '#212121'),  # dark lightens
    ('#4C0000', '#630000'),  # non-gray dark lightens
    ('#808080', '#808080'),  # meduim unchanged
    ('#0000FF', '#0000FF'),  # non-gray meduim unchanged
    ('#FFE8FF', '#E6D1E6'),  # non-gray light darkens
    ('#E8E8E8', '#D1D1D1'),  # light darkens
    ('#FFFFFF', '#E6E6E6')   # white darkens
])
def test_filter_symbols(color, shifted):
    assert shift(color) == shifted
