from market.idx.pylibrary.juggler import JugglerEventSender
from hamcrest import assert_that, calling, raises
import requests
import requests_mock


def test_client_send_event_ok():
    """Проверяем, что посылка события в глобальную ручку генерит правильный запрос
    """
    def match_request_text(request):
        return request.json() == {
            'source': 'market-indexer',
            'events': [
                {
                    'host': 'uinttest-host',
                    'service': 'unittest-service',
                    'status': 'OK',
                    'description': 'testing market/idx/pylibrary/juggler',
                    'tags': []
                }
            ]
        }

    with requests_mock.Mocker() as m:
        m.post('http://juggler-push.search.yandex.net/events', json={'events': [{'code': 200, 'message': 'OK'}], 'accepted_events': 1, 'success': True}, additional_matcher=match_request_text)
        client = JugglerEventSender()
        client.send_event('uinttest-host', 'unittest-service', 'OK', 'testing market/idx/pylibrary/juggler')


def test_client_send_event_bad_status():
    """Проверяем, что посылка события в глобальную ручку с невалидным статусом приводит к исключению
    """
    with requests_mock.Mocker() as m:
        m.post('http://juggler-push.search.yandex.net/events', json={'accepted_events': 0, 'events': [{'message': "Status 'BU' is not valid", 'code': 400}], 'success': True})
        client = JugglerEventSender()
        assert_that(
            calling(client.send_event).with_args(
                'uinttest-host', 'unittest-service', 'BU', 'testing market/idx/pylibrary/juggler'
            ),
            raises(requests.exceptions.HTTPError, "Status 'BU' is not valid"))
