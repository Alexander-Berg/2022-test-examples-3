from market.pylibrary.mi_util import util


def test_watching():
    assert util.watching_check_call(['/bin/echo']) == 0


def test_watching_input():
    assert util.watching_check_call(['/bin/cat'], cmd_input='asdf') == 0
