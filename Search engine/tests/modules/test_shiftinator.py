import pytest

from bot.aioshiftinator import _strip_logins


@pytest.mark.parametrize("test_input,expected", [
    (dict(login='marty'), 'marty'),
    (dict(login='marty1@'), 'marty1'),
    ('marty', 'marty'),
    ('marty@', 'marty'),
    (['marty@', 'marty1@', 'marty2@'], ['marty', 'marty1', 'marty2']),
    ([dict(login='marty@'), dict(login='marty1@'), dict(login='marty2@')], ['marty', 'marty1', 'marty2'])
])
def test_strip_logins(test_input, expected):
    assert _strip_logins(test_input) == expected
