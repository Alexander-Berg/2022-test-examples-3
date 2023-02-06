import json
import requests

from crypta.cm.services.common.data.python.id import TId


def test_valid(cm_client):
    response = cm_client.identify(TId("turbouid", "AAAWb+jcpmHaqk/ide7N8VjNWDoH1g7nxkZWQGRWHC5+qHFrHfI+Cuaqd5izFZnw5fe1PkprXB1I90yHusgAi3UhT572V6SDSHPJNxaY"))
    assert requests.codes.ok == response.status_code, response.text

    responded_ids = json.loads(response.text)
    assert 1 == len(responded_ids)

    responded_id = responded_ids[0]
    assert "yandexuid" == responded_id["type"]
    assert "12345678901234567890" == responded_id["value"]
    assert "domain.ru" == responded_id["attributes"]["domain"]


def test_broken_turbouid(cm_client):
    response = cm_client.identify(TId("turbouid", "BROKEN"))
    assert requests.codes.bad_request == response.status_code, response.text
