from mail.catdog.catdog.src.parser import RecipientAddress
from cpp import domainsInit, isPdd


def test_is_pdd_negative():
    assert isPdd(RecipientAddress(domain='negative').domain) is False


def test_is_pdd_positive():
    domainsInit(['positive'])
    assert isPdd(RecipientAddress(domain='positive').domain) is True


def test_is_pdd_cyrillic():
    domainsInit(['xn--80aalbavookw.xn--p1ai'])
    assert isPdd(RecipientAddress(domain='админкапдд.рф').domain) is True
