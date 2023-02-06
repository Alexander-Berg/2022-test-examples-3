
from datetime import datetime

from werkzeug.wrappers import Response

from pymail.web_tools.json_helpers import jdumps, json_response


def test_jdumps():
    assert jdumps({
        'int': 1,
        'date': datetime(2001, 1, 1)
    }) == '{"int": 1, "date": "2001-01-01T00:00:00"}'


def test_json_resonse():

    @json_response
    def test_target():
        return {'foo': 'bar'}

    response = test_target()
    assert isinstance(response, Response)
    assert response.mimetype == 'application/json'
    assert response.response[0] == '{"foo": "bar"}'
