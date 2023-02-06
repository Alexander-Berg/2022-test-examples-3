import pytest

from travel.avia.library.python.urls import travel_avia_route_landing, Environment
from travel.avia.library.python.urls.errors import MalformedUrlParameterValue


def test_route_landing():
    assert 'https://travel-test.yandex.ru/avia/routes/moscow--paris/' == travel_avia_route_landing(
        Environment.TESTING
    ).url('moscow', 'paris')
    assert 'https://travel.yandex.ru/avia/routes/moscow--paris/' == travel_avia_route_landing(
        Environment.PRODUCTION
    ).url('moscow', 'paris')

    with pytest.raises(MalformedUrlParameterValue):
        travel_avia_route_landing(Environment.PRODUCTION).url('moscow', '')

    with pytest.raises(MalformedUrlParameterValue):
        travel_avia_route_landing(Environment.PRODUCTION).url('moscow', None)

    with pytest.raises(MalformedUrlParameterValue):
        travel_avia_route_landing(Environment.PRODUCTION).url('', 'paris')

    with pytest.raises(MalformedUrlParameterValue):
        travel_avia_route_landing(Environment.PRODUCTION).url(None, 'paris')
