from search.geo.tools.util.geobase_utils import (
    init,
    is_id_in,
    get_city,
    get_country,
    get_parents_list,
)


def test_init():
    init('geodata4.bin')


def test_get_parents_list():
    assert(get_parents_list(213) == [1, 3, 225, 10001, 10000])
    assert(get_parents_list(20490) == [120542, 20279, 213, 1, 3, 225, 10001, 10000])
    assert(get_parents_list(149) == [166, 10001, 10000])


def test_get_city():
    assert(get_city(20490) == 213)
    assert(get_city(2) == 2)
    assert(get_city(157) == 157)


def test_get_country():
    assert(get_country(20490) == 225)
    assert(get_country(2) == 225)
    assert(get_country(157) == 149)


def test_is_id_in():
    assert(is_id_in(20490, 213))
    assert(is_id_in(213, 225))
    assert(not is_id_in(149, 1))
