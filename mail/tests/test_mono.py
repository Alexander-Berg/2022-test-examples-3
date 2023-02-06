import pytest

from mail.catdog.catdog.src.data.top200 import map_of_top_senders_icons
from mail.catdog.catdog.src import mono
from mail.catdog.catdog.src.parser import RecipientAddress
from mail.catdog.catdog.tests.mock_context import MockContext
from cpp import addOrganizationColor, setPalette


addOrganizationColor('domain', '#808080')
addOrganizationColor('d.ru', '#808080')
setPalette(['red', 'green', 'blue'])


@pytest.mark.parametrize(('email', 'chars', 'color'), [
    (RecipientAddress(display_name='ab', local='local', domain='domain'), 'A', '#808080'),
    (RecipientAddress(display_name='AB', local='local', domain='domain'), 'A', '#808080'),
    (RecipientAddress(display_name=' ab ', local='local', domain='domain'), 'A', '#808080'),
    (RecipientAddress(display_name='a b', local='local', domain='domain'), 'A', '#808080'),
    (RecipientAddress(local='local', domain='domain'), 'D', '#808080'),
    (RecipientAddress(local='local', domain='x.d.ru'), 'D', '#808080'),
    (RecipientAddress(local='local', domain='other'), 'O', 'green')
])
def test_mono1(email, chars, color):
    email = mono.type1(email, MockContext())
    assert email.mono == chars
    assert email.color == color


addOrganizationColor('third.second.first', '#ffffff')
addOrganizationColor('just.this', '#ffffff')

map_of_top_senders_icons['top3.top2.top1'] = '#ffffff'
map_of_top_senders_icons['just.top'] = '#ffffff'


@pytest.mark.parametrize(('orig', 'expected'), [
    ('d1', 'd1'),
    ('a.b.c.d', 'a.b.c.d'),
    ('third.second.first', 'third.second.first'),
    ('extra.just.this', 'just.this'),
    ('top3.top2.top1', 'top3.top2.top1'),
    ('extra.just.top', 'just.top')
])
def test_get_domain(orig, expected):
    assert mono.get_domain(RecipientAddress(domain=orig)) == expected


def test_colorhash_idempotency():
    assert mono.colorhash('abcdef') == mono.colorhash('abcdef')


def test_colorhash_variability():
    assert mono.colorhash('a') != mono.colorhash('b')


@pytest.mark.parametrize(('email', 'chars', 'color'), [
    (RecipientAddress(display_name='a', local='local', domain='domain'), 'A', 'green'),
    (RecipientAddress(display_name='abx', local='local', domain='domain'), 'AB', 'green'),
    (RecipientAddress(display_name='ABx', local='local', domain='domain'), 'AB', 'green'),
    (RecipientAddress(display_name=' abx ', local='local', domain='domain'), 'AB', 'green'),
    (RecipientAddress(display_name='ax bx', local='local', domain='domain'), 'AB', 'green'),
    (RecipientAddress(display_name='ax bx cx', local='local', domain='domain'), 'AB', 'green'),
    (RecipientAddress(display_name='Ya.Service', local='local', domain='domain'), 'YS', 'green'),
    (RecipientAddress(display_name='local@domain.ru', local='local', domain='domain.ru'), 'LO', 'blue'),
    (RecipientAddress(display_name='"local@domain.ru"', local='local', domain='domain.ru'), 'LO', 'blue'),
    (RecipientAddress(display_name='', local='local', domain='domain'), 'LO', 'green'),
    (RecipientAddress(local='l', domain='domain'), 'L', 'blue'),
    (RecipientAddress(local='l.o', domain='domain'), 'LO', 'red')
])
def test_mono2(email, chars, color):
    email = mono.type2(email, MockContext())
    assert email.mono == chars
    assert email.color == color


@pytest.mark.parametrize(('string', 'alnum'), [
    ('abc12', 'abc12'),
    ('a.-"<\'!@#b', 'ab'),
    ('вася', 'вася'),
    ('', '')
])
def test_filter_symbols(string, alnum):
    assert mono.filter_symbols(string) == alnum


@pytest.mark.parametrize(('lst', 'filtered'), [
    (['a1', 'b2'], ['a1', 'b2']),
    (['a.-"<\'!@#b', 'c'], ['ab', 'c']),
    (['!!!', 'a'], ['a'])
])
def test_filter_words(lst, filtered):
    assert mono.filter_words(lst) == filtered


def test_mono2_do_not_distinct_yandex_aliases():
    a = mono.type2(RecipientAddress(local='xxx', domain='yandex.ru'), MockContext())
    b = mono.type2(RecipientAddress(local='xxx', domain='ya.ru'), MockContext())
    assert a.mono == b.mono and a.color == b.color


def test_mono2_do_not_distinct_yandex_aliases_including_dots_and_dashes():
    a = mono.type2(RecipientAddress(local='a.bc', domain='yandex.ru'), MockContext())
    b = mono.type2(RecipientAddress(local='a-bc', domain='yandex.ru'), MockContext())
    assert a.mono == b.mono and a.color == b.color
