from mail.catdog.catdog.src import yahoo
from mail.catdog.catdog.src.parser import RecipientAddress


def test_is_yahoo_positive():
    assert yahoo.is_mailing_system(RecipientAddress(domain='yahoo.com')) is True


def test_is_yahoo_negative():
    assert yahoo.is_mailing_system(RecipientAddress(domain='vk.com')) is False
