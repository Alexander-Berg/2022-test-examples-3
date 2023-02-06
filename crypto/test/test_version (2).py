import logging
import re


logger = logging.getLogger(__name__)


def test_response_pattern(siberia_client):
    text = siberia_client.version().Message
    assert re.search(r"Built from Arcadia rev\. -?\d+", text)
