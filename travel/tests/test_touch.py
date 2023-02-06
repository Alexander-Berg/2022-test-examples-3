import pytest

from url_mapper import TouchToRaspMapper


DISABLED_PATHS = [
    'tariffs',
    'direction_stations',
    'main_tariffs',
    'suburban-directions',
    'stations'
]


@pytest.mark.parametrize("path", DISABLED_PATHS)
def test_touch_to_rasp_has_no_mapping(path):
    mapper = TouchToRaspMapper(domain='rasp.yandex.net', schema='https')
    path = '/{}/whatever'.format(path)
    assert mapper.has_mapping(path) is False
