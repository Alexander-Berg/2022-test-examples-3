from mail.devpack.lib.components.unimock import Unimock


def test_unimock_get(coordinator):
    unimock = coordinator.components[Unimock]
    response = unimock.get('/some_request?uid=123')
    assert response.status_code == 200
    assert response.text == 'ok'


def test_unimock_post(coordinator):
    unimock = coordinator.components[Unimock]
    response = unimock.post('/some_request?uid=123', '{"key": "value"}')
    assert response.status_code == 200
    assert response.text == 'ok'
