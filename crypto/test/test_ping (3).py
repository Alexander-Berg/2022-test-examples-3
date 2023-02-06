import logging

logger = logging.getLogger(__name__)


def test_response_ok(siberia_client):
    assert "OK" == siberia_client.ping().Message.strip()
