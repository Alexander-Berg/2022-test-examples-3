from travel.rasp.library.python.common23.xgettext.common import get_common_keyset


def test_get_common_keyset():
    assert get_common_keyset('days')['en']['days']['n7'] == 'Sunday'
