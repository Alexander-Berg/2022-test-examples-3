from crypta.cm.services.common.data.python.id import TId
from crypta.cm.services.common.ping.python import constants


def test_ping_response_value():
    assert TId("yandexuid", "10000000001400000000") == constants.PING_RESPONSE_ID
